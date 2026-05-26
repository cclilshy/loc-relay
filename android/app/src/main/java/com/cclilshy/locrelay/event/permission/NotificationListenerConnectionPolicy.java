package com.cclilshy.locrelay.event.permission;

public final class NotificationListenerConnectionPolicy {
    private NotificationListenerConnectionPolicy() {
    }

    public static boolean shouldRequestRebind(
            boolean notificationAccessGranted,
            boolean notificationEventsEnabled,
            boolean gatewayRunning,
            boolean listenerEnabled) {
        return notificationAccessGranted
                && notificationEventsEnabled
                && gatewayRunning
                && listenerEnabled;
    }
}
