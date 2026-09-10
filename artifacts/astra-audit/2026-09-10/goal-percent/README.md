# Goal progress stays meaningful throughout its history

FND-20260910-019 / DEC/IMP/VER-20260910-012. Baseline source: `3b857744de76347c6bc4b82a9c362ed861e0b6a7`. Application remains 0.3.66/code 72, schema 46, data epoch 6 and backup format 26. No release or migration.

A saved 0.05 against a target of 10 now reads **0.5%**, replacing the misleading 0%. One locale-aware rule replaces eight integer conversions in GoalScreens: Home/collection summaries, expanded cards, Insights, inspector outcomes, the accessible trend table and frozen closure history. Positive progress below 0.1% reads `<0.1%`; progress above 99.9% but below complete reads `>99.9%`. Exact zero and complete retain 0% and 100%. Other percentages use at most one decimal without unnecessary zeros.

GoalProgressPresentation formats the existing domain ratio. Actual measurement precision, milestone counts, exact elapsed duration, Goal lifecycle, domain calculations and frozen closure records remain unchanged. Existing productivity builders continue to own layout; this is a shared presentation rule rather than another card implementation.

## Native journeys and visual evidence

The first new MainActivity journey starts with a real saved measurement, checks Home and Goal cards, expands the card, opens the original inspector, reveals the trend table and recreates the Activity. It opens the same Goal from Insights, abandons it, checks its frozen closure, archives it and reopens/recreates its history. The complete closure snapshot must remain equal after archive and recreation.

The second journey creates four Goals representing a tiny start, nearly reached target, exact zero and exact target. It verifies each collection summary and reopens/recreates the near-target inspector, which retains Active status.

- `before/`: two personally inspected original PNG/XML pairs reproduce Home 0% and tiny/near collection truncation on unchanged production.
- `final-api34/` and `final-api37/`: nine personally inspected original pairs each cover Home, expanded card, overview, recreated trend table, Insights, closure, recreated archived history, all four endpoints and the recreated near-target inspector.
- `review.tsv` records individual observations and unlabeled-node counts. Manifests preserve original device filenames and hashes. The retained PNG bytes are unchanged; CRC/decompression, XML parsing and PNG/XML pair identity pass.
- Native phone and wide frames use the production dark theme at font scale 1.0. The phone is 1080×2400 at 420 dpi; the wide device is 2560×1800 at 320 dpi. Percentages and actions remain readable. Existing wide card width remains a shared reading-measure follow-up, not a claim of completed wide-layout design.
- `source-and-apk-sha256.json` records 496 source/test/script/APK hashes before the final campaigns. All remain equal after readiness. `text-normalization.json` records original and retained hashes for text-only whitespace normalization.

## Executed checks

| Run | Outcome |
| --- | --- |
| wcmZsG, `reproduction.log` | Two expected baseline failures: small and tiny progress display 0%. |
| `model.log` | Seven JVM checks pass: three new locale/endpoint methods and four existing compact elapsed/milestone summary checks. |
| uybwCo, `pilot.log` | Six of seven native methods pass. Fixture incorrectly expects two expanded percentage labels; builder correctly presents one. |
| igkxBs, `pilot-2.log` | Six of seven pass. Fixture targets a child title in the merged tree; corrected to the existing unmerged title. |
| 7U93OZ, `pilot-3.log` | Endpoint method passes; lifecycle fixture uses lowercase inspector section tags instead of displayed labels. |
| hBuE4M, `pilot-4.log` | Endpoint method passes; lifecycle fixture uses the internal Completed enum instead of the displayed History tab tag. |
| dqwsyP, `pilot-5.log` | Both complete journeys pass after selector corrections. No production navigation fix is claimed. |
| kQ4sKx, `final-api34.log` | 69 fresh Android checks pass, zero failures/skips/reuse. Goals profile plus Review source, Activity History and shared productivity-builder journeys. |
| `final-api37.log` | Nine methods pass in 98.677 seconds, nine successful completions and terminal instrumentation code -1. |
| `readiness.log` | 371 fresh JVM checks in 41 suites, zero failures/errors/skips; compilation, lint, debug packaging and affected static/harness checks pass. |

Final commands:

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile goals --android com.whip.app.ReviewOutcomeJourneyE2ETest --android com.whip.app.ActivityHistoryUiTest --android com.whip.app.ProductivityItemBuilderJourneyE2ETest --emulator

adb -s emulator-5556 install -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5556 shell am instrument -w -r -e class 'com.whip.app.GoalProgressJourneyE2ETest,com.whip.app.ReviewOutcomeJourneyE2ETest,com.whip.app.ui.GoalSecondaryMutationUiTest#terminalMilestoneSummaryUsesTheFrozenClosureRatherThanTheEditedDefinition,com.whip.app.ui.GoalSecondaryMutationUiTest#archivedInspectorPreservesLifecycleHistoryAndHidesMisleadingPin' commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner

scripts/check --ready
scripts/ui-catalog lint
```

The catalog now has 430 required states with no pending selectors or platform exceptions. Declared source inventory is 646 JVM + 1065 Android = 1711 product tests; those totals are not an executed whole-product suite. The audit matrix also restores three previously omitted Review selection rows using the already committed VER-20260910-010 evidence, without claiming new execution for those rows.

All campaigns are terminal. This chunk does not certify whole Goal/app acceptance, real-user screen-reader behavior, API 26, OS-process recovery or the remaining shared design work. No physical-phone operation, publication, delegation or new per-screen 200% campaign occurred.
