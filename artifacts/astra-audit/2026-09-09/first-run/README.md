# First-run setup: durable completion and content-sized layout

Related: `FND-20260909-002`, `FND-20260909-003`, `DEC-20260909-002`.

The two-step recommended/customize flow remains. Its dialog now sizes to its
content, keeping actions directly after the body and outside its scroll area.
Submission waits for a confirmed settings receipt, blocks duplicate input,
retains choices on failure or request interruption, and requests optional
notification permission only after successful completion.

| State | Before | After |
| --- | --- | --- |
| Real welcome | [PNG](before-welcome.png) / [XML](before-welcome.xml) | [PNG](after-welcome.png) / [XML](after-welcome.xml) |
| Customization | [PNG](before-customize.png) / [XML](before-customize.xml) | [PNG](after-customize.png) / [XML](after-customize.xml) |
| Optional preferences | [PNG](before-optional.png) / [XML](before-optional.xml) | [PNG](after-optional.png) / [XML](after-optional.xml) |
| Saving | No owned pending state | [PNG](after-saving.png) / [XML](after-saving.xml) |
| Failed save with draft retained | No failure state | [PNG](after-save-error.png) / [XML](after-save-error.xml) |
| Optional preferences, Android 200% text / RTL | Newly verified configuration | [PNG](after-optional-large-rtl.png) / [XML](after-optional-large-rtl.xml) |
| Save failure, Android 200% text / RTL | Newly verified configuration | [PNG](after-error-large-rtl.png) / [XML](after-error-large-rtl.xml) |

At the same 1080×2400 real-app viewport, the vertical gap between the welcome
explanation's text bounds and the primary button's label shrinks from 1,126
pixels to 76. The label moves upward by 525 pixels. Content, primary and
secondary actions, default choices, and settings paths remain available.
The expanded optional form now uses the available height instead of clipping
its final note above an unused region. At actual 200% Android text and 320 dp
dialog width, the body scrolls and the footer wraps into two reachable rows;
failure brings its explanation into view while retaining those actions.

Baseline: `build/instrumentation-results-MQLM5C`, with original exports in
`/tmp/whip-astra-first-run-before2`. After: the 12-owner, 39-state shared
capture in `build/instrumentation-results-lMVsSi`, exported to
`/tmp/whip-astra-first-run-shared-20260909`. Its manifest SHA-256 is
`78771dfb28adc352e2dc79e3df46ed0297706e689426c252448b23c1b1e4416b`.
All linked PNGs are unchanged device exports; XML line endings were normalized.
All seven after images and the four baseline states were personally inspected.

The failure/pending views use controlled request outcomes in the production
setup host; the welcome/customization/optional views use the actual activity.
The large-text test changes Android's font setting before activity launch,
asserts the title's rendered font scale, and restores the setting after
activity teardown. Earlier captures using only an outer Compose density
override are excluded from enlarged-text acceptance (`FND-20260909-006`).

This is scoped setup-layout and save-lifecycle evidence. The dark-dialog
status-icon mismatch remains tracked under `FND-20260909-004`. The
[configured-Home baseline](before-configured-home.png) exposes the separate
priority mismatch after choosing Tracks/Gym (`FND-20260909-005`). Broader
dialog accessibility and whole-product acceptance remain pending. No owner
phone, release version, or publication was involved.
