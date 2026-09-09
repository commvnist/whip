# Reconcile the Track frame review with original pixels

VER-20260909-020 corrects mistaken visual interpretations recorded with `6c95190`. The alleged missing Close icons, clipped Area outlines, overlapping phone Entry rows and touching wide destination labels are not supported by the original pixels and bounds. Those hypotheses are rejected; no production or capture-harness change is justified by this investigation.

The direct byte/pixel checks were decisive:

- The original API 26 enlarged inspector has 108 dark Close-icon pixels inside native bounds `[382,51][454,123]`, forming a complete X from `(408,77)` through `(427,96)`.
- The original API 37 ordinary inspector has 244 dark Close-icon pixels inside `[1888,196][1984,292]`, forming a complete X from `(1922,230)` through `(1949,257)`.
- The original API 34 ordinary archive paints its six Entry titles on distinct y intervals: 1209–1247, 1381–1419, 1553–1591, 1725–1763, 1897–1935 and 2069–2107. Each fits its independent native text bounds; the alleged fourth/fifth row overlap is absent.
- Representative horizontal rows through the original enlarged Area controls start painting at x=24 on API 26 and x=43 on API 34, preserving their normal left margins.
- The wide enlarged Archived label ends at x=2017 and Insights starts at x=2243, a 226-pixel gap. Inner Entries/Options/Track Insights have 40-pixel gaps (20 dp). Status ends at x=2018 and Restore starts at x=2184. The apparent touching text in the earlier review was a mistaken interpretation.

A temporary diagnostic overlay on the existing real Track journeys captured native frames at three samples separated by 750 ms and hierarchy export, then a fourth after advancing the Compose clock by 2 seconds. Both ordinary/enlarged journeys passed on API 26 and API 34 with zero failures/errors/skips. All four frames for each API 26 state are byte-identical. Five of six API 34 state sequences are also identical; only ordinary archive changes, by 188 pixels in the system status-icon region `(934,49)`–`(966,81)`. Its application pixels remain unchanged. The initial claim that a later frame acquired its Close icon was therefore wrong and has been corrected explicitly.

`sequence.tsv` records all 48 sample hashes and maps duplicates to 13 retained byte-original PNG/XML pairs. The overlay is preserved as `diagnostic.patch` for reproducibility and has been removed from the live test source. `read_png.py` is a small standard-library decoder for read-only original-pixel inspection; it does not modify images. Run it with the original inspector paths to reproduce Close-icon counts. The prior originals remain in [inline-keyboard](../inline-keyboard/README.md).

Commands used, after applying the diagnostic overlay to `6c95190`:

```sh
git apply --unidiff-zero artifacts/astra-audit/2026-09-09/track-frame-review/diagnostic.patch
ANDROID_SERIAL=emulator-5556 WHIP_ANDROID_TEST_SLOT=astra-track-frame-diagnostic ./gradlew --no-configuration-cache :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.whip.app.TrackHistoryJourneyE2ETest -Pandroid.testInstrumentationRunnerArguments.whipFrameDiagnostic=true -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
ANDROID_SERIAL=emulator-5554 WHIP_ANDROID_TEST_SLOT=astra-track-frame-phone ./gradlew --no-configuration-cache :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.whip.app.TrackHistoryJourneyE2ETest -Pandroid.testInstrumentationRunnerArguments.whipFrameDiagnostic=true -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
git apply --reverse --unidiff-zero artifacts/astra-audit/2026-09-09/track-frame-review/diagnostic.patch
```

This investigation does not accept the entire Track design. At 320×533 dp and actual 200% text, the archive opening still leaves barely any space for its first record. Repeated wide support context and zero-active counts alongside archived history also remain product-review topics. FND-20260909-019 is narrowed to the observed small-screen history composition; broad Tracks remains open.

Only disposable emulators were used. No product source, persistent contract, version, release or physical-device action changed. The accepted product/test source returns exactly to `6c95190` after removing the overlay; its passing readiness and neighboring checks remain applicable.
