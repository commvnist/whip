package com.whip.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = AreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("uuid", unique = true), Index("metricId", unique = true), Index("status"), Index("pinned"), Index("areaId")],
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val metricId: String,
    val name: String,
    val description: String,
    val areaId: String? = null,
    val area: String,
    val tagsCsv: String,
    val icon: String,
    val type: String,
    val dimension: String,
    val unitId: String,
    val precision: Int,
    val baseline: Double?,
    val targetMin: Double?,
    val targetMax: Double?,
    val direction: String,
    val startEpochDay: Long,
    val deadlineEpochDay: Long?,
    val aggregation: String,
    val aggregationPeriod: String,
    val rollingDays: Int?,
    val paceType: String,
    val consistencyPeriod: String,
    val consistencyRequiredPeriods: Int?,
    val reminderMinutes: Int?,
    val status: String,
    val pinned: Boolean,
    val position: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "goal_milestones",
    foreignKeys = [
        ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("uuid", unique = true), Index("goalId")],
)
data class GoalMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val goalId: Long,
    val name: String,
    val position: Int,
    val weight: Double,
    val completed: Boolean,
    val completedAtMillis: Long?,
    val reward: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
