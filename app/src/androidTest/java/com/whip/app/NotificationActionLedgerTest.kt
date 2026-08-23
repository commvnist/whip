package com.whip.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.reminders.NotificationActionLedger
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationActionLedgerTest {
    @Test
    fun retryCanClaimTheSameNotificationActionOnlyOnce() {
        val ledger = NotificationActionLedger(ApplicationProvider.getApplicationContext())
        val action = "test-${UUID.randomUUID()}"

        assertTrue(ledger.claim(action, 1_000_000L))
        assertFalse(ledger.claim(action, 1_000_001L))
        assertTrue(ledger.claim("$action-new", 1_000_002L))
    }

    @Test
    fun failedMutationCanReleaseReceiptAndRetry() {
        val ledger = NotificationActionLedger(ApplicationProvider.getApplicationContext())
        val action = "test-${UUID.randomUUID()}"

        assertTrue(ledger.begin(action, 2_000_000L))
        assertFalse(ledger.begin(action, 2_000_001L))
        assertTrue(ledger.release(action))
        assertTrue(ledger.begin(action, 2_000_002L))
        assertTrue(ledger.complete(action, 2_000_003L))
        assertFalse(ledger.begin(action, 2_000_004L))
    }

    @Test
    fun abandonedInFlightReceiptBecomesRetryableAfterTimeout() {
        val ledger = NotificationActionLedger(ApplicationProvider.getApplicationContext())
        val action = "test-${UUID.randomUUID()}"

        assertTrue(ledger.begin(action, 3_000_000L))
        assertFalse(ledger.begin(action, 3_299_999L))
        assertTrue(ledger.begin(action, 3_300_001L))
    }
}
