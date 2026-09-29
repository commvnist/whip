# Preserved native failure and incomplete excerpts

These are exact source-log excerpts from synthetic disposable-emulator tests. No credentials or owner-device data occur in the retained snippets; no redaction was required. Generated labels and line caps are identified explicitly.

## productivity-cancellingGoalDefinitionEditReturnsToHistory.log
Source: build/fresh-app-overhaul-20260929/productivity-cancellingGoalDefinitionEditReturnsToHistory.log
SHA-256: 018ef8793b070c609a7e299a3f449f76b88b40a4d71062e6f850030814695c8c

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: Failed to inject touch input.
Reason: Expected exactly '1' node but could not find any node that satisfies: (Text + InputText + EditableText contains 'Cancel' (ignoreCase: false))

	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchOneOrThrow(SemanticsNodeInteraction.kt:178)
	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchOneOrThrow$default(SemanticsNodeInteraction.kt:150)
	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchSemanticsNode(SemanticsNodeInteraction.kt:84)
	at androidx.compose.ui.test.ActionsKt.performTouchInput(Actions.kt:403)
	at androidx.compose.ui.test.AndroidActions.performClickImpl(Actions.android.kt:23)
	at androidx.compose.ui.test.ActionsKt.performClick(Actions.kt:61)
	at com.whip.app.ui.FreshProductivityUiTest.cancellingGoalDefinitionEditReturnsToHistory(FreshProductivityUiTest.kt:132)

```

Exact aggregate footer:
```text
Time: 6.296
FAILURES!!!
Tests run: 1,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## productivity-cancellingHabitDefinitionEditReturnsToHistory.log
Source: build/fresh-app-overhaul-20260929/productivity-cancellingHabitDefinitionEditReturnsToHistory.log
SHA-256: 5398954773c3381c07d4fa6892bc67865064b95f0a85e8115a650cc428a1a23d

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: Failed to inject touch input.
Reason: Expected exactly '1' node but could not find any node that satisfies: (Text + InputText + EditableText contains 'Cancel' (ignoreCase: false))

	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchOneOrThrow(SemanticsNodeInteraction.kt:178)
	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchOneOrThrow$default(SemanticsNodeInteraction.kt:150)
	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchSemanticsNode(SemanticsNodeInteraction.kt:84)
	at androidx.compose.ui.test.ActionsKt.performTouchInput(Actions.kt:403)
	at androidx.compose.ui.test.AndroidActions.performClickImpl(Actions.android.kt:23)
	at androidx.compose.ui.test.ActionsKt.performClick(Actions.kt:61)
	at com.whip.app.ui.FreshProductivityUiTest.cancellingHabitDefinitionEditReturnsToHistory(FreshProductivityUiTest.kt:115)

```

Exact aggregate footer:
```text
Time: 4.588
FAILURES!!!
Tests run: 1,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## productivity-repaired-goalReviewAndItemsRemainFullyReadableAtLargeText.log
Source: build/fresh-app-overhaul-20260929/productivity-repaired-goalReviewAndItemsRemainFullyReadableAtLargeText.log
SHA-256: f6c4fdc5375f26d849469232b6330cea0bfa9553725cc8f3e436c2cc4ed86c0b

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: Review must not clip
	at org.junit.Assert.fail(Assert.java:89)
	at org.junit.Assert.assertTrue(Assert.java:42)
	at org.junit.Assert.assertFalse(Assert.java:65)
	at com.whip.app.ui.FreshProductivityUiTest.goalReviewAndItemsRemainFullyReadableAtLargeText(FreshProductivityUiTest.kt:130)

```

Exact aggregate footer:
```text
Time: 6.945
FAILURES!!!
Tests run: 1,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## shell-review-area-return.log
Source: build/fresh-app-overhaul-20260929/shell-review-area-return.log
SHA-256: 2e3f52d2c86fc27abfd5db908fc191b639233fb71e70f85fe39b9ac54c64c913

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: Action performScrollTo() failed.
Reason: Expected exactly '1' node but could not find any node that satisfies: (Text + InputText + EditableText contains 'Monthly' (ignoreCase: false))

	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchOneOrThrow(SemanticsNodeInteraction.kt:178)
	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchOneOrThrow$default(SemanticsNodeInteraction.kt:150)
	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchSemanticsNode(SemanticsNodeInteraction.kt:84)
	at androidx.compose.ui.test.ActionsKt.performScrollTo(Actions.kt:82)
	at com.whip.app.ReviewJourneyE2ETest.allTrackEvidenceKeepsItsScopeAcrossAreaReviewAndRecreation(ReviewJourneyE2ETest.kt:57)

```

Exact aggregate footer:
```text
Time: 15.489
FAILURES!!!
Tests run: 1,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## shell-review-return-correlations-replay.log
Source: build/fresh-app-overhaul-20260929/shell-review-return-correlations-replay.log
SHA-256: 8d29d2c535e61135fa439266714c41de26c28bfd0f0af300a297edc8937428fd

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: Failed: performScrollToNode(TestTag = 'review-options-toggle')
Reason: Expected exactly '1' node but found '2' nodes that satisfy: (ScrollBy is defined)
Nodes found:
1) Node #442 at (l=0.0, t=300.0, r=1080.0, b=2169.0)px, Tag: 'home-list'
CollectionInfo = 'CollectionInfo(rowCount=3, columnCount=1)'
IsTraversalGroup = 'true'
Shape = 'androidx.compose.foundation.VerticalScrollableClipShape@64c2fe4'
VerticalScrollAxisRange = 'ScrollAxisRange(value=0.0, maxValue=0.0, reverseScrolling=false)'
Actions = [GetScrollViewportLength, IndexForKey, ScrollBy, ScrollByOffset, ScrollToIndex]
Has 9 children, 2 siblings
2) Node #494 at (l=0.0, t=326.0, r=1080.0, b=2337.0)px
CollectionInfo = 'CollectionInfo(rowCount=2, columnCount=1)'
IsTraversalGroup = 'true'
Shape = 'androidx.compose.foundation.VerticalScrollableClipShape@64c2fe4'
VerticalScrollAxisRange = 'ScrollAxisRange(value=1355.0, maxValue=1455.0, reverseScrolling=false)'
Actions = [GetScrollViewportLength, IndexForKey, ScrollBy, ScrollByOffset, ScrollToIndex]
Has 1 child, 2 siblings

	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchOneOrThrow(SemanticsNodeInteraction.kt:178)
	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchOneOrThrow$default(SemanticsNodeInteraction.kt:150)
	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchSemanticsNode(SemanticsNodeInteraction.kt:84)
	at androidx.compose.ui.test.ActionsKt.scrollToMatchingDescendantOrReturnScrollable(Actions.kt:278)
	at androidx.compose.ui.test.ActionsKt.performScrollToNode(Actions.kt:237)
	at com.whip.app.ReviewJourneyE2ETest.allTrackEvidenceKeepsItsScopeAcrossAreaReviewAndRecreation(ReviewJourneyE2ETest.kt:55)

```

Exact aggregate footer:
```text
Time: 25.239
FAILURES!!!
Tests run: 2,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## shell-search-focus-replay.log
Source: build/fresh-app-overhaul-20260929/shell-search-focus-replay.log
SHA-256: 7eddb6247a939add97e90aa45eb1258c13ba0997fd3e085417dfb49b6e1c3641

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: Failed to assert the following: (Focused = 'true')
Semantics of the node:
Node #60 at (l=53.0, t=1012.0, r=1027.0, b=1165.0)px, Tag: 'unified-search-result-Routine-2'
Focused = 'false'
Shape = 'RoundedCornerShape(topStart = CornerSize(size = 6.0.dp), topEnd = CornerSize(size = 6.0.dp), bottomEnd = CornerSize(size = 6.0.dp), bottomStart = CornerSize(size = 6.0.dp))'
Text = '[Strength B, Routine]'
Actions = [ClearTextSubstitution, GetTextLayoutResult, OnClick, RequestFocus, SetTextSubstitution, ShowTextSubstitution]
MergeDescendants = 'true'
Has 1 sibling
Selector used: (TestTag = 'unified-search-result-Routine-2')

	at androidx.compose.ui.test.AssertionsKt.assert(Assertions.kt:293)
	at androidx.compose.ui.test.AssertionsKt.assert$default(Assertions.kt:283)
	at androidx.compose.ui.test.AssertionsKt.assertIsFocused(Assertions.kt:117)
	at com.whip.app.ui.UnifiedSearchAdaptiveUiTest.enterActivatesTheFocusedSearchControlRatherThanAlwaysTheFirstResult(UnifiedSearchAdaptiveUiTest.kt:84)

```

Exact aggregate footer:
```text
Time: 7.126
FAILURES!!!
Tests run: 1,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## shell-search-focus.log
Source: build/fresh-app-overhaul-20260929/shell-search-focus.log
SHA-256: 9aa5f3230c29fdea5f33a0d435a8b9b51220d30228005fe49d65c3fb1126f1d1

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: expected:<2> but was:<1>
	at org.junit.Assert.fail(Assert.java:89)
	at org.junit.Assert.failNotEquals(Assert.java:835)
	at org.junit.Assert.assertEquals(Assert.java:120)
	at org.junit.Assert.assertEquals(Assert.java:146)
	at com.whip.app.ui.UnifiedSearchAdaptiveUiTest.enterActivatesTheFocusedSearchControlRatherThanAlwaysTheFirstResult$lambda$5(UnifiedSearchAdaptiveUiTest.kt:82)
	at com.whip.app.ui.UnifiedSearchAdaptiveUiTest$$ExternalSyntheticLambda40.invoke(D8$$SyntheticClass:0)
	at androidx.compose.ui.test.AndroidSynchronization_androidKt.runOnUiThread$lambda$0(AndroidSynchronization.android.kt:39)
	at androidx.compose.ui.test.AndroidSynchronization_androidKt$$ExternalSyntheticLambda0.call(D8$$SyntheticClass:0)
	at java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:487)
	at java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at android.app.Instrumentation$SyncRunnable.run(Instrumentation.java:2420)
	at android.os.Handler.handleCallback(Handler.java:958)
	at android.os.Handler.dispatchMessage(Handler.java:99)
	at android.os.Looper.loopOnce(Looper.java:205)
	at android.os.Looper.loop(Looper.java:294)
	at android.app.ActivityThread.main(ActivityThread.java:8177)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:552)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:971)
	Suppressed: java.util.concurrent.ExecutionException: An Exception occurred on the UI thread during runOnUiThread()
		at androidx.compose.ui.test.AndroidSynchronization_androidKt.runOnUiThread(AndroidSynchronization.android.kt:50)
		at androidx.compose.ui.test.AndroidComposeUiTestEnvironment$AndroidComposeUiTestImpl.runOnUiThread(ComposeUiTest.android.kt:844)
		at androidx.compose.ui.test.AndroidComposeUiTestEnvironment$AndroidComposeUiTestImpl.runOnIdle(ComposeUiTest.android.kt:851)
		at androidx.compose.ui.test.junit4.AndroidComposeTestRule.runOnIdle(AndroidComposeTestRule.android.kt:489)
		at com.whip.app.ui.UnifiedSearchAdaptiveUiTest.enterActivatesTheFocusedSearchControlRatherThanAlwaysTheFirstResult(UnifiedSearchAdaptiveUiTest.kt:82)
		at java.lang.reflect.Method.invoke(Native Method)
		at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
		at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
		at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
		at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
		at androidx.compose.ui.test.junit4.AndroidComposeTestRule$apply$testWithDisposal$1.evaluate(AndroidComposeTestRule.android.kt:426)
		at org.junit.rules.ExternalResource$1.evaluate(ExternalResource.java:54)
		at androidx.compose.ui.test.junit4.AndroidComposeTestRule$apply$1$evaluate$1.invokeSuspend(AndroidComposeTestRule.android.kt:448)
```
Excerpt stops after 35 source lines; source log retains the remainder.

Exact aggregate footer:
```text
Time: 6.293
FAILURES!!!
Tests run: 1,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## shell-search-review-native-replay.log
Source: build/fresh-app-overhaul-20260929/shell-search-review-native-replay.log
SHA-256: cc20d4a78cc677257346a57edcd235307fdea13ecbbcb942f985228e33f260ee

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: Action performScrollTo() failed.
Reason: Expected exactly '1' node but could not find any node that satisfies: (Text + InputText + EditableText contains 'Productivity: Work' (ignoreCase: false))

	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchOneOrThrow(SemanticsNodeInteraction.kt:178)
	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchOneOrThrow$default(SemanticsNodeInteraction.kt:150)
	at androidx.compose.ui.test.SemanticsNodeInteraction.fetchSemanticsNode(SemanticsNodeInteraction.kt:84)
	at androidx.compose.ui.test.ActionsKt.performScrollTo(Actions.kt:82)
	at com.whip.app.ReviewJourneyE2ETest.allTrackEvidenceKeepsItsScopeAcrossAreaReviewAndRecreation(ReviewJourneyE2ETest.kt:75)

```

Exact aggregate footer:
```text
Time: 29.914
FAILURES!!!
Tests run: 2,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## shell-supporting-native200.log
Source: build/fresh-app-overhaul-20260929/shell-supporting-native200.log
SHA-256: 7733d6ed2749bbaf4114619e4fe81f0008f7acf561a20eab4276bc6c33b740a0

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: Gym chrome must align with Tracks expected:<[Rect.fromLTRB(0.0, 0.0, 1080.0, 300.0), Rect.fromLTRB(795.0, 147.0, 932.0, 284.0), Rect.fromLTRB(53.0, 451.0, 1027.0, 724.0)]> but was:<[Rect.fromLTRB(0.0, 0.0, 1080.0, 305.0), Rect.fromLTRB(795.0, 150.0, 932.0, 287.0), Rect.fromLTRB(53.0, 456.0, 1027.0, 729.0)]>
	at org.junit.Assert.fail(Assert.java:89)
	at org.junit.Assert.failNotEquals(Assert.java:835)
	at org.junit.Assert.assertEquals(Assert.java:120)
	at com.whip.app.VisualCatalogPagesTest.captureSupportingWorkspaceOverview(VisualCatalogPagesTest.kt:105)

```

Exact aggregate footer:
```text
Time: 14.266
FAILURES!!!
Tests run: 1,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## tracks-gym-historical-final.log
Source: build/fresh-app-overhaul-20260929/tracks-gym-historical-final.log
SHA-256: a2574b341b8b95e6b00129f4830fd3727be9c2a9f9c100642ea177337eae87ff

Exact incomplete output:
```text
INSTRUMENTATION_STATUS: class=com.whip.app.GymLibraryJourneyE2ETest
INSTRUMENTATION_STATUS: current=1
INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
INSTRUMENTATION_STATUS: numtests=1
INSTRUMENTATION_STATUS: stream=
com.whip.app.GymLibraryJourneyE2ETest:
INSTRUMENTATION_STATUS: test=historicalSetCorrectionPreservesBothSessionsAndRejectsAStaleEdit
INSTRUMENTATION_STATUS_CODE: 1
```

## tracks-gym-historical-rerun.log
Source: build/fresh-app-overhaul-20260929/tracks-gym-historical-rerun.log
SHA-256: 25e0d439159ffed845f8ce13b03a3d61a4366f09f1eef79961a4774bcea9ad66

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.IllegalArgumentException: Can't scroll to index 100, it is out of bounds [0, 9)
	at androidx.compose.foundation.internal.InlineClassHelperKt.throwIllegalArgumentException(InlineClassHelper.kt:34)
	at androidx.compose.foundation.lazy.layout.LazyLayoutSemanticsModifierNode.updateCachedSemanticsValues$lambda$2(LazyLayoutSemantics.kt:272)
	at androidx.compose.foundation.lazy.layout.LazyLayoutSemanticsModifierNode$$ExternalSyntheticLambda4.invoke(D8$$SyntheticClass:0)
	at androidx.compose.ui.test.ActionsKt.scrollToIndex$lambda$1(Actions.kt:170)
	at androidx.compose.ui.test.ActionsKt$$ExternalSyntheticLambda7.invoke(D8$$SyntheticClass:0)
	at androidx.compose.ui.test.AndroidSynchronization_androidKt.runOnUiThread$lambda$0(AndroidSynchronization.android.kt:39)
	at androidx.compose.ui.test.AndroidSynchronization_androidKt$$ExternalSyntheticLambda0.call(D8$$SyntheticClass:0)
	at java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:487)
	at java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at android.app.Instrumentation$SyncRunnable.run(Instrumentation.java:2420)
	at android.os.Handler.handleCallback(Handler.java:958)
	at android.os.Handler.dispatchMessage(Handler.java:99)
	at android.os.Looper.loopOnce(Looper.java:205)
	at android.os.Looper.loop(Looper.java:294)
	at android.app.ActivityThread.main(ActivityThread.java:8177)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:552)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:971)
	Suppressed: java.util.concurrent.ExecutionException: An Exception occurred on the UI thread during runOnUiThread()
		at androidx.compose.ui.test.AndroidSynchronization_androidKt.runOnUiThread(AndroidSynchronization.android.kt:50)
		at androidx.compose.ui.test.AndroidComposeUiTestEnvironment$AndroidComposeUiTestImpl.runOnUiThread(ComposeUiTest.android.kt:844)
		at androidx.compose.ui.test.AndroidComposeUiTestEnvironment$AndroidTestOwner.runOnUiThread(ComposeUiTest.android.kt:984)
		at androidx.compose.ui.test.ActionsKt.scrollToIndex(Actions.kt:169)
		at androidx.compose.ui.test.ActionsKt.performScrollToIndex(Actions.kt:161)
		at com.whip.app.GymLibraryJourneyE2ETest.historicalSetCorrectionPreservesBothSessionsAndRejectsAStaleEdit(GymLibraryJourneyE2ETest.kt:89)
		at java.lang.reflect.Method.invoke(Native Method)
		at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
		at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
		at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
		at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
		at androidx.test.internal.runner.junit4.statement.RunAfters.evaluate(RunAfters.java:61)
		at androidx.compose.ui.test.junit4.AndroidComposeTestRule$apply$testWithDisposal$1.evaluate(AndroidComposeTestRule.android.kt:426)
		at androidx.compose.ui.test.junit4.AndroidComposeTestRule$apply$1$evaluate$1.invokeSuspend(AndroidComposeTestRule.android.kt:448)
```
Excerpt stops after 35 source lines; source log retains the remainder.

Exact aggregate footer:
```text
Time: 28.488
FAILURES!!!
Tests run: 1,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## tracks-gym-historical.log
Source: build/fresh-app-overhaul-20260929/tracks-gym-historical.log
SHA-256: 292722ccaabeee0123f0eb62f49a58f399251182975a3957f765e04d58fee322

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: expected:<WorkoutSession(id=1, uuid=6d2205f5-b4da-4a21-b4fb-c678432c06d1, name=Earlier workout, notes=, startedAt=2026-09-29T20:17:13.724Z, endedAt=2026-09-29T20:17:13.858Z, localDate=2026-09-29, zoneId=America/Toronto, state=Finished, keepScreenAwake=false, restTimerDeadlineMillis=null, restTimerDurationSeconds=null, archived=false, createdAtMillis=1790713033725, updatedAtMillis=1790713033858, sourceRoutineId=null, sourceRoutineDayId=null, sourceRoutineProgramKind=Static, sourceRoutinePhaseIndex=null, sourceRoutineCycle=null, sourceRoutineDayPosition=null, sourceRoutineDayProgressionIndex=null, programProgressAdvanced=false, requiredMainWorkInvalidated=false, invalidatedMainExerciseIds=[], sourceRoutinePhaseLabel=, sourceRoutinePhaseRole=Standard, workoutRevision=2, restTimerRevision=1, restTimerCleanupPending=true)> but was:<WorkoutSession(id=1, uuid=6d2205f5-b4da-4a21-b4fb-c678432c06d1, name=Earlier workout, notes=, startedAt=2026-09-29T20:17:13.724Z, endedAt=2026-09-29T20:17:13.858Z, localDate=2026-09-29, zoneId=America/Toronto, state=Finished, keepScreenAwake=false, restTimerDeadlineMillis=null, restTimerDurationSeconds=null, archived=false, createdAtMillis=1790713033725, updatedAtMillis=1790713033858, sourceRoutineId=null, sourceRoutineDayId=null, sourceRoutineProgramKind=Static, sourceRoutinePhaseIndex=null, sourceRoutineCycle=null, sourceRoutineDayPosition=null, sourceRoutineDayProgressionIndex=null, programProgressAdvanced=false, requiredMainWorkInvalidated=false, invalidatedMainExerciseIds=[], sourceRoutinePhaseLabel=, sourceRoutinePhaseRole=Standard, workoutRevision=2, restTimerRevision=1, restTimerCleanupPending=false)>
	at org.junit.Assert.fail(Assert.java:89)
	at org.junit.Assert.failNotEquals(Assert.java:835)
	at org.junit.Assert.assertEquals(Assert.java:120)
	at org.junit.Assert.assertEquals(Assert.java:146)
	at com.whip.app.GymLibraryJourneyE2ETest.historicalSetCorrectionPreservesBothSessionsAndRejectsAStaleEdit(GymLibraryJourneyE2ETest.kt:75)

```

Exact aggregate footer:
```text
Time: 19.162
FAILURES!!!
Tests run: 1,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## tracks-gym-malformed200-final.log
Source: build/fresh-app-overhaul-20260929/tracks-gym-malformed200-final.log
SHA-256: 79854766bf04c2e0b7359d451dc6a47653ca2f4cbc7f9de8de2300927c1579ee

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.IllegalStateException: Required value was null.
	at com.whip.app.GymPowerInputUiTest.setDetailsKeepsMalformedOptionalTextThroughRestoreAndDiscardReview(GymPowerInputUiTest.kt:174)

```

Exact aggregate footer:
```text
Time: 15.54
FAILURES!!!
Tests run: 1,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## tracks-gym-malformed200-rerun.log
Source: build/fresh-app-overhaul-20260929/tracks-gym-malformed200-rerun.log
SHA-256: 65826f3c7c660625cfcc481a5ce51455aeed16e857fb9c85c308dfc0e918993b

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: Complete Save must remain above the keyboard: Rect(853, 311 - 996, 402) versus Rect(0, 1604 - 1080, 2274)
	at org.junit.Assert.fail(Assert.java:89)
	at org.junit.Assert.assertTrue(Assert.java:42)
	at com.whip.app.GymPowerInputUiTest.setDetailsKeepsMalformedOptionalTextThroughRestoreAndDiscardReview(GymPowerInputUiTest.kt:181)

```

Exact aggregate footer:
```text
Time: 12.545
FAILURES!!!
Tests run: 1,  Failures: 1
INSTRUMENTATION_CODE: -1
```

## tracks-gym-repeated-date.log
Source: build/fresh-app-overhaul-20260929/tracks-gym-repeated-date.log
SHA-256: 248ffbed504bb4efb750d0d3591489705fcc8dd6f163270be53f60622af62185

Exact first failure block (at most 35 lines):
```text
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: Failed to perform checkIsDisplayed check: Expected at most 1 node but found 2 nodes that satisfy ((Text + InputText + EditableText contains 'Evening' (ignoreCase: false)) && (hasAnyAncestorThat(TestTag = 'track-entry-detail-surface')))
	at androidx.compose.ui.test.AndroidAssertions_androidKt.checkIsDisplayed(AndroidAssertions.android.kt:35)
	at androidx.compose.ui.test.AssertionsKt.isDisplayed(Assertions.kt:380)
	at androidx.compose.ui.test.AssertionsKt.assertIsDisplayed(Assertions.kt:33)
	at com.whip.app.TrackInsightsJourneyE2ETest.repeatedDateEvidenceOpensAndCorrectsItsExactEntryWithoutLosingInsights(TrackInsightsJourneyE2ETest.kt:83)

```

Exact aggregate footer:
```text
Time: 9.133
FAILURES!!!
Tests run: 1,  Failures: 1
INSTRUMENTATION_CODE: -1
```
