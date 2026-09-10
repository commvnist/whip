# Empty Insights recovery — 2026-09-10

FND-20260910-033; DEC, IMP and VER-20260910-023. Starting production: `89a47688`. API 34 phone at 1080×2400 / 420 dpi and API 37 wide at 2560×1800 / 320 dpi. Native reviewed journeys use ordinary text. No release or physical-device operations.

Workspace Insights now distinguishes no active Tracks in the selected view from active Tracks without Entries. It offers Create Track with the current Area, View Archived when relevant, or Open Tracks for recording. Per-Track Insights offers the actual Entry editor for active Tracks and read-only Entries navigation for archived Tracks. An unmatched filter keeps its condition and existing Clear All, then explains the empty view instead of showing zero/blank statistics.

The existing WhipEmptyState owns the presentation and bounds its text to 520 dp for wide reading. Features retain scope, filter saved state, editor requests and repository commands. Populated summaries, including zero recent periods with older evidence, remain unchanged. No schema, calculation, backup, version or historical-data change.

## Verification

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted tracks \
  --android com.whip.app.ReviewJourneyE2ETest \
  --android com.whip.app.ReviewAvailabilityUiTest \
  --android com.whip.app.WhipAccessibilityChecksTest \
  --android com.whip.app.TrackHistoryJourneyE2ETest --emulator
```

- Baseline `O8RXUF`: three new observation journeys pass on unchanged phone production; matching wide instrumentation passes three in 30.73s. Ten baseline originals are personally reviewed. They establish the empty-state hierarchy problem while preserving exact archived/other-Area data and existing filter recovery.
- Pilot `Kt0hFE`: thirteen phone checks pass, including four complete new recovery journeys, two numeric journeys and seven workspace/availability/adaptive checks.
- Final wide: TrackInsightRecoveryJourneyE2ETest, TrackInsightsJourneyE2ETest and ReviewJourneyE2ETest pass ten methods in 112.764s. This includes actual Track creation in Main, first Entry Save/recreation, filtered empty-state recovery, archived read-only navigation and complete saved-projection equality. Runner: `commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner`.
- Final phone `yN0488`: 142 fresh Android tests pass in three batches (52/48/42), with zero failures/skips/reuse. The profile JVM phase passes 73 tests in seven suites with zero failures/errors/skips.
- `scripts/check --ready`: 385 JVM tests in 44 suites, Android compilation, lint, debug build and static/harness checks pass in 2m54s.
- Catalog lint passes 515 required states with no pending selectors or platform exceptions. Declared source inventory is 655 JVM + 1088 Android = 1743, distinct from executed counts.

## Evidence and limits

Thirty original pairs are personally reviewed: ten baseline, fourteen final Track and six final neighboring Review views. `manifest.json` preserves original PNG/XML provenance and hashes; `review.tsv` names the personally reviewed scope of each image. PNG bytes remain original. Readable text normalizes trailing whitespace, with gzip copies retaining raw XML and logs. Newly discovered first-entry and archived-empty captures have no matching baseline image. Review screenshots are neighboring final checks, not a fresh whole-Review acceptance claim.

All 519 recorded production/test/harness/APK inputs remain identical through both final campaigns and readiness. Complete Tracks and whole-product acceptance, specialized analytics, large-history performance, independently filtered support panes, remaining Settings/integration/platform journeys and final cross-app gates remain open. No new TalkBack, process-death, physical-device or separate 200% campaign acceptance is claimed.
