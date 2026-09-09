# Whip activity theme contrast

Scoped evidence for `FND-20260908-015` and `DEC-20260909-001` in the active Astra whole-product audit.

Whip's selected Light/Dark theme now controls status and navigation icon contrast in the main app, widget configuration, and Health rationale. Recovery screens use Android's theme and return to Whip's stored preference afterward. One activity theme wrapper owns this behavior; ordinary component theming remains independent of Android windows. Android 8–9 also receive the matching navigation background scrim.

| Case | Before | After |
| --- | --- | --- |
| Dark Whip, Light Android | [Black icons against dark content](before-dark-on-light.png) | [Readable light icons](after-dark-on-light.png) |
| Light Whip, Dark Android | [White icons against light content](before-light-on-dark.png) | [Readable dark icons](after-light-on-dark.png) |

Additional inspected API 34 windows: [recovery after dark content](after-main-recovery-light.png), [Health rationale](after-health-dark-on-light.png), and [widget configuration](after-widget-dark-on-light.png). Matching accessibility XML accompanies these PNGs. The window contrast review does not constitute acceptance of those features' complete workflows.

Both themes were also inspected on the 320×533dp API 26 display ([dark](api26-dark-on-light.png), [light](api26-light-on-dark.png)) and the 1280×900dp API 37 display ([dark](api37-dark-on-light.png), [light](api37-light-on-dark.png)). API 37 includes matching XML; API 26 uses private screenshot files because MediaStore Downloads is unavailable. All four activity-theme tests pass on each platform, including live changes, recreation, recovery/return, and legacy scrim checks. The adjacent API 34 run passed 110 Android regressions; readiness passed 344 JVM tests plus debug build/lint.

The unchanged-production regression `build/instrumentation-results-RAdydS` failed both opposite-theme assertions. The corrected shared campaign `build/instrumentation-results-mXJwPF` passed nine owning tests with zero failures/skips/reuse and produced all 31 declared shared PNG/XML pairs. The five added theme states explicitly control and restore Android's theme, so the catalog's usual forced-dark setup cannot mask the mismatch.

Original corrected capture: `/tmp/whip-astra-theme-shared-20260909`. Its manifest SHA-256 is `5df48f6d6c7e2fedf7511dffe53e468e4344fb0d7729703f44f8b23acff3a9fd`. PNGs are byte-for-byte copies; checked-in XML normalizes line endings to LF. The before images come from `/tmp/whip-astra-theme-before-20260909`.

Final verification scope and limitations are recorded in `VER-20260909-002`. No application version, schema, backup format, user data contract, or release artifact changed. The whole-product goal remains active.
