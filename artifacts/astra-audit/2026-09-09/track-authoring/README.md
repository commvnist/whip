# Track authoring, nested input and Date selection

Scoped evidence for FND-20260909-020/021, DEC-20260909-018/019, IMP-20260909-021 and VER-20260909-022. Baseline was clean pushed `e9f9995`. Recovery found no interrupted Git operation, corrupt objects or active connected-test process. The recovered development test had ended with a normal assertion failure; its unfinished source was completed and verified here.

The small actual-200% nested Field Name had only 109 native pixels of its expected 132-pixel input, with the floating label visibly clipped above the keyboard. The Field heading now scrolls with configuration, while accessible pane identity and fixed Cancel/Save remain. The existing whole-input relocation helper responds to viewport changes. The existing 72% body bound stays: an intermediate full-height body made the card extend behind status icons and was rejected.

Two subsequent enlarged small-device journeys independently timed out at Date Set because Compose could not settle. The native diagnostic shows the wheel body cut after its selected row. Scrolling the existing Date body preserves the designed 144 dp wheel height and removes the reproduced failure. The opening date, explicit Set/Cancel and stored date semantics remain unchanged. Exact internal recomposition causality is not claimed.

The complete real-app journey authors all seven Field types; configures Number distance units/precision, Choice order/removal and fractional Scale; recreates and discards nested drafts; rejects missing required values; records all typed Entry values; recreates an invalid numeric edit; corrects it; and reopens the same Entry. It asserts coherent transactional persisted snapshots after observable commits. Untouched values retain identity and authored data; only refreshed revision timestamps are normalized for comparison. This test adjustment does not remediate the separate observable-projection coherence lead.

| Final run | Device | Journeys | Captured states |
| --- | --- | --- | --- |
| `astra-authoring-api26-date` | API 26, 480×800, 240 dpi | 2 passed | 18 |
| `3HvE6h` | API 34, 1080×2400, 420 dpi | 2 passed | 18 |
| `7EPtEI` | API 37, 2560×1800, 320 dpi | 2 passed | 18 |

Each run covers ordinary and actual Android 200% text, complete native Field/input-label bounds, actual Field/Date dialog text scale, the complete Year wheel, unchanged opening date and typed persistence/recreation. All reports have zero failures/errors/skips. Canonical modern runs reuse no results and pass native capture guards. All 54 final PNG/XML pairs are retained without transformation, have individual personal image review in `review.tsv`, and contain zero native `NAF=true` nodes. This is scoped accessibility evidence, not a full TalkBack/RTL review.

The 62 reviewed original PNGs comprise 54 final images, six initial API 34 Field images, one failing API 26 Field image and one unpaired API 26 Date diagnostic. `before-api34` retains only the six affected Field pairs from the passing 16-state initial run `2QuqUy`; its original complete catalog/manifest are retained as provenance, not a claim that all 16 files are in this directory. Every retained canonical pair matches its manifest lengths and SHA-256. API 26 uses app-private capture because canonical MediaStore capture requires API 29+. `SHA256SUMS` covers the retained files independently.

Failure reports preserve the original small Field failure and both Date timeout attempts. Earlier development failures concerned the CustomActions API, an unavailable separate-window resource-ID selector, an incorrect test-only date property, value revision timestamps, and an intermediate projection before Choice options arrived. They are documented in VER-20260909-022 and are not successful verification or proven user-data loss.

The final neighboring campaign `cUf1X6` passes 64 JVM and 110 fresh Android tests, including Track repository/definition/Entry/CSV/workspace, Date dependencies, productivity creation, Task editing and adaptive layout. Exact Android class inventory and aggregate/report are retained. Final `scripts/check --ready` passes 344 JVM tests in 34 suites, Android-test compilation, lint, debug packaging and static guards; `ready-jvm-suites.tsv` records the suite counts. Catalog lint passes; catalog and surface matrix have 299 matching IDs, including these 18 new states. Source inventory is 630 JVM / 998 Android tests, with 38 actual-font fixtures. These are inventory counts, not full-suite execution claims.

Final commands, from the repository root with the stated disposable emulator already booted:

```sh
ANDROID_SERIAL=emulator-5556 WHIP_ANDROID_TEST_SLOT=astra-authoring-api26-date \
  ./gradlew --no-configuration-cache :app:connectedDebugAndroidTest \
  '-Pandroid.testInstrumentationRunnerArguments.class=com.whip.app.TrackAuthoringJourneyE2ETest' \
  -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true

awk -F '\t' 'NR == 1 || $2 ~ /^tracks\.authoring\./' \
  docs/quality/ui-surface-catalog.tsv > build/astra-authoring-20260909/catalog.tsv
ANDROID_SERIAL=emulator-5554 \
  WHIP_UI_CATALOG_FILE=/root/repos/whip/build/astra-authoring-20260909/catalog.tsv \
  scripts/ui-catalog capture --family tracks build/astra-authoring-20260909/api34-corrected
ANDROID_SERIAL=emulator-5556 \
  WHIP_UI_CATALOG_FILE=/root/repos/whip/build/astra-authoring-20260909/catalog.tsv \
  scripts/ui-catalog capture --family tracks build/astra-authoring-20260909/api37-final

ANDROID_SERIAL=emulator-5554 scripts/qa-targeted tracks \
  --android com.whip.app.ui.EditorDependencyUxTest \
  --android com.whip.app.ProductivityCreationJourneyE2ETest \
  --android com.whip.app.TaskEditorJourneyE2ETest \
  --android com.whip.app.AdaptiveWhipScreenTest --emulator
scripts/ui-catalog lint
scripts/check --ready
```

The small enlarged Entry introduction can consume the first viewport after validation, existing Entry identity repeats above editable Name, and wide forms leave large label/control spans. Those design follow-ups remain open, alongside additional Field validation, projection consumers, CSV/source, filters/date windows and broader adaptive/accessibility review. Scrolling Date secondary controls is intentional; not every date gesture/locale or Field input combination is established here. At most two disposable emulators ran. No physical-device, release, schema, backup or data migration occurred. Complete Tracks and whole-product acceptance remain open.
