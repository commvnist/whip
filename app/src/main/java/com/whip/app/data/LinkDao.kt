package com.whip.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    @Query("SELECT * FROM link_rules ORDER BY enabled DESC, createdAtMillis, id")
    fun observeRules(): Flow<List<LinkRuleEntity>>

    @Query("SELECT * FROM link_rule_conditions ORDER BY linkRuleId, position")
    fun observeRuleConditions(): Flow<List<LinkRuleConditionEntity>>

    @Query("SELECT * FROM link_condition_choices ORDER BY conditionId, optionId")
    fun observeLinkConditionChoices(): Flow<List<LinkConditionChoiceEntity>>

    @Query("SELECT * FROM contributions ORDER BY timestampMillis DESC, id DESC")
    fun observeContributions(): Flow<List<ContributionEntity>>

    @Query("SELECT * FROM trigger_rules ORDER BY enabled DESC, createdAtMillis, id")
    fun observeTriggerRules(): Flow<List<TriggerRuleEntity>>

    @Query("SELECT * FROM trigger_rule_conditions ORDER BY triggerRuleId, position")
    fun observeTriggerConditions(): Flow<List<TriggerRuleConditionEntity>>

    @Query("SELECT * FROM trigger_condition_choices ORDER BY conditionId, optionId")
    fun observeTriggerConditionChoices(): Flow<List<TriggerConditionChoiceEntity>>

    @Query("SELECT * FROM trigger_field_mappings ORDER BY triggerRuleId, id")
    fun observeTriggerMappings(): Flow<List<TriggerFieldMappingEntity>>

    @Query("SELECT * FROM trigger_occurrences ORDER BY availableAtMillis DESC, id DESC")
    fun observeTriggerOccurrences(): Flow<List<TriggerOccurrenceEntity>>

    @Query("SELECT * FROM link_rules WHERE id = :id")
    suspend fun getRule(id: Long): LinkRuleEntity?

    @Query("SELECT * FROM link_rules")
    suspend fun getRules(): List<LinkRuleEntity>

    @Query("SELECT * FROM link_rule_conditions WHERE linkRuleId = :ruleId ORDER BY position")
    suspend fun getRuleConditions(ruleId: Long): List<LinkRuleConditionEntity>

    @Query("SELECT * FROM link_rule_conditions ORDER BY linkRuleId, position")
    suspend fun getAllRuleConditions(): List<LinkRuleConditionEntity>

    @Query("SELECT * FROM link_condition_choices WHERE conditionId IN (:conditionIds)")
    suspend fun getLinkConditionChoices(conditionIds: List<Long>): List<LinkConditionChoiceEntity>

    @Query("SELECT * FROM contributions WHERE linkRuleId = :ruleId")
    suspend fun getContributions(ruleId: Long): List<ContributionEntity>

    @Query("SELECT * FROM contributions")
    suspend fun observeContributionsSnapshot(): List<ContributionEntity>

    @Query("SELECT * FROM contributions WHERE linkRuleId = :ruleId AND sourceEventId = :eventId")
    suspend fun getContribution(ruleId: Long, eventId: String): ContributionEntity?

    @Query("SELECT * FROM trigger_rules WHERE id = :id")
    suspend fun getTriggerRule(id: Long): TriggerRuleEntity?

    @Query("SELECT * FROM trigger_rules")
    suspend fun getTriggerRules(): List<TriggerRuleEntity>

    @Query("SELECT * FROM trigger_rule_conditions WHERE triggerRuleId = :ruleId ORDER BY position")
    suspend fun getTriggerConditions(ruleId: Long): List<TriggerRuleConditionEntity>

    @Query("SELECT * FROM trigger_rule_conditions ORDER BY triggerRuleId, position")
    suspend fun getAllTriggerConditions(): List<TriggerRuleConditionEntity>

    @Query("SELECT * FROM trigger_condition_choices WHERE conditionId IN (:conditionIds)")
    suspend fun getTriggerConditionChoices(conditionIds: List<Long>): List<TriggerConditionChoiceEntity>

    @Query("SELECT * FROM trigger_field_mappings WHERE triggerRuleId = :ruleId ORDER BY id")
    suspend fun getTriggerMappings(ruleId: Long): List<TriggerFieldMappingEntity>

    @Query("SELECT * FROM trigger_field_mappings ORDER BY triggerRuleId, id")
    suspend fun getAllTriggerMappings(): List<TriggerFieldMappingEntity>

    @Query("SELECT * FROM trigger_field_mappings WHERE targetFieldId = :fieldId ORDER BY id")
    suspend fun getTriggerMappingsForField(fieldId: Long): List<TriggerFieldMappingEntity>

    @Query("SELECT * FROM trigger_occurrences WHERE triggerRuleId = :ruleId")
    suspend fun getTriggerOccurrences(ruleId: Long): List<TriggerOccurrenceEntity>

    @Query("SELECT * FROM trigger_occurrences")
    suspend fun getAllTriggerOccurrences(): List<TriggerOccurrenceEntity>

    @Query("SELECT * FROM trigger_occurrences WHERE id = :id")
    suspend fun getTriggerOccurrence(id: Long): TriggerOccurrenceEntity?

    @Query("SELECT * FROM trigger_occurrences WHERE fulfilledEntryId = :entryId ORDER BY id")
    suspend fun getTriggerOccurrencesForFulfilledEntry(entryId: Long): List<TriggerOccurrenceEntity>

    @Insert suspend fun insertRule(entity: LinkRuleEntity): Long
    @Update suspend fun updateRule(entity: LinkRuleEntity)
    @Upsert suspend fun upsertContribution(entity: ContributionEntity): Long
    @Insert suspend fun insertTriggerRule(entity: TriggerRuleEntity): Long
    @Update suspend fun updateTriggerRule(entity: TriggerRuleEntity)
    @Upsert suspend fun upsertTriggerOccurrence(entity: TriggerOccurrenceEntity): Long
    @Insert suspend fun insertRuleCondition(entity: LinkRuleConditionEntity): Long
    @Insert suspend fun insertTriggerCondition(entity: TriggerRuleConditionEntity): Long
    @Insert suspend fun insertTriggerMapping(entity: TriggerFieldMappingEntity): Long
    @Insert suspend fun insertLinkConditionChoice(entity: LinkConditionChoiceEntity)
    @Insert suspend fun insertTriggerConditionChoice(entity: TriggerConditionChoiceEntity)

    @Query("SELECT COUNT(*) FROM link_rule_conditions WHERE fieldId = :fieldId")
    suspend fun countLinkConditionsForField(fieldId: Long): Int

    @Query("SELECT COUNT(*) FROM trigger_rule_conditions WHERE fieldId = :fieldId")
    suspend fun countTriggerConditionsForField(fieldId: Long): Int

    @Query("SELECT COUNT(*) FROM trigger_field_mappings WHERE targetFieldId = :fieldId")
    suspend fun countTriggerMappingsForField(fieldId: Long): Int

    @Query("SELECT COUNT(*) FROM link_rules WHERE sourceFieldId = :fieldId")
    suspend fun countLinkRulesForSourceField(fieldId: Long): Int

    @Query("SELECT COUNT(*) FROM link_condition_choices WHERE optionId = :optionId")
    suspend fun countLinkConditionsForOption(optionId: Long): Int

    @Query("SELECT COUNT(*) FROM trigger_condition_choices WHERE optionId = :optionId")
    suspend fun countTriggerConditionsForOption(optionId: Long): Int

    @Query("SELECT COUNT(*) FROM trigger_field_mappings WHERE constantChoiceOptionId = :optionId")
    suspend fun countTriggerMappingsForOption(optionId: Long): Int

    @Query("DELETE FROM link_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("DELETE FROM contributions WHERE id = :id")
    suspend fun deleteContribution(id: Long)

    @Query("DELETE FROM trigger_rules WHERE id = :id")
    suspend fun deleteTriggerRule(id: Long)

    @Query("DELETE FROM link_rule_conditions WHERE linkRuleId = :ruleId")
    suspend fun deleteRuleConditions(ruleId: Long): Int

    @Query("DELETE FROM trigger_rule_conditions WHERE triggerRuleId = :ruleId")
    suspend fun deleteTriggerConditions(ruleId: Long): Int

    @Query("DELETE FROM trigger_field_mappings WHERE triggerRuleId = :ruleId")
    suspend fun deleteTriggerMappings(ruleId: Long): Int

    @Query("UPDATE contributions SET excluded = :excluded, updatedAtMillis = :now WHERE id = :id")
    suspend fun setContributionExcluded(id: Long, excluded: Boolean, now: Long)

    @Query("UPDATE contributions SET overrideValue = :value, updatedAtMillis = :now WHERE id = :id")
    suspend fun setContributionOverride(id: Long, value: Double?, now: Long)

    @Query("UPDATE trigger_occurrences SET dismissedAtMillis = :now WHERE id = :id")
    suspend fun dismissTriggerOccurrence(id: Long, now: Long)

    @Query("UPDATE trigger_occurrences SET deliveredAtMillis = :now WHERE id = :id AND deliveredAtMillis IS NULL")
    suspend fun markTriggerOccurrenceDelivered(id: Long, now: Long): Int

    @Query("UPDATE trigger_occurrences SET remindAtMillis = :whenMillis, deliveredAtMillis = NULL, dismissedAtMillis = NULL WHERE id = :id")
    suspend fun remindTriggerOccurrence(id: Long, whenMillis: Long): Int

    @Query("UPDATE trigger_occurrences SET fulfilledEntryId = :entryId, deliveredAtMillis = :now, dismissedAtMillis = NULL, remindAtMillis = NULL WHERE id = :id")
    suspend fun fulfillTriggerOccurrence(id: Long, entryId: Long, now: Long): Int

    @Query("UPDATE trigger_occurrences SET fulfilledEntryId = :entryId WHERE id = :id AND fulfilledEntryId IS NULL")
    suspend fun restoreTriggerOccurrenceFulfillment(id: Long, entryId: Long): Int
}
