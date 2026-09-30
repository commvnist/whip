# Whip UX overhaul acceptance - 2026-09-30

Status: bounded implementation and integrated verification complete through phased evidence; closing batch pushed with exact remote match. This is not a new phone release or a single all-green full-inventory run. Historical checkpoints below retain their original failed/pending state and are superseded by the closing checkpoint.

## Closing verification checkpoint

Final affected-class replay ended14:58:17 UTC: **42/42 unique methods passed**, zero failures/errors/skips/reuse, integration18 on5554, catalog10 on5556 and Goals14 on5558. Final required local gate passes;759 JVM methods/124 suites are restored FROM-CACHE, not freshly executed. Coverage remains84.82% lines/65.39% branches/69.49% core. [Exact XML, hashes, gate and reconciliation](../../artifacts/ux-overhaul/2026-09-30/verified/final-regression/README.md) preserve the original full inventory's1337pass/6fail result and intermediate40pass/2fail repair replay.

Production source and APK9a210929 remain identical. Manifest comparison shows exactly five changed test-private fixture files. All42 final method identities belong to the original1343; excluding them leaves1301 unchanged PASS methods. Their union provides phased current-production coverage of all1343, with zero missing identities, overlap, skips or unexecuted methods. **No new single all-green1343-method run is claimed**. Assertions, thresholds and original failed results remain; no additional production defect was found in final independent lane reviews.

[Six inspected current native surfaces](../../artifacts/ux-overhaul/2026-09-30/verified/final-regression/visuals/provenance.json) show common toolbar/content edges and all six bottom tabs at1080x2400; clear Home now has an original with drawn navigation, superseding the earlier capture-only limitation. This supplements existing before/after evidence rather than attributing visual changes to fixture repairs. The separately authorized phone build remains earlier8e8662b6/6f; laterb937 changes are excluded and no further installation occurred.

Closing delivery: [a84683cc](https://github.com/commvnist/whip/commit/a84683cc61623adbb99c613a3dba4b8014b441bc) normally pushed to main, exact remote match. [Delivery receipt](../../artifacts/ux-overhaul/2026-09-30/verified/final-regression/delivery-a84683cc.json) records zero hosted Actions/check/status entries. Combined pending with zero entries is no CI recorded. Prior phone-signature edit remains untouched and unstaged.

## Historical regression checkpoints

At14:12:27UTC, **complete frozen inventory is reconciled:1343 unique methods executed,1337 passed,6 failed,0 skipped,0 not-run,0duplicates**. Original UYOVBs naturally ended1042/1039pass/3fail; untouched groups11/14/16/17/18/19 then contributed301 methods with298pass/3fail, with reset19last. Accepted whole-group gates cover1123 methods; passes inside rejected groups are provisional rather than relabeled accepted. [All1343 original method receipts and exact failure stacks](../../artifacts/ux-overhaul/2026-09-30/verified/final-regression/README.md) preserve the failed attempts. The six classified issues need only five fixture-file corrections: Habit unmerged disclosure selection, Home expected-content readiness, valid Reduce80-to70 conversion setup, actual Upcoming empty-copy, exact empty-Home header/card Review controls, and completed-summary case/count. Production source/APKs remain unchanged. Isolated fixes await all42 affected-class native methods, then final aggregate judgment. No additional phone installation occurred.

At13:42UTC, the original frozen UYOVBs attempt has872 accepted methods, but is rejected by two completed-group failures: ProductivityCreationJourneyE2ETest cannot select the tagged Finished-for-Today disclosure in the merged tree (the original exception reports one matching unmerged node); VisualCatalogPagesTest attempts to find its seeded Home task before its helper establishes task-content readiness. Original failing-group XML proves33tests/1failure and44tests/1failure, with zero skips. The healthy worker continues. To collect the remaining inventory without restarting or duplicating tests, untouched original groups11/14/17 run on released5554 and group16 on released5558 via the unchanged targeted engine; reset19 is reserved until all ordinary work ends. Main source and APK hashes remain frozen. Continuation logs and exact untouched-class inventory are retained under build/qa-overhaul/2026-09-30/sixth-frozen-continuation. This is collection of remaining failure evidence, not an all-green campaign claim.

Current frozen repair [b937d9ca](https://github.com/commvnist/whip/commit/b937d9ca7a0b8092a884988cdcb99d367ce7d1f6) is normally pushed with exact remote match. At13:22:12UTC, sole sixth fresh **UYOVBs has 824/1343 accepted**, groups[1, 2, 3, 4, 5, 6, 7, 9, 10], zero failures/skips/reuse. Complete Gym group5 passes159/159 and routines group9 passes125/125. [Exact seven repair-method receipts](../../artifacts/ux-overhaul/2026-09-30/verified/canonical-creation/fresh-campaign-seven.json) confirm all Exercise editor callers, filtered-group creation/once-only protocol and strict phase recreation; the older-workout pagination/reopen method passes68.974s with its original10-second condition and fullhistory assertions. Selected diagnostic20ms pending0 is earlier focused evidence; its exact timing is not attributed to this full run because live logcat rotated away. Prior accepted CardDesign15/15 remains fresh full-campaign evidence. Remaining groups execute on the same three devices with frozen inputs. Exact product CI lookup has zero entries; complete native acceptance remains pending.

The validated product batch [6f0e006a](https://github.com/commvnist/whip/commit/6f0e006abf333ee1725b42c64826b8b9f361de35) is normally pushed with exact remote match. Fifth fresh campaign **jdsJXa naturally ended at11:41:32 UTC**, exit1 verified. Accepted complete groups contain **540/1342 methods** (batches1/2/3/4/6/7/10), zero failures/skips/reuse. All ten completed groups contain **867 methods:862 passing,5 failing,0 skipped**; remaining475 were not executed. This is a rejected full-inventory attempt. Original XML and worker logs are retained under build/instrumentation-results-jdsJXa; no healthy test process was stopped or duplicated.

The failures comprise one Exercise editor fixture tapping a lazy Advanced Options control after the keyboard reduced its viewport; one older-workout fixture timing out while56 genuine finished-session timer cleanups settle; and three visual-catalog failures from duplicate canonical captures or a stale Home Review absence expectation. The minimal editor helper dismisses the IME and scrolls the actual editor list before tapping; the original validation/no-submit assertions remain. Dedicated page journeys now own all canonical captures, while overview geometry assertions and the process-wide duplicate guard remain. Home retains its persistent Review header and contextual Review Progress action; both open/close paths are verified by the repaired fixture rather than removing either action.

The older-workout method passes unchanged1/1 in46.798s; its unchanged four-method class passes4/4 in167.866s on5556 with full prescription and56-session equality. These passes do not waive its campaign timeout. Test-only pending-ID/revision/elapsed logging preserves the original10-second condition and all domain assertions. Diagnostic replay passes1/1 with53 pending at28ms and0 at7510ms, showing load-sensitive startup work. The pagination fixture now prepares genuinely settled history as described below; no timer performance production fix is claimed.

A bounded sibling creation defect is now **natively reproduced**: phase filtering leaves the sole surviving placement at position1 instead of0. The detached-worktree proposal mechanically reuses the unchanged Gym DAO normalizer before the existing routine creation transaction returns. Baseline fails the new six-case assertion; repaired native checks pass **12/12**: six filtered-group combinations within one method, the existing once-per-exercise protocol and strict phase recreation, three Exercise editor callers, plus six existing Gym group mutation/normalization callers. Exact baseline/repaired app/test hashes and original native logs are preserved in build/qa-overhaul/2026-09-30/routine-canonical-baseline, routine-canonical-repaired and shared-normalizer-sibling-replay. This fix addresses the same durable creation race, rather than optional refactoring. The full canonical page catalog passes10/10 in272.748s on5558. Final CURRENTMAIN replay passes7/7 on5556: all four routine journeys plus independent actual startup cleanup and terminal/stale-revision repository cases. Final pagination seeding executes real scheduler cancellation before acknowledgment of the same captured revision, then asserts cleanup settled for each of56 histories. The original10-second UI wait and all history/import/recreation/prescription assertions remain. Diagnostic final wait sees56sessions/pending0 at20ms. This prepares pagination history through real external operations; it does not claim startup-burst performance is fixed. Selected repair runs total29method executions/28distinct methods across explicitly recorded APK pairs.

The dated audit's49-unfixed sentence is historical. Current Goal/Habit and GYM01-09/TASK01-05 findings are implementation-addressed with focused proof; this is not complete-inventory certification. Dedicated outbound workout Intent-body verification, completed-Task Save return, cycle-review injected failure/recreation, spoken accessibility and every OEM/locale/killed-process combination remain proof limits. Review's archived-workout pixel journey uses the actual production dialog with a controlled date, rather than claiming full-shell archive gestures. The product commit has zero hosted Actions/check/status entries; local and native evidence are separate.


At **10:18 UTC**, the fourth campaign **KqdBtz is terminal**, naturally ending at **10:10:46 UTC** with exit1. No campaign runner remains. Accepted complete batches1-6 contain **537/1342** methods, zero failures/skips/reuse. All nine completed groups contain **793 methods:787 passing,6 failing,0 skipped**. Remaining549 were not executed. This is a rejected full-inventory attempt; all original XML/logs are retained. The earlier automatically rejected stop did not execute; healthy workers continued to natural completion.

The six failures are now classified. A real routine instantiation race used global template positions after phase filtering, then background normalization rewrote set positions/timestamps. Creation now assigns contiguous selected-session positions in its existing transaction; the strict native equality assertion is preserved and the repository fixture adds an immediate position assertion. Checklist cards duplicated collapsed progress summaries; the footer now appears with expanded execution while direct checklist rows remain available. Two journey card-center taps hit nested execution controls; both now use the same proven title-tap helper as four sibling journeys. One Reset check targeted a noninteractive merged wrapper instead of its visible button. The final200% Reset assertion exposed coordinate-conversion rounding: measured126px at density2.625 is48dp, but subtracting separately converted coordinates yielded47.99997dp. The test now measures native layout height directly and additionally requires at least the physical48dp pixel size; no size threshold was lowered.

Integrated debug/test compilation passes. Initial diagnostic card replay passes2/3, preserving the original strict numerical failure and measured bounds before the accounting repair. Final focused native acceptance passes **23/23**:15 card methods in85.49s on5554; six shared-title journeys in176.587s on5558; two strict routine checks in51.421s on5556. Affected readiness passes in3m26s, including lint/compile/debug packaging. [Inspected originals and distinct APK provenance](../../artifacts/ux-overhaul/2026-09-30/verified/card-routine-repair/README.md) preserve measurements, actual native taps and recreation evidence. The full current-source local gate passes in **1m16s**:759 JVM methods across124 suites, zero failures/errors/skips; required debug/release/bundle/benchmark builds, lint/static/manifest/data-safety/diff checks and coverage floors pass. Coverage is84.82% lines,65.39% branches and69.49% core policy. Original current JVM XML is archived separately. The final immutable app/test/source manifest is frozen for one fifth fresh1342-method campaign; complete native acceptance remains pending. The baseline goals lane reproduced all original three card failures twice with unchanged assertions and retained30 original frames/hashes.

## Closing twelve-category acceptance

| Category | Verified scope | Remaining proof limit |
| --- | --- | --- |
| 1. Tab/header/content/action alignment | Shared normal/wide/200% geometry; all10 final canonical catalog methods; six inspected current originals | No observed alignment defect; device configurations remain sampled |
| 2. Typography/colors/icons/controls/states | Shared theme and components; full CardDesign15; normal/200% originals | No spoken or every-OEM certification |
| 3. Actual reuse | Shared chrome, summary/receipt/title-tap owners; Gym DAO normalizer reused by routine creation with existing callers verified | No speculative framework added |
| 4. Add/edit/save/cancel/delete/Back | Dirty/save guards, phase-local edits, parent returns, native interruption/recreation and full journeys | Dedicated completed-Task child Save return is unverified |
| 5. Navigation/labels/grouping | Retained ordinary tabs/daily intents, both exact Home Review controls, filtered group creation | Tested normal routes; no every-entrypoint certification |
| 6. Transitions/scroll/selections/drafts | Workspace and parent state retention, strict Track scope/selection recreation, durable drafts | Every killed-process combination unverified |
| 7. Validation/save/error/edit protection | Durable receipt and failure tests, truthful post-commit import warning, all Exercise callers; final Goal14 | Deadline preconditions pass; intermittent earlier missed Save was not a proven product defect |
| 8. Loading/empty/error/retry/completed | Explicit settled-unavailable/retry, real empty-state controls and final completed-summary assertions | Covered catalog/journeys; no universal fault injection claim |
| 9. Accessibility/contrast/touch/large-text/IME | Named roles/labels, native200%, strict clipping, measured48dp control and keyboard journeys | Spoken TalkBack/SwitchAccess/OEM certification unverified |
| 10. Data correctness |759 cached accepted JVM methods; strict phase/group/history/import/finite-period/direction native checks | Dedicated cycle-review delayed failure/recreation and outbound share Intent body unverified |
| 11. Native normal/repeat/interrupt/Back/recreate/cross-tab | Original three owners/serials; all1343 executed,1301 unchanged passes plus42 final fresh passes | Phased evidence; original full inventory retains six failures; no new single-green run |
| 12. Before/after/checks/commits/pushes | Original gallery and all failures preserved; final native/local checks pass; established main delivery | Exact a84683cc remote match; zero hosted Actions/check/status entries |

Final current-source local gate passes after the bounded repair (4m45s initial complete run;1s final reconciliation using accepted cached results). The complete759-method/124-suite XML is preserved with zero failures/errors/skips. Affected readiness passes51s; required lint/debug/release/bundle/benchmark/static/manifest/data-safety checks and coverage floors84.82% lines/65.39% branches/69.49% core pass. No new JVM execution is attributed to the cached reconciliation. Final immutable CURRENTMAIN app SHA2569a210929c30d1fd09970b2e62d8430219f659012a5e2645e0daf8cff7d2fae02; test SHA256a5fa92175b042b9c8306a240635b7881d8e436b398862fd992dbb95cbda96cbc; source-manifest SHA2568b1c91a283a1d7e9659ae21c92add60dbcfddf877d8a219075fe5569e0a2dbe6. These inputs are frozen for the sole new1343-method fresh campaign UYOVBs described above.

[Canonical creation counterfactual/repaired originals](../../artifacts/ux-overhaul/2026-09-30/verified/canonical-creation/README.md) and [canonical catalog10/10 originals](../../artifacts/ux-overhaul/2026-09-30/verified/catalog-canonical/README.md) retain exact pair provenance. The clear-review PNG alone lacks drawn bottom-navigation pixels; other cross-tab originals and geometry checks provide that proof.

Separately authorized phone update used source8e8662b6 including6f0e006a; its [receipt](../../artifacts/phone-releases/2026-09-30/0.3.91-current-6f0e006a/README.md) is incorporated without touching the prior signature edit. The later bounded batch is excluded from that phone build; no additional phone installation is authorized or claimed.

## Regression chronology

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
