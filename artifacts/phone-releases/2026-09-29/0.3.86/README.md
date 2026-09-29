# Whip 0.3.86 private phone update — 2026-09-29

Released as the first stage of FB-20260929-009, before resuming the full test campaign. Includes the navigation redesign from `eb8dcda8`; version advances to 0.3.86/code 92. This installation does not claim behavioral acceptance.

- Signed `./gradlew assembleRelease bundleRelease --console=plain` passes in 97.345 seconds, including R8 and release-vital lint.
- APK SHA-256: `bd4bdae75d0a53698b17907e09de059acac11e9fe95fa0c2684df68250a23c8f`.
- AAB SHA-256: `f5618504b0485eecfdafa5ec35d8b538cbcd14082c40d4284b9357cd5242e0f0`.
- Artifact checks confirm application ID, version, non-debuggable APK, ZIP integrity and established APK/AAB certificate `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`. Signing-tool warnings remain in the receipts.
- Guarded in-place install upgrades Samsung SM-F976W from 0.3.85/code 91. Installed base hash matches the signed APK; original app ID and first-install time remain unchanged. Guarded launch reaches MainActivity; smoke records a running process, 135 sampled log lines and zero startup-error or crash-payload lines.
- Source hashes, build, signatures, sanitized installation, before/after identity and launch receipts accompany this report. Transient phone address and private records are excluded. No reset, phone instrumentation or Play publication. Domain schema 46/epoch 6/backup 26 remain unchanged.

The full test campaign and repairs follow this release. Final post-test release remains outstanding. IMP-20260929-011 / VER-20260929-009.
