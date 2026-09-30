#!/usr/bin/env python3
"""Run only the two affected sizing methods at the verified 360dp phone width."""
import json
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path('/root/repos/whip')
BASE = ROOT / 'artifacts/action-sizing/2026-09-30/qa'
EVIDENCE = BASE / 'phone-content-height'
EVIDENCE.mkdir(exist_ok=False)
SERIAL = 'emulator-5558'

def adb(*args):
    return subprocess.run(['adb', '-s', SERIAL, 'shell', *args], check=True,
                          capture_output=True, text=True).stdout.strip()

subprocess.run([str(ROOT / 'scripts/android-target-guard'), 'instrumentation'], check=True,
               env={**os.environ, 'ANDROID_SERIAL': SERIAL}, cwd=ROOT)
avd = subprocess.run(['adb', '-s', SERIAL, 'emu', 'avd', 'name'], check=True,
                     capture_output=True, text=True).stdout.splitlines()[0]
assert avd == 'whip_api34_qa3'
assert 'Override' not in adb('wm', 'size') and 'Override' not in adb('wm', 'density')
initial = {'serial': SERIAL, 'avd': avd, 'size': adb('wm', 'size'), 'density': adb('wm', 'density'),
           'font_scale': adb('settings', 'get', 'system', 'font_scale')}
(EVIDENCE / 'initial-device.json').write_text(json.dumps(initial, indent=2) + '\n')
codes = []
try:
    adb('wm', 'size', '1080x2520')
    adb('wm', 'density', '480')
    for method, run_id in (
        ('relatedActivityButtonsShareSizeAndKeepFullLabels', 'phone-content-normal'),
        ('largeTextActivityButtonsShareSizeAndKeepFullLabels', 'phone-content-large'),
    ):
        result = subprocess.run(['python3', str(BASE / 'qa-native.py'),
                                 'com.whip.app.ui.ActivityActionLayoutUiTest#' + method, run_id], cwd=ROOT)
        codes.append(result.returncode)
        if result.returncode:
            break
finally:
    adb('wm', 'size', 'reset')
    adb('wm', 'density', 'reset')
    restored = {'serial': SERIAL, 'avd': avd, 'size': adb('wm', 'size'), 'density': adb('wm', 'density'),
                'font_scale': adb('settings', 'get', 'system', 'font_scale'), 'run_exit_codes': codes}
    (EVIDENCE / 'restored-device.json').write_text(json.dumps(restored, indent=2) + '\n')
    print(json.dumps(restored), flush=True)
sys.exit(0 if codes == [0, 0] else 1)
