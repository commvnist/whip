# Whip polish — private phone update installed

Installed in place on Samsung **SM-F976W**, package `commvne.com.whip.app`, **0.3.91/code97**, from pushed source [b061de4310d72466a1636d80414e35131f6a0e5a](https://github.com/commvnist/whip/commit/b061de4310d72466a1636d80414e35131f6a0e5a). Nick explicitly requested “And release to my phone.”

- Fresh signed release build from an isolated, clean checkout of that exact commit passed in **2m37s** (`:app:assembleRelease --max-workers=2`). Tracked production source hashes verified unchanged after build. No tests were rerun for release.
- Existing installed, candidate and installed-after certificates match `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`. Package/version/non-debuggable checks, v2 signature, 16KiB alignment and archive CRCs pass.
- Existing `scripts/device release-install` performed one `adb install -r` and returned **Success**. Installed bytes exactly match signed APK SHA256 **58090a71444541e145108e85a80aa6c97bc237a0f429763bf323342c4cc86e07**.
- App identity, first-install time, data directory and notification permission remain unchanged. Owner records were not inspected. No uninstall, data clearing, security-setting changes or public release.
- Existing `scripts/device release-run` reached the resumed foreground activity. Three-second process-scoped startup inspection found **zero** crash/database/startup signatures. This is a minimal launch check, not phone UX or fold certification.

Signed APK retained locally at `/tmp/whip-phone-release-20260930-polish-b061de43/build/releases/Whip-0.3.91-code97-polish-b061de43-private.apk`. [Exact installation facts](installation.json), build/source/signature/installation/launch evidence are retained here; connection addresses are redacted. [Focused polish evidence](../../../ux-polish/2026-09-30/README.md): 137 JVM tests and 11 distinct native methods passed in phases; no full-suite rerun. Earlier failed attempts and metric limits remain documented.

Two helper setup errors were corrected before their dependent actions: missing non-login-shell rg before build, and adb shell timestamp quoting before launch. Neither caused another installation or test run. The preexisting 2026-09-29 AAB receipt edit remains untouched (SHA256 `05e34843fb7926a88d04b25e3c4f0e926e9eb85a1f5f3a84d85a93f858e6adbe`). APK binaries and signing secrets remain local.
