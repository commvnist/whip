# Cross-page alignment and empty-state consistency

Owner correction FB-20260910-007, completed September 11 after starting September 10. Baseline: `8f656b8b3bcfdda000eeb6c1605eda2b3462b2f7` / Whip 0.3.68. Implementation and release preparation: IMP/VER-20260911-001, version 0.3.69/code 75.

The current owner display is 1080×2520 at 480 dpi (360 dp), narrower than the previous release's 411 dp evidence. All runtime tests and retained images here use synthetic data on an explicitly selected disposable API 34 emulator. Phone was locked during initial read-only inspection; no owner records are captured.

## Changes and observed geometry

- Page introductions and empty explanations share one 14 sp supporting-text renderer. Empty titles share the existing 20 sp semibold role, with a common 16 dp vertical inset. Ordinary introductions are concise, without reserved lines or clipping.
- All five primary phone headings begin at x=60/y=554; supporting text ends at y=736 and first content starts at y=760. The same test switches through 20 primary/neighboring sections. Task History places its selector below the stable History heading. Gym lists and Settings use the shared page insets and 8 dp sibling gap.
- Task capture and searches share `WhipInlineTextField`: an empty idle field uses its name as a placeholder, and reserves floating-label space when needed. A pixel assertion verifies the visible outline, because Material's internal label inset was invisible to the earlier node-bounds assertion.
- Empty Tracks and Gym now have matching title bounds y=808–879 and description bounds y=903–1012 in the retained matched phone states.
- One centered 1000 dp maximum workspace column replaces destination-specific 720/1000 dp widths. Within Gym's actual wide pane, Workout/History headings move from x=761 to the same x=621 used by Progress. Track list/Activity/Insights share x=460 after removing the list's separate 12 dp inset. Real support/browser/fold pane ownership remains distinct.
- The wide review exposed a partly clipped Gym chart-point chip without an accessible name. Its interactive node now retains the complete existing point label; its history action and values are unchanged.

## Retained native evidence

All 49 PNGs were personally inspected and retain their original bytes. Paired XML is normalized only for line endings/trailing whitespace. `captures.sha256.json` identifies all 98 assets; the geometry JSON files record tagged native bounds.

| Directory | Pairs | Provenance |
| --- | ---: | --- |
| before | 6 | Unchanged 0.3.68-source phone baseline |
| before-wide | 4 | First iteration, before the common workspace width; not release baseline |
| phone | 31 | Reviewed Tasks/Habits/Goals/Tracks/Gym sections and Settings index |
| wide | 8 | Common-width Tracks/Gym pages and exact Track record journey |

The phone captures precede the final wide-only width/master-inset changes and nonvisual chart semantic label. The final phone smoke verifies all current geometry, navigation and chart action after those changes. Final Gym wide captures include the label fix; other final wide images precede only that nonvisual fix. This is scoped evidence, not a fresh capture of every app state.

## Runtime results

`android-results.json` retains exact methods and phase outcomes. All 25 distinct selected methods have a passing latest result, with no unresolved failure. Existing tests were strengthened; source counts remain 652 JVM / 1077 Android.

| Run | Scope | Tests / failures |
| --- | --- | ---: |
| rcDlyu | Unchanged phone baseline catalogs | 5 / 0 |
| TQWpNd | Initial phone catalog, geometry and interactions | 19 / 2 |
| u5d2hZ | Shared inline field, phone geometry and interactions | 13 / 1 |
| dYBE4q | Exact navigation replacement and Gym search journey | 2 / 0 |
| cvCljq | Initial wide geometry/catalog | 3 / 1 |
| 7nh0G3 | Common wide column and three builder journeys | 6 / 1 |
| GvAx7V | Exact Gym catalog after chart-name fix | 1 / 0 |
| aYH6jd | Final phone geometry, navigation and chart action | 3 / 0 |

The initial phone failures were a wrong Goal tab tag and stale navigation copy. The refined navigation assertion then matched both page context and empty copy; it now targets the page-supporting-text role. The two wide Gym catalog failures reproduced the same unnamed chart-point control and are replaced by GvAx7V. Failed batches are not represented as wholly passing.

Commands use `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android <class>#<method>` with one repeated `--android` per selector. Exact class/method selections are in the JSON. Catalog retrieval uses `WHIP_DEVICE=emulator-5554 scripts/device-artifacts ui-catalog-pull <directory>`. Numbered MediaStore outputs were resolved independently for PNG and XML, since a rejected hierarchy can leave an image without its pair. Final readiness uses `scripts/check --ready`; inventory validation uses `scripts/ui-catalog lint` (534 surfaces, zero pending).

The first final readiness process ended with signal 15 after its successful Gradle build but before the wrapper's terminal success; it is not claimed as a completed gate. The completed replacement exits zero, passing 384 JVM checks/44 suites, target-guard fixtures, Android compilation, lint and debug packaging. Its log and the matching final 514-input hash comparison are recorded alongside this report. Inputs were frozen during every live instrumentation run. This bounded campaign does not reopen the closed exhaustive audit or claim full-suite/platform acceptance. Private release and owner appearance acceptance are separate from these emulator checks.
