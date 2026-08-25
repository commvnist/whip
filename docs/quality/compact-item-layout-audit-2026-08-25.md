# Compact Item Layout Parity Audit — 2026-08-25

## Outcome

Compact item layout is a first-class presentation of the same Tasks, Habits, Goals, and Tracks—not a summary mode. It may reduce padding, spacing, button emphasis, and redundant vertical stacking. It must not remove information, state, or an inline action that is present in the standard collection card.

The initial implementation violated that contract by suppressing secondary card content. This pass restores parity and replaces the narrow shared header lane with an explicit compact reflow.

## Display contract

- The domain state and available operations are identical in standard and compact display.
- Compactness comes from 6 dp vertical card padding, 4 dp internal rhythm, smaller card shape, horizontal progress summaries, and text-button treatment where appropriate.
- Item titles retain two lines. In compact mode, supporting metadata and Area move below the title/action row so neither competes with trailing controls.
- Primary and edit controls retain at least 48 dp interaction targets.
- Detail-card navigation remains available, but is not a substitute for an inline action that standard mode exposes.
- Content-rich rows such as checklists and milestones may remain relatively tall. Density is not allowed to make their controls disappear.
- Standard card behavior and hierarchy remain unchanged.

## Defect inventory and resolution

| Surface | Initial compact loss | Resolution |
| --- | --- | --- |
| Shared header | Title and metadata were squeezed between identity, primary action, and edit lanes; titles were often forced to one line; Area disappeared. | Compact header now uses a two-line title/action row, then full-width Area and supporting metadata. |
| Task | Notes and subtask progress disappeared. | Notes, schedule/deadline context, and subtask progress remain. Progress reflows to a horizontal bar/label pair. |
| Habit | Skip recovery, numeric controls, checklist items, checklist progress, flexible-schedule progress, value/target progress, raw values, and Health Connect provenance disappeared. | All states and controls remain. Dense numeric actions use lower-emphasis text buttons; checklist rows remain 48 dp and row-clickable. |
| Goal | Progress, current/consistency value, pace, forecast, elapsed timer, milestones, rewards, and description disappeared. | All remain. Progress reflows horizontally, elapsed type remains prominent, and milestone rows are 48 dp and row-clickable. |
| Track | Latest entry and Add Entry disappeared; long titles were forced to one line. | Latest and Add remain, long titles retain two lines, and Edit remains 48 dp. |

## State and interaction matrix

| Domain | States audited in compact mode | Inline interaction requirement | Automated evidence |
| --- | --- | --- | --- |
| Task | ordinary; notes; Area; schedule/deadline badge; completion; selection; subtask fraction/percent | Complete/select and Edit remain; card still opens details | `ProductivityCardDesignUiTest` compact Task and 200% text cases |
| Habit | check-off; count/decimal; duration; checklist; rating/log; skipped; synced; flexible target; completed/pending | parent completion; every checklist item; quick values; decrement; Set; Undo; Start/Stop; Rate/Log; Undo Skip; Edit | `ProductivityCardDesignUiTest` numeric, checklist, and state-matrix cases; `HabitRepositoryTest` auto-complete-on/off cases |
| Goal | measured progress; current value; consistency; pace/forecast; elapsed; weighted milestones; description; active/non-active | Log; Reset; milestone toggle; Edit | `ProductivityCardDesignUiTest` measured, elapsed, and milestone cases |
| Track | empty history; latest entry; archived; selection/reorder; adaptive master pane; user density; long title | Add Entry; Edit; select/reorder; card open | `TrackWorkspaceUiTest` compact parity case and 200% RTL workspace case |
| Cross-cutting | narrow 340 dp content; 200% font; dark/light theme; persisted setting; backup/restore | 48 dp actions; readable title lane; setting survives restart and backup | `ProductivityCardDesignUiTest`; `SettingsBehaviorUiTest`; `AppSettingsPersistenceTest`; `BackupRepositoryTest` |

## Design decisions

Compact cards do not have a universal fixed height. A simple task or empty Track becomes materially shorter; a three-item medication checklist stays tall enough to expose three usable controls. This is deliberate: the feature optimizes information density while preserving task completion speed and comprehension.

The setting description now states that information and inline actions remain available. This makes the user-facing promise match the enforced display contract.

## Release verification

Completed automated evidence:

- The focused 11-test compact/card suite passed on the disposable API 34 emulator.
- `scripts/check --full` passed, including 252 JVM tests, lint, coverage thresholds, minified release APK/AAB, and benchmark build.
- `ANDROID_SERIAL=emulator-5554 scripts/check --emulator` passed all 304 Android instrumentation tests across seven isolated batches.
- Total product regression baseline: 556 tests.
- The signed `0.3.9 (15)` APK was installed with `adb install -r` on `192.168.2.187:35089`; the local and installed base APK SHA-256 both equal `088292121fcf47814f91c3236f843fe0b2d68c56e66062251246d379323423b8`.
- The phone retained its original `firstInstallTime` of `2026-08-22 21:32:46`, confirming a data-preserving package upgrade. The app completed a 121 ms cold launch, became the resumed foreground activity, and emitted no process-scoped error logs in the post-launch check.

Physical-device instrumentation and destructive data operations were not used.
