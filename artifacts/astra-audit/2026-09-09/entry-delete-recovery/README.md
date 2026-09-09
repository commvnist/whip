# Entry deletion recovery and complete review reading

FND-20260909-028 / DEC-20260909-024 / IMP-20260909-026 / VER-20260909-027. Baseline: clean pushed `ee0cbc7`. This verifies one coherent deletion/recovery flow within the active whole-product goal; complete Tracks and app acceptance remain open.

## What improved

The old row-deletion review saved a complete edit snapshot, including hidden long values. After OS process death, a fresh preparation silently replaced the original revision. A visually unchanged confirmation could consequently delete a newer note the user had never reviewed. The strengthened baseline collected 1,231,024 Activity-state bytes for an accepted 1,200,008-character note.

The final review saves bounded presentation plus the exact original Entry/Track identity and canonical revision tokens. Editor-only contract arrays and full values stay out of review state. Recovery cannot replace that authority. Conflicts offer current same-identity history through Review Entry; a missing target can close, while transient failures may retry the original request boundary. Display excerpts preserve Unicode code points and do not alter stored content.

The small 200%-text review also exposed an unscrollable Undo explanation. The final heading/message share a scrolling body above fixed actions, and a new conflict resets reading to its heading. See [initial large review](api26/tracks.entry-delete.review.large.png), [complete Undo reading](api26/tracks.entry-delete.undo.large.png) and [conflict](api26/tracks.entry-delete.conflict.large.png).

## Final evidence

| Check | Exact result |
| --- | --- |
| Tracks plus recovery/mutation/definition/permanent-deletion neighbors, `LsPjz1` | 65 JVM checks and 94 fresh Android tests, two batches, zero failures/errors/skips/reuse |
| Canonical API 34 capture, `qYA3Sq` | Two owners, six states accepted; retained manifest/catalog and original PNG/XML pairs |
| API 26 focused state/recovery/repository union | 19 tests passed; seven settled ordinary/actual-200% PNG/XML pairs |
| API 37 same union on final source | 19 tests passed; six fresh ordinary/actual-200% PNG/XML pairs |
| Real API 37 process replacement | PID 4068 absent before PID 4252 restores task 57; newer note protected, current inspection reached, fresh Delete/Undo restores the same Entry UUID and note |
| Readiness | `scripts/check --ready` passes 345 JVM tests in 34 suites, both wrapper fixtures, Android compilation, lint, debug packaging and static guards |
| Source inventory | 631 JVM + 1,016 Android = 1,647 methods; 321 exact catalog/matrix states; 41 actual-font methods. Inventory is not full-suite execution. |
| Retained visual review | 47 individually reviewed originals, including 24 final frames; all 24 final native hierarchies have zero NAF nodes |

Final Activity-state measurements are collected without attempting oversized baseline Binder transport:

| Fixture | API 26 | API 34 | API 37 |
| --- | ---: | ---: | ---: |
| 1,200,008-character row deletion review | 27,120 B | 28,584 B | 29,816 B |
| Three completed 600,008-character edits | 25,332 B | 26,760 B | 27,988 B |
| Open 600,008-character editor | 27,024 B | 28,688 B | 29,920 B |

`logs/final-scroll-state-api*.log` retain diagnostic histories; the last three measurements per platform are the final run. The test guard is 128 KiB. This is a measured transport-size correction and a separately proven small-note OS recovery journey, not an induced Binder-crash demonstration.

## Reproduction

Use explicit disposable emulators only, with at most two live. API 34 used `emulator-5554` (1080×2400, density 420). API 26 small (480×800, density 240) and API 37 wide (2560×1800, density 320) used `emulator-5556` sequentially. Font rules restore baseline 1.0 after actual 200% runs. Keep emulator identities stable during Gradle instrumentation.

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile tracks \
  --android com.whip.app.RecoveryBoundaryIntegrationTest \
  --android com.whip.app.TrackEntryMutationUiTest \
  --android com.whip.app.TrackDefinitionMutationUiTest \
  --android com.whip.app.TrackDeletionUiTest

scripts/check --ready

# Install both current debug APKs on the explicitly guarded disposable target.
adb -s emulator-5556 shell am instrument -w -r \
  -e class com.whip.app.TrackDraftSavedStateJourneyTest,com.whip.app.ui.TrackEntryDeleteRecoveryUiTest,com.whip.app.TrackEntryIntegrityTest \
  commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner

python3 artifacts/astra-audit/2026-09-09/entry-delete-recovery/process_delete_review.py \
  emulator-5556 build/entry-delete-process-recheck
```

The driver refuses a non-emulator identity, seeds synthetic data through the opt-in real CSV fixture, opens the normal app, backgrounds/kills its process, proves absence, changes only a hidden note in the disposable database, restores the retained task and follows the review/conflict/inspection/fresh deletion/Undo flow. It records the restored UUID and note in `proof.json`. The controlled mutation isolates revision ownership; it does not represent a production background writer. Repository tests separately assert exact six-type drafts and UUID after Delete/Undo with the compact boundary.

## Evidence map and limits

- `api26/`, `api34/`, `api37/`: final original captures. API 34 includes the canonical manifest/catalog; API 37 `capture-files.json` records the fresh MediaStore source names/timestamps, including duplicate suffixes. API 26 uses the settled legacy private-file capture path. `undo.large` is a supplemental scroll position, not a seventh catalog state.
- `process-api37/`: five final normal-app frames, seed/launch logs, verified process absence, synthetic fixture metadata and exact proof. [Conflict](process-api37/after-confirm.png), [current note](process-api37/review-latest.png), [restored row](process-api37/undo.png).
- `review.tsv`: individual observations and explicit limits for every retained PNG. Small enlarged inspection is intentionally scrolled to the Note; it does not show every body item at once. Wide permanent UI fixtures explicitly supply Compact outer chrome; the normal-app process frames independently show the real wide navigation.
- `accessibility.json`: 24 final native trees and NAF counts. This is a narrow unlabeled-node check and complements interaction/actual-font assertions; no fresh TalkBack, RTL or rotation campaign is claimed.
- `reports/`: exact selected baseline/development and final Android XML plus 34 readiness JVM suites. `logs/` preserves commands' outcomes, including failures. Early files named `final-*` predate the last scrolling refinement; final-source authority is the `final-scroll-*` group plus `final-ready.log` and runs `LsPjz1`/`qYA3Sq`.
- No schema, portable backup, application-version, physical-phone or release changes. Full Field/CSV journeys, large-history performance, other saved-state owners and the complete cross-app matrix remain open.

## Preserved failures and superseded evidence

`Vtd0rK` fails the initial 600,008-character size check at 631,024 bytes. `x80ix7` strengthens it to 1,231,024 bytes. The baseline process driver (4240→4429) deletes the newer note; `UjHDe3` independently fails the permanent restored-owner regression with zero Entries. `baseline-process-api37/` retains the apparently normal but unsafe confirmation/result.

`zze70O` passes 19 tests after compact ownership, and the first corrected process (4770→4953) protects history. Its generic save-failure copy is rejected in `development-process-api37/`. `Zdf5Tl` then passes both corrected conflict-to-inspection journeys. `development-api37/` and `development-process-flow-api37/` preserve the successful pre-scroll wide states and process 5573→5757, without treating them as final-source checks.

`4Z6D4I` accepts 65 tests, then terminates with host exit 143 after the next batch reports 27/29; it is incomplete. A concurrent emulator offline warning is recorded, but signal causation is unproven. `uGzMxm` fails three UI assertions after the interruption strands API 34 at font scale 2.0; resetting the disposable baseline permits `tfSVM9` to pass all 94. API 26 fixture navigation initially fails to scroll to its offscreen Track card; a subsequent run exposes two existing editor assertions racing the asynchronous input shield. Explicit scrolling and a wait for merged interactive editor semantics resolve those fixture defects; `4Yaz9K` passes all three state tests.

`ExuXa4` fails both strengthened Undo-reading assertions because the body has no scroll parent, matching the clipped large `development-api26/` original. The first scrolling compile misses two imports and executes no Android tests. Corrected final `qYA3Sq`, `LsPjz1`, both final 19-test platform runs and the final process journey pass. Failures remain visible as diagnostic evidence and do not count toward accepted execution.

`SHA256SUMS` binds the retained evidence; verify from this directory with `sha256sum --quiet -c SHA256SUMS`.
