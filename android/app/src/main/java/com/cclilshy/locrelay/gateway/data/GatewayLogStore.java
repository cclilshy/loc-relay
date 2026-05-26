package com.cclilshy.locrelay.gateway.data;

import com.cclilshy.locrelay.common.log.BoundedLogBuffer;

import android.content.SharedPreferences;

public final class GatewayLogStore {
    public static final String KEY_TOTAL_LOG = "log_total";
    public static final String KEY_HTTP_LOG = "log_http";
    public static final String KEY_SOCKS_LOG = "log_socks5";
    public static final String KEY_FRPC_LOG = "log_frpc";
    public static final String KEY_WEBHOOK_LOG = "log_webhook";
    public static final String KEY_EVENT_LISTENER_LOG = "log_event_listener";
    private static final int MAX_LINES = 120;

    private GatewayLogStore() {
    }

    public static void append(SharedPreferences prefs, String key, String line) {
        append(prefs, key, line, key);
    }

    public static void append(SharedPreferences prefs, String key, String line, String totalPrefix) {
        String current = prefs.getString(key, "");
        String next = BoundedLogBuffer.append(current, line, MAX_LINES);
        SharedPreferences.Editor editor = prefs.edit().putString(key, next);
        if (!KEY_TOTAL_LOG.equals(key)) {
            String totalCurrent = prefs.getString(KEY_TOTAL_LOG, "");
            String totalLine = totalPrefix == null || totalPrefix.trim().isEmpty()
                    ? line
                    : totalPrefix + ": " + line;
            editor.putString(KEY_TOTAL_LOG, BoundedLogBuffer.append(totalCurrent, totalLine, MAX_LINES));
        }
        editor.apply();
    }

    public static String get(SharedPreferences prefs, String key) {
        return prefs.getString(key, "");
    }

    public static boolean isLogKey(String key) {
        return KEY_TOTAL_LOG.equals(key)
                || KEY_HTTP_LOG.equals(key)
                || KEY_SOCKS_LOG.equals(key)
                || KEY_FRPC_LOG.equals(key)
                || KEY_WEBHOOK_LOG.equals(key)
                || KEY_EVENT_LISTENER_LOG.equals(key);
    }

    public static String keyForExtension(String extensionId) {
        if ("http".equals(extensionId)) {
            return KEY_HTTP_LOG;
        }
        if ("socks5".equals(extensionId)) {
            return KEY_SOCKS_LOG;
        }
        if ("frpc".equals(extensionId)) {
            return KEY_FRPC_LOG;
        }
        if ("webhook".equals(extensionId)) {
            return KEY_WEBHOOK_LOG;
        }
        if ("event_listener".equals(extensionId)) {
            return KEY_EVENT_LISTENER_LOG;
        }
        return KEY_TOTAL_LOG;
    }
}
