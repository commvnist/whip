# Whip UI/UX/design critique — 2026-09-06

## Scope and evidence

This review covers every row in the source-linked visual catalog: 171 exact
page, dialog, menu, system, and widget states across Shared, Tasks, Habits,
Goals, Tracks, Gym, Settings, and Organization. The accepted evidence was
captured only on the disposable API 34 emulator in deterministic dark mode.
Every card was reviewed against the eight questions in
`UI_VISUAL_REVIEW_PROTOCOL.md`: purpose, hierarchy, composition, design
language, action grammar, state truth, reachability, and copy.

The first count-complete export was rejected because several labeled files
showed a preceding state or an incomplete test host. The critique below uses
only the corrected family captures recorded in `VER-20260906-014`. The owner
phone was not queried or used.

## Executive critique

Whip has a coherent foundation rather than 171 isolated designs. Primary
pages share stable destination chrome, headings, scoped controls, collection
cards, empty states, and bottom navigation. Editors share one identity,
organization, field, disclosure, validation, and commit grammar. Menus are
short and contextual. Destructive dialogs consistently identify impact and
separate permanent actions. Status, warning, and failure states use semantic
cards instead of color alone.

The strongest remaining inconsistency is inside read-first entity inspectors.
The redesigned Habit Today tab now groups the day into a purposeful summary,
but Task and Goal still lay related evidence directly on the canvas. Goal's
insight block compounds this with dense diagnostic language. One Goal reset
dialog also breaks the otherwise consistent one-cancel/one-confirm footer
grammar by presenting two reset commands around Cancel.

## Family accounting

| Family | Surfaces | Review result |
| --- | ---: | --- |
| Shared | 23 | Pass. Setup/recovery, global search, review, shared pickers, editors, inspector shell, unsaved and destructive dialogs use consistent roles. |
| Tasks | 22 | Pass after remediation. Pages, bulk actions, scheduling, menus, deletion, Overview, and Completed details now use coherent roles. |
| Habits | 19 | Pass. The Today redesign supplies the strongest inspector model; History, Options, entry/timer/pause, and deletion states remain consistent. |
| Goals | 13 | Pass after remediation. Pages, editors, measurement, history, menus, deletion, Overview evidence, and elapsed-reset decisions now share the intended hierarchy. |
| Tracks | 21 | Pass. Collection/detail/activity structure, definition and entry mutation, filters, CSV preview, menus, and deletion reuse the established language. |
| Gym | 44 | Pass. Dense workout and Routine workflows retain domain-specific structure while using shared cards, pickers, dialogs, actions, and state roles. |
| Settings | 14 | Pass. Sections, typed controls, rationale/status surfaces, backup/restore, reset, unit, and emoji flows have clear hierarchy and consequence copy. |
| Organization | 15 | Pass. Area/Tag managers, details, create/rename/color/move/merge/delete states and menus are coherent and visually faithful. |

## Confirmed findings

1. `FND-20260906-003`: Task and Goal inspectors need the same bounded
   read-only information role already established by Habit and shared page
   patterns. The repair should be shared, but action groups must stay visually
   separate rather than becoming cards inside cards.
2. `FND-20260906-004`: elapsed Goal reset needs one footer confirmation. “Reset
   to Now” belongs beside the date/time choice as an alternative, while Cancel
   and “Reset to Chosen Time” remain the dialog decision.

Both findings are resolved and visually accepted in the exact remediated Task
and Goal family captures recorded by `VER-20260906-015`.

## Reviewed non-findings and intentional exceptions

- At enlarged text, destination tabs remain one stable, horizontally
  scrollable row. The initial frame can show a partial trailing destination,
  but every destination retains its full visible/accessibility label and the
  selected destination is automatically brought into view. Existing RTL,
  direct-destination, touch-target, and selected-visibility tests support this
  as an intentional adaptive contract.
- Gray regions around modal-only screenshots are the platform dim layer, not
  an application surface-color inconsistency.
- Gym charts, workout execution, Routine structure/reorder, category
  allocation, and picker grids intentionally retain domain-specific layouts;
  forcing them into generic collection cards would reduce usability rather
  than improve consistency.
- Permanent-deletion dialogs are denser than ordinary alerts because they must
  expose reviewed impact and stale-safe consequences. Their hierarchy remains
  appropriate to risk.

## Closure criteria

The review closes only after both confirmed findings are implemented, focused
behavior tests pass, Tasks and Goals are recaptured and visually accepted, a
fresh whole-catalog export accounts for all 171 surfaces after source freeze,
and the final signed development build is installed and smoke-tested on the
owner phone. The Play Store candidate suite remains intentionally excluded
from this private-phone release.
