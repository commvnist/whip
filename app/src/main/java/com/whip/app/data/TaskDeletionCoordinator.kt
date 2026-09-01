package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.OccurrenceState
import com.whip.app.reminders.ReminderDeliveryCoordinator

/**
 * Owns the cross-feature cleanup required when a task is permanently deleted.
 *
 * Direct task children use foreign-key cascades. Links and automations are intentionally
 * polymorphic, so they must be removed explicitly to avoid orphan rules and derived data.
 */
class TaskDeletionCoordinator internal constructor(
    private val database: WhipDatabase,
    private val taskRepository: TaskRepository,
    private val linkRepository: LinkRepository,
    private val reminderDeliveryCoordinator: ReminderDeliveryCoordinator? = null,
    private val onDeletionPrepared: (Set<Long>) -> Unit = {},
    private val onDeletionCommitted: (Set<Long>) -> Unit = {},
    private val onDeletionInterrupted: suspend () -> Unit = {},
) {
    suspend fun preview(taskId: Long): TaskDeletionImpact = database.withTransaction {
        previewWithinTransaction(taskId)
    }

    suspend fun preview(taskIds: Set<Long>): TaskDeletionBatchImpact = database.withTransaction {
        previewBatchWithinTransaction(taskIds)
    }

    suspend fun delete(
        taskId: Long,
        expectedRevisionToken: Long? = null,
    ): TaskDeletionSummary {
        val result = delete(
            taskIds = setOf(taskId),
            expectedRevisionTokens = expectedRevisionToken?.let { mapOf(taskId to it) },
        )
        return TaskDeletionSummary(
            taskDeleted = result.tasksDeleted == 1,
            linkRulesDeleted = result.linkRulesDeleted,
            automationRulesDeleted = result.automationRulesDeleted,
        )
    }

    suspend fun delete(
        taskIds: Set<Long>,
        expectedRevisionTokens: Map<Long, Long>? = null,
    ): TaskDeletionBatchSummary {
        val requestedTaskIds = taskIds.filterTo(linkedSetOf()) { it > 0 }
        return try {
            withReminderStateBoundary {
                onDeletionPrepared(requestedTaskIds)
                val summary = database.withTransaction {
                    val impact = previewBatchWithinTransaction(taskIds)
                    if (impact.taskIds.isEmpty()) return@withTransaction TaskDeletionBatchSummary()
                    require(
                        expectedRevisionTokens == null ||
                            (impact.taskIds == impact.requestedTaskIds &&
                                expectedRevisionTokens == impact.revisionTokens),
                    ) {
                        "One or more tasks changed after the deletion preview. Review the updated impact before deleting."
                    }

                    impact.linkRuleIds.forEach { linkRepository.deleteRule(it) }
                    impact.automationRuleIds.forEach { linkRepository.deleteTrigger(it) }
                    impact.taskIds.sorted().forEach { taskId ->
                        check(taskRepository.deletePermanently(taskId)) { "Task no longer exists" }
                    }
                    TaskDeletionBatchSummary(
                        tasksDeleted = impact.taskIds.size,
                        linkRulesDeleted = impact.linkRuleIds.size,
                        automationRulesDeleted = impact.automationRuleIds.size,
                    )
                }
                onDeletionCommitted(requestedTaskIds)
                summary
            }
        } catch (error: Throwable) {
            onDeletionInterrupted()
            throw error
        }
    }

    private suspend fun <T> withReminderStateBoundary(block: suspend () -> T): T =
        reminderDeliveryCoordinator?.withStateBoundary(block) ?: block()

    private suspend fun previewWithinTransaction(taskId: Long): TaskDeletionImpact {
        val task = taskRepository.getTask(taskId) ?: return TaskDeletionImpact(taskId = taskId)
        val occurrences = taskRepository.getOccurrences(taskId)
        val steps = database.taskDao().getSteps(taskId)
        val stepIds = steps.mapTo(mutableSetOf()) { it.id }
        val linkRules = database.linkDao().getRules().count { rule ->
            (rule.sourceType == LinkSourceType.Task.name && rule.sourceEntityId == taskId) ||
                (rule.sourceType == LinkSourceType.Subtask.name && rule.sourceEntityId in stepIds)
        }
        val automations = database.linkDao().getTriggerRules().count { rule ->
            (rule.sourceType == LinkSourceType.Task.name && rule.sourceEntityId == taskId) ||
                (rule.sourceType == LinkSourceType.Subtask.name && rule.sourceEntityId in stepIds) ||
                (rule.targetType == TriggerTargetType.Task.name && rule.targetEntityId == taskId)
        }
        return TaskDeletionImpact(
            taskId = taskId,
            exists = true,
            title = task.title,
            recurring = task.recurrence != null,
            recordedOccurrenceCount = occurrences.size + if (task.recurrence == null && task.completedAtMillis != null) 1 else 0,
            completedOccurrenceCount = occurrences.count { it.state == OccurrenceState.Completed } +
                if (task.recurrence == null && task.completedAtMillis != null) 1 else 0,
            skippedOccurrenceCount = occurrences.count { it.state == OccurrenceState.Skipped },
            openOccurrenceCount = occurrences.count { it.state == OccurrenceState.Open },
            stepCount = steps.count { !it.archived },
            linkRuleCount = linkRules,
            automationRuleCount = automations,
            revisionToken = task.updatedAtMillis,
        )
    }

    private suspend fun previewBatchWithinTransaction(taskIds: Set<Long>): TaskDeletionBatchImpact {
        val requestedTaskIds = taskIds.filter { it > 0 }.toSet()
        val tasks = requestedTaskIds.sorted().mapNotNull { taskRepository.getTask(it) }
        val existingTaskIds = tasks.mapTo(linkedSetOf()) { it.id }
        val steps = existingTaskIds.flatMap { database.taskDao().getSteps(it) }
        val stepIds = steps.mapTo(mutableSetOf()) { it.id }
        val occurrences = existingTaskIds.flatMap { taskRepository.getOccurrences(it) }
        val linkRuleIds = database.linkDao().getRules()
            .filterTo(linkedSetOf()) { rule ->
                (rule.sourceType == LinkSourceType.Task.name && rule.sourceEntityId in existingTaskIds) ||
                    (rule.sourceType == LinkSourceType.Subtask.name && rule.sourceEntityId in stepIds)
            }
            .mapTo(linkedSetOf()) { it.id }
        val automationRuleIds = database.linkDao().getTriggerRules()
            .filterTo(linkedSetOf()) { rule ->
                (rule.sourceType == LinkSourceType.Task.name && rule.sourceEntityId in existingTaskIds) ||
                    (rule.sourceType == LinkSourceType.Subtask.name && rule.sourceEntityId in stepIds) ||
                    (rule.targetType == TriggerTargetType.Task.name && rule.targetEntityId in existingTaskIds)
            }
            .mapTo(linkedSetOf()) { it.id }
        val completedOneShotCount = tasks.count { it.recurrence == null && it.completedAtMillis != null }
        return TaskDeletionBatchImpact(
            requestedTaskIds = requestedTaskIds,
            taskIds = existingTaskIds,
            titles = tasks.map { it.title },
            recurringSeriesCount = tasks.count { it.recurrence != null },
            recordedOccurrenceCount = occurrences.size + completedOneShotCount,
            completedOccurrenceCount = occurrences.count { it.state == OccurrenceState.Completed } + completedOneShotCount,
            skippedOccurrenceCount = occurrences.count { it.state == OccurrenceState.Skipped },
            openOccurrenceCount = occurrences.count { it.state == OccurrenceState.Open },
            stepCount = steps.count { !it.archived },
            linkRuleIds = linkRuleIds,
            automationRuleIds = automationRuleIds,
            revisionTokens = tasks.associate { it.id to it.updatedAtMillis },
        )
    }
}

data class TaskDeletionImpact(
    val taskId: Long,
    val exists: Boolean = false,
    val title: String = "",
    val recurring: Boolean = false,
    val recordedOccurrenceCount: Int = 0,
    val completedOccurrenceCount: Int = 0,
    val skippedOccurrenceCount: Int = 0,
    val openOccurrenceCount: Int = 0,
    val stepCount: Int = 0,
    val linkRuleCount: Int = 0,
    val automationRuleCount: Int = 0,
    val revisionToken: Long = 0,
)

data class TaskDeletionSummary(
    val taskDeleted: Boolean = false,
    val linkRulesDeleted: Int = 0,
    val automationRulesDeleted: Int = 0,
)

data class TaskDeletionBatchImpact(
    val requestedTaskIds: Set<Long> = emptySet(),
    val taskIds: Set<Long> = emptySet(),
    val titles: List<String> = emptyList(),
    val recurringSeriesCount: Int = 0,
    val recordedOccurrenceCount: Int = 0,
    val completedOccurrenceCount: Int = 0,
    val skippedOccurrenceCount: Int = 0,
    val openOccurrenceCount: Int = 0,
    val stepCount: Int = 0,
    internal val linkRuleIds: Set<Long> = emptySet(),
    internal val automationRuleIds: Set<Long> = emptySet(),
    val revisionTokens: Map<Long, Long> = emptyMap(),
) {
    val linkRuleCount: Int get() = linkRuleIds.size
    val automationRuleCount: Int get() = automationRuleIds.size
}

data class TaskDeletionBatchSummary(
    val tasksDeleted: Int = 0,
    val linkRulesDeleted: Int = 0,
    val automationRulesDeleted: Int = 0,
)
