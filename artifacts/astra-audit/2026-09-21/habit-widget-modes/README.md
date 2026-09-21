# Pending Habit widget Count/Timer replay

`replay.py` is a prepared, syntax-checked host replay for a disposable API 34 Pixel Launcher with a real pinned Habit Tracking widget. It would create synthetic Count and Timer Habits through that widget, check Count increments, retain a running timer through Android-managed process death, and verify exactly one elapsed log on Stop. It refuses non-emulators and does not clear app data.

The owner paused the audit before this script was executed or its widget precondition was established on the current emulator. No Count/Timer behavior, screenshot or process-death result is claimed from it. The previously accepted Daily/CheckOff pinned-widget journey remains recorded in `../habit-widget-launcher/README.md`.
