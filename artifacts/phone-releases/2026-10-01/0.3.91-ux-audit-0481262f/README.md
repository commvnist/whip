# Whip UX audit private phone update — installed2026-10-01

Nick explicitly requested installation on his phone. Built **only source `0481262f464d69c47a2a87438ea963d077cc32ed`** in a clean detached checkout; no version/source edits or unrelated working-tree changes were included. Version remains **0.3.91/code97**, as authored in the authorized source.

Installed in place on verified **Samsung SM-F976W**, package `commvne.com.whip.app`, through the established physical-release guard and `scripts/device release-install` (`adb install -r`). Existing pairing was already connected. Installed bytes and certificate exactly match the signed artifact. AppId10995 and first-install time2026-08-26 17:59:24 remain unchanged; no uninstall/data clearing or owner-record inspection occurred.

- Signed offline APK-only `:app:assembleRelease` succeeded in143.6s (Gradle2m23s), with normal release-vital lint/R8 packaging. No check/test/full-suite/candidate task or AAB ran. Initial isolated SDK-path setup failure was corrected by supplying the existing SDK in the process environment; its log remains local.
- APK: `build/releases/Whip-0.3.91-code97-ux-audit-0481262f.apk`; **4395458bytes**, non-debuggable. Package/version, certificate, archive integrity and installed-byte verification passed.
- APK SHA256: `03977e9d61afd92d2e77068236f53fd6a8eec383ae7bc3d059dc32fcd5bfc2e1`.
- Certificate SHA256: `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788`; same established identity as the installed before-APK and prior release.
- Minimal foreground launch: `Status: ok`, warm83ms, production MainActivity resumed. No physical instrumentation, further UI/record inspection, security changes or Play/public release.

Receipt facts accompany this file; local APK binaries are excluded from staging. The original dirty release-signature file and preexisting artifacts are preserved. The separately blocked implementation-report upload remains untouched. The full suite still requires Nick's separate explicit clearance. This receipt confirms installation/startup only.
