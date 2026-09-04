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
import com.whip.app.domain.GoalClosureSnapshot
import com.whip.app.domain.GoalElapsedResetEvent
import com.whip.app.domain.GoalEligibilityBoundary
import com.whip.app.domain.GoalMeasurementBoundary
import com.whip.app.domain.GoalMilestone
import com.whip.app.domain.GoalMilestoneBoundary
import com.whip.app.domain.GoalMilestoneDraft
import com.whip.app.domain.GoalMutationBoundary
import com.whip.app.domain.GoalProgressBoundary
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.DEFAULT_GOAL_EMOJI
import com.whip.app.domain.normalizedIdentityEmoji
import com.whip.app.domain.MeasurementEntry
import com.whip.app.domain.MeasurementSourceType
import com.whip.app.domain.MeasurementValueKind
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.validationErrors
import com.whip.app.domain.withTypeSemantics
import com.whip.app.domain.eligibilityBoundary
import com.whip.app.domain.measurementBoundary
import com.whip.app.domain.milestoneBoundary
import com.whip.app.domain.mutationBoundary
import com.whip.app.domain.projectGoal
import com.whip.app.domain.progressBoundary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface GoalRepository {
    val goals: Flow<List<Goal>>
    val milestones: Flow<List<GoalMilestone>>
    val measurementEntries: Flow<List<MeasurementEntry>>
    val closureSnapshots: Flow<List<GoalClosureSnapshot>>
    val elapsedResetEvents: Flow<List<GoalElapsedResetEvent>>

    suspend fun get(id: Long): Goal?
    suspend fun create(draft: GoalDraft): Long
    suspend fun update(id: Long, draft: GoalDraft)
    suspend fun update(boundary: GoalMutationBoundary, draft: GoalDraft)
    suspend fun duplicate(id: Long): Long
    suspend fun duplicate(boundary: GoalMutationBoundary): Long
    suspend fun setStatus(id: Long, status: GoalStatus)
    suspend fun setStatus(boundary: GoalMutationBoundary, status: GoalStatus)
    suspend fun setArchived(boundary: GoalMutationBoundary, archived: Boolean)
    suspend fun setPinned(id: Long, pinned: Boolean)
    suspend fun setPinned(boundary: GoalMutationBoundary, pinned: Boolean)
    suspend fun reorder(ids: List<Long>)
    suspend fun recordMeasurement(id: Long, value: Double, date: LocalDate? = null, timestamp: Instant? = null, note: String = ""): String
    suspend fun recordMeasurement(boundary: GoalProgressBoundary, value: Double, date: LocalDate? = null, timestamp: Instant? = null, note: String = ""): String
    suspend fun updateMeasurement(
        id: Long,
        entryId: String,
        value: Double,
        date: LocalDate,
        note: String = "",
        enteredUnitId: String? = null,
    )
    suspend fun updateMeasurement(
        boundary: GoalMeasurementBoundary,
        value: Double,
        date: LocalDate,
        note: String = "",
        enteredUnitId: String? = null,
    )
    suspend fun deleteMeasurement(id: Long, entryId: String)
    suspend fun deleteMeasurement(boundary: GoalMeasurementBoundary)
    suspend fun toggleMilestone(id: Long, completed: Boolean)
    suspend fun toggleMilestone(boundary: GoalMilestoneBoundary, completed: Boolean)
    suspend fun resetElapsedStart(id: Long, start: Instant)
    suspend fun resetElapsedStart(boundary: GoalMutationBoundary, start: Instant)
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
    override val measurementEntries = measurementRepository.entries
    override val closureSnapshots = dao.observeClosureSnapshots().map { list -> list.map(GoalClosureSnapshotEntity::toDomain) }
    override val elapsedResetEvents = dao.observeElapsedResetEvents().map { list -> list.map(GoalElapsedResetEventEntity::toDomain) }

    override suspend fun get(id: Long): Goal? = dao.getGoal(id)?.toDomain()

    override suspend fun create(draft: GoalDraft): Long = database.withTransaction {
        val semanticDraft = draft.withTypeSemantics()
        semanticDraft.requireValid(clock.now().toEpochMilli())
        val area = areaRepository.resolve(semanticDraft.areaId, semanticDraft.area)
        val resolvedDraft = semanticDraft.copy(areaId = area.id, area = area.name)
        measurementRepository.ensureCustomUnit(semanticDraft.unitId, semanticDraft.unitId, semanticDraft.unitId, semanticDraft.dimension)
        val unit = requireNotNull(resolveUnit(semanticDraft.unitId)) { "Unknown goal unit" }
        val measurementId = measurementRepository.createMeasurement(
            name = semanticDraft.name,
            valueKind = MeasurementValueKind.Decimal,
            dimension = semanticDraft.dimension,
            defaultUnitId = semanticDraft.unitId,
            precision = semanticDraft.precision,
        )
        val now = clock.now().toEpochMilli()
        val goalId = dao.insertGoal(
            resolvedDraft.toEntity(ids.nextId(), measurementId, dao.nextPosition(), now, unit = unit),
        )
        syncMilestones(goalId, semanticDraft.milestones, now)
        goalId
    }

    override suspend fun update(id: Long, draft: GoalDraft) {
        val boundary = get(id)?.mutationBoundary() ?: error("Goal no longer exists")
        update(boundary, draft)
    }

    override suspend fun update(boundary: GoalMutationBoundary, draft: GoalDraft) = database.withTransaction {
        val semanticDraft = draft.withTypeSemantics()
        semanticDraft.requireValid(clock.now().toEpochMilli())
        val area = areaRepository.resolve(semanticDraft.areaId, semanticDraft.area)
        val resolvedDraft = semanticDraft.copy(areaId = area.id, area = area.name)
        measurementRepository.ensureCustomUnit(semanticDraft.unitId, semanticDraft.unitId, semanticDraft.unitId, semanticDraft.dimension)
        val unit = requireNotNull(resolveUnit(semanticDraft.unitId)) { "Unknown goal unit" }
        val existing = requireCurrent(boundary)
        require(existing.dimension == semanticDraft.dimension.name) {
            "A goal's measurement dimension cannot change; create a new goal instead"
        }
        val now = clock.now().toEpochMilli()
        dao.updateGoal(
            resolvedDraft.toEntity(
                uuid = existing.uuid,
                measurementId = existing.measurementId,
                position = existing.position,
                createdAtMillis = existing.createdAtMillis,
                id = existing.id,
                status = existing.lifecycleStatus(),
                archived = existing.archived,
                pinned = existing.pinned,
                updatedAtMillis = now,
                unit = unit,
            ),
        )
        val existingMilestones = dao.getMilestones(existing.id)
        if (!existingMilestones.matches(semanticDraft.milestones)) {
            syncMilestones(existing.id, semanticDraft.milestones, now)
        }
    }

    override suspend fun duplicate(id: Long): Long {
        val boundary = get(id)?.mutationBoundary() ?: error("Goal no longer exists")
        return duplicate(boundary)
    }

    override suspend fun duplicate(boundary: GoalMutationBoundary): Long = database.withTransaction {
        val current = requireCurrent(boundary)
        val goal = current.toDomain()
        val milestoneDrafts = dao.getMilestones(current.id).map {
            GoalMilestoneDraft(name = it.name, weight = it.weight, reward = it.reward)
        }
        create(goal.toDraft(milestoneDrafts, resolveUnit(goal.unitId)).copy(name = "${goal.name} copy"))
    }

    override suspend fun setStatus(id: Long, status: GoalStatus) {
        val boundary = get(id)?.mutationBoundary() ?: error("Goal no longer exists")
        if (status == GoalStatus.Archived) setArchived(boundary, true) else setStatus(boundary, status)
    }

    override suspend fun setStatus(boundary: GoalMutationBoundary, status: GoalStatus) {
        if (status == GoalStatus.Archived) {
            setArchived(boundary, true)
            return
        }
        database.withTransaction {
            val current = requireCurrent(boundary)
            val currentStatus = current.lifecycleStatus()
            if (currentStatus == status) return@withTransaction
            require(status != GoalStatus.Archived) { "Archive visibility is not a lifecycle status" }
            val now = clock.now().toEpochMilli()
            if (status in setOf(GoalStatus.Completed, GoalStatus.Abandoned)) {
                insertClosureSnapshot(current, status, now)
            }
            dao.updateGoal(current.copy(status = status.name, updatedAtMillis = now))
        }
    }

    override suspend fun setArchived(boundary: GoalMutationBoundary, archived: Boolean) = database.withTransaction {
        val current = requireCurrent(boundary)
        if (current.archived == archived) return@withTransaction
        val now = clock.now().toEpochMilli()
        dao.updateGoal(current.copy(archived = archived, updatedAtMillis = now))
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        val boundary = get(id)?.mutationBoundary() ?: error("Goal no longer exists")
        setPinned(boundary, pinned)
    }

    override suspend fun setPinned(boundary: GoalMutationBoundary, pinned: Boolean) = database.withTransaction {
        val current = requireCurrent(boundary)
        if (current.pinned == pinned) return@withTransaction
        if (pinned) {
            require(!current.archived && current.lifecycleStatus() in setOf(GoalStatus.Active, GoalStatus.Paused)) {
                "Only open, unarchived Goals can be pinned to Whip Home"
            }
        }
        dao.updateGoal(current.copy(pinned = pinned, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun reorder(ids: List<Long>) = database.withTransaction {
        val requested = ids.distinct()
        val all = dao.getAllGoals()
        require(requested.all { id -> all.any { it.id == id } }) { "Goal no longer exists" }
        val byId = all.associateBy(GoalEntity::id)
        val order = requested + all.filterNot { it.id in requested }.sortedBy(GoalEntity::position).map(GoalEntity::id)
        val now = clock.now().toEpochMilli()
        order.forEachIndexed { index, id ->
            val current = requireNotNull(byId[id])
            if (current.position != index) dao.updateGoal(current.copy(position = index, updatedAtMillis = now))
        }
    }

    override suspend fun recordMeasurement(
        id: Long,
        value: Double,
        date: LocalDate?,
        timestamp: Instant?,
        note: String,
    ): String = database.withTransaction {
        val goal = dao.getGoal(id)?.toDomain() ?: error("Goal no longer exists")
        recordMeasurement(goal.progressBoundary(), value, date, timestamp, note)
    }

    override suspend fun recordMeasurement(
        boundary: GoalProgressBoundary,
        value: Double,
        date: LocalDate?,
        timestamp: Instant?,
        note: String,
    ): String = database.withTransaction {
        val goal = requireCurrent(boundary).toDomain()
        require(!goal.archived && goal.status == GoalStatus.Active) { "Only active Goals can record progress" }
        require(goal.type !in setOf(GoalType.ElapsedSince, GoalType.WeightedMilestones)) {
            "This Goal type does not accept measurement progress"
        }
        require(value.isFinite()) { "Measurement value must be a finite number" }
        require(date == null || !date.isAfter(clock.today())) { "Measurement date cannot be in the future" }
        require(timestamp == null || !timestamp.isAfter(clock.now())) { "Measurement time cannot be in the future" }
        measurementRepository.record(
            measurementId = goal.measurementId,
            value = value,
            unitId = goal.unitId,
            timestamp = timestamp,
            localDate = date,
            zoneId = clock.zoneId(),
            sourceType = MeasurementSourceType.Goal,
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
    ) = database.withTransaction {
        val goal = dao.getGoal(id)?.toDomain() ?: error("Goal no longer exists")
        val entry = database.measurementDao().getEntry(entryId)?.toDomain() ?: error("Measurement no longer exists")
        updateMeasurement(goal.measurementBoundary(entry), value, date, note, enteredUnitId)
        Unit
    }

    override suspend fun updateMeasurement(
        boundary: GoalMeasurementBoundary,
        value: Double,
        date: LocalDate,
        note: String,
        enteredUnitId: String?,
    ) = database.withTransaction {
        val goal = requireCurrent(boundary.goal).toDomain()
        // A progress entry is historical evidence and may be corrected after a
        // Goal closes or is archived. The immutable closure snapshot remains
        // unchanged; the exact opening boundary still rejects concurrent edits.
        require(goal.type != GoalType.ElapsedSince) { "Elapsed-time Goals do not accept measurements" }
        require(value.isFinite()) { "Measurement value must be a finite number" }
        require(!date.isAfter(clock.today())) { "Measurement date cannot be in the future" }
        val entryEntity = database.measurementDao().getEntry(boundary.entryId)
            ?: error("Measurement no longer exists")
        val entry = entryEntity.toDomain()
        require(boundary == goal.measurementBoundary(entry)) { "Measurement changed; reopen it before saving" }
        requireEditableGoalEntry(goal, entryEntity)
        measurementRepository.record(
            measurementId = goal.measurementId,
            value = value,
            unitId = enteredUnitId ?: entryEntity.enteredUnitId ?: goal.unitId,
            timestamp = Instant.ofEpochMilli(entryEntity.timestampMillis),
            localDate = date,
            zoneId = ZoneId.of(entryEntity.zoneId),
            sourceType = MeasurementSourceType.valueOf(entryEntity.sourceType),
            sourceId = entryEntity.sourceId,
            note = note,
            existingEntryId = entryEntity.id,
        )
        Unit
    }

    override suspend fun deleteMeasurement(id: Long, entryId: String) = database.withTransaction {
        val goal = dao.getGoal(id)?.toDomain() ?: error("Goal no longer exists")
        val entry = database.measurementDao().getEntry(entryId)?.toDomain() ?: error("Measurement no longer exists")
        deleteMeasurement(goal.measurementBoundary(entry))
    }

    override suspend fun deleteMeasurement(boundary: GoalMeasurementBoundary) = database.withTransaction {
        val goal = requireCurrent(boundary.goal).toDomain()
        val entryEntity = database.measurementDao().getEntry(boundary.entryId)
            ?: error("Measurement no longer exists")
        val entry = entryEntity.toDomain()
        require(boundary == goal.measurementBoundary(entry)) { "Measurement changed; reopen it before removing it" }
        requireEditableGoalEntry(goal, entryEntity)
        measurementRepository.deleteEntry(entryEntity.id)
    }

    override suspend fun toggleMilestone(id: Long, completed: Boolean) = database.withTransaction {
        val milestone = dao.getMilestone(id) ?: error("Milestone no longer exists")
        val goal = dao.getGoal(milestone.goalId)?.toDomain() ?: error("Goal no longer exists")
        toggleMilestone(goal.milestoneBoundary(milestone.toDomain()), completed)
    }

    override suspend fun toggleMilestone(boundary: GoalMilestoneBoundary, completed: Boolean) = database.withTransaction {
        val goal = requireCurrent(boundary.goal).toDomain()
        require(!goal.archived && goal.status == GoalStatus.Active) { "Only active Goals can change milestones" }
        require(goal.type == GoalType.WeightedMilestones) { "Only milestone Goals can change milestones" }
        val milestone = dao.getMilestone(boundary.milestoneId) ?: error("Milestone no longer exists")
        require(milestone.goalId == goal.id && milestone.uuid == boundary.milestoneUuid) {
            "Milestone does not belong to this goal"
        }
        require(boundary == goal.milestoneBoundary(milestone.toDomain())) {
            "Milestone changed; reopen it before saving"
        }
        if (milestone.completed == completed) return@withTransaction
        val now = clock.now().toEpochMilli()
        dao.updateMilestone(
            milestone.copy(
                completed = completed,
                completedAtMillis = now.takeIf { completed },
                updatedAtMillis = now,
            ),
        )
    }

    override suspend fun resetElapsedStart(id: Long, start: Instant) = database.withTransaction {
        val goal = dao.getGoal(id)?.toDomain() ?: error("Goal no longer exists")
        resetElapsedStart(goal.mutationBoundary(), start)
    }

    override suspend fun resetElapsedStart(boundary: GoalMutationBoundary, start: Instant) = database.withTransaction {
        val current = requireCurrent(boundary)
        require(!current.archived && current.lifecycleStatus() == GoalStatus.Active) {
            "Only active Goals can reset elapsed time"
        }
        require(GoalType.valueOf(current.type) == GoalType.ElapsedSince) { "Only elapsed-time Goals can reset their start" }
        val resetAt = clock.now()
        require(!start.isAfter(resetAt)) { "Start time cannot be in the future" }
        val previousStart = current.elapsedStartMillis ?: current.startEpochDay
            .let(LocalDate::ofEpochDay)
            .atStartOfDay(clock.zoneId())
            .toInstant()
            .toEpochMilli()
        if (previousStart == start.toEpochMilli()) return@withTransaction
        dao.insertElapsedResetEvent(
            GoalElapsedResetEventEntity(
                uuid = ids.nextId(),
                goalId = current.id,
                goalUuid = current.uuid,
                previousStartMillis = previousStart,
                newStartMillis = start.toEpochMilli(),
                resetAtMillis = resetAt.toEpochMilli(),
                elapsedDurationMillis = (resetAt.toEpochMilli() - previousStart).coerceAtLeast(0L),
            ),
        )
        dao.updateGoal(
            current.copy(
                startEpochDay = start.atZone(clock.zoneId()).toLocalDate().toEpochDay(),
                elapsedStartMillis = start.toEpochMilli(),
                updatedAtMillis = resetAt.toEpochMilli(),
            ),
        )
    }

    private suspend fun requireCurrent(boundary: GoalMutationBoundary): GoalEntity {
        val current = dao.getGoal(boundary.goalId) ?: error("Goal no longer exists")
        require(current.uuid == boundary.goalUuid) { "Goal identity no longer matches" }
        require(current.toDomain().mutationBoundary() == boundary) {
            "Goal changed; reopen it before saving"
        }
        return current
    }

    private suspend fun requireCurrent(boundary: GoalProgressBoundary): GoalEntity {
        val current = dao.getGoal(boundary.goalId) ?: error("Goal no longer exists")
        require(current.uuid == boundary.goalUuid && current.measurementId == boundary.measurementId) {
            "Goal identity no longer matches"
        }
        require(current.toDomain().progressBoundary() == boundary) {
            "Goal progress settings changed; reopen it before saving"
        }
        return current
    }

    private suspend fun requireCurrent(boundary: GoalEligibilityBoundary): GoalEntity {
        val current = dao.getGoal(boundary.goalId) ?: error("Goal no longer exists")
        val goal = current.toDomain()
        require(current.uuid == boundary.goalUuid) { "Goal identity no longer matches" }
        require(goal.eligibilityBoundary() == boundary) {
            "Goal lifecycle changed; reopen it before saving"
        }
        return current
    }

    private suspend fun requireEditableGoalEntry(goal: Goal, entry: MeasurementEntryEntity) {
        require(entry.measurementId == goal.measurementId) { "Measurement does not belong to this goal" }
        require(entry.status == com.whip.app.domain.MeasurementEntryStatus.Recorded.name) {
            "Only recorded progress can be edited"
        }
        val owned = when (MeasurementSourceType.valueOf(entry.sourceType)) {
            MeasurementSourceType.Goal -> entry.sourceId == goal.uuid
            // Manually authored Goal progress has no external source identity.
            MeasurementSourceType.Manual -> entry.sourceId == null
            else -> false
        }
        require(owned) { "Linked or externally recorded progress must be edited at its source" }
    }

    private suspend fun insertClosureSnapshot(current: GoalEntity, status: GoalStatus, now: Long) {
        val goal = current.toDomain().copy(status = status)
        val entries = database.measurementDao().getEntriesForMeasurement(current.measurementId).map(MeasurementEntryEntity::toDomain)
        val milestones = dao.getMilestones(current.id).map(GoalMilestoneEntity::toDomain)
        val projection = projectGoal(goal, entries, milestones, clock.today())
        val elapsedDurationMillis = goal.elapsedStartMillis
            ?.takeIf { goal.type == GoalType.ElapsedSince }
            ?.let { started -> (now - started).coerceAtLeast(0L) }
        dao.insertClosureSnapshot(
            GoalClosureSnapshotEntity(
                uuid = ids.nextId(),
                goalId = current.id,
                completedAtMillis = now,
                value = projection.currentValue?.takeIf(Double::isFinite),
                progress = projection.progress?.takeIf(Double::isFinite),
                status = status.name,
                elapsedDurationMillis = elapsedDurationMillis,
                completedMilestoneCount = milestones.count(GoalMilestone::completed)
                    .takeIf { goal.type == GoalType.WeightedMilestones },
                totalMilestoneCount = milestones.size.takeIf { goal.type == GoalType.WeightedMilestones },
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

private fun List<GoalMilestoneEntity>.matches(drafts: List<GoalMilestoneDraft>): Boolean {
    val normalized = drafts.filter { it.name.isNotBlank() }
    if (size != normalized.size) return false
    return zip(normalized).withIndex().all { (index, pair) ->
        val (stored, draft) = pair
        stored.name == draft.name.trim() &&
            stored.position == index &&
            stored.weight == draft.weight.coerceAtLeast(0.0) &&
            stored.reward == draft.reward.trim()
    }
}

private fun GoalDraft.requireValid(nowMillis: Long) {
    val problems = validationErrors(nowMillis)
    require(problems.isEmpty()) { problems.first() }
}

private fun GoalDraft.toEntity(
    uuid: String, measurementId: String, position: Int, createdAtMillis: Long,
    id: Long = 0, status: GoalStatus = GoalStatus.Active, pinned: Boolean = false,
    archived: Boolean = false,
    updatedAtMillis: Long = createdAtMillis,
    unit: UnitDefinition,
): GoalEntity {
    fun canonical(value: Double?) = value?.let(unit::toCanonical)?.also {
        require(it.isFinite()) { "Converted goal value must be finite" }
    }
    return GoalEntity(
        id = id, uuid = uuid, measurementId = measurementId, name = name.trim(), description = description.trim(),
        areaId = areaId, area = area.trim(), tagsCsv = tags.map(String::trim).filter(String::isNotBlank).distinct().joinToString(","),
        icon = icon.normalizedIdentityEmoji(DEFAULT_GOAL_EMOJI), type = type.name, dimension = dimension.name,
        unitId = unitId, precision = precision, baseline = canonical(baseline),
        targetMin = canonical(targetMin), targetMax = canonical(targetMax), direction = direction.name,
        startEpochDay = startDate.toEpochDay(), deadlineEpochDay = deadline?.toEpochDay(),
        aggregation = aggregation.name, aggregationPeriod = aggregationPeriod.name,
        rollingDays = rollingDays, paceType = paceType.name,
        consistencyPeriod = consistencyPeriod.name,
        consistencyRequiredPeriods = consistencyRequiredPeriods, reminderMinutes = reminderMinutes,
        elapsedStartMillis = elapsedStartMillis,
        elapsedDisplayUnit = elapsedDisplayUnit.name,
        status = status.name, archived = archived, pinned = pinned, position = position,
        createdAtMillis = createdAtMillis, updatedAtMillis = updatedAtMillis,
    )
}

internal fun GoalEntity.toDomain() = Goal(
    id = id, uuid = uuid, measurementId = measurementId, name = name, description = description,
    areaId = areaId, area = area, tags = tagsCsv.split(',').map(String::trim).filter(String::isNotBlank),
    icon = icon, type = GoalType.valueOf(type),
    dimension = UnitDimension.valueOf(dimension), unitId = unitId, precision = precision,
    baseline = baseline, targetMin = targetMin, targetMax = targetMax,
    direction = GoalDirection.valueOf(direction), startDate = LocalDate.ofEpochDay(startEpochDay),
    deadline = deadlineEpochDay?.let(LocalDate::ofEpochDay), aggregation = GoalAggregation.valueOf(aggregation),
    paceType = GoalPaceType.valueOf(paceType),
    reminderMinutes = reminderMinutes, status = lifecycleStatus(), archived = archived || status == GoalStatus.Archived.name, pinned = pinned,
    position = position, createdAtMillis = createdAtMillis, updatedAtMillis = updatedAtMillis,
    aggregationPeriod = GoalAggregationPeriod.valueOf(aggregationPeriod), rollingDays = rollingDays,
    consistencyPeriod = GoalConsistencyPeriod.valueOf(consistencyPeriod),
    consistencyRequiredPeriods = consistencyRequiredPeriods,
    elapsedStartMillis = elapsedStartMillis,
    elapsedDisplayUnit = ElapsedDisplayUnit.valueOf(elapsedDisplayUnit),
)
internal fun GoalMilestoneEntity.toDomain() = GoalMilestone(id, uuid, goalId, name, position, weight, completed, completedAtMillis, reward, createdAtMillis, updatedAtMillis)

private fun GoalEntity.lifecycleStatus(): GoalStatus = if (status == GoalStatus.Archived.name) {
    GoalStatus.Active
} else {
    GoalStatus.valueOf(status)
}

private fun GoalClosureSnapshotEntity.toDomain() = GoalClosureSnapshot(
    id = id,
    uuid = uuid,
    goalId = goalId,
    completedAtMillis = completedAtMillis,
    value = value,
    progress = progress,
    status = GoalStatus.valueOf(status),
    elapsedDurationMillis = elapsedDurationMillis,
    completedMilestoneCount = completedMilestoneCount,
    totalMilestoneCount = totalMilestoneCount,
)

private fun GoalElapsedResetEventEntity.toDomain() = GoalElapsedResetEvent(
    id = id,
    uuid = uuid,
    goalId = goalId,
    goalUuid = goalUuid,
    previousStartMillis = previousStartMillis,
    newStartMillis = newStartMillis,
    resetAtMillis = resetAtMillis,
    elapsedDurationMillis = elapsedDurationMillis,
)

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
        elapsedStartMillis = elapsedStartMillis,
        elapsedDisplayUnit = elapsedDisplayUnit,
    )
}
