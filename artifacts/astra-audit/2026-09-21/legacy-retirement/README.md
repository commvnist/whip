# Legacy-workout retirement journey — 2026-09-21

Scope: an already-active 5/3/1 workout after new branded authoring was removed. Production `WhipApplication`, Room, repositories and app-shell Gym UI on disposable emulators; synthetic exercise/routine values. No owner phone or release.

`GymLegacyRetirementJourneyE2ETest` creates a legacy Performance-Informed routine with a Training Max Test Set, starts and completes its active workout, opens the real Gym screen and review, recreates `MainActivity` with the review open, applies its decision, and waits for the normal runtime to retire only the future saved template. It asserts that the active routine remains legacy through the review, the finished session and performed Set keep their original 5/3/1 provenance, and the next session starts from the converted generic Routine.

- API 34 normal text: `build/instrumentation-results-BvMIai`, 1/1 pass, zero skip/failure.
- API 34 actual font scale 2.0: `build/instrumentation-results-TMvGPI`, 1/1 pass; emulator font scale was restored to 1.0 afterward.
- API 26 and API 37: one exact class each via guarded direct `connectedDebugAndroidTest`, 1/1 pass per platform. Direct Gradle's connected-result XML is overwritten by subsequent direct runs.
- The earlier 87-state Gym-family capture includes `gym.legacy-program.review` and `.large` original PNG/XML pairs. Both originals were personally inspected here: normal text keeps decisions and both footer actions visible; large text uses a scrollable body with an always-visible decision footer. Existing `FiveThreeOneCycleReviewUiTest` covers the focused large-text component semantics.

This is a real Activity-recreation interruption and production-data/UI path, not a true OS process-kill/relaunch test. The startup source gate and `RoutineRepositoryTest#legacyRoutineConversionWaitsForActiveWorkoutAndKeepsCompletedSnapshot` support the latter, but cold-process acceptance remains separate audit work. No new 5/3/1 authoring was reintroduced.
