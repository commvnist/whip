# Whip 0.3.87 private phone update — 2026-09-29

Second release requested by FB-20260929-009, after the complete verification/repair campaign. Includes `21f01933` (IMP-20260929-012/013) and collection-first Tasks/Habits defaults from FB-20260929-010. Version advances to 0.3.87/code 93.

- Signed `./gradlew assembleRelease bundleRelease --console=plain` passes in 99.660 seconds, including R8 and release-vital lint.
- APK SHA-256: `80f956346f1d9b5f84ace88fd6ed991e4669bb0d97bf73ec7f3ab46c203631cc`.
- AAB SHA-256: `ff88b7c19ed569d112407e44acfb80329142056c32ef31d560ead1087060dcf8`.
- Artifact checks verify package/version/code, non-debuggable APK, APK/AAB signatures, ZIP integrity and the established certificate `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`.
- Guarded in-place installation upgrades Samsung SM-F976W from 0.3.86/code 92. Installed APK hash matches the signed artifact; original app identity and first-install time are preserved. MainActivity launches successfully. Bounded smoke observes a running process, 135 sampled log lines, zero startup-error matches and zero crash-payload lines.
- Application/test source hashes remain unchanged from the accepted campaign; only release version metadata changes. [Full verification evidence](../../../full-suite/2026-09-29/README.md): 724 JVM methods, all 1,268 Android methods across inventory plus repair replays, nine benchmark/profile scenarios, six harness fixtures, catalog, lint, coverage and builds pass.

No phone instrumentation, reset, private-record inspection or Play publication. Schema 46/epoch 6/backup 26 are unchanged. Subjective appearance awaits owner use. Sanitized receipts and source hashes accompany this report; copied log whitespace is normalized for repository review. IMP-20260929-014 / VER-20260929-011.
