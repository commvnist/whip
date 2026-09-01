# Verification and release evidence

### VER-20260831-001 — Gym remediation test evidence

- Scope/environment: JVM domain/repository/UI-rule suite plus targeted Android UI journeys on a disposable emulator; release Android sources compiled.
- Result: 402 JVM tests passed. Targeted routine-builder and workout journeys passed, including arbitrary Bench/Deadlift/Zercher programming, non-5/3/1 Training Max discovery, contextual exercise return, adaptive routine outline, workout-only additions, and additive Joker preservation.
- Counts and exclusions: Repository baseline is 402 JVM and 482 Android test sources. The complete 482 Android inventory was not executed in the final physical-device release pass.
- Related: `IMP-20260831-001`, `../testing.md`.
- Status: Verified for targeted regressions; broader emulator matrix remains part of `FB-20260831-012`.

### VER-20260831-002 — Physical-device release 0.3.34

- Scope/environment: Signed release deployment to the user's connected Samsung phone using the non-destructive release path.
- Procedure: `scripts/device release-deploy`, package/version inspection, installed-base hash comparison, cold launch, foreground activity check, app-PID-filtered fatal/database/migration log scan, and `git diff --check`.
- Result: Build/lint/R8/release gates passed; streamed install succeeded; cold launch completed in 127 ms; `MainActivity` resumed; installed and local hashes matched; no app-specific fatal, Room, SQLite, or migration errors were found.
- Artifact/version/hash: 0.3.34, version code 40, APK size 3,761,044 bytes, SHA-256 `7ddc4bfb209fef541530602420d8daa316f2e74c9b3d19f2c433f02624d7edfe`.
- Residual risk: Physical-device instrumentation was deliberately not run; broader Android regression execution belongs on disposable emulators.
- Related: `IMP-20260831-001`.
- Status: Released and smoke-verified.

### VER-20260831-003 — Product-memory skill and repository structure

- Scope/environment: Personal skill package, generated interface metadata, repository ledgers, workspace fallback, and reusable goal.
- Command: `uv run --with pyyaml python3 .../skill-creator/scripts/quick_validate.py /mnt/c/Users/commv/.codex/skills/maintain-whip-memory`; repository file-existence checks; TODO scan; `git diff --check`.
- Result: The official skill validator reported `Skill is valid!`; every canonical ledger and linked detailed source exists; no template TODO remained; repository diff checks passed.
- Counts and exclusions: Structural validation only. Behavioral quality must be assessed as the skill is used on future real Whip tasks.
- Related: `FB-20260831-013`, `IMP-20260831-002`.
- Status: Verified structurally; behavioral validation pending.

### VER-20260831-004 — Coherent commit-and-push workflow

- Scope/environment: Updated personal skill, schema reference, workspace instructions, durable memory, reusable goal, and the previously released Gym product batch.
- Procedure: Official `quick_validate.py` run in an isolated PyYAML environment; TODO/duplicate-ID/rule-presence scans; staged and unstaged diff review; `git diff --check`; focused Git commits; normal pushes to the configured `origin/main`; upstream reachability checks.
- Result: Skill validation reported `Skill is valid!`. The released Gym implementation, schemas, tests, audit, and evidence were isolated from the workflow files, committed as `5fc98dd`, pushed successfully, and found reachable from `origin/main`. The workflow change is the separate commit containing this entry and is verified by Git history rather than a recursive bookkeeping commit.
- Counts and exclusions: No product runtime changed in the workflow chunk. The personal skill installation lives outside the application repository; its equivalent durable rules are versioned in `AGENTS.md`, this memory, and the reusable goal.
- Commit/push: Gym batch `5fc98dd` on `origin/main`; workflow batch is the commit containing `VER-20260831-004` on `origin/main`.
- Related: `FB-20260831-014`, `DEC-20260831-007`, `IMP-20260831-003`.
- Status: Structurally verified and pushed; future chunks provide behavioral validation.

### VER-20260831-005 — Whole-product Iteration 1 baseline

- Scope/environment: Current `main`; disposable Android emulator `emulator-5554` at 1080 × 2400/density 420; 100% and 150% text; independent source audits for all product areas, platform surfaces, architecture, accessibility, ADHD workflows, QA, persistence, and Gym constraints.
- Commands: `scripts/check`; `WHIP_DEVICE=emulator-5554 scripts/device deploy`; non-destructive `scripts/device-artifacts capture` and `ui-dump`; source/test inventory and targeted static tracing.
- Result: The default build gate completed successfully, including JVM-test task, Android-test compilation, lint, debug assembly, and coverage thresholds. The current build cold-launched on the emulator. Every root empty state, the primary creation editors, required-field Goal validation, Gym start flow, all Settings categories, and representative 150% text screens were captured and inspected.
- Counts and exclusions: Source inventory remains 402 JVM and 482 Android tests. Some Gradle tasks were up-to-date, so this record does not claim 884 fresh test-method executions. Full instrumentation, populated workflow matrices, rotation/RTL/TalkBack, release build, and physical-device smoke remain later iteration gates.
- Findings: `FND-20260831-007` through `FND-20260831-018`; detailed evidence in `../WHOLE_PRODUCT_MAXIMUM_QUALITY_AUDIT_2026-08-31.md`.
- Status: Iteration 1 discovery verified; no release claim.

### VER-20260831-006 — Fail-closed restore recovery gate

- Scope/environment: Application-wide startup/live-restore admission and drain behavior; recovery marker lifetime and rollback; WorkManager ordering; workers, receivers, schedulers, ViewModels, widgets, notification actions, generation-scoped SavedState/imports; accessible blocking UI; complete shared JVM and Android suites; and cold-launch fault injection on disposable API 34 emulator `emulator-5554`.
- Commands: focused recovery/editor/widget JVM tests; focused recovery/widget/routine Android tests; `scripts/check`; `ANDROID_SERIAL=emulator-5554 scripts/check --emulator`; merged-manifest inspection; debug APK install and `am start -W`; non-destructive `scripts/device-artifacts capture`/`ui-dump`; explicit corrupt-marker injection/removal inside only the disposable debug app's private storage.
- Result: All 419 JVM tests passed. All 496 Android instrumentation tests ran across eight batches with zero failures and zero skips. Lint, debug build, Android-test compilation, Play assets, and coverage gates passed: deterministic domain lines 81.07% (3,384/4,174), branches 56.38% (2,015/3,574), and core settings/policy 63.62% (397/624). The merged manifest retains AndroidX `InitializationProvider` but contains no `WorkManagerInitializer`. The independent Product Director approved the final boundary after five challenge/revision rounds.
- Fault evidence: Ordinary cold launch completed successfully in 2,047 ms and reached `Welcome to Whip`. With a deliberately corrupt recovery marker, a second cold launch completed in 1,123 ms and exposed only `Whip Couldn't Safely Open Your Data` and `Retry Recovery`; no normal product surface appeared, and the marker remained in app-private storage. Focused tests additionally prove drain/late-denial/cancellation behavior, durable marker-before-generation ordering, failed-restore rebuilding, same-ID action rejection, process-recreated draft/import rejection, post-restore new-import survival, widget reference reconciliation, and blocked configuration saves.
- Counts and exclusions: Current source baseline is 915 product tests—419 JVM and 496 Android—plus 9 Macrobenchmark/Baseline Profile scenarios. The physical phone was not mutated or instrumented for this chunk, and no release is claimed.
- Related: `FND-20260831-007`, `DEC-20260831-008`, `IMP-20260831-004`.
- Status: P0 recovery defect resolved and verified; integrated release remains pending.

### VER-20260831-007 — Exact reminder delivery and mutation-boundary campaign

- Scope/environment: Task/Habit/Goal scheduled and snoozed delivery, action integrity, live eligibility and invalidation, source-backed Habits, quiet-hour rollover, time/time-zone broadcasts, legacy claim upgrade, malformed persisted/work input, repository/settings mutation races, permanent deletion rollback/process interruption, and full repository gates on disposable API 34 emulator `emulator-5554`.
- Commands: focused scheduler/worker/runtime JVM suites; focused Android worker/action/ledger/time/settings/deletion suites; `scripts/check`; and `ANDROID_SERIAL=emulator-5554 scripts/check --emulator`.
- Result: All 451 JVM tests passed. All 516 Android instrumentation tests ran with zero failures and zero skips. Play assets, compilation, lint, debug assembly, Android-test compilation, and coverage gates passed. The first full device attempt crossed the August/September boundary and exposed an unrelated calendar-semantic test that hardcoded `July 2026`; the assertion was changed to derive the prior month from Whip's clock, passed in isolation, and the entire 516-test matrix then passed from a clean restart. The final adversarial QA and independent Product Director reviews approved the implementation after repeated race, lock-order, malformed-data, source-invalidation, settings-lost-update, and cross-system deletion challenges.
- High-risk evidence: Tests prove valid claims post; legacy/malformed/stale claims do not; recurring complete/skip/move and early execution reconcile; exact notification actions are idempotent; mutable performance history does not invalidate definition fingerprints; Task/Habit/Goal production mutations cannot overlap resolve/post; separate Settings instances cannot lose quiet-hour/time-zone state; stale deletion revisions preserve valid notifications; committed deletion interrupted before platform cleanup is recovered from the durable journal; system broadcasts respect fixed versus device zones; and legacy work upgrades only after all rebuilds succeed.
- Counts and exclusions: Current source baseline is 967 product tests—451 JVM and 516 Android—plus 9 Macrobenchmark/Baseline Profile scenarios. No Room migration, backup-format change, signed release, or physical-phone mutation was required for this chunk. API 26/37, release build, and physical-device smoke remain integrated-goal gates rather than claims of this record.
- Related: `FND-20260831-008`, `DEC-20260831-009`, `IMP-20260831-005`.
- Status: P1 reminder-delivery defect resolved and verified; integrated release remains pending.

### VER-20260831-008 — Shared calendar and provenance focused campaign

- Scope/environment: Shared calendar context; same-date zone changes; fixed-zone invalidation; cutoff rollover; cross-domain coherence gate; Track rollover without repository mutation; explicit-zone Search indexing and reactive reindex; Gym default, backdated, copy, and duplicate provenance; Measurement default/explicit zone provenance; disposable API 34 emulator `emulator-5554`.
- Commands: Three Kotlin compilation targets; focused `AppSettingsTest`, `UnifiedSearchRulesTest`, and `LaunchAndHomeLoadPolicyTest`; targeted Android execution of `GymRepositoryTest`, `MeasurementTaxonomyRepositoryTest`, `TrackCalendarContextTest`, and `InteractionControlUiTest`.
- Result: Compilation succeeded. Focused JVM suites passed. All 66 selected Android tests ran with zero failures and zero skips; this includes the new rendered Search zone-change regression and no-write Track rollover regression.
- Compatibility evidence: Repository tests preserve source workout local date/zone while new copies use current Whip provenance. Explicit historical `startedAt` resolves in its chosen zone rather than today. No migration or historical rewrite was introduced.
- Counts and exclusions: Focused verification, not yet the full shared suite. Exact elapsed-Goal DST/editing behavior belongs to the next subchunk; complete JVM/Android gates and Director/QA rereview remain required before resolving `FND-20260831-009`.
- Related: `FND-20260831-009`, `DEC-20260831-010`, `IMP-20260831-006`.
- Status: Focused shared-calendar/provenance behavior verified; integrated release remains pending.

### VER-20260831-009 — Complete live calendar and elapsed-Goal campaign

- Scope/environment: Integrated `FND-20260831-009` shared context, Android invalidation, cross-domain coherence, Search/Track/Gym/Measurement provenance, exact elapsed-Goal time semantics, DST gaps/overlaps, active-zone presentation, narrow/large-text actions, compatibility, and full repository gates on disposable API 34 emulator `emulator-5554`.
- Commands: focused JVM resolver/presentation/calendar/Search/Home-policy suites; focused Android Search/Track/Gym/Measurement/elapsed-Goal suites; `scripts/check`; and `ANDROID_SERIAL=emulator-5554 scripts/check --emulator`.
- Result: All 460 JVM tests passed. Play assets, lint, debug build, Android-test compilation, and coverage floors passed: deterministic domain lines 81.07% (3,384/4,174), branches 56.38% (2,015/3,574), and core settings/policy lines 65.92% (443/672). All 523 Android instrumentation tests ran across nine bounded batches with zero failures and zero skips.
- Regression discovered and corrected: The first full emulator attempt exposed a direct/fold-layout Goal fixture rendering elapsed status from a zero default clock. `GoalUiState` now defaults to a current instant for direct hosts while production state remains injected from `WhipClock`; the isolated fold regression passed, and the entire 523-test campaign was restarted from corrected source and passed.
- High-risk evidence: Tests cover same-date zone changes, explicit invalidation across a cutoff, cross-domain rendering gates, Track rollover without repository mutation, Search reactive zone reindex without query loss, historical/current Gym provenance, explicit historical start instants, Measurement provenance, spring gaps, both fall overlap offsets with no implicit choice, canonical instant preservation, explicit Whip-zone labels, first-draft live validation, polite dynamic-error announcements, and 320dp/200% reset actions constrained to the actual dialog surface with measured 48dp targets.
- Independent challenge: Accessibility review initially rejected a persisted-only clock optimization, an unconstrained dialog test, non-wrapping reset actions, and silent dynamic validation. The implementation and test were amended, the reviewer returned `APPROVE`, and both complete gates were rerun from the amended source.
- Counts and exclusions: Current source baseline is 983 product tests—460 JVM and 523 Android—plus 9 Macrobenchmark/Baseline Profile scenarios. No Room migration, backup-format change, signed release, or physical-phone mutation occurred. Absolute focus/rest timers, physical export/Health windows, and reminder deadlines intentionally remain separate time concepts.
- Related: `FND-20260831-009`, `DEC-20260831-010`, `IMP-20260831-006`, `IMP-20260831-007`.
- Status: P1 live calendar/time-zone consistency resolved and fully verified; integrated physical-device release remains pending.

### VER-20260831-010 — Request-owned productivity definition save campaign

- Scope/environment: Task, Habit, and Goal authored definition editors; request admission/ownership; inline retry; filtered and stale Area reconciliation; repository commit versus derived follow-up; tags/reminders; activity/process restoration; Save-and-New ordering; pointer/Back/hardware-key blocking; deep draft/scroll retention; disposable API 34 emulator `emulator-5554`.
- Commands: focused `AppRuntimeTest` and `AreaScopeTest`; Android-test compilation; focused `EntitySaveCoordinatorUiTest` and `EntitySaveViewModelIntegrationTest`; `scripts/check`; `ANDROID_SERIAL=emulator-5554 scripts/check --emulator`; and `git diff --check`.
- Result: All 464 JVM tests passed. Play assets, compilation, lint, debug assembly, Android-test compilation, and coverage gates passed: deterministic domain lines 81.10% (3,391/4,181), branches 56.42% (2,020/3,580), and core settings/policy lines 67.56% (479/709). All 537 Android instrumentation tests executed across nine bounded batches with zero failures and zero skips. The 14 focused emulator tests passed after the final lifecycle correction.
- High-risk evidence: Tests prove atomic Idle-only admission; rejection while Running or terminal delivery remains unconsumed; exact request-ID settlement; stale terminal reclamation; rotation and process-orphan release; precommit failure versus postcommit warning; committed cancellation receipt; fatal-error propagation; real Room create/update failure behavior; one committed entity despite follow-up failure; authoritative/stale/missing Area routing; request rejection and Save-and-New fresh-session ordering; Back/pointer/hardware-key blocking; polite single error semantics; and retained Task/Habit/Goal drafts after deep-scroll failure.
- Independent challenge: Delivery/data-integrity review accepted the commit/follow-up and reminder/tag boundaries. The Product Director initially rejected Idle-only admission because an editor leaving after ViewModel completion but before terminal consumption could wedge the next editor. The coordinator was amended to reclaim only unowned or mismatched `Finished` outcomes while never adopting `Running`; a focused regression passed. Director and adversarial QA then returned `ACCEPT` with no blockers.
- Compatibility/exclusions: No Room schema, backup format, or historical record changed. Existing Tasks, Habits, Goals, Areas, tags, reminders, and completed history remain untouched. This resolves authored definition editors only; secondary mutation dialogs are tracked in `FND-20260831-019`. No signed release or physical-phone mutation occurred.
- Counts: Current baseline is 1,001 product tests—464 JVM and 537 Android—plus 9 Macrobenchmark/Baseline Profile scenarios.
- Related: `FND-20260831-010`, `FND-20260831-019`, `DEC-20260831-011`, `IMP-20260831-008`.
- Status: P1 authored definition-save integrity resolved, fully verified, committed, and pushed; integrated physical-device release remains pending.

### VER-20260831-011 — Habit secondary-mutation integrity campaign

- Scope/environment: Habit current-total Set, historical log create/edit/delete, scheduled-pause create/edit/delete, current and historical skip undo, child-parent ownership, custom units, Measurement provenance, commit/follow-up cancellation, Home/workspace request ownership, abandoned-terminal reclamation, activity/process restoration, replace-restore generation invalidation, 320dp/200% text, accessibility input blocking, and the complete disposable API 34 emulator suite.
- Commands: Kotlin/Android-test compilation; focused `HabitRepositoryTest`, `ActivityHistoryUiTest`, `EntitySaveCoordinatorUiTest`, and `EntitySaveViewModelIntegrationTest`; `scripts/check`; `ANDROID_SERIAL=emulator-5554 scripts/check --emulator`; and `git diff --check`.
- Result: All 466 JVM tests passed. Play assets, compilation, lint, debug assembly, Android-test compilation, and coverage gates passed: deterministic domain lines 81.10% (3,391/4,181), branches 56.42% (2,020/3,580), and core settings/policy lines 67.88% (486/716). All 561 Android instrumentation tests executed across nine bounded batches with zero failures and zero skips.
- High-risk evidence: Tests prove transactional wrong-parent rejection without mutation; exact repeated/missing skip failure; authoritative current-total reread; custom-unit conversion/no-op behavior; `0.1 + 0.2` versus displayed `0.3` no-op; real `-500` and `+1` corrections at a trillion-unit total; Habit-owned Measurement UUID provenance after edit; exact single request admission; committed cancellation/warnings; no cross-namespace result theft; abandoned Home/workspace reclamation in both directions; retained log/pause editor snapshots after the live child disappears and state restores; generation-change identity invalidation; total-setting language; rapid-tap shielding; and constrained large-text pause actions.
- Regression discovered and corrected: Independent QA, engineering, and Director reviews rejected an initial `1e-9 × magnitude` no-op tolerance because it could erase real changes at high totals. The comparison was replaced with `max(1e-12, 4 × max(ulp(target), ulp(current)))`; high-magnitude and decimal-noise regressions passed, and all three reviewers returned `ACCEPT` before the complete gates ran.
- Compatibility/exclusions: No schema, migration, or backup-format change. No physical-phone mutation or signed release occurred. This resolves only the Habit subset of `FND-20260831-019`; other secondary mutation families and the integrated release remain pending.
- Counts: Current baseline is 1,027 product tests—466 JVM and 561 Android—plus 9 Macrobenchmark/Baseline Profile scenarios.
- Related: `FND-20260831-019`, `DEC-20260831-015`, `IMP-20260831-009`.
- Status: Habit secondary-mutation integrity is fully verified and independently accepted; commit/push is the final chunk handoff and physical-device release remains deferred.

### VER-20260831-012 — Task secondary-mutation and recurring-series campaign

- Scope/environment: Task semantic editor boundaries; create/update and occurrence-aware series splits; same-millisecond conflicts; reschedule/Plan My Day undo; bulk metadata/date/archive; pin; complete/reopen/reset; permanent deletion revisions; notification action ledgers; reminder batch isolation; activity/process restoration; 320dp/200% text; explicit Open recurrence overrides; stable Subtask integration mapping; historical compatibility; disposable API 34 emulator `emulator-5554`.
- Commands: focused JVM `ReminderActionCommitTest`, `ReminderBatchSyncTest`, `TaskMutationCommitTest`, `ReminderSchedulerTest`, and `TaskUpcomingVisibilityTest`; focused Android `TaskRepositoryTest`, `TaskDeletionUiTest`, Task bulk/editor/reminder/notification/accessibility groups; `scripts/check`; `ANDROID_SERIAL=emulator-5554 scripts/check --emulator`; and `git diff --check`.
- Result: All 476 JVM tests passed. Play assets, compilation, lint, debug assembly, Android-test compilation, and coverage gates passed: deterministic domain lines 80.46% (3,392/4,216), branches 56.46% (2,018/3,574), and core settings/policy lines 67.88% (486/716). All 620 Android instrumentation tests executed across nine bounded batches with zero failures and zero skips. The focused Task repository class passed all 47 tests; the corrected Task detail class passed all 7 tests.
- High-risk evidence: Regressions cover saveable exact editor identity and stale rejection; closed-history non-splitting; Once/Anytime future conversion; completion races; stable-ID Subtask reorder/insertion; copied child conditions/choices/mappings; inbound automation retarget; rescheduled/state-only Open migration; Carry Unfinished baselines across multiple occurrences; finite-count remainder; later Goal Link activation; schedule- and completion-anchor UI/reminder reachability; deletion dependent-token invalidation; pre/post-authoritative ordinary/cancellation/fatal notification failures; request-owned bulk failure/success; large-text reachability; and exact complete/reopen/undo predicates.
- Independent challenge: Engineering delivery first rejected state migration and later Link windows; QA then rejected persistence-only state preservation because reminder and completion-anchor consumers omitted explicit Open rows. The Director revised its earlier acceptance after each challenge. The final design unions generated and explicit Open occurrence identities while preserving persisted scheduled dates. Engineering, adversarial QA, and Director all returned `ACCEPT` with no bounded P0/P1 blocker. The first complete emulator attempt also exposed two stale assertions expecting “Edit This and Future” for closed/archived records; they were corrected to the intentionally safe “Edit Series,” the focused class passed, and the entire 620-test campaign was rerun cleanly.
- Compatibility/exclusions: No schema, migration, backup-format, or historical-record rewrite. No physical-phone mutation or signed release occurred. This resolves only the Task subset of `FND-20260831-019`; Goal measurement/reset and remaining Track/Gym secondary flows plus the integrated release remain pending.
- Counts: Current baseline is 1,096 product tests—476 JVM and 620 Android—plus 9 Macrobenchmark/Baseline Profile scenarios.
- Related: `FND-20260831-019`, `DEC-20260831-016`, `IMP-20260831-010`.
- Status: Task secondary-mutation integrity is fully verified and independently accepted; commit/push is the final chunk handoff and physical-device release remains deferred.
