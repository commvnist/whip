# Habit widget Count/Timer replay

`replay.py` is a host replay for a disposable API 34 Pixel Launcher with a real pinned Habit Tracking widget. It creates synthetic Count and Timer Habits through that widget, checks Count increments, retains a running timer through Android-managed process death, and verifies exactly one elapsed log on Stop plus in-app History. It refuses non-emulators and does not clear app data.

The owner paused the September 21 audit before this script was executed. It was completed on September 27 for the separately authorized Ponytail Ultra plan; the [execution receipt](../../2026-09-27/habit-widget-modes/README.md) retains exact saved and rendered results. The earlier Daily/CheckOff pinned-widget journey remains recorded in `../habit-widget-launcher/README.md`.
