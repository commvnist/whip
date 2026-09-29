# Full Whip verification and repair campaign — 2026-09-29

FB-20260929-009/010; IMP-20260929-012/013; VER-20260929-010. Performed after the first private 0.3.86 release and before the second phone update.

- **724 JVM methods:** zero failures, errors or skips in the final `scripts/check --full` run (410.160 seconds). Debug/release packaging, lint and coverage pass. Domain coverage: 84.24% lines and 63.33% branches; core/settings: 69.49% lines.
- **1,268 Android methods:** every current method executed; cumulative latest results are all passed, zero missing/skipped. The initial inventory exposed product defects and obsolete fixtures. Repair groups and affected regressions then passed. This is a complete inventory plus repair replays, not one frozen rerun of every method after the last edit. Each completed grouped run records its source hashes and unchanged-source result. Interrupted emulator/compiler attempts remain failed or incomplete.
- **Nine benchmark/profile methods:** the full nine-method run passed eight; the remaining dense-workout method subsequently passed all three iterations after refreshing stale native tap coordinates (246.732 seconds including packaging). The original completion assertion and timeout remain intact. Emulator metrics do not establish phone performance.
- **Harness/catalog:** all six harness fixtures have passing evidence. The target-guard fixture's accepted full-campaign replay takes 59.980 seconds; its earlier 55-second timeout remains incomplete. Final catalog lint passes in 1.530 seconds with 530 required captures. This is catalog validation, not a new manual inspection of all 530 states.

Repairs cover collection-first Task/Habit defaults and explicit Today shortcuts, consistent transactional history reads, bounded large-history projection/record rebuilding, ordinary-workout memory use, Saved View casing, Review return routes, Track search/selection restoration, and complete enlarged-text inspector identities. No schema, data-epoch or backup-format change.

`android-inventory.json` maps every method to its preserved XML history and latest result. `jvm-xml`, `benchmark-xml`, `benchmark-metrics` and `campaign` retain exact results, commands and diagnostic failures. `accepted-source-sha256.json` records the tested source. Selected original captures in `visual` show the Task collection/search/Saved Views, large-text Track keyboard/restoration and repaired inspector; subjective appearance still awaits owner use. Perfetto traces and additional local diagnostics remain in the ignored build evidence directory.

Tests ran on three disposable API 34 emulators. No instrumentation, reset or private-record inspection ran on the owner phone. Signed installation is recorded separately in the phone-release receipt.

Copied XML/log line endings and trailing whitespace are normalized for repository review. XML filenames identify the original source-byte hashes; `SHA256SUMS` covers the stored copies. Original outputs remain in the local build directories.
