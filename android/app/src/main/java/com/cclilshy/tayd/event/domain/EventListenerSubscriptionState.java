package com.cclilshy.tayd.event.domain;

public final class EventListenerSubscriptionState {
    private final boolean callEnabled;
    private final boolean smsEnabled;
    private final boolean notificationEnabled;

    public EventListenerSubscriptionState(boolean callEnabled, boolean smsEnabled, boolean notificationEnabled) {
        this.callEnabled = callEnabled;
        this.smsEnabled = smsEnabled;
        this.notificationEnabled = notificationEnabled;
    }

    public boolean isCallEnabled() {
        return callEnabled;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }
}
