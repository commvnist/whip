package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.OccurrenceState
import com.whip.app.reminders.ReminderDeliveryCoordinator
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException

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
        expectedRevisionToken: String? = null,
    ): TaskDeletionSummary {
        val result = try {
            delete(
                taskIds = setOf(taskId),
                expectedRevisionTokens = expectedRevisionToken?.let { mapOf(taskId to it) },
            )
        } catch (cancelled: CommittedTaskDeletionBatchCancellation) {
            val committed = cancelled.summary.toSingleSummary()
            throw CommittedTaskDeletionCancellation(committed, cancelled)
        }
        return result.toSingleSummary()
    }

    private fun TaskDeletionBatchSummary.toSingleSummary() = TaskDeletionSummary(
        taskDeleted = tasksDeleted == 1,
        linkRulesDeleted = linkRulesDeleted,
        automationRulesDeleted = automationRulesDeleted,
        warnings = warnings,
    )

    suspend fun delete(
        taskIds: Set<Long>,
        expectedRevisionTokens: Map<Long, String>? = null,
    ): TaskDeletionBatchSummary {
        val requestedTaskIds = taskIds.filterTo(linkedSetOf()) { it > 0 }
        val result = try {
            withReminderStateBoundary {
                onDeletionPrepared(requestedTaskIds)
                val summary = database.withTransaction {
                    val impact = previewBatchWithinTransaction(taskIds)
                    require(
                        expectedRevisionTokens == null ||
                            (impact.taskIds == impact.requestedTaskIds &&
                                expectedRevisionTokens == impact.revisionTokens),
                    ) {
                        "One or more tasks changed after the deletion preview. Review the updated impact before deleting."
                    }
                    if (impact.taskIds.isEmpty()) return@withTransaction TaskDeletionBatchSummary()
                    deleteImpactWithinTransaction(impact)
                }
                val cleanupFailure = try {
                    ordinaryCleanupFailure(requestedTaskIds)
                } catch (cancelled: CancellationException) {
                    throw CommittedTaskDeletionBatchCancellation(summary, cancelled)
                }
                summary to cleanupFailure
            }
        } catch (error: Throwable) {
            notifyDeletionInterrupted(error)
            throw error
        }
        val cleanupFailure = result.second
        if (cleanupFailure != null) {
            try {
                suppressOrdinaryInterruptionFailure()
            } catch (cancelled: CancellationException) {
                throw CommittedTaskDeletionBatchCancellation(result.first, cancelled)
            }
        }
        return result.first.copy(
            warnings = result.first.warnings + listOfNotNull(
                cleanupFailure?.let {
                    "Reminder cleanup did not finish; the permanent deletion was committed and will be reconciled later."
                },
            ),
        )
    }

    suspend fun undoPromotion(
        promotedTaskId: Long,
        expectedRevisionToken: String,
        sourceTaskId: Long,
        sourceStepId: Long,
        expectedSourceStepUpdatedAtMillis: Long,
    ): TaskDeletionSummary {
        val requested = setOf(promotedTaskId)
        val result = try {
            withReminderStateBoundary {
                onDeletionPrepared(requested)
                val summary = database.withTransaction {
                    val impact = previewBatchWithinTransaction(requested)
                    require(
                        impact.taskIds == requested &&
                            impact.revisionTokens[promotedTaskId] == expectedRevisionToken,
                    ) { "The promoted Task changed and cannot be safely removed" }
                    val source = database.taskDao().getSteps(sourceTaskId)
                        .firstOrNull { it.id == sourceStepId }
                        ?: error("The source Subtask no longer exists")
                    require(source.archived && source.updatedAtMillis == expectedSourceStepUpdatedAtMillis) {
                        "The source Subtask changed and cannot be safely restored"
                    }
                    val deleted = deleteImpactWithinTransaction(impact)
                    database.taskDao().updateStep(source.copy(archived = false))
                    deleted
                }
                val committed = TaskDeletionSummary(
                    taskDeleted = summary.tasksDeleted == 1,
                    linkRulesDeleted = summary.linkRulesDeleted,
                    automationRulesDeleted = summary.automationRulesDeleted,
                )
                val cleanupFailure = try {
                    ordinaryCleanupFailure(requested)
                } catch (cancelled: CancellationException) {
                    throw CommittedTaskDeletionCancellation(committed, cancelled)
                }
                committed to cleanupFailure
            }
        } catch (error: Throwable) {
            notifyDeletionInterrupted(error)
            throw error
        }
        val cleanupFailure = result.second
        if (cleanupFailure != null) {
            try {
                suppressOrdinaryInterruptionFailure()
            } catch (cancelled: CancellationException) {
                throw CommittedTaskDeletionCancellation(result.first, cancelled)
            }
        }
        return result.first.copy(
            warnings = result.first.warnings + listOfNotNull(
                cleanupFailure?.let {
                    "Reminder cleanup did not finish; the promotion undo was committed and will be reconciled later."
                },
            ),
        )
    }

    private suspend fun deleteImpactWithinTransaction(
        impact: TaskDeletionBatchImpact,
    ): TaskDeletionBatchSummary {
        impact.linkRuleIds.forEach { linkRepository.deleteRule(it) }
        impact.automationRuleIds.forEach { linkRepository.deleteTrigger(it) }
        impact.taskIds.sorted().forEach { taskId ->
            check(taskRepository.deletePermanently(taskId)) { "Task no longer exists" }
        }
        return TaskDeletionBatchSummary(
            tasksDeleted = impact.taskIds.size,
            linkRulesDeleted = impact.linkRuleIds.size,
            automationRulesDeleted = impact.automationRuleIds.size,
        )
    }

    private suspend fun <T> withReminderStateBoundary(block: suspend () -> T): T =
        reminderDeliveryCoordinator?.withStateBoundary(block) ?: block()

    private fun ordinaryCleanupFailure(taskIds: Set<Long>): Exception? = try {
        onDeletionCommitted(taskIds)
        null
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        failure
    }

    private suspend fun suppressOrdinaryInterruptionFailure() {
        try {
            onDeletionInterrupted()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The authoritative deletion is already committed; later startup reconciliation retries cleanup.
        }
    }

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

    private suspend fun previewWithinTransaction(taskId: Long): TaskDeletionImpact {
        val task = taskRepository.getTask(taskId) ?: return TaskDeletionImpact(taskId = taskId)
        val occurrences = taskRepository.getOccurrences(taskId)
        val steps = database.taskDao().getSteps(taskId)
        val stepIds = steps.mapTo(mutableSetOf()) { it.id }
        val stepStates = database.taskDao().getAllStepStates().filter { it.taskId == taskId }
        val stepSnapshots = database.taskDao().getAllStepSnapshots().filter { it.taskId == taskId }
        val linkRules = database.linkDao().getRules().filter { rule ->
            (rule.sourceType == LinkSourceType.Task.name && rule.sourceEntityId == taskId) ||
                (rule.sourceType == LinkSourceType.Subtask.name &&
                    rule.sourceEntityId == taskId && rule.sourceItemId in stepIds)
        }
        val linkRuleConditions = linkRules.flatMap { database.linkDao().getRuleConditions(it.id) }
        val linkConditionChoices = linkRuleConditions.map { it.id }.takeIf { it.isNotEmpty() }
            ?.let { database.linkDao().getLinkConditionChoices(it) }
            .orEmpty()
        val automations = database.linkDao().getTriggerRules().filter { rule ->
            (rule.sourceType == LinkSourceType.Task.name && rule.sourceEntityId == taskId) ||
                (rule.sourceType == LinkSourceType.Subtask.name &&
                    rule.sourceEntityId == taskId && rule.sourceItemId in stepIds) ||
                (rule.targetType == TriggerTargetType.Task.name && rule.targetEntityId == taskId)
        }
        val triggerConditions = automations.flatMap { database.linkDao().getTriggerConditions(it.id) }
        val triggerConditionChoices = triggerConditions.map { it.id }.takeIf { it.isNotEmpty() }
            ?.let { database.linkDao().getTriggerConditionChoices(it) }
            .orEmpty()
        val triggerMappings = automations.flatMap { database.linkDao().getTriggerMappings(it.id) }
        val triggerOccurrences = automations.flatMap { database.linkDao().getTriggerOccurrences(it.id) }
        val linkedTrackEntries = triggerOccurrences.map { it.id }.takeIf { it.isNotEmpty() }
            ?.let { database.trackDao().getEntriesForSourceOccurrences(it) }
            .orEmpty()
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
            linkRuleCount = linkRules.size,
            automationRuleCount = automations.size,
            revisionToken = taskDeletionRevision(
                task = task,
                occurrences = occurrences,
                steps = steps,
                stepStates = stepStates,
                stepSnapshots = stepSnapshots,
                linkRules = linkRules,
                linkRuleConditions = linkRuleConditions,
                linkConditionChoices = linkConditionChoices,
                automations = automations,
                triggerConditions = triggerConditions,
                triggerConditionChoices = triggerConditionChoices,
                triggerMappings = triggerMappings,
                triggerOccurrences = triggerOccurrences,
                linkedTrackEntries = linkedTrackEntries,
            ),
        )
    }

    private suspend fun previewBatchWithinTransaction(taskIds: Set<Long>): TaskDeletionBatchImpact {
        val requestedTaskIds = taskIds.filter { it > 0 }.toSet()
        val tasks = requestedTaskIds.sorted().mapNotNull { taskRepository.getTask(it) }
        val existingTaskIds = tasks.mapTo(linkedSetOf()) { it.id }
        val steps = existingTaskIds.flatMap { database.taskDao().getSteps(it) }
        val stepIds = steps.mapTo(mutableSetOf()) { it.id }
        val occurrences = existingTaskIds.flatMap { taskRepository.getOccurrences(it) }
        val stepStates = database.taskDao().getAllStepStates().filter { it.taskId in existingTaskIds }
        val stepSnapshots = database.taskDao().getAllStepSnapshots().filter { it.taskId in existingTaskIds }
        val relevantLinkRules = database.linkDao().getRules()
            .filter { rule ->
                (rule.sourceType == LinkSourceType.Task.name && rule.sourceEntityId in existingTaskIds) ||
                    (rule.sourceType == LinkSourceType.Subtask.name &&
                        rule.sourceEntityId in existingTaskIds && rule.sourceItemId in stepIds)
            }
        val linkRuleIds = relevantLinkRules.mapTo(linkedSetOf()) { it.id }
        val relevantLinkRuleConditions = relevantLinkRules.flatMap {
            database.linkDao().getRuleConditions(it.id)
        }
        val relevantLinkConditionChoices = relevantLinkRuleConditions.map { it.id }
            .takeIf { it.isNotEmpty() }
            ?.let { database.linkDao().getLinkConditionChoices(it) }
            .orEmpty()
        val relevantAutomations = database.linkDao().getTriggerRules()
            .filter { rule ->
                (rule.sourceType == LinkSourceType.Task.name && rule.sourceEntityId in existingTaskIds) ||
                    (rule.sourceType == LinkSourceType.Subtask.name &&
                        rule.sourceEntityId in existingTaskIds && rule.sourceItemId in stepIds) ||
                    (rule.targetType == TriggerTargetType.Task.name && rule.targetEntityId in existingTaskIds)
            }
        val automationRuleIds = relevantAutomations.mapTo(linkedSetOf()) { it.id }
        val relevantTriggerConditions = relevantAutomations.flatMap {
            database.linkDao().getTriggerConditions(it.id)
        }
        val relevantTriggerConditionChoices = relevantTriggerConditions.map { it.id }
            .takeIf { it.isNotEmpty() }
            ?.let { database.linkDao().getTriggerConditionChoices(it) }
            .orEmpty()
        val relevantTriggerMappings = relevantAutomations.flatMap {
            database.linkDao().getTriggerMappings(it.id)
        }
        val triggerOccurrences = relevantAutomations.flatMap { database.linkDao().getTriggerOccurrences(it.id) }
        val linkedTrackEntries = triggerOccurrences.map { it.id }.takeIf { it.isNotEmpty() }
            ?.let { database.trackDao().getEntriesForSourceOccurrences(it) }
            .orEmpty()
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
            revisionTokens = tasks.associate { task ->
                val taskStepIds = steps.filter { it.taskId == task.id }.mapTo(mutableSetOf()) { it.id }
                val taskAutomations = relevantAutomations.filter { rule ->
                    (rule.sourceType == LinkSourceType.Task.name && rule.sourceEntityId == task.id) ||
                        (rule.sourceType == LinkSourceType.Subtask.name &&
                            rule.sourceEntityId == task.id && rule.sourceItemId in taskStepIds) ||
                        (rule.targetType == TriggerTargetType.Task.name && rule.targetEntityId == task.id)
                }
                val taskAutomationIds = taskAutomations.mapTo(mutableSetOf()) { it.id }
                val taskLinkRuleIds = relevantLinkRules.filter { rule ->
                    (rule.sourceType == LinkSourceType.Task.name && rule.sourceEntityId == task.id) ||
                        (rule.sourceType == LinkSourceType.Subtask.name &&
                            rule.sourceEntityId == task.id && rule.sourceItemId in taskStepIds)
                }.mapTo(mutableSetOf()) { it.id }
                val taskLinkConditionIds = relevantLinkRuleConditions
                    .filter { it.linkRuleId in taskLinkRuleIds }
                    .mapTo(mutableSetOf()) { it.id }
                val taskTriggerConditionIds = relevantTriggerConditions
                    .filter { it.triggerRuleId in taskAutomationIds }
                    .mapTo(mutableSetOf()) { it.id }
                val taskTriggerOccurrences = triggerOccurrences.filter { it.triggerRuleId in taskAutomationIds }
                val taskTriggerOccurrenceIds = taskTriggerOccurrences.mapTo(mutableSetOf()) { it.id }
                task.id to taskDeletionRevision(
                    task = task,
                    occurrences = occurrences.filter { it.taskId == task.id },
                    steps = steps.filter { it.taskId == task.id },
                    stepStates = stepStates.filter { it.taskId == task.id },
                    stepSnapshots = stepSnapshots.filter { it.taskId == task.id },
                    linkRules = relevantLinkRules.filter { it.id in taskLinkRuleIds },
                    linkRuleConditions = relevantLinkRuleConditions.filter { it.linkRuleId in taskLinkRuleIds },
                    linkConditionChoices = relevantLinkConditionChoices.filter { it.conditionId in taskLinkConditionIds },
                    automations = taskAutomations,
                    triggerConditions = relevantTriggerConditions.filter { it.triggerRuleId in taskAutomationIds },
                    triggerConditionChoices = relevantTriggerConditionChoices.filter {
                        it.conditionId in taskTriggerConditionIds
                    },
                    triggerMappings = relevantTriggerMappings.filter { it.triggerRuleId in taskAutomationIds },
                    triggerOccurrences = taskTriggerOccurrences,
                    linkedTrackEntries = linkedTrackEntries.filter { it.sourceOccurrenceId in taskTriggerOccurrenceIds },
                )
            },
        )
    }
}

private fun taskDeletionRevision(
    task: Any,
    occurrences: List<*>,
    steps: List<*>,
    stepStates: List<*>,
    stepSnapshots: List<*>,
    linkRules: List<*>,
    linkRuleConditions: List<*>,
    linkConditionChoices: List<*>,
    automations: List<*>,
    triggerConditions: List<*>,
    triggerConditionChoices: List<*>,
    triggerMappings: List<*>,
    triggerOccurrences: List<*>,
    linkedTrackEntries: List<*>,
): String {
    val canonical = buildString {
        append("task|").append(task).append('\n')
        listOf(
            "occurrence" to occurrences,
            "step" to steps,
            "step-state" to stepStates,
            "step-snapshot" to stepSnapshots,
            "link" to linkRules,
            "link-condition" to linkRuleConditions,
            "link-condition-choice" to linkConditionChoices,
            "automation" to automations,
            "trigger-condition" to triggerConditions,
            "trigger-condition-choice" to triggerConditionChoices,
            "trigger-mapping" to triggerMappings,
            "trigger-occurrence" to triggerOccurrences,
            "linked-track-entry" to linkedTrackEntries,
        ).forEach { (label, rows) ->
            rows.map(Any?::toString).sorted().forEach { row ->
                append(label).append('|').append(row).append('\n')
            }
        }
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
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
    val revisionToken: String = "",
)

data class TaskDeletionSummary(
    val taskDeleted: Boolean = false,
    val linkRulesDeleted: Int = 0,
    val automationRulesDeleted: Int = 0,
    val warnings: List<String> = emptyList(),
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
    val revisionTokens: Map<Long, String> = emptyMap(),
) {
    val linkRuleCount: Int get() = linkRuleIds.size
    val automationRuleCount: Int get() = automationRuleIds.size
}

data class TaskDeletionBatchSummary(
    val tasksDeleted: Int = 0,
    val linkRulesDeleted: Int = 0,
    val automationRulesDeleted: Int = 0,
    val warnings: List<String> = emptyList(),
)

class CommittedTaskDeletionCancellation(
    val summary: TaskDeletionSummary,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init { initCause(cause) }
}

class CommittedTaskDeletionBatchCancellation(
    val summary: TaskDeletionBatchSummary,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init { initCause(cause) }
}
