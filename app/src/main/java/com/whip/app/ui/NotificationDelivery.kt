package com.whip.app.ui

internal enum class NotificationDeliveryState(val label: String) {
    Deliverable("Deliverable"),
    Blocked("Blocked"),
    OffInWhip("Off in Whip"),
    OffInAndroid("Off in Android"),
}

internal fun notificationDeliveryState(
    permissionGranted: Boolean,
    appNotificationsEnabled: Boolean,
    configuredInWhip: Boolean,
    androidChannelEnabled: Boolean,
): NotificationDeliveryState = when {
    !permissionGranted -> NotificationDeliveryState.Blocked
    !configuredInWhip -> NotificationDeliveryState.OffInWhip
    !appNotificationsEnabled || !androidChannelEnabled -> NotificationDeliveryState.OffInAndroid
    else -> NotificationDeliveryState.Deliverable
}

internal fun overallNotificationDeliveryState(
    permissionGranted: Boolean,
    appNotificationsEnabled: Boolean,
    channels: Collection<NotificationDeliveryState>,
): NotificationDeliveryState = when {
    !permissionGranted -> NotificationDeliveryState.Blocked
    !appNotificationsEnabled -> NotificationDeliveryState.OffInAndroid
    channels.any { it == NotificationDeliveryState.OffInAndroid || it == NotificationDeliveryState.Blocked } ->
        NotificationDeliveryState.OffInAndroid
    channels.none { it == NotificationDeliveryState.Deliverable } -> NotificationDeliveryState.OffInWhip
    else -> NotificationDeliveryState.Deliverable
}

/** A missing channel is repairable by the test action itself; a blocked channel is not. */
internal fun canSendNotificationTest(
    permissionGranted: Boolean,
    appNotificationsEnabled: Boolean,
    taskChannelBlocked: Boolean,
): Boolean = permissionGranted && appNotificationsEnabled && !taskChannelBlocked
