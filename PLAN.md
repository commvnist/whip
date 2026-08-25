# Whip pre-release plan

This is the authoritative implementation plan. Dated UX audits in `docs/` are evidence and decision records; they do not override this file or the current source.

## Product purpose

Whip is a local-first system for turning intent into action without needless setup:

- **Task:** a finite action or commitment.
- **Habit:** a repeated practice or observation, defined by input method, success rule, schedule, and reminders.
- **Goal:** a desired outcome evaluated from measurements, consistency, or weighted milestones.
- **Workout:** a performed training session backed by reusable exercises, equipment profiles, and routines.
- **Area:** a durable context spanning Tasks, Habits, and Goals.
- **Tag:** lightweight cross-cutting metadata.
- **Metric entry:** internal evidence used by Habits and Goals, not a separate top-level destination.
- **Link:** an explicit relationship that derives evidence between domains.

The resolved feature-scope review is in `docs/UNUSED_FEATURE_INVENTORY_2026-08-22.md`.

## Current implementation contract

- Package ID: `commvne.com.whip.app`; product name: **Whip**.
- Room is currently schema 28. Public releases 0.3.0–0.3.7 shipped schema 27,
  which has an explicit lossless 27→28 migration. Every checked-in pre-release
  schema from 1 through 9 also has an explicit forward path to 28. Migration
  tests preserve existing Tasks and identity emojis, Links, Contributions,
  Automations, Track Entries, entity-tag links, Goal completion snapshots,
  identity values, elapsed Goals, fractional Scales, automation windows, and
  neutral Habit skips across upgrades.
- Complete backups write data version 8 and restore supported versions 5 through 8 with explicit in-memory upgrades. CSV remains an interoperability export, not a complete restore format.
- Task location reminders and Android location permissions do not exist.
- At least one active Area always exists. Area move/delete operations handle Tasks, Habits, and Goals explicitly.
- A Habit control is valid only when it changes logging, evaluation, schedule, reminders, presentation, filtering, or export.
- Habit **Skip Today** is a separate neutral occurrence with visible History/Insights state and Undo; it never creates a metric value. Missing days are derived, not manually logged.
- Goal type constrains direction and compatible aggregation. Log copy is derived from the actual calculation.
- Only Area color is stored because it is consistently rendered. No hidden color metadata is permitted.
- The global Add menu creates first-class objects only. **Log Goal Value** is an Active Goals action.
- Every persisted field must have a named production reader. Raw persistence alone is not a feature.

## UX implementation contract

- Navigation → page identity → view/options → content is the standard hierarchy.
- Creation, mode switches, filters, disclosure, selection, and navigation have distinct shared control patterns.
- Controls that reveal dependent settings keep those settings directly below the enabling control.
- Disabled controls explain both why they are unavailable and how to enable them.
- Naming uses title case for destinations/actions and shared labels for the same concept.
- Compact and open-fold layouts must preserve reading order, safe insets, pane ownership, and symmetric spacing.
- Fresh live screenshots and current source are evidence; checked-in historical screenshots are not.

See `docs/UX_ARCHITECTURE_IMPLEMENTATION_PLAN_2026-08-22.md` for the detailed responsive/control grammar.

## Definition of done for every change

1. The downstream behavior is explicit before a control or field is added.
2. Domain and persistence names describe the same concept shown to users.
3. Unit tests cover calculation/state rules; repository tests cover persistence; UI tests cover consequence and accessibility.
4. `scripts/check` passes.
5. Adaptive changes pass the compact, typical, and expanded emulator matrix;
   optional physical-device checks never run destructive instrumentation
   against a personal release app.
6. A signed release is installed only after the full non-device gate passes.
7. After public release, every schema change includes an explicit forward migration and backup compatibility decision. Destructive resets require new explicit owner approval.

## Remaining release work

The repository release-quality gate is complete at the 2026-08-25 audit
checkpoint. The following owner-controlled distribution activities are outside
that emulator-only audit and are not implied by a passing repository gate:

- Select and capture any new Play Store marketing screenshots after product
  approval.
- Upload the signed App Bundle and confirm Play Console accepts version code 15
  and its permission declarations.
- Create the distribution tag only when the product owner declares the release;
  public local data must never be treated as disposable.
