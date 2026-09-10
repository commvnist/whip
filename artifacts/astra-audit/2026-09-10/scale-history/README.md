# Retained Scale history and correction

2026-09-10 · FND/DEC/IMP/VER-20260910-001 · baseline `71841445496e1049a3372062bbdbc26f8c7290f1`.

Scale edits now explain an incompatible saved value beside Minimum, Maximum or Increment and disable invalid Field submission. The final transactional guard still rejects history added after local review, identifying the Field and value. Raw drafts survive recreation; correction, Save and reopening preserve every Entry/value and Field identity.

## Evidence and execution

| Run | Result |
| --- | --- |
| `baseline.log` / XJk18g | Both original native flows pass late rejection, recreation and correction; four original frames demonstrate the avoidable return from whole-Track Save to Field editing. No data loss was observed. |
| `correction.log` / raiATE | Ten JVM domain methods pass. Both native methods pass local bound/increment checks and then fail a fixture querying hidden Text under the failure notice's explicit accessibility description. |
| `visibility.log` / fIeYPY | Same fixture failure. Four personally inspected diagnostics establish visible input errors and complete parent rejection at both scales. |
| `focused.log` / RtxV1B | Both complete API 34 methods pass, zero failures/skips/reuse. |
| `api26.log` | Both complete methods pass in 55.066 seconds. |
| `api37-ordinary.log`, `api37-large.log` | Each complete method passes in a separate fresh app process at system font scale 1.0 / 2.0. |
| `neighbors.log` / cbDE7R | 115 fresh Android tests in two batches (65/50), zero failures/skips/reuse. |
| `ready.log`, `readiness-jvm/` | 346 fresh JVM tests in 34 suites, zero failures/errors/skips; Android compilation/lint/debug packaging pass. |
| `catalog-lint.log`, `catalog-lint-final.log` | Unsupported draft kind `editor` rejected; corrected established `page` kind passes. 373 states, zero exceptions/pending selectors. Source discovery is unchanged. |

Exact commands (run from the repository root):

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --jvm com.whip.app.domain.TrackDomainTest \
  --android 'com.whip.app.TrackFieldEditingJourneyE2ETest#incompatibleScaleHistoryCanBeCorrected,com.whip.app.TrackFieldEditingJourneyE2ETest#incompatibleScaleHistoryCanBeCorrectedAtLargeText' --emulator
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile tracks \
  --android com.whip.app.TrackAuthoringJourneyE2ETest --emulator
adb -s emulator-5556 shell am instrument -w -r \
  -e class 'com.whip.app.TrackFieldEditingJourneyE2ETest#incompatibleScaleHistoryCanBeCorrected,com.whip.app.TrackFieldEditingJourneyE2ETest#incompatibleScaleHistoryCanBeCorrectedAtLargeText' \
  commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner
scripts/check --ready
scripts/ui-catalog lint
```

Direct platform runs follow explicit installation of both debug APKs. API 37 uses each selector separately, force-stopping the app and explicitly setting font scale before launch. A terminal `OK (N tests)` result is required; shell exit alone is insufficient. Actual enlarged error TextLayoutResult is checked at 2.0.

API 26 is 480×800/240 dpi; API 34 is 1080×2400/420 dpi; API 37 is 2560×1800/320 dpi. No more than two disposable emulators ran concurrently. API 37 stopped before API 26 started, and API 26 stopped before the API 37 evidence recovery boot. No physical-phone operation or release/publication occurred.

## Visual review and provenance

`review.tsv` records all 32 personally inspected originals: 24 final platform frames, four baseline frames and four development diagnostics. Local error text and fixed actions are readable. Other Field controls can be above/below the scrolling viewport; these images do not claim simultaneous visibility of the entire form. The smallest enlarged late notice occupies most of the editor body while retaining its complete correction text and Save action.

One initial API 37 PNG pull had 57,489 bytes and lacked its terminal IEND chunk. A later pull of the existing emulator export recovered all 111,709 bytes without rerunning the test or editing pixels. The other seven PNGs and eight XMLs were identical. `capture-recovery.json` records both hashes; the cause of the initial partial transfer is unproven. Only the complete recovered PNG is accepted. All retained PNGs pass chunk CRC, decompression and terminal-chunk checks; final XMLs parse and contain zero NAF nodes.

PNGs are unchanged. `text-normalization.json` records original/retained hashes for LF, trailing physical whitespace and final-newline normalization. Source/APK hashes, exact native XML/aggregates and logs are retained. Capture manifests describe each raw pull and may include uncurated baseline names; `review.tsv` defines the accepted image inventory. `SHA256SUMS` covers final retained evidence.

This is scoped Scale correction and concurrency acceptance, not TalkBack/RTL, OS-process-death, large-history performance or complete Tracks/app acceptance. Existing normalization tolerance, selectable values, schema 46, backup 26 and version 0.3.66/code 72 remain unchanged. Continue bulk selection/reorder, realistic larger histories, selected-condition definition changes, remaining native CSV provider recovery and whole-product coverage. Normal main/upstream delivery is recorded in Git history.
