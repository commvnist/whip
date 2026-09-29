# Whip deep product review — 2026-09-29

Status: Investigating. FB-20260929-002. Baseline `04bc58e2`, released 0.3.83/code 89. The owner rejected the depth of the prior audit; its completed changes remain historical facts, not evidence that the remaining experience is satisfactory.

## Method and completion contract

1. Start with user intent and inspect complete paths: getting started, capturing/organizing a busy day, executing recurring work, tracking progress, designing/using a custom log, planning/completing training, and managing/recovering data. For each path examine entry visibility, choices, taps/scrolling, hierarchy, field order, confirmation, interruption and subsequent correction. Include mature data, competing tasks and alternate domain modes.
2. Inspect current rendered interfaces and trace their state/domain/persistence owners. Prior screenshots and test counts are navigation aids only. A source inventory is not an assessment of interaction quality, and a passing assertion is not visual approval. Document actual before/after rationale, including alternatives where a substantial redesign is warranted.
3. Publish concrete findings and accepted remedies before production edits. Implement every accepted item across its relevant callers, including reusable components and recovery. Do not invent abstractions or data semantics to inflate the scope; do not cap findings at a convenient number.
4. Use exact, selected checks only when their result can guide an edit or establish acceptance, each routine invocation bounded with `timeout --kill-after=3s 55s`. No full, readiness or candidate batch. Capture and personally inspect changed interfaces, with relevant compact/large-text/keyboard/wide states. Failed and incomplete attempts remain failures/incomplete.
5. Reconcile each accepted remedy with implementation, behavior evidence and rendered outcome. Report what was directly executed versus source-reviewed. Update durable memory, commit/push coherent verified chunks, and close only when the full accepted plan is delivered.

## Ownership

- Parent: Home, Tasks, onboarding, search/navigation/organization/Review, cross-feature design and reusable controls, architecture, integration, native observation and final verification.
- Existing productivity agent: Habits, Goals and Tracks, with independent critique of previous keeps and complete alternate-mode/mature-data journeys.
- Existing Gym/Settings agent: training creation/execution/correction and administrative/recovery jobs, with independent critique of previous keeps.

Only the two previously authorized agent slots are reused. Parent serializes builds and device operations. Physical phone remains on the verified release; synthetic disposable emulator data is used for investigation.

## Investigation questions

- Can a person identify and execute the next useful action without reading a manual, opening Options or traversing repeated dialogs?
- Does the normal screen emphasize the work itself, or is it dominated by navigation, filters, explanations, repeated summaries and empty space?
- Do creation flows ask the right questions in a useful order, expose necessary context, and allow safe correction without losing drafts or surprising meaning changes?
- Can users find and repair multiple records, compare evidence, or change scope without losing their place or repeating setup?
- Are domain concepts, status, dates, units, completion and failure explanations consistent and true across their consumers?
- Which repeated controls/behaviors should be shared, and which superficial similarities conceal genuinely different domain rules?
- Where do data volume, clock updates, query changes and lifecycle recreation cause avoidable work or stale/incoherent state?
- Would a simpler information hierarchy or fewer decisions materially improve a complete job, and can it be delivered through existing owners?

Concrete observed findings, alternatives and the accepted implementation plan will be added below as investigation establishes them. No prior keep is grandfathered in.
