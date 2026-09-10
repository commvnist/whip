# Clearer workout execution through a complete cycle — 2026-09-10

FND-20260910-029 / DEC, IMP and VER-20260910-020. Starting production `b4482e2e`. Normal API 34 phone and API 37 wide only.

NEXT now shows the upcoming Exercise/Set and practical target with a navigation arrow. Detailed prescription provenance stays in the Set. Removing redundant Up next and shortening workout-only scope bring actual inputs earlier. New program generation preserves authored notes exactly instead of inserting instructions that repeat in every workout and History. Existing saved notes remain untouched.

The existing execution builder now owns one named menu anchor for active/passive Sets; features keep their popup, state and exact commands. Shared disclosure buttons own their accessible name. Workout navigation accounts for all leading list items and measures the pinned execution lane, so Exercise/Set controls appear below it. This evolves existing responsibilities without changing programming, persistence, schema, backup or historical records.

## Verification

- `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted gym gym531 --android com.whip.app.ui.InteractionControlUiTest --emulator`: 116 JVM tests in nine suites and 226 fresh Android tests across three batches, zero failures/errors/skips. Run `rlQY4S`, zero reused checks.
- API 37 direct instrumentation: complete programmed cycle, prescribed-rest/override/recovery journey, and skipped-Set restore/log/History journey pass, three methods in 141.139 seconds. The same production, Android tests and APKs are used on both platforms.
- `scripts/check --ready`: passes 378 JVM checks in 42 suites, Android compilation, lint, debug packaging and static/harness checks; final Gradle build 2m40s. The first run rejected a whitespace-sensitive architecture assertion; only that JVM assertion changes after runtime regression. All 511 inputs are compared in `final-hash-check.json`; production, Android fixtures and APKs remain identical.
- `scripts/ui-catalog lint`: 505 required states, zero pending selectors/platform exceptions. Source inventory is 653 JVM + 1081 Android = 1734, distinct from executed counts.

The full native cycle uses Custom Bench TM 100 and default increase 2.5. All four phases are logged. Unconfirmed Hold survives Activity recreation without advancing the workout or writing a decision. Explicit Standard saves 100 → 102.5, cycle 2 resolves its first Set to 67.5 kg, and all earlier finished Sessions, completed Sets and the immutable decision remain exactly equal in reopened History. NEXT also verifies that Complete Set is visible and its menu lies below the pinned lane. The existing rest journey protects automatic/manual durations, reversible overrides, zero rest, exact deadline recovery and performed History.

## Original review and limits

Twenty original PNG/XML pairs are retained and personally reviewed: two unchanged-production phone baseline states; eight complete-cycle states plus automatic rest on each final phone/wide target. All accepted XML contains zero unlabeled interactive nodes. `review.tsv` records each image's scope. PNG bytes are original; readable XML/text normalizes trailing whitespace while gzip archives preserve original bytes. Manifests retain source filenames, run identities and hashes.

The initial workout is clearer, and one Next Set action reveals the first Set's identity, target, load, reps and Complete Set. History retains original performed values without the generated note paragraph. The existing single-Exercise decision dialog remains readable and is retained. Shared wide reading width, repeated setup guidance, and exact navigation within grouped/later Sets remain follow-ups; the current navigation correction targets Exercise blocks. No new process-death, TalkBack, RTL, 200% or final whole-product acceptance is claimed.

## Intermediate evidence

- `Yr3f0E`: unchanged-production complete-cycle baseline passes. Initial fixture compilation incorrectly assumed Sets had a sessionId; it was corrected to use their actual workoutExerciseId relationship before this accepted baseline.
- `iIa5sb` / `yygOzY`: corrected block targeting exposes an unnamed partially clipped Set menu; diagnostic identifies the nine-pixel anchor after its named icon disappears.
- `TWDe9W`: parent-owned menu naming fixes that capture. The full cycle then exposes a fixture snapshot race on restTimerCleanupPending; the final fixture waits for housekeeping before exact equality. The neighboring rest capture still identifies an unnamed disclosure.
- `p11xnt`: the ten-pixel completed-Sets disclosure fragment is unnamed when its child Text is hidden. Shared parent naming and measured scroll offset address the source; capture guards remain intact and diagnostic wrappers are removed.
- `og23Za`: both corrected phone journeys pass. The later final profile also includes the explicit pinned-lane/menu bounds assertion.
- The first readiness run fails one formatting-dependent assertion among 378 JVM checks. Its exact canonical card/tag requirement remains in the whitespace-tolerant replacement; production is unchanged.
