package com.whip.app.domain

import java.time.Instant
import java.time.LocalDate

enum class LinkKind { Contribution, Context }

enum class LinkSourceType { Habit, Task, Subtask, Workout, Exercise, Metric, Track }

enum class LinkSourceMetric {
    NumericValue,
    Success,
    Completion,
    Count,
    Duration,
    Volume,
    EstimatedOneRepMax,
    MaxWeight,
    Distance,
    Repetitions,
    EntryCount,
    FieldValue,
}

enum class LinkValueMode { SourceValue, FixedValue }

data class LinkRuleDraft(
    val name: String,
    val kind: LinkKind = LinkKind.Contribution,
    val sourceType: LinkSourceType,
    val sourceEntityId: Long? = null,
    val sourceMetricId: String? = null,
    val sourceItemId: Long? = null,
    val sourceMetric: LinkSourceMetric,
    val targetGoalId: Long,
    val targetMilestoneId: Long? = null,
    val valueMode: LinkValueMode = LinkValueMode.SourceValue,
    val fixedValue: Double? = null,
    val multiplier: Double = 1.0,
    val offset: Double = 0.0,
    val retroactiveFrom: LocalDate? = null,
    val enabled: Boolean = true,
    val trackAggregation: TrackAggregation? = null,
    val sourceFieldId: Long? = null,
    val conditionMode: TrackConditionMode = TrackConditionMode.MatchAll,
    val conditions: List<TrackCondition> = emptyList(),
)

data class LinkRule(
    val id: Long,
    val uuid: String,
    val name: String,
    val kind: LinkKind,
    val sourceType: LinkSourceType,
    val sourceEntityId: Long?,
    val sourceMetricId: String?,
    val sourceItemId: Long?,
    val sourceMetric: LinkSourceMetric,
    val targetGoalId: Long,
    val targetMilestoneId: Long?,
    val valueMode: LinkValueMode,
    val fixedValue: Double?,
    val multiplier: Double,
    val offset: Double,
    val retroactiveFrom: LocalDate?,
    val enabled: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val trackAggregation: TrackAggregation? = null,
    val sourceFieldId: Long? = null,
    val conditionMode: TrackConditionMode = TrackConditionMode.MatchAll,
    val conditions: List<TrackCondition> = emptyList(),
)

data class Contribution(
    val id: Long,
    val uuid: String,
    val linkRuleId: Long,
    val sourceEventId: String,
    val sourceType: LinkSourceType,
    val sourceEntityId: Long?,
    val targetGoalId: Long,
    val metricEntryId: String?,
    val canonicalValue: Double?,
    val localDate: LocalDate,
    val timestamp: Instant,
    val excluded: Boolean,
    val overrideValue: Double?,
    val explanation: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

enum class TriggerOutcome { Completed, Recorded, Failed, Skipped }
enum class TriggerTargetType { Habit, Task, Track }
enum class TriggerAction { PromptTask, PromptHabit, CheckOffHabit, PromptTrackEntry }
enum class TriggerSourceProperty { Title, Notes, Name, NumericValue, Unit, Outcome, EventDate, Constant }

data class TriggerFieldMapping(
    val targetFieldId: Long,
    val sourceProperty: TriggerSourceProperty,
    val constantValue: TrackValueDraft? = null,
)

data class TriggerRuleDraft(
    val name: String,
    val sourceType: LinkSourceType,
    val sourceEntityId: Long,
    val sourceItemId: Long? = null,
    val outcome: TriggerOutcome = TriggerOutcome.Completed,
    val targetType: TriggerTargetType,
    val targetEntityId: Long,
    val delayMinutes: Int = 0,
    val quietStartMinutes: Int? = null,
    val quietEndMinutes: Int? = null,
    val action: TriggerAction = if (targetType == TriggerTargetType.Habit) TriggerAction.PromptHabit else TriggerAction.PromptTask,
    val notificationEnabled: Boolean = false,
    val enabled: Boolean = true,
    val conditionMode: TrackConditionMode = TrackConditionMode.MatchAll,
    val conditions: List<TrackCondition> = emptyList(),
    val mappings: List<TriggerFieldMapping> = emptyList(),
)

data class TriggerRule(
    val id: Long,
    val uuid: String,
    val name: String,
    val sourceType: LinkSourceType,
    val sourceEntityId: Long,
    val sourceItemId: Long?,
    val outcome: TriggerOutcome,
    val targetType: TriggerTargetType,
    val targetEntityId: Long,
    val delayMinutes: Int,
    val quietStartMinutes: Int?,
    val quietEndMinutes: Int?,
    val action: TriggerAction,
    val notificationEnabled: Boolean,
    val enabled: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val conditionMode: TrackConditionMode = TrackConditionMode.MatchAll,
    val conditions: List<TrackCondition> = emptyList(),
    val mappings: List<TriggerFieldMapping> = emptyList(),
)

data class TriggerOccurrence(
    val id: Long,
    val triggerRuleId: Long,
    val sourceEventId: String,
    val availableAt: Instant,
    val deliveredAt: Instant?,
    val dismissedAt: Instant?,
    val remindAt: Instant? = null,
    val fulfilledEntryId: Long? = null,
    val sourceSnapshot: String = "",
)

data class LinkBackfillPreview(
    val scannedEventCount: Int,
    val eligibleEventCount: Int,
    val skippedEventCount: Int,
    val skippedReasons: Map<String, Int> = emptyMap(),
    val contributionCount: Int,
    val totalCanonicalValue: Double,
    val firstDate: LocalDate?,
    val lastDate: LocalDate?,
    val unitExplanation: String = "",
    val targetImpact: String = "",
)
