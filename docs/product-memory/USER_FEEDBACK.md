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

### FB-20260902-007 — Replace compatibility architecture with an explicit clean slate

- User need: Remove transitional, migratory, and obsolete legacy behavior instead of preserving architecture that exists only for old data. The next breaking update may require existing users to erase local data.
- Acceptance criteria: Define one canonical current schema/model; distinguish true compatibility code from current product concepts that happen to have legacy names; show an explicit destructive reset boundary; atomically clear database, preferences, work, reminders, widgets, caches, and private recovery journals; reject old backups clearly; and remove obsolete migrations/adapters/tests without weakening current integrity or fail-closed behavior.
- Risk/approval: Critical and intentionally destructive. The user explicitly authorizes a forced data reset on update; physical-device deployment remains separately gated.
- Status: Implemented, complete-candidate verified, and released as 0.3.37/code 43. The installed app is waiting at the explicit destructive fresh-start confirmation; no erase was performed automatically.

### FB-20260902-008 — Complete the residual whole-product audit backlog

- User need: Finish the remaining findings preserved when the two-day maximum-quality mission was stopped.
- Acceptance criteria: Reconcile the durable ledgers against current code, enumerate the concrete unresolved members of the two partial umbrella findings, retire stale backlog wording, implement real remaining gaps in coherent prioritized chunks, and finish with one cross-product acceptance campaign.
- Status: Implemented and complete-candidate verified in `VER-20260902-031`; released to the physical phone in `VER-20260903-001`.

### FB-20260902-009 — Make UI/UX gap discovery and QA fast and consistent

- User need: Spend less development time repeatedly running the entire suite; quickly identify and implement UI, UX, usability, accessibility, and visual-consistency fixes across every route/dialog/state.
- Acceptance criteria: Use a complete surface inventory, one canonical design contract, shared-code outlier scans, screen-family audits, evidence/priority scoring, exact targeted tests during development, signature-aware instrumentation batch reuse, and only one full gate after source freezes.
- Plan: `docs/quality/UI_UX_REMEDIATION_PLAN.md`.
- Status: Plan executed, all confirmed gaps in the run implemented, complete candidate verified in `VER-20260902-031`, and released in `VER-20260903-001`.

### FB-20260903-001 — Release schema-43 Whip to the phone

- User need: Install the latest fully verified Whip release on the physical phone exposed through Android wireless debugging at `192.168.2.187:44401`.
- Acceptance criteria: Select the exact physical endpoint despite the connected emulator; rerun the guarded release gate; build with the established Whip release key; install with `adb install -r` without clearing data; verify version, signer, artifact hash, installation identity, cold launch, foreground activity, fatal/database logs, and the explicit clean-start boundary.
- Destructive boundary: Installation and launch are authorized. The in-app “Erase all Whip data” confirmation remains a separate destructive user action and must not be pressed during release verification.
- Related: `DEC-20260903-001`, `IMP-20260903-001`, `VER-20260903-001`.
- Status: Released and verified on 2026-09-03; the phone is waiting at the explicit fresh-start confirmation.

### FB-20260903-002 — Preserve Performance review when editing a saved 5/3/1 routine

- User need: A 5/3/1 routine created and saved with Performance review must still show Performance review when reopened for editing instead of reverting to Standard 5/3/1 progression.
- Acceptance criteria: Routine editing round-trips both the saved progression mode and its higher-suggestion policy; opening and saving an existing routine never substitutes new-routine defaults for persisted program choices.
- Related: `FND-20260903-001`, `DEC-20260903-002`, `IMP-20260903-002`, `VER-20260903-002`.
- Status: Implemented, verified, and released to the physical phone in 0.3.38/code 44.

### FB-20260903-003 — Repair every remaining Routine edit-projection omission and release it

- User need: Fix the additional Gym Routine values found by the follow-up audit, then release the corrected build to the connected phone.
- Acceptance criteria: Editing round-trips Training Max basis kind/value/unit and per-lift increase eligibility; the regression uses non-default values; the focused 5/3/1 and complete release gates pass; the signed package is installed without automated data erasure and verified on the explicit physical endpoint.
- Related: `FND-20260903-002`, `DEC-20260903-003`, `IMP-20260903-003`, `VER-20260903-003`.
- Status: Implemented, verified, and released to the physical phone in `VER-20260903-004`.

### FB-20260903-004 — Audit and repair Gym data integrity, then release

- User need: Move outward from the Routine edit defect, inspect Gym data behavior across the subsystem, fix every concrete error found, and release the corrected build to the connected phone.
- Acceptance criteria: Derived records honor historical exercise policy and user settings; discarded/archived work cannot remain in records; copied workouts cannot inherit timer or progression-invalidity state; retained machine history remains interpretable after profile deletion; routine edits/deletion leave no dangling day references; graph presets cannot persist dangling/unsupported definitions; weekly PR attribution follows included workout identity/local date; backup/schema contracts match the corrected model; focused and broader Gym tests pass before a signed in-place phone deployment.
- Destructive boundary: The user previously authorized a current-only clean slate. This correction therefore establishes a new explicit schema/data epoch instead of adding a compatibility migration; deployment must not press the on-device erase confirmation automatically.
- Related: `FND-20260903-003`, `FND-20260903-004`, `DEC-20260903-004`, `IMP-20260903-005`, `VER-20260903-005`.
- Status: Implemented, verified, and released to the physical phone in Whip 0.3.39/code 45; see `VER-20260903-006`.

### FB-20260903-005 — Audit and repair data integrity across the rest of Whip, then release

- User need: Apply the same outward data audit used for Gym to Tasks, Habits, Goals, Tracks, measurements, taxonomy, backup/recovery, Settings, and cross-feature state; fix confirmed errors and release the result to the connected phone.
- Acceptance criteria: Authored inputs are rejected rather than silently reinterpreted; removed definitions cannot rewrite history; source and ownership references are valid; backup data is semantically valid before mutation; taxonomy changes update revision/search projections; active focus state cannot outlive its Task; mutation races serialize; regressions and the complete release gate pass before an in-place physical deployment.
- Destructive boundary: Current-only schema/data-epoch policy remains in force. Deployment must preserve Android installation identity and must not clear phone data or confirm Whip's fresh-start action for the user.
- Related: `FND-20260903-006` through `FND-20260903-009`, `DEC-20260903-005`, `IMP-20260903-007`, `IMP-20260903-008`, `VER-20260903-007`, `VER-20260903-008`.
- Status: Implemented, verified, pushed, and released to the physical phone as Whip 0.3.40/code 46.

### FB-20260903-006 — Refine Gym, Routine, and 5/3/1 authoring UX and release it

- User need: Perform another focused UI/UX pass across Gym Routines and the 5/3/1 builder, implement the concrete fixes, and release the finished build to the physical phone.
- Acceptance criteria: 5/3/1 setup uses the available phone/fold pane instead of a constrained alert; arbitrary user-defined lifts remain searchable and can be created in context; build blockers and selected presets are understandable without color alone; program-wide Training Max and phase controls are reachable without excessive scrolling; structured Main/Supplemental work is not casually rewritten by generic routine tools; numeric inputs use appropriate keyboards; routine-library chrome avoids controls with no current value; focused and broader Gym regression profiles pass before a signed in-place phone install.
- Destructive boundary: Deployment must preserve Android installation identity and must not clear phone data or run instrumentation on the physical device.
- Related: `FND-20260903-010` through `FND-20260903-012`, `DEC-20260903-006`, `IMP-20260903-009`, `IMP-20260903-010`, `VER-20260903-009`, `VER-20260903-010`.
- Status: Implemented, verified, pushed, and released to the physical phone as Whip 0.3.41/code 47.

### FB-20260903-007 — Unify Gym lift search/creation and keep 5/3/1 creation repeatable

- User need: “Add lift” should behave consistently throughout Gym: users can search, receive an actionable create prompt when nothing matches, and always access an explicit Create Lift/Exercise action. A new 5/3/1 user must be able to create and add several lifts successively without leaving the routine/program builder.
- Acceptance criteria: Shared picker presentation and semantics are reused by active-workout and 5/3/1 selection; a no-result query can seed the new Exercise name; contextual creation returns to the owning workflow; custom 5/3/1 always retains Add another lift even when every current library lift is already selected; duplicate lift selection remains prevented; focused and broader Gym regressions pass before an in-place signed release.
- Destructive boundary: Preserve Android installation identity and user data; do not run instrumentation on the physical phone.
- Related: `FND-20260903-013`, `FND-20260903-014`, `DEC-20260903-007`, `IMP-20260903-011`, `IMP-20260903-012`, `VER-20260903-011`, `VER-20260903-012`.
- Status: Implemented, verified, pushed, and released to the physical phone as Whip 0.3.42/code 48.

### FB-20260903-008 — Remove numeric Quick Buttons from checklist Habits

- User need: Checklist Habit creation must not show “Quick Buttons,” preset numeric adds, or range generation because those amounts have no relationship to checking named items.
- Acceptance criteria: Switching a Habit from Count to Checklist immediately removes Quick increment and the complete preset/range builder; saving cannot be blocked by stale quick-add text; the resulting Checklist persists no numeric presets; manual Count and Decimal Habits retain the feature.
- Related: `FND-20260903-015`, `DEC-20260903-008`, `IMP-20260903-013`, `VER-20260903-013`.
- Status: Implemented, verified, and released to the physical phone in Whip 0.3.43/code 49; see `VER-20260903-015`.

### FB-20260903-009 — Make every conditional control match the active configuration

- User need: Audit Whip top to bottom so settings, toggles, flags, and selected types expose only controls and dialogs that can actually affect the configured item; fix any UI, UX, design, or functional defect found and release the result to the phone.
- Acceptance criteria: Hidden fields cannot block saves or leak stale data; Habit target/schedule semantics are correct; Goal windows match Goal types; Gym exercise/machine options and Progress metrics match their input contracts; Settings expose available capabilities and omit unavailable subordinate controls; domain, repository, and UI tests agree; release without physical-device instrumentation or automatic data erasure.
- Related: `FND-20260903-016` through `FND-20260903-018`, `DEC-20260903-009`, `IMP-20260903-014`, `VER-20260903-014`.
- Status: Implemented, verified, and released to the physical phone as Whip 0.3.43/code 49; see `VER-20260903-015`.

### FB-20260903-010 — Make VERA-Codex automatic for development

- User need: VERA-Codex must be the standing default for development work; the user should not have to request it for each task.
- Acceptance criteria: Every software change is automatically classified and routed under VERA; required VERA agents and risk reviews may be spawned without a repeated prompt; trivial direct-parent work remains an explicit VERA route; unrelated conversational work is not over-orchestrated; destructive, credential, production, billing, publication, and external-action approval boundaries remain unchanged; canonical, global, and Whip policies agree.
- Related: `DEC-20260903-010`, `IMP-20260903-016`, `VER-20260903-016`.
- Status: Implemented globally, in Whip, and in canonical VERA-Codex source.

### FB-20260903-011 — Make shared Gym interactions and surfaces visibly consistent

- User need: Machine Profile's Linked Exercises flow must follow the established Gym search/create pattern instead of appearing in a tiny fixed dialog, and the active-workout Rest card must match the formatting and radius used by Home/context cards. Shared functionality, design elements, and components should be the default so the same interaction cannot drift across screens.
- Acceptance criteria: Machine linking uses the complete pane, shared search semantics, result count, actionable no-results creation, a permanent Create action when creation is supported, and a trimmed query seed; unavailable creation never renders a no-op control; creating an Exercise from a Routine's advanced Machine flow returns to and preserves both drafts and auto-links the new Exercise; Rest has one canonical surface owner with the same color/elevation/medium shape contract as Home collection cards, tokenized spacing/type, responsive actions, and accessible ready/running state.
- Scope boundary: Prefer bounded reusable Exercise-picker and collection-card primitives; do not introduce a generic UI DSL or indiscriminately replace intentional structural/chart surfaces.
- Related: `FND-20260903-019`, `FND-20260903-020`, `DEC-20260903-011`, `IMP-20260903-017`, `VER-20260903-017`.
- Status: Implemented, focused-emulator verified, independently accepted, committed, and pushed; not yet included in a physical-phone release.
