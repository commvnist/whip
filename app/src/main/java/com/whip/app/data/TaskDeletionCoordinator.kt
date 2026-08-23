package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.OccurrenceState

/**
 * Owns the cross-feature cleanup required when a task is permanently deleted.
 *
 * Direct task children use foreign-key cascades. Links and automations are intentionally
 * polymorphic, so they must be removed explicitly to avoid orphan rules and derived data.
 */
class TaskDeletionCoordinator(
    private val database: WhipDatabase,
    private val taskRepository: TaskRepository,
    private val linkRepository: LinkRepository,
) {
    suspend fun preview(taskId: Long): TaskDeletionImpact = database.withTransaction {
        previewWithinTransaction(taskId)
    }

    suspend fun delete(
        taskId: Long,
        expectedRevisionToken: Long? = null,
    ): TaskDeletionSummary = database.withTransaction {
        val impact = previewWithinTransaction(taskId)
        if (!impact.exists) return@withTransaction TaskDeletionSummary()
        require(expectedRevisionToken == null || expectedRevisionToken == impact.revisionToken) {
            "Task changed after the deletion preview. Review the updated impact before deleting."
        }

        val stepIds = database.taskDao().getSteps(taskId).mapTo(mutableSetOf()) { it.id }

        val linkRuleIds = database.linkDao().getRules()
            .filter { rule ->
                (rule.sourceType == LinkSourceType.Task.name && rule.sourceEntityId == taskId) ||
                    (rule.sourceType == LinkSourceType.Subtask.name && rule.sourceEntityId in stepIds)
            }
            .map { it.id }
        linkRuleIds.forEach { linkRepository.deleteRule(it) }

        val triggerRuleIds = database.linkDao().getTriggerRules()
            .filter { rule ->
                (rule.sourceType == LinkSourceType.Task.name && rule.sourceEntityId == taskId) ||
                    (rule.sourceType == LinkSourceType.Subtask.name && rule.sourceEntityId in stepIds) ||
                    (rule.targetEntityId == taskId && rule.targetType == TriggerTargetType.Task.name)
            }
            .map { it.id }
        triggerRuleIds.forEach { linkRepository.deleteTrigger(it) }

        check(taskRepository.deletePermanently(taskId)) { "Task no longer exists" }
        TaskDeletionSummary(
            taskDeleted = true,
            linkRulesDeleted = linkRuleIds.size,
            automationRulesDeleted = triggerRuleIds.size,
        )
    }

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

    private companion object {
        val TASK_SOURCE_TYPES = setOf(LinkSourceType.Task.name, LinkSourceType.Subtask.name)
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
