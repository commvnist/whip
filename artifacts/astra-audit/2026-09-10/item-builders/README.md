# Shared record-item builder pilot

2026-09-10 · FND/DEC/IMP/VER-20260910-003 · baseline `7bdcc113df668387a79ba3f89295e70cf1218db2`.

Track Entries and Activity previously assembled the same records through separate local layouts. Activity squeezed metadata between identity and actions and cut off the sample Weather fact at normal text scale. WhipRecordItem now owns the title/action header and full-width context/facts below it. Callers declare content and commands; the existing repositories retain mutation authority. Both entry points use one Track presentation mapping.

## Evidence

| Run | Outcome |
| --- | --- |
| `baseline.log` / 1w8kcE | Native record journey passes on previous production source; two inspected originals show inconsistent reading order and Activity truncation. |
| `pilot.log` / rPTeKm | Initial production pilot passes the same native journey. |
| `neighbors.log` / 7Af8CZ | 33 fresh Android checks pass on API 34, zero failures/skips/reuse: record journey, shared layout/action contracts and Track workspace/history/integrity/mutation/deletion-recovery neighbors. |
| `api37.log` | Three focused native/shared-builder tests pass in 33.951 seconds on the wide API 37 emulator. Both APK installations report Success. |
| `ready.log`, `readiness-jvm/` | 346 fresh JVM checks in 34 suites, zero failures/errors/skips; compilation, lint and debug packaging pass. |
| Catalog logs | Initial lint detects removal of two local menu owners. Reviewed source snapshot refresh yields 387 states, zero exceptions/pending selectors; catalog fixtures pass. |

The journey checks Open, Edit cancellation, Delete review cancellation, Open Track, recreation, archived read-only inspection and exact retained Entries/Fields/Options. Shared tests check a 320dp layout, record-keyed menu ownership, fresh callbacks and direct-action isolation. No source/APK input changed during final instrumentation, and hashes still match after readiness.

## Reproduce

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android com.whip.app.RecordItemBuilderJourneyE2ETest --android com.whip.app.ui.WhipRecordItemTest --android com.whip.app.TrackWorkspaceUiTest --android com.whip.app.TrackHistoryJourneyE2ETest --android com.whip.app.TrackEntryIntegrityTest --android com.whip.app.TrackEntryMutationUiTest --android com.whip.app.ui.TrackEntryDeleteRecoveryUiTest --emulator
adb -s emulator-5556 install -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5556 shell am instrument -w -r -e class com.whip.app.RecordItemBuilderJourneyE2ETest,com.whip.app.ui.WhipRecordItemTest commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner
scripts/check --ready
scripts/ui-catalog lint
scripts/test-ui-catalog
```

Both disposable emulators use system font scale 1.0: API 34 is 1080×2400 at 420dpi; API 37 is 2560×1800 at 320dpi. No physical device is targeted.

## Visual scope and provenance

`review.tsv` records ten personally inspected original PNGs: two before, four final per platform. Normal-scale records use the same reading order, sample facts are complete, menu actions stay distinct, and archive inspection remains available without row mutation controls. Long records can still scroll and previews remain bounded; this is not universal content, RTL, screen-reader or 200% acceptance.

Original PNG bytes are unchanged and pass chunk CRC, decompression and terminal-chunk checks. Paired XML parses and contains zero NAF nodes. Text-only line-ending/trailing-whitespace normalization has original and retained hashes in `text-normalization.json`. Capture manifests, native aggregates/XML, source/APK hashes and `SHA256SUMS` retain provenance. This is one verified builder family; productivity, navigation/settings, execution and broader app UX/design/bug review continue. No schema, backup, version or release change.
