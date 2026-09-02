package com.whip.app.health

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectPaginationTest {
    @Test
    fun everyProviderPageIsCollectedExactlyOnceInOrder() = runBlocking {
        val requestedTokens = mutableListOf<String?>()
        val records = collectHealthRecordPages { token ->
            requestedTokens += token
            when (token) {
                null -> HealthRecordPage(listOf("a", "b"), "page-2")
                "page-2" -> HealthRecordPage(listOf("c"), "page-3")
                "page-3" -> HealthRecordPage(listOf("d", "e"), null)
                else -> error("Unexpected token $token")
            }
        }

        assertEquals(listOf(null, "page-2", "page-3"), requestedTokens)
        assertEquals(listOf("a", "b", "c", "d", "e"), records)
    }
}
