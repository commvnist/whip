# Habits, Goals and Tracks experience overhaul — 2026-09-28

Status: **Verified within the selected scope: all six groups implemented, four exact JVM methods and six exact native journeys passed, fresh originals personally inspected.** Baseline clean `9014937f`, private release 0.3.82. This is a fresh product-experience review, not a replay of the completed defect audit. Parent owns shared shell/navigation/Home/Tasks, canonical memory and serialized verification. This agent owns feature-specific Habit/Goal/Track implementation and exact fixtures after agreement. PX1–PX5 correspond to the central plan's EP1–EP5.

Read `AGENTS.md`, Whip memory skill, complete memory INDEX, relevant current feedback/findings/decisions/implementation, and current feature screens, view models, domain models and mutation boundaries. Related records: FB-20260928-005, DEC-20260928-001/003, FND-20260928-002/003/010, IMP-20260928-008. Prior audit statements are context, not current proof. In particular, the previous Track “create/templates” keep overstates the source: Track currently has no template chooser.

## Proposed experience groups

| ID / priority | Current source and before experience | Proposed after experience / ownership | Compatibility and smallest acceptance |
| --- | --- | --- | --- |
| PX1 / high | `GoalMilestoneProgress` explicitly tells users to expand the Goal in Active Goals to check milestones. The inspector has no milestone action. A user arriving from Insights/search must leave their context and hunt the collection card. | Reuse the collection milestone controls in Goal Overview; live weighted progress updates without dismissing details. Existing no-request mutation owner and exact milestone boundary; preserve opening Goal-definition boundary for conflicting external edits. | GoalScreens only, no storage change. Exact native toggle, weighted progress, reopen and archived/closed read-only assertion. |
| PX2 / high | Every active Goal keeps completion only under Options → Availability, even when the visible progress is 100% or more. Achievement has no local next step. | Add an explicit completion opportunity beside the achieved Outcome for finite target-oriented Goals; preserve logging, manual below-target completion in Options and saved-completion celebration. | No automatic completion or change to over-target values; exclude ongoing Maintain Range, Open Trend and Elapsed semantics. Pure eligibility method plus native achieved/milestone workflow. |
| PX3 / high | Habit Today overview describes checklist item count but offers only parent completion. Individual items are available only in expanded collection cards. | Show the same accessible checklist inside Today; checking an item updates the inspector and optional parent completion in place. | Reuse current item IDs/date/toggle callback, skip/paused/off-schedule rules and parent-completion policy. Exact native final-item, uncheck and disabled-state assertions. |
| PX4 / medium | Track definition authoring is a schema list; users must save it and begin an Entry to discover their resulting form. Empty create starts with only Name and lacks the starters already offered by Habits/Goals. | Add explicit editable starter drafts for common reusable logs and a clearly labeled disposable Entry Preview using the production field renderer. Keep blank creation available. | TrackScreens/domain draft constructors only; no preview Entry persistence, no new schema/dependencies. Exact draft content test and native preview/sample input → close → definition save proof. |
| PX5 / high | Habit history starts with eight events and adds 25 per tap; Goal starts with 25. Finding an old note/value/date requires sequential paging. Habit history eagerly composes all expanded events in one scrolling Column. | Local History search over original displayed value/unit, ISO/localized effective date and authored notes, clear/no-match recovery and count; filter before paging. Habit includes skip/pause events and uses lazy event rows. Goal lifecycle/reset facts retain separate meaning. | No global-search/index/schema changes, no history mutation, editable source rules remain. Pure matching test and native oldest-result/edit/clear/restoration journey. |

Parent accepted all five groups, including starters paired with preview, before production changes. Implementation must preserve safe live projections and avoid reformatting whole histories on every keystroke. Track creation was traced through `WhipApp`'s editor route to the sole `TrackEditor` initial draft, plus the Track collection and Insights create callbacks: no alternate template entry route exists.

### Owner follow-up — retain context while maintaining history (EP6)

The owner reports that deleting each Habit history entry dismisses the inspector and returns to the Habits page. Current source confirms the cause before edits: Habit event-edit/add/pause callbacks call `closeHabitActions()` before opening the child editor, destroying the parent History section/query/scroll composition. Goal measurement editing similarly clears `actionsGoalId`, and the shared receipt handler also clears it on event saves/deletes. This affects corrections as well as deletion.

Accepted and implemented remedy: retain the Habit/Goal inspector behind event editors, close only child state after save/delete/cancel, and retain History selection, query and list position across repeated corrections and recreation. Goal inspector composition now precedes event dialogs so children are on top. Habit numeric/log-only/manual-duration entry and pause/skip history child forms retain the same parent ownership. Entity archive/deletion and elapsed-origin reset retain their existing navigation and stale-definition boundary protection. Goal observation receipts preserve the inspector only for event operations; local failures retain the child for correction or cancellation.

Track already retained its parent route, but source review found that content refresh/recreation collapsed a previously loaded window to the first 100 Entries. Accepted EP6 followthrough now saves the requested window, refills in existing 100-row repository calls with a hash set for linear deduplication, publishes atomically under request ownership, and grows only after successful Show More. Query/filter/sort changes reset the window. Initial restoration holds the list uncomposed until rows are ready so saved scroll is not clamped by loading UI; same-query search refresh snapshots/restores its viewport after fresh results settle without publishing stale rows or hiding the query input.

Acceptance selectors below include repeated Habit deletion, child cancellation, stale Goal deletion failure and fresh retry, Goal correction/recreation, plus older Track row visibility after deletion/correction/recreation without scrolling back to it. Parent recorded FB-20260928-008/FND-20260928-015 and accepted EP6 before changes. Both exact EP6 native journeys passed; receipts and original inspections are recorded below.

## Current-source workflow dispositions

Rows describe source review, not executed tests. **Keep** means a specific retained contract; it does not assert the experience cannot improve. **Change** points to a proposal above. **Integration** identifies parent/shared or unexecuted device evidence.

| Area / workflow | Disposition and evidence |
| --- | --- |
| Habit first use, true empty, no due Habits, scoped absence | Keep: Today distinguishes first use, schedule-empty and other-Area existence; direct templates/All/Show All recovery in HabitAreaContent. Parent owns scope chrome. |
| Habit templates and all tracking modes | Keep editable draft-first templates, Check Off/Count/Decimal/Timer/Checklist/Rating/Log Only semantics and existing saved-mode restrictions. |
| Habit name, emoji, Area and advanced settings | Keep shared identity/organization/editor components, raw draft and validation summary. No new wizard or field abstraction. |
| Habit units, numeric targets, quick amounts, end conditions | Keep converted configuration, exact numeric text and protected original log units. Existing additive affine rejection remains necessary. |
| Habit schedule, weekdays, flexible periods, reminders | Keep authored schedules and shared date/clock controls; no change to event-based flexible counting or first partial period. |
| Habit check-off and checklist execution | Change PX3: item execution belongs in the inspector as well as expanded cards. Preserve established autoCompleteFromItems: enabled by default, final item derives parent Success; disabled requires explicit parent completion. Unchecking an item does not reopen parent completion. |
| Habit Count/Decimal quick-add, Set Total, decrement, Undo | Keep separate additive/replacement actions and original exact total semantics; current quick-add cards remain available. |
| Habit Rating and note-only Log Only | Keep blank additive inputs and true nullable note-only fact; no manufactured zero/success. |
| Habit timer start, stop, review, manual duration | Keep monotonic session, original unit, uncertainty review and direct manual recovery; do not duplicate Focus timer controls. |
| Habit Today overview/status/low-pressure mode | Keep current date, actual state, metrics and neutral terminology; PX3 adds work where the overview already explains it. |
| Habit history order and original values | Change PX5 navigation only; preserve effective-date ordering, original units, notes, failed and linked facts. |
| Habit history edit/delete/Undo, foreign entries | Keep exact authored mutation and source-editability boundary. Search must not turn a read-only entry into an editable result. |
| Habit skip and dated/indefinite pauses | Keep skip-neutral rules and distinction between current pause availability and historical dated pause facts. PX5 includes their visible history. |
| Habit Insights, charts and performance | Keep actual scheduled-day/recorded-activity distinction, empty evidence and date labels. Current per-card filtering and day-state work are source-visible; no measured regression or blanket speed claim. |
| Habit archive, restore, permanent delete | Keep actual delete impact, timer guards, restore entry point and preserved history. |
| Habit draft dismissal/recreation/error/retry | Keep secondary raw draft guards and request coordinator; parent owns shared editor protection. Selected new controls need fresh native evidence. |
| Goal active/history/archive collections, reorder | Keep orthogonal lifecycle/archive meaning, pin partitions and native reorder owner. |
| Goal first use and templates | Keep editable templates for numeric, consistency, milestone and elapsed outcomes; no automatic template save. |
| Goal create/edit type, dimensions and units | Keep compatible persisted dimension/closure-outcome restrictions and exact numeric conversion. |
| Goal start/deadline, aggregation and pace | Keep explicit authored windows, window warnings and current numeric/typed-outcome rules. |
| Goal log/edit progress, backfill and latest | Keep effective-date latest ordering and original observation facts; new history search must preserve them. |
| Goal targetless trend/range/consistency meanings | Keep independent semantics. PX2 does not imply ongoing ranges or open trends are finished at a transient value. |
| Goal weighted milestone execution | Change PX1: inspector becomes a working checklist; no forced collection detour. Preserve milestone weight/reward and closure snapshot distinction. |
| Goal achievement and explicit completion | Change PX2: achieved Outcome offers its next action without changing explicit lifecycle or celebration ownership. |
| Goal elapsed reset, display, closed duration | Keep exact instants, selected unit breakdown, immutable reset events and frozen closed duration. |
| Goal Insights and chart/data table | Keep date-spaced trend, selected units, sparse evidence, target/rate meaning and earlier-day paging. PX1 removes the milestone dead-end reached from Insights. |
| Goal History observation navigation | Change PX5: local search before pagination; retain original value/unit/date/note display. |
| Goal lifecycle/reset History | Keep permanent snapshots and actual timer reset facts; never reconstruct missing historical settings. |
| Goal pause/resume/reopen/archive/duplicate/delete | Keep existing boundary-checked mutation owners and reviewed permanent impact. PX1 must not weaken stale-definition guards. |
| Goal failures, interrupted saves and drafts | Keep original snapshot ownership and recoverable raw drafts. Live milestone updates require a bounded live projection, not wholesale snapshot removal. |
| Track first use and create | Change PX4: source has only a blank Name-field draft, despite earlier audit template wording. Add safe optional starters. |
| Track definition fields, order, identity and required state | Change PX4 preview only; retain exact Field/Choice identity and explicit required/identity labels. |
| Track field type changes and historical locks | Keep value-preserving definition reviews and explicit migration/removal consent. |
| Track number/unit/scale fields | Keep finite canonical persistence, authored precision, native scale steps and actual unit labels. Preview reuses the same renderer. |
| Track choices, dates, Yes/No and missing values | Keep normalized stable choices, date clear and blank-versus-No distinction. Preview cannot silently select false/default facts. |
| Track entry create/edit and future dates | Keep future Track facts/plans, unlike Habit/Goal observation constraints; preserve exact entry/form boundaries. |
| Track duplicate identity and recovery | Keep duplicate review, clear conflict recovery and retained draft; starters do not bypass validation. |
| Track Entries search/filter/sort | Keep request/version-owned indexed search, typed sort, applied-condition chips and truthful failure/retry. PX5 reuses its established local-search grammar. |
| Track paging and long history | Change EP6 preserves the loaded window and viewport through event changes/recreation; retain 100-row pages, explicit Show More and error retry. No measured large-history performance claim. |
| Track detail/read-only archive | Keep discoverable restore and read-only entries, editable reviewed definition and unambiguous Track Insights scope. |
| Track per-field/workspace analytics | Keep selected units, robust numeric aggregation, supported sum/mean semantics and 30-day rate label. |
| Track import/export | Keep native document picker, typed mapping, preview, all-or-nothing commit, exact receipt recovery and original CSV format. No new data transport. |
| Track duplicate structure/archive/restore/delete | Keep copied structure without copied entries, identity-preserving archive and exact permanent impact. |
| Track private checkpoints/recreation/data reset | Keep complete private drafts, compact Android state and generation guard. Preview values are disposable and explicitly described as such. |
| All feature accessibility/adaptive layout | Reuse shared inspector/field/action roles, 48dp row targets, natural wrapping and lazy scroll. New checklist, search and preview states require fresh ordinary and constrained originals; no new broad TalkBack/RTL/fold claim. |

## Verification and delivery boundaries

Parent runs exact selected JVM/native methods under `timeout --kill-after=3s 55s`. No full, readiness or candidate batch; no phone release, commit or push from this agent. Source inspection and previous passing receipts are never described as fresh execution. No schema 46, backup format 26, epoch 6 or retired-product change is proposed. All accepted groups must be fully implemented and close with exact evidence or explicit remaining limits.

## Implementation checkpoint before execution

All six accepted groups have production implementation and focused fixtures; fresh compilation/execution remain pending parent serialization. An earlier Android compilation identified a missing GoalDraft type in the new fixture; it is corrected. The outcome of that failed compilation is not a pass.

- **EP1/PX1:** `GoalScreens.kt` extracts the existing responsive card milestone controls into `GoalMilestoneChecklist` and uses it in Overview. Active Goal projections refresh only while the opening Goal mutation boundary still matches; external definition changes retain the guarded snapshot. `GoalViewModel` accepts the inspector's existing authored request; its nonpersisted MilestoneChanged receipt keeps details open while busy shielding and failed/retry state remain local. The two-argument collection callback remains available. Exact milestone updates keep their current saved identity/revision. Closed and archived checklists stay read-only.
- **EP2/PX2:** `offersCompletion` exposes Target Reached plus Complete Goal for active, unarchived finite target-oriented outcomes at or above 100%. It excludes ongoing range/trend/elapsed semantics, nonfinite/unknown ratios and terminal snapshots. Existing committed lifecycle request and celebration owner remain authoritative.
- **EP3/PX3:** `HabitChecklist` is the original collection control reused in Today's Checklist. It retains 48dp checkbox semantics, actual current-date item IDs, optional final-item completion and independent unchecking; archived, linked, paused, skipped and off-schedule inspector states are read-only. `HabitViewModel` uses the existing authored coordinator and committed-reminder follow-up, with request-local saving/error/retry for details and the original four-argument collection callback. Today summary, checklist and Skip occupy separate lazy content items.
- **EP4/PX4:** `TrackAuthoringExperience.kt` supplies Blank/Reading/Spending/Reflection editable starters, temporary preview-only Field/Choice IDs and an Entry Preview that renders `TrackEntryField` and validates via `validateTrackEntryDraft`. Sample values live only inside the preview composition. Track editor validates the definition before opening preview, reviews replacement of a dirty draft, preserves the selected Area, and scrolls preview validation into view. Definition checkpoint/persistence remain unchanged.
- **EP5/PX5:** Goal and Habit history each prepare lowercased search strings with original display units/value, authored notes, ISO and localized effective dates once per changed history/unit/locale snapshot. Query changes normalize one short query and scan prepared strings; they do not reformat the full history. Filtering precedes pagination, and only the visible prefix is composed. Habit history moves from an eager scroll Column to stable-key lazy event rows; log keys use durable UUIDs because mirrored numeric IDs are hashes. Skips and pauses are searchable. Goal lifecycle/reset evidence remains independent of Progress History's query.

Minimal selectors handed to parent:

| Layer | Exact owner and method |
| --- | --- |
| JVM | `com.whip.app.ui.ProductivityExperienceTest#completionOpportunityRequiresAnAchievedActiveFiniteOutcome` |
| JVM | `com.whip.app.ui.ProductivityExperienceTest#goalHistorySearchUsesOriginalObservationFacts` |
| JVM | `com.whip.app.ui.ProductivityExperienceTest#trackStartersAndPreviewKeepAuthoredDefinitionsSeparateFromSampleFacts` |
| JVM | `com.whip.app.ui.HabitPresentationTest#historySearchKeepsOriginalUnitsDatesNotesAndNeutralEvents` |
| Native | `com.whip.app.ProductivityExperienceJourneyTest#goalMilestonesWorkInsideDetailsAndCompleteOnlyOnRequest` |
| Native | `com.whip.app.ProductivityExperienceJourneyTest#habitChecklistWorksInsideTodayAndRetainsParentCompletion` (actual font-scale rule) |
| Native | `com.whip.app.ProductivityExperienceJourneyTest#historySearchFindsOlderOriginalFactsAndRestoresItsQuery` |
| Native | `com.whip.app.ProductivityExperienceJourneyTest#trackStarterPreviewUsesRealFieldsWithoutSavingSampleValues` (actual font-scale rule) |
| Native | `com.whip.app.ProductivityExperienceJourneyTest#repeatedHistoryCleanupKeepsQueriesAndRecoversFromStaleGoalDeletion` |
| Native | `com.whip.app.ProductivityExperienceJourneyTest#trackHistoryChangesKeepLoadedOlderWindowAndSearchAcrossRecreation` |

`git diff --check` passed at the handoff. No new test execution, speed benchmark, TalkBack, RTL, process-kill or large imported-history claim is made at this checkpoint. The performance claim is the concrete preparation/lazy-composition mechanism above, not measured latency.

## Fresh verification and original inspection

Read the parent's preserved original receipts in `artifacts/experience-overhaul/2026-09-28/verification/`: the three selected `ProductivityExperienceTest` methods and one selected `HabitPresentationTest` method passed (4 total, no skips/errors). After the fixture's missing GoalDraft type and a nullable Track retry callback inferred as Job were corrected, `android-package-ready.log` reports successful packaging in 36 seconds. Failed earlier compilation remains recorded in its original logs.

`ProductivityExperienceJourneyTest#repeatedHistoryCleanupKeepsQueriesAndRecoversFromStaleGoalDeletion` passed 1/1 in 31.939 seconds, process/guard wall 34.389 seconds (`native/history-cleanup.log`, exit 0). This executes repeated Habit deletions, retained query/recreation, local stale Goal deletion failure, child cancellation, fresh deletion retry, correction and recreation, and checks exact surviving facts.

Personally inspected fresh original `originals/experience.habits.retained-history.png` and `originals/experience.goals.retained-history.png`: both retain the selected History section, their entered query and clear affordance, singular count, surviving original date/value or note, and unobstructed footer action. The Habit shows Cleanup 2 and the Goal shows Review corrected with value 11. No visible clipping, overlap or stranded child overlay in these dark portrait captures. These images establish the resulting resting context; the passing assertions establish intervening actions/failure handling. Remaining selected native evidence is pending.

`#habitChecklistWorksInsideTodayAndRetainsParentCompletion` passed 1/1 in 12.220 seconds (wall 14.559 seconds); `#historySearchFindsOlderOriginalFactsAndRestoresItsQuery` passed 1/1 in 22.913 seconds (wall 25.184 seconds). Personally inspected `experience.habits.working-checklist.large.png`: actual large font, Complete today, both readable checked rows, naturally wrapped final-item explanation and unobstructed Undo footer. The summary above is partially scrolled outside the viewport, not overlapped by the checklist. Inspected Habit and Goal `history-search.png` originals: Mountain query, 1 of 35 result counts, oldest Aug 25 note, original Goal value 34, clear and footer actions all readable without clipping.

Initial Track continuity native run failed after its no-rescroll deletion/correction/recreation assertions, because the fixture attempted to scroll before asynchronous query results arrived (23.062 seconds). Initial Goal milestones native run failed after completion because of a wrong collection tab tag (10.427 seconds). Initial preview native run failed because an exact-text assertion supplied only a prefix of the successful validation message (13.105 seconds). These remain recorded failed runs; three bounded fixture corrections were grouped into one parent rebuild before the final reruns below. Inspected intermediate Goal working-milestones original: Active status with 2/2, 100%, distinct Target Reached/Complete Goal opportunity and readable checked rows. Inspected intermediate Track retained-history original: 104 Entries and the oldest Retain corrected row/date fully visible below rows 99–102 after recreation. These captures do not convert the enclosing failed methods to passes.

### Final selected verification

After those three fixture corrections and the parent's serialized rebuild, personally read the fresh final original logs. Each contains `OK (1 test)`, status code 0 and process exit 0:

| Exact native method on `ProductivityExperienceJourneyTest` | Original receipt | Instrumentation / process wall |
| --- | --- | --- |
| `trackHistoryChangesKeepLoadedOlderWindowAndSearchAcrossRecreation` | `native/track-history-window-final.log` | 24.139 / 26.453 seconds |
| `goalMilestonesWorkInsideDetailsAndCompleteOnlyOnRequest` | `native/goal-milestones-final.log` | 12.121 / 14.193 seconds |
| `trackStarterPreviewUsesRealFieldsWithoutSavingSampleValues` | `native/track-preview-final.log` | 17.867 / 19.991 seconds |

Together with the earlier cleanup/checklist/history-search passes, all six selected productivity native methods now passed. Earlier failures remain preserved and described above; they are superseded by distinct fresh passing executions, not relabeled. All six accepted groups are implemented without a deferred remedy.

Personally inspected the latest `experience.goals.working-milestones (1).png`: Active remains explicit alongside 2 of 2 and 100%; Target Reached, Complete Goal and both checked milestone rows are readable and separated without overlap. The passing method subsequently proves explicit completion and closed read-only controls. Inspected `experience.tracks.retained-history (1).png`: 104 Entries and oldest Retain corrected remain fully visible with the original date after recreation; preceding rows 99–102 confirm the viewport is beyond the first loaded page. The passing method asserts this after deletion/correction/recreation without scrolling back and subsequently verifies retained search through recreation.

Inspected `experience.tracks.entry-preview.large.png` at the fixture's actual large font: title, Entry Date, complete successful validation text, Item/Amount required labels, Train ticket and 4.25 sample values, Notes, Check Sample and Close Preview are readable. The explanatory introduction is partly scrolled above the content viewport after validation navigation; field labels and bottom actions are not clipped or overlapped. The exact method closes the disposable preview, recreates the editor, saves the Spending definition and verifies its fields with zero persisted Entries.

Limits remain explicit: selected emulator coverage and source review, not a full suite, process-kill recovery proof, TalkBack/RTL/fold campaign or large-history latency benchmark. No schema/backup/epoch change, data migration or new dependency. Parent owns final shared integration/release judgment; this agent ran no build/device commands or commits. `git diff --check` passed after final report updates.

Parent integration follow-through: after correcting shared non-primary dialog safe drawing insets, the exact preview journey passes again in 16.151s (18.706s wall), and repeated Habit/Goal cleanup passes again in 34.074s (36.448s wall). `track-preview-insets-final.log` and `history-cleanup-insets-final.log` retain results. Parent personally inspected the final `(1)` preview and Habit/Goal retained-history originals: full validation, fields/actions and surviving historical context remain readable and unobstructed. These repeats supplement, rather than replace, the earlier unchanged-owner evidence.
