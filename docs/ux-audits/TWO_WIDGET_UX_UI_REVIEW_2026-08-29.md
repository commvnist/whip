# Task Agenda and Habit Tracking widget review

Date: 2026-08-29  
Scope: Android launcher widgets, configuration, direct completion/check-in, accessibility, and emulator QA

## Brief

Replace the combined Task/Habit count-and-capture widget with exactly two
purpose-built picker entries:

1. **Task Agenda**
2. **Habit Tracking**

Both keep one 48dp `+` action at the top end. Each instance retains its Area and
appearance settings independently.

## Review process

The initial brief was reviewed independently from four perspectives:

- UX and information architecture;
- RemoteViews UI interaction and platform constraints;
- visual/product design; and
- a simulated six-persona focus group covering a busy Task user, Habit-focused
  user, accessibility user, minimalist wallpaper user, novice, and power user.

The focus group was a persona-based critique, not recruited-user research.

## Decisions incorporated

| Question | Decision |
|---|---|
| Picker structure | Two distinct entries; the existing provider becomes Task Agenda so installed widgets upgrade in place. |
| Add affordance | One persistent 48dp `+` at top end; no large bottom Add buttons. |
| Agenda contract | Overdue plus Today/7-day/30-day range, default 7 days; Inbox excluded. |
| Task completion | Direct for simple Tasks. A Task with subtasks expands in place; each child is directly checkable against its exact occurrence, while unfinished parents retain the safe review route. |
| Habit population | Today’s scheduled Habits, remaining first and completed below; completed visibility and an all/custom Habit allowlist are configured per widget. |
| Habit actions | A scrollable collection exposes CheckOff/Checklist controls, numeric increment, Duration Start/Stop, Rate/Log app routing, and Health sync read-only. Checklist children appear only while their parent is expanded. |
| Checklist parent | Read-only progress indicator when children auto-complete the Habit; independently checkable only when auto-complete is disabled. |
| Transparency | 0–80% outer-card transparency with a live configuration preview; opaque protected header/row surfaces preserve contrast. |
| Responsive density | Both widgets use in-widget scrolling collections with 48dp rows and 48dp expand/collapse targets, so expanded children never replace access to later parent rows. |
| Privacy | Widget rows expose title and necessary date/progress only; notes, tags, and history stay inside Whip. |

## Acceptance contract

- Both picker entries have accurate names, descriptions, and previews.
- Area, range, completed-Habit preference, Habit selection, Task/Habit expansion state, and transparency persist on
  reconfiguration and are removed with the widget instance.
- Task actions resolve the current Task occurrence before mutation and tolerate
  stale or duplicate delivery.
- Habit actions validate the current Habit mode, source, item, and rendered
  local date before mutation.
- Task step/state and Habit checklist/state changes trigger immediate widget
  refreshes.
- Every add, row, toggle, and expansion action retains a 48dp target with an
  entity-specific accessibility description.
- Outer transparency never changes foreground alpha; protected surfaces remain
  readable over light, dark, and patterned wallpaper.
- Automated host-inflation tests cover every RemoteViews layout and collection
  factory state. Direct Task/subtask and Habit/checklist completion are exercised
  on a disposable emulator only. Pixel Launcher QA covers both scrollable
  collections, expansion, direct completion, and settings refresh.
