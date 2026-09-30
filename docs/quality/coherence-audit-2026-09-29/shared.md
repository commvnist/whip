# Shared shell, Home and design system audit — 2026-09-29

Baseline `6d79bd1f`; audit only. The parent reviewed shared source, original captures and bounded current native journeys. Domain-specific details belong to the component reports. **S** means a current source trace; **D** means a fresh manual device reproduction; **E** means a freshly executed named instrumentation method; **H** means a dated historical original; **U** means a runtime/accessibility question still unverified. A justified keep is a design disposition, not certification of every device state.

## Overall design assessment

Whip has a recognizable visual language: warm neutral surfaces, clear type hierarchy, restrained identity color, common disclosure and action patterns, named navigation, and explicit destructive actions. Fresh normal Home, Tasks, Habits, Goals, Tracks, Gym and Settings originals use this language consistently. The collection/execution/history/analysis split reflects different jobs. Goals does not need a daily execution tab; Gym's four destinations reflect a larger library/workout job. Archive is separate from completion. The owner-directed collection-first landing and single workspace Search are coherent and should remain.

The main design weakness is inconsistency at transitions. A daily summary can route to a remembered collection; an editor can omit some visible text from its draft contract; a correction can close the history that launched it; a persistence failure can be described as no import even after data committed. These are failures of the interface's promise about scope, continuation and result. Increasing visual uniformity alone would not repair them.

The enlarged-text design deliberately allows tall cards, wrapping names and multiline navigation labels. That trades density for legibility. This audit does not propose shrinking actual 200% text, hiding labels, flattening semantically distinct actions or imposing the same tabs on every component. Some bespoke controls still fail that common contract; those are recorded in their component reports.

Source paths below are under `app/src/main/java/com/whip/app/`. Source symbols are provided where an entire composable owns the behavior.

## Flow and state coverage

| # | Journey / state | Current owner and evidence | Disposition |
|---|---|---|---|
| 01 | Cold launch / data readiness | `MainActivity.kt`, `StartupRecoveryScreen.kt:39`; S | Keep distinct checking, ready and blocked states; no false empty workspace. U current failed-start device injection. |
| 02 | Failed recovery / Retry / keep data and close | `StartupRecoveryScreen.kt:39–170`; S | Keep explicit recovery guidance and non-destructive exit. Erase requires separate confirmation. |
| 03 | First launch recommended setup | `FirstRunSetupDialog.kt:53–96`; S/D first-run original | Keep short defaults path and receipt-owned dismissal. Parent selected Use Recommended successfully. |
| 04 | Customize Home sections / optional preferences | `FirstRunSetupDialog.kt:99–250`; S | Keep reversible choices and optional advanced preferences. U maximal 200%+IME custom setup. |
| 05 | Setup save failure / interrupted save / notification request | same host/coordinator; S | Keep retained choices and post-receipt notification request. No fresh fault injection. |
| 06 | Compact bottom navigation | `WhipAdaptiveShell`, `ItemControlPatterns.kt`; S/E normal roots | Keep visible text on all six primary destinations; Settings remains a named toolbar entry. |
| 07 | Actual 200% bottom labels / dense Home continuation | `DeepSharedJourneyTest#denseHomeContinuationRemainsReachableAtLargeText`; E/original | Keep wrapped labels and bounded three-item domain previews; test passes completion refill and continuation. Not a TalkBack pass. |
| 08 | Rail / wide support / expand content pane | `AdaptiveLayout.kt:20`, `WhipAdaptiveShell`; S/D wide Home | Keep readable rail, optional support context and content expansion. U hardware folds/tabletop native interaction. |
| 09 | Shared header/body alignment | `AdaptiveWhipScreenTest#expandedWorkspacesAlignChromeWithTheirDeclaredContentColumn`; E | Keep one declared content column; fresh exact bounds pass. Test emulates layout, not physical fold hardware. |
| 10 | Return to last workspace tab | `WhipApp.kt:1465–1472`; S | Keep root navigation's saved job. Explicit daily shortcuts must override that job (SH1). |
| 11 | Workspace Add action | `WhipApp.kt` workspace toolbar/`triggerAdd`; S/E shared-shell menu | Keep task/habit/goal/track-specific Add, Home's labelled global menu and Gym's context-specific actions. |
| 12 | Search one workspace / all Whip | `UnifiedSearchDialog.kt:286–628`; S/E search originals | Keep one workspace entry; explicit scope and Area override avoid duplicated search bars. |
| 13 | Search query / filters / no match | `UnifiedSearchDialog.kt:301–307,771–967,998`; S/E | Keep retained query, explicit zero result message and adjustable Filters. Current normal IME original readable. |
| 14 | Search incomplete sources / retry / bounded history | `UnifiedSearchDialog.kt:171–254,998–1172`; S | Keep incomplete/failed source disclosure and bounded history explanation. U fresh failure/network-independent data injection. |
| 15 | Search entity versus history result | index `distinctBy` for definitions plus typed child results; S | Keep entity lookup deduplication. Do not assert every Task definition result promises an exact completed occurrence. |
| 16 | Open result / stale/deleted source / exact ID | `WhipApp.kt` typed search handlers/launch requests; S | Keep authoritative ID routing and stale failure feedback. Domain reports inspect the destination return seam. |
| 17 | Area scope / All Areas / create destination | `AreaPicker.kt`, `AreaScopeFilters.kt`, `WhipApp.kt`; S | Keep scope named in chrome; unscoped authoritative editor data. U full fresh multi-Area device matrix. |
| 18 | Empty day versus empty product | `WhipApp.kt` Home and `HomeSupportPane:4462`; S/E | Keep clear-day copy separate from first-use introduction and domain failures. |
| 19 | Home plan / task daily shortcut | `WhipApp.kt:2100–2101`; S/E/D | Keep compact main-column shortcuts selecting Today explicitly. SH1 covers the wide support variant. |
| 20 | Home Habit progress / daily shortcut | `WhipApp.kt:2100`; S | Keep explicit Today selection; shared HabitCard preserves its execution action. SH1 support callback differs. |
| 21 | Home active Goal/pinned Track/Workout continuation | `WhipApp.kt:2104–2119`; S/E normal Home | Keep bounded useful preview and named continuation. Wide Gym summary can restore a different job (SH1). |
| 22 | Complete Home task / refill dense previews | `DeepSharedJourneyTest` method above; E | Keep next item appearing without starving other domains; no unbounded Home dump. |
| 23 | Home domain Loading / Error / Retry | `DomainRetryActions`, `AdaptiveLoadNotice`, Home load owners; S | Keep source-specific feedback and independent retry. A failed source is not counted as zero. |
| 24 | Home personalization and hidden sections | `AppSettings.homeSections`, Home preview/support; S | Keep both columns following selected sections. Settings report owns persistence. |
| 25 | Home → Review → original source → return | `ReviewAppRoute`, saved Review host; S/E Review root | Current root readable. Review component report owns precise retained options/scroll/source behavior. |
| 26 | Task Focus start / replace expected timer / duplicate submit | `FocusTimerUi.kt:73–171`; S | Keep receipt coordinator and expected active task/deadline guard; domain data remains independent. |
| 27 | Focus running / unavailable Task / completion | `FocusTimerUi.kt:120–162,241–350`; S | Keep Task unavailable/loading distinction, finishes-at metadata and explicit Open/Stop/Dismiss. U current tiny-height+200% Focus lane. |
| 28 | Focus alert unavailable / Android Settings return | `FocusTimerUi.kt:206–238,307–349`; S | Keep named alert reason and direct Android settings action. No guarantee about OEM delivery. |
| 29 | Focus versus Habit stopwatch / Gym rest | corresponding distinct owners; S | Keep distinct purposes; a completed Focus timer never silently completes a Task. |
| 30 | Inspector stable identity / tab / primary action | `EntityInspector.kt:82–184,200–318`; S | Keep saved sections, lifecycle-aware primary action and named Edit/Close controls. Domain reports identify incomplete return adoption. |
| 31 | Inspector normal/large layout and original identity | `EntityInspector.kt:151–158,257–279`; S/H/E root captures | Keep wide identity and full title under enlarged text. U full native200 inspector matrix. |
| 32 | Inspector Options / danger zone | `EntityInspector.kt:415–471`, `PermanentDeleteDialog.kt`; S | Keep destructive separation, explicit impact and rereview after source change. |
| 33 | Primary versus secondary button hierarchy | `ItemControlPatterns.kt`, `WhipItemBuilders.kt`, `EntityInspector`; S/E normal roots | Keep common action styling and disclosure separate from commit. Domain-specific clipped actions are findings. |
| 34 | Minimum touch target / reorder alternatives | `ItemControlPatterns.kt` 48dp controls, `WhipReorderHandle:581`; S | Keep named action, move alternatives and state announcements. U fresh spoken traversal across all reorder types. |
| 35 | Shared title/summary density | productivity/execution/record builders; S/E/H enlarged originals | Keep compact normal summaries and full enlarged identities. Do not reopen previously fixed truncation without current evidence. |
| 36 | Colors / theme / identity colors | `ui/theme/Theme.kt:99,136,169`; S/E normal dark, D normal light | Keep semantic success/warning/error distinct from identity color. U measured contrast for every dynamic palette and disabled state. |
| 37 | Typography / spacing / corner shapes | `ui/theme`, shared builder spacing; S/E originals | Keep repeated hierarchy and restrained grouped surfaces. No grounded global type/spacing rewrite needed. |
| 38 | Theme choice / app versus system appearance | Settings theme owner; S | Keep appearance choice independent of data. U fresh dynamic-color/light/dark matrix and OLED/OEM legibility. |
| 39 | Shared notice / warning / persistence failure | `WhipNoticeCard`, `PersistenceFailureNotice`, semantic warning owner; S/H prior spoken fix | Keep persistent errors in their owner; warnings use named title/message semantics. Old TalkBack proof is dated, not this audit's full spoken pass. |
| 40 | Transient success / Undo / navigation | `TransientFeedback.kt:43–125`; S | Keep routine success subordinate to local failure and recovery actions; invalidate irrelevant success on destination change. |
| 41 | Unsaved definition draft / Back/outside/Cancel | `EditorProtection.kt:8–28`, feature coordinators; S | Keep common Keep Editing/Discard Changes. Task pending subtask and Settings/Gym exceptions are component findings. |
| 42 | Save busy / duplicate / failure / retry | persistence request coordinator + editor snapshots; S | Keep request-owned receipt and drafts. Gym's incomplete adoption is a component finding; no blanket 'all editors safe' claim. |
| 43 | Dialog body / safe drawing / IME / fixed footer | `PaneAwareAlertDialog`, `ui/theme/WhipDialog.kt`, focused input owner; S/H | Keep one inset owner and scrollable body. U native200 short-height maximal dialogs at this baseline. |
| 44 | Date picker / disabled/future/selected date | `WhipDatePickerDialog.kt`, domain date policy; S | Keep explicit date semantics; Task calendar's Tomorrow bug is TASK03. U DST/locale full device matrix. |
| 45 | Unit / number inputs and changed definitions | unit owners/domain validators; S | Keep physical conversion separate from relabeling, original entered evidence and raw draft fidelity. Gym preset bypass is a finding. |
| 46 | Emoji presets / named saved search / Use Once | `IdentityEmojiPicker.kt:88–175`; S | Keep reusable identity versus one-off choice; shared Save & Use fails the confirmed-persistence contract (SH2). |
| 47 | Emoji validity / duplicate saved name / reset | same picker 139–175, custom editor; S | Keep emoji-specific validation and duplicate-name explanation. U native keyboard maximal grapheme selection. |
| 48 | Edit Area/Tags / custom units and emoji library | Settings organization owners; S | Settings report covers list/editor/search/delete constraints; shared picker SH2 is not duplicated there. |
| 49 | Hardware keyboard shortcuts / focus movement | `WhipApp.kt:1640–1698`, field IME owners; S | Keep named shortcuts with editing guards. U fresh Tab/Shift-Tab/Enter/Escape traversal of every overlay. |
| 50 | Screen reader headings / selected tabs / row actions | shared semantic owners; S/XML in originals | Keep semantic names and selection. XML is not spoken output; U complete current TalkBack/Switch Access journey. |
| 51 | RTL / rail/support column direction | shared adaptive owners + existing test source; S | Keep direction-aware alignment. U fresh populated RTL wide/IME originals. |
| 52 | Launch queue / interrupted or stale external action | `LaunchQueueOverflowDialog`, `PendingTaskEditorLaunchDialog`; S | Keep explicit queue overflow/review and unsaved interruption choice; widgets/platform report owns external policy. |
| 53 | Background/recreation / identity not object copy | saved IDs, coordinator orphan recovery; S | Keep authoritative identity and source revision checks. U killed-process pending operations and OEM lifecycle matrix. |
| 54 | Reduced motion / celebration preference | `GoalCelebration.kt`, Settings; S/H prior verified owner request | Keep one saved-only four-second celebration, tap dismissal and semantic fallback. No proposal for additional effects. |
| 55 | Portable backup / restore / device transfer | Settings + startup owners; S | Settings/platform reports cover preview, scope and interrupted recovery. No real owner data used in this audit. |
| 56 | Daily logical date / zones / DST across screens | app clock/calendar and domain owners; S | Keep one configured cutoff contract; Goal Insights and Habit end-threshold seams need correction. U travel/DST fresh device campaign. |

## Findings

### SH1 — P2: wide Home's daily support shortcuts restore a different job

**Evidence: S + D.** `WhipApp.kt:1710–1714` passes `::selectPrimaryDestination` into `HomeSupportPane`. Its `Tasks Today`, `Habits Needing Attention` and workout cards at `4570–4574` use that ordinary root navigation function, which retains the last Task/Habit/Gym tab (`1465–1472`). The main Home column explicitly selects Today/Workout (`2100–2104`).

Fresh reproduction: leave Tasks on its Tasks collection in Calendar view; open Home at emulator size2400×1600; tap left-pane **Tasks Today:1**. The result has **Tasks** underlined and the collection calendar, while the adjacent support pane still says **Tasks Today**. [Before](../../../artifacts/coherence-audit/2026-09-29/fresh/manual/shared.home.wide-route-before.png) and [after](../../../artifacts/coherence-audit/2026-09-29/fresh/manual/shared.home.wide-route-after.png), each with XML. Parent inspected both originals. The inconsistent Habit and Gym variants are source-traced, not separately reproduced. Size was restored.

Expected: daily summary shortcuts open the daily execution job consistently across widths. Ordinary rail/tab navigation should continue to restore its last job. Fix the support callback with the same explicit Home intent owners; avoid resetting every root navigation. Verify compact/wide Tasks from History/Calendar, Habits from Insights/History, and active Gym from Library/History, including saved Area and return context.

### SH2 — P2: shared “Save & Use” closes before a named emoji is durably saved

**Evidence: S; failure/restart not injected.** `IdentityEmojiPicker.kt:88` accepts a `Unit` save callback. `useCustom` at `153–159` calls it, immediately changes the entity emoji and closes. Entity hosts pass `SettingsViewModel.upsertCustomIdentityEmoji`, whose ordinary `update` (`305–317`) uses the non-confirming preferences path (`core/AppSettings.kt:297–301,380`); a busy data barrier can return without any save. There is no receipt, busy state or failure route. Reopening the custom editor seeds its name from the saved list (`165–175`), losing the authored reusable-name context if that save did not persist. The dedicated Settings emoji manager already uses `upsertCustomIdentityEmojiMutation`/confirmed persistence.

Expected: **Save & Use** means the named library choice is confirmed saved before the custom draft closes, with a local retained error and retry when saving fails. **Use Once** can remain immediate and independent. Reuse the existing typed custom-emoji mutation receipt rather than creating another settings pipeline. Verify failed commit/busy barrier, retained name/value, duplicate submission, reopening after recreation and entity-editor cancel after successfully saving the reusable choice. Do not claim a device disk failure was observed here.

## Verification boundaries

The main evidence receipt names eight fresh native methods, their exact results and manual journeys. Fresh geometry and dense native200 Home checks pass. Source review covers every listed flow; original visual review supports the explicitly named surfaces. This is a thorough current design/UX audit, **not** a full regression suite, complete accessibility certification, physical-fold campaign or phone release. Remaining U checks are consolidated in the top-level report; they are not invented defects or automatically accepted remedies.
