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

    @Query("SELECT * FROM contributions ORDER BY timestampMillis DESC, id DESC")
    fun observeContributions(): Flow<List<ContributionEntity>>

    @Query("SELECT * FROM trigger_rules ORDER BY enabled DESC, createdAtMillis, id")
    fun observeTriggerRules(): Flow<List<TriggerRuleEntity>>

    @Query("SELECT * FROM trigger_occurrences ORDER BY availableAtMillis DESC, id DESC")
    fun observeTriggerOccurrences(): Flow<List<TriggerOccurrenceEntity>>

    @Query("SELECT * FROM link_rules WHERE id = :id")
    suspend fun getRule(id: Long): LinkRuleEntity?

    @Query("SELECT * FROM link_rules")
    suspend fun getRules(): List<LinkRuleEntity>

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

    @Query("SELECT * FROM trigger_occurrences WHERE triggerRuleId = :ruleId")
    suspend fun getTriggerOccurrences(ruleId: Long): List<TriggerOccurrenceEntity>

    @Query("SELECT * FROM trigger_occurrences WHERE id = :id")
    suspend fun getTriggerOccurrence(id: Long): TriggerOccurrenceEntity?

    @Insert suspend fun insertRule(entity: LinkRuleEntity): Long
    @Update suspend fun updateRule(entity: LinkRuleEntity)
    @Upsert suspend fun upsertContribution(entity: ContributionEntity): Long
    @Insert suspend fun insertTriggerRule(entity: TriggerRuleEntity): Long
    @Update suspend fun updateTriggerRule(entity: TriggerRuleEntity)
    @Upsert suspend fun upsertTriggerOccurrence(entity: TriggerOccurrenceEntity): Long

    @Query("DELETE FROM link_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("DELETE FROM contributions WHERE id = :id")
    suspend fun deleteContribution(id: Long)

    @Query("DELETE FROM trigger_rules WHERE id = :id")
    suspend fun deleteTriggerRule(id: Long)

    @Query("UPDATE contributions SET excluded = :excluded, updatedAtMillis = :now WHERE id = :id")
    suspend fun setContributionExcluded(id: Long, excluded: Boolean, now: Long)

    @Query("UPDATE contributions SET overrideValue = :value, updatedAtMillis = :now WHERE id = :id")
    suspend fun setContributionOverride(id: Long, value: Double?, now: Long)

    @Query("UPDATE trigger_occurrences SET dismissedAtMillis = :now WHERE id = :id")
    suspend fun dismissTriggerOccurrence(id: Long, now: Long)

    @Query("UPDATE trigger_occurrences SET deliveredAtMillis = :now WHERE id = :id AND deliveredAtMillis IS NULL")
    suspend fun markTriggerOccurrenceDelivered(id: Long, now: Long): Int
}
