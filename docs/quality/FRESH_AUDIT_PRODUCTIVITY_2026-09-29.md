# Fresh productivity audit — 2026-09-29

Status: Implemented and Verified within the focused productivity acceptance scope; workbook closed. Baseline: `ba1f7cc5`. Scope: current Tasks, Habits, Goals source journeys. This report does not use earlier audit conclusions as evidence or as a checklist. Source tracing is distinct from device execution. The parent owns final shared readiness, memory and delivery; the continuation reviewer ran only the exact repeated-history method described below and inspected all eight final overview originals.

## Grounded findings and proposed implementation

### P1 — Habit collection hides availability and concrete schedule

**Confirmed rendering defect plus design improvement.** `HabitScreens.kt:HabitProgressCard` takes a separate management path for the first Habits tab. It supplies `Paused` through `summary {}`, with no disclosure. `WhipProductivityItemBuilder.kt:WhipProductivityItemContent` renders summaries only for collapsed disclosure rows; the no-disclosure path renders `details {}` alone. Consequently the only availability signal is discarded. Its remaining schedule description is just `Selected weekdays`, `Every N days`, or `Flexible times per week`, rather than the configured days, interval, or count. Scheduled pauses are not represented there either.

Reproduce from source: create two weekday Habits with different weekdays, pause one, then open the default Habits collection. Both expose the same generic mode/schedule text and the paused summary never renders.

Remedy: make the management card carry a concrete schedule line and a visible availability/target line in the actual rendered details; include scheduled-pause and linked-source context. Preserve the collection's management purpose and the separate Today execution controls. Reuse the existing weekday formatter, target/unit formatting and item builder. Do not add another search field. Parent owns any shared-builder change.

### P2 — Skipped Habits are still counted as remaining

**Confirmed logic defect.** `HabitAreaContent` builds the Today header with `state.today.count { !it.isDoneForToday() }`; `HabitList` partitions using `isFinishedForToday()`, which includes skipped Habits. With one skipped Habit the header says one remaining while the body says All Set for Today and places the Habit in Finished for Today.

Remedy: use the existing finished predicate for remaining count. Keep skipped evidence, neutral scoring, reminders and Undo unchanged. An exact policy check should cover pending/completed/skipped combinations.

### P3 — Task collection disclosure does not bound metadata density

**Design improvement grounded in rendered inputs, not a claimed screenshot failure.** `TaskComponents.kt:TaskRow` feeds identical full metadata lists into collapsed summary and expanded details. `detailSegments` includes scheduled date, time, reminder, deadline, priority, duration, effort, every tag and full repeat description; neither the list nor summary line count is bounded. A richly configured Task remains a large paragraph before expansion. The visual distinction between daily decision information and secondary configuration is absent.

Remedy: compact summary prioritizes timing/deadline urgency, priority and useful subtask completion; full reminder/effort/tags/recurrence configuration stays expanded. Never hide overdue status. Preserve all saved data and full inspector access. Verify a rich recurring Task and short/large-font layout.

### P4 — Task history mislabels reopened occurrences and archive recovery is buried

**Confirmed copy/state defect and grounded consistency improvement.** `TaskComponents.kt:SeriesHistory` labels every `OccurrenceState.Open` as `Moved to …`. `TaskRepository.kt:reopen`/`reopenOccurrence` persist an Open occurrence even when its scheduled date equals its original date. Completing and reopening a recurring occurrence therefore produces a false movement claim. The history action shares a horizontal row with unbounded narrative text, which deserves responsive stacking using the existing layout pattern.

`TaskActionsDialog` also removes its primary action when archived; Restore Task exists only under More, whereas Habit and Goal inspectors surface Restore immediately. An archived Task's obvious next action is restoration.

Remedy: say Open occurrence when dates match and Moved to only when they differ; keep the original date for moved rows and recorded completion timestamp for completed rows. Stack history controls at constrained width/font sizes. Expose Restore Task as the archived inspector's primary action using the existing restore callback.

### P5 — Habit input errors are silent until the user guesses the cause

**Confirmed validation UX defect.** `HabitValueDialog` and `HabitHistoryLogDialog` disable their save action when an amount is blank, malformed, or non-finite, but the shared local `NumberTextField` has no error state, explanation or semantic error. The date error is explicit; numeric errors are not. Goal progress entry already permits an attempted save and exposes an inline required-value message.

Remedy: use an attempted-save/touched validation state with a clear finite-number message on the existing field and an accessibility error. Preserve note-only LogOnly entries and nullable historical facts; do not introduce positive-only constraints because negative adjustment entries are supported. Keep persistence failures and unsaved-change guards.

### P6 — Habit actions offer forms that cannot commit while paused/archived

**Confirmed UI/persistence mismatch.** `HabitActionsDialog` shows Add Past Entry whenever the Habit is not sourced from another measurement, including archived and paused Habits. Its manual-duration entry likewise checks only mode and linked source. `RoomHabitRepository.log` calls `requireHabitCanAcceptManualProgress`, which rejects archived and paused Habits. The user can fill an entire form and only then learn that the action was unavailable. Existing history edits use `updateLog` and intentionally remain possible, so the availability rule must distinguish adding from correcting.

Remedy: expose the paused/archived prerequisite before opening a new-entry form (disabled action with a useful explanation or a short notice alongside existing Restore/Resume). Retain editing/deleting existing history. Do not weaken repository guards or alter historical-data policy merely to hide the mismatch.

### P7 — Definition editing and elapsed reset drop inspector context

**Confirmed navigation behavior; improvement follows the requested context-preservation pillar.** Habit entry and scheduled-pause child flows retain their parent inspector. However `HabitAreaContent.onEdit` calls `closeHabitActions()`. Goal measurement children similarly retain the parent, while Goal `onEdit` and `onResetElapsed` explicitly set `actionsGoalId = null`. Cancelling these children returns to the collection instead of the selected inspector section, and successful edits do the same.

Remedy: preserve the inspector id/section/scroll across definition edit and elapsed-reset children; return there on cancel and success. Handle the Goal's intentional mutation-boundary snapshot carefully: accept the newly committed definition after a successful save, never silently replace it while the user is still editing. An Area change may legitimately make the entity leave a scoped collection, but the parent can remain backed by the unscoped editor state. Maintain the existing unavailable/deleted-entity protections.

### P8 — Reached Goals and milestone Goals lack an obvious collection next action

**Grounded design improvement.** `GoalCard` keeps Log/+1 as the primary action even when `offersCompletion()` is true; the completion opportunity exists only inside the inspector. Weighted-milestone Goals have no primary action at all while the checklist is collapsed. The collection's most relevant action therefore requires discovering the chevron or opening the inspector.

Remedy: give a reached Goal a compact Review/Finish route into its existing explicit Complete Goal opportunity, keeping numeric progress above 100% and never auto-completing it. Give an active unfinished milestone Goal a labelled checklist expansion action. Keep recording available from the inspector. Prefer callbacks/disclosure already present to adding a second lifecycle mutation path.

## Flow coverage and reviewed keeps

| Domain / journey | Current evidence and disposition |
|---|---|
| Tasks — first navigation / root tabs | Collection-first route exists; shared header/tabs and global search remain parent-owned. Keep. |
| Tasks — one-off and unscheduled collection | `TaskWorkspacePolicy.collectionTasks` selects live definitions and real occurrence projections; distinguishes empty historical remnants from authored evidence. Keep. |
| Tasks — rich card / expand / edit | Full metadata appears collapsed and expanded. P3. |
| Tasks — inline capture / parsed assumptions / detail editor | Draft retained until persistence receipt; parsing preview exposes inferred fields; Add Details carries draft. Keep. |
| Tasks — full create/edit fields | Existing validation, unsaved guards, date/time/reminder and repeat controls were traced. Keep domain policies. |
| Tasks — unscheduled to recurring | Explicit schedule conversion confirmation, date and recurrence controls. Keep. |
| Tasks — repeat anchor / this-and-future edit | Editor exposes valid anchors; repository validates current occurrence/revision and preserved historical split. Keep. |
| Tasks — Today / Upcoming / unscheduled scopes | Workspace policy normalizes date scopes and route filters. Keep. |
| Tasks — calendar / agenda / Habit overlays | Planning window explicitly shown; unscheduled route remains available; Habit overlay opens actual Habit. Keep. |
| Tasks — filter draft / saved views / search criteria | Filters, saved views and active text criteria remain separately visible; no extra collection search proposed. Keep. |
| Tasks — multi-select / hidden selection / bulk complete | Hidden selection warning and Clear Hidden exist; incomplete subtasks cause completion confirmation; recurring collection-series mutation restrictions are explicit. Keep. |
| Tasks — manual reorder | Requires unconstrained complete collection; pin partitions and accessible move controls retained. Keep. |
| Tasks — complete / incomplete subtasks / reopen | Incomplete subtasks prompt; repository validates target identity and snapshots; completed inspector has Reopen. Keep. |
| Tasks — subtasks / promote / undo | Interactive inspector checklist and explicit convert confirmation; undo-capable event. Keep. |
| Tasks — skip / reschedule / reset | Occurrence identity retained and repository checks current schedule; repeat anchor meaning explained. Keep. |
| Tasks — series history / paging | Recorded occurrence list is ordered and paged; open-state wording false for reopened same-date rows. P4. |
| Tasks — archive / restore | Data retained; restore hidden under More while peer domains surface it. P4. |
| Tasks — permanent delete | Separate reviewed-impact flow, series-wide wording and confirmation. Keep. |
| Tasks — focus presets / custom / replacement | 15/30/45/60 and bounded custom time; replacement names active timer and target; error and busy states. Keep. |
| Habits — first navigation / collection | Collection-first and distinct Today are preserved. Concrete schedule and availability rendering deficient. P1. |
| Habits — Today / finished / skip / undo | Finished partition correctly includes skips, header does not. P2. |
| Habits — check-off / whole checklist completion | Direct checkbox routes through live repository; checklist item states have separate controls. Keep. |
| Habits — auto-complete checklist mode | Editor and inspector explicitly explain independent-vs-final-item completion. Keep. |
| Habits — count / decimal quick increments | Add, Set Total, decrement and Undo Last Entry are distinct; quick values progressively disclose. Keep. |
| Habits — rating / LogOnly | Rating uses latest value; note-only LogOnly entries remain nullable and unconverted into zero measurements. Keep; P5 for form errors. |
| Habits — numeric adjustments / unit conversions | Period totals persist deltas; unit change converts draft amounts and preserves entered units; affine-zero changes blocked where totals cannot preserve meaning. Keep. |
| Habits — manual duration | Manual entry exists; paused/archived availability mismatch. P6. |
| Habits — timer start/stop/review | Persistent session identity and monotonic clock; unresolved timers remain reachable even for unavailable/restored states; review supports continue/log/discard. Keep. |
| Habits — create / template / edit | Mode semantics, numeric/schedule validation, progressive details, save receipts and unsaved guards. Keep; P7 for return context. |
| Habits — schedule / pause dates / indefinite pause | Pause dates remain visible and editable in history; indefinite pause explicitly differs from historical neutrality. Keep. |
| Habits — reminder times / weekday overrides | Existing structured controls and scheduler sync retained. No new reminder mechanism proposed. |
| Habits — add earlier history | Future date blocked; paused/archived action misleadingly opens a form. P6. |
| Habits — edit/delete existing history | Parent remains open, query and pagination are saved; nullable original values and entered units preserved. Keep. |
| Habits — skip history / undo | Neutral event explicitly describes Undo and keeps normal schedule. Keep. |
| Habits — linked measurement history | Synthetic mirrored entries are read-only, source attributed and numeric canonical units converted. Keep. |
| Habits — Insights scoring / low pressure | Scheduled completion distinguishes no scored days; activity chart is explicitly not target success for flexible/no-target/low-pressure cases. Keep. |
| Habits — Insights chart / grid | Week-aligned eight-week chart has accessible observations; dated activity cells use symbols and semantic state labels. Keep. |
| Habits — reorder / scoped empty state | Pinned partitions, Show All Areas recovery, meaningful empty routes. Keep. |
| Habits — duplicate / pin / archive / permanent delete | Dedicated persistence operations and reviewed deletion impact retain history policy. Keep. |
| Goals — active / completed / archive / Insights | Root scope explicitly distinguishes active/paused and completed/abandoned. Saveable destination pages preserve list state. Keep. |
| Goals — compact card / action / milestone checklist | Progress/unit display grounded in projection; milestone and reached-goal next action weak. P8. |
| Goals — create / templates / edit | Compatible type/dimension/aggregation guards, milestone identity, unit conversion, required values and unsaved guards. Keep; P7 for return context. |
| Goals — progress amount / completion count | Record Completion explains one record vs completing the Goal; editing preserves original positive values unless toggled. Keep. |
| Goals — outside-window update | Explicit warning and Save to History confirmation; future date blocked. Keep. |
| Goals — edit/delete history child | Parent remains open, saved query/paging survive, persistence errors do not close child. Keep. |
| Goals — weighted milestones | Active-only toggles carry mutation boundary; closed outcome is frozen while current saved checklist is labelled. Keep. |
| Goals — elapsed time / reset | Exact start time and timezone/offset resolution; reset event history retained. Keep data behavior; P7 for context. |
| Goals — target reached / complete / reopen | Explicit completion freezes outcome, reopening retains lifecycle history; above-100% numeric progress intentionally retained. Keep contract, P8 improves route. |
| Goals — pause / abandon / archive / restore | Lifecycle and archived status handled separately; contextual primary action exists. Keep. |
| Goals — permanent delete | Preview and mutation-boundary confirmation; retain. |
| Goals — Insights trend / units / sparse evidence | Date-spaced observations, canonical-to-display conversion, separate typed units for completion/range/consistency, forecast eligibility and sparse-data explanation. Keep. |
| Goals — history access / evidence table | Paged original measurements, read-only sourced updates, closed snapshot limits explicitly described. Keep. |
| Cross-domain — accessibility | Existing semantic checkbox roles, 48dp controls, status descriptions and error notices retained; P4/P5 target specific remaining cases. |
| Cross-domain — loading / failure / retry | Separate load state, retry routes, mutation save coordinators and persistent errors were inspected. No broad state-management replacement proposed. |

## Verification and ownership plan

Parent accepted P1–P8 and assigned `HabitScreens.kt`, `GoalScreens.kt`, `TaskComponents.kt` and focused fixtures. All eight groups are implemented. Shared item builder, `WhipApp.kt`, all global geometry and Gradle/device execution remain parent-owned.

Fresh original images inspected: `artifacts/fresh-app-overhaul/2026-09-29/before/productivity/{tasks.collection,habits.all.populated,goals.active.populated}.png`. They confirm the excessive area-row height of management Habits, the dense numeric Goal sentence, and elapsed Reset's prominent daily-action position. The accepted visual additions now use compact Habit schedule/availability cards, concise numeric Goal current→target readings with units and a bounded progress bar, explicit unbounded percent text, and secondary elapsed Reset inside expanded details and the inspector. The existing explicit completion path remains authoritative. Parent supplied the shared compact/summary-content rendering seams.

`git diff --check` passes. The first parent-owned five-class JVM batch passed in 54 seconds (`build/fresh-app-overhaul-20260929/integration-jvm-repaired.log`): 7 `HabitPresentationTest`, 5 `CompactCollectionStatusTest`, 2 `TaskCollectionPresentationTest`, 28 `GymUxRulesTest` and 3 `BackupDocumentReadTest` methods, 45 total with zero failures/errors/skips in their XML receipts. The later third Task presentation method, `undatedRepeatingDefinitionDoesNotBecomeAnUnscheduledTask`, subsequently passed in the parent's final affected readiness batch. The current Task presentation XML confirms all 3 methods with zero failures/errors/skips; it is not attributed to the earlier two-method receipt. `build/fresh-app-overhaul-20260929/final-ready.log` passes affected readiness, Android-test compilation, lint and debug build; the parent records the complete 450-method JVM scope separately.

All ten distinct `FreshProductivityUiTest` methods have passing exact native receipts: management schedule/availability; current and historical invalid amounts; archived-entry prerequisite; Habit and Goal definition-cancel return; elapsed-reset hierarchy/cancel return; reached-goal Review with above-target progress; persisted Goal save/unchanged-save/external-edit safety; and actual-200% Review/Items text layout. Their logs are `build/fresh-app-overhaul-20260929/productivity-*.log`. The final label receipt is `productivity-final-goal-actions.log`, 8.195 seconds, one test passed.

Three adapted existing native methods also pass: `ProductivityCardDesignUiTest#scheduledDateAndRecurrenceUseFullWidthSummaryAndExpandWithoutDuplication` (3.239 seconds), `#habitAndGoalSummaryRowsKeepDetailsAndInlineActions` (5.423 seconds), and `WhipComposeSemanticsTest#elapsedGoalResetWorksFromHomeExpandedDetails` (7.595 seconds). Exact receipts are the matching `existing-*.log` files. The elapsed header-bound fixture in `AdaptiveWhipScreenTest` was source-adapted; this report does not claim it ran.

The original repeated-cleanup acceptance now has a fresh passing execution on the current productivity APK: `ProductivityExperienceJourneyTest#repeatedHistoryCleanupKeepsQueriesAndRecoversFromStaleGoalDeletion`, 46.957 seconds, one test passed under the 55-second bound. It deletes two persisted Habit logs without leaving History, retains its query through recreation, exposes stale Goal deletion failure locally, reopens and deletes the fresh entry, corrects the remaining entry, and checks exact final persisted notes. Receipt: `build/fresh-app-overhaul-20260929/sol-productivity-history.log`. The disposable `emulator-5554` target passed `scripts/android-target-guard instrumentation`; both installed APK SHA-256 values matched the current built pair in `sol-productivity-install.log`. No Gradle, broad test batch or phone action was run by the continuation reviewer.

These are 14 distinct productivity native methods with passing evidence, not a full-suite claim. Parent-owned shared-title and inspector-rename checks additionally pass in `root-enlargedCardIdentityRemainsCompleteBesideItsActions.log` and `root-renamingAnEntityPreservesItsScrolledInspectorSection.log`. Stable inspector keys use Habit/Goal IDs and occurrence-sensitive Task identities, preserving section scroll when titles change.

Integration review added inline Area context to compact Habit schedules and replaced the temporary Goal revision watcher with a one-time post-child repository reread guarded by Goal ID/UUID and user-data access. The refresh flag clears on success or failure; no unrelated later revision is automatically accepted. `FreshProductivityUiTest#savingGoalDefinitionReturnsToHistoryAndDoesNotFollowLaterExternalEdits` passes in 8.681 seconds and exercises a persisted rename, unchanged save, retained History and a later unrelated edit. The final independent source review traced authored-snapshot refresh, recovery gating, child request receipts and saved inspector identity without finding a further productivity defect requiring edits.

Failure history is preserved in the original receipts. Android preparation found a nullable gate-lambda type mismatch in the refresh helper, corrected before successful compilation. The two initial definition-cancel fixtures reached their editors but used incorrect text selectors; replay with the existing accessible icon descriptions passes (`productivity-repaired-cancelling{Habit,Goal}DefinitionEditReturnsToHistory.log`, 5.605/6.108 seconds). No product cancellation defect was inferred from those fixture failures.

The first measured-label replay detected real clipping. The final measurement reproduces Material's inherited-style merge and rounds each 4dp padding edge individually before converting the complete lane to dp, retaining the 64dp minimum and 112dp cap. This repairs the one-pixel shortage rather than weakening the layout assertion. The passing actual-200% check verifies font scale, one line, no clipping/ellipsis, every character, minimum touch height and bounded lane width. Parent's normal Goal screenshot also exposed a green default progress track; both collapsed and expanded Goal bars now use neutral `surfaceContainerHighest` tracks. The final normal/actual-200% originals after this correction and the shared-toolbar repair were independently inspected as listed below.

## Final original-image review

All eight final original PNGs were inspected on 2026-09-29. The visible frames are consistent with the accepted productivity hierarchy and shared geometry; no further defect was found. These overview frames supplement the exact native behavior and text-layout receipts above; they do not establish unshown History, validation or child-return behavior by themselves.

| Surface | Normal original | Actual-200% original | Observed final result |
|---|---|---|---|
| Tasks collection | [Normal Tasks](../../artifacts/fresh-app-overhaul/2026-09-29/after/productivity/tasks.collection.png) | [200% Tasks](../../artifacts/fresh-app-overhaul/2026-09-29/after/large-productivity/tasks.collection.png) | Concise schedule/status summaries, complete visible identity, readable disclosure/check controls, and consistent root toolbar/tab geometry. |
| Habits management | [Normal Habits](../../artifacts/fresh-app-overhaul/2026-09-29/after/productivity/habits.all.populated.png) | [200% Habits](../../artifacts/fresh-app-overhaul/2026-09-29/after/large-productivity/habits.all.populated.png) | Compact cards carry inline Area and concrete schedule plus availability; names and edit controls remain readable at enlarged text. Paused-state coverage remains the separate exact native receipt. |
| Active Goals | [Normal Goals](../../artifacts/fresh-app-overhaul/2026-09-29/after/productivity/goals.active.populated.png) | [200% Goals](../../artifacts/fresh-app-overhaul/2026-09-29/after/large-productivity/goals.active.populated.png) | Current→target readings and percent are clear, tracks are neutral, the savings identity wraps completely at 200%, and elapsed Reset no longer occupies the primary action lane. Review/Items readability remains covered by the actual-200% layout method. |
| Shared Home | [Normal Home](../../artifacts/fresh-app-overhaul/2026-09-29/after/productivity/shared.home.populated.png) | [200% Home](../../artifacts/fresh-app-overhaul/2026-09-29/after/large-productivity/shared.home.populated.png) | Task/Habit daily evidence and direct actions remain clear; enlarged summaries stack with readable counts and the visible daily Task retains concise metadata. |

No new build, test or device operation accompanied this final image review. No remaining productivity-specific source or visual remedy is open. The parent's final affected readiness now passes; memory and delivery remain tracked by the parent plan.
