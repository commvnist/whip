# Consistent organization headers and preserved history

FND-20260910-020 / DEC/IMP/VER-20260910-013. Baseline source: `8d0f8905afe7a6a226048de8eb16734890402de0`. Application remains 0.3.66/code 72, schema 46, data epoch 6 and backup format 26. No release or migration.

Areas and Tags now share `WhipManagementHeader`: one destination title, complete supporting text, optional Create/Back and trailing Close. The renderer owns typography, spacing, natural height and primary-action stacking. The two local header implementations and repeated Your Areas/Your Tags introductions are removed. Tag search is the first list control; Area count and reorder share one compact row. A selected Area correctly says **Move 1 Item**.

Features still own their records, navigation state, drafts, mutation coordinators and consequential child dialogs. This bounded contract improves normal 100% hierarchy without replacing transactional forms or importing domain behavior into a layout builder.

## Real application journeys

`OrganizationJourneyE2ETest` uses real repositories and MainActivity, entering through Settings → Organization.

- The Tag journey starts with one shared label referenced by a completed Task, logged Habit, measured Goal and Track. It creates a second label, globally renames the first, archives it, finds its archived result, restores the same identity, merges into the chosen destination and reopens the manager. Creation and rename drafts, archived search and the selected merge destination survive Activity recreation. Every stage checks exact references across all four domains; final Task completion and the entire retained Habit/measurement history must remain equal.
- The Area journey opens a populated root and selected Area, recreates an unsaved long rename draft, saves, returns through Back/Close and reopens the manager. The Task keeps its exact identity, title and Area ID while receiving the new Area name. This is bounded identity/rename/navigation acceptance; it does not exercise every Area lifecycle operation.

## Original visual evidence

`native-before/` retains three personally inspected original PNG/XML pairs from unchanged production: Tag root, Area root and renamed Area detail. The roots reproduce duplicate introductions before useful content; the detail shows the old singular caption. The complete corrected baseline journey passed before production changes.

`final-api34/` and `final-api37/` retain ten personally inspected original pairs each: Tag root, create draft, rename draft, archive impact, archived search, reviewed merge, surviving merged label, Area root, detail and renamed detail. Phone and wide frames use the production dark theme at font scale 1.0 (1080×2400/420 dpi and 2560×1800/320 dpi respectively). The final merged-label frame intentionally retains the search keyboard; the record and header remain usable. Long names and complete consequence copy remain readable. The Area detail body scrolls independently beneath its stable header.

`review.tsv` records individual observations and unlabeled-node counts. Manifests retain device filenames and hashes. All 23 PNGs are unchanged originals; PNG CRC/decompression, XML parsing and pair identity pass. `text-normalization.json` records text-only trailing-whitespace normalization. All 498 source/test/script/APK hashes in `source-and-apk-sha256.json` remain equal after the final campaigns and readiness checks.

Existing wide list reading width and the visually prominent Area Danger Zone remain broader design follow-ups. This chunk does not certify complete wide-layout aesthetics.

## Executed checks

| Run | Result |
| --- | --- |
| BUvOGZ, `catalog-before.log` | Both existing Area/Tag catalog owners pass on unchanged production. Area root/detail and Tag root originals were inspected. |
| lzFy7q, `native-before.log` | Tag journey passes. Area fixture incorrectly requests child text from its non-merged title container; no product lifecycle failure is claimed. |
| 1WCnaA, `native-before-2.log` | Both full new journeys pass on unchanged production after targeting the title descendant. |
| n6jJ6o, `pilot.log` | All 29 new journey and existing Area/Tag UI methods pass after the shared-header change. |
| 6SYr4F, `final-api34.log` | All 51 fresh Android checks pass with zero failures, errors, skips or reuse. Seven suites in one batch; terminal build in 2m 16s. |
| `final-api37.log` | Both native journeys and the two existing controlled large-text header contracts pass: four successful completions in 61.865 seconds, terminal instrumentation code -1. |
| `readiness.log` | 371 fresh JVM checks in 41 suites pass with zero failures/errors/skips. Compilation, lint, debug packaging and affected static/harness checks pass. This is affected-change readiness, not a frozen release candidate. |
| `catalog.log` | All 440 required catalog states have selectors; zero pending selectors or platform exceptions. |

Final commands:

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android com.whip.app.OrganizationJourneyE2ETest --android com.whip.app.ui.TagManagementUiTest --android com.whip.app.ui.AreaFeatureUiTest --android com.whip.app.ui.AreaMutationViewModelTest --android com.whip.app.ui.TagMutationViewModelTest --android com.whip.app.MeasurementTaxonomyRepositoryTest --android com.whip.app.SettingItemBuilderJourneyE2ETest --emulator

adb -s emulator-5556 install -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5556 shell am instrument -w -r -e class 'com.whip.app.OrganizationJourneyE2ETest,com.whip.app.ui.AreaFeatureUiTest#areaWorkspaceKeepsExitTrailingAndBackLeadingAtTwoHundredPercentText,com.whip.app.ui.TagManagementUiTest#narrowLargeTextHeaderStacksWithoutHidingPrimaryActions' commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner

scripts/check --ready
scripts/ui-catalog lint
git diff --check
```

Declared source inventory is 646 JVM + 1067 Android = 1713 product tests; this is not a full executed-suite claim. Ten native states expand the catalog and audit matrix to 440. Existing Area create/color/reorder/move/merge/archive/delete acceptance is not inferred from this header work. The existing focused Area deletion evidence remains separately scoped.

All campaigns are terminal. Whole-product review and final gates remain open. Activity recreation is not OS-process recovery proof; no new API 26 or real-user screen-reader validation occurred. Only the two disposable emulators were used. No physical-phone operation, release, publication, delegation or new per-screen 200% campaign occurred.
