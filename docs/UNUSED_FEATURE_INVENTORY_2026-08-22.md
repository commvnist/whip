# Whip product-scope reconciliation — 2026-08-22

This is the resolved outcome of a source audit and three independent reviews: a productivity-systems specialist, a five-perspective user focus group, and a product philosopher. The focus group represented a new user, an ADHD/low-pressure user, a power user, a lifter, and a privacy-first local-only user.

Whip's current purpose is a coherent, local-first system for Tasks, Habits, Goals, and Workouts. Areas provide durable context across those domains; Tags provide lightweight cross-cutting metadata; metric entries provide internal evidence. A stored choice must change presentation, evaluation, scheduling, filtering, navigation, export, or another testable behavior.

## Reconciled decisions

| Concept | Decision | Result |
| --- | --- | --- |
| Habit earliest/latest check-in | Remove | Reminders express preferred timing without policing honest late or backfilled logging. The controls, models, columns, backup fields, and tests are gone. |
| Habit Intent / Avoid missing-day policy | Merge and remove | Input method, target comparison, schedule, and reminders already express the behavior. Missing data is always unknown. A numeric limit is Count or Measurement plus **At Most**; a no-spend day is an explicit Check Off. |
| Habit Limit/Avoid mode | Remove | It contradicted itself by presenting a numeric limit while creating a Boolean metric. Existing general modes plus target rules cover the useful behavior. |
| Goal Entry Meaning | Merge and remove | Log guidance is derived from the actual aggregation: Sum adds an amount; the other measurable goal calculations record an observation. A separate label can no longer contradict calculation. |
| Goal milestone pace | Remove | Weighted milestone completion remains first-class, but Whip no longer claims milestone-aware pacing without scheduled milestones. Linear time comparison and no pace judgment remain. |
| Goal advanced semantics | Constrain | Goal type owns compatible aggregation and direction choices. Editors expose only meaningful combinations and explain their consequence. |
| Global Measurement item | Rename and regroup | It was a Goal shortcut, not a creatable object. It is now **Log Goal Value** under Active Goals rather than in the global Add menu. |
| Habit, Goal, exercise-category, and Tag colors | Remove | None had a renderer. Their controls, API parameters, domain properties, columns, backup fields, and tests are gone. Area color remains because it is visible throughout Area navigation and management. |
| Milestone target value and linked task ID | Remove | Neither was edited, assigned, rendered, nor calculated. Existing Link rules remain the explicit relationship model. |
| Goal completion snapshots | Remove | The hidden table was written but never consumed. Completion status and evidence remain in the visible Goal/metric records. |
| Entity–Tag link table | Remove | It had no production reader or writer. Current Tag assignments remain in each first-class item's tag set; Tag taxonomy remains useful for rename/archive management. |
| Task-location tombstones | Remove | The location-reminder feature was already removed. Its inert entity columns and compatibility writes are now gone. |
| Equal-weight subtask tombstones | Remove | Subtasks are always equal-weight, so unused `weight` columns are gone from definitions and snapshots. |
| Historical Room/backup compatibility | Reset | Because Whip is unreleased and a data wipe was authorized, the checked-in database is one clean version-1 schema. Historical migrations, fixtures, compatibility defaults, and old-format backup acceptance are gone. |

## Deliberate disagreement

The productivity specialist proposed expanding Habit Intent into a visible Habit Purpose and adding dated milestones. The focus group and philosopher preferred the smaller ontology: Whip already has tracking rules, and adding another first-class concept would increase setup cost and permit contradictions. The smaller model wins for the current product. Purpose-aware coaching or scheduled milestones may return only as complete, independently tested features—not dormant fields.

Some lifter participants also wanted a neutral body-metrics journal. That is potentially valuable, but it is a separate **Tracker** product concept. Whip will not preserve a misleading Measurement placeholder in anticipation of it.

## Features confirmed useful

- Custom Units change conversion, history, Goal progress, Habit logging, and Links. They remain manageable in Settings and creatable inside compatible editors.
- Exercise categories drive routine/history filtering and analytics. The categories remain; only their unused color was removed.
- Area color is consistently rendered and remains.
- Focus timer, notification channels, low-pressure mode, quiet hours, Health Connect sources, smart capture, recurring-task defaults, Gym field visibility, Gym analytics, and Task planning overlays all have live consumers and remain.
- Machine configuration versions protect the meaning of recorded training data and remain.

## Permanent audit rule

Every visible control and persisted field needs a named downstream consumer plus a regression test. Editor-only persistence is insufficient. If a feature is not complete, omit its control and schema until it is complete.

Before public release, destructive schema resets are acceptable only with explicit owner approval. After release, local user data is part of the product promise: every schema change requires an explicit forward migration and backup compatibility decision.
