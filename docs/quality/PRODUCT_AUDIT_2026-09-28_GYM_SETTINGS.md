# Product audit: Gym, Settings and external workflows — 2026-09-28

Status: **In progress; PGS-1–7 accepted by the parent before production changes.** Baseline: clean `d737209a705886e35f312fb29db3a35cc881da70`. This is the Gym/Settings contribution to the new full-product audit. The parent owns canonical product memory, shared UI, integration checks and delivery. No fresh test execution is claimed at this implementation checkpoint.

## Review boundaries

Read `AGENTS.md`, the complete memory index, current findings/decision entries and the preceding [Gym/Settings audit](UX_AUDIT_2_GYM_SETTINGS_2026-09-28.md). Rechecked current source at the workflow owners instead of adopting the earlier audit's passing verdict. The separate 5/3/1 authoring product was explicitly retired under FB-20260920-002; ordinary phased routines and retained legacy execution/history are current compatibility boundaries, not authorization to restore the retired wizard. Health Connect is similarly retired; local measurements and preserved old history remain.

The review traces entry, browse/search/filter, authoring, execution, history, archive/delete, restoration and external delivery. Current code has strong existing owners for request-scoped catalog/session mutations, immutable exercise/equipment policy snapshots, explicit destructive impact review, typed Settings transactions, portable backup verification and generation-gated receivers. Preserve them. UI findings below concern concrete lost facts or actions, not a new visual theme. Native enlarged-text/keyboard/TalkBack claims require fresh execution and original inspection; source review alone is not visual certification.

## Prioritized findings and proposed implementation

### PGS-1 — Numbered-machine records can be selected but disappear after Save

**Confirmed from current source, P1.** `recommendedTrackedRecords` and `availableTrackedRecords` in `GymScreens.kt` offer `PersonalRecordType.MaxMachineSetting` when an exercise has a numbered machine or an existing such record. `Exercise.supportedTrackedRecordTypes()` in `core/TrackedGymRecords.kt` never returns that type. The Progress section, Exercise tracked-count summaries, manager's original draft, existing-choice filter and weekly improvement count all apply that narrower helper. A user can save the offered machine benchmark, see no tracked record, then reopen and save a manager draft that silently omitted it.

**Remedy:** one consistent capability check for tracked selections, with numbered-machine choices conditioned on the actual machine/history context. Reuse it at every consumer; retain exact equipment scopes and reverse-number resistance behavior. Do not make numbered-machine records a universal recommendation for every exercise or coalesce unlike machines.

**Acceptance:** choose a numbered-machine record, save, display the exact current record/source, reopen and preserve selection/order, then save again. Cover active, archived and removed machine profile snapshots and an unsupported ordinary record. Add focused JVM capability coverage and a native manager→dashboard→source journey.

### PGS-2 — Tracked-record editing loses unsaved work on recreation or accidental close

**Confirmed from current source, P2.** `TrackedRecordsManagerDialog` stores its reordered/selected `List<TrackedGymRecord>` in `remember(initialExerciseId, original)`, while only the expanded exercise is saveable. Recreation restores the expanded exercise but reconstructs the draft from persisted settings. The Close/Back callback also dismisses directly with no dirty-draft review, unlike other authored editors.

**Remedy:** keep the small selection draft in saveable state using the existing tracked-record codec, retain the original snapshot for dirty comparison, and use the existing unsaved-changes dialog for dismissal. Preserve normal explicit Save and avoid a new persistence framework. Parent's generic editor changes must not duplicate this owner.

**Acceptance:** select/reorder/remove, recreate, verify the exact draft remains uncommitted, cancel a discard and retain it, then save exactly that selection. Clean close remains immediate.

### PGS-3 — Archiving library items changes historical summaries and makes evidence hard to find

**Confirmed from current source, P1/P2.** `GymProgressContent` passes `state.exercises` (active only) to `buildGymWeeklySummary`; the domain helper returns zero volume when the referenced Exercise is missing. The same page's category-volume lookup also uses active exercises. Thus archive leaves completed Sets/repetitions counted while their unchanged volume disappears. With every Exercise archived, Progress says “No Exercises to Track” even when completed evidence exists. History's Exercise/Routine/Category filters likewise offer active-only identities despite retaining their old workouts; `shareWorkout` uses active-only Exercise names and emits “Exercise” for an archived identity.

**Remedy:** distinguish current authoring choices from saved-evidence presentation. Progress calculations and historical lookup use retained active+archived identities; historical selectors disclose Archived context. Preserve current-only exercise pickers for new workouts/routine additions. Share retains the actual archived exercise name. Keep workout archival/discard exclusion from performance statistics intact.

**Acceptance:** finish a known-volume workout, archive its Exercise (and routine/category for filters), show the same weekly/category volume, browse its graph and exact source, locate it through historical filters, and share its name. Archived-only history must not become a first-use state. Current create/start pickers must still exclude archived exercises.

### PGS-4 — Old non-total loads can be relabelled into the wrong unit

**Confirmed from current source, P1.** `WorkoutSet.shortLabel` uses the raw `enteredWeight` for PerHand/PerSide/AddedLoad/bodyweight/assistance/displayed load meanings, but labels it using the current selected Exercise or machine display unit. It never converts from `enteredWeightUnitId`. For a saved 20 kg per-hand set, changing the Exercise display unit to pounds can show “20 lb per hand (88.2 total).” History, previous-set suggestions and sharing share this formatter, so one incorrect conversion boundary affects several workflows. `WorkoutSet` already retains the entered unit and canonical load; no missing data or migration is involved.

**Remedy:** convert the authored entered amount from its saved mass unit into the displayed unit before attaching its qualifier; use the saved exercise/machine snapshot only as a fallback for absent older metadata. Preserve canonical totals and numbered-machine settings. Fix the common formatter once.

**Acceptance:** kg↔lb PerHand/PerSide/AddedLoad/assistance cases and unchanged Total/numbered-machine cases; native History shows consistent qualified and total values after an unrelated Exercise unit change. Previously stored Sets must remain byte-for-byte unchanged.

### PGS-5 — Settings directs failed reminder reconciliation to an action that does not retry it

**Confirmed from current source, P2.** `SettingsViewModel.updateTypedSetting` catches Task/Habit/Goal scheduler failures after a durable quiet-hours/time-zone edit and returns “use Refresh Notification Status to retry.” The named Settings action only increments `diagnosticRefresh`; the resulting code rereads Android permission/channel/alarm availability and never invokes the failing schedulers. There is no scheduling retry behind the promised recovery action.

**Remedy:** make this existing action refresh all three reminder schedules through the existing Settings mutation/delivery boundaries, return truthful request-owned success/failure feedback, and refresh the Android snapshot. Attempt independent reminder domains even when one fails. Do not reset active timers, clear unrelated notifications, use recovery bypasses, or show successful reconciliation before it finishes. A transient warning should keep a real retry path.

**Acceptance:** a focused injectable scheduler check verifies all-domain attempts and partial-failure feedback; native Settings action shows in-progress/terminal feedback and remains repeatable. Existing channel-return and quiet-hours transaction tests remain neighbors. No guarantee of Android/OEM delivery is implied by a successful scheduling refresh.

### PGS-6 — Older manual Training Max decisions are counted but unreachable

**Confirmed from current source, P2.** History displays the complete standalone-decision count but renders only `standaloneTrainingMaxDecisions.take(20)` with no paging or “show more” action. A user with 21+ manual program edits cannot inspect older records, even though the data is retained and the count promises it.

**Remedy:** reuse the simple progressive reveal pattern already used for chart Data Points: initially show a bounded recent set, expose the remaining count and allow the entire filtered history to be reached. Keep filters/date order and stable decision data.

**Acceptance:** at least 21 distinguishable decisions, older exact amount/date/reason reached through the visible action, filtered count accurate, recreation predictable. Do not render an unbounded hidden list by default.

### PGS-7 — Gym CSV omits the status needed to interpret exported rows

**Confirmed output-contract gap, P2.** `RoomBackupRepository.exportGymCsv` joins all sessions and undeleted Sets, including active/discarded/archived workouts and incomplete planned Sets, but exports neither workout state/archive nor Set completion/placement outcome. It also labels the workout with the live Exercise tracking type instead of `workout_exercises.trackingTypeSnapshot`. A spreadsheet consumer cannot distinguish performed history from a template not yet performed and can receive a current type for old measurements.

**Remedy:** keep all retained records available, add explicit workout state/archive, placement outcome and Set completion fields, and expose the saved tracking type for joined workout rows (falling back to the Exercise only for its no-workout row). Do not silently filter out data, change the portable-backup format or add an import path.

**Acceptance:** a real Room export containing finished, active, discarded/archived and uncompleted Sets has correct explicit status fields and the historical type after an Exercise type edit. Existing CSV consumers/tests checked for column assumptions; document this additive CSV column change.

## Workflow disposition matrix

These are **source review dispositions**, not executed test counts. A Keep means no new defect was substantiated in this pass and names the existing ownership to preserve; it is not all-state certification.

| Workflow | Current evidence/owner | Disposition and acceptance boundary |
| --- | --- | --- |
| Gym loading/error/retry | `GymAreaContent`, `GymViewModel.retryLoading`, shared load owner | Keep explicit load failure instead of false empty content; shared constrained layout parent-owned. |
| Primary Gym navigation/Library children | `GymDestinationHost`, saved destination/back state | Keep current Workout/History/Progress/Library hierarchy and restored context. |
| Empty Gym/no library/no routines | `WorkoutContent`, `GymLibraryLanding` | Keep first-use choices; PGS-3 prevents archived evidence being mistaken for first use. |
| Ad hoc Workout start/name/date/notes | `WorkoutEditorDialog`, repository active-session guard | Keep explicit Save/error/dirty dismissal; no speculative date policy change. |
| Routine start/next/specific day/resume | `RoutineContent`, exact source day/phase | Keep preview and out-of-order confirmation. |
| Recovered active Workout | durable session UUID/revision and active-workout state | Keep one recovered session and existing stale-boundary checks. |
| Quick Set entry/prior set/suggestion | `QuickSetEntry`, request-scoped Set mutation | PGS-4 corrects prior-value units; preserve one composer and failed draft. |
| Correct completed Set | `WorkoutSetEditorDialog`, saved policy snapshot | Keep authored/performed distinction, exact mutation identity and dirty draft. |
| Add Exercise for current Workout | catalog/session mutation coordinators | Keep workout-scoped addition and return to exact session. |
| Substitute/remove Exercise | reviewed structure boundary and retained outcomes | Keep source history; PGS-7 exports outcome explicitly. |
| Add/duplicate/skip/restore Set | Set mutation boundary and session-owned undo | Keep completion/prerequisite checks and generation guards. |
| Superset/circuit/arrangement | keyed blocks, structure fingerprint and undo | Keep accessible reorder controls and batch arrangement. |
| Workout notes/equipment changes | placement boundary, machine-choice editor | Keep current session snapshot ownership; no archive resurrection. |
| Rest timer/presets/custom/default | `RestTimerCard`, deadline/revision, rest-duration owner | Keep recently verified fixed elapsed truth, proper precedence and transient session override. |
| Rest notification unavailable | effective current channel + lifecycle resume | Keep effective-channel warning; selected native neighbor retained. |
| Finish incomplete required work | finish boundary and explicit nonperformance review | Keep truthful omitted work and one advancement. |
| Discard active Workout | reviewed UUID/revision and repository rejection | Keep exact review; no new approval layer. |
| History list/query/date range | `WorkoutHistoryContent` | PGS-3 keeps archived identities filterable without changing session/archive scope. |
| History calendar/native large text | shared spacing and full-width date/count labels | Keep prior fix; fresh native evidence required before any new layout claim. |
| History collapsed/expanded row | `WorkoutHistoryCard`, all Exercise map | PGS-4 fixes load rendering; completed counts and outcomes retain their owner. |
| History edit details/resume/repeat | distinct callbacks and source identity | Keep original-vs-copy semantics and no repeat program advancement. |
| History Use Again/Save as Routine | exact retained source Sets and new requested UUIDs | Keep complete reviewed snapshot; no silent current-template substitution. |
| History sharing | `shareWorkout` + `shortLabel` | PGS-3/4 fix archived identity and entered-unit conversion; PGS-7 distinguishes exported performance status. |
| Manual Training Max history | standalone decision filter and `.take(20)` | PGS-6 exposes retained older decisions. |
| Workout archive/restore/permanent delete | impact/revision review, discarded/archived records | Keep explicit removed/recalculated/preserved boundaries. |
| Progress first use/archived-only | `GymProgressContent` early return | PGS-3 distinguishes absent data from archived library items. |
| Weekly volume/category allocation | `buildGymWeeklySummary`, category policy | PGS-3 retains archived Exercise contributions; category order remains semantic. |
| Graph capabilities/range/rep target | domain graph metrics and range validator | Keep relevant measurements and visible invalid input. |
| Graph exact point and comparison | stable series/session identity | Keep preceding exact-identity correction; PGS-3 permits historical archived choices. |
| Graph machine version/direction | equipment scope/snapshot and direction-aware best | Keep unlike-scale separation and historical version fallback. |
| Tracked record add/recommend/select | supported helper vs contextual choices | PGS-1 aligns capability consumers. |
| Tracked record ordering/dismiss/recreate | unsaved list in plain `remember` | PGS-2 preserves explicit authored draft and safe dismissal. |
| Tracked record source navigation | resolved personal-record source session | Keep exact source; PGS-1 verifies machine record route. |
| Routine outline/day copy/move/remove | `RoutineBuilderState` + existing ViewModel | Keep atomic draft, day identity and undo. |
| Routine applicable Set inputs | capabilities and `routineSetValidationError` | Keep prior timed/distance/Reps correction; no new finding supported. |
| Routine collapsed prescriptions | load/reps/distance/duration/rest/effort summary | Keep complete concise summary and selected editor. |
| Routine phases/Training Max/progression | generic phase owner and explicit TM apply | Keep ordinary flexible path and reviewed increase boundaries. |
| Warm-up generation/rep schemes | existing transformations and validation | Keep existing scheme owner; no duplicate template system. |
| Nested Exercise/Machine creation | existing builder child editor and return state | Keep return to exact day and placement. |
| Legacy 5/3/1 active session/history | `LegacyRoutineRetirement`, retained snapshots | Keep delayed conversion for active source and truthful old history; no new authoring. |
| Exercise library/search/sort/archive | shared search and archive collections | Keep current-only authoring; PGS-3 affects evidence consumers only. |
| Exercise unit/load meaning edit | explicit Convert/Keep/Reset defaults | PGS-4 prevents current defaults relabelling historical entered amounts. |
| Exercise permanent delete | reviewed impact + exact mutation | Keep explicit history deletion contract, archive alternative. |
| Machine search/create/version/archive | full link search and immutable configuration snapshots | Keep existing profile versioning; PGS-1 recognizes retained level records. |
| Categories/order/allocation | links and primary/full/fraction policy | Keep semantic order; PGS-3 retains historical identities in filtering. |
| Tools e1RM/percentages/plates | formula cutoff, unit review, bounded inventory solver | Keep explicit example and verified formula semantics; no new tool abstraction. |
| Settings categories/compact/wide | category-owned list state and typed-editor guard | Keep seven categories; parent shared editor changes coordinated. |
| Theme/dynamic color/celebration | platform capability and owner preference | Keep one celebration toggle and density; no new visual options. |
| Home sections/order/opening Area | minimum section guard and saved scope | Keep current controls; shared shell parent-owned. |
| Planning time zone/clock/week start | transactional typed edits and scheduler follow-up | PGS-5 supplies truthful retry; parent audits shared time-picker layout. |
| Numeric precision/units/custom units | typed transaction and immutable unit identities | Keep explicit conversion/versioning and request-local errors. |
| Task/Habit/Review defaults | documented new-entity preference boundaries | Keep existing rules; productivity agent owns feature meaning. |
| Gym input/statistics/rest preferences | settings transactions and actual consumers | Keep 36-rep cutoff and independently authored values. |
| Areas/Tags/custom emoji | organization mutations and shared taxonomy dialogs | Shared taxonomy parent-owned; no conflicting local edits. |
| Notification permissions/channels/exact access | actual platform diagnostics and repair intents | Keep real-channel repair and resume refresh; PGS-5 retries scheduling separately. |
| Test notification | current permission/channel availability + local result | Keep real outcome and no false delivery guarantee. |
| Quiet hours/overnight settings | serialized reminder semantic boundary | PGS-5 makes partial scheduler failure recoverable; settings remain saved. |
| Portable backup configure/reconnect/forget | mutex, confirmed preference write, provider grant | Keep safe shared-folder filename boundary and retained files. |
| Automatic backup/retention/retry | manager's verification/pruning and scheduler | Keep verified output and narrow automatic-file pruning; no provider data cleanup changes. |
| Plain/encrypted export/picker cancel | pending format request, transient passphrase | Keep fail-closed lost request and no secret saved state. |
| Restore preview/unlock/mismatch | checksum/domain validation + preview ownership | Keep local error and explicit preview; no silent restore. |
| Merge/replace/reset/recovery | atomic backup validation, application gate and recovery snapshot | Keep current destructive review and background rebuild; no new epoch/schema/format. |
| CSV exports | raw table exports and data privacy actions | PGS-7 clarifies Gym semantics; other exports reviewed without a new confirmed scope defect. |
| Widget configuration/preview/Area/Habits | `WhipWidgetConfigureActivity`, generation-keyed drafts | Keep saved selection, live area availability and existing host validation; no visual redesign assumed. |
| Widget actions/cache/cold start | `WhipWidgetProvider`, current-content/generation checks | Keep exact visible item validation, current scope and update/recovery gates. Prior widget campaign is context, not new execution. |
| Boot/time-zone/exact-access changes | `ReminderRuntimeMaintenance`, time invalidation receiver | Keep serialized rebuild, fixed-zone distinction and widget date refresh. |
| Notification actions and cold delivery | private receivers, revision/generation claims | Keep recently verified Focus/Rest guards; no new defect reproduced in this read-only pass. |
| External text capture/deep links | manifest text/plain entry and app route | Parent owns external routing/search; retained private mutation receivers remain unexported. |
| About/local-data statement | source-generated build identity and export wording | Keep truthful device-local storage statement; no Health Connect/cloud restoration. |

## Implementation allocation and verification plan

Proposed agent ownership: `GymScreens.kt`, `core/TrackedGymRecords.kt`, narrowly related `GymViewModel.kt` presentation helpers if needed, `SettingsScreens.kt`, `SettingsViewModel.kt`, `data/BackupRepository.kt`, focused JVM/native tests and this report. Parent must approve these groups before production changes. Avoid `WhipApp.kt` and shared controls. The first implementation chunk can coherently cover PGS-1–4/6 (saved Gym evidence); Settings retry and additive CSV context are subsequent independently reviewable changes.

Use existing codecs, capability helpers, Settings request receipts and unsaved-draft dialog. No dependency, schema, portable-backup format, epoch or release version change is proposed. The Gym CSV retains the existing column order, uses the saved workout tracking type in its existing `trackingType` column, and appends `workoutState`, `workoutArchived`, `workoutExerciseOutcome`, and `setCompleted`. No portable-backup field changes. The user explicitly restricts verification to exact bounded JVM/native checks; no affected readiness batch or candidate. Until granted a serialized build slot, this agent runs no Gradle or emulator checks. Every routine check stays under `timeout --kill-after=3s 55s`; timeout is incomplete, never passed and not repeatedly retried.

Planned focused evidence: `TrackedGymRecordsTest` plus a new exact native tracked-record method; `GymProgressScopeTest`/common formatter test plus native archived-history/unit-change evidence; `BackupRepositoryTest` exact CSV method; reminder reconciliation helper tests plus native Settings refresh feedback. Existing same-day chart source, reverse equipment, active Rest, quiet-hours, password/picker and backup roundtrip methods remain useful neighbors. Prefer a small number of meaningful combined journeys over implementation-mirroring unit tests.

Remaining limits at this checkpoint: no fresh executed JVM/native tests, no fresh native large-text/pixel inspection, no owner-phone changes, no claim to exhaust all Android devices/OEM notification policies. The source findings above are not promoted to Verified until their appropriate checks run.
