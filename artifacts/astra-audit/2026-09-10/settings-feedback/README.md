# Settings outcome and backup failure feedback — 2026-09-10

FND-20260910-037/038; DEC/IMP/VER-20260910-028. Baseline: `f15b173dab74b8bc4e2440faa1e4436f425dbf42`.

Settings reuses typed OperationStatus instead of classifying exception text. A rejected checksum now produces an error; unlock/preview/replacement/reset own their failure message inside the active dialog. A successful preview settles quietly. Existing busy lifecycles, callbacks, data boundaries and unsaved passphrases remain intact.

## Verification

- `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile settings --android com.whip.app.SettingsBehaviorUiTest --android com.whip.app.ui.SafetyChoiceUiTest --android com.whip.app.EncryptedBackupCodecTest --android com.whip.app.RecoveryBoundaryIntegrationTest`: **128 fresh Android checks**, 55/73 batches, zero failures/skips/reuse (`ntNwec`).
- Exact APK install and explicit API 37 `AndroidJUnitRunner` selection of `DataPrivacyJourneyE2ETest,com.whip.app.ui.SafetyChoiceUiTest#replacementFailureStaysInItsConfirmationAndAllowsRetry`: **five checks**, 95.042s, all passed.
- `scripts/check --ready`: **384 JVM checks / 44 suites**, zero failures/errors/skips; Android compilation, lint, debug build and routed harness checks pass. Final build stage: 2m40s.
- `scripts/ui-catalog lint` and `scripts/test-ui-catalog`: **534 states**, no pending selectors/platform exceptions. Source inventory **652 JVM + 1077 Android = 1729**. All **518** frozen source/build/harness/APK hashes match after verification.

The native journeys use actual Android document selection, replace an invalid file, retry a wrong passphrase, perform Merge, recreate the Activity and compare complete original records. Replacement-failure retry uses a component callback simulation; it does not prove a real rollback injection.

## Before/after and limits

All **twelve original images** were personally inspected: two reproduced phone failures, five final phone states and five final wide states. [review.tsv](review.tsv) records each scope; [manifest.json](manifest.json) records original PNG/XML hashes. PNG CRC/IEND/decompression and XML/no-NAF checks pass. Original XML and original CRLF test reports are compressed under `raw/`; readable reports normalize line endings only. Numbered MediaStore sources are recorded in `checks/final-retrieval.json`.

`Hx39y8` fails its expected error heading and proves green success styling for an invalid checksum. `B94W3r` fails the dialog-owned error assertion and proves wrong-passphrase feedback behind the modal. Both preserve original-record checks. Intermediate pilots `YoKXRy` (three) and `RKdL9K` (five) pass; final source then restores existing independent busy coordination, followed by the full final runs above. Failed evidence is retained separately from acceptance.

Phone: API 34, 1080×2400 at 420 dpi. Wide: API 37, 2560×1800 at 320 dpi. Both use normal text scale. Final unlock captures have the keyboard closed; no new keyboard-specific or full restoration/reset failure-injection claim is made. Quiet preview, typed error and in-dialog retry are accepted; complete encrypted export delivery, portable-folder/provider/process recovery, API 26, new 200%, TalkBack and exhaustive whole-product acceptance remain outside this increment.

The owner subsequently requested wrapping, private phone delivery and goal closure in FB-20260910-004. Release and deferred audit scope are recorded separately in VER-20260910-029.
