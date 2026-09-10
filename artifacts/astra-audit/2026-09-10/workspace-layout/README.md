# Consistent workspace composition — 2026-09-10

FND-20260910-030 / DEC, IMP and VER-20260910-021. Starting production: `86860fd7`. Ordinary API 34 phone and API 37 wide emulator review; no release or physical-device operations.

`WhipWorkspaceLayout` gives complete workspace headers and content one shared measure. Reading uses up to 720 dp; Home, Track Insights, Gym Progress and feature-owned browsers retain up to 1000 dp. Existing item builders still own their internal geometry, and features retain scrolling, state, commands and persistence.

Expanded Tracks now has one list/detail browser without a duplicate app statistics column. Existing Insights retains the Fields total; Activity retains chronological record access. Primary navigation, physical fold context, Settings categories and specialized Routine authoring remain. There is no schema, backup, calculation or historical-data change.

## Verification

Final phone command:

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted shell \
  --android com.whip.app.TrackWorkspaceUiTest \
  --android com.whip.app.TrackDraftSavedStateJourneyTest \
  --android com.whip.app.TrackDraftRecoveryUiTest \
  --android com.whip.app.RecordItemBuilderJourneyE2ETest \
  --android com.whip.app.ProductivityItemBuilderJourneyE2ETest \
  --android com.whip.app.SettingItemBuilderJourneyE2ETest \
  --android com.whip.app.RoutineAuthoringJourneyE2ETest \
  --android com.whip.app.FiveThreeOneCycleJourneyE2ETest \
  --android com.whip.app.ImeNavigationRailE2ETest \
  --android com.whip.app.GoalProgressJourneyE2ETest \
  --android com.whip.app.ProductivityCreationJourneyE2ETest --emulator
```

- `V3IwiZ`: 64 profile JVM tests in ten suites; 120 fresh Android tests across three batches (43/54/23), zero failures/errors/skips/reuse. Includes controlled adaptive/fold, keyboard, navigation, accessibility and neighboring native journeys.
- Matching APKs on API 37: direct instrumentation of RecordItemBuilderJourneyE2ETest, ProductivityItemBuilderJourneyE2ETest, SettingItemBuilderJourneyE2ETest, RoutineAuthoringJourneyE2ETest and FiveThreeOneCycleJourneyE2ETest passes six methods in 225.195 seconds. The recorded runner is `commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner`; `wide.log` retains every method result.
- `scripts/check --ready`: 379 JVM checks in 43 suites, Android compilation, lint, debug packaging and static/harness checks pass. Final Gradle build: 2m38s.
- All 513 source/test/harness/APK hashes remain identical through final runtime and readiness; see `final-hash-check.json`. Source inventory remains 653 JVM + 1081 Android = 1734; these are declared counts, distinct from executed totals.
- `scripts/ui-catalog lint`: 506 required states, zero pending selectors/platform exceptions. Matrix: 337 Verified / 127 Investigating / 42 In progress.

Native checks cover exact record actions and archived history; a running Habit timer through recreation and Stop; immediate choices versus confirmed Settings Save; both complete Routine journeys; and the full programmed cycle through review/recreation, next-cycle loading and unchanged earlier History. Bounds checks require the declared available width, centering and shared header containment. Tracks retains primary loading/error/empty states after removing the outer overview.

## Visual review and limits

Thirty personally reviewed original PNG/XML pairs are retained: eight baseline, nine final states on each normal phone/wide target, and four wide Gym/Routine neighbors. `review.tsv` identifies the scope of every image. PNG bytes are original. Readable XML/logs normalize trailing whitespace; gzip archives preserve original bytes. Manifests retain source filenames, run identities and hashes. Every retained hierarchy passes the existing unnamed-interaction guard.

The normal phone layout retains its reading and action geometry. Wide Entries gains a clearer list/detail division, Activity loses competing sidebar information, and serial Settings/Habit/workout controls share a bounded column with their headers. One Next Set action still reveals exact inputs and Complete Set below the pinned lane. Routine outline/placement composition remains deliberately broader.

This verifies the shared layout policy and the named journeys. Metric-row hierarchy in Insights, independently filtered support-pane context, exact later/grouped Set navigation and the remaining whole-product review stay open. There is no new process-death, TalkBack, physical-device or separate 200% campaign acceptance.

## Intermediate evidence

- Unchanged `86860fd7` baseline: three native methods pass on wide in 37.558s and phone in 29.971s. The initial phone runner found no target package after prior Gradle cleanup; installing the matching debug APK restored the disposable fixture. Both installed wide APK hashes match the prior evidence before replacement.
- `TFFWEr`: three phone pilot methods pass; the same initial wide pilot passes in 38.009s. Compilation passes in 10s.
- `KaRJh7`: strengthened Insights aggregate and primary Tracks loading/error/empty-state checks pass, two methods.
- `kQnlbd`: first 43 checks pass; batch 2 rejects two of 54 and batch 3 does not run. One old test requires the removed overview and global coordinates across different layout roles. Its replacement preserves action order and serial-screen alignment while checking each declared column. The complete-cycle fixture looks for Open Active Workout before asynchronous Start Next produces it; it now waits for that visible action. The obsolete overview string requirement is also removed from its JVM policy test. Production remains unchanged.
- `RandMV`: both corrected focused methods pass before the complete fresh final cohort. No historical assertion or capture guard is removed.
