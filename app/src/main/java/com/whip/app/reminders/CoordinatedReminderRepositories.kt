package com.whip.app.reminders

import com.whip.app.data.GoalRepository
import com.whip.app.data.HabitRepository
import com.whip.app.data.MeasurementRepository
import com.whip.app.data.TaskBulkEdit
import com.whip.app.data.TaskRepository
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalMeasurementBoundary
import com.whip.app.domain.GoalMilestoneBoundary
import com.whip.app.domain.GoalMutationBoundary
import com.whip.app.domain.GoalProgressBoundary
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskEditBoundary
import com.whip.app.domain.UnitDimension
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Production repository decorators linearize reminder-relevant commits with
 * worker resolve/post. A mutation that commits first is visible to the worker;
 * a notification that posts first was current at that ordering point.
 */
internal class CoordinatedTaskRepository(
    private val delegate: TaskRepository,
    private val coordinator: ReminderDeliveryCoordinator,
) : TaskRepository by delegate {
    override suspend fun create(draft: TaskDraft) = mutate { delegate.create(draft) }
    override suspend fun update(taskId: Long, draft: TaskDraft, fromOccurrence: LocalDate?) =
        mutate { delegate.update(taskId, draft, fromOccurrence) }
    override suspend fun updateIfCurrent(
        expected: TaskEditBoundary,
        draft: TaskDraft,
        fromOccurrence: LocalDate?,
    ) = mutate { delegate.updateIfCurrent(expected, draft, fromOccurrence) }
    override suspend fun complete(item: ScheduledTask) = mutate { delegate.complete(item) }
    override suspend fun completeOccurrence(taskId: Long, originalDate: LocalDate?) =
        mutate { delegate.completeOccurrence(taskId, originalDate) }
    override suspend fun skip(item: ScheduledTask) = mutate { delegate.skip(item) }
    override suspend fun reschedule(item: ScheduledTask, newDate: LocalDate) =
        mutate { delegate.reschedule(item, newDate) }
    override suspend fun setStepCompleted(item: ScheduledTask, stepId: Long, completed: Boolean) =
        mutate { delegate.setStepCompleted(item, stepId, completed) }
    override suspend fun promoteStep(item: ScheduledTask, stepId: Long) =
        mutate { delegate.promoteStep(item, stepId) }
    override suspend fun archive(taskId: Long) = mutate { delegate.archive(taskId) }
    override suspend fun restore(taskId: Long) = mutate { delegate.restore(taskId) }
    override suspend fun deletePermanently(taskId: Long) = mutate { delegate.deletePermanently(taskId) }
    override suspend fun reopen(taskId: Long) = mutate { delegate.reopen(taskId) }
    override suspend fun reopenIfCurrent(item: ScheduledTask) = mutate { delegate.reopenIfCurrent(item) }
    override suspend fun reopenOccurrence(item: ScheduledTask) = mutate { delegate.reopenOccurrence(item) }
    override suspend fun reopenAllIfCurrent(items: List<ScheduledTask>) =
        mutate { delegate.reopenAllIfCurrent(items) }
    override suspend fun resetOccurrence(taskId: Long, originalDate: LocalDate) =
        mutate { delegate.resetOccurrence(taskId, originalDate) }
    override suspend fun resetOccurrenceIfCurrent(item: ScheduledTask) =
        mutate { delegate.resetOccurrenceIfCurrent(item) }
    override suspend fun setPinned(taskId: Long, pinned: Boolean) = mutate { delegate.setPinned(taskId, pinned) }
    override suspend fun duplicate(taskId: Long) = mutate { delegate.duplicate(taskId) }
    override suspend fun completeAll(items: List<ScheduledTask>) = mutate { delegate.completeAll(items) }
    override suspend fun rescheduleAll(items: List<ScheduledTask>, newDate: LocalDate) =
        mutate { delegate.rescheduleAll(items, newDate) }
    override suspend fun planAll(items: List<ScheduledTask>, newDate: LocalDate) =
        mutate { delegate.planAll(items, newDate) }
    override suspend fun restoreSchedulesIfCurrent(items: List<ScheduledTask>, expectedDate: LocalDate) =
        mutate { delegate.restoreSchedulesIfCurrent(items, expectedDate) }
    override suspend fun archiveAll(taskIds: List<Long>) = mutate { delegate.archiveAll(taskIds) }
    override suspend fun archiveAllIfCurrent(items: List<ScheduledTask>) =
        mutate { delegate.archiveAllIfCurrent(items) }
    override suspend fun restoreAll(taskIds: List<Long>) = mutate { delegate.restoreAll(taskIds) }
    override suspend fun setPinnedAll(taskIds: List<Long>, pinned: Boolean) =
        mutate { delegate.setPinnedAll(taskIds, pinned) }
    override suspend fun updateMetadataAll(taskIds: List<Long>, edit: TaskBulkEdit) =
        mutate { delegate.updateMetadataAll(taskIds, edit) }
    override suspend fun updateMetadataAllIfCurrent(items: List<ScheduledTask>, edit: TaskBulkEdit) =
        mutate { delegate.updateMetadataAllIfCurrent(items, edit) }
    override suspend fun reorderAll(taskIdsInOrder: List<Long>) = mutate { delegate.reorderAll(taskIdsInOrder) }
    override suspend fun undoPromoteStep(promotedTaskId: Long, sourceTaskId: Long, sourceStepId: Long) =
        mutate { delegate.undoPromoteStep(promotedTaskId, sourceTaskId, sourceStepId) }

    private suspend fun <T> mutate(block: suspend () -> T): T = coordinator.withStateBoundary(block)
}

internal class CoordinatedHabitRepository(
    private val delegate: HabitRepository,
    private val coordinator: ReminderDeliveryCoordinator,
) : HabitRepository by delegate {
    override suspend fun create(draft: HabitDraft) = mutate { delegate.create(draft) }
    override suspend fun update(id: Long, draft: HabitDraft) = mutate { delegate.update(id, draft) }
    override suspend fun duplicate(id: Long) = mutate { delegate.duplicate(id) }
    override suspend fun setArchived(id: Long, archived: Boolean) = mutate { delegate.setArchived(id, archived) }
    override suspend fun setPinned(id: Long, pinned: Boolean) = mutate { delegate.setPinned(id, pinned) }
    override suspend fun setPaused(id: Long, paused: Boolean) = mutate { delegate.setPaused(id, paused) }
    override suspend fun reorder(ids: List<Long>) = mutate { delegate.reorder(ids) }
    override suspend fun addPause(id: Long, start: LocalDate, end: LocalDate?, note: String) =
        mutate { delegate.addPause(id, start, end, note) }
    override suspend fun updatePause(
        pauseId: Long,
        start: LocalDate,
        end: LocalDate?,
        note: String,
        expectedHabitId: Long?,
    ) = mutate { delegate.updatePause(pauseId, start, end, note, expectedHabitId) }
    override suspend fun deletePause(pauseId: Long, expectedHabitId: Long?) =
        mutate { delegate.deletePause(pauseId, expectedHabitId) }
    override suspend fun skipDay(habitId: Long, date: LocalDate) = mutate { delegate.skipDay(habitId, date) }
    override suspend fun undoSkip(habitId: Long, date: LocalDate) = mutate { delegate.undoSkip(habitId, date) }
    override suspend fun log(
        habitId: Long,
        value: Double?,
        status: HabitLogStatus,
        date: LocalDate?,
        timestamp: Instant?,
        note: String,
        sourceType: MetricSourceType,
        sourceId: String?,
    ) = mutate { delegate.log(habitId, value, status, date, timestamp, note, sourceType, sourceId) }
    override suspend fun setPeriodValue(habitId: Long, date: LocalDate, value: Double, note: String) =
        mutate { delegate.setPeriodValue(habitId, date, value, note) }
    override suspend fun undoLog(logId: Long, expectedHabitId: Long?) =
        mutate { delegate.undoLog(logId, expectedHabitId) }
    override suspend fun updateLog(
        logId: Long,
        value: Double?,
        status: HabitLogStatus,
        date: LocalDate,
        note: String,
        enteredUnitId: String?,
        expectedHabitId: Long?,
    ) = mutate { delegate.updateLog(logId, value, status, date, note, enteredUnitId, expectedHabitId) }
    override suspend fun setCheckOff(habitId: Long, date: LocalDate, completed: Boolean) =
        mutate { delegate.setCheckOff(habitId, date, completed) }
    override suspend fun toggleChecklistItem(habitId: Long, itemId: Long, date: LocalDate, completed: Boolean) =
        mutate { delegate.toggleChecklistItem(habitId, itemId, date, completed) }
    override suspend fun startTimer(habitId: Long) = mutate { delegate.startTimer(habitId) }
    override suspend fun stopTimer(habitId: Long, date: LocalDate?) = mutate { delegate.stopTimer(habitId, date) }

    private suspend fun <T> mutate(block: suspend () -> T): T = coordinator.withStateBoundary(block)
}

internal class CoordinatedGoalRepository(
    private val delegate: GoalRepository,
    private val coordinator: ReminderDeliveryCoordinator,
) : GoalRepository by delegate {
    override suspend fun create(draft: GoalDraft) = mutate { delegate.create(draft) }
    override suspend fun update(id: Long, draft: GoalDraft) = mutate { delegate.update(id, draft) }
    override suspend fun update(boundary: GoalMutationBoundary, draft: GoalDraft) =
        mutate { delegate.update(boundary, draft) }
    override suspend fun duplicate(id: Long) = mutate { delegate.duplicate(id) }
    override suspend fun duplicate(boundary: GoalMutationBoundary) = mutate { delegate.duplicate(boundary) }
    override suspend fun setStatus(id: Long, status: GoalStatus) = mutate { delegate.setStatus(id, status) }
    override suspend fun setStatus(boundary: GoalMutationBoundary, status: GoalStatus) =
        mutate { delegate.setStatus(boundary, status) }
    override suspend fun setArchived(boundary: GoalMutationBoundary, archived: Boolean) =
        mutate { delegate.setArchived(boundary, archived) }
    override suspend fun setPinned(id: Long, pinned: Boolean) = mutate { delegate.setPinned(id, pinned) }
    override suspend fun setPinned(boundary: GoalMutationBoundary, pinned: Boolean) =
        mutate { delegate.setPinned(boundary, pinned) }
    override suspend fun reorder(ids: List<Long>) = mutate { delegate.reorder(ids) }
    override suspend fun recordMeasurement(id: Long, value: Double, date: LocalDate?, timestamp: Instant?, note: String) =
        mutate { delegate.recordMeasurement(id, value, date, timestamp, note) }
    override suspend fun recordMeasurement(
        boundary: GoalProgressBoundary,
        value: Double,
        date: LocalDate?,
        timestamp: Instant?,
        note: String,
    ) = mutate { delegate.recordMeasurement(boundary, value, date, timestamp, note) }
    override suspend fun updateMeasurement(
        id: Long,
        entryId: String,
        value: Double,
        date: LocalDate,
        note: String,
        enteredUnitId: String?,
    ) = mutate { delegate.updateMeasurement(id, entryId, value, date, note, enteredUnitId) }
    override suspend fun updateMeasurement(
        boundary: GoalMeasurementBoundary,
        value: Double,
        date: LocalDate,
        note: String,
        enteredUnitId: String?,
    ) = mutate { delegate.updateMeasurement(boundary, value, date, note, enteredUnitId) }
    override suspend fun deleteMeasurement(id: Long, entryId: String) =
        mutate { delegate.deleteMeasurement(id, entryId) }
    override suspend fun deleteMeasurement(boundary: GoalMeasurementBoundary) =
        mutate { delegate.deleteMeasurement(boundary) }
    override suspend fun toggleMilestone(id: Long, completed: Boolean) =
        mutate { delegate.toggleMilestone(id, completed) }
    override suspend fun toggleMilestone(boundary: GoalMilestoneBoundary, completed: Boolean) =
        mutate { delegate.toggleMilestone(boundary, completed) }
    override suspend fun resetElapsedStart(id: Long, start: Instant) =
        mutate { delegate.resetElapsedStart(id, start) }
    override suspend fun resetElapsedStart(boundary: GoalMutationBoundary, start: Instant) =
        mutate { delegate.resetElapsedStart(boundary, start) }

    private suspend fun <T> mutate(block: suspend () -> T): T = coordinator.withStateBoundary(block)
}

internal class CoordinatedMeasurementRepository(
    private val delegate: MeasurementRepository,
    private val coordinator: ReminderDeliveryCoordinator,
) : MeasurementRepository by delegate {
    override suspend fun createCustomUnit(
        name: String,
        symbol: String,
        dimension: UnitDimension,
        toCanonicalFactor: Double,
    ) = mutate { delegate.createCustomUnit(name, symbol, dimension, toCanonicalFactor) }
    override suspend fun ensureCustomUnit(
        id: String,
        name: String,
        symbol: String,
        dimension: UnitDimension,
        toCanonicalFactor: Double,
    ) = mutate { delegate.ensureCustomUnit(id, name, symbol, dimension, toCanonicalFactor) }
    override suspend fun renameCustomUnit(id: String, name: String, symbol: String) =
        mutate { delegate.renameCustomUnit(id, name, symbol) }
    override suspend fun setCustomUnitArchived(id: String, archived: Boolean) =
        mutate { delegate.setCustomUnitArchived(id, archived) }
    override suspend fun createCustomUnitVersion(
        sourceId: String,
        name: String,
        symbol: String,
        toCanonicalFactor: Double,
    ) = mutate { delegate.createCustomUnitVersion(sourceId, name, symbol, toCanonicalFactor) }
    override suspend fun record(
        metricId: String,
        value: Double?,
        unitId: String?,
        status: MetricEntryStatus,
        timestamp: Instant?,
        localDate: LocalDate?,
        zoneId: ZoneId?,
        sourceType: MetricSourceType,
        sourceId: String?,
        note: String,
        existingEntryId: String?,
        createIfMissingForHealthReconciliation: Boolean,
    ) = mutate {
        delegate.record(
            metricId, value, unitId, status, timestamp, localDate, zoneId,
            sourceType, sourceId, note, existingEntryId, createIfMissingForHealthReconciliation,
        )
    }
    override suspend fun deleteEntry(entryId: String) = mutate { delegate.deleteEntry(entryId) }
    override suspend fun deleteSourceEntriesExcept(
        sourceType: MetricSourceType,
        sourcePrefix: String,
        retainedEntryIds: Set<String>,
    ) = mutate { delegate.deleteSourceEntriesExcept(sourceType, sourcePrefix, retainedEntryIds) }

    private suspend fun <T> mutate(block: suspend () -> T): T = coordinator.withStateBoundary(block)
}
