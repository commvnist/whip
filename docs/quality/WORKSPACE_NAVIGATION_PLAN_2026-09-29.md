# Workspace tabs and headers — redesign implementation

Status: **Implemented, verified and released in 0.3.87/code 93.** The original plan-only request was followed by explicit implementation authorization under FB-20260929-008, emphasizing stable visual transitions. Source baseline `d2817658`; the initial private phone release is 0.3.86/code 92 under VER-20260929-009. FB-20260929-009 resumes complete testing and a second release; FB-20260929-010 selects Tasks/Habits initially instead of Today. The complete test inventory and repair replays are accepted under VER-20260929-010; the second phone installation is accepted under VER-20260929-011. Related: FB-20260929-007/008, FND-20260929-013, DEC-20260929-003, IMP-20260929-010.

## Implementation disposition

- Steps 1–5: implemented in existing route, settings, projection and screen owners. Shared `WhipWorkspaceHeader` pins a context row with content-independent height and stable trailing action slots; tabs remain in the existing shared owner. Compact Task scope/layout menus sit in scrolling content so they do not add fixed rows above only one workspace. Wide Track collection chrome spans both panes.
- Task collection uses authored entities and preserves unprojected series. Collection recurring rows open definition editing; dated layouts keep occurrence actions. Today filters cannot change date scope. Apply/Cancel, per-route saved filters/scroll and explicit Saved Views are implemented; legacy route names remain decoding aliases, with consistent Unscheduled display language.
- Habits use distinct management rows while preserving active timer recovery; Goals retain lifecycle partitions; Tracks retain per-log detail and workspace Activity, with one global Area scope. Archive child routes restore originating views. Gym keeps its four jobs and uses Insights terminology.
- Step 6: complete testing is resumed. Current native fixtures assert the collection defaults, visible scope/archive routes and identical toolbar/tab/context bounds across primary destinations. Catalog discovery is reconciled to 530 required captures. All 1,268 native methods have passing campaign results, including rendered geometry and restoration assertions; 724 JVM methods, lint, coverage, builds and nine benchmark/profile scenarios also pass. Selected fresh originals are reviewed; this does not claim manual review of every catalog state. [Initial build/source evidence](../../artifacts/workspace-navigation/2026-09-29/README.md); current campaign is VER-20260929-010.

## Recommendation

Use three destinations for each productivity workspace. Give the collection the workspace's name, keep daily execution only where the domain supports it, and put analytics last. Archived items become a consistent secondary destination reached from More. Gym retains four destinations because training execution, authoring, historical receipts and analysis are distinct jobs.

| Workspace | Proposed tabs, in order | Default on first entry |
| --- | --- | --- |
| Tasks | **Tasks · Today · History** | Tasks |
| Habits | **Habits · Today · Insights** | Habits |
| Goals | **Goals · History · Insights** | Goals |
| Tracks | **Tracks · Activity · Insights** | Tracks |
| Gym | **Workout · Library · History · Insights** | Workout |

Fresh entry selects the first collection tab, per FB-20260929-010. Preserve explicit Home/widget/search targets and the last tab within the session. Keep tab order fixed, independent of counts or previous selection. Do not add blank or invented tabs to make every workspace have the same number.

## Baseline analysis before implementation

| Workspace | Current responsibilities and confirmed friction |
| --- | --- |
| Tasks | Today contains scheduled work through today, including carried-over work; Inbox contains undated Tasks; Upcoming holds future scheduled occurrences and List/Agenda/Calendar modes; History contains Completed and Archived sections. There is no complete active Task collection. The same filter dialog offers Any Date, Scheduled Today, Past Scheduled Date, Next 7 Days and No Scheduled Date inside Today and Inbox. Filtering happens **after** destination selection, so Any Date cannot broaden Today. Shared remembered criteria can survive tab changes. |
| Habits | Today contains scheduled Habits plus unresolved running timers; All Habits contains the nonarchived collection, including paused/ended configurations and the timer-recovery exception. Archived is maintenance; Insights contains historical patterns. All Habits and Today currently reuse execution-oriented rows, which weakens their different purposes. |
| Goals | Active includes active **and paused** Goals. History contains completed **and abandoned** Goals, not measurement-entry history. Archived is separate storage state. Insights covers active/paused Goals' trends, pace and data quality. A Today tab would misrepresent ongoing, elapsed and other Goal types. |
| Tracks | Tracks manages log definitions; Activity combines entries across active Tracks; Archived stores inactive definitions; Insights summarizes entry frequency and field values. Each individual Track separately has Entries/Insights/Options. Workspace analytics and a single Track's analytics have different scopes. |
| Gym | Four existing primary destinations already serve distinct jobs: Workout, History, Progress and Library. Library owns Routines, Exercises, Machines, Categories and Tools. Tools is not currently a fifth primary tab. Keep those jobs accessible instead of forcing Gym into three. |

There are two additional systemic problems. Saved Task Filters also restore destination, layout and Area, so selecting one can navigate while appearing to apply a filter. The Task dialog currently applies edits immediately and closes with Done, whereas the owner's mental model is an Apply operation. Also, the shared tab/header components do not establish a shared layout: Tasks pins its title and additional segmented rows above the list, while several other domains put titles inside their scrolling content. Repeated tab titles, variable subtitles and different fixed-row counts produce inconsistent page starts even when the same components are used.

Source owners: [Task routes/date rules](../../app/src/main/java/com/whip/app/ui/TaskWorkspacePolicy.kt), [Task projections](../../app/src/main/java/com/whip/app/ui/TaskViewModel.kt), [Task filters/root toolbar](../../app/src/main/java/com/whip/app/ui/WhipApp.kt), [saved navigation](../../app/src/main/java/com/whip/app/core/PowerUserSettings.kt), [Habits](../../app/src/main/java/com/whip/app/ui/HabitScreens.kt), [Habit scheduling](../../app/src/main/java/com/whip/app/ui/HabitViewModel.kt), [Goals](../../app/src/main/java/com/whip/app/ui/GoalScreens.kt), [Goal status partition](../../app/src/main/java/com/whip/app/ui/GoalViewModel.kt), [Tracks](../../app/src/main/java/com/whip/app/ui/TrackScreens.kt), [Gym](../../app/src/main/java/com/whip/app/ui/GymScreens.kt).

## Destination contracts

### Tasks: collection, execution, receipts

**Tasks** becomes the complete collection of unfinished, nonarchived Task definitions, including undated, future, overdue and recurring Tasks with no occurrence in the current projection window. Derive it from `taskEntities`; concatenating Inbox/Today/Upcoming would omit some series and duplicate others. The default List shows one row per authored Task with its schedule/next actionable occurrence. For recurring series, collection actions manage the series; occurrence completion remains explicitly tied to a dated occurrence. Never let a collection checkbox ambiguously complete a whole series.

The collection's compact scope selector offers **All Tasks / Unscheduled / Upcoming**. This is one selector backed by the filter state, not another tab or repeated chip strip. All Tasks is the default; Unscheduled takes over Inbox's undated collection; Upcoming preserves the existing future-work shortcut. Other schedule criteria belong in Filters, with an explicit Custom summary. Keep schedule dates and deadlines separate: a deadline does not make an undated Task scheduled today.

Retain List/Agenda/Calendar in a compact, labeled view menu rather than another full-width segmented bar. Agenda/Calendar display dated occurrences, not Task definitions. Display the visible window and occurrence count; show a clear route to excluded undated Tasks. Preserve the existing 30-day recurrence/calendar bound and availability of distant one-off Tasks in List. Switching to a dated layout from Unscheduled explicitly selects a dated scope; never silently show an empty calendar while claiming Unscheduled. Keep the existing optional Habit overlay. This is a meaningful representation change, not a promise of identical row counts.

**Today** is fixed to actionable work scheduled on or before the current logical day. Distinguish **Today** and **Carried over** in the content; label missed schedule dates separately from overdue deadlines. Retain Plan Today, execution, Focus, quick capture and recurrence/backlog policy. No Any Date, Tomorrow, Next 7 Days or No Scheduled Date filters here. Permitted filters only narrow this queue: priority, tag, effort, duration, pinned and deadline attributes. Clear Filters restores Today's full queue in the current Area, never all Tasks. Plan Today explicitly selects from the unscheduled collection.

**History** shows completed Tasks and completed recurring occurrences, with completion-date filters and reopen/correction actions. Move Archived out of its segmented control to More → Archived Tasks. Retain existing archived-history visibility rules; this redesign must not alter historical facts or fabricate missing occurrences.

### Habits: configuration, today's check-ins, patterns

**Habits** replaces All Habits. Show authored schedule, mode and lifecycle state so it is a useful management view: edit, pause/resume, reorder and open original History. Daily completion counters belong primarily in Today. Templates live in More and the creation flow. Preserve an explicit log/backfill route from Habit details; management must not accidentally record an off-schedule check-in.

**Today** remains scheduled check-ins, values, checklists and timers, with finished work collapsed as appropriate. Weekly/monthly flexible Habits must retain period targets rather than imply a daily obligation. Running-timer recovery remains reachable even when paused, ended, archived or not scheduled today. A filter cannot change the date or introduce nonscheduled Habits.

**Insights** retains genuine historical patterns, cadence, trends and links to original logs. Do not create a workspace History tab that merely duplicates every Habit's existing History inspector. **More → Archived Habits** remains available from every root tab, including empty collections.

### Goals: ongoing work, outcomes, analysis

**Goals** replaces Active, showing active and paused Goals with clear state labels. Preserve progress entry, milestone completion, elapsed values and complete/pause actions. Keep paused Goals discoverable; changing a label must not change membership.

**History** keeps completed and abandoned Goals, with distinct outcome labels and outcome-date filtering if introduced. It is not the place for individual measurement records; those remain in each Goal's retained History inspector. **Insights** retains trends, pace and data quality, explicitly scoped to ongoing Goals in the selected Area. Move archived definitions to **More → Archived Goals**. No Today view and no artificial daily-goal score.

### Tracks: definitions, records, analysis

Keep **Tracks / Activity / Insights**. Activity earns a primary destination: it retrieves and corrects entries across logs, while the collection manages definitions and Insights interprets them. Do not rename Activity to Today or silently exclude older entries. Its date filter is appropriate and defaults to Any Date; keep Track/field criteria relevant to the available records. Use the global Area selector rather than another redundant local Area control at the workspace root.

Move Archived to **More → Archived Tracks**. Preserve focused Track Entries/Insights/Options and compact Back/wide master-detail behavior. Workspace Insights is explicitly aggregate; Track Insights names the selected Track. Entry correction/deletion must retain the parent, date scope, filters and loaded older history.

### Gym: retain four meaningful jobs

Use **Workout / Library / History / Insights**, with the existing Progress content relabeled Insights for consistent analytical language. Keep Workout as the execution default and Library's existing child destinations. Do not flatten its library into more primary tabs or move live workout tools into generic filters. Apply the shared root header layout; preserve dedicated active-workout/editor chrome and original workout receipts.

## Shared header and control ownership

Use the existing global toolbar, then one destination row, then one compact context/actions row:

```text
Area selector                         +   Search   Settings
Tasks                    Today                    History
Tue 29 Sep · 5 remaining                    Filter     More
----------------------------------------------------------
Scrollable content, including any active-filter summary
```

This is a structural sketch, not a rendered mockup. Reuse the existing app palette, typography, spacing and selected-tab treatment.

- **Global toolbar:** one Area selector where applicable, one contextual Add, the existing top-right Search, and Settings. Preserve current expansion controls on wide layouts. No local search icon or always-visible find box. Search retrieves domain items independently of the current list filter; results show status and navigate to the correct destination.
- **Tabs:** destinations only. No filter buttons, creation commands, archive maintenance or numeric badges that change label width. Use the same height, baseline, padding and selected indicator. Scroll accessibly at narrow widths/large text instead of squeezing or truncating labels.
- **Context row:** useful scope/date/count information; optional compact scope/layout selectors where meaningful; Filter and More in stable trailing slots. Remove redundant large headings such as Today under the already-selected Today tab and generic explanatory subtitles. Root headers stay outside scrolling content consistently. At large text, the shared responsive layout expands without clipping or hiding actions.
- **Filter:** only criteria supported by the current destination. No fake filter icon on pages with nothing to filter. Reserve its trailing layout space so More does not shift, without adding an empty accessibility target. Active criteria appear in the same shared summary component inside the scrolling body, not as arbitrary extra fixed header rows.
- **More:** stable workspace maintenance/actions: Archived, relevant Templates, Saved Views for Tasks, Select and Reorder where supported. Keep disabled/inapplicable actions truthful. Selection/reordering replaces the normal context row with a clear mode and Done; do not stack another competing toolbar. Reorder is explicit, not a side effect of picking a sort order.
- **Creation:** keep the contextual + as the shared full-creation entry. Task inline quick capture remains a deliberately faster content workflow using the same persistence path, not another header Add button. Show the resulting schedule: Today creates for today; general collection capture is undated unless the user explicitly chooses or types a date. Do not infer creation dates from arbitrary filters.

Archives open as proper child pages with a Back action, clear Archived title, restore actions and retained original detail/history access. Returning restores the originating tab, scroll and filters. Archiving must not force the user out of a still-valid inspector unnecessarily. Empty pages keep navigation and archive discovery available.

## Filters and saved views

1. Give each destination its own remembered filter/sort/layout/scroll state. Area remains the existing explicit global scope. Cross-tab navigation must not carry a hidden schedule filter into Today or completion filters into the collection.
2. Make filter editing transactional: draft choices and a matching-count preview, **Apply**, **Cancel**, and **Reset filters** affecting the draft. Back/cancel leaves the list unchanged. Applying filters never navigates to another tab. Separate sort/group controls within the same surface; resetting filters must not unexpectedly reset authored order.
3. Rename the current Saved Filters concept to **Saved Views**, reached through More. Each saved view visibly names its destination, optional layout and Area. Opening one is explicit navigation. Keep saved text criteria visible and editable as criteria; do not add another collection search control.
4. Normalize old settings: Inbox → Tasks/Unscheduled; Upcoming → Tasks/Upcoming with its prior layout; Completed → History; Archived → the archived child page; Today → Today with valid narrowing criteria. Preserve the saved name, legitimate criteria, Area and ordering. Surface any removed incompatible schedule criterion when opening a legacy Today view instead of silently claiming equivalent results.
5. Update Home shortcuts, chosen opening destinations, widgets, unified-search routing, Back behavior and restored navigation together. Legacy Home filters restricted to Today must also discard impossible schedule restrictions while retaining valid narrowing. Preserve item IDs, dated occurrence identities, logs and backup data; route/settings decoding requires compatibility work, not a domain-data rewrite.

## Implementation sequence and acceptance

| Step | Concrete deliverable | Acceptance when verification resumes |
| --- | --- | --- |
| 1. Define route/scope contracts | Destination membership, filter capabilities, saved-view decoding, exact legacy mappings and Task entity-versus-occurrence rules in existing policy owners. | Every existing entry route resolves; Today cannot accept an out-of-scope schedule criterion; repeating/far-future/undated Tasks remain reachable. |
| 2. Establish shared root header | Compose existing `DestinationTabBar`, `WhipPageHeader`/action primitives and active-filter owner into a small shared workspace header. Domain content and filtering stay in their feature owners. | Stable toolbar/tab/context baselines and action anchors across all four workspaces, including empty state, long names, folded/wide and large-text layouts. |
| 3. Reorganize Tasks | Add real collection, preserve planning modes, scope Today, separate receipts/archive, transactional filters and explicit Saved Views. | All-date collection, fixed Today, recurrence-safe actions, date-vs-deadline behavior, cancel/apply, per-tab restoration and legacy saved-view journeys. |
| 4. Apply to Habits, Goals and Tracks | Three-tab routes, archive child pages, purposeful management rows and consistent control ownership. | Timers/flexible periods, active/paused/abandoned Goals, Activity date scope, original records and repeated history correction remain intact. |
| 5. Align Gym and routing | Shared root header, four retained jobs, Insights label, search/Home/widget/Back/creation updates and accessible names. | Live workout/editor retention, library reachability, exact receipt routing and no duplicated search/create/filter ownership. |
| 6. Inspect focused rendered journeys | Updated navigation/source/catalog contracts plus small, selected behavioral and visual checks. | Fresh comparison of identical seeded states; no label/count-dependent header jumps; full controls visible at actual large text; no private-phone instrumentation. |

Steps 1–5 are implemented; Step 6 remains pending while verification is paused. When resumed, use exact affected JVM/native methods under the established 55-second routine bound and inspect fresh rendered transitions before claiming visual acceptance. The navigation acceptance above does not complete the separately paused full-suite campaign.

## Alternatives rejected

- Four tabs everywhere: fills space with low-frequency maintenance and creates false equivalence between Today, lifecycle state and analytics.
- Two tabs everywhere: hides frequently used receipts/Activity/Insights in menus and increases navigation cost without simplifying their actual jobs.
- Merely rename Inbox to Tasks: still excludes scheduled work and leaves the Any Date promise false.
- Put Today on Goals or Tracks: imposes a daily obligation on domains that do not define one.
- Keep Upcoming as a fourth Task tab: duplicates the full collection's schedule scope and gives planning layouts their own competing navigation hierarchy.
- Merge archives with completion History: confuses reversible storage state with actual outcomes and is inconsistent across domains.
- Cosmetic tab changes alone: leave filter-state leakage, saved-view navigation and differing pinned header layouts unresolved.
