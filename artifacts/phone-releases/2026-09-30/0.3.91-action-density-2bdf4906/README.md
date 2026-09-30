# Signed compact-action phone update

Installed and verified on Samsung SM-F976W, Android API37, using the established signed in-place workflow. Source: `2bdf490639a7a81f29730bb6389b63abe443b590`; version **0.3.91 / code97**; package `commvne.com.whip.app`.

The final consultant accepted compact equal sibling controls, side-by-side phone rows, preserved checkbox spacing and readable native200% labels. [Before/after critique](../../../action-sizing/2026-09-30/design-density/README.md) and [focused validation](../../../action-sizing/2026-09-30/content-height/README.md) cover this source. The previous 80dp reserve is superseded.

- Signed APK: `/tmp/whip-phone-release-20260930-action-density-2bdf4906/build/releases/Whip-0.3.91-code97-action-density-2bdf4906-private.apk`
- APK SHA256: `c2eb5d5c519f1ff26085d4de705c1c2ae96610b317ea50fea859061287642071` (4,378,390 bytes).
- Existing, candidate and installed certificate SHA256: `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`.

A clean isolated checkout at the exact source built successfully in143.535s. Its264 production inputs and tracked checkout remained unchanged; package/version, non-debuggable status, signature, archive CRC and16KB alignment passed. No tests were rerun for release.

The authorized update completed via `scripts/device release-install` (`adb install -r`) in2.706s at23:08UTC. An independently pulled installed baseAPK matches the signed hash and certificate. Application identity, first install time, data directory and notification permission are preserved. `scripts/device release-run` confirmed MainActivity foreground; a three-second process-scoped startup check found no matching crash, database or startup error signatures.

[Machine receipt](installation.json), [provenance](source-manifest.json), [installation](install.log), [launch](launch.log) and [startup check](startup-check.json) retain the direct evidence. No uninstall, data clearing, owner-record inspection, phone fixture action or public release occurred. The APK and wireless endpoint remain private.
