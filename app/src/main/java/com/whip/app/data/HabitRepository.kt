package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.core.HabitTimerClock
import com.whip.app.core.WallOnlyHabitTimerClock
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitChecklistItem
import com.whip.app.domain.HabitChecklistItemDraft
import com.whip.app.domain.HabitChecklistState
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitPause
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitSkip
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.HabitTimerBoundary
import com.whip.app.domain.HabitTimerReviewResolution
import com.whip.app.domain.HabitTimerSessionState
import com.whip.app.domain.HabitTimerStartOutcome
import com.whip.app.domain.HabitTimerStartRequest
import com.whip.app.domain.HabitTimerStopOutcome
import com.whip.app.domain.withConfigurationSemantics
import com.whip.app.domain.DEFAULT_HABIT_EMOJI
import com.whip.app.domain.normalizedIdentityEmoji
import com.whip.app.domain.MeasurementEntryStatus
import com.whip.app.domain.MeasurementSourceType
import com.whip.app.domain.MeasurementValueKind
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.toWeekdayMask
import com.whip.app.domain.toWeekdays
import com.whip.app.domain.isScheduledOn
import com.whip.app.domain.supportsQuickAddAmounts
import com.whip.app.domain.valueForPeriod
import com.whip.app.domain.validationErrors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface HabitRepository {
    val habits: Flow<List<Habit>>
    val checklistItems: Flow<List<HabitChecklistItem>>
    val logs: Flow<List<HabitLog>>
    val checklistStates: Flow<List<HabitChecklistState>>
    val pauses: Flow<List<HabitPause>>
    val skips: Flow<List<HabitSkip>>

    suspend fun get(id: Long): Habit?
    suspend fun create(draft: HabitDraft): Long
    suspend fun update(id: Long, draft: HabitDraft)
    suspend fun duplicate(id: Long): Long
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun setPinned(id: Long, pinned: Boolean)
    suspend fun setPaused(id: Long, paused: Boolean)
    suspend fun reorder(ids: List<Long>)
    suspend fun addPause(id: Long, start: LocalDate, end: LocalDate?, note: String = ""): Long
    suspend fun updatePause(
        pauseId: Long,
        start: LocalDate,
        end: LocalDate?,
        note: String = "",
        expectedHabitId: Long? = null,
    ): Long
    suspend fun deletePause(pauseId: Long, expectedHabitId: Long? = null): Long
    suspend fun skipDay(habitId: Long, date: LocalDate)
    suspend fun undoSkip(habitId: Long, date: LocalDate)
    suspend fun log(
        habitId: Long,
        value: Double?,
        status: HabitLogStatus = HabitLogStatus.Recorded,
        date: LocalDate? = null,
        timestamp: Instant? = null,
        note: String = "",
        sourceType: MeasurementSourceType = MeasurementSourceType.Manual,
        sourceId: String? = null,
    ): Long
    suspend fun setPeriodValue(habitId: Long, date: LocalDate, value: Double, note: String = ""): Long?
    suspend fun undoLog(logId: Long, expectedHabitId: Long? = null): Long
    suspend fun updateLog(
        logId: Long,
        value: Double?,
        status: HabitLogStatus,
        date: LocalDate,
        note: String = "",
        enteredUnitId: String? = null,
        expectedHabitId: Long? = null,
    ): Long
    suspend fun setCheckOff(habitId: Long, date: LocalDate, completed: Boolean)
    suspend fun toggleChecklistItem(habitId: Long, itemId: Long, date: LocalDate, completed: Boolean)
    suspend fun startTimer(request: HabitTimerStartRequest): HabitTimerStartOutcome
    suspend fun stopTimer(boundary: HabitTimerBoundary, date: LocalDate? = null): HabitTimerStopOutcome
    suspend fun resolveTimerReview(
        boundary: HabitTimerBoundary,
        resolution: HabitTimerReviewResolution,
    ): HabitTimerStopOutcome
    suspend fun reconcileTimerClockState()
}

class RoomHabitRepository(
    private val database: WhipDatabase,
    private val measurementRepository: MeasurementRepository,
    private val clock: WhipClock,
    private val ids: WhipIdGenerator,
    private val timerClock: HabitTimerClock = WallOnlyHabitTimerClock(clock),
) : HabitRepository {
    private val dao = database.habitDao()
    private val areaRepository = RoomAreaRepository(database, clock, ids)

    override val habits = dao.observeHabits().map { it.map(HabitEntity::toDomain) }
    override val checklistItems = dao.observeChecklistItems().map { it.map(HabitChecklistItemEntity::toDomain) }
    override val logs = dao.observeLogs().map { it.map(HabitLogEntity::toDomain) }
    override val checklistStates = dao.observeChecklistStates().map { it.map(HabitChecklistStateEntity::toDomain) }
    override val pauses = dao.observePauses().map { it.map(HabitPauseEntity::toDomain) }
    override val skips = dao.observeSkips().map { it.map(HabitSkipEntity::toDomain) }

    override suspend fun get(id: Long): Habit? = dao.getHabit(id)?.toDomain()

    override suspend fun create(draft: HabitDraft): Long = database.withTransaction {
        val semanticDraft = draft.withConfigurationSemantics()
        validateHabit(semanticDraft)
        validateSourceMeasurement(semanticDraft)
        val area = areaRepository.resolve(semanticDraft.areaId, semanticDraft.area)
        val resolvedDraft = semanticDraft.copy(areaId = area.id, area = area.name)
        measurementRepository.ensureCustomUnit(semanticDraft.unitId, semanticDraft.unitId, semanticDraft.unitId, semanticDraft.dimension)
        val measurementId = measurementRepository.createMeasurement(
            name = semanticDraft.name,
            valueKind = semanticDraft.trackingMode.measurementValueKind(),
            dimension = semanticDraft.dimension,
            defaultUnitId = semanticDraft.unitId,
            precision = semanticDraft.precision,
        )
        val now = clock.now().toEpochMilli()
        val habitId = dao.insertHabit(
            resolvedDraft.toEntity(
                uuid = ids.nextId(),
                measurementId = measurementId,
                position = dao.nextPosition(),
                createdAtMillis = now,
            ),
        )
        syncChecklist(habitId, resolvedDraft.checklistItems, now)
        habitId
    }

    override suspend fun update(id: Long, draft: HabitDraft) = database.withTransaction {
        val semanticDraft = draft.withConfigurationSemantics()
        validateHabit(semanticDraft)
        validateSourceMeasurement(semanticDraft)
        val area = areaRepository.resolve(semanticDraft.areaId, semanticDraft.area)
        val resolvedDraft = semanticDraft.copy(areaId = area.id, area = area.name)
        measurementRepository.ensureCustomUnit(semanticDraft.unitId, semanticDraft.unitId, semanticDraft.unitId, semanticDraft.dimension)
        val existing = dao.getHabit(id) ?: error("Habit no longer exists")
        require(existing.dimension == semanticDraft.dimension.name) {
            "A habit's measurement dimension cannot change; create a new habit instead"
        }
        if (existing.timerSessionId != null) {
            val resolvedEndEpochDay = semanticDraft.endDate
                ?.takeIf { semanticDraft.endType == HabitEndType.OnDate }
                ?.toEpochDay()
            val resolvedEndValue = semanticDraft.endValue?.takeIf {
                semanticDraft.endType in setOf(
                    HabitEndType.AfterStreak,
                    HabitEndType.AfterCompletions,
                    HabitEndType.AfterTotal,
                )
            }
            require(
                semanticDraft.trackingMode == HabitTrackingMode.Duration &&
                    semanticDraft.dimension == UnitDimension.Duration &&
                    semanticDraft.unitId == existing.unitId &&
                    semanticDraft.sourceMeasurementId == null &&
                    semanticDraft.scheduleType.name == existing.scheduleType &&
                    semanticDraft.scheduleInterval == existing.scheduleInterval &&
                    semanticDraft.weekdays.toWeekdayMask() == existing.weekdaysMask &&
                    semanticDraft.flexibleTimesPerWeek == existing.flexibleTimesPerWeek &&
                    semanticDraft.startDate.toEpochDay() == existing.startEpochDay &&
                    semanticDraft.endType.name == existing.endType &&
                    resolvedEndEpochDay == existing.endEpochDay &&
                    resolvedEndValue == existing.endValue
            ) { "Stop or discard the running timer before changing its tracking unit or schedule" }
        }
        val now = clock.now().toEpochMilli()
        dao.updateHabit(
            resolvedDraft.toEntity(
                id = existing.id,
                uuid = existing.uuid,
                measurementId = existing.measurementId,
                timerStartedAtMillis = existing.timerStartedAtMillis,
                timerSessionId = existing.timerSessionId,
                timerNeedsReview = existing.timerNeedsReview,
                timerAccumulatedSeconds = existing.timerAccumulatedSeconds,
                timerAnchorElapsedRealtimeMillis = existing.timerAnchorElapsedRealtimeMillis,
                pinned = existing.pinned,
                position = existing.position,
                archived = existing.archived,
                paused = existing.paused,
                createdAtMillis = existing.createdAtMillis,
                updatedAtMillis = now,
            ),
        )
        val existingChecklist = dao.getChecklistItems(id)
        if (!existingChecklist.matches(resolvedDraft.checklistItems)) {
            syncChecklist(id, resolvedDraft.checklistItems, now)
        }
    }

    override suspend fun duplicate(id: Long): Long = database.withTransaction {
        val habit = dao.getHabit(id)?.toDomain() ?: error("Habit no longer exists")
        val items = dao.getChecklistItems(id).filterNot { it.archived }
        create(habit.toDraft(items).copy(name = "${habit.name} copy"))
    }

    override suspend fun setArchived(id: Long, archived: Boolean) = updateFlags(id) {
        require(!archived || it.timerSessionId == null) { "Stop or discard the running timer before archiving this Habit" }
        it.copy(archived = archived)
    }
    override suspend fun setPinned(id: Long, pinned: Boolean) = updateFlags(id) { it.copy(pinned = pinned) }
    override suspend fun setPaused(id: Long, paused: Boolean) = updateFlags(id) {
        require(!paused || it.timerSessionId == null) { "Stop or discard the running timer before pausing this Habit" }
        it.copy(paused = paused)
    }

    override suspend fun reorder(ids: List<Long>) = database.withTransaction {
        val requested = ids.distinct()
        val all = dao.getAllHabits()
        require(requested.all { id -> all.any { it.id == id } }) { "Habit no longer exists" }
        val byId = all.associateBy(HabitEntity::id)
        val order = requested + all.filterNot { it.id in requested }.sortedBy(HabitEntity::position).map(HabitEntity::id)
        val now = clock.now().toEpochMilli()
        order.forEachIndexed { index, id ->
            val current = requireNotNull(byId[id])
            if (current.position != index) dao.updateHabit(current.copy(position = index, updatedAtMillis = now))
        }
    }

    override suspend fun addPause(id: Long, start: LocalDate, end: LocalDate?, note: String): Long {
        require(end == null || !end.isBefore(start)) { "Pause end cannot precede its start" }
        requireNotNull(dao.getHabit(id)) { "Habit no longer exists" }
        return dao.insertPause(HabitPauseEntity(habitId = id, startEpochDay = start.toEpochDay(), endEpochDay = end?.toEpochDay(), note = note.trim()))
    }

    override suspend fun updatePause(
        pauseId: Long,
        start: LocalDate,
        end: LocalDate?,
        note: String,
        expectedHabitId: Long?,
    ): Long = database.withTransaction {
        require(end == null || !end.isBefore(start)) { "Pause end cannot precede its start" }
        val current = dao.getPause(pauseId) ?: error("Scheduled pause no longer exists")
        require(expectedHabitId == null || current.habitId == expectedHabitId) {
            "Scheduled pause does not belong to the selected Habit"
        }
        dao.updatePause(
            current.copy(
                startEpochDay = start.toEpochDay(),
                endEpochDay = end?.toEpochDay(),
                note = note.trim(),
            ),
        )
        current.habitId
    }

    override suspend fun deletePause(pauseId: Long, expectedHabitId: Long?): Long = database.withTransaction {
        val current = dao.getPause(pauseId) ?: error("Scheduled pause no longer exists")
        require(expectedHabitId == null || current.habitId == expectedHabitId) {
            "Scheduled pause does not belong to the selected Habit"
        }
        check(dao.deletePause(pauseId) == 1) { "Scheduled pause no longer exists" }
        current.habitId
    }

    override suspend fun log(
        habitId: Long,
        value: Double?,
        status: HabitLogStatus,
        date: LocalDate?,
        timestamp: Instant?,
        note: String,
        sourceType: MeasurementSourceType,
        sourceId: String?,
    ): Long = database.withTransaction {
        if (sourceType != MeasurementSourceType.Manual && !sourceId.isNullOrBlank()) {
            dao.getLogBySource(sourceType.name, sourceId)?.let { return@withTransaction it.id }
        }
        val habit = dao.getHabit(habitId)?.toDomain() ?: error("Habit no longer exists")
        val instant = timestamp ?: clock.now()
        val zone = clock.zoneId()
        val localDate = date ?: timestamp?.atZone(zone)?.toLocalDate() ?: clock.today(zone)
        requireHabitCanAcceptManualProgress(habit)
        require(!instant.isAfter(clock.now())) { "Habit check-ins cannot be recorded in the future" }
        require(!localDate.isAfter(clock.today(zone))) { "Habit check-ins cannot be recorded in the future" }
        dao.deleteSkip(habitId, localDate.toEpochDay())
        val logUuid = ids.nextId()
        val effectiveValue = if (
            habit.trackingMode == HabitTrackingMode.CheckOff &&
            status in setOf(HabitLogStatus.Recorded, HabitLogStatus.Success)
        ) value ?: 1.0 else value
        val entryStatus = when (status) {
            HabitLogStatus.Failed -> MeasurementEntryStatus.Failed
            else -> MeasurementEntryStatus.Recorded
        }
        val measurementEntryId = measurementRepository.record(
            measurementId = habit.measurementId,
            value = effectiveValue,
            unitId = habit.unitId.takeIf { effectiveValue != null },
            status = entryStatus,
            timestamp = instant,
            localDate = localDate,
            zoneId = zone,
            sourceType = MeasurementSourceType.Habit,
            sourceId = logUuid,
            note = note,
        )
        val canonical = database.measurementDao().getEntry(measurementEntryId)?.canonicalValue
        dao.insertLog(
            HabitLogEntity(
                uuid = logUuid,
                habitId = habitId,
                value = effectiveValue,
                canonicalValue = canonical,
                enteredUnitId = habit.unitId.takeIf { effectiveValue != null },
                status = status.name,
                timestampMillis = instant.toEpochMilli(),
                localEpochDay = localDate.toEpochDay(),
                zoneId = zone.id,
                offsetSeconds = zone.rules.getOffset(instant).totalSeconds,
                note = note.trim(),
                sourceType = sourceType.name,
                sourceId = sourceId,
                measurementEntryId = measurementEntryId,
                createdAtMillis = clock.now().toEpochMilli(),
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun setPeriodValue(
        habitId: Long,
        date: LocalDate,
        value: Double,
        note: String,
    ): Long? = database.withTransaction {
        require(value.isFinite()) { "Habit value must be finite" }
        val habit = dao.getHabit(habitId)?.toDomain() ?: error("Habit no longer exists")
        requireHabitCanAcceptManualProgress(habit)
        require(!date.isAfter(clock.today(clock.zoneId()))) { "Habit check-ins cannot be recorded in the future" }
        val current = habit.valueForPeriod(
            logs = dao.getLogsForHabit(habitId).map(HabitLogEntity::toDomain),
            date = date,
            customUnits = database.measurementDao().getAllUnits().map(UnitDefinitionEntity::toDomain),
        )
        val rawDelta = value - current
        // Ignore only binary representation noise. A relative epsilon grows
        // without bound and can erase real authored changes at large totals.
        val tolerance = max(1e-12, 4.0 * max(Math.ulp(value), Math.ulp(current)))
        val delta = rawDelta.takeUnless { abs(it) <= tolerance } ?: 0.0
        if (delta == 0.0 && note.isBlank()) return@withTransaction null
        log(habitId, delta, date = date, note = note)
    }

    override suspend fun undoLog(logId: Long, expectedHabitId: Long?): Long = database.withTransaction {
        val log = dao.getLog(logId) ?: error("Habit log no longer exists")
        require(expectedHabitId == null || log.habitId == expectedHabitId) {
            "Habit log does not belong to the selected Habit"
        }
        log.measurementEntryId?.let { measurementRepository.deleteEntry(it) }
        dao.deleteLog(logId)
        log.habitId
    }

    override suspend fun updateLog(
        logId: Long,
        value: Double?,
        status: HabitLogStatus,
        date: LocalDate,
        note: String,
        enteredUnitId: String?,
        expectedHabitId: Long?,
    ): Long = database.withTransaction {
        val existing = dao.getLog(logId) ?: error("Habit log no longer exists")
        require(expectedHabitId == null || existing.habitId == expectedHabitId) {
            "Habit log does not belong to the selected Habit"
        }
        val habit = dao.getHabit(existing.habitId)?.toDomain() ?: error("Habit no longer exists")
        val effectiveValue = if (habit.trackingMode == HabitTrackingMode.CheckOff && status in setOf(HabitLogStatus.Recorded, HabitLogStatus.Success)) {
            value ?: 1.0
        } else {
            value
        }
        val entryStatus = when (status) {
            HabitLogStatus.Failed -> MeasurementEntryStatus.Failed
            else -> MeasurementEntryStatus.Recorded
        }
        dao.deleteSkip(existing.habitId, date.toEpochDay())
        val zone = ZoneId.of(existing.zoneId)
        val instant = Instant.ofEpochMilli(existing.timestampMillis)
        val effectiveUnitId = enteredUnitId ?: existing.enteredUnitId ?: habit.unitId
        val measurementEntryId = measurementRepository.record(
            measurementId = habit.measurementId,
            value = effectiveValue,
            unitId = effectiveUnitId.takeIf { effectiveValue != null },
            status = entryStatus,
            timestamp = instant,
            localDate = date,
            zoneId = zone,
            // Measurement entries backing Habit logs always retain their Habit wrapper
            // provenance, even when the log itself records an initiating source.
            sourceType = MeasurementSourceType.Habit,
            sourceId = existing.uuid,
            note = note,
            existingEntryId = existing.measurementEntryId,
        )
        dao.updateLog(
            existing.copy(
                value = effectiveValue,
                canonicalValue = database.measurementDao().getEntry(measurementEntryId)?.canonicalValue,
                enteredUnitId = effectiveUnitId.takeIf { effectiveValue != null },
                status = status.name,
                localEpochDay = date.toEpochDay(),
                note = note.trim(),
                measurementEntryId = measurementEntryId,
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
        existing.habitId
    }

    override suspend fun setCheckOff(habitId: Long, date: LocalDate, completed: Boolean) {
        database.withTransaction {
            val habit = dao.getHabit(habitId)?.toDomain() ?: error("Habit no longer exists")
            requireHabitCanAcceptManualProgress(habit)
            require(!date.isAfter(clock.today(clock.zoneId()))) { "Habit check-ins cannot be recorded in the future" }
            require(habit.trackingMode in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist)) {
                "This habit cannot be completed with a check-off"
            }
            setCompletionWithinTransaction(habitId, date, completed)
        }
    }

    override suspend fun toggleChecklistItem(
        habitId: Long,
        itemId: Long,
        date: LocalDate,
        completed: Boolean,
    ) {
        database.withTransaction {
            val habit = dao.getHabit(habitId)?.toDomain() ?: error("Habit no longer exists")
            requireHabitCanAcceptManualProgress(habit)
            require(!date.isAfter(clock.today(clock.zoneId()))) { "Habit check-ins cannot be recorded in the future" }
            require(habit.trackingMode == HabitTrackingMode.Checklist) { "This habit does not have checklist items" }
            val activeItems = dao.getChecklistItems(habitId).filterNot(HabitChecklistItemEntity::archived)
            val item = activeItems.firstOrNull { it.id == itemId }
                ?: error("Checklist item no longer exists")
            val now = clock.now().toEpochMilli()
            dao.upsertChecklistState(
                HabitChecklistStateEntity(
                    habitId = habitId,
                    itemId = itemId,
                    localEpochDay = date.toEpochDay(),
                    completed = completed,
                    completedAtMillis = now.takeIf { completed },
                    nameSnapshot = item.name,
                ),
            )
            if (
                habit.autoCompleteFromItems &&
                activeItems.isNotEmpty() &&
                dao.completedChecklistCount(habitId, date.toEpochDay()) == activeItems.size
            ) {
                setCompletionWithinTransaction(habitId, date, completed = true)
            }
        }
    }

    override suspend fun skipDay(habitId: Long, date: LocalDate) = database.withTransaction {
        val habit = dao.getHabit(habitId)?.toDomain() ?: error("Habit no longer exists")
        require(!habit.archived) { "Archived habits cannot be skipped" }
        require(!habit.paused) { "Paused habits do not need to be skipped" }
        require(habit.sourceMeasurementId == null) { "Synced habits cannot be skipped manually" }
        require(habit.scheduleType !in setOf(HabitScheduleType.FlexibleTimesPerWeek, HabitScheduleType.FlexibleTimesPerMonth)) {
            "Flexible habits are completed any time during their period and do not have a daily occurrence to skip"
        }
        require(habit.isScheduledOn(date)) { "This habit is not scheduled for that date" }
        require(dao.getLogsForDate(habitId, date.toEpochDay()).isEmpty()) {
            "Remove the existing check-in before skipping this day"
        }
        require(dao.completedChecklistCount(habitId, date.toEpochDay()) == 0) {
            "Clear the completed checklist items before skipping this day"
        }
        val now = clock.now().toEpochMilli()
        val existing = dao.getSkips(habitId).firstOrNull { it.localEpochDay == date.toEpochDay() }
        dao.upsertSkip(
            HabitSkipEntity(
                uuid = existing?.uuid ?: ids.nextId(),
                habitId = habitId,
                localEpochDay = date.toEpochDay(),
                skippedAtMillis = existing?.skippedAtMillis ?: now,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            ),
        )
    }

    override suspend fun undoSkip(habitId: Long, date: LocalDate) = database.withTransaction {
        requireNotNull(dao.getHabit(habitId)) { "Habit no longer exists" }
        check(dao.deleteSkip(habitId, date.toEpochDay()) == 1) { "Skipped day no longer exists" }
    }

    override suspend fun startTimer(request: HabitTimerStartRequest): HabitTimerStartOutcome = database.withTransaction {
        dao.getTimerSession(request.requestId)?.let { existing ->
            require(existing.habitId == request.habitId) { "Timer request belongs to another Habit" }
            return@withTransaction if (existing.activeHabitId != null) {
                val habit = dao.getHabit(existing.habitId) ?: error("Habit no longer exists")
                require(habit.uuid == request.habitUuid) { "Habit changed after this timer action was created" }
                HabitTimerStartOutcome.AlreadyRunning(existing.boundary(habit), existing.state == HabitTimerSessionState.ReviewRequired.name)
            } else HabitTimerStartOutcome.AlreadyResolved
        }
        val habit = dao.getHabit(request.habitId) ?: error("Habit no longer exists")
        require(habit.uuid == request.habitUuid) { "Habit changed after this timer action was created" }
        require(habit.trackingMode == HabitTrackingMode.Duration.name && habit.dimension == UnitDimension.Duration.name) {
            "Only Duration Habits can use a timer"
        }
        require(habit.sourceMeasurementId == null) { "Synced Habits cannot start a manual timer" }
        require(!habit.archived) { "Restore this Habit before starting its timer" }
        require(!habit.paused) { "Resume this Habit before starting its timer" }
        require(habit.endType != HabitEndType.OnDate.name || habit.endEpochDay == null || habit.endEpochDay >= clock.today().toEpochDay()) {
            "This Habit has ended"
        }
        val measurement = database.measurementDao().getMeasurement(habit.measurementId) ?: error("Habit measurement no longer exists")
        require(measurement.valueKind == MeasurementValueKind.Duration.name && measurement.dimension == UnitDimension.Duration.name) {
            "Habit timer measurement contract is invalid"
        }
        val unit = BuiltInUnits.get(habit.unitId) ?: database.measurementDao().getUnit(habit.unitId)?.toDomain()
        require(unit?.dimension == UnitDimension.Duration) { "Choose a Duration unit before starting the timer" }
        dao.getActiveTimerSession(habit.id)?.let { active ->
            val reading = timerClock.read()
            // Consume the losing request identity. If this delayed or retried Start action
            // arrives again after the current timer stops, it must not create a surprise timer.
            dao.insertTimerSession(
                HabitTimerSessionEntity(
                    sessionId = request.requestId,
                    habitId = habit.id,
                    activeHabitId = null,
                    state = HabitTimerSessionState.Discarded.name,
                    anchorWallMillis = null,
                    anchorElapsedRealtimeMillis = null,
                    anchorBootId = null,
                    accumulatedCanonicalSeconds = null,
                    unitId = null,
                    createdAtMillis = reading.wallMillis,
                    resolvedAtMillis = reading.wallMillis,
                ),
            )
            return@withTransaction HabitTimerStartOutcome.AlreadyRunning(
                active.boundary(habit),
                active.state == HabitTimerSessionState.ReviewRequired.name,
            )
        }
        val reading = timerClock.read()
        val exact = reading.elapsedRealtimeMillis != null && reading.bootId != null
        val state = if (exact) HabitTimerSessionState.Running else HabitTimerSessionState.ReviewRequired
        val session = HabitTimerSessionEntity(
            sessionId = request.requestId,
            habitId = habit.id,
            activeHabitId = habit.id,
            state = state.name,
            anchorWallMillis = reading.wallMillis,
            anchorElapsedRealtimeMillis = reading.elapsedRealtimeMillis,
            anchorBootId = reading.bootId,
            accumulatedCanonicalSeconds = 0.0,
            unitId = habit.unitId,
            createdAtMillis = reading.wallMillis,
            resolvedAtMillis = null,
        )
        dao.insertTimerSession(session)
        dao.updateHabit(
            habit.copy(
                timerStartedAtMillis = reading.wallMillis,
                timerSessionId = session.sessionId,
                timerNeedsReview = !exact,
                timerAccumulatedSeconds = 0.0,
                timerAnchorElapsedRealtimeMillis = reading.elapsedRealtimeMillis.takeIf { exact },
                updatedAtMillis = reading.wallMillis,
            ),
        )
        HabitTimerStartOutcome.Started(session.boundary(habit), needsReview = !exact)
    }

    override suspend fun stopTimer(
        boundary: HabitTimerBoundary,
        date: LocalDate?,
    ): HabitTimerStopOutcome = database.withTransaction {
        val session = requireTimerSession(boundary)
        when (session.state) {
            HabitTimerSessionState.Completed.name -> HabitTimerStopOutcome.AlreadyCompleted(
                historyPresent = dao.getLogBySource(MeasurementSourceType.Manual.name, timerSourceId(session.sessionId)) != null,
            )
            HabitTimerSessionState.Discarded.name -> HabitTimerStopOutcome.AlreadyDiscarded
            HabitTimerSessionState.ReviewRequired.name -> HabitTimerStopOutcome.ReviewRequired(
                boundary,
                estimatedTimerSeconds(session, timerClock.read()),
            )
            HabitTimerSessionState.Running.name -> {
                val reading = timerClock.read()
                val anchorElapsed = session.anchorElapsedRealtimeMillis
                val exact = anchorElapsed != null &&
                    session.anchorBootId != null &&
                    reading.elapsedRealtimeMillis != null &&
                    reading.bootId == session.anchorBootId &&
                    reading.elapsedRealtimeMillis >= anchorElapsed
                if (!exact) {
                    markTimerReviewRequired(session, reading.wallMillis)
                    HabitTimerStopOutcome.ReviewRequired(boundary, estimatedTimerSeconds(session, reading))
                } else {
                    val seconds = requireNotNull(session.accumulatedCanonicalSeconds) +
                        (requireNotNull(reading.elapsedRealtimeMillis) - anchorElapsed) / 1_000.0
                    stopAndLogTimer(session, boundary, seconds, date, reading.wallMillis)
                }
            }
            else -> error("Timer has an unknown state")
        }
    }

    override suspend fun resolveTimerReview(
        boundary: HabitTimerBoundary,
        resolution: HabitTimerReviewResolution,
    ): HabitTimerStopOutcome = database.withTransaction {
        val session = requireTimerSession(boundary)
        when (session.state) {
            HabitTimerSessionState.Completed.name -> return@withTransaction HabitTimerStopOutcome.AlreadyCompleted(
                historyPresent = dao.getLogBySource(MeasurementSourceType.Manual.name, timerSourceId(session.sessionId)) != null,
            )
            HabitTimerSessionState.Discarded.name -> return@withTransaction HabitTimerStopOutcome.AlreadyDiscarded
            HabitTimerSessionState.ReviewRequired.name -> Unit
            else -> error("Timer no longer requires review")
        }
        val reading = timerClock.read()
        when (resolution) {
            is HabitTimerReviewResolution.StopAndLog -> stopAndLogTimer(
                session,
                boundary,
                resolution.canonicalSeconds.validTimerSeconds(),
                resolution.date,
                reading.wallMillis,
            )
            is HabitTimerReviewResolution.Continue -> {
                val elapsed = requireNotNull(reading.elapsedRealtimeMillis) { "This device cannot safely continue the timer" }
                val boot = requireNotNull(reading.bootId) { "This device cannot safely continue the timer" }
                val seconds = resolution.canonicalSeconds.validTimerSeconds()
                dao.updateTimerSession(
                    session.copy(
                        state = HabitTimerSessionState.Running.name,
                        anchorWallMillis = reading.wallMillis,
                        anchorElapsedRealtimeMillis = elapsed,
                        anchorBootId = boot,
                        accumulatedCanonicalSeconds = seconds,
                    ),
                )
                val habit = dao.getHabit(session.habitId) ?: error("Habit no longer exists")
                dao.updateHabit(
                    habit.copy(
                        timerStartedAtMillis = reading.wallMillis,
                        timerNeedsReview = false,
                        timerAccumulatedSeconds = seconds,
                        timerAnchorElapsedRealtimeMillis = elapsed,
                        updatedAtMillis = reading.wallMillis,
                    ),
                )
                HabitTimerStopOutcome.Continued(boundary, seconds)
            }
            HabitTimerReviewResolution.Discard -> {
                settleTimer(session, HabitTimerSessionState.Discarded, reading.wallMillis)
                HabitTimerStopOutcome.Discarded
            }
        }
    }

    override suspend fun reconcileTimerClockState() = database.withTransaction {
        val reading = timerClock.read()
        val sessions = dao.getActiveTimerSessions()
        val activeByHabit = sessions.associateBy(HabitTimerSessionEntity::habitId)
        sessions.forEach { stored ->
            val session = if (
                stored.state == HabitTimerSessionState.Running.name &&
                (stored.anchorBootId == null || stored.anchorElapsedRealtimeMillis == null ||
                    reading.bootId != stored.anchorBootId || reading.elapsedRealtimeMillis == null ||
                    reading.elapsedRealtimeMillis < stored.anchorElapsedRealtimeMillis)
            ) {
                val updated = stored.copy(state = HabitTimerSessionState.ReviewRequired.name)
                dao.updateTimerSession(updated)
                updated
            } else stored
            dao.getHabit(session.habitId)?.let { habit ->
                val wall = session.anchorWallMillis ?: reading.wallMillis
                val needsReview = session.state == HabitTimerSessionState.ReviewRequired.name
                val elapsedAnchor = session.anchorElapsedRealtimeMillis.takeUnless { needsReview }
                if (
                    habit.timerSessionId != session.sessionId || habit.timerStartedAtMillis != wall ||
                    habit.timerNeedsReview != needsReview ||
                    habit.timerAccumulatedSeconds != (session.accumulatedCanonicalSeconds ?: 0.0) ||
                    habit.timerAnchorElapsedRealtimeMillis != elapsedAnchor
                ) {
                    dao.updateHabit(
                        habit.copy(
                            timerSessionId = session.sessionId,
                            timerStartedAtMillis = wall,
                            timerNeedsReview = needsReview,
                            timerAccumulatedSeconds = session.accumulatedCanonicalSeconds ?: 0.0,
                            timerAnchorElapsedRealtimeMillis = elapsedAnchor,
                            updatedAtMillis = reading.wallMillis,
                        ),
                    )
                }
            }
        }
        dao.getAllHabits().filter { it.timerSessionId != null && it.id !in activeByHabit }.forEach { habit ->
            dao.updateHabit(
                habit.copy(
                    timerSessionId = null,
                    timerStartedAtMillis = null,
                    timerNeedsReview = false,
                    timerAccumulatedSeconds = 0.0,
                    timerAnchorElapsedRealtimeMillis = null,
                    updatedAtMillis = reading.wallMillis,
                ),
            )
        }
    }

    private suspend fun requireTimerSession(boundary: HabitTimerBoundary): HabitTimerSessionEntity {
        val habit = dao.getHabit(boundary.habitId) ?: error("Habit no longer exists")
        require(habit.uuid == boundary.habitUuid) { "Habit changed after this timer action was created" }
        val session = dao.getTimerSession(boundary.sessionId) ?: error("Timer session no longer exists")
        require(session.habitId == boundary.habitId) { "Timer belongs to another Habit" }
        return session
    }

    private suspend fun markTimerReviewRequired(session: HabitTimerSessionEntity, now: Long) {
        dao.updateTimerSession(session.copy(state = HabitTimerSessionState.ReviewRequired.name))
        val habit = dao.getHabit(session.habitId) ?: return
        dao.updateHabit(
            habit.copy(
                timerNeedsReview = true,
                timerAnchorElapsedRealtimeMillis = null,
                updatedAtMillis = now,
            ),
        )
    }

    private suspend fun stopAndLogTimer(
        session: HabitTimerSessionEntity,
        boundary: HabitTimerBoundary,
        canonicalSeconds: Double,
        date: LocalDate?,
        now: Long,
    ): HabitTimerStopOutcome {
        val seconds = canonicalSeconds.validTimerSeconds()
        val logId = if (seconds == 0.0) null else insertCanonicalTimerLog(session, seconds, date, now)
        settleTimer(session, HabitTimerSessionState.Completed, now)
        return HabitTimerStopOutcome.Stopped(boundary, seconds, logId)
    }

    private suspend fun insertCanonicalTimerLog(
        session: HabitTimerSessionEntity,
        canonicalSeconds: Double,
        date: LocalDate?,
        now: Long,
    ): Long {
        val habit = dao.getHabit(session.habitId) ?: error("Habit no longer exists")
        val measurementDao = database.measurementDao()
        val measurement = measurementDao.getMeasurement(habit.measurementId) ?: error("Habit measurement no longer exists")
        require(measurement.valueKind == MeasurementValueKind.Duration.name && measurement.dimension == UnitDimension.Duration.name) {
            "Habit timer measurement contract is invalid"
        }
        val unitId = requireNotNull(session.unitId) { "Timer has no saved duration unit" }
        val unit = BuiltInUnits.get(unitId) ?: measurementDao.getUnit(unitId)?.toDomain()
        require(unit?.dimension == UnitDimension.Duration) { "Timer's saved duration unit is unavailable" }
        val value = unit.fromCanonical(canonicalSeconds)
        require(value.isFinite()) { "Timer duration cannot be represented in its saved unit" }
        val instant = Instant.ofEpochMilli(now)
        val zone = clock.zoneId()
        val effectiveDate = date ?: clock.today(zone)
        val logUuid = ids.nextId()
        val entryId = ids.nextId()
        val sourceId = timerSourceId(session.sessionId)
        val firstEntry = measurementDao.entryCount(measurement.id) == 0
        measurementDao.upsertEntry(
            MeasurementEntryEntity(
                id = entryId,
                measurementId = measurement.id,
                canonicalValue = canonicalSeconds,
                enteredValue = value,
                enteredUnitId = unitId,
                status = MeasurementEntryStatus.Recorded.name,
                timestampMillis = now,
                localEpochDay = effectiveDate.toEpochDay(),
                zoneId = zone.id,
                offsetSeconds = zone.rules.getOffset(instant).totalSeconds,
                sourceType = MeasurementSourceType.Habit.name,
                sourceId = logUuid,
                note = "",
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        if (firstEntry && !measurement.dimensionLocked) {
            measurementDao.upsertMeasurement(measurement.copy(dimensionLocked = true, updatedAtMillis = now))
        }
        return dao.insertLog(
            HabitLogEntity(
                uuid = logUuid,
                habitId = habit.id,
                value = value,
                canonicalValue = canonicalSeconds,
                enteredUnitId = unitId,
                status = HabitLogStatus.Recorded.name,
                timestampMillis = now,
                localEpochDay = effectiveDate.toEpochDay(),
                zoneId = zone.id,
                offsetSeconds = zone.rules.getOffset(instant).totalSeconds,
                note = "",
                sourceType = MeasurementSourceType.Manual.name,
                sourceId = sourceId,
                measurementEntryId = entryId,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
    }

    private suspend fun settleTimer(
        session: HabitTimerSessionEntity,
        state: HabitTimerSessionState,
        now: Long,
    ) {
        dao.updateTimerSession(
            session.copy(
                activeHabitId = null,
                state = state.name,
                anchorWallMillis = null,
                anchorElapsedRealtimeMillis = null,
                anchorBootId = null,
                accumulatedCanonicalSeconds = null,
                unitId = null,
                resolvedAtMillis = now,
            ),
        )
        val habit = dao.getHabit(session.habitId) ?: return
        dao.updateHabit(
            habit.copy(
                timerStartedAtMillis = null,
                timerSessionId = null,
                timerNeedsReview = false,
                timerAccumulatedSeconds = 0.0,
                timerAnchorElapsedRealtimeMillis = null,
                updatedAtMillis = now,
            ),
        )
    }

    private suspend fun syncChecklist(habitId: Long, drafts: List<HabitChecklistItemDraft>, now: Long) {
        val existing = dao.getChecklistItems(habitId)
        val existingById = existing.associateBy(HabitChecklistItemEntity::id)
        val existingByUuid = existing.associateBy(HabitChecklistItemEntity::uuid)
        val retainedIds = mutableSetOf<Long>()
        drafts.filter { it.name.isNotBlank() }.sortedBy { it.position }.forEachIndexed { index, draft ->
            val current = draft.id?.let(existingById::get)
                ?: draft.uuid?.let(existingByUuid::get)
            if (current == null) {
                dao.insertChecklistItem(HabitChecklistItemEntity(uuid = ids.nextId(), habitId = habitId, name = draft.name.trim(), position = index, archived = false, createdAtMillis = now, updatedAtMillis = now))
            } else {
                retainedIds += current.id
                dao.updateChecklistItem(current.copy(name = draft.name.trim(), position = index, archived = false, updatedAtMillis = now))
            }
        }
        existing.filterNot { it.id in retainedIds || it.archived }.forEach { item ->
            dao.updateChecklistItem(item.copy(archived = true, updatedAtMillis = now))
        }
    }

    private suspend fun validateSourceMeasurement(draft: HabitDraft) {
        val sourceId = draft.sourceMeasurementId ?: return
        val source = requireNotNull(database.measurementDao().getMeasurement(sourceId)) {
            "Connected data source no longer exists"
        }
        require(!source.archived) { "Restore the connected data source before using it" }
        require(source.dimension == draft.dimension.name) { "Connected data source uses a different measurement type" }
        val expectedMode = when (MeasurementValueKind.valueOf(source.valueKind)) {
            MeasurementValueKind.Integer -> HabitTrackingMode.Count
            MeasurementValueKind.Duration -> HabitTrackingMode.Duration
            else -> HabitTrackingMode.Decimal
        }
        require(draft.trackingMode == expectedMode) {
            "Habit tracking must match its connected data source"
        }
    }

    private suspend fun updateFlags(id: Long, transform: (HabitEntity) -> HabitEntity) = database.withTransaction {
        val current = dao.getHabit(id) ?: return@withTransaction
        dao.updateHabit(transform(current).copy(updatedAtMillis = clock.now().toEpochMilli()))
    }

    private suspend fun setCompletionWithinTransaction(
        habitId: Long,
        date: LocalDate,
        completed: Boolean,
    ) {
        val positiveLogs = dao.getLogsForDate(habitId, date.toEpochDay())
            .filter {
                it.status in setOf(HabitLogStatus.Recorded.name, HabitLogStatus.Success.name) &&
                    (it.value ?: 0.0) > 0.0
            }
        if (completed && positiveLogs.isEmpty()) {
            log(habitId, 1.0, HabitLogStatus.Success, date)
        } else if (!completed) {
            positiveLogs.forEach { entry ->
                entry.measurementEntryId?.let { measurementRepository.deleteEntry(it) }
                dao.deleteLog(entry.id)
            }
        }
    }
}

private fun List<HabitChecklistItemEntity>.matches(drafts: List<HabitChecklistItemDraft>): Boolean {
    val active = filterNot(HabitChecklistItemEntity::archived)
    val normalized = drafts.filter { it.name.isNotBlank() }.sortedBy(HabitChecklistItemDraft::position)
    if (active.size != normalized.size) return false
    return active.zip(normalized).withIndex().all { (index, pair) ->
        val (stored, draft) = pair
        !stored.archived &&
            stored.name == draft.name.trim() &&
            stored.position == index
    }
}

private fun validateHabit(draft: HabitDraft) {
    val problems = draft.validationErrors()
    require(problems.isEmpty()) { problems.first() }
}

private fun requireHabitCanAcceptManualProgress(habit: Habit) {
    require(!habit.archived) { "Archived Habits cannot be changed" }
    require(!habit.paused) { "Paused Habits cannot be changed" }
    require(habit.sourceMeasurementId == null) { "Synced Habits are updated by their connected data source" }
}

private fun HabitTrackingMode.measurementValueKind() = when (this) {
    HabitTrackingMode.CheckOff -> MeasurementValueKind.Boolean
    HabitTrackingMode.Count -> MeasurementValueKind.Integer
    HabitTrackingMode.Decimal, HabitTrackingMode.LogOnly -> MeasurementValueKind.Decimal
    HabitTrackingMode.Duration -> MeasurementValueKind.Duration
    HabitTrackingMode.Checklist -> MeasurementValueKind.Checklist
    HabitTrackingMode.Rating -> MeasurementValueKind.Rating
}

private fun HabitDraft.toEntity(
    id: Long = 0, uuid: String, measurementId: String, timerStartedAtMillis: Long? = null,
    timerSessionId: String? = null, timerNeedsReview: Boolean = false,
    timerAccumulatedSeconds: Double = 0.0,
    timerAnchorElapsedRealtimeMillis: Long? = null,
    pinned: Boolean = false, position: Int, archived: Boolean = false, paused: Boolean = false,
    createdAtMillis: Long, updatedAtMillis: Long = createdAtMillis,
) = HabitEntity(
    id, uuid, measurementId, name.trim(), notes.trim(), areaId, area.trim(), tags.map(String::trim).filter(String::isNotBlank).distinct().joinToString(","), icon.normalizedIdentityEmoji(DEFAULT_HABIT_EMOJI),
    trackingMode.name, dimension.name, unitId, precision, comparison.name, targetMin,
    targetMax, targetPeriod.name, rollingDays, scheduleType.name, scheduleInterval,
    weekdays.toWeekdayMask(), flexibleTimesPerWeek, startDate.toEpochDay(), endType.name,
    endDate?.takeIf { endType == HabitEndType.OnDate }?.toEpochDay(),
    endValue?.takeIf { endType in setOf(HabitEndType.AfterStreak, HabitEndType.AfterCompletions, HabitEndType.AfterTotal) },
    if (sourceMeasurementId == null && trackingMode.supportsQuickAddAmounts()) quickIncrement else 1.0,
    if (sourceMeasurementId == null && trackingMode.supportsQuickAddAmounts()) quickActions.joinToString(",") else "",
    reminderMinutes.joinToString(","),
    weekdayReminderMinutes.toReminderCsv(), weekStart.name, timerStartedAtMillis, pinned, position,
    archived, paused, createdAtMillis, updatedAtMillis, sourceMeasurementId, autoCompleteFromItems,
    timerSessionId, timerNeedsReview, timerAccumulatedSeconds, timerAnchorElapsedRealtimeMillis,
)

private fun HabitEntity.toDomain(): Habit {
    val targetComparison = TargetComparison.valueOf(comparison)
    val resolvedTargetMin = targetMin.takeUnless { targetComparison == TargetComparison.AtMost }
    val resolvedTargetMax = if (targetComparison == TargetComparison.AtMost) targetMax ?: targetMin else targetMax
    return Habit(
        id, uuid, measurementId, name, notes, areaId, area, tagsCsv.split(',').map(String::trim).filter(String::isNotBlank), icon,
        HabitTrackingMode.valueOf(trackingMode), UnitDimension.valueOf(dimension), unitId,
        precision, targetComparison, resolvedTargetMin, resolvedTargetMax,
        TargetPeriod.valueOf(targetPeriod), rollingDays, HabitScheduleType.valueOf(scheduleType),
        scheduleInterval, weekdaysMask.toWeekdays(), flexibleTimesPerWeek,
        LocalDate.ofEpochDay(startEpochDay), HabitEndType.valueOf(endType),
        endEpochDay?.let(LocalDate::ofEpochDay), endValue, quickIncrement,
        quickActionsCsv.split(',').mapNotNull(String::toDoubleOrNull),
        reminderMinutesCsv.split(',').mapNotNull(String::toIntOrNull),
        weekdayReminderMinutesCsv.fromReminderCsv(),
        java.time.DayOfWeek.valueOf(weekStart), timerStartedAtMillis, pinned, position,
        archived, paused, createdAtMillis, updatedAtMillis, sourceMeasurementId, autoCompleteFromItems,
        timerSessionId, timerNeedsReview, timerAccumulatedSeconds, timerAnchorElapsedRealtimeMillis,
    )
}

private fun HabitTimerSessionEntity.boundary(habit: HabitEntity) = HabitTimerBoundary(
    habitId = habitId,
    habitUuid = habit.uuid,
    sessionId = sessionId,
)

private fun timerSourceId(sessionId: String) = "habit-timer-v1:$sessionId"

private fun Double.validTimerSeconds(): Double = also {
    require(isFinite() && this >= 0.0) { "Timer duration must be a non-negative finite number" }
}

private fun estimatedTimerSeconds(
    session: HabitTimerSessionEntity,
    reading: com.whip.app.core.HabitTimerClockReading,
): Double {
    val accumulated = session.accumulatedCanonicalSeconds ?: 0.0
    val anchor = session.anchorWallMillis ?: return accumulated.coerceAtLeast(0.0)
    val wallDelta = ((reading.wallMillis.toDouble() - anchor.toDouble()) / 1_000.0).coerceAtLeast(0.0)
    return (accumulated + wallDelta).takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: accumulated.coerceAtLeast(0.0)
}

private fun HabitChecklistItemEntity.toDomain() = HabitChecklistItem(id, uuid, habitId, name, position, archived, createdAtMillis, updatedAtMillis)
private fun HabitLogEntity.toDomain() = HabitLog(id, uuid, habitId, value, canonicalValue, enteredUnitId, HabitLogStatus.valueOf(status), Instant.ofEpochMilli(timestampMillis), LocalDate.ofEpochDay(localEpochDay), zoneId, offsetSeconds, note, MeasurementSourceType.valueOf(sourceType), sourceId, measurementEntryId, createdAtMillis, updatedAtMillis)
private fun HabitChecklistStateEntity.toDomain() = HabitChecklistState(habitId, itemId, LocalDate.ofEpochDay(localEpochDay), completed, completedAtMillis, nameSnapshot)
private fun HabitPauseEntity.toDomain() = HabitPause(id, habitId, LocalDate.ofEpochDay(startEpochDay), endEpochDay?.let(LocalDate::ofEpochDay), note)
private fun HabitSkipEntity.toDomain() = HabitSkip(uuid, habitId, LocalDate.ofEpochDay(localEpochDay), skippedAtMillis, createdAtMillis, updatedAtMillis)

private fun Habit.toDraft(items: List<HabitChecklistItemEntity>) = HabitDraft(
    name = name,
    notes = notes,
    areaId = areaId,
    area = area,
    tags = tags,
    icon = icon,
    trackingMode = trackingMode,
    dimension = dimension,
    unitId = unitId,
    precision = precision,
    comparison = comparison,
    targetMin = targetMin,
    targetMax = targetMax,
    targetPeriod = targetPeriod,
    rollingDays = rollingDays,
    scheduleType = scheduleType,
    scheduleInterval = scheduleInterval,
    weekdays = weekdays,
    flexibleTimesPerWeek = flexibleTimesPerWeek,
    startDate = startDate,
    endType = endType,
    endDate = endDate,
    endValue = endValue,
    quickIncrement = quickIncrement,
    quickActions = quickActions,
    reminderMinutes = reminderMinutes,
    weekdayReminderMinutes = weekdayReminderMinutes,
    weekStart = weekStart,
    checklistItems = items.mapIndexed { index, item ->
        HabitChecklistItemDraft(item.name, index, id = item.id, uuid = item.uuid)
    },
    autoCompleteFromItems = autoCompleteFromItems,
    sourceMeasurementId = sourceMeasurementId,
)

private fun Map<java.time.DayOfWeek, List<Int>>.toReminderCsv(): String = entries
    .sortedBy { it.key.value }
    .joinToString(";") { (day, times) -> "${day.name}=${times.distinct().sorted().joinToString(",")}" }

private fun String.fromReminderCsv(): Map<java.time.DayOfWeek, List<Int>> = split(';').mapNotNull { segment ->
    val pieces = segment.split('=', limit = 2)
    val day = pieces.getOrNull(0)?.let { runCatching { java.time.DayOfWeek.valueOf(it) }.getOrNull() }
        ?: return@mapNotNull null
    day to pieces.getOrNull(1).orEmpty().split(',').mapNotNull(String::toIntOrNull).filter { it in 0..1439 }
}.toMap()
