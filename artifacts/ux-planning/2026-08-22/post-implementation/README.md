# Post-implementation UX acceptance

Core build under review: `commvne.com.whip.app`, version `0.3.9` (`15`), signed release APK. A later same-version follow-up fixes Fold status-bar ownership; its exact build identity and evidence are recorded in `acceptance.txt` and `../status-bar-regression/`.

## Evidence rules

- Every capture comes from the live installed release, never historical repository screenshots.
- The installed base APK SHA-256 must match the locally built signed release before capture.
- Android status/notification and Samsung navigation/task bars are excluded from every retained image.
- Captures containing notifications, personal content, a keyboard when it obscures the reviewed surface, or another app are rejected and replaced.
- Open/closed Fold transitions are performed manually by the device owner. No instrumentation, wake, unlock, lock, or fold shell command is used on the personal phone.
- UI state is verified before each capture so a previous frame cannot be filed under the next screen's name.

## Accepted screenshot sets

- `compact-fold/cropped/`: the full core-route inventory in closed-Fold mode at `1080 x 2263`, cropped from the native `1080 x 2520` display at the live system insets.
- `open-fold/cropped/`: exact-build book-mode evidence at `2256 x 2133`, cropped from the native `2256 x 2504` display at the live system insets. The Home split layout validates the persistent navigation rail, destination context pane, content pane, and the regression that previously pushed content below the viewport.

The core set covers Home; Tasks Today and Upcoming; Task creation and repeat consequences; Habits Today and Insights; the true Check Off editor state; Goals and goal creation; Gym Workout and Library; Settings; Areas list/detail; scoped search; and Review & Trends. The exact build identity, test gate, and capture inventory are recorded in `acceptance.txt`.

| Compact capture | Acceptance focus |
| --- | --- |
| `home.png` | global context, Area scope, Home hierarchy, bottom navigation |
| `tasks-today.png` | destination hierarchy, compact Filter, immediate Quick Add, empty state |
| `tasks-upcoming.png` | valid List/Agenda/Calendar planning views |
| `task-editor-top.png` | identity, Area, Schedule order |
| `task-editor-repeat.png` | Repeat consequence plus immediately adjacent Starts/Repeats controls |
| `habits-today.png` | true check-off action and item controls |
| `habits-insights.png` | low-data analytical state |
| `habit-editor.png` | Check Off mode with no numeric target and its explicit consequence |
| `goals-active.png` | goal destination hierarchy and full tab labels |
| `goal-editor.png` | Starting Value, Target, Deadline, and Area order |
| `gym-workout.png` | Gym page anatomy and complete Progress label |
| `gym-library.png` | navigation-row grammar for library destinations |
| `settings.png` | category landing rows with descriptions and chevrons |
| `areas.png` | destination-sized Area list and create action |
| `area-detail-main.png` | Identity, Organization, and Lifecycle grouping |
| `search-all.png` | explicit All Whip scope and progressive search controls |
| `review-trends.png` | full-screen empty analytics and one Close affordance |

`open-fold/cropped/home.png` verifies the exact release's two-pane Home layout after the tab-strip measurement regression fix.
