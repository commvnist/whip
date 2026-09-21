"""Pinned Habit Tracking Add/check-off through an Android process kill.

Requires a disposable API 34 emulator with current debug Whip, completed
Setup, and a real Habit Tracking widget pinned in Pixel Launcher. Adds one
synthetic Habit without clearing other data.
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
        result = shell("uiautomator", "dump", "/sdcard/habit-widget-audit.xml")
        if "dumped to" in result:
            xml = shell("cat", "/sdcard/habit-widget-audit.xml")
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
    assert not inaccessible, ("Habit widget has unlabeled controls", inaccessible)
    (out / (name + ".xml")).write_text(xml)
    (out / (name + ".png")).write_bytes(
        subprocess.check_output(adb + ["exec-out", "screencap", "-p"]),
    )


assert any(shell("getprop", key).strip() == "1" for key in ("ro.boot.qemu", "ro.kernel.qemu"))
assert shell("getprop", "ro.build.version.sdk").strip() == "34"
assert shell("pm", "path", pkg).strip()
assert "value=\"true\"" in shell("run-as", pkg, "cat", "shared_prefs/whip-settings.xml")
assert "com.whip.app.widget.HabitTrackingWidgetProvider" in shell("dumpsys", "appwidget")
title = "Habit launcher audit " + str(int(time.time()))
assert query(f"SELECT COUNT(*) FROM habits WHERE name='{title}';") == "0"
shell("input", "keyevent", "3")
before, _ = find("content-desc", "Add habit to All areas")
capture("pinned-habit-before", before)
tap("content-desc", "Add habit to All areas")
find("text", "Create Habit")
tap("class", "android.widget.EditText")
shell("input", "text", title.replace(" ", "%s"))
find("text", title)
shell("input", "keyevent", "4")
tap("text", "Save")
for _ in range(25):
    row = query(
        "SELECT id,trackingMode,scheduleType FROM habits "
        f"WHERE name='{title}';",
    )
    if row:
        break
    time.sleep(.4)
habit_id, tracking, schedule = row.split("|")
assert (tracking, schedule) == ("CheckOff", "Daily")
assert query(f"SELECT COUNT(*) FROM habit_logs WHERE habitId={habit_id};") == "0"
shell("input", "keyevent", "3")
visible, _ = find("content-desc", "Mark habit " + title + " complete")
capture("pinned-habit-row", visible)
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
survived, _ = find("content-desc", "Mark habit " + title + " complete")
capture("pinned-habit-after-kill", survived)
tap("content-desc", "Mark habit " + title + " complete")
for _ in range(25):
    saved = query(
        "SELECT COUNT(*) FROM habit_logs WHERE "
        f"habitId={habit_id} AND status='Success' AND value=1.0;",
    )
    if saved == "1":
        break
    time.sleep(.4)
else:
    raise AssertionError("Widget check-off did not save one success log")
completed, _ = find("content-desc", "Mark habit " + title + " incomplete")
capture("pinned-habit-completed", completed)
tap("content-desc", "Mark habit " + title + " incomplete")
for _ in range(25):
    remaining = query(f"SELECT COUNT(*) FROM habit_logs WHERE habitId={habit_id};")
    if remaining == "0":
        break
    time.sleep(.4)
else:
    raise AssertionError("Widget reversal did not remove the success log")
reversed_view, _ = find("content-desc", "Mark habit " + title + " complete")
capture("pinned-habit-reversed", reversed_view)
new_pid = shell("pidof", pkg).strip()
assert new_pid and new_pid != old_pid
proof = dict(
    serial=serial, habitId=habit_id, oldPid=old_pid, newPid=new_pid,
    title=title, trackingMode=tracking, scheduleType=schedule,
    successLogsAfterCheckoff=1, successLogsAfterUndo=0,
    visibleBeforeAndAfterKill=True,
    incompleteActionVisibleAfterCheckoff=True, completeActionVisibleAfterUndo=True,
)
(out / "proof.json").write_text(json.dumps(proof, indent=2) + "\n")
print(json.dumps(proof))
