"""Explicit disposable-emulator fixture: real process death, recovery and exact Save.

Install the current debug and androidTest APKs first. This replaces debug fixture
data through the opt-in test seed; it refuses every non-emulator target.
Usage: python3 process_recovery.py emulator-5554 OUTPUT_DIRECTORY
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

serial, destination = sys.argv[1:]
assert re.fullmatch(r"emulator-\d+", serial)
out = pathlib.Path(destination)
out.mkdir(parents=True, exist_ok=True)
pkg = "commvne.com.whip.app.debug"
adb = ["adb", "-s", serial]


def shell(*args, check=True):
    return subprocess.run(adb + ["shell", *args], text=True, capture_output=True, check=check).stdout


assert any(shell("getprop", key).strip() == "1" for key in ("ro.boot.qemu", "ro.kernel.qemu"))
shell("am", "force-stop", pkg)
seed = shell("am", "instrument", "-w", "-e", "class",
             "com.whip.app.TrackDraftSavedStateJourneyTest#largeAcceptedEntryKeepsSavedStateWithinPlatformTransport",
             "-e", "whipTrackDraftSeedOnly", "true", pkg + ".test/androidx.test.runner.AndroidJUnitRunner")
(out / "seed.log").write_text(seed)
assert "OK (1 test)" in seed, seed
fixture = dict(line.split("=", 1) for line in shell(
    "run-as", pkg, "cat", "files/track-draft-process-fixture.txt").splitlines())
(out / "fixture.json").write_text(json.dumps(fixture, indent=2) + "\n")
launch = shell("am", "start", "-W", "-f", "0x10200000", "-n", pkg + "/com.whip.app.MainActivity",
               "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER")
(out / "launch.log").write_text(launch)


def hierarchy():
    for _ in range(10):
        result = shell("uiautomator", "dump", "/sdcard/track-draft-driver.xml")
        if "dumped to" in result:
            xml = shell("cat", "/sdcard/track-draft-driver.xml")
            return xml, list(ET.fromstring(xml).iter("node"))
        time.sleep(.3)
    raise AssertionError("No settled Android hierarchy")


def find(attribute, value):
    for _ in range(12):
        xml, nodes = hierarchy()
        matches = [n for n in nodes if n.get(attribute) == value]
        if matches:
            return xml, matches[0]
        time.sleep(.3)
    (out / "unexpected.xml").write_text(xml)
    raise AssertionError((attribute, value))


def tap(attribute, value):
    _, node = find(attribute, value)
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    shell("input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def capture(name, xml):
    (out / (name + ".xml")).write_text(xml)
    image = subprocess.check_output(adb + ["exec-out", "screencap", "-p"])
    (out / (name + ".png")).write_bytes(image)


tap("content-desc", "Tracks tab")
tap("text", "Imported observation archive")
tap("content-desc", "Edit Entry River trail")
tap("text", "River trail")
shell("input", "keyevent", "123")
shell("input", "text", "%sfinal-recovery")
expected_name = "River trail final-recovery"
find("text", expected_name)
shell("input", "keyevent", "4")
xml, _ = find("text", expected_name)
capture("before", xml)
activities = shell("dumpsys", "activity", "activities")
task_id = re.search(re.escape(pkg) + r"/com\.whip\.app\.MainActivity t(\d+)", activities)[1]
old_pid = shell("pidof", pkg).strip()
shell("input", "keyevent", "3")
time.sleep(2)
stopped = shell("dumpsys", "activity", "activities")
(out / "background.txt").write_text(stopped)
assert "STOPPED" in stopped and pkg in stopped
files = shell("run-as", pkg, "find", "no_backup/track-editor-checkpoints", "-type", "f").splitlines()
assert files
shell("am", "kill", pkg)
for _ in range(30):
    if not shell("pidof", pkg, check=False).strip():
        break
    time.sleep(.3)
else:
    raise AssertionError("The old process did not die")
(out / "kill.txt").write_text("pidof empty after am kill; old process absent\n")
shell("am", "task", "focus", task_id)
xml, _ = find("text", expected_name)
new_pid = shell("pidof", pkg).strip()
assert new_pid and new_pid != old_pid
capture("recovered", xml)
tap("text", "Save")
find("content-desc", "Edit Entry " + expected_name)
xml, _ = hierarchy()
capture("saved", xml)
for suffix in ("", "-wal"):
    data = subprocess.check_output(adb + ["exec-out", "run-as", pkg, "cat", "databases/whip.db" + suffix])
    (out / ("verification.db" + suffix)).write_bytes(data)
db = sqlite3.connect(out / "verification.db")
assert db.execute("PRAGMA integrity_check").fetchone() == ("ok",)
values = dict(db.execute("select f.name,v.textValue from track_values v join track_fields f on f.id=v.fieldId"))
original = "".join(f"Observation {i}: check the route, weather and conditions on the next walk.\n" for i in range(12000))[:600000] + "KEEP-END"
assert values["Name"] == expected_name
assert values["Source notes"] == original
entries = db.execute("select id,uuid,entryEpochDay from track_entries").fetchall()
assert len(entries) == 1 and str(entries[0][0]) == fixture["entryId"] and entries[0][1] == fixture["entryUuid"]
remaining = shell("run-as", pkg, "find", "no_backup/track-editor-checkpoints", "-type", "f").splitlines()
assert not remaining
proof = dict(serial=serial, oldPid=old_pid, newPid=new_pid, taskId=task_id, entry=entries,
             savedName=expected_name, sourceChars=len(original), exactSourceEqual=True,
             sourceSha256=hashlib.sha256(original.encode()).hexdigest(), checkpointFilesAfterSave=0)
(out / "proof.json").write_text(json.dumps(proof, indent=2) + "\n")
print(json.dumps(proof))
