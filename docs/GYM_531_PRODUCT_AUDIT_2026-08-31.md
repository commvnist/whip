# Gym / 5/3/1 product audit and remediation — 2026-08-31

Status: implementation and verification complete

## Evidence boundary

This review combines source/schema/test inspection, a fresh-install emulator walkthrough,
and simulated persona reviews by separate methodology/domain and UX/accessibility
specialists. The persona work is an expert simulation, not recruited human-subject
research. Baseline screenshots are under `artifacts/gym-531-audit/` and
`artifacts/gym-audit/2026-08-31/baseline/`.

The methodology comparison uses public material from Jim Wendler's site, including:

- [The 5/3/1 Philosophy for Beginners](https://www.jimwendler.com/blogs/jimwendler-com/101065094-5-3-1-for-a-beginner), which distinguishes the Training Max, main work, 5x5 FSL, and push/pull/single-leg-or-core assistance;
- [Boring But Big](https://www.jimwendler.com/blogs/jimwendler-com/101077382-boring-but-big) and the [BBB three-month challenge](https://www.jimwendler.com/blogs/jimwendler-com/boring-but-big-3-month-challenge), which describe supplemental 5x10 work as a percentage of TM and show that main/supplemental/assistance are not synonyms; the former explicitly permits 5x10 or reduced 3x10 during its deload;
- the official high-rep squat variation [Boring But Big...And Really Sore](https://www.jimwendler.com/blogs/jimwendler-com/101082438-boring-but-big-and-really-sore), which instead removes its unusually high-rep squats during deload and therefore demonstrates that deload defaults are template-specific;
- the official [5/3/1 Forever contents and supplemental-lift excerpt](https://www.jimwendler.com/blogs/jimwendler-com/5-3-1-forever-table-of-contents-excerpt), which treats Leaders/Anchors, 7th Week Protocol, assistance, SSL, and BBS as distinct programming concepts; and
- the official [Beyond 5/3/1 description](https://www.jimwendler.com/products/beyond-5-3-1-hard-copy), which identifies FSL and Joker Sets as intentional program concepts rather than generic extra sets.

Whip does not reproduce proprietary book text. Where a public source establishes that a
concept exists but does not publish a complete prescription, the product must expose an
honest structured/custom path instead of inventing a supposedly canonical template.

## Director baseline verdict

5/3/1 was **not genuinely first-class at baseline**. The August 30 release added a sound,
narrow calculation/progression kernel: an explicit stable TM, phase-specific percentage
sets, classic and 5s PRO tables, BBB/FSL generation, cycle/day position, immutable TM/load
snapshots, equipment rounding, and tests. It did not add a whole-program authoring flow or
an expressive 5/3/1 product model.

The fresh-install path was:

`Gym → Library → Routines → Create Routine → Add Exercises → select lift → Add → open the
placement → Build a 5/3/1 Program`.

The builder then said that it built all four phases **for one exercise** and explicitly said
other exercises and days were unchanged. Configuring four lifts therefore repeated the same
deep flow four times. The prior audit's claim that the release mapped four lifts in a guided
flow was not supported by the shipped UI and is superseded by this report.

### Falsifiable calculation example

For an explicit 300 lb TM and 5 lb rounding, classic 5s week main work is:

| Set | Raw target | Expected rounded prescription | Baseline calculation |
| --- | ---: | ---: | ---: |
| 65% x 5 | 195 lb | 195 x 5 | 195 x 5 |
| 75% x 5 | 225 lb | 225 x 5 | 225 x 5 |
| 85% x 5+ | 255 lb | 255 x 5+ PR set | 255 x 5+ AMRAP |

The arithmetic was correct. The product semantics around it were incomplete: `Main Work`
and `Supplemental` existed only in generated note strings; PR-set intent was only a generic
AMRAP toggle; assistance and optional work were generic exercises/sets; and program identity
could not express `5s PRO + BBB` as two independent choices.

## Significant baseline findings

| Severity | Observed behavior | Expected behavior and why it matters | Affected users | Evidence / recommendation |
| --- | --- | --- | --- | --- |
| P0 | Finishing an empty, partial, or failed final program workout advanced the cycle and automatically increased every placement TM. | Schedule advancement and the decision to increase TM must not be the same event. An incomplete prescription must never silently raise the next cycle's loads. | All programmed lifters; highest risk for novices | `GymRepository.kt:364-442`; persist cycle TM-increase eligibility, hold TMs after incomplete required main work, and test empty/partial/under-target journeys. |
| P1 | `RoutineProgramKind` mixed a main scheme and a supplemental template in one mutually exclusive enum. Applying 5s PRO + BBB persisted only `BoringButBig`; changing another placement's supplement overwrote the routine label without rewriting previous sets. | Main scheme, supplemental scheme, optional work, and assistance role are compositional per-lift facts. Incorrect labels destroy trust and make future conditionals fragile. | Intermediate/experienced 5/3/1 users; engineers; History users | `GymModels.kt:151-170`, `FiveThreeOneProgramming.kt:168-175`, `RoutineBuilder.kt:191-213`; replace identity inference with typed per-placement fields while preserving legacy enum values. |
| P1 | The only 5/3/1 entry was inside one selected exercise placement. A four-lift program required roughly 45–60 taps/scrolls plus repeated typing. | A novice needs an unmistakable whole-program entry that maps the four lifts, confirms explicit TMs/increments once, and previews what will be saved. | Beginner, intermediate, one-handed mobile user | `RoutineBuilder.kt:1090-1111`; baseline builder screenshots; add a hybrid top-level program setup while retaining the generic routine path. |
| P1 | Main and supplemental work were encoded as set classification plus generated note text; assistance was an undifferentiated exercise and Joker work did not exist. | `Main → Supplemental → Assistance`, with Optional/Joker separate, must be structured and visible during authoring, execution, and History. | Every 5/3/1 persona; screen-reader users | `FiveThreeOneProgramming.kt:27-47`, `GymModels.kt:500-609`; add stable roles/types and immutable workout snapshots. |
| P1 | SSL, BBS, Joker Sets, Leaders/Anchors, and 7th Week uses did not exist. | The UI must neither call these supported nor force users to fake them with generic notes. Publicly specified schemes can be generated; book-specific structures need honest structured/custom representation. | Experienced 5/3/1 users | `FiveThreeOneProgramming.kt:21-25`; add supported schemes and publish an explicit support matrix. |
| P1 | Optional sets, if represented as ordinary planned sets, would become the next required set and be counted as incomplete at finish. | Joker candidates must be visibly optional, independently recorded, and excluded from required-work completion/error language. | Mobile lifter, experienced user | `GymScreens.kt:1137-1145,1224-1240`; make execution and finish logic work-type aware. |
| P2 | The active workout emphasized the next set and timer well, but did not show program section/role. | A glance should answer main/supplemental/assistance/optional and PR/Joker status without decoding classification or notes. | Distracted, fatigued, one-handed users | `GymScreens.kt:1453-1471,1769-1955`; add concise section labels and optional-state differentiation without extra cards. |
| P2 | Training Max was explicit and snapshotted, but the generic builder still exposed a legacy “TM % of estimated 1RM” path near the guided explicit field. | Actual/best lift, e1RM, TM, and working weight need unambiguous labels; e1RM may suggest an initial TM but must not mutate it later. | Novice and intermediate users | `RoutineBuilder.kt:1283-1284`, `RoutineRepository.kt:1031-1093`; keep legacy compatibility behind advanced disclosure and teach the explicit path. |
| P3 | Builder screens were dense and card-heavy, with important program entry below exercise selection. | Progressive disclosure should put program/template choice before low-level set configuration and preserve 48 dp targets/merged semantics. | Mobile, large-text, TalkBack, Switch Access users | `RoutineBuilder.kt`, `FiveThreeOneBuilder.kt`; retain existing accessible switch primitive and reduce repeated low-level entry. |

## Persona workflow assessment at baseline

Scores use 1 (poor/high risk) to 5 (excellent/low risk). Interaction cost is an
approximation from the fresh-install walkthrough.

| Persona / workflow | Discoverability | Cognitive load | Interaction cost | Outcome confidence | Configuration-error risk |
| --- | ---: | ---: | ---: | ---: | ---: |
| Beginner creates valid four-lift 5/3/1 | 1 | 5 | ~45–60 actions + typing | 2 | High |
| Intermediate configures four TMs and BBB/FSL | 2 | 4 | Four repeated per-lift flows | 3 | Medium-high |
| Experienced user configures SSL/BBS/Joker/Leader-Anchor | 1 | 5 | Dead end or manual approximation | 1 | Very high |
| General gym user creates a static routine | 4 | 3 | Reasonable generic flow | 4 | Low-medium |
| One-handed lifter performs prescribed sets | 4 | 2 | One dominant composer + timer | 4 | Low, except optional semantics |

## Variant support matrix at baseline

| Variation / concept | Baseline status | Evidence |
| --- | --- | --- |
| Classic 5/3/1 percentages | Genuinely supported, narrow | Central golden table, explicit TM, equipment rounding, phase/cycle tests. |
| PR sets | Partially supported | AMRAP classification and minimum target existed; intent was a generic toggle, not structured PR-set policy. |
| 5s PRO | Genuinely calculated; partially modeled | Correct five-rep main tables, but identity was lost when combined with BBB/FSL. |
| FSL 5x5 | Genuinely calculated; partially modeled | Correct first-set percentages, but only note/classification distinguished supplemental work. |
| BBB 5x10 | Genuinely calculated; partially modeled | Configurable TM percentage, but same compositional/section limits and no alternate-lift whole-program setup. |
| SSL | Unsupported | No enum, generator, UI, or test. |
| Boring But Strong | Unsupported | No enum, generator, UI, or test. |
| Joker Sets | Unsupported | No domain or optional execution behavior. |
| 5/3/1 for Beginners | Technically reproducible but awkward | User could manually create three days and repeated lifts, but there was no intentional template or assistance guidance. |
| Leader / Anchor | Unsupported | A linear phase cursor is not a multi-cycle block model and cannot safely express changing PR/supplemental policy. |
| 7th Week deload / TM test / PR test | Unsupported as a distinct protocol | One old-style `Deload` phase existed; TM/PR test intent and protocol role did not. |
| Custom per-lift schedules | Partially supported | Generic phase-specific sets were possible; program identity/advancement remained routine-wide and undiscoverable. |

## Resolved design dialectics

### Existing builder versus a dedicated programming environment

1. **Position A — refine the existing builder.** Lowest implementation cost and no second
   interaction model, but it keeps 5/3/1 hidden at placement depth and repeats four-lift data.
2. **Position B — replace it with a dedicated program builder.** Clear semantics, but makes
   ordinary routines pay a large complexity tax and risks an unjustified generic DSL.
3. **Evidence/constraints.** Static routine creation is already effective; the failure is
   program discovery/composition. Serious programming and today's workout have different
   interaction needs.
4. **Failure modes.** A remains generic-tracker-first. B overbuilds and can strand current
   routine users.
5. **Decision — hybrid.** Keep the normal builder; add a top-level whole-program template
   path that produces the same typed routine domain and remains editable in the advanced
   builder.
6. **Why superior here.** It makes 5/3/1 deliberate without making every Gym user learn it.

### Generic flags/notes versus a programming DSL versus typed composition

1. **Position A — continue classifications, notes, and `if program == ...`.** Cheap but
   semantically lossy and impossible to trust in History.
2. **Position B — build a fully generic strength-program DSL.** Expressive but premature,
   difficult to validate, and expensive to teach.
3. **Evidence/constraints.** The actual repeated concepts are few and stable: main scheme,
   supplement, assistance role, optional kind, phase, percentage, and TM.
4. **Failure modes.** A creates scattered conditionals; B creates an internal programming
   language before a second first-class program proves the need.
5. **Decision — small typed composition.** Persist those concepts and snapshot them into
   workouts; keep phase-specific sets as the executable prescription.
6. **Why superior here.** Correctness and UX improve immediately while the architecture
   remains understandable and migration-safe.

### Automatic versus manual Training Max increases

1. **Position A — always auto-increase at rollover.** Fast and predictable, but baseline did
   so after empty/failed work.
2. **Position B — require a manual TM decision every cycle.** Faithful to lifter judgment but
   adds repetitive friction and makes ordinary success unnecessarily slow.
3. **Evidence/constraints.** A TM is a deliberate program state, not a live e1RM; Wendler's
   public beginner guidance says the lifter must earn the increase with strong, technically
   sound work.
4. **Failure modes.** A compounds failed prescriptions. B is irritating and easy to forget.
5. **Decision — eligibility-gated automation.** Advance the schedule on an intentional
   finish, but hold the upcoming TM if required main work was incomplete/under target;
   preserve an explicit program-position/TM correction path.
6. **Why superior here.** Successful cycles stay fast while the app never converts failure
   into a silent load increase.

### Parallel phase metadata versus inference versus set-authoritative policy

1. **Position A — store phase policy in placement flags or parallel phase lists.** This is
   easy to display, but a copied or edited prescription can drift from the label and make an
   Anchor appear to have PR or Joker work that is not actually present.
2. **Position B — infer every policy from the set shape.** The executable work remains the
   source of truth, but three non-AMRAP sets of five are inherently ambiguous between 5s PRO
   and classic minimum-rep work.
3. **Evidence/constraints.** Leader/Anchor customization changes policy by phase; phases can
   be copied and reindexed; old routines have no typed policy; completed workouts must retain
   what was prescribed at session start.
4. **Failure modes.** A produces truthful-looking but stale metadata. B silently guesses at
   intentional methodology where two programs share the same set shape.
5. **Decision — optional typed policy tags on executable Main and Supplemental sets.** The
   repository validates that tags occur in the correct section and agree with the active set
   shape; legacy untagged sets use conservative inference. Joker state comes only from an
   actual Optional/Joker set.
6. **Why superior here.** One phase has one source of truth. Copying, removing, reindexing,
   starting a workout, and preserving History all carry the policy with the prescription,
   without a second list that can drift or a premature programming DSL.

### Universal deload supplemental rule versus template-specific defaults

1. **Position A — omit all Supplemental work from every generated Deload.** This is a
   conservative recovery default and cleanly separates ordinary training from a deload.
2. **Position B — carry the selected Supplemental template through every Deload.** This
   preserves the builder's original behavior and avoids surprising a user who selected a
   supplement for the cycle.
3. **Evidence/constraints.** Wendler's public BBB article explicitly permits its normal
   5×10 during deload or a reduced 3×10, while his high-rep squat BBB variation explicitly
   removes the high-rep squats. The public Beginners article specifies FSL on its three
   training days but does not publish a deload prescription. The available public sources
   likewise do not establish universal Deload FSL, SSL, or BBS rules. Advanced custom phases
   must remain able to represent the lifter's actual source program.
4. **Failure modes.** A contradicts documented BBB. B turns one BBB-specific instruction
   into an unsupported universal rule and silently adds substantial volume to the app-added
   Beginners deload.
5. **Decision — template-specific generated defaults.** Standard BBB retains 5×10 during
   generated Deload. FSL, SSL, BBS, and the Beginners wizard generate Main-only Deload work.
   Advanced Program Structure may explicitly add any supported supplement to Deload.
6. **Why superior here.** The app preserves the one directly documented default, refuses to
   fabricate the others, and remains expressive where published templates differ. Generator
   tests make the distinction falsifiable instead of burying it in UI conditionals.

## Prioritized remediation

### P0 — correctness / data integrity

- Decouple schedule advancement from TM increase eligibility.
- Snapshot every new structured program role into the workout so History never changes when
  a routine is edited.
- Add an explicit, backward-compatible migration and backup defaults; map uncertain legacy
  data to `Unspecified`, never to an invented meaning.

### P1 — fundamental domain and UX

- Persist per-lift main scheme, supplemental scheme, assistance role, Joker enablement, and
  per-set work/optional type.
- Add a visible whole-program four-lift 5/3/1 setup with explicit TMs/increments.
- Add SSL, BBS, structured PR-set policy, and optional Joker candidates.
- Make required versus optional work explicit in workout execution and finish review.

### P2 — major improvements

- Add intentional 5/3/1 for Beginners day/assistance template support.
- Add semantic phase roles for Leader/Anchor and 7th Week deload/TM-test/PR-test before
  claiming those programs are supported.
- Present main/supplemental/assistance sections in routine preview, active workout, and
  History.

### P3 — polish

- Reduce builder container density, improve preview scanning, and retain 48 dp/large-text,
  keyboard, screen-reader, focus-return, and non-color state cues.

## Key-question answers at baseline

1. **Is 5/3/1 first-class?** No; calculations were first-class, whole-program product
   semantics were not.
2. **Truly supported variants?** Narrow classic, 5s PRO, BBB 5x10, and FSL 5x5 calculations.
3. **Manual-only variants?** Beginner layout and some custom phase work; the result was
   awkward and semantically lossy.
4. **TM semantics correct?** Explicit TM/snapshot arithmetic was correct; automatic increase
   after incomplete work was not.
5. **Distinct work types?** No.
6. **Natural Joker use?** No.
7. **PR sets versus 5s PRO?** Calculable, but only partially and non-compositionally modeled.
8. **Leader/Anchor?** No.
9. **7th Week Protocol?** No distinct representation.
10. **Can a novice create a valid program?** Only with substantial external knowledge and
    repetition.
11. **Can an expert customize without fighting it?** Not beyond the narrow kernel.
12. **Is supplemental work obvious?** Only after discovering the deep per-lift builder.
13. **Is assistance obvious?** No 5/3/1-specific structure or guidance.
14. **Does customization preserve progression?** Phase sets did; semantic identity did not.
15. **Is active workout fast?** Strong for required generic sets; optional/program hierarchy
    was absent.
16. **Dedicated builder?** A hybrid template-first path is justified; replacement is not.
17. **Future-program domain?** The phase/TM kernel is useful, but roles/composition were too
    weak.
18. **Central or scattered rules?** Arithmetic was central; identity and presentation were
    coupled to UI enums/notes.
19. **Missing?** Whole-program setup, SSL, BBS, Joker, assistance roles, Leader/Anchor,
    distinct 7th Week uses, TM test, and explicit cycle eligibility.
20. **Present but undiscoverable?** The entire guided 5/3/1 flow, percentage previews,
    per-lift increments, and phase-specific prescriptions.

## Implemented product direction

The selected direction is a **hybrid strength-program experience**. Ordinary Gym users keep
the existing static routine composer. A visible `Start a strength program → Set Up 5/3/1`
entry now creates a coherent standard, Beginners, or user-chosen-lift program and feeds the
same editable routine domain.
The implementation deliberately adds a small typed programming vocabulary instead of a
generic programming DSL.

### Domain, persistence, and History

- A compositional `FiveThreeOne` program now separates Main scheme, Supplemental scheme,
  assistance role, work section, optional work kind, and phase role. Legacy program kinds
  remain readable.
- Routine and workout sets distinguish Main, Supplemental, Assistance, and Optional work.
  Main/Supplemental policy is tagged on the executable phase sets; Joker state is derived
  only from an actual Optional/Joker set.
- Workout placements snapshot TM, unit, cycle increment, Main/Supplemental policy,
  assistance role, and Joker availability. Sessions snapshot phase label/role. A later
  routine edit therefore does not rewrite what a completed workout prescribed.
- Room schemas 33–34 and portable-backup format 12 add explicit defaults. The v32→33 migration
  backfills only high-confidence generated semantics and maps uncertainty to `Unspecified`.
  Legacy non-static programs receive their previous final-phase TM boundary; new programs
  persist boundaries explicitly. The v33→34 migration adds a session-level
  `requiredMainWorkInvalidated` safety snapshot so removing/substituting a Main placement
  cannot erase required-set evidence through a foreign-key cascade.
- Repeated 5/3/1 main-lift placements are synchronized in the builder and rejected at the
  repository boundary if TM/progression/prescriptions drift.

The primary implementation is in `domain/GymModels.kt`, `data/RoutineEntities.kt`,
`data/RoutineRepository.kt`, `data/GymEntities.kt`, `data/GymRepository.kt`,
`data/WhipDatabase.kt`, and `data/BackupRepository.kt`.

### Programming and authoring

- The whole-program setup can map Squat, Bench Press, Deadlift, and Overhead Press once, or
  build from any ordered set of distinct Weight + Reps exercises. Custom lifts use accessible
  add/remove/reorder controls and each become their own training day. With an empty library,
  the standard presets create only the missing standard lifts and reconcile asynchronous IDs
  without overwriting deliberate manual selections.
- The four-day layout creates Squat / Bench / Deadlift / Press days. The Beginners layout
  creates Monday Squat+Bench, Wednesday Deadlift+Press, and Friday Bench+Squat, forces FSL
  5×5, and teaches Push/Pull/Single-leg-or-Core assistance targets without inventing the
  user's exercise selection.
- Explicit per-lift TM and cycle increment are separate from equipment load increment. A
  recent max/e1RM can produce an editable 80–90% one-time suggestion (85% default); applying
  it copies a rounded explicit TM that no longer changes with the source value.
- Main choices are Classic with PR set, Classic minimum reps, and 5s PRO. Supplemental
  choices are None, FSL, SSL, BBB, and BBS. One optional starting Joker candidate can be
  offered where readiness and bar speed justify it.
- Generated Deload policy is template-specific: standard BBB retains 5×10; FSL, SSL, BBS,
  and Beginners omit Supplemental work by default. Advanced phase editing can deliberately
  opt any supported supplement back into Deload.
- Advanced Program Structure supports up to 52 named phases with Standard, Leader, Anchor,
  Deload, TM Test, and PR Test roles, explicit TM boundaries, and safe copy/remove/reindex.
  The selected phase can independently regenerate Main/Supplemental/Joker policy across all
  mapped lifts. A global BBB prescription is materialized for every current phase before one
  phase diverges, preventing double work or cross-phase mutation.
- The UI explicitly says copied/custom phases are not a canonical proprietary Leader,
  Anchor, or 7th Week prescription; the user must review the executable sets.
- Routine cards summarize active phase work rather than adding every stored phase together.
  For example, Beginners reads `4 phases · 3–8 active sets/phase · Main + FSL`, not the
  misleading `32 sets × 5 reps` found during the independent post-change walkthrough.
- The legacy per-placement generator remains useful for a one-day draft. A multi-day static
  draft cannot be partially converted: it is routed to whole-program standard/custom setup,
  and the replacement review explicitly states that current draft days will be replaced.
- Saving after adding an exercise to an existing routine persists in place, refreshes the
  edit baseline, and keeps the lifter on the same routine/day. `Close` is now the separate
  navigation action; creating a brand-new routine still exits after its first save.

The central calculation/authoring code is in `ui/FiveThreeOneProgramming.kt`,
`ui/FiveThreeOneBuilder.kt`, and `ui/RoutineBuilder.kt`.

### Post-implementation walkthrough findings

| Severity | Observed behavior | Expected behavior and why it matters | Affected users | Evidence / resolution |
| --- | --- | --- | --- | --- |
| P1 | A freshly built Beginners card displayed `32 sets × 5 reps`, aggregating all four stored phases as though they were one workout. | Authoring summaries must describe active work in a phase; otherwise a novice reasonably believes the template prescribes 32 sets today. | Beginner, general gym user, one-handed user scanning the routine | `artifacts/gym-531-audit/final/beginners-built-top.png`, `RoutineBuilder.kt`; replaced with phase-aware counts/ranges and work structure, with pure and UI regression assertions. |
| P2 | Changing a phase role to Deload/TM Test/PR Test could briefly leave its now-disabled local Joker switch visually On until Apply. | Unavailable optional work must immediately show Off and Disabled so state is never conveyed ambiguously. | Experienced user, screen-reader and switch users | `artifacts/gym-531-audit/final/deload-joker-disabled.png`, `RoutineBuilder.kt`, `RoutineBuilderUiTest.kt`; role selection now clears the local switch immediately. |
| P1 | Every selected Supplemental template originally carried into generated Deload, including five FSL/SSL sets and ten BBS sets; Beginners therefore showed eight active sets in its app-added Deload. | Deload work must be template-specific. Public evidence supports standard BBB 5×10 (or 3×10), but does not support treating FSL/SSL/BBS or Beginners the same way. | Beginner and experienced 5/3/1 users; anyone trusting the wizard's default volume | `FiveThreeOneProgramming.kt`, `RoutineBuilderStateTest.kt`, `RoutineBuilderUiTest.kt`; generated FSL/SSL/BBS and Beginners Deloads are Main-only, BBB remains 5×10, and Advanced stays explicit opt-in. |
| P1 | Renaming/reordering/removing phases or days could map the persisted current cursor by a stale numeric position; one removal transform was initially attached to metadata editing. | Editing labels or structure must preserve the same logical current phase/day whenever it still exists, and map deterministically when it is removed. | Experienced programmers editing a live block | `RoutineBuilder.kt`, `RoutineRepository.kt`, `RoutineBuilderStateTest.kt`, `RoutineRepositoryTest.kt`; stable edit-only identity hints now flow to the repository, with rename/reorder/remove regressions. |
| P1 | Removing or substituting one Main placement from a multi-main active workout cascade-deleted its planned sets. Finish then saw only the remaining completed Main work and could permit every lift's boundary TM increase. | Destructive live-workout edits must never turn missing prescribed work into success, while discarding the session must not mutate routine eligibility. | Beginners Friday Squat+Bench, custom multi-lift days, all TM-gated programs | `GymEntities.kt`, `GymRepository.kt`, schema 34, `RoutineRepositoryTest.kt`; the session is permanently marked invalid for TM advancement before cascade deletion, and finish explains the hold. |
| P1 | A former Main lift reclassified as assistance retained hidden TM/increment fields, and the rollover loop incremented every routine exercise. | Only placements with executable Main prescriptions are progression targets; stale historical configuration must not silently change. | Experienced editors and migrated routines | `GymRepository.kt`, `RoutineBuilder.kt`, `RoutineRepositoryTest.kt`; boundary updates now require an actual Main prescription while preserving migrated typed Main work. |
| P1 | The old per-lift converter could turn a multi-day static routine into a programmed routine while rewriting only one day; Main-less days then permanently poisoned cycle eligibility. | Whole-program conversion must produce a valid Main prescription for every programmed day or refuse the partial conversion. | Intermediate users converting existing splits | `RoutineBuilder.kt`, `RoutineBuilderUiTest.kt`; multi-day drafts now use the whole-program review/replacement route and the state transform has a defensive one-day invariant. |
| P1 | `Add Exercise → select → Save` while editing an existing routine returned to the routine library instead of the edited day. | Save should persist without discarding authoring context; leaving is a separate intentional action. | All routine authors, especially mobile users adding several assistance movements | `RoutineBuilder.kt`, `RoutineBuilderUiTest.kt`; editing Save remains in place and the regression verifies the selected routine/day stays open. |

### Workout execution and TM progression

- Active work orders Main before Supplemental/Assistance and labels each section. A PR set is
  distinguishable from prescribed minimum reps.
- An Optional/Joker row is never the required next set and is excluded from incomplete-work
  finish counts. The lifter explicitly chooses `Perform Joker` or `Skip`.
- Schedule advancement is separate from TM eligibility. Every required Main set at an
  explicit boundary must exist, be complete, avoid Failure, and meet both prescribed reps
  and load. Empty, partial, failed, under-rep, or under-load work advances the schedule only;
  it holds the TM. Successful work increments once and finish is idempotent.
- Removing or substituting any placement that contained prescribed Main work marks only that
  session ineligible for a TM increase before its sets are cascade-deleted. Finishing still
  advances the schedule; discarding leaves the routine untouched. TM rollover updates only
  routine placements that still contain executable Main prescriptions.

### Post-remediation persona assessment

Scores use 1 (poor/high risk) to 5 (excellent/low risk). These remain specialist persona
simulations, not recruited human usability research.

| Persona / workflow | Discoverability | Cognitive load | Interaction cost | Outcome confidence | Configuration-error risk |
| --- | ---: | ---: | ---: | ---: | ---: |
| Beginner creates valid four-lift 5/3/1 | 5 | 2 | ~15–25 actions + four TM entries | 4 | Low-medium |
| Intermediate configures four TMs and BBB/FSL | 5 | 2 | One guided flow, ~12–20 actions + values | 5 | Low |
| Experienced user creates Leader/Anchor phase differences | 4 | 3 | ~8–12 actions per changed phase | 4 | Low-medium; destructive scope is stated |
| General gym user creates a static routine | 4 | 3 | Existing flow unchanged | 4 | Low-medium |
| One-handed lifter performs prescribed/optional work | 5 | 1–2 | One dominant set action; one Joker decision | 5 | Low |

### Support matrix after remediation

| Variation / concept | Final status | Product boundary |
| --- | --- | --- |
| Classic 5/3/1 | First-class | Explicit TM, golden percentage tables, PR/minimum-rep choice, rounding, cycle state, snapshots, and rollover tests. |
| PR sets | First-class | Typed Main policy plus an active Main AMRAP; validation rejects a PR policy without the set. |
| 5s PRO | First-class | Independent Main policy, composable with every supported supplement; every active Main set must be five reps without AMRAP. |
| FSL 5×5 | First-class | Typed phase-aware Supplemental sets validated against the active first Main percentage; generated Deload is Main-only unless explicitly customized. |
| SSL 5×5 | First-class | Typed phase-aware Supplemental sets validated against the active second Main percentage; generated Deload is Main-only unless explicitly customized. |
| BBB 5×10 | First-class, same-lift wizard path | Configurable percentage, exactly 5×10, including the documented standard-BBB Deload default and phase materialization on divergence. Alternate-lift BBB remains an advanced/custom routine edit. |
| Boring But Strong 10×5 | First-class | Typed generator and exact set-shape validation; generated Deload is Main-only unless explicitly customized. |
| Joker Sets | Intentional optional support | One starting candidate per enabled phase, independent recording, explicit perform/skip, never required. No automated multi-Joker ladder or readiness decision. |
| 5/3/1 for Beginners | First-class layout | Exact public three-day main-lift/FSL structure and assistance category guidance; the app-added Deload is conservatively Main-only because the public article does not define its Deload FSL. Exercise choice remains user-owned. |
| Leader / Anchor | Clean structured representation, not a canonical preset | Named/typed phases can independently change PR/5s PRO, supplement, and Joker policy across multiple cycles. The app does not invent book-specific prescriptions. |
| 7th Week deload / TM test / PR test | Clean structured representation, not a canonical preset | Distinct phase roles and explicit TM boundary; lifter reviews/customizes sets for the intended protocol. |
| Custom chosen-lift schedules | First-class guided support | Any distinct Weight + Reps exercises can be added, removed, reordered, and assigned independent TMs/increments; each becomes one training day. More complex mixed-lift schedules remain editable in Advanced. |

### Final answers to the twenty key questions

1. **Is 5/3/1 genuinely first-class?** Yes for the supported templates and execution model;
   it now has a visible whole-program path, typed semantics, and central validation.
2. **Which major variants are truly supported?** Classic, PR/minimum reps, 5s PRO, FSL,
   SSL, BBB, BBS, Beginners, and one-candidate optional Jokers.
3. **Which remain manual/custom?** Exact proprietary Leader/Anchor and 7th Week
   prescriptions, alternate-lift BBB, multi-Joker ladders, and complex mixed-lift schedules.
4. **Are TM semantics correct?** Yes for explicit TM versus working load, rounding,
   snapshots, gated increases, unit conversion, and stable suggestions. There is not yet a
   separate queryable TM-change ledger.
5. **Are work types distinct?** Yes: Main, Supplemental, Assistance, and Optional are typed
   and snapshotted.
6. **Can Jokers be naturally enabled and used?** Yes, per phase, as visibly optional work
   that never blocks completion.
7. **Can users configure PR sets versus 5s PRO?** Yes, independently of supplemental work,
   including per-phase Leader/Anchor differences.
8. **Can Leader/Anchor be represented cleanly?** Yes as typed, named multi-phase blocks with
   phase-authoritative prescriptions; not as a misleading one-click canonical preset.
9. **Is 7th Week representable?** Yes with distinct Deload/TM Test/PR Test roles, explicit
   sets, and TM boundary; exact protocol selection remains deliberate/custom.
10. **Can a novice create a valid program?** Yes through the top-level standard, custom-lift,
    or Beginners setup, stable TM help, clear defaults, and preview.
11. **Can an expert customize without fighting it?** Substantially yes through selected-phase
    policies, copy/remove/reindex, explicit boundaries, and low-level set editing.
12. **Is supplemental work obvious?** Yes in setup, phase policy, routine rows, and workout
    labels.
13. **Is assistance work obvious?** Yes as Push/Pull/Single-leg-or-Core roles and guidance,
    though exercise selection intentionally remains manual.
14. **Does customization preserve progression?** Yes; prescriptions carry phase indices and
    policy tags, repeated-lift drift is blocked, and boundaries are explicit.
15. **Is active workout fast enough?** Yes: required next action remains dominant; section
    labels and a single optional Joker decision add no required navigation.
16. **Does Gym need a dedicated builder?** It needs—and now has—a hybrid template-first path,
    not a replacement generic programming environment.
17. **Is the model strong enough for future structured programs?** It now has reusable typed
    phase/prescription/TM/work-role primitives. A generic DSL should wait for another real
    program to prove additional abstractions.
18. **Are semantics central or scattered?** Calculations, active-policy resolution,
    validation, and progression are central. Compose edits typed drafts rather than owning
    the business rules.
19. **What remains missing?** A TM-change ledger, canonical licensed/book-specific presets,
    automatic assistance exercise programming, alternate-lift BBB setup, and multi-Joker
    autoregulation.
20. **What was present but undiscoverable?** The former deep per-placement 5/3/1 generator is
    now superseded by a top-level whole-program entry; low-level editing remains available
    under advanced disclosure.

### Automated verification

- `scripts/check`: **passed** — 382 JVM tests, lint, Android-test compilation, debug build,
  and coverage floors. Deterministic domain lines 79.66%, branches 54.22%, and core
  settings/policy lines 63.62%.
- Comprehensive emulator regression: `ANDROID_SERIAL=emulator-5554 scripts/check
  --emulator` **passed the exact final tree's 447 Android instrumentation tests**, with zero
  failures and zero skips in eight isolated batches. Aggregate:
  `build/instrumentation-results-joTLmS/aggregate.tsv`.
- The final device matrix includes `RoutineRepositoryTest` 20/20,
  `WhipDatabaseMigrationTest` 10/10, `BackupRepositoryTest` 16/16, and
  `RoutineBuilderUiTest` 15/15. The three release-blocker regressions were also run alone
  after implementation and passed 3/3 before the broad campaign.
- Focused JVM suites: `RoutineBuilderStateTest` 25/25 and `GymUxRulesTest` 9/9.
- `git diff --check`: passed.

This campaign was restarted after the final schema-34 progression fixes and documentation
reconciliation. It therefore covers the dedicated Program Structure page, save-in-place
routine editing, custom Bench/Deadlift/Zercher setup, multi-day conversion guard, phase/day
identity preservation, and required-Main invalidation rather than relying on an earlier
pre-remediation run.

Coverage includes the 300 lb golden table, kg/lb/equipment rounding, empty/partial/failed and
successful TM rollover, no-boundary TM hold, repeated Beginners lifts, phase-authoritative
Leader/Anchor/Deload snapshots, invalid scheme/tag rejection, v32→33 migration, v5–12 backup
compatibility, v33→34 session-safety migration, multi-main remove/substitute holds, static
routine regression, empty-library four-lift creation, a custom
Bench/Deadlift/Zercher program, stable one-time TM suggestion, advanced program structure,
template-specific Deload supplemental defaults, and the corrected phase-aware summary.

### Signed release and physical-phone verification

- `scripts/check --full` passed the release, bundle, optimized benchmark, lint-vital, JVM,
  coverage, and artifact-metadata gates.
- A fresh signed `0.3.27` / version-code 33 APK and Play bundle were produced. The APK is
  3,679,124 bytes with SHA-256
  `26e4bf114fd5eb5ee472bb220d4403b5533b7f3b23496ef9bfe76387362a29cc`;
  `apksigner` verified the Whip release certificate SHA-256
  `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`.
- `adb install -r` upgraded the connected Samsung SM-F976W without clearing application
  data. Package evidence retained the original `firstInstallTime=2026-08-26 17:59:24` and
  recorded `lastUpdateTime=2026-08-31 12:36:26`.
- Reading the installed base APK produced the same SHA-256 as the built artifact. A cold
  launch returned `Status: ok`, and `MainActivity` was the resumed foreground activity.
  The release process remained alive and its error log contained no application, Room, or
  migration exception.

### Definition-of-done closure

| Required outcome | Authoritative final evidence | Status |
| --- | --- | --- |
| Accurate 5/3/1 concepts and supported variants | Central percentage/rep/supplement generator; 300 lb golden example; template-specific Deload dialectic; `RoutineBuilderStateTest` | Proven for the published support matrix |
| Distinct Main, Supplemental, Assistance, and Optional work | Typed routine/workout fields, set-authoritative policy validation, immutable snapshots, section-labelled active workout | Proven |
| Novice and experienced authoring | Top-level standard, user-chosen-lift, and Beginners setup, explicit TM education/suggestions, advanced phase policy/copy/remove controls, fresh-install screenshots, `RoutineBuilderUiTest` | Proven within the declared template/custom boundaries |
| Joker, PR-set, 5s PRO, FSL/SSL/BBB/BBS behavior | Typed policies and deterministic validation; optional perform/skip execution; generator/repository/UI regressions | Proven |
| Leader/Anchor and 7th Week representation | Named phase roles, per-phase executable policies, explicit TM boundaries, no misleading canonical preset claim | Proven as structured/custom representation |
| Fast, mobile in-gym execution | Main-before-Supplemental ordering, dominant complete action, optional work never blocking finish, timer fallback, force-stop resume, one-handed/1.5× walkthrough | Proven |
| General Gym remains usable | Existing static routine composer retained; static-progression and broad Gym/routine suites remain green | Proven |
| Maintainable central business rules | `FiveThreeOneProgramming.kt` owns generation/rounding; repositories own validation, snapshotting, and progression; no generic DSL introduced | Proven |
| Existing data and History remain valid | Explicit schemas 33–34 and backup 12 compatibility, conservative legacy inference, v32→33, v33→34, and v5–12 tests, workout snapshots | Proven |
| Production regression coverage | 382/382 final-state JVM tests, lint/build/Android-test compilation, and 447/447 Android instrumentation tests in eight isolated batches | Proven |
| Physical release | Signed 0.3.27/code-33 APK hash-matched after `install -r`; original install timestamp retained; cold launch foreground verified | Proven |

### Independent final emulator walkthrough

The Director did not accept the automated suites as the final UX verdict. A separate agent
installed the refreshed APK on the disposable API 34 emulator and repeated the flow from an
empty Gym state. Final evidence is under `artifacts/gym-531-audit/final/`.

- The top-level program entry, empty-library standard-lift creation, explicit TM fields,
  layout/main/supplemental choices, and optional-Joker explanation were visible and reachable
  (`builder-entry.png`, `531-setup-top.png`, `531-setup-lifts.png`,
  `531-setup-programming.png`, `531-setup-joker.png`).
- The corrected routine summary shows `4 phases · 3–8 active sets/phase · Main + FSL`; the
  routine saved and its cycle/week/next-lift card was correct (`summary-fixed-deload.png`,
  `routine-saved.png`).
- The active workout made prescribed percentage, equipment-rounded load, reps, current set,
  Main versus Supplemental work, and the dominant completion action scannable
  (`active-workout.png`, `active-workout-sections.png`).
- Notification denial did not break the rest timer: the explicit in-app fallback ran, one-tap
  completion advanced to Set 2 and reset the timer, and force-stop/resume preserved the
  active workout (`timer-running.png`, `after-complete-one.png`).
- At 1.5× system font, the screen remained scrollable and the target/current action remained
  legible (`active-workout-large-text-late.png`).
- Changing a phase to Deload produced Joker `checked=false`, `enabled=false`, with a textual
  explanation (`deload-joker-disabled.png`).

The final `RoutineBuilderUiTest` run passed 15/15 in the comprehensive emulator campaign,
including phase-aware summary, Joker role change, custom chosen lifts, multi-day conversion
routing, and save-in-place assertions. No additional source defect was found in the final
cross-agent review.

### Chosen-lift extension

The guided builder now also exposes `Choose Your Lifts`. Any non-empty ordered set of
distinct Weight + Reps exercises can receive independent TMs and cycle increments; each
becomes one program day. Full-size add/remove controls, exercise-specific Move
Earlier/Later semantics, duplicate exclusion, and exact generated-day persistence make the
path usable without assigning a custom movement to an inaccurate Squat/Bench/Deadlift/Press
role. The regression example builds Bench Press, Deadlift, and Zercher Squat as a three-day
program. The final 382-JVM/447-Android campaign includes this extension and every subsequent
Program Structure, routine-navigation, schema-34, and progression-integrity correction.

## Re-opened control-integrity audit: ordinary routines

The audit was reopened after a user correctly identified that `% of Training Max` could be
selected in an ordinary routine even though that routine offered no usable way to set the
Training Max. The defect was treated as a product-wide control-integrity failure, not an
isolated missing text field. Every routine input that affects a future prescription was
traced through visibility, validation, persistence, reconstruction, workout generation,
progression, history, and repair after referenced data changes.

| Severity | Observed behavior | Expected behavior and affected users | Resolution and evidence |
| --- | --- | --- | --- |
| P0 | An ordinary routine could select `% of Training Max`, but could neither enter a TM nor reliably clear an older stored TM. | Any mass-based non-5/3/1 routine must be able to enter an explicit TM or deliberately derive one from an exercise/equipment-scoped e1RM. Saving a changed source must not preserve hidden stale data. | The set editor now reveals an ordinary-routine Training Max section whenever `%TM` is selected, with explicit/derived sources, a resolved preview, and missing-estimate validation. Routine updates now replace rather than merge the TM child record. Covered by `ordinaryRoutineCanEnterAnExplicitTrainingMaxForPercentageSets` and `changingTrainingMaxSourceToDerivedClearsTheStoredExplicitValue`. |
| P0 | Routine edits reconstructed absolute weights in the current app unit and lost per-set weight unit, distance unit, bodyweight snapshot, and day progression cursor. | Editing without touching a set must be lossless for metric/imperial users, bodyweight movements, distance work, and routines using load waves. | Builder state and draft mapping now round-trip all four values; mass-unit changes explicitly convert absolute values. Covered by `routineEditRoundTripsSetUnitsBodyweightAndDayProgression`. |
| P0 | Percentage prescriptions reused an exercise-level load multiplier and could silently halve or otherwise distort unilateral/per-side machine work. Dynamic bodyweight and assisted load models could also enter percentage modes with no stable meaning. | Prescribed load must use the selected set's exact equipment semantics; unsupported percentage combinations must be impossible in both UI and repository boundaries. | Workout resolution now calculates the multiplier per set. UI and repository validation reject bodyweight-dependent, assisted, and ordinal percentage modes. Covered by `unilateralPercentagePrescriptionUsesTheSetSpecificLoadMultiplier` and `bodyweightDependentAndAssistedPercentagePrescriptionsAreRejected`. |
| P0 | A machine load labelled `Exact` could still be snapped to a configured increment or stack choice. | Exact means the stored requested load; snapping is appropriate only for percentage-derived or explicitly progressed prescriptions. | Resolution now preserves literal exact loads while retaining snapping for calculated prescriptions. Covered by `exactMachineLoadRemainsExactInsteadOfSilentlySnapping`. |
| P1 | Removing an exercise from a machine could leave routines apparently valid even though their equipment binding was no longer selectable. Archived exercise references were also difficult to repair. | Existing routines must retain enough identity to be repaired, must visibly require equipment, and must not silently become free-weight work. Historical workouts must remain unchanged. | Machine unlink now transactionally marks affected placements `NeedsEquipment`, retains snapshots for repair, and leaves workout history untouched. Existing archived exercises remain reconstructable while new selection stays active-only. Covered by `unlinkingMachineExerciseMarksAffectedRoutineBindingForRepair`. |
| P1 | Static load-wave routines advanced after a session merely started, and their current step was effectively hidden. | A wave advances only after all prescribed work is successfully completed; failed, partial, and empty sessions hold. Users must see and be able to reset the next step. | Advancement now requires completed non-failure work. Routine status exposes the next load-cycle step and reset action, and the snapshot is recorded only when the routine actually uses a wave. Covered by `staticLoadCycleAdvancesOnlyAfterCompletedNonFailureWork`. |
| P1 | A failed required 5/3/1 Main set could correctly hold TM progression, but there was no deliberate recovery control if the lifter later chose to resume progression. | The hold must be visible and recoverable without rewriting workout history, resetting program position, or silently increasing the current TM. | Program status now explains the held state and offers a confirmed `Restore Training Max Eligibility` action affecting future progression only. Covered by `trainingMaxEligibilityCanBeRestoredWithoutResettingProgramPosition`. |
| P1 | Deleted-machine and historical-workout imports could reinterpret missing equipment as free weight, lose Level targets, or create mixed performed/prescribed rep ranges. | Imports must preserve what was actually performed and surface missing configuration rather than inventing new semantics. | Snapshot-aware import now preserves units, bodyweight and Level targets, uses performed reps consistently, and emits `NeedsEquipment` for missing machines. |
| P1 | Starting a routine created planned sets, and the presence of those rows locked the in-workout machine selector before the lifter performed anything. | A mobile lifter must be able to move to an available compatible machine until the first set is completed, without deleting the prescription or rewriting completed history. | Equipment now locks on completion, not set existence. Before completion, actual and prescribed canonical targets are translated through the replacement machine's unit, base/add-on load, pulley ratio, stack/per-side interpretation, and available settings. Untranslatable ordinal/mass changes fail explicitly. Covered by `plannedRoutineMachineCanChangeBeforeCompletionAndRetargetsThePrescription`. |
| P1 | The exercise-details dialog launched notes and machine saves as independent coroutines; both were full-row read/modify/write operations and could restore the other's stale value. | One Save action must be atomic: either notes and equipment both persist, or neither does. | The UI now issues one operation backed by `updateWorkoutExerciseDetails`, which sequences notes and machine retargeting inside one Room transaction. The machine-retarget regression asserts that both the replacement machine and changed notes survive. |

The architectural decision is a bounded hybrid: 5/3/1 keeps its guided program builder and
typed program semantics, while ordinary routines receive the same reliable TM prescription
primitive at the placement level. This avoids forcing general Gym users into 5/3/1 concepts
and avoids duplicating percentage calculation rules in Compose. Prescription capability is
centralized by tracking/load interpretation; repositories remain the final validation and
snapshot boundary.

### Final re-opened-audit verification and phone release

- `ANDROID_SERIAL=emulator-5554 scripts/check --emulator --full` passed the final source
  tree: **382 JVM tests and 458 Android instrumentation tests**, with zero Android failures
  and zero skips across eight isolated batches. Aggregate evidence:
  `build/instrumentation-results-wILYZL/aggregate.tsv`.
- Lint, debug/release APKs, signed Play bundle, optimized benchmark build, Android-test
  compilation, and release metadata all passed. Coverage remained above every enforced
  floor: deterministic domain lines 79.53%, branches 54.19%, and core settings/policy lines
  63.62%.
- The methodology/domain reviewer, Gym Product Director, and mobile lifter reviewer each
  returned GO after the final equipment-retarget and atomic-save corrections.
- Signed release `0.3.30` / code 36 was installed with `adb install -r` on the connected
  Samsung SM-F976W. The existing `firstInstallTime=2026-08-26 17:59:24` was preserved and
  `lastUpdateTime=2026-08-31 14:47:16` recorded the upgrade.
- The 3,695,508-byte installed APK hash matches the local artifact:
  `dc3f7da543a2f798839c8891b13a53dca2f569cc802e3124bf86de569bb5a014`.
  The release certificate SHA-256 remains
  `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`.
  A cold launch returned `Status: ok`; `MainActivity` was the resumed foreground activity,
  and the launched process emitted no fatal, Room, or migration error.

## Re-opened assistance-provenance and hidden-control audit

The audit was reopened after the former `Quick assistance` controls failed the most
important user question: where do Push, Pull, and Single-leg/Core come from? The compact
chips looked like filters or generated recommendations, the relationship to Exercise
Library categories was unstated, and the routine model used one legacy role field for both
the structural main-lift role and assistance classification. The remediation therefore
covered UI explanation, interaction intent, persistence, backup/migration, immutable workout
history, analytics, active-workout authoring, and related hidden-default rules.

The definitive product rule is now:

> The lifter assigns an assistance category to an exercise placement in a specific routine
> day. Whip never infers that assignment from an exercise name, muscle field, equipment, or
> Exercise Library category. The same reusable exercise can be Pull in one day and General
> or another role elsewhere without changing its library record.

Concrete example: selecting Dumbbell Row from `Add Pull assistance` on Day A persists
`placementKind=Assistance` and `assistanceCategory=Pull` for that placement. It does not
modify Dumbbell Row's reusable exercise or library-category links. A generated 5/3/1 main
lift persists `placementKind=MainLift` independently, so it cannot be casually relabelled as
assistance. Starting a workout snapshots both values; later routine edits do not rewrite
what historical sets meant.

| Severity | Observed behavior | Expected behavior, affected users, and why it matters | Resolution and source evidence |
| --- | --- | --- | --- |
| P1 | `Quick assistance` was a cramped row of unexplained category chips directly against nearby actions and copy. Nothing answered whether roles came from the template, exercise metadata, or the user. | Novices need source and consequence before acting; experienced lifters need fast explicit control; mobile lifters need full-size one-handed targets. Ambiguous provenance creates confidently misconfigured programs. | `RoutineBuilder.kt` now renders a full-width `Assistance for [day]` plan card. It states that assignment is routine-local and user-owned, explains all three roles with examples, separates unclassified work, and explains Previous Workout copy semantics. |
| P1 | The shared exercise picker always looked generic, even when opened from an assistance action. | The picker must carry the user's intent through selection and state exactly what Save will do. | The picker now says `Choose Pull assistance`, names the target day, explains that filters come from library metadata but never assign the role, and uses a result CTA such as `Add 1 as Pull to Day A`. Completing it returns to the same routine/day instead of opening a placement editor or leaving the routine. |
| P0 | One legacy `assistanceRole` enum represented both `MainLift` structure and Push/Pull assistance. Program template identity was partly inferred from notes and shape. | Structural ownership and optional assistance classification are different invariants. Conflating them makes invalid edits, migrations, history, and future program changes likely. All existing and future structured-program users are affected. | `GymModels.kt` introduces `RoutinePlacementKind`, `RoutineAssistanceCategory`, and `RoutineProgramTemplateKey`. Entities/repositories persist exact template key/revision and kind/category; schema 35 and backup format 13 backfill conservatively. Repositories reject structural contradictions at their boundary. |
| P1 | Assistance identity was not a first-class immutable workout-history fact. | History must describe what was prescribed/performed at the time, even if a routine is reclassified later. | Workout exercise snapshots now store kind/category. Active workout and History display `Assistance · Pull` (or the relevant role) from the immutable snapshot, with deterministic legacy fallback. |
| P1 | Adding a set during a programmed workout could clone a required Main set, remain planned, and accidentally affect completion/progression. | User-authored in-workout work must be optional/ad hoc unless the program definition explicitly prescribed it. | `GymRepository.addSet` now keeps useful load/rep values but clears scheme/Joker identity, marks the set unplanned, and places it in Optional for programmed workouts. Repository regression coverage proves it cannot become required Main work. |
| P1 | `Primary category only` analytics secretly assigned an exercise to the linked category with the lowest database ID. The UI offered no way to control that winner. | A configurable-looking analytics choice must follow visible user order, not storage history. Library categories must also remain clearly separate from routine assistance roles. | Exercise Library category membership moved out of Advanced; category screens explain scope; analytics is named `First linked category only` and uses visible category position. Settings explains how to control the result and that it never assigns Push/Pull roles. |
| P1 | Some 5/3/1 lift-slot and increment defaults came from name matching without visible provenance; an unmatched slot silently used the next exercise. | Suggestions may reduce work, but hidden inference must never masquerade as a confirmed programming decision. | The wizard labels confident matches `Suggested from exercise name`; fallbacks show `Needs confirmation` and block Apply until confirmed/replaced. Cycle increments identify their editable suggestion source. |
| P1 | Toggling Joker policy regenerated Main/Supplemental sections and could overwrite customized notes/rest details even when those schemes had not changed. | Optional-feature toggles must not destroy unrelated authored details. | Phase policy application now regenerates only a deliberately changed scheme. Joker-only changes preserve Main/Supplemental details; a unit regression retains a customized supplemental note and rest value. |

### Explicit design dialectic

1. **Position A — labels-only refinement:** keep compact chips and add a sentence. This is
   cheap and preserves the existing layout, but still makes a consequential assignment look
   like a filter and leaves tiny targets in the busiest part of the routine screen.
2. **Position B — dedicated assistance exercise library:** create separate Push/Pull/Core
   lists. This makes roles visible, but duplicates the exercise catalog and falsely turns a
   routine-local programming decision into global exercise truth.
3. **Evidence and constraints:** the same movement can serve different roles across routines;
   general Gym users should not inherit 5/3/1 taxonomy; mobile lifters need a short path and
   large targets; existing exercises, histories, and category analytics must remain valid.
4. **Failure modes:** A preserves ambiguity and tap-density; B creates conflicting libraries,
   repetitive data entry, synchronization problems, and dogmatic classifications.
5. **Decision:** use a hybrid shared library with typed routine intent. Full-width role
   actions open the existing exercise catalog in a contextual assignment mode; the persisted
   placement records the role independently of library metadata.
6. **Why this is superior here:** it gives beginners an explicit source and outcome,
   experienced lifters a fast reusable catalog, non-5/3/1 users an uncluttered general path,
   and engineering one deterministic source of truth without inventing a generic DSL.

### Final assistance UX and correctness pass

The disposable API 34 emulator was rebuilt from an empty app and the complete path was
performed manually after the automated campaign:

- `assistance-outline.png` shows the full-screen routine hierarchy and explicit template
  provenance.
- `assistance-panel.png` shows the full-width role actions, plain-language role examples,
  separated unclassified work, and Previous Workout consequence text with comfortable
  spacing and one-handed targets.
- `pull-picker.png` shows the contextual Pull intent, target day, non-inference explanation,
  library-filter provenance, and intent-specific CTA.
- `assistance-returned-outline.png` proves the picker returns to the same routine/day and the
  new placement is visibly labelled `Assistance · Pull · rep target needs review`.

No layout collision, clipped action, dead end, ambiguous assignment source, or unintended
editor navigation was found in that final pass.

### Final verification and phone release: 0.3.31

- `ANDROID_SERIAL=emulator-5554 scripts/check --emulator --full` passed the final tree:
  **382 JVM tests and 461 Android instrumentation tests (843 total)**, zero Android failures
  and zero skips across eight isolated batches. Aggregate evidence:
  `build/instrumentation-results-LZVYlp/aggregate.tsv`.
- Lint, release APK, signed Play bundle, optimized benchmark build, Android-test compilation,
  and coverage gates passed. Coverage was 79.00% deterministic-domain lines, 53.92%
  deterministic-domain branches, and 63.62% core settings/policy lines.
- The release metadata check now derives its expected version code from
  `app/build.gradle.kts`; it can no longer drift behind the app due to a duplicated hard-coded
  release number.
- Signed release `0.3.31` / code 37 was installed with `adb install -r` on the connected
  Samsung SM-F976W. Existing app data was preserved; the original
  `firstInstallTime=2026-08-26 17:59:24` remains and
  `lastUpdateTime=2026-08-31 16:14:11` records the upgrade.
- The 3,695,504-byte installed APK matches the local artifact at SHA-256
  `978c77cf0fd625349c63a507f4f7d44143d0c3035544216baa388f8b1dff9df1`.
  The verified release-certificate SHA-256 remains
  `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`.
- A cold launch returned `Status: ok`, `MainActivity` became the resumed foreground activity,
  and post-upgrade logs contained no fatal, Room, SQLite, or migration error.

## Follow-up Gym-wide UI, accessibility, and control audit

The completed implementation was reopened from a clean disposable API 34 emulator and every
top-level Gym destination was walked again at phone width. Exercise Library and editing,
routine authoring, empty/no-history states, workout execution controls, and navigation were
also repeated at Android `font_scale=2.0`. This pass deliberately included ordinary routines
instead of treating structured programming controls as 5/3/1-only behavior.

| Severity | Observed behavior | Expected behavior and affected users | Implemented resolution and evidence |
| --- | --- | --- | --- |
| P1 | An ordinary routine placement supported `% Training Max`, but the Training Max control appeared below its sets only after an advanced path or a dependent set had already been configured. | The dependency must be discoverable before it is needed. General strength-programming users should be able to set a TM without creating or converting to a 5/3/1 program. Hidden ordering made a valid feature practically unconfigurable. | `RoutineBuilder.kt` now places a visible `Training Max & Percentage Loads` disclosure before the set editor for every compatible placement. It summarizes explicit/derived/missing TM state, opens automatically when `% Training Max` is chosen, and validates an explicitly entered TM independently of set mode. `RoutineBuilderUiTest#staticRoutineExposesTrainingMaxBeforeAnySetUsesIt` proves the non-5/3/1 path. |
| P1 | Exercise defaults could reach Save with an empty name, non-positive increment, impossible rest duration, or malformed/non-positive plate list; persistence rejected some values without identifying the field while plate parsing silently discarded others. | Invalid defaults must remain on screen and explain exactly what must change. All exercise creators are affected; silent plate loss is a data-integrity and trust problem. | `GymScreens.kt` now performs field-level validation plus an accessible `Review Required Fields` summary, expands Advanced when its values fail, and never submits partial plate parsing. `GymRepository.kt` independently rejects rest outside 1–86,400 seconds and non-finite/non-positive plates. UI and repository regressions cover both layers. `artifacts/gym-uiux-audit/postfix-exercise-validation-large.png` demonstrates the usable 200% text state. |
| P2 | Progress with configured exercises but no completed sets displayed an arbitrary selected exercise and metric beside `No eligible data`, making setup defaults resemble measured results. | Progress must distinguish configuration from history and avoid invented selection/trend state. New lifters and users importing exercises need confidence that charts reflect completed work only. | `GymScreens.kt` now returns an honest no-history state before metric controls, retains user-owned Tracked Records, explains that defaults never become results, and offers `Start a Workout`. `GymPowerInputUiTest#progressWithoutCompletedWorkoutsDoesNotShowInventedTrendControls` and `artifacts/gym-uiux-audit/postfix-progress-no-history.png` cover the behavior. |
| P2 | Rest timer controls were a single dense row and screen readers announced only `−15` / `+15`. At 200% text this risked crowding and did not communicate the action target. | In-gym controls must survive large text, one-handed input, fatigue, and nonvisual use. | `GymScreens.kt` stacks timer status and actions when width/font scale requires it and exposes `Start rest timer`, `Subtract 15 seconds from rest timer`, `Add 15 seconds to rest timer`, and `Stop rest timer`. A 200% text semantics/layout regression covers all actions. |
| P2 | Routine-set headers placed reorder, set identity, classification, duplicate, and delete controls in one horizontal row. | Set identity and destructive/reorder controls must remain readable and reachable at large text without truncating classification. | `RoutineBuilder.kt` uses a responsive header: classification moves to its own full-width row at narrow width or large text, and duplicate/delete semantics include the set number. Existing normal-width behavior remains compact. |

No new P0 persistence or calculation defect was found in this follow-up. The controls added
here do not alter historical workout snapshots or recompute completed work; they expose and
validate existing structured data before persistence. The active 5/3/1 calculation,
prescription, snapshot, eligibility, migration, and backup regressions all passed unchanged.

### Follow-up verification and phone release: 0.3.32

- `ANDROID_SERIAL=emulator-5554 scripts/check --emulator --full` passed the final tree:
  **382 JVM tests and 466 Android instrumentation tests (848 total)**, zero Android failures
  and zero skips across eight isolated batches. Aggregate evidence:
  `build/instrumentation-results-TzYohg/aggregate.tsv`.
- Lint, release APK, signed Play bundle, optimized benchmark build, Room schema/migration
  checks, and coverage gates passed. Coverage remained 79.00% deterministic-domain lines,
  53.92% deterministic-domain branches, and 63.62% core settings/policy lines.
- Signed release `0.3.32` / code 38 was installed with the data-preserving release path on
  the connected Samsung SM-F976W. The original
  `firstInstallTime=2026-08-26 17:59:24` remains; the successful upgrade records
  `lastUpdateTime=2026-08-31 17:59:00`.
- The installed and local release APKs match at SHA-256
  `7eebd1dd95b511d228efb28e56f17a21722270fa4268f79a2eefd25f73419b50`.
  A cold launch returned `Status: ok`, `MainActivity` is the resumed foreground activity,
  and post-upgrade logs contain no fatal, Room, SQLite, migration, or crash error.

## Adaptive Training Max and cycle-decision audit

The audit was reopened once more around a lifter's actual decision loop: establish a source
max, select an appropriate Training Max percentage, run a cycle, interpret PR/Joker/test
performance, and deliberately choose the next cycle's TM. A simulated five-person lifter
panel produced and challenged the following use cases; this remains an expert product
simulation rather than recruited human-subject research.

| # | Lifter use case | Final behavior |
| ---: | --- | --- |
| 1 | Start from a tested 1RM | Actual 1RM is identified as source data, multiplied by an editable TM percentage, and snapshotted. |
| 2 | Start from recent rep performance | e1RM is identified separately from actual 1RM and direct TM entry. |
| 3 | Feel weak today / choose a conservative TM | TM percentage is editable from 1–100%; values outside the common 80–90% range receive a warning rather than being blocked. |
| 4 | Feel unusually strong | A higher initial percentage is possible; the UI does not silently change it from workout performance. |
| 5 | Use a direct TM without a source max | Direct entry remains first-class and is audited as explicit TM provenance. |
| 6 | Use TM percentages in a non-5/3/1 routine | Every compatible routine placement exposes the same explicit/derived TM primitive before a `% TM` set is required. |
| 7 | Build standard four-lift 5/3/1 | Guided defaults remain available. |
| 8 | Build Bench/Deadlift/Zercher 5/3/1 | Any ordered, distinct Weight + Reps exercises may be program lifts; the standard four are not a lock-in. |
| 9 | Run classic PR sets | PR-set identity is preserved independently of performed outcome. |
| 10 | Run 5s PRO | Main scheme is distinct from supplemental choice. |
| 11 | Add FSL/SSL/BBB/BBS | Supplemental work is structured separately from assistance and optional work. |
| 12 | Add assistance by Push/Pull/Single-leg-or-Core intent | Category is an explicit routine-local choice, never guessed from the exercise library. |
| 13 | Use optional Jokers | Joker evidence is independent, optional, and only corroborates higher suggestions at a meaningful load. |
| 14 | Excel on one AMRAP | A single strong session cannot justify an above-standard increase by itself. |
| 15 | Excel across independent sessions | With the advanced opt-in enabled, corroborated PR/Joker evidence may produce a cautious, bounded higher suggestion. |
| 16 | Have a bad cycle | Failed required work holds eligibility; independent repeated misses or an explicit failed TM test may recommend a one-standard-step decrease. |
| 17 | Follow ordinary 5/3/1 progression | New programs default to the saved standard per-lift increase without requiring advisory review. |
| 18 | Review suggestions but decline one | Standard, suggestion, hold, ignore recommendation, and bounded custom decisions are separate, explicit, per-lift actions. |
| 19 | Resume/finish after closing the app | Only an unadvanced boundary session opens review; archived, mismatched-cycle, mismatched-program, or mismatched-TM evidence is excluded. |
| 20 | Inspect why the TM changed | Standard, advisory, ignored, custom, and manual edit decisions are persisted in program TM history; completed workout prescriptions never recompute. |
| 21 | Run a 7th Week TM test | Each logical main lift requires exactly one explicit 100%-TM 3–5-rep test prescription in the test phase. |
| 22 | Fail that TM test | Performed `Failure` no longer erases immutable TM-test intent, so the recommendation engine sees a failed test rather than an ordinary miss. |
| 23 | Set a five-minute rest timer | The first visible value is capped at 5:00 and the first elapsed second displays 4:59, never 5:01. |
| 24 | Edit a routine on a narrow/foldable screen | The editor/exercise pane can use the full available pane, and adding an exercise returns to the same routine/day. |

### Significant findings and resolutions

| Severity | Observed behavior | Expected behavior / why / affected users | Implemented solution and evidence |
| --- | --- | --- | --- |
| P1 | Training Max provenance could be inferred from a UI chip and could not be re-derived after program creation. | Actual 1RM, e1RM, TM, and working load must remain different facts. Novices need teaching; experienced lifters need editable control. | Typed basis kind/value/unit and `trainingMaxPercent` are persisted, repeated placements synchronize them, and Program Structure offers explicit Actual/e1RM/Other/Direct re-derivation. `RoutineBuilderUiTest`, `RoutineRepositoryTest`, schema 36, backup 14. |
| P1 | Cycle progression was either automatic standard behavior or an unaudited caller decision; the app could not safely recommend a lower/higher/no increase from immutable evidence. | Advice must be optional, per lift, explainable, and transactionally recomputed so UI or stale callers cannot forge it. All performance-review users are affected. | `FiveThreeOneProgression` is a pure engine; repository commit reselects immutable same-cycle/same-kind/same-TM evidence and validates action/delta/eligibility/higher opt-in. Decisions and reasons are audited. `FiveThreeOneProgressionTest`, `FiveThreeOneCycleReviewTest`, `RoutineRepositoryTest`. |
| P1 | PR/Joker evidence could be overstated by several sets from one session, a light Joker, or missing effort data. | A higher-than-standard suggestion is consequential and requires independent corroboration plus meaningful effort/load evidence. Experienced lifters and novices trusting defaults are affected. | Evidence carries exposure ID and TM snapshot; strong evidence requires distinct sessions, meaningful Joker load, and RPE/RIR. The UI calls this `Evidence strength`, not statistical confidence. |
| P1 | UI evidence and transactional evidence differed after a program-kind edit. | A displayed action must be commit-able against the same cohort. Otherwise the final workout appears broken. | Both boundaries now require the active and evidence sessions' snapshotted program kind to match the routine. Cross-kind unit/repository regressions cover both paths. |
| P1 | TM-test phases allowed zero designated tests, and marking a prescribed test as failed erased test identity. | A test phase must be structurally valid and performance outcome must not overwrite prescription intent. 7th Week/test users and historical correctness are affected. | Exact-one validation spans repeated placements; schema 37 adds immutable `prescribedClassificationSnapshot`; backup 15 and 36→37 migration preserve existing data conservatively. Persisted failure regression proves Decrease Review. |
| P1 | Derived-TM source fields remained editable after Apply, so `200 × 85% = 170` could be followed by an unapplied source edit to 300 while Save persisted false `300 × 85%` provenance for the unchanged 170 TM. | Provenance must describe the source the user actually applied, including edits that happen to round to the same TM. Novices, History readers, and migration/decision audits are affected. | Both guided setup and Program Structure now keep draft-only pending derivation inputs separate from applied provenance. Build/Save is disabled with an accessible explanation until Apply commits source/kind/unit/percentage and TM atomically. Compose regressions cover 300 and same-rounded 201 edits. |
| P1 | Repeated-main-lift synchronization copied a designated TM-test set into every occurrence, conflicting with the repository's exact-one test invariant. | Beginners-style repeated squat/bench schedules must share ordinary prescriptions while only one placement owns each lift's phase-local test. | Builder synchronization now copies non-test work separately, preserves or deliberately transfers one test owner per phase/lift, and rekeys sibling sets uniquely. `RoutineBuilderStateTest#repeatedLiftSynchronizationPreservesOneTrainingMaxTestOwnerPerPhase` covers the authoring rule. |
| P1 | A selected 5:00 rest timer could briefly render 5:01 because Room published a fresh deadline before the UI's one-second clock caught up. | Selected duration is an authoritative upper bound; the countdown must be trustworthy at a glance. Every in-gym user is affected. | Ceiling deadline arithmetic is clamped to the configured duration, and timer adjustments use the same boundary semantics. Tests cover stale-clock, +1 ms, +999 ms, +1000 ms, expiry, and repository adjustments. |
| P2 | Manual TM changes were not part of visible program decision history. | Historical workouts must remain immutable while future-program edits remain explainable. | Routine updates create explicit TM-edit audit rows and History renders decisions not tied to a workout session. |

### Product decisions

- 5/3/1 remains a hybrid first-class template plus advanced routine/program structure; normal
  routines retain a direct path and do not inherit 5/3/1-only language.
- Standard progression is the safe default. Performance-informed review is opt-in and never
  silently applies a higher, lower, or zero change.
- Recommendations are advisory, not an attempt to replace lifter judgment. Standard, Hold,
  Ignore, and Custom remain available even when Whip has evidence.
- Leader/Anchor and 7th Week roles are structurally representable, but book-specific complete
  templates are not advertised as one-tap canonical programs where public prescriptions are
  insufficient.

### Final merged verification and phone release

- `ANDROID_SERIAL=emulator-5554 scripts/check --emulator --full` passed **401 JVM tests and
  481 Android instrumentation tests (882 total)**. All Android tests executed with zero
  skips across eight isolated batches; aggregate evidence is
  `build/instrumentation-results-wZbb2X/aggregate.tsv`.
- Lint, debug/release APK, Play bundle, optimized benchmark build, Room schema 37, backup
  format 15, and release metadata checks passed.
- Coverage passed at 81.07% deterministic-domain lines, 56.38% deterministic-domain branches,
  and 63.62% core settings/policy lines.
- After two adversarial NO-GO rounds and remediation, the Gym Product Director, methodology
  reviewer, and final lifter panel returned GO with no remaining P0/P1 blocker.
- Signed release `0.3.33` / code 39 was built and certificate-verified. The 3,761,044-byte
  APK SHA-256 is `9f70fb5e84f2b858703e2e1462f7d38aee9241fe99223da86d6c241a00b4ca20`;
  its signing-certificate SHA-256 remains
  `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`.
- Release `0.3.33` was installed on the Samsung SM-F976W with `adb install -r`; Android reports
  code 39, version `0.3.33`, preserved `firstInstallTime` (`2026-08-26 17:59:24`), and updated
  `lastUpdateTime` (`2026-08-31 20:23:58`). The installed APK's SHA-256 matched the signed local
  artifact byte-for-byte. A cold launch placed `MainActivity` in `ResumedActivity`, and the
  launch log contained no fatal, AndroidRuntime, SQLite, Room, or migration failure.

## Advanced 5/3/1 expansion: 2026-09-02

The former optional backlog is now an emulator- and release-build-verified implementation:

- One-tap BBB Leader → FSL Anchor and FSL Leader → FSL Anchor structures generate two
  three-week 5s PRO Leaders, a 7th Week transition, one three-week PR-set/FSL Anchor, and a
  closing selectable 7th Week protocol. Arbitrary ordered Weight + Reps lifts remain supported.
- Deload, Training Max Test, and PR Test are one-tap, reviewable prescriptions in both initial
  setup and Program Structure. The app shows all generated percentages and calls the presets
  book-guided/editable rather than claiming unsupported official-edition parity.
- BBB can use the day's Main lift or another selected program lift. Alternate BBB is a distinct
  Supplemental placement, uses the alternate lift's own TM, and stays synchronized when that
  lift's TM advances.
- Jokers support one to three optional +5% or +10% TM candidates. Each candidate is logged
  independently and appears only after successful prerequisite work; skip, failure, missed
  targets, RPE 9+, or RIR 1 or lower ends the ladder without removing Supplemental work.
- Setup can draft Push/Pull/Single-leg-Core assistance from compatible existing Library
  exercises, with visible 3×10 targets (5×10 for Beginners), deterministic suggestions, and
  explicit replacement/omission. It never creates an exercise or demotes a canonical main lift
  silently.
- Repeated-lift Beginners schedules keep synchronized editable protocol templates and assign
  one balanced runtime owner per logical lift. Squat executes Monday, Deadlift + Press execute
  Wednesday, and Bench executes Friday, so every protocol runs each lift exactly once without
  an invalid save, duplicated test, TM-edit drift, or empty training day.
- That once-per-lift runtime rule is provenance-gated to 5/3/1 template revision 2. Existing
  revision-1 Beginners deloads continue to execute both saved Squat and Bench exposures; using
  an explicit new protocol action durably opts the routine into revision-2 semantics.
- Program Structure identifies alternate-lift BBB by exercise, includes it in the phase policy
  and prescription summary, preserves it when Main/Joker controls change, and replaces it only
  when the user explicitly selects another Supplemental scheme. Existing-program 7th Week
  actions show their percentage/rep matrices before applying.

Current support status:

| Capability | Status after expansion |
| --- | --- |
| Leader / Anchor | First-class for the two named generated structures; every phase remains editable. |
| 7th Week Deload / TM Test / PR Test | First-class one-tap explicit matrices and phase roles. |
| Alternate-lift BBB | First-class within generated one-main-lift-per-day programs. |
| Multi-Joker ladder | First-class optional 1–3 candidates with bounded workout gating. |
| Automatic assistance | First-class transparent draft from the user's compatible active Library. |

Verification is recorded in `VER-20260902-017`: 560 JVM and 921 Android tests passed with
zero failures/skips, followed by the complete release/R8/benchmark gate. No Room/backup format
changed, no existing routine or completed workout was recomputed, and this candidate has not
been installed on a physical phone.
