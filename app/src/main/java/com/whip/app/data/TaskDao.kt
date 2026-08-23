package com.whip.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE archived = 0 ORDER BY createdAtMillis ASC")
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAtMillis ASC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task_occurrences")
    fun observeOccurrences(): Flow<List<TaskOccurrenceEntity>>

    @Query("SELECT * FROM task_steps ORDER BY taskId, position, id")
    fun observeSteps(): Flow<List<TaskStepEntity>>

    @Query("SELECT * FROM task_step_states")
    fun observeStepStates(): Flow<List<TaskStepStateEntity>>

    @Query("SELECT * FROM task_step_snapshots ORDER BY taskId, occurrenceKey, position")
    fun observeStepSnapshots(): Flow<List<TaskStepSnapshotEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT COALESCE(MAX(manualPosition), -1) + 1 FROM tasks")
    suspend fun nextManualPosition(): Int

    @Query("SELECT * FROM task_occurrences")
    suspend fun getAllOccurrences(): List<TaskOccurrenceEntity>

    @Query("SELECT * FROM task_steps")
    suspend fun getAllSteps(): List<TaskStepEntity>

    @Query("SELECT * FROM task_step_states")
    suspend fun getAllStepStates(): List<TaskStepStateEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTask(taskId: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE archived = 0")
    suspend fun getActiveTasks(): List<TaskEntity>

    @Query(
        "SELECT id FROM tasks " +
            "WHERE archived = 0 AND reminderEnabled = 1 AND timeMinutes IS NOT NULL",
    )
    suspend fun getReminderTaskIds(): List<Long>

    @Query(
        "SELECT id FROM tasks " +
            "WHERE archived = 0 AND completedAtMillis IS NULL AND locationReminderEnabled = 1",
    )
    suspend fun getLocationReminderTaskIds(): List<Long>

    @Query("SELECT * FROM task_occurrences WHERE taskId = :taskId")
    suspend fun getOccurrences(taskId: Long): List<TaskOccurrenceEntity>

    @Query("SELECT * FROM task_steps WHERE taskId = :taskId ORDER BY position, id")
    suspend fun getSteps(taskId: Long): List<TaskStepEntity>

    @Query(
        "SELECT * FROM task_step_states " +
            "WHERE taskId = :taskId AND occurrenceKey = :occurrenceKey",
    )
    suspend fun getStepStates(taskId: Long, occurrenceKey: Long): List<TaskStepStateEntity>

    @Query(
        "SELECT * FROM task_step_snapshots " +
            "WHERE taskId = :taskId AND occurrenceKey = :occurrenceKey ORDER BY position",
    )
    suspend fun getStepSnapshots(
        taskId: Long,
        occurrenceKey: Long,
    ): List<TaskStepSnapshotEntity>

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Insert
    suspend fun insertStep(step: TaskStepEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Update
    suspend fun updateStep(step: TaskStepEntity)

    @Upsert
    suspend fun upsertOccurrence(occurrence: TaskOccurrenceEntity)

    @Upsert
    suspend fun upsertStepState(state: TaskStepStateEntity)

    @Upsert
    suspend fun upsertStepSnapshot(snapshot: TaskStepSnapshotEntity)

    @Query(
        "DELETE FROM task_occurrences " +
            "WHERE taskId = :taskId AND originalEpochDay >= :fromEpochDay",
    )
    suspend fun deleteOccurrencesFrom(taskId: Long, fromEpochDay: Long)

    @Query(
        "DELETE FROM task_step_states " +
            "WHERE taskId = :taskId AND occurrenceKey >= :fromOccurrenceKey",
    )
    suspend fun deleteStepStatesFrom(taskId: Long, fromOccurrenceKey: Long)

    @Query(
        "DELETE FROM task_step_snapshots " +
            "WHERE taskId = :taskId AND occurrenceKey >= :fromOccurrenceKey",
    )
    suspend fun deleteStepSnapshotsFrom(taskId: Long, fromOccurrenceKey: Long)

    @Query(
        "DELETE FROM task_step_snapshots " +
            "WHERE taskId = :taskId AND occurrenceKey = :occurrenceKey",
    )
    suspend fun deleteStepSnapshotsForOccurrence(taskId: Long, occurrenceKey: Long)

    @Query(
        "DELETE FROM task_occurrences " +
            "WHERE taskId = :taskId AND originalEpochDay = :originalEpochDay",
    )
    suspend fun deleteOccurrence(taskId: Long, originalEpochDay: Long): Int

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long): Int
}
