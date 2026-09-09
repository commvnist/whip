# Track and Entry form hierarchy

FB-20260908-006 · FND-20260909-023/024 · DEC-20260909-021 · IMP-20260909-023 · VER-20260909-024.

The representative change puts Track Name and Entry Fields before optional Description/Emoji, replaces repeated Entry headlines/instructions with concise Track context, and centers each complete editor header/body in a column up to 720 dp wide. It preserves the full-screen destination, fixed responsive exit/commit, field order, all seven value types, organization, validation and existing draft/persistence ownership. Wide side margins are intentional; arbitrary Fields remain one ordered form.

Matched small API 26 originals show Track Name moving upward 120 pixels at ordinary text and 234 pixels at actual 200% text. Entry Name moves upward 164 and 232 pixels respectively. Both initial Entry Name and Distance inputs now fit fully at both scales. `opening-native-bounds.tsv` records exact native input and label bounds. This is a composition improvement, not a prior data-loss claim.

`review.tsv` contains individual observations for all 83 retained original images: 28 before, 54 final and one rejected diagnostic. The final selection includes 20/28 captured states on API 26, 20/28 on API 37 and 14/28 on API 34. Before selections retain 14/26 states on each API 26/37. Modern manifests/catalogs remain the original complete capture inventories; selected PNG/XML bytes match their corresponding manifest hashes and sizes. The 54 final retained hierarchies have zero `NAF` nodes. Review is scoped to these actual frames; unchanged nested Field/Date captures are exercised by the complete journey but not counted as newly visually reviewed here. Their earlier acceptance is in `../track-authoring/`.

Environments are disposable API 26 small (480×800, 240 dpi), API 34 phone (1080×2400, 420 dpi) and API 37 wide (2560×1800, 320 dpi), ordinary and actual Android 200% text. No more than two emulators ran concurrently. Real native keyboard windows and complete focused input/label bounds are asserted, alongside initial Name/Distance visibility and aligned centered header/body geometry.

The existing two real-app journeys now also author Description and Tags, verify persisted values, create/recover/validate/edit an Entry, and reopen the original Track editor to verify both metadata controls. Number unit/precision, Choice order/removal, fractional Scale, all seven typed values, nested draft recreation/discard, invalid numeric recreation, corrected save and stable Entry/Field identities remain covered.

Focused results:

- Corrected API 26 before: 2 tests, zero failures/errors/skips; API 37 before `yClCkw`: 2 tests and 26 states, zero failures/skips/reuse.
- API 37 representative `9wZZBy`: 2 tests and 26 states pass. Two rendered representative opening frames were inspected before the final metadata extension.
- Final API 37 `xPN5YN`: 2 tests and 28 states pass; API 34 `lIUZov`: 2 tests and 28 states pass; final API 26 `astra-track-forms-api26-capture`: 2 tests and 28 captured states pass. All have zero failures/errors/skips; modern capture campaigns have zero reused tests and pass catalog guards.
- Initial API 26 baseline typing evidence lacked the keyboard in one enlarged Track-name frame. That original PNG/XML and report are retained in `before-api26-diagnostic/` and excluded from keyboard acceptance. Explicit native IME waits correct the fixture.
- The first final API 26 invocation passed both tests but omitted the established leave-installed flag, so default Gradle cleanup removed app-private captures before pull. Its test report is retained separately; no visual acceptance is claimed for those unavailable images. The corrected invocation also disables configuration cache as required by the existing execution-time target guard.
- An initial neighboring invocation passed its JVM profile but the Android inventory guard rejected two incorrectly named editor selectors before execution. The corrected selectors below use the actual source-declared classes.
- The corrected neighboring campaign `fI8nA3` passes 97 Track/adaptive Android tests but fails 1/17 editor-dependency tests at the Repeat confirmation. Its failed report and native keyboard events remain retained. The unchanged exact method passes three fresh isolated runs (`ajrvQM`, `XUZMDz`, `L3oMD3`). The fixture now waits for initial native keyboard presentation/removal and verifies Inbox before the same scroll/tap and original assertions. Three fresh complete 17-test editor runs (`Oa8cm8`, `T5k1ox`, `K5OLAk`) then pass with zero failures/errors/skips/reuse: 51 executions of 17 distinct tests. Final accepted neighboring coverage is 97 + 17 = 114 distinct Android tests; the earlier combined campaign remains failed evidence.
- The five-suite Tracks JVM profile executes 64 tests successfully at 19:55:09–10 UTC; `neighbor-jvm-suites.tsv` preserves its exact suite counts. Subsequent unchanged profile invocations reuse those current outputs rather than execute the tests again.

Commands from `/root/repos/whip`, with the stated emulator already booted:

```sh
awk -F '\t' 'NR == 1 || $2 ~ /^tracks\.authoring\./' \
  docs/quality/ui-surface-catalog.tsv > build/astra-track-forms-20260909/catalog.tsv
ANDROID_SERIAL=emulator-5556 \
  WHIP_UI_CATALOG_FILE=/root/repos/whip/build/astra-track-forms-20260909/catalog.tsv \
  scripts/ui-catalog capture --family tracks build/astra-track-forms-20260909/api37-final
ANDROID_SERIAL=emulator-5554 \
  WHIP_UI_CATALOG_FILE=/root/repos/whip/build/astra-track-forms-20260909/catalog.tsv \
  scripts/ui-catalog capture --family tracks build/astra-track-forms-20260909/api34-final
ANDROID_SERIAL=emulator-5556 WHIP_ANDROID_TEST_SLOT=astra-track-forms-api26-capture \
  ./gradlew --no-configuration-cache :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.whip.app.TrackAuthoringJourneyE2ETest \
  -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted tracks \
  --android com.whip.app.TrackAuthoringJourneyE2ETest \
  --android com.whip.app.TrackHistoryJourneyE2ETest \
  --android com.whip.app.TrackInsightsJourneyE2ETest \
  --android com.whip.app.ui.EditorDependencyUxTest \
  --android com.whip.app.AdaptiveWhipScreenTest
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted \
  --android com.whip.app.ui.EditorDependencyUxTest --repeat 3
scripts/ui-catalog lint
scripts/check --ready
```

`scripts/check --ready` passes 344 freshly executed JVM tests in 34 suites, Android-test compilation, lint, debug packaging, asset/destructive-migration guards and whitespace checks. `ready-jvm-suites.tsv` retains suite counts and execution timestamps. Catalog lint passes with 309 matching catalog/matrix IDs; source inventory stays 630 JVM / 1000 Android tests and 38 actual-font fixtures. These inventory counts are not a full-suite execution claim. `SHA256SUMS` covers every retained evidence file except itself.

No schema, data epoch, backup format, release version or physical-device change occurs. Additional Field validation/removal/conflict, oversized imported text editing, complete CSV/repository/ViewModel/recovery, large histories, RTL/TalkBack/fold/rotation and whole-product review remain open. This representative acceptance does not authorize unexamined propagation to other forms.
