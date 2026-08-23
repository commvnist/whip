package com.whip.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "task_occurrences",
    primaryKeys = ["taskId", "originalEpochDay"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class TaskOccurrenceEntity(
    val taskId: Long,
    val originalEpochDay: Long,
    val scheduledEpochDay: Long,
    val state: String,
    val completedAtMillis: Long?,
)
