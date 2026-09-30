# Design, coherence and UX audit evidence — 2026-09-29

Product baseline: `6d79bd1f82da9c1666960834410b3e08ed6d3ea1`. Owner's local date: September29, America/Toronto; XML execution timestamps are September30 UTC. Audit only: no application/test/schema change, owner-phone instrumentation or deployment. Reviewers use current source and distinguish older originals from new evidence. [Consolidated report](../../../docs/quality/DESIGN_COHERENCE_UX_AUDIT_2026-09-29.md).

## Environment and artifact

Two disposable Android14/API34 emulators were used: `emulator-5554` and `emulator-5556`. No owner device was connected or touched. Native runners use `scripts/android-target-guard` through the existing instrumentation engine. Their fixtures intentionally reset disposable emulator app data; synthetic manual records contain only audit names. The parent restored manual font scale to1.0 and screen size to1080×2400. Wide Home used2400×1600 at the same density; this is an emulator configuration, not physical folding hardware.

- Package `commvne.com.whip.app.debug`, `0.3.90-debug`, versionCode96, minSdk26.
- Built APK `app/build/outputs/apk/debug/app-debug.apk` SHA256: `7c38c71ddc77385f2c2bf33af333b9396dca9dc91e893c3113dfe5cbb496dcae`.
- This debug hash is not the signed private release hash. No new release was created.

## Fresh executed checks

Every command used this exact pattern (single class/method):

```sh
timeout --kill-after=3s 55s env ANDROID_SERIAL=SERIAL scripts/android-test-engine --mode targeted --class 'SELECTOR' --evidence-dir artifacts/coherence-audit/2026-09-29/fresh/DIRECTORY
```

| Directory / device | Exact selector after `com.whip.app.` | Result | XML aggregate duration | Executed scope |
|---|---|---|---|---|
| [productivity](fresh/productivity),5554 | `VisualCatalogPagesTest#captureProductivityWorkspaceOverview` | PASS1 |19.568s | Fresh dark Home/Tasks/Habits/Goals roots; shared productivity chrome equality. |
| [supporting](fresh/supporting),5556 | `VisualCatalogPagesTest#captureSupportingWorkspaceOverview` | PASS1 |18.268s | Fresh dark Tracks/Gym/Settings roots and shared chrome. |
| [home-native200](fresh/home-native200),5556 | `DeepSharedJourneyTest#denseHomeContinuationRemainsReachableAtLargeText` | PASS1 |15.038s | Actual200% dense Home preview balance, completion refill and continuations. |
| [wide-geometry](fresh/wide-geometry),5556 | `AdaptiveWhipScreenTest#expandedWorkspacesAlignChromeWithTheirDeclaredContentColumn` | PASS1 |15.209s | Exact declared content-column/chrome bounds under simulated expanded Compose layout. No catalog PNG produced. |
| [shared-shell](fresh/shared-shell),5556 | `VisualCatalogSharedShellTest#captureSharedShellCatalog` | PASS1 |24.974s | Global Add, one scoped Search with IME/match/no match, Review root. |
| [gym-phased-launch](fresh/gym-phased-launch),5556 | `GymPhasedRoutineJourneyE2ETest#routineStartsFromLibraryAndShowsItsActualPhaseInWorkout` | PASS1 |14.852s | Ordinary Custom phased routine Library→Workout and explicit actual phase; active-routine edit guard. Does not test prescription-tool phase preservation. |
| [track-history-native200](fresh/track-history-native200),5556 | `TrackHistoryJourneyE2ETest#archivedHistoryCanBeSearchedRecreatedAndRestoredAtLargeText` | PASS1 |43.090s | Actual200% older archived history/search/IME/no match/query recreation/exact read-only inspector/Restore; thirty synthetic Entries. |
| [platform-area-entry](fresh/platform-area-entry),5556 | `PlatformEntrySurfaceE2ETest#widgetDoesNotShowATemporaryBannerForTheOnlyVisibleArea` | PASS1 |7.317s | Widget Habit-tracking intent selects Today and sole Area without misleading temporary banner. No launcher/widget visual capture or out-of-scope timer proof. |

**Eight distinct methods, eight native invocations, eight passes, zero failures/errors/skips/reused batches, zero timeouts.** Each directory contains copied native `result.xml`, runner `android-aggregate.tsv` and campaign signature. Durations above are XML aggregate durations, not complete wrapper/build wall time. The longest wrapper completed in49s. No JVM suite, full native suite, coverage gate, lint/candidate or Play release was run as audit acceptance. Source test names in component reports are not additional passes.

## Fresh manual journeys

The parent operated the current debug app through native UI input and saved unmodified `adb screencap` PNG plus `uiautomator dump` XML. Exported XML line endings were normalized from CRLF to LF for repository whitespace checks; XML content and screenshot pixels were preserved. The helper stayed outside the repository. Each listed original was personally inspected; XML names/bounds support the stated conclusion. These are manual observations, not instrumentation method counts.

| Original(s) under [fresh/manual](fresh/manual) | Procedure / observed result | Finding |
|---|---|---|
| `first-run.normal` | Fresh debug install opens readable Welcome; Use Recommended succeeds. | Justified keep, ordinary configuration only. |
| `tasks.pending-subtask`, `tasks.pending-subtask-dismissed` | Edit existing Task; type New Subtask without pressing its Add control; header Cancel exits without unsaved review. | TASK-01's Cancel variant reproduced. Save omission is separately source-proven. |
| `tasks.tomorrow-points-today` | Tasks collection→Calendar→Tomorrow; Selected remains September29(today). | TASK-03 reproduced. |
| `goals.reduce-blank-baseline-90`, `goals.reduce-blank-baseline-70` | Create Reduce target80 with blank starting value; log90 then70.90 displays112.5%, reached/Review;70 displays87.5%, unreached. | G1 both directions reproduced; explicit inverted baseline variant source-only. |
| `shared.home.wide-route-before`, `shared.home.wide-route-after` | Leave Tasks collection Calendar selected; on wide Home tap left Tasks Today:1. Opens Tasks collection Calendar, unlike main daily shortcut. | SH1 Tasks variant reproduced; Habit/Gym variants source-only. |
| `gym.active-empty`, `gym.active-empty-after-create`, `gym.created-exercise-library` | Start empty workout; direct Create New Exercise→save Audit empty route exercise. Workout remains0/0 empty; exercise exists in Library. | GYM-03 reproduced. |
| `settings.index-bottom.native200`, `settings.index-return.native200` | Actual200% index scroll to About→open→Back to Settings. Returns to Appearance & Home rather than prior viewport. | SET-04 reproduced. |
| `habits.large-increment.normal`, `habits.large-increment.native200` | Create Count increment1000000000; Today. XML retains full amount; visible fixed-width button clips it, at200% to+100. | H2 reproduced. Shorter typical increments remain unmeasured. |

Seven grounded findings have fresh defect reproductions; the first-run journey is a keep. This does not certify every variant of those seven defects.

## Original gallery and provenance

There are **36 fresh original PNG/XML pairs**: seven productivity/supporting roots, one native200 Home, four shared-shell surfaces, two phased Gym surfaces, seven native200 Track surfaces and fifteen manual surfaces. Parent inspected each fresh original. Earlier component-review originals remain linked with their actual dates and baseline limitations in the reports; they are not copied into this fresh directory or counted as fresh.

The emulator download directory persists older catalog files. Pulling the shared-shell directory initially included unrelated leftovers; they were excluded before saving this evidence. Only four surfaces actually produced by that exact shared-shell invocation remain in its catalog. Other captures were pulled by exact names. Generated `fresh-files.sha256` records the saved originals and receipts without modifying their pixels.

Semantic XML is not spoken TalkBack output. The wide bounds test is not a physical-fold journey. The sole-area intent method is not a full launcher campaign. This audit has no fresh comprehensive TalkBack/Switch Access, RTL/locale/DST, OEM reminder-delivery, maximal editor short-height/IME, provider-failure or process-death campaign. These are explicitly retained evidence gaps, not hidden passes.

## Documentation validation

The following bounded representative documentation route passed after all reports and memory records were written:

```sh
timeout --kill-after=3s 55s scripts/check --ready --path docs/quality/DESIGN_COHERENCE_UX_AUDIT_2026-09-29.md --path docs/quality/coherence-audit-2026-09-29/shared.md --path docs/product-memory/INDEX.md --path docs/product-memory/USER_FEEDBACK.md --path docs/product-memory/FINDINGS.md --path docs/product-memory/IMPLEMENTATION_LOG.md --path docs/product-memory/VERIFICATION.md --path docs/quality/README.md
```

Result: docs profile, no JVM/Android/static selectors, affected-change readiness passed; no frozen candidate. Evidence binaries/receipts are audit documentation, not changed harness inputs; they were separately validated rather than sending unclassified artifact paths through the all-production fallback.

Independent integrity validation confirms all nine report matrices total784, all grounded finding headings total52, all linked local report/source/artifact targets exist, all36 PNGs have matching XML, and all eight copied native receipts execute exactly one test with zero failures/errors/skips. `git diff --check` passes. Application/test/schema/scripts diffs are empty. This checks audit integrity, not implementation acceptance of the proposed fixes.
