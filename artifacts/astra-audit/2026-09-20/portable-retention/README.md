# Portable-backup file-ownership evidence — 2026-09-20

This is a scoped shared-folder deletion-safety checkpoint under FND/DEC-20260920-003, not completion of backup or whole-product acceptance. All data and providers are synthetic on a disposable API 34 emulator.

- Native fake-provider baseline `build/instrumentation-results-Q91s4s` executes both new safety tests and fails both: a checksum-valid manual backup named `whip-2026-08-17.whip.json` is pruned by automatic retention, and an unrelated `whip-INCOMPLETE-personal-notes.txt` is deleted by crash cleanup.
- Production now centralizes exact ownership predicates: automatic finals are only `whip-YYYY-MM-DD-HHmmss.whip.json`; abandoned staging files are only `whip-INCOMPLETE-<UUID>.partial`. Manual exports, unrelated files and lookalikes are neither inspected for retention nor deleted. Checksum/record-count verification, protected newly-created URI and failure-safe staging cleanup remain intact.
- `build/instrumentation-results-AzZcef` passes all 13 `PortableBackupManagerTest` methods with zero failure/skip. It covers both corrections plus corrupt/offline/low-storage/read/rename/committed-read failures, staging recovery, retention, revoked-access forgetting, empty scheduled backup and restart-persistent scheduling. `build/instrumentation-results-gKcq66` passes the exact Settings boundary-copy method. All 657 JVM methods pass, including exact-name policy and the updated E2E coverage contract.
- The retained current-source original PNG/XML pair was captured by the existing dark Data & Privacy journey (`build/instrumentation-results-p7DT6c`) and personally inspected at 1080×2520/480 dpi. It explains that retention and cleanup act only on Whip's automatic-backup and incomplete-write filenames; the hierarchy has no NAF nodes.
- Final `scripts/check --ready` passes in 2m9s with all 657 JVM methods, Android-test compilation, lint and debug packaging; it creates no frozen store-candidate evidence.

Real third-party provider revocation/process recovery, replacement/reset failure injection, complete Settings review, full Android matrix and full 523-state recapture remain open under FB-20260920-001.
