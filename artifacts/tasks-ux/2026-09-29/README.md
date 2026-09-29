# Tasks clarity and content hierarchy

Owner feedback: FB-20260929-011. Implementation: IMP-20260929-015. Verification: VER-20260929-012.

The quick-entry field says **Add a task**, with a plus action. The collection summary uses one count and scope (for example, **3 unfinished tasks**). **Plan My Day** is first in the existing overflow menu when applicable, freeing space above the records. Cards, inspectors, duplication, subtask conversion and planning use **Unscheduled** consistently. Tasks / Today / History and the shared toolbar/context anchors remain in place.

This uses the existing field, menu and record components. There is no schema, stored-route or release-version change. The existing planner regression also exercises the new collection entry and retained preview before returning to Today.

## Verification

- Debug and instrumentation APK preparation passes (135 seconds after compiler recovery).
- Affected readiness JVM portion passes: 427 tests, 44 suites, zero failures/errors. Android test compilation and debug packaging pass.
- `timeout --kill-after=3s 55s scripts/ui-catalog lint` passes: 530 required captures, zero platform exceptions or pending selectors.
- Planner draft restoration through Home, collection and Today passes in 25.403 s. Quick capture/default-date and Add Details assertions report a passed method; the containing two-method run reaches 55 s during its next test, so that run is incomplete. The empty-state method passes separately in 16.590 s, including the corrected “No Unfinished Tasks” collection heading.
- The isolated native keyboard fixture initially lacked interactive-window inspection flags. After making it self-contained, the replay reaches and captures the complete label/input above the keyboard after recreation, then hits the 55 s bound. Its full save/recreation journey remains incomplete; the separate creation/default-date check and manual keyboard submission pass.
- Final settled-source `scripts/check --ready` passes, including the 427 affected JVM tests, Android compilation, debug packaging and lint. The final lint/build invocation takes 6m42s; routine attempts and actual native runs retain their 55-second bounds. No full-suite or frozen-candidate batch is run.
- Personally reviewed originals: `visual/tasks.collection.png`, `tasks.actions.png`, `tasks.large.png`, `goals.large.png` and `tasks.quick-capture.native.ordinary.png`. The normal and actual-200% page keep full labels and accessible actions. Matching hierarchies prove identical Task/Goal toolbar, context, search and settings bounds at 200%; see `checks/header-bounds.txt`. Font scale restored to 1.0. Appearance still awaits owner validation.

The initial icon receiver compilation error was fixed. Subsequent readiness/Gradle native attempts exceeded their 55-second bounds; the first native build also encountered a stale compiler socket. These are incomplete, not passes. APK preparation was then separated from test execution. Direct instrumentation uses `scripts/android-target-guard instrumentation`, the matching installed APKs, explicit disposable emulator serials and 55-second timeouts. No physical phone operation or full-suite rerun occurs in this correction.

Exact diagnostic/build/test logs are retained locally under `build/tasks-ux-20260929`; accepted focused results and selected original captures are retained alongside this receipt at closeout.
