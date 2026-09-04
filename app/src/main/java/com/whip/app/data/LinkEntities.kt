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
        ForeignKey(entity = TrackFieldEntity::class, parentColumns = ["id"], childColumns = ["sourceFieldId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("uuid", unique = true), Index("targetGoalId"), Index("targetMilestoneId"), Index("sourceFieldId"), Index(value = ["sourceType", "sourceEntityId"])],
)
data class LinkRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val kind: String,
    val sourceType: String,
    val sourceEntityId: Long?,
    val sourceMeasurementId: String?,
    val sourceItemId: Long? = null,
    val sourceMeasurement: String,
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
    val trackAggregation: String?,
    val sourceFieldId: Long?,
    val conditionMode: String,
)

@Entity(
    tableName = "link_rule_conditions",
    foreignKeys = [
        ForeignKey(entity = LinkRuleEntity::class, parentColumns = ["id"], childColumns = ["linkRuleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackFieldEntity::class, parentColumns = ["id"], childColumns = ["fieldId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("linkRuleId"), Index("fieldId"), Index(value = ["linkRuleId", "position"], unique = true)],
)
data class LinkRuleConditionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val linkRuleId: Long,
    val fieldId: Long?,
    val entryDate: Boolean,
    val operator: String,
    val position: Int,
    val textValue: String?,
    val numberValue: Double?,
    val secondNumberValue: Double?,
    val dateEpochDay: Long?,
    val secondDateEpochDay: Long?,
)

@Entity(
    tableName = "link_condition_choices",
    primaryKeys = ["conditionId", "optionId"],
    foreignKeys = [
        ForeignKey(entity = LinkRuleConditionEntity::class, parentColumns = ["id"], childColumns = ["conditionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackChoiceOptionEntity::class, parentColumns = ["id"], childColumns = ["optionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("conditionId"), Index("optionId")],
)
data class LinkConditionChoiceEntity(val conditionId: Long, val optionId: Long)

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
        Index("measurementEntryId", unique = true),
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
    val measurementEntryId: String?,
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
    val sourceItemId: Long? = null,
    val outcome: String,
    val targetType: String,
    val targetEntityId: Long,
    val delayMinutes: Int,
    val quietStartMinutes: Int?,
    val quietEndMinutes: Int?,
    val action: String,
    val notificationEnabled: Boolean,
    val conditionMode: String,
    val enabled: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "trigger_occurrences",
    foreignKeys = [
        ForeignKey(entity = TriggerRuleEntity::class, parentColumns = ["id"], childColumns = ["triggerRuleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackEntryEntity::class, parentColumns = ["id"], childColumns = ["fulfilledEntryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index(value = ["triggerRuleId", "sourceEventId"], unique = true), Index("availableAtMillis"), Index("fulfilledEntryId")],
)
data class TriggerOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerRuleId: Long,
    val sourceEventId: String,
    val availableAtMillis: Long,
    val deliveredAtMillis: Long?,
    val dismissedAtMillis: Long?,
    val remindAtMillis: Long?,
    val fulfilledEntryId: Long?,
    val sourceSnapshot: String,
)

@Entity(
    tableName = "trigger_rule_conditions",
    foreignKeys = [
        ForeignKey(entity = TriggerRuleEntity::class, parentColumns = ["id"], childColumns = ["triggerRuleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackFieldEntity::class, parentColumns = ["id"], childColumns = ["fieldId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("triggerRuleId"), Index("fieldId"), Index(value = ["triggerRuleId", "position"], unique = true)],
)
data class TriggerRuleConditionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerRuleId: Long,
    val fieldId: Long?,
    val entryDate: Boolean,
    val operator: String,
    val position: Int,
    val textValue: String?,
    val numberValue: Double?,
    val secondNumberValue: Double?,
    val dateEpochDay: Long?,
    val secondDateEpochDay: Long?,
)

@Entity(
    tableName = "trigger_condition_choices",
    primaryKeys = ["conditionId", "optionId"],
    foreignKeys = [
        ForeignKey(entity = TriggerRuleConditionEntity::class, parentColumns = ["id"], childColumns = ["conditionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackChoiceOptionEntity::class, parentColumns = ["id"], childColumns = ["optionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("conditionId"), Index("optionId")],
)
data class TriggerConditionChoiceEntity(val conditionId: Long, val optionId: Long)

@Entity(
    tableName = "trigger_field_mappings",
    foreignKeys = [
        ForeignKey(entity = TriggerRuleEntity::class, parentColumns = ["id"], childColumns = ["triggerRuleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackFieldEntity::class, parentColumns = ["id"], childColumns = ["targetFieldId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackChoiceOptionEntity::class, parentColumns = ["id"], childColumns = ["constantChoiceOptionId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("triggerRuleId"), Index("targetFieldId"), Index("constantChoiceOptionId"), Index(value = ["triggerRuleId", "targetFieldId"], unique = true)],
)
data class TriggerFieldMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerRuleId: Long,
    val targetFieldId: Long,
    val sourceProperty: String,
    val constantText: String?,
    val constantNumber: Double?,
    val constantUnitId: String?,
    val constantDateEpochDay: Long?,
    val constantBoolean: Boolean?,
    val constantChoiceOptionId: Long?,
    val constantScale: Double?,
)
