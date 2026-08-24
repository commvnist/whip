# Whip Tasks post-fix live UX and design audit

Date: 2026-08-23  
Package: `commvne.com.whip.app`  
Release: version code 15, version 0.3.9  
Evidence: fresh captures from the installed signed release and current source. Checked-in legacy screenshots were not used.

## Outcome

The Tasks feature now follows one coherent model:

`finite action -> placement -> optional constraints -> occurrence outcome`

- Placement is exactly one of Inbox, Anytime, Scheduled Date, or Repeat.
- A Scheduled Date controls when work enters the actionable queue.
- A Deadline is a separate optional final constraint.
- Priority, Duration, and Effort are optional planning evidence. Unknown Effort is not silently treated as Medium.
- Completed and explicitly skipped occurrences are historical facts. A missed-occurrence policy no longer manufactures user history.
- Area and Tags organize Tasks without changing Task state.

The implementation reconciles live QA evidence with independent product-philosophy and interaction-design reviews. The simple path is now short and literal; advanced controls remain discoverable through progressive disclosure, details tabs, filters, selection mode, templates, and power-mode actions.

## Fresh evidence

All retained screenshots exclude the Android status bar and bottom system bar.

- [Unfolded Today](screenshots/unfolded-tasks-today.png)
- [Unfolded Task editor](screenshots/unfolded-task-editor.png)
- [Unfolded Repeat dependencies](screenshots/unfolded-repeat-dependencies.png)
- [Unfolded date picker](screenshots/unfolded-date-picker.png)
- [Unfolded reschedule context](screenshots/unfolded-reschedule-context.png)
- [Unfolded selection actions](screenshots/unfolded-selection-actions.png)
- [Unfolded Task History](screenshots/unfolded-task-history.png)
- [Compact Today](screenshots/compact-tasks-today.png)
- [Compact Anytime](screenshots/compact-tasks-anytime.png)
- [Compact Task editor](screenshots/compact-task-editor.png)
- [Compact Repeat dependencies](screenshots/compact-repeat-dependencies.png)
- [Compact selection actions](screenshots/compact-selection-actions.png)
- [Compact filters](screenshots/compact-task-filters.png)
- [Compact Upcoming views](screenshots/compact-upcoming-views.png)
- [Compact Upcoming calendar](screenshots/compact-upcoming-calendar.png)
- [Compact Task History](screenshots/compact-task-history.png)
- [Compact Task details](screenshots/compact-task-details.png)
- [Compact date picker](screenshots/compact-date-picker.png)

## Final interaction grammar

### Workspace

The Tasks workspace now reads in this order:

1. Active destination: Inbox, Today, Upcoming, or Anytime.
2. Page identity and count.
3. Search, Filter & Sort, and page overflow.
4. A view selector only where multiple views are useful.
5. Active filters, capture, and Task content.

History is a deliberate page-overflow destination rather than a hidden fifth peer tab. Completed and Archived are local History sections, and History has a visible Back to Today action. The app-level ellipsis was replaced with a Settings icon so it no longer competes with the Task page menu.

### Create and edit

Create Task is a full content-pane editor on an unfolded Fold and a full-screen destination on compact screens. Back, title, overflow, and one primary Save action remain fixed while only the body scrolls.

The first viewport contains:

- Task title;
- Use a Template as a secondary action;
- Placement;
- the immediate consequence of that Placement;
- Area and Priority as space permits.

Placement defaults honor the entry point:

| Entry point | Default | Saved result remains visible in |
|---|---|---|
| Inbox | Inbox | Inbox |
| Today | Scheduled Date = today | Today |
| Anytime | Anytime | Anytime |
| Upcoming | Scheduled Date | Upcoming |

Time and reminder controls do not exist for Inbox or Anytime because those states cannot honor them. Scheduled Date and Repeat reveal their dependent controls directly beneath the enabling choice. Area helper text claims inheritance only when the Area actually came from the visible Area scope, not when Whip merely supplied the required default Area.

Save + New is in overflow instead of competing with Save. Recurring edits retain their “this and future” scope through the editor and save action.

### Details and lifecycle

Task details uses Overview, Schedule, and Options sections:

- Overview supports Subtask check-off without closing the Task.
- Schedule explains the current placement or recurrence and owns rescheduling and explicit Skip.
- Options contains pinning, duplication, Inbox/Anytime movement, focus timer, Archive, and separated permanent deletion.

Move Subtask to New Task previews both effects and is undoable. Permanent deletion waits for an exact impact preview and consistently calls child items Subtasks. Opening a date picker preserves the Schedule tab underneath it; Cancel returns to the Task rather than dropping the user at the list.

### Search, filter, views, and selection

Search uses the existing header action and includes Task titles, notes, and Subtasks. Filter & Sort is destination-aware:

- active destinations distinguish Scheduled Date from Deadline;
- Completed uses completion lifecycle data;
- Archived avoids active-work semantics;
- grouped dates and priorities use semantic values rather than formatted-label order.

Upcoming retains List, Agenda, and Calendar. The shared Whip date picker follows the user’s configured first day of week and exposes full-date semantics to accessibility services.

Select Tasks is a mode entered from page overflow. Quick Capture, Plan My Day, and focus surfaces disappear while selection is active. The bulk toolbar uses two rows with two actions each, avoiding hidden horizontal scrolling, clipped actions, and label compression on narrow panes. Destructive bulk archive still presents an exact scope preview.

## Honest automation and data behavior

### Quick Capture

Quick Capture preserves the literal title. Smart date/repeat parsing is explicit and previewable in the detailed editor; capture no longer interprets text without consent.

### Plan My Day

Plan My Day now:

- includes existing Today estimates in capacity;
- exposes unknown estimates as assumptions;
- ranks by explicit planning evidence rather than treating High Effort as importance;
- previews the exact selected Tasks and total capacity;
- treats the user’s selection as authoritative;
- restores schedule and Inbox state through one Undo;
- does not appear when Anytime has no candidates.

### Repeating Tasks

Missed-occurrence policy controls projection without writing synthetic Skipped history. Only an explicit Skip records a skipped occurrence, with a real action timestamp. Schedule-relative and completion-relative recurrence remain distinct and their consequences are explained beside the anchor choice.

## Second live pass findings and corrections

The first post-fix live pass found issues that source-only review did not reveal:

1. Nested dialogs initially centered across the physical hinge. Task confirmations, filters, rescheduling, bulk edit, permanent deletion, and Review filter naming now use pane-aware surfaces.
2. Canceling Choose Date lost the parent Task context. The details state is now retained beneath the date picker.
3. The system Material calendar used Sunday first while Whip was configured for Monday. It was replaced with a Whip-native picker backed by the app setting.
4. Selection leaked normal-mode capture/planning controls. Those controls are now removed during selection and Calendar is normalized back to List.
5. The first responsive bulk toolbar correction technically fit but wrapped labels badly. The final two-by-two toolbar keeps all actions legible without scrolling.
6. Undo snackbars could remain indefinitely and obstruct controls. Task-operation undo now uses a bounded long duration.
7. Empty Anytime exposed a dead Plan My Day action. It now appears only with candidates.
8. Empty History exposed an unhelpful overflow and no visible return. It now has a visible Back to Today action and no empty menu.
9. The last Step/Subtask naming mismatches survived in move/delete confirmations. User-facing language is now consistently Subtask.
10. Area inheritance helper copy could make a false claim in All Areas. The copy now reflects the actual source of the default.
11. The four primary destinations initially depended on horizontal scrolling at compact width, which left Inbox or Anytime partially clipped. Labels up to eight characters now share the available width at normal text scale, while larger labels and accessibility font scales retain intentional scrolling. A 360 dp regression test verifies both edges remain visible.
12. Compact selection left the normal Add Task FAB visible over the lower-right bulk action. Selection state is now reported to the app shell, which removes the global add action on compact and unfolded layouts until selection ends. A navigation-policy regression test covers normal Tasks, Task selection, Settings, and Gym library states.
13. A new Task initially displayed a red title error before the user acted. Save now stays available, the untouched editor remains neutral, and an invalid Save attempt explains the exact requirement without invoking persistence. Android UI coverage verifies both states.

## Simple-user review

- A Task can be captured literally from the list or created with title, visible Placement, and Save.
- Entry-point defaults do not make the saved Task disappear.
- Empty screens contain one concise state rather than repeated zero/absence messages.
- Conditional settings appear where their enabling choice is made.
- Scheduled Date and Deadline no longer compete for the meaning of “due.”
- Canceling a child action returns to the parent context.
- Disabled actions are removed when they have no meaningful input, rather than remaining unexplained.

## Power-user review

- Templates, recurrence, Priority, Duration, Effort, Subtask policies, Notes, Tags, saved filters, grouping, three Upcoming views, bulk edit, bulk movement, archive/restore, permanent deletion, focus timers, and keyboard-safe editing remain available.
- Batch commands are visible in selection mode and secondary commands remain in More.
- Saved filters and Task History are intentional destinations rather than permanent chrome.
- Plan My Day gives an exact preview and reversible mutation instead of a black-box plan.
- Stable semantic distinctions make exported, filtered, grouped, and automated behavior predictable.

## Accessibility review

- Icon-only Task actions have unique spoken labels.
- Conditional-control explanations have accessibility descriptions.
- Calendar navigation and every date expose descriptive semantics and selected state.
- Checkbox, row body, and edit targets remain separate.
- The editor keeps its primary action fixed and body scrollable for keyboard and large-text use.
- Rectangular controls and consistent selected-state colors preserve the app’s visual language.
- Selection actions no longer rely on an undiscoverable horizontal scroll.

## Verification matrix

| Journey | Source/unit coverage | Android UI coverage | Installed release |
|---|---:|---:|---:|
| Inbox/Today/Anytime/Upcoming defaults | Yes | Compiles | Verified unfolded and compact |
| Scheduled Date vs Deadline state matrix | Yes | Compiles | Verified unfolded |
| Literal Quick Capture | Yes | Compiles | Verified unfolded |
| Plan My Day capacity, exact selection, Undo | Yes | Compiles | Verified unfolded |
| No synthetic missed-occurrence history | Yes | Compiles | Source/data verified |
| Repeat dependencies beside Placement | Yes | Compiles | Verified unfolded and compact |
| Anytime has no time/reminder | Yes | Compiles | Verified unfolded and compact |
| Untouched title and invalid Save feedback | Yes | Compiles | Verified compact with IME |
| Integrated Anytime Schedule date picker | Yes | Compiles | Verified unfolded and compact; no crash |
| Monday/Sunday calendar start and selection | Yes | Compiles | Monday verified live |
| Task details context after date Cancel | State verified | Compiles | Verified unfolded and compact |
| Search/filter/sort/group | Yes | Compiles | Verified unfolded and compact |
| Upcoming List/Agenda/Calendar | Yes | Compiles | Verified unfolded and compact |
| Selection and bulk actions | Yes | Compiles | Verified unfolded and compact |
| History/back/empty state | Yes | Compiles | Verified unfolded and compact |
| Four primary destinations at compact width | Yes | Compiles | Verified on closed Fold |
| Permanent-delete impact gate | Yes | Compiles | Verified without deletion |

Android UI tests are compiled locally but are not executed against the user’s personal phone. Live inspection uses safe ADB input only, and QA-prefixed data is removed through the app UI after validation.

## Rules for future Tasks work

1. A creation context is a promise about where the result will appear.
2. A visible control must save a real value; otherwise remove it or explain how to enable it.
3. Dependent controls live directly below the choice that enables them.
4. Scheduled Date and Deadline are never synonyms.
5. Absence of user action is not a historical event.
6. Automation may suggest and preview; it may not fabricate certainty.
7. Multi-effect mutations require an exact preview and one-step Undo.
8. Normal browsing, selection, editing, and destructive confirmation are distinct modes with distinct controls.
9. Every child surface returns to its parent context on Cancel.
10. A responsive fix must be visually inspected at target width; fitting bounds alone is not acceptance.
11. Compact, unfolded, keyboard-open, large-text, TalkBack semantics, empty, populated, and error states are part of the definition of done.
12. Fresh installed-app evidence is required; repository screenshots are never proof of current behavior.
