# Whip UX overhaul acceptance - 2026-09-30

Status: implementation and native acceptance in progress. This is the resumed work, not a release receipt or a claim that all twelve acceptance categories passed.

## Execution and preservation

The Windows parent invokes the existing Arch WSL distribution (`wsl.exe -d archlinux --exec`). Source, Gradle and Android tools execute in Arch on beast at `/root/repos/whip`. Branch `main`, starting commit `0030c226507a0f7a8e5222da71e7c52dd38b6e58`; established remote `https://github.com/commvnist/whip.git` was verified at that commit. All 35 initial tracked modifications and the paused QA handoff were preserved and integrated.

Exactly three GPT-6.1 Sol/high overhaul lanes run: integration/shared design, routines/tasks, and goals/habits. No replacement diagnostic worker is running.

| Lane | AVD | Serial | Proven native use |
| --- | --- | --- | --- |
| Integration | whip_api34 | emulator-5554 | Home/Tasks/Habits/Goals/Tracks/Gym taps, original PNG/XML, shared draft/import/widget tests |
| Routines/tasks | whip_api34_parallel | emulator-5556 | Home/Gym/Library/Routines/Builder and pending-subtask Cancel/Keep Editing, native failure/recreation journeys |
| Goals/habits | whip_api34_qa3 | emulator-5558 | Recommended setup/Goals/Habits, retained legacy weight observations through upgrade, native period/recreation journeys |

All three reported adb device, qemu=1, API34 and sys.boot_completed=1. Every operation names its serial. The owner phone was not installed, instrumented or reset.

The dated pre-resume debug artifact is **0.3.90-debug/code96**, SHA256 `6b2d4f18dce889025a9dfcfb4f925424eab91837b1ae84ee9ef4ddc62438f44e`; it predates current HEAD and is not presented as an exact HEAD build. The first immutable changed pair is in `build/qa-overhaul/2026-09-30/batch-one/`: app SHA256 `06133a699159291e0fa687192ae4bf99f1839042ab1c59292abcbd30a98354d2`, test APK `97ac356e72a3138c0d955e23feca5fce9ae4702e1b5cc6f404e5adb4fd1bb707`, **0.3.91-debug/code97**. Later widget and final-day Habit changes require the later frozen acceptance package.

## Design contract and native visual checkpoint

Reuse the existing warm neutral theme, typography, content columns, safe insets, chrome, cards, fields and dialog/action owners. Preserve recognizability and functions. Ordinary tab changes restore their workspace; explicit daily shortcuts select Today/Workout. Changed authoring drafts receive Keep Editing/Discard; submitted input has one owned receipt, protected navigation, retained errors and deliberate recovery.

Original same-device normal screenshots confirm matching toolbar, workspace-tab, content-edge and bottom-navigation positions across productivity tabs. Gym keeps its deliberate unscoped title and four workspace jobs. No evidence justified replacing the global theme or inventing new navigation.

A concrete inconsistency was repaired through existing `WhipEmptyState`: empty Habits and Today now offer **Create Habit**, matching Goals' template/direct-create organization. See the [native visual evidence](../../artifacts/ux-overhaul/2026-09-30/README.md). Native measured cross-tab/wide/large-text acceptance remains part of the final campaign.

## Acceptance checklist

| Category | Implemented/verified evidence so far | Remaining acceptance |
| --- | --- | --- |
| 1. Headers, content edges, spacing, toolbars and actions | Same-device major-tab originals; consistent shared chrome; routines lane normal/200% geometry methods passed | Compact cross-tab geometry, wide columns and enlarged tab labels passed together (3/3 on emulator-5554); final screenshot review remains |
| 2. Shared type/colors/icons/cards/fields/dialogs/selections | Existing shared visual language retained; direct Habit action uses existing empty-state owner | Dark/large-text/keyboard originals and aggregate regressions |
| 3. Real code/style reuse | Shared precise Habit target summary reused by widget; one widget mutation/result owner for Task/Habit; Task editor state registration moved to existing host | Final caller review and regression gate |
| 4. Add/edit/save/cancel/delete/Back | Pending subtask belongs to dirty/save; taxonomy draft guards; Routine/Workout/rest-setting/emoji owned receipts | Final delayed/failing/recreated and child-return tests |
| 5. Navigation/labels/grouping | Daily Home intent separated from retained tab changes; direct Habit creation; exact platform date/Area/session routes; truthful history/export copy | New real platform-entry journeys plus Review/Track navigation regression |
| 6. Transitions/scroll/selections/drafts | Existing workspace holder retained; Settings/Area parent list states retained; completed Task/Track child editors preserve inspector context | Final restored scope/scroll and repeat/Back acceptance |
| 7. Validation/progress/error/success/edit protection | Named receipt owners, preserved failures, accurate committed import warning, inactive Goal fields normalized | Final native failure injection and retry tests |
| 8. Loading/empty/error/retry/disabled/completed | No false source absence while loading; named settled-unavailable messages; distinct widget failed/saved-warning/stale states | Frozen loading/error and source-retry acceptance |
| 9. Contrast/touch/labels/large text/keyboard | Nine native widget regressions passed incl precise 0.001 action, upper bound/date semantics and extreme text; routine large-text input passed | Final normal/200% short-height keyboard and native a11y regressions; spoken certification not claimed |
| 10. Bugs and data safety | Direction-aware goals; phase-local routine tools; durable saves; committed import; one discrete Habit period outcome; finite/future evidence and Today retention | Final consolidated JVM/native evidence, no data migration/reset on phone |
| 11. Native normal/repeat/interruption/Back/recreation/cross-tab | Actual native use on all three distinct devices, immutable-package source/result provenance | Frozen complete native inventory and exact adverse-case replays |
| 12. Before/after/regressions/commits/push | Selected original PNG/XML pairs retained in repository; first failures and package hashes retained | Aggregate pass, final issue dispositions, commits/push/remote verification |

## Current verified batches and first failures

- Integration production compile, Android-test compile, JaCoCo instrumentation and APK packaging passed. JaCoCo initially rejected WhipScreen's oversized method; moving its existing Task editor state registration to the existing route host repaired it without excluding code or disabling coverage.
- First integration native invocation: nine real methods passed plus one wrong-package initialization failure. The corrected Area draft test then passed separately. The original failed log remains preserved.
- Shared widget native batch: **9/9 passed**, including injected write failure versus postcommit reminder warning, stale/generation safety, precise extreme-text actions, contrast palettes and cached-refresh regression.
- Routines first immutable native batch: **14 run, 12 passed, 2 failed**. Failures were a test toggling already-expanded power fields closed and a History test toggling a retained expanded card closed. Corrections preserve the assertions and require replay.
- Goals/habits first new journey batch: **3 run, 2 passed, 1 failed** at an unscoped Ready-today visibility assertion. A scoped visible-inspector wait preserves the requirement; final replay is pending.
- Goals/habits existing native batch: **17 run, 12 passed, 5 failed**. Exact percentage checks need the visible percentage owner, inspector completion must exclude underlying manual card actions, and direct checked checklist rows remain usable independently of collapsed card details. Precise repairs/replays are pending.
- Focused Habit/widget JVM batch: **44 run, 43 passed, 1 failed** because the monthly-quota fixture crossed Sep30->Oct1. The exact corrected within-period method passed; final complete JVM evidence is pending.

Goal manual originals retain both prior measurements: target75 with no valid baseline now reports honest current value and correct target attainment without an invented percentage; authored baseline100 yields40% at90 and120% at70. **Manual Complete Goal remains available independently of attainment**; Target Reached and the suggested completion card use directional truth.

## Limits

Three disposable API34 emulators do not certify physical fold/OEM delivery, every locale/DST edge, TalkBack speech/Switch Access or killed-process recovery of every operation. Compose restoration, actual app recreation, controlled receipt failures and scoped native snapshots are reported separately. No Play candidate, owner-phone build, release, deployment or destructive repository operation is authorized by this acceptance.

## Stable-batch checkpoint

The second immutable pair built successfully in6s, app SHA256 `f7a19e1318b9e2d2f63c1386f82dfddb7892e69064e430555a137e03499a3498`, test `926fe0936d4413fbb161d911e685dcc2f4840d9f92a4ccc73b69f2b38e4ac18e`. Its source manifest SHA256 is `82e14f2ed85e498dc3ba6be36dc39fae14f17f3b114dfaaf3540958a9a42ac75`. Later finite flexible streak and precise native fixture corrections require the final pair.

On emulator-5554, AdaptiveWhipScreenTest passed all three selected native methods: exact shared chrome across five workspaces and their jobs, expanded content-column alignment, and enlarged compact tab labels. The first integration invocation had two initialization failures (two Platform methods inferred non-Unit returns; wrong Adaptive class package) and one completed-Task fixture tag failure. Unit returns and the actual Schedule tag were corrected; the original failed log is retained. No product failure is inferred from those fixture errors.

Implementation is frozen for aggregate verification rather than growing scope. The first commit/push requires corrected native replays, the stable readiness gate and complete local JVM/lint/build/static gate. Full fresh Android inventory on all three emulators remains necessary for full-overhaul acceptance; a commit checkpoint alone does not declare that acceptance complete.

## Local gate and root native checkpoint

Full local gate passed in5m18s:754/754 JVM tests, zero failures/errors/skips, domain lines84.82%, branches65.38%, core settings/policy lines69.49%, lint, Android compile, debug/release APK and bundle, benchmark build, brand/manifest/data-safety/diff guards. The earlier full run had three failures (stale E2E evidence reference at HEAD, shared-header architecture location, missing localized Workout failure fallback); repaired without removing assertions. Final readiness passes including real Gradle target probes and exact three-emulator distribution/cache/failure fixtures. The55s readiness attempt timed out and was incomplete; the explicit comprehensive run completed.

Root final7 methods passed4/7 with three selector failures (duplicate background timer title, Cancel is the editor header action, Create Habit title conflicts with the new background direct-create action). Exact corrected replay passed3/3 on the same production APK: authoritative out-of-Area timer/saved scope, frozen checklist plus completed Task selected-section return, and actual200% Habit editor header/exit with software keyboard. Existing Goal and Task editor large-text methods passed in the earlier4. Captures preceding the IME visibility assertion are labelled before-IME; no screenshot containing no keyboard is represented as keyboard proof.

GitHub reports zero Actions workflows and zero runs for the intended repository. Push verification can confirm remote commit identity; no CI execution can be claimed.

## First validated improvement batch

All 754 JVM methods now have preserved fresh XML (124 suites, zero failures/errors/skips) in `build/qa-overhaul/2026-09-30/all-jvm-accepted/`; `all-jvm-fresh-accepted.log` records the actual forced test-task execution. A cache-restored receipt and the earlier failed run are separate logs. Final native-fixture lint passes after all gesture changes.

Goals/Habits:21 unique methods accepted on the final production APK across19 final-run passes plus2 corrected exact replays. Routine/Gym/Task lane:18 unique methods accepted across packages, with the final2/2 confirming six durable scheme attempts and actual atomic empty-workout creation/recreation. Root normal/wide/enlarged geometry3/3, widget9/9, original date/session2/2 and exact corrected timer/Task-return/Habit-IME3/3 pass. This is composite focused evidence, not a fabricated single aggregate run.

The final production APK SHA256 is `cb5c6169bff1f01c2c17cd379126a3c4c243ffc2f11a81b5820e49baf1f69cc3`; final test APK `8b58e568b43844afebb778c0e3210e40b464cac056cd1eb7bdba3ffb81c281c3`; source manifest `ef050da5127793acf98488143289e5038117b69c17f1e9eeb78e8cad92635f29`. All three devices are booted and released from individual instrumentation for the next campaign.

The first batch is ready for normal commit/push. Remaining full-overhaul acceptance: complete fresh1333-method Android inventory on the three assigned emulators; specifically transformed multiphase persisted reopen/instantiation, populated Gym Insights return context, cross-unit plate preset and older-than50 Routine import native journeys; final issue/category dispositions. These are tracked acceptance gaps, not new product scope. The owner-phone release receipt's pre-existing trailing-whitespace modification remains untouched and unstaged.
