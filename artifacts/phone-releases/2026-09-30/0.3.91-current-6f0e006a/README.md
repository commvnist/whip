# Whip current improvements — private phone update installed

The user explicitly approved current-build installation while full regression was unfinished and requested a separate phone release agent. Source 8e8662b6095f65bb6eaa47228e300da1cb9154c3 contains app fixes 6f0e006abf333ee1725b42c64826b8b9f361de35. Subsequent uncommitted campaign repairs are excluded.

**Installed in place on Samsung SM-F976W**, package commvne.com.whip.app. Version remains **0.3.91/code97**, explicitly accepted for this delivery. These are new bits: prior installed hash d117083b90eaaa1bcf0a39879a8b46eec7ec58c8ca7651f171a72e7d00e7f3dc; current installed hash **3984fd178d4b95b4b1e4d0a124a2953e9ed8c286a443594e2458afef40164c08**.

- Fresh signed APK built in this separate checkout in 2m47s, all 54 tasks executed, release-vital lint included. Two workers; no shared app/build edits or emulator commands. Main source/version unchanged by release work.
- Package/version/non-debuggable output, APK v2 signature, 16KiB alignment and archive integrity verified. Installed bytes exactly match candidate. Installed before/after certificate matches cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788.
- Existing scripts/device release-install reported Success via adb install -r and matched installed hash. AppId 10995, firstInstallTime 2026-08-26 17:59:24, data directory and notification grant remain unchanged. These are preservation indicators; owner database records were not inspected.
- Existing scripts/device release-run cold foreground launch passed in 135ms. Three-second process-scoped startup inspection found zero crash/database/startup signatures. No phone fixtures, instrumentation, app-data clear, uninstall, signing changes, public release or third-party upload.
- Frozen source manifest matches checkout. Database schema 46 and backup version 26 unchanged.
- Binary retained locally at build/releases/Whip-0.3.91-code97-current-6f0e006a-private.apk. [Installed facts](installation.json), before/after metadata, signatures, build, install and launch receipts retained here. Connection addresses redacted.
- Full native regression has not passed; parent reported five failures with repairs continuing. This receipt certifies only the authorized private build, compatible update and startup scope.

The preexisting modified 0.3.91 AAB receipt in main remains unchanged, SHA256 05e34843fb7926a88d04b25e3c4f0e926e9eb85a1f5f3a84d85a93f858e6adbe. This receipt is isolated for parent integration.
