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
- Status: Proposed; goal prepared but not executed.

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
