package com.cclilshy.locrelay.qr.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ServerScanPayloadTest {
    @Test
    public void parsesLocRelayServerQrPayload() {
        ServerScanPayload payload = ServerScanPayload.parse(
                "loc-relay://server?addr=frp.example.com&port=7000&token=test-token");

        assertEquals("frp.example.com", payload.getServer());
        assertEquals(7000, payload.getPort());
        assertEquals("test-token", payload.getToken());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonLocRelayPayloads() {
        ServerScanPayload.parse("https://example.com");
    }
}
