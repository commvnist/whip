# Track collection failure recovery — 2026-09-21

Scope: `FND/DEC/IMP/VER-20260921-002` under the active whole-product audit `FB-20260920-001`. This checkpoint covers actual Track reorder failure and request-owned multi-Track pin/archive/restore failure. It is not complete Tracks or whole-product acceptance.

## Finding and source-backed baseline

`RoomTrackRepository.reorder` and the multi-Track mutations were already transactional, so a storage exception preserved database truth. The collection UI did not preserve interaction truth: `AllTracksPage` cleared `selectedIds` and exited selection mode immediately after dispatching an asynchronous bulk request. A failed transaction therefore removed the exact user selection before its result existed and forced the user to reconstruct it. Reorder failure also surfaced the storage exception's raw message without explicitly saying whether the prior order remained intact.

The failure path is source-backed by the pre-change immediate `selectedIds = emptySet()` / `selecting = false` statements and reproduced against production Room with SQLite `RAISE(ABORT)` triggers in `TrackCollectionFailureJourneyE2ETest`. There is no accepted before-image because the missing state was precisely the retry UI that disappeared.

## Implementation

- Multi-Track pin/archive/restore now uses one request-scoped `PersistenceRequestState` and the shared save coordinator. Actions remain disabled while their owned request runs; success exits selection; failure retains the exact visible IDs and presents an inline retry path that survives Activity recreation.
- Each collection mutation runs in one Room transaction. Failure copy promises no changes only because the trigger journey verifies exact rollback of complete `TrackProjection` graphs.
- Pinning separates the committed Track transaction from best-effort Home-section reveal. A post-commit reveal problem is a success warning, never a retryable failure for an already-saved mutation. The same truth boundary now applies to single-Track pinning.
- Reorder failure uses explicit, sanitized copy: the previous order is unchanged and can be retried. The repository transaction remains the source of rollback truth.
- `tracks.collection.mutation-failure` is a new catalog state, bringing the current inventory to 525 required captures with zero pending selectors or platform exceptions.

## Verification

- Exact final forced-failure/retry journey: `build/instrumentation-results-tcfbal`, 1/1 Android method, zero failures/skips. It installs a real reorder trigger, verifies the exact original projections after failure, removes the trigger and persists the retry; it then fails the second selected Track during archive, verifies whole-transaction rollback plus retained two-Track selection, removes the trigger, retries, recreates the Activity and verifies durable Archived placement.
- Earlier exact final behavior run before the copy-only refinement: `build/instrumentation-results-krkAQg`, 1/1. Focused production/repository neighbors: `build/instrumentation-results-0Qdgyj`, 15/15.
- Complete Tracks development profile: 73/73 JVM checks and 131/131 Android methods across three fresh batches, zero failures/skips/reuse; Android evidence `build/instrumentation-results-1XPw02`.
- Final `scripts/check --ready` passes in 2m6s with 385 routed JVM methods in 43 suites, Android-test compilation, static/catalog/asset checks, lint and debug packaging. It correctly records that a fresh frozen candidate remains required only at the final release boundary.
- Catalog lint: 525 required captures, zero pending/platform exceptions. The final 1080×2520 PNG/XML pair is retained here. Original-resolution review confirms complete failure copy, both selected rows, enabled retry actions, 48 dp controls, unclipped navigation and no `NAF` node. PNG SHA-256 `f08be9ea118c3f97dd591fb27446af0f741c088ea815cad8e4f0d4652ab3f89c`; XML SHA-256 `5c115d07d09e060bbba1e0190f6c7e84f7ee17b97ce80ca9f7eaf4fa4887b79f`.
- One initial test draft (`build/instrumentation-results-TdHTrU`) used a second drag while the reorder preview animation was still settling and timed out despite unchanged database truth. It was replaced by the same user-visible accessibility reorder action, which deterministically exercises the production callback. It is fixture evidence, not a product failure.

Current inventory at this checkpoint: 657 JVM + 1087 Android = 1744 product methods, plus 525 catalog states. No schema, data epoch, backup format, version, signed artifact, physical-phone operation or store action changed. Realistic large-history performance, specialized analytics, residual Tracks accessibility/design, the full catalog and final whole-product acceptance remain open.
