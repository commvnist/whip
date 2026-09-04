package com.whip.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
            assertEquals(DataEpochState.Current(6), DataEpochGate(context, name) { false }.evaluate())
            assertEquals("current:6", base.readText())
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
            assertEquals(DataEpochState.ResetInProgress(6), DataEpochGate(context, name) { true }.evaluate())
            gate.markCurrent()
            assertEquals(DataEpochState.Current(6), DataEpochGate(context, name) { true }.evaluate())
        } finally {
            base.delete()
            File(base.path + ".bak").delete()
        }
    }

}
