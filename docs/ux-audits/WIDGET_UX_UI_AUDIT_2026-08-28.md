# Android widget UX/UI and correctness audit

> Historical baseline. The combined widget described here was superseded by
> [Task Agenda and Habit Tracking widget review](TWO_WIDGET_UX_UI_REVIEW_2026-08-29.md).

Date: 2026-08-28  
Surface: Android home-screen widget, configuration flow, platform entry actions  
Evidence: source/data-flow trace, JVM policy tests, Android instrumentation, Pixel Launcher API 34 live QA, Samsung One UI host metadata

## Outcome

Whip currently ships one widget type: an area-aware Today summary with Task and
Habit quick-add actions. The implementation was visually serviceable but its
product contract was unclear and its two headline numbers did not mean what the
description implied.

The correctness pass is implemented. The widget now reports top-level Tasks
that are actually due today and Habits that still need attention today. It uses
the same recurrence, completion, flexible-schedule, pause, skip, end-condition,
and health-mirroring rules as the in-app Today surfaces. The current widget is
now a dependable baseline for an overhaul, but it should not be stretched into
every future use case.

The reported “2 open tasks” could not have directly counted a Task subtask or a
Habit checklist item: the old code counted only rows in the top-level Task
table. It did, however, count every open top-level Task—including Inbox and
future Tasks—so a second Task record could appear even when only one item was
visible as due today. A Habit automation that creates a real Task is still a
top-level Task, but it is now counted only while that Task is due.

## Existing widget inventory

There is one provider and one configuration flow:

| Variant | What changes | Current role |
|---|---|---|
| All areas | Aggregates every active Area | Whole-day overview |
| One Area | Counts and quick-adds within one Area | Work, Home, Health, or another focused context |
| Standard 3 × 2 | Two metrics plus Add task and Add habit | Primary portrait presentation |
| Short/landscape | Metrics remain; actions hide when 48dp targets no longer fit | Read-only glance |
| Empty, singular, plural | `0 tasks due`, `1 task due`, `2 tasks due`, and equivalent Habit copy | Honest count states |

The widget does not currently show individual items, support completion from
the launcher, expose Goal/Gym/Track data, or offer a mode choice.

## User use cases

### Jobs the current widget should own

1. **Morning triage:** answer “How much still needs attention today?” without
   opening Whip.
2. **Between-task glance:** distinguish remaining Tasks from remaining Habits.
3. **Scoped work context:** keep separate Work and Home widgets whose counts and
   quick adds remain independent.
4. **Fast capture:** start a Task or Habit in the widget's configured Area.
5. **Completion feedback:** see the number fall promptly after finishing or
   skipping work in the app.

### Distinct jobs that deserve purpose-built widgets

| Proposed widget | Best sizes | Primary job | Priority |
|---|---:|---|---:|
| Today | 3 × 2, 4 × 2, 4 × 3 | Counts plus the next 2–4 due items; complete or open them | P0 |
| Quick Capture | 2 × 1, 3 × 1 | Add Task, Habit, or Track Entry with minimal chrome | P0 |
| Habit Check-in | 2 × 2, 4 × 2 | One selected Habit or the next due Habits; check in directly | P1 |
| Goal Progress | 2 × 1, 3 × 2 | One Goal's current value, target, trend, and next action | P1 |
| Active Workout | 4 × 2, 4 × 3 | Current exercise, set progress, and rest timer | P2 |
| Track Logger | 2 × 1, 3 × 2 | One-tap entry into a selected Track template | P2 |

These should be separate picker entries, not hidden modes inside one generic
“Whip” card. The launcher picker is where users decide what permanent home-
screen job they are adding; separate entries make that choice understandable
before configuration.

## Canonical data contract

| Displayed value | Included | Excluded |
|---|---|---|
| Tasks due | Open one-time Tasks scheduled today or earlier; the one active occurrence selected by each recurring Task's missed-occurrence policy | Inbox/undated Tasks, future Tasks, completed occurrences, skipped occurrences, archived Tasks, Task steps |
| Habits due | Active Habits scheduled today whose current target is not satisfied | Completed targets, satisfied flexible periods, skips, pause windows, ended/archived Habits, health-mirrored targets already met, checklist items as separate work |
| Area label | All areas, Unassigned, the exact Area name, and an archived suffix when applicable | Silent fallback to a different Area |
| Area count | Only records matching the widget's saved scope | Records in other Areas |

The summary is deliberately phrased as **due**, not **open**. “Open” describes a
database lifecycle; “due” describes why an item needs the user's attention on a
Today widget.

## Findings and implementation status

| Severity | Finding | Resolution |
|---|---|---|
| Critical | “Open tasks” counted all unarchived incomplete Task definitions, contradicting the Today description. | Replaced the raw row count with the canonical occurrence-based Today projection. |
| Critical | A Habit stayed “due” after its daily target was completed. | Reused the canonical attention/reminder outcome contract. |
| High | Pause windows and health-mirrored completion were absent from the widget calculation. | Added pause and metric-entry projections to the summary. |
| High | Creating/archiving a Habit, renaming an Area, changing units, or crossing the logical day could leave the widget stale until a periodic update. | Expanded app refresh observation to every input used by the summary, including the logical-date flow. |
| High | Reconfiguring an Area widget started at All areas and could silently erase the existing scope. | Configuration now loads and visibly retains the saved Area. |
| High | A widget quick action launched during first-run setup was consumed behind the modal setup and never resumed. | Platform entry requests remain pending and resume after setup completes. |
| High | The picker showed only the Whip app icon, so users could not preview the widget. | Added a real `previewLayout`; the picker now renders the actual 3 × 2 card. |
| High | There was no host-inflation test for the strict `RemoteViews` class allowlist. | Added an Android host inflation contract test; live QA also validated Pixel Launcher rendering. |
| Medium | `1 open tasks` and raw ISO date copy felt technical and could be grammatically wrong. | Added Android plurals and a clear `Today · Area` subtitle. |
| Medium | The missing/Unassigned Area labels could claim “All areas” or the first active Area while applying a different filter. | Added honest Unassigned, unavailable, and archived labels. |
| Medium | Default themed Buttons were visually host-dependent and made the widget feel detached from Whip. | Replaced them with explicit 48dp, rounded, high-contrast action surfaces. |
| Medium | A short host size could clip actions; using only the option's minimum height also hid them in a tall portrait layout. | Added orientation-aware available-height policy and a summary-only fallback. |
| Low | The description explained contents but not the remaining-attention meaning or scoped quick add. | Rewrote picker description and configuration guidance. |

## Live and automated test matrix

### Calculation states

- Empty state and singular/plural copy.
- Today and overdue one-time Tasks.
- Inbox, future, completed, and archived Task exclusion.
- Completed recurring occurrence exclusion while the Task definition remains
  open.
- Task with a child step contributes one top-level item, not two.
- Area isolation across both Task and Habit metrics.
- Habit completion, skip, pause, archive, flexible target, and health-mirrored
  completion exclusion.

### Platform and lifecycle states

- Pixel Launcher API 34 real 3 × 2 add, host inflation, preview, and rendering.
- Empty → one Task → completed Task live refresh without waiting for the 30-
  minute platform interval.
- Add task and Add habit PendingIntent routing.
- First-run setup followed by automatic resumption of the original widget
  action.
- New configuration, reconfiguration, independent multi-widget scopes, Area
  deletion fallback, and widget-deletion preference cleanup.
- 100% and 130% system font scale; labels and actions remain legible without
  collision.
- Samsung One UI physical-host metadata confirmed the installed release widget
  is a 3 × 2 instance with a 276 × 200dp allocation. No destructive or
  instrumentation work was performed on the physical phone.

## Direction for the overhaul

### 1. Make Today actionable, not denser

Keep the two remaining-attention metrics as the header. At 4 × 2 or taller,
show a small, deterministic list: overdue first, then high-priority Tasks, then
pinned/due Habits. Cap the list rather than shrinking typography. Each row gets
one unambiguous primary action and an open-in-app target.

### 2. Separate capture from review

Ship a dedicated Quick Capture entry. Its job is speed and muscle memory, so it
should fit 2 × 1 and avoid summary data. Let configuration choose Area and the
visible actions. Today can then spend its larger canvas on actual next work.

### 3. Use a launcher-native visual system

Add day/night resources and, on supported Android versions, wallpaper-derived
colors while preserving Whip's identity. Use a quieter wordmark, stronger data
hierarchy, consistent 48dp interaction targets, and a single radius family.
Avoid filling taller sizes with blank space; larger allocations must reveal
more useful content.

### 4. Treat configuration as a previewable contract

Configuration should begin with the widget type and show a live preview, then
Area and item-specific choices. Reconfiguration must always load existing
values. Explain exactly what is counted and what each tap opens. Invalid or
deleted selections should be shown as unavailable rather than silently changed.

### 5. Build one shared projection layer

Today in-app, reminders, notifications, and widgets should consume shared
domain projections for “needs attention.” Widget rendering should receive an
immutable snapshot, not reproduce schedule logic. The current summary already
shares the canonical Task and Habit rules; the overhaul should move that
projection fully out of UI code before list widgets are added.

## Recommended delivery sequence

1. **Foundation (implemented here):** correct counts, refresh sources, plural
   copy, safe host rendering, preview, configuration retention, and platform-
   action lifecycle.
2. **Today v2:** responsive list rows, direct completion/check-in, day/night
   palette, and large-size information gain.
3. **Quick Capture:** separate picker entry with configurable actions and Area.
4. **Habit + Goal:** one-entity interactive widgets backed by shared snapshots.
5. **Live activity widgets:** Active Workout/rest timer and Track Logger after
   update-frequency, battery, and accidental-input policies are defined.

The next implementation target should be **Today v2 plus Quick Capture**. They
cover the two highest-frequency jobs without forcing one widget to compromise
between glanceability and action density.
