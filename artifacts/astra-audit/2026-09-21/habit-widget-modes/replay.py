"""Exercise pinned Count and Timer Habit widget actions through process death.

Run on a disposable API 34 emulator with current debug Whip, completed Setup,
and a real Habit Tracking widget already pinned in Pixel Launcher. The script
adds two uniquely named synthetic Habits without clearing app data.
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
        result = shell("uiautomator", "dump", "/sdcard/habit-widget-modes-audit.xml")
        if "dumped to" in result:
            xml = shell("cat", "/sdcard/habit-widget-modes-audit.xml")
            return xml, list(ET.fromstring(xml).iter("node"))
        time.sleep(.3)
    raise AssertionError("No settled Android hierarchy")


def find(attribute, value, substring=False, attempts=20):
    for _ in range(attempts):
        xml, nodes = hierarchy()
        matches = [
            node for node in nodes
            if (value in node.get(attribute, "") if substring else node.get(attribute) == value)
        ]
        if matches:
            return xml, matches[0]
        time.sleep(.3)
    (out / "unexpected.xml").write_text(xml)
    raise AssertionError((attribute, value))


def tap(attribute, value, substring=False):
    _, node = find(attribute, value, substring)
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


def wait_for(sql, predicate, description):
    for _ in range(30):
        value = query(sql)
        if predicate(value):
            return value
        time.sleep(.4)
    raise AssertionError((description, value))


assert any(shell("getprop", key).strip() == "1" for key in ("ro.boot.qemu", "ro.kernel.qemu"))
assert shell("getprop", "ro.build.version.sdk").strip() == "34"
assert shell("pm", "path", pkg).strip()
assert "value=\"true\"" in shell("run-as", pkg, "cat", "shared_prefs/whip-settings.xml")
assert "com.whip.app.widget.HabitTrackingWidgetProvider" in shell("dumpsys", "appwidget")
stamp = str(int(time.time()))
count_title = "Count widget audit " + stamp
timer_title = "Timer widget audit " + stamp


def create_habit(title, choice, expected_mode):
    assert query(f"SELECT COUNT(*) FROM habits WHERE name='{title}';") == "0"
    shell("input", "keyevent", "3")
    find("content-desc", "Add habit to All areas")
    tap("content-desc", "Add habit to All areas")
    find("text", "Create Habit")
    tap("class", "android.widget.EditText")
    shell("input", "text", title.replace(" ", "%s"))
    find("text", title)
    shell("input", "keyevent", "4")
    tap("text", choice)
    tap("text", "Save")
    row = wait_for(
        f"SELECT id,trackingMode,scheduleType,uuid FROM habits WHERE name='{title}';",
        bool,
        "Habit was not saved",
    )
    habit_id, mode, schedule, uuid = row.split("|")
    assert (mode, schedule) == (expected_mode, "Daily")
    return habit_id, uuid


count_id, _ = create_habit(count_title, "Count", "Count")
timer_id, _ = create_habit(timer_title, "Timer", "Duration")
assert query(f"SELECT COUNT(*) FROM habit_logs WHERE habitId IN ({count_id},{timer_id});") == "0"
shell("input", "keyevent", "3")
before, _ = find("content-desc", "Add 1 to " + count_title)
find("content-desc", "Start " + timer_title)
capture("count-and-timer-before-death", before)
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
survived, _ = find("content-desc", "Add 1 to " + count_title)
find("content-desc", "Start " + timer_title)
capture("count-and-timer-after-death", survived)

for expected in (1, 2):
    tap("content-desc", "Add 1 to " + count_title)
    wait_for(
        f"SELECT COUNT(*) FROM habit_logs WHERE habitId={count_id} AND value=1.0;",
        lambda value: value == str(expected),
        "Count widget did not record one increment",
    )
counted, _ = find("content-desc", "Add 1 to " + count_title)
capture("count-twice", counted)

tap("content-desc", "Start " + timer_title)
timer_state = wait_for(
    f"SELECT timerStartedAtMillis,timerSessionId FROM habits WHERE id={timer_id};",
    lambda value: len(value.split("|")) == 2 and all(value.split("|")),
    "Timer widget did not start",
)
running, _ = find("content-desc", "Stop and log " + timer_title, substring=True)
capture("timer-running", running)
time.sleep(2)
shell("am", "start", "-W", "-n", "com.android.settings/.Settings")
for _ in range(30):
    shell("am", "kill", pkg)
    if not shell("pidof", pkg, check=False).strip():
        break
    time.sleep(.3)
else:
    raise AssertionError("Whip timer process was not killed as a cached process")
shell("input", "keyevent", "3")
continued, _ = find("content-desc", "Stop and log " + timer_title, substring=True)
capture("timer-running-after-death", continued)
tap("content-desc", "Stop and log " + timer_title, substring=True)
timer_log = wait_for(
    f"SELECT COUNT(*),COALESCE(SUM(value),0) FROM habit_logs WHERE habitId={timer_id};",
    lambda value: value.split("|")[0] == "1" and float(value.split("|")[1]) > 0,
    "Timer widget did not record elapsed time exactly once",
)
assert query(f"SELECT timerStartedAtMillis,timerSessionId FROM habits WHERE id={timer_id};") == "|"
stopped, _ = find("content-desc", "Start " + timer_title)
capture("timer-stopped", stopped)
new_pid = shell("pidof", pkg).strip()
assert new_pid and new_pid != old_pid
proof = dict(
    serial=serial, oldPid=old_pid, newPid=new_pid,
    countHabitId=count_id, timerHabitId=timer_id,
    countLogs=2, countTotal=2, timerLog=timer_log,
    timerSessionBeforeSecondDeath=timer_state.split("|")[1],
    widgetActionsSurvivedProcessDeath=True,
)
(out / "proof.json").write_text(json.dumps(proof, indent=2) + "\n")
print(json.dumps(proof))
