# Durable product and engineering decisions

### DEC-20260910-030 — Corresponding page roles share typography, spacing and ordering

- Status: Verified under IMP/VER-20260911-001; FB-20260910-007 / FND-20260910-040. Private release preparation remains in progress.
- Decision: Extend existing WhipPageHeader/WhipEmptyState with one supporting-text renderer, compact page context and the same collection rhythm. Keep readable semantic heading roles; use one 14 sp supporting role for page context and empty explanations, and one 20 sp empty heading. Reduce the shared empty inset to 16 dp. Move Task History's selector below its identity. Reserve Task capture's floating-label allowance only when the label floats.
- Consistency: Ordinary page context must fit a single line at the owner width without clipping or reserving blank lines. Longer authored context and enlarged text wrap naturally. Match heading origins and first-content boundaries across the actual primary routes, inspect neighboring routes, and compare Track/Gym empty text layout results rather than inferring typography from source alone.
- Compatibility: Preserve controls, record ownership, scrolling/keyboard behavior and data formats. Use shared builders and existing spacing tokens; avoid another parallel screen framework. This refines DEC-20260910-029's natural-height rule with concise copy and complete role geometry. Private 0.3.69/code 75 release is authorized by the owner.
- Wide policy: Supersede DEC-20260910-021's per-destination 720/1000 dp measure split, which native review reproduces as a 140 px heading/action jump. Keep the existing workspace builder with a single 1000 dp maximum, content-owned readable text measures, and the same 20 dp list inset in Track master panes. Actual multi-pane/fold ownership remains unchanged.

### DEC-20260910-029 — Shared page headers measure content and parents own inter-item gaps

- Status: Verified; FB-20260910-005 / FND-20260910-039 / IMP, VER-20260910-030.
- Decision: Refine the existing WhipPageHeader builder rather than adding another header family. Keep its 48 dp title/action row and 4 dp title/subtitle relationship, measure actual subtitle lines, and remove built-in trailing space. Use the shared 8 dp sibling rhythm for equivalent collection pages. Centralize page insets so fixed Task controls and their continuing list meet at one sibling gap, with the same outer title/record edges as scrolling pages.
- Constraints: Retain feature-owned navigation, filters, selection, ordering, keyboard behavior and scrolling. Natural wrapping is allowed; padding or truncating content solely to force identical page heights is rejected. No persistence, schema, backup or release change.
- Consequences: Supersedes the old two-line page-subtitle assertion. Shared header changes also improve Goals, Tracks, Gym, Settings and Review callers; update existing geometry assertions and inspect ordinary phone/wide evidence with relevant interaction checks.

### DEC-20260910-028 — Settings operation state carries its result instead of classifying text

- Decision: Reuse existing OperationStatus for Settings outcome severity and working copy; derive message only from terminal outcomes. Keep existing busy coordination, each callback and data boundary. The shared status-card renderer uses action wording, while opening a validated/decrypted backup preview settles quietly because the visible dialog is the acknowledgement.
- Alternatives/tradeoffs: Expanding the regex cannot cover arbitrary exception messages or authored names; another outcome framework duplicates OperationStatus. A first implementation derived busy from Running, but source review found immediate automatic-backup setting failures can overlap an existing file operation. Preserve the existing busy lifecycle so such feedback cannot prematurely end that operation's busy state. This increment corrects outcome truth and ownership without replacing operation admission. No new scroll policy is justified by the native invalid-file return.
- Compatibility: Presentation/runtime state only; preserve exact backup data, validation, native picker, destructive review, typed setting receipts and domain mutation authority. Cancellation clears terminal feedback without clearing an active Running operation.
- Modal ownership: Native FND-20260910-038 additionally requires error feedback inside encrypted unlock and the current backup preview/replacement/reset dialog. Reuse WhipStatusCard and PermanentDeleteDialog.error, suppress the duplicate page card while a dialog owns the result, clear stale feedback on a new reset review, and guard unlock dismissal while Running. Keep plaintext passphrases out of saved state.
- Status: Verified for FND-20260910-037/038 / IMP/VER-20260910-028. Native phone/wide recovery and component replacement retry pass; no data-boundary redesign.

### DEC-20260910-027 — Data controls share action-list geometry and restrained destructive sections

- Decision: Reuse WhipActionList/WhipActionRow for three one-off backup actions, with complete left-aligned labels and adjacent explanations. Keep portable-folder setup distinct, shorten its initial explanation without hiding plaintext disclosure, and explain CSV as individual tables. Give shared WhipDangerZone a neutral section and an error-colored semantic heading; explicit destructive actions and final confirmations remain authoritative.
- Alternatives/tradeoffs: Patching button spacing preserves inconsistent hierarchy; another builder duplicates the existing action-list role; a backup wizard adds steps without a demonstrated decision-flow failure. Shared geometry supports natural wrapping. Descriptions add reading height but replace detached prose. Native light/dark and neighboring caller review must verify destructive discoverability.
- Compatibility: Presentation only. Preserve labels, request ownership, settings, files, encryption, Merge/Replace semantics, two-step replacement/reset boundaries, schema and version. No per-screen 200% patches.
- Status: Verified under FND-20260910-036 / IMP/VER-20260910-027. Normal phone/wide light/dark and neighboring callers reviewed; explicit confirmations and data contracts retained. Whole-product audit remains active.

### DEC-20260910-026 — Retire Health Connect and preserve historical records

- Status: Verified in IMP/VER-20260910-026; FB-20260910-003.
- Decision: Remove the integration rather than hiding its Settings section. Remove SDK, manifest permissions/activities, provider manager, runtime sync/deletion, configuration and authoring affordances. Keep only compatibility attribution for existing source records and old backups.
- History: Before normal runtime opens (including after restore), atomically materialize the existing measurement projection of legacy Health-linked Habits into stored logs and disconnect the retired source. Preserve values, dates, units, provenance, existing manual history and all measurement entries; allow future manual check-ins. Repeating recovery must not duplicate logs. This is a data compatibility step, not provider functionality.
- Alternatives: Hiding controls leaves permission/runtime behavior installed. Deleting imported history violates retained user data. Leaving source-linked Habits indefinitely read-only would strand everyday tracking after sync removal. No database schema change or provider operation is needed.


### DEC-20260910-025 — Health follow-up warnings belong to the owned mutation receipt

- Status: Superseded by FB-20260910-003 / DEC-20260910-026 before delivery. The owner requested complete integration removal during the warning pilot.
- Decision: Send failed last-sync persistence and failed deletion-marker cleanup through the existing HealthMutationReceipt.warnings and shared completed-warning presentation. Keep record commits and successful deletion authoritative; explain the remaining local follow-up without converting it into an operation failure.
- Alternatives/compatibility: Rendering all healthRuntime.message values would mix transient provider status, success and durability problems without exact action ownership. A new notification framework or transaction rollback would add complexity and misstate committed data. Existing mutation receipts already supply the right boundary. No schema, backup, conversion, provider or permission changes.
- Verification seam: Preserve the public Application-only SettingsViewModel constructor used by Android; allow internal injection of the existing HealthConnectManager boundary for real Settings-control tests with deterministic provider/storage failures. Production uses the same manager and reconciliation path.

### DEC-20260910-024 — Custom Units declare record roles and retain feature-owned mutations

- Status: Verified in IMP/VER-20260910-024 under FND-20260910-034 and FB-20260910-001/002.
- Decision: Reuse WhipRecordItem for custom-unit identity, full-width dimension/archive context, complete conversion detail and identity-owned commands. Put each unit in a stable keyed parent-list item, leaving list gaps to existing Settings layout. Add command availability to the record builder so asynchronous mutation guards survive adoption.
- Alternatives/tradeoffs: Adding gaps to the old local Row would retain a separate hierarchy and one eagerly composed aggregate item. A separate manager/search/filter destination is not justified by this baseline. Shared records add modest natural height in exchange for consistent hierarchy and readable state; existing Settings navigation, order and Create placement stay intact.
- Compatibility: No unit arithmetic, domain schema, data, backup, version or history migration. Editors and exact mutation boundaries remain feature-owned. Existing record callers retain enabled actions by default.

### DEC-20260910-023 — Empty Insights explain the current view before offering recovery

- Status: Verified in IMP/VER-20260910-023; FND-20260910-033.
- Decision: Reuse WhipEmptyState before summary rendering when there is no evidence. Workspace empty states offer Create Track plus existing Archived access, or Open Tracks to record an Entry. Per-Track empty state offers Add Entry for active Tracks and View Entries for archived Tracks; unmatched filters retain their visible conditions and existing Clear All. Keep populated summaries, including zero recent periods, unchanged.
- Alternatives/tradeoffs: Rewording the footer leaves the zero dashboard dominant and entryless Tracks unexplained. A new empty-state framework duplicates an established role. Use feature-owned state/callbacks and shared presentation; bound empty-state text centrally for wide reading. First-use and filtered emptiness remain distinct without inspecting hidden Areas or changing saved scope.
- Compatibility: No calculation, schema, backup, history or release change. Existing editor request, archive navigation and filter saved state remain owners; no automatic data mutation or duplicate clear-filter action.

### DEC-20260910-022 — Shared summaries separate headline measures, facts and series

- Status: Verified in IMP/VER-20260910-022; FND-20260910-031/032, FB-20260910-001/002.
- Decision: Add a small read-only summary builder. Callers declare title, headline metrics and supporting facts; the renderer owns their typography, spacing, grouping and available-width layout. A companion short-series renderer owns bars, labels and exact accessible descriptions. Use existing surfaces and spacing, natural text height and up to two headline columns. The wide pilot rejected a three-plus-one arrangement for four metrics; two columns retain the same coherent reading order across phone and detail panes. Reuse WhipRecordItem for recent Tracks with the exact Insights navigation action.
- Improve versus replace: Shorter copy alone leaves duplicated layout ownership. Replacing all analytics with a universal chart/data framework would couple unrelated Goal/Habit semantics and create unnecessary churn. Extending the existing read-only surface vocabulary provides a production pilot across workspace and per-Track summaries while leaving specialized charts and feature state intact. Navigation-only metric tiles remain navigation controls; summary measures do not acquire empty click actions.
- Compatibility: Keep Number/Scale formatting, missing values, non-additive totals, conditional scope, dates and historical evidence. Recent Track counts share an inclusive date-window policy ending today; future Entries remain in all-time evidence. There is no schema, backup, version or stored-data change.
- Acceptance: Verify normal phone/wide hierarchy and source navigation, recent-window boundaries, exact mixed-unit/temperature/Scale values and recreation. Use existing neighboring Track/filter/builder checks and readiness. Do not launch a per-screen enlarged-text campaign or infer whole-product acceptance from this pilot.

### DEC-20260910-021 — One workspace owner chooses panes and reading measure

- Status: Verified; FND-20260910-030 / IMP/VER-20260910-021. Final native phone/wide journeys, 120 Android regressions, 379 readiness JVM checks and lint/build pass.
- Supersession: DEC-20260910-030 replaces this decision's per-destination 720/1000 dp split with one stable 1000 dp workspace measure after owner feedback and native reproduction of a 140 px section-switch jump. Pane ownership and historical verification remain valid.
- Decision: Evolve AdaptiveNavigationFrame and add shared named workspace composition roles. Serial collection/settings/execution views use a centered column up to 720 dp; Home, Track Insights, Gym Progress and feature-owned list/detail use up to 1000 dp. The complete content header and body share that measure. Preserve existing item builders, scrolling, destination saved state and native actions.
- Pane ownership: Expanded Tracks owns its existing list/detail composition and receives no extra statistics pane from the app shell. Keep primary rail navigation, current selection and the existing browser. Existing Insights/Activity retain overview and recent-record access; include total Fields in the Insights overview so the removed sidebar's aggregate remains available. Preserve separate context on physical book/tabletop folds and useful Settings category navigation.
- Alternatives/tradeoffs: A global 720 dp cap would collapse useful browser/analytics layouts; a new navigation framework would risk saved-state and fold/editor recovery without resolving content ownership. Only removing the sidebar leaves inconsistent label/control spans elsewhere. Explicit shared roles improve both problems while leaving deliberate side whitespace for readable serial work. The old outer Tracks expansion action becomes unnecessary where there is no outer pane.
- Compatibility/acceptance: No domain, schema, backup, version or history change. Preserve compact navigation and real native record/menu/edit/archive/history, Habit timer recreation and confirmed Settings Save. Review before/after 100% phone/wide, appropriate fold/IME/state and shared-shell neighbors, then readiness before delivery. Normal UX and wider app quality remain the priority; no per-screen 200% campaign.

### DEC-20260910-020 — Separate execution summaries, Set evidence and template guidance

- Status: Verified; FND-20260910-029 / IMP/VER-20260910-020. Final phone/wide journeys, 226 Android regressions, 378 readiness JVM checks and lint/build pass.
- Decision: Keep the current workout workspace and shared execution renderer. NEXT presents the upcoming Exercise/Set and resolved target with a visible navigation affordance; full source provenance stays in Set targets. Active Set classification uses the status role, while redundant “Up next” values are removed. Structural-action scope is concise and retains the source-Routine boundary. New program generation preserves notes exactly instead of inserting template instructions.
- Alternatives: A new workout workspace or generic form framework adds disruption without resolving these repeated-content responsibilities. Merely shortening stored prescription strings would alter future snapshot text while leaving old display repetition and note pollution intact. A presentation summary plus correct role assignment improves ordinary reading without reinterpreting stored evidence.
- Compatibility: No schema, backup, version, load calculation, progression or completed-history mutation. Existing notes, including older generated text, remain intact. Relevant setup guidance and explicit Standard/Adaptive review rules remain available. This refines active Set presentation only; passive/omitted/History identity → values → status → target contracts stay intact.
- Acceptance: Complete native cycle through review/recreation/next cycle/unchanged History, authored-note preservation, normal phone/wide before-and-after review, neighboring execution/rest/Routine regressions and readiness. Repeated setup guidance and shared wide reading policy remain broader follow-ups.
- Pilot refinement: The execution builder gains one named menu role, retaining the existing feature-owned popup and callbacks. It owns the 48 dp button, icon and accessible name on the button itself; active/passive callers stop assembling duplicate icon-only anchors. The workout owner centralizes the leading-item offset used by NEXT, automatic follow and requested Exercise navigation. Exact within-group Set focus remains a separate behavior to substantively review; this change corrects block targeting.
- Scroll/disclosure refinement: Measure the actual pinned execution lane and offset block navigation by its height, allowing header/content changes without device-specific patches. Shared DisclosureButton owns its stable name as well as expanded/collapsed state, so an occluded child label cannot leave the remaining clickable fragment unnamed.

## DEC-20260910-019 — A 5/3/1 Exercise owns one complete setup configuration
- **Status:** Verified
- **Decision:** Replace parallel configuration lists with one immutable, saveable configuration per Exercise. Reordering, removal, selection and library reconciliation operate on that complete value. Keep the existing guided setup, shared layout builders, program generator and repositories.
- **Rationale:** Fixing only the custom sort leaves identity-sensitive bookkeeping duplicated across many fields and mutation paths. Consolidation prevents field transfer and omission at the feature-state boundary. Replacing the full editor or domain model adds disruption without improving this guarantee.
- **Contract:** Preserve TM, actual/e1RM basis, percentages, applied derivation, valid BBB targets and authored cycle increases by Exercise identity. New identities receive fresh state. Untouched increase suggestions follow role/name/unit; reconfirmation preserves authored state. Existing initial custom ordering may remain, but subsequent user order is authoritative.
- **Compatibility:** No schema, backup, version or historical-data mutation. Existing complete-state hydration guarantees remain required.
- **Verification:** Full-state policy regressions; native reordered authoring → library creation → recreation → saved program → execution → History; normal-scale phone/wide rendered review; neighboring setup regressions and readiness. FND-20260910-028.
- **Result:** VER-20260910-019 verifies the complete authoring boundary. Supplemental choices also resolve the Main Exercise identity while earlier incomplete configurations are omitted from the preview. No positional field arrays remain in guided setup.

### DEC-20260910-018 — Resolve rest duration once and make workout overrides reversible

- Context: FND-20260910-027; the ready execution lane displays the app default while completing its visible next Set uses the Set/Exercise rest. The workout duration editor offers an override but no return to prescribed defaults. Set rest may explicitly be zero.
- Improve versus replace: Extend the existing Rest renderer and extract duration precedence into a small domain policy shared with both transactional completion paths. Replacing the timer or creating a general timer builder would disrupt proven deadline/revision recovery without improving this choice.
- Decision: Ready/manual Start follows the visible next Set, then its Exercise default, then the app default. An explicit workout override wins and is visibly named. With no next Set, ready/manual rest uses the app default unless overridden. Automatic rest resolves the actual completed Set using the same precedence. Running countdowns retain their saved deadline when NEXT advances. Zero prescribed rest remains zero, disables manual Start until an override is chosen, and does not auto-start a timer. “Follow Set rest” clears the saveable workout override; no authored defaults or historical prescriptions change.
- Tradeoffs: Stop returns to the now-visible next Set's rest, which may differ from the completed Set. The ready source label explains this context. Overrides retain the existing Activity saved-state lifecycle; this increment does not add persistent session configuration or change notification delivery.
- Status: Verified in IMP/VER-20260910-018: complete native duration/override/recreation/History journeys, 210 Android regressions, four wide methods plus strengthened recovery, 133 focused and 373 readiness JVM checks, and 15 reviewed originals. Shared wide reading width and whole-product acceptance remain open.
- Related: FB-20260910-001/002; DEC-20260901-023; FND-20260903-020; VER-20260910-017.

### DEC-20260910-017 — Put executable prescriptions before optional placement tools

- Evidence/decision: FND-20260910-026 shows ordinary Set fields below a viewport of conversion and bulk helpers at 100%. Keep the full scrolling placement editor, equipment and programming authority, Training Max disclosure and advanced-field access. Move ordinary notes, saved schemes, warm-up generation, copy-previous behavior and 5/3/1 conversion after the Set list. Use the existing shared section-heading/disclosure roles to distinguish prescriptions from supporting tools.
- Alternatives/tradeoffs: Shrinking controls or adding screen-specific scale branches would preserve the wrong reading order. A universal form builder or forcing editable Sets into passive record/execution builders would add an unproven contract. Permanently hiding bulk helpers behind a new switch would change discovery and add another state. Existing shared roles improve the hierarchy now; long Set lists still require scrolling to bulk tools, a tradeoff to assess during native review. Program-controlled Main/Supplemental restrictions, advanced authored values, percentage-load dependencies, ordering and history remain unchanged.
- Acceptance/status: Verified in IMP/VER-20260910-017: complete ordinary native author/recovery/save/reopen/start/complete/History continuity, warm-up generation/removal, 209 Android regressions, seven wide methods, 371 JVM readiness checks and 25 accepted-scope original reviews. First Working label moves 840 pixels upward at 100%; the whole first prescription and Add Set now fit. Existing shared section roles are sufficient for this hierarchy change; specialized Set and programming state remains feature-owned. Related: FB-20260910-001/002, DEC-20260903-006, DEC-20260910-016. No schema, backup or release change.

### DEC-20260910-016 — Give day authoring priority through existing presentation roles

- Decision: Keep Routine's single scrolling outline and complete state owner. Guided 5/3/1 setup declares a title, concise explanation and navigation callback through NavigationRow. Optional routine notes use DisclosureRow below the day content, opening by default when authored notes exist. Ordinary Add Exercises precedes placements. Preserve all split choices, notes, contextual creation, programming authority and specialized placement/reorder controls.
- Alternatives/tradeoffs: Per-screen text sizing or scale branches would leave the hierarchy unchanged. A new universal form builder would add architecture without a demonstrated second form contract. Existing navigation/disclosure roles already own consistent surfaces, spacing and semantics. Optional notes require deliberate discovery lower in the outline, while saved notes remain visible when that section is reached. Do not force complex programming/execution records into the simple record builder.
- Status: Verified under FND-20260910-025 / IMP/VER-20260910-016. Normal phone/wide authoring, notes and selected-day recovery, 208 Android regressions, focused/readiness JVM checks and 24 original reviews pass. No persistence/schema/backup/version change. FND-20260910-026 and complete programming/execution remain open.


### DEC-20260910-015 — Extend record roles for Gym catalogs without a second layout framework

- Decision: Exercises, Machines and Categories declare title, full-width context/details, optional Open/Edit, one direct action, overflow commands and reorder state through WhipRecordItem. The renderer owns natural title height, action geometry, information placement and the existing reorder handle; the feature retains ordering, filtering, callbacks, saved drafts and exact repository requests. Track facts keep their existing bounded preview, while complete catalog configuration uses explicit detail lines. Reordering suppresses Open and ordinary actions centrally.
- Alternatives/tradeoffs: Shorter introductions alone would leave three parallel record layouts. A new Gym-only catalog builder would duplicate the existing family. A universal catalog controller would couple presentation to unrelated persistence and history policies. Extend the small record contract and remove the replaced local layout/menu code. Natural title wrapping increases long-record height but preserves names without screen-specific scale patches. Direct Category archive/restore stays one action; Machine configuration/version operations retain their menu and review flow.
- Adjacent discovery correction: FND-20260910-024 requires indexing all machine exerciseIds, retaining active/archived profile search semantics. Reuse WhipSearchField for explicit query clearing without changing Gym's query limits or saved-state ownership.
- Status/compatibility: Verified under FND-20260910-023/024 and VER-20260910-015. Native editing/recreation/reorder/archive/restore/version/history journeys, normal phone/wide original review and affected Track/Review builder regressions pass; 95 production regressions, twelve final focused methods per platform, 371 JVM readiness checks and 45 original reviews provide bounded acceptance. No schema, backup, data-epoch or release change. Routine program/execution bodies remain specialized and their broader journey review remains open.

### DEC-20260910-014 — Active search remains visible and directly clearable

- Decision: A count threshold may hide an unused search field, but never an entered query. Areas retain the existing greater-than-eight threshold until text is entered; their manager and picker preserve the field through smaller option sets. WhipSearchField owns the shared input, full-width single-line layout and conditional accessible Clear Search action. Area and Tag callers declare label, query and callback while retaining their own length limits, filtering and saved-state ownership.
- Alternatives/tradeoffs: Clearing queries implicitly on every mutation would lose intentional context; keeping search permanently visible in every small picker would add unnecessary controls. Separate local clear buttons would repeat the same composition. A small shared input role fixes the demonstrated recovery problem and improves consistent everyday interaction without introducing a collection/domain-state framework.
- Compatibility/status: Verified under FND-20260910-021 / VER-20260910-014 and FB-20260910-001/002. Preserve all mutation receipts, archived identity, history, scope reconciliation and keyboard editing behavior. Normal-scale native and focused controlled-picker acceptance pass on API 34/37, with 86 final-production regressions, eight final focused methods on each platform and 371 JVM readiness tests. Whole-app design/final gates remain open.
- Related recovery decision: FND-20260910-022 shows a mismatched color input when a new-Area name resolves to an existing identity. Reuse WhipColorField with the actual saved color, Saved Color label and disabled chooser. An editable field for an ignored value misrepresents authorship; overwriting the stored color would violate DEC-20260902-008. Keep the new-Area draft privately so changing to a new name restores its intended color.

### DEC-20260910-013 — Give management destinations one header and an earlier list

- Evidence: FND-20260910-020 confirms duplicate introductions in both Organization managers; prior FND-20260907-006 already established a single selected-Area identity owner. The existing management operations and child dialogs provide complete consequence copy.
- Decision: A shared management-header renderer declares title, supporting text, optional primary action/Back and trailing Close. It owns action placement, typography, full-width supporting text and natural height; Create moves below the title when available width requires it. Remove root Your Tags/Your Areas page introductions, preserve Tag search and expose Area count/reorder in one compact list row. Existing child dialogs, full-screen insets, list scrolling and wide master/detail ownership remain intact.
- Alternatives/tradeoffs: Deleting paragraphs alone would leave separate header geometry and squeezed descriptions. Replacing all manager records/forms with one new DSL would exceed the demonstrated benefit and risk domain behavior. Reuse a small header contract for the two observed peers, retaining record-specific controls and all exact mutation coordinators. No data/schema/backup compatibility change is intended.
- Status: Verified in VER-20260910-013: 51 API 34 checks, four API 37 methods, 371 JVM readiness checks and 23 original visual reviews. Real Tag operations preserve all four domain references and saved history; Area identity/rename/reopen remains intact. Broader Area lifecycle and whole-product review remain active.

### DEC-20260910-012 — Goal percentages preserve small progress and exact endpoints

- Evidence: FND-20260910-019 and eight independent GoalScreens integer conversions cover collection/Home summaries, expanded cards, Insights, inspector outcomes, trend data and closure history. Existing domain calculations already retain the fractional progress; DEC-20260831-017 requires closure outcomes to remain frozen.
- Decision: Use one locale-aware Goal percentage formatter with up to one decimal place. Positive progress below 0.1% reads below that threshold; progress above 99.9% but below 100% reads above 99.9%. Exact zero and complete progress retain 0% and 100%. Keep milestone counts, elapsed duration and actual measurement precision as separate domain facts.
- Alternatives/tradeoffs: Integer truncation conceals early progress; displaying many decimals everywhere adds noise; copying Review's normalized-score formatter would conflate a score with a percent. One concise Goal presentation rule removes duplicated conversions without changing calculations, repository state, closure truth or shared item layout.
- Status: Verified in VER-20260910-012: 69 API 34 checks, nine API 37 methods, 371 JVM readiness checks and 20 personally inspected original images. Native Home/collection/insight/closure/archive journeys and recreation preserve the percentage and saved outcome. Ordinary 100% UX remains the priority; broader app audit remains active.

### DEC-20260910-011 — Review cards expose their contributing outcomes before source navigation

- Evidence: FND-20260910-016 reproduces four empty general destinations after nonempty Review totals. Existing per-entity inspectors already expose domain details and archived history.
- Decision: Give all four cards an in-context outcome list using WhipRecordItem. Each row identifies its original entity, outcome date, archive context and normalized contribution where applicable. One presentation projection feeds both daily totals/correlations and detail rows, keeping them aligned. Use a lazy list with bounded reading width, shared Back/exit controls and the existing wide pane/hinge boundary. Preserve Review period/sections, overview scroll and selected details through Activity recreation; only the selected section is saved, never the history payload.
- Alternatives/tradeoffs: Choosing Archived only when all results are archived would still fail mixed histories. Routing every card to Insights would omit archived Habit/Goal data and lose period context. A new universal history editor would duplicate mature source behavior. A read-only contributing list adds one deliberate step before opening an original record, while making the total explainable and retaining existing domain-owned editing/history flows.
- Compatibility/status: Verified in VER-20260910-011: 202 API 34 checks, 13 API 37 methods, 368 JVM readiness checks and 31 original reviews. Domain calculations, Area/global Gym/Track boundaries, availability and historical truth remain intact. No schema, backup or release change. Separate Goal percentage truncation, broader shared reading-width and whole-app review remain active.

### DEC-20260910-010 — Empty Review copy describes the selected view

- Decision: Use No Outcomes in This View and point to the existing Review Options before explaining how new outcomes appear. Keep the shared WhipEmptyState layout. Show card-opening guidance only when outcome cards are actually rendered. Track evidence already explains its distinct role; remove the duplicate global-history-driven empty explanation.
- Alternatives/tradeoffs: Inferring first use from every domain's entire retained history would add calculation and another historical interpretation solely for copy. A second filter toolbar or new empty-state builder would duplicate existing roles. Scoped wording is accurate for both first use and filtered/older history and improves the ordinary journey without architecture churn.
- Compatibility/status: Verified under FND-20260910-015 / VER-20260910-010 with native phone/wide selection/recreation/reopen, neighboring Review cases, readiness and original visual review. No persistence, domain calculation, navigation, schema, backup or version change. Broader source-card navigation and full-app review remain open.

### DEC-20260910-009 — Qualify incomplete Review data and recover within the dashboard

- Evidence: FND-20260910-013 reproduces loading/failed data presented as numeric results or unqualified evidence through the production host. DomainRetryActions already routes retries to the owning ViewModels.
- Decision: One shared status notice names loading and failed sources. Only ready selected outcomes and ready global Track evidence enter the dashboard; correlations use only ready selected outcome sources. The retry action targets failed included sources, preserving the open Review and current period/section choices. Empty-history copy requires all selected outcome sources to be ready.
- Alternatives/tradeoffs: A full-screen loader would hide useful available progress. One warning card per source would dominate the normal phone view. Keeping failed values beside a warning would still permit incomplete results to look authoritative. A single notice plus current-source filtering provides a compact honest dashboard; absent cards can return after recovery. Tracks remain global evidence and excluded from scores/correlations.
- Compatibility/status: Verified in IMP/VER-20260910-009. Reuse WhipNoticeCard and existing source callbacks; no new persistence coordinator, schema, backup or version change. Native state/retry/section/restoration, available-source correlations and phone/wide rendering pass. Complete Review/app remains open.

### DEC-20260910-008 — Review reflects retained history and exposes controls without dominating progress

- Decision: Preserve archived Task/Habit outcomes, honor All Tracks across Area scope, and make Track-only evidence discoverable from Home. Review's global Track action temporarily opens All Areas using the established restoration mechanism; the saved productivity Area stays unchanged. Keep Track values separate from comparable outcomes and correlations.
- Layout: Compact Review uses one persistent disclosure with the current period, sections and productivity Area; detailed controls expand in place. Wide Review retains its dedicated control pane. Existing chart/evidence surfaces remain domain-specific.
- Wide refinement: FND-20260910-014's measured one-pixel overflow justifies replacing independently rounded FlowRow widths with explicit weighted rows. Column count and reading order remain the same; the renderer owns residual-pixel distribution instead of caller patches or epsilon offsets.
- Alternatives/tradeoffs: Relabeling All Tracks as selected-Area data contradicts the explicit evidence boundary; changing the durable Area on drill-down would disturb the user's workspace. Counting only active definitions makes cleanup rewrite progress. A complete dashboard rewrite is unnecessary; shared disclosure removes the demonstrated opening-view obstruction at the cost of one extra action to change compact options.
- Compatibility/status: Verified under FND-20260910-010/011/012/014 and IMP/VER-20260910-008. Final native phone/wide journeys, 196 Android neighbors, readiness and 20 original image reviews pass. Saved data, section/period preferences, normalized outcomes, neutral dates, immutable completed Gym facts and correlation exclusions remain intact. No schema, backup or version change. FND-20260910-013 partial-data handling and complete Review/app acceptance remain open.

### DEC-20260910-007 — Execution builders own Set reading order while domain code owns outcomes and actions

- Decision: Replace four separately assembled Set bodies with WhipExecutionItem: identity, optional leading/action slots, values, status, target, supporting evidence and specialized inputs. Keep the existing passive/active surface distinction and geometry. One Gym outcome formatter supplies classification, completion/removal and effort; History keeps its explicit Not performed fallback and its extra evidence.
- Alternatives/tradeoffs: A local Removed-string fix leaves the recurring hierarchy with four owners. A universal record/form builder would blur active input and read-only evidence and impose unrelated state policy. This bounded family consolidates the existing successful Set design while correcting a demonstrated outcome inconsistency; omission cards gain contextual identity and a distinct status line.
- Compatibility/status: Verified under FND-20260910-008/009 and IMP/VER-20260910-007. Refines DEC-20260907-012/013 without changing persistence, progression, exact boundaries, timers, reorder or available actions. The real optional-Set Undo/draft/recreation/completion/History journey, 177 phone neighbors, four wide methods and readiness pass. Builders own the four recurring item families; whole-app review and justified specialized bodies remain active.

### DEC-20260910-006 — Settings items declare their interaction and information roles

- Decision: Introduce WhipSettingItem over existing theme/spacing tokens. Each item declares exactly one toggle, choice or typed edit plus optional explanation. The renderer owns a shared label/control header, a distinct complete current-value line when applicable, full-width explanation, natural height, single interactive semantics owner and identity-scoped choice menu. A down arrow opens choices, a pencil opens typed editing, and a switch changes a Boolean.
- Alternatives/tradeoffs: Tuning each existing row preserves three independent layouts; making every value a modal introduces unnecessary friction; converting Settings navigation into a new wrapper adds no demonstrated benefit. The setting builder improves normal reading without changing the typed coordinator or selection callback authority. Choice rows lose their input-like outline and gain the same inset/type hierarchy as adjacent preferences. Long values/explanations may grow naturally.
- Adoption/compatibility: Route SettingsDropdown and TransactionalSettingsField through the builder; WhipSettingsRow becomes the existing Boolean entry point over its toggle role, including Home/Health/editor uses. Preserve public labels, tags, callbacks, enabled state, exact draft/source identity, persistence receipts and menu selected-state meaning. Form-only SelectionField and actual navigation/action rows remain their distinct roles.
- Status: Native and normal-scale visual acceptance verified under FND/IMP/VER-20260910-006 and FB-20260910-001/002: 147 API 34 checks, five API 37 methods and 25 personally inspected originals. Broader builder adoption and whole-product acceptance remain open. No release/schema/backup/data change.

### DEC-20260910-005 — Selected elapsed units stay visible in collection summaries

- Decision: Goal collection and Home summary text use the complete authored ElapsedDisplay label in the shared full-width information row. Remove the unused three-part overview formatter after its only production caller migrates. Preserve the restrained bodySmall role and existing natural wrapping.
- Alternatives/tradeoffs: Retaining the three-part overview requires expansion to see deliberately selected smaller units and diverges from editor/Insights/support-pane meaning. Reintroducing a larger metric band would complicate the newly unified summary grammar. Complete text can add one or two lines for broad configurations; that modest height is justified by authored information and current full-width layout.
- Supersession/compatibility: Explicitly supersedes DEC-20260907-009's remaining three-part elapsed summary rule, following FB-20260906-009/010 and FB-20260907-015. No arithmetic, exact instant, configured selection, resets, schema, backup or mutation changes. Normal-scale source/native review must prove the benefit; one strengthened existing large-text contract is a shared responsiveness check, not a new per-screen campaign.
- Status: Verified for collection/Home elapsed summaries; FND/IMP/VER-20260910-005 under the ongoing whole-app objective. Normal native phone/wide review shows all six configured units fit the current status role; broader app acceptance remains open.

### DEC-20260910-004 — Productivity builders own complete expanded reading order

- Decision: Replace the seven production header call sites with a scoped productivity-content builder over the existing WhipItemCard. Declare summary/details text through one typography role, Area, notice, disclosure, primary action and specialized expanded content. Render the complete information at full width, then one full-width Edit footer for disclosed items. Undisclosed archived/insight rows keep their appropriate direct header edit or no edit.
- Alternatives/tradeoffs: Removing only the duplicate timer leaves split ownership and width-dependent Edit placement. Moving Edit into the header would add a third control and crowd titles. A footer makes information continuous and the edit location predictable; long expanded bodies may require scrolling to it. Existing domain inputs, progress/elapsed metrics and inline actions remain specialized content.
- Compatibility: Preserve caller-owned selection/reorder gestures, container state colors, saveable disclosure keys, callbacks, completion/timer/milestone boundaries and exact persistence. Collapsed summaries are replaced by richer detail without duplicated facts. Remove the unused production persistent-summary branch while migrating its old synthetic test to the actual summary/details contract.
- Scope/status: Verified for the seven migrated call sites; FND/IMP/VER-20260910-004, FB-20260910-001/002 and FB-20260907-015. Refines DEC-20260907-011's expanded Edit placement, preserving its full-width information and centered header needs. Normal-scale phone/wide native journeys, neighboring behavior and 33 original reviews support this scope; broader adoption remains open.

### DEC-20260910-003 — Build item families from semantic content and action roles

- Decision: Introduce a small record-item builder over existing WhipItemCard geometry and shared edit/overflow controls. Features declare title/identity, context, labeled facts, edit and named commands. The renderer owns typography, reading order, spacing, optional-content collapse and menu state keyed to record identity. Date precedes context and facts in both Track record entry points.
- Alternatives/tradeoffs: Patching each local Row preserves drift; replacing the theme or imposing one universal body on all domains adds unnecessary scope. Role-specific builders can extend the existing productivity/navigation/execution patterns incrementally. Feature state, history, drafts and repositories remain caller-owned.
- Reading policy: Context wraps naturally across the card width; supporting facts use a shared three-line preview with full values still available in the inspector. Title remains a two-line summary. Natural height replaces caller-specific row sizing; list scrolling and insets remain screen responsibilities.
- Scope/status: Verified for the first Track Entries and Activity pilot with normal phone/wide validation; remaining family adoption is In progress. No per-screen 200% campaign. FND-20260910-003 / VER-20260910-003; rollout and remaining families in docs/quality/WHIP_ITEM_BUILDERS_2026-09-10.md.

### DEC-20260910-002 — Keep Track bulk selection within its displayed collection

- Alternatives: Retain cross-view selection with explicit hidden counts and management, as Tasks does for its richer filtered bulk workflow; or retain only selected Tracks still in the current collection. Clearing all selection on any Area change would unnecessarily discard visible choices.
- Decision: Derive one visible selected set for summary, pin intent and action IDs. Prune hidden IDs after a successful source load; disable actions while loading or failed. Explain that only Tracks in this view stay selected. Preserve visible selection and recreation, and remove hidden selection permanently when narrowing Areas or changing active/Archived collection.
- Benefit/tradeoffs: Directly bounds simple Track pin/archive/restore actions without adding a cross-view selection manager. Selecting across Areas remains available in All Areas. Tasks keeps its intentional hidden-selection contract. No persistence, schema or domain behavior changes.
- Related/status: Verified under FND-20260910-002 / VER-20260910-002 / FB-20260908-006. Scope remains the simple Track collection workflow.

### DEC-20260910-001 — Explain retained Scale conflicts where users can correct them

- Context: FND-20260910-001 confirms late generic rejection despite a live history projection being available to the Field editor.
- Alternatives: Improve only the parent error, or show live Field-local feedback while retaining the final transactional check. The local check avoids two submissions and reopening for a known conflict; the parent guard still handles concurrently added history.
- Decision: Share a bounded selectable-value compatibility helper with existing normalization. Generate the candidate choices once for a history check, identify the first incompatible saved value, associate it with the relevant bound or increment and disable invalid Field submission. Keep raw drafts and validate again at repository commit. Late errors name the Field and value.
- Compatibility: No reinterpretation, rounding of history, automatic correction, schema/version change, or reliance on UI validation for persistence safety. Preserve tolerance and every currently accepted Scale value.
- Related/status: Verified in IMP/VER-20260910-001; FB-20260908-006 / VER-20260910-001.

### DEC-20260909-029 — Keep history controls reachable and sorting valid as Fields change

- Context/evidence: FND-20260909-035 proves inaccessible Choice conditions and a live removed-Field crash. The existing two-stage filter workflow supports all seven types and explicit draft Apply/Cancel.
- Alternatives: Replace filters with a new full-page builder, or repair the existing bounded dialog and its live selection contract. The latter retains efficient short conditions and avoids new navigation/state ownership without sacrificing large-form access.
- Decision: Make the condition body scroll and use established focused-input relocation for text/Number controls. Resolve sort display and behavior against the current Field graph, clear an invalid retained ID and preserve the selected built-in sort/direction. Keep the sort body scrollable for small enlarged layouts.
- Constraints: Do not reinterpret saved values, alter exact paging content ownership, weaken repository mutation review, or claim unmeasured performance gains. Applied-filter clarity and nested-condition restoration are exercised separately before any further change.
- Nested recovery: FND-20260909-036 confirms the completed condition returns. Reuse the proven Field-editor pattern: a Track-scoped SaveableStateHolder, a new key per opening, and synchronous removeState on Add/Cancel. Preserve active-condition recreation.
- Applied-filter reading: FND-20260909-037 confirms that technically correct results lack their defining criteria. Share only the summary role between Entries and Track Insights; keep their filter state independent. Show the combination rule and values/units, with bounded multiline chips and explicit remove semantics. This completes the existing workflow without replacing its builder.
- Small keyboard refinement: The API 26 original shows the nested unit-name/symbol label wrapping above Maximum. Use the configured unit symbol directly in the input label, falling back to the unit name when it has no symbol. Field identity and canonical conversion remain unchanged; final focused platform tests cover the shorter label.
- Related: FB-20260908-006 / VER-20260909-032.
- Status: Verified in VER-20260909-032.


### DEC-20260909-028 — Bound removal reading while keeping its decision explicit

- Context: FND-20260909-034 shows unreadable small enlarged warning/destination and excessive empty space in ordinary final review.
- Decision: Reuse the established scrolling-heading/body pattern for draft removal and include the exact review heading in its lazy reading owner. Keep footer actions fixed, allow natural bounded height, and opt this replacement selector into wrapping its complete selected value. Generic selection defaults remain unchanged.
- Alternatives/tradeoffs: Removing the draft confirmation would discard nested Field authorship without a review or Undo. Removing final review would weaken explicit historical-data consent. Another wizard adds navigation and state ownership. A duplicated destination summary adds competing information; wrapping the authoritative selection is clearer. Long content still scrolls and exact review/commit safeguards remain mandatory.
- Compatibility/status: Verified under VER-20260909-031. No data/schema/backup/version change.

### DEC-20260909-027 — Submit one canonical Track draft and reveal each rejected attempt

- Context: FND-20260909-031/032 reproduce a valid-but-unfinished Save and offscreen parent validation. The ordinary native destructive journey separately verifies review/replace/recreation/stale-impact rejection and exact final mutation on current production.
- Decision: Keep exact draft equality as the review ownership guard. At explicit valid Save, install canonical content into editor state before requesting review; if it differs, invalidate the old removal review and obtain a fresh one. Retain raw invalid/in-progress authorship until that boundary. Track validation attempts separately from their message so every rejected attempt reveals the current summary.
- Alternatives/tradeoffs: Dropping draft equality risks accepting responses for different authored content. Comparing independently normalized live/response drafts adds a second interpretation path. Sending raw content while separately validating canonical content keeps two different submitted states and unresolved Area naming. Canonical submission uses the existing validation boundary and exact state ownership, without a wizard or new persistence model.
- Compatibility/status: Verified under VER-20260909-031. Preserve canonical domain acceptance, IDs/history, response/session ownership, stale-impact rejection and explicit destructive confirmation. No schema, backup or release change.
- Nested ownership: FND-20260909-033 requires a SaveableStateHolder scoped to the parent Track session and a separate key for each Field opening. Remove that key synchronously on Save/discard/deletion, including before composition teardown. A key alone would prevent some reuse but leave closed state retained; an explicit owner/removal lifecycle preserves active drafts and releases completed ones.

### DEC-20260909-026 — Validate Field configuration at its editing controls

- Context: FND-20260909-030 reproduces late duplicate-Choice rejection and misleading Scale error attribution in real existing-history editing.
- Decision: Keep nested Field editing and repository authority. Reuse canonical Choice normalization/duplicate detection, mark affected options and block invalid Field submission; put bound/increment errors on their respective inputs. Preserve raw drafts during correction/recreation and describe unit changes by their actual effect on new input and saved history.
- Alternatives/tradeoffs: A wizard adds steps and recovery ownership without improving these local corrections. Whole-Track-only validation causes avoidable backtracking; separate normalization rules risk UI/repository disagreement. Shared narrow label logic and local error ownership solve the observed causes without altering definitions, identity or historical values.
- Adaptive detail: Bounds stack below 360 dp of content width or at font scale 1.5 and above. Reuse focused-input relocation for bounds, increment and Choice labels; keep short local messages and fixed actions. Long forms scroll, including the complete Number history explanation.
- Verification/compatibility: Ordinary/actual-200% native editing, persisted Choice identities and complete Entry values, type/dimension locks, Scale/default-unit save/reopen and neighboring regressions pass. No schema, backup format or release change. Status: Verified in IMP-20260909-029 / VER-20260909-030; whole Tracks and product acceptance remain open.

### DEC-20260909-025 — Show one interpreted CSV Entry within the existing review

- Context: FND-20260909-029 confirms that the current review exposes mapping configuration and validation totals without the resulting values. Existing exact batch, file and form ownership is sound and remains required by DEC-20260901-022.
- Alternatives: Keep counts and add more explanatory copy; build a separate multi-step import wizard; render every valid Entry; or provide one browsable interpreted Entry in the existing scrollable review. Copy cannot reveal wrong-but-valid mappings. A new wizard adds navigation/recovery complexity, while rendering up to 5,000 Entries and large values at once is unnecessary.
- Decision: Keep one import review and its current mapping/retry/commit lifecycle. Add a read-only Entry preview with previous/next navigation, position/count, effective Entry date and each Field's interpreted value. Resolve choices/units against the same frozen form that produced the draft. Use lazy Field rows and retain only a selected integer index across recreation; reset selection when file/mapping revision changes. Never retain a second draft copy, modify imported values or substitute current live definitions.
- Product detail: Make blank values distinct from No, preserve readable multiline text and numeric units, and keep import actions fixed. Clarify validation pluralization and relevant copy within this flow. Preserve the early error/replacement priority from DEC-20260909-007. Native compact/wide and enlarged review must justify the final hierarchy before acceptance.
- Large-text payload policy: Preview at most 1,000 Unicode code points of a displayed value and explicitly label any shortened excerpt; the complete draft and import remain unchanged. This prevents a valid multi-megabyte CSV cell from being eagerly rendered simply to check its mapping. It is preview disclosure, not a new authored-value limit.
- Result reading: The first smallest/200% candidate successfully imports but retains a lower review position, hiding the completion receipt above the viewport. Reset the existing lazy reading position when completion or an authoritative recovery/error message arrives. Keep normal Entry browsing and mapping scroll intact. The native journey must see the completion without scrolling for it.
- Related/status: Verified in IMP-20260909-027 / VER-20260909-028 under FND-20260909-029 / FB-20260908-006. No schema, portable-format, import-receipt or data-compatibility change. Complete Tracks and whole-product acceptance remain open.

### DEC-20260909-024 — Preserve one compact Entry deletion review across recovery

- Context: FND-20260909-028 reproduces both unbounded hidden payload and a newer revision replacing the user's original review after process death.
- Alternatives: Reuse the new editor's private checkpoint storage for the full delete snapshot; retain only an Entry ID and fetch its current state; or save the small exact deletion authority and presentation once. Full checkpoint storage adds file ownership/failure handling for content the review never needs. ID-only refresh repeats the confirmed unsafe revision substitution.
- Decision: A deletion candidate contains exact Entry/Track identities and semantic revision tokens, date, populated-value count and bounded Unicode-safe display excerpts. Retain the existing repository deletion API and transaction checks; remove editor-only Field/Choice/unit contract arrays from this deletion-only boundary because deletion compares their canonical semantic tokens. Do not copy Entry values, full forms or Track metadata into saved review state. Initialize preparation only while no original candidate exists, and never replace a restored candidate with a new preparation result.
- Benefit/limits: Recovery stays faithful to the original review, aggregate review state is independent of note/schema payload size, and no extra private-file lifecycle is needed. Display excerpts do not truncate stored history or editable values. A changed Entry must be kept and reviewed again before deletion; existing owned outcomes and exact same-process Undo remain. Repository regression and actual process-death evidence must verify the compact boundary rather than assuming token equivalence.
- Recovery UX: Final runtime review exposes misleading generic “Save Didn't Finish” copy and a useless repeat Delete after a confirmed conflict. Use one deletion-specific title/message and a Review Entry action only when the current projection still has the original Track/Entry UUIDs. It closes the old review and opens current read-only history; missing/reidentified targets can only close. Ordinary transient failures retain retry against the same original boundary.
- Adaptive reading: The smallest 200%-text original clips unscrollable Undo copy. Reuse WhipDialogHeading inside one vertical body scroll, retain fixed actions, and key the reading position to the error state so a conflict starts at its beginning. Keep the supplemental compact Undo-reading frame outside the canonical catalog because it is a scroll position of the same review and adds no distinct surface on taller windows.
- Related/status: Verified in IMP-20260909-026 / VER-20260909-027 under FND-20260909-028 / FB-20260908-006. No schema, portable-format or release change. Complete Tracks and whole-product acceptance remain open.

### DEC-20260909-023 — One Track editor session owns a private recovery checkpoint

- Context: FND-20260909-027 reproduces both oversized active saved state and completed draft accumulation. Limiting only one input cannot bound the aggregate route/Field/Entry graph, and current historical values must remain intact.
- Alternatives: Reinstate arbitrary text truncation; compress the Android bundle; spill the whole Activity Parcel; or consolidate Track-specific recovery ownership with private checkpoints. Truncation loses authorship, compression cannot bound incompressible data, and whole-Activity Parcel persistence couples unrelated UI/platform state to an unsuitable durable format.
- Decision: Keep the existing pure definition/Entry state holders inside one Activity-scoped Track editor session. The session owns root routing and snapshots both holders with the exact opening route. Save an immutable, checksummed, versioned checkpoint in app-private no-backup storage and put a small reference in SavedStateHandle. Write checkpoints on Android state saving, not on every keystroke. Reuse unchanged checkpoints, retain the active session's referenced snapshots, and clear them when that session is explicitly completed/discarded. MainActivity has no single-instance launch constraint, so never prune other owners' files: they may belong to another retained Activity. Cleanup may remove only this owner's unreferenced checkpoints after restoration or its entire explicitly closed session.
- Failure/compatibility: A missing/corrupt/incompatible checkpoint must present recovery failure and preserve saved history; it cannot silently become a fresh editor. A checkpoint write failure keeps the live draft and must be disclosed. Data-generation checks and exact Track/form/Entry mutation boundaries remain authoritative. This is private transient recovery state, excluded from portable backups, with no Room/schema or historical-data rewrite.
- Acceptance: Bound actual Activity state for a large accepted value and repeated completed sessions; restore the complete route, raw inputs and exact opening boundaries through a newly constructed owner and an actual background/process-recovery journey. Exercise corruption/write-failure, discard/completion cleanup, ordinary/enlarged authoring neighbors and API endpoints before acceptance. Keep complete Field/CSV/product review open.
- Reset/restore refinement: The private file graph must participate in existing data-generation invalidation. Advance generation and remove all previous checkpoints under the application's generation monitor; checkpoint read/write uses that same monitor and rejects stale owners before accessing disk. This prevents a retained old editor from recreating files after Reset or replace-restore. Compact references carry their generation so intentionally invalidated drafts close without a false recovery error. Add this guarantee to the existing exclusive-reset integration test.
- Status: Verified in IMP-20260909-025 / VER-20260909-026. Measured state, actual process recovery, exact content/boundaries, failure truth, cleanup and generation invalidation pass proportionate checks; no whole-Track/app acceptance is implied.

### DEC-20260909-022 — Track text edits preserve the complete accepted value

- Context: FND-20260909-025 proves data loss from input-only character ceilings that CSV/domain/repository contracts do not share.
- Alternatives: Enforce those limits on all imports and historical edits; grandfather each original length while rejecting growth; or remove the unilateral input truncation. Retrofitting the limits rejects already-valid history and changes import semantics. Grandfathered lengths allow corrections but arbitrarily prohibit additions to the same saved note. Neither limit is an established domain contract or a total draft-memory budget.
- Decision: Pass the complete proposed Short/Long Text value to the existing Entry draft owner. Keep single-line/multiline presentation, exact form/Entry concurrency protection, explicit Save, CSV receipts and established outer-whitespace normalization. No schema, migration, stored-data rewrite or new arbitrary character policy.
- Acceptance/limits: Reproduce the old loss, then verify complete same-length edits, supplementary-Unicode additions, recreation, save/reopening, unchanged neighboring values and supported API endpoints. This fixes silent truncation; it does not establish unlimited performance or extreme saved-state capacity. Continue realistic large-value/draft review under the whole Tracks audit.
- Status: Verified in IMP-20260909-024 / VER-20260909-025. Both complete preservation/addition journeys pass on API 26/34/37; 64 JVM / 67 distinct Android neighbors, 344 JVM readiness and both check-wrapper fixtures pass. Extreme-value capacity and full import/recovery acceptance remain open.

### DEC-20260909-021 — Prioritize recording structure in a readable Track authoring column

- Context: FND-20260909-023 shows generic guidance and optional appearance delaying the core form, plus wide single-column controls spanning the whole window.
- Alternatives: Only shorten copy; split arbitrary Fields into multiple columns or a new stepped flow; or retain existing authoring/state ownership while reorganizing priorities and bounding the complete header/body. Copy alone leaves wide control distances and optional metadata ahead of Fields. A stepped or automatic multi-column flow adds navigation/ordering complexity to arbitrary ordered Field types without evidence it improves this task.
- Selected representative: Keep the full-screen overlay and responsive WhipEditorHeader, center a form up to 720 dp wide, put Track Name and Entry Fields before optional Description/Emoji, and replace the Entry's repeated headline/instructions with concise Track context. Keep existing organization, value-type controls, fixed commit/exit, draft owners and nested configuration. Context remains semantically complete when visual wrapping is bounded.
- Benefit/tradeoff: Core input and field setup appear earlier; related labels, editing controls and commit remain nearby on wide screens. Wide layouts intentionally retain side whitespace instead of stretching serial fields. The order of optional Track metadata changes; no capability, data/schema, validation rule or navigation step is removed.
- Acceptance: Inspect actual opening, keyboard, Field setup, validation, Date, saving and reopening at ordinary/200% text on small/phone/wide devices. Require complete first Entry inputs and focused primary names/labels, nearby header/body geometry and unchanged typed persistence/recovery. Extend the pattern to other forms only after this representative flow is accepted and their distinct needs are reviewed.
- Status: Verified for this representative Track/Entry composition in IMP-20260909-023 / VER-20260909-024. Ordinary/actual-200% full journeys pass on API 26/34/37, including initial/focused native inputs, aligned bounded forms, typed persistence, draft recovery and metadata reopening. All 83 retained originals have scoped review. Other forms require their own source/journey assessment before propagation.

### DEC-20260909-020 — Publish a coherent transactional Track graph

- Context: FND-20260909-022 reproduces mixed Track/Field revisions and Entries without committed required values in the live Flow. UI delays or filtering only missing children cannot establish a coherent revision and could hide valid optional/empty states.
- Decision: Follow the existing Gym invalidation/snapshot pattern. Observe all five Track tables, bulk-read their rows inside one transaction, then assemble domain projections outside the transaction. Keep current SQL ordering and public per-table Flows. CSV export and mutation boundaries retain their existing transactional implementation.
- Tradeoff: Every relevant invalidation reloads five whole-table lists, including unchanged tables. This replaces independently scheduled table reads and avoids per-Track query multiplication; projection assembly already materializes the entire Track graph. Large-history responsiveness remains a separate audit requirement. Do not introduce a schema, polling delay or UI-specific synchronization rule.
- Acceptance: Every collected definition/Entry update must remain coherent during public writes, including concurrent observation and rapid renaming. Verify live collection order, existing Track repository/definition/Entry/CSV/workspace behavior and full real-app authoring/recovery. Check the supported platform endpoints and affected static/build/readiness gates.
- Status: Verified in IMP-20260909-022 / VER-20260909-023. Both consistency/order regressions pass on API 26/34/37; 64 JVM / 64 Android neighbors, 344 JVM readiness tests and check-wrapper fixtures pass. No schema or stored-data contract changes; complete large-history responsiveness remains open.

### DEC-20260909-019 — Date selection keeps full wheel geometry in a scrolling body

- Context: FND-20260909-021 reproduces failure to settle in a short enlarged real-app Date picker; its non-scrolling body constrains the nominal three-row wheel viewports.
- Alternatives: Shrink wheel rows/text, force a different date input mode, or let existing content scroll inside the already bounded dialog. Select scrolling content: it retains native text size, existing calendar/wheel interaction and explicit Set/Cancel while removing the wheel measurement constraint. Secondary actions may require scrolling on short screens; their meaning and availability remain intact.
- Acceptance: The complete ordinary/enlarged Track journey must settle, retain today's opening selection, show the full native 144 dp Year wheel when brought into view, and persist the chosen Date through recreation. Check existing Date picker and neighboring editor suites, plus API 26/34/37 rendered states. No stored date, timezone, calendar or locale semantics change.
- Status: Verified in IMP-20260909-021 / VER-20260909-022. Ordinary/actual-200% complete journeys pass on API 26/34/37 with stable selection, native wheel bounds and typed persistence; 110 neighboring Android checks and readiness pass. Short screens may require scrolling to the secondary controls.

### DEC-20260909-018 — Use the available nested Field body for focused input

- Context: FND-20260909-020 confirms that the small enlarged Field dialog clips its focused floating label. The body is reduced a second time by a 72% height constraint.
- Alternatives: Replace the Field editor with a separate full-screen flow; move its title into scrolling content; or retain the existing dialog and use its available body with the shared whole-input relocation helper. Begin with the last option: it removes the avoidable constraint while preserving the familiar title, fixed actions and nested draft ownership. A separate flow introduces navigation changes without evidence they are needed; a scrolling title remains a fallback if the measured body still cannot fit one input.
- Tradeoff/acceptance: The Field dialog can occupy more of its existing 92%-bounded window, giving configuration and focused input more space. Keep native font scaling, complete field/label bounds and Cancel/Save above the keyboard. Verify full authoring, typed persistence, discard/recreation and ordinary/enlarged API 26/34/37 renders. Do not change shared window policy or persisted Track contracts.
- Representative evidence: Using all available height passes the native Field/label check, but the API 26 enlarged card extends behind status icons. Select the bounded-height alternative with the title inside the scrolling list, preserving its accessible pane identity and fixed actions. Keep whole-input relocation and the existing 72% body bound. A later Date-picker idle timeout in that run occurs at Entry authoring, after Field configuration has succeeded; it does not establish a Field sizing loop.
- Status: Verified with the bounded scrolling-title alternative in IMP-20260909-021 / VER-20260909-022. Final ordinary/enlarged API 26/34/37 journeys preserve complete Field Name/label, native font size, fixed actions, draft recovery and typed persistence. Shared window policy remains unchanged; broader form composition remains open.

### DEC-20260909-017 — Short phone Track detail prioritizes its local reading flow

- Context: FND-20260909-019 confirms that the 320×533 dp/200%-text archive opens with barely any record content. Its fixed global Area/search bar, named two-row app navigation, Track identity and two destination levels spend more height than the history itself.
- Alternatives: Shorten only the archive copy or reduce spacing; move restoration back into Options; or give the existing inline detail a focused presentation in short phone windows. Copy/spacing alone cannot recover enough height for a complete two-line record and date. Hiding Restore would undo its useful contextual placement. Select a focused detail presentation when the compact content window is shorter than 600 dp, retaining a clear Back route, compact Track identity and local destinations while the collection restores the full named app navigation.
- User benefit/tradeoff: Saved records become useful immediately and remain readable during browsing. Switching app areas or global scope/search from this short detail requires Back first. The existing local Entry search remains direct, and active Tracks must retain a local Add Entry action because the global Add bar is absent. Normal phone and wide layouts keep their existing navigation. This is a bounded child-flow exception to DEC-20260902-005, not an icon-only replacement for named navigation.
- Ownership: Use stable content parents across size, keyboard and selection changes. Keep current Track selection, destination, query, archive/restoration and historical records. A missing selection must never suppress the global navigation without a local exit.
- Acceptance: Complete first archived record title/date without preliminary scrolling on the short enlarged device; native identity/Back/query bounds; local Add Entry after restoration; Back restores Archived and named primary destinations; recreation and saved-history preservation; ordinary/large API 26/34/37 and affected neighboring/build checks.
- Status: Verified for the bounded short-detail flow in VER-20260909-021. All three platform journeys preserve navigation, active Add Entry, local destinations and exact history; broader Track design and normal-use validation remain open.

### DEC-20260909-016 — Inline inputs own keyboard space without panning the application

- Context: FND-20260909-018 shows a partly covered native query and Track identity beneath status icons. The activity leaves adjustment unspecified; the content Scaffold imports but does not apply IME padding. Persistent wide navigation intentionally stays outside keyboard avoidance. At 200% text the short Track detail also spends much of its body on a two-line headline and repeated count/Area metadata.
- Alternatives: Move local Track search into another modal; force a local window flag while each input is focused; or give the existing activity/content boundary explicit inset ownership and adapt the short detail header. Another modal adds a separate search workflow; per-input flags duplicate window policy. Select explicit activity resize with IME avoidance at the content Scaffold, keeping the rail outside that modifier. Evaluate hiding global content chrome during inline typing so the query and records have useful space; retain local identity and Back.
- Representative design: Short Track detail uses its existing compact title treatment and omits repeated header metadata where it consumes reading space. Keep the same composition parents for query and history across size/keyboard changes. Preserve local drafts, selected destinations, archive capabilities and saved records.
- Evidence-driven extension: Both native Task Quick Capture scales fail whole-field visibility after settled input. Share the viewport-driven whole-field visibility behavior between Track search and Task Quick Capture. While Quick Capture owns the keyboard, omit its repeated destination introduction and list toolbar; selected workspace navigation remains, and the introduction/tools return on keyboard dismissal. Track's width-based Edit shortcut remains available on short screens where it fits. These changes preserve exact submission and draft ownership.
- Copy refinement: The first shared-layout pass exposes the enlarged multiline “Quick Capture to Today” floating label clipping above its field. Use concise “Task for Today” / “Task for Inbox”; retain the destination meaning and explicit native complete-label assertions. The existing destination-specific empty state and creation defaults remain intact under DEC-20260904-005.
- Platform basis: [Android's inset guidance](https://developer.android.com/develop/ui/compose/system/insets-ui) places avoidance at the appropriate content owner and describes consumption that prevents duplicate keyboard padding. Existing ImeNavigationRailE2ETest remains a required neighbor; a passing Compose node assertion does not establish native visibility.
- Acceptance: Native whole query/label, identity/Back and reachable results above the keyboard; a complete older Entry title/date on the short enlarged screen; real query/recreation/restore on API 26/34/37; neighboring inline Task, native editor/Search, adaptive and navigation checks. No schema, historical data or release change.
- Status: Verified for the bounded native input and short-record flow in IMP-20260909-018 / VER-20260909-019. All four journeys pass on API 26/34/37, with 64 JVM / 109 Android neighbors and 344 JVM readiness checks. Whole Tracks and the visual follow-up FND-20260909-019 remain open.

### DEC-20260909-015 — Archived history uses a compact status with contextual restoration

- Context: FND-20260909-017 shows that a prominent empty-state explanation spends scarce height ahead of populated history, especially at 200% text.
- Alternatives: Shorten only the existing centered hero; replace the entire nested Track navigation; or replace this status with a compact, wrapping status/action row. Shorter hero copy still carries oversized empty-state spacing. A navigation redesign reaches unrelated active/history/Insights behavior before its complete review.
- Decision: Use concise “Archived · Read-only” text and the existing Restore Track command in one naturally wrapping row above history tools. Use ordinary body typography and established 48 dp text-button behavior. Restoration follows the same ViewModel/repository action as Options; all Entry controls remain governed by the persisted archived state. Keep Options restoration available.
- Benefit/tradeoff: Records and scoped search appear earlier, and restoration is available where the restriction is visible. One useful contextual shortcut replaces long instructions; no new domain capability or generic notice component is needed. No persistence, data, schema or release change. Existing archived read-only guarantees in DEC-20260908-001 remain in force.
- Representative-flow refinement: The compact status alone does not give the API 26/200%-text query a visible native target. When a single-pane Track detail has less than 440 dp of workspace height, omit the outer workspace tabs and retain the existing Back, identity and inner destinations. Collection pages and wide split panes keep workspace tabs. This bounded adaptive change is preferable to shrinking text or replacing all navigation; the tradeoff is one Back step to another workspace destination on short detail. Selection, local draft ownership and archived routing remain unchanged.
- Related: FND-20260909-017, FB-20260908-006, VER-20260909-016.
- Status: Verified in IMP-20260909-017 / VER-20260909-018 for archive status/restoration and bounded short-detail navigation. Native API 26/34/37 journeys, neighboring/readiness checks and scoped original-image review pass. FND-20260909-018 retains the remaining native keyboard/very short enlarged-history reading constraints for the next improvement.

### DEC-20260909-014 — Track Insights share unit-aware numeric presentation

- Context: FND-20260909-016 proves canonical measurements are mislabeled as display units in workspace summaries; per-Track presentation has its own conversion and precision path. A temperature Sum also applies an affine offset to an aggregate, and workspace Scale totals imply an additive meaning absent from per-Track Scale Insights.
- Alternatives: Patch only the missing workspace conversion; replace the Insights pages with a new analytics model; or share a small presentation contract while preserving existing evidence, navigation and aggregation boundaries. The first leaves precision and offset inconsistencies; a new analytics product adds unnecessary disruption.
- Decision: Use one numeric presentation helper for Number/Scale Insights. Convert canonical Number values to the Field's current unit, honor Number precision, retain enough fractional places for Scale averages and configured increments, and format locale correctly. Differences divide by the conversion factor without applying an absolute offset. Missing/nonfinite values render as unavailable. Totals remain for Number quantities with zero-offset units, excluding Temperature; Scale and temperature readings retain averages/ranges/latest/trend without a misleading total.
- Benefit/tradeoff: Equivalent evidence reads consistently across workspace and per-Track summaries. Omitting non-additive totals removes a misleading statistic while preserving recorded values and useful comparisons. No Entry mutation, unit definition, persisted aggregation, schema, migration or release change is needed. Date-window and broader Track design findings remain separate follow-ups.
- Related: FND-20260909-016, FB-20260908-006, VER-20260909-016.
- Status: Verified in IMP-20260909-016 / VER-20260909-017 with mixed-unit/temperature/Scale journeys, neighboring regression, native API 34/37 image review and readiness. Broader Tracks review remains open.

### DEC-20260909-013 — Choose dialog status icons from their actual backdrop

- Context: FND-20260909-015 proves that light content can have a darkened status backdrop. A single content-theme flag cannot represent both the dimmed exterior and Search's opaque painted inset.
- Alternatives: Reducing all dialog dimming would weaken modal focus throughout Whip. Painting full backgrounds or replacing window/inset layouts would change the existing overlay and keyboard behavior. Copying local icon fixes would repeat policy across owners.
- Decision: Let the shared dialog boundary use light status icons over its existing dimmed exterior. Callers that paint the status inset with their content background explicitly opt into content-theme icons; Search is the current caller. Keep activity appearance, navigation icons/scrims, native dim amount and all layout/dismissal behavior unchanged.
- Benefit/compatibility: Correct the observed clock/icon contrast without changing the product composition or data. This refines the status-backdrop assumption in DEC-20260909-004; its shared ownership and navigation policy remain. Require actual native pixel contrast across all owners, theme changes/recreation, API 26/34/37 and keyboard neighbors.
- Related: FND-20260909-015, DEC-20260909-004/012, VER-20260909-014.
- Status: Verified for native status contrast on API 26/34/37 with neighboring regression, final visual review and readiness.

### DEC-20260909-012 — Keep Search inside its actual native viewport

- Context: FND-20260909-014 exposes status-bar overlap, keyboard-hidden results and query focus loss during responsive reflow despite prior inline visibility checks.
- Decision: Retain the Search workspace and engine. Paint its full existing background and explicitly consume native system-bar/IME insets. Below the existing 440 dp workspace boundary, keep title/exit and query fixed while context, filters, count and results share one list. Keep one stable controls/divider/results structure and vary only measured row/column placement so the query never changes parents.
- Benefit/tradeoff: Short windows gain reachable results and choices without shrinking text or touch targets; controls require scrolling only where fixed controls otherwise consume the viewport. Taller compact and wide arrangements keep their existing policy. Micro vertical query spacing supplies room for the full 84 dp two-line title observed on API 26. Settled complete-result announcements remain on the pane when their heading scrolls away.
- Alternatives: Insets alone left no result space. A replacement search flow/engine would not address this defect. Window-owned focus/keyboard requests were insufficient, and movable content still lost focus while reparenting. Both attempts were removed; the original one-time opening focus remains without retries or reopening a deliberately hidden keyboard. Native status pixels, not icon flags alone, exposed the need to paint the inset backdrop.
- Compatibility: Preserve indexing budgets, incomplete-result truth, local/global scope, filters, exact routing and persistence; no data/schema/version change.
- Verification: Actual Android 200% text, native title/exit/result bounds and status-backdrop pixels, scope/Match Any/recreation/exact routing on API 26/34/37, deterministic wide→short→wide focus, neighboring regressions, inspected images and readiness pass in VER-20260909-013.
- Related: FND-20260909-014, DEC-20260909-011, IMP/VER-20260909-013.
- Status: Verified for this Search correction; complete product review remains open.

### DEC-20260909-011 — Give the Task editor explicit system-bar and keyboard inset ownership

- Context: FND-20260909-010 is reproduced after idle/render synchronization: Android clips the editor heading and omits its exit while Compose still measures a full title. The current Task dialog combines platform-fitted decor with explicit IME padding. The local Compose DialogWrapper bytecode sets unspecified soft-input adjustment for fitted decor, allowing Android to move the window around the focused field.
- Alternatives/decision: Shrinking text or removing the shared-draft warning would hide the symptom. Forcing a soft-input flag from a separate side effect would compete with DialogWrapper updates. First verify a local correction using the existing Compose window API: disable platform decor fitting and consume system-bar plus IME insets at the Task root, preserving the fixed header and scrollable body. Reproduce and inspect neighboring primary editors before deciding whether they need the same treatment.
- Compatibility/verification: Preserve pane positioning, actual font scale, draft/focus behavior, warning semantics, Save/Cancel, and persistence ownership. Require complete native title/exit bounds, keyboard-visible focused input, real shared-capture lifecycle coverage, ordinary/enlarged and API boundary evidence before acceptance. No schema/version/release change.
- Related: FND-20260909-010, FND-20260909-006, FB-20260908-006.
- Neighbor/lifecycle refinement: The Habit name and later Goal target maintain full native headers with their existing primary shell, so no shared-primary change is justified by these cases. Real cold shared-Task launch instead exposes a missing initial keyboard focus (FND-20260909-013). Request it once from the actual dialog after its window is focused, preserving editing and child-dialog return behavior.
- Platform refinement: Read the keyboard controller from the actual dialog composition alongside its focus effect. The early API 26 recreation capture initially suggested lost keyboard presentation; explicit native-window waiting subsequently proves it opens. Reject the ineffective production frame delay. Preserve the real-keyboard assertion and synchronize capture to actual platform state.
- Small-window geometry: The fixed outer vertical form padding clips 21 of the Task label's 48 pixels on the short API 26 window at actual 200% text. Keep horizontal inset and vertical edge spacing, but put vertical padding inside scrolling content to recover usable viewport space. Require full focused-label bounds before accepting the correction.
- Status: Verified in IMP/VER-20260909-012. Native header/label bounds, cold/recreated focus, durable save/reopen, neighboring editors, API 26/34/37, inspected captures and readiness pass. This is a scoped Task correction; wider product design review remains active.

### DEC-20260909-010 — Resolve exact test routes from declared Kotlin packages

- Context: FND-20260909-012 shows the changed-path router disagrees with the instrumentation inventory when a package differs from its directory. Renaming existing files would conceal the routing defect and create unnecessary source churn.
- Decision: Read the package declaration and verify the filename's top-level class, following the existing instrumentation inventory contract. Retain deterministic deduplication, deleted-test profile routing, and release authority. Missing or unsupported changed source must fall back to the full relevant test source set rather than invent an exact selector.
- Verification: Reproduce the two existing mismatched Android packages, exercise a JVM package/directory mismatch and unavailable/unsupported source in isolated fixtures, retain rename/delete cases, and verify the check wrapper executes the correctly routed real Area class.
- Related: FND-20260909-012, DEC-20260904-003, DEC-20260906-002.
- Status: Verified in IMP/VER-20260909-011; exact declared packages and broader unresolved-source fallback pass without weakening release or instrumentation authority.

### DEC-20260909-009 — Give Area cleanup a readable impact summary and visible preservation choice

- Context: FND-20260909-011 exposes a partially clipped first destination at ordinary text despite a passing callback test. The generic 200 dp choice cap includes a long explanation and zero-count categories.
- Alternatives/decision: Increasing every choice dialog would add unnecessary height to simple selections. A replacement wizard would change the established request-owned cleanup flow. Keep this dialog and its exact actions; opt its existing list into a larger parent-bounded cap and scrolling heading, summarize nonzero counts with existing quantity wording, and place destinations ahead of extended cleanup details. Keep permanent-deletion meaning explicit and the filter/widget-reset disclosure reachable, including for empty Areas.
- Compatibility: Preserve exact destination identity, move-versus-delete callbacks, saving/dismissal guards, repository authority, item history, and lifecycle receipts from DEC-20260902-008. No schema or data mutation changes.
- Verification: Require complete initial-choice bounds at ordinary and actual 200% text, last-choice access, stable actions, accurate nonzero impact copy, and inspected normal/enlarged/empty renders with existing Area regressions.
- Related: FND-20260909-011, FB-20260908-006.
- Status: Verified in IMP/VER-20260909-010 for scoped layout, retained choices, cleanup disclosure, and neighboring behavior.

### DEC-20260909-008 — Let long dialog context scroll and give choices enough height

- Context: FND-20260909-009 shows a one-line reading viewport in short destructive dialogs, no initial template choice at 320% text, and hidden Area/backup choices. Source review also finds the shared choice list capped at 200 dp regardless of text size.
- Alternatives: A forced full-screen replacement would ignore deliberate small-window constraints and alter every modal journey. Shrinking requested text or touch targets would undermine accessibility. Wrapping existing scroll containers in another vertical scroller risks competing gestures and unbounded lazy-list measurement.
- Decision: Keep the existing dialog/window/action shell and make its fixed title optional. Long destructive, template, and backup content can include an accessible heading in their existing single scroll owner, preserving pane context and fixed actions. Give shared choice lists a font-aware height cap bounded by their parent. Use concise template names, place optional template guidance after choices, and simplify the backup choice hierarchy while retaining all preview/compatibility details and its second replacement gate.
- Compatibility: Preserve callbacks, draft values, exact backup merge/replacement guards, busy/retry behavior, domain/history semantics, and requested font scale. Only known long-content callers opt into scrolling headings; ordinary dialogs keep their fixed title.
- Verification: Require useful body bounds, visible initial choices, full error readability after scrolling, stable actions, exact chosen template, affected ordinary/large-text pixels, and neighboring dialog/backup regressions before propagation or acceptance.
- Related: `FB-20260908-006`, `FND-20260909-009`, `DEC-20260909-005`.
- Status: Verified for the scoped callers in IMP/VER-20260909-009; Area deletion was subsequently verified in IMP/VER-20260909-010. Broader dialog review remains open.

### DEC-20260909-007 — Prioritize CSV recovery before the mapping form

- Context: Actual enlarged text exposes a zero-valid-row recovery action below the mapping form; failed-session recovery already appears near the error. Generic setup guidance competes with the active failure message.
- Alternatives: Scrolling the test would prove reachability but preserve poor recovery discovery. A new multi-step import wizard would disrupt an existing coherent preview and its retained mapping/retry lifecycle without a demonstrated benefit.
- Decision: Keep the current preview. Use one early file-recovery action for error and zero/invalid-preview states, retain its existing Choose Another File versus Replace File meaning, and show mapping guidance as secondary text only when no authoritative error is active. Keep frozen mapping review scrollable and explicit.
- Compatibility: No change to mapping identity, preview editing policy, allowed imports, receipts, retry/replacement callbacks, persistence, or historical data. Completed and unavailable-target states retain their existing action ownership.
- Related: `FND-20260909-008`, `FND-20260909-006`, `DEC-20260901-022`, `VER-20260909-008`.
- Status: Verified in `IMP/VER-20260909-008`; recovery priority, exact callback, frozen mapping access, and normal preview behavior pass.

### DEC-20260909-006 — Inspector identity gets a full-width row when space is constrained

- Context: Actual 200% text exposes word splitting and ellipsis caused by keeping the emoji, identity column, Edit, and Close in one row. This is shared by Task/Habit/Goal/Gym inspectors.
- Alternatives: Shrinking text/targets would defeat accessibility. Expanding every inspector to a full screen would change navigation and body behavior. Reusing the authored-editor header directly would change trailing exit placement and add unrelated save-action structure.
- Decision: Keep the existing roomy header. At narrow effective widths, place emoji and trailing actions in the top row, followed by full-width title, context, and status. Use the established width/font-aware threshold; preserve two-line title/context bounds, semantic labels, existing actions, tabs, body scrolling, and primary-action dock.
- Compatibility: This supersedes only historical assumptions that the inspector header remains visually fixed, such as the scoped boundary in `DEC-20260906-004`; those records' completed history and domain decisions remain valid. No authored data or domain behavior changes.
- Related: `FB-20260908-006`, `FND-20260909-007`, `DEC-20260909-005`.
- Status: Verified in `IMP/VER-20260909-007`; scoped readability, scrolling, stable actions, and normal-header evidence pass.

### DEC-20260909-005 — Dialog accessibility tests verify the actual Android text scale

- Context: `FND-20260909-006` establishes that an outer Compose density override is replaced inside a native dialog. Existing fixtures mix real dialogs, inline screens, and inline screens that open dialogs; their names alone cannot determine coverage.
- Alternatives: Teaching production dialogs to inherit a fake test density would change app behavior to satisfy the fixture. Blindly changing every density test would disturb useful deterministic inline-layout contracts. Relying only on the system setting would still leave rendered-text coverage unproven.
- Decision: Use a shared, method-scoped JUnit rule to set Android's font scale before activity launch and restore the original value after teardown. Correct affected dialog tests and assert their rendered TextLayoutResult scale inside the actual dialog. Keep inline geometry fixtures explicit; extend real-dialog coverage where only an inline component was previously exercised.
- Evidence/compatibility: First run already proves this ordering works; changing font scale after activity launch recreates the activity and can lose test content. No production density override or font-scaling workaround is authorized by this verification change. Any newly exposed product defect gets its own finding and proportionate remediation.
- Related: `FB-20260908-006`, `FND-20260909-006`.
- Status: Implemented and verified for the 32 inventoried fixtures in `VER-20260909-007/008`. Broader real-window coverage discovery and newly exposed layout findings remain in progress.

### DEC-20260909-004 — Each dialog owns contrast from the rendered Whip theme

- Context: Activity appearance updates cannot reach separate Compose dialog windows. Source discovery finds four window creators: shared productivity dialogs, Task editing, Entity Inspector, and Unified Search.
- Alternatives: Copying window effects into four owners would duplicate platform policy. Changing only the shared editor would leave three independent windows wrong. Replacing dialog layouts would disrupt working navigation, keyboard, draft, and dismissal behavior without addressing the cause more directly.
- Decision: Expose the resolved dark/light choice through the pure Whip theme, and use one small themed dialog boundary at all four window creators. Apply the same icon/legacy-scrim policy as activities to that dialog's own window. Preserve each caller's dimensions and DialogProperties.
- User benefit/compatibility: Status and navigation controls stay readable when Whip differs from Android, including live theme changes and reopening. No data, history, preferences, or domain changes; modern automatic navigation contrast and existing keyboard/dismissal behavior remain in place.
- Related: `FB-20260908-006`, `FND-20260909-004`, `DEC-20260909-001`.
- Status: Verified in `VER-20260909-006` on API 26/34/37 with actual screenshots, neighboring Android regression, and readiness checks. DEC-20260909-013 subsequently refines the status-backdrop assumption after native pixels expose low contrast on dimmed light windows; shared ownership and navigation policy remain intact.

### DEC-20260909-003 — Empty Home prioritizes selected sections while teaching every tool

- Context: The real Tracks/Gym setup journey stores the right dashboard choices but still introduces Tasks/Habits first. Existing source and Settings coverage deliberately preserve discovery of every primary tool, including hidden dashboard sections.
- Alternatives: Hiding all unselected introductions would lose that discovery guarantee. Keeping hard-coded Task/Habit priority contradicts the preceding setup decision. A new onboarding wizard or additional preference would duplicate existing intent.
- Decision: Feed the current ordered visible Home sections into the existing starting group. Keep all other tools in the secondary group, preserving their actions and descriptions. Derive priority from stable `HomeSection` identity, not display titles; no new setting or migration.
- Related: `FB-20260908-006`, `FND-20260909-005`.
- Status: Verified in `VER-20260909-005`; selected order, single/all choices, all-tool discovery, actual setup/reopening, and affected Home pixels pass.

### DEC-20260909-002 — Keep first run concise and own its confirmed completion

- Context: First run already provides a one-action recommended path and optional customization, but uses an oversized editor shell and an unconfirmed settings write.
- Alternatives: A replacement full-screen wizard would add navigation and design churn to a short, optional choice. Retaining fixed editor height preserves the observed empty space and awkward action separation. A content-sized version of the existing two-step dialog retains familiar choices and accessibility while removing that gap.
- Decision: Keep the recommended/customize structure; use the shared bounded content-sized dialog, with a scrollable body and fixed reachable actions. Route submission through the existing durable typed-settings boundary and saveable request coordinator, showing saving and retryable errors. Preserve draft ownership even if observed preferences temporarily publish `setupCompleted`; request notification permission only after the matching success receipt.
- Compatibility: No schema, backup, default-selection, or feature-availability changes. Retry applies the same selected settings; process interruption retains the draft with a clear retry message. Dialog-window contrast and empty-Home prioritization are tracked separately.
- Related: `FB-20260908-006`, `FND-20260909-002`, `FND-20260909-003`.
- Status: Verified for setup layout and request ownership in `VER-20260909-004`; separate window/Home and broader accessibility follow-ups remain active.

### DEC-20260909-001 — Activity themes own content and Android bar contrast together

- Context: `FND-20260908-015` reproduces both opposite-theme failures in the real main activity. Widget/Health hosting already adjusts icon flags, but its recovery branch bypasses that adjustment; Android 8–9 also retain an Android-derived navigation scrim.
- Position A: Copy the external host's icon updates into MainActivity and handle recovery and older scrims independently.
- Position B: Share a small `WhipActivityTheme` wrapper at each real activity content boundary, including recovery, while keeping reusable `WhipTheme` free of window side effects.
- Decision: B. One resolved theme drives the rendered content and both bar appearances; preserve AndroidX's legacy scrim values and modern automatic navigation contrast protection. Existing edge-to-edge layout setup remains with each activity.
- Consequences: No changes to preferences, data, history, navigation, or domain logic. The shared wrapper requires an explicit activity, so component previews and isolated test surfaces cannot accidentally restyle unrelated windows.
- Related: `FB-20260908-006`, `FND-20260908-015`.
- Status: Verified for this theme boundary on API 26/34/37 in `VER-20260909-002`; the broader whole-product platform audit remains pending.

### DEC-20260908-010 — Home gives daily action priority and shares truthful Habit summaries

- Context: Home's raw Habit counts disagree with neutral-skip semantics, and accumulated summary/review blocks delay its actionable collections.
- Position A: Patch the count expressions and retain all existing stacked presentation. This repairs arithmetic but preserves competing hierarchy and duplicate clear-day Review ownership.
- Position B: Introduce a narrow daily Habit summary shared by phone/adaptive presentation, compact the two daily shortcuts into responsive summary surfaces, and integrate the secondary Review entry with the Home heading. Let the clear-day explanatory state own its sole Review action.
- Decision: Evaluate and implement B as one bounded Home improvement. Scored completed/remaining Habits exclude skipped and unavailable states; skipped outcomes are separately named and unresolved timer recovery remains reachable. Stack summaries at narrow effective widths or enlarged text, preserve 48 dp actions, retain all configured Home sections and collection-card behavior, and verify original-size rendered output before accepting the layout.
- Why this is superior for Whip: Daily work appears sooner while progress, neutral outcomes, and review/navigation remain understandable. It corrects one shared projection instead of accumulating independent special cases; no data-model or whole-app architecture replacement is needed.
- Consequences / reversal conditions: Rework the presentation if compact/large-text/RTL evidence shows worse readability or daily action access. Preserve explicit owner preferences for collection-card hierarchy and single action ownership.
- Related: `FB-20260908-006`, `FND-20260908-013`, `FND-20260908-014`.
- Status: Verified through `IMP-20260908-014` / `VER-20260908-014`.

### DEC-20260831-001 — Structured strength programs accept arbitrary compatible lifts

- Context: 5/3/1 defaults traditionally emphasize four lifts, but Whip users want other schedules and lifts.
- Decision: Treat the standard four as convenient defaults, not a domain constraint. Permit ordered, distinct compatible exercises such as Bench Press, Deadlift, and Zercher Squat.
- Why this is superior for Whip: It preserves accurate percentage programming while supporting real lifter customization.
- Reversal conditions: Only safety or calculation evidence specific to an exercise capability should restrict selection; names alone must not.
- Status: Accepted and released.

### DEC-20260831-002 — 1RM, e1RM, Training Max, percentage, and working load are separate facts

- Context: Conflation made controls misleading and progression unsafe.
- Decision: Persist provenance and user-adjustable TM percentage; allow direct TM or derivation from actual/e1RM; expose TM outside the 5/3/1 wizard when percentage prescriptions require it.
- Consequences: Historical prescriptions remain snapshots and current edits do not retroactively recompute completed workouts.
- Status: Accepted and released.

### DEC-20260831-003 — Joker is optional and additive

- Context: A Joker toggle should not regenerate or compete with Supplemental work.
- Decision: Model Joker as independently recordable Optional work inserted after Main and before Supplemental, with a dedicated narrow mutation path.
- Consequences: Enabling/disabling Joker preserves exact Main and Supplemental authored data.
- Status: Accepted and released.

### DEC-20260831-004 — Workout instances may diverge from templates without rewriting them

- Context: Lifters need substitutions and one-off exercises during actual training.
- Decision: Store workout-only additions with the session/history as ad-hoc Optional work; do not mutate the routine or future workouts unless the user explicitly edits the routine.
- Status: Accepted and released.

### DEC-20260831-005 — Cycle progression is advisory, explainable, conservative, and per lift

- Context: AMRAP/Joker/test performance can inform the next cycle but does not justify opaque automation.
- Decision: Offer standard, higher, lower, hold, ignore, or custom user-confirmed choices from bounded evidence. Stronger-than-standard recommendations require corroborated independent evidence; skipped optional work is neutral.
- Consequences: Recommendation evidence and decisions are auditable; historical workouts stay immutable.
- Status: Accepted and released; longitudinal calibration remains open to user evidence.

### DEC-20260831-006 — Repository-backed memory is mandatory for substantial Whip work

- Context: Chat compaction and isolated audit reports cannot reliably carry a long-running product program.
- Position A: Add a memory paragraph only to the large goal prompt.
- Position B: Create a reusable skill plus canonical repository ledgers and a goal checkpoint.
- Evidence and constraints: Skills can enforce procedure across tasks; repository files survive chats and are reviewable; neither should override current code evidence.
- Failure modes: Prompt-only memory disappears outside that goal. Unstructured documentation becomes stale or contradictory. Memory treated as truth can preserve outdated claims.
- Decision: Use the personal `maintain-whip-memory` skill, workspace fallback instructions, stable linked ledgers, and mandatory start/during/end writeback in the maximum-quality goal.
- Why this is superior for Whip: It is durable, searchable, auditable, reusable across task sizes, and explicit about evidence quality.
- Status: Accepted and implemented; future-task validation pending.

### DEC-20260831-007 — Version work as coherent pushed outcomes

- Context: Long-running local changes are difficult to audit, bisect, compare, and safely revert.
- Position A: Commit everything once at the end of a large goal.
- Position B: Commit and push each coherent, verified, independently revertible outcome before unrelated work begins.
- Evidence and constraints: Smaller purposeful commits make provenance and rollback clearer, but microcommits and unsafe staging in a dirty worktree reduce signal and can capture user changes.
- Failure modes: End-only commits combine unrelated regressions. File-by-file microcommits may not build. Automatic broad staging can leak secrets, caches, artifacts, or unrelated work. Force-pushing can destroy shared history.
- Decision: Use behavior-level chunks, explicit staging, focused commits, normal upstream pushes, reachability verification, and immediate blocker reporting. Never force-push or rewrite shared history.
- Why this is superior for Whip: It balances traceability and safe reverts with buildable, comprehensible product changes.
- Status: Accepted and implemented in the working protocol.

### DEC-20260831-008 — Unresolved restore recovery fails closed

- Context: Normal startup currently continues after recovery failure, allowing new writes into unresolved state.
- Position A: Gate only the visible Activity, cancel its scopes during restore, and rely on the persistent marker/generation checks for later Retry.
- Position B: Use one application-wide counted admission/drain boundary, then generation-scope state and actions that can survive the database replacement.
- Evidence and constraints: Activity-scoped ViewModels, workers, receivers, widgets, schedulers, Health sync, configuration Activities, and process-restorable drafts can outlive ordinary composition. Cancelling every scope risks partial transactions and lost drafts; one global serial mutex would unnecessarily block safe concurrent reads.
- Failure modes: Activity-only gating permits background writes and same-numeric-ID aliasing. Broad cancellation can strand partial multi-step work. An undifferentiated global mutex increases latency and can deadlock the Settings restore initiator. A counted barrier must still version SavedState, widget references, caches, and external action intents that survive replacement.
- Synthesis/decision: Fail closed with an application-level reader admission/drain barrier, a serialized exclusive restore attempt, privileged full background rebuild, preserved marker, non-restored data generation, accessible Retry, and generation-aware persistent/transient surfaces. Existing admitted operations finish; late work is denied; normal access reopens only after authoritative rebuilding succeeds.
- Why this is superior for Whip: It preserves the last trustworthy atomicity boundary and completed admitted work without accepting mixed-generation mutations, stale action aliasing, or a speculative whole-app transaction rewrite.
- Status: Accepted, implemented, and verified.

### DEC-20260831-009 — Reminder workers re-resolve live delivery eligibility

- Context: WorkManager inputs can outlive edits, completions, pauses, skips, moves, and reminder configuration.
- Position A: add persisted schedule revisions and cancellation calls to every mutation, or generalize reminders behind a reusable scheduling DSL.
- Position B: treat every queued delivery as an untrusted versioned claim, resolve exact live domain eligibility at the last responsible moment, and linearize production mutation versus resolve/post without changing user history.
- Evidence and constraints: reminder truth includes Task occurrences, Habit pauses/skips/checklists/source metrics, Goal type/status/deadline, quiet hours, time zone, and custom-unit history—not one entity row. Room changes coroutine identity inside transactions; WorkManager and NotificationManager cannot share Room atomicity; queued work and action intents survive process death and edits.
- Failure modes: a row revision misses settings/source/history changes and requires a schema/backup migration; cancellation-only still loses races; a generic DSL hides domain semantics. Claims alone fail if fingerprints include mutable performance history, if notification actions are weaker, if settings writers lose updates, or if mutation commits can occur between resolve and post. Reentrant coroutine-context locks deadlock across Room; mixed entity/state lock order deadlocks during rebuild.
- Synthesis/decision: use definition-only deterministic fingerprints plus live eligibility, exact action claims, scheduled-versus-snoozed kinds, one-time legacy upgrade, awaited queue operations, bounded source invalidation, a strictly non-reentrant mutation/delivery boundary, raw delegates under one explicit outer owner, entity→state lock order, and a durable deletion-cleanup journal. Missing/malformed/stale work succeeds silently without posting and reconciles authoritative future work.
- Why this is superior for Whip: It covers the real cross-domain sources of reminder truth, preserves all existing history and persistence formats, remains understandable per domain, and makes race outcomes deterministic without a speculative scheduling language or fragile scattered flags.
- Status: Accepted, implemented, adversarially challenged, and verified.

### DEC-20260831-010 — Live time behavior uses one explicit Whip zone and date flow

- Context: System-zone shortcuts and repository-triggered date snapshots can make screens and reminders disagree.
- Position A: Add one application-scoped calendar context over `WhipClock`, including active zone, physical date, cutoff-adjusted logical date, cutoff, and follow-device policy; gate cross-domain rendering until projections share it.
- Position B: Patch Track, Search, Goal, and Gym separately with local tickers and zone arguments.
- Position C: Replace all wall clocks, deadlines, countdowns, dates, and instants with a universal time framework.
- Evidence and constraints: Independent minute loops can advance visible domains in different frames; `LocalDate` alone suppresses same-date zone changes; Track previously sampled only after repository emissions; Search used the device zone; Gym mixed physical and cutoff-adjusted Today. Historical dates and exact instants are provenance, while focus/rest timers and reminder deadlines are separate absolute-time concerns.
- Failure modes: Local patches recur and retain mixed Home state. A universal clock conflates calendar days, historical instants, and elapsed countdowns and destabilizes unrelated systems. A centralized date without zone/follow policy still misses same-date travel changes.
- Synthesis/decision: Route live calendar behavior through one application-scoped `WhipCalendarContext`, invalidate it on settings and Android time/date/zone changes and aligned minute boundaries, and retain the previous rendered snapshot until all date-derived domain states match. Keep specialized absolute timers separate. New records use explicit Whip provenance; explicit historical starts derive from their supplied instant; persisted history is never re-dated.
- Why this is superior for Whip: Every visible domain shares one falsifiable meaning of Today without imposing cutoff semantics on exports, Health windows, timers, reminders, or historical records.
- Status: Accepted, implemented in `IMP-20260831-006` / `IMP-20260831-007`, and fully verified in `VER-20260831-009`.

### DEC-20260831-011 — Save-dependent navigation occurs only after confirmed persistence

- Context: Area scope changes and success notices currently precede asynchronous persistence in several productivity editors.
- Position A: Keep local callback-driven dismissal and move Area navigation into each callback.
- Position B: Treat the existing global `OperationStatus` as the authoritative save outcome for every open editor.
- Position C: Give each authored editor one typed, request-scoped state and a post-commit receipt; keep global status as presentation feedback only.
- Evidence and constraints: Task, Habit, and Goal writes are asynchronous; Area-filtered entities can disappear from scoped projections during an edit; activity recreation can retain a ViewModel while process restoration cannot; repository commit may succeed before reminder/tag refresh; rapid input can race; historical records must not be recomputed; and a user must never be encouraged to retry an entity that already committed.
- Failure modes: Position A duplicates lifecycle and exact-once logic and cannot distinguish commit from follow-up. Position B lets unrelated success/failure dismiss the wrong editor and can render a Snackbar behind a modal. Position C can wedge if terminal outcomes are not explicitly consumed, can become a generic framework if overextended, and must preserve cancellation/fatal-error semantics.
- Synthesis/decision: Use Position C for Task/Habit/Goal authored definitions. Atomically admit only an Idle request, settle only the matching UUID, reclaim unowned terminal results, never adopt another live Running request, block editor input during the write, preserve failure inline, and move/dismiss only after a successful authoritative receipt. Define repository commit as the point of no return: ordinary post-commit work adds warnings; pre-commit failure remains retryable; fatal errors and structured cancellation retain their meaning.
- Why this is superior for Whip: It makes persistence, lifecycle ownership, navigation, and user messaging one falsifiable sequence without coupling domain rules to Compose or inventing a cross-product transaction DSL. It also leaves quick reversible mutations free to use lighter behavior after individual audit.
- Status: Accepted and implemented for authored Task/Habit/Goal definition editors in `IMP-20260831-008`; secondary mutation families remain `FND-20260831-019`.

### DEC-20260831-012 — Large text preserves visible destination names

- Context: At 150% text, six direct phone destinations become icon-only.
- Decision: Prefer a measured named row, then a stable two-row named layout, then a labeled drawer only when neither fits. Do not use an arbitrary normal-size More bucket.
- Why this is superior for Whip: It preserves spatial memory and one-tap access while making increased text improve comprehension.
- Status: Accepted; implementation pending.

### DEC-20260831-013 — Habit reminders stay visible while advanced schedule controls are disclosed

- Context: Hiding everything weakens reminder awareness; showing every weekday/end/week-boundary option overloads simple creation.
- Decision: Keep a concise reminder summary in the basic path and disclose advanced controls, auto-expanding for existing data, power mode, or validation errors.
- Status: Implemented in `IMP-20260902-003` and verified in `VER-20260902-003`.

### DEC-20260831-014 — Preserve meaningful legacy Goal history, not every retired table

- Context: Public completion snapshots are user history; some other retained tables merely duplicate canonical state.
- Decision: Preserve or migrate completion snapshots through backup/restore. Omit redundant internals only after proving canonical equivalence and explicitly retiring their lifecycle.
- Status: Accepted; implementation pending.

### DEC-20260831-015 — Habit authored history is request-owned; lightweight reversible actions remain lightweight

- Context: Habit value, history, pause, and skip dialogs dismissed after dispatch, while Home and the Habit workspace shared one ViewModel outcome channel. A generic optimistic path could lose drafts or lie after deletion; applying a modal coordinator to every quick increment would make routine check-ins irritating.
- Position A: Keep optimistic dispatch-and-dismiss for all Habit mutations and rely on global status.
- Position B: Route every Habit action, including single-tap increments and checklist toggles, through one blocking authored-mutation coordinator.
- Evidence and constraints: Log/pause edits carry user-authored drafts and historical meaning; deletes can remove the live row before result delivery; reminders are post-commit derived work; Home/workspace surfaces may exchange ownership; numeric IDs can alias after replace restore; current-total Set is absolute and custom-unit-aware; rapid skip undo must be exactly once. Quick increments and check-offs are intentionally low-cost, immediately reversible interactions.
- Failure modes: Position A loses retry context, permits duplicate destructive taps, and can mutate a wrong restored child without parent validation. Position B adds modal latency and excess coordination to frequent one-handed actions, and one undifferentiated surface can steal another surface's terminal result. Unbounded relative floating tolerance can also silently erase real large-value changes.
- Synthesis/decision: Use typed request ownership for draft-bearing or destructive Habit history, pause, absolute-total, and skip-undo flows. Bind child mutations to the expected Habit inside the transaction; retain saveable child snapshots; namespace each UI surface; let another namespace reclaim only an abandoned terminal after an owner grace period; reset state at the user-data generation boundary; treat the Room mutation as commit and reminders as warning-capable follow-up. Keep ordinary quick increments/check-offs on the existing lighter path. Compare Set totals with a small ULP-bounded noise tolerance, not a magnitude-relative epsilon.
- Why this is superior for Whip: It protects authored data, history, lifecycle recovery, and cross-surface correctness exactly where failure is costly while preserving the speed of everyday Habit check-ins.
- Status: Accepted, implemented, independently challenged, and fully verified for the Habit secondary-mutation family; other `FND-20260831-019` families remain open.

### DEC-20260831-016 — Task edits preserve exact occurrence identity and authored Open overrides

- Context: Recurring Task edits span a definition, generated virtual occurrences, persisted exceptions, Subtask state/history, Goal Links, automations, reminders, and lifecycle-restored editor drafts. Timestamp-only conflict checks and position-based child copying could silently overwrite same-millisecond edits, attach integrations to the wrong Subtask, or preserve data that no consumer could reach.
- Position A: Treat the current recurrence rule and latest Task row as authoritative; copy future children by position; dismiss secondary dialogs after dispatch; release a notification action claim after any exception.
- Position B: Capture a compact saveable semantic boundary; validate definition/occurrence/Subtask state transactionally; map retained children by stable ID; migrate compatible Open intent across a series split; treat explicit Open rows as authored overrides; report authoritative commit separately from fallible follow-up.
- Evidence and constraints: Closed occurrences and Subtask snapshots are historical facts. Subtask toggles intentionally create state without occurrence rows. A new cadence or completion anchor can stop generating that date. Inbound automation duplication can double-fire; copied Goal Links must not start before either the split or their configured window. Reminder and deletion follow-up may fail after the Room mutation is already durable.
- Failure modes: Position A loses drafts on lifecycle/failure, permits stale overwrites, can orphan future progress, weakens Goal windows, duplicates series boundaries, and offers unsafe retries after committed deletion or notification action. An indiscriminate full-snapshot/generic-DSL approach would bloat saved state and make frequent interactions needlessly modal.
- Synthesis/decision: Use a compact semantic `TaskEditBoundary` and exact repository preconditions. Split only an open recurring occurrence; preserve the historical definition before the boundary; retain finite remaining counts; map retained Subtasks/integrations by stable ID; retarget inbound automations and copy outbound rules; migrate compatible Open state, materializing state-only occurrence identities; preserve `max(split, configured Link start)`; union generated dates with explicit Open rows in UI and reminder consumers. Use typed request receipts for draft-bearing/destructive flows, exact undo predicates, and phase-aware committed-warning/cancellation semantics.
- Why this is superior for Whip: It keeps real history immutable, makes customization and offline/lifecycle recovery trustworthy, preserves one-handed quick interactions where safe, and gives every persisted authored occurrence a consistent visible/remindable consumer path without inventing a generic scheduling DSL.
- Status: Accepted after three challenge rounds, implemented in `IMP-20260831-010`, and fully verified in `VER-20260831-012`; Goal/Track/Gym subsets of `FND-20260831-019` remain open.

### DEC-20260831-017 — Goal lifecycle, history, and authored mutations use risk-proportionate exactness

- Context: Goal edits previously mixed optimistic dialog dismissal, status-based archive, live terminal recomputation, numeric-only snapshot identity, incomplete deletion impact, and UI-owned business rules. The result could lose retry drafts, mislabel abandonment, rewrite the meaning of a completed Goal, duplicate restored history, or offer a destructive retry after an authoritative commit.
- Position A: Keep dispatch-and-dismiss, use one broad entity revision, represent archive as a lifecycle status, recompute terminal cards from live Goal state, and treat all Measurement writes as generic upserts.
- Position B: Give every Goal interaction a blocking full-snapshot coordinator, freeze all history permanently, reject any post-close correction, and introduce a generic programming/mutation DSL.
- Evidence and constraints: Definition edits, progress drafts, reset times, lifecycle changes, deletion, reminders, backup merge, Home/workspace ownership, replace restore, custom units, and Area deletion have different risk and latency. Historical entries may require factual correction, but a terminal outcome must describe what happened when the Goal closed. Archive answers organization, not outcome. Existing backups lack stable closure identity. Quick pin/milestone actions must remain one-handed and responsive. Health Connect separately relies on deterministic first-import upserts.
- Failure modes: Position A loses drafts, accepts stale edits, conflates Archived with Completed/Abandoned, retroactively changes terminal results, duplicates merge history, and can lie after post-commit cleanup failure. Position B makes routine interactions irritating, freezes correctable evidence, over-couples unrelated domains, and creates more state than lifecycle restoration can safely own.
- Synthesis/decision: Use semantic Goal boundaries scoped to each mutation; request-own draft-bearing, lifecycle, archive, reset, and destructive flows; retain exact lightweight pin/milestone actions. Model archive orthogonally. Permit exact correction/deletion of Goal-owned historical measurements after close/archive while freezing closure outcomes. Persist specialized elapsed/milestone closure data and stable UUIDs; preserve reset events as immutable history. Reject future progress and require explicit confirmation for History-only dates outside the Goal window. Treat Room commit as authoritative and reconcile fallible cross-system work as warnings; fatal errors still escape. Keep ordinary Measurement edits update-only, with an explicitly narrow stable-ID insertion exception only for identified Health Connect reconciliation.
- Resolved disagreements:
  1. Optimistic dismissal versus request ownership: request ownership wins for draft-bearing or consequential actions because failure must retain input and exact retry meaning; lightweight exact actions remain direct.
  2. Full Goal snapshot versus semantic boundary: semantic boundaries win because progress should ignore unrelated pin changes while definition, aggregation, date-window, lifecycle, archive, and target changes invalidate it.
  3. Immutable ledger versus correctable evidence: exact Goal-owned entries remain correctable, but terminal snapshots never recompute, preserving both factual correction and historical truth.
  4. Archive status versus orthogonal archive: orthogonal archive wins because organization cannot erase Completed versus Abandoned outcome.
  5. Live terminal projection versus specialized snapshot: frozen value/progress, elapsed duration, and milestone counts win because later edits/reopens must not rewrite closure meaning; legacy absent fields render truthful fallback rather than live claims.
  6. Numeric snapshot IDs versus stable UUIDs: stable UUIDs win because portable merge must be idempotent across database-local IDs.
  7. Silent future/out-of-window progress versus explicit intent: future records are invalid; out-of-window historical evidence requires an explicit History-only confirmation.
  8. Globally relaxed upsert versus typed reconciliation intent: authored edits remain strict; only Health Connect may recreate an absent row whose deterministic ID exactly matches its source.
- Why this is superior for Whip: It protects lifecycle truth, authored drafts, restore/merge identity, reminder correctness, and destructive-action integrity without turning fast gym/productivity interactions into a generic modal framework.
- Status: Accepted after independent Director, data-integrity, and adversarial-QA challenge; implemented in `IMP-20260831-011` and fully verified in `VER-20260831-013`. Track/Gym subsets of `FND-20260831-019` remain open.

### DEC-20260901-018 — Protect active Gym programming before Track schema remediation

- Context: The secondary-mutation audit found two P0 paths: Gym deletion could erase active 5/3/1 main work and corrupt Training Max progression, while a stale Track schema confirmation could erase newly added unreviewed values.
- Position A: Fix Track first because its stale confirmation can destroy historical values.
- Position B: Fix Gym first because an in-gym destructive action can erase the live prescription and change the next cycle's Training Max decision.
- Evidence and constraints: Both need exact transactional boundaries. The Gym path is reachable during the highest-distraction, lowest-attention workflow and crosses active set truth, required-main-work eligibility, Routine provenance, and 5/3/1 progression. Track requires a larger definition/editor contract that should remain a separate reviewable chunk. Visual polish, search, and unrelated Settings work provide no comparable correctness reduction.
- Failure modes: Track-first leaves an active strength-program corruption path shipping longer. Gym-first leaves known Track stale-schema loss pending. Combining them makes regression ownership, rollback, and historical compatibility harder to audit.
- Synthesis/decision: Remediate Gym permanent Exercise/Routine deletion first as one coherent exact-mutation tranche, commit/push it after complete gates, then begin Track definition/schema integrity immediately.
- Why this is superior for Whip: It removes the live programming-corruption path without diluting either domain's invariants, and preserves a clean commit boundary for the next P0.
- Status: Accepted and executed for `IMP-20260901-012`; Track remains `FND-20260901-022`.

### DEC-20260901-019 — Gym deletion uses exact impact, active guards, and durable outcome ownership

- Context: Permanent deletion crosses Room cascades, active workouts, Routine templates, performed history, PRs, Training Max audits, graph presets, machines/categories, Goal Links, Triggers, automation-created Track entries, settings shortcuts, and fallible post-commit reconciliation.
- Position A: Keep the existing immediate confirmation and generic global operation status.
- Position B: Never permit permanent Exercise/Routine deletion; archive only.
- Position C: Freeze a complete dependency impact, block only active programming hazards, validate the exact revision transactionally, and return a request-owned committed receipt with durable unknown-outcome verification.
- Evidence and constraints: Users explicitly need archive and permanent cleanup, but historical and program consequences must be intelligible. Completed workout placements/sets are deleted only when the user reviews that exact count; Routine deletion instead preserves completed/discarded workout snapshots by clearing the mutable source reference. Training Max decisions already contain immutable Routine/Exercise identity snapshots and must remain as audit history. Process death may occur before the UI learns whether Room committed.
- Failure modes: Position A permits stale confirmation, false retries, wrong-surface delivery, and active progression corruption. Position B prevents deliberate cleanup and leaves no exact impact contract. Position C can misclassify an unknown process-death outcome unless ownership survives SavedState and missing-target UI continues to offer verification.
- Synthesis/decision: Use Position C. SHA-256 revisions include the root and every affected dependency; active Exercise placements/alternatives and active Routine-sourced sessions block deletion; exact counts must match inside one transaction; immutable Training Max decisions and explicitly preserved workout snapshots remain; ordinary post-commit PR/Link/settings failures become warnings; fatal errors still escape. The request/recovery token lives in `SavedStateHandle`, candidates/generation live in saveable UI state, and Retry Verification transfers ownership to a fresh read-only verification request.
- Why this is superior for Whip: The user can make an informed permanent-cleanup choice while Whip protects live programming, preserves audit truth, rejects stale impact, and never encourages a second destructive write merely because delivery was interrupted.
- Status: Accepted after repeated UX/engineering/QA challenge and fully verified in `VER-20260901-014`.

### DEC-20260901-020 — Track definition saves use a compact authored boundary plus exact removal review

- Context: The Track editor persists a complete definition draft but authorizes destructive Field and Choice changes with raw database IDs and opening-time Entry counts. A concurrent editor or CSV import can add Fields, Choices, values, or dormant Link/Trigger references after confirmation; the stale save can then delete or retarget rows the user never reviewed. Track saves also report fallible tag follow-up as if the authoritative Track write failed.
- Position A: Freeze the entire Track projection, including all Entries and values, and reject any intervening change before every definition save.
- Position B: Keep the existing definition save and strengthen only each destructive confirmation with a count or per-removal token.
- Evidence and constraints: A harmless new Entry must not block a rename, reorder, or same-dimension default-unit edit. Conversely, a concurrent empty Field/Choice is absent from a stale full-definition draft and would be silently deleted without a whole authored-definition precondition. Same-count value edits and newly added compatibility references defeat count-only review. Link and Trigger execution was deliberately retired in schema 31; its hidden rows remain dormant compatibility/audit data and cannot become an uneditable blocker. Existing pin, archive, list position, denormalized Area labels, search rows, historical generated facts, and unrelated Entry values are outside this editor's authored definition.
- Failure modes: Position A creates nuisance conflicts during ordinary logging/import, produces an unbounded lifecycle payload, and couples definition editing to unrelated history. Position B still permits stale schema overwrite, same-count history mutation, replacement-target drift, and unreviewed legacy-reference changes. Blocking every dormant integration reference would dead-end users because Whip no longer exposes an editor for retired automation rules.
- Synthesis/decision: Use two exact boundaries. A compact semantic revision covers Track identity and editable metadata plus every ordered Field/Choice identity and property, excluding pin/archive/order, timestamps, derived Area name, Entries, and values. A repository-originated removal review separately fingerprints the normalized destructive plan, exact affected value rows, replacement identity, and every affected dormant Link/Trigger row. Validate both inside the same Room transaction before any write; derive removals from the draft rather than accepting confirmed IDs. Any definition or reviewed-impact change retains the draft and requires renewed review. Continue the schema-31 compatibility behavior for explicitly reviewed dormant references, disclose it as legacy integration impact, and preserve generated historical facts. Request-own the save, treat the Room write as authoritative, and downgrade fallible tag/Area verification to committed warnings.
- Why this is superior for Whip: It prevents unreviewed history loss and stale schema overwrite while allowing unrelated one-handed logging and imports to continue. It preserves upgrade compatibility without reviving a retired automation product, keeps SavedState compact, and makes retry behavior truthful after the commit point.
- Status: Accepted after domain, UI, product, and adversarial-QA challenge; implemented and fully verified in `IMP-20260901-013` / `VER-20260901-015`.

### DEC-20260901-021 — Track Entry edits use exact rejection and stable create identity

- Context: Entry is a historical fact, but current create/update/delete flows share global status and identify edits only by numeric ID. A stale full-form draft can erase a concurrent value; rapid or unknown-outcome create can duplicate history; delete/Undo can separate a restored Entry from its fulfilled TriggerOccurrence.
- Position A: Automatically merge Field-level changes from concurrent Entry editors.
- Position B: Keep last-write-wins but warn users that another writer may have changed the Entry.
- Position C: Freeze a compact opening form contract plus the exact Entry/value revision, reject any conflicting update/delete transactionally, retain the draft, and preallocate stable create identity so exact retry is idempotent.
- Evidence and constraints: Values are keyed by stable Field UUID, which makes exact comparison reliable, but a full three-way merge must also resolve Field removal/type changes, required additions, Choice membership, units, provenance, and user intent. Timestamp-only checks fail for same-millisecond writes. Freezing unrelated Track decoration or all other Entries would create nuisance conflicts. Field/Choice labels still convey user-facing meaning and cannot safely be treated as purely cosmetic while a form is open. Existing Room transactions and search atomicity are sound foundations. Automation execution is retired, but preserved TriggerOccurrence/Contribution facts remain audit history.
- Failure modes: Position A can silently combine incompatible intent or reinterpret values after schema changes. Position B knowingly permits lost updates and cannot make retries truthful. Position C can create nuisance conflicts if its form scope is too broad and cannot provide durable Undo across process death without a tombstone; it must distinguish exact achieved retry from mismatch and explain same-process Undo limits honestly.
- Synthesis/decision: Use Position C. The form boundary covers stable Track identity/writable state and user-facing Field/Choice meaning needed to interpret the Entry, while excluding Track description/icon/Area/tags/pin/order, Field list-display decoration, unrelated Entries, timestamps as authority, and rebuildable search. The Entry boundary hashes stable identity, date, immutable provenance, and every typed value identity/content using raw numeric bits. Create receives a process-saveable preallocated Entry UUID and returns an exact transaction receipt; update/delete reject stale boundaries without mutation. Request/session/generation owns delivery. New authorship freezes the complete visible unit contract, while Undo accepts harmless unit rename/archive decoration only when stable ID, dimension, and raw conversion semantics still match. Delete snapshots and atomically restores compatible occurrence fulfillment for same-process Undo; incompatible restore rolls back. Whip exposes one exact Undo at a time: confirming a newer deletion supersedes an untouched prior Undo, but a failed restore remains retryable and cannot be overwritten. CSV batch idempotency remains a separate bounded protocol.
- Why this is superior for Whip: It protects historical truth and user intent with a small domain-specific contract, makes mobile double taps and exact retries safe, preserves editable drafts on conflict, and avoids both a generic merge engine and coupling one Entry to unrelated Track activity.
- Status: Accepted after independent domain, UI/accessibility, adversarial QA, and Director challenge; implemented and fully verified in `IMP-20260901-014` / `VER-20260901-016`.

### DEC-20260901-022 — Track CSV imports use private receipts plus deterministic row identity

- Context: A complete CSV transaction could commit while its UI callback was lost, leaving a restored session that offered the same batch again with fresh Entry UUIDs. The protocol must distinguish an exact completed retry from intentional duplicate content, preserve historical edits/deletions, keep portable backups clean, and remain usable at 5,000 rows without binding persistence to mutable UI projections.
- Position A: Deduplicate by visible row content or payload fingerprint. This is simple but prevents intentional duplicate facts, cannot distinguish edited/deleted imported history, and makes later semantic changes retroactively affect identity.
- Position B: Use deterministic Entry UUIDs only. This prevents duplicate inserts, but a stale retry after all imported rows were deliberately deleted could resurrect history and cannot prove that an entire batch, search index, and receipt committed together.
- Position C: Give each reviewed session a fresh batch UUID; freeze an exact versioned request and deterministic per-row identities; atomically insert Entries, values, search rows, and a private constant-size receipt; verify the full receipt envelope before reading the source URI on recovery.
- Evidence and constraints: Track numeric IDs are database-local, so the envelope also binds stable Track UUID/creation identity. Field/Choice labels and selected unit conversion contracts affect interpretation and must share the opening snapshot. URI, filename, headers, mappings, and values must not be retained in the receipt. Portable backup format 16 already preserves Entry UUIDs and historical facts; operational retry proof should not cross devices. A maximum import permits 5,000 rows and 100,000 cells, so per-cell live-unit queries and per-row form reloads are unacceptable.
- Failure modes: Position A silently drops legitimate duplicates and conflates current display with historical authorship. Position B can resurrect deliberately removed facts and offers no exact achieved-state proof. Position C can become overly conservative or slow if it fingerprints unrelated mutable units, trusts a newer form after parsing, weakly compares restored receipts, or validates custom units per cell.
- Synthesis/decision: Use Position C. Parse only against a repository-originated atomic `TrackEntryFormSnapshot`; revalidate the same Track, Field, Choice, writable, and selected/default unit contracts during preparation and commit; canonicalize with versioned SHA-256 and deterministic UUIDv8 identities; compare the complete stable receipt envelope as Missing/Exact/Collision; reject archived non-default units while retaining an archived Field default. Keep receipts Track-cascaded, digest-only, excluded from portable backup, and invalidated with user-data generation on replace restore. Build FTS from the validated in-memory form, checkpoint cancellation, and validate each distinct used unit once per transaction.
- Why this is superior for Whip: Exact retries are safe without content deduplication, intentional duplicate history remains possible, edits/deletions are never resurrected, process recovery is truthful even when the URI is unreadable, schema/unit races cannot reinterpret facts, and the product-level 5,000-row/100,000-cell contract is measured rather than assumed.
- Status: Accepted only after repeated domain, UX/accessibility, and adversarial-QA rejection/remediation cycles; implemented in `IMP-20260901-015` and fully verified in `VER-20260901-017`.

### DEC-20260901-023 — Gym commits exact facts; derived work reconciles from them

- Context: Active Gym mutations cross performed Set truth, optional work, retired placements, Routine provenance, 5/3/1 progression, personal records, Links, rest notifications, Activity/process lifecycle, and independently invalidated Room streams. The product must reject a genuinely stale author without making normal one-handed execution race its own UI projection.
- Position A: Keep best-effort UI callbacks and global status, then rebuild derived state opportunistically. This minimizes new types but cannot identify the exact editor, Set, session, timer generation, or commit point.
- Position B: Freeze or rewrite the entire workout graph for every action. This is maximally conservative but turns independent Set logging into nuisance conflicts, creates large lifecycle payloads, and risks retroactively recomputing performed history from mutable program definitions.
- Relevant evidence and constraints: A quick Set has stable Set/workout UUIDs and narrow revisions; finish has a specific session UUID/revision and optional Training Max decisions. Required Main-work eligibility, authored classification, program provenance, equipment, and units must survive later edits. Joker Sets are optional additions, not replacements for BBB/FSL/BBS. PRs, Links, and notifications are reconstructible; the committed workout is not. WorkManager enqueue/cancel completion and notification delivery are asynchronous. Room invalidates observed tables independently unless the active graph is reread transactionally.
- Failure modes: Position A permits duplicate/stale saves, wrong-surface dismissal, false destructive retry, stale timer delivery, and progression from missing evidence. Position B blocks harmless parallel work, bloats state, obscures precise conflicts, and couples historical truth to a generic snapshot/DSL. Treating every follow-up failure as commit failure can duplicate the authoritative action; clearing timer state before notification delivery can lose the only retryable proof.
- Synthesis/decision: Use narrow exact boundaries and immutable historical facts. Quick-set submit binds Set UUID/update time plus current workout revision; finish binds session UUID/revision and explicit 5/3/1 decisions. Placement and Set outcomes retain completed facts while removing execution eligibility. `requiredForProgressionSnapshot`, authored classification, work-section, optional-kind, equipment, unit, TM, and program provenance remain frozen. Active UI consumes one transactionally coherent Room graph, and newly added focus is retained until that graph contains the placement. Request-owned receipts cross Activity recreation; unknown process recovery warns rather than guessing. Room commit is authoritative. PR/Link/timer projections reconcile from durable rows. Timer schedule/cancel is awaited, revision/deadline acknowledged exactly, and notification-first delivery is at least once and idempotent.
- Reason the selected design is superior for this application: It protects serious strength-programming semantics and completed history while preserving fast independent Set entry. It makes conflicts small and intelligible, keeps Joker/supplemental/assistance meaning explicit, survives lifecycle and scheduler failure, and provides a reusable fact-plus-projection pattern without inventing a generic workout DSL.
- Status: Accepted after multiple domain, lifter/UX, engineering, accessibility, and adversarial-QA challenge rounds; implemented in `IMP-20260901-016` and fully verified in `VER-20260901-018`.

### DEC-20260901-024 — Typed Settings use explicit modal drafts and durable request receipts

- Context: Typed Settings formerly wrote every parseable keystroke. The first remediation tried inline drafts with focus-loss/IME commit; later proposals used a modal plus timeout or observation of the current preference as success. Each could still lose intent during disposal, deadlock on an unchanged value, accept a late write after a timeout, or mistake process-local `SharedPreferences` publication for durable persistence.
- Position A: Retain inline fields, commit on focus loss/IME, and infer completion from callbacks or the observed current value. This minimizes taps and keeps values visible in-place.
- Position B: Use a focused modal draft with explicit Save/Cancel, strict complete-field parsing, and one request-owned repository receipt that confirms the durable write before dismissal.
- Relevant evidence and constraints: Typed fields include seconds, days, percentages, counts, exact `HH:MM`, region/fixed-offset zones, and coupled quiet-hours state. Android `SharedPreferences.commit()` can return false after exposing a process-local attempted value. Activity recreation, Back/Escape, parent-mode changes, rapid double submit, oversized paste, the IME, 320dp width, 200% text, and concurrent Settings surfaces all affect authorship. Fast boolean/list choices should not inherit modal friction without evidence.
- Failure modes of Position A: Focus disposal can skip the final edit or write a parseable prefix; a global/current-value observer cannot identify the initiating request; a timeout creates a late-outcome ambiguity; an unchanged or normalized value can wait forever; and a failed disk commit can still make the attempted value look current in process memory. Position B adds one tap and can hide external changes unless the modal tracks semantic source identity, retains draft/conflict state, and keeps its actions above the IME.
- Synthesis/decision: Use Position B only for typed Settings. The modal owns a bounded saveable draft, semantic opening/source identity, explicit conflict and discard behavior, and a durable-retry obligation. Save admission is atomic and request-scoped. Confirmed repositories serialize authored writes, use durable commit, restore the prior process-visible state on commit failure, and settle the exact receipt off the main thread. While a request runs, observed values never advance the durable baseline. Parent modes and section navigation cannot silently invalidate the child editor. Explicit Cancel may discard; Back, outside dismissal, and Escape require confirmation whenever uncommitted intent or durability ambiguity remains. Unchanged values avoid writes unless a prior failed request still requires durable retry.
- Reason the selected design is superior for this application: It trades one deliberate tap for truthful, testable configuration authorship across Gym, Health, reminders, backup, and time policy. It supports one-handed and assistive use, distinguishes region zones from follow-device/fixed-offset modes, survives normal lifecycle changes, and reuses Whip's narrow persistence coordinator without spreading a generic form DSL or modalizing harmless toggles.
- Status: Accepted after the inline, timeout, process-observation, commit-failure, and Back-dismiss proposals were challenged and corrected by domain, UX/accessibility, and adversarial-QA specialists; implemented in `IMP-20260901-017` and fully verified in `VER-20260901-019`.

### DEC-20260901-025 — Gym layout uses an exact batch Arrange editor; discrete mutations remain narrow

- Context: Active-workout exercise order, groups, retired placements, tombstones, and Set rows form one historical layout, but normal logging must stay fast and legible. The previous always-available reorder/remove callbacks could race one another, advance revisions for a semantic no-op, restore into a newer position collision, or deliver a failure to the wrong surface.
- Position A: Refine always-visible drag/reorder controls and persist every gesture immediately. This minimizes mode switching and resembles a generic list editor.
- Position B: Put structural layout changes in a dedicated batch Arrange editor, while retaining narrow exact transactions for discrete add, remove, group, machine, Set, discard, and History-copy actions.
- Relevant evidence and constraints: In-gym users operate one-handed with limited attention; drag affordances compete with Set logging; groups require contiguous collision-free order; retired placements and tombstones remain historical evidence; exact layout Undo must not roll back newer Set values or completion; and History Copy can cross route or process reconstruction. The existing stable UUIDs and session revisions support precise boundaries without a generic programming DSL.
- Failure modes of Position A: Normal workout cards stay visually noisy, partial gesture persistence creates excess revisions and stale conflicts, group invariants can be exposed between writes, and a broad Undo can erase unrelated newer work. Position B adds an explicit mode switch and can reject an old batch after concurrent change; it also fails if its fingerprint includes mutable Set performance or if global busy/error state leaks between owners.
- Synthesis/decision: Select Position B for layout. Capture an exact versioned structure boundary, include active and retired placement/group/Set structure, exclude Set values/completion from the canonical fingerprint, and commit the whole collision-free arrangement atomically. Scope its Undo to the same session/data generation and preserve newer Set values. Keep discrete operations as stable-identity, replay-safe exact transactions. Give active-session and History Copy separate lifecycle-persistent coordinators, while combining their busy state only to prevent conflicting interaction.
- Reason the selected design is superior for this application: It keeps the normal training surface fast and glanceable, makes advanced structure editing deliberate and reversible, preserves completed facts and tombstones, and gives each action a small intelligible conflict boundary. It supports serious Gym customization without imposing a generic builder or freezing the full workout graph for every Set edit.
- Status: Accepted after domain, lifter/UX/accessibility, and adversarial-QA specialists repeatedly challenged tombstones, value-preserving Undo, lifecycle ownership, responsive controls, and cross-coordinator races; implemented in `IMP-20260901-018` and fully verified in `VER-20260901-020`.

### DEC-20260901-026 — Whole-app destructive maintenance closes one global data-access gate

- Context: Reset Whip touches portable-backup ownership, Health reconciliation/deletion, reminder claims and notifications, every Room table, background workers, active editors, and runtime projections. The existing Health and reminder locks did not prevent an already-admitted Gym or other repository mutation from committing after the database clear.
- Position A: Keep local subsystem locks and add more reset-specific cancellation calls. This is smaller and lets unrelated work continue during reset.
- Position B: Close one application-wide admission gate, drain every active data lease, quiesce workers/runtime, advance the user-data generation, perform reset under a documented lock order, rebuild runtime, and only then reopen access.
- Relevant evidence and constraints: A production `CoordinatedMeasurementRepository` concurrency test proved the Health → reminder → Room order does not deadlock. A reset-vs-Gym regression proved Position A could report success while a late writer recreated user data. Reset has no rollback marker, so failures must leave access blocked only while runtime cannot safely resume, not silently pretend atomic rollback exists.
- Failure modes of Position A: It requires an ever-growing list of domain locks, misses new repositories, permits late commits, and makes destructive success dependent on cancellation timing. Position B temporarily hides normal runtime and can deadlock if the caller already owns a data lease or if lock order is inconsistent.
- Synthesis/decision: Select Position B. `SettingsViewModel` invokes reset outside a normal data-access lease; `StartupRecoveryGate.runExclusiveMaintenance` closes admission and drains active owners; `WhipApplication.resetAllData` clears portable-folder ownership and acquires Health → reminder → Room, advances generation before deletion, cancels visible notifications, and rebuilds background/runtime state before `Ready`. Late access is rejected rather than queued under stale ownership.
- Reason the selected design is superior for this application: Reset becomes a truthful whole-product operation that remains correct as Whip gains domains. The brief recovery surface is safer and easier to test than scattered cancellation, and generation invalidation prevents stale editors from acting on the new empty dataset.
- Status: Accepted after engineering, UX, and adversarial-QA challenge; implemented in `IMP-20260901-019` and verified in `VER-20260901-021`.

### DEC-20260901-027 — Portable backups exclude device-local recovery state; private rollback preserves it

- Context: Health deletion/sync journals and last-action receipts are required to recover interrupted work on the current installation, but moving them to another device can trigger irrelevant cleanup or describe provider state that does not exist there. Replace restore also needs a private pre-restore snapshot capable of returning the current installation to its exact operational state.
- Position A: Use one backup payload for both user export and internal rollback. This is simple and guarantees every current field round-trips everywhere.
- Position B: Define one historical/user-data contract with two export profiles: portable export omits device-local operational state, while a private recovery snapshot preserves it for same-install rollback.
- Relevant evidence and constraints: Completed workouts, custom units, user Exercises, performed sets, Training Max history, and other authored facts must remain portable. Health journals/receipts are operational proof scoped to a provider installation. Removing them from internal rollback would make a failed restore lose pending recovery state; exporting them to another device would create false work.
- Failure modes of Position A: Cross-device restore can inherit stale provider cleanup and misleading sync outcome. Position B can accidentally omit authored history if profile selection is broad or ad hoc, and a private recovery export must never become the user-visible portable artifact.
- Synthesis/decision: Select Position B with an explicit `exportRecoveryBackup` entry point used only by `RestoreRecoveryManager`. Portable export excludes only enumerated local Health journal/receipt fields; private rollback includes them. Import remains backward-compatible and completed historical facts are not recomputed.
- Reason the selected design is superior for this application: It separates user-owned history from installation-owned recovery proof without introducing two schemas, preserves exact rollback, and makes cross-device restore unsurprising and safe.
- Status: Accepted after backup/recovery and Health challenge; implemented in `IMP-20260901-019` and verified in `VER-20260901-021`.

### DEC-20260901-028 — Restore compatibility is proven against each domain’s real unit contract

- Context: A structurally valid backup can contain values that are impossible for the owning feature: unknown/custom units where Gym accepts only built-ins, mismatched entered/canonical nullability, incorrect dimensions, non-finite canonical conversion, duplicated stable identities, or a machine load inconsistent with its interpretation. A generic “unit exists and dimension matches” check is insufficient.
- Position A: Accept any registered compatible unit and let each repository normalize after restore. This is flexible and minimizes preflight code.
- Position B: Before replacement, validate every unit-bearing fact against the actual historical contract of its domain, including canonical parity, raw conversion, stable identity, and feature-specific restrictions; grandfather legacy display labels that do not alter conversion semantics.
- Relevant evidence and constraints: Gym prescriptions/workouts/TM decisions/PRs use built-in load and distance semantics; Tracks, Goals, Habits, and Measurements can intentionally use custom units. Historical rows must describe what happened, not be reinterpreted by current defaults. Existing users may have older long custom-unit names/symbols even though new UI authorship is now bounded.
- Failure modes of Position A: Restore can commit impossible facts, overflow canonical values, or silently reinterpret history. Position B can reject legitimate old data if it applies current UI length limits retroactively or hardcodes a generic unit whitelist detached from the owning model.
- Synthesis/decision: Select Position B. Preflight walks Measurements, Habit logs, Goals, Tracks, exercises, machines, routines, workout history, TM snapshots/decisions, PRs, and automation constants. It enforces value/unit and entered/canonical null parity, finite expected canonical values, actual Gym built-in/canonical contracts, machine-aware load semantics, and exact stable identities. Existing custom-unit labels are grandfathered; new names/symbols remain bounded at the authoring UI.
- Reason the selected design is superior for this application: Restore fails before destructive replacement when historical meaning cannot be proven, while legitimate legacy customization remains portable. The policy is precise enough for serious Gym history without making flexible productivity domains artificially rigid.
- Status: Accepted after domain, product, and adversarial-QA challenge; implemented in `IMP-20260901-019` and verified in `VER-20260901-021`.

### DEC-20260901-029 — Legacy canonical repair requires exact paired-fact proof

- Context: An older automation path could write a Habit log whose canonical value was derived incorrectly for a custom unit. Rejecting every affected backup preserves bad analytics; blindly recomputing all generated rows risks rewriting intentional history or rows whose source fact no longer proves the conversion.
- Position A: Never repair; reject mismatched backups and leave live legacy rows unchanged.
- Position B: Recompute generated Habit canonical values whenever a current unit definition is available.
- Relevant evidence and constraints: Generated rows retain a paired metric entry with provenance, metric identity, raw value, unit, and canonical value. User-authored Habit history must never be rewritten. Unit definitions can change through versioning, and mere numeric similarity is not proof of original intent.
- Failure modes of Position A: Upgraded users retain a known calculation defect and portable backups fail despite recoverable evidence. Position B can alter unrelated or intentionally authored history and apply a newer unit contract to an older fact.
- Synthesis/decision: Apply a narrow proof-based repair only when the paired metric entry matches the full generated-row provenance, value, unit, metric, and canonical-conversion contract. Normalize qualifying portable backups during preflight and run the same awaited repair at startup while normal data access remains closed. Future generated writes take canonical truth from the metric entry and repair an existing exact generated row if replayed.
- Reason the selected design is superior for this application: It corrects the demonstrated legacy defect without general historical recomputation. Exact provenance turns repair into evidence-based reconciliation rather than a heuristic migration.
- Status: Accepted after domain and QA challenge; implemented in `IMP-20260901-019` and verified in `VER-20260901-021`.

### DEC-20260901-030 — External Task capture is bounded, ordered, and draft-safe

- Context: Android share targets can receive arbitrary `CharSequence` text and rapid `onNewIntent` deliveries. Whip retained the full extra in Activity saved state, interpreted every additional nonblank line as a subtask, and represented only one launch request. A large deliverable share could therefore fail during recreation, while a second share or widget Add Task could replace an open draft without a save/discard decision.
- Position A: Treat the newest platform intent as authoritative, immediately replace the editor, and report excess work with transient feedback. This is small and fast when only one intent arrives.
- Position B: Bound shares at the Activity boundary; retain accepted launch requests in a saveable FIFO; admit a new Task editor request only into a saveable conflict handoff; and retain a counted overflow fact until the user acknowledges that exact delivery/count.
- Relevant evidence and constraints: Android can reject an intent above Binder limits before Whip runs, so Whip controls only payloads it receives. `Intent.EXTRA_TEXT` is a `CharSequence`, emoji must not be split, and UTF-16 length is not a truthful character count. Activity recreation may occur before or after Compose admission. Widget/notification/deep-link actions must not inherit share-only capacity. A queued widget Add Task owns both its requested date and resolved creation Area even if saving the current draft changes visible Area scope.
- Failure modes of Position A: It silently loses authored drafts, later intents, or rejection notices; it can misdescribe a dropped notification as a share; and process death can erase a Snackbar before the user sees it. Position B can deadlock or replay unless consumption is exact-head-only, save/restore distinguishes an intentionally empty FIFO, conflicts provide Replace/Keep Editing, and acknowledgment binds both delivery ID and observed rejected-share count.
- Synthesis/decision: Select Position B. Bound raw share input to 8,192 code points and 32 KiB UTF-8, then retain a 200-code-point title plus at most 50 subtasks of 200 code points each. Normalize line endings and blank lines, never split a code point, and surface a persistent shortening warning. Keep accepted platform launches in an Activity-owned FIFO restored as an exact sequence; limit only waiting Task shares to four; collapse further shares into one counted overflow marker; never capacity-drop widget, notification, or deep-link actions. A saveable Task-editor handoff freezes capture text, shortening, date, and resolved Area and requires explicit Replace or Keep Editing. The overflow marker remains the FIFO head through recreation and is consumed only after acknowledgment of its exact delivery ID and current count.
- Reason the selected design is superior for this application: Share-to-Task remains fast for ordinary capture while large, rapid, mixed-source entry is deterministic and honest. Existing drafts and platform actions survive, partial imports are visible, Area/date ownership remains stable, and the architecture stays a narrow launch policy rather than a generic import framework.
- Status: Accepted after repeated platform, UX/accessibility, lifecycle, mixed-entry, and adversarial-size challenge; implemented in `IMP-20260901-020` and verified in `VER-20260901-022`.

### DEC-20260902-001 — Use a single-developer implementation loop

- Context: The initial maximum-quality instruction deliberately required extensive multi-agent audits, simulated focus groups, formal dialectics, and repeated adversarial approval rounds. That discovery phase produced a substantial evidence-backed backlog, but continuing the same orchestration for each implementation issue adds latency, context volume, and process overhead.
- Decision: The primary agent now owns reproduction, design judgment, implementation, focused regression coverage, proportional shared checks, emulator verification, concise durable memory, and commit/push. Work proceeds one coherent priority chunk at a time. No subagents, simulated agent panels, recursive audits, formal debates, or repeated approval rounds are used unless the user explicitly requests them.
- Rationale: The existing backlog and test infrastructure provide enough evidence to implement directly. This preserves correctness, compatibility, accessibility, durable memory, and revertible commits while returning the day-to-day workflow to normal software development.
- Scope: Historical audit records remain unchanged as evidence of work already performed. The simplified process overrides orchestration language in the reusable goal for all future work; it does not weaken product acceptance criteria or release gates.
- Related: `FB-20260902-001`, `MAXIMUM_QUALITY_GOAL.md`
- Status: Accepted by direct user instruction and active from 2026-09-02.

### DEC-20260902-002 — Habit timers use a durable canonical session ledger

- Context: A timestamp stored on `Habit` cannot safely own retry, unit conversion, reboot, restore, competing Start requests, or stale Stop actions, while historical duration must remain an immutable statement of what was logged.
- Decision: `habit_timer_sessions` is the authoritative timer ledger. Each Start has a stable request/session ID, one unresolved session per Habit, frozen Duration-unit identity, wall anchor for understandable recovery, monotonic anchor plus boot identity for exact running time, accumulated canonical seconds, and terminal Completed/Discarded tombstones. `Habit` retains only observable lightweight mirrors for UI/widget projection.
- Runtime policy: Running time and its display use monotonic elapsed time. Boot mismatch, missing monotonic identity, legacy migration, or portable restore becomes `ReviewRequired`; the user may correct and Stop & Log, Continue from a confirmed duration, or Discard. Exact Stop converts canonical seconds through the frozen unit and writes the Measurement, Habit log, terminal session, and cleared mirrors in one Room transaction. Competing Start request IDs are consumed so a delayed replay cannot create a surprise timer later.
- Compatibility: Schema 41 migrates every legacy active timestamp into review without creating or changing history. Portable backup format 18 carries unresolved timers only as review-required and removes device clock identity; private rollback preserves exact sessions; merge imports completed history but never a foreign active timer. Existing completed logs and metric entries are never recomputed.
- Rationale: This is narrower and more auditable than a generic timer engine, while providing the ownership and historical guarantees required by a productivity app that can be interrupted, restored, and controlled from widgets.
- Related: `FND-20260902-001`, `IMP-20260902-002`, `VER-20260902-002`.
- Status: Accepted and implemented.

### DEC-20260902-003 — Habit Today models action needed, finished, and unavailable as different states

- Context: Completion, skipping, pausing, and an off-schedule date have different historical meanings but all affect whether a user should act today. Treating them as one generic pending/done boolean makes Today counts and quick actions misleading.
- Decision: A completed or skipped Habit is finished for today's attention queue, while skip remains a distinct neutral historical outcome and never becomes completion. Paused and off-schedule Habits are unavailable for ordinary one-tap check-in and state that no check-in is expected. Intentional outside-schedule logging is exposed only as an explicit inspector action. An unresolved active timer overrides availability filtering so Stop/Review cannot disappear. User-visible time conversion uses `LocalWhipZone`.
- Rationale: The model stays narrow—no new persistence state or generic workflow engine—while matching what users need to decide at a glance. It prevents accidental history without forbidding deliberate exceptions and preserves truthful skip, completion, pause, and timer semantics.
- Compatibility: Existing Habit rows, schedules, pauses, skips, check-ins, timer sessions, history, Room schema 41, and backup format 18 are unchanged. The legacy `habit-done-disclosure` test tag remains stable while visible language becomes “Finished for Today.”
- Related: `FND-20260902-002`, `IMP-20260902-004`, `VER-20260902-004`.
- Status: Accepted and implemented.

### DEC-20260902-004 — Habit History follows effective dates and includes started neutral events

- Context: A creation/update timestamp answers when Whip wrote a row, while a Habit event's local date answers when the user's check-in, skip, or pause applied. History and Insights are user-facing accounts of the latter.
- Decision: Sort Habit history first by effective local date and only use write time/identity as a same-day tie breaker. Include check-ins, skips, and pauses whose start date is today or earlier in one editable “Habit History”; keep future pauses in Options until they begin. Paused/skipped dates are neutral authored events, not completion or failure. When no completed/missed period exists, Insights says “No scored periods.” Pause changes covering today/past explicitly disclose derived-stat recalculation while preserving check-ins and skips.
- Rationale: This produces the chronology users mean without changing persistence or inventing historical snapshots. It keeps future planning separate from occurred history, makes neutral states explainable, and warns precisely where a schedule edit can alter derived interpretation.
- Compatibility: Existing check-ins, skips, pauses, timestamps, Room schema 41, backup format 18, and calculation rules are unchanged. Presentation is derived from existing immutable facts; no row is migrated or recomputed in storage.
- Related: `FND-20260902-003`, `IMP-20260902-005`, `VER-20260902-005`.
- Status: Accepted and implemented.

### DEC-20260902-005 — Enlarged text preserves named direct navigation; clear Home preserves context

- Context: Six short primary destinations can fit one phone row at some enlarged scales but not all. A fixed threshold discarded all visible names at 150%, while a fixed rail width clipped them. Home also treated “nothing due” as sufficient even when existing work was merely outside the dashboard.
- Decision: Measure rendered destination labels against the actual compact width. Use one stable named row when every label fits and two stable three-item named rows otherwise; never fall back to icon-only primary navigation. Size the rail from its rendered names and make its direct list vertically scrollable in short windows. On a settled, clear returning Home, show at most three concrete recovery routes ordered Inbox → Upcoming → Habits → Goals → Tracks → Gym, while recent completion evidence continues to route to Review & Trends.
- Rationale: The interface adapts to real content instead of an arbitrary font threshold, retains one-tap/muscle-memory access, and bounds Home assistance without becoming a guilt dashboard or a second navigation system.
- Compatibility: App-destination order, saved workspace state, Back behavior, deep links, keyboard shortcuts, Room schema 41, backup format 18, and every domain record remain unchanged. Recovery links only select existing workspace destinations.
- Related: `FND-20260831-011`, `FND-20260831-017`, `FND-20260902-004`, `IMP-20260902-006`, `VER-20260902-006`.
- Status: Accepted and implemented.

### DEC-20260902-006 — Global search owns cross-Track discovery; local search owns collection filtering

- Context: Whip already had a complete global index and exact result router for Track definitions and Entry content, while a second Track-list query existed only as unreachable state. Adding another visible text field would duplicate scope, conflict with reorder behavior, and place two search owners in the same compact workspace.
- Decision: Use the persistent workspace search as the single cross-Track owner and name its exact live scope in the accessibility action, placeholder, scope summary, and empty guidance. Keep Activity search for filtering the visible chronological feed and per-Track Entry search for one Track's history. Remove the unreachable Track-list query branches. Route an archived Track result to the Archived destination and wait for the scoped projection before consuming a cross-Area request.
- Rationale: Users get one predictable entry point that is more capable than the dead branch, while legitimately different local searches remain close to the collections they filter. The design reduces state and contradictory controls without removing any reachable behavior.
- Compatibility: Search indexing, Entry FTS, Track/Entry identities, Area scope, saved workspace state, Room schema 41, backup format 18, and all history remain unchanged. Only action labeling, result presentation/navigation, and unreachable UI state changed.
- Related: `FND-20260831-018`, `FND-20260902-005`, `IMP-20260902-007`, `VER-20260902-007`.
- Status: Accepted and implemented.

### DEC-20260902-007 — Test real viewport contracts on valid profiles and preserve semantic parity across layouts

- Context: A 320dp physical root cannot render a synthetic 360dp or desktop-width host merely because a test requests it. Conversely, a clipped result under a real API 26 keyboard is a production defect, not a test inconvenience. Wide master/detail layouts may intentionally present the selected identity in more than one semantic region.
- Decision: Validate compact behavior on a physically compact API 26 profile, adaptive/fold/wide contracts on an API 37 display large enough to host them, and the complete product gate on the representative API 34 phone. Tests derive physical edges from the root, scroll through the owning collection before interacting, branch only on genuine compact-versus-persistent navigation, and scope intentional wide-pane duplicates to the relevant owner. Search disclosure changes reset to the state-summary region; emoji search prioritizes the result above custom creation while the keyboard is open.
- Rationale: This keeps production geometry honest, prevents false failures caused by impossible canvases, and still treats every defect reproducible in a valid supported viewport as a product bug. Semantic parity matters more than forcing compact and wide compositions to expose identical node counts or simultaneous visibility.
- Compatibility: Presentation, focus, keyboard, and test targeting only. No Room schema, backup format, search index, identity, workout/program rule, saved record, or historical fact changed.
- Related: `FND-20260902-006`, `IMP-20260902-008`, `VER-20260902-008`.
- Status: Accepted and implemented.

### DEC-20260902-008 — Area changes use one request-owned result with repository-authoritative truth

- Context: Areas are shared ownership metadata for Tasks, Habits, Goals, Tracks, Entries, saved scopes, and widgets. Their apparently small controls can therefore trigger cross-domain moves, deletions, and Settings reconciliation. Optimistically closing a dialog before that work finishes loses authorship and cannot distinguish a rejected transaction from a committed transaction whose derived cleanup failed.
- Decision: Serialize Area-management mutations through one ViewModel-owned request state and return a typed receipt naming the exact operation, source identity, optional destination identity, and committed follow-up warnings. The initiating surface retains its draft/choice, disables dismissal and duplicate submission only while saving, consumes only its matching result, and closes on authoritative success. Repository/database operations remain the source of truth; destructive methods do not depend on an asynchronously collected UI projection. Read-modify-write color changes are transactional. Archived-name creation restores the same Area and preserves its color; archive, restore, search, and conflict copy all model archived state explicitly.
- Rationale: This gives consequential cross-domain operations the same lifecycle and retry truth as other authored Whip changes without forcing harmless pickers or immediate preferences into a generic transaction DSL. A committed warning cannot trigger an unsafe retry, while a pre-commit failure leaves the user's intent intact and understandable.
- Compatibility: No Room schema, migration, backup format, Area ID, assignment, historical record, or existing saved color is rewritten. Existing archived Area identity is reused on restoration. Atomic deletion and assignment behavior remains in the existing repositories/coordinators.
- Related: `FND-20260831-019`, `FND-20260901-027`, `FND-20260902-007`, `IMP-20260902-009`, `VER-20260902-009`.
- Status: Accepted and implemented.

### DEC-20260902-009 — Tags use explicit global operations and a stable archived lifecycle

- Context: Tags are shared labels whose canonical registry row is keyed by ID while Task, Habit, Goal, and Track references are intentionally denormalized names. That makes a rename or merge cross-domain, while archive should hide a reusable choice without rewriting saved items.
- Decision: Keep the existing schema, but define separate transactional operations: Rename changes one Tag’s spelling and every matching reference; Merge replaces the source spelling with one active destination across all four domains and then removes only the source row; Archive changes only registry visibility and preserves every item reference. Startup/save reconciliation may ensure that a referenced label exists but cannot silently restore an archived Tag. Explicit Create/Restore may reactivate the same identity. Commas remain reserved by current CSV persistence and are rejected in both domain and UI. The manager owns create, rename, merge, archive, restore, search, usage disclosure, and exact request receipts.
- Rationale: The design fixes demonstrated correctness and usability failures without migrating all four product domains to a speculative tag-link abstraction. Archived state becomes durable and predictable, global operations are falsifiable, and users can see consequences before acting while ordinary item editors remain lightweight.
- Compatibility: No Room schema or backup-format change. Existing Tag IDs and item records are retained. Archive never rewrites references; explicit Rename/Merge intentionally updates Tag labels on saved Tasks, Habits, Goals, and Tracks while preserving their identities and other history.
- Related: `FND-20260831-019`, `FND-20260901-027`, `FND-20260902-008`, `IMP-20260902-010`, `VER-20260902-010`.
- Status: Accepted and implemented.

### DEC-20260902-010 — Workout deletion removes the selected fact graph and preserves programming decisions

- Context: A completed Workout owns placements, groups, and Sets, but it is also referenced by reconstructible personal records and immutable 5/3/1 Training Max decisions. Gym History previously confirmed against asynchronously collected UI projections and closed before persistence reported an outcome.
- Decision: Review and commit permanent Workout deletion through one transaction-derived `WorkoutDeletionImpact` and revision token. Delete only the selected session graph; reject active sessions and changed reviews. Rebuild personal records after commit, apply the retired-Link compatibility policy without retracting its audit rows, cancel the timer afterward, and classify failures in those post-commit follow-ups as warnings. Preserve Training Max decisions, Goal contributions, generated Habit check-ins, automation occurrences, Exercise definitions, and Routine templates as historical facts. Keep the dialog/request owned until exact success, failure, or read-only recovery verification.
- Rationale: Historical workout Sets are authoritative performed facts, personal records and timers are reconstructible projections, retired Link/automation rows are immutable audit history, and Training Max decisions explain why later programming changed. This boundary prevents stale destructive confirmation, avoids false retries after commit, and keeps 5/3/1 and automation history truthful without adding a new schema or generic programming DSL.
- Compatibility: No Room schema, migration, or backup-format change. Only the explicitly selected completed Workout graph is removed. Existing Training Max decisions and linked historical facts remain; derived records are reconciled from surviving workout data.
- Related: `FND-20260831-019`, `FND-20260901-025`, `FND-20260902-009`, `IMP-20260902-011`, `VER-20260902-011`.
- Status: Accepted, implemented, and fully verified.

### DEC-20260902-011 — Machine deletion uses the shared exact Gym request lifecycle

- Context: Machine deletion already had a transaction-derived impact and revision check, but its presentation result was not owned across recreation. Completed workout equipment snapshots are historical facts while routine references are current editable definitions.
- Decision: Represent Machine deletion as `GymDeletionKind.Machine` in the same bounded request lifecycle used by other exact Gym deletions. Save the target ID, UUID, data generation, reviewed impact, and revision; reject UUID or revision mismatch; serialize one owner; and on process recovery read current repository truth. An absent exact target settles achieved once, while a present or unverified target requires retry/review. Preserve the existing transaction semantics: block active use, delete only the selected Machine, mark affected routines as needing equipment, and retain completed-workout snapshots.
- Rationale: One lifecycle gives every consequential Gym deletion the same authorship, replay, and recovery guarantees without duplicating a coordinator or inferring deletion from asynchronously collected lists. It preserves history while making uncertainty explicit.
- Rejected alternatives: Callback-only success because it is lost across lifecycle changes; list-based absence inference because projections may be stale; and a separate Machine-only coordinator because it would duplicate exact-deletion state and drift.
- Compatibility: No Room schema, migration, backup-format, completed-set, workout snapshot, Exercise, or Routine identity change. Only the reviewed Machine profile is removed; existing affected routines retain identity and are marked for repair.
- Related: `FND-20260902-010`, `IMP-20260902-012`, `VER-20260902-012`.
- Status: Accepted, implemented, and fully verified.

### DEC-20260902-012 — Adopt bounded VERA routing without restoring recursive Whip process

- Context: The provided VERA bundle defines cheap-first Luna/Terra/Sol routing, explicit risk gates, bounded retries, and deterministic stopping. The user separately directed Whip back to a conventional primary-developer process with no recursive panels or unrequested subagents.
- Decision: Install VERA globally and in Whip, including all five roles, a three-agent cap, one same-tier repair, deterministic verification order, and selective Sol escalation. Global defaults become Terra/medium for new sessions. Whip retains a repository-level override: roles are available, but delegation occurs only when the user explicitly requests it. Preserve all existing global notify, MCP, plugin, trust, desktop, approval, and sandbox settings.
- Rationale: This makes the requested agent system available and reproducible while honoring the more specific Whip workflow preference and preventing the unbounded debate/retry behavior the user rejected.
- Compatibility: No application source, build, persistence, user data, release artifact, or device state changes. The current already-running Codex session keeps its existing model; new sessions load the new defaults.
- Related: `FB-20260902-001`, `FB-20260902-002`, `IMP-20260902-013`, `VER-20260902-013`.
- Status: Superseded by `DEC-20260902-013`; the user explicitly rejected the merged Whip override and requested canonical VERA only.

### DEC-20260902-013 — Canonical VERA is the sole orchestration policy

- Context: The initial installation preserved VERA's mechanics but paraphrased its policies and layered ATIS globally plus a Whip-specific no-unrequested-delegation rule. The user explicitly requested a clean orchestration slate and an independent VERA repository.
- Decision: Treat `commvnist/vera-codex` as the canonical source. Install its `AGENTS.md`, `.codex/config.toml`, five custom agent definitions, and `routing-policy.yaml` without content changes in Whip. Globally, install the same AGENTS/routing/role contents and exact VERA model/agent values while preserving unrelated machine-local integrations. Remove `atis-fast-explorer.toml`, all ATIS instructions, and the Whip conventional-development adaptation. Do not retain another agent methodology in the active instruction chain.
- Rationale: Archive parity makes the installation falsifiable and eliminates ambiguity about whether VERA or a locally synthesized hybrid governs orchestration. Preserving unrelated integrations avoids conflating an agent-policy reset with destructive loss of authentication, plugins, MCP servers, notifications, project trust, or desktop preferences.
- Compatibility: No Whip application source, schema, data, build artifact, or device state changes. Historical product-memory records remain as an audit trail but are not active Codex instruction sources. New Codex sessions load the reset instruction chain; this already-running session retains the instructions it began with.
- Related: `FB-20260902-004`, `IMP-20260902-014`, `VER-20260902-015`.
- Status: Accepted and implemented.

### DEC-20260902-014 — Give every physical release a unique upgrade identity

- Context: The latest source is newer than the phone-verified `0.3.34` release, but the build metadata still reused version code 40 and version name 0.3.34. Reinstalling materially different code under the same identity would make upgrade diagnosis, rollback tracking, and installed-artifact evidence ambiguous.
- Decision: Release the current candidate as version `0.3.35` with monotonically increasing version code 41. Require the full release gate and signing verification before a data-preserving `adb install -r`; verify the exact endpoint is a physical device and capture installed version, signer, hash, and `firstInstallTime` before and after installation. Do not clear application data or run instrumentation on the phone.
- Rationale: A unique release identity makes support and provenance falsifiable while retaining Android's normal in-place upgrade and Room migration path for existing users.
- Compatibility: The version change does not alter application data. The candidate includes Room schema 41 and its explicit migrations; previously completed records remain governed by their existing compatibility guarantees.
- Related: `FB-20260902-005`, `IMP-20260902-015`.
- Status: Accepted for the requested physical-device release.

### DEC-20260902-015 — Expand 5/3/1 through generated, editable program structure

- Context: Whip already persists typed phases, Training Max boundaries, work sections, and immutable workout prescriptions, but new-program setup still hardcodes one four-week cycle, one same-lift Supplemental block, and one ungated Joker candidate. Book-level Leader/Anchor structures, exact 7th Week uses, alternate-lift BBB, Joker ladders, and balanced assistance would otherwise require repetitive manual editing or misleading generic placements.
- Decision: Add editable 11-week Leader/Anchor plans generated from typed phase specifications: two three-week 5s PRO Leaders, a 7th Week transition, one three-week PR-set/FSL Anchor, and a closing 7th Week protocol. Use standard 5/3/1 weekly percentages and explicit Deload, TM Test, and PR Test matrices. Advance Training Maxes after Leader 1 and after each completed 7th Week boundary. For schedules that repeat a main lift, persist synchronized non-test protocol templates on every occurrence, retain one explicit TM-test owner, and select deterministic balanced runtime owners so each logical lift executes once while the available training days remain useful. Represent alternate BBB with a dedicated Supplemental placement using the selected alternate lift's own Training Max. Represent one to three Jokers as ordered Optional rows and expose each next row only after successful prerequisite work; a skip, failure, RPE 9+, or RIR 1 or lower ends the ladder. Build optional Push/Pull/Single-leg-Core assistance drafts only from compatible active exercises already in the user's library, with every choice visible and replaceable before save.
- Truthfulness boundary: Public Wendler material establishes the concepts and same-lift/alternate-lift BBB, but does not publish every numeric book prescription. UI calls these “5/3/1 Forever structure” and “book-guided editable”, displays every generated percentage, and tells lifters to verify against the edition/template they follow rather than claiming an official exact template.
- Rationale: Generated sets and existing phase metadata are already the durable domain representation, so no Room or backup migration is necessary. A single new string-backed Supplemental placement kind fixes alternate-lift semantics without a programming DSL. Existing routines are not recomputed; completed sessions retain their original snapshots.
- Compatibility: Once-per-lift runtime protocol ownership requires a phase-specific once-per-lift role plus recognized 5/3/1 template revision 2 or later. Legacy revision-1 Beginners deloads keep every saved repeated exposure; explicitly applying a new protocol upgrades the routine's durable template provenance but opts in only that edited phase. A second untouched legacy protocol phase retains its base role and saved runtime behavior. Template revisions otherwise apply only to newly built or explicitly replaced drafts. Eligibility continues to derive solely from Main work, while Training Max changes synchronize to Supplemental placements for the same exercise. Existing Boolean Joker snapshots remain a correct summary of whether any Joker rows exist.
- Related: `FB-20260902-006`.
- Status: Accepted, implemented, and independently approved after the VERA architecture gates.

### DEC-20260902-016 — Primary editors share one responsive chrome contract

- Context: Full-screen authoring surfaces had independently evolved title, dismissal, navigation, divider, and commit controls, producing inconsistent hierarchy and fragile compact layouts.
- Decision: Use `WhipEditorHeader` for primary Task, Habit, Goal, Track, Track Entry, Routine, Machine, Exercise, tracked-record, and Set editors. The title and one exit/up action own the first row; the filled commit action stays visually primary and moves to a trailing second row when the available width cannot support enlarged text. Routine child pages use Back to the outline; the outline uses one X exit and no duplicate Back/Close actions.
- Rationale: One small shared primitive fixes the visible cross-product inconsistency without coupling form bodies or persistence rules to a generic editor framework. Width-and-font-scale adaptation preserves identity and actions rather than hiding either.
- Compatibility: Presentation only; no domain, Room, backup, routine, workout, or historical data changes.
- Related: `FND-20260902-011`, `IMP-20260902-019`, `VER-20260902-020`.
- Status: Accepted, implemented, and targeted-test verified.

### DEC-20260902-019 — Bound in-memory search before introducing a persistent index

- Context: Unified Search already builds off-main and caps each domain, but per-entity newest-history selection still performed a full sort. A persistent full-text/index schema would add synchronization and migration complexity without evidence that the bounded in-memory source set is insufficient.
- Decision: Select the newest N values with a stable bounded priority queue: one selector evaluation per input, O(N) retained memory, O(total log N) work, descending output, and original-order stability for equal timestamps. Keep the existing independent 2,000-result domain cap and explicit limited-source state. Add deterministic 10,000-value and 10,000-results-per-domain contracts without flaky wall-clock thresholds.
- Rationale: This removes the demonstrated unbounded intermediate work while retaining simple, inspectable search semantics. Operation-count contracts catch algorithmic regressions on any CI host; a persistent index remains available if real measurements later justify it.
- Compatibility: Search projection only; no schema, backup, query syntax, result identity, navigation, or historical data changes.
- Related: `FND-20260902-014`, `IMP-20260902-022`, `VER-20260902-023`.
- Status: Accepted, implemented, and targeted-test verified.

### DEC-20260902-017 — Cross the breaking persistence boundary with a durable data epoch

- Context: The user explicitly authorized an update-time local-data wipe so Whip could stop carrying migratory and compatibility architecture. Room destructive fallback alone cannot coordinate preferences, restore journals, widgets, work, notifications, or stale launch requests and cannot provide an authored confirmation boundary.
- Decision: Make schema 42 and portable-backup envelope 3/data version 19/data epoch 2 the only accepted contracts. Before any Whip database or recovery access, resolve an `AtomicFile` epoch marker from no-backup storage. Existing Whip state without the current marker enters a dedicated two-step reset screen; a durable `ResetInProgress` marker precedes all deletion; an interrupted or confirmed failed reset remains blocked and resumes under one mutex. Clear enumerated Whip preferences and files, cancel work/notifications, recreate and verify schema 42/defaults/a fresh generation, invalidate widgets and launch requests, then mark Current and start normal runtime. Never use Room destructive fallback or accept an old backup.
- Rationale: A durable gate makes the intentional incompatibility visible and recoverable across process death while eliminating the false promise that obsolete shapes remain supported. Exact-current backups and one schema drastically reduce conditional architecture without silently recomputing history.
- Safety boundary: Epoch-inspection failures can only recheck or close; they cannot authorize reset. Rapid retries are state-claimed and mutex-serialized. Non-Whip/platform preferences and external user-owned backup documents are left intact.
- Compatibility: Deliberately breaking. Updating users must explicitly erase local Whip data before entering schema 42; pre-epoch backups are rejected. This is the requested clean slate, not a data-preserving migration.
- Related: `FND-20260902-012`, `IMP-20260902-020`, `VER-20260902-021`.
- Status: Accepted, implemented, and targeted/emulator verified.

### DEC-20260902-018 — Separate persisted enum identity from deliberate interface language

- Context: Kotlin enum names were convenient stable identifiers, but several were also rendered as UI copy. That coupled persistence/programming names to capitalization, spacing, localization, and product terminology.
- Decision: Give every audited user-visible enum an explicit `label` (or purpose-specific `periodLabel`) while continuing to serialize by enum `name` where the current schema requires it. Render localized weekdays through `DayOfWeek.getDisplayName`. Reuse `EditorSectionHeader` for Settings and Gym section hierarchy instead of repeating divider/type/spacing recipes.
- Rationale: Explicit labels allow “In progress”, “All Habits”, “Exercise sessions”, and similar product language without changing stored identities. One section primitive makes hierarchy consistent while keeping each screen's content independent.
- Compatibility: Presentation/model metadata only. Enum constants and storage names remain unchanged; no schema, backup, existing setting, routine, workout, or history is rewritten.
- Related: `FND-20260902-013`, `IMP-20260902-021`, `VER-20260902-022`.
- Status: Accepted, implemented, and targeted-test verified.

### DEC-20260902-020 — Give repeated controls intent-specific semantics

- Context: A repeated visible label can be clear within a visual column while remaining ambiguous to accessibility services and automation once the same action appears for several lifts, fields, or work sections.
- Decision: Shared selection options announce `field label + option value`; repeated 5/3/1 Training Max mode controls expose lift-specific descriptions and stable lift-role tags; workout-only exercise actions are scoped to a named active-workout empty-state region. Retain ordinal selectors only for noninteractive duplicate display assertions where order itself is the contract.
- Rationale: Contextual semantics improve actual assistive use and make regression tests describe intent. They are more durable than globally unique copy or layout-position assumptions and do not add visible noise.
- Compatibility: Semantics and tests only; no schema, backup, routine, prescription, workout, or historical-data changes.
- Related: `FND-20260902-015`, `IMP-20260902-023`, `VER-20260902-024`.
- Status: Accepted, implemented, and targeted-test verified.

### DEC-20260902-021 — Let every consequential secondary dialog own its exact result

- Context: The broad secondary-mutation campaign had one remaining authored draft (Exercise Category) and one destructive graph (Track) that still closed on dispatch rather than authoritative completion.
- Decision: Give Category saving a dedicated request state and dialog-owned coordinator. For Track deletion, build the definition/history/value/Link/automation impact inside one transaction, hash that graph into the reviewed revision, require the same revision at commit, publish one request-owned receipt, and downgrade only post-commit reconciliation failure to a warning. Preserve lightweight synchronous picker and local-preference actions where failure cannot abandon a draft or create an ambiguous historical outcome.
- Rationale: This completes the product-wide interaction rule without imposing a generic workflow engine on harmless controls. Users keep context exactly where retry matters, and permanent deletion becomes falsifiable and safe from stale review or false replay.
- Compatibility: No schema or backup-format change. Category data changes only after a successful authored save; Track deletion remains explicitly permanent but now binds the exact reviewed graph.
- Related: `FND-20260831-019`, `FND-20260901-027`, `FND-20260902-016`, `IMP-20260902-024`, `VER-20260902-025`.
- Status: Accepted, implemented, and targeted emulator verified.

### DEC-20260902-022 — Use one typed request channel for Gym catalog authoring

- Context: Category saving already survived recreation, but Machine/Exercise editors independently mixed local `saving` flags with callbacks owned by a particular composition.
- Decision: Route Category, Machine, Machine-version, Exercise, and nested Exercise-for-Machine persistence through one mutex-serialized ViewModel request state with typed receipts. Compose coordinators reclaim only their namespaced request, retain drafts and inline failures, and close the precise editor layer after success. Extract catalog overlays from the Gym shell so the ownership logic remains testable and coverage instrumentation stays below JVM method limits.
- Rationale: The catalog permits only one foreground authoring operation, so one typed channel is smaller and clearer than five ad hoc callback protocols. Typed outcomes distinguish closing a whole editor stack from returning a newly created Exercise to the underlying Machine editor.
- Compatibility: No schema, backup, catalog identity, Routine definition, active Workout, or completed History change.
- Related: `FND-20260902-017`, `IMP-20260902-025`, `VER-20260902-026`.
- Status: Accepted, implemented, and targeted emulator verified.

### DEC-20260902-023 — Make schema 43 the canonical structured-Gym model

- Context: The user authorized a breaking clean slate specifically to remove migratory and legacy behavior. Schema 42 still carried old program-kind aliases and a duplicated assistance-role representation even though current 5/3/1 generation already expresses Classic/5s PRO and BBB/FSL/SSL/BBS as independent executable policies.
- Decision: Persist only `Static`, `Custom`, or canonical `FiveThreeOne` program identity. Persist work structure only as `placementKind` plus `assistanceCategory`; keep the assistance-role picker as transient builder state. Require applied Training Max values to declare the `Explicit` source and require every Joker candidate, including a single Joker, to follow the ordered 5-point ladder above the final Main percentage. Cross a new explicit fresh-start boundary at Room schema 43, data epoch 3, and portable-backup data version 20; retain only the current schema and accept no migration or older backup.
- Rationale: Main work, supplemental work, optional work, and assistance are orthogonal program concepts, not alternate program identities. One source of truth prevents impossible combinations while retaining full customization. A new epoch is safer and more truthful than pretending a schema-shape removal is compatible.
- Compatibility: Deliberately breaking as previously authorized. Updating installations must confirm the fresh-start gate; historical schema 42 and portable-backup version 19 are rejected rather than transformed. No released history is silently recomputed.
- Supersedes: The current-boundary values in `DEC-20260902-017`; that decision remains historical evidence for the preceding clean-slate step.
- Related: `FND-20260902-018`, `IMP-20260902-026`, `VER-20260902-027`.
- Status: Accepted, implemented, and targeted emulator verified.

### DEC-20260902-024 — Keep current-only Settings and direct users to the first invalid field

- Context: The corrected Settings targeted profile surfaced both leftover preference migration behavior and a Custom Unit form whose inline error could be created outside the visible viewport.
- Decision: Treat stored Settings as current-epoch data only: ignore unknown old keys, stop deleting them as a migration side effect, and derive Health Connect categories only from the explicit current category set. For Custom Unit validation, retain the enabled explanatory action but focus and bring the first invalid Name or conversion-factor field into view. Keep the destructive epoch-reset integration test in an isolated emulator batch rather than mixing it with lifecycle/UI tests.
- Rationale: The data-epoch gate owns destructive upgrade behavior; repositories should not carry a second hidden compatibility system. Error-directed focus preserves clear validation without disabling an action whose purpose is to explain what is missing. Isolated destructive QA is both faster and deterministic.
- Compatibility: Intentionally current-only under the authorized clean reset. No current setting name/value changes; no Room or backup change beyond schema 43. Presentation behavior changes only after an invalid Custom Unit submission.
- Related: `FND-20260902-019`, `FND-20260902-020`, `IMP-20260902-027`, `VER-20260902-028`.
- Status: Accepted, implemented, and targeted emulator verified.

### DEC-20260902-025 — Locale-sensitive composables read observable configuration

- Context: The design-consistency pass intentionally localized weekday labels, but one Habit schedule path read the Java process default directly during composition.
- Decision: Capture the current Android configuration locale in the owning composable and pass that value into weekday label functions. Apply the same explicit observable locale to the corresponding Settings weekday selectors.
- Rationale: Configuration is Compose-observable and preserves the explicit-label contract across runtime language changes without moving presentation concerns into persistence identifiers.
- Compatibility: Presentation-only; no setting, Habit schedule, stored weekday, database, or backup changes.
- Related: `FND-20260902-021`, `IMP-20260902-028`, `VER-20260902-029`.
- Status: Accepted, implemented, and lint verified.

### DEC-20260902-026 — Recovery operation completion includes terminal-state publication

- Context: The core recovery gate updates its own state before returning, while Whip's application-facing aggregate copied that state asynchronously for pre-gate/fresh-start support.
- Decision: In addition to continuous collection, copy the underlying gate's current state in `finally` after replace restore and exclusive reset, directly after pending-recovery blocking, and after retry settles. The gate remains the authority; the application aggregate cannot lag beyond the operation boundary.
- Rationale: This preserves fail-closed access during transitions while giving callers a deterministic postcondition. Waiting or polling in every consumer would spread lifecycle races through UI, workers, widgets, and tests.
- Compatibility: Runtime synchronization only; no persisted data, schema, backup format, or recovery decision changes.
- Related: `FND-20260902-022`, `IMP-20260902-028`, `VER-20260902-029`.
- Status: Accepted, implemented, and repeated emulator verified.

### DEC-20260902-027 — Keep navigation identity independent from interface copy

- Context: `DestinationTabBar` supports an explicit stable tag value, but Habit navigation relied on the default visible label. The label became “All Habits” while the durable enum identity remained `All`.
- Decision: Habit destinations use `HabitDestination.name` for test identity and `HabitDestination.label` for visible and spoken interface language in both loading and normal workspace states.
- Rationale: Product copy should optimize comprehension and remain free to localize; automation identity should optimize stability. Keeping those concerns explicit preserves fast, meaningful regression coverage without compromising interface language.
- Compatibility: Semantics/test identity only. No Habit, schedule, log, setting, database, or backup data changes.
- Related: `FND-20260902-023`, `IMP-20260902-029`, `VER-20260902-030`.
- Status: Accepted, implemented, and repeatedly emulator verified.

### DEC-20260903-001 — Release the breaking epoch without silently erasing phone data

- Context: Version 0.3.37/code 43 intentionally accepts only Room schema 43, data epoch 3, and backup version 20. The connected phone still held the released 0.3.35/code 41 installation and older local-data epoch.
- Decision: Upgrade the signed package in place with `adb install -r`, cold-launch it, and verify that the explicit two-step fresh-start boundary is presented. Do not invoke the in-app erase action as part of deployment or automated smoke verification.
- Rationale: Android package identity and the established signer remain continuous while the product truthfully requires direct user confirmation before the authorized breaking local reset. This keeps deployment and destructive data erasure distinct and auditable.
- Compatibility: The installed application is upgraded, but existing Whip local data remains untouched until the user confirms “Erase all Whip data”. Older backups remain intentionally unsupported by the current epoch.
- Related: `FB-20260903-001`, `IMP-20260903-001`, `VER-20260903-001`.
- Status: Accepted, released, and physically verified.

### DEC-20260903-002 — Existing-routine edit projections copy persisted program policy

- Context: The persistence model retained Performance review, but the edit adapter reconstructed a partial program draft and allowed defaults intended for new programs to replace two omitted persisted fields.
- Decision: Treat the saved routine as authoritative when entering edit mode and explicitly project `progressionMode` and `allowNonStandardHigherSuggestions` alongside all other program metadata. Protect the projection boundary with a round-trip regression fixture using non-default values.
- Rationale: Default constructor values are appropriate for creation, not hydration. An explicit complete projection prevents the editor from visually or durably changing authored program policy.
- Compatibility: No schema, backup, historical Workout, or calculation change. Existing correctly persisted routines immediately reopen with their actual configuration after updating.
- Related: `FB-20260903-002`, `FND-20260903-001`, `IMP-20260903-002`, `VER-20260903-002`.
- Status: Accepted, implemented, verified, and released in 0.3.38/code 44.

### DEC-20260903-003 — Make the Routine edit projection lossless for complete per-lift state

- Context: The program-level projection repair exposed the same default-substitution risk in four `RoutineExerciseDraft` fields that are consumed and persisted by the builder but were absent from its UI projection.
- Decision: Copy Training Max basis kind, basis value, basis unit, and increase eligibility directly from every saved Routine placement. Extend the existing advanced-programming round-trip regression with non-default values for each field.
- Rationale: The edit draft is a complete replacement contract for child placements. Preserving these fields at its only UI hydration boundary is smaller and safer than repository exceptions that guess whether a default was intentional.
- Compatibility: No schema, backup, completed Workout, Training Max calculation, or current-cycle change. Existing routines retain their already-stored values after updating.
- Related: `FB-20260903-003`, `FND-20260903-002`, `IMP-20260903-003`, `VER-20260903-003`.
- Status: Accepted, implemented, verified, and released in 0.3.38/code 44.

### DEC-20260903-004 — Derive Gym summaries from eligible immutable history

- Context: The Gym-wide follow-up found that record eligibility, machine-setting direction, copied-session state, routine-day references, graph-preset validity, and weekly attribution were enforced in different layers or not enforced at all.
- Decision: Centralize record eligibility in the rebuild transaction using the workout placement’s immutable exercise-policy snapshot plus current warm-up/assisted preferences and session inclusion. Store numbered-machine direction in every workout placement and use that snapshot for PR reconstruction and deleted-machine graph fallback. Reset every timer/progression-invalidity field when repeating a workout, remove the unused invalid “finished copy” mode, clear routine-day numeric references before replacing/deleting their rows, validate graph presets atomically, and count weekly records by included source-session identity. Establish current-only Room schema 44, data epoch 4, and backup data version 21 rather than migrating incomplete historical snapshots.
- Rationale: Completed history must remain self-describing, while current user preferences should control whether that history participates in derived records. Session identity/local date is a safer attribution boundary than UTC timestamps, and rejecting malformed authored data is safer than downstream fallback.
- Compatibility: Intentionally breaking under the user-authorized clean slate. Older local data/backups are rejected through the explicit existing reset boundary; the release process installs in place but never confirms erasure for the user.
- Related: `FB-20260903-004`, `FND-20260903-003`, `FND-20260903-004`, `FND-20260903-005`, `IMP-20260903-005`, `VER-20260903-005`.
- Status: Accepted, implemented, verified, and released in Whip 0.3.39/code 45.

### DEC-20260903-005 — Enforce semantic validity at each durable cross-feature boundary

- Context: The post-Gym audit found a repeated pattern outside Gym: presentation paths prevented some invalid inputs, while repositories, notification actions, taxonomy operations, settings, and backup restore could still create ambiguous or cross-owned state.
- Decision: Make repositories authoritative for authored constraints; preserve referenced history through archival rather than deletion; model connected Habit progress and focus timers as ownership-bearing states; update dependent revision/search projections in the same transaction as taxonomy changes; normalize measurement identities before lookup; and require whole-snapshot semantic validation before backup preview, merge, or restore. Serialize read-modify-write mutations that can be reached concurrently.
- Rationale: Every mutation path then observes one falsifiable contract, historical facts remain interpretable, derived projections match canonical rows, and invalid portable data is rejected before partial durable effects. This is targeted hardening of existing domain concepts rather than a new generic framework.
- Compatibility: No new Room schema or backup version was required beyond schema 44/data epoch 4/backup 21. Invalid newly authored or restored data is now rejected; valid current data round-trips unchanged. Checklist definitions referenced by history remain as archived rows.
- Related: `FB-20260903-005`, `FND-20260903-006` through `FND-20260903-009`, `IMP-20260903-007`, `VER-20260903-007`.
- Status: Accepted, implemented, verified, and released in Whip 0.3.40/code 46.

### DEC-20260903-006 — Use a guided full-pane 5/3/1 setup with Program Structure as the edit authority

- Context: 5/3/1 creation has substantially more hierarchy and validation than an ordinary exercise edit, but lifters still need fast arbitrary-lift customization and familiar routine editing after generation.
- Decision: Keep the hybrid model: a dedicated full-pane guided 5/3/1 setup produces the initial structured routine, while Program Structure remains the authoritative place for later program-wide Training Max, phase, progression, and prescription edits. Reuse the shared searchable exercise picker rather than introducing another lift selector. Suppress generic placement rewrite tools only for program-controlled Main/Supplemental work; keep explicit advanced set editing and ordinary-routine flexibility intact.
- Rationale: This preserves a clear novice path and efficient expert customization without locking 5/3/1 to four named lifts, duplicating library UX, or building a generic programming DSL. Central edit routing makes semantic ownership visible and reduces accidental prescription loss.
- Rejected alternatives: Refining the compact alert still constrains a program-sized task; putting every program option directly into ordinary placement editing scatters ownership and conditional logic; disabling all set-level edits would unnecessarily remove expert control.
- Compatibility: No persistence or schema change. Existing routines and completed workouts retain their exact stored structure; only authoring presentation and access to unsafe bulk helpers changes.
- Related: `FB-20260903-006`, `FND-20260903-010` through `FND-20260903-012`, `IMP-20260903-009`, `VER-20260903-009`.
- Status: Accepted, implemented, verified, and released in Whip 0.3.41/code 47.

### DEC-20260903-007 — Treat search and contextual creation as one reusable Gym picker contract

- Context: Selecting an existing Exercise and creating a missing one are two outcomes of the same user intent. Separate or passive empty-state behavior caused inconsistent return paths, repeated typing, and a custom 5/3/1 dead end.
- Decision: The shared single-select Gym picker owns search, visible results, contextual noun labels, a permanent Create action, and a query-specific empty-state Create action. It passes the normalized query to the shared Exercise editor and leaves persistence/return ownership with the invoking workout or program workflow. Multi-select Routine and Machine pickers retain their specialized selection UI but follow the same search/always-create/actionable-empty-state contract. Custom 5/3/1 models “add next lift” as an open slot rather than requiring an existing unused library row.
- Rationale: This gives every Gym entry point the same mental model without forcing different single- and multi-select tasks into one oversized component. The open-slot model supports unlimited successive creation while preserving distinct per-lift Training Max state.
- Compatibility: UI/state-flow only; no persistence or schema change. Existing Exercises, Routines, active workouts, and historical workouts are unchanged.
- Related: `FB-20260903-007`, `FND-20260903-013`, `FND-20260903-014`, `IMP-20260903-011`, `VER-20260903-011`.
- Status: Accepted, implemented, verified, and released in Whip 0.3.42/code 48.

### DEC-20260903-008 — Make numeric quick adds an explicit Habit tracking-mode capability

- Context: Quick increment, preset amounts, and generated ranges only drive one-tap accumulation on the Count and Decimal Habit cards, but the editor exposed their configuration to all manual modes and the shared validator treated the increment as universally meaningful.
- Decision: `HabitTrackingMode.supportsQuickAddAmounts()` is the single semantic gate. Only unsynced Count and Decimal drafts render, parse, validate, and persist quick-add values. Every other mode persists the neutral non-null database representation (`1.0`, empty presets).
- Rationale: Visibility alone would leave hidden invalid state and alternate repository callers inconsistent. One small capability rule keeps UI, validation, and persistence aligned without introducing a generic behavior framework.
- Compatibility: No schema or historical-log change. Existing irrelevant presets on a nonnumeric Habit become invisible immediately and are removed the next time that Habit is saved; Count/Decimal behavior is unchanged.
- Related: `FB-20260903-008`, `FND-20260903-015`, `IMP-20260903-013`, `VER-20260903-013`.
- Status: Accepted, implemented, verified, and released in Whip 0.3.43/code 49.

### DEC-20260903-009 — Conditional UI and durable semantics share one capability authority

- Context: Several editors correctly hid subordinate controls but continued validating or persisting their prior values. Gym also constructed option lists independently from the fields a selected tracking or machine type could produce.
- Decision: Model consequential capability and type rules in pure domain functions, normalize drafts at repository boundaries, and consume the same rules when presenting editor and analytics choices. Hidden fields retain no behavioral authority; completed-workout snapshots remain immutable.
- Rationale: Presentation-only conditionals cannot protect alternate callers, restored drafts, type changes, or legacy-shaped rows. A compact capability layer aligns UI, validation, and persistence without introducing a generic rules DSL.
- Compatibility: No schema or completed-history rewrite. Former At Most rows are interpreted correctly on read and canonicalized when edited; new writes remove irrelevant current-definition fields.
- Related: `FB-20260903-009`, `FND-20260903-016` through `FND-20260903-018`, `IMP-20260903-014`, `VER-20260903-014`.
- Status: Accepted, implemented, verified, and released in Whip 0.3.43/code 49.

### DEC-20260903-010 — VERA-Codex is standing policy, not an opt-in phrase

- Context: VERA roles and routing files were installed, but a development run could still execute directly when the current prompt did not explicitly name VERA.
- Decision: Automatically apply VERA classification and routing to every request that changes, repairs, refactors, tests, builds, packages, migrates, or deploys software. The user's standing authorization satisfies the delegation trigger for required VERA roles; per-task repetition is unnecessary.
- Boundaries: A genuinely trivial task may remain in the parent only through VERA's explicit direct-parent route. Read-only conversation is outside development routing. Standing delegation does not authorize destructive, irreversible, credential, production, billing, publication, or unrelated external effects.
- Rationale: Installation without automatic activation is not truthful use of an orchestration system. Explicit scope preserves the efficient vanilla path for trivial work while guaranteeing the medium/high-risk review gates.
- Related: `FB-20260903-010`, `IMP-20260903-016`, `VER-20260903-016`.
- Status: Accepted and implemented.

### DEC-20260903-011 — Reuse bounded product primitives at semantic ownership boundaries

- Context: Machine-linked Exercise selection and active-workout Rest each locally recreated behavior or styling already owned by an established Whip pattern. The inconsistency was visible, but a repository-wide generic dialog/card framework would add abstraction without proving a product need.
- Decision: Share one Exercise-specific search/create body between single- and multi-select Gym workflows while leaving selection policy with each caller. Use `WhipCollectionCard` as the sole low-emphasis content-surface owner and explicitly centralize its medium shape, color, and elevation. Keep Rest containerless inside the execution lane and let nested creation return through existing domain-specific save receipts.
- Rationale: This removes duplicate search, empty-state, creation, and card-style decisions at the exact boundaries that must stay consistent, while preserving specialized single/multi selection, Machine draft ownership, and timer behavior.
- Rejected alternatives: Merely resizing the Machine alert would retain behavioral duplication; forcing every picker into one generic dialog would couple unrelated selection policies; nesting a rounded Rest card inside another card would create double surfaces; replacing all raw `Surface` uses would conflate content cards with intentional panes, charts, and backgrounds.
- Compatibility: No database, schema, backup, completed-workout, Routine prescription, or timer calculation change. Existing Machine and Routine drafts retain their current save boundaries.
- Related: `FB-20260903-011`, `FND-20260903-019`, `FND-20260903-020`, `IMP-20260903-017`, `VER-20260903-017`.
- Status: Accepted, implemented, and independently approved.

### DEC-20260903-012 — Fresh defaults follow domain meaning and never overwrite authorship

- Context: Numbered machines, measurement Fields, Goal templates, and reminder times each had enough domain information to choose a useful safe starting value, but several creation paths used blank, first-in-list, literal, or already-occupied values instead.
- Decision: Keep semantic defaults small and domain-owned. Level Set insertion uses explicit → same placement → exact completed Exercise/Profile history → direction-aware endpoint precedence. Blank Routine Level templates may derive only their actual setting, never a prescription. Fresh Track Number Fields use live unit/precision preferences; saved/opening drafts remain unchanged. Goal templates convert a canonical basis into the chosen valid unit. Reminder creation chooses and validates an unused conventional slot.
- Rationale: A default is correct only when the product can explain why it is appropriate for that field. Applying it at the creation/persistence boundary keeps quick and full editors consistent, while authorship/history guards prevent convenience behavior from becoming data mutation.
- Rejected alternatives: UI-only picker initialization would leave repository callers inconsistent; always forcing a machine endpoint would discard useful prior work; a generic cross-product heuristic framework would hide unrelated rules and be harder to test; recomputing saved prescriptions or history would falsify authored facts.
- Compatibility: No schema, backup, migration, completed-history, or existing-draft rewrite. Archived Profiles remain valid only for existing bindings; new assignment still rejects them.
- Related: `FB-20260903-012`, `FND-20260903-021`, `FND-20260903-022`, `IMP-20260903-018`, `VER-20260903-018`.
- Status: Accepted, implemented, verified, and independently approved.

### DEC-20260903-013 — Exercise is the interface noun; lift remains an internal compatibility detail

- Context: Gym's library object is an Exercise, but parts of structured-program UI independently called the same object a lift. Renaming persisted `MainLift` identities would add migration risk without improving what users see.
- Decision: Use Exercise/Exercises for every user-facing movement reference, including Main exercise and Supplemental exercise. Retain established internal type, enum, variable, serialization, and test-tag names where changing them would be cosmetic or compatibility-sensitive. Preserve words embedded in real exercise names, search aliases, and “lifter.”
- Rationale: One visible noun makes creation, selection, editing, and programming coherent while keeping the change isolated from user data and historical workouts.
- Compatibility: Copy and assertions only; no schema, backup, progression, prescription, completed-workout, or stored-identity change.
- Related: `FB-20260903-014`, `FND-20260903-023`, `IMP-20260903-020`, `VER-20260903-020`.
- Status: Accepted, implemented, and independently approved.

### DEC-20260903-014 — One summary-first collection density replaces the compact/comfortable choice

- Context: Whip offered an expansive default card and an optional compact row, but the preference bundled information disclosure, insets, shape, and list rhythm. The compact interaction was more scannable and retained full capability through expansion, while its 6 dp vertical inset and 4 dp list gap felt cramped.
- Position A: Keep both modes and only add padding to compact rows.
- Position B: Make standard cards slightly smaller while retaining all details at once.
- Evidence and constraints: The established compact parity contract already preserves primary actions, two-line titles, complete expandable information, multiple saved disclosures, 48 dp targets, and narrow/large-text reflow. Current shared geometry is small shape/10×6 dp/4 dp internal/4 dp list gap versus medium shape/14×14 dp/6 dp internal/12 dp list gap. Track master-pane compactness is viewport-driven and independent from the user preference.
- Failure modes: Treating every internal nested gap as a density token would distort controls; leaving a dead serialized Boolean would contradict the requested clean cut; removing viewport compact code would regress adaptive layouts; forcing summary rows through selection/reorder would lose their dedicated controls; added insets could squeeze titles beside disclosure and primary-action lanes at 320 dp/200% text.
- Decision: Use one summary-first collection grammar. Shared cards use the medium shape, 12 dp horizontal and 10 dp vertical padding, and 6 dp spacing between direct content groups; collection items use an 8 dp gap. Remove the Appearance toggle, `AppSettings.compactItemLayout`, its SharedPreferences key, its backup representation, and every user-density rendering branch. Keep Track master-pane, selection, and reorder paths independent. Bump the exact-match backup format from 23 to 24 so backups containing the retired setting are rejected cleanly. No Room schema/data-epoch change or local reset is required because the obsolete preference byte becomes unreachable.
- Why this is superior for Whip: It preserves the useful information hierarchy of compact rows, restores calm breathing room, removes an unnecessary setup decision, and reduces cross-product behavioral variance without altering data or capability.
- Consequences / reversal conditions: All users see the same collection layout and new backups no longer carry density state; any stale preference key is ignored as an unknown value rather than modeled. Reintroduce a density choice only if validated usage shows a distinct expert workflow that the balanced pattern cannot support without compromising ordinary use.
- Related: `FB-20260903-016`, `FND-20260903-026`, `IMP-20260903-026`, `VER-20260903-026`.
- Status: Accepted, implemented, verified, and independently approved.

### DEC-20260903-015 — Converge UI by semantic role, with destructive review as a separate safety contract

- Context: The whole-product follow-up found real visual and ownership drift after the collection-density cleanup: ordinary information/settings surfaces, dialog bodies, editor identity/organization groups, Gym catalog cards, a Task-owned cross-product date picker, duplicated Unit hierarchy, oversized Gym route ownership, and duplicated external-activity hosting. It also found that Habit permanent deletion lacks the repository-reviewed revision and request lifecycle used by other destructive domains.
- Decision: Reuse remains bounded by product meaning rather than widget shape. Introduce only a standard grouped-information surface and ordinary dialog-body grammar beside the existing collection, notice, settings, editor, and pane-aware dialog primitives. Migrate equivalent Settings and Gym collection callers while preserving separate selection, reorder, warning/provenance, chart/calendar, workout-execution, and destructive-review roles. Move the date picker to a neutral module with no forwarding alias; share slot-based productivity identity/organization layout without moving draft or validation policy; remove duplicate outer Unit labels. Split Gym ownership only at stable route/overlay seams after request-lifetime proof. Share external Activity startup/theme/window hosting without absorbing Widget or Health behavior.
- Destructive-action boundary: Habit owns a repository-authored impact and complete-graph revision, exact transactional revalidation, request-owned preview/save/failure state, stable candidate identity, and commit-aware warning/cancellation result. Presentation may share spacing and status primitives, but impact calculation, warning copy, and mutation semantics remain domain-specific. A stale or uncertain result must never be replayed as a new permanent deletion.
- Review boundary: Track values remain evidence rather than comparable outcomes. Review may show a period-bounded, explicitly All Tracks evidence card with entry and touched-Track counts plus an Open Tracks action, but Track does not enter `ReviewSection`, Area-scoped correlations, or score aggregation.
- Rejected alternatives: A universal card/dialog/form DSL; a blanket raw-`Card` ban; one generic deletion calculation; moving every `GymAreaContent` state into a state bag; scoring arbitrary Track values; retaining compatibility aliases after neutral module moves.
- Compatibility/data: Clean cut at source ownership boundaries, but no schema, data-epoch, or backup-format change and no reset are justified. Existing authored data, completed Gym history, calculations, and persisted Review-section choices remain unchanged.
- Verification: Serialize independently reviewable tranches. Prove ≥48 dp actions and reachability at 320 dp/200% text plus dark, RTL, and fold placement for affected surfaces; run focused domain/UI/lifecycle gates after each tranche, the complete deterministic gate at the end, a fresh Sol architecture review, and visual inspection on the disposable emulator. Do not deploy or reset the physical phone.
- Related: `FB-20260903-017`, `FND-20260903-027` through `FND-20260903-033`.
- Status: Accepted, implemented, fully verified, and independently approved. The bounded semantic exceptions and no-reset boundary remain part of the decision.

### DEC-20260904-001 — Calendar presentation is a small shared contract, not a calendar framework

- Context: Several pages independently rendered weekday abbreviations and month navigation, creating locale and accessibility drift. Other equivalent UI roles had isolated one-off implementations.
- Decision: Share only locale-aware weekday formatting and standard calendar header chrome, plus existing semantic action/information/empty-state roles. Keep state, date selection, data ownership, exercise logic, and feature-specific action policy in their domains.
- Constraints: Preserve `DayOfWeek` persistence identity, two-letter calendar legibility where possible, RTL-aware navigation, 48 dp controls, dark mode, 320 dp width, and 200% text behavior. Do not introduce a universal form/calendar DSL or migration.
- Consequences: Feature pages present the same accessible calendar language and less visual drift without coupling their behavior or data models.
- Related: `FB-20260904-002`, `FND-20260904-001`, `IMP-20260904-002`, `VER-20260904-002`.
- Status: Accepted, implemented, verified, and independently approved.

### DEC-20260904-002 — Device QA fails closed while routine verification stays proportional

- Context: An unscoped connected-test command reached both a disposable emulator and a physical phone. Subsequent repair work also showed that forcing the entire fresh Android inventory after every test-only edit adds substantial delay without increasing evidence quality when exact inputs are unchanged.
- Decision: One shared guard classifies a caller-selected serial from live ADB state and `ro.boot.qemu`. Instrumentation requires exactly one explicit `ANDROID_SERIAL`, state `device`, and qemu value `1`; physical release requires explicit `WHIP_DEVICE`, state `device`, a successful property read, and any value other than `1`. Missing, malformed, ambiguous, offline, unauthorized, or unclassifiable targets fail before execution. Supported wrappers and both Android modules enforce the same contract, and every ADB install/launch uses explicit `-s` targeting.
- Verification policy: `scripts/check --emulator` is the routine acceptance path. It reuses only exact successful batch signatures and reruns affected batches. A fresh complete emulator or coverage campaign is reserved for product/runtime changes, harness/cache-integrity changes, explicit freshness requirements, or evidence investigation. Fresh coverage remains 11 isolated processes with exact class, result, skip, failure, and execution-data accounting.
- Release/data boundary: Physical release remains a separate explicitly authorized action using a signed in-place `install -r`; no fallback selection, compatibility path, uninstall, downgrade, reset, or implicit data migration is allowed.
- Related: `FB-20260904-003`, `FB-20260904-004`, `FND-20260904-002`, `IMP-20260904-003`, and `VER-20260904-003`.
- Status: Accepted, implemented, verified, independently approved, and exercised by the 0.3.47 physical release.

### DEC-20260904-003 — Change gates are proportional; frozen-candidate authority is complete and fresh

- Context: Repeated complete test/build campaigns delayed small changes even when exact affected evidence was available, while release still requires one trustworthy complete qualification of immutable inputs.
- Decision: `scripts/check` deterministically maps every changed path to the union of named profiles and exact selectors, explains each route, and fails closed for unknown production/build/harness inputs. `scripts/candidate` is the only complete release-candidate authority: it snapshots repository inputs, runs complete JVM coverage and fresh complete Android E2E coverage, lint/static checks, and debug/release/AAB/benchmark builds, rejects drift, and publishes canonical checksummed evidence atomically. Release/deployment remains separately authorized and unchanged by this task.
- Android boundary: One shared engine owns inventory, graphics-first/reset-last batching, emulator guards, source/APK/runner/device signatures, exact requested/executed class counts, test/failure/skip counts, and fresh coverage data. Only reusable development mode may accept exact successful cache entries; fresh/candidate coverage never does. Compilation and packaging may remain incrementally up to date because execution freshness is independently proved by new result files.
- Rejected alternatives: Running the full suite after every edit; letting path heuristics silently ignore unknown code; treating cached Android success as frozen-candidate evidence; weakening the explicit emulator-only guard.
- Compatibility recovery: `scripts/check --full` retains its historical device-independent complete JVM/static/build contract and never aliases candidate creation; instrumentation is added only by explicit `--emulator`. `scripts/candidate` remains the explicit fresh emulator authority and its accepted evidence retains, checksums, and semantically revalidates the merged manifest plus release output metadata.
- Compatibility/rollback: No application behavior, schema, backup, release script, installed package, or user data changes. Roll back router, wrappers, engine, executable fixtures, tests, and docs together while retaining the existing Android target guards; discard generated candidate evidence after rollback. `scripts/device` remains byte-identical to its pre-overhaul version.
- Related: `FB-20260904-004` through `FB-20260904-006`, `DEC-20260904-002`, `IMP-20260904-004`, `VER-20260904-004`, and `VER-20260904-005`.
- Status: Accepted and implemented locally; the compatibility recovery is deterministically verified, while complete real-emulator candidate creation remains intentionally unexecuted in this change.

### DEC-20260904-004 — Autonomous persistence is the VERA default, bounded by real authority

- Context: Intermediate progress and actionable repair cycles can be mistaken for a terminal state, causing an agent to return before the user’s task contract is actually satisfied.
- Decision: Treat status updates, elapsed time, partial results, and optional user context as nonterminal. Continue through every safe, reversible, in-scope investigation, implementation, repair, validation, and review action until acceptance gates pass. If helpful context is optional, state the smallest conservative reversible assumption and proceed.
- Boundary: Stop only when the contract is accepted or no safe in-scope action can make meaningful progress because a genuine external blocker, required input, or protected final boundary remains. A second Sol failure ends that retry loop only; pursue any distinct safe in-scope path before reporting the blocker. Existing approval and non-inference limits remain unchanged.
- Related: `FB-20260904-009`, `IMP-20260904-007`, `VER-20260904-008`.
- Status: Accepted and implemented.

### DEC-20260904-005 — Task capture supplements rather than replaces empty-state orientation

- Context: The Task workspace needs a fast entry point, but no authored Task leaves the page visually silent after that input.
- Decision: Always render the existing destination-specific `EmptyTasks` message when the visible Task collection is empty. Preserve Quick Capture as the first and only creation control on Today/Inbox; do not add a template CTA or alter Task placement behavior.
- Consequences: New users receive the same reassuring orientation as the other primary workspaces while retaining Task's unique rapid-capture flow. No persistence, schema, backup, or data behavior changes.
- Related: `FB-20260904-011`, `FND-20260904-003`.
- Status: Accepted.

### DEC-20260906-001 — No standing Codex orchestration layer

- Context: The user found that VERA-Codex's always-on policy, differentiated role files, and autonomous subagent routing added enough latency to slow ordinary Codex work, superseding the pending request to make that layer more autonomous and Sol/xhigh by default.
- Decision: Whip and both personal Codex homes carry no active VERA instructions, skill, role definitions, or VERA-owned model/reasoning/`[agents]` projection. Codex uses the remaining user-owned configuration without a repository-imposed standing orchestration layer; any future specialized orchestration must be an explicit new choice rather than an automatic continuation of VERA.
- Preservation boundary: Keep the root Whip trust and every unrelated Windows approval, reviewer, sandbox, service-tier, notification, marketplace, plugin, feature, MCP/environment, desktop, Windows, project-trust, and shell-policy setting semantically exact. Remove the Windows trust entry for the deleted VERA checkout. Preserve historical installation and verification records rather than rewriting the past.
- Cleanup order: Remove and verify the active layers first, obtain fresh critical review, commit only the exact Whip uninstall/memory allowlist locally, and only then remove the canonical `/root/repos/vera-codex` repository and retained `/root/.local/state/vera-codex` state. No remote, device, application, credential, plugin, release, or publication action is part of this decision.
- Rollback: Before the final source/state purge, restore the exact recorded preimages and modes from the owner-only transaction backup if any material acceptance gate fails; after accepted cleanup, reinstating VERA requires an explicit new installation decision.
- Related: `FB-20260906-001`, `FB-20260906-002`, `IMP-20260906-001`, `VER-20260906-001`.
- Status: Implemented.

### DEC-20260906-002 — Development feedback is tiered by cost and authority

- Context: The changed-path gate was proportionate in test selection but still compiled the complete Android test source set and ran lint/debug packaging for ordinary production edits. Harness edits were marked candidate-required before their focused fixtures could establish basic correctness. This made the nominal inner loop carry pre-commit and candidate costs.
- Position A: Keep one development command that always compiles Android tests and performs static packaging checks.
- Position B: Separate fast behavioral feedback, affected Android execution, pre-commit readiness, frozen-candidate qualification, and physical release into explicit authority levels.
- Evidence and constraints: JVM profiles already give narrow deterministic domain feedback; Android execution already has exact selector/cache/device guards; candidate qualification already freezes inputs and executes complete fresh coverage. Android-test-only edits still need compilation as their minimum useful headless check. Unknown, build, schema, automation, and harness inputs must never become release-ready from a narrow run.
- Failure modes: Treating a quick pass as release evidence; silently skipping all checks for an Android-test-only edit; rebuilding different release bytes after candidate acceptance; routing a harness change through the entire product profile; weakening explicit emulator and physical-device boundaries.
- Decision: `scripts/check` is the fast default and runs only routed JVM behavior plus lightweight source guards. `--emulator` adds selected Android execution, and `--ready` adds Android-test compilation, lint, and debug packaging. Android-test-only changes compile even in headless quick mode. Known harness files route to deterministic shell fixtures, but remain candidate-required. One explicit fresh `scripts/candidate` qualifies frozen release bytes; separately authorized deployment installs those accepted bytes with `scripts/device release-install` rather than rebuilding them.
- Why this is superior for Whip: The most frequent edit/test loop pays only for relevant behavioral feedback, progressively more expensive work is invoked once at the boundary where it adds confidence, and release authority remains stronger rather than being conflated with development convenience.
- Consequences / reversal conditions: A fast pass is intentionally insufficient for commit or release claims. Use `--ready` before handoff and candidate qualification after source freeze. Restore mandatory inner-loop Android compilation only if deterministic evidence shows JVM compilation plus the explicit readiness boundary permits recurring Android source breakage to escape development.
- Related: `FB-20260906-003`, `DEC-20260904-002`, `DEC-20260904-003`, `IMP-20260906-002`, `VER-20260906-002`.
- Status: Superseded only for personal-phone release qualification by `DEC-20260906-003`; its fast development tiers and Play Store candidate boundary remain active.

### DEC-20260906-003 — Personal-phone releases are fast; complete candidates are store-only

- Context: The owner uses the signed physical-phone installation as a development version. During the 0.3.50 release, the user clarified that complete fresh candidate testing should be paid only for Play Store publication, not every private phone update.
- Position A: Require the store-grade frozen candidate before any signed physical-phone installation.
- Position B: Use affected tests plus signed build/install/smoke for the personal phone, while reserving the complete fresh candidate for store publication.
- Evidence and constraints: Personal-phone deployment is reversible through a later higher signed build and reaches only the explicitly selected owner device. The release guard already rejects emulators, ambiguous/offline targets, uninstalls, downgrades, and untargeted ADB. Play Store publication has a materially broader audience and warrants immutable full-suite evidence.
- Failure modes: Mistaking a private-phone smoke result for complete regression evidence; weakening the physical target guard; clearing local data; publishing an artifact that never passed candidate qualification; rebuilding bytes after store qualification.
- Decision: `scripts/device release-deploy` runs the changed-path fast check, builds signed APK/AAB outputs, installs the APK in place, and launches it. It does not invoke `scripts/check --full` or `scripts/candidate`. A complete fresh `scripts/candidate` is required only when Play Store release is explicitly requested, and store publication must use its exact qualified artifacts.
- Why this is superior for Whip: It matches risk and latency to audience: seconds/minutes for the owner's iterative device loop, complete fresh qualification once for public distribution.
- Consequences / reversal conditions: Phone-release records must state that complete suites were not run. Restore the stronger boundary for private installs only if additional users or irreversible external distribution enter that lane.
- Related: `FB-20260906-004`, `FB-20260906-005`, `DEC-20260906-002`.
- Status: Accepted, implemented, fixture-verified, and exercised by the 0.3.50 physical release.

### DEC-20260906-004 — Habit Today is a daily snapshot, not a field report

- Context: Habit Today exposed correct information through three generic inspector groups, but equal-weight headings and vertically labeled facts forced the user to assemble the day’s meaning before acting.
- Decision: The Today tab owns one bounded daily overview: a plain-language current state is the focal point, the localized date supplies context, and streak/completion/flexible-period values scan together as compact metrics. Tracking-specific input remains inside that overview; availability exceptions use a separate, explained secondary-action row; the stable inspector header, tabs, and docked primary action remain unchanged.
- Rejected alternatives: Decorative dividers around the existing field stacks; a denser generic dashboard shared across unrelated inspectors; promoting Skip Today to a second primary action.
- Constraints and consequences: Preserve every archived, skipped, paused, off-schedule, timer, low-pressure, flexible-schedule, source-owned, and manual-duration behavior. The composition must remain scrollable, dark-mode legible, and actionable at 320 dp and 200% text.
- Related: `FB-20260906-006`, `FND-20260906-001`, `IMP-20260906-005`, `VER-20260906-005`.
- Status: Accepted, implemented, emulator-verified, and released in Whip 0.3.51/code 57; awaiting user validation.

### DEC-20260906-005 — Visual quality is governed by an exact surface catalog

- Context: Whip had broad UI tests and individual screenshots, but no fail-closed answer to which pages, dialogs, menus, and meaningful states had actually been reviewed. A monolithic screenshot journey would also make routine design iteration unnecessarily slow.
- Decision: Keep one source-linked TSV catalog as the coverage contract. Every required row owns a stable surface ID and exact Android test selector; capture must run only on a guarded emulator, export one PNG and one accessibility hierarchy per row, reject missing or uncatalogued evidence, and produce a hash/size manifest. Selectors are split by product family so a changed family can be recaptured independently.
- Evidence and constraints: Production UI discovery is fingerprinted so new composables or modal call sites fail closed until triaged. MediaStore Download assets survive Gradle's post-instrumentation package removal, unlike app-private files. The collector deletes only its exact emulator-owned capture directory and the test clears the matching MediaStore collection before the first capture.
- Failure modes: A stale catalog after UI source growth; screenshots silently lost after test APK uninstall; duplicate IDs; a full visual campaign paid for every small edit; accidental instrumentation or cleanup on the owner's physical phone.
- Consequences / reversal conditions: Baseline and final audits must reach zero pending selectors. Family capture is the normal iteration loop; full catalog capture is reserved for audit milestones. The physical phone remains out of scope until final signed deployment and smoke. Replace TSV or MediaStore only if a successor preserves exact coverage, deterministic export, emulator isolation, and family-level latency.
- Related: `FB-20260906-008`, `IMP-20260906-007`, `IMP-20260906-014`, `IMP-20260906-016`, `VER-20260906-017`, `VER-20260906-018`.
- Status: Accepted, fully implemented, frozen-audit verified, and released in Whip 0.3.52/code 58.

### DEC-20260906-006 — Inspector evidence is contained; dialog footers make one decision

- Context: The exhaustive review found that Habit Today grouped related read-only evidence, while Task and Goal inspectors used the same bare heading primitive for both factual summaries and action sections. It also found an elapsed-reset dialog with one reset command in the dismiss slot and another in the confirm slot.
- Decision: Add a narrowly named `EntityInspectorInformationGroup` backed by Whip's established low-emphasis grouped-information card. Use it for related read-only summary evidence, while action sections, danger zones, charts, and domain-specific interaction roles remain explicit. Dialog footers retain one Cancel and one commit; consequential shortcuts to a different value belong with the input choices in the body.
- Rejected alternatives: Put every `EntityInspectorGroup` in a card, which would nest action cards and erase semantic distinctions; build bespoke Task and Goal summary components, which would perpetuate family drift; keep two commit actions in the Material footer and tune spacing around the resulting wrap.
- Constraints and consequences: Preserve inspector frame/navigation/action behavior and all Task/Goal persistence semantics. Summary cards must remain scrollable and legible in dark mode. The elapsed reset keeps exact instant, daylight-saving overlap/gap, discard, saving, failure, and both reset paths unchanged.
- Related: `FB-20260906-008`, `FND-20260906-003`, `FND-20260906-004`, `IMP-20260906-015`, `VER-20260906-015`, `VER-20260906-017`, `VER-20260906-018`.
- Status: Accepted, implemented, focused-tested, frozen-audit accepted, and released in Whip 0.3.52/code 58.

### DEC-20260906-007 — Elapsed Goals own an ordered multi-unit display definition

- Context: Count Time Since currently stores and renders one enum unit, which cannot express the motivating duration a user wants to see continuously. Adding Months also introduces a choice between fixed-duration approximations and calendar-aware human time.
- Decision: Replace the domain's scalar display choice with a serializable `ElapsedDisplayFormat`: either Automatic or a non-empty canonical descending combination of Years, Months, Weeks, Days, Hours, and Minutes. Automatic retains the familiar single best-fit unit. Authored combinations decompose from the exact start instant to the display instant in Whip's active time zone using calendar years/months/weeks/days followed by exact hours/minutes, and render every selected component—including zero values—in the same order on every Goal surface.
- Persistence and compatibility: Encode the new definition inside the existing Room text column so schema 46 installations upgrade in place without rewriting or clearing owner data. Decode every legacy `Auto` and scalar unit string losslessly; encode new selections with an explicit `Selected:` grammar. Advance the exact-match portable backup contract to version 25 and validate the same codec before restore.
- Interface hierarchy: The editor presents Automatic plus multi-select unit chips, a live configured preview, and explicit copy that the choice stays visible on cards, Home, Insights, and details. Elapsed collection cards move the configured duration out of the one-line metadata slot into an always-visible primary status that may wrap without dropping selected units.
- Rejected alternatives: A single new Months enum value, which leaves the core limitation intact; multiple independent Boolean columns, which would force a Room migration for a display-only evolution; fixed 30/365-day composite approximations, which make calendar-month and anniversary language misleading; hiding the combination in expanded details, which contradicts the motivational always-view requirement.
- Constraints and consequences: The exact start instant remains authoritative; changing display never creates progress history or changes outcome semantics. Terminal snapshots use the stored frozen duration from the Goal's exact start when available. No empty authored selection is valid; removing the final selected unit returns to Automatic.
- Related: `FB-20260906-009`, `FND-20260906-005`.
- Status: Accepted, implemented, focused-tested, visually verified, and released in Whip 0.3.53/code 59.

### DEC-20260906-008 — Visible state acknowledges success; transient bars carry new information

- Context: Whip already defaults routine operation success to Inline, yet explicit overrides and local Snackbar hosts still announce many saves and other immediately visible state changes.
- Decision: Suppress passive success-only action bars app-wide. Show transient app feedback only for failures, post-commit warnings, or a meaningful recovery/continuation control such as Undo, Retry, Restore view, or Edit after quick capture. Keep Android reminders, alarms, foreground/ongoing notifications, and permission surfaces unchanged.
- Rejected alternatives: Removing only “Goal saved,” which leaves the same interruption elsewhere; removing every Snackbar, which would hide failures and time-bounded recovery; replacing bars with decorative checkmarks or animations, which preserves noise without adding information.
- Constraints and consequences: Success states must still update visibly and accessibility semantics must expose the new state. Post-commit warnings remain visible even though the primary action succeeded. Recoverable actions retain exact token ownership and arbitration.
- Related: `FB-20260906-010`, `FND-20260906-008`.
- Status: Accepted, implemented, focused emulator-verified, and released in Whip 0.3.54/code 60; awaiting real-use validation.

### DEC-20260906-009 — Elapsed time is one composed metric, not a headline treatment

- Context: First-class visibility does not require every elapsed component to compete with the Goal name or page heading.
- Decision: Render elapsed displays through one responsive composition across cards, editor preview, Insights, and details. Values use modest medium emphasis; unit words use the surrounding supporting-text role; components wrap atomically with consistent rhythm and one merged spoken label. Remove the redundant expanded-card duration and retain its start context instead.
- Rejected alternatives: A single uniformly bold string; per-unit chips or tiles that turn time into controls; reducing the collection counter to one truncated line; bespoke typography per screen.
- Constraints and consequences: Every authored unit, including zero values, stays visible; canonical order and calendar arithmetic are unchanged. The component must work in dark/light themes, 320 dp, and enlarged text without dangling separators.
- Related: `FB-20260906-010`, `FND-20260906-007`, `DEC-20260906-007`.
- Status: Accepted, implemented, visually verified across the fresh Goals family, and released in Whip 0.3.54/code 60; awaiting real-use validation.

### DEC-20260906-010 — One guarded owner resolves every device artifact path

- Context: The source-linked UI catalog correctly uses a scoped MediaStore Downloads collection so screenshots survive test-package removal, but its shell collector directly owned the matching device path and therefore failed Whip's current complete gate.
- Decision: Keep capture production in the test-owned MediaStore collection, but move device-path resolution, exact collection cleanup, and host pulling into specialized `scripts/device-artifacts` operations. Those operations independently reject physical hardware; `scripts/ui-catalog` retains its earlier instrumentation guard and addresses the artifact owner only through the selected emulator identity.
- Rejected alternatives: Exempt `scripts/ui-catalog` from the source guard, which would create a second device-path authority; move evidence to app-private storage, which is removed before export by the Gradle instrumentation lifecycle; weaken the complete gate because earlier captures happened to succeed.
- Constraints and consequences: Cleanup remains limited to the exact `Download/whip-ui-catalog/` MediaStore collection and directory. General screenshots/UI dumps retain their existing explicitly selected-device behavior. The owner phone remains prohibited for catalog capture, cleanup, pull, and instrumentation.
- Related: `FB-20260906-012`, `FND-20260906-009`, `DEC-20260906-005`.
- Status: Accepted, implemented, fixture-verified, and exercised by the fresh 172-surface audit without any physical-device artifact operation.

### DEC-20260906-011 — 5/3/1 guidance follows dependencies and proves the complete seam

- Context: The targeted Gym audit found healthy local behavior but a misleading first-run blocker, ambiguous cycle-decision confirmation, and no single real-app proof across 5/3/1 authoring, persistence, program launch, and active execution.
- Decision: 5/3/1 setup blockers must name the first action the current state actually permits, and a disabled Build Program action must expose that same reason. Cycle review must restate the selected action, audit meaning, and resulting Training Max in text. The fast Gym/5/3/1 acceptance lane must include one Activity-to-database-to-Activity journey and catalog states that distinguish blocked setup, ready setup, real multi-phase structure, and active program execution.
- Product boundary: Preserve 5/3/1 terminology, explicit Training Max authorship, optional Joker semantics, supplemental/assistance structure, progression eligibility, immutable History, and existing Routine data. This is guidance, presentation, and evidence work; it does not silently change programming choices or user data.
- QA/release boundary: Iterate with routed JVM/UI tests and the Gym-only exact catalog on a disposable emulator. A clean pushed higher-version source may use the guarded fast owner-phone release lane; Play Store candidate qualification and phone instrumentation remain out of scope.
- Related: `FB-20260906-013`, `FND-20260906-016`, `FND-20260906-017`, `FND-20260906-018`, `DEC-20260906-003`, `DEC-20260906-005`.
- Status: Implemented and verified in `IMP-20260906-024` and `VER-20260906-025`.

### DEC-20260907-001 — Canonical 5/3/1 stays fixed; adaptive AMRAP guidance is bounded and explicit

- Context: Jim Wendler's published 5/3/1 guidance uses deliberately submaximal Training Maxes and fixed cycle increases. Strong AMRAPs primarily validate that the Training Max is appropriate; they do not make a larger increase canonical. Whip nevertheless supports an opt-in advisory mode, and repeated objective results can justify a cautious non-standard alternative without forcing users to estimate RPE/RIR.
- Decision: Preserve the configured standard increase as the canonical/default choice in every case. Label the opt-in path Adaptive review and explicitly identify it as non-standard. A rep-only higher alternative requires at least two qualifying PR/AMRAP sets from distinct successful required-work exposures; every set must meet its prescription, use at least 85% of its snapshotted Training Max, the evidence set must include at least one set at 90% or more, and each conservative load-adjusted estimated 1RM must support the proposed next Training Max. Estimate conservatively from actual load and capped repetitions, require the repeated estimates to agree within a narrow tolerance, and retain incomplete/deleted/ambiguous/missed/failed required work as a higher-priority veto. Cap this objective-only alternative at 1.25 times the standard increase.
- Corroborated tier: Permit at most 1.5 times the standard increase only after the repeated objective gate also has credible favorable RPE/RIR on the qualifying performances or a genuinely strong Joker that exceeds its prescribed result at or above current Training Max without grinder evidence. A single AMRAP, Joker, or Training Max test is never sufficient. A failed Training Max test retains decrease-review priority and a passing test alone does not accelerate progression.
- Interaction contract: Standard remains selected and described as recommended. Adaptive results are advisory, explain why they qualified or did not, and never mutate a Training Max until the user explicitly confirms a cycle-review choice. Cycle audit records continue to distinguish Standard, Suggestion, Hold, Ignore, and Custom.
- Rejected alternatives: Make large AMRAPs automatically increase the Training Max; require RPE/RIR for every higher suggestion; let Joker completion alone corroborate a larger increase; use the most optimistic estimated-1RM formula or uncapped high-rep result; present adaptive progression as though it were canonical 5/3/1.
- Constraints and consequences: The engine remains deterministic from snapshotted workout evidence, does not infer recovery/readiness, and cannot eliminate judgment. Conservative false negatives are preferred to compounding an aggressive Training Max. Existing routine settings and history remain compatible.
- Related: `FB-20260907-001`, `FND-20260907-001`, `FND-20260907-002`, `FND-20260907-003`, `IMP-20260907-001`, `IMP-20260907-002`, `VER-20260907-001`, `VER-20260907-002`.
- Status: Accepted, implemented, code-reviewed, emulator-verified, and released in Whip 0.3.57/code 63.

### DEC-20260907-002 — High-emphasis UI must describe true state and available action

- Context: The fresh whole-product catalog is broadly coherent, but its remaining friction clusters around hierarchy that repeats or fabricates identity, controls that stay visible after becoming unavailable, and a recovery action styled too weakly for warning copy that names it. The Area scope popup also demonstrates that spatial truth depends on an anchor whose bounds match its visible trigger.
- Decision: Each surface has one owner for primary identity and summary. New entities identify their parent/context until the user authors an identity; archived or otherwise unavailable actions with no direct repair path are omitted rather than dimmed; warning-named recovery actions use the established full-width secondary control; popup anchors explicitly wrap the visible trigger. Preserve existing domain operations, data, persistence, and intentional dense workflows.
- Rejected alternatives: Cosmetic color/spacing changes without fixing information ownership; disabling every unavailable action in place; inventing a placeholder Entry name; repeating selected Area identity in both frame and content; making diagnostic refresh a new primary action; accepting a detached catalog popup as a harmless test artifact.
- Constraints and consequences: Active Track add, archived restoration, Track-entry validation/duplicate detection, Area operations, notification diagnostics, adaptive layouts, and popup menu contents remain unchanged. Changes require focused semantics/interaction tests, affected-family recaptures, and a fresh exact full catalog before release.
- Related: `FB-20260907-002`, `FND-20260907-004`, `FND-20260907-005`, `FND-20260907-006`, `FND-20260907-007`, `FND-20260907-008`, `DEC-20260906-005`.
- Status: Accepted, implemented in `bac0dec`, and focused emulator-verified in `VER-20260907-003`.

### DEC-20260907-003 — Android QA may use one explicit matching secondary emulator

- Context: Whip's centralized Android runner already divides instrumentation into independent bounded processes, but one serial and AGP's shared connected-test output directories kept them sequential. Launching separate commands manually would race cleanup, XML, coverage, cache, and candidate evidence and could accidentally select the connected owner phone.
- Decision: Preserve `ANDROID_SERIAL` as the required primary and add only `WHIP_ANDROID_SECONDARY_SERIAL` as an explicit opt-in secondary. Validate both through the emulator-only guard, require API 34+ plus identical API/system-image fingerprint/ABI, and cap the set at two. Build shared APK inputs once; run the graphics batch first on primary, balance ordinary batches over isolated worker output slots, and run the destructive reset last on primary. Aggregate only complete per-batch rows in declared order. Candidate v2 binds the count and ordered set hash; historical v1 evidence remains verifiable.
- Rejected alternatives: Automatic ADB discovery, because a physical phone is connected and target choice must remain explicit; arbitrary worker counts, because current evidence, host capacity, and scheduling only justify two; two independent runner invocations, because shared AGP paths and source-drift authority make their evidence unsafe; parallel graphics/reset execution, because those boundaries intentionally control global rendering and data state.
- Constraints and consequences: One-emulator commands stay compatible. A mismatched, duplicate, physical, offline, unauthorized, malformed, or third target fails before instrumentation. Every worker owns its AGP result/report/coverage tree, cache publication is atomic, failed-worker logs remain visible, and UI-catalog artifacts merge only after duplicate-name rejection. This changes development QA latency and evidence, not app behavior, schema, backups, release identity, or phone deployment policy.
- Related: `FB-20260907-003`, `FND-20260907-013`, `DEC-20260904-003`, `DEC-20260906-003`, `DEC-20260906-005`, `IMP-20260907-008`, `VER-20260907-009`.
- Status: Verified.

### DEC-20260907-004 — Rich persistent status belongs outside the card action lane

- Context: The same multi-unit Count Time Since value appeared side by side as a punctuated scalar support label and a composed card metric. The card metric also shared the weighted title column with identity, disclosure, and Reset, creating avoidable wrapping and disappearing when details expanded.
- Decision: Preserve the shared productivity card shell, but add an optional persistent summary slot below its identity/action row. Rich status uses that full-width slot and remains present in collapsed, expanded, and reorder states; short scalar summaries retain the compact inline slot. Navigation rows likewise accept either scalar text or structured supporting content. Both the fold support pane and Goal cards render elapsed time through the existing `ElapsedGoalMetric`, including its canonical value/unit hierarchy, responsive atomic wrapping, and merged spoken label.
- Rejected alternatives: Replace all productivity cards for one timer case; shrink or truncate selected units; remove or hide Reset; tune one screenshot with fixed widths; convert the composed metric back to a separator-delimited string; duplicate a fold-specific timer renderer.
- Constraints and consequences: Elapsed start instants, authored unit combinations, calendar arithmetic, terminal snapshots, reset semantics, Goal persistence, schema 46, data epoch 6, and backup version 25 remain unchanged. The new card/navigation slots are reusable presentation boundaries and do not alter nonelapsed call sites unless explicitly adopted.
- Related: `FB-20260907-004`, `FND-20260907-014`, `DEC-20260906-007`, `DEC-20260906-009`, `IMP-20260907-009`, `VER-20260907-010`.
- Status: Verified.

### DEC-20260907-005 — Collection cards use one title-column reading grid

- Context: The shared medium card shell is appropriately dense, but Tasks, Habits, Goals, and Tracks currently disagree about where equivalent status begins, which typography represents support, and whether disclosure precedes the frequent action. The elapsed-Goal full-width slot solved action-lane wrapping but exposed the missing cross-domain grid.
- Decision: Retain the shared medium card surface (now `WhipItemCard`) and define one internal grammar: 36 dp identity, 8 dp gap, `titleMedium` semibold title, concise support on the title column, disclosure before the trailing primary action, then expanded identity-owned context on the same logical title edge. A reusable 44 dp start gutter aligns persistent rich status and supporting context without constraining their trailing width. Equivalent secondary prose uses a shared small supporting-text role; metadata chips may retain categorical containment but inherit the same anchor and emphasis. Full-width progress, charts, and interactive evidence remain deliberate lower-level exceptions.
- Track consequence: Ordinary Track summaries adopt `ProductivityItemHeader`; selection/reorder and Entry variants keep their necessary interaction structure but use the same title weight, support type, 8 dp identity gap, and title-column alignment. Task schedule metadata becomes shared persistent header content instead of an independent hard-coded column.
- Rejected alternatives: Replace the card shell or enlarge every card, which would sacrifice useful density without fixing hierarchy; force every domain detail into identical visuals, which would erase meaningful chips, metrics, progress, and actions; tune each screenshot with local padding; keep rich elapsed status aligned to the emoji solely because it has more width.
- Constraints and consequences: Preserve all open/edit/complete/log/reset/add/selection/reorder behavior, authored data, schema 46, data epoch 6, and backup version 25. Logical `start` alignment must mirror in RTL, and large text may wrap vertically without truncating controls or reverting to another anchor. Acceptance requires explicit geometry/semantics regressions plus fresh affected-family pixels on disposable emulators.
- Related: `FB-20260907-006`, `FND-20260907-015`, `DEC-20260903-014`, `DEC-20260907-004`.
- Status: Verified; implemented in `IMP-20260907-011` and accepted in `VER-20260907-012`.

### DEC-20260907-006 — Global recovery actions have one visible owner

- Context: Routine cards expose distinct next-day and out-of-order day actions, but an active session currently rewrites every one into “Open Active Workout.” The same global outcome therefore occupies several local action slots at once.
- Position A: Keep every slot and vary styling or wording so the repetition looks less obvious.
- Position B: Disable every day action in place and rely only on the Gym Workout destination for recovery.
- Evidence and constraints: Users need an explicit path back to interrupted work, especially after navigating into Library. The active source Routine is the most stable contextual owner when visible; focused/archived/ad-hoc states can hide that source. Equipment repair on another Routine remains a genuine distinct action, while editing the source Routine is already locked during its active workout.
- Failure modes: Position A preserves duplicate outcomes and accessibility noise. Position B weakens recovery discoverability and leaves a page full of unavailable controls. A page-only action can scroll away immediately after starting from a card; a source-card-only action disappears when the source is filtered or an ad-hoc workout is active.
- Decision: Render exactly one filled Open Active Workout action on the visible source Routine; if no visible Routine owns the active session, render one page-level fallback. Do not rewrite next/day start controls into recovery aliases. Hide unavailable start controls while any session is active, but retain a day-specific Resolve Equipment action for a different editable Routine where that distinct work is still valid.
- Why this is superior for Whip: Recovery remains explicit and close to its context without multiplying commands, day actions keep truthful meanings, active-source editing constraints stay coherent, and filtered/ad-hoc cases retain a safe fallback.
- Consequences / reversal conditions: Starting, program advancement, active-session identity, equipment repair, Room schema 46, data epoch 6, and backup version 25 do not change. Revisit only if Gym adopts a persistent app-level active-workout affordance that remains visible across Library scrolling and can replace both owners.
- Related: `FB-20260907-007`, `FND-20260907-016`, `DEC-20260906-006`.
- Status: Verified; implemented in `IMP-20260907-013` and accepted in `VER-20260907-014`.

### DEC-20260907-007 — Workout sets use one information-preserving execution density

- Context: Gym alone still offers compact and comfortable workout-set rows after Whip removed user-selectable collection density elsewhere. Compact mode improves scan speed by tightening the exercise card and set gaps, but hides classification, planning, and RPE context; comfortable mode retains the context but uses a looser 14 dp/8 dp rhythm than the current shared card grammar.
- Decision: Remove the Gym density preference and render every workout exercise with one balanced execution grammar: medium app card shape and surface role, 12 dp horizontal/10 dp vertical inset, 6 dp direct-content and set rhythm, 48 dp controls, and always-visible concise classification/planned/effort and target support. Keep the active-set composer visually distinct because it is an input workspace, not another passive collection row; keep workout-specific grouping, reorder, completion, optional-work, and progression semantics intact.
- Persistence boundary: Remove `gymCompactSetRows` from `AppSettings`, SharedPreferences writes/reads, backup export/import, Settings, documentation, cause/effect inventory, and composable/test call sites. Advance the exact-match portable backup format from 25 to 26 so the changed JSON contract has an unambiguous boundary. Do not change Room schema 46 or data epoch 6; an obsolete local preference byte is ignored and no owner data is rewritten or cleared.
- Rejected alternatives: Preserve both modes under a renamed setting; silently force the current comfortable branch while retaining dead state; make compact mode the default and discard lifting context; wrap passive sets in a second heavy nested-card system; remove domain-specific active-composer emphasis merely to resemble productivity cards.
- Why this is superior for Whip: It combines space efficiency with the information a lifter needs to judge a set, removes a product-wide visual fork and its long-lived state burden, and aligns ordinary geometry with Whip without flattening workout execution into a generic task row.
- Consequences / reversal conditions: Existing workouts, sets, calculations, Routine/5/3/1 progression, and local installation data are unchanged. Older backup formats remain intentionally rejected by the exact-match restore gate. Reintroduce a density preference only if measured use demonstrates a distinct workflow that responsive layout and the balanced row cannot serve.
- Related: `FB-20260907-008`, `FND-20260907-017`, `DEC-20260903-014`, `DEC-20260903-015`.
- Status: Partially superseded by `DEC-20260907-012` only for the rejection of light nested passive-Set surfaces; the verified one-density, persistence, preserved-information, progression, and distinct-composer decisions remain active.

### DEC-20260907-008 — Repeated interaction labels require either one owner or explicit scope

- Context: Repeated wording is appropriate on entity-owned card actions such as Log or Reset, where the adjacent item identity supplies scope. It is harmful when two simultaneously visible navigation controls have the same label but different scopes, or when a frequent control's reliable touch geometry depends on its text wrapping.
- Decision: Keep legitimate per-entity repeated actions, but name nested navigation when its scope would otherwise be ambiguous; the selected-Track destination becomes “Track Insights” while the workspace aggregate remains “Insights.” Independently, every rendering of the clickable next-set jump receives a 48 dp minimum height rather than relying on prescription length to enlarge it.
- Rejected alternatives: Remove one Track Insights destination, because aggregate and selected-Track analysis are both useful; rename the global destination, which would make all top-level areas less consistent; accept position-only scope; add padding that makes wrapped 5/3/1 banners unnecessarily taller.
- Constraints and consequences: Track routes, selected destinations, filters, analytics, workout ordering, and jump behavior do not change. Tests must lock both explicit Track labels and the compact next-set target floor.
- Related: `FB-20260907-008`, `FND-20260907-018`, `FND-20260907-019`.
- Status: Verified; implemented in `IMP-20260907-014` and accepted in `VER-20260907-015`.

### DEC-20260907-009 — Uniform collection geometry is summary-state uniformity, not fixed-height content

- Later refinement (2026-09-10): DEC-20260910-005 also supersedes the three-part elapsed overview rule. The shared full-width summary now shows the complete authored unit combination in its existing restrained text role. The original short-card tradeoff below remains historical context.
- Context: Owner feedback after the title-grid work shows that adjacent Home cards still feel different because Tasks and elapsed Goals add a second persistent collapsed band. Literal fixed heights across simple tasks, multi-phase Routines, charts, editors, and workout composers would either truncate information or create large artificial voids.
- Decision: Equivalent collapsed collection summaries use one named geometry contract: 12 dp horizontal inset, 10 dp vertical inset, 6 dp internal rhythm, `titleMedium` semibold identity, concise small status in the title column, stable 48 dp controls, and a 68 dp ordinary summary height. Content grows only for wrapping, enlarged text, or genuinely expanded/domain-rich evidence. Task metadata becomes a concise scalar overview while collapsed and remains complete when expanded. Elapsed Goals show at most three largest meaningful configured parts in the collapsed visual overview while preserving every selected part in the expanded card/detail surfaces and full accessibility description.
- Cross-product consequence: Home status cards delegate to the same navigation-row grammar. Gym Exercise and Machine libraries, Routine browse headers, and Routine Builder placements use the same insets and title/support hierarchy, while active set composers, charts, notices, grouped Settings blocks, and program evidence remain purpose-specific surfaces.
- Supersession: This narrows `DEC-20260907-004`: full-width elapsed metrics remain correct for expanded/details/support panes, but are no longer a persistent collapsed-card band because current owner feedback prioritizes equal summary geometry. It extends, rather than reverses, the title-column principle in `DEC-20260907-005`.
- Rejected alternatives: Force every card in every state to a fixed pixel height; preserve chip/metric bands and merely adjust padding; hide authored elapsed units from accessibility/details; flatten active workout inputs or 5/3/1 program evidence into generic list rows.
- Constraints and consequences: Data, calculations, routes, completion/log/reset/start behavior, Room schema 46, data epoch 6, and backup format 26 remain unchanged. Acceptance requires exact collapsed-height/alignment tests, large-text growth checks, representative Gym/Routine visuals, and a fresh Home catalog state.
- Related: `FB-20260907-012`, `FND-20260907-021`, `DEC-20260907-004`, `DEC-20260907-005`, `DEC-20260907-007`.
- Status: Superseded.

### DEC-20260907-010 — Structural program choices must expose comparable consequences before selection

- Context: 5/3/1 schedule layout changes the number and composition of training days, forced versus selected Supplemental work, assistance guidance, and whether arbitrary Exercises are allowed. The prior chip row communicated only names.
- Decision: Render each available 5/3/1 layout as a selected choice card with concise, parallel supporting copy derived from the exact generator contract. Keep selected/state semantics and stable tags. When a long-term Leader/Anchor plan excludes the standalone Beginners layout, state that it is available under Classic cycle instead of silently hiding the reason.
- Evidence and constraints: Four-Day generates one Squat, Bench Press, Deadlift, and Overhead Press day; Beginners generates three full-body days, forces FSL 5 × 5, and uses 50-rep automatic assistance drafts with 50–100-rep guidance; Custom creates one day per ordered compatible Exercise and does not require the standard four. Program generation, saved data, percentages, progression, history, schema 46, data epoch 6, and backup 26 must not change.
- Failure modes: Overlong tutorial prose would bury the next action; a selected-only description would still prevent comparison; disabling an unsupported choice without explanation would look broken; copy that diverges from generation would be worse than no copy.
- Why this is superior for Whip: The component weight now matches decision weight, all options are comparable at a glance, and users can choose confidently without external 5/3/1 knowledge while the editor remains scrollable and fully customizable.
- Consequences / reversal conditions: The setup becomes modestly taller but more legible and interruption-resilient. Revisit only if an equally discoverable compact comparison pattern is proven on 320 dp/200% text.
- Related: `FB-20260907-013`, `FB-20260907-014`, `FND-20260907-022`.
- Status: Implemented and verified in `IMP-20260907-019` / `VER-20260907-021`.

### DEC-20260907-011 — Productivity cards use a centered header row and full-width information row

- Later refinement (2026-09-10): DEC-20260910-004 retains this header/full-width information contract but replaces the old expanded Edit placement with one footer after the complete specialized body. Its builder removes the split information ownership and duplicated timer status. The historical reasoning below remains valid.
- Context: The shared 68 dp collapsed-card target made adjacent cards uniform, but achieved that by squeezing status into the title/action lane and forcing one-line ellipsis. Owner use confirms that complete scheduling and repeat context is more valuable than preserving the shortest possible row.
- Position A: Keep status under the title and tune abbreviations, font size, or action widths to preserve the 68 dp shape.
- Position B: Separate identity/title/actions from information. Keep the title start-aligned and vertically centered in a 48 dp header row, then render complete supporting information below from the emoji's leading edge, allowing content-driven wrapping and modest height growth.
- Evidence and constraints: The four equivalent Task/Habit/Goal/Track cards already share `ProductivityItemHeader`, 10 dp top/bottom card inset, 48 dp controls, and stable action order. Metadata values are bounded but often exceed the remaining title-column width. Expanded cards must not repeat the same Task metadata merely to reveal its clipped suffix.
- Failure modes: Horizontally centered titles would break scan alignment; retaining `maxLines=1` would only move the ellipsis; padding the information to the title edge would surrender 44 dp; always showing both summary and expanded detail could duplicate facts; forcing equal fixed height would clip wrapping or create voids.
- Decision: Adopt Position B for equivalent productivity collection cards. Collapsed summary and any persistent status use a full-width information lane below the header. Expanded detail also starts at the identity edge; a non-persistent collapsed summary is replaced by richer expanded information, while persistent content remains visible exactly once. Card height is content-driven, with equal 10 dp outer vertical padding and the existing 48 dp action floor.
- Why this is superior for Whip: It preserves the established card shell and action consistency while making the reading order literal: who/what and actions first, complete state second, deeper detail after expansion. Users gain information without smaller type, unexplained abbreviations, or routine extra taps.
- Consequences / reversal conditions: This supersedes only `DEC-20260907-009`'s requirement that ordinary productivity status occupy the title column and target 68 dp. The shared 68 dp token remains valid for one-band navigation/status cards. Data, actions, accessibility meaning, persistence, and specialized rich surfaces do not change. Revisit only if measured scrolling cost outweighs verified comprehension at compact width and large text.
- Related: `FB-20260907-015`, `FND-20260907-025`, `DEC-20260907-005`, `DEC-20260907-009`.
- Status: Verified.

### DEC-20260907-012 — Workout Sets use light nested surfaces and full-width evidence rows

- Context: The one-density decision in `DEC-20260907-007` removed a harmful presentation fork and preserved lifting context, but its direct passive rows now place too much information and two controls into one visual stream. Owner real-use evidence specifically authorizes nested Set cards when they improve long-term readability.
- Decision: Preserve one density and the outer Exercise `WhipItemCard`, then render each passive or removed Set as a subordinate `surfaceContainerHigh` surface using the small Whip shape, shared 12 dp horizontal/10 dp vertical content inset and 6 dp content rhythm, and an 8 dp sibling gap. Order content as optional programmed section + Set number, full-width entered/planned load and reps, classification + performed/planned state + effort, then full-width target. Keep 48 dp menu/completion targets in the identity row and the entire passive surface as the exact Set editor target. Keep the active composer on its existing primary-tinted surface, but align its identity and target ordering with the passive cards.
- Responsive/accessibility boundary: Text grows and wraps rather than truncating; the load/reps and target rows are not width-constrained by actions. General/ad-hoc Sets omit a meaningless section prefix. Reorder, incomplete, removed/undo, optional Joker, Main/Supplemental/Assistance, exact edit/menu/completion, and completion-collapse behavior retain their current semantics.
- Rejected alternatives: Reintroduce compact/comfortable modes; place every detail in one heading; add outlines/elevation that create a heavy card stack; flatten the focused input composer into a passive Set; or restyle Routine editors and already-bounded History Sets merely for literal component reuse.
- Why this is superior for Whip: It makes Set boundaries and priority visible at a glance without hiding training evidence, separates reading from actions, remains recognizably part of the Exercise, and scales to longer prescriptions and larger text without reviving persistent density state.
- Supersession: This supersedes only `DEC-20260907-007`'s rejected-alternative position against nested passive Set surfaces. Its one-density, preserved-information, persistence, progression, and active-composer decisions remain active.
- Related: `FB-20260907-016`, `FND-20260907-026`, `DEC-20260907-007`, `IMP-20260907-025`, `VER-20260907-027`.
- Status: Verified; implemented in `IMP-20260907-025` and accepted in `VER-20260907-027`.

### DEC-20260907-013 — Active and historical Sets share one core information grammar

- Context: Active execution and completed History show the same persisted Set at different lifecycle stages. The active surface needs edit, menu, completion, and quick-entry controls; History is read-only and additionally exposes rest, tempo, unilateral, note, equipment, and program-snapshot evidence.
- Position A: Preserve the existing History layout because it is already bounded, accepting its separate surface tokens and information order as a contextual difference.
- Position B: Share the passive Set surface and core identity → values → status/effort → target grammar across Workout and History, then append genuinely historical evidence without importing active controls.
- Evidence and constraints: `FND-20260907-027` confirms that History's differences are implementation chronology rather than a distinct user task. The active hierarchy already passes narrow/large-text, exact-edit, touch-target, 5/3/1, and general-workout coverage. Historical truth, removal/substitution outcomes, prescription snapshots, notes, and completed values must remain unchanged.
- Failure modes: Flattening History into an editable card would imply unsupported actions; dropping “Performed” or removal outcomes would blur historical truth; putting every history field at primary emphasis would recreate the original cramped scan; adding another density or appearance preference would revive a rejected design fork.
- Decision: Adopt Position B. Use one reusable `surfaceContainerHigh` small Set surface with Whip's 12×10 dp inset and 6 dp rhythm for passive active Sets, removed Sets, and historical Sets. History uses program section + Set number as identity, performed values as the primary line, classification + performed/removal + RPE/RIR as status, then target; rest/tempo/unilateral and note remain subordinate history-only rows. The active composer stays a distinct tinted input workspace.
- Why this is superior for Whip: The Set remains immediately recognizable across execution and review while each destination still communicates its real capabilities. Shared implementation tokens also make future spacing or contrast changes propagate instead of drifting again.
- Consequences / reversal conditions: Presentation, semantics, and test/catalog evidence change; persistence, workout calculations, order, progression, and saved history do not. Revisit only if real History use shows that a different information priority materially improves comparison without harming cross-destination recognition.
- Related: `FB-20260907-017`, `FND-20260907-027`, `DEC-20260907-012`.
- Status: Verified; implemented in `IMP-20260907-026` and accepted in `VER-20260907-028`.

### DEC-20260908-001 — Archived Track mutation capability and discovery follow one state owner

- Context: `FND-20260908-002` found that archived Entry rows and search routing only partially apply the read-only-history policy. The initial duplicate-render signal in `FND-20260908-001` did not survive authoritative source reconciliation and requires no production change.
- Position A: Patch each visible symptom independently—leave disabled Edit visible, retain Delete, and keep archived search results on the editor's blocking page.
- Position B: Enforce one archived-state rule across capability, routing, and evidence: one complete writable action cluster only when the Track is writable, and one read-only exact-record route for archived Entry discovery.
- Evidence and constraints: Archived Tracks deliberately preserve reviewable history while requiring restoration before mutation. Search must land on the selected fact. Active Entry editing/deletion, exact repository boundaries, archive identity, and completed history remain unchanged.
- Failure modes: Hiding only Edit leaves destructive mutation available; disabling More leaves a dead high-salience control; routing every archived result only to the Track list loses exact Entry context; making archived history writable contradicts the established lifecycle boundary.
- Decision: Adopt Position B. Omit the entire Entry mutation cluster for archived Tracks; route an archived Entry request to its read-only inspector within Archived detail; and require a populated archived-detail catalog regression with one compact detail-owner assertion.
- Why this is superior for Whip: The same state truth controls pixels, semantics, navigation, and mutation availability, eliminating partial policies while preserving low-friction access to historical evidence.
- Consequences / reversal conditions: Active Track behavior is unchanged. Archived Entry changes require explicit Track restoration. Reconsider only if Whip introduces a separately designed historical-correction mode with explicit semantics and exact repository policy.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-001`, `FND-20260908-002`.
- Status: Accepted.

### DEC-20260908-002 — Local Entry search remains distinct from global Track discovery

- Context: `FND-20260908-003` found that per-Track query, FTS, fallback, sorting, paging, and no-match logic are present but unreachable. Whip's existing `DEC-20260902-006` intentionally keeps local collection filtering separate from global Tracks & Entries discovery.
- Options considered: Delete the dormant local query path and require global search; add another always-visible field competing with the shell search; or expose a compact scoped action beside Entry Filter and Sort that expands an in-page field only when requested.
- Decision: Use the scoped expandable action. Its accessible label names the current Track, its field names Entries, closing it clears the query, and existing filter/sort/no-match behavior remains one coherent local collection state.
- Why this is superior for Whip: Global search answers “where is this fact across Tracks?” while local search answers “which Entries in the Track I am reviewing match?” The compact action preserves page breathing room until needed and reuses the already bounded repository path.
- Consequences / reversal conditions: The catalog gains one active local-search surface and real repository proof. Reconsider only if Whip replaces search/filter/sort with one unified per-collection query model that preserves both scopes and exact result routing.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-003`, `DEC-20260902-006`.
- Status: Accepted.

### DEC-20260908-003 — Task query belongs to the current-list filter recipe

- Context: `FND-20260908-004` found a complete persisted Task-query path that cannot be authored. Tasks already consolidate sort, grouping, criteria, and saved recipes in one dialog, while the shell search deliberately owns broad Task-and-step discovery.
- Options considered: Delete the dormant query from persistence; add an always-visible page field beside global search and the existing filter action; or expose the query as the first criterion in the existing Task filter dialog.
- Decision: Put one full-width `Search Current List` field at the top of `Sort, Group & Filter Tasks`. It searches title, notes, and step text, offers immediate clearing, participates in the existing active-filter row and saved-filter model, and leaves global search unchanged.
- Why this is superior for Whip: The placement makes the scope and persistence model self-evident, avoids competing search controls in the page chrome, and completes rather than fragments the established Task-filter workflow.
- Consequences / reversal conditions: Every Task destination gains local narrowing through the same dialog. Manual reorder remains unavailable while a query is active. Reconsider only if Whip replaces dialog filters and shell search with a unified query model that preserves current-list scope, cross-state discovery, and saved recipes.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-004`, `DEC-20260902-006`.
- Status: Accepted.

### DEC-20260908-004 — Catalog acceptance requires distinct pixels and current accessibility state

- Context: `FND-20260908-005` found that the catalog can count a stale state twice, capture an unopened menu, and pair current screenshots with a preceding page's cached accessibility hierarchy.
- Options considered: Rely on manual gallery review; add one-off waits to the two exposed tests; or strengthen both state owners and the shared collector so false evidence fails closed.
- Decision: Use exact semantic assertions at transition-heavy owners, remove catalog aliases that represent no distinct current surface, force a final accessibility window-content event plus API-supported cache eviction and connected-tree refresh immediately before hierarchy export, and reject any completed capture whose PNG hash is shared by two declared surfaces.
- Why this is superior for Whip: State-specific assertions prove intent, the collector fixes the cross-cutting cache boundary, and hash uniqueness prevents future coverage inflation without judging subjective similarity. Exact pixel identity cannot represent two distinct user-visible states.
- Consequences / reversal conditions: Purely semantic variants must also provide a visible state if they are to count as separate visual catalog surfaces; nonvisual accessibility contracts remain ordinary interaction tests. Reconsider only if the catalog explicitly models a separate nonvisual evidence class with its own acceptance rules.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-005`, `DEC-20260906-005`.
- Status: Accepted.

### DEC-20260908-005 — Color identity and exact value separate only where the layout supports both

- Context: `FND-20260908-006` found that the shared color picker repeats custom hex text because its two-line preview consumes a compact single-line formatter.
- Position A: Change `colorDisplayName` globally to return only `Custom`, removing the duplicate in the dialog but also stripping exact identity from collapsed fields and accessibility descriptions.
- Position B: Preserve the compact combined formatter for one-slot summaries and spoken identity, while giving the dialog's two-line preview a role-specific kind label above one exact value/explanation line.
- Evidence and constraints: Areas, Habits, Goals, and Gym share the picker; preset names and exact RGB values are both useful; the existing field and `Color preview` semantics identify custom colors precisely; no persistence or color math changes are needed.
- Failure modes: Removing the field's hex makes different custom colors indistinguishable by text; retaining the combined title repeats information; hiding the exact value behind Custom Color harms review; changing preview semantics would weaken existing accessibility identification.
- Decision: Adopt Position B. The visual preview renders `Default`/preset name/`Custom` as identity and app-default guidance or one hex value as support. Existing combined field and color-swatch semantics remain exact. Stable preview tags expose the visible hierarchy to regression tests.
- Why this is superior for Whip: Each context carries the same information once at the hierarchy its layout can support, improving scanability without trading away precision or accessibility.
- Consequences / reversal conditions: Presentation and test semantics change only inside the two-line preview; saved values, preset selection, exact editing, and caller behavior remain unchanged. Revisit only if the color system gains another identity dimension that needs a richer preview model.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-006`.
- Status: Verified in `IMP-20260908-005` / `VER-20260908-005`.

### DEC-20260908-006 — Settled empty search states name the controls that recover them

- Context: `FND-20260908-007` found that Unified Search reports a definitive zero result without connecting the user to the still-visible query and Filters recovery controls.
- Position A: Keep `No matching items` as a minimal status and rely on control proximity.
- Position B: Use one concise, actionable sentence for the settled empty state while retaining separate incomplete/loading language.
- Evidence and constraints: Query editing and Filters are always available in compact and wide workspaces; filters may be collapsed; partial source failures cannot truthfully claim an exhaustive no-match result; copy must remain readable at compact width and 200% text.
- Failure modes: Generic `Try again` copy does not identify what changes the result; implying a load failure is inaccurate; tutorial-length guidance overwhelms the empty surface; reusing definitive copy during partial loading hides uncertainty.
- Decision: Adopt Position B: `No matching items. Try another search or adjust Filters.` Use it only after selected sources have settled, and retain the existing partial-data message for incomplete results.
- Why this is superior for Whip: The message turns a dead result into two obvious next actions without adding controls, changing search logic, or obscuring data completeness.
- Consequences / reversal conditions: Only settled-empty presentation and exact copy assertions change. Revisit if search and filtering become one combined control with a different recovery vocabulary.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-007`.
- Status: Verified in `IMP-20260908-005` / `VER-20260908-005`.

### DEC-20260908-007 — Backup import choices belong in the explanatory body, not a three-action footer

- Context: `FND-20260908-008` found that the backup preview compresses three consequential actions into AlertDialog footer space and separates each action from the text that explains it.
- Position A: Keep all actions in the footer and reduce type, wording, or spacing until they fit.
- Position B: Leave only Cancel in the footer and pair each restore mode with its own concise description and full-width action in the scrollable body.
- Evidence and constraints: Merge is the safer additive default; Replace is destructive, snapshots first, and already has a separate final confirmation; compact width and 200% text must remain usable; compatibility may disable both restore actions; persistence and recovery behavior must not change.
- Failure modes: Smaller controls reduce readability and touch clarity; stacked footer buttons still detach explanation from choice; making Replace visually primary overstates it; removing the final confirmation weakens recovery safety.
- Decision: Adopt Position B. Present a compact backup summary followed by `Merge New Data` as the filled primary action and `Replace Everything` as an error-toned outlined action, each directly beneath its consequence copy. Keep Cancel as the sole footer action and retain the existing final destructive confirmation.
- Why this is superior for Whip: The layout scales vertically, connects each decision to its outcome, makes the safer option easiest to recognize, and preserves an explicit but subordinate destructive route.
- Consequences / reversal conditions: Visual hierarchy, tags, and responsive tests change; restore transactions, compatibility checks, duplicate handling, snapshots, and confirmation ownership remain unchanged. Revisit only if restore becomes a guided multi-page workflow with comparable safety and recovery clarity.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-008`.
- Status: Verified in `IMP-20260908-006` / `VER-20260908-006`.

### DEC-20260908-008 — Settings page titles are not repeated as immediate section headings

- Context: `FND-20260908-009` found two compact Settings destinations whose first section heading exactly repeats the fixed page title.
- Decision: A Settings destination title establishes the page once. The first content may begin directly with its card or control identity; section headings are retained only for meaningful subdivisions within that destination.
- Why this is superior for Whip: It removes noise without flattening information architecture, saves scarce vertical space, and makes remaining headings stronger navigational landmarks.
- Consequences / reversal conditions: Organization begins with the Areas card and About begins with the Whip identity card. Wide Settings retains its sidebar-level destination context. Revisit only if detail content becomes independently embeddable without a visible destination title.
- Related: `FB-20260908-001`, `FB-20260908-002`, `FND-20260908-009`.
- Status: Verified in `IMP-20260908-006` / `VER-20260908-006`.

### DEC-20260908-009 — Play Store candidate authority includes cryptographic upload readiness

- Context: `FND-20260908-011` proved that complete tests, coverage, lint, builds, frozen inputs, and checksummed evidence can still produce an unusable public candidate when Gradle constructs unsigned release artifacts.
- Decision: `scripts/candidate` must load Whip's established release/upload signing boundary before any public candidate build, require the exact canonical signed APK and AAB paths, cryptographically verify both artifacts before evidence publication, and repeat signature verification whenever retained artifacts are checked. An unsigned fallback is a failed candidate regardless of all other green evidence.
- Why this is superior for Whip: Candidate acceptance then means the exact frozen bundle is both behaviorally qualified and uploadable, closing the gap between QA authority and Play Console reality without exposing credentials or weakening emulator-only instrumentation.
- Consequences / reversal conditions: Candidate creation now requires the protected local keystore/password files already used by private releases; source-only evidence verification may omit local artifacts, but full verification cannot. Revisit only if signing moves to a separately attested remote build service whose signed output and certificate identity are bound into the same frozen evidence.
- Related: `FB-20260908-004`, `FND-20260908-011`, `DEC-20260904-003`, `DEC-20260906-003`.
- Status: Accepted, implemented, and verified by the signed 0.3.66 candidate in `VER-20260908-012`.
