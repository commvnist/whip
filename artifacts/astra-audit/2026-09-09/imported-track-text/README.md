# Imported Track text preservation

FB-20260908-006 · FND-20260909-025/026 · DEC-20260909-022 · IMP-20260909-024 · VER-20260909-025.

Baseline production is clean pushed `7166a40`. CSV parsing and public preparation/receipt transactions preserve values longer than the Entry editor's former input-only ceilings. Two real-app tests change one leading character, recreate, save and reopen the same Entry. Both baseline tests fail in `rTghLu`, zero errors/skips:

| Field | Imported characters | Characters after the baseline edit/save |
| --- | ---: | ---: |
| Short Text | 618 | 300 |
| Long Text | 6,478 | 5,000 |

The existing Entry identity, date and separate primary Name remain unchanged. The failing length assertions concern persisted values after reopening, not visual ellipsis. `baseline-tests.xml` retains the complete report. The final journey adds a supplementary-Unicode suffix after the first successful correction and verifies another recreation/save/reopen; this later phase cannot be reached on the failed baseline.

Production passes the complete proposed text to the existing Entry draft owner, preserving domain validation, explicit Save, exact mutation/CSV ownership, whitespace normalization and single-line versus multiline presentation. UI-only truncation is removed without imposing a new restriction on accepted history. Text lost by an earlier committed edit cannot be reconstructed automatically.

The import setup exercises the actual parser and public repository receipt path. The system file chooser and complete ViewModel/recovery flow remain separate coverage. Extreme-value saved-state capacity and large-history performance remain open under the whole Tracks audit; this change does not establish unlimited text capacity.

Final ordinary-text journeys pass 2/2 on disposable API 26 small (480×800, 240 dpi), API 34 phone (1080×2400, 420 dpi; `XpeTMf`) and API 37 wide (2560×1800, 320 dpi; `P4VCzw`). All preserve 618/6,478 characters after the first edit, then the complete additional space/compass suffix after another recreation/save/reopen. All runs have zero failures/errors/skips; modern capture campaigns have zero reused tests and pass catalog guards. No more than two emulators run concurrently. These are Activity-recreation and persistence checks, not an extreme-value process-death qualification.

The broader `coxxXf` campaign passes 64 JVM / 62 Android tests. A supplementary five-test mutation UI run (`2hAMSj`) fails only an obsolete create-catalog introduction assertion. Requiring the current Add Entry title and exact Track context preserves the underlying identity guarantee; the corrected complete suite passes 5/5 in `p4sYkm`. This produces 67 distinct accepted Android neighbors. Failed reports remain separate. The permanent Tracks profile now includes both imported-text journeys and selects 64 JVM / 60 Android tests.

`review.tsv` records all nine retained final originals: six real-app platform frames plus three isolated ordinary-text create/edit/delete fixtures. Every native hierarchy has zero NAF nodes. Modern real-app PNG/XML hashes and sizes match the retained complete manifests. The isolated fixture files come from the final five-test run: Android MediaStore kept earlier basenames and added ` (1)` to fresh Edit/Delete outputs. Those verified 16:40 files were pulled and named by surface ID locally; earlier 16:38 files are excluded. The isolated host's system-bar appearance is not actual-app theme acceptance.

Commands from `/root/repos/whip`, with the stated disposable emulator already booted:

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted tracks \
  --android com.whip.app.TrackImportedTextJourneyE2ETest \
  --android com.whip.app.TrackAuthoringJourneyE2ETest
ANDROID_SERIAL=emulator-5556 WHIP_ANDROID_TEST_SLOT=astra-imported-text-api26 \
  ./gradlew --no-configuration-cache :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.whip.app.TrackImportedTextJourneyE2ETest \
  -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
awk -F '\t' 'NR == 1 || $2 ~ /^tracks\.imported-text\./' \
  docs/quality/ui-surface-catalog.tsv > build/astra-imported-text-20260909/catalog.tsv
ANDROID_SERIAL=emulator-5554 \
  WHIP_UI_CATALOG_FILE=/root/repos/whip/build/astra-imported-text-20260909/catalog.tsv \
  scripts/ui-catalog capture --family tracks build/astra-imported-text-20260909/api34-final
ANDROID_SERIAL=emulator-5556 \
  WHIP_UI_CATALOG_FILE=/root/repos/whip/build/astra-imported-text-20260909/catalog.tsv \
  scripts/ui-catalog capture --family tracks build/astra-imported-text-20260909/api37-final
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android com.whip.app.TrackEntryMutationUiTest
scripts/check --ready
scripts/ui-catalog lint
```

`scripts/check --ready` passes 344 fresh JVM tests in 34 suites, both check-wrapper fixtures, Android-test compilation, lint, debug packaging, asset/destructive-migration guards and whitespace checks. `ready-jvm-suites.tsv` records exact counts/timestamps. Catalog lint and exact catalog/matrix matching pass 311 states. Source inventory is 630 JVM / 1002 Android tests (1,632) and 38 actual-font fixtures. `SHA256SUMS` covers every retained evidence file except itself. No schema, data epoch, backup format, version, release or physical-device operation changes.
