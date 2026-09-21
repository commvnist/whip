"""Replay the live legacy-workout review across real Android process death.

Install current debug and androidTest APKs first. This script resets only the
disposable emulator fixture through an opt-in instrumentation argument.
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


assert any(shell("getprop", key).strip() == "1" for key in ("ro.boot.qemu", "ro.kernel.qemu"))
assert shell("getprop", "ro.build.version.sdk").strip() == "34"
shell("am", "force-stop", pkg)
seed = shell(
    "am", "instrument", "-w", "-e", "class",
    "com.whip.app.GymLegacyRetirementJourneyE2ETest#interruptedLegacyWorkoutRetainsReviewThenConvertsOnlyFutureTemplate",
    "-e", "whipLegacyReviewSeedOnly", "true",
    pkg + ".test/androidx.test.runner.AndroidJUnitRunner",
)
(out / "seed.log").write_text(seed)
assert "OK (1 test)" in seed, seed
fixture = dict(line.split("=", 1) for line in shell(
    "run-as", pkg, "cat", "files/legacy-review-process-fixture.txt",
).splitlines())
routine_id, session_id = int(fixture["routineId"]), int(fixture["sessionId"])
(out / "fixture.json").write_text(json.dumps(fixture, indent=2) + "\n")
assert "value=\"true\"" in shell("run-as", pkg, "cat", "shared_prefs/whip-settings.xml")
launch = shell(
    "am", "start", "-W", "-f", "0x10200000", "-n", pkg + "/com.whip.app.MainActivity",
    "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER",
)
(out / "launch.log").write_text(launch)


def hierarchy():
    for _ in range(10):
        result = shell("uiautomator", "dump", "/sdcard/legacy-review-driver.xml")
        if "dumped to" in result:
            xml = shell("cat", "/sdcard/legacy-review-driver.xml")
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
    assert x2 > x1 and y2 > y1
    shell("input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def capture(name, xml):
    (out / (name + ".xml")).write_text(xml)
    image = subprocess.check_output(adb + ["exec-out", "screencap", "-p"])
    (out / (name + ".png")).write_bytes(image)


def query(sql):
    command = f"run-as {pkg} sqlite3 databases/whip.db {shlex.quote(sql)}"
    return shell(command).strip()


tap("content-desc", "Gym tab")
find("text", "Existing strength plan")
shell("input", "swipe", "540", "1800", "540", "650", "450")
tap("text", "Finish")
before, _ = find("text", "Apply Decisions & Finish")
find("text", "Standard +2.5")
capture("before-process-death", before)
assert query(f"SELECT programKind FROM gym_routines WHERE id={routine_id};") == "FiveThreeOne"
activities = shell("dumpsys", "activity", "activities")
task_id = re.search(re.escape(pkg) + r"/com\.whip\.app\.MainActivity t(\d+)", activities)[1]
old_pid = shell("pidof", pkg).strip()
assert old_pid.isdecimal()
shell("input", "keyevent", "3")  # Home saves the Activity task state before a system kill.
for _ in range(20):
    stopped = shell("dumpsys", "activity", "activities")
    if "state=STOPPED" in stopped and f" t{task_id}" in stopped:
        break
    time.sleep(.3)
else:
    raise AssertionError("Whip did not reach the stopped task state")
(out / "background.txt").write_text(stopped)
# Move another real Activity to the foreground so Android can treat Whip as a
# cached background process. ActivityManager's managed kill preserves its task
# state; a raw SIGKILL here can be treated as a crash and discard the task.
shell("am", "start", "-W", "-n", "com.android.settings/.Settings")
for _ in range(30):
    shell("am", "kill", pkg)
    if not shell("pidof", pkg, check=False).strip():
        break
    time.sleep(.3)
else:
    raise AssertionError("The original Whip process did not die")
shell("am", "task", "focus", task_id)
recovered, _ = find("text", "Apply Decisions & Finish")
find("text", "Standard +2.5")
new_pid = shell("pidof", pkg).strip()
assert new_pid and new_pid != old_pid
capture("recovered-review", recovered)
assert query(f"SELECT programKind FROM gym_routines WHERE id={routine_id};") == "FiveThreeOne"
tap("text", "Apply Decisions & Finish")
for _ in range(25):
    row = query(
        "SELECT s.state,r.programKind,s.sourceRoutineProgramKind,s.programProgressAdvanced "
        "FROM workout_sessions s JOIN gym_routines r ON r.id=s.sourceRoutineId "
        f"WHERE s.id={session_id};",
    )
    if row == "Finished|Custom|FiveThreeOne|1":
        break
    time.sleep(.4)
else:
    raise AssertionError(("Legacy completion/retirement did not settle", row))
performed = query(
    "SELECT COUNT(*) FROM workout_sets ws "
    "JOIN workout_exercises we ON we.id=ws.workoutExerciseId "
    f"WHERE we.sessionId={session_id} AND ws.classification='TrainingMaxTest' AND ws.completed=1;",
)
assert performed == "1", performed
finished, _ = hierarchy()
capture("finished-generic-future", finished)
tap("text", "Start from Routine")
generic, _ = find("text", "Phased Routine · Cycle 2 · Test · Next · Bench")
capture("generic-routine-next", generic)
tap("text", "Start Next · Bench")
for _ in range(20):
    next_row = query(
        "SELECT id,state,sourceRoutineProgramKind FROM workout_sessions "
        f"WHERE sourceRoutineId={routine_id} ORDER BY id DESC LIMIT 1;",
    )
    if next_row.endswith("|Active|Custom") and int(next_row.split("|", 1)[0]) != session_id:
        break
    time.sleep(.4)
else:
    raise AssertionError(("The next workout did not start as a generic Routine", next_row))
next_workout, _ = hierarchy()
capture("next-generic-workout", next_workout)
proof = dict(
    serial=serial, taskId=task_id, oldPid=old_pid, newPid=new_pid,
    routineId=routine_id, sessionId=session_id,
    sessionAndRoutine=row, completedLegacySets=int(performed),
    reviewRestored=True, nextWorkout=next_row,
)
(out / "proof.json").write_text(json.dumps(proof, indent=2) + "\n")
print(json.dumps(proof))
