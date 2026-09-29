package com.whip.app.data

import java.io.FilterInputStream
import java.io.InputStream

/** Includes the encrypted envelope; bound provider streams before retaining or parsing JSON. */
internal const val MAX_BACKUP_IMPORT_BYTES = 32 * 1024 * 1024

internal fun InputStream.readBackupDocument(maxBytes: Int = MAX_BACKUP_IMPORT_BYTES): String {
    require(maxBytes > 0)
    val limitLabel = if (maxBytes >= 1024 * 1024) "${maxBytes / 1024 / 1024} MiB" else "$maxBytes bytes"
    val bounded = object : FilterInputStream(this) {
        private var remaining = maxBytes.toLong()

        private fun consumed(count: Int) {
            require(count <= remaining) {
                "This backup is too large to open (limit $limitLabel). " +
                    "Choose another backup. Your current data is unchanged."
            }
            remaining -= count
        }

        override fun read(): Int = `in`.read().also { if (it >= 0) consumed(1) }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            `in`.read(buffer, offset, minOf(length.toLong(), remaining + 1).toInt())
                .also { if (it > 0) consumed(it) }
    }
    return bounded.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
