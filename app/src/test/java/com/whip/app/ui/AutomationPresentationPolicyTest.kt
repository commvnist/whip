package com.whip.app.ui

import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.TriggerAction
import com.whip.app.domain.TriggerOccurrence
import com.whip.app.domain.TriggerOutcome
import com.whip.app.domain.TriggerRule
import com.whip.app.domain.TriggerTargetType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationPresentationPolicyTest {
    private val now = Instant.parse("2026-08-29T20:00:00Z")

    @Test
    fun habitWorkspaceIncludesOnlyRulesItsEditorCanRoundTrip() {
        assertTrue(rule().isSupportedByHabitAutomationWorkspace())
        assertTrue(
            rule(
                sourceType = LinkSourceType.Task,
                targetType = TriggerTargetType.Task,
                action = TriggerAction.PromptTask,
            ).isSupportedByHabitAutomationWorkspace(),
        )
        assertTrue(rule(sourceType = LinkSourceType.Workout).isSupportedByHabitAutomationWorkspace())
        assertFalse(rule(sourceType = LinkSourceType.Subtask, sourceItemId = 8).isSupportedByHabitAutomationWorkspace())
        assertFalse(rule(sourceType = LinkSourceType.Track).isSupportedByHabitAutomationWorkspace())
        assertFalse(
            rule(
                targetType = TriggerTargetType.Track,
                action = TriggerAction.PromptTrackEntry,
            ).isSupportedByHabitAutomationWorkspace(),
        )
    }

    @Test
    fun readyRequiresSupportedEnabledManualUnresolvedAndEffectiveDueTime() {
        val manual = rule()
        assertTrue(occurrence(availableAt = now.minusSeconds(1)).isReadyForHabitAutomation(manual, now))
        assertFalse(occurrence(availableAt = now.plusSeconds(1)).isReadyForHabitAutomation(manual, now))
        assertFalse(
            occurrence(availableAt = now.minusSeconds(60), remindAt = now.plusSeconds(60))
                .isReadyForHabitAutomation(manual, now),
        )
        assertFalse(occurrence(dismissedAt = now).isReadyForHabitAutomation(manual, now))
        assertFalse(occurrence(fulfilledEntryId = 4).isReadyForHabitAutomation(manual, now))
        assertFalse(occurrence().isReadyForHabitAutomation(rule(enabled = false), now))
        assertFalse(occurrence().isReadyForHabitAutomation(rule(action = TriggerAction.CheckOffHabit), now))
        assertFalse(
            occurrence().isReadyForHabitAutomation(
                rule(sourceType = LinkSourceType.Track),
                now,
            ),
        )
    }

    @Test
    fun directDoNowAllowsOnlyInputFreeLocallyWritableHabitTargets() {
        val promptHabit = rule()
        assertTrue(promptHabit.supportsReadyDoNow(HabitTrackingMode.CheckOff, null))
        assertTrue(promptHabit.supportsReadyDoNow(HabitTrackingMode.Count, null))
        assertTrue(promptHabit.supportsReadyDoNow(HabitTrackingMode.Decimal, null))
        assertFalse(promptHabit.supportsReadyDoNow(HabitTrackingMode.Duration, null))
        assertFalse(promptHabit.supportsReadyDoNow(HabitTrackingMode.Checklist, null))
        assertFalse(promptHabit.supportsReadyDoNow(HabitTrackingMode.Rating, null))
        assertFalse(promptHabit.supportsReadyDoNow(HabitTrackingMode.LogOnly, null))
        assertFalse(promptHabit.supportsReadyDoNow(HabitTrackingMode.Count, "health.steps"))
        assertFalse(promptHabit.supportsReadyDoNow(null, null))
        assertFalse(
            rule(targetType = TriggerTargetType.Task, action = TriggerAction.PromptTask)
                .supportsReadyDoNow(HabitTrackingMode.CheckOff, null),
        )
        assertFalse(
            rule(action = TriggerAction.CheckOffHabit)
                .supportsReadyDoNow(HabitTrackingMode.CheckOff, null),
        )
    }

    @Test
    fun enabledToggleDraftPreservesSubtaskIdentity() {
        val rule = rule(sourceType = LinkSourceType.Subtask, sourceItemId = 81)

        val paused = rule.toAutomationDraft(enabled = false)

        assertEquals(81L, paused.sourceItemId)
        assertFalse(paused.enabled)
        assertEquals(rule.sourceEntityId, paused.sourceEntityId)
        assertEquals(rule.targetEntityId, paused.targetEntityId)
    }

    private fun rule(
        sourceType: LinkSourceType = LinkSourceType.Habit,
        targetType: TriggerTargetType = TriggerTargetType.Habit,
        action: TriggerAction = TriggerAction.PromptHabit,
        enabled: Boolean = true,
        sourceItemId: Long? = null,
    ) = TriggerRule(
        id = 1,
        uuid = "rule-1",
        name = "Continue",
        sourceType = sourceType,
        sourceEntityId = 1,
        sourceItemId = sourceItemId,
        outcome = TriggerOutcome.Completed,
        targetType = targetType,
        targetEntityId = 2,
        delayMinutes = 0,
        quietStartMinutes = null,
        quietEndMinutes = null,
        action = action,
        notificationEnabled = false,
        enabled = enabled,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun occurrence(
        availableAt: Instant = now.minusSeconds(1),
        remindAt: Instant? = null,
        dismissedAt: Instant? = null,
        fulfilledEntryId: Long? = null,
    ) = TriggerOccurrence(
        id = 1,
        triggerRuleId = 1,
        sourceEventId = "event-1",
        availableAt = availableAt,
        deliveredAt = null,
        dismissedAt = dismissedAt,
        remindAt = remindAt,
        fulfilledEntryId = fulfilledEntryId,
    )
}
