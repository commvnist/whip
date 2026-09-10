# Settings item builder — 2026-09-10

FND/DEC/IMP/VER-20260910-006 under the continuing whole-app audit and FB-20260910-001/002. Starting source: `0e83be9ad3671a52fd8ccf12e50f685df2e3a34b`.

Settings now declares toggle, choice or typed-edit roles through `WhipSettingItem`. One renderer owns label/control alignment, a complete current-value line, full-width explanation, natural height and a single action target. Existing Boolean callers adopt the same layout through `WhipSettingsRow`. Choice callbacks remain immediate; typed editor drafts, validation, source identity and durable Save coordinator remain unchanged. Ordinary navigation and specialized groups retain their separate roles.

## Acceptance

- API 34 `jKsH4S`: 147 fresh Android checks across three accepted batches (43/84/20); zero failures/skips/reuse. Includes builder contracts, Settings persistence/recovery and native navigation, plus Task, Track and Gym neighbors.
- API 37: five checks pass in 55.175 seconds: two builder contracts, native choice/draft/recreation/Save/reopen, Settings catalog and Gym category-allocation choice. The test navigation helper follows either the compact index or actual wide support pane.
- Readiness: `scripts/check --ready` passes 346 fresh JVM tests in 34 suites with zero failures/errors/skips, compilation, lint and debug packaging in 2m22.
- UI catalog lint/fixtures pass: 391 required states, zero exceptions/pending selectors. Current source inventory is 632 JVM + 1,049 Android = 1,681; this is an inventory, not a full-suite execution claim.
- Twenty-five original PNGs personally inspected: seven baseline Settings pages and nine final states on each API 34/37. `review.tsv` records viewport limits. PNG signature/chunks/CRC/decompression/IEND and XML parsing pass. Both emulator system font settings finish at 1.0. NAF counts are retained diagnostics, not TalkBack acceptance.

## Failed checkpoints and limits

`4irjMH` passes both new builder contracts and Settings catalog but the first native fixture exceeds the existing 40-character Area-name limit before launch. The sample is corrected to exactly 40 characters. `LHtQLy` accepts 43 shared checks, then finishes 102/103 second-batch passes: immediate Activity recreation races the entered text's applied Compose saveable state. `s0I8Ds` passes after asserting displayed draft 3 before recreation; temporary lifecycle tracing confirms restored draft 3, and is removed before final acceptance. FND-20260910-007 records this fixture synchronization issue. There is no claimed production draft-loss repair.

The first API 37 run passes both builder contracts but fails two native fixtures because they assume compact category tags. Final fixtures use the actual external support pane on wide layouts. Failed campaigns remain separate from final acceptance. Baseline `j22ywF` and pilot `38BvLf` precede the final 32dp label/control header and menu-close refinement; pilot evidence is not final-source acceptance.

Initial viewports do not establish all content below the fold or complete specialized feature acceptance. Existing large-text neighbors run as regression coverage; this increment prioritizes normal-scale hierarchy. Whole-app acceptance, execution-family investigation and final product-wide gates remain open. App version, schema, backup format and saved-value semantics are unchanged. No physical-device, release or publication operation occurred.

## Reproduction

Final phone campaign (`final-neighbors.log`):

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted \
  --android com.whip.app.ui.SettingItemBuilderUiTest \
  --android com.whip.app.SettingItemBuilderJourneyE2ETest \
  --android com.whip.app.VisualCatalogPagesTest#captureSettingsPageCatalog \
  --android com.whip.app.VisualCatalogPagesTest#captureGymCategoryAllocationCatalog \
  --android com.whip.app.ui.SettingsResponsiveUiTest \
  --android com.whip.app.ui.SettingsHealthControlUiTest \
  --android com.whip.app.SettingsBehaviorUiTest \
  --android com.whip.app.AppSettingsPersistenceTest \
  --android com.whip.app.ui.InteractionControlUiTest \
  --android com.whip.app.TaskBulkSelectionUiTest \
  --android com.whip.app.TrackWorkspaceUiTest \
  --android com.whip.app.GymPowerInputUiTest --emulator
```

The same final debug/test APKs are installed explicitly on `emulator-5556`, followed by:

```sh
adb -s emulator-5556 shell am instrument -w -r -e class 'com.whip.app.ui.SettingItemBuilderUiTest,com.whip.app.SettingItemBuilderJourneyE2ETest,com.whip.app.VisualCatalogPagesTest#captureSettingsPageCatalog,com.whip.app.VisualCatalogPagesTest#captureGymCategoryAllocationCatalog' commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner
scripts/check --ready
scripts/ui-catalog lint
scripts/test-ui-catalog
```

`source-and-apk-sha256.json` identifies the tested source/tests/helper/profile and APKs. Native campaign XML/TSV/TXT and readiness XML are retained. Capture manifests map retained originals to device filenames; PNGs are untouched. Text-only line-ending/trailing-space normalization and original/retained hashes are recorded in `text-normalization.json`. `SHA256SUMS` covers the complete bundle except itself. Logs may print historical or store-candidate guidance; no candidate or store release was requested or performed in this increment.
