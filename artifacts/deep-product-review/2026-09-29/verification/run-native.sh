#!/usr/bin/env bash
set -u
export PATH=/root/Android/Sdk/platform-tools:$PATH
export ANDROID_SERIAL=emulator-5554
selector=$1
receipt=$2
printf '%s\n' "$selector" > "$receipt"
test_started_ms=$(date +%s%3N)
scripts/android-target-guard instrumentation >> "$receipt" 2>&1 || exit $?
adb -s emulator-5554 shell am instrument -w -r -e class "$selector" commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner >> "$receipt" 2>&1
test_exit=$?
printf '\nElapsed ms: %s\nCommand exit: %s\n' "$(( $(date +%s%3N) - test_started_ms ))" "$test_exit" >> "$receipt"
tail -12 "$receipt"
test "$test_exit" -eq 0 && rg -Fq 'OK (1 test)' "$receipt"
