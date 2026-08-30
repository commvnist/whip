# Full-app UI, UX, design, and QA audit — 2026-08-29

## Scope and method

The baseline for this audit is commit `f7e2dc8` (Whip 0.3.18). Three independent
read-only investigations reviewed the entire product:

- visual language and design-system consistency;
- fundamental UX, accessibility, navigation, and destructive safety; and
- QA, responsiveness, state recovery, deep links, widgets, and test coverage.

An independent direction pass then reconciled their evidence into one product
standard. Findings were checked against production source and existing tests;
an initially reported widget parent-completion bypass was rejected after the
current provider guard and regression test were verified.

No P0 defect or justification for a wholesale visual rewrite was found. Whip's
semantic light/dark theme, restrained shapes, pane-aware dialogs, shared action
controls, 48 dp targets, fold/rail architecture, responsive Review workspace,
and safe widget transparency are strong foundations and must not regress.

## Direction

Whip is a trustworthy command center, not a collection of feature dialogs.

1. Never show false emptiness. Loading, failure, stale content, and genuine
   empty state are distinct.
2. Never lose work or data silently. High-effort drafts are protected and
   destructive replacement requires a deliberate second decision.
3. Return users to context. Root destinations preserve meaningful route and
   selection state unless an explicit deep link or invalid record overrides it.
4. Adapt workspaces, not just navigation. Search, Settings, choice surfaces,
   and widgets follow the compact/rail/fold/wide discipline already used by
   Review.
5. Semantics are typed. Status tone, selection, error, and destructive meaning
   come from state rather than display copy or local color choices.
6. Consolidate while touching. Systemic primitives replace the highest-risk
   outliers incrementally; there is no mechanical restyle.

## Accepted findings and implementation phases

### Phase 1 — integrity

- Add recoverable error/retry state to Tasks and Tracks.
- Prevent Home from showing onboarding or clear-day conclusions until every
  visible domain settles; keep successful sections usable during partial
  failure and expose actionable domain errors.
- Resolve entity/deep-link targets only after the owning domain loads, including
  Tracks; stale records are consumed once with explicit unavailable feedback.
- Add a second destructive gate before **Replace Everything**.
- Protect dirty Workout and Workout Group drafts on every dismissal route.
- Bound arbitrary Area and exercise choice lists and expose one correct
  radio/checkbox semantics node per option.
- Replace copy-derived inspector colors with typed `WhipStatusTone` and
  `WhipStatusBadge`.
- Move Whip's full data reset entry into the danger-zone visual grammar.

### Phase 2 — adaptive resilience

- Make global search a compact full-screen and wide two-pane workspace with a
  sticky query, lightweight results, and settled-result live announcements.
- Move Track CSV reads, writes, generation, and preview parsing off the main
  thread; add limits, cancellation, progress, and stage-specific errors.
- Preserve CSV mapping and nested automation drafts across recreation without
  putting full CSV payloads in the saved-state bundle.
- Preserve the last successful widget snapshot and distinguish refresh failure
  from real empty data.
- Add minimum-height/large-font widget variants and source preview colors from
  widget resources.
- Preserve root route/selection/filter context, make the short wide Settings
  sidebar scrollable, and auto-mirror shared navigation chevrons.

### Phase 3 — design-system consolidation

- Explicitly own the heavily used `titleSmall`, `bodySmall`, `labelMedium`, and
  `labelSmall` typography roles.
- Establish canonical collection-card, metric-tile, notice, and Settings-section
  roles and migrate touched/high-drift surfaces incrementally.
- Use spacing tokens in shared infrastructure and prevent new arbitrary content
  rhythm without banning legitimate geometry, charts, touch targets, or
  breakpoints.
- Extract user-facing strings from every touched/shared surface and prevent new
  literals; a complete legacy localization retrofit remains a separate project.
- Strengthen the visual matrix with bounds, visibility, overlap, reachability,
  large-text, nested-dialog, and widget assertions.

## Implementation closure

All accepted release-scope findings above are implemented in Whip 0.3.19.
The work was iterated through the three specialist reviews, an opinionated
direction synthesis, a post-implementation direction audit, and a final
blocker-closure review.

- Domain state is now explicit across Home, compact content, adaptive support
  panes, entity launch routing, and widgets. Loading, failed, cached/partial,
  empty, and ready states no longer collapse into one another; every failed
  Track support surface exposes retry.
- Destructive replacement has a second decision, full reset has a synchronous
  single-submission latch, dirty Workout and Workout Group saves close only
  after persistence succeeds, and bulk Task completion reviews unfinished
  subtasks before mutation. Externally shared Task drafts are explicitly
  protected as unsaved work, and trailing subtask completion owns a distinct
  48 dp target instead of overlapping the adjacent conversion action.
- Root destinations preserve their own route and selection state. Launch
  delivery identity survives Activity recreation while distinct new intents
  remain independently deliverable.
- Search is a full-window adaptive workspace. Its cross-domain index is built
  away from the UI thread directly into per-domain bounded storage, and any
  bounded source is presented as incomplete rather than a definitive miss.
- Track CSV uses bounded cancellable background I/O, compact saved-state
  descriptors, persisted document permission where available, automatic
  revalidation after process recreation, guarded import dismissal, and
  recoverable preview/write failures.
- Widget configuration, headers, collection actions, expansion, selection,
  snapshots, retry rows, transparency, RTL padding, night colors, and extreme
  text behavior follow the same interaction and semantic grammar as Whip.
- Shared collection, metric, notice, Settings-section, status-tone, spacing,
  typography, trailing-action, and directional-navigation roles now anchor the
  touched high-drift surfaces. New copy on audited surfaces is resource-backed.

Verification includes all 363 JVM tests, all 407 tests in the disposable API 34
emulator suite, Jacoco-instrumented compilation, release lint, a verified
signed and minified 0.3.19 APK, focused failure/recreation matrices, and
additional checks for 125-result pagination, IME/Escape, compact/fold/expanded
Track truth, reset rapid double-tap, Task and Habit widgets at 3.2× text,
day/night widget contrast, and every typed status tone in light, dark, and
dynamic themes. Physical-device destructive instrumentation was intentionally
not used.

## Rejected or deferred advice

- **Widget parent completion bypass:** rejected as obsolete; the current build
  expands unfinished subtasks and has regression coverage.
- **Mechanical replacement of every raw dp, Card, or Surface:** rejected;
  explicit geometry is valid for breakpoints, charts, and touch targets.
- **Full legacy string localization in this iteration:** deferred as an XL
  migration; no-new-literals and touched-surface extraction are accepted now.
- **Variable inspector frame height:** deferred. Stable inspector height
  prevents tab-to-tab jumping; sparse empty space is lower risk.

## Required acceptance matrix

- Home: all-loading, partial-loading, one-failed, mixed success/failure,
  all-ready-empty, and historical-only states at compact/rail and 1×/2× text.
- Tasks/Tracks: Loading → Error → Retry → Content and repeated failure.
- Entity routes: found, scoped-out, still-loading, deleted, and duplicate
  deliveries for every domain.
- Restore/reset: first Replace, cancel second gate, final confirm, and busy
  double-tap in light/dark themes.
- Dirty editors: pristine and dirty Back/outside/Cancel, Keep Editing, Discard,
  and Save.
- Choices: 0, 1, and 30+ items at 320×480 and 2× text, with a single correct
  semantic option node and reachable final item/actions.
- Status: every tone in light/dark/dynamic themes, independent of label copy.
- Search: IME open; 0, 1, 50, and 100+ results; filters; compact/rail/wide;
  Enter/Escape and screen-reader announcement.
- CSV: malformed, near-limit, over-limit, cancel, write error, and recreation.
- Widgets: successful empty, populated, first failure, failure after success,
  220×160 and larger, light/night, and 1×/2×/3.2× text.
- Settings/context/RTL: short wide panes, root round-trip, invalid selection,
  explicit deep-link override, and mirrored shared navigation.

Automated visual acceptance must assert critical element bounds, visibility,
overlap, minimum targets, and scroll reachability. Opaque screenshot corners
alone do not constitute a visual pass.

This matrix is intentionally implemented as a coordinated suite rather than a
single oversized test: `VisualAcceptanceMatrixTest` owns adaptive chrome,
theme/font/RTL screenshots, 31-item scroll reachability, nested second-gate
dialogs, bounds, and explicit 48 dp actions; `UnifiedSearchAdaptiveUiTest` owns
compact/wide search, 125-result pagination, IME/Escape, and announcements;
`WhipWidgetAreaScopeTest` owns both factories' empty/populated/first-failure/
failure-after-success behavior; `WidgetExtremeTextTest` owns minimum-size widget
layout at 3.2× text and day/night contrast; `StatusToneThemeUiTest` owns all
typed tones in light, dark, and dynamic themes; and `SafetyChoiceUiTest` owns
destructive second gates, dirty-draft dismissal, retry, and option semantics.
