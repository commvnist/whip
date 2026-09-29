# Deep product review evidence — 2026-09-29

All16 accepted groups are implemented and verified within the selected scope. [Completed plan](../../../docs/quality/DEEP_PRODUCT_REVIEW_2026-09-29.md). Baseline `04bc58e2`, private phone 0.3.83/code89. Only synthetic emulator records are used here; no phone installation or data inspection.

## Accepted execution

- **30 distinct native methods**, every latest result passing: [latest ledger](verification/native-latest.tsv). Across47 attempts,34 pass and13 fail; all failures and fixes remain in the [attempt ledger](verification/native-attempts.tsv). Passing guarded commands take4.712–34.614seconds. One separate baseline catalog method also passes; it is not counted as changed-behavior acceptance.
- **Nine distinct JVM methods**: Home preview boundary(1), Review content identity(1), productivity event/date/series/search contracts(3), Gym conversion/selection/mature-history/search contracts(3), Routine request ownership(1). Exact selectors/logs and available XML are retained. The three-method productivity class XML was overwritten by the next invocation; its raw targeted success log remains.
- Fresh app/test package builds retain JaCoCo. Latest production followthrough package passes22s; final fixture-only package4s. [Accepted source hashes](verification/accepted-source.sha256), [APK hashes](verification/accepted-apks.sha256), [installed hashes](verification/installed-apks.json) identify the final source and emulator binaries. Installed app/test hashes match their local artifacts.
- Normal1080×2400/density420, selected actual200% text; short1080×1600 chooser and quick-machine keyboard/failure/retry; wide1800×2400 Review→workout→Review/recreation. Size and font baseline restored, temporarily disabled unrelated Google app re-enabled: [restoration](verification/emulator-restored.log).

## Implementation-to-evidence map

All rows are implemented and have passing focused evidence; domain reports explain preserved alternate-mode contracts and source-reviewed keeps.

| Plan | Passing receipt / inspected result |
| --- | --- |
| DS1 balanced Home | `home-preview-native-2`, `home-large-native`; truthful counts, refill, full collection and actual200% continuation |
| DS2 local Task search | `task-find-native-2`; inline query, no duplicate chip/restored keyboard, retained filter naming parent |
| DS3 execution inspector | `task-inspector-native-2`, `task-subtask-large-native`, `task-focus-native`; edit/recreation/cancel, compact conversion, large controls, precise custom Focus |
| DS4 Review return/cache | `review-return-native-1`, `review-habit-native-2`, `review-goal-native`, `review-gym-wide-native`, `review-content-jvm`; all four source domains and wide return |
| DP1 Habit execution | `habit-numeric-native-1`, `habit-large-native`; exact original values, shared actions and focused evidence |
| DP2 Goal event meaning | `goal-completion-native-1`, `productivity-jvm`; one completion, original values preserved by unrelated edits |
| DP3 collection search | `collection-search-native`, `track-collection-native-3`; archived/finished matches, retained queries, visible-only mutation |
| DP4 milestone flow | `milestone-native-4`; repeated validation, stable details/reorder/recreation, real save |
| DP5 analytical scope | `track-evidence-native-4`, `productivity-jvm`; uneven-date plot, same30days, originalmL correction and retained expanded evidence |
| DP6 configured details | `track-collection-native-3`; remaining-field count and full fourth fact through Entries and Activity |
| DGS1 workout flow | `gym-overview-native-2`, `gym-repeated-native`, `gym-finish-native`, `gym-overview-short-native`; chosen drafts, correction, exact finish receipt, short200 chooser |
| DGS2 measurement entry | `gym-weight-native`, `gym-distance-native`, `gym-rules-jvm`; snapshot conversion, effort order and saved units |
| DGS3 Exercise authoring | `exercise-authoring-native-2`, `exercise-duration-native`; essential meaning, bounded category search, saved selection and cancel |
| DGS4 routine recovery | `routine-child-native`, `routine-machine-short-native-2`, `routine-request-jvm`; failure/retry/recreation/duplicate handling and visible short200 error |
| DGS5 Settings discovery | `settings-search-native`, `gym-rules-jvm`; query restoration and exact control/disclosure destinations |
| DGS6 relevant Progress | `gym-progress-native-3`, `gym-rules-jvm`; recent exercise/equipment,500sessions/12,000sets, correction refresh and exact History |

`repeated-history-native.log` freshly reconfirms the owner's original complaint: two Habit deletions keep History/query open; Goal stale deletion fails locally, fresh retry/correction and recreation keep the parent context.

## Original visual review

Parent inspected final Home normal/200%, Task search/inspector/large subtask and Review normal/wide originals. Domain reviewers personally inspected Habit numeric/large execution/Insights/history, Goal one-event/compact-expanded milestones, Track search/details/trend/matching evidence, Gym execution/effort/finish/history/short chooser/authoring/progress, and Settings200% destinations. Latest suffixed images are the final captures; earlier images are retained observations, not silently replaced acceptance.

The first normal workout-overview PNG is an external Google crash sheet; the next captures the underlying session before the dialog composed. Neither is accepted as a chooser original. The strict short200 chooser capture supplies the actual dialog view. Sparse baseline Home demonstrates hierarchy; the dense after fixture independently demonstrates truncation/refill. No historical sparse image is presented as a dense before/after measurement.

The Exercise basics capture still shows its Change Entry Meaning confirmation. It supports that confirmation's visual review, not a settled basics-form screenshot. Category selection originals and strict saved unit/load/category assertions provide the remaining selected authoring evidence; no additional broad visual campaign is implied.

## Method

Every routine check is individually bounded by `timeout --kill-after=3s 55s`. Native receipts name one exact method and include the emulator target guard, instrumentation result and elapsed wall time. `verification/run-native.sh` rejects a nonzero command or a missing `OK (1 test)`; Android's shell exit code alone is insufficient. No full/readiness/candidate batch is run.

Original PNG/XML captures are retained in `before/` and `after/`. A capture from a failing fixture is observation only, not a passing journey. Parent and domain reviewers inspect original images; assertion success alone does not establish visual acceptance.

## Build snapshots and unsuccessful attempts

- Baseline debug package and exact shared-page catalog passed before edits.
- `shared-package-1.log`: positional Habit dialog API compile mismatch; restored compatible parameter ordering.
- `shared-package-2.log` / `instrumentation-diagnosis.log`: JaCoCo `MethodTooLargeException` for root `WhipScreen`; extracted Review and Home Habit editor state into feature owners. Coverage remains enabled.
- `shared-package-3/4/5.log`: fixture compile errors corrected (TaskStepDraft named parameters/position and duplicate Gym arguments).
- `shared-package-6.log`: 55-second timeout after Kotlin compilation. Incomplete, never passed. The separately bounded packaging-only `initial-package-phase.log` uses completed compilation outputs and passes in 26 seconds; initial APK/source hashes identify that snapshot.
- `home-preview-native-1.log`: selector targeted a non-merged layout tag. Repaired by addressing the actual accessible completion control; its earlier screenshot remains visual observation.
- `gym-unit-jvm.log`: new pure fixture confused the nested Track point/entry identity. Corrected fixture; exact three-method productivity class subsequently passes in `productivity-jvm.log` (3-second Gradle build). Its XML was overwritten by the next targeted Gradle invocation; the raw success log is retained.
- `final-native-compile-1/2.log`: missing Settings and machine fixture imports; corrected before packaging.
- `final-package-1.log`: fresh app/test build succeeds in 17 seconds with JaCoCo. `final-source.sha256` and `final-apks.sha256` identify the compiled snapshot. Later fixture-only changes, if any, are recorded separately; no silently reused binary is claimed.
- Initial Track collection/evidence failures report a root without window focus. The first Gym overview capture then explicitly rejects a non-Whip obscuring window. `dumpsys window` confirms `Application Error: com.google.android.googlequicksearchbox`; `emulator-external-crash.log` retains that unrelated app's repeated OOMs. Temporarily disable that app on the disposable emulator, retain Whip's capture/focus guards, retry only interrupted exact methods, and restore the external app at completion.
- Initial milestone fixture assumed a compact card exposed Edit directly. Use its actual inspector Edit action; preserve all validation/recreation/reorder/persistence assertions. The Routine fixture uses the saving overlay because the shared busy boundary intentionally hides underlying editor semantics. These fixture repairs receive a separately built/installed test snapshot.
- Raw Android XML uses CRLF and compiler logs include original trailing whitespace. Preserve those diagnostic bytes; source/document whitespace checks exclude raw `artifacts/` rather than rewriting evidence.

## Scope of claims

Source reports assess complete jobs and alternate modes. Exact executed receipts support the selected behaviors only. No full-suite, all-device, TalkBack/locale or measured frame-rate certification is claimed. Content-keyed calculations avoid known unnecessary work; this is not a physical speed benchmark. The owner phone stays on its previously verified release.
