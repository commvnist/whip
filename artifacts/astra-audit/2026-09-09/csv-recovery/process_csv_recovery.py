"""Native CSV recovery on an explicitly selected disposable emulator.

Install debug and test APKs first. The opt-in seed replaces only debug fixture
data. Usage: python3 process_csv_recovery.py emulator-5554 OUTPUT_DIRECTORY [2.0]
"""
import hashlib
import json
import pathlib
import re
import sqlite3
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

serial, destination = sys.argv[1:3]
font = sys.argv[3] if len(sys.argv) > 3 else "1.0"
assert re.fullmatch(r"emulator-\d+", serial) and font in ("1.0", "2.0")
out = pathlib.Path(destination)
out.mkdir(parents=True, exist_ok=True)
pkg = "commvne.com.whip.app.debug"
adb = ["adb", "-s", serial]
transitions = []


def shell(*args, check=True):
    return subprocess.run(adb + ["shell", *args], check=check, text=True,
                          capture_output=True, timeout=180).stdout


assert any(shell("getprop", key).strip() == "1" for key in ("ro.boot.qemu", "ro.kernel.qemu"))
original_font = shell("settings", "get", "system", "font_scale").strip()


def hierarchy():
    for _ in range(6):
        result = shell("uiautomator", "dump", "/sdcard/whip-csv-driver.xml")
        if "dumped to" in result:
            xml = shell("cat", "/sdcard/whip-csv-driver.xml")
            return xml, list(ET.fromstring(xml).iter("node"))
        time.sleep(.25)
    raise AssertionError("No settled Android hierarchy")


def bounds(node):
    return tuple(map(int, re.findall(r"\d+", node.get("bounds"))))


def find(attribute, value, scroll=False):
    for _ in range(16):
        xml, nodes = hierarchy()
        matches = [n for n in nodes if n.get(attribute) == value]
        if matches:
            return xml, matches[-1]
        if scroll:
            containers = [n for n in nodes if n.get("scrollable") == "true" and n.get("package") == pkg]
            if containers:
                x1, y1, x2, y2 = bounds(max(containers, key=lambda n: bounds(n)[3] - bounds(n)[1]))
                shell("input", "swipe", str((x1+x2)//2), str(y2-(y2-y1)//5),
                      str((x1+x2)//2), str(y1+(y2-y1)//5), "350")
        time.sleep(.2)
    (out / "unexpected.xml").write_text(xml)
    capture("unexpected", xml)
    raise AssertionError((attribute, value))


def tap(attribute, value, scroll=False):
    _, node = find(attribute, value, scroll)
    assert node.get("enabled") != "false", value
    x1, y1, x2, y2 = bounds(node)
    shell("input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def capture(name, xml=None):
    if xml is None:
        xml, _ = hierarchy()
    (out / (name + ".xml")).write_text(xml)
    (out / (name + ".png")).write_bytes(subprocess.check_output(adb + ["exec-out", "screencap", "-p"]))
    print("Captured", name, flush=True)


def select_file():
    tap("content-desc", "Show roots")
    tap("text", "Downloads")
    tap("text", fixture["fileName"])


def snapshot(name):
    path = out / (name + ".db")
    for suffix in ("", "-wal"):
        result = subprocess.run(adb + ["exec-out", "run-as", pkg, "cat", "databases/whip.db" + suffix], capture_output=True)
        if suffix == "":
            result.check_returncode()
        if result.returncode == 0:
            pathlib.Path(str(path) + suffix).write_bytes(result.stdout)
    with sqlite3.connect(path) as db:
        assert db.execute("PRAGMA integrity_check").fetchone() == ("ok",)
        return dict(entries=db.execute("select id,uuid,entryEpochDay from track_entries order by id").fetchall(),
                    values=db.execute("select e.uuid,f.name,v.textValue,v.enteredNumber,v.enteredUnitId,v.booleanValue "
                                      "from track_values v join track_fields f on f.id=v.fieldId "
                                      "join track_entries e on e.id=v.entryId order by e.id,f.position").fetchall(),
                    receipts=db.execute("select batchUuid,requestFingerprint,entryIdentityDigest,rowCount from track_csv_import_receipts").fetchall())


def restart_process(name, while_dead=None):
    old_pid = shell("pidof", pkg).strip()
    activities = shell("dumpsys", "activity", "activities")
    task = re.search(re.escape(pkg) + r"/com\.whip\.app\.MainActivity t(\d+)", activities)[1]
    shell("input", "keyevent", "3")
    time.sleep(2)
    stopped = shell("dumpsys", "activity", "activities")
    assert "STOPPED" in stopped and pkg in stopped
    (out / (name + "-background.txt")).write_text(stopped)
    shell("am", "kill", pkg)
    for _ in range(30):
        if not shell("pidof", pkg, check=False).strip():
            break
        time.sleep(.2)
    else:
        raise AssertionError("The old process did not die")
    database = snapshot(name)
    if while_dead:
        while_dead()
    shell("am", "task", "focus", task)
    for _ in range(30):
        new_pid = shell("pidof", pkg, check=False).strip()
        if new_pid:
            break
        time.sleep(.2)
    assert new_pid and new_pid != old_pid
    transitions.append(dict(name=name, oldPid=old_pid, newPid=new_pid, oldProcessAbsent=True,
                            taskId=task, entriesBeforeRestart=len(database["entries"])))
    (out / "transitions.json").write_text(json.dumps(transitions, indent=2) + "\n")
    print("Replaced process", name, old_pid, "->", new_pid, flush=True)
    return database


try:
    shell("am", "force-stop", pkg)
    seed = shell("am", "instrument", "-w", "-e", "class",
                 "com.whip.app.TrackCsvJourneyE2ETest#csvValuesCanBeReviewedRemappedRecoveredAndImported",
                 "-e", "whipCsvSeedOnly", "true", pkg + ".test/androidx.test.runner.AndroidJUnitRunner")
    (out / "seed.log").write_text(seed)
    assert "OK (1 test)" in seed, seed
    fixture = dict(line.split("=", 1) for line in shell("run-as", pkg, "cat", "files/csv-process-fixture.txt").splitlines())
    (out / "fixture.json").write_text(json.dumps(fixture, indent=2) + "\n")
    source_path = "/sdcard/Download/" + fixture["fileName"]
    original = shell("cat", source_path)
    assert "Distance (Unit)" in original
    shell("settings", "put", "system", "font_scale", font)
    shell("am", "start", "-W", "-f", "0x10200000", "-n", pkg + "/com.whip.app.MainActivity",
          "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER")
    tap("content-desc", "Tracks tab")
    tap("text", fixture["trackName"], scroll=True)
    tap("content-desc", "Options")
    tap("text", "Import Entries from CSV", scroll=True)
    select_file()
    find("text", "Import 2 Entries")
    tap("text", "Next Entry", scroll=True)
    xml, _ = find("text", "Hill circuit", scroll=True)
    capture("ready-second", xml)
    before = restart_process("unchanged")
    assert not before["entries"] and not before["receipts"]
    xml, _ = find("text", "Hill circuit", scroll=True)
    capture("recovered-unchanged", xml)

    changed = original.replace("River loop", "Changed river loop")
    replacement = out / "changed.csv"
    replacement.write_text(changed)
    def replace_source():
        subprocess.run(adb + ["push", str(replacement), source_path], check=True, capture_output=True)
    before = restart_process("changed", replace_source)
    assert not before["entries"] and not before["receipts"]
    xml, _ = find("text", "The selected file changed after this preview was saved. Choose Replace File to review the current contents as a new import.", scroll=True)
    capture("changed-file-blocked", xml)
    tap("text", "Replace File", scroll=True)
    select_file()
    xml, _ = find("text", "Changed river loop", scroll=True)
    capture("replacement-reviewed", xml)
    tap("text", "Import 2 Entries")
    find("text", "Import Complete")
    find("text", "Done")
    capture("import-complete")
    committed = restart_process("source-removed", lambda: shell("rm", "-f", source_path))
    assert len(committed["entries"]) == 2 and len(committed["receipts"]) == 1
    assert committed["receipts"][0][3] == 2
    assert [v[2] for v in committed["values"] if v[1] == "Name"] == ["Changed river loop", "Hill circuit"]
    assert [v[4] for v in committed["values"] if v[1] == "Distance"] == ["mile", "kilometre"]
    xml, _ = find("text", "This 2-Entry import had already finished. Nothing was duplicated.", scroll=True)
    capture("completed-without-source", xml)
    assert "Replace File" not in xml
    tap("text", "Done")
    final = snapshot("final")
    assert final == committed
    proof = dict(serial=serial, fontScale=font, transitions=transitions,
                 sourceChangedSha256=hashlib.sha256(changed.encode()).hexdigest(),
                 sameEntriesAndValuesAfterRecovery=True, committed=committed)
    (out / "proof.json").write_text(json.dumps(proof, indent=2) + "\n")
    print(json.dumps(proof), flush=True)
finally:
    shell("settings", "put", "system", "font_scale", original_font)
