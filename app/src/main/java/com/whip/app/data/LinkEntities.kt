package com.whip.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "link_rules",
    foreignKeys = [
        ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["targetGoalId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GoalMilestoneEntity::class, parentColumns = ["id"], childColumns = ["targetMilestoneId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("uuid", unique = true), Index("targetGoalId"), Index("targetMilestoneId"), Index(value = ["sourceType", "sourceEntityId"])],
)
data class LinkRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val kind: String,
    val sourceType: String,
    val sourceEntityId: Long?,
    val sourceMetricId: String?,
    val sourceItemId: Long?,
    val sourceMetric: String,
    val targetGoalId: Long,
    val targetMilestoneId: Long?,
    val valueMode: String,
    val fixedValue: Double?,
    val multiplier: Double,
    val offset: Double,
    val retroactiveFromEpochDay: Long?,
    val enabled: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "contributions",
    foreignKeys = [
        ForeignKey(entity = LinkRuleEntity::class, parentColumns = ["id"], childColumns = ["linkRuleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["targetGoalId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index("uuid", unique = true),
        Index(value = ["linkRuleId", "sourceEventId"], unique = true),
        Index("targetGoalId"),
        Index("metricEntryId", unique = true),
        Index(value = ["sourceType", "sourceEntityId"]),
    ],
)
data class ContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val linkRuleId: Long,
    val sourceEventId: String,
    val sourceType: String,
    val sourceEntityId: Long?,
    val targetGoalId: Long,
    val metricEntryId: String?,
    val canonicalValue: Double?,
    val localEpochDay: Long,
    val timestampMillis: Long,
    val excluded: Boolean,
    val overrideValue: Double?,
    val explanation: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "trigger_rules",
    indices = [Index("uuid", unique = true), Index(value = ["sourceType", "sourceEntityId"]), Index(value = ["targetType", "targetEntityId"])],
)
data class TriggerRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val sourceType: String,
    val sourceEntityId: Long,
    val outcome: String,
    val targetType: String,
    val targetEntityId: Long,
    val delayMinutes: Int,
    val quietStartMinutes: Int?,
    val quietEndMinutes: Int?,
    val autoCompleteTargetHabit: Boolean,
    val enabled: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "trigger_occurrences",
    foreignKeys = [ForeignKey(entity = TriggerRuleEntity::class, parentColumns = ["id"], childColumns = ["triggerRuleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["triggerRuleId", "sourceEventId"], unique = true), Index("availableAtMillis")],
)
data class TriggerOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerRuleId: Long,
    val sourceEventId: String,
    val availableAtMillis: Long,
    val deliveredAtMillis: Long?,
    val dismissedAtMillis: Long?,
)
