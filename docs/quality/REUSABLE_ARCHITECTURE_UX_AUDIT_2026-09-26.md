# Whip reusable architecture and UX audit — 2026-09-26

Owner request: FB-20260926-001. This is the current-source plan and implementation ledger for the new audit. The September 3 and 10 shared-component work is the foundation; the September 21 whole-product audit remains historically paused, not silently accepted by this document.

## Baseline and method

- Opening source: `84374002` on `main`, clean before the feedback record. Production has 103,820 Kotlin source lines across 181 files; 86 UI files. Room schema, backup format, and user data are outside this presentation refactor.
- Read the current composition and callers across the root shell, seven primary destinations, editors, widgets, reminders, and build/test policy. Compare existing shared primitives with direct Material composition and repeated feature code. Distinguish semantic exceptions from accidental copies.
- Fresh visual pass: disposable API 34 emulator `emulator-5554`, current debug build, ordinary and actual 200% text. Retain named captures and review navigation, content height, empty/populated hierarchy, and selected workflows. Historical captures are context only.
- Existing source-linked catalog contains 523 distinct current states at the September 21 checkpoint. Final acceptance requires a new current-source family/campaign capture and personal review, not old green counts.

## Existing reusable system to keep

`WhipTheme`, semantic colors, `WhipSpacing`, `WhipPageHeader`, `WhipWorkspaceLayout`, `DestinationTabBar`, `WhipItemCard`, `WhipProductivityItemContent`, `WhipRecordItem`, `WhipSettingItem`, `WhipExecutionItem`, `WhipEmptyState`, pane-aware dialogs, and adaptive navigation already own recurring roles. Expand those exact owners where evidence warrants it. Specialized charts, calendars, selection/reorder, execution input, destructive review, and domain state keep their own composition.

## Ranked findings and resolution

| Rank | Finding | Current evidence | Resolution |
| --- | --- | --- | --- |
| 1 | FND-20260926-001: toggles and searches have parallel implementations | Task/Habit/Routine switch rows vs Settings whole-row toggle; Track/Gym/Routine fields vs `WhipSearchField` | Give equivalent controls one shared whole-row/clear-query presentation, migrate all identified peers, preserve each callback and draft state. |
| 2 | FND-20260926-003: navigation spends space twice and hides overflow | Track detail repeats tab names as body headlines; all long destination bars partly overflow at 200% without a cue | Remove duplicate Track headlines; retain filter/guidance; make scrollable peer tabs visibly discoverable and auto-reveal selected tabs. Review short-window reading. |
| 3 | FND-20260926-002: headings, card insets, selected support rows, and status colors drift | Raw Track/Goal headings lack common semantics; Gym/Routine copy `WhipCardGeometry`; Track/Settings support rows are near-copies; information and success share green | Consolidate the existing heading/item/navigation roles; separate informative from success color at `WhipNoticeCard`. |
| 4 | FND-20260926-004: stale width choices remain in source | Unused 920/1200 dp values, redundant 1040 dp Track caps under the 1000 dp shell, repeated authored-form 720 dp widths | Keep one existing 1000 dp workspace measure, name genuine 720 dp authored-form measure, delete inactive caps/tokens. |
| 5 | FND-20260926-005: non-UI guarded plumbing is duplicated | Task/Habit widget factories/providers; alarm schedulers; two Gradle target-guard hooks; two source-policy scans | Share only proven identical lifecycle, cancellation and guard-launch plumbing. Keep distinct claims, SAF exemptions, renderers and fallback order; verify boundary tests. |

The Gym top-bar identity and first-use CTA variations are under ordinary-scale UX review before any redesign. Specific first-use actions may legitimately differ; all already use `WhipEmptyState`. The fact that `WhipScreen` and `GymScreens.kt` are large is an audit signal, not a reason to move state into a speculative framework.

## Implementation sequence

1. **Shared controls:** unify toggle and search roles and migrate equivalent Task, Habit, Routine, Track, and Gym callers. Exercise enabled/disabled, conditional controls, query clearing, keyboard and accessibility semantics.
2. **Visual grammar:** share section headings and ordinary card insets, consolidate support-pane selected rows and nav item content, separate informative/success tones, and remove delegate-only wrappers when their callers can use the real owner.
3. **Navigation and UX:** simplify Track detail tab bodies, improve peer-tab overflow affordance, review normal/large Task header action stacking and Gym global identity, and make focused compact detail yield useful content without hiding a necessary route.
4. **Layout/source cleanup:** remove dead width tokens and redundant caps; unify only repeated authored-form measures, preserving DEC-20260910-030's single workspace alignment.
5. **Non-UI complexity:** refactor the verified widget, alarm, Gradle and source-policy copies in separate small chunks, after mapping exact error/recovery behavior. Do not remove compatibility state or weaken emulator/phone guards.
6. **Acceptance:** run affected JVM checks while editing, compile/lint and native UI journeys after each coherent tranche, then one fresh complete Android/JVM/build/lint gate and current visual catalog review. Use at most two disposable emulators. Record exact pass counts, images, remaining limits, and commit/push each verified chunk.

## Acceptance checks

- Equivalent item/control roles render from the same composable, with no remaining identified manual copies; supported actions and persisted values still work.
- Corresponding headings, notices, card insets, selected navigation, and editor measures align at ordinary phone and wide layouts; 200% text remains navigable with visible choices and actionable content.
- Task/Habit/Goal/Track/Gym/Settings/Home and relevant editor/detail journeys are intuitively labeled, reachable by touch and Back, and have named accessible controls and at least 48 dp actions.
- Widget refresh/fallback, alarm cancellation/recovery, emulator-only instrumentation guarding, and SAF source policy retain their existing strict behavior.
- Final evidence is tied to one current source candidate. Source-only checks or old captures cannot establish a full UX result.

## Ponytail complexity disposition

`shrink:` repeated toggle/search/section/card/navigation composition into existing owners. `delete:` dead width tokens and delegate-only wrappers. `native:` retain Compose's native scroll, semantics, and Material behavior rather than building a new UI framework. Dependency cut: none identified. The implementation diff currently removes a net 165 production Kotlin lines (766 added, 931 deleted), including the small shared widget lifecycle helper.

## Implementation outcome

| Finding | Disposition | Current implementation |
| --- | --- | --- |
| FND-20260926-001 | Implemented | `WhipToggleRow` delegates to `WhipSettingItem` for whole-row accessible switches; `WhipSearchField` owns equivalent query controls with hints, clear, enabled and IME submit behavior. Task, Habit, Routine, Settings, setup, Track, Gym, global search and pickers now call those owners. The old Routine and setup switch copies were removed. |
| FND-20260926-003 | Implemented | Track detail Insights/Options keep guidance and filters without repeating their tab title. Shared tabs expose scroll directions and reveal selection. The page header measures title/action fit to stack only when needed, including large text. |
| FND-20260926-002 | Implemented | Shared heading semantics, card insets, selected support rows and navigation content replace equivalent local renderers. Informative notice color is distinct from success. Specialized domain components remain specialized. |
| FND-20260926-004 | Implemented | Dead 920/1200 dp values and redundant 1040 dp Track caps are gone; one 720 dp authored-form token serves the repeated real measure. The 1000 dp workspace owner remains. |
| FND-20260926-005 | Implemented | Widget provider async completion and collection refresh, collection-factory snapshot/cache/error-first fallback, Gradle target-guard launch and source-policy checks share proven plumbing. Domain loaders/renderers/IDs remain local. Timer and reminder schedulers retain distinct registry/cancellation ordering, so merging them would add a conditional framework around different behavior. |

The source review found no further equivalent toggle/search renderer to migrate. Gym’s library identity and first-use actions were visually reviewed at ordinary and enlarged text; their existing shared shell and empty-state roles fit the product. Splitting large feature files or adding a universal screen DSL would not remove a confirmed rendering inconsistency.

## UX review and current evidence

- Fresh normal and actual-200% API 34 captures cover the seven primary destinations, editors, Track detail tabs, Gym library and routines, Settings, global/contextual search, setup and RTL. The full named catalog has 523 current PNG/XML pairs collected by 193 capture methods in 20 batches on two disposable emulators. All pairs match their manifest hashes; XML has zero `NAF=true` nodes. Family contact sheets cover every state; roughly 90 original screens across all app areas were separately inspected for hierarchy, clipping, selection and action reachability. These include `shared.first-run.optional-large-rtl`, `shared.search.native-filters-large`, `tracks.detail.options`, `tracks.history-controls.insights.large`, `gym.library.landing`, `settings.restore-confirm.large`, and both theme inversions. No further concrete actionable inconsistency emerged.
- Affected native UI journeys passed 8/8 methods in three fresh batches, including 200% header, toggle semantics, Track navigation and search flows. `UiDesignArchitectureTest` passed 17/17. Exact source-policy and candidate-harness fixtures, catalog lint (523 required, zero pending/exception), Kotlin compilation and diff whitespace checks passed.
- The complete frozen build/JVM/Android candidate and any post-final-source gallery recapture belong to the final verification receipt below. The September 21 historical whole-product pause remains a separate record; this audit concerns reusable presentation architecture and UX consistency on the current app.
