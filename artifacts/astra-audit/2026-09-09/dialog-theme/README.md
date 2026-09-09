# Dialog-window theme evidence

Related: `FND-20260909-004`, `DEC-20260909-004`, `IMP/VER-20260909-006`.

Each Compose dialog creates a separate Android window. It now receives Whip's resolved theme through `WhipDialog`; the existing layouts, keyboard properties, and dismissal behavior remain with their callers.

The baseline real journeys failed the appearance checks in both theme directions. Dark welcome, Search, and Habit-editor screenshots visibly lose their status icons. Task-editor and inspector screenshots can appear correct through the activity while their own dialog flags remain wrong; the tests and pixels establish different parts of the evidence.

| State | Before | After |
| --- | --- | --- |
| Dark welcome on light Android | [Before](before-platform.dialog.dark-on-light.png) | [After](after-platform.dialog.dark-on-light.png) |
| Light welcome on dark Android | [Before](before-platform.dialog.light-on-dark.png) | [After](after-platform.dialog.light-on-dark.png) |
| Task inspector | [Before](before-platform.dialog.inspector-dark.png) | [After](after-platform.dialog.inspector-dark.png) |
| Task editor | [Before](before-platform.dialog.task-editor-dark.png) | [After](after-platform.dialog.task-editor-dark.png) |
| Search | [Before](before-platform.dialog.search-dark.png) | [After](after-platform.dialog.search-dark.png) |
| Shared primary Habit editor | [Before](before-platform.dialog.primary-editor-dark.png) | [After](after-platform.dialog.primary-editor-dark.png) |

All twelve images were personally inspected. Matching XML files preserve semantics; PNG bytes are unchanged and XML line endings are normalized. Before source is `88a5286` plus the two regression tests. After images come from the fresh 45-state shared capture in `/tmp/whip-astra-dialog-theme-shared-20260909` (`XhV2Yv`), manifest SHA-256 `1ecf541d7d0f0fae253d8d7adb2a2963f9eff12e59452c0140b70729364e04be`.

Android 8's compact [dark](api26-dark-on-light.png) and [light](api26-light-on-dark.png) windows were also personally inspected. The API 26 tests assert icon appearance and legacy navigation scrim luminance through both real journeys. Modern Android retains its automatic navigation contrast behavior, so window flags and the final gesture-indicator pixels need not imply the same literal color.

Android 17's wide [dark](api37-dark-on-light.png) and [light](api37-light-on-dark.png) windows were personally inspected after both full dialog journeys passed (`n0cG3u`); matching XML is retained.

These images accept the window-theme change only. Whole-feature design, broader dialog large-text evidence, and the remaining app-wide audit are tracked separately.
