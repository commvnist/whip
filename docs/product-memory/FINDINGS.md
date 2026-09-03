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
- Resolution: Compact navigation now measures rendered labels and retains one named row when they fit, otherwise uses two stable named rows. The rail measures its label width, keeps names at every text scale, and scrolls as one direct list in short windows. See `IMP-20260902-006` / `VER-20260902-006`.
- Status: Resolved and verified at 150%, 200%, and 320% text, short rail height, RTL/theme/fold matrices, and full emulator/release gates.

### FND-20260831-012 — Basic Habit creation exposes advanced schedule machinery

- Severity/category: P1 creation UX and executive-function accessibility.
- Observed: Reminder overrides, end conditions, and week-boundary configuration remain in the ordinary path even when advanced controls are meant to stay folded.
- Expected: A visible reminder summary and progressively disclosed advanced configuration, auto-expanded for existing data, power mode, or errors.
- Evidence: `HabitScreens.kt`, baseline Habit editor capture, specialist workflow review.
- Resolution: `IMP-20260902-003` keeps required cadence inline, adds a localized reminder/configuration summary, and discloses reminder overrides, ending rules, and week boundaries with automatic expansion for existing configuration, Power Mode, and hidden validation errors.
- Status: Resolved and verified in `VER-20260902-003`.

### FND-20260831-013 — Settings can persist intermediate numeric and time edits

- Severity/category: P1 configuration correctness.
- Observed: Parseable keystrokes committed immediately, so replacing `300` with `600` could leave `6` or `60`; the old clock parser could accept a prefix of malformed input such as `12:30:99`. A write callback or process-local `SharedPreferences` observation was not durable proof that the requested value reached disk.
- Expected: Bounded local drafts, strict whole-field validation, an explicit Save boundary, and a request-scoped durable receipt. Failed durable writes must restore the prior process-visible value and retain an uncommitted draft for retry or deliberate discard.
- Why it matters: Rest timing, Gym defaults and estimated-1RM policy, Health retention, quiet hours, time zone, backup retention, and other cross-product settings could silently persist a partial or malformed value or claim success before durability was known.
- Affected users: Every user who edits typed Settings, especially one-handed gym users, keyboard users, large-text users, and people on devices experiencing lifecycle interruption or storage failure.
- Evidence: `AppSettings.kt`, `PortableBackupManager.kt`, `SettingsViewModel.kt`, `SettingsScreens.kt`, `ProductivityEditorComponents.kt`, `SettingsPresentationPolicyTest.kt`, `SettingsResponsiveUiTest.kt`, `SettingsBehaviorUiTest.kt`, and `AppSettingsPersistenceTest.kt`.
- Resolution: Typed values now open a bounded modal draft with strict parsers, explicit Save/Cancel, IME-safe actions, dirty/conflict/discard handling, semantic parent-mode identity, and request-owned durable settlement. Confirmed writes serialize through repository locks and use synchronous durable commits; a false commit restores the previous process-local preference state before releasing the lock. Failure retains the exact draft and a durability-retry obligation even if a transient process-visible value equals the draft. No-op and normalized-equal edits close without writes unless durability remains unresolved.
- Status: Resolved, independently accepted after repeated domain/UX/QA challenge, and fully verified in `IMP-20260901-017` / `VER-20260901-019`.

### FND-20260831-014 — External shared text is unbounded across Activity recreation

- Severity/category: P1 stability and input safety.
- Observed: Arbitrary shared text is retained in Activity state and every additional line may become a subtask.
- Expected: Intent-boundary title/notes/subtask budgets with explicit truncation or refusal feedback.
- Evidence: `MainActivity.kt`, `TaskEditorDialog.kt`.
- Resolution: `SharedTaskCapturePolicy` bounds every deliverable share before Activity saved state to 8,192 code points/32 KiB UTF-8, a 200-code-point title, 50 subtasks, and 200 code points per subtask. It consumes the standard `CharSequence` contract, preserves complete Unicode code points, and shows a persistent review warning. Activity now owns a restored exact FIFO: only waiting shares have a four-item capacity, additional shares collapse into a counted durable acknowledgment, and widget/notification/deep-link actions remain ordered. A saveable Task-editor launch handoff prevents shares or widget Add Task from replacing an open draft and preserves the new request's date and resolved Area through Save, Close, Replace, and recreation.
- Status: Resolved and fully verified in `IMP-20260901-020` / `VER-20260901-022`.

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
- Resolution: A returning clear Home now offers up to three concrete, count-aware recovery routes in attention order—Inbox, Upcoming, Habits, Goals, Tracks, or Gym—and opens the actual relevant collection rather than a generic root. Review evidence retains its dedicated Progress route. See `IMP-20260902-006` / `VER-20260902-006`.
- Status: Resolved and emulator/release-build verified.

### FND-20260831-018 — Track list query behavior is unreachable

- Severity/category: P2 discoverability and dead state.
- Observed: Track list query/no-match/reorder logic exists but `AllTracksPage` exposes no setter.
- Expected: Purposeful local search or removal of the unreachable branch in favor of explicit global Search routing.
- Evidence: `TrackScreens.kt`.
- Resolution: The workspace search is now the single owner for finding Tracks and Entries across active and archived data. Its action, live scope, placeholder, and empty guidance all name “Tracks & Entries”; selecting an archived Track restores the Archived workspace context. The unreachable Track-list query/no-match/reorder branches were removed. See `IMP-20260902-007` / `VER-20260902-007`.
- Status: Resolved and emulator/release-build verified.

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
- Gym destructive sub-resolution: Permanent Exercise, Routine, and completed-Workout deletion now freeze complete reviewed impacts, reject changed dependencies, block active-workout corruption, preserve immutable Training Max decisions and Routine-sourced workout history as explicitly stated, distinguish pre-commit failure from committed follow-up warnings, and retain exact request/recovery ownership across rotation and process replacement. Other Gym secondary mutations remain open for separate risk-proportionate review.
- Gym evidence: `DomainDeletionCoordinator.kt`, `GymDao.kt`, `RoutineDao.kt`, `GymViewModel.kt`, `GymScreens.kt`, `DomainDeletionCoordinatorTest.kt`, `GymDeletionViewModelIntegrationTest.kt`, `GymPowerInputUiTest.kt`, and `WorkoutDeletionUiTest.kt`.
- Gym active-session sub-resolution: Add/create/substitute Exercise, detailed Set and Exercise editing, quick-set completion, workout finish/5/3/1 review, and discard now use exact stable identities, revisions, or request-owned terminal receipts proportionate to their risk. Drafts/errors survive Activity recreation; a process-recreated orphan warns and releases instead of hanging. Workout-only additions create and focus an editable first Set without changing the Routine. Finish binds the exact session UUID/revision and required Main-work evidence. Derived PR/Link/timer work is durable, warning-based, and startup-reconcilable rather than a false retry of the committed workout fact.
- Gym active-session evidence: `GymModels.kt`, `GymEntities.kt`, `GymDao.kt`, `GymRepository.kt`, `RoutineRepository.kt`, `GymViewModel.kt`, `GymScreens.kt`, `RestTimerNotifications.kt`, schema 40, backup format 17, and focused JVM/Android/migration/backup/E2E regressions.
- Track sub-resolution: Definition, direct Entry, and CSV batch mutation families are resolved in `IMP-20260901-013` through `IMP-20260901-015`; Track/Entry search ownership is separately resolved in `IMP-20260902-007` and remains outside authored-save integrity.
- Settings/Health/Area/Tag sub-resolution: Numeric, clock, time-zone, quiet-hour, retention, and other typed Settings use bounded explicit drafts, strict validation, durable request receipts, failed-commit rollback/retry, lifecycle/conflict protection, and IME/large-text reachability. Custom Unit create/rename/archive/version flows now have exact identity/revision ownership, retained drafts, and narrow/large-text reachability. Health Connect now has least-privilege category scope, serialized mutations, atomic source-window reconciliation, durable interrupted-action recovery, explicit external-provider ownership, and exact local-copy deletion. Whole-app reset is exclusive with every admitted data-access lease. Area and Tag management now publish exact request-owned results, retain reviewed dialogs on failure, distinguish rename from merge, and model archived identity/search/restoration explicitly. Area destructive work uses repository truth; Tag rename/merge updates Task, Habit, Goal, and Track references transactionally.
- Status: Resolved for the complete inventoried asynchronous/draft-bearing secondary-mutation set. The final Gym Category save and Track permanent-deletion gaps are resolved in `IMP-20260902-024` / `VER-20260902-025`; synchronous, immediately reversible picker selection and local preference shortcuts intentionally remain lightweight.

### FND-20260831-020 — Health reconciliation is not batch-atomic

- Severity/category: P2 external-data consistency and recovery.
- Observed: Health reconciliation upserts each provider row in a separate repository transaction and prunes absent rows afterward. An exception midway can leave an imported prefix partially refreshed and skip authoritative pruning.
- Expected: One source-window reconciliation either commits its validated upserts and prefix-scoped pruning together or retains the prior local mirror with a retryable failure.
- Why it matters: A provider/API/unit/collision failure during a multi-record sync can temporarily mix old and new mirror state even though deterministic row identity prevents duplication.
- Evidence: `HealthConnectManager.reconcileHealthRecords`, `RoomMeasurementRepository.record`, `deleteSourceEntriesExcept`, and the three-agent Measurement regression audit summarized in `VER-20260831-013`.
- Recommended solution: Introduce a narrow repository-level Health source-snapshot transaction after defining window/prefix ownership and collision behavior; do not generalize it into a cross-domain import DSL.
- Resolution: Health provider rows are validated and reconciled as one exact prefix/window transaction. Stable provenance, ID/prefix collision checks, provider offset policy, narrow-window preservation, and time-zone policy are revalidated before commit. Sync, policy changes, local deletion, and recovery share one manager boundary; a failed transaction retains the prior local mirror and a durable retryable journal.
- Status: Resolved, independently accepted, and fully verified in `IMP-20260901-019` / `VER-20260901-021`.

### FND-20260902-001 — Habit timers could corrupt duration history and lose ownership

- Severity/category: P0 duration correctness, persistence, recovery, and mobile usability.
- Observed: Stopping a five-minute Habit timer passed `300` elapsed seconds through the Habit's entered unit, so a minute-based Habit could record 300 minutes / 18,000 canonical seconds. Start overwrote the only timestamp, Stop was owned only by numeric Habit ID, elapsed time used the mutable wall clock, and no durable session distinguished retries, stale widget actions, reboot, restore, or a newer timer. Active timers could also disappear behind schedule/Area/archive filters and exposed no live elapsed or recovery UI.
- Expected: Timer actions own an exact stable Habit/session identity; elapsed time and visible state use a boot-owned monotonic clock; canonical seconds convert through a frozen Duration unit exactly once; uncertain elapsed time requires explicit review; and unresolved timers remain reachable on screen and in widgets.
- Why it matters: Ordinary timer use could overstate progress by 60×, a delayed action could stop or start the wrong session, clock changes could contradict saved history, and reboot/restore ambiguity could silently become an authoritative fact.
- Affected users: Duration-Habit users, custom-unit users, widget users, travelers, upgraded/restored users, one-handed mobile users, and anyone retrying after process or reminder failure.
- Evidence: `HabitRepository.kt`, `HabitEntities.kt`, `HabitDao.kt`, `HabitTimerClock.kt`, `HabitViewModel.kt`, `HabitScreens.kt`, widget timer paths, `WhipDatabase.kt`, `BackupRepository.kt`, and focused repository/UI/migration/backup tests.
- Resolution: Implemented in `IMP-20260902-002` and fully verified in `VER-20260902-002`.
- Status: Resolved and emulator/release-build verified; physical-device release remains separate.

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
- Resolution: Every selected file now owns a saveable batch UUID, exact payload/mapping/form fingerprint, deterministic per-row Entry UUIDs, and a private digest-only Track-owned receipt. Preview and persistence share one atomic form snapshot; exact receipt verification precedes URI access after recreation; changed content, Track identity, Field/Choice/unit semantics, malformed requests, or identity collisions fail closed. A fresh batch identity still permits intentional duplicate facts. The bounded insertion path builds search rows from the validated in-memory form, checks cancellation transactionally, and validates distinct selected units once; the maximum 5,000-row/100,000-cell custom-unit case is directly exercised.
- Status: Resolved, independently accepted, and fully verified in `IMP-20260901-015` / `VER-20260901-017`.

### FND-20260901-025 — Active Gym outcomes were not one exact historical transaction boundary

- Severity/category: P0/P1 workout history, 5/3/1 progression, concurrency, lifecycle, and notification integrity.
- Observed: Active-workout presentation and mutation were assembled from independently invalidated session/placement/Set streams and several global or callback-owned outcomes. A stale quick-set or finish action could bind the wrong workout revision; removed/substituted placements could disappear from the evidence used for progression and history; optional Joker work could displace supplemental work; a failed post-commit PR/Link/timer action could look like the workout itself failed; and a process or Activity transition could lose the exact editor/result owner. Timer work was not bound to an exact timer generation and stale PR cleanup was undiscoverable after the last completed Set disappeared.
- Expected: What the lifter reviewed, performed, skipped, replaced, or optionally accepted must remain an immutable, exact workout fact. 5/3/1 advancement must derive from required Main-work evidence, never mutable presentation flags. Joker, supplemental, assistance, and optional work must remain additive/distinct. Authoritative Room commits must not be retried because a derived notification/index failed, and UI delivery must survive ordinary lifecycle interruption.
- Why it matters: A one-handed in-gym action could overwrite a concurrent Set, advance a Training Max after incomplete work, lose performed history, remove BBB/FSL/BBS after accepting a Joker, duplicate a quick save, or leave a stale rest notification. These are trust failures for every Gym user and program-correctness failures for 5/3/1 lifters.
- Affected users: Mobile/in-gym users, novice and advanced 5/3/1 lifters, arbitrary-lift program users, users resuming partial workouts, and anyone with process interruption or unreliable notification scheduling.
- Evidence: `GymEntities.kt`, `GymDao.kt`, `GymRepository.kt`, `RoutineRepository.kt`, `GymViewModel.kt`, `GymScreens.kt`, `FiveThreeOneCycleReview.kt`, `RestTimerNotifications.kt`, `WhipApplication.kt`, migration 39→40, backup 16→17, and the focused plus full campaigns in `VER-20260901-018`.
- Resolution: Workout/session and timer generations are explicit; quick-set authorship freezes stable Set identity and exact submission revision; finish freezes session identity/revision and progression decisions; required progression evidence and placement/Set outcomes are immutable snapshots; retired placements remain historical but non-executable; Joker acceptance is independent and additive; generic copies are sanitized; active projections come from one transactional Room snapshot; new-placement focus waits for that snapshot. Request-owned mutation results retain drafts and errors across Activity recreation. PR/Link/timer follow-ups reconcile from durable rows; timer scheduling is awaited and at-least-once with exact revision/deadline acknowledgement.
- Status: Resolved, independently accepted after repeated domain/UX/QA challenge, and fully verified in `IMP-20260901-016` / `VER-20260901-018`.

### FND-20260901-026 — Lightweight Gym structure mutations still lack exact no-op and Undo boundaries

- Severity/category: P1/P2 active-workout structural integrity, concurrency, and interaction truth.
- Observed: Active-session group normalization may advance a workout revision even when the normalized structure is already identical. Exercise/group removal, reorder, and Undo remain split across callback/global-status paths; a delete followed by reorder and restore can reuse or collide with mutable positions instead of restoring one exact reviewed structure.
- Expected: A normalization pass that changes no persisted semantics is a true no-op. Removal/reorder/restore should bind stable identities and one exact workout revision, preserve a collision-free order, and report the outcome only to the initiating interaction.
- Why it matters: Background repair can create nuisance stale-write conflicts, while rapid one-handed remove/reorder/Undo can restore a different layout than the lifter intended or make the visible revision disagree with the meaningful workout facts.
- Affected users: Mobile/in-gym users, grouped-circuit/superset users, users correcting an active workout, and users resuming after lifecycle interruption.
- Evidence: `GymRepository.kt` group normalization and structural mutations, `GymDao.kt` ordered group/placement rows, `GymViewModel.kt` startup normalization plus `removeWorkoutExercise`, `removeWorkoutExerciseFromGroup`, and `reorderWorkoutExercises`, and `GymScreens.kt` grouped reorder/removal interactions.
- Recommended solution: Compute normalized structure before writing and return without revision change when equal. Introduce a narrow stable structure boundary and transaction-owned receipt for remove/reorder/restore; renumber positions atomically, retain an exact removable snapshot only as long as Undo is offered, and add concurrency, lifecycle, no-op, and grouped-order regressions. Reuse existing Gym request/receipt primitives rather than a generic workout DSL.
- Resolution: Normalization now compares the canonical structure before writing and is a true no-op when equal. Add/create/substitute, machine assignment, remove/ungroup/group, batch Arrange, structural restore, Set add/update/complete/duplicate/delete/Undo, discard, and History Copy bind stable identities and exact session/placement/Set/structure versions inside atomic repository transactions. Arrangement includes retired placements and tombstones, while its structure fingerprint deliberately excludes mutable Set values/completion so same-session Undo restores layout without erasing newer logged work. Dedicated request coordinators retain authorship, errors, disabled states, replay identity, and History Copy intent across Activity/process reconstruction. The active screen keeps normal logging uncluttered and exposes drag handles only in an explicit responsive Arrange mode.
- Status: Resolved, fully regression-tested, and independently accepted after repeated domain, lifter/UX/accessibility, and adversarial-QA challenge in `IMP-20260901-018` / `VER-20260901-020`.

### FND-20260901-027 — Remaining Settings, Health, and secondary-dialog mutations need risk-proportionate ownership

- Severity/category: P1/P2 authored-state, privacy/defaults, and cross-product consistency.
- Observed: The typed Settings family is now transactional, but adjacent choice/destructive flows are not uniformly request-owned. Custom-unit secondary dialogs and some reminder/Health choices can still dismiss or settle from broad asynchronous state, and Health import/default/deletion semantics need an explicit least-privilege product review. This is separate from the already-recorded non-atomic Health reconciliation in `FND-20260831-020`.
- Expected: Draft-bearing or consequential choices retain input until their exact authoritative result, expose committed-follow-up warnings without false retry, and survive Activity recreation. Health defaults, import scopes, and deletion wording should make source ownership and privacy consequences explicit without adding friction to harmless preferences.
- Why it matters: A polished typed editor does not protect a neighboring unit, reminder, or Health action that can lose a draft, close on failure, or leave users unsure whether Whip or the external provider owns the underlying fact.
- Affected users: Custom-unit power users, reminder users, Health Connect users, accessibility users, and anyone editing during lifecycle or storage interruption.
- Evidence: `SettingsScreens.kt`, `SettingsViewModel.kt`, Custom Unit dialog flows, Health Connect Settings/import/delete paths, and the cross-feature Settings/Health specialist audit completed with `VER-20260901-019`.
- Recommended solution: Inventory each adjacent action by draft cost, reversibility, historical effect, external-system ownership, and latency. Extend request-owned receipts only where a lost outcome would be ambiguous; preserve lightweight immediate toggles where rollback is explicit. Resolve `FND-20260831-020` with a narrow source-window transaction and add privacy copy, recreation, large-text, failure, and deletion-provenance tests.
- Resolution: Custom Unit, Health, Area-management, and Tag-management flows now use exact, lifecycle-owned outcomes proportionate to their risk. Custom Unit drafts survive failure/recreation; IDs and semantic versions are collision-safe; Health mutations are serialized, recover interrupted local actions durably, distinguish committed warnings from failures, state provider ownership explicitly, and remain reachable at narrow width/large text. Area and Tag dialogs retain their initiating draft/choice until one authoritative result; archived identities are explicit and global rename/merge is transactional. Harmless immediate choices remain lightweight rather than inheriting a generic transaction framework.
- Status: Resolved for the complete inventoried draft-bearing, consequential, and asynchronous secondary-dialog set. The last Gym Category and Track deletion outcomes are covered by `IMP-20260902-024` / `VER-20260902-025`; harmless synchronous preferences and reversible picker choices remain intentionally lightweight.

### FND-20260901-028 — Portable restore and whole-app reset could accept semantically invalid or concurrently recreated data

- Severity/category: P0/P1 data integrity, recovery, and destructive-operation safety.
- Observed: Backup preflight did not prove every restored value/unit/canonical triple against the actual owning domain contract, private recovery state shared the same export surface as portable data, and reset serialized only selected Health/reminder work rather than every repository lease. A concurrent admitted mutation could therefore complete during reset and recreate data after deletion. Legacy automation-generated Habit rows could also carry a custom-unit canonical value calculated from an identifier instead of the paired metric fact.
- Expected: Portable restore must reject malformed or semantically incompatible historical facts before replacing user data; private rollback may retain local recovery journals without exporting them across devices; reset must drain and exclude all readers/writers until runtime is rebuilt; a legacy repair may run only when the original metric fact proves the intended value and unit conversion exactly.
- Why it matters: A checksum-valid backup can still be internally impossible, and a visually successful reset that leaves late-created rows is a false destructive outcome. Retrospective canonical repair without proof could rewrite historical meaning.
- Affected users: Everyone using backup/restore or Reset Whip, users with custom units, Health Connect users, long-lived upgraded installs, Gym users with historical Training Max/PR/workout facts, and automation-created Habit history.
- Evidence: `BackupRepository.kt`, `RestoreRecoveryManager.kt`, `StartupRecoveryGate.kt`, `WhipApplication.kt`, `MeasurementRepository.kt`, `HabitDao.kt`, `LinkRepository.kt`, focused backup/recovery/concurrency tests, and the independent domain/UX/adversarial-QA reviews for `VER-20260901-021`.
- Resolution: Semantic preflight now validates units, dimensions, canonical parity/range, machine/load contracts, stable IDs, Training Max snapshots/decisions, PRs, exercises, routines, workouts, Goals, Tracks, Habits, and automation constants before replacement. Portable export omits device-local Health journals/receipts while private rollback preserves them. An application-wide maintenance gate closes admission, drains active leases, advances user-data generation, performs reset under the fixed Health → reminder → Room order, and rebuilds runtime before reopening. Startup repairs the narrow legacy Habit defect only when the paired metric row proves full provenance/value/unit/metric identity.
- Status: Resolved, independently accepted, and fully verified in `IMP-20260901-019` / `VER-20260901-021`.

### FND-20260902-002 — Habit availability states looked actionable or unfinished when no action was expected

- Severity/category: P1 Habit Today truthfulness, accidental input, mobile usability, accessibility, and time-zone consistency.
- Observed: Paused and off-schedule Habit cards could display an ordinary pending status and expose the same one-tap check-in, value, checklist, and target controls as a scheduled Habit. A scheduled pause inspector offered a normal primary check-in. Skipped Habits remained in the action-needed Today/Home group even though the skip deliberately resolved today's expectation. Timer start time in the inspector used the process system zone rather than Whip's configured zone.
- Expected: Today distinguishes work that still needs attention from completed/skipped work and neutral unavailable states. Paused/off-schedule cards explain why no check-in is expected and cannot be logged accidentally; intentional off-schedule logging remains available through an explicit details action. Active timers always remain reachable. Every displayed Habit time uses Whip's configured live zone.
- Why it matters: A quick glance could tell users to act when they intentionally paused, skipped, or did not schedule a Habit. One-handed taps could create history outside the intended schedule, Home counts could overstate unfinished work, and travelers could see contradictory timer times.
- Affected users: All Habit users, especially ADHD/low-attention users, one-handed mobile users, scheduled-pause users, flexible-schedule users, travelers, and users reviewing or undoing skipped work.
- Evidence: `HabitScreens.kt`, `WhipApp.kt`, `CompactCollectionStatusTest.kt`, `ProductivityCardDesignUiTest.kt`, `ActivityHistoryUiTest.kt`, and `HabitSkipJourneyE2ETest.kt`; the paused All-Habits capture and semantics hierarchy live under `artifacts/full-product-audit/2026-09-02/habit-availability/`.
- Resolution: Implemented in `IMP-20260902-004` and fully verified in `VER-20260902-004`.
- Status: Resolved and emulator/release-build verified; broader Habit History and cross-platform accessibility work remains separate.

### FND-20260902-003 — Habit History hid pauses and sorted backfilled events by edit time

- Severity/category: P1 Habit historical truth, understandability, pause editing, and destructive-action disclosure.
- Observed: History combined check-ins and skips but omitted scheduled-pause records. Backfilled check-ins were ordered by their write timestamp rather than the effective local date, so entering an older day today could move it above newer activity. A Habit with only a pause said “No activity yet” in Insights and showed a misleading 0% completion. Editing/deleting a past pause did not explain that unlogged dates, streak, and consistency can recalculate, and permanent Habit deletion omitted pause records from its impact count.
- Expected: One chronological Habit History orders every started check-in, skip, and pause by the date it describes; upcoming pauses remain schedule configuration. Pause-only history is visible and neutral rather than failed. Any pause change that reaches today/past states exactly which derived history can change and which authored facts remain. Permanent deletion accounts for every owned record type.
- Why it matters: Users correcting earlier history could see a false chronology, overlook why a period was neutral, mistake an intentional recovery/travel pause for inactivity or failure, or approve a destructive change without understanding its impact on derived streaks.
- Affected users: Habit users who backfill entries, schedule travel/recovery breaks, inspect long histories, tune streak behavior, use assistive technology, or permanently delete a Habit.
- Evidence: `HabitScreens.kt`, `ActivityPresentationTest.kt`, and `ActivityHistoryUiTest.kt`; emulator captures and semantics hierarchies live under `artifacts/full-product-audit/2026-09-02/habit-history/`.
- Resolution: Implemented in `IMP-20260902-005` and fully verified in `VER-20260902-005`.
- Status: Resolved and emulator/release-build verified; no historical rows were rewritten.

### FND-20260902-004 — Primary navigation and clear Home removed context when users needed more support

- Severity/category: P1 accessibility/navigation plus P2 returning-user and ADHD context recovery.
- Observed: Compact navigation removed all six visible labels at 150% text even though one named row could still fit; the rail used a fixed 80dp width, ellipsized enlarged labels, and hid every label in short windows. Separately, a returning user with Inbox, Upcoming, archived/paused/unpinned, or Gym-library data could receive only “Your Day Is Clear” with no route back to that saved context.
- Expected: Text enlargement must never convert understandable navigation into glyph memorization. Whip should measure the current labels, preserve stable direct destinations, remain usable in short/landscape windows, and give a clear returning Home a bounded nonjudgmental recovery route that opens the relevant collection.
- Why it matters: Sighted low-vision, cognitive/ADHD, first-time, and interruption-prone users were given less information precisely when they requested more, while existing work appeared to have vanished from an otherwise empty Home.
- Affected users: 150–320% text users, narrow/landscape/fold users, keyboard and one-handed users, returning users, users who intentionally leave work in Inbox/Upcoming or unpinned collections, and anyone with saved Gym plans or equipment but nothing due today.
- Evidence: `WhipApp.kt`, `WhipNavigationPolicyTest.kt`, `AdaptiveWhipScreenTest.kt`, `VisualAcceptanceMatrixTest.kt`, and the captures/hierarchies under `artifacts/full-product-audit/2026-09-02/navigation-home/`.
- Resolution: Implemented in `IMP-20260902-006` and fully verified in `VER-20260902-006`; this also resolves `FND-20260831-011` and `FND-20260831-017`.
- Status: Resolved and emulator/release-build verified; no saved navigation or user data was changed.

### FND-20260902-005 — Track search had no coherent owner and lost archive context

- Severity/category: P2 discoverability, information architecture, accessibility, and navigation correctness.
- Observed: `TrackAreaContent` retained a local Track-name query with no control capable of changing it, including dead no-match and “clear search” reorder branches. The reachable shell action announced only “Search Tracks” even though its actual scope included Tracks and Entries. Its empty hint claimed to search across all Whip while that narrower scope was active, and selecting an archived Track forced the active Tracks destination.
- Expected: One visible search owner should state exactly what it searches, find both Track definitions and Entry content across active/archived data, keep guidance synchronized with the live scope, and land results in their truthful workspace context. Local Activity and per-Track Entry searches remain purposeful filters for those specific collections.
- Why it matters: Users with many structured logs could not discover the dormant list behavior, screen-reader users heard an incomplete scope, sighted users received contradictory guidance, and archived results appeared under the wrong navigation state.
- Affected users: Track users with many logs or Entries, archived history users, keyboard/screen-reader users, ADHD/interruption-prone users, and anyone searching from a filtered Area.
- Evidence: `TrackScreens.kt`, `WhipApp.kt`, `WhipNavigationPolicy.kt`, `UnifiedSearchDialog.kt`, `TrackWorkspaceUiTest.kt`, `GlobalSearchRoutingTest.kt`, and the inspected capture/hierarchy under `artifacts/full-product-audit/2026-09-02/track-search/`.
- Resolution: Implemented in `IMP-20260902-007` and fully verified in `VER-20260902-007`; this also resolves `FND-20260831-018`.
- Status: Resolved and emulator/release-build verified; no Track, Entry, Area, schema, or backup data changed.

### FND-20260902-006 — Short-window search dialogs could hide the result or state the user just requested

- Severity/category: P1 mobile accessibility and interaction truthfulness plus P2 cross-device test portability.
- Observed: On a 320×533dp API 26 phone, opening the keyboard in the emoji picker left a matching saved choice as a nearly invisible sliver below redundant search copy and editor controls. Unified Search could retain a deep result-list offset after filters were expanded or collapsed, hiding the active-filter summary. Several otherwise valid journeys also assumed a requested synthetic width would exceed the physical root or that below-fold controls and duplicated wide-pane labels were already visible.
- Expected: Search results and active state changes remain immediately reachable in the actual post-keyboard viewport. Compact and wide layouts expose the same semantic actions without requiring impossible physical dimensions, and automation scrolls the owning collection or scopes duplicated master/detail content rather than weakening production layout constraints.
- Why it matters: A one-handed user could type the exact emoji or filter they wanted and appear to receive no usable result. Low-height, large-text, keyboard, screen-reader, fold, and desktop users need equivalent reachable behavior, while a trustworthy platform matrix must distinguish product defects from invalid test geometry.
- Affected users: Small-phone and landscape users, software-keyboard users, 150–320% text users, keyboard/TalkBack users, foldable/tablet/desktop users, and anyone searching a long result list.
- Evidence: `IdentityEmojiPicker.kt`, `UnifiedSearchDialog.kt`, the portable navigation/journey regressions, and inspected screenshot/UI-hierarchy evidence under `artifacts/full-product-audit/2026-09-02/platform-matrix/`.
- Resolution: Implemented in `IMP-20260902-008` and fully verified in `VER-20260902-008`.
- Status: Resolved across API 26/34/37, actual TalkBack keyboard traversal, adaptive visual/semantic matrices, the complete emulator gate, and release builds; no user data or domain semantics changed.

### FND-20260902-007 — Area management lost exact outcomes and misrepresented archived Areas

- Severity/category: P1 authored-choice integrity, destructive interaction ownership, information architecture, and accessibility.
- Observed: Area create, rename, recolor, reorder, move, merge, archive, restore, and delete used broad asynchronous Settings state; several dialogs dismissed as soon as Save was tapped, so a failure erased the reviewed choice or draft. An archived Area detail labeled its lifecycle action “Restore Area” but opened the archive path. Creating a name that matched an archived Area offered “Select Existing” without restoring it, search hid archived matches until the Archived section had been manually expanded, and rename conflicts told users to merge into an archived destination that merge correctly rejects. Color updates also read and wrote outside one Room transaction.
- Expected: Each Area mutation belongs to the initiating surface until one exact authoritative result. Failure preserves the reviewed choice and error in context; a committed database change followed by Settings cleanup reports a warning instead of inviting a destructive retry. Archived identities are searchable, explicitly restorable, never selected as though active, and keep their saved identity/color. Repository read-modify-write rules remain atomic.
- Why it matters: Users organizing Tasks, Habits, Goals, Tracks, and Gym context could lose a draft, repeat a committed destructive action, select an Area that pickers then hide, follow impossible conflict guidance, or race a color update. These failures undermine trust in a cross-product ownership feature.
- Affected users: All Area users, especially users with archived projects, many Areas, one-handed/low-attention workflows, assistive technology, storage interruption, or mixed-domain assignments.
- Evidence: `AreaManagementDialog.kt`, `AreaPicker.kt`, `WhipColorPicker.kt`, `SettingsViewModel.kt`, `AreaRepository.kt`, `AreaFeatureUiTest.kt`, `AreaMutationViewModelTest.kt`, `MeasurementTaxonomyRepositoryTest.kt`, `DomainDeletionCoordinatorTest.kt`, and the captures/hierarchies under `artifacts/full-product-audit/2026-09-02/area-management/`.
- Resolution: Implemented in `IMP-20260902-009` and fully verified in `VER-20260902-009`.
- Status: Resolved and emulator/release-build verified; other secondary-dialog families remain separately tracked under `FND-20260831-019` and `FND-20260901-027`.

### FND-20260902-008 — Tag management conflated rename, merge, archive, and incomplete cross-domain references

- Severity/category: P1 cross-domain data semantics, authored-choice integrity, discoverability, and accessibility.
- Observed: Renaming a Tag to an existing name silently merged and deleted the source without saying so; Track references were omitted from global rename/merge; archive was not one transaction and exposed no affected-item counts; archived labels could remain hidden when reused; and the comma reserved by `tagsCsv` could be authored as though it were part of one Tag. Tag actions lived in the long Organization page, closed optimistically, and did not provide one searchable active/archived manager or retained failure context.
- Expected: Rename changes spelling while merge is an explicit source-to-destination operation. Both update every current Task, Habit, Goal, and Track reference atomically. Archive preserves references and identity, remains stable until explicit Restore, and is searchable. Invalid separator input is rejected before persistence. Each mutation belongs to its initiating surface through one exact result and remains usable on a narrow or enlarged-text phone.
- Why it matters: A harmless-looking rename could delete taxonomy identity, leave Tracks under stale labels, or make archived state ineffective. Storage-specific syntax could create an unreferenceable Tag, while a failure could discard the user’s reviewed choice.
- Affected users: Task, Habit, Goal, and Track users; people with many labels or archived projects; customization-heavy and ADHD users; one-handed mobile users; and assistive-technology/enlarged-text users.
- Evidence: `MeasurementRepository.kt`, `MeasurementDao.kt`, `SettingsViewModel.kt`, `SettingsScreens.kt`, `TagManagementDialog.kt`, `TaskEditorRouteHost.kt`, `WhipApp.kt`, the focused Tag repository/ViewModel/Compose tests, and final captures/hierarchies under `artifacts/full-product-audit/2026-09-02/tag-management/`.
- Resolution: Implemented in `IMP-20260902-010` and fully verified in `VER-20260902-010`.
- Status: Resolved and emulator/release-build verified; no Room schema or backup migration was required.

### FND-20260902-009 — Completed-Workout deletion reviewed stale projections and abandoned its outcome

- Severity/category: P1 historical-data integrity, destructive UX, 5/3/1 auditability, and lifecycle ownership.
- Observed: Gym History built permanent-deletion counts from independently collected UI lists, dispatched an ID-only delete, and immediately closed the confirmation. A workout or its sets could change after review; a failure lost the reviewed surface; post-commit PR/Link/rest-timer failure looked indistinguishable from delete failure; and the dialog did not explain that Training Max decisions and linked history remain immutable.
- Expected: The repository produces one exact, revision-tokened impact from a transaction; active sessions cannot be erased; commit checks the reviewed revision; failure retains context; a committed delete is never retried because derived cleanup failed; and preserved versus removed history is stated before confirmation.
- Why it matters: Completed workouts are source evidence for progress charts, personal records, 5/3/1 cycle decisions, and user trust. A false retry or stale impact can erase more history than the lifter reviewed, while deleting Training Max audit evidence would retroactively rewrite programming decisions.
- Affected users: All Gym History users, especially 5/3/1 lifters, users correcting old workouts, one-handed/mobile users, and users interrupted by process or storage failure.
- Evidence: `DomainDeletionCoordinator.kt`, `GymViewModel.kt`, `GymScreens.kt`, `DomainDeletionCoordinatorTest.kt`, `GymDeletionViewModelIntegrationTest.kt`, `WorkoutDeletionUiTest.kt`, and inspected final-source artifacts under `artifacts/full-product-audit/2026-09-02/workout-deletion/`.
- Resolution: Implemented in `IMP-20260902-011` and fully verified in `VER-20260902-011`. The exact transaction removes only the selected session graph, rebuilds personal-record projections after commit, preserves Training Max decisions and retired Link/automation audit history under the compatibility policy, and publishes an owned result across lifecycle recovery.
- Status: Resolved and emulator/release-build verified; no schema, backup format, completed record outside the selected workout, Exercise/Routine definition, or historical Training Max decision was rewritten.

### FND-20260902-010 — Machine deletion had an exact transaction but an unowned UI outcome

- Severity/category: P1 destructive lifecycle ownership, equipment-catalog integrity, responsive UX, and accessibility.
- Observed: Machine deletion already used an exact domain transaction, but the screen relied on callback/global status and an in-memory impact. Activity or process recreation could lose the reviewed target, revision, and terminal outcome; a failure could detach from the dialog that authored it.
- Expected: The request saves the Machine ID, stable UUID, data generation, exact impact, and revision; commit accepts only that reviewed revision; one request-owned terminal state survives recreation; recovery verifies current repository presence before declaring success; mismatch or uncertainty requires explicit retry/review.
- Why it matters: Deleting equipment can change multiple routines while completed workouts must continue to describe what happened. Losing request identity can cause duplicate destructive attempts, false success, or uncertainty about affected routines.
- Affected users: Users maintaining Machine profiles and routines, especially users interrupted during deletion, users with large text or narrow phones, and lifters relying on truthful historical equipment snapshots.
- Evidence: `GymViewModel.kt`, `GymScreens.kt`, `GymDeletionViewModelIntegrationTest.kt`, `GymPowerInputUiTest.kt`, and `artifacts/full-product-audit/2026-09-02/machine-deletion/`.
- Resolution: Implemented in `IMP-20260902-012` and fully verified in `VER-20260902-012` through the shared exact Gym-deletion lifecycle.
- Status: Resolved and emulator/release-build verified; no schema, backup format, Machine identity outside the selected target, Routine identity, or completed-workout history was rewritten.

### FND-20260902-011 — Full-screen editors had no consistent action hierarchy

- Severity/category: P1 cross-product interaction consistency and large-text accessibility.
- Observed: Task, Track, Track Entry, Routine, and shared Productivity/Gym editors independently composed their title, exit action, divider, and Save action. Some used a filled primary action, others a text action; compact behavior differed; Routine exposed both Back and Close for the outline.
- Expected: Every primary editor keeps one stable title/exit row, one visually primary commit action, predictable nested Back behavior, and an action row that wraps below the identity at narrow widths or enlarged text.
- Why it matters: Relearning basic editor controls increases errors and cognitive load, while crowded headers can truncate the editor identity or hide the action users need.
- Affected users: All authoring users, especially one-handed, ADHD/interruption-prone, narrow-phone, and enlarged-text users.
- Evidence: `WhipPagePatterns.kt`, `ProductivityEditorComponents.kt`, `TaskEditorDialog.kt`, `TrackScreens.kt`, `RoutineBuilder.kt`, `GymScreens.kt`, `UiDesignArchitectureTest.kt`, and `InteractionControlUiTest.kt`.
- Resolution: Implemented in `IMP-20260902-019` and verified in `VER-20260902-020`.
- Status: Resolved for primary editor chrome; request-owned persistence and dirty-state behavior remain independently audited functional concerns.

### FND-20260902-012 — Compatibility scaffolding prevented a truthful clean-slate release boundary

- Severity/category: P0 data-integrity boundary and P1 architecture/maintainability.
- Observed: Whip still registered a long Room migration chain, retained historical schemas and upgrade-only tests, accepted multiple old portable-backup shapes, and ran compatibility repair/default logic after the user explicitly chose a breaking clean slate.
- Expected: One canonical schema and backup contract, an explicit pre-database epoch decision, no implicit destructive Room fallback, and a user-confirmed reset that either finishes durably or keeps the entire product blocked. Completed reset must remove only Whip-owned local state, cancel stale work/actions, rebuild schema 42, and never expose an unconfirmed destructive retry.
- Why it matters: Half-removing compatibility can corrupt meaning, resurrect stale work, or make a reset appear successful while old state survives. Deleting before durable confirmation would violate the user's authorship of a destructive upgrade.
- Affected users: Every updating user, backup/restore users, widget/reminder users, and developers changing persistence or program semantics.
- Evidence: `DataEpochGate.kt`, `LocalDataResetter.kt`, `WhipApplication.kt`, `MainActivity.kt`, `WhipDatabase.kt`, `BackupRepository.kt`, schema exports, startup/reset UI, and the focused tests recorded in `VER-20260902-021`.
- Resolution: Implemented in `IMP-20260902-020`; independent review found no remaining P0/P1 blocker after reset serialization, launch-request invalidation, exact-current backup enforcement, removal of schema 1–41 and ignored pre-epoch backup tests, and twice-run destructive reset integration coverage.
- Status: Resolved and targeted/emulator verified; the frozen candidate still requires the complete release gate before distribution.

### FND-20260902-013 — User-facing enum names and section headings drifted across product areas

- Severity/category: P2 understandability, localization, visual consistency, and accessibility.
- Observed: Settings, Home/Review choices, Task priority filters, Goal status/periods, Habit navigation/weekdays, Gym destinations/session state/group types, and measurement dimensions often rendered Kotlin enum `.name` values directly. Section titles in Settings and Gym also rebuilt typography/dividers ad hoc. This made labels dependent on internal identifiers, produced unclear terms such as `Active` for an in-progress workout, and bypassed locale-aware weekday names.
- Expected: Every user-facing choice owns an explicit product label; storage keys remain separate. Repeated form/analysis sections use one shared heading hierarchy, and weekday labels follow the user's locale.
- Why it matters: Internal naming leaks make UI copy fragile, block intentional wording, hinder localization, and force users to reinterpret the same hierarchy on each screen.
- Affected users: All Whip users, especially screen-reader, enlarged-text, non-English-locale, ADHD/interruption-prone, and new Gym users.
- Evidence: `AppSettings.kt`, `TaskModels.kt`, `GoalModels.kt`, `MeasurementModels.kt`, `GymModels.kt`, `HabitScreens.kt`, `SettingsScreens.kt`, `GymScreens.kt`, `WhipApp.kt`, and `UiDesignArchitectureTest.kt`.
- Resolution: Implemented in `IMP-20260902-021` and targeted verified in `VER-20260902-022`.
- Status: Resolved for the audited visible choices and shared section families; later features must use the same explicit-label contract.

### FND-20260902-014 — Historical search selection was bounded only after a full sort

- Severity/category: P1 scalability/perceived performance and P2 deterministic QA.
- Observed: Unified Search moved index work off the UI thread and capped displayed history, but `newestSearchValues` still sorted every value for an entity before taking 100 or 500. A long-lived Track/Habit could therefore allocate and compare the entire history to show a small bounded subset.
- Expected: Work and retained memory scale with the explicit result cap; selection remains newest-first and stable for ties; large histories from one domain cannot crowd another domain out.
- Why it matters: Search is a global recovery tool and should not stutter or allocate proportionally to years of logging, especially on lower-memory phones.
- Affected users: Long-term Track/Habit/Gym users, frequent loggers, older phones, and users relying on search during interrupted workflows.
- Evidence: `UnifiedSearchDialog.kt` and `UnifiedSearchRulesTest.kt`.
- Resolution: Implemented in `IMP-20260902-022` and verified in `VER-20260902-023`.
- Status: Resolved at current product scale; query-backed indexing remains a measured future option rather than an unproven architecture requirement.

### FND-20260902-015 — Repeated Gym controls exposed ambiguous actions to users and tests

- Severity/category: P1 accessibility/interaction certainty and P2 regression reliability.
- Observed: Repeated 5/3/1 Training Max mode actions and shared selection-menu options exposed identical visible and semantic labels. High-value Gym workflows therefore selected controls by list position, and the active-workout empty state had no stable contextual boundary.
- Expected: Repeated interactive choices identify both their field/lift context and action; tests target user intent or a named region rather than an ordinal that changes when layout or optional work changes.
- Why it matters: Screen-reader users cannot distinguish identical actions, and a layout change can silently make an automated journey press the wrong control while still finding matching text.
- Affected users: 5/3/1 lifters configuring several lifts, screen-reader and switch users, customization-heavy users, and maintainers changing responsive Gym layouts.
- Evidence: `ItemControlPatterns.kt`, `RoutineBuilder.kt`, `GymScreens.kt`, `RoutineBuilderUiTest.kt`, and `FirstClassWorkflowE2ETest.kt`.
- Resolution: Implemented in `IMP-20260902-023` and verified in `VER-20260902-024`.
- Status: Resolved for repeated high-risk Gym actions; positional assertions for duplicated, noninteractive display content remain a lower-risk continuous-cleanup item.

### FND-20260902-016 — Category save and Track deletion abandoned their initiating dialogs

- Severity/category: P1 authored-state/destructive integrity, lifecycle ownership, and cross-product interaction consistency.
- Observed: Exercise Category create/edit dispatched an asynchronous save and immediately discarded its draft. Permanent Track deletion displayed counts from the live screen projection, dispatched an ID-only deletion, closed immediately, and could report a failed post-commit Link rebuild as though the destructive commit itself had failed.
- Expected: Category drafts remain visible through their exact save result and Activity recreation. Track deletion reviews one transaction-derived, revision-tokened impact; rejects any changed definition, history, or integration graph; disables dismissal during commit; and distinguishes an authoritative deletion from a follow-up warning.
- Why it matters: A failed category save looked successful and lost typing. A stale or falsely retryable permanent deletion can remove more history than reviewed or invite a second destructive action after the first already committed.
- Affected users: Gym organizers, Track users with long histories or automations, unreliable-storage users, accessibility users, and interruption-prone mobile users.
- Evidence: `GymViewModel.kt`, `GymScreens.kt`, `DomainDeletionCoordinator.kt`, `TrackViewModel.kt`, `TrackScreens.kt`, `EditorStateRecreationTest.kt`, `TrackDeletionUiTest.kt`, and `DomainDeletionCoordinatorTest.kt`.
- Resolution: Implemented in `IMP-20260902-024` and verified in `VER-20260902-025`.
- Status: Resolved; no schema, backup, existing Category, unrelated Track, or surviving historical record is rewritten.
