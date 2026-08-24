package com.whip.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = AreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("uuid", unique = true), Index("areaId")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String,
    val title: String,
    val notes: String,
    val scheduleKind: String,
    val dateEpochDay: Long?,
    val recurrenceUnit: String?,
    val recurrenceInterval: Int,
    val weekdaysMask: Int,
    val recurrenceEnd: String?,
    val recurrenceEndEpochDay: Long?,
    val recurrenceCount: Int?,
    val timeMinutes: Int?,
    val reminderEnabled: Boolean,
    val archived: Boolean,
    val completedAtMillis: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val showSubtaskProgress: Boolean,
    val progressDisplay: String,
    val autoCompleteFromSteps: Boolean,
    val repeatStepPolicy: String,
    val pinned: Boolean,
    val priority: String,
    val areaId: String? = null,
    val area: String,
    val tagsCsv: String,
    val deadlineEpochDay: Long?,
    val recurrenceAnchor: String,
    val reminderOffsetsMinutesCsv: String,
    val missedOccurrencePolicy: String = "KeepLatest",
    val inbox: Boolean = false,
    val durationMinutes: Int? = null,
    val effort: String = "Unspecified",
    val manualPosition: Int = 0,
)
