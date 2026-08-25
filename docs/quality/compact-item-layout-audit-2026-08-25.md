# Compact Item Layout Parity Audit — 2026-08-25

## Outcome

Compact item layout is a first-class presentation of the same Tasks, Habits, Goals, and Tracks. Its collapsed state is a scannable list row, while its inline expanded state exposes the complete information and control set from the standard collection card. No functionality requires leaving the collection.

The initial implementation violated that contract by permanently suppressing secondary card content. The first parity repair made everything visible but could not achieve genuine list density. This pass combines both requirements through coordinated, responsive disclosure.

## Display contract

- The domain state and available operations are identical in standard and compact display.
- A collapsed row contains identity, a title of up to two lines, a concise status, the domain's primary action, and a dedicated disclosure action. Ordinary rows target roughly 56–72 dp and must remain materially shorter than standard cards.
- The primary action never toggles disclosure. Completion, logging, reset, timer, rating, numeric increment, and Track entry creation remain one tap away.
- Expansion happens inline and reveals Area, supporting metadata, Edit, notes, progress, histories, provenance, and every secondary action or child item.
- At most one compact row is expanded across the app. The selected row is saveable across navigation and configuration recreation.
- An active duration Habit or elapsed Goal may expand automatically only when no row is already selected. Automatic expansion never replaces the user's current disclosure.
- At narrow widths or 150%+ text, expanded metadata and actions stack. Wider rows use columns. Titles retain two lines and all action targets remain at least 48 dp.
- Detail-card navigation remains available, but is not a substitute for an inline action that standard mode exposes.
- Standard card behavior and hierarchy remain unchanged.

## Defect inventory and resolution

| Surface | Initial compact loss | Resolution |
| --- | --- | --- |
| Shared header | Title and metadata were squeezed between identity, primary action, and edit lanes; titles were often forced to one line; Area disappeared. | Collapsed rows reserve stable lanes for identity, a two-line title/status, primary action, and disclosure. Expansion reflows Area/supporting metadata and Edit below the divider. |
| Task | Notes and subtask progress disappeared. | The collapsed row keeps completion and a schedule/progress summary. Expansion restores notes, schedule/deadline context, Area, Edit, and subtask progress. |
| Habit | Skip recovery, numeric controls, checklist items, checklist progress, flexible-schedule progress, value/target progress, raw values, and Health Connect provenance disappeared. | The collapsed row keeps the correct primary control and state-specific summary. Expansion restores checklist items, progress, quick/decrement/set/undo actions, timer details, skip recovery, targets, values, and provenance. |
| Goal | Progress, current/consistency value, pace, forecast, elapsed timer, milestones, rewards, and description disappeared. | The collapsed row keeps Log or Reset plus a type-specific status. Expansion restores progress, current/consistency, pace, forecast, timer detail, milestones, rewards, description, and Edit. |
| Track | Latest entry and Add Entry disappeared; long titles were forced to one line. | The collapsed row keeps a two-line title, Area/count summary, and Add Entry. Expansion restores Latest and 48 dp Add/Edit actions. Selection and reorder modes preserve their existing interaction model. |

## State and interaction matrix

| Domain | States audited in compact mode | Inline interaction requirement | Automated evidence |
| --- | --- | --- | --- |
| Task | ordinary; notes; Area; schedule/deadline badge; completion; selection; subtask fraction/percent | Complete/select and Edit remain; card still opens details | `ProductivityCardDesignUiTest` compact Task and 200% text cases |
| Habit | check-off; count/decimal; duration; checklist; rating/log; skipped; synced; flexible target; completed/pending | parent completion; every checklist item; quick values; decrement; Set; Undo; Start/Stop; Rate/Log; Undo Skip; Edit | `ProductivityCardDesignUiTest` numeric, checklist, and state-matrix cases; `HabitRepositoryTest` auto-complete-on/off cases |
| Goal | measured progress; current value; consistency; pace/forecast; elapsed; weighted milestones; description; active/non-active | Log; Reset; milestone toggle; Edit | `ProductivityCardDesignUiTest` measured, elapsed, and milestone cases |
| Track | empty history; latest entry; archived; selection/reorder; adaptive master pane; user density; long title | Add Entry; Edit; select/reorder; card open | `TrackWorkspaceUiTest` compact parity case and 200% RTL workspace case |
| Cross-cutting | narrow 340 dp content; 200% font; dark/light theme; disclosure replacement; state restoration; persisted setting; backup/restore | one expanded item; independent primary action; 48 dp actions; responsive stack; readable title lane | `CompactItemExpansionStateTest`; `ProductivityCardDesignUiTest`; `SettingsBehaviorUiTest`; `AppSettingsPersistenceTest`; `BackupRepositoryTest` |

## Design decisions

Collapsed compact cards do not have a universal fixed height, but ordinary rows are list-sized and materially shorter than standard cards. A three-item medication checklist is initially as compact as another Habit; expanding it reveals three independently usable 48 dp rows and its parent auto-completion policy remains unchanged.

The setting description explicitly distinguishes the always-available primary action from the complete information and controls available through expansion. This makes the user-facing promise match the enforced display contract.

## Release verification

Completed automated evidence:

- The focused compact/card matrix passed ten Android UI tests plus two fast state-controller tests on the disposable API 34 emulator.
- `scripts/check --full` passed, including 254 JVM tests, lint, coverage thresholds, minified release APK/AAB, and benchmark build.
- `ANDROID_SERIAL=emulator-5554 scripts/check --emulator` passed all 305 Android instrumentation tests across seven isolated batches.
- Total product regression baseline: 559 tests.
- The repository release script installed signed `0.3.9 (15)` with a data-preserving package upgrade on `192.168.2.187:35089`; the local and installed base APK SHA-256 both equal `4f377dce45ac1c09103f529720f20f42efa2e9901293a9a35914b1e66096865c`.
- The phone retained its original `firstInstallTime` of `2026-08-22 21:32:46`. The app completed a 104 ms cold launch and became the resumed foreground activity. The post-launch process log contained no Android runtime exception or fatal application error; two device-vendor library/service diagnostics were unrelated to Whip.

Physical-device instrumentation and destructive data operations were not used.
