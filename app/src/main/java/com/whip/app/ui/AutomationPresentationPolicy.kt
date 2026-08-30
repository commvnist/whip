package com.whip.app.ui

import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.TriggerAction
import com.whip.app.domain.TriggerOccurrence
import com.whip.app.domain.TriggerRule
import com.whip.app.domain.TriggerRuleDraft
import com.whip.app.domain.TriggerTargetType
import java.time.Instant

/**
 * The Habits workspace owns the compact Task/Habit next-action editor. Track
 * capture and Track-source rules keep their richer editor in Tracks until the
 * two editors share one complete endpoint model.
 */
internal fun TriggerRule.isSupportedByHabitAutomationWorkspace(): Boolean =
    sourceType in setOf(LinkSourceType.Habit, LinkSourceType.Task, LinkSourceType.Workout) &&
        sourceItemId == null &&
        targetType in setOf(TriggerTargetType.Habit, TriggerTargetType.Task) &&
        conditions.isEmpty() &&
        mappings.isEmpty() &&
        when (action) {
            TriggerAction.PromptHabit, TriggerAction.CheckOffHabit -> targetType == TriggerTargetType.Habit
            TriggerAction.PromptTask -> targetType == TriggerTargetType.Task
            TriggerAction.PromptTrackEntry -> false
        }

/** One source of truth for the in-app Ready queue and its delayed/reminded state. */
internal fun TriggerOccurrence.isReadyForHabitAutomation(
    rule: TriggerRule?,
    now: Instant,
): Boolean {
    if (rule == null || !rule.isSupportedByHabitAutomationWorkspace()) return false
    if (!rule.enabled || rule.action == TriggerAction.CheckOffHabit) return false
    if (dismissedAt != null || fulfilledEntryId != null) return false
    return !(remindAt ?: availableAt).isAfter(now)
}

/** Direct completion is reserved for local target modes with an input-free quick action. */
internal fun TriggerRule.supportsReadyDoNow(
    targetMode: HabitTrackingMode?,
    targetSourceMetricId: String?,
): Boolean =
    targetType == TriggerTargetType.Habit &&
        action == TriggerAction.PromptHabit &&
        targetSourceMetricId == null &&
        targetMode in setOf(
            HabitTrackingMode.CheckOff,
            HabitTrackingMode.Count,
            HabitTrackingMode.Decimal,
        )

/** Preserve every rule detail when a surface changes only the enabled state. */
internal fun TriggerRule.toAutomationDraft(enabled: Boolean = this.enabled) = TriggerRuleDraft(
    name = name,
    sourceType = sourceType,
    sourceEntityId = sourceEntityId,
    sourceItemId = sourceItemId,
    outcome = outcome,
    targetType = targetType,
    targetEntityId = targetEntityId,
    delayMinutes = delayMinutes,
    quietStartMinutes = quietStartMinutes,
    quietEndMinutes = quietEndMinutes,
    action = action,
    notificationEnabled = notificationEnabled,
    enabled = enabled,
    conditionMode = conditionMode,
    conditions = conditions,
    mappings = mappings,
)
