# Cross-feature UX, design, and QA implementation audit

Date: 2026-08-23

This audit is based on the current source and live-build behavior. Repository screenshots were not used as evidence.

## Verified problems

| Surface | Verified cause | Implemented contract |
| --- | --- | --- |
| Track detail navigation | `Automations` competed with three peers in a narrow split pane | The visible tab is `Auto`; its accessibility name and full-page heading remain `Automations` |
| Goal detail navigation | `Completed` was longer than the equivalent state needs to be | The visible tab is `Done`; its semantic name remains `Completed` |
| Insights placement | Habits placed Insights third while Goals and Tracks placed it second | Insights is the second destination in every workspace that has an Insights destination |
| Track Entry identity | Validation required exactly one required Short Text Primary Field; the editor silently cleared every prior primary selection and stripped `showInList` | One or more required Fields of any compatible type form a composite Entry Identity; saving never clears another identity Field or its inline-label preference |
| Track duplicate warning | Only the first text Field was compared | Possible duplicates compare the entire normalized composite identity and appear once after the Fields |
| Track expansion | Switching split/full-pane branches disposed page-local saveable state | The adaptive frame owns one `SaveableStateProvider` for primary content across every posture branch |
| Expanded navigation | Full-pane mode removed the rail and exposed no direct Home control | The top Home mark appears in compact and expanded-content modes; the five-domain bottom bar remains uncluttered |
| Search filters | Several selected types collapsed into one long FilterChip and could shrink to an unreadable sliver | Content Types wrap as minimum-size controls, Match All/Any is a two-state segmented choice, and collapsed active filters remain separate removable chips |
| Elapsed/sobriety tracking | No Goal represented continuously elapsed time; a normal value Goal would require manual logs | `Count Time Since` is a general Goal type with an exact start instant, Auto/Minutes/Hours/Days/Weeks/Years display, and Reset to Now or a chosen date/time |
| Automation endpoint validity | Exercise/raw-Metric follow-up triggers were accepted even though the trigger schema cannot identify a Metric and no editor exposes those paths | Unsupported follow-up sources are rejected before persistence; they remain available as measurable Goal progress sources |
| Automation target validity | Prompt targets could disappear or be invalid without validation | Every Task, Habit, Track, Subtask, outcome, target/action pairing, archive state, and cycle is validated before a rule is stored |

## Interaction hierarchy decisions

1. A tab changes peer content in place. Its full meaning is retained for accessibility even when a narrow label is displayed.
2. Insights always occupies the second position when present.
3. Expanded content must preserve selection, editor draft, filter, scroll, and destination state; layout posture is not navigation.
4. Home is globally reachable whenever the persistent navigation rail is absent.
5. Identity may be composite. A Track Entry headline joins its identity values with a stable separator; optional labelled values remain supporting metadata.
6. Inactive search/filter machinery stays compact, but every active constraint remains individually visible and removable.
7. Elapsed time is derived from one authoritative instant. It does not manufacture measurements, progress percentages, streaks, or automation contributions.

## QA cause/effect matrix

Automation coverage now includes:

- every exposed consequence: prompt Task, prompt Habit, automatic Habit Check Off, and prompt Track Entry;
- every Habit outcome: Recorded, Completed, Failed, and Skipped;
- completed/skipped Task, completed Subtask, completed Workout, and condition-matching Track Entry sources;
- Track capture mapping and fulfillment, pause/resume, archive suspension, delay/remind/dismiss, idempotent rebuild, edit/delete/restore reconciliation, contribution override/exclusion, and cycle rejection;
- every Track aggregation in deterministic domain coverage, plus repository-backed count/filter/numeric contribution paths; and
- rejection tests for impossible outcomes, missing endpoints, unsupported follow-up sources, incompatible units, stale Fields/options, invalid mappings, and elapsed-time Goal targets.

Every `AppSettings` field must have named persistence and behavioral evidence in `docs/quality/settings-cause-effect.tsv`. `SettingsCauseEffectContractTest` fails when a setting is added or removed without updating that cause/effect inventory. The existing feature matrix in `docs/testing.md` remains the first-class feature gate.

## Release gate

Before a release candidate is shipped:

1. `scripts/check` must pass JVM tests, Android-test compilation, lint, debug assembly, schema checks, icon checks, and manifest permission checks.
2. `scripts/check --emulator` must pass the complete instrumentation suite on a disposable API 34+ emulator.
3. `scripts/check --full` must additionally pass minified release and benchmark builds.
4. The signed release must be installed on the physical Fold and smoke-tested in compact, flat expanded, split, and expanded-content modes. The test must cover Home recovery, Track selection across expansion, composite Entry creation/edit, search filters, elapsed-goal create/reset, and one Automation cause/effect journey.

A release build is not accepted if any step is skipped or if the checked-in test-count baseline is stale.

## Verification completed

- `scripts/check --full` passed all 221 JVM tests, Android-test compilation, lint,
  debug assembly, minified release APK/AAB assembly, and the benchmark build.
- `scripts/check --emulator` passed all 243 Android instrumentation tests on a
  disposable API 34 emulator across six isolated process batches with no skips.
- The physical Fold passed 27 focused adaptive/control/settings UI tests and 44
  repository, migration, backup, settings, Goal, Track, and Automation tests.
- The final signed `commvne.com.whip.app` release (`versionCode 15`, `0.3.9`) was
  installed, hash-compared with the built APK, cold-launched, and verified as the
  resumed activity. The final Play bundle was separately signature-verified.
