# Routine, Gym and Task overhaul acceptance — 2026-09-30

Device: disposable AVD `whip_api34_parallel`, `emulator-5556`, Android API34. Boot complete and QEMU verified. Every interaction/install targets this serial explicitly. No phone installation or real user-data fixture.

## Immutable first package and actual before/after evidence

Before package: debug0.3.90/code96, SHA256 `6b2d4f18dce889025a9dfcfb4f925424eab91837b1ae84ee9ef4ddc62438f44e`. This is the preserved pre-resume APK, not a build of the resumed dirty checkout. Native taps visited Home → Gym → Library → Routines → Create Routine, then Tasks → new Task → pending New Subtask. Before screenshots/hierarchies: `build/overhaul-evidence/routines/before/`.

After first package:
- app SHA256 `06133a699159291e0fa687192ae4bf99f1839042ab1c59292abcbd30a98354d2`
- test SHA256 `97ac356e72a3138c0d955e23feca5fce9ae4702e1b5cc6f404e5adb4fd1bb707`
- immutable files: `build/qa-overhaul/2026-09-30/batch-one/`

Real taps repeated Home/Gym/Library/Routine editor navigation. Pending New Subtask “Pack Shoes” remains visible after Cancel → Keep Editing; matching PNG and accessibility XML are in `build/overhaul-evidence/routines/after/pending-subtask{,-discard,-retained}.*`. A priority chip changed during manual scrolling, so this manual sequence alone cannot establish that pending text was the sole dirty field. The corrected focused Task fixture verifies that stronger condition.

## First native batch: 14 run, 12 passed, 2 fixture failures

Complete log: `build/overhaul-evidence/routines/native-batch-one.log`. AndroidJUnitRunner reports 14 tests and 2 failures. adb process exit0 is not interpreted as a passing test result.

| Test class | Methods and outcome |
|---|---|
| RoutineBuilderUiTest | PASS `phasePrescriptionToolsPreserveCommonAndSiblingRowsThroughSaveAndRestore`, `delayedRoutineSaveBlocksBackRetainsFailureAcrossRestoreAndAllowsOneRetry`, `quickMachineKeepsFailedDraftAndScrollableNumericFieldsAtLargeText`, `nestedExerciseFailureRetainsDraftAndOwnedResultAcrossRecreation` |
| ui.SafetyChoiceUiTest | PASS `workoutSaveFailureKeepsParentAndDraftOpenForRetry`, `workoutSaveSubmissionBlocksDuplicateActionsAndDismissal` |
| ui.GymDeletionViewModelIntegrationTest | PASS `workoutDetailsUseOwnedReceiptsForAtomicStartAndDurableUpdate` against the actual Room repository |
| GymPowerInputUiTest | PASS `pendingRestPresetSurvivesDiscardReviewFailureAndRestoreUntilConfirmedReceipt`, `readyRestTimerMakesWorkoutScopedDurationDiscoverableAndUsesIt` |
| ui.EditorDependencyUxTest | FAIL `pendingSubtaskIsDirtySurvivesCancelAndBelongsToSaveAndNewWithoutDuplicates`: power mode already expanded optional fields; the test collapsed them. Removed the erroneous disclosure click. Rerun pending. |
| RoutineAuthoringJourneyE2ETest | PASS `twoDayRoutinePreservesContextAndStartsTheChosenDay`; FAIL `authoredPrescriptionSurvivesRecoveryExecutionAndLaterRoutineEdits` at final History toggle. Retained expanded History was unconditionally collapsed. Test now checks Expanded before toggling. The earlier assertions reached actual40kg×7 performance and later45kg routine persistence; full test acceptance requires rerun. |
| GymWorkspaceConsistencyUiTest | PASS normal and200% methods `libraryChildrenAndEmptyInsightsKeepContextAndReturnActions{AtLargeText}`. Real workspace bounds/insets/context assertions across Library children and empty Insights. |

26 assets from passing fault/recreation fixtures were pulled to `build/overhaul-evidence/routines/native-captures/`: 13 PNG+XML pairs for phase tools, Routine delayed/failure/success, Workout delayed/failure, and rest preset before/failure/success/later-draft restoration. Discard and later-rest-draft PNGs were visually inspected.

## Second immutable package and corrected acceptance

Second package app SHA256 `f7a19e1318b9e2d2f63c1386f82dfddb7892e69064e430555a137e03499a3498`, test SHA256 `926fe0936d4413fbb161d911e685dcc2f4840d9f92a4ccc73b69f2b38e4ac18e`. Complete log `build/overhaul-evidence/routines/native-batch-two.log`; structured result `native-batch-two-manifest.json`:17 run,16 passed,1 failure. 70 fresh assets were pulled to `batch-two-native-captures/` using a recorded device timestamp cutoff; the first62 assets remain separately preserved.

Both earlier failed fixtures pass in this package: the sole-pending-Subtask dirty/save-and-new/duplicate case and the full Routine prescription save/reopen/performance/later-template-edit/immutable-history journey. Existing rep scheme create/edit/delete tests also pass. The single new scheme failure was an assertion checking Text while the shared error notice exposes a merged accessibility content description; all three checks now use the actual description.

Final package app SHA256 `cb5c6169bff1f01c2c17cd379126a3c4c243ffc2f11a81b5820e49baf1f69cc3`, test SHA256 `970342691a47a4a5ca71ae91f390d6b4d6a5f86ebcf7cd6d82e029e03cd22b4d`. Its focused replay passed `RoutineBuilderUiTest#repSchemeMutationsRetainDraftAndConfirmationAcrossDelayedFailureAndRestore`, including all six create/reorder/delete submissions, error descriptions, raw draft/confirmation restoration and confirmed retry. The actual-shell `GymLibraryJourneyE2ETest#emptyWorkoutCreateExerciseAddsOneReusablePlacementAndSetAcrossRecreation` also passed. Final complete receipt:2 run,2 passed,0 failed in139.244s; `build/overhaul-evidence/routines/native-final-focused.log` and `native-final-focused-manifest.json`. All14 fresh PNG/XML assets are in `final-native-captures/`; atomic-create and failed-restored-scheme PNGs were visually inspected. The device was relinquished to integration for the full fresh native inventory. The16 accepted second-package methods and2 final focused methods establish18 unique accepted lane methods across these packages; this is not represented as an18-test run of a single final APK.

## Historical finding trace and acceptance limits

The original audit is [DESIGN_COHERENCE_UX_AUDIT_2026-09-29.md](DESIGN_COHERENCE_UX_AUDIT_2026-09-29.md). These statuses distinguish implementation from fresh native acceptance.

| Finding | Current implementation and evidence | Remaining acceptance |
|---|---|---|
| GYM-01 | Shared phase replacement preserves common/sibling rows; selected-phase warmups use that phase's working load. Phase isolation native test passes; latest RoutineBuilderStateTest45 and RoutineWarmupTest4 pass. | Full Room reopen/instantiation of the specifically transformed multiphase fixture is not established by the isolated UI callback test. |
| GYM-02 | RoutineBuilderViewModel retains submitted receipt/draft; duplicate/late callbacks and cancellation settle; parent Save/Back/fields freeze. Delayed save/failure/restoration native case and complete durable save/reopen/performance/later-edit/History journey pass in second package. | Final broad campaign remains integration-owned. |
| GYM-03 | Preserved draft captures current workout structure boundary+UUIDs in GymScreens. GymCatalogMutationUi invokes createExerciseAndAdd; repository owns atomic createExerciseAndAddToWorkout. Final actual-shell native journey passes exactly-one Exercise/placement/initial-set ownership and retained id/uuid lists across recreation, Library and original-session return. No duplicate product repair added. | Repository stale/replayed create-and-author method belongs to final full campaign. |
| GYM-04 | Gym destination SaveableStateHolder retains browse/analysis state; precise Insights origin and return Back are retained. External search waits for loading and settles missing sources via unavailable callback. | The two workspace geometry tests pass, but exact populated Insights→source→return selections/range need native acceptance. |
| GYM-05 | Preserved Set/cycle-review inputBlocking plus new Workout editor existing-session typed owner. Native delayed/recreated failure/retry, duplicate submission, and Room start/update receipts pass. | Set and cycle-review siblings need final focused campaign results. |
| GYM-06 | Preserved preset application converts authored target to preset unit before applying preset bar/plates/inventory, preserving physical meaning instead of relabelling. | Cross-unit preset UI choice/conversion native acceptance; source alone does not certify it. |
| GYM-07 | Share text distinguishes Completed / Planned, not performed / Not completed; Exercise Status Included/Removed/Substituted replaces misleading Outcome Active. Existing archived-identity JVM assertion passes in GymUxRulesTest28/28. | Native share intent text. |
| GYM-08 | Preserved Category/preset/quick-machine discard guards. Rep scheme create/edit/reorder/delete now use existing typed Settings mutation with durable confirmation; failure retains authored draft/confirmation. Reorder transforms latest stored schemes and keeps newly added records. New six-attempt delayed/failure/restoration native case passes on final package. | Other secondary authoring sibling guards require campaign receipt. |
| GYM-09 | Routine Add from Workout offers another50 via Show older workouts, preserving selected day import. | Native older-than50 selection and persisted day import. |
| TASK-01 | Pending raw Subtask participates in dirty and parent Save/Save & New draft, with duplicate prevention. Corrected native sole-dirty/Cancel/Keep Editing/Save & New/explicit Add duplicate fixture passes in batch two. | Final broad campaign remains integration-owned. |
| TASK-04 | Completed inspector stable key remains when opening definition editor; saved inspector section owns return context. | Actual completed-inspector edit/cancel/save layering and selected-section return require native verification. |
| TASK-05 | Integration repaired the exact consumer to read frozen ScheduledSubtask title/notes, rather than the current step definition. Source independently verified in TaskComponents861/864. | Integration owns the differing-definition-versus-snapshot native fixture and its acceptance receipt. |

Latest readiness JVM acceptance reported by integration:460/460 passed. The focused XML independently confirms RoutineBuilderStateTest45/45, RoutineWarmupTest4/4 and GymUxRulesTest28/28 at2026-09-30T04:41Z (77 focused tests). Earlier failures in a shared architecture assertion and Track CSV ceiling assertion were repaired by integration; the initial failed runs remain part of the logs.

No lane commit or push performed. Integration owns final lint/type/build/JVM/native aggregation, evidence disposition, cohesive commits and push. No full overhaul completion is claimed.
