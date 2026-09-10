# Productivity item builders — 2026-09-10

FND/DEC/IMP/VER-20260910-004, FB-20260910-001/002. Opening source: `b24b8c7`. Seven productivity-header callers now declare content roles through WhipProductivityItemContent. The renderer owns full-width information and puts Edit after the complete expanded body. Habit running-timer status appears once. Domain actions, state colors, selection/reorder, saveable disclosure and stored interpretation remain unchanged.

## Accepted scope

- API 34 phone: 43 shared controls and 47 product neighbors passed in sR6HSf batches 1/2. Its third catalog batch failed and the overall campaign is rejected. The Track capture fixture was then corrected to wait for the asynchronous no-match result before scrolling; production and the 90 accepted tests did not change. RdkfiK passes all five final page-catalog methods, zero failures/skips/reuse. These separate accepted batches cover 95 distinct methods; this is not a successful 94-test sR6HSf campaign.
- API 37 wide: direct instrumentation passes eight focused/card/catalog methods in 156.322s and a separate Home method in 16.350s. Same production as final API 34, before the last capture-only wait. API 37's original Track capture already passed.
- `scripts/check --ready`: 346 fresh JVM tests in 34 suites, no failures/errors/skips, compilation/lint/debug packaging, 2m23. This creates no store candidate or whole-suite acceptance.
- `scripts/ui-catalog lint` and `scripts/test-ui-catalog`: pass; 389 required states, zero exceptions or pending selectors. The original lint rejection is preserved; discovery was reviewed and its snapshot refreshed for the new shared renderer.
- Exactly two disposable emulators: API 34 phone 1080×2400/420dpi and API 37 wide 2560×1800/320dpi, font scale 1.0. No physical-device operation, release, publication or new per-screen 200% campaign.

## Native commands

The API 34 neighbor command uses `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --emulator`, with one `--android` argument per selector below. The final catalog command uses the five `VisualCatalogPagesTest` selectors alone, adding Shared/Home.

```text
com.whip.app.ProductivityItemBuilderJourneyE2ETest
com.whip.app.ProductivityCardDesignUiTest
com.whip.app.ui.InteractionControlUiTest
com.whip.app.HabitSkipJourneyE2ETest
com.whip.app.ProductivityCreationJourneyE2ETest#taskHabitAndGoalCanBeCreatedAndUsedThroughTheRealUi
com.whip.app.ui.ElapsedGoalTimeUiTest
com.whip.app.TaskBulkSelectionUiTest
com.whip.app.TaskReorderJourneyE2ETest
com.whip.app.TrackWorkspaceUiTest
com.whip.app.VisualCatalogPagesTest#captureTaskPageCatalog
com.whip.app.VisualCatalogPagesTest#captureHabitPageCatalog
com.whip.app.VisualCatalogPagesTest#captureGoalPageCatalog
com.whip.app.VisualCatalogPagesTest#captureTrackPageCatalog
```

Final fifth catalog selector: `com.whip.app.VisualCatalogPagesTest#captureSharedPageCatalog`.

API 37 installs the debug and debug-test APKs with `adb -s emulator-5556 install -r`, then runs `adb -s emulator-5556 shell am instrument -w -r -e class '<comma-separated selectors>' commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner`. The eight selectors are the new native timer method, the two `productivityBuilder...` InteractionControlUiTest methods, the normal mixed-family ProductivityCardDesignUiTest geometry method and the four domain catalog methods. Exact executed names are in api37.log. api37-home.log runs the Shared/Home selector separately.

## Failure and refinement history

- aMg7Uk baseline: five methods, timer reproduces two status nodes; Track catalog races its asynchronous no-match state; the three other domain catalogs pass. Five retained baseline originals were personally inspected.
- 0fpEQ2 pilot: two layout methods, full-width text assertion fails because text still measures intrinsically; later batches do not execute. Renderer now fills the information width.
- vcheWG pilot: two layout methods and 15 existing card tests pass; the timer journey passes one-status/recreation before an invented close-editor selector fails. Correct selector is Cancel Habit editing. The final renderer uses the shared 6dp section gap and invokes the specialized body directly to avoid empty-wrapper spacing.
- sR6HSf: 90 accepted neighbors, rejected final four-method catalog batch. Waiting for the actual search result fixes that fixture; RdkfiK is the final accepted replacement.

## Visual and artifact review

All 33 retained original PNGs were personally inspected: five baseline and 14 final states per device. `review.tsv` records observations for each image. Headers align, summaries/details span available width, expanded Edit follows domain information, and the timer is singular. Native checks exercise start, Activity recreation, Edit cancellation and stop while preserving Habit identity/measurement. Some lower list content scrolls beyond the image viewport; the existing compact elapsed-Goal unit policy is unchanged and remains a separate review lead.

PNG signature/chunk CRCs/zlib/IEND and paired XML parsing pass. PNG bytes are unchanged. Capture manifests preserve original device filenames/hashes; text files normalize only line endings/trailing whitespace, with original/retained hashes in text-normalization.json. `source-and-apk-sha256.json` snapshots final local source/test/APK bytes after readiness; it does not claim the preceding API 37 test APK contains the later fixture wait. Native batch XML and failed/final logs preserve actual execution. Zero XML NAF nodes are not TalkBack acceptance.

Whole-app UX, aesthetics, functional coverage and other builder families remain open. Version 0.3.66/code72, schema46/dataepoch6/backup26 are unchanged. Git history records normal main/upstream delivery.
