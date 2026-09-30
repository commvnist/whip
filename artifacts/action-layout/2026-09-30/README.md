# Habit and Goal action layout

**Independent consultant acceptance: accepted**, including corrected checkbox spacing. Logging/check-in controls now lead after status/progress, with filled primary and outlined secondary actions. Start/Stop & Log, Enter Duration/Log Duration, and Complete Goal retain distinct meanings. Corrections retain Save Changes; numeric replacement retains Set Total.

## Before and refined after

Original emulator captures show the same unchanged fixtures. Compact cards and inspector use the original 1080x2400 / density 420 / font 1.0 viewport.

| Flow | Before | Refined after |
| --- | --- | --- |
| Duration and Check In | [Before](design/before-habit-checkin.png) | [After](design/final-compact-habit-cards.png) |
| Goal log/completion at 0% | [Before](design/before-goal-card.png) | [After](design/final-compact-goal-card.png) |
| Check-in inspector | [Before](design/before-checkin-inspector.png) | [After](design/final-compact-checkin-inspector.png) |
| Duration entry, unsaved 30 with IME | [Before](design/before-duration-entry-keyboard.png) | [After](design/final-duration-entry-keyboard.png) |

Wide: [Habits](design/final-habits-wide.png), [Goals](design/final-goal-wide.png). Native 200%: [Goal actions](design/final-goal-large.png), [duration with keyboard](design/final-duration-entry-large-keyboard.png), [timer review with keyboard](implementation/native-v2/ux-upgrades.habits-timer-review.large.png), [completion confirmation](design/final-goal-completion-large.png). The compact duration IME capture includes Android's transient taskbar after resizing; original-height compact cards were refreshed separately.

## Verification and critique

[Consultant report](../../../docs/quality/ACTION_LAYOUT_2026-09-30.md) records required corrections and final acceptance. [Design evidence](design/FINAL_REVIEW.md), [implementation validation](implementation/README.md), and [independent QA](qa/README.md) retain focused passed/failed/not-run evidence and exact APK provenance.

41 JVM methods passed once; final compile/package/lint passed (0 errors). 18 successful native executions / 16 unique methods cover compact/wide, native 200%, keyboard/drafts, repeated logging, timer recovery, correction and completion. Results are phased across identified app/test APKs, with three original fixture-failed attempts retained. Both corrected wide journeys passed; every valid earlier pass was retained. No full-suite rerun, destructive fixture, phone installation or public release. All focused checks and consultant acceptance are complete; source delivery is the cohesive commit containing this index.

Refined app SHA256: 3cc019a94838fa488ab2b07f7a0950e537b7c963de69ab401909367b3cb50024. APK binaries and rejected/diagnostic baseline captures are excluded from the source commit. Prior user receipt work remains unstaged.

Final test-v3 SHA256: a8dfe2db7ebda2036b9acf0338279cfe85e7f1c2476fca10d6cc6980bea1ec55. Its only change from test-v2 narrows the wide-list fixture selector; production bytes are unchanged.
