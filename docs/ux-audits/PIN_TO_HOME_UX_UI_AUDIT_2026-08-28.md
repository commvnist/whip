# Pin to Whip Home UX/UI audit

Date: 2026-08-28  
Scope: Tasks, Habits, Goals, Tracks, Gym routines, Home Overview settings, compact and expanded Home layouts, action feedback, and accessibility semantics.

## Product contract

“Pin” targets **Whip Home inside the app**. It never creates an Android launcher shortcut, changes scheduling, changes priority, or enables reminders.

| Item | Whip Home result |
|---|---|
| Task | Appears first in Tasks when it is due today. |
| Habit | Appears first in Habits when it is due. |
| Goal | Remains in the compact active-Goal summary; all pinned active Goals are retained even when there are more than three. |
| Track | Appears as a Quick Log Entry shortcut. |
| Routine | Appears as a start shortcut when no workout is active. |

Pinning reveals and expands the owning Home section. Unpinning never deletes, archives, reschedules, or otherwise changes the item.

## Findings and resolutions

| ID | Severity | Finding | Resolution |
|---|---:|---|---|
| PIN-01 | High | “Home” could mean the Android launcher rather than Whip’s Home destination. | Standardized action labels on “Pin to Whip Home” / “Unpin from Whip Home”; documented the distinction. |
| PIN-02 | High | One generic label hid materially different domain outcomes. | Added exact, per-type supporting copy at the action and a contract in the user guide. |
| PIN-03 | High | A pin could appear to do nothing when its Home section was hidden or collapsed. | Every pin operation now reveals and expands its owning Home section while preserving section order. |
| PIN-04 | Medium | Tasks, Habits, and Goals were reordered without a visual explanation on Home. | Added accessible pinned group headings and explicit boundaries before unpinned items. |
| PIN-05 | High | Home’s three-Goal limit could silently omit a fourth pinned active Goal. | Pinned Goals are never dropped; unpinned Goals use only the remaining slots in the three-item default summary. |
| PIN-06 | High | A pinned routine did not make Gym count as Home content, so a routine-only Home could render the empty state instead of the shortcut. | Gym Home presence and count now use the active workout or pinned routine shortcuts. |
| PIN-07 | Medium | Success feedback said only “updated,” giving no observable cause-and-effect confirmation. | Added domain-specific snackbar feedback for pin and unpin actions, including bulk actions. |
| PIN-08 | Medium | Track and bulk-action wording varied in capitalization and destination specificity. | Standardized singular and bulk copy around Whip Home and Home Quick Log. |

## UI and accessibility checks

- Pinned boundary labels are semantic headings, use a pin icon decoratively, and expose readable text with counts.
- Labels remain short enough for compact layouts; the explanatory sentence lives below inspector actions rather than inside a menu label.
- Existing section ordering, Area scope, schedules, completion state, and archived data remain unchanged.
- Home settings explicitly state that a new pin reveals and expands the owning section.
- Automated coverage protects settings reveal behavior, pinned summary limits, routine-only Home counts, and rendered pinned boundaries.

## Accepted constraints

- Tasks and Habits remain contextual daily surfaces. A future or Inbox Task and a Habit not due today do not become out-of-context daily cards; the pin action says when they will appear.
- Area scope still governs Home. A pin does not silently switch the user to another Area.
- Users may intentionally hide or collapse a section again after pinning. The pin remains saved for future display.
