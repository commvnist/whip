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
- Status: Implemented, focused-emulator verified, independently accepted, committed, pushed, and released to the physical phone in Whip 0.3.44/code 50; see `VER-20260903-019`.

### FB-20260903-012 — Make fresh inputs start from the semantically easiest useful value

- User need: A fresh set for a numbered Machine marked “Higher number = less resistance” should start at its maximum configured level because that is the lightest setting. Apply the same kind of sensible, context-aware defaulting across Whip instead of making users repeatedly correct mechanically chosen values.
- Acceptance criteria: Fresh Level sets and blank Routine templates respect machine direction; explicit input, same-placement work, and exact Exercise/Profile history remain higher-priority than a configured endpoint; archived profiles remain valid for already assigned workouts and Routines; no authored prescription or completed history is rewritten. Confirmed analogous defects in fresh Track Number fields, the reach-weight Goal template, and reminder creation are corrected without overriding existing drafts.
- Scope boundary: Add small domain-specific default resolvers at established ownership boundaries; do not create a generic heuristic engine, invent workout performance fields, alter schema, or deploy to a phone without a separate release request.
- Related: `FND-20260903-021`, `FND-20260903-022`, `DEC-20260903-012`, `IMP-20260903-018`, `VER-20260903-018`.
- Status: Implemented, targeted-emulator verified, independently accepted, committed, pushed, and released to the physical phone in Whip 0.3.44/code 50; see `VER-20260903-019`.

### FB-20260903-013 — Release the semantic-default candidate to the phone

- User need: Release the latest verified Whip candidate, including shared Gym UI and semantic-default remediation, to the connected physical phone.
- Acceptance criteria: Assign a new version/code, run Whip's complete guarded release build, produce correctly signed APK/AAB artifacts, install in place on the explicit Samsung endpoint, verify local/installed artifact identity, preserve Android installation identity and user data, cold-launch successfully, and observe no Whip/runtime/database fatality. Do not run instrumentation, clear data, or confirm the app's fresh-start action on the phone.
- Related: `FB-20260903-011`, `FB-20260903-012`, `IMP-20260903-019`, `VER-20260903-019`.
- Status: Released and physically verified as Whip 0.3.44/code 50.

### FB-20260903-014 — Use Exercise as Gym's single user-facing movement term

- User need: Replace the competing Lift/Lifts vocabulary across Gym with Exercise/Exercises so an action never says “Create a Lift” and then opens “Create Exercise.”
- Acceptance criteria: Routine and 5/3/1 setup, Training Max guidance, workout states, Settings, accessibility labels, and surfaced validation all use Exercise consistently; actual exercise names such as Deadlift remain intact; stored identities and existing user data require no migration.
- Related: `FND-20260903-023`, `DEC-20260903-013`, `IMP-20260903-020`, `VER-20260903-020`.
- Status: Implemented, focused-emulator verified, independently accepted, committed, pushed, and released in Whip 0.3.45/code 51; see `VER-20260903-023`.

### FB-20260903-015 — Do not label a selected 5/3/1 Exercise as an alternative

- Date/source: 2026-09-03, direct new-user report from Whip 0.3.45.
- User need: When creating a new 5/3/1 routine and selecting Flat Barbell Bench Press for Exercise 1, the picker must present it as the selected programmed Exercise—not label it “Planned alternative.”
- Acceptance criteria: Preferred picker ordering must not imply a domain role; the 5/3/1 picker uses neutral/current-selection presentation; an Exercise created from an empty library can become Exercise 1 without alternative wording; generated work remains Main work with no substitute IDs; actual routine substitutes are explicitly optional and configured separately; active-workout substitution may still identify preferred substitutes in its own context.
- Affected users/workflows: New 5/3/1 users, empty-library creation, custom Exercise selection, active-workout substitution, screen-reader users.
- Related: `FND-20260903-025`, `IMP-20260903-024`, `VER-20260903-024`.
- Status: Implemented in `ad2f3a9`, verified, independently accepted, and released to the physical phone in Whip 0.3.46/code 52; awaiting real-user validation. See `VER-20260903-025`.

### FB-20260903-016 — Replace compact and comfortable item modes with one balanced layout

- Date/source: 2026-09-03, direct user UX/UI feedback.
- User need: Compact mode feels under-padded, while maintaining a separate comfortable mode adds an unnecessary choice. Tasks, Habits, Goals, and Tracks should use one default presentation that lands between the two current densities.
- Acceptance criteria: Remove the compact-row setting and its stored/backup representation as a clean cut; use one consistent collection-row interaction and density across supported productivity surfaces; retain one-tap primary actions, complete information through clear disclosure, 48 dp targets, two-line titles, large-text/narrow-width reachability, reorder/selection behavior, and adaptive Track master-pane behavior; choose spacing and card shape that feel meaningfully less cramped than current compact rows without returning to the oversized standard cards. No compatibility or transitional behavior is required; a data reset is authorized if genuinely necessary.
- Affected users/workflows: Appearance settings; Home, Tasks, Habits, Goals, and Tracks collections; restored/backed-up installations; compact phones, Fold panes, and enlarged text.
- Related: `FND-20260903-026`, `DEC-20260903-014`, `IMP-20260903-026`, `VER-20260903-026`.
- Notes: The user explicitly superseded the initial compatibility assumption after the first design gate: remove the legacy preference/backup field completely rather than leaving inert state.
- Status: Implemented in `b6e3ef2`, automated/visual QA verified, independently accepted, committed, and pushed to `origin/main`; awaiting user validation.

### FB-20260903-017 — Re-architect reusable UI components across Whip

- Date/source: 2026-09-03, direct user request and active implementation goal.
- User need: Perform another whole-product UX/UI/design pass, identify every component or module that should become a reusable piece, and carry out the rearchitecture rather than stopping at an audit.
- Acceptance criteria: Inventory every Compose screen/component family and its current reuse/duplication boundaries; identify concrete visually or behaviorally duplicated primitives; separately identify and remediate UX/UI/design issues and inconsistencies in hierarchy, spacing, semantics, discoverability, accessibility, responsive behavior, and workflow context even where reuse is not the right solution; distinguish legitimate domain-specific composition from accidental one-off implementations; prioritize and implement reusable components that materially improve coherence and maintenance; remove obsolete duplicates and update callers/tests/documentation; visually exercise affected screen families; run proportionate full regression and independent high-risk review. A universal UI DSL, abstraction for abstraction's sake, local-defect omission, and unverified report-only completion are not acceptable.
- Affected users/workflows: Whole product—Home, Tasks, Habits, Goals, Tracks, Gym, Settings, onboarding, search/review, editors, pickers, dialogs, collection states, adaptive layouts, widgets, and accessibility users.
- Related: Existing bounded-reuse decision `DEC-20260903-011`; `FND-20260903-027` through `FND-20260903-033`; `DEC-20260903-015`; `IMP-20260903-027`; `VER-20260903-027`.
- Notes: Destructive data changes are authorized if genuinely required, but this does not authorize physical-device reset, release/deployment, or unrelated destructive operations. Current evidence must justify any data change before it is used.
- Status: Implemented across `b7222de` through `a65889f`, fully regression-tested, independently accepted, committed, and pushed to `origin/main`; awaiting user validation.

### FB-20260904-001 — Clean-replace active VERA-Codex globally and in Whip

- Date/source: 2026-09-04, direct user instruction.
- User need: Install the exact current working-tree VERA-Codex active bundle globally in both personal Codex homes and clean-replace the older Whip routing layer.
- Acceptance criteria: Every non-trivial software development, diagnosis, testing, and code-review request automatically invokes `$vera-codex`; the only supported roles are Terra scout/low, Terra builder/medium, Terra reviewer/high, Sol architect/xhigh, and Sol critical builder/xhigh; no active Luna or standalone routing policy remains; Whip receives byte-exact source `AGENTS.md`, `.codex/config.toml`, role files, and skill files; global configs change only the seven approved VERA keys while every unrelated personal setting and Whip trust entry remains exact; recoverable backups and executable validation cover all three scopes.
- Clean-cut constraints: Do not merge or rephrase the old prompt, retain Luna, commit, push, install a plugin, change application source/tests/device/data, use credentials, deploy, publish, or edit unrelated configuration.
- Related: `FB-20260903-010`, `IMP-20260904-001`, `VER-20260904-001`.
- Status: Implemented, deterministically verified, and independently accepted; remains local, uncommitted, and unpushed.

### FB-20260904-002 — Top-to-bottom product quality pass

- Date/source: 2026-09-04, direct user request using `/goal`.
- User need: Make Whip clear, intuitive, appealing, engaging, flawless, and bug free through a top-to-bottom UX, UI, design, and QA pass grounded in foundational product principles.
- Acceptance criteria: Independently re-inventory the entire currently implemented product after the previous UI convergence pass; identify and remediate evidence-backed usability, visual-hierarchy, interaction, accessibility, responsive-layout, copy, state, and defect issues; reuse components only where semantic ownership is genuinely shared; exercise changed workflows visually and with automation; pass proportionate targeted checks, the full deterministic product gate, and fresh high-risk review.
- Affected users/workflows: All Whip users across navigation, Home, productivity, Gym, Settings, Review, onboarding, search, editors, dialogs, widgets, external activities, compact/foldable layouts, enlarged text, keyboard, RTL, and assistive technologies.
- Related: `FB-20260903-017`, `DEC-20260903-015`, `DEC-20260904-001`, `FND-20260904-001` through `FND-20260904-002`, `IMP-20260904-002`, and `VER-20260904-002`.
- Status: Verified and independently accepted; awaiting user validation.
- Notes: This authorizes local, reversible implementation and verification only. It does not authorize a physical-device reset, release/deployment, credentials, publication, or other external/destructive action.

### FB-20260904-005 — Overhaul testing philosophy and system for faster change release

- Date/source: 2026-09-04, direct user instruction.
- User need: Reduce the time required to release a change by overhauling Whip's testing philosophy and the supporting test system, after first preserving the current VERA-Codex work as named remote commits.
- Acceptance criteria: Define and implement an evidence-based, proportionate test strategy that keeps correctness, regression confidence, coverage accounting, and emulator-only safety intact while materially shortening the normal change-to-release feedback path; make the required development, candidate, and release gates explicit and executable; avoid redundant broad reruns when trustworthy scoped evidence is sufficient; retain an intentional complete/fresh gate for frozen release candidates; update durable testing documentation and regression coverage for the routing rules.
- Non-goals/constraints: Do not weaken or bypass deterministic quality checks, allow physical-device instrumentation, claim release/deployment, alter user data/schema, disclose credentials, or make unrelated application changes. Local reversible edits and verification are authorized; future push/release/deployment actions require their own explicit approval.
- Affected users/workflows: Whip contributors' development, test selection, candidate qualification, release evidence, and Android emulator QA workflows.
- Related: `FB-20260904-004`, `DEC-20260904-003`, `IMP-20260904-004`, `VER-20260904-004`, `IMP-20260904-003`, and `VER-20260904-003`.
- Status: Blocked after a second fresh Sol review found release-flow and candidate-evidence contract regressions; no real fresh emulator candidate or release was run. See `VER-20260904-004`.

### FB-20260904-006 — Resume the bounded testing-system compatibility recovery

- Date/source: 2026-09-04, direct user instruction after reviewing the blocked testing-system overhaul.
- User need: Complete the approved recovery without erasing the failed-review history: retain the historical device-independent meaning of `scripts/check --full`, keep `scripts/candidate` as the explicit emulator-only fresh authority, and make retained evidence independently enforce the manifest and release-metadata safety claims.
- Acceptance criteria: `scripts/check --full` must run complete JVM coverage, Android-test compilation, lint/static guards, debug/release APK, release AAB, benchmark builds, merged-manifest safety, and release application/version validation without requiring `ANDROID_SERIAL` or invoking candidate/instrumentation. Candidate evidence must require and checksum retained merged-manifest and release-output metadata, then reject missing, tampered, location-permission, wrong-application-ID, and wrong-version evidence even after checksums are rewritten. Deterministic fixtures, shell/help/diff checks, unchanged `scripts/device`, and an environment-unset real `scripts/check --full` must pass.
- Non-goals/constraints: Do not run a real emulator or physical-device command, create a release candidate, sign/install/deploy/release, change application code/data/schema, or erase the prior NO-GO evidence. Preserve concurrent work and limit edits to the testing harness, testing documentation, and product-memory ledgers.
- Related: `FB-20260904-005`, `DEC-20260904-003`, `IMP-20260904-004`, `VER-20260904-004`, and `VER-20260904-005`.
- Status: Implemented and locally verified; awaiting the required fresh final review. No emulator candidate, physical-device action, release, install, or deployment was run.

### FB-20260904-003 — Fail-closed device QA and physical release

- Date/source: 2026-09-04, direct user instruction.
- User need: Fix the P0 QA-device safety defect, then release the verified UX/UI candidate to the user's phone.
- Acceptance criteria: Any direct or scripted Android instrumentation command must fail before test execution unless explicitly pinned to a connected emulator; physical devices must be rejected as instrumentation targets; the guard needs deterministic regression coverage and must preserve supported emulator QA. After full release-candidate checks and fresh independent review, build the next signed release, install it in place on the explicitly selected phone without clearing data, launch it, and verify package/version/process and a bounded smoke journey.
- Affected users/workflows: Whip developers running Android QA and the user's installed Whip release/data.
- Related: `FND-20260904-002`, `DEC-20260904-002`, `IMP-20260904-003`, and `VER-20260904-003`.
- Status: Released as Whip 0.3.47/code 53; the fail-closed guard, full candidate evidence, signed in-place install, and cold-launch smoke passed. Awaiting user validation.
- Notes: This message explicitly authorizes the final signed build, in-place install, launch, and smoke check on the user's phone. It does not authorize data reset, uninstall, credential disclosure, store publication, or unrelated device changes.

### FB-20260904-004 — Proportional exact-signature Android QA

- Date/source: 2026-09-04, direct user instruction after redundant fresh reruns added roughly 40 minutes to small changes.
- User need: Small, localized changes must not trigger the complete fresh Android suite repeatedly when exact, trustworthy evidence can identify the affected test batch.
- Acceptance criteria: The ordinary emulator gate reuses only successful results whose source, test-class set, APK, runner, and device signatures still match; a localized test-only change reruns only affected batches; product/runtime or cache-integrity changes invalidate the relevant evidence; explicitly requested fresh coverage still uses isolated complete processes and exact accounting.
- Related: `DEC-20260904-002`, `IMP-20260904-003`, and `VER-20260904-003`.
- Status: Implemented and verified. The final localized repair reran only its affected batch before all 923 tests were accepted with zero failures or skips.
