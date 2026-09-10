# Routine prescription authoring — 2026-09-10

FND-20260910-026 / DEC, IMP and VER-20260910-017. Starting production: d50a3e6. Normal 100% experience; no release or physical-device operation.

Routine placement editing now presents Set prescriptions before notes, saved schemes, warm-up generation, copy-previous behavior and 5/3/1 conversion. Existing shared section headings and Training Max disclosure supply the hierarchy. The first Working label moves from y=1995 to y=1155 on the matched 1080×2400 phone: 840 pixels upward. Load, both repetition bounds and Add Set are visible in the opening view. An empty Superset heading disappears when the placement has neither a peer nor a group.

No draft, repository, schema, backup or program calculation changes. Program-controlled Main/Supplemental restrictions, complete advanced values and percentage-load prerequisites remain intact. Bulk helpers require scrolling after long Set lists; shared wide reading width and unused master-pane space remain separate design work.

## Evidence and acceptance

- `xsVeJy`: 209 fresh API 34 Android checks, zero failures/skips, in 192- and 17-method batches. Both native Routine journeys, all 33 Routine UI methods, Gym/5/3/1 and editor recovery/dependency regressions pass.
- API 37: seven methods, terminal `OK (7 tests)` in 138.333 seconds; both native Routine journeys, real 5/3/1 setup/start, explicit/optional Training Max, controlled Main editing and saved-scheme lifecycle. Same final production and fixture inputs as the phone.
- 109 focused JVM results in eight suites pass on final production. The fixture-only rerun reuses the unchanged JVM task; these are not claimed as 109 newly executed tests in that rerun.
- Final `scripts/check --ready`: passed in 2m28s; 371 JVM tests in 41 suites, zero failures/errors/skips, Android compilation, lint, debug packaging and static/harness checks. All 505 input hashes remain unchanged afterward.
- Catalog: 488 required states, zero pending selectors or platform exceptions. Source inventory: 646 JVM + 1077 Android = 1723, distinct from executed counts.

The new native journey authors 40 kg × 6–8 reps, RPE 8, RIR 2, 90 seconds rest, tempo 3010 and a complete Set note. Activity recreation, advanced-field hiding, duplicate/delete and three generated/removed warm-ups preserve the exact working draft. Save/reopen exposes correctly disabled unchanged Save; a real Exercise-note edit preserves every Set value. Start copies exact prescription snapshots. Logging 7 reps, finishing, then changing the Routine to 45 kg leaves the entire completed Set unchanged and History still displays 40 kg × 7 reps with the original target, rest, tempo and note.

Twenty-five accepted PNG/XML pairs were personally inspected: five before, ten final phone and ten final wide. `review.tsv` records each scope. The retained rejected initial pair shows the picker, so it is excluded from placement acceptance. PNG bytes are original. Readable XML uses normalized line endings/trailing whitespace; `.xml.gz` preserves every original XML byte. `manifest.json` records native filenames, original and artifact SHA-256 hashes and run provenance.

Result XML and copied install logs also use repository-normalized whitespace; `text-normalization.json` records those transformations and their byte-preserving gzip archives. This changes no test result or screenshot content.

## Commands

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile gym --profile gym531 \
  --android com.whip.app.EditorStateRecreationTest \
  --android com.whip.app.ui.EditorDependencyUxTest --emulator
```

For the wide run, install the matching debug and test APKs on explicit emulator-5556, then:

```sh
adb -s emulator-5556 shell am instrument -w -r -e class \
'com.whip.app.RoutineAuthoringJourneyE2ETest,com.whip.app.GymFiveThreeOneJourneyE2ETest,com.whip.app.RoutineBuilderUiTest#ordinaryRoutineCanEnterAnExplicitTrainingMaxForPercentageSets,com.whip.app.RoutineBuilderUiTest#staticRoutineExposesTrainingMaxBeforeAnySetUsesIt,com.whip.app.RoutineBuilderUiTest#structuredMainExerciseUsesProgramStructureAndHidesGenericRewriteControls,com.whip.app.RoutineBuilderUiTest#savedRepSchemeCanBeAppliedEditedAndDeleted' \
commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner
scripts/check --ready
scripts/ui-catalog lint
```

API 34: 1080×2400, 420 dpi. API 37: 2560×1800, 320 dpi. Both use font scale 1.0. Final hashes cover 505 product/source/test/script/build/APK inputs. The selector correction changed only RoutineAuthoringJourneyE2ETest and the test APK; `selector-delta.json` records the exact difference from the first broad run.

## Rejected and intermediate checkpoints

- `ubmoxy`: one baseline failure because the fixture clicked disabled unchanged Save. Corrected by checking that state and authoring a real note change; production unchanged.
- `B6Rsmx`: complete baseline passes, but initial capture still shows the picker. Four other baseline originals remain valid. Compose synchronization and a placement assertion produce the accepted initial replacement in passing `7nvicj`.
- `i4usDi`: all 36 hierarchy pilot methods pass. Final production then removes the empty Superset heading; final fixture adds warm-up generation/removal coverage.
- `JY3GrB`: 192 executed, 191 pass, one fixture failure matching two Warm-up labels. Numbered Set controls remove that ambiguity. `xsVeJy` passes the complete 209-method union afterward.

FND-20260910-027 remains Investigating: the ready manual timer displays the app's 2:00 default while the authored Set/History contain 1:30; automatic completion uses a separate fallback chain. This requires complete rest/override/recovery review. Complete programmed-cycle/history acceptance, shared wide-screen policy and full-product final gates remain open. This increment adds no OS-process, TalkBack, RTL, API 26 or 200% acceptance.
