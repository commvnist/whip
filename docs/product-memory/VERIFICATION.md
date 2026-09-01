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
