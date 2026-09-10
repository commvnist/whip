# Track selection scope and bulk actions

2026-09-10 · FND/DEC/IMP/VER-20260910-002 · baseline `b1749fbeb0a2a88334347971d2a65a53dda3b2e5`.

Changing Area or switching between active and Archived Tracks used to retain invisible selections. The page could report one selected Track and enable Unpin/Archive while every visible checkbox was clear. AllTracksPage now derives its count, pin intent and mutation IDs from selected Tracks in the current collection. It prunes hidden IDs after a successful load, preserves visible choices and recreation, disables actions while loading/failed, and explains the selection scope.

The native journeys select across Areas, narrow and broaden the view, switch archive state, recreate the Activity, pin/unpin/archive/restore and reopen. They verify exact retained Entries, Fields, Options, Area membership, positions and unrelated projections. This is collection-selection acceptance; actual Track drag/reorder and asynchronous bulk-failure recovery remain open.

## Execution and accounting

| Evidence | Result |
| --- | --- |
| `baseline.log` / MwrEBO | Three stale-selection reproductions; the fourth, ordinary Area fixture, fails on an ambiguous popup/row selector before reproduction. Three original frames are retained. |
| `correction.log` / hNgGBV | Four corrected API 34 native journeys pass, zero failures/skips/reuse. |
| `api26.log`, initial API 37 logs | Initial complete journeys pass four on API 26 and two per normal/enlarged API 37 process. |
| `final-api26.log` | Wrapper refuses API 26 before execution; its supported minimum is API 34. This is not a native test result. |
| `neighbors.log` | 64 + 54 tests execute without reported test failures, but test-source drift during the capture refinement invalidates the campaign. Discarded as acceptance. |
| `neighbors-final.log` / kZiFDt | Frozen source passes 118 fresh Android tests in two batches (64/54), zero failures/skips/reuse, including all four final collection journeys and neighboring Track/Task contracts. |
| `api26-final.log` | Four final methods pass in 37.696 seconds. |
| `api37-final-ordinary.log`, `api37-final-large.log` | Two final methods pass in each fresh normal/enlarged app process. |
| `ready.log`, `readiness-jvm/` | 346 fresh JVM tests in 34 suites, zero failures/errors/skips; compilation, lint and debug packaging pass. Tested source/APK hashes remain identical afterward. |
| Harness/catalog logs | Fast-check and UI-catalog fixtures pass; catalog lint accepts 383 states, zero exceptions/pending selectors. |

Commands from the repository root:

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android com.whip.app.TrackCollectionJourneyE2ETest --emulator
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile tracks --android com.whip.app.TaskReorderJourneyE2ETest --emulator
adb -s emulator-5556 shell am instrument -w -r -e class com.whip.app.TrackCollectionJourneyE2ETest commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner
scripts/check --ready
scripts/test-check-fast
scripts/test-ui-catalog
scripts/ui-catalog lint
```

Direct platform runs follow explicit installation of both debug APKs. API 37 uses separate selectors/processes for the two ordinary methods and the two `AtLargeText` methods, sets system font scale to 1.0/2.0 before launch, and restores 1.0 afterward. A terminal `OK (N tests)` is required. At most two disposable emulators run together: API 34 stays live while API 37 and API 26 alternate on the other port after confirmed shutdown. No physical-phone operation or release/publication occurs.

## Visual evidence and scope

`review.tsv` records 36 personally inspected originals: 30 final platform frames, three before frames and three capture diagnostics. At ordinary text the count, explanation and action hierarchy remain clear. In the smallest enlarged viewport the panel scrolls; count, actions and rows cannot all appear at once. Final captures separately expose the count and complete Unpin label. PNGs are unchanged. Native selection-summary TextLayoutResult checks confirm actual 2.0 font scale; XML without NAF nodes is supporting evidence, not a TalkBack audit.

Retained PNGs pass chunk CRC, decompression and terminal-chunk checks; XMLs parse. Logs/XML use normalized line endings, trailing physical whitespace and final newlines, with original/retained hashes in `text-normalization.json`. `source-and-apk-sha256.json`, native XML/aggregates and `SHA256SUMS` provide provenance. Equal normal-scale summary/action frames are intentional: the supplemental action capture is needed primarily for the smallest enlarged viewport.

Recovery checks found clean local/live upstream `b1749fb`, no unfinished Git operation or stash, no missing Git objects and matching checksums for all prior Scale-history evidence. An unreachable September 1 stash commit predates this interruption and is preserved. No source work was pending when this chunk began.

Version 0.3.66/code 72, schema 46 and backup 26 remain unchanged. Source inventory is 632 JVM + 1,041 Android = 1,673 tests; 383 catalog states and 53 actual-font methods are inventories, not whole-source execution. Whole Tracks/app acceptance remains open.

The owner's newer FB-20260910-001/002 prioritizes 100% UX/UI, aesthetics, consistency and bugs. After this commit, pursue shared item builders for recurring layouts and natural responsiveness; do not continue per-screen enlarged-layout polishing as the dominant workstream.
