# Whip Settings QA audit — 2026-08-23

## Scope and method

This pass inventories every control exposed by Whip Settings, traces its
persisted `AppSettings` value to the feature that consumes it, checks compact
and wide Settings navigation, audits instructions that point users back to a
setting, and adds persistence and UI-effect regression coverage. Live evidence
is taken from the installed app; repository screenshots are not evidence.

The audit treats a preference as working only when all four layers agree:

1. the control communicates its scope;
2. the value is saved and normalized;
3. the owning feature reacts to the value;
4. the visible result matches the explanation.

## Findings and resolutions

| Area | Finding | Resolution |
| --- | --- | --- |
| Home visibility | Hiding Goals was persisted, but empty Home always rendered all five module shortcuts. Hidden content could also influence whether Home considered itself non-empty. | Home content detection and empty-day shortcuts now use only visible Home sections. A UI regression test hides Goals and verifies that its shortcut disappears. |
| Home configuration | Raw section switches did not explain whether they affected Home, navigation, data, order, or expansion. Every section could be hidden despite first-run requiring one. | Added a Home Overview explanation, explicit “Show … on Home” controls, state-specific supporting text, a named Home Details disclosure, and a one-visible-section invariant in UI and repository normalization. |
| Health categories | Pausing Health Connect displayed every saved category as unchecked even though the selections were retained and restored later. | Paused categories remain visibly selected but disabled, with “sync paused” status. |
| Notification test | The test button required an already-deliverable Task channel even though the test action itself creates that channel. A fresh install could not run the diagnostic. | A missing channel is now repairable by Send Test Notification; only permission, app-level blocking, or an explicitly blocked channel disables it. Status refreshes after the test. |
| Settings information architecture | Reminders claimed to contain Health Connect, while Health Connect had moved to Data & Privacy. About & Diagnostics claimed to contain notification tests that were actually in Reminders. | Renamed the categories to Reminders and About Whip and rewrote all category summaries to match their actual children. |
| Instructional paths | Onboarding, the workout rest editor, Health Connect rationale, the user guide, backup labels, CSV coverage, and destructive-reset language contained ambiguous or stale locations. | Every actionable reference now names the current route, such as Settings → Planning & Units → Gym Defaults or Settings → Data & Privacy → Health & Privacy. The guide now includes Tracks and current button names. |
| Gym effort defaults | RPE and RIR were mutually exclusive data but appeared as independent switches. Selecting one silently cleared the other. | Replaced the switches with one Default Workout Effort Field choice: None, RPE, or RIR. Exercise-level overrides are explained. |
| Numeric settings | Rest seconds accepted zero and unbounded values while the workout editor supports only 15–3,600 seconds. Other bounded fields silently clamped invalid input. Clock fields silently ignored malformed time. | Added visible range validation for precision, e1RM cutoff, rest time, backup retention, and Health sync days. Clock fields now show the accepted 24-hour format. Repository normalization enforces the same limits. |
| Category allocation | Internal token `PrimaryOnly` leaked into the interface. | Values now read Full Contribution, Split Contribution, and Primary Category Only, with scope explained. |
| CSV export layout | Five equal-weight buttons were compressed into one narrow row. | Export controls now use a centered adaptive three-per-row layout with an Export CSV heading. |
| Reset semantics | Delete All Local Data also reset preferences but did not say so, and it retained portable-backup configuration. A revoked document provider could prevent forgetting the folder. | The action is now Reset Whip and Delete All Data, explicitly lists the affected domains, disconnects the backup folder without deleting external files, and tolerates an already-revoked provider grant. |
| Date boundary changes | A new time zone or late-night cutoff could take up to one minute to update Today in Tasks, Habits, and Goals. | All three date flows now react immediately to Settings changes as well as minute boundaries. |
| Low-pressure mode | Its actual scope was not stated and could look like a global no-op. | Renamed it Low-Pressure Habit Presentation and explains exactly what changes and what does not. |
| Defaults versus existing records | First-day-of-week, Habit week start, Gym units, and workout effort did not consistently state which existing records retain overrides. | Added local consequence copy beside each relevant control. |

## Control-to-consumer inventory

### Appearance & Home

- Advanced controls: editor disclosure defaults and power-user surfaces.
- Low-pressure Habit presentation: Habit cards and Insights language.
- Theme and Android dynamic colors: root `WhipTheme` composition.
- Home visibility/order/details: Home overview sections and empty-day links.
- Hardware keyboard help: appears only when advanced controls are enabled.

### Planning & Units

- First day of week: weekday order, Task/Habit editors, calendars, Review, and
  Gym weekly analytics.
- Time zone and cutoff: application clock, Today, reminders, entries, exports,
  Health import, and portable-backup timestamps.
- Precision and unit defaults: compatible new records and aggregate displays;
  existing equipment-specific units remain stable.
- Custom units: Habit, Goal, and Track number fields, including inline creation.
- Review period, repeating occurrences, Habit planning overlay, repeating-task
  subtask policy, smart capture, and new-Habit week start: each reaches its
  owning editor or view.
- Gym formula, cutoff, rest behavior, set density, effort field, tempo,
  warm-ups, hard-set categories, allocation, effort adjustment, and assisted
  PR policy: each reaches workout execution, repository calculations, or Gym
  analytics.

### Organization

- Areas opens the first-class Areas manager and reports current assignment use.
- Tags list supports rename/merge and archive/restore; tag creation remains
  contextual in editors.

### Reminders

- Permission, app/channel state, repair links, battery guidance, real test
  delivery, and quiet hours correspond to Android notification state and all
  reminder schedulers.

### Data & Privacy

- Portable and one-off backups, retention, restore/merge, five CSV domains,
  complete reset, Health Connect selection/access/sync, and privacy copy match
  their actual storage and permission behavior.

### About Whip

- Release/development identity, version code/name, package ID, and local-data
  summary are the only contents; no moved diagnostic tool is advertised here.

## Regression requirements

- All editable `AppSettings` values survive repository recreation.
- Invalid persisted settings normalize into supported ranges and at least one
  Home section remains visible.
- Hiding a Home section removes both its populated summary and empty-day link.
- A paused Health category remains visibly selected.
- A missing Task reminder channel does not disable the test action.
- A revoked backup provider cannot prevent clearing local folder configuration.
- Every Settings category remains reachable in compact navigation.
- Source integrity rejects retired category names and stale instruction paths.

## Verification completed

- `scripts/check`: 214 JVM tests, Android-test compilation, lint, asset checks,
  and debug assembly passed.
- 24 Settings-focused device tests passed across behavior, compact navigation,
  accessibility, Health pause state, Home shortcut visibility, preference
  persistence, and portable-backup cleanup.
- The signed release APK was installed as `commvne.com.whip.app` version 0.3.9
  (version code 15) without clearing the existing app data.
- Active source and user-facing documentation contain the current category and
  subsection paths; retired `Reminders & Integrations`, `About & Diagnostics`,
  and `Delete all local data` instructions are rejected by regression tests.
