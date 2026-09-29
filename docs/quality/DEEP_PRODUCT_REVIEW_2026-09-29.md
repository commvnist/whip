# Whip deep product review — 2026-09-29

Status: Investigating. FB-20260929-002. Baseline `04bc58e2`, released 0.3.83/code 89. The owner rejected the depth of the prior audit; its completed changes remain historical facts, not evidence that the remaining experience is satisfactory.

## Method and completion contract

1. Start with user intent and inspect complete paths: getting started, capturing/organizing a busy day, executing recurring work, tracking progress, designing/using a custom log, planning/completing training, and managing/recovering data. For each path examine entry visibility, choices, taps/scrolling, hierarchy, field order, confirmation, interruption and subsequent correction. Include mature data, competing tasks and alternate domain modes.
2. Inspect current rendered interfaces and trace their state/domain/persistence owners. Prior screenshots and test counts are navigation aids only. A source inventory is not an assessment of interaction quality, and a passing assertion is not visual approval. Document actual before/after rationale, including alternatives where a substantial redesign is warranted.
3. Publish concrete findings and accepted remedies before production edits. Implement every accepted item across its relevant callers, including reusable components and recovery. Do not invent abstractions or data semantics to inflate the scope; do not cap findings at a convenient number.
4. Use exact, selected checks only when their result can guide an edit or establish acceptance, each routine invocation bounded with `timeout --kill-after=3s 55s`. No full, readiness or candidate batch. Capture and personally inspect changed interfaces, with relevant compact/large-text/keyboard/wide states. Failed and incomplete attempts remain failures/incomplete.
5. Reconcile each accepted remedy with implementation, behavior evidence and rendered outcome. Report what was directly executed versus source-reviewed. Update durable memory, commit/push coherent verified chunks, and close only when the full accepted plan is delivered.

## Ownership

- Parent: Home, Tasks, onboarding, search/navigation/organization/Review, cross-feature design and reusable controls, architecture, integration, native observation and final verification.
- Existing productivity agent: Habits, Goals and Tracks, with independent critique of previous keeps and complete alternate-mode/mature-data journeys.
- Existing Gym/Settings agent: training creation/execution/correction and administrative/recovery jobs, with independent critique of previous keeps.

Only the two previously authorized agent slots are reused. Parent serializes builds and device operations. Physical phone remains on the verified release; synthetic disposable emulator data is used for investigation.

## Investigation questions

- Can a person identify and execute the next useful action without reading a manual, opening Options or traversing repeated dialogs?
- Does the normal screen emphasize the work itself, or is it dominated by navigation, filters, explanations, repeated summaries and empty space?
- Do creation flows ask the right questions in a useful order, expose necessary context, and allow safe correction without losing drafts or surprising meaning changes?
- Can users find and repair multiple records, compare evidence, or change scope without losing their place or repeating setup?
- Are domain concepts, status, dates, units, completion and failure explanations consistent and true across their consumers?
- Which repeated controls/behaviors should be shared, and which superficial similarities conceal genuinely different domain rules?
- Where do data volume, clock updates, query changes and lifecycle recreation cause avoidable work or stale/incoherent state?
- Would a simpler information hierarchy or fewer decisions materially improve a complete job, and can it be delivered through existing owners?

## Accepted implementation plan

Accepted before production edits. Linked reports contain exact source traces, before/after rationale, compatibility and selected acceptance scenarios. Interaction savings are structural analysis, not measured user timings.

### Gym and Settings

All six remedies in [the independent Gym/Settings review](DEEP_PRODUCT_REVIEW_2026-09-29_GYM_SETTINGS.md) are accepted:

- DGS-1: reachable workout navigation/finish, a saveable one-Set quick-logging choice preserving queue/optional policies and drafts, collapsed completed Sets, exact successful finish→History.
- DGS-2: completed/compatible previous suggestions converted to receiving units, truthful snapshot-based labels and effort before submission. Include quick draft→full editor handoff and refresh only after its exact owned save; cancellation retains the quick draft and external conflicts remain detected.
- DGS-3: essential exercise units/load meaning before optional metadata; bounded searchable category assignment in the existing shared editor.
- DGS-4: retain routine child exercise/machine drafts through exact request/result ownership, busy/failure/retry/recreation, and constrained quick-machine layout.
- DGS-5: local remembered-purpose Settings search opening the actual control/disclosure, distinguishing Mass from Gym units.
- DGS-6: recent performed exercise as initial Progress source; content-keyed graph/weekly calculations and structure fingerprints preserving exact evidence/formula semantics.

### Habits, Goals and Tracks

All six remedies in [the independent productivity review](DEEP_PRODUCT_REVIEW_2026-09-29_PRODUCTIVITY.md) are accepted:

- DP1: execution-first Habit Today, shared numeric quick/Set Total/Undo controls plus clearly additive Add Amount; focused Insights reuse the existing chart/evidence body. Preserve parent History and child failure/draft context.
- DP2: completion-based Goals explicitly record one completion without artificial numeric entry; unrelated edits preserve original values, deliberate outcome changes write 1/0.
- DP3: searchable scoped entity collections/archives/Insights, retained query/context, clear recovery, safe full-list reorder and visible-only Track selection.
- DP4: compact milestone name rows with optional Weight & Reward disclosure, stable identities and auto-revealed invalid details.
- DP5: shared Track detail analysis/date scope, explicit inclusive recent/all/custom ranges, date-spaced numeric/scale trends and accessible original facts, exact View Matching Entries→correction→return.
- DP6: discoverable remaining configured Track list fields through the existing shared row, preserving order/unit truth without expanding every field by default.

### Home, Tasks and Review — accepted

- DS1: Balance Home across domains. Fresh baseline `shared.home.populated.png` shows the existing hierarchy with one Task; `HomeContent` renders *all* Today Tasks, due Habits, pinned Tracks/routines and pinned Goals before subsequent sections. A 20-Task day therefore forces twenty record rows before Habits. Use consistent three-item previews ordered pinned first, truthful total counts and explicit View All continuation for every truncated section; retain direct completion and the authored section order. Keep saved Task filter choices in one horizontally scrolling row instead of unbounded wrapped rows. Alternative rejected: hide the whole section or auto-change user order. Acceptance: dense mixed Home with many pins can reach the next domain, full count/continuation opens the correct collection, completing a preview refills it, large-text controls remain reachable.
- DS2: Find and repeatedly manage Tasks in context. `TaskAreaContent` exposes current-list search only inside Sort, Group & Filter; Save These Filters dismisses that parent before opening a name dialog, and Cancel/Save returns to the collection. Add a visible Find Tasks action with an inline shared search field/count, keep it separate from quick capture to avoid competing keyboards, and retain the filter parent through name Save/Cancel. Saved queries and ordinary filters keep one existing owner; no new filter engine. Acceptance: query→open/return/recreate→clear and nested save/cancel retain results and filter context; reorder still clears constraints explicitly.
- DS3: Make the Task inspector an execution surface. `TaskActionsDialog` puts context before subtasks and hides Focus presets under Options; scheduling is a separate Activity visit even when the user needs a first date. Put subtasks first when present, expose the existing Focus controls in Overview, and make the existing schedule action available beside its timing fact. Keep Activity for series history and Options for lifecycle/organization. Retain child drafts, replacement confirmation and exact focus request ownership; canceling a child stays in the originating section. Acceptance: direct Overview subtask/focus/schedule journey plus short large-text action reachability. No new timer or completion semantics.
- DS4: Preserve the review-and-correct loop. `ReviewAppRoute` invokes `onDismiss` for every source outcome; opening its original record destroys Review's detail selection/list state and closing the record leaves another workspace. Retain Review's existing saveable subtree and provide an explicit Return to Review path in the destination, with Back returning after the child closes; this works across Tasks/Habits/Goals/workout History and through recreation. Do not clone record editors into Review or keep an invisible modal intercepting source interaction. Also key Review outcomes/signals/correlations to actual content/date/zone/availability, rather than whole clock-ticking feature states. Acceptance: two source records inspected/corrected without reconstructing Review period/detail/position; source deletion refreshes outcomes; exact domain score/date semantics remain unchanged and clock-only ticks reuse projections.

No schema, migration, dependency, generic form framework or second persistence coordinator is warranted. Parent serializes focused verification and inspects original captures. Source completion alone does not close these items. Further concrete findings require reconciliation before their edits.

### Shared follow-through accepted before repair

- DS3 also retains the Task inspector behind its definition editor and on Pin changes; current shell clears its key on both actions, while the editor is composed before the inspector. Reorder those owners so the child is topmost and clear only the child after save/cancel. Keep the inspector's scroll owner outside temporary Focus dialogs.
- Fresh `deep.tasks.execution-overview.png` shows each compact subtask spending a second full row on the secondary Convert to Task command. Move conversion into the existing per-row overflow pattern while keeping the checkbox direct; all card/inspector callers share this row. A confirmation remains explicit and cancellation returns to the same subtask context. Fresh `deep.tasks.inline-find.png` shows redundant query chip/clear controls and automatic IME reopening after recreation; hide the duplicate query chip while the query field is visible and request focus only on explicit Find, not restored composition.
- DS4 packaging exposed a real instrumentation failure in `WhipAppKt` after adding root saveable Review ownership (`shared-package-2.log`). Extract that ownership into the existing Review feature boundary rather than disabling coverage or adding a second router. The root's existing Back handler must prioritize the retained Review return after source-child Back handling; a separately registered earlier handler would lose to the existing destination handler.
