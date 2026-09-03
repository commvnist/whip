package com.whip.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.data.WhipDatabase
import com.whip.app.startup.DataEpochGate
import com.whip.app.startup.DataEpochState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataEpochBoundaryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun trulyFreshInstallEstablishesCurrentMarkerWithoutReset() {
        val name = "test-fresh-data-epoch-${System.nanoTime()}"
        val base = File(context.noBackupFilesDir, name)
        try {
            assertEquals(DataEpochState.Current(3), DataEpochGate(context, name) { false }.evaluate())
            assertEquals("current:3", base.readText())
        } finally {
            base.delete()
            File(base.path + ".bak").delete()
        }
    }

    @Test fun markerStatePersistsAndInterruptedResetResumes() {
        val name = "test-data-epoch-${System.nanoTime()}"
        val base = File(context.noBackupFilesDir, name)
        val gate = DataEpochGate(context, name) { true }
        try {
            assertEquals(DataEpochState.ResetRequired, gate.evaluate())
            gate.markResetInProgress()
            assertEquals(DataEpochState.ResetInProgress(3), DataEpochGate(context, name) { true }.evaluate())
            gate.markCurrent()
            assertEquals(DataEpochState.Current(3), DataEpochGate(context, name) { true }.evaluate())
        } finally {
            base.delete()
            File(base.path + ".bak").delete()
        }
    }

    @Test fun schemaFortyTwoCannotBeOpenedDirectlyAsCanonicalSchemaFortyThree() {
        val name = "schema-42-direct-open-${System.nanoTime()}"
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { legacy ->
            legacy.execSQL("CREATE TABLE pre_epoch_marker (id INTEGER PRIMARY KEY NOT NULL)")
            legacy.execSQL("PRAGMA user_version = 42")
        }
        val database = Room.databaseBuilder(context, WhipDatabase::class.java, name).build()
        try {
            val failure = runCatching { database.openHelper.writableDatabase }.exceptionOrNull()
            assertTrue("Room must reject schema 42 without a migration", failure != null)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
