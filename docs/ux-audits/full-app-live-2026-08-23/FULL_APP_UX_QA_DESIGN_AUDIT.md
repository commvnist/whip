# Whip full-app UX, QA, and design audit

Date: 2026-08-23  
Build audited: `commvne.com.whip.app`, version code 15, version 0.3.9  
Device: Samsung Fold, physical closed and open-Fold configurations  
Theme/data state: dark theme, empty local dataset

## Executive verdict

Whip has a substantially stronger foundation than its screen count suggests. The established Tasks, Habits, Goals, Gym, Areas, and Settings surfaces share a restrained rectangular visual language, a single accent, common control wrappers, generally sound touch targets, and explicit empty states. The compact Tasks page is particularly clear. The local release build and all 198 JVM tests pass.

The audited build is not ready for a wider testing release, however. The audit records 1 P0, 5 P1, 10 P2, and 1 P3 finding. Tracks has the release-blocking adaptive-layout defect, two high-risk product gaps, and no UI regression coverage comparable to the older productivity domains. The most serious behavior is visible on both Fold configurations:

- In open-Fold mode, the Track editor is nested inside an already split Track workspace and is reduced to a clipped sliver.
- In closed mode, the editor content occupies the application workspace while the underlying app header, bottom navigation, and add button remain visible. Its semantic close/save actions overlap controls that visually appear to be the Whip logo and Settings.
- Track field and entry drafts use `remember`, not saveable state, so part of a draft can disappear after activity recreation while other fields survive.

Release recommendation: fix F-01 through F-06 before wider internal testing. F-07 onward can follow immediately as the information-architecture and polish pass.

## Method and evidence rules

This audit intentionally did not use the checked-in legacy screenshots as evidence.

- **Live:** 38 new screenshots and matching UI hierarchy dumps were captured from the installed release app. Representative compact and open-Fold routes cover Home, Tasks, Habits, Goals, Tracks, Gym, Areas, global add, Search, Review, and every Settings category. Android status and navigation bars are removed from the report copies.
- **Source:** navigation, editor state, permissions, accessibility semantics, Room behavior, copy, layout policies, and control primitives were inspected in the current worktree.
- **Local verification:** `testDebugUnitTest`, `lintDebug`, Android-test compilation, benchmark compilation, and `assembleRelease` passed. Instrumentation was not run on the personal phone.
- **Safety:** creation surfaces were opened and cancelled; no user records, permissions, notification settings, or phone security state were changed. Guarded input stopped when the phone entered its lock screen.

Because the live database was empty, populated item rows, completed history, active workout sets, automation results, restore/import outcomes, and long-history performance were source/test audited rather than claimed as live-tested. Those scenarios belong in a disposable emulator matrix.

### Severity

- **P0 — release blocker:** a primary flow is broken or materially misleading on a supported layout.
- **P1 — high:** core capability, data confidence, discoverability, or release confidence is compromised.
- **P2 — medium:** material friction, density, hierarchy, terminology, or maintainability issue.
- **P3 — low:** polish or technical hygiene with limited immediate user impact.

## Prioritized findings

### F-01 — Track editor does not own a valid adaptive surface

**Severity:** P0  
**Lenses:** UX, QA, responsive design, accessibility

Live evidence:

- [Open-Fold Track editor](screenshots/report/open_track_editor.png): the editor is clipped to the rightmost part of a nested master/detail workspace. Text fields, icon choices, instructions, and actions extend beyond its visible width.
- [Closed Track editor](screenshots/report/closed_track_editor.png): editor content appears beneath the global app chrome; the Whip logo, Area control, Settings icon, bottom navigation, and contextual add button remain visible.
- The closed UI dump exposes `Close Track Editor` at the same top-left bounds where the screenshot visually shows the Whip mark, while an unlabeled editor action overlaps the top-right Settings region. This is a visual/semantic mismatch, not merely tight spacing.

Source cause:

- `WhipApp.kt:925-929` gives every dialog a fixed 94% pane width, including compact mode.
- `TrackScreens.kt:379-394` creates an additional 38/62 split whenever the Track surface itself is at least 760 dp wide.
- `TrackScreens.kt:394-420` mounts `TrackEditor` inside the destination content rather than in a single app/root overlay host.
- `TrackScreens.kt:2235+` then treats that nested modifier as a destination-sized `WhipFullScreenSurface`.

Required design:

1. Editors must be hosted once, above the application scaffold/navigation, with one owner of insets and one owner of the adaptive pane.
2. Compact: the Track editor takes the complete app window inside safe drawing insets. No underlying top bar, navigation bar, or FAB is visible or actionable.
3. Open Fold: the editor may use the content pane or an intentionally expanded full-width surface, but it must not inherit the Track list/detail split. It gets the entire selected pane width.
4. The close action, title, and save action must be visually present and semantically aligned.
5. At 200% text and RTL, required fields and footer actions remain reachable by scrolling and no horizontal clipping occurs.

Acceptance tests:

- Add Track create/edit/entry cases to `AdaptiveWhipScreenTest` for compact, book Fold, expanded pane, RTL, and 200% font.
- Assert the editor bounds are wholly inside the selected pane and do not overlap primary navigation.
- Assert Close and Save are displayed, have at least 48 dp targets, and occupy the same bounds as their visible controls.
- Screenshot-test Track editors specifically; the current visual matrix only renders the base `WhipScreen` shell.

### F-02 — Track field and entry drafts can partially reset

**Severity:** P1  
**Lenses:** QA, data confidence, power users

`TrackEditor` saves the name, description, icon, Area, and tags with `rememberSaveable`, but stores Fields, confirmed deletions, and option replacements with plain `remember` (`TrackScreens.kt:2253-2261`). `TrackEntryEditor` likewise keeps its value map in plain `remember` (`TrackScreens.kt:2600`). After recreation, a user can see part of a draft survive while newly added Fields, choice edits, or Entry values revert. That partial survival is more dangerous than a clearly discarded draft because it implies the full form is intact.

`EditorStateRecreationTest` covers Task, Habit, Goal, Exercise, Machine, Routine, and inline workout-set state, but no Track editor or Track Entry editor.

Required fix and tests:

- Store the complete Track and Entry drafts in a saveable ViewModel/saved-state representation, not independent mixed-lifetime values.
- Recreate after adding, removing, and reordering Fields; editing choice options; entering every supported Field type; and changing the Entry date.
- Preserve dirty-state detection and explicit discard confirmation after recreation.

### F-03 — Tracks is implemented as a destination but is not discoverable as one

**Severity:** P1  
**Lenses:** UX, information architecture, product coherence

The source defines `AppDestination.Tracks`, search routing, deep links, a complete list/detail workspace, and global creation. But `primaryAppDestinations` contains only Home, Tasks, Habits, Goals, and Gym (`WhipApp.kt:182-188`). `HomeSection` also excludes Tracks. A zero-Track user can only reach the list indirectly by choosing **Add → Track**; Home only exposes **All Tracks** after a Track has already been pinned. First-run copy says Tasks, Habits, Goals, and Gym remain in main navigation, reinforcing the omission.

Required information architecture:

- Keep the five compact bottom destinations stable, but add a consistently placed app-level **More Destinations** control in the compact header containing Tracks, Review & Trends, and Settings. Do not reuse the Add menu or a sixth cramped bottom item for navigation.
- Show a compact Tracks section on Home even when empty, with **Create Track** and **All Tracks** actions.
- In rail layouts, use the available height to show Tracks directly at the same hierarchy as Gym.
- Opening and closing a Track editor returns to the Track workspace, and the persistent destination UI identifies Tracks as current.

Acceptance: a first-time user who has never created a Track can find the Track workspace from Home or persistent navigation without guessing that creation is also navigation.

### F-04 — Scroll-only destination bars hide important pages

**Severity:** P1  
**Lenses:** UX, accessibility, control consistency

The compact Habit bar visibly shows Today, All, and Insights. Connections and Archived exist, but are beyond the right edge. The More menu only contains Browse Templates. Goals similarly shows Active, Insights, and Completed while Archived is off-screen. `DestinationTabBar` automatically becomes a horizontal scroller for more than four destinations (`ItemControlPatterns.kt:101-150`) but supplies no overflow affordance, partial next label, count, fade, or **More** action.

This hides archival recovery and automation Connections behind an undocumented gesture.

Required pattern:

- Use visible peer tabs only for the three or four highest-frequency destinations.
- Put secondary destinations in the existing page overflow with stable names: **Connections**, **Archived**, and **Browse Templates**.
- When a secondary destination is active, replace the lowest-priority visible tab with that selected destination so location remains visible.
- Apply the same rule to every destination family; do not rely on invisible horizontal scrolling for navigation.

### F-05 — Notification diagnostics says delivery is impossible and “Enabled” simultaneously

**Severity:** P1  
**Lenses:** UX, QA, trust

[Live evidence](screenshots/report/open_settings_reminders.png) says notification permission has not been granted and reminders cannot be shown. Immediately below, Task reminders, Habit reminders, Goal reminders, Rest timer, Automation prompts, and Focus timer each say **Enabled**. The code is reporting Android channel health, but the screen presents it as feature delivery status (`SettingsScreens.kt:491-510`).

Required status model:

- Overall: **Blocked — notification permission required**.
- Per row: distinguish **Configured**, **Channel available**, and **Deliverable**. If the global permission is absent, the user-facing delivery status cannot be Enabled.
- Keep one primary repair action. Secondary Android settings links can sit under **Troubleshooting** disclosure.
- Test every combination of app permission, application notifications, individual channel state, and battery advice.

### F-06 — The strongest UI suite compiles but is not part of an executed local release gate

**Severity:** P1  
**Lenses:** QA, release engineering

There are 224 instrumentation test methods across 38 suites, including repository persistence, E2E creation, fold layouts, accessibility checks, backup/restore, notification deep links, and visual acceptance. They compile, but this audit did not execute them because the only attached target was a personal phone and destructive/instrumented tests must not run there. Compilation cannot prove runtime behavior.

Required local-only gate:

- Maintain a disposable API 34+ emulator dedicated to Whip QA.
- Before a Play bundle: run JVM tests, lint, assemble release, the complete connected test suite, then the adaptive/font/RTL screenshot matrix.
- Seed representative empty, small, and large datasets. Include active workout, Track automations, archived items, and failed backup restore.
- Save results and screenshots locally; no GitHub Action is required.

### F-07 — Home makes an empty day feel denser than an active one

**Severity:** P2  
**Lenses:** UX, visual design, low-pressure productivity

[Compact](screenshots/report/closed_initial.png) and [open-Fold](screenshots/report/open_initial.png) Home repeat section title, zero badge, chevron, and explanatory empty card for Tasks, Habits, Goals, and Gym. The compact FAB overlaps lower empty-state content during scrolling. On open Fold, the support pane is largely empty while the detail pane carries the full repeated structure.

Required design:

- When a section is empty, use one compact navigation row: icon, label, concise state, chevron. Do not also render a count badge and a separate empty card.
- Collapse all-zero Home into a calm **Nothing needs attention today** summary followed by short creation/navigation actions.
- Reserve full cards for actual items or actionable alerts.
- Give list content bottom padding equal to navigation + FAB clearance; test the final card at 200% text.
- Use the Fold support pane for daily summary/quick actions rather than repeating empty prose.

### F-08 — Search repeats its identity and gives inactive machinery excessive weight

**Severity:** P2  
**Lenses:** UX, design hierarchy

[Live Search](screenshots/report/open_search.png) displays **Search Whip** as the dialog title and again as the field label. Search Filters and Advanced Search are two full-width, equally weighted disclosure buttons before any result content. With the Fold keyboard open, the dialog becomes a control panel.

Required design:

- Title: **Search**. Field label/placeholder: **Tasks, habits, goals, tracks…**.
- Combine advanced filters under one **Filters** disclosure; show applied filters as removable chips and a numeric badge.
- Keep keyboard focus on open, but keep the query and first results visually dominant.
- Retain the current scoped-search wording because **Scope: All Whip** is useful orientation.

### F-09 — Review’s empty state explains but does not help the user act

**Severity:** P2  
**Lenses:** UX, product design

[Review & Trends](screenshots/report/open_review.png) uses only the top portion of the open Fold. It tells users to complete a task, habit, goal, or workout, but provides no destination actions. Weekly/Monthly controls and an ISO date range remain prominent despite there being no data. The date format conflicts with the localized dates elsewhere in Whip.

Required design:

- In the zero-data state, de-emphasize period controls and show four compact actions: **Open Tasks**, **Open Habits**, **Open Goals**, **Open Gym**.
- Localize the range (for example, `Aug 17–23, 2026`) and use the same formatter across the app.
- When data exists, let period controls lead and preserve the existing section-detail model.

### F-10 — Settings descriptions and content architecture disagree

**Severity:** P2  
**Lenses:** UX writing, information architecture

- Root label **Appearance & Home** becomes **Appearance and Home** inside the category.
- Root **Advanced** promises diagnostics and uncommon controls, but the live page contains only About Whip/build/package/privacy information.
- **Power mode (surface advanced controls sooner)** describes an implementation strategy rather than a user outcome.
- Organization includes a dense Track mini-manual. Track creation, Fields, Entry aggregation, import/export, and automations are Track help—not organization settings.

Required changes:

- Use one category name everywhere. Prefer `&` for compact category labels and sentence case in descriptions.
- Rename Advanced to **About & Diagnostics** and either move Delivery Diagnostics there or change the description to match its actual contents.
- Rename Power mode around the benefit, such as **Show advanced controls by default**, with clear supporting text.
- Move Track instructions into contextual, dismissible onboarding within Tracks. Organization should contain Areas and Tags only.
- Add a copy-routing test that asserts root descriptions match section headings/features.

### F-11 — Areas has count, pluralization, and noun-style defects

**Severity:** P2  
**Lenses:** OCD/consistency, UX writing

[Live Areas](screenshots/report/open_areas.png) shows `0 Track Entrys`, caused by the generic `noun + s` helper (`AreaManagementDialog.kt:661-670`). It also mixes sentence-case tasks/habits/goals with product-noun capitalization for Tracks and Entries, and repeats `0 items` before every zero category.

Required changes:

- Use a real plural resource/helper: `Entry` → `Entries`.
- Adopt a casing rule: sentence case in prose/counts (`tracks`, `track entries`); title case only for headings and action labels.
- Summarize empty Areas as **No items**. Reveal category counts only when at least one is non-zero or in Area detail.
- Keep the current create, move, merge, archive, restore, and permanent-delete lifecycle; source and live hierarchy show those concepts are now coherently grouped.

### F-12 — Goal measurement setup exposes implementation details before intent

**Severity:** P2  
**Lenses:** UX, productivity, novice/power-user balance

[Live Goal editor](screenshots/report/open_goal_editor.png) shows Goal type, a Measurement Unit explanation, Measurement type, Unit, another explanation, and Decimal places before the target itself. The default combination **Measurement type: No Unit** with **Unit: number** reads as contradictory. Precision is a low-frequency advanced choice.

Required design:

- After Goal type, ask for **Target** and **Starting value** first.
- Use one **Unit** selector with common choices, custom units, and **No unit**. Do not expose a separate internal measurement taxonomy unless it changes available units.
- Put Decimal places in **Advanced measurement options** and infer a sensible default from entered values.
- Keep inline custom-unit creation.

### F-13 — Task placement and recurrence share a control grammar despite different consequences

**Severity:** P2  
**Lenses:** UX, control semantics

The compact Task editor clearly places repeat-specific settings directly below Repeat, which fixes the earlier dependency-order problem. However, Inbox, Anytime, Scheduled Date, and Repeat still share the same chip treatment even though the first three are mutually exclusive placement choices and Repeat is an independent modifier.

Required design:

- Keep **Placement** as one segmented/single-choice group.
- Present **Repeat** as a labeled switch or disclosure immediately below Placement.
- Continue placing every conditionally revealed setting directly beneath its enabling control. Apply this dependency rule across Habit cadence/reminders, Goal automation, Track Fields/automations, and Gym timers.

### F-14 — Gym calculator defaults look like saved user data

**Severity:** P2  
**Lenses:** UX, QA, safety communication

[Workout Tools](screenshots/report/open_gym_tools.png) opens with 175 lb, 8 reps, and 5 lb rounding already entered and immediately presents an estimated 1RM. The safety disclaimer is good, but unexplained values can look remembered or authoritative.

Required design:

- Prefer empty required inputs with examples in supporting text, or explicitly label prefilled values **Example** and offer **Use Last Set**.
- Preserve the current formula selector, unit labels, rounding control, and planning-aid disclaimer.
- Test invalid, zero, extreme, localized decimal, unit-switch, and keyboard-submit behavior.

### F-15 — Destructive database fallback remains enabled

**Severity:** P2 before public release; accepted during current pre-release development  
**Lenses:** QA, data integrity

Room exports schema version 3 but uses `fallbackToDestructiveMigration(dropAllTables = true)` (`WhipDatabase.kt:58-99`). This matches the current permission to wipe an unreleased database, but it will silently erase data if a future version lacks an explicit migration.

Required release rule: remove destructive fallback before any public build, add schema migration tests for every supported version, and verify Track tables/links/backups across upgrades.

### F-16 — Very large UI files increase consistency and regression risk

**Severity:** P2 engineering risk  
**Lenses:** QA, maintainability, design-system governance

The UI package contains about 30,229 lines. `GymScreens.kt` is 5,817 lines, `WhipApp.kt` 4,302, `TrackScreens.kt` 3,032, and four other screen/editor files exceed 1,600 lines. This makes adaptive ownership and cross-feature patterns difficult to review—the Track overlay defect is an example.

Required refactor after behavior is guarded:

- Split by route and responsibility: list, detail, editor, automation, analytics, dialogs.
- Keep shared patterns in named components with previews/tests.
- Do not refactor before adding the regression cases for F-01 and F-02.

### F-17 — Lint is clean of errors but not clean of warnings

**Severity:** P3  
**Lenses:** QA, code hygiene

Lint reports 37 warnings and 14 hints: 34 Compose modifier-signature warnings, two mutable-collection state warnings, one KTX warning, and 14 primitive-state boxing hints. The mutable collection warnings in Goal and Habit editors deserve priority because state/recomposition defects affect users; the remainder can be a bounded cleanup.

## Domain-by-domain audit

| Domain | Live verdict | Source/test verdict | Required next action |
|---|---|---|---|
| Home | Clear daily orientation, but all-zero state is overbuilt and FAB can cover lower content. | Tracks is absent until pinned; configurable sections exclude it. | Implement F-03 and F-07. |
| Tasks | Strongest compact hierarchy: destination tabs → page identity/actions → Quick Capture → content. Search/filter/overflow are distinct icon actions. | Repeat dependencies are now ordered correctly; placement policy intentionally limits Agenda/Calendar to Upcoming. | Separate Repeat from Placement; preserve current Today hierarchy. |
| Habits | Check Off is the default, consequence copy is clear, and the editor starts simply. | Weekday-specific reminders start empty and do not silently create Monday; hidden Connections/Archived remain. | Implement F-04; preserve optional reminder behavior. |
| Goals | Clear goal-type choice, but measurement setup dominates the first viewport. | A supplied starting baseline no longer triggers Log First Value; Archived is hidden by scroll. | Implement F-04 and F-12. |
| Tracks | List concept and dynamic Fields are promising; compact editor chrome and Fold editor layout are broken. | Repository/domain automation coverage is substantial, but editor/adaptive/state tests are absent. | F-01 through F-03 are the first workstream. |
| Gym | Workout/Library/Tools hierarchy is understandable; Library rows read as navigation, calculator switch reads as a view choice. | Strong calculation, repository, routine, set-input, rest-timer, and large-history tests exist. Active workout was not live-seeded in this audit. | Fix F-14 and add the active-workout disposable-emulator journey. |
| Areas | Clear dedicated management surface and lifecycle language. | Move/merge/delete coordinators and tests exist. | Fix F-11; retain lifecycle model. |
| Search | Scope is explicit and keyboard focus is immediate. | Nine domains and advanced filtering exist. | Reduce duplicate identity/control weight per F-08. |
| Review & Trends | Naming is now correct; empty Fold use is poor. | Review integrates the core productivity domains. | Add zero-data actions and shared date formatting. |
| Settings | Root categories are scan-friendly and save behavior is explained. | Descriptions/copy routing drift; notification health concepts are conflated. | Implement F-05 and F-10. |
| Onboarding | Notification opt-in is default-off and only requests permission after explicit selection. | Main-navigation copy omits Tracks. | Add Track discovery without auto-requesting permissions. |
| Data & Privacy | Local-data, backup, encrypted backup, CSV, restore, and deletion concepts are present. | Backup/restore tests compile; location permissions are absent. Destructive migration is temporary debt. | Run on emulator and remove destructive fallback before public launch. |

## Design-system audit

### What is working

- Feature screens do not bypass the Whip control wrappers with direct Material Button/OutlinedButton/TextButton/FilterChip calls.
- Shapes follow a restrained 4/6/8/10/12 dp rectangular system rather than inconsistent pills.
- One primary accent is used for selection and high-emphasis actions; separators and surfaces remain visually quiet.
- Shared spacing tokens use an understandable 4/8/12/16/20/24/32 progression.
- Bottom navigation and rail selection states are centered and visually consistent.
- Page headers, empty states, navigation rows, destination bars, and disclosure controls are shared components rather than one-off styling.
- Tasks correctly distinguishes page actions with icons rather than recreating a second segmented button row.

### Design rules to enforce

1. **Navigation is not an action.** Peer pages use tabs; deeper pages use navigation rows/chevrons; independent actions use icon or text buttons; filters use chips; disclosures use a label plus disclosure indicator.
2. **An enabling control owns its consequences.** Revealed settings appear immediately below the switch/chip/selector that enables them.
3. **One surface owns one destination.** Overlays attach at the root and obscure/disable underlying navigation.
4. **Sentence case in prose; title case in headings/actions.** Product nouns are not randomly capitalized in body copy.
5. **Inactive machinery stays compact.** Filters, selection mode, diagnostics, and advanced precision do not consume permanent primary space.
6. **Empty states are shorter than populated states.** Explain once, then offer one or two relevant actions.
7. **Visible and semantic controls must agree.** The icon/text a user sees must occupy the same bounds and perform the action accessibility services announce.

## Accessibility audit

Positive source evidence:

- About 360 explicit content descriptions, semantics blocks, and test tags exist in the UI package.
- Shared page headers mark headings, page icon actions use 48 dp targets, and destination bars declare tab roles/selection.
- Android 14 Compose accessibility checks exist for primary destinations and Task/Habit/Goal editors.
- Adaptive tests cover 200% text, RTL, compact, open Fold, tabletop Fold, and field-pair stacking for established domains.

Gaps:

- Accessibility checks do not open Tracks, Track Entries, Track Fields, active workout sets, Search filters, Review details, or Areas lifecycle dialogs.
- The Track editor currently violates the visible/semantic alignment requirement in compact mode.
- The scroll-only Habit/Goal destination issue is both discoverability and switch-access/navigation-order debt.
- The visual matrix checks opaque edges, not screenshot goldens or per-screen clipping.

Required accessibility gate: run API 34+ accessibility checks on every primary destination, every editor type, destructive confirmations, Search with keyboard, active workout, and both Fold configurations; include 200% text and RTL for Tracks.

## QA audit

### Verified locally

Command:

```text
./gradlew testDebugUnitTest lintDebug compileDebugAndroidTestKotlin compileBenchmarkKotlin assembleRelease --stacktrace
```

Result:

- Release build: passed.
- JVM suites: 43.
- JVM tests: 198 passed, 0 failed, 0 errored, 0 skipped.
- Instrumentation source: 38 suites / 224 test methods compiled.
- Lint: 0 errors, 37 warnings, 14 hints.
- Location permissions: none in the current manifest.
- Declared permissions: notifications, boot completion, and six Health Connect reads (weight, steps, distance, hydration, sleep, exercise).

### Strong coverage already present

- Recurrence, reminders, quiet hours, quick capture, task progress, habit and goal rules.
- Gym calculations, analytics, routine programming, quick sets, rest behavior, and large-history regression.
- Areas, scope filtering, deletion coordination, backup/restore, encrypted backup, and CSV policies.
- Track repository CRUD, Field identity, choice replacement, automations, filtered aggregates, idempotent reconciliation, large histories, and habit/goal integration.
- Fold shell geometry, RTL, font scale, editor recreation for established entity types, and Compose accessibility scaffolding.

### Required missing coverage

1. Track editor and Track Entry editor state recreation.
2. Track create/edit/entry layout in compact, Fold, expanded, RTL, and 200% text.
3. Persistent access to Tracks for a zero-data user.
4. Habit/Goal secondary-destination discoverability and accessibility.
5. Notification status truth table.
6. Full-screen surface visual/semantic ownership and navigation suppression.
7. Populated live journeys for task actions, habit check-ins, goal progress, Track Entry automation, workout sets/rest presets, and Review drill-down.
8. Explicit Room migration tests before public release.
9. Performance baselines for 10k tasks, Track Entries with many dynamic Fields, large workout histories, global search, backup, and restore.

## Confirmed non-findings and repaired behaviors

These were checked so future work should not regress or repeatedly “fix” them:

- Current manifest/source does not request background, coarse, or fine location.
- Whip does not automatically request notification permission during inspection. First-run notification opt-in starts off.
- Habit check-off exists and is the default creation mode; no numeric `1` is required.
- Weekday-specific Habit reminders start with no day/time and do not silently schedule Monday.
- Task Repeat dependencies are directly below Repeat in the current editor.
- A Goal created with a starting value does not ask to log the first value again.
- Areas and Review render an opaque full-width app surface on the open Fold; the earlier split-background exposure was not reproduced.
- The thin gray right-edge handle in screenshots belongs to Samsung's Edge panel, not Whip.
- No explicit Coming Soon/TODO placeholder feature was found in current user-facing source.

## Implementation sequence and definition of done

### Wave 0 — Guard the broken behavior

- Add failing Track layout, semantics, and recreation tests.
- Create the disposable local emulator lane and seed fixtures.
- Capture before-fix compact/open-Fold Track screenshots.

### Wave 1 — Make Tracks safe and reachable

- Move all Track editors to the app/root overlay host.
- Remove nested Track workspace adaptation from editor constraints.
- Persist the complete Track/Entry draft.
- Add zero-data Track navigation/Home entry and rail destination.
- Pass the full adaptive/accessibility matrix.

### Wave 2 — Unify navigation and consequence controls

- Replace hidden scroll-only destinations with visible tabs + page overflow.
- Separate Task Placement from Repeat.
- Apply the enabling-control dependency rule across all editors.
- Correct notification delivery status semantics.

### Wave 3 — Reduce density and repair information architecture

- Compact empty Home.
- Simplify Search and Review empty state.
- Simplify Goal measurement setup.
- Move Track help out of Organization and align Settings labels/descriptions.
- Fix Area count grammar and noun casing.

### Wave 4 — Release hardening

- Execute all 224 instrumentation tests on a disposable API 34+ emulator.
- Run populated, archived, error, large-history, RTL, light/dark, 100/200/320% font, compact/open-Fold/tabletop cases.
- Replace destructive Room fallback with tested migrations.
- Resolve mutable collection lint warnings, then bounded lint/style cleanup.
- Produce a final fresh screenshot matrix from the release candidate.

The audit is complete when every P0/P1 acceptance condition is automated, the populated emulator matrix passes, and a fresh live review shows no clipped, hidden, semantically mismatched, or unexpectedly destructive primary flow.

## Fresh evidence catalog

All linked files below were captured from the installed release during this audit and exclude Android status/navigation bars.

### Open Fold

- [Home](screenshots/report/open_initial.png)
- [Tasks](screenshots/report/open_tasks.png)
- [Habits](screenshots/report/open_habits.png)
- [Habit editor](screenshots/report/open_habit_editor.png)
- [Goals](screenshots/report/open_goals.png)
- [Goal editor](screenshots/report/open_goal_editor.png)
- [Track editor](screenshots/report/open_track_editor.png)
- [Gym workout](screenshots/report/open_gym_workout.png)
- [Gym library](screenshots/report/open_gym_library.png)
- [Gym tools](screenshots/report/open_gym_tools.png)
- [Global add](screenshots/report/open_global_add.png)
- [Areas](screenshots/report/open_areas.png)
- [Search](screenshots/report/open_search.png)
- [Search filters](screenshots/report/open_search_filters.png)
- [Review & Trends](screenshots/report/open_review.png)
- [Settings](screenshots/report/open_settings.png)
- [Appearance & Home](screenshots/report/open_settings_appearance.png)
- [Planning & Units](screenshots/report/open_settings_planning.png)
- [Organization](screenshots/report/open_settings_organization.png)
- [Reminders & Integrations](screenshots/report/open_settings_reminders.png)
- [Data & Privacy](screenshots/report/open_settings_data.png)
- [Advanced](screenshots/report/open_settings_advanced.png)

### Closed Fold

- [Home](screenshots/report/closed_initial.png)
- [Tasks](screenshots/report/closed_tasks.png)
- [Task editor](screenshots/report/closed_task_editor.png)
- [Habits](screenshots/report/closed_habits.png)
- [Habit More menu](screenshots/report/closed_habits_more.png)
- [Goals](screenshots/report/closed_goals.png)
- [Goal editor](screenshots/report/closed_goal_editor.png)
- [Tracks](screenshots/report/closed_tracks.png)
- [Track editor](screenshots/report/closed_track_editor.png)
- [Gym workout](screenshots/report/closed_gym_workout.png)
- [Gym library](screenshots/report/closed_gym_library.png)
- [Gym tools](screenshots/report/closed_gym_tools.png)
- [Global add](screenshots/report/closed_global_add.png)
- [Settings](screenshots/report/closed_settings.png)
- [Organization](screenshots/report/closed_settings_organization.png)
- [Areas](screenshots/report/closed_areas.png)

Matching fresh UI hierarchy dumps are in [`ui-dumps/`](ui-dumps/).
