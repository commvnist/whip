# Whip UX implementation meeting decision

Date: 2026-08-23  
Input: [Full-app live UX, QA, and design audit](FULL_APP_UX_QA_DESIGN_AUDIT.md)  
Status: implemented; automated acceptance complete on 2026-08-23

## Implementation result

The decision below is now reflected in the app and its regression suite:

- Tracks is a direct fifth module in compact, Fold, expanded, tabletop, keyboard,
  Search, deep-link, global-create, Home, backup, and release paths.
- Track, Field, and Entry editing is owned by the root editor host, uses coherent
  `SavedStateHandle` drafts, protects dirty work, and suppresses the background
  shell from accessibility while foreground editing is active.
- Shared destination, Pages, action, disclosure, filter, form, and responsive
  page-header patterns replace hidden peers and visually ambiguous controls.
- Home, Search, Review, Settings, Areas, Task placement/repeat, Goal measurement,
  notification truth, and Gym calculators/workout controls follow the hierarchy
  and wording decisions recorded below.
- The pre-release Room schema has an explicit baseline with no destructive
  fallback; Track data, automation, backup, Search, and deletion are first-class.
- High-risk Track/editor/navigation responsibilities are separated into focused
  files and shared policies/components. Lint is clean and behavior is guarded
  before any further mechanical file-size work.
- The local gate covers 431 product tests: 206 JVM tests and all 225 Android
  instrumentation tests. Instrumentation runs only on a disposable API 34+
  emulator and uses bounded class lifecycles for host-emulator stability.
- Release APK/AAB identity, version 15, retired-location permissions, Play Store
  assets, lint, minification, and the benchmark harness are checked locally.

The fresh live compact/Fold evidence used to make the decisions is retained in
`screenshots/raw`, `screenshots/report`, and `ui-dumps` beside this document.
Quantitative first-use thresholds and the owner-device physical smoke matrix are
release-validation activities; destructive instrumentation remains prohibited on
the owner's phone.

## Participants and decision process

- **UX lead — Laplace:** information architecture, interaction hierarchy, progressive disclosure, novice/power-user balance.
- **Design lead — Archimedes:** visual system, adaptive geometry, editor presentation, responsive/accessibility acceptance.
- **Product philosopher — Russell:** Whip's purpose, ontology, honesty, and protection against feature sprawl.
- **Chair — Codex:** reconciled disagreements against the user's explicit requirement and current source constraints.

Each lead reviewed F-01 through F-17 independently. The first round agreed on the fixes but proposed keeping Home in the compact bottom navigation and placing Tracks in a More Destinations menu. The chair challenged that position because the user explicitly requires Tracks to be first-class and not hidden. After rebuttal, all three leads changed or confirmed their vote for the architecture below.

This document is the decision of record. Future implementation should not re-open settled choices unless live evidence fails an acceptance threshold recorded here.

## Unanimous product model

Whip is a private, local-first loop between intention, action, evidence, and reflection. It is not a general-purpose personal database.

| Surface | User question | Product ownership |
|---|---|---|
| Home | What needs my attention now? | Cross-domain daily overview and quick execution |
| Tasks | What finite action do I intend to finish? | Placement, dates/deadlines, recurrence, subtasks, completion |
| Habits | What behavior do I practice repeatedly? | Cadence, check-ins, period outcomes, reminders |
| Goals | What outcome am I trying to reach? | Targets, starting state, progress meaning, deadlines |
| Tracks | What happened, and what structured facts should I retain? | Reusable dated evidence with user-defined Fields |
| Gym | What am I training right now? | Sessions, exercises, sets, equipment, rest, training history |
| Areas | Which life context does this belong to? | Cross-domain organization, never behavior |
| Search | Where is the exact thing I need? | Cross-domain retrieval and routing |
| Review & Trends | What changed, and should I adjust? | Interpreted outcomes, not raw logging volume |

Tracks remains bounded. Keep typed Fields, Entries, descriptive Insights, explicit Goal/Habit automations, Areas, Search, CSV, and backup. Do not add formulas, relational tables, Kanban states, scheduled Entries, implicit streaks, arbitrary dashboards, or targets inside Tracks. Those belong to Tasks, Habits, or Goals. Gym remains purpose-built and is not reimplemented as a Track template.

## Settled first-class navigation architecture

### Compact

```text
[Whip/Home]  [Current destination · Area]       [Search] [Settings]

 Tasks        Habits        Goals        Tracks        Gym
```

- The five labeled bottom destinations are exactly **Tasks, Habits, Goals, Tracks, Gym**.
- The persistent Whip mark becomes the Home control. Home is an overview, while the five bottom items are peer working modules.
- Tracks is never placed under Add, More, Settings, Organization, or an unlabeled overflow as its primary route.
- Home active: the Whip control has the same selected-container treatment as module navigation; no bottom module is falsely selected.
- Module active: its bottom item is selected and the Whip control becomes **Go to Home**.
- The Whip/Home target is at least 48 dp, focusable, keyboard/D-pad reachable, and announced as **Home, selected** or **Go to Home**.
- A one-time non-blocking coach mark may say **Tap Whip to return Home**. It supplements the visible selected treatment; it is not the only affordance.
- Android Back at a module root returns Home. Back on Home follows normal system behavior.
- Settings stays a standard global gear. Areas stays in the Area scope control. Review & Trends stays a clearly labeled Home utility. No generic More menu is needed to hide core navigation.

### Book Fold and expanded rail

- The top rail/Home control is the Whip mark with a small visible **Home** label where space permits.
- Below it, show Tasks, Habits, Goals, Tracks, Gym with identical icon, label, selected container, spacing, and semantics.
- Tracks is a direct peer at every expanded width.
- Search remains global and scopes to the current module by default.
- Settings may be pinned as a utility at the rail bottom or remain the same global gear, but must not be visually confused with the module list.

### Tabletop

- Keep Whip/Home persistently reachable in the app bar/support region.
- The horizontal navigation contains the same five module destinations in the same order.
- Do not reintroduce a separate hidden route for Tracks.

### Keyboard and routing

- `Ctrl+H`: Home.
- `Ctrl+1` through `Ctrl+5`: Tasks, Habits, Goals, Tracks, Gym in visible order.
- `Ctrl+K`: scoped Search.
- `Ctrl+N`: contextual create.
- Global Add → Track opens the Track editor; successful creation lands in Tracks with Tracks selected.
- Search/deep link to a Track or Entry selects Tracks before routing to detail.
- Closing a Track editor returns to its exact origin; a successfully created Track opens in Tracks.
- Update first-run text, keyboard help, guides, and tests. The current four/five-section language becomes invalid.

### Home treatment after Tracks becomes direct

- Do not add a permanent, full empty Tracks card merely for discovery; persistent navigation now does that job.
- Add Tracks to `HomeSection` so users can order/hide Track content consistently with other modules.
- When relevant, render a compact **Quick Log** section containing pinned Tracks, pending Entry prompts, and explicit **Add Entry** actions.
- An all-zero Home has one calm summary and one compact creation/navigation cluster, not separate zero cards for every module.
- Split any current reuse of `HomeSection` for Review analytics into a separate `ReviewSection`. First-class navigation does not mean raw Track Entry count is a productivity score.

## Settled root surface and editor architecture

### Ownership

Create one root `EditorHost` above `AdaptiveNavigationFrame`. Feature lists/details emit typed editor requests; they do not mount full editors inside their own layout trees.

Suggested route model:

```text
EditorRoute.Task
EditorRoute.Habit
EditorRoute.Goal
EditorRoute.Track
EditorRoute.TrackField
EditorRoute.TrackEntry
EditorRoute.Exercise
EditorRoute.Machine
EditorRoute.Routine
```

The host owns:

- full-window foreground background and system-bar appearance;
- Fold/content-pane geometry;
- safe-drawing and IME insets;
- focus trap and accessibility suppression of the underlying app;
- Back/Escape and dirty-discard behavior;
- visible Close, title, and Save/Create controls;
- child pickers and confirmations anchored to the editor pane.

Remove `paneDialogModifier`/94%-width ownership from full editors. Short confirmations, date/time pickers, menus, and destructive decisions may remain dialogs or anchored sheets.

### Geometry

- **Compact:** the editor fills the app window. Global top bar, bottom modules, and FAB are absent and absent from accessibility traversal.
- **Book Fold:** the root overlay paints the full foreground/system-bar surface, dims and disables the support pane, and gives the editor 100% of the content pane. It never inherits a domain's nested list/detail split.
- **Narrow Fold/high font scale:** if the content pane cannot satisfy the editor's measured minimum width, expand intentionally across the window. Never squeeze columns or clip horizontally.
- **Tabletop:** use the unobstructed region after hinge and IME geometry; never straddle the horizontal fold.
- **Expanded non-Fold:** use one intentionally measured editor surface, capped only by an explicit editor policy—not a modifier passed through a feature workspace.

### Shared editor grammar

All primary editors use one `EditorScaffold` contract:

- fixed editor top bar with Close/Back, title, and filled high-emphasis Save/Create;
- one scrollable body and bottom clearance of at least 96 dp;
- immutable ViewModel/SavedState draft;
- explicit dirty baseline and discard confirmation;
- inline validation after interaction or attempted save;
- form order: intent → essential values → consequences → optional details;
- at 200%/320% text, paired controls stack instead of clipping.

An enabling control immediately owns every setting it reveals. One-of-many uses single-choice semantics; booleans use switches; filters use chips; disclosures use chevrons; child pages use navigation rows.

## Page-level navigation contract

Delete invisible horizontal scrolling as the only route to a page. Introduce an adaptive peer navigation model with explicit primary and secondary destinations.

- Compact normally shows three high-frequency peers; four are allowed only when measured width/font scale fits.
- Secondary pages live in the page overflow under a labeled **Pages** group, separate from actions.
- When a secondary page is active, it replaces the lowest-priority visible tab and appears selected. The displaced peer moves into Pages.
- The global Whip/Home control, global Search, page overflow, Filter, and disclosure chevron remain visually and semantically distinct.

| Workspace | Visible peers | Pages overflow |
|---|---|---|
| Tasks | Inbox, Today, Upcoming, Anytime when it fits | Task History; when active, replace Anytime |
| Habits | Today, All, Insights | Connections, Archived, Browse Templates; active page replaces Insights |
| Goals | Active, Insights, Completed | Archived; active page replaces Completed |
| Tracks detail | Entries, Insights, Automations | Options; active Options replaces Automations |
| Gym | Workout, History, Progress, Library | Library children remain Routines, Exercises, Machines, Categories, Tools with clear breadcrumb/back treatment |

## Finding-by-finding implementation decision

### F-01 — Track editor adaptive failure

**Decision:** root-host Track, Field, and Entry editors immediately.

- Compact editor chrome must replace app chrome, not overlap it.
- Open Fold editor bounds equal the complete content pane, never the 62% Track detail sub-pane.
- The support side is visibly dimmed, inert, and removed from semantics while editing.
- Close/title/Save visible pixels and semantic bounds must coincide.
- Track Field child editing uses the same owned pane or a full compact child route.

**Tests:** compact, open Fold, expanded pane, tabletop, RTL, 100/200/320% text, IME open/closed, Close/Save geometry, no underlying navigation semantics, no hinge crossing.

### F-02 — Track draft recreation loss

**Decision:** one coherent saved draft per editor.

- `TrackEditorDraft`: identity, description, icon, Area, tags, Fields/order/options, selected section, deletion decisions/replacements, dirty baseline.
- `TrackEntryEditorDraft`: Entry date, every typed Field value, selected child picker, dirty baseline.
- Store the immutable draft in a screen ViewModel backed by `SavedStateHandle`; do not mix `remember` and `rememberSaveable` lifetimes.
- Draft recovery does not create a persisted Track Entry. Evidence is recorded only on explicit Save.

**Tests:** recreate/fold/unfold after every Field type, option mutation, reorder/delete/replacement, date change, and dirty-discard state.

### F-03 — Tracks is not first-class

**Decision:** implement Whip/Home plus five direct modules exactly as specified above.

- Change the shared navigation list to Tasks, Habits, Goals, Tracks, Gym.
- Turn the brand mark into the selected Home control.
- Apply the model to compact bottom bar, Fold rail, tabletop navigation, keyboard shortcuts, Back, first run, deep links, Search, and editor return.
- Add Tracks to Home configuration without adding a mandatory empty card.
- Split Home and Review domain enums.

**Human acceptance:** from Tasks, at least 90% of first-use participants open Tracks in one tap within five seconds; at least 90% return Home within ten seconds after normal first-run exposure.

### F-04 — Hidden destination tabs

**Decision:** implement the primary/secondary page model above; remove scroll-only navigation.

**Tests:** 320/360/412 dp, 100/200/320% text, RTL, TalkBack traversal, active secondary location, Pages versus Actions grouping.

### F-05 — Contradictory notification diagnostics

**Decision:** model outcome before implementation detail.

- Overall states: **Deliverable**, **Blocked**, **Off in Whip**, **Off in Android**.
- Precedence: permission blocked → app notifications blocked → channel blocked → deliverable.
- If permission is absent, no row may claim Enabled/Deliverable.
- Reminders shows one repair summary and primary action.
- Put channel/configuration truth under **Delivery Details** and Android/battery links under **Troubleshooting**.

**Tests:** complete permission/app/channel/configuration truth table, including permanently denied/rationale states and each channel.

### F-06 — Instrumentation is not an executed release gate

**Decision:** add a disposable local API 34+ emulator lane. Never run destructive suites on the owner's phone.

The local release command/script must run:

1. JVM tests.
2. Lint.
3. Release assembly/bundle.
4. All connected instrumentation tests.
5. Adaptive/RTL/font/theme screenshot matrix.
6. Artifact identity/version/permission checks.

Fixtures: empty, representative small data, archives, active workout/rest, Track with Fields/Entries/automation, notification blocked, failed restore, and large history.

### F-07 — Home empty-state density

**Decision:** one calm all-zero state.

- Remove `Habit Progress 0 of 0` when no habits exist.
- Replace heading + zero badge + chevron + card repetitions with compact rows or one creation cluster.
- Actual items, active workout, pending prompt, or pinned Quick Log earns a card; absence does not.
- Review & Trends becomes a compact navigation row in an empty day.
- Calculate bottom clearance from navigation + FAB + 24 dp; verify the final item at 320% text.
- Open Fold support pane owns date/summary/quick actions; content pane owns actual items.

### F-08 — Search density and duplicate identity

**Decision:** one root Search utility.

- Global Search is present on ordinary destinations and starts scoped to the current module.
- Title: **Search**. Query hint: **Tasks, habits, goals, tracks…**.
- Merge Search Filters and Advanced Search into one Filters surface.
- Active filters appear as chips plus a numeric badge; inactive machinery stays collapsed.
- Results visibly distinguish Track from Track Entry and retain query/result priority with the IME open.
- Remove redundant page Search actions after global Search is present.

### F-09 — Review empty state and dates

**Decision:** use outcome-aware empty copy and direct actions.

- Shared localized range, for example `Aug 17–23, 2026`.
- Empty actions: Open Tasks, Habits, Goals, Tracks, Gym.
- Track action is neutral: record/inspect evidence. Entry count is never success.
- If Tracks contain evidence but no reviewable outcome exists, say so; do not claim nothing was recorded.
- Period controls become secondary until reviewable data exists.

### F-10 — Settings architecture and copy

**Decision:** exact category names:

- Appearance & Home
- Planning & Units
- Organization
- Reminders & Integrations
- Data & Privacy
- About & Diagnostics

Organization contains Areas and Tags only. Track onboarding/help moves into Tracks. Rename Power mode to **Show Advanced Controls by Default** with benefit-focused supporting copy. Reminders keeps the repair summary; detailed notification test tools may live in About & Diagnostics. Root descriptions and child headings must match exactly.

### F-11 — Area language and density

**Decision:** centralize quantity grammar and preserve the current lifecycle.

- Correct `Entry → Entries` and use sentence case in prose/counts.
- Empty Area row: **Main · No items**.
- Nonempty row: total plus at most two nonzero categories; full breakdown in Area detail.
- Preserve create, rename, move, merge, archive, restore, and permanent delete.
- Move Items is enabled only when meaningful; lifecycle actions stay in overflow/detail.

### F-12 — Goal measurement hierarchy

**Decision:** intent before measurement machinery.

For Reach a Target: Name → Goal Type → Target → Starting Value → Unit → progress/schedule → optional automation → Advanced Measurement Options.

- One Unit selector contains common units, custom units, No Unit, and Create Unit.
- Hide internal Measurement Type unless it materially constrains available choices.
- Infer precision; Decimal Places lives under Advanced Measurement Options.
- Target/Starting Value may share a row only when each is at least 160 dp; stack at high font scale.

### F-13 — Task placement and Repeat grammar

**Decision:** Placement is one single-choice group; Repeat is an independent switch/disclosure immediately below.

- Placement: Inbox, Anytime, Scheduled.
- Scheduled owns date, time, reminder, and deadline.
- Repeat owns cadence, anchor, and end.
- Turning Repeat on from Inbox/Anytime must explain any required placement/date transition and never silently mutate unrelated values.
- No time/reminder is editable where persistence discards it.
- Apply enabling-control adjacency to Habit cadence/reminders, Goal automation, Track Field settings/automations, and Gym timers.

### F-14 — Gym calculator defaults

**Decision:** no unexplained example data.

- Weight/Reps start empty; output asks for valid inputs.
- Examples belong in supporting text or an explicit **Use Example** action.
- Genuine history enables **Use Last Set** and names the source.
- Formula, units, rounding preference, and planning-aid disclaimer stay visible.
- Test zero, extreme, invalid, localized decimal, unit switch, IME, and no-history/history cases.

### F-15 — Destructive database fallback

**Decision:** use the permitted pre-release clean cutover once, then remove silent fallback.

- Treat the current complete schema as the clean migration baseline.
- During implementation/deployment, wipe/reinstall the owner-only development database once if required.
- Remove `fallbackToDestructiveMigration` before wider internal data is entrusted to Whip.
- Every schema version after the baseline requires an explicit migration and migration test.
- Never make a failed upgrade look like a successful empty app.

### F-16 — Large-file/refactor risk

**Decision:** guard behavior first, then split by route/responsibility.

- `WhipApp`: shell, navigation, overlay host, Home.
- Tracks: workspace, list, detail, editor, Field editor, Entry editor, automation, Insights.
- Gym: active workout, history/progress, library, tools, editors.
- Shared: app/page/editor scaffolds, destination navigation, form sections, status/availability.
- Every extracted shared component gets focused geometry/accessibility tests and representative previews.
- Do not mix an unguarded refactor with the F-01/F-02 behavior change.

### F-17 — Lint debt

**Decision:** user-affecting state warnings first.

1. Replace mutable collection `MutableState` in Goal/Habit editors with immutable/snapshot-backed state.
2. Fix modifier contracts.
3. Fix KTX and primitive-state hints.
4. Gate no new warnings; drive the explicit budget to zero after P0/P1.

## Implementation work packages

Each package must end in a runnable, internally coherent app. Do not land half of a new control model.

### WP-0 — Regression guards and local QA lane

- Add failing Track bounds/semantics/recreation tests.
- Add deterministic emulator fixtures and local release script.
- Record before-fix fresh screenshots.

Exit: tests reproduce F-01/F-02 and cannot target the personal phone.

### WP-1 — Root EditorHost and Track integrity

- Implement root overlay ownership and adaptive geometry.
- Move Track/Field/Entry editors.
- Implement complete saved drafts.
- Remove nested editor modifier ownership.

Exit: F-01/F-02 pass across compact/Fold/RTL/font/IME/recreation.

### WP-2 — First-class Tracks and shell navigation

- Implement Whip/Home control and five-module navigation.
- Update Fold/tabletop, Back, shortcuts, deep links, global Add, first run, Home settings, Search routing, and guides.
- Split HomeSection/ReviewSection.

Exit: Tracks one-tap visibility and Home discovery thresholds pass; exactly one top-level selection is exposed.

### WP-3 — Navigation/control grammar

- Replace scroll-only destination bars.
- Establish Pages versus Actions overflow grouping.
- Add global scoped Search and remove duplicates.
- Separate Task Placement/Repeat and apply dependency adjacency.

Exit: no page is reachable only by swipe; control roles are consistent at 320% text and TalkBack.

### WP-4 — Density, copy, and form hierarchy

- Home, Search, Review, Settings, Areas, Goal measurement, and Gym calculator changes.
- Notification outcome model and truth table.
- Contextual Track onboarding/help.

Exit: F-05 and F-07 through F-14 acceptance scenarios pass with fresh screenshots.

### WP-5 — Data/release hardening

- Perform the clean pre-release database cutover.
- Remove destructive fallback and establish migration baseline/tests.
- Execute the complete connected suite and screenshot matrix.

Exit: clean install and supported upgrade pass; no silent reset path remains.

### WP-6 — Guarded modularization and warning cleanup

- Split large files by the boundaries above.
- Resolve lint state warnings, then bounded remaining cleanup.
- Re-run all behavior, screenshot, performance, and accessibility gates.

Exit: no P0/P1 regression, no lint errors/new warnings, and shared components have focused tests.

## Visual and QA acceptance matrix

Use a deterministic disposable emulator with dynamic color off for goldens. Use a real Fold only for non-destructive final visual validation.

Dimensions:

- compact portrait and landscape;
- 360 dp and 412 dp widths;
- open Book Fold and tabletop Fold;
- expanded tablet;
- LTR and RTL;
- 100%, 200%, and 320% font;
- light and dark themes;
- IME closed and open where relevant.

Fixtures:

- completely empty;
- representative tasks/habits/goals/tracks/workouts;
- Track with every Field type, options, Entries, automation, and large history;
- active workout and rest timer;
- archived/completed/secondary pages;
- validation errors;
- notifications blocked at every layer;
- failed backup restore;
- large histories/search results.

Per-screen invariants:

- no horizontal clipping, hinge crossing, footer/IME overlap, partial selector, hidden final row, or unexplained truncation;
- all five modules and the Home state remain legible and centered;
- exactly one top-level destination is selected;
- no underlying navigation/FAB/focus nodes remain under an editor/utility surface;
- foreground system-bar background is intentional and opaque;
- primary intent appears before advanced machinery;
- enabling settings are adjacent to their controller;
- visible and semantic controls share bounds;
- touch targets are at least 48 dp;
- status/error meaning is not color-only;
- final content clears navigation/FAB by at least 24 dp.

Required live routes: Home; every Task/Habit/Goal page and editor; Tracks zero/list/detail/Entries/Insights/Automations/Options/create/edit/Field/Entry; Gym Workout/active set/rest/History/Progress/Library/Tools; Search empty/query/filter/keyboard; Review empty/populated/drill-down; Settings root/every category; Areas empty/populated/move/archive/delete.

## Non-negotiable completion criteria

- F-01 through F-06 pass before wider internal testing.
- Every F-01 through F-17 line has an automated check, a fresh visual check, or both.
- Tracks is visible as a labeled direct module from every ordinary compact screen and every expanded navigation surface.
- Home remains discoverable through the Whip control and Back; evidence meets the first-use thresholds.
- No editor can be mounted inside a domain list/detail split.
- No visible control saves to nothing or claims an outcome that Android/the data layer cannot deliver.
- No primary/secondary page is discoverable only through an undocumented gesture.
- The owner phone is used only for guarded, non-destructive final visual validation.
- The final release candidate receives a fresh screenshot matrix; legacy repository screenshots are never accepted as proof.
