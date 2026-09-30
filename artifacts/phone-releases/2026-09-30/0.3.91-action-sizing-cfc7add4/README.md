# Private phone update: equal activity sizes

Source `cfc7add4fc484a2388247eeea9d16cc8f6e20e34` installed successfully on the authorized Samsung SM-F976W before Nick's subsequent side-by-side correction arrived. This records that completed update; its fixed-width design acceptance is superseded by the correction.

- Package `commvne.com.whip.app`, version **0.3.91 / 97**.
- Signed APK: `/tmp/whip-phone-release-20260930-action-sizing-cfc7add4/build/releases/Whip-0.3.91-code97-action-sizing-cfc7add4-private.apk`.
- APK and independently pulled installed bytes: SHA256 `6f854badba975554c325722708486e38ac3c8dec32f0fb66f02f68fed48eba48`.
- Existing, candidate and installed certificate: SHA256 `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`.
- Isolated exact-source build passed in 114.716s; 264 production inputs and tracked checkout unchanged. Package, signature, non-debuggable status, archive CRC and 16KB alignment verified.
- Established `scripts/device release-install` completed an in-place `adb install -r` in 2.805s. App ID, first-install time, data directory and notification permission matched before/after.
- `scripts/device release-run` confirmed MainActivity foreground. Process survived the three-second check with zero matching crash, database or startup error signatures.

[Machine-readable verification](installation.json), [source manifest](source-manifest.json), [before](installed-before.json), [after](installed-after.json), [signature](signature-installed.txt), [install](install.log), [launch](launch.log).

No tests rerun for this update; no uninstall, data clearing, owner-record inspection or public release. Metadata equality verifies update continuity, not a full data-content audit.
