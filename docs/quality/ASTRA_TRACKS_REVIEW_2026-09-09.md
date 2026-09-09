# Tracks review in progress

Owner: FB-20260908-006. Verification: VER-20260909-016. The whole Tracks area remains Investigating.

## Baseline and scope

The fresh 28-state baseline was captured from clean `5ae0892` on the API 34 phone in run `zBSwa4`: 11 owners, zero failures/skips/reuse. All 28 original PNGs have now received individual personal review. Original PNG/XML pairs, capture manifest/catalog and checksums are retained in [the baseline evidence](../../artifacts/astra-audit/2026-09-09/track-baseline/). This is a review of the displayed frames, not acceptance of complete journeys, every state, or accessibility. Four subsequently added numeric states have separate, bounded verification and retained evidence in [the numeric correction](../../artifacts/astra-audit/2026-09-09/track-numeric-insights/README.md), delivered in `482669b`.

## Per-frame observations

| Surface suffix (`tracks.`) | Rendered observation and limits |
| --- | --- |
| all.populated | Clear single Reading Log card, count, expansion and Add Entry. Sparse one-item fixture does not exercise a realistic collection. |
| all.expanded | Area, latest Entry and Edit are readable. Complete collection density and long labels remain open. |
| all.empty | Create First Track is clear. Intro and empty guidance repeat generic recording/comparison language; a concrete example may help discovery. |
| activity.populated | Chronological Entry and local search/filter are readable. Intro/count wrap and repeated context merit realistic-history review. |
| activity.menu | Anchored Open Track and separated destructive Delete Entry are legible. |
| reorder.menu | The displayed menu contains Select Tracks only. It is not evidence of selection or reordering. |
| archived.populated | Readable archived collection and no Add Entry action. Generic active-recording intro does not explain archive. |
| archived.detail.entries | A large Track Archived notice precedes populated history. The read-only row is clear; the notice and two navigation levels spend substantial compact-screen space before records. |
| detail.entries | Workspace tabs, Track identity and inner tabs precede scoped search/filter/sort and the Entry. Short-window and enlarged-text journeys remain necessary. |
| detail.search | Ordinary API 34 keyboard, query and full matching Entry are visible. Does not establish short-window or large-text quality. |
| detail.search.empty | Ordinary keyboard, No Matching Entries and recovery guidance are visible. |
| entry.menu | Anchored Delete Entry is legible and distinct. |
| entry.details | Identity, date, Recorded status and primary Book value are readable. Text-only fixture does not cover other value types. |
| detail.options | Actions are clearly grouped. “Stable Entry identity” and “one atomic import” expose implementation concepts; describe the user outcome instead. Danger zone starts below the frame. |
| insights.populated | Text-only workspace overview/frequency is legible. Numeric semantics were absent from this original fixture and are corrected separately. |
| detail.insights | Text-only counts/dates are readable. Recent Weekly Rate does not disclose its fixed 30-day denominator. Future-date windows and numeric primary Fields need review. |
| editor.create | Default required primary Name makes minimum creation simple. Optional Description/Emoji precede core Field setup. This is an isolated editor host; do not infer actual-app status-bar behavior from its gray band. |
| editor.edit | Long Track and first Fields are readable. Screenshot precedes the owning test's later conflict; it is not conflict-recovery visual evidence. |
| entry.create | Title and Entry Date are legible. Large introductory heading/helper stack warrants a real keyboard and multi-type form journey. Sep 1 is the fixture's intentional date. |
| entry.edit | Entry identity is repeated above the editable Title; date and Delete Entry are clear at ordinary text. |
| entry-delete | Exact Entry/date/value count, Undo explanation and Keep/Delete choices are readable. Isolated-host window width is not an actual-app layout conclusion. |
| detail.unavailable | Actually Entry Unavailable. “It was not reinterpreted as a new Entry” exposes an internal guarantee without helping recovery. Missing Track is a separate, uncaptured state. |
| filter | Empty filter draft: Match All/Any, no conditions, Add Condition and Apply. Existing catalog label “configured” was inaccurate. |
| condition | New default Entry Date / is on condition. Other subjects/operators/values are not covered. |
| definition-removal.large | Actual large text shows review title, impact counts and both fixed actions. Long content scrolls. Copy is verbose; full removal/conflict journey remains separate. |
| csv-import.empty-large | Empty-file error and Choose Another File appear first; Cancel and disabled zero-row import are reachable. Existing scoped recovery improvement remains visible. |
| csv-import.frozen-large | Error and Replace File are prominent; mapping is secondary and body scrolls above fixed actions. “Frozen import preview” is implementation language. |
| csv-import | Ordinary valid one-row import is legible; “1 rows” needs correct pluralization. Large unused body space belongs to this isolated host; inspect actual-app presentation before changing window size. |

## Source and journey follow-ups

- Archived status/restoration and short-detail navigation now have real 30-Entry ordinary/enlarged journeys on API 26/34/37, under FND-20260909-017 / DEC-20260909-015 / VER-20260909-018. The compact status brings the first phone record 221/444 pixels earlier. FND-20260909-018 is now verified in VER-20260909-019: whole native Track/Task input and labels, short older-record title/date, recovery and saving pass on API 26/34/37 with 64 JVM / 109 Android neighbors and 344 JVM readiness tests. All 41 retained frames have scoped review in the inline-keyboard evidence. Continue FND-20260909-019: reproduce clipped Area outlines, apparent row overlap and intermittently missing painted action icons with settled frame sequences, and reassess the smallest enlarged archive opening and wide destination spacing. Wide support counts, repeated context, occasional tight enlarged tab spacing and the API 26 boot-emoji glyph also remain review topics.
- Complete Track/Field/Choice authoring and Entry value-type journeys. Discover Field editor type configuration, Choice ordering/removal/discard, Scale validation, Number unit/precision, invalid Save after scrolling, and recovery states absent from the catalog.
- Inspect history sorting, applied multi-condition filters, pagination, bulk selection/reorder, missing Track and asynchronous definition conflict. Catalog source ownership alone cannot establish state coverage.
- Reconcile Number Field helper text promising “compatible Goals” with current source after schema 46 removed legacy automation. Do not retain implementation terminology or unsupported capability promises.
- Verify future-dated recent counts, primary numeric identity formatting with custom units, and Insights for numeric primary Fields. These are source leads, not yet runtime-confirmed defects.
- Inspect complete repository/CSV/ViewModel boundaries, realistic large-history responsiveness, RTL/TalkBack, compact/wide/rotation/IME and interruption behavior. Existing exact history, mutation and CSV receipt contracts remain required.

No general visual acceptance or whole-Track completion is claimed by these observations. Findings and decisions will follow reproducible behavior and complete representative flows.
