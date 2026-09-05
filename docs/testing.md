# Verification and release checklist

Local quality gate:

```bash
# Explain or execute checks selected from the current working-tree changes.
scripts/check --explain
scripts/check
ANDROID_SERIAL=emulator-5554 scripts/check --emulator
# Complete JVM/static/release-build compatibility gate; no device is required.
scripts/check --full

# Qualify one frozen source tree with complete fresh evidence.
ANDROID_SERIAL=emulator-5554 scripts/candidate
scripts/candidate verify

# Explicit development selectors remain available.
scripts/coverage
scripts/qa-targeted gym531
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted gym531 --emulator
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android com.whip.app.RoutineRepositoryTest#testName --repeat 3
```

`scripts/check` is the primary development/change gate. It consumes Git
name-status changes, or explicit `--base`, `--path`, or `--changes-file` inputs,
then explains every route and unions/deduplicates named profiles and exact
JVM/Android selectors. Documentation-only, JVM-test-only, Android-test-only,
feature-domain, and shared UI/core changes stay proportionate. Deletions and
renames route both affected names. Unknown production, build/configuration,
benchmark, quality-register, automation, and harness paths fail closed and
require `scripts/candidate`.

Without `--emulator`, selected Android tests compile but do not execute. With
`--emulator`, the shared engine executes only the selected classes through the
emulator guard. `scripts/qa-targeted` remains the explicit-selector development
tool: profiles may be unioned, exact `--jvm` and `--android Class#method`
selectors are supported, and `--repeat` is reserved for timing investigations.
Development evidence is not a release claim.

`scripts/check --full` preserves the historical complete local gate used by
release tooling: complete JVM coverage, Android-test compilation, lint/static
guards, debug and release APKs, release AAB, benchmark builds, merged-manifest
safety, and release application/version metadata validation. With no
`--emulator` it neither reads `ANDROID_SERIAL` nor invokes candidate or
instrumentation code. `--emulator` explicitly adds the complete reusable
Android inventory, and `--emulator --fresh-emulator` explicitly disables batch
reuse. None of these compatibility forms creates frozen-candidate evidence;
only an explicit `scripts/candidate` command can do that.

All instrumentation entry points require one explicit `ANDROID_SERIAL`. The
selected target must report `device` state and `ro.boot.qemu=1`; physical,
offline, unauthorized, ambiguous, blank, managed, and other non-connected test
targets fail closed. The root Gradle guard applies the same rule to direct app
and benchmark `connected*AndroidTest` tasks, while Android-test compilation and
assembly remain device-independent.

`scripts/android-test-engine` is the sole Android inventory, batching, signature,
and result-accounting implementation used by check and coverage. Reusable
evidence requires exact production/build, debug APK, Android-test APK, runner,
emulator, shared-support, requested-selector, and class-file signatures. A
localized test-only edit invalidates only its batch; a production change
invalidates every batch. Each cached record must prove the exact
requested/executed class count, nonzero tests, and zero failures/skips. Fresh
and coverage modes never consume that cache.

Android instrumentation resets only the separate
`commvne.com.whip.app.debug` app, so it can run beside the signed
`commvne.com.whip.app` release
without replacing release data. The gate refuses physical hardware and requires
exactly one connected disposable API 34+ emulator, preventing a test from
clearing or otherwise disturbing a personal phone. `--device` remains an alias
for compatibility but has the same emulator-only guard. Execution remains
deliberately opt-in. Instrumentation classes run in bounded batches so Compose
and graphics state is released between runner processes. The graphics-heavy
interaction class runs first against the fresh emulator; every `*Test.kt` class
is still included, and the gate fails if a test file does not declare the
matching top-level class. The whole-app data-epoch reset class runs alone and
last. At the current 90-class baseline this is exactly 11 runner processes: one
graphics process, nine batches of at most ten ordinary classes, and one reset
process.

`scripts/candidate` is the only frozen-candidate authority. It snapshots every
tracked or unignored repository input, then runs complete JVM coverage,
complete fresh Android E2E coverage, debug and release-vital lint, static
guards, and debug/release APK, release AAB, and benchmark builds. Repository
drift is checked between phases and inside every Android batch. Compilation and
packaging may remain incrementally up to date; instrumentation must produce new
XML after each batch marker, and coverage additionally requires exactly one new
nonempty execution-data file per process. Success is published atomically under
`build/candidate-evidence/runs/`. `scripts/candidate verify` rejects source
drift, evidence tampering, any missing required evidence file or manifest field,
non-fresh Android manifests/aggregates, rewritten checksum inventories, missing
or changed artifacts, malformed pointers, and incomplete manifests. Each
accepted run retains its merged manifest and release `output-metadata.json`
inside the checksummed evidence directory. Verification semantically rechecks
the retained manifest for retired location permissions and the retained release
metadata for the expected application ID and source version code, including
when a checksum inventory has been recomputed after tampering.

Emulator screenshots, UI hierarchy dumps, traces, and other inspection
artifacts must be created with `scripts/device-artifacts`. The tool stores
user-visible development files under `/storage/emulated/0/whip-debug` and
shell-only tooling under `/data/local/tmp/whip-debug`; it never writes directly
to either storage root. Normal release exports remain user-initiated through
Android's document picker and should use `/storage/emulated/0/whip` when local
shared storage is desired.

The Macrobenchmark fixture communicates seed readiness through its visible
benchmark-only Activity. Benchmark JSON, messages, and Perfetto traces are
collected by the Android Gradle plugin under `benchmark/build/outputs`; Whip's
harness does not write status files to a device storage root.

The frozen-candidate gate also builds the minified release and optimized
Macrobenchmark target/harness. Release
compatibility uses disposable API 26 (minimum), API 34 (typical phone/full
instrumentation), and API 37 (target/latest, large screen) emulators. Emulator
benchmark runs are execution and regression smoke, not claims about retail
hardware performance. See [`performance.md`](performance.md). Platform-owned
permission, document-provider, notification, Health Connect, rotation, and
adaptive-window surfaces are exercised on those emulators; physical hardware
is optional supplementary evidence and is never a release-gate prerequisite.

The stable headless API 34 configuration used by the August 2026 release gate
disables host Vulkan and uses the software renderer. This avoids treating a
host/QEMU RenderThread failure as an application failure while still exercising
the complete Compose instrumentation suite:

```bash
$ANDROID_HOME/emulator/emulator @whip_api34 \
  -no-window -no-audio -no-boot-anim \
  -no-snapshot-load -no-snapshot-save \
  -gpu software -feature -Vulkan
ANDROID_SERIAL=emulator-5554 scripts/candidate
```

Release signing is opt-in and never checked into the repository. Set all four
variables before building a signed release:

```bash
export WHIP_KEYSTORE_FILE=/absolute/path/to/whip-release.jks
export WHIP_KEYSTORE_PASSWORD='...'
export WHIP_KEY_ALIAS='...'
export WHIP_KEY_PASSWORD='...'
./gradlew assembleRelease
```

Without them, `assembleRelease` still performs the R8/resource-shrinking build
and emits an unsigned APK suitable for local verification.

On the configured WSL workstation, `WHIP_DEVICE=SERIAL scripts/device
release-deploy` first proves that the explicitly selected target is connected
physical hardware, then reads the
mode-600 key and password file under `/root/.android/whip`, exports those values
only to the Gradle child process, builds the signed release, installs it with
`adb -s SERIAL install -r`, and launches it. `WHIP_DEVICE=SERIAL scripts/device
release-install` reuses an
already-built release APK. Never copy the keystore or password into this
repository, and keep a secure offline copy of both.

## Query and background-work review

Frequently joined foreign keys and lookup fields are indexed in the Room
entities: stable UUIDs, archive/status fields, task/occurrence keys, measurement
and source IDs, workout session/exercise IDs, habit dates, and goal status. UI
projections consume observable table flows and perform
bounded presentation filtering. Graph rendering downsamples to at most 200
points without deleting source history.

Task reminders use one replaceable WorkManager request per configured offset;
habit and goal reminders use one per logical reminder. Rest timers use one
request per session. Finishing,
archiving, rescheduling, or editing an item cancels/replaces its prior work.
Quiet hours shift reminder delivery to the configured end of the quiet window.
No polling service or wakelock is used.

## Feature coverage matrix

Every product area has fast domain coverage and at least one persisted or UI
path. New behavior must add its regression to the narrowest applicable suite
and update this matrix if it introduces a new feature area.

Current baseline: 1537 product tests—612 fast JVM tests and 925 Android
instrumentation tests—plus 9 Macrobenchmark/Baseline Profile scenarios, lint,
debug/release/benchmark builds, and the disposable API 34 emulator suite. API
26 and API 37 compatibility runs cover the minimum and target/latest platform;
the gate must not claim configurations that were not run.

`scripts/coverage` generates AGP/JaCoCo's deterministic report and enforces the
audited domain/core floors. `scripts/coverage --emulator` additionally runs the
complete suite using the same 11-process graphics-first/ten-class/reset-last
topology as `scripts/check`. Every nested connected task revalidates its explicit
emulator target. After every process the script requires fresh XML proving the
exact requested class set, nonzero execution, and zero failures/skips, plus
exactly one fresh nonempty `.ec` copied into that run's unique evidence
directory. It rejects source/build drift, stale results, cached execution, and
partial coverage data. The public Gradle
`createBatchedDebugAndroidTestCoverageReport` task then generates XML/HTML from
the union of all copied `.ec` files before the E2E coverage floors run.
Generated Room `*_Impl.kt` code is excluded only from the product-code
aggregate; its behavior is still executed through the repository tests and
remains visible in the raw report. Reports are written to
`app/build/reports/coverage/test/debug/index.html` and
`app/build/reports/coverage/androidTest/debug/connected/index.html`.

To roll back this harness, revert the router, check/candidate/coverage wrappers,
shared Android engine, repository-state helper, executable fixtures (including
`scripts/test-check-full`), tests, and documentation
together while retaining `scripts/android-target-guard` and
the module-local Gradle guards. Generated `build/candidate-evidence/`,
`build/coverage-results-*`, and `build/instrumentation-results-*` directories
are disposable local evidence, but accepted candidate evidence must not be
reused after rollback. This tooling change has no database, schema, installed
package, or user-data transition to reverse.

Line/branch percentages are not treated as a substitute for product behavior.
The machine-checked cause/effect register in
`docs/quality/e2e-coverage.tsv` requires every first-class capability to name a
real UI/integration happy path, alternate or failure path, persistence or
recreation path, and accessibility/adaptive evidence. Platform-owned surfaces
retain explicit emulator smoke items where deterministic application assertions
cannot replace the operating system UI.

| Product capability | Deterministic/domain coverage | Persisted/integration coverage | UI/E2E coverage |
| --- | --- | --- | --- |
| App launch, Home, and all primary areas | supporting projections | real application repositories | `WhipAppTest`, `WhipNavigationTest`, `CoreFeatureJourneyE2ETest` |
| Global add, basic/advanced editors, Goal templates, and Goal-value shortcut | validation in domain/repository suites | creation paths for each domain | `WhipComposeSemanticsTest` |
| Inbox/undated, one-shot, completed, and archived tasks | recurrence and visibility suites | `TaskRepositoryTest` | navigation, Home, and task-area journeys |
| Daily/every-N/weekday/month/year and completion-relative recurrence, reschedule/skip | `RecurrenceEngineTest`, `TaskUpcomingVisibilityTest` | per-occurrence rows and power fields in `TaskRepositoryTest` and full backup | task-area semantics path |
| Task priorities, area/tags, named filters, multi-reminders, deadlines, smart capture, agenda/calendar, and bulk actions | `TaskQuickCaptureParserTest` covers supported phrases, exact highlight ranges, invalid input, conflicts, and date boundaries; `TaskWorkspacePolicyTest` covers literal/enabled quick-add drafts; `SmartTaskCaptureVisualTransformationTest` verifies every assumed source range receives the visible style; plus `TaskReminderRulesTest` and `PowerUserSettingsTest` | `TaskRepositoryTest` round trip and `AppSettingsPersistenceTest` preserve saved data and the opt-in | `EditorDependencyUxTest` verifies highlight announcements and review/apply/save; `ProductivityCreationJourneyE2ETest` toggles the real setting, checks enabled examples, saves a highlighted one-time Quick Capture, proves disabling restores literal capture, then reviews and persists a repeating Task through Add Details; calendar/filter semantics remain in `WhipComposeSemanticsTest` |
| Subtasks, snapshots, promotion, and percentage | `TaskProgressTest` | `TaskRepositoryTest` | real Home/task rendering in the core journey |
| Task archive, unified occurrence recovery, and permanent deletion | recurrence visibility rules | cascade plus cross-domain cleanup in `TaskRepositoryTest` and `DomainDeletionCoordinatorTest` | confirmation, reopen, undo skip, reset date, and cadence explanation in `TaskDeletionUiTest` |
| One balanced summary-first row layout for Tasks, Habits, Goals, and Tracks | `ItemDisclosureStateTest` verifies collapsed initial state, independent multi-row expansion, collapse-all, and restoration; `UiDesignArchitectureTest` locks the shared medium-shape, 12×10 dp card inset, 6 dp internal rhythm, and 8 dp collection gap | `AppSettingsPersistenceTest` and `BackupRepositoryTest` prove density is no longer persisted or exported; backup contract 24 rejects older representations | `ProductivityCardDesignUiTest` verifies elapsed Goals also begin collapsed, simultaneous disclosures, clean workspace-tab resets, complete expanded controls, responsive 200% text, and 48 dp targets; `TrackWorkspaceUiTest` verifies summary-row parity plus selection/reorder/adaptive behavior; Settings tests verify the retired toggle is absent |
| Habit input modes, target rules, schedules, streaks, neutral Skip/Undo, derived missing days, numeric-only range/value quick buttons, and history | `HabitRulesTest`, `NumericSequenceTest` | `HabitRepositoryTest` proves Checklist/Check-off habits discard irrelevant numeric quick-add state; migration/backup coverage proves skips create no values while reminders observe the occurrence | `EntitySaveCoordinatorUiTest` proves switching to Checklist removes preset/range controls and saves canonical nonnumeric state; `HabitSkipJourneyE2ETest` verifies confirmation, card state, History, Insights, and Undo through real flows |
| Habit timer identity, canonical Duration conversion, monotonic elapsed display, reboot/reopen review, and widget reachability | `HabitTimerDomainTest`, `HabitTimerClockTest`, `HabitTimerPresentationTest` | `HabitRepositoryTest`, `DataEpochBoundaryTest`, and `BackupRepositoryTest` cover idempotent requests, stale actions, unit snapshots, the epoch-6 boundary, and portable/private/merge profiles | `ProductivityCardDesignUiTest` verifies Start/Stop/Review semantics, 48 dp targets at 320dp/200% text, and editable Stop/Continue/Discard recovery; widget suites cover filtered active timers |
| Goal types, constrained aggregations, honest pace comparison, ranges, consistency, milestones, exact elapsed-time counters, orthogonal archive/lifecycle, and immutable closure/reset history | `GoalRulesTest`, `GoalMutationCommitTest` | `GoalRepositoryTest`, `IdentityEmojiTest`, `DataEpochBoundaryTest`, and `BackupRepositoryTest` cover stale boundaries, frozen terminal outcomes, correction-safe history, idempotent merge, and the epoch-6 contract | `GoalSecondaryMutationUiTest` and `ProductivityCardDesignUiTest` cover request-owned dialogs, custom units, archived actions, deletion impact, large text, and frozen elapsed/milestone outcomes |
| First-class Tracks, arbitrary typed Fields, exact authored-definition/removal and Entry mutation boundaries, fractional-increment Scales, explicit sorting, 10,000-Entry calculations, typed filters, numeric summaries, paging, exact process-safe CSV batches, and choice replacement | `TrackDomainTest`, `TrackCsvBatchIdentityTest`, `TrackCsvInputBoundaryTest`, `TrackEditorStateTest`, `TransientFeedbackTest`, `SortDirectionTest`, and `EditorFeatureIntegrityTest` | `TrackDefinitionIntegrityTest`, `TrackEntryIntegrityTest`, `TrackCsvImportIntegrityTest`, `TrackRepositoryTest`, and `BackupRepositoryTest` preserve exact Track/Entry/value history, deterministic batch/row identity, transactional Entry/value/FTS/receipt outcomes, process-safe exact retry, maximum 5,000-row/100,000-cell custom-unit imports, current reference validation, and malformed/tampered-request rejection | `TrackCsvImportRecoveryViewModelTest` and `TrackCsvImportUiTest` cover receipt-first restoration, unreadable URI, transient Track-load retry, frozen mapping, authoritative errors, full input shielding, and 320dp/200% reachability; `TrackDefinitionMutationUiTest`, `TrackEntryMutationUiTest`, `InteractionControlUiTest`, `TrackWorkspaceUiTest`, and `AdaptiveWhipScreenTest` cover exact mutation ownership, typed input/unit semantics, navigation, RTL, large text, and adaptive layouts |
| First-class productivity Areas, global scope, stable assignment, merge/archive, and inline creation | `AreaScopeTest`, `PowerUserSettingsTest` | `MeasurementTaxonomyRepositoryTest`, backup/merge/CSV coverage | `AreaFeatureUiTest`; Home/Tasks/Habits/Goals/Search/Review routing and accessibility suites |
| Gym exercises, sessions, sets, equipment-native units/increments, timer, next-set focus, compact rows, plate presets, routine-day shortcuts, and history | `GymCalculationsTest`, `DisplayUnitsTest`, `NumericSequenceTest`, `PowerUserSettingsTest` | `GymRepositoryTest`, settings round trip | pound hardware switching, input-before-save ordering, a single incomplete-set completion path, and accessible reorder actions in `GymPowerInputUiTest`; active workout and exercise render through real flows |
| Exercise-specific machines, compact/custom mass stacks, ordinal pin/level ranges, exact-value stepping, machine-scoped history, routines, records, and graphs | `GymAnalyticsTest`, `NumericSequenceTest` | `GymRepositoryTest`, `RoutineRepositoryTest`, backup/CSV coverage | `GymPowerInputUiTest`; machine library and unit explanation in `WhipComposeSemanticsTest` |
| Scalable routine composer, duplicate placements, user-owned rep-scheme library, structured rep-range prescriptions, equipment bindings, supersets, day lifecycle, records, accessible graphs, e1RM, and volume | `GymAnalyticsTest`, `RoutineBuilderStateTest`, `PowerUserSettingsTest` | settings encoding, `RoutineRepositoryTest`, and full-domain/settings backup round trip | 205-exercise search/multi-select, blank/add/apply/edit/delete rep schemes, and nested exercise/machine creation in `RoutineBuilderUiTest`; chart summary/semantics in `CoreFeatureJourneyE2ETest` |
| First-class 5/3/1 programs, standard or user-chosen main exercises, actual/e1RM-derived or direct Training Maxes, adjustable TM percentages, PR sets or 5s PRO, FSL/SSL/BBB/BBS, optional Jokers, assistance roles, structured test weeks, and standard or performance-informed per-exercise cycle decisions | `FiveThreeOneProgressionTest`, `FiveThreeOneCycleReviewTest`, `RoutineBuilderStateTest`, `GymUxRulesTest`, and central 300 lb/kg/lb calculation tests cover rounding, effort evidence, independent exposures, failed TM tests, unique repeated-exercise TM-test ownership, timer boundaries, and advisory increase/decrease/hold behavior | `RoutineRepositoryTest` proves immutable prescriptions, failed-test identity, exactly one TM-test prescription per logical exercise, transactional decision recomputation/auditing, custom chosen exercises, repeated-exercise consistency, and immutable History; `BackupRepositoryTest` validates the current clean data contract | `RoutineBuilderUiTest` and `FiveThreeOneCycleReviewUiTest` cover the top-level template entry, neutral current-Exercise selection distinct from optional workout substitutes, actual/e1RM source selection, explicit Apply before derived provenance can be saved—even when rounding is unchanged—adjustable TM percentage, a three-exercise Bench/Deadlift/Zercher program, phase structure, ordinary-routine Training Max discovery, responsive editing, explicit standard/suggestion/hold/ignore/custom decisions, and 200% text reachability |
| Dependency-aware habit/goal/gym deletion | source/derived-data rules | cascade, orphan, graph-preset, PR, and history-preservation checks in `DomainDeletionCoordinatorTest` | impact confirmation in domain screens and `TaskDeletionUiTest` |
| Measurements and custom units, including custom mass-to-kilogram factors | `MeasurementTest`, `DisplayUnitsTest` | habit, goal, and gym repository suites | `InteractionControlUiTest` creates and selects a unit from the in-editor chooser; Settings retains dimension-specific management |
| Settings, units, week start, timezone, quiet hours, and notification diagnostics | `AppSettingsTest`, `QuietHoursTest`, `SettingsCauseEffectContractTest` | every setting is registered in `docs/quality/settings-cause-effect.tsv`; settings repositories and background schedulers | diagnostics and test-notification controls in `WhipComposeSemanticsTest` |
| Exact-record search, review, Whip-native restore/merge, and CSV export | search/insight and portable-backup suites | transactional backup repositories, exact current-epoch backup validation, idempotent stable-history merge, and CSV assertions | all active/archived search domains in `GlobalSearchRoutingTest`; Settings asserts third-party import is absent |
| Foldables, tablets, phones, contextual pane, pane fullscreen, and edge-to-edge overlay ownership | `AdaptiveLayoutTest` | posture selection | `AdaptiveWhipScreenTest` covers separating and flat/non-separating book folds, editor/detail containment, and duplicate-header suppression; `InteractionControlUiTest` verifies that destination-sized backgrounds own the window while content alone receives safe insets; editor recreation plus compact/large-screen emulator matrix |
| Canonical local persistence and clean data boundary | checked-in schema 46 and epoch-boundary rules | `DataEpochBoundaryTest`, `DataEpochResetIntegrationTest`, and `PersistentStorageE2ETest` prove older local schemas never enter the runtime, a person-confirmed fresh start creates schema 46, and a file-backed canonical database reopens cleanly | fresh-start confirmation and normal app recreation journeys |
| Full backup/restore, encryption, and tamper safety | filename/retention policy and codec rules | all first-class domains, routines, settings, checksum/authentication rejection, recovery rollback, and exact epoch/version rejection | restore preview, passphrase, and folder controls |
| Portable folder, crash-safe staging, retention, and scheduled backup | `PortableBackupPolicyTest` | manager recreation, staged write/read/rename/read verification, corrupt cleanup, validate-before-prune, empty-source protection, unique WorkManager job | Settings portable-backup journey |
| Health-backed Habits and Goals | goal/habit/source rules | fake-provider import/update/delete, provenance, and reconciliation | source choice and Settings reconciliation paths |
| Notification delivery, actions, and reminder health | exact versioned claims, live Task/Habit/Goal eligibility, quiet-hour/time-zone rules, malformed/early/stale rejection, definition fingerprints, and invalidation policy | awaited scheduler reconstruction, source-backed Habit synchronization, production mutation linearization, serialized Settings snapshots, and durable deletion cleanup across rollback/process interruption | real worker posting/non-posting, exact idempotent notification actions, time broadcasts, per-channel health, exact-record routes, permission-ungranted creation, and explicit opt-in request paths |
| Long histories and bounded graphs | 100,000-point `LargeHistoryRegressionTest` | bounded queries/projections | graph screen smoke and `DenseDataBenchmark` |
| Accessibility, interaction grammar, locale, and large text | localized number/range rules | Compose Accessibility Test Framework on API 34+ | `InteractionControlUiTest` verifies roles, state, 48 dp targets, scrollable tabs, 200% font, and RTL; `ProductivityCardDesignUiTest` locks Task/Habit/Goal identity, action, and edit columns to one hierarchy; adaptive suites cover labeled actions and live/error semantics |

Platform-owned surfaces—notification permission prompts, Health Connect's
system picker, notification shade, and adaptive window transitions—retain an
emulator smoke check because an app assertion cannot replace those operating-
system interactions. Their underlying schedule, conversion, persistence, and
adaptive-layout rules remain automated.

Android's document-provider picker is also platform-owned.
`SettingsBehaviorUiTest` now opens and cancels the real emulator picker, while a
deterministic fake document provider covers every post-selection behavior:
persisted configuration, write, read-back verification, corruption cleanup,
retention, restart, and scheduling.

## Emulator-only release matrix

Run this after all automated gates pass. Use the API 26 small-screen, API 34
typical-phone, and API 37 large-screen AVDs. Test dark and light themes at 100%
and 200% font scale where applicable:

- compact outer display, flat expanded display, book posture, tabletop posture,
  split pane, full pane, continuous resize, and multi-window;
- start a dirty Task, Habit, Goal, Exercise, Machine, Routine, and active-set
  edit, then rotate/fold/unfold with the IME open and verify draft, selection,
  scroll, date/filter, dialog, and focus continuity;
- accessibility-service linear traversal and actions, hardware
  Tab/Shift-Tab/Enter/Escape, and mouse input with no duplicate saves;
- notification denial, rationale/retry, each Task/Habit/Goal/rest channel
  disabled separately, Snooze/increment/complete actions, reboot, timezone and
  DST change, and battery-restriction diagnostics;
- Health Connect grant/revoke, backfill preview, provider edit/delete, and exact
  provenance; and
- release-equivalent upgrade, plaintext/encrypted backup round trip, provider
  disconnect/reconnect, full workout journey, and benchmark evidence capture.

The release APK is not considered accepted merely because it builds. Record
failures in `PLAN.md`; fix and rerun the affected automated and emulator rows.
