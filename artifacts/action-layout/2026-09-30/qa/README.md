# Action layout QA — 2026-09-30

**Passed: 9 exact native executions, 7 unique methods.** Eight executions use the final production APK; the earlier compact Goal pass uses the initial implementation APK. Three fixture-failed attempts are retained separately. No product failure remains in this bounded scope.

All calls targeted API 34 `emulator-5558` / `whip_api34_qa3`. Compact display: 1080×2400, density 420. Wide display: 1920×1200, density 240 (1280×800 dp). Large text uses Android font scale 2.0. Each run retains its actual runner output, installed-package hashes, device identity, result, and any fresh PNG/UI-hierarchy XML captures under `native/<run>`.

| Exact method | Passed configuration and run |
| --- | --- |
| `ActivityActionLayoutUiTest#habitDurationTimerCheckInAndAmountsKeepTheirMeaning` | Compact 1.0: `final-compact-habit`; wide 1.0: `final-wide-habit-replay` |
| `ActivityActionLayoutUiTest#goalLoggingCorrectionAndManualCompletionStaySeparate` | Initial compact 1.0: `compact-goal`; final wide 1.0: `final-wide-goal-replay` |
| `ActivityActionLayoutUiTest#largeTextDurationKeyboardAndCancelPreserveDraft` | Compact 2.0 with actual numeric IME: `final-large-duration` |
| `HabitDirectExecutionUiTest#unavailableRowsHideEntryControlsButKeepTimerRecoveryAndSkipUndo` | Compact 1.0: `final-availability` |
| `HabitDirectExecutionUiTest#collectionAndTodayRunEveryTrackingModeWithoutOpeningDetails` | Compact 1.0: `final-direct-normal` |
| `HabitDirectExecutionUiTest#collectionAndTodayKeepDirectInputsReachableAtLargeText` | Compact 2.0: `final-direct-large` |
| `HabitDirectExecutionUiTest#extremeIncrementKeepsItsFullAmountVisibleAndItsPrimaryActionReadable` | Compact 2.0: `final-extreme-large` |

The journeys verify saved additive duration (5 + 30 = 35), duplicate-submit prevention, timer recreation/stop, quantity addition and total replacement, check-in/undo persistence, drafts/Cancel/Back, and Goal logging, correction, below-target manual completion, and completed History/navigation. Guard coverage preserves running-timer recovery while hiding manual duration for paused, archived, and unscheduled habits. Footer/touch-target assertions supplement real interactions and captures.

## Build provenance

- Final production APK SHA-256: `3cc019a94838fa488ab2b07f7a0950e537b7c963de69ab401909367b3cb50024`.
- Six final compact/large calls used test APK `117f101d0ecaa17fb1fbb1a9517c388c507c0f0e517abccd26ae40b63fca1a37`.
- Both corrected wide calls used test APK `a8dfe2db7ebda2036b9acf0338279cfe85e7f1c2476fca10d6cc6980bea1ec55`; test-only packaging passed in 3 seconds ([log](test-v3-package.log)). Production bytes stayed unchanged.
- Final new test source SHA-256: `9ecd53fb7657d6a17418e2433ebf650ff477399fb62e0fdb17b8b849961e63cc` ([source receipt](final-test-source-sha256.txt), [APK receipt](final-apk-sha256.txt)).
- Earlier compact Goal used production APK `b72ef429c7a3e333165f3e41602fc60903d3212c1a156935b2725413cf73deae` and test APK `5528df63f6aa53e81cb16f3b3b4ed5ff56ca1bbf348a1d06781b38a5fb2c2ce1`.

## Retained failures and evidence limits

`compact-habit` failed when the fixture attempted to locate a virtualized off-screen check-in card. `final-wide-habit` and `final-wide-goal` failed before product assertions because their generic lazy-list selector matched both support and workspace lists. The smallest scroll-helper corrections resolved all three; original failed output/results remain intact. Corrected wide Habit and Goal passed in 43.165 and 43.064 seconds.

Accepted baseline duration evidence: [card](baseline/duration-card.png) and [entry](baseline/enter-duration.png). Final examples: [duration card](native/final-compact-habit/action-layout.qa.duration-ready.normal%20%281%29.png), [200% duration with keyboard](native/final-large-duration/action-layout.qa.duration-keyboard.large.png), [wide Goal actions](native/final-wide-goal-replay/action-layout.qa.goal-ready.normal%20%281%29.png), and [wide Goal completed History](native/final-wide-goal-replay/action-layout.qa.goal-completed-history.normal%20%281%29.png). The baseline `goal-card.png` shows an unsaved draft and is excluded; baseline `enter-duration-keyboard.png` does not prove IME visibility. Captures named `check-in-saved` show a Pending/Check In transition, so they are not checked-state visual proof; saved check-in and undo are verified by persistence assertions. The design lane supplies matching Pending check-in action and inspector pairs; it does not claim a checked-state screenshot.

Not run: the full instrumentation suite, the existing database-clearing collection-dialog method, TalkBack, additional OS/device/locales. The implementation lane's separate two timer-review passes are recorded in [its receipt](../implementation/native-v2/receipt.json), excluded from the nine QA executions above.

[Restoration receipt](restored-device.json): physical 1080×2400, density 420, font scale 1.0, both replay exit codes 0. The assigned emulator remains booted. No data clearing, uninstall, global ADB reset, or phone action occurred.
