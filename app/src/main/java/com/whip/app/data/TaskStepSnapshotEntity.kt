package com.whip.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "task_step_snapshots",
    primaryKeys = ["taskId", "occurrenceKey", "stepId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId"), Index(value = ["taskId", "occurrenceKey"])],
)
data class TaskStepSnapshotEntity(
    val taskId: Long,
    val occurrenceKey: Long,
    val stepId: Long,
    val title: String,
    val position: Int,
    val notes: String,
    /** Retained in the database for backup compatibility; subtask progress is always equal-weight. */
    val weight: Double,
    val completed: Boolean,
    val completedAtMillis: Long?,
)
