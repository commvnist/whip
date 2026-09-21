# Current-source surface disposition — working audit register

This register continues FB-20260920-001 without rewriting the frozen [534-row September 8 matrix](astra-surface-review-2026-09-08.tsv). Its review denominator is 181 rows: 160 frozen unfinished IDs that still exist in the [current UI catalog](ui-surface-catalog.tsv), 14 post-freeze current IDs, and seven retired/replaced 5/3/1 IDs whose exact disposition is in [the retirement record](ASTRA_GYM_RETIREMENT_DISPOSITION_2026-09-21.md). Previously Verified rows keep their historical evidence and still participate in the final whole-product campaign.

Status here is **working review**, not whole-product acceptance. The full 523-card current-source capture and final app/platform gate must pass before these visual observations become final evidence. `build/astra-full-current-20260921-v2` supplied diagnostic originals for this review; it executed every selector but was rejected for six redundant visual aliases. The later source corrected those aliases and four misleading generic Gym fixtures. Exact final manifest IDs/hashes and source/journey results will be reconciled in this register after the fresh capture. A catalog screenshot demonstrates a rendered state, not persistence or recovery by itself.

## Tasks — 26 frozen unfinished IDs

Source boundary: `WhipApp` Task workspace, `TaskEditorDialog`/Task components, `TaskViewModel` and the Task repository/recurrence rules. Native journey owners include `ProductivityCreationJourneyE2ETest`, `TaskEditorJourneyE2ETest`, `TaskBulkSelectionUiTest`, `TaskReorderJourneyE2ETest`, `TaskDeletionUiTest`, `PlatformEntrySurfaceE2ETest` and `NotificationDeepLinkE2ETest`; the fresh whole-app gate must execute them on final source. The diagnostic originals below were individually inspected at their declared ordinary or actual enlarged scale. Full semantic/visual acceptance remains provisional until the final exact capture.

| Frozen ID | Current-source/journey disposition and rendered observation |
| --- | --- |
| `tasks.filter` | Workspace filter owns search, sort, group, priority and scheduled-date choices with Reset/Done; the applied chip is visible in `tasks.today.filtered`. Filter and recurrence behavior belongs to the Task workspace/repository journey, not the dialog image alone. |
| `tasks.today.filtered` | The `Query: Plan` chip and Clear All appear above the matching Task while the normal Today action/header remain. No evidence of a hidden filter is visible in the diagnostic original. |
| `tasks.today.empty` | A real Task can coexist with the truthful `Today Is Clear` state when none is due; Quick Capture and global navigation remain. The persisted due/recurrence classifier is covered separately by Task rules. |
| `tasks.inbox.populated` | Inbox separates Quick Capture, Plan My Day and an unscheduled Task; the card retains its schedule context. Actual planning changes need the native Task journey. |
| `tasks.upcoming.list` | Two scheduled Tasks show distinct dates, repeat metadata and completion actions under List. |
| `tasks.upcoming.agenda` | Agenda groups the same Tasks under complete dated headings; Include Habits is an explicit optional overlay, not an implied Task count. |
| `tasks.upcoming.calendar` | Month grid marks days with Tasks and names the selected date; the diagnostic selected day correctly shows `No Matching Tasks` when that day has none. List/Agenda/Calendar share persisted Task ownership. |
| `tasks.history.completed` | Completed history is selected and its completed Task is struck through but remains expandable. Completion/reopen persistence belongs to Task mutation/deletion journeys. |
| `tasks.history.archived` | Archived is separately selected and exposes the saved Task for restoration; history is not conflated with completed occurrences. |
| `tasks.bulk-archive.single-large` | Actual 200% text retains one selected Task, impact meaning and both Cancel/Archive actions within the bounded dialog. |
| `tasks.bulk-archive.large` | The long eight-Task impact list scrolls while Cancel and Archive 8 remain fixed and legible at actual 200%. |
| `tasks.templates.extreme` | At the declared extreme scale/short window, the last recipe and Cancel remain reachable after scrolling; the visible viewport is intentionally narrow. |
| `tasks.bulk-selection` | At enlarged text, the selected-count/action lane, item checkbox and responsive two-row navigation remain in view; overflow carries secondary actions. Request-owned mutation/selection recovery is verified by the owning UI journey, not this card alone. |
| `tasks.editor.create` | The native form prioritizes required name/emoji before schedule; Save is fixed and the keyboard leaves the active title visible. Real creation/persistence is covered by the cross-product creation journey. |
| `tasks.editor.edit` | The saved title, emoji, scheduled date, repeat/deadline/time fields and Save remain readable; exact edit/recreation/reopen is owned by `TaskEditorJourneyE2ETest`. |
| `tasks.recipe` | Four Task templates have distinct meanings and the Cancel action remains separate; selecting a template is an editable draft, not an automatic save. |
| `tasks.repeat-change` | A changed title remains in the editor while repeat controls are visible; recurrence series/occurrence semantics are repository-owned and require their native tests. |
| `tasks.actions` | Inspector overview exposes timing, notes, Activity/Options and a docked Complete Task action; status and action owner are clear. |
| `tasks.completed-detail` | A completed Task shows its outcome and preserved context with docked Reopen Task, not a duplicate complete action. |
| `tasks.reschedule` | Calendar shows the selected date, month navigation and explicit Set/Cancel over the Task inspector; the original Task remains visible behind the review. |
| `tasks.permanent-delete` | The ordinary dialog names the exact Task, occurrence/subtask impact, irreversible outcome and backup advice before Delete Permanently. Exact deletion/history integrity is covered separately. |
| `tasks.bulk-edit` | The enlarged form states that only enabled fields change and distinguishes series from occurrence scope; the body scrolls above fixed Cancel/Apply. |
| `tasks.batch-delete` | The enlarged single-selection review retains counts, backup advice and separate Cancel/Delete actions without clipping. |
| `tasks.pending-editor-launch` | A queued external Task request presents Keep Editing versus Replace Open Draft instead of silently discarding unsaved work. Queue/recreation and OS-process-death journeys provide behavioral evidence; overflow under process death remains a separate gap. |
| `tasks.row.menu` | Reorder Tasks and Select Tasks appear as an anchored workspace menu without obscuring Task identity; the reorder journey tests saved ordering. |
| `tasks.area-scope.menu` | All Areas and named Area counts are visible in a scoped popup while the current Task workspace remains behind it. Area ownership and cross-domain moves are separate Organization journeys. |

## Shared shell and primitives — 20 frozen unfinished IDs

Source boundary: `WhipApp`/`MainActivity` shell and `LaunchRequestQueue`, shared pickers, inspector, disclosure and destructive-review components. The exact catalog selectors are in `ui-surface-catalog.tsv`; behavioral neighbors include `PlatformEntrySurfaceE2ETest`, `GlobalSearchRoutingTest`, `EditorDependencyUxTest`, `InteractionControlUiTest`, `DataEpochBoundaryTest` and `PersistentStorageE2ETest`. The v2 diagnostic originals were inspected at their declared scale; these are visual observations pending final-source capture.

| Frozen ID | Current-source/journey disposition and rendered observation |
| --- | --- |
| `shared.clock-picker` | The reminder-time dialog shows numeric time, analog dial, AM/PM, Cancel and Add without conflating time choice with scheduling; reminder rules and Settings cause/effect own the persisted result. |
| `shared.color-picker` | Preset swatches, exact custom hex and hue controls appear in one reversible choice; Apply is separate from Cancel. |
| `shared.date-picker` | Calendar displays selected date/month and Set/Cancel; date conversion and saved domain fields are covered by their owning editors. |
| `shared.date-picker.large` | Actual 200% text uses three reachable year/month/day wheels with full-height Cancel/Set; month abbreviation is a deliberate wheel constraint, with full selected date above. |
| `shared.disclosure.menu` | Today/Upcoming/History appears anchored to the View control with the current choice marked, not a hidden route. |
| `shared.domain.failure` | Load failure explicitly says saved data is unchanged and offers Try Again; repository failure/refresh paths provide behavioral ownership. |
| `shared.domain.loading` | Loading state names the domain and does not counterfeit an empty saved collection. |
| `shared.first-run.welcome-large` | At actual enlarged text the privacy/account promise and both Customize/Use Recommended actions remain in the onboarding scroll; setup persistence has separate native coverage. |
| `shared.global-add.menu` | Task/Habit/Goal/Track/Workout creation are direct visible choices with the underlying Home context intact. |
| `shared.identity-emoji.custom` | Custom emoji/name inputs and Save & Use remain distinct from read-only common choices; Settings and entity editors share the saved identity. |
| `shared.identity-emoji.picker` | Search, Add Custom Emoji and the common-grid choice coexist; one selection is a draft until the owning editor saves. |
| `shared.inspector.base` | Entity identity, Today/History/Options, edit/close and the fixed primary action remain legible, with long body content scrolling independently. |
| `shared.launch-queue-overflow` | The guard quantifies two rejected extra shares, explains that current/waiting drafts remain and gives a clear dismissal; actual two-share process-death recovery is verified separately, while overflow through death remains unproven. |
| `shared.overflow.menu` | Edit/Duplicate/Archive occupy an anchored menu rather than replacing the entity identity; exact mutation is owned by each domain. |
| `shared.permanent-delete` | The generic guard names irreversible settings/history impact, backup advice and separate Cancel/Delete Permanently; domain-specific deletion coordinators own exact impact. |
| `shared.review.summary` | Review & Trends separates Task, Habit, Goal and Gym outcomes with source-scoped cards; Track evidence is labeled separately from productivity scores. Review/source-failure journeys cover loaded and unavailable variants. |
| `shared.startup-recovery.failure` | Startup blocks unsafe data opening with an explicit recovery message and Retry Recovery, rather than showing an empty workspace; the storage/epoch failure paths own transactional correctness. |
| `shared.unit.menu` | Existing unit choices and Create Custom Unit remain distinguishable; custom factor persistence is Settings-owned. |
| `shared.unsaved-changes` | The dirty-editor guard names unsaved Habit changes and separates Keep Editing from Discard Changes; editor recreation tests own draft retention. |
| `shared.weekday-reminders` | Weekday choice explains that nothing schedules until a time is added; seven day controls, Cancel and existing time chip remain visible. Reminder scheduler/domain rules own effective alarms. |

## Habits — 16 frozen unfinished IDs

Source boundary: `HabitDraft`/`HabitRepository` and its log, pause, skip and timer entities; `HabitViewModel` and Habit screens render their projections. Native owners include `ActivityHistoryUiTest`, `HabitDeletionUiTest`, `ProductivityCreationJourneyE2ETest`, `ProductivityCardDesignUiTest`, `HabitReminderJourneyE2ETest` and the real launcher-widget journey recorded in product memory. The catalog images alone do not prove a scheduled notification or a saved log.

| Frozen ID | Current-source/journey disposition and rendered observation |
| --- | --- |
| `habits.all.populated` | All Habits lists distinct active practices with separate expand and check-off actions and a visible All Habits tab. |
| `habits.editor.create` | Tracking choice precedes target/schedule fields; Save and required name stay identifiable and the draft is not auto-saved by a choice. |
| `habits.editor.edit` | Persisted name/emoji/mode and schedule reopen in the same form; editor and repository tests own exact update/recreation semantics. |
| `habits.history-log` | Earlier-day logging names the Habit and date with optional note and explicit Record; it does not expose internal state/value storage fields. |
| `habits.history-log-edit` | Exact prior amount/note are editable with Save Changes; failed-save draft retention belongs to `ActivityHistoryUiTest`. |
| `habits.inspector.history` | Earlier-day check-in and saved Skip/Pause history remain distinguishable, with the fixed current-day action separate. |
| `habits.inspector.options` | Pin, pause dates, duplicate, archive and destructive entry are grouped by intent; saved pause spans are named rather than silently excluded. |
| `habits.inspector.today` | Identity, scheduled day, streak/completion rate and current action fit the compact inspector; the action is docked below the scrollable summary. |
| `habits.pause-create` | Start/end/no-end choices and the effect on check-ins, streak misses and reminders are explicit before Schedule Pause. |
| `habits.pause.large-error` | Actual 200% text retains the failed-save explanation and the original pause draft; Delete/Cancel/Save Changes remain reachable in the short dialog. |
| `habits.permanent-delete` | The reviewed Habit and exact measurement/check-in/checklist/skip/pause/timer impact are named with backup advice and separate destructive action. |
| `habits.row.menu` | Browse Templates and Reorder All Habits remain anchored to the Habit workspace, not mistaken for per-row check-in. |
| `habits.template` | Hydration, medication, reading, meditation, no-spend, exercise and rating templates explain their starting tracking meaning; selection still opens an editable draft. |
| `habits.timer-review` | Uncertain elapsed timer explicitly offers correction, Continue, Stop & Log and Discard; timer persistence/delivery is separate from this dialog. |
| `habits.today.empty` | No Habits Here names the empty scope and offers Browse Templates without pretending past/archived records were deleted. |
| `habits.value-entry` | Current total and optional note appear before Save; repository semantics distinguish setting today's total from adding a second increment. |

## Goals — 14 frozen unfinished IDs

Source boundary: `GoalDraft`/`GoalRepository`, progress and elapsed-reset history, `GoalViewModel` and Goal screens. Native owners include `GoalSecondaryMutationUiTest`, `ElapsedGoalTimeUiTest`, `AdaptiveWhipScreenTest`, `ProductivityCreationJourneyE2ETest` and deletion/measurement repository tests. The diagnostic images were reviewed for title, value, date/time, action and error placement; final-source hashes remain pending.

| Frozen ID | Current-source/journey disposition and rendered observation |
| --- | --- |
| `goals.actions` | An active Goal inspector states Ready to begin and exposes Log Progress without implying prior observations; Options/History retain separate tabs. |
| `goals.active.empty` | Start Your First Goal and Browse Templates appear inside the empty active scope; historical or archived Goals are not implied to be gone. |
| `goals.archived.populated` | Saved archived Goal is visible under its own tab with restore via inspector, separate from lifecycle-completed history. |
| `goals.editor.create` | Name, type, target and unit are ordered by dependency; Save is fixed while a longer form scrolls. |
| `goals.editor.edit` | Existing target/unit reopen with their exact value; elapsed type supplies separate counter-origin controls rather than rewriting progress by a generic numeric field. |
| `goals.editor.elapsed-display` | The user-selected year/month/week/day/hour/minute combination and live preview remain visible before saving; timezone/origin persistence is verified by the owning elapsed journey. |
| `goals.elapsed-reset` | Actual large-text review explains replacement of the counter origin without deleting the Goal, names the timezone and chosen instant, and separates Reset to Now from Reset to Chosen Time/Cancel. |
| `goals.elapsed.book-fold` | The book-fold projection retains elapsed cards and support pane without squeezing one count into an unreadable single line; posture-specific source journey owns behavior. |
| `goals.history.populated` | Completed/abandoned outcomes remain in Goal History with their last progress, not presented as active targets. |
| `goals.measurement` | Log Progress names the Goal, observed value unit, optional note and date, with explicit Cancel/Log Progress. |
| `goals.measurement.large` | At actual 200% text, value, unit, date and action remain within the scrollable/fixed dialog layout; failed-save draft/duplicate submission has a native journey. |
| `goals.permanent-delete` | Exact progress, milestones, closure snapshot and timer-reset impact are listed before the irreversible action with backup advice. |
| `goals.row.menu` | Browse Templates and Reorder Goals are anchored to the active workspace, distinct from the per-card Log/Reset action. |
| `goals.template` | Weight, savings, distance, pages, consistency, project and elapsed examples explain differing Goal types; selection starts an editable Goal, not an immediate save. |

## Tracks — 23 frozen unfinished IDs

Source boundary: Track definition/Entry Room repositories, field/condition rules, bounded projection and CSV preparation/commit, plus Track workspace/inspector screens. Native owners include `TrackDefinitionMutationUiTest`, `TrackCsvImportUiTest`, `TrackCsvJourneyE2ETest`, `TrackCollectionJourneyE2ETest` and `TrackCsvImportIntegrityTest`; the seven literal 5,000-row commits and API 26/34/37 SQLite binding boundary are separately recorded in product memory. The v2 originals below show the named state, not by themselves a completed file-picker or durable mutation.

| Frozen ID | Current-source/journey disposition and rendered observation |
| --- | --- |
| `tracks.definition-removal.large` | At actual 200% text, the exact historical values/Choices to be removed are listed and the no-change-until-reviewed warning precedes Keep Editing/Apply Reviewed Changes; source validation rechecks the revision at commit. |
| `tracks.csv-import.empty-large` | An empty file is named as an error, Choose Another File remains reachable and Import 0 Entries is disabled; date and field mapping context remain visible. |
| `tracks.csv-import.frozen-large` | The invalidated five-field preview says attention is required before writing, offers Replace File and disables Import 0 Entries; frozen mapping is not silently committed. |
| `tracks.all.empty` | Track What Matters and Create First Track distinguish no active Track from an unavailable database or hidden filter. |
| `tracks.activity.populated` | Cross-Track Activity lists a named Entry with Track/Area/date context and edit/menu affordances. |
| `tracks.archived.populated` | The archived Track remains listed with its saved Entry count; its detail is reachable rather than being converted to an empty active Track. |
| `tracks.archived.detail.entries` | Archived detail explicitly says Read-only, still shows its Entry and provides Restore Track; no editing action is misrepresented as active. |
| `tracks.insights.populated` | Overview count, field count, recent-window counts, frequency and recently active Track are labeled separately; the source-bound recent-window/units journeys own numeric correctness. |
| `tracks.detail.entries` | Per-Track title/count and Entries/Options/Track Insights tabs persist above search/filter/sort and Entry list. |
| `tracks.detail.search` | Search stays scoped to this Track, keeps the entered query visible above the matching Entry and IME. |
| `tracks.detail.search.empty` | No Matching Entries advises clearing Search or Filters without claiming the saved Track has zero Entries. |
| `tracks.detail.options` | Edit, pin, duplicate structure, CSV export/import, archive and danger action state their separate consequences; CSV is not confused with backup/restore. |
| `tracks.detail.insights` | Per-Track total, first/latest date and weekly rate are visible; bounded projection and exact filter/date/number tests own historical integrity. |
| `tracks.detail.unavailable` | A deleted/missing Entry shows an explicit unavailable message and Close, never reinterprets an old deep link as a new Entry. |
| `tracks.editor.create` | Required Track name, initial identity field, Add Field and organization controls precede Save; no Entry is implicitly created. |
| `tracks.editor.edit` | A long field list is scrollable below the saved name with per-field editing; asynchronous definition conflict retains a recovery path in the owning native test. |
| `tracks.entry.details` | Read-only inspector names Track, Entry, date and recorded field value rather than an inferred Task/Habit outcome. |
| `tracks.filter` | Match All/Match Any and Add Condition are explicit; an empty draft states every Entry remains included until Apply Filters. |
| `tracks.condition` | Entry Date condition separates Field, operator, date and Cancel/Add; Date Field range evaluation is verified by the dedicated native regression. |
| `tracks.csv-import` | File/date mapping, validation count and per-Entry preview precede Import 1 Entry; repeated maximum imports and picker flow are distinct test boundaries. |
| `tracks.activity.menu` | Open Track and Delete Entry appear on the cross-Track Activity row with the named Entry still visible. |
| `tracks.entry.menu` | Delete Entry is attached to the per-Track row, with exact preview/Undo handled by the deletion journey. |
| `tracks.reorder.menu` | Select Tracks appears in the active workspace menu; bulk archive/reorder failure keeps selection for retry in the native journey. |

## Settings — 18 frozen unfinished IDs

Source boundary: `SettingsScreens`/`SettingsViewModel`, `SettingsRepository`, reminder runtime/scheduler and backup/restore/portable-folder managers. Native owners include `SettingsBehaviorUiTest`, `SettingsResponsiveUiTest`, `SafetyChoiceUiTest`, `DataPrivacyJourneyE2ETest`, `PortableBackupManagerTest` and reminder delivery journeys. In particular, the diagnostic v2 Data & Privacy screenshots predate the newly confirmed toggle/receipt/retention writes; final-source captures are required before accepting them.

| Frozen ID | Current-source/journey disposition and rendered observation |
| --- | --- |
| `settings.about-diagnostics` | Package/version and the local-data/account promise are visible without implying cloud sync or an unpublished release. |
| `settings.appearance` | Theme, dynamic color, opening Area and low-pressure presentation are labeled with their effect; the Hardware Keyboard shortcut reference remains separate from touch controls. |
| `settings.backup-preview` | Import preview distinguishes Merge New Data from Replace Everything, names record count and explains that merge keeps current settings; a second destructive gate owns replacement. |
| `settings.custom-emoji` | Emoji and name are editable before Add, with common read-only choices described as distinct from user-owned custom ones. |
| `settings.custom-unit` | Name, symbol, dimension and conversion factor remain in a draft above Cancel/Create; incompatible or failed persistence is handled by the owning Settings journey. |
| `settings.custom-unit.large` | At actual 200% text with IME, name/factor and fixed Cancel/Create remain reachable; the factor explanation scrolls with the form. |
| `settings.data-privacy` | Plain/encrypted backup, preview/restore, CSV and portable folder appear as distinct operations with backup limitations stated; final capture must include the confirmed-write state. |
| `settings.delete-retry.large` | Failed destructive cleanup explicitly says the recovery marker is active and keeps Retry Deletion/Cancel visible at actual 200% text; no false completion is shown. |
| `settings.index` | The seven top-level categories have short effect descriptions and direct navigation; About is informational, not a storage control. |
| `settings.number-editor.discard-large` | A dirty numeric setting invokes Keep Editing versus Discard Changes at actual enlarged text rather than silently dismissing the value. |
| `settings.number-editor.large` | Bounded numeric input names its setting/value, explains Save and keeps Cancel/Save reachable in a short 200%-text viewport. |
| `settings.organization` | Area count, custom emoji library and Tags scope are separated; common emojis are explicitly read-only. |
| `settings.planning` | Week start, timezone, late-night cutoff, precision and unit defaults show saved values and cause/effect copy; the settings contract table owns downstream consumers. |
| `settings.reminders` | Permission-denied state accurately says delivery is blocked, offers precise-timing and notification opt-ins, diagnostics and Refresh; Send Test Notification is unavailable while permission is missing. Actual alarms/shade delivery require platform journeys. |
| `settings.reset` | Whole-app reset names local records, settings, schedules and portable-folder disconnection; the private recovery marker/rollback journey owns failure safety. |
| `settings.restore-confirm.large` | At actual 200% text, replacement consequences and private-snapshot rollback are scrollable above fixed Cancel/Replace Everything; the lower bullets need final original inspection at scrolled positions. |
| `settings.restore-preview` | Merge, replacement and Cancel have separate meanings with existing-device library count; merely selecting a backup does not mutate local data. |
| `settings.restore-preview.large` | The preview's record count, merge/replacement distinctions and Cancel stay reachable in the constrained large-text dialog; the body scrolls to remaining details. |

## Organization — two frozen unfinished IDs

Source boundary: `AreaRepository`/`AreaDeletionCoordinator`, `AreaFeatureUiTest` and the real `AreaOrganizationJourneyE2ETest`. The broader Area/Tag lifecycle already has frozen Verified rows; these two controls remain a specific visual/semantics reconciliation at final source.

| Frozen ID | Current-source/journey disposition and rendered observation |
| --- | --- |
| `organization.area-picker.menu` | The selected Area is marked in the compact scope popup with its item count; Create Area and Manage Areas are separate commands. Search/clear and named-Area lifecycle belong to the existing native journeys. |
| `organization.move-area.large` | At actual 200% text, the dialog quantifies two Tasks, lists four destinations, explains that source Area/history stay unchanged and keeps Cancel/Move 2 Items visible; the mutation remains disabled until a destination is chosen. |

## Gym — 41 continuing frozen unfinished IDs

Seven additional frozen Gym IDs have exact retirement/replacement mappings in `ASTRA_GYM_RETIREMENT_DISPOSITION_2026-09-21.md`; they are not current generic capture cards. Continuing source owners are the Routine/Exercise/Machine/Workout repositories and `GymScreens`, `RoutineBuilderUiTest`, `GymPowerInputUiTest`, `WorkoutDeletionUiTest`, `RoutineAuthoringJourneyE2ETest`, `GymPhasedRoutineJourneyE2ETest`, `GymLegacyRetirementJourneyE2ETest` and the grouped-NEXT journey. The v2 diagnostics below contain four old 5/3/1-named *generic fixtures*; FND-20260921-009 corrected those fixtures, so those exact cards must be re-inspected from final source before visual acceptance. Historical performed 5/3/1 data and the legacy-only active-session review retain provenance by design.

| Frozen ID | Current-source/journey disposition and rendered observation |
| --- | --- |
| `gym.routine-authoring.workout` | A saved two-day Routine starts its chosen day with exact exercise/Set/NEXT context; native authoring journey proves the chosen-day identity and unchanged source Routine. |
| `gym.machine.delete-retry.large` | At actual 200% text, an unverified deletion outcome is named and Retry Verification stays distinct from Cancel/Delete Profile; history is not falsely declared gone. |
| `gym.exercise.delete-retry.large` | Stale impact shows Review Updated Impact above the changed removed/reference counts, with Cancel/Delete reachable at actual 200%. |
| `gym.workout.delete-blocked.large` | An active Workout is explicitly blocked from deletion and directed to finish/discard first; removed/recalculated/kept consequences remain scrollable with fixed actions. The v2 name was a generic-fixture defect now corrected. |
| `gym.workout.empty` | No Workout in Progress offers Create First Exercise or Start Empty Workout as separate entry paths. |
| `gym.workout.active` | Active Workout shows source/day/date, next exact Set, rest status, Add Exercise, arrangement and editable Set values; completion is scoped to the selected Set. |
| `gym.routine.active-blocked` | Generic phased Routine card explains why the active Workout must finish/discard before editing progression while preserving Browse All Phases/Open Active Workout. |
| `gym.history.populated` | Finished sessions remain searchable with history filters and expandable cards, separate from the current Workout. |
| `gym.history.expanded` | Historical Set retains exact load/reps, classification, RPE/RIR, Training Max target and note, with Restore to History rather than mutating performance in place. |
| `gym.progress.populated` | Tracked Records empty state and exercise/measurement trend controls distinguish no chosen benchmark from no completed workout; completed-source projection owns values. |
| `gym.routines.populated` | Library shows saved Routine and workout-day count with independent edit/overflow/start routes. |
| `gym.tools` | One-rep-max and plate calculators label estimates as planning aids, keep known-1RM choice and source Set reuse explicit. |
| `gym.routine-builder.outline` | Ordinary New Routine owns name, day, Exercise placement, optional notes, previous-workout reuse and Add Phases; no new 5/3/1 setup action. |
| `gym.routine-builder.exercise-picker` | Search/filter/205-item list and Create Exercise remain available; only checked Exercises enter the day outline. |
| `gym.routine-builder.workout-picker` | Empty completed-workout library says no reusable session exists and keeps back navigation reachable, not a blank picker. |
| `gym.rep-scheme` | Reusable Set/rep prescription draft requires valid set count and minimum/maximum reps, distinguishes Set classification and optional rest. |
| `gym.quick-machine` | Machine can be created for a placement without abandoning the Routine; name/location, mass versus numbered stack and units remain explicit. |
| `gym.exercise.editor` | Saved Exercise name/type, notes and applicable advanced options remain editable; duration-only type does not present irrelevant weight controls. |
| `gym.exercise.picker` | Search, current library result and Create Exercise are visible within the Routine flow. |
| `gym.exercise.actions` | Inspector keeps tracking type, notes, increments, rest default and load meaning readable with edit/close separate. |
| `gym.exercise.permanent-delete` | Active placement blocks deletion; removed workout placements/Sets and retained records are explained before an irreversible choice. |
| `gym.machine.editor` | Machine name/location/model, resistance type, unit and load range can be authored as one profile with advanced details below. |
| `gym.machine.choice` | Search, No Machine/Free Weights, Quick-Create and advanced profile creation are separate choices inside placement editing. |
| `gym.machine.permanent-delete` | Removed profile/current metadata and kept completed workouts/Sets are differentiated; final source must preserve the same impact meaning. |
| `gym.workout.editor` | Start Workout names optional title/note/date and keep-awake preference; Cancel does not create a session. |
| `gym.workout.set-editor` | Exact Set editor shows load/reps, classification, planned/completed state, RPE, tempo, rest and note under Save. |
| `gym.workout.group` | Superset/Circuit choice and optional group name are deliberate and reversible before Create. |
| `gym.workout.notes` | Notes edit preserves the active Workout context, with Cancel/Save and a machine-creation route that does not discard the note draft. |
| `gym.workout.permanent-delete` | Reviewed deletion distinguishes removed session data, recalculated progress/records and kept Exercise/Routine definitions. The v2 title is obsolete generic-fixture copy; final capture must show its neutral replacement. |
| `gym.tracked-records` | Per-Exercise record checkboxes and Remove from Tracked Records are distinct; saved record history persists independently of this presentation choice. |
| `gym.routine.program-position` | Generic phase/day/cycle positioning keeps future-start semantics and performed History unchanged. The v2 5/3/1 phase labels were incorrect fixture data; final capture must show `Custom` Volume/Build/Peak/Recovery. |
| `gym.routine.permanent-delete` | Active source Workout blocks removal and historical sessions/decisions remain preserved; v2 branded fixture must be replaced in the final generic card. |
| `gym.confirmation` | Required Main Set removal is called Mark Not Performed and explicitly says the prescribed Set stays in Workout History. |
| `gym.defaults-change` | Exercise default changes expose unit/increment and graph/record options; source conversion test owns pound-hardware versus decimal conversion correctness. |
| `gym.category-allocation` | Overlapping category contribution choice explains full/split/first-linked accounting; defaults, RPE/RIR and assisted-record controls are separately exposed. |
| `gym.workout-set.menu` | Duplicate Set and Remove Set are anchored to the exact Set card, not a global Workout action. |
| `gym.workout-exercise.menu` | Substitute/Remove from Workout are scoped to the active Exercise; completed history remains separate. |
| `gym.workout-history.menu` | Edit Details, Save as Routine, Share, Resume Original Workout and Delete Permanently are attached to the selected historical session. |
| `gym.routine.menu` | Routine Duplicate/Pin/Archive/Delete are grouped in its Library card and distinct from Open Active Workout. |
| `gym.routine-placement.menu` | Duplicate/Remove operate on the selected Exercise placement inside the editable Routine, not its library definition. |
| `gym.classification.menu` | Warm-up, Working, Back-off, Drop, AMRAP, Training Max test and Failure are named classifications for a generic prescription. |

## Fourteen post-freeze current IDs

These IDs were added after the immutable frozen matrix, so they are review additions, not retroactive September 8 statuses. New-source native journey and original-screen evidence is recorded in the linked product-memory verification entries. The v2 images were inspected where present; `settings.delete-retry.large-impacts` was added later and awaits the exact final capture.

| Current ID | Source/journey and provisional visual disposition |
| --- | --- |
| `gym.legacy-program.review` | Legacy-active-session-only Training Max decisions retain the original 5/3/1 terms and explicit Keep Training/Apply Decisions & Finish. `GymLegacyRetirementJourneyE2ETest` and the true OS-process-kill replay verify the historical/next-template boundary. |
| `gym.legacy-program.review.large` | Actual 200% legacy dialog preserves evidence, selectable decisions and fixed finish action while the long explanation scrolls; it is not a generic new-Routine screen. |
| `gym.routine-builder.phases` | Generic Routine can label/reorder/copy/remove phases and deliberately mark a Training Max boundary; the screen says preview does not advance current position and Routine Save is the persistence boundary. |
| `gym.routine-builder.primary-progression` | Per-exercise Training Max source/value and cycle increase are editable in the ordinary Routine; actual or estimated 1RM remains distinct and advanced prescription fields stay optional. |
| `gym.routine-phased.workout` | Ordinary `Strength blocks` Workout displays Volume phase, exact target load/reps and NEXT Set without a 5/3/1 generator. |
| `gym.workout.grouped-next-set` | A long Superset's NEXT handoff visibly lands on the exact `Navigation row` Set 1/Complete Set while the header names the same target; normal/wide/actual-200% and recreation are in the native journey. |
| `settings.data-controls.csv-export.saved` | Native picker returns a completed CSV export with success feedback; format/rows are asserted by `DataPrivacyJourneyE2ETest`. |
| `settings.data-controls.encrypted-export.saved` | Encrypted backup selected through real DocumentsUI survives Activity recreation and acknowledges the saved encrypted format, not plain JSON. |
| `settings.data-controls.plain-export.saved` | Plain JSON backup selected through real DocumentsUI reports its format and leaves the other export paths unchanged. |
| `settings.data-controls.portable-folder-reconnect` | Genuine moved SAF tree retains last verified name/time, shows access-loss warning and exposes Reconnect or Change Folder/Forget Folder; `DataPrivacyJourneyE2ETest` proves the exact tree move and reselection. |
| `settings.delete-retry.large-impacts` | New 200%-text scrolled destructive state exposes final consequence/backup advice and fixed recovery action; no v2 card exists, so final original inspection is mandatory. |
| `tracks.collection.mutation-failure` | Real failed archive keeps both exact selected Tracks, says no Tracks changed and offers an inline retry; persisted rollback/recreation is asserted by `TrackCollectionFailureJourneyE2ETest`. |
| `tracks.detail.insights.date-range` | Authored `Observed` Date Field Between chip remains visible over three matching Entries/analytics after recreation. The displayed first/latest dates are Entry dates; the condition uses the separate Observed value. |
| `tracks.history-controls.date-range` | Three boundary-matching Entries retain their Entry date and distinct Observed values under the same inclusive filter; `TrackHistoryControlsJourneyE2ETest` proves both endpoints and history retention. |

All 181 review rows now have a provisional source/journey/visual or explicit retirement mapping. They are **not yet final Verified dispositions**: the final-source 523-card capture, complete app/platform gates, inspection of changed originals and cross-family acceptance remain open.
