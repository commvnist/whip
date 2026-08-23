# Whip live UX evidence — August 22, 2026

This directory is the evidence set for the August 22 UX architecture review. Every image under `live-core/` was captured from the installed release build of Whip on the connected Samsung Galaxy Fold (`SM-F976W`), package `commvne.com.whip.app`, version code 15 / version name 0.3.9.

The resulting implementation plan is [`../../../docs/UX_ARCHITECTURE_IMPLEMENTATION_PLAN_2026-08-22.md`](../../../docs/UX_ARCHITECTURE_IMPLEMENTATION_PLAN_2026-08-22.md).

The accepted post-implementation evidence is indexed in [`post-implementation/README.md`](post-implementation/README.md). It is kept separate from this audit baseline so future work cannot mistake old before-state captures for the current UI.

The intentionally system-bar-inclusive Fold overlay regression evidence is isolated in [`status-bar-regression/README.md`](status-bar-regression/README.md).

The checked-in legacy screenshot collections were not used as evidence. The live device was the source of truth, with the current source code used to explain behavior that is not visible in a still image.

## Capture treatment

- Fresh live capture through ADB on August 22, 2026.
- Open-fold book layout unless the filename says `expanded-content`.
- Cropped from `(0, 113)` through `(2256, 2246)`.
- Final dimensions: `2256 × 2133` pixels.
- The crop removes the Android status/notification bar and Samsung task/navigation bar.
- `14-task-editor-initial.png` was recaptured after QA found a transient personal Messages banner in the original. The original was overwritten; the retained file is notification-free and has no keyboard overlay.
- The phone was not folded, unlocked, or otherwise forced into another posture by automation. Fresh closed-Fold and final book-mode evidence is retained separately under `post-implementation/`.

## Screenshot inventory

### Home and global surfaces

| File | State |
| --- | --- |
| `live-core/01-home.png` | Home, open-fold split view |
| `live-core/02-review-trends.png` | Review & Trends |
| `live-core/42-global-overflow.png` | Global overflow menu |
| `live-core/43-global-search.png` | Search Whip dialog |
| `live-core/44-global-add-menu.png` | Global add menu |
| `live-core/45-home-expanded-content.png` | Home, expanded content |

### Tasks

| File | State |
| --- | --- |
| `live-core/03-tasks-today-list.png` | Today, List |
| `live-core/04-tasks-today-agenda.png` | Today, Agenda |
| `live-core/05-tasks-today-calendar.png` | Today, Calendar |
| `live-core/06-tasks-inbox-calendar.png` | Inbox, Calendar |
| `live-core/07-tasks-upcoming-calendar.png` | Upcoming, Calendar |
| `live-core/08-tasks-anytime-calendar.png` | Anytime, Calendar |
| `live-core/09-tasks-anytime-list.png` | Anytime, List |
| `live-core/10-tasks-inbox-list.png` | Inbox, List |
| `live-core/11-tasks-upcoming-list.png` | Upcoming, List |
| `live-core/12-tasks-filter-sort.png` | Filter & Sort dialog |
| `live-core/13-tasks-list-overflow.png` | Task-list overflow |
| `live-core/14-task-editor-initial.png` | Create Task, initial fields |
| `live-core/15-task-editor-repeat.png` | Create Task, Repeat dependencies visible |
| `live-core/46-tasks-expanded-content.png` | Today, List, expanded content |

### Habits

| File | State |
| --- | --- |
| `live-core/16-habits-today.png` | Today's Habits |
| `live-core/17-habits-all.png` | All Habits |
| `live-core/18-habits-insights.png` | Habit Insights |
| `live-core/19-habits-connections.png` | Habit Connections |
| `live-core/20-habit-editor-initial.png` | Create Habit, initial fields |
| `live-core/21-habit-editor-reminders.png` | Create Habit, reminder controls |

### Goals

| File | State |
| --- | --- |
| `live-core/22-goals-active.png` | Active Goals |
| `live-core/23-goals-insights.png` | Goal Insights |
| `live-core/24-goals-completed.png` | Completed Goals |
| `live-core/25-goals-archived.png` | Archived Goals |
| `live-core/26-goal-editor-initial.png` | Create Goal, initial fields |

### Gym

| File | State |
| --- | --- |
| `live-core/27-gym-workout.png` | Current Workout |
| `live-core/28-gym-history.png` | Workout History |
| `live-core/29-gym-progress.png` | Gym Progress |
| `live-core/30-gym-library.png` | Gym Library landing page |
| `live-core/31-gym-exercise-library.png` | Exercise Library |
| `live-core/32-gym-routines.png` | Routines |
| `live-core/33-gym-machines.png` | Machines |
| `live-core/34-gym-categories.png` | Categories |
| `live-core/35-gym-tools.png` | Gym Tools |

### Settings and Areas

| File | State |
| --- | --- |
| `live-core/36-settings-general.png` | General settings |
| `live-core/37-settings-organization.png` | Organization settings |
| `live-core/38-settings-reminders.png` | Reminder settings |
| `live-core/39-settings-data-backup.png` | Data & Backup settings |
| `live-core/40-area-manager.png` | Areas manager |
| `live-core/41-area-details.png` | Area details/actions |

## Coverage boundary

This is a core-surface inventory, not an exhaustive database-state matrix. Empty, populated, error, loading, destructive-confirmation, keyboard, large-text, RTL, and compact-posture variants belong in the implementation test matrix. Source inspection is permitted for those behavioral states; old screenshots are not.
