# Whip durable product memory

Current snapshot: **2026-09-27**. This page is the entry point, not a replay of every dated checkpoint. Historical outcomes and exact test scopes remain in the canonical ledgers, linked audit records, release receipts and Git history.

## Product and verification state

- **Latest private owner-phone release:** Whip **0.3.76/code 82**, built from clean pushed source `7f3b510d` and installed in place on the selected Samsung. The installed APK matches SHA-256 `2492f10c150ce36f3f5c1c40c755e7cd29b83a0135f719b9385b45a0fe99b9c3`. Signer and first-install identity, cold launch, foreground process and bounded runtime checks passed. See [VER-20260927-014](VERIFICATION.md) and the [release receipt](../../artifacts/quality/2026-09-27/phone-release-0.3.76/README.md). There was no reset, schema/backup-format change or Play publication.
- **Latest completed design scope:** Eight confirmed consistency issues were resolved using shared layout, heading, warning, navigation and destructive-action owners. The final [design overhaul](../quality/DESIGN_CONSISTENCY_OVERHAUL_2026-09-26.md) passed the 523-state visual campaign and an exact frozen candidate with **659 JVM + 1,113 fresh Android methods**, zero failures/skips/reuse, coverage, lint, signed outputs and evidence checks. See [VER-20260927-004/010](VERIFICATION.md). The earlier [reusable architecture/UX audit](../quality/REUSABLE_ARCHITECTURE_UX_AUDIT_2026-09-26.md) also completed within its recorded scope.
- **Latest bounded plan:** The three [Ponytail Ultra items](../quality/NEXT_WORK_PONYTAIL_ULTRA_2026-09-27.md) are completed. Track's weekly-rate label now names its 30-day window; one real TalkBack warning defect was fixed in the shared notice Card; a pinned Count/Timer widget journey passed through process recreation. [VER-20260927-013](VERIFICATION.md) links rendered, spoken and exact saved-state evidence.
- **Data contract:** Room schema 46, data epoch 6 and portable-backup format 26. The update changed no persisted domain format. Normal-use phone appearance awaits owner judgment; focused TalkBack speech was checked on one API 34 configuration.

## Current work and boundaries

- A default-on Goal completion celebration is at concept stage under [FB-20260927-003](USER_FEEDBACK.md); the owner is reviewing visual directions. No application change has been made for it.
- The [Ponytail Ultra plan](../quality/NEXT_WORK_PONYTAIL_ULTRA_2026-09-27.md) is **Completed**. See the [quality plan index](../quality/README.md) to distinguish completed and paused records.
- The separate [whole-product audit continuation](../quality/ASTRA_CONTINUATION_2026-09-20.md) is **paused at owner request** under FB-20260921-001. Its frozen unfinished review rows are not confirmed defects. The later design-specific catalog and full candidate do not retroactively satisfy that broader audit's journey, platform, accessibility and performance contract. The [September 10 goal closeout](../quality/ASTRA_GOAL_CLOSEOUT_2026-09-10.md) also stays closed.
- A future product issue starts with a current reproduction or explicit owner feedback, then the smallest suitable existing owner and proportionate evidence.

## Read order

1. Read this snapshot and the relevant [owner feedback](USER_FEEDBACK.md).
2. Follow the issue IDs through [findings](FINDINGS.md), [decisions](DECISIONS.md), [implementation](IMPLEMENTATION_LOG.md) and [verification](VERIFICATION.md).
3. Read the linked focused audit/receipt and verify its dated claims against current source before acting. [Architecture](../architecture.md) and [testing](../testing.md) describe current shared owners and test commands.

## Memory contract

Use stable `FB|FND|DEC|IMP|VER-YYYYMMDD-NNN` IDs. Keep dated evidence and superseded reasoning in their ledgers. For substantial work, record feedback before changes, distinguish compilation from executed checks, update exact implementation/verification status, and commit/push each coherent verified chunk. Keep this index short and current.
