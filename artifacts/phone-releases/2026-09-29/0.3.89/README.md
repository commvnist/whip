# Whip 0.3.89 private phone release — 2026-09-29

FB-20260929-015 explicitly authorizes repository push and phone release. Product overhaul `9a9020b9` and release source `86c166cb204b8b639842d736df4f90c5df1ecc0e` are pushed to `origin/main`. This release includes all 24 accepted overhaul remedies and the previously verified recurring Task identity correction.

- `WHIP_DEVICE=<selected physical phone> scripts/device release-deploy` passes its target guard, clean changed-input fast route, signed `assembleRelease bundleRelease`, in-place installation, installed-byte comparison and cold foreground launch. Gradle reports **1m41s**. The clean route reruns no tests; product acceptance remains the [450 affected JVM / 39 selected native methods](../../../fresh-app-overhaul/2026-09-29/README.md), plus VER-20260929-014's Task identity boundaries.
- Independent package checks confirm `commvne.com.whip.app`, **0.3.89/code 95**, minSdk 26, targetSdk 37 and a non-debuggable APK. APK v2 signature, AAB JAR signature, both ZIP integrity checks and established certificate `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788` pass. Standard AAB self-signed/no-timestamp certificate warnings are retained in `aab-signature.txt`.
- APK SHA-256: `9879ba968aabd099066c889e17294f8a304f3a4b5c166f7b8bc82cc44894f8ca`; 4,326,450 bytes.
- AAB SHA-256: `98c6300e881e21a0c754e25a870cf1e431abf9658aad976d504d8d5ebb3a6c0a`; 11,673,494 bytes.
- Samsung **SM-F976W** is updated from 0.3.88/code 94. Independent installed APK hashing matches the signed artifact. `appId=10995` and original `firstInstallTime=2026-08-26 17:59:24` remain unchanged; installation uses `-r`, without reset/uninstall.
- Cold foreground MainActivity launch passes in **127 ms**. The process remains running; bounded process-only smoke samples 252 log lines with zero startup-error matches and zero crash-payload lines. Raw runtime content remains local rather than copied into the repository.
- All **547 accepted application/test/resource files** retain their overhaul hashes; only release metadata advances. Schema 46, epoch 6 and backup format 26 remain unchanged.

Exact sanitized build/deploy, package, signing, installed bytes, source and smoke facts accompany this receipt. No full suite, candidate, phone instrumentation or Play publication occurred. Appearance awaits owner use. IMP-20260929-019 / VER-20260929-016.
