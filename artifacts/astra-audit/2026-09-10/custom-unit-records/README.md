# Custom Unit record hierarchy

FND-20260910-034; DEC/IMP/VER-20260910-024. Normal 100% experience on API 34 phone (1080×2400, 420 dpi) and API 37 wide (2560×1800, 320 dpi). Baseline production: `056fea9c`; final source is the owning Git commit.

Custom Units now use the shared record builder: identity/actions above complete dimension, archive state and conversion detail. Stable keyed Settings items supply consistent gaps. Feature-owned editors, conversion versions, persistence receipts and recorded meaning remain unchanged. Shared command availability preserves the asynchronous save guard.

| View | Before | After |
| --- | --- | --- |
| Phone collection | [Original](before-phone/settings.units.collection.png) | [Original](final-phone/settings.units.collection.png) |
| Wide collection | [Original](before-wide/settings.units.collection.png) | [Original](final-wide/settings.units.collection.png) |
| Conversion version | [Original](before-phone/settings.units.version.png) | [Original](final-phone/settings.units.version.png) |

The complete native journey creates a unit, recreates its draft, records two original bundles, renames through recreation, creates a new conversion version, archives/restores it and recreates the result. Full measurement definitions and entries remain equal; the original unit retains its conversion and identity while the new version receives a separate identity. The shared regression also checks disabled individual commands, closure when all commands become unavailable, recovery and single dispatch.

- Baseline: `79snEi` passes one phone journey; matching wide passes one in 33.197 seconds.
- Pilot: `mlQ1kw` passes five phone tests.
- Final phone: `kXMOMt` passes 111 fresh Android tests in batches of 74 and 37, zero failures/errors/skips/reuse. Command: `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted settings --android com.whip.app.ui.WhipRecordItemTest --android com.whip.app.RecordItemBuilderJourneyE2ETest --android com.whip.app.TrackInsightsJourneyE2ETest --emulator`.
- Final wide: direct native runner passes CustomUnitJourneyE2ETest, WhipRecordItemTest and RecordItemBuilderJourneyE2ETest: six methods in 84.476 seconds. A repeat of the unit journey passes in 31.885 seconds to replace one incomplete image export.
- `scripts/check --ready` passes 385 JVM tests in 44 suites, zero failures/errors/skips, Android compilation, lint and debug packaging. Final build stage: 2m40s. Catalog lint and fixtures pass: 521 captures, no pending selectors/platform exceptions.
- All 516 recorded production/test/harness/build/APK inputs match across final native runs and readiness. Both emulators finish at font scale 1.0.

All 24 retained, valid PNG originals were personally inspected. `review.tsv` records each scope: 10 accepted baseline frames, 12 final frames and two rejected baseline active-menu frames captured before their popup rendered. The final fixture explicitly awaits its menu choices. One first-run final-wide version PNG contained only a 61-byte header despite the passing native method; its original bytes are retained in `checks/truncated-wide-version.png.gz`. The accepted version pair comes from the passing repeat, with unchanged source/APKs. PNG chunk CRC, complete IEND, compressed image data and all retained XML hierarchies are checked; no hierarchy contains an unnamed interactive node.

PNG bytes are original. XML formatting is whitespace-normalized, with exact bytes retained under `raw`; `manifest.json` records both hashes and original export names. Compressed check logs include the initial fixture compilation failure (nonexistent Number enum, corrected to Integer), baseline, pilot, final and replacement runs. These are scoped acceptance results, not a whole-product candidate. No real TalkBack, process-death, separate enlarged-text campaign, physical-phone or release acceptance is claimed. Broader Settings, integrations and whole-app review remain active.
