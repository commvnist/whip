package com.whip.app.data

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupContractTest {
    @Test fun exactCurrentContractIsAccepted() {
        validateBackupContract(ENVELOPE_VERSION, CURRENT_DATA_MODEL_EPOCH, BACKUP_DATABASE_VERSION)
    }

    @Test fun oldEpochIsRejectedClearly() {
        val message = rejected { validateBackupContract(ENVELOPE_VERSION, 1, BACKUP_DATABASE_VERSION) }
        assertTrue(message.contains("older Whip data epoch"))
    }

    @Test fun futureEpochIsRejectedClearly() {
        val message = rejected { validateBackupContract(ENVELOPE_VERSION, 3, BACKUP_DATABASE_VERSION) }
        assertTrue(message.contains("newer Whip data epoch"))
    }

    @Test fun oldAndFutureDataVersionsAreRejected() {
        assertTrue(rejected { validateBackupContract(ENVELOPE_VERSION, 2, 18) }.contains("old data version"))
        assertTrue(rejected { validateBackupContract(ENVELOPE_VERSION, 2, 20) }.contains("future data version"))
    }

    @Test fun envelopeMustAlsoMatchExactly() {
        assertTrue(rejected { validateBackupContract(2, 2, 19) }.contains("unsupported envelope version"))
    }

    private fun rejected(block: () -> Unit): String = try {
        block()
        fail("Expected backup contract rejection")
        error("unreachable")
    } catch (error: IllegalArgumentException) {
        error.message.orEmpty()
    }
}
