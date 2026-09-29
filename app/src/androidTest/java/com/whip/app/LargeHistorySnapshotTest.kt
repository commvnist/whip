package com.whip.app

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.data.MeasurementEntryEntity
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.core.SystemWhipClock
import com.whip.app.core.UuidWhipIdGenerator
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LargeHistorySnapshotTest {
    @Test
    fun replacingHistoryWhileReadingMultipleCursorWindowsKeepsSnapshotsCoherent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "history-snapshot-${UUID.randomUUID()}.db"
        val database = Room.databaseBuilder(context, WhipDatabase::class.java, name)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
        try {
            withTimeout(25_000) {
                val sql = database.openHelper.writableDatabase
                sql.execSQL("INSERT INTO measurement_definitions VALUES ('history','History','Decimal','Unitless','unitless',1,1,0,1,1)")
                suspend fun replace(generation: Int) = database.withTransaction {
                    sql.execSQL("DELETE FROM measurement_entries")
                    val count = if (generation % 2 == 0) 2_500 else 500
                    // Over 5 MB of notes forces Android to refill its CursorWindow.
                    sql.execSQL(
                        "WITH RECURSIVE seq(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM seq WHERE x < $count) " +
                            "INSERT INTO measurement_entries " +
                            "SELECT 'entry-'||x,'history',$generation,$generation,'unitless','Recorded',x,1,'UTC',0,'Manual',NULL,printf('%02048d',x),1,1 FROM seq",
                    )
                }
                fun verify(entries: List<MeasurementEntryEntity>) {
                    assertTrue("Only a complete replacement may be observed", entries.size in setOf(500, 2_500))
                    assertEquals("Cursor windows must come from one generation", 1, entries.map { it.canonicalValue }.distinct().size)
                    assertEquals(entries.size, entries.map { it.id }.distinct().size)
                }
                replace(0)
                val repository = RoomMeasurementRepository(database, SystemWhipClock, UuidWhipIdGenerator)
                coroutineScope {
                    val observer = launch(Dispatchers.IO) {
                        repository.entries.collect { entries ->
                            assertTrue("All pages must belong to a complete replacement", entries.size in setOf(500, 2_500))
                            assertEquals(1, entries.map { it.canonicalValue }.distinct().size)
                            assertEquals(entries.size, entries.map { it.id }.distinct().size)
                            assertEquals(entries.sortedByDescending { it.timestamp }, entries)
                        }
                    }
                    val reader = async(Dispatchers.IO) {
                        repeat(16) {
                            verify(database.measurementDao().getAllEntries())
                            verify(database.measurementDao().getEntriesForMeasurement("history"))
                        }
                    }
                    val writer = launch(Dispatchers.IO) {
                        repeat(16) { generation -> replace(generation + 1); delay(1) }
                    }
                    reader.await()
                    writer.join()
                    observer.cancel()
                }
                verify(database.measurementDao().getAllEntries())
            }
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
