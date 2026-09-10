# Review availability and recovery

FND-20260910-013; DEC/IMP/VER-20260910-009. Baseline production is clean pushed `622f2e20a4ba8361da5312b812982544191dd389`. Exact tested changed-source and debug APK hashes are in `source-and-apk-sha256.json`; Git history identifies the delivered increment.

Review now names loading and failed sources in one shared notice and keeps ready results visible. Unavailable sources cannot contribute totals, Track evidence or correlations. Retry targets only failed selected outcome sources plus global Tracks, through the existing source-owned callbacks. A global empty-outcome state requires every selected outcome source to be ready. Recovery preserves the open dashboard.

## Verification

- `neighbors.log`, native run `YF2gUp`: 91 fresh API 34 shared-app Android checks in 43/48 batches, zero failures/skips/reuse.
- `api37.log`: seven final API 37 methods pass in 50.821 seconds: three availability methods, the three existing real persisted Review journeys and the hinge-aware wide dashboard regression.
- `focused.log`, native run `7CO9U5`: six Review methods and 16 JVM tests in four suites pass. The three new JVM policies and three new Android methods are included in the permanent shell profile. Counts overlap and are not additive.
- `readiness.log`: `scripts/check --ready` passes in 2m23, including 364 JVM tests in 39 suites with zero failures/errors/skips, Android-test compilation, lint, debug packaging, static and routed harness checks.
- `scripts/ui-catalog lint` and `scripts/test-ui-catalog` pass: 406 required states, zero pending selectors/platform exceptions. Source declares 639 JVM and 1,056 Android tests (1,695 total); the full matrix was not executed here.
- Exactly two disposable emulators were used: API 34 phone 1080×2400/420dpi and API 37 wide 2560×1800/320dpi. Both finish with `font_scale=1.0`. App source, tests, scripts and APKs stayed frozen during native campaigns.

Final phone command:

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile shell \
  --android 'com.whip.app.VisualCatalogSharedShellTest#captureSharedShellCatalog' --emulator
```

After installing both exact final debug APKs on `emulator-5556`:

```sh
/usr/sbin/adb -s emulator-5556 shell am instrument -w -r \
  -e class 'com.whip.app.ReviewAvailabilityUiTest,com.whip.app.ReviewJourneyE2ETest,com.whip.app.AdaptiveWhipScreenTest#reviewUsesTheWholeWideCanvasAsAHingeAwareDashboard' \
  commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner
```

## What the new checks prove

The controlled production Home → Review host renders a ready Task beside loading Habits and failed Goals/Tracks. Tests check exact ready totals, absence of unavailable totals/evidence, saved-instance-state restoration and exact retry recipients after the selected sections change. Retry never dispatches to loading or hidden outcome sources. Recovery restores honest zero totals and saved Track evidence without closing Review.

A separate source transition proves that failed/unfinished sources do not become an empty-history claim; the normal empty dashboard appears only after all outcome sources settle ready/empty. The comparison case preserves a valid 30-day Task–Workout correlation while Tracks fail, removes it and the Gym total when Gym fails, and restores it on recovery. Weekly totals are seven each; the 30-day coefficient is 1.00 with n=30.

These availability inputs and retry completions are controlled UI states. They exercise production rendering and callback routing, not injected Room failures or actual filesystem repair. The existing persisted Review journeys separately retain archive totals, global Track scope/drill-down and option/recreation guarantees. Saved-state emulation in the new fixture is distinct from OS process-death testing.

## Original image evidence and rejected checkpoints

Fifteen original PNGs were personally inspected: twelve final images (six per device), one mixed-source baseline, one retained-value baseline and one pre-transition pilot diagnostic. `review.tsv` records observations and limits. Manifests preserve exported names and hashes. PNG structure/CRC/decompression and XML parse/pair identity are checked. Images are unedited; text-only normalization records original/retained hashes.

- `before/shared.review.partial-sources`: loading Habit is shown as zero; failed Track evidence is unqualified.
- `stale-checkpoint/shared.review.sources-unavailable`: a retained failed Task value is shown as current; this is not the later empty-source transition.
- `pilot/shared.review.sources-unavailable`: capture occurs before the requested failed-Task transition settles. It shows a still-ready Task with other sources loading. This frame cannot accept the intended transition; final capture waits for Compose idle and shows the exact failed state.
- `final-api34/` and `final-api37/`: incomplete mixed sources, recovered results, all-incomplete outcomes, complete empty data, available correlation and failed comparison source. Phone comparison frames are deliberately scrolled to the result. The controlled phone host has test-Activity system-bar chrome; it does not establish production status-bar appearance.

`reproduction.log` is a fixture compilation failure: an ambiguous StateRestorationTester import and null non-null Track Area. Corrected `reproduction-2.log` / `BfjnTU` fails both intended availability assertions on unchanged production. `empty-reproduction.log` / `PBXqUP` also fails the missing notice, but its early frame does not prove the later empty-data state. `pilot.log` / `lV9NSn` passes the first two corrected production methods. Final tests strengthen capture synchronization and add explicit comparison coverage.

## Remaining scope

This verifies Review availability and recovery at the UI/source boundary. Complete Review still needs substantive period-empty copy, full source drill-down journeys and deeper outcome/correlation semantics review. The whole-app design, accessibility, platform/performance and final comprehensive gates remain active. Version 0.3.66/code72, schema46/dataepoch6 and backup26 are unchanged. No physical-phone, release or publication operation occurred.
