package com.whip.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchQueueOverflowStateTest {
    @Test
    fun acknowledgementBindsToDeliveryAndObservedRejectedShareCount() {
        val state = LaunchQueueOverflowState()

        assertFalse(state.admit(deliveryId = 42L, rejectedShareCount = 1))
        state.acknowledge()
        assertTrue(state.admit(deliveryId = 42L, rejectedShareCount = 1))

        assertFalse(state.admit(deliveryId = 42L, rejectedShareCount = 2))
        assertTrue(state.dialogVisible)
        state.acknowledge()
        assertTrue(state.admit(deliveryId = 42L, rejectedShareCount = 2))
    }
}
