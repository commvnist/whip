# CSV replacement and process recovery

Verified 2026-09-09 under IMP-20260909-028 / VER-20260909-029. Production baseline is `48204c0`; this chunk adds regression coverage and evidence. Existing behavior passed the reviewed journeys, so no further production change was justified. Complete Tracks and whole-app acceptance remain open.

The interrupted work was intact: Git object integrity passed, the expected test/memory edits remained, and the pending ordinary process driver had completed successfully. No unfinished Git operation or active test job required repair.

## Native journeys

Two new TrackCsvJourneyE2ETest methods exercise an invalid Number in the first of two CSV rows, verify zero partial writes and disabled Import, cancel native file replacement with Back, verify the original error survives, then select corrected bytes. Each continues through seven-type Entry review, text/unit remapping, Activity recreation, exact import, native export and full draft reparsing.

| Execution | Result |
| --- | --- |
| Initial API 34 new-method pair | `MvEnpT`: 2 fresh tests, zero failures/skips/reuse |
| API 26, 480×800 / 240 dpi | Both new methods pass; ordinary and actual 200% text |
| API 37, 2560×1800 / 320 dpi | Both new methods pass; ordinary and actual 200% text |
| Final full API 34 class, 1080×2400 / 420 dpi | `ig0Onn`: all 4 methods pass together, zero failures/skips/reuse |
| External API 34 process driver | Ordinary and 200% runs pass three actual process replacements each |
| `scripts/check --ready` | Affected readiness gate passes, including compilation, lint and debug packaging |
| Catalog lint and fixtures | Pass; 335 required states, no pending selectors/platform exceptions |

The final owning-class command was:

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android com.whip.app.TrackCsvJourneyE2ETest
```

API 26/37 ran the two new methods with direct `adb -s SERIAL shell am instrument -w -e class 'com.whip.app.TrackCsvJourneyE2ETest#invalidFileCanBeReplacedWithoutImportingPartialRows,com.whip.app.TrackCsvJourneyE2ETest#invalidFileCanBeReplacedWithoutImportingPartialRowsAtLargeText' commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner`, after installing both debug APKs. Terminal `OK (2 tests)` is retained in each log. The permanent Tracks profile already owns this class. Its preceding production-neighbor campaign remains VER-20260909-028; no production source changed here.

## Actual process boundaries

`process_csv_recovery.py` requires an explicitly verified disposable emulator and installed debug/test APKs. It seeds synthetic debug data using the exact ordinary method and `whipCsvSeedOnly=true`, ends instrumentation, then drives the real app and document picker externally:

```sh
python3 process_csv_recovery.py emulator-5554 OUTPUT_DIRECTORY
python3 process_csv_recovery.py emulator-5554 LARGE_OUTPUT_DIRECTORY 2.0
python3 verify_database_snapshots.py OUTPUT_DIRECTORY
```

Each run backgrounds the Activity, kills its process, proves its old PID absent, and focuses the same task with a different PID. An unchanged file restores the selected second Entry without writing rows. Changed file bytes produce the explicit Replace File error; a fresh selection shows the changed value before import. After successful import, the driver deletes only its named synthetic source while the process is dead. Recovery shows the already-completed receipt and duplicates nothing.

`proof.json`, `transitions.json` and stopped-task dumps record those boundaries. `complete-database-proof.json` additionally compares every column in the before/after Entry, value and receipt tables: two Entries, all 14 values and one receipt are identical, and SQLite integrity checks pass. This supplements the driver's narrower value projection. Raw databases remain local; retained data is synthetic. The seven-type interpretation/export guarantee is also checked by the permanent native tests.

## Rendered review and limits

All 30 original PNGs were personally inspected: 18 platform invalid/cancelled/replacement frames and 12 process frames. `review.tsv` gives individual observations and limits. Both cancelled-picker images intentionally preserve the preceding invalid state. Fixed actions remain reachable; compact/enlarged bodies scroll. A clipped item at a scroll boundary does not establish lost content. Native trees have zero NAF nodes; that narrow check does not establish TalkBack behavior.

This verifies the exercised local document-provider boundaries. It does not accept revoked grants or unavailable sources before commit, provider outages, realistic large-history performance, remaining Field/destructive behavior, RTL/TalkBack, or the whole product. No physical-phone operation or release occurred. At most two disposable emulators ran. Process drivers restored text scale in `finally`, and the surviving API 37 device was checked at 1.0. API 34 later exited after the complete final test result was saved; no active test or missing result remained. No kernel OOM/crash report established its exit cause.

`source-and-apk-sha256.json`, fresh execution XML/aggregates and retained logs bind the checkpoint. Source inventory is 631 JVM / 1,021 Android methods and 43 actual-font methods; those counts are not execution claims. PNG bytes are original. `text-normalization.json` records any CRLF/trailing-whitespace normalization of text evidence. Verify retained files with `sha256sum --quiet -c SHA256SUMS` in this directory.
