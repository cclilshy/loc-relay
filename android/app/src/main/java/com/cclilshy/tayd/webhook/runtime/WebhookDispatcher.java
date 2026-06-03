package com.cclilshy.tayd.webhook.runtime;

import com.cclilshy.tayd.event.data.EventWebhookSubscriptionStore;
import com.cclilshy.tayd.webhook.net.WebhookClient;
import com.cclilshy.tayd.webhook.data.WebhookChannelStore;
import com.cclilshy.tayd.webhook.domain.WebhookChannel;
import com.cclilshy.tayd.gateway.data.GatewayLogStore;
import com.cclilshy.tayd.gateway.data.GatewayPrefs;
import com.cclilshy.tayd.R;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WebhookDispatcher {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private WebhookDispatcher() {
    }

    public static void dispatch(Context context, String eventType, String json) {
        SharedPreferences prefs = GatewayPrefs.get(context);
        if (!prefs.getBoolean(GatewayPrefs.KEY_WEBHOOK_ENABLED, false)) {
            return;
        }
        String selected = GatewayPrefs.getString(prefs, subscriptionKey(eventType), "");
        List<String> channelNames = EventWebhookSubscriptionStore.parse(selected);
        if (channelNames.isEmpty()) {
            return;
        }
        String storedChannels = GatewayPrefs.getString(prefs, GatewayPrefs.KEY_WEBHOOK_CHANNELS, "");
        for (String channelName : channelNames) {
            WebhookChannel channel = WebhookChannelStore.find(storedChannels, channelName);
            if (channel != null) {
                EXECUTOR.execute(() -> post(context, channel, json));
            }
        }
    }

    public static String subscriptionKey(String eventType) {
        if ("call".equals(eventType)) {
            return GatewayPrefs.KEY_EVENT_CALL_WEBHOOK_CHANNELS;
        }
        if ("sms".equals(eventType)) {
            return GatewayPrefs.KEY_EVENT_SMS_WEBHOOK_CHANNELS;
        }
        return GatewayPrefs.KEY_EVENT_NOTIFICATION_WEBHOOK_CHANNELS;
    }

    private static void post(Context context, WebhookChannel channel, String json) {
        try {
            int code = WebhookClient.postJson(channel, json);
            if (code < 200 || code >= 300) {
                GatewayLogStore.append(
                        GatewayPrefs.get(context),
                        GatewayLogStore.KEY_WEBHOOK_LOG,
                        "WebHook " + channel.getName() + " failed: http " + code,
                        "WebHook");
            }
        } catch (Exception err) {
            GatewayLogStore.append(
                    GatewayPrefs.get(context),
                    GatewayLogStore.KEY_WEBHOOK_LOG,
                    "WebHook " + channel.getName() + " failed: " + err.getMessage(),
                    "WebHook");
        }
    }
}
