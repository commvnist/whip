package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.reminders.ReminderDeliveryCoordinator
import com.whip.app.reminders.ReminderDomain
import java.time.LocalDate

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
            val summary = database.withTransaction {
                val habit = database.habitDao().getHabit(habitId)
                    ?: return@withTransaction DomainDeletionSummary()
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
                DomainDeletionSummary(true, links.size, triggers.size)
            }
            linkRepository.rebuildAll()
            summary
        }

    suspend fun deleteGoal(goalId: Long): DomainDeletionSummary =
        withDurableReminderDeletion(ReminderDomain.Goal, goalId) {
            val summary = database.withTransaction {
                val goal = database.goalDao().getGoal(goalId)
                    ?: return@withTransaction DomainDeletionSummary()
                val links = database.linkDao().getRules().filter { rule ->
                    rule.targetGoalId == goalId || rule.sourceMetricId == goal.metricId
                }
                links.forEach { linkRepository.deleteRule(it.id) }
                check(database.goalDao().deleteGoal(goalId) == 1) { "Goal no longer exists" }
                database.measurementDao().deleteMetric(goal.metricId)
                DomainDeletionSummary(true, links.size)
            }
            linkRepository.rebuildAll()
            summary
        }

    suspend fun deleteTrack(trackId: Long): DomainDeletionSummary {
        val summary = database.withTransaction {
            database.trackDao().getTrack(trackId) ?: return@withTransaction DomainDeletionSummary()
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
            DomainDeletionSummary(true, links.size, triggers.size)
        }
        linkRepository.rebuildAll()
        return summary
    }

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
