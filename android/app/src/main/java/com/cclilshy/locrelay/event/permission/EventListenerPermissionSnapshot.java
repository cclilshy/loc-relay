package com.cclilshy.locrelay.event.permission;

public final class EventListenerPermissionSnapshot {
    private final boolean callGranted;
    private final boolean smsGranted;
    private final boolean notificationGranted;

    public EventListenerPermissionSnapshot(boolean callGranted, boolean smsGranted, boolean notificationGranted) {
        this.callGranted = callGranted;
        this.smsGranted = smsGranted;
        this.notificationGranted = notificationGranted;
    }

    public boolean isCallGranted() {
        return callGranted;
    }

    public boolean isSmsGranted() {
        return smsGranted;
    }

    public boolean isNotificationGranted() {
        return notificationGranted;
    }

    public int grantedCount() {
        int count = 0;
        if (callGranted) {
            count++;
        }
        if (smsGranted) {
            count++;
        }
        if (notificationGranted) {
            count++;
        }
        return count;
    }
}
