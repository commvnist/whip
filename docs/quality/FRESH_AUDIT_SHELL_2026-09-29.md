# Fresh shell, Home, Review and Settings audit — 2026-09-29

Baseline: `ba1f7cc5`. Status: **Verified** within focused shell acceptance; parent owns final integrated readiness. The findings below preserve the original current-source audit. This review independently traces current source. Prior audit reports, findings and screenshots were not used as evidence or a checklist. The memory index was read only to recover owner/data boundaries. Parent owns integration and commits.

Owner constraints: one global top-right search entry for main collections; Tasks and Habits initially open their first collection tab; stable geometry; preserved user data. No schema/backup migration is proposed. All rendered outcomes below are acceptance scenarios, not claims that a device test has run.

## Findings and design direction

### SHELL-01 — Home removes its daily orientation after completing the last Task

**Evidence:** `HomeTodayHeader.kt:139–198` creates the Task summary only when `taskTotal > 0`; when both types are present it uses a two-column Row, but switches to a one-column Habit card at zero Tasks. `WhipApp.kt:5100–5117` supplies the current remaining count, not a stable “today exists” condition. Review and Plan actions are also conditionally inserted in the heading FlowRow.

**Journey/impact:** Start Home with one Task and unfinished Habits, complete the Task. Its summary disappears and the Habit card expands/reflows; everything below shifts. The UI removes the positive zero-remaining result precisely when the user has earned it. At 200%, stacked content loses a whole card of height.

**Remedy:** Give Home a stable “Today” orientation with persistent Task/Habit summary slots for the enabled daily domains and honest loading/error/zero states. Keep completion visible as “0 remaining”/“All done” without inventing completed counts; use one consistent secondary-action row. Home preview contents may naturally change, but the top orientation should not. Do not add another search or creation entry. Keep actual execution controls on the cards.

**Acceptance:** Normal/200% before-after last Task, last Habit, empty new account, scoped-empty account and loading failure; stable summary role/bounds and usable direct Today navigation. Decide whether hidden Home sections also hide their summaries rather than accidentally imposing unrelated domains.

### SHELL-02 — Review silently loses its originating Area after visiting Track evidence

**Evidence:** `ReviewAppRoute.kt:onOpenTracks` calls `onTemporarilySelectAreaScope(AreaScope.All)`. `ReviewSession.kt` retains open/return/presentation state but no Area. `WhipApp.kt:2577–2585` rebuilds returning Review from the current Area and scoped productivity states.

**Journey/impact:** Select Work Area → Review → Open Tracks → Return to Review. Task/Habit/Goal evidence changes from Work to all Areas, despite returning to the same Review session. Recreation preserves the widened temporary Area. Totals can look like changed data when only scope changed.

**Remedy:** Make the originating Area part of the Review session, restore it on return through the shell's existing Area owner, and retain source navigation while the source is open. Preserve explicit close behavior and deliberate user scope changes separately. No duplicate domain state.

**Acceptance:** Two Areas with distinct outcomes, scoped Review → Track evidence → return, with recreation in the source. Original Area label, totals, selected period, detailed section and scroll must return together.

### SHELL-03 — Review's visual hierarchy obscures evidence and breaks down at large text

**Evidence:** `ReviewDialog.kt:ReviewOverview` places a large Track evidence explanation above the primary four outcomes. `ReviewSignalCard` lays the title and total in an unweighted `Row(Arrangement.SpaceBetween)`; the title can consume the available width before the number is measured. Grid selection uses raw 620/1240dp thresholds without font scale; the wide dashboard uses raw 720dp. `ReviewLineChart` independently rescales each series to its maximum with no visible scale or date endpoints.

**Journey/impact:** On a 720–900dp pane at 200%, “Workouts · All gym data” competes with the total inside narrow cards. A visually identical sparkline can mean 1 or 100 outcomes, and it has no visible date anchors. The largest preliminary explanation competes with the actual answer “what did I accomplish?”

**Remedy:** Lead with period/scope and outcome totals; put evidence-only Track content after the outcome grid, and make correlations a secondary disclosure. Build a resilient card with title above/weighted separately from a clearly labeled number, a visible daily scale and start/end dates, and a visible detail affordance. Use font-aware column thresholds. Keep raw heterogeneous Track values out of a fabricated total, keep Goal's normalized unit explicit, and preserve correlation limitations.

**Acceptance:** Compact 320dp normal/200%; 720–900dp wide 200%; all four outcomes and large totals; no evidence/partial loading; scoped Gym label. Capture originals to judge whether the hierarchy communicates outcome → daily pattern → optional comparison. Keep chart semantics including actual daily values.

### SHELL-04 — Search steals Enter from every focused control

**Evidence:** `UnifiedSearchDialog.kt:626–640` installs `onPreviewKeyEvent` on the entire workspace. Any Enter keydown with nonempty results calls `onSelect(results.first())`, before a focused result/filter/Close button can handle it.

**Journey/impact:** Search for a term matching multiple records → Tab to result 2 or a filter or Close → Enter. The first result opens instead of activating the focused control. This violates keyboard control roles and can navigate to the wrong record.

**Remedy:** Handle submit at the query field only (reuse the existing `WhipSearchField.onSubmit` owner); keep workspace Escape if useful. Native buttons/results own their own Enter.

**Acceptance:** Hardware-key Enter in query opens first result; Enter on second result opens second; Enter on Filters/Close activates that control. IME Search still submits. Single global toolbar search entry remains intact.

### SHELL-05 — Settings category switching discards reading position

**Evidence:** `SettingsScreens.kt:228` uses `rememberSaveable(section, saver = LazyListState.Saver) { LazyListState() }`. The section is an invalidating input, not a per-section storage key. Returning after selecting another category builds a new list at the top.

**Journey/impact:** Scroll Planning to a lower setting → inspect Reminders → return to Planning. Reading position is lost. Wide category navigation makes this interruption especially easy. Existing editor draft protection is good but does not protect surrounding navigation context.

**Remedy:** Retain one scroll state per Settings section with the existing saveable-state tools, without moving dialog/coordinator ownership into disposed content. Search navigation should intentionally jump to the selected anchor; ordinary category navigation should restore its position.

**Acceptance:** Two different saved category offsets, switch and return, then recreate; search must still jump to and reveal a collapsed anchor. Keep active typed-editor navigation guards.

### SHELL-06 — Fast restore failure can permanently disable retry and Cancel

**Evidence:** `SettingsScreens.kt:1931–1942` resets `replacementSubmitted` only after a composition observes `busy == true` and later false. `SettingsViewModel.kt:runIo` publishes Running and terminal failure asynchronously; a quick failure may be conflated before UI observes Running. `PermanentDeleteDialog` receives `busy || replacementSubmitted`.

**Journey/impact:** Confirm replacement → immediate validation/access failure delivered without an observed busy frame. The error is visible but the dialog remains busy forever; both retry and Cancel are disabled. This is a state-machine defect demonstrable by invoking the dialog callback and supplying a terminal error while keeping busy false.

**Remedy:** Tie duplicate-submit protection to a terminal receipt/failure callback, not the observation of a transient visual frame. Reuse the existing request coordinator or explicit terminal callback pattern used for reset. Preserve protection against duplicate destructive actions.

**Acceptance:** Synchronous/fast failure before a busy frame; delayed failure; duplicate tap; retry after failure; recreation during Running and after terminal failure. No real destructive reset is needed in the owner phone.

### SHELL-07 — Area picker can keep an invisible text filter

**Evidence:** `WhipApp.kt:AreaScopeMenu` stores `query` with `rememberSaveable`; only renders the field if `activeAreas.size > 8`; applies the query to the Area list regardless of that condition. The query remains after dismissal and management changes.

**Journey/impact:** With nine Areas, search for a specific Area → close picker → archive/delete enough Areas in management to leave eight → reopen picker. The list is still filtered, but its field is gone; valid Areas appear missing and cannot be revealed from the picker.

**Remedy:** Keep a nonblank query field visible regardless of the count, provide Clear and an explicit no-match state, or reset the query when the searchable condition disappears. A short query hint alone is not a recovery action.

**Acceptance:** Nine-to-eight Area transition with saved query; no matches; recreate picker; selected archived Area remains visible per current intended policy.

### SHELL-08 — Restore reads an arbitrary selected document entirely before validation

**Evidence:** `SettingsViewModel.kt:previewRestore` uses `bufferedReader().readText()` before format/checksum validation. `EncryptedBackupCodec.isEncrypted` parses the entire string; plain restore then parses it again. There is no input bound in those owners. The picker accepts `*/*`.

**Journey/impact:** Selecting a very large or wrong document can exhaust the app heap before the UI can report a usable import error. This is a confirmed unbounded allocation path, not a measured crash on a particular file size.

**Remedy:** Establish one explicit, documented input limit at the document-read boundary, with a bounded stream read independent of unreliable provider size metadata and a specific recoverable error. Include encrypted envelope overhead. Avoid imposing an arbitrary new limit on ordinary valid exports without checking realistic maximum data fixtures; if large valid backups must exceed the safe heap budget, streaming is a separate format/parser project.

**Acceptance:** Small valid plain/encrypted backups, exact boundary, over-boundary input with unknown metadata, read failure, and retry with a valid document. UI stays responsive and no data mutation occurs before preview approval.

### SHELL-09 — Custom emoji authoring bypasses the Settings editor recovery system

**Evidence:** `SettingsScreens.kt:CustomIdentityEmojiDialog` has a non-scrolling `WhipDialogBody` containing explanation plus two supporting-text fields. The caller at ~1811 immediately closes after `viewModel.upsertCustomIdentityEmoji`; that method delegates to `update` without a request receipt/error state. Dismiss closes dirty text immediately.

**Journey/impact:** At short height/200% with keyboard, Name and validation can fall outside the body's viewport. A failed Settings save closes the only copy of authored text without an inline retry; accidental Back also drops it. This is inconsistent with neighboring typed settings and custom units.

**Remedy:** Reuse the typed-setting request coordinator, keep the editor until its durable receipt, show retry in-place, make the body scroll, and guard dirty dismissal using the existing unsaved-changes dialog. Keep built-in/duplicate validation.

**Acceptance:** Short 200% editor with keyboard; dirty dismissal/cancel/reopen; injected persistence failure retaining both fields; retry success; recreation. Do not add another general editor abstraction.

## Complete scope coverage

“Keep” below means a current-source rationale; it is not a claim of device execution. “Change” references an actionable finding above. Tests/visuals remain parent-scheduled.

| Flow / state | Current source traced | Disposition and grounded rationale |
|---|---|---|
| Cold root navigation / chosen destination | `WhipScreen` saved destination states, navigation policy | Keep: initial Tasks All / Habits All are collection-first; tab choices saved. |
| Compact bottom navigation | `PrimaryDestinationNavigationBar` | Keep: text measurement chooses one/two rows; all primary domains stay discoverable. Render 200% and short height. |
| Wide/rail/fold transition | `AdaptiveNavigationFrame` | Keep: `movableContentOf` carries content across layouts; support panes and expansion have explicit owners. Render physical hinge/200% later. |
| Root toolbar / search | `WhipScreen.topBar` | Keep single search entry and Area-first toolbar; parent owns shared visual integration. |
| Global Add from Home | `globalAddExpanded`, `triggerAdd` | Keep contextual Task/Habit/Goal/Track/Workout choices; current-area defaults route through existing editors. |
| Add while selection/editor active | `globalAddAvailable` | Keep: incompatible root actions suppressed; no duplicated add affordance proposed. |
| Keyboard focus in root content | `inlineKeyboardVisible`, editor routes | Keep keyboard space ownership and suppressed chrome; capture actual short screen before changes. |
| Home first use | `homeEmptyStateEligible`, `HomeGettingStarted` | Keep: gated on settled data, avoids declaring new account while domains load. |
| Home existing data but no daily work | `HomeResumePath`, `hasReviewEvidence` | Keep: explicit saved-work destinations/Review route rather than false onboarding. |
| Home Area empty | `hasUnscopedUserData`, `onShowAllAreas` | Keep: names scoped emptiness and offers Show All Areas. |
| Home task execute / final completion | `TaskRow`, `TodayHeader` | Change SHELL-01: stable daily orientation, preserve direct execution. |
| Home Habit execute / neutral outcomes / timers | `homeHabitSummary`, finished disclosure | Keep denominator excludes skipped/paused/targetless; change surrounding stable summary under SHELL-01. |
| Home Habit child value editor | `HomeHabitValueEditor` host callbacks | Keep dedicated value editor owner and existing persistence coordination; parent domain agent reviews arithmetic. |
| Home preview continuation and pins | `pinnedHomeSummary`, `HomePreviewContinuation` | Keep bounded three-item previews and destination continuation; avoids unbounded dashboard lists. |
| Home Goal/Track actions | `GoalCard`, `TrackRow` callbacks | Keep direct record/log action and inspect/edit distinction; domain agent owns semantics. |
| Home Gym active/routine | `HomeContent.Gym` | Keep all-Gym scope stated, active session distinct, routine day launch explicit. |
| Home loading/error across domains | `HomeDomainLoadNotice`, `DomainRetryActions` | Keep per-domain retry and evidence-gated empty states. |
| Area picker / >8 areas / no matches | `AreaScopeMenu` | Change SHELL-07: invisible query can hide options. |
| Area create / rename / color | `AreaManagementDialog`, coordinators | Keep draft retention and mutation receipt routing; name conflict distinguished. |
| Area move / merge / archive / restore | `MoveAreaItemsDialog`, `MergeAreaDialog`, mutation handler | Keep explicit affected counts, target selection, preserved detail except meaningful removal; archive Undo is purposeful. |
| Area permanent delete / last Area | `PermanentAreaDeleteDialog`, `LastAreaRequiredDialog` | Keep keep-items/delete-items choices and last-active-Area guard. Render long target names at 200%. |
| Search query / filters / all-Whip / Areas | `UnifiedSearchDialog`, query/index helpers | Keep debouncing, off-main indexing/matching, explicit scope expansion and visible criteria. |
| Search result keyboard activation | Workspace `onPreviewKeyEvent` | Change SHELL-04; query submission must not own controls' Enter. |
| Search incomplete / bounded history | `UnifiedSearchDataStatus`, bounded index | Keep explicit loading/failed/limited status; no false definitive empty result. A future inline retry can reuse domain retry actions; not required to invent recovery state now. |
| Search compact/wide IME transition | `UnifiedSearchBodyLayout` | Keep stable children so query focus survives reflow; short view scrolls contextual controls. |
| Search selected result / cross-Area route | `WhipApp` search result handler | Keep temporary Area switch necessary to expose selected source; query closes deliberately. Do not silently add persistent global search drafts. |
| Review entry / period / sections | `ReviewControlPanel` | Keep explicit range and section selection; last included section cannot be removed. |
| Review overview / charts / units | `ReviewOverview`, signal/chart owners | Change SHELL-03 for primary evidence hierarchy and honest visible scale. |
| Review missing/partial/failed data | `reviewAvailability`, retry actions | Keep partial evidence separately identified; per-domain retry available. |
| Review correlation | `pearsonCorrelation`, 30-day inputs | Keep variance and seven nonzero-day safeguards, 30-day label and Area-scoped Gym exclusion. Do not pretend association is causation. |
| Review outcome inspection → source → return | `ReviewSession`, `ReviewOutcomeDetails` | Keep retained detailed section/scroll through SaveableStateHolder; change SHELL-02 so scope is retained too. |
| Review Track evidence/source | `trackReviewEvidence`, `ReviewAppRoute` | Keep heterogeneous Track entries counted as evidence and excluded from productivity scores; fix Area return. |
| Settings index / wide categories / search anchors | `SettingsContent`, `SettingsSearchEntries` | Change SHELL-05 to restore each category's position; preserve search's deliberate anchor jump. |
| Appearance/Home / Planning / Gym preference fields | Typed settings mutation/editor | Keep save/cancel transaction, external conflict, durable retry and focused input behavior. Domain policy changes out of this audit. |
| Settings custom units / conversions / versions | `CustomUnitDialog`, coordinator | Keep separation of name edit and definition version; durable receipt guards archival operations. |
| Settings custom emoji | `CustomIdentityEmojiDialog`, caller | Change SHELL-09 for scroll/draft/durable feedback. |
| Reminder permission denied / permanent denial / return | `SettingsContent.Reminders` | Keep runtime rationale, Android settings route and resume refresh; never label Android denial as a Whip switch. |
| Exact alarm/battery/channel diagnostics | notification delivery and platform snapshots | Keep explicit delivery limits, channel checks and test notification; render long status text at 200%. |
| Quiet hours / reminder refresh failure | Typed mutation/coordinator | Keep current draft, precise schedule status and explicit retry feedback. |
| Backup status / choose/reconnect/forget folder | DataPrivacy backup group, portable manager calls | Keep verified backup status before configuration and direct recovery action; CSV clearly not restorable. |
| Plain/encrypted export / document cancellation | `prepareDocumentExport`, `completeDocumentExport` | Keep URI cancellation inert and interrupted export explicit; passphrase intentionally not saved to bundle. |
| Import file / unlock / invalid checksum | `previewRestore`, codec, preview builder | Change SHELL-08 input boundary; keep validation before mutation, wrong-passphrase retry and retained selected payload. |
| Merge vs replace / confirmation child | `BackupRestorePreviewDialogs` | Keep consequences and child Cancel return; change SHELL-06 transient busy dependence. |
| Reset data / failure | `submitDestructiveActionOnce`, failure callback | Keep explicit consequences, one submission and direct failure release; useful model for restore repair. |
| First run recommended/custom/default persistence | `FirstRunSetupHost/Dialog` | Keep simple recommended route, optional advanced preferences, durable receipt before dismissal and permission opt-in. |
| Shared buttons/switches/choices | `WhipControls`, `WhipPagePatterns` | Keep 48dp actions, single whole-row switch/choice semantics, disabled destructive styling. |
| Shared headers/tabs | `WhipPageHeader`, `DestinationTabBar`, workspace header | Keep separate nav/tab/filter roles and text-measured tab scrolling; parent integrates visual hierarchy consistently. |
| Shared dialogs/IME/insets | `ProductivityEditorDialog`, `PaneAwareAlertDialog` | Keep one window/inset owner, scrollable body contract and fixed actions; SHELL-09 repairs the caller violating scroll contract. |
| Shared async notice / errors | `WhipNoticeCard`, persistence coordinator | Keep named polite live regions, semantic errors and retained retry drafts; no toast-only success proof. |

## Proposed ownership and verification

Parent accepted SHELL-01–09 after reviewing this report. Shell agent owns the feature files below and agreed bounded regions of `WhipApp.kt`: `rememberReviewSession` call, Home's `TodayHeader` call and `AreaScopeMenu`. Parent owns shared style integration and the SHELL-08 bounded document reader. `ReviewAppRoute.kt` required no edit because the existing source-navigation callback can retain its deliberate all-Track scope while the session restores origin on return.

Useful exact test homes: `HomeDestinationLinksTest`, `UnifiedSearchRulesTest`/native search UI suite, `SettingsResponsiveUiTest`, `SafetyChoiceUiTest`, `ReviewOutcomesTest` plus the native Review source-return test. Add only behavior-focused checks that fail before the repair. Parent schedules each routine command under `timeout --kill-after=3s 55s`, one stable affected readiness batch and selected native/visual acceptance. The audit phase was read-only. Parent subsequently authorized guarded native checks on emulator-5556; their execution is recorded below. No Gradle, commits or pushes were executed by this agent.

## Implemented follow-through and prepared checks

| Finding | Implementation / authored source | Failure, context and verification contract |
|---|---|---|
| SHELL-01 | `HomeTodayHeader.kt`, `WhipApp.kt:HomeContent` | Persistent daily summary slots follow enabled Home domains; zero remaining survives final completion. Loading/unavailable values never pretend to be zero. Plan My Day retains its action role, with disabled reason when no actionable unscheduled tasks; Review remains reachable. Shared measured `WhipPageHeader` owns heading/action geometry. Native: `HomeDestinationLinksTest#todaySummaryGeometrySurvivesFinalCompletionAndVisibleDestinationsStayScoped` and existing `#todaySummaryKeepsNeutralOutcomesAndActionsReadableAtLargeTextAndRtl`. |
| SHELL-02 | `ReviewSession.kt`, `WhipApp.kt:rememberReviewSession` | Origin Area saved with session; all return paths invoke one restore callback. Source navigation remains all-Track while open; closing Review clears its session. Native: `ReviewJourneyE2ETest#allTrackEvidenceKeepsItsScopeAcrossAreaReviewAndRecreation` now recreates in source and returns to original Work label/Task total while retaining all-Track evidence. |
| SHELL-03 | `ReviewDialog.kt` | Outcome totals first; Track evidence follows; correlation disclosure retains state. Font-aware wide/grid thresholds; wrapped title separate from total; visible date/scale legend matches actual sparkline bounds. Empty/failed outcome sources still allow ready Track evidence. Native: `ReviewAvailabilityUiTest#outcomeCardsKeepLongLabelsTotalsAndDailyScaleReadableAtLargeText` (native 200% rule), `#correlationsUseReadySourcesEvenWhenOtherEvidenceIsUnavailable` adapted to disclosure. `SurfaceRoleArchitectureTest` updated for disclosure plus grouped information content. |
| SHELL-04 | `UnifiedSearchDialog.kt` | Workspace handles Escape only; query's existing IME action owns submit, native focused controls own Enter. Native: `UnifiedSearchAdaptiveUiTest#enterActivatesTheFocusedSearchControlRatherThanAlwaysTheFirstResult` checks second result, Filters, query submit and Close; existing `#imeSearchActionRemainsAvailable` remains applicable. |
| SHELL-05 | `SettingsScreens.kt:SettingsContent` | Independently saveable list state for every fixed Settings category; dialog and mutation coordinators remain at existing host. Native: `SettingsResponsiveUiTest#categoryScrollPositionsSurviveSwitchingAndRecreation`; existing `#settingsSearchRestoresQueryAndLandsOnExactControlsAndDisclosures` covers intentional anchor navigation. |
| SHELL-06 | `SettingsScreens.kt:BackupRestorePreviewDialogs`, `SettingsViewModel.kt:confirmRestore` and request state/consume declarations | Dedicated request ID/terminal receipt routes through existing persistence coordinator. Fast failure releases submission without observing Running; terminal error/draft survives recreation; duplicate destructive requests rejected. Missing restored request gets an actionable interruption warning. Native: `SafetyChoiceUiTest#replacementTerminalFailureBeforeBusyFrameKeepsRetryAndCancelAcrossRecreation`, `#replaceEverythingRequiresFinalConfirmationAndBusyBlocksDuplicates`, existing Cancel/large-choice fixtures adapted to request callback. |
| SHELL-07 | `WhipApp.kt:AreaScopeMenu` | A nonblank query keeps its field visible below nine Areas; shared query field supplies Clear; no-match text is explicit. Native: `AreaFeatureUiTest#areaQueryStaysVisibleAndClearableWhenAreaCountShrinks` includes recreation. |
| SHELL-08 | Parent-owned bounded reader and `SettingsViewModel.kt:previewRestore`; shell-owned restore action supporting copy in `SettingsScreens.kt` | Parent selected a 32 MiB UTF-8 document cap, including encrypted envelope, bounded even without provider metadata. New document selection clears the preceding staged restore. UI states the cap; exports and internal recovery are not capped. This mitigates unbounded file-read allocation; it does not promise every JSON below the cap is heap-safe or arbitrary-size backup support. Parent records exact helper and valid-format checks. |
| SHELL-09 | `SettingsScreens.kt:CustomIdentityEmojiDialog` and host; `SettingsViewModel.kt:upsertCustomIdentityEmojiMutation` | Scrollable fields; dirty Back/Cancel uses existing discard dialog; typed-settings durable receipt retains draft through failure, retry and recreation. Legacy convenience picker saves retain their existing signature; the Settings authoring editor uses the receipt. Native: `SettingsResponsiveUiTest#customEmojiDraftSurvivesDirtyDismissalFailureAndRecreation` (native 200% rule) covers both fields, Keep Editing, immediate failure, restoration and successful retry. |
| R3 / FND-20260929-032 | `WhipApp.kt` shared TopAppBar identity row | Fresh native 200% overview fails exact Tracks/Gym geometry: Gym's two-line identity exceeds the shared 52 dp minimum. All destinations now reserve the sum of the actual measured Whip and Gym text heights, including their typography/font padding; normal minimum stays 52 dp. No geometry tolerance or duplicate search control was introduced. |

Fresh original inspection performed after the source audit: `artifacts/fresh-app-overhaul/2026-09-29/before/productivity/shared.home.populated.png` confirms the competing Home heading actions and two-card daily orientation; `before/supporting/settings.overview.png` supports keeping the clear Settings category index. These are parent-generated current-baseline originals, not historical evidence. New acceptance captures are prepared as `fresh.review.area-return`, `fresh.review.scaled-outcomes.large`, and `fresh.settings.emoji-failure.large` when their native tests execute.

Verification: `git diff --check` passed. Parent built APKs; shell agent installed both with `-r` on guarded `emulator-5556` and ran the exact native selectors below. Every instrumentation command used `timeout --kill-after=3s 55s`; no timeout is counted as passed. Logs are under `build/fresh-app-overhaul-20260929/`.

| Native execution | Evidence / result |
|---|---|
| Home stable zero summary | `shell-home-geometry.log`: 1 passed, 4.915 s. |
| Settings independent category positions/recreation | `shell-settings-scroll.log`: 1 passed, 6.158 s. |
| Restore immediate terminal failure/recreation and duplicate submission | `shell-restore-receipts.log`: 2 passed, 12.891 s. |
| Area shrinking list/nonblank query/recreation | `shell-area-query.log`: 1 passed, 13.028 s. |
| Custom emoji dirty dismissal/failure/recreation/retry, native 200% | `shell-emoji-large.log`: 1 passed, 13.902 s. |
| Review long title/zero total/daily chart scale, native 200% | `shell-review-large.log`: 1 passed, 6.627 s. |
| Settings query restoration/exact anchors/disclosures, native 200% | `shell-settings-search.log`: 1 passed, 18.497 s. |
| Review partial-source correlation disclosure | `shell-review-correlations.log`: 1 passed, 6.319 s; also passed in later return/correlation pair. |
| Home neutral summary at synthetic 200% RTL and search IME submit | `shell-home-large-search-ime.log`: 2 passed, 7.441 s. |
| Focused keyboard search | `shell-search-focus.log` failed: query remained focused (selected first result). `shell-search-focus-replay.log` failed before Enter: explicit second-result focus assertion false after Compose-injected Tab. Actual UiDevice Tab/Enter with focus assertions passes the method (`INSTRUMENTATION_STATUS_CODE: 0`) in `shell-search-review-native-replay.log`; that 29.914 s two-method batch still fails its Review companion. The focused-search method was not rerun after its accepted pass. |
| Review Area/source/return/recreation | Earlier `shell-review-area-return.log` fails on uncomposed options; `shell-review-return-correlations-replay.log` has an ambiguous foreground/underlying scroll selector; `shell-search-review-native-replay.log` cannot find the lazily hidden Area control after return. Final fixture reveals the compact dashboard's retained options summary and Tasks signal. `shell-final-review-area-return.log`: 1 passed, 35.442 s; strict Work summary/stored Area, Monthly period, saved section selection, exact Task total 1 and all-Track evidence survive source recreation/return. |
| Shared supporting workspace geometry, normal text | `shell-final-supporting-normal.log`: 1 passed, 21.448 s. Exact toolbar/search/context rectangles match Tracks and Gym; original PNG/XML pairs inspected. Toolbar `[0,0][1080,300]`, search `[795,147][932,284]`, context `[53,451][1027,640]` match both destinations. |
| Shared supporting workspace geometry, actual Android 200% text | `shell-supporting-native200.log` preserves the original 5 px toolbar/context and 3 px search mismatch (1 failure, 14.266 s). After measured-height repair, `shell-final-supporting-native200.log`: 1 passed, 15.926 s, unchanged exact assertions. Toolbar `[0,0][1080,305]`, search `[795,150][932,287]`, context `[53,456][1027,729]` match both destinations. Android font scale restored to 1.0. |

Capture provenance: device catalog was explicitly cleared immediately after APK installation before the initial shell tests; all files pulled into `artifacts/fresh-app-overhaul/2026-09-29/after/shell` were captured during this goal. `shared.review.area-evidence` was newly captured by the current Review journey before its fixture failure. `settings.backup-preview`, `settings.restore-preview` and `settings.restore.failure-retry` were newly captured by the current restore pair. Successful visual captures include `fresh.review.scaled-outcomes.large`, `fresh.settings.emoji-failure.large`, `shared.home.summary-large-rtl`, and both available/unavailable correlation states. Settings standalone anchor captures use a bare theme without a root Surface and are behavioral evidence only for positioning, not product color acceptance. Parent-requested true-root normal/200% supporting overview captures are recorded separately.

Final shell continuation: parent-built current APKs were installed with `-r` on guarded `emulator-5556` (`shell-final-install.log`, both Success). The three exact methods above each use `timeout --kill-after=3s 55s adb -s emulator-5556 shell am instrument -w -r -e class CLASS#METHOD commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner`; no Gradle/native runs overlap on that device. Catalog cleared before each run, then pulled through `scripts/device-artifacts` into `after/shell-final` (8 files), `after/supporting-final` (6) and `after/large-supporting-final` (6). Prior failed PNG/XML files remain untouched in `after/large-supporting`.

The seven final original PNGs were inspected: returned Review communicates monthly outcomes and all-Track evidence with visible daily scale; normal and enlarged Tracks/Gym keep readable identity and aligned single search; Settings category copy wraps naturally at enlarged text and continues in its scrollable index. Native assertions establish restored Area and exact geometry; the Review capture is intentionally scrolled to the outcome/evidence content. Source and workbook pass `git diff --check`. No shell remedy remains pending; final integrated readiness and memory/commit delivery remain with the parent.
