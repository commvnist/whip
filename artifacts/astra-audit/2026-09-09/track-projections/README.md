# Coherent live Track updates

Evidence for FND-20260909-022, DEC-20260909-020 and VER-20260909-023. Baseline is clean pushed `194ffa0`. The preceding seven-type authoring journey exposed an intermediate observable definition before its Choice options arrived; its persisted transactional graph was intact.

Both baseline observer tests fail in two separate API 34 runs. The strengthened final baseline pairs Track `Reading 1` with Fields `Title 0`/`Genre 0`, and publishes Entry 1 without its required Title value. The tests validate every collected emission, so an eventually correct last state cannot hide those intermediate failures. Definition observation overlaps eight rapid public repository renames; Entry observation covers eight public writes with required text/Choice values and newest-first ordering.

The repository now observes invalidation of all five Track tables and reads their ordered rows in one transaction. Domain conversion and grouping occur after releasing the transaction. This preserves complete live definitions, Entry identities and summaries, with the same Track/Field/Choice/Entry ordering and existing non-empty Field guard. Public individual-table streams, CSV export and mutation/preparation boundaries keep their existing behavior. There are five bulk reads per relevant invalidation, with no per-Track query multiplication; complete large-history responsiveness remains a later audit requirement.

| Evidence | Result |
| --- | --- |
| `before-initial.xml` | API 34, 2 tests / 2 failures |
| `before-final.xml` | API 34 concurrent-writer baseline, 2 tests / 2 failures |
| `representative-api34.xml` | First correction, 2 tests passed; before the explicit ordering assertion was added |
| `api37.xml` | Final consistency/order tests, 2 passed |
| `api26.xml` | Final consistency/order tests, 2 passed |
| `neighbors-api34.xml` / `neighbors-aggregate.tsv` | Final targeted campaign `MeVcX4`, 64 Android tests passed, zero reused |
| `neighbors-jvm-suites.tsv` | 64 JVM tests in five suites passed |

Every report has zero errors/skips. The final passing reports have zero failures. The Android campaign includes the six-suite Tracks profile plus complete ordinary/actual-200% authoring and archived-history/recovery, plus two ordinary-text numeric Insights journeys. Both consistency regressions now belong to the permanent Tracks profile. Source inventory is 630 JVM / 1000 Android tests (1,630); the 299-state native catalog and 38 font fixtures are unchanged by this data-only correction.

Final `scripts/check --ready` passes 344 JVM tests in 34 suites, both fast/full check-wrapper fixtures, Android-test compilation, lint, debug packaging and static guards. Exact output and suite counts are retained in `ready.log` and `ready-jvm-suites.tsv`; `SHA256SUMS` covers every retained file.

Reproduce the two observer tests on a selected disposable emulator:

```sh
ANDROID_SERIAL=emulator-5554 WHIP_ANDROID_TEST_SLOT=astra-projections-after \
  ./gradlew --no-configuration-cache :app:connectedDebugAndroidTest \
  '-Pandroid.testInstrumentationRunnerArguments.class=com.whip.app.TrackProjectionConsistencyTest' \
  -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true

ANDROID_SERIAL=emulator-5554 scripts/qa-targeted tracks \
  --android com.whip.app.TrackAuthoringJourneyE2ETest \
  --android com.whip.app.TrackHistoryJourneyE2ETest \
  --android com.whip.app.TrackInsightsJourneyE2ETest --emulator
scripts/check --ready
```

API 37 used `emulator-5556` / slot `astra-projections-api37`; after that emulator stopped, API 26 used the same serial / slot `astra-projections-api26`. At most two disposable emulators ran. To repeat the failing baseline, overlay the new consistency test on application source `194ffa0`; no production test seam or capture override is needed.

No composable, visual asset, schema, stored-data format, release version or physical device changed. The preceding authoring visual corpus remains the design evidence; this directory adds observable/persistence evidence, not a new visual acceptance claim. CSV export and Entry mutation/preparation already used transactional reads; no malformed export or stored-data loss was found. Full CSV repository/ViewModel/recovery, Entry hierarchy/wide forms, large histories and whole-product acceptance remain open.
