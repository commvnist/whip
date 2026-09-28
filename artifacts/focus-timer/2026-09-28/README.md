# Focus timer overhaul evidence

Date: 2026-09-28. Status: Verified within the accepted Focus scope. Feedback: FB-20260928-004. [Plan and workflow dispositions](../../../docs/quality/FOCUS_TIMER_UX_2026-09-28.md). Baseline: e799ab02; plan committed before implementation as 78923c7c.

The implementation adds 15/30/45/60 presets, validated saveable **Custom Time**, replacement consent, confirmed start/stop feedback, a pinned shared Home/Tasks card and native countdown notification with Open/Stop. Related fixes cover custom-time spoken labels, Android clock format, completion acknowledgement, stale actions and cancellation/deletion races, clipped Home accessible names and constrained layouts. It reuses the durable Task/deadline pair and existing exact-alarm/WorkManager delivery, with no ticker service or dependency.

## Accepted execution

| Receipt | Actual result and scope |
| --- | --- |
| [Focused JVM](focused-jvm-repaired.log) | 7/7 methods pass after the retained initial compile failure. |
| [Current final readiness](readiness-current.log), [412-method ledger](verification/jvm-method-results.tsv), [original XML](verification/jvm-final/) | Fresh 412 JVM methods in 46 classes pass, zero failures/errors/skips; compilation, JaCoCo instrumentation, lint, debug assembly and static/diff checks pass. JVM Gradle task takes 4 seconds; build/lint gate takes 2m54s. Final command is bounded to 300 seconds. |
| [Normal native acceptance](android-normal-accepted.log), [aggregate](verification/accepted-normal/aggregate.tsv), [methods](verification/android-normal-method-results.tsv) | 45 distinct methods pass in two frozen batches, zero failures/skips/reuse, disposable API 34 emulator. This source precedes the compact short-screen repair. |
| [Short actual 200% acceptance](android-short-large-accepted.log), [aggregate](verification/accepted-short-large/aggregate.tsv), [methods](verification/android-accepted-short-large-method-results.tsv) | 4/4 pass at 1080×1600, density 420, system font scale 2.0. Presets/custom/reminder/Card/Home checks retain 48dp controls, complete non-ellipsized clock text, at least 160dp of Home list and real scrolling. |
| [Current Card/Home follow-through](android-layout-followthrough.log), [aggregate](verification/accepted-layout-followthrough/aggregate.tsv), [methods](verification/android-accepted-layout-followthrough-method-results.tsv) | 7/7 pass after all production/fixture changes, at normal display size, including annotated actual 200% cases. Completion and the original all-section clipped-name trigger pass strict native capture. |
| [External permission and cold-process protocol](permission-cold-process/README.md), [asserted result](permission-cold-process/assertion-summary.json), [timestamped states](permission-cold-process/states.jsonl) | Real runtime revocation/reopening preserves the first pair; a fresh Android-prompt-denied start has no native record. Home Alerts opens Android Settings; granting there and returning publishes the same second pair/deadline. Native Stop with PID absent clears the pair/notification, leaves Launcher foreground and Task incomplete. Native Open from a cold third session launches the exact inspector with unchanged pair/deadline. Short denied UI has 163.81dp of Home list at actual 200%. |

Accepted native execution is **56 methods across three campaigns, 45 distinct and 11 repeats**, with zero failures/skips/reuse. [Latest per-method receipt](verification/android-latest-method-results.tsv) points to the most recent accepted execution of each method; the short-configuration ledger remains separate. Native campaigns are incremental, not a claim that all 45 methods ran again on the final APK. The final seven methods cover the changed Card/Home owner; the unchanged duration/notification/mutation neighbors retain the 45-method acceptance.

[Acceptance metadata and final APK hashes](verification/acceptance-summary.json) separate the final build from the earlier normal campaign. The external protocol verifies that its installed debug APK matches the final debug hash. Source inventory is **693 JVM + 1,219 Android = 1,912 methods**, including **4 + 28 additions**; inventory/compilation are not fresh execution claims.

## Failed attempts and repairs

| Retained attempt | Result and disposition |
| --- | --- |
| focused-jvm.log | Missing coroutine import; repaired seven-method run passes. |
| readiness.log / readiness-jvm | 412 passing JVM methods, then three fixture Kotlin errors; remaining failed client stopped. Not accepted readiness. |
| readiness-repaired.log / readiness-final.log | 55-second timeouts are incomplete. JaCoCo diagnostic identifies oversized instrumented WhipScreen; extracting the Focus owner passes without excluding coverage. Initial completed readiness-acceptance.log is historical and predates the final UI changes. |
| android-normal.log / verification/failed-normal | 41 methods, 12 failures: incorrect Options selectors, ineffective app-op denial setup and a clipped unnamed Home control. |
| android-normal-repaired.log | Fixture geometry compile failure; zero native execution. |
| android-normal-final.log / verification/failed-followthrough | 41 methods, five failures: busy fixture timing/matching, ineffective UID app-op assumptions and unnamed clipped section heading. Card-only repair is insufficient; shared heading is also named. |
| android-normal-acceptance.log / verification/failed-final-fixture | 41 methods, one fixture failure. Busy description is longer than an exact-match assertion; substring matching repairs the fixture while preserving visible-state, Back blocking, single submission and retry checks. Subsequent full acceptance is 45/45. |
| android-short-large.log / verification/failed-short-large | 3/4 pass. Real Home height is 131.81dp rather than required 160dp. FND-20260928-008 records the defect before compact metadata/copy repair. |
| android-short-metadata.log / verification/failed-short-metadata | 3/4 pass. Broad text overflow assertion fails before geometry; no accepted short-layout claim. |
| android-short-diagnostic.log / verification/short-text-diagnostic | One diagnostic method fails and records the text layout. Paragraph width is 910px while measured clock/finish widths are 412/553px; the complete visible lines fit. Final fixture verifies logical text end, no ellipsis, max-lines and measured line bounds with at most one physical-pixel rounding tolerance. Actual font scaling and strict Home geometry remain unchanged. |

All failures/timeouts remain diagnostic evidence. No failed receipt is relabeled as accepted. Android 14 enabled-notification status reads runtime permission; app-op overrides are not valid substitutes. The external protocol uses real permission changes and the actual Android prompt/Settings.

## Reviewed originals

[Gallery](gallery.html) links unedited originals and native hierarchies. Accepted sets: [normal](visuals-normal/) (25 pairs), [short 200%](visuals-short-large/) (7 pairs), [final Card/Home](visuals-layout-followthrough/) (7 pairs), and [external platform journey](permission-cold-process/). Earlier normal images retain their original source/configuration; short/latest images certify the compact repair. Failed originals stay with their failed campaign.

Final short Home list bounds are [0,729][1080,1159], 430px / 2.625 = 163.81dp, up from 131.81dp. The original constrained scrolling check shows a complete subsequent Task card while Focus remains pinned. Task title alone may ellipsize and retains full accessible identity; clock/finish/status/actions remain complete.

The exported native bounds can shrink after scrolling because Compose 1.12.0 uncovered-node pruning subtracts a partly visible sibling's expanded minimum-touch rectangle. Source/bytecode review traces LazyColumn's existing scrolling clip and the separate clipped pointer hit test. Actual layout, target geometry, pixels and scrolling remain correct; no speculative extra clipping layer or weaker NAF/name policy was added. This is an investigated keep, not TalkBack speech certification.

Android drawer originals survive UTP uninstall via scoped Downloads. One external drawer dump failed to reach idle during animation/ticking; its first PNG is diagnostic. The settled original shows the real countdown/Stop; the external tap uses that visible Stop, and PID/pair/native-record/foreground facts establish the cold outcome. No stale external hierarchy is used as accepted evidence. [SHA-256 manifest](SHA256SUMS) covers retained files other than itself.

[Scoped evidence attributes](.gitattributes) follow the existing audit receipts: preserve native Android/JUnit CRLF bytes and the compiler's original trailing padding in the one focused JVM log. Source/documentation whitespace and other receipt checks remain enabled; raw evidence is not reformatted to satisfy a whitespace check.

## Boundaries

Only disposable synthetic emulator data is used. The owner phone remains on **0.3.81/code 87**; this task does not install a new phone update or publish to Play. Room 46, epoch 6, backup 26, release version, package, signing and dependencies remain unchanged.

Notification/exact-alarm denial, OEM deferral, force-stop and explicit user dismissal remain Android/user boundaries. Existing notification records can survive runtime revocation on this API 34 emulator; fresh denied publication is tested separately. Channel recovery proves presentation refresh without replacing scheduled work; cold startup reconciliation is not claimed to preserve a WorkManager UUID. No whole-platform, actual TalkBack speech, full native inventory, coverage campaign or Play candidate certification is claimed.
