# Cumulative Track history / SQLite bind ceiling

Scope: FB-20260920-001, FND/DEC-20260921-006 and IMP/VER-20260921-007. Disposable Android emulators only; no owner-phone installation, signed release, schema, backup or data-epoch change.

## Reproduction and correction

- A new production Room/Track-repository journey seeds 1,200 persisted one-field Entries and Values, then asks for the complete Track projection, CSV export, rebuilt search index, exact deletion preview and committed deletion. Before the production correction, this fails at the first direct projection on the API 26 `whip_api26_small` emulator: `SQLiteException: too many SQL variables` in Room's `getValuesForEntries` expansion. The same pre-fix fixture passes on API 34 after correcting a test-only CSV trailing-newline count; that initial assertion is not a product defect.
- A separate fixture seeds 33,000 persisted Entries/Values through bounded direct DAO inserts and checks the complete direct projection's count and first/last text plus a 100-row page. This exceeds SQLite's modern default 32,766 host-parameter ceiling. The final API 34 test passed 1/1 in `build/instrumentation-results-8ben7O`; its in-test direct-projection timing was 703 ms on that emulator. The first attempt was a JUnit fixture signature error (`runBlocking` expression returned `Log.i`'s integer), not a product result; adding a final `Unit` corrected it.
- The fix centralizes 900-ID value-query batches and uses them inside the existing transaction boundaries for projection/export, index rebuild, paging and deletion impact. It does not trim history or weaken the existing 5,000-row/100,000-cell CSV import contract.

The 33,000-Entry fixture models a cumulatively grown persisted Track but does **not** execute seven actual CSV requests or prove seven separate import receipts. The existing exact 5,000×20 CSV import/receipt test covers that per-request boundary. Multiple real CSV requests, specialized multi-Field analytics, actual-device jank and whole-product acceptance remain open.

## Final verification

- API 26 exact 1,200-Entry journey: 1/1 pass after the fix and final shared fixture extraction, via guarded direct `connectedDebugAndroidTest` on `emulator-5554`.
- API 34 exact 33,000-Entry journey: 1/1 pass in `build/instrumentation-results-8ben7O`.
- API 37 wide emulator: both exact tests pass independently, 1/1 each, using `adb shell am instrument -w -e class` against the current debug and test APKs; the larger fixture takes 12.033 seconds end-to-end.
- Final affected-family `ANDROID_SERIAL=emulator-5556 scripts/qa-targeted tracks --emulator`: 74 Track JVM methods and 134/134 Android methods, zero failure/skip/reuse, across three accepted batches in `build/instrumentation-results-vwGoew`. This includes both new regressions and the existing exact 5,000×20 CSV/receipt case. That case measured 16,872 ms import commit, 438 ms full projection, 12 ms page, 10 ms analytics and 365 ms refresh on the API 34 emulator. The 33,000-Entry direct projection measured 532 ms in this later family run.
- Final `scripts/qa-targeted --all-jvm` passes all 659 JVM methods; `scripts/check --ready` passes in 2m8s with Android-test compilation, lint and debug packaging. `scripts/ui-catalog lint` reports 528 required states, zero pending/exception. This is not the 1,101-method complete Android matrix or a 528-state exact visual capture.

Platform bind ceilings are not inferred solely from emulator behavior; see the [official SQLite limits](https://www.sqlite.org/limits.html). The real API 26 failure and exact post-fix replay establish this product's minimum-platform impact.
