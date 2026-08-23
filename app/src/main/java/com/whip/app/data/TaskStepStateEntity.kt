package com.whip.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "task_step_states",
    primaryKeys = ["stepId", "occurrenceKey"],
    foreignKeys = [
        ForeignKey(
            entity = TaskStepEntity::class,
            parentColumns = ["id"],
            childColumns = ["stepId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("stepId"), Index("taskId"), Index(value = ["taskId", "occurrenceKey"])],
)
data class TaskStepStateEntity(
    val stepId: Long,
    val taskId: Long,
    val occurrenceKey: Long,
    val completed: Boolean,
    val completedAtMillis: Long?,
    val titleSnapshot: String,
)
