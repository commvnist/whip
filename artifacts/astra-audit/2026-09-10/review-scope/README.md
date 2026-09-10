# Review scope and empty-view recovery

FND-20260910-015 / DEC/IMP/VER-20260910-010. Application remains 0.3.66/code 72; no release or data-format change.

Review now says **No Outcomes in This View**, directs users to period/section choices, and explains which completed outcomes contribute. It no longer implies first use when saved progress exists outside the view. Card-opening guidance appears only when outcome cards exist. The existing Track evidence card owns its explanation once.

The native journey records a current Task and a successful Habit log 40 days earlier, archives the Habit, and opens Weekly/Habits. Including Tasks reveals exactly one completion. The selected sections and result survive Activity recreation; returning to Habits alone, recreating, closing and reopening Review preserves the empty selection. This verifies the real repositories/settings/Activity route. The neighboring availability tests use controlled supplied source states and saved-state emulation.

## Evidence

- `before/shared.review.empty-selection.png`: unchanged production at f1c95a6 displays first-use wording and card-opening guidance despite no cards. Reproduction `lS1WSx` fails the expected scoped-title assertion.
- `final-api34/` and `final-api37/`: five personally inspected original PNG/XML pairs each: empty selection, revealed Task result, reopened empty view, Track-only evidence, and ready-empty source recovery. Original filenames/hashes are in each manifest; individual observations and zero unlabeled-node counts are in `review.tsv`.
- The native phone frames retain production theme; API 34's controlled source-recovery fixture has gray system-bar chrome and is not theme acceptance. The scrolled phone result shows the full Task card while the Habit card continues below the viewport. Wide empty descriptions retain the existing long centered line measure; a shared reading-width improvement remains a design follow-up, not a clipping failure.
- `source-and-apk-sha256.json` identifies the exact final production file, both changed tests and installed APKs. PNG CRC/decompression, XML parsing, pair identity and final source/APK equality passed. Text-only whitespace normalization preserves original/retained hashes in `text-normalization.json`; PNG bytes are unchanged.

## Executed checks

```sh
# Baseline production: expected failure, lS1WSx.
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android 'com.whip.app.ReviewJourneyE2ETest#changingReviewOptionsRevealsSavedOutcomesWithoutLosingTheView' --emulator

# Initial implementation: 16 JVM pass; six of seven Android pass, mTQNrk.
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android com.whip.app.ReviewJourneyE2ETest --android com.whip.app.ReviewAvailabilityUiTest --jvm 'com.whip.app.ui.Review*Test' --emulator

# Final API 34: seven fresh methods, zero failures/skips/reuse, L7wMDb.
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android com.whip.app.ReviewJourneyE2ETest --android com.whip.app.ReviewAvailabilityUiTest --emulator

# Final API 37: explicit installation of both exact APKs, then seven methods pass in 47.305 seconds.
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5556 shell am instrument -w -r -e class 'com.whip.app.ReviewJourneyE2ETest,com.whip.app.ReviewAvailabilityUiTest' commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner

scripts/check --ready
scripts/ui-catalog lint
scripts/test-ui-catalog
```

Readiness passes 364 fresh JVM tests in 39 suites with zero failures/errors/skips, Android compilation, lint, debug packaging and static/harness checks. Catalog gates pass with 409 required states, zero pending selectors/exceptions. Source inventory is 639 JVM + 1,057 Android = 1,696; these inventory totals are not an executed whole-product suite. Both emulators use font scale 1.0.

The intermediate native failure was a fixture attempting to scroll the fixed Close button after already completing result recovery and two recreations. The corrected test asserts that the header action is visible and clicks it. No production scrolling fix is claimed. All failed/final logs and native results are retained separately.

After native execution, evidence capture and readiness completed, the long-lived API 34 emulator exited. Its log shows graceful shutdown without identifying the initiator. No campaign was left running; retained evidence and APKs validate. A detached restart did not persist, so a retained foreground emulator session was started; it booted in 14.170 seconds and reports font scale 1.0. `emulator-recovery.log` records installation and launch of the same tested APK after restart. This is environment recovery, not a new product-process-death test.

This increment changes presentation only. Source-card navigation, deeper correlation meaning, wider shared design review and complete app acceptance remain open. There was no API 26, physical-phone, publication or new scaled-resolution campaign in this increment.
