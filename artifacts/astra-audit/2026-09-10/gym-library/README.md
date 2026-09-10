# Gym Library: shared records and complete equipment search

Exercises, Machines and Categories now share title/action composition and full-width supporting information. Long names wrap naturally. Category Archive/Restore stays direct, Machine commands retain destructive separation, and reorder mode suppresses ordinary actions. Shorter introductions focus on the task. Equipment search and Any Machine filtering include every linked Exercise; search exposes the shared Clear Search control.

The existing record family also serves Track Entries/Activity and Review outcomes. Features retain filters, ordering, forms, save requests and history. Complete Machine configuration uses detail lines; Track facts keep their three-line preview. See [the builder direction](../../../../docs/quality/WHIP_ITEM_BUILDERS_2026-09-10.md) and DEC/IMP/VER-20260910-015.

| Evidence | Result |
| --- | --- |
| Unchanged-production catalog, E3rz30 | 1 pass |
| Unchanged-production authoring/history, yEXbKg | 2 passes |
| Native/shared-renderer pilot, dXLiOC | 6 passes |
| Final-production API 34 regression, dXPrxX | 95 passes, zero failures/skips/reuse |
| Final-fixture API 34 focus, Gibyir | 12 passes, zero failures/skips/reuse |
| Final-fixture API 37 focus | 12 passes, terminal `OK (12 tests)`, 156.343 seconds |
| Final readiness | 371 JVM tests in 41 suites, zero failures/errors/skips; build, lint and static/harness checks pass |
| Original visual review | 45 PNG/XML pairs: 4 before, 18 final phone, 18 final wide, 5 phone catalog |

Native journeys cover query/filter/recreation/clear recovery; Exercise and Category editing/reordering; Category archive/restoration and saved links; and Machine editing/archive/restoration/new-version/reopening with exact completed Session, placement and Set snapshots. The new-version journey changes the name and retains copied resistance settings. Broader load rules retain GymRepositoryTest coverage. Neighboring Track/Review journeys verify original source actions, archive behavior and saved facts.

The disposable phone uses API 34, 1080×2400, 420 dpi; the wide emulator uses API 37, 2560×1800, 320 dpi. Both use font scale 1.0. No physical device or release operation occurred.

The 95-test run selects `GymLibraryJourneyE2ETest`, `ui.WhipRecordItemTest`, `GymRepositoryTest`, `EditorStateRecreationTest`, `ui.EditorDependencyUxTest`, `RecordItemBuilderJourneyE2ETest`, `ReviewOutcomeJourneyE2ETest` and `VisualCatalogPagesTest#captureGymPageCatalog` through `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android … --emulator`. The final twelve-test selections are the Library journey, record component, Track record journey and Review outcome journey classes. Wide runs the same APKs with `adb -s emulator-5556 shell am instrument -w -r -e class … commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner`. Readiness uses `scripts/check --ready`; `scripts/ui-catalog lint` accepts 475 captures with no pending selectors. Exact terminal logs and native/JVM XML are retained here.

`source-and-apk-sha256.json` retains 505 final input hashes. Compared with the 95-test regression, only the Library fixture, its test APK and the JVM architecture fixture changed. The Library fixture disambiguates master/detail names using catalog Edit actions. The JVM fixture checks the shared Machine record renderer instead of requiring removed local-card syntax. Native, regression and final hash snapshots plus `final-fixture-delta.json` preserve these boundaries. Every production/script file and the application APK match all final native campaigns. All campaigns are terminal.

`review.tsv` records individual observations. Manifests preserve device filenames and original hashes; PNG bytes are unchanged. PNG CRC/decompression, paired XML parsing and final input hashes pass. Text-only newline/trailing-whitespace normalization is recorded separately. The five catalog frames belong to the 95-test campaign; focused journeys do not recapture those IDs.

The failed baseline chhFwk reproduces the missing secondary linked Exercise. Separate fixture corrections address an invalid enum, unordered Category links and pending rest-service cleanup before history capture. A wrong declared package is rejected before instrumentation. The first wide run passes ten methods and encounters two ambiguous selectors; the final twelve pass after scoping. The first readiness run rejects obsolete local-card syntax; the updated contract passes. These checkpoints remain in the logs and are not final acceptance.

This is bounded catalog acceptance. Routine authoring/programmed journeys, the wider audit, master/detail duplication and shared reading-width policy remain open. Activity recreation is not OS process recovery; no new real-drag, TalkBack, RTL, API 26 or 200% campaign is claimed. Version 0.3.66/code 72, schema 46, data epoch 6 and backup 26 are unchanged.
