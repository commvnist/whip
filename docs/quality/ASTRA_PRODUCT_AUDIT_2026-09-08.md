# Whip whole-product quality evolution

Started 2026-09-08. Status: In progress. Owner request: `FB-20260908-006`.
Full objective: [preserved goal](ASTRA_QUALITY_GOAL_2026-09-08.md).

## Baseline and evidence authority

- Clean baseline `4f4a5dc` on `main`; application source 0.3.66/code 72. No edits were present at start.
- Current source has 159 main Kotlin files, including 67,391 lines across the UI package. Architecture is local-first Room/Flow with domain-owned projections, Jetpack Compose, and shared adaptive presentation.
- Read `docs/architecture.md`, `docs/testing.md`, the memory index, latest whole-product feedback/verification, and visual-review protocol. Old accepted design is a hypothesis for this review, not a constraint beyond explicit owner requirements.
- Catalog lint passes: 185 capture rows, zero pending selectors, zero platform exceptions. This proves current declared inventory consistency only. Platform journeys and UI owners absent from the catalog still require discovery/review.
- Fresh baseline capture started on explicit API 34 emulators `emulator-5554` and `emulator-5556`; evidence directory `/tmp/whip-astra-baseline-20260908`, log `/tmp/whip-astra-baseline-20260908.log`. Results remain pending.
- Original baseline source inventory and per-surface review state are preserved beside this file. All review statuses start unverified; no historical pass is copied forward.

## Acceptance and operating contract

1. Review all named areas plus newly discovered surfaces and consequential states, tracing UI actions through domain/persistence results and reopening.
2. Separate observed defects, supported design opportunities, inference, and aesthetic preference. Record impact, evidence, cause, alternatives, and selected approach before material remediation.
3. Preserve useful capabilities, owner data/history, and domain meaning. Local redesign and supporting refactoring are permitted when benefits justify disruption; whole-product replacement and unrelated expansion are outside scope.
4. Establish the design direction from rendered baseline evidence. Implement and inspect a complete representative journey before propagating broad changes.
5. Review compact/wide/fold/rotation/IME/large-text/RTL/screen-reader/touch behavior, realistic data, recovery, time/unit correctness, and cross-feature consistency.
6. Execute focused development checks per coherent chunk, then fresh comprehensive functional/build/lint/static/visual acceptance. Screenshots existing and tests passing do not constitute design acceptance.
7. Maintain this coverage record and memory; commit/push verified coherent chunks. Use one agent and no more than two disposable emulators. The owner's explicit emulator-only scope overrides the historical visual protocol's phone-deployment closeout sentence.
8. Completion requires substantive review of every inventory item, verified resolution of confirmed critical/high-impact defects and justified material improvements, final cross-app consistency review, exact evidence, and honest limitations. Missing evidence remains incomplete.

## Area coverage matrix

Each row requires source/domain review, live journey review, visual/state review, and verification; a family test alone cannot close a row. Per-surface details live in [the surface matrix](astra-surface-review-2026-09-08.tsv).

| Area | Main source owners / journeys | Review state | Findings / implementation / verification |
| --- | --- | --- | --- |
| First run and recovery | FirstRunSetupDialog, StartupRecoveryScreen, DataEpochGate, launch and recovery | Investigating | Baseline pending |
| Shell, Home, adaptive navigation | WhipApp, WhipNavigationPolicy, WhipPagePatterns, shared controls | Investigating | Baseline pending |
| Tasks | WhipApp.TaskAreaContent, TaskEditorDialog, TaskComponents, TaskViewModel/Repository; recurrence, subtasks, planning, filters, bulk actions, focus | Investigating | Baseline pending |
| Habits | HabitScreens, HabitViewModel/Repository, MeasurementRepository; cadence, timers, checklists, skips/pauses/history | Investigating | Baseline pending |
| Goals | GoalScreens, GoalViewModel/Repository; goal types, milestones, elapsed time, contributions/history | Investigating | Baseline pending |
| Tracks | TrackScreens, TrackViewModel/Repository, TrackEditorViewModels; fields, entries, units/precision, queries, history, archive, CSV | Investigating | Baseline pending |
| Gym execution/history/progress | GymScreens, GymViewModel/Repository, GymAnalytics; live sets, rest, prior context, completion/copy/deletion | Investigating | Baseline pending |
| Exercise and machine library | GymScreens, GymExercisePicker, GymCatalogMutationUi; create/edit/profile/version/archive/delete | Investigating | Baseline pending |
| Routines and 5/3/1 | RoutineBuilder, FiveThreeOneBuilder/Programming/CycleReview, RoutineRepository; setup, execution, progression, review | Investigating | Baseline pending |
| Search, Review and Insights | UnifiedSearchDialog, ReviewDialog, TrackAnalytics, CrossDomainInsights; scope/filter/drilldown/evidence | Investigating | Baseline pending |
| Areas and Tags | AreaManagementDialog, AreaPicker, AreaScopeFilters, TagManagementDialog and repositories; organization lifecycle | Investigating | Baseline pending |
| Settings and units | SettingsScreens/ViewModel, AppSettings, UnitSelectionField, DisplayUnits; immediate/committed settings, defaults/custom units | Investigating | Baseline pending |
| Backup, restore, import/export | BackupRepository, PortableBackupManager, RestoreRecoveryManager, TrackCsv; preview, merge/replace, encryption, recovery | Investigating | Baseline pending |
| Health Connect | HealthConnectManager, reconciliation and rationale; optional permissions, provider truth, sync/failure | Investigating | Baseline pending |
| Timers, reminders, notifications | focus/habit/rest timers, reminder workers/rules and action receivers; interruption, stale delivery, timezone | Investigating | Baseline pending |
| Widgets and external capture | WhipWidgetProvider, WidgetContent, configuration, MainActivity/launch delivery; creation, actions, queued drafts | Investigating | Baseline pending |
| Shared components and forms | ItemControlPatterns, ProductivityEditorComponents, EntityInspector, date/time/color/emoji/unit controls | Investigating | Baseline pending |
| Platform, accessibility, performance | manifest, adaptive/window hosting, semantics, keyboard/RTL, API compatibility, large data and benchmarks | Investigating | Baseline pending |

## Prioritized findings and design direction

Pending fresh runtime critique. Do not invent changes to populate the backlog.

## Completion audit

All area reviews, per-surface reviews, additional-state discovery, implementation, before/after review, comprehensive functional/static/build checks, adaptive/accessibility/platform evidence, and final design acceptance remain incomplete.
