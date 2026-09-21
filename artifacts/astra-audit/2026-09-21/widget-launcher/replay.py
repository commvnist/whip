"""Pinned Task Agenda Add/complete journey across an Android process kill.

Requires a disposable API 34 emulator with the current debug APK, completed
Setup, and one Task Agenda widget pinned on the Pixel Launcher. Adds one
synthetic scheduled Task; does not clear other app or launcher data.
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
        result = shell("uiautomator", "dump", "/sdcard/widget-launcher-audit.xml")
        if "dumped to" in result:
            xml = shell("cat", "/sdcard/widget-launcher-audit.xml")
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
    inaccessible = [
        node.get("resource-id") for node in ET.fromstring(xml).iter("node")
        if node.get("package") == pkg and node.get("NAF") == "true"
    ]
    assert not inaccessible, ("Whip widget has unlabeled controls", inaccessible)
    (out / (name + ".xml")).write_text(xml)
    (out / (name + ".png")).write_bytes(
        subprocess.check_output(adb + ["exec-out", "screencap", "-p"]),
    )


assert any(shell("getprop", key).strip() == "1" for key in ("ro.boot.qemu", "ro.kernel.qemu"))
assert shell("getprop", "ro.build.version.sdk").strip() == "34"
assert shell("pm", "path", pkg).strip()
assert "value=\"true\"" in shell("run-as", pkg, "cat", "shared_prefs/whip-settings.xml")
assert "com.google.android.apps.nexuslauncher" in shell("dumpsys", "appwidget")
assert "com.whip.app.widget.WhipWidgetProvider" in shell("dumpsys", "appwidget")
title = "Widget launcher audit " + str(int(time.time()))
assert query(f"SELECT COUNT(*) FROM tasks WHERE title='{title}';") == "0"
shell("input", "keyevent", "3")
empty, _ = find("content-desc", "Add task to All areas")
capture("pinned-widget-before", empty)
tap("content-desc", "Add task to All areas")
find("text", "Create Task")
shell("input", "text", title.replace(" ", "%s"))
find("text", title)
shell("input", "keyevent", "4")
shell("input", "swipe", "550", "1800", "550", "800", "400")
tap("text", "Scheduled")
tap("text", "Save")
for _ in range(25):
    row = query(
        "SELECT id,scheduleKind,inbox,completedAtMillis FROM tasks "
        f"WHERE title='{title}';",
    )
    if row:
        break
    time.sleep(.4)
task_id, schedule, inbox, completed = row.split("|")
assert (schedule, inbox, completed) == ("Once", "0", "")
shell("input", "keyevent", "3")
visible, _ = find("content-desc", "Complete task " + title)
capture("pinned-widget-row", visible)
old_pid = shell("pidof", pkg).strip()
assert old_pid.isdecimal()
shell("am", "start", "-W", "-n", "com.android.settings/.Settings")
for _ in range(30):
    shell("am", "kill", pkg)
    if not shell("pidof", pkg, check=False).strip():
        break
    time.sleep(.3)
else:
    raise AssertionError("Whip was not killed as a cached process")
shell("input", "keyevent", "3")
survived, _ = find("content-desc", "Complete task " + title)
capture("pinned-widget-after-kill", survived)
tap("content-desc", "Complete task " + title)
for _ in range(25):
    completed = query(f"SELECT completedAtMillis FROM tasks WHERE id={task_id};")
    if completed.isdecimal():
        break
    time.sleep(.4)
else:
    raise AssertionError("Widget click did not complete the Task")
for _ in range(15):
    cleared, nodes = hierarchy()
    if not any(node.get("content-desc") == "Complete task " + title for node in nodes):
        break
    time.sleep(.3)
else:
    raise AssertionError("Completed Task stayed in the widget")
capture("pinned-widget-completed", cleared)
new_pid = shell("pidof", pkg).strip()
assert new_pid and new_pid != old_pid
proof = dict(
    serial=serial, taskId=task_id, oldPid=old_pid, newPid=new_pid,
    title=title, schedule=schedule, inbox=False,
    completedAtMillis=completed, visibleBeforeAndAfterKill=True,
    removedAfterWidgetCompletion=True,
)
(out / "proof.json").write_text(json.dumps(proof, indent=2) + "\n")
print(json.dumps(proof))
