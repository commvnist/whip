package com.whip.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetSnapshotCacheTest {
    @Test
    fun directionalPaddingMirrorsStartAndEndInRtl() {
        assertEquals(28 to 8, resolveWidgetHorizontalPadding(28, 8, isRtl = false))
        assertEquals(8 to 28, resolveWidgetHorizontalPadding(28, 8, isRtl = true))
    }
    @Test
    fun codecRoundTripsUnicodeAndSeparators() {
        val snapshot = CachedWidgetSnapshot(
            rows = listOf(
                CachedWidgetRow("💊 Medication | morning", "Today\nMain", isChild = false, completed = false),
                CachedWidgetRow("Lexapro", "Medication", isChild = true, completed = true),
            ),
            savedAtMillis = 42L,
            dataGeneration = 9L,
        )

        assertEquals(snapshot, WidgetSnapshotCodec.decode(WidgetSnapshotCodec.encode(snapshot)))
    }

    @Test
    fun codecRejectsUnknownOrCorruptSnapshots() {
        assertNull(WidgetSnapshotCodec.decode("3|42|9"))
        assertNull(WidgetSnapshotCodec.decode("2|42"))
        assertNull(WidgetSnapshotCodec.decode("1|not-a-time\n0|0|bad|bad"))
        assertNull(WidgetSnapshotCodec.decode("1|42\n0|0|not base64|still bad"))
    }

    @Test
    fun legacySnapshotBelongsOnlyToPreRestoreGenerationZero() {
        val legacy = WidgetSnapshotCodec.decode("1|42\n0|0|VGFzaw|VG9kYXk")

        assertEquals(0L, legacy?.dataGeneration)
        assertEquals("Task", legacy?.rows?.single()?.title)
    }
}
