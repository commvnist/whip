# Second UX audit — accepted evidence

The [190-row audit and 32-workstream plan](../../../docs/quality/UX_AUDIT_2_2026-09-28.md) is implemented and verified within its affected scope. Three Astra/high domain agents investigated and implemented in parallel; the parent integrated, ran checks and retained evidence. Baseline `7c77c075`; planning commits `4df59310` and `e05bc42d` preceded implementation.

Implementation delivered on `origin/main`: `8ed3b575` (Goals/Track), `ece2126f` (daily workflows/shared UI), `d9832910` (Gym/Settings). These commits contain the final tested source; subsequent closeout edits affect documentation and retained evidence only.

Open the [interactive gallery](gallery.html) for five before/after comparisons and **150 original accepted captures**. This is a capture count, not 150 distinct screens: repeated states in different accepted campaigns remain attached to their own receipts. Images and matching XML are unmodified synthetic API34 emulator output. No physical-phone data is included.

## Accepted checks

**419 distinct JVM methods** passed: [stable affected readiness](verification/readiness-stable/) executed 417 methods in 47 classes; the [final Goal/Settings readiness](verification/readiness-followthrough/run.log) executed 58 in eight classes after the numeric/notification corrections, including two new methods. Both passed Android-test compilation, JaCoCo instrumentation, lint, debug packaging, static/assets and diff checks. [Latest per-method ledger](verification/jvm-latest-method-results.tsv) records exact scope; [all 21 new JVM methods](verification/new-jvm-methods.txt) are covered.

Commands: `scripts/check --ready --base 7c77c075`, then `scripts/check --ready --path app/src/main/java/com/whip/app/domain/GoalModels.kt --path app/src/main/java/com/whip/app/ui/SettingsScreens.kt`. The final follow-through JVM task executed freshly in six seconds; its remaining readiness tasks completed in 2m42s.

**175 distinct Android methods** passed, with zero failures/skips/reuse in accepted batches. The [latest per-method ledger](verification/android-latest-method-results.tsv) reconciles overlapping runs; [all 40 new Android methods](verification/new-android-methods.txt) are covered. There are 186 executions in the selected receipts because the final correction repeats 11 previously accepted methods. Acceptance is incremental, not one frozen Play candidate.

| Accepted receipt | Methods | Scope/configuration |
| --- | ---: | --- |
| [shared](verification/accepted/shared/run.log) | 3 | Short inspector, long Retry and nested-list Retry; 1080×1400, density420, actual200%. |
| [short](verification/accepted/short/run.log) | 7 | Selection, planner/IME, history correction, Gym calendar/active lane and Settings/password; 1080×1600, density420, actual200% where annotated. |
| [normal-1](verification/accepted/normal-1/run.log) | 10 | Habit/Home, raw Task duration, exact Gym points, machines and real Rest-channel return. |
| [normal-3](verification/accepted/normal-3/run.log) | 120 | 106 repository methods plus Goal evidence, Track query/search and global-search neighbors. |
| [normal-4](verification/accepted/normal-4/run.log) | 15 | Existing inspectors, Track Insights, ordinary/200% authoring, archive/workspace, routines and document-provider backups. |
| [normal-5](verification/campaigns/normal-5/run.log) | 5 | Existing Gym/Routine neighbors; unchanged affected paths from the earlier accepted group. |
| [selection-neighbors](verification/accepted/selection-neighbors/run.log) | 4 | Bulk actions, history actions, saved selection/edit and retained selection after failure. |
| [settings-final](verification/accepted/settings-final/run.log) | 22 | Clean replacement of normal-2, seven repeated Goal evidence methods, actual channel disable/repair/return, notification test, encrypted-provider/wrong-password and deep-result/password neighbors. |

Native commands use `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android CLASS[#METHOD] ...`; each receipt retains selectors and raw XML/TSV. Configuration files are retained for the normal/short groups; the shared configuration is recorded above. The final group also retains its exact [command](verification/accepted/settings-final/command.txt). Default normal configuration is1080×2400/density420; actual200% annotations restore system font scale afterward. The shared correction used the required 55-second routine bound and completed successfully. Source and APKs were held stable during each accepted group; later changes received affected follow-through checks.

## Failures retained honestly

- `verification/shared-baseline.*`: two reproduced layout failures. The load-error PNG exists, but clipped Retry caused the XML accessibility capture to fail; no missing XML is invented. `shared-unaccepted-diagnostic.log` has two passing assertions but was invalidated by source mutation and contributes no accepted methods.
- Gym/Settings baseline receipts and `before/`: double-digit Gym date/count wrapping and an offscreen deep Settings result. Earlier compilation/packaging attempts include an incomplete 55-second timeout, never a pass.
- Initial/focused/previous readiness receipts: precise-draft rounding, a stale private-function source assertion, a missing Goal fixture type and JaCoCo's oversized WhipScreen method were corrected. Final readiness supersedes these attempts; coverage was not disabled.
- `verification/campaigns/`: selection's real90.29dp failure; notification fixtures aborted by self-revoking the instrumented app's runtime permission; Home recovery incorrectly persisted All Areas; two Goal/Track fixture selectors/assertions; three real nested-scroll Track crashes. `normal-5` is the accepted exception explicitly used above.
- `verification/quick-correction/`: an intermediate selection layout passed102dp but visual review found an empty toolbar; the final fixture requires160dp and toolbar restoration. Other failures involved scrolling a tall parent rather than its status, a content-description selector, calendar versus wheel mode, the actual Insights destination tag, and an unjustified multiple-fling assumption.
- `verification/final-settings-diagnostic/`: nine methods passed, but the batch failed on the unchanged strict channel-return assertion. Moving the refresh-state read into the parent platform-status scope corrected a real stale-state defect. No failure screenshot existed before its failing assertion; the final accepted pre-assert screenshot visibly shows Off in Android and the Task repair action.

The final independent source review additionally found rolling-window cancellation (`1e16,1,2` losing the retained1 after expiry). Standard decimal totals and pre-conversion means now agree across rolling/current/consistency calculations; both new regressions pass in final readiness. Stored history and timestamp tie order remain unchanged.

## Boundaries

Source inventory: **689 JVM +1,191 Android =1,880 methods**, a delta of21+40; inventory and compiled tests are not claims of complete fresh execution. No schema46, epoch6, backup26, dependency, release-version, private-phone installation or Play candidate change. Some historical pause/snapshot metadata was never stored and cannot be reconstructed. No fresh full TalkBack traversal, every-device/locale campaign or very-long milestone stress certification. Subjective design acceptance awaits owner use.

`SHA256SUMS` binds this retained evidence byte-for-byte. `before/` includes an explicitly labelled intermediate Task selection layout; `after/` contains accepted originals. Historical attempts remain in `verification/`, separate from final method ledgers. VER-20260928-002 records the durable closeout.
