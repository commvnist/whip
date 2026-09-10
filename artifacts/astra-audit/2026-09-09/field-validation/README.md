# Track Field validation and existing-history editing

2026-09-09 · FND-20260909-030 · DEC-20260909-026 · IMP-20260909-029 · VER-20260909-030.

Baseline application source: `4f38aae25c4e45e2cdd371a91ba63e5cb048499b`. This is scoped acceptance under the active whole-product goal, not whole-Tracks completion.

The baseline accepts duplicate Choice labels in the nested editor and outlines Increment when Minimum is invalid. The correction shares canonical Choice equivalence with persistence, identifies conflicting options before leaving the Field, and places Scale errors at the input needing correction. Bounds stack on narrow/enlarged dialogs; focused controls reuse existing relocation. Number guidance describes default units and preserved history. Repository authority and accepted domain values are unchanged.

The native journeys edit a real four-Field Track containing two saved Entries. They retain raw invalid drafts across Activity recreation, correct them, save the parent Track and reopen persisted configuration. Complete Entry objects/value maps, Field and Choice identities remain equal. Existing type/dimension locks, Choice renaming, valid Scale changes and changing miles to kilometres are exercised. These are real UI/repository assertions; a screenshot alone does not prove persistence.

## Execution and provenance

| Evidence | Result |
| --- | --- |
| `baseline.log` | First fixture compile used an incorrect draft property; zero tests executed. |
| `baseline2.log`, `6pXjqV` | Both ordinary baseline regressions fail at the expected product boundaries. Two originals show the defects. |
| `corrected.log`, `CSv3H4` | Two Choice methods pass; two Scale methods fail at a nonexistent unit-menu content-description selector. |
| `corrected2.log`, `PEtCuG` | Three methods pass; the generic enlarged-font sweep reaches an unlaid lazy node. The final assertion checks actual visible minimum-error text. |
| `corrected3.log`, `g54ePq` | Four methods pass on API 34 after validation/layout and fixture corrections. TrackDomainTest passes ten JVM methods. |
| `neighbors.log`, `lCw6Ne` | 66 JVM checks, then 111 Android tests across two batches; zero failures/skips/reuse. Includes definition mutation, authoring, CSV, recovery, repository and workspace neighbors. |
| `api37.log` | Initial full class has one AndroidFontScaleRule setup timeout before app entry; three methods pass. |
| `api37-large2.log`, `api37-ordinary2.log` | Fresh-process pairs each pass two methods after explicitly applying the matching system font scale. |
| `api26.log` | Two Choice journeys pass; Scale navigation fails because scrolling a tall lazy item does not ensure the specific child is visible. |
| `api26-2.log` | All four pass after exact-child scrolling and closing the opening keyboard. |
| `final-api34.log`, `YNT2AQ` | All four pass with the corrected navigation. |
| `api26-final.log`, `final-api34-2.log` / `7qmuw2` | Final four-method classes pass, including explicit unit-history-helper visibility. |
| `api37-final-large.log`, `api37-final-ordinary.log` | Final two-method fresh-process runs at actual 200% and ordinary text. |
| `ready.log`, `readiness-jvm/` | 346 JVM tests in 34 suites, zero failures/errors/skips; compilation, lint and debug packaging pass. |
| `ready-final.log` | Final readiness after test navigation/copy assertions; unchanged JVM task may be reused as shown in the log. |
| `check-fixtures.log`, `catalog-fixtures.log`, `catalog-lint.log` | Wrapper/catalog fixtures and catalog contract. |

Final source and APK hashes are in `source-and-apk-sha256.json`. The initial accepted hashes remain separately identified: production source and application APK did not change after `g54ePq`; later changes strengthen fixture navigation and helper visibility. Initial failures are retained as failures, not counted as successful platform coverage.

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile tracks \
  --android com.whip.app.TrackDefinitionMutationUiTest \
  --android com.whip.app.TrackAuthoringJourneyE2ETest --emulator

ANDROID_SERIAL=emulator-5554 scripts/qa-targeted \
  --android com.whip.app.TrackFieldEditingJourneyE2ETest --emulator

adb -s emulator-5556 shell am instrument -w -r \
  -e class com.whip.app.TrackFieldEditingJourneyE2ETest \
  commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner

scripts/check --ready
scripts/ui-catalog lint
```

The direct class command above is the API 26 run. API 37 final execution selects the two `AtLargeText` methods together after setting system `font_scale` to `2.0` and force-stopping the debug package; the two ordinary methods run in a fresh process after restoring `1.0`. Each direct log must contain `OK (N tests)`; shell exit status alone is insufficient. APKs were installed explicitly before direct runs. API 26 uses 480×800/240 dpi; API 34 uses 1080×2400/420 dpi; API 37 uses 2560×1800/320 dpi. No more than two disposable emulators ran concurrently.

## Visual review and limits

`review.tsv` records individual observations for 22 retained originals: two baseline, two initial correction diagnostics and six final states on each API. PNGs are untouched; paired native trees accompany them. The catalog contains six distinct states, not one state per platform or scroll position. Large-text checks inspect actual TextLayoutResult density at 2.0.

The invalid input and its guidance remain readable above fixed actions. Large/small dialogs require scrolling; content beyond a scroll boundary is not claimed to fit simultaneously. The Number unit and complete history helper are deliberately brought into view in final capture. Choice error text wraps to three lines on the smallest enlarged screen, but stays readable without covering the input or footer. Wide ordinary bounds remain adjacent; narrow/enlarged bounds stack.

This campaign does not establish TalkBack, RTL, destructive Choice/Field replacement, retained-history-incompatible Scale rejection UX, duplicate parent Field-name visibility, realistic large histories or whole-app acceptance. Those remain active follow-ups. No schema/backup/version change, physical-phone operation, release or publication occurred.

After the Windows-update interruption, Git object checks found no corruption and no unfinished Git operation. The neighboring test job had completed successfully; the next emulator and saved captures already existed. Verification resumed from their actual state. `git-fsck.log` may list dangling historical objects; none were deleted or rewritten. Text-only evidence normalization is recorded with original/retained hashes in `text-normalization.json`; PNG bytes are unchanged. `SHA256SUMS` covers retained evidence.
