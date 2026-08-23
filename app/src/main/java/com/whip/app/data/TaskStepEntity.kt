package com.whip.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_steps",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId"), Index("uuid", unique = true)],
)
data class TaskStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String,
    val taskId: Long,
    val title: String,
    val position: Int,
    val notes: String,
    /** Retained in the database for backup compatibility; subtask progress is always equal-weight. */
    val weight: Double,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
