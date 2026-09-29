# Fresh Tracks and Gym audit — 2026-09-29

Baseline: `ba1f7cc5`. Independent current-source review by the assigned Astra/high agent. Prior audit reports were not read. The memory index was read for contracts only; its completion claims were not treated as evidence. The parent owns shared chrome, fresh emulator captures, verification scheduling, memory integration and commits.

## Evidence and limits

Reviewed current UI, view-model, repository and domain owners listed below and the relevant source tests. The independent review stage ran no Gradle or emulator commands; source tests at that stage established intended contracts rather than passing evidence. Three dated original images were inspected only as visual context: `artifacts/full-suite/2026-09-29/visual/tracks.history.search.large.png`, `artifacts/deep-product-review/2026-09-29/after/deep.tracks.dated-trend.png`, and `artifacts/deep-product-review/2026-09-29/after/deep.gym.session-overview.native200.png`. These are not fresh baseline evidence. The final acceptance below records newly executed emulator checks and original-image inspection; the parent owns the shared readiness build, memory and commits.

## Accepted implementation plan

### TG-1 — Set Details must preserve and validate authored text

Confirmed from `GymScreens.kt:WorkoutSetEditorDialog`: optional RPE, RIR, bodyweight and rest text is parsed into nullable numbers before validation. A nonempty value such as `.` becomes `null`, so domain validation treats it as omitted. Dirty checking compares parsed drafts, so replacing a blank optional value with invalid text can also escape the discard guard. Required values in incomplete/planned sets can suffer the same parse-to-null problem. The editor labels RPE as 0–10 although `validateWorkoutSetDraft` accepts 1–10.

Reproduction: open Set Details for a valid set with blank rest/RPE; enter malformed optional text or integer overflow; Save can remain enabled and drop the authored text. Cancel can dismiss without reviewing those changes.

Remedy: validate nonblank raw numeric inputs before building the save eligibility, identify the affected field, retain raw text through recreation/failure, and compare raw editor fields for dirty state. Keep the existing domain validator and set editor. Correct the effort ranges. Use existing inline field/error components and the same persistence coordinator.

Acceptance: malformed optional and incomplete-set input never saves as blank; correction unblocks Save; valid zero RIR/rest remain valid; blank remains optional; raw malformed text survives recreation; Cancel requests discard; save failure retains text; short/200% layout has reachable feedback and actions.

### TG-2 — Correct a historical Set without resuming the workout

Confirmed from `WorkoutHistoryCard`/`HistoricalWorkoutSetRow`: completed evidence exposes no Set correction action. The offered route is Resume Original Workout, whose DAO intentionally clears the finish time and makes the original active. That is a valid resume operation but an expensive and misleading prerequisite for fixing an old value, including when another workout is active. `RoomGymRepository.updateSet` already supports exact historical Set mutation with `requireActive = false`, preserves prescription snapshots, and the view-model already rebuilds personal records.

Remedy: route a historical Set's Edit action to the existing Set Details editor. Resolve its owning placement/session from all history, capture that exact session's boundary, and keep the parent History state mounted. Only nonremoved Sets of unarchived finished workouts expose correction. Keep active-only mutation helpers active-only; introduce an explicit historical-capable boundary option/helper only for editing. Preserve original start/end/date, progression decisions and any unrelated active workout.

Acceptance: edit a finished workout while another session is active; persistence changes only the selected Set; original dates/duration/state and program decisions remain; personal records reconcile; archived/deleted Sets remain read-only; stale source edits reject overwrite; failure/recreation retains draft and expanded parent context.

### TG-3 — Track trend evidence should open its exact Entry

Confirmed from `TrackReviewExperience.kt:TrackRecordedTrend`: Recorded Values renders anonymous date/value text, even though repeated-date observations are explicitly supported. It neither names the Entry nor opens it. Seeing an anomaly therefore requires leaving Insights and reconstructing its identity in Entries; two observations on one date are ambiguous.

Remedy: use existing record/navigation rows for identity, date and original entered-unit value, opening the existing Entry details dialog with its Edit action. Preserve scope, field disclosure, evidence page and parent scroll through child editing. Add compact min/max value context to the existing graph if it remains legible in the fresh capture; no extra toolbar/search/chrome.

Acceptance: two same-date Entries have distinct accessible identities; each opens the correct saved Entry; editing returns to the same evidence scope/page; archived source stays read-only; missing/deleted target closes or provides truthful recovery; restored child selection resolves only after current data is ready.

Owned implementation: `GymScreens.kt`, `GymViewModel.kt`, `TrackReviewExperience.kt`; parent-approved narrow `TrackScreens.kt` Insights/detail callback seam; feature-only tests. No shared/root geometry change, schema migration, dependency or program regeneration is needed.

### TG-4 — Put logging ahead of workout administration

Fresh parent capture `artifacts/fresh-app-overhaul/2026-09-29/before/supporting/gym.workout.populated.png` confirms that an ordinary one-exercise workout places its actual input below workout title/date, several zero metrics, Next/rest, a large Add/Arrange toolbar and a scope explanation. `WorkoutContent` renders that exact hierarchy. The adjacent fresh Tracks collection capture was also inspected; its stable collection hierarchy stays.

Remedy accepted by parent: compact the workout identity and summary; show completed/total Sets and elapsed time without redundant zero reps/volume, retain meaningful performed totals behind a disclosure; place Edit/Add/Arrange in a labelled Workout options menu; keep empty-workout Add prominent and preserve sticky Next and immediately available rest actions. Reduce rest padding without shrinking touch targets. Update block-scroll indices and test action access/next-set navigation, normal/200% layouts and running-rest state. Shared workspace geometry stays parent-owned.

## Mainline coverage and reasoned dispositions

Fresh integration refinement: the normal and 200% collection originals expose the exact summary “1 tracks · Reusable logs.” That intermediate evidence is preserved at `artifacts/fresh-app-overhaul/2026-09-29/intermediate/track-count-grammar-2026-09-29/{normal,native200}/tracks.all.populated.*`. Both collection-header owners now reuse the existing `quantityLabel(count, "track")`, rendering “1 track” and pluralizing other counts. This is a bounded copy correction under TG scope; no extra formatter or broad test is added. The final affected readiness build compiles the change, and refreshed normal/actual-200% originals at `after/{supporting-final,large-supporting-final}/tracks.all.populated.*` show the corrected singular count.

| Workflow | Current-source evidence | Disposition and reason |
|---|---|---|
| Track collection entry and global search | `TrackAreaContent`, `AllTracksPage` | Keep one global collection search and stable parent navigation; correct the freshly observed singular count through the existing quantity formatter. |
| Track empty/filtered/load failure | `AllTracksPage`, `TrackActivityPage`, domain load states | Keep distinct create/clear/retry actions; they identify the next achievable action. |
| Track creation and starters | `TrackEditor`, `TrackStarter.draft` | Keep blank/reading/spending/reflection templates; generated fields remain editable and replacement is reviewed. |
| Track schema preview | `TrackEntryPreviewDialog` | Keep disposable preview values; they cannot enter repository/editor checkpoint. |
| Identity and typed fields | `TrackModels`, `TrackFieldEditor`, `TrackDomainTest` | Keep explicit composite identity, required flags, and typed operators; no generic schema simplification. |
| Numeric units and scales | `TrackEntryField`, `TrackInsightNumberFormat`, scale domain helpers | Keep canonical numbers with original entered units and fractional scale steps. |
| Field reorder and edit | `TrackEditor`, field editor state holder | Keep scoped field editor and explicit order; child draft has its own saved-state key. |
| Removing fields/options with evidence | `reviewDefinitionUpdate`, `TrackDefinitionIntegrityTest` | Keep exact impact review and replacement identity checks; concurrent values invalidate destructive review. |
| Definition conflict/recreation | `TrackEditorSessionViewModel`, `TrackEditorViewModels`, definition conflict UI | Keep generation-keyed recovery and Save Draft as New Track; do not overwrite newer source. |
| Entry create/date/duplicate warning | `TrackEntryEditor`, `prepareEntryCreate`, `addEntry` | Keep entry date separate from creation time and stable create receipt identity. |
| Entry required/numeric validation | raw numeric checkpoint and `validateWorkout`-independent Track form validation | Keep current raw number validation rather than copying Gym's parsed-only behavior. |
| Entry edit/delete/undo | `updateEntry`, `deleteEntry`, `restoreEntry`, `TrackEntryDeleteRoute` | Keep exact identity boundaries, retained parent and conflict-safe undo. |
| Archived Track/Entry | archived Entries status and `TrackEntryDetailsDialog` | Keep restore affordance and read-only history; correction requires intentional restore. |
| Entry history paging | `TrackEntriesPage`, `entryPage`, `TrackHistoryJourneyE2ETest` | Keep bounded database page loading, preserved requested count, and list-state guard while reload is pending. |
| Entry text search | `searchEntryIds`, async search state and retry | Keep scoped child search; distinguish it from global collection search. |
| Entry filters/sort | saved `TrackReviewScope`, `TrackFilterDialog`, typed sorting | Keep draft Apply for filters and explicit sort state; numeric/date operations remain typed. |
| Activity across Tracks | `TrackActivityPage` | Keep area-limited active Track scope, Track/date/text filter dialog, and direct source links. |
| Workspace Insights | `TrackWorkspaceInsightsPage` | Keep overview frequency and per-Track numeric units; avoid combining incompatible quantities. |
| Track Insights scope | `TrackInsightsPage`, `TrackReviewRangeControl` | Keep shared date/conditions scope with View Matching Entries, explicit future-date inclusion for All Dates. |
| Track numeric/categorical evidence | field summaries and `TrackRecordedTrend` | Implement TG-3; keep missing-value omission, original units, same-date order, and paged evidence. |
| CSV picker/mapping | `TrackCsvImportDialog`, `TrackCsvEntryPreview` | Keep frozen form, explicit fallback date/unit, typed per-field mapping and concrete row preview. |
| CSV invalid rows and unavailable target | target lookup states and `commitIdentityOrNull` | Keep blocked all-or-nothing import and distinct failed/missing/archived target recovery. |
| CSV duplicate/recovery | `importEntries`, durable receipts and deterministic entry UUIDs | Keep exact batch identity and transaction; do not replace with blind retries. |
| CSV export | export preflight, `buildTrackCsv`, export launcher | Keep bounded export and truthful failure/cancel state, original entered unit values. |
| Gym primary navigation | `GymDestinationHost` | Keep Workout/History/Progress/Library jobs; parent owns header geometry. |
| Exercise library discovery | `ExerciseLibraryContent`, global search route | Keep scoped filter/sort disclosure, count/filter context and record edit actions; no second collection search. |
| Exercise creation/edit | `ExerciseEditorDialog` | Keep tracking-specific basic fields, advanced disclosure, raw defaults validation and explicit unit conversion choice. |
| Categories | `ExerciseCategoryContent`, category picker | Keep library category assignment separate from routine role and assistance meaning. |
| Machine creation/versioning | `MachineEditorDialog`, `RoomGymRepository.updateMachine` | Keep resistance profile/version identity and history snapshots; arbitrary machine levels are not mass. |
| Archive/permanent library deletion | exercise/machine/routine impact review surfaces | Keep impact-specific review and route to affected routines; protect authored workout history. |
| First workout/routine quick start | `WorkoutStartContent`, `routineStartDay` | Keep one clear start action, equipment resolution and next programmed day. |
| Active workout execution | `WorkoutContent`, `selectNextWorkoutSet`, groups | Keep next-set lane and ordered grouped execution; arbitrary manual selection returns to order. |
| Quick logging/history suggestion | `QuickSetEntry`, `convertedQuickSetSuggestion` | Keep entered-value conversion plus equipment/policy compatibility checks and one Complete Set action. |
| Set Details | `WorkoutSetEditorDialog` | Implement TG-1; preserve existing snapshot policy and exact coordinator rather than another form. |
| Add/remove/substitute/group/reorder | structure boundaries, arrangement UI | Keep explicit workout-only scope, retained completed evidence, previewed arrangement and undo. |
| Rest timer/presets/zero rest | `RestTimerCard`, `WorkoutRestDuration`, rest journey source | Keep prescribed/override distinction, explicit zero rest, durable deadline and notification explanation. |
| Finish/discard/recreation | finish boundary, session coordinator and repository finish receipt | Keep exact review and idempotent advancement; incomplete required work is reviewed separately from optional Jokers. |
| Workout History list/calendar/filter | `WorkoutHistoryContent` | Keep calendar month/date and exact deep-link scope with parent expansion state. |
| Historical prescription vs performance | `HistoricalWorkoutSetRow`, snapshots | Keep separate performed/target/status/rest/notes and retained removal evidence. Implement TG-2 correction action. |
| Repeat/reuse/save routine/share | history actions and repository copies | Keep new session identity, original evidence and explicit source/target boundaries. |
| Resume original | `resumeWorkout`, `GymDao.resumeSession` | Keep as intentional lifecycle action; TG-2 removes need to misuse it for correction. |
| Progress initial exercise/source | `GymProgressContent`, most-recent performed selection | Keep performed default and exact source workout opening. |
| Progress machine scope/units | equipment scope keys and metric unit conversion | Keep machine/version separation and original level direction; raw machine levels must not form mass comparisons. |
| Progress date/rep controls | `validateGymGraphRange`, `repTargetValid` | Keep invalid-input empty evidence and clear errors, explicit custom window. |
| Weekly/category/record summaries | `buildWeeklyGymSummary`, scoped hard-set allocation | Keep allocation policy and snapshot effective loads; do not total unrelated raw settings. |
| Static routine authoring | `RoutineBuilderScreen`, placement/set editor | Keep day outline, selected set disclosure, explicit per-placement equipment and independent draft checkpoint. |
| Phased routine authoring | `RoutineProgramStructurePage`, phase state transformations | Keep explicit phase/primary-lift roles and independence of prescriptions across phases. |
| Percentage load resolution | `resolveRoutinePrescribedLoad`, `RoutineProgrammingTest` | Keep stable explicit Training Max, inverse load interpretation, conservative equipment rounding and missing-max failures. |
| Existing 5/3/1 programs | `FiveThreeOneProgramming`, legacy sections of builder | Keep editable existing Main/Supplemental/assistance roles and original snapshots. Do not resurrect retired creation entry points. |
| 5/3/1 leader/anchor/protocol editing | per-phase policies and synchronization tests | Keep explicit linked-edit scope, separate protocol ownership and no implicit global regeneration. |
| Optional Jokers/eligibility | optional selection helpers and progression repository | Keep readiness-gated opt-in, ladder stop rules, skipped/removed evidence and explicit TM decision boundary. |
| Routine save failure/recreation | `RoutineBuilderViewModel`, raw set state validation | Keep raw draft checkpoint, own child library creation receipts and same-generation recovery. |
| Short/large/wide state | adaptive forms and pane-aware dialog source; fresh normal/actual-200% changed-state originals inspected during final acceptance | Preserve shared adaptive owners. Current changed states have fresh originals; wide adaptation remains a source-review disposition in this Track/Gym subset. |

## Exact verification requests to parent

New focused tests should cover raw malformed optional/incomplete Set fields and raw discard state; historical boundary capture belonging to the selected finished session while another is active; a historical edit UI journey with stale/failure/recreation; and repeated-date Track evidence opening/editing the exact source while retaining scope/page. Reuse existing `GymPowerInputUiTest`, `GymRepositoryTest`/history journey and Track insight journey patterns where appropriate. Run one final affected readiness batch and selected device methods under the repository's bounded-command policy; no independent broad or repeated test campaign.

Capture current Track Insights with two different Entry identities on one date and Recorded Values expanded; historical Set correction with another active workout; Set Details with invalid optional text at normal and 200% short keyboard geometry. The existing screenshot paths above must remain labeled as dated context.

## Implementation checkpoint

TG-1–4 are implemented in the assigned files. The historical editor resolves its saved Set UUID against all workout placements, with a separate `captureSetEditBoundary` owner; active-only actions still restrict to the active session. Existing repository writes, record reconciliation and persistence receipts remain unchanged. Track evidence saves the selected Entry UUID and leaves Insights/disclosure/page mounted through the existing Entry editor; a removed source gives an explicit unavailable dialog. Set Details keeps raw fingerprints and malformed raw numeric feedback through saved-state restoration. Quick-entry handoff refuses malformed text instead of silently normalizing it to null.

Workout options contains Edit, Add, Arrange and Show/Hide Workout Totals. The main identity uses compact type, and the normal execution path removes the separate structure toolbar. Empty workouts retain Add as their primary action. The scroll target calculation accounts for the conditional context/arrangement item; running timer actions and touch targets remain available.

Added source tests:

- `GymUxRulesTest.setRawInputRejectsMalformedOptionalNumbersAndIntegerOverflow`
- `GymUxRulesTest.historicalSetEditBoundaryOwnsItsFinishedSessionWhileActiveMutationsStayScoped`
- `GymPowerInputUiTest.setDetailsKeepsMalformedOptionalTextThroughRestoreAndDiscardReview`
- `GymLibraryJourneyE2ETest.historicalSetCorrectionPreservesBothSessionsAndRejectsAStaleEdit`
- `GymLibraryJourneyE2ETest.workoutLoggingPrecedesAdministrationAndOptionsRetainActionsAndTotals`
- `TrackInsightsJourneyE2ETest.repeatedDateEvidenceOpensAndCorrectsItsExactEntryWithoutLosingInsights`

Adapted `FirstClassWorkflowE2ETest` to the new Workout options route and retained the clipped-input interaction check in `ExecutionItemBuilderJourneyE2ETest` using the load field instead of the removed toolbar. `git diff --check` passes. Parent owns compile/JVM/readiness verification.

First focused native pass on explicitly guarded emulator-5558 used the newly built debug and test APKs, with each selector bounded by `timeout --kill-after=3s 55s`. Malformed optional input passed at actual 200% (22.217s), and compact workout logging/options/totals passed (17.092s). The historical correction reached successful save/recreation but its pre-launch equality baseline included pending rest-timer cleanup; its test now waits for existing cleanup before capturing the baseline. The repeated-date Track check opened the exact requested entry but its title matcher also found the Name fact; its test now uses the existing inspector-title tag. At that checkpoint, those two checks required rebuilt-test reruns and were not recorded as passed.

Fresh native 200% Set Details evidence exposed shared primary-editor status-bar overlap while the keyboard was open. The original PNG/XML are preserved at `artifacts/fresh-app-overhaul/2026-09-29/before/primary-editor/fresh.gym.set-invalid-optional.*`. Parent owns the shared inset fix; the malformed-input test now additionally measures native title/status-bar and complete Save/IME bounds. Logging capture also waits for menu/totals dismissal before recording the after state. Logs are under `build/fresh-app-overhaul-20260929/tracks-gym-*.log`; pulled captures are under `artifacts/fresh-app-overhaul/2026-09-29/after/tracks-gym`.

## Final acceptance

Status: Verified within the requested focused scope. The parent-owned `scripts/check --ready` passes 450 JVM methods in 49 suites with zero failures, errors or skips; its JVM stage completes in 12 seconds. Android test compilation, debug lint and debug assembly pass in 2m54s; the final incremental test-APK assembly passes in 2 seconds. Receipts are preserved under `artifacts/fresh-app-overhaul/2026-09-29/receipts`.

Every native selector below used the explicit emulator guard and `timeout --kill-after=3s 55s adb -s emulator-5558 shell am instrument -w -r -e class CLASS#METHOD commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner`. Each completes with `OK (1 test)` and a success status; no timeout is counted as passing.

| Exact selector | Passing result | Receipt under `receipts/raw/` |
|---|---|---|
| `GymPowerInputUiTest#setDetailsKeepsMalformedOptionalTextThroughRestoreAndDiscardReview` | 12.161s, actual 200%; overflow/dot drafts survive restore and discard review, the invalid-number explanation scrolls into view with the IME open, native title stays below the status bar, complete Save stays above IME, correction saves zero rest and fractional RPE | `tracks-gym-malformed200-complete.log` |
| `GymLibraryJourneyE2ETest#historicalSetCorrectionPreservesBothSessionsAndRejectsAStaleEdit` | 30.917s; finished and unrelated active sessions remain equal, child draft survives recreation, parent History remains, stale overwrite rejects and retains the raw draft | `tracks-gym-historical-verified.log` |
| `TrackInsightsJourneyE2ETest#repeatedDateEvidenceOpensAndCorrectsItsExactEntryWithoutLosingInsights` | 28.088s; repeated-date observations open the selected identity, correction persists, and Insights scope/disclosure survives return | `tracks-gym-repeated-date-rerun.log` |
| `GymLibraryJourneyE2ETest#workoutLoggingPrecedesAdministrationAndOptionsRetainActionsAndTotals` | 19.354s; load/reps lead the normal execution path, all options and complete totals remain reachable, logging still completes the set | `tracks-gym-compact-logging-rerun.log` |
| `DialogThemeContrastTest#lightDialogsOnDarkAndroidFollowThemeAcrossEveryWindowOwner` | 32.735s; live/recreated dialog windows and actual status-band pixels follow the selected light app theme on dark Android | `tracks-gym-shared-dialog-theme-verified.log` |
| `VisualCatalogPagesTest#captureSupportingWorkspaceOverview`, normal Android text | 13.718s; shared Track/Gym geometry and current Track, Gym and Settings originals | `tracks-gym-supporting-normal-complete.log` |
| `VisualCatalogPagesTest#captureSupportingWorkspaceOverview`, actual Android `font_scale=2.0` | 13.124s; shared geometry and current 200% originals; Android scale restored to 1.0 afterward | `tracks-gym-supporting-native200-complete.log` |

Fresh originals are inspected at `after/tracks-gym`, `after/dialog-theme`, `after/supporting-final` and `after/large-supporting-final`. The final enlarged invalid Set has readable title, complete disabled Save, raw dot input and the full “Enter a valid number” explanation above the numeric keyboard. Historical correction shows 40 kg × 7 reps beside its Edit action and the unrelated active-workout route. The corrected Track inspector retains identity/date and the entered 2.0 L value. Both collection profiles show “1 track · Reusable logs”; normal Gym load/reps are visible, and 200% Next/rest/action labels wrap legibly with the body still scrollable. Settings categories remain readable. These are original PNG/XML captures, not composites or old screenshots reused as fresh evidence.

Failure history is retained. Historical fixture comparisons initially included asynchronous rest cleanup, then an out-of-bounds `performScrollToIndex(100)` was replaced by scrolling to the actual stale-error node. The Track title matcher initially also selected the Name fact. Two primary-editor bounds attempts incorrectly compared native text to a merged Button or required a native `android.widget.Button` ancestor; the actual disabled Compose control exposes an `android.view.View` parent. The final fixture translates the full Save layout using the already measured complete native title and independently checks the native label. A historical final attempt interrupted during handoff is incomplete. The shared status-bar overlap was a real rendered defect and is fixed by the parent's explicit system/IME inset owner; its original remains under `before/primary-editor`.

Final debug and test APKs were installed in place on the guarded disposable emulator and their installed bytes match the readiness-built artifacts; `receipts/raw/tracks-gym-final-installed.txt` records SHA-256 values. No physical device, full suite, frozen candidate, data format or migration belongs to this focused acceptance. Subjective appearance remains for owner use; no accepted Track/Gym remedy or scoped verification remains pending.
