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
the corrected family captures recorded in `VER-20260906-014`, the remediated
Task/Goal families in `VER-20260906-015`, and the final accepted frozen export
in `VER-20260906-017`. The owner phone was not queried or used during capture
or review.

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

## Frozen acceptance

The post-remediation audit executed 61 emulator tests in seven fresh batches
with zero failures, skips, or result reuse. It exported exactly 171 PNG and 171
paired semantics files, with no missing or unexpected surface IDs. All 23
generated contact sheets were reviewed. The review initially rejected four
popup artifacts whose files existed but whose menus were absent; after exact
state gates were added, the replacement frozen run shows all four popups in
both pixels and semantics. No additional product-design finding remained.

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

The UI review and remediation is closed: both confirmed findings are
implemented, focused behavior tests pass, Tasks and Goals are visually
accepted, and the replacement frozen whole-catalog export accounts for all 171
surfaces after source freeze. Whip 0.3.52/code 58 was subsequently installed
in place and smoke-tested on the owner phone in `VER-20260906-018`, closing the
overall delivery. The Play Store candidate suite remains intentionally
excluded from this private-phone release.

## Current-source 172-surface revalidation

The later Count Time Since work added one authored elapsed-display surface, so
`FB-20260906-012` re-audited the authoritative 172-row catalog instead of
assuming that the accepted 171-surface result remained complete. The initial
fresh export ran 62 tests with zero failures/skips, produced 172 PNG/XML pairs,
and was reviewed across all 23 family contact sheets. Its pixels confirmed the
same coherent navigation, page, editor, action, modal, destructive, and empty-
state grammar described above; the restrained multi-unit Goal metric also fits
that system. No new visual-design defect was found.

The semantics review did find one cross-editor accessibility seam:
`FND-20260906-011`. The Task editor's Repeat, Separate Deadline, and Time
switches were visually labeled but the interactive nodes did not own those
labels. A shared labeled-switch modifier now covers those controls and the
corresponding standalone Task/Habit switches. The capture harness also fails
closed on any future `NAF="true"` node.

The exact Whip 0.3.55/code 61 replacement export is
`/tmp/whip-ui-goal-final-20260906-172/index.html`; its manifest SHA-256 is
`f430cfce8277d94bda6dad6c12c382e039144c64fd8fef0cb05e8ec2380549a3`.
All 172 PNG/XML pairs are present, no hierarchy contains an unlabeled
interactive node, and exact final Task/Habit/Goal/About inspection shows no
pixel regression beyond the expected About-version line. The accompanying
fresh complete matrix passed all 1,575 product tests with no failure, skip, or
Android result reuse. `VER-20260906-023` is the detailed acceptance record.

This revalidation closes with no known residual whole-app UX/UI/design defect.
As before, the private-development result does not replace Play Store
qualification, and subjective comfort remains appropriate to validate through
normal owner use.
