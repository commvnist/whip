package com.whip.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaRepositoryRulesTest {
    @Test
    fun namesAreWhitespaceNormalizedAndLimitedToFortyCharacters() {
        assertEquals("Personal projects", normalizeAreaName("  Personal   projects  "))
        assertEquals("work", areaNameKey(" Work "))
        assertTrue(runCatching { normalizeAreaName("x".repeat(41)) }.exceptionOrNull()?.message?.contains("40") == true)
        assertTrue(runCatching { normalizeAreaName("   ") }.isFailure)
    }
}
