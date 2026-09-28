#!/usr/bin/env python3
"""External synthetic-emulator driver; never targets the owner's phone."""
import json
from datetime import datetime, timezone
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

SERIAL = "emulator-5554"
PACKAGE = "commvne.com.whip.app.debug"
REMOTE = "/storage/emulated/0/whip-debug/ui/focus-permission-current.xml"

def adb(*args):
    return subprocess.check_output(["adb", "-s", SERIAL, *args], text=True).strip()

def hierarchy():
    adb("shell", "mkdir", "-p", "/storage/emulated/0/whip-debug/ui")
    adb("shell", "rm", "-f", REMOTE)
    result = adb("shell", "uiautomator", "dump", REMOTE)
    if "dumped to" not in result:
        raise SystemExit(f"No fresh native hierarchy: {result}")
    return ET.fromstring(adb("exec-out", "cat", REMOTE))

def timer_pair():
    root = ET.fromstring(adb("exec-out", "run-as", PACKAGE, "cat", "shared_prefs/whip-settings.xml"))
    return {node.attrib["name"]: int(node.attrib["value"]) for node in root
            if node.attrib.get("name") in ("focusTimerTaskId", "focusTimerDeadlineMillis")}

action = sys.argv[1]
if action == "dump":
    for node in hierarchy().iter("node"):
        if node.attrib.get("text") or node.attrib.get("content-desc"):
            print(json.dumps({key: node.attrib.get(key, "") for key in
                              ("text", "content-desc", "class", "clickable", "checked", "bounds")}, ensure_ascii=False))
elif action == "tap":
    attr, value = sys.argv[2:4]
    nodes = [node for node in hierarchy().iter("node") if node.attrib.get(attr) == value]
    if len(nodes) != 1:
        raise SystemExit(f"Expected one {attr}={value!r}, got {len(nodes)}")
    values = list(map(int, re.findall(r"\d+", nodes[0].attrib["bounds"])))
    left, top, right, bottom = values
    if right <= left or bottom <= top:
        raise SystemExit("Target has empty visible bounds")
    print(adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2)))
elif action == "pair":
    print(json.dumps(timer_pair(), sort_keys=True))
elif action == "state":
    package_dump = adb("shell", "dumpsys", "package", PACKAGE)
    permission = [line.strip() for line in package_dump.splitlines()
                  if "android.permission.POST_NOTIFICATIONS:" in line]
    pid = subprocess.run(["adb", "-s", SERIAL, "shell", "pidof", PACKAGE],
                         text=True, capture_output=True).stdout.strip()
    keys = [line for line in adb("shell", "cmd", "notification", "list").splitlines()
            if f"|{PACKAGE}|40001|" in line]
    resumed = [line.strip() for line in adb("shell", "dumpsys", "activity", "activities").splitlines()
               if "mResumedActivity" in line or "topResumedActivity=" in line]
    print(json.dumps({"stage": sys.argv[2], "utc": datetime.now(timezone.utc).isoformat(),
                      "target": SERIAL, "timer": timer_pair(), "process_pid": pid,
                      "runtime_permission": permission, "focus_notification_keys": keys,
                      "resumed_activity": resumed,
                      "display": adb("shell", "wm", "size"),
                      "font_scale": adb("shell", "settings", "get", "system", "font_scale")}, sort_keys=True))
else:
    raise SystemExit(f"Unsupported action {action!r}")
