package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.Contribution
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.LinkBackfillPreview
import com.whip.app.domain.LinkKind
import com.whip.app.domain.LinkRule
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.LinkValueMode
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TriggerOccurrence
import com.whip.app.domain.TriggerOutcome
import com.whip.app.domain.TriggerRule
import com.whip.app.domain.TriggerRuleDraft
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.estimatedOneRepMax
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface LinkRepository {
    val rules: Flow<List<LinkRule>>
    val contributions: Flow<List<Contribution>>
    val triggerRules: Flow<List<TriggerRule>>
    val triggerOccurrences: Flow<List<TriggerOccurrence>>

    suspend fun createRule(draft: LinkRuleDraft, commitBackfill: Boolean = false): Long
    suspend fun updateRule(id: Long, draft: LinkRuleDraft)
    suspend fun deleteRule(id: Long)
    suspend fun setRuleEnabled(id: Long, enabled: Boolean)
    suspend fun previewBackfill(draft: LinkRuleDraft): LinkBackfillPreview
    suspend fun rebuildRule(id: Long)
    suspend fun rebuildAll()
    suspend fun rebuildSources(sourceTypes: Set<LinkSourceType>)
    suspend fun setContributionExcluded(id: Long, excluded: Boolean)
    suspend fun setContributionOverride(id: Long, canonicalValue: Double?)

    suspend fun createTrigger(draft: TriggerRuleDraft): Long
    suspend fun updateTrigger(id: Long, draft: TriggerRuleDraft)
    suspend fun deleteTrigger(id: Long)
    suspend fun dismissTriggerOccurrence(id: Long)
}

class RoomLinkRepository(
    private val database: WhipDatabase,
    private val measurementRepository: MeasurementRepository,
    private val clock: WhipClock,
    private val ids: WhipIdGenerator,
) : LinkRepository {
    private val dao = database.linkDao()
    private val rebuildMutex = Mutex()

    override val rules = dao.observeRules().map { list -> list.map(LinkRuleEntity::toDomain) }
    override val contributions = dao.observeContributions().map { list -> list.map(ContributionEntity::toDomain) }
    override val triggerRules = dao.observeTriggerRules().map { list -> list.map(TriggerRuleEntity::toDomain) }
    override val triggerOccurrences = dao.observeTriggerOccurrences().map { list -> list.map(TriggerOccurrenceEntity::toDomain) }

    override suspend fun createRule(draft: LinkRuleDraft, commitBackfill: Boolean): Long {
        validateRule(draft)
        val now = clock.now().toEpochMilli()
        val entity = draft.toEntity(ids.nextId(), now).copy(
            retroactiveFromEpochDay = draft.retroactiveFrom?.toEpochDay()
                .takeIf { commitBackfill },
        )
        val id = dao.insertRule(entity)
        rebuildRule(id)
        return id
    }

    override suspend fun updateRule(id: Long, draft: LinkRuleDraft) {
        validateRule(draft, excludingRuleId = id)
        val current = dao.getRule(id) ?: error("Link no longer exists")
        dao.updateRule(
            draft.toEntity(current.uuid, current.createdAtMillis).copy(
                id = current.id,
                retroactiveFromEpochDay = draft.retroactiveFrom?.toEpochDay(),
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
        rebuildRule(id)
    }

    override suspend fun deleteRule(id: Long) = database.withTransaction {
        dao.getContributions(id).forEach { it.metricEntryId?.let { entryId -> measurementRepository.deleteEntry(entryId) } }
        dao.deleteRule(id)
    }

    override suspend fun setRuleEnabled(id: Long, enabled: Boolean) {
        val current = dao.getRule(id) ?: return
        dao.updateRule(current.copy(enabled = enabled, updatedAtMillis = clock.now().toEpochMilli()))
        rebuildRule(id)
    }

    override suspend fun previewBackfill(draft: LinkRuleDraft): LinkBackfillPreview {
        validateRule(draft)
        val temporary = draft.toEntity("preview", clock.now().toEpochMilli()).copy(
            id = -1,
            retroactiveFromEpochDay = draft.retroactiveFrom?.toEpochDay() ?: Long.MIN_VALUE,
        )
        val target = database.goalDao().getGoal(draft.targetGoalId) ?: error("Goal no longer exists")
        val events = sourceEvents(temporary).filter { eligibleValue(temporary, it, target) != null || temporary.kind == LinkKind.Context.name }
        val values = events.mapNotNull { eligibleValue(temporary, it, target) }
        return LinkBackfillPreview(
            eligibleEventCount = events.size,
            contributionCount = events.size,
            totalCanonicalValue = values.sum(),
            firstDate = events.minOfOrNull(SourceEvent::date),
            lastDate = events.maxOfOrNull(SourceEvent::date),
        )
    }

    override suspend fun rebuildRule(id: Long) = rebuildMutex.withLock {
        database.withTransaction { rebuildRuleLocked(id) }
    }

    override suspend fun rebuildAll() = rebuildMutex.withLock {
        database.withTransaction {
            dao.getRules().forEach { rebuildRuleLocked(it.id) }
            rebuildTriggersLocked()
        }
    }

    override suspend fun rebuildSources(sourceTypes: Set<LinkSourceType>) = rebuildMutex.withLock {
        if (sourceTypes.isEmpty()) return@withLock
        val names = sourceTypes.mapTo(mutableSetOf(), LinkSourceType::name)
        database.withTransaction {
            dao.getRules().filter { it.sourceType in names }.forEach { rebuildRuleLocked(it.id) }
            rebuildTriggersLocked(names)
        }
    }

    override suspend fun setContributionExcluded(id: Long, excluded: Boolean) {
        val now = clock.now().toEpochMilli()
        dao.setContributionExcluded(id, excluded, now)
        val contribution = dao.observeContributionsSnapshot().firstOrNull { it.id == id } ?: return
        rebuildRule(contribution.linkRuleId)
    }

    override suspend fun setContributionOverride(id: Long, canonicalValue: Double?) {
        val now = clock.now().toEpochMilli()
        dao.setContributionOverride(id, canonicalValue, now)
        val contribution = dao.observeContributionsSnapshot().firstOrNull { it.id == id } ?: return
        rebuildRule(contribution.linkRuleId)
    }

    override suspend fun createTrigger(draft: TriggerRuleDraft): Long {
        validateTrigger(draft)
        val now = clock.now().toEpochMilli()
        val id = dao.insertTriggerRule(draft.toEntity(ids.nextId(), now))
        rebuildAll()
        return id
    }

    override suspend fun updateTrigger(id: Long, draft: TriggerRuleDraft) {
        validateTrigger(draft, id)
        val current = dao.getTriggerRule(id) ?: error("Trigger no longer exists")
        dao.updateTriggerRule(
            draft.toEntity(current.uuid, current.createdAtMillis).copy(
                id = id,
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
        rebuildAll()
    }

    override suspend fun deleteTrigger(id: Long) {
        database.withTransaction {
            val current = dao.getTriggerRule(id) ?: return@withTransaction
            removeGeneratedHabitLogs(current, emptySet())
            dao.deleteTriggerRule(id)
        }
    }

    override suspend fun dismissTriggerOccurrence(id: Long) =
        dao.dismissTriggerOccurrence(id, clock.now().toEpochMilli())

    private suspend fun rebuildRuleLocked(id: Long) {
        val rule = dao.getRule(id) ?: return
        val existing = dao.getContributions(id).associateBy(ContributionEntity::sourceEventId)
        if (!rule.enabled) {
            existing.values.forEach { removeContribution(it) }
            updateLinkedMilestone(rule)
            return
        }
        val target = database.goalDao().getGoal(rule.targetGoalId) ?: return
        val events = sourceEvents(rule)
        val retained = mutableSetOf<String>()
        events.forEach { event ->
            val transformed = eligibleValue(rule, event, target)
            if (rule.kind == LinkKind.Contribution.name && transformed == null && rule.targetMilestoneId == null) return@forEach
            retained += event.id
            val old = existing[event.id]
            val uuid = old?.uuid ?: ids.nextId()
            val excluded = old?.excluded ?: false
            val override = old?.overrideValue
            val effectiveCanonical = override ?: transformed
            val metricEntryId = if (
                rule.kind == LinkKind.Contribution.name &&
                rule.targetMilestoneId == null &&
                !excluded &&
                effectiveCanonical != null
            ) {
                val targetUnit = resolveUnit(target.unitId) ?: error("The linked goal unit no longer exists")
                measurementRepository.record(
                    metricId = target.metricId,
                    value = targetUnit.fromCanonical(effectiveCanonical),
                    unitId = target.unitId,
                    timestamp = event.timestamp,
                    localDate = event.date,
                    zoneId = clock.zoneId(),
                    sourceType = event.metricSourceType,
                    sourceId = uuid,
                    note = "Linked: ${event.explanation}",
                    existingEntryId = old?.metricEntryId ?: ids.nextId(),
                )
            } else {
                old?.metricEntryId?.let { measurementRepository.deleteEntry(it) }
                null
            }
            dao.upsertContribution(
                ContributionEntity(
                    id = old?.id ?: 0,
                    uuid = uuid,
                    linkRuleId = rule.id,
                    sourceEventId = event.id,
                    sourceType = event.type.name,
                    sourceEntityId = event.entityId,
                    targetGoalId = target.id,
                    metricEntryId = metricEntryId,
                    canonicalValue = transformed,
                    localEpochDay = event.date.toEpochDay(),
                    timestampMillis = event.timestamp.toEpochMilli(),
                    excluded = excluded,
                    overrideValue = override,
                    explanation = event.explanation,
                    createdAtMillis = old?.createdAtMillis ?: clock.now().toEpochMilli(),
                    updatedAtMillis = clock.now().toEpochMilli(),
                ),
            )
        }
        existing.values.filter { it.sourceEventId !in retained }.forEach { removeContribution(it) }
        updateLinkedMilestone(rule)
    }

    private suspend fun removeContribution(entity: ContributionEntity) {
        entity.metricEntryId?.let { measurementRepository.deleteEntry(it) }
        dao.deleteContribution(entity.id)
    }

    private suspend fun updateLinkedMilestone(rule: LinkRuleEntity) {
        val milestoneId = rule.targetMilestoneId ?: return
        val milestone = database.goalDao().getMilestone(milestoneId) ?: return
        val completed = dao.getContributions(rule.id).any { !it.excluded }
        val now = clock.now().toEpochMilli()
        database.goalDao().updateMilestone(
            milestone.copy(
                completed = completed,
                completedAtMillis = now.takeIf { completed },
                updatedAtMillis = now,
            ),
        )
    }

    private suspend fun validateRule(draft: LinkRuleDraft, excludingRuleId: Long? = null) {
        require(draft.name.isNotBlank()) { "Link name is required" }
        require(draft.multiplier.isFinite() && draft.offset.isFinite()) { "Enter a finite transformation" }
        if (draft.valueMode == LinkValueMode.FixedValue) requireNotNull(draft.fixedValue) { "Enter a fixed value" }
        val target = database.goalDao().getGoal(draft.targetGoalId) ?: error("Goal no longer exists")
        draft.targetMilestoneId?.let { milestoneId ->
            require(database.goalDao().getMilestone(milestoneId)?.goalId == target.id) { "Milestone does not belong to this goal" }
        }
        if (draft.kind == LinkKind.Contribution && draft.targetMilestoneId == null && draft.valueMode == LinkValueMode.SourceValue) {
            val sourceDimension = sourceDimension(draft.sourceType, draft.sourceEntityId, draft.sourceMetricId, draft.sourceMetric)
            require(sourceDimension == UnitDimension.valueOf(target.dimension)) {
                "The source uses $sourceDimension but the goal uses ${target.dimension}"
            }
        }
        if (draft.sourceType == LinkSourceType.Metric) {
            val metricId = requireNotNull(draft.sourceMetricId) { "Choose a source metric" }
            require(metricId != target.metricId) { "A goal cannot contribute to itself" }
            val edges = dao.getRules().filter { it.id != excludingRuleId && it.enabled && it.sourceType == LinkSourceType.Metric.name }
                .mapNotNull { link ->
                    val goal = database.goalDao().getGoal(link.targetGoalId) ?: return@mapNotNull null
                    link.sourceMetricId?.let { it to goal.metricId }
                } + (metricId to target.metricId)
            require(!hasDirectedCycle(edges)) { "This link would create a circular goal dependency" }
        }
    }

    private suspend fun sourceDimension(type: LinkSourceType, entityId: Long?, metricId: String?, metric: LinkSourceMetric): UnitDimension = when (type) {
        LinkSourceType.Habit -> UnitDimension.valueOf(database.habitDao().getHabit(requireNotNull(entityId))?.dimension ?: error("Habit no longer exists"))
        LinkSourceType.Task, LinkSourceType.Subtask -> UnitDimension.Count
        LinkSourceType.Workout -> when (metric) {
            LinkSourceMetric.Duration -> UnitDimension.Duration
            LinkSourceMetric.Volume -> UnitDimension.Custom
            else -> UnitDimension.Count
        }
        LinkSourceType.Exercise -> when (metric) {
            LinkSourceMetric.EstimatedOneRepMax, LinkSourceMetric.MaxWeight -> UnitDimension.Mass
            LinkSourceMetric.Distance -> UnitDimension.Distance
            LinkSourceMetric.Repetitions, LinkSourceMetric.Count -> UnitDimension.Count
            LinkSourceMetric.Duration -> UnitDimension.Duration
            LinkSourceMetric.Volume -> UnitDimension.Custom
            else -> UnitDimension.Unitless
        }
        LinkSourceType.Metric -> UnitDimension.valueOf(database.measurementDao().getMetric(requireNotNull(metricId))?.dimension ?: error("Metric no longer exists"))
    }

    private suspend fun eligibleValue(rule: LinkRuleEntity, event: SourceEvent, target: GoalEntity): Double? {
        if (rule.valueMode == LinkValueMode.FixedValue.name) {
            val unit = resolveUnit(target.unitId) ?: return null
            return rule.fixedValue?.let(unit::toCanonical)?.times(rule.multiplier)?.plus(rule.offset)
        }
        return event.canonicalValue?.times(rule.multiplier)?.plus(rule.offset)
    }

    private suspend fun sourceEvents(rule: LinkRuleEntity): List<SourceEvent> {
        val all = sourceEvents(
            LinkSourceType.valueOf(rule.sourceType),
            rule.sourceEntityId,
            rule.sourceMetricId,
            rule.sourceItemId,
            LinkSourceMetric.valueOf(rule.sourceMetric),
        )
        val earliestEpochDay = rule.retroactiveFromEpochDay
        return all.filter { event ->
            if (earliestEpochDay != null) event.date.toEpochDay() >= earliestEpochDay
            else event.timestamp.toEpochMilli() >= rule.createdAtMillis
        }
    }

    private suspend fun resolveUnit(id: String): UnitDefinition? = BuiltInUnits.get(id)
        ?: database.measurementDao().getUnit(id)?.let { unit ->
            UnitDefinition(
                id = unit.id,
                name = unit.name,
                symbol = unit.symbol,
                dimension = UnitDimension.valueOf(unit.dimension),
                toCanonicalFactor = unit.toCanonicalFactor,
                toCanonicalOffset = unit.toCanonicalOffset,
                custom = unit.custom,
                createdAtMillis = unit.createdAtMillis,
                updatedAtMillis = unit.updatedAtMillis,
            )
        }

    private suspend fun sourceEvents(
        type: LinkSourceType,
        entityId: Long?,
        metricId: String?,
        itemId: Long?,
        metric: LinkSourceMetric,
    ): List<SourceEvent> = when (type) {
        LinkSourceType.Habit -> habitEvents(requireNotNull(entityId), metric)
        LinkSourceType.Task -> taskEvents(requireNotNull(entityId))
        LinkSourceType.Subtask -> subtaskEvents(requireNotNull(entityId), itemId)
        LinkSourceType.Workout -> workoutEvents(metric)
        LinkSourceType.Exercise -> exerciseEvents(requireNotNull(entityId), metric)
        LinkSourceType.Metric -> metricEvents(requireNotNull(metricId))
    }

    private suspend fun habitEvents(habitId: Long, metric: LinkSourceMetric): List<SourceEvent> {
        val habit = database.habitDao().getHabit(habitId) ?: return emptyList()
        habit.sourceMetricId?.let { sourceMetricId ->
            val sourceEvents = metricEvents(sourceMetricId)
            if (metric != LinkSourceMetric.Success) {
                return sourceEvents.map { event ->
                    event.copy(
                        id = "habit-source:${habit.uuid}:${event.id}",
                        type = LinkSourceType.Habit,
                        entityId = habitId,
                        explanation = "${habit.name}: ${event.explanation}",
                    )
                }
            }
            val unit = resolveUnit(habit.unitId)
            val targetMin = habit.targetMin?.let { unit?.toCanonical(it) ?: it }
            val targetMax = habit.targetMax?.let { unit?.toCanonical(it) ?: it }
            return sourceEvents.groupBy(SourceEvent::date).mapNotNull { (day, events) ->
                val entered = events.sumOf { it.canonicalValue ?: 0.0 }
                val success = when (TargetComparison.valueOf(habit.comparison)) {
                    TargetComparison.AtLeast -> targetMin?.let { entered >= it }
                    TargetComparison.AtMost -> (targetMax ?: targetMin)?.let { entered <= it }
                    TargetComparison.Exactly -> targetMin?.let { entered == it }
                    TargetComparison.WithinRange -> targetMin?.let { min -> targetMax?.let { max -> entered in min..max } }
                    TargetComparison.None -> events.isNotEmpty()
                } == true
                if (!success) return@mapNotNull null
                val latest = events.maxBy(SourceEvent::timestamp)
                SourceEvent(
                    id = "habit-source:${habit.uuid}:day:$day:success",
                    type = LinkSourceType.Habit,
                    entityId = habitId,
                    canonicalValue = 1.0,
                    dimension = UnitDimension.Count,
                    date = day,
                    timestamp = latest.timestamp,
                    explanation = "${habit.name} reached its target from ${latest.explanation}",
                    metricSourceType = latest.metricSourceType,
                )
            }
        }
        val logs = database.habitDao().getAllLogs().filter { it.habitId == habitId }
        if (metric == LinkSourceMetric.Success) {
            return logs.groupBy(HabitLogEntity::localEpochDay).mapNotNull { (day, dayLogs) ->
                val recorded = dayLogs.filter { it.status in setOf(HabitLogStatus.Recorded.name, HabitLogStatus.Success.name, HabitLogStatus.Failed.name) }
                val entered = recorded.sumOf { it.value ?: 0.0 }
                val latest = dayLogs.maxByOrNull(HabitLogEntity::timestampMillis)
                val success = when {
                    recorded.any { it.status == HabitLogStatus.Success.name } -> true
                    habit.trackingMode == "CheckOff" -> recorded.any { (it.value ?: 0.0) > 0.0 }
                    else -> when (TargetComparison.valueOf(habit.comparison)) {
                        TargetComparison.AtLeast -> habit.targetMin?.let { entered >= it }
                        TargetComparison.AtMost -> (habit.targetMax ?: habit.targetMin)?.let { entered <= it }
                        TargetComparison.Exactly -> habit.targetMin?.let { entered == it }
                        TargetComparison.WithinRange -> habit.targetMin?.let { min -> habit.targetMax?.let { max -> entered in min..max } }
                        TargetComparison.None -> recorded.isNotEmpty()
                    } == true
                }
                if (!success || latest == null) null else SourceEvent(
                    "habit:${habit.uuid}:day:$day:success", LinkSourceType.Habit, habitId, 1.0,
                    UnitDimension.Count, LocalDate.ofEpochDay(day), Instant.ofEpochMilli(latest.timestampMillis),
                    "${habit.name} succeeded on ${LocalDate.ofEpochDay(day)}", MetricSourceType.Habit,
                )
            }
        }
        return logs.filter { it.status in setOf(HabitLogStatus.Recorded.name, HabitLogStatus.Success.name) && it.canonicalValue != null }
            .map { log -> SourceEvent(
                "habit-log:${log.uuid}", LinkSourceType.Habit, habitId, log.canonicalValue,
                UnitDimension.valueOf(habit.dimension), LocalDate.ofEpochDay(log.localEpochDay),
                Instant.ofEpochMilli(log.timestampMillis), "${habit.name}: ${log.value} ${log.enteredUnitId.orEmpty()}", MetricSourceType.Habit,
            ) }
    }

    private suspend fun taskEvents(taskId: Long): List<SourceEvent> {
        val task = database.taskDao().getTask(taskId) ?: return emptyList()
        val events = mutableListOf<SourceEvent>()
        task.completedAtMillis?.let { completed ->
            val instant = Instant.ofEpochMilli(completed)
            events += SourceEvent("task:$taskId:single", LinkSourceType.Task, taskId, 1.0, UnitDimension.Count,
                instant.atZone(clock.zoneId()).toLocalDate(), instant, "Completed task: ${task.title}", MetricSourceType.Task)
        }
        database.taskDao().getOccurrences(taskId).filter { it.state == "Completed" && it.completedAtMillis != null }.forEach { occurrence ->
            events += SourceEvent("task:$taskId:${occurrence.originalEpochDay}", LinkSourceType.Task, taskId, 1.0, UnitDimension.Count,
                LocalDate.ofEpochDay(occurrence.originalEpochDay), Instant.ofEpochMilli(requireNotNull(occurrence.completedAtMillis)),
                "Completed task: ${task.title} (${LocalDate.ofEpochDay(occurrence.originalEpochDay)})", MetricSourceType.Task)
        }
        return events
    }

    private suspend fun subtaskEvents(taskId: Long, itemId: Long?): List<SourceEvent> {
        val task = database.taskDao().getTask(taskId) ?: return emptyList()
        val steps = database.taskDao().getAllSteps().filter { it.taskId == taskId }.associateBy { it.id }
        return database.taskDao().getAllStepStates().filter { it.taskId == taskId && it.completed && it.completedAtMillis != null && (itemId == null || it.stepId == itemId) }
            .map { state ->
                val title = state.titleSnapshot.ifBlank { steps[state.stepId]?.title.orEmpty() }
                val instant = Instant.ofEpochMilli(requireNotNull(state.completedAtMillis))
                SourceEvent("subtask:$taskId:${state.occurrenceKey}:${state.stepId}", LinkSourceType.Subtask, taskId, 1.0,
                    UnitDimension.Count, instant.atZone(clock.zoneId()).toLocalDate(), instant,
                    "Completed subtask $title in ${task.title}", MetricSourceType.Task)
            }
    }

    private suspend fun workoutEvents(metric: LinkSourceMetric): List<SourceEvent> {
        val gym = database.gymDao()
        val sessions = gym.getAllSessions().filter { it.state == "Finished" && !it.archived }
        val workoutExercises = gym.getAllWorkoutExercises()
        val sets = gym.getAllWorkoutSets()
        val exercises = gym.getAllExercises().associateBy(ExerciseEntity::id)
        return sessions.mapNotNull { session ->
            val sessionExercises = workoutExercises.filter { it.sessionId == session.id }
            val sessionSets = sets.filter { set -> sessionExercises.any { it.id == set.workoutExerciseId } && set.completed && set.deletedAtMillis == null }
            val valueAndDimension = when (metric) {
                LinkSourceMetric.Count, LinkSourceMetric.Completion -> 1.0 to UnitDimension.Count
                LinkSourceMetric.Duration -> ((session.endedAtMillis ?: session.startedAtMillis) - session.startedAtMillis).coerceAtLeast(0).div(1_000.0) to UnitDimension.Duration
                LinkSourceMetric.Volume -> sessionSets.sumOf { set -> rawSetVolume(set, sessionExercises, exercises) } to UnitDimension.Custom
                else -> return@mapNotNull null
            }
            SourceEvent("workout:${session.uuid}:${metric.name}", LinkSourceType.Workout, session.id, valueAndDimension.first,
                valueAndDimension.second, LocalDate.ofEpochDay(session.localEpochDay), Instant.ofEpochMilli(session.endedAtMillis ?: session.startedAtMillis),
                "Workout ${session.name.ifBlank { LocalDate.ofEpochDay(session.localEpochDay).toString() }}: ${metric.name}", MetricSourceType.Workout)
        }
    }

    private suspend fun exerciseEvents(exerciseId: Long, metric: LinkSourceMetric): List<SourceEvent> {
        val gym = database.gymDao()
        val exercise = gym.getExercise(exerciseId) ?: return emptyList()
        val sessions = gym.getAllSessions().filter { it.state == "Finished" && !it.archived }.associateBy(WorkoutSessionEntity::id)
        val workoutExercises = gym.getAllWorkoutExercises().filter { it.exerciseId == exerciseId }
        val sets = gym.getAllWorkoutSets()
        return workoutExercises.mapNotNull { workoutExercise ->
            val session = sessions[workoutExercise.sessionId] ?: return@mapNotNull null
            val eligible = sets.filter { it.workoutExerciseId == workoutExercise.id && it.completed && it.deletedAtMillis == null }
            val valueDimension = when (metric) {
                LinkSourceMetric.EstimatedOneRepMax -> eligible.mapNotNull { rawEstimatedOneRepMax(it, exercise) }.maxOrNull()?.let { it to UnitDimension.Mass }
                LinkSourceMetric.MaxWeight -> eligible.mapNotNull(WorkoutSetEntity::canonicalWeightKg).maxOrNull()?.let { it to UnitDimension.Mass }
                LinkSourceMetric.Distance -> eligible.sumOf { it.canonicalDistanceMetres ?: 0.0 } to UnitDimension.Distance
                LinkSourceMetric.Repetitions, LinkSourceMetric.Count -> eligible.sumOf { it.repetitions ?: 0 }.toDouble() to UnitDimension.Count
                LinkSourceMetric.Duration -> eligible.sumOf { it.durationSeconds ?: 0L }.toDouble() to UnitDimension.Duration
                LinkSourceMetric.Volume -> eligible.sumOf { rawSetVolume(it, listOf(workoutExercise), mapOf(exerciseId to exercise)) } to UnitDimension.Custom
                else -> null
            } ?: return@mapNotNull null
            SourceEvent("exercise:${exercise.uuid}:workout:${session.uuid}:${metric.name}", LinkSourceType.Exercise, exerciseId,
                valueDimension.first, valueDimension.second, LocalDate.ofEpochDay(session.localEpochDay),
                Instant.ofEpochMilli(session.endedAtMillis ?: session.startedAtMillis), "${exercise.name}: ${metric.name}", MetricSourceType.Exercise)
        }
    }

    private suspend fun metricEvents(metricId: String): List<SourceEvent> {
        val metric = database.measurementDao().getMetric(metricId) ?: return emptyList()
        return database.measurementDao().getAllEntries().filter { it.metricId == metricId && it.status == MetricEntryStatus.Recorded.name && it.canonicalValue != null }
            .map { entry -> SourceEvent("metric-entry:${entry.id}", LinkSourceType.Metric, null, entry.canonicalValue,
                UnitDimension.valueOf(metric.dimension), LocalDate.ofEpochDay(entry.localEpochDay), Instant.ofEpochMilli(entry.timestampMillis),
                "${metric.name} measurement", MetricSourceType.valueOf(entry.sourceType)) }
    }

    private suspend fun rebuildTriggersLocked(sourceTypeNames: Set<String>? = null) {
        dao.getTriggerRules().filter { it.enabled && (sourceTypeNames == null || it.sourceType in sourceTypeNames) }.forEach { rule ->
            val events = triggerSourceEvents(rule)
                .filter { it.timestamp.toEpochMilli() >= rule.createdAtMillis }
            val retained = events.mapTo(mutableSetOf(), SourceEvent::id)
            val existing = dao.getTriggerOccurrences(rule.id).associateBy(TriggerOccurrenceEntity::sourceEventId)
            events.forEach { event ->
                if (event.id !in existing) {
                    dao.upsertTriggerOccurrence(
                        TriggerOccurrenceEntity(
                            triggerRuleId = rule.id,
                            sourceEventId = event.id,
                            availableAtMillis = quietHoursAdjusted(event.timestamp.plusSeconds(rule.delayMinutes.toLong() * 60), rule).toEpochMilli(),
                            deliveredAtMillis = null,
                            dismissedAtMillis = null,
                        ),
                    )
                }
            }
            val automaticallyLogsTargetHabit = rule.targetType == TriggerTargetType.Habit.name &&
                (rule.autoCompleteTargetHabit || rule.sourceType == LinkSourceType.Workout.name)
            if (automaticallyLogsTargetHabit) {
                syncGeneratedHabitLogs(rule, events)
            } else {
                removeGeneratedHabitLogs(rule, emptySet())
            }
            existing.values.filter { it.sourceEventId !in retained }.forEach { stale ->
                database.openHelper.writableDatabase.execSQL("DELETE FROM trigger_occurrences WHERE id = ?", arrayOf(stale.id))
            }
        }
    }

    private suspend fun triggerSourceEvents(rule: TriggerRuleEntity): List<SourceEvent> {
        val type = LinkSourceType.valueOf(rule.sourceType)
        val outcome = TriggerOutcome.valueOf(rule.outcome)
        if (type == LinkSourceType.Habit && outcome != TriggerOutcome.Completed) {
            val habit = database.habitDao().getHabit(rule.sourceEntityId) ?: return emptyList()
            val expected = if (outcome == TriggerOutcome.Failed) HabitLogStatus.Failed.name else HabitLogStatus.Skipped.name
            return database.habitDao().getAllLogs().filter { it.habitId == habit.id && it.status == expected }.map { log ->
                SourceEvent("habit-log:${log.uuid}:${outcome.name}", type, habit.id, null, UnitDimension.Count,
                    LocalDate.ofEpochDay(log.localEpochDay), Instant.ofEpochMilli(log.timestampMillis),
                    "${habit.name} ${outcome.name.lowercase()}", MetricSourceType.Habit)
            }
        }
        if (type == LinkSourceType.Task && outcome == TriggerOutcome.Skipped) {
            val task = database.taskDao().getTask(rule.sourceEntityId) ?: return emptyList()
            return database.taskDao().getOccurrences(task.id).filter { it.state == "Skipped" }.map { occurrence ->
                val timestamp = Instant.ofEpochMilli(task.updatedAtMillis)
                SourceEvent("task:${task.id}:${occurrence.originalEpochDay}:skipped", type, task.id, null, UnitDimension.Count,
                    LocalDate.ofEpochDay(occurrence.originalEpochDay), timestamp, "Skipped task: ${task.title}", MetricSourceType.Task)
            }
        }
        val metric = when (type) {
            LinkSourceType.Habit -> LinkSourceMetric.Success
            LinkSourceType.Task, LinkSourceType.Subtask, LinkSourceType.Workout -> LinkSourceMetric.Completion
            LinkSourceType.Exercise -> LinkSourceMetric.Count
            LinkSourceType.Metric -> LinkSourceMetric.NumericValue
        }
        return sourceEvents(type, rule.sourceEntityId, null, null, metric)
    }

    private suspend fun syncGeneratedHabitLogs(rule: TriggerRuleEntity, events: List<SourceEvent>) {
        val habit = database.habitDao().getHabit(rule.targetEntityId) ?: return
        val prefix = "trigger:${rule.uuid}:"
        val retained = events.mapTo(mutableSetOf()) { prefix + it.id }
        events.forEach { event ->
            val sourceId = prefix + event.id
            if (database.habitDao().getLogBySource(event.metricSourceType.name, sourceId) == null) {
                val logUuid = ids.nextId()
                val entryId = measurementRepository.record(
                    habit.metricId, 1.0, habit.unitId, timestamp = event.timestamp, localDate = event.date,
                    sourceType = event.metricSourceType, sourceId = sourceId, note = "Automatically logged by ${rule.name}",
                )
                val now = clock.now().toEpochMilli()
                database.habitDao().insertLog(
                    HabitLogEntity(uuid = logUuid, habitId = habit.id, value = 1.0, canonicalValue = BuiltInUnits.get(habit.unitId)?.toCanonical(1.0) ?: 1.0,
                        enteredUnitId = habit.unitId, status = HabitLogStatus.Success.name, timestampMillis = event.timestamp.toEpochMilli(),
                        localEpochDay = event.date.toEpochDay(), zoneId = clock.zoneId().id,
                        offsetSeconds = clock.zoneId().rules.getOffset(event.timestamp).totalSeconds, note = "Automatically logged by ${rule.name}",
                        sourceType = event.metricSourceType.name, sourceId = sourceId, metricEntryId = entryId, createdAtMillis = now, updatedAtMillis = now),
                )
            }
        }
        removeGeneratedHabitLogs(rule, retained)
    }

    private suspend fun removeGeneratedHabitLogs(rule: TriggerRuleEntity, retained: Set<String>) {
        val prefix = "trigger:${rule.uuid}:%"
        database.habitDao().getLogsBySourcePrefix(prefix).filter { it.sourceId !in retained }.forEach { log ->
            log.metricEntryId?.let { measurementRepository.deleteEntry(it) }
            database.habitDao().deleteLog(log.id)
        }
    }

    private suspend fun validateTrigger(draft: TriggerRuleDraft, excludingId: Long? = null) {
        require(draft.name.isNotBlank()) { "Trigger name is required" }
        require(draft.delayMinutes >= 0) { "Delay cannot be negative" }
        require(draft.sourceType != LinkSourceType.Workout || draft.outcome == TriggerOutcome.Completed) {
            "Workouts can trigger automation only when completed"
        }
        require(!(draft.sourceType.name == draft.targetType.name && draft.sourceEntityId == draft.targetEntityId)) { "A trigger cannot target itself" }
        val edges = dao.getTriggerRules().filter { it.id != excludingId && it.enabled }.map {
            "${it.sourceType}:${it.sourceEntityId}" to "${it.targetType}:${it.targetEntityId}"
        } + ("${draft.sourceType.name}:${draft.sourceEntityId}" to "${draft.targetType.name}:${draft.targetEntityId}")
        require(!hasDirectedCycle(edges)) { "This trigger would create a cycle" }
    }

    private fun quietHoursAdjusted(instant: Instant, rule: TriggerRuleEntity): Instant {
        val start = rule.quietStartMinutes ?: return instant
        val end = rule.quietEndMinutes ?: return instant
        val zone = clock.zoneId()
        val zoned = instant.atZone(zone)
        val minute = zoned.hour * 60 + zoned.minute
        val quiet = if (start <= end) minute in start until end else minute >= start || minute < end
        if (!quiet) return instant
        val date = if (start <= end || minute >= start) zoned.toLocalDate() else zoned.toLocalDate()
        var release = date.atStartOfDay(zone).plusMinutes(end.toLong())
        if (!release.isAfter(zoned)) release = release.plusDays(1)
        return release.toInstant()
    }
}

private data class SourceEvent(
    val id: String,
    val type: LinkSourceType,
    val entityId: Long?,
    val canonicalValue: Double?,
    val dimension: UnitDimension,
    val date: LocalDate,
    val timestamp: Instant,
    val explanation: String,
    val metricSourceType: MetricSourceType,
)

private fun rawSetVolume(set: WorkoutSetEntity, workoutExercises: List<WorkoutExerciseEntity>, exercises: Map<Long, ExerciseEntity>): Double {
    val workoutExercise = workoutExercises.firstOrNull { it.id == set.workoutExerciseId } ?: return 0.0
    val exercise = exercises[workoutExercise.exerciseId] ?: return 0.0
    if (!exercise.includeInVolume || set.classification == "WarmUp") return 0.0
    return (rawEffectiveLoad(set, exercise) ?: return 0.0) * (set.repetitions ?: 1)
}

private fun rawEffectiveLoad(set: WorkoutSetEntity, exercise: ExerciseEntity): Double? {
    val external = set.canonicalWeightKg ?: 0.0
    return when (exercise.trackingType) {
        "WeightReps", "WeightOnly", "WeightDuration" -> set.canonicalWeightKg
        "BodyweightReps" -> when (exercise.bodyweightLoadPolicy) {
            "ExternalWeightOnly" -> set.canonicalWeightKg
            "BodyweightPlusExternal" -> set.bodyweightKg?.plus(external)
            else -> set.bodyweightKg?.times(exercise.effectiveBodyweightPercent / 100.0)?.plus(external)
        }
        "AssistedBodyweightReps" -> set.bodyweightKg?.let { (it * exercise.effectiveBodyweightPercent / 100.0 - external).coerceAtLeast(0.0) }
        else -> null
    }
}

private fun rawEstimatedOneRepMax(set: WorkoutSetEntity, exercise: ExerciseEntity): Double? {
    if (!exercise.includeInPersonalRecords || set.classification == "WarmUp") return null
    return estimatedOneRepMax(rawEffectiveLoad(set, exercise) ?: return null, set.repetitions ?: return null,
        EstimatedOneRepMaxFormula.valueOf(exercise.oneRepMaxFormula))
}

private fun hasDirectedCycle(edges: List<Pair<String, String>>): Boolean {
    val graph = edges.groupBy({ it.first }, { it.second })
    val visiting = mutableSetOf<String>()
    val visited = mutableSetOf<String>()
    fun visit(node: String): Boolean {
        if (node in visiting) return true
        if (!visited.add(node)) return false
        visiting += node
        val cycle = graph[node].orEmpty().any(::visit)
        visiting -= node
        return cycle
    }
    return graph.keys.any(::visit)
}

private fun LinkRuleDraft.toEntity(uuid: String, createdAtMillis: Long) = LinkRuleEntity(
    uuid = uuid, name = name.trim(), kind = kind.name, sourceType = sourceType.name,
    sourceEntityId = sourceEntityId, sourceMetricId = sourceMetricId, sourceItemId = sourceItemId,
    sourceMetric = sourceMetric.name, targetGoalId = targetGoalId, targetMilestoneId = targetMilestoneId,
    valueMode = valueMode.name, fixedValue = fixedValue, multiplier = multiplier, offset = offset,
    retroactiveFromEpochDay = retroactiveFrom?.toEpochDay(), enabled = enabled,
    createdAtMillis = createdAtMillis, updatedAtMillis = createdAtMillis,
)

private fun LinkRuleEntity.toDomain() = LinkRule(id, uuid, name, LinkKind.valueOf(kind), LinkSourceType.valueOf(sourceType),
    sourceEntityId, sourceMetricId, sourceItemId, LinkSourceMetric.valueOf(sourceMetric), targetGoalId,
    targetMilestoneId, LinkValueMode.valueOf(valueMode), fixedValue, multiplier, offset,
    retroactiveFromEpochDay?.let(LocalDate::ofEpochDay), enabled, createdAtMillis, updatedAtMillis)

private fun ContributionEntity.toDomain() = Contribution(id, uuid, linkRuleId, sourceEventId, LinkSourceType.valueOf(sourceType),
    sourceEntityId, targetGoalId, metricEntryId, canonicalValue, LocalDate.ofEpochDay(localEpochDay),
    Instant.ofEpochMilli(timestampMillis), excluded, overrideValue, explanation, createdAtMillis, updatedAtMillis)

private fun TriggerRuleDraft.toEntity(uuid: String, createdAtMillis: Long) = TriggerRuleEntity(
    uuid = uuid, name = name.trim(), sourceType = sourceType.name, sourceEntityId = sourceEntityId,
    outcome = outcome.name, targetType = targetType.name, targetEntityId = targetEntityId,
    delayMinutes = delayMinutes, quietStartMinutes = quietStartMinutes, quietEndMinutes = quietEndMinutes,
    autoCompleteTargetHabit = autoCompleteTargetHabit, enabled = enabled, createdAtMillis = createdAtMillis, updatedAtMillis = createdAtMillis,
)

private fun TriggerRuleEntity.toDomain() = TriggerRule(id, uuid, name, LinkSourceType.valueOf(sourceType), sourceEntityId,
    TriggerOutcome.valueOf(outcome), TriggerTargetType.valueOf(targetType), targetEntityId, delayMinutes,
    quietStartMinutes, quietEndMinutes, autoCompleteTargetHabit, enabled, createdAtMillis, updatedAtMillis)

private fun TriggerOccurrenceEntity.toDomain() = TriggerOccurrence(id, triggerRuleId, sourceEventId,
    Instant.ofEpochMilli(availableAtMillis), deliveredAtMillis?.let(Instant::ofEpochMilli), dismissedAtMillis?.let(Instant::ofEpochMilli))
