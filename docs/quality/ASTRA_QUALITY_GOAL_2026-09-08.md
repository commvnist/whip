# Owner-provided Astra quality goal

Create and pursue a durable goal: audit and elevate the entire Whip app to an exceptionally high standard of UX, UI, visual design, functionality, accessibility, reliability, and engineering quality.

Carry this through investigation, design, implementation, and verification. Deliver an improved app, with evidence of what became better.

PRODUCT INTENT AND DESIGN FREEDOM

Approach Whip with fresh judgment. Previous audits, passing tests, and accepted designs provide context; they do not establish that the current experience is the best available solution.

Preserve Whip’s purpose, useful capabilities, user data, historical truth, and intentional domain behavior. Build on sound foundations. This is an evolution of the existing product, not a full greenfield rebuild.

You have latitude to substantially redesign individual screens, simplify workflows, reorganize information, replace weak components, consolidate patterns, and refactor supporting architecture when the improvement justifies the disruption. Small additions that complete an existing workflow are in scope. Avoid unrelated feature expansion.

For consequential changes, compare improving the existing approach with replacing it. Briefly document the user benefit, tradeoffs, compatibility implications, and why the selected approach is better. Do not let sunk cost preserve a poor experience, or let novelty justify unnecessary churn.

READ THE PRODUCT BEFORE CHANGING IT

Use the maintain-whip-memory skill. Read applicable repository instructions, the product-memory index, relevant user feedback, decisions, previous audits, and verification records. Reconcile them with current source and actual running behavior.

Distinguish explicit user preferences from historical implementation choices. Preserve the underlying needs behind prior feedback while allowing a better design to satisfy them differently. Record superseded decisions and their rationale.

Establish a current baseline: architecture, feature inventory, critical journeys, design patterns, screenshots, testing capabilities, and known limitations.

AUDIT THE WHOLE APP WITH EQUAL DEPTH

Cover Home, navigation, Tasks, Habits, Goals, Tracks, Gym, Routines, 5/3/1, search, review and insights, Areas, Tags, Settings, units, timers, reminders, notifications, widgets, Health Connect, external capture, import/export, backup/restore, and cross-feature behavior.

Discover additional surfaces from the code and running app. Do not treat this list or the existing screenshot catalog as exhaustive.

Inventory every distinct screen, editor, dialog, sheet, picker, menu, reusable component, and consequential state. Maintain a coverage matrix connecting each area to its source, runtime review, findings, changes, and verification.

Give Tracks and less prominent features the same scrutiny as Gym and frequently discussed cards.

Evaluate complete journeys: discovery, setup, everyday use, advanced use, editing, interruption, recovery, history, organization, and cleanup. Follow actions through validation, state changes, persistence, feedback, and subsequent reopening.

EVALUATE PRODUCT QUALITY, NOT JUST DEFECTS

For each area, assess:

- Whether users understand where they are, what matters, and what to do next.
- Whether common tasks require unnecessary steps, decisions, navigation, or repeated input.
- Information hierarchy, typography, spacing, alignment, density, color, contrast, icons, copy, and interaction feedback.
- Whether shared patterns are coherent and domain-specific differences are justified.
- Whether advanced capabilities remain discoverable without overwhelming ordinary use.
- Empty, populated, loading, error, unavailable, archived, and recovery states.
- Compact and wide layouts, foldables, rotation, keyboard visibility, large text, RTL, screen readers, and touch targets.
- Correctness, performance with realistic data volumes, responsiveness, lifecycle recovery, persistence, units, dates, and cross-feature consistency.

Identify worthwhile design opportunities even when the current behavior technically works. Passing tests and matching existing components are necessary evidence, not proof of excellent UX.

Aim for a calm, clear, deliberate interface with strong hierarchy and efficient interactions. Avoid decorative complexity, excessive containers, duplicated information, gratuitous confirmations, and generic styling that weakens Whip’s identity.

DESIGN AND IMPLEMENT COHERENT IMPROVEMENTS

Build a prioritized backlog of observed defects and justified improvement opportunities. Separate evidence from inference and aesthetic preference. Prioritize user impact, frequency, confusion, accessibility, data integrity, and systemic benefit.

Establish a concise design direction from the baseline. For broad visual or interaction changes, implement and inspect a representative complete flow before propagating the pattern.

Fix systemic causes at the appropriate shared layer. Do not force unrelated domain behavior into one abstraction merely to make the code or screens look uniform.

Work in coherent, reviewable increments. Continue across the entire coverage matrix; do not stop after improving the most visible areas.

Operate autonomously within this scope. Resolve routine design and engineering choices yourself. Ask only when a consequential ambiguity genuinely requires my intent, and continue independent work while waiting.

Use the established single-agent workflow unless I explicitly request delegation.

VERIFY THE EXPERIENCE YOU ACTUALLY BUILT

Use disposable emulators, with no more than two running concurrently. Exercise real application journeys as well as isolated components. Visually inspect rendered output; do not infer visual quality from source, semantics, screenshots merely existing, or passing tests.

Capture before-and-after evidence for meaningful changes. Check realistic content, long labels, populated histories, advanced configurations, and failure/recovery behavior.

Use focused checks during development and the appropriate comprehensive regression, build, lint, accessibility, adaptive-layout, and visual review gates for final acceptance. Add meaningful regression coverage where behavior or risk warrants it.

Revisit affected neighboring features and complete a final cross-app consistency review. Update obsolete tests when behavior intentionally improves, while preserving the underlying user guarantees.

Keep this goal to implementation and emulator verification. Physical-phone operations and release/publication are separate follow-ups.

DELIVERY AND COMPLETION

Maintain concise durable findings, decisions, implementation records, and verification evidence. Commit and push coherent verified chunks under the repository workflow, preserving unrelated changes.

Complete the goal when every inventoried area has received a substantive review, all confirmed critical/high-impact defects and justified material improvements are implemented and verified, and the final app has passed the applicable whole-product checks.

Do not manufacture changes to prove effort. An unchanged area is acceptable when its quality has been examined and justified. Do not claim exhaustive verification where coverage is missing.

Finish with a concise account of the principal improvements, significant design decisions, before-and-after evidence, coverage and test results, and any remaining limitations or items requiring real-user validation.
