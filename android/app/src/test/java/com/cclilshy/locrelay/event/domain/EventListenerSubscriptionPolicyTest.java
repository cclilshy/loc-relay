package com.cclilshy.locrelay.event.domain;

import com.cclilshy.locrelay.event.permission.EventListenerPermissionSnapshot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EventListenerSubscriptionPolicyTest {
    @Test
    public void syncDisablesOnlyEnabledEventTypesWhosePermissionIsMissing() {
        EventListenerPermissionSnapshot snapshot = new EventListenerPermissionSnapshot(false, true, false);

        EventListenerSubscriptionState state = EventListenerSubscriptionPolicy.sync(
                true,
                true,
                true,
                snapshot);

        assertFalse(state.isCallEnabled());
        assertTrue(state.isSmsEnabled());
        assertFalse(state.isNotificationEnabled());
    }

    @Test
    public void offEventTypesStayOffWithoutRequiringPermission() {
        EventListenerPermissionSnapshot snapshot = new EventListenerPermissionSnapshot(false, false, false);

        EventListenerSubscriptionState state = EventListenerSubscriptionPolicy.sync(
                false,
                false,
                false,
                snapshot);

        assertFalse(state.isCallEnabled());
        assertFalse(state.isSmsEnabled());
        assertFalse(state.isNotificationEnabled());
    }
}
