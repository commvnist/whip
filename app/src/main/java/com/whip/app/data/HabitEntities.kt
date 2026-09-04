package com.whip.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = AreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("uuid", unique = true), Index("measurementId", unique = true), Index("archived"), Index("pinned"), Index("areaId")],
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val measurementId: String,
    val name: String,
    val notes: String,
    val areaId: String? = null,
    val area: String,
    val tagsCsv: String,
    val icon: String,
    val trackingMode: String,
    val dimension: String,
    val unitId: String,
    val precision: Int,
    val comparison: String,
    val targetMin: Double?,
    val targetMax: Double?,
    val targetPeriod: String,
    val rollingDays: Int?,
    val scheduleType: String,
    val scheduleInterval: Int,
    val weekdaysMask: Int,
    val flexibleTimesPerWeek: Int?,
    val startEpochDay: Long,
    val endType: String,
    val endEpochDay: Long?,
    val endValue: Double?,
    val quickIncrement: Double,
    val quickActionsCsv: String,
    val reminderMinutesCsv: String,
    val weekdayReminderMinutesCsv: String,
    val weekStart: String,
    val timerStartedAtMillis: Long?,
    val pinned: Boolean,
    val position: Int,
    val archived: Boolean,
    val paused: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sourceMeasurementId: String? = null,
    @ColumnInfo(defaultValue = "1") val autoCompleteFromItems: Boolean = true,
    val timerSessionId: String? = null,
    @ColumnInfo(defaultValue = "0") val timerNeedsReview: Boolean = false,
    @ColumnInfo(defaultValue = "0") val timerAccumulatedSeconds: Double = 0.0,
    val timerAnchorElapsedRealtimeMillis: Long? = null,
)

@Entity(
    tableName = "habit_timer_sessions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId"), Index("activeHabitId", unique = true)],
)
data class HabitTimerSessionEntity(
    @PrimaryKey val sessionId: String,
    val habitId: Long,
    /** Equal to habitId only while unresolved; nullable uniqueness enforces one active session. */
    val activeHabitId: Long?,
    val state: String,
    val anchorWallMillis: Long?,
    val anchorElapsedRealtimeMillis: Long?,
    val anchorBootId: String?,
    val accumulatedCanonicalSeconds: Double?,
    val unitId: String?,
    val createdAtMillis: Long,
    val resolvedAtMillis: Long?,
)

@Entity(
    tableName = "habit_checklist_items",
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("uuid", unique = true), Index("habitId")],
)
data class HabitChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val habitId: Long,
    val name: String,
    val position: Int,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("uuid", unique = true), Index("habitId"), Index(value = ["habitId", "localEpochDay"]), Index(value = ["sourceType", "sourceId"])],
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val habitId: Long,
    val value: Double?,
    val canonicalValue: Double?,
    val enteredUnitId: String?,
    val status: String,
    val timestampMillis: Long,
    val localEpochDay: Long,
    val zoneId: String,
    val offsetSeconds: Int,
    val note: String,
    val sourceType: String,
    val sourceId: String?,
    val measurementEntryId: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "habit_checklist_states",
    primaryKeys = ["habitId", "itemId", "localEpochDay"],
    foreignKeys = [
        ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = HabitChecklistItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("habitId"), Index("itemId"), Index(value = ["habitId", "localEpochDay"])],
)
data class HabitChecklistStateEntity(
    val habitId: Long,
    val itemId: Long,
    val localEpochDay: Long,
    val completed: Boolean,
    val completedAtMillis: Long?,
    val nameSnapshot: String,
)

@Entity(
    tableName = "habit_pauses",
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId")],
)
data class HabitPauseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val startEpochDay: Long,
    val endEpochDay: Long?,
    val note: String,
)

/**
 * A deliberately skipped scheduled occurrence. This is separate from
 * [HabitLogEntity] because a skip is not a measurement and must never affect
 * totals, averages, or value-based Goal progress.
 */
@Entity(
    tableName = "habit_skips",
    primaryKeys = ["habitId", "localEpochDay"],
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("uuid", unique = true), Index("habitId")],
)
data class HabitSkipEntity(
    val uuid: String,
    val habitId: Long,
    val localEpochDay: Long,
    val skippedAtMillis: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
