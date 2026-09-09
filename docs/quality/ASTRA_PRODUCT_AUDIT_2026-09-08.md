# Whip whole-product quality evolution

Started 2026-09-08. Status: In progress. Owner request: `FB-20260908-006`.
Full objective: [preserved goal](ASTRA_QUALITY_GOAL_2026-09-08.md).

## Baseline and evidence authority

- Clean baseline `4f4a5dc` on `main`; application source 0.3.66/code 72. No edits were present at start.
- Current source has 159 main Kotlin files, including 67,391 lines across the UI package. Architecture is local-first Room/Flow with domain-owned projections, Jetpack Compose, and shared adaptive presentation.
- Read `docs/architecture.md`, `docs/testing.md`, the memory index, latest whole-product feedback/verification, and visual-review protocol. Old accepted design is a hypothesis for this review, not a constraint beyond explicit owner requirements.
- Catalog lint passes: 185 capture rows, zero pending selectors, zero platform exceptions. This proves current declared inventory consistency only. Platform journeys and UI owners absent from the catalog still require discovery/review.
- Fresh baseline capture passed 66/66 owning tests with zero failures/skips/reuse across seven batches on explicit API 34 emulators `emulator-5554` and `emulator-5556`. It exported all 185 PNG/XML pairs to `/tmp/whip-astra-baseline-20260908`; execution evidence is `build/instrumentation-results-khC0SB`, with log `/tmp/whip-astra-baseline-20260908.log`. This proves capture completeness, not fresh design or complete behavioral acceptance of every family.
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
| First run and recovery | FirstRunSetupDialog, StartupRecoveryScreen, DataEpochGate, launch and recovery | Investigating | Baseline catalog captured; substantive review pending |
| Shell, Home, adaptive navigation | WhipApp, WhipNavigationPolicy, WhipPagePatterns, shared controls | In progress | Home summary and activity theme verified in IMP/VER-20260908-014 and IMP/VER-20260909-002; broader design/journey review remains open |
| Tasks | WhipApp.TaskAreaContent, TaskEditorDialog, TaskComponents, TaskViewModel/Repository; recurrence, subtasks, planning, filters, bulk actions, focus | Investigating | Baseline catalog captured; substantive review pending |
| Habits | HabitScreens, HabitViewModel/Repository, MeasurementRepository; cadence, timers, checklists, skips/pauses/history | Investigating | Baseline catalog captured; substantive review pending |
| Goals | GoalScreens, GoalViewModel/Repository; goal types, milestones, elapsed time, contributions/history | Investigating | Baseline catalog captured; substantive review pending |
| Tracks | TrackScreens, TrackViewModel/Repository, TrackEditorViewModels; fields, entries, units/precision, queries, history, archive, CSV | Investigating | Baseline catalog captured; substantive review pending |
| Gym execution/history/progress | GymScreens, GymViewModel/Repository, GymAnalytics; live sets, rest, prior context, completion/copy/deletion | Investigating | Baseline catalog captured; substantive review pending |
| Exercise and machine library | GymScreens, GymExercisePicker, GymCatalogMutationUi; create/edit/profile/version/archive/delete | Investigating | Baseline catalog captured; substantive review pending |
| Routines and 5/3/1 | RoutineBuilder, FiveThreeOneBuilder/Programming/CycleReview, RoutineRepository; setup, execution, progression, review | Investigating | Baseline catalog captured; substantive review pending |
| Search, Review and Insights | UnifiedSearchDialog, ReviewDialog, TrackAnalytics, CrossDomainInsights; scope/filter/drilldown/evidence | Investigating | Baseline catalog captured; substantive review pending |
| Areas and Tags | AreaManagementDialog, AreaPicker, AreaScopeFilters, TagManagementDialog and repositories; organization lifecycle | Investigating | Baseline catalog captured; substantive review pending |
| Settings and units | SettingsScreens/ViewModel, AppSettings, UnitSelectionField, DisplayUnits; immediate/committed settings, defaults/custom units | Investigating | Baseline catalog captured; substantive review pending |
| Backup, restore, import/export | BackupRepository, PortableBackupManager, RestoreRecoveryManager, TrackCsv; preview, merge/replace, encryption, recovery | Investigating | Baseline catalog captured; substantive review pending |
| Health Connect | HealthConnectManager, reconciliation and rationale; optional permissions, provider truth, sync/failure | Investigating | Baseline catalog captured; substantive review pending |
| Timers, reminders, notifications | focus/habit/rest timers, reminder workers/rules and action receivers; interruption, stale delivery, timezone | Investigating | Baseline catalog captured; substantive review pending |
| Widgets and external capture | WhipWidgetProvider, WidgetContent, configuration, MainActivity/launch delivery; creation, actions, queued drafts | Investigating | Baseline catalog captured; substantive review pending |
| Shared components and forms | ItemControlPatterns, ProductivityEditorComponents, EntityInspector, date/time/color/emoji/unit controls | Investigating | Baseline catalog captured; substantive review pending |
| Platform, accessibility, performance | manifest, adaptive/window hosting, semantics, keyboard/RTL, API compatibility, large data and benchmarks | In progress | Activity theme/recovery boundary verified on API 26/34/37; legacy emulator guard repaired; full platform/accessibility/performance scope remains pending |

## Prioritized findings and design direction

1. `FND-20260908-013`: Neutral skipped-Habit scoring and adaptive unfinished counts are verified in `IMP/VER-20260908-014`. A fresh persisted regression reproduced `1 of 2` for one completed/one skipped Habit; four new policy tests and the skip/recreation/undo journey pass, with 161 fresh shell/Habit/core/adaptive/accessibility Android tests and 348 routed JVM readiness tests.
2. `FND-20260908-014`: A compact responsive Home header and single clear-day Review owner are verified in `IMP/VER-20260908-014`. The first Task card moves from y=1232 to y=982 in matched 1080×2400 captures while retaining its geometry/actions. Large-text RTL review caught and corrected numeric phrase reordering and an unpainted fixture background. The first clear-day capture was rejected because onboarding obscured the state; the repaired fixture and replacement 26-state shared capture pass, and the corrected clear-day pixels/XML were inspected. [Preserved before/after evidence](../../artifacts/astra-audit/2026-09-08/home/README.md).
3. Investigate semantic notice color: `WhipNoticeTone.Informative` and `Success` currently share the same green container despite different roles. Requires current pixels and neighboring-state comparison before deciding a change.
4. `FND-20260908-015` is verified in `IMP/VER-20260909-002`: the main app, widget configuration, Health rationale, and recovery content share activity theme ownership. Both opposite-theme defects were reproduced before correction; API 26/34/37 tests and inspected phone/large-screen captures prove readable bars. Five explicit theme states expand the inventory to 193, including 31 shared states. [Preserved before/after and platform evidence](../../artifacts/astra-audit/2026-09-09/theme/README.md).
5. `FND-20260909-001` is verified in separate commit `81c4213`: canonical target validation recognizes legacy emulator identity; catalog operations reuse that guard. The fast lane retains its API 34 minimum; API 26 compatibility uses guarded direct Gradle execution. This fixes access to the older platform without weakening emulator-only verification.

Initial design direction: put the user's next action near the top; use compact, readable summaries that state outcomes accurately; reserve prominent surfaces and semantic color for decisions/status that need attention; preserve the established collection-card and editor roles unless a concrete benefit supports changing them.

## Discovery follow-ups

- First-run customization and its expanded optional preferences are separate meaningful states absent from the starting catalog. Add their visual/state evidence during the remaining shared review.
- Review Home section customization, scoped/saved filters, all-finished states, and sparse returning use beyond the current header improvement. In particular, assess whether global header shortcuts should respect hidden Home sections and whether empty per-domain prompts distract from another domain's daily actions. These are unverified opportunities, not accepted product changes.
- The API 37 theme capture exposes an additional design question: sparse Home repeats its Task summary in the support pane and main content, alongside several zero-count destinations. Evaluate whether that use of wide-screen space serves daily work; the theme correction does not accept or change this composition.
- Inspect platform-owned permissions/providers/widgets/notifications and light/dynamic themes explicitly; zero catalog exceptions does not mean those journeys have been covered.

## Completion audit

The first Home and activity-theme corrections are verified and ready for owner feedback. Whole-product area and per-surface review, additional-state discovery, remaining justified improvements, comprehensive final functional/static/build checks, full adaptive/accessibility/platform evidence, and final design acceptance remain incomplete. The goal remains active; continue with first-run and remaining shared/Home review, then every area in the matrix at equal depth.
