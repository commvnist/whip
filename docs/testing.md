# Verification and release checklist

Local quality gate:

```bash
scripts/check
scripts/check --full
scripts/check --emulator
scripts/coverage
ANDROID_SERIAL=emulator-5554 scripts/coverage --emulator
```

`scripts/check` is the required pre-commit gate: deterministic JVM tests with
an enforced coverage floor,
Android-test compilation, lint, and a debug build. `--emulator` additionally runs
the persisted-data, navigation, Compose Accessibility Test
Framework, editor-recreation, notification-action, backup fault-injection, and
adaptive-layout tests. Android instrumentation resets only the separate
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
matching top-level class.

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

`scripts/check --full` is the comprehensive local gate; it also builds the
minified release and optimized Macrobenchmark target/harness. Release
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
scripts/check --emulator
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

On the configured WSL workstation, `scripts/device release-deploy` reads the
mode-600 key and password file under `/root/.android/whip`, exports those values
only to the Gradle child process, builds the signed release, installs it with
`adb install -r`, and launches it. `scripts/device release-install` reuses an
already-built release APK. Never copy the keystore or password into this
repository, and keep a secure offline copy of both.

## Query and background-work review

Frequently joined foreign keys and lookup fields are indexed in the Room
entities: stable UUIDs, archive/status fields, task/occurrence keys, metric and
source IDs, workout session/exercise IDs, habit dates, goal status, and link
source/target IDs. UI projections consume observable table flows and perform
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

Current baseline: 566 product tests—257 fast JVM tests and 309 Android
instrumentation tests—plus 9 Macrobenchmark/Baseline Profile scenarios, lint,
debug/release/benchmark builds, and the disposable API 34 emulator suite. API
26 and API 37 compatibility runs cover the minimum and target/latest platform;
the gate must not claim configurations that were not run.

`scripts/coverage` generates AGP/JaCoCo's deterministic report and enforces the
audited domain/core floors. `scripts/coverage --emulator` additionally runs the
complete suite in one isolated runner, emits the real application E2E report,
and enforces product-code, repository, first-class-screen, Track, and Gym
non-regression floors. Generated Room `*_Impl.kt` code is excluded only from the
product-code aggregate; its behavior is still executed through the repository
tests and remains visible in the raw report. Reports are written to
`app/build/reports/coverage/test/debug/index.html` and
`app/build/reports/coverage/androidTest/debug/connected/index.html`.

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
| Task priorities, area/tags, named filters, multi-reminders, deadlines, smart capture, agenda/calendar, and bulk actions | `TaskQuickCaptureParserTest`, `TaskReminderRulesTest`, `PowerUserSettingsTest` | `TaskRepositoryTest` round trip | calendar/filter/editor semantics in `WhipComposeSemanticsTest` |
| Subtasks, snapshots, promotion, and percentage | `TaskProgressTest` | `TaskRepositoryTest` | real Home/task rendering in the core journey |
| Task archive, unified occurrence recovery, and permanent deletion | recurrence visibility rules | cascade plus cross-domain cleanup in `TaskRepositoryTest` and `LinkRepositoryTest` | confirmation, reopen, undo skip, reset date, and cadence explanation in `TaskDeletionUiTest` |
| User-selected standard cards or expandable compact list rows for Tasks, Habits, Goals, and Tracks | `CompactItemExpansionStateTest` verifies one-at-a-time and non-displacing automatic expansion; setting cause/effect contract and default behavior | `AppSettingsPersistenceTest` and `BackupRepositoryTest` preserve the preference | `ProductivityCardDesignUiTest` and `TrackWorkspaceUiTest` verify list-sized collapsed rows, independent primary actions, complete expanded controls, disclosure replacement/restoration, responsive 200% text, and 48 dp targets; `SettingsBehaviorUiTest` verifies the Appearance toggle |
| Habit input modes, target rules, schedules, streaks, neutral Skip/Undo, derived missing days, range/value quick buttons, and history | `HabitRulesTest`, `NumericSequenceTest` | `HabitRepositoryTest`, `LinkRepositoryTest`, and migration/backup coverage prove skips create no values while reminders and Automations observe the occurrence | `HabitSkipJourneyE2ETest` verifies confirmation, card state, History, Insights, and Undo through real flows |
| Goal types, constrained aggregations, honest pace comparison, ranges, consistency, milestones, and exact elapsed-time counters | `GoalRulesTest` | `GoalRepositoryTest`, `IdentityEmojiMigrationTest` | Home and Goals render through real flows; elapsed create/edit/reset controls compile in the adaptive journey |
| First-class Tracks, arbitrary typed Fields, fractional-increment Scales, explicit ascending/descending sorting, 10,000-Entry calculations, typed filters, every numeric/count aggregation, paging, CSV, choice replacement, Goal Automations, Capture/Follow-up Automations, and durable prompts | `TrackDomainTest`, `SortDirectionTest`, `EditorFeatureIntegrityTest`, `AutomationUxPolicyTest` | `TrackRepositoryTest` verifies the movie-Entry-count → Goal cause/effect across delete/restore, plus `TrackScaleMigrationTest`, `BackupRepositoryTest`, and `LinkRepositoryTest` | `AutomationConfigurationE2ETest` configures Count Entries through the real UI and verifies two Entries create exactly two Goal contributions; `InteractionControlUiTest` covers exact Scale stepping/clearing; Track navigation/adaptive semantics compile in `AdaptiveWhipScreenTest`; release-equivalent compact and large-screen emulator smoke supplies live evidence |
| First-class productivity Areas, global scope, stable assignment, merge/archive, and inline creation | `AreaScopeTest`, `PowerUserSettingsTest` | `MeasurementTaxonomyRepositoryTest`, backup/merge/CSV coverage | `AreaFeatureUiTest`; Home/Tasks/Habits/Goals/Search/Review routing and accessibility suites |
| Gym exercises, sessions, sets, equipment-native units/increments, timer, next-set focus, compact rows, plate presets, routine-day shortcuts, and history | `GymCalculationsTest`, `DisplayUnitsTest`, `NumericSequenceTest`, `PowerUserSettingsTest` | `GymRepositoryTest`, settings round trip | pound hardware switching, input-before-save ordering, a single incomplete-set completion path, and accessible reorder actions in `GymPowerInputUiTest`; active workout and exercise render through real flows |
| Exercise-specific machines, compact/custom mass stacks, ordinal pin/level ranges, exact-value stepping, machine-scoped history, routines, records, and graphs | `GymAnalyticsTest`, `NumericSequenceTest` | `GymRepositoryTest`, `RoutineRepositoryTest`, backup/CSV coverage | `GymPowerInputUiTest`; machine library and unit explanation in `WhipComposeSemanticsTest` |
| Scalable routine composer, duplicate placements, user-owned rep-scheme library, structured rep-range prescriptions, equipment bindings, supersets, day lifecycle, records, accessible graphs, e1RM, and volume | `GymAnalyticsTest`, `RoutineBuilderStateTest`, `PowerUserSettingsTest` | settings encoding, `RoutineRepositoryTest`, and full-domain/settings backup round trip | 205-exercise search/multi-select, blank/add/apply/edit/delete rep schemes, and nested exercise/machine creation in `RoutineBuilderUiTest`; chart summary/semantics in `CoreFeatureJourneyE2ETest` |
| Dependency-aware habit/goal/gym deletion | source/derived-data rules | cascade, orphan, graph-preset, PR, and history-preservation checks in `DomainDeletionCoordinatorTest` | impact confirmation in domain screens and `TaskDeletionUiTest` |
| Goal Automations, Next-Action Automations, contributions, prompts, and workout → weekly habit | `AutomationUxPolicyTest`, `CrossDomainInsightsTest`, flexible-period event regression | complete exposed action/outcome matrix, endpoint rejection, idempotence, live ordering, overrides, exclusion, cycles, Track Entry count, and undo/resume in `LinkRepositoryTest` and `TrackRepositoryTest` | `AutomationConfigurationE2ETest` covers configuration plus observable effect; linked domain projections enter normal screens |
| Measurements and custom units, including custom mass-to-kilogram factors | `MeasurementTest`, `DisplayUnitsTest` | habit, goal, gym, and link repository suites | `InteractionControlUiTest` creates and selects a unit from the in-editor chooser; Settings retains dimension-specific management |
| Settings, units, week start, timezone, quiet hours, and notification diagnostics | `AppSettingsTest`, `QuietHoursTest`, `SettingsCauseEffectContractTest` | every setting is registered in `docs/quality/settings-cause-effect.tsv`; settings repositories and background schedulers | diagnostics and test-notification controls in `WhipComposeSemanticsTest` |
| Exact-record search, review, Whip-native restore/merge, and CSV export | search/insight and portable-backup suites | transactional backup repositories, v5/v6/v7→v8 backup-data compatibility, and CSV assertions | all active/archived search domains in `GlobalSearchRoutingTest`; Settings asserts third-party import is absent |
| Foldables, tablets, phones, contextual pane, pane fullscreen, and edge-to-edge overlay ownership | `AdaptiveLayoutTest` | posture selection | `AdaptiveWhipScreenTest` covers separating and flat/non-separating book folds, editor/detail containment, and duplicate-header suppression; `InteractionControlUiTest` verifies that destination-sized backgrounds own the window while content alone receives safe insets; editor recreation plus compact/large-screen emulator matrix |
| Internal persistence and forward-compatible upgrades | checked-in pre-release schemas 1–9 and public schemas 27–28, all forward to 29 | `WhipDatabaseMigrationTest` preserves schema-1 Tasks/Links/Contributions/Automations, schema-2 Tracks, schema-9 current-domain state, schema-27 public entity-tag/Goal-snapshot data, and schema-28 checklist Habits through schema 29; it also defaults upgraded checklist Habits to final-item auto-completion and converts legacy Habit Skip/Excuse rows without retaining false measurements. `PersistentStorageE2ETest` closes and reopens a file-backed DB | signed public-0.3.7 APK upgrade retains a populated sentinel; `CoreFeatureJourneyE2ETest` recreates the Activity |
| Full backup/restore, encryption, and tamper safety | filename/retention policy and codec rules | all first-class domains, routines, links, settings, checksum/authentication rejection, and recovery rollback | restore preview, passphrase, and folder controls |
| Portable folder, crash-safe staging, retention, and scheduled backup | `PortableBackupPolicyTest` | manager recreation, staged write/read/rename/read verification, corrupt cleanup, validate-before-prune, empty-source protection, unique WorkManager job | Settings portable-backup journey |
| Health-backed Habits and Goals | goal/habit/source rules | fake-provider import/update/delete, provenance, link rebuild | source choice and Settings reconciliation paths |
| Notification actions and reminder health | reminder/outcome rules | action ledger idempotency and scheduler reconstruction | per-channel health, exact-record routes, permission-ungranted creation, and explicit opt-in request paths |
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
