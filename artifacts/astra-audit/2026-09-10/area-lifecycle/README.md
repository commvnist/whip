# Area lifecycle and shared search evidence — 2026-09-10

Verified scope: normal-scale Area organization and recovery, shared Area/Tag search, and truthful saved-color presentation. Related FND-20260910-021/022, DEC/IMP/VER-20260910-014 and FB-20260910-001/002. Starting revision: `cafa61e56226c0ef93668c439c9a29ff0b82f44a`.

## Changes and native journeys

`WhipSearchField` owns the common label, single-line query and accessible Clear Search action. The Area manager and picker keep entered text visible when their option count falls below the normal search threshold. Tags use the same control. Features retain filtering, limits, saved state and transaction ownership.

When a proposed Area name matches an existing identity, the color field shows its actual Saved Color with choosing disabled. Changing back to a new name restores the private new-Area color draft. Restoration retains the existing ID and saved color under DEC-20260902-008.

Three new real MainActivity/repository journeys cover:

- Last-active-Area guard → create with Blue → draft recreation → saved accessible reorder → Violet color draft recreation → archive → Undo → archive again → archived disclosure → restore through the existing-name path → reopen the same identity.
- Four assigned domain items and their history → reviewed move → recreated destination → merge into Main → exact reference/history and saved-scope reconciliation → nine-to-eight search transition → recreation → Clear Search → reopen.
- Cancel cleanup without mutation → choose and recreate the keep-items destination → delete the source while preserving all assigned history → reopen the destination → recreate destructive review → delete the assigned entities and dependent history → reopen with the unrelated Main Task intact.

The history assertions compare Task completion, complete Habit logs, Goal measurements, Track Entries and typed values. Four top-level items means one Task, Habit, Goal and Track; the Track Entry is separately reported history. The controlled picker case changes nine options to eight and verifies query visibility, clearing and selection. Existing native Tag and Area-rename journeys also rerun.

## Accepted verification

| Cohort | Result | Provenance |
| --- | --- | --- |
| Final-production API 34 regressions | 86 fresh passes, zero failures/skips/reuse, two batches | `regression-api34.log`, `native/FM2S6X/` |
| Final-fixture API 34 focus | 8 fresh passes, zero failures/skips/reuse, two batches | `final-api34.log`, `native/YQAkIy/` |
| Final-fixture API 37 focus | 8 passes, terminal `OK (8 tests)`, 126.999 seconds | `final-api37.log` |
| Final affected-change readiness | 371 JVM tests in 41 suites, zero failures/errors/skips; build, lint and static/harness checks pass | `readiness.log`, `readiness-jvm/` |
| Catalog | 461 capture states, zero pending/exceptions | `catalog.log` |

API 34 uses disposable `emulator-5554`, 1080×2400 / 420 dpi / font scale 1.0. API 37 uses disposable `emulator-5556`, 2560×1800 / 320 dpi / font scale 1.0. Only those two emulators were used. Final source, scripts, tests and APKs were frozen during each live native campaign.

`source-and-apk-sha256.json` records 500 final source/script/APK hashes, all rechecked after readiness with no drift. The preceding 86-test production regression has its own `regression-source-and-apk-sha256.json`. `final-fixture-delta.json` proves that only two Android fixture files and the test APK changed afterward: the merge popup now waits for its actual menu, and the controlled picker host has safe drawing insets. Every production/script file and the application APK match that 86-test run exactly. The final eight-test cohorts verify the stronger fixtures; the 86 tests were not rerun against the final test APK.

The API 34 regression selects AreaOrganizationJourneyE2ETest, OrganizationJourneyE2ETest, AreaFeatureUiTest, TagManagementUiTest, AreaMutationViewModelTest, TagMutationViewModelTest, MeasurementTaxonomyRepositoryTest, DomainDeletionCoordinatorTest, SettingItemBuilderJourneyE2ETest, and InteractionControlUiTest#sharedColorPickerSupportsPresetsAndExactCustomColors via `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android … --emulator`.

Final API 34 focus selects both journey classes, AreaFeatureUiTest#areaPickerKeepsActiveSearchWhenItsOptionsShrink, AreaFeatureUiTest#captureAreaManagementCatalog and the same color-control method. Final API 37 instrumentation selects both journey classes, the picker-shrink case, AreaFeatureUiTest#areaWorkspaceKeepsExitTrailingAndBackLeadingAtTwoHundredPercentText and TagManagementUiTest#narrowLargeTextHeaderStacksWithoutHidingPrimaryActions. These last two are existing controlled font regressions, not a new global-scale campaign. Final readiness command: `scripts/check --ready`.

## Visual evidence and rejected checkpoints

All 48 retained original PNGs were individually inspected: two before states, 24 final phone states and 22 final wide states. Each has its original paired native XML and an observation in `review.tsv`. Manifests retain the device filename and original hash. PNG bytes are unchanged; CRCs, compressed data and XML parsing pass. Text-only line-ending/trailing-whitespace normalization is recorded in `text-normalization.json`.

- `baseline-3/organization.lifecycle.search-after-merge.png` reproduces the invisible active query after a real nine-to-eight merge.
- `checkpoint-final-api37/organization.lifecycle.restore-existing.png` shows the old editable Default color while restoration keeps saved Violet.
- `final-api34/` and `final-api37/` show the corrected query/clear action and actual read-only Violet, with the complete ordinary creation, move, merge, archive and cleanup states. The phone folder also retains the original Area menu and rename dialog.

`baseline.log` records three fixture mistakes (five versus four items and Purple versus Violet). `baseline-2.log` passes creation/recovery and cleanup but encounters duplicate Move 4 Items labels; dialog scoping corrects the fixture. `baseline-3.log` then fails the intended search assertion. The 33-method pilot and initial 86-test phone/eight-test wide/readiness runs are checkpoints before the saved-color correction. Their original reorder and archive captures were taken before the intended state settled and were rejected as acceptance; saved-order/idle and visible-dialog assertions correct capture timing. `wide-popup-checkpoint.log` later passes seven of eight methods but looks up Area Main before its popup exists. Its failure is retained separately from the accepted final rerun.

## Limits and continuing audit

Activity recreation and the accessible reorder action were exercised; actual OS process death, the drag gesture, TalkBack operation and API 26 were not newly tested in this increment. Controlled picker captures establish the component transition, not acceptance of every production picker host. The broad Area section stays open for shared reading width, danger-panel emphasis and global picker review. Other product areas and final whole-app gates remain outstanding.

Source inventory is 646 JVM + 1,071 Android = 1,717 methods, not an executed full-suite claim. No schema, backup, data epoch, app version, release or physical-device change occurred. `SHA256SUMS` covers the retained evidence.
