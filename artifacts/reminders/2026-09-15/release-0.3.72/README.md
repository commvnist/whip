# Whip 0.3.72 private owner-phone release

## Outcome

- Status: Released on 2026-09-15.
- Package: `commvne.com.whip.app`
- Version: `0.3.72` / code `78`
- Exact application source: clean pushed `71c9e7f257852339e5abbe1ea4ae4f1b0ed64030` on `origin/main`
- Device: physical Samsung SM-F976W, Android API 37; the target guard reported `ro.kernel.qemu=0`.

## Build and verification

- `scripts/check --ready` passed all 661 JVM methods, Android-test compilation, lint and debug packaging before the source commit.
- The signed `assembleRelease bundleRelease` build completed from the clean pushed source.
- APK: 4,166,322 bytes; SHA-256 `029d2a3db8010768bef945c4846e92a3c6351969729d000d8c59d4f7f3b05e13`.
- AAB: 11,245,877 bytes; SHA-256 `b795dbee13aaf6afd4c174a99fd1f225b9d8369b033fde924e945fa1a31a8440`.
- APK v2 verification and bundle signature/ZIP integrity passed. APK signer certificate SHA-256 is `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`.
- Reminder behavior evidence remains in `VER-20260915-001`: exact cold receiver wakeup without an Activity, denied-access fallback, five independent minute-spaced Habit claims, late-successor preservation and 31 neighboring Android reminder/recovery tests pass on a disposable emulator.

## In-place installation

- Pre-install version was `0.3.70` / code `76`; installed APK SHA-256 was `cb022c6b40e5bc418efd6ac023e2157b7108d1a1ad550218c3f57f2f66c34faf`.
- Guarded streamed `install -r` returned `Success`; no uninstall, clear, reset, downgrade or physical-device instrumentation occurred.
- The installed base APK SHA-256 exactly matches the signed 0.3.72 APK.
- `firstInstallTime=2026-08-26 17:59:24` is unchanged; final `lastUpdateTime=2026-09-15 22:23:17`.
- Android precise-alarm access is explicitly `allow`; notification permission remains granted. Task, Habit and Goal reminder channels remain enabled at importance 3 with notification sound configured.
- MainActivity reached the foreground in a live process; the measured non-forced launch completed in 66 ms. The package is not stopped, and the bounded PID-scoped scan found zero fatal, ANR, Room, SQLite, startup or reminder-reconciliation error matches.

## Boundaries

- The launch deliberately avoided a force-stop after exact-alarm reconciliation because Android cancels app alarms on force-stop. No owner records were read or changed by the release checks.
- This is a private owner-phone release, not Play Store candidate qualification. Very closely spaced alarms can still be throttled during Android deep idle; Whip retains independent upcoming claims and a WorkManager fallback rather than claiming a platform-impossible absolute guarantee.
