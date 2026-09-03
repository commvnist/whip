package com.whip.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY pinned DESC, position, name")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_checklist_items ORDER BY habitId, position, id")
    fun observeChecklistItems(): Flow<List<HabitChecklistItemEntity>>

    @Query("SELECT * FROM habit_logs ORDER BY timestampMillis DESC, id DESC")
    fun observeLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_checklist_states ORDER BY localEpochDay DESC")
    fun observeChecklistStates(): Flow<List<HabitChecklistStateEntity>>

    @Query("SELECT * FROM habit_pauses ORDER BY startEpochDay DESC")
    fun observePauses(): Flow<List<HabitPauseEntity>>

    @Query("SELECT * FROM habit_skips ORDER BY localEpochDay DESC, skippedAtMillis DESC")
    fun observeSkips(): Flow<List<HabitSkipEntity>>

    @Query("SELECT * FROM habit_timer_sessions WHERE activeHabitId IS NOT NULL ORDER BY createdAtMillis")
    fun observeActiveTimerSessions(): Flow<List<HabitTimerSessionEntity>>

    @Query("SELECT * FROM habits")
    suspend fun getAllHabits(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE archived = 0")
    suspend fun getActiveHabits(): List<HabitEntity>

    @Query("SELECT id FROM habits WHERE archived = 0 AND (reminderMinutesCsv != '' OR weekdayReminderMinutesCsv != '')")
    suspend fun getReminderHabitIds(): List<Long>

    @Query(
        """SELECT id FROM habits
            WHERE archived = 0
              AND sourceMetricId = :sourceMetricId
              AND (reminderMinutesCsv != '' OR weekdayReminderMinutesCsv != '')""",
    )
    suspend fun getReminderHabitIdsForSourceMetric(sourceMetricId: String): List<Long>

    @Query(
        """SELECT DISTINCT habits.id FROM habits
            LEFT JOIN habit_logs ON habit_logs.habitId = habits.id
            WHERE habits.archived = 0
              AND (habits.unitId = :unitId OR habit_logs.enteredUnitId = :unitId)
              AND (habits.reminderMinutesCsv != '' OR habits.weekdayReminderMinutesCsv != '')""",
    )
    suspend fun getReminderHabitIdsForUnit(unitId: String): List<Long>

    @Query("SELECT * FROM habit_pauses WHERE habitId = :habitId")
    suspend fun getPauses(habitId: Long): List<HabitPauseEntity>

    @Query("SELECT * FROM habit_pauses WHERE id = :id")
    suspend fun getPause(id: Long): HabitPauseEntity?

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllLogs(): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    suspend fun getLogsForHabit(habitId: Long): List<HabitLogEntity>

    @Query("SELECT * FROM habit_skips")
    suspend fun getAllSkips(): List<HabitSkipEntity>

    @Query("SELECT * FROM habit_skips WHERE habitId = :habitId")
    suspend fun getSkips(habitId: Long): List<HabitSkipEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: Long): HabitEntity?

    @Query("SELECT * FROM habit_timer_sessions WHERE sessionId = :sessionId")
    suspend fun getTimerSession(sessionId: String): HabitTimerSessionEntity?

    @Query("SELECT * FROM habit_timer_sessions WHERE activeHabitId = :habitId LIMIT 1")
    suspend fun getActiveTimerSession(habitId: Long): HabitTimerSessionEntity?

    @Query("SELECT * FROM habit_timer_sessions WHERE activeHabitId IS NOT NULL")
    suspend fun getActiveTimerSessions(): List<HabitTimerSessionEntity>

    @Query("SELECT * FROM habit_checklist_items WHERE habitId = :habitId ORDER BY position, id")
    suspend fun getChecklistItems(habitId: Long): List<HabitChecklistItemEntity>

    @Query("SELECT * FROM habit_logs WHERE id = :id")
    suspend fun getLog(id: Long): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND localEpochDay = :epochDay ORDER BY timestampMillis, id")
    suspend fun getLogsForDate(habitId: Long, epochDay: Long): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE sourceType = :sourceType AND sourceId = :sourceId LIMIT 1")
    suspend fun getLogBySource(sourceType: String, sourceId: String): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE sourceId LIKE :sourcePrefix")
    suspend fun getLogsBySourcePrefix(sourcePrefix: String): List<HabitLogEntity>

    @Query("SELECT COUNT(*) FROM habit_checklist_states WHERE habitId = :habitId AND localEpochDay = :epochDay AND completed = 1")
    suspend fun completedChecklistCount(habitId: Long, epochDay: Long): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM habits")
    suspend fun nextPosition(): Int

    @Insert suspend fun insertHabit(entity: HabitEntity): Long
    @Insert suspend fun insertChecklistItem(entity: HabitChecklistItemEntity): Long
    @Insert suspend fun insertLog(entity: HabitLogEntity): Long
    @Insert suspend fun insertPause(entity: HabitPauseEntity): Long
    @Insert suspend fun insertTimerSession(entity: HabitTimerSessionEntity)
    @Upsert suspend fun upsertSkip(entity: HabitSkipEntity)

    @Update suspend fun updateHabit(entity: HabitEntity)
    @Update suspend fun updateChecklistItem(entity: HabitChecklistItemEntity)
    @Update suspend fun updateLog(entity: HabitLogEntity)
    @Update suspend fun updatePause(entity: HabitPauseEntity)
    @Update suspend fun updateTimerSession(entity: HabitTimerSessionEntity)

    @Upsert suspend fun upsertChecklistState(entity: HabitChecklistStateEntity)

    @Query("DELETE FROM habit_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("DELETE FROM habit_pauses WHERE id = :id")
    suspend fun deletePause(id: Long): Int

    @Query("DELETE FROM habit_checklist_items WHERE id = :id")
    suspend fun deleteChecklistItem(id: Long): Int

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long): Int

    @Query("DELETE FROM habit_checklist_states WHERE habitId = :habitId AND localEpochDay = :epochDay")
    suspend fun deleteChecklistStatesForDate(habitId: Long, epochDay: Long)

    @Query("DELETE FROM habit_skips WHERE habitId = :habitId AND localEpochDay = :epochDay")
    suspend fun deleteSkip(habitId: Long, epochDay: Long): Int
}
