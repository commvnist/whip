# Google Play phone screenshots

The Play Console requires 2–8 phone screenshots. Each asset must:

- be a PNG or JPEG no larger than 8 MB;
- use a 9:16 or 16:9 aspect ratio; and
- measure between 320 px and 3,840 px on each side.

The five listing-ready captures in `screenshots/` are native 1080×1920 PNGs
showing Home, Tasks, Habits, Goals, and Gym with representative local-only
data. They were captured from the Android app in immersive mode, so neither
the Android notification/status bar nor the Samsung navigation/taskbar is
present. Whip's bottom navigation remains because it is part of the app UI.

Run `java scripts/RenderBrandIcon.java --verify` from the repository root to
validate the complete Play Store asset set, including count, format,
dimensions, aspect ratio, and file-size limits for these screenshots.
