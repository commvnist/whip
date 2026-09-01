package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.reminders.ReminderDeliveryCoordinator
import java.util.concurrent.CancellationException

/** Performs an all-or-nothing deletion of an Area and every item assigned to it. */
class AreaDeletionCoordinator internal constructor(
    private val database: WhipDatabase,
    private val areaRepository: AreaRepository,
    private val taskDeletionCoordinator: TaskDeletionCoordinator,
    private val domainDeletionCoordinator: DomainDeletionCoordinator,
    private val reminderDeliveryCoordinator: ReminderDeliveryCoordinator? = null,
    private val onDeletionPrepared: (AreaDeletionSummary) -> Unit = {},
    private val onDeletionCommitted: (AreaDeletionSummary) -> Unit = {},
    private val onDeletionInterrupted: suspend () -> Unit = {},
) {
    suspend fun deleteAreaAndItems(areaId: String): AreaDeletionSummary {
        var committedSummary: AreaDeletionSummary? = null
        val warnings = mutableListOf<String>()
        try {
            withReminderStateBoundary {
                val summary = database.withTransaction {
                    requireNotNull(database.measurementDao().getArea(areaId)) { "Area no longer exists" }
                    val taskIds = database.taskDao().getAllTasks().filter { it.areaId == areaId }.map { it.id }
                    val habitIds = database.habitDao().getAllHabits().filter { it.areaId == areaId }.map { it.id }
                    val goalIds = database.goalDao().getAllGoals().filter { it.areaId == areaId }.map { it.id }
                    val trackIds = database.trackDao().getAllTracks().filter { it.areaId == areaId }.map { it.id }
                    val affected = AreaDeletionSummary(taskIds, habitIds, goalIds, trackIds)
                    onDeletionPrepared(affected)

                    taskIds.forEach { taskDeletionCoordinator.deleteWithinTransaction(it) }
                    habitIds.forEach { domainDeletionCoordinator.deleteHabitWithinTransaction(it) }
                    goalIds.forEach { domainDeletionCoordinator.deleteGoalWithinTransaction(it) }
                    trackIds.forEach { domainDeletionCoordinator.deleteTrackWithinTransaction(it) }
                    areaRepository.deletePermanently(areaId)

                    affected
                }
                committedSummary = summary
                try {
                    domainDeletionCoordinator.rebuildLinksAfterCommittedDeletion()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    warnings += "Link reconciliation did not finish; the Area deletion was committed and will be reconciled later."
                }
                try {
                    onDeletionCommitted(summary)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    warnings += "Reminder cleanup did not finish; the Area deletion was committed and will be reconciled later."
                }
            }
        } catch (cancelled: CancellationException) {
            notifyDeletionInterrupted(cancelled)
            val committed = committedSummary
            if (committed != null) {
                throw CommittedAreaDeletionCancellation(committed.copy(warnings = warnings), cancelled)
            }
            throw cancelled
        } catch (error: Throwable) {
            notifyDeletionInterrupted(error)
            throw error
        }
        val committed = requireNotNull(committedSummary).copy(warnings = warnings)
        if (warnings.isNotEmpty()) {
            try {
                onDeletionInterrupted()
            } catch (cancelled: CancellationException) {
                throw CommittedAreaDeletionCancellation(committed, cancelled)
            } catch (fatal: Error) {
                throw fatal
            } catch (_: Exception) {
                // Durable cleanup markers are reconciled on the next startup.
            }
        }
        return committed
    }

    private suspend fun <T> withReminderStateBoundary(block: suspend () -> T): T =
        reminderDeliveryCoordinator?.withStateBoundary(block) ?: block()

    private suspend fun notifyDeletionInterrupted(original: Throwable) {
        try {
            onDeletionInterrupted()
        } catch (fatal: Error) {
            fatal.addSuppressed(original)
            throw fatal
        } catch (secondary: CancellationException) {
            original.addSuppressed(secondary)
        } catch (secondary: Exception) {
            original.addSuppressed(secondary)
        }
    }
}

data class AreaDeletionSummary(
    val taskIds: List<Long>,
    val habitIds: List<Long>,
    val goalIds: List<Long>,
    val trackIds: List<Long>,
    val warnings: List<String> = emptyList(),
) {
    val total: Int get() = taskIds.size + habitIds.size + goalIds.size + trackIds.size
}

class CommittedAreaDeletionCancellation(
    val summary: AreaDeletionSummary,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init { initCause(cause) }
}
