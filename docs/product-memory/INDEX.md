# Whip durable product memory

Current snapshot: **2026-09-27**. This page is the entry point, not a replay of every dated checkpoint. Historical outcomes and exact test scopes remain in the canonical ledgers, linked audit records, release receipts and Git history.

## Product and verification state

- **Latest private owner-phone release:** Whip **0.3.75/code 81**, built from clean pushed source `0db95f77` and installed in place on the selected Samsung. The installed APK matches the signed candidate SHA-256 `43e5806b7f7f1d1d46caa2e72dc2b1a4fdfbc8f3890e7cae9886d5d55c5177f3`. Signer and first-install identity, cold launch, foreground process and bounded runtime checks passed. See [VER-20260927-011](VERIFICATION.md) and the [release receipt](../../artifacts/design-consistency/2026-09-27/phone-release-0.3.75/README.md). There was no reset, schema/backup-format change or Play publication.
- **Latest completed design scope:** Eight confirmed consistency issues were resolved using shared layout, heading, warning, navigation and destructive-action owners. The final [design overhaul](../quality/DESIGN_CONSISTENCY_OVERHAUL_2026-09-26.md) passed the 523-state visual campaign and an exact frozen candidate with **659 JVM + 1,113 fresh Android methods**, zero failures/skips/reuse, coverage, lint, signed outputs and evidence checks. See [VER-20260927-004/010](VERIFICATION.md). The earlier [reusable architecture/UX audit](../quality/REUSABLE_ARCHITECTURE_UX_AUDIT_2026-09-26.md) also completed within its recorded scope.
- **Data contract:** Room schema 46, data epoch 6 and portable-backup format 26. The design release changed no persisted domain format. Normal-use phone appearance awaits owner judgment; spoken TalkBack output after the latest design change was not measured.

## Current work and boundaries

- [Ponytail Ultra next-work plan](../quality/NEXT_WORK_PONYTAIL_ULTRA_2026-09-27.md): one current Track copy ambiguity and two bounded verification gaps. All are **Proposed**; this documentation cleanup does not implement them or claim new app tests. See the [quality plan index](../quality/README.md) to distinguish active, completed and paused records.
- The separate [whole-product audit continuation](../quality/ASTRA_CONTINUATION_2026-09-20.md) is **paused at owner request** under FB-20260921-001. Its frozen unfinished review rows are not confirmed defects. The later design-specific catalog and full candidate do not retroactively satisfy that broader audit's journey, platform, accessibility and performance contract. The [September 10 goal closeout](../quality/ASTRA_GOAL_CLOSEOUT_2026-09-10.md) also stays closed.
- No new app change or phone update is needed for this documentation-only task. A future product issue starts with a current reproduction or explicit owner feedback, then the smallest suitable existing owner and proportionate evidence.

## Read order

1. Read this snapshot and the relevant [owner feedback](USER_FEEDBACK.md).
2. Follow the issue IDs through [findings](FINDINGS.md), [decisions](DECISIONS.md), [implementation](IMPLEMENTATION_LOG.md) and [verification](VERIFICATION.md).
3. Read the linked focused audit/receipt and verify its dated claims against current source before acting. [Architecture](../architecture.md) and [testing](../testing.md) describe current shared owners and test commands.

## Memory contract

Use stable `FB|FND|DEC|IMP|VER-YYYYMMDD-NNN` IDs. Keep dated evidence and superseded reasoning in their ledgers. For substantial work, record feedback before changes, distinguish compilation from executed checks, update exact implementation/verification status, and commit/push each coherent verified chunk. Keep this index short and current.
