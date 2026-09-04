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
    private val rebuildPersonalRecordsAfterExerciseDeletion: suspend (Long) -> Unit = {
        routineRepository.rebuildPersonalRecords(it)
    },
    private val rebuildLinksAfterGymDeletion: suspend () -> Unit = { linkRepository.rebuildAll() },
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
                rule.sourceMeasurementId == habit.measurementId
        }
        links.forEach { linkRepository.deleteRule(it.id) }
        val triggers = database.linkDao().getTriggerRules().filter { rule ->
            (rule.targetType == TriggerTargetType.Habit.name && rule.targetEntityId == habitId) ||
                (rule.sourceType == LinkSourceType.Habit.name && rule.sourceEntityId == habitId)
        }
        triggers.forEach { linkRepository.deleteTrigger(it.id) }
        check(database.habitDao().deleteHabit(habitId) == 1) { "Habit no longer exists" }
        database.measurementDao().deleteMeasurement(habit.measurementId)
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
        check(database.measurementDao().deleteMeasurement(impact.measurementId) == 1) {
            "Goal progress history changed before it could be deleted"
        }
        return GoalDeletionSummary(
            goalDeleted = true,
            linkRulesDeleted = impact.linkRuleCount,
            milestonesDeleted = impact.milestoneCount,
            progressEntriesDeleted = impact.progressEntryCount,
            closureSnapshotsDeleted = impact.closureSnapshotCount,
            elapsedResetEventsDeleted = impact.elapsedResetEventCount,
            contributionsDeleted = impact.contributionCount,
        )
    }

    suspend fun previewTrackDeletion(trackId: Long): TrackDeletionImpact? = database.withTransaction {
        buildTrackDeletionImpact(trackId)
    }

    suspend fun deleteTrack(
        trackId: Long,
        expectedRevisionToken: String,
    ): TrackDeletionSummary {
        val summary = database.withTransaction {
            deleteTrackExactWithinTransaction(trackId, expectedRevisionToken)
        }
        return try {
            linkRepository.rebuildAll()
            summary
        } catch (cancelled: CancellationException) {
            throw CommittedTrackDeletionCancellation(summary, cancelled)
        } catch (_: Exception) {
            try {
                onDeletionInterrupted()
            } catch (cancelled: CancellationException) {
                throw CommittedTrackDeletionCancellation(summary, cancelled)
            } catch (fatal: Error) {
                throw fatal
            } catch (_: Exception) {
                // The deletion is authoritative; startup reconciliation owns the retry.
            }
            summary.copy(
                warnings = summary.warnings +
                    "Link and automation reconciliation did not finish; the Track itself was deleted.",
            )
        }
    }

    /** Room cleanup only. The caller owns the surrounding transaction and every post-commit action. */
    internal suspend fun deleteTrackWithinTransaction(trackId: Long): DomainDeletionSummary {
        val impact = buildTrackDeletionImpact(trackId) ?: return DomainDeletionSummary()
        val summary = deleteTrackExactWithinTransaction(trackId, impact.revisionToken)
        return DomainDeletionSummary(
            deleted = summary.trackDeleted,
            linkRulesDeleted = summary.linkRulesDeleted,
            automationRulesDeleted = summary.automationRulesDeleted,
        )
    }

    private suspend fun deleteTrackExactWithinTransaction(
        trackId: Long,
        expectedRevisionToken: String?,
    ): TrackDeletionSummary {
        val impact = requireNotNull(buildTrackDeletionImpact(trackId)) { "Track no longer exists" }
        if (expectedRevisionToken != null) {
            require(impact.revisionToken == expectedRevisionToken) {
                "The Track or its history changed while the confirmation was open. Review the updated impact before deleting."
            }
        }
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
        return TrackDeletionSummary(
            trackDeleted = true,
            entriesDeleted = impact.entryCount,
            fieldsDeleted = impact.fieldCount,
            choiceOptionsDeleted = impact.choiceOptionCount,
            savedValuesDeleted = impact.savedValueCount,
            linkRulesDeleted = links.size,
            automationRulesDeleted = triggers.size,
        )
    }

    internal suspend fun rebuildLinksAfterCommittedDeletion() = rebuildLinksAfterGoalDeletion()

    suspend fun previewExerciseDeletion(exerciseId: Long): ExerciseDeletionImpact? = database.withTransaction {
        buildExerciseDeletionImpact(exerciseId)
    }

    suspend fun deleteExercise(
        exerciseId: Long,
        expectedRevisionToken: String? = null,
    ): ExerciseDeletionSummary {
        val summary = database.withTransaction {
            val impact = buildExerciseDeletionImpact(exerciseId)
                ?: return@withTransaction ExerciseDeletionSummary()
            require(impact.activePlacements == 0) {
                "This Exercise is referenced by the active workout. Finish or discard it, or archive the Exercise instead."
            }
            if (expectedRevisionToken != null) {
                require(impact.revisionToken == expectedRevisionToken) {
                    "The Exercise or its deletion impact changed while the confirmation was open. Review the updated impact before deleting."
                }
            }

            impact.linkRuleIds.forEach { linkRepository.deleteRule(it) }
            impact.automationRuleIds.forEach { linkRepository.deleteTrigger(it) }

            val routinePlacements = database.routineDao().deleteExercisesForExercise(exerciseId)
            check(routinePlacements == impact.routinePlacementCount) {
                "Exercise routine references changed before they could be deleted"
            }
            val workoutPlacements = database.gymDao().deleteWorkoutExercisesForExercise(exerciseId)
            check(workoutPlacements == impact.workoutPlacementCount) {
                "Exercise workout history changed before it could be deleted"
            }
            impact.routineAlternativeUpdates.forEach { (placementId, retainedAlternatives) ->
                val placement = requireNotNull(database.routineDao().getExercise(placementId)) {
                    "Exercise alternatives changed before they could be updated"
                }
                database.routineDao().updateExercise(
                    placement.copy(
                        alternativeExerciseIdsCsv = retainedAlternatives,
                        updatedAtMillis = System.currentTimeMillis(),
                    ),
                )
            }
            impact.graphPresetUpdates.forEach { (presetId, retainedExercises) ->
                val preset = database.routineDao().getGraphPresets().firstOrNull { it.id == presetId }
                    ?: error("Exercise graph presets changed before they could be updated")
                database.routineDao().updateGraphPreset(preset.copy(exerciseIdsCsv = retainedExercises))
            }
            impact.graphPresetDeleteIds.forEach { database.routineDao().deleteGraphPreset(it) }
            check(database.gymDao().deleteExercise(exerciseId) == 1) { "Exercise no longer exists" }

            ExerciseDeletionSummary(
                exerciseDeleted = true,
                linkRulesDeleted = impact.linkRuleCount,
                linkConditionsDeleted = impact.linkConditionCount,
                linkConditionChoicesDeleted = impact.linkConditionChoiceCount,
                contributionsDeleted = impact.contributionCount,
                automationRulesDeleted = impact.automationRuleCount,
                automationConditionsDeleted = impact.automationConditionCount,
                automationConditionChoicesDeleted = impact.automationConditionChoiceCount,
                automationMappingsDeleted = impact.automationMappingCount,
                automationOccurrencesDeleted = impact.automationOccurrenceCount,
                linkedTrackEntriesDetached = impact.linkedTrackEntryCount,
                workoutPlacementsDeleted = impact.workoutPlacementCount,
                workoutSetsDeleted = impact.workoutSetCount,
                routinePlacementsDeleted = impact.routinePlacementCount,
                routineSetsDeleted = impact.routineSetCount,
                routineAlternativeReferencesCleared = impact.routineAlternativeReferenceCount,
                graphPresetsUpdated = impact.graphPresetUpdateCount,
                graphPresetsDeleted = impact.graphPresetDeleteCount,
                personalRecordsDeleted = impact.personalRecordCount,
                trainingMaxDecisionsPreserved = impact.trainingMaxDecisionCount,
                machineReferencesCleared = impact.machineReferenceCount,
                categoryReferencesDeleted = impact.categoryReferenceCount,
            )
        }
        if (!summary.exerciseDeleted) return summary

        val warnings = mutableListOf<String>()
        try {
            rebuildPersonalRecordsAfterExerciseDeletion(exerciseId)
        } catch (cancelled: CancellationException) {
            throw CommittedExerciseDeletionCancellation(summary.copy(warnings = warnings), cancelled)
        } catch (_: Exception) {
            warnings +=
                "Personal-record reconciliation did not finish; the Exercise deletion was committed and will be reconciled later."
        }
        try {
            rebuildLinksAfterGymDeletion()
        } catch (cancelled: CancellationException) {
            throw CommittedExerciseDeletionCancellation(summary.copy(warnings = warnings), cancelled)
        } catch (_: Exception) {
            warnings += "Link reconciliation did not finish; the Exercise deletion was committed and will be reconciled later."
        }
        return summary.copy(warnings = warnings)
    }

    suspend fun previewRoutineDeletion(routineId: Long): RoutineDeletionImpact? = database.withTransaction {
        buildRoutineDeletionImpact(routineId)
    }

    suspend fun deleteRoutine(
        routineId: Long,
        expectedRevisionToken: String? = null,
    ): RoutineDeletionSummary {
        val summary = database.withTransaction {
            val impact = buildRoutineDeletionImpact(routineId)
                ?: return@withTransaction RoutineDeletionSummary()
            require(!impact.activeSession) {
                "This Routine started the active workout. Finish or discard it, or archive the Routine instead."
            }
            if (expectedRevisionToken != null) {
                require(impact.revisionToken == expectedRevisionToken) {
                    "The Routine or its deletion impact changed while the confirmation was open. Review the updated impact before deleting."
                }
            }
            val historyReferences = database.gymDao().clearSourceRoutine(routineId)
            check(historyReferences == impact.preservedWorkoutHistoryCount) {
                "Routine workout history changed before it could be preserved"
            }
            check(database.routineDao().deleteRoutine(routineId) == 1) { "Routine no longer exists" }
            RoutineDeletionSummary(
                routineDeleted = true,
                daysDeleted = impact.dayCount,
                routinePlacementsDeleted = impact.routinePlacementCount,
                routineSetsDeleted = impact.routineSetCount,
                preservedWorkoutHistoryCount = historyReferences,
                trainingMaxDecisionsPreserved = impact.trainingMaxDecisionCount,
            )
        }
        if (!summary.routineDeleted) return summary

        return try {
            rebuildLinksAfterGymDeletion()
            summary
        } catch (cancelled: CancellationException) {
            throw CommittedRoutineDeletionCancellation(summary, cancelled)
        } catch (_: Exception) {
            summary.copy(
                warnings = listOf(
                    "Link reconciliation did not finish; the Routine deletion was committed and will be reconciled later.",
                ),
            )
        }
    }

    suspend fun previewWorkoutDeletion(sessionId: Long): WorkoutDeletionImpact? = database.withTransaction {
        buildWorkoutDeletionImpact(sessionId)
    }

    suspend fun deleteWorkout(
        sessionId: Long,
        expectedRevisionToken: String? = null,
    ): WorkoutDeletionSummary {
        val summary = database.withTransaction {
            val impact = buildWorkoutDeletionImpact(sessionId)
                ?: return@withTransaction WorkoutDeletionSummary()
            require(impact.state != WorkoutSessionState.Active.name) {
                "The active workout cannot be permanently deleted. Finish or discard it first."
            }
            if (expectedRevisionToken != null) {
                require(impact.revisionToken == expectedRevisionToken) {
                    "The Workout or its recorded history changed while the confirmation was open. Review the updated impact before deleting."
                }
            }
            check(database.gymDao().permanentlyDeleteSession(sessionId) == 1) { "Workout no longer exists" }
            WorkoutDeletionSummary(
                workoutDeleted = true,
                workoutPlacementsDeleted = impact.workoutPlacementCount,
                workoutGroupsDeleted = impact.workoutGroupCount,
                workoutSetsDeleted = impact.workoutSetCount,
                completedSetsDeleted = impact.completedSetCount,
                personalRecordsRecalculated = impact.personalRecordCount,
                trainingMaxDecisionsPreserved = impact.trainingMaxDecisionCount,
                contributionsPreserved = impact.contributionCount,
                generatedHabitLogsPreserved = impact.generatedHabitLogCount,
                triggerOccurrencesPreserved = impact.triggerOccurrenceCount,
                affectedExerciseIds = impact.exerciseIds,
            )
        }
        if (!summary.workoutDeleted) return summary

        val warnings = mutableListOf<String>()
        summary.affectedExerciseIds.forEach { exerciseId ->
            try {
                rebuildPersonalRecordsAfterExerciseDeletion(exerciseId)
            } catch (cancelled: CancellationException) {
                throw CommittedWorkoutDeletionCancellation(summary.copy(warnings = warnings), cancelled)
            } catch (_: Exception) {
                if (warnings.none { it.startsWith("Personal-record reconciliation") }) {
                    warnings +=
                        "Personal-record reconciliation did not finish for one or more affected Exercises; the Workout deletion was committed and will be reconciled when Gym opens again."
                }
            }
        }
        try {
            rebuildLinksAfterGymDeletion()
        } catch (cancelled: CancellationException) {
            throw CommittedWorkoutDeletionCancellation(summary.copy(warnings = warnings), cancelled)
        } catch (_: Exception) {
            warnings +=
                "Link reconciliation did not finish; the Workout deletion was committed and will be reconciled later."
        }
        return summary.copy(warnings = warnings)
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
        val measurement = database.measurementDao().getMeasurement(goal.measurementId)
        val milestones = database.goalDao().getMilestones(goalId)
        val progressEntries = database.measurementDao().getEntriesForMeasurement(goal.measurementId)
        val links = database.linkDao().getRules().filter { rule ->
            rule.targetGoalId == goalId || rule.sourceMeasurementId == goal.measurementId
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
        val closureSnapshots = database.goalDao().getClosureSnapshots(goalId)
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
            closureSnapshotCount = closureSnapshots.size,
            elapsedResetEventCount = elapsedResetEvents.size,
            linkRuleCount = links.size,
            contributionCount = contributions.size,
            revisionToken = goalDeletionRevision(
                goal = goal,
                measurement = measurement,
                milestones = milestones,
                progressEntries = progressEntries,
                closureSnapshots = closureSnapshots,
                elapsedResetEvents = elapsedResetEvents,
                links = links,
                linkConditions = linkConditions,
                linkConditionChoices = linkConditionChoices,
                contributions = contributions,
            ),
            measurementId = goal.measurementId,
            linkRuleIds = linkRuleIds,
        )
    }

    private suspend fun buildExerciseDeletionImpact(exerciseId: Long): ExerciseDeletionImpact? {
        val exercise = database.gymDao().getExercise(exerciseId) ?: return null
        val workoutSessions = database.gymDao().getAllSessions().associateBy { it.id }
        val allWorkoutPlacements = database.gymDao().getAllWorkoutExercises()
        val workoutPlacements = allWorkoutPlacements.filter { it.exerciseId == exerciseId }
        val activePlacementIds = allWorkoutPlacements.filterTo(linkedSetOf()) { placement ->
            workoutSessions[placement.sessionId]?.state == WorkoutSessionState.Active.name &&
                (
                    placement.exerciseId == exerciseId ||
                        exerciseId in placement.alternativeExerciseIdsCsvSnapshot.parseDeletionIds()
                    )
        }.mapTo(linkedSetOf(), WorkoutExerciseEntity::id)
        val workoutPlacementIds = workoutPlacements.mapTo(linkedSetOf(), WorkoutExerciseEntity::id)
        val workoutSets = database.gymDao().getAllWorkoutSets().filter {
            it.workoutExerciseId in workoutPlacementIds
        }

        val allRoutinePlacements = database.routineDao().getAllExercises()
        val routinePlacements = allRoutinePlacements.filter { it.exerciseId == exerciseId }
        val routineSets = routinePlacements.flatMap { database.routineDao().getSets(it.id) }
        val routineAlternativePlacements = allRoutinePlacements.filter { placement ->
            placement.exerciseId != exerciseId && exerciseId in placement.alternativeExerciseIdsCsv.parseDeletionIds()
        }
        val routineAlternativeUpdates = routineAlternativePlacements.associate { placement ->
            placement.id to placement.alternativeExerciseIdsCsv.parseDeletionIds()
                .filterNot { it == exerciseId }
                .joinToString(",")
        }

        val primaryMachineReferences = database.gymDao().getAllMachines().filter { it.exerciseId == exerciseId }
        val machineCompatibilityReferences = database.gymDao().getMachineExerciseJoinsForExercise(exerciseId)
        val categoryReferences = database.gymDao().getCategoryJoins(exerciseId)
        val personalRecords = database.routineDao().getAllPersonalRecords().filter { it.exerciseId == exerciseId }
        val trainingMaxDecisions = database.routineDao().getAllTrainingMaxDecisions().filter {
            it.exerciseUuid == exercise.uuid
        }

        val graphPresetMutations = database.routineDao().getGraphPresets().mapNotNull { preset ->
            val exerciseIds = preset.exerciseIdsCsv.parseDeletionIds()
            if (exerciseId !in exerciseIds) return@mapNotNull null
            preset to exerciseIds.filterNot { it == exerciseId }.joinToString(",")
        }
        val graphPresetUpdates = graphPresetMutations
            .filter { (_, retainedExercises) -> retainedExercises.isNotEmpty() }
            .associate { (preset, retainedExercises) -> preset.id to retainedExercises }
        val graphPresetDeletes = graphPresetMutations
            .filter { (_, retainedExercises) -> retainedExercises.isEmpty() }
            .map { (preset) -> preset }

        val linkRules = database.linkDao().getRules().filter {
            it.sourceType == LinkSourceType.Exercise.name && it.sourceEntityId == exerciseId
        }
        val linkRuleIds = linkRules.mapTo(linkedSetOf(), LinkRuleEntity::id)
        val linkConditions = linkRules.flatMap { database.linkDao().getRuleConditions(it.id) }
        val linkConditionChoices = linkConditions.map(LinkRuleConditionEntity::id)
            .takeIf { it.isNotEmpty() }
            ?.let { database.linkDao().getLinkConditionChoices(it) }
            .orEmpty()
        val contributions = linkRules.flatMap { database.linkDao().getContributions(it.id) }

        val automations = database.linkDao().getTriggerRules().filter {
            it.sourceType == LinkSourceType.Exercise.name && it.sourceEntityId == exerciseId
        }
        val automationRuleIds = automations.mapTo(linkedSetOf(), TriggerRuleEntity::id)
        val automationConditions = automations.flatMap { database.linkDao().getTriggerConditions(it.id) }
        val automationConditionChoices = automationConditions.map(TriggerRuleConditionEntity::id)
            .takeIf { it.isNotEmpty() }
            ?.let { database.linkDao().getTriggerConditionChoices(it) }
            .orEmpty()
        val automationMappings = automations.flatMap { database.linkDao().getTriggerMappings(it.id) }
        val automationOccurrences = automations.flatMap { database.linkDao().getTriggerOccurrences(it.id) }
        val linkedTrackEntries = automationOccurrences.map(TriggerOccurrenceEntity::id)
            .takeIf { it.isNotEmpty() }
            ?.let { database.trackDao().getEntriesForSourceOccurrences(it) }
            .orEmpty()

        return ExerciseDeletionImpact(
            exerciseId = exercise.id,
            displayName = exercise.name,
            activePlacements = activePlacementIds.size,
            routinePlacementCount = routinePlacements.size,
            routineSetCount = routineSets.size,
            routineAlternativeReferenceCount = routineAlternativePlacements.size,
            workoutPlacementCount = workoutPlacements.size,
            workoutSetCount = workoutSets.size,
            linkRuleCount = linkRules.size,
            linkConditionCount = linkConditions.size,
            linkConditionChoiceCount = linkConditionChoices.size,
            contributionCount = contributions.size,
            automationRuleCount = automations.size,
            automationConditionCount = automationConditions.size,
            automationConditionChoiceCount = automationConditionChoices.size,
            automationMappingCount = automationMappings.size,
            automationOccurrenceCount = automationOccurrences.size,
            linkedTrackEntryCount = linkedTrackEntries.size,
            graphPresetUpdateCount = graphPresetUpdates.size,
            graphPresetDeleteCount = graphPresetDeletes.size,
            personalRecordCount = personalRecords.size,
            trainingMaxDecisionCount = trainingMaxDecisions.size,
            machineReferenceCount = primaryMachineReferences.size + machineCompatibilityReferences.size,
            categoryReferenceCount = categoryReferences.size,
            revisionToken = gymDeletionRevision(
                rootLabel = "exercise",
                root = exercise,
                rows = listOf(
                    "active-workout-placement" to allWorkoutPlacements.filter { it.id in activePlacementIds },
                    "workout-placement" to workoutPlacements,
                    "workout-set" to workoutSets,
                    "routine-placement" to routinePlacements,
                    "routine-set" to routineSets,
                    "routine-alternative" to routineAlternativePlacements,
                    "primary-machine-reference" to primaryMachineReferences,
                    "machine-compatibility-reference" to machineCompatibilityReferences,
                    "category-reference" to categoryReferences,
                    "personal-record" to personalRecords,
                    "preserved-training-max-decision" to trainingMaxDecisions,
                    "graph-preset" to graphPresetMutations.map { it.first },
                    "link" to linkRules,
                    "link-condition" to linkConditions,
                    "link-condition-choice" to linkConditionChoices,
                    "contribution" to contributions,
                    "automation" to automations,
                    "automation-condition" to automationConditions,
                    "automation-condition-choice" to automationConditionChoices,
                    "automation-mapping" to automationMappings,
                    "automation-occurrence" to automationOccurrences,
                    "linked-track-entry" to linkedTrackEntries,
                ),
            ),
            linkRuleIds = linkRuleIds,
            automationRuleIds = automationRuleIds,
            routineAlternativeUpdates = routineAlternativeUpdates,
            graphPresetUpdates = graphPresetUpdates,
            graphPresetDeleteIds = graphPresetDeletes.mapTo(linkedSetOf(), GraphPresetEntity::id),
        )
    }

    private suspend fun buildTrackDeletionImpact(trackId: Long): TrackDeletionImpact? {
        val track = database.trackDao().getTrack(trackId) ?: return null
        val fields = database.trackDao().getFields(trackId)
        val fieldIds = fields.map(TrackFieldEntity::id)
        val options = fieldIds.takeIf { it.isNotEmpty() }
            ?.let { database.trackDao().getOptionsForFields(it) }
            .orEmpty()
        val entries = database.trackDao().getEntries(trackId)
        val entryIds = entries.map(TrackEntryEntity::id)
        val values = entryIds.takeIf { it.isNotEmpty() }
            ?.let { database.trackDao().getValuesForEntries(it) }
            .orEmpty()
        val links = database.linkDao().getRules().filter {
            it.sourceType == LinkSourceType.Track.name && it.sourceEntityId == trackId
        }
        val linkConditions = links.flatMap { database.linkDao().getRuleConditions(it.id) }
        val linkConditionChoices = linkConditions.map(LinkRuleConditionEntity::id)
            .takeIf { it.isNotEmpty() }
            ?.let { database.linkDao().getLinkConditionChoices(it) }
            .orEmpty()
        val contributions = links.flatMap { database.linkDao().getContributions(it.id) }
        val triggers = database.linkDao().getTriggerRules().filter {
            (it.sourceType == LinkSourceType.Track.name && it.sourceEntityId == trackId) ||
                (it.targetType == TriggerTargetType.Track.name && it.targetEntityId == trackId)
        }
        val triggerConditions = triggers.flatMap { database.linkDao().getTriggerConditions(it.id) }
        val triggerConditionChoices = triggerConditions.map(TriggerRuleConditionEntity::id)
            .takeIf { it.isNotEmpty() }
            ?.let { database.linkDao().getTriggerConditionChoices(it) }
            .orEmpty()
        val triggerMappings = triggers.flatMap { database.linkDao().getTriggerMappings(it.id) }
        val triggerOccurrences = triggers.flatMap { database.linkDao().getTriggerOccurrences(it.id) }
        return TrackDeletionImpact(
            trackId = track.id,
            trackUuid = track.uuid,
            displayName = track.name,
            entryCount = entries.size,
            fieldCount = fields.size,
            choiceOptionCount = options.size,
            savedValueCount = values.size,
            linkRuleCount = links.size,
            automationRuleCount = triggers.size,
            revisionToken = gymDeletionRevision(
                rootLabel = "track",
                root = track,
                rows = listOf(
                    "field" to fields,
                    "choice-option" to options,
                    "entry" to entries,
                    "saved-value" to values,
                    "link" to links,
                    "link-condition" to linkConditions,
                    "link-condition-choice" to linkConditionChoices,
                    "contribution" to contributions,
                    "automation" to triggers,
                    "automation-condition" to triggerConditions,
                    "automation-condition-choice" to triggerConditionChoices,
                    "automation-mapping" to triggerMappings,
                    "automation-occurrence" to triggerOccurrences,
                ),
            ),
        )
    }

    private suspend fun buildRoutineDeletionImpact(routineId: Long): RoutineDeletionImpact? {
        val routine = database.routineDao().getRoutine(routineId) ?: return null
        val days = database.routineDao().getDays(routineId)
        val placements = days.flatMap { database.routineDao().getExercises(it.id) }
        val sets = placements.flatMap { database.routineDao().getSets(it.id) }
        val workoutReferences = database.gymDao().getAllSessions().filter { it.sourceRoutineId == routineId }
        val activeSessions = workoutReferences.filter { it.state == WorkoutSessionState.Active.name }
        val preservedWorkoutHistory = workoutReferences.filterNot { it.state == WorkoutSessionState.Active.name }
        val trainingMaxDecisions = database.routineDao().getAllTrainingMaxDecisions().filter {
            it.routineUuid == routine.uuid
        }
        return RoutineDeletionImpact(
            routineId = routine.id,
            displayName = routine.name,
            activeSession = activeSessions.isNotEmpty(),
            dayCount = days.size,
            routinePlacementCount = placements.size,
            routineSetCount = sets.size,
            preservedWorkoutHistoryCount = preservedWorkoutHistory.size,
            trainingMaxDecisionCount = trainingMaxDecisions.size,
            revisionToken = gymDeletionRevision(
                rootLabel = "routine",
                root = routine,
                rows = listOf(
                    "routine-day" to days,
                    "routine-placement" to placements,
                    "routine-set" to sets,
                    "workout-reference" to workoutReferences,
                    "preserved-training-max-decision" to trainingMaxDecisions,
                ),
            ),
        )
    }

    private suspend fun buildWorkoutDeletionImpact(sessionId: Long): WorkoutDeletionImpact? {
        val session = database.gymDao().getSession(sessionId) ?: return null
        val placements = database.gymDao().getWorkoutExercises(sessionId)
        val placementIds = placements.mapTo(linkedSetOf(), WorkoutExerciseEntity::id)
        val sets = database.gymDao().getAllWorkoutSets().filter { it.workoutExerciseId in placementIds }
        val groups = database.gymDao().getWorkoutGroups(sessionId)
        val personalRecords = database.routineDao().getAllPersonalRecords().filter {
            it.sourceSessionId == sessionId
        }
        val trainingMaxDecisions = database.routineDao().getAllTrainingMaxDecisions().filter {
            it.sessionUuid == session.uuid
        }
        val contributions = database.linkDao().observeContributionsSnapshot().filter {
            it.sourceEventId.startsWith("workout:${session.uuid}:") ||
                it.sourceEventId.contains(":workout:${session.uuid}:")
        }
        val triggerOccurrences = database.linkDao().getTriggerRules()
            .flatMap { database.linkDao().getTriggerOccurrences(it.id) }
            .filter {
                it.sourceEventId.startsWith("workout:${session.uuid}:") ||
                    it.sourceEventId.contains(":workout:${session.uuid}:")
            }
        val generatedHabitLogs = database.habitDao().getAllLogs().filter {
            it.sourceId?.startsWith("trigger:") == true &&
                it.sourceId.contains(":workout:${session.uuid}:")
        }
        val exerciseIds = placements.map(WorkoutExerciseEntity::exerciseId).distinct().sorted()
        return WorkoutDeletionImpact(
            sessionId = session.id,
            sessionUuid = session.uuid,
            displayName = session.name,
            localDate = LocalDate.ofEpochDay(session.localEpochDay),
            state = session.state,
            archived = session.archived,
            workoutPlacementCount = placements.size,
            workoutGroupCount = groups.size,
            workoutSetCount = sets.size,
            completedSetCount = sets.count { it.completed && it.deletedAtMillis == null },
            personalRecordCount = personalRecords.size,
            trainingMaxDecisionCount = trainingMaxDecisions.size,
            contributionCount = contributions.size,
            generatedHabitLogCount = generatedHabitLogs.size,
            triggerOccurrenceCount = triggerOccurrences.size,
            revisionToken = gymDeletionRevision(
                rootLabel = "workout",
                root = session,
                rows = listOf(
                    "workout-group" to groups,
                    "workout-placement" to placements,
                    "workout-set" to sets,
                    "personal-record" to personalRecords,
                    "preserved-training-max-decision" to trainingMaxDecisions,
                    "preserved-contribution" to contributions,
                    "preserved-generated-habit-log" to generatedHabitLogs,
                    "preserved-trigger-occurrence" to triggerOccurrences,
                ),
            ),
            exerciseIds = exerciseIds,
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
    measurement: Any?,
    milestones: List<*>,
    progressEntries: List<*>,
    closureSnapshots: List<*>,
    elapsedResetEvents: List<*>,
    links: List<*>,
    linkConditions: List<*>,
    linkConditionChoices: List<*>,
    contributions: List<*>,
): String {
    val canonical = buildString {
        append("goal|").append(goal).append('\n')
        append("measurement|").append(measurement).append('\n')
        listOf(
            "milestone" to milestones,
            "progress-entry" to progressEntries,
            "closure" to closureSnapshots,
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

private fun String.parseDeletionIds(): List<Long> = split(',').mapNotNull { it.trim().toLongOrNull() }

private fun gymDeletionRevision(
    rootLabel: String,
    root: Any,
    rows: List<Pair<String, List<*>>>,
): String {
    val canonical = buildString {
        append(rootLabel).append('|').append(root).append('\n')
        rows.forEach { (label, values) ->
            values.map(Any?::toString).sorted().forEach { value ->
                append(label).append('|').append(value).append('\n')
            }
        }
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

data class ExerciseDeletionImpact(
    val exerciseId: Long,
    val displayName: String,
    val activePlacements: Int,
    val routinePlacementCount: Int,
    val routineSetCount: Int,
    val routineAlternativeReferenceCount: Int,
    val workoutPlacementCount: Int,
    val workoutSetCount: Int,
    val linkRuleCount: Int,
    val linkConditionCount: Int,
    val linkConditionChoiceCount: Int,
    val contributionCount: Int,
    val automationRuleCount: Int,
    val automationConditionCount: Int,
    val automationConditionChoiceCount: Int,
    val automationMappingCount: Int,
    val automationOccurrenceCount: Int,
    val linkedTrackEntryCount: Int,
    val graphPresetUpdateCount: Int,
    val graphPresetDeleteCount: Int,
    val personalRecordCount: Int,
    val trainingMaxDecisionCount: Int,
    val machineReferenceCount: Int,
    val categoryReferenceCount: Int,
    val revisionToken: String,
    internal val linkRuleIds: Set<Long> = emptySet(),
    internal val automationRuleIds: Set<Long> = emptySet(),
    internal val routineAlternativeUpdates: Map<Long, String> = emptyMap(),
    internal val graphPresetUpdates: Map<Long, String> = emptyMap(),
    internal val graphPresetDeleteIds: Set<Long> = emptySet(),
) : Serializable

data class ExerciseDeletionSummary(
    val exerciseDeleted: Boolean = false,
    val linkRulesDeleted: Int = 0,
    val linkConditionsDeleted: Int = 0,
    val linkConditionChoicesDeleted: Int = 0,
    val contributionsDeleted: Int = 0,
    val automationRulesDeleted: Int = 0,
    val automationConditionsDeleted: Int = 0,
    val automationConditionChoicesDeleted: Int = 0,
    val automationMappingsDeleted: Int = 0,
    val automationOccurrencesDeleted: Int = 0,
    val linkedTrackEntriesDetached: Int = 0,
    val workoutPlacementsDeleted: Int = 0,
    val workoutSetsDeleted: Int = 0,
    val routinePlacementsDeleted: Int = 0,
    val routineSetsDeleted: Int = 0,
    val routineAlternativeReferencesCleared: Int = 0,
    val graphPresetsUpdated: Int = 0,
    val graphPresetsDeleted: Int = 0,
    val personalRecordsDeleted: Int = 0,
    val trainingMaxDecisionsPreserved: Int = 0,
    val machineReferencesCleared: Int = 0,
    val categoryReferencesDeleted: Int = 0,
    val warnings: List<String> = emptyList(),
) {
    val deleted: Boolean get() = exerciseDeleted
}

class CommittedExerciseDeletionCancellation(
    val summary: ExerciseDeletionSummary,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init { initCause(cause) }
}

data class RoutineDeletionImpact(
    val routineId: Long,
    val displayName: String,
    val activeSession: Boolean,
    val dayCount: Int,
    val routinePlacementCount: Int,
    val routineSetCount: Int,
    val preservedWorkoutHistoryCount: Int,
    val trainingMaxDecisionCount: Int,
    val revisionToken: String,
) : Serializable

data class RoutineDeletionSummary(
    val routineDeleted: Boolean = false,
    val daysDeleted: Int = 0,
    val routinePlacementsDeleted: Int = 0,
    val routineSetsDeleted: Int = 0,
    val preservedWorkoutHistoryCount: Int = 0,
    val trainingMaxDecisionsPreserved: Int = 0,
    val warnings: List<String> = emptyList(),
) {
    val deleted: Boolean get() = routineDeleted
    val preservedHistoryReferences: Int get() = preservedWorkoutHistoryCount
}

class CommittedRoutineDeletionCancellation(
    val summary: RoutineDeletionSummary,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init { initCause(cause) }
}

data class WorkoutDeletionImpact(
    val sessionId: Long,
    val sessionUuid: String,
    val displayName: String,
    val localDate: LocalDate,
    val state: String,
    val archived: Boolean,
    val workoutPlacementCount: Int,
    val workoutGroupCount: Int,
    val workoutSetCount: Int,
    val completedSetCount: Int,
    val personalRecordCount: Int,
    val trainingMaxDecisionCount: Int,
    val contributionCount: Int,
    val generatedHabitLogCount: Int,
    val triggerOccurrenceCount: Int,
    val revisionToken: String,
    internal val exerciseIds: List<Long> = emptyList(),
) : Serializable

data class WorkoutDeletionSummary(
    val workoutDeleted: Boolean = false,
    val workoutPlacementsDeleted: Int = 0,
    val workoutGroupsDeleted: Int = 0,
    val workoutSetsDeleted: Int = 0,
    val completedSetsDeleted: Int = 0,
    val personalRecordsRecalculated: Int = 0,
    val trainingMaxDecisionsPreserved: Int = 0,
    val contributionsPreserved: Int = 0,
    val generatedHabitLogsPreserved: Int = 0,
    val triggerOccurrencesPreserved: Int = 0,
    val warnings: List<String> = emptyList(),
    internal val affectedExerciseIds: List<Long> = emptyList(),
)

class CommittedWorkoutDeletionCancellation(
    val summary: WorkoutDeletionSummary,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init { initCause(cause) }
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
    val closureSnapshotCount: Int = 0,
    val elapsedResetEventCount: Int = 0,
    val linkRuleCount: Int = 0,
    val contributionCount: Int = 0,
    val revisionToken: String = "",
    internal val measurementId: String = "",
    internal val linkRuleIds: Set<Long> = emptySet(),
) : Serializable

data class GoalDeletionSummary(
    val goalDeleted: Boolean = false,
    val linkRulesDeleted: Int = 0,
    val milestonesDeleted: Int = 0,
    val progressEntriesDeleted: Int = 0,
    val closureSnapshotsDeleted: Int = 0,
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

data class TrackDeletionImpact(
    val trackId: Long,
    val trackUuid: String,
    val displayName: String,
    val entryCount: Int,
    val fieldCount: Int,
    val choiceOptionCount: Int,
    val savedValueCount: Int,
    val linkRuleCount: Int,
    val automationRuleCount: Int,
    val revisionToken: String,
) : Serializable

data class TrackDeletionSummary(
    val trackDeleted: Boolean = false,
    val entriesDeleted: Int = 0,
    val fieldsDeleted: Int = 0,
    val choiceOptionsDeleted: Int = 0,
    val savedValuesDeleted: Int = 0,
    val linkRulesDeleted: Int = 0,
    val automationRulesDeleted: Int = 0,
    val warnings: List<String> = emptyList(),
)

class CommittedTrackDeletionCancellation(
    val summary: TrackDeletionSummary,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init { initCause(cause) }
}
