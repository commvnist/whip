# Whip live UX, design, aesthetic, QA, and power-user audit — pass 2

Date: 2026-08-23  
Evidence: fresh signed release `commvne.com.whip.app` version `0.3.9` (15) on a Samsung SM-F976W.  
Evidence directory: `screenshots/raw` and `ui-dumps` beside this document. Repository screenshots from earlier audits were not used as visual evidence.

## Product rule adopted by this pass

Whip has two icon systems with different jobs:

1. **Identity emoji** identify user-created Habits, Goals, and Tracks. They are editable data, use one shared preset/custom picker, and render in an optically centered container.
2. **Interface icons** communicate navigation, actions, selection, disclosure, state, and editing. They use the Material vector set with labels or accessibility descriptions. They are never stored as user data and are not replaced by emoji.

This distinction preserves the warmth and flexibility requested for identity while avoiding the inconsistent metrics, ambiguous meaning, and unreliable screen-reader behavior that would result from using emoji for functional controls.

## Synthesized findings

### Critical consistency and aesthetic findings

- **C-01 — Mixed Track icon language:** the live Track editor mixes `▤`, `♟`, `✓`, `◆`, `◫`, and `✦` with full-color emoji. The glyphs have different baselines, weights, and apparent sizes.
- **C-02 — Three incompatible identity pickers/defaults:** Tracks use a hard-coded chip grid; Habits and Goals use a dropdown plus a two-character custom field; defaults and templates are mostly typographic symbols. Custom multi-code-point emoji can be truncated.
- **C-03 — Functional checks are text:** Area, unit, page, sort, and generic selection menus prepend or append `✓` as text. This repeats the same baseline problem and does not expose a consistent selected visual.
- **C-04 — Primary editor grammar diverges:** Task and Track editors use a full content-pane surface with fixed top actions, while Habit and Goal editors use tall rounded cards with bottom actions. In the live Fold view, Goal supporting copy is clipped near the bottom action row and the editor feels like a long alert rather than a primary workflow.
- **C-05 — Narrow pane tabs do not budget for Pages:** the live Tasks view truncates Upcoming and Anytime; Goals truncates Completed. The tab capacity calculation ignores the width consumed by the labeled Pages control.
- **C-06 — Gear semantics change by layout:** in the normal open-Fold workspace the gear opens a mixed “Expand Content / Open Settings” menu, although a gear conventionally means Settings. A mixed application-actions menu needs an overflow affordance.

### UX and novice findings

- **U-01:** selecting or changing an identity marker should be optional, compact while inactive, and identical in every supported editor.
- **U-02:** presets need human names and large targets; custom emoji needs explicit validation and helpful copy instead of a UTF-16 character limit.
- **U-03:** validation and consequences must remain next to their enabling control. The existing dependent-setting notices are retained.
- **U-04:** prose must use sentence case even when domain names are title-cased in navigation. Fresh evidence includes “goals and Tracks,” “Goal progress,” and “Every Entry.”
- **U-05:** background workspace controls must remain dimmed and inert while any primary editor is open.

### QA and accessibility findings

- **Q-01:** Track icon values are not validated; backup restore/merge can write arbitrary icon strings directly.
- **Q-02:** legacy symbol values need a deterministic one-time database migration so the database, not only the UI, satisfies the new invariant.
- **Q-03:** custom emoji must support variation selectors, skin tones, flags, and joined emoji without accepting arbitrary words or two unrelated emoji.
- **Q-04:** emoji tiles need single-choice semantics, a selected state, stable 48 dp or larger targets, and names that do not rely on the screen reader interpreting the emoji glyph.
- **Q-05:** the release gate needs domain tests for emoji normalization, UI tests for preset/custom selection, migration/restore coverage, adaptive editor geometry, narrow destination labels, and a static guard against legacy identity defaults.

### Power-user findings

- **P-01:** the preset is a shortcut, not a whitelist. Any valid single emoji sequence can be pasted, including a skin-tone or joined emoji.
- **P-02:** changing an emoji must remain available during later edits and must not change identity, links, history, ordering, or automations.
- **P-03:** keyboard/D-pad users need focusable tiles and a direct custom-entry path; no hidden long-press-only functionality is introduced.
- **P-04:** the Track field model, CSV, filters, sorting, bulk operations, automations, and advanced controls remain intact. This visual pass must not reduce Track’s first-class utility.

## Implementation specification

### Workstream 1 — shared identity emoji system

- Add domain constants and validation/normalization for Habit, Goal, and Track defaults.
- Provide a curated cross-domain preset with accessible labels and a custom-emoji path.
- Accept one emoji grapheme-like sequence, including variation selectors, modifiers, flags, keycaps, and ZWJ sequences; reject prose and unrelated multiple emoji.
- Render identity emoji through one optically centered composable.
- Replace all Habit, Goal, and Track defaults/templates/fallbacks with emoji.
- Replace Track’s private grid and the old Habit/Goal dropdown with the shared picker.
- Preserve Material vector icons for interface actions.

### Workstream 2 — stored-data integrity

- Bump Room from schema 3 to 4 and install an explicit migration that converts every legacy or invalid identity icon to a valid emoji without deleting user records.
- Normalize repository writes and backup restore/merge values so invalid identity data cannot re-enter the database.
- Keep stable IDs, relations, logs, entries, and automation references unchanged.

### Workstream 3 — interaction and editor consistency

- Introduce a full-pane primary productivity editor surface for Habit and Goal, matching Task/Track geometry: rectangular owned pane, fixed Close/title/Save bar, one scrollable body, and safe bottom clearance.
- Keep small confirmations, pickers, and destructive decisions as dialogs.
- Use exact content-pane width on a book Fold and full width on compact; never straddle the hinge.
- Replace textual checkmarks in menus with a vector Check leading/trailing icon.
- Change the mixed Fold application-actions trigger from a gear to overflow; retain a gear only for a direct Settings action.

### Workstream 4 — density, hierarchy, copy, and adaptive fit

- Budget destination tabs for the Pages control and use compact Pages padding so labels remain whole at phone/Fold content-pane widths and elevated font scales.
- Keep no more peer tabs visible than can fit; the active secondary destination must remain visible and selected.
- Correct sentence-case domain references in the audited surfaces.
- Replace star/circle text used as functional Gym state with vector icons where touched by this pass.

## Acceptance criteria

- No Habit, Goal, Track default, template, or fallback uses an ASCII/typographic identity glyph.
- The same picker is visible in all three editors; all preset tiles are centered and all custom emoji sequences round-trip unchanged.
- Invalid custom text cannot be applied or persisted.
- Existing schema-3 user records open on schema 4 with valid emoji and unchanged object/history counts.
- Menus no longer build selection state by concatenating `✓` into labels.
- Habit and Goal editor screenshots match Task/Track ownership and fixed-action hierarchy in compact and book-Fold layouts.
- Tasks, Habits, Goals, and Track-detail page labels remain readable beside Pages at 360–412 dp and at 200% font scale.
- JVM tests, lint, release APK/AAB, all disposable-emulator instrumentation, package/version/permission checks, and fresh live smoke tests pass.

## Review pass

After implementation, repeat the five lenses in this order: QA failure scan, narrow/adaptive geometry, novice task completion, power-user reachability, and aesthetic consistency. Any regression found in the repeat pass is fixed before release installation.

## Implementation and verification result

Completed on 2026-08-23.

- Identity data now uses one shared emoji policy across Habits, Goals, and Tracks. The preset contains 30 named choices; a validated custom path accepts one emoji sequence, including flags, skin tones, keycaps, and joined emoji.
- Every identity emoji renders through a fixed, optically centered container. Material vector icons remain the sole language for navigation, actions, disclosure, and selection.
- Room schema 4 migrates legacy symbols in place. Repository writes and backup restore/merge normalize identity values at the data boundary without changing IDs, history, links, or automation references.
- Habit and Goal creation now use the same full-pane, fixed-action editor grammar as Task and Track. Close, title, and Save stay reachable while only the editor body scrolls.
- Destination navigation preserves four short destinations when they fit and uses a labeled Pages menu when a longer workspace or elevated font scale needs the space. Selected hidden pages are promoted into the visible row.
- Text checkmarks in selection menus were replaced with Material Check icons; the Fold layout/settings affordance now uses an overflow icon with explicit actions.
- Instructional prose uses sentence case; functional Gym star/circle glyphs touched by this pass use Material icons.

Fresh post-implementation evidence is in `screenshots/implemented`. It covers the open-Fold Track editor and emoji grid, Tasks navigation, Habit editor, Fold app-actions menu, and the physical closed-Fold Home, Track editor, and emoji grid. Both physical configurations were captured from the installed signed release, not repository fixtures.

Verification completed:

- 210 JVM tests passed.
- 226 Android instrumentation tests passed on a wiped disposable API 34 emulator, including the Room 3→4 migration, emoji preset/custom selection, 200% text, Fold geometry, accessibility, deep links, backup, first-class Track flows, and cross-domain creation journeys.
- Debug lint, release lint, optimized signed APK, signed AAB, and benchmark harness passed.
- Installed package `commvne.com.whip.app` version `0.3.9` (15) was hash-verified on the physical Fold after installation.
