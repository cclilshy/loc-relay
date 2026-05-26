package com.cclilshy.locrelay.event.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EventWebhookSubscriptionStoreTest {
    @Test
    public void togglesWebhookChannelsPerEventType() {
        String stored = "";

        stored = EventWebhookSubscriptionStore.setSelected(stored, "ops", true);
        stored = EventWebhookSubscriptionStore.setSelected(stored, "audit", true);
        stored = EventWebhookSubscriptionStore.setSelected(stored, "ops", false);

        assertFalse(EventWebhookSubscriptionStore.isSelected(stored, "ops"));
        assertTrue(EventWebhookSubscriptionStore.isSelected(stored, "audit"));
        assertEquals(1, EventWebhookSubscriptionStore.parse(stored).size());
    }
}
