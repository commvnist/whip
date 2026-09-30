# Direct daily execution receipt — 2026-09-29

FB-20260929-018 / FND-20260929-043 / DEC-20260929-005 / IMP-20260929-023 / VER-20260929-020. Baseline `ecdb3eee`; [surface-by-surface implementation](../../../docs/quality/DIRECT_DAILY_EXECUTION_2026-09-29.md). Habit, Task, Goal and static routine execution now bypass item expansion. Existing Track capture, workout execution, widgets and notification actions were reviewed and retained.

**Focused behavioral acceptance passes; full readiness is incomplete.** 430 affected JVM methods / 44 suites and 18 distinct selected Android methods pass with zero failures/errors/skips. All native executions are fresh, zero reused. Nine fresh PNG/XML originals were inspected, including actual 200% text. [Original gallery](index.html), [native method inventory](native-results.tsv), [metadata](verification.json), [source manifest](source.sha256) and final XML/logs are retained.

## Exact verification scope

- Affected command: `timeout --kill-after=3s 55s scripts/check --ready`. JVM execution passes; the final invocation reaches the cap during lint. It is not a completed readiness pass. The earlier build attempt also remains incomplete; JaCoCo reported an oversized `WhipScreen` method. Extracting the independent saved Focus launcher fixes instrumentation, proved by bounded `./gradlew :app:jacocoDebug` and successful native packaging/execution.
- Compilation: bounded `./gradlew :app:compileDebugAndroidTestKotlin --console=plain` passes after two fixture compiler errors are corrected. Final focused fixture compilation also passes. Native engine builds both APKs successfully after the final Calendar lazy-list correction.
- Lint: bounded `./gradlew :app:lintDebug --console=plain` and `./gradlew :app:lintAnalyzeDebug --console=plain` reach their caps. Neither is accepted; full lint/readiness remains pending. Logs are in [attempts](attempts/).
- Exact native commands follow the template below. Each row in [native-results.tsv](native-results.tsv) identifies its accepted `CLASS#METHOD`; its native directory contains `command.log`, final `result-1.xml`, aggregate TSV and campaign signature. Device property `whip_api34` is emulator-5554, `whip_api34_parallel` is emulator-5556. Each invocation is individually bounded at 55 seconds.

```sh
timeout --kill-after=3s 55s env ANDROID_SERIAL=emulator-5554 scripts/android-test-engine \
  --mode targeted --class 'CLASS#METHOD' \
  --evidence-dir artifacts/direct-execution/2026-09-29/native/ROW
```

The 18 accepted methods cover every Habit mode on collection/execution cards, persisted Count/manual Duration/Rating/note-only input, unavailable guards and active timer recovery, extreme increments, Task Subtasks/Focus/completion/Reopen, custom Focus validation/replacement, real recurring occurrence identity and recreation, Home persistence, all nine Goal types and inactive states, below-target frozen closure, reached-target logging, normal/200% Goal labels and elapsed units, shared columns, current-calendar Habit execution/recurrence/Tomorrow, and the exact static routine day saved to the active workout.

## Attempt accounting

26 native invocations: **18 accepted, six assertion failures, one rejected nonexistent selector (zero tests), one incomplete Home invocation**. The isolated Home journey subsequently passes. The six corrected failures were two semantics lookups needing the unmerged tree, two fractional-width overflow assertions, and two Calendar fixture lookups (explicit today selection and lazy-list materialization). No product failure is counted as a pass. Geometry checks still require complete label ends, no ellipsis, every glyph extent inside the measured width with at most one pixel rounding, readable height, actual200% scale, bounded action lanes and exact numeric callbacks. Persisted outcome assertions remain unchanged.

Compiler and instrumentation failures, incomplete readiness/lint commands and native failed XML are preserved in [attempts](attempts/). Cumulative accepted evidence uses the same final product APK; fixture-only corrections changed the test APK. Unchanged methods retain their earlier accepted receipts.

## Original provenance and exclusions

Nine originals: normal/200% Habit checklist, extreme increment200%, reached Goal, Goal actions200%, recurring collection, Home Goal confirmation, current-calendar execution and static routine choices. PNG bytes are unchanged; XML line endings are normalized to LF. Only named current captures were selected from the device catalog. The accepted Home capture was the second file (`direct.home.goal-completion (1)`), stored under the canonical gallery name; the first, incomplete attempt is excluded from acceptance.

Debug0.3.90/code96 APK SHA256 `6b2d4f18dce889025a9dfcfb4f925424eab91837b1ae84ee9ef4ddc62438f44e`. Source inventory is 736 JVM + 1312 Android methods, including twelve new Android methods; this is not a full-suite count of executed tests. No phone release, candidate, full native suite, comprehensive TalkBack/OEM campaign or schema/epoch/backup-format change. H2/TASK-02/TASK-03 overlap implemented repairs; the other49 audit findings, including all five P1 items, remain unfixed.
