package com.cclilshy.locrelay.event.runtime;

import com.cclilshy.locrelay.event.domain.EventListenerLogFormatter;
import com.cclilshy.locrelay.event.domain.AndroidEventInfo;
import com.cclilshy.locrelay.webhook.runtime.WebhookDispatcher;
import com.cclilshy.locrelay.gateway.data.GatewayLogStore;
import com.cclilshy.locrelay.gateway.data.GatewayPrefs;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.content.SharedPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EventNotificationListenerService extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        if (!isActive()) {
            return;
        }
        StatusBarNotification[] activeNotifications = getActiveNotifications();
        if (activeNotifications == null) {
            return;
        }
        for (StatusBarNotification notification : activeNotifications) {
            onNotificationPosted(notification);
        }
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        if (isActive() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(new ComponentName(this, EventNotificationListenerService.class));
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !isActive()) {
            return;
        }
        String payload = EventListenerLogFormatter.notificationPosted(toNotificationInfo(sbn));
        GatewayLogStore.append(
                GatewayPrefs.get(this),
                GatewayLogStore.KEY_EVENT_LISTENER_LOG,
                payload,
                "events");
        WebhookDispatcher.dispatch(this, "notification", payload);
    }

    private EventListenerLogFormatter.NotificationInfo toNotificationInfo(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Bundle extras = notification == null ? null : notification.extras;
        return new EventListenerLogFormatter.NotificationInfo(
                sbn.getPackageName(),
                sbn.getId(),
                sbn.getTag(),
                sbn.getKey(),
                sbn.getPostTime(),
                getExtraText(extras, Notification.EXTRA_TITLE),
                getExtraText(extras, Notification.EXTRA_TEXT),
                notificationExtras(sbn, notification, extras));
    }

    private Map<String, Object> notificationExtras(
            StatusBarNotification sbn,
            Notification notification,
            Bundle extras) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("clearable", sbn.isClearable());
        info.put("ongoing", sbn.isOngoing());
        info.put("groupKey", sbn.getGroupKey());
        info.put("overrideGroupKey", sbn.getOverrideGroupKey());
        if (notification != null) {
            info.put("category", notification.category);
            info.put("channelId", notification.getChannelId());
            info.put("shortcutId", notification.getShortcutId());
            info.put("sortKey", notification.getSortKey());
            info.put("group", notification.getGroup());
            info.put("when", notification.when);
            info.put("flags", notification.flags);
            info.put("defaults", notification.defaults);
            info.put("priority", notification.priority);
            info.put("visibility", notification.visibility);
            info.put("color", notification.color);
            info.put("number", notification.number);
            info.put("timeoutAfter", notification.getTimeoutAfter());
            info.put("tickerText", AndroidEventInfo.normalize(notification.tickerText));
            info.put("extras", AndroidEventInfo.bundleToMap(extras));
        }
        return info;
    }

    private String getExtraText(Bundle extras, String key) {
        if (extras == null) {
            return null;
        }
        CharSequence value = extras.getCharSequence(key);
        return value == null ? null : value.toString();
    }

    private boolean isActive() {
        SharedPreferences prefs = GatewayPrefs.get(this);
        return prefs.getBoolean(GatewayPrefs.KEY_RUNNING, false)
                && prefs.getBoolean(GatewayPrefs.KEY_EVENT_LISTENER_ENABLED, false)
                && prefs.getBoolean(GatewayPrefs.KEY_EVENT_NOTIFICATION_ENABLED, false);
    }
}
