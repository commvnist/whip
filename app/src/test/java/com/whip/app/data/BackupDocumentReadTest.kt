package com.whip.app.data

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDocumentReadTest {
    @Test fun preservesUtf8AtExactByteLimitAndClosesTheProviderStream() {
        val content = "{\"title\":\"Reading 📚\"}"
        val bytes = content.toByteArray(Charsets.UTF_8)
        var closed = false
        val stream = object : ByteArrayInputStream(bytes) {
            override fun close() { closed = true; super.close() }
        }
        assertEquals(content, stream.readBackupDocument(bytes.size))
        assertTrue(closed)
        assertTrue(runCatching { bytes.inputStream().readBackupDocument(bytes.size - 1) }.isFailure)
    }

    @Test fun unknownLengthProviderStopsAtFirstExcessByteAndClosesOnFailure() {
        var readCount = 0
        var closed = false
        val stream = object : InputStream() {
            override fun available() = 0
            override fun read(): Int { readCount++; return 'x'.code }
            override fun close() { closed = true }
        }
        val failure = runCatching { stream.readBackupDocument(1024) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(requireNotNull(failure).message.orEmpty().contains("current data is unchanged"))
        assertEquals(1025, readCount)
        assertTrue(closed)
    }

    @Test fun acceptsEmptyAndShortReadsWithoutTreatingProviderMetadataAsLength() {
        assertEquals("", byteArrayOf().inputStream().readBackupDocument(1))
        val bytes = "{\"encrypted\":\"é\"}".toByteArray(Charsets.UTF_8)
        val stream = object : ByteArrayInputStream(bytes) {
            override fun read(buffer: ByteArray, offset: Int, length: Int) =
                super.read(buffer, offset, minOf(1, length))
            override fun available() = Int.MAX_VALUE
        }
        assertEquals(bytes.toString(Charsets.UTF_8), stream.readBackupDocument(bytes.size))
    }
}
