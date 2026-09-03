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

### IMP-20260901-013 — Exact Track definition and history integrity

- Behavior changed: Editing a Track definition now freezes the exact authored definition the user opened. Save rejects concurrent identity or semantic changes without overwriting them, retains the local draft, and offers a normal “Save Draft as New Track” recovery. Removing Fields or Choices requires a repository-originated review of exact affected values, replacement destinations, and dormant legacy Link/Trigger references; any changed impact returns to review. Entry routes resolve from unscoped, generation-bound identity and show an explicit unavailable surface instead of rendering blank or turning Edit into Add.
- Domain/architecture: `TrackDefinitionBoundary` hashes Track identity, editable metadata, and ordered Field/Choice semantics while intentionally excluding Entries, pin/archive/list order, timestamps, derived Area names, and search projections. A separate reviewed-removal fingerprint covers the normalized deletion/replacement plan, exact affected value rows, replacement sibling joins, and dormant compatibility references. Both are recomputed and validated in the same Room transaction before any mutation. Typed conflicts and committed receipts distinguish pre-commit rejection from authoritative success and fallible post-commit Area/tag reconciliation.
- Correctness/compatibility: Field and Choice deletions assert exact transactional postconditions. Choice replacement deduplicates destination joins. Number dimension changes are rejected when history or dormant references exist; same-dimension default-unit changes preserve entered and canonical historical values. Scale changes validate saved values and dormant Trigger constants. Archived Area/unit references may be retained but cannot be newly selected. Schema-31-retired automation definitions are disclosed and reconciled without reviving dead-end configuration, while generated Contribution/TriggerOccurrence history remains intact.
- Lifecycle/accessibility: The definition save coordinator admits one exact request, scopes settlement to route/session/generation, blocks pointer/Back/key input during persistence, and treats the Room commit as authoritative. Unknown process outcomes explain how to verify before retry. Permanent conflict closes stale removal review, clears only its authorization, scrolls the conflict recovery card into view, and announces it. The exact review remains scrollable with reachable actions at 320dp/200% text. Dirty Field dialogs require discard confirmation, and valued Number fields explain locked measurement type/default-unit rules.
- Important files: `TrackModels.kt`, `TrackDao.kt`, `LinkDao.kt`, `TrackRepository.kt`, `TrackEditorViewModels.kt`, `TrackViewModel.kt`, `TrackScreens.kt`, `ProductivityEditorComponents.kt`, `WhipApp.kt`, `TrackDefinitionIntegrityTest.kt`, `TrackDefinitionMutationUiTest.kt`, `TrackRepositoryTest.kt`, and focused JVM/UI regressions.
- Persistence/history impact: No Room schema, migration, or portable-backup change. Existing Tracks, Entries, values, timestamps, Contribution/TriggerOccurrence history, Areas, units, and retired compatibility rows are preserved unless the user approves the exact disclosed destructive impact. Ordinary definition edits do not conflict with unrelated Entry logging/import.
- Related: `FND-20260901-022`, `FND-20260831-019`, `DEC-20260901-020`.
- Verification: `VER-20260901-015`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff and physical-device release remains deferred while the maximum-quality goal continues.

### IMP-20260901-014 — Exact Track Entry mutation and same-process recovery integrity

- Behavior changed: Adding or editing a Track Entry now begins from an atomic repository preparation instead of a lagging list projection. Save/delete owns one exact route, session, data generation, request, form contract, and historical Entry revision; conflicts or failures leave the editor and draft open, while success dismisses exactly once. Delete reviews the persisted date/value count and closes only after commit. Optional malformed Number input remains visible, named, announced, and blocks Save rather than becoming an accidental blank. Active and archived historical units remain visible and understandable.
- Domain/architecture: Create preallocates a saveable Entry UUID and is idempotent only for the exact normalized payload; mismatched reuse is an identity collision and a fresh UUID still permits intentional duplicates. Update/delete compare raw typed values, stable identities, provenance, Track identity, and semantic form/unit contracts inside one Room transaction, including same-millisecond changes and finite canonical-number validation. Presentation no longer owns mutation truth through shared `OperationStatus`; typed receipts are request/route/generation delivered. Structural Entry page versions prevent same-count/same-timestamp values or Choice changes from leaving stale pages.
- Recovery/history: Delete snapshots the exact Entry, values, source occurrence, and all fulfilled occurrences. Same-process Undo restores UUIDs, timestamps, value content, and occurrence links atomically; incompatible Track/Field/value/unit/provenance identity rolls back. Harmless labels, added Fields, archive decoration, and unchanged unit conversions remain compatible. One-level Undo is explicit: confirming a newer delete supersedes an untouched prior Undo, while a failed restore remains bound to its exact snapshot and Retry action.
- UI/accessibility: Pointer, Back, keyboard, and destructive controls are shielded during persistence. Loading, archived, missing, verification failure, conflict, unknown-outcome, and retry states use explicit copy and live semantics. The editor works at 320dp/200% text, Number fields expose their name/unit and raw editable text, and feedback arbitration preserves recoverable actions across destinations without cross-feature Snackbar theft. Cohesive feedback effects were extracted from the root Compose host to keep JaCoCo instrumentation viable without exclusions.
- Persistence/history impact: No Room schema, migration, or portable-backup change. Existing Track, Entry, typed-value, Contribution, TriggerOccurrence, Area, unit, and completed historical facts remain intact; no prior Entry is retroactively recomputed. Undo is intentionally same-process rather than a durable tombstone.
- Important files: `TrackModels.kt`, `TrackDao.kt`, `LinkDao.kt`, `TrackRepository.kt`, `TrackEditorViewModels.kt`, `TrackViewModel.kt`, `TrackScreens.kt`, `TrackEntryFeedback.kt`, `TransientFeedback.kt`, `OperationFeedbackEffects.kt`, `WhipApp.kt`, `TrackEntryIntegrityTest.kt`, `TrackEntryMutationUiTest.kt`, `TrackRepositoryTest.kt`, `LinkBackfillRepositoryTest.kt`, `InteractionControlUiTest.kt`, and focused JVM regressions.
- Related: `FND-20260901-023`, `FND-20260831-019`, `DEC-20260901-021`.
- Verification: `VER-20260901-016`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff. Physical-device release remains deferred while the maximum-quality goal continues.

### IMP-20260901-015 — Durable, exact, and accessible Track CSV batch imports

- Behavior changed: CSV import now owns one saveable batch identity from file selection through completion. Rapid submit is shielded; process-restored sessions verify a complete durable receipt before touching the document URI; exact completed retries return the prior outcome; missing receipts retain the reviewed preview for explicit retry; changed files, stale mappings, replaced Tracks, malformed requests, and batch/Entry identity collisions fail closed. Strict UTF-8 decoding rejects damaged/binary input, CSV quote/header/width rules are explicit, and trailing blank records cannot falsely exceed the data-row limit.
- Domain/architecture: Preview parses exclusively against an atomic `TrackEntryFormSnapshot`, not a lagging projection plus separately cached units. Versioned length-prefixed canonical SHA-256 binds stable Track identity, payload, mapping, fallback date, exact Field/Choice/unit meaning, normalized drafts, and protocol versions. Deterministic UUIDv8 row identities plus a private Track-cascaded receipt make Entries, typed values, FTS rows, and outcome one Room transaction. A fresh batch UUID deliberately permits identical facts. Selected/default unit contracts are compared once per distinct unit; archived non-default units reject while a retained archived Field default remains valid.
- UI/accessibility: The dialog distinguishes target loading, load failure, settled absence, archive, domain conflict, retryable persistence failure, and terminal completion. Completion supersedes stale live projection state. Authoritative errors/actions render before file and mapping detail, remain live at 320dp/200% text, and never contradict a generic coordinator banner. Mapping uses the frozen form even for invalid previews; lookup retry is generation-owned and preserves the session/frozen form. File/date/mapped-count context, progressive mapping disclosure, full input shielding, replacement cancellation, and understandable Replace/Choose/Cancel/Restore actions support one-handed recovery.
- Persistence/compatibility: Room schema 39 adds only `track_csv_import_receipts`, containing stable identity, versioned digests/counts, and commit time—no URI, filename, header, mapping, or value. The row cascades with its Track, is intentionally excluded from portable backup format 16, survives merge/failure rollback locally, and is cleared by successful replace through Track cascade plus data-generation invalidation. Existing Entries, values, FTS, custom units, completed history, and user-defined schemas are not backfilled or recomputed.
- Performance/testing: Batch insertion reuses the loaded form and normalized maps, writes FTS without discarded snapshot reloads, chunks identity preflight, and checkpoints cancellation. The exact 5,000-row × 20-Number-Field case persists 100,000 custom-unit values, 5,000 Entries/FTS rows, one receipt, search reachability, and exact retry in 40.525 seconds on the API 34 emulator (prepare 2.807 s, commit 36.495 s, retry 1.223 s; 120 s harness ceiling).
- Important files: `TrackCsv.kt`, `TrackEntities.kt`, `TrackDao.kt`, `TrackRepository.kt`, `WhipDatabase.kt`, schema 39, `TrackViewModel.kt`, `TrackScreens.kt`, CSV strings, `TrackCsvBatchIdentityTest`, `TrackCsvInputBoundaryTest`, `TrackCsvImportIntegrityTest`, `TrackCsvImportRecoveryViewModelTest`, `TrackCsvImportUiTest`, `WhipDatabaseMigrationTest`, `BackupRepositoryTest`, and `TrackDefinitionIntegrityTest`.
- Related: `FND-20260901-024`, `DEC-20260901-022`.
- Verification: `VER-20260901-017`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff. Physical release remains deferred until the maximum-quality goal is complete.

### IMP-20260901-016 — Exact, recoverable Gym session execution and 5/3/1 outcomes

- Behavior changed: Adding or creating an Exercise for the active workout atomically creates an editable first Set, returns to the workout, and focuses that Set without changing the Routine. Quick-save is bound to stable Set/workout identity and exact revisions; concurrent identical saves append at most once. Workout-only and replacement placements become executable immediately. Finish reviews incomplete work, binds the exact session graph, preserves all saved values in History, and applies explicit Training Max decisions only at valid 5/3/1 boundaries. Joker acceptance remains optional and additive; BBB/FSL/BBS/supplemental work is never displaced.
- Domain/history: `WorkoutExerciseOutcome`, replacement identity, Set removal reasons, authored classification, and `requiredForProgressionSnapshot` separate performed facts from current execution eligibility. Completed retired placements remain available to History and performance review; empty retired placements are not executed or reused. Main, Supplemental, Assistance, and Optional work retain distinct snapshots. Generic workout/routine copies deliberately sanitize program-only progression/failure semantics. Training Max advancement uses immutable required Main-work evidence rather than mutable `planned`; held/ineligible lifts remain auditable.
- Lifecycle/UI/accessibility: Add, create, substitute, detailed Set/Exercise edit, finish/cycle review, and discard use request-owned outcomes. Draft/error state survives Activity recreation; orphaned process results release with an actionable verification warning. Quick-set authorship survives recreation. New-exercise focus is consumed only after a coherent Room snapshot contains the target and scroll completes. Stable finish semantics, scrollable confirmation, narrow 320dp/200% layouts, live errors, input shielding, and explicit History wording support one-handed and assistive use.
- Derived-state reliability: The active Gym projection now rereads Exercise/session/placement/Set/group rows in one Room transaction after invalidation, avoiding mixed-revision UI boundaries. Personal-record reconciliation includes existing PR rows so deleting the last completed Set can remove stale records. Finish/delete/skip/undo distinguish authoritative commit from fallible PR/Link follow-up and warn without inviting duplicate writes. Rest timers carry monotonic revision, exact deadline, and durable cleanup-pending state. Schedule/cancel awaits WorkManager; worker delivery posts a stable notification before exact DB completion, retries ordinary failure, rejects stale generations, and replays idempotently after process death. Gym open and app background recovery rebuild derived timer/PR state from durable rows.
- Persistence/compatibility: Room schema 40 adds `workoutRevision`, immutable placement/Set outcome/progression snapshots, and `restTimerRevision`/`restTimerCleanupPending`; migration derives required Main-work evidence from immutable prescription/classification rather than current `planned` alone and marks preexisting timer state for one cleanup reconciliation. Portable backup format 17 round-trips the new historical facts and defaults pre-v17 fields conservatively. Completed sets, user Exercises, workout equipment/unit/TM snapshots, Routine provenance, and prior history are preserved; completed workouts are never recomputed from a later Routine definition.
- QA-driven corrections: Independent QA first rejected stale PR cleanup and timer reconciliation, then rejected unawaited WorkManager operations and DB-clear-before-notify delivery. The fixes added exact timer acknowledgement and notification-first at-least-once delivery. The full emulator gate then exposed an obsolete E2E path that added a redundant Set after Exercise selection plus a receipt-before-projection focus race. The journey was updated to the intended atomic initial-Set behavior; production gained a coherent Room graph and retained focus handoff. Re-review returned unconditional `ACCEPT`.
- Important files: `GymModels.kt`, `GymEntities.kt`, `GymDao.kt`, `GymRepository.kt`, `RoutineRepository.kt`, `WhipDatabase.kt`, `BackupRepository.kt`, schema 40, `RestTimerNotifications.kt`, `WhipApplication.kt`, `FiveThreeOneCycleReview.kt`, `GymViewModel.kt`, `GymScreens.kt`, `GymRepositoryTest.kt`, `RoutineRepositoryTest.kt`, `WhipDatabaseMigrationTest.kt`, `BackupRepositoryTest.kt`, `GymPowerInputUiTest.kt`, `FiveThreeOneCycleReviewUiTest.kt`, `FirstClassWorkflowE2ETest.kt`, and focused JVM policy/worker tests.
- Related: `FND-20260831-019`, `FND-20260901-025`, `DEC-20260901-023`.
- Verification: `VER-20260901-018`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff. Physical release remains deferred until the maximum-quality goal is complete.

### IMP-20260901-017 — Transactional, accessible typed Settings editing

- Behavior changed: Numeric, clock, time-zone, and other typed Settings are now action rows that open one focused editor. Values remain local until explicit Save; strict parsers reject prefixes and malformed whole fields; normalized no-ops close without persistence; failures retain the draft and offer exact retry; rapid submit admits one request. Region zones, fixed offsets, Follow device, and coupled quiet-hour modes preserve their distinct meaning. Oversized paste is invalid rather than silently truncated. Back/outside/Escape requires deliberate discard when a dirty, conflicting, or durability-ambiguous intent exists, while explicit Cancel deliberately discards it.
- Domain/persistence: `SettingsRepository.updateAndConfirm` and `PortableBackupManager.setRetentionCountAndConfirm` provide serialized durable commits for authored typed values. A failed `SharedPreferences.commit()` restores the prior process-visible preference state under the same reentrant lock before another writer can observe it. Portable-backup state uses the same rollback rule. Normal lightweight updates retain asynchronous `apply()`. `SettingsViewModel` atomically admits one request, commits off main, delivers a request-scoped `SettingsMutationReceipt`, resets on user-data generation changes, and holds reminder mutation ownership through durable save plus synchronization follow-up. A committed reminder save with a sync failure returns a warning rather than a false retry.
- Lifecycle/conflict semantics: The editor stores a bounded saveable draft, source identity, and durability-retry obligation. Running requests do not let live/current values redefine the durable baseline. External same-field or parent-mode changes retain the local draft and show a conflict instead of overwriting it. Parent controls, compact navigation, and section changes cannot silently dispose an active child editor. A false commit remains retryable even when Android briefly published the attempted value in process memory.
- Accessibility/responsiveness: Non-primary dialogs opt out of platform decor fitting so explicit IME insets keep Save and Cancel reachable; primary full-screen editors retain their established behavior. Actions remain above the keyboard, dialog content can grow/scroll instead of using a tiny fixed window, status/error changes use live semantics, focus returns to the initiating row, and 320dp/200% text plus keyboard/Escape behavior are covered. Settings copy now distinguishes quick choices from typed values.
- Persistence/compatibility: No Room schema, migration, portable-backup format, or historical-record change. Existing Settings keys and values remain compatible; completed workouts and other history are not recomputed. The only persistence change is that explicitly saved typed values wait for durable confirmation and roll back process-local state on a false commit.
- Important files: `AppSettings.kt`, `PortableBackupManager.kt`, `SettingsViewModel.kt`, `SettingsScreens.kt`, `ProductivityEditorComponents.kt`, `SettingsPresentationPolicyTest.kt`, `SettingsResponsiveUiTest.kt`, `SettingsBehaviorUiTest.kt`, `AppSettingsPersistenceTest.kt`, and `docs/testing.md`.
- Related: `FND-20260831-013`, `FND-20260831-019`, `FND-20260901-027`, `DEC-20260901-024`.
- Verification: `VER-20260901-019`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff. Physical release remains deferred until the maximum-quality goal is complete.

### IMP-20260901-018 — Exact Gym structure, layout Undo, and recoverable History Copy

- Behavior changed: Active-workout structure edits no longer rely on loose callbacks or always-visible drag affordances. Add/create/substitute returns to the workout with one editable Set; machine create-and-assign is atomic; remove, ungroup, group, and every Set mutation bind the exact reviewed target; and layout editing uses an explicit responsive Arrange mode with one atomic Save/Cancel boundary plus exact Undo. Required Main Set removal is worded as “Mark Main Set Not Performed.” Finish, discard, grouping, menus, quick completion, and structure controls visibly disable while any conflicting Gym mutation is running.
- Domain/persistence: `WorkoutStructureBoundary`, `WorkoutPlacementBoundary`, `WorkoutSetBoundary`, arrangement snapshots, finish boundaries, and versioned receipts capture stable UUIDs and canonical structure revisions. Canonical SHA-256 structure fingerprints include session, placement, group, and Set structure while excluding mutable Set values/completion. Repository transactions implement idempotent achieved-state replay, stale rejection before writes, collision-free renumbering, tombstone/retired-placement preservation, value-preserving same-session restore, true no-op normalization, exact machine retarget guards, exact discard, and replay-safe History Copy with requested placement/Set identities.
- Lifecycle/UI/accessibility: Active-session structure and History Copy have separate namespaced persistence coordinators. History-copy authorship is strictly encoded in `SavedStateHandle` with source/target versions, requested UUIDs, and data generation; malformed or truncated payloads fail closed. Combined busy state blocks conflicting owners without sharing their errors. Arrange controls remain reachable at narrow width and 200% text, drag handles appear only in Arrange, destructive dialogs freeze the reviewed boundary through recomposition, dynamic status/errors remain with their initiating surface, and nested create flows close safely on generation reset.
- History/compatibility: Completed Set values, completion state, program classification, retired placements, tombstones, and existing workout history remain what actually occurred. No Room schema, migration, portable-backup format, or historical recomputation changed in this tranche.
- QA-driven corrections: Review rejected partial/global authorship, tombstone-blind order, full-graph fingerprints that would erase newer Set values, process-local History Copy identity, interactive controls during a separate copy request, and an obsolete machine-retarget message assertion. Each gained a narrow production or regression correction. Three independent reviewers returned unconditional `ACCEPT` only after the final combined-busy and value-preserving Undo campaigns.
- Important files: `GymModels.kt`, `GymDao.kt`, `GymRepository.kt`, `GymViewModel.kt`, `GymScreens.kt`, `GymRepositoryTest.kt`, `GymRepositoryTestSupport.kt`, `RoutineRepositoryTest.kt`, `GymPowerInputUiTest.kt`, `GymDeletionViewModelIntegrationTest.kt`, `SafetyChoiceUiTest.kt`, and `GymUxRulesTest.kt`.
- Related: `FND-20260831-019`, `FND-20260901-025`, `FND-20260901-026`, `DEC-20260901-023`, `DEC-20260901-025`.
- Verification: `VER-20260901-020`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff. Physical release remains deferred because no phone is connected and the maximum-quality whole-product goal remains active.

### IMP-20260901-019 — Exact Health mirror, Custom Units, semantic restore, and exclusive reset

- Behavior changed: Health Connect category choices now express least-privilege scope even while paused, provider availability/install paths are explicit, interrupted sync/delete/policy actions recover durably, and local-copy deletion cannot imply that provider records or Android permissions were removed. Custom Unit create/rename/archive/version flows retain drafts, expose inline validation, use accessible focused dialogs, and remain usable at 320dp/200% text. Data & Privacy is ordered Health → Backup → Reset; reset requires deliberate confirmation and its permanent-delete surface scrolls at extreme viewport/text sizes.
- Health correctness: Provider reads, policy changes, sync, reconciliation, and local deletion share one manager mutation boundary. Each exact source prefix/window is validated for stable provenance, pagination, ID/prefix collision, time-zone policy, provider offset, and deletion scope before one atomic repository transaction. Narrow-window sync preserves outside rows; null provider offsets preserve the prior zone for the same unchanged stable record; local deletion is two-phase and clears its journal only after rows and links reconcile. Startup recovery is retryable and fail-closed.
- Custom Unit correctness: Stable caller-provided IDs make create retry exact; rename/archive/version use compare-and-set semantic boundaries; dimension, factor, offset, collision, name, and symbol validation live below the UI. Creation receipts and dialog state are lifecycle-owned. Existing unbounded legacy labels continue to restore, while new authorship is bounded.
- Backup/recovery/history: Portable backups omit installation-local Health action journals and receipts; private recovery snapshots preserve them. Restore preflight proves every unit-bearing Measurement, Habit, Goal, Track, Exercise, Machine, Routine, workout, Training Max, PR, and automation fact against its actual domain contract before replacement. It rejects unknown/blank units, null-parity mismatches, invalid dimensions, non-finite/overflowing canonical values, stable-ID collisions, and Gym values inconsistent with built-in/machine semantics. Completed history is preserved and never retroactively recalculated from current program definitions.
- Reset/concurrency: `StartupRecoveryGate.runExclusiveMaintenance` closes and drains the application-wide data-access gate. Reset quiesces workers/runtime, clears portable-folder ownership, acquires Health → reminder → Room, advances durable user-data generation before deletion, cancels notifications, deletes data, rebuilds runtime, and reopens only at `Ready`. An admitted mutation finishes before reset; late access is rejected; reset removes the admitted result. The Settings caller does not hold a normal data lease, avoiding self-deadlock.
- Legacy integrity: Startup and portable-backup normalization repair the historical custom-unit Habit canonical defect only when a paired metric entry proves exact provenance, metric identity, value, unit, and conversion. Future generated writers take canonical truth from the paired metric row and repair an existing exact generated row on replay.
- QA-driven corrections: Review rejected per-row Health commits, unlocked provider operations, lossy time-zone fallback, portable leakage of local journals, generic unit compatibility, current-UI limits applied to old labels, reset guarded by only local locks, and heuristic legacy repair. Production and regressions were revised after every rejection. Domain/5/3/1, UX/accessibility, and adversarial-QA reviewers returned unconditional `ACCEPT` on the final shared tree.
- Important files: `WhipApplication.kt`, `StartupRecoveryGate.kt`, `HealthConnectManager.kt`, `BackupRepository.kt`, `RestoreRecoveryManager.kt`, `MeasurementRepository.kt`, `MeasurementDao.kt`, `MeasurementModels.kt`, `HabitDao.kt`, `HabitRepository.kt`, `LinkRepository.kt`, `CoordinatedReminderRepositories.kt`, `SettingsViewModel.kt`, `SettingsScreens.kt`, `PermanentDeleteDialog.kt`, `UnitSelectionField.kt`, `GoalScreens.kt`, `HabitScreens.kt`, and the focused Health/backup/recovery/custom-unit/settings test suites.
- Related: `FND-20260831-020`, `FND-20260901-027`, `FND-20260901-028`, `DEC-20260901-026`, `DEC-20260901-027`, `DEC-20260901-028`, `DEC-20260901-029`.
- Verification: `VER-20260901-021`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff. Physical release remains deferred because only the disposable emulator is connected and the maximum-quality whole-product goal remains active.

### IMP-20260901-020 — Bounded, Unicode-safe Share-to-Task capture

- Behavior changed: Text shared from another Android app still opens a prefilled Task with the first nonblank line as title and following lines as subtasks. Oversized deliverable shares show a persistent shortened-draft warning. A second share or widget Add Task can no longer replace a draft: users may replace deliberately or keep editing, then the waiting request opens after Save or confirmed Close. Widget date and Area ownership survive that handoff.
- Input/lifecycle contract: `SharedTaskCapturePolicy` enforces 8,192 raw code points, 32 KiB UTF-8, 200 title code points, 50 subtasks, and 200 code points per subtask before `LaunchRequest` can enter Activity saved state. It normalizes line endings/blank lines and stops only at complete Unicode code points. `MainActivity` accepts Android's standard `CharSequence`, owns a saveable exact-head FIFO, restores an intentionally empty queue without replay, applies capacity only to waiting shares, and preserves all other platform actions. Four shares may wait; later shares collapse into one counted marker. Compose admits Task-editor launches into a saveable text/shortening/date/resolved-Area handoff. Overflow acknowledgment is a non-dismissible saveable dialog bound to delivery ID plus current count; the Activity head is consumed only after exact acknowledgment.
- Failure boundary: Payloads larger than Android's Binder transaction limit can be rejected by the operating system before the Activity starts; Whip cannot observe or recover those. The verified policy covers large payloads Android can deliver and prevents a second failure during recreation or editor state saving.
- Compatibility: No Room schema, backup format, existing Task, Subtask, or historical record changes. Saved Tasks are not rewritten. The change only bounds new external share drafts before persistence.
- QA-driven corrections: Review rejected a 1.5 MB Binder fixture that never reached Whip, a single mutable launch slot, replay of an empty restored FIFO, share-conflict overwrite/discard, generic capacity that dropped non-share actions, transient overflow Snackbar ownership, delivery-only acknowledgment, widget Add Task draft replacement, and loss of queued widget Area. The final regressions separate Android delivery limits from app bounds, prove FIFO/collapsed overflow policy deterministically, bind acknowledgment to delivery/count, and exercise share/widget conflict, schedule, Area, recreation, Unicode, large text, and focused warning semantics.
- Important files: `SharedTaskCapturePolicy.kt`, `MainActivity.kt`, `LaunchDeliveryEffect.kt`, `LaunchQueueOverflowDialog.kt`, `PendingTaskEditorLaunchDialog.kt`, `TaskEditorRouteHost.kt`, `WhipApp.kt`, `TaskEditorDialog.kt`, `SharedTaskCapturePolicyTest.kt`, `LaunchRequestQueueTest.kt`, `LaunchQueueOverflowStateTest.kt`, `PlatformEntrySurfaceE2ETest.kt`, and `EditorDependencyUxTest.kt`.
- Related: `FND-20260831-014`, `DEC-20260901-030`.
- Verification: `VER-20260901-022`.
- Status: Implemented and verified; commit/push is the final chunk handoff. Physical release remains deferred because only the disposable emulator is connected and the maximum-quality whole-product goal remains active.

### IMP-20260902-001 — Simplified the maximum-quality development workflow

- Changed the active reusable goal from mandatory multi-agent orchestration to a conventional single-developer loop: verify, inspect, implement, test proportionately, exercise affected UI, record concise facts, commit, push, and move on.
- Removed future requirements for simulated panels, recursive audits, formal dialectics, Director approvals, and repeated specialist review. Delegation now requires an explicit user request.
- Preserved historical audit records, existing acceptance criteria, data-safety requirements, accessibility expectations, test gates, durable memory, and coherent commit/push discipline.
- Important files: `docs/product-memory/MAXIMUM_QUALITY_GOAL.md`, `USER_FEEDBACK.md`, `DECISIONS.md`, and `INDEX.md`.
- Compatibility: Process/documentation-only change; no application source, schema, backup, test, or user data changed.
- Related: `FB-20260902-001`, `DEC-20260902-001`.
- Verification: `VER-20260902-001`.
- Status: Implemented.

### IMP-20260902-002 — Durable, unit-correct, accessible Habit timers

- Added the schema-41 `habit_timer_sessions` ledger and stable typed Start/Stop/Review outcomes. Production uses Android elapsed realtime plus boot count; Start/Stop is idempotent and exact-owner bound; duplicate or stale actions cannot mutate a newer session; zero duration settles without inventing history.
- Timer Stop now commits canonical seconds, entered value in the frozen Duration unit, paired Measurement/Habit history, and terminal session state atomically. Duration tracking rejects incompatible measurement dimensions. Active timers block archive, pause, unit, and schedule changes while harmless metadata edits remain available.
- Added live monotonic elapsed display, `m:ss`/`h:mm:ss`/day formatting, spoken screen-reader duration, explicit Start / Stop & Log / Review Timer semantics, 48dp targets, an editable recovery dialog with Stop & Log / Continue / Discard, manual-duration access, and timer-first inspector status. Unresolved timers remain reachable despite schedule, Area, selection, pause, archive, or end-state filters.
- Widgets carry stable Start request and exact Stop session identities; stale cached actions fail closed. Running and review states remain visible and use the same elapsed policy as the app.
- Schema 40→41 turns legacy timestamps into ReviewRequired sessions without logging. Backup format 18 validates timer/Habit/unit/mirror coherence, converts portable timers to ReviewRequired, retains exact state only in private rollback, and drops active timers during merge while preserving recorded history.
- Important files: `HabitTimerClock.kt`, `HabitModels.kt`, `HabitEntities.kt`, `HabitDao.kt`, `HabitRepository.kt`, `WhipDatabase.kt`, schema 41, `BackupRepository.kt`, `HabitViewModel.kt`, `HabitScreens.kt`, and Habit widget paths.
- Related: `FND-20260902-001`, `DEC-20260902-002`.
- Verification: `VER-20260902-002`.
- Status: Implemented and verified; commit/push is the chunk handoff.

### IMP-20260902-003 — Progressive Habit reminder and schedule configuration

- Simplified ordinary Habit creation without removing capability: required cadence and its dependent fields remain inline, while default reminders, weekday-specific overrides, ending rules, and first-day-of-week configuration now live behind one clearly labeled in-place disclosure.
- Added an always-visible reminder summary using the device's time format. Configured ending rules and a week boundary that differs from the app default remain visible even while the controls are collapsed, so progressive disclosure does not hide consequential state.
- Existing Habits with reminders, weekday overrides, ending rules, or a non-default week boundary open the section automatically. Power Mode also opens it. A rejected Save automatically reveals the section when an ending-rule error would otherwise be hidden.
- Kept Additional Details independent from schedule complexity; reminder/end settings no longer force tags, quick actions, and notes open. Both disclosure states survive saveable state restoration.
- Added Compose regressions for the basic collapsed path, intentional reveal, configured-data auto-expansion, hidden-error recovery, state restoration, and 200% text reachability.
- Compatibility: UI/state-only change. Habit domain data, Room schema 41, backup format 18, existing reminders, historical check-ins, and scheduling semantics are unchanged.
- Important files: `HabitScreens.kt`, `EntitySaveCoordinatorUiTest.kt`, `AdaptiveWhipScreenTest.kt`, and `docs/testing.md`.
- Related: `FND-20260831-012`, `DEC-20260831-013`.
- Verification: `VER-20260902-003`.
- Status: Implemented and fully verified; commit/push is the chunk handoff. Physical release remains deferred while the whole-product goal continues.

### IMP-20260902-004 — Truthful Habit availability and Today resolution

- Paused and off-schedule cards now use explicit availability summaries and suppress ordinary one-tap check-in, numeric, checklist, flexible-schedule, and target controls. Expanded cards explain how to resume/edit a pause or deliberately log outside the schedule. Active timer Stop/Review remains reachable even if a legacy or unusual state is otherwise unavailable.
- Scheduled-pause inspectors no longer offer a misleading primary check-in. Off-schedule inspectors use mode-specific “Outside Schedule” actions so exceptions are intentional and understandable. Timer start time now follows Whip's configured `LocalWhipZone`.
- Today and Home now classify completed and skipped Habits as “Finished for Today,” collapse them behind a review/undo disclosure, and exclude them from the action-needed Home count. Skip remains neutral and historically distinct from completion; undo continues to restore it to the attention queue.
- Added deterministic classification/status tests, focused Compose card and inspector tests, configured-zone coverage, and updated the full skip/history/insights/undo journey to require correct finished placement.
- Compatibility: UI and derived-presentation logic only. No persistence, schema, migration, backup, scheduling, timer-ledger, check-in, pause, skip, or historical-data format changed.
- Important files: `HabitScreens.kt`, `WhipApp.kt`, `CompactCollectionStatusTest.kt`, `ProductivityCardDesignUiTest.kt`, `ActivityHistoryUiTest.kt`, and `HabitSkipJourneyE2ETest.kt`.
- Related: `FND-20260902-002`, `DEC-20260902-003`.
- Verification: `VER-20260902-004`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-005 — Effective-date Habit History and transparent pause impact

- Replaced write-time-only History assembly with one deterministic effective-date projection for manual/synced check-ins, skipped days, and started scheduled pauses. Same-day entries retain stable secondary ordering; future pauses remain under Options. Pause rows open the existing exact-owner editor directly.
- Renamed the inspector group to “Habit History” and generalized pagination copy from earlier check-ins to earlier events. Pause rows explain that check-ins, misses, and reminders were excluded.
- Insights now treats a started pause as neutral activity, renders its paused grid state, and says “No scored periods” when the recent window contains no completion or miss instead of showing a misleading 0% failure.
- Pause creation/editing warns when the selected range includes today/past and identifies derived streak/consistency recalculation. Delete confirmation states that completed check-ins and skips remain while unlogged dates may become missed. Permanent Habit deletion now counts scheduled-pause records.
- Added one deterministic JVM ordering/filter regression and one pause-only Insights Compose regression; expanded the existing editor/history test for visible pause history, exact edit routing, impact warning, delete consequences, failure-draft retention, and historical skip undo.
- Compatibility: Presentation and confirmation copy only. No repository, calculation, Room schema, migration, backup, check-in, skip, pause, or historical data format changed.
- Important files: `HabitScreens.kt`, `ActivityPresentationTest.kt`, `ActivityHistoryUiTest.kt`, and `docs/testing.md`.
- Related: `FND-20260902-003`, `DEC-20260902-004`.
- Verification: `VER-20260902-005`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-006 — Named adaptive navigation and returning-Home recovery

- Replaced the font-scale icon-only switch with measured label fitting. Compact/tabletop navigation renders all six names in one row when they fit and otherwise uses a stable Home/Tasks/Habits then Goals/Tracks/Gym two-row layout with unchanged direct tab semantics and minimum touch targets.
- Made the persistent rail label-aware: its width follows the widest rendered destination, every item remains named at 150–320% text, item height grows with text, and short/landscape rails scroll directly to Home through Settings instead of stripping labels.
- Added a bounded “Pick Up Where You Left Off” section when settled Home is clear for a returning user. It recognizes Inbox, Upcoming/planning, saved/archived Habits and Goals, unpinned/archived Tracks, and Gym workouts/routines/exercises/machines, displays at most three count-aware routes, and opens the exact relevant destination. Existing Review & Trends recovery remains primary when completion evidence exists.
- Added one JVM policy regression and four Compose regressions covering bounded priority, exact Inbox routing, 150/200/320% compact and rail labels, label containment, stable one/two-row geometry, and scroll reachability in a 360dp-high rail. Expanded resource-policy coverage for all new copy.
- Compatibility: Presentation and in-memory navigation state only. No persistence, migration, backup, entity semantics, history, deep-link, or keyboard-shortcut changes.
- Important files: `WhipApp.kt`, `strings.xml`, `WhipNavigationPolicyTest.kt`, `AdaptiveWhipScreenTest.kt`, `AuditUiStringResourcePolicyTest.kt`, and `docs/testing.md`.
- Related: `FND-20260831-011`, `FND-20260831-017`, `FND-20260902-004`, `DEC-20260902-005`.
- Verification: `VER-20260902-006`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-007 — Explicit Track/Entry search ownership and archive-correct routing

- Centralized workspace search accessibility labels on `WhipSearchEntryContext`: Tracks now announces “Search Tracks & Entries,” Tasks announces “Search Tasks & Steps,” and Gym destinations announce the exact scope they open.
- Removed the unreachable Track-list query, no-match, query/reorder, and clear-query branches. Track list selection and reordering now operate only on the visible active or archived source; Activity and per-Track Entry search remain unchanged.
- Made Track result routing projection-owned: the request waits until the selected Area projection is present, then opens Tracks or Archived from the record's actual state instead of forcing every result into active Tracks.
- Made unified-search empty guidance follow the live scope. The inspected Tracks flow now presents a matching action, “Search tracks & entries” placeholder, “Scope · Tracks & Entries” summary, and “within Tracks & Entries” guidance.
- Added one JVM policy regression and one Compose ownership/routing regression; expanded the production-repository global-search journey to create, archive, find, and open a real Track in the selected Archived destination.
- Compatibility: UI/policy and in-memory navigation only. No repository, search-index, Room schema, migration, backup, Track/Entry identity, Area, history, or reorder persistence change.
- Important files: `WhipNavigationPolicy.kt`, `WhipApp.kt`, `TrackScreens.kt`, `UnifiedSearchDialog.kt`, `strings.xml`, `WhipNavigationPolicyTest.kt`, `TrackWorkspaceUiTest.kt`, `GlobalSearchRoutingTest.kt`, and `docs/testing.md`.
- Related: `FND-20260831-018`, `FND-20260902-005`, `DEC-20260902-006`.
- Verification: `VER-20260902-007`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-008 — Cross-device search visibility and accessibility-matrix hardening

- Reordered emoji search mode around the user's immediate intent: matches appear directly after the search field, redundant headings disappear, search-only spacing tightens, matching saved emoji are brought into view, and custom creation remains available below. Saved-emoji management moves to the dialog footer during search, and choosing/closing explicitly clears focus and hides the software keyboard.
- Gave Unified Search results an owned list state and return-to-summary behavior whenever filter disclosure changes, so active filter summaries cannot remain hidden behind a stale deep scroll position.
- Made affected end-to-end tests viewport-aware without relaxing product assertions: tests scroll the owning Home, workout, Task, or routine surface; derive compact tab edges from the physical root; wait for exact editor/removal semantics; distinguish compact Settings navigation from a persistent wide sidebar; and scope intentional wide master/detail duplicate identities to their semantic owner.
- Added final API 26 emoji screenshot/UI-hierarchy evidence and API 37 actual-TalkBack keyboard-navigation evidence to the product audit artifact tree.
- Compatibility: UI ordering, focus/keyboard behavior, and test targeting only. No persistence, Room schema, migration, backup, search index, emoji identity format, Gym semantics, or historical record changed.
- Important files: `IdentityEmojiPicker.kt`, `UnifiedSearchDialog.kt`, `InteractionControlUiTest.kt`, `CoreFeatureJourneyE2ETest.kt`, `EditorStateRecreationTest.kt`, `WhipNavigationTest.kt`, `WhipComposeSemanticsTest.kt`, `RoutineBuilderUiTest.kt`, `TrackWorkspaceUiTest.kt`, `GlobalSearchRoutingTest.kt`, and `artifacts/full-product-audit/2026-09-02/platform-matrix/`.
- Related: `FND-20260902-006`, `DEC-20260902-007`.
- Verification: `VER-20260902-008`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-009 — Exact and archive-correct Area management

- Added a typed `AreaMutationReceipt` boundary covering create, rename, color, reorder, move-all, merge, archive, restore, delete-while-moving, and delete-with-items. `SettingsViewModel` serializes these requests, retains their exact terminal result through Activity recreation, reads destructive truth from repositories, and classifies post-commit Settings reconciliation as a warning rather than a retryable failure.
- Area-management dialogs now stay owned by the initiating request, retain reviewed fields and destination choices on failure, expose in-context saving/errors, disable dismissal and duplicate submission only while persistence is active, and close the matching child surface only after authoritative success. Archive success stays in the Area detail, offers Undo, and uses an exact Restore request.
- Corrected archived-state behavior throughout the flow: archived details execute Restore; archived search matches appear without prior disclosure; archived-name creation says Restore and reactivates the same Area while preserving its saved color; active duplicates still select existing; rename conflicts explain that an archived destination must be restored before merge.
- Wrapped Area color read-modify-write in the existing Room transaction. No generic mutation DSL or schema expansion was introduced.
- Added four Area-management Compose regressions, three exact ViewModel mutation regressions, and two repository archived-identity/conflict regressions. Preserved inspected manager, detail, archive-confirmation, and archive-result screenshots plus UI hierarchies.
- Compatibility: No Room schema, migration, backup format, Area ID, cross-domain assignment, completed history, or saved color was changed. Existing archived identities are restored rather than duplicated.
- Important files: `AreaManagementDialog.kt`, `AreaPicker.kt`, `WhipColorPicker.kt`, `SettingsViewModel.kt`, `AreaRepository.kt`, `AreaFeatureUiTest.kt`, `AreaMutationViewModelTest.kt`, `MeasurementTaxonomyRepositoryTest.kt`, `docs/testing.md`, and `artifacts/full-product-audit/2026-09-02/area-management/`.
- Related: `FND-20260831-019`, `FND-20260901-027`, `FND-20260902-007`, `DEC-20260902-008`.
- Verification: `VER-20260902-009`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-010 — Explicit, cross-domain, archive-correct Tag management

- Added a dedicated full-screen Tag manager with active/archived search, usage counts across Tasks/Habits/Goals/Tracks, explicit Create/Rename/Merge/Archive/Restore flows, in-context saving/error states, archive Undo, and narrow/large-text responsive controls.
- Added typed request-owned Tag mutation receipts. Drafts and destination choices remain in the initiating dialog on failure; every mutating row/menu action, duplicate submission, and dismissal is disabled only while the exact write is active.
- Split Rename from Merge in the repository. Both are one Room transaction and now update Track tags as well as Task, Habit, and Goal tags. Merge requires an active destination and removes only the source after every replacement succeeds.
- Made archive state stable: ordinary taxonomy reconciliation reuses an archived identity without restoring it, while explicit Create/Restore can reactivate the same ID. Item references and searchability remain intact. Active-only Tag suggestions no longer advertise archived labels in Task editing and bulk editing.
- Rejected comma-containing Tag names at both UI and domain boundaries because comma is the current denormalized persistence separator. Deduplication during replacement now uses locale-stable normalization.
- Added five repository regressions, four request/usage ViewModel regressions, and seven Compose interaction/accessibility regressions. Preserved final manager, merge, and archive evidence under `artifacts/full-product-audit/2026-09-02/tag-management/`.
- Compatibility: No Room schema, migration, backup format, Tag ID, entity ID, completion/progress fact, Gym record, or measurement history changed. Archive never rewrites item data; Rename/Merge intentionally updates the selected taxonomy label across saved item references.
- Important files: `MeasurementDao.kt`, `MeasurementRepository.kt`, `SettingsViewModel.kt`, `SettingsScreens.kt`, `TagManagementDialog.kt`, `TaskEditorRouteHost.kt`, `WhipApp.kt`, `MeasurementTaxonomyRepositoryTest.kt`, `TagMutationViewModelTest.kt`, `TagManagementUiTest.kt`, and `docs/testing.md`.
- Related: `FND-20260831-019`, `FND-20260901-027`, `FND-20260902-008`, `DEC-20260902-009`.
- Verification: `VER-20260902-010`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-011 — Exact, outcome-owned completed-Workout deletion

- Added transaction-derived `WorkoutDeletionImpact` and revision-checked deletion summaries. The reviewed snapshot includes session identity/lifecycle, placements, groups, all Sets, completed Sets, sourced personal records, preserved Training Max decisions, linked Goal contributions, automation occurrences, and affected Exercise identities.
- Active workouts are rejected. A changed session or history graph invalidates confirmation without mutation. The commit removes only the selected Workout graph; post-commit personal-record rebuilding and the retired-Link compatibility check use warnings/cancellation receipts so the UI never invites a second destructive attempt after the first commit. Preserved Goal contributions, generated Habit check-ins, and automation occurrences remain immutable audit evidence and are disclosed before confirmation.
- Added a request-owned ViewModel flow with exact target UUID validation, retained preview/error/missing state, rest-timer follow-up ownership, data-generation invalidation, and process-restored absent-target reconciliation.
- Replaced the optimistic generic confirmation with a scrollable, accessible review that distinctly names Removed, Recalculated, and Kept data; discloses immutable 5/3/1 Training Max audit history; blocks active sessions; disables stale confirmation; and offers read-only re-review. Extracted the three Gym deletion review hosts to keep the main Compose method instrumentable.
- Added four coordinator regressions, two real-application ViewModel lifecycle regressions, and three Compose interaction/accessibility regressions. A final-source 1080×2400 walkthrough created an Exercise and Workout, finished it, opened History, reviewed the exact impact, committed once, and verified the empty History/success outcome. Artifacts are under `artifacts/full-product-audit/2026-09-02/workout-deletion/`.
- Compatibility: No Room schema, migration, backup format, Exercise/Routine definition, unrelated completed Workout, or historical Training Max decision changed. The only irreversible data removal is the selected completed Workout and its owned placements/groups/Sets.
- Important files: `DomainDeletionCoordinator.kt`, `GymViewModel.kt`, `GymScreens.kt`, `DomainDeletionCoordinatorTest.kt`, `GymDeletionViewModelIntegrationTest.kt`, `WorkoutDeletionUiTest.kt`, `docs/testing.md`, and the final visual artifacts.
- Related: `FND-20260831-019`, `FND-20260901-025`, `FND-20260902-009`, `DEC-20260902-010`.
- Verification: `VER-20260902-011`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-012 — Exact, recoverable Machine-profile deletion

- Added `GymDeletionKind.Machine` to the shared request-owned deletion lifecycle, with a saveable candidate ID/UUID/data generation, explicit preparing/ready/error/missing states, transaction-derived impact/revision, exact commit admission, and one consumable terminal receipt.
- Added process-restored recovery against fresh repository truth. An absent exact target settles as achieved exactly once; a present or unverified target stays actionable and never masquerades as success. UUID mismatch is rejected before preview or commit.
- Reworked the Machine confirmation into a scrollable responsive review with explicit impact, error, missing-target, retry, and fixed-footer actions. At 320dp and 200% text the content scrolls while Cancel/Delete remain reachable.
- Preserved existing domain behavior: active-workout use blocks deletion, affected routines are marked `Needs equipment`, and completed-workout Machine snapshots remain immutable historical evidence.
- Added three real-application ViewModel regressions, one new 320dp/200%-text Compose recovery regression, and strengthened the existing destructive Machine UI test. Final visual evidence is retained under `artifacts/full-product-audit/2026-09-02/machine-deletion/`.
- Compatibility: No Room schema, migration, backup format, Machine identifier outside the reviewed target, Routine identity, completed Set, or historical workout snapshot changed. No physical-device release occurred.
- Important files: `GymViewModel.kt`, `GymScreens.kt`, `GymDeletionViewModelIntegrationTest.kt`, `GymPowerInputUiTest.kt`, `docs/testing.md`, and the final visual artifacts.
- Related: `FND-20260902-010`, `DEC-20260902-011`.
- Verification: `VER-20260902-012`.
- Status: Implemented and fully verified; physical release remains deferred at user-directed mission closeout.

### IMP-20260902-013 — VERA-Codex global and Whip routing installation

- Installed the five provided Luna/Terra/Sol role definitions, bounded routing policy, Terra/medium primary default, Luna default subagent, three-agent cap, and interrupt behavior in the Windows Codex home at `/mnt/c/Users/commv/.codex`.
- Merged VERA guidance into the existing global `AGENTS.md` while preserving the ATIS-specific policy and every unrelated global configuration section.
- Added the matching repository-owned `.codex/config.toml`, five `.codex/agents/*.toml` definitions, `routing-policy.yaml`, and a concise Whip policy section. The Whip section explicitly preserves `FB-20260902-001`: no agents or recursive review unless the user requests them.
- Compatibility: Configuration/instructions only. No Whip application code, schema, data, build output, device state, or release artifact changed.
- Related: `FB-20260902-002`, `DEC-20260902-012`.
- Verification: `VER-20260902-013`.
- Status: Superseded by the canonical clean-slate installation in `IMP-20260902-014`.

### IMP-20260902-014 — Canonical VERA repository and clean orchestration installation

- Created `/root/repos/vera-codex` directly from the supplied archive, normalized only filesystem permissions, confirmed all nine tracked file contents match the archive, committed it as `2c763f0`, created private GitHub repository `commvnist/vera-codex`, pushed `main`, and verified the remote ref.
- Replaced global and Whip orchestration instructions with the canonical VERA `AGENTS.md`; restored the canonical multiline `routing-policy.yaml`; retained the archive-exact project `.codex/config.toml` and five archive-exact custom role files.
- Removed global `atis-fast-explorer.toml`, the ATIS instruction section, the Whip-specific no-unrequested-subagents adaptation, and the previous condensed VERA wording. The global VERA values remain embedded alongside unrelated existing machine-local integrations, which were deliberately preserved.
- Compatibility: Configuration/instructions only. No Whip application code, schema, user data, tests, release artifacts, or device state changed.
- Related: `FB-20260902-004`, `DEC-20260902-013`.
- Verification: `VER-20260902-015`.
- Status: Implemented and verified.

### IMP-20260902-015 — Prepare Whip 0.3.35 for physical-device release

- Advanced the Android release identity from `0.3.34`/40 to `0.3.35`/41 so the current source is distinguishable from the previously installed physical release.
- Kept application ID `commvne.com.whip.app`, release signing configuration, Room schema, backup format, and user-data semantics unchanged.
- Built the signed release APK/AAB after the complete deterministic gate, paired and connected the exact requested physical phone, and upgraded it in place with `adb install -r`. The app was cold-launched without clearing data or running physical-device instrumentation.
- Important file: `app/build.gradle.kts`.
- Related: `FB-20260902-005`, `DEC-20260902-014`.
- Verification: `VER-20260902-016`.
- Status: Released and verified on the physical phone.

### IMP-20260902-016 — First-class advanced 5/3/1 program expansion

- Added two one-tap, editable 11-phase structures: BBB Leaders → FSL Anchor and FSL Leaders → FSL Anchor. Both support the user's own ordered Weight + Reps lifts, use 5s PRO during two Leader cycles, insert a 7th Week transition, use Classic PR-set Main work plus FSL in the Anchor, and expose Training Max boundaries after Leader 1 and both 7th Week phases.
- Added one-tap Deload, Training Max Test, and PR Test prescriptions with every percentage and rep range visible before build and independently applicable to an existing program phase. Public sources do not expose every book prescription, so the UI truthfully calls these book-guided editable structures and asks the lifter to verify the edition/template they follow.
- Added a dedicated Supplemental placement for alternate-lift BBB. The selected alternate uses its own Training Max and follows Main in the workout; inactive Anchor/test placements are omitted instead of producing empty exercise cards. Cycle changes synchronize every programmed Main/Supplemental placement for the lift while eligibility remains Main-only.
- Expanded Jokers from one candidate to an ordered one-to-three-set ladder at +5% or +10% TM steps. Each next Joker is offered only after successful Main/previous-Joker targets; a skip, failure, under-target result, RPE 9+, or RIR 1 or lower ends the ladder. Jokers remain Optional, individually logged, and additive before Supplemental work. Legacy arbitrary single-Joker routines remain valid.
- Added transparent automatic Push/Pull/Single-leg-Core assistance drafts from compatible active rep-based Library exercises: 3×10 per category for standard plans and 5×10 for Beginners. Suggestions are deterministic, never create exercises silently, never silently demote an unselected canonical main lift, and remain visible, replaceable, or omittable before save.
- Reworked setup into numbered, scrollable, full-width sections for preset, schedule/lifts, programming, optional work, assistance, and review. Added a phase timeline, policy explanations, explicit optional-state language, Supplemental labels during workouts, 48dp targets, and verified navigation at 320dp/200% text.
- Closed the final acceptance gaps: repeated Squat/Bench days in the Beginners layout keep synchronized editable protocol templates while a deterministic balanced runtime owner assigns Squat to Monday, Deadlift + Press to Wednesday, and Bench to Friday. Each logical lift therefore executes one complete Deload/TM Test/PR Test without invalid saves, TM-edit drift, duplicate tests, or an empty training day. Program Structure recognizes and names alternate-lift BBB, preserves it through Main/Joker edits, and removes it only when the lifter deliberately chooses another Supplemental scheme. Existing-program protocol controls now show their full matrix before the one-tap change, and assistance copy uses human-readable category names.
- Compatibility: No Room schema, migration, or backup-format change. Existing routines are not regenerated; completed workouts retain their saved prescriptions and outcomes. Once-per-lift runtime protocol ownership requires both a phase-specific role and 5/3/1 template revision 2, while legacy repeated exposures execute unchanged. Explicitly applying a new protocol upgrades durable routine provenance but marks only the selected phase for once-per-lift ownership. New advanced programs author their protocol phases directly with that phase-level provenance.
- Important files: `FiveThreeOneProgramming.kt`, `RoutineBuilder.kt`, `GymScreens.kt`, `RoutineRepository.kt`, `GymRepository.kt`, `GymModels.kt`, `RoutineBuilderStateTest.kt`, `GymUxRulesTest.kt`, `RoutineRepositoryTest.kt`, and `RoutineBuilderUiTest.kt`.
- Related: `FB-20260902-006`, `DEC-20260902-015`.
- Verification: `VER-20260902-017`.
- Status: Implemented and fully verified after final-review remediation; not yet released to a physical phone.

### IMP-20260902-017 — Targeted subsystem QA runner

- Added `scripts/qa-targeted` as the normal development-loop entry point. Named profiles cover the app shell, Tasks, Habits, Goals, Tracks, Gym, 5/3/1, and Settings; each runs its bounded JVM suites and compiles Android tests.
- Added exact repeatable `--jvm` patterns and `--android Class#method` selectors. Android execution retains the emulator-only guard and refuses physical devices; `--repeat` supports isolated timing-sensitive verification.
- Kept `scripts/check --emulator` and `scripts/check --full` unchanged as the complete acceptance/release authorities. Targeted results are explicitly described as chunk evidence, not a whole-product release claim.
- Related: `FB-20260902-009`, `docs/quality/UI_UX_REMEDIATION_PLAN.md`.
- Verification: `VER-20260902-018`.
- Status: Implemented and targeted-tool verified.

### IMP-20260902-018 — Exact-signature instrumentation batch resume

- Extended the complete emulator gate with a local successful-batch cache keyed by production source, build configuration, the emulator image, shared Android-test support, the exact requested class set, and the source files for those classes.
- A production-source change invalidates every device batch. A correction confined to one test batch reruns that batch while unchanged successful batches retain their exact executed-class, test-count, and zero-skip evidence.
- Added `--fresh-emulator` as an explicit cache bypass. JVM tests, Android-test compilation, lint, coverage, artifact checks, complete source-test accounting, and zero-skip enforcement always run and cannot be satisfied by the cache.
- Related: `FB-20260902-009`, `docs/quality/UI_UX_REMEDIATION_PLAN.md`.
- Verification: `VER-20260902-019`.
- Status: Implemented and structurally verified; its first frozen-candidate emulator run remains the integration gate.

### IMP-20260902-019 — Consistent adaptive editor chrome

- Added `WhipEditorHeader`, a shared full-width editor header with a 48dp identity row, heading semantics, standard divider/spacing, and automatic action stacking for narrow or enlarged-text layouts.
- Adopted it in shared Productivity editors, Task, Track, Track Entry, and Routine Builder. Routine outline now has one unambiguous X exit; nested builder pages use Back to the outline.
- Standardized primary Gym editor Save actions as filled buttons and their dismiss actions as accessible 48dp icon targets for Machine, Exercise, tracked-record, and Set editing.
- Added source architecture contracts for adoption, primary action hierarchy, single-section inspector suppression, and shared inspector navigation; added a focused 320dp/200%-text Compose regression.
- Related: `FND-20260902-011`, `DEC-20260902-016`.
- Verification: `VER-20260902-020`.
- Status: Implemented and targeted-test verified.

### IMP-20260902-020 — Canonical schema-42 data epoch and clean-slate reset

- Raised Whip to `0.3.36`/version code 42 and made Room schema 42 the sole exported schema. Removed migrations, historical schema exports, migration-only sources/tests, compatibility settings initialization, old entity-tag storage, and ignored pre-epoch backup-upgrade tests.
- Reduced portable backups to exact envelope 3, data epoch 2, data version 19, and the canonical current table set. Older, newer, malformed, or differently shaped backups fail before mutation; current replace/merge/private rollback safety remains.
- Added a pre-Room `DataEpochGate` with durable Current/ResetRequired/ResetInProgress states and a repository-independent `LocalDataResetter`. Reset cancels work and notifications, releases Whip's portable-folder grant, deletes Whip database/files/preferences, preserves non-Whip preferences and external documents, rebuilds schema/defaults, creates a fresh generation, and locks widgets until success.
- Added a dedicated accessible fresh-start flow with two explicit destructive actions, safe close, non-interactive progress, distinct pre-confirmation check failure, and confirmed-reset retry. Serialized reset retries, discarded all epoch-gated launch requests, and kept workers/widgets behind the Ready gate.
- Isolated the destructive integration test as the last standalone instrumentation batch so its intended runtime teardown cannot contaminate neighboring tests.
- Important files: `DataEpochGate.kt`, `LocalDataResetter.kt`, `WhipApplication.kt`, `MainActivity.kt`, `StartupRecoveryScreen.kt`, `WhipDatabase.kt`, `BackupRepository.kt`, `scripts/check`, schema 42, and focused JVM/Android regressions.
- Related: `FND-20260902-012`, `DEC-20260902-017`.
- Verification: `VER-20260902-021`.
- Status: Implemented and targeted/emulator verified; not released to a physical phone.

### IMP-20260902-021 — Explicit interface labels and shared section hierarchy

- Added deliberate labels for theme/Home/Review/Health choices, Task priorities, Goal states/consistency periods, measurement dimensions, Habit destinations, workout state/group types, 1RM formulas, and Gym destinations.
- Replaced visible enum-name rendering in onboarding, Review, Settings, Task filters/editors, Goal flows/search, Habit navigation, Gym navigation/history/grouping, and unified search. An active workout now reads “In progress”; Habit “All” now reads “All Habits”.
- Replaced hand-built Settings and major Gym section headings with `EditorSectionHeader`; changed Task/Habit/Settings weekday labels to locale-aware display names.
- Added architecture regressions that reject direct internal-name rendering for these audited surfaces and require the shared heading contract.
- Related: `FND-20260902-013`, `DEC-20260902-018`.
- Verification: `VER-20260902-022`.
- Status: Implemented and targeted-test verified; not released to a physical phone.

### IMP-20260902-022 — One-pass bounded historical search selection

- Replaced full-history sorting with a stable bounded priority queue. Search now evaluates each timestamp/rank selector once and retains only the requested newest 100/500 values before final ordering.
- Preserved independent per-domain limits and incomplete-source disclosure so a large Task, Habit, Track, or Gym history cannot consume another domain's result budget.
- Added deterministic 10,000-value and dual-domain 20,000-result regressions and reconciled the prior section/internal-label/search audit statuses.
- Important files: `UnifiedSearchDialog.kt`, `UnifiedSearchRulesTest.kt`, and `TOP_DOWN_UX_UI_FUNCTIONAL_QA_AUDIT_2026-08-27.md`.
- Related: `FND-20260902-014`, `DEC-20260902-019`.
- Verification: `VER-20260902-023`.
- Status: Implemented and targeted-test verified.
