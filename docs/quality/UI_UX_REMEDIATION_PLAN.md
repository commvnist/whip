# Fast whole-product UI/UX remediation plan

## Outcome

Audit every user-reachable route, dialog, state, and platform handoff without
turning each finding into a whole-app research project. Fix shared causes before
leaf screens, preserve Whip's warm visual identity, and make interaction grammar
consistent for mobile, enlarged text, keyboard/screen-reader users, distracted
gym users, ADHD users, and power users who edit and customize heavily.

## One canonical design contract

Before judging individual screens, freeze the reusable contract:

- spacing, typography, shape, color, elevation, icon, and motion tokens;
- page, pane, section, row, card, dialog, sheet, tab, menu, form, and status
  primitives;
- primary, secondary, low-frequency, destructive, disabled, loading, selected,
  completed, paused, archived, warning, and error hierarchy;
- 48 dp minimum touch targets, visible focus, semantic names/roles/state, logical
  focus order, non-color state cues, and scroll-to-error behavior;
- compact phone, 320 dp/200% text, standard phone, fold/tabletop, tablet, RTL,
  light/dark, keyboard, and TalkBack expectations.

A screen may diverge only when its job requires it. The exception and reason go
in the gap ledger so accidental local dialects cannot become precedent.

## Fast audit loop

### 1. Generate the surface inventory

Build one machine-readable row for every destination, subdestination, dialog,
sheet, picker, editor, empty/error/loading state, notification/widget entry,
document-provider handoff, and destructive confirmation. Link each row to its
route, composable, state owner, mutation boundary, and existing tests. This is
the completeness check; screenshots alone are not the inventory.

### 2. Scan for shared-code outliers

Use static searches and small policy tests to flag:

- direct Material dialogs/buttons/cards where a Whip primitive exists;
- raw colors, radii, spacing, sizes, typography, or duplicated layout constants;
- fixed-height scroll content, nested cards, clipped actions, or pane-local
  constraints that can recreate the tiny routine-editor failure;
- optimistic dismissals, callback-only saves, hidden prerequisites, inactive
  controls, stale fallback state, duplicate semantics, icon-only destinations,
  hard-coded copy, and color-only state;
- custom controls without minimum target, keyboard action, content description,
  selected/expanded state, or error association.

Static findings are leads, not automatic defects. Each must be confirmed in its
real workflow before implementation.

### 3. Audit by screen family, not file order

Review one representative and then every exception in each family:

1. App shell, Home, global add/search/review, primary navigation.
2. Collection lists, compact/expanded rows, tabs, filters, archive/history.
3. Basic and advanced creation/edit forms, pickers, dependency controls.
4. Detail/inspector pages and secondary actions.
5. Quick actions, historical corrections, destructive and failure flows.
6. Gym library/routines/program builder/active workout/history.
7. Settings, backup/reset, Health, permissions, units, diagnostics.
8. Widgets, notifications, share/deep links, document and system surfaces.

For each surface exercise ready, empty, loading, recoverable error, validation
error, success, destructive review, keyboard-open, large-text, and narrow-width
states where applicable.

### 4. Score and fix

Record observed behavior, expected behavior, affected users, severity, route,
source, screenshot/hierarchy evidence, shared cause, proposed fix, and regression
test. Prioritize with:

`score = severity × frequency × affected-users × error-cost ÷ implementation-cost`

- P0: destructive, inaccessible, data-integrity, or impossible primary path.
- P1: misleading state, blocked workflow, severe mobile/large-text issue, lost
  draft/context, or inconsistent high-frequency interaction.
- P2: discoverability, avoidable effort, hierarchy, density, or cross-area
  inconsistency.
- P3: visual polish with no material workflow or comprehension cost.

Fix the shared token/component/state owner first when it safely resolves three
or more surfaces. Otherwise make a bounded leaf fix. Do not build a universal
UI abstraction merely to eliminate similar-looking code. Every confirmed gap is
implemented in the same screen-family wave; only false positives or unavoidable
platform-owned behavior may be closed without a code change, and the evidence
must say why.

### 5. Verify proportionately

Use three test tiers:

- Inner loop: compile plus the exact JVM class and Android method being changed.
- Chunk gate: affected domain/repository classes and the relevant screen-family
  Compose suite; repeat a timing-sensitive regression three times in isolation.
- Release gate: one complete JVM/lint/coverage/emulator run and one release build
  only after product source is frozen.

The instrumentation runner should cache successful batches by a signature of
production source, build configuration, the batch's class list, and those test
files. An unchanged successful batch can be reused after a test-only correction
in another batch; any production-source change invalidates all device batches.
Every resumed final report must still prove that every current test belongs to
one successful exact-signature batch with zero skips.

### 6. Close each coherent chunk

Update the durable gap ledger, run the targeted gate, review the diff, commit and
push the related change, and confirm upstream reachability. Do not accumulate a
multi-area visual rewrite in one commit. Run the full cross-product gate once at
the end, then perform the final physical-device release only when explicitly
requested.

## Immediate trajectory

1. Remove obsolete migration/compatibility behavior behind an explicit breaking
   reset boundary; establish one canonical fresh-install schema and backup format.
2. Enumerate the concrete unresolved members of `FND-20260831-019` and
   `FND-20260901-027`; close stale umbrella wording where all real members are
   already resolved.
3. Implement signature-aware targeted QA commands and batch resume.
4. Run the screen-family audit, starting with shared shell/controls and the
   high-frequency Gym, Task, Habit, Goal, Track, and Settings paths.
5. Fix P0/P1 issues first, then shared P2 consistency improvements and bounded
   P3 polish.
6. Execute one final adaptive/accessibility/cross-product acceptance campaign.

## Current execution state — 2026-09-02

- Shared editor chrome, labels, section hierarchy, repeated-control semantics,
  bounded search, Gym catalog mutation ownership, and compact Routine detail
  navigation have been remediated and targeted-tested.
- The Gym screen-family matrix has passed 320 dp/200% text, full-height Routine
  exercise editing, active-workout composition, timer actions, progressive
  disclosure, theme/font, and RTL coverage.
- The breaking model candidate now uses one current persistence contract and one
  canonical 5/3/1/work-placement vocabulary. Adjacent Settings validation now
  moves focus and scrolls to the first invalid custom-unit field.
- The remaining execution step is the frozen-candidate complete emulator,
  accessibility/lint/coverage, and release-build gate. Physical-device release
  remains an explicit user action.

## Definition of done

- Every inventory row has an explicit pass, accepted exception, or linked fix.
- Every visible control has a valid configuration and outcome path.
- Shared states and actions use one understandable visual/interaction grammar.
- Narrow, enlarged-text, RTL, keyboard, and screen-reader paths remain usable.
- No unresolved P0/P1 remains; every P2 is fixed or explicitly rejected with a
  product reason; P3 decisions are recorded rather than silently forgotten.
- Targeted checks are the normal development loop; the complete suite runs once
  per frozen candidate and produces exact execution accounting.
