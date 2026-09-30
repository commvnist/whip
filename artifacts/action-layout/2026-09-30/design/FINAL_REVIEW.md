# Design lane final evidence

The same ordinary-UI fixtures now show a consistent leading activity theme. Duration uses filled Start Timer and outlined Enter Duration; check-off keeps its checkbox and readable Check In label; Goal uses filled Log Progress followed by outlined Complete Goal. Status/progress remain above actions, while Edit/disclosure stay in the header. The consultant’s checkbox spacing correction is present in the refined captures.

## Exact refined evidence

Refined APK: `3cc019a94838fa488ab2b07f7a0950e537b7c963de69ab401909367b3cb50024`, frozen at `build/action-layout/2026-09-30/frozen-v2/app-debug.apk`. Implementation reported source manifest `c3086cd7ee4e98734ad477ac12e4a1eb12e0a22d10653d21384e200912d09bd8` and test APK `117f101d0ecaa17fb1fbb1a9517c388c507c0f0e517abccd26ae40b63fca1a37`. This lane independently verified the app hash before installing it in place on `emulator-5554` / `whip_api34`.

| Baseline | Refined final | What it establishes |
| --- | --- | --- |
| before-habit-checkin.png | final-compact-habit-cards.png | Same 0-second Duration / Pending CheckOff records; leading actions and corrected checkbox gap; original viewport restored. |
| before-checkin-inspector.png | final-compact-checkin-inspector.png | Same Habits entry route; Check In footer moves from centered to leading. |
| before-goal-card.png | final-compact-goal-card.png | Same target 100 / no observations / 0% Goal; leading primary Log Progress and secondary Complete Goal; completion remains available before target. |
| before-duration-entry-keyboard.png | final-duration-entry-keyboard.png | Same unsaved value 30 with IME; duration-specific title/input/Log Duration and leading submission. |

Final wide layout at 2400×1600, density 420, font 1.0: `final-habits-wide.png`, `final-goal-wide.png`. Final native 200% text at 1080×2400: `final-habits-large.png`, `final-goal-large.png`, `final-duration-entry-large-keyboard.png`, `final-duration-inspector-large.png`, `final-checkin-inspector-large.png`, `final-goal-inspector-large.png`, `final-goal-completion-large.png`. Labels and primary actions remain readable; secondary card actions wrap from the leading edge. Inspector body content scrolls, and the four-tab Habit section strip shows its horizontal-scroll affordance rather than all tab labels simultaneously.

## Real interactions and limits

- Typed duration 30 → Back hides IME → Cancel opens the existing draft guard → Keep Editing retains 30 (`final-duration-discard.png`, `final-duration-draft-kept.png`) → Cancel / Discard returns to Habits without logging. Repeated at native 200% and discarded.
- Complete Goal at native 200% opens explicit confirmation with the 0% outcome and pre-target warning; Cancel returns to the unchanged Goal. Closure confirmation intentionally retains ordinary confirm/Cancel ordering, distinct from routine logging.
- Opened/closed both Habit inspectors and Goal inspector from their respective collection route; selected domain and status remain stable. No persisted logs, timer starts, check-offs, or closures were saved during this comparison, preserving matching before/after fixture values. Independent QA owns persistence, repeated submission, timer, editing and completion proof.
- Early `after-*` captures belong to the initial APK `b72ef429c7a3e333165f3e41602fc60903d3212c1a156935b2725413cf73deae`. The consultant accepted their Goal/forms/inspector hierarchy and required a checkbox-label gap correction. They are phased evidence, not refined-APK captures. Initial APK provenance remains separate in `frozen/`.
- `final-habit-cards.png`, `final-habit-cards-restored.png` and `final-checkin-inspector.png` captured the refined UI while Android’s temporary taskbar reduced available height after wide resizing. Prefer the final-compact files above for the original viewport comparison. The taskbar disappeared after font configuration; no platform debugging or data reset was needed.

Original emulator settings restored and verified: physical 1080×2400, density 420, font_scale 1.0. No uninstall, app data clear, global adb reset, phone action, source edit, commit or push was performed by this lane. Actual TalkBack speech and physical folding hardware remain outside this evidence. The optional Codex memory lookup was rejected by automatic review and abandoned; it did not block the app review.
