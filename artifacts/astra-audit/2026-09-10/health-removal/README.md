# Health Connect retirement — 2026-09-10

Owner request FB-20260910-003 supersedes the active Health warning fix and the integration portion of the whole-product audit. Base: `fd4efb9935292f003ad947ce321564751fbe5d9b`. DEC/IMP/VER-20260910-026 track this bounded removal; the larger quality goal remains active.

Whip no longer contains the Health Connect SDK, permission requests, provider-query declaration, rationale activity/alias, Settings controls, provider manager, startup sync, local-copy deletion workflow or provider reconciliation API. New settings and exports omit the integration configuration; compatible older backups ignore those retired fields. Historical source markers remain readable so existing data keeps its provenance.

Legacy Health-linked Habits retain their formerly projected activity as stored logs before the retired source link is cleared. The conversion runs atomically before normal startup and inside backup restore/merge. Values, canonical amounts, units, status, dates, offsets, source IDs, notes and authored timestamps survive; the formerly transient log receives a database row ID. Existing manual history and all measurement entries remain unchanged. Repeated recovery does not duplicate history, and future manual check-ins work normally. Schema and app version are unchanged.

## Native verification

- Final phone: `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile settings --profile habits --android com.whip.app.RecoveryBoundaryIntegrationTest --android com.whip.app.ActivityThemeContrastTest --android com.whip.app.PlatformEntrySurfaceE2ETest --emulator` — `xhVRkW`, **193 fresh tests**, batches 103/69/21, no failures or skips.
- Wide: matching production APK on API 37, direct AndroidJUnitRunner selection of `LegacyHealthHistoryTest`, `LegacyImportJourneyE2ETest`, and `SettingItemBuilderJourneyE2ETest` — **7 methods**, 37.371 seconds, no failures. Subsequent changes only correct an unrelated Android fixture's retired wording, JVM documentation contracts, and a Gradle comment; the production APK and all seven wide target fixtures remain identical.
- The new tests cover complete imported/manual history, built-in and custom conversion, archived and empty Habits, transaction rollback/retry, repeat restore/merge, continued manual logging, and absence of installed Health permissions, activities and SDK classes. The real MainActivity journey restores legacy data, opens Data & Privacy, records +1, reopens History and recreates the Activity.
- API 34 phone: 1080×2400, 420 dpi. API 37 wide: 2560×1800, 320 dpi. Both finish at font scale 1.0; no physical-device or release operation.

## Evidence and rejected observations

`scripts/check --ready` passes **402 JVM tests in 49 suites**, Android compilation, lint/debug packaging and routed harness checks (final build stage 2m44s). The complete `./gradlew :app:testDebugUnitTest :app:lintDebug` run passes **652 tests in 113 suites**, zero failures/errors/skips, and final lint in 14s. Catalog lint and its fixtures pass 521 states. All 516 final source/build/harness/APK hashes match. Source inventory is 652 JVM + 1072 Android = 1724; catalog review is 354 Verified / 125 Investigating / 42 In progress.

[review.tsv](review.tsv) records ten personally inspected original images; [manifest.json](manifest.json) records original and retained hashes. There are six final native PNG/XML pairs, three pilot pairs and one historical SettingsContent control-fixture pair. The historical fixture demonstrates the removed controls; its scrolled position and gray status bar are not MainActivity or matched-geometry acceptance. The pilot manual-card image shows 0 before the persisted entry reaches the UI and is explicitly rejected as completion evidence. The final capture waits for visible 1/10000.

The earlier warning baseline and pilot were superseded before acceptance. The removal pilot `gTljsr` passes 36 checks. Intermediate compilation errors were corrected before instrumentation. `IScbie` passes 103 checks, then runs 69 with one failure because a generic linked-measurement fixture still expected the retired Health label; the final 193-check run passes with corrected wording. Initial readiness runs 402 checks with two stale documentation contracts: a missing already-inventoried Track capability and a removed Gym instructional sentence. Those contracts are reconciled with current source and documentation before final readiness.

PNG CRC, complete IEND and decompressed image data are checked. XML is parsed and checked for unnamed interactive nodes; original XML bytes are retained as gzip beside readable normalized copies. Logs and reports, including failures, are archived in `checks/`. Frozen inputs and explicit fixture/JVM-only differences are recorded there.

The first complete JVM run also exposes two stale guards from the earlier builder migration: fixed per-file forward-icon counts and the former Insight/Custom Unit card types. The guards now verify the retained mirrored-icon requirement and current shared summary/record roles. These corrections change JVM tests only; the subsequent complete run and lint pass.

This verifies the retirement and retained-history behavior. Whole-product acceptance, further Settings visual refinement and release qualification remain separate work.
