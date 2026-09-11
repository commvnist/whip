# Independent 5/3/1 supplemental work

FB/FND/DEC-20260911-001; IMP/VER-20260911-002. Started from clean pushed `f8cf108fce7bc87fb35b3ef5827d0654e5ba872d`. The owner requests Day 1 Flat Barbell Bench Press with BBB and Day 2 Zercher Deadlift with FSL.

## Behavior and ownership

Each main exercise now owns its supplemental scheme, BBB percentage and alternate-BBB target. Leader/Anchor plans expose separate schemes for each role. Presets supply untouched defaults; authored choices survive preset/layout changes, reorder, library reconciliation and Activity recreation. Existing `SelectionField` controls own the selector layout and allow complete selected values to wrap. Custom schedule headings say Day 1, Day 2, etc.

The existing generator resolves each exercise's scheme in the actual phase. BBB can use another selected exercise's Training Max while retaining the originating exercise's BBB percentage. Explicit None remains None. The 7th Week protocol and Training Max progression boundaries retain their rules. Existing set/placement fields persist the generated work; saved routines and completed workouts are not recomputed. Customized preset names no longer claim the unchanged BBB/FSL template. No Room/backup/epoch/version change is needed.

The UI uses Supplemental Work, consistent with [Wendler's public FSL/BBB discussion](https://www.jimwendler.com/blogs/jimwendler-com/5x5-first-set-last-vs-boring-but-big). Custom mixtures are user-authored configurations, not a claim of an exact published template.

## Verification

- `scripts/qa-targeted --profile gym531 --jvm-only`: 86 JVM checks pass. New tests verify mixed BBB/FSL prescriptions in every training week, independent Leader/Anchor/None and alternate-BBB phase ownership, invalid percentages, progression boundaries, full-state serialization/reconciliation and default-versus-authored choice behavior.
- Native owner-example journey: Bench TM 100 / BBB 60% produces five sets of 10 at 60 kg; Zercher TM 200 / FSL produces five sets of five at 130 kg in week one. Reorder/recreation, exact rounded preview, saved day prescriptions, reopening, both complete workouts and unchanged performed history all pass.
- Six existing Gym/5/3/1 Android profile classes execute 88 methods. Their latest results all pass after six exact navigation replacements. `android-results.json` retains every method, original failures and replacements.
- Final fixture-stamped `scripts/check --ready` exits zero and passes 387 JVM checks in 44 suites, Android compilation, lint and debug packaging. Catalog lint passes 538 states/zero pending. Wide run vSVyYZ passes both the complete owner-example native journey and the existing Leader/Anchor/alternate-BBB/assistance/Joker builder journey.

Runtime commands use explicit `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted` with repeated `--android` selectors. The six classes are GymFiveThreeOneJourneyE2ETest, FiveThreeOneAuthorshipJourneyE2ETest, FiveThreeOneCycleJourneyE2ETest, RoutineRepositoryTest, RoutineBuilderUiTest and FiveThreeOneCycleReviewUiTest, under `com.whip.app`. Exact method selections and evidence-directory IDs are in the JSON.

## Failures and replacements

The first JVM pass had one test-only List-versus-Set mismatch in the expected progression boundaries. Initial Android OJZG7f failed two assertions that read the unmerged outer SelectionField column. The next replacement initially lacked a matcher import; nosnnA then passed the incomplete-TM mapping journey and failed the new preview equality assertion because it supplied only part of the full text. jGLieL passes the exact full-text replacement and complete two-day journey.

Broad run 1YbRL3 executes 88 methods with six failures: five existing setup tests tapped/asserted choices without scrolling them into view at the actual 360 dp phone width; the Leader/Anchor mapping fixture kept the keyboard open while asserting an outer field container. 0HzfM0 passes all six exact replacements after making each target visible, verifying preset selection and dismissing the keyboard before targeting the named mapping button. Production inputs are unchanged across those replacements. No failed batch is described as wholly green.

## Native artifacts and limits

Phone evidence uses synthetic records on a disposable API 34 emulator at 1080×2520/480 dpi (360 dp), 100% text. `phone/` retains four personally reviewed PNG/XML pairs. All PNG bytes are original; XML is normalized only for line endings/trailing whitespace. `phone-capture-provenance.json` records original and retained hashes and numbered MediaStore sources. The setup frame shows the complete BBB percentage and wrapped target; review shows both distinct supplemental prescriptions. Workout frames show correct day/exercise context; exact supplemental sets and completed history are asserted by the native journey beyond the captured initial viewport.

`wide/` adds five personally reviewed original PNG/XML pairs at 1800×1200/160 dpi. The four owner-example states retain complete identity, controls, prescriptions and native day context in the centered setup/workspace panes. The fifth frame covers the unchanged Leader/Anchor assistance/Joker review after using the relocated alternate-BBB control. All nine retained images are inspected, XML has no unnamed interactive nodes, and all 18 capture hashes are recorded. All 514 final source/build/test/harness hashes match. After broad runtime coverage only the six navigation fixtures changed; final compilation/lint/readiness and both wide methods use those exact inputs.

Source inventory: 655 JVM + 1078 Android = 1733; 538 catalog states. All test processes are terminal; task emulator display overrides are reset and the emulator is stopped. This is bounded implementation/emulator acceptance, not a complete new platform matrix, physical-phone release or resumption of the closed exhaustive audit. Existing phone version remains 0.3.69/code 75 until a release is requested.

Subsequent authorized delivery: the owner requested installation, completed as [Whip 0.3.70/code 76](../release-0.3.70/README.md) under FB-20260911-002 / IMP, VER-20260911-003. The preceding verification scope remains historical; only application versionName/versionCode changed among its 514 inputs.
