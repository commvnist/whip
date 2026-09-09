# Focused short Track detail

Evidence for FND-20260909-019, DEC-20260909-017, IMP-20260909-020 and VER-20260909-021. This change gives saved history more room in compact content windows shorter than 600 dp.

The selected Track keeps Back, compact identity, Edit, local destinations, history tools and contextual Restore while global content bars yield. Active Tracks retain a local 48 dp Add Entry action. Back returns to the collection and restores the named primary navigation and global controls. The tradeoff is an extra Back step to switch app areas from short detail. Normal phone and wide navigation retain their existing presentation. Selection, query, editor ownership, restoration and recorded data keep their existing behavior; no schema, backup or release version changes.

The original small-device opening at actual 200% text exposes only 12 pixels of the first title and no date. The final title occupies `[48,476][432,583]`, followed by the full date at `[48,588][233,630]`. Its title starts 96 pixels earlier; yielding the lower navigation additionally restores 216 pixels of reading height. At ordinary text, the first title also moves up 96 pixels. Before images are byte-identical copies from the prior accepted inline-keyboard corpus; its production source is identical to baseline `0b3db50`.

The strengthened real journeys start with 30 archived Entries. They require complete first-title/date bounds before scrolling, then exercise older-record reading, native query/label and identity/Back bounds, query retention and recreation, read-only inspection, returning to Archived with all named primary destinations, contextual Restore, active Add Entry open/cancel, Options, Track Insights, Entries, and exact preservation of Field/Choice/Entry graphs. Both ordinary and actual Android 200% text are covered.

The new restored-state capture also exposed unlabeled Entry Edit/More buttons at the bottom of the phone list. Their icon children had scrolled outside the visible viewport while a strip of each clickable button remained. Labels now belong to the buttons and their icons are decorative. `before-actions-api34` retains the enlarged screenshot from rejected run `kN2ZKW` and a temporary native hierarchy export taken immediately before that capture; the temporary export was removed from test source. Reports preserve both this accessibility failure and the independent ordinary-test error that attempted to scroll an already visible destination in a fixed tab row. The corrected test scrolls only destinations not already displayed.

`review.tsv` records the personal inspection scope of each retained original. Long compact Track identities intentionally ellipsize while preserving their accessible identity; local destinations scroll horizontally at large text. Small enlarged active Entry titles can break inside words beside the two action columns, which remains part of the broader Entry-row design review. The API 26 platform lacks the boot emoji glyph. Query and result reading can require separate scrolling while the keyboard is open. Whole Tracks and whole-product review remain in progress.

The earlier missing-icon, outline-clipping and text-collision claims were rejected in [the original-pixel correction](../track-frame-review/README.md). This change addresses the supported initial reading constraint and does not reinstate those claims.

Final runs and checks are recorded in VER-20260909-021. Native PNG/XML bytes, including XML line endings, are retained without transformations. The scoped catalog includes all 12 outputs from the two Track history owners. No physical-device or release operation occurred; API 26 and API 37 used the second emulator transport at different times, with at most two disposable emulators running.

| Final evidence | Device | Run | Result |
| --- | --- | --- | --- |
| final-api26 | API 26, 480×800 px, 240 dpi | astra-track-focused-final | 2/2 journeys; 12 pairs |
| final-api34 | API 34, 1080×2400 px, 420 dpi | UbbBAC | 2/2 journeys; 12 canonical pairs |
| final-api37 | API 37, 2560×1800 px, 320 dpi | Kfb6F0 | 2/2 journeys; 12 canonical pairs |

Every final report has zero failures, errors or skips; modern runs reuse no results. All 36 final native hierarchies have zero unlabeled interactive nodes. All 39 retained originals (36 final, two initial-layout baselines and one action-label diagnostic) received individual personal review.

The broader neighboring run `QXAADI` passed 64 JVM and 111 fresh Android tests (89 + 22) on the recovered layout before the Entry-label correction. The shared root layout did not change afterward. Final `3rI8c1` reran the affected Tracks profile on the completed source: 64 JVM and 56 fresh Android tests, zero failures/errors/skips/reuse. Retained reports and expected-class/suite inventories distinguish these executions from complete source coverage.

Final `scripts/check --ready` passed in 2m 23s: 344 JVM tests in 34 suites, zero failures/errors/skips, Android test compilation, debug packaging, lint and static guards. `reports/ready-jvm.tsv` preserves the executed suite inventory. Complete source inventory remains 630 JVM / 996 Android tests; catalog/matrix inventory is 281 states with 37 actual-font fixtures. These scoped checks do not establish complete whole-product acceptance. `SHA256SUMS` covers all retained evidence files except itself.

Reproduction, from the repository root and with the corresponding disposable emulator selected:

```sh
ANDROID_SERIAL=emulator-5556 WHIP_ANDROID_TEST_SLOT=astra-track-focused-final ./gradlew --no-configuration-cache :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.whip.app.TrackHistoryJourneyE2ETest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
ANDROID_SERIAL=emulator-5554 WHIP_UI_CATALOG_FILE="$PWD/artifacts/astra-audit/2026-09-09/focused-track-detail/capture-catalog.tsv" scripts/ui-catalog capture build/focused-track-api34
ANDROID_SERIAL=emulator-5556 WHIP_UI_CATALOG_FILE="$PWD/artifacts/astra-audit/2026-09-09/focused-track-detail/capture-catalog.tsv" scripts/ui-catalog capture build/focused-track-api37
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted tracks --android com.whip.app.WhipNavigationTest --android com.whip.app.GlobalSearchRoutingTest --android com.whip.app.AdaptiveWhipScreenTest --android com.whip.app.ImeNavigationRailE2ETest --android com.whip.app.TaskEditorJourneyE2ETest --android com.whip.app.ProductivityCreationJourneyE2ETest --android com.whip.app.ProductivityDefaultsUiTest --android com.whip.app.InlineTaskCaptureE2ETest --emulator
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted tracks --emulator
scripts/check --ready
```
