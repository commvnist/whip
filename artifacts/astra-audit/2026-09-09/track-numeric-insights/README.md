# Correct numeric Track Insights

Baseline: clean pushed `5ae0892`. One mile plus one kilometre was shown as **2609.34 mi** in workspace Insights, because canonical metres received the display-unit label. The corrected total is **1.621 mi**, matching per-Track Insights; both pages show the configured three-place average **0.811 mi**.

One shared presentation helper converts canonical Number values, honors Field precision and locale, preserves fine Scale increments and fractional averages, and treats absolute temperatures separately from differences. Temperature and Scale totals are omitted; averages, ranges and trends remain. Entries, unit definitions, aggregation contracts and stored history are unchanged.

- Five focused JVM tests cover mixed units, Fahrenheit/Celsius/Kelvin, fine and integer Scale averages, locale/precision, and unavailable/nonfinite values.
- Two real-app journeys cover persisted entries, workspace and per-Track Insights, scrolling and recreation. Native text bounds verify complete numeric readings. The final phone run is `fO7NTj`; the wide API 37 run is `wry0Tg`. Both pass two tests with zero failures/skips/reuse and export all four numeric states.
- Neighboring Track domain/editor/CSV/search policies and repository/definition/Entry/CSV/workspace suites pass **69 JVM and 56 Android tests** (`lOYN5s`).
- `scripts/check --ready` passes **349 JVM tests in 35 suites**, Android-test compilation, debug packaging, lint and static gates. This is affected-change readiness, not full-suite or release qualification.
- All eight final phone/wide images were individually inspected. The original failing workspace image is preserved in `before/`. See `review.tsv` for scoped judgments and limits.

The new fixture needed corrections: descendant/heading scroll actions did not expose complete cards; `assertIsDisplayed` accepted a partly clipped average. The final fixture adds a real gesture and native bounds. A wide capture then stalled at Android's idle wait after touch injection. Its thread trace identified the capture boundary; explicitly settling the Compose test clock first resolves it. `tests/api37-interrupted.xml` retains the intentionally stopped failed run. The phone evidence uses identical production and all numeric/native assertions; the subsequent fixture-only change adds that idle wait before capture. Full chronology is in VER-20260909-017.

PNG bytes are original. XML is normalized only to LF. `capture-manifest.tsv` is the original phone manifest, SHA-256 `82b7b5fe5b3b5fa976c9304c7e96e4cd35f4bf911f1e4a849d3c3cfd04abba00`; `api37-manifest.tsv` retains the wide export. `SHA256SUMS` covers this preserved tree.

This accepts numeric correctness and ordinary-text readability in these journeys. Whole Tracks, date-window behavior, large text/RTL/screen-reader coverage, realistic large histories, and the broader composition of Insights remain under review in VER-20260909-016. No physical-device, schema, version or release operation occurred.
