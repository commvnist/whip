# Gym product audit and release plan — 2026-08-30

Status: implementation and emulator QA complete; approved for signed physical release
Scope: Gym user experience, visual design, accessibility, functional correctness,
ease and speed of use, strength-program authoring, QA, and physical-device release

## Method and evidence boundary

This pass combines:

- a source, schema, test, and existing-device-artifact review;
- a simulated persona review from the perspective of powerlifters, bodybuilders,
  and intermediate lifters;
- separate specialist audits for program correctness and UX/accessibility;
- a director synthesis that reconciled both reports into one release plan; and
- the repository's automated and physical-device release gates.

The persona pass is an expert simulation, not recruited-user research. It is useful
for finding workflow and domain-model failures, but it must not be represented as
observed human-subject evidence.

Baseline before implementation: `scripts/check` passed with 786 product tests
(363 JVM and 423 Android instrumentation tests), Android-test compilation, lint,
coverage floors, and debug assembly. Destructive UI execution is reserved for the
connected disposable API 34 emulator. The connected Samsung Fold is used only for
non-destructive release installation and smoke inspection.

## Baseline director verdict

At baseline, the focused active-workout composer, equipment snapshots, automatic next-set
focus, rest timer, scalable exercise picker, alternatives, and recoverable archive
are strong foundations. Routine programming and historical review are not yet at
the same standard.

| Lens | Audit score | Main reason |
| --- | ---: | --- |
| Workout logging | 7/10 | Fast focused composer, but first-set and previous-set paths still add friction |
| Routine authoring | 6/10 | Flexible static editor, but program cycles are not modeled safely |
| Accessibility | 6/10 | Good shared primitives, with unlabeled switch rows and dense advanced flows remaining |
| History as a training log | 4/10 | Sessions are visible, but performed and prescribed set details are incomplete |
| Bodybuilding support | 5/10 | RPE/RIR, groups, drops and alternatives exist; volume attribution and group-rest semantics are weak |
| Faithful 5/3/1 support | 2/10 | Percentage inputs exist, but training max and multi-day cycle state are not stable or correct |

## Persona findings

### Powerlifter

Strengths:

- percentage load resolution, equipment rounding, AMRAP classification, rest
  timing, and reusable alternatives already exist;
- machine/exercise configuration is snapshotted into the workout; and
- planned sets enter the same focused logging flow as ad-hoc sets.

Release blockers:

- routine progression uses the routine-wide number of finished sessions. In a
  four-lift week, successive lift days receive successive phases instead of the
  same program week;
- the current estimated 1RM is read again when a routine starts, so a PR can move
  later loads inside the same cycle;
- a routine cannot persist distinct 5s, 3s, 5/3/1, and deload prescriptions as one
  auditable repeating program; and
- opening and saving a routine omits training-max, progression, and alternative
  fields, silently resetting them.

### Bodybuilder

Strengths:

- rep ranges, RPE/RIR, tempo, rest, set classifications, supersets, circuits,
  alternatives, and high-volume prescriptions are representable;
- the exercise picker scales and filters large libraries; and
- BBB-style supplemental work fits the existing set model once program phases are
  modeled.

Priority issues:

- rest can start after each member instead of after the configured group boundary;
- routine groups are reconstructed as supersets even when a circuit is intended;
- previous-set reuse does not first match set position and classification; and
- `PrimaryOnly` hard-set allocation currently relies on category ordering rather
  than explicit primary-muscle semantics. This remains a P2 data-taxonomy item and
  must not be presented as precise muscle-volume science.

### Intermediate lifter

Strengths:

- reusable exercises, routines, presets, and inline validation teach a sound
  progression from simple logging to advanced programming;
- blank workouts and saved routines coexist; and
- editor state survives nested exercise/machine creation and retry.

Priority issues:

- there is no guided path from four known lifts and training maxes to a complete,
  previewable program;
- starting the next intended routine day/phase is not obvious;
- an empty workout can require separate Add Exercise and Add Set actions before
  the first entry; and
- advanced raw switch rows do not expose a single, clearly labeled accessibility
  target.

## Cross-cutting audit

### Functional correctness

P0:

1. Preserve every `RoutineExerciseDraft` field across open/edit/save.
2. Replace routine-global phase inference with persisted, auditable program state.
3. Persist an explicit per-lift training max; an e1RM may suggest an initial value
   once, but later PRs must not silently rewrite the current cycle.
4. Snapshot source routine day, phase, cycle, and training max into each workout.
5. Keep ordinary static routines backward-compatible.

P1:

- support phase-specific sets, stable discard/repeat behavior, cycle rollover,
  and independent upper/lower increments;
- preflight known setup blockers before Start; and
- preserve program semantics through duplicate, backup/restore, and migration.

### User experience and visual design

- Add a guided program authoring surface without turning the normal routine editor
  into a mandatory wizard.
- Show a complete preview before saving: lift, phase, percentage, resolved load,
  reps/AMRAP, supplemental work, and rounding.
- Put **Start next workout** and current cycle/phase on a program routine card,
  while retaining manual day selection.
- Keep one dominant next-set affordance during training and hide structural reorder
  chrome outside an explicit Arrange mode where practical.
- Expand History into an actual training log with performed versus prescribed
  values and equipment context.

### Accessibility

- Every switch row is one merged, labeled, stateful target of at least 48 dp.
- Percentage targets announce both source and resolved meaning, for example
  `85 percent of training max, 77.5 kilograms, at least 5 reps, AMRAP`.
- Program preview, routine start, set completion, and History remain usable at 200%
  and 320% text, with TalkBack, Switch Access, keyboard navigation, RTL, and high
  contrast.
- No status is conveyed only by color. Modal focus returns to the invoking action.

### Ease and speed of use

- Classic 5/3/1 setup should require mapping four lifts, confirming suggested or
  explicit training maxes, choosing a variant, reviewing loads, and saving.
- Repeating the intended next workout should take at most two taps.
- Adding an exercise to a blank workout should create and focus one blank set.
- Previous-set suggestions should prefer the same exercise, equipment, set
  position, and classification, and name their source.
- Large-library and long-history state construction must be bounded and measured;
  correctness work must not add main-thread full-history scans.

## Strength-program acceptance contract

The classic public 5/3/1 pattern is based on a fixed training max for the cycle:

| Phase | Set 1 | Set 2 | Set 3 |
| --- | --- | --- | --- |
| 5s | 65% × 5 | 75% × 5 | 85% × 5+ AMRAP |
| 3s | 70% × 3 | 80% × 3 | 90% × 3+ AMRAP |
| 5/3/1 | 75% × 5 | 85% × 3 | 95% × 1+ AMRAP |
| Deload | 40% × 5 | 50% × 5 | 60% × 5 |

The guided authoring release cut also covers:

- **5s PRO:** five prescribed reps at each main-work percentage without AMRAP;
- **Boring But Big:** configurable 5 × 10 supplemental work based on training
  max, using the same or an alternate lift; and
- **First Set Last:** configurable supplemental sets/reps using each phase's first
  work-set percentage.

Joker sets are explicitly outside this release cut unless added as an honest,
optional workout-time percentage-of-training-max set. They must not be simulated
by silently changing the fixed cycle prescription.

Program references used to verify the public patterns and terminology:

- Jim Wendler, [5/3/1 for a Beginner](https://www.jimwendler.com/blogs/jimwendler-com/101065094-5-3-1-for-a-beginner)
- Jim Wendler, [Boring But Big](https://www.jimwendler.com/blogs/jimwendler-com/101077382-boring-but-big)
- Jim Wendler, [Boring But Big 3 Month Challenge](https://www.jimwendler.com/blogs/jimwendler-com/boring-but-big-3-month-challenge)
- Jim Wendler, [5/3/1: How to Build Pure Strength](https://t-nation.com/t/5-3-1-how-to-build-pure-strength/281694)

## Implementation ownership

1. Program/data: schema, migration, explicit training max, per-day phase/cycle
   state, phase-specific sets, workout snapshots, advancement, backup, repository
   tests.
2. Guided authoring: classic/5s PRO/BBB/FSL generation and preview, routine-editor
   integration, accessibility semantics, UI/state tests.
3. General Gym UX: lossless routine edit reconstruction, History detail, routine
   readiness/next-phase presentation, focused accessibility/speed improvements.
4. Integration/release: reconcile contracts, run full local and emulator gates,
   inspect the release on the physical Fold, commit, push, sign, hash-verify, and
   install without clearing release data.

## Implemented release outcome

- Added persisted program kind, cycle, phase, next-day position, per-day
  progression, explicit per-lift training max and increment, phase-specific sets,
  and immutable workout program snapshots. Legacy static routines remain valid,
  and migration 31→32 preserves their existing wave position.
- Added a guided 5/3/1 builder for Classic 5/3/1, 5s PRO, Boring But Big 5×10,
  and First Set Last 5×5. It maps lifts, accepts explicit training maxes, previews
  all four phases in kg or lb with equipment rounding, and restores its variation
  when edited.
- Added Start Next, visible cycle/phase/next-day status, explicit reset/set program
  position controls, safe manual out-of-order starts, and correct cycle advancement.
  Training maxes remain stable during a cycle and advance only at rollover.
- Made routine editing lossless for progression and equipment fields, blocked
  mutation of a routine that owns an active workout, converted programmed loads
  safely when equipment units change, and expanded History with prescribed versus
  performed program context.
- Aligned the Gym empty-state title with shared page typography, retained
  title-case routine labels such as `Upper / Lower` and `Push Pull Legs`, improved
  large-text layout and switch-row semantics, and exposed percentage prescriptions
  with resolved load meaning.
- Full-suite QA also corrected a launch-delivery deadlock affecting direct-entry
  routes and a pound-equipment rounding fallback for percentage prescriptions.
  These cross-cutting defects were found by the release matrix rather than waived.

The P2 bodybuilding taxonomy items identified above—especially explicit
primary-muscle attribution, typed circuit reconstruction, and group-boundary rest
semantics—remain documented follow-up work. This release does not claim those
analytics are precise muscle-volume science.

## Completed QA evidence

- `scripts/check --emulator` passed as one uninterrupted release-candidate run.
- 369 JVM tests and all 428 Android instrumentation tests passed; instrumentation
  ran across eight isolated batches with zero failures and zero skips.
- Lint, Android-test compilation, debug assembly, Play Store asset verification,
  and the enforced deterministic coverage floors passed.
- Coverage results: domain lines 79.74% (3023/3791), domain branches 54.22%
  (1746/3220), and settings/policy lines 63.62% (397/624).
- Focused regressions cover golden 5/3/1 tables, four-lift phase/cycle advancement,
  kg/lb equipment rounding, stable training max behavior, migration, backup,
  authoring, History, launch routing, and the real Compose workflows that exposed
  integration failures during QA.

No release-blocking defect remains in the tested scope. Physical installation is
non-destructive and is performed only after the audited source is committed and
pushed.

## QA release gate

- Golden program tables pass in kg and lb with equipment rounding.
- A four-lift, four-phase repository journey keeps all lift days in the intended
  phase, holds TM stable after an AMRAP PR, rolls the cycle once, and applies
  configured increments only at rollover.
- Discard, repeat, skip/manual-day selection, recreation, duplicate, edit,
  backup/restore, and schema migration are deterministic.
- Static routines, quick set, rest timer, group behavior, machine snapshots,
  alternatives, archive, and delete regressions pass.
- Intermediate, powerlifter, and bodybuilder persona journeys run through the real
  UI on the disposable emulator.
- Compact, expanded, separating Fold, IME, large text, accessibility, light/dark,
  and RTL checks pass.
- `scripts/check --full` passes and emits the minified release APK/AAB and benchmark
  harness.
- The signed APK installed on the physical phone has the same SHA-256 as the local
  artifact, launches the release package, preserves data, and passes a
  non-destructive Gym smoke check.
