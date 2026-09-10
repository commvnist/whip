# Keep 5/3/1 values with their Exercise — 2026-09-10

FND-20260910-028 / DEC, IMP and VER-20260910-019. Starting production: `7142f5b8`. API 34 phone and API 37 wide at normal scale; no release or physical-device operations.

Adding an Exercise after reordering a custom program previously restored library order while leaving Training Max fields in their old positions. Bench Press received Squat's 200 kg TM, Squat received Bench's 100 kg, and authored increases of 4 and 7 reset to defaults. The baseline reproduces the setup corruption before saving; it does not demonstrate completed History corruption.

The existing guided setup now stores one complete immutable configuration per Exercise. Reordering, removal and library/schedule reconciliation keep TM, actual/e1RM basis, percentage, applied derivation, authored increase and valid BBB target together. New identities get fresh state. Untouched increase suggestions follow role/name/unit; reconfirming an existing choice preserves authored values. Supplemental choices also use Main Exercise identity while preceding incomplete configurations are omitted from the preview. Existing shared layout builders, program generation and repositories remain; no schema, backup, version or history migration.

## Verification

- `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile gym531 --emulator`: 83 JVM checks in six suites and 86 fresh Android checks, zero failures/errors/skips; Android run `gGnYbt`, one batch, zero reused checks.
- API 37 direct instrumentation: three methods pass in 87.414 seconds. The full native authoring journey, incomplete-setup supplemental mapping, and arbitrary custom Exercise ordering use the same source, tests and APKs as the final phone run. The exact class/method stream is in `wide.log`.
- `scripts/check --ready`: passes in 2m42s, including 116 JVM checks in nine suites, zero failures/errors/skips, Android compilation, lint, debug packaging and static/harness checks. All 510 regression input hashes remain unchanged.
- `scripts/ui-catalog lint`: 497 required states, zero pending selectors or platform exceptions. Source inventory: 653 JVM + 1080 Android = 1733; these are source counts, not executed totals.

The MainActivity journey authors Bench 100/increase 4 and Squat 200/increase 7, reorders Squat first, creates and adds Paused Press, reconfirms Squat and recreates the Activity. It verifies the saved three-day order and values, starts the scheduled Squat workout with 130 kg from its 200 kg TM, completes all eight generated Sets, advances the next day and checks exact performed-Set equality in History. Five pure state tests separately protect complete derivation/provenance and supplemental ownership, removed/new identities, authored versus suggested increases and serialization. An additional UI regression selects BBB after Bench while earlier Squat is incomplete, then builds and verifies the actual supplemental Exercise and TM.

## Original review and limits

Ten native PNG/XML pairs were personally reviewed: two before states on unchanged phone production and four final states on each phone/wide. Authored values remain readable in existing stacked/paired fields; scheduled load and performed History are correct. PNG bytes are original. Readable XML and text reports normalize trailing whitespace; accompanying gzip archives preserve original bytes. Manifests record native filenames, run identities and original/artifact hashes.

This increment fixes configuration integrity. Repeated setup guidance, verbose generated workout/History copy, shared wide reading measure and full-cycle execution/review still need substantive design work. Whole-app consistency and final comprehensive acceptance remain open. No new process-death, RTL, TalkBack or 200% acceptance is claimed.

## Retained intermediate evidence

- `IGFuCQ`: one expected product failure reproduces the original configuration transfer. Both baseline states are retained.
- `LK5vgp`: repaired values pass after library creation; the new fixture then seeks exact `Squat`, while the existing picker renders `Squat · Current selection`. Correcting that selector changes no production behavior.
- `WAbeI5`: the corrected complete native journey passes. The final profile also includes the later incomplete-setup BBB regression and the final fixture names.
- The first focused run passes 40 JVM checks. Final profile/readiness reports supersede it for acceptance; their counts are reported separately rather than summed as unique coverage.
