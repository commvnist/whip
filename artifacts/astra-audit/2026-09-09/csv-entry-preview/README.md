# Interpreted CSV Entry review

Verified 2026-09-09 for FND-20260909-029 / DEC-20260909-025 / IMP-20260909-027 / VER-20260909-028. Baseline: `bccd2f9`. The complete Tracks and whole-app audit remain open.

The old import review showed mappings and validation counts, but no values. The new review browses one interpreted Entry at a time using the exact frozen form. It exposes a Name accidentally mapped to Notes and a Distance interpreted as miles until its unit column is selected. Dates, text, numbers, Choice labels, Scale values and Yes/No remain readable before import. Only a selected index is saved; a mapping/file revision resets it.

Long values show an explicit 1,000-code-point excerpt while the complete draft remains available to Import. The excerpt test retains 1,401,001 UTF-16 characters through the submission callback. The native journey independently verifies actual two-Entry persistence and exact seven-type export/reparse equality.

## Before and after

- `baseline/api34.png` uses the same seven-Field native fixture as the final journey; its counts-only review cannot show River loop or its interpreted values.
- `baseline/api37.png` independently shows the same limitation with a two-Field Track.
- `api34/tracks.csv-review.remapped.ordinary.png` exposes “Wind, then rain” as the wrongly mapped Name.
- `api37/tracks.csv-review.second.ordinary.png` displays the second Entry with 2.00 km, Street, 4, the complete multiline note, Visit date and Yes.
- `diagnostic/hidden-completion.large.png` records the first candidate's retained lower scroll position after a successful import. The receipt was above the viewport. `api26/tracks.csv-review.complete.large.png` shows the corrected initial completion position and complete receipt copy.

The review keeps fixed actions, ordinary Field rows and the existing import lifecycle. A new wizard would add navigation and recovery complexity without improving this mapping check. Completion and authoritative recovery/error messages now reset reading to the beginning. No schema, receipt, parser, history, backup format or export-format change occurs.

## Verification

| Check | Result |
| --- | --- |
| `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile tracks --emulator` | 65 JVM / 95 fresh Android tests; `hMILHp`, two Android batches, zero failures/errors/skips/reuse |
| Final native API 26 / 34 / 37 | Two ordinary/actual-200% journeys pass on each; exact file selection, remapping, recreation, import, native export and full reparsing |
| `scripts/check --ready` | 345 JVM tests in 34 suites; compilation, lint, debug packaging and wrapper/static gates pass |
| Catalog lint and fixtures | 329 states, no pending selectors/platform exceptions; catalog and check-fast fixtures pass |
| Individual image/tree review | 28 original PNGs reviewed: two baselines, two rejected diagnostics, 24 final frames; final trees have zero NAF nodes |

Final direct instrumentation uses `adb -s SERIAL shell am instrument -w -r -e class com.whip.app.TrackCsvJourneyE2ETest commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner` after installing both debug APKs. API 26 is 480×800 at 240 dpi, API 34 is 1080×2400 at 420 dpi, and API 37 is 2560×1800 at 320 dpi. Only two disposable emulators ran concurrently; both surviving devices return to font scale 1.0.

The journey changes Name→Notes→Name and explicitly maps the nonstandard Distance Unit column. It proves 2.00 mi before mapping and 2.00 km afterward, preserves the selected Entry and completed receipt through Activity recreation, checks exact repository values, uses native Create Document, then compares saved bytes and every reparsed draft. The Tracks profile permanently includes this journey plus CSV UI and recovery ViewModel tests.

## Evidence authority and rejected runs

`review.tsv` contains individual observations and visible-state limits. `capture-files.json` in each platform directory identifies original device paths and timestamps. API 34 retained originals come from `z6fKNd` on final production; the subsequent final native test additionally waits for unblocked Done. API 26/37 retained originals use that final test. `source-and-apk-sha256.json`, execution XML/aggregates, logs and `SHA256SUMS` bind the checkpoint. No screenshot is accepted merely because its file exists.

Initial native setup failed because UiAutomation does not interpret shell pipes/quotes like a shell. A single-token synthetic fixture script and byte equality assertion resolved that. The subsequent baseline fails at the missing Entry value. Later failures exposed a wrong unit auto-mapping assumption, an unstable Options-list selector, and a test request whose row count did not match its preview; each is recorded separately from product defects in VER-20260909-028.

The first API 26 candidate genuinely hid its completed receipt at a retained lower scroll position. Separately, `diagnostic/stale-completion.ordinary.png` shows the preceding preview while its tree already reports completion. Legacy captures now use the existing canonical draw/accessibility/distinct-image pipeline with app-private output below API 29. The final test waits for the input shield to clear and Compose to settle, preventing a transient dim frame. Both diagnostic originals are rejected as final acceptance. A direct API 34 attempt with the app absent after connected-test cleanup executed no tests; reinstalling the debug APK restored the test environment.

## Limits and next review

This accepts the interpreted preview and normal native transfer. It does not close native invalid-file replacement, full CSV OS-process recovery, realistic large-history latency, remaining Field/destructive flows, RTL/TalkBack or whole-app acceptance. Existing receipt-first recovery tests pass, but Activity recreation does not establish every process/provider interruption. At small sizes, the user scrolls through values; a single screenshot does not show every Field simultaneously. Source inventory is 631 JVM / 1,019 Android tests, 329 catalog/matrix states and 42 actual-font methods, not a claim that every source test ran here. No physical-phone operation or release occurred.

Text evidence normalizes CRLF line endings and trailing physical-line whitespace for Git; `text-normalization.json` records original and retained hashes. PNG originals are unchanged. Verify retained file integrity from this directory with `sha256sum --quiet -c SHA256SUMS`.
