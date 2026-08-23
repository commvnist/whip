package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalAggregationPeriod
import com.whip.app.domain.GoalConsistencyPeriod
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalMilestone
import com.whip.app.domain.GoalMilestoneDraft
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.MetricValueKind
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.compatibleAggregations
import com.whip.app.domain.defaultDirection
import com.whip.app.domain.withTypeSemantics
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface GoalRepository {
    val goals: Flow<List<Goal>>
    val milestones: Flow<List<GoalMilestone>>
    val metricEntries: Flow<List<MetricEntry>>

    suspend fun create(draft: GoalDraft): Long
    suspend fun update(id: Long, draft: GoalDraft)
    suspend fun duplicate(id: Long): Long
    suspend fun setStatus(id: Long, status: GoalStatus)
    suspend fun setPinned(id: Long, pinned: Boolean)
    suspend fun reorder(ids: List<Long>)
    suspend fun recordMeasurement(id: Long, value: Double, date: LocalDate? = null, timestamp: Instant? = null, note: String = ""): String
    suspend fun updateMeasurement(
        id: Long,
        entryId: String,
        value: Double,
        date: LocalDate,
        note: String = "",
        enteredUnitId: String? = null,
    )
    suspend fun deleteMeasurement(id: Long, entryId: String)
    suspend fun toggleMilestone(id: Long, completed: Boolean)
}

class RoomGoalRepository(
    private val database: WhipDatabase,
    private val measurementRepository: MeasurementRepository,
    private val clock: WhipClock,
    private val ids: WhipIdGenerator,
) : GoalRepository {
    private val dao = database.goalDao()
    private val areaRepository = RoomAreaRepository(database, clock, ids)
    override val goals = dao.observeGoals().map { it.map(GoalEntity::toDomain) }
    override val milestones = dao.observeMilestones().map { it.map(GoalMilestoneEntity::toDomain) }
    override val metricEntries = measurementRepository.entries

    override suspend fun create(draft: GoalDraft): Long = database.withTransaction {
        val semanticDraft = draft.withTypeSemantics()
        validateGoal(semanticDraft)
        val area = areaRepository.resolve(semanticDraft.areaId, semanticDraft.area)
        val resolvedDraft = semanticDraft.copy(areaId = area.id, area = area.name)
        measurementRepository.ensureCustomUnit(semanticDraft.unitId, semanticDraft.unitId, semanticDraft.unitId, semanticDraft.dimension)
        val unit = requireNotNull(resolveUnit(semanticDraft.unitId)) { "Unknown goal unit" }
        val metricId = measurementRepository.createMetric(
            name = semanticDraft.name,
            valueKind = MetricValueKind.Decimal,
            dimension = semanticDraft.dimension,
            defaultUnitId = semanticDraft.unitId,
            precision = semanticDraft.precision,
        )
        val now = clock.now().toEpochMilli()
        val goalId = dao.insertGoal(
            resolvedDraft.toEntity(ids.nextId(), metricId, dao.nextPosition(), now, unit = unit),
        )
        syncMilestones(goalId, semanticDraft.milestones, now)
        goalId
    }

    override suspend fun update(id: Long, draft: GoalDraft) = database.withTransaction {
        val semanticDraft = draft.withTypeSemantics()
        validateGoal(semanticDraft)
        val area = areaRepository.resolve(semanticDraft.areaId, semanticDraft.area)
        val resolvedDraft = semanticDraft.copy(areaId = area.id, area = area.name)
        measurementRepository.ensureCustomUnit(semanticDraft.unitId, semanticDraft.unitId, semanticDraft.unitId, semanticDraft.dimension)
        val unit = requireNotNull(resolveUnit(semanticDraft.unitId)) { "Unknown goal unit" }
        val existing = dao.getGoal(id) ?: error("Goal no longer exists")
        require(existing.dimension == semanticDraft.dimension.name) {
            "A goal's measurement dimension cannot change; create a new goal instead"
        }
        val now = clock.now().toEpochMilli()
        dao.updateGoal(
            resolvedDraft.toEntity(
                uuid = existing.uuid,
                metricId = existing.metricId,
                position = existing.position,
                createdAtMillis = existing.createdAtMillis,
                id = existing.id,
                status = GoalStatus.valueOf(existing.status),
                pinned = existing.pinned,
                updatedAtMillis = now,
                unit = unit,
            ),
        )
        syncMilestones(id, semanticDraft.milestones, now)
    }

    override suspend fun duplicate(id: Long): Long {
        val goal = dao.getGoal(id)?.toDomain() ?: error("Goal no longer exists")
        val milestones = dao.getMilestones(id).map {
            GoalMilestoneDraft(name = it.name, weight = it.weight, reward = it.reward)
        }
        return create(goal.toDraft(milestones, resolveUnit(goal.unitId)).copy(name = "${goal.name} copy"))
    }

    override suspend fun setStatus(id: Long, status: GoalStatus) = database.withTransaction {
        val current = dao.getGoal(id) ?: return@withTransaction
        val now = clock.now().toEpochMilli()
        dao.updateGoal(current.copy(status = status.name, updatedAtMillis = now))
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        val goal = dao.getGoal(id) ?: return
        dao.updateGoal(goal.copy(pinned = pinned, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun reorder(ids: List<Long>) = database.withTransaction {
        ids.forEachIndexed { index, id ->
            dao.getGoal(id)?.let { dao.updateGoal(it.copy(position = index, updatedAtMillis = clock.now().toEpochMilli())) }
        }
    }

    override suspend fun recordMeasurement(
        id: Long,
        value: Double,
        date: LocalDate?,
        timestamp: Instant?,
        note: String,
    ): String {
        val goal = dao.getGoal(id)?.toDomain() ?: error("Goal no longer exists")
        return measurementRepository.record(
            metricId = goal.metricId,
            value = value,
            unitId = goal.unitId,
            timestamp = timestamp,
            localDate = date,
            zoneId = clock.zoneId(),
            sourceType = MetricSourceType.Goal,
            sourceId = goal.uuid,
            note = note,
        )
    }

    override suspend fun updateMeasurement(
        id: Long,
        entryId: String,
        value: Double,
        date: LocalDate,
        note: String,
        enteredUnitId: String?,
    ) {
        val goal = dao.getGoal(id)?.toDomain() ?: error("Goal no longer exists")
        val entry = database.measurementDao().getEntry(entryId) ?: error("Measurement no longer exists")
        require(entry.metricId == goal.metricId) { "Measurement does not belong to this goal" }
        measurementRepository.record(
            metricId = goal.metricId,
            value = value,
            unitId = enteredUnitId ?: entry.enteredUnitId ?: goal.unitId,
            timestamp = Instant.ofEpochMilli(entry.timestampMillis),
            localDate = date,
            zoneId = ZoneId.of(entry.zoneId),
            sourceType = MetricSourceType.valueOf(entry.sourceType),
            sourceId = entry.sourceId,
            note = note,
            existingEntryId = entryId,
        )
    }

    override suspend fun deleteMeasurement(id: Long, entryId: String) {
        val goal = dao.getGoal(id) ?: error("Goal no longer exists")
        val entry = database.measurementDao().getEntry(entryId) ?: return
        require(entry.metricId == goal.metricId) { "Measurement does not belong to this goal" }
        val linked = database.linkDao().observeContributionsSnapshot().any { it.metricEntryId == entryId }
        require(!linked) { "Exclude a linked contribution from the link history instead" }
        measurementRepository.deleteEntry(entryId)
    }

    override suspend fun toggleMilestone(id: Long, completed: Boolean) {
        val milestone = dao.getMilestone(id) ?: return
        val now = clock.now().toEpochMilli()
        dao.updateMilestone(
            milestone.copy(
                completed = completed,
                completedAtMillis = now.takeIf { completed },
                updatedAtMillis = now,
            ),
        )
    }

    private suspend fun syncMilestones(goalId: Long, drafts: List<GoalMilestoneDraft>, now: Long) {
        val existing = dao.getMilestones(goalId)
        val existingById = existing.associateBy(GoalMilestoneEntity::id)
        val existingByUuid = existing.associateBy(GoalMilestoneEntity::uuid)
        val retainedIds = mutableSetOf<Long>()
        drafts.filter { it.name.isNotBlank() }.forEachIndexed { index, draft ->
            val current = draft.id?.let(existingById::get)
                ?: draft.uuid?.let(existingByUuid::get)
            if (current == null) {
                dao.insertMilestone(
                    GoalMilestoneEntity(
                        uuid = ids.nextId(), goalId = goalId, name = draft.name.trim(),
                        position = index, weight = draft.weight.coerceAtLeast(0.0),
                        completed = false, completedAtMillis = null, reward = draft.reward.trim(),
                        createdAtMillis = now, updatedAtMillis = now,
                    ),
                )
            } else {
                retainedIds += current.id
                dao.updateMilestone(
                    current.copy(
                        name = draft.name.trim(), position = index,
                        weight = draft.weight.coerceAtLeast(0.0),
                        reward = draft.reward.trim(), updatedAtMillis = now,
                    ),
                )
            }
        }
        existing.filterNot { it.id in retainedIds }.forEach { dao.deleteMilestone(it.id) }
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
}

private fun validateGoal(draft: GoalDraft) {
    require(draft.name.isNotBlank()) { "Goal name is required" }
    require(draft.deadline == null || !draft.deadline.isBefore(draft.startDate)) { "Deadline cannot precede the start date" }
    require(listOfNotNull(draft.baseline, draft.targetMin, draft.targetMax).all(Double::isFinite)) { "Goal values must be finite numbers" }
    require(draft.aggregation in draft.type.compatibleAggregations()) { "Goal calculation does not match its type" }
    require(draft.direction == draft.type.defaultDirection()) { "Goal direction does not match its type" }
    require(draft.paceType == GoalPaceType.None || draft.deadline != null) { "Pace guidance requires a deadline" }
    when (draft.type) {
        GoalType.ReachValue, GoalType.ReduceValue, GoalType.AccumulateTotal, GoalType.MeetAverage ->
            require(draft.targetMin?.isFinite() == true) { "Enter a target" }
        GoalType.MaintainRange -> require(
            draft.targetMin?.isFinite() == true && draft.targetMax?.isFinite() == true && draft.targetMin <= draft.targetMax,
        ) { "Enter a valid goal range" }
        GoalType.WeightedMilestones -> {
            require(draft.milestones.any { it.name.isNotBlank() }) { "Add at least one milestone" }
            require(draft.milestones.all { it.weight.isFinite() && it.weight >= 0.0 }) { "Milestone weights must be non-negative numbers" }
            require(draft.milestones.any { it.weight > 0.0 }) { "At least one milestone must have a positive weight" }
        }
        GoalType.Consistency, GoalType.OpenEndedTrend -> Unit
    }
    if (draft.aggregationPeriod == GoalAggregationPeriod.RollingDays) require((draft.rollingDays ?: 0) > 0) { "Enter a positive rolling window" }
    if (draft.type == GoalType.Consistency) {
        require((draft.targetMin ?: 0.0) > 0.0) { "Enter a per-period consistency target" }
        require((draft.consistencyRequiredPeriods ?: 0) > 0) { "Enter how many periods the goal should cover" }
    }
}

private fun GoalDraft.toEntity(
    uuid: String, metricId: String, position: Int, createdAtMillis: Long,
    id: Long = 0, status: GoalStatus = GoalStatus.Active, pinned: Boolean = false,
    updatedAtMillis: Long = createdAtMillis,
    unit: UnitDefinition,
): GoalEntity {
    fun canonical(value: Double?) = value?.let(unit::toCanonical)
    return GoalEntity(
        id = id, uuid = uuid, metricId = metricId, name = name.trim(), description = description.trim(),
        areaId = areaId, area = area.trim(), tagsCsv = tags.map(String::trim).filter(String::isNotBlank).distinct().joinToString(","),
        icon = icon, type = type.name, dimension = dimension.name,
        unitId = unitId, precision = precision, baseline = canonical(baseline),
        targetMin = canonical(targetMin), targetMax = canonical(targetMax), direction = direction.name,
        startEpochDay = startDate.toEpochDay(), deadlineEpochDay = deadline?.toEpochDay(),
        aggregation = aggregation.name, aggregationPeriod = aggregationPeriod.name,
        rollingDays = rollingDays, paceType = paceType.name,
        consistencyPeriod = consistencyPeriod.name,
        consistencyRequiredPeriods = consistencyRequiredPeriods, reminderMinutes = reminderMinutes,
        status = status.name, pinned = pinned, position = position,
        createdAtMillis = createdAtMillis, updatedAtMillis = updatedAtMillis,
    )
}

private fun GoalEntity.toDomain() = Goal(
    id = id, uuid = uuid, metricId = metricId, name = name, description = description,
    areaId = areaId, area = area, tags = tagsCsv.split(',').map(String::trim).filter(String::isNotBlank),
    icon = icon, type = GoalType.valueOf(type),
    dimension = UnitDimension.valueOf(dimension), unitId = unitId, precision = precision,
    baseline = baseline, targetMin = targetMin, targetMax = targetMax,
    direction = GoalDirection.valueOf(direction), startDate = LocalDate.ofEpochDay(startEpochDay),
    deadline = deadlineEpochDay?.let(LocalDate::ofEpochDay), aggregation = GoalAggregation.valueOf(aggregation),
    paceType = GoalPaceType.valueOf(paceType),
    reminderMinutes = reminderMinutes, status = GoalStatus.valueOf(status), pinned = pinned,
    position = position, createdAtMillis = createdAtMillis, updatedAtMillis = updatedAtMillis,
    aggregationPeriod = GoalAggregationPeriod.valueOf(aggregationPeriod), rollingDays = rollingDays,
    consistencyPeriod = GoalConsistencyPeriod.valueOf(consistencyPeriod),
    consistencyRequiredPeriods = consistencyRequiredPeriods,
)
private fun GoalMilestoneEntity.toDomain() = GoalMilestone(id, uuid, goalId, name, position, weight, completed, completedAtMillis, reward, createdAtMillis, updatedAtMillis)

private fun Goal.toDraft(milestones: List<GoalMilestoneDraft>, unit: UnitDefinition?): GoalDraft {
    fun display(value: Double?) = value?.let { unit?.fromCanonical(it) ?: it }
    return GoalDraft(
        name = name, description = description, areaId = areaId, area = area, tags = tags, icon = icon,
        type = type, dimension = dimension, unitId = unitId,
        precision = precision, baseline = display(baseline), targetMin = display(targetMin),
        targetMax = display(targetMax), direction = direction, startDate = startDate,
        deadline = deadline, aggregation = aggregation,
        paceType = paceType, reminderMinutes = reminderMinutes, milestones = milestones,
        aggregationPeriod = aggregationPeriod, rollingDays = rollingDays,
        consistencyPeriod = consistencyPeriod,
        consistencyRequiredPeriods = consistencyRequiredPeriods,
    )
}
