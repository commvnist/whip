# Smart Task Capture audit · 2026-08-25

> Follow-up · 2026-08-26: the opt-in default below has been superseded. Smart
> Capture now migrates on by default, retains an explicit persistent opt-out,
> teaches a weekday example in Quick Capture, and accepts common weekday-list
> variants including abbreviations, plurals, Oxford commas, `&`, `/`, `+`,
> `weekly on`, weekday/weekend groups, and interval weekday schedules.
>
> Follow-up · 2026-08-26: Smart Capture now covers the complete planning intent
> of a Task. Its deterministic, on-device language includes one-time dates,
> 12/24-hour times, richer calendar and completion-anchored recurrence,
> recurrence endings, natural deadlines, reminders and offsets, priority,
> duration, effort, and `#tags`. Quick Capture and Add Details persist the same
> highlighted interpretation. Generic examples teach useful combinations while
> explicit false-positive tests keep ordinary phrases literal.

## Finding

Smart Task Capture was opt-in and local, but its automated evidence stopped at three parser unit tests. The real setting, quick-capture surface, editor application, and saved repository result were not connected by an end-to-end test. The interface also showed no interpretation while the user typed. Direct Quick Capture remained literal even after the setting was enabled, while the editor only explained the result after a generic apply action. This made the setting's cause and effect unclear.

## Interaction contract

- Smart Task Capture is enabled by default, retains an explicit persistent opt-out, and all parsing stays on-device.
- When enabled, only exact recognized source phrases receive a contrast-safe inline highlight. A preview immediately labels the resulting schedule, time, repeat, repeat ending, Deadline, reminder, priority, duration, effort, and tag assumptions.
- Quick Capture applies exactly those visible assumptions when Add is selected. If no supported phrase is highlighted, destination defaults and the literal title are preserved.
- Add Details remains review-first: highlighting and the preview do not mutate the draft. **Apply Highlighted Details** performs the disclosed changes.
- The setting expands to show realistic examples and states that only highlighted text is interpreted.
- Invalid dates, zero/invalid intervals, possessives such as `today’s`, and a Deadline before the scheduled start remain literal. When multiple schedule phrases compete, only the first is assumed and highlighted.
- Every highlighted detail is applied consistently by direct Quick Capture and the review-first editor; recurrence and a separate final Deadline can coexist without data loss.

## Supported deterministic language

- One-time schedule: `today`, `tomorrow`, `on Friday`, `this Tuesday`, `next Friday`, `in 3 weeks`, `on Sep 5`, or `starting 2026-09-01`.
- Time: `at 9am`, `9:30 p.m.`, `@14:05`, `at noon`, or `at midnight`.
- Repeat cadence: `daily`, `every other week`, `each month after completion`, `monthly on the 31st`, or `yearly on September 5`.
- Selected days: full or short weekday names with commas, `and`, `or`, `&`, `/`, or `+`; also `weekdays`, `weekends`, plurals such as `Mondays and Thursdays`, and `every 2 weeks on Mon & Thu`.
- Repeat ending: `until Dec 31` or `for 10 occurrences`.
- Deadline: `due tomorrow`, `by next Friday`, `deadline Sep 5`, or `due in 2 months`.
- Reminder: `with reminder`, `remind me`, or `remind me 30m before`, provided a scheduled time is present.
- Planning metadata: `!high`, `priority: urgent`, `for 45m`, `duration 1h 30m`, `light effort`, and one or more `#tags`.

## Automated evidence

- `TaskQuickCaptureParserTest`: output, exact source ranges, interpretations, schedule/date/time variants, recurrence and endings, Deadline, reminders, priority, duration, effort, tags, invalid values, possessives, competing dates, invalid ordering, punctuation, and deliberate false positives.
- `TaskWorkspacePolicyTest`: destination defaults remain literal while disabled; enabled capture applies every recognized detail to the saved draft.
- `SmartTaskCaptureVisualTransformationTest`: every parser range receives the intended background, foreground, and unchanged identity offset mapping.
- `EditorDependencyUxTest`: the real editor announces and renders all assumption types, requires explicit application, and saves the complete interpreted draft.
- `ProductivityCreationJourneyE2ETest`: the real Settings toggle persists, examples appear/disappear, highlighted Quick Capture saves complete planning metadata through the repository, disabling restores literal behavior, and Add Details persists a reviewed repeating Task.

This checkpoint raises the product baseline from 568 to 579 automated tests: 266 fast JVM tests and 313 Android instrumentation tests.

## Release evidence

- `scripts/check --full` passed all 266 JVM tests, lint, coverage ratchets, debug/release APKs, the signed Play bundle, and benchmark packaging.
- Deterministic domain coverage increased to 79.33% lines (2,599/3,276) and 52.65% branches (1,429/2,714); core settings/policy coverage remains above its ratchet at 63.25% (327/517).
- `ANDROID_SERIAL=emulator-5554 scripts/check --emulator` passed all 313 Android instrumentation tests across seven isolated batches with no failures or skips.
- `WHIP_DEVICE=emulator-5554 scripts/device release-deploy` installed and cold-launched signed `0.3.9 (15)` on the disposable API 34 emulator. The local and installed APK SHA-256 both equal `76a369d197f2c8a53949b234bfb7bbaadc95a68bd427a1631fce0975e8647a95`; launch status was `ok` and `MainActivity` remained foregrounded.

### Planning-language expansion checkpoint · 2026-08-26

- The product baseline is now 603 automated tests: 284 fast JVM tests and 319 Android instrumentation tests.
- `scripts/check --full` passed the complete JVM suite, lint, coverage ratchets, debug/release APKs, signed Play bundle, and benchmark packaging. Deterministic domain coverage is 81.04% lines (2,996/3,697) and 54.80% branches (1,696/3,095); core settings/policy coverage is 63.67% lines (333/523).
- `ANDROID_SERIAL=emulator-5554 scripts/check --emulator` passed all 319 Android instrumentation tests across seven isolated batches with no failures or skips. Focused editor and real persistence journeys also passed independently before the full run.
- Signed `0.3.13 (19)` was installed and cold-launched on the authorized physical Fold without running instrumentation or clearing user data. The local and installed APK SHA-256 both equal `0df24919e1b1fbcf0bc47f1b40fc642a9274a3a3f338fee0de5fea99d9282c48`; `MainActivity` was verified foregrounded.
- The Play release bundle is `artifacts/releases/whip-0.3.13-19.aab` (9,494,496 bytes; SHA-256 `8d37d152c3135e1d45c480951f28c4dc27f25dfbec198056ef3f81462742ce12`).
