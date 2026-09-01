package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whip.app.WhipApplication
import com.whip.app.core.OperationStatus
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real-application integration coverage for the request ownership that surrounds exact Gym deletion.
 *
 * DomainDeletionCoordinatorTest owns the full dependency/count transaction matrix. These tests keep
 * the database and repositories real while concentrating on UI-process boundaries: atomic admission,
 * process-restored request adoption, read-only outcome verification, generation invalidation, and
 * one-owner terminal-result consumption.
 */
@RunWith(AndroidJUnit4::class)
class GymDeletionViewModelIntegrationTest {
    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    private lateinit var store: ViewModelStore
    private lateinit var viewModel: GymViewModel

    @Before
    fun resetAndCreateViewModel() = runBlocking {
        app.backupRepository.deleteAllData()
        replaceViewModel()
    }

    @After
    fun clearViewModelAndData() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        runBlocking { app.backupRepository.deleteAllData() }
    }

    @Test
    fun rapidDoubleConfirmAdmitsOnlyOneDeletionAndTerminalResultHasOneOwner() = runBlocking {
        val exerciseId = createExercise("Atomic press")
        val impact = previewExercise(exerciseId)

        assertTrue(
            viewModel.deleteExercisePermanently(
                exerciseId,
                impact.revisionToken,
                requestId = "exercise:first",
            ),
        )
        assertFalse(
            viewModel.deleteExercisePermanently(
                exerciseId,
                impact.revisionToken,
                requestId = "exercise:second",
            ),
        )

        val finished = awaitFinished("exercise:first")
        val success = finished.result as WhipResult.Success
        assertEquals(GymDeletionKind.Exercise, success.value.kind)
        assertEquals(exerciseId, success.value.targetId)
        assertNull(app.database.gymDao().getExercise(exerciseId))

        // Another surface cannot consume the outcome, while the owning request can consume it once.
        viewModel.consumeGymDeletionResult("exercise:second")
        assertEquals(finished, viewModel.gymDeletionState.value)
        viewModel.consumeGymDeletionResult("exercise:first")
        assertEquals(PersistenceRequestState.Idle, viewModel.gymDeletionState.value)
        viewModel.consumeGymDeletionResult("exercise:first")
        assertEquals(PersistenceRequestState.Idle, viewModel.gymDeletionState.value)
    }

    @Test
    fun restoredRequestWithPresentExerciseIsAdoptedThenReportedInterruptedWithoutDeleting() = runBlocking {
        val exerciseId = createExercise("Still present row")
        replaceViewModel() // A new ViewModel models process recreation, not Activity rotation.

        assertTrue(viewModel.adoptOrphanedGymDeletionRequest("restored:present"))
        assertFalse(viewModel.adoptOrphanedGymDeletionRequest("restored:duplicate"))
        assertEquals("restored:present", viewModel.orphanedGymDeletionRequestId.value)

        val impact = previewExercise(exerciseId)
        assertEquals(exerciseId, impact.exerciseId)
        viewModel.finishOrphanedGymDeletionAsInterrupted("restored:present")

        val finished = awaitFinished("restored:present")
        val failure = finished.result as WhipResult.Failure
        assertTrue(failure.message.contains("interrupted before it committed"))
        assertNull(viewModel.orphanedGymDeletionRequestId.value)
        assertNotNull(app.database.gymDao().getExercise(exerciseId))
    }

    @Test
    fun restoredRequestWithAbsentExerciseReconcilesToAchievedExactlyOnce() = runBlocking {
        val exerciseId = createExercise("Already removed row")
        val exercise = requireNotNull(app.database.gymDao().getExercise(exerciseId))
        val impact = requireNotNull(app.domainDeletionCoordinator.previewExerciseDeletion(exerciseId))
        app.domainDeletionCoordinator.deleteExercise(exerciseId, impact.revisionToken)
        assertNull(app.database.gymDao().getExercise(exerciseId))
        replaceViewModel()

        val generation = viewModel.currentDataGeneration()
        assertTrue(viewModel.adoptOrphanedGymDeletionRequest("restored:absent"))
        viewModel.finishOrphanedExerciseDeletionAsAchieved(
            requestId = "restored:absent",
            exerciseId = exerciseId,
            exerciseUuid = exercise.uuid,
            expectedDataGeneration = generation,
        )
        // A duplicate settlement cannot start a second reconciliation job.
        viewModel.finishOrphanedExerciseDeletionAsAchieved(
            requestId = "restored:absent",
            exerciseId = exerciseId,
            exerciseUuid = exercise.uuid,
            expectedDataGeneration = generation,
        )

        val finished = awaitFinished("restored:absent")
        val success = finished.result as WhipResult.Success
        assertEquals(GymDeletionKind.Exercise, success.value.kind)
        assertEquals(exerciseId, success.value.targetId)
        assertNull(viewModel.orphanedGymDeletionRequestId.value)
        val operation = viewModel.operationStatus.value as OperationStatus.Succeeded
        assertTrue(operation.message.contains("already absent"))
    }

    @Test
    fun restoredRequestWithAbsentRoutineReconcilesWithoutRecreatingOrDeletingHistory() = runBlocking {
        val exerciseId = createExercise("Routine member")
        val routineId = app.routineRepository.createRoutine(
            RoutineDraft(
                name = "Already removed routine",
                days = listOf(
                    RoutineDayDraft("Day", listOf(RoutineExerciseDraft(exerciseId))),
                ),
            ),
        )
        val impact = requireNotNull(app.domainDeletionCoordinator.previewRoutineDeletion(routineId))
        app.domainDeletionCoordinator.deleteRoutine(routineId, impact.revisionToken)
        assertNull(app.database.routineDao().getRoutine(routineId))
        replaceViewModel()

        val generation = viewModel.currentDataGeneration()
        assertTrue(viewModel.adoptOrphanedGymDeletionRequest("restored:routine-absent"))
        viewModel.finishOrphanedRoutineDeletionAsAchieved(
            requestId = "restored:routine-absent",
            routineId = routineId,
            expectedDataGeneration = generation,
        )

        val finished = awaitFinished("restored:routine-absent")
        val success = finished.result as WhipResult.Success
        assertEquals(GymDeletionKind.Routine, success.value.kind)
        assertEquals(routineId, success.value.targetId)
        assertNull(viewModel.orphanedGymDeletionRequestId.value)
        assertNull(app.database.routineDao().getRoutine(routineId))
        assertNotNull(app.database.gymDao().getExercise(exerciseId))
    }

    @Test
    fun unverifiedOutcomeCanBeReplacedTwiceAndOnlyLatestRequestCanSettle() = runBlocking {
        val exerciseId = createExercise("Retry verification row")
        val exercise = requireNotNull(app.database.gymDao().getExercise(exerciseId))
        val impact = requireNotNull(app.domainDeletionCoordinator.previewExerciseDeletion(exerciseId))
        replaceViewModel()

        assertTrue(viewModel.adoptOrphanedGymDeletionRequest("verify:one"))
        viewModel.finishOrphanedGymDeletionAsUnverified("verify:one")
        val firstFailure = awaitFinished("verify:one").result as WhipResult.Failure
        assertTrue(firstFailure.message.contains("could not be verified"))
        assertEquals("verify:one", viewModel.orphanedGymDeletionRequestId.value)

        assertTrue(viewModel.restartOrphanedGymDeletionVerification("verify:one", "verify:two"))
        assertFalse(viewModel.restartOrphanedGymDeletionVerification("verify:one", "verify:stale"))
        viewModel.finishOrphanedGymDeletionAsUnverified("verify:two")
        awaitFinished("verify:two")
        assertTrue(viewModel.restartOrphanedGymDeletionVerification("verify:two", "verify:three"))

        app.domainDeletionCoordinator.deleteExercise(exerciseId, impact.revisionToken)
        viewModel.finishOrphanedExerciseDeletionAsAchieved(
            requestId = "verify:one",
            exerciseId = exerciseId,
            exerciseUuid = exercise.uuid,
            expectedDataGeneration = viewModel.currentDataGeneration(),
        )
        assertEquals(PersistenceRequestState.Running("verify:three"), viewModel.gymDeletionState.value)

        viewModel.finishOrphanedExerciseDeletionAsAchieved(
            requestId = "verify:three",
            exerciseId = exerciseId,
            exerciseUuid = exercise.uuid,
            expectedDataGeneration = viewModel.currentDataGeneration(),
        )
        val success = awaitFinished("verify:three").result as WhipResult.Success
        assertEquals(exerciseId, success.value.targetId)
        assertNull(viewModel.orphanedGymDeletionRequestId.value)
    }

    @Test
    fun unverifiedPresentTargetSurvivesProcessReplacementAndFreshVerificationInterruptsSafely() = runBlocking {
        val exerciseId = createExercise("Second-process present row")
        val firstProcessHandle = SavedStateHandle()
        replaceViewModel(firstProcessHandle)

        assertTrue(viewModel.adoptOrphanedGymDeletionRequest("process-present:unverified"))
        viewModel.finishOrphanedGymDeletionAsUnverified("process-present:unverified")
        awaitFinished("process-present:unverified")
        assertEquals("process-present:unverified", viewModel.orphanedGymDeletionRequestId.value)

        val restoredHandle = restoredCopy(firstProcessHandle)
        replaceViewModel(restoredHandle)
        assertEquals(PersistenceRequestState.Idle, viewModel.gymDeletionState.value)
        assertEquals("process-present:unverified", viewModel.orphanedGymDeletionRequestId.value)
        assertTrue(
            viewModel.restartOrphanedGymDeletionVerification(
                previousRequestId = "process-present:unverified",
                requestId = "process-present:fresh-read",
            ),
        )

        assertEquals(exerciseId, previewExercise(exerciseId).exerciseId)
        viewModel.finishOrphanedGymDeletionAsInterrupted("process-present:fresh-read")

        val failure = awaitFinished("process-present:fresh-read").result as WhipResult.Failure
        assertTrue(failure.message.contains("interrupted before it committed"))
        assertNull(viewModel.orphanedGymDeletionRequestId.value)
        assertNotNull(app.database.gymDao().getExercise(exerciseId))
    }

    @Test
    fun unverifiedAbsentTargetSurvivesProcessReplacementAndFreshVerificationReconciles() = runBlocking {
        val exerciseId = createExercise("Second-process absent row")
        val exercise = requireNotNull(app.database.gymDao().getExercise(exerciseId))
        val impact = requireNotNull(app.domainDeletionCoordinator.previewExerciseDeletion(exerciseId))
        val firstProcessHandle = SavedStateHandle()
        replaceViewModel(firstProcessHandle)

        assertTrue(viewModel.adoptOrphanedGymDeletionRequest("process-absent:unverified"))
        viewModel.finishOrphanedGymDeletionAsUnverified("process-absent:unverified")
        awaitFinished("process-absent:unverified")
        app.domainDeletionCoordinator.deleteExercise(exerciseId, impact.revisionToken)
        assertNull(app.database.gymDao().getExercise(exerciseId))

        val restoredHandle = restoredCopy(firstProcessHandle)
        replaceViewModel(restoredHandle)
        assertEquals(PersistenceRequestState.Idle, viewModel.gymDeletionState.value)
        assertEquals("process-absent:unverified", viewModel.orphanedGymDeletionRequestId.value)
        assertTrue(
            viewModel.restartOrphanedGymDeletionVerification(
                previousRequestId = "process-absent:unverified",
                requestId = "process-absent:fresh-read",
            ),
        )

        viewModel.previewExerciseDeletion(exerciseId)
        withTimeout(5_000) { viewModel.exerciseDeletionTargetMissing.first { it } }
        viewModel.finishOrphanedExerciseDeletionAsAchieved(
            requestId = "process-absent:fresh-read",
            exerciseId = exerciseId,
            exerciseUuid = exercise.uuid,
            expectedDataGeneration = viewModel.currentDataGeneration(),
        )

        val success = awaitFinished("process-absent:fresh-read").result as WhipResult.Success
        assertEquals(GymDeletionKind.Exercise, success.value.kind)
        assertEquals(exerciseId, success.value.targetId)
        assertNull(viewModel.orphanedGymDeletionRequestId.value)
        assertNull(app.database.gymDao().getExercise(exerciseId))
        val operation = viewModel.operationStatus.value as OperationStatus.Succeeded
        assertTrue(operation.message.contains("already absent"))
    }

    @Test
    fun activityRotationKeepsTheSameRunningOwnerAndDoesNotRedeliverAfterConsumption() = runBlocking {
        assertTrue(viewModel.adoptOrphanedGymDeletionRequest("rotation:owner"))

        lateinit var afterRotation: GymViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // Activity recreation retains its ViewModelStore; asking the retained store again must
            // return the same request owner rather than constructing a second delivery surface.
            afterRotation = provider(store)[GymViewModel::class.java]
        }
        assertSame(viewModel, afterRotation)
        assertEquals(PersistenceRequestState.Running("rotation:owner"), afterRotation.gymDeletionState.value)

        afterRotation.finishOrphanedGymDeletionAsInterrupted("rotation:owner")
        awaitFinished("rotation:owner")
        afterRotation.consumeGymDeletionResult("rotation:owner")
        assertEquals(PersistenceRequestState.Idle, afterRotation.gymDeletionState.value)

        lateinit var sameOwnerAfterDelivery: GymViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            sameOwnerAfterDelivery = provider(store)[GymViewModel::class.java]
        }
        assertSame(afterRotation, sameOwnerAfterDelivery)
        assertEquals(PersistenceRequestState.Idle, sameOwnerAfterDelivery.gymDeletionState.value)
    }

    @Test
    fun userDataGenerationChangeClearsTheOldOwnerAndPreventsStaleSuccess() = runBlocking {
        val exerciseId = createExercise("Generation boundary row")
        val exercise = requireNotNull(app.database.gymDao().getExercise(exerciseId))
        val snapshot = app.backupRepository.exportBackup()
        val oldGeneration = viewModel.currentDataGeneration()
        assertTrue(viewModel.adoptOrphanedGymDeletionRequest("generation:old"))

        withTimeout(20_000) { app.restoreBackup(snapshot) }
        withTimeout(5_000) {
            combine(viewModel.gymDeletionState, viewModel.orphanedGymDeletionRequestId) { state, orphan ->
                state to orphan
            }.first { (state, orphan) -> state == PersistenceRequestState.Idle && orphan == null }
        }
        assertTrue(viewModel.currentDataGeneration() != oldGeneration)

        viewModel.finishOrphanedExerciseDeletionAsAchieved(
            requestId = "generation:old",
            exerciseId = exerciseId,
            exerciseUuid = exercise.uuid,
            expectedDataGeneration = oldGeneration,
        )

        assertEquals(PersistenceRequestState.Idle, viewModel.gymDeletionState.value)
        assertNull(viewModel.orphanedGymDeletionRequestId.value)
        assertNotNull(app.database.gymDao().getExercise(exerciseId))
        assertFalse(
            (viewModel.operationStatus.value as? OperationStatus.Succeeded)
                ?.message.orEmpty().contains("already absent"),
        )
    }

    private suspend fun createExercise(name: String): Long =
        app.gymRepository.createExercise(ExerciseDraft(name = name))

    private suspend fun previewExercise(exerciseId: Long) = withTimeout(5_000) {
        viewModel.previewExerciseDeletion(exerciseId)
        viewModel.exerciseDeletionImpact.first { it?.exerciseId == exerciseId }
            ?: error("Exercise deletion preview disappeared")
    }

    private suspend fun awaitFinished(
        requestId: String,
    ): PersistenceRequestState.Finished<GymDeletionReceipt> = withTimeout(10_000) {
        @Suppress("UNCHECKED_CAST")
        (viewModel.gymDeletionState.first { state ->
            state is PersistenceRequestState.Finished && state.requestId == requestId
        } as PersistenceRequestState.Finished<GymDeletionReceipt>)
    }

    private fun replaceViewModel(savedStateHandle: SavedStateHandle? = null) {
        if (::store.isInitialized) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            store = ViewModelStore()
            viewModel = provider(store, savedStateHandle)[GymViewModel::class.java]
        }
    }

    private fun provider(
        targetStore: ViewModelStore,
        savedStateHandle: SavedStateHandle? = null,
    ): ViewModelProvider {
        val factory = if (savedStateHandle == null) {
            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        } else {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GymViewModel(app, savedStateHandle) as T
            }
        }
        return ViewModelProvider(targetStore, factory)
    }

    private fun restoredCopy(source: SavedStateHandle): SavedStateHandle = SavedStateHandle(
        source.keys().associateWith { key -> source.get<Any?>(key) },
    )
}
