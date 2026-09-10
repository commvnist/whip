# Review history, scope and ordinary dashboard layout

Verified increment: FND-20260910-010/011/012/014; DEC/IMP/VER-20260910-008. Baseline production is `ee2bd962b1136229e34ed76d68cfaee9e364a29d`. Exact tested changed-source and debug APK hashes are in `source-and-apk-sha256.json`; the earlier pre-grid checkpoint is separate. Git history identifies the delivered commit.

Archiving definitions now preserves completed Task/Habit outcomes. Global Track evidence counts both Areas, its drill-down temporarily opens All Areas without changing the saved productivity scope, and Track-only Home offers Review. Compact options disclose in place while keeping the current scope visible. Wide cards use equal weighted rows to distribute actual pixels reliably.

## Accepted final checks

- `accepted-neighbors.log`, native run `HMQrZg`: 196 fresh Android tests across 43/153 batches on API 34, zero failures/skips/reuse.
- `api37-final.log`: all three real Review journeys, the existing shared-shell catalog and the wide hinge-aware dashboard regression pass: five tests, 46.309 seconds.
- `readiness.log`: `scripts/check --ready` passes in 2m28. Retained XML contains 361 JVM tests in 38 suites, zero failures/errors/skips. Android-test compilation, lint, debug packaging, static and routed harness checks pass. Earlier focused checks pass 15 JVM tests; counts overlap and are not additive.
- `catalog-lint.log` and `catalog-fixtures.log`: `scripts/ui-catalog lint` and `scripts/test-ui-catalog` pass. Current inventory is 400 required states, zero pending selectors/platform exceptions; source declares 636 JVM and 1,053 Android tests, not all executed here.
- Both disposable emulators use `font_scale=1.0`: API 34 phone 1080×2400/420dpi and API 37 wide 2560×1800/320dpi. Production, tests, scripts and APKs were frozen during native campaigns. No physical-phone operation or release occurred.

Phone command:

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile shell \
  --android com.whip.app.ui.AreaFeatureUiTest \
  --android com.whip.app.TaskRepositoryTest \
  --android com.whip.app.HabitRepositoryTest \
  --android com.whip.app.TrackWorkspaceUiTest \
  --android 'com.whip.app.VisualCatalogSharedShellTest#captureSharedShellCatalog' --emulator
```

After installing both exact debug APKs on `emulator-5556`, the final wide command was:

```sh
/usr/sbin/adb -s emulator-5556 shell am instrument -w -r \
  -e class 'com.whip.app.ReviewJourneyE2ETest,com.whip.app.VisualCatalogSharedShellTest#captureSharedShellCatalog,com.whip.app.AdaptiveWhipScreenTest#reviewUsesTheWholeWideCanvasAsAHingeAwareDashboard' \
  commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner
```

## Original visual evidence

All 20 retained original PNGs were individually inspected. `review.tsv` records scoped observations; folder manifests preserve original exported names and hashes. XML companions were parsed, PNG structure/CRCs/decompression verified, and each PNG/XML pair's original stem matched. Images were not edited. Text-only whitespace normalization is recorded with original/retained hashes in `text-normalization.json`.

- `before/`: original ordinary Review hierarchy, incorrect Area-scoped All Tracks count and missing archived outcomes.
- `wide-before/`: two successful-test checkpoint originals reveal the collapsed two-column grid. Each 861px card plus a 24px gap requires 1746px, but the content provides 1745px.
- `diagnostic/`: the failed-test original already shows the correct three Tasks and one Habit. It documents a merged-tree selector mistake, not missing rendered progress.
- `final-api34/` and `final-api37/`: seven states each cover ordinary summary, Area/global Track evidence, archived outcomes, Monthly/section options after recreation, All Tracks drill-down, Track-only Home and Track-only Review.

The real archived journey asserts three Work Task outcomes (once plus two recurring completions) and one Work Habit outcome, with Personal evidence excluded, before and after recreation. The Track journey asserts two Entries across two Tracks, persisted Monthly/section choices and unchanged saved Work Area after global drill-down. Native wide geometry checks shared rows, complete width and the final partial row after deselecting Gym. Track-only Review keeps evidence separate from comparable outcomes.

## Failed and intermediate checkpoints

These are retained for diagnosis and are not combined with final acceptance:

- `baseline.log` / `vzfMiy`: existing shared-shell catalog passes on unchanged production.
- `reproduction.log` / `4mNk6h`: new fixture incorrectly used a getting-started-only Home tag; corrected to the actual returning-Home action.
- `reproduction-2.log` / `KjBpEH`: reproduces missing archived outcomes and missing Track-only Home discovery. `area-reproduction.log` / `h26IH2` adds a genuine Work outcome to reach the separate incorrect global Track count.
- `pilot.log` and `instrumentation-diagnostic.log`: production compiles but JaCoCo rejects oversized WhipScreen bytecode. Extracting its existing Area-label mapping into ReviewEvidence reduces host inlining; instrumentation remains enabled.
- `pilot-2.log` / `rimL8k`: initial two journeys plus shared-shell capture pass. `focused.log` then rejects a JVM fixture Boolean where HabitLogStatus is required, before Android execution.
- `focused-2.log` / `FBRnS3`: 15 JVM checks pass; three of four Android checks pass. `archived-focused.log` / `3vM66m` rejects a timing hypothesis. `archived-diagnostic.log` / `WzpFPU` proves correct ViewModels and visible totals; querying unmerged child Text fixes the test. Temporary diagnostics were removed. `archived-final.log` / `byVTwh` passes the corrected exact native totals.
- `neighbors.log`: preflight rejects an incorrect AreaFeatureUiTest package. `neighbors-final.log` / `uYwFQv` then passes 196 Android checks. `api37.log` passes five methods in 38.886 seconds, but personal visual review finds the wide-grid defect.
- `wide-reproduction.log` / `eL0mCJ`: new native row assertion fails the old grid. `wide-focused.log` / `5nWTPc`: weighted rows pass the complete Area/options/recreation/drill-down journey. Final phone and wide campaigns above repeat the changed source.
- `api37-missing-runner.log`: Gradle cleanup had uninstalled the runner; `api37-final-install.log` reinstalls both final APKs before the accepted five-test replay. No product failure is inferred from missing test installation.

## Limits and next work

This accepts retained history, declared Track scope/discovery and bounded normal-scale Review layout. It does not accept the complete Review domain or whole app. FND-20260910-013 still requires runtime investigation of loading/error qualification, partial progress and recovery. Deeper correlations, remaining source journeys, broader design/accessibility/platform coverage and final whole-product gates remain open. Existing sparse wide Home duplication remains in the broader design backlog. No schema, data epoch, backup format or version changed.
