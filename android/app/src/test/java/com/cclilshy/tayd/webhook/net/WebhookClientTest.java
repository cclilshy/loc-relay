package com.cclilshy.tayd.webhook.net;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WebhookClientTest {
    @Test
    public void alwaysUsesJsonPostContentType() {
        assertEquals("application/json; charset=utf-8", WebhookClient.jsonContentType());
    }

    @Test
    public void createsBasicProxyAuthorizationHeaderWhenProxyUrlHasCredentials() throws Exception {
        assertEquals(
                "Basic YWxpY2U6c2VjcmV0",
                WebhookClient.proxyAuthorizationHeader("http://alice:secret@127.0.0.1:8080"));
    }

    @Test
    public void omitsProxyAuthorizationHeaderWhenProxyUrlHasNoCredentials() throws Exception {
        assertEquals("", WebhookClient.proxyAuthorizationHeader("http://127.0.0.1:8080"));
        assertEquals("", WebhookClient.proxyAuthorizationHeader(""));
    }
}
