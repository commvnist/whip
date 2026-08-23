# whip product and implementation plan

Last updated: 2026-08-20

This file is the source of truth for planned work on `whip`. Update it as work
is started, completed, tested, deferred, or changed. Do not mark a phase
complete until its acceptance criteria and required tests pass.

## Status legend

- `[x]` complete and verified
- `[ ]` not started
- `[~]` in progress
- `[!]` blocked; add the blocker beside the item
- `[-]` deliberately deferred or removed; add the reason beside the item

## Product direction

Whip has four first-class domains:

- **Tasks** represent finite work.
- **Habits** represent repeated behavior.
- **Goals** represent longer-term outcomes.
- **Gym** represents workout sessions, user-defined exercises, and set-level
  performance.

They remain separate in the interface and domain model. They share schedules,
measurement primitives, tags/areas, event history, and an explicit link engine
so one domain can contribute to another without becoming the same thing.

```text
Task/subtask completion ----+
Habit check-ins ------------+--> measurement/event ledger --> link rules --> goals
Workout sets/sessions ------+
Manual measurements --------+
```

Core product principles:

- Keep Tasks, Habits, Goals, and Gym semantically distinct.
- Make all user data local-first and usable without an account.
- Start with empty user-owned libraries instead of pretending sample data is
  real data.
- Use templates only as optional editor presets; templates must create normal,
  fully editable records.
- Use progressive disclosure: simple defaults first, advanced controls behind
  an expandable section.
- Store source events and derive progress from them. Cached statistics are
  projections, never the sole source of truth.
- Make linked contributions explainable, reversible, and resistant to double
  counting.
- Preserve historical meaning when definitions, units, exercises, or links are
  edited.

## Productivity UX parity pass `[x]`

Tasks, Habits, and Goals must be as quick and spatially polished as the active
workout experience. This pass is intentionally presentation-focused: it keeps
the existing domain depth while reorganizing it around capture, daily action,
review, and contextual detail.

- [x] Replace centered, crease-crossing entity dialogs with a shared
  pane-contained, IME-safe editor presentation.
- [x] Reduce the default Tasks workspace to Today, Inbox, Upcoming, quick
  capture, and the task list; place alternate views, archives, filters, and
  bulk tools behind explicit workspace controls.
- [x] Redesign basic Habit creation around tracking shapes (done, amount,
  timer, checklist, rating, limit) and derive technical defaults.
- [x] Replace raw reminder/range syntax with structured builders while keeping
  optional expert text entry.
- [x] Promote Task, Habit, and Goal detail/history/connections out of long
  undifferentiated action dialogs and into clear sections.
- [x] Add useful Habit trends and a first-class Goal Insights destination.
- [x] Remove duplicate create affordances and hide per-record ordering controls
  until Manage order is enabled.
- [x] Add template descriptions, plain-language goal terminology, and
  consistent connection naming.
- [x] Add UI journey, Fold geometry, IME, large-text, accessibility, dense-list,
  template, and cross-domain connection tests for the redesigned flows.

Acceptance criteria:

- No Task, Habit, or Goal editor intersects a separating hinge.
- Save and Cancel remain visible and tappable with the software keyboard open.
- A basic task, check-off habit, and target goal can each be configured without
  encountering aggregation, comparison, raw time syntax, or bulk controls.
- Every removed top-level control remains reachable through a labelled
  workspace, detail, or advanced surface.
- Template selection only prefills an unsaved editor.
- Compact, Fold split, expanded pane, 200% text, TalkBack, and hardware keyboard
  checks pass.

Acceptance evidence on 2026-08-19: all 316 product tests pass (137 JVM and
179 Android instrumentation), including a real Task → Habit → Goal creation and
action journey, template non-persistence, pane geometry in LTR/RTL, 200% text,
IME, recreation, accessibility, repeated notification routing, and destructive
task navigation. Lint, debug, optimized release, and benchmark builds pass. The
signed release was installed in place without clearing release data; local and
installed APK SHA-256 both equal
`8ec6870fd571b8ffe34758f5e2ea712e6aa10b0dd896a4f6885f0be959f6606e`,
and its verified cold launch completed in 145 ms with no release-process crash,
ANR, Room, or SQLite error.

## First-class productivity Areas `[x]`

Areas are the durable Personal/Work/Health-style organizing concept for Tasks,
Habits, and Goals. They are intentionally not called Projects: an Area is an
ongoing context, while a future Project may be finite and belong to an Area.

- [x] Give each Area a stable identity and each Task, Habit, and Goal a nullable,
  indexed `areaId` foreign key.
- [x] Normalize and deduplicate legacy area names in migration 26→27, preserve
  display-name compatibility for one schema generation, and enforce foreign-key
  integrity on the three bounded relationships.
- [x] Add an All/Unassigned/specific-Area global scope across Home, Tasks,
  Habits, Goals, Search, and Review; keep Gym explicitly unscoped.
- [x] Add basic-editor Area pickers with inline creation, inherited Area scope,
  searchable large lists, and accessible tappable badges on record cards.
- [x] Add Area creation, color, ordering, rename/merge, archive, usage counts,
  and safe active-scope transitions in Settings.
- [x] Preserve Area identity and scope through backup/restore/merge, notification
  and widget deep links, saved filters, and CSV export.
- [x] Cover scope behavior, large assignment sets, migration/index/query plans,
  repository integrity, backup remapping, editor creation, badges, fold layouts,
  and accessibility.

Acceptance evidence on 2026-08-20: all 334 product tests pass (142 JVM and 192
Android instrumentation) on the physical Fold, with zero lint errors. The
full v1→27 migration suite, v26→27 Area migration, foreign-key validation,
backup/merge/CSV paths, and Compose Accessibility Test Framework checks pass.

## Market research summary

The following products informed the plan. These are feature references, not a
requirement to clone their interfaces.

- Todoist supports subtasks and lets recurring parents reset those subtasks on
  each occurrence. Whip should expose this explicitly per task.
  [Todoist recurring task behavior](https://www.todoist.com/help/articles/complete-a-task-with-a-recurring-date-dmI6SVqdP)
- TickTick demonstrates demand for tasks, calendars, habits, and statistics in
  one product. Whip will keep separate work areas but provide a unified Home.
  [TickTick features](https://ticktick.com/features?language=en_US)
- Habitify separates a habit's schedule from its numerical goal, supports
  custom units and goal periods, and includes end conditions, checklists,
  areas, and habit-stacking reminders.
  [Habit goals](https://intercom.help/habitify-app/en/articles/12393175-good-habit-setting-goal)
  [Habit creation options](https://intercom.help/habitify-app/en/articles/11203157-add-a-new-habit-on-website-desktop-app)
- Strides uses Habit, Target, Average, and Project tracker archetypes. Whip's
  goal modes expand this useful taxonomy.
  [Strides tracker types](https://www.stridesapp.com/)
- Exist supports generic quantities, decimals, durations, scales, percentages,
  time values, trends, and correlations. Whip should establish similarly
  flexible measurement primitives before adding insights.
  [Exist manual tracking](https://exist.io/page/help-manual-tracking/)
- FitNotes emphasizes fast workout logging, previous-set values, per-exercise
  history, custom exercises/types, routines, supersets, rest timers, records,
  calculators, calendar history, graphs, local storage, backup, and CSV export.
  [Workout tracking](https://www.fitnotesapp.com/workout_tracking/)
  [Exercises](https://www.fitnotesapp.com/exercises/)
  [Routines](https://www.fitnotesapp.com/routines/)
  [Progress tracking](https://www.fitnotesapp.com/progress_tracking/)
  [Workout tools](https://www.fitnotesapp.com/workout_tools/)
  [Data and export](https://www.fitnotesapp.com/settings/)

FitNotes metrics specifically worth supporting include estimated one-rep max,
max weight, max reps, max weight for a rep count, set volume, workout volume,
total reps, personal records, distance, duration, speed, and pace. Its graphs
can aggregate per workout, week, or month and filter by exercise/category.
[FitNotes charts](https://www.getfitnotes.com/docs/stats-graphs-and-charts.html)

The product opportunity is the middle ground between shallow all-in-one tools
and deep but disconnected specialist trackers: Whip should provide
specialist-level task, habit, goal, and workout behavior while connecting them
through explicit, explainable links. Requests from users of integrated products
to link tasks and habits rather than logging the same action twice are
directional, anecdotal evidence for this approach.
[Example TickTick request](https://www.reddit.com/r/ticktick/comments/u78i8x/)

## Current verified baseline

- [x] Native Android project with Kotlin, Compose, Room, and WorkManager.
- [x] API 26 minimum and API 37 target.
- [x] Local-first persistence with exported Room schema version 27 and explicit
  migrations from every prior version.
- [x] Anytime and one-shot tasks.
- [x] Daily, every-N-days, and selected-weekday recurrence.
- [x] Recurrence end date and occurrence-count limits.
- [x] Per-occurrence complete, skip, and reschedule actions.
- [x] Edit-current-and-future recurring series behavior.
- [x] Today, Upcoming, Anytime, and Completed task collections.
- [x] Keep Upcoming compact by showing only the next occurrence of each
  repeating task by default, with a setting to show every occurrence in the
  30-day window.
- [x] Task-time reminders through WorkManager.
- [x] Unit coverage for the recurrence engine.
- [x] Wireless build/install/launch helper in `scripts/device`.
- [x] Persist user-created habits; the library starts empty and templates create
  normal editable records.
- [x] Implement generalized goals.
- [x] Implement the empty-by-default gym tracker, routines, records, and graphs.
- [x] Implement auditable cross-domain contribution, context, and trigger links.

## Information architecture

Use five primary destinations:

1. **Home** — date-oriented overview and quick logging.
2. **Tasks** — Today, Upcoming, Anytime, and Completed tabs.
3. **Habits** — Journal, All Habits, and Insights tabs.
4. **Gym** — Current Workout, History, Exercises, and Routines.
5. **Goals** — Active, Completed, and Archived.

Home requirements:

- [x] Show separate, labeled Tasks, Habits, Goals, and Gym sections.
- [x] Show today's tasks and support completion from Home.
- [x] Show today's habits with controls appropriate to their input type, such
  as check, increment, timer, or rating.
- [x] Show compact goal cards with current value, progress, and pace/status.
- [x] Show an active workout card while a session is in progress.
- [x] Allow quick completion/logging without opening every detail screen.
- [x] Provide a global add menu: Task, Habit, Goal, Exercise, Workout, or
  Measurement.
- [x] Allow Home sections to be reordered, collapsed, or hidden.
- [x] Allow individual tasks, habits, goals, and gym routines to be pinned.
- [x] Do not calculate a combined life/productivity score by default.

## Shared measurement foundation

The shared measurement layer must exist before advanced habits, goals, and
cross-domain progress are built.

Value kinds:

- Boolean/status
- Integer count
- Decimal quantity
- Duration
- Percentage
- Bounded rating/scale
- Time of day
- Checklist completion

Unit dimensions:

- Count/custom count
- Duration
- Distance
- Volume
- Mass
- Length
- Money
- Energy
- Percentage
- Unitless number
- Custom dimension

Rules:

- [x] Store values in a canonical unit while retaining the unit entered by the
  user for display/audit purposes.
- [x] Permit conversions only within compatible dimensions.
- [x] Permit custom units, symbols, dimensions, and canonical conversion factors.
- [x] Lock the underlying dimension after the first entry; changing kg to lb is
  safe, changing mass to duration requires a new metric or an explicit data
  migration.
- [x] Keep missing, zero, failed, skipped, and excused as distinct states.
- [x] Store timestamp, local date, timezone/offset, source, and optional note on
  every entry.
- [x] Support editing/backdating and recalculate all affected projections.
- [x] Define configurable start-of-week, time-zone, and late-night day-cutoff behavior.

Planned shared entities:

- `MetricDefinition`
- `MetricEntry`
- `UnitDefinition`
- `Area`
- `Tag`
- Domain-specific join tables

## UX and editor strategy

Every creation flow should use progressive disclosure so broad flexibility does
not make common setup exhausting.

Basic editor mode:

- Name/title
- Tracking or goal style
- Target where relevant
- Schedule where relevant

Expandable advanced mode:

- Units and precision
- Comparison/aggregation rules
- Reminders and time windows
- End conditions and pause behavior
- Streak/missed-day policy
- Links and automation
- Card/display options

Optional templates may prefill the generic editors for hydration, medication,
reading, exercise, meditation, no-spend days, weight, savings, and project
milestones. Templates must create ordinary records using the shared schema;
they must never require special-case tables or calculations.

## Phase 0 — architecture and migration safety `[x]`

- [x] Record architecture decisions for task occurrences, metric storage,
  progress projection, and links.
- [x] Add a central clock/date abstraction for deterministic tests.
- [x] Add stable ID generation and consistent created/updated timestamps.
- [x] Add explicit Room migrations; never use destructive migration for user
  data.
- [x] Add migration tests starting from the checked-in version 1 schema.
- [x] Add repository interfaces so UI/ViewModels do not directly depend on the
  concrete database.
- [x] Add a common result/error model for validation and persistence failures.
- [x] Replace ViewModel fire-and-forget writes with observable operation status
  where user feedback is required.
- [x] Decide backup/export envelope versioning before new domains are added.
- [x] Restructure navigation into Home, Tasks, Habits, Goals, and Gym without
  removing existing task functionality.

Acceptance criteria:

- Existing task data survives an upgrade from database version 1.
- Existing task tests, lint, and debug builds continue to pass.
- Every future schema phase can add a migration test from the prior version.

## Phase 1 — task subtasks and progress `[x]`

### Task step behavior

- [x] Add, rename, remove, and reorder subtasks from the task editor/detail
  screen.
- [x] Treat subtasks as checklist steps, not independently scheduled tasks.
- [x] Add a "Promote to task" action for a step that needs its own schedule.
- [x] Persist subtask definition separately from per-occurrence state.
- [x] Tie repeating-task step state to the task's original occurrence key.
- [x] Retain historical completed-step snapshots when step names are edited.
- [x] Add a per-recurring-task reset policy:
  - Reset all steps for the next occurrence (default).
  - Carry only unfinished steps forward.
- [x] Add parent completion policy:
  - Auto-complete when every active step completes (recommended default).
  - Manual parent completion.
- [x] Warn before manually completing a parent with unfinished steps.
- [x] Make subtask completion undoable.

### Progress display

- [x] Add `showSubtaskProgress` to tasks.
- [x] Add display styles: percentage, fraction, and both.
- [x] Calculate equal-weight progress first:
  `completed active steps / total active steps`.
- [x] Hide progress when no active subtasks exist.
- [x] Always expose the completed/total count inside task details even if the
  task-card percentage is disabled.
- [x] Keep every subtask equal-weighted. Subtasks deliberately do not expose a
  weight field; progress stays predictable and easy to understand.
- [x] Store final occurrence progress in history when a parent is manually
  completed early.

Planned entities:

- `TaskStepEntity`
- `TaskStepStateEntity`
- `TaskStepSnapshotEntity` or equivalent occurrence snapshot

Tests:

- [x] Percentage for zero, partial, and fully complete step lists.
- [x] Step reset and carry-forward behavior across recurring occurrences.
- [x] Step history after renaming/deleting definitions.
- [x] Parent auto-completion and manual completion warning behavior.
- [x] Room migration and cascade behavior.

Acceptance criteria:

- `3/5` completed steps can display `60%` on a task card.
- Monday's completed recurring steps do not mark Tuesday's steps complete.
- Existing task completion, rescheduling, reminders, and history still work.

## Phase 2 — gym core `[x]`

Gym starts with an empty exercise library. Do not seed exercises such as bench
press, squat, or deadlift. The first empty state explains how to create an
exercise. Optional suggestions may prefill an editor only after the user picks
one; they must not silently create records.

Terminology:

- **Exercise**: a user-defined movement, such as "Flat Barbell Bench Press."
- **Workout session**: the actual training performed on a date.
- **Routine**: a reusable workout/day template.
- **Set**: one recorded effort within an exercise in a workout.

### Exercise library

- [x] Start with zero exercises.
- [x] Create, edit, archive, restore, duplicate, search, favorite, and reorder
  user-defined exercises.
- [x] Require only a name and tracking type.
- [x] Optional exercise fields:
  - Notes/form cues
  - User-created category or muscle tags
  - Equipment
  - Primary and secondary muscles
  - Weight unit
  - Weight/repetition increment
  - Default rest duration
  - Default progress graph
  - Preferred estimated-1RM formula
  - Bar weight and available plates
  - Include/exclude from volume and PR calculations
- [x] Keep categories/tags user-editable; do not force a fixed anatomy model.
- [x] Prevent destructive deletion when history exists; archive instead or
  require an explicit history-deletion flow.

Exercise tracking types:

- [x] Weight + repetitions
- [x] Bodyweight + repetitions
- [x] Assisted bodyweight + repetitions
- [x] Repetitions only
- [x] Weight only
- [x] Distance + duration
- [x] Weight + duration
- [x] Repetitions + duration
- [x] Distance only
- [x] Duration only

The initial user request is satisfied by Weight + Repetitions, but the schema
must not require every exercise to use that pair.

### Current workout/day

- [x] Start a blank workout for today or a selected date/time.
- [x] Give a workout an optional user-defined name and notes.
- [x] Add an existing exercise or create a new one without leaving the workout.
- [x] Add, edit, complete, duplicate, reorder, and remove sets.
- [x] For Weight + Repetitions sets, record decimal weight and integer reps.
- [x] Prefill new set values from the preceding set, then from the exercise's
  most recent workout.
- [x] Show the previous workout's sets beside the current exercise.
- [x] Support planned versus completed sets.
- [x] Support set classifications:
  - Warm-up
  - Working
  - Back-off
  - Drop
  - AMRAP
  - Failure
- [x] Add optional set note, RPE, RIR, tempo, and rest duration.
- [x] Allow the user to hide RPE/RIR/tempo fields globally or per exercise.
- [x] Auto-save each set locally immediately.
- [x] Make deletion undoable during the active session.
- [x] Reorder exercises within a workout.
- [x] Group exercises into supersets/circuits and advance through the group.
- [x] Show live session totals: exercises, completed sets, repetitions, volume,
  and elapsed duration.
- [x] Finish, resume, edit, duplicate, or discard a workout with confirmation.
- [x] Show the active workout on Home and persist it across process death or a
  device restart.
- [x] Offer a keep-screen-awake option only while the workout screen is active.

### Rest timer

- [x] Manual timer available from the active workout.
- [x] Per-exercise default with global fallback.
- [x] Optional automatic start when a completed set is saved.
- [x] Add/subtract time controls.
- [x] Sound, vibration, and notification settings.
- [x] Notification identifies the next exercise/set when known.
- [x] Timer deadline survives backgrounding and process recreation.

### Workout history

- [x] Calendar and chronological list views.
- [x] Filter by exercise, category/tag, routine, date range, or PR.
- [x] Open and edit a historical workout with projections recalculated.
- [x] Copy an exercise's sets or an entire workout into today.
- [x] Show per-exercise and per-workout notes.

Planned entities:

- `ExerciseEntity`
- `ExerciseCategoryEntity`
- `ExerciseCategoryJoinEntity`
- `WorkoutSessionEntity`
- `WorkoutExerciseEntity`
- `WorkoutSetEntity`
- `WorkoutGroupEntity` for supersets/circuits

Tests:

- [x] Empty library behavior.
- [x] Exercise archive versus history deletion.
- [x] Decimal weights and unit normalization.
- [x] Set ordering, completion, copying, editing, and undo.
- [x] Active session restoration.
- [x] Rest timer deadline restoration.
- [x] Workout total calculations.

Acceptance criteria:

- A new user can create "Flat Barbell Bench Press," start today's workout, and
  log `3 x 8 @ 80 kg` without any seeded exercise data.
- Leaving and reopening the app does not lose the active workout.
- Editing an old set updates all affected totals and charts.

## Phase 3 — gym routines, calculations, and analytics `[x]`

### Routines

- [x] Create named reusable routines such as Push, Pull, Legs, or Day A.
- [x] Routines contain ordered exercises and optional planned sets.
- [x] Planned set fields may be fixed or copied from the previous workout.
- [x] Add multiple days/sections to a routine.
- [x] Start a workout from a routine without mutating the routine definition.
- [x] Save a completed workout as a new routine.
- [x] Edit, duplicate, archive, and reorder routines.
- [x] Support supersets/circuits inside routines.

### Strength calculations

- [x] Set volume: normalized load multiplied by repetitions.
- [x] Exercise workout volume: sum of eligible set volumes.
- [x] Workout volume: sum of eligible exercise volumes.
- [x] Total completed sets and repetitions.
- [x] Estimated 1RM per eligible set.
- [x] Highest estimated 1RM per exercise/workout/day.
- [x] Default Epley formula: `weight × (1 + reps / 30)`.
- [x] Optional Brzycki formula: `weight × 36 / (37 - reps)` for valid rep
  ranges.
- [x] Warn that estimates become less reliable at high repetition counts.
- [x] Configurable e1RM rep cutoff, recommended default 10.
- [x] Exclude warm-up/incomplete sets from PRs by default, with a setting.
- [x] Bodyweight load policy per exercise:
  - External weight only
  - Bodyweight plus external weight
  - User-defined effective bodyweight percentage
- [x] Assisted exercise load semantics must be explicit and must not produce
  misleading negative volume.
- [x] Speed and pace for distance + duration exercises.

### Personal records

- [x] Highest actual weight.
- [x] Highest repetitions at a selected weight.
- [x] Best weight for each rep count.
- [x] Highest estimated 1RM.
- [x] Highest set volume.
- [x] Highest workout/exercise volume.
- [x] Max repetitions, distance, duration, and speed; minimum pace.
- [x] Mark new PRs at save time and retain an auditable record history.
- [x] Recalculate records when historical sets change.
- [x] Do not generate celebratory PRs from imported history unless requested.

### Graphs and dashboards

- [x] Exercise graph metrics:
  - Estimated 1RM
  - Max weight
  - Max repetitions
  - Max weight for selected repetitions
  - Set volume
  - Workout volume
  - Total repetitions
  - Actual rep-max history
  - Distance
  - Duration
  - Speed/pace
- [x] Date ranges: month, 3 months, 6 months, year, all, and custom.
- [x] Aggregation: per workout/day, week, and month.
- [x] Tap a point to open the source workout.
- [x] Pan long graphs horizontally and use ranges as bounded zoom controls.
- [x] Compare a small set of compatible exercises on one graph.
- [x] Save graph/filter presets.
- [x] Workout/category breakdown charts.
- [x] Weekly summaries:
  - Workouts and training days
  - Session duration
  - Sets/repetitions/volume
  - Volume and hard sets by user-defined category/muscle
  - New personal records
- [x] Clearly label derived estimates and formulas.
- [x] Downsample long histories without losing source records.

### Workout tools

- [x] Standalone 1RM and rep-max calculator.
- [x] Percentage/set calculator using an actual or estimated max.
- [x] Plate calculator with bar weight, available plates, and unit.
- [x] Copy/share a readable workout summary.
- [x] CSV export for exercises, workouts, and sets.

Tests:

- [x] Volume calculations across kg/lb inputs.
- [x] Epley and Brzycki boundary cases and rep cutoff.
- [x] Bodyweight/assistance policies.
- [x] PR creation, tie behavior, and historical recalculation.
- [x] Daily/weekly/monthly graph aggregation.
- [x] Routine instantiation does not mutate its template.

Acceptance criteria:

- Each exercise exposes meaningful history and graphs appropriate to its type.
- Estimated 1RM and volume match documented formulas for fixture workouts.
- A historical edit deterministically updates records, summaries, and graphs.

## Phase 4 — habits `[x]`

### Habit creation and organization

- [x] Remove in-memory starter habits.
- [x] Start with an empty user habit list and optional setup templates.
- [x] Create, edit, duplicate, archive, restore, search, and reorder habits.
- [x] Add icon, color, notes, area, and tags.
- [x] Choose intent: build, limit, avoid, or observe/log only.
- [x] Keep the Habits area separate while showing due habits on Home.

### Habit tracking modes

- [x] Check-off/completion.
- [x] Count.
- [x] Decimal quantity.
- [x] Duration with optional timer.
- [x] Reusable checklist/routine.
- [x] Rating scale.
- [x] Limit/avoid behavior.
- [x] Log-only metric with no success threshold.

### Targets and schedules

- [x] Target comparisons: at least, at most, exactly, within range, or none.
- [x] Target periods: occurrence, day, week, month, or rolling N days.
- [x] Keep target period separate from schedule.
- [x] Schedules: daily, every N days, selected weekdays, or flexible N times per
  week/month.
- [x] Start/end dates and pause periods.
- [x] Morning, afternoon, evening, or custom time window.
- [x] End after a date, streak, completion count, or total amount.
- [x] Quick increment amount and custom quick-action buttons.
- [x] Multiple time reminders and optional per-weekday times.
- [x] Skip/excused/missed states.
- [x] Per-habit streak rules and week-start behavior, with global defaults.
- [x] Require an explicit policy for avoid-habit success; missing data cannot
  silently imply success unless the user chooses that behavior.
- [x] Streaks, completion rate, totals, averages, and calendar heatmap.
- [x] Archive and restore while retaining all historical logs.
- [x] Expose goal links in habit actions once the link engine exists.
- [x] Expose habit-stacking triggers once automation exists.

Example abstraction for "6 of 8 glasses":

- Tracking kind: integer count
- Comparison: at least
- Target: 8
- Unit: user-defined `glass`
- Period: day
- Quick increment: 1
- Displayed value: sum of today's logs, e.g. `6 / 8`

Planned entities:

- `HabitEntity`
- `HabitTargetEntity`
- `HabitScheduleEntity` or shared schedule representation
- `HabitLogEntity`
- `HabitChecklistItemEntity`

Tests:

- [x] Threshold behavior for every comparison type.
- [x] Daily versus flexible weekly targets.
- [x] Missing/zero/skipped/excused distinctions.
- [x] Unit conversion and quick increments.
- [x] Streak behavior across timezone/day-cutoff changes.

Acceptance criteria:

- Hydration, reading, medication, meditation duration, exercise frequency,
  ratings, and avoidance habits can be created without special-case code.

## Phase 5 — goals `[x]`

Goal modes:

- [x] Reach a latest value, e.g. reach 75 kg.
- [x] Reduce to a latest/average value, e.g. reduce screen time.
- [x] Accumulate a total, e.g. run 500 km or save $5,000 in contributions.
- [x] Maintain a range, e.g. remain between 70 and 75 kg.
- [x] Meet an average, e.g. average 7.5 hours of sleep.
- [x] Consistency across periods, e.g. exercise 3 times/week for 12 weeks.
- [x] Weighted milestone/project goal, e.g. release an application.
- [x] Open-ended tracker/trend without a terminal target.

Goal settings:

- [x] Name, description, icon, color, area, and tags.
- [x] Baseline, target/range, unit, precision, and increase/decrease direction.
- [x] Start date and optional deadline.
- [x] Aggregation: latest, sum, average, minimum, maximum, completion count, or
  time in range.
- [x] Ask whether an entry is a current total or an amount to add.
- [x] Linear pace, milestone pace, or no expected pace.
- [x] Manual measurements, notes, and backdating.
- [x] Progress percentage, current value, delta, pace, forecast, and history.
- [x] Optional reminder to record a measurement.
- [x] Pause, complete, abandon, archive, and restore while retaining history.
- [x] Weighted goal milestones and linked tasks.
- [x] Optional milestone rewards without requiring gamification.

Planned entities:

- `GoalEntity`
- `GoalMilestoneEntity`
- `GoalMeasurementEntity` or shared `MetricEntry`
- Derived `GoalProgressProjection`

Tests:

- [x] Increasing and decreasing progress formulas.
- [x] Baseline equal to target.
- [x] Cumulative, latest, average, consistency, and time-in-range aggregation.
- [x] Deadline pace and timezone boundaries.
- [x] Historical edits and goal completion snapshots.

Acceptance criteria:

- Weight, savings, distance, reading, sleep, consistency, and milestone goals
  all use the same generalized system.

## Phase 6 — cross-domain links and automation `[x]`

Keep three link concepts separate:

1. **Contribution** updates goal progress.
2. **Context** associates an item with a goal without changing progress.
3. **Trigger** causes a reminder or reveals the next action.

Required contribution mappings:

- [x] Habit numeric value -> sum into goal.
- [x] Habit measurement -> latest goal value.
- [x] Habit success -> consistency goal count.
- [x] Task completion -> goal milestone or fixed contribution.
- [x] Subtask completion -> parent progress and optional goal milestone.
- [x] Completed workout -> habit success, without a second manual check-in.
- [x] Workout count/session duration/volume -> cumulative or consistency goal.
- [x] Exercise estimated 1RM/max weight -> latest-value strength goal.
- [x] Exercise distance/repetitions/volume -> cumulative goal.
- [x] Gym/body measurement -> weight or other measurement goal.

Example links:

| Source | Transform | Goal |
| --- | --- | --- |
| Running exercise logs 5 km | Add normalized distance | Run 500 km |
| Weigh-in habit logs 78.2 kg | Use latest value | Reach 75 kg |
| Reading habit logs 20 pages | Add quantity | Read 10,000 pages |
| Complete "Release beta" task | Complete weighted milestone | Publish whip |
| Finish a workout | Count one qualifying day | Train 3 days/week |
| Bench press working set | Use highest valid e1RM | Reach 120 kg bench e1RM |

Link safety:

- [x] Give every source event a stable identity.
- [x] Enforce at most one contribution per link/source-event pair.
- [x] Show the source of every contribution in the goal history.
- [x] Recalculate when source data is edited or deleted.
- [x] Validate unit compatibility and transformation direction.
- [x] Reject circular dependencies.
- [x] Make retroactive backfill opt-in with a preview.
- [x] Permit exclusion/override without deleting the source event.
- [x] Avoid automatic bidirectional mirroring in the initial implementation.

Trigger automation:

- [x] Habit stacking: remind/reveal B after A completes, fails, or is skipped.
- [x] Optionally offer the next task after a habit or workout completes.
- [x] Delay and quiet-hour controls.
- [x] Cycle detection and rate limiting.

Planned entities:

- `LinkRuleEntity`
- `ContributionEntity`
- `TriggerRuleEntity`
- `ProjectionCheckpointEntity` if performance requires it

Tests:

- [x] Idempotence and double-count prevention.
- [x] Edit/delete/undo recalculation.
- [x] Unit conversion and incompatible links.
- [x] Cycle rejection.
- [x] Retroactive backfill preview and commit.
- [x] Workout-to-habit linking does not create duplicate check-ins.

Acceptance criteria:

- Every linked goal value can explain exactly which task, habit, workout, set,
  or manual measurement produced it.

## Phase 7 — insights, integrations, and data portability `[x]`

- [x] Unified search across all domains, including archived records.
- [x] Configurable weekly/monthly review.
- [x] Cross-domain trend overlays.
- [x] Correlations only after sufficient observations; label correlation as
  non-causal and expose sample size.
- [x] Home widgets and quick actions.
- [x] Notification action buttons where safe.
- [x] Full versioned local backup and restore, including preferences and background-state rebuild.
- [x] CSV export per domain and a complete machine-readable export.
- [x] Optional Health Connect integration on supported devices for user-selected
  types such as weight, steps, distance, hydration, sleep, and exercise.
- [x] Request only the exact Health Connect permissions required for enabled
  links and provide sync/access controls in Settings.
- [x] Never require Health Connect for manual tracking.

Health Connect reference:

- [Android Health Connect overview](https://developer.android.com/health-and-fitness/health-connect)
- [Permissions and data access](https://developer.android.com/health-and-fitness/health-connect/ui/permissions)
- [Publishing and data declarations](https://developer.android.com/health-and-fitness/health-connect/publish)

## Phase 8 — production hardening and release readiness `[x]`

- [x] Accessibility audit: screen readers, touch targets, focus order, contrast,
  dynamic type, and non-color status cues.
- [x] Phone, tablet/foldable, portrait, landscape, max-width, and edge-to-edge layout coverage.
- [x] Performance tests with multi-year habit, goal, and workout histories.
- [x] Database index/query review using realistic fixture sizes.
- [x] Background work and notification reliability tests.
- [x] Battery-use review for timers, reminders, and integrations.
- [x] Privacy policy and in-app data controls.
- [x] Health/fitness disclaimer; do not present estimated 1RM or correlations as
  medical or safety advice.
- [x] Crash-safe import, restore, migration, and projection rebuild.
- [x] Release signing and secure secret handling.
- [x] Automated unit, migration, UI, lint, and release build checks.
- [x] Device test matrix is configured for API 26, 28, and 35 in CI. The current
  Android 17 physical device passes all 46 instrumentation tests; the older API
  jobs were not duplicated locally because this WSL workspace has no emulator
  system images installed.
- [x] Wireless deployment smoke test on a physical device.
- [x] User documentation for backups, measurements, formulas, links, and data
  deletion.

## Refinement pass — 2026-08-18 `[x]`

- [x] Replace text-symbol controls with consistently sized Material icons and
  at least 48 dp command targets across the app bar, navigation, tasks, habits,
  goals, and gym screens.
- [x] Move dense gym commands into labeled overflow menus and make task
  destinations wrap on narrow screens.
- [x] Remove weighted subtasks; keep one-level checklist items with a title,
  notes, ordering, promotion to a full task, and equal progress contribution.
- [x] Validate saved unit preferences and accept legacy aliases such as
  `pounds`, `lb`, and `lbs`.
- [x] Apply preferred units and decimal precision to gym logging, routines,
  summaries, graphs, records, tools, sharing, and new habit/goal defaults.
- [x] Apply configured first-day-of-week behavior to task/habit weekday order,
  gym calendars, weekly summaries, and weekly graph aggregation.
- [x] Verify Kotlin compilation, unit tests, lint, all 42 physical-device
  instrumentation tests, and a wireless deployment/visual smoke test.

## Automation reliability and foldable pass — 2026-08-18 `[x]`

- [x] Make a completed workout deterministically rebuild linked habit and goal
  projections before reporting completion.
- [x] Treat every completed workout as one auditable habit check-in, including
  legacy workout-to-habit rules whose old automatic-check-in flag was false.
- [x] Count individual successful source events for flexible weekly/monthly
  habits, so multiple legitimate workouts on one date remain visible.
- [x] Display flexible habit progress as completions in the active period, such
  as `3 / 4 completions this week`.
- [x] Restrict workout automation to the Completed outcome exposed by the
  workout event model.
- [x] Add live-order, repeat-build, undo/resume, legacy-rule, and invalid-outcome
  automation regressions.
- [x] Add a single local/CI quality command and a feature coverage matrix that
  makes required tests explicit for future work.
- [x] Observe live folding features and resize continuously rather than caching
  a device category.
- [x] Use bottom navigation on compact phones, persistent navigation on wide
  windows, and hinge-aware book/tabletop layouts on folding devices.
- [x] Treat the Fold's flat large inner display as a two-pane surface even when
  its crease is reported as non-separating, and retain an overview pane on
  expanded windows that do not report a folding feature.
- [x] Make the secondary fold pane useful with live, clickable task, habit,
  goal, and workout status instead of merely stretching phone content.
- [x] Add deterministic posture/window tests and Compose semantics tests for
  book and tabletop layouts.
- [x] Theme-fill fold pane and system-inset boundaries so dark mode cannot
  expose the activity window background; guard the top-left region with a
  physical-device pixel regression.
- [x] Keep all four At-a-glance cards at one measured width in vertical panes
  and one shared fixed width in horizontal/tabletop layouts.
- [x] Let users expand the primary pane across the unfolded display and restore
  the split from an accessible app-bar control; retain destination navigation
  while focused.
- [x] Pass all 99 automated tests (53 JVM and 46 physical-device), lint, Android
  test compilation, and debug build gates; deploy the verified debug APK.

## Persistence and end-to-end hardening pass — 2026-08-18 `[x]`

- [x] Audit feature coverage by product capability across deterministic rules,
  Room repositories, migration/backup integration, Compose, and real-device E2E.
- [x] Add a real application journey that seeds Tasks, Habits, Goals, and Gym,
  renders them through Room/ViewModels/Compose, navigates domain screens, and
  verifies state after Activity recreation.
- [x] Fix the Home crash found by that journey when a habit and goal shared the
  same numeric Room ID by domain-qualifying all cross-section lazy-list keys.
- [x] Prove core on-device persistence by closing and reopening a file-backed
  Room database containing task/subtask, habit/log, goal/measurement, and
  workout/set data.
- [x] Expand full backup coverage across tasks, subtasks, occurrences, habits,
  goals, measurements, gym, routines, PRs, graphs, and links.
- [x] Prove tampered backups fail checksum validation without replacing live data.
- [x] Let the user select a portable backup folder through Android's Storage
  Access Framework and retain its narrow read/write URI permission across restarts.
- [x] Verify every folder backup by reopening it and comparing checksum and row
  count before recording success or pruning history.
- [x] Support one-tap backup, 1–30 file retention, safe unrelated-file handling,
  visible last-success/error state, change-folder, and forget-folder flows.
- [x] Add one uniquely scheduled, battery-aware daily WorkManager backup and
  protect the last useful archives by skipping automatic writes from an empty DB.
- [x] Keep one-off backup/restore and per-domain CSV export available alongside
  the remembered folder workflow.
- [x] Document device-to-device restore, privacy/encryption boundaries, external
  file lifecycle, and the platform-picker test boundary.
- [x] Pass all 112 automated tests (57 JVM and 55 physical-device), lint,
  Android-test compilation, debug build, deployment, and physical launch smoke.

## Task lifecycle and feature/UX audit — 2026-08-18 `[x]`

- [x] Keep Stop series/archive reversible and add a visually separate permanent
  task/series deletion action with explicit second confirmation.
- [x] Delete task definitions, subtasks, occurrence state/history, task-owned
  links and automations, derived goal measurements/generated habit logs, and
  reminders without leaving orphans.
- [x] Allow a completed recurring occurrence to be reopened while retaining its
  occurrence-specific schedule and current subtask state.
- [x] Fix source-qualified automation deduplication so rebuilding a task-to-habit
  trigger cannot add duplicate generated check-ins.
- [x] Make long task action dialogs scrollable and clarify archive language.
- [x] Isolate debug builds and instrumentation data from the signed release app.
- [x] Add repository, cross-domain, and Compose regressions for the new lifecycle
  behavior; current baseline is 117 tests (57 JVM and 60 instrumentation).
- [x] Record the simple-user/power-user audit, verified capability matrix,
  market signals, and prioritized P1/P2 findings in
  `docs/feature-ux-audit.md`.

## P1 feature and UX improvements — 2026-08-18 `[x]`

- [x] Add one task-series history for completed, skipped, and moved occurrences
  with occurrence-level recovery.
- [x] Add Basic/Advanced progressive disclosure to task, habit, and goal editors,
  plus common goal templates.
- [x] Make every global search result open or focus the exact selected record.
- [x] Add dependency-aware permanent deletion for habits, goals, exercises,
  routines, and workouts while retaining archive as the safe default.
- [x] Give gym charts accessible summaries, labeled bounds, and inspectable data
  points.
- [x] Add notification delivery diagnostics, Android settings shortcuts, and a
  test-notification action.
- [x] Expand repository/Compose/device regressions and deploy the signed release.

## P2 power-user expansion — 2026-08-18 `[x]`

- [x] Add task priorities, reusable area/tag values, persistent named filters,
  multi-select bulk actions, and list/agenda/month-calendar planning views.
- [x] Add monthly/yearly recurrence, scheduled- versus completion-anchored
  cadence, multiple reminder offsets, separate work/deadline dates, and an
  optional on-device smart-capture parser.
- [x] Apply named task filters on Home, add saved Review section filters, and
  make every Home/Fold/Review summary count drill into its owning area.
- [x] Accelerate workouts with a visible next-set target and explicit jump,
  configurable compact set rows, saved plate presets shared with exercise
  setup, and direct pinned routine-day shortcuts.
- [x] Retain the six editable goal templates for weight, savings, distance,
  reading, consistency, and weighted projects.
- [x] Add opt-in arrival/leave task cues using Android geofencing, one-tap
  current coordinates, a global kill switch, permission diagnostics, reboot
  restoration, local-only coordinates, and clear battery/privacy disclosure.
- [x] Migrate the Room schema to version 14 without data loss, keep older Whip
  backups restorable through column defaults, and extend backup/CSV settings
  coverage for every new preference and task field.
- [x] Add deterministic, repository, migration, Compose, and physical Fold
  regressions for this expansion, reach 136 tests (65 JVM + 71 device), and
  close every P2 audit item.

## Machine-aware gym tracking and unit clarity — 2026-08-18 `[x]`

- [x] Explain custom units by dimension and name every canonical base directly,
  including mass-to-kilogram examples instead of implying custom units are only
  hydration volumes.
- [x] Add user-owned machine profiles tied to an exercise, with a stable name,
  location/setup notes, archive lifecycle, and either a kg/lb mass stack or an
  ordinal setting scale such as pin 1–10.
- [x] Record immutable machine identity and resistance semantics on each workout
  exercise so later profile edits cannot reinterpret old history.
- [x] Keep previous sets, routine carry-forward, personal records, and progress
  graphs scoped to the exact machine; never merge Home pin 7 with a public-gym
  stack or with another machine that happens to show the same number.
- [x] Exclude ordinal settings from mass, volume, and estimated-1RM math while
  providing a machine-scoped highest-setting graph and record.
- [x] Preserve machine identity/settings in routines, workout duplication,
  sharing, full backups, and gym CSV exports.
- [x] Migrate the Room schema to version 15 and cover custom mass conversion,
  ordinal/mass persistence, routine propagation, machine-scoped analytics,
  migration, and Compose discoverability with automated regressions; current
  baseline is 142 tests (66 JVM + 76 device).

## Brand mark refinement — 2026-08-18 `[x]`

- [x] Replace the layered outline launcher icon with a soft, vector-native W
  ribbon using near-black and warm white with a restrained overlap seam.
- [x] Add a dedicated Android monochrome layer for themed icons and use the same
  mark consistently in the top app bar and foldable navigation rail.
- [x] Retain the generated visual concept in `docs/brand` and verify the final
  deterministic vectors through Android resource compilation and device review.

## Equipment-native numeric input and power-user UX audit — 2026-08-18 `[x]`

- [x] Make kg/lb an exercise-owned choice and use real equipment defaults when
  switching systems: 45 lb/20 kg bars, standard plate inventories, and 5
  lb/2.5 kg entry increments instead of converted decimal artifacts.
- [x] Make routines, current/previous sets, sharing, single-exercise progress,
  and personal records honor the exercise or machine unit; retain the global
  gym unit for aggregate summaries and multi-exercise chart axes.
- [x] Let machine profiles accept compact ranges such as `1-10` or `50-500`
  with an explicit increment, or exact irregular lists; persist expanded exact
  values and drive workout −/+ controls from them.
- [x] Inherit the exercise unit for new machines, keep ordinal settings out of
  mass calculations, compact large-stack previews, and avoid malformed chip
  rows for large ranges.
- [x] Extend range/value quick-entry to habits, remove trailing `.0` noise from
  whole-number editors, make long quick-action rows scroll, and explain global
  versus record-owned unit behavior in Settings.
- [x] Add deterministic parser/default/stepper tests, persisted pound-equipment
  and stack tests, and Compose regressions for pound switching and ordinal
  ranges. The verified baseline is 153 tests (74 JVM + 79 device).

## Global settings inventory

- [x] Appearance/theme and Home customization.
- [x] System locale, first day of week, device-or-pinned time-zone behavior, and day cutoff.
- [x] Default mass, distance, volume, and gym weight units.
- [x] Number precision and rounding rules.
- [x] Default task subtask-reset behavior.
- [x] Upcoming repeating-task occurrence visibility.
- [x] Smart task capture, saved task/Home/Review filters, and location-cue kill switch.
- [x] Habit missed-day and streak behavior.
- [x] Gym e1RM formula, valid rep cutoff, rest timer, sounds, vibration,
  keep-screen-awake behavior, compact set rows, and portable plate presets.
- [x] Notification quiet hours.
- [x] Backup/export/import.
- [x] Health Connect connection and permissions.
- [x] Archived data management.
- [x] Delete-all-data flow with explicit confirmation, backup suggestion, and background-work cancellation.

## Quality gates for every phase

- [x] Domain rules have deterministic unit tests.
- [x] Every schema change has forward migration tests from all supported prior
  versions.
- [x] UI instrumentation covers launch, accessible navigation, empty states, and
  a Compose semantics path through global add, the Habit editor, and Tasks;
  repository/device tests cover persisted happy paths.
- [x] Lint has zero errors; new warnings are fixed or documented.
- [x] `testDebugUnitTest`, Android test compilation, `lintDebug`, and
  `assembleDebug` pass.
- [x] The debug APK launches on a physical device before a phase is marked
  complete.
- [x] README and this plan are updated with any changed behavior or scope.

Cross-phase engineering coverage must explicitly include:

- [x] Recurring subtask reset and carried-state cases.
- [x] Zero-subtask percentage behavior.
- [x] Unit conversions and incompatible dimensions, including custom factors.
- [x] Daily, weekly, monthly, and rolling target windows.
- [x] Missing versus zero measurements.
- [x] Timezones and configurable day cutoffs.
- [x] Increasing/decreasing, range, average, consistency, and time-in-range goal formulas.
- [x] Link idempotency, source edits/deletes, overrides, and cycle rejection.
- [x] Archived Tasks, Habits, Goals, Exercises, and Workouts retaining history.

## Original task/habit/goal plan traceability

The original product plan remains fully in scope; Gym extends it and does not
replace or narrow it:

- Original navigation/Home design -> Information Architecture and Phase 0.
- Original subtask/progress design -> Phase 1.
- Original generic habit types, units, targets, schedules, streaks, and
  reminders -> Shared Measurement Foundation and Phase 4.
- Original reach, accumulate, reduce, range, average, consistency, milestone,
  and open-ended goals -> Phase 5.
- Original contribution, context, and trigger links -> Phase 6.
- Original insights, templates, Health Connect, import/export, and correlation
  cautions -> Phase 7.
- Original engineering test list -> per-phase tests and global quality gates.

## Deliberate non-goals until the foundations are stable

- Social feeds, public profiles, leaderboards, and competitive rankings.
- AI-generated medical, training, or weight-loss advice.
- Automatic workout programming that hides its reasoning.
- Mandatory accounts or cloud synchronization.
- Community exercise databases that populate the user's library without their
  explicit action.
- Arbitrarily deep task nesting; subtasks begin as one-level checklist steps.
- Treating correlation as proof that one behavior caused another outcome.

## Focus-group follow-through — 2026-08-18 `[~]`

This program incorporates the full lifter, productivity, quantified-self,
accessibility, privacy, and Fold 8 Ultra walkthrough. The user explicitly
requested every item, including the ideas initially recommended for deferral;
the ordering below is dependency-based rather than a reduction in scope.

### Phase 16 — correctness and truthful outcomes `[x]`

- [x] Apply the Upcoming recurrence visibility preference and the documented
  30-day horizon identically in List, Agenda, and Calendar views.
- [x] Give recurring completion an immediate Undo path, paginate complete series
  history, define a user-selectable missed-occurrence policy, and make “this and
  future” edits create an immutable series boundary that cannot rewrite history.
- [x] Make numeric and avoidance habits expose explicit `+`, custom quick-action,
  decrement, Set, and Undo controls without requiring a hidden secondary dialog.
- [x] Calculate flexible weekly/monthly streaks and completion rates by target
  period, honoring the configured first day of week, DST, skips, excuses, pause,
  and the missed-day policy instead of treating valid rest days as failures.
- [x] Make Review and correlations count successful habit periods and meaningful
  goal outcomes rather than raw log-entry counts; explain partial progress.
- [x] Fix free-weight graph comparison gating so only genuinely machine-scoped
  history disables cross-exercise comparison.
- [x] Reject incomplete, negative, non-finite, and out-of-domain workout values;
  enforce RPE/RIR/rating/rest/repetition bounds while preserving legitimate
  assisted-lift and custom-equipment semantics.
- [x] Give Habit, Goal, and Gym honest loading, durable failure, retry, and
  accessible operation-result states rather than flashing empty content.

Acceptance evidence:

- [x] List/Agenda/Calendar recurrence tests cover preference off/on and exclude
  dates beyond 30 days.
- [x] Flexible 3x/week and monthly tests span arbitrary days, week boundaries,
  every first-day-of-week option, DST, pause/skip/excuse, edits, and restore.
- [x] A 6-of-8 habit is exercised through every quick action, partial and complete
  outcomes, Undo, recreation, linked-goal rebuild, and backup round trip.
- [x] Free-weight and every machine-scope graph combination has a regression test.

### Phase 17 — reliable reminders and lifecycle continuity `[~]`

- [x] Request notification permission in context when any Task, Habit, Goal, or
  rest-timer notification is enabled; handle denial, rationale, permanent denial,
  and recovery in Settings without pretending the reminder is healthy.
- [x] Diagnose app notification status and Task, Habit, Goal, and rest-timer
  channels independently, with direct system-settings repair actions.
- [x] Stop Habit reminders once the current daily/flexible target is satisfied;
  add exact-record deep links, Snooze, numeric increment actions, and action
  idempotency across retries, reboot, timezone changes, and DST.
- [x] Make notification Task completion use the same unfinished-subtask rules,
  confirmations, history, Undo, and recurrence behavior as in-app completion.
- [x] Preserve every dirty Task, Habit, Goal, exercise, machine, routine, and set
  editor plus navigation destination, tabs, filters, calendar date, scroll,
  selection, pane/fullscreen state, and pending dialog across rotation, live
  fold/unfold, resize, multi-window, Activity recreation, and process recreation.
- [~] Restore keyboard/IME focus predictably after recreation and posture changes;
  support Tab/Shift-Tab, Enter, Escape, mouse, and stylus without duplicate saves.

Acceptance evidence:

- [~] Permission/channel tests cover denied, rationale, permanent denial, every
  individually disabled channel, battery restriction, reboot, timezone, and DST.
- [~] Notification taps/actions prove exact routing, satisfied-reminder suppression,
  Snooze timing, numeric updates, idempotency, and subtask parity.
- [~] Every dirty editor and navigation state survives the complete compact/Fold
  8 Ultra posture, IME, resize, recreation, and process-death matrix.

### Phase 18 — crash-safe, private, portable storage `[~]`

- [x] Write portable backups to an unmistakable staging document, flush and close,
  parse/checksum/read-back verify, and only then commit the final visible name;
  clean recoverable staging artifacts on the next run.
- [x] Validate every retention candidate before counting or pruning it so a corrupt
  or interrupted newest file can never displace the last known-good backup.
- [x] Make restore a recoverable staged operation covering Room, settings, and
  scheduled background work; maintain a recovery marker/snapshot and either
  finish or roll back after failure/process death. Make the UI describe the exact
  transactional boundary honestly.
- [x] Check in and restore full backup fixtures for every supported schema version,
  including rich task recurrence, custom measurements, links, machine semantics,
  routines, and multiple same-day workout sessions.
- [x] Add an optional passphrase-encrypted, authenticated portable backup format
  with explicit plaintext interoperability; never retain the passphrase.
- [x] Keep domain CSV as an export-only interoperability format; Whip-native JSON
  remains the sole restore/merge format.

Acceptance evidence:

- [x] Fault injection covers interruption after every write/verify/rename/restore
  stage, offline or permission-revoked providers, low storage, duplicate names,
  corrupt newest files, retention, wrong passwords, tampering, and recovery.
- [~] Signed-release upgrade and every legacy fixture prove no history or settings
  loss and correct reconstruction of reminders/geofences/background work.

### Phase 19 — explicit gym meaning and faster logging `[~]`

- [x] Add an exercise-owned load interpretation: total system, per hand, per side,
  added external load, bodyweight plus/percentage, assisted subtraction, machine
  displayed mass, or ordinal setting. Explain the effect on volume/e1RM/graphs.
- [x] Snapshot load interpretation, multiplier, exercise type, unit, bodyweight
  policy, e1RM policy, and equipment configuration into each performed set or
  immutable workout-exercise record so later edits never reinterpret history.
- [x] Version structured machine configuration including seat/back positions,
  attachment, pulley ratio, independent/dual stacks, add-on plates, stack labels,
  ordinal levels, mass mapping, location, and user notes while keeping comparisons
  restricted to compatible versions selected by the user.
- [x] Add inline machine creation and selection during a workout without losing
  the pending set.
- [x] Replace modal-first logging with validated inline or Save-and-next entry,
  previous-value fill, exact machine history, deterministic increments, copy set,
  repeat last set, recent-session choices, keyboard/IME next-field flow, immediate
  persistence, and superset-aware next exercise/rest timer behavior.
- [x] Separate immutable routine prescription from performed sets, preserving
  targets when actual weight/reps/RPE/RIR differ; add per-day exercise ordering,
  selected-only/search views, duplication, and progression/history inspection.
- [x] Make plate profiles inventory-aware with per-denomination quantities,
  bar/collar/sled/base load, per-side semantics, compatible units, and a bounded
  closest-load solver for irregular equipment; never recommend unavailable plates.
- [x] Preview kg/lb or interpretation changes with Convert values, Keep entered
  numbers, Reset to equipment standard, or Cancel and show affected defaults and
  future analytics before committing.
- [x] Make analytics honest and configurable: hard-set definition, overlapping
  category allocation, e1RM formula plus RPE/RIR adjustment, comparison
  compatibility, historical policy snapshots, unilateral sets, assisted/negative
  external loads, PR rules, and visible exclusions/data-quality explanations.

Acceptance evidence:

- [x] Unit/repository tests cover total/per-hand/per-side volume and e1RM, each
  bodyweight/assistance policy, ordinal exclusion, historical snapshots, config
  version compatibility, dual stacks, pulley ratios, and unilateral work.
- [x] Plate tests prove exact and closest solutions respect inventory quantities
  for standard and irregular kg/lb profiles.
- [~] Device tests prove a full workout can be logged, edited, reordered, copied,
  supersetted, timed, finished, duplicated, restored, and graphed without a modal
  per set or silent reinterpretation.

### Phase 20 — focused simple mode and productivity power tools `[x]`

- [x] Add a skippable first-run setup for enabled Home areas, default units,
  notification intent, privacy/backup choice, and simple versus power mode;
  every area remains available later and no account is required.
- [x] Add guided plain-language recipes for common tasks, flexible habits, numeric
  targets, avoid habits, linked goals, and workouts while retaining fully custom
  creation and progressive disclosure.
- [x] Add a true Inbox/triage state, task duplication/templates, duration and
  effort, Plan My Day with capacity, gentle defer/snooze, bulk postpone, optional
  focus timer, and a low-pressure/no-streak presentation.
- [x] Expand global search and saved filters to area/tag/date/status/deadline,
  Boolean multi-tag logic, and planning fields; make List/Agenda/Calendar controls
  contextual and honor first-day-of-week everywhere.
- [x] Use completion date for completed-calendar semantics, provide full searchable
  history/pagination, and add reversible destructive and bulk operations.
- [x] Turn the Fold support pane into contextual master/detail for selected Task,
  day agenda, Habit period, Goal trend, Review drilldown, or live workout while
  preserving the explicit full-pane option.

Acceptance evidence:

- [x] Casual/simple and power-mode E2E journeys prove optional areas do not hide or
  delete data and common capture/check-in/set actions remain one-step from Home.
- [x] Inbox, capacity, bulk, filters, recurrence boundaries, completion dates,
  contextual Fold detail, and keyboard operations have deterministic tests.

### Phase 21 — goal insight, Health Connect, and performance `[x]`

- [x] Add inspectable Goal charts, full paginated history, target/range overlays,
  rate, forecast, milestones, confidence/data-quality explanations, and accessible
  data tables for reach/reduce/range/average/consistency/open-trend goals.
- [x] Expose supported Health Connect measurements as user-selectable Habit and
  Goal sources with explicit units, permissions, provenance, deletion/update
  idempotency, backfill preview, and no medical interpretation.
- [x] Replace global once-per-second Gym-state reconstruction with scoped timer
  state and indexed/precomputed history projections after measuring the current
  implementation.
- [x] Add Macrobenchmark and Baseline Profile coverage for cold/warm startup, Home
  with 10k tasks/logs, active-workout input latency, Fold transition, and gym/goal
  graphs with 10k/100k points; set evidence-based jank/memory/battery budgets.

Acceptance evidence:

- [x] Every Goal type has deterministic chart/forecast/history and accessible-table
  tests including missing/zero/partial data, unit conversion, and large histories.
- [x] Fake Health Connect sync proves permission, import, update, deletion, linked
  rebuild, backfill, provenance, and restore behavior.
- [x] Benchmarks and Baseline Profile generation run in CI/release documentation,
  and optimization claims cite measured before/after evidence.

### Phase 22 — accessibility and release acceptance gate `[~]`

- [~] Enable Compose accessibility checks and resolve all actionable findings;
  add meaningful action labels, selected/state descriptions, traversal order,
  headings, live regions, error semantics, and chart/table alternatives.
- [~] Reflow all two-column and dense editors at 200% font without clipped text,
  unreachable controls, horizontal scrolling, or obscured IME content; preserve
  at least 48dp targets in compact and expanded layouts.
- [~] Verify static and dynamic light/dark contrast, system dynamic colors, RTL,
  locale decimal commas, long translations, reduced motion, and screen-reader
  announcements for loading, success, failure, timers, Undo, and destructive UI.
- [~] Add screenshot-golden coverage for compact, navigation rail, book, tabletop,
  Fold 8 Ultra expanded split/fullscreen, continuous resize, empty/dense/error
  data, all themes, 100–200% font scale, and RTL.
- [ ] Run TalkBack linear navigation, Switch Access, hardware keyboard, mouse,
  stylus, multi-window, posture changes, and editor-plus-IME tests on the physical
  Fold 8 Ultra before release.
- [~] Update README, backup/privacy documentation, migration fixtures, test counts,
  release signing, and device deployment instructions; produce and install a
  verified non-debug release APK only after all phases and gates pass.

Automated implementation is complete through the API/emulator-facing portions
of this program. The remaining `[~]`/`[ ]` evidence is intentionally the live
Fold 8 Ultra permission, posture, input, visual-golden, and end-to-end workout
matrix documented in
`docs/testing.md`; it cannot be truthfully substituted with a host-only test.

Host acceptance on 2026-08-19: all 119 JVM tests passed, all 148 Android
instrumentation tests passed on the physical Fold, lint reported zero findings, and debug,
release, and optimized benchmark APKs built successfully. The final release APK
was signed with the Whip release certificate and independently verified with
Android's `apksigner`.

Physical performance acceptance on 2026-08-19: all nine benchmark/profile
scenarios passed on the SM-F976W Fold 8 Ultra. A Whip-only Baseline Profile was
generated, packaged, and proved a 217.6 ms median cold initial display versus
305.0 ms without compilation. Dense Home (10k tasks/logs), Goal/Gym (100k
history points), active-workout input, primary navigation, and forced expanded
resize all completed without the prior Binder-exhaustion crash. Detailed results
and the one tracked resize-frame outlier are in `docs/performance.md`. The full
updated instrumentation suite then passed 121/121. The matching-certificate
signed production APK was installed as an in-place upgrade, retained the
original package install identity, launched cold in 190 ms, stayed foreground,
and produced no new fatal or Binder-exhaustion log. Physical posture changes and
assistive/peripheral input remain the active hands-on acceptance gate.

### Phase 23 — machine profile lifecycle and immutable history scope `[x]`

- [x] Separate the editable/live machine row from an immutable machine-profile
  UUID snapshot used by workout history, previous-set lookup, graphs, and PRs.
- [x] Add a v21→v22 migration that backfills workout, routine, and PR equipment
  scope, including deterministic identities for unexpected dangling legacy IDs.
- [x] Permanently delete one machine configuration profile without removing or
  reinterpreting completed workouts, sets, canonical values, graphs, or records.
- [x] Clear only live navigation references on deletion, preserve all workout
  calculation/configuration snapshots, and keep deleted machines distinct from
  free weights and every other physical machine.
- [x] Preserve routine prescriptions after profile deletion, mark them `Needs
  equipment`, preflight routine start before creating a session, and restrict
  replacement choices to the same exercise and compatible scale/unit/meaning.
- [x] Define Archive as reversible and hidden from new assignments while keeping
  existing routines usable; provide Snackbar Undo and an archived-equipment note.
- [x] Block permanent deletion during an active workout and re-check a revisioned
  dependency preview inside the deletion transaction to prevent stale confirms.
- [x] Replace crowded machine-card actions with a labelled 48dp overflow and a
  deletion dialog that distinguishes Removed, Kept, Needs attention, active-use
  blockers, sibling versions, historical snapshots, and older-backup privacy.
- [x] Export stable machine scope and configuration metadata to Gym CSV; preserve
  deleted-profile scopes through backup/restore without recreating the profile.

Acceptance evidence:

- [x] Migration, coordinator, repository, backup round-trip, graph partition,
  active-workout blocker, archive/routine, and destructive-dialog tests pass.
- [x] All 107 JVM tests, lint, debug assembly, and all 127 Android instrumentation
  tests pass on the connected SM-F976W Fold 8 Ultra with zero failures.

### Phase 24 — stable identity and historical meaning audit `[x]`

- [x] Give habit checklist items and goal milestones stable draft identities;
  synchronize by identity, delete omitted rows, and preserve completion/link state
  through insertion, reordering, editing, and removal.
- [x] Aggregate habit measurements through canonical values while rendering and
  editing each historical record in the unit actually entered.
- [x] Preserve the entered unit for goal measurements and validate historical gym
  sets against their immutable workout-placement policy rather than a later edit
  to the exercise library.
- [x] Clear a workout's old ending timestamp when resuming it so elapsed time,
  automation, and the next finish share one coherent lifecycle.
- [x] Validate and normalize every habit end type and numeric goal strategy so
  incomplete or non-finite definitions cannot create silent, inert trackers.

Acceptance evidence:

- [x] Repository tests cover insert-at-front, reorder, remove, state/link identity,
  canonical unit conversion, entered-unit editing, snapshot-aware sets, workout
  resume, habit end boundaries, and numeric goal validation.

### Phase 25 — trustworthy reminders and notification actions `[x]`

- [x] Carry a habit reminder's logical schedule date through quiet-hour shifts so
  weekday reminders delivered the following morning are not discarded.
- [x] Include independently moved recurring task occurrences in reminder lookup,
  carry the exact occurrence key in notifications, and route that occurrence.
- [x] Give every delivered Activity intent an event identity so two legitimate
  taps for the same entity both navigate while recomposition cannot replay one.
- [x] Prevent goal reminders shifted beyond their deadline from notifying.
- [x] Cancel rest-timer work and visible notifications on workout finish/discard;
  make the worker verify the matching active session and persisted deadline.
- [x] Make notification action claims retryable after failure while retaining
  successful idempotency; de-duplicate source-stamped additive habit logs.

Acceptance evidence:

- [x] Pure scheduler/worker tests cover logical dates, moved occurrences,
  deadlines, timer state, retries/timeouts, and additive idempotency; device E2E
  covers repeated same-entity deep links and exact occurrence routing.

### Phase 26 — interruption-safe, Fold-aware editing and navigation `[x]`

- [x] Preserve dirty Task, Habit, Goal, Exercise, Machine, and Routine drafts
  through Activity recreation and require explicit discard on editor Back/Cancel.
- [x] Treat an IME-closing Back separately from editor dismissal and keep the
  unsaved-change dialog's own Back behavior unambiguous.
- [x] Keep every task/habit/gym destination composed in a horizontally scrollable
  strip, bring the selected destination into view, and provide trailing clearance
  from the Fold hinge rather than clipping a partially visible control.
- [x] Constrain global dialogs to the active content pane in separating book mode,
  while retaining explicit full-pane operation.
- [x] Keep workout Finish/Discard controls clear of Snackbars and coalesce noisy
  timer feedback instead of building an obstructive message queue.
- [x] Replace internal habit labels with plain language, split Settings into four
  focused areas, pin the Settings heading/section navigation, and distinguish
  saved Health Connect categories from active sync.
- [x] Add edit/delete lifecycle controls for graph presets and accessible value
  summaries for compact charts.

Acceptance evidence:

- [x] Nine-editor recreation/discard suite and the full Fold semantics/navigation
  suite pass on the physical SM-F976W; a discovered off-screen Settings section
  switcher was corrected and re-tested before the final full run.

### Phase 27 — practical gym conversion and scale integrity `[x]`

- [x] Offer explicit Convert, Keep numbers, Reset, and Cancel policies for exercise
  and machine unit changes, with concrete before/after examples.
- [x] Use practical conventional pound defaults instead of exposing raw floating
  conversion noise (for example 2.5 kg defaults become 5 lb and 20 kg becomes
  45 lb), without altering completed-workout snapshots.
- [x] Convert machine load lists, increments, base resistance, add-on resistance,
  and mass mappings atomically; preserve numbered/ordinal stack labels as labels.
- [x] Let a numbered stack's physical mapping be entered in either kg or lb while
  storing a canonical mass for reliable comparisons and links.

Acceptance evidence:

- [x] Conversion policy and repository tests cover conventional defaults,
  dependent fields, numbered stacks, canonical mappings, and historical
  immutability.

### Phase 28 — independent QA/UX audit and release gate `[x]`

- [x] Run independent QA and UX agents through source, automated coverage, and
  read-only signed-release traversal, then reproduce representative flows in the
  disposable debug package on the physical Fold.
- [x] Add a 10,000-recurring-task projection regression test and avoid expanding
  every future occurrence when compact Upcoming mode needs only the next one.
- [x] Separate device commands for development and release packages; verify APK
  SHA-256, installed package/version/update time, and resumed component so an old
  release can no longer masquerade as the newly deployed debug build.
- [x] Make the quality script enforce documented test-count freshness.
- [x] Pass the final host gate: 115 JVM tests, Android-test compilation, lint with
  zero failures, and debug assembly.
- [x] Pass all 142 instrumentation tests on the connected Fold 8 Ultra with zero
  failures (257 product tests total, plus nine performance benchmarks).
- [x] Build, verify, install, and launch the final signed release after the CI
  release/benchmark build completes; then inspect foreground identity and fatal
  logs without clearing the user's release data.

Release evidence on 2026-08-19: the optimized app and benchmark harness built;
the production APK verified under Android Signature Scheme v2 with the Whip
release certificate; local and installed SHA-256 both equal
`6667911f2e673490d528a1783f404de9e14011be3e8dbfc0c30f7e89bad8d408`.
The in-place update retained `com.whip.app`, cold-launched the exact release
activity in 185 ms on the Fold, remained the top resumed activity, rendered the
opaque expanded dark layout with equal At-a-glance cards, and showed no fatal
exception, ANR, native fatal signal, or crash exit in the post-launch audit.

### Phase 29 — scalable routine composer `[x]`

- [x] Separate a routine's selected outline from the full exercise library and
  virtualize search/multi-select so hundreds of exercises remain manageable.
- [x] Preserve the entire dirty draft in a dedicated `SavedStateHandle` model
  through nested editors and Activity recreation; keep duplicate placements as
  independent, ordered items with stable draft keys.
- [x] Create exercises and quick or advanced machine profiles without leaving
  the routine, auto-select successful creations, block save races, disclose that
  library items persist independently, and keep the draft open after save errors.
- [x] Add compact expandable prescription cards with rep ranges, set types,
  load/distance/duration, RPE/RIR, rest, tempo, notes, unilateral state, quick
  schemes, set duplication, and copy-previous behavior.
- [x] Add complete day lifecycle controls: templates, add, rename, reorder,
  duplicate, delete/undo, plus placement drag/keyboard ordering, move, copy,
  duplicate, and removal.
- [x] Replace raw group strings with visual superset/circuit grouping that
  prevents singleton or stale groups.
- [x] Add favorite, recent, category, equipment, and muscle discovery filters;
  import a previous workout and retain its equipment and set prescription.
- [x] Use compact navigation on a Fold pane and inline master/detail in a wide or
  explicitly expanded pane; hide competing navigation while a draft is open and
  constrain nested dialogs to the active pane.
- [x] Add Room v23 and portable-backup support for inclusive repetition-range
  targets without changing completed-set meaning.

Acceptance evidence:

- [x] Unit tests cover recreation, templates, stable duplication/reordering, and
  superset invariants; repository and migration tests cover duplicate exercises,
  range persistence, workout creation, v22→v23, and legacy backups through v23.
- [x] Compose tests cover a 205-exercise library, nested exercise and machine
  creation, independently persisted-library disclosure, and failed-save draft
  retention on the physical SM-F976W Fold 8 Ultra.
- [x] Pass the complete host, instrumentation, CI build, signed-release deploy,
  and physical Fold smoke gates.

Release evidence on 2026-08-19: all 267 product tests passed (119 JVM and
148 instrumentation), lint reported zero failures, and debug, optimized release,
and benchmark APKs built successfully. The signed `com.whip.app` release was
installed in place on the SM-F976W, migrated existing data to Room v23, and
cold-launched in 125 ms with no new fatal, ANR, Room-verification, or SQLite
error. The local and installed APK SHA-256 is
`3ad2358ddd8739041fc46dd73db6554747ae4dd9de9ca0e7037379a710a70c7a`;
Android `apksigner` verifies its v2 signature and Whip release certificate.

### Phase 30 — user-owned rep prescription schemes `[x]`

- [x] Replace built-in routine rep shortcuts with an initially empty, reusable
  scheme library and a prominent `+` action in the placement editor.
- [x] Let users add, apply, rename, edit, and permanently remove schemes with
  set count, inclusive rep range, set type, and optional rest time.
- [x] Persist schemes in app settings and portable backups while keeping
  already-applied routine prescriptions independent from later template edits.
- [x] Cover encoding validation, application semantics, backup round trip, and
  the complete blank/add/apply/edit/delete Compose flow.
- [x] Pass the host, connected-device, CI build, signed-release deployment, and
  physical Fold smoke gates.

Acceptance evidence on 2026-08-19: all 272 product tests passed (121 JVM and
151 instrumentation), including real SharedPreferences recreation, portable
backup restore, prescription application invariants, and six routine-builder
Compose journeys. Lint, debug, optimized release, and benchmark builds passed.
The signed release was installed in place without clearing user data; local and
installed APK SHA-256 both equal
`00f78eee22d132008f97b207aca5f63f28a145f0478b2d745f49313ec38f70b4`.
It cold-launched in 179 ms on the SM-F976W Fold, remained the top resumed
activity, and the post-launch log/exit audit found no new crash or ANR.

### Phase 31 — focus-group trust, execution, and scale remediation `[x]`

Trust and correctness:

- [x] Make the onboarding backup privacy choice truthful: route encrypted
  selection into an encrypted manual-backup setup, never imply that plaintext
  automatic backups are protected, and label every backup artifact by format.
- [x] Make active-workout `Save + next` advance to an existing incomplete
  planned/rotating set before appending, with exactly-once persistence and timer.
- [x] Keep Task, Habit, Goal, Exercise, Machine, link, and automation drafts open
  until asynchronous saves succeed; expose saving/error state and prevent doubles.
- [x] Prefill all task/habit/goal recipes and templates without writing until an
  explicit Save, and make destructive task previews identify and quantify impact.
- [x] Aggregate habit/goal history through canonical units and version custom
  units so later edits never reinterpret historical values.
- [x] Replace partial sequential bulk task mutations with transactional batches,
  explicit occurrence-vs-series previews, per-item failures, and Undo where safe.

Productivity, linking, and scale:

- [x] Make area-level add one-tap and focused while Home retains the global menu;
  add Save-and-new, Enter/multiline paste capture, and Android share intake.
- [x] Make onboarding Skip retain Simple mode and complete low-pressure behavior
  across Home, Today, Insights, actions, notifications, and review.
- [x] Replace silent task/search/history/contribution caps with incremental paging,
  disclosed counts, exact-match ranking, and search across steps/logs/measurements,
  machines, and contributions.
- [x] Add editable/pausable links and automations, backfill preview, friendly
  prompts with Open/Do now, deterministic delayed wake-up, and incremental rebuilds.
- [x] Complete saved views, Plan My Day preview, canonical area/tag management,
  focus timer, keyboard shortcuts, restore compatibility/replace disclosure, and
  generic portable import/merge previews.
- [x] Make secondary navigation, dialogs, editors, contextual panes, and selected
  Gym destination hinge-safe and state-preserving at large text and on Fold.

Gym execution and programming:

- [x] Build a sticky workout execution lane with aligned previous/routine values,
  exact inline machine increments, explicit next target, and configurable source.
- [x] Add safe mid-workout exercise/machine substitutions, reusable alternatives,
  selective editable supersets/circuits, smart rotation, and preserved history.
- [x] Surface immutable machine setup and form cues while lifting; add a truthful
  finish review, post-workout summary, routine-diff review, and detailed history.
- [x] Add searchable exercise/routine/machine/history selectors, record history,
  source-workout/graph drill-down, date axes, and muscle/category target trends.
- [x] Make routine prescription fields match every tracking type and make Simple
  gym mode require only essential inputs while retaining discoverable power fields.
- [x] Add equipment-aware warm-up templates, `%1RM`/training-max prescriptions,
  multi-week waves/deloads/progression rules, compound/reorderable rep schemes,
  reusable equipment templates, intervals, and portable program bundles.

Verification and release:

- [x] Add deterministic repository/UI/worker/migration/performance tests for every
  acceptance item, including 100,000 events, 1,000 visible records, failures,
  process recreation, TalkBack, keyboard, 200–320% text, and Fold transitions.
- [x] Pass host, complete Fold instrumentation, CI release/benchmark, signed
  in-place deployment, artifact identity, launch, and crash/ANR smoke gates.

Acceptance evidence on 2026-08-19: schema 26 adds stable task/subtask UUIDs and
manual task ordering with a verified v25→v26 migration. All 311 product tests
pass (140 JVM and 171 Android instrumentation) on the physical Fold 8 Ultra;
lint, debug, optimized release, and benchmark builds pass. All nine isolated
Macrobenchmark/Baseline Profile scenarios pass, including 10,000 tasks and
habit logs, 100,000 goal/gym points, active-set input and Save + next latency,
startup, navigation, and live resize. The signed release is installed in place
without clearing user data; artifact identity, foreground launch, screenshot,
and crash/ANR audits pass.

### Phase 32 — remove third-party app imports `[x]`

- [x] Remove the FitNotes CSV picker, preview, confirmation, and all other
  third-party app import affordances from Settings.
- [x] Remove the FitNotes parser/repository and application/view-model wiring so
  third-party import is not merely hidden.
- [x] Retain Whip-native plain/encrypted backup restore and merge, portable backup,
  domain CSV exports, and optional Health Connect sync.
- [x] Remove obsolete importer tests, baseline-profile symbols, and user-facing
  documentation; add a device UI regression asserting the importer is absent.

Acceptance evidence on 2026-08-19: all 308 product tests pass (137 JVM and 171
Android instrumentation) on the physical Fold, with lint and debug/release builds
green. The signed release and installed APK hashes match at
`f6c62bc97e4404a022dce87a8f0edddfe6301bd0cff691d4e4a724d6282f6366`;
live Settings traversal and a packaged-artifact string scan find no third-party
import surface or FitNotes implementation.

### Phase 33 — default primary navigation order `[x]`

- [x] Order every primary navigation surface as Home, Tasks, Habits, Goals, Gym.
- [x] Align accessibility traversal and hardware shortcuts so Ctrl+4 opens Goals
  and Ctrl+5 opens Gym.
- [x] Add a Fold regression that asserts the sidebar positions in order.

Acceptance evidence on 2026-08-19: the host build/lint/JVM gate and targeted
physical-Fold navigation test pass. Live signed-release hierarchy confirms
strictly increasing sidebar bounds for Tasks, Habits, Goals, and Gym. The local
and installed APK SHA-256 is
`a477a176f81c13ba8dcb987e9b616ac4ea435daf9613c95c75196b937a30a7d6`.

## Execution protocol

When implementation begins:

1. Start at the earliest incomplete phase unless the user explicitly changes
   priority.
2. Change the active phase/items to `[~]` before making implementation changes.
3. Keep schema migrations additive and test them before UI work relies on them.
4. Check off individual items only after implementation and relevant tests.
5. Record material scope changes directly in this file.
6. At each phase boundary, run all quality gates and summarize remaining known
   limitations.
7. Do not mark a phase complete merely because its UI exists; persistence,
   history, edge cases, and verification are part of completion.
