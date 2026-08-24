package com.whip.app.ui

import com.whip.app.reminders.RestTimerNotifications
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeliveryTest {
    @Test
    fun permissionAlwaysTakesPrecedenceOverImplementationDetails() {
        listOf(false, true).forEach { appEnabled ->
            listOf(false, true).forEach { configured ->
                listOf(false, true).forEach { channelEnabled ->
                    assertEquals(
                        NotificationDeliveryState.Blocked,
                        notificationDeliveryState(false, appEnabled, configured, channelEnabled),
                    )
                }
            }
        }
    }

    @Test
    fun outcomeStatesDistinguishWhipConfigurationFromAndroidBlocking() {
        assertEquals(NotificationDeliveryState.OffInWhip, notificationDeliveryState(true, true, false, false))
        assertEquals(NotificationDeliveryState.OffInAndroid, notificationDeliveryState(true, false, true, true))
        assertEquals(NotificationDeliveryState.OffInAndroid, notificationDeliveryState(true, true, true, false))
        assertEquals(NotificationDeliveryState.Deliverable, notificationDeliveryState(true, true, true, true))
    }

    @Test
    fun overallStateNeverClaimsDeliverableWhenAnyRequiredLayerBlocksDelivery() {
        assertEquals(NotificationDeliveryState.Blocked, overallNotificationDeliveryState(false, true, listOf(NotificationDeliveryState.Deliverable)))
        assertEquals(NotificationDeliveryState.OffInAndroid, overallNotificationDeliveryState(true, false, listOf(NotificationDeliveryState.Deliverable)))
        assertEquals(NotificationDeliveryState.OffInAndroid, overallNotificationDeliveryState(true, true, listOf(NotificationDeliveryState.Deliverable, NotificationDeliveryState.OffInAndroid)))
        assertEquals(NotificationDeliveryState.OffInWhip, overallNotificationDeliveryState(true, true, listOf(NotificationDeliveryState.OffInWhip)))
        assertEquals(NotificationDeliveryState.Deliverable, overallNotificationDeliveryState(true, true, listOf(NotificationDeliveryState.Deliverable)))
    }

    @Test
    fun notificationTestCanCreateAMissingChannelButCannotBypassAndroidBlocking() {
        assertTrue(canSendNotificationTest(true, true, taskChannelBlocked = false))
        assertFalse(canSendNotificationTest(false, true, taskChannelBlocked = false))
        assertFalse(canSendNotificationTest(true, false, taskChannelBlocked = false))
        assertFalse(canSendNotificationTest(true, true, taskChannelBlocked = true))
    }

    @Test
    fun restTimerSoundAndVibrationSettingsSelectFourDistinctChannels() {
        val channels = buildSet {
            listOf(false, true).forEach { sound ->
                listOf(false, true).forEach { vibration ->
                    add(RestTimerNotifications.channelId(sound, vibration))
                }
            }
        }
        assertEquals(4, channels.size)
        assertTrue(RestTimerNotifications.channelId(false, false).contains("silent-still"))
        assertTrue(RestTimerNotifications.channelId(true, true).contains("sound-vibrate"))
    }
}
