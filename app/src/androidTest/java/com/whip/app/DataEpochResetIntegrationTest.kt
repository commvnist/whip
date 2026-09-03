package com.whip.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.whip.app.core.AppSettings
import com.whip.app.core.SharedPreferencesSettingsRepository
import com.whip.app.core.normalized
import com.whip.app.data.PortableBackupWorker
import com.whip.app.data.WhipDatabase
import com.whip.app.startup.LocalDataResetter
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataEpochResetIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun resetIsRepeatableCanonicalAndLeavesNonWhipPreferencesAlone() {
        val unrelatedPreferences = context.getSharedPreferences("third_party_test_state", Context.MODE_PRIVATE)
        assertTrue(unrelatedPreferences.edit().putString("owner", "platform").commit())

        seedWhipArtifacts("first")
        val firstWorkId = enqueueStaleWork()
        val firstGeneration = LocalDataResetter(context).resetAndVerify()
        assertCanonicalReset(firstGeneration, firstWorkId)

        seedWhipArtifacts("second")
        val secondWorkId = enqueueStaleWork()
        val secondGeneration = LocalDataResetter(context).resetAndVerify()
        assertCanonicalReset(secondGeneration, secondWorkId)

        assertNotEquals(firstGeneration, secondGeneration)
        assertEquals("platform", unrelatedPreferences.getString("owner", null))
        unrelatedPreferences.edit().clear().commit()
    }

    private fun seedWhipArtifacts(value: String) {
        assertTrue(
            context.getSharedPreferences("whip-settings", Context.MODE_PRIVATE)
                .edit().putBoolean("setupCompleted", true).commit(),
        )
        assertTrue(
            context.getSharedPreferences("whip_widget_areas", Context.MODE_PRIVATE)
                .edit().putString("area", value).commit(),
        )
        assertTrue(
            context.getSharedPreferences("whip_widget_snapshots", Context.MODE_PRIVATE)
                .edit().putString("snapshot", value).commit(),
        )
        WhipDatabase.get(context).openHelper.writableDatabase.apply {
            execSQL("CREATE TABLE IF NOT EXISTS epoch_reset_sentinel (value TEXT NOT NULL)")
            execSQL("DELETE FROM epoch_reset_sentinel")
            execSQL("INSERT INTO epoch_reset_sentinel(value) VALUES (?)", arrayOf(value))
        }
    }

    private fun enqueueStaleWork() = OneTimeWorkRequestBuilder<PortableBackupWorker>()
        .setInitialDelay(1, TimeUnit.DAYS)
        .build()
        .also { request -> WorkManager.getInstance(context).enqueue(request).result.get() }
        .id

    private fun assertCanonicalReset(generation: Long, staleWorkId: java.util.UUID) {
        assertNotEquals(0L, generation)
        assertEquals(AppSettings().normalized(), SharedPreferencesSettingsRepository(context).current())
        assertFalse(context.getSharedPreferences("whip_widget_areas", Context.MODE_PRIVATE).contains("area"))
        assertFalse(context.getSharedPreferences("whip_widget_snapshots", Context.MODE_PRIVATE).contains("snapshot"))

        val database = WhipDatabase.get(context)
        database.openHelper.writableDatabase.query("PRAGMA user_version").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(43, cursor.getInt(0))
        }
        database.openHelper.writableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'epoch_reset_sentinel'",
        ).use { cursor -> assertEquals(0, cursor.count) }

        val staleInfo = WorkManager.getInstance(context).getWorkInfoById(staleWorkId).get()
        assertTrue(
            staleInfo == null || staleInfo.state == WorkInfo.State.CANCELLED,
        )
        assertNotNull(context.getSharedPreferences("whip_recovery_runtime", Context.MODE_PRIVATE))
    }
}
