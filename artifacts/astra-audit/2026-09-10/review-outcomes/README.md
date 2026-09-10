# Review outcomes and original sources

Verified increment from `e5046e8`; FND-20260910-016/017/018, DEC/IMP/VER-20260910-011. The whole-app goal remains active.

Review cards now reveal the actual contributing outcomes before opening an original Task, Habit, Goal or Workout. The same record builder provides their reading order, typography, context and action surface. One projection of existing domain calculations feeds totals, correlations and list rows. Mixed active/archived history, period/Area scope, distinct recurring occurrences, source failure/retry and Activity recreation retain their meaning.

## Results

| Campaign | Result |
| --- | --- |
| Final API 34, QXgfos | 202 fresh Android tests, 3 batches, zero failures/skips/reuse |
| Final API 37, ordinary wide emulator | 13 methods passed in 111.349 seconds; all 13 completion statuses checked |
| Readiness | 368 fresh JVM tests in 40 suites, zero failures/errors/skips; compilation, lint, debug packaging and affected-change static/harness checks passed |
| Catalog | 421 required states, zero pending selectors; reviewed source inventory gains ReviewAppRoute |
| Visual evidence | 31 personally inspected originals: 4 baseline, 3 diagnostic source destinations, 12 final phone and 12 final wide; paired PNG/XML, original hashes and per-frame observations retained |

The final Android command was:

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted shell tasks \
  --android com.whip.app.GlobalSearchRoutingTest \
  --android com.whip.app.ActivityHistoryUiTest \
  --android com.whip.app.WhipComposeSemanticsTest \
  --android com.whip.app.ProductivityItemBuilderJourneyE2ETest \
  --android com.whip.app.RecordItemBuilderJourneyE2ETest \
  --android com.whip.app.ui.WhipRecordItemTest --emulator
scripts/check --ready
scripts/ui-catalog lint
```

API 37 explicitly installed both matching debug APKs, then ran:

```sh
adb -s emulator-5556 shell am instrument -w -r \
  -e class com.whip.app.ReviewOutcomeJourneyE2ETest,com.whip.app.ReviewJourneyE2ETest,com.whip.app.ReviewAvailabilityUiTest \
  commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner
```

## Before, checkpoints and limits

`r2FrdV` reproduces four unrelated empty general destinations from nonzero cards on unchanged production. The first pilot has a compile-only spacing-token error. Following pilots compile but exceed JaCoCo's instrumented WhipScreen method limit; extracting the Task index alone is insufficient, and extracting the Review host resolves it without disabling instrumentation. One extraction compile needs the existing zoneId extension import.

`Q3uppH` passes 11 native cases, but original visual review identifies wrong destination context: archived Habit opens Options/Danger Zone, archived undated Task claims Inbox, and Workout history claims Search. These are corrected and asserted in the final native journeys. The new numeric test first fails because 0.005 becomes 0.0; the shared locale-aware formatter corrects this across Review totals, chart descriptions and detail facts. A compile-only test typo is also retained.

`cura9v` passes nine of 13 methods; four fixture failures assume text lives on the badge container and use the wrong undated Task key. Correct assertions inspect the status descendant and canonical key. `ZWRavN` then passes 13. A further original review corrects the selected-source failure copy and hides instructions for absent rows; final QXgfos and API 37 cover those last changes.

`review.tsv` identifies each inspected frame and its limits. The source Goal inspector still truncates 0.5% to 0%: FND-20260910-019 is a confirmed next priority, and its image is route evidence rather than full Goal acceptance. Source editors retain their existing domain-specific archive behavior. Controlled source-state restoration is not actual Room-failure injection or OS process-death proof. This increment does not certify every app surface, dataset size, theme, accessibility modality or device configuration. No new per-screen 200% campaign, physical-phone operation, migration or release occurred.

`source-and-apk-sha256.json` matches final tested inputs after readiness. PNG bytes remain unchanged; `text-normalization.json` records trailing-whitespace normalization of retained textual logs/XML. Folder manifests preserve original capture names and hashes. `SHA256SUMS` covers this curated evidence.
