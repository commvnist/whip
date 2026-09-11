# Whip 0.3.71 (77) — verified signed build

Prepared September 11, 2026 under FB-20260911-004 / IMP, VER-20260911-005 from clean pushed source `0f2db6758c77e262b386c6760277757ec5ade351`. The owner subsequently confirmed the phone connection was down and explicitly requested forgoing installation. This is a verified build, not an installed release.

This update contains the [saved 5/3/1 supplemental editing fix](../saved-supplemental-edit/README.md): BBB/FSL edits apply across matching Standard, Leader or Anchor training weeks by default, with an explicit This Phase Only override. The next workout uses the saved sets and weights; other exercises and completed history remain intact. Existing routines require an explicit supplemental selection and Save.

All 389 fast JVM checks across 44 suites pass with zero failures/errors/skips. Signed optimized `./gradlew assembleRelease bundleRelease` passes in 1m8s using the established private signing configuration. Independent APK/AAB signature verification, package identity/version and both ZIP integrity checks pass. Of 514 previously verified inputs, only app/build.gradle.kts changes for version 0.3.71/code 77; all other 513 hashes match. Room 46, data epoch 6 and backup 26 remain unchanged. Existing feature Android/readiness evidence remains applicable; no full candidate or Android rerun was needed for this private build.

| Artifact | SHA-256 |
| --- | --- |
| APK, 4,166,194 bytes | `f1568a9419336ad2c4cf8d3bffe4752702f763c6ad7ee9658808fde1b41f0057` |
| AAB, 11,237,021 bytes | `96bdf49015a62b701ef4425f827cbdf2db61e8dd7b020dda1f02762bfcb97010` |
| APK signing certificate | `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788` |

ADB listed no authorized device; its discovered wireless debugging endpoint refused connection through both installed clients, including after restarting the empty ADB server. After being asked to reconnect, the owner replied “it's down right now, just forgo that part.” Phone installation is therefore Deferred at owner direction. No install or startup check occurred; the last verified installed version remains 0.3.70/code 76. No uninstall, clear, reset, owner-record extraction or store publication occurred. No connection monitoring or automatic follow-up is scheduled.

`build-receipt.json` and supporting logs retain build/signature evidence only. The signed APK remains at `app/build/outputs/apk/release/app-release.apk`; compare its hash above before any future requested installation. Later delivery requires the physical target guard, captured prior install identity, guarded in-place installation and independent artifact/startup checks.
