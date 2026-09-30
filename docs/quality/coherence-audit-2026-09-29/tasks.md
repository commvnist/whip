# Tasks coherence and UX audit — 2026-09-29

Baseline: `6d79bd1f`. Related request: FB-20260929-017. Scope: Tasks, its authoring/execution/history/planning routes, and the shared owners directly used by those routes. This is an audit, with **60 workflow dispositions, five confirmed source findings, eight inspected historical PNG originals, three historical XML originals, and three parent-captured fresh PNG originals inspected**. No production/test change, build, instrumentation, or fresh screenshot capture was performed by this reviewer. The coordinating parent owns fresh runtime follow-through; TASK-01 dismissal and TASK-03 were reproduced by the parent on the current 0.3.90-debug build.

The main Tasks structure is coherent: definitions live in Tasks, dated execution lives in Today, and completion receipts live in History. Retained normal/200% originals support the shared capture anchor and readable primary card identity. The remaining actionable defects occur at transitions: unfinished subtask authoring, recurring planning projections, calendar navigation, and completed-record inspection. Previous broad audit passes did not exercise these exact paths and do not certify them.

## Evidence boundaries and read map

Read `AGENTS.md`, the maintain-whip-memory skill, the complete product-memory index, and current Task-related feedback/findings/decisions/implementation/verification entries. Relevant records include FB-20260929-005/008/010/011/013/014/016/017, FB-20260928-008, FND-20260929-003/013/022/025/026/027/030/033, DEC-20260929-002/003/004 and VER-20260929-012/014/015/017. Also reviewed the workspace navigation plan, fresh productivity workbook/overhaul and workspace anchors plan. These are the map of intended contracts; findings below come from current source.

Evidence notation in the coverage table: **S** = current production source traced; **T** = existing test source inspected, not executed; **P** = an existing original PNG inspected directly with `view_image`; **X** = existing XML parsed/inspected. “Keep” means no grounded remedy was found in that reviewed path; it is not a fresh device pass. “Gap” marks behavior needing device or platform evidence. Line references refer to the frozen baseline.

Principal source owners: `app/src/main/java/com/whip/app/ui/WhipApp.kt` (`TaskAreaContent` at 5786), `TaskEditorDialog.kt`, `TaskComponents.kt`, `TaskWorkspacePolicy.kt`, `TaskViewModel.kt`, `TaskEditorRouteHost.kt`, `TaskNavigationIndex.kt`; `app/src/main/java/com/whip/app/data/TaskRepository.kt`. Paths in the coverage table are relative to these UI/data directories unless stated otherwise.

## Confirmed findings

### TASK-01 — Pending New Subtask text is silently omitted from Save and dirty dismissal (P2)

**Evidence:** `TaskEditorDialog.kt:292` stores `newStepTitle` separately. The New Subtask field writes only that state at **1114–1117**; only the separate Add icon transfers it into `stepDrafts` at **1120–1130**. The dirty comparison at **321–347** checks `stepDrafts` and other authored fields but omits `newStepTitle`. The saved draft at **394–401** also takes only `stepDrafts`. Header Save at **517–521** saves `currentDraft` with no pending-subtask reconciliation. The parent freshly confirmed the dismissal branch on 0.3.90-debug: edit unchanged Audit Task, type Unsaved audit step, hide IME, Cancel Task editing exits without discard confirmation. [Pending text](../../../artifacts/coherence-audit/2026-09-29/fresh/manual/tasks.pending-subtask.png) and [dismissed editor](../../../artifacts/coherence-audit/2026-09-29/fresh/manual/tasks.pending-subtask-dismissed.png), with paired XML, were captured by the parent and directly inspected by this reviewer. Save omission remains source-confirmed, not freshly executed.

**Reproduction:** Open an existing unchanged Task, reveal Estimates, Subtasks, Notes & Tags, type a meaningful value into New Subtask, and press the editor Close control. The editor considers this unchanged and can dismiss without its discard confirmation. Alternatively press Save: the title/Task save succeeds, but this typed subtask is excluded. Reopening the Task cannot recover the text. This applies to Save & New as well.

**Impact:** Authoring a field inside the Task editor has a different save/discard contract from the surrounding fields. The parent Save action appears to accept all authored information, while one visible text value is lost unless the user knows to press an additional icon first.

**Concrete remedy:** Include pending nonblank New Subtask text in dirty state. On Save/Save & New, flush the pending subtask into the composed draft using the existing trimmed step construction, or require an explicit add/discard decision while preserving the text. Do not silently clear it. Verify unchanged-existing-Task Close, Save, Save & New and recreation with pending text. Existing `EditorDependencyUxTest#repeatSettingsStayBetweenScheduleChoiceAndPlanningFields` reveals this section but does not assert pending text; existing editor E2E tests rename only.

### TASK-02 — All Tasks Agenda/Calendar omit today's schedule-anchored recurring occurrence (P2)

**Evidence:** `WhipApp.kt:6059–6061` sources All Tasks List from `collectionTasks`, but All Tasks Agenda/Calendar from `state.planning`. Its Calendar window explicitly includes today at **6066**. `TaskViewModel.kt:1693–1703` computes the due recurring candidate and adds it only to `todayItems`. The schedule-anchored recurring branch adds only `futureByOriginal.values` to `planningItems` at **1747**. One-off Tasks add their current item to planning at **1592**, and completion-anchored recurrences add due items at **1641–1642**, so the omission is specific and inconsistent. `forPlanningView` at `WhipApp.kt:8025–8033` cannot recover an item missing from the source.

**Reproduction:** Create a daily recurrence anchored to Schedule starting today. Confirm today's occurrence in Today. Open Tasks → All Tasks → Agenda, then Calendar and select today. The definition is still in List, but today's occurrence is absent from both dated layouts. With an unrelated one-off today, that one-off remains visible, making the omission particularly misleading. A carried-over schedule-anchored occurrence has the same source asymmetry.

**Impact:** A view presenting scheduled occurrences gives an incomplete plan for the current date and treats otherwise valid recurrence anchors differently. This is not the intentional distinction between one row per definition and many dated occurrences.

**Concrete remedy:** Include the due candidate in the dated planning projection (or explicitly merge Today and planning at the planning-view owner), deduplicate by `stableKey`, and retain the selected missed-occurrence policy. Calendar should still enforce its window; Agenda should retain its deliberate older-date behavior. Verify Schedule versus Completion anchors, today versus carried-over, and explicit moved Open occurrences. Existing `TaskUpcomingVisibilityTest` covers projection policies but does not assert current due recurring membership in `planning`; retained calendar captures predate All Tasks Calendar.

### TASK-03 — Calendar's Tomorrow shortcut selects today in All Tasks (P3)

**Evidence:** `TaskMonthPlanner` renders a button named Tomorrow at `WhipApp.kt:7831`. Its caller supplies `onStart = { selectedDate = planningWindow.start; ... }` at **6748**. All Tasks sets that start to `state.currentDate` at **6066**, whereas Upcoming starts tomorrow.

**Reproduction:** Tasks → All Tasks → Calendar, select a later date, then tap Tomorrow. Selected date becomes today. The parent freshly reproduced this on 0.3.90-debug with logical today September 29, 2026; [the original](../../../artifacts/coherence-audit/2026-09-29/fresh/manual/tasks.tomorrow-points-today.png) and paired XML show Tomorrow alongside Selected: Sep 29, 2026. This reviewer inspected that original directly.

**Impact:** The shortcut announces a different day from the one it selects, including to accessibility users. It also makes the same calendar control behave differently across scopes.

**Concrete remedy:** Either pass the actual tomorrow date for a consistently named Tomorrow action, or name a deliberate window-start action Today/Start of Window according to its target. Keep month and date selection synchronized. A small scope-specific behavior test should assert displayed name and selected epoch day together.

### TASK-04 — Editing a completed Task drops the inspector and its History context (P2)

**Evidence:** The active Task inspector's Edit handler at `WhipApp.kt:2607–2609` calls `openTaskEditor(item)` while retaining `actionItemKey`. The completed inspector's analogous handler at **2738–2740** opens the editor then clears `completedItemKey`. `openTaskEditor` at **1166–1194** and `closeTaskEditor` at **1195–1208** retain no separate completed-inspector return route. `CompletedTaskDialog` has a saved selected section at `TaskComponents.kt:670` and the shared `EntityInspector` saves section content at `EntityInspector.kt:171–173`; clearing the owning route removes that useful context.

**Reproduction:** History → a completed recurring Task → Activity → Show More as needed → Edit Series. Cancel editing, or save a title-only change that keeps it completed. The user returns to the Task list, instead of the same completed inspector/Activity view. Active Task editing preserves its parent route, so the equivalent job has inconsistent return behavior.

**Impact:** Repeated historical inspection and correction requires finding/opening the receipt, switching sections and loading older series records again. This is analogous to the explicit FB-20260928-008 continuity requirement, without a lifecycle change that makes the parent unavailable.

**Concrete remedy:** Keep the completed inspector route behind its editor, or use the existing stable Task/occurrence return owner. Clear it only when the record is actually reopened, deleted, or otherwise unavailable. Verify Cancel, successful rename, save failure, recreation and Activity's loaded page/scroll. Existing `TaskBulkSelectionUiTest#successfulReopenClosesCompletedInspectorWhenStableKeyBecomesOpen` is relevant to the intentional lifecycle exit, not proof of edit-return continuity.

### TASK-05 — Completed history hides the named subtask evidence it preserves (P2)

**Evidence:** The unfinished-subtask completion confirmation promises that saved history keeps progress at `WhipApp.kt:2714–2715`. `TaskViewModel.kt:1487–1512` reconstructs the exact completed snapshots, including each original title, notes, completion flag and timestamp. Active `TaskActionsDialog` displays the individual subtask rows at `TaskComponents.kt:433–450`. The completed inspector only displays a total (`Subtasks: n of m complete`) at **724–726**. Its entire Overview/Activity/More switch at **703–758** has no named subtask list; expanded `TaskRow` supplies a progress bar/count, not snapshot rows, at **242–259**.

**Reproduction:** Create a Task with two named subtasks, complete one, then complete the parent with Complete Anyway. Open the completed receipt. It says 1 of 2 complete, but cannot identify the unfinished step or show its saved notes. Editing the Task shows the current definition, which is especially insufficient after subsequent recurring-series changes; it is not an inspection of the historical snapshot.

**Impact:** The app stores richer trustworthy evidence than its History UI lets the user inspect. Users cannot audit what was left unfinished or compare the saved completion against a later definition without reopening work merely to inspect it.

**Concrete remedy:** Add a read-only completed-subtask checklist to Outcome/Overview using `item.subtasks` snapshot titles/notes/completion state. Preserve historical meaning; do not expose live completion toggles in a receipt. Test Complete Anyway, all-complete, Carry Unfinished, renamed/removed step definitions, and 200% scrolling. Existing `TaskRepositoryTest#manualCompletionSnapshotsNamesAndUnfinishedProgress` covers stored data, not its visible inspection.

## Workflow coverage

| # | User job / state | Current source symbol and line | Evidence | Verdict |
|---|---|---|---|---|
| 01 | First entry and Tasks/Today/History tabs | `TaskWorkspacePolicy.kt:42`, `WhipApp.kt:944,6339` | S,T `WhipNavigationTest` | Keep collection-first and three jobs; chosen routes restore. |
| 02 | Complete unfinished collection, far-future series | `TaskWorkspacePolicy.kt:210 collectionTasks` | S,T `TaskWorkspacePolicyTest:64` | Keep definition-backed collection; avoids projection-window loss. |
| 03 | Empty older-series remnants and custom icons | `TaskWorkspacePolicy.kt:216–232`, `TaskRepository.kt:153–160` | S,T `TaskWorkspacePolicyTest:23` | Keep evidence-aware suppression; legitimate authored records survive. |
| 04 | All/Unscheduled/Upcoming scope selection | `WhipApp.kt:6667–6691 navigateTask` | S | Keep explicit scope selector and destination normalization. |
| 05 | Today execution and carried-over grouping | `TaskViewModel.kt:1583–1593,1693–1703`; `WhipApp.kt:6834` | S,P | Keep schedule versus deadline distinction and group headings. |
| 06 | Completed receipt ordering | `TaskViewModel.kt:1549–1573,1774` | S,T `TaskCollectionPresentationTest` | Keep actual completion timestamps, not original schedule sorting. |
| 07 | Archive discovery, Back and restored selection | `WhipApp.kt:6173–6175,6382–6385` | S,T `WhipNavigationTest`, `ReviewOutcomeJourneyE2ETest` | Keep child archive and source-aware Back; no extra primary tab. |
| 08 | Load/error/retry state | `WhipApp.kt:5849–5868 TaskAreaContent` | S | Keep DomainLoadContent and visible navigation; fresh error rendering gap. |
| 09 | Empty/filtered-empty orientation | `WhipApp.kt:6791–6805,7983 EmptyTasks` | S,P | Keep truthful No Matching Tasks; 200% state requires ordinary scrolling. |
| 10 | Normal Tasks/Today quick-capture anchor | `WhipApp.kt:6580–6659` | S,P,X | Keep capture first below pinned context; optional scope/tools follow. |
| 11 | Enlarged capture and restored filter badge | `WhipApp.kt:6364–6369,6581`; `WhipWorkspaceHeader.kt:27` | S,P,X,T `TaskCaptureGeometryUiTest` | Keep enlarged dot/full spoken count; inspected XML has no unnamed interactive node. |
| 12 | Quick capture default schedule/Area | `WhipApp.kt:6257–6283`; `TaskWorkspacePolicy.kt:171` | S,T `ProductivityDefaultsUiTest:123` | Keep Today date and explicit creation Area rules. |
| 13 | Quick capture submit/failure/reconnect | `WhipApp.kt:5957–5973,6269–6282` | S,T `TaskBulkSelectionUiTest:378` | Keep request-bound draft clearing only on matching persisted receipt. |
| 14 | Smart capture highlighted interpretation | `WhipApp.kt:5974–5987,6624`; `TaskEditorDialog.kt:255–264` | S,T `EditorDependencyUxTest:116` | Keep preview explains inline automatic apply versus editor explicit apply. |
| 15 | Add Details continuation | `WhipApp.kt:6641–6653`; `TaskEditorDialog.kt:188–205` | S,T `ProductivityCreationJourneyE2ETest` | Keep carried text and multiline step initialization. |
| 16 | New Task/editor keyboard and header Save | `TaskEditorDialog.kt:443–533` | S,P,T `TaskEditorJourneyE2ETest` | Keep system/IME padding, focused new title and fixed header; fresh lower-field gap. |
| 17 | Existing title/emoji editing and revision boundary | `TaskEditorDialog.kt:211–213,570–624`; `TaskRepository.kt:241` | S,T `TaskBulkSelectionUiTest:432` | Keep stable identity and stale-edit protection. |
| 18 | Parent dirty discard guard | `TaskEditorDialog.kt:322–349` | S,T | TASK-01: pending New Subtask is omitted despite other fields being guarded. |
| 19 | Save & New | `TaskEditorDialog.kt:494–510`; `TaskEditorRouteHost.kt:47` | S | TASK-01 also applies; keep busy protection and intent ownership. |
| 20 | Unscheduled ↔ scheduled conversion | `TaskEditorDialog.kt:418–428,684–706,1250` | S,T `EditorDependencyUxTest:392` | Keep explicit consequential conversion; hidden reminders/deadline cleared. |
| 21 | Schedule date and separate deadline | `TaskEditorDialog.kt:727–760,360–369` | S,T `TaskWorkspacePolicyTest` | Keep start/deadline distinction and validation. |
| 22 | Repeat activation from Unscheduled | `TaskEditorDialog.kt:711–723,1278` | S,T `EditorDependencyUxTest:270` | Keep explicit activation consequence and direct recurrence controls. |
| 23 | Repeat cadence, weekdays, end date/count | `TaskEditorDialog.kt:763–910` | S,T `EditorDependencyUxTest:270` | Keep visible dependent settings and invalid-state explanation. |
| 24 | Schedule/Completion anchors | `TaskEditorDialog.kt:817–852,1359`; `TaskViewModel.kt:1601` | S,T `TaskWorkspacePolicyTest` | Keep bounded anchor choices; TASK-02 exposes projection mismatch. |
| 25 | Missed-occurrence policy | `TaskEditorDialog.kt:828–853`; `TaskViewModel.kt:1688–1702` | S,T `TaskUpcomingVisibilityTest` | Keep truthful oldest/latest/current-only behavior without fabricated skips. |
| 26 | This-and-future versus entire-series edit | `WhipApp.kt:1179–1187`; `TaskRepository.kt:130–257` | S,T `TaskRepositoryTest:360,414` | Keep current boundary validation and history-preserving split. |
| 27 | Time/reminder/custom-offset authoring | `TaskEditorDialog.kt:924–948,1397 TaskTimeSettings` | S,T `EditorDependencyUxTest` | Keep dependent Time controls and explicit reminder enable; platform permission gap. |
| 28 | Priority and effort meaning | `TaskEditorDialog.kt:979–993,1016–1030` | S,T | Keep distinct authored fields, human-readable labels and energy explanation. |
| 29 | Duration and rejected save | `TaskEditorDialog.kt:362–374,1002–1013`; `TaskDayPlanDialog.kt:53` | S,T `TaskDayPlannerUiTest` | Keep valid 1–1440 minutes and named unknown 30-minute assumption. |
| 30 | Area selection/create/inherited scope | `TaskEditorDialog.kt:951–976`; `TaskEditorRouteHost.kt:72–83` | S,T `TaskBulkSelectionUiTest:352` | Keep explicit Area when needed; archive/missing Area device gap. |
| 31 | Notes/tags authoring and suggestions | `TaskEditorDialog.kt:1166–1207`; `TaskEditorRouteHost.kt:84` | S | Keep optional disclosure and active-tag suggestions; Unicode/long tag rendering gap. |
| 32 | Existing subtask title/notes/reorder/remove | `TaskEditorDialog.kt:1036–1107` | S,T `TaskProgressTest` | Keep stable step keys and accessible existing reorder owner. |
| 33 | Add pending subtask, save and discard | `TaskEditorDialog.kt:1110–1133,322–346,398` | S | TASK-01; entered field excluded until extra Add icon. |
| 34 | Subtask progress/auto-complete/repeat carry | `TaskEditorDialog.kt:911–923,1136–1164`; `TaskRepository.kt:356` | S,T `TaskRepositoryTest:96,162` | Keep independently explained settings and per-occurrence data. |
| 35 | Template application | `TaskEditorDialog.kt:1290–1345,1494 TaskRecipeDialog` | S | Keep new-task-only templates and explicit selection; fresh interruption gap. |
| 36 | Shared/widget draft and pending launch | `TaskEditorRouteHost.kt:31–46,99`; `core/SharedTaskCapturePolicy.kt:20` | S,T `PlatformEntrySurfaceE2ETest` | Keep bounded shares, visible shortening and queued-draft protection. |
| 37 | Card collapsed/expanded hierarchy | `TaskComponents.kt:109–259,852 collectionSummary` | S,P,T `ProductivityCardDesignUiTest` | Keep concise timing/priority, rich expanded details and authored icon. |
| 38 | Direct completion versus row/edit navigation | `WhipApp.kt:7716–7735`; `TaskComponents.kt:131–168` | S,T `TaskDeletionUiTest:48` | Keep definition-series guard and separate Edit; spoken series-open wording gap. |
| 39 | Active inspector overview checklist/Focus | `TaskComponents.kt:433–475` | S,T `EntityInspectorUiTest` | Keep execution-first checklist and explicit 15/30/45/60/custom controls. |
| 40 | Focus custom/replacement/failure | `TaskComponents.kt:573–626`; `WhipApp.kt:2600–2651` | S,T `FocusTimeSelectionUiTest` | Keep target identity, bounded custom value and request shielding; background timer platform gap. |
| 41 | Complete with unfinished subtasks | `WhipApp.kt:1454 requestCompletion,2708–2730`; `TaskRepository.kt:1066` | S,T `TaskRepositoryTest:116` | Keep confirmation/snapshots; TASK-05 blocks named receipt inspection. |
| 42 | Toggle/promote subtask and undo | `TaskComponents.kt:265–344,631`; `TaskViewModel.kt:493–541` | S,T `TaskRepositoryTest:224` | Keep owned conversion confirmation and revision-aware undo. |
| 43 | Completed outcome inspection | `TaskComponents.kt:659–762`; `TaskViewModel.kt:1487–1512` | S,T | TASK-05: exact stored named checklist unavailable to read. |
| 44 | Completed receipt edit return | `WhipApp.kt:2738–2740`; `TaskComponents.kt:671` | S | TASK-04: receipt owner cleared before edit outcome. |
| 45 | Reopen completed Task/occurrence | `TaskComponents.kt:695–700`; `TaskViewModel.kt:741–762` | S,T `TaskBulkSelectionUiTest:168` | Keep relevant reopen action and intentional unavailable-receipt exit. |
| 46 | Skip/move/reset occurrence | `TaskComponents.kt:499–513,825–831`; `TaskViewModel.kt:443–490,765` | S,T `TaskRepositoryTest:293` | Keep original identity, explicit moved label and reset-date action. |
| 47 | Series history paging and factual labels | `TaskComponents.kt:764–849` | S,T `TaskDeletionUiTest:317` | Keep 20-item paging and truthful Open/Moved/Skipped distinctions. |
| 48 | Duplicate/pin/unpin | `TaskComponents.kt:536–557`; `TaskRepository.kt:532` | S,T `TaskRepositoryTest` | Keep duplicate-as-unscheduled and clear due-Home pin semantics. |
| 49 | Archive/Restore | `TaskComponents.kt:416–420,558`; `TaskViewModel.kt:545–562` | S,T `TaskDeletionUiTest:265` | Keep direct archived Restore and archive reversibility. |
| 50 | Permanent delete/impact/failure | `TaskComponents.kt:906–969`; `WhipApp.kt:3475 TaskPermanentDeleteRoute` | S,T `TaskDeletionUiTest:144` | Keep second confirmation, exact impact, series-wide wording and local retry. |
| 51 | Completion/move/archive/quick-add undo | `TaskViewModel.kt:423–439,1039–1095` | S,T `TaskRepositoryTest:822,931` | Keep current-snapshot checks; undo across process death remains a stated gap. |
| 52 | Current-list search and filter apply/cancel | `WhipApp.kt:6933–7120,7637 matches` | S,T `TaskCaptureGeometryUiTest` restored-filter fixture | Keep draft isolation, result count and visible applied criteria. |
| 53 | Sort/group/priority/tags/date/effort/duration criteria | `WhipApp.kt:6970–7101`; `TaskWorkspacePolicy.kt:27,115,127` | S,T `TaskWorkspacePolicyTest` | Keep Today narrowing and completion-date filters; invalid zero maximum has no explanation (minor gap). |
| 54 | Saved Views save/apply/delete/legacy normalization | `WhipApp.kt:6176–6186,7124–7157`; `TaskWorkspacePolicy.kt:107` | S,T `PowerUserSettingsTest` | Keep explicit view destination/Area/layout and preserved capitalization. |
| 55 | Global search / deep link / keyboard route | `WhipApp.kt:2542–2558,1296–1299,1587`; `TaskNavigationIndex.kt:6` | S,T `GlobalSearchRoutingTest`, `PlatformEntrySurfaceE2ETest` | Keep identity-aware unscoped navigation; current physical keyboard/TalkBack gap. |
| 56 | Agenda/Calendar and unscheduled continuation | `WhipApp.kt:6059–6066,6702–6705,6811–6831,7803` | S,P | TASK-02 and TASK-03; keep explicit projection limits and Unscheduled route. |
| 57 | Include Habits in dated planning | `WhipApp.kt:6085–6095,6723–6729,6752–6776,7675 plannedOn` | S,T | Keep separate labeled Habit evidence; recurrence-count/calendar large-history gap. |
| 58 | Plan My Day preview/capacity/apply/recreation | `TaskDayPlanDialog.kt:36–181`; `TaskViewModel.kt:846–875,1431` | S,T `TaskDayPlannerUiTest` | Keep all-Area capacity, unknown estimates, preview checkbox selection, transaction revalidation and retry. |
| 59 | Multi-select bulk actions/frozen target/hidden selection | `WhipApp.kt:6402–6559,7160–7406,7513` | S,T `TaskBulkSelectionUiTest` | Keep relevant actions and frozen revision guards; disabled mixed-series completion needs fresh discoverability assessment. |
| 60 | Manual reorder, Back/recreation, wide/200% access | `WhipApp.kt:6136–6166,6303–6322,7738`; `TaskViewModel.kt:1004`; `TaskEditorDialog.kt:453` | S,T `TaskReorderJourneyE2ETest`, P historical wide | Keep unconstrained all-Area reorder and pin partitions; fresh wide/200% calendar, receipt and process-recovery evidence outstanding. |

## Inspected original evidence

All eight images below were inspected individually, not inferred from a contact sheet. Their dates and old source are retained; the September 10 wide image and September 27 calendar have retired navigation and are explicitly **not evidence of current root geometry**.

| Existing original | Observation and limitation |
|---|---|
| [Tasks normal](../../../artifacts/workspace-anchors/2026-09-29/accepted/task-capture.tasks.normal.png) | Clear tabs/count/capture/scope/card order, anchored quick entry, readable ordinary cards. Draft present; not idle-state proof. |
| [Today actual 200%](../../../artifacts/workspace-anchors/2026-09-29/accepted/task-capture.today.native200.png) | Count wraps truthfully, full capture and Add Details visible, titles wrap beside disclosure/checkbox, two-row bottom navigation. |
| [Restored actual 200%](../../../artifacts/workspace-anchors/2026-09-29/accepted/task-capture.restored.native200.png) | Full Pinned/query criteria and bounded badge dot; secondary content naturally pushes results below first viewport. Scrollability remains essential. |
| [Filtered empty actual 200%](../../../artifacts/workspace-anchors/2026-09-29/accepted/task-capture.filtered-empty.native200.png) | Retained draft/action and truthful zero count; No Matching Tasks begins below viewport, not evidence of clipping after scroll. |
| [Existing Task input actual 200%](../../../artifacts/reusable-architecture-ux/2026-09-26/final/tasks.editor.existing-input.large.png) | Light editor title/Save and full Task field above keyboard. Historical focused-title state only; lower subtask input not covered. |
| [More menu normal](../../../artifacts/tasks-ux/2026-09-29/visual/tasks.actions.png) | Clear Plan My Day/Archive/Saved Views/Save/Reorder/Select list. Predates final capture-anchor movement; menu vocabulary remains current in source. |
| [Calendar actual 200%, September 27](../../../artifacts/ux-overhaul/2026-09-27/after/ux-upgrades.tasks-calendar-dense.large.png) | Dense day/count presentation remains readable in this scrolled original; retired Upcoming root chrome. Does not show current All Tasks date selection. |
| [Wide Today, September 10](../../../artifacts/ui-alignment/2026-09-10/page-headers/wide/tasks.today.populated.png) | Large-width constrained main content and left task context existed; old tabs/header/capture wording. Current-wide acceptance still requires fresh capture. |

XML inspected: `workspace-anchors/.../accepted/task-capture.tasks.normal.xml`, `task-capture.tasks.native200.xml`, and `tasks-ux/.../visual/tasks.actions.xml`. The first two expose owned Filter/Complete task names and contained bounds; parsing found zero clickable nodes lacking their own or descendant text/content-description. The third is the popup window hierarchy. This establishes only these retained hierarchies, not current TalkBack speech/order or every interaction.

## Justified keeps and remaining verification

- Keep the existing Tasks/Today/History division, child Archive route, collection-first initial selection, one root search entry and scrolling scope/layout controls. These match explicit owner feedback and avoid adding competing chrome to solve transition-specific defects.
- Keep quick capture ahead of optional context, request-bound persistence feedback and visible highlighted parsing assumptions. Saved view/filter content can grow; do not pin extra rows above every screen to eliminate ordinary scrolling.
- Keep concise card summaries, preserved full expanded metadata, distinct deadline versus past-schedule notices, and natural 200% title wrapping. Retained originals provide no grounded reason to redesign their shape/colors/spacing again.
- Keep finite planning limits, explicit excluded-unscheduled continuation, recurring-definition completion guards, pin partitions in reorder, hidden-selection warnings and frozen target revision checks. Fix the missing due recurring projection without changing legitimate authored/history policies.
- Keep archived Restore as the primary next action, explicit whole-series deletion impact, truthful Open/Moved history labels and immutable historical snapshots. TASK-05 asks to expose existing evidence, not mutate it.

Fresh device gaps are substantive: current-wide layout, actual-200% dense Calendar/Agenda and historical subtask receipt, existing subtask field with keyboard, completed-editor return/recreation, configuration versus cold-process recovery, physical keyboard/IME Back sequencing, and TalkBack focus/spoken state. Current test source was reviewed for targeted journeys, but no test was executed by this reviewer and no old report was counted as fresh verification. Parent fresh manual reproduction confirms TASK-01 dismissal and TASK-03 as described above; remaining new paths retain their source-only evidence status. Retained source tests are strongest for repository integrity and request ownership; none asserts the five new findings exactly.

Additional source-only assessment to resolve on device: mixed one-off/series selection disables Complete and date moves without explaining which selected series causes the restriction (`WhipApp.kt:6078,6462,6501`); duration filter accepts 0 even though authorable duration begins at 1 (`WhipApp.kt:7091`); Calendar's series-wide final deadline can display several distinct occurrences under the same due date (`WhipApp.kt:7820,8032`). These are uncertainties/discoverability questions, not additional confirmed defects or stylistic redesign requests.

Recommended parent fresh order: pending subtask Close/Save; daily schedule-anchored recurrence Today versus All Calendar today; Calendar Tomorrow target; completed recurring Activity → Edit → Cancel; incomplete-subtask completed receipt. Follow with 200%/wide accessibility for whichever remedies are accepted. No broad verification or release is implied by this audit.
