# Whip UX architecture implementation plan

Status: implemented on `codex/initial-public-release`; automated release gate passed August 22, 2026

Evidence date: August 22, 2026

Evidence index: [`../artifacts/ux-planning/2026-08-22/README.md`](../artifacts/ux-planning/2026-08-22/README.md)

## Implementation record

The architecture in this document is now the product contract, not a future proposal. The implementation includes:

- shared rectangular controls, semantic typography/spacing/shape roles, adaptive destination tabs, common page headers, filter summaries, empty states, settings rows, and danger zones;
- the Tasks workspace (`Today | Inbox | Upcoming | History`), valid-view normalization, compact filtering, protected selection mode, immediate Quick Add with Edit/Undo, and removal of the ambiguous calendar shortcut; undated tasks formerly split between Inbox and Anytime are now consolidated in Inbox;
- one route-scoped search surface across Whip and Gym libraries, including keyboard Enter/Escape and explicit `Search All Whip` escape from a local scope;
- causally ordered Task, Habit, and Goal editors with compact icon selection, dirty-draft protection, and save-operation ownership so stale operation state cannot dismiss a new editor;
- Areas as destination-sized list/detail navigation with atomic move/merge/archive/delete choices and the one-active-Area invariant;
- shared page anatomy and low-data empty states across Habits, Goals, Gym, Review, and secondary pages;
- compact Settings category navigation plus wide master/detail navigation using the six categories in D9;
- fold-aware support panes, content expansion when support content is not useful, consistent surface colors, and adaptive overflow behavior;
- saved-filter/backup normalization and object-specific accessibility semantics for rows, calendars, selections, and check-off controls.

Verification completed locally with deterministic unit tests, Android instrumentation-source compilation, debug lint, debug APK assembly, signed/minified release assembly, Play asset verification, and exact installed-APK hash comparison. Instrumentation remains restricted to CI/emulators; it is not run on the personal Fold. The final device matrix is recorded in the evidence index as captures are accepted.

This plan supersedes screen-by-screen toolbar fixes from earlier UX passes where they conflict with the architecture below. The earlier work improved individual controls, but the fresh live audit shows that Whip still lacks one page hierarchy, one search model, one responsive navigation model, and one visual hierarchy system.

## Outcome

Whip should feel like one product across Tasks, Habits, Goals, Gym, Areas, Settings, and global utilities. A user should be able to predict whether a control navigates, changes a view, filters content, reveals details, edits an item, or starts a temporary mode before tapping it.

Every primary list surface must follow this order:

```text
global context
→ peer destination
→ page identity
→ applicable view/options
→ active state summary
→ content and contextual actions
```

The current Tasks order—destination, view selector, full-width action row, page title, content—must not survive this pass.

## Evidence and limitations

- The audit used 46 fresh captures from the installed release on the connected Samsung Galaxy Fold, package `commvne.com.whip.app`, version code 15 / version name 0.3.9.
- All retained evidence is under `artifacts/ux-planning/2026-08-22/live-core/` and is cropped to exclude the Android status/notification bar and Samsung task/navigation bar.
- `14-task-editor-initial.png` was recaptured after QA found a transient personal Messages banner in the first capture. The contaminated capture was overwritten and is not part of the retained evidence.
- Checked-in historical screenshots were not used.
- The pre-implementation audit set is open-fold/book-layout evidence. The post-implementation acceptance set adds fresh compact/closed-fold and open-fold captures from the exact installed release; see `artifacts/ux-planning/2026-08-22/post-implementation/`.
- Tabletop, populated-at-scale, error, large-text, RTL, and assistive-technology matrices remain local emulator responsibilities. Current source and deterministic tests inform those gates but do not substitute for the retained manual device evidence.
- Automated instrumentation must run on an emulator or dedicated test device. The personal phone is reserved for controlled manual validation because prior instrumentation affected its lock/fold behavior.

## Debate participants and convergence

The review was conducted independently by a UX/IA architect, a visual/product designer, and a QA/accessibility/power-user reviewer, followed by a rebuttal round.

They converged on these non-negotiables:

1. Page identity appears before page controls.
2. A view selector appears only when every offered view can represent the current dataset.
3. Search is one scoped system, with one visible entry point at a time.
4. Inactive filtering machinery does not occupy a permanent row.
5. Fold companion panes provide navigation, aggregates, or selected-item context; they do not duplicate content.
6. Required editor decisions precede optional details, and dependent settings remain adjacent to the choice that enables them.
7. Areas uses first-class list/detail navigation, not nested management dialogs.
8. Shared components and semantic visual tokens land before isolated screen polish.
9. One action is visually primary in a state; destructive actions are separated from routine actions.
10. Accessibility, Back behavior, saved-state normalization, and fresh responsive captures are release criteria—not follow-up polish.

## Decision record

### D1 — Shared page anatomy

All list/workspace pages use:

1. `DestinationTabBar` or parent navigation.
2. `WhipPageHeader`: title, supporting count/description, optional overflow.
3. Optional `WhipViewAndFilterRow` when the dataset supports alternate views.
4. `WhipActiveFilterRow` only when query/filter/sort state requires a visible summary.
5. Content, quick action, or `WhipEmptyState`.

Habits, Goals, and Gym child pages migrate to this same anatomy. They may omit steps that do not apply.

### D2 — Task navigation and valid views

Top-level Tasks navigation becomes:

```text
Today | Inbox | Upcoming | History
```

`History` contains an internal two-way switch:

```text
Completed | Archived
```

View availability:

| Destination | Views |
| --- | --- |
| Inbox | List only |
| Today | List only |
| Upcoming | List, Agenda, Calendar |
| History / Completed | List only |
| History / Archived | List only |

Why: the current implementation filters an already bucketed collection. Inbox owns every task without a planning date, while a Today month view can only contain Today. A universal view selector is therefore misleading, not merely dense. The former Anytime destination duplicated Inbox's undated data and was removed; its persisted enum remains an internal compatibility representation only.

Upcoming remains the planning destination for this release because `TaskUiState.upcoming` is already the authoritative preference-aware 30-day collection. A separate Planner is deferred until it has a purpose-built dataset rather than duplicating Upcoming.

Before removing Today Agenda, validate Today List with dense timed data. The current Agenda implementation only adds one date heading over the already day-scoped collection, so it is redundant today. If testing shows that power users need a day schedule, add a chronological timeline section inside Today backed by actual times; do not restore the universal cross-destination view switch.

The global calendar icon is removed. If a shortcut to planning remains in overflow, its label is `Open Task Planner` and it routes explicitly to Tasks → Upcoming → Agenda.

### D3 — One adaptive, scoped search system

`UnifiedSearchDialog` becomes a scoped search surface rather than coexisting with independent local search implementations.

- From Home: scope defaults to All Whip.
- From Tasks: scope defaults to `Tasks & Steps`; from Habits/Goals: scope defaults to that domain.
- From a Gym library child: scope defaults to that library type.
- The current scope is named and removable; `Search All Whip` is always available.
- Default All is represented as `All Types`, not seven selected chips.
- Type filters and query syntax move under `Filters` / `Advanced Search` disclosure.
- Only one search affordance is visible in a layout.
- `Ctrl+K` remains the explicit All Whip shortcut. Touch entry points start from their visible page context.

Placement is adaptive, which resolves the debate between global consistency and local discoverability:

- Expanded/compact layouts with app-bar room: conventional app-bar Search icon.
- Book-fold split layouts where global actions are in overflow: a compact Search icon in `WhipPageHeader`.
- Both placements launch the same controller and scoped surface; they are not separate search models.

Search state survives expand/restore and posture changes. When scope changes because the user navigates, Whip either visibly updates the scope or clears it—never silently keeps a hidden scope.

### D4 — Filter, sort, and selection

- Filter is labeled `Filter` on wide panes and uses a filter/tune icon with badge on narrow panes.
- The badge counts membership filters only. Sort and group choices do not inflate the filter count.
- Active membership filters appear as individually removable chips plus `Clear All`.
- A non-default sort/group may appear as a quiet summary such as `Sorted by Date`; it is not styled as an active membership filter.
- `Select Tasks` lives in page overflow and remains reachable without long press. Long press is an optional faster entry.
- Selection and reorder are explicit temporary modes with a contextual header, Done/Cancel, visible selected count, and Back handling.
- Changing destination, Area, or filters while selection contains hidden items exits selection or asks for confirmation. Hidden selections never remain active.

### D5 — Quick Add and detailed creation

The current `Capture a Task` field and `+` both open the full editor. That duplication ends.

- `Quick Add` saves immediately.
- Today defaults to today; Inbox defaults to Inbox; other destinations follow an explicit documented mapping.
- The current Area scope is inherited.
- The quick-capture parser remains available for recognized plain-language metadata.
- Success feedback offers `Undo` and `Edit` actions.
- The contextual `+` always opens the detailed editor.
- If immediate creation cannot land safely in the Tasks phase, remove the inline field until it can. Do not leave two routes to the same editor.

### D6 — Editor information architecture

Task, Habit, and Goal editors use the same high-level sequence:

1. Identity
2. Defining behavior
3. Schedule/target and directly dependent settings
4. Organization
5. Optional details

Concrete order:

- Task: title → Area → schedule/repeat and dependencies → priority/effort/estimate → subtasks → reminders → notes/tags/advanced.
- Habit: name/icon → intent → tracking mode and target dependencies → cadence → Area → reminders → presentation/data source/end conditions → notes/tags.
- Goal: name/icon → goal type → starting value/target/milestones → deadline → Area → measurement/pace/reminders → notes/tags/presentation.

`Advanced Options` moves after defining decisions. Power Mode may expand optional sections, but it never changes field order or moves optional fields above required ones. Collapsing a section cannot hide an active validation error.

Use shared section labels, sticky action footers, dirty-draft protection, and pane-aware placement. Replace oversized icon text fields with a compact icon picker and preview.

### D7 — Areas is first-class secondary navigation

Areas does not become a sixth bottom-navigation destination. The Area selector’s `Manage Areas` action opens a real list/detail route and remembers its caller.

Areas list:

- `Areas` page identity and a compact `New Area` action.
- Rows show color swatch, name, correctly pluralized item counts, and overflow.
- Tapping a row navigates to Area Details.

Area Details groups:

1. Identity: Rename, Choose Color.
2. Organization: counts, Move All Items, Merge Into.
3. Lifecycle: Archive.
4. Separated Danger Zone: Delete Permanently.

Preserve existing data integrity rules:

- At least one active Area always exists.
- `Main` is the initial/default Area; there is no placeholder `No Area` state.
- Deleting/archiving the final active Area requires creating or choosing a replacement first.
- Delete offers an explicit choice between moving affected items to another active Area and deleting affected items.
- Move/Merge/Delete confirmations show exact task, habit, and goal counts and destination before commit.
- Closing Areas restores the caller destination and a valid Area scope.

Dialogs remain appropriate for confirmations, but list and detail content do not stack management dialogs over Settings.

### D8 — Fold companion panes and responsive navigation

`FoldContextPane` becomes a destination-specific master/context slot:

- Home: chronological Today context or aggregate status, not the same cards as the feed.
- Tasks: bucket counts, saved views, active filter summary, or selected-task detail.
- Habits: period aggregate and selected-habit context.
- Goals: progress/next-milestone aggregate and selected-goal context.
- Gym: active workout controls; otherwise `Gym Overview`.
- Gym Library children: Library destinations as master navigation.
- Settings: Settings category navigation.

If a page has no useful companion content, expand the working pane instead of showing a mostly empty half-screen.

Destination tabs must measure available space:

- fixed/equal-width when all labels fit;
- otherwise scrollable with start/end padding, visible edge fade or peek, and selected item fully brought into view;
- never shrink text below the type system or leave a partially clipped word as the only discovery cue.

Expand/restore, fold/unfold, rotation, and recreation preserve destination, subsection, query, filters, selection where valid, editor draft, and scroll position.

### D9 — Settings information architecture

Settings categories become:

- Appearance & Home
- Planning & Units
- Organization
- Reminders & Integrations
- Data & Privacy
- Advanced

Presentation:

- Book fold/tablet: category master list in the companion pane; selected section in content.
- Compact: Settings landing list → section page.
- Expanded single pane: side navigation when width permits.

Horizontal tabs are not the primary Settings architecture. The live `Data & Backup` clipping is the failure case this decision prevents.

System Back and Close return to the exact prior app destination rather than exiting unexpectedly. Settings section and scroll state survive temporary navigation.

### D10 — Visual hierarchy system

The pass defines semantic tokens before local styling:

- Typography: app title, page title, pane title, section title, card title, body, supporting body, label, metric.
- Spacing: 4 dp base; 8 sibling gap; 12 compact internal gap; 16 standard grouping/card padding; 20 compact edge; 24 expanded edge/major group; 32 major separation.
- Shape: 6 dp controls/inputs/chips; 8 dp cards/navigation/disclosures; 12 dp dialogs/panes. Platform-recognizable switches and checkboxes stay recognizable.
- Color roles: primary for selection/one primary action, primary container for selected state, surface containers for grouping, outline for input/segmented boundaries, outline variant for quiet separators, error only for validation/destruction.
- Borders identify inputs and explicit selection, not every action. Ordinary cards are tonal by default.
- Elevation is reserved for menus, modal panes, sticky bars, and active floating elements.

Do not change the dynamic-color default as part of this architecture pass. The debate found that a fixed Whip palette could strengthen brand identity, but it is separable from the usability correction. Both dynamic and fixed themes must use the same semantic hierarchy and pass contrast/state tests. A later brand decision may change the new-install default without reopening the UX architecture.

### D11 — Shared empty and modal states

`WhipEmptyState` has:

- small domain icon;
- concise title;
- one supporting sentence;
- at most one primary action and one quiet secondary action.

Do not duplicate a visible header Create action with an equally prominent empty-state Create action.

`WhipModalPane` has:

- semantic pane title;
- compact sticky header;
- scrollable body;
- stable bottom action row;
- 20 dp compact / 24 dp expanded body padding;
- dirty-draft and IME-safe behavior where applicable.

A destination-sized overlay must also own the complete edge-to-edge window background, including the transparent system-bar regions. Safe drawing insets belong on an inner content container, not on the background surface. A pane-contained dialog instead retains deliberate outer gutters and visible parent context. Never combine full-width content below the status bar with split-pane coloring or a hinge divider left visible only inside the status bar.

Empty analytical states dominate the screen. Review & Trends and Insights should not show noisy zero-like charts and every filter control when there is no data.

### D12 — Back and transient-state model

Back unwinds the highest active layer in this order:

```text
IME
→ menu/dialog/sheet
→ contextual search
→ selection/reorder mode
→ child/detail page
→ Settings/Areas to caller
→ primary destination history
→ Activity exit from Home root
```

The state machine is shared and tested rather than implemented as unrelated screen-local `BackHandler`s. Editors retain their dedicated dirty-draft confirmation at the dialog layer.

## Control grammar

| Meaning | Visual/behavior pattern |
| --- | --- |
| Primary app destination | Navigation bar/rail |
| Peer page destination | Underline destination tabs or master-list navigation |
| Same-data alternate view | Segmented control, only for valid choices |
| Child page | Tonal row/card with right chevron |
| Reveal in place | Disclosure row/button with up/down chevron and expanded semantics |
| Single value choice | Selection field/menu with down arrow |
| Current filter/attribute | Removable chip |
| Frequent command | Compact labeled or icon action; 48 dp target |
| Rare actions/mode entry | Overflow menu |
| Edit exact item | Quiet edit icon or item overflow with object-specific label |
| Destructive action | Error-styled Danger Zone + explicit confirmation |

## Shared components to build first

Add or refactor these before migrating screens:

- `WhipPageScaffold`
- `WhipPageHeader`
- `WhipPageActions`
- `WhipViewAndFilterRow`
- `WhipActiveFilterRow`
- `WhipEmptyState`
- `WhipSection`
- `WhipSettingsRow`
- `WhipModalPane`
- `WhipDangerZone`
- `WhipMasterDetailScaffold`
- `WhipCard` variants: content, metric, navigation, disclosure
- adaptive `DestinationTabBar`
- route-derived `WhipSearchScope` and one `WhipSearchController`
- shared transient-mode/Back coordinator

Existing `SearchOrActionsRow` must not remain the default list chrome for Tasks, Habits, or Goals. It may remain only for an exceptional screen whose information architecture genuinely requires it.

## Implementation sequence

### Phase 0 — Lock contracts and regression harness

Primary files:

- `docs/UX_ARCHITECTURE_IMPLEMENTATION_PLAN_2026-08-22.md`
- `app/src/test/java/com/whip/app/ui/InteractionConsistencyTest.kt`
- `app/src/androidTest/java/com/whip/app/ui/InteractionControlUiTest.kt`
- `app/src/androidTest/java/com/whip/app/AdaptiveWhipScreenTest.kt`
- `app/src/androidTest/java/com/whip/app/WhipNavigationTest.kt`

Work:

- Encode the decision record as failing policy/state tests before UI migration.
- Add a task-view policy function and tests for every destination.
- Add saved-filter normalization tests.
- Add Back-state transition tests.
- Define the before/after screenshot manifest and privacy checklist.

Exit criteria:

- Policy tests fail for current universal task views and pass only after normalization behavior is explicit.
- No physical-device instrumentation is required to execute this phase.

### Phase 1 — Shared primitives and semantic tokens

Primary files:

- `app/src/main/java/com/whip/app/ui/ItemControlPatterns.kt`
- `app/src/main/java/com/whip/app/ui/WhipControls.kt`
- new shared scaffold/component files under `app/src/main/java/com/whip/app/ui/`
- `app/src/main/java/com/whip/app/ui/theme/Type.kt`
- `app/src/main/java/com/whip/app/ui/theme/Shape.kt`
- `app/src/main/java/com/whip/app/ui/theme/Theme.kt`

Work:

- Implement the shared components and roles listed above.
- Make `DestinationTabBar` adaptive and visibly scrollable when needed.
- Add heading/pane semantics and object-specific control labels.
- Preserve 48 dp targets while reducing visual weight through layout, not smaller touch regions.

Exit criteria:

- Component tests cover role, selected/expanded state, center alignment, 48 dp targets, LTR/RTL, and 1×/1.5×/2× text.
- No domain screen adopts a one-off replacement component.

### Phase 2 — Tasks architecture

Primary files:

- `app/src/main/java/com/whip/app/ui/WhipApp.kt`
- `app/src/main/java/com/whip/app/ui/TaskViewModel.kt`
- `app/src/main/java/com/whip/app/ui/TaskComponents.kt`
- `app/src/main/java/com/whip/app/core/PowerUserSettings.kt`
- `app/src/main/java/com/whip/app/ui/SettingsViewModel.kt`
- `app/src/main/java/com/whip/app/domain/TaskQuickCaptureParser.kt`

Work:

- Add UI navigation state for History/Completed/Archived without needlessly renaming persisted domain values.
- Move page identity above controls.
- Restrict planning views to Upcoming.
- Remove the permanent Search/Filter row.
- Add compact Filter, active chips, and separated sort/group summary.
- Implement selection-mode Back behavior and hidden-selection protection.
- Implement genuine Quick Add with Undo/Edit; retain `+` for full editor.
- Remove the ambiguous app-bar calendar action.

Saved-filter normalization:

- Completed/Archived routes map to History plus subsection.
- Agenda/Calendar on any destination other than Upcoming normalizes to List.
- Unknown destination normalizes to Today or the documented safe default.
- Normalization occurs when decoding/applying and is covered in backup/restore tests.
- Do not silently show an empty view.

Exit criteria:

- Every legacy data bucket remains reachable; records formerly split between Inbox and Anytime are both reachable through Inbox.
- No invalid view control is rendered.
- Quick Add, Undo, Edit, Cancel, Area inheritance, and parser behavior pass tests.
- The undated-task scheduling crash regression remains covered.

### Phase 3 — Scoped unified search

Primary files:

- `app/src/main/java/com/whip/app/ui/UnifiedSearchDialog.kt`
- `app/src/main/java/com/whip/app/ui/WhipApp.kt`
- `app/src/main/java/com/whip/app/ui/HabitScreens.kt`
- `app/src/main/java/com/whip/app/ui/GoalScreens.kt`
- `app/src/main/java/com/whip/app/ui/GymScreens.kt`

Work:

- Introduce route-derived search scope and one controller/state model.
- Make the adaptive search placement show only one affordance.
- Remove full-width local Search buttons as each domain migrates.
- Collapse default All Types and advanced syntax.
- Add explicit Area scope and Search All Whip behavior.
- Restore focus to the invoking control on close.

Exit criteria:

- Domain search is one tap in every layout.
- Scope is visible and testable.
- Query, scope, results routing, keyboard Enter/Escape, and fold expand/restore pass.

### Phase 4 — Editors and causal grouping

Primary files:

- `app/src/main/java/com/whip/app/ui/TaskEditorDialog.kt`
- `app/src/main/java/com/whip/app/ui/ProductivityEditorComponents.kt`
- `app/src/main/java/com/whip/app/ui/HabitScreens.kt`
- `app/src/main/java/com/whip/app/ui/GoalScreens.kt`
- `app/src/main/java/com/whip/app/ui/EditorProtection.kt`

Work:

- Adopt the shared editor sequence and sections.
- Move Advanced after essentials without weakening Power Mode.
- Keep every dependency directly beneath its enabling control.
- Add compact icon picker.
- Add IME action/focus order, heading/pane semantics, validation announcements, and legible disabled Save.

Exit criteria:

- Basic Task, Check Off Habit, and target Goal can be created without traversing unrelated optional controls.
- Every tracking/goal/schedule mode has an ordering assertion.
- Unsaved state survives recreation/posture change and warns correctly on dismissal.

### Phase 5 — First-class Areas

Primary files:

- `app/src/main/java/com/whip/app/ui/AreaManagementDialog.kt` (replace/remove management content)
- `app/src/main/java/com/whip/app/ui/AreaPicker.kt`
- `app/src/main/java/com/whip/app/ui/AreaScopeFilters.kt`
- `app/src/main/java/com/whip/app/ui/WhipApp.kt`
- `app/src/main/java/com/whip/app/data/AreaRepository.kt`
- existing Area deletion/move coordinator and tests

Work:

- Add Areas list/detail route and caller-return state.
- Reuse existing move/merge/delete coordinators; do not bypass atomic data operations.
- Group identity, organization, lifecycle, and danger actions.
- Fix pluralization and accessible swatch labels.
- Preserve one-active-Area and Main behavior.

Exit criteria:

- No workflow shows Settings → Areas dialog → Area Details dialog as three stacked layers.
- Move All and both delete outcomes show exact counts and pass atomicity, backup, scope, widget, and search tests.
- Back/Close restores the caller.

### Phase 6 — Habits, Goals, and Gym anatomy

Primary files:

- `app/src/main/java/com/whip/app/ui/HabitScreens.kt`
- `app/src/main/java/com/whip/app/ui/GoalScreens.kt`
- `app/src/main/java/com/whip/app/ui/GymScreens.kt`
- `app/src/main/java/com/whip/app/ui/ItemControlPatterns.kt`

Work:

- Migrate page headers, actions, empty states, search, Templates/Reorder, and overflow to shared anatomy.
- Standardize Gym child order: parent/back → header/primary action → contextual search/filter → content/empty state.
- Make Machine prerequisites actionable rather than disabled dead ends.
- Make Gym global `+` contextual or hide it on child pages with owned creation.
- Preserve the clear Library navigation-row grammar and compact Back to Library behavior.
- Add object-specific labels to habit/task check controls and full semantics to calendar days.

Exit criteria:

- Search/Templates/Reorder no longer consume permanent full-width chrome.
- Every child page has predictable creation, Back, empty, populated, archived, and prerequisite behavior.
- Check Off is visually and semantically unambiguous.

### Phase 7 — Fold panes, Settings, Home, and analytics

Primary files:

- `app/src/main/java/com/whip/app/ui/WhipApp.kt`
- `app/src/main/java/com/whip/app/ui/SettingsScreens.kt`
- `app/src/main/java/com/whip/app/ui/ReviewDialog.kt`
- `app/src/main/java/com/whip/app/ui/GymScreens.kt`
- `app/src/main/java/com/whip/app/ui/HabitScreens.kt`
- `app/src/main/java/com/whip/app/ui/GoalScreens.kt`

Work:

- Slot destination-specific content into the companion pane.
- Give Settings master/detail navigation and new categories.
- Remove duplicated Home/support-pane summaries.
- Change Review & Trends from a utility-weight modal to a destination-sized Home insight surface, or use the shared modal shell as an explicit interim step.
- Make empty analytics lead with the empty state and hide inactive configuration.

Exit criteria:

- No clipped Settings/domain destinations.
- No mostly empty companion pane is shown when expanded content is more useful.
- Settings Back returns to caller.
- Review/Insights empty and populated states pass hierarchy checks.

### Phase 8 — Accessibility, performance, and live visual release gate

Automated matrix:

| Area | Required variants |
| --- | --- |
| Navigation/Back | all primary destinations, Settings, Areas, Gym children; compact/book/tabletop/expanded |
| Tasks | every destination × allowed view; empty/1/100/10k; query/filter/sort/group/saved filter/selection/repeat |
| Habits | every tracking mode/cadence; reminder unset/weekday; search/reorder/archive |
| Goals | every goal type/status; starting value; manual/link/first-log paths |
| Gym | all primary/child pages; empty/populated/archived/prerequisite/active workout |
| Areas | 1/2/50 Areas; move/merge/archive/delete/replacement/delete-items |
| Accessibility | TalkBack semantics, switch access, 1×/1.5×/2×/3.2× text, RTL, contrast, dynamic/fixed themes |
| Keyboard | IME, fold keyboard, hardware keyboard; Tab/Shift+Tab/Enter/Escape/shortcuts/focus restoration |
| State | recreation, process death, fold changes, expansion, Area/scope changes |

Live visual matrix:

- Recapture the 46 baseline routes after implementation.
- Add fresh compact/closed-fold counterparts.
- Add open-fold and compact populated, selected, filtered, error, loading, destructive-confirmation, keyboard-open, and active-workout states.
- Crop Android system bars.
- Inspect every image for notifications, personal content, clipping, overlap, stale data, and wrong build/version before repository inclusion.
- Capture manual fold transitions; do not automate physical fold/lock state on the personal phone.

Exit criteria:

- 48 dp minimum targets.
- Normal text contrast ≥ 4.5:1; large text/control boundaries ≥ 3:1.
- Screen-reader and keyboard order matches visual order.
- No color-only state.
- No clipped selected navigation at required font scales.
- No notification/system bars or personal data in retained screenshots.
- Release tests run on an emulator/dedicated device; final manual acceptance runs on the phone without instrumentation.

## Required test additions and updates

- `TaskPlanningViewPolicyTest`: allowed views and normalization for every destination.
- `TaskWorkspaceNavigationTest`: History routing, destination/view reset, expansion preservation.
- `SavedTaskFilterNormalizationTest`: invalid destination/view, legacy Completed/Archived, backup decode.
- `QuickAddTaskTest`: destination/Area defaults, parser, Undo, Edit, failure, recreation.
- `ScopedSearchUiTest`: adaptive entry placement, named scope, Search All Whip, routing, focus restore.
- `BackNavigationStateTest`: full unwind order and caller restoration.
- `EditorInformationArchitectureUiTest`: required fields before advanced; dependency adjacency for every editor mode.
- `AreaNavigationUiTest`: list/detail/caller return, one-active invariant, move/delete variants.
- `ListAnatomyUiTest`: page heading before controls and one primary CTA across Tasks/Habits/Goals/Gym.
- `CalendarSemanticsUiTest`: full date, selected state, count, keyboard focus.
- `PopulatedRowAccessibilityTest`: unique checkbox/edit/overflow labels.
- Extend `InteractionControlUiTest`, `AdaptiveWhipScreenTest`, `WhipComposeSemanticsTest`, `WhipNavigationTest`, and backup/migration tests rather than duplicating their existing coverage.

## Definition of done and anti-half-done guardrail

A phase is not complete when only one screenshot looks better. It is complete only when:

1. The shared primitive/state model exists and the migrated screens use it.
2. The replaced one-off pattern is removed or has a documented exceptional use.
3. Behavior, accessibility, Back, restoration, and migration tests pass.
4. Compact, book-fold, expanded, large-text, RTL, and keyboard variants are covered in proportion to the phase.
5. Fresh device evidence is captured for affected routes and passes the privacy/system-bar checklist.
6. Documentation and this plan are updated with the implementation status and any approved deviation.
7. No issue is marked fixed solely because it was moved into overflow; reachability, naming, active state, and Back behavior must also be verified.

Do not perform broad screen-by-screen visual edits before Phase 1. Doing so would recreate the inconsistency this plan is intended to remove.

## Explicitly deferred decisions

- A separate Planner destination beyond Upcoming: defer until the dataset extends beyond the current 30-day Upcoming model.
- Fixed Whip palette as the new-install default: separate brand/product decision; semantic roles and contrast are required now.
- Making Areas a sixth primary navigation item: rejected; Areas is first-class secondary navigation.
- Removing all local-looking search affordances: rejected. Search placement is adaptive, but the system/state is singular and scope is explicit.
- Long press as the only route to selection: rejected.
- Preserving universal view controls for visual symmetry: rejected.
