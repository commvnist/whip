# Reusable Whip maximum-quality goal

> Run status: **Closed by user direction on 2026-09-02.** The two-day mission stopped before the original definition of done and must not resume automatically. The text below remains a reusable future goal; preserved residual work is indexed in `INDEX.md`.

Copy the text below into a new Codex request when ready to run the whole-product program.

```text
/goal Whip Maximum-Quality Product Remediation — Durable, Focused, Iterative Implementation

Transform the entire Whip application into an exceptionally coherent, usable, reliable, accessible, understandable, and customizable product.

This is an implementation goal, not a report, design proposal, or superficial polish pass. Continue investigating, implementing, testing, and refining the product until the completion criteria are credibly satisfied.

You have authority to rewrite, replace, consolidate, migrate, or remove weak interfaces, components, navigation structures, data models, state systems, persistence logic, and architectural abstractions when this materially improves the product. Do not preserve a flawed abstraction because it already exists. Do not rewrite functioning systems without concrete benefit. Preserve user data, historical records, completed activity, custom content, and compatible behavior unless an explicit, tested, safe migration is implemented.

## Mandatory durable-memory protocol

Use `$maintain-whip-memory` throughout this goal. Chat history is not the project record.

Before investigation or planning:

1. Read `AGENTS.md` and `docs/product-memory/INDEX.md` completely.
2. Search and read all relevant feedback, findings, decisions, implementation entries, verification, detailed audits, and source evidence.
3. Reconcile recorded claims with current code, tests, schemas, and device behavior.
4. Record every new user requirement and acceptance criterion before implementation.

During every iteration:

- Assign stable IDs to new feedback, findings, decisions, implementations, and verification.
- Record observed behavior separately from expectation, inference, and decision.
- Capture root causes, unresolved risks, affected users, source paths/symbols, and regression requirements. Record alternatives or rejected approaches only when they materially affect compatibility, safety, or future maintenance.
- Update memory at meaningful checkpoints and before any context-heavy phase ends.
- Preserve superseded history rather than rewriting it as if it never happened.

At the end of every implementation or verification batch:

- Link changes to their feedback, findings, decisions, migrations, and tests.
- Record exact commands and distinguish test-source counts, compilation, targeted execution, complete-suite execution, device smoke testing, and release.
- Update statuses only when supported by evidence.
- Update the index with current state, next work, residual risk, and release details.
- Treat the batch as a Git chunk when it is a coherent, independently understandable, proportionately verified, safely revertible outcome. Inspect all diffs, stage only owned paths/hunks, create a focused commit, push normally to the configured upstream, and verify upstream reachability before beginning substantially different work.
- Do not create file-by-file microcommits or combine unrelated outcomes into a final mega-commit. Never sweep unrelated dirty-worktree changes into a commit, bypass required hooks, force-push, amend another author's work, rewrite shared history, or claim delivery after a failed push.
- If commit or push is blocked by validation, authentication, or ambiguous upstream state, preserve the local work, record and report the exact blocker, and do not start another large unrelated chunk until it is resolved or the user directs otherwise.

No iteration is complete if its material context exists only in chat.
No completed chunk is delivered if it exists only in the local worktree.

## Entire product scope

Audit Whip from top to bottom: Home, Tasks, Habits, Goals, Tracks, Gym, Settings, global navigation, search, review, widgets, reminders, notifications, backups, import/export, onboarding, and cross-feature behavior.

Inventory and exercise every:

- screen, destination, route, tab, and navigation transition
- list, detail, empty, loading, offline, error, and recovery state
- form, editor, builder, dialog, bottom sheet, menu, popover, and picker
- card, field, switch, button, icon, timer, notification, and destructive control
- create, edit, reorder, duplicate, substitute, archive, restore, delete, undo, import, export, and migration workflow
- small-screen, large-screen, foldable, resized, rotated, keyboard, screen-reader, high-text-scale, and one-handed state
- persistence, validation, state restoration, background work, caching, synchronization, database, and historical-data boundary

Do not sample only attractive or familiar screens. Trace every action to its result and every result back to a discoverable configuration path.

## Development process

Use a conventional single-developer workflow. The primary agent owns investigation, prioritization, implementation, testing, documentation, commits, and final quality.

For each coherent issue or closely related group:

1. Reproduce or verify the current behavior.
2. Inspect the relevant UI, domain, persistence, and test boundaries.
3. Select the smallest coherent solution that fixes the root problem without unnecessary abstraction.
4. Implement the change.
5. Add focused regression coverage and run proportionate shared checks.
6. Exercise the affected workflow on an emulator when interaction behavior changed.
7. Record concise durable facts, commit, push, and move to the next priority.

Do not create subagents, simulated agent panels, recursive audits, formal debates, Director reviews, adversarial-review loops, or repeated approval rounds. Do not reopen a settled decision without new contradictory evidence. Use delegation or an external specialist only when the user explicitly asks for it.

## Representative use cases

Evaluate realistic workflows for:

- a first-time user
- a casual productivity user
- an advanced productivity-system enthusiast
- a user with ADHD or impaired executive function
- a user who constantly reorganizes, tweaks, overrides, and experiments
- a keyboard-oriented desktop/foldable user
- a mobile-only and one-handed user
- a screen-reader, large-text, or reduced-dexterity user
- a user with poor connectivity
- a user returning after weeks away
- a user with years of historical data
- a beginner gym user
- a general fitness user
- a powerlifter
- beginner, intermediate, and expert 5/3/1 lifters
- a coach who expects precise programming semantics

Exercise representative tasks without relying on hidden implementation knowledge. Record confusion, misinterpretation, errors, dead ends, repetitive entry, hidden functionality, unsafe editing, unclear terminology, poor defaults, excessive taps, and unmet expectations. These are developer-run use-case checks, not simulated focus groups or claims of recruited user research.

Pay special attention to people who want to modify, edit, reorder, substitute, duplicate, override, and customize nearly everything. Customization must remain understandable and must not silently corrupt rules, progression, relationships, or history.

## Mandatory regressions derived from real user feedback

Treat these as product-wide regression patterns, not isolated patches:

- 5/3/1 accepts user-selected lifts; Bench Press, Deadlift, and Zercher Squat works naturally without requiring the standard four.
- Actual 1RM, estimated 1RM, Training Max, Training Max percentage, and calculated working weight remain distinct.
- Training Max is discoverable and configurable outside the 5/3/1 wizard whenever an ordinary routine uses `% Training Max`.
- TM can be direct or derived from actual/e1RM using an adjustable percentage.
- Readiness adjustments are deliberate and explain their future impact.
- Per-lift cycle review may offer standard, higher, lower, hold, ignore, or custom choices using bounded AMRAP, PR-set, Joker, completion, failure, and test evidence.
- Suggestions are conservative, transparent, explainable, optional, and user-confirmed.
- Adding an exercise while editing a routine returns to the exact routine and day.
- Adding an exercise during a workout may affect only that workout while remaining truthful in history.
- Joker Sets remain additive and never remove, replace, consume, or regenerate BBB, FSL, SSL, Boring But Strong, custom Supplemental, Assistance, or Main work.
- Main, Supplemental, Assistance, Optional/Joker, and workout-only work remain semantically distinct.
- Editors never trap content in a tiny fixed pane that shows approximately one item.
- Responsive fixes do not create surprise expansion of unrelated content.
- A five-minute timer displays 5:00 and then 4:59, never 5:01 then 5:00.
- Every available control has a discoverable, valid way to configure its prerequisite data.
- Technically present functionality is not accepted when users cannot find, understand, or safely edit it.

Search the entire application for other instances of these underlying failure modes.

## Workflow evaluation

For every feature, test:

- discoverability and first-use comprehension
- terminology and information hierarchy
- happy path and advanced customization
- cancellation, Back, Save, Close, and unsaved-change behavior
- duplication, reordering, deletion, undo, restore, and error recovery
- empty, malformed, interrupted, partial, offline, and resumed states
- historical correctness and future-template separation
- accessibility and responsive layout
- interaction with related features

Every Add, Edit, Save, Close, Back, and destructive action must return the user to the context they reasonably expect.

Estimate discoverability, cognitive load, interaction cost, confidence, and error likelihood for major workflows. Count unnecessary taps/clicks.

## UX, ADHD, and customization standards

Separate planning/configuration interfaces from rapid execution interfaces when their requirements differ.

Minimize unnecessary taps, repeated entry, ambiguous controls, hidden prerequisites, modal stacking, excessive cards, walls of text, jargon, context loss, destructive proximity, and unexpected mutation of future or historical records.

Improve progressive disclosure, defaults, inline explanation, consequence previews, save state, contextual editing, recovery, glanceable mobile hierarchy, touch targets, typography, spacing, state differentiation, and button hierarchy.

For ADHD and executive-function usability, ask whether:

- the next action is obvious
- screens cause decision paralysis
- setup can be paused and safely resumed
- unfinished work is preserved
- the user can recover context after interruption
- optional detail is separated from what matters now
- reminders, state, and progress are understandable

Do not remove advanced control merely to look simple. Provide approachable defaults and progressively disclosed depth.

## Gym and strength-programming standards

Gym must feel designed for serious strength programming rather than a generic tracker with labels added.

Accurately model Exercise, actual 1RM, e1RM, Training Max, TM percentage, working weight, Main work, PR/AMRAP work, Joker, Supplemental, Assistance, Optional, workout-only work, completed sets, immutable historical prescriptions, progression, cycle review, test/deload protocols, and Leader/Anchor structures where intentionally supported.

Audit classic 5/3/1, 5s PRO, PR sets, FSL, SSL, BBB, Boring But Strong, Jokers, Leaders/Anchors, 7th Week Protocol, TM testing, assistance categories, custom schedules, custom percentages, arbitrary lifts, and progression adjustments.

Classify every variation as intentionally supported, partially supported, manually reproducible but awkward, incorrectly represented, or unsupported. Manual approximation is not first-class support.

Centralize deterministic calculations with explicit units, rounding, increment constraints, inputs, validation, and independent tests. Editing a program must not retroactively change completed workouts.

## Accessibility and in-context usability

Audit touch size/separation, semantics, focus order, keyboard operation, dialog focus, contrast, text scaling, color-only state, disabled clarity, dynamic announcements, timers, motion, orientation, fold posture, and screen resizing.

Account for fatigue, shaking hands, sweat, glare, darkness, distraction, poor connectivity, and 30–60 seconds between sets.

## Engineering standards

Find and remediate duplicated calculations, magic strings, arbitrary JSON replacing domain concepts, UI-only persistence state, fragile conditional chains, inconsistent validation, stale caches, context-loss navigation, historical mutation, race conditions, timer boundaries, date/timezone errors, unit conversion, migration hazards, and silent failures.

Move business rules out of presentation components. Prefer coherent explicit models and services over scattered flags. Do not create a universal DSL unless demonstrated needs justify it.

Before persistence changes, inspect compatibility. Make migrations explicit, preserve completed records and custom data, update backups, test upgrades from supported schemas, and never silently recompute history.

## Evidence and priority

Every significant finding must record observed behavior, expected behavior, why it matters, affected users, severity, reproduction, source paths/symbols, evidence, root cause, solution, acceptance criteria, and regression coverage.

Use screenshots for visual findings and concrete numerical examples for domain calculations.

Prioritize:

- P0: crash, data loss/corruption, incorrect domain behavior, unusable primary workflow, or security issue
- P1: fundamental navigation, domain, accessibility, configuration, or usability failure
- P2: major efficiency, flexibility, comprehension, and design improvement
- P3: polish and consistency

Fix P0/P1 before extensive P3 work.

## Work sequence

Work through the prioritized backlog one coherent chunk at a time:

1. Correctness and data-integrity defects.
2. Broken, inaccessible, or misleading primary workflows.
3. UX, information hierarchy, customization, responsive layout, and visual consistency.
4. Secondary workflow and platform-integration gaps.
5. Whole-app regression, migration, accessibility, and release checks.

Test fresh installs, upgrades with data, app closure during editing, partial saves, rapid taps, resize/rotation/folds, large text, invalid/zero/negative/large values, duplicates, missing/deleted references, unit changes, rounding, timer boundaries, workout restoration, routine edits during active blocks, optional/skipped work, historical stability, offline behavior, and malformed imports where relevant to the current chunk. Fix discovered regressions and rerun affected suites.

Continue while a confirmed P0/P1 remains, a primary workflow has a dead end, navigation loses context, customization corrupts rules/history, a major feature is undiscoverable, mobile/accessibility remains blocking, or tests reveal instability. Avoid recursive investigation after the evidence is sufficient to implement safely.

## Test and release gates

Add automated coverage for every repaired bug and critical workflow. Run domain, persistence, migration, navigation, UI, accessibility, small-screen, static-analysis, release-build, and regression checks.

Run destructive instrumentation only on disposable emulators. Do not clear physical-device application data.

After all gates pass, release the signed build to the connected phone while preserving data. Verify installed version, package hash, cold launch, foreground activity, migration/database health, and app-specific fatal logs. Record the exact evidence in durable memory.

## Definition of done

This goal is complete only when:

- every product area and interaction surface has been inventoried and exercised
- no P0/P1 remains
- major P2 work is implemented or explicitly rejected with evidence
- navigation preserves context
- settings and prerequisites are discoverable
- customization is powerful, predictable, and safe
- history remains truthful
- strength and 5/3/1 semantics are accurate
- arbitrary lifts work naturally
- Gym is practical during real workouts
- productivity workflows support simple and advanced users
- ADHD users can identify the next action and recover interrupted context
- mobile, foldable, keyboard, and accessibility interaction are first-class
- the visual system is coherent
- domain rules are centralized and tested
- migrations and important workflows have regression coverage
- existing user data remains valid
- the release passes all gates and is verified on the phone
- every durable conclusion, implementation, test result, risk, and next step exists in `docs/product-memory/`, not only chat

The standard is not “the feature exists.”

The standard is:

“A wide range of users—including demanding lifters, powerlifters, 5/3/1 users, productivity enthusiasts, ADHD users, accessibility users, beginners, and relentless customizers—can understand Whip, configure it confidently, modify it safely, use it rapidly, trust its behavior, and recover gracefully from mistakes.”

Continue implementing, recording concise evidence, testing, and progressing through the backlog until that standard is credibly met.
```
