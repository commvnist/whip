# Action layout design inventory — 2026-09-30

Owner: design/inventory worker. Consultant owns independent critique and acceptance; implementation and QA have separate owners.

## Observed inconsistency

The user’s second “Habits” refers to Goals: the live Goal card has a trailing **Log** control and centered **Complete Goal** footer. The live Duration Habit card has trailing **Start** next to Edit and leading **Enter Duration** below its status. The check-off Habit checkbox is also trailing in its live collection card; its inspector’s **Check In** is centered. This inventory follows actual controls instead of interpreting the wording literally.

| Flow | Actual baseline | Proposed hierarchy and behavior |
| --- | --- | --- |
| Habit check-off / checklist | Checkbox in trailing card header; centered Check In inspector footer | One leading checkbox + readable Check In / Undo Check-In toggle target; preserve checked state and toggle semantics. Checklist items remain distinct inputs. |
| Habit duration | Trailing Start / Stop / Review; leading Enter Duration below status | Leading filled Start Timer / Stop & Log / Review Timer action, outlined Enter Duration beside it; wrap in order from the same edge. Starting measures time; stopping records elapsed time. |
| Habit manual duration | Enter Duration opens Add Amount dialog; Amount to Add (s); trailing Add Amount submit | Enter Duration title, Duration (unit) field and leading Log Duration submit, with predictable Cancel. Record an additive entry without stopping an independently running timer. |
| Habit quantity / rating / log-only | Main +N / Rate / Log trailing; quantity controls leading below | Leading main activity action. Preserve quick increment / Add Amount / Set Total / Undo distinctions; wrap secondary controls without centering. |
| Goal progress | Trailing Log card action; centered Log Progress inspector footer; trailing Log Progress form submit | Leading filled Log Progress across card, inspector and entry form. Preserve observed-value versus additive-total semantics and saving feedback. |
| Goal completion count | Card +1 versus inspector/form Record Completion | Prefer explicit Record Completion where readable wrapping is available. Recording one event remains distinct from closing the Goal. |
| Goal lifecycle completion | Centered Complete Goal footer on executable Goal, including 0% | Leading outlined Complete Goal after activity action for all currently eligible executable Goals; preserve confirmation and frozen History outcome. Do not restrict availability to reached targets. |
| Elapsed Goal | Trailing Reset on card; Reset Timer within inspector | Leading explicit Reset Timer; preserve its reset/start-boundary semantics and confirmation. No ordinary progress logging action for elapsed Goals. |
| History correction / definition editors | Header Save on definition editors; Save Changes on history correction | Keep editing/navigation semantics distinct from recording an entry. Preserve draft guards, correction save behavior and errors; do not relabel edits as logging. |

## Consultant critique incorporated before implementation

1. Put a dedicated leading activity row **after always-visible status/progress and before expandable detail**, retaining status on expansion. Edit/disclosure remain trailing navigation.
2. Make ordinary activity visibly stronger than lifecycle completion. Preserve Complete Goal availability for every currently executable supported case, including before the target is reached, and retain confirmation.
3. Preserve the Habit completion checkbox and expose its label as one accessible toggle target at least 48 dp high; use existing filled/outlined components for textual actions.
4. Opt Habit/Goal inspectors into leading activity docks; leave unrelated inspector behavior unchanged.
5. Include new-entry submission rows and duration-specific language in the theme. Distinguish a new Log Duration/Log Progress from Save Changes during correction and header Save during definition editing.
6. Use existing components/tokens with scoped opt-ins. No speculative layout framework or unrelated whole-app redesign.

## Direct baseline evidence

All accepted screenshots are settled native app captures from the existing baseline debug APK on exclusively owned `emulator-5554` / `whip_api34`. Fixtures were created through ordinary UI; no uninstall, `pm clear`, app data reset, global adb reset, phone action or fixture test with destructive setup was used.

- `before-duration-card.png`: actual Start top-right / Enter Duration leading below status.
- `before-duration-entry.png`, `before-duration-entry-keyboard.png`: Add Amount title/input/submit from Enter Duration; keyboard remains visible with footer available.
- `before-duration-inspector.png`: leading Enter Duration Manually in Today body; centered Start Timer footer.
- `before-goal-card.png`: actual DesignGoal target 100, 0% progress, top-right Log and centered Complete Goal.
- `before-goal-log.png`, `before-goal-log-keyboard.png`: Log Progress form and keyboard, trailing submit.
- `before-goal-inspector.png`: centered Log Progress activity footer.
- `before-habit-checkin.png`: real Duration and CheckOff collection cards; trailing Start and checkbox.
- `before-checkin-inspector.png`: centered Check In activity footer.

Related XML hierarchies share the capture basenames where available. Diagnostic navigation capture is explicitly named `diagnostic-settings-navigation`, excluded from baseline claims. An early Goal draft/discard screenshot was replaced by the settled card before acceptance; `goal-draft.xml` records the retained draft. It is not before/after evidence.

Fixture names for paired final capture are `Design%20Focus` (Android input typed `%20` literally), `DesignCheckIn`, and `DesignGoal` (target 100, starting value 0). Keep the same records/values for direct comparison. No manual logging or goal closure was saved during baseline creation. Duration draft cancellation produced the existing Keep Editing / Discard Changes confirmation; the draft was explicitly discarded. Goal drafting also exercised Keep Editing after keyboard-driven scrolling, then saved the retained definition.

## Final acceptance requirements

Consultant must inspect actual paired final screenshots and real interactions, not accept geometry-only tests. Required evidence: leading activity buttons and text in compact and wide layouts; 200% text with long labels; duration manual-entry keyboard and Cancel/Back draft retention; timer Start → running → Stop & Log; repeated logging and stable parent/status; goal Log Progress → updated value, Complete Goal confirmation → cancel and final completion with frozen History outcome; inspector and history correction Save Changes remain coherent. QA owns focused persistence/accessibility checks. At least 48 dp targets, readable wraps, no duplicate submit, no action propagation into inspector navigation, and saving/error feedback must be preserved.

## Limits

This worker’s baseline is a direct visual/interaction inspection, not a passing automated suite. Wide/large-text, persistence, history correction, and final timer/closure journeys belong to final validation. Native TalkBack speech and physical folding hardware cannot be certified by these captures.
