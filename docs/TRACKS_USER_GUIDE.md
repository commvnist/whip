# Tracks in Whip

Tracks are reusable structured logs. Use a Track to record evidence; use a
Goal to decide what that evidence means; use a Task or Habit for action.

## Example: count books

1. Choose Add, then Track.
2. Name the Track `Books Read` and rename its first Entry Identity Field to `Title`.
3. Add optional Fields such as `Author`, `Genre`, `Pages`, and `Thoughts`.
4. Add an Entry whenever you finish a book.
5. In the Track's Automations tab, choose **Create a Goal From Entries**.
6. Select Count Entries, name the Goal `Read 50 Books`, and enter `50` as the
   target.

Every eligible Entry contributes one auditable count. Editing or deleting an
Entry reconciles the Goal automatically.

If the Goal already exists, choose **Connect Entries to an Existing Goal**
instead. A normal Reach Goal may initially calculate from its latest value;
Whip explains that Count Entries needs a Total and makes that calculation
change when the Automation is saved.

## Example: filtered numeric progress

Create a `Chess Openings Learned` Track with an `Opening` Entry Identity Field, a
Single Choice `Side` Field, and a 1–5 Scale `Confidence` Field. Set a Goal using
Latest Number or Scale Field, choose Confidence, and add conditions such as
`Side is Black` and `Confidence is at least 4`.

Scale Fields have separate minimum, maximum, and increment settings. For
example, use minimum `1`, maximum `5`, and increment `0.5` for familiar movie
ratings that include `3.5`. Entry controls move by that exact increment, and
CSV, Insights, Goal progress, and Automation constants preserve it.

The preview shows how many Entries were scanned, which were eligible, why any
were skipped, and the resulting value before history is committed. Sum,
Average, Latest, Minimum, Maximum, Count Entries, and a fixed amount per Entry
use the same flow. Conditions define which Entries are eligible, so a separate
“matching count” mode is unnecessary.

## Next-Action Automations

A Prompt-Entry Automation can prompt for a Track Entry after a Task completes, a
specific Subtask completes, or a Habit is recorded, completed, failed, or
skipped. `Recorded` means any saved Habit result; `Completed` means the Habit
reached its target. Prefilled data is always reviewed before Whip saves a
structured Entry.

A Continue-After-Entry Automation starts with a matching Track Entry and can prompt an
existing Task or Habit. It can automatically Check Off a Habit only when that
explicit action is selected and the target Habit uses Check Off.

Notifications are optional. Enabling a notification on an Automation is the
only point where Tracks may request Android notification permission; pending
prompts remain available inside Whip either way.

## History, choices, and portability

When connecting a Track to a Goal, explicitly choose whether to use only new
Entries, Entries since the Goal start, Entries since a date, or all history.
Large work is atomic and cancellable, so cancellation leaves no partial
Automation.

When removing a used Single Choice option, either replace it with a remaining
choice—which also retargets Automation references—or explicitly delete its
saved values. CSV export/import is for interoperability; Whip backup and
restore preserves the complete Track and Automation graph.
