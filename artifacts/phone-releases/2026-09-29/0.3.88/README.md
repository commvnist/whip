# Whip 0.3.88 private phone update — 2026-09-29

FB-20260929-012 releases the Tasks UX correction from pushed product commit `90ab1bdeacb23502b6064ac39c76734fd6cc24a0` (IMP-20260929-015). Version advances to 0.3.88/code 94.

- Signed `./gradlew assembleRelease bundleRelease --console=plain` passes in 105.934 seconds, including R8 and release-vital lint.
- APK SHA-256: `1850a2fa602e4a69010be2a388756a0507ec0493bc2b336e7dd34a8b77b1e49e`.
- AAB SHA-256: `c7a832624924e42cbef3915f958837eb8bb4bec5e2e503fa7f460ec7a9f1aca0`.
- Package/version/code, non-debuggable APK, APK/AAB signatures, ZIP integrity and established signing certificate `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788` pass. Application/test source hashes are unchanged; only release metadata advances.
- Guarded `scripts/device release-install` upgrades Samsung SM-F976W in place from 0.3.87/code 93. Installed APK hash matches exactly; app identity and original first-install time are preserved.
- `scripts/device release-run` cold-launches MainActivity in 145 ms and confirms foreground activity. Bounded process smoke samples 168 log lines, zero startup-error matches and zero crash-payload lines.

Product verification remains [VER-20260929-012's focused evidence](../../../tasks-ux/2026-09-29/README.md), including its explicitly incomplete longer keyboard E2E. No repeated full suite, phone instrumentation, reset, private-record inspection or Play publication. Schema 46, epoch 6 and backup format 26 remain unchanged. Subjective appearance awaits owner use. IMP-20260929-016 / VER-20260929-013.
