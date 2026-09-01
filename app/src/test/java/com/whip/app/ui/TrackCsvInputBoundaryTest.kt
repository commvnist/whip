package com.whip.app.ui

import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackCsvInputBoundaryTest {
    @Test
    fun strictUtf8DecoderAcceptsBomButRejectsMalformedBytesAndNul() {
        val bomCsv = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            "Title\nValid\n".toByteArray(StandardCharsets.UTF_8)
        assertEquals("Title\nValid\n", decodeTrackCsvUtf8(bomCsv))

        val malformed = runCatching {
            decodeTrackCsvUtf8(byteArrayOf(0xC3.toByte(), 0x28))
        }.exceptionOrNull()
        assertTrue("Malformed UTF-8 must fail closed, but was $malformed", malformed is IllegalArgumentException)
        assertTrue(requireNotNull(malformed?.message).contains("UTF-8"))

        val nul = runCatching {
            decodeTrackCsvUtf8("Title\nA\u0000B\n".toByteArray(StandardCharsets.UTF_8))
        }.exceptionOrNull()
        assertTrue("NUL-containing CSV must fail closed, but was $nul", nul is IllegalArgumentException)
        assertTrue(requireNotNull(nul?.message).contains("NUL"))
    }

    @Test
    fun exactMaximumDataRowsMayHaveOneTrailingBlankPhysicalRecord() {
        val exactMaximum = exactMaximumCsv()

        assertEquals(5_000, validateTrackCsvEnvelope(exactMaximum + "\n").dataRows)
        assertEquals(5_000, validateTrackCsvEnvelope(exactMaximum + " \t\r\n").dataRows)
    }

    @Test
    fun quotedEmptyInteriorBlankAndMultipleTrailingBlankRecordsStillCountTowardTheLimit() {
        val exactMaximum = exactMaximumCsv()
        val quotedEmpty = runCatching { validateTrackCsvEnvelope(exactMaximum + "\"\"\n") }.exceptionOrNull()
        val interiorBlank = runCatching {
            validateTrackCsvEnvelope("Title\n\n" + List(5_000) { "Row $it" }.joinToString("\n", postfix = "\n"))
        }.exceptionOrNull()
        val twoTrailingBlankRecords = runCatching {
            validateTrackCsvEnvelope(exactMaximum + "\n\n")
        }.exceptionOrNull()

        listOf(
            quotedEmpty to "more than 5,000",
            interiorBlank to "more than 5,000",
            twoTrailingBlankRecords to "physical rows",
        ).forEach { (failure, expectedMessage) ->
            assertTrue("Boundary record must not be silently discarded: $failure", failure is IllegalArgumentException)
            assertTrue(requireNotNull(failure?.message).contains(expectedMessage))
        }
    }

    private fun exactMaximumCsv(): String = buildString {
        appendLine("Title")
        repeat(5_000) { ordinal -> appendLine("Row $ordinal") }
    }
}
