# Whip user guide

## Five core sections

- **Tasks** are finite work. Unscheduled tasks live in **Inbox**; tasks can also
  be one-shot, daily, every N
  days/weeks/months/years, or scheduled on selected weekdays. Recurrence can
  stay anchored to calendar slots or start the next interval when the current
  occurrence is completed. Recurring occurrences can be moved
  or skipped independently. Subtasks can reset or carry unfinished steps, use
  equal-weight progress, and show percentage/fraction progress. Upcoming shows
  only the next occurrence of each repeating task by default; Settings can
  expand every occurrence in the next 30 days.
- **Habits** are repeatable behavior. A habit can be a check-off, count,
  decimal quantity, timer-backed duration, reusable checklist, rating,
  limit/avoid tracker, or unscored log. Schedule and target period are separate,
  so “three times each week” and “eight glasses each day” are ordinary setups.
  Numeric quick buttons accept either exact values (`1,2.5,8`) or a compact
  range (`1-10`) expanded by the Quick increment.
  A scheduled daily occurrence can be **Skipped Today** before it has a
  check-in. The card immediately shows **Skipped Today · Streak Protected**;
  History and Insights retain the skipped day, today's reminders stop, and the
  day neither completes nor breaks the streak. Use **Undo Skip** to restore it.
  Unlogged past scheduled days become missed automatically—there is no separate
  “mark missing” or “excuse” action. Flexible weekly/monthly habits have no
  daily skip because their obligation belongs to the whole period.
- **Goals** are long-term outcomes. They support latest-value, reduction,
  accumulation, range, average, consistency, weighted milestone, and open-ended
  trend modes. Measurements can be backdated and edited.
- **Tracks** are structured logs with user-defined fields. They preserve the
  evidence behind an outcome—such as books read, chess openings studied, or
  symptoms observed—and can automate progress into Goals or connect entries to
  Tasks and Habits. Scale Fields use a user-defined increment, so a 1–5 movie
  rating can allow half points such as 3.5 instead of forcing whole numbers.
  Entry sorting always separates the chosen **Sort By** field from its
  **Ascending / Descending** order; Tasks and the Exercise Library use the same
  control pattern.
- **Gym** stores user-created exercises and set-level workouts. The exercise
  library intentionally starts empty. Tracking types include weights/reps,
  bodyweight and assisted movements, durations, and distances.

Home keeps these sections visibly separate while supporting quick logging. Use
**Settings → Appearance & Home → Home Overview** to reorder, collapse, or hide
Home sections. Pin important tasks, Habits, Goals, Tracks, and routines to
surface them first.

The first-run setup chooses the Home sections you want emphasized, simple or
power defaults, kg or lb, notification intent, and low-pressure display. **Use
Defaults** starts with Tasks and Habits on Home, simple presentation, kg, and no
notification request. Backups stay out of onboarding and can be configured when
needed in **Settings → Data & Privacy**. Hiding a section removes its Home
summary and empty-day shortcut only: its data and peer destination remain
available in main navigation and global add/search.
Low-pressure mode removes streak pressure from Habit cards and Insights while
keeping the underlying history intact.
On Home and the Habits **Today** page, habits that still need attention remain
first. A completed occurrence moves to the subdued, collapsible **Done** section
for confirmation or undo, then returns as pending on its next scheduled day.

Task, habit, and goal editors open in a shorter basic mode. **Advanced Options**
expands to reveal notes, metadata, units, aggregation details, and other power
settings without discarding hidden values. Choices that reveal more settings
keep those settings immediately below the choice and explain any non-obvious
effect; unavailable controls state how to unlock them. Goal templates provide editable
starting points for weight, savings, distance, reading, consistency, and
weighted projects. Global search opens or focuses the exact matching record,
including archived records and discarded workouts.

Advanced task setup adds priority, comma-separated tags, a
work date distinct from its final deadline, and multiple notification offsets.
Areas group tasks, habits, and goals under any names you choose. Whip creates
**Main** automatically, assigns new items to an Area, and always
keeps at least one active Area. **Edit Areas** can move every item from one Area
to another in one action while retaining both the records and their history.
Before deleting the only active Area, create its replacement first.
Tasks, Habits, Goals, and Tracks share the same 100 common identity emojis. Those
defaults are always available and read-only. Choose **Add Custom Emoji** in an
emoji picker to save a reusable named choice, or open **Settings → Organization
→ Custom Emojis** to add, rename, replace, or remove your own choices. Custom
names are searchable in every picker. Removing a choice from **My Emojis** does
not alter items that already use its glyph.
The Tasks screen supports named filters,
multi-select complete/archive/restore/pin actions, an agenda, and a navigable
month calendar. A saved task filter can also constrain the Tasks section on
Home. The calendar action in the top bar opens the Upcoming agenda, while
**Review & Trends** is available as a named action on Home. Review supports
named combinations of Tasks, Habits, Goals, and Gym; Tracks remains neutral
evidence rather than a productivity score. Summary counts on Home, the Fold
pane, and Review open the owning area.

The Whip mark opens Home, and Tasks, Habits, Goals, Tracks, and Gym are five
direct peer modules. Each workspace keeps its highest-frequency pages visible;
secondary pages use the labeled **Pages** menu instead of hidden horizontal
scrolling. In Tasks, List/Agenda/Calendar is a separate persistent view control.
Search is global and starts scoped to the current module; Filters and selection
remain contextual controls with distinct roles;
habit overlays only apply to Agenda and Calendar. Gym's Library is a child-page
landing screen rather than a show-more control. On wide screens Settings uses a
persistent category list, and create moves to the top app bar so it does not
cover content.

Quick captures can enter a real **Inbox** for later triage. **Plan My Day**
selects work within a user-entered time capacity using duration, urgency, and
effort; missing estimates count as 30 minutes. Tasks can be duplicated back to
Inbox, gently deferred, bulk-postponed, and filtered by Area, tags, dates,
status, deadline, Inbox, effort, and duration. A local focus timer can be
attached to the current task. Completion date, rather than the original due
date, places finished work on the completed calendar. **Select Tasks** opens a
contextual action surface: active tasks can be completed, archived, edited,
pinned, or rescheduled; completed tasks can be reopened; archived tasks can be
restored. Permanent bulk deletion first shows the exact affected history,
subtasks, goal sources, and automations and asks for confirmation.

The task editor labels planning intensity as **Effort**, with the parallel
choices **Light**, **Medium**, and **High**.

The Home add menu creates tasks for today so they remain visible after saving.
Capture from the Inbox tab, the share target, or the widget when the task should
wait for later triage.

Habit number cards expose increment, decrement, Set, configured quick values,
and Undo directly. A value such as 6 of 8 remains partial until the period total
reaches 8; Review, reminder suppression, linked Goals, and streaks use that
single period outcome rather than counting six entries as six successes.

Smart task capture is disabled by default. When enabled in **Settings →
Planning & Units → Task Defaults**, **Apply
smart date and repeat** recognizes explicit local phrases such as `tomorrow`,
`next Friday`, `every 2 months`, `on 2026-09-01`, and `deadline 2026-09-05`.
The parser is deterministic, only runs when tapped, and never sends title text
off the device.

## Workout calculations

Eligible set volume is effective load in kilograms multiplied by repetitions.
Warm-up and incomplete sets are excluded by default. Assisted-bodyweight load
is clamped at zero. Exercise settings control bodyweight semantics and whether
a movement participates in volume or personal records.

Estimated 1RM uses Epley by default:

```text
weight × (1 + repetitions / 30)
```

Brzycki is also available:

```text
weight × 36 / (37 - repetitions)
```

The configurable repetition cutoff defaults to 10 because estimates become
less dependable at high repetitions. Graph points retain a route back to their
source workout. Editing historical sets rebuilds records and derived views.
Every graph also exposes a textual point count, range, change, screen-reader
summary, labeled high/low bounds, and an optional full data table. Tapping a
data row opens its exact value and source-workout details.
Workout history provides both a chronological list and a navigable month
calendar with per-day workout counts and filters.
During a workout, Whip marks the next incomplete set and offers an explicit
**Jump to Next Set** control. **Settings → Planning & Units → Gym Defaults**
can use denser set rows. Workout Tools
can save named bar/plate inventories; those presets are reusable in both the
plate calculator and exercise setup. Every day in a pinned multi-day routine
is directly startable from Home.

The workout execution lane shows the current rest duration. Choose **Adjust**
to use a preset, enter seconds directly, or step by 15 seconds, then choose
**Use for This Workout**. This overrides automatic and manually started rest
timers for the current workout without changing the default in **Settings →
Planning & Units → Gym Defaults**. While
a timer is running, **−15** and **+15** adjust that timer immediately. The
initial presets are 1:00, 1:30, 2:00, 2:30, 3:00, and 5:00. Choose **Manage
Presets** in the same dialog to add or remove persistent shortcuts, or restore
that default set.

Gym keeps Workout, History, and Progress in the primary destination row. Open
**Library** for Routines, Exercises, Machines, Categories, and Tools. In an
active workout, one focused composer owns the next incomplete set. Enter its
required values and choose **Complete Set**. Completed and future sets stay as
compact rows; structural movement remains available from overflow menus rather
than permanent drag handles during workout execution.

Each exercise declares what an entered load means: total system, per hand, per
side, added external load, bodyweight plus load, bodyweight percentage,
assistance subtracted from bodyweight, machine-displayed mass, or an ordinal
machine setting. Per-hand/per-side multipliers and unilateral work are applied
before volume/e1RM. Ordinal settings are never presented as kilograms. Whip
snapshots load meaning, unit, calculation policy, and equipment version into
performed history, so editing an exercise later cannot silently reinterpret an
old set. Excluded or incompatible data is explained in Progress.

Active workouts use inline validated entry with **Use Previous**, deterministic
equipment increments, one explicit **Complete Set** action, and immediate
persistence. Routine prescriptions remain separate from performed values: an
actual set may differ in weight, reps, RPE, RIR, duration, or machine setting
without changing the reusable routine.

### Machines and resistance scales

Open **Gym > Machines** to create a profile for each physical machine and
exercise combination. Use **Weight Stack / Mass** when its markings are actual
kg or lb, or **Numbered Stack / Level** for scales such as pin 1–10. Enter a
compact range such as `50-500` or `1-10` and its increment, or enter an
irregular list such as `1,2,4,7,10`. The resulting exact values drive the −/+
controls and optional one-tap choices during set entry. New machine profiles
inherit their exercise unit. Versioned configuration also stores gym/location,
seat and back positions, attachment, pulley ratio, single/dual/independent
stacks, add-on plates, stack labels, mass mappings, and setup notes that make
the resistance unique. A replacement or recalibrated machine can become a new
version in the same comparison group. Compatible versions are included only
when the user explicitly requests them.

Each exercise owns its entry unit and increment. Switching an exercise from kg
to lb applies real pound-equipment defaults—a 45 lb bar, 5 lb entry increment,
and 45/35/25/10/5/2.5 lb plates—instead of displaying converted metric
decimals. These are editable starting points for specialty bars, microplates,
or nonstandard equipment. Switching units does not rewrite workout history.

Choose the machine when adding an exercise to a workout. Whip then compares
previous sets, records, and charts only with that same profile. Even mass labels
remain separated between machines because pulley ratios and lever arms differ.
Numbered settings are graphed as settings and are deliberately excluded from
kg/lb volume and estimated-1RM calculations. To change machines after sets have
been entered, add the exercise again; this prevents old values being silently
reinterpreted. Routines remember the selected machine.

## Units, dates, and time zones

Built-in units cover counts, durations, distance, volume, mass, length, money,
energy, temperature, speed, pace, frequency, percentages, and unitless values.
Common choices include days and weeks; metric and imperial mass and length;
Celsius, Fahrenheit, and Kelvin; km/h, mph, and m/s; min/km and min/mi; and
per-minute rates. Create a custom unit directly from a
compatible Habit or Goal unit chooser, or manage all custom units under
**Settings → Planning & Units → Unit Defaults**. A custom unit has a symbol,
dimension, and canonical conversion factor. For example, a volume unit named
`glass` can use factor `250`, meaning one glass is stored as 250 millilitres.
Custom units also cover mass and every other listed dimension: a mass unit
named `stone` uses factor `6.35029318`, meaning one stone is stored as
6.35029318 kilograms. The editor states the canonical base for the selected
dimension directly rather than assuming millilitres.
The mass, distance, and volume defaults select units for compatible new
records. The gym unit selects aggregate summaries, tools, and new exercises.
An existing exercise or machine keeps its own chosen equipment unit; its set
entry, single-exercise progress, records, routine values, and sharing use that
unit. Cross-exercise summaries and comparison charts use the global gym unit
so they retain a coherent common scale. Values remain stored canonically, so
none of these display choices rewrite history.

Whip follows the device time zone unless **Settings → Planning & Units → Date
and Number Defaults** pins an IANA region such as
`America/Toronto`. Existing entries keep the date, zone, and offset recorded at
the time; the selected zone controls new entries, reminders, imports, and what
counts as today. The late-night cutoff can treat early-morning entries as part
of the prior day.

## Links and automation

A contribution link maps a stable task, habit, workout, exercise, or metric
event into a goal. The goal history explains each source. The same source event
cannot contribute twice through the same rule, and editing or deleting a
source rebuilds its contribution. Historical backfill always has a preview.
Context links associate records without adding progress. Trigger rules reveal a
next habit/task after an outcome and support delays, quiet hours, cycle checks,
and rate limiting.

## Notification delivery

**Settings → Reminders** shows whether Android notification permission, each Task, Habit,
Goal, and rest-timer channel, and battery optimization may affect delivery. It
links directly to
Android's notification and battery screens and can send a real test
notification. Battery policies vary by device, so a successful test confirms
the current notification path but does not guarantee that Android will never
delay future background work.

Task reminders can select several offsets, including custom minutes before the
task time.

Explicitly adding a reminder, enabling automatic rest-timer alerts in
**Settings → Planning & Units → Gym Defaults**,
or manually starting a rest timer requests notification permission in context.
Saving an item or completing a set does not open a permission prompt.
Notifications deep-link to the exact record and support applicable
complete, snooze, and numeric-increment actions. Action IDs are recorded so an
Android retry, reboot, or time-zone change cannot apply the same mutation
twice. Habit reminders stop once the current daily, weekly, or monthly target
is satisfied.

## Backup, restore, and CSV

All live records are stored in the app's on-device Room database and remain
available offline and after app/device restarts. **Settings → Data & Privacy**
offers two complete
backup paths:

- **Portable Backup Folder** asks Android's folder picker for a location in
  Files, a cloud-backed document provider, or removable storage. **Back up
  now** writes an `INCOMPLETE` staging document, closes and reads it back,
  validates its checksum and record count, atomically commits the visible
  timestamped `.whip.json`, and verifies it again. Automatic backup creates one
  battery-aware daily WorkManager job. Retention is configurable from 1 to 30
  verified Whip backups; unrelated files are never pruned. Automatic backup
  skips an empty database so deleting local data cannot gradually rotate away
  the last useful archives.
- **Save Plain JSON Backup** and **Save Passphrase-Encrypted Backup** create a
  complete archive without remembering a folder. The encrypted option uses an
  authenticated Whip envelope; its passphrase is required for restore and is
  never retained by the app.

The selected-folder permission survives app and device restarts. If the folder
is moved, deleted, disconnected, or its provider revokes access, **Settings →
Data & Privacy → Backup & Export** shows
the last error and the user can choose it again. **Forget Folder** revokes
Whip's remembered access and cancels its periodic job; it does not delete files
already written there.

To move to another device, install the same or a compatible Whip build, copy or
sync a `.whip.json` to a location visible in that device's Files picker, then
use **Preview and Restore Backup**. Keep plaintext backups private. Restore
validates the envelope, authentication/checksum, database version, row counts,
preferences, and duplicates before asking for confirmation. Whip then stores a
private recovery snapshot while it replaces local data and preferences and
rebuilds derived data, reminders, and scheduled work. Failure rolls
back immediately; process interruption recovers on next launch.

Tasks, Habits, Goals, Tracks, and Gym data also have CSV exports. CSV is intended for
analysis and interoperability; only the complete JSON envelope is a full-fidelity
backup.

## Data lifecycle

Archive keeps history and permits restore. This applies to tasks, habits,
goals, exercises, routines, and discarded workouts. Set removal during an
active workout is soft-deleted and undoable. Each first-class record also has
an advanced **Delete Permanently** action with a confirmation that previews
the affected history, measurements, routine/workout references, links, and
automations. Cross-feature cleanup is transactional; workout-derived goal
values and records are recalculated. Deleting a routine preserves completed
workouts created from it, while deleting an exercise explicitly removes that
exercise's sets from workout history and routine templates.

Recurring task history combines completed, skipped, and moved occurrences.
Completed occurrences can be reopened, skips undone, and moved dates reset;
the series cadence explains how the next scheduled slot is chosen.
**Reset Whip and Delete All Data** is the broad destructive action and
explicitly suggests creating a backup first. It clears the internal database
and app settings, disconnects the remembered portable backup folder, and
returns to setup, but does not delete backup files stored through Android's
document picker.
