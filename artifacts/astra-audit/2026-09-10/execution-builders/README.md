# Execution item builder — 2026-09-10

FND-20260910-008/009 and DEC/IMP/VER-20260910-007 under the continuing whole-app audit and FB-20260910-001/002. Starting source: `b36dc0d97db8a4bd567ffdbf6f7b9a84a27a9d47`.

`WhipExecutionItem` owns the identity, values, outcome, target, supporting evidence and specialized-input reading order for all four Set renderers. It preserves active/passive emphasis and existing geometry. Gym code owns exact actions, reorder, draft lifecycle, saved facts and one outcome formatter. Omitted optional Sets now keep Optional identity and say Skipped when actually skipped. Add Exercise and Arrange carry their accessible names on the buttons, preserving names during partial occlusion beneath the sticky execution panel.

## Acceptance

- Final API 34 `0IRv0l`: 177 fresh Android tests in one accepted batch, zero failures/skips/reuse, 4m45 Gradle duration. Includes full Gym/5/3/1 profiles, earlier-Set correction, full workout completion, catalog and the new native Set journey.
- Final API 37: four selected methods pass in 53.223 seconds on the same source/APKs: native journey, passive Set geometry, historical Set geometry and Gym page catalog.
- New journey: persisted skipped optional Set → Activity recreation → exact Undo → enter 45 kg / 6 reps → wait for applied input → recreation → controlled 4dp toolbar exposure → Complete Set → Finish → matching History. Set UUID, unrelated completed Set equality and persisted values are asserted. The strict catalog unlabeled-node guard is unchanged.
- `scripts/check --ready`: 346 fresh JVM tests in 34 suites, zero failures/errors/skips, compilation/lint/debug packaging pass in 2m25.
- Catalog lint and fixtures pass: 394 required states, zero exceptions/pending selectors. Source inventory is 632 JVM + 1,050 Android = 1,682; this is not a full-suite execution claim.
- Fourteen native original PNGs personally inspected: three baseline, five final on each API 34/37 and one controlled pre-fix clipping frame. PNG signature/chunks/CRC/decompression/IEND and XML parsing pass. Both emulator system font settings finish at 1.0. NAF counts are diagnostic evidence, not a TalkBack acceptance claim.

## Failed checkpoints and evidence provenance

Baseline `xZ3iT7` passes the existing Gym catalog. The first fixture `mxvjnV` incorrectly uses Main work with Skipped and is rejected by the domain before launch. Correct optional work in `hAAVFj` reproduces the original product bug: Set 1 says Removed and loses Optional identity.

Pilot `TrbzfZ` passes 36/38: the native fixture wrongly assumes a Finish confirmation even when all work is complete, and an existing Machine test misses its chooser after scroll. `GebbE2` passes the Machine replacement and both geometry checks; the native journey reaches correct History values then fails an incomplete status assertion (Performed versus Working · Performed). The initial API 37 run also encounters duplicate Routines labels in sidebar and page. Final fixtures use conditional confirmation, complete status and the actual page-title tag; no Machine or navigation production fix is claimed.

`DXG9Gg` finishes 176/177, failing only the restored-state unlabeled-node guard. `lNQzyv` passes the full journey at a different scroll position. `Z2YYNQ` cannot expose the toolbar at the short-content scroll limit and does not prove clipping. Probes after Undo (`dTLFNG`/`qGzPSs`) reproduce two visible interactive nodes with no available names. `clipped-node-evidence.txt` identifies Add Exercise and Arrange. Their names now live on the full button semantics instead of depending solely on child Text visibility. Final isolated `fnN5ou` and full phone/wide campaigns pass with asserted 4dp exposure; temporary tracing is removed.

Successful capture manifests identify original device filenames and hashes. An earlier rejected hierarchy left an extra restored PNG on API 34, so MediaStore's PNG/XML suffix counters differ. `capture-pair-provenance.json` verifies that the final restored PNG and XML were written in adjacent seconds; the final hierarchy contains the named full-button descendants at the clipped controls. Matching suffixes alone are not relied upon for this pair. The failed controlled frame retains its original PNG and exact node attributes only: no matching XML survived the guard, and an older XML is not substituted.

## Reproduction

Final phone campaign (`accepted-neighbors.log`):

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted \
  --profile gym --profile gym531 \
  --android com.whip.app.FirstClassWorkflowE2ETest#editingAnEarlierSetDoesNotInvalidateTheActiveSetComposer \
  --android com.whip.app.FirstClassWorkflowE2ETest#workoutCanBeCreatedLoggedFinishedAndFoundInHistoryThroughTheRealUi \
  --android com.whip.app.VisualCatalogPagesTest#captureGymPageCatalog --emulator
```

The same final debug/test APKs are installed explicitly on `emulator-5556`, followed by:

```sh
adb -s emulator-5556 shell am instrument -w -r -e class 'com.whip.app.ExecutionItemBuilderJourneyE2ETest,com.whip.app.GymPowerInputUiTest#passiveWorkoutSetCardSeparatesIdentityValuesStatusAndTargetAtLargeText,com.whip.app.GymPowerInputUiTest#historicalSetCardSharesActiveHierarchyAndInsetsAtLargeText,com.whip.app.VisualCatalogPagesTest#captureGymPageCatalog' commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner
scripts/check --ready
scripts/ui-catalog lint
scripts/test-ui-catalog
```

`source-and-apk-sha256.json` identifies the tested source, tests, profile and APKs. Native campaign XML/TSV/TXT and readiness XML are retained. PNGs are untouched; text-only newline/trailing-space normalization and hashes are recorded in `text-normalization.json`. `SHA256SUMS` covers the bundle except itself.

## Limits

Normal 100% experience is the priority. Existing large-text geometry methods remain proportionate shared regression coverage. Initial viewports do not establish all lower content or every Gym editor. Whole-app visual/journey review and final complete-product gates remain open. No app version, schema, backup or data change; no physical-device, release, publication or delegation operation occurred. Store-candidate guidance printed by scripts does not mean a candidate was created.
