# Whip full-app design and UX audit — 2026-08-24

## Scope and evidence

This pass evaluates Whip as one product rather than as a collection of feature
screens. The evidence set is the current source, current Compose semantics, the
disposable-emulator interaction suite, and a fresh signed-release physical
smoke check. Historical repository screenshots are not treated as current
evidence.

The audit uses three lenses:

- **Consistent design:** whether equivalent actions, hierarchy, state, color,
  spacing, and navigation use the same visual language.
- **General design:** whether the layout has a clear focal point, sensible
  density, bounded reading widths, useful large-screen composition, and good
  visual rhythm.
- **UX design:** whether controls reveal their consequences, remain reachable,
  explain unavailable states, preserve context, and work for both simple and
  advanced use.

## Findings and decisions

| Priority | Finding | Design decision | Implementation |
| --- | --- | --- | --- |
| P1 | Fold dialogs could be centered on the whole display and cross the hinge, while destination overlays did not share one ownership rule. | Small transient surfaces belong to the active content pane; true destinations own the full window and apply safe insets to their content. | All transient AlertDialogs now use one pane-aware dialog primitive. Primary editors retain their pane-owning full-window host; Areas and Review use an opaque destination surface with safe content insets. |
| P1 | Large layouts often showed generic summary filler beside the real destination. Settings and Tracks felt like widened phone screens rather than first-class tablet/Fold experiences. | Supporting panes must be destination-owned and actionable. | Home, Tasks, Habits, Goals, Tracks, and Settings now provide real support content. Tracks and Settings use navigable master/detail structures. |
| P1 | Navigation hid useful pages behind an equally prominent menu even when enough width remained. Labels could also truncate at elevated font scales. | Fill available capacity with real destinations first; expose only true overflow through a lower-emphasis **More** action. | The shared destination bar now budgets width, long labels, and font scale, promotes the selected destination, and shows up to four direct pages when they fit. |
| P1 | Task, Habit, and Goal cards taught different scan and action patterns. Some whole-card taps edited while others opened details. | Cards share an identity column, content column, state/action column, and explicit edit affordance. Whole-card taps open details; the pencil edits. | Productivity card geometry, semantics, state colors, and behavior are unified and protected by a cross-domain UI test. |
| P1 | Editor controls were organized by data shape instead of cause and effect. For example, repeat-dependent fields and reminder-dependent fields could appear far from the switch that enabled them. | Put dependent controls directly below their enabling choice and organize editors into named task-oriented sections. | Task, Habit, and Goal editors now use Basics, Schedule/Target/Tracking, Organization, Planning/Details, and related sections. Repeat, weekday, deadline, pace, and reminder dependencies are adjacent to their triggers. |
| P2 | Review & Trends used full-width rows and text sparklines, then retained a transient-dialog width cap after becoming a destination. On a Fold or desktop-width window it looked like a phone surface placed inside the larger canvas. | Reviews are a true adaptive dashboard: a compact single-scroll destination, a sidebar plus responsive overview on flat wide windows, and controls/hinge/overview as three explicit regions on a book Fold. | The 720 dp dialog constraint is removed. Wide Review owns the destination window, places period/sections/saved views in a persistent control pane, uses the remaining canvas for a responsive 1/2/4-column signal grid and correlations, and reserves the physical hinge as a non-content gutter. |
| P2 | Areas exposed high-impact bulk movement alongside routine row actions, and its usage summary omitted domains once more than two were present. | Keep routine actions direct, destructive/bulk actions in overflow, and make impact summaries complete. | **Move All Items…** is in overflow; usage and organization copy covers Tasks, Habits, Goals, Tracks, and Track Entries. |
| P2 | Track Entries mixed “open” and “edit”; the editor title could be derived from a primary field, and the compact “Auto” label was vague. | Entries open a read-only detail surface, edit through a pencil, and use object-level titles. Track navigation calls the causal workspace **Rules**. | Entry cards, details, editor titles, nav labels, empty states, and navigation tests use the new grammar. |
| P2 | Search filters consumed more visual weight than the results and active filter chips could become undersized. | Inactive machinery stays compact; active state remains visible and touchable. | Search uses a compact Filter action with a count, 48 dp active chips, and a pane-aware filter surface. |
| P2 | Most accent states had been collapsed into the same pink action color, making success, warning, selection, and destructive states hard to distinguish. | Color communicates stable semantic roles, not feature identity. | Action/selection remains primary; completion/success is green; skip/warning is amber; destructive/error is red. Static architecture tests prevent collapsing these roles again. |
| P2 | Several controls used 44 dp targets, and the Fold rail home affordance was icon-only. | Every interactive target is at least 48 dp and important global routes have visible labels plus semantics. | Shared tabs, search chips, Home, and reorder controls meet the target; the rail exposes a visible **Home** route. |
| P2 | High-frequency copy was inconsistent or implementation-oriented: “Placement,” bare “High,” raw ISO dates, duplicate Settings headings, and Track “Auto.” | Labels describe the user's decision: **When**, **Priority: High**, **Effort: High**, localized dates, **Rules**, and concise non-duplicated page identity. | Audited shell, cards, Track, editor, Gym chart, Settings, search, and review copy now follows this rule. Shared navigation/action strings and Track entry plurals are resource-backed. |
| P3 | Very wide content lines and surfaces weakened scanability. | Use content bounds by task: 560 dp transient dialogs, 920 dp reading/editing content, and 1200 dp dashboards. | Shared width tokens and centered bounded content are applied to audited surfaces. |

## Resulting design language

The product hierarchy is now:

1. app destination and Area context;
2. page identity;
3. page destinations or view mode;
4. compact search/filter/overflow actions;
5. content and one clear creation path.

The interaction grammar is:

- tap a card to inspect it;
- use its state control to complete/log/advance it;
- use the pencil to edit it;
- use overflow for infrequent, destructive, or bulk actions;
- use tabs for peer destinations and buttons for actions;
- keep advanced controls available without giving them the visual weight of
  the normal path.

## Regression contract

The implementation adds or updates checks for pane-safe modal ownership,
destination capacity, adaptive support panes, card consistency, 48 dp targets,
semantic color separation, editor section hierarchy, localized shared chrome,
navigation reachability, large text, and Track entry behavior. The full release
gate and physical signed-release observations are recorded below.

## Final verification

- `./scripts/check` passed with 246 deterministic JVM tests, Android-test
  compilation, lint, the debug build, and all coverage thresholds.
- `./scripts/check --full` passed with the optimized release and benchmark
  builds. The complete 275-test Compose/instrumentation inventory was exercised
  on an isolated disposable API 34 emulator in bounded batches; every affected
  class was rerun after expectation updates and finished green.
- Compact, expanded, and separating book-fold navigation, modal ownership,
  support panes, large text, and destination reachability are covered by the
  adaptive instrumentation suite. Physical-device inspection was intentionally
  non-destructive and used the connected compact Fold display.
- The signed `commvne.com.whip.app` release (version 0.3.9, version code 15) was
  installed without clearing user data. The built and installed APKs have the
  same SHA-256 hash:
  `31d311de751780c53d21686dd42478d33287a4d258bbef231697d38aa1a8ce9d`.
- Fresh release evidence: [Home](live-compact-home-final.png),
  [Tracks](live-compact-tracks-final.png),
  [Track details](live-compact-track-detail-final.png),
  [Settings](live-compact-settings-final.png), and
  [Review & Trends](live-compact-review-final.png). The subsequent wide-layout
  redesign is captured in [wide Review & Trends](review-wide-redesign-final.png).
  Matching accessibility-tree
  dumps are stored beside the images. Development artifacts were captured only
  through `scripts/device-artifacts` under the designated `whip-debug` device
  directory.
