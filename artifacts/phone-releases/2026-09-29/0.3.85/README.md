# Whip 0.3.85 private phone update — 2026-09-29

**Released; packaging, installation and launch verified.** Includes the [shared-search/design correction](../../../../docs/quality/SEARCH_DESIGN_REMEDIATION_2026-09-29.md) and observable-locale repair. The full test campaign and fresh behavioral/rendered verification remain paused at the owner's request.

- [Signed APK](../../../../build/releases/Whip-0.3.85-code91-private.apk): `commvne.com.whip.app`, **0.3.85/code 91**, SHA-256 `479250aa999970b509ede52a91711c2423ce9a9c483f87d4f9c5e37a355514bb`.
- [Signed AAB](../../../../build/releases/Whip-0.3.85-code91-private.aab): SHA-256 `af071b1208074c58e432abec08a03bb2bfea1bc7b6029aaca7478febc2c36bfe`.
- Signed `./gradlew assembleRelease bundleRelease --console=plain` passed in **160.056 seconds**, including release-vital lint and R8. Packaging compiles production source; updated JVM, Android and benchmark fixtures remain uncompiled/unexecuted after the pause. No full, readiness, candidate or behavioral batch was run.
- Individually bounded artifact commands verify package/version/code, non-debuggable APK, APK/AAB signatures, ZIP integrity and established certificate SHA-256 `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`. AAB self-signed/timestamp/archive-reading warnings remain in the original signature receipt.
- Physical-target guard selects Samsung `SM-F976W`. Guarded `scripts/device release-install` upgrades **0.3.84/code 90 → 0.3.85/code 91** in place in **3.316 seconds**. Installed APK hash matches the signed artifact. First install `2026-08-26 17:59:24` and app ID `10995` are unchanged; update time `2026-09-29 08:34:47`.
- Guarded `scripts/device release-run` verifies foreground MainActivity after a **110 ms cold launch**. Read-only bounded smoke finds a live process, 502 sampled log lines, zero startup-error matches and no process crash-buffer payload. Private records and phone screenshots were not collected.
- [Production source hashes](production-source-sha256.json) cover 257 tracked source/build files. This build used the working tree over `fd0925e2`; [source.txt](source.txt) records the subsequent source checkpoint. Earlier accepted source/test hashes do not certify these changes.
- No reset, uninstall, downgrade, phone instrumentation, storage-contract change or Play publication. Room 46, epoch 6 and portable-backup format 26 remain unchanged. UI discovery/catalog reconciliation, behavioral acceptance and subjective appearance remain outstanding. The transient phone address is excluded from committed evidence.

Exact build, artifact, sanitized install/launch and before/after identity receipts accompany this report. `SHA256SUMS` covers receipt files except itself. FB-20260929-006 / IMP-20260929-008 / VER-20260929-006.
