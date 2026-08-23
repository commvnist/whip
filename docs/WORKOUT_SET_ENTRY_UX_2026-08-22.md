# Workout Set Entry UX Decision — 2026-08-22

## Evidence

This decision uses fresh captures from the connected Fold and current source code. Repository screenshots from earlier audits were not treated as evidence.

- Before: `artifacts/ux-audit/2026-08-22/set-entry-revamp/before-workout-fold.png`
- Verified result: `artifacts/ux-audit/2026-08-22/set-entry-revamp/final-workout-fold.png`
- The matching UI hierarchy dumps are stored beside the captures.

## Reconciled review

A visual/product designer, a lifting/productivity reviewer, and a UX-architecture reviewer independently selected the same model: **one focused active-set composer with compact surrounding records**. A fully editable table is useful with a mouse but too cramped and error-prone on a phone; a modal for every set interrupts training; expanding every incomplete set makes the page vertically unbounded.

The shared interaction order is:

1. Exercise identity and progress.
2. One active set and any prescription.
3. Previous-set reuse.
4. Required performance inputs.
5. One explicit completion action.
6. User-enabled effort and rare details.
7. Completed/future set ledger, history, setup, and notes.

## Implemented contract

- Only the globally next incomplete set owns an editor. Other incomplete sets are compact `Ready` rows.
- The active set uses one restrained accent surface and one filled `Complete Set` action.
- Load and repetitions use the same integrated 48 dp `− / value / +` stepper grammar.
- Missing values remain neutral until completion is attempted; errors then attach to their fields.
- `Use Previous` copies performance values but not effort or notes.
- Completion never silently creates an extra set. `Add Set` is explicit.
- Completed values stay legible and are never struck through.
- Reorder handles are absent during workout execution; Move Up/Down remains in overflow and accessibility actions at the structural level.
- Exercise setup, cues, and the workout note share one `Exercise Details` disclosure below execution content.
- The rest lane never shows notification-permission prose while idle. Its
  visible `Adjust` action edits a workout-scoped duration used by both manual
  and automatic rest timers without mutating the global default. Preset
  shortcuts are user-managed and persisted; defaults are 1:00, 1:30, 2:00,
  2:30, 3:00, and 5:00.
- Initial entry does not auto-scroll the exercise identity out of view. A successful completion may advance to the next set.
- Supersets/circuits select the designated rotating member before falling back to the next available incomplete set.
- Quick entry shows at most one effort scale. Global settings are mutually exclusive, and exercise configuration exposes one `Effort field` choice: global, off, RPE, or RIR.
- The primary action precedes optional effort/details so it remains visible on the open Fold workout pane.

## Regression requirements

- No error styling or error copy on a pristine set.
- Exactly one active composer.
- Required inputs precede completion in layout and TalkBack order.
- Previous-set reuse populates load and repetitions.
- Both stepper actions remain inside the owning field and expose contextual descriptions.
- Only one of RPE/RIR can appear in quick entry, including legacy settings where both flags were true.
- Completed and future rows have no normal-mode drag handles.
- Group rotation and exhausted-member fallback are unit tested.
- Large text stacks safely; all interactive targets remain at least 48 dp.
- Fresh-device verification must use the live app, not checked-in historical screenshots.
