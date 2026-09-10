# Track history controls

2026-09-09 · FND-20260909-035/036/037 · DEC-20260909-029 · IMP-20260909-031 · VER-20260909-032.

Baseline: `3d05d42cf1b356b312ff08bd5b325747f37e09fe`. Scoped acceptance covers paged history, condition reachability/recreation, complete applied criteria, independent Insights filtering and live sort-Field removal. Whole Tracks and whole-product acceptance remain open.

## Findings and result

- The enlarged Choice dialog originally exposed eight of eighteen options without a scrolling body. Its body now scrolls, and active conditions survive Activity recreation. The ordinary API 34 baseline already fits all eighteen options; its unconditional-scroll assertion was not evidence of an ordinary access defect.
- A completed condition originally returned as the next draft after recreation. Each opening now owns a distinct saveable key, with cleanup on Add/Cancel. The native journey proves active-condition restoration and fresh state after Add.
- Correct filtered results originally displayed Field/operator chips without values or a combination rule. Entries and Track Insights now share complete bounded criteria and Match All/Any. Their filter state stays independent.
- Removing the selected sort Field originally threw NoSuchElementException. The current built-in sort/direction now remains usable through definition change and recreation. Saved Entry values stay identical.
- A small enlarged range label wrapped the redundant unit name and symbol. Short symbol labels keep the focused Maximum field complete above the keyboard.

## Execution

| Evidence | Result |
| --- | --- |
| `baseline.log` / PskZwq | Four fixture seed failures: unknown unit ID. |
| `baseline-2.log` / hz2QbZ | Four fixture navigation failures: invented destination selector. |
| `baseline-3.log` / f8kS6i | Four fixture failures: unconditional scroll of already-visible controls without scroll parents. |
| `baseline-4.log` / v3QV3G | Both sort-removal cases throw; enlarged Choice access fails. Ordinary Choice failure is an unnecessary scroll assertion, as its original shows all options. |
| `correction-1.log` / pMTxKS | Both corrected sort journeys pass; both filters reach Choice eighteen and active restoration, then expose stale next-condition state. |
| `nested-baseline*.log` | Two disconnected-target refusals execute no native tests; 703GdF then reproduces stale state with a personally inspected original. A foreground emulator session replaces the exited processes. |
| `nested-correction.log` / V6f9oY | Nested state is corrected; fixture omits the active-filter badge suffix. |
| `applied-baseline.log` / bLeqqb | Exact matching Entry reached; missing visible combination rule confirmed. |
| `summary-correction.log` / sWDE1u | All four core journeys pass. |
| `final-journey.log` / wugME8 | All four expanded API 34 journeys pass, including Between/keyboard and independent Insights. |
| `neighbors.log` / 4nRUTj | 115 fresh Android tests across two batches, zero failures/skips/reuse. This precedes only the final shorter input label and compact-mode fixture refinement. |
| `api26.log` | Three of four pass; enlarged fixture assumes visible Match Any instead of the adaptive dropdown. Its range original motivates the short label. |
| `final-adaptive.log` / ehdfh7 | All four final API 34 journeys pass, zero failures/skips/reuse. |
| `api26-final.log` | All four final journeys pass in 72.207 seconds. |
| `api37-ordinary.log`, `api37-large.log` | Both final pairs pass in fresh processes, 57.529 / 55.718 seconds. |
| `ready.log`, `readiness-jvm/` | 346 fresh JVM tests in 34 suites, zero failures/errors/skips; Android compilation, lint and debug packaging pass. |
| `check-fixtures.log`, `catalog-fixtures.log`, `catalog-lint-final.log` | Harness fixtures pass; reviewed source discovery and all 365 catalog states lint successfully. Initial lint rejection is retained separately. |

Representative exact commands:

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted \
  --android com.whip.app.TrackHistoryControlsJourneyE2ETest --emulator
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile tracks \
  --android com.whip.app.TrackHistoryJourneyE2ETest \
  --android com.whip.app.TrackInsightsJourneyE2ETest --emulator
adb -s emulator-5556 shell am instrument -w -r \
  -e class com.whip.app.TrackHistoryControlsJourneyE2ETest \
  commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner
scripts/check --ready
scripts/test-check-fast
scripts/test-ui-catalog
scripts/ui-catalog lint
```

Direct runs follow explicit installation of both debug APKs. API 37 selects the two ordinary methods together at system font scale 1.0, then their two `AtLargeText` counterparts at 2.0 in a fresh app process. Require the terminal `OK (N tests)` result; shell exit alone does not prove acceptance. Final surviving emulators return to font scale 1.0.

API 26 is 480×800/240 dpi, API 34 is 1080×2400/420 dpi and API 37 is 2560×1800/320 dpi. No more than two disposable emulators ran concurrently; API 26 was stopped before API 37 started. No physical-phone operation, release, publication, schema/backup/version change or history rewrite occurred.

## Visual and evidence scope

All 36 final originals and six retained before originals were personally inspected; `review.tsv` gives each frame a scoped observation. The smallest enlarged frame shows the focused Maximum field and fixed actions while Minimum is above the scrolling viewport. Long Choice lists scroll; a single frame does not show every option at once. Applied criteria wrap within three lines; the full filter dialog remains their review surface. The enlarged Insights capture establishes the applied summary, not simultaneous visibility of all metrics.

PNG bytes are untouched. Capture manifests record the original pulls, including some uncurated diagnostic names; `review.tsv` defines the retained image set. `text-normalization.json` records original/retained hashes for text-only LF, trailing physical whitespace and final-newline normalization. Source/APK hashes and native aggregate/XML evidence are retained. `SHA256SUMS` covers the final evidence directory.

The journey uses 125 Entries and eighteen Choices. It is not a large-history performance benchmark, a full OS-process-death test, or TalkBack/RTL acceptance. Retained-value-incompatible Scale editing, selected-condition definition changes, bulk selection/reordering, larger histories and complete Tracks/app review remain open. Normal main/upstream delivery is recorded in Git history.
