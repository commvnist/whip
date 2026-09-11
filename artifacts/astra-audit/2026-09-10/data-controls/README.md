# Data controls and shared destructive sections — 2026-09-10

FND-20260910-036 / DEC, IMP and VER-20260910-027. Baseline: `7de94702205c5387ef210f855a35fae313c7ca63`. This is a bounded increment within the active whole-product audit.

Data & Privacy now uses the existing shared action list to pair each backup choice with its explanation. Plaintext disclosure remains explicit; CSV is described as spreadsheet tables rather than a restorable backup. The shared destructive section uses a neutral background with red heading/action text across Settings, Areas, Tracks and entity inspectors. Existing final confirmations and data operations are preserved. No schema, version or release change.

## Executed checks

- `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile settings --android com.whip.app.SettingsBehaviorUiTest --android com.whip.app.ui.SafetyChoiceUiTest --android com.whip.app.TrackDeletionUiTest --android com.whip.app.ActivityHistoryUiTest --android com.whip.app.EntityInspectorUiTest` — **140 fresh Android tests**, 72/68 batches, zero failures/skips/reuse (`Cjm4Bp`).
- API 37 wide, matching APKs, explicit `AndroidJUnitRunner` selection of `DataPrivacyJourneyE2ETest`, `TrackDeletionUiTest`, and `ActivityHistoryUiTest#inspectorMakesScheduledPausesEditableAndHistoricalSkipsUndoable` — **4 tests**, 89.660 seconds, zero failures. Install and runner logs retained.
- `scripts/check --ready` — **384 JVM tests / 44 suites**, zero failures/errors/skips; Android compilation, lint and debug build passed. Final build stage: 2m42s. Routed fast/full harness fixtures passed.
- `scripts/ui-catalog lint` and `scripts/test-ui-catalog` passed: **529 states**. Inventory: **652 JVM + 1074 Android = 1726**. All **514** frozen source/build/harness/APK hashes still match after verification.

The new native journey checks passphrase mismatch/correction, encrypted-export picker cancellation, actual document selection, backup preview, cancellation of Replace, real Merge, reset cancellation, Activity recreation and reopened Inbox. It compares complete original/imported Task records (allowing only the remapped database ID) and preserved current preferences. Existing regressions cover actual reset, backup/recovery, exact Track deletion and Area cleanup/history.

## Visual evidence and limitations

All **19 retained original images** were personally inspected; [review.tsv](review.tsv) records their precise scope. [manifest.json](manifest.json) records PNG and original XML hashes. Original XML is retained under `raw/`; readable normalized XML accompanies each image. PNG CRC/IEND/decompression, XML parsing and unnamed-interaction checks pass. Fixed-theme destructive text contrast against the new section is **5.78:1 light / 10.17:1 dark**.

`before-phone` contains the successful baseline run `G2sEKU`. `pilot-phone` contains six final-production images from the passing four-method pilot `9yi1sF`; production and its DataPrivacy journey did not change afterward. `final-wide` and `neighbors-phone` come from final checks. Phone is API 34, 1080×2400 at 420 dpi; wide is API 37, 2560×1800 at 320 dpi. Both use normal text scale. Habit inspector images are component fixtures, not native MainActivity journeys. The Area frame shows the destructive label with its lower button edge outside that scroll position; the journey scrolls to the action before invoking it. No complete button-bounds claim is made for that frame.

The first baseline (`4O9kW3`) passed the persisted-data checks but failed its final Task visibility assertion because undated Tasks belong in Inbox; the fixture now opens Inbox and the baseline passes. Initial retrieval accidentally selected earlier unsuffixed MediaStore filenames. Those were rejected as final evidence; the retained files were retrieved by their exact numbered names and recorded in the retrieval manifests. No production change was made to address either test/evidence issue.

This does not establish complete encryption-file delivery, portable-folder failure/process-recovery, API 26, dynamic-color or new 200% acceptance. The generic Settings outcome banner, visible behind the preview and partially scrolled in the wide cleanup state, remains a broader feedback-review item. The whole-product audit remains active. No physical device, release or store publication was used.
