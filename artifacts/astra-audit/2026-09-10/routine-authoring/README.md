# Ordinary Routine authoring

2026-09-10 · FND-20260910-025 · DEC/IMP/VER-20260910-016 · baseline `9d165e2`.

Routine setup now uses the shared navigation role, optional notes use the shared disclosure role, and ordinary Add Exercises sits immediately after the selected-day controls. Its initial phone label moves from y=2196 to y=1432: **764 pixels higher** on the same 1080×2400, 420 dpi, 100% viewport. No text-size branch or domain/persistence change is involved.

The new native journey creates Upper/Lower days, chooses three exercises, recreates the selected placement, saves routine notes, reopens the routine, updates the Lower cue without leaving its editor, and starts that exact day. It compares every authored placement field and Set draft across editing, allowing the repository's documented replacement of child row IDs/timestamps. Existing 5/3/1 tests retain program creation, explicit Training Max targets, one-owner recovery, custom selection, program authority and scoped editing.

## Results and provenance

- `EdaBeb`: first baseline fixture fails because it reads a non-merged Surface instead of the visible saved-in-place message. The corrected `Oh4sql` baseline passes the complete journey on unchanged production.
- `khJBCK`: 35 pilot authoring/5/3/1 tests pass. Final production additionally removes an empty ordinary-list item that doubled the gap before notes.
- `Md3fyk`: 190/191 tests pass; one neighboring Machine test loses its scroll target after name input. All retained final phone capture owners pass in this run. Its full batch remains rejected and its original failure is preserved.
- `oOTEpN`: the unchanged Machine method passes alone. Its two matching fixtures now close the keyboard before scrolling, removing a plausible IME/bring-into-view race while retaining every selection/save assertion. A preceding direct runner attempt executes zero tests because Gradle removed instrumentation registration; the normal targeted installer resolves it.
- API 37: five selected native/controlled methods pass in 90.044 seconds, terminal `OK (5 tests)`. Exact selection appears below and output is in `wide.log`.
- Focused JVM: 109 tests in eight suites pass with zero failures/errors/skips; individual reports are retained.
- Final `OooTg1`: all **208 Android checks** pass in two fresh batches, zero failures/skips/reuse. Readiness passes **371 JVM tests in 41 suites**, zero failures/errors/skips, compilation, lint, debug packaging and static/harness checks. Final reports and `regression-final.log` / `readiness.log` are retained. Catalog lint passes 482 states, zero pending selectors/exceptions; `git diff --check` passes.

`final-inputs.json` records all 505 source/script/build/APK inputs used by the reviewed native phone and wide runs. `final-fixture-inputs.json` and `final-fixture-delta.json` identify the only subsequent differences: GymPowerInputUiTest and its test APK. All production files, the app APK, scripts and the selected authoring/5/3/1 test sources are identical. The final regression validates the fixture update. No source/APK changes occur while instrumentation is live.

## Original visual evidence

Twenty-four original PNG/XML pairs were individually inspected. `review.tsv` records each observation; `manifest.json` retains the original device filename, original SHA-256 and readable-artifact SHA-256. PNG chunks/CRC/decompression and XML parsing were checked. PNG bytes are unchanged. Original native XML bytes are retained in `.xml.gz`; readable XML/logs use LF and remove trailing line whitespace, as recorded in `text-normalization.json`. Logs also normalize the local repository root to `<repo>`. XML attributes and semantics are unchanged.

- `before/`: six ordinary phone frames from the passing baseline.
- `phone/`: seven final-production authoring frames from the passing owning method in Md3fyk.
- `wide/`: seven matching API 37 authoring frames.
- `phone-neighbors/`, `wide-neighbors/`: the real generated 5/3/1 active-Routine and programmed-Workout states on each platform.

The specialized placement editor still puts conversion and bulk tools before ordinary Set inputs; **FND-20260910-026 remains open**. Wide master/detail duplication and broad reading measure remain a shared screen-level design follow-up. This increment does not claim complete Routine programming, workout completion/history, OS-process recovery, TalkBack, RTL, API 26 or new 200% acceptance. Version 0.3.66/code 72, schema 46, epoch 6 and backup 26 are unchanged; no release occurred.

## Reproduction

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile gym --profile gym531 --android com.whip.app.EditorStateRecreationTest --android com.whip.app.ui.EditorDependencyUxTest --emulator

adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5556 shell am instrument -w -r -e class 'com.whip.app.RoutineAuthoringJourneyE2ETest,com.whip.app.GymFiveThreeOneJourneyE2ETest,com.whip.app.RoutineBuilderUiTest#blankRoutineSurfacesTopLevelFiveThreeOneEntry,com.whip.app.RoutineBuilderUiTest#addingExerciseToExistingRoutineSavesWithoutLeavingTheEditedDay,com.whip.app.RoutineBuilderUiTest#exerciseListUsesTheRoutinePaneInsteadOfAOneRowNestedViewport' commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner

scripts/check --ready
scripts/ui-catalog lint
git diff --check
```
