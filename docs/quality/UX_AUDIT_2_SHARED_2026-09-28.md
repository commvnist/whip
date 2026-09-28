# Second shared interaction investigation — 2026-09-28

Status: **Confirmed findings; accepted implementation plan**. FB-20260928-002. Baseline `7c77c075`.

| Workflow / owner | Investigation and disposition |
| --- | --- |
| Destination/adaptive shell (`WhipApp`, `AdaptiveLayout`, `WhipWorkspaceLayout`) | Keep six destinations, Area scope and fold-aware panes. Domain agents inspect Home scope/count semantics. No evidence supports replacing navigation; verify route/recreation neighbors. |
| Inspector (`EntityInspector`) | **SH-1 native layout defect.** At1080×1400/density420 and actual200% text, long title/context/status leave less than96dp evidence space. Bound scrollable identity height while keeping Edit/Close, tabs and primary action reachable; retain ordinary layout and section state. |
| Loading/error/retry (`DomainLoadContent`, `WhipStatusCard`) | **SH-2 native recovery defect.** Long failure text in320×300dp/200% clips Retry and its label; capture detects an unlabeled action. Scroll content within available height, retain centered short states, full error, polite announcement and retry. |
| Search history bounds (`UnifiedSearchDialog`, `BoundedSearchIndexBuilder`) | **SH-3 source correctness defect.** Per-entity100-log/500-entry caps silently omit content; only2000-result cap marks partial. Route history limits through incomplete-domain accounting; preserve limits and explain partial search in user language, with full feature-history guidance. |
| Search numeric identity/history | **SH-4 source presentation defect.** Track numeric values expose stored unit IDs; Habit history formatter lacks custom units. Reuse domain agents' resolved-unit helpers; retain storage/duplicate identity and exact routing. |
| Search Task return route | **SH-5 source navigation defect.** Search resolves Today tasks through the broad planning collection and chooses Upcoming behind their inspector; archived/completed routes may inherit the prior tab. Set destination from actual collection precedence, including Today before planning; verify closing a Today result returns to Today. |
| Search debounce/scope/loading/errors/pagination | Keep asynchronous indexing, settlement, domain-specific failures, clear query, Area override and50-result pagination. Existing rules/native tests cover partial loading/focus; extend this contract instead of replacing search. |
| Choices / Area / units | Keep full values, native selected semantics and disabled menu dismissal. Domain agents correct incompatible choices and reinterpretation at editor owners. |
| Authored dialogs / dirty drafts / saving | Reviewed `ProductivityEditorComponents`, `EditorProtection`, `WhipDialog`: keep full-window placement, IME padding, fixed Save/Cancel, mutation overlay and discard protection. Domain matrices own actual validation semantics. |
| Date wheels / calendar / clock | Reviewed switching, leap-day clamping, first-day settings, saving lock and picker state. Keep shared API; enforce caller-specific date boundaries at save. No generic bound redesign without a concrete need. |
| Snackbar / undo / Area feedback | Reviewed operation dispatch, failure priority, tokens and recovery scope. Keep existing delivery/cleanup; no new demonstrated defect. Native mutation/recovery neighbors protect it. |
| Buttons/chips/destructive actions/headings/theme | Keep existing48dp actions, native roles, disabled destructive styling and semantic colors. No arbitrary replacement palette/library. Source review is not universal accessibility certification. |

## Reproduction and acceptance

`ANDROID_SERIAL=emulator-5554 timeout --kill-after=3s 55s scripts/qa-targeted --android com.whip.app.ui.SharedResilienceAuditUiTest` completed2 tests with2 failures, not a timeout. Raw results: `build/instrumentation-results-4tUKfI`; personally inspected originals: `build/audit2-before/`. Inspector failed minimum evidence-space assertion; clipped Retry failed accessible-label capture.

- SH-1: actual200%/short window, at least96dp evidence viewport, last row scrollable, tabs and fixed actions operable; existing ordinary/large inspector neighbors.
- SH-2: full failure text available, Retry scrollable/readable/clickable exactly once; short-state layout retained.
- SH-3/4: exact/below/above bounds disclose omissions for only affected selected domains; direct matches/routes remain correct and custom units are readable.
- SH-5: existing large-text/recreation search journey closes the resolved Today inspector and asserts Today selected; no change to record/occurrence precedence.

First post-fix SH-1/2 diagnostic executed2 methods with zero failures, but the runner rejected its evidence because a resource changed during the batch. Retain as diagnostic only and rerun once source is stable; do not count it as accepted validation.

Retain baseline failures and final originals; report exact executed scope and remaining limits.
