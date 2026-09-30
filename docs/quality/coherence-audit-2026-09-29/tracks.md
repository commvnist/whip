# Tracks: design, coherence and UX audit — 2026-09-29

Tracks has a coherent core: reusable definitions, recorded Entries, collection-wide Activity, and separate scoped analysis. Its strongest current design choices are the shared record presentation, explicit archive semantics, typed authoring, exact destructive reviews and recoverable CSV import. The remaining confirmed problems are inconsistent inspector return behavior, misleading duplicate-match language during editing, and destructive advice when CSV export reaches its size ceiling. Enlarged selection/reorder identity and several constrained layouts remain inspection gaps, not certified passes.

Audit baseline: `6d79bd1f82da9c1666960834410b3e08ed6d3ea1`. Related owner request: FB-20260929-017. This is an independent Tracks-only source and original-image review. No production or test changes, build, instrumentation, device operation, commit or canonical-memory edits were performed by this reviewer. Parent coordination owns new device evidence and final cross-component reconciliation.

## Evidence and reading boundaries

Read `AGENTS.md`, the complete product-memory `INDEX.md`, current FB-20260929-017/016/014/008/005 and FB-20260928-008 acceptance, DEC-20260929-002/003/004, relevant Track findings/decisions and current implementation/verification records. Historical records were used to locate contracts and avoid reviving fixed defects. Rechecked those contracts in current source rather than adopting previous audit verdicts. In particular, FND-20260929-023's lost search/recreation state, FND-20260908-002/003's archived mutation and unreachable search, and TG-3's anonymous trend evidence have current repairs and are not new findings.

Source shorthand in the inventory:

- **S**: `app/src/main/java/com/whip/app/ui/TrackScreens.kt`.
- **V**: `app/src/main/java/com/whip/app/ui/TrackViewModel.kt`.
- **A**: `app/src/main/java/com/whip/app/ui/TrackAuthoringExperience.kt`.
- **R**: `app/src/main/java/com/whip/app/ui/TrackReviewExperience.kt`.
- **M**: `app/src/main/java/com/whip/app/domain/TrackModels.kt`.
- **D**: `app/src/main/java/com/whip/app/data/TrackRepository.kt`.
- **C**: `app/src/main/java/com/whip/app/domain/TrackCsv.kt`.
- **E**: `app/src/main/java/com/whip/app/ui/TrackEditorViewModels.kt`.
- **P**: `app/src/main/java/com/whip/app/ui/TrackEditorSessionViewModel.kt`.
- **W**: `app/src/main/java/com/whip/app/ui/WhipApp.kt`.
- **Q**: `app/src/main/java/com/whip/app/ui/UnifiedSearchDialog.kt`.
- **X**: `app/src/main/java/com/whip/app/data/DomainDeletionCoordinator.kt`.

Original PNGs inspected with `view_image`:

| Evidence | Date/provenance | What inspection establishes |
| --- | --- | --- |
| [Fresh ordinary populated root](../../../artifacts/coherence-audit/2026-09-29/fresh/supporting/tracks.all.populated.png), [XML](../../../artifacts/coherence-audit/2026-09-29/fresh/supporting/tracks.all.populated.xml) | Current audit, 2026-09-29; parent-provided baseline original | Three destinations, compact count/context, one shared toolbar search, shared Track card, disclosure/add hierarchy and correct singular count. One short name and one Entry; not evidence for long-name, empty, editor or wide states. |
| [Earlier enlarged populated root](../../../artifacts/fresh-app-overhaul/2026-09-29/after/large-supporting-final/tracks.all.populated.png) | Earlier completed overhaul on the same date; historical evidence | Readable ordinary shared Track card and wrapping root navigation. Does not prove current selection/reorder cards. |
| [Earlier corrected exact Entry inspector](../../../artifacts/fresh-app-overhaul/2026-09-29/after/tracks-gym/fresh.tracks.exact-evidence-corrected.png) | Earlier completed overhaul, 2026-09-29 | Named source, date and original 2.0 L value in an inspector. Source now retains this route from Insights. Does not prove retention from Entries or Activity. |
| [Earlier scoped matching Entries](../../../artifacts/deep-product-review/2026-09-29/after/deep.tracks.matching-evidence.png) | Earlier deep review, 2026-09-29; older four-tab chrome | Date range, exact range boundaries, count and original mixed-unit values are comprehensible. Old archive tab is superseded and must not be copied into a remedy. |
| [Earlier enlarged history search with keyboard](../../../artifacts/full-suite/2026-09-29/visual/tracks.history.search.large.png) | Earlier full-suite campaign, 2026-09-29 | Archived read-only status/restore, local search and query above IME. Content is scrolled beneath fixed chrome; screenshot alone does not show a clipping defect. Not fresh current-state certification. |
| [Dated enlarged archived inspector](../../../artifacts/astra-audit/2026-09-09/track-frame-review/api34/tracks.history.inspector.large.compose-advanced.png) | 2026-09-09 | Long title, parent context and date readable in that historical dialog. Current shared inspector has subsequently evolved. |
| [Dated enlarged duplicate Choice validation](../../../artifacts/astra-audit/2026-09-09/field-validation/api34/tracks.field-edit.choice-invalid.large.png) | 2026-09-09 | Local duplicate-label message and disabled Save Field were visible. Current source still uses local validation, but no new keyboard/window geometry is claimed. |
| [Dated Number-history editor](../../../artifacts/astra-audit/2026-09-09/field-validation/api34/tracks.field-edit.number-history.ordinary.png) | 2026-09-09 | Historical type/dimension locks and default-unit explanation. Current source has improved the old disabled-selector presentation into Saved Field facts; the old presentation is not a present defect. |

Verdicts below distinguish **Keep/source** (the current design and handlers are coherent on inspection), **Observed keep** (the limited fresh root image supports that specific layout), **Finding** (a present source-grounded issue), **Gap** (runtime/rendered evidence remains absent) and **N/A** (the requested flow is deliberately unsupported). Source test declarations were inspected as intended contracts by this reviewer.

After the component source review, the parent freshly executed `TrackHistoryJourneyE2ETest#archivedHistoryCanBeSearchedRecreatedAndRestoredAtLargeText`: one pass, zero failures/skips/reuse, under the 55-second command cap. [Current originals and result receipt](../../../artifacts/coherence-audit/2026-09-29/fresh/track-history-native200/) cover archived read-only Entries, older record/date completeness, search above the IME, no-match recovery, retained query after recreation, exact archived inspector and Restore. The parent personally inspected all seven original PNGs and corresponding XML. This closes that specific enlarged history journey's uncertainty; it does not verify TR-01 correction return, selection/reorder title completeness, Scale endpoints or all authoring states.

## Prioritized findings

### TR-01 — P2: Entry inspector context survives correction from Insights but closes from Entries and Activity

**Current evidence:** `S:1221` sets `viewedEntryId = null` before dispatching Activity's Entry edit; `S:2306` similarly clears `viewEntryId` in Entries. In contrast, `TrackInsightsPage` (`S:2577–2594`) keeps `viewedEntryUuid` while the Entry editor opens and resolves the corrected Entry afterward. All three reuse `TrackEntryDetailsDialog` (`S:2370`). This is a present handler difference, not the previously fixed older-page/history-window defect.

**Reproduction:** In an active Track, open an Entry card's read-only inspector, choose Edit, change a recorded value and Save. From Entries or Activity, the editor returns to the list and the inspector is gone. Repeat through Track Insights → Recorded Values → exact Entry → Edit: the inspector remains and displays the corrected source. Canceling the child editor also exposes the same difference.

**Impact:** Users checking several values must reopen the source after each correction in two routes, while the same operation retains inspection context in the third. The source inspection workflow therefore depends on how the Entry was discovered. This falls short of DEC-20260929-004's retained-child-context rule even though the underlying history/query/window preservation remains intact.

**Remedy:** Keep the selected Entry identity through child editing in Entries and Activity, matching the Insights contract. Prefer UUID identity; resolve updated evidence after exact success. If deletion, archive/Area scope change or identity removal makes the source unavailable, provide an explicit read-only unavailable/close state instead of silently displaying a different record. Preserve list/query/loaded-window state independently.

**Acceptance still needed:** Ordinary and actual-200% flows from all three entry points; child Save, Cancel and stale-write failure; Activity recreation; removal while open. Verify the inspector shows the exact corrected UUID/value and the parent remains in its prior scope/viewport. No runtime reproduction was run by this reviewer.

### TR-02 — P2: Duplicate-match guidance calls an existing Entry edit a new Entry

**Current evidence:** `TrackEntryEditor` computes `duplicatePrimaryMatches` for both create and edit (`S:3996–4001`); only the edited Entry itself is excluded. The match-review dialog unconditionally says “Editing the existing Entry will discard this unsaved new Entry” (`S:4162`) and offers “Keep New Entry” (`S:4166`). Its confirm action clears the draft and switches to the matched Entry (`S:4165`). `editing` is available in the same composable (`S:3945`) but does not control this copy.

**Reproduction:** Create Entries A and B with distinct required identity values. Edit A and change its identity fields to match B, then open Possible Existing Entry → Review B. The user is correcting A, but the recovery choice says Keep New Entry and the consequence says an unsaved new Entry is discarded.

**Impact:** The dialog misstates which authored work is being abandoned. Users can infer that a third Entry is about to be added, or that they are choosing to create a duplicate, when the normal Save action would update A. This is consequential switch-target guidance, not merely a capitalization preference.

**Remedy:** Keep duplicate detection and allowed duplicates. In edit mode use “Keep Editing This Entry” and explain that switching to the other Entry discards unsaved changes to the current Entry; include both identities/dates where ambiguity remains. Keep create-mode wording for actual new Entries. Use the full Entry inspector or its existing bounded-preview role for match details if needed; do not introduce a new duplicate policy.

**Acceptance still needed:** Create and edit mode matches, composite identities, same-date matches, match-dialog Cancel and Edit Existing, dirty raw Number input, recreation. Verify A remains unchanged when switching and B is the exact new editor target.

### TR-03 — P2: CSV export failure instructs users to delete evidence to overcome a format ceiling

**Current evidence:** `D:2468` and `D:2476`, `buildTrackCsv`, enforce a 25 MiB output bound with the message “This Track export is larger than 25 MB. Remove unneeded long-text values or older Entries, then try again.” Export's failure dialog displays that repository message verbatim (`S:887–890`). Track text/history can be valid above this output size; this is an export limit, not a data-validation failure. `TrackCsvReliabilityPolicyTest#exportStopsBeforeCrossingItsByteCeiling` (`app/src/test/java/com/whip/app/ui/TrackCsvReliabilityPolicyTest.kt:146`) documents the ceiling contract but does not test the usability of the advice.

**Reproduction:** Export a valid Track whose quoted UTF-8 field values and metadata exceed `TRACK_CSV_MAX_EXPORT_BYTES` (25 × 1024 × 1024), for example a long-running text-heavy log. Review the displayed failure. Its recommended way to obtain a portable copy is to remove original content/history first. This is a deterministic source path; no maximum-size device export was executed here.

**Impact:** A user trying to preserve or move history is directed toward permanently altering the source, without the existing whole-app portable backup alternative. Changing destination or retrying the same export cannot fix this size error, so generic export retry controls are also weak guidance for this particular cause.

**Remedy:** Keep the intentional allocation ceiling. Explain the supported CSV size and that saved data is intact, then offer or direct to the existing portable backup in Settings. A later date/field-scoped export or streaming export could address the actual product need, but is not necessary for the immediate copy fix. Do not present deletion as the default export-recovery path. Differentiate size-limit failure from destination/write failure so the next action can succeed.

**Acceptance still needed:** Deterministic bounded source fixture plus native error presentation at ordinary/200%; existing Settings backup handoff/return if made actionable. Confirm the failed CSV path does not change Entries or values.

### TR-04 — P3: Permanent-delete preparation promises integration verification that its current impact does not contain

**Current evidence:** Loading copy says Whip is verifying “Track, history, fields, and integrations” (`S:713`). The current `X:655–690` impact builder and `X:1075` `TrackDeletionImpact` only include Track, Fields, Choices, Entries and saved values; the revision includes those rows. No active Link/Trigger authoring or execution route remains. DEC-20260901-020 records retired schema-31 Link/Trigger compatibility, so a historical integration feature must not be inferred as live from old memory.

**Reproduction:** Track Options → Delete Track Permanently → preparation state. Compare the verification claim with the subsequent exact impact counts and current builder.

**Impact:** Minor stale product language implies a broader dependency review than the current deletion path performs and distracts from the understandable fact/history consequences.

**Remedy:** Name the exact current graph being reviewed. If legacy compatibility references are intentionally consequential here, first define and expose that actual impact; do not claim verification solely because older versions had integrations. The current exact revision and blocked-dismissal behavior should remain.

## Source-supported risks requiring fresh reproduction

These are not additional confirmed findings or automatic redesign recommendations.

1. **Selection/reorder title regression at enlarged text.** Ordinary Track cards use the recently repaired shared productivity identity renderer (`S:1768`). `TrackRow`'s selection/reorder branch still hardcodes `maxLines = 2` with ellipsis (`S:1706–1707`), plus a one-line Area/count and two-line latest summary. At 200% a long ordinary identity may be hidden precisely while selecting a consequential bulk action. Capture a long-name Track in normal, selection and reorder at identical width before concluding severity. Remedy, if reproduced: use the shared enlarged-identity policy in this branch without expanding ordinary density.
2. **Scale endpoint competition at enlarged text.** `TrackEntryField` uses a Row with weighted low-label Text and unweighted high-label Text (`S:4368–4380`) while both endpoint labels can have 40 characters. The numerical increment controls and recorded-values paging also use fixed Rows (`S:4326`, `R:127`). Capture long endpoint labels, 200%, narrow pane and short keyboard viewport; verify both endpoints, controls and full action labels are visible or naturally wrapped. Source alone cannot prove overflow.
3. **Entry-conflict draft recovery needs a complete journey.** A blocked conflict (`S:4056–4076`) retains draft data but directs the user to close and review history; closing a dirty draft offers Discard or Keep Editing (`S:4139`). Check whether the complete shell route provides a practical way to review the current source while keeping reusable authored data. Definition conflict has explicit Save Draft as New Track (`S:3310`); Entry conflicts have different semantics and must not be given unsafe automatic merge/retry behavior. Record actual user cost before proposing an additional action.
4. **Wide master mode controls differ from compact controls.** External wide More always offers Select Tracks (`S:610`), while compact More toggles Cancel Selection (`S:1479`). The shared selection panel still has Done, so this is not a trap. Inspect the external menu while already selecting/reordering and see whether it creates an unnecessary or misleading duplicate mode entry before changing it.

## Flow-state coverage inventory

Each row is one reviewed user job/state or explicit supported boundary. Grouping a field's ordinary validation with its source does not imply the full Cartesian product of every device and form state was observed. Device variants are separately inventoried below. Source line anchors are for the audited baseline.

| ID | Flow/state | Current owner and evidence | Verdict and reasoning |
| --- | --- | --- | --- |
| T01 | Fresh entry into Tracks | S:284 `TrackAreaContent`; fresh root | Observed keep: three distinct collection/activity/insights jobs, no artificial Today. |
| T02 | Root count and context | S:1450; fresh root | Observed keep: correct singular count, concise reusable-log purpose. |
| T03 | Ordinary populated Track card | S:1741 `TrackSummaryRow`; fresh root | Observed keep: recognizable identity, count/Area, disclosure, direct capture. |
| T04 | Expanded Track card/latest record | S:1781 `details`; shared disclosure state | Keep/source: concise latest identity/date behind consistent disclosure; need fresh expanded original. |
| T05 | Empty active collection | S:1554 `WhipEmptyState` | Keep/source: creation and Area-scope explanation distinguish scoped emptiness from absence everywhere. |
| T06 | Collection loading | S:1550 `DomainLoadContent` | Keep/source: does not render authoritative empty result while loading. |
| T07 | Collection failure/retry | S:1550; V:834 `retryLoading` | Keep/source: domain-owned retry; failure rendering not freshly executed. |
| T08 | Area scope narrows Tracks | V:108 `forArea`; W TrackAreaContent state | Keep/source: filtered projection owns collection and workspace analysis. |
| T09 | Global collection search | `WhipNavigationPolicy.kt:42`; Q:374 | Keep/source: one toolbar search, explicit Tracks & Entries scope. |
| T10 | Search active Track result | S:467 `openTrackIdRequest` | Keep/source: selects exact definition and Entries destination. |
| T11 | Search archived Track result | S:471 archive branch | Keep/source: lands in archived context rather than active collection. |
| T12 | Search active Entry result | S:491 `openEntryIdRequest` | Keep/source: exact active editor route, selected Track established. |
| T13 | Search archived Entry result | S:497 `requestedReadOnlyEntryId` | Keep/source: exact read-only inspector; old writable-archive defect repaired. |
| T14 | Large-history global lookup | Q:84, Q:393 bounded history index | Keep/source: intentionally limited global search; completeness notice is shell-owned, not exhaustive per-Track search. |
| T15 | Open archive from collection | S:525 `openArchive`; S:1468 | Keep/source: secondary destination has clear return route. |
| T16 | Open archive from Activity/Insights | S:642, S:658; archiveReturn | Keep/source: records origin destination before entering archive. |
| T17 | Archive system Back | S:532 `BackHandler` | Keep/source: returns archive origin after detail Back; explicit root-return integration belongs to parent. |
| T18 | Archived populated cards | S:1800 `primaryAction` | Keep/source: unavailable add action omitted; definition/history still accessible. |
| T19 | Archived empty collection | S:1556 | Keep/source: truthful explanation, no invalid creation-in-archive prompt. |
| T20 | Long-press bulk selection | S:1654 `toggleable`; S:1754 `combinedClickable` | Keep/source: touch/semantic selection with explicit mode. |
| T21 | Selection pruned by Area/data scope | S:1417–1427 | Keep/source: visible-only action IDs, prune only after successful load. |
| T22 | Bulk pin/unpin | S:1515; V:1235 | Keep/source: actionable exact visible selection, Home Quick Log meaning stated. |
| T23 | Bulk archive/restore | S:1528; V:1273 | Keep/source: transaction and request receipt; failure keeps selection. |
| T24 | Selection failure/recreation | S:1400 `rememberPersistenceRequestCoordinator` | Keep/source: matching result owns exit; orphan recovery keeps selection. Native evidence absent in this audit. |
| T25 | Reorder active definitions | S:1439 `moveWithin`, S:1495 mode bar; V:1287 | Keep/source: explicit pinned/other boundaries and silent successful reorder. |
| T26 | Reorder from filtered Area | S:1407; W onShowAllAreasForReorder callback | Keep/source: Show All Areas & Reorder explains scope expansion. |
| T27 | Reorder error | V:1287 `runSilentReorder` | Keep/source: rollback copy; no fresh failure execution. |
| T28 | Wide list/detail responsive move | S:584 width >=760; movableContentOf, saveable holders | Keep/source: one feature-owned pane boundary preserves detail/list owners; wide original absent. |
| T29 | Short focused detail navigation | S:568; S:1840; W:1824 | Keep/source: removes redundant root chrome under constraint; shared shell plus/header add retains capture route. |
| T30 | Selected Track context/title | S:1849 `track-detail-header` | Gap: ordinary/200% and narrow header identity require current originals. |
| T31 | Detail Entries/Options/Track Insights | S:220; S:1881 destination bar | Keep/source: names selected-Track analysis scope distinctly. |
| T32 | Detail direct capture | W:1631 shell Add; S:1869 focused header | Keep/source: add is available through current selected-Track shell action; absence of a second normal body button is not a defect. |
| T33 | Create blank Track | S:3130 default draft | Keep/source: one required Name identity provides a usable starting form. |
| T34 | Select Reading template | A:18–43; S:3444 | Keep/source: Title/Pages/Notes, count unit and precision have concrete everyday meaning. |
| T35 | Select Spending template | A:18–43 | Keep/source: required amount and explicit currency unit; no invented multi-currency analytics. |
| T36 | Select Reflection template | A:18–43 | Keep/source: named identity, 1–5 mood and optional notes. |
| T37 | Template replaces dirty draft | S:3453–3470 | Keep/source: explicit replacement review names affected draft and preserved Area. |
| T38 | Draft identity/name/description/icon | S:3330, S:3379; M:489 | Keep/source: bounded name/description and shared emoji owner; full unicode picker belongs to parent. |
| T39 | Definition Area/tags/new Area | S:3402 `ProductivityOrganizationSection` | Keep/source: shared organization owner, tags normalized at Save; async new Area recovery shared. |
| T40 | Definition required/name/duplicate-field validation | S:3250; M:489–514 | Keep/source: canonical validation, duplicate names rejected, repeated failed Save reveals summary. |
| T41 | Field creation and nested cancel | S:3476; S:3614 `TrackFieldEditor` | Keep/source: completed child state removed; active child draft saveable. |
| T42 | Field reorder | S:3350 `WhipReorderHandle` | Keep/source: explicit order has accessibility affordance and scoped lifecycle. |
| T43 | Short Text field configuration | S:3614; M:525 | Keep/source: simple type/name/required/identity/list flags; no meaningless extra settings. |
| T44 | Long Text field configuration | S:3614; M:525 | Keep/source: shares sensible identity/required controls; authoring values have separate multiline UI. |
| T45 | Number type/dimension/default unit | S:3740–3780; `freshTrackNumberUnitId`:3901 | Keep/source: fresh settings defaults and same-dimension compatibility; original units preserved. |
| T46 | Number precision 0–6 | S:3782; M:529 | Keep/source: explicit bounded presentation setting. |
| T47 | Archived unit retention/custom unit creation | S:3758 `UnitSelectionField` | Keep/source: retained archived default disclosed, active replacement and shared custom creation. |
| T48 | Existing Number type/dimension lock | S:3731 `Saved Field`; D definition mutation boundary | Keep/source: clear explanation instead of an actionable-looking disabled selector. |
| T49 | Single Choice add/rename/reorder | S:3786–3828 | Keep/source: local labels and stable identities, explicit ordering. |
| T50 | Blank/duplicate Choice label | S:3809; M:517 | Keep/source: immediate local error and disabled Save Field. Dated large original supports historical presentation only. |
| T51 | Scale presets/bounds/step | S:3830–3873; M:421 scale helpers | Keep/source: fractional steps, explicit selectable count and native text errors. |
| T52 | Scale saved-history compatibility | S:3665 `incompatibleScaleValue`; M:461 | Keep/source: cannot remove an existing selectable value accidentally. |
| T53 | Scale endpoint labels | S:3875–3876, S:4368 | Gap: long-label enlarged entry layout needs current geometry; source risk 2. |
| T54 | Date/Yes-No field configuration | S:3614 type switch | Keep/source: no extraneous numeric controls, optional-answer semantics preserved. |
| T55 | Composite Entry identity | S:3878; M:495; S:4616 | Keep/source: one or more identity fields, all required; duplicate values are permitted. |
| T56 | Definition form preview | A:46, A:72; S:3340 | Keep/source: reuses typed Entry controls, explicitly disposable sample, Check Sample validates. |
| T57 | Preview invalid number/required | A:114 `validation`; S:3342 | Keep/source: malformed optional raw Number does not masquerade as valid blank. |
| T58 | Remove Field in draft | S:3510 `confirmFieldDeleteIndex` | Keep/source: explains saved values and exact final review; prevents deleting final/primary field via direct control. |
| T59 | Remove/remap Choice with saved evidence | S:3540 removal review; S:3570 candidates | Keep/source: explicit Delete Saved Values or Move Values to stable candidate, exact review refresh. |
| T60 | Definition stale/target missing | S:3300 conflict; V:1079 review | Keep/source: draft retained, Save Draft as New Track available; newer original protected. |
| T61 | Definition save failure/busy | S:3320 error; S:3430 saving overlay | Keep/source: exact result owns dismissal; Repeat Save/dismiss blocked while running. |
| T62 | Definition discard | S:3190 dirty; S:3530 discard confirmation | Keep/source: dirty warning, not automatic loss. |
| T63 | Definition full draft recovery | E:20; P:61 `saveCheckpoint` | Keep/source: file-backed authored graph; Android registry holds compact reference, generation guard. Process death not newly executed. |
| T64 | Recovery file error/retry | P:86, P:108; `TrackEditorRecoveryNotice.kt:12` | Keep/source: distinguishes open draft without recovery copy from unreadable checkpoint. |
| T65 | Add Entry from card/Home/search | S:1800; W:1631; S:511 addEntry request | Keep/source: exact Track form boundary; shared editor route. Home Quick Log presentation parent-owned. |
| T66 | Entry Date/backfill/future | S:4088 date control; M `TrackEntryDraft`; R:19 ranges | Keep/source: date belongs to fact, creation time unchanged; future All Dates explicitly disclosed. |
| T67 | Entry time-of-day field | M `TrackFieldType`; S:4079 | N/A: no user-authored time field. Creation timestamp orders tied Entry dates; audit does not invent time authoring. |
| T68 | Short/Long Text Entry | S:4235–4260 | Keep/source: text data retained, multiline form bounded to 3–8 visible lines with scrolling. Keyboard evidence gap. |
| T69 | Number Entry/raw invalid input | S:3984 invalidNumbers; E:149 updateNumberValue | Keep/source: raw text survives and dirty logic includes invalid text; field errors and summary guide correction. |
| T70 | Entry number unit switching | S:4300 compatible units | Keep/source: entered unit meaning explicit; retained archived unit plus active compatible choices. |
| T71 | Single Choice Entry/unanswered | S:4316 | Keep/source: explicit Unanswered and required validation; stable option UUID. |
| T72 | Scale Entry/increment/clear | S:4320–4390 | Keep/source: Not Set, exact increment, slider, optional Clear; constrained label/control geometry gap. |
| T73 | Date field select/clear | S:4392 | Keep/source: optional date clear, separate from Entry Date. TalkBack selected-date speech unmeasured. |
| T74 | Yes/No/unanswered Entry | S:4405 | Keep/source: optional unanswered distinct from No; required flag requires explicit answer. |
| T75 | Required Entry error and repeated Add | S:3982, S:4037, S:4017 validation scroll | Keep/source: first invalid field revealed; polite error semantics. |
| T76 | Entry correction/stale write | V:1366 saveEntry; D:907 updateEntry | Keep/source: exact captured boundary protects current values; conflict draft practical recovery gap. |
| T77 | Duplicate detection in create | S:4005, S:4120 | Keep/source: warns, permits intended duplicates and offers reviewed handoff. |
| T78 | Duplicate detection in edit | S:4162–4166 | Finding TR-02: language claims new Entry even while correcting a saved one. |
| T79 | Duplicate Entry action | S:2330 entry commands; S:3028 options | N/A: there is duplicate-identity detection, but no direct duplicate-record command. Duplicate Structure deliberately copies no Entries. |
| T80 | Entry editor dirty cancel/recreation | S:3980 dirty; S:4139; E:101; P:61 | Keep/source: discard protects raw text and typed values; current runtime not executed. |
| T81 | Row deletion preparation/review | S:774; S:923 `TrackEntryDeleteRoute` | Keep/source: compact exact candidate, date/value count and immediate Undo meaning. |
| T82 | Delete from Entry editor | S:4170 deleteConfirm; D:961 | Keep/source: exact saved snapshot rather than unsaved draft; blocked while deleting. |
| T83 | Delete conflict/read-only review | S:988 hasConflict; S:810 onReviewEntry | Keep/source: review current exact source, unavailable target cannot blindly retry. |
| T84 | Undo deletion/superseded undo/failure | V:1562; `TrackEntryFeedback.kt:119` | Keep/source: exact latest recovery token, persistent retry on failure; not unlimited undo history. |
| T85 | Archived Entries/details/restoration | S:2105, S:2240; S:2303 | Keep/source: read-only controls omitted, Restore Track explicit. |
| T86 | Ordinary paged history/load/error | S:1983 `reloadPage`; S:2050 databasePagedView | Keep/source: 100-row pages, authoritative loading/error, Try Again. |
| T87 | Load more/history correction return | S:2236; requestedEntryCount/pageContentVersion | Keep/source: older loaded window and viewport retained rather than reset after child write. |
| T88 | Search Entries text/result/no match | S:2015 search effect; S:2130 search field; V:1763 | Keep/source: accessible local scope, debounced repository FTS, failure not definitive empty result. |
| T89 | Search failure/retry/recreation | S:2029, S:2170; movable detail boundary S:566 | Keep/source: search generation/structural content keys and preserved query; old recreation issue repaired. |
| T90 | Sort by Entry Date/creation/typed field | S:2260 sort dialog; S:4636 sortedEntries | Keep/source: visible selected ordering, missing field resets to valid fallback. |
| T91 | Conditions Apply/Cancel/Match All/Any | S:4451 TrackFilterDialog | Keep/source: staged filter state with explicit Apply, edited child lifecycle cleared. |
| T92 | Typed text/number/choice/date/yes-no conditions | S:4508 TrackConditionEditor; M condition operators | Keep/source: compatible operators, unit-aware canonical thresholds, optional blank/answered predicates. |
| T93 | Applied conditions and removal | S:4417 TrackAppliedConditions | Keep/source: combination rule, value/unit summary, per-condition remove and Clear All. |
| T94 | Shared Entries/Insights date-condition scope | S:1839 reviewScopeState; R:33 reviewEntries | Keep/source: one saved meaning; View Matching Entries clears unrelated text search explicitly in handler. |
| T95 | Entry inspector from Entries/Activity | S:1221, S:2306 | Finding TR-01: closes before child edit while Insights retains source. |
| T96 | Activity normal/empty/filter/no match | S:1037–1204 | Keep/source: active visible Tracks only, date/Track/text criteria in staged dialog; scope truthfully stated. |
| T97 | Activity open source/delete/edit | S:1150 TrackEntryRow; S:645 onOpenTrack | Keep/source: direct exact source and correction, archive history intentionally separate; inspector return TR-01. |
| T98 | Workspace Insights empty/no evidence | S:1270–1301 | Keep/source: Create Track/Open Tracks before a zero-filled dashboard; archive access retained. |
| T99 | Workspace frequency/recent/numeric | S:1307–1361; TrackInsightNumberFormat | Keep/source: date-window bounds, named recent sources, no incompatible cross-Track sums. |
| T100 | Track Insights no evidence/no match | S:2480 | Keep/source: meaningful first Entry/restore guidance, retained filters not disguised as empty history. |
| T101 | Number/Scale/temperature summaries | S:2515–2555; `TrackInsightNumberFormat.kt:11` | Keep/source: canonical arithmetic with selected-unit conversion; nonadditive totals omitted. |
| T102 | Categorical/date/text summaries | S:2557–2574 | Keep/source: unanswered distinct, per-field evidence counts; qualitative fields not converted into scores. |
| T103 | Numeric trend/missing/repeated dates | R:38, R:64 | Keep/source: real observations, omitted missing values, repeated dates stay separate; interpolating line is accompanied by explanation. |
| T104 | Recorded Values exact source/page | R:108–131; S:2577 | Keep/source: named original entered units, 25 observations/page and exact UUID inspector. Earlier corrected original is supporting context only. |
| T105 | Insights correction/source disappears | S:2587–2602 | Keep/source: selected UUID retained, correction updates exact evidence, missing source explicit. |
| T106 | Structure duplication | S:3070; V:1218; D:583 | Keep/source: copies definition with fresh child identities and no Entries; no immediate open-new-copy affordance, optional discoverability refinement. |
| T107 | Options pin/archive/restore/export/import | S:3028 | Keep/source: explanatory action rows, archived import omitted and definition edits explicitly allowed. |
| T108 | Permanent Track delete/counts/failure | S:684–767; X:655 | Keep/source for exact own-graph revision/count review and request ownership; TR-04 stale integration preparation copy. |
| T109 | CSV document picker/cancel/replace cancel | S:394 launcher; S:3022 cancellation policy | Keep/source: cancel new selection ends it; cancel replacement preserves existing reviewed file. |
| T110 | CSV reading/UTF-8/bounds | V:412, V:432, V:2037 | Keep/source: 5 MiB/5,000 rows/100 columns/100,000 cells, strict text envelope before preview. |
| T111 | CSV default mapping/Entry Date fallback | V:506; S:2860 | Keep/source: displayed fallback date and per-field mapping; not silently using current date after recovery. |
| T112 | CSV Number/unit/choice/date interpretation | C preview parser; S:2890–2930 | Keep/source: explicit column/default-unit mapping and typed invalid-row feedback. |
| T113 | CSV concrete row preview/page/large cell | `TrackCsvEntryPreview.kt:33`; S:2960 | Keep/source: interpreted draft shown, bounded Unicode excerpt discloses shortening; complete imported data retained. |
| T114 | CSV invalid rows/mapping correction/file replacement | S:2811, S:2935 | Keep/source: all-or-nothing import, first 50 issues plus remaining count, recoverable mapping/file choice. |
| T115 | CSV commit blocked/dismissal/exact identity | S:2688; V:1423 importEntries | Keep/source: reviewed batch identity and frozen form owns commit; no double submission while running. |
| T116 | CSV interruption/receipt recovery | V:226 descriptor; V:285 store; V restoreCsvImportSession | Keep/source: deterministic row identities/receipt verification distinguish already applied from retryable failure. |
| T117 | CSV changed/archived/missing target | S:2657 targetLookupStatus; V:1477 conflict | Keep/source: explicit unavailable state, replacement rather than unsafe retry of changed form. |
| T118 | CSV completion | S:2755 complete receipt | Keep/source: count/already-applied outcome and Done retained; no premature modal close. |
| T119 | CSV export destination/error/cancel | S:870; V:2010; D:2456 | Finding TR-03 for size error guidance; ordinary write failure keeps retry/another destination. |
| T120 | CSV export original units/UUID/date/escaping | D:2516 build columns/rows | Keep/source: original entered values plus canonical Number columns and identity/date, UTF-8 byte-aware escaping. |
| T121 | Settings defaults influence new Number fields | S:3901 freshTrackNumberUnitId | Keep/source: current preferred unit and number precision apply to fresh deliberate Number fields; history unaffected. |
| T122 | Retired Link/Trigger creation/execution | No active UI/source catalog; DEC-20260901-020; current D | N/A: retired schema-31 feature; historical compatibility rows do not justify inventing live handoff. |
| T123 | Review/global Track evidence handoff | `ReviewSourceContent.kt:15`; W Review Track callback | Keep/source: Track evidence remains separate from scored outcomes; parent owns whole-app Area return and global-source presentation. |
| T124 | Fresh ordinary phone visual | Current root PNG inspected | Observed keep within one populated root fixture, not universal Tracks acceptance. |
| T125 | Actual 200% phone visual | Historical enlarged root/search/Choice originals inspected; S:1708 | Gap: current root + long title + special modes/editor evidence needed. |
| T126 | Short-height/IME visual | S:568, S:1840; focused-input visibility in field/condition/search | Gap: fresh geometry/actions/focus recovery for mixed-entry and nested forms. |
| T127 | Wide/narrow pane visual | S:584; field bounds stack S:3839 | Gap: current wide original, pane dialogs, long identity and mode actions. |
| T128 | TalkBack reading/focus/error/reorder | S:4230 accessibleFieldName; S:4409 liveRegion; R:96 Canvas semantics | Gap: semantic intent inspected, but no current spoken/focus traversal; required state for Scale/Yes-No/Choice needs actual speech. |

Reviewed count: **128 flow-state dispositions**. These include supported behavior, unavailable variants and explicitly unsupported routes; they are not 128 instrumented executions.

## Justified keeps and cross-component design implications

- Keep the single root search entry and the three workspace jobs. Activity text narrowing belongs in Filters, while per-Track Entries can retain its local search. Adding another collection search field would repeat the owner's already rejected design.
- Keep definitions and facts distinct. Duplicate Structure means reusable schema, archive means capture paused, and Entry Date means when evidence belongs. Task recurrence, Habit Today and Goal completion concepts should not be imported into Tracks to make tab counts look uniform.
- Keep typed controls and field-local errors. Required flags, optional unanswered states, canonical units, fractional scale increments and stable Choice identities are product meaning, not visual inconsistency to flatten away.
- Keep exact destructive reviews, form revisions and stable create/import identities. They allow meaningful recovery without speculative automatic merges. The remedy is consistent retained context and clearer actions, not optimistic dismissals.
- Keep neutral shared record/summary roles, readable original-unit facts and named trend source rows. The current shared root card is compact and balanced. No evidence supports reintroducing a bespoke Track card system or anonymous chart-only analysis.
- Keep bounded preview/import/export allocation. A bounded excerpt or global search index is acceptable when clearly disclosed; bounded export must offer recovery consistent with preserving the user's original evidence.
- Keep explicit all-dates/future-date and recent-window semantics. Historical screenshots showing future totals beside recent zeroes are not contradictory when the current labels name their scopes.

## Verification limits and next evidence

Current source supports the reviewed route/meaning decisions, and the fresh ordinary populated root supports that specific hierarchy. It does not establish completed keyboard, TalkBack, native process-death, wide or actual-200% acceptance for every Tracks flow. The historical images retain useful examples of validation and exact facts, but their previous suite outcomes do not certify this baseline's whole experience.

Prioritize new captures/journeys in this order: inspector correction from Entries/Activity versus Insights; edit-mode duplicate-match review; long identity in selection/reorder at 200%; long Scale endpoint labels and lower mixed-entry keyboard field; CSV malformed/ready/size-limit failure; definition removal/Choice remapping; wide selected Track plus special mode; spoken required/error/selected-date state and reorder affordances. Include explicit Save/Cancel/failure/recreation results, rather than only static surfaces. The parent may append or link its exact fresh receipts after inspection without changing these source-only conclusions into unsupported passes.
