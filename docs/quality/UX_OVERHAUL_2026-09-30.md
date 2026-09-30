# Whip UX overhaul acceptance - 2026-09-30

Status: implementation and native acceptance in progress. This is the resumed work, not a release receipt or a claim that all twelve acceptance categories passed.

## Latest regression checkpoint

The second complete-inventory attempt, vPxC4e, is rejected: completed batches contain 298 methods, 291 passing and seven failing, with zero skips. Only its first two batches were accepted (46 + 124 = 170); a later partially executed batch is incomplete. Original XML and logs remain under build/instrumentation-results-vPxC4e and full-fresh-android-replacement.log. Its verified dedicated campaign process group was stopped before fixture edits; emulator processes, shared adb and the owner phone were untouched.

The seven failures traced to native gesture/fixture assumptions, not waived domain assertions: three card-center taps hit nested actions instead of opening details; a Task selector matched both inspector and background card; the current-date Calendar cell was lazy and in the preceding month on September 30; two strict clipping fixtures depended on incidental content height. Five test files now address those exact causes. Original identity, save/history, neighbor preservation, completion-once, Task-open, recreation, accessibility-role and strict clipping assertions remain intact.

Fresh native replays from one immutable pair pass **8/8**: integration 2/2 in 109.394s on emulator-5554; routines 2/2 in 66.01s on emulator-5556; goals/habits 4/4 in 148.108s on emulator-5558. All three lanes inspected original captures. Root Calendar/Task [selected originals](../../artifacts/ux-overhaul/2026-09-30/verified/grouped-regression/README.md) retain original PNG bytes and equivalent XML. The Home completion snackbar overlaps the Gym heading in its capture; heading semantics/click assertions pass, but that image alone is not visual heading proof.

App SHA256: 0672b8b8284f778a1fd0191c577f286537bd1693edf78adc470124029208e64a (unchanged production). Test SHA256: 89fdca06ebfe66507172c791349b98d628fc9713d12887d8a32974d48020b32f. Source manifest: 0d18f34b52db06eead93637c2ffb2f9ce0e3fe4e9239802fdbb3e51340da981f. Final fixture lint passes 1m32s; affected scripts/check --ready passes. Current-source 759 JVM acceptance and full required local build/coverage gates remain valid; no new JVM execution is claimed for test-only changes.

The third fresh campaign, r5Kv0E, is rejected: accepted batches 1/2/4 contain **216 methods**, zero failures/skips/reuse. Completed batch 3 contains 82 methods with one failure: Home checklist execution attempted performScrollTo on a lazy child before materializing it. Complete batches therefore contain 298 methods, 297 passing and one failing; later partial groups remain unaccepted. Original failed XML and all logs are preserved. The verified dedicated process group 3239622 was stopped before edits; only debug/test fixture apps on the three disposable serials were stopped. No emulator/shared-adb/phone process was killed.

The smallest test-only repair waits for the Home list, then scrolls that actual lazy owner to the checklist and both Goal actions. All original log, no-inspector and exact 0.4 frozen-closure assertions remain unchanged. Fresh **DirectDailyExecutionJourneyTest passes 3/3 in a 54-second native Gradle run** on emulator-5554 (yxXQyZ), zero failures/skips/reuse. Its [Home confirmation original](../../artifacts/ux-overhaul/2026-09-30/verified/grouped-regression/direct.home.goal-completion.png) was inspected: 1 of 1 Habit complete; 40% Goal progress with honest manual-closure warning. The PNG precedes confirmation; final persisted closure is established by the unchanged native assertion.

Production APK remains 0672b8b8284f778a1fd0191c577f286537bd1693edf78adc470124029208e64a. Rebuilt test APK is 81eb49007f2628302e829e41b0d42e693a42ef8a47f50a9bd0ecb4e61ffa69ba; source manifest f4c82e5893416ceee51aaeb88e9659a8fd625991f5b328840829d5719af02900. Final affected readiness passes in 1m04s, including Android-test lint/compile and debug packaging. Fourth fresh campaign KqdBtz has **378 accepted methods** in batches 1/2/3/4/6, zero failures/skips/reuse in those accepted batches. Completed batch 7 contains 99 methods with three ProductivityCardDesignUiTest failures (96 passing); complete groups therefore contain 477 methods, 474 passing and three failing. Complete-inventory acceptance is rejected; original XML and full-fresh-android-fourth.log remain intact.

At 09:55:05 UTC, healthy batches 5 and 9 continue on emulator-5554/5556 at 46/159 and 35/124 with zero failures/skips; logs advanced at 09:54:49 and 09:54:55 UTC. Batch 7's failed worker is terminal on emulator-5558. Main source/tests/scripts/APKs remain frozen. Automatic approval review rejected a campaign process-group stop because the latest instruction keeps healthy tests running and the target state was not freshly verified; **no stop executed**. Fresh read-only evidence confirms the two healthy workers and they are being left running.

Exact failures: checklistHabitSummaryKeepsParentAndEachSubItemInteractive (line648, collapsed count/row expectations conflict with intentional direct controls and expose redundant footer count); goalSummaryKeepsElapsedTimerResetAndMilestoneControls (line932, assertion targets primary wrapper rather than actual Reset action); summaryCardsKeepLongTextReadableAtTwoHundredPercentFontScale (line1034, 48dp Reset bounds assertion lacks its measured value). Read-only owner review preserves direct rows, expanded/inspector count and the 48dp requirement. Native pixel/semantics diagnostics are required before classifying the height failure or accepting any repair. No assertion threshold was lowered and no product/test repair is claimed yet.

Read-only feature-lane reconciliation found no additional concrete material defect. Retained evidence limits: outbound workout share text has JVM coverage but no native Intent-body journey; cycle-review delayed/failure/recreation uses the source-reviewed shared receipt owner without a dedicated injected native variant. These do not certify physical/OEM/TalkBack or every killed-process combination.

| Acceptance categories | Verified current scope | Remaining |
| --- | --- | --- |
| 1-3. Layout, style and reuse | Native cross-tab/normal/wide/200% geometry; shared chrome/empty states, existing receipt and summary owners; originals inspected | Complete native regression |
| 4-6. Actions, navigation and retained state | Owned save/cancel/Back, phase-local editing, parent scroll returns, Track foreground and strict filter/selection recreation | Complete native regression |
| 7-8. Validation and state feedback | Failure/draft protection, committed import warning, stale/retry distinction, settled unavailable and completion states | Complete native regression |
| 9. Accessibility/input | Focused normal/200%/IME, exact 4dp and half-clipped fixtures, named actions/roles and preserved keyboard bounds | Complete native regression; no TalkBack speech/OEM certification |
| 10. Data correctness | Fresh759 JVM with coverage floors; focused finite periods, goal direction, routine save/phase, import and neighbor preservation | Complete native regression |
| 11-12. Native evidence and delivery | Three actual devices; all eight failed-journey replays pass; preserved originals and prior failures; three verified pushes | Full 1342 accounting, final disposition/commit/remote/CI receipt |

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

## Landed batch and finite-caller follow-up

The first validated batch is normally pushed as [4f534f858b64e3478a4c21f9f6720ff54dc2dcaa](https://github.com/commvnist/whip/commit/4f534f858b64e3478a4c21f9f6720ff54dc2dcaa). Remote hash matches. GitHub API for this exact commit reports zero check-runs, zero Actions runs and zero repository workflows; no CI success is claimed. Earlier ready-for-push wording above is historical.

Final actual-caller review repaired three sibling H1 projections: widgets used raw open-period success/future logs and dropped the final earning day; task planning repeated those decisions; flexible completion rates accumulated misses after ending. Existing authoritative day state and final active date now own those decisions. Timers remain reachable; ongoing/unfinished/recent-window semantics stay covered.

Fresh targeted JVM execution passed50/50 in51s: HabitRules34, WidgetContent11, ProductivityExperience5; zero failures/errors/skips. Two preceding missing-import compile attempts remain separate and unaccepted. Current declared inventory is759 JVM plus1337 Android tests; counts alone do not prove execution.

Frozen follow-up SHA256: app80390d4d580186ce6faeec466c3ad0a1155f0350239907ea1eb1db59c0936099, test7ede0a96e3217488015cc45062f259b02543256ccd4e9bbfbaf80034ae725d5e, source manifestbf25b9b6144848449bc52f188e6f87f45eb7048e2f6f61e4afc9053541cf407b. All three explicit emulator serials remain connected. Comprehensive local gate and four persisted Gym journeys are in progress; full fresh inventory has not started.

### Twelve-category checkpoint (current campaign freeze)

| Category | Verified scope | Remaining gate |
| --- | --- | --- |
| 1. Tab/header/content/action geometry | Shared normal/wide/enlarged geometry3/3; actual six-tab originals | Complete integrated native regression |
| 2. Shared visual controls/states | Existing theme/owners, direct Habit action, precise widgets, visible corrected Track editor | Complete native regression |
| 3. Real reuse | Shared chrome/target summaries/widget receipts/period outcomes; sibling callers reviewed | Complete caller regression |
| 4. Action semantics | Owned Save/Cancel/Back/failure receipts; all four persisted Gym journeys; strict Track return | Complete native regression |
| 5. Organization/navigation | Exact date/session/Area routes; Insights source return and older-workout import accepted | Complete platform/navigation inventory |
| 6. Transition/state retention | Selected Task context, actual enlarged Settings/Area parent offsets, Track filter/inspector/draft/recreation | Complete native inventory |
| 7. Validation/save/recovery | Precommit failure versus committed warning; precise Goal endpoints; durable authoring | Complete native fault inventory |
| 8. Empty/loading/error/completed | Direct-create/settled-unavailable/retry/final Habit day; Review older-window and archived count | Complete state inventory |
| 9. Accessibility/font/keyboard | Actual200% keyboard, extreme widget text, contrast/touch assertions, both enlarged parent returns | Complete native inventory; spoken/OEM certification outside evidence |
| 10. Correctness/data safety | Fresh759 JVM/124 suites, all coverage floors/lint/required builds/static gates;50 focused caller regressions | Complete native inventory |
| 11. Real native acceptance | All three assigned devices used; all nine new follow-up journeys accepted; normal/repeat/Back/recreation evidence | Fresh1342-method three-device campaign running |
| 12. Evidence/commit/push | Original galleries/failure provenance; first commit/remote match; final readiness passes3m08s | Full campaign receipt, follow-up commit/push and exact remote verification |

Follow-up complete local gate passes in3m26s:759 JVM tests with fresh preserved XML; zero failures/errors/skips. Domain lines84.82%, branches65.39%, core69.49%; lint/Android compile/debug/release/bundle/benchmark/brand/manifest/data-safety/diff gates pass. Four new native journeys first failed at lazy scrolling/shared destination selector; assertions and first logs retained; minimal fixture correction/replay is pending.

Current frozen source inventory:759 JVM plus1342 Android (2101 total). Final fixture compilation and lint pass44s; final affected readiness passes. The four persisted Gym follow-up methods are now fully accepted across focused runs on unchanged production APK80390d4d...; phase endpoint one-method replay passes50.004s. Both native200% parent returns pass on5554, with unchanged parent viewport PNG bytes after each round trip. Settings first failed only at an incorrect About test tag, corrected to actual About Whip; Area passed initially. Review older-window correlation passes; Review archive/point capture-name error and Track immediate-restoration assertion remain pending exact replay. Full fresh1342-method inventory remains pending.


### Native-discovered Track integration repair

The strict Entries/Activity edit journey exposed a visible layered-window defect: the retained inspector remained above the editor after recreation. Existing editor-open state now suppresses all three Track inspector hosts while preserving their selected IDs. Original pixels and displayed draft assertions verify the editor stays in front, including duplicate resolution and recreation.

The preserved replay then exposed a separate Activity return defect: after recreation and Save, the loaded Activity had lost both its filter and inspector. A ten-second settlement still failed; this is a reproduced product issue, not a waived timing assertion. Activity uniquely created its existing workspace saved-state holder inside responsive layout subcomposition. Moving that unchanged holder alongside the collection holder aligns its restoration ownership with working Entries. The same unchanged strict test is being replayed; acceptance remains pending until its saved record, query, inspector and subsequent recreation all pass.

The current pre-holder full local gate passed in3m16s: fresh759 JVM methods in124 suites, no failures/errors/skips; unchanged coverage floors passed (domain84.82% lines/65.39% branches, core69.49%), lint, all required compile/build/static gates. XML remains separately archived before targeted gates overwrite working reports. The holder change compiled in13s and requires final affected readiness and native regression. Review archived-workout count/single-point chart now passes its exact native replay; the older-window correlation already passed. Full fresh1342-method native inventory has not started.


### Track restoration accepted; final campaign freeze

The holder-only replay still failed and remains unaccepted. Hoisting the five existing Activity filter/selection states to TrackAreaContent, with a retained lazy-list state, resolved the reproduced loss without another navigation framework. The unchanged strict Track method passes1/1 in60.175s on5558: actual duplicate Keep Editing, visible draft fields before/after Activity recreation, Entries Cancel/return, Activity Save/return and another recreation, exact filter/selection, exactly two saved records and an unchanged sibling Entry. App SHA2560672b8b8284f778a1fd0191c577f286537bd1693edf78adc470124029208e64a; unchanged strict testbaf2783b482426658656d93e57d7c87a2a7f0356cad99e886ee8c228424d434f; source manifest2cba357865408d0e5a6922854d4684775a42ca57d25448ed1bbf36f8dc091f07.

All nine new follow-up native journeys now have focused acceptance (four Gym, two enlarged parent returns, two Review and one Track); complete inventory regression remains required. Original Track foreground before/after pixels were personally reviewed by integration. See the [Review/Track receipt](REVIEW_TRACK_ACCEPTANCE_2026-09-30.md) and [selected originals](../../artifacts/ux-overhaul/2026-09-30/verified/review-track-follow-up/README.md). The following full campaign is frozen against production/test/build/script/APK mutations; documentation and selected evidence can be reconciled independently.


Full fresh native campaign started on explicit5554/5556/5558 after final affected readiness passed3m08s. No execution reuse; source and APK inputs frozen. Independent read-only lane review found no concrete regression or stale sibling caller in the finite Habit/Track changes. Current campaign acceptance is pending, separately from focused passes.


### Second verified improvement batch landed

The focused Habit/Track follow-up and nine native acceptance journeys are normally pushed as [1ad6aaad98fa4a3fc6337568814538b9512041bb](https://github.com/commvnist/whip/commit/1ad6aaad98fa4a3fc6337568814538b9512041bb); remote main matched exactly. Source/APK signatures remain frozen during the already-running full campaign; committing source metadata does not change its inputs. Only the pre-existing phone signature receipt modification remains unstaged.

Exact-commit GitHub API reports zero Actions runs, zero check-runs and zero commit statuses. Combined pending with zero entries means no hosted CI recorded, not an active job or a failure. Local gate/native evidence is reported separately. The full fresh campaign accepted its graphics batch46/46 with zero failures/skips, then began three ordinary workers. Its complete1342-method acceptance is still pending.


### First complete-inventory attempt rejected

Fresh campaign8a57d8 accepted the46-method graphics batch with zero failures/skips, then found two DataPrivacyJourneyE2ETest failures: unscoped exact Unscheduled text matched three nodes while selecting the Task collection scope. Both privacy/backup methods failed before their final visible-record checks. The campaign was controlled-stopped by its verified dedicated process group; only disposable debug/test app fixture processes on5554/5556/5558 were stopped. Emulators/shared adb/owner phone remained untouched. Partial ordinary groups and preserved failure logs are not complete-inventory acceptance.

The shared test helper now selects the exact label under the actual Popup owner for scope and layout menus, following existing Track journey gesture conventions. All final records/settings/theme/recreation assertions remain intact. Exact privacy replay and a replacement full fresh campaign are required; no passing result is inferred from the selector correction.


### Replacement campaign current-source gate

The corrected shared Popup gesture passes all8 DataPrivacyJourneyE2ETest methods fresh on5556 in3m45s, zero failures/skips/reuse (DTcVpZ); it preserves exact imported/local records, settings, cancelled reset and visible reopened tasks. The complete local gate passes in1m42s. Current-source fresh759 JVM methods/124 suites executed during final Track-owner readiness (after that source edit); their XML is archived in final-current-jvm-accepted. The full gate legitimately reuses those exact current-source results rather than claiming another execution. Coverage floors remain84.82%/65.39%/69.49%; required lint/build/static checks pass.

Exactly one replacement full fresh1342-method campaign started after prior processes ended. Immutable inputs: app0672b8b8284f778a1fd0191c577f286537bd1693edf78adc470124029208e64a; testa2bdfdeaeb81e3e0b79dff67f2de96d2d5d794aff67ab40c355c12c1f3599a14; source manifesta321cbe041d5b02c4b2f4c25a1475508d8bc46462165177b20a76c8cbbe28ca4. Original failed campaign and targeted accepted replay remain separate logs. Full replacement acceptance remains pending.
