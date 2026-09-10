# Workout rest consistency — 2026-09-10

FND-20260910-027 / DEC, IMP and VER-20260910-018. Starting HEAD: 9ca0056. Normal API 34 phone and API 37 wide; no release or physical-device operations.

The matched prescription workout previously showed 2:00 beside a Set authored with 90 seconds of rest. It now shows 1:30 and “Next Set rest.” Ready/manual Start and transactional automatic completion share one duration policy: workout override, Set prescription, Exercise default, app default. The existing execution renderer names the ready source. Zero prescribed rest stays zero and disables Start until an override is chosen. “Follow Set rest” clears an override without altering authored defaults or historical Sets.

Automatic rest still belongs to the Set actually completed. Running deadlines stay authoritative when NEXT advances. After Stop, ready/manual rest follows the now-visible next Set; with no next Set it uses the app default unless overridden. Overrides retain the existing Activity saved-state lifecycle. No timer engine, notification delivery, session schema, backup or version change.

## Verification

- `jpSv9t`: 210 fresh API 34 Android tests, zero failures/skips/reuse, in batches of 192 and 18. Command: `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile gym --profile gym531 --jvm com.whip.app.reminders.ReminderWorkerRulesTest --jvm com.whip.app.core.AppSettingsTest --android com.whip.app.EditorStateRecreationTest --android com.whip.app.ui.EditorDependencyUxTest`.
- 133 focused JVM tests in ten suites pass on final production. The subsequent fixture-only rerun reuses the unchanged JVM task; it does not represent 133 newly executed JVM checks.
- API 37: four methods pass in 113.431 seconds: the new native rest journey, full Routine prescription journey, preset/override/reset UI, and existing large-text rest action semantics. A subsequent strengthened Routine recreation check passes in 11.672 seconds. Only that unrelated fixture and the test APK differ between the four-method wide run and the final input snapshot; production and all four target fixtures are identical.
- `scripts/check --ready`: passed in 2m33s, including 373 JVM tests in 41 suites, zero failures/errors/skips, Android compilation, lint, debug packaging and static/harness checks. All 507 final input hashes remain unchanged.
- Catalog lint: 493 required states, zero pending selectors or platform exceptions. Current source inventory: 648 JVM + 1078 Android = 1726; source counts are distinct from executed checks.

The new MainActivity journey follows four Sets with 90/45/0/inherited rest, an Exercise default of 75 seconds and app default of 120 seconds. It proves manual Start, exact deadline/revision recovery, automatic rest from the completed Set, next-Set handoff, a 60-second override through recreation/manual/automatic use, override reset, zero rest, Exercise/app fallback, finish and exact completed-Set preservation in History. The existing Routine journey independently authors a complete 90-second prescription, executes it, edits the later template and verifies unchanged History. Preset editing and reset remain covered by the component behavior test.

## Visual evidence and limits

All 15 retained PNG/XML pairs were personally inspected: two matched before views, five final rest states on each platform, two matched final prescription workouts, and one component-hosted duration dialog. The 14 native pairs establish app context; the component pair proves dialog content and controls within its test host. `review.tsv` gives each scope. The ready source, running actions, reversible override, zero-rest state and original History values are readable at normal scale. Wide screen reading width, sparse context panes and broader programmed-cycle review remain separate work.

PNG bytes are original. Readable XML and text reports use normalized whitespace; accompanying gzip files preserve original bytes. `manifest.json` and `text-normalization.json` record source paths, run/native identities and original/artifact SHA-256 hashes. This is bounded acceptance, not whole-product completion or a new process-death/notification delivery campaign.

## Retained intermediate failures

- `wDPbHU`: two of three methods passed. Manual Start saved the correct duration, then the new fixture found no Compose hierarchy. Its notification permission was unmanaged while Start requests that permission. Explicitly granting it on the disposable emulator resolved the fixture; `Jhsz4n` passes the entire rest journey.
- `0qgkH1`: 191 of 192 methods passed; Routine draft recreation saw an empty field. Its second batch did not run. The unchanged test passed separately on wide in 10.656 seconds. The fixture now verifies the authored text before recreation and keeps the original post-recreation assertion. Both strengthened wide recovery and the complete 210-test phone rerun pass. The initial failure's precise timing cause was not conclusively isolated; it remains diagnostic evidence, not an accepted run or demonstrated persistent data loss.
