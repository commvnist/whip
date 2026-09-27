# Pinned Count and Timer Habit widget journey

The source-linked [replay](../../2026-09-21/habit-widget-modes/replay.py) ran on disposable API 34 Pixel emulator `emulator-5556` with a real `HabitTrackingWidgetProvider` pinned in Pixel Launcher. Setup was completed before the run. The replay created two uniquely named daily Habits through the widget's Add action, used only the debug package, and did not clear app data. [Exact proof](run-api34/proof.json) accompanies seven original PNG/XML pairs.

| Boundary | Observed visible and saved result |
| --- | --- |
| Count, before/after first managed process death | The pinned `Add 1` row remained visible with `0 of 1`; PID changed from 28314 to 29362. |
| Count increment | Exactly one `Recorded` log, `value=1.0`, `canonicalValue=1.0`; widget changed to `1 of 1`. [Rendered state](run-api34/count-once.png). |
| Timer start and second managed process death | Widget offered Stop with elapsed time; `habit_timer_sessions` retained `Running|2` across the second death. PID changed from 29362 to 29578 after the next action. [Recovered running state](run-api34/timer-running-after-death.png). |
| Timer stop and in-app review | Exactly one canonical `12.217` second `Recorded` log with source `habit-timer-v1:d6280577-c901-4c76-9405-0a0509508469`; timer session became `Completed`, saved timer fields cleared, widget returned to Start, and Habit History showed `Logged 12 sec`. [History](run-api34/timer-history.png). |

Every retained XML pair has no unlabeled Whip-owned interactive (`NAF=true`) node. Original widget and History screenshots were inspected for row identity, state and legibility. No Count/Timer widget product defect reproduced on this configuration. This is one launcher/API journey, not an OEM or cross-day matrix.
