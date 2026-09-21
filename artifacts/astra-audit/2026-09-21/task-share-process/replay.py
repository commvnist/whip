"""Cold external Task share through a managed Android process kill.

Requires the current debug APK installed and Setup completed on a disposable
API 34 emulator. Preserves other synthetic app data; adds one named Task.
Usage: python3 replay.py emulator-5556 OUTPUT_DIRECTORY
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
assert re.fullmatch(r"emulator-\d+", serial), "Refusing a non-emulator target"
out = pathlib.Path(destination)
out.mkdir(parents=True, exist_ok=True)
pkg = "commvne.com.whip.app.debug"
adb = ["adb", "-s", serial]


def shell(*args, check=True):
    return subprocess.run(adb + ["shell", *args], text=True, capture_output=True, check=check).stdout


def query(sql):
    return shell(f"run-as {pkg} sqlite3 databases/whip.db {shlex.quote(sql)}").strip()


assert any(shell("getprop", key).strip() == "1" for key in ("ro.boot.qemu", "ro.kernel.qemu"))
assert shell("getprop", "ro.build.version.sdk").strip() == "34"
assert shell("pm", "path", pkg).strip()
assert "value=\"true\"" in shell("run-as", pkg, "cat", "shared_prefs/whip-settings.xml")
original = "External process audit " + str(int(time.time()))
edited = original + " reviewed"
assert query(f"SELECT COUNT(*) FROM tasks WHERE title='{original}';") == "0"
shell("am", "force-stop", pkg)
launch = shell(
    f"am start -W -a android.intent.action.SEND -t text/plain "
    f"--es android.intent.extra.TEXT {shlex.quote(original)} "
    f"-n {pkg}/com.whip.app.MainActivity",
)
(out / "launch.log").write_text(launch)


def hierarchy():
    for _ in range(10):
        result = shell("uiautomator", "dump", "/sdcard/task-share-process.xml")
        if "dumped to" in result:
            xml = shell("cat", "/sdcard/task-share-process.xml")
            return xml, list(ET.fromstring(xml).iter("node"))
        time.sleep(.3)
    raise AssertionError("No settled Android hierarchy")


def find(attribute, value):
    for _ in range(15):
        xml, nodes = hierarchy()
        matches = [node for node in nodes if node.get(attribute) == value]
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


find("text", original)
shell("input", "keyevent", "123")
shell("input", "text", "%sreviewed")
before, _ = find("text", edited)
capture("before-process-death", before)
assert query(f"SELECT COUNT(*) FROM tasks WHERE title='{edited}';") == "0"
activities = shell("dumpsys", "activity", "activities")
task_id = re.search(re.escape(pkg) + r"/com\.whip\.app\.MainActivity t(\d+)", activities)[1]
old_pid = shell("pidof", pkg).strip()
assert old_pid.isdecimal()
shell("input", "keyevent", "3")
for _ in range(20):
    stopped = shell("dumpsys", "activity", "activities")
    if "state=STOPPED" in stopped and f" t{task_id}" in stopped:
        break
    time.sleep(.3)
else:
    raise AssertionError("Whip did not reach the stopped task state")
shell("am", "start", "-W", "-n", "com.android.settings/.Settings")
for _ in range(30):
    shell("am", "kill", pkg)
    if not shell("pidof", pkg, check=False).strip():
        break
    time.sleep(.3)
else:
    raise AssertionError("The original Whip process did not die")
shell("am", "task", "focus", task_id)
recovered, _ = find("text", edited)
new_pid = shell("pidof", pkg).strip()
assert new_pid and new_pid != old_pid
assert "Review New Task Request?" not in recovered
capture("recovered-unsaved-draft", recovered)
assert query(f"SELECT COUNT(*) FROM tasks WHERE title='{edited}';") == "0"
tap("text", "Save")
for _ in range(25):
    saved_count = query(f"SELECT COUNT(*) FROM tasks WHERE title='{edited}';")
    if saved_count == "1":
        break
    time.sleep(.4)
else:
    raise AssertionError(("Edited shared Task was not saved exactly once", saved_count))
assert query(f"SELECT COUNT(*) FROM tasks WHERE title='{original}';") == "0"
tap("text", "Inbox")
saved, _ = find("text", edited)
capture("saved-task", saved)
proof = dict(
    serial=serial, taskId=task_id, oldPid=old_pid, newPid=new_pid,
    original=original, edited=edited, unsavedBeforeKill=True,
    unsavedAfterRestore=True, savedCount=1, uneditedCount=0,
)
(out / "proof.json").write_text(json.dumps(proof, indent=2) + "\n")
print(json.dumps(proof))
