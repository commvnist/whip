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


def find_widget(attribute, value, substring=False):
    for downward in (True, False):
        previous = None
        stationary = 0
        for _ in range(20):
            xml, nodes = hierarchy()
            matches = [
                node for node in nodes
                if (value in node.get(attribute, "") if substring else node.get(attribute) == value)
            ]
            if matches:
                return xml, matches[0]
            widget = next((
                node for node in nodes
                if node.get("resource-id") == f"{pkg}:id/widget_habit_list"
            ), None)
            assert widget is not None, "Pinned Habit Tracking widget is not visible"
            visible = tuple(
                node.get("content-desc") for node in widget.iter("node")
                if node.get("resource-id") == f"{pkg}:id/widget_row_body"
            )
            if visible == previous:
                stationary += 1
                if stationary == 5:
                    break
                time.sleep(.4)
                continue
            previous = visible
            stationary = 0
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", widget.get("bounds")))
            x = (x1 + x2) // 2
            start, end = (y2 - 30, y1 + 30) if downward else (y1 + 30, y2 - 30)
            shell("input", "swipe", str(x), str(start), str(x), str(end), "250")
    (out / "unexpected.xml").write_text(xml)
    raise AssertionError((attribute, value))


def tap(attribute, value, substring=False):
    _, node = find(attribute, value, substring)
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    shell("input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def tap_widget(attribute, value, substring=False):
    _, node = find_widget(attribute, value, substring)
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    shell("input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def widget_meta(xml, action):
    for row in ET.fromstring(xml).iter("node"):
        if row.get("resource-id") != f"{pkg}:id/widget_row":
            continue
        if any(node.get("content-desc", "").startswith(action) for node in row.iter("node")):
            return next(
                node.get("text") for node in row.iter("node")
                if node.get("resource-id") == f"{pkg}:id/widget_row_meta"
            )
    raise AssertionError(("Widget action has no visible row", action))


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
count_action = "Add 1 to " + count_title
start_action = "Start " + timer_title
stop_action = "Stop and log " + timer_title
before, _ = find_widget("content-desc", count_action)
count_before = widget_meta(before, count_action)
capture("count-before-death", before)
find_widget("content-desc", start_action)
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
survived, _ = find_widget("content-desc", count_action)
assert widget_meta(survived, count_action) == count_before
capture("count-after-death", survived)
find_widget("content-desc", start_action)

tap_widget("content-desc", count_action)
count_log = wait_for(
    f"SELECT value,canonicalValue,status FROM habit_logs WHERE habitId={count_id};",
    lambda value: value.split("|") == ["1.0", "1.0", "Recorded"],
    "Count widget did not record exactly one increment",
)
for _ in range(20):
    counted, _ = find_widget("content-desc", count_action)
    count_after = widget_meta(counted, count_action)
    if count_after != count_before:
        break
    time.sleep(.4)
assert count_after != count_before and count_after.split()[0] == "1"
capture("count-once", counted)
after_first_pid = shell("pidof", pkg).strip()
assert after_first_pid.isdecimal() and after_first_pid != old_pid

tap_widget("content-desc", start_action)
timer_state = wait_for(
    f"SELECT timerStartedAtMillis,timerSessionId FROM habits WHERE id={timer_id};",
    lambda value: len(value.split("|")) == 2 and all(value.split("|")),
    "Timer widget did not start",
)
session_id = timer_state.split("|")[1]
running_session = wait_for(
    f"SELECT state,activeHabitId FROM habit_timer_sessions WHERE sessionId='{session_id}';",
    lambda value: value == f"Running|{timer_id}",
    "Timer session was not saved as running",
)
running, _ = find_widget("content-desc", stop_action, substring=True)
assert widget_meta(running, stop_action).endswith(" elapsed")
capture("timer-running", running)
time.sleep(2)
before_second_pid = shell("pidof", pkg).strip()
assert before_second_pid.isdecimal()
shell("am", "start", "-W", "-n", "com.android.settings/.Settings")
for _ in range(30):
    shell("am", "kill", pkg)
    if not shell("pidof", pkg, check=False).strip():
        break
    time.sleep(.3)
else:
    raise AssertionError("Whip timer process was not killed as a cached process")
shell("input", "keyevent", "3")
continued, _ = find_widget("content-desc", stop_action, substring=True)
assert widget_meta(continued, stop_action).endswith(" elapsed")
assert query(f"SELECT state,activeHabitId FROM habit_timer_sessions WHERE sessionId='{session_id}';") == running_session
capture("timer-running-after-death", continued)
tap_widget("content-desc", stop_action, substring=True)
timer_log = wait_for(
    f"SELECT value,canonicalValue,enteredUnitId,status,sourceId FROM habit_logs WHERE habitId={timer_id};",
    lambda value: len(value.split("|")) == 5 and value.split("|")[2:] == [
        "second", "Recorded", "habit-timer-v1:" + session_id,
    ] and float(value.split("|")[0]) > 0 and float(value.split("|")[1]) > 0,
    "Timer widget did not record elapsed time exactly once",
)
assert float(timer_log.split("|")[0]) == float(timer_log.split("|")[1])
assert query(f"SELECT timerStartedAtMillis,timerSessionId FROM habits WHERE id={timer_id};") == "|"
assert query(f"SELECT state,activeHabitId FROM habit_timer_sessions WHERE sessionId='{session_id}';") == "Completed|"
stopped, _ = find_widget("content-desc", start_action)
assert widget_meta(stopped, start_action) == "Start"
capture("timer-stopped", stopped)
new_pid = shell("pidof", pkg).strip()
assert new_pid.isdecimal() and new_pid != before_second_pid
assert query(f"SELECT COUNT(*) FROM habit_logs WHERE habitId={count_id};") == "1"
tap_widget("content-desc", "Open habit " + timer_title)
find("content-desc", "Close Habit details")
find("text", timer_title)
tap("text", "History")
find("text", "Habit History")
history, entry = find("text", "Logged ", substring=True)
assert re.fullmatch(r"Logged \d+ sec", entry.get("text", ""))
assert abs(int(entry.get("text").split()[1]) - float(timer_log.split("|")[1])) <= .5
capture("timer-history", history)
proof = dict(
    serial=serial, oldPid=old_pid, afterFirstKillPid=after_first_pid,
    beforeSecondKillPid=before_second_pid, afterSecondKillPid=new_pid,
    countHabitId=count_id, timerHabitId=timer_id,
    countLog=count_log, countWidgetBefore=count_before, countWidgetAfter=count_after,
    timerLog=timer_log, timerSessionId=session_id,
    timerSessionBeforeAndAfterDeath=running_session,
    timerSessionAfterStop="Completed|", timerHistoryLabel=entry.get("text"),
    widgetActionsSurvivedProcessDeath=True,
)
(out / "proof.json").write_text(json.dumps(proof, indent=2) + "\n")
print(json.dumps(proof))
