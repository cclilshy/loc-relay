package com.cclilshy.tayd.webhook.data;

import com.cclilshy.tayd.webhook.domain.WebhookChannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.junit.Test;

public class WebhookChannelStoreTest {
    @Test
    public void serializesWebhookChannelsWithTargetAndOptionalProxy() {
        String stored = WebhookChannelStore.serialize(WebhookChannelStore.upsert(
                "",
                new WebhookChannel("ops", "https://hooks.example.com/events", "http://127.0.0.1:8080")));

        List<WebhookChannel> channels = WebhookChannelStore.parse(stored);

        assertEquals(1, channels.size());
        assertEquals("ops", channels.get(0).getName());
        assertEquals("https://hooks.example.com/events", channels.get(0).getTargetUrl());
        assertEquals("http://127.0.0.1:8080", channels.get(0).getProxyUrl());
    }

    @Test
    public void replacesAndRemovesByName() {
        String stored = WebhookChannelStore.serialize(WebhookChannelStore.upsert(
                "",
                new WebhookChannel("ops", "https://old.example.com", "")));

        stored = WebhookChannelStore.serialize(WebhookChannelStore.upsert(
                stored,
                new WebhookChannel("ops", "https://new.example.com", "")));
        stored = WebhookChannelStore.remove(stored, "ops");

        assertNull(WebhookChannelStore.find(stored, "ops"));
        assertEquals(0, WebhookChannelStore.parse(stored).size());
    }
}
