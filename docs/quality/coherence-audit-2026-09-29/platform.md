# Widgets and Android integration coherence audit — 2026-09-29

Baseline: `6d79bd1f82da9c1666960834410b3e08ed6d3ea1`. Audit only; no application/test/memory edits, builds, test execution, device invocation, reset or release. This report records current-source conclusions, personally inspected **dated** originals, justified keeps and unresolved runtime questions. Existing test methods below are available contracts, **not fresh passes**. The coordinating parent owns fresh native evidence and final synthesis. Shared Focus presentation and Review are owned by other audit components.

The strongest gaps are exact-source handoff and truthful widget summaries. There are **9 source-grounded P2 findings**, **148 numbered flow/state dispositions**, and **1 narrowly scoped fresh native keep check supplied by the parent**. There is no evidence here of a new scheduling reliability regression; preserve the previously released alarm, claim, recovery, idempotence and Task capture contracts.

## Findings, strongest first

### PLAT-01 — P2: Out-of-Area timer Review can open Habits without opening the timer

- **Trigger:** Configure a Habit widget for Area A; start a Duration Habit in Area B from the app. Running/review timers intentionally appear outside normal widget filters. Tap the row body, or its Review action after the timer needs review.
- **Current source:** [WidgetContent.kt:145](../../../app/src/main/java/com/whip/app/widget/WidgetContent.kt#L145) bypasses Area/selection/archive/pause filters for a non-null timer session. [WhipWidgetProvider.kt:394](../../../app/src/main/java/com/whip/app/widget/WhipWidgetProvider.kt#L394) sends the widget's Area on `COLLECTION_OPEN_HABIT`. [WhipApp.kt:598](../../../app/src/main/java/com/whip/app/ui/WhipApp.kt#L598) treats an explicit widget Area as authoritative and returns before resolving the target's Area. It filters Habit state at 682; [HabitScreens.kt:332](../../../app/src/main/java/com/whip/app/ui/HabitScreens.kt#L332) searches only `state.all` and `state.archived` to open the requested ID. [AreaScopeFilters.kt:55](../../../app/src/main/java/com/whip/app/ui/AreaScopeFilters.kt#L55) excludes the out-of-Area Habit even though unscoped editor state exists.
- **Harm:** A recovery row is visible and promises Review, but the source inspector cannot be admitted under the retained Area. Direct Stop has a repository path; Review requires the app and is therefore particularly affected. The launch FIFO consumes the command while the inner request remains unresolved, rather than explaining the mismatch.
- **Root cause:** The deliberate timer visibility exception is absent from the widget-to-app scope-resolution contract.
- **Small remedy:** Retain ordinary widget header/Add persistence, but resolve entity opens against the exact entity Area when the requested widget scope excludes it, or admit the exact inspector from unscoped editor state with explicit source context. Do not remove the timer visibility exception.
- **Verification:** Native pinned Area-A widget with Area-B running and review timers, ordinary and 200% text; body/Open/Review, recreation and Back; exact timer inspector and unchanged logs/session. Add a targeted source-level resolution case for the scope exception. Existing `WidgetSummaryTest#activeAndReviewTimersStayReachableOutsideNormalWidgetFilters` checks row inclusion, not this handoff. **Source-confirmed; no fresh native reproduction in this audit.**

### PLAT-02 — P2: Completed-period filtering can hide a running or review timer

- **Trigger:** Disable “Keep completed Habits visible”; a Duration Habit already has sufficient recorded period progress, then a new timer starts in the app, or that timer enters review.
- **Current source:** [WidgetContent.kt:145](../../../app/src/main/java/com/whip/app/widget/WidgetContent.kt#L145) preserves timer sessions through most filters, but 167 calculates `completed` solely from recorded period logs and 174 applies `showCompleted || !it.completed` without the timer exception. [HabitModels.kt:433](../../../app/src/main/java/com/whip/app/domain/HabitModels.kt#L433) determines period outcome from logs; a new active session does not erase earlier completed progress. The existing active-timer test has empty logs, so it does not cover this case.
- **Harm:** Stop/Review disappears even though the session is still live. An apparently finished empty widget may say “You're done for today” while a timer still needs action.
- **Root cause:** Completion filtering occurs after the timer safety override and does not preserve it.
- **Small remedy:** Include live/review timer sessions regardless of the completed-visibility option. Keep completed stationary Habits hidden as configured.
- **Verification:** Exact calculation test with a completed period plus running/review session and `showCompleted=false`; pinned widget → app start → visible Stop/Review → one completed timer source log. Include a completed weekly Duration period. **Source-confirmed; runtime not executed.**

### PLAT-03 — P2: Rest-complete notification forgets the Workout destination and session identity

- **Trigger:** While a workout remains active, navigate Gym to Library, History or Progress; receive “Rest complete” and tap it while the existing Activity is retained.
- **Current source:** [RestTimerNotifications.kt:180](../../../app/src/main/java/com/whip/app/reminders/RestTimerNotifications.kt#L180) authors `ACTION_OPEN_GYM` with the session ID. [MainActivity.kt:328](../../../app/src/main/java/com/whip/app/MainActivity.kt#L328) decodes that ID, but [LaunchDeliveryEffect.kt:176](../../../app/src/main/java/com/whip/app/ui/LaunchDeliveryEffect.kt#L176) reduces it to `OpenGym`, and [WhipApp.kt:1320](../../../app/src/main/java/com/whip/app/ui/WhipApp.kt#L1320) only selects the Gym workspace. `gymDestination` is independently saveable at 1462 and passed into Gym at 2374. The equivalent Home action explicitly selects Workout at 2111.
- **Harm:** A notification naming the next Set lands on the previously visited Gym tab, adding a recovery step during execution. The cold default is Workout, which can hide the defect in cold-only testing. The requested session ID is discarded, so an obsolete notification also cannot explain that its original workout has ended.
- **Root cause:** The notification carries a source identity but the command models only the workspace.
- **Small remedy:** Route the rest alert explicitly to Workout and validate the requested active session; show a truthful unavailable/history outcome if the original session ended. Reuse existing Gym source navigation rather than a new navigation stack.
- **Verification:** Real alert tapped from Library/History/Progress, warm Activity and process recreation; exact original active workout and next Set, ended-session fallback, no change to timer delivery/history. No existing `ACTION_OPEN_GYM` native reference was found in the inspected test tree. **Source-confirmed; native tap not executed.**

### PLAT-04 — P2: Habit widget numbers use a different and incomplete meaning contract

- **Trigger:** Decimal quick increment `0.001` at precision 3; a unit-bearing Habit; AtMost or WithinRange target; weekly/monthly/rolling target period.
- **Current source:** [HabitWidgetRemoteViewsService.kt:255](../../../app/src/main/java/com/whip/app/widget/HabitWidgetRemoteViewsService.kt#L255) formats the action through a private `formatWidgetNumber` at 387–390, fixed to two decimal places. The receiver applies the actual `habit.quickIncrement` at [WhipWidgetProvider.kt:462](../../../app/src/main/java/com/whip/app/widget/WhipWidgetProvider.kt#L462). Meta at 360–363 contains value and `targetMin` only, with no unit, comparison, `targetMax` or period. The app already has precision-aware, unit-aware, comparison-aware period copy in [HabitScreens.kt:960](../../../app/src/main/java/com/whip/app/ui/HabitScreens.kt#L960), while notification formatting supports six decimal places at [HabitReminderScheduler.kt:946](../../../app/src/main/java/com/whip/app/reminders/HabitReminderScheduler.kt#L946).
- **Harm:** A `+0` widget action can persist `0.001`. AtMost displays only a number, omitting its upper bound; WithinRange displays only its lower bound. “3 of 5” does not say whether it means litres today or hours this week. Decimal-comma locales also use dot-specific trimming; e.g. rounded noninteger `0.001` can render `0,` instead of a meaningful amount.
- **Root cause:** Widget-local formatting duplicates only the simplest Count/AtLeast contract and loses the domain's semantics.
- **Small remedy:** Feed a compact shared Habit amount/target summary into RemoteViews, honoring configured precision, unit, comparison and period. Reuse a locale-aware formatter rather than trimming a locale-specific number with `'.'`. Action amount must describe the actual mutation.
- **Verification:** Render +0.001, six-decimal increment, decimal-comma locale, custom unit, pounds/minutes, AtMost, Exactly, WithinRange and weekly target; exact posted amount matches the displayed/spoken amount. Unitless Count can retain concise “1 of 5.” **Source-confirmed; no new locale/device run.**

### PLAT-05 — P2: A stopped Duration widget loses the evidence it just recorded

- **Trigger:** Stop a Duration timer, or manually record a Duration entry in the app, then return to the widget.
- **Current source:** [HabitWidgetRemoteViewsService.kt:353](../../../app/src/main/java/com/whip/app/widget/HabitWidgetRemoteViewsService.kt#L353) displays elapsed only while running/reviewing; otherwise meta is “Start.” `HabitWidgetRow` already retains recorded period `value` at [WidgetContent.kt:200](../../../app/src/main/java/com/whip/app/widget/WidgetContent.kt#L200). This duplicates the trailing Start action and omits saved progress/target/unit.
- **Dated original personally inspected:** `artifacts/astra-audit/2026-09-27/habit-widget-modes/run-api34/timer-stopped.png` shows the stopped timer with **Start in both body metadata and action**. Its dated receipt separately confirms one 12.217-second log and in-app History “Logged 12 sec.” This verifies the older visible omission, not a fresh 2026-09-29 pass. Current source still implements that summary.
- **Harm:** The user cannot confirm from the execution surface that Stop saved duration, or judge remaining target work. Header completion alone cannot distinguish this Habit’s amount from others.
- **Root cause:** Timer control state is reused as the evidence subtitle.
- **Small remedy:** Use recorded period progress/target for stopped Duration metadata; retain Start as the independent control. While running, distinguish saved progress from session elapsed when both are meaningful.
- **Verification:** Manual Duration, first Stop, second session, target completion, unit conversion, weekly period and process recreation; compare exact canonical log with rendered amount and in-app summary. The prior Count/Timer receipt remains a keep for successful persistence. **Current-source omission plus dated visual corroboration.**

### PLAT-06 — P2: Widget elapsed text looks live but is a refresh-time snapshot

- **Trigger:** Start a Duration timer and leave the widget visible for several minutes without a domain mutation.
- **Current source:** [HabitWidgetRemoteViewsService.kt:353](../../../app/src/main/java/com/whip/app/widget/HabitWidgetRemoteViewsService.kt#L353) calls `widgetTimerElapsed` once when creating RemoteViews, and 366–383 writes a text snapshot. There is no widget Chronometer/tick mechanism. [WhipApplication.kt:504](../../../app/src/main/java/com/whip/app/WhipApplication.kt#L504) refreshes on data/calendar changes; the calendar stream is `distinctUntilChanged` on date/zone/cutoff at [AppSettings.kt:150](../../../app/src/main/java/com/whip/app/core/AppSettings.kt#L150), not every minute. Provider XML requests a 30-minute periodic update. The in-app Duration view has a one-second ticker at [HabitScreens.kt:972](../../../app/src/main/java/com/whip/app/ui/HabitScreens.kt#L972).
- **Harm:** A label such as “0:05 elapsed” can remain visible while the timer has accumulated several minutes, undermining the reason to glance at a running timer. Saved timing uses the repository's clock boundary and is not shown to be wrong.
- **Root cause:** A sampled value is phrased as current elapsed time without a live host-supported presentation.
- **Small remedy:** Prefer an appropriate native RemoteViews chronometer if its clock/review contract fits; otherwise label the sample honestly and provide exact live elapsed in the app. Avoid waking the application every second to rewrite widgets.
- **Verification:** Launcher visible timer across 60+ seconds, screen off/on, process death and clock change; observed text cadence versus authoritative stop amount. Review/estimated time must remain distinct. **Source-confirmed static rendering; exact host refresh cadence unresolved.**

### PLAT-07 — P2: Extreme-text metadata is hidden without its promised semantic substitute

- **Trigger:** System font scale reaches 200% or greater; inspect a Task due tomorrow/overdue, a numeric Habit with progress, or a checklist with item progress.
- **Current source:** [WidgetSnapshotCache.kt:252](../../../app/src/main/java/com/whip/app/widget/WidgetSnapshotCache.kt#L252) explicitly promises secondary metadata remains in content descriptions, but 270 hides it. Task row body description at [TaskWidgetRemoteViewsService.kt:187](../../../app/src/main/java/com/whip/app/widget/TaskWidgetRemoteViewsService.kt#L187) is only “Open [Task],” losing date/time; action can preserve unfinished-count only in the review case. Habit body at [HabitWidgetRemoteViewsService.kt:180](../../../app/src/main/java/com/whip/app/widget/HabitWidgetRemoteViewsService.kt#L180) is only “Open [Habit]”; Increment description at 326 names the increment, not current amount/target/period; checklist Toggle names next state without item progress. Existing `WidgetExtremeTextTest` asserts metadata `GONE` and 48 dp controls, not retained meaning.
- **Harm:** Users selecting enlarged text and screen-reader users lose the task date/time and Habit progress precisely where the visual layout intentionally reduces detail. This is not a request to undo the minimum-height layout fix.
- **Root cause:** Responsive geometry and semantics use different payloads.
- **Small remedy:** Keep hidden metadata and icon-only actions at extreme text, but append the compact current-state summary to the source body description and preserve mutation amount/state on the action. Do not create duplicate navigation controls or bring back the decorative logo action.
- **Verification:** Inflate RemoteViews at 1x/2x/3.2x; assert nonvisual metadata on accessible owners, then spoken traversal in a real launcher for overdue/date/time, Count, Decimal, stopped/running timer, checklist and refresh failure. **Source-confirmed semantic gap; no TalkBack speech claimed.**

### PLAT-08 — P2: Overnight Habit reminders do not explain or preserve the day on Open

- **Trigger:** A 23:00 Habit reminder shifts to 07:00 tomorrow under quiet hours; the logical reminder day differs from Whip Today. Tap the notification body instead of Mark done/+amount.
- **Current source:** [HabitReminderScheduler.kt:270](../../../app/src/main/java/com/whip/app/reminders/HabitReminderScheduler.kt#L270) intentionally schedules yesterday's logical reminder after midnight. Notification body at 672 is generic; the content intent at 660–667 contains only Habit ID. By contrast actions carry logical date at 686/710 and persist using that date at 840–845. MainActivity has occurrence decoding, but this Habit content intent supplies none; [HabitScreens.kt:332](../../../app/src/main/java/com/whip/app/ui/HabitScreens.kt#L332) opens the current Habit inspector rather than a reminder-day context.
- **Harm:** Body/Open and one-tap completion can refer to different dates without telling the user. Non-direct modes (Checklist/Duration/Rating/LogOnly) are especially affected because their only logging path is Open. The user can record today while responding to a prior-day reminder.
- **Root cause:** Logical reminder provenance survives background actions but is dropped at presentation and app handoff.
- **Small remedy:** Preserve the reminder logical date in the open request and display it when it differs from current Today; offer the appropriate exact historical log/checklist context or an explicit current-day decision. Do not change quiet-hour scheduling or date validation.
- **Verification:** Quiet-hours midnight crossing with default cutoff and nonzero cutoff, fixed/device zones, CheckOff/Decimal versus Checklist/manual Duration; Open and direct action produce intentional identical dated outcomes. **Source-confirmed provenance omission; actual notification→log journey not executed.**

### PLAT-09 — P2: Direct widget mutation failures can look like ignored taps

- **Trigger:** Task completion/child check, Habit increment/check-off or timer stop throws before persistence (e.g. transient data write failure).
- **Current source:** [WhipWidgetProvider.kt:156](../../../app/src/main/java/com/whip/app/widget/WhipWidgetProvider.kt#L156) and 436 wrap direct mutations in `runCatching` and ignore failures, then `updateAll` at 201/489. If the subsequent read succeeds, the collection renderer shows ordinary unchanged rows. `WidgetCollectionSnapshotState` only presents a refresh error when its read fails; it has no mutation failure receipt.
- **Harm:** The user receives neither confirmation nor failure explanation and may repeat a numeric increment, assuming it did not work. Successful refresh cannot explain why the action failed. A stale rejected action also silently refreshes, which is appropriate safety behavior but should remain distinct from a write failure.
- **Root cause:** Background mutation outcome is discarded before the presentation owner sees it.
- **Small remedy:** Retain a per-widget, generation-bound failure outcome with a concise retry/open explanation, cleared by the next accepted action/reconfiguration. Do not show an error for an expected stale/day/generation rejection and do not make cached rows actionable.
- **Verification:** Inject one precommit failure for Task, numeric Habit and Stop; saved state unchanged, failure rendered, deliberate retry commits once, recreation keeps meaningful recovery, no false failure after an authoritative commit followed by reminder-sync failure. **Source-confirmed lack of failure presentation; injected runtime failure not executed.**

## Numbered flow/state matrix

Disposition: **K** = justified keep in source; **F** = linked finding; **R** = source supports the flow but fresh runtime evidence is unresolved; **N** = capability absent in the inspected production integration, not a defect. Rows are stable platform flow IDs **PL-001 through PL-148**. A K is a design disposition, not a test pass. File abbreviations: WC=`widget/WidgetContent.kt`; WP=`widget/WhipWidgetProvider.kt`; CFG=`widget/WhipWidgetConfigureActivity.kt`; PREF=`widget/WidgetPreferences.kt`; CACHE=`widget/WidgetSnapshotCache.kt`; HRV/TRV=`widget/HabitWidgetRemoteViewsService.kt`/`TaskWidgetRemoteViewsService.kt`; LA=`ui/LaunchDeliveryEffect.kt`; APP=`ui/WhipApp.kt`; MA=`MainActivity.kt`; HRS/GRS/RS=`reminders/HabitReminderScheduler.kt`/`GoalReminderScheduler.kt`/`ReminderScheduler.kt`; RN=`reminders/ReminderNotifications.kt`; RA=`reminders/ReminderActionReceiver.kt`; SS=`ui/SettingsScreens.kt`. Paths are under `app/src/main/java/com/whip/app/` unless XML is specified.

### Widget setup, scope and lifecycle

| # | Flow / state / transition | Current contract and disposition |
|---|---|---|
| PL-001 | Launcher widget picker | **K/R:** Only Task Agenda and Habit Tracking providers, descriptions/previews and retained original Task provider identity; Manifest78–100, WP58–61. Real launcher discovery depends on host. |
| PL-002 | First placement with configuration | **K/R:** CFG85–109 validates assigned ID and provider kind; default RESULT_CANCELED until successful Save. Shared external host/recovery gate at111. |
| PL-003 | Optional configuration skipped | **K:** Provider XML declares `configuration_optional`; PREF22–30 defaults All Areas, 20% transparency, next seven days, show completed, all matching Habits. |
| PL-004 | Reconfigure installed widget | **K:** XML `reconfigurable`; CFG117–155 restores current per-widget choices with generation-aware saveable drafts. |
| PL-005 | Cancel / Back configuration | **K/R:** Cancel finishes with canceled result; unsaved display configuration does not persist until Save. Existing configuration stays intact. No required irreversible data discard. |
| PL-006 | Save configuration | **K:** CFG515–531 validates expected generation under data access, saves, invalidates prior snapshot and updates; result OK at482–489. |
| PL-007 | Save busy / failed admission | **K/R:** CFG156/451/490 disables duplicate Save, reenables on false admission; recovery host covers blocked data. An exception from save/update is not a rendered error contract. |
| PL-008 | Appearance preview | **K/R:** CFG620–630 uses widget palette resources rather than app-theme colors; preview is explicitly illustrative, not live data. System day/night and app host theme have separate owners. |
| PL-009 | Transparency 0/20/40/60/80 | **K:** PREF231 clamps opacity; CFG433 explains only outer card changes, header/row contrast remains. Arbitrary wallpaper contrast remains runtime-dependent. |
| PL-010 | Task window Today /7/30 | **K:** WC86–94 filters through range, overdue retained, recurring siblings condensed, exact keys deduplicated. Window labels and inclusive endpoint should be checked against owner expectations, not silently changed. |
| PL-011 | Area All | **K:** Both widgets scoped with `AreaScope.All`; header/Add scope is deliberate persistent app selection. |
| PL-012 | Area Unassigned | **K:** Distinct storage key and configuration chip; no collision with All; PREF79–86 preserves Unassigned through replacement. |
| PL-013 | Area One active | **K:** Header/Add and normal rows carry configured scope; source entity open resolves ID. PLAT-01 is the timer exception. |
| PL-014 | Archived Area retained | **K/R:** CFG255 keeps current archived Area selectable; WP758 appends Archived to scope label. Active entity lifecycle remains independent. |
| PL-015 | Area deleted | **K:** WP300–315 clears affected widget scope to All and refreshes, PREF save invalidates cache; unavailable label exists for unresolved references. |
| PL-016 | No chosen Habits | **K:** PREF null=all, empty set=none; CFG182–184 disables authored Choose-Habits Save with no matching selection; restored generation can deliberately produce none with clear widget copy. |
| PL-017 | Choose Habits / Select All / Clear | **K/R:** CFG320–375/533–603 presents active matching candidates, checkable 52 dp rows, explicit selected count. No direct Goal/Track selection masquerades as Habit selection. |
| PL-018 | Habit selection versus pinned status | **K:** CFG170 sorts selection list pinned first; WC does not restrict runtime to pinned. Pinning is a priority aid, selection is the inclusion contract. |
| PL-019 | Selected Habit moves Area | **K/R:** Current scope re-evaluated; selection retained independently. A moved stationary Habit disappears from the old scoped widget, live timers are exceptions. |
| PL-020 | Selected Habit archived / deleted | **K:** WC148 filters archive unless timer session; deleted entities have no current rows, expansion IDs prune. Manual selected set can become empty/unavailable without aliasing another generation. |
| PL-021 | Paused / skipped / ended / off-schedule Habit | **K:** WC148–166 excludes ordinary ineligible work using domain pause/skip/end/schedule truth; current timer exception remains. |
| PL-022 | Keep completed on | **K:** WC174–175 retains completed after incomplete, allowing CheckOff/checklist reversal. |
| PL-023 | Keep completed off | **K/F:** Stationary completed Habits hide; completed running/review sessions also hide, PLAT-02. |
| PL-024 | Resize height / orientation | **K/R:** WP206/494 rerender options; WP732–748 hides brand/subtitle for compact height or enlarged text, preserving title/Add and collection viewport. |
| PL-025 | Resize width / narrow host | **R:** XML minimum220×160dp, target3×3, both axes; RemoteViews text/control layout depends on actual host allocation. No current pinned minimum-size visual evidence collected here. |
| PL-026 | 200%/extreme rows | **K/F:** Icon-only actions and one primary line preserve 48 dp controls; hidden metadata semantics incomplete, PLAT-07. |
| PL-027 | Multiple widget instances | **K:** PREF keys, service URIs and PendingIntent identity per widget; configuration/expansion/cache independent. |
| PL-028 | Widget removed | **K:** Both providers clear preferences and snapshots, WP215–218/503–506. |
| PL-029 | Launcher restoration / widget ID remapping | **R:** No custom `onRestored(oldIds,newIds)` override found. Android host behavior and retention of per-ID selection after host restoration need scoped evidence; do not claim migration support from ordinary process recovery. |
| PL-030 | App package upgrade | **K/R:** Provider class identity retained; time receiver handles replacement, runtime maintenance rebuilds. Existing widgets should retain host binding; exact new-host behavior unexecuted. |
| PL-031 | User data replacement / ID reuse | **K:** PREF73–92 strips old entity scope/selected identifiers/expansion and cache; display-only transparency/range/show-completed retained. Custom selected set becomes empty, not unrelated restored Habits. |
| PL-032 | User data startup / epoch barrier | **K:** `withUserDataAccess` guards provider/factory/configure paths; update-required display hides Add/collection and opens recovery. Expected recovery is not a mutation error. |

### Task Agenda execution and recovery

| # | Flow / state / transition | Current contract and disposition |
|---|---|---|
| PL-033 | Scheduled single Task due today | **K:** WC64–95 uses shared Task projection; body exact source and independent completion control, TRV181–224. |
| PL-034 | Overdue scheduled Task | **K/F:** Retained in all windows; date label Overdue at TRV307–313, hidden with extreme metadata (PLAT-07). |
| PL-035 | Upcoming Task | **K:** Range includes scheduled occurrence; date Today/Tomorrow/local date and authored clock label. |
| PL-036 | Unscheduled / Inbox capture | **N/K:** Task Agenda projects today/upcoming, not full Inbox. Configuration says overdue/scheduled; no invented Inbox widget flow. |
| PL-037 | Recurring Task | **K:** Distinct stable occurrence identity; UI condenses future siblings; fill-in carries original date, not just mutable scheduled date. |
| PL-038 | Moved recurring occurrence | **K/R:** Projection chooses visible scheduled date while source action retains original occurrence key. Fresh move→widget→complete not executed here. |
| PL-039 | Parent Task with unfinished Subtasks | **K:** Indeterminate/review action expands collapsed children; expanded parent opens source; provider rechecks unfinished current children before completion. |
| PL-040 | Expand / collapse Subtasks | **K:** Desired expanded boolean and exact stable task key per widget, current eligibility rechecked; not a toggle vulnerable to duplicate inversion. |
| PL-041 | Complete exact Subtask | **K:** Current visible occurrence and Step validated, `completed` payload is desired state; child struck/identity retained. |
| PL-042 | All Subtasks complete → parent complete | **K:** Parent completion can proceed after re-resolve; reminder resync follows. Task row disappears when no longer in agenda. |
| PL-043 | Task completion reversal | **N/K:** No direct Task widget Undo because completed Tasks leave Agenda. App source/history can reopen; notification Undo is a separate supported contract. |
| PL-044 | Stale Task row after archive/delete/config change | **K:** Generation/day/current exact visibility checks reject mutation and refresh. Entity Open can use unavailable fallback. |
| PL-045 | Stale row at logical day cutoff | **K/R:** Collection action rendered-date must equal `app.clock.today`; stale completion does not land on the new date. Header Add carries its rendered date; exact rollover freshness remains runtime scope. |
| PL-046 | Task row source open | **K:** WP819–845 carries ID and original date; LA141–168 searches unscoped occurrences, shared APP resolution waits for data and resolves Area. |
| PL-047 | Current Task mutation error | **F:** Ignored mutation exception plus successful refresh does not expose failure, PLAT-09. |
| PL-048 | Long collection / scroll | **K/R:** Factories return all content rows, not a capped three-row list. Cache is separately capped100. Native launcher scroll viewport/hit targets unexecuted here. |

### Habit Tracking execution and meaning

| # | Flow / state / transition | Current contract and disposition |
|---|---|---|
| PL-049 | CheckOff due / done / undo | **K:** Desired state payload, live action and current-state validation, `setCheckOff`; completed parent keeps identity. Dated real launcher reversal evidence exists. |
| PL-050 | Checklist parent completion | **K:** ToggleHabit allowed; checklist child progress independent; whole-Habit completion follows existing domain semantics. Not equivalent to Task unfinished-Subtask blocking. |
| PL-051 | Checklist expand / collapse | **K:** Desired expansion plus current eligibility; item order, state and parent distinction retained. |
| PL-052 | Checklist child check / uncheck | **K:** Current expanded child exists and desired state differs; shared repository auto-completion rules, not separate widget arithmetic. |
| PL-053 | Count quick add | **K/F:** Logs exact current quickIncrement and refreshes; numeric meaning incomplete, PLAT-04. Dated one-add persistence remains accepted. |
| PL-054 | Decimal quick add | **F:** Same actual mutation as app but fixed 2 decimal formatter can display +0, PLAT-04. |
| PL-055 | AtLeast / AtMost / Exactly / Range | **F:** Source outcome is domain-derived; visible subtitle only `targetMin`, so upper/range/direction meaning lost, PLAT-04. |
| PL-056 | Daily / weekly / monthly / rolling progress | **K/F:** Actual value uses period bounds and source-unit conversion, but compact copy does not name period, PLAT-04. |
| PL-057 | Custom / built-in numeric units | **K/F:** Values computed using customUnits; row/action no unit symbol, PLAT-04. |
| PL-058 | Duration idle → Start | **K:** Current Start action must remain eligible, ID/UUID/request ID validated; durable timer owner handles actual start. |
| PL-059 | Duration Running → Stop | **K/F:** Exact UUID/session boundary, current day and row action; source persists timer log once. Snapshot elapsed is PLAT-06; hidden completed timer is PLAT-02. |
| PL-060 | Manual Duration entry | **K/F:** Row body opens exact Habit inspector, manual amount available in app; no separate widget amount editor. Recorded widget evidence is absent, PLAT-05. |
| PL-061 | Duration review after clock/reboot ambiguity | **K/F:** `timerNeedsReview` maps to Open/Review rather than unsafe Stop; Area handoff can block inspector, PLAT-01. |
| PL-062 | Timer outside Area / selection / paused / archived | **K/F:** Deliberate safety inclusion, existing unit contract; scope handoff and completed filter exceptions incomplete, PLAT-01/02. |
| PL-063 | Rating | **K:** Label Rate at normal text, Open action leads exact Habit inspector; no direct rating mutation or numeric default fabricated. |
| PL-064 | LogOnly / note-only Habit | **K:** Label Log and Open; actual authored note remains app responsibility. No one-tap empty log from widget. |
| PL-065 | Measurement-sourced Habit | **K:** Projected shared measurements counted, action ReadOnly/sync; no direct widget mutation; body opens exact Habit. |
| PL-066 | Habit body source versus execution action | **K/F:** Independent body/Open and direct trailing controls; out-of-Area timer exception PLAT-01. Normal body opens inspector, not silently mutating. |
| PL-067 | Habit quick-action menu / amount decrement | **N/K:** Widget supports configured quickIncrement only; correction and other amounts via app. Do not invent a widget amount chooser. |
| PL-068 | All complete / no scheduled / none selected | **K/F:** Distinct empty copy; completed-live timer can trigger misleading all-complete, PLAT-02. |
| PL-069 | Visible mutation failure | **F:** No per-widget authoritative error receipt, PLAT-09. |

### Widget cache, identity, privacy and accessibility

| # | Flow / state / transition | Current contract and disposition |
|---|---|---|
| PL-070 | Initial collection load | **K/R:** RemoteViews default loading view (`getLoadingView=null`); successful empty separate from RefreshError. Exact launcher loading indication not observed fresh. |
| PL-071 | First read failure | **K:** RefreshError first with no-cache explanation and retry fill-in. Provider header failures do not prevent binding/notification. |
| PL-072 | Later read failure | **K:** Last successful display rows and leading explicit retry; cached rows have no mutation/Open fill-in and are labeled saved items. |
| PL-073 | Cache freshness / old date | **R:** Timestamp persisted but not presented; generic “last saved” is honest, relative “Today/elapsed” cached meta lacks date. Assess stale-age usefulness; do not call saved rows current. |
| PL-074 | Retry read failure | **K:** Refresh action notifies the relevant collection; success swaps to current rows and cache. |
| PL-075 | Configuration changes during failure | **K:** PREF save invalidates former display snapshot; old scope cannot be relabeled under new header. |
| PL-076 | Cache corruption | **K:** Codec rejects invalid snapshot; fresh failure remains explicit instead of crashing the renderer. |
| PL-077 | Same action delivered twice | **K/R:** Boolean/expansion intents set desired state; timer request/session boundaries protect start/stop. Numeric widget adds represent deliberate taps, no persistent per-delivery ledger; rapid/duplicate host deliveries require runtime distinction. |
| PL-078 | Refresh after domain mutation | **K:** Application merges relevant Task/Habit/source/Area/calendar flows, debounces250 ms, updates all; provider action also refreshes. Exact cross-widget timing requires host evidence. |
| PL-079 | Process death / recreation | **K/R:** Preferences/cache persisted, factory rebuilds fresh source, timer domain durable. Dated Task/CheckOff/Count/Timer host receipts support those specific historical configurations only. |
| PL-080 | Reboot / clock / zone / day cutoff | **K/R:** Runtime maintenance time receiver refreshes relevant widgets; application calendar tracks logical day/zone/cutoff. Precise closed-process custom cutoff update latency not demonstrated. |
| PL-081 | Day/night theme | **K/R:** Widget resource palette follows system host configuration; config app host follows user theme via ExternalWhipActivityHost. Opposite themes are intentional platform boundaries. |
| PL-082 | RTL | **K/R:** Relative XML roles plus resolved logical cache padding; real launcher RTL traversal/numeric control order unresolved. |
| PL-083 | Decorative logo | **K:** No brand PendingIntent; source header owns labeled navigation. Preserve DEC-20260921-007 rather than reintroducing small duplicate actions. |
| PL-084 | Accessibility target / name | **K/F:** 48 dp controls and named action/body/expand; extreme hidden detail missing, PLAT-07. Absence of NAF is not a TalkBack speech pass. |
| PL-085 | Widget permission | **K:** Exported providers protected by `BIND_APPWIDGET`; collection services private with `BIND_REMOTEVIEWS`; mutable collection template explicitly targets own receiver. |
| PL-086 | Launcher/private data exposure | **K/R:** Home-screen widget intentionally displays user content in host; no in-app hide-sensitive-widget mode found. Lock-screen presentation/OEM/screenshot controls are host/runtime scope; no unsupported leak claim. |

### Reminders and native notifications

| # | Flow / state / transition | Current contract and disposition |
|---|---|---|
| PL-087 | Task reminder creation | **K:** TaskEditorDialog943 requests permission on enable and defaults missing offset to0; removing authored time removes reminder settings. Schedule/time validation remains domain-owned. |
| PL-088 | Habit default/weekday reminder creation | **K:** HabitScreens2688–2703 requests permission on first configured group; times normalize/deduplicate; visible summary and disclosed advanced schedule retained. |
| PL-089 | Goal reminder creation | **K:** GoalScreens2224–2230 uses common clock picker and permission request; Goal reminder type prompt differs by action. |
| PL-090 | Notification access first grant / declined | **K/R:** MA207–245 requests Android13+ permission from enabled use; Settings distinguishes unasked/rationale/permanent denial and repairs via system UI. Exact in-editor denial visibility not observed fresh. |
| PL-091 | Permanently denied / disabled app | **K:** SS1163–1236 explains block and app settings repair; no false “Deliverable” from permission alone. |
| PL-092 | Disabled specific channel | **K:** NotificationDelivery state distinguishes Whip configuration, global access and channel; SS1265 targeted channel repair under Troubleshooting. |
| PL-093 | Exact alarm permission allowed/denied | **K:** SS1178–1208 exposes precise timing separately; ReminderAlarmScheduler39–82 installs durable fallback first and retains due-time WorkManager when exact access absent. |
| PL-094 | Send test notification | **K/R:** Task channel missing can be created, blocked channel disables action, visible persisted outcome; one test channel does not certify Habit/Goal channels or actual timer scheduling. |
| PL-095 | Refresh reminder status | **K:** SS305–317/1321–1345 uses receipt/coordinator, retained error, exact failed admission warning; not a silent global repair. |
| PL-096 | Task notification display/Open | **K:** RN content title/source occurrence, Open intent original date and task ID; exact current source resolution waits for loading. Generic due copy omits full Task schedule, but expected action is exact task. |
| PL-097 | Task notification Complete | **K:** Current definition, exact occurrence, generation, stable UUID/fingerprint and current unfinished Subtasks validated inside coordinated transaction. |
| PL-098 | Task notification with unfinished children | **K:** Worker replaces Complete with Review subtasks; action opens exact source, never bypasses current child review. |
| PL-099 | Task completion Undo | **K:** Separate Undo notification with current validated completed occurrence; reopen exact recurring or single Task. No Habit Undo notification invented. |
| PL-100 | Task Snooze10 | **K:** Valid originating state/offset/fingerprint; schedule snoozed transport rather than add history; quiet-hours rules retained. |
| PL-101 | Habit notification CheckOff | **K/F:** Mark done exact logical date with claim/generation/idempotence; Open lacks matching date, PLAT-08. |
| PL-102 | Habit notification Count/Decimal | **K/F:** Six-decimal localized action, increment matches current definition and canonical source ID; logical day authored; Open date provenance missing, PLAT-08. |
| PL-103 | Habit Checklist/Duration/Rating/LogOnly reminder | **K/F:** No fabricated direct completion/amount; body Open + Snooze; prior-day context omitted, PLAT-08. |
| PL-104 | Habit synced reminder | **K:** No one-tap manual mutation; live shared evidence eligibility can cancel delivered reminder; body Open remains supported. |
| PL-105 | Habit Snooze10 | **K:** Complete versioned claim and current eligibility, no historical log, originating date preserved. |
| PL-106 | Goal notification display/Open/Snooze | **K:** Type-specific “Record/Review/Log” prompt, exact Goal ID, validated claim on Snooze; app opens current exact Goal inspector. No one-tap Goal completion fabricated. |
| PL-107 | Goal completed/archived/deleted/deadline changed | **K:** Live scheduler/worker claim validation suppresses obsolete delivery and rebuilds valid future state. App exact-target resolver can route archive/completed or unavailable. |
| PL-108 | Task/Habit paused/skipped/moved/ended/edit race | **K:** Definition-only fingerprints plus live cross-table truth, entity→state boundary and raw authoritative delegates; prior release contract retained. |
| PL-109 | Duplicate notification action / interrupted claim | **K:** NotificationActionLedger begin/complete/release; numeric Habit source ID gives repository idempotence; Task executeClaimedNotificationAction protects authoritative commit from failed followup. |
| PL-110 | Reminder read/write failure | **K/R:** Worker recovery retry/claim rejection, action leaves current alert or reconciles stale source; direct error speech/system presentation unobserved. Do not conflate with PLAT-09 widget errors. |
| PL-111 | Quiet hours enable/disable/edit | **K/R:** Settings typed save/error owns start/end, overnight label; schedulers adjust delivery instant while retaining source day. PLAT-08 affects Habit handoff, not adjustment itself. |
| PL-112 | Multiple close Habit reminder times | **K:** Up to 16 independent upcoming claims, quiet-window delivery dedup, succession anchored to delivered claim; preserves released owner-reported reliability repair. |
| PL-113 | Cold process exact wakeup/fallback | **K/R:** Exact receiver promotes same unique WorkManager path carrying claim/generation; application establishes recovery before WorkManager initialization. Existing dated cold-process receipt is not current device proof. |
| PL-114 | Boot / time / zone / package/access change | **K:** Manifest66–75 and runtime maintenance serialize rebuild, fixed-zone/follow-device distinction, private claim-version journal and widget refresh. |
| PL-115 | OEM idle / closely spaced allow-while-idle / force stop | **R:** Source has exact+fallback and Settings troubleshooting; Android/OEM delivery limits remain outside universal guarantee. No OEM matrix or long-idle test performed here. |
| PL-116 | Notification privacy / lock screen | **K/R:** Private receivers and immutable actions/content intents; system channel/access owns display. No explicit custom public/private-version policy found; actual lock-screen content follows device/channel configuration and is untested here. |
| PL-117 | Workout rest notification | **K/F:** Exact revision/deadline/session validates delivery and post-before-durable-clear recovers interruption; warm tap ignores Workout tab/session, PLAT-03. |
| PL-118 | Focus running/completed/Open/Stop notification | **K/R:** Existing generation/deadline/current-task validation and serialized lifecycle retained. Shared Focus auditor owns presentation details; this report does not duplicate those findings. |
| PL-119 | Track reminder notification | **N:** No Track reminder scheduler/channel or production notification producer found; existing internal OpenTrack action is not evidence of a Track reminder feature. |
| PL-120 | Scheduled automatic backup notification | **N:** No user-facing backup notification flow invented; backup outcome/repair handled in Settings. |

### Android entry, queue, files, Back and draft handoff

| # | Flow / state / transition | Current contract and disposition |
|---|---|---|
| PL-121 | Launcher ordinary opening | **K/R:** MAIN/LAUNCHER Manifest37–41, startup recovery gate before NormalWhipContent, configured opening Area/tabs authoritative. |
| PL-122 | Android static/dynamic/pinned app shortcuts | **N:** No shortcuts manifest metadata/XML or `ShortcutManager`/`ShortcutInfo` production registration found. Widget Add is a separate supported surface, not an app shortcut. |
| PL-123 | URI/browser/universal deep links | **N:** No VIEW/BROWSABLE intent filter. `whip://widget/...` is PendingIntent/service identity for explicit internal intents, not a public registered browser flow. |
| PL-124 | ACTION_SEND text/plain | **K:** Manifest42–46 and MA314–323 normalize standard CharSequence into bounded Task capture; no direct save before reviewed editor. |
| PL-125 | Styled text / Unicode / oversized share | **K:** SharedTaskCapturePolicy bounds raw 8192 code points/32 KiB, title 200 and 50 child lines; durable shortening warning rather than silent multi-megabyte saved state. |
| PL-126 | Blank / unsupported share type / file attachments | **K/N:** No editor for blank/unsupported payload; only text/plain receive contract. SEND_MULTIPLE/image/file-attachment capture absent. |
| PL-127 | Share while setup incomplete | **K:** LA blocks admission until setup/Area ready, retained Activity queue supplies action later; startup epoch reset drops unsafe old launches. |
| PL-128 | Multiple onNewIntent deliveries | **K:** MA LaunchRequestQueue FIFO, monotonic IDs, exact-head consume, no recomposition replay; only waiting Task shares capacity 4. |
| PL-129 | Share overflow | **K:** Counted retained overflow marker with exact delivery/count acknowledgment; widget/notification requests not capacity-dropped. |
| PL-130 | Widget Add Task while Task draft open | **K:** APP1251–1269 queues captured date and resolved Area; PendingTaskEditorLaunchState saveable; explicit Replace/Keep Editing, save/close releases queued request. |
| PL-131 | Share while Task draft open | **K:** APP1271–1289 retains text/shortening/Area, same authored draft conflict policy. Dated actual process death single-share evidence remains scoped. |
| PL-132 | Queued capture after original draft save changes Area | **K:** Frozen resolved Area, not current scope when admitted; existing `queuedWidgetAddTaskPreservesRequestedAreaAfterCurrentDraftSave` contract. |
| PL-133 | Keep Editing / Back from conflict | **K:** Pending dialog Back defers, does not erase waiting request; saving disables replace/discard controls. |
| PL-134 | New share same content intentional repeat | **K:** New delivery ID admits new request; consumed old ID not replayed by recomposition/recreation. |
| PL-135 | Activity state save/restore empty or nonempty queue | **K:** MA251–261/376–396 preserves exact sequence and delivery counter; intentional consumed empty queue does not re-read original Intent. |
| PL-136 | Existing entity loading/error | **K:** resolveLaunchTarget Pending waits; LoadFailed navigates to workspace but does not consume pending request; domain retry can resume resolution. |
| PL-137 | Existing entity deleted/unavailable | **K/R:** Exact ID unavailable message; Task missing occurrence distinct from whole missing entity. Recoverable transient feedback stored by shared coordinator, runtime interruption scope parent-owned. |
| PL-138 | Existing archived Habit/Goal/Task source | **K:** Unscoped target resolution sees archived/completed collections; inner source destinations explicitly chosen. Out-of-Area timer explicit scope is PLAT-01. |
| PL-139 | OpenTrack explicit internal request | **K/N:** MA/LA decode exact Track ID, wait load and select app Track inspector. No widget/notification producer/public URI registration found; test/internal route only in inspected integration. |
| PL-140 | Widget Add Habit while Habit editor open | **R:** APP1291 sets create flag; HabitScreens326–330 sets `creating=true`; existing editor uses saveable authored state. No Task-style generalized pending handoff is present. Verify no stacking, accidental creation mode or draft overwrite across actual host launch. No confirmed data-loss claim from source alone. |
| PL-141 | Notification/Open while another domain editor open | **R:** Most source commands switch appDestination and select exact ID; shared saveable workspace/editor ownership can retain drafts, but Task conflict queue applies specifically to incoming capture. Cross-domain back/draft restoration needs actual warm-task evidence. |
| PL-142 | Back from external source editor/inspector | **K/R:** Feature editor dismissal/unsaved guards and Activity back remain their owners; incoming platform request does not fabricate a Review return stack. Scope persistence is established product behavior. |
| PL-143 | Portable backup file tapped outside Whip | **N:** Manifest has no ACTION_VIEW backup MIME/file route. Restore is initiated inside Settings through SAF. |
| PL-144 | Settings SAF backup import/export | **K/R:** SS283–291 launch CreateDocument/OpenDocument/OpenDocumentTree; previewRestore owns picked import, no immediate data replacement. Existing preview/confirmation/recovery contracts belong Settings component. Picker grant/cancel/recreation unresolved fresh. |
| PL-145 | Track CSV SAF import/export | **K/R:** TrackScreens385 uses OpenDocument from app; there is no external file-view auto-import registration. Native picker and exact target selection part of Track scope. |
| PL-146 | Workout outbound share | **K/R:** GymScreens uses ACTION_SEND text/plain, user-initiated Android chooser. No automatic external transmission or recipient messaging introduced. |
| PL-147 | Replace/reset data while external request queued | **K/R:** Epoch barrier clears unsafe launches; Compose UserDataGenerationBoundary clears inner authored state. Non-Focus pending entity opens do not retain UUID/generation in LaunchRequest—see unresolved scope below. |
| PL-148 | External widget config recovery / theme | **K:** ExternalWhipActivityHost uses common startup and WhipActivityTheme, preserving user-selected app chrome; widget palette remains system day/night. |

## Justified keeps and prior evidence reconciliation

1. Preserve the Task-versus-Habit checklist difference: Task completion must review unfinished children; Habit checklist whole-period completion is an existing explicit action. Current widget receivers reflect these distinct domain meanings, not an accidental inconsistency.
2. Keep row identity, body/Open, execution action and expand as distinct roles. Do not make the decorative brand clickable again; dated real launcher NAF findings were specifically repaired under DEC/FND/IMP/VER-20260921-007/010.
3. Keep timer UUID/request/session validation and the intentional archive/pause/Area/selection escape for active sessions. PLAT-01/02 complete that contract, rather than replacing it with normal filtering.
4. Keep readonly stale snapshots, bounded 100-row cache, explicit failure-first retry, generation invalidation and independent widget preferences. Displaying stale data does not authorize acting on it.
5. Keep minimum-size extreme-text geometry, hidden brand/subtitle, icon-only controls and 48 dp targets. PLAT-07 repairs alternate meaning rather than restoring cramped labels.
6. Keep the September 15 reliable reminder repair: exact alarm wakeup plus the already-validated WorkManager transport, independent upcoming Habit claims, live state fingerprints, lifecycle rebuild and platform-limit copy. No new reliability regression was established.
7. Keep Task notification child review, exact occurrence mutations and current claim idempotence; Goal reminders remain review/log entry points with Snooze, not invented completion actions.
8. Keep bounded shared-text capture and durable FIFO/overflow/conflict admission. Do not remove persistent shortening or queue notices for cosmetic simplicity.
9. Keep explicit widget Area persistence (FND-20260929-019's repaired Today tab), configurable opening Area behavior and current archive fallbacks. Entity-opening safety exceptions must not change ordinary header/Add behavior.
10. Keep Android backup disabled for automatic platform data extraction and use user-initiated portable backup/SAF flows. Manifest is not a public file auto-import handler.

Fresh parent-supplied native evidence: `PlatformEntrySurfaceE2ETest#widgetDoesNotShowATemporaryBannerForTheOnlyVisibleArea` **PASS 1**, zero failures/skips/reuse, current baseline on disposable Android 34 `emulator-5556`, bounded at 55 seconds. [Aggregate receipt](../../../artifacts/coherence-audit/2026-09-29/fresh/platform-area-entry/android-aggregate.tsv); [result XML](../../../artifacts/coherence-audit/2026-09-29/fresh/platform-area-entry/result.xml), copied from `build/instrumentation-results-bIUDm1`. This reconfirms the single visible Area/Today entry without a false temporary banner (PL-013 and PL-138 normal case). It does **not** cover out-of-scope timer Review, completed active-timer filtering, rest-alert warm routing, logical-day notification handoff, widget numeric rendering, or the other newly identified findings. This agent did not invoke that device run.

Memory read as a map: full INDEX, relevant entries FB-20260915-001; FND-20260929-019, FND-20260921-007/005, FND-20260831-008/014; DEC-20260921-007, DEC-20260915-001, DEC-20260831-009/010 and DEC-20260901-030; IMP-20260927-008, IMP-20260921-009/010/011, IMP-20260915-001; VER-20260921-009/010/011 and VER-20260915-001; linked dated widget receipts. These historical entries establish prior decisions/results within their recorded scope; source rechecked above is current audit evidence. The prior Count/Timer “no defect reproduced” receipt concerns successful actions/persistence on one host and does not certify precision/unit/range/manual-duration/cross-Area/extreme-text/elapsed semantics.

Personally inspected originals (existing evidence only):

- **2026-09-27:** [timer-stopped.png](../../../artifacts/astra-audit/2026-09-27/habit-widget-modes/run-api34/timer-stopped.png), confirms repeated Start label despite saved duration described in its receipt.
- **2026-09-27:** [count-once.png](../../../artifacts/astra-audit/2026-09-27/habit-widget-modes/run-api34/count-once.png), confirms clear Count “1 of 1” and independent +1; also idle Duration repeated Start. No clipping judgment extrapolated to fresh 200% or other hosts.
- Read [2026-09-27 Count/Timer receipt](../../../artifacts/astra-audit/2026-09-27/habit-widget-modes/README.md) and [2026-09-21 Task launcher receipt](../../../artifacts/astra-audit/2026-09-21/widget-launcher/README.md). Their exact PID/canonical-log claims are dated receipt evidence, not actions repeated by this agent.

## Unresolved runtime scope and smallest useful follow-through

- **Queued identity across replacement:** Ordinary Task/Habit/Goal/rest content opens carry IDs without stable UUID/generation (Focus separately validates both timer/current generation). MA's saveable LaunchRequest does not preserve generation. Notification cancellation and epoch gating reduce exposure, but delayed accepted intents around replace/reset should be exercised before declaring an aliasing defect. Readonly source opens are distinct from already-guarded background mutations.
- **Actual Android Activity task behavior:** The audit traced `acceptLaunchIntent/onNewIntent` and FIFO, but does not prove every sender/launcher routes into the existing Activity. Standard launchMode, CLEAR_TOP without SINGLE_TOP in some widget routes and OS task restoration need actual host observation. Do not infer draft loss from task flags alone.
- **Cross-day freshness:** Rendered-date mutation guards are strong; closed-process custom cutoff/fixed-zone widgets have periodic/broadcast refresh latency. Reproduce stale header Add versus intended Today date before changing capture semantics.
- **No new all-platform acceptance:** OEM Samsung launcher, Android 26/34/37 widget hosts, reboot/restored-widget-ID migration, extreme text, RTL, TalkBack spoken output, lock-screen privacy, alarm quotas, notification dismissal, SAF grant loss and cross-domain unsaved-editor returns were not freshly executed here.
- **Proportionate verification for remedies:** One affected stable `scripts/check --ready` batch, then exact selected widget/external/notification native journeys; all routine commands bounded by `timeout --kill-after=3s 55s COMMAND...`. The parent's exact `PlatformEntrySurfaceE2ETest#widgetDoesNotShowATemporaryBannerForTheOnlyVisibleArea` pass reconfirms the repaired Today/Area keep, but cannot certify the newly identified exceptions. New assertions should cover the root contract rather than mirror private formatter implementation.

No app change is proposed for capabilities marked N. No new P0/P1 correctness or privacy conclusion is justified by this source-only component audit. The P2 findings describe concrete information loss, recovery/handoff failures or ambiguous mutation outcomes and each has a bounded verification proposal.
