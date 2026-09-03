# Whip top-down UX, UI, functionality, and QA audit — 2026-08-27

## Status

Complete. Implementation, compact/expanded verification, exact-set automated gates, release builds, and the independent senior-QA confirmation all pass. This is the durable source of truth for the product-quality goal; every finding is implemented and verified, deliberately dispositioned with a reason, or retained as explicit non-blocking debt.

The objective is refinement, not a wholesale redesign. Whip's primary navigation and overall layout remain intact. The work should make every surface feel authored by the same product team: consistent controls, language, hierarchy, feedback, responsiveness, and cause/effect.

## Non-negotiable constraints

- Do not bump `versionCode` or `versionName` without explicit instruction.
- Preserve user data and avoid destructive testing on the physical phone.
- Use disposable emulators for instrumentation, adaptive-layout, and destructive verification.
- Prefer one shared interaction grammar over local one-off controls.
- Remove or consolidate a feature when it duplicates a clearer existing path and has no distinct user value.
- A passing test count is not proof of coverage. Tests must assert meaningful user-observable outcomes and persisted consequences.

## Product-quality questions

Every audited feature must answer:

1. Can a first-time user predict what to do without documentation?
2. Does the control look and behave like the equivalent control elsewhere in Whip?
3. Does the result match the label, including navigation, persistence, side effects, undo, and recovery?
4. Is the feature necessary, or is another feature already solving the same job better?
5. Does the common path remain fast while advanced capability stays discoverable?
6. Does compact, expanded, book-fold, large-font, keyboard, TalkBack, and empty/loading/error state behavior remain coherent?
7. Can automation prove the input → action → output → persisted consequence chain?

## Audit rubric

| Dimension | Release-quality expectation |
| --- | --- |
| Discoverability | Primary actions and state changes are understandable on first inspection. |
| Consistency | Equivalent jobs use the same components, wording, placement, and feedback. |
| Directness | The shortest safe path is the default; dialogs and modes earn their cost. |
| Feedback | Motion, state, and recovery explain what happened without snackbar/status spam. |
| Spatial behavior | Dragging, resizing, keyboard display, and folds preserve the user's mental map. |
| Correctness | Inputs validate clearly; outputs, navigation, persistence, and side effects agree. |
| Resilience | Empty, filtered, archived, interrupted, invalid, and restored states remain safe. |
| Accessibility | 48dp targets, semantics, focus, non-gesture alternatives, and large text are first-class. |
| Testability | Stable semantics expose user intent; tests assert outcomes rather than implementation trivia. |

## Product surface inventory

### App shell and shared systems

- Compact navigation bar, expanded/navigation-rail layouts, book fold support pane, keyboard/inset behavior.
- Global add, area scope, search/filter/sort, selection, editor/dialog placement, feedback/undo, loading/error states.
- Shared cards, headers, empty states, menus, choice controls, dates, units, identity emoji, reorder controls.

### Home

- Today/support pane, empty onboarding, section ordering/visibility, quick actions, item inspectors.

### Tasks

- Today, Inbox, Upcoming list/agenda/calendar, History, smart capture, editor, recurrence, reminders, subtasks, filters, manual order, completion and undo.

### Habits

- Today/check-in, checklists, quantitative logging, skip/fail, recurrence, history/insights, editor, pinned/manual order, reminders.

### Goals

- Goal types and calculations, measurement entry, milestones, pace/status, editor, pinned/manual order, history and source links.

### Tracks

- Track library, schemas/fields/choices, entry capture/editing, search/filter/sort, analytics, automation/link behavior, archive/export, manual order.

### Gym

- Active workout, quick-set entry, exercise substitution, supersets/circuits, rest timers, history, progress/graphs/records, exercise/machine/category/routine libraries, routine builder, tools.

### Settings and platform surfaces

- Appearance/layout, home configuration, Areas, units, health/notifications/privacy, custom emoji, backup/import/export, widgets/shortcuts/share intents.

## Drag/reorder audit

### Current implemented contract

- A dedicated, low-emphasis 48dp handle accepts immediate vertical or horizontal drag while the complete owned item lifts and follows it.
- Live targets use measured row/card geometry. Crossed siblings animate into the vacated slot and the preview clamps to the collection's true first/last position.
- Persistence commits once on release; cancellation clears the visual preview and performs no write.
- Pickup/drop haptics, Alt+arrow keyboard movement, TalkBack custom actions, position/total semantics, live target announcements, and post-move announcements exist.
- Vertical lazy collections and horizontally scrolling Routine days edge-autoscroll while a handle is held near an edge.
- Reordering is constrained or disabled for ambiguous searched, filtered, archived, scoped, pinned, or non-manual lists.
- Entering Task reorder persists **Custom Order** as the active presentation, so a successful move still looks moved after leaving the screen or cold restarting the app.
- Group blocks and members have separate reorder scopes in workouts and routines.
- Repository normalization protects omitted/hidden records and legacy group integrity.

### Critical first-instinct assessment

| ID | Finding | Severity | Initial decision | Status |
| --- | --- | --- | --- | --- |
| DRAG-001 | The user needed an explanation of what could be dragged. Discoverability therefore failed even though the gesture technically worked. | High | Rework entry points and contextual guidance; do not rely on documentation. | Implemented: browse surfaces expose `Reorder …`, one shared mode bar, instruction, and Done action. |
| DRAG-002 | Only the handle translates. The row/card stays behind and then snaps after release, which can feel like an unfinished button gesture rather than direct manipulation. | High | Prototype a shared spatial drag presentation where the authored item/block visibly lifts and follows the gesture. | Implemented and emulator-verified: the owned card/block lifts, shadows, and tracks the gesture. |
| DRAG-003 | No insertion placeholder or explicit destination is shown before release. Users cannot confidently predict the final slot for multi-row movement. | High | Add a shared drop-position preview tied to actual list geometry. | Implemented: measured sibling displacement creates the destination slot; variable-height regression added. |
| DRAG-004 | Long lists do not edge-autoscroll, preventing useful drag movement beyond the visible viewport. | High | Add bounded edge autoscroll for lazy lists or provide an equally direct accessible move-to-position path. | Implemented for vertical lazy lists and horizontal Routine days; instrumentation covers the vertical long-list path. |
| DRAG-005 | `Manual`, `Custom Order`, and `Reorder` describe the same concept differently, and some surfaces expose handles immediately while others require a mode. | High | Define one cross-app authored-order vocabulary and mode grammar. | Implemented: **Custom Order** is a sort choice; **Reorder** is an action/mode. |
| DRAG-006 | Handles disappear for single items and constrained boundaries, but the UI often does not explain why reorder is unavailable under search/filter/scope. | Medium | Show concise contextual guidance only when the user has expressed reorder intent. | Implemented: explicit clear/search/scope transition commands; browse modes reserve a stable lane where appropriate. |
| DRAG-007 | Reorder is not item transfer. Users may reasonably expect dragging a task to a date/Area or an exercise into a workout. | Medium | State and visually scope reorder precisely; separately evaluate high-value transfer gestures instead of implying universal drag-and-drop. | Dispositioned: current gesture is deliberately labelled Reorder; semantic transfers remain explicit commands until a safe drop/undo contract exists. |
| DRAG-008 | Pickup and drop use the same long-press haptic token even though they represent different events. | Polish | Review available haptic vocabulary and use distinct, restrained feedback where supported. | Implemented: pickup and positional/drop feedback use different available Material haptic tokens. |
| DRAG-009 | Whole-row drag would conflict with open/check/edit actions; the dedicated handle is necessary, but its icon/placement must read as movable rather than as decoration. | Design constraint | Keep a dedicated affordance; improve its relationship to the moving surface. | Accepted |
| DRAG-010 | The primitive clamps the grip to 75% of one 48dp threshold (36dp) even when a drop requests several positions. Visual travel and final movement can therefore disagree dramatically. | High | Replace distance-to-steps-at-drop with live spatial targeting; the visible item and final destination must stay congruent. | Implemented with measured geometry and hard collection-boundary clamping. |
| DRAG-011 | The normal grip is an opaque elevated-looking square, so it reads as a button and adds visual weight before the user interacts. | Medium | Keep a 48dp hit target but use an icon-only/low-emphasis resting treatment; add container/elevation only while grabbed. | Implemented. |
| DRAG-012 | No grabbed state, live target announcement, cancellation announcement, or post-drop live-region feedback exists. | High | Add a semantic drag state and restrained announcements/haptics at meaningful position changes. | Implemented for live target and post-move position; cancellation restores silently without a false success announcement. |
| DRAG-013 | Returning no handle at a one-item/boundary-only collection causes the content lane to shift when a second reorderable item appears. | Polish | Reserve/animate the lane in explicit reorder modes and clamp unavailable-direction motion. | Implemented: reserved only in explicit browse modes; inline authored one-item controls stay absent. |
| DRAG-014 | Pinned boundaries differ: Goals/Habits/Tracks visibly partition and block cross-boundary moves, while Task Custom Order ignores pin state and can appear undone when switching back to Smart sort. | High | Define one invariant. Initial safe implementation: persistent section boundaries with `Pinned items reorder separately`; evaluate atomic cross-drop pin toggling only with transactional preview/commit support. | Implemented across Tasks, Goals, Habits, and Tracks; repository normalization and tests preserve the invariant. |
| DRAG-015 | Reordering filtered/scoped subsets is sometimes hidden, sometimes disabled, and sometimes explained passively. | High | Never reorder a partial collection. Offer an explicit `Clear filters & reorder all` transition, preserve hidden/archived relative order, and make scope visible. | Implemented with explicit clear/show-all commands and transient—not persisted—Area scope. |
| DRAG-016 | In nested Gym structures, group and member grips can look equivalent even though one moves a block and the other moves within it. | Medium | Preserve contiguity and distinguish visual/semantic labels: `Move group` versus `Move exercise`. | Implemented with independent block/member/set scopes and semantic labels. |
| DRAG-017 | Task manual positions persisted, but a cold launch returned the screen to Smart sorting. The move therefore looked undone even though the repository was correct. | Critical correctness | Persist the active Task sort presentation and include it in settings backup; selecting another sort remains the explicit way to leave Custom Order. | Implemented after emulator discovery; focused settings/backup/reorder tests and compact/expanded cold-restart evidence pass. |

### Reachable reorder surface accounting

The design audit found 27 production invocations; one is inside an unreferenced `legacy-routine-editor`, leaving **26 reachable reorder surfaces**:

- 8 browse collections: Tasks, Goals, Habits, Tracks, Areas, Exercise Library, Categories, and Routines.
- 4 active-workout placements: group, exercise, focused set, and other sets.
- 14 authored/editor collections: Task subtasks; Goal milestones; Habit checklist; Track fields and choices; Settings Home sections and custom emoji; Routine days, groups, placements, prescription schemes, and sets; tracked exercises and metrics.

Final source accounting: the legacy editor was removed and every reachable whole-item handle now declares a measured layout scope. No reachable whole-item call remains on the old fixed-threshold overlap fallback.

Policy decision to validate with the UX review:

- Browse collections use an explicit, visible Reorder mode with one instruction and Done action.
- Authored editor sequences may keep always-available grips because order is part of authoring.
- Active Workout needs a contextual policy that does not permanently tax every high-frequency set row with an unexplained 48dp lane.

### Drag acceptance criteria

- A first-time user can discover reordering from the screen itself.
- The dragged item or group—not merely the icon—visibly lifts and tracks the gesture.
- The destination is visible before release, including multi-position movement.
- Long lists can be reordered beyond the current viewport.
- Search/filter/scope constraints are explained at the moment they matter.
- Equivalent lists use the same label, entry mode, handle placement, feedback, and accessible alternatives.
- Pinned sections, hidden completed sets, groups, archived data, and omitted repository rows cannot corrupt order.
- Tests perform real gestures and assert visual/semantic state, persisted order, boundaries, cancellation, and recreation.

## Cross-app UX/UI audit backlog

This section will be populated from source inspection, emulator traversal, and the independent UI/UX reviews. Findings must prefer shared solutions before screen-specific patches.

| ID | Surface | Finding | Severity | Decision | Status |
| --- | --- | --- | --- | --- | --- |
| SYS-001 | Shared menus/dialogs | Command menus mixed direct Material items with the shared selected/destructive/icon grammar; legacy editor chrome remained. | Medium | Migrate commands, retain direct items only for value choices, delete proven-dead UI. | Implemented for touched command surfaces; 21 remaining direct items are value pickers. |
| SYS-002 | Search/filter/sort | Scope could mutate global Area; peer tabs could move; historical projection used input order and partial reorder constraints varied. | Critical | Make search/filter scope local or transient, keep peers stable, make reset/reorder consequences explicit. | Implemented; query-backed search paging remains staged architecture. |
| SYS-003 | Feedback | Routine successes, completion, set, and checklist updates competed with obvious visual changes and produced snackbar/status spam. | High | Silent/inline for obvious reversible state; snackbar for failures, undo, destructive and non-obvious async results. | Implemented and contract-tested; running/inline-success statuses no longer surface globally. |
| SYS-004 | Responsive UI | Compact peer replacement, inspector compaction, scheduled metadata truncation, fold/IME rail movement, and crowded actions were priority defects. | Critical | Preserve stable controls and mental map; use responsive wrapping rather than hiding two-item navigation. | Implemented on reported/shared surfaces; compact/expanded evidence and existing adaptive matrix retained. |
| SYS-005 | Feature necessity | Review saved views, low-value graph presets, duplicate completion feedback, and unsupported tracked-record targets duplicated clearer paths. | Medium | Remove features that do not earn their complexity. | Implemented in prior iterations preserved by this pass. |
| SYS-006 | Authored order | Goal, Habit, and Track lists use an explicit reorder mode, while Tasks and the Exercise Library expose handles whenever an internal `Manual` sort is selected. User-facing language alternates between `Manual`, `Custom Order`, and `Reorder`. | High | Standardize on **Custom Order** for a sort choice and **Reorder** for an action/mode; add one shared, explanatory reorder-mode treatment. | Implemented. |
| SYS-007 | Authored order constraints | Tasks explain hidden reorder handles under search/filter/Area scope, and the Exercise Library explains search/filter constraints, but Goal/Track Area constraints are buried in disabled menu copy and Habit silently removes the action. | Medium | Use the same concise explanation at the point of expressed intent; never present a disabled action without explaining how to enable it. | Implemented across Tasks, Goals, Habits, Tracks, Exercise Library, Categories, Routines, and Areas. |
| SYS-008 | Single-section inspectors | Track Entry passes a no-op section callback to a one-section `EntityInspector`. This is not itself a dead button if the inspector omits navigation for one section, but it is an architectural smell worth a contract test. | Low | Verify the shared inspector hides section navigation when only one section exists; keep callback internal and non-interactive. | Verified in the shared architecture contract: one-section inspectors omit navigation. |
| SYS-009 | Reorder placement | Vertical grips are leading on most collections, but trailing on Track fields, bottom-right on Goal milestones, detached in Home settings, and attached to the selected day editor rather than the reordered day chip in Routine Builder. | High | Standardize a leading reorder lane for vertical collections and attach horizontal reorder to the object actually moving. | Implemented on Track fields, Goal milestones, browse collections, Routine days, and nested Gym structures; Home keeps its labelled section control row. |
| SYS-010 | Responsive action lanes | Several rows combine a 48dp grip with completion, title, overflow, edit, remove, duplicate, or delete controls in one fixed Row. Compact widths and large text are predictably crowded. | High | Introduce a shared responsive item/action lane; preserve primary action, move low-frequency actions into overflow when space is constrained. | Implemented for browse reorder modes by suppressing competing completion/edit/filter/add controls; shared responsive item header remains the collection baseline. |
| SYS-011 | Editor chrome | Task, Track, Habit/Goal, Machine/Exercise, and Routine editors use different Close/Back/Cancel, title, Save prominence, saving, validation, and dirty-state patterns. | High | Create and incrementally adopt one `WhipEditorScaffold` without redesigning form bodies. | Primary editor chrome implemented through `WhipEditorHeader`; request-owned save/dirty behavior remains a separate functional contract. |
| SYS-012 | Command menus | `WhipMenuItem` defines selected/destructive/icon command grammar, but 39 direct `DropdownMenuItem` calls remain. Some are valid value pickers; command menus in Goals, Habits, global Add, and Task save options bypass the shared grammar. | Medium | Keep direct items for value selection; migrate command menus to `WhipMenuItem`. | Implemented for command menus touched by the audit; remaining direct items are value-selection menus. |
| SYS-013 | Inspector navigation | Two-page inspectors now correctly keep Overview and Options visible side by side, but use bespoke outlined pills instead of the shared page-control grammar. | Medium | Preserve the fixed visibility behavior and consolidate styling/logic with shared navigation primitives. | Implemented with the shared `DestinationTabBar` contract and architecture coverage. |
| SYS-014 | Legacy UI | A private unreferenced `RoutineEditorDialog` marked `legacy-routine-editor` remains and contains a stale reorder call. | Medium | Prove no caller/reflection/test dependency, then delete it and update source contracts. | Implemented; source-contract checks updated. |
| SYS-015 | Section hierarchy | Shared `WhipSection`/`EditorSectionHeader` coexist with private Settings and raw Gym headings. | Low | Consolidate section tokens/components during touched-surface work. | Accepted; staged migration |
| SYS-016 | Saved Task Filters | Saved Task Filters remain across Tasks, Home, settings persistence, and backup. This may or may not conflict with the earlier instruction to remove Review saved views. | Product decision | Treat the prior instruction as Review-specific unless evidence says otherwise; audit Task saved filters for distinct value before retaining. | Retained: Task filters have a distinct reusable planning/Home job; Review saved views remain removed. |
| SYS-017 | Compact peer navigation | `DestinationTabBar` and inspector selectors can replace the final visible peer with a selected item formerly hidden in More. Tabs therefore move/disappear after selection. | Critical UX | Keep primary visible identities/order fixed. Represent a hidden selection as checked inside a stable More menu, or use a stable scrollable row; never swap peers based on selection. | Implemented and instrumented. |
| SYS-018 | Home filter scope | A saved Task filter chip on Home also calls global `onSelectAreaScope`, silently changing sibling Habit/Goal/Track sections. | Critical correctness | Home Task filters remain local to Tasks. Embedded Area appears in the filter label; global Area changes only through the global selector. | Implemented. |
| SYS-019 | Search scope | Opening Search and choosing cross-Area results can permanently set global Area to All/another Area. `Search all areas` is not local, and result navigation mutates a preference. | Critical correctness | Search all Areas locally. Cross-Area navigation uses the existing transient scope/Restore mechanism, never a persistent preference mutation. | Implemented with visible Restore affordance. |
| SYS-020 | Search scalability | Unified Search eagerly materializes searchable text for every historical entity/value before limiting results. Growth can produce unbounded UI memory/latency. | High | Move toward indexed/query-backed, paged domain search; immediately bound projection work and add a 10k-entry latency/memory contract. | Accepted; staged architecture |
| SYS-021 | Home customization | Home order/visibility lives under Settings > Appearance, far from the object being customized; `Home Details` is vague. | Medium | Add Home overflow > `Customize Home` reusing the current management UI; rename to `Expanded on Home` or `Show details by default`. | Implemented; direct entry reuses the current Settings management UI. |
| SYS-022 | Action language | Task subtask `Move` actually promotes to a new Inbox task, and global add menus mix nouns and actions. | Medium | Use consequence verbs (`Convert to Task`, `New Task`, `Start Workout`, `Add to Workout`) consistently. | Implemented on audited commands. |
| SYS-023 | Internal labels | Several surfaces render enum `.name` or raw names; this risks strings such as `WeightedMilestones` or group implementation names leaking to users. | Medium | Add/require explicit `uiLabel`; create a source contract that rejects unapproved user-facing `.name`. | Accepted; staged cleanup |

### UX scope policy

- **Collection browsing:** Tasks, All Habits, Goals, All Tracks, Exercise Library, Home sections, and Areas enter an explicit Reorder mode.
- **Construction/execution:** active workout, Routine Builder, task steps, Habit checklist, Track fields/options, and custom settings collections may show inline grips because order is intrinsic to the work.
- Reorder never silently crosses Area, archive, completion/status, search/filter, or Gym group boundaries.
- Semantic transfer is a separately named action/drop target, not implied by universal drag. A future pinned-divider drop may change pin state only if preview, transaction, undo, and accessibility all make that consequence explicit.

## Functional correctness audit

The audit must trace each important workflow as a cause/effect chain, not only inspect isolated screens:

`input → validation → save/action → visible result → persisted state → dependent side effects → undo/recovery → restart/recreation`

Priority risk clusters:

- Recurrence/date/time parsing and timezone boundaries.
- Filtered/manual ordering and hidden/archived data normalization.
- Completion/check-in/logging idempotency and undo.
- Grouped workout/routine execution order and rest-timer consequences.
- Substitution, deletion, archive, restore, and source-workout navigation.
- Units, machine configurations, records, graph calculations, and imports/exports.
- Cross-feature links/automation and stale references.
- Permission denial, process recreation, fold/keyboard changes, and large datasets.

## QA baseline and audit plan

Current counted baseline after the refinement additions:

- 310 JVM tests.
- 358 Android instrumentation tests, including variable-height/spacing geometry, horizontal edge-scroll, repeated accessibility-move identity, live IME geometry, and shared loading/error/retry regressions.
- 9 Macrobenchmark/Baseline Profile scenarios.
- `scripts/check` enforces unit coverage, Android-test compilation, lint, debug assembly, artifact policy, migration safety, and optional disposable-emulator instrumentation.

The number is not the conclusion. The senior QA review must classify every feature journey as:

- Full E2E cause/effect coverage.
- UI contract plus repository consequence coverage.
- Domain-only coverage.
- Manual/platform-owned verification.
- Missing or misleading coverage.

Required QA refinements:

1. Build a feature × input/action/output/persistence/side-effect/recovery/adaptive/accessibility matrix.
2. Replace brittle text-count and implementation-detail selectors with stable user-intent semantics.
3. Add real gesture tests for spatial reorder, cancellation, autoscroll, and boundary behavior.
4. Add end-to-end workflows for every high-risk cause/effect cluster.
5. Prove compact and expanded behavior without relying on unrelated tests that happen to render those layouts.
6. Run a second senior QA audit after implementation and incorporate the findings.

### Senior QA initial audit findings

| ID | Finding | Severity | Required response | Status |
| --- | --- | --- | --- | --- |
| QA-001 | Reorder tests prove the primitive callback and repositories separately, but no real app surface proves gesture → visible position → persisted order → recreation. | Critical | Add real-surface reorder journeys for representative list, editor, Gym group/set, Settings, constraints, cancellation, rapid movement, compact/expanded, and alternate input. | Implemented: real Task gesture/persistence/recreation/cancel journey plus shared measured-preview, boundary, vertical/horizontal edge-scroll, semantics, and repository coverage. |
| QA-002 | `e2e-coverage.tsv` overstates E2E: its contract checks a class and method-name string, not `@Test`, activity launch, user action, assertion, or evidence tier. | High | Introduce explicit evidence tiers and validate executable test methods; stop calling component/repository/domain checks E2E. | Implemented: the refined matrix uses explicit tiers and exact `Class#method` references; its contract resolves every class and verifies the method is annotated `@Test`. |
| QA-003 | Settings cause/effect coverage checks class names, not that a setting action changes downstream behavior. | High | Add real cause/effect tests for high-impact visual, Home, unit, Gym, reminder/permission, Health, and backup settings. | Implemented proportionately: compact layout now proves a downstream row consequence, Home visibility proves its downstream guide behavior, and exhaustive settings persistence/backup remains separate. The matrix does not claim every low-impact preference has a duplicate full-app journey. |
| QA-004 | Notification permission tests are order-dependent because one grants permission without restoring it while another assumes denial. | Critical | Add deterministic reset/restore in cleanup and an isolation lane that changes class order. | Implemented without unsafe self-revocation: each denied-permission journey establishes its own precondition, notification cleanup is deterministic, and the harness does not kill its instrumentation host. |
| QA-005 | `scripts/check --emulator` reports a source annotation count rather than aggregating executed/skipped XML across batches; `--full` does not run instrumentation and API-lane claims are not automated. | Critical | Persist and parse every batch result, require expected execution and zero skips, aggregate results, and accurately document automated versus targeted lanes. | Implemented: every batch compares its exact requested class set with XML testcase classes, aggregates executed/skipped totals, and docs distinguish full from emulator lanes. Source counting now recognizes only annotation lines rather than `@Test` text inside contract strings. |
| QA-006 | Visual checks sample only corner pixels; accessibility checks cover mostly empty screens/editors; no live IME rail-bounds test exists. | High | Add populated menu/dialog/drag/list accessibility checks, geometry assertions, screenshot evidence, and a real IME rail-position test. | Implemented: populated real-app and shared-primitive checks, exact compact/expanded captures, and a live wide-activity IME test assert the persistent rail destination bounds do not move. |
| QA-007 | Loading/error/retry UI and injected operation failures are largely untested despite prior coverage claims. | High | Add shared loading/error/retry contracts and representative operation-family failure tests proving no partial side effect and successful retry. | Implemented: the shared production component proves loading → actionable error → retry without false success, paired with transactional operation-failure tests that reject partial side effects. |
| QA-008 | Indexed selectors and hundreds of unscoped text selectors make workflows ambiguous and brittle. | Medium | Add stable intent semantics to shared controls and migrate high-risk indexed selectors first. | Accepted; continuous cleanup |
| QA-009 | Test documentation says database schema 29 while the app and migration tests are at 30. | Medium | Correct documentation and add a drift check where practical. | Implemented; documentation and release-coverage matrix now name schema 30. |

### Senior QA second-audit findings and incorporation

The second senior audit found no product build, migration, persistence, release, or drag-interaction blocker. It independently reconciled all then-current Android test classes and methods with zero missing, extra, skipped, or failed tests, and judged the drag system discoverable, spatial, persistent, constrained, accessible, and consistent across compact and expanded layouts. It did identify four QA-proof defects, all incorporated before the final gates:

| ID | Finding | Resolution |
| --- | --- | --- |
| SQA2-001 | The refined matrix was not itself machine-checked and contained invalid evidence names. | Added an executable contract for its exact schema, capability set, evidence tiers, stable manual IDs, exact `Class#method` references, and real `@Test` annotations; corrected every invalid reference. |
| SQA2-002 | The completion checklist claimed all high-risk coverage while settings consequences, live IME geometry, loading/retry, workout lifecycle, and graph-source proof were staged or under-described. | Added representative downstream settings, live IME, and loading/retry journeys; paired active-workout recreation with repository persistence; reconciled the already-existing exact graph-point source-workout journey; narrowed the checklist claim to representative high-risk coverage. True target-process kill remains explicitly platform/manual because killing it would also kill instrumentation. |
| SQA2-003 | Notification documentation claimed runtime-permission restoration although the safe harness intentionally does not revoke it. | Corrected the record: denied journeys establish their own precondition, notification state is cleaned, and teardown does not self-terminate the instrumentation host. |
| SQA2-004 | The emulator runner aggregated totals but did not prove that every requested class, and only those classes, executed. | Each batch now persists sorted requested and XML-executed class sets, fails their diff, records both class counts, aggregates testcase/skip totals, and rejects annotation text embedded in strings when counting source tests. |

Final senior-QA confirmation found no product or QA blocker. The reviewer independently reconciled 57 Android test classes and all 358 testcases with zero skips, failures, or errors; confirmed SQA2-001 through SQA2-004 resolved; rechecked the clean diff and unchanged `20 / 0.3.14` version; and classified the remaining platform, macrobenchmark-runtime, search-scale, and architectural items as explicit non-blocking limits rather than hidden claims.

Evidence tier definitions for the replacement matrix:

- **Real-app E2E:** launches the app, performs a user-semantic action, observes the user-visible result, and proves the persisted/dependent consequence.
- **Component journey:** renders a real production component and proves its interaction/output contract without the full app/data path.
- **Repository integration:** proves durable data and relationship behavior below UI.
- **Domain/unit:** proves deterministic policy/calculation behavior.
- **Manual/platform-owned:** reserved for behavior that cannot be made deterministic in the automated harness, with explicit steps and evidence.

## Evidence and iteration log

### 2026-08-27 — Audit opened

- Preserved the existing navigation/layout direction.
- Recorded the current drag implementation and its first-instinct failure: the released build required verbal explanation.
- Started independent UI/design, UX/product, and senior QA source audits.
- Confirmed the repository already contains substantial prior audit evidence; this pass will reconcile rather than blindly duplicate it.
- Confirmed the working tree contains the completed cross-app reorder implementation and associated tests; those changes are the baseline, not disposable work.

### 2026-08-28 — Top-down implementation pass

#### Drag and authored order

- Replaced fixed 48dp step previews with measured item geometry, complete-item lift, sibling displacement, target retention through repository recomposition, and first/last boundary clamping.
- Added vertical lazy-list and horizontal scrolling-row edge autoscroll. Routine days now use the horizontal host.
- Scoped all reachable whole-item reorder callsites: browse lists, active workout blocks/members/sets, Routine Builder blocks/days/schemes/sets, Task subtasks, Goal milestones, Habit checklist, Track fields/options, Home sections/custom emoji, and tracked-record exercises/metrics.
- Corrected Task Custom Order to keep pinned and other Tasks as stable sections. Browse sections now use the same Pinned/Other naming and boundary note.
- Made browse reorder a focused mode: completion, edit, quick capture, filters, overflow menus, archived disclosures, routine-start actions, and shell Add are suppressed while placement is the user's job. Back and Done exit safely.
- Kept editor grips inline because order is intrinsic during authoring. One-item inline lists no longer reserve empty lanes; explicit browse modes do.
- Added live TalkBack position state, post-move announcements, Alt+arrow/custom actions, distinct pickup/position haptics, and a stale one-item→two-item gesture-key fix.

#### Cross-app consistency and correctness

- Standardized **Custom Order** (sort choice) and **Reorder** (action/mode), plus explicit `Clear … & Reorder All` / `Show All Areas & Reorder` commands.
- Made temporary all-Area reorder/search scope non-persistent and visibly recoverable. Home Task filters remain local to the Task section.
- Stabilized compact destination/inspector peer controls so selecting a More item does not move or replace visible peers.
- Added direct Home customization entry and clarified action labels whose old nouns did not describe their consequence.
- Removed the dead legacy Routine editor and migrated audited command menus to the shared selected/destructive command grammar.
- Corrected search history selection so the newest records are retained regardless of source ordering; bounded history projection and result rendering remain in place.
- Suppressed global running and inline-success feedback for obvious visual state changes while preserving failures, destructive recovery, and Undo snackbars.
- Preserved the previously implemented Gym cleanup: no floating add buttons; scoped top-right add actions; sustainable history filtering; functional data table/source navigation; formula disclosure; machine many-to-many exercise support and configurable level direction; explicit group removal; tracked-record configuration; substitution replacement; and record de-duplication.

#### QA architecture

- Added the feature-level cause/effect matrix in `QA_CAUSE_EFFECT_MATRIX_2026-08-27.tsv`. It records input, visible output, persistence/dependent consequence, recovery/failure contract, evidence tier, executable evidence, adaptive/accessibility evidence, remaining gap, and disposition.
- Added a real-app Task reorder journey that proves gesture → visible order → repository order → cancel/no write → activity recreation, and now asserts focused-mode suppression.
- Added variable-height insertion/boundary geometry, vertical edge-scroll, horizontal edge-scroll, keyboard/TalkBack, pinned-boundary, repository normalization, and rapid/cancel coverage around the shared primitive.
- Hardened notification tests against execution order and hardened `scripts/check --emulator` to compare exact requested/executed class sets and aggregate executed/skipped XML instead of trusting source annotation counts.

#### Independent UI/UX critic reconciliation

| Review finding | Resolution |
| --- | --- |
| Variable card spacing was omitted from the first spatial-target algorithm, so a visual midpoint could disagree with the final slot. | Reorder scopes now measure both item geometry and arrangement spacing; unequal-height and boundary tests cover the calculation. |
| A Track reorder handle retained a dead semantic role after the interaction was moved into shared mode. | Removed the stale role and asserted the shared move semantics instead. |
| Browse reorder still allowed shell actions to compete with the focused placement job. | Reorder is shell-modal: Add, search, settings, Area switching, destination changes, row completion/edit, and competing list tools are suppressed or disabled until Done/Back. |
| Temporarily switching to All Areas for reorder could become a permanent global-scope change. | Added a transient Area transaction that automatically restores the prior scope when reorder ends. |
| A scoped Task list had no direct, truthful path into the complete collection it must reorder. | The menu now says `Show All Areas & Reorder`; filtered states use `Clear Filters & Reorder All`. |
| Active Workout `Up Next` could show repository order while the spatial list preview showed authored order. | All dependent projections now consume the same authored placement order. |
| Routine `Circuit` terminology promised behavior the execution engine did not actually distinguish from a superset. | User-facing grouping is truthful: `Superset`; duplicated labels such as `Circuit: Superset` and `Superset: Superset` were removed. |
| Index-based gesture identity could move the wrong item after insertion, deletion, or recomposition. | Task steps, Goal milestones, Habit checklist rows, workout sets, Routine placements, and other authored rows use stable domain keys; repeated-move regressions verify identity. |
| Area management exposed redundant move controls alongside the new direct manipulation contract. | Removed the dead/duplicative controls; explicit Reorder mode and accessible move actions remain. |
| Rep prescription schemes looked local to one routine even though they are app-wide. | Copy and placement now state that schemes are reusable app-wide. |

#### Verification-discovered refinements

- Increased Smart Capture input capacity from 100 to 200 characters. The old cap silently truncated complex natural-language commands before their reminder clause; successful parsing still replaces the command with the concise final title.
- Made exact discarded/archived workout search results render from the authoritative session collection, auto-expand, and expose the real Restore action instead of depending on a hidden browse-mode toggle.
- Rewrote Machine Library guidance to explain the physical-machine profile, many-to-many exercise links, and equipment-specific history/progress model.
- Repaired stale tests so they assert the current shared language and accessibility contract (`New Task`, state descriptions, `Close … details`, `Activity`, and stable More navigation) rather than obsolete labels.
- Kept form validation in place while adding a live-region summary and exact inline error; a rejected Task draft emits no persistence action.
- Corrected the platform-notification cleanup harness: revoking runtime permission in `@After` killed the instrumentation host before it could report results. The disposable-emulator lane now cancels notifications without self-terminating.
- Made responsive Track navigation, progress formula disclosure, exact Gym source/restore navigation, and removed per-load target assumptions executable at the current UI contract.
- During final manual drag proof, found and fixed the Task Custom Order cold-restart regression described in DRAG-017. Settings recreation and backup round-trip now cover the new preference.
- Added an app-level compact-layout consequence test, a shared loading/error/retry state-machine test, and a live wide-layout IME regression that proves the navigation rail remains geometrically fixed while typing.
- Reconciled the existing Gym chart-point journey as exact graph → source workout → History evidence instead of leaving that path incorrectly marked as missing.

#### Deliberate limits, not hidden claims

- Search is bounded and off-main-thread, but fully indexed/query-backed paging remains a future data-layer change; the current implementation should not be described as unbounded-scale search architecture.
- Platform-owned DocumentsUI, live Health Connect provider/account UI, OEM notification/battery screens, launcher rendering, signed public-APK upgrade, and true process-death workout recovery remain explicit manual/device or dedicated harness lanes in the matrix.
- Editor chrome is materially more consistent on touched surfaces, but replacing every bespoke editor with one new scaffold would be the giant redesign this goal excludes. The shared `ProductivityEditorDialog`, item header, page header, section header, and control primitives are the convergence path.
- The shared reorder primitive currently uses a short staged settling window (80 ms, with a 750 ms fallback) because its generic callback cannot observe each repository's actual asynchronous acknowledgement. Repository tests prove durable commits, but the animation must not be described as a literal persistence acknowledgement.
- `WhipScreen` remains a very large Compose method. The release compiler accepts it, but decomposing it behind stable feature boundaries is architectural debt and should be done incrementally rather than folded into a risky visual rewrite.

## Final verification evidence

### Automated gates

- `scripts/check --emulator` passed on disposable `emulator-5580`.
  - 310 JVM tests passed.
  - All 358 Android instrumentation tests executed across seven exact-class-set batches with zero skips and zero failures.
  - Aggregate execution ledger: `/root/repos/whip/build/instrumentation-results-FhavC0/aggregate.tsv`.
  - Enforced coverage: deterministic domain lines 81.35% (3045/3743), deterministic domain branches 53.91% (1736/3220), core settings/policy lines 63.46% (382/602).
- `scripts/check --full` passed after the final behavioral change.
  - Debug and release compilation, lint and release lint-vital, R8/minification, optimized resources, release APK/AAB, and the optimized benchmark harness all built successfully.
  - The nine Macrobenchmark/Baseline Profile scenarios are compiled release-lane assets; `--full` builds but does not execute them, and this audit does not overstate them as runtime results.
- `git diff --check` passed.
- Version remains deliberately unchanged at `versionCode = 20`, `versionName = 0.3.14`.

### Emulator visual and cause/effect proof

All files were captured from disposable `emulator-5582` with `scripts/device-artifacts`; no physical-device instrumentation or destructive action was used.

- `docs/quality/evidence/reorder-compact-final.png` / `.xml`: latest-APK compact Reorder mode, complete instructions, stable leading handles, grouping, and Done action.
- `docs/quality/evidence/reorder-expanded-final.png` / `.xml`: latest-APK expanded Reorder mode with the same controls and vocabulary; no unnecessary compact-only disclosure pattern.
- `docs/quality/evidence/reorder-expanded-after-drag.png` / `.xml`: real handle drag changed the semantic order to Alpha → Charlie → Bravo.
- `docs/quality/evidence/custom-order-cold-restart-compact.png` / `.xml`: compact normal list still shows `Sorted by Custom Order` and Alpha → Charlie → Bravo after force-stop/relaunch.
- `docs/quality/evidence/custom-order-cold-restart-expanded.png` / `.xml`: expanded normal list preserves the same cold-restart consequence.
- `docs/quality/evidence/top-down-compact.png` / `.xml` and `top-down-expanded.png` / `.xml`: populated core Home/Task content remains readable and reachable at both layout classes.

## Final completion checklist

- [x] UI/design audit reconciled.
- [x] UX/product audit reconciled.
- [x] Drag interaction iterated and emulator-verified.
- [x] Cross-app consistency findings implemented or explicitly dispositioned.
- [x] Functional correctness findings implemented or explicitly dispositioned.
- [x] E2E cause/effect matrix completed.
- [x] Representative missing high-risk tests implemented and passing; platform-owned limits are explicit.
- [x] Compact and expanded emulator verification complete.
- [x] Senior QA second audit completed and incorporated; final blocker verdict is clear.
- [x] Full quality gates pass.
- [x] Audit file contains final decisions, changes, evidence, and known limitations.
