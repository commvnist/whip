package com.whip.app.ui

import com.whip.app.domain.AreaScope
import com.whip.app.domain.Area
import com.whip.app.domain.Contribution
import com.whip.app.domain.LinkKind
import com.whip.app.domain.LinkRule
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.LinkValueMode
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AreaScopeFilterRelationshipsTest {
    @Test
    fun invalidArchivedAndZeroAreaScopesRecoverToAll() {
        val work = Area("work", "Work", null, 0, false, 1, 1)
        val archived = work.copy(archived = true)
        assertEquals(AreaScope.One("work"), AreaScope.One("work").validFor(listOf(work)))
        assertEquals(AreaScope.All, AreaScope.One("missing").validFor(listOf(work)))
        assertEquals(AreaScope.All, AreaScope.One("work").validFor(listOf(archived)))
        assertEquals(AreaScope.All, AreaScope.Unassigned.validFor(emptyList()))
    }

    @Test
    fun goalScopeKeepsCrossAreaRelationshipContext() {
        val rule = LinkRule(
            id = 1, uuid = "rule", name = "Health habit to Work goal", kind = LinkKind.Contribution,
            sourceType = LinkSourceType.Habit, sourceEntityId = 7, sourceMetricId = null, sourceItemId = null,
            sourceMetric = LinkSourceMetric.Success, targetGoalId = 99, targetMilestoneId = null,
            valueMode = LinkValueMode.FixedValue, fixedValue = 1.0, multiplier = 1.0, offset = 0.0,
            retroactiveFrom = null, enabled = true, createdAtMillis = 1, updatedAtMillis = 1,
        )
        val contribution = Contribution(
            id = 2, uuid = "contribution", linkRuleId = rule.id, sourceEventId = "event",
            sourceType = LinkSourceType.Habit, sourceEntityId = 7, targetGoalId = 99, metricEntryId = null,
            canonicalValue = 1.0, localDate = LocalDate.of(2026, 8, 20), timestamp = Instant.EPOCH,
            excluded = false, overrideValue = null, explanation = "Completed", createdAtMillis = 1, updatedAtMillis = 1,
        )

        val scoped = GoalUiState(linkRules = listOf(rule), contributions = listOf(contribution))
            .forArea(AreaScope.One("health"))

        assertEquals(listOf(rule), scoped.linkRules)
        assertEquals(listOf(contribution), scoped.contributions)
    }
}
