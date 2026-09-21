"""Exercise the full external-Task share queue through Android process death.

Run only on a disposable API 34 emulator with the current debug app and setup
complete. The script adds six uniquely named Tasks; it does not clear app data.
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


def hierarchy():
    for _ in range(10):
        result = shell("uiautomator", "dump", "/sdcard/task-share-overflow-audit.xml")
        if "dumped to" in result:
            xml = shell("cat", "/sdcard/task-share-overflow-audit.xml")
            return xml, list(ET.fromstring(xml).iter("node"))
        time.sleep(.3)
    raise AssertionError("No settled Android hierarchy")


def find(attribute, value, attempts=15):
    for _ in range(attempts):
        xml, nodes = hierarchy()
        matches = [node for node in nodes if node.get(attribute) == value]
        if matches:
            return xml, matches[0]
        time.sleep(.3)
    (out / "unexpected.xml").write_text(xml)
    raise AssertionError((attribute, value))


def visible_text(value):
    xml, nodes = hierarchy()
    return xml, any(node.get("text") == value for node in nodes)


def tap(attribute, value):
    _, node = find(attribute, value)
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    shell("input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def capture(name, xml):
    inaccessible = [
        node.get("resource-id") for node in ET.fromstring(xml).iter("node")
        if node.get("package") == pkg and node.get("NAF") == "true"
    ]
    assert not inaccessible, ("Share UI has unlabeled controls", inaccessible)
    (out / (name + ".xml")).write_text(xml)
    (out / (name + ".png")).write_bytes(
        subprocess.check_output(adb + ["exec-out", "screencap", "-p"]),
    )


assert any(shell("getprop", key).strip() == "1" for key in ("ro.boot.qemu", "ro.kernel.qemu"))
assert shell("getprop", "ro.build.version.sdk").strip() == "34"
assert shell("pm", "path", pkg).strip()
assert "value=\"true\"" in shell("run-as", pkg, "cat", "shared_prefs/whip-settings.xml")
stamp = str(int(time.time()))
titles = [f"Overflow audit {stamp} item {index}" for index in range(1, 9)]
for title in titles:
    assert query(f"SELECT COUNT(*) FROM tasks WHERE title='{title}';") == "0"


def share(title, flags=None):
    command = "am start -W "
    if flags:
        command += f"-f {flags} "
    command += (
        "-a android.intent.action.SEND -t text/plain "
        f"--es android.intent.extra.TEXT {shlex.quote(title)} "
        f"-n {pkg}/com.whip.app.MainActivity"
    )
    return shell(command)


shell("am", "force-stop", pkg)
for index, title in enumerate(titles):
    (out / f"launch-{index + 1}.log").write_text(
        share(title, "0x34000000" if index else None),
    )
    if index == 0:
        find("text", title)

before, _ = find("text", "Review New Task Request?")
capture("full-queue-before-death", before)
for title in titles:
    assert query(f"SELECT COUNT(*) FROM tasks WHERE title='{title}';") == "0"
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
recovered, _ = find("text", "Review New Task Request?")
new_pid = shell("pidof", pkg).strip()
assert new_pid and new_pid != old_pid
capture("full-queue-after-death", recovered)

overflow_seen = False
for index, title in enumerate(titles[:6]):
    overflow, shown = visible_text("Share Queue Full")
    if shown:
        assert "2 additional" in overflow, "Rejected-share count was lost"
        capture("overflow-recovered", overflow)
        tap("text", "Got It")
        overflow_seen = True
    if visible_text("Review New Task Request?")[1]:
        tap("text", "Keep Editing")
    current, _ = find("text", title)
    capture(f"accepted-draft-{index + 1}", current)
    tap("text", "Save")
    for _ in range(25):
        if query(f"SELECT COUNT(*) FROM tasks WHERE title='{title}';") == "1":
            break
        time.sleep(.4)
    else:
        raise AssertionError(("Accepted share did not save", title))
    dialog, shown = visible_text("Share Queue Full")
    if shown:
        assert "2 additional" in dialog, "Rejected-share count was lost"
        capture("overflow-recovered", dialog)
        tap("text", "Got It")
        overflow_seen = True

if not overflow_seen:
    dialog, _ = find("text", "Share Queue Full")
    assert "2 additional" in dialog, "Rejected-share count was lost"
    capture("overflow-recovered", dialog)
    tap("text", "Got It")

for title in titles[:6]:
    assert query(f"SELECT COUNT(*) FROM tasks WHERE title='{title}';") == "1"
for title in titles[6:]:
    assert query(f"SELECT COUNT(*) FROM tasks WHERE title='{title}';") == "0"
proof = dict(
    serial=serial, taskId=task_id, oldPid=old_pid, newPid=new_pid,
    acceptedTitles=titles[:6], rejectedTitles=titles[6:],
    acceptedOnce=True, rejectedUnchanged=True, rejectedCountVisible=2,
)
(out / "proof.json").write_text(json.dumps(proof, indent=2) + "\n")
print(json.dumps(proof))
