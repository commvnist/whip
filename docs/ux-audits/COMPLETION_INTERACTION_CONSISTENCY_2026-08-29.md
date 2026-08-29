# Completion interaction consistency audit — 2026-08-29

## Scope and evidence

This audit reconciles three independent read-only reviews of the current app
and widget implementation:

- a UI-system inventory of Task, Habit, Goal, search, archive, and widget
  surfaces;
- an interaction and accessibility review covering touch reach, semantics,
  large text, and right-to-left layouts; and
- a five-persona simulated focus group covering right-handed and left-handed
  operation, motor accessibility, large text, and frequent widget use.

The persona exercise is design critique, not recruited-user research. Its
findings were treated as hypotheses and checked against the code, emulator
behavior, accessibility semantics, and automated UI geometry tests.

## Reconciled findings

1. Completion and selection had overlapping checkbox grammar. Completion now
   uses Whip's success green; selection retains the active selection color.
2. Parent Tasks used a trailing completion lane, but subtasks and checklist
   items placed completion before their text. Finite child work now uses the
   same logical trailing lane across Tasks, Habit checklists, and Goal
   milestones.
3. Completed app rows did not consistently match the widget. Finite completed
   titles now remain full-opacity, use the muted content color, and receive a
   complete strike-through. Durable Habit identity remains unstruck because a
   Habit continues beyond one completed occurrence.
4. Child controls needed clearer separation and larger targets. Rows now expose
   at least a 48 dp completion target with an explicit 8 dp content gap, while
   the whole child row remains toggleable.
5. Large text could compress a child action and checkbox into the title.
   Secondary actions move below the title at narrow widths or enlarged font
   scales.
6. A widget Task parent could complete while unfinished subtasks remained. It
   now shows an indeterminate state and expands or opens the Task for review.
7. History, Archive, and Search exposed inconsistent state cues. Completed and
   archived Tasks now present state without an invalid active completion
   affordance, and search includes non-active status in supporting text.

## Intentional consistency rules

- "Right side" means the logical trailing edge so the layout mirrors correctly
  in right-to-left locales.
- Task and Habit child rows use the same completion spacing, touch target,
  strike-through, and accessibility toggle semantics in app and widget.
- Expansion and edit remain separate actions from completion.
- Habit identity is durable; only a completed checklist item is crossed out.
- Data-entry controls in Gym, Track fields, and selection mode are not
  completion controls and keep their domain-specific behavior.

## Verification criteria

- completion is trailing on every finite-work row;
- completed finite-work titles are muted and fully struck through;
- visible completion controls have a 48 dp target and at least 8 dp text gap;
- child-row toggles announce checkbox role and current state;
- large text does not overlap completion and secondary actions;
- widget parents cannot bypass unfinished Task subtasks; and
- app and widget preserve readable contrast in light, dark, and transparent
  widget configurations.
