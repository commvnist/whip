# Compact Item Layout Parity Audit — 2026-08-25

> Superseded on 2026-09-03 by `DEC-20260903-014`: Whip now uses one balanced summary-first collection layout and no longer exposes or persists a compact/comfortable choice. The parity findings below remain historical evidence for the disclosure contract.

## Outcome

Compact item layout is a first-class presentation of the same Tasks, Habits, Goals, and Tracks. Its collapsed state is a scannable list row, while its inline expanded state exposes the complete information and control set from the standard collection card. No functionality requires leaving the collection.

The initial implementation violated that contract by permanently suppressing secondary card content. The first parity repair made everything visible but could not achieve genuine list density. This pass combines both requirements through coordinated, responsive disclosure.

## Display contract

- The domain state and available operations are identical in standard and compact display.
- A collapsed row contains identity, a title of up to two lines, a concise status, the domain's primary action, and a dedicated disclosure action. Ordinary rows target roughly 56–72 dp and must remain materially shorter than standard cards.
- The primary action never toggles disclosure. Completion, logging, reset, timer, rating, numeric increment, and Track entry creation remain one tap away.
- Expansion happens inline and reveals Area, supporting metadata, Edit, notes, progress, histories, provenance, and every secondary action or child item.
- Any number of compact rows can be expanded together so users can compare or operate on multiple items without repeatedly closing prior rows. The expanded set is saveable across configuration recreation.
- Switching a primary app tab, collection workspace tab, or Task planning/history sub-tab collapses the expanded set. This gives every destination a clean list state instead of carrying unrelated disclosure state across tabs.
- Every compact row begins collapsed, including active duration Habits and elapsed-since Goals. Their primary Stop or Reset action and current status remain available in the collapsed row, so automatic expansion is unnecessary.
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

## Follow-up visual audit from the Fold photographs

The supplied open-Fold photographs exposed five related presentation defects that the initial parity pass did not catch:

- The shared compact primary-action lane was 56 dp wide. Material text-button padding left too little text width, so the elapsed-goal action rendered as `Rese` / `t`. Compact text actions now use a reusable single-line button primitive with reduced horizontal padding, a minimum 48 dp target, and label-aware lanes; Reset receives 80 dp. Default and 200% font-scale tests assert that its label remains one line.
- Completed Check Off Habits fell through target formatting and displayed `1/1`, even though their subtitle already described completion. Their collapsed status is now semantic (`Done · 1 day streak`) rather than a storage value.
- Expanded Checklist and Check Off Habits fell through raw-value or target-progress rendering, exposing the internal completion sentinel as a bare `1` or a redundant `1 / 1` block. Binary and checklist modes are now excluded from both numeric branches. Rating and Log Only values remain available with explicit labels.
- The Fold support pane synthesized a percentage for every Goal, causing elapsed Goals to say `0% progress` beside the correct `2 days` or `9 hours` status. Compact rows, adaptive context, and support panes now share one type-aware Goal status formatter.
- In stacked expanded rows, Edit floated near the center of a full-width button. Its content is now leading-aligned while the full 48 dp target remains available.

The live signed-release follow-up on the connected narrow physical display confirmed one-line Reset actions, `Done · 1 day streak` for Creatine, `3/3 items · 1 day streak` for Medication, all three checklist controls, leading-aligned Edit, and no standalone `1`. The repository-owned captures and hierarchy dumps are under `/storage/emulated/0/whip-debug/{screenshots,ui}` with the `compact-*-followup` names. Open-Fold companion-pane consistency is covered by `AdaptiveWhipScreenTest`; normal and 200% Reset sizing plus both binary-value leak paths are covered by `ProductivityCardDesignUiTest`.

## State and interaction matrix

| Domain | States audited in compact mode | Inline interaction requirement | Automated evidence |
| --- | --- | --- | --- |
| Task | ordinary; notes; Area; schedule/deadline badge; completion; selection; subtask fraction/percent | Complete/select and Edit remain; card still opens details | `ProductivityCardDesignUiTest` compact Task and 200% text cases |
| Habit | check-off; count/decimal; duration; checklist; rating/log; skipped; synced; flexible target; completed/pending | parent completion; every checklist item; quick values; decrement; Set; Undo; Start/Stop; Rate/Log; Undo Skip; Edit | `ProductivityCardDesignUiTest` numeric, checklist, and state-matrix cases; `HabitRepositoryTest` auto-complete-on/off cases |
| Goal | measured progress; current value; consistency; pace/forecast; elapsed; weighted milestones; description; active/non-active | Log; Reset; milestone toggle; Edit | `ProductivityCardDesignUiTest` measured, elapsed, and milestone cases |
| Track | empty history; latest entry; archived; selection/reorder; adaptive master pane; user density; long title | Add Entry; Edit; select/reorder; card open | `TrackWorkspaceUiTest` compact parity case and 200% RTL workspace case |
| Cross-cutting | narrow 340 dp content; 200% font; dark/light theme; multiple disclosures; tab reset; state restoration; persisted setting; backup/restore | independently expanded items; clean state after tab changes; independent primary action; 48 dp actions; responsive stack; readable title lane | `ItemDisclosureStateTest`; `ProductivityCardDesignUiTest`; `SettingsBehaviorUiTest`; `AppSettingsPersistenceTest`; `BackupRepositoryTest` |

## Design decisions

Collapsed compact cards do not have a universal fixed height, but ordinary rows are list-sized and materially shorter than standard cards. A three-item medication checklist is initially as compact as another Habit; expanding it reveals three independently usable 48 dp rows and its parent auto-completion policy remains unchanged.

The setting description explicitly distinguishes the always-available primary action from the complete information and controls available through expansion. This makes the user-facing promise match the enforced display contract.

## Release verification

Completed automated evidence:

- Focused state and Compose regressions passed on the disposable API 34 emulator, including simultaneous Task/Habit/Goal disclosures, workspace-tab collapse, saved-state restoration, normal and 200% Reset sizing, checklist and Check Off sentinel suppression, and open-Fold support-pane parity.
- `scripts/check --full` passed, including 257 JVM tests, lint, coverage thresholds, minified release APK/AAB, and benchmark build.
- `ANDROID_SERIAL=emulator-5554 scripts/check --emulator` passed all 311 Android instrumentation tests across seven isolated batches.
- Total product regression baseline: 568 tests.
- The repository release script installed and cold-launched signed `0.3.9 (15)` on the disposable API 34 emulator. The local and installed base APK SHA-256 both equal `146b44d81f3a1bab34991456eec5f7805ee35dea29cbad485dbead062da34df7`.
- An earlier narrow-display follow-up also installed signed `0.3.9 (15)` with a data-preserving package upgrade on `192.168.2.187:35089`; that prior visual pass completed a 102 ms cold launch without a fatal application error.

Physical-device instrumentation and destructive data operations were not used.
