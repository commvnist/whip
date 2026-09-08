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
- Status: Superseded by `FB-20260904-008`; this entry remains the historical record of the prior working-tree installation.

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

### FB-20260904-007 — Build a Play Store bundle and release the latest source to the phone

- Date/source: 2026-09-04, direct user instruction.
- User need: Produce a signed Android App Bundle suitable for Play Store upload and release the latest Whip source to the connected Samsung phone.
- Acceptance criteria: Advance the Android release version/code; build and verify signed APK/AAB artifacts from the pushed current source; preserve the complete local release quality gate; install the APK in place only on the explicitly selected physical phone, without uninstalling or clearing data; verify installed package/version/artifact identity; cold-launch and perform bounded process/log smoke verification; make the AAB available locally for Play Store upload.
- Non-goals/constraints: Do not publish to the Play Store, clear/reset data, uninstall, downgrade, run instrumentation on the phone, disclose signing credentials, or target any unselected device. The user authorizes the signed build, AAB generation, in-place phone install, launch, smoke check, commit, and push of necessary release-version/memory records.
- Execution constraint (2026-09-04): Continue autonomously through authorized recovery, qualification, signing, installation, and verification until this release is complete. Do not return control merely because an intermediate check is running or a repair/requalification cycle remains actionable; report only a genuine external blocker or the completed release.
- Affected users/workflows: Whip Play Store distribution and the installed Samsung release while preserving existing user data.
- Related: `FB-20260904-003`, `FB-20260904-005`, `FB-20260904-006`, `VER-20260904-003`, and `VER-20260904-005`.
- Status: Released as Whip 0.3.49/code 55; signed AAB retained locally and in-place phone installation verified. Awaiting user validation.

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

### FB-20260904-008 — Install pinned public VERA-Codex as a clean cut

- Date/source: 2026-09-04, direct user request with an approved critical Sol plan.
- User need: Install the exact latest publicly pushed VERA-Codex revision `e80d2cbd5e92c602c0f2d0db68aa9a1f239eec8d` across Whip and both personal Codex homes, replacing the prior active skill body cleanly rather than merging prompts.
- Acceptance criteria: Require clean `HEAD == origin/main` identities for canonical VERA and Whip before mutation; keep the active runtime boundary to source `AGENTS.md`, project config, five roles, skill body, and skill metadata; make exactly four runtime deltas—the three stale skill bodies and the Windows personal top-level model from Sol to Terra; retain byte-exact roles, metadata, Whip instructions/config, approved global instruction-path adaptation, root config, and all unrelated personal-config semantics; keep Luna and standalone routing-policy paths absent; create recoverable owner-only backups; pass the 5-role/43-case validator, exact mapping/hash/TOML/stale-policy/Doctor/diff/scope gates; and leave Whip with only the project skill plus four memory-ledger changes.
- Affected users/workflows: All subsequent non-trivial Whip and personal Codex software implementation, diagnosis, testing, and review routing.
- Related: `FB-20260904-001`, `IMP-20260904-005`, `VER-20260904-006`.
- Status: Verified locally and installed as requested; remote publication remains pending separate approval.
- Notes: Continue automatically through reversible local installation and verification without pausing for intermediate approval. Do not merge or preserve the old prompt logic. No commit, push, release, deployment, publication, credential use, plugin installation, device/emulator command, or application/test/harness edit is authorized; remote publication remains a separate explicit-approval boundary.

### FB-20260904-009 — Make VERA-Codex persist autonomously to task completion

- Date/source: 2026-09-04, direct user instruction.
- User need: VERA-Codex must complete actionable tasks autonomously rather than exiting for intermediate progress or waiting for optional responses; update canonical policy first and then bring the validated policy to Whip.
- Acceptance criteria: The canonical skill states a task-completion persistence rule; it continues through safe, reversible, in-scope investigation, repair, validation, and review; it treats progress reports as nonterminal; it makes conservative reversible assumptions when optional context is absent; it returns only at accepted completion or a genuine external/required-input/protected-action boundary. The canonical validator enforces the rule, and Whip receives the byte-exact validated skill body with durable evidence.
- Constraints: Preserve all existing non-inference and protected-action boundaries. Do not authorize destructive or irreversible operations, production, credentials/secrets, billing, publication, deployment, or other external effects merely by making the policy autonomous.
- Related: `FB-20260904-008`, `DEC-20260904-004`, `IMP-20260904-007`, `VER-20260904-008`.
- Status: Implemented and verified in canonical VERA-Codex and the Whip project; personal Codex homes remain intentionally outside this synchronization.

### FB-20260904-010 — Roll out and publish the corrected VERA persistence policy

- Date/source: 2026-09-04, direct user instruction after the canonical policy consistency review.
- User need: Repair the contradictory second-Sol wording, then copy the finalized canonical persistence policy into both personal Codex homes and Whip and publish the canonical VERA-Codex and Whip repository changes.
- Acceptance criteria: Canonical skill and README consistently end only the failed Sol retry loop, require a distinct-safe-path scan before blocker reporting, and retain protected-action boundaries; the validator rejects the stale immediate-report wording; both personal-home skill copies and the Whip project skill match the validated canonical policy without changing unrelated configuration; authorized repository changes are committed and pushed normally.
- Result: The two personal-home skill copies and Whip project skill now match canonical VERA; the authorized repository commits and pushes were completed normally.
- Related: `FB-20260904-009`, `DEC-20260904-004`, `IMP-20260904-007`, `IMP-20260904-008`, `VER-20260904-008`, `VER-20260904-009`.
- Status: Implemented and published.

### FB-20260904-011 — Complete UX/UI/design/QA pass and signed in-place release

- Date/source: 2026-09-04, direct user request using `/goal`.
- User need: Audit Whip top-to-bottom, correct every evidence-backed UX/UI/design/QA issue found, then deliver the resulting signed release to the selected phone without clearing its data.
- Acceptance criteria: Audit onboarding, navigation, Home, Tasks, Habits, Goals, Tracks, Gym, Settings, search/review, editors, dialogs, empty/loading/error states, accessibility, responsive behavior, and tests; implement confirmed corrections with focused regression coverage; create a fresh complete emulator candidate and fresh critical review; build a higher signed version and install it in place on the explicitly selected physical phone, verify identity/version/hash/cold launch/log smoke, and retain the Play-ready AAB locally.
- Non-goals/constraints: Do not reset, clear, uninstall, downgrade, run instrumentation on the phone, publish to Play Store, disclose secrets, or perform unrelated device actions. The user expressly authorizes normal commits/pushes needed for this goal and the final signed in-place phone installation.
- Related: `FB-20260904-002`, `FB-20260904-007`, `FND-20260904-003`, `VER-20260904-012`.
- Status: Released as Whip 0.3.49/code 55; awaiting user validation.

### FB-20260906-001 — Make VERA autonomous with a Sol/xhigh default

- Date/source: 2026-09-06, direct user request.
- User need: Make VERA-Codex persist autonomously until development tasks are complete, audit the orchestration for quality, efficiency, and speed, remove the Terra start/default lock, and use `gpt-5.6-sol` at `xhigh` as the parent and fallback default before synchronizing Whip and global Codex.
- Acceptance criteria: The canonical VERA policy treats action requests as instructions to complete all safe, reversible, in-scope work; initiates required bounded subagent routes without another permission prompt; retains executable checks, fresh risk-appropriate review, one-writer ownership, and protected final-action boundaries; configures Sol/xhigh as both parent and unspecified-child default; preserves explicit Terra scout/builder/reviewer routes for lighter work; passes canonical validation, adversarial mutations, skill validation, forward tests, strict Doctors, exact downstream parity, and fresh Sol acceptance reviews; and is committed locally before Whip is synchronized and committed.
- Affected users/workflows: All future VERA-governed development tasks in Whip and both personal Codex homes, after a new Codex session loads the updated instruction chain.
- Related: `FB-20260906-002`, `FB-20260904-009`, `FB-20260904-010`, `DEC-20260904-004`, `DEC-20260906-001`, `IMP-20260906-001`, `VER-20260906-001`.
- Status: Superseded by `FB-20260906-002` at the user's direct request.
- Notes: The user authorized autonomous reversible implementation and local commits. No push, deployment, release, credential use, destructive action, or other protected external effect is authorized. Current runtime-confirmed spawn overrides remain Sol and Terra; the public API guidance names a newer model that this Codex collaboration runtime does not expose, so the explicit `gpt-5.6-sol` target is preserved.

### FB-20260906-002 — Uninstall VERA-Codex completely

- Date/source: 2026-09-06, direct user request.
- User need: Remove VERA-Codex because its standing autonomous orchestration and subagent routing slowed normal Codex work, and return Whip and both personal Codex homes to having no active VERA layer.
- Acceptance criteria: Remove the exact Whip and personal-home VERA instruction, role, skill, metadata, and project-config files; remove VERA-owned model, reasoning, and `[agents]` settings from both personal configs plus the stale Windows trust entry for the deleted VERA checkout; preserve all unrelated configuration and the complete historical memory of earlier installations; verify exact absence, semantic config preservation, strict Codex Doctors, repository scope, and secret hygiene; then make a local Whip commit before deleting the canonical VERA repository and its retained state.
- Affected users/workflows: Future Whip and personal Codex sessions use no standing VERA orchestration policy or custom VERA roles; existing sessions can retain instructions already loaded into their context until restarted.
- Related: `FB-20260906-001`, `DEC-20260906-001`, `IMP-20260906-001`, `VER-20260906-001`.
- Status: Implemented.
- Notes: Preserve unrelated Whip changes and personal settings. Do not touch application, test, build, device, release, credential, plugin, or remote state; do not stage, commit, push, or delete the canonical repository/state until the ordered acceptance boundary is reached.

### FB-20260906-003 — Make prompt-to-release development feedback as fast as possible

- Date/source: 2026-09-06, direct user request.
- User need: Overhaul Whip's development cycle so ordinary changes receive useful test feedback quickly instead of repeatedly paying for the complete suite, while retaining a trustworthy path from prompt to change to test to release.
- Acceptance criteria: Provide one obvious fast default that selects only change-relevant checks; avoid Android-test compilation, lint, packaging, complete profiles, and fresh full-suite work in the edit/test inner loop unless the changed inputs require them; retain explicit affected-emulator, readiness, frozen-candidate, and separately authorized physical-release boundaries; make the command contract discoverable and deterministically tested; preserve fail-closed routing for unknown/high-risk inputs.
- Affected users/workflows: Whip development, automated coding sessions, targeted JVM and Android regression work, candidate qualification, and release preparation.
- Related: `FB-20260902-009`, `DEC-20260904-002`, `DEC-20260904-003`, `DEC-20260906-002`, `IMP-20260906-002`, `VER-20260906-002`.
- Status: Implemented and fixture-verified; fresh candidate qualification remains required before Play Store release because the harness itself changed.
- Notes: This request authorizes development-process and repository-tooling changes, not an application release or physical-device deployment.

### FB-20260906-004 — Release the latest source to the phone

- Date/source: 2026-09-06, direct user request.
- User need: Qualify and install the latest pushed Whip source on the user's physical phone.
- Acceptance criteria: Assign a higher monotonic release identity; use the personal-phone fast affected-check/build path defined by `FB-20260906-005`; build signed APK/AAB artifacts from the latest source; use an explicitly selected connected physical target; install in place without clearing, resetting, uninstalling, downgrading, or running instrumentation on the phone; verify source/artifact/signature/package identity, installed version/hash, preserved first-install identity, cold launch, foreground activity, and bounded fatal/Room/SQLite logs.
- Affected users/workflows: The user's installed production Whip application and future upgrade continuity.
- Related: `FB-20260906-003`, `FB-20260906-005`, `DEC-20260906-003`.
- Status: Released and verified as Whip 0.3.50/code 56; awaiting user validation.
- Notes: Installation and launch are authorized. Play Store publication and any destructive in-app fresh-start confirmation remain out of scope.

### FB-20260906-005 — Reserve complete candidate testing for Play Store releases

- Date/source: 2026-09-06, direct user clarification during the 0.3.50 phone release.
- User need: Treat the user's phone as a personal development installation and keep its release loop fast; do not run the complete candidate suite for ordinary phone updates.
- Acceptance criteria: Personal-phone deployment runs only affected development checks, produces a signed build, installs in place, and performs package/hash/launch/log smoke. Complete fresh JVM/Android coverage, candidate evidence, and store-grade artifact qualification run only for an explicitly requested Play Store release. Preserve explicit physical-device targeting and all no-reset/no-uninstall/no-downgrade protections.
- Affected users/workflows: Owner-only physical-phone development releases versus Play Store publication.
- Related: `FB-20260906-003`, `FB-20260906-004`, `DEC-20260906-003`.
- Status: Implemented, fixture-verified, and exercised by the 0.3.50 physical install.
- Notes: The already-running 0.3.50 candidate was stopped during Android batch 7/11; its partial output is not acceptance evidence.

### FB-20260906-006 — Make the Habit Today detail feel coherent and purposeful

- Date/source: 2026-09-06, direct user feedback with a physical-phone screenshot of the Habit detail pane.
- User need: The Habit Today menu should read as one intentionally designed status surface rather than loose headings, text, database-like facts, and an isolated action.
- Acceptance criteria: Establish a clear current-state focal point; visually group the date and progress metrics; give values a deliberate dashboard hierarchy; integrate the optional manual-duration path; present Skip Today as a clearly secondary action with its consequence; preserve the stable header/tabs/bottom primary action, all Habit state semantics, 48 dp targets, scrolling, narrow widths, and 200% text accessibility.
- Affected users/workflows: Habit Today detail for checklist, check-off, numeric, duration, rating, log-only, paused, skipped, archived, off-schedule, low-pressure, and flexible-schedule states.
- Related: `FND-20260906-001`, `DEC-20260906-004`, `IMP-20260906-005`, `VER-20260906-005`.
- Status: Released in Whip 0.3.51/code 57; awaiting user validation.
- Notes: The screenshot shows Whip 0.3.50 on the user's foldable phone in dark mode.

### FB-20260906-007 — Release the Habit Today redesign to the owner phone

- Date/source: 2026-09-06, direct user request immediately after the focused redesign verification.
- User need: Install the latest pushed Habit Today design on the user's connected development phone for real-device use.
- Acceptance criteria: Advance to a higher monotonic private release identity; use the fast owner-phone lane rather than candidate/full-suite qualification; build signed APK/AAB outputs from pushed source; explicitly target the connected physical phone; install in place without reset, clear, uninstall, downgrade, or phone instrumentation; verify signer, artifact integrity, installed version/hash, preserved first-install identity, cold launch, foreground activity, and bounded fatal/ANR/Room/SQLite logs.
- Affected users/workflows: Owner-only Whip development installation and Habit Today real-device validation.
- Related: `FB-20260906-005`, `FB-20260906-006`, `DEC-20260906-003`, `IMP-20260906-005`, `IMP-20260906-006`, `VER-20260906-005`, `VER-20260906-006`.
- Status: Released and device-verified as Whip 0.3.51/code 57; awaiting user validation.
- Notes: Play Store publication and complete candidate qualification remain out of scope.

### FB-20260906-008 — Systematically capture and raise every Whip surface to one design standard

- Date/source: 2026-09-06, direct user request as a durable completion goal.
- User need: Create a repeatable system that captures every Whip page and dialog, critique the complete UI/UX/design experience, and implement the findings until the app is consistently and purposefully designed throughout.
- Acceptance criteria: Maintain a canonical, machine-checkable inventory of pages, dialogs, sheets, menus, and meaningful states; capture deterministic emulator screenshots and UI hierarchies with explicit coverage accounting; audit hierarchy, density, rhythm, typography, color, components, navigation, action priority, copy, responsiveness, dark/light presentation, and accessibility against one design language; turn every confirmed issue into traceable prioritized findings; implement and recapture all accepted fixes; run focused checks during iteration and the appropriate final release verification; deploy only the completed result to the user's physical phone.
- Affected users/workflows: Every Whip workspace, inspector, editor, dialog, sheet, menu, empty/populated/error state, adaptive layout, and final owner-phone release.
- Related: `FB-20260902-009`, `DEC-20260903-015`, `DEC-20260906-005`, `DEC-20260906-006`, `IMP-20260906-017`, `VER-20260906-017`, `VER-20260906-018`.
- Status: Completed and device-verified in Whip 0.3.52/code 58.
- Notes: The physical phone is explicitly prohibited for discovery, capture, testing, and review; use disposable emulators until the final deployment step. The task is not complete at inventory or critique—the accepted feedback must be implemented and verified.

### FB-20260906-009 — Make elapsed time a configurable first-class Goal display

- Date/source: 2026-09-06, direct user request.
- User need: Count Time Since Goals should celebrate meaningful duration—such as sobriety, recovery, or an anniversary—using the exact time-unit combination the user wants to keep visible, including months, weeks, days, hours, and minutes.
- Acceptance criteria: Let an author choose any non-empty combination of Years, Months, Weeks, Days, Hours, and Minutes or retain Automatic; persist and restore that choice; migrate existing Automatic and single-unit Goals without loss; make the resulting duration the always-visible primary status on Goal collections and consistent in detail, Insights, completion, and reset states; keep the exact event instant authoritative; cover formatting, repository persistence, backup validation/restore, editor interaction, and compact/responsive presentation with focused regression tests; then issue a higher signed in-place release through the fast owner-phone lane.
- Affected users/workflows: Creating and editing Count Time Since Goals, daily Goal browsing, motivational sobriety/recovery counters, detail and Insights review, completed/abandoned snapshots, backup/restore, and owner-phone development releases.
- Related: `FND-20260906-005`, `DEC-20260906-007`, `FB-20260906-005`.
- Status: Implemented, focused-tested, visually accepted, and released in place as Whip 0.3.53/code 59; awaiting real-use feedback.
- Notes: The physical phone remains excluded from development testing and may be used only for the final explicitly requested deployment. Complete Play Store candidate qualification is out of scope.

### FB-20260906-010 — Integrate elapsed counters and remove redundant action confirmations

- Date/source: 2026-09-06, direct real-use feedback after Whip 0.3.53.
- User need: The new Count Time Since display feels bolted onto Goal cards because it is too bold and visually detached. Routine action-result bars such as “Goal saved” are also annoying because the changed screen already confirms what happened.
- Acceptance criteria: Redesign elapsed counters across collection cards, Home, Overview, Insights, terminal states, and editor preview so they use Whip's established information hierarchy, restrained emphasis, coherent spacing, and responsive wrapping in every supported appearance; preserve every configured unit and accessibility label. Remove passive success-only app-action pop-ups across Whip when the committed result is already apparent; retain failures, warnings, recovery/choice prompts, meaningful Undo, and Android system reminders/ongoing notifications.
- Affected users/workflows: Every routine create/edit/log/complete/archive action that currently emits redundant transient success feedback, plus all active and terminal Count Time Since Goal presentations.
- Related: `FB-20260906-009`, `FND-20260906-005`, `FND-20260906-007`, `FND-20260906-008`, `DEC-20260906-007`, `DEC-20260906-008`, `DEC-20260906-009`.
- Status: Implemented, focused-tested, visually accepted, and released in place as Whip 0.3.54/code 60; awaiting real-use feedback.
- Notes: Treat the phone report as authoritative subjective evidence. Development inspection, capture, and regression testing remain emulator-only.

### FB-20260906-011 — Release the integrated counter and quiet-feedback correction

- Date/source: 2026-09-06, direct user request after focused implementation and emulator acceptance.
- User need: Install the corrected Count Time Since presentation and quiet routine-action feedback on the owner's development phone for real use.
- Acceptance criteria: Advance to a higher monotonic private release identity; use the fast owner-phone lane rather than candidate/full-suite qualification; build signed APK/AAB outputs from clean pushed source; explicitly target the connected physical phone; install in place without reset, clear, uninstall, downgrade, or phone instrumentation; verify signer, archive integrity, installed version/hash, preserved first-install identity, cold launch, foreground activity, live process, and bounded fatal/ANR/Room/SQLite/activity-start logs.
- Affected users/workflows: Owner-only development installation, Count Time Since review, and routine app actions across Whip.
- Related: `FB-20260906-010`, `DEC-20260906-003`, `IMP-20260906-020`, `VER-20260906-021`.
- Status: Released and device-verified as Whip 0.3.54/code 60; awaiting user validation.
- Notes: The complete fresh candidate remains reserved for an explicitly requested Play Store release.

### FB-20260906-012 — Cohesive whole-app UX/UI/design overhaul with fast private QA

- Date/source: 2026-09-06, direct user goal.
- User need: Review and improve Whip end to end so its common journeys, navigation, pages, editors, menus, dialogs, sheets, pickers, Settings, and supporting states feel like one polished, intuitive, low-friction product rather than a collection of local interface dialects.
- Acceptance criteria: Re-audit the authoritative current product and its complete surface inventory; improve evidence-backed hierarchy, terminology, discoverability, action priority, spacing, typography, color, component reuse, feedback, responsiveness, and accessibility without flattening legitimate domain-specific interaction; preserve user data and important behavior; implement coherent independently verifiable batches; use focused automated, emulator, visual, semantic, and accessibility feedback during iteration; use the guarded fast non-Play Store owner-phone lane for rapid real-device validation when a releasable product change exists; finish with comprehensive whole-product regression and visual-consistency evidence, resolve discovered issues, and document any residual risk before completion.
- Affected users/workflows: The complete Whip product, including Home, Tasks, Habits, Goals, Tracks, Gym, Settings, onboarding, search/review, inspectors, editors, menus, dialogs, pickers, empty/loading/error/destructive states, adaptive layouts, enlarged text, keyboard, RTL, screen readers, widgets, notifications, and external entry points.
- Related: `FB-20260903-017`, `FB-20260904-002`, `FB-20260906-003`, `FB-20260906-005`, `FB-20260906-008`, `DEC-20260903-015`, `DEC-20260906-003`, `DEC-20260906-005`.
- Status: Completed and released as Whip 0.3.55/code 61. All 172 catalog surfaces are visually accepted, all semantics hierarchies pass the unlabeled-interactive guard, the fresh 1,575-test product matrix has zero failures/skips, and the guarded in-place owner-phone install/cold-launch/log smoke passes. Subjective comfort remains open only to normal real-use feedback.
- Notes: This goal authorizes normal reversible product implementation, proportionate emulator QA, coherent commits/pushes, and guarded private owner-phone releases needed for the design iteration. It does not authorize user-data reset, uninstall, downgrade, physical-device instrumentation, Play Store publication, credential disclosure, or unrelated destructive/external actions.

### FB-20260906-013 — End-to-end Gym and 5/3/1 UX/design/QA analysis and fast release

- Date/source: 2026-09-06, direct user request following the whole-app UX closure.
- User need: Re-examine Gym and the 5/3/1 Routine as one end-to-end workflow and find the same classes of functional UX, visual-design, accessibility, consistency, and QA-evidence defects targeted by the whole-app audit, rather than assuming the prior Gym remediations remain sufficient.
- Acceptance criteria: Trace Gym entry, library and Routine discovery, 5/3/1 setup, Exercise selection/creation, Training Max configuration, generated Program Structure, Routine editing, workout start/execution, optional/Joker and supplemental work, finish/cycle review, progression decisions, History/Progress, recovery, and destructive/supporting states; review the complete Gym catalog family and relevant semantic/adaptive states; implement evidence-backed fixes without flattening strength-program semantics or rewriting owner data; use fast routed/focused JVM, emulator, and Gym-family visual feedback; freeze and push a clean higher-version source; then use the guarded owner-phone lane for an in-place signed release and smoke without phone instrumentation, reset, clear, uninstall, or downgrade.
- Affected users/workflows: General Gym users and novice/advanced 5/3/1 lifters, especially one-handed in-workout use, custom-Exercise libraries, enlarged text, screen readers, lifecycle recovery, and users relying on accurate Training Max/history decisions.
- Related: `FB-20260831-001`, `FB-20260831-006`, `FB-20260831-007`, `FB-20260902-006`, `FB-20260903-006`, `FB-20260903-007`, `FB-20260903-011`, `FB-20260903-014`, `FB-20260903-015`, `DEC-20260903-006`, `DEC-20260903-007`, `VER-20260906-012`, `FB-20260906-012`, `FND-20260906-016` through `FND-20260906-018`, `DEC-20260906-011`, `IMP-20260906-024`, `IMP-20260906-025`, `VER-20260906-025`, `VER-20260906-026`.
- Status: Released and device-verified as Whip 0.3.56/code 62; awaiting normal real-use feedback.
- Notes: The accepted lane used fast private-development QA rather than Play Store candidate qualification. Emulator instrumentation and catalog capture remained isolated to the disposable emulator; the owner-phone install was in place with first-install identity and data preserved, without instrumentation or reset.

### FB-20260907-001 — Let repeated strong AMRAPs inform an optional 5/3/1 increase

- Date/source: 2026-09-07, direct user feedback following review of the existing 5/3/1 suggestion behavior.
- User need: When a lifter records multiple convincingly strong AMRAPs, Whip should be able to offer a cautious above-standard alternative even when RPE/RIR was not recorded, while keeping Jim Wendler's standard fixed Training Max progression intact.
- Acceptance criteria: Keep canonical 5/3/1 Standard fixed at the configured increase; make above-standard guidance explicitly optional and non-standard; require repeated objective evidence from separate successful non-deload workouts; normalize performance for load and cap high-rep inflation; reject thin, stale, inconsistent, failed, deleted, or incomplete evidence; cap rep-only guidance below fully corroborated guidance; never let one AMRAP, Joker, or Training Max test unlock acceleration; explain the evidence and uncertainty in plain language at setup and cycle review; add focused engine/UI regressions; perform code, UX/UI/design, emulator, and visual QA; address confirmed findings; then release a clean higher version in place to the explicitly selected owner phone through the fast non-Play Store lane.
- Affected users/workflows: 5/3/1 Routine setup, logged PR/AMRAP and Joker sets, cycle-boundary progression review, Training Max decisions, audit history, accessibility, and owner-phone validation.
- Related: `FB-20260906-005`, `FB-20260906-013`, `FND-20260907-001`, `FND-20260907-002`, `FND-20260907-003`, `DEC-20260907-001`, `IMP-20260907-001`, `IMP-20260907-002`, `VER-20260907-001`, `VER-20260907-002`.
- Status: Implemented, code-reviewed, emulator-accepted, and released in place as Whip 0.3.57/code 63; awaiting normal real-use feedback.
- Notes: RPE/RIR remains useful corroboration, not a prerequisite for the lower adaptive tier. Suggestions remain advisory and require an explicit user choice; no existing Training Max is rewritten automatically.

### FB-20260907-002 — Re-audit Whip as one cohesive, low-friction product

- Date/source: 2026-09-07, direct user goal following the 0.3.57 adaptive 5/3/1 release.
- User need: Perform a highly critical end-to-end UX/UI/design and consistency review of the current Whip app, challenge existing choices rather than assuming earlier audits remain sufficient, implement well-supported findings, and release the improved product to the owner's phone.
- Acceptance criteria: Re-establish the authoritative current surface and journey inventory; review common flows plus pages, menus, dialogs, editors, forms, and empty/loading/error/supporting states for hierarchy, terminology, component consistency, discoverability, action placement, visual balance, responsiveness, accessibility, and unfinished behavior; preserve intentional domain-specific workflows and user data; implement and code-review confirmed findings in coherent chunks; iterate with focused automated tests, emulator interaction, semantic/accessibility checks, and visual evidence; complete proportionate whole-product acceptance; then commit and push a higher-version signed private build, install it in place on the explicitly selected phone through the fast non-Play Store lane, and verify version/hash, preserved installation identity, cold launch, foreground/live process, and bounded fatal/persistence logs.
- Affected users/workflows: The complete Whip product, including Home, primary navigation, Tasks, Habits, Goals, Tracks, Gym, Settings, Review/search, onboarding and external entry points, common creation/edit/completion paths, recovery/destructive states, compact and enlarged-text layouts, keyboard, screen-reader semantics, and owner-phone use.
- Related: `FB-20260906-012`, `FB-20260907-001`, `FND-20260907-004` through `FND-20260907-012`, `DEC-20260907-002`, `IMP-20260907-003` through `IMP-20260907-007`, `VER-20260907-003` through `VER-20260907-008`.
- Status: Implemented, code-reviewed, whole-product emulator-accepted, and released in place as Whip 0.3.58/code 64; awaiting normal real-use feedback.
- Notes: Earlier audits were used only as context; the accepted evidence is current and fresh where state fidelity matters. Development instrumentation/catalog capture stayed emulator-only. The owner phone was not reset, cleared, uninstalled, downgraded, or instrumented; Play Store candidate qualification remains out of scope.

### FB-20260907-003 — Use up to two emulators for faster Android QA

- Date/source: 2026-09-07, direct user request after discussing emulator-bound QA latency.
- User need: Let Whip use as many as two disposable emulators so independent Android test work can complete faster without weakening the fast-QA safety or evidence model.
- Acceptance criteria: Keep one-emulator commands compatible; make the second emulator explicit and opt-in; reject missing, duplicate, physical, offline, unauthorized, incompatible, or third targets; never instrument the owner phone; build shared APK inputs once; isolate concurrent result, report, coverage, and cache ownership; retain graphics-first and destructive-reset-last boundaries; aggregate every requested class and testcase exactly once with zero failures/skips; preserve fresh/candidate source-drift and atomic-evidence guarantees; add deterministic harness regressions and clear operator documentation.
- Affected users/workflows: Development `check`/`qa-targeted` Android execution, complete emulator compatibility runs, fresh coverage/candidate qualification, and systematic UI-catalog capture where safe parallel work is available.
- Related: `DEC-20260904-003`, `DEC-20260906-003`, `DEC-20260906-005`, `IMP-20260902-018`, `IMP-20260906-003`.
- Status: Verified.
- Notes: Two emulators are a maximum, not a requirement. Physical-phone release protections and the fast non-Play Store lane remain unchanged.

### FB-20260907-004 — Make elapsed Goal timers visually consistent and unconstrained

- Date/source: 2026-09-07, direct user feedback with a real-use book-fold screenshot.
- User need: The same Count Time Since duration should read as one consistent timer across the fold support pane and the main Goal cards, without awkward line breaks caused by the card's generic header/action layout.
- Acceptance criteria: Audit every active/terminal/editor/Insights/detail/support-pane elapsed presentation and the shared card constraints; challenge the current card structure where it forces rich status into a narrow title lane; preserve authored unit combinations, calendar arithmetic, reset behavior, and accessibility; implement a reusable long-term presentation boundary rather than a screenshot-specific spacing tweak; verify compact, fold/adaptive, enlarged-text, semantics, and current Goal journeys with focused tests and fresh visual evidence.
- Affected users/workflows: Count Time Since Goals on compact phones, foldables, tablets, Home, Goal lists, adaptive support panes, Insights, editor previews, details, terminal snapshots, and reset flows.
- Related: `FB-20260906-009`, `FB-20260906-010`, `FB-20260907-005`, `FND-20260906-007`, `FND-20260907-014`, `DEC-20260906-009`, `DEC-20260907-004`, `IMP-20260907-009`, `IMP-20260907-010`, `VER-20260907-010`, `VER-20260907-011`.
- Status: Released.
- Notes: Greenfield card changes were authorized where useful; the shared card shell was retained with a reusable rich-status boundary. Production data and timer meaning remain unchanged, and the result is installed in Whip 0.3.59/code 65.

### FB-20260907-005 — Release the elapsed-timer fix to the owner phone

- Date/source: 2026-09-07, direct user follow-up after accepting the implemented timer-layout fix.
- User need: Put the verified elapsed Goal presentation on the owner's phone for normal use.
- Acceptance criteria: Advance to a higher private-release version; preserve signer, package identity, first-install identity, and existing data; commit and push the exact release source before installation; use the guarded fast non-Play Store lane against the explicitly selected physical phone; do not reset, clear, uninstall, downgrade, or run instrumentation on the phone; verify signed artifact identity/hash, installed version, in-place update, cold launch, foreground Activity, live process, and bounded fatal/AndroidRuntime/ANR/Room/SQLite errors.
- Affected users/workflows: Owner-phone use of Count Time Since Goals, especially the book-fold support/main-card layout, plus release engineering and data-preserving upgrade behavior.
- Related: `FB-20260907-004`, `DEC-20260907-004`, `IMP-20260907-009`, `IMP-20260907-010`, `VER-20260907-010`, `VER-20260907-011`.
- Status: Released.
- Notes: This is a private owner-phone release, not Play Store publication or frozen-candidate qualification.

### FB-20260907-006 — Make card reading order maximally uniform

- Date/source: 2026-09-07, direct user feedback with an owner-phone Home screenshot after the 0.3.59 release.
- User need: Task, Habit, Goal, Track, and related cards should share an immediately learnable reading order instead of placing secondary text under different anchors, centering some content, or varying equivalent typography, sizing, and emphasis.
- Acceptance criteria: Audit shared and bespoke collection cards plus Home variants for identity, title, status, metadata, badges, disclosure, primary action, expanded details, typography, horizontal anchors, vertical rhythm, and accessibility order; identify other defects of the same category beyond the screenshot; define and implement the strongest reusable hierarchy that preserves domain-specific actions and rich content; verify compact, adaptive/fold, large-text, RTL, semantics, and affected families through focused tests and fresh visual evidence; code-review and repair regressions; commit and push; then advance to a higher signed private release and install it in place on the explicitly selected owner phone with preserved identity/data and full guarded smoke verification.
- Affected users/workflows: Home summaries and Task, Habit, Goal, and Track collections, including active/completed/archived rows, rich elapsed status, schedule/repeat metadata, progress/streak summaries, tags/badges, and expanded details.
- Related: `FB-20260903-016`, `FB-20260907-004`, `FND-20260903-026`, `FND-20260907-015`, `DEC-20260903-014`, `DEC-20260907-004`, `DEC-20260907-005`, `IMP-20260907-011`, `IMP-20260907-012`, `VER-20260907-012`, `VER-20260907-013`.
- Status: Released in place as Whip 0.3.60/code 66 after code review, two-emulator acceptance, fresh affected-family visual review, and guarded owner-phone verification; awaiting normal real-use feedback.
- Notes: Maximal uniformity means one semantic grammar for equivalent information, while genuinely different controls or domain evidence remain explicit rather than cosmetically forced into sameness. The existing card shell was retained because the constraint was the internal reading grid, not the container.

### FB-20260907-007 — Eliminate duplicate visible actions

- Date/source: 2026-09-07, direct user feedback with an owner-phone Gym Routine screenshot from Whip 0.3.60.
- User need: A surface must not present the same action twice, as the Routine detail currently does with two adjacent “Open Active Workout” buttons; find and correct other visual double-button errors of the same category across Whip.
- Acceptance criteria: Trace the duplicate to its action owners rather than hiding one label cosmetically; audit screens, dialogs, sheets, menus, cards, docked actions, and responsive branches for simultaneously visible controls that invoke the same outcome; distinguish legitimate repeated navigation in separate contexts from adjacent duplicate decisions; preserve the strongest context-appropriate action and all domain behavior; add executable duplicate-action regressions; perform focused emulator interaction, semantic/accessibility, responsive, and fresh visual QA; fix confirmed related cases and push a coherent verified source change.
- Affected users/workflows: Gym Routine details and active-workout recovery, plus any Whip surface where duplicated commands weaken hierarchy, consume space, create uncertainty, or cause assistive technology to announce the same decision twice.
- Related: `FB-20260906-008`, `FND-20260906-004`, `DEC-20260906-006`.
- Status: Released in place as part of Whip 0.3.61/code 67 after the later broader release authorization; see `IMP-20260907-013`, `IMP-20260907-016`, `VER-20260907-014`, and `VER-20260907-017`.
- Notes: The original request did not separately authorize a release, so Whip 0.3.60 initially remained installed. `FB-20260907-008` later authorized the combined higher-version release.

### FB-20260907-008 — Remove Gym set density mode and converge the complete product

- Date/source: 2026-09-07, direct user goal following the collection-card and duplicate-action corrections.
- User need: Gym workout sets must stop offering a separate compact-row mode and instead use one carefully balanced design that combines the useful information of the current comfortable rows with the scanability of compact rows. Whip should then be re-reviewed end to end—including Gym and Routines—as one uniform, polished, highly functional product.
- Acceptance criteria: Remove the Gym compact-set toggle and every rendering, settings, persistence, backup, documentation, and test branch it owns; preserve useful set classification, prescription, effort, completion, editing, and accessibility information in one responsive row grammar; critically inspect the complete current surface catalog and common journeys for hierarchy, spacing, typography, component, terminology, action, responsive, theme, accessibility, and interaction inconsistencies; implement all well-supported findings while preserving intentional domain workflows and owner data; code-review and verify with focused automation, two explicitly selected disposable emulators, fresh semantic/accessibility and visual evidence, and a proportionate full-product regression; commit and push clean source; then create a higher-version signed private release and install it in place on the explicitly selected owner phone through the fast non-Play Store lane, without reset, clear, uninstall, downgrade, or physical-device instrumentation, and verify identity, version/artifact, cold launch, foreground process, and bounded fatal/persistence logs.
- Affected users/workflows: Workout execution and set editing, Gym defaults, 5/3/1 and general Routine sessions, settings and portable backups, the complete Whip surface inventory and navigation, and owner-phone upgrade validation.
- Related: `FB-20260903-016`, `FB-20260907-002`, `FB-20260907-003`, `FB-20260907-006`, `FB-20260907-007`, `FND-20260903-026`, `DEC-20260903-014`, `DEC-20260903-015`.
- Status: Released in place as Whip 0.3.61/code 67 through `IMP-20260907-016` / `VER-20260907-017`, after the interim APK handoff and later owner-phone reconnection.
- Notes: “One consistent look” means equivalent information and controls share a predictable grammar. Workout execution may retain domain-appropriate high-signal controls, but it must not preserve a second user-selectable density dialect.

### FB-20260907-009 — Deliver the accepted release as an APK instead of installing it

- Date/source: 2026-09-07, direct user follow-up during the private release step.
- User need: The owner phone is unavailable, so do not continue trying to connect or install; provide the higher-version signed APK directly for manual installation.
- Acceptance criteria: Preserve the already accepted product and exact clean pushed release source; build and verify the signed private APK; give the artifact a clear versioned filename; document version, package, signer, and hash evidence; do not claim device installation or device-runtime verification.
- Affected users/workflows: Private release delivery only. Product behavior and owner data are unchanged until the APK is manually installed in place.
- Related: `FB-20260907-008`, `IMP-20260907-014`, `IMP-20260907-015`, `VER-20260907-015`, `VER-20260907-016`.
- Status: Released; Whip 0.3.61/code 67 was supplied as a signed private APK, then installed only after the user separately provided a reachable target in `FB-20260907-010`.
- Notes: This replaced the physical-install portion of `FB-20260907-008` while the phone was unavailable; the later explicit install request superseded that temporary limitation. Neither request broadens the release to the Play Store or qualifies a frozen public candidate.

### FB-20260907-010 — Install the delivered Whip 0.3.61 APK on the reconnected owner phone

- Date/source: 2026-09-07, direct user follow-up after providing a new wireless-debugging endpoint.
- User need: Install the already signed Whip 0.3.61/code 67 private APK on the now-reachable owner phone.
- Acceptance criteria: Connect only to the user-selected physical target; verify target identity and the pre-install package/version/first-install state; perform an in-place signed release installation without reset, clear, uninstall, downgrade, or instrumentation; verify installed version and APK hash, preserved first-install identity, successful cold launch, foreground Activity, live process, and absence of relevant fatal or persistence errors in a bounded post-launch scan.
- Affected users/workflows: Owner-phone upgrade from Whip 0.3.60/code 66 to the accepted single-density Gym and whole-product consistency release.
- Related: `FB-20260907-008`, `FB-20260907-009`, `IMP-20260907-014`, `IMP-20260907-015`, `IMP-20260907-016`, `VER-20260907-015`, `VER-20260907-016`, `VER-20260907-017`.
- Status: Released; the signed 0.3.61/code 67 APK was installed in place with identity preserved and passed the guarded device smoke.
- Notes: The transient device address is intentionally not stored in durable product memory.

### FB-20260907-011 — Workout set edits fail after changing previous and active sets together

- Date/source: 2026-09-07, direct owner feedback from normal use of Whip 0.3.61/code 67.
- User need: Editing an earlier exercise/set and then changing the active set's weight and reps must not leave the workout unable to save behind an unclear notification.
- Acceptance criteria: Reconstruct the previous-set plus active-set edit sequence on disposable test state; identify the exact surfaced notification, validation boundary, and persisted-versus-draft state involved; determine whether the failure protects a valid invariant or is an unintended conflict; assess message/action clarity and data-loss risk; report the confirmed cause and a bounded repair recommendation without mutating the physical phone.
- Affected users/workflows: Active Gym workout execution, editing completed/previous sets, modifying the current active set, and saving the combined workout state.
- Related: `FND-20260901-025`, `FND-20260907-020`, `DEC-20260901-023`, `IMP-20260901-016`, `VER-20260901-018`, `VER-20260907-018`.
- Status: Confirmed; a previous Set save advances the whole workout revision while the active quick-entry composer retains its opening revision, so its otherwise valid draft is deterministically rejected as stale.
- Notes: The owner could not recall the notification text, so the investigation must derive it from executable state and source rather than assuming its wording.

### FB-20260907-012 — Make card geometry visually uniform across Whip and repair active quick-save

- Date/source: 2026-09-07, direct user goal following real-use review of Whip 0.3.61/code 67.
- User need: Cards—especially mixed Task, Habit, Goal, and Track cards on Home—must have uniform sizing and feel like one coherent visual system while still communicating genuinely different data. Routines, Gym, and other card families must follow the same product grammar where their roles are equivalent. The confirmed prior-Set edit versus active quick-save failure must be fixed in the same release.
- Acceptance criteria: Inventory current card shells, heights, insets, title/support baselines, identity gutters, action lanes, expansion behavior, section gaps, and responsive/large-text/fold states across Home, Task, Habit, Goal, Track, Routine, Gym, Settings, search, history, and supporting surfaces; define and implement a reusable responsive geometry contract that makes equivalent cards maximally uniform without truncating data, inventing empty space, or forcing specialized charts/editors/active input workspaces into collection-row dimensions; add exact geometry and reading-order regressions for mixed Home and representative domain families; implement a narrow Gym quick-save boundary that accepts unrelated previous-Set edits while still rejecting same-Set, placement/equipment/interpretation, removal/substitution, and duplicate-submit conflicts; perform code review plus fast two-emulator behavioral, accessibility/semantics, responsive, and fresh visual-catalog QA; fix regressions; preserve owner data and domain semantics; commit and push clean source; advance to a higher signed private version; install it in place on the explicitly selected physical owner phone without reset, clear, uninstall, downgrade, or physical instrumentation; verify artifact/signing/version/hash, preserved first-install identity, cold launch, foreground Activity, live process, and bounded fatal/persistence logs.
- Affected users/workflows: Home scanning and comparison; Task, Habit, Goal, Track, Routine, and Gym collections; responsive/fold/large-text layouts; active workout correction and Set completion; owner-phone private release.
- Related: `FB-20260907-006`, `FB-20260907-008`, `FB-20260907-011`, `FND-20260907-015`, `FND-20260907-017`, `FND-20260907-020`, `DEC-20260907-005`, `DEC-20260907-007`, `DEC-20260907-008`, `IMP-20260907-011`, `IMP-20260907-014`, `VER-20260907-012`, `VER-20260907-015`, `VER-20260907-018`.
- Status: Released and device-verified in place as Whip 0.3.62/code 68 with owner installation identity and data preserved.
- Notes: Greenfield UI changes are authorized where they materially improve the long-term system. “Uniform size” is interpreted as shared geometry for equivalent information states, plus predictable responsive growth for richer content—not clipping or padding every card to one fixed pixel height regardless of content.

### FB-20260907-013 — Complete Whip to a whole-product gold standard

- Date/source: 2026-09-07, direct user goal supplied as an attached implementation specification.
- User need: Treat the complete current Whip product as one coherent system and continue evidence-driven audit, root-cause remediation, regression testing, visual/accessibility review, durable recording, and release work until correctness, usability, design, customization, historical truth, and recovery are credibly exceptional rather than merely present or test-passing.
- Acceptance criteria: Reconcile the complete current surface and journey inventory against source and reproducible emulator behavior; re-verify `FB-20260907-011` and `FB-20260907-012`; resolve every confirmed P0/P1 and implement or evidence-reject every major P2; preserve owner data, installation identity, completed history, and canonical 5/3/1 semantics; maintain one responsive design grammar without reviving density dialects or duplicate actions; protect contextual navigation, drafts, lifecycle recovery, accessibility, and adaptive layouts; add executable regressions for repaired defects; use proportionate fast QA with up to two disposable emulators and a fresh complete visual/semantic campaign; commit and push coherent verified batches; then build a higher-version signed private APK/AAB and install it in place on the explicitly selected owner phone with guarded identity, hash, launch, process, and bounded fatal/persistence verification, or provide the verified APK if the phone is unavailable.
- Affected users/workflows: All Whip users and product areas, including first-use and returning Home, Tasks, Habits, Goals, Tracks, taxonomy, reminders, timers, focus, Gym, Routines, active workouts, History, Progress, 5/3/1, Settings, Health Connect, backup/restore/reset, widgets, notifications, external capture, compact/fold/tablet layouts, large text, RTL, keyboard, screen-reader, interruption, error, and recovery paths.
- Related: `FB-20260907-002`, `FB-20260907-011`, `FB-20260907-012`, `FND-20260907-020`, `FND-20260907-021`, `DEC-20260907-009`, `IMP-20260907-017`, `IMP-20260907-018`, `VER-20260907-019`, `VER-20260907-020`.
- Status: Released and device-verified in place as Whip 0.3.63/code 69 from exact pushed release source `fe943ec`, with owner installation identity and data preserved.
- Notes: Current 0.3.62/code 68 and its prior complete QA are the baseline, not proof of this new run. Instrumentation and destructive test state remain emulator-only; public Play Store qualification is out of scope.

### FB-20260907-014 — Explain 4-Day 5/3/1 versus 5/3/1 for Beginners in the builder

- Date/source: 2026-09-07, direct user question during normal evaluation of the 5/3/1 Routine Builder, reconciled into the current whole-product goal.
- User need: Understand the practical difference between the 4-Day and Beginners layouts before selecting one, without needing external knowledge or trial-building both programs.
- Acceptance criteria: Present comparable concise consequences for every schedule choice before selection, including weekly day/exercise structure, built-in Supplemental work, assistance expectations, arbitrary-Exercise behavior, and any plan compatibility boundary; keep the exact generated program authoritative; preserve compact/large-text scrollability and accessible selected-state semantics.
- Affected users/workflows: New and returning 5/3/1 users choosing a Routine schedule, especially beginners and users deciding between a standard four-day template, the three-day Beginners template, and arbitrary custom Exercises.
- Related: `FB-20260906-013`, `FB-20260907-013`, `FND-20260907-022`, `DEC-20260907-010`.
- Status: Implemented, focused-tested on both disposable emulators, and visually accepted; included in the active whole-product release candidate.
- Notes: The builder now presents all available layouts as parallel explanatory choice cards before selection. A long-term Leader/Anchor choice explicitly points users back to Classic cycle for the standalone Beginners layout.

### FB-20260907-015 — Give collection-card metadata its own full-width row

- Date/source: 2026-09-07, direct owner feedback after normal use of Whip 0.3.63/code 69 and explicit clarification that titles remain left-aligned.
- User need: Preserve the newly consistent cards, but stop squeezing status and scheduling information into the title/action lane. The emoji, left-aligned title, expand/collapse control, and completion or primary action should form one vertically centered header row; supporting information should occupy a separate row below it so values such as “Scheduled · Sep 7, 2026 · Repeats · Mon, Thu” remain readable rather than ellipsized.
- Acceptance criteria: Apply one shared two-row grammar to equivalent Task, Habit, Goal, and Track collection cards; keep title text left-aligned and vertically centered with the identity and trailing controls; start the information row at the emoji's leading edge and use the full card content width; permit truthful responsive wrapping instead of forced one-line truncation; preserve equal top/bottom card padding and 48 dp controls; inspect and adjust expanded cards so metadata is neither duplicated nor misaligned and actions remain reachable at compact width and enlarged text; add exact geometry/content regressions and visually inspect affected collapsed and expanded surfaces.
- Affected users/workflows: Home and Task/Habit/Goal/Track collections, expanded inline detail, selection and reorder states, long scheduling/recurrence labels, elapsed Goal status, compact phones, fold panes, and enlarged text.
- Related: `FB-20260907-006`, `FB-20260907-012`, `FND-20260907-021`, `FND-20260907-025`, `DEC-20260907-005`, `DEC-20260907-009`, `DEC-20260907-011`, `IMP-20260907-017`, `IMP-20260907-024`, `VER-20260907-019`, `VER-20260907-026`.
- Status: Verified.
- Notes: Implemented and emulator-verified in `IMP-20260907-024` / `VER-20260907-026`; source is not yet released to the owner phone. The requested alignment is start/left alignment within the title slot, not horizontally centered title text. A modest increase in collapsed height is explicitly accepted in exchange for complete, calmer information presentation.

### FB-20260907-016 — Give active-workout Sets a readable nested-card hierarchy

- Date/source: 2026-09-07, direct owner feedback with a phone screenshot of the active 5/3/1 Workout in installed Whip 0.3.63/code 69.
- User need: The active Workout's current sequence of Set text, metadata, overflow actions, and completion controls is too cramped to scan reliably. Each Set should become a clearly bounded nested card whose information is organized by priority and given enough spacing and padding to remain readable, while still feeling like Whip rather than a separate Gym design language.
- Acceptance criteria: Audit every active-workout Set state and any reused presentation with the same cramped grammar; introduce one responsive nested Set-card component inside the Exercise card; make Set number/work section and performed or planned load/reps the primary scan line; group classification, prescription/target, effort, and status as secondary information without flattening distinct meanings; keep overflow and completion controls obvious, stable, at least 48 dp, and unambiguous; preserve the distinct active input composer, next-set focus, rest timer, grouping/reorder behavior, 5/3/1 progression, editing, and historical truth; use balanced internal/external spacing, Whip typography, surface roles, and shapes; verify incomplete/completed, programmed/general, long-text, narrow-width, enlarged-text, and interaction states on both disposable emulators; inspect related Gym/History/Routine surfaces and fix only genuinely equivalent cramped rows.
- Affected users/workflows: Active Gym workout execution, 5/3/1 Main and Supplemental work, general Routine and ad-hoc Sets, previous-Set review/editing, completion, rest-timer use, compact phones, and enlarged text.
- Related: `FB-20260907-008`, `FB-20260907-012`, `FND-20260907-017`, `FND-20260907-026`, `DEC-20260907-007`, `DEC-20260907-012`, `IMP-20260907-014`, `IMP-20260907-025`, `VER-20260907-015`, `VER-20260907-027`.
- Status: Verified.
- Notes: Implemented in `IMP-20260907-025` and accepted in `VER-20260907-027`. This feedback supersedes the earlier rejection of any nested passive-Set cards in `DEC-20260907-007`, but does not request a second density setting or a visually heavy card-within-card stack. The result is one calm nested hierarchy with deliberate surface contrast and responsive content growth; no release or physical-phone operation was requested for this follow-up.

### FB-20260907-017 — Critically compare and finish the Gym design, then release it

- Date/source: 2026-09-07, direct owner follow-up after the nested active-workout Set implementation.
- User need: Reassess the complete Gym-side experience against Whip's other cards and views instead of assuming the latest Set layout is the best final design. Correct any remaining hierarchy, spacing, component, responsive, accessibility, or interaction inconsistencies and put the accepted result on the owner phone.
- Acceptance criteria: Compare Workout, History, Progress, Library, Routine, 5/3/1, editors, menus, dialogs, empty/error/loading states, and the revised Set hierarchy against the shared Whip card grammar and one another; distinguish justified domain-specific workspaces from accidental one-off styling; implement every well-supported improvement without flattening training evidence or changing workout/program meaning; add or strengthen focused regressions; use the approved fast two-emulator behavioral, semantic/accessibility, responsive, and visual QA lane; code-review and fix regressions; commit and push clean source; then advance to a higher signed private version and install it in place on the explicitly selected physical owner phone without reset, clear, uninstall, downgrade, or physical instrumentation; verify artifact/signing/version/hash, preserved first-install identity, cold launch, foreground Activity, live process, and bounded fatal/persistence logs.
- Affected users/workflows: Gym destination navigation, active general and 5/3/1 workouts, completed-workout History, Progress, Exercise/Machine/Routine libraries and editors, Routine Builder, compact/fold/large-text layouts, accessibility, and owner-phone release.
- Related: `FB-20260907-008`, `FB-20260907-012`, `FB-20260907-013`, `FB-20260907-016`, `FND-20260907-026`, `DEC-20260907-012`, `IMP-20260907-025`, `VER-20260907-027`.
- Status: Released and device-verified as Whip 0.3.64/code 70.
- Notes: The accepted implementation is recorded in `IMP-20260907-026` / `VER-20260907-028`; the signed in-place owner-phone release is recorded in `IMP-20260907-027` / `VER-20260907-029`. Public Play Store qualification remains out of scope.

### FB-20260908-001 — Bring every Whip surface to the established gold standard using emulators only

- Date/source: 2026-09-08, direct owner request following normal-use feedback and the 0.3.64 Gym/card releases.
- User need: Re-audit the complete current Whip app against the high UX, UI, visual-design, consistency, functionality, and QA standards established through prior owner feedback, then implement the resulting whole-product overhaul rather than stopping at recommendations or a report.
- Acceptance criteria: Formulate and start a durable implementation goal; inspect every product area, common and edge journey, page, card, row, editor, form, menu, dialog, sheet, state, system integration, and supporting interaction; treat the accepted two-row collection-card hierarchy, readable nested Gym Sets, single-owner actions, exact edit/save ownership, consistent timer presentation, one-density system, discoverable 5/3/1 choices, responsive layouts, accessibility, historical truth, and clear recovery as product-wide regression patterns; challenge legacy structures and permit evidence-backed greenfield changes where they improve the long-term system; preserve user data and intentional domain behavior; implement and code-review all supported P0/P1 and major P2 findings plus consistency issues discovered along the way; use focused fast QA and no more than two explicitly targeted disposable emulators for interaction, accessibility/semantics, adaptive/responsive, visual-catalog, and full regression evidence; commit and push coherent verified chunks; do not release, version-bump for release, publish, connect to, query, install on, launch, inspect, clear, uninstall, downgrade, or instrument any physical phone.
- Affected users/workflows: The entire Whip product, including Home, global navigation, Tasks, Habits, Goals, Tracks, taxonomy and organization, search/review, timers and reminders, Gym/Routines/5/3/1, Settings, Health Connect, backup/restore/reset, widgets, notifications, external capture, first-use and returning use, error/recovery, compact/fold/tablet/large-text/RTL/keyboard/screen-reader states, and long-lived user data.
- Related: `FB-20260907-002`, `FB-20260907-003`, `FB-20260907-004`, `FB-20260907-006` through `FB-20260907-008`, `FB-20260907-011` through `FB-20260907-017`, `DEC-20260903-015`, `DEC-20260907-009`, `DEC-20260907-011`, `DEC-20260907-013`, `VER-20260907-024`, `VER-20260907-026`, `VER-20260907-028`, `VER-20260907-029`.
- Status: Verified.
- Notes: Completed by `IMP-20260908-008` / `VER-20260908-008`: all 1,588 product tests and 185 catalog surfaces passed across two disposable emulators. No physical-device release was part of that goal.

### FB-20260908-002 — Audit Tracks and every other feature with equal depth

- Date/source: 2026-09-08, direct owner clarification while formulating the whole-product implementation goal.
- User need: Tracks must be examined thoroughly, and no other Whip feature may receive a shallow or sampled review simply because Gym or collection cards have generated more recent feedback.
- Acceptance criteria: Trace Track creation, field configuration, entry capture/editing, units and precision, history, Insights, search/discovery, filters, import/export, archive/restore/delete, related reminders/integrations, persistence and malformed-data boundaries, lifecycle recovery, and every populated/empty/error/adaptive/accessibility state; apply an equivalently complete source/domain/persistence/UI/QA audit to Tasks, Habits, Goals, Gym/Routines/5/3/1, Settings, organization, search/review, system integrations, and cross-feature behavior; inventory every declared surface and important state instead of accepting representative screenshots as whole-product proof.
- Affected users/workflows: All Track users and all other Whip feature users, including long-lived data, advanced customization, compact/fold/tablet layouts, large text, RTL, keyboard, screen reader, interruption, and failure recovery.
- Related: `FB-20260908-001`, `FB-20260907-002`, `FB-20260907-013`, `FND-20260831-018`, `FND-20260901-022` through `FND-20260901-024`, `FND-20260902-005`, `DEC-20260902-006`, `DEC-20260902-019`.
- Status: Verified.
- Notes: “Thorough” required behavior and data-boundary evidence in addition to visual-catalog coverage. Tracks received complete profile coverage and 25 final visual states; every other feature received the same standard. See `VER-20260908-008`.

### FB-20260908-003 — Release the accepted whole-product overhaul to the owner phone

- Date/source: 2026-09-08, direct owner follow-up after emulator-only completion.
- User need: Put the fully accepted Whip whole-product overhaul on the previously selected physical owner phone.
- Acceptance criteria: Advance to the next higher private version; preserve package/signer identity, first-install identity, app data, history, routines, and 5/3/1 state; run the established fast private-phone readiness lane against the already complete emulator acceptance; commit and push exact release source before construction; build signed optimized APK/AAB artifacts; install the APK in place with no reset, clear, uninstall, downgrade, or physical instrumentation; verify version, artifact hash equality, signer continuity, successful cold launch, foreground MainActivity, live process, and absence of relevant fatal, persistence, database, or startup errors.
- Affected users/workflows: The complete accepted Whip product and the owner's existing local data on the selected Samsung phone.
- Related: `FB-20260908-001`, `FB-20260908-002`, `IMP-20260908-008`, `VER-20260908-008`, `DEC-20260906-003`.
- Status: Released and device-verified.
- Notes: Completed by `IMP-20260908-009` / `VER-20260908-009` as private Whip 0.3.65/code 71. The signed APK installed in place on the selected Samsung with package, signer, first-install identity, and owner data preserved; this was not Play Store candidate qualification or publication.
