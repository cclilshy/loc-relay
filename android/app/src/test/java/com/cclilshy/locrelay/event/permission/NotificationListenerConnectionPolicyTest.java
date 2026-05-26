package com.cclilshy.locrelay.event.permission;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NotificationListenerConnectionPolicyTest {
    @Test
    public void requestsRebindOnlyWhenNotificationSubscriptionCanActuallyReceiveEvents() {
        assertTrue(NotificationListenerConnectionPolicy.shouldRequestRebind(true, true, true, true));

        assertFalse(NotificationListenerConnectionPolicy.shouldRequestRebind(false, true, true, true));
        assertFalse(NotificationListenerConnectionPolicy.shouldRequestRebind(true, false, true, true));
        assertFalse(NotificationListenerConnectionPolicy.shouldRequestRebind(true, true, false, true));
        assertFalse(NotificationListenerConnectionPolicy.shouldRequestRebind(true, true, true, false));
    }
}
