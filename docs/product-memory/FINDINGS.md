# Durable findings

### FND-20260831-001 — Configurable behavior lacked its prerequisite control

- Severity/category: P0 control integrity and domain correctness.
- Observed: Ordinary routines could select `% Training Max` while the required Training Max was hidden or unconfigurable.
- Expected: Every exposed mode has a discoverable, valid path to configure its prerequisites before Save.
- Why it matters: General strength users could create invalid or misleading prescriptions.
- Evidence: `RoutineBuilder.kt`; `RoutineRepository.kt`; detailed evidence in `../GYM_531_PRODUCT_AUDIT_2026-08-31.md`.
- Root cause: UI capability and persisted domain requirements evolved separately.
- Resolution: Place Training Max controls before dependent sets, support explicit/derived sources, validate missing estimates, and replace stale child data transactionally.
- Status: Verified and released in 0.3.34.

### FND-20260831-002 — Adaptive layout and navigation lost routine context

- Severity/category: P1 mobile UX and navigation.
- Observed: A fixed nested exercise viewport was too small; exercise Save could return to the library root; an early remedy expanded routine content unexpectedly.
- Expected: Editors use available screen/pane space and selection returns to the initiating routine/day.
- Why it matters: Mobile and foldable users could see one exercise at a time and repeatedly reconstruct context.
- Evidence: `WhipApp.kt`, `RoutineBuilder.kt`, `RoutineBuilderUiTest.kt`; screenshots linked by the detailed Gym audit.
- Root cause: The support pane was treated as a generic fixed container and picker navigation did not preserve an explicit return target.
- Status: Verified and released in 0.3.34; subjective device validation remains ongoing.

### FND-20260831-003 — Optional-policy toggles regenerated unrelated prescriptions

- Severity/category: P1 authored-data preservation.
- Observed: Toggling Joker policy could regenerate Main/Supplemental work and overwrite custom details; Joker could appear to compete with ending work.
- Expected: Joker is additive Optional work and cannot mutate BBB/FSL/SSL/BBS/custom Supplemental work.
- Evidence: `RoutineBuilderState.setFiveThreeOneJokerEnabled`; `RoutineBuilderStateTest` additive-preservation regressions.
- Root cause: A broad phase-regeneration path was used for a narrow optional-feature change.
- Status: Verified and released in 0.3.34.

### FND-20260831-004 — Rest-timer display could exceed selected duration

- Severity/category: P1 in-gym trust and boundary correctness.
- Observed: A fresh five-minute deadline could render 5:01 before reaching 5:00.
- Expected: Selected duration is an authoritative upper display bound; first elapsed second is 4:59.
- Evidence: Timer calculation tests and the adaptive training section of `../GYM_531_PRODUCT_AUDIT_2026-08-31.md`.
- Root cause: Ceiling deadline arithmetic observed a newer persisted deadline with a stale UI clock tick.
- Status: Verified and released in 0.3.34.

### FND-20260831-005 — Active-workout customization could mutate program meaning

- Severity/category: P1 history and progression integrity.
- Observed: Ad-hoc set/exercise additions risked becoming planned Main work or altering the source routine.
- Expected: Workout-only work is explicit Optional/ad-hoc content, persists with the session and history, and leaves future routine prescriptions unchanged.
- Evidence: `GymRepository`, `WhipApp.kt`, `FirstClassWorkflowE2ETest.kt`, repository regressions.
- Root cause: Template prescriptions and session-instance customization were insufficiently separated.
- Status: Verified and released in 0.3.34.

### FND-20260831-006 — Product knowledge was fragmented across chats and dated audits

- Severity/category: P1 process reliability and cross-session continuity.
- Observed: Many useful audit reports existed, but there was no canonical current index, stable issue linkage, or required writeback protocol.
- Expected: Every substantial task begins from and ends by updating concise repository-backed memory, while code remains authoritative.
- Why it matters: Future agents could repeat investigations, forget user corrections, overclaim verification, or regress accepted decisions.
- Root cause: Documentation was report-oriented rather than lifecycle-oriented.
- Resolution: Personal `maintain-whip-memory` skill, workspace instructions, canonical ledgers, stable IDs, and a goal with mandatory memory checkpoints.
- Status: Implemented; effectiveness must be validated through future tasks.

### FND-20260831-007 — Failed restore recovery allowed normal startup to continue

- Severity/category: P0 data integrity and recovery.
- Observed: `WhipApplication.onCreate` discarded `recoverIfNeeded` failure, then initialized normal repositories, a default Area, scheduling, Health work, and widgets.
- Expected: An unresolved recovery marker blocks every normal write-capable path until Retry succeeds.
- Why it matters: New work can compound mixed state or later disappear when rollback succeeds.
- Evidence: `WhipApplication.kt`, `RestoreRecoveryManager.kt`, and `../WHOLE_PRODUCT_MAXIMUM_QUALITY_AUDIT_2026-08-31.md`.
- Root cause: Atomic restore recovery was treated as best-effort startup housekeeping, and write-capable workers, receivers, widgets, schedulers, Activity-scoped drafts, and live restore were not governed by one application-wide maintenance boundary.
- Resolution: A counted application data-access barrier now denies late work and drains admitted work before replace restore. Pending recovery, live restore, rollback, runtime rebuilding, WorkManager startup, workers, receivers, widgets, schedulers, ViewModels, SavedState drafts/imports, notification actions, and widget entity references share fail-closed generation-aware rules. Recovery failure presents an accessible blocking screen with Retry and preserves the recovery marker; historical databases and completed records are not recomputed.
- Status: Resolved and verified on the disposable emulator; see `IMP-20260831-004` and `VER-20260831-006`.

### FND-20260831-008 — Reminder delivery trusts stale queued work

- Severity/category: P1 correctness and user trust.
- Observed: Task, Habit, and Goal workers do not all re-resolve the exact current occurrence, configuration, schedule version, and pause/skip/completion state immediately before posting.
- Expected: Missing or stale delivery targets fail closed; valid current targets still notify and schedule the next occurrence.
- Why it matters: Obsolete prompts teach reminder-dependent and ADHD users to ignore Whip.
- Affected users: recurring Task users, every Habit/Goal reminder user, source-backed Habit users, travelers, users editing near delivery, and users acting from the notification shade.
- Root cause: queued WorkManager inputs were treated as authority; delivery/action rules were split across schedulers and presentation paths; cancellation was not awaited; mutable source/history/settings state lacked one final linearization boundary; and deletion crossed Room/NotificationManager without durable cleanup intent.
- Resolution: Every Task/Habit/Goal request now carries a versioned exact claim containing stable identity, logical day, trigger, kind, and deterministic definition fingerprint. Workers and actions resolve live domain state immediately before posting/mutation, fail closed on malformed or stale input, distinguish scheduled/snoozed/optional work, and reconcile current future work. A non-reentrant state boundary linearizes production repository commits with resolve/post while explicit raw composition and entity→state lock order avoid Room/deadlock traps. Quiet/time-zone writes and full-snapshot settings writes are serialized; source-backed Habit invalidation is bounded; legacy queues are rebuilt once; system time broadcasts reconcile the relevant schedules; and a durable deletion-cleanup journal bridges Room commit with visible-notification/work cleanup across rollback or process death.
- Compatibility: No Room schema or backup-format change. Historical occurrences, completed records, saved logical dates, custom units, and restored data remain unchanged; legacy unverifiable work is canceled/rebuilt rather than accepted.
- Evidence: `ReminderDeliveryClaims.kt`, `CoordinatedReminderRepositories.kt`, `ReminderDeletionCleanupStore.kt`, `ReminderWorker.kt`, `ReminderScheduler.kt`, `HabitReminderScheduler.kt`, `GoalReminderScheduler.kt`, `ReminderActionReceiver.kt`, `ReminderRuntimeMaintenance.kt`, `ReminderTimeChangeReceiver.kt`, `WhipApplication.kt`, and focused JVM/Android regression suites.
- Status: Resolved and verified on the disposable emulator; see `IMP-20260831-005` and `VER-20260831-007`.

### FND-20260831-009 — Time and logical-date semantics diverge from configured Whip time

- Severity/category: P1 date/time correctness.
- Observed: Follow-device zone changes do not reschedule work; Tracks can retain yesterday without a repository emission; Search and elapsed Goal detail use the system zone directly.
- Expected: One explicit active zone/current-date flow controls live behavior while saved historical dates remain immutable.
- Evidence: `WhipApplication.kt`, `AppSettings.kt`, `TrackViewModel.kt`, `UnifiedSearchDialog.kt`, `GymRepository.kt`, `GymScreens.kt`, `GoalViewModel.kt`, `GoalScreens.kt`, `ReminderTimeChangeReceiver.kt`, and focused JVM/Android regressions.
- Resolution: `DATE_CHANGED`, `TIME_SET`, and `TIMEZONE_CHANGED` enter one serialized receiver path; one application-scoped calendar context now carries active zone, physical date, cutoff-adjusted logical date, cutoff, and follow-device policy. Task, Habit, Goal, Track, Search, Gym, and widgets consume it; the root holds the last coherent cross-domain snapshot until every projection catches up. Search completion dates use the Whip zone; Track rolls without repository writes; new/default/copied Gym and Measurement records snapshot explicit current provenance while historical instants/dates remain unchanged. Every elapsed-Goal surface receives the same live clock and Whip zone. Editors/reset dialogs freeze their opening zone and canonical instant, reject nonexistent DST times, require an explicit occurrence for overlaps, and preserve seconds/milliseconds until wall time is deliberately edited.
- Status: Resolved, fully regression-tested, and independently challenged in `IMP-20260831-006`, `IMP-20260831-007`, and `VER-20260831-009`; integrated physical-device release remains deferred with the larger goal.

### FND-20260831-010 — Productivity saves can change Area scope before persistence succeeds

- Severity/category: P1 navigation and state integrity.
- Observed: Task, Habit, and Goal save paths reconcile Area visibility before asynchronous Save confirms success.
- Expected: Scope and success notices change only in the confirmed-success branch; failure retains draft and context.
- Why it matters: A failed or interrupted write could look successful, move the user away from their working context, lose a deep draft, or invite a duplicate retry after the entity had already committed.
- Affected users: Task, Habit, and Goal authors; filtered-Area users; keyboard and one-handed users; users on unreliable storage or during process/activity lifecycle changes.
- Root cause: Navigation, global operation feedback, repository commit, reminder/tag follow-up, and editor dismissal were connected by callbacks without a typed request-owned commit boundary.
- Resolution: Authored definition editors now own UUID-scoped outcomes. They block Back, pointer, and hardware-key editing while saving; retain draft, scroll, and Area on retryable failure; and reconcile Area/dismissal only from their exact confirmed receipt. Repository commit is an explicit point of no return, so ordinary reread/tag/reminder failures become warnings rather than duplicate-producing failures. Receipts re-read authoritative Area, fall back to All Areas if it cannot be verified, atomically reject concurrent or unconsumed requests, reclaim stale terminal delivery, and preserve fatal-error/cancellation semantics.
- Evidence: `AppRuntime.kt`, `AreaScope.kt`, `ProductivityEditorComponents.kt`, `TaskViewModel.kt`, `HabitViewModel.kt`, `GoalViewModel.kt`, `WhipApp.kt`, `TaskEditorDialog.kt`, `HabitScreens.kt`, `GoalScreens.kt`, `EntitySaveCoordinatorUiTest.kt`, and `EntitySaveViewModelIntegrationTest.kt`.
- Status: Resolved for Task/Habit/Goal authored definition editors and verified on the disposable emulator; see `IMP-20260831-008` and `VER-20260831-010`. Adjacent secondary mutation dialogs remain `FND-20260831-019`.

### FND-20260831-011 — Large text removes every visible primary navigation label

- Severity/category: P1 accessibility and navigation.
- Observed: At font scale 1.5 the compact phone navigation becomes six icon-only destinations; screenshots are in `artifacts/full-product-audit/2026-08-31/baseline/`.
- Expected: A measured named row, stable two-row named layout, or labeled drawer preserves visible destination names.
- Affected users: Sighted low-vision, cognitive/ADHD, first-time, and narrow-landscape users.
- Evidence: `WhipApp.kt`, `large-text-home.png`, `large-text-gym.png`.
- Status: Confirmed; remediation pending.

### FND-20260831-012 — Basic Habit creation exposes advanced schedule machinery

- Severity/category: P1 creation UX and executive-function accessibility.
- Observed: Reminder overrides, end conditions, and week-boundary configuration remain in the ordinary path even when advanced controls are meant to stay folded.
- Expected: A visible reminder summary and progressively disclosed advanced configuration, auto-expanded for existing data, power mode, or errors.
- Evidence: `HabitScreens.kt`, baseline Habit editor capture, specialist workflow review.
- Status: Confirmed; remediation pending.

### FND-20260831-013 — Settings can persist intermediate numeric and time edits

- Severity/category: P1 configuration correctness.
- Observed: Parseable keystrokes commit immediately, so replacing `300` with `600` can leave `6` or `60`; clock parsing accepts extra segments.
- Expected: Local drafts, strict whole-field validation, and explicit IME/focus commit.
- Evidence: `SettingsScreens.kt` numeric/time setting controls.
- Status: Confirmed; remediation pending.

### FND-20260831-014 — External shared text is unbounded across Activity recreation

- Severity/category: P1 stability and input safety.
- Observed: Arbitrary shared text is retained in Activity state and every additional line may become a subtask.
- Expected: Intent-boundary title/notes/subtask budgets with explicit truncation or refusal feedback.
- Evidence: `MainActivity.kt`, `TaskEditorDialog.kt`.
- Status: Confirmed; remediation pending.

### FND-20260831-015 — Legacy public Goal completion history is omitted from backup

- Severity/category: P1 backup and historical integrity.
- Observed: `goal_completion_snapshots` survives database upgrades but is absent from backup; replace restore deletes it.
- Expected: Meaningful completion history is preserved directly or migrated into the current historical model.
- Evidence: `GoalEntities.kt`, `BackupRepository.kt`, backup tests.
- Resolution: Portable-backup format 16 exports and merges closure snapshots with stable UUIDs and specialized elapsed/milestone outcomes. Pre-v16 imports synthesize deterministic UUIDs; repeated merge is idempotent, and replace restore preserves meaningful completion/abandonment history.
- Status: Resolved and fully verified in `IMP-20260831-011` / `VER-20260831-013`.

### FND-20260831-016 — Abandoned Goals are labeled Completed

- Severity/category: P2 information architecture and historical trust.
- Observed: Completed and Abandoned Goals share the destination labeled Completed.
- Expected: History/Closed organization with truthful Completed and Abandoned distinctions.
- Evidence: `GoalViewModel.kt`, `GoalScreens.kt`.
- Resolution: The shared destination is now labeled History, individual rows and lifecycle snapshots retain truthful Completed versus Abandoned outcomes, and archive remains a separate organization control rather than a false lifecycle state.
- Status: Resolved and fully verified in `IMP-20260831-011` / `VER-20260831-013`.

### FND-20260831-017 — Returning users lack a context-recovery path on clear Home

- Severity/category: P2 ADHD and returning-user UX.
- Observed: Existing Inbox, upcoming, paused, recent, or unpinned content can still yield only “Your Day Is Clear.”
- Expected: A compact, nonjudgmental “Pick up where you left off” path distinct from first-run guidance.
- Evidence: `WhipApp.kt` Home clear state.
- Status: Confirmed; remediation pending.

### FND-20260831-018 — Track list query behavior is unreachable

- Severity/category: P2 discoverability and dead state.
- Observed: Track list query/no-match/reorder logic exists but `AllTracksPage` exposes no setter.
- Expected: Purposeful local search or removal of the unreachable branch in favor of explicit global Search routing.
- Evidence: `TrackScreens.kt`.
- Status: Confirmed; product decision pending.

### FND-20260831-019 — Secondary mutation dialogs still dismiss optimistically

- Severity/category: P1 authored-state and interaction integrity.
- Observed: Several draft-bearing or destructive secondary flows still close immediately after dispatching asynchronous work, including Task rescheduling, Habit logging/history/pause actions, Goal measurement/reset actions, and Track/Gym secondary editors or pickers.
- Expected: Any flow whose failure would lose authored input, lie about completion, or make retry ambiguous should use an outcome-aware boundary proportionate to its risk; historical mutations must remain snapshots of what actually happened.
- Why it matters: The definition-editor fix proves the former callback pattern is unsafe, but applying one giant generic coordinator without inspecting each workflow could also add needless friction to quick gym and productivity actions.
- Affected users: Mobile/in-gym users, ADHD users, keyboard users, filtered-Area users, and anyone editing under unreliable storage or lifecycle interruption.
- Evidence: Dispatch-and-dismiss call sites in `WhipApp.kt`, `HabitScreens.kt`, `GoalScreens.kt`, `TrackScreens.kt`, `GymScreens.kt`, and related ViewModels/repositories.
- Recommended solution: Audit each mutation by reversibility, draft cost, historical impact, and latency; reuse the typed request/receipt primitives for authored data, use idempotent optimistic UI only where rollback is explicit and proven, and add focused lifecycle/failure/accessibility regressions before resolving each family.
- Habit sub-resolution: Habit current-total entry, historical log creation/edit/deletion, scheduled-pause creation/edit/deletion, and historical skip undo now use typed request-owned outcomes. Transactional parent checks prevent cross-Habit mutation; exact skip deletion rejects repeated/missing targets; removed log/pause targets remain restorable snapshots until terminal delivery; Home and workspace namespaces cannot steal outcomes; data-generation changes invalidate saved numeric identities; measurement provenance and custom-unit conversions remain authoritative; and binary-noise no-ops use a bounded ULP comparison that preserves real large-value changes. The UI says “Set Today's/This Week's/This Month's Total” and explains that Save sets rather than adds.
- Habit evidence: `HabitRepository.kt`, `HabitDao.kt`, `MeasurementRepository.kt`, `HabitViewModel.kt`, `HabitScreens.kt`, `EntityInspector.kt`, `ProductivityEditorComponents.kt`, `WhipApp.kt`, `HabitRepositoryTest.kt`, `ActivityHistoryUiTest.kt`, `EntitySaveCoordinatorUiTest.kt`, `EntitySaveViewModelIntegrationTest.kt`, and `HabitMutationCommitTest.kt`.
- Task sub-resolution: Task create/edit, occurrence-aware series editing, reschedule/Plan My Day undo, bulk metadata/date/archive, pin, complete/reopen/reset, permanent deletion, reminder notification actions, and reminder reconciliation now use exact preconditions or request-owned terminal receipts proportionate to risk. A saveable semantic edit boundary survives activity/process recreation and rejects stale task, occurrence, or Subtask state. “Edit This and Future” preserves closed history, remaps retained Subtasks and integrations by stable ID, migrates compatible Open occurrence/step state, preserves finite-count remainder and Carry Unfinished baselines, and rejects conflicting closed future history. Explicit Open rows remain visible and remindable even after cadence or anchor changes. Completed/skipped/archived occurrences use “Edit Series,” avoiding duplicate split boundaries. Deletion revisions cover dependent integrations and Track history; committed deletion cancellation/warnings never invite a destructive retry. Notification action ledgers distinguish pre-authoritative failure from committed follow-up failure.
- Task evidence: `TaskModels.kt`, `TaskDao.kt`, `TaskRepository.kt`, `TaskDeletionCoordinator.kt`, `TrackDao.kt`, `ReminderActionReceiver.kt`, `ReminderScheduler.kt`, `ReminderWorker.kt`, `CoordinatedReminderRepositories.kt`, `TaskViewModel.kt`, `TaskComponents.kt`, `TaskEditorDialog.kt`, `WhipApp.kt`, focused JVM tests, and the Task/reminder/UI Android regression suites.
- Goal sub-resolution: Goal progress create/edit/delete, elapsed reset, definition save/duplicate, lifecycle/archive, and permanent deletion now use exact semantic boundaries or request-owned outcomes proportionate to risk. Drafts and errors survive failure/recreation, modal input is blocked only while saving, Home/workspace result namespaces cannot steal one another, outside-window records require explicit History-only confirmation, and future progress is rejected. Closed or archived Goal-owned history remains correctable while immutable closure snapshots keep the actual terminal outcome. Pin/milestone quick actions remain lightweight but exact. Deletion revisions cover Goal-owned measurements, milestones, closures, resets, and Link dependents; post-commit ordinary failures are warnings rather than false retries.
- Goal evidence: `GoalModels.kt`, `GoalEntities.kt`, `GoalDao.kt`, `GoalRepository.kt`, `DomainDeletionCoordinator.kt`, `AreaDeletionCoordinator.kt`, `GoalViewModel.kt`, `GoalScreens.kt`, `WhipApp.kt`, schema 38, backup format 16, and focused JVM/Android/migration/backup regressions.
- Gym destructive sub-resolution: Permanent Exercise and Routine deletion now freeze complete reviewed impacts, reject changed dependencies, block active-workout corruption, preserve immutable Training Max decisions and Routine-sourced workout history as explicitly stated, distinguish pre-commit failure from committed follow-up warnings, and retain exact request/recovery ownership across rotation and process replacement. Other Gym quick-set/session mutation families remain open for separate risk-proportionate review.
- Gym evidence: `DomainDeletionCoordinator.kt`, `GymDao.kt`, `RoutineDao.kt`, `GymViewModel.kt`, `GymScreens.kt`, `DomainDeletionCoordinatorTest.kt`, `GymDeletionViewModelIntegrationTest.kt`, and `GymPowerInputUiTest.kt`.
- Status: Partially resolved. Habit is verified in `IMP-20260831-009` / `VER-20260831-011`, Task in `IMP-20260831-010` / `VER-20260831-012`, Goal in `IMP-20260831-011` / `VER-20260831-013`, and the Gym permanent Exercise/Routine subset in `IMP-20260901-012` / `VER-20260901-014`. Track and remaining Gym secondary flows stay open.

### FND-20260831-020 — Health reconciliation is not batch-atomic

- Severity/category: P2 external-data consistency and recovery.
- Observed: Health reconciliation upserts each provider row in a separate repository transaction and prunes absent rows afterward. An exception midway can leave an imported prefix partially refreshed and skip authoritative pruning.
- Expected: One source-window reconciliation either commits its validated upserts and prefix-scoped pruning together or retains the prior local mirror with a retryable failure.
- Why it matters: A provider/API/unit/collision failure during a multi-record sync can temporarily mix old and new mirror state even though deterministic row identity prevents duplication.
- Evidence: `HealthConnectManager.reconcileHealthRecords`, `RoomMeasurementRepository.record`, `deleteSourceEntriesExcept`, and the three-agent Measurement regression audit summarized in `VER-20260831-013`.
- Recommended solution: Introduce a narrow repository-level Health source-snapshot transaction after defining window/prefix ownership and collision behavior; do not generalize it into a cross-domain import DSL.
- Status: Confirmed during `VER-20260831-013`; remediation pending in a separate coherent chunk.

### FND-20260901-021 — Gym permanent deletion could corrupt an active 5/3/1 outcome

- Severity/category: P0 program correctness, historical integrity, and destructive-action safety.
- Observed: Permanently deleting an Exercise used by the active workout removed its placement and prescribed/performed sets without passing through the required-main-work invalidation path. Later 5/3/1 progression could treat the now-missing main-work outcome as eligible and advance a Training Max. Permanently deleting the source Routine of an active workout detached the workout from its progression source.
- Expected: An active prescription is an authoritative in-progress record. Exercise or Routine deletion must be blocked while it could change that workout's main-work/progression meaning; every non-active dependency must be reviewed exactly and committed atomically.
- Why it matters: A library cleanup action could silently turn an incomplete or failed 5/3/1 workout into a Training Max increase, erase live set data, or suppress the expected cycle decision.
- Affected users: All in-gym users, especially 5/3/1 lifters using PR sets, supplemental work, Joker sets, and performance-informed cycle review.
- Evidence: `DomainDeletionCoordinator.kt`, `GymDao.kt`, `RoutineDao.kt`, `GymViewModel.kt`, `GymScreens.kt`, `DomainDeletionCoordinatorTest.kt`, `GymDeletionViewModelIntegrationTest.kt`, and `GymPowerInputUiTest.kt`.
- Resolution: Exercise and Routine deletion now use exact SHA-256 impact revisions, active-workout guards, transactional count checks, immutable Training Max audit preservation, explicit completed/discarded workout-history handling, request-owned terminal receipts, post-commit warnings, and saved-state outcome verification. The confirmation surface lists removed, changed, and retained dependencies; offers a direct active-workout route; remains usable at 320dp/200% text; and announces asynchronous states.
- Status: Resolved, independently accepted, and fully verified in `IMP-20260901-012` / `VER-20260901-014`.

### FND-20260901-022 — Stale Track schema confirmation can erase unreviewed history

- Severity/category: P0 Track history and schema integrity.
- Observed: Track definition drafts approve field/choice removal by raw ID and current UI counts, but repository update does not validate an exact schema/dependency boundary. Values, fields, choices, or automation dependencies added after confirmation can be deleted by the stale Save without renewed review.
- Expected: Destructive schema mutation commits only against the exact Track definition, affected values, replacements, and dependent Link/Trigger state the user reviewed; any semantic change produces a conflict and retains the draft.
- Why it matters: A stale editor, concurrent surface, or import can cause irreversible Track-history deletion that the confirmation never disclosed.
- Affected users: Track power users, CSV importers, multi-surface users, and anyone customizing fields or choices.
- Evidence: `TrackRepository.kt`, `TrackScreens.kt`, `TrackEditorViewModels.kt`, and the independent Track secondary-mutation audit completed during `VER-20260901-014`.
- Resolution: Track definition saves now require a compact opening `TrackDefinitionBoundary` plus repository-originated exact removal review. One Room transaction revalidates authored definition semantics, affected value rows, replacement joins, and dormant Link/Trigger compatibility rows before any write. Typed conflicts retain the draft; removal-impact drift requires renewed review; permanent conflicts can copy the draft into a new Track without overwriting the concurrent original. Request ownership, generation-bound routes, explicit missing-target surfaces, large-text review reachability, and post-commit warning semantics prevent false retry, Edit-to-Create identity drift, and inaccessible destructive review.
- Status: Resolved, independently accepted, and fully verified in `IMP-20260901-013` / `VER-20260901-015`.

### FND-20260901-023 — Track Entry mutations can overwrite history or report the wrong outcome

- Severity/category: P0/P1 Track history integrity, lifecycle ownership, and accessibility.
- Observed: Entry update/delete accept only a numeric ID and the current draft, so a stale editor can overwrite a concurrent edit or delete a value added through a newer optional Field. New Entry UUIDs are allocated only inside each commit, while global `OperationStatus` admits rapid queued submissions and a retained callback can close a different editor session. Delete closes before persistence succeeds; failures are hidden behind the full-screen editor. Undo omits the TriggerOccurrence fulfillment link that deletion clears. Optional malformed Number text is represented as blank and can erase a saved value. Visible paged history invalidates only from count/max Entry timestamp, so same-millisecond value changes and Choice replacement can remain blank/stale.
- Expected: Create/update/delete use stable request/Entry identity, an exact opening form+Entry boundary, transaction-owned receipts, and route/session/generation-owned delivery. Stale conflicts retain the draft; create retry is idempotent; deletion is confirmed and closes only after success; exact Undo restores compatible provenance links or rolls back; every mutable control is shielded during persistence; invalid nonblank input is announced and blocks Save; value/Choice changes invalidate visible pages exactly.
- Why it matters: Two ordinary surfaces, a double tap, process interruption, or a concurrent Track tweak can duplicate facts, silently erase newer history, dismiss the wrong editor, or leave automation audit history contradictory.
- Affected users: All Track users, especially quick-log/mobile users, CSV/import users, users with large histories, assistive-technology users, and power users editing schema and Entries concurrently.
- Evidence: `TrackRepository.kt`, `TrackDao.kt`, `LinkEntities.kt`, `TrackViewModel.kt`, `TrackEditorViewModels.kt`, `TrackScreens.kt`, `WhipApp.kt`, and the three independent Track Entry audits performed after `VER-20260901-015`.
- Resolution: Entry creation now preallocates a stable UUID and returns an exact achieved/collision receipt; edits and deletion validate atomic opening form and Entry/value/provenance boundaries inside one Room transaction. Exact same-process Undo preserves UUIDs, timestamps, typed values, source occurrence, and every fulfilled occurrence link, rolling back on incompatibility. UI requests are route/session/data-generation owned, input-shielded, and draft retaining; delete waits for the exact result, malformed optional Number text cannot erase a value, historical pages invalidate from structural content, and visible active/archived units remain understandable. One-level Undo supersession is explicit and failed restore remains retryable.
- Status: Resolved, independently accepted, and fully verified in `IMP-20260901-014` / `VER-20260901-016`. CSV batch outcome idempotency remains separately tracked in `FND-20260901-024`.

### FND-20260901-024 — A committed CSV import can be repeated after process interruption

- Severity/category: P1 Track import idempotency and historical accuracy.
- Observed: CSV mapping/session state survives process recreation, but batch commit identity and outcome do not. If one atomic import commits and the process dies before its callback clears the session, retry can insert the same batch again with new Entry UUIDs—up to 5,000 duplicated facts.
- Expected: Each previewed batch has a durable stable identity/fingerprint and an exact receipt or verification gate. Retrying the same request returns the already-committed batch only when Track, mapping, payload, and generated identities match; intentional content duplicates remain allowed under a new batch identity.
- Why it matters: Transaction atomicity prevents partial rows but does not prevent a complete duplicate batch when UI delivery is interrupted.
- Affected users: Track CSV importers, especially large-history users and devices under memory pressure.
- Evidence: `TrackCsvImportSessionDescriptor`, `TrackViewModel.importEntries`, `RoomTrackRepository.importEntries`, and the independent Track Entry audits performed after `VER-20260901-015`.
- Recommended solution: Give the saved import session a stable batch/request ID, derive stable per-row Entry identities or persist a private receipt, verify achieved state before retry, and keep the exact mapping/payload fingerprint bounded. Do not deduplicate by visible content.
- Status: Confirmed; queued as the next Track import chunk after direct Entry mutation integrity.
