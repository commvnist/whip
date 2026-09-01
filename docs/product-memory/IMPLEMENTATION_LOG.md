# Implementation history

### IMP-20260831-001 — Gym and 5/3/1 first-class remediation released

- Behavior changed: Added arbitrary-lift 5/3/1 creation; distinct actual/e1RM/TM semantics and adjustable derivation; ordinary-routine TM controls; typed Main/Supplemental/Assistance/Optional work; additive Jokers; performance-informed cycle review; workout-only exercises; contextual picker return; adaptive routine panes; honest no-history state; accessible timers; and timer-boundary correction.
- Important files: `GymEntities.kt`, `RoutineEntities.kt`, `GymRepository.kt`, `RoutineRepository.kt`, `WhipDatabase.kt`, `FiveThreeOneProgression.kt`, `FiveThreeOneBuilder.kt`, `FiveThreeOneCycleReview.kt`, `FiveThreeOneProgramming.kt`, `RoutineBuilder.kt`, `GymScreens.kt`, `WhipApp.kt`, and related JVM/Android tests.
- Persistence/history impact: Explicit migrations through schema 37 and backup format updates preserve historical workout prescriptions and completed work.
- Compatibility: Existing data installed in place; current and historical records are not silently recomputed from edited templates.
- Commit/push: `5fc98dd` on `origin/main`.
- Related: `FB-20260831-001` through `FB-20260831-010`; detailed audit `../GYM_531_PRODUCT_AUDIT_2026-08-31.md`.
- Verification: `VER-20260831-001`, `VER-20260831-002`.
- Status: Released in 0.3.34; continued field validation applies.

### IMP-20260831-002 — Durable product-memory infrastructure

- Behavior changed: Added a personal `maintain-whip-memory` skill, repository fallback instructions, canonical ledgers, stable IDs, evidence-state rules, and a reusable maximum-quality goal with mandatory memory checkpoints.
- Important files: `/mnt/c/Users/commv/.codex/skills/maintain-whip-memory/`, `AGENTS.md`, and `docs/product-memory/`.
- Compatibility: Documentation/process only; no application data or runtime behavior changed.
- Related: `FB-20260831-013`, `FND-20260831-006`, `DEC-20260831-006`.
- Verification: `VER-20260831-003`.
- Status: Implemented and structurally validated; future-task behavioral validation pending.

### IMP-20260831-003 — Coherent commit-and-push discipline

- Behavior changed: The personal memory skill, workspace fallback, memory schema, and maximum-quality goal now require every completed coherent chunk to be narrowly staged, committed, normally pushed, and verified before unrelated work begins.
- Important files: `/mnt/c/Users/commv/.codex/skills/maintain-whip-memory/SKILL.md`, its memory-schema reference, `AGENTS.md`, and `docs/product-memory/`.
- Compatibility: Workflow-only. It explicitly preserves unrelated dirty-worktree changes and prohibits force-push/history rewriting.
- Related: `FB-20260831-014`, `DEC-20260831-007`.
- Verification: `VER-20260831-004`.
- Status: Implemented and structurally verified; ongoing behavioral validation applies.

### IMP-20260831-004 — Fail-closed startup restore recovery

- Behavior changed: Whip now resolves pending interrupted restore before normal product work and routes live replace restore through a counted application-wide admission/drain barrier. Workers, receivers, schedulers, widgets, Health/background jobs, ViewModel operations, editor/import SavedState, notification/widget actions, and WorkManager startup fail closed or carry the current non-restored data generation. Widget display preferences survive restore while identity-bearing references and snapshots are reconciled; late stale actions cannot alias restored rows with reused numeric IDs. Recovery or runtime initialization failure produces a full-screen accessible blocking state with serialized Retry.
- Important files: `WhipApplication.kt`, `MainActivity.kt`, `AndroidManifest.xml`, `data/RestoreRecoveryManager.kt`, `startup/StartupRecoveryGate.kt`, `startup/UserDataGeneration.kt`, `ui/StartupRecoveryScreen.kt`, affected ViewModels, reminder/timer/portable schedulers and receivers, widget providers/factories/preferences/snapshot cache/configuration Activity, recovery strings, and focused JVM/Android regression tests.
- Persistence/history impact: No schema or backup-format change. A failed rollback, invalid snapshot, or failed recovery rebuild keeps the existing private recovery marker. The marker is removed only after both restore and background-state rebuilding succeed, preserving the prior atomicity boundary and all existing user records.
- Compatibility: Generation zero accepts legacy unversioned SavedState/widget snapshots only before the first replace attempt. The generation token is excluded from backups, increments only after a durable recovery marker exists, and invalidates stale identity references without changing historical workout/task/habit/goal records. Ordinary startup still reaches the existing product flow; other AndroidX Startup initializers remain while WorkManager's initializer is removed and application configuration is provided on demand.
- Commit/push: Focused recovery commit containing this entry on `origin/main`.
- Related: `FND-20260831-007`, `DEC-20260831-008`.
- Verification: `VER-20260831-006`.
- Status: Implemented and verified; physical-device release is deferred until the integrated goal release.

### IMP-20260831-005 — Exact live reminder delivery integrity

- Behavior changed: Task, Habit, and Goal reminder work is now an untrusted, versioned exact claim. Delivery and notification actions re-resolve live eligibility, timing, occurrence state, optional action semantics, source-backed progress, and semantic fingerprints before posting or mutating. Stale/malformed work fails closed and reconciles; visible notifications are removed on relevant edits/deletions; early execution requeues the still-due reminder; quiet-hour rollover includes the prior logical day; and fixed/follow-device time behavior is explicitly reconciled.
- Architecture: Production Task/Habit/Goal/Measurement mutations and worker resolve/post decisions share a non-reentrant state boundary. Raw delegates are passed only beneath one outer owner, entity locks always precede state locks, full-snapshot Settings updates are process-serialized, and durable deletion cleanup spans Room and NotificationManager across rollback/process death. One-time claim-version maintenance cancels legacy visible reminders and durably marks success only after every domain rebuild completes.
- Important files: `WhipApplication.kt`, `AndroidManifest.xml`, `core/AppSettings.kt`, deletion coordinators, DAO/repository malformed-data guards, `reminders/ReminderDeliveryClaims.kt`, `CoordinatedReminderRepositories.kt`, `ReminderDeletionCleanupStore.kt`, `ReminderRuntimeMaintenance.kt`, `ReminderTimeChangeReceiver.kt`, all three reminder schedulers/workers/actions/notifications, related ViewModels, and focused JVM/Android tests.
- Persistence/history impact: No Room schema or portable-backup format change. A private claim-version marker and private deletion-cleanup journal are operational metadata only. Existing Task occurrences, Habit logs/skips/pauses, Goal progress, completed workouts, custom units, and historical local dates are preserved and never retroactively recomputed.
- Commit/push: Focused reminder-integrity commit containing this entry on `origin/main`.
- Related: `FND-20260831-008`, partial prerequisite work for `FND-20260831-009`, and `DEC-20260831-009`.
- Verification: `VER-20260831-007`.
- Status: Implemented and verified; physical-device release remains deferred until the integrated maximum-quality goal release.

### IMP-20260831-006 — Coherent Whip calendar context and new-record provenance

- Behavior changed: One eager application-scoped calendar context now carries the active Whip zone, physical date, cutoff-adjusted logical date, cutoff, and follow-device policy. Settings changes, aligned minute ticks, and Android date/time/zone invalidations recompute it. Task, Habit, Goal, Track, Search, Gym, widgets, and root CompositionLocals consume the same source; cross-domain UI retains its prior snapshot until every date-derived projection catches up. Track rolls without a repository write, Search completion filters use the Whip zone and reindex without clearing the query, Gym Today ranges honor the cutoff without resetting a browsed month, and new/default/copied workout and measurement records snapshot Whip date/zone provenance. Explicit historical workout starts derive their physical local date from the supplied instant.
- Important files: `WhipApplication.kt`, `core/AppSettings.kt`, `GymRepository.kt`, `MeasurementRepository.kt`, `TaskViewModel.kt`, `HabitViewModel.kt`, `GoalViewModel.kt`, `TrackViewModel.kt`, `WhipApp.kt`, `UnifiedSearchDialog.kt`, `GymScreens.kt`, `TrackScreens.kt`, and focused JVM/Android tests.
- Persistence/history impact: No Room or backup migration. Existing Task occurrences, Habit/Measurement history, Goal entries, Track entries/import drafts, workout sessions, completed sets, saved zone IDs, and elapsed origins are not recomputed or rewritten.
- Commit/push: `8b27374` on `origin/main`.
- Related: `FND-20260831-009`, `DEC-20260831-010`.
- Verification: `VER-20260831-008`.
- Status: Implemented, focused-verification complete, committed, and pushed; followed by the exact elapsed-Goal subchunk in `IMP-20260831-007`.

### IMP-20260831-007 — Exact elapsed-Goal time and DST behavior

- Behavior changed: Elapsed Goal cards, adaptive summaries, Insights, inspectors, editors, and reset dialogs now share the ViewModel's injected live instant and active Whip zone. An unchanged editor/reset draft preserves the canonical stored instant byte-for-byte, including seconds and milliseconds, even across recomposition, recreation, or a later zone change. Editing wall time uses an exact resolver: spring-forward gaps are rejected with the next valid time explained, and fall-back overlaps expose both offsets and require a deliberate occurrence choice. Start labels use the Whip zone rather than raw UTC/device time. The 30-second ticker runs while Goal UI is subscribed, so future validation also advances while creating or converting the first elapsed Goal.
- Accessibility/UX: The overlap choices are full-width 48dp-class actions with explicit first/second offset labels and selected text. Gap, overlap, and future-state explanations are polite live regions. Reset actions wrap at constrained widths; a 320dp, 200%-font emulator regression constrains the actual dialog surface and proves Reset Now, Cancel, and the chosen-time action remain within it with at least 48dp targets while exact-instant preservation holds.
- Important files: `core/AppRuntime.kt`, `GoalViewModel.kt`, `GoalScreens.kt`, `ProductivityEditorComponents.kt`, `AppRuntimeTest.kt`, `ElapsedGoalPresentationTest.kt`, and `ElapsedGoalTimeUiTest.kt`.
- Persistence/history impact: No schema or backup-format change. `elapsedStartMillis` remains the authoritative immutable instant until a user explicitly saves a changed time; no prior Goal history is rewritten.
- Commit/push: Focused elapsed-Goal commit containing this entry on `origin/main`.
- Related: `FND-20260831-009`, `DEC-20260831-010`.
- Verification: `VER-20260831-009`.
- Status: Implemented and fully verified; integrated physical-device release remains deferred until the maximum-quality goal is complete.

### IMP-20260831-008 — Request-owned productivity definition saves

- Behavior changed: Task, Habit, and Goal definition editors now retain their draft, scroll position, and Area context until their exact asynchronous request settles. Saving blocks Back, pointer, and hardware-key editing with an accessible live overlay. Failure stays inline and retryable; success dismisses or opens a fresh Save-and-New session exactly once and only then reconciles the visible Area.
- Architecture: `PersistenceRequestState` and `EntitySaveReceipt` separate request ownership from global presentation. Admission is atomic and Idle-only; stale terminal delivery is reclaimed without adopting unrelated Running work. `completeCommittedEntitySave` marks the authoritative repository write as the point of no return, preserves the committed identity across cancellation, converts only ordinary post-commit failures to warnings, and lets fatal errors propagate. Saved Area is authoritatively reread; unverifiable or no-longer-active Areas route safely to All Areas. Every authored save retries tag and reminder derivation, making the recovery message truthful.
- Important files: `core/AppRuntime.kt`, `domain/AreaScope.kt`, `ui/ProductivityEditorComponents.kt`, `TaskViewModel.kt`, `HabitViewModel.kt`, `GoalViewModel.kt`, `WhipApp.kt`, `TaskEditorDialog.kt`, `HabitScreens.kt`, `GoalScreens.kt`, and the focused JVM/Android tests.
- Persistence/history impact: No Room schema or backup-format change. Existing entities, completed records, reminder definitions, Areas, tags, and historical data are untouched. The change governs only the sequence and outcome reporting of new authored saves.
- Compatibility: Legacy non-editor ViewModel callers retain Boolean/global operation feedback. UI defaults no longer fabricate success when a request path is absent. Filtered list projections remain scoped while editor entity resolution uses unscoped authoritative state or a captured Task snapshot.
- Commit/push: Focused productivity-save commit containing this entry on `origin/main`.
- Related: `FND-20260831-010`, `FND-20260831-019`, `DEC-20260831-011`.
- Verification: `VER-20260831-010`.
- Status: Implemented, independently accepted, fully verified, committed, and pushed; integrated physical-device release remains deferred.

### IMP-20260831-009 — Request-owned Habit history, pauses, totals, and skip undo

- Behavior changed: Current Habit totals, past check-ins, edited/deleted logs, scheduled pauses, and historical skip undo now remain open and input-shielded until their exact request settles. Failure is announced inline with the draft retained; success closes editors exactly once. The Habit inspector exposes editable scheduled pauses and historical skipped days. Absolute Count/Decimal/Duration entry now says which period total is being set, shows the current total, and explicitly says Save sets rather than adds.
- Architecture: A typed `HabitMutationReceipt` and committed-mutation boundary separate authoritative Room writes from reminder follow-up warnings. Generic persistence coordination now namespaces Home quick entry, Home Habit details, and the Habit workspace; a non-owner cannot steal another surface's result, while an abandoned terminal is reclaimed after an owner grace period. Log/pause snapshots are saveable through target removal and recreation. `UserDataGenerationBoundary` recreates screen identity state after replace restore, and `HabitViewModel` clears outstanding request state on generation changes.
- Correctness: Repository transactions re-read log/pause ownership and require the expected Habit before update/delete. Skip undo requires one exact stored Habit/date row. Absolute Set re-reads the authoritative Room total and all custom units, ignores only bounded binary ULP noise, and writes real high-magnitude changes. Editing a Habit log preserves the backing Measurement entry's Habit/UUID provenance.
- Accessibility/UX: Saving produces one accessible interaction-blocking overlay for dialogs or the full Habit inspector. Draft fields remain scrollable; a 320dp/200%-text regression proves the pause dialog stays within its pane and its Save target remains at least 48dp. State-restoration regressions cover log/pause target removal and user-data-generation replacement.
- Persistence/history impact: No Room schema or portable-backup change. Existing Habits, logs, skips, pauses, custom units, reminder definitions, and historical timestamps remain unchanged. Previously completed history is never recomputed from current settings.
- Important files: `AppRuntime.kt`, `HabitModels.kt`, `HabitDao.kt`, `HabitRepository.kt`, `MeasurementDao.kt`, `MeasurementRepository.kt`, `CoordinatedReminderRepositories.kt`, `HabitViewModel.kt`, `HabitScreens.kt`, `EntityInspector.kt`, `ProductivityEditorComponents.kt`, `WhipApp.kt`, and focused JVM/Android regressions.
- Commit/push: Focused Habit-mutation commit containing this entry on `origin/main`.
- Related: `FND-20260831-019`, `DEC-20260831-015`.
- Verification: `VER-20260831-011`.
- Status: Implemented, independently accepted, and fully verified; integrated physical-device release remains deferred while the maximum-quality goal continues.

### IMP-20260831-010 — Exact Task secondary-mutation and recurring-series integrity

- Behavior changed: Task editors retain drafts and exact edit identity across recreation; stale same-millisecond definition, occurrence, or Subtask changes fail closed. Completed/skipped/archived recurring records edit the series definition, while an open occurrence may safely edit this-and-future. Reschedule, Plan My Day undo, bulk edit/archive, pin, completion/reopen/reset, delete, and notification actions now surface the exact request outcome rather than assuming dispatch means success. Saving and bulk operations shield duplicate input and retain retry context. Date controls, destructive actions, and large-text layouts remain reachable.
- Recurring correctness: Future splits preserve closed history and the old definition, remaining finite occurrence counts, Carry Unfinished state, reschedules, compatible future Open state, Track mappings, Link/Trigger child rules, and stable Subtask identity through reorder/insertion. Inbound automations retarget without duplicate firing. State-only future progress materializes an Open occurrence when necessary; schedule- and completion-anchored projections plus reminders treat explicit Open rows as authored overrides. Copied Goal Links preserve the later of split boundary and configured activation date.
- Commit boundaries: Permanent deletion fingerprints all dependent state, including occurrence/step history, Link/Trigger conditions/choices/mappings, inbound/outbound automation, and linked Track entries. Post-commit ordinary failures become warnings; committed cancellation carries a typed receipt; fatal reconciliation escapes. Notification action claims release only before the authoritative mutation and remain committed through fallible follow-up, preventing replay after success.
- Compatibility/history impact: No Room schema, migration, or portable-backup change. Existing Task UUIDs, definitions, custom Areas/tags, completed/skipped occurrences, completed-set-like Subtask snapshots, Links, automations, Track history, and reminders are preserved. Previously closed history is never recomputed from current recurrence settings. No physical-device mutation or signed release occurred.
- Important files: `TaskModels.kt`, `TaskDao.kt`, `TaskRepository.kt`, `TaskDeletionCoordinator.kt`, `TrackDao.kt`, `ReminderActionReceiver.kt`, `ReminderScheduler.kt`, `ReminderWorker.kt`, `CoordinatedReminderRepositories.kt`, `TaskViewModel.kt`, `TaskComponents.kt`, `TaskEditorDialog.kt`, `WhipApp.kt`, and focused JVM/Android regressions.
- Commit/push: Focused Task-integrity commit containing this entry on `origin/main`.
- Related: `FND-20260831-019`, `DEC-20260831-016`.
- Verification: `VER-20260831-012`.
- Status: Implemented, independently accepted, and fully verified; integrated physical-device release remains deferred while the maximum-quality goal continues.

### IMP-20260831-011 — Exact Goal lifecycle, archive, history, deletion, and backup integrity

- Behavior changed: Goal definition, duplicate, progress create/edit/delete, elapsed reset, status, archive, and permanent deletion flows now surface matching request outcomes, preserve drafts/errors through failure and recreation, block duplicate input while saving, and separate Home from workspace ownership. Progress dates in the future are rejected; dates outside the tracking window require explicit History-only confirmation. History is the truthful shared destination for Completed and Abandoned Goals. Closed/archived Goal-owned progress may be corrected without rewriting the terminal result.
- Domain/history: `GoalMutationBoundary`, `GoalProgressBoundary`, `GoalMeasurementBoundary`, `GoalMilestoneBoundary`, and `GoalEligibilityBoundary` validate only relevant semantic state. Archive is an orthogonal Boolean. Closure snapshots have stable UUIDs and freeze value/progress, elapsed duration, and milestone completion counts; elapsed resets retain old/new origins and prior duration. Same-state milestone writes preserve timestamps, and only open unarchived Goals may gain a pin or new progress.
- Persistence/compatibility: Room schema 38 adds orthogonal archive state, stable closure identity/specialized outcomes, and elapsed-reset history. The migration converts legacy Archived rows without erasing recoverable Completed/Abandoned outcomes. Portable-backup format 16 preserves archive/closure/reset history; pre-v16 upgrades synthesize deterministic closure UUIDs, and repeated merge is idempotent. Existing Goal measurements, completed records, custom units, user exercises, and workout history are not retroactively recomputed.
- Deletion/reminders: Goal deletion SHA-256 revisions cover the Goal, metric, entries, milestones, closure/reset history, and Link rules/conditions/choices/contributions. Area deletion owns aggregate Room deletion transactionally; post-commit settings/focus/widget cleanup continues through ordinary warnings and preserves fatal errors. Archived Goals never schedule or validate reminder claims; archive participates in the reminder fingerprint, and milestone automation excludes archived/non-active Goals.
- Cross-suite correction: Strict authored Measurement edits exposed Health Connect's distinct deterministic upsert contract and a notification fixture that changed source identity while testing value changes. Health reconciliation can now recreate only an identified `HealthConnect` row whose stable ID equals `entry-$sourceId`; existing metric/provenance remains immutable. Link rebuilds create a replacement derived entry when its referenced row is absent. The notification regression retains one provider identity.
- Accessibility/UX: Large-text and narrow dialog content scrolls, input/save/back behavior is explicit, errors use one live region, custom units render human labels, terminal elapsed/milestone outcomes are frozen, and permanent deletion previews complete impact before confirmation.
- Important files: `GoalModels.kt`, `GoalEntities.kt`, `GoalDao.kt`, `GoalRepository.kt`, `MeasurementRepository.kt`, `LinkRepository.kt`, `DomainDeletionCoordinator.kt`, `AreaDeletionCoordinator.kt`, `BackupRepository.kt`, `WhipDatabase.kt`, `GoalReminderScheduler.kt`, `GoalViewModel.kt`, `GoalScreens.kt`, `SettingsViewModel.kt`, and focused JVM/Android/migration/backup tests.
- Related: `FND-20260831-015`, `FND-20260831-016`, `FND-20260831-019`, `DEC-20260831-017`.
- Verification: `VER-20260831-013`.
- Status: Implemented, independently accepted, and fully verified; integrated physical-device release remains deferred while the maximum-quality goal continues.

### IMP-20260901-012 — Exact Gym Exercise and Routine deletion integrity

- Behavior changed: Exercise and Routine permanent deletion no longer dispatch and dismiss optimistically. The UI first loads a complete structured impact, blocks confirmation for active workout use, rejects a stale revision, retains the modal through its exact request outcome, and provides inline retry/review states. The Exercise review distinguishes removed workout/routine sets, changed alternative/machine/category references, deleted PR/graph/Link/Trigger definitions, retained automation-created Track history, and preserved Training Max decisions. Routine deletion states that template/program state is removed while completed/discarded workout snapshots and Training Max decisions remain.
- Domain/architecture: `DomainDeletionCoordinator` now provides transactional Exercise/Routine previews and SHA-256 revisions over every affected row. Exact count assertions guard cascades and reference rewrites. Active Exercise placements—including active substitution alternatives—and active Routine-sourced sessions fail closed. Routine workout source references are cleared only after the reviewed count matches. Training Max decision rows are immutable audit records and are never deleted. PR/Link/settings reconciliation is post-commit, with typed committed-cancellation and ordinary warning semantics.
- Lifecycle/accessibility: Gym deletions have dedicated request-owned state instead of borrowing global `OperationStatus`. Rapid double confirm admits one request. Preview generations reject stale reads; user-data generations invalidate restored numeric identity. A `SavedStateHandle` token plus saveable candidate identity preserves unknown-outcome verification through rotation and repeated process replacement. Present targets resolve to interrupted-before-commit; absent targets rerun idempotent reconciliation and resolve achieved; transient reads keep Retry Verification. Dialog loading, ready, blocker, error, missing, and saving states have polite semantics, modal input blocking, sticky actions, and 320dp/200% reachability.
- Persistence/history impact: No Room schema, migration, or backup-format change. No existing record is rewritten on upgrade. A user-confirmed Exercise permanent deletion removes exactly the disclosed placements/sets and dependencies; archive remains the non-destructive alternative. Routine deletion preserves already performed/discarded workout prescriptions and sets, and both deletions preserve immutable Training Max decisions.
- Important files: `DomainDeletionCoordinator.kt`, `GymDao.kt`, `RoutineDao.kt`, `GymViewModel.kt`, `GymScreens.kt`, `DomainDeletionCoordinatorTest.kt`, `GymDeletionViewModelIntegrationTest.kt`, `GymPowerInputUiTest.kt`, and `docs/testing.md`.
- Related: `FND-20260831-019`, `FND-20260901-021`, `DEC-20260901-018`, `DEC-20260901-019`.
- Verification: `VER-20260901-014`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff and physical-device release remains deferred while the maximum-quality goal continues.
