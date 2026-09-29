# Whip complete product-experience overhaul — 2026-09-28

Subsequent release: all twelve groups were installed privately on the owner phone in **0.3.83/code 89** on 2026-09-29, under [VER-20260929-001 and the release receipt](../../artifacts/phone-releases/2026-09-29/0.3.83/README.md). The audit evidence below retains its original pre-release scope.

Status: **Complete — all twelve groups implemented and verified within the owner's focused scope**. FB-20260928-007/008, IMP-20260928-011, VER-20260928-007. Clean pushed baseline `9014937f`; current private phone release remains 0.3.82/code 88. Parent plus exactly two additional GPT-6 Astra/high agents.

## Required outcome

Independently audit the current app from discovery through successful use and recovery, identify substantial improvements to experience, visual design/language, clarity, speed and quality, then implement every recommendation. Earlier audits guide navigation to owners but do not prove current optimality. The intended result is a complete implemented improvement plan, not a finding list or arbitrary quota of small fixes.

## Work sequence and ownership

1. Recheck current source, durable decisions and representative running workflows; map all major areas and cross-feature transitions. Distinguish source review, measurement and observed rendering.
2. Publish prioritized findings before production changes, with impact, intended before/after, all affected owners, compatibility and a focused acceptance check. Investigate uncertainty before accepting a remedy.
3. Implement coherent groups, including underlying state/persistence, accessibility and recovery. Coordinate shared owners and reuse actual equivalent behavior.
4. Run only useful exact methods/fixtures under 55 seconds with 3-second kill grace; inspect fresh changed-state originals and repair confirmed followthrough. Do not run full batches, affected-readiness or candidate campaigns. Treat timeouts as incomplete and avoid repeated blind retries.
5. Reconcile each accepted recommendation and each goal requirement against actual implementation and evidence, update memory, and commit/push verified work. Report exact limits without substituting them for completion of accepted work.

| Owner | Scope | Detailed report |
| --- | --- | --- |
| Parent | Onboarding, Home, Tasks, shared navigation/search/organization, cross-feature consistency, shared controls, architecture, integration and performance evidence | This report |
| Astra/high productivity agent | Habits, Goals and Tracks; complete authoring/action/history/analysis/import/export journeys and underlying rules | `EXPERIENCE_OVERHAUL_2026-09-28_PRODUCTIVITY.md` |
| Astra/high Gym/Settings agent | Gym/Library/Routines/workout/history/progress; Settings, backup/recovery, units, notifications/timers/widgets and external boundaries | `EXPERIENCE_OVERHAUL_2026-09-28_GYM_SETTINGS.md` |

The parent serializes builds and emulator work. The physical phone remains on its verified release; this goal uses synthetic disposable emulator data. No schema, backup-format or release-version change is assumed necessary.

## Completion requirements and evidence ownership

| Requirement | Evidence needed before completion |
| --- | --- |
| Full product and connected user journeys | Current-source disposition of every major area, entry/authoring/action/history/correction/archive/delete/recovery, with current running examples. |
| Substantial experience improvement | Concrete before/after for accepted changes to interaction effort, discoverability, hierarchy and clear next actions; no arbitrary small finding limit. |
| Visual language and accessible interaction | Existing/new shared role ownership, fresh changed-state originals and relevant light/dark, keyboard, enlarged-text and compact/larger layout checks. |
| Clarity and trustworthy meaning | Consistent terms, units/dates, state/error/success explanation and domain calculations across affected consumers. |
| Speed and responsiveness | Evidence-based assessment of startup/navigation/rendering/search/calculation/database/large-history hot paths; measured remedies where problems are substantiated. |
| Extensibility and quality | Actual shared reuse, explicit state/domain boundaries, persistence/history protection and tested recovery for materially changed behavior. |
| Complete implementation | Every accepted group mapped to delivered source and passing selected checks; no recommendation silently deferred. |
| Efficient verification | Exact commands/results/timings, fresh versus historical evidence, failed attempts and explicit scope limits. |
| Durable delivery | Coherent commits/pushes, canonical memory, final completed plan and reviewable visual evidence. |

## Accepted implementation groups

The twelve groups below were accepted before their production changes, including the owner's subsequent history-continuity feedback. The three reports now cover 137 current-source workflow dispositions: 32 shared/Home/Tasks, 45 productivity and 60 Gym/Settings. A justified keep is an assessment outcome; it is not a postponed recommendation or an executed-test claim.

| ID / priority | Current problem and intended improvement | Owners / compatibility | Focused acceptance |
| --- | --- | --- | --- |
| EP1 / high | Weighted Goal inspector displays milestones but cannot execute them; the user must leave and expand a collection card. Put the same actionable milestone checklist inside Overview and keep committed progress live. | GoalScreens and existing milestone boundary; closed/archived/stale contexts retain explicit read-only behavior. | Toggle and persist from inspector, keep it open, verify progress/source and protected stale/closed behavior. |
| EP2 / high | A reached finite Goal hides explicit completion in Options. Offer completion at the moment the target is reached while preserving manual choice and ongoing Goal semantics. | Goal inspector and existing completion receipt; no automatic completion or reinterpretation of MaintainRange/trend/elapsed behavior. | Eligible reached versus incomplete/ongoing/closed Goals, confirmed persisted completion and feedback. |
| EP3 / high | Habit Today inspector describes a checklist but does not let the user work through it. Reuse actionable checklist rows inside the inspector. | HabitScreens and existing checklist callback; preserve the authored auto-complete-from-items setting and explicit parent action, plus skip/pause/off-schedule rules. | Toggle/reopen/recreate and verify stored items and the configured parent-completion behavior. |
| EP4 / high | Track creation starts with an unexplained schema and provides no entry-form preview. Offer editable starter logs plus a preview through the actual entry-field renderer, so users can understand and refine what they are building before saving. | Track editor/field renderer; existing supported field types, no template auto-save or preview-value persistence. | Choose/customize starter, preview actual required/ordered fields, save/reopen; blank creation remains supported. |
| EP5 / high | Finding an old Habit/Goal event requires sequential Show More and scrolling. Add saved local History search over displayed date/value/unit/note and relevant neutral events, result/clear recovery, and lazy Habit history composition. | Existing loaded historical facts; pagination after filtering, no schema/index change or rewritten history. | Oldest match immediately reachable, edit exact source, clear/query recreation, no-match recovery; assess large-history work. |
| EP6 / high | Owner reports repeated Habit history deletion dismisses the parent and returns to the collection. Preserve the parent History view through single-record correction/deletion, including equivalent Goal/Track/Task/Gym child-record workflows where affected. | Existing source boundaries and request owners; close only the finished child editor/review, keep query/section/useful position, and retain intentional navigation after entity removal. | Delete two distinct historical records and correct another without reopening History; failure/retry, actual stored outcomes, query/section and recreation remain coherent. Audit analogous handlers across domains. |
| EGS1 / high | Returning Gym users see an inert no-workout state rather than useful starting choices. Provide a compact launchpad with meaningful routine choices, exact next-day/static-day behavior, Browse All and the latest completed workout's source. | Existing workout/routine/history commands; preserve first use, empty workout creation and active-session recovery. | Start correct programmed/static day, deduplicated bounded choices, exact history navigation and active-workout guard. |
| EGS2 / medium | The Routine library lacks search despite potentially large named day/exercise structures. Add remembered search, truthful counts and clear/no-match recovery. | RoutineContent; names/notes/day/exercise lookup, focused routing and constrained reorder protection. | Find routine through nested content, clear/recreate, prevent reorder from changing unseen items. |
| EGS3 / high | Data & Privacy places bulky folder administration ahead of backup/restore and exposes every CSV action at once. Lead with truthful protection status and core backup/restore actions; progressively disclose folder administration and spreadsheet exports. | Settings presentation, unchanged backup verification/preview/request owners; errors and busy state remain explicit. | Configured/unconfigured/error/protected states, direct actions, disclosure recovery and constrained rendering. |
| EGS4 / high | Every Gym clock tick recalculates workout statistics and rebuilds exercise lookup even though only elapsed/rest time changes. Update only time-dependent summary fields between authoritative data emissions. | GymViewModel; preserve data/settings recalculation and exact elapsed/rest rules. | Tick/full-calculation equivalence, rollback/ended/no-session cases, representative before/after timing observation without flaky thresholds. |
| ES1 / high | Plan My Day is buried in Inbox and renders its entire candidate list inside one large scrolling item, leaving Apply below all candidates. Make planning directly discoverable from Home/Tasks Today and Inbox, in a focused dialog with lazy candidates and fixed confirmation. | Parent Task/Home presentation and existing request-owned atomic plan mutation; all-Area capacity and scoped/filtered Inbox eligibility remain explicit. | Direct entry, long candidate list and keyboard at enlarged text, capacity/selection/recreation/failure and successful commit without losing the preview. |
| ES2 / high | Global Search rebuilds its cross-feature index for irrelevant Gym clock ticks and repeatedly parses query/ranking terms per result/comparison. Key indexing to real search content and parse one query per search execution. | UnifiedSearchDialog and pure query rules; preserve exact structured filters, All/Any terms, scope, partial-state disclosure and stable ranking. | Content-key independence from clock/derived summaries, invalidation on changed searchable content, matcher/ranking equivalence and representative before/after timing. |

Detailed domain reports retain current-source traces, all reviewed workflows and exact fixture selectors. Every accepted row must end with implementation and inspected evidence; additional supported findings will be added before their changes.

### Initial direct inspection and compatibility followthrough

Fresh synthetic emulator inspection of the pre-enhancement debug binary confirms onboarding's current clear setup choice and coherent hierarchy. The retained binary is 0.3.81-debug from the final preceding audit (SHA recorded in baseline install receipt), not a newly compiled acceptance build. Final changed-interface acceptance will use current compiled source. The private phone is not used for this inspection.

The architecture document's Health Connect paragraph contradicts current retirement decisions/source. Correct it to describe retained provenance and no current integration, rather than resurrecting retired functionality. This documentation correction is part of the architecture assessment.

ES2 integration review also found that Goal's 30-second clock is irrelevant to its indexed definition/history content, and a completed search snapshot could be treated as settled while its source index had changed. Include Goal content-only invalidation and index identity in settled-result ownership, and explicitly mark an index rebuild loading. These are part of the same search consistency/performance remedy, accepted before their edits.

## Parent workflow and architecture assessment

These are current-source dispositions, supplemented by the specifically identified native examples. A source keep is not a claim that every device/state was freshly executed. The two domain reports supply the Habit/Goal/Track and Gym/Settings matrices; this table covers their shared and Task/Home connections.

| Journey / concern | Assessment and implemented disposition | Owners |
| --- | --- | --- |
| First launch | Keep recommended setup followed by optional customization; meaningful default and no account prerequisite. Fresh baseline onboarding inspected. | FirstRunSetupDialog, FirstRunSetupRoute |
| Startup/recovery | Keep canonical recovery before normal writes, explicit blocked/retry state and data generation boundary. Do not trade data recovery for an unmeasured launch claim. | WhipApplication, StartupRecoveryGate, UserDataGenerationBoundary |
| Empty Home | Keep explanatory Task/Habit starting choices followed by other domains. Fresh baseline Home original inspected. | HomeGettingStarted, HomeComponentCard |
| Returning clear day | Keep available history/review/resume paths and distinguish no work today from no stored data. | HomeResumePath, homeEmptyStateEligible |
| Scoped Home | Keep named current Area and Show All recovery; distinguish hidden data from first use. | HomeContent, AreaScopeFilters |
| Daily planning discovery | ES1 adds direct Home and Tasks Today entry to the same Inbox planning owner. | TodayHeader, HomeContent, TaskAreaContent |
| Planning execution | ES1 extracts a lazy candidate dialog with fixed Apply/Close; retains draft after close/recreation and failure, clears on confirmed success. | TaskDayPlanDialog, request coordinator |
| Capacity truth | Keep all-Area Today workload, assumed 30-minute unknown durations, scoped candidates and atomic revalidation. Today date constraints no longer accidentally remove Inbox candidates. | forDayPlanning, TaskViewModel.planMyDay |
| Task quick capture | Keep inline parser explanation, raw text/draft, request-owned save and Add Details handoff. | SmartTaskCaptureUi, TaskAreaContent |
| Task full authoring | Keep identity first, scheduled versus Inbox explanation, explicit Repeat transition and separate Deadline; primary save and authored validation remain. | TaskEditorDialog, TaskEditorRouteHost |
| Task execution/subtasks | Keep complete primary action and actionable Overview subtasks; child conversion has explicit review. The same missing-action issue in Habits/Goals is remedied by EP1/EP3. | TaskActionsDialog, TaskSubtaskChecklist |
| Focus | Keep existing start/presets/custom, active progress, permission recovery and explicit Stop separate from Task completion. | FocusTimerUi, TaskActionsDialog |
| Recurring/dated corrections | Keep exact occurrence/series distinction, edit wording and schedule/history boundaries. | TaskComponents, TaskMutationReceipt, repository boundaries |
| Task collection navigation | Keep Today/Inbox/Upcoming/History with Completed versus Archived within History. | TaskWorkspacePolicy, TaskAreaContent |
| Upcoming views | Keep List/Agenda/Calendar only where supported and explicit recurrence window; no misleading dates on Inbox. | TaskWorkspacePolicy, TaskMonthPlanner |
| Sort/filter/group recipes | Keep named persisted filters and independent Area scope; planning copy retains applicable task criteria. | SavedTaskFilter, matchesTaskDateFilter |
| Bulk selection/reorder | Keep focused selection chrome, explicit hidden-selection count, target revisions and full-list reorder ownership. | TaskAreaContent, TaskBulkEditDialog |
| Archive/reopen/delete | Keep separate reversible lifecycle and reviewed permanent removal, dependency counts and exact source snapshots. | PermanentTaskBatchDeleteDialog, TaskPermanentDeleteRoute |
| Global Add | Keep destination-aware creation and explicit Area choice when required; no extra creation system. | globalAddAvailable, RootEditorHost |
| Cross-feature search | ES2 keeps exact source routes, structured terms, All/Any semantics and truthful bounded/partial disclosure while reducing wasted work. | UnifiedSearchDialog, WhipSearchQuery |
| Search refresh | ES2 keys Gym and Goal contributions to content, binds matches to their source index, marks rebuild loading and preserves query/focus. | GymSearchContent, SearchResultsSnapshot |
| Search/historical identity | Keep actual Task destination resolution and archived source routing rather than pretending current visibility. EP5 adds local full-loaded-history lookup where global history is deliberately capped. | TaskNavigationIndex, source selection routes |
| Review/Trends | Keep source-aware unavailable/loading notices and retry; neutral and targetless outcomes retain their own meaning. | ReviewAvailability, ReviewEvidence, ReviewOutcomes |
| Area lifecycle | Keep request-owned creation/rename/color/move/merge/archive/delete with exact impact and saved-scope reconciliation. | AreaManagementDialog, AreaScopeMenu |
| Tag lifecycle | Keep search/manage/rename/archive and same-context undo receipt ownership. | TagManagementDialog |
| External entry points | Keep restored launch queue, explicit source identities and unavailable recovery; data reset invalidates old routes. New planner request also resets. | MainActivity, resolveLaunchTarget, returnToHomeAfterDataReset |
| Adaptive navigation | Keep compact bottom bar, persistent wide rail, fold-pane geometry and focused editor navigation ownership. New modal reuses pane-aware placement. | AdaptiveNavigationFrame, whipScreenDialogGeometry |
| Dialog ordering | Ordinary work precedes analysis/history, options and danger; EP1/EP3 place execution in Overview, EP2 offers explicit completion when earned, EGS3 puts recovery before administration. | EntityInspectorDialog, SettingsContent |
| Design roles | Keep shared spacing, typography, surfaces, notices, action rows, toggles, search and button roles. New workflow controls use existing roles; no parallel styling system. | ProductivityEditorComponents, WhipActionList, WhipSearchField, theme |
| Reusable components | Extract Goal and Habit checklists from collection cards to their inspectors; use actual TrackEntryField for preview; extract TaskDayPlanDialog from the large shell. Domain-specific semantics stay local. | GoalMilestoneChecklist, HabitChecklistItems, TrackAuthoringExperience, TaskDayPlanDialog |
| Extensibility | Preserve pure domain models, repository mutation boundaries, receipt-owned ViewModels and stateless UI surfaces. No new dependency, database schema, backup format or generic form framework. | Feature repositories/ViewModels, new pure helpers |
| Architecture documentation | Correct obsolete Health Connect integration description to retirement plus idempotent historical recovery, consistent with DEC-20260910-026. | docs/architecture.md |

### Speed assessment and measured scope

The confirmed recurring waste is addressed in EGS4/ES2. Same-process synthetic observations: 2,000 search rows have median previous matching/ranking **9,193,637 ns** versus compiled-query **1,270,196 ns**; a 200-exercise/120-set Gym fixture has median previous calculation **46,006 ns** versus clock-only copy **1,695 ns** per tick. Both exact methods assert equivalent behavior and impose no timing threshold. These are CPU-work observations, not physical-phone frame-rate or startup improvements.

Large Habit history now uses lazy rows; Habit/Goal searchable display text is prepared per source snapshot, then filtered before pagination. Plan My Day composes lazy candidate rows and keeps confirmation outside the list. Existing global history/result bounds remain explicit. Repository writes retain transactions and concurrency boundaries; projections retain their established source ownership. Baseline first debug launch on the fresh emulator measured 3,565 ms, a single unoptimized startup observation with no controlled comparison, so this audit makes no startup regression or improvement claim.

EP3 source clarification: the existing authored `autoCompleteFromItems` setting completes a Habit after its last active item when enabled; when disabled, parent completion is explicit. Unchecking does not silently reopen the parent. The initial plan's unconditional “explicit” wording was too broad and is corrected above; this audit preserves both established modes rather than changing stored user intent.

### Focused verification checkpoint

Initial production compile found a missing RoutineExercise import and failed in 29 seconds; corrected. The combined follow-up compile/test exceeded the 55-second limit and is incomplete. A separate production compile passed in 46 seconds, then the exact search reference/equivalence/performance method passed in 14 seconds including Gradle setup (0.49 seconds test execution). Eight selected domain/policy methods passed in 11 seconds including setup. Native changed-state acceptance and visual inspection remain pending at this checkpoint; full, readiness and candidate batches are deliberately excluded.

### History continuity follow-through (FB-20260928-008)

The owner's repeated-cleanup report adds EP6 before its implementation. Current Habit log/pause callbacks explicitly destroy the inspector before opening their editor, losing History/query/list state. Goal measurement editing and its general receipt handler similarly dismiss the parent. Preserve those parent compositions behind their child dialogs, then dismiss only the completed editor/review; keep existing authored save/error ownership and exact historical IDs. Whole-entity lifecycle changes and elapsed-origin reset still respect their definition boundary and intentional navigation.

Analogous current-source review: Track's selected definition and Entries search/filter/sort/list remain composed behind its Entry overlay; deletion clears the child candidate/route only. Task occurrence Reopen/Reset callbacks do not clear the inspector; an operation on its selected completed occurrence can legitimately remove that specific completed context, whereas other historical occurrence actions leave it open. Task subtasks remain inside the inspector. Gym SetUpdated clears only the child Set editor, WorkoutSetRemoved clears its local confirmation and preserves workout context, and retained History sessions stay expanded after detail edits. Removing the entire selected Workout invalidates that session and remains in History. Routine Set/placement edits retain the builder. Settings unit editors and emoji menus close only their child surface. These are concrete keeps; native proof focuses on the affected Habit/Goal repeated-edit path and retained Track context.

Deeper Track paging review found an EP6 defect despite correct overlay ownership: every content change reloads just the first 100 Entries, collapsing any Show More window after an old record is changed or after recreation. Accept the additional remedy before editing: retain the requested loaded window with saveable state, reset on actual query/filter/sort change, and refill it using existing bounded 100-row repository calls. Keep cancellation/stale-query/error handling, and only increase the window after a successful Show More. Add focused loaded-window correction/recreation proof; do not introduce a new paging framework.

Fresh short-screen 200% inspection also refined ES1: Preview now reveals the proposed tasks immediately, and a failed Apply reveals its error while retaining selection. The original setup-position capture remains in `diagnostic-before/`; the final exact fixture verifies the proposed summary without manually scrolling, then reaches the sixteenth candidate with Apply still fixed and visible. This is completion of the same planning workflow, not an additional deferred recommendation.

The subsequent wider 1800×2400 original exposed clipped bottom card/action chrome behind Android's tablet taskbar despite a passing visible-label assertion. Accept a shared ES1/design-role follow-through before editing: non-primary `ProductivityEditorDialog` currently opts out of platform inset fitting but applies only IME padding. Apply safe drawing insets at that shared owner, preserving primary editor behavior; strengthen the wider fixture to bound the whole Apply control above the system inset. Recheck the short IME and representative affected child-dialog paths. Do not treat the earlier wide image as clean visual acceptance.

## Delivery and acceptance map

All twelve remedies are implemented. Exact method names, failed attempts, timing, binary hashes and fresh originals are retained in the [evidence receipt](../../artifacts/experience-overhaul/2026-09-28/README.md); the domain reports assess their actual screenshots. Ten distinct JVM methods and sixteen distinct Android methods have passing evidence. Final shared-inset acceptance passes the wide whole-action bounds, short actual-200%/IME planner, large Track preview, backup disclosure/child dialog and repeated Habit/Goal history cleanup. Every passing native invocation takes 8.557–42.301 seconds including guard/process overhead. No full/readiness/candidate batch ran.

| Group | Delivered implementation | Focused evidence |
| --- | --- | --- |
| EP1 | Shared actionable Goal milestone checklist in card and inspector, live projection with definition-boundary protection, owned mutation/failure | `goalMilestonesWorkInsideDetailsAndCompleteOnlyOnRequest`; working-milestones original |
| EP2 | Explicit reached-target completion alongside progress; ongoing/elapsed rules preserved | Same native journey plus `completionOpportunityRequiresAnAchievedActiveFiniteOutcome` |
| EP3 | Shared Habit checklist in Today/card with committed mutation and reminder follow-up; authored auto-complete policy retained | `habitChecklistWorksInsideTodayAndRetainsParentCompletion`; actual 200% working-checklist original |
| EP4 | Four editable Track starters, dirty-draft replacement review and disposable preview through actual field renderer | `trackStartersAndPreviewKeepAuthoredDefinitionsSeparateFromSampleFacts`, `trackStarterPreviewUsesRealFieldsWithoutSavingSampleValues`; actual 200% preview original |
| EP5 | Saved Habit/Goal history query over prepared original facts; filter before pagination; lazy Habit rows | Two original-history JVM rules plus `historySearchFindsOlderOriginalFactsAndRestoresItsQuery`; oldest-match originals |
| EP6 | Habit/Goal child editing retains parent History; Track preserves requested older window/viewport across content changes and recreation | `repeatedHistoryCleanupKeepsQueriesAndRecoversFromStaleGoalDeletion`, `trackHistoryChangesKeepLoadedOlderWindowAndSearchAcrossRecreation`, existing `changedQueriesCannotShowStaleMatchesAndFailuresRemainRetryable`; retained-history originals |
| EGS1 | Bounded returning-user workout launchpad through exact existing start/history commands; first-use preserved | `workoutLaunchChoicesAndRoutineSearchKeepAuthoredScope`, `workoutLaunchpadKeepsExactChoicesAtLargeText`; launchpad/first-use originals |
| EGS2 | Saved multi-term routine-content search/count/clear, explicit archived scope and safe reorder behavior | Same pure rule, `routineSearchRestoresScopeAndLaunchesTheExplicitDay`; routine-search and started originals |
| EGS3 | Protection summary and core backup/restore before folder/CSV disclosures; warnings and settled results remain visible | `backupOverviewKeepsRecoveryVisibleAndAdministrationRestorable`, `deepDataOperationMakesItsSettledResultVisible`, `plainAndCsvExportsKeepTheirRequestedFormats`; protection/warning/result originals |
| EGS4 | Clock-only elapsed/rest update, data/settings-owned totals | `clockTickPreservesProjectedTotalsAndOnlyUpdatesTime`; equivalent totals and bounded timing observation |
| ES1 | Home/Today/Inbox entry, shared lazy planner, fixed actions, retained draft/error, preview/error reveal and shared safe drawing insets | Exact policy method plus three `TaskDayPlannerUiTest` journeys; normal/light, short/200%/IME and wider/taskbar originals |
| ES2 | Prepared query, content-only Gym/Goal invalidation, index identity ownership and loading on rebuild | Two exact `UnifiedSearchRulesTest` methods and `workoutClockAndChangedContentKeepSearchResultsCurrent`; current-content dark/IME original |

Visual hierarchy uses existing shared roles: work inside Overview, earned completion next to progress, focused planning with fixed confirmation, starter/preview before schema commitment, useful next workout choices, and protection before backup administration. No parallel visual system, generic form engine, dependency, storage migration or backup-version change was introduced. The inspector checklists and Track field renderer are reused at their actual equivalent interaction sites; new pure helpers and the extracted planner reduce shell coupling.

The source matrices cover discovery through authoring, execution, correction, history, archival/removal and recovery, including cross-feature routes and retired integrations. Accessibility evidence includes actual Android 200% text, scrollable long content, labelled controls, IME and whole-action system-inset checks. It does not claim fresh execution of every disposition, TalkBack speech, RTL, every fold/device/OEM condition or a physical-phone speed result.

Final reconciliation: every accepted row above has delivered source, selected passing evidence and inspected rendering where applicable; all 137 source-review rows have an implement/keep disposition. The measured CPU improvements preserve behavior, existing data formats are unchanged, and no accepted recommendation remains open. Delivery and source/evidence integrity are recorded in VER-20260928-007. This completion does not imply a new phone release.
