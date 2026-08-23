package com.whip.app.data

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class PortableBackupPolicyTest {
    @Test
    fun fileNameUsesSelectedZoneAndContainsNoProviderUnsafeCharacters() {
        val name = portableBackupFileName(
            Instant.parse("2026-08-18T23:01:02Z"),
            ZoneId.of("America/Toronto"),
        )

        assertEquals("whip-2026-08-18-190102.whip.json", name)
    }

    @Test
    fun retentionDeletesOnlyOldWhipBackups() {
        val files = listOf(
            file("whip-2026-08-15-120000.whip.json", 1),
            file("whip-2026-08-16-120000.whip.json", 2),
            file("whip-2026-08-17-120000.whip.json", 3),
            file("whip-tasks-2026-08-01.csv", 0),
            file("family-photo.jpg", 0),
        )

        assertEquals(
            listOf("whip-2026-08-15-120000.whip.json"),
            portableBackupItemsToPrune(files, retentionCount = 2, Item::name, Item::modified).map(Item::name),
        )
    }

    @Test
    fun retentionIsClampedToAtLeastOneVerifiedBackup() {
        val files = listOf(file("whip-old.whip.json", 1), file("whip-new.whip.json", 2))

        assertEquals(
            listOf("whip-old.whip.json"),
            portableBackupItemsToPrune(files, retentionCount = 0, Item::name, Item::modified).map(Item::name),
        )
    }

    @Test
    fun newlyWrittenBackupIsProtectedWhenProviderHasNoModifiedTimestamp() {
        val newestWrite = file("whip-new-write.whip.json", 0)
        val files = listOf(
            file("whip-old.whip.json", 100),
            file("whip-older.whip.json", 50),
            newestWrite,
        )

        assertEquals(
            listOf("whip-old.whip.json", "whip-older.whip.json"),
            portableBackupItemsToPrune(
                files,
                retentionCount = 1,
                displayName = Item::name,
                lastModifiedMillis = Item::modified,
                protected = { it === newestWrite },
            ).map(Item::name),
        )
    }

    private fun file(name: String, modified: Long) = Item(name, modified)
    private data class Item(val name: String, val modified: Long)
}
