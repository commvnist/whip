# Dialog text-scale coverage and CSV recovery

Related: FND-20260909-006/008/009/010, DEC-20260909-005/007, IMP/VER-20260909-008.

The restart left clean, intact, pushed commit `f702c96`; the unapplied temporary migration was reconstructed from the committed inventory. All 28 remaining native-dialog fixtures now set Android font scale before activity launch and assert the actual rendered text scale. Along with four previously verified fixtures, this completes the 32-entry fidelity inventory. It does not establish whole-product accessibility or design acceptance.

The corrected tests exposed CSV recovery below the mapping form. The existing preview now puts Choose Another File / Replace File beside the error and makes routine guidance secondary. File/date/mapping context, immutable import/retry guards, retained draft, and callbacks are preserved.

## Evidence

- `before-tracks.csv-import.*-large`: unchanged production with actual 200% Android text, from the initial resumed tests.
- `after-light-tracks.csv-import.*-large`: matching light-theme replacement captures from the focused 15-test pass. The `(1)` generation in the export was selected deliberately; the unsuffixed files were before evidence.
- The 34 bare surface IDs are the exact final catalog generation, personally reviewed. All original PNG bytes match `capture-manifest.tsv`; XML line endings alone are normalized.
- `pending-*` pairs retain the initial short-dialog observations. Their matching final captures still exhibit the problem.
- `review.tsv` records a separate assessment for every final state. Isolated test-host exteriors are not app-shell acceptance.

## Verification and limits

One explicit disposable API 34 emulator, 1080×2400, density 420. Most owning methods request 2.0; Task Templates requests 3.2, and the normal CSV preview uses 1.0. The font rule restored Android to 1.0.

The first batch was rejected before instrumentation due to three incorrect Kotlin package selectors. After correction, the initial 28 tests produced 25 passes and three failures: hidden CSV recovery, a lazy CSV mapping node requiring owner scrolling, and offscreen Machine impact requiring owner scrolling. No corrected rendered-scale assertion failed.

All 14 CSV UI cases plus Machine impact passed 15/15 in `build/instrumentation-results-VKkCdg`. The final capture passed 29/29 owners with zero failures/skips/reuse in `build/instrumentation-results-cwzA28` and exported exactly 34 pairs. Command: `WHIP_UI_CATALOG_FILE=/root/repos/whip/build/astra-dialog-font-20260909/capture-catalog.tsv ANDROID_SERIAL=emulator-5554 scripts/ui-catalog capture build/astra-dialog-font-20260909/final-capture`. The subset is the canonical catalog filtered to the 28 corrected selectors plus the normal CSV owner; selectors remain in the canonical catalog and font-review inventory.

`scripts/check --ready` passed 344 JVM tests across 34 suites, Android-test compilation, debug build/lint and routed static checks. This is scoped development evidence, not the full 625-JVM/983-Android goal gate. Logs remain under `build/astra-dialog-font-20260909/`.

## Open review items

- FND-20260909-009: short Task Templates and destructive recovery dialogs need more useful reading space. Area and backup choosers initially show no choice after their introductory content. Their scroll tests pass; their initial discovery/layout is not accepted.
- FND-20260909-010: the shared Task capture frame overlaps the status bar with IME open. Establish settled-window behavior before selecting a correction.
- The adjacent September date-wheel label is ellipsized; the full selected date remains explicit. Continue the complete picker review.
- Minor copy observations for subsequent domain review: normal CSV says “1 rows,” and the Habit pause error repeats retained-draft/retry guidance.

No data/schema/backup/version/release change or physical-device operation occurred. All area-level substantive reviews and the final whole-product gate remain open.
