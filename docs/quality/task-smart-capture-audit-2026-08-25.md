# Smart Task Capture audit · 2026-08-25

## Finding

Smart Task Capture was opt-in and local, but its automated evidence stopped at three parser unit tests. The real setting, quick-capture surface, editor application, and saved repository result were not connected by an end-to-end test. The interface also showed no interpretation while the user typed. Direct Quick Capture remained literal even after the setting was enabled, while the editor only explained the result after a generic apply action. This made the setting's cause and effect unclear.

## Interaction contract

- Smart Task Capture remains disabled by default and all parsing stays on-device.
- When enabled, only exact recognized source phrases receive a contrast-safe inline highlight. A preview immediately labels the resulting Schedule, Repeat, and Deadline assumptions.
- Quick Capture applies exactly those visible assumptions when Add is selected. If no supported phrase is highlighted, destination defaults and the literal title are preserved.
- Add Details remains review-first: highlighting and the preview do not mutate the draft. **Apply Highlighted Details** performs the disclosed changes.
- The setting expands to show realistic examples and states that only highlighted text is interpreted.
- Invalid dates, zero/invalid intervals, possessives such as `today’s`, and a Deadline before the scheduled start remain literal. When multiple schedule phrases compete, only the first is assumed and highlighted.
- Recurrence and a separate final Deadline can be applied together without the editor silently dropping the Deadline.

## Supported deterministic language

- Relative schedule: `today`, `tomorrow`, `next Friday`.
- Explicit schedule: `on 2026-09-01` or `work 2026-09-01`.
- Repeat interval: `every day`, `every 2 weeks`, and corresponding month/year units.
- Selected weekdays: `every Monday, Wednesday and Friday`.
- Separate final date: `deadline 2026-09-05` or `due 2026-09-05`.

## Automated evidence

- `TaskQuickCaptureParserTest`: output, exact source ranges, interpretations, recurrence, Deadline, invalid dates/intervals, possessives, competing dates, invalid date ordering, punctuation, and next-weekday boundaries.
- `TaskWorkspacePolicyTest`: destination defaults remain literal while disabled; enabled capture applies only recognized assumptions to the saved draft.
- `SmartTaskCaptureVisualTransformationTest`: every parser range receives the intended background, foreground, and unchanged identity offset mapping.
- `EditorDependencyUxTest`: the real editor announces and renders all assumptions, requires explicit application, and saves the title, recurrence start, interval, and Deadline.
- `ProductivityCreationJourneyE2ETest`: the real Settings toggle persists, examples appear/disappear, highlighted Quick Capture saves one-time date/Deadline data, disabling restores literal behavior, and Add Details persists a reviewed repeating Task through the repository.

This checkpoint raises the product baseline from 568 to 579 automated tests: 266 fast JVM tests and 313 Android instrumentation tests.

## Release evidence

- `scripts/check --full` passed all 266 JVM tests, lint, coverage ratchets, debug/release APKs, the signed Play bundle, and benchmark packaging.
- Deterministic domain coverage increased to 79.33% lines (2,599/3,276) and 52.65% branches (1,429/2,714); core settings/policy coverage remains above its ratchet at 63.25% (327/517).
- `ANDROID_SERIAL=emulator-5554 scripts/check --emulator` passed all 313 Android instrumentation tests across seven isolated batches with no failures or skips.
- `WHIP_DEVICE=emulator-5554 scripts/device release-deploy` installed and cold-launched signed `0.3.9 (15)` on the disposable API 34 emulator. The local and installed APK SHA-256 both equal `76a369d197f2c8a53949b234bfb7bbaadc95a68bd427a1631fce0975e8647a95`; launch status was `ok` and `MainActivity` remained foregrounded.
