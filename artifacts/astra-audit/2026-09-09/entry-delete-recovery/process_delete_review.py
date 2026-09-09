"""Disposable-emulator fixture: retained deletion review, conflict, fresh Delete and Undo.

Install the current debug and androidTest APKs first. This replaces debug fixture
data through the opt-in test seed; it refuses every non-emulator target.
Usage: python3 process_delete_review.py emulator-5556 OUTPUT_DIRECTORY
"""
import json
import pathlib
import re
import shlex
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
    return subprocess.run(adb + ["shell", shlex.join(args)], text=True, capture_output=True, check=check).stdout


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
shell("run-as", pkg, "sqlite3", "databases/whip.db", "UPDATE track_values SET textValue='Opening note' WHERE fieldId=(SELECT id FROM track_fields WHERE name='Source notes');")
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
tap("content-desc", "More Actions for River trail")
tap("text", "Delete Entry")
xml, _ = find("text", "Delete River trail?")
capture("before", xml)
activities = shell("dumpsys", "activity", "activities")
task_id = re.search(re.escape(pkg) + r"/com\.whip\.app\.MainActivity t(\d+)", activities)[1]
old_pid = shell("pidof", pkg).strip()
shell("input", "keyevent", "3")
time.sleep(2)
shell("am", "kill", pkg)
for _ in range(30):
    if not shell("pidof", pkg, check=False).strip(): break
    time.sleep(.3)
else: raise AssertionError("Old process did not die")
(out / "kill.txt").write_text("old process absent after am kill\n")
# Controlled concurrent edit in the synthetic fixture after the reviewed owner dies.
# Keep all identity and display fields equal; the exact content revision must protect it.
shell("run-as", pkg, "sqlite3", "databases/whip.db", "UPDATE track_values SET textValue='Newer note written after review' WHERE fieldId=(SELECT id FROM track_fields WHERE name='Source notes');")
shell("am", "task", "focus", task_id)
xml, _ = find("text", "Delete River trail?")
# Allow asynchronous new-owner preparation to settle before confirming the retained review.
time.sleep(2)
xml, _ = hierarchy()
new_pid = shell("pidof", pkg).strip()
assert new_pid and new_pid != old_pid
capture("recovered", xml)
tap("text", "Delete Entry")
time.sleep(1)
xml, _ = hierarchy()
capture("after-confirm", xml)
rows = shell("run-as", pkg, "sqlite3", "databases/whip.db", "SELECT count(*) FROM track_entries;").strip()
value = shell("run-as", pkg, "sqlite3", "databases/whip.db", "SELECT textValue FROM track_values WHERE fieldId=(SELECT id FROM track_fields WHERE name='Source notes');").strip()
proof=dict(oldPid=old_pid,newPid=new_pid,taskId=task_id,remainingEntries=int(rows),remainingNote=value)
(out / "proof.json").write_text(json.dumps(proof,indent=2)+"\n")
print(json.dumps(proof))
assert rows == '1' and value == 'Newer note written after review', "Recovered review deleted a newer unreviewed Entry revision"

tap("text", "Review Entry")
xml, _ = find("text", "Newer note written after review")
capture("review-latest", xml)
tap("content-desc", "Close Track Entry details")
tap("content-desc", "More Actions for River trail")
tap("text", "Delete Entry")
find("text", "Delete River trail?")
tap("text", "Delete Entry")
tap("text", "Undo")
xml, _ = find("content-desc", "Edit Entry River trail")
capture("undo", xml)
restored = shell("run-as", pkg, "sqlite3", "databases/whip.db", "SELECT textValue FROM track_values WHERE fieldId=(SELECT id FROM track_fields WHERE name='Source notes');").strip()
uuid = shell("run-as", pkg, "sqlite3", "databases/whip.db", "SELECT uuid FROM track_entries;").strip()
assert restored == 'Newer note written after review' and uuid == fixture['entryUuid']
proof.update(reviewLatestReached=True, freshDeletionUndone=True, restoredNote=restored, restoredEntryUuid=uuid)
(out / "proof.json").write_text(json.dumps(proof,indent=2)+"\n")
print(json.dumps(proof))
