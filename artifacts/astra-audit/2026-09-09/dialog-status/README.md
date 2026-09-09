# Readable status icons over light dialogs

FND-20260909-015 · DEC-20260909-013 · IMP/VER-20260909-014. Baseline: clean pushed `1ee7a93`.

Light welcome and Task dialogs used dark status text/icons over Android's dimmed gray exterior. Native pixels measured 2.4143:1 contrast despite passing appearance-flag tests. The shared dialog now uses light icons over that exterior. Search explicitly keeps content-theme icons over the opaque background it paints through the status inset. The inspected welcome contrast rises to 6.1932:1 without changing dim amount, layout, navigation policy or data.

## Evidence and review limits

- `before/`: five personally inspected baseline pairs. Welcome is the original native-Search neighboring capture; the four light owner additions come from the unchanged-production reproduction.
- `final/`: 15 final API 34 PNG/XML pairs, including both themes for all dialog owners and actual-200%-text Search/shared-Task keyboard/recreation neighbors.
- `api26/`: two personally inspected native status PNGs and the four-test result. `api37/`: two personally inspected final PNG/XML pairs and its four-test result.
- All 19 final images have individual observations in [review.tsv](review.tsv). API 26 existing-Task editing shows a separate keyboard/title-viewport concern requiring a dedicated journey investigation; this chunk accepts status contrast, not that entire editor experience. Wide forms and whole-product review remain open.
- PNGs are original bytes; XML line endings alone are normalized to LF. Original capture manifest SHA-256: `85bbce70674f5792fa0e1fa7468fedf7e45d632df008ba5c093aa016a5c1ebf6`. `SHA256SUMS` verifies the preserved tree.

## Verification

- Unchanged-production native pixel checks fail both opposite-theme methods (`JSF7Xc`), identifying light welcome/recreation/theme changes and Task editing at 2.4143:1.
- Six focused API 34 dialog/activity tests pass (`bTMm3F`). The broader final-production campaign passes 56 JVM and 103 fresh Android tests, zero failures/skips/reuse (`qGb15w`).
- API 26 passes four theme/native Search/shared-Task journeys in guarded slot `astra-status-api26`. API 37 passes the same four (`XjWBeU`).
- The first API 37 measurement selected its larger green charging indicator instead of neutral clock/icon ink (`Kca4p6`). The test now excludes independently colored system indicators while keeping its 4.5 threshold and the original failing colors. Production was unchanged; the earlier API 26/neighbor selector already chose the same neutral foreground.
- Final capture passes four owners / 15 states (`C7jYWu`) with fidelity, uniqueness and accessibility guards. The corrected pixel selector executes in this final API 34 capture.
- `scripts/check --ready` passes 344 JVM tests in 34 XML suites, zero failures/errors/skips, Android-test compilation, debug packaging, lint, assets and static checks.
- Current declared inventory remains 625 JVM / 988 Android tests; four discovered light states bring the catalog/matrix to 255. There are 34 actual-font fixtures. This is proportionate verification, not execution of the complete product inventory.

Full commands and failed attempts are in VER-20260909-014. At most two disposable emulators ran concurrently; no physical-phone, schema/version, release or publication action occurred.
