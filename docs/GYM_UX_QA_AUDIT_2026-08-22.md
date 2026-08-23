# Gym UX and QA Audit — 2026-08-22

## Verdict

Gym has a strong underlying training model: workout history is snapshotted, machine identity is explicit, calculations are deterministic, routines preserve their templates, destructive actions generally explain retained data, and the existing domain/repository tests are substantially better than the current UI polish suggests.

It is not yet ready to call production-grade. The open-Fold first-exercise editor is unusable, modal behavior changes between screens, several controls expose actions that are invalid in the current state, and parts of History, Progress, and Workout Tools can imply semantics different from what the code actually calculates. Those are release blockers or beta blockers, not cosmetic preferences.

Priority definitions:

- **P0 — release blocker:** prevents a core journey on a supported layout or risks unrecoverable behavior.
- **P1 — beta blocker:** misleading, state-unsafe, inaccessible, or likely to cause repeated task failure.
- **P2 — important:** material efficiency, comprehension, or scalability problem.
- **P3 — polish:** low-risk consistency issue.

## Implementation status — completed 2026-08-22

All fourteen product findings in this audit have been addressed in the shared UI/state rules rather than with screenshot-specific offsets:

- every Gym prompt now uses the pane-aware short-dialog contract, while every long editor uses the adaptive full-window editor host;
- first use leads with Create First Exercise, an empty active workout explains both add/create paths, and rest notifications are disclosed only at explicit timer start;
- History separates read-only details, metadata editing, and confirmed Finished → Active restoration, and gates every conflict while another workout is active;
- quick set fields are atomic, steppers announce their target/increment, all missing requirements are listed without duplicate domain wording, completed sets collapse until requested, and the next incomplete set is focused on entry;
- relative graph ranges end today, custom ranges validate strictly, comparison series share one time/value scale, and every chart point is touch- and accessibility-operable;
- Workout Tools are separated, known-1RM mode removes Reps, unit changes require Convert/Keep/Reset, and invalid plate/inventory input suppresses calculation instead of falling back;
- exercise discovery shares one metadata-query rule and adds favorites/category/tracking/equipment/machine filters plus name/recently-used/recently-added/favorites sorting;
- routine one-day controls are hidden when meaningless, active-workout conflicts route to the active workout, machine definition locks offer versioning in context, and advanced machine setup is staged;
- Fold context is destination-aware and actionable, exercise cards jump into the matching active-workout card, empty context has useful guidance, and singular/plural text uses one tested formatter;
- SQLite triggers enforce the single-active-workout invariant behind the repository’s friendly checks.

Fresh post-fix open-Fold evidence is in [`artifacts/ux-audit/2026-08-22/gym-live/fixed`](../artifacts/ux-audit/2026-08-22/gym-live/fixed). The full non-destructive gate passes 183 JVM tests, Android-test compilation for 199 instrumentation tests, lint, asset verification, and debug assembly. Instrumentation was not executed on the owner’s personal phone because that suite clears debug-app state; physical closed-Fold, TalkBack/Switch Access, and 200% text remain release-candidate device checks rather than unresolved product defects.

## Scope and evidence

The audit used the live `commvne.com.whip.app.debug` build on a connected Samsung Fold in its open, separating posture (2256 × 2504 at 480 dpi). Audit fixtures were inserted only into the debug database. The installed release package and its data were not changed.

Journeys exercised:

- empty Gym and first-run discovery;
- exercise, machine, category, and routine libraries;
- active workout, quick set entry, full set editor, rest timer affordance, finishing/discarding, and adding exercises;
- workout history, invalid resume while another workout is active, copying actions, and archived-data wording;
- progress graphs, graph options, records, and data-table affordance;
- routine creation, day organization, exercise prescriptions, previous-value behavior, and machine-aware warm-ups;
- 1RM, percentage, plate-loading, and preset tools;
- open-Fold pane, dialog, hinge, status-bar, and taskbar behavior;
- source, data invariants, accessibility semantics, and current automated coverage.

Fresh cropped evidence is in [`artifacts/ux-audit/2026-08-22/gym-live/cropped`](../artifacts/ux-audit/2026-08-22/gym-live/cropped). The [contact sheet](../artifacts/ux-audit/2026-08-22/gym-live/gym-contact-sheet.png) gives a compact overview. Raw captures are retained separately only because they help diagnose Fold window/status-bar geometry; user-facing evidence should use the cropped copies.

Physical compact/closed-Fold interaction was not performed because changing the device posture without the owner risks locking or interrupting the personal phone. Compact behavior was inspected in source and tests, but a final closed-Fold smoke pass remains part of the release acceptance criteria below.

## What is already strong

- Exercise tracking supports weight, repetitions, duration, distance, bodyweight, assistance, RPE, RIR, tempo, unilateral work, and explicit load interpretation without pretending ordinal machine settings are mass.
- Machine configuration snapshots preserve historical meaning. Configuration versioning is the right model for hardware whose resistance definition changes.
- Workout completion retains incomplete planned sets and explains this before finishing.
- Destructive machine deletion distinguishes data removed, data retained, routine impact, and active-workout blockers.
- Routine drafts support searching a large library, inline exercise/machine creation, reusable prescription schemes, previous-workout values, and equipment-aware warm-ups.
- Workout reordering includes accessibility custom actions rather than relying only on drag gestures.
- Progress charts expose a useful full semantic description even though the visual and interactive chart is weak.
- Notification permission is requested only when the user explicitly starts a rest timer, not when Gym or the app opens.
- Current JVM tests and lint pass. The existing calculation and repository coverage is meaningful rather than superficial.

## Findings and required resolution

### GYM-01 — P0: the first exercise cannot be created in open-Fold mode

Evidence: [`05-exercise-editor-basic.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/05-exercise-editor-basic.png).

`ExerciseEditorDialog` applies the right-pane offset/width modifier to Material `AlertDialog`. The platform dialog window is sized around the unshifted card, so moving the card into the content pane clips its right side and its bottom actions. Helper text is truncated and Save/Cancel are not reliably reachable. This blocks the first-run path because Whip intentionally seeds no exercises.

Required resolution:

- Host the editor in `ProductivityEditorDialog`, as the machine editor already does.
- Pass the active pane modifier to the full-window dialog host, not a platform-width `AlertDialog` card.
- Guarantee title, required fields, validation, Save, and Cancel remain visible/reachable at every supported width, posture, IME state, and font scale.
- Add an open-Fold Compose/instrumentation regression test that creates and saves the first exercise.

### GYM-02 — P1: Gym has three incompatible modal contracts

Evidence: exercise editor clipping above; [`14-machine-editor-fold.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/14-machine-editor-fold.png), [`20-add-exercise-picker-fold.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/20-add-exercise-picker-fold.png), and [`08-set-editor-fold.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/08-set-editor-fold.png).

Exercise editing is offset inside a platform dialog and clips. Machine editing uses a full-window host but is not passed the pane modifier, so it expands across the Fold. Set and exercise pickers use centered platform dialogs across both panes. Routine editing becomes a pane-local page. The user cannot predict whether an action will stay in context, cover both panes, cross the hinge, or clip.

Required resolution:

- Define one adaptive contract:
  - short confirmations/choices use `PaneAwareAlertDialog` wholly inside the active pane;
  - long creation/editing flows use a pane-local full-height editor page or `ProductivityEditorDialog` wholly inside the active pane;
  - a true whole-window modal is reserved for an intentional global interruption and must not place controls on the hinge.
- Inventory and migrate every direct `AlertDialog` in `GymScreens.kt`; do not repair only the observed exercise editor.
- Test every Gym modal with compact, expanded, separating Fold, content-pane-expanded, IME-visible, RTL, and 200% text configurations.

### GYM-03 — P1: empty Gym does not teach the prerequisite workflow

Evidence: [`01-gym-empty-workout.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/01-gym-empty-workout.png) and [`12-exercise-library.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/12-exercise-library.png).

The empty workout says “Start a blank workout, then add only the exercises you created” and offers only **Start Workout**. A clean install has no exercises. Creation exists under the global `+`, Library, and the later Add Exercise picker, but the empty state does not explain this sequence. “Only” reads like a limitation rather than a reusable-library benefit.

Required resolution:

- Make the empty state a two-path onboarding decision: **Create First Exercise** and **Start Empty Workout**.
- After starting an empty workout, show a focused next step: **Add Exercise**, with **Create New Exercise** available in the same flow.
- Explain the model positively: exercises are created once and reused in workouts/routines.
- If starter movements are ever offered, make them an explicit opt-in template import rather than hidden seed data.

### GYM-04 — P1: quick-entry controls break apart and are not self-describing

Evidence: [`07-workout-quick-entry.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/07-workout-quick-entry.png).

The weight field and its −/+ buttons are separate children in one `FlowRow`. On the current right-pane width the + button wraps to the next line before Reps. The visual and keyboard order becomes weight, minus, plus, repetitions even though the controls are one atomic input. The buttons also lack contextual accessibility labels such as “Increase bench weight by 2.5 kg.” RPE and RIR appear together by default, further increasing density for a novice.

Required resolution:

- Put each value field and its steppers in an atomic responsive group that never splits internally.
- Use the same `SteppedNumberField` contract in quick and full set editors.
- Give steppers explicit semantic labels including exercise, direction, increment, and unit.
- At narrow width/large text, make each required field full-width and place the stepper inside or directly below it.
- Show all currently missing requirements rather than only the first validation failure; keep the primary action adjacent to that summary.

### GYM-05 — P1: History conflates viewing, editing, and resuming

Evidence: [`09-history-populated.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/09-history-populated.png).

Every completed workout presents **Edit / Resume** as its dominant action. Resuming changes the session back to Active and clears its end time; that is materially different from viewing or editing a note. When another workout is active, the same enabled action remains visible and fails only after a tap with “Another workout is active.” **Copy to Today** has the same active-session conflict. The card also renders “1 exercises,” and one full-width copy-sets action per exercise makes history increasingly dense.

Required resolution:

- Make the card/open action a read-only workout detail view.
- Separate **Edit Details** from **Resume as Active Workout**. Confirm the latter and describe its effect on History.
- While a workout is active, replace invalid actions with **Open Active Workout** or disable them with an inline reason; never offer a known-invalid primary action.
- Move per-exercise copying into workout detail, expansion, or overflow.
- Centralize singular/plural formatting (`1 exercise`, `2 exercises`) and test it.

### GYM-06 — P1: Progress range and comparison semantics are misleading

Evidence: [`10-progress-populated.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/10-progress-populated.png).

The “1/3/6 Months” and “1 Year” ranges end at the most recent recorded workout, not today. A user returning after a long break can select “3 Months” and receive old data outside the last three calendar months. Blank or invalid custom dates silently fall back to an unbounded/startless range ending at the latest workout. “Compare up to 3 Exercises” renders independent charts with independently scaled axes, so their visual slopes cannot be compared safely.

The chart itself is a large line with only High, Low, and Earlier → Later. It has no date axis, unit axis, grid, focusable/tappable points, or on-chart details; users must discover a separate data table to inspect a point.

Required resolution:

- Anchor relative ranges to the app’s current local date. If product intent is “ending at latest workout,” state that explicitly in the label.
- Validate both custom dates, require From ≤ To, and do not calculate until the range is valid.
- Render comparison series on one shared, labeled scale or present a normalized comparison explicitly; never imply comparability across independent scales.
- Use actual time spacing, labeled axes and units, a modest grid, focusable/tappable points, and a value/date tooltip.
- Preserve the existing rich accessibility summary and add per-point semantics.

### GYM-07 — P1: Workout Tools can silently change or ignore user input

Evidence: [`18-tools.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/18-tools.png).

Changing Plate Unit immediately resets the bar and available-plate fields but leaves other numbers under a new unit label. This mixes reset and relabel semantics without consent. Invalid inventory syntax sets an error on the field but the calculator still uses an empty inventory map, effectively calculating as unlimited. Invalid plate tokens are silently discarded. When “Weight Is a Known 1RM” is enabled, Reps stays visible and editable even though it is ignored.

Required resolution:

- On unit change, offer **Convert Values**, **Keep Numbers**, or **Reset Defaults**, matching the app’s other unit-semantic safeguards.
- Suppress loading results while any required plate/inventory input is invalid; never calculate with a silent fallback that changes the meaning.
- Identify the exact invalid plate token and retain the user’s input for correction.
- Hide/disable Reps with an explanation when the entered weight is a known 1RM.
- Separate the 1RM and Plate calculators into compact sections/pages so only the active tool dominates the screen.

### GYM-08 — P2: starting the rest timer looks like a static value

In the Ready state, the only action label is “2:00.” Tapping it starts the timer and may legitimately open Android’s notification permission prompt. A bare duration does not communicate that consequence, so the prompt can feel unsolicited even though the code requests it only after the tap.

Required resolution:

- Label the action **Start 2:00 Rest** (or use a play icon plus accessible text).
- Before the first permission request, briefly explain that notifications allow the timer to alert when Whip is not visible.
- A denial must not block in-app timing; show a non-modal path to Settings only when background alerts are unavailable.
- Test that opening Gym never requests notification permission and that only an explicit timer start can request it.

### GYM-09 — P2: the active workout spends too much space repeating status

Evidence: [`06-workout-populated.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/06-workout-populated.png).

The header presents summary metrics, then “Jump to next set · …,” followed immediately by a sticky “NEXT · …” header and Rest Timer. This delays the exercise/set that the user opened Gym to log. Completed-set drag handles, checkboxes, summaries, and menus are useful but visually heavy when repeated.

Required resolution:

- Keep one next-set affordance, not two. Make it the sticky focus/navigation element.
- Collapse workout statistics and rest timing into a compact training status bar.
- Keep completed sets visually quiet while preserving edit/reorder access.
- Measure success by showing the next incomplete set and its required inputs without scrolling on typical compact and pane widths.

### GYM-10 — P2: library browsing will not scale with the supported data model

Exercise Library has only **Show Archived** plus the global Gym search. It has no inline search, sort, favorites-only, category, equipment, or tracking-type filter. The routine picker is already tested with 200 exercises, so the product explicitly anticipates libraries large enough to need these controls.

Required resolution:

- Add inline search and a compact filter/sort affordance whose active filters appear as removable chips.
- Support favorites, category, equipment/machine, tracking type, archived, and alphabetical/recently used sorting.
- Reuse the same exercise-picker/search component across Library, active workout, routine builder, and substitution so query behavior does not drift.

### GYM-11 — P2: advanced routine and machine controls need clearer availability

Evidence: [`16-routine-builder.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/16-routine-builder.png), [`17-routine-exercise-editor.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/17-routine-exercise-editor.png), and [`14-machine-editor-fold.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/14-machine-editor-fold.png).

With one routine day, move-left, move-right, and delete controls remain visible but disabled without explaining the one-day invariant. The machine editor says the resistance definition is unavailable after history exists, but the corrective **Create New Configuration Version** action lives elsewhere in an overflow menu. The machine form also exposes identity, repeatable setup, resistance semantics, mapping, comparison compatibility, and presets in one long pass.

Required resolution:

- Hide day-reorder controls until multiple days exist; replace last-day deletion with a clear “a routine needs one day” explanation if invoked.
- Put **Create New Configuration Version** directly in the locked-definition notice.
- Stage machine creation into **Identity**, **Resistance**, and optional **Advanced Setup** sections while keeping saved semantics visible for expert review.
- Keep the strong consequence text directly beneath the control that enables dependent settings.

### GYM-12 — P2: the Fold support pane is informative but passive

The left “Workout Context” pane repeats the active workout and up to five exercise names on every Gym destination. The cards are not actions, so they consume half the display without enabling navigation. On History, Progress, and Library, the same context may be less useful than destination-specific summaries.

Required resolution:

- Make the workout card open the active workout and exercise cards jump to the matching workout card/set.
- Use destination-aware support content: recent workouts for History, selected metric/record for Progress, and favorites/recent items for Library.
- Offer **Expand Content** consistently and remember the preference without moving dialogs across the hinge.

### GYM-13 — P2: finish the accessibility contract

Positive foundations include 48 dp menu buttons, labeled edit actions, chart summaries, and custom reorder actions. Remaining gaps include context-free −/+ steppers, icon-only routine controls whose disabled state is unexplained, charts that are semantic as a whole but not operable by point, and modal focus order that changes with the host.

Required resolution:

- Test the entire core journey with TalkBack and Switch Access, not just semantics inspection.
- Every icon/stepper action must describe target and consequence.
- Modal opening must focus the title/first invalid field; closing must restore focus to the invoker.
- Validate touch targets, traversal order, 200% text, RTL, high contrast, and no color-only state.

### GYM-14 — P3: minor density and duplication remain in Library

Evidence: [`19-categories.png`](../artifacts/ux-audit/2026-08-22/gym-live/cropped/19-categories.png).

The empty Categories screen shows **Create Category** in both the page header and empty state. Duplication is harmless but adds to the app’s broader tendency to give every action equal visual weight. Keep the contextual empty-state action and reduce/remove the header action until content exists.

## QA assessment

### Baseline executed in this pass

- `./gradlew testDebugUnitTest lintDebug` — passed.
- 177 JVM tests executed, 0 failures, 0 errors.
- Live manual smoke used a deterministic debug-only database with 3 exercises, 1 machine, 4 sessions, 10 sets, and 1 routine; SQLite integrity and foreign-key checks passed before installation.

### Existing relevant coverage

The repository contains 196 Android instrumentation test methods; 31 are directly in Gym/Routine UI and repository suites. The focused JVM Gym/Routine suites contain 38 test methods. Strong current coverage includes:

- calculation formulas, volume, bodyweight/assistance, load interpretation, validation bounds, and practical unit conversion;
- graph aggregation/downsampling, machine scoping, historical snapshots, and compatible machine versions;
- quick-set next/reuse/append decisions;
- persistence, undo/delete, timer deadlines, substitution, archives, machine identity, and historical set editing;
- routine immutability, machine binding, duplicate placements, reusable alternatives, record rebuilding, graph presets, and searchable 200-exercise selection;
- inline exercise/machine creation without losing a routine draft and retry after failed routine save.

Instrumentation was reviewed but not executed on the owner’s personal phone because those tests can reset app state. It should run on an emulator or dedicated QA package/device in CI.

### Missing regression coverage

| Risk | Required automated test |
|---|---|
| First exercise editor clips | Separating vertical Fold: open Library → Create Exercise, enter required data, expose IME, scroll, save, and assert every action remains inside the content pane. |
| Mixed modal geometry | Parameterized screenshot/geometry tests for every Gym dialog in compact, expanded, BookFold, content-expanded, RTL, and 1.0/1.5/2.0 font scale. |
| Broken quick-entry wrapping | At each supported width, assert load field and steppers share one semantic/layout group and precede Reps; run with kg, lb, machine levels, and long localized labels. |
| Invalid active-session actions | With an active workout, assert History resume/copy and routine start are replaced, disabled with reason, or routed to the active workout; repository failures must not be the first explanation. |
| Historical state changes | Confirm Resume explicitly changes Finished → Active and requires confirmation; Edit Details must not change state/end time. |
| Relative/custom ranges | Clock-fixed tests proving relative ranges end today; invalid/blank/reversed custom dates show errors and produce no chart. |
| Misleading comparisons | Assert all compared series share one scale/unit and each point is operable/announced with exercise, date, value, and unit. |
| Calculator silent fallbacks | Invalid plate/inventory tokens suppress output; unit changes test convert/keep/reset separately; known-1RM mode ignores no visible enabled field. |
| Notification prompt timing | Fresh install: opening app/Gym/workout triggers no permission; explicit **Start Rest** may request; denial preserves in-app timer. |
| Process/lifecycle recovery | Kill/recreate during active workout, quick entry, running rest timer, routine draft, and each editor; verify no duplicate set/session and no lost committed input. |
| Single-active invariant | Concurrent start/resume/copy/routine-start attempts plus malformed restore data; database/repository must surface exactly one active session deterministically. |
| Scale | 1,000 exercises, 10,000 workouts/sets, long names, duplicate names, archived machines, and mixed units without blocked main-thread frames. |
| Accessibility | TalkBack/Switch Access journey from first exercise through completed workout; focus restoration, 48 dp targets, traversal order, RTL, and 200% text. |
| Physical devices | Closed Fold, open Fold, slab phone, tablet, portrait/landscape, keyboard/IME, and hinge/status-bar screenshots with the release candidate. |

## Implementation sequence and definition of done

### Phase 0 — unblock and standardize adaptive editors

Resolve GYM-01 and GYM-02 together. Do not ship a one-off offset fix. Completion requires migration of every Gym modal to the documented adaptive contract, geometry tests, and fresh compact/open-Fold screenshots.

### Phase 1 — make state transitions safe and first use obvious

Resolve GYM-03, GYM-05, and GYM-08. Completion requires a clean-install first-workout test, explicit History detail/resume separation, active-session gating, and notification-permission timing tests.

### Phase 2 — optimize the in-workout loop

Resolve GYM-04 and GYM-09. Completion requires atomic responsive inputs, contextual accessibility labels, a single next-set status, and a no-scroll next-set acceptance check on compact and Fold panes.

### Phase 3 — make analytics and calculators truthful

Resolve GYM-06 and GYM-07. Completion requires clock-based ranges, strict custom validation, shared-scale comparisons, interactive chart points, safe unit changes, and no result under invalid inputs.

### Phase 4 — scale and polish power-user organization

Resolve GYM-10 through GYM-14. Completion requires one reusable exercise-query component, actionable Fold context, staged machine configuration, accessibility device testing, and removal of duplicate low-value chrome.

A phase is not complete when only the visible occurrence is fixed. It is complete when the shared component or state rule is corrected, unit/UI tests cover the rule, accessibility behavior is verified, and fresh compact plus open-Fold evidence is checked into the audit artifacts.

## Release gate

Before calling Gym up to standard:

- all P0/P1 findings are closed;
- all Gym dialogs satisfy the same adaptive contract;
- clean install → first exercise → first workout → first completed set → History → Progress works without hidden prerequisites;
- an active workout makes every conflicting action unavailable before the user taps it;
- charts/calculators never display results for an invalid or semantically ambiguous input state;
- JVM, lint, and the dedicated emulator instrumentation suite pass;
- TalkBack and 200% text complete the core journey;
- fresh release-candidate screenshots pass on both open and closed Fold postures without clipped content, hinge-crossing controls, or unintended status/navigation-bar treatment.
