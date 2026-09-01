package com.whip.app.startup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDataGenerationTest {
    @Test
    fun legacyPayloadsAreAcceptedOnlyBeforeTheFirstReplaceRestore() {
        assertTrue(generationMatches(current = 0L, presented = MISSING_USER_DATA_GENERATION))
        assertFalse(generationMatches(current = 1L, presented = MISSING_USER_DATA_GENERATION))
    }

    @Test
    fun onlyTheCurrentGenerationCanMutateRestoredData() {
        assertTrue(generationMatches(current = 7L, presented = 7L))
        assertFalse(generationMatches(current = 7L, presented = 6L))
        assertFalse(generationMatches(current = 7L, presented = 8L))
    }
}
