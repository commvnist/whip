# Whip 0.3.84 private phone update — 2026-09-29

**Released and device verified.** Includes all 16 groups in the [deep product review](../../../../docs/quality/DEEP_PRODUCT_REVIEW_2026-09-29.md). Clean pushed source `3ba7a50c` on origin/main; full revision in [source.txt](source.txt).

- [Signed APK](../../../../build/releases/Whip-0.3.84-code90-private.apk): `commvne.com.whip.app`, **0.3.84/code 90**, SHA-256 `83a07bb042e4bdabf0c48e49f13f96df0fe2f4ffd3e5234ed98f3413f211384b`.
- [Signed AAB](../../../../build/releases/Whip-0.3.84-code90-private.aab): SHA-256 `13bfd6600b858dceb115cf286ab3a7d8b8361bd7a0b2468f575374ed5f03dae3`.
- Signed `./gradlew assembleRelease bundleRelease` passed in **161.164 seconds**, including release-vital lint and R8. This is packaging time, not a test run. All 493 accepted Kotlin source/test hashes match VER-20260929-002; only version metadata changes. Prior acceptance remains nine distinct JVM and 30 distinct native methods; no repeated full/readiness/candidate or behavioral batch.
- Artifact checks individually bounded at 55 seconds verify metadata, non-debuggable release, both ZIPs and established APK/AAB certificate SHA-256 `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`. Ordinary AAB self-signed/timestamp/archive-reading warnings remain in the signature receipt.
- Physical-target guard selected Samsung `SM-F976W`. Guarded `scripts/device release-install` upgraded **0.3.83/code 89 → 0.3.84/code 90** in place in **2.634 seconds**. Installed APK hash equals the signed artifact. First install `2026-08-26 17:59:24` and app ID `10995` are unchanged; update time `2026-09-29 07:27:56`.
- Guarded `scripts/device release-run` verifies foreground MainActivity after a **139 ms cold launch**. Bounded read-only smoke finds a live process, 139 sampled log lines, zero startup-error matches and no crash-buffer payload for that process. Private records and phone screenshots were not collected.
- No reset, uninstall, downgrade, phone instrumentation, storage-contract change or Play publication. Room 46, epoch 6 and portable-backup format 26 are preserved. Subjective appearance awaits owner use. The existing connection worked; its transient address is excluded from committed evidence.

Exact build, signature, artifact metadata, sanitized install/launch and before/after identity receipts accompany this report. `SHA256SUMS` covers receipt files except itself. FB-20260929-003 / IMP-20260929-005 / VER-20260929-003.
