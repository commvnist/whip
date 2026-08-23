package com.whip.app.data

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
    indices = [Index("uuid", unique = true), Index("metricId", unique = true), Index("archived"), Index("pinned"), Index("areaId")],
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val metricId: String,
    val name: String,
    val notes: String,
    val areaId: String? = null,
    val area: String,
    val tagsCsv: String,
    val icon: String,
    val colorArgb: Long?,
    val intent: String,
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
    val timeWindowStartMinutes: Int?,
    val timeWindowEndMinutes: Int?,
    val quickIncrement: Double,
    val quickActionsCsv: String,
    val reminderMinutesCsv: String,
    val weekdayReminderMinutesCsv: String,
    val weekStart: String,
    val avoidMissingPolicy: String,
    val timerStartedAtMillis: Long?,
    val pinned: Boolean,
    val position: Int,
    val archived: Boolean,
    val paused: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sourceMetricId: String? = null,
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
    val metricEntryId: String?,
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
