package com.cclilshy.tayd.gateway.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GatewayLogStoreTest {
    @Test
    public void mapsWebhookExtensionToDedicatedDeliveryLog() {
        assertEquals("log_webhook", GatewayLogStore.keyForExtension("webhook"));
        assertTrue(GatewayLogStore.isLogKey("log_webhook"));
    }
}
