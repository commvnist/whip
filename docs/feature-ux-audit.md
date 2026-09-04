# Whip feature and UX audit

Audited: 2026-08-18

This audit reviews the current implementation, automated coverage, and the
expectations created by established task, habit, goal, and workout products. It
separates verified behavior from future product opportunities. “Complete” here
means the current behavior is implemented and tested; it does not imply that a
product can never be improved or that automated tests can prove the absence of
every defect.

## Product stance

Whip's strongest position is a private, local-first personal system with four
specialist areas and explicit links between them. It should serve two modes
without forcing either one on everyone:

- **Simple mode:** capture something quickly, see what matters now, log it with
  one action, recover from mistakes, and ignore every advanced setting.
- **Power mode:** configure schedules, units, history, analytics, links,
  automations, backups, and large-screen layouts without losing auditability.

Cloud accounts, social feeds, collaboration, and prescriptive training or
medical advice are not baseline requirements. They would change the product's
privacy and complexity model rather than merely fill a feature gap.

## Market signals used

- Todoist hides future occurrences by default, supports recurrence anchored to
  either the schedule or completion date, resets recurring subtasks, and offers
  an undo after completion. This validates Whip's compact Upcoming default and
  highlights occurrence recovery and broader recurrence as important task
  workflows. [Todoist recurring dates](https://www.todoist.com/help/articles/introduction-to-recurring-dates-YUYVJJAV)
  and [recurring completion](https://www.todoist.com/help/articles/complete-a-task-with-a-recurring-date-dmI6SVqdP)
- TickTick combines fast capture with lists, tags, filters, calendars, multiple
  layouts, reminders, habits, statistics, and widgets. This makes task
  organization and alternate planning views an important depth benchmark Whip
  now meets with named filters, bulk actions, agenda, and calendar views;
  collaboration and mandatory cross-platform sync are outside its present
  local-first stance. [TickTick features](https://ticktick.com/features?language=en_US)
- Habitify keeps a habit's repeat schedule separate from its numeric goal and
  supports areas, checklists, end conditions, multiple reminders, weekday
  reminder times, and habit stacking. Whip already covers most of this model,
  including monthly/yearly date schedules behind progressive disclosure.
  [Habitify habit creation](https://intercom.help/habitify-app/en/articles/11203157-add-a-new-habit-on-website-desktop-app)
  and [reminders](https://intercom.help/habitify-app/en/articles/12396874-good-habit-setting-reminders).
  Its numeric-goal guidance and progress-recording flow also validate generic
  units, partial progress, and user-configured quick values rather than
  hydration-specific assumptions. [Habitify goal settings](https://intercom.help/habitify-app/en/articles/12393175-good-habit-setting-goal)
  and [recording progress](https://intercom.help/habitify-app/en/articles/9387661-record-progress-on-a-good-habit)
- FitNotes emphasizes low-friction set entry, exercise history, metric-specific
  graphs, estimated and actual rep maxes, statistics, records, local backup, and
  CSV export. Its set fields auto-populate prior values and offer −/+ controls
  driven by exercise-specific increments; its settings distinguish global
  units from exercise-specific units and increments. Whip now follows those
  high-frequency entry principles while retaining machine identity. [FitNotes workout tracking](https://www.fitnotesapp.com/workout_tracking/),
  [progress tracking](https://www.fitnotesapp.com/progress_tracking/), and
  [settings/data](https://www.fitnotesapp.com/settings/)

## Use-case audit

| User job | Current support | Audit result |
| --- | --- | --- |
| Capture an unscheduled or dated task | Global add, Inbox and Scheduled date modes | Strong; the editor exposes more fields than a first-time user needs |
| Repeat a task daily, every N days/weeks/months/years, on chosen weekdays, or relative to completion | Per-occurrence completion, move, skip, end date/count, schedule/completion anchor, next-only Upcoming | Strong with compact defaults and power-user cadence controls |
| Break work into steps and see progress | Equal-weight subtasks, notes, reorder, promote, percentage/fraction, reset/carry policy | Strong; one-level nesting is an intentional simplicity boundary |
| Recover from a task mistake | Restore archive, reopen one-shot task, unified recurring history, undo skip/reset move, explicit permanent-delete confirmation | Strong; each occurrence can be recovered without shifting the series |
| Track “6 of 8 glasses” or another custom quantity | Count/decimal modes, comparisons, periods, custom units, quick increments | Strong |
| Build, limit, avoid, time, rate, or observe a habit | Eight tracking modes, flexible schedules, reminders, checklist, pause, backdate/edit | Strong; basic mode keeps advanced configuration out of the initial path |
| Turn workouts into a “Gym 4× weekly” habit | Completed-workout trigger creates one auditable check-in and rebuilds on resume/edit | Strong and regression-tested |
| Track weight, savings, distance, averages, ranges, consistency, or projects | Generalized measurement goals, milestones, pace, forecast, history, links, editable templates | Strong for both basic setup and custom power-user models |
| Log an entirely user-defined workout | Empty library, custom exercises/types, live sets, previous values, routines, timer, history | Strong |
| Analyze strength or endurance progress | e1RM, actual rep max, volume, records, ranges, aggregation, comparisons, saved graph presets | Strong calculations with labeled ranges, point inspection, data tables, and screen-reader summaries |
| Move or protect data | Full JSON backup/restore, verified portable folder, retention, and domain CSV exports | Strong; release and debug data are now isolated during device testing |
| Work on a Fold/large screen | Persistent navigation, useful secondary pane, hinge layouts, full-screen primary pane | Strong and device-tested on the target form factor |

## What changed during this audit

- Added permanent task deletion as a separate action from archive/Stop series.
- Added a second, explicit destructive confirmation describing series history,
  subtasks, links, automations, and backup implications.
- Made permanent deletion transactional across task rows, occurrence history,
  subtask state/snapshots, derived goal measurements, contribution rules,
  automation rules, generated habit logs, and task reminders.
- Added **Reopen occurrence** for completed recurring tasks while preserving the
  occurrence's moved date and live subtask state.
- Fixed task-triggered automatic habit check-ins so projection rebuilds use the
  event's real source type and cannot create duplicate logs.
- Made task action content scrollable for long subtask lists and clarified that
  stopping a series archives it.
- Gave debug builds the isolated `commvne.com.whip.app.debug` identity and `Whip Dev`
  label so device tests cannot overwrite signed release data.
- Added a unified task-series history for completed, skipped, and moved
  occurrences, with reopen, undo-skip, reset-date, and cadence explanations.
- Added basic/advanced task, habit, and goal editors and six editable goal
  templates for common outcomes.
- Made every search domain open or visibly focus the exact selected active or
  archived record, including discarded workouts.
- Added dependency-aware permanent deletion for habits, goals, exercises,
  routines, and workouts, with impact previews and transactional derived-data
  cleanup while preserving archive as the default.
- Added chart min/max/change summaries, labeled visual bounds, point details,
  accessible data tables, and screen-reader descriptions.
- Added notification permission/channel/battery diagnostics, Android settings
  shortcuts, and a real test-notification action.
- Replaced converted kg-to-lb equipment decimals with real pound defaults when
  an exercise unit changes: 45 lb bar, 5 lb increment, and standard pound
  plates. Existing exercise settings and history remain exercise-owned.
- Added compact numeric ranges plus explicit increments for both mass and
  ordinal machine stacks, exact-value stepping during workouts, and compact
  previews that do not overflow on large stacks.
- Made new machines inherit their exercise unit; made routines, current sets,
  previous sets, sharing, single-exercise charts, and records honor that unit.
  Cross-exercise comparisons and aggregate summaries retain one global axis.
- Extended the same range/value grammar to habit quick buttons, removed noisy
  trailing `.0` values from editable whole numbers, and made long quick-button
  collections horizontally scrollable.

## Prioritized findings

### P0 — release blockers

No known P0 blocker remains after this pass. Permanent deletion, linked-data
cleanup, reminder cancellation, recurrence recovery, and their UI boundaries
are covered by new persisted and Compose tests.

### P1 — completed high-value work

1. **Occurrence history and recovery — implemented:** show completed, skipped, and moved
   occurrences in one task-series history. Allow undo skip and explain how the
   next date was calculated.
2. **Progressive editors — implemented:** give Tasks, Habits, and Goals a short basic lane and
   collapse advanced fields until requested. Task should initially need only a
   title and optional date; templates should help with common goals as they
   already do with habits.
3. **Search deep links — implemented:** every result opens or visibly focuses
   the exact record, including archived records.
4. **Dependency-aware deletion parity — implemented:** habits, goals, exercises, routines,
   and workouts mostly use archive by design. Add an advanced permanent-delete
   flow that previews history, measurement, and link impact instead of exposing
   raw destructive buttons.
5. **Accessible analytics — implemented:** add labeled axes/ranges, a selected-point detail,
   textual min/max/change summaries, and accessibility descriptions to gym
   charts and comparisons.
6. **Notification diagnostics — implemented:** surface whether Android notification
   permission and battery restrictions may prevent expected delivery; expose a
   direct test-notification action.

### P2 — completed power-user expansion

- **Implemented:** task priorities, reusable areas/tags, named filters, bulk
  complete/archive/restore/pin actions, and list/agenda/month-calendar views.
- **Implemented:** monthly/yearly and completion-relative recurrence, multiple
  reminder offsets, deadlines distinct from work dates, and opt-in local
  natural-language capture.
- **Implemented:** saved Home task filters, saved Review section filters, and
  drill-down from Home, Fold, and Review summary counts.
- **Implemented:** explicit next-set focus/jump, balanced summary-first collection rows,
  reusable plate presets, and pinned routine-day shortcuts.
- **Verified existing implementation:** editable templates for weight, savings,
  distance, reading, consistency, and weighted projects.
- **Retired before release:** task arrival/leave cues and their location
  permissions were removed to reduce setup friction, battery-policy risk, and
  control-scheme complexity.

### Machine-aware gym tracking — completed

- **Gym-goer need:** the number printed on a selectorized machine is a property
  of that machine, not a universally comparable weight. Pin 7 at home is not
  70 lb, and 70 lb on two commercial machines may produce different resistance
  because of pulley ratios, cams, cable routing, and lever arms.
- **Implemented:** exercise-specific machine profiles with names, locations,
  setup/model notes, compact ranges (`1-10`, `50-500`), explicit increments,
  custom irregular values, archive support, and explicit mass-stack versus
  numbered-level modes.
- **Implemented:** machine choice during workout entry, machine presets in the
  set editor, routine persistence, locked historical semantics, and exact
  machine matching for previous-set suggestions.
- **Implemented:** progress and personal records are partitioned by physical
  machine. Ordinal levels have a highest-setting metric but cannot feed mass,
  tonnage, or estimated-1RM calculations. This favors honest longitudinal data
  over a false conversion estimate.
- **Power-user boundary:** changing the physical stack, unit, scale kind, or
  exercise after history exists requires a new profile. Names, setup notes, and
  available-entry presets remain editable, while old workout snapshots remain
  auditable.

## UX rules for future changes

### Shared control grammar

Tasks, habits, goals, exercises, machines, categories, routines, and routine
placements follow the same control meanings:

- Tapping an item card opens its details or the closest non-destructive detail
  surface. A card must not silently mean Edit in one library and Details in
  another.
- A trailing pencil always opens the editor for that exact item. It remains a
  separate 48 dp target and never relies on a decorative ellipsis.
- The status-specific quick action (check off, log, start/stop, complete) is a
  separate control. It must not also open details or edit the item.
- A vertical-ellipsis control always opens a real overflow menu. Overflow is
  reserved for lower-frequency actions such as duplicate, reorder, archive,
  restore, and permanent delete.
- Detail surfaces keep Edit in a persistent footer opposite Close. Changing
  Overview/Today, History/Schedule, Connections, or Options must never make Edit
  disappear.
- Permanent deletion lives in Options/overflow, uses error styling, and requires a
  consequence-aware confirmation. Archive and restore remain reversible.
- Peer destination changes use one horizontally scrollable, underlined tab row;
  no destination is hidden behind a More button. Filters use chips, temporary
  list modes use a visibly selected control that becomes Done, and controls that
  reveal content in place keep a stable label with an up/down disclosure icon.
- A right chevron always opens a child page. A down arrow always opens a choice
  menu. Settings categories are tabs, while Gym's Library is a landing page whose
  chevron rows open Routines, Exercises, Machines, Categories, and Tools.
- Settings promotes those peer categories to a persistent navigation list when
  the content pane is at least 840 dp wide. On wide layouts, create moves into
  the top app bar so it cannot cover list cards; compact layouts retain the
  familiar FAB with enough trailing scroll clearance.
- Selection or reorder modes may temporarily replace ordinary item controls,
  but the mode and its exit control must be explicit.

Accessibility descriptions use the same verbs: `Open <type> details for
<name>`, `Edit <type> <name>`, and `More options for <name>`. Automated UI
tests should invoke and distinguish these controls independently.

1. A first successful task, habit, goal, or workout must be possible without
   opening an advanced section.
2. Archive is reversible and uses neutral language. Delete is permanent, uses
   destructive styling, explains dependency impact, and requires confirmation.
3. Every automatic value must identify its source; every source edit/delete
   must deterministically rebuild the result.
4. Empty states teach one next action without silently seeding user data.
5. A global search result must land on the exact thing selected.
6. Every chart must have an equivalent textual interpretation.
7. Touch targets remain at least 48 dp and layouts must remain usable at large
   text sizes, narrow phone widths, landscape, and the Fold's inner display.
8. New settings need a nearby explanation of what changes and whether they
   affect existing or only future data.
9. Unit changes must choose values from the user's real equipment system, not
   expose conversion artifacts as defaults. Canonical conversion is for
   storage and comparison, not for inventing plate or stack markings.
10. Repeated numeric entry must support direct typing and predictable −/+
    controls. Large exact-value sets need compact summaries and must not create
    overflowing rows of controls.

## Verification expectations

Any P1/P2 implementation must add a deterministic rule test where applicable,
a persisted repository test for data mutation, and a Compose/device path for
user-visible behavior. Destructive flows additionally require cascade/orphan
checks and a test proving cancellation leaves data unchanged.
