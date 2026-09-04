package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.domain.OccurrenceState
import com.whip.app.reminders.ReminderDeliveryCoordinator
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException

/** Owns durable task deletion. Task children are removed by the canonical Room cascades. */
class TaskDeletionCoordinator internal constructor(
    private val database: WhipDatabase,
    private val taskRepository: TaskRepository,
    private val reminderDeliveryCoordinator: ReminderDeliveryCoordinator? = null,
    private val onDeletionPrepared: (Set<Long>) -> Unit = {},
    private val onDeletionCommitted: (Set<Long>) -> Unit = {},
    private val onDeletionInterrupted: suspend () -> Unit = {},
) {
    internal suspend fun deleteWithinTransaction(taskId: Long): TaskDeletionSummary {
        val impact = previewBatchWithinTransaction(setOf(taskId))
        return if (impact.taskIds.isEmpty()) TaskDeletionSummary() else deleteImpactWithinTransaction(impact).toSingleSummary()
    }

    suspend fun preview(taskId: Long): TaskDeletionImpact = database.withTransaction { previewWithinTransaction(taskId) }
    suspend fun preview(taskIds: Set<Long>): TaskDeletionBatchImpact = database.withTransaction { previewBatchWithinTransaction(taskIds) }

    suspend fun delete(taskId: Long, expectedRevisionToken: String? = null): TaskDeletionSummary = try {
        delete(setOf(taskId), expectedRevisionToken?.let { mapOf(taskId to it) }).toSingleSummary()
    } catch (cancelled: CommittedTaskDeletionBatchCancellation) {
        throw CommittedTaskDeletionCancellation(cancelled.summary.toSingleSummary(), cancelled)
    }

    suspend fun delete(
        taskIds: Set<Long>,
        expectedRevisionTokens: Map<Long, String>? = null,
    ): TaskDeletionBatchSummary {
        val requested = taskIds.filterTo(linkedSetOf()) { it > 0 }
        return try {
            withReminderStateBoundary {
                onDeletionPrepared(requested)
                val summary = database.withTransaction {
                    val impact = previewBatchWithinTransaction(requested)
                    require(expectedRevisionTokens == null ||
                        (impact.taskIds == impact.requestedTaskIds && expectedRevisionTokens == impact.revisionTokens)) {
                        "One or more tasks changed after the deletion preview. Review the updated impact before deleting."
                    }
                    if (impact.taskIds.isEmpty()) TaskDeletionBatchSummary() else deleteImpactWithinTransaction(impact)
                }
                try {
                    onDeletionCommitted(requested)
                    summary
                } catch (cancelled: CancellationException) {
                    throw CommittedTaskDeletionBatchCancellation(summary, cancelled)
                } catch (_: Exception) {
                    summary.copy(warnings = listOf("Reminder cleanup did not finish; the permanent deletion was committed and will be reconciled later."))
                }
            }
        } catch (error: Throwable) {
            notifyDeletionInterrupted(error)
            throw error
        }
    }

    suspend fun undoPromotion(
        promotedTaskId: Long,
        expectedRevisionToken: String,
        sourceTaskId: Long,
        sourceStepId: Long,
        expectedSourceStepUpdatedAtMillis: Long,
    ): TaskDeletionSummary {
        val requested = setOf(promotedTaskId)
        return try {
            withReminderStateBoundary {
                onDeletionPrepared(requested)
                val summary = database.withTransaction {
                    val impact = previewBatchWithinTransaction(requested)
                    require(impact.taskIds == requested && impact.revisionTokens[promotedTaskId] == expectedRevisionToken) {
                        "The promoted Task changed and cannot be safely removed"
                    }
                    val source = database.taskDao().getSteps(sourceTaskId).firstOrNull { it.id == sourceStepId }
                        ?: error("The source Subtask no longer exists")
                    require(source.archived && source.updatedAtMillis == expectedSourceStepUpdatedAtMillis) {
                        "The source Subtask changed and cannot be safely restored"
                    }
                    deleteImpactWithinTransaction(impact).also { database.taskDao().updateStep(source.copy(archived = false)) }
                }
                try {
                    onDeletionCommitted(requested)
                    summary.toSingleSummary()
                } catch (cancelled: CancellationException) {
                    throw CommittedTaskDeletionCancellation(summary.toSingleSummary(), cancelled)
                } catch (_: Exception) {
                    summary.toSingleSummary().copy(warnings = listOf("Reminder cleanup did not finish; the promotion undo was committed and will be reconciled later."))
                }
            }
        } catch (error: Throwable) {
            notifyDeletionInterrupted(error)
            throw error
        }
    }

    private suspend fun deleteImpactWithinTransaction(impact: TaskDeletionBatchImpact): TaskDeletionBatchSummary {
        impact.taskIds.sorted().forEach { check(taskRepository.deletePermanently(it)) { "Task no longer exists" } }
        return TaskDeletionBatchSummary(tasksDeleted = impact.taskIds.size)
    }

    private suspend fun previewWithinTransaction(taskId: Long): TaskDeletionImpact {
        val batch = previewBatchWithinTransaction(setOf(taskId))
        return TaskDeletionImpact(
            taskId = taskId,
            exists = taskId in batch.taskIds,
            title = batch.titles.singleOrNull().orEmpty(),
            recurring = batch.recurringSeriesCount == 1,
            recordedOccurrenceCount = batch.recordedOccurrenceCount,
            completedOccurrenceCount = batch.completedOccurrenceCount,
            skippedOccurrenceCount = batch.skippedOccurrenceCount,
            openOccurrenceCount = batch.openOccurrenceCount,
            stepCount = batch.stepCount,
            revisionToken = batch.revisionTokens[taskId].orEmpty(),
        )
    }

    private suspend fun previewBatchWithinTransaction(taskIds: Set<Long>): TaskDeletionBatchImpact {
        val requested = taskIds.filterTo(linkedSetOf()) { it > 0 }
        val tasks = requested.sorted().mapNotNull { taskRepository.getTask(it) }
        val existing = tasks.mapTo(linkedSetOf()) { it.id }
        val steps = existing.flatMap { database.taskDao().getSteps(it) }
        val occurrences = existing.flatMap { taskRepository.getOccurrences(it) }
        val states = database.taskDao().getAllStepStates().filter { it.taskId in existing }
        val snapshots = database.taskDao().getAllStepSnapshots().filter { it.taskId in existing }
        val oneShotCompleted = tasks.count { it.recurrence == null && it.completedAtMillis != null }
        return TaskDeletionBatchImpact(
            requestedTaskIds = requested,
            taskIds = existing,
            titles = tasks.map { it.title },
            recurringSeriesCount = tasks.count { it.recurrence != null },
            recordedOccurrenceCount = occurrences.size + oneShotCompleted,
            completedOccurrenceCount = occurrences.count { it.state == OccurrenceState.Completed } + oneShotCompleted,
            skippedOccurrenceCount = occurrences.count { it.state == OccurrenceState.Skipped },
            openOccurrenceCount = occurrences.count { it.state == OccurrenceState.Open },
            stepCount = steps.count { !it.archived },
            revisionTokens = tasks.associate { task ->
                task.id to taskDeletionRevision(
                    task,
                    occurrences.filter { it.taskId == task.id },
                    steps.filter { it.taskId == task.id },
                    states.filter { it.taskId == task.id },
                    snapshots.filter { it.taskId == task.id },
                )
            },
        )
    }

    private suspend fun <T> withReminderStateBoundary(block: suspend () -> T): T = reminderDeliveryCoordinator?.withStateBoundary(block) ?: block()
    private suspend fun notifyDeletionInterrupted(original: Throwable) {
        try { onDeletionInterrupted() } catch (fatal: Error) { fatal.addSuppressed(original); throw fatal } catch (secondary: Exception) { original.addSuppressed(secondary) }
    }
}

private fun taskDeletionRevision(task: Any, occurrences: List<*>, steps: List<*>, states: List<*>, snapshots: List<*>): String {
    val canonical = buildString {
        append("task|").append(task).append('\n')
        listOf("occurrence" to occurrences, "step" to steps, "step-state" to states, "step-snapshot" to snapshots).forEach { (label, rows) ->
            rows.map(Any?::toString).sorted().forEach { append(label).append('|').append(it).append('\n') }
        }
    }
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()).joinToString("") { "%02x".format(it) }
}

private fun TaskDeletionBatchSummary.toSingleSummary() = TaskDeletionSummary(tasksDeleted == 1, warnings = warnings)

data class TaskDeletionImpact(val taskId: Long, val exists: Boolean = false, val title: String = "", val recurring: Boolean = false, val recordedOccurrenceCount: Int = 0, val completedOccurrenceCount: Int = 0, val skippedOccurrenceCount: Int = 0, val openOccurrenceCount: Int = 0, val stepCount: Int = 0, val revisionToken: String = "")
data class TaskDeletionSummary(val taskDeleted: Boolean = false, val warnings: List<String> = emptyList())
data class TaskDeletionBatchImpact(val requestedTaskIds: Set<Long> = emptySet(), val taskIds: Set<Long> = emptySet(), val titles: List<String> = emptyList(), val recurringSeriesCount: Int = 0, val recordedOccurrenceCount: Int = 0, val completedOccurrenceCount: Int = 0, val skippedOccurrenceCount: Int = 0, val openOccurrenceCount: Int = 0, val stepCount: Int = 0, val revisionTokens: Map<Long, String> = emptyMap())
data class TaskDeletionBatchSummary(val tasksDeleted: Int = 0, val warnings: List<String> = emptyList())
class CommittedTaskDeletionCancellation(val summary: TaskDeletionSummary, cause: CancellationException) : CancellationException(cause.message) { init { initCause(cause) } }
class CommittedTaskDeletionBatchCancellation(val summary: TaskDeletionBatchSummary, cause: CancellationException) : CancellationException(cause.message) { init { initCause(cause) } }
