package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.domain.AvoidMissingPolicy
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitChecklistItem
import com.whip.app.domain.HabitChecklistItemDraft
import com.whip.app.domain.HabitChecklistState
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitIntent
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitPause
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.MetricValueKind
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.toWeekdayMask
import com.whip.app.domain.toWeekdays
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface HabitRepository {
    val habits: Flow<List<Habit>>
    val checklistItems: Flow<List<HabitChecklistItem>>
    val logs: Flow<List<HabitLog>>
    val checklistStates: Flow<List<HabitChecklistState>>
    val pauses: Flow<List<HabitPause>>

    suspend fun create(draft: HabitDraft): Long
    suspend fun update(id: Long, draft: HabitDraft)
    suspend fun duplicate(id: Long): Long
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun setPinned(id: Long, pinned: Boolean)
    suspend fun setPaused(id: Long, paused: Boolean)
    suspend fun reorder(ids: List<Long>)
    suspend fun addPause(id: Long, start: LocalDate, end: LocalDate?, note: String = "")
    suspend fun log(
        habitId: Long,
        value: Double?,
        status: HabitLogStatus = HabitLogStatus.Recorded,
        date: LocalDate? = null,
        timestamp: Instant? = null,
        note: String = "",
        sourceType: MetricSourceType = MetricSourceType.Manual,
        sourceId: String? = null,
    ): Long
    suspend fun undoLog(logId: Long)
    suspend fun updateLog(
        logId: Long,
        value: Double?,
        status: HabitLogStatus,
        date: LocalDate,
        note: String = "",
        enteredUnitId: String? = null,
    )
    suspend fun setCheckOff(habitId: Long, date: LocalDate, completed: Boolean)
    suspend fun toggleChecklistItem(habitId: Long, itemId: Long, date: LocalDate, completed: Boolean)
    suspend fun startTimer(habitId: Long)
    suspend fun stopTimer(habitId: Long, date: LocalDate? = null): Long
}

class RoomHabitRepository(
    private val database: WhipDatabase,
    private val measurementRepository: MeasurementRepository,
    private val clock: WhipClock,
    private val ids: WhipIdGenerator,
) : HabitRepository {
    private val dao = database.habitDao()
    private val areaRepository = RoomAreaRepository(database, clock, ids)

    override val habits = dao.observeHabits().map { it.map(HabitEntity::toDomain) }
    override val checklistItems = dao.observeChecklistItems().map { it.map(HabitChecklistItemEntity::toDomain) }
    override val logs = dao.observeLogs().map { it.map(HabitLogEntity::toDomain) }
    override val checklistStates = dao.observeChecklistStates().map { it.map(HabitChecklistStateEntity::toDomain) }
    override val pauses = dao.observePauses().map { it.map(HabitPauseEntity::toDomain) }

    override suspend fun create(draft: HabitDraft): Long = database.withTransaction {
        validateHabit(draft)
        val area = areaRepository.resolve(draft.areaId, draft.area)
        val resolvedDraft = draft.copy(areaId = area.id, area = area.name)
        measurementRepository.ensureCustomUnit(draft.unitId, draft.unitId, draft.unitId, draft.dimension)
        val metricId = measurementRepository.createMetric(
            name = draft.name,
            valueKind = draft.trackingMode.metricValueKind(),
            dimension = draft.dimension,
            defaultUnitId = draft.unitId,
            precision = draft.precision,
        )
        val now = clock.now().toEpochMilli()
        val habitId = dao.insertHabit(
            resolvedDraft.toEntity(
                uuid = ids.nextId(),
                metricId = metricId,
                position = dao.nextPosition(),
                createdAtMillis = now,
            ),
        )
        syncChecklist(habitId, resolvedDraft.checklistItems, now)
        habitId
    }

    override suspend fun update(id: Long, draft: HabitDraft) = database.withTransaction {
        validateHabit(draft)
        val area = areaRepository.resolve(draft.areaId, draft.area)
        val resolvedDraft = draft.copy(areaId = area.id, area = area.name)
        measurementRepository.ensureCustomUnit(draft.unitId, draft.unitId, draft.unitId, draft.dimension)
        val existing = dao.getHabit(id) ?: error("Habit no longer exists")
        require(existing.dimension == draft.dimension.name) {
            "A habit's measurement dimension cannot change; create a new habit instead"
        }
        val now = clock.now().toEpochMilli()
        dao.updateHabit(
            resolvedDraft.toEntity(
                id = existing.id,
                uuid = existing.uuid,
                metricId = existing.metricId,
                timerStartedAtMillis = existing.timerStartedAtMillis,
                pinned = existing.pinned,
                position = existing.position,
                archived = existing.archived,
                paused = existing.paused,
                createdAtMillis = existing.createdAtMillis,
                updatedAtMillis = now,
            ),
        )
        syncChecklist(id, resolvedDraft.checklistItems, now)
    }

    override suspend fun duplicate(id: Long): Long {
        val habit = dao.getHabit(id)?.toDomain() ?: error("Habit no longer exists")
        val items = dao.getChecklistItems(id).filterNot { it.archived }
        return create(habit.toDraft(items).copy(name = "${habit.name} copy"))
    }

    override suspend fun setArchived(id: Long, archived: Boolean) = updateFlags(id) { it.copy(archived = archived) }
    override suspend fun setPinned(id: Long, pinned: Boolean) = updateFlags(id) { it.copy(pinned = pinned) }
    override suspend fun setPaused(id: Long, paused: Boolean) = updateFlags(id) { it.copy(paused = paused) }

    override suspend fun reorder(ids: List<Long>) = database.withTransaction {
        ids.forEachIndexed { index, id -> updateFlags(id) { it.copy(position = index) } }
    }

    override suspend fun addPause(id: Long, start: LocalDate, end: LocalDate?, note: String) {
        require(end == null || !end.isBefore(start)) { "Pause end cannot precede its start" }
        dao.insertPause(HabitPauseEntity(habitId = id, startEpochDay = start.toEpochDay(), endEpochDay = end?.toEpochDay(), note = note.trim()))
    }

    override suspend fun log(
        habitId: Long,
        value: Double?,
        status: HabitLogStatus,
        date: LocalDate?,
        timestamp: Instant?,
        note: String,
        sourceType: MetricSourceType,
        sourceId: String?,
    ): Long = database.withTransaction {
        if (sourceType != MetricSourceType.Manual && !sourceId.isNullOrBlank()) {
            dao.getLogBySource(sourceType.name, sourceId)?.let { return@withTransaction it.id }
        }
        val habit = dao.getHabit(habitId)?.toDomain() ?: error("Habit no longer exists")
        val instant = timestamp ?: clock.now()
        val zone = clock.zoneId()
        val localDate = date ?: timestamp?.atZone(zone)?.toLocalDate() ?: clock.today(zone)
        val logUuid = ids.nextId()
        val effectiveValue = when (habit.trackingMode) {
            HabitTrackingMode.CheckOff -> value ?: 1.0
            else -> value
        }
        val entryStatus = when (status) {
            HabitLogStatus.Skipped -> MetricEntryStatus.Skipped
            HabitLogStatus.Missing -> MetricEntryStatus.Missing
            HabitLogStatus.Failed -> MetricEntryStatus.Failed
            HabitLogStatus.Excused -> MetricEntryStatus.Excused
            else -> MetricEntryStatus.Recorded
        }
        val metricEntryId = measurementRepository.record(
            metricId = habit.metricId,
            value = effectiveValue,
            unitId = habit.unitId.takeIf { effectiveValue != null },
            status = entryStatus,
            timestamp = instant,
            localDate = localDate,
            zoneId = zone,
            sourceType = MetricSourceType.Habit,
            sourceId = logUuid,
            note = note,
        )
        val canonical = database.measurementDao().getEntry(metricEntryId)?.canonicalValue
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
                metricEntryId = metricEntryId,
                createdAtMillis = clock.now().toEpochMilli(),
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun undoLog(logId: Long) = database.withTransaction {
        val log = dao.getLog(logId) ?: return@withTransaction
        log.metricEntryId?.let { measurementRepository.deleteEntry(it) }
        dao.deleteLog(logId)
    }

    override suspend fun updateLog(
        logId: Long,
        value: Double?,
        status: HabitLogStatus,
        date: LocalDate,
        note: String,
        enteredUnitId: String?,
    ) = database.withTransaction {
        val existing = dao.getLog(logId) ?: error("Habit log no longer exists")
        val habit = dao.getHabit(existing.habitId)?.toDomain() ?: error("Habit no longer exists")
        val effectiveValue = if (habit.trackingMode == HabitTrackingMode.CheckOff && status in setOf(HabitLogStatus.Recorded, HabitLogStatus.Success)) {
            value ?: 1.0
        } else {
            value
        }
        val entryStatus = when (status) {
            HabitLogStatus.Skipped -> MetricEntryStatus.Skipped
            HabitLogStatus.Missing -> MetricEntryStatus.Missing
            HabitLogStatus.Failed -> MetricEntryStatus.Failed
            HabitLogStatus.Excused -> MetricEntryStatus.Excused
            else -> MetricEntryStatus.Recorded
        }
        val zone = ZoneId.of(existing.zoneId)
        val instant = Instant.ofEpochMilli(existing.timestampMillis)
        val effectiveUnitId = enteredUnitId ?: existing.enteredUnitId ?: habit.unitId
        val metricEntryId = measurementRepository.record(
            metricId = habit.metricId,
            value = effectiveValue,
            unitId = effectiveUnitId.takeIf { effectiveValue != null },
            status = entryStatus,
            timestamp = instant,
            localDate = date,
            zoneId = zone,
            sourceType = MetricSourceType.valueOf(existing.sourceType),
            sourceId = existing.sourceId,
            note = note,
            existingEntryId = existing.metricEntryId,
        )
        dao.updateLog(
            existing.copy(
                value = effectiveValue,
                canonicalValue = database.measurementDao().getEntry(metricEntryId)?.canonicalValue,
                enteredUnitId = effectiveUnitId.takeIf { effectiveValue != null },
                status = status.name,
                localEpochDay = date.toEpochDay(),
                note = note.trim(),
                metricEntryId = metricEntryId,
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun setCheckOff(habitId: Long, date: LocalDate, completed: Boolean) {
        database.withTransaction {
            val habit = dao.getHabit(habitId)?.toDomain() ?: error("Habit no longer exists")
            require(habit.trackingMode == HabitTrackingMode.CheckOff) { "This habit is not a check-off habit" }
            val positiveLogs = dao.getLogsForDate(habitId, date.toEpochDay())
                .filter { it.status in setOf(HabitLogStatus.Recorded.name, HabitLogStatus.Success.name) && (it.value ?: 0.0) > 0.0 }
            if (completed && positiveLogs.isEmpty()) {
                log(habitId, 1.0, HabitLogStatus.Success, date)
            } else if (!completed) {
                positiveLogs.forEach { entry ->
                    entry.metricEntryId?.let { measurementRepository.deleteEntry(it) }
                    dao.deleteLog(entry.id)
                }
            }
        }
    }

    override suspend fun toggleChecklistItem(
        habitId: Long,
        itemId: Long,
        date: LocalDate,
        completed: Boolean,
    ) {
        database.withTransaction {
        val item = dao.getChecklistItems(habitId).firstOrNull { it.id == itemId && !it.archived }
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
            log(habitId, dao.completedChecklistCount(habitId, date.toEpochDay()).toDouble(), date = date)
        }
    }

    override suspend fun startTimer(habitId: Long) {
        updateFlags(habitId) { it.copy(timerStartedAtMillis = clock.now().toEpochMilli()) }
    }

    override suspend fun stopTimer(habitId: Long, date: LocalDate?): Long = database.withTransaction {
        val habit = dao.getHabit(habitId) ?: error("Habit no longer exists")
        val start = habit.timerStartedAtMillis ?: error("Timer is not running")
        val seconds = ((clock.now().toEpochMilli() - start) / 1_000.0).coerceAtLeast(0.0)
        updateFlags(habitId) { it.copy(timerStartedAtMillis = null) }
        log(habitId, seconds, date = date)
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
        existing.filterNot { it.id in retainedIds }.forEach { dao.deleteChecklistItem(it.id) }
    }

    private suspend fun updateFlags(id: Long, transform: (HabitEntity) -> HabitEntity) {
        val current = dao.getHabit(id) ?: return
        dao.updateHabit(transform(current).copy(updatedAtMillis = clock.now().toEpochMilli()))
    }

}

private fun validateHabit(draft: HabitDraft) {
    require(draft.name.isNotBlank()) { "Habit name is required" }
    require(draft.scheduleInterval > 0) { "Schedule interval must be positive" }
    require(draft.quickIncrement.isFinite() && draft.quickIncrement > 0.0) { "Quick increment must be positive" }
    require(draft.quickActions.all { it.isFinite() && it >= 0.0 }) { "Quick actions must be non-negative numbers" }
    if (draft.scheduleType == HabitScheduleType.SelectedWeekdays) require(draft.weekdays.isNotEmpty()) { "Pick at least one weekday" }
    require(listOfNotNull(draft.targetMin, draft.targetMax).all(Double::isFinite)) { "Habit targets must be finite numbers" }
    when (draft.comparison) {
        TargetComparison.AtLeast, TargetComparison.Exactly -> require(draft.targetMin != null) { "Enter a target" }
        TargetComparison.AtMost -> require(draft.targetMin != null || draft.targetMax != null) { "Enter a maximum" }
        TargetComparison.WithinRange -> require(draft.targetMin != null && draft.targetMax != null && draft.targetMin <= draft.targetMax) { "Enter a valid target range" }
        TargetComparison.None -> Unit
    }
    if (draft.targetPeriod == TargetPeriod.RollingDays) require((draft.rollingDays ?: 0) > 0) { "Enter a positive rolling window" }
    when (draft.endType) {
        HabitEndType.Never -> Unit
        HabitEndType.OnDate -> require(draft.endDate != null && !draft.endDate.isBefore(draft.startDate)) { "Choose an end date on or after the start date" }
        HabitEndType.AfterStreak, HabitEndType.AfterCompletions -> require(
            draft.endValue?.let { it.isFinite() && it > 0.0 && it % 1.0 == 0.0 } == true,
        ) { "Enter a positive whole-number ending threshold" }
        HabitEndType.AfterTotal -> require(draft.endValue?.let { it.isFinite() && it > 0.0 } == true) { "Enter a positive ending total" }
    }
    if (draft.intent == HabitIntent.Avoid) require(draft.avoidMissingPolicy != AvoidMissingPolicy.Unknown || draft.comparison != TargetComparison.None) { "Choose how missing avoid-habit data is handled" }
}

private fun HabitTrackingMode.metricValueKind() = when (this) {
    HabitTrackingMode.CheckOff, HabitTrackingMode.LimitAvoid -> MetricValueKind.Boolean
    HabitTrackingMode.Count -> MetricValueKind.Integer
    HabitTrackingMode.Decimal, HabitTrackingMode.LogOnly -> MetricValueKind.Decimal
    HabitTrackingMode.Duration -> MetricValueKind.Duration
    HabitTrackingMode.Checklist -> MetricValueKind.Checklist
    HabitTrackingMode.Rating -> MetricValueKind.Rating
}

private fun HabitDraft.toEntity(
    id: Long = 0, uuid: String, metricId: String, timerStartedAtMillis: Long? = null,
    pinned: Boolean = false, position: Int, archived: Boolean = false, paused: Boolean = false,
    createdAtMillis: Long, updatedAtMillis: Long = createdAtMillis,
) = HabitEntity(
    id, uuid, metricId, name.trim(), notes.trim(), areaId, area.trim(), tags.map(String::trim).filter(String::isNotBlank).distinct().joinToString(","), icon, colorArgb, intent.name,
    trackingMode.name, dimension.name, unitId, precision, comparison.name, targetMin,
    targetMax, targetPeriod.name, rollingDays, scheduleType.name, scheduleInterval,
    weekdays.toWeekdayMask(), flexibleTimesPerWeek, startDate.toEpochDay(), endType.name,
    endDate?.takeIf { endType == HabitEndType.OnDate }?.toEpochDay(),
    endValue?.takeIf { endType in setOf(HabitEndType.AfterStreak, HabitEndType.AfterCompletions, HabitEndType.AfterTotal) },
    timeWindowStartMinutes, timeWindowEndMinutes,
    quickIncrement, quickActions.joinToString(","), reminderMinutes.joinToString(","),
    weekdayReminderMinutes.toReminderCsv(), weekStart.name, avoidMissingPolicy.name, timerStartedAtMillis, pinned, position,
    archived, paused, createdAtMillis, updatedAtMillis, sourceMetricId,
)

private fun HabitEntity.toDomain() = Habit(
    id, uuid, metricId, name, notes, areaId, area, tagsCsv.split(',').map(String::trim).filter(String::isNotBlank), icon, colorArgb, HabitIntent.valueOf(intent),
    HabitTrackingMode.valueOf(trackingMode), UnitDimension.valueOf(dimension), unitId,
    precision, TargetComparison.valueOf(comparison), targetMin, targetMax,
    TargetPeriod.valueOf(targetPeriod), rollingDays, HabitScheduleType.valueOf(scheduleType),
    scheduleInterval, weekdaysMask.toWeekdays(), flexibleTimesPerWeek,
    LocalDate.ofEpochDay(startEpochDay), HabitEndType.valueOf(endType),
    endEpochDay?.let(LocalDate::ofEpochDay), endValue, timeWindowStartMinutes,
    timeWindowEndMinutes, quickIncrement, quickActionsCsv.split(',').mapNotNull(String::toDoubleOrNull),
    reminderMinutesCsv.split(',').mapNotNull(String::toIntOrNull),
    weekdayReminderMinutesCsv.fromReminderCsv(),
    java.time.DayOfWeek.valueOf(weekStart), AvoidMissingPolicy.valueOf(avoidMissingPolicy),
    timerStartedAtMillis, pinned, position, archived, paused, createdAtMillis, updatedAtMillis, sourceMetricId,
)

private fun HabitChecklistItemEntity.toDomain() = HabitChecklistItem(id, uuid, habitId, name, position, archived, createdAtMillis, updatedAtMillis)
private fun HabitLogEntity.toDomain() = HabitLog(id, uuid, habitId, value, canonicalValue, enteredUnitId, HabitLogStatus.valueOf(status), Instant.ofEpochMilli(timestampMillis), LocalDate.ofEpochDay(localEpochDay), zoneId, offsetSeconds, note, MetricSourceType.valueOf(sourceType), sourceId, metricEntryId, createdAtMillis, updatedAtMillis)
private fun HabitChecklistStateEntity.toDomain() = HabitChecklistState(habitId, itemId, LocalDate.ofEpochDay(localEpochDay), completed, completedAtMillis, nameSnapshot)
private fun HabitPauseEntity.toDomain() = HabitPause(id, habitId, LocalDate.ofEpochDay(startEpochDay), endEpochDay?.let(LocalDate::ofEpochDay), note)

private fun Habit.toDraft(items: List<HabitChecklistItemEntity>) = HabitDraft(
    name, notes, areaId, area, tags, icon, colorArgb, intent, trackingMode, dimension, unitId, precision,
    comparison, targetMin, targetMax, targetPeriod, rollingDays, scheduleType,
    scheduleInterval, weekdays, flexibleTimesPerWeek, startDate, endType, endDate,
    endValue, timeWindowStartMinutes, timeWindowEndMinutes, quickIncrement, quickActions,
    reminderMinutes, weekdayReminderMinutes, weekStart, avoidMissingPolicy,
    items.mapIndexed { index, item ->
        HabitChecklistItemDraft(item.name, index, id = item.id, uuid = item.uuid)
    }, sourceMetricId,
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
