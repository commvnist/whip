# Gym and Settings experience overhaul — 2026-09-28

Parent integration follow-through: after the shared non-primary dialog safe-drawing repair, the exact backup-overview journey passes again in 10.585s (12.933s wall), including the passphrase child and disclosure/recreation/busy states. Receipt `verification/native/backup-overview-insets-final.log`; parent inspected the latest warning `(1)` original, with full error, receipt and repair actions readable. Other EGS source is unchanged from the evidence below.

Status: **Implemented and verified within the selected scope: two exact JVM methods, five exact native methods and nine personally reviewed fresh originals.** Parent accepted EGS-1–4 before their production edits. Baseline clean `9014937f`, private release 0.3.82. This is a fresh current-source product review, not continuation of the already completed same-day audit. Canonical memory, shared controls, builds and device execution belong to the parent. This agent inspected retained results and originals; the parent executed all checks.

## Evidence and boundaries

Read repository `AGENTS.md`, the full memory index, current feedback/findings/decisions/implementation entries, architecture and the two preceding Gym/Settings matrices. Current owners reviewed: Gym destination routing, Workout first-use/execution/finish, Exercise and Machine libraries, Routines and builder, History/Progress, Settings sections, transactional inputs, restore preview and backup operations. Source review establishes the workflow facts below; it does not establish frame timing, TalkBack speech, provider durability or visual acceptance.

Personally inspected retained originals `artifacts/product-audit/2026-09-28/visuals/product-audit.gym.archived-progress.png`, `product-audit.gym.archived-history-units.png`, and `product-audit.settings.reminder-refresh.png`. They support the established record/summary hierarchy and truthful notification distinction; they are historical evidence, not fresh captures of this change. The first Progress image precedes the already-fixed plural wording and must not be treated as a new defect.

Also personally inspected `artifacts/ux-audit2/2026-09-28/after/short/ux-upgrades.gym.active-lane.native200.png` and `after/normal-5/ux-upgrades.gym.routine-selected-set.png`: the constrained active lane preserves next-set navigation, countdown and ±15/Stop controls above a scrollable composer; the selected routine Set retains adjacent prescriptions as concise context with only its own fields expanded. Their existing hierarchy is purposeful; this review does not claim those older images certify the new landing page or a fresh active-workout run.

Preserve schema 46, epoch 6, portable format 26, authored units/sets/routines, immutable equipment and completed-workout snapshots, phased routine position and existing permission/delivery boundaries. New 5/3/1 authoring and Health Connect integration remain retired. The parent corrected the stale active-integration paragraph in `docs/architecture.md`; current source and documentation agree on retirement.

## Accepted-plan candidates

### EGS-1 — Make the Workout entry useful between sessions

**Grounded workflow improvement, P1.** `WorkoutContent` renders only “No Workout in Progress” plus a generic routine/library/empty-workout decision whenever `activeSession == null`. An experienced user with saved plans and history gets the same empty-state hierarchy, must open Library/Routines to find the next planned day, and receives no recent-session context. This is a source-confirmed navigation cost, not a measured performance defect.

Before: returning-user empty state. After: a compact launchpad with a deliberate start action, routine context and exact recent History access; retain the true first-use guide. Candidate routine policy is a bounded recent/pinned collection plus explicit Browse All, with no inference of “next day” for ordinary multi-day routines. Programmed next-day starts use the existing `startRoutine(routineId, null)` owner; one-day static starts use the explicit day; multi-day static routines open the exact routine to choose. Missing equipment opens the routine to resolve it instead of beginning an invalid workout. Existing active workout rendering is unchanged.

Owners: `GymScreens.kt`, existing route callback structure; no repository mutation change. Acceptance: direct next-day start, static multi-day explicit choice, equipment-resolution route, most-recent completed workout opens exact History, first-use and active-workout keeps. Exact selectors will be recorded after implementation.

### EGS-2 — Find a saved routine by its actual contents

**Grounded workflow improvement, P2.** `RoutineContent` offers archive and manual ordering but no search. Exercise, Machine and History libraries already support direct lookup. A person remembering an exercise or training day must expand routines one by one.

Before: full routine collection with only archive scope. After: saveable local multi-term search across routine name/notes, day names and linked retained Exercise names; visible result count and Clear Search recovery. Keep archived scope explicit; an exact incoming route still owns visibility. Searching cannot reorder a filtered subset; choosing Reorder clears constraints using the existing library precedent. Active-session recovery stays visible even when its routine is filtered out.

Owners: `GymScreens.kt`, reusable `WhipSearchField`; a small pure matcher if needed. Acceptance: exercise/day cross-term match, no-match recovery, archived scope, recreation, exact route and reorder boundaries. Search is local presentation and changes no persisted order or authored text.

### EGS-3 — Put backup protection and recovery before administration

**Grounded information-hierarchy improvement, P1/P2.** Configured Data & Privacy starts with a tall folder-management card (receipt/file, automatic toggle, retention editor, change/forget controls), followed by backup/restore, then five always-visible spreadsheet export rows. Administrative detail dominates the two common jobs: making a recoverable copy and restoring one.

Before: configuration-first list. After: compact truthful folder-protection overview, reachable backup and restore choices, disclosure for folder administration and spreadsheet exports. Show the verified receipt and automatic state without asserting that an unverified folder is protected; warnings remain visible outside collapsed administration. Keep plain versus encrypted backup explanations, transient passphrases, native document picker ownership, merge/replace preview and two-stage destructive review. Preserve all action labels and the existing status/retry owner.

Owners: `SettingsScreens.kt`; existing state and ViewModel callbacks. Acceptance: configured/unconfigured/no-receipt/error summaries, one-off backup/restore reachability, expanded administration and CSV choices, disclosure recreation, in-progress disabling, settled result reveal. No change to SAF grants, retention policy, serialization or backup contents.

### EGS-4 — Update clock values without rebuilding unchanged workout totals

**Source-confirmed avoidable work, P2; accepted by parent before production edits.** `buildGymUiState` already calculates the authoritative summary on the background data/settings projection. `withClockTick` then rebuilds the complete exercise lookup, placement/set lists, volume and e1RM every second on its collector path. Only elapsed time and rest countdown depend on that tick. Copy the existing summary with the same clamped elapsed rule, retaining totals until the next data/settings projection. Add one exact JVM comparison/timing fixture without a timing threshold; report observed CPU work separately from unmeasured frame smoothness. Parent separately owns removal of clock-only invalidation in unified search.

Active-execution hierarchy remains a justified keep after fresh source tracing: compact sticky next-set/rest controls retain full exercise/prescription context in the scrollable body at large text; completed sets disclose separately; one finish owner and exact mutation recovery remain. The timer optimization must not rewrite these interaction owners or calculations.

## Complete workflow dispositions

Each row is a current-source review disposition, not an executed test. Keep means the existing behavior is suitable within this request, not that future improvement is impossible.

| Workflow | Disposition and user impact | Current owner / acceptance boundary |
| --- | --- | --- |
| Gym entry, new user | Keep create-first-exercise and start-empty choice; reusable library is explained | `WorkoutContent`; EGS-1 first-use keep |
| Gym entry, returning user | EGS-1 replaces empty-state hierarchy with useful next action and last-session context | `WorkoutContent`, route callbacks |
| Gym primary navigation | Keep Workout/History/Progress/Library; stable child back navigation | `GymDestinationHost`; parent owns shell |
| Library discovery | Keep five named destinations with descriptions | `GymLibraryLanding` |
| Exercise browse/search/filter | Keep multi-term matching, archive/favorite/category/equipment scope and explicit clear recovery | `ExerciseLibraryContent` |
| Exercise order | Keep full active-list manual order and clear-filter affordance | `sortedForLibrary`, reorder owner |
| Exercise authoring | Keep capability-driven tracking/unit/bodyweight/defaults and immutable history interpretation | Exercise editor + repository snapshot |
| Exercise archive/delete | Keep explicit archive versus impact-reviewed permanent delete; retained references | deletion review coordinators |
| Machine search and linking | Keep profile/location/version search across every linked exercise | `MachineLibraryContent`, `machineMatchesQuery` |
| Machine resistance | Keep mass versus ordinal settings and direction, never fabricate mass | machine editor, `equipmentScopeKey` |
| Machine versions/archive/delete | Keep explicit new version, existing archived bindings and reviewed removal | machine snapshot/deletion owners |
| Categories | Keep membership/reorder/archive and existing historical lookup | `ExerciseCategoryContent`, history identities |
| Routine lookup | EGS-2 adds direct search across actual routine contents | `RoutineContent` |
| Routine start | EGS-1 reuses current next-phase and explicit static-day rules | `startRoutine` |
| Routine active recovery | Keep one global/owning-routine Open Active Workout; do not repeat per day | `RoutineContent` |
| Routine outline | Keep full-screen save/discard, explicit day/placement ordering, copy/import and retained draft | `RoutineBuilderScreen` |
| Routine prescriptions | Keep one selected Set editor, collapsed applicable load/reps/time/distance summaries | `RoutineSetEditorCard`, `routineSetPrescriptionSummary` |
| Routine grouping/warmups | Keep explicit grouping boundaries and generated warmup review | builder grouping/warmup helpers |
| Ordinary phased routines | Keep independent phase authoring and explicit training-max basis | `startCustomPhasedRoutine`, phase helpers |
| Retired 5/3/1 | Keep retired creation; preserve imported/legacy execution and snapshots | legacy retirement + program-kind compatibility |
| Active workout identity/actions | Keep stable session header, add/edit/group/arrange and one Finish owner | `WorkoutContent` |
| Quick Set | Keep exact target/placement boundary, local draft, atomic complete and optional next | `QuickSetEntry`, `saveQuickSet` |
| Execution readability | Keep shared execution cards, identity→values→status→target hierarchy | `WorkoutExerciseCard`, `WhipExecutionItem` |
| Grouped progression | Keep explicit group rotation and exact requested-placement focus | `selectNextWorkoutSet`, grouping helpers |
| Workout layout undo | Keep same-session/generation boundary and stale-arrangement refusal | structure boundary/coordinator |
| Set omission/removal | Keep omission reason, undo, optional prerequisite and exact mutation identity | set boundary/deletion owners |
| Rest timing | Keep app presets versus local override and explicit seconds/Custom Time | `RestTimerCard`, `RestDurationDialog` |
| Rest external alert | Keep effective channel/permission notice and deadline-owned delivery | Rest notification scheduler/receiver |
| Finish/discard | Keep reviewed incomplete/optional work, exact revision and immutable finish decision | finish review and repository receipt |
| History browse/calendar | Keep named search/filter scope, archived identities, single expanded record and date selection | `WorkoutHistoryContent` |
| History evidence | Keep saved load qualifiers/units, equipment snapshots and omitted/performed distinction | `HistoricalWorkoutSetRow`, `shortLabel` |
| Repeat versus Resume | Keep new-copy versus reopen distinction and Resume confirmation | History actions |
| History reuse/share | Keep exact source copy boundary and retained exercise names | history copy coordinator, `workoutShareText` |
| Old training decisions | Keep complete Show All route beyond first 20 | History standalone decisions |
| Progress first use | Keep explicit exercise/workout creation route instead of fabricated zero trend | `GymProgressContent` |
| Progress overview | Keep weekly summary with optional detail, selected tracked records and trend below | Progress grouped hierarchy |
| Progress graph | Keep metric capabilities, rep target validation, exact point/source identity and selected-unit axis | graph projection/presentation |
| Progress equipment scope | Keep separate immutable profiles; combine versions only by explicit compatibility choice | equipment scope selector |
| Tracked records | Keep saveable reorder/selection draft, discard review and numbered-machine capability | `TrackedRecordsManagerDialog` |
| Gym calculators | Keep local calculators and presets; do not silently rewrite logged prescriptions | `GymToolsContent` |
| Settings category discovery | Keep descriptive categories, compact drill-in and wide sidebar | `SettingsContent`, sidebar |
| Theme/presentation | Keep native theme/dynamic-color availability, single density, low-pressure and Goal celebration switch | Appearance section |
| Home/Area preferences | Keep explicit opening-area behavior and section order; parent owns product behavior | Appearance section |
| Dates/units/numbers | Keep transactional validation, raw draft continuity and explicit time-zone/cutoff meaning | Planning, typed editor |
| Custom units | Keep identity-preserving rename versus new version and archived definitions | custom unit coordinator |
| Organization | Keep direct Area/Tag managers and named custom emoji; parent owns managers | Organization section |
| Gym defaults | Keep separate input/rest/calculation category with direct preset editor | Gym section |
| Notification diagnostics | Keep actual Android permission/channel/alarm status and real repair routes | Reminders section |
| Reminder refresh | Keep all-domain attempt, truthful partial failure, request-owned retry | Settings mutation coordinator |
| Quiet hours | Keep transactional start/end, overnight summary and pinned editor during mutation | typed settings |
| Backup discovery/protection | EGS-3 reduces administration before recovery while keeping verified truth | Data & Privacy |
| Folder backup durability | Keep staging→reopen/verify→rename→reverify→receipt→retention | `PortableBackupManager` |
| Encrypted export/unlock | Keep transient secrets, password keyboard, matching validation and fail-closed picker request | Settings export/unlock + ViewModel |
| Import preview/merge | Keep authenticated compatibility preview and idempotent new-record merge | backup preview/repository |
| Replace/recovery | Keep two-stage confirmation and atomic rollback marker across database/preferences/jobs | restore coordinator |
| Spreadsheet export | EGS-3 disclosure preserves five named domain exports and explicit non-backup meaning | CSV action list |
| Reset all data | Keep separate danger zone, duplicate-submit guard and global recovery | Settings reset owner |
| About/privacy | Keep exact package/build and local-data explanation | About section |
| Widgets/deep links | Keep generation and exact identity gates; parent reviews shell/open routing | widget/action receivers |
| Health history | Keep historical records, no new external sync or resurrected integration | `LegacyHealthHistory`; architecture reconciled by parent |

## Verification plan and limits

No arbitrary finding quota. Only EGS-1–4 are recommended; accepted remedies must be fully implemented. Parent serializes all Gradle/device work and retains fresh captures. Minimal exact JVM/native selectors are each bounded by `timeout --kill-after=3s 55s`; no full/ready/candidate batch. Device screenshots, native font scale and real document-provider effects must be distinguished from source contracts and synthetic component state. No commit, push, release or owner-phone operation by this agent.

### Implementation checkpoint

EGS-1 uses two quick routine cards: pinned first, then most recent completed use, then authored order, deduplicated by identity. Two cards bound the choice set while leaving an explicit Browse All action and an empty-workout action in the same short list; the fresh native enlarged-text capture shows both choices and both actions without overlap. Programmed starts pass no day override; static one-day starts pass the exact day; static multi-day and missing-equipment choices route to the exact routine. Latest finished, nonarchived session opens its exact History. Active workout continues through its existing owner. Routine start suppresses another submit while the existing operation reports Running; the repository still enforces one active session transactionally.

EGS-2 stores the query with `rememberSaveable`, searches retained names/notes/day/exercise content, preserves active versus archived scope and exact routes, and clears scope before full-list reorder. Search results and launchpad selection memoize their actual source lists so unrelated clock ticks do not rebuild them. EGS-3 retains Back Up Now and error repair at the overview; last verified timestamp stays visible, filename moves into folder detail. Administration and CSV disclosures restore their expanded state. Existing exact action labels, busy states and all backup persistence are unchanged. EGS-4 copies only elapsed time from the authoritative projected summary and uses the existing rest countdown helper.

| Exact selector | Acceptance scope | Status |
| --- | --- | --- |
| `com.whip.app.ui.GymUxRulesTest#workoutLaunchChoicesAndRoutineSearchKeepAuthoredScope` | Pinned/recent/deduplicated limit, archived exclusion, static day choice, programmed next day, cross-term retained exercise search | Passed in parent exact JVM batch |
| `com.whip.app.ui.GymUxRulesTest#clockTickPreservesProjectedTotalsAndOnlyUpdatesTime` | Baseline summary equality, ended/rollback clocks, refreshed totals, absent session, countdown; 200-exercise/120-set old/new median CPU observation without threshold | Passed in parent exact JVM batch |
| `com.whip.app.GymPowerInputUiTest#workoutLaunchpadKeepsExactChoicesAtLargeText` | Actual-font-scale component choices, exact callback IDs, routine equipment route, recreation, first use; synthetic immutable fixture | PASS 1/1, 7.195s; `verification/native/gym-launchpad.log` |
| `com.whip.app.GymLibraryJourneyE2ETest#routineSearchRestoresScopeAndLaunchesTheExplicitDay` | Real library search, archive scope, recreation, clear recovery, constrained reorder, exact routine route, persisted chosen-day active workout | PASS 1/1, 15.752s; `verification/native/routine-search.log` |
| `com.whip.app.ui.SettingsResponsiveUiTest#backupOverviewKeepsRecoveryVisibleAndAdministrationRestorable` | Actual-font-scale overview, unconfigured/unverified/verified/error states, encrypted form entry, disclosure recreation and busy CSV; synthetic provider state | PASS 1/1, 10.229s; `verification/native/backup-overview.log` |
| `com.whip.app.ui.SettingsResponsiveUiTest#deepDataOperationMakesItsSettledResultVisible` | Existing settled-result reveal after deep CSV position; updated to open CSV disclosure | PASS 1/1, 6.160s; `verification/native/backup-result.log` |
| `com.whip.app.DataPrivacyJourneyE2ETest#plainAndCsvExportsKeepTheirRequestedFormats` | Existing real provider formats and return; updated to open CSV disclosure | PASS 1/1, 40.009s; `verification/native/backup-csv.log` |
| `com.whip.app.SettingsBehaviorUiTest#portableBackupExplainsItsExactSharedFolderDeletionBoundary` | Existing exact deletion-boundary explanation; updated to open folder disclosure | Updated, not run |
| `com.whip.app.ui.SettingsResponsiveUiTest#wideSettingsCannotSwitchCategoriesBehindATypedEditor` | Existing wide editor/CSV busy checks; updated to open CSV disclosure | Updated, not run |

Paths in the table are relative to `artifacts/experience-overhaul/2026-09-28/`. These are three new native methods and two updated existing neighbors. All five retained logs show `OK (1 test)` and exit 0, with respective command wall times 9.906s, 18.104s, 12.156s, 8.557s and 42.301s. The two remaining updated neighbors are explicitly unexecuted in this selected campaign. `git diff --check` passed after the coherent production/fixture edits. No schema/settings format/migration/dependency change. No frame-rate, battery, TalkBack or full-provider-durability acceptance claim.

Integration attempt: the parent's initial exact JVM command stopped during production compilation after 29 seconds because the new routine matcher lacked the `RoutineExercise` import (`artifacts/experience-overhaul/2026-09-28/verification/search-initial.log`). The import is corrected. That attempt executed no acceptance tests and is not a pass. Production and fixtures are frozen for the parent's next serialized check.

Parent reports the corrected production compile passed in 46 seconds (`verification/production-compile.log`). Compilation is distinct from the domain acceptance methods above. Parent also applied the accepted no-routine launchpad secondary action wording “Open Routines,” matching its actual collection callback.

Personally read the retained `artifacts/experience-overhaul/2026-09-28/verification/jvm/TEST-com.whip.app.ui.GymUxRulesTest.xml`: both new methods passed, zero failures/errors/skips, suite 0.659 seconds. The same 200-exercise/120-set fixture reports median old full summary calculation **46,006 ns** versus clock-only copy **1,695 ns** per tick over seven 100-call samples after warmup. This roughly 27× difference concerns this CPU fixture only; no timing threshold, device frame-rate or battery-life claim. The Gym clock fix avoids rebuilding unchanged workout totals; the parent's separate shared-search content keys avoid rebuilding the cross-feature index for those same unrelated clock updates.

### Fresh visual acceptance and remaining boundaries

Personally inspected these nine PNG originals under `artifacts/experience-overhaul/2026-09-28/originals/`, paired with the native methods above:

| Original | Visual assessment |
| --- | --- |
| `experience.gym.launchpad.native200.png` | Two readable, naturally wrapping routine choices; exact programmed next day and explicit static-day choice differ clearly. Browse All and Start Empty remain visible; latest-workout context continues below the viewport through normal scrolling. The synthetic programmed routine intentionally has zero exercises. |
| `experience.gym.first-use.native200.png` | First-use heading, reusable-exercise explanation, Create First Exercise and Start Empty remain readable without clipping. |
| `experience.gym.routine-search.png` | Real dark-theme shell shows the retained query, one-result Active count, archive scope and exact matching routine cleanly. |
| `experience.gym.routine-started.png` | Exact chosen Sunday identity is visible with the next Set, target load/reps, rest controls and exercise actions; normal execution hierarchy remains coherent. |
| `experience.settings.backup-unverified.native200.png` | Connected folder explicitly says no verified backup yet; automatic state and Back Up Now lead into the save/restore section with readable wrapping. |
| `experience.settings.backup-warning.native200.png` | Prior receipt, full folder-access warning and Reconnect or Change Folder are visible outside administration. The large error card legitimately pushes recovery choices further down the scrollable page. |
| `experience.settings.backup-verified.native200.png` | Retained scroll position starts midway through the summary; visible automatic-on state, Back Up Now and backup choices are readable. This image does not independently certify the entire receipt; the warning image shows that receipt and the native state assertion covers the clean state. |
| `audit2.settings.deep-operation.before.png` | Three backup/restore actions precede folder administration and the expanded, clearly non-restorable CSV exports. All five export choices are readable. |
| `audit2.settings.deep-operation.failure.png` | Settled failure is brought into view above the backup controls after deep scrolling; its message and Dismiss action are clear and the backup/restore actions remain accessible. |

No visual follow-through is required for the inspected states. Latest History is covered by exact callback identity plus production routing review, not a new full History-navigation device journey. The started-workout image certifies its entry state, not a complete new finish/timer regression. Backup overview and settled-result component cases use synthetic provider state; the separate plain/CSV device journey covers actual requested export formats and picker return. Neither these checks nor source-reviewed keeps claim new coverage of all import, retention, restore rollback, delivery or accessibility paths.

### Follow-through: deleting one nested history record must retain its parent

Fresh owner feedback about Habit history dismissal prompted a read-only sibling audit. No analogous confirmed Gym/Settings defect was found. `GymAreaContent`'s session mutation receipt closes only the matching Set editor after `SetUpdated`; `WorkoutSetRemoved` changes skip/undo state without navigation. `WorkoutExerciseCard` observes the removed Set and clears only its local removal confirmation. Workout History retains its expanded-session identity through Edit Details; historical Sets and Training Max decisions are read-only, with no individual-history-delete command to repair. Deleting a whole workout clears only that workout's exact focus and stays on History, which is the expected entity-level transition.

RoutineBuilder Set deletion mutates the selected placement's draft; placement/day deletion updates selection and undo inside the open builder. Settings custom-unit archive/restore leaves the collection open, create/rename/version closes only the child editor, and custom-emoji Remove closes only the row menu. Backup replacement/reset are whole-data actions and remain distinct. These are **source-reviewed keeps**, not new executed deletion regressions; parent owns the reported Habit repair and any shared/nondomain cases.
