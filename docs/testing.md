# Verification and release checklist

Local quality gate:

```bash
scripts/check
scripts/check --device
```

`scripts/check` is the required pre-commit gate: deterministic JVM tests,
Android-test compilation, lint, and a debug build. `--device` additionally runs
the persisted-data, migration, navigation, Compose Accessibility Test
Framework, editor-recreation, notification-action, backup fault-injection, and
adaptive-layout tests. Android instrumentation resets only the separate
`commvne.com.whip.app.debug` app, so it can run beside the signed
`commvne.com.whip.app` release
without replacing release data. Device execution remains deliberately opt-in.
CI uses `scripts/check --ci`, which also builds the minified release and
optimized Macrobenchmark target/harness, and runs the device suite on every
supported API tier.

The GitHub workflow runs the build gate and instrumentation tests on API 26,
28, and 35 emulators. A manually dispatched performance job runs the benchmark
scenarios on a fixed API 35 emulator and archives the raw reports; those numbers
are execution smoke, not physical-device performance evidence. See
[`performance.md`](performance.md). A current physical device remains the final
smoke test for platform permission surfaces, wireless install/launch, Health
Connect presence, input devices, and real folding transitions.

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
and emits an unsigned APK suitable for CI verification.

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
habit and goal reminders use one per logical reminder. Optional task location
cues use Android geofences instead of GPS polling. Rest timers use one request
per session. Finishing,
archiving, rescheduling, or editing an item cancels/replaces its prior work.
Quiet hours shift reminder delivery to the configured end of the quiet window.
No polling service or wakelock is used.

## Feature coverage matrix

Every product area has fast domain coverage and at least one persisted or UI
path. New behavior must add its regression to the narrowest applicable suite
and update this matrix if it introduces a new feature area.

Current baseline: 354 product tests—149 fast JVM tests and 205 Android
instrumentation tests—plus 9 Macrobenchmark/Baseline Profile scenarios, lint,
debug/release/benchmark builds, and the three-API CI emulator matrix.

| Product capability | Deterministic/domain coverage | Persisted/integration coverage | UI/E2E coverage |
| --- | --- | --- | --- |
| App launch, Home, and all primary areas | supporting projections | real application repositories | `WhipAppTest`, `WhipNavigationTest`, `CoreFeatureJourneyE2ETest` |
| Global add, progressive editors, and goal templates | validation in domain/repository suites | creation paths for each domain | `WhipComposeSemanticsTest` |
| One-shot, anytime, completed, and archived tasks | recurrence and visibility suites | `TaskRepositoryTest` | navigation, Home, and task-area journeys |
| Daily/every-N/weekday/month/year and completion-relative recurrence, reschedule/skip | `RecurrenceEngineTest`, `TaskUpcomingVisibilityTest` | per-occurrence rows and power fields in `TaskRepositoryTest` and full backup | task-area semantics path |
| Task priorities, area/tags, named filters, multi-reminders, deadlines, smart capture, agenda/calendar, and bulk actions | `TaskQuickCaptureParserTest`, `TaskReminderRulesTest`, `PowerUserSettingsTest` | v14 planning migration and `TaskRepositoryTest` round trip | calendar/filter/editor semantics in `WhipComposeSemanticsTest` |
| Subtasks, snapshots, promotion, and percentage | `TaskProgressTest` | `TaskRepositoryTest` | real Home/task rendering in the core journey |
| Task archive, unified occurrence recovery, and permanent deletion | recurrence visibility rules | cascade plus cross-domain cleanup in `TaskRepositoryTest` and `LinkRepositoryTest` | confirmation, reopen, undo skip, reset date, and cadence explanation in `TaskDeletionUiTest` |
| Habit modes, targets, schedules, streaks, range/value quick buttons, and history | `HabitRulesTest`, `NumericSequenceTest` | `HabitRepositoryTest` | Home and Habits render through real flows |
| Goal types, windows, pace, ranges, consistency, and milestones | `GoalRulesTest` | `GoalRepositoryTest` | Home and Goals render through real flows |
| First-class productivity Areas, global scope, stable assignment, merge/archive, and inline creation | `AreaScopeTest`, `PowerUserSettingsTest` | v27 migration, `MeasurementTaxonomyRepositoryTest`, backup/merge/CSV coverage | `AreaFeatureUiTest`; Home/Tasks/Habits/Goals/Search/Review routing and accessibility suites |
| Gym exercises, sessions, sets, equipment-native units/increments, timer, next-set focus, compact rows, plate presets, routine-day shortcuts, and history | `GymCalculationsTest`, `DisplayUnitsTest`, `NumericSequenceTest`, `PowerUserSettingsTest` | `GymRepositoryTest`, settings round trip | pound hardware switching, input-before-save ordering, a single incomplete-set completion path, and accessible reorder actions in `GymPowerInputUiTest`; active workout and exercise render through real flows |
| Exercise-specific machines, compact/custom mass stacks, ordinal pin/level ranges, exact-value stepping, machine-scoped history, routines, records, and graphs | `GymAnalyticsTest`, `NumericSequenceTest` | `GymRepositoryTest`, `RoutineRepositoryTest`, v15 migration, backup/CSV coverage | `GymPowerInputUiTest`; machine library and unit explanation in `WhipComposeSemanticsTest` |
| Scalable routine composer, duplicate placements, user-owned rep-scheme library, structured rep-range prescriptions, equipment bindings, supersets, day lifecycle, records, accessible graphs, e1RM, and volume | `GymAnalyticsTest`, `RoutineBuilderStateTest`, `PowerUserSettingsTest` | settings encoding, `RoutineRepositoryTest`, v23 migration, and full-domain/settings backup round trip | 205-exercise search/multi-select, blank/add/apply/edit/delete rep schemes, and nested exercise/machine creation in `RoutineBuilderUiTest`; chart summary/semantics in `CoreFeatureJourneyE2ETest` |
| Dependency-aware habit/goal/gym deletion | source/derived-data rules | cascade, orphan, graph-preset, PR, and history-preservation checks in `DomainDeletionCoordinatorTest` | impact confirmation in domain screens and `TaskDeletionUiTest` |
| Links, contributions, triggers, and workout → weekly habit | `CrossDomainInsightsTest`, flexible-period event regression | idempotence, live ordering, overrides, exclusion, cycles, undo/resume, legacy rules in `LinkRepositoryTest` | linked domain projections enter normal screens |
| Measurements and custom units, including custom mass-to-kilogram factors | `MeasurementTest`, `DisplayUnitsTest` | habit, goal, gym, and link repository suites | dimension-specific explanation in Settings |
| Settings, units, week start, timezone, quiet hours, and notification diagnostics | `AppSettingsTest`, `QuietHoursTest` | settings repositories and background schedulers | diagnostics and test-notification controls in `WhipComposeSemanticsTest` |
| Exact-record search, review, Whip-native restore/merge, and CSV export | search/insight and portable-backup suites | transactional backup repositories and CSV assertions | all active/archived search domains in `GlobalSearchRoutingTest`; Settings asserts third-party import is absent |
| Foldables, tablets, phones, contextual pane, and pane fullscreen | `AdaptiveLayoutTest` | posture selection | `AdaptiveWhipScreenTest` covers separating and flat/non-separating book folds, editor/detail containment, and duplicate-header suppression; editor recreation plus physical Fold matrix |
| Internal persistence across restart | Room schema and migration model | `PersistentStorageE2ETest` closes and reopens a file-backed DB | `CoreFeatureJourneyE2ETest` recreates the Activity |
| Full backup/restore, encryption, and tamper safety | filename/retention policy and codec rules | all first-class domains, routines, links, settings, checksum/authentication rejection, and recovery rollback | restore preview, passphrase, and folder controls |
| Portable folder, crash-safe staging, retention, and scheduled backup | `PortableBackupPolicyTest` | manager recreation, staged write/read/rename/read verification, corrupt cleanup, validate-before-prune, empty-source protection, unique WorkManager job | Settings portable-backup journey |
| Migrations from every supported version | schema fixtures | `WhipDatabaseMigrationTest` | launch after migrated DB is covered by release smoke |
| Health-backed Habits and Goals | goal/habit/source rules | fake-provider import/update/delete, provenance, link rebuild | source choice and Settings reconciliation paths |
| Notification actions and reminder health | reminder/outcome rules | action ledger idempotency and scheduler reconstruction | per-channel health, exact-record routes, permission-ungranted creation, and explicit opt-in request paths |
| Long histories and bounded graphs | 100,000-point `LargeHistoryRegressionTest` | bounded queries/projections | graph screen smoke and `DenseDataBenchmark` |
| Accessibility, locale, and large text | localized number/range rules | Compose Accessibility Test Framework on API 34+ | labeled actions, live/error semantics, 200% font and RTL adaptive tests |

Platform-owned surfaces—notification permission prompts, Health Connect's
system picker, and physical folding—retain a physical smoke check because an
app test cannot faithfully replace those operating-system/device interactions.
Their underlying schedule, conversion, persistence, and adaptive-layout rules
remain automated.

Android's document-provider picker is also platform-owned. Tests therefore
exercise the picker-facing Settings UI and a deterministic fake document
provider for every post-selection behavior: persisted configuration, write,
read-back verification, corruption cleanup, retention, restart, and scheduling.

## Physical Fold 8 Ultra release matrix

Run this after all automated gates pass and before installing a release as the
daily driver. Test dark, light, and dynamic color at 100%, 150%, and 200% font
scale where applicable:

- compact outer display, flat expanded display, book posture, tabletop posture,
  split pane, full pane, continuous resize, and multi-window;
- start a dirty Task, Habit, Goal, Exercise, Machine, Routine, and active-set
  edit, then rotate/fold/unfold with the IME open and verify draft, selection,
  scroll, date/filter, dialog, and focus continuity;
- TalkBack linear traversal and actions, Switch Access, hardware
  Tab/Shift-Tab/Enter/Escape, mouse, and stylus with no duplicate saves;
- notification denial, rationale/retry, each Task/Habit/Goal/rest channel
  disabled separately, Snooze/increment/complete actions, reboot, timezone and
  DST change, and battery-restriction diagnostics;
- Health Connect grant/revoke, backfill preview, provider edit/delete, and exact
  provenance; and
- signed release upgrade, plaintext/encrypted backup round trip, provider
  disconnect/reconnect, wireless deployment, full workout journey, and
  benchmark evidence capture.

The release APK is not considered accepted merely because it builds. Record
failures in `PLAN.md`; fix and rerun the affected automated and physical rows.
