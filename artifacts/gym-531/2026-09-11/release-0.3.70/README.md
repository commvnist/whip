# Whip 0.3.70 (76) — private phone release

Released September 11, 2026 under FB-20260911-002 / IMP, VER-20260911-003 from clean pushed source `1724f50a5f5f8425ad5e29db76bc05423dfa7e0d`.

This update delivers independent 5/3/1 Supplemental Work choices for each main exercise: Bench can use BBB while Zercher Deadlift uses FSL. BBB percentages and Leader/Anchor choices are independent. [Feature and emulator evidence](../per-exercise-supplemental/README.md) records the exact two-day save/reopen/recreation/workout journey and neighboring checks. Of its 514 verified inputs, only app/build.gradle.kts changes to version 0.3.70/code 76; the other 513 hashes match.

`scripts/check` passes 387 JVM methods across 44 suites with zero failures/errors/skips. `WHIP_DEVICE=<selected-owner-phone> scripts/device release-deploy` passes the fast affected gate, signed optimized APK/AAB construction, in-place installation on Samsung SM-F976W, exact installed version/hash and a 145 ms cold foreground MainActivity launch. Independent APK/AAB signatures, ZIP integrity, first-install/signer identity, foreground process and bounded runtime checks pass. The 300-line app-log request returns 302 lines including buffer headings, with zero relevant fatal/ANR/Room/SQLite/startup errors.

| Artifact | SHA-256 |
| --- | --- |
| APK, 4,166,194 bytes | `cb022c6b40e5bc418efd6ac023e2157b7108d1a1ad550218c3f57f2f66c34faf` |
| AAB, 11,232,356 bytes | `064f6e49b56d90a1cea2e7291148ffe571317120bf059ffa5c7196dd490ceebf` |
| APK signing certificate | `cdaaa6cf1d6758396aa4ebb8cb408455010e127a018f6d52d359b93929b6d788` |

Installed package is `commvne.com.whip.app`, version 0.3.70/code 76. Original first install remains 2026-08-26 17:59:24; update time is 2026-09-11 11:44:11. Before update, the installed APK exactly matched the preceding verified 0.3.69 release. Room 46, data epoch 6 and backup 26 are unchanged; the in-place update preserves existing app data.

`receipt.json` and sanitized supporting logs record delivery. No transient device identifier, private records or raw app logs are retained. This is private release/startup verification; no complete candidate rerun, phone instrumentation, reset, clear, uninstall, downgrade, store publication or closed-audit resumption occurred. Normal-use owner validation remains separate.
