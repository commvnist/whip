# Productivity workflow audit — 2026-09-28

Status: **Complete — 84 workflow dispositions, nine accepted repairs, ten distinct productivity JVM methods and four distinct native methods passed.** Baseline: clean `main` at `d737209a`. The investigation and first seven implementation groups were recorded before production edits; P-08/P-09 were then confirmed and accepted during the long-history/numeric trace. This is the productivity half of the owner's new full-app audit, covering Tasks/Home task surfaces, Habits, Goals, Track and their domain/repository/view-model owners. The parent owns shared shell, Settings, navigation, design components, canonical memory, build serialization and final integrated acceptance. No phone operation is authorized by this report.

## Evidence contract and prior work

Read the repository `AGENTS.md`, the Whip memory skill and complete memory index; searched relevant feedback, findings, decisions and implementation records. Reviewed the second-audit matrices and their retained test/visual coverage as a map, then retraced current callers. Related history: FB-20260928-002/004, DEC-20260902-003/004, IMP-20260928-002/003, VER-20260928-002/004. The paused older whole-product audit remains separate.

**C** below means source-confirmed behavior with a concrete reproducible input, not a newly executed emulator test. **K** means a justified current keep after source tracing. **R** names a verification limit or a parent-owned integration dependency. Prior images and receipts establish their dated fixtures only. No new Gradle, instrumentation or device command had run at this planning checkpoint.

The common review path is entry/discovery, empty/populated/scoped state, authoring, validation, commit/error/retry, execution, edit/history, archive/delete/undo, draft/recreation, ordering and accessibility. The changes preserve schema 46, backup format 26, data epoch 6, stable IDs, historical entered values/units/timestamps and the existing shared layout/control owners.

## Accepted findings and implementation plan

The parent accepted P-01–04 first, then P-05–07 after the broader date/history trace, and P-08/P-09 after checking concrete long-history and ordinary decimal cases. All nine are implemented. Evidence in the finding column describes the baseline defect; final owners and execution evidence appear below.

| ID / priority | Evidence and user consequence | Smallest complete remedy and acceptance |
| --- | --- | --- |
| **P-01 / P1 — Habit unit edits reinterpret configuration** | `HabitEditorDialog`'s `UnitSelectionField.onSelect` assigns only `unitId` (`HabitScreens.kt`). A saved 2 L target becomes 2 mL when selecting mL; quick increment/actions and an After Total ending threshold keep their raw numbers too. Repository update persists those numbers in the newly selected display unit while historical logs correctly retain canonical/original units. Goal already converts corresponding physical targets. | Convert relevant complete numeric drafts together; preserve blanks, invalid raw text and the current unit if conversion cannot complete. Reuse Goal's exact conversion helper and preserve history. Convert only an After Total ending amount, keeping completion/streak counts unchanged. Existing additive Habits reject switches across different zero points, with an explanation and a Create New Habit recovery: no constant target conversion preserves every possible summed affine reading. A selected native fixture saves/reopens L→custom cup→L, checks targets/quick amounts/ending total and original log identity, and preserves incomplete text. |
| **P-02 / P2 — recurring and undated deadlines lose overdue state** | `buildUiState` sets `isDeadlineOverdue` only in `ScheduleKind.Once`. Recurring deadlines can be authored by the editor/parser, and repository-valid undated tasks can retain deadlines. All other projections default false, so overdue filtering, row emphasis, inspector status and search omit overdue meaning. | Derive active overdue state once in the shared projection-decoration path. Preserve completed/archived neutrality and exact occurrence identity; distinguish schedule lateness from deadline lateness. Verify Once, Anytime, both recurrence anchors, open overrides, future schedule/past deadline, and terminal history. |
| **P-03 / P2 — Duplicate violates name limits** | All four repositories append ` copy`/` Copy` without budgeting for the current name limit. Valid 100-character Habit/Goal/Track names become 105 and fail their create validation. Task duplicate inserts a 205-code-point title directly without calling its create validator. | A small shared bounded copy-name function, respecting each existing UTF-16/code-point limit and Unicode boundaries. Use it at all four duplicate owners and validate the Task duplicate draft. Verify maximum names and repeated copies, no orphan/partial records, independent new children, no copied history, and source identity unchanged. |
| **P-04 / P2 — Habit secondary drafts disappear on dismissal** | `HabitValueDialog`, `HabitHistoryLogDialog`, and `HabitPauseDialog` retain draft fields with `rememberSaveable` but pass Back/outside/Cancel straight to `onDismiss`. A typed journal note, history correction or pause range is silently discarded; corresponding Goal progress editing already uses `UnsavedChangesDialog`. | Reuse the existing discard dialog on every dismissal path once the raw value/note/date/range differs from opening state. Keep clean dismissal immediate; keep saving shielded; explicit Save/Delete remains authoritative. Verify Cancel/Back, Keep Editing, Discard, restored raw drafts and failed save without duplicate submission. |
| **P-05 / P1 — Goal Latest follows write time instead of the observation date** | `aggregateGoalValue`, `GoalInsightWindow` and consistency Latest choose the largest `MeasurementEntry.timestamp`; `projectGoal` sorts history by that timestamp. Repository manual backfill supplies the authored effective `localDate` but uses the current write timestamp. Logging yesterday's 80 after today's 75 makes 80 current and contaminates the final date-based trend point. Track already sorts effective Entry Date first; DEC-20260902-004 establishes the same authored/effective distinction for Habits. | Select Latest by effective local date, then timestamp, retaining the existing first-input tie behavior. Apply one shared comparator to current/insight/legacy consistency reading and chronological history. Preserve stored date/time/unit/provenance and closure snapshots. Verify backfill, date correction, equal dates/timestamps, rolling expiry, and native current/History/Insights agreement. |
| **P-06 / P2 — Review disagrees with Goal evidence eligibility** | `goalOutcomeScoreOnDate` returns 1 for Open Ended Trend before checking start/deadline or finite observations, so explicitly saved History-only dates contribute to Review. Weighted Milestones always returns 0 because the no-measurement early return precedes milestone handling, even though each milestone has a saved `completedAtMillis`. | Apply effective-date/finite eligibility before trend outcomes. Score completed milestone weight on its saved completion date in the active Whip zone, divided by all meaningful authored milestone weight; retain bounded Review scores, zero-weight behavior and no fabricated elapsed outcome. Pass the existing Review zone to the domain scorer. Verify before/after-window facts, missing/failed/nonfinite values, midnight zone boundaries, multiple milestones, zero weights and unfinished milestones. |
| **P-07 / P2 — flexible Habit's first partial period vanishes from its rate** | `completionRateOverRecentPeriods` compares a week/month start directly with `Habit.startDate`; a Wednesday-created weekly Habit has Monday before its start, so the whole first week is omitted even if its reduced eligible-day target is achieved. `flexibleProgress` and flexible streak already include the first partial interval. | Compare against the anchored first period while preserving the bounded lookback, open-period neutrality, actual pause/skip eligibility and established raw-event counting. Verify midweek/month starts, achieved/open/closed first periods, custom week starts and the neighboring streak/Review behavior. |
| **P-08 / P2 — current Habit streak loses earned history after a year** | `HabitViewModel` builds exactly 366 dates of success/neutral state before `habitStreak` walks back to the Habit start. A continuous 500-day daily Habit therefore stops at missing map evidence after 366 days; older skipped/paused dates also lose their neutral meaning. This is earned streak, not a deliberately bounded recent statistic. | Move current-streak evidence into `Habit.currentStreak`, group logs once, memoize period outcomes, and walk only as far as the actual gap/start. Preserve current unfinished-day, pause/skip and schedule semantics. Exact JVM assertions cover 500 successes, older skips/pauses and a real recent gap; flexible-period streak retains its existing implementation. |
| **P-09 / P2 — ordinary decimal totals change Habit outcomes and Track statistics** | Additive `Habit.valueForPeriod` uses binary Double summation, so authored Exactly 0.3 misses after 0.1 + 0.2. The same arithmetic affects After Total and Insights. Track Sum/Average and both analytics displays also use ordinary sums: 1e16 + 1 − 1e16 becomes 0, while the mean of two finite `Double.MAX_VALUE` observations overflows. Goal already has decimal accumulation solving these cases. | Extract the existing Goal `decimalTotal`/`decimalMean` unchanged into `NumericAggregates`; use narrow sum/mean conveniences for finite Habit/Track values and preserve Goal filtering/DECIMAL128 behavior. Retain previous nonfinite propagation for already-overflowed unit conversions, avoiding a new BigDecimal exception. Exact JVM assertions cover 0.1 + 0.2, cancellation, finite extreme means and nonfinite propagation. No changes to stored measurements, temperature/rating sum eligibility or display precision. |

Owned production files: `ui/HabitScreens.kt`, `ui/HabitViewModel.kt`, `ui/TaskViewModel.kt`, `domain/GoalModels.kt`, `domain/HabitModels.kt`, `domain/TrackAnalytics.kt`, `ui/TrackScreens.kt`, `ui/ReviewOutcomes.kt`, and the four repositories. Three narrow shared owners remove or prevent duplication: `domain/CopiedItemName.kt`, `domain/NumericAggregates.kt`, and `ui/NumericDraftUnits.kt`; the existing Goal-only converter moved without semantic change and its callers were updated. Parent owns `UnitSelectionField`, `WhipApp.kt`, shared clock/dialog controls and canonical ledgers.

## Workflow disposition matrix

Each row is an investigated workflow, not a passed test. The sources named are current owners; proposed native assertions below remain unexecuted until exact receipts are recorded.

### Tasks and Home task workspace

| Workflow | Current evidence / disposition |
| --- | --- |
| Home true first use, scoped absence, hidden sections | **K/R:** existing unscoped existence evidence and temporary Show All Areas recovery preserve persistent scope. Parent owns fresh shared-shell rendering. |
| Home Task summaries, pinned order and completion | **K:** shared Task rows and actual Today/Upcoming sources; recent Focus timer work retains pinned feedback. P-02 corrects overdue meaning at the projection owner. |
| Inbox quick capture and Add Details | **K:** `buildQuickAddTaskDraft`, parser assumptions and request-owned capture preserve raw text, multiline steps and failed saves. |
| Smart dates, repeat, priority, tags, duration | **K:** parser retains unrecognized text and reports concrete interpretations; editor enforces raw duration bounds instead of clamping. |
| Full Task create/edit, required title and Area | **K:** `TaskEditorDialog` uses the canonical draft and exact editor boundary; title/Area/duration errors remain visible with saved raw input. |
| Schedule, repeat anchor/end, reminders | **K:** recurrence contract, future-series split and original occurrence identity remain authoritative. Parent owns the duplicated clock-picker repair. |
| Deadline authoring, overdue filters and notices | **C P-02:** ordinary recurring authoring reaches the missing projection flag. Keep the independent scheduled-date/deadline distinction. |
| Move to Inbox and schedule removal | **K:** explicit consequence dialog names removed date/time/reminders/deadline. No invisible expansion of Inbox authoring. |
| Subtasks, progress, manual/automatic parent completion | **K:** stable steps, snapshots and intentional parent-completion policy. Child unchecking does not silently rewrite authored parent completion. |
| Subtask promotion and guarded Undo | **K:** exact promoted/source revisions protect Undo; preserve source history and surfaced follow-up warnings. |
| Task inspector Overview/Activity/Options | **K/R:** domain context and destructive boundaries remain; shared constrained-inspector behavior belongs to parent. |
| Focus timer start/custom/replace/stop/recovery | **K:** latest source matches the separately completed timer audit; this pass must not disturb its exact task/deadline ownership. |
| Today, Upcoming List/Agenda/Calendar | **K:** current horizon distinctions, optional Habit overlay, missing-date states and named view switches. |
| Planning capacity and stale preview | **K:** total daily capacity uses all Areas; candidates stay scoped; transaction rechecks current workload and retains a failed preview. |
| Search, saved filters, sort/group/pinned partitions | **K:** local list query is distinct from global search; seven-day and completion-date filters are explicit. P-02 fixes the same overdue bit for every consumer. |
| Bulk selection, hidden selection and mutations | **K:** visible stable keys, revision-aware authored previews, selection retention and request-owned archive/delete. No new generic mutation framework. |
| Manual reorder and recreation | **K:** existing shared handles/drag owner and authored sequence; retain pinned partition rules and current scope semantics. |
| Complete/reopen, recurring history, archive/restore | **K:** actual completion timestamp is separate from scheduled date; exact occurrence reopening preserves history/snapshots. P-02 must not color terminal history overdue. |
| Duplicate | **C P-03:** maximum valid title produced an invalid stored copy. Correct the bounded title at persistence, retaining the existing undated Inbox destination and existing stored deadline/time behavior. |
| Delete, Undo, load/error/busy | **K:** permanent whole-series review, stale identity guards, exact Undo and domain error/retry remain. Selected changed flows need fresh native checks. |

### Habits

| Workflow | Current evidence / disposition |
| --- | --- |
| Today/All/History/Insights discovery and scoped empty | **K:** separate scheduled attention, off-schedule activity, timer recovery and true first use; parent owns global Area behavior. |
| Creation modes and templates | **K:** Check Off, Count, Decimal, Timer, Checklist, Rating and Log Only remain distinct; templates are drafts, not immediate persistence. |
| Existing tracking-mode and dimension edits | **K:** compatible existing dimension is fixed and unsupported mode transitions explained. Preserve source-linked restrictions. |
| Unit selection, targets, increments and endings | **C P-01:** unit edit currently relabels authored configuration. Counts, date intervals and completion/streak thresholds must remain counts. |
| Empty Custom dimension / create custom unit | **C parent-owned repair:** keep empty-collection creation reachable and unresolved selection truthful. Integration also found that creation receipts can precede the unit definition's projection: the shared chooser now retains a saveable pending identity and waits for that definition before invoking Habit/Goal conversion. A newer selection/creation cancels a stale pending identity. Parent's exact shared fixture delays projection through recreation; the Habit fixture covers real saved custom-unit conversion. |
| Raw numeric/advanced input and precision | **K:** semantic cleanup and validation distinguish active fields; P-01 must preserve incomplete raw text and reveal conversion failure. |
| Target rules and periods | **K with explicit limit:** occurrence/day/week/month/rolling scope is authored. Existing weekly/monthly aggregation includes its whole period by contract; do not silently replace it with a daily as-of rule. |
| Flexible schedule/event counting | **K:** current tests deliberately count positive eligible events, including multiple per day, rather than distinct dates; preserve that behavior. |
| First partial flexible period and completion rate | **C P-07:** rate alone drops the beginning interval; align with existing progress/streak eligibility. |
| Check Off and Checklist execution | **K:** checklist item controls, auto-complete option, direct parent action and original item identity remain. |
| Count/Decimal quick adds, decrement, Set Total | **K/C P-01/P-09:** additive shortcuts and authoritative replacement total are separate; preserve no-op/correction handling. Convert authored unit configuration and compute ordinary decimal sums without changing target outcomes. |
| Rating and additive Log Only | **K/C P-04:** optional number starts blank for each additive entry; a real note-only fact stays nullable. Typed drafts require dismissal protection. |
| Timer start/stop/review/continue/discard | **K:** exact monotonic session, original unit and uncertainty review; running session blocks unit/schedule edits. |
| Today value and history draft dismissal | **C P-04:** Saveable state exists but Cancel/Back/outside deletes the active draft without review. |
| History effective dates/status/value/unit | **K:** the previous audit preserves failed/check-based facts, original entered units and authored timestamps; future dates are rejected before writes. |
| History correction, linked/source records, delete/undo | **K:** only user-owned entries editable; number↔note transactionality and foreign measurement attribution retained. |
| Pause indefinitely and dated pauses | **K/C P-04:** current availability does not fabricate historic pause intervals; dated ranges alter derived schedules with explicit copy. Draft range/note dismissal is unguarded. |
| Skip/Undo Skip and out-of-schedule logging | **K:** skipped days are neutral authored facts, explicit off-schedule capture remains possible, timers stay reachable. |
| End conditions, streaks, Insights charts and Review | **C P-07/P-08/P-09:** retain authored threshold and neutral-day semantics; repair first-period rate omission, the 366-date earned-streak truncation, and decimal sum/mean outcomes. Explicit bounded recent-statistic windows remain bounded. |
| Duplicate, pin, reorder, archive/restore/delete | **C P-03 / K:** long-name Duplicate fails; other operations preserve stable identity, protected timers and reviewed deletion impact. |
| Draft recreation/error/busy/keyboard | **K/R:** entity editor and mutation state preserve failures; P-01/P-04 need focused ordinary/200% saved-state and input tests. No new TalkBack claim. |

### Goals

| Workflow | Current evidence / disposition |
| --- | --- |
| Collections, scope, selection, reorder and empty guidance | **K:** active/paused, completed/abandoned and archived organization remain distinct with direct local recovery. |
| Templates and all nine Goal types | **K:** authored type chooses valid aggregation/direction and dedicated numeric/milestone/elapsed controls. |
| Identity, dimension, compatible type and unit edits | **K:** immutable saved measurement dimension; exact target conversion and closure-outcome-kind guards already exist. P-01 should reuse conversion rather than create divergent Goal behavior. |
| Baseline/target/range, precision and partial drafts | **K:** exact initialization and semantic validation retain raw text; no new domain ranges or precision reductions. |
| Start Date, deadline and aggregation window | **K:** explicit Start Date, past-window warning and confirmation, future-progress rejection and current/rolling-window exclusion. |
| Log/edit progress and original entered units | **K:** exact mutation boundary, raw draft, failure/retry and original unit/date/time/provenance preservation. |
| Latest reading after manual backfill/date correction | **C P-05:** authored write time can outrank the later observation date. Correct derived selection without changing any saved fact. |
| Accumulate/Average/Min/Max numeric calculations | **K:** recent exact decimal aggregate correction remains; retain finite observation filtering and sparse-window behavior. |
| Maintain Range and time-in-range meaning | **K:** observation percentage is dimensionless; physical range values/targets use actual selected units. |
| Consistency period denominator/window | **K:** explicit required periods, ended-window copy and immutable closed outcome kind. P-05 also aligns retained legacy Latest configurations. |
| Weighted Milestones authoring, weight and toggles | **K:** stable item IDs, partial raw weights, exact toggle boundary and weighted live progress. **C P-06:** earned dated weight never reaches Review. |
| Elapsed timer/reset/display/history | **K:** true instants, Whip zone, selected composite format, reset history and frozen closure duration. No elapsed duration is manufactured as daily Review progress. |
| Current value, progress >100%, bounded bar and pace | **K:** quantity/progress display can exceed target; bar/Review remain bounded and completion is explicit. |
| Insights timeline and table pagination | **K/P-05:** date-spaced trends and per-point windows exist; backfill ordering must agree with current latest. Full historical observations remain inspectable. |
| Sparse/invalid/out-of-window evidence and forecasts | **K/P-06:** chart/forecast filtering is already explicit; Review must apply the same eligibility instead of accepting History-only open trends. |
| Pause/resume/complete/abandon/reopen | **K:** semantic lifecycle boundary, committed receipts and frozen closure snapshots; Save/reopen must not reinterpret closed outcomes. |
| Completion celebration and reduced motion | **K:** saved explicit completion owns one Victory Shower; no new celebration variant or automatic threshold completion. |
| Duplicate, pin/archive/restore/delete | **C P-03 / K:** name limit fails duplication. Orthogonal archive and exact destructive impact remain. |
| Draft discard, failed save and recreation | **K:** existing Goal confirmation/input shielding is the pattern reused for Habit secondary editors. |
| History/Review date semantics and timezone | **C P-05/P-06:** preserve observation chronology and score milestone completion in the selected zone. Parent shared clock design remains separate. |

### Track

| Workflow | Current evidence / disposition |
| --- | --- |
| All Tracks/Activity/Insights/Archive and scoped empty | **K:** collection grammar, explicit local recovery and global Area separation remain. |
| Create/templates/name/description/emoji/organization | **K:** draft-first definition editor, early name/Fields and bounded wide form; no added wizard. |
| Empty Custom dimension and selected-unit truth | **C parent-owned repair:** shared empty-unit/create and unresolved-selection fixes also serve Number Field authoring. Newly created-unit selection waits for the actual definition; it cannot silently use a label or factor absent from the current projection. |
| Field add/edit/order/type and measurement locks | **K:** repository-bound meaning, exact existing IDs, active/archived custom units and history locks are necessary complexity. |
| Composite identity and numeric/custom-unit titles | **K:** readable `entryDisplayTitle` is separate from canonical duplicate identity; original historical unit remains a fact. |
| Required and optional Short/Long Text | **K:** complete imported text is editable without suffix truncation; blank/required semantics and current trimming policy retained. |
| Number input/units/precision | **K:** entered value/unit represent authored input; unlike a Habit configuration-unit edit this is deliberate reauthoring. Exact finite canonical persistence retained. |
| Scale/range/step and historical compatibility | **K:** discrete native controls, explicit unset/clear and precise saved-history compatibility checks. |
| Choice labels/options/removal/replacement | **K:** normalized duplicate labels, stable option identity and reviewed migration of actual historical values. |
| Date and Yes/No three-state authoring | **K:** required values begin unset, optional No differs from blank, optional Date can clear. Existing concrete test coverage is retained but not claimed rerun. |
| Entry Date, future records and metadata | **K:** future-dated Track facts/plans are supported; bounded recent insights exclude them. Do not import Goal/Habit future-date rules globally. |
| Entry create, duplicate review, edit/delete/undo | **K:** frozen form/entry boundaries, private draft recovery and exact post-commit receipts; no generic replacement protocol. |
| Default history paging and alternate full sort/filter | **K:** explicit 100-row pages and Show More; other conditions use the complete projection. Keep visible load failures/retry. |
| Search pending/stale/cancel/error/retry | **K:** normalized query/content version owns results, failures remain errors rather than silently changing search semantics. |
| Field sort, null-last, canonical numbers | **K:** deterministic typed sorting and chronological Entry Date remain; no string/numeric coercion. |
| Conditions, Match All/Any, numeric/date Between | **K:** original canonical-unit bounds and nested saved-state cleanup remain; missing Field conditions stay disclosed and removable. |
| Per-Track numeric/categorical/date Insights | **K/C P-09:** identity Fields participate, temperature/rating sums remain withheld, units/precision remain explicit. Concrete cancellation/finite-mean defects are reproduced by the selected JVM method and fixed using the existing Goal accumulator in domain and both UI summary owners. |
| Workspace latest records and recent rates | **K:** count plus Show More reaches all Tracks, explicit 30-day weekly-rate window, independent all-time latest section. |
| Archived inspect/edit/restore/pin | **K:** Entries stay read-only; definition corrections remain reviewed and available; no unavailable Home shortcut promise. |
| Duplicate Structure | **C P-03:** long valid names fail create; copy only structure/ordered fields/options, never Entries or their identities. |
| CSV map/preview/errors/import/receipts/recovery/export | **K:** existing typed bounds, exact batch identity, all-or-nothing commit, native picker and receipt-driven recovery stay; no CSV or backup format change. |
| Large drafts, saved-state private checkpoint, generation reset | **K/R:** complete private checkpoints keep Android state compact and refuse stale recovery; no fresh process-kill/very-large-history campaign in this planning checkpoint. |
| Accessibility/large text/IME/RTL/adaptive dialogs | **R:** current reusable owners and prior originals inform test selection. Parent owns shared improvements; changed dependent authoring/duplicate flows require exact fresh native evidence. |

## Verification plan and honest boundaries

Use exact JVM methods at the owning domain/UI policy, listed below. Four selected native methods protect real persist/reopen and original-history preservation across unit edits, three secondary Habit drafts with restoration/Back at actual 200% text, Goal effective-date current/trend/history agreement and all four Duplicate repository owners. Parent owns the empty-Custom-unit shared fixture, without duplicating it per caller. Assert values/identities in addition to labels.

Every routine check is bounded with `timeout --kill-after=3s 55s`. No Gradle runs until the parent grants a serialized slot. This task's explicit verification scope is small exact JVM and selected emulator methods; no full or readiness batch. A timed-out command is incomplete. No source test is counted as executed, no prior image is counted as new rendering, and no broad all-device/all-language/TalkBack or release claim follows from this audit.

## Implementation and verification checkpoint

All nine production groups and their focused fixtures are implemented. `git diff --check` passed before the native compilation handoff. No database/schema/backup version changed. Canonical ledger entries and commits/pushes belong to the integrating parent.

Design and extensibility disposition: retain the existing shared entity inspectors, section navigation, identity/action rows, numeric fields, unit chooser, and persistence notices. P-01 places an actionable conversion problem beside the owning Unit field while keeping the selected unit/raw draft truthful. P-04 reuses `UnsavedChangesDialog` and its existing destructive action style; it introduces no parallel dialog framework. Goal date ordering and Task deadline state are fixed in their shared projections so cards, filters, History, Insights and consumers agree. The three small pure helpers share concrete existing behavior rather than adding a generalized editor or mutation abstraction. Historical entered values/units remain visibly distinct from current configuration. Fresh visual acceptance is deliberately restricted to the originals named below.

The parent ran the following ten productivity JVM methods in its 14-method selected integration set; all passed. Receipts: `artifacts/product-audit/2026-09-28/verification/jvm-accepted.log`, `jvm-methods.tsv`, and `jvm-xml/`. The first combined command timed out during test compilation after production compilation; it remains incomplete. Separate bounded test compilation and exact execution then succeeded. The Track method was rerun after the parent's nonfinite convenience-helper correction and passed (`jvm-numeric-overflow.log`, `jvm-overflow-xml/`). Final integration also removed the three newly added finite filters at Habit period/ending totals and Track aggregation, preserving the original nonfinite eligibility rather than silently dropping overflowed conversions. The exact Habit decimal and Track decimal methods passed again in 5.694s; `jvm-numeric-final.log` and `jvm-final-xml/` retain that final-source evidence.

| JVM owner | Exact method |
| --- | --- |
| `CopiedItemNameTest` | `copyNamesStayWithinTheirExistingLengthContractsWithoutSplittingUnicode` |
| `TaskUpcomingVisibilityTest` | `overdueDeadlinesReachEveryActiveScheduleButNotTerminalHistory` |
| `GoalRulesTest` | `backfilledObservationsDoNotReplaceLaterDatedLatestProgressOrHistory` |
| `GoalRulesTest` | `openTrendReviewExcludesHistoryOnlyAndNonRecordedEvidence` |
| `ReviewOutcomesTest` | `completedMilestonesContributeTheirWeightOnTheSelectedLocalDate` |
| `HabitRulesTest` | `flexibleCompletionRateIncludesTheFirstPartialWeekAndMonth` |
| `HabitRulesTest` | `currentStreakKeepsMoreThanOneYearAndOlderNeutralDays` |
| `HabitRulesTest` | `exactDecimalTargetsAndTotalsRetainTheAuthoredAmounts` |
| `TrackDomainTest` | `decimalAggregatesRetainCancellationAndFiniteMeans` |
| `GoalProgressPresentationTest` | `changingGoalUnitsPreservesMassAndAffineTargetsWithoutConsumingIncompleteText` |

Native methods compiled successfully after correcting the fixture's two TaskStep identity assertions from unavailable `uuid` to `id`. The initial compile failure is retained in `android-compile.log`; the succeeding `native-assembly.log` is compilation/assembly evidence. Selected execution is recorded individually below:

| Native owner | Exact method / acceptance |
| --- | --- |
| `HomeHabitAudit2UiTest` | **Passed:** `habitUnitChangesSaveConvertedConfigurationAndRetainOriginalHistory`: custom-unit conversion, real Room save/reopen, incomplete raw numeric draft, original log identity. Accepted two-method integration run in 46.111s; `android-units.log` / `android-units.time.json`, original XML under `build/instrumentation-results-9JPjka`. The paired shared fixture also passed deferred new-unit selection after receipt and recreation. |
| `HomeHabitAudit2UiTest` | **Passed:** `secondaryHabitDraftsSurviveDismissalAndRecreation`: Value/History/Pause raw notes, state restoration, Cancel/Back, Keep Editing/Discard, actual 200% text. Accepted two-method integration run: 38.361s at 1080×1600/density 420; `android-draft-large.log` and `android-draft-large.time.json`. |
| `GoalProgressJourneyE2ETest` | **Passed:** `backfilledGoalHistoryKeepsTheLaterObservationCurrentAfterRecreation`: full app current/trend/History, recreation, original stored entry equality. Accepted two-method integration run in 31.677s; `android-tag-goal.log` / `android-tag-goal.time.json`, original XML under `build/instrumentation-results-UqcIAC`. |
| `ProductivityDuplicateRepositoryTest` | **Method passed:** `maximumNamesDuplicateIndependentStructuresWithoutCopyingHistory`: max names across four persisted owners, repeated Track copy, independent child identities and source/history preservation. Method XML records 0.312s with zero failures. The containing two-method run (`android-tag-duplicates.log`, 20.328s, `build/instrumentation-results-fjYIKV`) failed its separate Tag fixture; that fixture was repaired and accepted in the later Goal run. The failed containing batch is not described as green. |

The workflow matrix is source review, not a claim that every listed journey ran again. No new broad locale, TalkBack, RTL, process-kill, large imported history or release/device campaign is claimed. P-01's additive affine-unit rejection is deliberate; Goal affine conversion remains supported because its physical targets and current-reading model have different semantics.

Fresh visual personally inspected: `artifacts/product-audit/2026-09-28/visuals/product-audit.habits.discard-draft.large.png`. At actual 200% text, the entire confirmation heading/explanation and both stacked actions fit. Keep Editing precedes the distinct red Discard Changes action; neither label overlaps or truncates. The underlying pause form remains visibly contextual but dimmed. This inspection supports that captured dialog state only.

Also inspected the original 1080×2400 `product-audit.goals.backfilled-current.png`: the dark inspector retains distinct identity/status/section navigation, the graph labels September 27 at 80% and September 28 at 75%, and the expanded table repeats 80.0 then 75.0 by effective date. The rate correctly points downward; the forecast explanation remains honest for movement away from the target. Both table rows and Log Progress remain readable/reachable. The scroll position crops the top of the graph region, not a data-table label or action.

Inspected the original 1080×2400 `product-audit.habits.converted-unit.png` after the successful round trip: litres (L), minimum 2, maximum 3 and quick increment 0.5 agree; the inline explanation clearly separates converted configuration from retained historical units. The tracking controls wrap without collision, the form scrolls, and the fixed Save action remains reachable. No additional geometry change was required in these captured productivity states.
