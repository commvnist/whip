# Whip 0.3.91 private update — installed

FB-20260929-019 requests release first, then extensive QA/UX/design and SDLC research. Product source `b8c6495e` was already pushed to `origin/main`; IMP-20260929-024 advances metadata to **0.3.91/code97**. Prior product evidence is [430 affected JVM and18 selected native passes](../../../direct-execution/2026-09-29/README.md); its full readiness/lint limitation remains explicit.

**Installed in place on Samsung SM-F976W.** Exact installed APK SHA256 matches the signed artifact. AppId10995 and original first-install time remain unchanged. Cold foreground launch takes131ms and process-only startup inspection finds zero crash/startup signatures. No physical instrumentation, data reset, owner-record inspection or Play publication.

- Fast affected check passes in25s. Signed offline `:app:assembleRelease :app:bundleRelease` passes in1m58s, including release-vital lint. All555 accepted app/test/resource hashes from the direct-execution source remain unchanged.
- Independent APK verification confirms the established release certificate, `commvne.com.whip.app`, version0.3.91/code97, minSdk26/targetSdk37 and non-debuggable output. APK signature, AAB JAR signature and both archive integrity checks pass. Private self-signed/no-timestamp warnings are ordinary and retained.
- APK SHA256: `d117083b90eaaa1bcf0a39879a8b46eec7ec58c8ca7651f171a72e7d00e7f3dc`; 4,342,834bytes. Local retained binary: `build/releases/Whip-0.3.91-code97-private.apk`.
- AAB SHA256: `a3bb486671d58ef3db46b7c2265518bb66cb9339bf92e994cd9acdc15b203efd`; 11,721,796bytes. Local retained binary: `build/releases/Whip-0.3.91-code97-private.aab`.
- [Installed facts](installation.json), before/after identity, package/signature, build/installation and launch receipts accompany this record. Device connection addresses are omitted from retained logs. Schema46, epoch6 and backup26 are unchanged.

VER-20260929-021 accepts this private installation/startup scope. Extensive QA begins afterward; this release is not an absolute bug-free, full-suite or Play-candidate claim. Later QA fixes require their own delivery decision.
