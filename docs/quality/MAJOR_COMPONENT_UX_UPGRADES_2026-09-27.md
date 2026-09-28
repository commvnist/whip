# Major-component UX upgrades — 2026-09-27

Status: **Implemented and Verified within the accepted scope**. Owner request: FB-20260927-011. Baseline: clean `f85ba9d4` on `main`. Final acceptance: 2026-09-28.

The owner rejected the initial narrow review as insufficient: this must be a thorough review and overhaul plan, not a small issue per area. All three agents paused after preserving initial edits and expanded the audit before resuming. The detailed matrices below cover roughly 100 domain screen/workflow rows plus shared interactions, with source evidence, inspected baseline originals, confirmed findings, design proposals, intentional keeps and reproduction-gated concerns. The initial nine findings remain a subset of this expanded scope.

Three Astra agents with high reasoning reviewed and implemented Home/Tasks and Habits, Goals and Track, and Gym and Settings in parallel. The parent integrated shared controls, reviewed the results and executed checks. Retained screenshots informed the initial design; fresh originals and native journeys verify the implementation. The older paused whole-product audit remains separate.

## Full review and accepted implementation scope

- [Home/Tasks and Habits matrix](UX_OVERHAUL_HOME_HABITS_2026-09-27.md): returning/first-use Home, task collections/capture/filtering/calendar/planning/editors/details/history/bulk actions, Habit modes/targets/timers/schedules/editors/history/analytics and recovery.
- [Goals and Track matrix](UX_OVERHAUL_GOALS_TRACK_2026-09-27.md): lifecycle-aware browse summaries, all Goal types/editors/logging/evidence/history; Track definitions/fields/entries/filtering/analytics/archive/CSV/error states.
- [Gym and Settings matrix](UX_OVERHAUL_GYM_SETTINGS_2026-09-27.md): active training/rest/history/progress/libraries/routines/phases/prescriptions/tools; every Settings category, backup/export/restore, delivery feedback and typed editing.
- [Shared interaction matrix](UX_OVERHAUL_SHARED_2026-09-27.md): shell/navigation, cards/editors/inspectors, choices/units, search, status/recovery, destructive actions and accessibility.

| Workstream (all implemented) | Accepted overhaul | Verification focus; exact executed scope below |
| --- | --- | --- |
| Home recovery (HT-01/02) | Recover hidden tasks; separate no-due Habits, empty Area and first use. | Filter/Area persistence and ordinary/200% native journeys. |
| Habit meaning (HT-05) | Truthful rule/bounds/unit/period/recorded-state labels, flexible streak units and consistent low-pressure copy. | Rule/period presentation tests; zero-rating/note-only/numeric/paused/skipped native cases. |
| Task planning (HT-03) | Date-specific empty states and explicit supported calendar horizon. | End-of-month/empty-date/covered-date native assertions. |
| Task/Habit authoring (HT-04) | Remove duplicated guidance; put core decisions first and optional tuning under meaningful disclosures. | Edit/recreate/save/invalid-field reveal and 200% first-view inspection. |
| Habit Insights (HT-06) | Type-appropriate metrics, visible isolated chart observations, dates/scale, readable activity calendar and direct History route. | Real populated daily/flexible/targetless history, gap/single-point and large-text fixtures. |
| Task filtering (HT-07) | Saved recipes/query/count upfront, advanced narrowing with an explicit summary. | Load/edit/reset/save/reopen with live count and preserved scope. |
| Task history (HT-08) | Actual completion time and clear scheduled-occurrence date. | Delayed completion, recurrence and configured time zone. |
| Goal browse (G3) | Current/target in selected units and explicit paused/terminal state in existing cards. | Pounds/custom units/>100% and frozen closure context. |
| Goal authoring (G4/G5) | Clearable deadline, coherent measurement rules, less repeated prose and stable raw milestone weight drafts. | Decimal input, reorder/recreation, remove/save/reopen and large text. |
| Goal evidence (G1/G2/G6) | Type-specific milestones, leading progress, dated/labelled chart and independently reachable older trend rows. | Irregular dates, >25 observations, snapshot truth and unit/rate regressions. |
| Track browsing/filtering (T1–T4) | Truthful latest/scope labels, local recovery, visible active constraints and editing existing conditions. | Combined filters/stale local IDs/archive/recreation, numeric/date/unit condition editing. |
| Track definition (T5) | Earlier single Add Field action, useful number metadata and concise immutable historical facts. | Long form/field reorder/save/review and 200% selected values. |
| Track entry flow (T6) | Early Entry Date, reversible optional Date and one clear Choice label. | Backdate/recreate/clear/save/reopen and required-field/error-scroll preservation. |
| Gym Progress (GS-A) | Context before results, compact tracked-record setup, visible required reps, correct reversed-machine best and readable result data. | One/multiple/no points, invalid reps/range, both machine directions, source drill-down. |
| Gym browse and routines (GS-B) | Calendar/filter/archive recovery, full-width History metadata, concise Routine overview and disclosed day/detail choices. | Static/phased/start-next/out-of-order/active/equipment/long-name journeys. |
| Routine authoring (GS-C) | Day-first outline, one expanded Set editor with compact sibling summaries, clearer phase management. | Multi-day/multi-set/reorder/duplicate/invalid hidden input/recreation/save at compact/wide/200%. |
| Settings organization (GS-D) | Dedicated Gym category, coherent Rest/Input/Calculation groups, direct global preset editing, clearer organization/reference/section hierarchy. | Every old setting reachable, category navigation/scroll policy, typed drafts, preset propagation. |
| Settings feedback/tools (GS-E) | Local announced reminder results, return refresh, clear quiet-hours context, consistent busy-aware CSV export and readable calculator results. | Delivery result/permission-return/busy export/backup neighbors and 200% facts. |
| Shared choices (X1) | Complete selected labels, selected option semantics, decorative checks and disabled-menu dismissal across form/unit/segmented menus. | Long chosen text at 200%, selected/unselected states and disabled transition. |

All accepted workstreams are implemented. HT-10 and GS-F reproduced and repaired the many-filter Task header, dense Calendar, timer-review keyboard and active-workout fixed controls. Machine-library search and extreme short-inspector redesign remain evidence-dependent proposals, with no confirmed defect left unimplemented in the accepted scope.

Execution of HT-05/06 uncovered an additional confirmed defect within the accepted note-only workflow: the Habit UI allowed an entry without a number, but its repository rejected the save. FND-20260927-019 is repaired using the already nullable amount and measurement link. Creation, numeric↔note editing, stable identity/provenance, repository recreation, undo, strict numeric validation, backup restoration and CSV note inclusion pass.

## Delivery sequence and ownership

1. Record this expanded plan and domain matrices before broad implementation (done).
2. Implement domain work in three parallel owned branches of the shared checkout; parent owns shared controls, integration, durable ledgers and command execution. Coordinate shared-file boundaries explicitly.
3. Review each diff for scope, authoring/history truth, accessible labels, interaction recovery and layout. Add focused executable behavioral evidence; do not write source-mirroring tests for every cosmetic edit.
4. Once implementations are stable, run one affected readiness batch and selected native/visual journeys. Fix actual failures, rerun only affected checks and inspect current originals across all six areas.
5. Update every plan row's disposition, record exact evidence and limits, then commit/push coherent verified groups. No incomplete group is called delivered; no release or physical-phone action is implied.

## Preliminary findings retained within the expanded scope

| ID | Area and observed friction | Accepted implementation | Acceptance evidence |
| --- | --- | --- | --- |
| H1 | Home's saved Task filter can hide every Today task, suppress its controls and claim the day is clear. | Retain a filtered empty Tasks section with the filter context and Show All Tasks recovery; keep loading/error and Area scope truthful. | A saved filter hides the sole Today task; recovery restores it and false all-clear is absent. |
| H2 | Habits Today suggests creation when existing habits are off schedule; an empty Area is not distinguished from an empty library. | Schedule-aware empty copy and View All Habits; Show All Areas for scoped absence; preserve first-use templates. Update Home Habit guidance accordingly. | Off-schedule, empty-Area and first-use journeys, including enlarged text. |
| G1 | Weighted milestone Goals ask for observation logs/trends although milestones are edited directly. | Show completed/total milestones and weighted progress in Insights and the inspector; preserve saved closure snapshots. | Milestone Insights, inspector and completed-snapshot assertions. |
| G2 | Goal Insights puts chart/no-data prose ahead of progress and joins pace, rate and forecast into one run. | Lead with progress and pace; use existing fact rows for rate and localized forecast. | Existing pounds and over-target journeys plus milestone rendering. |
| T1 | Track's all-time latest-entry summary is titled Recently Active Tracks, including future-dated entries. | Label it Latest Entries by Track without altering records or date selection. | Workspace summary assertion and current rendering. |
| T2 | Track Entries/Activity no-match states lack local recovery; an archived empty Track invites adding despite read-only state. | Add clear-local-search/filter recovery, preserve global Area and sorting; give archived empty copy that points to existing Restore, and an active first-entry action. | Entry and Activity recovery plus archived read-only guidance. |
| Y1 | Gym calendar absence can say No Workout History despite existing workouts on other dates. | Name empty date/month scope and offer Show All History recovery, retaining archive collection and sort. | Calendar-empty and recovery journey. |
| Y2 | Gym Exercise Library tells users to clear narrowing controls without a direct empty-state action. | Add Clear Search and Filters using existing controls and empty-state component. | Combined filtering, recovery and saved-state journey. |
| S1 | Appearance Settings starts with advanced preferences and keyboard help before theme controls. | Theme/colors, Presentation, existing Opening Area/Home Overview, then unconditional keyboard reference, with shared headings. | Initial ordering, controls and keyboard reachability. |

Use existing card, heading, fact and empty-state owners. Preserve two-row collection cards, specialized Gym execution, explicit Goal completion, over-target percentages, display-unit conversion, data and persistence formats. No new dependency, release version or physical-phone operation is planned.

## Verification and outcome

All 19 accepted workstreams are implemented. The additional HT-10 and GS-F constrained layouts were reproduced, repaired and accepted at actual Android 200% text on a 1080×1600 emulator window. The real note-only Habit save defect is repaired; full Habit repository and selected backup checks pass, including number↔note transitions and true nullable backup restoration.

Final `scripts/check --ready` passed **409 JVM tests in 48 suites**, Android-test compilation, lint, assembly and static checks. Every one of **171 selected Android methods** has a latest passing result, with no missing selection, skip or reused batch. Four constrained scenarios also passed with personally inspected originals. Catalog lint passes at 524 required captures, zero exceptions and zero pending selectors. Source inventory is 668 JVM + 1,151 Android; this was an affected campaign, not execution of that entire inventory.

The [evidence directory](../../artifacts/ux-overhaul/2026-09-27/README.md) retains unmodified images, a [browsing gallery](../../artifacts/ux-overhaul/2026-09-27/gallery.html), exact per-method XML/results and unsuccessful attempts. The earlier 55-second capture timeout remains incomplete. Final review corrected fixture assumptions about readonly facts, active filter counts, frozen snapshots and nested scrolling while preserving behavioral assertions. OS notification-settings return refresh is source-reviewed only. No release, phone installation, new dependency or persistence-format change occurred. Subjective appearance remains for normal owner use.
