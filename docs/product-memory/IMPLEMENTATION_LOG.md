# Implementation history

### IMP-20260909-022 — Keep live Track definitions and Entries consistent

- Behavior: Track projections observe invalidation of Tracks, Fields, Choices, Entries and Values, then load all five ordered tables in one transaction. Domain conversion/grouping happens outside the transaction. Live identity/summary consumers receive one committed graph instead of combinations of different table revisions.
- Preservation: Retain current display/history ordering, the non-empty Field guard and public per-table streams. CSV export and mutation/preparation boundaries already use transactions and remain unchanged. Five bulk reads avoid per-Track query multiplication; full large-history responsiveness is still an audit requirement.
- Regression: Two observer tests validate every collected graph during definition creation/rapid concurrent renaming and Entry creation, including required Title/Choice values and newest-first order. Both failed before the correction. They now belong to the permanent Tracks profile, which selects 64 JVM / 58 Android tests.
- Verification: Final regressions pass on API 26/34/37; API 34's broader campaign passes 64 JVM / 64 Android tests including ordinary/enlarged authoring and history/recovery, plus two ordinary-text numeric Insights journeys. Readiness passes 344 JVM tests, both check-wrapper fixtures, Android compilation, lint, packaging and static guards. Evidence: `artifacts/astra-audit/2026-09-09/track-projections/README.md`.
- Related/status: FND-20260909-022, DEC-20260909-020, VER-20260909-023, FB-20260908-006. Verified for live projection coherence; no UI/asset/schema/data-format/version or physical-device changes. Source inventory is 1,630 tests; 299 catalog/matrix states and 38 font fixtures remain. Whole Tracks and whole-product acceptance remain open.

### IMP-20260909-021 — Keep nested Track input and short Date selection usable

- Behavior: The Field editor scrolls its heading with configuration, retains an accessible pane title and fixed Cancel/Save, and relocates the whole focused Field Name when its viewport changes. The shared Date picker scrolls its body so its three-row wheels keep their designed height on short enlarged windows; Cancel/Set remain fixed.
- User benefit: The small actual-200% Field Name and floating label remain visible above the native keyboard. The reproduced short Date-picker idle failure is eliminated while the opening date, explicit confirmation and persisted value remain intact.
- Journey: Two real-app tests author all seven Field types, configure units/precision, reorder/remove Choice options, configure fractional Scale, recover/discard nested drafts, validate and save an Entry, recreate an invalid numeric edit, correct it and reopen the same persisted Entry. Assertions use a transactional snapshot after observable commits and ignore only refreshed revision timestamps for untouched values; this does not claim to fix the open observable-projection coherence lead.
- Verification: Both ordinary/actual-200% journeys pass on API 26/34/37; 64 JVM / 110 Android neighboring tests and 344 JVM readiness tests pass with compilation/lint/packaging/static guards. All 62 retained original images have individual review; 54 final native hierarchies contain zero unlabeled interactive nodes. Eighteen catalog/matrix states raise the total to 299, with 38 actual-font fixtures and 1,628 source tests.
- Related/evidence: FND-20260909-020/021, DEC-20260909-018/019, VER-20260909-022, FB-20260908-006; `artifacts/astra-audit/2026-09-09/track-authoring/README.md`.
- Status: Verified for this recovered authoring/input/Date chunk. No data/schema/backup/version or release changes. Complete Tracks and whole-product acceptance remain open; Entry introduction, wide form composition, remaining validation/CSV/source and projection-consumer review continue.

### IMP-20260909-020 — Give short Track detail room for saved history

- Behavior: In a compact content window under 600 dp high, a resolved selected Track uses its local detail flow: Back, compact identity, Edit, destinations, history tools and contextual Restore remain, while the global content bars yield. Returning to the collection restores the full named primary navigation and global Area/search/settings controls. Normal phone and wide layouts retain their existing navigation.
- Capability: Active focused Tracks expose a local 48 dp Add Entry action with the same Track-qualified description and editor owner as the global Add action. Archived Tracks continue to suppress Entry creation until restoration persists. Stable content parents preserve selection, query and lifecycle state.
- Accessibility: Restored active Entry Edit/More labels now belong to their interactive buttons, so partially visible actions retain identity when their icon children scroll out of the viewport. Visible icons and callback behavior are unchanged; the final native catalog gate verifies the discovered clipping state.
- Verification: Both ordinary/actual-200% journeys pass on API 26/34/37, including complete first-title/date bounds, native query/Back, older records, recreation, unchanged history, named navigation after Back, Add Entry open/cancel and Options/Insights/Entries return. The broader recovered-layout run passed 64 JVM / 111 Android tests; after the action-label correction, final Tracks coverage passed 64 JVM / 56 Android tests. All 39 retained originals have individual review; final readiness passes 344 JVM checks, Android compilation, lint, packaging and static guards. Evidence: `artifacts/astra-audit/2026-09-09/focused-track-detail/README.md`.
- Related: FND-20260909-019, DEC-20260909-017, VER-20260909-021, FB-20260908-006.
- Status: Verified for this coherent history/navigation/accessibility chunk. No data/schema/backup/version or release changes. Whole Tracks review remains VER-20260909-016.

### IMP-20260909-019 — Correct the Track visual review using byte and pixel evidence

- Outcome: Reconcile the missing-icon, Area-clipping, overlapping-row and touching-label review hypotheses against original PNG pixels and native bounds. Correct the affected evidence notes explicitly; original images remain byte-identical. No production or permanent test/capture-harness behavior changes.
- Investigation: Existing ordinary/enlarged Track journeys pass with an opt-in four-frame diagnostic overlay on both API 26/34. Stable image hashes, complete X-shaped icon pixels, normal margins and separated native text intervals reject those hypotheses. Remove the overlay from live source and retain its patch with reproducible evidence.
- Remaining product work: The smallest enlarged archive still shows barely any initial record content; FND-20260909-019 is narrowed to that supported composition opportunity. Whole Tracks remains open.
- Related: FND-20260909-019, VER-20260909-020, FB-20260908-006.
- Status: Verified for this evidence correction; product/test source remains exactly `6c95190`.

### IMP-20260909-018 — Keep inline input and short Track history readable

- Behavior: MainActivity requests resize, and the active content Scaffold consumes keyboard space while persistent side navigation retains its position. Global content chrome yields while an inline keyboard is visible. Short Track detail keeps Back, a compact one-line title, width-appropriate Edit and inner destinations; repeated count/Area metadata yields to history. The existing archive/restore operation and stored records are unchanged.
- Shared input: `FocusedInputVisibility.kt` requests the whole focused field plus an 8 dp floating-label allowance when its scrolling viewport changes. Track search and Task Quick Capture use it without changing parents or requesting keyboard focus. While Quick Capture owns the keyboard, its repeated destination introduction/list toolbar yields; workspace selection remains, and the controls return on dismissal. “Task for Today” / “Task for Inbox” keeps the saving destination legible at enlarged text.
- Verification additions: Strengthened real Track history journeys check full native query/label, identity below status icons, 48 dp Back, older record date, reachable search result, explicit keyboard dismissal/draft retention, recreation and unchanged history after Restore. Two native Quick Capture journeys verify actual text scale, draft recreation, complete field/label, IME save, accepted-draft clearing, retained destination and persisted reopening. Native assertions reuse the catalog's accessibility refresh. Four new states bring the current catalog to 279; the new font owner brings that inventory to 37.
- Compatibility/tradeoffs: No schema, history, unit, notification, data, version or release change. Short Track titles can ellipsize while retaining complete accessible text. Keyboard-visible compact workflows use local workspace navigation; other global controls return when the keyboard closes. Native small-screen search can require scrolling between the full query and full result.
- Related: FND-20260909-018, DEC-20260909-016, VER-20260909-019, FB-20260908-006.
- Status: Verified for full native input/label, short record reading, draft recovery and saving: four final journeys on each API 26/34/37, 64 JVM / 109 Android neighbors and 344 JVM readiness tests pass. All 36 final originals plus five before/supplemental frames have individual scoped review in artifacts/astra-audit/2026-09-09/inline-keyboard/. Whole Tracks and FND-20260909-019 remain open; no blanket visual acceptance is claimed.

### IMP-20260909-017 — Prioritize archived history and contextual restoration

- Behavior: Replace the centered archive hero with a naturally wrapping read-only status and the existing Restore Track command. On single-pane Track detail with less than 440 dp of workspace height, retain the existing Back/identity/inner destinations and omit the redundant workspace tab row. Collections and wide split panes retain workspace tabs. On the API 34 phone the first Entry title moves up 221 pixels at ordinary text and 444 pixels at actual 200% text.
- Sources: `TrackScreens.kt`, updated archived-state assertion in `TrackWorkspaceUiTest`, and two real `TrackHistoryJourneyE2ETest` tests. The new journey browses 30 persisted Entries, searches with a real keyboard, recreates, inspects an exact read-only record, returns to Archived, reopens and restores, then verifies unchanged Field/Choice/Entry graphs and restored Edit availability.
- Inventory/evidence: All 28 original Track baseline frames now have individual review notes and retained evidence in `track-baseline/`. Eight new native history states and one actual-font owner expand the coverage inventory. Current baseline labels now distinguish an empty filter draft, a new date condition and missing Entry. Final before/after/platform evidence lives in `artifacts/astra-audit/2026-09-09/track-history/`.
- Compatibility: The existing ViewModel/repository owns restoration and archived Entry capabilities. No schema, stored Entry, unit, version, release or physical-device changes. The short-detail navigation tradeoff is one Back step to another workspace destination.
- Related: FND-20260909-017, DEC-20260909-015, VER-20260909-016/018, FB-20260908-006.
- Status: Verified for this bounded archive/status/navigation improvement: both native journeys pass on API 26/34/37; 64 JVM / 97 Android neighbors, 344 JVM readiness tests, compile/build/lint/static checks pass. Thirty retained original before/final images were individually reviewed with the limits recorded in track-history/review.tsv. Eight new catalog states and one actual-font owner bring the current inventory to 275 states / 36 font fixtures. FND-20260909-018 explicitly retains the native keyboard and very short enlarged-history viewport problems; no whole-Track acceptance is claimed.

### IMP-20260909-016 — Present Track summaries in the correct unit and precision

- Behavior: Workspace and per-Track Insights share `TrackInsightNumberFormat`. Canonical measurements convert to the Field unit with configured precision; fine Scale increments and fractional averages remain visible. Absolute temperatures and differences use the appropriate conversion. Temperature and Scale totals are omitted while useful averages, ranges and trends remain. One mile plus one kilometre now shows 1.621 mi, replacing the workspace's incorrect 2609.34 mi.
- Sources: `ui/TrackInsightNumberFormat.kt`, `ui/TrackScreens.kt`, five JVM formatter cases, and two real persisted `TrackInsightsJourneyE2ETest` journeys. The test fixture verifies recreation, real scrolling and complete native numeric bounds, settling Compose before native capture.
- Compatibility: No Entry, unit definition, persisted aggregation, schema, version or release change. Four new catalog states cover measurements absent from the existing text-only catalog fixture; overlapping temperature/Scale frames are represented once per page.
- Verification: 69 neighboring JVM / 56 Android tests, final two-owner/four-state captures on each API 34/37, eight individually inspected final images, and 349 JVM readiness tests plus compile/build/lint/static checks pass. Preserve the failing original workspace image and fixture failures honestly in VER-20260909-017. Evidence: `artifacts/astra-audit/2026-09-09/track-numeric-insights/README.md`.
- Related: FND-20260909-016, DEC-20260909-014, VER-20260909-016/017, FB-20260908-006.
- Status: Verified for numeric truth and scoped ordinary-text readability. Whole Tracks source/journey/design/accessibility review continues; date-window semantics and wider composition remain separate follow-ups.

### IMP-20260909-015 — Verify existing Task editing through interruption and reopening

- Change: Add ordinary and actual-200%-text real-app journeys in `TaskEditorJourneyE2ETest`. They scroll Home to the persisted Task, open its inspector/editor, check complete native label/field/header visibility around the software keyboard, edit, recreate the draft, save, and recreate/reopen. Exact entity comparison protects untouched Task values.
- Product judgment: Fresh API 26 ordinary/enlarged journeys show readable focused fields on unchanged production. The earlier status-only frame does not establish a settled Task defect. Preserve existing focus/layout behavior; do not impose an unrequested keyboard-closed contract or add speculative focus/timing workarounds.
- Inventory: Eight discovered native states and one actual-font owner supplement existing new/shared-Task coverage. No production, schema, version, release or physical-device change.
- Related: FB-20260908-006, FND-20260909-010/013/015, VER-20260909-015.
- Status: Verified for this existing-Task native edit/interruption journey. Both tests pass on API 26/34/37; the two-owner/eight-state final capture, 18 individually inspected platform/catalog frames, and 344 JVM readiness checks pass. Evidence: `artifacts/astra-audit/2026-09-09/task-edit-journey/README.md`. Full Task/wide-form and whole-product review remain open.

### IMP-20260909-014 — Keep status icons readable over dimmed light dialogs

- Behavior: The shared WhipDialog boundary uses light status icons over its dimmed exterior. Search explicitly retains content-theme icons over the opaque background it paints through the inset. Activity status appearance, navigation icons/scrims, dim amount, layout, focus and dismissal behavior remain unchanged.
- Sources: `ui/theme/WhipDialog.kt`, `WhipActivityTheme.kt`, the explicit Search backdrop argument in `UnifiedSearchDialog.kt`; existing `DialogThemeContrastTest` now measures dominant native status foreground/background contrast and captures the four previously missing light owners.
- Compatibility: No authored data, history, schema, version, preference or release change. DEC-20260909-013 refines the earlier assumption that dialog content and status backdrops always share a theme.
- Verification: Six focused API 34 tests, 103 neighboring Android / 56 JVM tests, four theme/native Search/shared-Task journeys on each API 26/37, four owners / 15 final catalog states, 19 personally inspected final images and 344 JVM readiness checks pass. Welcome contrast improves from 2.4143:1 to 6.1932:1. Evidence: `artifacts/astra-audit/2026-09-09/dialog-status/README.md`. The separately observed API 26 existing-Task title/keyboard viewport remains a journey follow-up.
- Related: FND-20260909-015, DEC-20260909-013, VER-20260909-014, FB-20260908-006.
- Status: Verified for this native status-contrast correction; whole-product acceptance remains open.

### IMP-20260909-013 — Keep Search usable inside short keyboard-constrained windows

- Change: Unified Search paints its existing background through the native window and explicitly consumes system-bar and keyboard insets. One stable controls/divider/results parent structure preserves query focus across Wide/Compact reflow. Below 440 dp of workspace height, its title/exit and query remain fixed while scope/actions, filters, result count and results share the existing lazy list. Short query spacing uses the existing micro token; taller compact and wide controls keep their arrangement. Settled result announcements remain available on the short pane even when its heading scrolls away.
- Sources: `UnifiedSearchDialog.kt`; real app regression in `GlobalSearchRoutingTest.kt` and deterministic bidirectional reflow regression in `UnifiedSearchAdaptiveUiTest.kt`; shared native header helper now accepts a stable title tag for Search's duplicate visible label. The new journey verifies actual Android 200% text, a fully visible native result title, scope/Match Any, recreation and exact persisted Task routing.
- Compatibility: Search engine/index budgets, partial-result meaning, scope, filters, result identity and persistence remain unchanged. No schema/version or release action.
- Related: FND-20260909-014, DEC-20260909-012, VER-20260909-013, FB-20260908-006.
- Status: Verified. Final 56 JVM / 99 adjacent Android tests, native API 26/34/37 journeys, four owners / 13 final catalog states, all 19 final platform/catalog images personally reviewed, and 344 JVM readiness checks pass. Preserved evidence: `artifacts/astra-audit/2026-09-09/native-search/README.md`; whole-product acceptance remains open.

### IMP-20260909-012 — Keep the Task editor header visible above the keyboard

- Behavior: Task's dialog explicitly consumes system-bar and IME insets. Initial focus and the keyboard controller belong to the actual dialog window, once per new editor instance. Vertical form padding now scrolls with content, preserving edge spacing while keeping the focused label readable on short windows.
- Verification infrastructure: Native header/exit and focused-label bounds supplement actual Android text-scale assertions. The real oversized-share journey covers cold launch, recreation, edited durable save and Inbox inspector/reopening; Task list scrolling uses a stable tag. Existing Quick Capture tests now wait for the visible enabled/cleared state after persistence, and the Inbox test scrolls to its explanation. Four new states bring the catalog to 248; 33 fixtures verify actual font scale. Product source counts remain 625 JVM / 986 Android.
- Compatibility: Draft values, shortened-share meaning, Save/Cancel, existing-task focus and request/persistence ownership remain intact. Habit/Goal retain their primary shell after native keyboard/header checks passed. No schema, backup, version, release, or physical-device change.
- Related: FND-20260909-010, FND-20260909-013, DEC-20260909-011, VER-20260909-012.
- Status: Verified for scoped Task insets/focus and checked neighbors: 87 adjacent Android tests, API 26/34/37 journeys, 13 final catalog images plus five platform images personally inspected, and 344 JVM readiness checks with compile/build/lint pass. Full product acceptance remains open.

### IMP-20260909-011 — Route changed tests by their declared package

- Behavior: `scripts/change-router` reads the Kotlin package and verifies its filename/top-level-class contract before emitting an exact selector. Missing or unsupported changed source broadens to all tests in the relevant source set; deleted tests retain feature-profile routing.
- Verification infrastructure: `scripts/test-change-router` covers both existing misplaced Android classes plus isolated JVM package mismatch, missing Android source, unsupported JVM class identity, and renamed tests. Existing deterministic profile, deduplication, documentation, and release-boundary fixtures remain intact.
- Compatibility: No app source, data/schema, product test count, catalog state, version, physical-device, or release change. Instrumentation still validates the exact selected class independently.
- Related: FND-20260909-012, DEC-20260909-010, VER-20260909-011.
- Status: Verified; 18 routing fixtures, fast/full check-wrapper fixtures, the 19-test real Area wrapper integration, and routed readiness pass. The full app audit remains active.

### IMP-20260909-010 — Make Area deletion choices and cleanup consequences readable

- Behavior: Area deletion opts into a larger parent-bounded choice list with a scrolling heading. Nonzero impact counts use existing singular/plural wording. The preservation destinations precede extended details; deletion permanence and saved filter/widget resets remain explicit, including empty Areas. Saving also disables destination changes.
- Source/compatibility: `AreaManagementDialog.kt` and an optional `WhipChoiceList.maxHeight` in `WhipPagePatterns.kt`. The default choice cap is unchanged for neighbors. Destination identity, exact callbacks, repository transactions, lifecycle receipts, data/history, schema, version, and release state are preserved.
- Inventory: Strengthen the existing ordinary first-choice regression and add enlarged busy/failure/retry plus empty-cleanup tests. Three new states bring the catalog/matrix to 244; source inventory is 625 JVM / 986 Android.
- Related: FND-20260909-011, DEC-20260909-009, VER-20260909-010, DEC-20260902-008.
- Status: Verified; the reproduced ordinary bounds failure is corrected, all 3 focused / 73 adjacent Android tests and 5 capture owners pass, all 7 final states are personally reviewed, and 344 JVM readiness tests plus compile/build/lint/static checks pass. Complete organization and whole-product acceptance remain open.

### IMP-20260909-009 — Preserve reading space and initial choices in enlarged dialogs

- Behavior: Long destructive, Task-template, and backup-preview headings participate in the existing content scroll while actions remain outside it. Destructive retry errors lead the impact details. Template names are concise and guidance follows choices. Backup choices expose each action once with its supporting meaning; record count/export date stay ahead of choices, complete metadata remains below, and compatibility errors stay prominent. Shared choice-list height grows with text size within parent constraints.
- Source: `ProductivityEditorComponents.kt` permits an optional fixed title and provides `WhipDialogHeading`; `PermanentDeleteDialog.kt`, `TaskEditorDialog.kt`, `SettingsScreens.kt`, and `WhipPagePatterns.kt` apply the scoped changes. Ordinary titles and primary-editor chrome retain their roles.
- Verification/inventory: Strengthened four actual-large-text fixtures with useful reading bounds, complete initial choices/error visibility, stable actions, and exact Subtask draft. One new editor-to-template-to-Save journey verifies the dated editable draft. Three captures expand the catalog/matrix to 241; current source is 625 JVM / 984 Android. Correct the testing guide's stale 1606/625/981 sentence to 1609/625/984.
- Compatibility: Existing single scroll owners, requested text scale, destructive/busy guards, replacement second gate, merge compatibility, authored values and history are preserved. No schema, backup format, version, release, or physical-device change.
- Related: `FB-20260908-006`, `FND-20260909-009`, `DEC-20260909-008`, `VER-20260909-009`.
- Status: Verified: 9 focused and 86 adjacent Android tests, 38 catalog owners / 92 captures plus 2-owner / 4-state final backup replacement, and 344 JVM readiness tests with compile/build/lint/static checks pass. Sixteen final states were personally reviewed; fifteen have scoped layout acceptance and Area deletion remains FND-20260909-011.

### IMP-20260909-008 — Verify remaining dialog text scaling and prioritize CSV recovery

- Behavior: Track CSV preview presents its file-recovery action immediately after the active error, ahead of file/date/mapping controls. Routine mapping guidance is secondary and hidden while an authoritative error is present. Empty, invalid, failed, completed, and unavailable-target states retain their action guards and existing replacement meanings.
- Verification infrastructure: Twenty-eight existing methods across 17 Android classes now use the shared pre-launch font rule and assert real dialog TextLayoutResult scaling. Correct three fully qualified selectors to their declared Kotlin packages. Offscreen Machine impact and frozen CSV mapping assertions scroll their actual list owners.
- Inventory: Twenty-six new catalog states bring the catalog and audit matrix to 238; source inventory remains 625 JVM / 983 Android. All 32 font-review entries now have verified scale evidence. Preserve 34 fresh state pairs, CSV before/after comparisons, and per-state review notes under `artifacts/astra-audit/2026-09-09/dialog-font-coverage/`.
- Compatibility: No domain, persistence, schema, backup, version, release, or history change. No production font override; the emulator setting restores to 1.0. CSV regression asserts visible action order and one replacement callback.
- Related: `FB-20260908-006`, `FND-20260909-006/008/009/010`, `DEC-20260909-005/007`, `VER-20260909-008`.
- Status: Verified; 15 focused Android tests, 29 catalog owners / 34 inspected states, and 344 JVM readiness checks with Android-test compilation/debug build/lint pass. Short-dialog reading space and the shared Task keyboard frame remain open findings, not accepted layouts.

### IMP-20260909-007 — Give enlarged inspector identity room and verify real dialog text scaling

- Behavior: Narrow or enlarged-text inspector headers place emoji and trailing Edit/Close actions above full-width title, context, and status. Roomy headers retain their existing arrangement; tabs, scrollable body, primary action, and domain callbacks retain their roles.
- Source: `ui/EntityInspector.kt`; shared `AndroidFontScaleRule` configures Android before Compose activity launch and restores the setting after teardown. EntityInspector, nested-choice, and first-run persistence tests assert actual rendered dialog text scale. Inspector assertions check painted line bounds rather than unused paragraph width in wrap-content status badges.
- Inventory/evidence: Five explicit dialog states expand the catalog/matrix from 207 to 212. The source count remains 625 JVM / 983 Android tests. The 32-fixture review inventory records four corrected fixtures and 28 still requiring actual dialog scaling verification; passing other old outer-density fixtures does not close that backlog.
- Compatibility: No data, schema, backup, history, version, or release change. Task Subtask conversion is reached through the existing body scroll; no new navigation or callback behavior is introduced.
- Related: `FB-20260908-006`, `FND-20260909-006/007`, `DEC-20260909-005/006`, `VER-20260909-007`.
- Status: Verified; 31 focused Android tests, 17 catalog owners / 50 exact states, and 344 JVM readiness checks with Android-test compilation/debug build/lint pass. Whole-product acceptance remains pending.

### IMP-20260909-006 — Dialog windows follow Whip's selected theme

- Behavior: Dialog status/navigation appearance follows the rendered Whip theme independently of Android's theme. The dark welcome, Search, and shared primary editor now keep visible status icons; theme changes and recreation update the owned window, and dismissal retains the activity appearance.
- Source: `ui/theme/WhipDialog.kt`, `Theme.kt:LocalWhipDarkTheme`, and `WhipActivityTheme.kt:applyWhipWindowAppearance`. All four native dialog creators in ProductivityEditorComponents, TaskEditorDialog, EntityInspector, and UnifiedSearchDialog use the shared boundary with their existing DialogProperties and content.
- Verification/inventory: Two real `DialogThemeContrastTest` journeys cover shared alert/primary, Task, inspector, Search, live changes, recreation, and return. Catalog discovery recognizes themed calls without losing their owners; its fixture protects that contract. Six theme-specific states bring the catalog to 207; the source inventory is 625 JVM / 983 Android tests. Exact changed scope is recorded in `VER-20260909-006`.
- Compatibility: No preference, persistence, schema, backup, history, layout, dismissal, or release change. Shared legacy scrims apply only below API 29; newer Android retains automatic navigation contrast handling.
- Related: `FB-20260908-006`, `FND-20260909-004`, `DEC-20260909-004`.
- Status: Verified on API 26/34/37 with 100 neighboring Android regressions, 344 JVM readiness checks, debug build/lint, and inspected fresh window/first-run captures. Whole-product acceptance remains pending.

### IMP-20260909-005 — Let selected Home sections lead the getting-started guide

- Behavior changed: Empty Home's starting group follows the currently visible Home sections in their saved order. Every other tool remains discoverable below. Group membership uses stable `HomeSection` identity instead of Task/Habit display titles; an all-selected Home has no empty secondary group.
- Important files: `ui/WhipApp.kt:HomeContent/HomeGettingStarted/HomeDestinationLinks`, `FirstRunJourneyTest`, and `ui/HomeDestinationLinksTest`. Current source inventory is 625 JVM / 981 Android / 201 catalog states.
- Compatibility: No new setting, schema/backup change, data mutation, release, or card-geometry change. The existing hidden-section discovery guarantee remains exercised by Settings and destination-action tests.
- Evidence: [Selected Home priority](../../artifacts/astra-audit/2026-09-09/home-priority/README.md). The preceding first-run save/layout chunk is committed and reachable on `origin/main` as `025f29d`.
- Related: `FB-20260908-006`, `FND-20260909-005`, `DEC-20260909-003`.
- Verification: `VER-20260909-005`.
- Status: Verified for empty-Home priority; broader Home/adaptive and whole-product review remain active.

### IMP-20260909-004 — Confirm first-run saves and keep setup actions near their content

- Behavior changed: `FirstRunSetupHost` owns a saveable request and draft until confirmed completion. `SettingsViewModel.completeSetup` now uses the existing durable typed-settings boundary. Pending saves block duplicate input, failures retain choices, lost requests explain interruption and allow retry, and optional notification permission follows the matching success receipt. Completed setup does not consume another Settings editor's result.
- Design: The existing recommended/customize structure uses a content-sized bounded dialog, with a scrollable body and fixed wrapping actions. Empty Home selection explains why saving is disabled; errors scroll into view. At the matched real-app viewport, the gap between the welcome explanation and button label decreases from 1,126 to 76 pixels.
- Important files: `ui/FirstRunSetupDialog.kt`, `ui/SettingsViewModel.kt`, `ui/WhipApp.kt`, `ui/FirstRunSetupPersistenceUiTest.kt`, and eight newly registered catalog states. Source inventory: 625 JVM / 980 Android / 201 catalog states.
- Compatibility: No schema, backup, release, default-choice, or feature-availability change. Failure cases use controlled outcomes in the production host; successful real-activity journeys exercise the actual settings repository. Process interruption is modeled through restored UI state with a lost request, not an actual process-kill campaign.
- Evidence: [First-run before/after](../../artifacts/astra-audit/2026-09-09/first-run/README.md).
- Related: `FB-20260908-006`, `FND-20260909-002`, `FND-20260909-003`, `DEC-20260909-002`; separate follow-ups `FND-20260909-004` through `FND-20260909-006` remain open.
- Verification: `VER-20260909-004`.
- Status: Verified for setup persistence ownership and layout; wider goal remains active.

### IMP-20260909-003 — Preserve real first-run setup journey coverage

- Behavior changed: Added two real-activity tests for recommended setup and customized Tracks/Gym setup. The customized journey retains selections through recreation and checks stored Home visibility, advanced controls, low-pressure presentation, and pound units after completion; the recommended journey checks Tasks/Habits and kilogram defaults. Both verify that no notification permission request is recorded.
- Important files: `app/src/androidTest/java/com/whip/app/FirstRunJourneyTest.kt`; current source inventory reconciled to 625 JVM / 975 Android tests.
- Compatibility and limitations: Test-only checkpoint requested by the owner. Production behavior, data formats, and release remain unchanged. Exploratory captures are not yet registered or visually accepted catalog states; the canonical inventory remains 193. These success journeys do not prove disk-write failure handling or process-death durability.
- Related: `FB-20260908-006`, `FB-20260831-014`.
- Verification: `VER-20260909-003`.
- Status: Verified for the two targeted journeys; first-run audit remains open.

### IMP-20260909-002 — Keep Android window chrome readable in Whip's selected theme

- Behavior changed: Main, widget-configuration, and Health-rationale activity content share `WhipActivityTheme`. Both system-bar icon appearances follow the rendered Whip theme; recovery follows Android's theme and returns to the stored Whip preference. Android 8–9 navigation scrims also follow the content; modern automatic contrast protection is preserved.
- Important files: `MainActivity.kt`, `ExternalWhipActivityHost.kt`, `ui/theme/WhipActivityTheme.kt`, `ActivityThemeContrastTest.kt`. Five opposite-theme/recovery states expand the catalog and active surface matrix to 193 states. Source inventory is 625 JVM plus 973 Android tests.
- Compatibility: No settings, Room schema, backup version, domain/history, navigation, or release change. The pure component `WhipTheme` retains its existing behavior. The Android 8 test branch exports screenshots to private external files because the general catalog uses Android 10+ MediaStore Downloads.
- Evidence: `artifacts/astra-audit/2026-09-09/theme/README.md`; matched before/after API 34 captures plus small API 26 and wide API 37 images.
- Related: `FB-20260908-006`, `FND-20260908-015`, `DEC-20260909-001`; supporting guard fix `81c4213` is separately committed and reachable from `origin/main`.
- Verification: `VER-20260909-002`.
- Status: Verified. Broader shared/Home and whole-product review remains active.

### IMP-20260909-001 — Recognize legacy emulators in the canonical Android target guard

- Behavior changed: When `ro.boot.qemu` is absent, the guard checks `ro.kernel.qemu`; instrumentation still requires a connected explicit target with positive emulator identity. Legacy emulators also remain excluded from release actions. Catalog artifact operations now reuse the canonical guard.
- Important files: `scripts/android-target-guard`, `scripts/device-artifacts`, `scripts/test-android-target-guard`.
- Compatibility: This restores API 26 verification access; no guard override, product behavior, data, or release changed. The guard fixtures cover missing/negative/failed legacy reads and both instrumentation/artifact ownership.
- Related: `FB-20260908-006`, `FND-20260909-001`.
- Verification: `VER-20260909-001`.
- Status: Verified.

### IMP-20260908-014 — Make Home progress truthful and daily actions easier to reach

- Behavior changed: Home now presents responsive Task/Habit summaries beside one another where readable, stacks them for narrow/enlarged text, and places secondary Review alongside its heading. A clear day owns one Review action. Skipped and unavailable Habits no longer depress the scored completion denominator or masquerade as remaining scheduled work; neutral skips are explicit and unresolved timers still count as attention. Adaptive Habit context uses real status instead of generic `log` text. English numeric phrases retain their reading direction inside RTL layouts. Summary counts are withheld while the corresponding domain is loading/failed.
- Important files: `ui/HomeTodayHeader.kt`, `WhipApp.kt`, `values/strings.xml`, `HomeHabitSummaryTest`, `HomeDestinationLinksTest`, `HabitSkipJourneyE2ETest`, `VisualCatalogPagesTest`, `AdaptiveWhipScreenTest`, and the expanded visual catalog. Reviewable before/after evidence: [Home evidence](../../artifacts/astra-audit/2026-09-08/home/README.md).
- Persistence/migration/history impact: Presentation/projection only; data, history, schema 46, epoch 6, backup 26, and release 0.3.66/code 72 remain unchanged. The owner phone was not accessed.
- Compatibility and limitations: Preserves existing Home sections/filters, collection-card geometry, completion/edit/disclosure actions, neutral-skip semantics, and timer recovery. The fresh audit remains active; `FND-20260908-015` records a separate system-bar theme mismatch exposed by this work. Home customization and sparse-use design opportunities still require review.
- Related: `FB-20260908-006`, `FND-20260908-013`, `FND-20260908-014`, `DEC-20260908-010`, `VER-20260908-014`.
- Verification: Four new JVM policy tests; reproduced failing then passing persisted skip/recreation/undo; 348-test routed readiness with lint/build; 161 fresh Android shell/Habit/core/interaction/adaptive/accessibility regressions across two emulators; corrected five-owner, 26-surface shared capture; manual Home/skip/RTL/clear-day image and hierarchy review; final fixture-only readiness and Git/catalog checks.
- Status: Verified; subjective owner validation remains outstanding, and no release is part of this goal.

### IMP-20260908-013 — Establish the fresh Astra audit scope and evidence matrix

- Behavior changed: No application behavior changed. Preserved the owner's complete objective, recorded the new active goal independently of completed audits, and created area, source-owner, and 185-surface review matrices with unverified initial states.
- Important files: `docs/quality/ASTRA_PRODUCT_AUDIT_2026-09-08.md`, `ASTRA_QUALITY_GOAL_2026-09-08.md`, `astra-source-baseline-2026-09-08.tsv`, `astra-surface-review-2026-09-08.tsv`, and the feedback/index ledgers.
- Persistence/migration/history impact: None. Application source remains 0.3.66/code 72 at baseline `4f4a5dc`.
- Related: `FB-20260908-006`, `VER-20260908-013`.
- Verification: Routed readiness, catalog/source discovery, matrix structure, and Git whitespace checks. Fresh runtime capture is running separately and is not claimed as complete.
- Status: Verified.

### IMP-20260831-001 — Gym and 5/3/1 first-class remediation released

- Behavior changed: Added arbitrary-lift 5/3/1 creation; distinct actual/e1RM/TM semantics and adjustable derivation; ordinary-routine TM controls; typed Main/Supplemental/Assistance/Optional work; additive Jokers; performance-informed cycle review; workout-only exercises; contextual picker return; adaptive routine panes; honest no-history state; accessible timers; and timer-boundary correction.
- Important files: `GymEntities.kt`, `RoutineEntities.kt`, `GymRepository.kt`, `RoutineRepository.kt`, `WhipDatabase.kt`, `FiveThreeOneProgression.kt`, `FiveThreeOneBuilder.kt`, `FiveThreeOneCycleReview.kt`, `FiveThreeOneProgramming.kt`, `RoutineBuilder.kt`, `GymScreens.kt`, `WhipApp.kt`, and related JVM/Android tests.
- Persistence/history impact: Explicit migrations through schema 37 and backup format updates preserve historical workout prescriptions and completed work.
- Compatibility: Existing data installed in place; current and historical records are not silently recomputed from edited templates.
- Commit/push: `5fc98dd` on `origin/main`.
- Related: `FB-20260831-001` through `FB-20260831-010`; detailed audit `../GYM_531_PRODUCT_AUDIT_2026-08-31.md`.
- Verification: `VER-20260831-001`, `VER-20260831-002`.
- Status: Released in 0.3.34; continued field validation applies.

### IMP-20260831-002 — Durable product-memory infrastructure

- Behavior changed: Added a personal `maintain-whip-memory` skill, repository fallback instructions, canonical ledgers, stable IDs, evidence-state rules, and a reusable maximum-quality goal with mandatory memory checkpoints.
- Important files: `/mnt/c/Users/commv/.codex/skills/maintain-whip-memory/`, `AGENTS.md`, and `docs/product-memory/`.
- Compatibility: Documentation/process only; no application data or runtime behavior changed.
- Related: `FB-20260831-013`, `FND-20260831-006`, `DEC-20260831-006`.
- Verification: `VER-20260831-003`.
- Status: Implemented and structurally validated; future-task behavioral validation pending.

### IMP-20260831-003 — Coherent commit-and-push discipline

- Behavior changed: The personal memory skill, workspace fallback, memory schema, and maximum-quality goal now require every completed coherent chunk to be narrowly staged, committed, normally pushed, and verified before unrelated work begins.
- Important files: `/mnt/c/Users/commv/.codex/skills/maintain-whip-memory/SKILL.md`, its memory-schema reference, `AGENTS.md`, and `docs/product-memory/`.
- Compatibility: Workflow-only. It explicitly preserves unrelated dirty-worktree changes and prohibits force-push/history rewriting.
- Related: `FB-20260831-014`, `DEC-20260831-007`.
- Verification: `VER-20260831-004`.
- Status: Implemented and structurally verified; ongoing behavioral validation applies.

### IMP-20260831-004 — Fail-closed startup restore recovery

- Behavior changed: Whip now resolves pending interrupted restore before normal product work and routes live replace restore through a counted application-wide admission/drain barrier. Workers, receivers, schedulers, widgets, Health/background jobs, ViewModel operations, editor/import SavedState, notification/widget actions, and WorkManager startup fail closed or carry the current non-restored data generation. Widget display preferences survive restore while identity-bearing references and snapshots are reconciled; late stale actions cannot alias restored rows with reused numeric IDs. Recovery or runtime initialization failure produces a full-screen accessible blocking state with serialized Retry.
- Important files: `WhipApplication.kt`, `MainActivity.kt`, `AndroidManifest.xml`, `data/RestoreRecoveryManager.kt`, `startup/StartupRecoveryGate.kt`, `startup/UserDataGeneration.kt`, `ui/StartupRecoveryScreen.kt`, affected ViewModels, reminder/timer/portable schedulers and receivers, widget providers/factories/preferences/snapshot cache/configuration Activity, recovery strings, and focused JVM/Android regression tests.
- Persistence/history impact: No schema or backup-format change. A failed rollback, invalid snapshot, or failed recovery rebuild keeps the existing private recovery marker. The marker is removed only after both restore and background-state rebuilding succeed, preserving the prior atomicity boundary and all existing user records.
- Compatibility: Generation zero accepts legacy unversioned SavedState/widget snapshots only before the first replace attempt. The generation token is excluded from backups, increments only after a durable recovery marker exists, and invalidates stale identity references without changing historical workout/task/habit/goal records. Ordinary startup still reaches the existing product flow; other AndroidX Startup initializers remain while WorkManager's initializer is removed and application configuration is provided on demand.
- Commit/push: Focused recovery commit containing this entry on `origin/main`.
- Related: `FND-20260831-007`, `DEC-20260831-008`.
- Verification: `VER-20260831-006`.
- Status: Implemented and verified; physical-device release is deferred until the integrated goal release.

### IMP-20260831-005 — Exact live reminder delivery integrity

- Behavior changed: Task, Habit, and Goal reminder work is now an untrusted, versioned exact claim. Delivery and notification actions re-resolve live eligibility, timing, occurrence state, optional action semantics, source-backed progress, and semantic fingerprints before posting or mutating. Stale/malformed work fails closed and reconciles; visible notifications are removed on relevant edits/deletions; early execution requeues the still-due reminder; quiet-hour rollover includes the prior logical day; and fixed/follow-device time behavior is explicitly reconciled.
- Architecture: Production Task/Habit/Goal/Measurement mutations and worker resolve/post decisions share a non-reentrant state boundary. Raw delegates are passed only beneath one outer owner, entity locks always precede state locks, full-snapshot Settings updates are process-serialized, and durable deletion cleanup spans Room and NotificationManager across rollback/process death. One-time claim-version maintenance cancels legacy visible reminders and durably marks success only after every domain rebuild completes.
- Important files: `WhipApplication.kt`, `AndroidManifest.xml`, `core/AppSettings.kt`, deletion coordinators, DAO/repository malformed-data guards, `reminders/ReminderDeliveryClaims.kt`, `CoordinatedReminderRepositories.kt`, `ReminderDeletionCleanupStore.kt`, `ReminderRuntimeMaintenance.kt`, `ReminderTimeChangeReceiver.kt`, all three reminder schedulers/workers/actions/notifications, related ViewModels, and focused JVM/Android tests.
- Persistence/history impact: No Room schema or portable-backup format change. A private claim-version marker and private deletion-cleanup journal are operational metadata only. Existing Task occurrences, Habit logs/skips/pauses, Goal progress, completed workouts, custom units, and historical local dates are preserved and never retroactively recomputed.
- Commit/push: Focused reminder-integrity commit containing this entry on `origin/main`.
- Related: `FND-20260831-008`, partial prerequisite work for `FND-20260831-009`, and `DEC-20260831-009`.
- Verification: `VER-20260831-007`.
- Status: Implemented and verified; physical-device release remains deferred until the integrated maximum-quality goal release.

### IMP-20260831-006 — Coherent Whip calendar context and new-record provenance

- Behavior changed: One eager application-scoped calendar context now carries the active Whip zone, physical date, cutoff-adjusted logical date, cutoff, and follow-device policy. Settings changes, aligned minute ticks, and Android date/time/zone invalidations recompute it. Task, Habit, Goal, Track, Search, Gym, widgets, and root CompositionLocals consume the same source; cross-domain UI retains its prior snapshot until every date-derived projection catches up. Track rolls without a repository write, Search completion filters use the Whip zone and reindex without clearing the query, Gym Today ranges honor the cutoff without resetting a browsed month, and new/default/copied workout and measurement records snapshot Whip date/zone provenance. Explicit historical workout starts derive their physical local date from the supplied instant.
- Important files: `WhipApplication.kt`, `core/AppSettings.kt`, `GymRepository.kt`, `MeasurementRepository.kt`, `TaskViewModel.kt`, `HabitViewModel.kt`, `GoalViewModel.kt`, `TrackViewModel.kt`, `WhipApp.kt`, `UnifiedSearchDialog.kt`, `GymScreens.kt`, `TrackScreens.kt`, and focused JVM/Android tests.
- Persistence/history impact: No Room or backup migration. Existing Task occurrences, Habit/Measurement history, Goal entries, Track entries/import drafts, workout sessions, completed sets, saved zone IDs, and elapsed origins are not recomputed or rewritten.
- Commit/push: `8b27374` on `origin/main`.
- Related: `FND-20260831-009`, `DEC-20260831-010`.
- Verification: `VER-20260831-008`.
- Status: Implemented, focused-verification complete, committed, and pushed; followed by the exact elapsed-Goal subchunk in `IMP-20260831-007`.

### IMP-20260831-007 — Exact elapsed-Goal time and DST behavior

- Behavior changed: Elapsed Goal cards, adaptive summaries, Insights, inspectors, editors, and reset dialogs now share the ViewModel's injected live instant and active Whip zone. An unchanged editor/reset draft preserves the canonical stored instant byte-for-byte, including seconds and milliseconds, even across recomposition, recreation, or a later zone change. Editing wall time uses an exact resolver: spring-forward gaps are rejected with the next valid time explained, and fall-back overlaps expose both offsets and require a deliberate occurrence choice. Start labels use the Whip zone rather than raw UTC/device time. The 30-second ticker runs while Goal UI is subscribed, so future validation also advances while creating or converting the first elapsed Goal.
- Accessibility/UX: The overlap choices are full-width 48dp-class actions with explicit first/second offset labels and selected text. Gap, overlap, and future-state explanations are polite live regions. Reset actions wrap at constrained widths; a 320dp, 200%-font emulator regression constrains the actual dialog surface and proves Reset Now, Cancel, and the chosen-time action remain within it with at least 48dp targets while exact-instant preservation holds.
- Important files: `core/AppRuntime.kt`, `GoalViewModel.kt`, `GoalScreens.kt`, `ProductivityEditorComponents.kt`, `AppRuntimeTest.kt`, `ElapsedGoalPresentationTest.kt`, and `ElapsedGoalTimeUiTest.kt`.
- Persistence/history impact: No schema or backup-format change. `elapsedStartMillis` remains the authoritative immutable instant until a user explicitly saves a changed time; no prior Goal history is rewritten.
- Commit/push: Focused elapsed-Goal commit containing this entry on `origin/main`.
- Related: `FND-20260831-009`, `DEC-20260831-010`.
- Verification: `VER-20260831-009`.
- Status: Implemented and fully verified; integrated physical-device release remains deferred until the maximum-quality goal is complete.

### IMP-20260831-008 — Request-owned productivity definition saves

- Behavior changed: Task, Habit, and Goal definition editors now retain their draft, scroll position, and Area context until their exact asynchronous request settles. Saving blocks Back, pointer, and hardware-key editing with an accessible live overlay. Failure stays inline and retryable; success dismisses or opens a fresh Save-and-New session exactly once and only then reconciles the visible Area.
- Architecture: `PersistenceRequestState` and `EntitySaveReceipt` separate request ownership from global presentation. Admission is atomic and Idle-only; stale terminal delivery is reclaimed without adopting unrelated Running work. `completeCommittedEntitySave` marks the authoritative repository write as the point of no return, preserves the committed identity across cancellation, converts only ordinary post-commit failures to warnings, and lets fatal errors propagate. Saved Area is authoritatively reread; unverifiable or no-longer-active Areas route safely to All Areas. Every authored save retries tag and reminder derivation, making the recovery message truthful.
- Important files: `core/AppRuntime.kt`, `domain/AreaScope.kt`, `ui/ProductivityEditorComponents.kt`, `TaskViewModel.kt`, `HabitViewModel.kt`, `GoalViewModel.kt`, `WhipApp.kt`, `TaskEditorDialog.kt`, `HabitScreens.kt`, `GoalScreens.kt`, and the focused JVM/Android tests.
- Persistence/history impact: No Room schema or backup-format change. Existing entities, completed records, reminder definitions, Areas, tags, and historical data are untouched. The change governs only the sequence and outcome reporting of new authored saves.
- Compatibility: Legacy non-editor ViewModel callers retain Boolean/global operation feedback. UI defaults no longer fabricate success when a request path is absent. Filtered list projections remain scoped while editor entity resolution uses unscoped authoritative state or a captured Task snapshot.
- Commit/push: Focused productivity-save commit containing this entry on `origin/main`.
- Related: `FND-20260831-010`, `FND-20260831-019`, `DEC-20260831-011`.
- Verification: `VER-20260831-010`.
- Status: Implemented, independently accepted, fully verified, committed, and pushed; integrated physical-device release remains deferred.

### IMP-20260831-009 — Request-owned Habit history, pauses, totals, and skip undo

- Behavior changed: Current Habit totals, past check-ins, edited/deleted logs, scheduled pauses, and historical skip undo now remain open and input-shielded until their exact request settles. Failure is announced inline with the draft retained; success closes editors exactly once. The Habit inspector exposes editable scheduled pauses and historical skipped days. Absolute Count/Decimal/Duration entry now says which period total is being set, shows the current total, and explicitly says Save sets rather than adds.
- Architecture: A typed `HabitMutationReceipt` and committed-mutation boundary separate authoritative Room writes from reminder follow-up warnings. Generic persistence coordination now namespaces Home quick entry, Home Habit details, and the Habit workspace; a non-owner cannot steal another surface's result, while an abandoned terminal is reclaimed after an owner grace period. Log/pause snapshots are saveable through target removal and recreation. `UserDataGenerationBoundary` recreates screen identity state after replace restore, and `HabitViewModel` clears outstanding request state on generation changes.
- Correctness: Repository transactions re-read log/pause ownership and require the expected Habit before update/delete. Skip undo requires one exact stored Habit/date row. Absolute Set re-reads the authoritative Room total and all custom units, ignores only bounded binary ULP noise, and writes real high-magnitude changes. Editing a Habit log preserves the backing Measurement entry's Habit/UUID provenance.
- Accessibility/UX: Saving produces one accessible interaction-blocking overlay for dialogs or the full Habit inspector. Draft fields remain scrollable; a 320dp/200%-text regression proves the pause dialog stays within its pane and its Save target remains at least 48dp. State-restoration regressions cover log/pause target removal and user-data-generation replacement.
- Persistence/history impact: No Room schema or portable-backup change. Existing Habits, logs, skips, pauses, custom units, reminder definitions, and historical timestamps remain unchanged. Previously completed history is never recomputed from current settings.
- Important files: `AppRuntime.kt`, `HabitModels.kt`, `HabitDao.kt`, `HabitRepository.kt`, `MeasurementDao.kt`, `MeasurementRepository.kt`, `CoordinatedReminderRepositories.kt`, `HabitViewModel.kt`, `HabitScreens.kt`, `EntityInspector.kt`, `ProductivityEditorComponents.kt`, `WhipApp.kt`, and focused JVM/Android regressions.
- Commit/push: Focused Habit-mutation commit containing this entry on `origin/main`.
- Related: `FND-20260831-019`, `DEC-20260831-015`.
- Verification: `VER-20260831-011`.
- Status: Implemented, independently accepted, and fully verified; integrated physical-device release remains deferred while the maximum-quality goal continues.

### IMP-20260831-010 — Exact Task secondary-mutation and recurring-series integrity

- Behavior changed: Task editors retain drafts and exact edit identity across recreation; stale same-millisecond definition, occurrence, or Subtask changes fail closed. Completed/skipped/archived recurring records edit the series definition, while an open occurrence may safely edit this-and-future. Reschedule, Plan My Day undo, bulk edit/archive, pin, completion/reopen/reset, delete, and notification actions now surface the exact request outcome rather than assuming dispatch means success. Saving and bulk operations shield duplicate input and retain retry context. Date controls, destructive actions, and large-text layouts remain reachable.
- Recurring correctness: Future splits preserve closed history and the old definition, remaining finite occurrence counts, Carry Unfinished state, reschedules, compatible future Open state, Track mappings, Link/Trigger child rules, and stable Subtask identity through reorder/insertion. Inbound automations retarget without duplicate firing. State-only future progress materializes an Open occurrence when necessary; schedule- and completion-anchored projections plus reminders treat explicit Open rows as authored overrides. Copied Goal Links preserve the later of split boundary and configured activation date.
- Commit boundaries: Permanent deletion fingerprints all dependent state, including occurrence/step history, Link/Trigger conditions/choices/mappings, inbound/outbound automation, and linked Track entries. Post-commit ordinary failures become warnings; committed cancellation carries a typed receipt; fatal reconciliation escapes. Notification action claims release only before the authoritative mutation and remain committed through fallible follow-up, preventing replay after success.
- Compatibility/history impact: No Room schema, migration, or portable-backup change. Existing Task UUIDs, definitions, custom Areas/tags, completed/skipped occurrences, completed-set-like Subtask snapshots, Links, automations, Track history, and reminders are preserved. Previously closed history is never recomputed from current recurrence settings. No physical-device mutation or signed release occurred.
- Important files: `TaskModels.kt`, `TaskDao.kt`, `TaskRepository.kt`, `TaskDeletionCoordinator.kt`, `TrackDao.kt`, `ReminderActionReceiver.kt`, `ReminderScheduler.kt`, `ReminderWorker.kt`, `CoordinatedReminderRepositories.kt`, `TaskViewModel.kt`, `TaskComponents.kt`, `TaskEditorDialog.kt`, `WhipApp.kt`, and focused JVM/Android regressions.
- Commit/push: Focused Task-integrity commit containing this entry on `origin/main`.
- Related: `FND-20260831-019`, `DEC-20260831-016`.
- Verification: `VER-20260831-012`.
- Status: Implemented, independently accepted, and fully verified; integrated physical-device release remains deferred while the maximum-quality goal continues.

### IMP-20260831-011 — Exact Goal lifecycle, archive, history, deletion, and backup integrity

- Behavior changed: Goal definition, duplicate, progress create/edit/delete, elapsed reset, status, archive, and permanent deletion flows now surface matching request outcomes, preserve drafts/errors through failure and recreation, block duplicate input while saving, and separate Home from workspace ownership. Progress dates in the future are rejected; dates outside the tracking window require explicit History-only confirmation. History is the truthful shared destination for Completed and Abandoned Goals. Closed/archived Goal-owned progress may be corrected without rewriting the terminal result.
- Domain/history: `GoalMutationBoundary`, `GoalProgressBoundary`, `GoalMeasurementBoundary`, `GoalMilestoneBoundary`, and `GoalEligibilityBoundary` validate only relevant semantic state. Archive is an orthogonal Boolean. Closure snapshots have stable UUIDs and freeze value/progress, elapsed duration, and milestone completion counts; elapsed resets retain old/new origins and prior duration. Same-state milestone writes preserve timestamps, and only open unarchived Goals may gain a pin or new progress.
- Persistence/compatibility: Room schema 38 adds orthogonal archive state, stable closure identity/specialized outcomes, and elapsed-reset history. The migration converts legacy Archived rows without erasing recoverable Completed/Abandoned outcomes. Portable-backup format 16 preserves archive/closure/reset history; pre-v16 upgrades synthesize deterministic closure UUIDs, and repeated merge is idempotent. Existing Goal measurements, completed records, custom units, user exercises, and workout history are not retroactively recomputed.
- Deletion/reminders: Goal deletion SHA-256 revisions cover the Goal, metric, entries, milestones, closure/reset history, and Link rules/conditions/choices/contributions. Area deletion owns aggregate Room deletion transactionally; post-commit settings/focus/widget cleanup continues through ordinary warnings and preserves fatal errors. Archived Goals never schedule or validate reminder claims; archive participates in the reminder fingerprint, and milestone automation excludes archived/non-active Goals.
- Cross-suite correction: Strict authored Measurement edits exposed Health Connect's distinct deterministic upsert contract and a notification fixture that changed source identity while testing value changes. Health reconciliation can now recreate only an identified `HealthConnect` row whose stable ID equals `entry-$sourceId`; existing metric/provenance remains immutable. Link rebuilds create a replacement derived entry when its referenced row is absent. The notification regression retains one provider identity.
- Accessibility/UX: Large-text and narrow dialog content scrolls, input/save/back behavior is explicit, errors use one live region, custom units render human labels, terminal elapsed/milestone outcomes are frozen, and permanent deletion previews complete impact before confirmation.
- Important files: `GoalModels.kt`, `GoalEntities.kt`, `GoalDao.kt`, `GoalRepository.kt`, `MeasurementRepository.kt`, `LinkRepository.kt`, `DomainDeletionCoordinator.kt`, `AreaDeletionCoordinator.kt`, `BackupRepository.kt`, `WhipDatabase.kt`, `GoalReminderScheduler.kt`, `GoalViewModel.kt`, `GoalScreens.kt`, `SettingsViewModel.kt`, and focused JVM/Android/migration/backup tests.
- Related: `FND-20260831-015`, `FND-20260831-016`, `FND-20260831-019`, `DEC-20260831-017`.
- Verification: `VER-20260831-013`.
- Status: Implemented, independently accepted, and fully verified; integrated physical-device release remains deferred while the maximum-quality goal continues.

### IMP-20260901-012 — Exact Gym Exercise and Routine deletion integrity

- Behavior changed: Exercise and Routine permanent deletion no longer dispatch and dismiss optimistically. The UI first loads a complete structured impact, blocks confirmation for active workout use, rejects a stale revision, retains the modal through its exact request outcome, and provides inline retry/review states. The Exercise review distinguishes removed workout/routine sets, changed alternative/machine/category references, deleted PR/graph/Link/Trigger definitions, retained automation-created Track history, and preserved Training Max decisions. Routine deletion states that template/program state is removed while completed/discarded workout snapshots and Training Max decisions remain.
- Domain/architecture: `DomainDeletionCoordinator` now provides transactional Exercise/Routine previews and SHA-256 revisions over every affected row. Exact count assertions guard cascades and reference rewrites. Active Exercise placements—including active substitution alternatives—and active Routine-sourced sessions fail closed. Routine workout source references are cleared only after the reviewed count matches. Training Max decision rows are immutable audit records and are never deleted. PR/Link/settings reconciliation is post-commit, with typed committed-cancellation and ordinary warning semantics.
- Lifecycle/accessibility: Gym deletions have dedicated request-owned state instead of borrowing global `OperationStatus`. Rapid double confirm admits one request. Preview generations reject stale reads; user-data generations invalidate restored numeric identity. A `SavedStateHandle` token plus saveable candidate identity preserves unknown-outcome verification through rotation and repeated process replacement. Present targets resolve to interrupted-before-commit; absent targets rerun idempotent reconciliation and resolve achieved; transient reads keep Retry Verification. Dialog loading, ready, blocker, error, missing, and saving states have polite semantics, modal input blocking, sticky actions, and 320dp/200% reachability.
- Persistence/history impact: No Room schema, migration, or backup-format change. No existing record is rewritten on upgrade. A user-confirmed Exercise permanent deletion removes exactly the disclosed placements/sets and dependencies; archive remains the non-destructive alternative. Routine deletion preserves already performed/discarded workout prescriptions and sets, and both deletions preserve immutable Training Max decisions.
- Important files: `DomainDeletionCoordinator.kt`, `GymDao.kt`, `RoutineDao.kt`, `GymViewModel.kt`, `GymScreens.kt`, `DomainDeletionCoordinatorTest.kt`, `GymDeletionViewModelIntegrationTest.kt`, `GymPowerInputUiTest.kt`, and `docs/testing.md`.
- Related: `FND-20260831-019`, `FND-20260901-021`, `DEC-20260901-018`, `DEC-20260901-019`.
- Verification: `VER-20260901-014`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff and physical-device release remains deferred while the maximum-quality goal continues.

### IMP-20260901-013 — Exact Track definition and history integrity

- Behavior changed: Editing a Track definition now freezes the exact authored definition the user opened. Save rejects concurrent identity or semantic changes without overwriting them, retains the local draft, and offers a normal “Save Draft as New Track” recovery. Removing Fields or Choices requires a repository-originated review of exact affected values, replacement destinations, and dormant legacy Link/Trigger references; any changed impact returns to review. Entry routes resolve from unscoped, generation-bound identity and show an explicit unavailable surface instead of rendering blank or turning Edit into Add.
- Domain/architecture: `TrackDefinitionBoundary` hashes Track identity, editable metadata, and ordered Field/Choice semantics while intentionally excluding Entries, pin/archive/list order, timestamps, derived Area names, and search projections. A separate reviewed-removal fingerprint covers the normalized deletion/replacement plan, exact affected value rows, replacement sibling joins, and dormant compatibility references. Both are recomputed and validated in the same Room transaction before any mutation. Typed conflicts and committed receipts distinguish pre-commit rejection from authoritative success and fallible post-commit Area/tag reconciliation.
- Correctness/compatibility: Field and Choice deletions assert exact transactional postconditions. Choice replacement deduplicates destination joins. Number dimension changes are rejected when history or dormant references exist; same-dimension default-unit changes preserve entered and canonical historical values. Scale changes validate saved values and dormant Trigger constants. Archived Area/unit references may be retained but cannot be newly selected. Schema-31-retired automation definitions are disclosed and reconciled without reviving dead-end configuration, while generated Contribution/TriggerOccurrence history remains intact.
- Lifecycle/accessibility: The definition save coordinator admits one exact request, scopes settlement to route/session/generation, blocks pointer/Back/key input during persistence, and treats the Room commit as authoritative. Unknown process outcomes explain how to verify before retry. Permanent conflict closes stale removal review, clears only its authorization, scrolls the conflict recovery card into view, and announces it. The exact review remains scrollable with reachable actions at 320dp/200% text. Dirty Field dialogs require discard confirmation, and valued Number fields explain locked measurement type/default-unit rules.
- Important files: `TrackModels.kt`, `TrackDao.kt`, `LinkDao.kt`, `TrackRepository.kt`, `TrackEditorViewModels.kt`, `TrackViewModel.kt`, `TrackScreens.kt`, `ProductivityEditorComponents.kt`, `WhipApp.kt`, `TrackDefinitionIntegrityTest.kt`, `TrackDefinitionMutationUiTest.kt`, `TrackRepositoryTest.kt`, and focused JVM/UI regressions.
- Persistence/history impact: No Room schema, migration, or portable-backup change. Existing Tracks, Entries, values, timestamps, Contribution/TriggerOccurrence history, Areas, units, and retired compatibility rows are preserved unless the user approves the exact disclosed destructive impact. Ordinary definition edits do not conflict with unrelated Entry logging/import.
- Related: `FND-20260901-022`, `FND-20260831-019`, `DEC-20260901-020`.
- Verification: `VER-20260901-015`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff and physical-device release remains deferred while the maximum-quality goal continues.

### IMP-20260901-014 — Exact Track Entry mutation and same-process recovery integrity

- Behavior changed: Adding or editing a Track Entry now begins from an atomic repository preparation instead of a lagging list projection. Save/delete owns one exact route, session, data generation, request, form contract, and historical Entry revision; conflicts or failures leave the editor and draft open, while success dismisses exactly once. Delete reviews the persisted date/value count and closes only after commit. Optional malformed Number input remains visible, named, announced, and blocks Save rather than becoming an accidental blank. Active and archived historical units remain visible and understandable.
- Domain/architecture: Create preallocates a saveable Entry UUID and is idempotent only for the exact normalized payload; mismatched reuse is an identity collision and a fresh UUID still permits intentional duplicates. Update/delete compare raw typed values, stable identities, provenance, Track identity, and semantic form/unit contracts inside one Room transaction, including same-millisecond changes and finite canonical-number validation. Presentation no longer owns mutation truth through shared `OperationStatus`; typed receipts are request/route/generation delivered. Structural Entry page versions prevent same-count/same-timestamp values or Choice changes from leaving stale pages.
- Recovery/history: Delete snapshots the exact Entry, values, source occurrence, and all fulfilled occurrences. Same-process Undo restores UUIDs, timestamps, value content, and occurrence links atomically; incompatible Track/Field/value/unit/provenance identity rolls back. Harmless labels, added Fields, archive decoration, and unchanged unit conversions remain compatible. One-level Undo is explicit: confirming a newer delete supersedes an untouched prior Undo, while a failed restore remains bound to its exact snapshot and Retry action.
- UI/accessibility: Pointer, Back, keyboard, and destructive controls are shielded during persistence. Loading, archived, missing, verification failure, conflict, unknown-outcome, and retry states use explicit copy and live semantics. The editor works at 320dp/200% text, Number fields expose their name/unit and raw editable text, and feedback arbitration preserves recoverable actions across destinations without cross-feature Snackbar theft. Cohesive feedback effects were extracted from the root Compose host to keep JaCoCo instrumentation viable without exclusions.
- Persistence/history impact: No Room schema, migration, or portable-backup change. Existing Track, Entry, typed-value, Contribution, TriggerOccurrence, Area, unit, and completed historical facts remain intact; no prior Entry is retroactively recomputed. Undo is intentionally same-process rather than a durable tombstone.
- Important files: `TrackModels.kt`, `TrackDao.kt`, `LinkDao.kt`, `TrackRepository.kt`, `TrackEditorViewModels.kt`, `TrackViewModel.kt`, `TrackScreens.kt`, `TrackEntryFeedback.kt`, `TransientFeedback.kt`, `OperationFeedbackEffects.kt`, `WhipApp.kt`, `TrackEntryIntegrityTest.kt`, `TrackEntryMutationUiTest.kt`, `TrackRepositoryTest.kt`, `LinkBackfillRepositoryTest.kt`, `InteractionControlUiTest.kt`, and focused JVM regressions.
- Related: `FND-20260901-023`, `FND-20260831-019`, `DEC-20260901-021`.
- Verification: `VER-20260901-016`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff. Physical-device release remains deferred while the maximum-quality goal continues.

### IMP-20260901-015 — Durable, exact, and accessible Track CSV batch imports

- Behavior changed: CSV import now owns one saveable batch identity from file selection through completion. Rapid submit is shielded; process-restored sessions verify a complete durable receipt before touching the document URI; exact completed retries return the prior outcome; missing receipts retain the reviewed preview for explicit retry; changed files, stale mappings, replaced Tracks, malformed requests, and batch/Entry identity collisions fail closed. Strict UTF-8 decoding rejects damaged/binary input, CSV quote/header/width rules are explicit, and trailing blank records cannot falsely exceed the data-row limit.
- Domain/architecture: Preview parses exclusively against an atomic `TrackEntryFormSnapshot`, not a lagging projection plus separately cached units. Versioned length-prefixed canonical SHA-256 binds stable Track identity, payload, mapping, fallback date, exact Field/Choice/unit meaning, normalized drafts, and protocol versions. Deterministic UUIDv8 row identities plus a private Track-cascaded receipt make Entries, typed values, FTS rows, and outcome one Room transaction. A fresh batch UUID deliberately permits identical facts. Selected/default unit contracts are compared once per distinct unit; archived non-default units reject while a retained archived Field default remains valid.
- UI/accessibility: The dialog distinguishes target loading, load failure, settled absence, archive, domain conflict, retryable persistence failure, and terminal completion. Completion supersedes stale live projection state. Authoritative errors/actions render before file and mapping detail, remain live at 320dp/200% text, and never contradict a generic coordinator banner. Mapping uses the frozen form even for invalid previews; lookup retry is generation-owned and preserves the session/frozen form. File/date/mapped-count context, progressive mapping disclosure, full input shielding, replacement cancellation, and understandable Replace/Choose/Cancel/Restore actions support one-handed recovery.
- Persistence/compatibility: Room schema 39 adds only `track_csv_import_receipts`, containing stable identity, versioned digests/counts, and commit time—no URI, filename, header, mapping, or value. The row cascades with its Track, is intentionally excluded from portable backup format 16, survives merge/failure rollback locally, and is cleared by successful replace through Track cascade plus data-generation invalidation. Existing Entries, values, FTS, custom units, completed history, and user-defined schemas are not backfilled or recomputed.
- Performance/testing: Batch insertion reuses the loaded form and normalized maps, writes FTS without discarded snapshot reloads, chunks identity preflight, and checkpoints cancellation. The exact 5,000-row × 20-Number-Field case persists 100,000 custom-unit values, 5,000 Entries/FTS rows, one receipt, search reachability, and exact retry in 40.525 seconds on the API 34 emulator (prepare 2.807 s, commit 36.495 s, retry 1.223 s; 120 s harness ceiling).
- Important files: `TrackCsv.kt`, `TrackEntities.kt`, `TrackDao.kt`, `TrackRepository.kt`, `WhipDatabase.kt`, schema 39, `TrackViewModel.kt`, `TrackScreens.kt`, CSV strings, `TrackCsvBatchIdentityTest`, `TrackCsvInputBoundaryTest`, `TrackCsvImportIntegrityTest`, `TrackCsvImportRecoveryViewModelTest`, `TrackCsvImportUiTest`, `WhipDatabaseMigrationTest`, `BackupRepositoryTest`, and `TrackDefinitionIntegrityTest`.
- Related: `FND-20260901-024`, `DEC-20260901-022`.
- Verification: `VER-20260901-017`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff. Physical release remains deferred until the maximum-quality goal is complete.

### IMP-20260901-016 — Exact, recoverable Gym session execution and 5/3/1 outcomes

- Behavior changed: Adding or creating an Exercise for the active workout atomically creates an editable first Set, returns to the workout, and focuses that Set without changing the Routine. Quick-save is bound to stable Set/workout identity and exact revisions; concurrent identical saves append at most once. Workout-only and replacement placements become executable immediately. Finish reviews incomplete work, binds the exact session graph, preserves all saved values in History, and applies explicit Training Max decisions only at valid 5/3/1 boundaries. Joker acceptance remains optional and additive; BBB/FSL/BBS/supplemental work is never displaced.
- Domain/history: `WorkoutExerciseOutcome`, replacement identity, Set removal reasons, authored classification, and `requiredForProgressionSnapshot` separate performed facts from current execution eligibility. Completed retired placements remain available to History and performance review; empty retired placements are not executed or reused. Main, Supplemental, Assistance, and Optional work retain distinct snapshots. Generic workout/routine copies deliberately sanitize program-only progression/failure semantics. Training Max advancement uses immutable required Main-work evidence rather than mutable `planned`; held/ineligible lifts remain auditable.
- Lifecycle/UI/accessibility: Add, create, substitute, detailed Set/Exercise edit, finish/cycle review, and discard use request-owned outcomes. Draft/error state survives Activity recreation; orphaned process results release with an actionable verification warning. Quick-set authorship survives recreation. New-exercise focus is consumed only after a coherent Room snapshot contains the target and scroll completes. Stable finish semantics, scrollable confirmation, narrow 320dp/200% layouts, live errors, input shielding, and explicit History wording support one-handed and assistive use.
- Derived-state reliability: The active Gym projection now rereads Exercise/session/placement/Set/group rows in one Room transaction after invalidation, avoiding mixed-revision UI boundaries. Personal-record reconciliation includes existing PR rows so deleting the last completed Set can remove stale records. Finish/delete/skip/undo distinguish authoritative commit from fallible PR/Link follow-up and warn without inviting duplicate writes. Rest timers carry monotonic revision, exact deadline, and durable cleanup-pending state. Schedule/cancel awaits WorkManager; worker delivery posts a stable notification before exact DB completion, retries ordinary failure, rejects stale generations, and replays idempotently after process death. Gym open and app background recovery rebuild derived timer/PR state from durable rows.
- Persistence/compatibility: Room schema 40 adds `workoutRevision`, immutable placement/Set outcome/progression snapshots, and `restTimerRevision`/`restTimerCleanupPending`; migration derives required Main-work evidence from immutable prescription/classification rather than current `planned` alone and marks preexisting timer state for one cleanup reconciliation. Portable backup format 17 round-trips the new historical facts and defaults pre-v17 fields conservatively. Completed sets, user Exercises, workout equipment/unit/TM snapshots, Routine provenance, and prior history are preserved; completed workouts are never recomputed from a later Routine definition.
- QA-driven corrections: Independent QA first rejected stale PR cleanup and timer reconciliation, then rejected unawaited WorkManager operations and DB-clear-before-notify delivery. The fixes added exact timer acknowledgement and notification-first at-least-once delivery. The full emulator gate then exposed an obsolete E2E path that added a redundant Set after Exercise selection plus a receipt-before-projection focus race. The journey was updated to the intended atomic initial-Set behavior; production gained a coherent Room graph and retained focus handoff. Re-review returned unconditional `ACCEPT`.
- Important files: `GymModels.kt`, `GymEntities.kt`, `GymDao.kt`, `GymRepository.kt`, `RoutineRepository.kt`, `WhipDatabase.kt`, `BackupRepository.kt`, schema 40, `RestTimerNotifications.kt`, `WhipApplication.kt`, `FiveThreeOneCycleReview.kt`, `GymViewModel.kt`, `GymScreens.kt`, `GymRepositoryTest.kt`, `RoutineRepositoryTest.kt`, `WhipDatabaseMigrationTest.kt`, `BackupRepositoryTest.kt`, `GymPowerInputUiTest.kt`, `FiveThreeOneCycleReviewUiTest.kt`, `FirstClassWorkflowE2ETest.kt`, and focused JVM policy/worker tests.
- Related: `FND-20260831-019`, `FND-20260901-025`, `DEC-20260901-023`.
- Verification: `VER-20260901-018`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff. Physical release remains deferred until the maximum-quality goal is complete.

### IMP-20260901-017 — Transactional, accessible typed Settings editing

- Behavior changed: Numeric, clock, time-zone, and other typed Settings are now action rows that open one focused editor. Values remain local until explicit Save; strict parsers reject prefixes and malformed whole fields; normalized no-ops close without persistence; failures retain the draft and offer exact retry; rapid submit admits one request. Region zones, fixed offsets, Follow device, and coupled quiet-hour modes preserve their distinct meaning. Oversized paste is invalid rather than silently truncated. Back/outside/Escape requires deliberate discard when a dirty, conflicting, or durability-ambiguous intent exists, while explicit Cancel deliberately discards it.
- Domain/persistence: `SettingsRepository.updateAndConfirm` and `PortableBackupManager.setRetentionCountAndConfirm` provide serialized durable commits for authored typed values. A failed `SharedPreferences.commit()` restores the prior process-visible preference state under the same reentrant lock before another writer can observe it. Portable-backup state uses the same rollback rule. Normal lightweight updates retain asynchronous `apply()`. `SettingsViewModel` atomically admits one request, commits off main, delivers a request-scoped `SettingsMutationReceipt`, resets on user-data generation changes, and holds reminder mutation ownership through durable save plus synchronization follow-up. A committed reminder save with a sync failure returns a warning rather than a false retry.
- Lifecycle/conflict semantics: The editor stores a bounded saveable draft, source identity, and durability-retry obligation. Running requests do not let live/current values redefine the durable baseline. External same-field or parent-mode changes retain the local draft and show a conflict instead of overwriting it. Parent controls, compact navigation, and section changes cannot silently dispose an active child editor. A false commit remains retryable even when Android briefly published the attempted value in process memory.
- Accessibility/responsiveness: Non-primary dialogs opt out of platform decor fitting so explicit IME insets keep Save and Cancel reachable; primary full-screen editors retain their established behavior. Actions remain above the keyboard, dialog content can grow/scroll instead of using a tiny fixed window, status/error changes use live semantics, focus returns to the initiating row, and 320dp/200% text plus keyboard/Escape behavior are covered. Settings copy now distinguishes quick choices from typed values.
- Persistence/compatibility: No Room schema, migration, portable-backup format, or historical-record change. Existing Settings keys and values remain compatible; completed workouts and other history are not recomputed. The only persistence change is that explicitly saved typed values wait for durable confirmation and roll back process-local state on a false commit.
- Important files: `AppSettings.kt`, `PortableBackupManager.kt`, `SettingsViewModel.kt`, `SettingsScreens.kt`, `ProductivityEditorComponents.kt`, `SettingsPresentationPolicyTest.kt`, `SettingsResponsiveUiTest.kt`, `SettingsBehaviorUiTest.kt`, `AppSettingsPersistenceTest.kt`, and `docs/testing.md`.
- Related: `FND-20260831-013`, `FND-20260831-019`, `FND-20260901-027`, `DEC-20260901-024`.
- Verification: `VER-20260901-019`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff. Physical release remains deferred until the maximum-quality goal is complete.

### IMP-20260901-018 — Exact Gym structure, layout Undo, and recoverable History Copy

- Behavior changed: Active-workout structure edits no longer rely on loose callbacks or always-visible drag affordances. Add/create/substitute returns to the workout with one editable Set; machine create-and-assign is atomic; remove, ungroup, group, and every Set mutation bind the exact reviewed target; and layout editing uses an explicit responsive Arrange mode with one atomic Save/Cancel boundary plus exact Undo. Required Main Set removal is worded as “Mark Main Set Not Performed.” Finish, discard, grouping, menus, quick completion, and structure controls visibly disable while any conflicting Gym mutation is running.
- Domain/persistence: `WorkoutStructureBoundary`, `WorkoutPlacementBoundary`, `WorkoutSetBoundary`, arrangement snapshots, finish boundaries, and versioned receipts capture stable UUIDs and canonical structure revisions. Canonical SHA-256 structure fingerprints include session, placement, group, and Set structure while excluding mutable Set values/completion. Repository transactions implement idempotent achieved-state replay, stale rejection before writes, collision-free renumbering, tombstone/retired-placement preservation, value-preserving same-session restore, true no-op normalization, exact machine retarget guards, exact discard, and replay-safe History Copy with requested placement/Set identities.
- Lifecycle/UI/accessibility: Active-session structure and History Copy have separate namespaced persistence coordinators. History-copy authorship is strictly encoded in `SavedStateHandle` with source/target versions, requested UUIDs, and data generation; malformed or truncated payloads fail closed. Combined busy state blocks conflicting owners without sharing their errors. Arrange controls remain reachable at narrow width and 200% text, drag handles appear only in Arrange, destructive dialogs freeze the reviewed boundary through recomposition, dynamic status/errors remain with their initiating surface, and nested create flows close safely on generation reset.
- History/compatibility: Completed Set values, completion state, program classification, retired placements, tombstones, and existing workout history remain what actually occurred. No Room schema, migration, portable-backup format, or historical recomputation changed in this tranche.
- QA-driven corrections: Review rejected partial/global authorship, tombstone-blind order, full-graph fingerprints that would erase newer Set values, process-local History Copy identity, interactive controls during a separate copy request, and an obsolete machine-retarget message assertion. Each gained a narrow production or regression correction. Three independent reviewers returned unconditional `ACCEPT` only after the final combined-busy and value-preserving Undo campaigns.
- Important files: `GymModels.kt`, `GymDao.kt`, `GymRepository.kt`, `GymViewModel.kt`, `GymScreens.kt`, `GymRepositoryTest.kt`, `GymRepositoryTestSupport.kt`, `RoutineRepositoryTest.kt`, `GymPowerInputUiTest.kt`, `GymDeletionViewModelIntegrationTest.kt`, `SafetyChoiceUiTest.kt`, and `GymUxRulesTest.kt`.
- Related: `FND-20260831-019`, `FND-20260901-025`, `FND-20260901-026`, `DEC-20260901-023`, `DEC-20260901-025`.
- Verification: `VER-20260901-020`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff. Physical release remains deferred because no phone is connected and the maximum-quality whole-product goal remains active.

### IMP-20260901-019 — Exact Health mirror, Custom Units, semantic restore, and exclusive reset

- Behavior changed: Health Connect category choices now express least-privilege scope even while paused, provider availability/install paths are explicit, interrupted sync/delete/policy actions recover durably, and local-copy deletion cannot imply that provider records or Android permissions were removed. Custom Unit create/rename/archive/version flows retain drafts, expose inline validation, use accessible focused dialogs, and remain usable at 320dp/200% text. Data & Privacy is ordered Health → Backup → Reset; reset requires deliberate confirmation and its permanent-delete surface scrolls at extreme viewport/text sizes.
- Health correctness: Provider reads, policy changes, sync, reconciliation, and local deletion share one manager mutation boundary. Each exact source prefix/window is validated for stable provenance, pagination, ID/prefix collision, time-zone policy, provider offset, and deletion scope before one atomic repository transaction. Narrow-window sync preserves outside rows; null provider offsets preserve the prior zone for the same unchanged stable record; local deletion is two-phase and clears its journal only after rows and links reconcile. Startup recovery is retryable and fail-closed.
- Custom Unit correctness: Stable caller-provided IDs make create retry exact; rename/archive/version use compare-and-set semantic boundaries; dimension, factor, offset, collision, name, and symbol validation live below the UI. Creation receipts and dialog state are lifecycle-owned. Existing unbounded legacy labels continue to restore, while new authorship is bounded.
- Backup/recovery/history: Portable backups omit installation-local Health action journals and receipts; private recovery snapshots preserve them. Restore preflight proves every unit-bearing Measurement, Habit, Goal, Track, Exercise, Machine, Routine, workout, Training Max, PR, and automation fact against its actual domain contract before replacement. It rejects unknown/blank units, null-parity mismatches, invalid dimensions, non-finite/overflowing canonical values, stable-ID collisions, and Gym values inconsistent with built-in/machine semantics. Completed history is preserved and never retroactively recalculated from current program definitions.
- Reset/concurrency: `StartupRecoveryGate.runExclusiveMaintenance` closes and drains the application-wide data-access gate. Reset quiesces workers/runtime, clears portable-folder ownership, acquires Health → reminder → Room, advances durable user-data generation before deletion, cancels notifications, deletes data, rebuilds runtime, and reopens only at `Ready`. An admitted mutation finishes before reset; late access is rejected; reset removes the admitted result. The Settings caller does not hold a normal data lease, avoiding self-deadlock.
- Legacy integrity: Startup and portable-backup normalization repair the historical custom-unit Habit canonical defect only when a paired metric entry proves exact provenance, metric identity, value, unit, and conversion. Future generated writers take canonical truth from the paired metric row and repair an existing exact generated row on replay.
- QA-driven corrections: Review rejected per-row Health commits, unlocked provider operations, lossy time-zone fallback, portable leakage of local journals, generic unit compatibility, current-UI limits applied to old labels, reset guarded by only local locks, and heuristic legacy repair. Production and regressions were revised after every rejection. Domain/5/3/1, UX/accessibility, and adversarial-QA reviewers returned unconditional `ACCEPT` on the final shared tree.
- Important files: `WhipApplication.kt`, `StartupRecoveryGate.kt`, `HealthConnectManager.kt`, `BackupRepository.kt`, `RestoreRecoveryManager.kt`, `MeasurementRepository.kt`, `MeasurementDao.kt`, `MeasurementModels.kt`, `HabitDao.kt`, `HabitRepository.kt`, `LinkRepository.kt`, `CoordinatedReminderRepositories.kt`, `SettingsViewModel.kt`, `SettingsScreens.kt`, `PermanentDeleteDialog.kt`, `UnitSelectionField.kt`, `GoalScreens.kt`, `HabitScreens.kt`, and the focused Health/backup/recovery/custom-unit/settings test suites.
- Related: `FND-20260831-020`, `FND-20260901-027`, `FND-20260901-028`, `DEC-20260901-026`, `DEC-20260901-027`, `DEC-20260901-028`, `DEC-20260901-029`.
- Verification: `VER-20260901-021`.
- Status: Implemented, independently accepted, and fully verified; commit/push is the final chunk handoff. Physical release remains deferred because only the disposable emulator is connected and the maximum-quality whole-product goal remains active.

### IMP-20260901-020 — Bounded, Unicode-safe Share-to-Task capture

- Behavior changed: Text shared from another Android app still opens a prefilled Task with the first nonblank line as title and following lines as subtasks. Oversized deliverable shares show a persistent shortened-draft warning. A second share or widget Add Task can no longer replace a draft: users may replace deliberately or keep editing, then the waiting request opens after Save or confirmed Close. Widget date and Area ownership survive that handoff.
- Input/lifecycle contract: `SharedTaskCapturePolicy` enforces 8,192 raw code points, 32 KiB UTF-8, 200 title code points, 50 subtasks, and 200 code points per subtask before `LaunchRequest` can enter Activity saved state. It normalizes line endings/blank lines and stops only at complete Unicode code points. `MainActivity` accepts Android's standard `CharSequence`, owns a saveable exact-head FIFO, restores an intentionally empty queue without replay, applies capacity only to waiting shares, and preserves all other platform actions. Four shares may wait; later shares collapse into one counted marker. Compose admits Task-editor launches into a saveable text/shortening/date/resolved-Area handoff. Overflow acknowledgment is a non-dismissible saveable dialog bound to delivery ID plus current count; the Activity head is consumed only after exact acknowledgment.
- Failure boundary: Payloads larger than Android's Binder transaction limit can be rejected by the operating system before the Activity starts; Whip cannot observe or recover those. The verified policy covers large payloads Android can deliver and prevents a second failure during recreation or editor state saving.
- Compatibility: No Room schema, backup format, existing Task, Subtask, or historical record changes. Saved Tasks are not rewritten. The change only bounds new external share drafts before persistence.
- QA-driven corrections: Review rejected a 1.5 MB Binder fixture that never reached Whip, a single mutable launch slot, replay of an empty restored FIFO, share-conflict overwrite/discard, generic capacity that dropped non-share actions, transient overflow Snackbar ownership, delivery-only acknowledgment, widget Add Task draft replacement, and loss of queued widget Area. The final regressions separate Android delivery limits from app bounds, prove FIFO/collapsed overflow policy deterministically, bind acknowledgment to delivery/count, and exercise share/widget conflict, schedule, Area, recreation, Unicode, large text, and focused warning semantics.
- Important files: `SharedTaskCapturePolicy.kt`, `MainActivity.kt`, `LaunchDeliveryEffect.kt`, `LaunchQueueOverflowDialog.kt`, `PendingTaskEditorLaunchDialog.kt`, `TaskEditorRouteHost.kt`, `WhipApp.kt`, `TaskEditorDialog.kt`, `SharedTaskCapturePolicyTest.kt`, `LaunchRequestQueueTest.kt`, `LaunchQueueOverflowStateTest.kt`, `PlatformEntrySurfaceE2ETest.kt`, and `EditorDependencyUxTest.kt`.
- Related: `FND-20260831-014`, `DEC-20260901-030`.
- Verification: `VER-20260901-022`.
- Status: Implemented and verified; commit/push is the final chunk handoff. Physical release remains deferred because only the disposable emulator is connected and the maximum-quality whole-product goal remains active.

### IMP-20260902-001 — Simplified the maximum-quality development workflow

- Changed the active reusable goal from mandatory multi-agent orchestration to a conventional single-developer loop: verify, inspect, implement, test proportionately, exercise affected UI, record concise facts, commit, push, and move on.
- Removed future requirements for simulated panels, recursive audits, formal dialectics, Director approvals, and repeated specialist review. Delegation now requires an explicit user request.
- Preserved historical audit records, existing acceptance criteria, data-safety requirements, accessibility expectations, test gates, durable memory, and coherent commit/push discipline.
- Important files: `docs/product-memory/MAXIMUM_QUALITY_GOAL.md`, `USER_FEEDBACK.md`, `DECISIONS.md`, and `INDEX.md`.
- Compatibility: Process/documentation-only change; no application source, schema, backup, test, or user data changed.
- Related: `FB-20260902-001`, `DEC-20260902-001`.
- Verification: `VER-20260902-001`.
- Status: Implemented.

### IMP-20260902-002 — Durable, unit-correct, accessible Habit timers

- Added the schema-41 `habit_timer_sessions` ledger and stable typed Start/Stop/Review outcomes. Production uses Android elapsed realtime plus boot count; Start/Stop is idempotent and exact-owner bound; duplicate or stale actions cannot mutate a newer session; zero duration settles without inventing history.
- Timer Stop now commits canonical seconds, entered value in the frozen Duration unit, paired Measurement/Habit history, and terminal session state atomically. Duration tracking rejects incompatible measurement dimensions. Active timers block archive, pause, unit, and schedule changes while harmless metadata edits remain available.
- Added live monotonic elapsed display, `m:ss`/`h:mm:ss`/day formatting, spoken screen-reader duration, explicit Start / Stop & Log / Review Timer semantics, 48dp targets, an editable recovery dialog with Stop & Log / Continue / Discard, manual-duration access, and timer-first inspector status. Unresolved timers remain reachable despite schedule, Area, selection, pause, archive, or end-state filters.
- Widgets carry stable Start request and exact Stop session identities; stale cached actions fail closed. Running and review states remain visible and use the same elapsed policy as the app.
- Schema 40→41 turns legacy timestamps into ReviewRequired sessions without logging. Backup format 18 validates timer/Habit/unit/mirror coherence, converts portable timers to ReviewRequired, retains exact state only in private rollback, and drops active timers during merge while preserving recorded history.
- Important files: `HabitTimerClock.kt`, `HabitModels.kt`, `HabitEntities.kt`, `HabitDao.kt`, `HabitRepository.kt`, `WhipDatabase.kt`, schema 41, `BackupRepository.kt`, `HabitViewModel.kt`, `HabitScreens.kt`, and Habit widget paths.
- Related: `FND-20260902-001`, `DEC-20260902-002`.
- Verification: `VER-20260902-002`.
- Status: Implemented and verified; commit/push is the chunk handoff.

### IMP-20260902-003 — Progressive Habit reminder and schedule configuration

- Simplified ordinary Habit creation without removing capability: required cadence and its dependent fields remain inline, while default reminders, weekday-specific overrides, ending rules, and first-day-of-week configuration now live behind one clearly labeled in-place disclosure.
- Added an always-visible reminder summary using the device's time format. Configured ending rules and a week boundary that differs from the app default remain visible even while the controls are collapsed, so progressive disclosure does not hide consequential state.
- Existing Habits with reminders, weekday overrides, ending rules, or a non-default week boundary open the section automatically. Power Mode also opens it. A rejected Save automatically reveals the section when an ending-rule error would otherwise be hidden.
- Kept Additional Details independent from schedule complexity; reminder/end settings no longer force tags, quick actions, and notes open. Both disclosure states survive saveable state restoration.
- Added Compose regressions for the basic collapsed path, intentional reveal, configured-data auto-expansion, hidden-error recovery, state restoration, and 200% text reachability.
- Compatibility: UI/state-only change. Habit domain data, Room schema 41, backup format 18, existing reminders, historical check-ins, and scheduling semantics are unchanged.
- Important files: `HabitScreens.kt`, `EntitySaveCoordinatorUiTest.kt`, `AdaptiveWhipScreenTest.kt`, and `docs/testing.md`.
- Related: `FND-20260831-012`, `DEC-20260831-013`.
- Verification: `VER-20260902-003`.
- Status: Implemented and fully verified; commit/push is the chunk handoff. Physical release remains deferred while the whole-product goal continues.

### IMP-20260902-004 — Truthful Habit availability and Today resolution

- Paused and off-schedule cards now use explicit availability summaries and suppress ordinary one-tap check-in, numeric, checklist, flexible-schedule, and target controls. Expanded cards explain how to resume/edit a pause or deliberately log outside the schedule. Active timer Stop/Review remains reachable even if a legacy or unusual state is otherwise unavailable.
- Scheduled-pause inspectors no longer offer a misleading primary check-in. Off-schedule inspectors use mode-specific “Outside Schedule” actions so exceptions are intentional and understandable. Timer start time now follows Whip's configured `LocalWhipZone`.
- Today and Home now classify completed and skipped Habits as “Finished for Today,” collapse them behind a review/undo disclosure, and exclude them from the action-needed Home count. Skip remains neutral and historically distinct from completion; undo continues to restore it to the attention queue.
- Added deterministic classification/status tests, focused Compose card and inspector tests, configured-zone coverage, and updated the full skip/history/insights/undo journey to require correct finished placement.
- Compatibility: UI and derived-presentation logic only. No persistence, schema, migration, backup, scheduling, timer-ledger, check-in, pause, skip, or historical-data format changed.
- Important files: `HabitScreens.kt`, `WhipApp.kt`, `CompactCollectionStatusTest.kt`, `ProductivityCardDesignUiTest.kt`, `ActivityHistoryUiTest.kt`, and `HabitSkipJourneyE2ETest.kt`.
- Related: `FND-20260902-002`, `DEC-20260902-003`.
- Verification: `VER-20260902-004`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-005 — Effective-date Habit History and transparent pause impact

- Replaced write-time-only History assembly with one deterministic effective-date projection for manual/synced check-ins, skipped days, and started scheduled pauses. Same-day entries retain stable secondary ordering; future pauses remain under Options. Pause rows open the existing exact-owner editor directly.
- Renamed the inspector group to “Habit History” and generalized pagination copy from earlier check-ins to earlier events. Pause rows explain that check-ins, misses, and reminders were excluded.
- Insights now treats a started pause as neutral activity, renders its paused grid state, and says “No scored periods” when the recent window contains no completion or miss instead of showing a misleading 0% failure.
- Pause creation/editing warns when the selected range includes today/past and identifies derived streak/consistency recalculation. Delete confirmation states that completed check-ins and skips remain while unlogged dates may become missed. Permanent Habit deletion now counts scheduled-pause records.
- Added one deterministic JVM ordering/filter regression and one pause-only Insights Compose regression; expanded the existing editor/history test for visible pause history, exact edit routing, impact warning, delete consequences, failure-draft retention, and historical skip undo.
- Compatibility: Presentation and confirmation copy only. No repository, calculation, Room schema, migration, backup, check-in, skip, pause, or historical data format changed.
- Important files: `HabitScreens.kt`, `ActivityPresentationTest.kt`, `ActivityHistoryUiTest.kt`, and `docs/testing.md`.
- Related: `FND-20260902-003`, `DEC-20260902-004`.
- Verification: `VER-20260902-005`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-006 — Named adaptive navigation and returning-Home recovery

- Replaced the font-scale icon-only switch with measured label fitting. Compact/tabletop navigation renders all six names in one row when they fit and otherwise uses a stable Home/Tasks/Habits then Goals/Tracks/Gym two-row layout with unchanged direct tab semantics and minimum touch targets.
- Made the persistent rail label-aware: its width follows the widest rendered destination, every item remains named at 150–320% text, item height grows with text, and short/landscape rails scroll directly to Home through Settings instead of stripping labels.
- Added a bounded “Pick Up Where You Left Off” section when settled Home is clear for a returning user. It recognizes Inbox, Upcoming/planning, saved/archived Habits and Goals, unpinned/archived Tracks, and Gym workouts/routines/exercises/machines, displays at most three count-aware routes, and opens the exact relevant destination. Existing Review & Trends recovery remains primary when completion evidence exists.
- Added one JVM policy regression and four Compose regressions covering bounded priority, exact Inbox routing, 150/200/320% compact and rail labels, label containment, stable one/two-row geometry, and scroll reachability in a 360dp-high rail. Expanded resource-policy coverage for all new copy.
- Compatibility: Presentation and in-memory navigation state only. No persistence, migration, backup, entity semantics, history, deep-link, or keyboard-shortcut changes.
- Important files: `WhipApp.kt`, `strings.xml`, `WhipNavigationPolicyTest.kt`, `AdaptiveWhipScreenTest.kt`, `AuditUiStringResourcePolicyTest.kt`, and `docs/testing.md`.
- Related: `FND-20260831-011`, `FND-20260831-017`, `FND-20260902-004`, `DEC-20260902-005`.
- Verification: `VER-20260902-006`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-007 — Explicit Track/Entry search ownership and archive-correct routing

- Centralized workspace search accessibility labels on `WhipSearchEntryContext`: Tracks now announces “Search Tracks & Entries,” Tasks announces “Search Tasks & Steps,” and Gym destinations announce the exact scope they open.
- Removed the unreachable Track-list query, no-match, query/reorder, and clear-query branches. Track list selection and reordering now operate only on the visible active or archived source; Activity and per-Track Entry search remain unchanged.
- Made Track result routing projection-owned: the request waits until the selected Area projection is present, then opens Tracks or Archived from the record's actual state instead of forcing every result into active Tracks.
- Made unified-search empty guidance follow the live scope. The inspected Tracks flow now presents a matching action, “Search tracks & entries” placeholder, “Scope · Tracks & Entries” summary, and “within Tracks & Entries” guidance.
- Added one JVM policy regression and one Compose ownership/routing regression; expanded the production-repository global-search journey to create, archive, find, and open a real Track in the selected Archived destination.
- Compatibility: UI/policy and in-memory navigation only. No repository, search-index, Room schema, migration, backup, Track/Entry identity, Area, history, or reorder persistence change.
- Important files: `WhipNavigationPolicy.kt`, `WhipApp.kt`, `TrackScreens.kt`, `UnifiedSearchDialog.kt`, `strings.xml`, `WhipNavigationPolicyTest.kt`, `TrackWorkspaceUiTest.kt`, `GlobalSearchRoutingTest.kt`, and `docs/testing.md`.
- Related: `FND-20260831-018`, `FND-20260902-005`, `DEC-20260902-006`.
- Verification: `VER-20260902-007`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-008 — Cross-device search visibility and accessibility-matrix hardening

- Reordered emoji search mode around the user's immediate intent: matches appear directly after the search field, redundant headings disappear, search-only spacing tightens, matching saved emoji are brought into view, and custom creation remains available below. Saved-emoji management moves to the dialog footer during search, and choosing/closing explicitly clears focus and hides the software keyboard.
- Gave Unified Search results an owned list state and return-to-summary behavior whenever filter disclosure changes, so active filter summaries cannot remain hidden behind a stale deep scroll position.
- Made affected end-to-end tests viewport-aware without relaxing product assertions: tests scroll the owning Home, workout, Task, or routine surface; derive compact tab edges from the physical root; wait for exact editor/removal semantics; distinguish compact Settings navigation from a persistent wide sidebar; and scope intentional wide master/detail duplicate identities to their semantic owner.
- Added final API 26 emoji screenshot/UI-hierarchy evidence and API 37 actual-TalkBack keyboard-navigation evidence to the product audit artifact tree.
- Compatibility: UI ordering, focus/keyboard behavior, and test targeting only. No persistence, Room schema, migration, backup, search index, emoji identity format, Gym semantics, or historical record changed.
- Important files: `IdentityEmojiPicker.kt`, `UnifiedSearchDialog.kt`, `InteractionControlUiTest.kt`, `CoreFeatureJourneyE2ETest.kt`, `EditorStateRecreationTest.kt`, `WhipNavigationTest.kt`, `WhipComposeSemanticsTest.kt`, `RoutineBuilderUiTest.kt`, `TrackWorkspaceUiTest.kt`, `GlobalSearchRoutingTest.kt`, and `artifacts/full-product-audit/2026-09-02/platform-matrix/`.
- Related: `FND-20260902-006`, `DEC-20260902-007`.
- Verification: `VER-20260902-008`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-009 — Exact and archive-correct Area management

- Added a typed `AreaMutationReceipt` boundary covering create, rename, color, reorder, move-all, merge, archive, restore, delete-while-moving, and delete-with-items. `SettingsViewModel` serializes these requests, retains their exact terminal result through Activity recreation, reads destructive truth from repositories, and classifies post-commit Settings reconciliation as a warning rather than a retryable failure.
- Area-management dialogs now stay owned by the initiating request, retain reviewed fields and destination choices on failure, expose in-context saving/errors, disable dismissal and duplicate submission only while persistence is active, and close the matching child surface only after authoritative success. Archive success stays in the Area detail, offers Undo, and uses an exact Restore request.
- Corrected archived-state behavior throughout the flow: archived details execute Restore; archived search matches appear without prior disclosure; archived-name creation says Restore and reactivates the same Area while preserving its saved color; active duplicates still select existing; rename conflicts explain that an archived destination must be restored before merge.
- Wrapped Area color read-modify-write in the existing Room transaction. No generic mutation DSL or schema expansion was introduced.
- Added four Area-management Compose regressions, three exact ViewModel mutation regressions, and two repository archived-identity/conflict regressions. Preserved inspected manager, detail, archive-confirmation, and archive-result screenshots plus UI hierarchies.
- Compatibility: No Room schema, migration, backup format, Area ID, cross-domain assignment, completed history, or saved color was changed. Existing archived identities are restored rather than duplicated.
- Important files: `AreaManagementDialog.kt`, `AreaPicker.kt`, `WhipColorPicker.kt`, `SettingsViewModel.kt`, `AreaRepository.kt`, `AreaFeatureUiTest.kt`, `AreaMutationViewModelTest.kt`, `MeasurementTaxonomyRepositoryTest.kt`, `docs/testing.md`, and `artifacts/full-product-audit/2026-09-02/area-management/`.
- Related: `FND-20260831-019`, `FND-20260901-027`, `FND-20260902-007`, `DEC-20260902-008`.
- Verification: `VER-20260902-009`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-010 — Explicit, cross-domain, archive-correct Tag management

- Added a dedicated full-screen Tag manager with active/archived search, usage counts across Tasks/Habits/Goals/Tracks, explicit Create/Rename/Merge/Archive/Restore flows, in-context saving/error states, archive Undo, and narrow/large-text responsive controls.
- Added typed request-owned Tag mutation receipts. Drafts and destination choices remain in the initiating dialog on failure; every mutating row/menu action, duplicate submission, and dismissal is disabled only while the exact write is active.
- Split Rename from Merge in the repository. Both are one Room transaction and now update Track tags as well as Task, Habit, and Goal tags. Merge requires an active destination and removes only the source after every replacement succeeds.
- Made archive state stable: ordinary taxonomy reconciliation reuses an archived identity without restoring it, while explicit Create/Restore can reactivate the same ID. Item references and searchability remain intact. Active-only Tag suggestions no longer advertise archived labels in Task editing and bulk editing.
- Rejected comma-containing Tag names at both UI and domain boundaries because comma is the current denormalized persistence separator. Deduplication during replacement now uses locale-stable normalization.
- Added five repository regressions, four request/usage ViewModel regressions, and seven Compose interaction/accessibility regressions. Preserved final manager, merge, and archive evidence under `artifacts/full-product-audit/2026-09-02/tag-management/`.
- Compatibility: No Room schema, migration, backup format, Tag ID, entity ID, completion/progress fact, Gym record, or measurement history changed. Archive never rewrites item data; Rename/Merge intentionally updates the selected taxonomy label across saved item references.
- Important files: `MeasurementDao.kt`, `MeasurementRepository.kt`, `SettingsViewModel.kt`, `SettingsScreens.kt`, `TagManagementDialog.kt`, `TaskEditorRouteHost.kt`, `WhipApp.kt`, `MeasurementTaxonomyRepositoryTest.kt`, `TagMutationViewModelTest.kt`, `TagManagementUiTest.kt`, and `docs/testing.md`.
- Related: `FND-20260831-019`, `FND-20260901-027`, `FND-20260902-008`, `DEC-20260902-009`.
- Verification: `VER-20260902-010`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-011 — Exact, outcome-owned completed-Workout deletion

- Added transaction-derived `WorkoutDeletionImpact` and revision-checked deletion summaries. The reviewed snapshot includes session identity/lifecycle, placements, groups, all Sets, completed Sets, sourced personal records, preserved Training Max decisions, linked Goal contributions, automation occurrences, and affected Exercise identities.
- Active workouts are rejected. A changed session or history graph invalidates confirmation without mutation. The commit removes only the selected Workout graph; post-commit personal-record rebuilding and the retired-Link compatibility check use warnings/cancellation receipts so the UI never invites a second destructive attempt after the first commit. Preserved Goal contributions, generated Habit check-ins, and automation occurrences remain immutable audit evidence and are disclosed before confirmation.
- Added a request-owned ViewModel flow with exact target UUID validation, retained preview/error/missing state, rest-timer follow-up ownership, data-generation invalidation, and process-restored absent-target reconciliation.
- Replaced the optimistic generic confirmation with a scrollable, accessible review that distinctly names Removed, Recalculated, and Kept data; discloses immutable 5/3/1 Training Max audit history; blocks active sessions; disables stale confirmation; and offers read-only re-review. Extracted the three Gym deletion review hosts to keep the main Compose method instrumentable.
- Added four coordinator regressions, two real-application ViewModel lifecycle regressions, and three Compose interaction/accessibility regressions. A final-source 1080×2400 walkthrough created an Exercise and Workout, finished it, opened History, reviewed the exact impact, committed once, and verified the empty History/success outcome. Artifacts are under `artifacts/full-product-audit/2026-09-02/workout-deletion/`.
- Compatibility: No Room schema, migration, backup format, Exercise/Routine definition, unrelated completed Workout, or historical Training Max decision changed. The only irreversible data removal is the selected completed Workout and its owned placements/groups/Sets.
- Important files: `DomainDeletionCoordinator.kt`, `GymViewModel.kt`, `GymScreens.kt`, `DomainDeletionCoordinatorTest.kt`, `GymDeletionViewModelIntegrationTest.kt`, `WorkoutDeletionUiTest.kt`, `docs/testing.md`, and the final visual artifacts.
- Related: `FND-20260831-019`, `FND-20260901-025`, `FND-20260902-009`, `DEC-20260902-010`.
- Verification: `VER-20260902-011`.
- Status: Implemented and fully verified; physical release remains deferred while the whole-product goal continues.

### IMP-20260902-012 — Exact, recoverable Machine-profile deletion

- Added `GymDeletionKind.Machine` to the shared request-owned deletion lifecycle, with a saveable candidate ID/UUID/data generation, explicit preparing/ready/error/missing states, transaction-derived impact/revision, exact commit admission, and one consumable terminal receipt.
- Added process-restored recovery against fresh repository truth. An absent exact target settles as achieved exactly once; a present or unverified target stays actionable and never masquerades as success. UUID mismatch is rejected before preview or commit.
- Reworked the Machine confirmation into a scrollable responsive review with explicit impact, error, missing-target, retry, and fixed-footer actions. At 320dp and 200% text the content scrolls while Cancel/Delete remain reachable.
- Preserved existing domain behavior: active-workout use blocks deletion, affected routines are marked `Needs equipment`, and completed-workout Machine snapshots remain immutable historical evidence.
- Added three real-application ViewModel regressions, one new 320dp/200%-text Compose recovery regression, and strengthened the existing destructive Machine UI test. Final visual evidence is retained under `artifacts/full-product-audit/2026-09-02/machine-deletion/`.
- Compatibility: No Room schema, migration, backup format, Machine identifier outside the reviewed target, Routine identity, completed Set, or historical workout snapshot changed. No physical-device release occurred.
- Important files: `GymViewModel.kt`, `GymScreens.kt`, `GymDeletionViewModelIntegrationTest.kt`, `GymPowerInputUiTest.kt`, `docs/testing.md`, and the final visual artifacts.
- Related: `FND-20260902-010`, `DEC-20260902-011`.
- Verification: `VER-20260902-012`.
- Status: Implemented and fully verified; physical release remains deferred at user-directed mission closeout.

### IMP-20260902-013 — VERA-Codex global and Whip routing installation

- Installed the five provided Luna/Terra/Sol role definitions, bounded routing policy, Terra/medium primary default, Luna default subagent, three-agent cap, and interrupt behavior in the Windows Codex home at `/mnt/c/Users/commv/.codex`.
- Merged VERA guidance into the existing global `AGENTS.md` while preserving the ATIS-specific policy and every unrelated global configuration section.
- Added the matching repository-owned `.codex/config.toml`, five `.codex/agents/*.toml` definitions, `routing-policy.yaml`, and a concise Whip policy section. The Whip section explicitly preserves `FB-20260902-001`: no agents or recursive review unless the user requests them.
- Compatibility: Configuration/instructions only. No Whip application code, schema, data, build output, device state, or release artifact changed.
- Related: `FB-20260902-002`, `DEC-20260902-012`.
- Verification: `VER-20260902-013`.
- Status: Superseded by the canonical clean-slate installation in `IMP-20260902-014`.

### IMP-20260902-014 — Canonical VERA repository and clean orchestration installation

- Created `/root/repos/vera-codex` directly from the supplied archive, normalized only filesystem permissions, confirmed all nine tracked file contents match the archive, committed it as `2c763f0`, created private GitHub repository `commvnist/vera-codex`, pushed `main`, and verified the remote ref.
- Replaced global and Whip orchestration instructions with the canonical VERA `AGENTS.md`; restored the canonical multiline `routing-policy.yaml`; retained the archive-exact project `.codex/config.toml` and five archive-exact custom role files.
- Removed global `atis-fast-explorer.toml`, the ATIS instruction section, the Whip-specific no-unrequested-subagents adaptation, and the previous condensed VERA wording. The global VERA values remain embedded alongside unrelated existing machine-local integrations, which were deliberately preserved.
- Compatibility: Configuration/instructions only. No Whip application code, schema, user data, tests, release artifacts, or device state changed.
- Related: `FB-20260902-004`, `DEC-20260902-013`.
- Verification: `VER-20260902-015`.
- Status: Implemented and verified.

### IMP-20260902-015 — Prepare Whip 0.3.35 for physical-device release

- Advanced the Android release identity from `0.3.34`/40 to `0.3.35`/41 so the current source is distinguishable from the previously installed physical release.
- Kept application ID `commvne.com.whip.app`, release signing configuration, Room schema, backup format, and user-data semantics unchanged.
- Built the signed release APK/AAB after the complete deterministic gate, paired and connected the exact requested physical phone, and upgraded it in place with `adb install -r`. The app was cold-launched without clearing data or running physical-device instrumentation.
- Important file: `app/build.gradle.kts`.
- Related: `FB-20260902-005`, `DEC-20260902-014`.
- Verification: `VER-20260902-016`.
- Status: Released and verified on the physical phone.

### IMP-20260902-016 — First-class advanced 5/3/1 program expansion

- Added two one-tap, editable 11-phase structures: BBB Leaders → FSL Anchor and FSL Leaders → FSL Anchor. Both support the user's own ordered Weight + Reps lifts, use 5s PRO during two Leader cycles, insert a 7th Week transition, use Classic PR-set Main work plus FSL in the Anchor, and expose Training Max boundaries after Leader 1 and both 7th Week phases.
- Added one-tap Deload, Training Max Test, and PR Test prescriptions with every percentage and rep range visible before build and independently applicable to an existing program phase. Public sources do not expose every book prescription, so the UI truthfully calls these book-guided editable structures and asks the lifter to verify the edition/template they follow.
- Added a dedicated Supplemental placement for alternate-lift BBB. The selected alternate uses its own Training Max and follows Main in the workout; inactive Anchor/test placements are omitted instead of producing empty exercise cards. Cycle changes synchronize every programmed Main/Supplemental placement for the lift while eligibility remains Main-only.
- Expanded Jokers from one candidate to an ordered one-to-three-set ladder at +5% or +10% TM steps. Each next Joker is offered only after successful Main/previous-Joker targets; a skip, failure, under-target result, RPE 9+, or RIR 1 or lower ends the ladder. Jokers remain Optional, individually logged, and additive before Supplemental work. Legacy arbitrary single-Joker routines remain valid.
- Added transparent automatic Push/Pull/Single-leg-Core assistance drafts from compatible active rep-based Library exercises: 3×10 per category for standard plans and 5×10 for Beginners. Suggestions are deterministic, never create exercises silently, never silently demote an unselected canonical main lift, and remain visible, replaceable, or omittable before save.
- Reworked setup into numbered, scrollable, full-width sections for preset, schedule/lifts, programming, optional work, assistance, and review. Added a phase timeline, policy explanations, explicit optional-state language, Supplemental labels during workouts, 48dp targets, and verified navigation at 320dp/200% text.
- Closed the final acceptance gaps: repeated Squat/Bench days in the Beginners layout keep synchronized editable protocol templates while a deterministic balanced runtime owner assigns Squat to Monday, Deadlift + Press to Wednesday, and Bench to Friday. Each logical lift therefore executes one complete Deload/TM Test/PR Test without invalid saves, TM-edit drift, duplicate tests, or an empty training day. Program Structure recognizes and names alternate-lift BBB, preserves it through Main/Joker edits, and removes it only when the lifter deliberately chooses another Supplemental scheme. Existing-program protocol controls now show their full matrix before the one-tap change, and assistance copy uses human-readable category names.
- Compatibility: No Room schema, migration, or backup-format change. Existing routines are not regenerated; completed workouts retain their saved prescriptions and outcomes. Once-per-lift runtime protocol ownership requires both a phase-specific role and 5/3/1 template revision 2, while legacy repeated exposures execute unchanged. Explicitly applying a new protocol upgrades durable routine provenance but marks only the selected phase for once-per-lift ownership. New advanced programs author their protocol phases directly with that phase-level provenance.
- Important files: `FiveThreeOneProgramming.kt`, `RoutineBuilder.kt`, `GymScreens.kt`, `RoutineRepository.kt`, `GymRepository.kt`, `GymModels.kt`, `RoutineBuilderStateTest.kt`, `GymUxRulesTest.kt`, `RoutineRepositoryTest.kt`, and `RoutineBuilderUiTest.kt`.
- Related: `FB-20260902-006`, `DEC-20260902-015`.
- Verification: `VER-20260902-017`.
- Status: Implemented and fully verified after final-review remediation; not yet released to a physical phone.

### IMP-20260902-017 — Targeted subsystem QA runner

- Added `scripts/qa-targeted` as the normal development-loop entry point. Named profiles cover the app shell, Tasks, Habits, Goals, Tracks, Gym, 5/3/1, and Settings; each runs its bounded JVM suites and compiles Android tests.
- Added exact repeatable `--jvm` patterns and `--android Class#method` selectors. Android execution retains the emulator-only guard and refuses physical devices; `--repeat` supports isolated timing-sensitive verification.
- Kept `scripts/check --emulator` and `scripts/check --full` unchanged as the complete acceptance/release authorities. Targeted results are explicitly described as chunk evidence, not a whole-product release claim.
- Related: `FB-20260902-009`, `docs/quality/UI_UX_REMEDIATION_PLAN.md`.
- Verification: `VER-20260902-018`.
- Status: Implemented and targeted-tool verified.

### IMP-20260902-018 — Exact-signature instrumentation batch resume

- Extended the complete emulator gate with a local successful-batch cache keyed by production source, build configuration, the emulator image, shared Android-test support, the exact requested class set, and the source files for those classes.
- A production-source change invalidates every device batch. A correction confined to one test batch reruns that batch while unchanged successful batches retain their exact executed-class, test-count, and zero-skip evidence.
- Added `--fresh-emulator` as an explicit cache bypass. JVM tests, Android-test compilation, lint, coverage, artifact checks, complete source-test accounting, and zero-skip enforcement always run and cannot be satisfied by the cache.
- Related: `FB-20260902-009`, `docs/quality/UI_UX_REMEDIATION_PLAN.md`.
- Verification: `VER-20260902-019`.
- Status: Implemented and structurally verified; its first frozen-candidate emulator run remains the integration gate.

### IMP-20260902-019 — Consistent adaptive editor chrome

- Added `WhipEditorHeader`, a shared full-width editor header with a 48dp identity row, heading semantics, standard divider/spacing, and automatic action stacking for narrow or enlarged-text layouts.
- Adopted it in shared Productivity editors, Task, Track, Track Entry, and Routine Builder. Routine outline now has one unambiguous X exit; nested builder pages use Back to the outline.
- Standardized primary Gym editor Save actions as filled buttons and their dismiss actions as accessible 48dp icon targets for Machine, Exercise, tracked-record, and Set editing.
- Added source architecture contracts for adoption, primary action hierarchy, single-section inspector suppression, and shared inspector navigation; added a focused 320dp/200%-text Compose regression.
- Related: `FND-20260902-011`, `DEC-20260902-016`.
- Verification: `VER-20260902-020`.
- Status: Implemented and targeted-test verified.

### IMP-20260902-020 — Canonical schema-42 data epoch and clean-slate reset

- Raised Whip to `0.3.36`/version code 42 and made Room schema 42 the sole exported schema. Removed migrations, historical schema exports, migration-only sources/tests, compatibility settings initialization, old entity-tag storage, and ignored pre-epoch backup-upgrade tests.
- Reduced portable backups to exact envelope 3, data epoch 2, data version 19, and the canonical current table set. Older, newer, malformed, or differently shaped backups fail before mutation; current replace/merge/private rollback safety remains.
- Added a pre-Room `DataEpochGate` with durable Current/ResetRequired/ResetInProgress states and a repository-independent `LocalDataResetter`. Reset cancels work and notifications, releases Whip's portable-folder grant, deletes Whip database/files/preferences, preserves non-Whip preferences and external documents, rebuilds schema/defaults, creates a fresh generation, and locks widgets until success.
- Added a dedicated accessible fresh-start flow with two explicit destructive actions, safe close, non-interactive progress, distinct pre-confirmation check failure, and confirmed-reset retry. Serialized reset retries, discarded all epoch-gated launch requests, and kept workers/widgets behind the Ready gate.
- Isolated the destructive integration test as the last standalone instrumentation batch so its intended runtime teardown cannot contaminate neighboring tests.
- Important files: `DataEpochGate.kt`, `LocalDataResetter.kt`, `WhipApplication.kt`, `MainActivity.kt`, `StartupRecoveryScreen.kt`, `WhipDatabase.kt`, `BackupRepository.kt`, `scripts/check`, schema 42, and focused JVM/Android regressions.
- Related: `FND-20260902-012`, `DEC-20260902-017`.
- Verification: `VER-20260902-021`.
- Status: Implemented and targeted/emulator verified; not released to a physical phone.

### IMP-20260902-021 — Explicit interface labels and shared section hierarchy

- Added deliberate labels for theme/Home/Review/Health choices, Task priorities, Goal states/consistency periods, measurement dimensions, Habit destinations, workout state/group types, 1RM formulas, and Gym destinations.
- Replaced visible enum-name rendering in onboarding, Review, Settings, Task filters/editors, Goal flows/search, Habit navigation, Gym navigation/history/grouping, and unified search. An active workout now reads “In progress”; Habit “All” now reads “All Habits”.
- Replaced hand-built Settings and major Gym section headings with `EditorSectionHeader`; changed Task/Habit/Settings weekday labels to locale-aware display names.
- Added architecture regressions that reject direct internal-name rendering for these audited surfaces and require the shared heading contract.
- Related: `FND-20260902-013`, `DEC-20260902-018`.
- Verification: `VER-20260902-022`.
- Status: Implemented and targeted-test verified; not released to a physical phone.

### IMP-20260902-022 — One-pass bounded historical search selection

- Replaced full-history sorting with a stable bounded priority queue. Search now evaluates each timestamp/rank selector once and retains only the requested newest 100/500 values before final ordering.
- Preserved independent per-domain limits and incomplete-source disclosure so a large Task, Habit, Track, or Gym history cannot consume another domain's result budget.
- Added deterministic 10,000-value and dual-domain 20,000-result regressions and reconciled the prior section/internal-label/search audit statuses.
- Important files: `UnifiedSearchDialog.kt`, `UnifiedSearchRulesTest.kt`, and `TOP_DOWN_UX_UI_FUNCTIONAL_QA_AUDIT_2026-08-27.md`.
- Related: `FND-20260902-014`, `DEC-20260902-019`.
- Verification: `VER-20260902-023`.
- Status: Implemented and targeted-test verified.

### IMP-20260902-023 — Intent-specific Gym control semantics

- Added field-qualified semantics to every shared `SelectionField` menu option so identical values in different controls remain distinguishable.
- Added lift-qualified descriptions and stable tags to repeated 5/3/1 Training Max entry/calculation controls.
- Added a named active-workout empty-state region and scoped the workout-only exercise regression to it.
- Removed high-risk positional action selectors from the Routine Builder workflows and synchronized their navigation assertions with the shared editor chrome.
- Important files: `ItemControlPatterns.kt`, `RoutineBuilder.kt`, `GymScreens.kt`, `RoutineBuilderUiTest.kt`, and `FirstClassWorkflowE2ETest.kt`.
- Related: `FND-20260902-015`, `DEC-20260902-020`.
- Verification: `VER-20260902-024`.
- Status: Implemented and complete-candidate verified in `VER-20260902-031`.

### IMP-20260902-024 — Request-owned Category save and exact Track deletion

- Added a dedicated Gym catalog request state. Category create/edit now preserves its name/type draft through Activity recreation, blocks duplicate submission and dismissal while saving, shows failures inline, and closes only for its own successful receipt.
- Added transaction-derived Track deletion previews covering the Track definition, Fields, Choice options, Entries, values, Link rules and children, and automation rules and children. Commit rejects a stale revision and removes only the reviewed graph.
- Added a Track deletion request state and responsive review surface with explicit counts, integration consequences, progress, retry, missing-target, and commit-failure states. A committed deletion with interrupted derived reconciliation reports success with a warning instead of inviting destructive replay.
- Added focused recreation, exact-impact, stale-definition, stale-automation, and end-to-end deletion regressions.
- Important files: `DomainDeletionCoordinator.kt`, `TrackViewModel.kt`, `TrackScreens.kt`, `GymViewModel.kt`, `GymScreens.kt`, `DomainDeletionCoordinatorTest.kt`, `TrackDeletionUiTest.kt`, and `EditorStateRecreationTest.kt`.
- Related: `FND-20260831-019`, `FND-20260901-027`, `FND-20260902-016`, `DEC-20260902-021`.
- Verification: `VER-20260902-025`.
- Status: Implemented and complete-candidate verified in `VER-20260902-031`.

### IMP-20260902-025 — Request-owned Gym catalog editors and contextual Routine Back

- Generalized the Gym catalog receipt into typed Category, Machine, Machine-version, Exercise, and Exercise-for-Machine outcomes behind one serialized request runner.
- Replaced composition-owned Machine/Exercise completion callbacks with namespaced coordinators that retain the draft, expose inline failure, prevent duplicate submission/dismissal during the owned request, and close only after the matching success result.
- Extracted Machine/Exercise catalog overlays from the broad Gym shell, reducing its bytecode enough for JaCoCo instrumentation while keeping dialog behavior cohesive.
- Changed the Routine Builder header to expose Back whenever a placement detail is selected, preserving routine-level Save without presenting a destructive editor-close action as the way out of a child pane.
- Added successful-save closure assertions to the existing Exercise/Machine Activity-recreation tests.
- Important files: `GymCatalogMutationUi.kt`, `GymViewModel.kt`, `GymScreens.kt`, `RoutineBuilder.kt`, and `EditorStateRecreationTest.kt`.
- Related: `FND-20260902-017`, `DEC-20260902-022`.
- Verification: `VER-20260902-026`.
- Status: Implemented and complete-candidate verified in `VER-20260902-031`.

### IMP-20260902-026 — Canonical 5/3/1 identity and structural work model

- Removed obsolete 5/3/1 program-kind aliases and the unknown-template key; current programs use canonical `FiveThreeOne` plus explicit phase, Main-work, supplemental, optional-work, and progression policies.
- Removed persisted routine/workout assistance-role columns and their inference paths. Routine and historical workout records now use `placementKind` plus `assistanceCategory`; the familiar Push/Pull/Single-leg or Core/Other picker remains transient builder state.
- Tightened program validation so an applied Training Max has an explicit source and every one-to-three Joker ladder uses ordered 5-point percentage steps without a compatibility exception.
- Established Room schema 43, data epoch 3, backup data version 20, and app 0.3.37/version code 43. Removed schema 42 and updated current-boundary and backup contracts; older installations/backups require the already-authored clean-start path.
- Updated the current 5/3/1 support matrix to reflect one-tap Leader/Anchor plans, all three 7th Week presets, alternate-lift BBB, multi-Jokers, automatic assistance drafts, and Training Max decision history.
- Important files: `GymModels.kt`, `RoutineEntities.kt`, `GymEntities.kt`, `RoutineRepository.kt`, `FiveThreeOneBuilder.kt`, `FiveThreeOneProgramming.kt`, `RoutineBuilder.kt`, `GymScreens.kt`, `WhipDatabase.kt`, `DataEpochGate.kt`, `BackupRepository.kt`, and schema 43.
- Related: `FND-20260902-018`, `DEC-20260902-023`.
- Verification: `VER-20260902-027`.
- Status: Implemented and complete-candidate verified in `VER-20260902-031`.

### IMP-20260902-027 — Current-only Settings and error-directed Custom Unit validation

- Removed Settings-construction/save cleanup for obsolete preference keys and removed the old Health-enabled-to-all-categories inference. Current explicit values continue to persist normally; unknown keys have no effect.
- Added Name and conversion-factor bring-into-view targets to Custom Unit create/rename/version. Invalid submit now focuses and scrolls to the first failing field instead of leaving supporting text outside the viewport.
- Corrected the Settings responsive-suite package in `scripts/qa-targeted`, expanded the profile with non-destructive portable-backup/recovery/data-epoch coverage, and documented that destructive reset runs in its own exact batch.
- Reconciled the fast UI/UX plan so every confirmed gap is implemented in its family wave and recorded the current completion/final-gate state.
- Important files: `AppSettings.kt`, `SettingsScreens.kt`, `AppSettingsPersistenceTest.kt`, `SettingsResponsiveUiTest.kt`, `scripts/qa-targeted`, and `UI_UX_REMEDIATION_PLAN.md`.
- Related: `FND-20260902-019`, `FND-20260902-020`, `DEC-20260902-024`.
- Verification: `VER-20260902-028`.
- Status: Implemented and complete-candidate verified in `VER-20260902-031`.

### IMP-20260902-028 — Frozen-gate localization, recovery-state, and evidence reconciliation

- Replaced non-observable process-locale reads in Habit/Settings weekday composables with the current observable Android configuration locale.
- Made restore, reset, pending-recovery block, and recovery retry publish the underlying gate's terminal state to `WhipApplication.startupRecoveryState` before the operation boundary completes.
- Updated the 1,485-test inventory, schema-43 cause/effect evidence references, and canonical 5/3/1 history/status expectations discovered by the complete gate.
- Important files: `HabitScreens.kt`, `SettingsScreens.kt`, `WhipApplication.kt`, `GymPowerInputUiTest.kt`, `DataEpochBoundaryTest.kt`, `testing.md`, `e2e-coverage.tsv`, and `QA_CAUSE_EFFECT_MATRIX_2026-08-27.tsv`.
- Related: `FND-20260902-021`, `FND-20260902-022`, `DEC-20260902-025`, `DEC-20260902-026`.
- Verification: `VER-20260902-029`.
- Status: Implemented and complete-candidate verified in `VER-20260902-031`.

### IMP-20260902-029 — Stable Habit destination identity

- Assigned explicit enum-name tag values to Habit destination bars in loading and normal states while retaining the clearer explicit display labels.
- Restored the app-wide navigation contract without reverting “All Habits” or coupling future copy changes to automation identifiers.
- Important files: `HabitScreens.kt` and `WhipNavigationTest.kt`.
- Related: `FND-20260902-023`, `DEC-20260902-027`.
- Verification: `VER-20260902-030`.
- Status: Implemented and complete-candidate verified in `VER-20260902-031`.

### IMP-20260903-001 — Signed schema-43 physical release

- Revalidated the frozen candidate through `scripts/check --full`, then rebuilt the release APK/AAB with the configured Whip release key.
- Installed 0.3.37/code 43 over 0.3.35/code 41 on the explicitly selected Samsung endpoint using the non-clearing release path and cold-launched `MainActivity`.
- Verified the local and installed APK hashes, established signing certificate, preserved Android installation identity, foreground process, absence of fatal/database startup errors, and visible explicit fresh-start boundary.
- Left the destructive “Erase all Whip data” action untouched for the user to confirm on-device.
- Important files: `scripts/device`, `scripts/device-artifacts`, release APK/AAB, and the schema-43 startup recovery surface.
- Related: `FB-20260903-001`, `DEC-20260903-001`.
- Verification: `VER-20260903-001`.
- Status: Released and physically verified.

### IMP-20260903-002 — Lossless 5/3/1 progression edit projection

- Added the persisted progression mode and non-standard higher-suggestion policy to `routineDraftForEditing`, preventing saved Performance review routines from hydrating as Standard progression.
- Expanded the advanced-programming reconstruction test with deliberately non-default progression values so future omissions fail at the edit boundary.
- Important files: `GymScreens.kt` and `GymPowerInputUiTest.kt`.
- Related: `FB-20260903-002`, `FND-20260903-001`, `DEC-20260903-002`.
- Verification: `VER-20260903-002`.
- Status: Implemented, verified, and released in `VER-20260903-004`.

### IMP-20260903-003 — Complete per-lift Routine edit hydration

- Added the saved Training Max basis kind/value/unit and increase-eligibility state to every Routine Exercise edit draft.
- Expanded the reconstruction regression with an actual-1RM basis, non-default source value/unit, and held eligibility, covering the four defaults that previously masked the loss.
- Important files: `GymScreens.kt` and `GymPowerInputUiTest.kt`.
- Related: `FB-20260903-003`, `FND-20260903-002`, `DEC-20260903-003`.
- Verification: `VER-20260903-003`.
- Status: Implemented, verified, and released in `VER-20260903-004`.

### IMP-20260903-004 — Signed lossless-Routine-edit physical release

- Bumped Whip to 0.3.38/code 44 and provisioned a 4 GB Gradle heap after the otherwise-green release gate proved 2 GB insufficient for Android bundle packaging.
- Reused the completed successful gate work, rebuilt the APK/AAB with the established release key, installed the release in place on the explicitly selected Samsung endpoint, and cold-launched `MainActivity`.
- Preserved package installation identity and did not clear application data, bypass the keyguard, run instrumentation on the phone, or invoke Whip's pending destructive fresh-start action.
- Important files: `app/build.gradle.kts`, `gradle.properties`, signed APK/AAB, and `scripts/device`.
- Related: `FB-20260903-003`, `FND-20260903-001`, `FND-20260903-002`.
- Verification: `VER-20260903-004`.
- Status: Released and physically verified within the non-destructive/keyguard-visible boundary.

### IMP-20260903-005 — Gym historical-data and derived-record integrity repair

- Reworked personal-record reconstruction to exclude discarded/archived sessions and consistently honor immutable per-workout record/volume policy, warm-up preference, assisted-record preference, and positive-volume requirements. Discard/restore now reconcile affected exercise records immediately.
- Added immutable numbered-machine direction to workout placements, populated it for routine/free-workout creation and equipment changes, used it after machine deletion in records/graphs, exported it to Gym CSV, and validated/round-tripped it in backups.
- Made repeated workouts start with clean timer, cleanup, invalidation, and revision state; removed the unused API path that could create a finished workout with unfinished copied sets.
- Cleared historical routine-day numeric references before routine child replacement/deletion while retaining stable routine association and authored day position. Serialized machine-version/category position allocation and validated graph preset exercise/enum data transactionally.
- Changed weekly PR attribution to included finished source-session identity, preserving authored local-date semantics for backdated and time-zoned workouts.
- Established Room schema 44, data epoch 4, and backup data version 21 with no migration from the intentionally current-only preceding epoch.
- Important files: `RoutineRepository.kt`, `GymRepository.kt`, `GymDao.kt`, `GymEntities.kt`, `GymModels.kt`, `GymAnalytics.kt`, `GymViewModel.kt`, `GymScreens.kt`, `BackupRepository.kt`, startup epoch files, schema 44, and focused JVM/Android regressions.
- Related: `FB-20260903-004`, `FND-20260903-003`, `FND-20260903-004`, `FND-20260903-005`, `DEC-20260903-004`.
- Verification: `VER-20260903-005`.
- Status: Implemented, verified, and released in `VER-20260903-006`.

### IMP-20260903-006 — Signed schema-44 Gym data-integrity release

- Refreshed the declared inventory to 1,491 product tests, assigned Whip 0.3.39/version code 45, and ran the guarded full release build after the 152-test Gym subsystem pass.
- Built the minified release APK and Play AAB with the established Whip release signer, installed the APK in place on the explicitly selected Samsung endpoint, and cold-launched the application without clearing data or running instrumentation on the phone.
- Verified package/version, exact installed APK hash, signer certificate, preserved Android first-install identity, foreground process/activity, and absence of fatal/Room/SQLite startup logs. Captured the final UI hierarchy/screenshot only through the approved `/storage/emulated/0/whip-debug` helper path; the device was keyguard-locked, so no in-app pixel claim is made.
- Important files: `app/build.gradle.kts`, signed APK/AAB, `scripts/device`, `scripts/device-artifacts`, and the schema-44 startup fresh-start boundary.
- Related: `FB-20260903-004`, `DEC-20260903-004`, `IMP-20260903-005`, `VER-20260903-005`.
- Verification: `VER-20260903-006`.
- Status: Released and physically verified within the non-destructive/keyguard-visible boundary.

### IMP-20260903-007 — Cross-feature semantic and lifecycle integrity repair

- Preserved removed Habit checklist definitions as archived history while limiting current auto-completion to active items; validated connected sources, dimensions, tracking modes, availability, and log dates before writes.
- Centralized repository-owned authored validation for Tasks, Habits, Goals, and Tracks, including bounded names, complete schedules, valid time/duration/reminder values, delimiter-safe tags, and unique child identities.
- Made Area/Tag changes revise affected consumer rows and conditionally rebuild Track search; normalized custom-unit identity transactionally and enforced paired measurement values/units.
- Added pre-mutation backup validation for taxonomy uniqueness, enum/time semantics, metric compatibility, ownership graphs, completion consistency, and exact Track value shapes.
- Made focus task/deadline state atomic, rejected missing Task identities, cleared focus state/schedules after Task or Area-cascade deletion, and protected vulnerable Task/Habit/Track flag and duplication operations with transactions.
- Updated the declared baseline to 1,500 product tests: 583 JVM and 917 Android.
- Important files: `BackupRepository.kt`, `HabitRepository.kt`, `TaskRepository.kt`, `AreaRepository.kt`, `MeasurementRepository.kt`, `TrackRepository.kt`, `AppSettings.kt`, `WhipApplication.kt`, domain models, and focused JVM/Android regressions.
- Related: `FB-20260903-005`, `FND-20260903-006` through `FND-20260903-009`, `DEC-20260903-005`.
- Verification: `VER-20260903-007`.
- Status: Implemented and pushed in `0c37914`.

### IMP-20260903-008 — Signed Whip 0.3.40 cross-feature data-integrity release

- Assigned version 0.3.40/code 46, reran the guarded release gate, and built the minified APK and Play bundle with the established Whip signer.
- Installed the APK in place on the explicitly selected Samsung endpoint, verified the installed artifact hash and package identity, and cold-launched `MainActivity` without clearing data or running instrumentation on the phone.
- Important files: `app/build.gradle.kts`, signed APK/AAB, `scripts/check`, and `scripts/device`.
- Related: `FB-20260903-005`, `DEC-20260903-005`, `IMP-20260903-007`.
- Verification: `VER-20260903-008`.
- Status: Released and physically verified; release version commit `48e8b3b`.

### IMP-20260903-009 — Gym Routine and 5/3/1 authoring UX remediation

- Promoted 5/3/1 program setup from a constrained alert to the shared full-pane primary editor with stable Back/Build actions and an immediate live validation/status summary.
- Added searchable arbitrary-lift selection and contextual Weight + Reps lift creation while preserving the in-progress setup; selected plan/protocol cards now expose semantic selected state and numeric program fields request decimal keyboards.
- Made Program Structure's Training Max detail progressively disclosed with pending-change visibility, and added a direct Program Structure route from generated Main/Supplemental placements.
- Removed app-wide scheme, warm-up generation, and copy-previous bulk controls only from program-controlled Main/Supplemental placements so ordinary routines and explicit advanced edits remain flexible.
- Hid the Routine library's archived filter when no archived routine exists and made the shared exercise-picker query a stable single-line searchable field.
- Added two Android regressions and updated the declared baseline to 1,502 product tests: 583 JVM and 919 Android.
- Important files: `RoutineBuilder.kt`, `GymScreens.kt`, `RoutineBuilderUiTest.kt`, and `docs/testing.md`.
- Related: `FB-20260903-006`, `FND-20260903-010` through `FND-20260903-012`, `DEC-20260903-006`.
- Verification: `VER-20260903-009`.
- Status: Implemented and pushed in `4262468`.

### IMP-20260903-010 — Signed Whip 0.3.41 Gym authoring release

- Assigned version 0.3.41/code 47, ran the guarded release gate, and built the minified signed APK and Play bundle with the established Whip signer.
- Installed the release in place on the explicitly selected Samsung endpoint, verified the exact installed artifact hash and preserved installation identity, and cold-launched `MainActivity` without clearing app data or running physical-device instrumentation.
- Important files: `app/build.gradle.kts`, signed APK/AAB, `scripts/check`, and `scripts/device`.
- Related: `FB-20260903-006`, `IMP-20260903-009`, `VER-20260903-009`.
- Verification: `VER-20260903-010`.
- Status: Released and physically verified; version commit `cd1c4ba`.

### IMP-20260903-011 — Shared Gym lift/exercise search and repeatable 5/3/1 creation

- Rebuilt `ExercisePickerDialog` as the full-pane shared single-select contract with persistent search, result count, an always-visible contextual Create action, an actionable no-results card, query-prefilled creation, 48dp selection rows, stable semantics/test identities, and workout/program-specific explanatory copy.
- Carried the search seed through the Gym catalog editor while retaining the active-workout add/substitute mutation boundary and clearing the seed at every completion, dismissal, or invalidated-authorship boundary.
- Changed custom 5/3/1 Add another lift from “only if an unused Exercise already exists” to an unconditional open-slot picker. Empty libraries now start through that same picker, and a chosen/created lift appends its complete independent Training Max, basis, cycle-increment, and BBB-mapping state.
- Added two Android regressions and updated the declared baseline to 1,504 product tests: 583 JVM and 921 Android.
- Important files: `GymScreens.kt`, `GymCatalogMutationUi.kt`, `RoutineBuilder.kt`, `RoutineBuilderUiTest.kt`, and `docs/testing.md`.
- Related: `FB-20260903-007`, `FND-20260903-013`, `FND-20260903-014`, `DEC-20260903-007`.
- Verification: `VER-20260903-011`.
- Status: Implemented and pushed in `9ba83b0`.

### IMP-20260903-012 — Signed Whip 0.3.42 lift-creation release

- Assigned version 0.3.42/code 48, ran the guarded release gate, and built the minified signed APK and Play bundle with the established Whip signer.
- Installed in place on the selected physical Samsung endpoint, verified artifact/package/signing identity and preserved first-install identity, then cold-launched the app without clearing data or running physical-device instrumentation.
- Important files: `app/build.gradle.kts`, signed APK/AAB, `scripts/check`, and `scripts/device`.
- Related: `FB-20260903-007`, `IMP-20260903-011`, `VER-20260903-011`.
- Verification: `VER-20260903-012`.
- Status: Released and physically verified; version commit `7455073`.

### IMP-20260903-013 — Checklist Habit quick-add removal

- Restricted Quick increment and the preset/range/expert Quick Buttons builder to unsynced Count and Decimal Habits.
- Made draft validation ignore irrelevant quick-add state for all other modes and made repository persistence canonicalize it to increment `1.0` with an empty preset list.
- Updated Details guidance for nonnumeric modes and protected Count → Checklist switching, save output, and repository round-trip with domain and Android regressions.
- Added one JVM and two Android tests; updated the declared baseline to 1,507 product tests: 584 JVM and 923 Android.
- Important files: `HabitModels.kt`, `HabitRepository.kt`, `HabitScreens.kt`, `HabitRulesTest.kt`, `HabitRepositoryTest.kt`, `EntitySaveCoordinatorUiTest.kt`, and `docs/testing.md`.
- Related: `FB-20260903-008`, `FND-20260903-015`, `DEC-20260903-008`, `VER-20260903-013`.
- Verification: `VER-20260903-013`.
- Status: Implemented in `bc3de02`, verified, and released in `VER-20260903-015`.

### IMP-20260903-014 — Cross-app conditional-control and semantic remediation

- Added canonical Habit configuration semantics across target, cadence, ending, precision, checklist, and quick-add state. Corrected At Most to label, bind, evaluate, and persist one maximum value while repairing the former row shape on read.
- Extended Goal type semantics so types that do not use aggregation windows cannot be blocked or influenced by hidden rolling-period state.
- Added Gym tracking/load capability rules shared by Exercise and Machine editors, repository writes, default graph selection, Progress metrics, and comparison eligibility. Hidden machine-level fields no longer block mass-machine saves, and incompatible Exercise/Machine fields are canonicalized.
- Made always-active keyboard shortcuts discoverable, gated Dynamic Color with an Android-version explanation, removed dead child controls beneath hidden Home sections, and populated hard-set choices from the complete current classification enum.
- Added nine JVM and four Android regressions; updated the declared baseline to 1,520 product tests: 593 JVM and 927 Android.
- Important files: `HabitModels.kt`, `HabitRepository.kt`, `HabitScreens.kt`, `GoalModels.kt`, `GoalScreens.kt`, `GymModels.kt`, `GymAnalytics.kt`, `GymRepository.kt`, `GymScreens.kt`, `SettingsScreens.kt`, `AppSettings.kt`, and their focused tests.
- Related: `FB-20260903-009`, `FND-20260903-016` through `FND-20260903-018`, `DEC-20260903-009`.
- Verification: `VER-20260903-014`.
- Status: Implemented in `9e09457`, verified, and released in `VER-20260903-015`.

### IMP-20260903-015 — Signed Whip 0.3.43 conditional-semantics release

- Assigned Whip 0.3.43/code 49, reran the guarded release gate, and built the minified signed APK and Play bundle with the established release signer.
- Installed the signed package in place on the explicitly selected physical Samsung endpoint, confirmed that the installed APK matched the local release artifact, and cold-launched `MainActivity`.
- Preserved Android installation identity and user data; did not run instrumentation on the phone, clear application state, or invoke Whip's destructive fresh-start action.
- Important files: `app/build.gradle.kts`, signed APK/AAB, `scripts/check`, and `scripts/device`.
- Related: `FB-20260903-008`, `FB-20260903-009`, `IMP-20260903-013`, `IMP-20260903-014`.
- Verification: `VER-20260903-015`.
- Status: Released and physically verified; version/release source `c770132`.

### IMP-20260903-016 — Automatic VERA-Codex activation

- Added a default-activation and standing-authorization contract to canonical VERA-Codex, the global Codex policy, and Whip's repository policy.
- Defined development work precisely, retained VERA's explicit direct-parent route for trivial changes, and preserved separate approval requirements for destructive or external effects.
- Marked automatic development activation and standing delegation in the machine-readable seed policy, documented intended installation behavior, and retained the existing enabled Luna/Terra/Sol role configuration.
- Canonical VERA-Codex source was committed and pushed at `bd8629c`.
- Important files: global `AGENTS.md`/`config.toml`, Whip `AGENTS.md`, and canonical VERA-Codex `AGENTS.md`, `README.md`, `.codex/config.toml`, and `routing-policy.yaml`.
- Related: `FB-20260903-010`, `DEC-20260903-010`.
- Verification: `VER-20260903-016`.
- Status: Implemented globally and in both repositories.

### IMP-20260903-017 — Shared Gym Exercise picker and collection-card contracts

- Added `GymExercisePickerBody` as a bounded shared module for search, result count, normalized contextual creation, actionable empty states, progress/error presentation, stable semantics, and a full-height list. Existing single-select and Machine multi-select rows retain their own selection policy.
- Replaced Machine Profile's fixed linked-Exercise alert with a full-pane editor, made creation capability explicit/nullable, propagated the search seed through the Gym catalog path, and assigned distinct Machine/Exercise editor semantics.
- Completed the previously missing Routine Builder advanced-Machine path: nested Exercise creation preserves the Routine and Machine drafts, waits for the created Exercise in library state, auto-links it, and records the independent library save.
- Made the active-workout execution lane a canonical `WhipCollectionCard`, removed Rest's nested raw surface, centralized the shared card's medium shape/elevation/color, adopted spacing/type tokens, and added accessible ready/running duration state.
- Added one Android regression and strengthened existing picker, Machine, Rest, and architecture coverage; declared baseline is now 1,521 product tests: 593 JVM and 928 Android.
- Important files: `GymExercisePicker.kt`, `GymScreens.kt`, `GymCatalogMutationUi.kt`, `RoutineBuilder.kt`, `WhipPagePatterns.kt`, `GymPowerInputUiTest.kt`, `RoutineBuilderUiTest.kt`, `UiDesignArchitectureTest.kt`, and `docs/testing.md`.
- Related: `FB-20260903-011`, `FND-20260903-019`, `FND-20260903-020`, `DEC-20260903-011`.
- Verification: `VER-20260903-017`.
- Status: Implemented in `c8286e0` and released in Whip 0.3.44/code 50; see `VER-20260903-019`.

### IMP-20260903-018 — Direction-aware machine starts and cross-app semantic defaults

- Added pure numbered-machine endpoint and field-precedence resolvers; Set insertion now consults the latest non-null exact Exercise/Profile history before a matched Profile endpoint. Existing archived bindings remain usable, while new archived assignment stays rejected.
- Made blank Level Routine templates derive an actual machine setting without fabricating prescribed machine or canonical load. Explicit template settings remain both actual and prescribed.
- Made fresh Track Number Fields honor the current dimension-specific unit and decimal precision while preserving every authored value when reopening/editing a Field.
- Corrected the reach-weight Goal template to use 150 lb or a canonical 75 kg converted into the selected valid mass unit, including 75,000 g.
- Made reminder creation choose an unused conventional time, disable duplicate confirmation with explanatory error text, and expose a truthful fully occupied state.
- Added five JVM and six Android regressions; declared baseline is now 1,532 product tests: 598 JVM and 934 Android.
- Important files: `GymModels.kt`, `GymDao.kt`, `GymRepository.kt`, `RoutineRepository.kt`, `TrackScreens.kt`, `GoalScreens.kt`, `ProductivityEditorComponents.kt`, their focused tests, and `docs/testing.md`.
- Related: `FB-20260903-012`, `FND-20260903-021`, `FND-20260903-022`, `DEC-20260903-012`.
- Verification: `VER-20260903-018`.
- Status: Implemented in `92c25f9` and `829c444` and released in Whip 0.3.44/code 50; see `VER-20260903-019`.

### IMP-20260903-019 — Signed Whip 0.3.44 semantic-default release

- Assigned Whip 0.3.44/code 50 and pushed release-source commit `c01b2ff`.
- Ran the complete guarded release workflow, produced minified signed APK and Play bundle artifacts, and installed the release in place on the explicitly selected Samsung endpoint.
- Verified package version, local/installed APK identity, single-signer release certificate, preserved first-install identity, successful cold launch, foreground activity, and absence of Whip/runtime/database fatal errors.
- Preserved user data; no physical-device instrumentation, uninstall, package clear, fresh-start confirmation, or unrestricted device-artifact write occurred.
- Important files: `app/build.gradle.kts`, signed APK/AAB, `scripts/check`, and `scripts/device`.
- Related: `FB-20260903-011` through `FB-20260903-013`, `IMP-20260903-017`, `IMP-20260903-018`.
- Verification: `VER-20260903-019`.
- Status: Released and physically verified; release source `c01b2ff`.

### IMP-20260903-020 — Unified Gym Exercise terminology

- Reworded routine creation, guided and advanced 5/3/1 programming, Training Max progression/review, BBB mapping, workout substitution/removal, Settings, accessibility descriptions, and surfaced repository validation around Exercise/Exercises.
- Preserved `MainLift`, `FiveThreeOneProgramLift`, internal variables, serialized enum identity, stable `*-lift-*` test tags, Deadlift names, search synonyms, and fixture values.
- Updated UI and state assertions, including an explicit visible “Add an Exercise” entry assertion for the empty-library custom 5/3/1 journey.
- Important files: `RoutineBuilder.kt`, `GymScreens.kt`, `FiveThreeOneBuilder.kt`, `FiveThreeOneCycleReview.kt`, `FiveThreeOneProgramming.kt`, `SettingsScreens.kt`, `GymRepository.kt`, `RoutineRepository.kt`, and focused tests.
- Related: `FB-20260903-014`, `FND-20260903-023`, `DEC-20260903-013`.
- Verification: `VER-20260903-020`.
- Status: Implemented in `6527350`, committed, pushed, and released in Whip 0.3.45/code 51; see `VER-20260903-023`.

### IMP-20260903-021 — Canonical Exercise/Measurement clean-data boundary

- Established the current-only Room schema 45, data epoch 5, and backup data version 22. Older local data and complete backups are rejected; the two-step user-confirmed fresh-start gate is the sole entry into the contract.
- Made `Exercise` the persisted Gym/program noun, including 5/3/1 main-work identities, and renamed the shared Habit/Goal ledger from `Metric` to `Measurement` through Room, backup, Health, tests, benchmark seeding, and baseline profile.
- Retained the distinct graph-analytics `GymGraphMetric` terminology and removed obsolete identity-symbol conversion and unowned Goal mutation entry points.
- Removed old Room schemas and the old widget snapshot decoder. Retired rule-engine runtime rebuilding/scheduling and future Task-series replication are disconnected; Link/Trigger persistence removal remains a separate hard-cut chunk.
- Source: `b73893e` on `origin/main`; released in Whip 0.3.45/code 51 after the `acbf2d4` hard cut; see `VER-20260903-023`.

### IMP-20260903-022 — Retired automation persistence hard cut

- Removed the unsupported Link/Trigger Automation subsystem from Room, application wiring, repositories/domain models, runtime scheduling, Task-series copying, Track provenance/CSV logic, deletion coordinators and previews, backup export/import/remap, UI state, baseline profile, and test fixtures.
- Replaced compatibility-shaped deletion and merge paths with current authored/history contracts, including removal of the final no-op backup child-merge helper and automation-only test infrastructure.
- Advanced the intentional clean-data boundary to Room schema 46, data epoch 6, and backup version 23. Older app data and backups are not migrated; the existing two-step in-app fresh-start confirmation remains the only authorized reset path.
- Updated current product, architecture, testing, and QA documentation, including the exact `dataModelEpoch` backup envelope key and the 1,503-test baseline.
- Important files: `WhipDatabase.kt`, `BackupRepository.kt`, deletion coordinators, Track persistence/domain/UI, `DataEpochGate.kt`, `LocalDataResetter.kt`, schema 46, baseline profile, focused tests, `README.md`, `docs/architecture.md`, and `docs/testing.md`.
- Related: `FND-20260903-024`, `IMP-20260903-021`, `VER-20260903-022`.
- Status: Implemented, independently accepted, committed, and pushed in `acbf2d4`; released in Whip 0.3.45/code 51; see `VER-20260903-023`.

### IMP-20260903-023 — Signed Whip 0.3.45 clean-slate release

- Assigned Whip 0.3.45/code 51 and pushed release-source commit `e163318`.
- Ran the complete guarded local release gate, built minified signed APK and Play bundle artifacts, and installed the signed package in place on the explicitly selected Samsung endpoint.
- Verified the installed version and APK hash against the local artifact, the established single release signer, preserved Android installation identity, successful cold launch, foreground activity, and no Whip/AndroidRuntime/Room/SQLite fatal error.
- Did not run instrumentation, clear data, uninstall, or confirm the destructive fresh-start action on the physical phone. The epoch-6 gate leaves that authorized decision to the user in the app.
- Important files: `app/build.gradle.kts`, signed APK/AAB, `scripts/check`, and `scripts/device`.
- Related: `FND-20260903-023`, `FND-20260903-024`, `IMP-20260903-020` through `IMP-20260903-022`.
- Verification: `VER-20260903-023`.
- Status: Released and physically verified; release source `e163318`.

### IMP-20260903-024 — Context-owned Exercise picker priority semantics

- Behavior changed: Preferred ordering in the shared Exercise picker no longer assigns a substitute role. The 5/3/1 setup labels its selected Exercise as “Current selection”; only active-workout substitution opts into preferred-substitute copy. The Routine advanced section is now “Preferred workout substitutes” and explicitly states that it is optional and leaves the programmed Exercise unchanged.
- Important files/symbols: `GymExercisePickerBody`, `ExercisePickerDialog`, `FiveThreeOneProgramSetupDialog`, `RoutinePlacementEditor`, and `RoutineBuilderUiTest`.
- Persistence/migration/history impact: None. Existing priority and substitute IDs are unchanged; 5/3/1 generation continues to create Main-work placements with empty substitute lists unless a user separately configures substitutes.
- Compatibility and limitations: Actual substitution ordering is preserved. The active-workout caller owns its contextual copy; ordinary workout addition and 5/3/1 selection do not inherit it.
- Commit/push: `ad2f3a9` on `origin/main`.
- Related: `FB-20260903-015`, `FND-20260903-025`, `VER-20260903-024`.
- Verification: `VER-20260903-024`.
- Status: Implemented, focused-emulator verified, independently accepted, committed, pushed, and released in Whip 0.3.46/code 52; see `VER-20260903-025`.

### IMP-20260903-025 — Signed Whip 0.3.46 Exercise-picker semantics release

- Assigned Whip 0.3.46/code 52 and pushed release-source commit `1f0e3af`, containing the `ad2f3a9` Exercise-picker correction.
- Ran the complete guarded local release gate and installed the signed package in place on the explicitly selected Samsung endpoint `192.168.2.187:44401`.
- Verified the installed APK byte-for-byte against the signed local release, the established single release signer, preserved Android installation identity, successful cold launch, resumed `MainActivity`, and absence of app/runtime/database fatals.
- Did not run instrumentation, clear data, uninstall, or confirm the user-owned epoch-6 fresh-start action on the physical phone.
- Related: `FB-20260903-015`, `FND-20260903-025`, `IMP-20260903-024`, `VER-20260903-024`.
- Verification: `VER-20260903-025`.
- Status: Released and physically verified; release source `1f0e3af`.

### IMP-20260903-026 — One balanced summary-first productivity collection layout

- Behavior changed: Removed the compact/comfortable density choice and made the expandable summary row the sole Tasks, Habits, Goals, and Tracks collection grammar across Home and domain workspaces. Shared cards now use the medium shape, 12 dp horizontal/10 dp vertical padding, 6 dp direct-content rhythm, and 8 dp collection gaps. Primary actions remain one tap away; details and secondary controls expand inline.
- Important files/symbols: `ProductivityItemCard` and `ProductivityItemHeader` in `ItemControlPatterns.kt`; `TaskRow`, `HabitProgressCard`, `GoalCard`, and `TrackSummaryRow`; list ownership in `WhipApp.kt`, `HabitScreens.kt`, `GoalScreens.kt`, and `TrackScreens.kt`; Appearance cleanup in `SettingsScreens.kt`.
- Persistence/migration/history impact: Cleanly removed `compactItemLayout` from `AppSettings`, SharedPreferences, backup export/import, composition locals, fixtures, and cause/effect inventories. Backup format is 24 and rejects format 23; Room schema 46 and data epoch 6 are unchanged. No local reset or authored/history rewrite was needed.
- Compatibility and limitations: This is an explicit clean cut with no density compatibility layer or old-backup upgrade path. Track master-pane compactness and other viewport-driven adaptive layouts remain independent. The change is not released to a physical phone.
- Commit/push: `b6e3ef2` on `origin/main`.
- Related: `FB-20260903-016`, `FND-20260903-026`, `DEC-20260903-014`, `VER-20260903-026`.
- Verification: Focused and full JVM, targeted API 34 emulator, exact stale-symbol, visual, and independent high-risk review evidence are recorded in `VER-20260903-026`.
- Status: Implemented, verified, independently accepted, committed, and pushed; awaiting user validation.

### IMP-20260903-027 — Whole-product semantic UI convergence

- Behavior changed: Completed the whole-product follow-up to the unified summary-first collection layout. Ordinary grouped information and Settings groups now use canonical surfaces; dialogs share one body rhythm; Task, Habit, Goal, and Track editors share slot-based identity and organization sections; the cross-product date picker has neutral ownership; and duplicate Unit hierarchy was removed.
- Gym/Routine boundary: Machine and workout-history cards use the shared collection role, Gym and Routine share bounded Exercise-picker mechanics, “Favorites” copy is consistent, and `GymDestinationHost` extracts only stable destination chrome. Selection, reorder, chart/calendar, provenance/warning, workout execution, editors, coordinators, and overlays retain explicit domain ownership.
- Destructive integrity: Habit permanent deletion now reviews a repository-authored graph impact bound to UUID and complete-graph revision, revalidates transactionally, rejects stale confirmation, restores request state, and retains reviewed impact through safe retry or uncertain completion.
- Review/platform boundary: Review now exposes period-bounded All Tracks evidence without scoring incomparable values and has one Open Tracks action. Widget configuration and Health-permission rationale share visual startup/theme/window hosting while keeping their platform behavior separate.
- UX defects remediated during full regression: saved Gym exercise graph defaults now initialize per selected Exercise; the neutral date picker retains stable two-letter weekday labels; summary-first Habit/Task/Goal/Track journeys reveal details before secondary mutation; Goal milestones reveal before milestone actions; and semantic tags identify Track primary actions without position coupling.
- Clean-cut ownership: Removed the remaining active `CompactItem*` disclosure vocabulary rather than retaining aliases. No new schema, data-epoch, backup-format, or authored-history change was required beyond the already accepted backup-24 density clean cut, and no app data was reset.
- Important files/symbols: `WhipGroupedInformationCard`, `WhipSettingsSectionCard`, `WhipDialogBody`, `ProductivityIdentitySection`, `ProductivityOrganizationSection`, `WhipDatePickerDialog`, `GymExercisePickerBody`, `GymDestinationHost`, `ExternalWhipActivityHost`, `ItemDisclosureState`, and Habit deletion preview/coordinator state.
- Source: audit/decision commits `b7222de` and `b0effdd`; implementation and regression commits `5297940` through `a65889f`, all pushed to `origin/main`.
- Related: `FB-20260903-017`, `FND-20260903-027` through `FND-20260903-033`, `DEC-20260903-015`, `VER-20260903-027`.
- Status: Implemented, fully verified, independently accepted, committed, and pushed; not released to the physical phone and awaiting user validation.

### IMP-20260904-001 — Current VERA-Codex clean replacement

- Behavior changed: Installed the frozen current working-tree VERA-Codex bundle in `/mnt/c/Users/commv/.codex`, `/root/.codex`, and Whip. Non-trivial development, diagnosis, testing, and code review now invoke `$vera-codex` automatically; the active role set is exactly Terra scout/low, Terra builder/medium, Terra reviewer/high, Sol architect/xhigh, and Sol critical builder/xhigh.
- Exact replacement: Both personal homes received the exact role files and exact skill tree. Their `AGENTS.md` files are identical derivatives of the source with only the repository-relative skill path changed to `$CODEX_HOME/skills/vera-codex/SKILL.md`. Whip received byte-exact source `AGENTS.md`, `.codex/config.toml`, `.codex/agents/`, and `.agents/skills/vera-codex/`. Active Luna and `routing-policy.yaml` files were removed.
- Personal compatibility: Only `model`, `model_reasoning_effort`, and the five approved `[agents]` values were overlaid. Windows notifications, plugins, MCP servers/environment, approvals, sandbox, desktop, project trust, and shell policy remain unchanged; root's Whip trust remains unchanged.
- Rollback: Owner-only pre-install copies, absence markers, staged targets, frozen source hashes, and restoration guidance are stored outside Whip and active instruction discovery at `/root/.local/state/vera-codex/backups/20260904T050326Z`.
- Scope/constraints: No application source, test, device, user data, credential, plugin, deployment, publication, commit, or push action was part of this change. Concurrent unrelated Whip application edits were preserved untouched.
- Related: `FB-20260903-010`, `FB-20260904-001`, `VER-20260904-001`.
- Verification: `VER-20260904-001`.
- Status: Superseded by `IMP-20260904-005`; this entry remains the historical record of the prior working-tree installation.

### IMP-20260904-002 — Product-quality presentation convergence

- Behavior changed: Introduced shared localized calendar presentation for weekday labels and month headers, then migrated the neutral date picker, Home planner, Task, Habit, Gym, productivity, and Settings consumers. Task and Track now share the selection action surface; Health rationale uses the grouped information surface; Routine's empty workout picker uses the canonical empty state.
- Important files/symbols: `WhipWeekdayFormatter`, `rememberWhipWeekdayFormatter`, `WhipCalendarMonthHeader`, `WhipCalendarWeekdayHeader`, and `WhipSelectionActionPanel`.
- Compatibility/data impact: Presentation and test-only change. No schema, data epoch, backup, date identity, lifecycle, persistence, release, reset, or physical-device change.
- Verification: New unit, architecture, and Compose tests cover localized recomposition, RTL/dark/narrow/large-text month controls, 48 dp targets, selection panels, and Routine empty state; complete results are in `VER-20260904-002`.
- Related: `FB-20260904-002`, `FND-20260904-001`, `DEC-20260904-001`, `VER-20260904-002`.
- Status: Implemented, verified, and independently accepted; awaiting user validation.

### IMP-20260904-003 — Fail-closed Android targeting, proportional QA, and Whip 0.3.47 release

- Behavior changed: Added `scripts/android-target-guard` and applied it to `scripts/check`, `scripts/coverage`, `scripts/qa-targeted`, `scripts/device`, and the app/benchmark connected-test tasks. Instrumentation now accepts only an explicitly selected connected emulator, while release accepts only an explicitly selected connected physical device. There is no implicit or transitional selector.
- Test system: The routine emulator gate retains exact source/device/test-batch signatures so unchanged successful batches are reused. Fresh coverage runs the 923-test inventory in 11 isolated processes, requires exact class/result accounting with zero skips or failures, accepts exactly one fresh nonempty execution-data artifact per process, and combines them through a dedicated report task. Two bounded Compose tests gained synchronization-only stability repairs.
- Release: Bumped the app to 0.3.47/code 53, committed and pushed source `6b02d75`, built the signed APK/AAB, then installed the APK in place on the explicitly guarded phone. No uninstall, downgrade, reset, schema, data-epoch, backup-format, or production-behavior change occurred in this P0 repair.
- Rollback: Source can revert `6b02d75`; device rollback is not automated because Android downgrade would be destructive or require data loss. Preserve the installed data and advance with a higher signed version if a repair is needed.
- Related: `FB-20260904-003`, `FB-20260904-004`, `FND-20260904-002`, `DEC-20260904-002`, and `VER-20260904-003`.
- Status: Implemented, verified, committed, pushed, signed, and physically released; awaiting user validation.

### IMP-20260904-004 — Deterministic change routing and frozen-candidate evidence

- Behavior changed: `scripts/check` is now the proportional development gate backed by `scripts/change-router`; it unions/deduplicates profiles and exact selectors, explains every changed path, handles deletions/renames, and escalates unknown production/build/harness inputs. `scripts/candidate` is the one fresh complete candidate gate with repository snapshots, drift rejection, atomic evidence, and canonical verification of every required file, manifest field, fresh Android aggregate, checksum row, and artifact.
- Shared Android system: `scripts/android-test-engine` now supplies the one inventory/batch/accounting implementation used by check/targeted and coverage paths. It preserves graphics-first/reset-last ordering, emulator-only target validation, exact class/test/failure/skip accounting, fresh coverage execution-data rules, and exact cache signatures including source, APKs, runner, selectors, and emulator identity.
- Release boundary: The pre-existing `scripts/device` implementation remains byte-for-byte unchanged by this task. Candidate evidence is local qualification evidence only; no release/device command was executed or newly authorized.
- Regression tools: `scripts/test-change-router`, `scripts/test-candidate-evidence`, and expanded `scripts/test-android-target-guard` cover route fixtures, evidence/source/artifact rejection, target guards, fresh coverage, exact reuse, localized test invalidation, production-wide invalidation, and corrupt-cache rejection.
- Compatibility recovery: Restored `scripts/check --full` as the historical device-independent complete JVM coverage, Android-test compile, lint/static, debug/release/AAB/benchmark build, merged-manifest, and release-metadata gate. It no longer aliases `scripts/candidate`; Android execution remains explicit. Candidate evidence now retains and checksums `merged-manifest.xml` and `release-output-metadata.json`, and verification rejects their absence or semantic tampering after checksum rewrites. `scripts/test-check-full` proves the no-serial/no-candidate/no-instrumentation contract, while `scripts/test-candidate-evidence` covers the added evidence failures.
- Compatibility/rollback: Tooling and documentation only; no app source, Gradle guard, release script, schema, backup, package, or data change. Revert the complete harness/docs set together, retain the target guards, and discard generated `build/candidate-evidence` state.
- Related: `FB-20260904-005`, `FB-20260904-006`, `DEC-20260904-003`, `VER-20260904-004`, and `VER-20260904-005`.
- Status: Implemented and deterministically verified locally, including the real device-independent full gate; uncommitted, unpushed, unreleased, and awaiting fresh final review.

### IMP-20260904-005 — Pinned e80d2cb VERA-Codex clean-cut installation

- Behavior changed: Materialized `.agents/skills/vera-codex/SKILL.md` from pinned public source object `e80d2cbd5e92c602c0f2d0db68aa9a1f239eec8d`, verified its SHA-256, and atomically replaced the three stale Whip/root/Windows skill bodies. Changed only the Windows personal config's top-level `model` from `gpt-5.6-sol` to canonical `gpt-5.6-terra`.
- Important files/symbols: `/root/repos/whip/.agents/skills/vera-codex/SKILL.md`, `/root/.codex/skills/vera-codex/SKILL.md`, `/mnt/c/Users/commv/.codex/skills/vera-codex/SKILL.md`, and the top-level `model` key in `/mnt/c/Users/commv/.codex/config.toml`.
- Compatibility and limitations: These are exactly four runtime deltas. The five role files and skill metadata in every scope, Whip `AGENTS.md`/project config, both approved global `AGENTS.md` derivatives, root personal config, every unrelated personal-config semantic value, and all six Luna/routing-policy absences remain unchanged. Existing sessions may retain their already-loaded instructions; a new session is required for end-to-end observation.
- Persistence/migration/history impact: No Whip application source, test, harness, schema, data, package, device, release, deployment, credential, or plugin state changed. Owner-only recovery data and rollback guidance are at `/root/.local/state/vera-codex/backups/20260904T185021Z-e80d2cb`.
- Commit/push: None. The installation and memory records remain local; any remote publication requires separate explicit approval.
- Related: `FB-20260904-001`, `FB-20260904-008`, `IMP-20260904-001`, `VER-20260904-006`.
- Verification: `VER-20260904-006`.
- Status: Implemented, deterministically verified, and independently accepted; remote publication remains pending separate approval.

### IMP-20260904-006 — Deterministic workout-group save retry test synchronization

- Behavior changed: The Android-only workout-group retry test now invokes the exposed semantic submit action and proves the saving transition before inspecting disabled controls. Production save, persistence, schema, and user behavior are unchanged.
- Important file/symbol: `SafetyChoiceUiTest.workoutGroupSaveFailureKeepsParentAndDraftOpenForRetry`.
- Verification: `VER-20260904-007`.
- Related: `FB-20260904-007`.
- Status: Included in the fresh complete 0.3.49 candidate and released in Whip 0.3.49/code 55; awaiting user validation.

### IMP-20260904-007 — VERA-Codex autonomous task-completion policy

- Behavior changed: Whip now carries the canonical VERA persistence rule: progress reports are nonterminal, optional context does not pause safe reversible work, and the parent continues through actionable investigation, repair, validation, and review until acceptance or a genuine no-progress boundary.
- Safety preserved: The policy expressly retains non-inference for destructive/irreversible, production, credential, billing, publication, deployment, and other external actions. A second Sol failure ends only that retry loop and requires any distinct safe path to be considered first.
- Important file: `.agents/skills/vera-codex/SKILL.md`, synchronized byte-for-byte from the canonical VERA source after its validator passed.
- Related: `FB-20260904-009`, `DEC-20260904-004`, `VER-20260904-008`.
- Status: Implemented, byte-for-byte synchronized, and independently accepted in canonical VERA-Codex and Whip.

### IMP-20260904-008 — Corrected VERA policy prepared for global rollout

- Behavior changed: Canonical VERA-Codex now states consistently that a second material Sol failure ends only that retry loop, requires a scan for a distinct safe in-scope path, and permits blocker reporting only when no such path can make meaningful progress. The README routes and decision topology now express the same contract, and the validator enforces it while rejecting the stale immediate-report wording.
- Important files/symbols: Canonical `.agents/skills/vera-codex/SKILL.md` repair/escalation contract, `README.md` routes and decision topology, and `scripts/validate_bundle.py` second-Sol policy invariants; Whip durable rollout records only.
- Compatibility and rollback: Protected-action and non-inference boundaries remain unchanged. Global preimages are owner-only at `/root/.local/state/vera-codex/backups/20260904T_global_persistence`; all three runtime copies now match canonical.
- Commit/push: Canonical VERA-Codex was published at `d82bad8`; Whip synchronization was published at `e84bbf5`.
- Related: `FB-20260904-010`, `DEC-20260904-004`, `IMP-20260904-007`, `VER-20260904-009`.
- Verification: `VER-20260904-009`.

### IMP-20260904-009 — Task empty-state orientation below Quick Capture

- Behavior changed: Today and Inbox now retain their existing destination-specific clear-state message whenever no Task is visible, including on a new profile. Quick Capture remains the first, direct action and no second creation CTA was added.
- Important files: `WhipApp.kt`, `ProductivityDefaultsUiTest.kt`, and `docs/quality/full-product-surface-inventory-2026-09-04.tsv`.
- Compatibility/data: Presentation-only. No Task placement, capture parsing, schema, data epoch, backup, or authored data behavior changed.
- Related: `FB-20260904-011`, `FND-20260904-003`, `DEC-20260904-005`.
- Verification: `VER-20260904-010`, `VER-20260904-012`.
- Status: Included in the fresh complete candidate and released in Whip 0.3.49/code 55; awaiting user validation.

### IMP-20260904-010 — Deterministic Settings retry lifecycle test

- Behavior changed: The Settings responsive retry regression now waits for the exact request it owns before injecting simulated persistence results. It continues to prove failure visibility, deliberate discard, distinct retry ownership, and successful close.
- Important file: `SettingsResponsiveUiTest.kt`.
- Compatibility/data: Test-only synchronization. Production settings behavior, persistence, schema, backup, and user data are unchanged.
- Related: `FND-20260904-004`, `VER-20260904-011`.
- Verification: `VER-20260904-011`, `VER-20260904-012`.
- Status: Included in the fresh complete candidate and released in Whip 0.3.49/code 55; awaiting user validation.

### IMP-20260904-011 — Signed Whip 0.3.49 UX/QA release

- Release: Advanced to 0.3.49/code 55, built the signed APK and Play-ready AAB, and installed the APK in place on the explicitly selected physical Samsung without clearing, resetting, uninstalling, or downgrading.
- Behavior included: Task Today and Inbox retain explicit clear-state orientation below Quick Capture; Settings retry lifecycle coverage is deterministic.
- Important files/artifacts: `app/build.gradle.kts`, `app/build/outputs/apk/release/app-release.apk`, `app/build/outputs/bundle/release/app-release.aab`, and `VER-20260904-012`.
- Compatibility/data: Schema 46, data epoch 6, backup version 24, package identity, and signer continuity are unchanged. Android install identity was preserved; forward fixes must use a higher code.
- Source: `0bc32cb` on `origin/main`.
- Status: Released and physically verified; awaiting user validation.

### IMP-20260906-001 — VERA-Codex active-layer uninstall

- Behavior changed: Deleted the nine tracked Whip VERA runtime files (`AGENTS.md`, project `.codex/config.toml`, five project role TOMLs, and the two-file project skill); deleted the eight VERA-only instruction/role/skill files from each of `/root/.codex` and `/mnt/c/Users/commv/.codex`; and removed the top-level `model`, `model_reasoning_effort`, and complete `[agents]` table from both personal configs. The Windows config also lost its first two VERA comments and the stale trust entry for `/root/repos/vera-codex`. Resulting empty VERA skill/role/project-config directories were removed without touching shared skill directories.
- Configuration preservation: The root Whip trust remains `trusted`. Normalized before/after comparison proves all unrelated root and Windows config semantics are identical; Windows approval policy/reviewer, sandbox, service tier, notification, marketplaces, plugins, features, MCP/environment, desktop, Windows, every other project trust, and shell policy remain exact. Independent textual-subtraction checks prove the global configs changed only by the authorized VERA blocks and deleted-checkout trust entry.
- Compatibility and history: New sessions load no standing VERA policy or custom VERA roles; an already-running session can retain previously loaded instructions in its context. Historical feedback, decisions, implementation, and verification evidence remains intact. No Whip application, test, build, generated, device, release, credential, plugin, or remote state changed.
- Rollback: Owner-only transaction preimages, SHA-256/mode manifests, and normalized config projections were held at `/root/.local/state/vera-uninstall.OfcocF` through final acceptance, then deliberately purged with the deleted source and retained VERA state.
- Cleanup result: Fresh critical review passed; the reviewed active-layer removal was committed locally as `9821225`; `/root/repos/vera-codex`, `/root/.local/state/vera-codex`, and the temporary transaction backup were deleted and verified absent. No push or remote deletion occurred.
- Related: `FB-20260906-001`, `FB-20260906-002`, `DEC-20260906-001`, `VER-20260906-001`.
- Verification: `VER-20260906-001`.
- Status: Implemented.

### IMP-20260906-002 — Tiered fast development and release-readiness loop

- Behavior changed: The default `scripts/check` path now runs routed JVM tests and lightweight source checks without compiling Android tests, linting, or packaging. `scripts/check --ready` explicitly adds those pre-commit costs; `--emulator` continues to execute only routed Android classes. Android-test-only edits still compile headlessly. Candidate-required changes can receive focused development feedback without being mistaken for release-ready.
- Important files/symbols: `scripts/check`, `scripts/qa-targeted --jvm-only`, `scripts/change-router` `HARNESS` records, `scripts/test-check-fast`, `scripts/test-change-router`, `README.md`, and `docs/testing.md`.
- Persistence/migration/history impact: Development tooling and documentation only; no application source, schema, backup format, release identity, installed package, or user data changed.
- Compatibility and limitations: `scripts/check --full`, fresh candidate semantics, emulator/physical target guards, signing, and deployment remain intact. This harness-changing work itself still requires one fresh candidate before any release. Physical release was not authorized or performed.
- Commit/push: `7fab98c` pushed to `origin/main`.
- Related: `FB-20260906-003`, `DEC-20260906-002`, `VER-20260906-002`.
- Verification: Deterministic router, fast/default, full-gate, candidate-evidence, and Android-target fixture suites passed; the real changed-path fast command passed in about one second of reported wall time.
- Status: Implemented, fixture-verified, committed, and pushed; candidate qualification remains pending only before Play Store release.

### IMP-20260906-003 — Fast owner-phone release lane

- Behavior changed: `scripts/device release-deploy` now runs the changed-path fast check before signing/building/installing instead of the complete local suite. The complete fresh candidate is explicitly reserved for Play Store qualification. The physical target guard, `adb -s` install, signed package identity, in-place update, and launch verification remain mandatory.
- Important files/symbols: `scripts/device` `build_release_apk`, `scripts/change-router` harness routing, `scripts/check` Play Store messaging, `scripts/test-android-target-guard`, `README.md`, and `docs/testing.md`.
- Persistence/migration/history impact: No application behavior, schema, data epoch, backup format, or existing user data changed. Whip 0.3.50/code 56 remains the prepared identity.
- Compatibility and limitations: A personal-phone release proves only affected checks, signed artifact creation, exact physical install, and smoke; it must not be represented as complete-suite or Play Store evidence. No physical target was connected while this tooling chunk was implemented.
- Commit/push: `a9fd764` pushed to `origin/main`.
- Related: `FB-20260906-004`, `FB-20260906-005`, `DEC-20260906-003`, `VER-20260906-003`.
- Verification: Router and Android target/release regressions passed, including the new assertion that personal-phone deployment invokes `scripts/check` and never `scripts/check --full`.
- Status: Implemented, fixture-verified, committed, pushed, and exercised by the 0.3.50 phone release.

### IMP-20260906-004 — Signed Whip 0.3.50 owner-phone release

- Behavior changed: Advanced Whip to 0.3.50/code 56 and installed the latest signed source in place on the explicitly selected physical phone through the new fast owner-development lane. This release contains the prompt-to-test cycle overhaul and personal-phone/store qualification split; production application behavior remains the 0.3.49 baseline.
- Important files/symbols: `app/build.gradle.kts`, `scripts/device`, signed `app-release.apk`, local `app-release.aab`, and `VER-20260906-004`.
- Persistence/migration/history impact: Schema 46, data epoch 6, and exact-match backup version 24 are unchanged. Android first-install identity and existing application data were preserved; no reset, clear, uninstall, downgrade, or fresh-start confirmation occurred.
- Compatibility and limitations: This owner-phone release used affected checks, signed build, exact install/hash, cold launch, and log smoke. Per direct user policy it is not complete-candidate or Play Store evidence; public release requires a new complete fresh candidate.
- Commit/push: Release identity source `6051d6c`; fast phone-lane source `a9fd764`, both pushed to `origin/main` before build/install.
- Related: `FB-20260906-004`, `FB-20260906-005`, `DEC-20260906-003`, `VER-20260906-004`.
- Verification: Signed APK/AAB integrity, established certificate, installed package/version/hash, preserved first-install time, cold launch/foreground activity, and bounded fatal logs passed.
- Status: Released and physically verified; awaiting user validation.

### IMP-20260906-005 — Purposeful Habit Today overview

- Behavior changed: Habit Today now presents one rounded daily overview with tracking-aware status copy, localized date context, and value-first progress metrics. Manual duration entry stays in that context. Skip Today is an icon-led secondary card that explains its effect instead of an isolated button under another heading; the existing header, tabs, state actions, and docked primary check-in action are unchanged.
- Important files/symbols: `HabitActionsDialog`, `HabitDayProgress.inspectorTodaySummary`, `HabitTodayMetric`, and `ActivityHistoryUiTest.habitTodayUsesOneResponsiveOverviewAndExplainedSecondaryAction`.
- Persistence/migration/history impact: Presentation and copy only. No schema, data epoch, backup format, scheduling, streak calculation, mutation, or stored user data changed.
- Compatibility and limitations: Verified in dark mode at a constrained 320 dp pane and 200% text. The complete product suite and Play Store candidate were intentionally not run; the current physical-phone release remains 0.3.50/code 56 and does not contain this change.
- Commit/push: `13f00a0` pushed to `origin/main`.
- Related: `FB-20260906-006`, `FND-20260906-001`, `DEC-20260906-004`, `VER-20260906-005`.
- Verification: `VER-20260906-005`.
- Status: Released in Whip 0.3.51/code 57; awaiting user validation.

### IMP-20260906-006 — Signed Whip 0.3.51 Habit Today owner-phone release

- Behavior changed: Advanced Whip to 0.3.51/code 57 and installed the purposeful Habit Today redesign in place on the explicitly selected owner phone through the fast private-development lane.
- Important files/symbols: `app/build.gradle.kts`, `HabitActionsDialog`, signed `app-release.apk`, local `app-release.aab`, and `VER-20260906-006`.
- Persistence/migration/history impact: Schema 46, data epoch 6, and exact-match backup version 24 are unchanged. Android first-install identity and existing app data were preserved; no reset, clear, uninstall, downgrade, or fresh-start confirmation occurred.
- Compatibility and limitations: This private release used the prior focused Habit verification plus a fast clean-source check, signed release build/install, and device smoke. It is not complete-suite, candidate, or Play Store evidence.
- Commit/push: Release source `e833272` pushed to `origin/main` before artifact creation and install.
- Related: `FB-20260906-005`, `FB-20260906-006`, `FB-20260906-007`, `DEC-20260906-003`, `DEC-20260906-004`, `IMP-20260906-005`, `VER-20260906-006`.
- Verification: `VER-20260906-006`.
- Status: Released and physically verified; awaiting user validation.

### IMP-20260906-007 — Emulator-only visual catalog capture runtime

- Behavior changed: Added a dedicated instrumentation capture helper and deterministic MainActivity journeys for 45 empty/populated primary pages across Home, Tasks, Habits, Goals, Tracks, Gym, and Settings. `scripts/ui-catalog capture` now runs the exact implemented selectors, exports matching device-level PNG/XML pairs through a persistent MediaStore staging directory, rejects missing and extra evidence, and writes a SHA-256/byte manifest. `--family` limits execution and accounting to one product family.
- Important files/symbols: `VisualCatalogCapture.kt`, `VisualCatalogPagesTest`, `scripts/ui-catalog capture`, and `docs/quality/ui-surface-catalog.tsv`.
- Persistence/migration/history impact: Test and quality tooling only. Production schema 46, data epoch 6, backup version 24, application behavior, release identity, and user data are unchanged. Test fixtures delete only disposable emulator data and the exact `Download/whip-ui-catalog` evidence collection.
- Compatibility and limitations: The catalog currently accounts for 171 required surfaces; 45 page states are implemented and 126 dialog/menu/special-state selectors remain pending. Full lint deliberately remains fail-closed until those rows are implemented. No candidate, signed build, phone connection, phone instrumentation, install, or publication occurred.
- Related: `FB-20260906-008`, `DEC-20260906-005`, `VER-20260906-007`.
- Status: Implemented and emulator-verified as the first capture-runtime milestone; exhaustive audit work remains in progress.

### IMP-20260906-008 — Complete Shared and Tasks visual-catalog families

- Behavior changed: Expanded the emulator-only catalog from primary pages to every declared Shared and Tasks surface. Production component fixtures now cover setup/recovery/domain states, reusable inspectors and pickers, destructive/unsaved/queue dialogs, task create/edit/recipe/repeat/value/action/detail/delete flows, workspace menus, rescheduling, bulk selection/edit/delete, and pending editor launch. Real `MainActivity` journeys cover global add/search/review and preserve production navigation semantics.
- Capture reliability: Moved exact MediaStore cleanup to the guarded host collector so selector batches cannot erase evidence produced by earlier test processes. Cleanup remains restricted to `Download/whip-ui-catalog/` on an explicitly selected emulator; each family export still rejects missing and extra PNG/XML pairs.
- Important files/symbols: `VisualCatalogSharedComponentsTest`, `VisualCatalogSharedShellTest`, `VisualCatalogTaskComponentsTest`, `TaskBulkSelectionUiTest`, `VisualCatalogCapture.kt`, `scripts/ui-catalog`, and `docs/quality/ui-surface-catalog.tsv`.
- Persistence/migration/history impact: Android-test and quality tooling only. Production application behavior, schema 46, data epoch 6, backup version 24, release identity, and user data are unchanged.
- Compatibility and limitations: Shared and Tasks are exhaustive against the current 171-row catalog. Ninety-one selectors in later families remain pending, so full lint intentionally remains fail-closed. No candidate, signed build, owner-phone query, phone instrumentation, install, or publication occurred.
- Related: `FB-20260906-008`, `DEC-20260906-005`, `IMP-20260906-007`, `VER-20260906-008`.
- Status: Implemented, exact-family captured, and ready for the remaining catalog families.

### IMP-20260906-009 — Complete Habits visual-catalog family

- Behavior changed: Added exact emulator capture ownership for every declared Habit surface: empty/populated workspace pages, all inspector tabs, create/edit forms, template chooser, numeric check-in, timer review, past-log create/edit, pause scheduling, lifecycle actions, impact-aware permanent deletion, and row overflow. Stateful/private surfaces reuse their production behavior tests and the template/menu path runs through the real application; only standalone editors use a deterministic production-component fixture.
- Important files/symbols: `VisualCatalogHabitComponentsTest`, `VisualCatalogPagesTest.captureHabitPageCatalog`, `ActivityHistoryUiTest`, `ProductivityCardDesignUiTest`, `HabitDeletionUiTest`, and `docs/quality/ui-surface-catalog.tsv`.
- Persistence/migration/history impact: Android-test fixtures and catalog metadata only. Production Habit behavior, schema 46, data epoch 6, backup version 24, release identity, and stored user data are unchanged.
- Compatibility and limitations: Habits is exhaustive against the current catalog; 77 later-family selectors remain pending. No candidate, signed build, owner-phone query, phone instrumentation, install, or publication occurred.
- Related: `FB-20260906-008`, `DEC-20260906-005`, `VER-20260906-009`.
- Status: Implemented and exact-family captured; visual critique and remediation remain pending.

### IMP-20260906-010 — Complete Goals visual-catalog family

- Behavior changed: Added exact capture ownership for all declared Goal surfaces. A deterministic production-component journey covers create/edit, progress measurement, active-goal inspector actions, and impact-aware deletion; the established constrained elapsed-reset test owns the reset dialog; the real application owns active/empty/history/archive/insights pages plus overflow and template chooser states.
- Important files/symbols: `GoalSecondaryMutationUiTest.captureGoalComponentCatalog`, `ElapsedGoalTimeUiTest`, `VisualCatalogPagesTest.captureGoalPageCatalog`, and `docs/quality/ui-surface-catalog.tsv`.
- Persistence/migration/history impact: Android-test fixtures and catalog metadata only. Production Goal behavior, schema 46, data epoch 6, backup version 24, release identity, and stored user data are unchanged.
- Compatibility and limitations: Goals is exhaustive against the current catalog; 69 later-family selectors remain pending. No candidate, signed build, owner-phone query, phone instrumentation, install, or publication occurred.
- Related: `FB-20260906-008`, `DEC-20260906-005`, `VER-20260906-010`.
- Status: Implemented and exact-family captured; cross-family visual critique and remediation remain pending.

### IMP-20260906-011 — Complete Tracks visual-catalog family

- Behavior changed: Added exact capture ownership for every declared Track surface: collection/detail pages and missing-target state; definition and entry create/edit forms; entry details; filter and condition dialogs; CSV preview; entry deletion; and collection/activity/entry overflow menus. Production mutation tests own authored/destructive states, while the real `MainActivity` journey owns private workspace dialogs and menus.
- Capture reliability: Popup captures intentionally transition through another production destination before further interaction because the UI-automation hierarchy pass can invalidate popup-window focus. Capture-only tests isolate surfaces whose existing behavior tests later rely on Espresso back handling.
- Important files/symbols: `VisualCatalogPagesTest.captureTrackPageCatalog`, `TrackDefinitionMutationUiTest`, `TrackEntryMutationUiTest`, `TrackCsvImportUiTest`, and `docs/quality/ui-surface-catalog.tsv`.
- Persistence/migration/history impact: Android-test fixtures and catalog metadata only. Production Track behavior, schema 46, data epoch 6, backup version 24, release identity, and stored user data are unchanged.
- Compatibility and limitations: Tracks is exhaustive against the current catalog; 56 Gym/Settings/Organization selectors remain pending. No candidate, signed build, owner-phone query, phone instrumentation, install, or publication occurred.
- Related: `FB-20260906-008`, `DEC-20260906-005`, `VER-20260906-011`.
- Status: Implemented and exact-family captured; cross-family visual critique and remediation remain pending.

### IMP-20260906-012 — Complete Gym visual-catalog family

- Behavior changed: Added deterministic capture ownership for every declared Gym surface: empty/populated workout, History, Progress, Library, Routines, Exercises, Machines, Categories, and Tools pages; Routine Builder outline, pickers, program structure, 5/3/1 setup/review, rep-scheme and quick-machine flows; exercise, machine, workout, tracked-record, program-position, confirmation, category-allocation, and impact-aware deletion dialogs; and every declared Gym overflow menu. Populated page fixtures now include a linked category and machine instead of visually empty content.
- Capture testability: Five stateful production composables are module-visible so instrumentation can render the exact production workout-set, workout-note, exercise-inspector, tracked-record, and routine-position surfaces without duplicating their UI. No production behavior or styling changed.
- Important files/symbols: `GymPowerInputUiTest.captureWorkoutComponentCatalog`, `RoutineBuilderUiTest.captureRepSchemeCatalog`, `VisualCatalogPagesTest.captureGymPageCatalog`, `VisualCatalogPagesTest.captureGymCategoryAllocationCatalog`, `FiveThreeOneCycleReviewUiTest`, `SafetyChoiceUiTest`, `WorkoutDeletionUiTest`, `GymScreens.kt`, and `docs/quality/ui-surface-catalog.tsv`.
- Persistence/migration/history impact: Android-test fixtures, catalog metadata, and Kotlin visibility only. Production behavior, schema 46, data epoch 6, backup version 24, release identity, and stored user data are unchanged.
- Compatibility and limitations: Gym is exhaustive against the current catalog; 22 Settings/Organization selectors remain pending. No candidate, signed build, owner-phone query, phone instrumentation, install, or publication occurred.
- Related: `FB-20260906-008`, `DEC-20260906-005`, `VER-20260906-012`.
- Status: Implemented and exact-family captured; cross-family visual critique and remediation remain pending.

### IMP-20260906-013 — Complete Settings and Organization visual-catalog families

- Behavior changed: Completed capture ownership for Health permission rationale, backup/restore preview, reset, Health-copy deletion, custom-unit, and custom-emoji surfaces; and for every declared Area and Tag manager, detail, create, rename, color, merge, move, permanent-delete, invariant, and menu state. Compact Settings pages remain real-application captures; focused safety and management journeys render production components with deterministic content.
- Important files/symbols: `AreaFeatureUiTest.captureAreaManagementCatalog`, `TagManagementUiTest.captureTagManagementCatalog`, `SafetyChoiceUiTest.replaceEverythingRequiresFinalConfirmationAndBusyBlocksDuplicates`, `SettingsBehaviorUiTest`, `SettingsResponsiveUiTest`, `HealthPermissionsRationaleUiTest`, and `docs/quality/ui-surface-catalog.tsv`.
- Persistence/migration/history impact: Android-test fixtures and catalog metadata only. Production Settings, Area, and Tag behavior; schema 46; data epoch 6; backup version 24; release identity; and stored user data are unchanged.
- Compatibility and limitations: All 171 catalog rows now have concrete capture selectors with zero platform exceptions or pending rows. This milestone proves exact family capture, not yet the final whole-catalog baseline, critique, remediation, or release. No candidate, signed build, owner-phone query, phone instrumentation, install, or publication occurred.
- Related: `FB-20260906-008`, `DEC-20260906-005`, `VER-20260906-013`.
- Status: Implemented and exact-family captured; whole-catalog baseline and design work remain in progress.

### IMP-20260906-014 — Fidelity-checked visual catalog and searchable review gallery

- Behavior changed: The catalog collector now creates a dependency-free searchable/filterable HTML gallery, fixes dark mode for the duration of emulator capture, and restores the emulator's prior night-mode and crash-dialog settings on both success and failure. Accepted capture helpers wait through Compose and rendered-frame boundaries, prove the requested Whip state is present, and reject a hierarchy that loses the Whip package or exposes a crash/ANR sheet. Multi-state and behavior-sensitive journeys were split or synchronized so a correctly named file cannot silently preserve the preceding state.
- Capture fidelity: Component fixtures for Area scope, workout components, Track entry mutation, Health rationale, Safety choices, and Settings use production-sized dark Whip hosts. Search results, bulk Task actions, Area/Tag workflows, Gym menus/dialogs, reset/restore confirmations, and other previously stale transitions now assert exact state before capture. The stock-AVD-only error-sheet setting prevents unrelated Google-process crash sheets from obscuring evidence, while captured overlays still fail closed.
- Important files/symbols: `scripts/ui-catalog`, `scripts/test-ui-catalog`, `VisualCatalogCapture.kt`, affected Android UI capture tests, `docs/quality/ui-surface-catalog.tsv`, and `docs/quality/UI_VISUAL_REVIEW_PROTOCOL.md`.
- Persistence/migration/history impact: Test tooling, test fixtures, evidence protocol, and catalog metadata only. Production behavior, schema 46, data epoch 6, backup version 24, release identity, and user data are unchanged.
- Compatibility and limitations: The collector remains emulator-only and deletes only its exact MediaStore evidence path. Family capture is the fast iteration unit; a whole 171-surface capture is reserved for frozen audit milestones. Candidate qualification remains Play Store-only. No signed build, owner-phone query, phone instrumentation, install, or publication occurred.
- Related: `FB-20260906-008`, `FND-20260906-002`, `DEC-20260906-005`, `VER-20260906-014`.
- Verification: `VER-20260906-014`.
- Status: Implemented, frozen-audit verified, and released in Whip 0.3.52/code 58.

### IMP-20260906-015 — Coherent Task/Goal summaries and elapsed-reset action hierarchy

- Behavior changed: Task Overview and Completed details now contain related outcome, context, timing, notes, deadline, and subtask evidence in the same low-emphasis summary role used across Whip. Goal Overview now separates Outcome and Progress Insight into purposeful summary cards, replaces “Target overlay” with one readable Target fact, and explains that rate/forecast appear as history grows. Empty Goal insight copy now correctly says “1 source type.” The elapsed-reset body presents Reset to Now as an alternative to its date/time selection, leaving Cancel and Reset to Chosen Time as the single footer decision.
- Important files/symbols: `EntityInspectorInformationGroup`, `TaskActionsDialog`, `CompletedTaskDetailsDialog`, `GoalActionsDialog`, `ElapsedGoalResetDialog`, `buildGoalInsights`, focused Task/Goal Android tests, and `GoalRulesTest`.
- Persistence/migration/history impact: Presentation, copy, and UI composition only. Task/Goal repositories, mutations, persisted history, schema 46, data epoch 6, backup version 24, release identity, and user data are unchanged.
- Compatibility and limitations: Existing inspector tabs, scroll/frame behavior, docked primary actions, Task actions, Goal charts/data-table disclosure, timer reset semantics, daylight-saving resolution, and failure/saving ownership remain intact. The affected-change readiness gate passed; the whole 171-surface frozen recapture and final private-phone deployment remain pending. No candidate, signed build, phone query, phone instrumentation, install, or publication occurred.
- Related: `FB-20260906-008`, `FND-20260906-003`, `FND-20260906-004`, `DEC-20260906-006`, `VER-20260906-015`.
- Verification: `VER-20260906-015`.
- Status: Implemented, frozen-audit accepted, and released in Whip 0.3.52/code 58.

### IMP-20260906-016 — Close final Habit and Track popup capture gaps

- Behavior changed: Added state-specific visible-menu assertions and settle boundaries before the Habit row overflow and the Track collection, activity-entry, and detail-entry overflow captures. The Track collection gate uses the invariant Select Tracks action so it remains valid across scoped reorder labels.
- Important files/symbols: `VisualCatalogPagesTest.captureHabitPages`, `VisualCatalogPagesTest.captureTrackPages`, and the four `*.menu` catalog artifacts.
- Persistence/migration/history impact: Android-test capture synchronization only. Production UI/behavior, schema 46, data epoch 6, backup version 24, release identity, and user data are unchanged.
- Compatibility and limitations: Exact Habit and Track family recaptures now show the intended popups in PNG and semantics evidence. A resource-exhausted stock emulator run failed closed on non-Whip hierarchy ownership and was rejected; refreshing only the disposable emulator restored deterministic capture. The final whole-catalog recapture remains pending. No candidate, signed build, owner-phone query, phone instrumentation, install, or publication occurred.
- Related: `FB-20260906-008`, `FND-20260906-002`, `DEC-20260906-005`, `VER-20260906-016`.
- Verification: `VER-20260906-016`.
- Status: Implemented, frozen-audit verified, and released in Whip 0.3.52/code 58.

### IMP-20260906-017 — Signed Whip 0.3.52 whole-UI owner-phone release

- Behavior changed: Advanced Whip to 0.3.52/code 58 and installed the completed whole-UI design remediation in place on the explicitly selected owner phone through the fast private-development lane. The release contains the shared Task/Goal inspector-information role, corrected Goal copy, elapsed-reset hierarchy, and the reusable 171-surface review system in source.
- Important files/symbols: `app/build.gradle.kts`, `EntityInspectorInformationGroup`, `ElapsedGoalResetDialog`, `scripts/ui-catalog`, signed `app-release.apk`, local `app-release.aab`, and `VER-20260906-018`.
- Persistence/migration/history impact: Schema 46, data epoch 6, and exact-match backup version 24 are unchanged. Android first-install identity and existing app data were preserved; no reset, clear, uninstall, downgrade, or fresh-start confirmation occurred.
- Compatibility and limitations: This private release uses the accepted focused checks and frozen 171-surface audit plus release-vital lint, R8/resource optimization, signed build/install, and physical smoke. It is not Play Store candidate evidence; public publication still requires one fresh `scripts/candidate` run.
- Commit/push: Release source `957bf64` was pushed to `origin/main` before the signed build and deployment.
- Related: `FB-20260906-008`, `FND-20260906-002`, `FND-20260906-003`, `FND-20260906-004`, `DEC-20260906-005`, `DEC-20260906-006`, `VER-20260906-017`, `VER-20260906-018`.
- Verification: `VER-20260906-018`.
- Status: Released and device-verified as Whip 0.3.52/code 58; awaiting real-use feedback.

### IMP-20260906-018 — First-class multi-unit Count Time Since display

- Behavior changed: Replaced the scalar elapsed-display choice with `ElapsedDisplayFormat`: Automatic or any non-empty canonical combination of Years, Months, Weeks, Days, Hours, and Minutes. Authored combinations decompose calendar-aware human time in Whip's active zone while preserving the exact event instant; all selected components, including zeros, remain visible. The Goal editor now uses a coherent multi-select configuration card with live preview and explicit always-visible scope. Collapsed and reorder Goal cards render atomic wrapping metric components, while Home, adaptive support, Insights, Overview, terminal snapshots, and reset-adjacent views share the same configured value.
- Persistence/migration/history impact: Room remains schema 46 and existing `Auto`, `Minutes`, `Hours`, `Days`, `Weeks`, and `Years` rows decode losslessly. New values use `Selected:` plus ordered unit identities inside the existing text column, avoiding an owner-data migration or reset. Exact-match portable backup advances to version 25 and validates/restores the same codec; elapsed display changes still create no progress history and do not alter outcome semantics.
- QA/tooling changed: Added focused calendar/codec, editor/save, 320 dp/200%-text card, repository legacy-row, and composite backup coverage; expanded the visual catalog to 172 states with a dedicated elapsed-editor surface and representative first-class elapsed Goal card; repaired both stale root-package selectors in the supported Goals QA profile.
- Important files/symbols: `ElapsedDisplayFormat`, `elapsedDisplay`, `GoalProjection.elapsedDisplayValue`, `ElapsedGoalPrimaryStatus`, `GoalEditorDialog`, `GoalRepository`, `BackupRepository`, `ElapsedGoalTimeUiTest`, `VisualCatalogPagesTest`, `docs/quality/ui-surface-catalog.tsv`, and `scripts/qa-targeted`.
- Compatibility and limitations: Automatic intentionally retains the existing single best-fit duration behavior. Calendar composites use the current Whip zone, while the saved start remains one exact instant. No complete candidate suite or Play Store qualification was run; the requested private-phone release remains a separate fast deployment step.
- Related: `FB-20260906-009`, `FND-20260906-005`, `FND-20260906-006`, `DEC-20260906-007`, `VER-20260906-019`.
- Verification: `VER-20260906-019`.
- Status: Implemented, focused-tested, readiness-verified, visually accepted, and released in Whip 0.3.53/code 59.

### IMP-20260906-019 — Signed Whip 0.3.53 multi-unit elapsed-display owner-phone release

- Behavior changed: Advanced Whip to 0.3.53/code 59 and installed the first-class Count Time Since display in place on the explicitly selected owner phone through the fast private-development lane. Authors can retain Automatic or choose any combination of Years, Months, Weeks, Days, Hours, and Minutes, with the configured duration kept prominent across Goal collection and detail surfaces.
- Persistence/migration/history impact: Schema 46 and data epoch 6 remain unchanged; the exact-match backup contract is version 25. Legacy elapsed-display strings decode in place, Android first-install identity was preserved, and no reset, clear, uninstall, downgrade, or fresh-start confirmation occurred.
- Compatibility and limitations: The release uses the accepted focused Goals checks, 14-surface visual review, affected readiness gate, release-vital lint, signed artifact build/install, and physical smoke. It is a private owner-phone development release, not Play Store candidate evidence; public publication still requires one fresh `scripts/candidate` run.
- Commit/push: Feature commit `1e940cd` and release source `a23fb6b` were pushed to `origin/main` before the signed build and deployment.
- Related: `FB-20260906-009`, `FND-20260906-005`, `DEC-20260906-007`, `IMP-20260906-018`, `VER-20260906-019`, `VER-20260906-020`.
- Verification: `VER-20260906-020`.
- Status: Released and device-verified as Whip 0.3.53/code 59; awaiting real-use feedback.

### IMP-20260906-020 — Integrated elapsed metric and quiet app-action success

- Behavior changed: Replaced the bolt-on elapsed treatments with one responsive `ElapsedGoalMetric` used by collection/reorder cards, Home-derived cards, Insights, editor preview, and Goal Overview. Numeric values use modest medium emphasis while unit labels use the surrounding supporting role; value/unit pairs wrap atomically and expose one merged full-duration accessibility label. Expanded cards now show start/terminal context instead of a duplicate counter, and elapsed Overview uses one information group rather than repeating a Progress card.
- Feedback changed: Added one shared success-feedback classifier: routine committed actions whose new state is already visible are consumed inline, while failures, post-commit warnings, and meaningful token-owned Undo/Edit/Retry actions retain transient bars. Applied the policy across Goal, Habit, Task, Track, Gym, Area, Tag, and Track-entry mutation paths, including authored saves, creates, logs, pins, restores, catalog mutations, and non-recoverable deletion confirmations. Android reminders, alarms, permission UI, and ongoing/system notifications are unchanged.
- Regression coverage: Added the classifier truth table and source-architecture guard, strengthened Goal editor/view-model tests, removed the duplicate elapsed detail assertion, retained large-text/all-unit coverage, and extended the real app-shell journey to create a Goal and prove no “Goal created” bar appears after persistence.
- Persistence/migration/history impact: Presentation and transient-feedback behavior only. Goal arithmetic and selected-unit persistence, schema 46, data epoch 6, backup version 25, release identity, and owner data are unchanged.
- Compatibility and limitations: Failure visibility and exact recovery-token ownership remain intact. Focused development checks and the Goals visual family are accepted, but no complete suite, candidate qualification, signed release build, phone-selected command, phone install, or Play Store publication occurred.
- Commit/push: Implementation source `7ab2255` was pushed to `origin/main` before final memory reconciliation.
- Related: `FB-20260906-010`, `FND-20260906-007`, `FND-20260906-008`, `DEC-20260906-008`, `DEC-20260906-009`, `VER-20260906-021`.
- Verification: `VER-20260906-021`.
- Status: Implemented, readiness-verified, visually accepted, and released in Whip 0.3.54/code 60; awaiting real-use feedback.

### IMP-20260906-021 — Signed Whip 0.3.54 integrated-metric owner-phone release

- Behavior changed: Advanced Whip to 0.3.54/code 60 and installed the integrated Count Time Since metric plus quiet routine-action feedback in place on the explicitly selected owner phone through the fast private-development lane.
- Important files/symbols: `app/build.gradle.kts`, implementation source `7ab2255`, release source `2de0170`, signed `app-release.apk`, local `app-release.aab`, and `VER-20260906-022`.
- Persistence/migration/history impact: Schema 46, data epoch 6, and exact-match backup version 25 are unchanged. Android first-install identity and existing app data were preserved; no reset, clear, uninstall, downgrade, or fresh-start confirmation occurred.
- Compatibility and limitations: The release reuses the accepted focused behavior/UI checks and Goals-family evidence, then adds release-vital lint, R8/resource optimization, signed artifact build/install, and physical smoke. It is a private owner-phone development release, not Play Store candidate evidence; public publication still requires one fresh `scripts/candidate` run.
- Commit/push: Implementation source `7ab2255` and release source `2de0170` were pushed to `origin/main` before the signed build and deployment.
- Related: `FB-20260906-010`, `FB-20260906-011`, `FND-20260906-007`, `FND-20260906-008`, `DEC-20260906-003`, `DEC-20260906-008`, `DEC-20260906-009`, `IMP-20260906-020`, `VER-20260906-021`, `VER-20260906-022`.
- Verification: `VER-20260906-022`.
- Status: Released and device-verified as Whip 0.3.54/code 60; awaiting real-use feedback.

### IMP-20260906-022 — Whole-app accessibility and fail-closed UX evidence closure

- Behavior changed: Added one shared labeled-switch semantics modifier and applied it to standalone Task scheduling/progress/completion/reminder switches plus the Habit checklist auto-complete switch. Screen readers now receive each setting name and explicit On/Off state without altering the accepted visual hierarchy, layout, or interaction behavior.
- QA/evidence changed: Made the 172-surface catalog reject any `NAF="true"` interactive node; centralized its exact MediaStore cleanup/pull behind the emulator-only device-artifact owner; corrected the elapsed-Goal E2E reference, current backup-version tests, elapsed-card accessibility assertion, release-version fixture, and documented 95-class runner topology. The shared Android engine now wakes/unlocks each batch, suppresses pre-existing unrelated application-error sheets for the scoped campaign, and restores the prior emulator setting on exit.
- Important files/symbols: `whipLabeledSwitchSemantics`, `TaskEditorDialog`, `HabitEditorDialog`, `VisualCatalogCapture`, `scripts/ui-catalog`, `scripts/device-artifacts`, `scripts/android-test-engine`, `docs/quality/e2e-coverage.tsv`, and focused harness/UI regressions.
- Persistence/migration/history impact: Presentation semantics, regression tests, and emulator-only QA tooling. Schema 46, data epoch 6, exact-match backup version 25, repository behavior, and owner data are unchanged. Release identity advances to Whip 0.3.55/code 61 for the authorized in-place private deployment.
- Compatibility and limitations: Visual pixels are unchanged except the About-version line. Catalog operations remain forbidden on physical hardware; instrumentation remains emulator-only. This exhaustive private-development evidence is not a Play Store-qualified frozen candidate, and public publication still requires `scripts/candidate`.
- Related: `FB-20260906-012`, `FND-20260906-009` through `FND-20260906-015`, `DEC-20260906-010`, `VER-20260906-023`.
- Verification: `VER-20260906-023`.
- Commit/push: Verified release source `001caf8` was pushed to `origin/main` before the signed build and owner-phone deployment.
- Status: Implemented, complete-suite verified, semantically verified, visually accepted, and released in Whip 0.3.55/code 61.

### IMP-20260906-023 — Signed Whip 0.3.55 whole-app audit owner-phone release

- Behavior changed: Installed the whole-app accessibility and fail-closed evidence closure in place on the explicitly selected owner phone. Standalone Task/Habit editor switches now announce their setting identity and On/Off state; accepted visual behavior remains otherwise unchanged.
- Important files/symbols: Release source `001caf8`, `app/build.gradle.kts`, signed `app-release.apk`, local `app-release.aab`, and `VER-20260906-024`.
- Persistence/migration/history impact: Schema 46, data epoch 6, and exact-match backup version 25 are unchanged. Android first-install identity and existing owner data were preserved; no reset, clear, uninstall, downgrade, or fresh-start confirmation occurred.
- Compatibility and limitations: The release follows the unusually comprehensive fresh 1,575-test and 172-surface private-development evidence in `VER-20260906-023`, then adds release-vital lint, signed build/install, and physical smoke. It is still not Play Store candidate evidence; public publication requires a fresh `scripts/candidate` run.
- Commit/push: Release source `001caf8` was clean and synchronized with `origin/main` before signed build/install; the following reconciliation commit changes memory only.
- Related: `FB-20260906-012`, `FND-20260906-011`, `DEC-20260906-010`, `IMP-20260906-022`, `VER-20260906-023`, `VER-20260906-024`.
- Verification: `VER-20260906-024`.
- Status: Released and device-verified as Whip 0.3.55/code 61; awaiting normal real-use feedback.

### IMP-20260906-024 — Gym and 5/3/1 end-to-end UX and evidence closure

- Behavior changed: The 5/3/1 setup status now follows feasible dependencies: an empty standard layout directs the user to create its missing Weight + Reps exercises or choose a custom layout, while an empty custom layout asks for one active Weight + Reps exercise. The disabled Build Program action exposes the same blocking reason to accessibility services. Cycle review now restates every Standard, Suggestion, Hold, Ignore, or Custom choice as a live textual decision with its recorded meaning and resulting Training Max.
- QA/evidence changed: Added one real `MainActivity` journey that seeds the standard exercises, opens Gym, builds and saves a four-day 5/3/1 routine, observes the persisted structured program, starts its next day, and verifies generated cycle/week/day and Squat prescription context in the active workout. Expanded the source-linked catalog from 172 to 174 states and the Gym family from 44 to 46 states with distinct blocked setup and active 5/3/1 workout captures; corrected ready-setup and Program Structure fixtures to depict configured and genuine four-phase states.
- Important files/symbols: `FiveThreeOneProgramSetupDialog`, `FiveThreeOneCycleReviewDialog`, `GymFiveThreeOneJourneyE2ETest`, `RoutineBuilderUiTest`, `FiveThreeOneCycleReviewUiTest`, `docs/quality/e2e-coverage.tsv`, `docs/quality/ui-surface-catalog.tsv`, and `scripts/qa-targeted`.
- Persistence/migration/history impact: Guidance, accessibility semantics, presentation, tests, and QA metadata only. Routine generation, Training Max decisions, Room schema 46, data epoch 6, exact-match backup version 25, and existing owner data are unchanged. Release identity advances to Whip 0.3.56/code 62 for the authorized in-place private deployment.
- Compatibility and limitations: The final fast Gym/5/3/1 profile, exact Gym-family gallery, catalog semantics guard, and affected readiness tier passed. No complete fresh suite, candidate qualification, physical instrumentation, phone reset, uninstall, downgrade, or Play Store publication occurred.
- Related: `FB-20260906-013`, `FND-20260906-016`, `FND-20260906-017`, `FND-20260906-018`, `DEC-20260906-011`, `VER-20260906-025`.
- Verification: `VER-20260906-025`.
- Status: Implemented, focused-tested, semantically verified, visually accepted, and released in Whip 0.3.56/code 62.

### IMP-20260906-025 — Signed Whip 0.3.56 Gym/5/3/1 owner-phone release

- Behavior changed: Advanced Whip to 0.3.56/code 62 and installed the end-to-end Gym/5/3/1 UX closure in place on the explicitly selected owner phone through the fast private-development lane. First-run setup guidance, disabled-action semantics, textual cycle decisions, and the verified generated-program workflow are now in normal-use release code.
- Important files/symbols: Release source `9006233`, `app/build.gradle.kts`, `FiveThreeOneProgramSetupDialog`, `FiveThreeOneCycleReviewDialog`, signed `app-release.apk`, local `app-release.aab`, and `VER-20260906-026`.
- Persistence/migration/history impact: Schema 46, data epoch 6, and exact-match backup version 25 are unchanged. Android first-install identity and existing owner data were preserved; no reset, clear, uninstall, downgrade, or fresh-start confirmation occurred.
- Compatibility and limitations: The release follows the accepted focused 170-test Gym/5/3/1 profile, 46-state Gym visual/semantics review, and affected readiness tier, then adds release-vital lint, R8/resource optimization, signed build/install, and physical smoke. It is a private owner-phone development release, not Play Store candidate evidence; public publication still requires a fresh `scripts/candidate` run.
- Commit/push: Release source `9006233` was clean and synchronized with `origin/main` before signed build/install; the following reconciliation commit changes memory only.
- Related: `FB-20260906-013`, `FND-20260906-016`, `FND-20260906-017`, `FND-20260906-018`, `DEC-20260906-011`, `IMP-20260906-024`, `VER-20260906-025`, `VER-20260906-026`.
- Verification: `VER-20260906-026`.
- Status: Released and device-verified as Whip 0.3.56/code 62; awaiting normal real-use feedback.

### IMP-20260907-001 — Bounded objective-AMRAP 5/3/1 adaptive review

- Behavior changed: Replaced the first higher-suggestion heuristic with progression engine version 2. Canonical 5/3/1 Standard still returns exactly the saved per-exercise increase. When the explicit above-standard option is enabled, the engine now requires the two most recent qualifying PR/AMRAP exposures from separate sessions, actual performance at 85–110% of the snapshotted Training Max with at least one exposure at 90% or more, two or more reps beyond prescription, conservative minimum Epley/Brzycki estimates with repetitions capped at 12, support for the proposed next Training Max at a 90% e1RM ceiling, and no later capacity drop beyond 5%. Objective evidence alone is capped at 1.25× Standard; 1.5× additionally requires favorable RPE/RIR on both qualifying AMRAPs or a Joker at/above current Training Max that convincingly beats its target without marginal/contradictory effort evidence.
- Integrity changed: Session start time is carried into both UI-preview and transaction-authoritative evidence so “recent” and regression checks are deterministic. Existing missing/deleted/ambiguous/failed required-work, repeated-miss, failed Training Max test, Hold, decrease, and lower-increase priorities remain ahead of adaptive logic. When both RPE and RIR exist, both must corroborate favorable effort. A single AMRAP, Joker, or Training Max test cannot unlock acceleration.
- UX/design changed: Renamed the user-facing mode to “Adaptive review · non-standard,” consistently retained “5/3/1 standard · recommended,” and exposed the above-standard opt-in during initial setup as well as Program Structure. The cycle dialog states the canonical/adaptive boundary, defaults to Standard, uses a neutral advisory badge rather than error color, and separates evidence strength from scannable rationale lines.
- Regression coverage: Expanded pure rule-table/counterexample coverage for rep-only, effort-corroborated, strong/weak/grinder Joker, separate-session, intensity, conservative-capacity, timestamp, and regression boundaries; protected setup-to-draft persistence; changed repository audit expectation to the engine constant; and made the catalog's cycle-review fixture depict the 1.25× adaptive decision.
- Persistence/migration/history impact: No Room schema, data epoch, or backup-format change. Existing routines retain their progression mode and opt-in value; existing cycle-decision history retains its recorded engine version and rationale. Only new reviews use engine version 2.
- Compatibility and limitations: The adaptive mode remains advisory, opt-in, and user-confirmed. It does not infer recovery/readiness, and deliberately prefers conservative false negatives. The final review additionally prevents corroboration from selecting the 1.5× tier unless the conservative estimates support that exact next Training Max. Release identity is staged as Whip 0.3.57/code 63; Play Store candidate qualification remains out of scope for this private owner-phone release.
- Related: `FB-20260907-001`, `FND-20260907-001`, `FND-20260907-002`, `FND-20260907-003`, `DEC-20260907-001`, `VER-20260907-001`.
- Verification: `VER-20260907-001`.
- Status: Implemented, code-reviewed, emulator-accepted, and released in Whip 0.3.57/code 63 through `IMP-20260907-002`.

### IMP-20260907-002 — Signed Whip 0.3.57 adaptive 5/3/1 owner-phone release

- Behavior changed: Advanced Whip to 0.3.57/code 63 and installed progression engine version 2 plus the Adaptive review decision UX in place on the explicitly selected owner phone through the fast private-development lane. Canonical Standard remains the default; the two bounded optional tiers and their conservative evidence explanations are now available for normal owner use.
- Important files/symbols: Release source `59b42fc`, `app/build.gradle.kts`, `FiveThreeOneProgression`, `FiveThreeOneProgramSetupDialog`, `FiveThreeOneCycleReviewDialog`, signed `app-release.apk`, local `app-release.aab`, and `VER-20260907-002`.
- Persistence/migration/history impact: Room schema 46, data epoch 6, and exact-match backup version 25 are unchanged. Android first-install identity and existing owner data were preserved; no reset, clear, uninstall, downgrade, or fresh-start confirmation occurred.
- Compatibility and limitations: The release follows the release-stamped 344-JVM/572-Android readiness gate and exact 46-surface Gym gallery, then adds release-vital lint, R8/resource optimization, signed build/install, and physical smoke. It is a private owner-phone development release, not Play Store candidate evidence; public publication still requires a fresh `scripts/candidate` run.
- Commit/push: Release source `59b42fc` was clean and synchronized with `origin/main` before signed build/install; the following reconciliation commit changes product memory only.
- Related: `FB-20260907-001`, `FND-20260907-001`, `FND-20260907-002`, `FND-20260907-003`, `DEC-20260907-001`, `IMP-20260907-001`, `VER-20260907-001`, `VER-20260907-002`.
- Verification: `VER-20260907-002`.
- Status: Released and device-verified as Whip 0.3.57/code 63; awaiting normal real-use feedback.

### IMP-20260907-003 — Whole-product state-truth and action-hierarchy cleanup

- Behavior changed: New Track Entry editors now lead with the parent Track name and identify the work as a new Entry instead of fabricating `New <Field>` identity text. Existing Entry editors retain their saved composite identity. Archived Track summaries omit the unavailable add control while preserving open, expand, selection, restore, and history behavior.
- Hierarchy changed: Area detail status and usage moved into the selected-destination header, and the repeated body identity block was removed so Identity actions begin in the first viewport. Reminder status refresh now uses Whip's full-width secondary action grammar with a stable semantic test identity. The Area scope popup anchor explicitly wraps the visible chip, preventing inherited full-screen minimum constraints from detaching its menu.
- Regression coverage: Added new-entry hierarchy assertions, archived-action absence coverage, and a bounded Area-menu anchor regression. Existing Area-management catalog behavior remains asserted. No domain mutation, repository, persistence, notification-delivery, Track schema, archive lifecycle, or Area operation changed.
- Persistence/migration/history impact: Presentation, semantics, and popup geometry only. Room schema 46, data epoch 6, exact-match backup version 25, existing owner data, and release identity remain unchanged at this implementation boundary.
- Compatibility and limitations: The implementation preserves compact/adaptive app-shell behavior and all domain-specific workflows. Focused emulator tests and the three affected visual families are accepted; final whole-product recapture, release stamping, signed build, and phone deployment remain separate acceptance steps.
- Commit/push: Product source `bac0dec` was pushed to `origin/main` before this memory reconciliation.
- Related: `FB-20260907-002`, `FND-20260907-004` through `FND-20260907-008`, `DEC-20260907-002`, `VER-20260907-003`.
- Verification: `VER-20260907-003`.
- Status: Implemented, code-reviewed, focused-tested, semantically verified, and visually accepted across affected families.

### IMP-20260907-004 — Coarse-clock-safe fast Android evidence freshness

- Behavior changed: The shared Android test engine still deletes exact prior result/coverage outputs and requires strict `find -newer` freshness, but now moves the new invocation marker one second into the past before Gradle starts. Current near-instant output is accepted even on a coarse filesystem clock, while intentionally stale year-2000 XML/coverage fixtures remain rejected.
- QA changed: The target-guard source contract now requires the timestamp margin. Its complete synthetic coverage, reusable-cache, selective invalidation, corrupt-cache, class-mismatch, zero/failure/skip, multiple/empty coverage, stale-result, physical-target, and release-version campaign passes without adding any sleep to the 11-batch loop.
- Persistence/migration/history impact: Test harness only. App behavior, release artifacts, Room schema 46, data epoch 6, backup version 25, and owner data are unchanged.
- Compatibility and limitations: The safety argument depends on exact pre-run output cleanup, which remains in the same guarded batch function. The strict freshness predicate was not weakened and physical instrumentation remains rejected.
- Commit/push: Harness source `be084e1` was pushed to `origin/main` before this memory reconciliation.
- Related: `FB-20260907-002`, `FND-20260907-009`, `DEC-20260906-003`, `DEC-20260906-005`, `DEC-20260906-010`, `VER-20260907-004`.
- Verification: `VER-20260907-004`.
- Status: Implemented, shell-validated, fixture-verified, and ready for the final release-stamped acceptance gate.

### IMP-20260907-005 — Clean-tree emulator readiness no-op routing

- Behavior changed: `scripts/check` now forwards `--emulator` to the targeted runner only when a runnable profile or selector remains after filtering the `docs` sentinel. Clean/doc-only readiness still performs the explicit emulator target guard, diff check, and readiness completion, but it no longer invokes `qa-targeted` with an empty request.
- Regression coverage: `scripts/test-check-fast` now creates a guarded emulator fixture, commits its earlier production edit to produce a genuinely clean tree, runs `check --ready --emulator`, proves no Gradle/test work ran, and asserts both the no-change route and successful readiness result. Existing fast, explicit readiness, candidate-boundary, full-gate, and router fixtures remain green.
- Persistence/migration/history impact: Shell routing only. App behavior, artifacts, version identity, Room schema 46, data epoch 6, backup version 25, and owner data are unchanged.
- Compatibility and limitations: Nonempty emulator routes, `--all-android`, `--fresh-emulator`, static readiness, and candidate-required reporting retain their existing behavior. The clean route is a no-op only because accepted evidence was already produced before the source became clean and pushed.
- Commit/push: Routing source `36f2d14` was pushed to `origin/main` before this memory reconciliation.
- Related: `FB-20260907-002`, `FND-20260907-010`, `DEC-20260906-003`, `VER-20260907-005`.
- Verification: `VER-20260907-005`.
- Status: Implemented, shell-validated, and routing-fixture verified.

### IMP-20260907-006 — Render-bound visual-catalog evidence

- Behavior changed: Catalog capture now primes the device screenshot path, forces and awaits an actual draw from the single resumed Activity, and crosses subsequent display-frame boundaries before exporting pixels. Callers may require a new surface to differ materially from an earlier captured surface; the helper samples app content while excluding dynamic system bars, retries rejected frames for a bounded 600 ms, and fails instead of accepting stale pixels.
- Gym evidence changed: Every Gym Library child now asserts its exact visible page title before capture and must be visually distinct from the captured Library landing page. This converts the observed Tools mismatch from a manual discovery into an executable pixel-state contract.
- Persistence/migration/history impact: Android test and visual-evidence tooling only. App behavior, artifacts, Room schema 46, data epoch 6, backup version 25, and owner data are unchanged.
- Compatibility and limitations: Pixel distinction is intentionally opt-in for transitions with a known different reference; ordinary captures retain render synchronization and hierarchy guards without making arbitrary visual-difference assumptions. Dynamic status/navigation bars are excluded from the fingerprint but remain present in exported device screenshots.
- Commit/push: Render/state source `e4ed209` and bounded retry correction `7f2b3e2` were pushed to `origin/main` before this memory reconciliation.
- Related: `FB-20260907-002`, `FND-20260906-002`, `FND-20260907-011`, `FND-20260907-012`, `DEC-20260906-005`, `VER-20260907-006`.
- Verification: `VER-20260907-006`.
- Status: Implemented, code-reviewed, exact-tested, and accepted across the complete Gym catalog family.

### IMP-20260907-007 — Signed Whip 0.3.58 whole-product UX owner-phone release

- Behavior changed: Advanced Whip to 0.3.58/code 64 and installed the cohesive UX cleanup in place on the explicitly selected owner phone. New Track Entries identify their parent Track, archived Track rows omit an impossible add action, Area detail removes repeated identity, Area popups remain attached to their trigger, and Reminder status refresh uses the established secondary-action grammar.
- QA/release changed: The private lane also includes coarse-clock-safe Android result freshness, clean-tree emulator readiness, and render-bound catalog evidence with an executable Gym page-pixel distinction. These change development confidence, not production behavior.
- Persistence/migration/history impact: Room schema 46, data epoch 6, and exact-match backup version 25 are unchanged. Android first-install identity and existing owner data were preserved; no reset, clear, uninstall, downgrade, or fresh-start confirmation occurred.
- Compatibility and limitations: The release follows proportionate whole-product private-development evidence and release-vital lint/R8/resource optimization. It is not a frozen Play Store candidate; public publication still requires a fresh `scripts/candidate` run. Subjective aesthetic acceptance remains open to normal owner-use feedback.
- Commit/push: Clean release source `0eca86d` was synchronized with `origin/main` before signed build/install; the following reconciliation changes memory only.
- Related: `FB-20260907-002`, `FND-20260907-004` through `FND-20260907-012`, `DEC-20260907-002`, `IMP-20260907-003` through `IMP-20260907-006`, `VER-20260907-007`, `VER-20260907-008`.
- Verification: `VER-20260907-007`, `VER-20260907-008`.
- Status: Released and device-verified as Whip 0.3.58/code 64; awaiting normal real-use feedback.

### IMP-20260907-008 — Explicit two-emulator Android QA scheduling and evidence isolation

- Behavior changed: Added `scripts/android-emulator-set` as the single maximum-two target authority. Existing `ANDROID_SERIAL` commands remain valid; setting `WHIP_ANDROID_SECONDARY_SERIAL` opts into one matching disposable emulator. The shared Android engine builds once, gives each worker an invocation-owned AGP output slot, splits small targeted selections when useful, runs graphics first and reset last on primary, balances ordinary batches across both workers, publishes cache rows atomically, and assembles the final aggregate only after every worker succeeds.
- Evidence/release changed: Check, targeted QA, coverage, candidate creation, and UI-catalog capture now validate the same explicit set. Candidate format v2 records emulator count and an ordered-set hash while verification retains v1 compatibility. Catalog capture prepares/restores both emulators, pulls their collections independently, rejects duplicate artifact names, and performs the existing exact accounting after merge. Usage and testing documentation now describe the opt-in lane and current 1,581-test/96-class inventory.
- Important files/symbols: `scripts/android-emulator-set`, `scripts/android-test-engine`, `WHIP_ANDROID_TEST_SLOT` in `app/build.gradle.kts`, `scripts/candidate`, `scripts/ui-catalog`, `scripts/device-artifacts`, and the target/candidate/check/catalog fixtures.
- Persistence/migration/history impact: Test/release harness and documentation only. Whip production behavior, Room schema 46, data epoch 6, exact-match backup version 25, 0.3.58/code 64 release identity, installed app, and owner data are unchanged.
- Compatibility and limitations: Maximum concurrency is two and remains operator-selected; Whip does not auto-discover targets. The emulators must match to keep device-dependent evidence comparable. Graphics and reset boundaries stay serial by design, so the speedup applies to independent ordinary batches. A full frozen Play Store candidate was not created because no store release was requested.
- Commit/push: Harness source `3711d5b` was pushed to `origin/main`; the following reconciliation changes memory only.
- Related: `FB-20260907-003`, `FND-20260907-013`, `DEC-20260907-003`, `VER-20260907-009`.
- Verification: `VER-20260907-009`.
- Status: Verified.

### IMP-20260907-009 — Unified unconstrained elapsed Goal presentation

- Behavior changed: Active Count Time Since cards now keep the authored multi-unit counter visible below the identity/action row in collapsed, expanded, and reorder states. The book-fold support pane uses the same responsive value/unit composition instead of a separator-delimited scalar string, so side-by-side timers share one visual and spoken grammar while wrapping only for their actual pane widths.
- Shared UI changed: `ProductivityItemHeader` now offers a reusable full-width persistent-summary slot without changing existing compact scalar summaries. `NavigationRow` now accepts mutually exclusive scalar or structured supporting content. `ElapsedGoalMetric` owns width and merged accessibility semantics wherever it is used.
- Regression coverage: The fold regression renders two real Goal cards plus their support rows, verifies identical timer semantics, rejects scalar fallback, checks full-width/action-lane geometry, and contributes `goals.elapsed.book-fold` to the exact visual catalog. Existing card coverage now proves the timer survives expansion while Reset and milestone behavior remain intact.
- Persistence/migration/history impact: Presentation and test/catalog coverage only. Elapsed start instants, authored display selections, calendar decomposition, reset behavior, terminal history, Room schema 46, data epoch 6, backup version 25, and existing user data are unchanged.
- Compatibility and limitations: The shared card shell was retained because its structure is sound once rich status has a proper slot. Other entity families keep their existing hierarchy unless they explicitly adopt the new slot. This source is not installed on the owner phone; installed release identity remains Whip 0.3.58/code 64.
- Commit/push: Product source `a8c430e` was pushed to `origin/main`; the following reconciliation changes product memory only.
- Related: `FB-20260907-004`, `FND-20260907-014`, `DEC-20260907-004`, `VER-20260907-010`.
- Verification: `VER-20260907-010`.
- Status: Verified.

### IMP-20260907-010 — Signed Whip 0.3.59 elapsed-timer owner-phone release

- Behavior changed: Advanced Whip to 0.3.59/code 65 and installed the unified elapsed Goal presentation in place on the explicitly selected owner phone through the fast private-development lane. Main cards and book-fold support rows now use one responsive timer grammar, and rich card status remains outside the title/action lane.
- Important files/symbols: Release source `b4e2a53`, `app/build.gradle.kts`, `ProductivityItemHeader.persistentSummaryContent`, `NavigationRow.supportingContent`, `ElapsedGoalMetric`, signed `app-release.apk`, and `VER-20260907-011`.
- Persistence/migration/history impact: Room schema 46, data epoch 6, and exact-match backup version 25 are unchanged. The existing Android installation and owner data were preserved; first-install identity did not change, and no reset, clear, uninstall, downgrade, or fresh-start confirmation occurred.
- Compatibility and limitations: The release follows the broad two-emulator acceptance in `VER-20260907-010` plus a version-aware JVM check, release-vital lint, R8/resource optimization, signing, in-place installation, and independent phone smoke. This is a private owner-phone release, not frozen Play Store candidate evidence.
- Commit/push: Clean release source `b4e2a53` was synchronized with `origin/main` before signed build/install; the following reconciliation changes product memory only.
- Related: `FB-20260907-004`, `FB-20260907-005`, `FND-20260907-014`, `DEC-20260907-004`, `IMP-20260907-009`, `VER-20260907-010`, `VER-20260907-011`.
- Verification: `VER-20260907-011`.
- Status: Released.

### IMP-20260907-011 — One title-column grammar across productivity cards

- Behavior changed: Task, Habit, Goal, and ordinary Track collection cards now read through the same identity → title/support → disclosure → primary-action hierarchy. A reusable logical 44 dp identity gutter aligns persistent status, metadata, Area context, and expanded identity-owned evidence under the title rather than the card edge or a locally guessed inset. Equivalent supporting copy uses one `bodySmall`/secondary-color role, while Task schedule chips retain categorical containment with reduced `labelSmall` emphasis.
- Domain changes: Task schedule/repeat metadata and notes now use the shared header contract; Habit and nonelapsed Goal status share the support role; elapsed Goal metrics remain persistent but move from the emoji edge to the title edge; ordinary Track summaries adopt `ProductivityItemHeader`, place disclosure before Add, and use semibold titles. Track selection/reorder, Activity, and Entry structures retain their domain interactions while matching the same title/support typography and alignment.
- Regression coverage: Added cross-domain title/status geometry contracts, elapsed-title alignment, Track action-order/alignment checks, and a direct shared-header compact/expanded contract. Hardened the Habit timer-review catalog fixture to wait for the entered value and enabled Stop & Log action before committing, removing a UI-synchronization race without changing production behavior.
- Persistence/migration/history impact: Presentation, semantics, and tests only. Card actions and domain mutations are unchanged; Room schema 46, data epoch 6, exact-match backup version 25, release identity, and existing user data are unchanged at this boundary.
- Compatibility and limitations: The established medium card shell remains because the defect was internal information geometry, not container shape. Full-width progress, charts, authored evidence, selection/reorder controls, and responsive wrapping remain intentional exceptions. The preserved `TrackRow.compact` internal argument no longer changes hierarchy but remains source-compatible with direct component tests.
- Commit/push: Accepted implementation source `b87d9ff` was pushed to `origin/main`; release identity was then advanced separately in clean source `eb6a17a`.
- Related: `FB-20260907-006`, `FND-20260907-015`, `DEC-20260907-005`, `VER-20260907-012`.
- Verification: `VER-20260907-012`.
- Status: Implemented, code-reviewed, emulator-accepted, visually verified, and released through `IMP-20260907-012`.

### IMP-20260907-012 — Signed Whip 0.3.60 card-hierarchy owner-phone release

- Behavior changed: Advanced Whip to 0.3.60/code 66 and installed the unified Task, Habit, Goal, and Track card reading grid in place on the explicitly selected owner phone through the fast private-development lane. Equivalent card information now shares title-column alignment, title/support typography, disclosure/action order, and expanded-context placement in normal use.
- Important files/symbols: Release source `eb6a17a`, implementation source `b87d9ff`, `app/build.gradle.kts`, `ProductivityItemHeader`, `ProductivityItemAlignedColumn`, `ProductivityItemSupportingText`, Task/Habit/Goal/Track card call sites, signed `app-release.apk`, local `app-release.aab`, and `VER-20260907-013`.
- Persistence/migration/history impact: Room schema 46, data epoch 6, and exact-match backup version 25 are unchanged. Android first-install identity and the existing installation were preserved; no reset, clear, uninstall, downgrade, fresh-start confirmation, or physical instrumentation occurred.
- Compatibility and limitations: This private owner-phone release uses the accepted 344-JVM/574-Android two-emulator gate and fresh exact 100-surface affected-family review, followed by release-vital lint, R8/resource optimization, signed artifact validation, in-place install, and independent phone smoke. It is not a frozen Play Store candidate; normal use remains the final subjective aesthetic validation.
- Commit/push: Exact release source `eb6a17a` was clean and synchronized with `origin/main` before signed build/install; the following reconciliation changes product memory only.
- Related: `FB-20260907-006`, `FND-20260907-015`, `DEC-20260907-005`, `IMP-20260907-011`, `VER-20260907-012`, `VER-20260907-013`.
- Verification: `VER-20260907-013`.
- Status: Released and device-verified as Whip 0.3.60/code 66; awaiting normal real-use feedback.

### IMP-20260907-013 — Single-owner active-workout recovery in Routine detail

- Behavior changed: An active workout's visible source Routine now owns one full-width “Open Active Workout” action. If the source Routine is absent because the view is filtered, archived differently, or the workout is ad hoc, Routine detail renders one page-level fallback instead. Programmed next-day and per-day actions no longer rewrite themselves into recovery aliases; unavailable start actions are absent while a session is active.
- Preserved behavior: Day-specific Resolve Equipment remains available on another editable Routine because it is a distinct task, while the active source Routine retains its existing edit/program-position lock. Starting a Routine, out-of-order program behavior, program advancement, history, deletion-dialog recovery, and the single-expanded-card Workout History action are unchanged.
- Systemic review: Production action strings/callback owners and the current visual catalog were audited for simultaneously visible repeated outcomes. The remaining destructive-dialog recovery actions each belong to one dialog; Workout History permits one expanded owner; repeated entity actions target different records; and the global Search shortcut and local editable filter have distinct focus-versus-filter roles. No other same-outcome double-button defect was confirmed.
- Regression coverage: The persisted four-day 5/3/1 journey now starts a real workout, requires exactly one visible “Open Active Workout” semantic node, captures `gym.routine.active-blocked`, and opens the active workout through that sole action. The declared catalog increases from 175 to 176 states and Gym from 46 to 47 surfaces.
- Persistence/migration/history impact: Presentation, navigation ownership, semantics, Android regression coverage, and catalog metadata only. Room schema 46, data epoch 6, exact-match backup version 25, existing data, and release identity are unchanged.
- Compatibility and limitations: The accepted source was not installed at this implementation boundary, but was later included in the guarded Whip 0.3.61/code 67 owner-phone release in `IMP-20260907-016`. Public publication still requires a fresh frozen candidate, and subjective phone aesthetics remain for normal owner validation.
- Commit/push: Product source, regression, catalog declaration, and this reconciliation are committed as one coherent change and pushed to `origin/main`.
- Related: `FB-20260907-007`, `FND-20260907-016`, `DEC-20260907-006`, `VER-20260907-014`.
- Verification: `VER-20260907-014`.
- Status: Implemented, code-reviewed, emulator-accepted, semantically audited, visually verified, and subsequently released through `IMP-20260907-016`.

### IMP-20260907-014 — One Gym set density and whole-product consistency remediation

- Behavior changed: Removed the compact Gym set-row preference and its Settings, SharedPreferences, backup, composable, test-call, cause/effect, and documentation paths. Every workout exercise now uses the shared medium `WhipItemCard` surface and one 12×10 dp / 6 dp rhythm while retaining concise work-section, classification, planned, effort, and prescription context. The active composer remains a distinct input workspace.
- Whole-product review changed: Re-reviewed all 176 declared surfaces as one system. The frequent next-set jump now has an explicit 48 dp minimum in its shortest state. Selected-Track analytics is named “Track Insights” beside the workspace aggregate “Insights,” removing adjacent scope ambiguity without deleting either capability.
- Shared UI changed: Renamed the formerly productivity-specific collection surface to `WhipItemCard` and adopted it across Task, Habit, Goal, Track, and Gym call sites. Domain-specific headers, controls, charts, editors, and progression semantics remain caller-owned.
- Regression coverage: Settings/persistence/backup tests reject resurrection of the retired density field; Gym UI proves visible passive-set context; visual-catalog tests lock the next-set geometry and both Track labels; navigation/source contracts follow the scoped Track name. Backup exact-match version advances 25→26; Room schema 46 and data epoch 6 are unchanged.
- Compatibility and limitations: Existing workout, set, Routine, 5/3/1, history, calculation, and local-owner data are unchanged. Older backup archives are intentionally rejected at the clean exact-match contract. This is verified private-development source, not a frozen Play Store candidate; release installation remains a separate step.
- Detailed audit: `docs/quality/UI_UX_DESIGN_CONSISTENCY_AUDIT_2026-09-07.md`.
- Commit/push: Accepted implementation source `40b9f96` and release source `07e5c70` are pushed to `origin/main`; the following release-memory reconciliation changes documentation only.
- Related: `FB-20260907-008`, `FND-20260907-017`, `FND-20260907-018`, `FND-20260907-019`, `DEC-20260907-007`, `DEC-20260907-008`, `VER-20260907-015`.
- Verification: `VER-20260907-015`.
- Status: Implemented, code-reviewed, broad-tested, semantically verified, visually accepted, and packaged through `IMP-20260907-015`; physical installation was explicitly replaced with APK delivery by `FB-20260907-009`.

### IMP-20260907-015 — Signed Whip 0.3.61 private APK handoff

- Behavior changed: Advanced Whip to 0.3.61/code 67 and built the accepted single-density Gym and whole-product consistency work as a signed private APK. The artifact was copied to the clearly versioned handoff path `/tmp/Whip-0.3.61-code67-private.apk` for direct delivery.
- Important files/symbols: Release source `07e5c70`, implementation source `40b9f96`, `app/build.gradle.kts`, `app/build/outputs/apk/release/app-release.apk`, `/tmp/Whip-0.3.61-code67-private.apk`, and `VER-20260907-016`.
- Persistence/migration/history impact: Room schema 46 and data epoch 6 remain unchanged; exact-match backup version is 26 because the retired Gym density field no longer belongs to the format. No phone data or installation was touched during this handoff.
- Compatibility and limitations: The APK uses Whip's established private signer and package identity and is intended for an in-place manual upgrade. Because the phone became unavailable and the user explicitly requested the artifact instead, 0.3.61 was not installed, launched, or runtime-smoked on the physical device. Whip 0.3.60/code 66 remains the latest device-verified installation, and this private artifact is not a frozen Play Store candidate.
- Commit/push: Exact release source `07e5c70` was clean and synchronized with `origin/main` before the signed build; this memory-only reconciliation follows it.
- Related: `FB-20260907-008`, `FB-20260907-009`, `FND-20260907-017`, `FND-20260907-018`, `FND-20260907-019`, `DEC-20260907-007`, `DEC-20260907-008`, `IMP-20260907-014`, `VER-20260907-015`, `VER-20260907-016`.
- Verification: `VER-20260907-016`.
- Status: Released as a cryptographically verified Whip 0.3.61/code 67 artifact and subsequently installed/device-verified through `IMP-20260907-016` / `VER-20260907-017` after explicit user reconnection.

### IMP-20260907-016 — Whip 0.3.61 owner-phone in-place installation

- Behavior changed: Installed the existing signed Whip 0.3.61/code 67 private APK in place on the explicitly selected physical Samsung owner phone after the user supplied a reachable wireless-debugging target. The accepted single-density Gym, shared-card, Track scope, and Routine recovery changes are now available for normal owner use.
- Important files/symbols: Release source `07e5c70`, implementation source `40b9f96`, signed `app/build/outputs/apk/release/app-release.apk`, package `commvne.com.whip.app`, and `VER-20260907-017`.
- Persistence/migration/history impact: Android performed a streamed `install -r`; package identity, established signer, and the original 2026-08-26 first-install timestamp were preserved. Room schema 46 and data epoch 6 are unchanged; exact-match backup version is 26. No reset, clear, uninstall, downgrade, fresh-start confirmation, or physical instrumentation occurred.
- Compatibility and limitations: The installed APK exactly matches the previously verified handoff artifact. The guarded cold-launch, foreground, live-process, and bounded fatal/persistence-log checks passed. This is a private owner-phone release, not a frozen Play Store candidate; subjective real-use ergonomics remain awaiting owner validation.
- Commit/push: Exact release source `07e5c70` and the prior artifact record are reachable on `origin/main`; this installation adds product-memory documentation only.
- Related: `FB-20260907-008`, `FB-20260907-009`, `FB-20260907-010`, `FND-20260907-017`, `FND-20260907-018`, `FND-20260907-019`, `DEC-20260907-007`, `DEC-20260907-008`, `IMP-20260907-014`, `IMP-20260907-015`, `VER-20260907-015`, `VER-20260907-016`, `VER-20260907-017`.
- Verification: `VER-20260907-017`.
- Status: Released and device-verified as Whip 0.3.61/code 67; awaiting normal owner validation.

### IMP-20260907-017 — Uniform card summaries and exact active-Set quick-save authorship

- Behavior changed: Added one named card-geometry contract for equivalent collection summaries: 12 dp horizontal inset, 10 dp vertical inset, 6 dp content rhythm, 68 dp ordinary collapsed height, `titleMedium` semibold titles, concise title-column status, and stable action lanes. Task schedule/recurrence metadata is one compact overview line and expands to its complete value; elapsed Goals show at most three meaningful overview components while preserving the complete configured metric in expanded/detail and accessibility output. Home status cards now use the same navigation-row grammar.
- Cross-product UI changed: Exercise, Machine, Category, Routine browse, Routine Builder placement, Workout History, and active-workout Exercise headers consume the shared inset/typography hierarchy where their roles are equivalent. Rich Machine facts, Routine evidence, history detail, charts, editors, and the active Set composer remain responsively sized instead of being forced into an arbitrary fixed height.
- Active-workout behavior changed: `QuickSetAuthorshipBoundary` and `saveQuickSet` no longer reject an active Set merely because an unrelated earlier Set advanced the session revision. The frozen boundary now protects the exact Set UUID/update and exact workout-exercise UUID/update; completed/removed/replaced Sets, changed placement interpretation, and duplicate completion remain rejected.
- Regression coverage: Added exact mixed-card height/status alignment, compact/full elapsed representation, expanded Task metadata, quick-boundary restoration, repository prior-Set-edit acceptance, duplicate/stale protection, and a real MainActivity → Gym → previous-Set editor → active composer → database journey. Stabilized existing fold, nested-action, and History viewport tests to assert the new hierarchy without weakening behavior.
- Persistence/migration impact: No Room, data-epoch, backup-format, route, workout-calculation, Routine, or 5/3/1 progression change. Existing owner data is preserved.
- QA/evidence: `VER-20260907-019`; exact whole-product review gallery `/tmp/whip-whole-ui-card-uniform-20260907/index.html`.
- Commit/push: Accepted implementation source `a9d16d0` and release source `70506ab` are pushed to `origin/main`; the following release-memory reconciliation changes documentation only.
- Related: `FB-20260907-011`, `FB-20260907-012`, `FND-20260907-020`, `FND-20260907-021`, `DEC-20260907-009`, `VER-20260907-018`, `VER-20260907-019`.
- Status: Implemented, code-reviewed, fully emulator-accepted, semantically checked, visually accepted, and subsequently released through `IMP-20260907-018`.

### IMP-20260907-018 — Signed Whip 0.3.62 owner-phone release

- Behavior changed: Advanced Whip to 0.3.62/code 68, built the accepted uniform-card and active quick-save implementation with the established private signer, and installed it in place on the explicitly selected physical Samsung owner phone. The shared summary geometry and repaired prior-Set-edit → active-Set completion workflow are now available for normal use.
- Important files/symbols: Implementation source `a9d16d0`, release source `70506ab`, signed `app/build/outputs/apk/release/app-release.apk`, bundle `app/build/outputs/bundle/release/app-release.aab`, package `commvne.com.whip.app`, and `VER-20260907-020`.
- Persistence/migration/history impact: Android performed a streamed `install -r`; package/signing identity and the original 2026-08-26 first-install timestamp were preserved. Room schema 46, data epoch 6, and exact-match backup version 26 are unchanged. No reset, clear, uninstall, downgrade, fresh-start confirmation, or physical instrumentation occurred.
- Compatibility and limitations: APK hash `9420ca4fba4908531a454d515df28cdb297896859717b10f2bf2e5955ef0ece3`; AAB hash `c13deb209926268cbb5ad4a0c0f5e22ad71d797e63dac3e6084b7f6b737ee744`; established signer certificate SHA-256 `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`. This is a private owner-phone release, not a frozen Play Store candidate; normal real-use aesthetics and ergonomics remain the final subjective validation.
- Commit/push: Exact release source `70506ab` and implementation source `a9d16d0` were clean, synchronized, and reachable from `origin/main` before build/install; this release record is documentation-only.
- Related: `FB-20260907-011`, `FB-20260907-012`, `FND-20260907-020`, `FND-20260907-021`, `DEC-20260907-009`, `IMP-20260907-017`, `VER-20260907-019`, `VER-20260907-020`.
- Verification: `VER-20260907-020`.
- Status: Released and device-verified as Whip 0.3.62/code 68; awaiting normal owner validation.

### IMP-20260907-019 — Comparable 5/3/1 program-layout choices

- Behavior changed: The 5/3/1 Routine Builder now explains each available schedule before selection. Four-Day states its four once-weekly main Exercises and editable Main/Supplemental structure; Beginners states its three full-body pairings, included FSL 5 × 5, and 50–100-rep assistance guidance; Custom states its one-day-per-selected-Exercise contract and freedom from the standard four.
- Shared UI changed: Program presets and schedule layouts now consume one `FiveThreeOneProgramChoiceCard` rather than mixing explanatory cards with bare filter chips. Both groups share full-width geometry, title/support hierarchy, selected color, accessibility selected/state semantics, and stable test tags. Duplicated selected-only explanatory paragraphs were removed.
- Compatibility boundary: A long-term Leader/Anchor plan continues to exclude the standalone Beginners template, but the UI now explains that Beginners is available by choosing Classic cycle. The exact generator remains authoritative; no schedule, percentage, Supplemental work, assistance generation, Training Max progression, or history behavior changed.
- Regression coverage: `RoutineBuilderUiTest` requires all three Classic schedule choices and their exact supporting descriptions, requires Four-Day to be selected by default, and verifies at compact width/enlarged text that a long-term plan omits Beginners while exposing the recovery explanation.
- Persistence/migration/history impact: Presentation model, Compose UI, and Android UI assertions only. Room schema 46, data epoch 6, exact-match backup version 26, current Routines, completed workouts, and owner data are unchanged.
- QA/evidence: `VER-20260907-021`; focused accepted Gym gallery `/tmp/whip-gold-standard-531-layout-final2-20260907/index.html`.
- Related: `FB-20260907-013`, `FB-20260907-014`, `FND-20260907-022`, `DEC-20260907-010`, `VER-20260907-021`.
- Verification: `VER-20260907-021`.
- Status: Implemented, code-reviewed, two-emulator accepted, and visually verified; whole-product acceptance and release remain in progress.

### IMP-20260907-020 — Current test-inventory documentation reconciliation

- Behavior changed: Reconciled the current feature-coverage baseline in `docs/testing.md` from 1,581/621/960 to the executable 1,584 product tests: 621 JVM and 963 Android instrumentation tests. Historical verification records remain unchanged because they truthfully describe earlier source states.
- QA behavior: The existing complete gate continues to fail closed when its counted source inventory and the current testing guide disagree. No guard was relaxed, bypassed, or waived.
- Persistence/product impact: Documentation only. No production code, tests, Room schema, data epoch, backup format, user data, or release identity changed.
- Commit/push: This reconciliation is committed and pushed as its own auditable QA-maintenance chunk before the final whole-product Android and visual replacement campaigns.
- Related: `FB-20260907-013`, `FND-20260907-023`, `VER-20260907-019`, `VER-20260907-022`.
- Verification: `VER-20260907-022`.
- Status: Implemented and complete-gate verified.

### IMP-20260907-021 — Deterministic Goal saving-overlay lifecycle regression

- Test behavior changed: The large-text Goal progress failure regression now waits up to five seconds for the saving overlay to be genuinely displayed before and after hardware Back. It retains the stronger viewport-visible assertion instead of weakening the contract to node existence.
- Preserved contract: The test still proves one submission, blocked dismissal during persistence, retained `123.5` draft after failure, visible owned error, and an enabled retry. Production `GoalMeasurementDialog`, saving-overlay focus/semantics, repositories, and user behavior are unchanged.
- Evidence: The original assertion passed five isolated repeats before modification, identifying suite-load synchronization rather than a deterministic product failure. The corrected assertion then passed five repeats and the complete eight-test `GoalSecondaryMutationUiTest` class on the same previously failing emulator.
- Persistence/product impact: Android test synchronization and durable QA memory only. No Room schema, data epoch, backup format, owner data, or release identity changed.
- Related: `FB-20260907-013`, `FND-20260907-024`, `VER-20260907-023`.
- Verification: `VER-20260907-023`.
- Status: Implemented and fully emulator-verified; the complete clean-source 963-test replacement and final 176-surface acceptance passed in `VER-20260907-024`.

### IMP-20260907-022 — Whole-product gold-standard acceptance candidate

- Behavior changed: Completed the fresh whole-product audit from the device-verified 0.3.62 baseline. The only supported product P2 found in this run was the unexplained 5/3/1 schedule selector, now replaced by comparable explanatory choice cards for Four-Day, Beginners, and custom layouts. The complete gate also exposed and resolved stale test-inventory documentation and a loaded-suite Goal overlay synchronization race without changing production behavior.
- Review result: Re-reviewed the complete declared 176-surface catalog across Shared, Tasks, Habits, Goals, Tracks, Gym/Routines/5/3/1, Settings, and Organization. Equivalent surfaces retain one responsive design grammar; specialized charts, editors, active workout input, pickers, and destructive reviews remain deliberate role-specific exceptions. No additional supported P0/P1/P2 remained after final visual, semantic, source, and behavior review.
- Persistence/product impact: The 5/3/1 change is presentation-only and keeps exact generated program semantics authoritative. Room schema 46, data epoch 6, exact backup version 26, user data, completed history, routine/program state, and release identity are unchanged.
- Commit/push: Product and regression changes are clean and reachable on `origin/main` at `95761df`; release versioning follows as a separate auditable chunk.
- Related: `FB-20260907-013`, `FB-20260907-014`, `FND-20260907-022`, `FND-20260907-023`, `FND-20260907-024`, `DEC-20260907-010`, `IMP-20260907-019`, `IMP-20260907-020`, `IMP-20260907-021`, `VER-20260907-024`.
- Verification: `VER-20260907-024`.
- Status: Implemented, code-reviewed, and accepted for the higher-version private release lane.

### IMP-20260907-023 — Whip 0.3.63 gold-standard private release

- Behavior changed: Advanced Whip from 0.3.62/code 68 to 0.3.63/code 69, built a signed optimized private APK and AAB from exact pushed source `fe943ec`, and installed the APK in place on the explicitly selected physical Samsung owner phone at `192.168.2.187:39787`.
- Artifact: The signed APK is `/root/repos/whip/app/build/outputs/apk/release/app-release.apk` and the identical direct-handoff copy is `/tmp/Whip-0.3.63-code69-private.apk`; SHA-256 is `1a942de2faa79c569134b436778d6a3bb026cc0abb0a3b8fddd793e9fa04f7df` and size is 4,308,945 bytes. The AAB SHA-256 is `7078e508db7d295ec8b291d44dfbeb0a16df604402cf3634e8f02248084c0f37`, size is 11,596,536 bytes, and archive integrity passes.
- Signing/package: Package remains `commvne.com.whip.app`; the APK verifies with APK Signature Scheme v2, one RSA-4096 signer, and established certificate SHA-256 `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`.
- Persistence/history impact: Android used streamed `install -r`; `firstInstallTime=2026-08-26 17:59:24` is unchanged, while `lastUpdateTime` advanced to `2026-09-07 21:58:23`. Room schema 46, data epoch 6, exact backup version 26, package identity, signer, installed data, completed history, and Routine/5/3/1 state are preserved. No reset, clear, uninstall, downgrade, or physical instrumentation occurred.
- Commit/push: Acceptance source `95761df`, acceptance memory `ff8f010`, and exact release source `fe943ec` were clean and pushed before construction and installation; this release-evidence reconciliation changes documentation only.
- Related: `FB-20260907-013`, `FB-20260907-014`, `IMP-20260907-022`, `VER-20260907-024`, `VER-20260907-025`.
- Verification: `VER-20260907-025`.
- Status: Released and device-verified; normal owner use remains the final subjective validation channel.

### IMP-20260907-024 — Full-width productivity-card information rows

- Behavior changed: Task, Habit, Goal, and ordinary Track collection cards now separate identity and actions from supporting information. Emoji, start-aligned title, disclosure, and completion/Log/Reset/Add action share one vertically centered 48 dp header row; concise metadata occupies a second content-driven row from the emoji edge to the card content edge and may wrap instead of ellipsizing.
- Expanded-state result: Collapsed-only summaries are replaced by the richer expanded block instead of being duplicated. Persistent elapsed Goal status remains exactly once, and expanded Area badges, supporting text, and compact-width Edit actions now use the same identity edge. Existing action order, touch targets, card shell, click ownership, selection/reorder behavior, and domain-specific rich content are preserved.
- Important files/symbols: `ProductivityItemHeader`, `ProductivityItemInformationColumn`, `ProductivityItemSupportingText`, `TaskRow`, `HabitProgressCard`, `GoalCard`, `TrackSummaryRow`, their geometry regressions, and `VisualCatalogPagesTest`. Four explicit expanded-card catalog states increase the declared inventory from 176 to 180; the representative Task fixture now exercises the complete “Scheduled · Sep 7, 2026 · Repeats · Mon, Thu” label.
- Persistence/product impact: Presentation and QA catalog only. Room schema 46, data epoch 6, exact backup version 26, package/version identity, Tasks/Habits/Goals/Tracks, history, and owner data are unchanged. No physical-phone instrumentation, installation, reset, clear, uninstall, downgrade, or release occurred.
- Commit/push: Exact implementation and evidence source `5207c88` is reachable on `origin/main`; this documentation-only reconciliation records that pushed identity.
- Related: `FB-20260907-015`, `FND-20260907-025`, `DEC-20260907-011`, `VER-20260907-026`.
- Verification: `VER-20260907-026`.
- Status: Verified.

### IMP-20260907-025 — Nested active-workout Set hierarchy

- Behavior changed: Passive, incomplete, completed, and removed active-workout Sets now occupy light nested surfaces inside the Exercise card instead of flowing together as bare rows. Programmed section and Set number form the identity row; entered/planned values, status/classification/effort, and prescription each receive their own full-width reading row. General Sets omit the redundant “Workout” prefix.
- Active/interactions: The active quick-entry composer remains a distinct primary-tinted workspace but now shares the same identity-first ordering and gives its target full width. Exact whole-card editing, overflow menus, completed/incomplete controls, required-Main removal review, undo, completion collapsing, and Set/exercise reordering retain their callbacks and ordering. The completed checkbox is explicitly 48 dp after focused semantics evidence exposed its prior 24 dp node.
- Design system: The outer Exercise stays the medium `WhipItemCard`; nested Sets use `surfaceContainerHigh`, the small Whip shape, shared 12 dp horizontal/10 dp vertical padding, 6 dp internal evidence rhythm, and 8 dp sibling spacing. No new density setting, persistence, schema, or heavy elevation layer was added.
- Scope review: Workout History already uses bounded Set surfaces and Routine setup is an authored editor rather than an equivalent passive stream; both remain unchanged. Rest timer, next-set focus, program progression, 5/3/1 generation, and historical truth are unaffected.
- Important files/symbols: `WorkoutExerciseCard`, `workoutExecutionIdentityLabel`, `workoutExecutionStatusLabel`, and `GymPowerInputUiTest#passiveWorkoutSetCardSeparatesIdentityValuesStatusAndTargetAtLargeText`.
- Persistence/product impact: Presentation, semantics, and focused Android regression coverage only. Room schema 46, data epoch 6, exact backup version 26, workout/set data, package/version identity, and owner installation are unchanged. No release or physical-phone operation is part of this follow-up.
- Commit/push: Exact implementation and accepted evidence source `ef73b42` is clean and reachable from `origin/main`; this reconciliation changes durable documentation only.
- Related: `FB-20260907-016`, `FND-20260907-026`, `DEC-20260907-012`, `VER-20260907-027`.
- Verification: `VER-20260907-027`.
- Status: Implemented, code-reviewed, and emulator-verified.

### IMP-20260907-026 — One Set grammar across active execution and History

- Behavior changed: Active passive/removed Sets and completed-workout History Sets now consume one `WorkoutSetInformationSurface`: `surfaceContainerHigh`, Whip's small shape, shared 12×10 dp inset, and 6 dp internal rhythm. History follows the same identity → values → status/effort → target order as the active workout, then appends read-only rest, tempo, unilateral, and note evidence.
- Preserved distinctions: The active quick-entry composer remains a primary-tinted input workspace with edit, overflow, completion, and reorder controls. History remains read-only and preserves performed values, RPE/RIR, prescriptions, removal/substitution outcomes, equipment/setup, notes, and program snapshots. Exercise cards, Routine authorship surfaces, 5/3/1 generation/progression, and calculations are unchanged.
- QA support changed: Added a 320 dp/200% expanded-History hierarchy/inset regression and a dedicated `gym.history.expanded` catalog owner. Hardened catalog startup against taller lazy-list cards and emulator weekday rollover, and corrected the remaining elapsed-Goal regression to Whip's owner-approved emoji-edge information row.
- Persistence/product impact: Presentation, semantics, and Android QA only. Room schema 46, data epoch 6, exact backup version 26, package/version identity, workout data, completed history, and owner data are unchanged.
- Commit/push: This coherent implementation/evidence chunk is committed and pushed before separate release versioning; its exact pushed identity is reconciled in the subsequent release record.
- Related: `FB-20260907-015`, `FB-20260907-016`, `FB-20260907-017`, `FND-20260907-027`, `FND-20260907-028`, `DEC-20260907-011`, `DEC-20260907-013`, `VER-20260907-028`.
- Verification: `VER-20260907-028`.
- Status: Implemented, code-reviewed, two-emulator accepted, visually verified, and released in Whip 0.3.64/code 70 through `IMP-20260907-027`.

### IMP-20260907-027 — Whip 0.3.64 cross-Gym consistency private release

- Behavior changed: Advanced Whip from 0.3.63/code 69 to 0.3.64/code 70 and shipped the shared active/History Set information grammar, dedicated expanded-History regression/catalog state, and deterministic catalog/card QA corrections accepted in `VER-20260907-028`.
- Artifact/signing: The signed optimized APK is `app/build/outputs/apk/release/app-release.apk` with direct handoff copy `/tmp/Whip-0.3.64-code70-private.apk`, SHA-256 `64052035aed2d77ec3afb618b84a6cbb84b42638cd7fd2213259657845fe34f1`, and size 4,308,945 bytes. The AAB SHA-256 is `01d063d867c99b76119810d396ca45ef9272b304fbe424caf5322cb99ff72cd3`, size 11,597,936 bytes, with clean archive integrity. Package remains `commvne.com.whip.app`; APK Signature Scheme v2 verifies with the established RSA-4096 signer certificate SHA-256 `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`.
- Persistence/history impact: Android performed streamed `install -r`; `firstInstallTime=2026-08-26 17:59:24` is unchanged and `lastUpdateTime=2026-09-08 00:45:20`. Room schema 46, data epoch 6, exact backup version 26, package identity, signer, installed data, completed history, and Routine/5/3/1 state are preserved. No reset, clear, uninstall, downgrade, fresh-start confirmation, or physical instrumentation occurred.
- Commit/push: Accepted implementation/evidence source `565bc1a` and exact release source `33a01d6` were clean, pushed, and equal to `origin/main` before construction and installation; this release-evidence reconciliation is documentation-only.
- Related: `FB-20260907-017`, `FND-20260907-027`, `FND-20260907-028`, `DEC-20260907-013`, `IMP-20260907-026`, `VER-20260907-028`, `VER-20260907-029`.
- Verification: `VER-20260907-029`.
- Status: Released and device-verified; normal owner use remains the final subjective validation channel.

### IMP-20260908-001 — Read-only archived Track evidence and exact search routing

- Behavior changed: Archived Track Entry rows now omit the complete Edit/More/Delete mutation cluster. Selecting an archived Entry from unified search opens the exact read-only Entry inspector in the Archived Track workspace instead of sending the user to a blocked writable editor. Active Track and Entry search/edit/delete behavior is unchanged.
- QA/catalog changed: The representative archived Track now contains a real Entry, and `tracks.archived.detail.entries` adds an explicit read-only-history surface to the catalog, increasing the declared inventory from 181 to 182. The focused route regression also proves one compact `track-detail-navigation` owner and no archived Entry mutation actions.
- Review correction: An initial streamed source excerpt appeared to show a duplicated compact detail call, but current source, `git show`, and line-specific blame proved only one owner. `FND-20260908-001` was rejected and durable memory corrected before production changes.
- Persistence/product impact: Presentation, in-app routing state, and Android QA fixtures only. Room schema 46, data epoch 6, exact backup version 26, Track definitions, Entries, CSV behavior, package/version identity, and user data are unchanged. No physical phone was queried or mutated.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-001`, `FND-20260908-002`, `DEC-20260908-001`, `VER-20260908-001`.
- Verification: `VER-20260908-001`.
- Status: Implemented, code-reviewed, two-emulator accepted, and visually verified; the wider whole-product goal remains in progress.

### IMP-20260908-002 — Reachable scoped search within one Track's Entries

- Behavior changed: Every Track Entries page now exposes a compact Search action beside Filter and Sort. The accessible action names the current Track, expands a full-width `Search Entries` field, shows active state, supports one-tap clearing, and clears the query when closed. Matching, filtering, sorting, paging-mode switching, and no-match guidance continue through the pre-existing indexed repository path.
- Scope boundary: This local search narrows the Track already being reviewed; shell-level `Search Tracks & Entries` remains the sole owner of cross-Track and archived discovery. Archived Entries can be searched and inspected but remain read-only under `IMP-20260908-001`.
- QA/catalog changed: Added matching and explicit no-match Track-detail surfaces, increasing the declared catalog from 182 to 184. The real repository catalog journey exercises the field and result state, while the complete Tracks profile reconfirms FTS/fallback, large-history paging, filters, sorting, editors, CSV, lifecycle recovery, archive/history, and integrity boundaries.
- Persistence/product impact: Compose UI and visual QA inventory only. Room schema 46, data epoch 6, exact backup version 26, Entry index/schema/content, Track/Entry identity, package/version, and user data are unchanged. No physical phone was queried or mutated.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-003`, `DEC-20260908-002`, `VER-20260908-002`.
- Verification: `VER-20260908-002`.
- Status: Implemented, code-reviewed, two-emulator accepted, and visually verified; the whole-product goal remains in progress.

### IMP-20260908-003 — Authorable current-list query in Task filters

- Behavior changed: `Sort, Group & Filter Tasks` now starts with a full-width `Search Current List` field. It searches Task titles, notes, and step text through the existing matching path, names its local scope, offers one-tap clearing, and surfaces the existing active `Query` chip after application.
- Scope boundary: The field completes the current-list and saved-filter recipe for Today, Inbox, Upcoming, Completed, and Archived. Shell-level Task search remains the cross-state discovery owner, and manual reorder remains correctly unavailable while narrowing criteria are active.
- QA/catalog changed: Added `tasks.filter` and `tasks.today.filtered`, increasing the source-linked catalog from 184 to 186 surfaces. Focused semantics now authors a query and proves the matching Task remains reachable; the catalog journey also resets the query before continuing through every other Task destination.
- Persistence/product impact: Compose UI and visual/interaction QA only. The pre-existing `SavedTaskFilter.textQuery`, Room schema 46, data epoch 6, exact backup version 26, Task data, package/version identity, and owner installation are unchanged. No physical phone was queried or mutated.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-004`, `DEC-20260908-003`, `VER-20260908-003`.
- Verification: `VER-20260908-003`.
- Status: Implemented, code-reviewed, two-emulator accepted, and visually verified; the whole-product goal remains in progress.

### IMP-20260908-004 — Truthful visual-catalog state and accessibility evidence

- QA behavior changed: Catalog capture now sends a final accessibility content-change signal, evicts the platform accessibility cache on API 34+, refreshes the connected active-window tree, and only then exports hierarchy XML. Completed family/whole-product captures also fail closed when two declared surfaces have byte-identical PNG evidence.
- Owner coverage changed: The Habit inspector owner proves the exact Options and History selected/content states before capture; the Goal page owner proves its overflow menu is displayed. The non-state-changing `habits.actions` alias was removed, leaving the canonical Options inspector and row-overflow menu as the two truthful action surfaces.
- Catalog impact: The Task additions remain, while removal of the duplicate Habit alias changes the declared inventory from 186 to 185 distinct PNG/XML surface pairs. No product surface, user-facing behavior, or accessibility semantics were removed.
- Persistence/product impact: Android test infrastructure, catalog accounting, and durable evidence only. Room schema 46, data epoch 6, exact backup version 26, package/version identity, app behavior, and user data are unchanged. Only disposable emulator execution occurred; no physical phone was queried or mutated.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-005`, `DEC-20260908-004`, `VER-20260908-004`.
- Verification: `VER-20260908-004`.
- Status: Implemented, code-reviewed, harness-verified, and accepted against complete Habit and Goal families; the whole-product goal remains in progress.

### IMP-20260908-005 — Intentional shared color and search-empty hierarchies

- Behavior changed: The shared two-line color preview now separates `Default`, a preset name, or `Custom` from its supporting app-default explanation or exact hex value, eliminating repeated custom hex text across Area, Habit, Goal, and Gym dialogs. Single-slot fields and spoken color-preview descriptions retain `Custom · #RRGGBB` precision. A fully settled Unified Search with zero results now says `No matching items. Try another search or adjust Filters.`; incomplete/loading source states retain their uncertainty-specific copy.
- Important files/symbols: `WhipColorPickerDialog`, `colorPickerPreviewName`, `color-picker-preview-name`, `color-picker-preview-value`, `search_no_matches`, `InteractionControlUiTest`, `UnifiedSearchAdaptiveUiTest`, and `VisualCatalogSharedShellTest`.
- Persistence/migration/history impact: None. Stored ARGB values, preset identities, color math, search indexing/filtering, routing, schema 46, data epoch 6, and backup format 26 are unchanged.
- Compatibility and limitations: The change is shared by every existing color-picker caller and both compact/wide search layouts. It intentionally does not alter the compact field string or color-preview content description, because those contexts need exact identity in one slot. No release/version change or physical-device operation occurred.
- Commit/push: Included in the owned shared-shell/organization source chunk; exact pushed SHA is authoritative in Git history.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-006`, `FND-20260908-007`, `DEC-20260908-005`, `DEC-20260908-006`.
- Verification: `VER-20260908-005`.
- Status: Verified; commit/push follows this memory reconciliation.

### IMP-20260908-006 — Responsive backup decision hierarchy and edited Settings headings

- Behavior changed: Backup preview now presents a scannable export summary followed by separately explained, full-width restore choices. Additive `Merge New Data` is the filled primary action; destructive `Replace Everything` is an error-toned outlined action and still opens the existing independent final confirmation; Cancel is the sole footer action. Organization and About no longer repeat their destination title as the first section heading and begin directly with the Areas and Whip cards.
- Important files/symbols: `SettingsContent`, `BackupRestorePreviewDialogs`, `merge-new-data`, `request-replace-everything`, `SafetyChoiceUiTest`, `SettingsBehaviorUiTest`, and `WhipComposeSemanticsTest`.
- Persistence/migration/history impact: None. Merge compatibility, stable-ID handling, relationship remapping, atomic commits, Replace snapshots/rollback, settings replacement, request ownership, schema 46, data epoch 6, and exact backup format 26 are unchanged.
- Compatibility and limitations: Restore choices scroll inside a bounded dialog body and remain reachable at 320 dp/200% text. The summary uses the existing locale/zone timestamp and preview facts. No release/version change or physical-device operation occurred.
- Test inventory: One responsive Android regression increases current source inventory to 1,588 tests: 621 JVM and 967 Android; `docs/testing.md` and `INDEX.md` are reconciled to that exact source count.
- Commit/push: Included in the owned Settings/recovery source chunk; exact pushed SHA is authoritative in Git history.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-008`, `FND-20260908-009`, `DEC-20260908-007`, `DEC-20260908-008`.
- Verification: `VER-20260908-006`.
- Status: Verified; commit/push follows this memory reconciliation.

### IMP-20260908-007 — Truthful residual Gym catalog states and production-faithful hosts

- QA behavior changed: Routine Builder capture owners now prove the exact equipment chooser, quick-machine editor, rep-prescription editor, and classification menu before capture; sequential states also use the catalog's pixel-distinctness guard. Standalone rest, destructive review, required-Main confirmation, Workout History menu, expanded History, and Workout deletion owners now paint Whip's full dark application background instead of exposing the transparent Compose test canvas.
- Product audit result: The complete Gym/Routines/5/3/1 source, domain, persistence, interaction, responsive, and visual review found no supported production behavior or design change beyond the evidence defects. Active execution and completed History retain the accepted shared nested-Set grammar; Routine authorship, machine interpretation, exact Set mutations, immutable history/prescriptions, generated Four-Day/Beginners/Custom programs, bounded adaptive Training Max review, deletion recovery, timers, and progress analytics remain intact.
- Important files/symbols: `RoutineBuilderUiTest`, `GymPowerInputUiTest`, `WorkoutDeletionUiTest`, `gym.machine.choice`, `gym.quick-machine`, `gym.rep-scheme`, `gym.classification.menu`, and the standalone component catalog hosts.
- Persistence/product impact: Android test state/evidence only. No production source, Room schema 46, data epoch 6, exact backup format 26, workout/Routine/5/3/1 data, calculations, package/version identity, or user data changed. No release or physical-phone operation occurred.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-010`, `DEC-20260908-004`.
- Verification: `VER-20260908-007`.
- Status: Verified; commit/push follows this memory reconciliation while final whole-product acceptance remains in progress.

### IMP-20260908-008 — Gold-standard whole-product emulator-only completion

- Product result: Completed the equal-depth audit and implementation goal across Tracks, Tasks, Habits, Goals, Home/shared shell, search, Areas/Tags, Settings, backup/recovery, Health/reminders, Gym, Routines, and 5/3/1. The accepted product changes make archived Track history truthfully read-only, route archived search results to an exact inspector, expose scoped Track Entry and current-Task-list search, clarify shared custom-color and settled-empty-search evidence, and give backup import a responsive safe-versus-destructive decision hierarchy without weakening domain-specific workflows.
- QA result: Hardened the source-linked catalog so declared Habit/Goal/Gym states are semantically proven, accessibility hierarchy export is current, distinct declared surfaces cannot silently share one PNG, and standalone Gym evidence paints production-faithful application backgrounds. The final whole-product visual review accepted all 185 surfaces against hierarchy, role consistency, spacing, truncation, action ownership, empty/error/loading truth, accessibility, and responsive behavior.
- Review result: No supported P0/P1/major P2 remains after the final functional, semantic, visual, and code review. Equivalent collection cards retain one summary grammar; specialized editors, charts, calendars, pickers, workout execution, History, destructive reviews, and 5/3/1 authorship remain deliberate capability-based variants rather than forced visual sameness.
- Persistence/release impact: No Room schema, data epoch, backup format, package/version identity, or owner data changed. No version bump, signed release, Play Store candidate/publication, physical-phone query, installation, launch, reset, clear, uninstall, downgrade, or instrumentation occurred.
- Commit/push: The seven coherent implementation/evidence commits from `720ef11` through exact accepted product source `3dd9268` are reachable from `origin/main`; this final durable-memory reconciliation is documentation-only.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-002` through `FND-20260908-010`, `IMP-20260908-001` through `IMP-20260908-007`.
- Verification: `VER-20260908-008`.
- Status: Implemented, code-reviewed, fully emulator-accepted, visually accepted, committed, and pushed; normal future owner feedback and any separately requested release remain outside this goal.

### IMP-20260908-009 — Whip 0.3.65 whole-product overhaul private release

- Behavior released: Advanced Whip from 0.3.64/code 70 to 0.3.65/code 71 and released the accepted whole-product overhaul from `IMP-20260908-008`, including exact archived Track inspection, scoped Track Entry and Task-list search, truthful color/search evidence, responsive backup decisions, and the fully accepted shared card, Gym Set, Routine, and 5/3/1 design system.
- Artifact/signing: The signed optimized APK is `app/build/outputs/apk/release/app-release.apk` with direct handoff copy `/tmp/Whip-0.3.65-code71-private.apk`, SHA-256 `1e28139b74e07656e6d032b29cfad978bf6dfde1c1d697fd8816f41a22cd60e3`, and size 4,325,365 bytes. The signed AAB SHA-256 is `00b96c64d24732edd657a51ca59a10b7015ade80066918219c0d4bbad41ce100`, size 11,605,206 bytes. Both archives pass integrity checks; APK Signature Scheme v2 verifies with the established RSA-4096 certificate SHA-256 `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`.
- Persistence/history impact: Android performed streamed `install -r` on the explicitly selected Samsung `SM-F976W` at `192.168.2.187:39787`. `firstInstallTime=2026-08-26 17:59:24` is unchanged and `lastUpdateTime=2026-09-08 10:23:00`. Package `commvne.com.whip.app`, signer identity, Room schema 46, data epoch 6, exact backup version 26, installed data, completed history, routines, and 5/3/1 state were preserved. No reset, clear, uninstall, downgrade, fresh-start confirmation, Play Store action, or physical instrumentation occurred.
- Commit/push: Exact release source `347257f` was clean, pushed, and equal to `origin/main` before artifact construction and installation. The earlier exact accepted product source is `3dd9268`; this release-evidence reconciliation is documentation-only.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FB-20260908-003`, `IMP-20260908-008`, `VER-20260908-008`, `VER-20260908-009`, `DEC-20260906-003`.
- Verification: `VER-20260908-009`.
- Status: Released and independently device-verified; normal owner use remains the final subjective validation channel.

### IMP-20260908-010 — Signed frozen-candidate authority and Whip 0.3.66 staging

- Release-gate behavior changed: `scripts/candidate` now requires the established protected release keystore/password boundary, passes it only to the release build, requires the canonical `app-release.apk` and `app-release.aab`, verifies APK signing with `apksigner`, verifies AAB signing and its readable certificate with `jarsigner`/`keytool`, and repeats those checks when retained artifacts are fully verified. Unsigned or wildcard fallback artifacts can no longer become accepted Play Store evidence.
- Regression coverage changed: `scripts/test-candidate-evidence` supplies isolated signer doubles and proves both unsigned APK and unsigned AAB rejection while preserving source/artifact/checksum/manifest/freshness rejection coverage. The release assertion in `scripts/test-android-target-guard` now tracks staged Whip 0.3.66/code 72.
- Version/release impact: Advanced application identity from 0.3.65/code 71 to 0.3.66/code 72 because the owner requires any source-adjusted store candidate to be re-released to the phone. Application behavior, package `commvne.com.whip.app`, Room schema 46, data epoch 6, exact backup version 26, and user data are unchanged.
- Rejected evidence: The nominally accepted 0.3.65 evidence at `/root/repos/whip/build/candidate-evidence/runs/20260908T145545Z-2042661-739285e43ffd` is explicitly invalid for publication because it records `app-release-unsigned.apk` and an unsigned AAB. It remains preserved as failure evidence and must not be uploaded or cited as release authority.
- Important files/symbols: `scripts/candidate`, `verify_release_artifact_signatures`, `scripts/test-candidate-evidence`, `scripts/test-android-target-guard`, and `app/build.gradle.kts`.
- Related: `FB-20260908-004`, `FND-20260908-011`, `DEC-20260908-009`, `DEC-20260904-003`, `DEC-20260906-003`.
- Verification: `VER-20260908-010`.
- Status: Implemented and verified. Exact source was pushed, the signed 0.3.66 candidate passed, and owner-phone parity completed in `VER-20260908-012`; direct bundle handoff superseded Play Console publication.

### IMP-20260908-011 — Candidate tag-merge regression isolation

- QA behavior changed: `TagMutationViewModelTest.mergePublishesTheExactSourceAndDestination` now verifies the owned postcondition—exact receipt identities, removed source, retained destination—without requiring the entire shared-process Tag repository to contain no unrelated records.
- Product/release impact: Android test source only. Application behavior, release APK/AAB contents apart from source provenance, Whip 0.3.66/code 72 identity, package/signer, Room schema 46, data epoch 6, backup format 26, and user data are unchanged.
- Failed evidence: The incomplete signed-candidate workspace at `/root/repos/whip/build/candidate-evidence/.pending-WiF6Wy` failed closed on the former five-second exact-global-list predicate. It is diagnostic failure evidence and is not eligible for upload.
- Related: `FB-20260908-004`, `FND-20260908-012`, `IMP-20260908-010`, `VER-20260908-011`.
- Verification: `VER-20260908-011`; one entirely fresh frozen candidate remains mandatory.
- Status: Implemented, pushed in exact candidate source `65a6b3b`, and verified by the complete fresh candidate in `VER-20260908-012`.

### IMP-20260908-012 — Whip 0.3.66 signed Play-bundle handoff and owner-phone parity

- Release delivered: Qualified Whip 0.3.66/code 72 from exact pushed candidate source `65a6b3b` and produced the directly deliverable bundle `build/releases/Whip-0.3.66-code72-Play.aab`. The owner explicitly retained Play Console upload and publication, so no store mutation occurred.
- Candidate/artifact result: Atomic evidence is `/root/repos/whip/build/candidate-evidence/runs/20260908T160827Z-2302537-d3b9c9c26dee`, input SHA-256 `d3b9c9c26deec86890e8163aa268b0472a3997e5baecf32dec43529872f8fe0e`. Signed AAB SHA-256 is `a75ec4c1650db4672cb583c6391934d172c6684800e8495c174c4839cfcfeeb6`, size 11,605,209 bytes. Matching signed APK SHA-256 is `ad37e283cc65dedf3167fff97d1d7144c004d466dc341c2a523a3f8ee2ff7fc1`, size 4,325,365 bytes. Both archives pass integrity; APK v2 and AAB JAR signing verify with established certificate SHA-256 `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`.
- Phone parity: Installed the exact matching signed APK with streamed `install -r` on selected Samsung `SM-F976W` at `192.168.2.187:39787`. Package remained `commvne.com.whip.app`; version is 0.3.66/code 72; `firstInstallTime=2026-08-26 17:59:24` remained unchanged; `lastUpdateTime=2026-09-08 12:09:50`. No reset, clear, uninstall, downgrade, or physical instrumentation occurred.
- Persistence/product impact: Candidate/signing/test corrections and version identity only; app behavior and stored model remain unchanged. Room schema 46, data epoch 6, exact backup format 26, installed data, completed history, Routines, and 5/3/1 state were preserved.
- Related: `FB-20260908-004`, `FB-20260908-005`, `FND-20260908-011`, `FND-20260908-012`, `DEC-20260908-009`, `IMP-20260908-010`, `IMP-20260908-011`.
- Verification: `VER-20260908-012`.
- Status: Candidate-qualified, directly handed off, and device-verified; Play upload/publication intentionally remains owner-managed.
