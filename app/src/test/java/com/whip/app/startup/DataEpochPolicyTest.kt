package com.whip.app.startup

import org.junit.Assert.assertEquals
import org.junit.Test

class DataEpochPolicyTest {
    @Test fun freshInstallEstablishesCurrentEpoch() {
        assertEquals(DataEpochState.Current(3), DataEpochPolicy.resolve(null, hasAppOwnedState = false))
    }

    @Test fun markerlessInstallWithAnyOwnedStateRequiresReset() {
        assertEquals(DataEpochState.ResetRequired, DataEpochPolicy.resolve(null, hasAppOwnedState = true))
    }

    @Test fun onlyExactCurrentMarkerOpensAndOldOrMalformedMarkersFailClosed() {
        assertEquals(DataEpochState.Current(3), DataEpochPolicy.resolve("current:3", true))
        assertEquals(DataEpochState.ResetRequired, DataEpochPolicy.resolve("current:1", false))
        assertEquals(DataEpochState.ResetRequired, DataEpochPolicy.resolve("invalid", false))
    }

    @Test fun interruptedCurrentResetResumesButForeignResetMarkerRequiresConfirmation() {
        assertEquals(DataEpochState.ResetInProgress(3), DataEpochPolicy.resolve("reset-in-progress:3", true))
        assertEquals(DataEpochState.ResetRequired, DataEpochPolicy.resolve("reset-in-progress:1", true))
    }

    @Test fun everyDurableStateHasStableEncoding() {
        assertEquals("current:3", DataEpochPolicy.encode(DataEpochState.Current(3)))
        assertEquals("reset-required", DataEpochPolicy.encode(DataEpochState.ResetRequired))
        assertEquals("reset-in-progress:3", DataEpochPolicy.encode(DataEpochState.ResetInProgress(3)))
    }

    @Test fun unrelatedPlatformArtifactsDoNotMakeFreshInstallLookOld() {
        assertEquals(
            false,
            DataEpochPolicy.hasMeaningfulWhipState(
                databaseFileNames = setOf("androidx.work.workdb", "androidx.work.workdb-wal"),
                sharedPreferenceNames = setOf("androidx.work.util.preferences", "profileinstaller_profileWrittenFor_lastUpdateTime.dat"),
                noBackupFileNames = setOf("androidx.work.workdb", "com.google.android.datatransport.events"),
            ),
        )
    }

    @Test fun eachCanonicalWhipArtifactMakesMarkerlessInstallRequireReset() {
        assertEquals(
            true,
            DataEpochPolicy.hasMeaningfulWhipState(setOf("whip.db-wal"), emptySet(), emptySet()),
        )
        assertEquals(
            true,
            DataEpochPolicy.hasMeaningfulWhipState(emptySet(), setOf("whip-settings"), emptySet()),
        )
        assertEquals(
            true,
            DataEpochPolicy.hasMeaningfulWhipState(emptySet(), emptySet(), setOf("restore-recovery.whip.json.bak")),
        )
    }

    @Test fun resetSelectsOnlyWhipOwnedPreferenceStores() {
        assertEquals(
            setOf("whip-settings", "portable_backups"),
            DataEpochPolicy.selectWhipPreferenceNames(
                setOf("whip-settings", "portable_backups", "androidx.work.util.preferences", "platform-state"),
            ),
        )
    }
}
