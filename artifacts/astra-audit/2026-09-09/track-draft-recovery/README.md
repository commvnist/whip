# Track draft recovery

2026-09-09. FND-20260909-027, DEC-20260909-023, IMP-20260909-025, VER-20260909-026.
Baseline: clean pushed `0180efe`. This is one verified increment of the active whole-product goal; complete Tracks and app acceptance remain open.

## Result

An accepted 600,008-character CSV note previously put 1,233,552 bytes into the real MainActivity saved-state Bundle. Three completed edits left 1,830,080 bytes behind. Android documents a shared 1 MB Binder buffer; the failed baseline deliberately measured the Bundle without transporting it or claiming a reproduced crash. See [Android's transaction limit](https://developer.android.com/reference/android/os/TransactionTooLargeException) and [state persistence guidance](https://developer.android.com/topic/libraries/architecture/saving-states).

One Activity-owned Track session now keeps the exact opening route, definition/Entry draft, raw Number input and conflict boundaries together. Android saves a compact reference to a versioned, checksummed private checkpoint. Successful Save, discard and Activity finish clear the session; configuration/background destruction retains it. Reset and replace-restore invalidate the generation and remove all prior checkpoints under the same monitor used for checkpoint access. A stale owner cannot recreate those files.

| Final platform | Open large Entry | After three saved/closed editors |
| --- | ---: | ---: |
| API 26, 480×800, 240 dpi | 27,024 bytes | 25,324 bytes |
| API 34, 1080×2400, 420 dpi | 28,688 bytes | 26,760 bytes |
| API 37, 2560×1800, 320 dpi | 29,924 bytes | 27,988 bytes |

The permanent tests require the full Activity Bundle to remain below 128 KiB in these fixtures. They also reconstruct an independent session owner and compare the complete edited value and opening snapshot. This does not establish an app-wide aggregate limit for unrelated saved-state owners.

## Process and failure evidence

- `process-api34/`: final production path. The supplied emulator-only `process_recovery.py` seeds through the real CSV preflight/parser/public receipt import, opens the app normally, changes the Name, backgrounds it, verifies the old process is gone, focuses its retained task, saves the recovered draft, and checks SQLite identity/content independently. PID 26116 became 26385. The original Entry UUID and all 600,008 source characters survive; SHA-256 is `aa7e3dc9e922a2879405389d7aeac87e2db94b800ae0373233910ea3a6c5ee09`. No checkpoint files remain after Save. The temporary verification database is intentionally not versioned.
- `process-api37-development/`: earlier actual process recovery, write failure/retry and checksum failure. Saved Track/Field/Entry/value/CSV-receipt rows remain exactly equal after the failures. These frames exposed the generic save-error component's incorrect claim that an unrecovered draft was still available; final recovery-specific messages replace it. A too-early first kill only resumed the same process and is excluded. The subsequent successful kill used a retained Activity stack created by the manual launch commands; the final driver avoids those extra activities.
- `api26/`, `api34/`, `api37/`: the final four recovery-dialog states on each platform. These isolated fixtures verify actual dialog text scale and action behavior; their neutral Activity background does not establish full-app chrome acceptance. API 34 uses the canonical capture lane; API 37's fresh MediaStore ` (1)` files are identified by timestamp in `capture-files.json`. API 26 uses private files and settled-window waits because canonical MediaStore capture requires API 29+.
- `development-dialogs/`: retained intermediate originals. The two ordinary API 26 frames were captured during fades and are rejected as visual evidence. Final replacements are settled. Other intermediate frames have superseded copy. Every retained PNG has a scoped observation in `review.tsv`.

All 15 final native hierarchies have zero `NAF` nodes. The 12 final dialog originals and three final process frames were individually inspected: complete labels/messages and actions remain legible and reachable, with distinct live-draft versus unavailable-draft language. Pixel review cannot prove the off-screen note suffix; exact content assertions provide that evidence.

## Commands and results

Commands run from the repository root. Android targets were always explicit disposable emulators; no more than two ran concurrently. No physical-phone operation or release occurred.

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted \
  --profile tracks \
  --android com.whip.app.RecoveryBoundaryIntegrationTest \
  --android com.whip.app.TrackEntryMutationUiTest \
  --android com.whip.app.TrackDefinitionMutationUiTest
```

Final `4hDefb`: 64 JVM and 90 fresh Android tests, zero failures/errors/skips/reuse. Includes exact Track mutation/CSV neighbors and six recovery-boundary tests. The existing exclusive-reset test now also verifies private-file removal, rejection of a retained old draft, and quiet rejection of its old recovery reference.

Final API 26 direct instrumentation: 15 state/checkpoint/recovery-boundary tests plus two settled dialog tests, all passing. Final API 37 direct instrumentation: the same 17 tests pass. The earlier 13-test campaign on each API 26/34/37 additionally executed both complete ordinary/enlarged seven-type authoring journeys; it predates the final generation-cleanup refinement. Final generation and unchanged authoring evidence are distinguished in the logs.

```sh
adb -s emulator-5556 shell am instrument -w -r \
  -e class com.whip.app.TrackDraftSavedStateJourneyTest,com.whip.app.ui.TrackEditorCheckpointTest,com.whip.app.RecoveryBoundaryIntegrationTest,com.whip.app.TrackDraftRecoveryUiTest \
  commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner

ANDROID_SERIAL=emulator-5554 \
WHIP_UI_CATALOG_FILE=/root/repos/whip/build/astra-track-draft-state-20260909/recovery-catalog.tsv \
  scripts/ui-catalog capture --family tracks \
  /root/repos/whip/build/astra-track-draft-state-20260909/final-canonical-api34

python3 artifacts/astra-audit/2026-09-09/track-draft-recovery/process_recovery.py \
  emulator-5554 build/astra-track-draft-state-20260909/final-process-api34

scripts/check --ready
```

Final canonical `h1LKuY`: two owning tests, four exact states. Readiness: 344 JVM tests, Android-test compilation, lint, debug packaging, static guards and both check-wrapper fixtures pass. The current declared source inventory is 630 JVM / 1,013 Android methods, 315 catalog/matrix states and 40 actual-font methods, all inventoried. The prior index's 38-font count was stale; this chunk adds one font method to the previously existing 39.

Failed development evidence is retained: `Lcln5S` (one size failure), `qjNt50` (two size failures), the JaCoCo method-size build failure and one missing fixture constructor argument. Extracting the ViewModel lookup resolved the JaCoCo limit without disabling coverage. The corrected focused `JsK1BF`, neighboring `GtBLy6`, platform `4GiyDP` and intermediate canonical `sTxrVT` results remain separate from final acceptance.

Final staged review restored the existing FlowPreview compiler opt-in to WhipApplication after insertion of the checkpoint-directory constant had displaced it. Repeated `scripts/check --ready` passes in `logs/final-review-ready.log`, including the 344 JVM reports and a successful 2m 27s Android compile/lint/package build. Repeated catalog lint also passes. This annotation-only correction changes no runtime behavior; the earlier emulator evidence is retained without claiming another emulator run. `SHA256SUMS` covers every other curated evidence file and was verified before commit.

## Limits and continued review

Private checkpoints are transient recovery state, excluded from portable backups; no schema, application version or stored-history format changes. A missing/incompatible checkpoint cannot reconstruct unavailable authorship, and the UI reports that truthfully. Android must have saved state before process death for this recovery path to run.

Session files are removed on explicit completion/discard/finish and generation invalidation. Other Activity owners are not pruned because MainActivity allows multiple instances; abnormal task abandonment and long-lived checkpoint retention remain storage-review topics. Complete Field validation/replacement, CSV/file-chooser/recovery, row-delete saved-state ownership, realistic large-value/history performance, RTL/TalkBack and the remaining app matrix stay open. The 600 KB test's emulator execution time is not a measured user-interaction latency benchmark.
