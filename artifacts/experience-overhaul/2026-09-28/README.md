# Complete experience overhaul — evidence

The canonical [plan](../../../docs/quality/EXPERIENCE_OVERHAUL_2026-09-28.md) owns twelve implemented groups and 137 current-source workflow dispositions. FB-20260928-007/008 own scope and repeated-history cleanup. Final integration receipts below distinguish execution from source review; no accepted remedy is deferred.

## Evidence boundaries

- `baseline/` contains direct synthetic-emulator onboarding/Home inspection of the retained pre-enhancement 0.3.81-debug binary. The install receipt distinguishes it from current-source acceptance.
- `verification/` retains exact commands' outputs, including failed compilation and a 55-second incomplete timeout. A timeout is never a passing result.
- `verification/jvm/` contains selected method receipts, not a full class/inventory campaign. `search-benchmark.xml` compares the previous matcher/ranker with a prepared query on identical 2,000-row input. Gym's fixture compares old and new clock work on 200 exercises/120 sets.
- `originals/` contains fresh native screenshots and accessibility hierarchies. Numbered duplicates preserve successive runs; the final gallery below identifies accepted versions. `diagnostic-before/` preserves the initial planner setup-position image. Earlier wide rendering exposed a real taskbar overlap despite passing visible-label assertions; final whole-control bounds and a shared safe-drawing repair address it.
- The physical phone remains on its separately verified 0.3.82 private release. This goal uses only the disposable API 34 emulator, without a phone install or instrumentation.

## Measured CPU work

Acceptance totals: **10 distinct JVM methods passed; 16 distinct Android methods have passing evidence**. Native history contains 24 passing executions and six corrected fixture failures; one setup-only runner attempt executed no method. Passing native wall time is 8.557–42.301 seconds. This is focused acceptance, not a full suite.

| Fixture | Previous median | New median | Verified scope |
| --- | ---: | ---: | --- |
| Search, 2,000 results | 9,193,637 ns | 1,270,196 ns | Same structured matching and rank order, compiled once per search |
| Gym clock, 200 exercises/120 sets | 46,006 ns | 1,695 ns | Same totals, elapsed/rest semantics and data-change recalculation |

These are bounded synthetic CPU observations without pass/fail timing thresholds. They do not establish physical-device frame rate or startup improvement.

## Execution policy

Only exact selected methods/fixtures, each routine invocation bounded with `timeout --kill-after=3s 55s`. Build and device operations are serialized. No full batch, `scripts/check --ready`, candidate campaign or broad coverage claim. Original screenshots and hierarchy inspection supplement assertions; source review of unchanged workflows remains distinct from fresh native execution.

## Reproducible commands and binary ownership

JVM acceptance uses `timeout --kill-after=3s 55s scripts/qa-targeted --jvm EXACT_CLASS.METHOD --jvm-only`; the selected-rules invocation contains eight explicit method filters, never a whole class. Original XML is retained in `verification/jvm/`. Build-only invocations use `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest` under the same timeout; fixture-only follow-through builds only `:app:assembleDebugAndroidTest`.

Each native receipt's first line names its exact selector. The actual invocation is `timeout --kill-after=3s 55s bash -c 'scripts/android-target-guard instrumentation && adb -s emulator-5554 shell am instrument -w -r -e class "SELECTOR" commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner'`, with SDK platform-tools first in PATH and ANDROID_SERIAL explicitly emulator-5554. Success requires `OK (1 test)`, not merely the adb exit code. Every method has a separate invocation and process wall timing. [Native ledger](verification/native-methods.json) retains all passes and failures; [JVM ledger](verification/jvm-methods.json) retains exact selected methods.

Initial accepted feature packaging is `android-package-ready.log` (36s), followed by planner preview/error navigation (15s/14s builds), shared safe drawing (7s), and small fixture-only builds (2–3s). [Final APK hashes](verification/apk-hashes-accepted.txt) and [source/test hashes](verification/source-sha256.txt) identify final bytes. Earlier algorithm/behavior receipts remain valid for their unchanged owners; final shared-inset repeats specifically exercise wider whole-action bounds, short IME/200% planning, Track preview, repeated history editing and backup dialogs. This is incremental acceptance, not a claim that every test ran against every APK.

Both [installed emulator APK hashes](verification/installed-apks.json) match those exact final builds. The [evidence manifest](verification/evidence-sha256.txt) covers raw receipts, originals and metadata; README delivery bookkeeping is excluded to avoid circular hashing. Scoped Git attributes preserve raw XML/log line endings and compiler whitespace without relaxing source/document checks.

## Attempt history

- Production compile initially failed on a missing RoutineExercise import; corrected. One follow-up command exceeded 55 seconds during compilation and is incomplete. A separate compile passed in 46s; selected JVM checks then passed.
- Native fixture packaging caught missing GoalDraft.type and a nullable callback returning Job instead of Unit; both corrected before accepted assembly.
- Initial native Track continuity, Goal milestones and Track preview checks failed on an asynchronous-result wait, wrong History tab tag and an exact-string/prefix mismatch respectively. Corrected fixtures subsequently pass; failed logs remain.
- The wider full-control assertion initially used Android's stale accessibility root, then an unavailable direct text lookup. Three failed fixture attempts remain. The final assertion uses Compose's physical screen position plus the full measured control height, compared with actual platform system insets. It passes and is corroborated by the original screenshot.
- An unavailable optional timing executable and an initial missing pull directory failed before their intended work; setup/pull receipts remain. No timeout, failed fixture or intermediate screenshot is relabelled as passing acceptance.

## Visual assessment and configuration

Normal emulator display is 1080×2400, density 420, font 1.0. Annotated methods set actual Android font 2.0 and restore it. Planner compact tests explicitly use 1080×1600; wider restoration uses 1800×2400 with Android's tablet taskbar. These overrides are restored after acceptance. Light planner/Gym/Settings and dark productivity/search originals were personally inspected by the parent or responsible agent. The domain reports record each assessment and its limits.

Search preserves a readable changed-content result and source scope with the IME open. Planner failure retains the selected task, explains retry and exposes Apply; the short preview reveals its proposal automatically, reaches candidate sixteen with fixed actions, and preserves the capacity field above the IME. The corrected wider card and whole action sit above the taskbar. Habit/Goal cleanup originals retain History/query and surviving original facts; Track retains the corrected oldest row beyond its first 100-row window. The preview uses real required fields with readable sample validation and saves no sample Entries. Gym and backup originals show the revised next-action and protection hierarchies without clipped labels/actions in the selected views.

These checks do not certify every source-review row, process-kill/physical-device behavior, TalkBack speech, RTL, all folds/OEMs, provider durability or a full release candidate. No schema, epoch, backup-format or release-version change occurred.

## Selected inspected originals

| Result | Original |
| --- | --- |
| Habit History stays open after two deletions and recreation | [Habit retained context](<originals/experience.habits.retained-history (1).png>) |
| Goal stale failure, fresh deletion and correction preserve History | [Goal retained context](<originals/experience.goals.retained-history (1).png>) |
| Older Track window and corrected row survive recreation | [Track retained window](<originals/experience.tracks.retained-history (1).png>) |
| Executable Goal milestones and explicit completion opportunity | [Goal milestones](<originals/experience.goals.working-milestones (1).png>) |
| Habit checklist at actual 200% | [Habit checklist](originals/experience.habits.working-checklist.large.png) |
| Direct lookup of original historical facts | [Habit search](originals/experience.habits.history-search.png), [Goal search](originals/experience.goals.history-search.png) |
| Disposable Track form at actual 200% | [Entry Preview](<originals/experience.tracks.entry-preview.large (1).png>) |
| Short planner proposal, last candidate and keyboard | [Preview](<originals/experience.task-day-plan.preview.large (1).png>), [last candidate](<originals/experience.task-day-plan.short-large (2).png>), [keyboard](<originals/experience.task-day-plan.keyboard.large (2).png>) |
| Retained failed plan and corrected wide system insets | [Failure](<originals/experience.task-day-plan.failure (1).png>), [wide restored plan](<originals/experience.task-day-plan.restored (3).png>) |
| Current global search with keyboard | [Search](originals/experience.search.dark-current-content.png) |
| Returning Gym launch choices | [Launchpad](originals/experience.gym.launchpad.native200.png), [first use](originals/experience.gym.first-use.native200.png) |
| Exact routine lookup and selected-day start | [Routine search](originals/experience.gym.routine-search.png), [started workout](originals/experience.gym.routine-started.png) |
| Backup protection and actionable warning | [Unverified](<originals/experience.settings.backup-unverified.native200 (1).png>), [warning](<originals/experience.settings.backup-warning.native200 (1).png>), [verified](<originals/experience.settings.backup-verified.native200 (1).png>) |
| A deep backup operation reveals its settled failure | [Visible result](originals/audit2.settings.deep-operation.failure.png) |

Each PNG has a same-name XML hierarchy. Earlier unsuffixed and numbered originals remain for honest chronology; the wider `(1)` image precedes the system-inset fix and is diagnostic, while `(3)` belongs to the final passing whole-control check. The latest short and preview originals were inspected after the shared repair.
