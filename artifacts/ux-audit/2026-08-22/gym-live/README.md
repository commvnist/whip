# Fresh Gym Audit Evidence — 2026-08-22

These captures came from the live `commvne.com.whip.app.debug` package on a connected Samsung Fold in the open, separating posture. The installed release package and its database were not modified.

Use `cropped/` for review: those copies exclude the notification/status bar and bottom taskbar. `raw/` is retained only for diagnosing Fold window and system-bar geometry. `gym-contact-sheet.png` is a compact index assembled from the cropped captures.

The deterministic debug fixture is documented in `seed.sql`. Before it was pushed, SQLite reported `integrity_check = ok`, no foreign-key violations, and these counts:

- 3 exercises
- 1 machine profile
- 4 workout sessions (1 active, 3 finished)
- 10 sets
- 1 two-exercise routine

## Capture index

| File | State/journey |
|---|---|
| `00-first-run.png` | debug-only setup/clean start |
| `01-gym-empty-workout.png` | Gym with no exercises or sessions |
| `02-history-empty.png` | empty History |
| `03-progress-empty.png` | empty Progress |
| `04-library.png` | Library landing |
| `05-exercise-editor-basic.png` | clipped first-exercise editor on open Fold |
| `06-workout-populated.png` | active workout header/next/rest/exercise hierarchy |
| `07-workout-quick-entry.png` | quick entry wrapping at pane width |
| `08-set-editor-fold.png` | full set editor modal geometry |
| `09-history-populated.png` | populated History and card actions |
| `10-progress-populated.png` | progress graph and controls |
| `11-library-populated.png` | populated Library landing |
| `12-exercise-library.png` | exercise list |
| `13-machine-library.png` | machine profile list |
| `14-machine-editor-fold.png` | locked machine editor geometry and density |
| `15-routines.png` | routine library |
| `16-routine-builder.png` | routine outline/day controls |
| `17-routine-exercise-editor.png` | routine exercise prescription editor |
| `18-tools.png` | 1RM/percentage and plate tools |
| `19-categories.png` | empty categories state |
| `20-add-exercise-picker-fold.png` | active-workout exercise picker modal geometry |
