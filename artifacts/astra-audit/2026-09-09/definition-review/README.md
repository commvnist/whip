# Track definition Save, recovery and removal review

2026-09-09 · FND-20260909-031/032/033/034 · DEC-20260909-027/028 · VER-20260909-031.

Baseline: `11c442977895c41b3112e8e04eefe552300f6e6e`. This is scoped evidence within the active whole-product goal; complete Tracks/app acceptance remains open.

An existing Track Save could remain unfinished when canonical normalization changed the submitted name, Tags, Field/Choice labels or Area name. Save now installs that canonical draft before requesting review, preserving exact response ownership. Every rejected validation attempt reveals its summary, including repeated identical errors. Closed nested Field sessions release their saved state so recreation and reopening cannot resurrect old text.

Small enlarged removal review also hid its warning and selected replacement. The warning and headings now scroll above fixed actions; the consequential replacement value wraps. The final review takes its natural bounded height, bringing short content and actions together. Initial draft removal and exact historical-data review retain separate purposes and remain explicit. IDs, history, canonical domain validation and transactional mutation authority are preserved.

The six permanent native methods seed two Entries with text, Choice and note values, edit through the real app, recreate the Activity, save and reopen. They verify raw authorship before Save, canonical text/Tags/Area after Save, exact Entry/value maps and stable Field/Choice identities. Destructive review moves Street values to Trail, removes Notes, retains the selected replacement through recreation, rejects an old review after a third Entry appears, then applies a fresh exact review. Only selected note values and the intended Choice reference/timestamps change. Parent validation tests duplicate Field names, repeated attempts, missing Entry Identity, correction and recreation.

## Execution record

| Evidence | Outcome |
| --- | --- |
| `baseline.log` / `HgT9Tu` | Expected normalization timeout. Removal reaches persistence but its fixture incorrectly expects unchanged definition timestamps. |
| `baseline2.log` / `UFbJ5S` | Expected offscreen validation failure; corrected removal fixture passes. |
| `corrected.log` / `mOuCdx` | Both removals pass; normalization exposes stale reopened Choice text; parent selector incorrectly omits a bullet. |
| `corrected2.log` / `GA4Q3E` | Parent validation passes; stale Choice remains with a real Area change. |
| `corrected3.log` / `V3ZUh8` | Four parent/removal methods pass; canonical persistence/reopening pass, then the discard fixture uses the wrong button label. |
| `corrected4.log` / `TkNsDY` | All six initial corrected methods pass on API 34. |
| `neighbors.log` / `ihnCg7` | 66 JVM and 117 Android checks, zero failures/skips/reuse. This precedes the final adaptive refinement. |
| `api37-ordinary.log` | Interrupted, exit 143, no terminal acceptance. |
| `api37-ordinary-resumed.log`, `api37-large.log` | Three ordinary and three actual-200% methods pass before adaptive refinement. |
| `api26.log` | Four methods pass; both normalization methods stop in an unnecessary Espresso keyboard call after canonical persistence succeeds. Visual review finds the warning/destination defects. |
| `adaptive-api34.log` / `dFFUCS`, `api26-adaptive.log` | All six pass on each platform with the adaptive correction and keyboard fixture fix. |
| `adaptive-neighbors.log` / `TL3OiW` | 55 tests: 43 shared-control, eight mutation, two authoring and two recovery; zero failures/skips/reuse. |
| `api37-final-large.log`, `api37-final-ordinary.log` | Three enlarged methods pass; ordinary removal hits a false-positive paragraph-width assertion. |
| `api37-overflow-diagnostic*.log` | Initial device checks find a disconnected emulator. Executed diagnostic 3 reproduces Text width 920 versus unused paragraph constraint 1024; the original image shows complete text. The fixture now checks actual line edges, ellipses and height. |
| `ready.log`, `initial-readiness-jvm/` | 346 JVM tests in 34 suites, no failures/errors/skips; compilation, lint and packaging pass before adaptive refinement. |
| `final-api34.log` / `OmHHhj` | All six methods pass with corrected rendered-line assertions, zero failures/skips/reuse. |
| `api37-accepted-ordinary.log`, `api37-accepted-large.log` | Final three-method processes each pass, 91.183 and 75.712 seconds. |
| `ready-final.log`, `readiness-jvm/` | Final production passes 346 fresh JVM tests in 34 suites, zero failures/errors/skips, plus compilation/lint/debug packaging. Later fixture-only line checks compile and execute in OmHHhj and both final API 37 runs. |
| `check-fixtures-final.log`, `catalog-fixtures-final.log`, `catalog-lint-final.log` | Final wrapper/catalog contract checks pass; 353 catalog states. |

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile tracks \
  --android com.whip.app.TrackDefinitionMutationUiTest \
  --android com.whip.app.TrackAuthoringJourneyE2ETest --emulator
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted \
  --android com.whip.app.TrackDefinitionReviewJourneyE2ETest --emulator
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted \
  --android com.whip.app.TrackDefinitionMutationUiTest \
  --android com.whip.app.TrackAuthoringJourneyE2ETest \
  --android com.whip.app.TrackDraftRecoveryUiTest \
  --android com.whip.app.ui.InteractionControlUiTest --emulator
adb -s emulator-5556 shell am instrument -w -r \
  -e class com.whip.app.TrackDefinitionReviewJourneyE2ETest \
  commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner
scripts/check --ready
scripts/test-check-fast
scripts/test-ui-catalog
scripts/ui-catalog lint
```

API 26 uses the direct full-class command after explicit APK installation. API 37 selects its three ordinary methods together after setting system `font_scale=1.0` and force-stopping the debug app, then its three `AtLargeText` methods at 2.0 in a fresh process. Require `OK (N tests)`; a successful shell exit alone is insufficient. API 26 is 480×800/240 dpi, API 34 is 1080×2400/420 dpi and API 37 is 2560×1800/320 dpi. At most two disposable emulators ran concurrently. No physical-phone operation, release, publication, schema, backup or version change occurred.

## Visual scope

All 45 retained originals have personal review: 36 final states and nine before/diagnostic frames. API 26 frames come from `api26-adaptive.log`, API 34 frames from `dFFUCS`, and API 37 frames from the two accepted final processes. All share the final production behavior; the later change corrects only the width assertion and captures its diagnostic before asserting. The initial capture manifests describe everything originally pulled, while `review.tsv` lists the deliberately retained subset. Two API 26 before images have no retained paired tree.

Original PNGs are unchanged. `review.tsv` identifies each retained before, diagnostic and final frame. Before evidence includes raw unfinished Save, hidden validation, stale Field text, small enlarged clipping and unnecessarily tall ordinary reviews. Final frames cover six states at both text scales on each platform. Paired trees supplement personal inspection; neither tree counts nor successful tests establish TalkBack acceptance.

Long forms and short enlarged dialogs scroll; one frame does not claim all content fits simultaneously. The small enlarged removal frame intentionally shows the end of the warning with its title above the viewport. The replacement frame shows the complete Trail destination. The smallest enlarged single-line Option input shows only part of its horizontally editable value; persistence and semantics verify the complete text, and no claim of simultaneous full-value display is made.

Copy-recovery validation uses the same attempt-reveal helper but has no new complete copy journey here. Long arbitrary Field/Choice labels, realistic large histories, retained-history-incompatible Scale UX, RTL/TalkBack and complete Tracks/app review remain open. Git interruption reconciliation found intact changes and no corruption; no history was rewritten.

`tested-source-and-apk-sha256.json` records the tested inputs. Final source differs only in indentation of the new nested blocks; `formatting-proof.json` verifies that reversing that indentation exactly reproduces the tested source hash. `source-and-apk-sha256.json` records retained source and unchanged APKs. `text-normalization.json` records original/retained hashes for text-only LF, trailing physical whitespace and final-newline normalization; PNG bytes remain unchanged. `SHA256SUMS` covers retained evidence.
