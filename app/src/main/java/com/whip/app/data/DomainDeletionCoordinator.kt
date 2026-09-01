package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.reminders.ReminderDeliveryCoordinator
import com.whip.app.reminders.ReminderDomain
import java.io.Serializable
import java.security.MessageDigest
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

/**
 * Performs irreversible cross-feature deletion without leaving polymorphic links,
 * generated measurements, automation events, or gym references behind.
 */
class DomainDeletionCoordinator internal constructor(
    private val database: WhipDatabase,
    private val linkRepository: LinkRepository,
    private val routineRepository: RoutineRepository,
    private val reminderDeliveryCoordinator: ReminderDeliveryCoordinator? = null,
    private val onDeletionPrepared: (ReminderDomain, Set<Long>) -> Unit = { _, _ -> },
    private val onDeletionCommitted: (ReminderDomain, Set<Long>) -> Unit = { _, _ -> },
    private val onDeletionInterrupted: suspend () -> Unit = {},
    private val rebuildLinksAfterGoalDeletion: suspend () -> Unit = { linkRepository.rebuildAll() },
) {
    suspend fun previewMachineDeletion(machineId: Long): MachineDeletionImpact? = database.withTransaction {
        buildMachineDeletionImpact(machineId)
    }

    suspend fun deleteMachine(
        machineId: Long,
        expectedRevisionToken: String? = null,
    ): MachineDeletionResult = database.withTransaction {
        val impact = buildMachineDeletionImpact(machineId)
            ?: return@withTransaction MachineDeletionResult(deleted = false)
        require(impact.activePlacements == 0) {
            "This machine is used by the active workout. Finish it or change that exercise's equipment first."
        }
        if (expectedRevisionToken != null) {
            require(impact.revisionToken == expectedRevisionToken) {
                "Machine usage changed while the confirmation was open. Review the updated impact before deleting."
            }
        }
        val historicalReferencesCleared = database.gymDao().clearWorkoutMachineReferences(machineId)
        val routineBindingsMarked = database.routineDao().markMachineBindingsNeedsEquipment(
            machineId,
            System.currentTimeMillis(),
        )
        val recordReferencesCleared = database.routineDao().clearPersonalRecordMachineReferences(machineId)
        check(database.gymDao().deleteMachine(machineId) == 1) { "Machine no longer exists" }
        MachineDeletionResult(
            deleted = true,
            historicalReferencesCleared = historicalReferencesCleared,
            routineBindingsMarkedNeedsEquipment = routineBindingsMarked,
            personalRecordReferencesCleared = recordReferencesCleared,
            preservedWorkoutPlacements = impact.historicalPlacements,
            preservedSets = impact.setCount,
        )
    }

    suspend fun deleteHabit(habitId: Long): DomainDeletionSummary =
        withDurableReminderDeletion(ReminderDomain.Habit, habitId) {
            val summary = database.withTransaction { deleteHabitWithinTransaction(habitId) }
            linkRepository.rebuildAll()
            summary
        }

    /** Room cleanup only. The caller owns the surrounding transaction and every post-commit action. */
    internal suspend fun deleteHabitWithinTransaction(habitId: Long): DomainDeletionSummary {
        val habit = database.habitDao().getHabit(habitId) ?: return DomainDeletionSummary()
        val links = database.linkDao().getRules().filter { rule ->
            (rule.sourceType == LinkSourceType.Habit.name && rule.sourceEntityId == habitId) ||
                rule.sourceMetricId == habit.metricId
        }
        links.forEach { linkRepository.deleteRule(it.id) }
        val triggers = database.linkDao().getTriggerRules().filter { rule ->
            (rule.targetType == TriggerTargetType.Habit.name && rule.targetEntityId == habitId) ||
                (rule.sourceType == LinkSourceType.Habit.name && rule.sourceEntityId == habitId)
        }
        triggers.forEach { linkRepository.deleteTrigger(it.id) }
        check(database.habitDao().deleteHabit(habitId) == 1) { "Habit no longer exists" }
        database.measurementDao().deleteMetric(habit.metricId)
        return DomainDeletionSummary(true, links.size, triggers.size)
    }

    suspend fun previewGoalDeletion(goalId: Long): GoalDeletionImpact = database.withTransaction {
        buildGoalDeletionImpact(goalId)
    }

    suspend fun deleteGoal(
        goalId: Long,
        expectedRevisionToken: String? = null,
    ): GoalDeletionSummary {
        val requested = setOf(goalId)
        val cleanupWarnings = mutableListOf<String>()
        var committedSummary: GoalDeletionSummary? = null
        try {
            withReminderStateBoundary {
                onDeletionPrepared(ReminderDomain.Goal, requested)
                val summary = database.withTransaction {
                    deleteGoalWithinTransaction(goalId, expectedRevisionToken)
                }
                committedSummary = summary
                try {
                    rebuildLinksAfterCommittedDeletion()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    cleanupWarnings +=
                        "Link reconciliation did not finish; the permanent deletion was committed and will be reconciled later."
                }
                try {
                    onDeletionCommitted(ReminderDomain.Goal, requested)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    cleanupWarnings +=
                        "Reminder cleanup did not finish; the permanent deletion was committed and will be reconciled later."
                }
            }
        } catch (cancelled: CancellationException) {
            notifyDeletionInterrupted(cancelled)
            val committed = committedSummary
            if (committed != null) {
                throw CommittedGoalDeletionCancellation(
                    committed.copy(warnings = committed.warnings + cleanupWarnings),
                    cancelled,
                )
            }
            throw cancelled
        } catch (error: Throwable) {
            notifyDeletionInterrupted(error)
            throw error
        }

        val committed = requireNotNull(committedSummary).copy(warnings = cleanupWarnings)
        if (cleanupWarnings.isNotEmpty()) {
            try {
                onDeletionInterrupted()
            } catch (cancelled: CancellationException) {
                throw CommittedGoalDeletionCancellation(committed, cancelled)
            } catch (fatal: Error) {
                throw fatal
            } catch (_: Exception) {
                // The deletion and its durable cleanup marker are already committed.
                // Startup reconciliation will retry without inviting a destructive replay.
            }
        }
        return committed
    }

    /** Room cleanup only. The caller owns the surrounding transaction and every post-commit action. */
    internal suspend fun deleteGoalWithinTransaction(
        goalId: Long,
        expectedRevisionToken: String? = null,
    ): GoalDeletionSummary {
        val impact = buildGoalDeletionImpact(goalId)
        require(impact.exists) { "Goal no longer exists" }
        if (expectedRevisionToken != null) {
            require(impact.revisionToken == expectedRevisionToken) {
                "The Goal or its history changed while the confirmation was open. Review the updated impact before deleting."
            }
        }
        impact.linkRuleIds.forEach { linkRepository.deleteRule(it) }
        check(database.goalDao().deleteGoal(goalId) == 1) { "Goal no longer exists" }
        check(database.measurementDao().deleteMetric(impact.metricId) == 1) {
            "Goal progress history changed before it could be deleted"
        }
        return GoalDeletionSummary(
            goalDeleted = true,
            linkRulesDeleted = impact.linkRuleCount,
            milestonesDeleted = impact.milestoneCount,
            progressEntriesDeleted = impact.progressEntryCount,
            legacyClosureSnapshotsDeleted = impact.legacyClosureSnapshotCount,
            elapsedResetEventsDeleted = impact.elapsedResetEventCount,
            contributionsDeleted = impact.contributionCount,
        )
    }

    suspend fun deleteTrack(trackId: Long): DomainDeletionSummary {
        val summary = database.withTransaction { deleteTrackWithinTransaction(trackId) }
        linkRepository.rebuildAll()
        return summary
    }

    /** Room cleanup only. The caller owns the surrounding transaction and every post-commit action. */
    internal suspend fun deleteTrackWithinTransaction(trackId: Long): DomainDeletionSummary {
        database.trackDao().getTrack(trackId) ?: return DomainDeletionSummary()
        val links = database.linkDao().getRules().filter {
            it.sourceType == LinkSourceType.Track.name && it.sourceEntityId == trackId
        }
        links.forEach { linkRepository.deleteRule(it.id) }
        val triggers = database.linkDao().getTriggerRules().filter {
            (it.sourceType == LinkSourceType.Track.name && it.sourceEntityId == trackId) ||
                (it.targetType == TriggerTargetType.Track.name && it.targetEntityId == trackId)
        }
        triggers.forEach { linkRepository.deleteTrigger(it.id) }
        database.trackDao().deleteSearchForTrack(trackId)
        check(database.trackDao().deleteTrack(trackId) == 1) { "Track no longer exists" }
        return DomainDeletionSummary(true, links.size, triggers.size)
    }

    internal suspend fun rebuildLinksAfterCommittedDeletion() = rebuildLinksAfterGoalDeletion()

    suspend fun deleteExercise(exerciseId: Long): DomainDeletionSummary {
        val affectedExerciseIds = mutableSetOf<Long>()
        val summary = database.withTransaction {
            database.gymDao().getExercise(exerciseId) ?: return@withTransaction DomainDeletionSummary()
            affectedExerciseIds += exerciseId
            val links = database.linkDao().getRules().filter {
                it.sourceType == LinkSourceType.Exercise.name && it.sourceEntityId == exerciseId
            }
            links.forEach { linkRepository.deleteRule(it.id) }
            val triggers = database.linkDao().getTriggerRules().filter {
                it.sourceType == LinkSourceType.Exercise.name && it.sourceEntityId == exerciseId
            }
            triggers.forEach { linkRepository.deleteTrigger(it.id) }

            val routinePlacements = database.routineDao().deleteExercisesForExercise(exerciseId)
            val workoutPlacements = database.gymDao().deleteWorkoutExercisesForExercise(exerciseId)
            cleanGraphPresets(exerciseId)
            check(database.gymDao().deleteExercise(exerciseId) == 1) { "Exercise no longer exists" }
            DomainDeletionSummary(
                deleted = true,
                linkRulesDeleted = links.size,
                automationRulesDeleted = triggers.size,
                workoutReferencesDeleted = workoutPlacements,
                routineReferencesDeleted = routinePlacements,
            )
        }
        affectedExerciseIds.forEach { routineRepository.rebuildPersonalRecords(it) }
        linkRepository.rebuildAll()
        return summary
    }

    suspend fun deleteRoutine(routineId: Long): DomainDeletionSummary = database.withTransaction {
        database.routineDao().getRoutine(routineId) ?: return@withTransaction DomainDeletionSummary()
        val historyReferences = database.gymDao().clearSourceRoutine(routineId)
        check(database.routineDao().deleteRoutine(routineId) == 1) { "Routine no longer exists" }
        DomainDeletionSummary(deleted = true, preservedHistoryReferences = historyReferences)
    }

    suspend fun deleteWorkout(sessionId: Long): DomainDeletionSummary {
        val exerciseIds = database.gymDao().getWorkoutExercises(sessionId).map { it.exerciseId }.distinct()
        val summary = database.withTransaction {
            database.gymDao().getSession(sessionId) ?: return@withTransaction DomainDeletionSummary()
            check(database.gymDao().permanentlyDeleteSession(sessionId) == 1) { "Workout no longer exists" }
            DomainDeletionSummary(deleted = true, workoutReferencesDeleted = exerciseIds.size)
        }
        exerciseIds.forEach { routineRepository.rebuildPersonalRecords(it) }
        linkRepository.rebuildAll()
        return summary
    }

    private suspend fun cleanGraphPresets(exerciseId: Long) {
        database.routineDao().getGraphPresets().forEach { preset ->
            val retained = preset.exerciseIdsCsv.split(',')
                .mapNotNull(String::toLongOrNull)
                .filterNot { it == exerciseId }
            if (retained.isEmpty()) {
                database.routineDao().deleteGraphPreset(preset.id)
            } else if (retained.joinToString(",") != preset.exerciseIdsCsv) {
                database.routineDao().updateGraphPreset(preset.copy(exerciseIdsCsv = retained.joinToString(",")))
            }
        }
    }

    private suspend fun <T> withReminderStateBoundary(block: suspend () -> T): T =
        reminderDeliveryCoordinator?.withStateBoundary(block) ?: block()

    private suspend fun <T> withDurableReminderDeletion(
        domain: ReminderDomain,
        entityId: Long,
        block: suspend () -> T,
    ): T {
        val ids = setOf(entityId)
        return try {
            withReminderStateBoundary {
                onDeletionPrepared(domain, ids)
                block().also { onDeletionCommitted(domain, ids) }
            }
        } catch (error: Throwable) {
            onDeletionInterrupted()
            throw error
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

    private suspend fun buildGoalDeletionImpact(goalId: Long): GoalDeletionImpact {
        val goal = database.goalDao().getGoal(goalId)
            ?: return GoalDeletionImpact(goalId = goalId)
        val metric = database.measurementDao().getMetric(goal.metricId)
        val milestones = database.goalDao().getMilestones(goalId)
        val progressEntries = database.measurementDao().getEntriesForMetric(goal.metricId)
        val links = database.linkDao().getRules().filter { rule ->
            rule.targetGoalId == goalId || rule.sourceMetricId == goal.metricId
        }
        val linkRuleIds = links.mapTo(linkedSetOf(), LinkRuleEntity::id)
        val linkConditions = links.flatMap { database.linkDao().getRuleConditions(it.id) }
        val linkConditionIds = linkConditions.map(LinkRuleConditionEntity::id)
        val linkConditionChoices = linkConditionIds.takeIf { it.isNotEmpty() }
            ?.let { database.linkDao().getLinkConditionChoices(it) }
            .orEmpty()
        val contributions = database.linkDao().observeContributionsSnapshot().filter { contribution ->
            contribution.linkRuleId in linkRuleIds || contribution.targetGoalId == goalId
        }
        val legacyClosureSnapshots = database.goalDao().getClosureSnapshots(goalId)
        val elapsedResetEvents = database.goalDao().getElapsedResetEvents(goalId)
        return GoalDeletionImpact(
            goalId = goalId,
            exists = true,
            name = goal.name,
            status = goal.status,
            archived = goal.archived,
            milestoneCount = milestones.size,
            completedMilestoneCount = milestones.count(GoalMilestoneEntity::completed),
            progressEntryCount = progressEntries.size,
            legacyClosureSnapshotCount = legacyClosureSnapshots.size,
            elapsedResetEventCount = elapsedResetEvents.size,
            linkRuleCount = links.size,
            contributionCount = contributions.size,
            revisionToken = goalDeletionRevision(
                goal = goal,
                metric = metric,
                milestones = milestones,
                progressEntries = progressEntries,
                legacyClosureSnapshots = legacyClosureSnapshots,
                elapsedResetEvents = elapsedResetEvents,
                links = links,
                linkConditions = linkConditions,
                linkConditionChoices = linkConditionChoices,
                contributions = contributions,
            ),
            metricId = goal.metricId,
            linkRuleIds = linkRuleIds,
        )
    }

    private suspend fun buildMachineDeletionImpact(machineId: Long): MachineDeletionImpact? {
        val machine = database.gymDao().getMachine(machineId) ?: return null
        val placements = database.gymDao().getWorkoutExercisesForMachineScope(machine.uuid)
        val placementIds = placements.mapTo(mutableSetOf()) { it.id }
        val sessions = database.gymDao().getAllSessions().associateBy { it.id }
        val completedSessions = placements.mapNotNull { placement ->
            sessions[placement.sessionId]?.takeIf { it.state == WorkoutSessionState.Finished.name }
        }.distinctBy { it.id }
        val sets = database.gymDao().getAllWorkoutSets().filter {
            it.workoutExerciseId in placementIds && it.deletedAtMillis == null
        }
        val routinePlacements = database.routineDao().getAllExercises().filter { it.machineId == machineId }
        val routineDayById = routinePlacements.mapNotNull { placement ->
            database.routineDao().getDay(placement.routineDayId)
        }.associateBy { it.id }
        val routineNames = routineDayById.values.mapNotNull { day ->
            database.routineDao().getRoutine(day.routineId)?.name
        }.distinct().sorted()
        val personalRecordCount = database.routineDao().getAllPersonalRecords().count {
            it.machineProfileUuidSnapshot == machine.uuid && it.current
        }
        val siblingVersions = database.gymDao().getAllMachines().count {
            it.id != machine.id && it.configurationGroupId == machine.configurationGroupId
        }
        val firstDate = completedSessions.minOfOrNull { LocalDate.ofEpochDay(it.localEpochDay) }
        val lastDate = completedSessions.maxOfOrNull { LocalDate.ofEpochDay(it.localEpochDay) }
        val activePlacements = database.gymDao().activeWorkoutMachinePlacementCount(machineId)
        val revisionToken = listOf(
            machine.uuid,
            machine.updatedAtMillis,
            machine.hashCode(),
            placements.size,
            sets.size,
            routinePlacements.size,
            activePlacements,
            personalRecordCount,
        ).joinToString(":")
        return MachineDeletionImpact(
            machineId = machine.id,
            machineUuid = machine.uuid,
            displayName = machine.displayName(),
            configurationVersion = machine.configurationVersion,
            historicalPlacements = placements.size,
            completedSessions = completedSessions.size,
            setCount = sets.size,
            firstWorkoutDate = firstDate,
            lastWorkoutDate = lastDate,
            activePlacements = activePlacements,
            routineReferences = routinePlacements.size,
            routineNames = routineNames,
            currentPersonalRecords = personalRecordCount,
            siblingVersions = siblingVersions,
            revisionToken = revisionToken,
        )
    }
}

private fun goalDeletionRevision(
    goal: Any,
    metric: Any?,
    milestones: List<*>,
    progressEntries: List<*>,
    legacyClosureSnapshots: List<*>,
    elapsedResetEvents: List<*>,
    links: List<*>,
    linkConditions: List<*>,
    linkConditionChoices: List<*>,
    contributions: List<*>,
): String {
    val canonical = buildString {
        append("goal|").append(goal).append('\n')
        append("metric|").append(metric).append('\n')
        listOf(
            "milestone" to milestones,
            "progress-entry" to progressEntries,
            "legacy-closure" to legacyClosureSnapshots,
            "elapsed-reset" to elapsedResetEvents,
            "link" to links,
            "link-condition" to linkConditions,
            "link-condition-choice" to linkConditionChoices,
            "contribution" to contributions,
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

data class GoalDeletionImpact(
    val goalId: Long,
    val exists: Boolean = false,
    val name: String = "",
    val status: String = "",
    val archived: Boolean = false,
    val milestoneCount: Int = 0,
    val completedMilestoneCount: Int = 0,
    val progressEntryCount: Int = 0,
    val legacyClosureSnapshotCount: Int = 0,
    val elapsedResetEventCount: Int = 0,
    val linkRuleCount: Int = 0,
    val contributionCount: Int = 0,
    val revisionToken: String = "",
    internal val metricId: String = "",
    internal val linkRuleIds: Set<Long> = emptySet(),
) : Serializable

data class GoalDeletionSummary(
    val goalDeleted: Boolean = false,
    val linkRulesDeleted: Int = 0,
    val milestonesDeleted: Int = 0,
    val progressEntriesDeleted: Int = 0,
    val legacyClosureSnapshotsDeleted: Int = 0,
    val elapsedResetEventsDeleted: Int = 0,
    val contributionsDeleted: Int = 0,
    val warnings: List<String> = emptyList(),
)

class CommittedGoalDeletionCancellation(
    val summary: GoalDeletionSummary,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init { initCause(cause) }
}

data class MachineDeletionImpact(
    val machineId: Long,
    val machineUuid: String,
    val displayName: String,
    val configurationVersion: Int,
    val historicalPlacements: Int,
    val completedSessions: Int,
    val setCount: Int,
    val firstWorkoutDate: LocalDate?,
    val lastWorkoutDate: LocalDate?,
    val activePlacements: Int,
    val routineReferences: Int,
    val routineNames: List<String>,
    val currentPersonalRecords: Int,
    val siblingVersions: Int,
    val revisionToken: String,
)

data class MachineDeletionResult(
    val deleted: Boolean,
    val historicalReferencesCleared: Int = 0,
    val routineBindingsMarkedNeedsEquipment: Int = 0,
    val personalRecordReferencesCleared: Int = 0,
    val preservedWorkoutPlacements: Int = 0,
    val preservedSets: Int = 0,
)

data class DomainDeletionSummary(
    val deleted: Boolean = false,
    val linkRulesDeleted: Int = 0,
    val automationRulesDeleted: Int = 0,
    val workoutReferencesDeleted: Int = 0,
    val routineReferencesDeleted: Int = 0,
    val preservedHistoryReferences: Int = 0,
)
