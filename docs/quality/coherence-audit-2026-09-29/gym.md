# Gym design consistency, coherence, and UX audit — 2026-09-29

Baseline: 6d79bd1f. Scope: Gym only. This is an audit, with no production, test, or canonical product-memory edits and no builds, tests, device commands, or commits by this reviewer.

## Evidence and interpretation

Current source is authoritative. Prior decisions were reconciled using maintain-whip-memory, INDEX, current FINDINGS/DECISIONS/USER_FEEDBACK, relevant implementation and verification records, and the earlier Gym audit workbook. Existing test bodies were inspected for intended contracts and gaps; their presence is not a test pass. One fresh parent-run phased launch test is explicitly identified as F4 below. “Source coherent” means the inspected ownership, wording, and transition agree, not that every runtime state was exercised.

Fresh native artifacts came from the parent reviewer on the current baseline. This reviewer personally inspected the original PNGs and XML for the empty-workout reproduction, and the original fresh populated-workout PNG. Older images were inspected as dated evidence, not fresh executions.

| Evidence | Original artifact and observed scope |
| --- | --- |
| F1 | [Fresh normal active workout](../../../artifacts/coherence-audit/2026-09-29/fresh/supporting/gym.workout.populated.png), with corresponding XML supplied in the same directory. Active Full Body, Goblet Squat, previous suggestion, load/reps composer, compact next/rest lane. PNG inspected; this review does not claim an interaction journey from that image. |
| F2 | [Fresh empty workout before creation](../../../artifacts/coherence-audit/2026-09-29/fresh/manual/gym.active-empty.png) and [XML](../../../artifacts/coherence-audit/2026-09-29/fresh/manual/gym.active-empty.xml). PNG and XML personally inspected. Shows 0/0 sets, Add Your First Exercise, and Create New Exercise. |
| F3 | [Fresh empty workout after creation](../../../artifacts/coherence-audit/2026-09-29/fresh/manual/gym.active-empty-after-create.png) and [XML](../../../artifacts/coherence-audit/2026-09-29/fresh/manual/gym.active-empty-after-create.xml). PNG and XML personally inspected. Parent reports creating and saving “Audit empty route exercise”; view returns to the same 0/0 empty workout. |
| F4 | [Fresh phased workout](../../../artifacts/coherence-audit/2026-09-29/fresh/gym-phased-launch/gym.routine-phased.workout.png) and [XML](../../../artifacts/coherence-audit/2026-09-29/fresh/gym-phased-launch/gym.routine-phased.workout.xml), plus [active-source editing block](../../../artifacts/coherence-audit/2026-09-29/fresh/gym-phased-launch/gym.routine.active-blocked.png) and [XML](../../../artifacts/coherence-audit/2026-09-29/fresh/gym-phased-launch/gym.routine.active-blocked.xml), personally inspected. Parent-run routineStartsFromLibraryAndShowsItsActualPhaseInWorkout: [result XML](../../../artifacts/coherence-audit/2026-09-29/fresh/gym-phased-launch/result.xml) and [aggregate](../../../artifacts/coherence-audit/2026-09-29/fresh/gym-phased-launch/android-aggregate.tsv) read: 1 executed, 0 failures/errors/skips, no reuse. Proves Library launch, actual Custom phase and active-source editing guard, not scheme/warm-up preservation. |
| D1 | [Dated Library child at 200%](../../../artifacts/workspace-anchors/2026-09-29/accepted/workspace-consistency.gym.library-child.native200.png). Shared Gym header, Library child back row, empty exercise collection, scrollable lower content. |
| D2 | [Dated populated workout at 200%](../../../artifacts/fresh-app-overhaul/2026-09-29/after/large-supporting-final/gym.workout.populated.png). Tall next/rest lane and lower composer. Does not establish short-window or IME reachability. |
| D3 | [Dated large-text session overview](../../../artifacts/deep-product-review/2026-09-29/after/deep.gym.session-overview.native200.png). Full long exercise identity, complete set count, clear follow-order and finish actions. |
| D4 | [Dated direct historical correction](../../../artifacts/fresh-app-overhaul/2026-09-29/after/tracks-gym/fresh.gym.history-correction.png). Finished session expanded with direct Edit and Open Active Workout. Historical count grammar in this image is already fixed in current source. |
| S | Current source inspection, including relevant repository/domain owners. Deterministic data transforms are distinguished from unobserved scheduling/recreation behavior. |
| T | Existing test source read, not executed. Selected classes are listed below. |
| U | Runtime state not demonstrated by inspected artifacts. Listed explicitly under remaining unknowns. |

Source shorthand used in the coverage table:

- G: app/src/main/java/com/whip/app/ui/GymScreens.kt.
- R: app/src/main/java/com/whip/app/ui/RoutineBuilder.kt.
- VM: app/src/main/java/com/whip/app/ui/GymViewModel.kt.
- Repo: app/src/main/java/com/whip/app/data/GymRepository.kt.
- Catalog: app/src/main/java/com/whip/app/ui/GymCatalogMutationUi.kt.
- Host: app/src/main/java/com/whip/app/ui/GymDestinationHost.kt.
- Dialog: app/src/main/java/com/whip/app/ui/ProductivityEditorComponents.kt.
- Cycle: app/src/main/java/com/whip/app/ui/FiveThreeOneCycleReview.kt.
- Retirement: app/src/main/java/com/whip/app/data/LegacyRoutineRetirement.kt.

## Prioritized findings

Nine grounded findings: two P1 and seven P2. Priority reflects loss of authored programming first, then completion/state ownership, then recoverability and accurate scope. Only GYM-03 has a fresh native action reproduction in this component review; the other findings are source-grounded.

### GYM-01 — P1 — Phase-scoped prescription tools rewrite the entire exercise

**Reproduce:** Create an ordinary Custom phased Routine with two phases. Give an exercise three distinct working sets in Phase 1 and three in Phase 2. Open that exercise, select Phase 2, and apply a saved two-set rep prescription. Alternatively author distinct warm-ups in each phase, select Phase 2, and generate equipment-aware warm-ups.

**Evidence:** R:2213–2920 RoutinePlacementEditor displays only selected-phase and common sets at 2589–2614, but the scheme action at 2728 passes all current.sets to applyRepPrescriptionScheme. R:4141–4155 returns exactly scheme.setCount rows, seeded from the first rows of the entire input. Custom phased placements are not excluded by the legacy program-controlled-placement guard. The warm-up action at R:2745–2752 also passes the entire placement; generateWarmupSets at 5189–5237 chooses the first global working set, creates unscoped rows, and removes every existing WarmUp row across all phases.

**Exact before/after:** With globally ordered [P1-A, P1-B, P1-C, P2-A, P2-B, P2-C], applying a two-set scheme while viewing Phase 2 produces [P1-A', P1-B']. Both retained rows keep their original Phase 1 index; all Phase 2 rows and the third Phase 1 row disappear. No selected-phase argument reaches the helper. With P1 working load 100 and P2 working load 60, generating while viewing P2 computes the ramp from 100, removes both phases' authored warm-ups, and inserts warm-ups with no phase index, so they become common to every phase. This is a deterministic source transformation, not a claimed native execution.

**Impact:** The editor presents phase-local work while silently deleting or rewriting programming outside that scope. Save makes that loss persistent, and subsequent workouts can instantiate the altered prescription.

**Remedy:** Apply both tools to an explicitly selected phase scope and merge the result back into the complete placement, preserving untouched phase rows, keys, and ordering. Define whether common rows are included or edited separately. Warm-up generation should choose that phase's working prescription and create rows with its phase index. Any deliberate all-phase replacement needs an explicit scope and review, not the current local action. Acceptance should compare all nonselected-phase fields before/after apply, save/reopen, and workout instantiation; existing static scheme/warm-up tests do not cover this seam.

### GYM-02 — P1 — Routine Save has no request-owned lifecycle and permits continued edits or closing

**Reproduce:** Open a new or existing Routine, return to its outline, tap Save with persistence delayed, then edit its name/day/structure or use the header close/system Back. Repeat with recreation or process interruption while the save is pending.

**Evidence:** R:197 keeps routineSaveInFlight as a saveable local Boolean. R:211–236 requestDismiss and BackHandler do not gate on it. R:379 disables Save, but R:390–412 leaves navigation active and captures the submitted builder in an asynchronous callback. RoutineBuilderHeader at 767–813 keeps Back/Close active and the button reads only Save. VM:2171–2187 launches the routine save with a callback and global OperationStatus, without an owned request/result receipt. On success, a new Routine clears the state holder and closes; an existing Routine installs the captured builder as its saved baseline.

**Impact:** Changes made after submission are not part of the saved draft; a successful new-Routine save can clear them. A user can approve discarding or leave while a save still creates the Routine. A restored true local Boolean lacks a recreated receipt owner that can reliably settle it after interruption. The concurrency/input mismatch is deterministic in source; the precise recreation symptom remains a runtime follow-up.

**Remedy:** Use the request/receipt persistence ownership already used for child Library saves, with a stable request identity, explicit Saving state, inline terminal failure/retry, and interrupted-save recovery. Block the Routine editor's inputs and exit/navigation while submitting the owned draft, or explicitly preserve subsequent edits as a separate draft. Existing failedRoutineSaveKeepsTheCompleteDraftOpenForRetry reads an immediate failure callback; it does not establish delayed-save or recreation behavior.

### GYM-03 — P2 — Empty-workout Create New Exercise saves to Library but does not populate the workout

**Reproduce:** Start a blank workout; choose Create New Exercise directly from Add Your First Exercise; enter a name and save. Parent performed this on the current debug baseline. F2/F3 PNG and XML confirm the empty 0/0 surface before and after the supplied creation journey.

**Evidence:** G:3279 promises creation without leaving the workout and says this changes only this workout. Its action at G:1351–1357 opens the reusable exercise editor without capturing an add-to-workout structure boundary or requested placement/set identities. Catalog:115–212 chooses create-and-add only when those owners exist; otherwise it saves a Library exercise. Add Exercise establishes the structure boundary and placement/set identities at G:1364–1371; its picker Create action preserves that owner at G:1913–1919, demonstrating an existing correct path.

**Impact:** A prominent empty-state recovery action completes without recovering the empty state. The same copy also misstates reusable catalog creation as workout-only. Users must discover that the exercise was saved elsewhere, reopen the picker, and add it manually.

**Remedy:** Route this direct Create action through the existing atomic create-and-add path, capturing the current workout boundary and stable placement/set identities. State that it creates a reusable Library exercise and adds it to this workout. Preserve the authored draft and local failure receipt if either part cannot commit.

### GYM-04 — P2 — Insights source drill-down removes its analytical scope and has no return owner

**Reproduce:** Choose a nondefault exercise, equipment scope, metric, range/custom dates, and comparison in Insights. Open a chart point or tracked record, choose View History, then return through Back or the Insights tab.

**Evidence:** G:8067–8159 stores the analysis selections and disclosures in local rememberSaveable state. At G:8570–8578 View History clears the selected point and routes to History. G:1503–1510 changes destination and focusedWorkoutId. The destination switch at G:1309–1520 disposes GymProgressContent; Host:20–55 has no per-Gym-destination SaveableStateHolder. The outer WhipApp holder protects the app destination, not Gym's internal routes. G's Back owner handles Library children, while ordinary shell Back is handled by WhipApp:1129–1137. G:1293–1303 tab selection also clears the focused source. Tracked-record source actions at G:7625–7715 use the same route.

**Impact:** The precise source workout can be opened, but the analysis that motivated the visit is not retained with an explicit return transition. Returning to Insights reconstructs defaults and loses the comparison/range/scroll context. This makes evidence verification much more expensive than ordinary source drill-down.

**Remedy:** Give each Gym destination stable saveable ownership or hoist the analysis scope, and record the drill-down origin. Back from an Insights-origin workout should return to the same exercise/equipment/metric/range/comparison/disclosure/scroll context. Normal entry into History can keep its existing browse semantics. Native return/recreation behavior remains unprobed here; do not treat the existing exact-source-point test as return coverage.

### GYM-05 — P2 — Several saving editors remain interactive after capturing the submitted draft

**Reproduce:** Delay a Set Details save, Workout Details/start save, or eligible active legacy cycle-review Apply. Tap the primary action, then change a visible field or choice before completion.

**Evidence:** WorkoutSetEditorDialog at G:10424–10558 omits ProductivityEditorDialog's inputBlocked parameter. Save and dismissal are guarded, but fields/toggles remain writable while the submitted pendingDraft is fixed. WorkoutEditorDialog at G:10841–10944 likewise omits inputBlocked, captures name/notes/date/keepAwake, and closes on success. Cycle:280–477 guards dismissal and Apply but leaves choice/custom input content active. Dialog:458–595 already provides an input-blocking overlay and semantics treatment. Exercise (G:9838–9840), Machine (G:6156–6158), quick Machine (R:3501–3504), Category, Group, and delete dialogs use that existing contract.

**Impact:** The screen can show a later value that will never be saved, then close as though that value succeeded. This is inconsistent with nearby editors that visibly own the pending submission. No delayed native save was executed here.

**Remedy:** Pass the existing saving state into inputBlocked with a specific saving label for these dialogs, and retain only the owned submitted draft until the receipt settles. Workout Details also needs request-owned interruption handling rather than its current nonsaveable local saving Boolean. Legacy cycle review is a compatibility-only case; this recommendation does not restore retired 5/3/1 creation.

### GYM-06 — P2 — Selecting a plate preset changes the target's unit without converting or reviewing its meaning

**Reproduce:** In Library → Tools → Plate Calculator, enter a 100 kg target, then select a pound plate preset. Reverse with a 225 lb target and a kilogram preset.

**Evidence:** G:8617–8627 keeps targetWeight and plateUnit separately. The explicit unit dropdown at 8776–8779 uses pendingPlateUnit and the Convert/Keep/Reset meaning-change owner. Its conversion helper at 8630–8641 preserves physical mass. Preset selection at 8757–8766 directly assigns plateUnit and equipment values without touching targetWeight or invoking that owner.

**Impact:** 100 kg becomes a target of 100 lb; 225 lb becomes 225 kg. The new label is visible, but a routine equipment-selection action silently changes the physical target and yields a different loading plan than intended. This is inconsistent with the calculator's explicit unit-change flow.

**Remedy:** Route preset changes across units through the same meaning-change operation, preserving the target mass by conversion or asking the existing Convert/Keep/Reset choice. Bar, plate, collar, and inventory values may intentionally come from the selected preset. Verify both directions with an authored target and same-unit presets.

### GYM-07 — P2 — Shared workout text makes planned and performed sets indistinguishable

**Reproduce:** Run a routine containing two prefilled equal sets. Complete one, leave the other incomplete, finish with the existing incomplete-work review, then share the completed workout.

**Evidence:** G:11453–11478 workoutShareText includes all nondeleted set rows and renders values, classification, optional RPE, and notes. It does not render completed/planned state or the placement's execution outcome. HistoricalWorkoutSetRow at G:7406–7445 does distinguish execution status, and CSV export at BackupRepository:285–287 includes workout/exercise/set state. A finished workout can intentionally retain incomplete planned rows.

**Impact:** A retained prescription reads as an achieved set in the exported human-readable summary. The exported representation contradicts History's performed/not-performed truth, rather than merely omitting ancillary metadata.

**Remedy:** Reuse execution status labels in shared text so completed, planned/not performed, and skipped outcomes remain explicit. Alternatively emit performed-only text with that scope stated. Keep the current equipment/unit snapshots; do not infer completion from numeric values.

### GYM-08 — P2 — Secondary authoring dialogs do not protect changed drafts on Back/outside dismissal

**Reproduce:** Change a Category's name/type, author a quick Machine from a Routine, edit a saved rep-prescription scheme, or change several entries in Manage Rest Presets; press Android Back or dismiss the dialog outside its surface without saving.

**Evidence:** Category dialog G:6667–6728 clears its open identity without a dirty fingerprint or unsaved review. R:3478–3552 quick Machine and R:2960–3075 scheme dialogs call onDismiss directly outside saving. G:4734–4883 Manage Rest Presets has a local multi-entry presetDraft and direct dismissal. In contrast, Exercise, Machine, Set, Workout, and Group editors use the existing dirty fingerprint/UnsavedChangesDialog pattern. Category raw values live above its dialog and may remain for a same-screen reopen; that is an incidental retention mechanism, not a preserved explicit editor draft across navigation.

**Impact:** System/back dismissal has inconsistent meaning across otherwise similar authoring flows. Multi-field or multi-entry work can disappear without the protection users encounter in primary Gym editors. Simple one-shot selection dialogs do not need this treatment.

**Remedy:** Apply the existing changed-draft dismissal guard to these authoring dialogs, preserve the draft for Keep Editing, and keep unchanged dismissal immediate. In Manage Rest Presets distinguish abandoning the preset list from changing a temporary workout rest value. Do not add confirmations to unchanged pickers.

### GYM-09 — P2 — Add from Workout silently excludes every session beyond the latest 50

**Reproduce:** With more than 50 completed workouts, edit a specific existing Routine day and choose Add from Workout to reuse the 51st or an older workout.

**Evidence:** R:3410–3437 WorkoutPickerPage shows gymState.history.take(50) with no search, pagination, Show More, or visible cap explanation. The full history exists elsewhere; importWorkoutIntoDay at R:4021–4078 operates on the selected target day. Saving an older session as a separate Routine from History is not equivalent to importing into that existing day.

**Impact:** Reuse is a primary authoring flow but excludes valid older source material invisibly. Users cannot tell whether their workout is missing, unsupported, or simply outside an unexplained window.

**Remedy:** Provide contextual search and/or incremental Show More for this authoring picker, retaining the selected Routine day and showing the visible/total scope. The shared global search rule for main collection pages permits local contextual picker search; no second main-library search owner is needed.

## Flow-state coverage and dispositions

Each row identifies the actual source owner and the inspected state. “Keep” is justified source coherence unless native evidence is explicitly named. Unknowns are not passes.

| ID | Flow/state | Source symbol and lines | Evidence | Disposition |
| --- | --- | --- | --- | --- |
| GY01 | Gym landing, four jobs, shared shell/child back | G GymAreaContent 602–741, 1287–1310; Host 20–55 | S, F1, D1, GymWorkspaceConsistencyUiTest source | Keep four job ownership and shared workspace context; child back returns Library. |
| GY02 | Loading/data-unavailable/recovery interaction gating | G GymAreaContent 602–1286; VM mutation generation/owners | S | Guards exist; interrupted recovery runtime unknown. Do not claim every error presentation exercised. |
| GY03 | First-use start with no Library or history | G WorkoutStartContent 2745–2820 | S | Keep direct blank start and useful create/library affordances. |
| GY04 | Returning start, quick Routine subset, latest workout | G workoutQuickRoutines/routineStartDay 2727–2744; WorkoutStartContent 2745–2820 | S | Coherent bounded launchpad and exact latest history source. |
| GY05 | Blank start title, date, notes, keep-awake | G WorkoutEditorDialog 10841–10944; VM startWorkout 1451–1479; Repo startWorkout 630–677 | S, T | Date is chosen for a new session; see GYM-05 for pending editor input. |
| GY06 | Template/routine/day/programmed next-day start | G RoutineContent 8884–9417; R toRoutineDraft 3657–3778 | S, F4 | Fresh parent-run Custom phase launch passed, with actual phase and explicit TM target visible. Full authoring and all start variants remain unprobed. |
| GY07 | Start with equipment or archived source resolution | G MachineChoiceDialog 6505–6557; Repo requireResolvedEquipmentForNewWorkout 2683–2688 | S, T | Keep requiring usable equipment for a new workout while preserving historical snapshots. |
| GY08 | Repeat previous workout creates a new execution | Repo duplicateWorkout 1155–1229; G WorkoutHistoryCard 7080–7368 | S, GymRepositoryTest source | Keep fresh clock and clean planned execution; performed/history source is preserved. |
| GY09 | Copy a historical exercise into active workout | VM/history-copy ownership; Repo copyWorkoutExerciseToActive 1230 onward; G History route 2648–2726 | S, T | Exact source/active structure owners and retry boundary are coherent. Runtime recreation not rerun. |
| GY10 | Active session title/date/elapsed/count hierarchy | G WorkoutContent 2821–3517 | S, F1, D2 | Keep quiet identity and overview entry. Active date is not silently edited; new-date creation is separate. |
| GY11 | Empty active workout recovery | G 3275–3290, onCreateExercise 1351–1357; Catalog 115–212 | S, F2/F3 | GYM-03, native confirmed. |
| GY12 | Populated active next-set and local composer | G WorkoutContent 2869–3268; QuickSetEntry 4259–4584 | S, F1 | Keep next required work before setup/history details. Short/IME height remains unknown. |
| GY13 | Overview jump/follow-order/finish | WorkoutOverviewDialog 18–94; G requested execution/overview state 2869–3020 | S, D3, overview/grouped navigation test source | Keep explicit selection vs follow-order. Long identity is exposed in overview. |
| GY14 | Completed set disclosure while next set remains primary | G WorkoutExerciseCard 3636–4179 | S, F1, T | Keep progressive disclosure; no demand for always-expanded completed cards. |
| GY15 | Weight+reps, weight-only, weight+duration | G QuickSetEntry 4259–4584; WorkoutSetEditorDialog 10266–10568 | S, T | Fields depend on tracking contract; Keep accurate load labels and validation. |
| GY16 | Reps-only/bodyweight/assisted bodyweight | Same G editors; shortLabel 11134–11182 | S, policy/repository test source | Keep external vs effective bodyweight and assistance semantics. Native specialty inputs unknown. |
| GY17 | Duration, distance+duration, distance-only and other tracking combinations | Same G editors; R RoutineSetEditorCard 3076–3271 | S, T | Required fields follow type; units are exposed. Every combination not rerun on device. |
| GY18 | Per-hand, per-side, added-load and canonical resistance | G editor load labels 10431 onward; steppedWorkoutLoad 11437–11452; Repo retargetWorkoutLoad 2812–2922 | S, T | Keep distinction between entered value and canonical resistance. |
| GY19 | Machine mass/ordinal/reverse ordinal/add-on/pulley mapping | G MachineEditorDialog 5967–6504; Set editor 10266–10568; R withLevelMachinePrescriptions 3913–3929 | S, T | Keep ordinal settings distinct from mass and compatible equipment scoping; native all machine configurations unknown. |
| GY20 | Unit changes and value meaning | G DefaultsMeaningChangeDialog 10239–10265; Exercise editor 9697–10198; R withProgramMassUnit 3945–3972 | S, T | Keep explicit Convert/Keep/Reset where implemented; preset bypass is GYM-06. |
| GY21 | Raw malformed drafts and validation | G workoutSetRawNumberError 10569–10576; editors raw saved fields 10305–10423 | S, relevant power-input test source | Keep raw author text and field error; previous sanitization-loss defect is already fixed. |
| GY22 | Reps/load/duration increments and zero/negative validity | G NumberField/SteppedNumberField 10577–10648; QuickSetEntry validation | S, T | Keep type-specific validation and optional-field distinction. Keyboard/locale combinations unknown. |
| GY23 | RPE, RIR, tempo, note, unilateral, rest override | G QuickSetEntry 4315 onward; Set editor 10325–10568; Exercise policies 9697–10198 | S, T | Optional detail remains secondary; GYM-05 concerns pending save input. |
| GY24 | Previous values: eligibility, conversion, equipment type | G QuickSetEntry 4259–4584; source prior/compatible suggestions owners | S, F1, conversion test source | Keep current display-entry units and rejecting ordinal/mass mismatch. Old mixed-unit issue is fixed. |
| GY25 | Prescription vs actual and progress guidance | G prescriptionLabel 11183–11225; exercise card 3636–4179 | S, T | Keep separate planned and performed values and guidance near execution. |
| GY26 | Quick draft → Set Details handoff and refresh | G editedQuickSetDraft/refresh 686–694; QuickSetEntry 4259–4584 | S, T | Keep current full handoff and explicit refresh. Existing regression test read, not rerun. |
| GY27 | Quick Log exact pending authored values and busy boundary | G quick authorship state 4285 onward; VM session mutation owner; Repo set mutation owners | S, T | Keep idempotent/concurrency-owned logging. No delayed native log probe here. |
| GY28 | Set Details Save/failed Save/dirty close | G WorkoutSetEditorDialog 10266–10568 | S, T | Raw draft, error and dirty close coherent; pending input remains GYM-05. |
| GY29 | Optional set accept/skip/Undo vs required next set | G selectPendingOptionalWorkoutSet 2389–2421; accepted/skip state 2869 onward | S, T | Keep optional gate distinct from required completion; undo bound to current session/generation. |
| GY30 | Legacy Joker prerequisite/effort stop/optional ladder | G 2371–2421; Cycle compatibility owners | S, T | Retained active-legacy behavior only. No new Joker authoring expected. |
| GY31 | Add existing exercise, picker search and atomic authored creation | G add action 1364–1371 and picker handlers 1880–1919; GymExercisePicker 37–180; Catalog 115–212 | S, T | Picker contextual search is appropriate; atomic picker creation is keep. Direct empty creation diverges, GYM-03. |
| GY32 | Choose/create equipment for active placement | G equipment/machine boundary state 717–731; MachineChoiceDialog 6505–6557; Catalog owners | S, T | Keep local owned failure and equipment snapshots. |
| GY33 | Arrange exercise blocks and set rows | G reorderWorkoutBlock/member 2470–2494; arrangement state 2877–2883 | S, T | Keep groups as blocks and stale structure rejection rather than silent moves. |
| GY34 | Superset/circuit creation, custom name and rotation | G buildWorkoutExerciseBlocks 2445–2465; WorkoutExerciseGroupSurface 3566–3635; WorkoutGroupDialog 10995–11133 | S, grouped journey/repository test source | Keep contiguous group semantics, explicit member order, dirty guard and saving overlay. |
| GY35 | Group/ungroup/layout undo and session isolation | G visible layout undo 351–370; Repo restoreWorkoutLayout 1779 onward | S, T | Keep exact layout snapshot restoration bound to active session. |
| GY36 | Substitute/remove exercise or set with prior performed rows | G action/card boundaries 3699–4179; Repo substituteWorkoutExercise 1528 onward, deleteSet 2335 onward | S, T | Keep review of consequences and stale identity guard. Historical work is not inferred from current catalog. |
| GY37 | Rest precedence app/exercise/set/workout override | WorkoutRestDuration 1–22; G RestTimerCard 4585–4733 | S, rest journey/repository test source | Keep explicit selected-source label and per-workout override. |
| GY38 | Rest start, auto log start, pause/cancel/adjust and deadline | G RestTimerCard 4585–4733; Repo startRestTimer/adjustRestTimer 2369–2390 | S, T | Deadline-based persistence and terminal cleanup are coherent. Native background/recreation unprobed here. |
| GY39 | Rest presets and custom time limits | G RestDurationDialog 4734–4883 | S, T | Keep labelled custom time and bounded presets. Changed multi-entry dismissal is GYM-08. |
| GY40 | Notification denied/channel disabled/settings recovery | G restTimerNotificationsAvailable 4716–4732; RestTimerCard 4585–4715 | S, blocked-channel test source | Keep truthful availability and recovery route. Do not claim live alarm delivery was verified. |
| GY41 | Finish all complete and incomplete review | G WorkoutCompletionActions 3518–3565; finish identity state 671–681; Repo finishWorkout 678 onward | S, T | Keep unfinished work explicitly reviewable before finish and exact boundary. |
| GY42 | Generic phased day/phase/cycle advancement | G RoutineProgramPositionDialog 9418–9481; Repo finishWorkout 678 onward; R generic programming | S, F4 | Keep authored next-position progression separate from recorded execution. F4 proves actual launch phase; complete finish/boundary advancement remains source-reviewed. |
| GY43 | Active legacy cycle review/hold/custom eligibility | Cycle 212–477; G trainingMaxCycleReviewOpen owner | S, T | Keep bounded reviewed decisions and archived compatibility. Pending controls: GYM-05. |
| GY44 | Discard active and restore from History | G discard boundary 677–681; Repo discard/restore 1134–1154 | S, T | Keep reversible discard with terminal timer cleanup and single-active constraint. |
| GY45 | Process recovery/recreation/generation mutation guards | G owned boundaries/coordinators 602–1286; VM mutation state; Repo requireExactSetMutation 2482 onward | S, T | Much stronger than local callback ownership. Full fresh interrupted lifecycle remains U; Routine save is GYM-02. |
| GY46 | History empty, search no-results and archived/discarded filters | G WorkoutHistoryContent 6731–7079 | S, history journey source | Keep different recovery actions for empty data vs no matching results. |
| GY47 | History date/calendar/custom range validation | G WorkoutMonthCalendar 7546–7624; validateGymGraphRange 11401–11420 | S, T | Keep calendar route and validation; native large calendar source tests read, not run. |
| GY48 | Finished session overview, performed/planned/skipped status | G WorkoutHistoryCard 7080–7368; HistoricalWorkoutSetRow 7406–7445 | S, D4 | Keep direct truthful row correction and retained execution context. |
| GY49 | Historical correction with an unrelated active workout | G History callbacks 1440–1495; Set editor 10266–10568; Repo updateSet 2211 onward | S, D4, direct correction test source | Keep completed-session identity; earlier indirect-resume issue is fixed. |
| GY50 | Resume, repeat, save-as-Routine, active-workout return | G History card 7080–7368; Repo resume/duplicate 1126–1229 | S, T | Keep distinct correction vs resumed execution, with single-active guard. |
| GY51 | History destructive deletion impact and review | G WorkoutDeletionReviewSurface 5510–5566; WorkoutPermanentDeleteDialog 5567–5700 | S, T | Keep exact candidate UUID/generation and saving block; no hypothetical new approval step. |
| GY52 | Human text Share and structured CSV Export | G workoutShareText/shareWorkout 11453–11489; BackupRepository exportGymCsv 285–287 | S | CSV includes execution state; text loses it, GYM-07. External chooser behavior unknown. |
| GY53 | Insights no workouts/no matching history/recent performed default | G GymProgressContent 8067–8345 | S, power-input test source | Keep last performed exercise and clear recovery instead of arbitrary first catalog item. |
| GY54 | Insights exercise, archived identity, machine/version scope | G 8090–8145, 8340–8470; requiresMachineScope 8590–8591 | S, T | Keep coherent equipment/comparable-version scoping and retained archived history. |
| GY55 | Metric, range, custom date, aggregation, rep target | G GymProgressContent 8140–8558; validateGymGraphRange 11401–11420 | S, T | Keep invalid custom dates explicit; analytics return ownership is GYM-04. |
| GY56 | Compare exercises and independently readable series | G ExerciseComparisonField 521–601; Progress graph options/series | S | Comparison labels/scopes exist; older comparison-point TalkBack reachability remains U. |
| GY57 | Chart point identity and exact session source | G SharedGymLineChart 9568–9674; selected point owner 8157–8159, dialog 8559–8588 | S, exact point test source | Keep same-date session identity and source action. Return scope is GYM-04. |
| GY58 | Textual chart data and expansion | G GymProgressContent 8460–8558; chartDescription 9675–9696 | S | Primary-series data can be disclosed beyond recent points; native TalkBack/older comparison coverage remains U. |
| GY59 | PR tracking choice, defaults, archived volume/context | G TrackedRecordsSection 7625–7748; manager 7804–8059; PersonalRecord formatters 11257–11305 | S, T | Keep performed source context and compatible units. PR drill-down return inherits GYM-04. |
| GY60 | Exercise Library main search, filters, favorites, reorder | G ExerciseLibraryContent 4885–5111; sortedForLibrary 385–417; exerciseMatchesQuery 11364–11377 | S, D1, library journey source | Keep global collection search owner and optional scoped filters; no redundant main search requested. |
| GY61 | Exercise create/edit basics, advanced policy, category picker | G ExerciseEditorDialog 9697–10198; ExerciseCategoryPickerDialog 10199–10238 | S, T | Keep expandable advanced details, dirty guard, validation and meaning-change choices. |
| GY62 | Exercise independent child save/retry/recreation receipt | R child Library save ownership 300–378; Catalog overlay 115–212; VM saveExercise 1229 onward | S, child failure test source | Keep fixed committed Library item vs unsaved Routine draft distinction. |
| GY63 | Exercise archive/restore/favorite/duplicate history implications | G ExerciseActionsDialog 10720–10840; Repo catalog mutations | S, T | Archive is distinct from permanent delete; preserving history is explicit. |
| GY64 | Exercise permanent deletion impacts | G ExercisePermanentDeleteDialog 5184–5328; review 5329–5386 | S, T | Keep impact review and exact identity, with input blocked during deletion. |
| GY65 | Machines linked search/create/edit/version/archive | G MachineLibraryContent 5112–5183; MachineEditorDialog 5967–6504; Repo machine mutation interface 100–103 | S, T | Keep exercise links, versioned meaning changes and snapshots. Broad native machine configurations U. |
| GY66 | Machine delete impacts and source version guard | G MachineDeletionReviewSurface 5445–5509; MachinePermanentDeleteDialog 5828–5966 | S, T | Keep explicit retained-history/reusable-reference consequences and saving block. |
| GY67 | Category optionality/archive/order/global defaults effect | G ExerciseCategoryContent 6558–6729 | S, T | Keep explaining order's First linked category impact and active/archived distinction. Dirty close: GYM-08. |
| GY68 | Tools 1RM formula/input/rounding and plate inventory | G GymToolsContent 8598–8853 | S | Coherent separate tool tabs and local computation. Preset target-unit bypass: GYM-06. |
| GY69 | Routine blank/static basics/day templates/title and validation | R RoutineBuilderScreen 152–766; OutlinePane 1665–2082; validation 3779–3862 | S, T | Keep outline/day/placement hierarchy and local drafts. Pending whole-Routine save: GYM-02. |
| GY70 | Routine saved-in-place edit and independent child persistence | R 300–428; library item draft ownership | S, T | Existing Routine remains on its selected day after save; independently saved Library items survive Routine discard. |
| GY71 | Current Custom phased creation/copied independent prescriptions | R startCustomPhasedRoutine 4948–4977; addProgramPhase 4978–5006; structure page 825–1628 | S, phased state/UI test source | Keep generic two-copy authoring and independently editable phases. Phase tools diverge, GYM-01. |
| GY72 | Phase labels/reorder/remove with preserved indexing | R updateProgramPhaseMetadata 4928–4947; remove/moveProgramPhase 5007–5074 | S, T | Source has explicit phase transform owners; destructive native phase handling not rerun. |
| GY73 | Routine exercise add/search/category/quick Machine/advanced Machine | R ExercisePickerPage 3272–3409; EquipmentPickerPane 3438–3477; QuickMachineDialog 3478–3552 | S, T | Keep contextual picker and exercise-seeded equipment. Secondary dirty close: GYM-08. |
| GY74 | Routine move/copy/duplicate day, placement and groups | R 3973–4140, group transforms 5240–5311 | S, T | Keep explicit scope, independent copies, block/group normalization and local undo. |
| GY75 | Add from performed workout into selected day | R WorkoutPickerPage 3410–3437; importWorkoutIntoDay 4021–4078 | S, T | GYM-09 unexplained oldest-source exclusion. |
| GY76 | Rep prescription author/apply/reorder/delete | R SchemeRow/Dialog 2920–3075; applyRepPrescriptionScheme 4141–4155 | S, T | Shared scheme edits do not retroactively alter saved Routines. Phased apply: GYM-01; changed dialog close: GYM-08. |
| GY77 | Warm-up generation freeweight/mass/ordinal/reverse ordinal | R generateWarmupSets 5189–5237 | S, RoutineWarmupTest source | Keep equipment snapping for static scope; phase-local behavior is GYM-01. |
| GY78 | TM derivation/manual values/basis/calibration | R pending derivations 205–208; Structure/Placement editors; withProgramMassUnit 3945–3972 | S, relevant TM UI/state test source | Keep explicit confirm/apply derivation and unit-aware generic TM. Native calibration error/recreation U. |
| GY79 | Percentage prescriptions, primary TM linkage, boundaries | R RoutinePlacementEditor 2213–2920; updateProgramPlacement 5094–5181 | S, T | Keep generic authored %/TM/cycle decisions. No demand for regenerated branded programming. |
| GY80 | Supplemental/main/assistance legacy metadata and active cycle review | R legacy program guards 2213–2920; Cycle 1–499 | S, T | Compatibility owners remain; new branded supplement/generator controls intentionally retired. |
| GY81 | 5/3/1 retirement, saved conversion and active exception | Retirement 16–83; R buildInitialRoutineState 3553–3656; legacy guards | S, current memory | Keep in-place Custom conversion preserving identity/values/history, clearing generator/Joker metadata, pausing automatic increases. Active legacy source is excluded until finish. |
| GY82 | Routine archive/restore/duplicate/delete and source history | G RoutineContent 8884–9417; RoutinePermanentDeleteDialog 5701–5827 | S, T | Keep explicit reusable routine consequences and impact review; no loss inferred from archive. |
| GY83 | Normal and 200% full-shell appearance | G WorkoutContent; Host; shared editor components | F1, D1–D3, S | Current hierarchy/shared shell justified. Dated evidence is not fresh device verification. |
| GY84 | Short/wide/IME pane and composer reachability | G WorkoutContent pinned lane; Dialog 458–595; R Outline/Placement panes | S, standalone large-workout UI test source | U. Standalone 320×640 test omits full shell and IME; cannot establish all-window reachability. |
| GY85 | Keyboard, Back, recreation for changed forms | G/R saveable fields and dirty owners; Dialog pane/IME ownership | S, T | Primary raw/dirty contracts improved; GYM-02/04/05/08 identify remaining seams. Full runtime matrix U. |
| GY86 | TalkBack actions, pane names, error announcements, reorder | G/R labelled actions/tested semantics; Dialog 458–595 | S, T | Many explicit labels and accessible reorder actions exist. Full focus order, countdown announcements and large-series access U. |

## Justified keeps and intentional constraints

The four Gym jobs are meaningful: Workout executes, Library authors reusable resources, History owns completed/discarded execution, and Insights investigates performed data. Library child collections retain the shared context and explicit return. The global main-collection search owner should remain singular; contextual pickers can own local search.

The active workout has a sensible hierarchy in the fresh normal image: session identity, next required set/rest, current input, then supporting arrangement/history. The overview is an appropriate complete identity and jump surface. Lower controls being outside a screenshot is not evidence of clipping when the body scrolls. Large-text/short-window concerns need the full-shell measurement, not a demand to remove content arbitrarily.

Keep the recent previous-value unit/eligibility fix, raw Set Details validation/draft protection, quick-to-details handoff, direct completed-set correction with unrelated active work, and independently saved Library child receipts. Current source and existing regression test bodies support those designs; this reviewer did not re-execute their checks. Earlier screenshot grammar or older fixed bugs must not be reopened as current findings.

New 5/3/1 generation, branded setup, Joker authoring, and supplemental program pickers were intentionally retired by FB-20260920-002/DEC-20260920-001. Current authoring is ordinary Custom phased Routines with authored set values, generic percentage/TM linkage and cycle boundaries. Legacy source files and cycle review are compatibility behavior, not a promise of fresh program creation. Restoring branded generator controls would contradict the accepted product direction. GYM-01 specifically concerns the current Custom phased path.

Keep machine ordinal/mass separation, equipment snapshots, exact-session mutation boundaries, undo isolation, explicit incomplete finish review, reversible discard, and destructive impact review. These are consequential semantics rather than visual preferences; simplification should not flatten them into ambiguous values or generic delete actions.

## Remaining unknowns and concrete follow-ups

No native delayed-save, process-death, notification-delivery, landscape, short-window, or TalkBack journey was performed by this reviewer. The following are verification gaps, not additional defects:

- Measure active work at native 200% in a short full-shell window with IME open and running rest. Confirm composer and Log remain reachable, pane scroll ownership is usable, and next/rest does not leave an impractically small viewport. GymPowerInputUiTest's compact test inspects a standalone 320×640 body with zero shell padding; it cannot settle that question.
- Exercise Insights → exact source → return with custom scope, and recreation at each boundary. The source ownership gap is GYM-04; a fresh native artifact would establish the exact user-visible reset.
- Delay Routine/Set/Workout/cycle saves, edit/back during submission, rotate/recreate, then interrupt the process. Verify receipt ownership, retained raw draft and localized retry; source callback behavior must not be mistaken for a runtime pass.
- Use TalkBack for every tracking type, invalid numeric field, optional accept/skip, group reorder/jump, rest availability, and multi-series chart. Primary-series expanded data exists, but complete access to older comparison-series points and focus restoration after source drill-down is not established.
- Verify notification denial, revoked permission, disabled channel, sound/vibration-off semantics, background timer delivery and terminal cancellation using real permission/channel transitions. Existing source test names are not fresh OS evidence.
- Run current generic phased authoring end to end with per-phase TM values, common rows, mixed equipment, percentage prescriptions and cycle boundary review. Test active legacy calibration/review only with a legitimate retained active fixture; do not seed a new retired product path and call it current authoring.
- Confirm external Share chooser and exported CSV values after incomplete finish, archived equipment, and historical correction. GYM-07's missing completion status is deterministic before the chooser.

Selected inspected test sources: GymRepositoryTest, GymLibraryJourneyE2ETest, GymPowerInputUiTest, GymWorkspaceConsistencyUiTest, RoutineBuilderUiTest, RoutineBuilderStateTest, RoutineWarmupTest, GymPhasedRoutineJourneyE2ETest, WorkoutGroupedSetNavigationE2ETest, and WorkoutRestJourneyE2ETest. Relevant coverage includes previous-unit conversion, raw invalid draft restoration, direct historical correction, grouped identity/jump, rest override, exact chart-point session identity, child Library save failure receipts, phased independence, and static prescription/warm-up helpers. This reviewer executed none. The parent executed the single fresh phased-launch test recorded in F4; this must not be generalized to the rest of these test bodies.
