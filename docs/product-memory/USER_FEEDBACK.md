# User feedback and acceptance criteria

These records preserve durable user intent. “Released” means the change reached a device build; it does not erase later user validation or regression findings.

### FB-20260831-001 — Arbitrary lifts in 5/3/1

- User need: Build 5/3/1 around personally selected lifts rather than the traditional four only; Bench Press, Deadlift, and Zercher Squat is the concrete example.
- Acceptance criteria: Any compatible distinct lift can be selected, ordered, assigned a Training Max, programmed, edited, and progressed without requiring four standard lifts.
- Related: `DEC-20260831-001`, `IMP-20260831-001`, `VER-20260831-001`
- Status: Released; awaiting continued user validation.

### FB-20260831-002 — Preserve routine-editor context after adding exercise

- User need: Adding an exercise from a routine/day must return to that exact routine and day, not the Gym library/routines root.
- Acceptance criteria: Selection and Save retain routine identity, day identity, editor state, and clear confirmation.
- Related: `FND-20260831-002`, `IMP-20260831-001`
- Status: Released; awaiting continued user validation.

### FB-20260831-003 — Replace tiny fixed routine exercise pane

- User need: The bottom-right exercise window showed approximately one item and was too small to use. Automatically expanding unrelated routine UI was not an acceptable substitute.
- Acceptance criteria: The exercise/editor content can use the available pane or screen, lists scroll naturally, and opening the routine does not unexpectedly expand unrelated content.
- Related: `FND-20260831-002`, `IMP-20260831-001`
- Status: Released; awaiting continued user validation.

### FB-20260831-004 — Training Max outside the 5/3/1 wizard

- User need: A non-5/3/1 routine using `% Training Max` must provide a discoverable way to set or derive that Training Max.
- Acceptance criteria: The dependency is visible before it is required; explicit and derived sources are configurable; stale hidden values can be cleared; validation explains missing or invalid inputs.
- Related: `FND-20260831-001`, `DEC-20260831-002`, `IMP-20260831-001`
- Status: Released; awaiting continued user validation.

### FB-20260831-005 — Gym-wide control-integrity audit

- User need: Find and fix other controls that expose behavior without a discoverable or valid configuration path.
- Acceptance criteria: Audit the Gym surface, domain logic, persistence, errors, responsive UI, accessibility, and regression coverage for systemic versions of the same failure.
- Related: `FND-20260831-001`, `IMP-20260831-001`
- Status: Implemented and documented in the Gym 5/3/1 audit; broader whole-product recurrence remains in `FB-20260831-012`.

### FB-20260831-006 — Comprehensive Gym usability pass

- User need: Perform and implement another UI, UX, functionality, accessibility, and understandability pass across Gym.
- Acceptance criteria: Major workflows are discoverable, responsive, accessible, semantically correct, and tested rather than merely visually polished.
- Related: `IMP-20260831-001`, `VER-20260831-001`
- Status: Released; awaiting continued user validation.

### FB-20260831-007 — Readiness-adjustable Training Max and cycle suggestions

- User need: Derive Training Max from actual/e1RM with an adjustable percentage, and offer higher, lower, unchanged, or standard next-cycle choices using AMRAP, PR-set, Joker, missed-work, and test evidence.
- Acceptance criteria: Suggestions are conservative, bounded, per lift, transparent, explainable, optional, user-confirmed, and never silently mutate history or progression.
- Related: `DEC-20260831-002`, `DEC-20260831-005`, `IMP-20260831-001`
- Status: Released; awaiting longitudinal user validation.

### FB-20260831-008 — Correct five-minute rest-timer boundary

- User need: Starting a five-minute timer must show 5:00 and then 4:59, never 5:01 then 5:00.
- Acceptance criteria: Display is clamped to the selected duration across fresh deadlines, clock skew, adjustments, expiry, persistence, and resume.
- Related: `FND-20260831-004`, `IMP-20260831-001`
- Status: Released.

### FB-20260831-009 — Add an exercise to only the active workout

- User need: Add an exercise for this workout without modifying the source routine or future workouts.
- Acceptance criteria: The addition is explicitly workout-scoped, remains optional/ad hoc, is persisted in this session and history, and does not affect program-required completion or progression.
- Related: `FND-20260831-005`, `DEC-20260831-004`, `IMP-20260831-001`
- Status: Released; awaiting continued user validation.

### FB-20260831-010 — Joker Sets must be additive

- User need: Enabling a Joker Set must not remove an ending option such as BBB, FSL, SSL, Boring But Strong, or custom supplemental work.
- Acceptance criteria: Joker remains separate optional work; enabling/disabling it preserves exact Main and Supplemental objects, order, count, keys, notes, and rest details.
- Related: `FND-20260831-003`, `DEC-20260831-003`, `IMP-20260831-001`
- Status: Released.

### FB-20260831-011 — Reusable maximum-quality product goal

- User need: A goal that repeatedly audits and improves every app surface for lifters, powerlifters, 5/3/1 users, productivity users, ADHD users, accessibility users, beginners, and advanced customizers.
- Acceptance criteria: The goal audits top-to-bottom, allows justified restructuring, implements rather than reports, retests with user panels, performs adversarial QA, preserves data, and releases only after gates pass.
- Related: `FB-20260831-012`
- Status: Superseded by the repository-backed goal in `MAXIMUM_QUALITY_GOAL.md`.

### FB-20260831-012 — Execute the maximum-quality iteration with durable evidence

- User need: Run repeated whole-product discovery, remediation, focus-group review, QA, and release without losing detail between sessions.
- Acceptance criteria: Invoke the memory protocol throughout; leave every finding, decision, implementation, verification result, residual risk, and next action in the repository.
- Related: `DEC-20260831-006`, `MAXIMUM_QUALITY_GOAL.md`
- Status: Closed by user direction on 2026-09-02 after a two-day run. Substantial remediation and verification landed, but the original exhaustive definition of done and physical-phone release were not completed. Do not resume automatically.

### FB-20260831-013 — Long-term memory outside chat

- User need: Issues, investigations, implementation details, decisions, problems, test evidence, and unresolved work must be written down so chat context is not the sole memory.
- Acceptance criteria: An automatically discoverable skill and workspace fallback require read-before-work and update-before-finish behavior, use stable IDs, preserve history, and distinguish implementation from verification and release.
- Related: `FND-20260831-006`, `DEC-20260831-006`, `IMP-20260831-002`
- Status: Implemented; awaiting validation through future tasks.

### FB-20260831-014 — Commit and push every coherent work chunk

- User need: Related work must be committed and pushed in trackable, revertible chunks instead of accumulating only in the worktree or one final mega-commit.
- Acceptance criteria: A chunk is coherent, independently understandable, proportionately tested, narrowly staged, committed with a focused message, pushed normally to its configured upstream, and verified before unrelated work begins. Unrelated user edits, failing checks, secrets, caches, and transient artifacts are not swept in; blocked pushes are reported rather than misrepresented.
- Related: `DEC-20260831-007`, `IMP-20260831-003`, `VER-20260831-004`
- Status: Implemented and structurally verified; ongoing behavior is enforced for future chunks.

### FB-20260902-001 — Return to a conventional development process

- User need: Stop recursive subagent calls, simulated panels, formal debates, and repeated review loops for each issue. Use a straightforward development process now that the major problems are identified.
- Acceptance criteria: One primary developer verifies the problem, implements the smallest coherent fix, adds proportionate tests, exercises affected UI, records concise durable facts, commits and pushes the chunk, and moves on. Subagents or specialist reviews are used only when the user explicitly requests them.
- Related: `DEC-20260902-001`, `MAXIMUM_QUALITY_GOAL.md`
- Status: Superseded on 2026-09-02 by the user's later clean-slate VERA direction in `FB-20260902-004`; the Whip-specific no-unrequested-subagents override was removed.

### FB-20260902-002 — Install VERA-Codex globally and inside Whip

- User need: Install the provided VERA-Codex agent system in the global Codex configuration and in the Whip repository.
- Acceptance criteria: Preserve existing global integrations and project trust; install the Luna/Terra/Sol role definitions and bounded routing policy in both scopes; keep Whip's explicit conventional-development/no-unrequested-subagents preference; validate configuration loading and model availability; commit and push the repository layer separately.
- Source: `/mnt/c/Users/commv/Documents/Codex/2026-09-02/i-wa/outputs/vera-codex.tar.gz`.
- Related: `DEC-20260902-012`, `IMP-20260902-013`, `VER-20260902-013`.
- Status: Initial adapted installation implemented and verified, then superseded by the archive-exact clean-slate installation in `FB-20260902-004`.

### FB-20260902-003 — Stop and close the long-running Whip mission

- User need: Close out the last active Whip work and terminate the two-day maximum-quality goal even though it is not complete.
- Acceptance criteria: Finish, verify, commit, and push the active coherent change; preserve an honest residual backlog and release state; leave no dirty worktree or running worker; close the goal by user direction without claiming the original definition of done was reached.
- Related: `FB-20260831-012`, `IMP-20260902-012`, `VER-20260902-012`, `VER-20260902-014`, `MAXIMUM_QUALITY_GOAL.md`.
- Status: Implemented on 2026-09-02.

### FB-20260902-004 — Reset orchestration to canonical VERA and publish it independently

- User need: Remove the merged/adapted agent methods, start from a clean orchestration slate, implement only the supplied VERA system, and move VERA into its own repository on the remote.
- Acceptance criteria: Publish the supplied bundle without content changes in a standalone repository; remove the ATIS role/instructions and Whip-specific conventional-development override; make global and Whip VERA policy/config/roles match that canonical source; preserve unrelated Codex authentication, plugin, MCP, notification, trust, and desktop configuration; validate and push every repository change.
- Source: `/mnt/c/Users/commv/Documents/Codex/2026-09-02/i-wa/outputs/vera-codex.tar.gz`.
- Related: `DEC-20260902-013`, `IMP-20260902-014`, `VER-20260902-015`.
- Status: Implemented and verified on 2026-09-02.

### FB-20260902-005 — Release the latest Whip build to the physical phone

- User need: Build, sign, and install the current `origin/main` Whip application on the phone exposed through Android wireless debugging at `192.168.2.187:42785`.
- Acceptance criteria: Give the candidate a unique monotonically increasing release identity; run the complete deterministic release gate; verify package, signer, version, and artifact hash; upgrade with `adb install -r` without clearing user data; launch successfully; and confirm the installed artifact and preserved installation identity. Never run destructive instrumentation or write debug artifacts to unrestricted physical-device storage.
- Related: `DEC-20260902-014`, `IMP-20260902-015`.
- Status: Released and verified on the physical phone on 2026-09-02. See `VER-20260902-016`.

### FB-20260902-006 — Complete the advanced 5/3/1 expansion

- User need: Add a polished, accessible, one-tap path for Leader/Anchor programming and exact 7th Week Deload/TM Test/PR Test presets, plus alternate-lift BBB, multi-Joker ladders with deliberate autoregulation, and automatic assistance-exercise programming.
- Intended behavior: Presets create a fully reviewable multi-cycle program from user-selected main lifts; every generated phase, work role, lift mapping, Joker step, assistance exercise, rep target, Training Max dependency, and progression boundary remains visible and editable before save. Workout execution keeps Main, Supplemental, Assistance, and Optional/Joker work additive and historically immutable.
- Non-goals: Do not lock users to Wendler's standard four lifts; do not silently add optional sets, change a Training Max, invent performance facts, advertise an unofficial configuration as an exact published template, or create a general-purpose programming DSL.
- Affected boundary: Gym routine/program authoring, 5/3/1 prescription generation and validation, persisted routine/program definitions, workout snapshots, cycle review/autoregulation, exercise selection/assistance roles, responsive Compose UI, migrations/backups if required, and JVM/Android regression coverage.
- Risk/approval: High risk because the work crosses programming correctness, progression, persistence/history, and several UI/domain subsystems. VERA requires a read-only Sol plan before implementation, deterministic gates, and a fresh independent Sol review after checks. Repository and emulator changes are authorized; physical-device installation remains a separate explicit release action.
- Acceptance criteria: One-tap presets are truthful and sourced; arbitrary main and alternate lifts work; 7th Week roles have exact validated prescriptions; Joker ladders are optional, bounded, performance-gated, individually loggable, and never replace supplemental work; assistance suggestions use compatible user/library exercises, explain their source, allow replacement/omission, and meet visible category/rep targets; existing routines and completed workouts remain valid; 320dp/200%-text, keyboard/screen-reader semantics, lifecycle, migration/backup, and complete regression gates pass.
- Status: Implemented, fully verified, and independently accepted on 2026-09-02; not installed on a physical phone.
