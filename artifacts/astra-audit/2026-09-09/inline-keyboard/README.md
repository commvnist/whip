# Inline input and short Track reading

Evidence for FND-20260909-018, DEC-20260909-016, IMP-20260909-018 and VER-20260909-019. This is a bounded keyboard, recovery and record-reading improvement within the continuing app-wide audit.

The activity now resizes for inline input. Its content consumes keyboard space, global content controls yield during typing, and persistent side navigation retains its position. Track search and Task Quick Capture request room for the whole focused field and floating label. Short Track detail uses a compact identity; Task capture temporarily yields its repeated introduction. Saved data and historical records are unchanged.

All four complete ordinary/actual-200% journeys passed on each platform:

| Evidence | Device | Run | Result |
| --- | --- | --- | --- |
| final-api26 | API 26, 480×800 px, 240 dpi (320×533 dp) | astra-inline-keyboard-complete | 4/4; 12 PNG/XML pairs |
| final-api34 | API 34, 1080×2400 px, 420 dpi | tydjGh | 4/4; 12 distinct canonical pairs |
| final-api37 | API 37, 2560×1800 px, 320 dpi | jRye4g | 4/4; 12 distinct canonical pairs |

The final Android reports have zero failures, errors or skips. Both modern canonical runs have zero reused results. `capture-catalog.tsv` includes all outputs of the four selected owners. API 26 uses the fixtures' private native-capture fallback; only named catalog states are retained. Every final original image was personally inspected; `review.tsv` distinguishes bounded acceptance from remaining visual concerns.

Track journeys exercise 30 archived Entries, older-record reading, native input, full query/label and identity/Back bounds, a reachable full matching title above the keyboard, query retention after dismissal/recreation, the read-only inspector, Back to Archived, contextual Restore and unchanged Field/Choice/Entry graphs. Task journeys check actual text layout scale, full native field/label, draft recreation, IME submission, accepted-draft clearing, retained Tasks selection and persisted reopening.

Neighboring verification passed 64 JVM tests in five suites and 109 fresh Android tests in two batches (`HkNJ7Y`). This covers Tracks plus navigation, global Search routing, adaptive layouts, persistent rail during IME, Task editing/creation and productivity defaults. Final `scripts/check --ready` results are recorded in VER-20260909-019; the retained JVM suite inventory contains 344 tests in 34 suites. These are executed subsets, not the declared complete 630-JVM/996-Android inventory.

The two `before-task` pairs show the intermediate state after activity resize but before shared field visibility and Task-header correction. They are not a pristine whole-product baseline. Prior Track baseline originals remain in [the preceding history evidence](../track-history/README.md). Three supplemental pairs show whole matching results while the keyboard is still open: two from passing API 26 `astra-inline-keyboard-final5`, one enlarged API 34 focused diagnostic. The canonical `search-result` states instead capture retained queries after dismissal, avoiding duplicate ordinary query/result images.

Known limits remain open: the smallest enlarged archive initially shows very little history; query and result can require scrolling separately; single-line Task input horizontally scrolls long drafts; the API 26 font lacks the boot emoji. The earlier alleged touching destinations, clipped Area outlines, overlapping rows and missing painted action icons were rejected by direct original-pixel and stable-frame verification in [VER-20260909-020 evidence](../track-frame-review/README.md). Those were mistaken review interpretations; original PNG/XML bytes remain unchanged and review.tsv is corrected explicitly. FND-20260909-019 now covers the supported initial small enlarged-history constraint. Broad Tracks and whole-product acceptance remain open.

Reproduction commands, from the repository root:

```sh
ANDROID_SERIAL=emulator-5556 WHIP_ANDROID_TEST_SLOT=astra-inline-keyboard-complete ./gradlew --no-configuration-cache :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.whip.app.TrackHistoryJourneyE2ETest,com.whip.app.InlineTaskCaptureE2ETest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
ANDROID_SERIAL=emulator-5554 WHIP_UI_CATALOG_FILE="$PWD/build/astra-tracks-20260909/keyboard-catalog.tsv" scripts/ui-catalog capture build/astra-tracks-20260909/keyboard-api34-final4
ANDROID_SERIAL=emulator-5556 WHIP_UI_CATALOG_FILE="$PWD/build/astra-tracks-20260909/keyboard-catalog.tsv" scripts/ui-catalog capture build/astra-tracks-20260909/keyboard-api37-final
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted tracks --android com.whip.app.WhipNavigationTest --android com.whip.app.GlobalSearchRoutingTest --android com.whip.app.AdaptiveWhipScreenTest --android com.whip.app.ImeNavigationRailE2ETest --android com.whip.app.TaskEditorJourneyE2ETest --android com.whip.app.ProductivityCreationJourneyE2ETest --android com.whip.app.ProductivityDefaultsUiTest --emulator
scripts/check --ready
```

Use the matching named disposable emulator for each command; API 26 and API 37 occupied the same second transport at different times. At most two disposable emulators ran concurrently. No physical-device or release action occurred.

Original PNG/XML bytes are preserved, including native XML line endings. `SHA256SUMS` covers this evidence directory excluding itself. Modern manifest SHA-256: API 34 `1ff22d8519440adf9e607495bbb2419b822a760bef3cef65515d748601ad7d3e`; API 37 `e2824708a0f55645773419d17c22803c146c55682466ef23dbd748c7bbf1d47f`.
