# Whip Tasks live UX, design, and product audit

> Historical record: on August 25, 2026, the redundant Anytime destination and placement were removed. Every undated task now appears in Inbox; references below describe the audited August 23 build.

Date: 2026-08-23  
Installed build: `commvne.com.whip.app`, version code 15, version 0.3.9  
Evidence source: fresh screenshots from the installed release and current source code. No checked-in legacy screenshot was used.

## Scope and safety boundary

The live pass reached the unfolded Fold Tasks workspace and Create Task editor.
The phone then showed an active-call indicator, so device input stopped immediately.
The audit title was left only in an unsaved draft; no test Task or other user data was
committed. Compact-posture and persisted-item journeys remain mandatory follow-up
verification after the call ends.

Retained app-only evidence:

- `01-home-app-only.png`
- `02-tasks-empty-fold-app-only.png`
- `03-create-task-fold-app-only.png`

The raw captures containing system bars were moved to Trash. The retained captures
exclude the status and navigation bars.

## Perspectives

The final recommendations reconcile three independent passes:

- Live QA: actual installed behavior, layout, control availability, and test gaps.
- Senior product/interaction designer: hierarchy, adaptive layout, interaction
  grammar, accessibility, and visual density.
- Product philosopher: whether stored facts, labels, automated consequences, and
  user promises have honest and stable meanings.

## Verdict

Tasks is not release-ready. Its basic domain has good foundations, but the current
UX breaks promises at the exact points where a user establishes trust: where a new
Task will appear, what a date means, whether a reminder is retained, and what an
automation actually did.

The sound foundation to preserve is:

- Inbox means untriaged and undated.
- Anytime means triaged and undated.
- A scheduled date is when Whip presents work for action.
- A Deadline is an optional final constraint, not another name for the scheduled
  date.
- A recurring series is distinct from its occurrences.
- Completed and explicitly skipped occurrences are historical facts.
- Area and Tags organize; they do not change Task state.

The user-facing model should be:

`finite action -> placement -> optional constraint -> occurrence outcome`

Placement is exactly one of Inbox, Anytime, Scheduled Date, or Repeat. Deadline,
Priority, Duration, and Effort are optional metadata. Outcome is Open, Completed,
or explicitly Skipped.

## Confirmed live defects

### P0. Create Task is structurally broken with Fold and the keyboard

The fresh unfolded screenshot shows a floating modal inside the right pane while
the split keyboard is open. Only the title and part of Area fit. The Area selector
is cut in half and looks empty. Schedule and nearly every meaningful Task setting
are below an internal scroll, while Cancel, Save + New, and Save compete above the
keyboard.

This is caused by a full-window `Dialog`, an offset inset surface, IME padding, a
`weight(fill = false)` scroll body, and a three-action footer. It technically
avoids the hinge but fails as an editor.

Required design:

- Compact: full-screen editor destination.
- Unfolded Book Fold: fill the right content pane; keep the left context pane if it
  remains useful.
- Sticky top app bar: Back/Cancel, Create Task or Edit Task, one filled Save action.
- Save + New moves to overflow or power mode.
- Only the editor body scrolls.
- The focused control is always fully visible above the IME with at least 12 dp of
  clearance.

### P0. Creation context does not reliably determine placement

Opening Create Task from Anytime passes no scheduled date. The editor then defaults
every undated new Task to `inbox = true`. Saving from Anytime therefore creates an
Inbox Task that disappears from the page where creation began.

Context defaults must be explicit and tested:

| Entry point | Initial placement | Result remains visible |
|---|---|---|
| Inbox | Inbox | Inbox |
| Today | Scheduled Date = today | Today |
| Anytime | Anytime, not Inbox | Anytime |
| Upcoming | Scheduled Date, prominently chosen | Upcoming |
| Neutral/global add | Placement shown before Save | Matching destination |

The selected placement must be in the first viewport while the keyboard is open.

### P0. Anytime exposes time and reminder controls whose values are discarded

The editor shows Set a Time, Notification, and reminder offsets for Anytime. The
database mapper discards time and disables reminder when schedule kind is Anytime,
and reminder generation returns no candidates for Anytime.

A visible control may not save to nothing. Hide those controls for Inbox/Anytime
and show a short consequence: `Schedule this Task to add a time or reminder.` When
changing a scheduled Task to Inbox/Anytime, preview every date/time/reminder value
that will be removed.

### P0. Scheduled date is repeatedly mislabeled as a due date

The editor distinguishes Work Date and Deadline, but downstream surfaces say Tasks
Due, Nothing Due Today, Due Today, Change Due Date, and `Due <work date>`. The model
also marks a past scheduled date as overdue when there is no Deadline.

Use one terminology contract everywhere:

- `Scheduled Date`: when work enters the actionable queue.
- `Deadline`: final cutoff.
- `Past Scheduled Date`: the plan date passed; this is not Deadline Overdue.
- `Deadline Overdue`: today is after the explicit Deadline.
- `Today`: needs attention now, including deliberate carryover.

A Task scheduled August 20 with Deadline August 30, viewed August 23, must display
`Past Scheduled Date · Deadline Aug 30`, never `Overdue`.

### P0. Quick Capture silently interprets titles

Quick Capture always runs the natural-language parser, although the full editor
presents parsing as an explicit Apply action and the setting may be disabled. A
title such as `Buy tomorrow's groceries` can therefore be rewritten and scheduled
without explicit consent.

Quick Capture must be literal. Optional parsing needs a visible preview and Apply
action before the title or placement changes.

### P0. Plan My Day makes an unsupported promise

The current planner treats missing duration as exactly 30 minutes, ranks higher
Effort ahead of lower Effort, ignores work already in Today, moves selected Tasks
to Today, clears Inbox state, and reports that the result fits capacity. It provides
no complete undo for the combined mutation.

Effort is cost, not importance; missing duration is uncertainty, not evidence.

Required behavior:

- Capacity is either total daily capacity including existing Today work, or clearly
  labeled additional capacity.
- Unknown estimates remain visibly assumed and user-reviewable.
- Ranking uses explicit Priority/Deadline, never high Effort as importance.
- Preview states exact consequences, such as `Move 3 Tasks to Today; mark 2 Inbox
  Tasks triaged`.
- User selection is authoritative.
- One Undo restores both schedule and Inbox state.
- The final message reports facts and assumptions rather than claiming certainty.

### P0. Auto-skip manufactures history

The Auto-skip missed-occurrence policy writes a Skipped occurrence for every elapsed
slot, indistinguishable from an explicit user skip. Absence of evidence is not a
user action.

Missed-occurrence policy should normally control projection only. If generated
closures must be persisted, store a distinct cause and display it as system-resolved,
not user-skipped. Completion-relative cadence must use an actual closure timestamp
or use honest closure-based language.

## Major workspace and editor improvements

### P1. Replace Schedule + Inbox with one Placement control

The editor currently asks for Schedule = Anytime and then separately asks whether
the Task is Inbox. That exposes a storage detail.

Use one control:

`Inbox | Anytime | Scheduled Date | Repeat`

Revealed controls remain directly underneath the choice that enabled them:

- Scheduled Date: date, time/reminder, optional Deadline.
- Repeat: start date, cadence/days, anchor/missed behavior, end, time/reminder.

### P1. Rebuild the editor around causal groups

1. Essentials: Title, Placement, Area, Priority.
2. Schedule consequence: date/repeat settings, time/reminder, Deadline.
3. Planning: Duration and Effort.
4. Steps: Subtasks, progress, parent completion; repeating-step policy appears
   within this group under `For Repeating Tasks`.
5. More: Notes and Tags.

Rename Plain-Language Recipe to Use a Template and move it to overflow/secondary
placement. Remove permanent teaching copy where labels and feedback suffice.

Save must never become disabled without a local, accessible explanation beside the
invalid field.

### P1. Simplify the empty Tasks workspace hierarchy

The live Today page says the same thing repeatedly: support-pane zero, page zero,
Quick Capture instruction, Today Is Clear, and Nothing Due Today. It also places a
Filter icon by itself on an otherwise empty row.

Use this hierarchy:

1. Task destinations.
2. Page identity and Search/Filter/page overflow in one header.
3. View switcher only when a destination actually has multiple views.
4. Active filter chips only when filters exist.
5. Quick Capture and content/one empty state.

Keep one absence message and one dominant capture mechanism. Move undo/edit
instructions into the post-save snackbar.

### P1. Make History intentional instead of horizontally hidden

Five peer destinations exceed the pane width; the live Fold shows Inbox, Today,
Upcoming, and Anytime while History is completely hidden with no useful affordance.

Keep the four active destinations as peer tabs. Put History in the page overflow
as a labeled row with an icon, then show Completed/Archived inside the History page
with an explicit back context. This is preferable to a permanently scrolling tab
bar for a low-frequency lifecycle destination.

### P1. Remove stacked ellipsis ambiguity

The app-shell ellipsis and Tasks ellipsis have the same visual weight and appear
near each other. Use explicit global Settings/Review icons where possible. Reserve
the page ellipsis for Task operations such as History and Select Tasks.

### P1. Clarify detailed creation versus Quick Capture

- Global/page `+`: detailed Task editor.
- Inline control: label `Quick Capture`.
- Enter/trailing arrow: literal save now, then Edit and Undo snackbar actions.
- A small Add Details action carries the current text into the editor without
  saving.

### P1. Keep Task details open during subtask work

Checking a subtask currently closes the Task details dialog, forcing repeated
re-entry. Update in place and close only when final-subtask automation completes
the parent.

Rename Promote to `Move to New Task`, preview that it creates an Inbox Task and
removes the Subtask, and provide Undo.

### P1. Preserve recurring edit scope throughout the flow

An initiating action may say Edit This and Future, but the editor falls back to
generic Edit Task and Save. Keep scope visible:

- `Editing this and future occurrences from Aug 23.`
- `Earlier history remains unchanged.`
- Primary action: `Save This and Future`.

### P1. Make filters destination-aware

Active views filter scheduled dates and Deadlines separately. Completed filters and
groups by completion date. Archived uses lifecycle fields only when meaningful.
Dates and priorities sort by underlying values, not formatted labels.

Clear All must reset every filter, including hidden text-query state.

## Secondary refinements

- Effort needs an Unspecified state; silently defaulting every Task to Medium invents
  planning evidence. Rename the stored `Deep` enum to `High` while breaking changes
  remain acceptable.
- Show non-default Effort independently of Duration; current cards show Effort only
  when Duration exists.
- Selection uses a contextual page header and compact sticky bottom action bar,
  not a wrapping card that can consume several content rows.
- Hide Select Tasks when the current source is empty.
- Normal row grammar remains checkbox = Complete, row body = Details, trailing
  pencil = Edit. All targets are separate, at least 48 dp, and uniquely labeled.
- Move destructive actions to a clearly separated final section.
- Rename the template `Break a project into steps` to `Break complex work into
  subtasks`; do not imply a hidden Project ontology.
- The grey handle on the far right of the live screenshot is Samsung Edge Panel
  system chrome, not a Whip control.

## Verification gaps

The existing Android test named `anytimeTaskScheduleSectionCanAssignItsFirstDateWithoutCrashing`
mounts TaskActionsDialog alone and verifies only that a callback fires. It never
opens the integrated WhipApp date picker, persists the result, or verifies the Task
moves from Anytime to the expected destination.

The adaptive 200% text test also contains stale labels and invocation paths. Its
source compiles, but it has not validated this release on a device.

Required live and automated journeys:

- Create/edit Inbox, Today, Anytime, and Upcoming; result remains visible.
- Schedule an Anytime Task through integrated details and persist it without crash.
- One-time Task with Scheduled Date, time/reminder, and Deadline.
- Repeat on weekdays, interval cadence, anchor, end, and missed policy.
- Add/reorder/delete Subtasks; complete several without reopening details.
- Complete, skip, reopen, archive, restore, and permanently delete with correct
  series scope.
- Literal Quick Capture and explicit parser preview/apply.
- Filter/apply/clear/save; Calendar and Agenda; selection/bulk actions.
- Plan My Day preview, assumptions, existing load, combined Undo.
- Process recreation, keyboard open, rotate, fold/unfold, and background/restore
  without draft loss.

## Adaptive and accessibility acceptance matrix

Test closed Fold portrait and landscape, unfolded Book Fold, tabletop, keyboard
open and closed, and font scales 1.0, 1.3, and 2.0.

Acceptance criteria:

- No control crosses the hinge, overlaps IME/footer, clips, or masquerades as an
  empty selector.
- Title, Placement, and primary Save are reachable with the keyboard open.
- Common Scheduled Task creation requires at most title, placement/date, and Save.
- Anytime requires title and Save after entering from Anytime.
- Time/reminder never appears for Inbox/Anytime.
- One filled primary action exists per surface.
- Icon actions have unique spoken labels; conditional sections announce appearance;
  errors are read; touch targets are at least 48 dp.
- Body-text contrast reaches 4.5:1 and large/control graphics reach 3:1.
- An Aug 20 scheduled date with Aug 30 Deadline is never labeled Overdue on Aug 23.
- No policy-generated missed occurrence is labeled as a user Skip.
- Fresh visual regression captures cover empty and populated destinations, all
  Upcoming views, History, editor placement states, details, filters, and selection
  in compact and unfolded layouts.

## Recommended implementation order

1. Fix semantics and data contracts: Placement defaults, scheduled-date/deadline
   model, Anytime time/reminder, literal Quick Capture, Plan My Day, and generated
   skip provenance.
2. Replace the modal editor with pane/full-screen navigation and causal grouping.
3. Simplify workspace hierarchy: header actions, Filter, History, ellipses, Quick
   Capture, and empty state.
4. Repair details, Subtask interaction, recurring scope, filters, selection, and
   card metadata.
5. Add integrated tests and execute the full live device/posture/accessibility
   matrix before another release build.
