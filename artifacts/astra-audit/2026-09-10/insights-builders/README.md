# Shared Insights summaries — 2026-09-10

FND-20260910-031/032; DEC, IMP and VER-20260910-022. Starting production: `cfe8a4a2`. Normal API 34 phone (1080×2400, 420 dpi) and API 37 wide (2560×1800, 320 dpi), both font 1.0. No release or physical-device operations.

`WhipSummaryCard` owns headline metrics, supporting facts, spacing, typography and column reflow. `WhipSummarySeries` presents short ordered series with exact readings and complete accessible dates. Track features retain calculation, units, conditions, navigation and saved data. Recently Active Tracks now uses the existing record builder. The initial phone view includes its complete source card where the previous view reached only the section heading. `docs/architecture.md` maps all six shared presentation families and their ownership boundaries.

Recent per-Track counts previously included future Entries. Eight persisted Entries around the 7/30/90-day boundaries produced incorrect recent totals 3/5/7 and weekly rate 1.17. Both Insights contexts now count inclusively through today: 2/4/6 and 0.93. All eight Entries, their values and the future latest date remain unchanged. Existing mixed-unit precision and non-additive temperature/Scale rules remain intact.

## Acceptance

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted tracks \
  --android com.whip.app.TrackHistoryJourneyE2ETest \
  --android com.whip.app.ReviewOutcomeJourneyE2ETest \
  --android com.whip.app.GymLibraryJourneyE2ETest \
  --android com.whip.app.WhipAccessibilityChecksTest --emulator
```

- `LDk82s`: 138 fresh Android tests in three batches (52/51/35), zero failures/skips/reuse; 73 JVM tests in seven suites. Includes persistence, schema editing, CSV, history/filter recovery, record actions, Review and Gym neighbors.
- `6iPsmC`: 13 final focused Android methods pass after limiting headline metrics to two columns and scrolling explicitly to numeric readings. Includes TrackInsightWindowJourneyE2ETest, TrackInsightsJourneyE2ETest, RecordItemBuilderJourneyE2ETest, WhipSummaryBuilderTest and TrackWorkspaceUiTest.
- Matching final APKs on wide: the same three native journey classes plus WhipSummaryBuilderTest pass six methods in 52.155s. Runner: `commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner`.
- Original-image review rejects the new window fixture's premature workspace captures. Its visible-page assertion and Compose idle synchronization are then verified by `RUsmnJ` (one phone method) and one wide method in 9.284s; both corrected workspace/detail pairs are personally reviewed.
- `scripts/check --ready`: 385 JVM checks in 44 suites, zero failures/errors/skips, Android compilation, lint, debug build and static/harness checks pass. Initial readiness took 2m37s; the capture-only final rerun passed in 28s.
- Catalog lint: 508 required states, no pending selectors or platform exceptions. Declared inventory: 655 JVM + 1084 Android = 1739; this is distinct from executed counts.

## Provenance and visual scope

Seven original baseline pairs and fourteen final pairs are retained and individually reviewed. `review.tsv` describes each accepted scope; two rejected premature captures are retained separately. Numeric views intentionally scroll within their lists, so acceptance concerns the visible measurements and their context, not every card heading being simultaneously visible. All retained accepted hierarchies pass the unnamed-interaction guard. PNG bytes are original; readable XML/log files normalize trailing whitespace and gzip files preserve raw bytes.

All 518 inputs match through the broad regression. The final refinement changes only WhipSummaryBuilders, the numeric journey and their two APKs; the subsequent capture correction changes only the window journey and test APK. Exact snapshots and deltas are in `checks/`. All 518 delivery inputs match after the last native run and readiness. No data, schema, backup or version change.

## Intermediate results and limits

Unchanged baselines pass three native methods on phone (30.499s) and wide (37.954s). `QVSfuN` then reproduces the recent-window defect on unchanged production. `5hxsGt` exposes merged-parent bounds in older numeric fixtures and intrinsic metric-width overflow. `YBtAne` passes native data journeys while the width case still fails; `rp2vhx` passes all five focused replacements after the shared width correction. A wide checkpoint exposes heading-only scrolling; exact numeric visibility assertions are retained when correcting it.

`FmNIQ7` is incomplete after both emulators disappear. The same two AVDs are restarted in persistent exec sessions; no interrupted result counts as acceptance. The broad regression and final campaigns above complete normally.

This is a bounded shared-summary and correctness increment. Remaining app coverage, empty/no-entry Insights hierarchy, specialized Goal/Habit analytics, independently filtered support panes and final whole-product acceptance remain open. There is no new TalkBack, process-death, physical-device or separate 200% acceptance claim.
