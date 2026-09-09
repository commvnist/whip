# Native Search: keyboard, large text and responsive focus

FND-20260909-014 · DEC-20260909-012 · IMP/VER-20260909-013. Baseline: clean pushed `cc5e1a9`.

The original real Search dialog overlaps the status bar and hides results behind the keyboard on API 26 at 320×533 dp and actual 200% text. Explicit insets alone make the header readable but leave no room for results. A later API 37 journey also exposes query focus loss when keyboard height moves the workspace between wide and compact layouts.

Search now paints and consumes its system-window insets. Below 440 dp of usable height, title/exit and query stay fixed while context, filters, count and results share the existing list. One stable child structure preserves query focus as its placement changes between columns and rows. Indexing, matching, result identity, scope, persistence and ordinary layout policy are preserved. Font sizes and touch targets are unchanged.

## Evidence

- `before-api26/` shows the original overlapping header and hidden results; `intermediate-insets-api26/` shows why insets alone were insufficient.
- `intermediate-short-api26/` preserves the accepted short-layout step before the final backdrop/focus correction.
- `before-api34/`, `intermediate-api34/` and `intermediate-api37/` preserve native baseline and backdrop observations.
- `intermediate-focus-api37/` shows the actual unfocused Search field with no keyboard. A deterministic wide→short regression reproduced the same focus loss; movable-content reparenting also failed.
- `final/` contains 13 final API 34 catalog states. `api26/` and `api37/` each contain three final native frames and their test result. All 19 final images were personally inspected; [review.tsv](review.tsv) records individual scope and limits.
- The final API 37 recreated frame includes Gboard's font-size banner, causing wide→compact reflow while the query remains focused and the keyboard remains visible.
- PNGs are original bytes. XML line endings alone are normalized to LF. `capture-manifest.tsv` preserves the original capture hashes; its SHA-256 is `f1725ca95baa4da5fc87943b3b2fec8007fdf3e0ca01e7daccc619ad9b481509`. `SHA256SUMS` hashes the preserved files.

## Verification

- Final neighboring campaign: 56 JVM and 99 fresh Android tests, zero failures/skips/reuse (`build/instrumentation-results-ZOH1cQ`).
- Final API 37 native/dual-theme sequence: 3/3 (`Xa0EwN`). Final API 26 native journey: 1/1 (`astra-search-api26-stable`). Both verify actual 200% scale, keyboard-visible native bounds, status-backdrop pixels, query/filter retention and exact persisted Task routing.
- Final canonical capture: 4 owning tests / 13 states (`R3Jipk`), capture fidelity/uniqueness/accessibility guards passed.
- `scripts/check --ready`: 344 JVM tests across 34 XML suites with zero failures/errors/skips; Android-test compilation, debug packaging, lint, assets and static gates passed.
- Current declared inventory: 625 JVM / 988 Android tests, 251 catalog states and 34 actual-font fixtures. This chunk did not execute the complete product inventory.

The detailed failed attempts and exact commands remain in VER-20260909-013. At most two disposable emulators ran concurrently; no physical-phone, schema/version or release action occurred. Whole-product acceptance remains open. The neighboring light welcome dialog's dimmed status backdrop needs separate contrast investigation; Review and global-add receive only scoped visual observations here.
