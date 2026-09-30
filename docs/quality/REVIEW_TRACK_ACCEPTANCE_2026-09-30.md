# Review and Track follow-up acceptance — 2026-09-30

Status: all three selected acceptance methods have passing evidence across exact replays. Track foreground and recreated Activity Save/return are verified with all strict assertions intact. Integration reports the final affected readiness gate passed in 3m8s; the full fresh 1,342-method native campaign is now running and remains integration-owned. This receipt does not certify the complete overhaul or a phone build.

## Exact scope

Three methods were added to existing native test classes, with no extra class, framework or production hook:

- ReviewJourneyE2ETest#emptySelectedWeekStillOffersTheOlderThirtyDayCorrelation: real shell, current empty week and seven paired older Habit/Goal dates; 30-day correlation remains available and survives recreation.
- ReviewOutcomeJourneyE2ETest#archivedFinishedWorkoutKeepsItsOneDayReviewCountAndVisiblePoint: controlled actual production ReviewDialog, persisted Room finished-workout archive/restoration, fixed first-of-month logical date. Every stage asserts count 1, one chart observation and visible primary-color pixels, plus original outcome title/archived status. This is not a full-shell archive gesture journey.
- TrackHistoryJourneyE2ETest#entryCorrectionRetainsEntriesAndActivityInspectorsAndDuplicateDraft: real shell, two persisted Entries. Duplicate handoff cancellation retains exact editing draft; Entries Cancel/recreation retains original inspector/query and unchanged records. Activity edit/recreation retains visible draft and must Save back to the original inspector, filter and exactly two records with only the intended Notes changed.

Original native screenshots exposed a real Track child ordering bug: the old inspector remained above the recreated editor while Compose assertions found underlying draft fields. Integration propagated existing editorOpen to Entries, Activity and Insights and suppressed parent rendering while retaining its saved ID. Strengthened checks require zero parent inspector nodes and displayed exact draft inputs before/after recreation. Actual new device PNG/XML confirm foreground drafts.

A bounded post-save wait proved the second bug is durable: loaded Activity returns with both cards, no inspector and no text filter after 10 seconds. Moving only the nested state holder outside BoxWithConstraints did not fix it. Integration then moved the five existing Activity rememberSaveable states and its scroll owner to ordinary TrackAreaContent scope, passing existing states into the private Activity page. The unchanged strict native test passed; this resolves the observed state-ownership defect without adding a state framework. All exact title/draft/selection/query/data assertions remained intact.

## Verification ledger

All direct native actions targeted emulator-5558 only; no Gradle was invoked by this lane. Application and test APK hashes were verified before both installs.

| Fresh invocation | Outcome | Immutable provenance |
| --- | --- | --- |
| native-final-review-track.log | 3 run, 1 pass, 2 fail; 65.603s. Correlation passed. Review failed only on duplicate capture ID; Track immediate post-recreation title assertion failed. | batch-two-all-final app80390d4d580186ce6faeec466c3ad0a1155f0350239907ea1eb1db59c0936099; test a6ea511374b9fe193dba07e277676e03cf962717bddaafb3332d39e5294efead; source56ea69a70a92792734c7232a2fe292d6cf1c1382d5a64ae1c7f86b3ec223afed |
| native-final-review-track-guard-replay.log | 2 run, Review pass, Track fail; 47.193s. Review unique before/archived/restored IDs resolve capture-only failure; foreground/draft checks pass; Track fails immediately after recreated editor Save. | appda64c8be3ad5ad5e887d9a570b8b1f758036a2c10dcf8f0e7bd8271bc209cb7a; test233685ab36a5f0422304658cd79dd94797dd3c98587e5f241a4f5fc5cbef8f6e; sourcec891d80fda60324af515b8365c40bd23b45e1ab21ee8eb1346d41c0739801e1c |
| native-track-final-settlement-replay.log | 1 run, 1 fail; 54.301s. Same strict Track behavior plus bounded post-save wait/capture. 10-second absence proves restoration loss. | Same appda64c8be; testbaf2783b482426658656d93e57d7c87a2a7f0356cad99e886ee8c228424d434f; sourceac9ed728c1736f60c9d16ef9bd0cfd408ab2736d749b8c93ce8fd47c6fb25039 |
| native-track-workspace-owner-replay.log | 1 run, 1 fail; 60.239s. Holder-only hoist is insufficient; exact same post-save 10-second timeout. | app8f0d616303e61a2bb0d6973443a10f652941f6126d6c480e1a1aad4eb6b49ce5; unchanged testbaf2783b; source3827fa2bcd6031726d2c77968ec7f50c72e6d0527512dba58cbf6791957c9551 |
| native-track-activity-owner-replay.log | **1 run, 1 pass; 60.175s. STATUS_CODE 0 and OK (1 test).** Foreground draft/recreation, duplicate cancellation, Entries return, Activity Save/recreation inspector, retained text filter/Activity selection, exactly two records and unchanged sibling Entry all pass. | app0672b8b8284f778a1fd0191c577f286537bd1693edf78adc470124029208e64a; unchanged testbaf2783b482426658656d93e57d7c87a2a7f0356cad99e886ee8c228424d434f; source2cba357865408d0e5a6922854d4684775a42ca57d25448ed1bbf36f8dc091f07 |

Raw logs, cutoff timestamps and full original catalogs remain under build/qa-overhaul/2026-09-30/goals-habits/. Selected original pairs with exact provenance are in [the follow-up gallery](../../artifacts/ux-overhaul/2026-09-30/verified/review-track-follow-up/README.md). The actual timeout capture runs before ActivityScenario teardown and fixture deletion; a post-teardown launcher hierarchy was excluded from product diagnosis.

Native Review pixel assertions and source checks do not replace the upcoming full native inventory. Exact killed-process races, RV04 destructive interruption combinations and spoken accessibility certification are not added claims. Integration owns production fixes, full readiness/aggregate checks, commits/push and final campaign disposition.

Personally inspected final PNGs: both recreated drafts visibly contain their exact edited values, and the post-save/recreated inspector displays the original Entry title and Saved correction from Activity. Before repository normalization, all twenty copies were byte-equal to source assets. Current repository copies preserve original PNG bytes and equivalent XML hierarchies as recorded below; raw/copy SHA256 provenance verifies. Emulator-5558 was released immediately after the final originals were pulled; no subsequent device actions. No independent commit/push or Gradle invocation from this lane.

Repository evidence validation: all ten PNG copies remain byte-identical to originals; ten XML copies normalize only CRLF to LF, with parsed hierarchy equality verified. provenance.tsv records separate raw/copy SHA256. Raw device originals remain preserved in the ignored evidence paths. Staged diff whitespace validation passes after this normalization.
