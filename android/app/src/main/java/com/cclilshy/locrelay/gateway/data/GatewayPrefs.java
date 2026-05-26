package com.cclilshy.locrelay.gateway.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class GatewayPrefs {
    public static final String NAME = "gateway";
    public static final String KEY_BIND_HOST = "bind_host";
    public static final String KEY_HTTP_ENABLED = "http_enabled";
    public static final String KEY_HTTP_PORT = "http_port";
    public static final String KEY_HTTP_REMOTE_PORT = "http_remote_port";
    public static final String KEY_HTTP_AUTH_USERNAME = "http_auth_username";
    public static final String KEY_HTTP_AUTH_PASSWORD = "http_auth_password";
    public static final String KEY_SOCKS_ENABLED = "socks_enabled";
    public static final String KEY_SOCKS_PORT = "socks_port";
    public static final String KEY_SOCKS_REMOTE_PORT = "socks_remote_port";
    public static final String KEY_SOCKS_AUTH_USERNAME = "socks_auth_username";
    public static final String KEY_SOCKS_AUTH_PASSWORD = "socks_auth_password";
    public static final String KEY_FRPC_ENABLED = "frpc_enabled";
    public static final String KEY_FRPC_SERVER = "frpc_server";
    public static final String KEY_FRPC_SERVER_PORT = "frpc_server_port";
    public static final String KEY_FRPC_TOKEN = "frpc_token";
    public static final String KEY_FRPC_PROXY_MAPPINGS = "frpc_proxy_mappings";
    public static final String KEY_WEBHOOK_ENABLED = "webhook_enabled";
    public static final String KEY_WEBHOOK_CHANNELS = "webhook_channels";
    public static final String KEY_EVENT_LISTENER_ENABLED = "event_listener_enabled";
    public static final String KEY_EVENT_CALL_ENABLED = "event_call_enabled";
    public static final String KEY_EVENT_SMS_ENABLED = "event_sms_enabled";
    public static final String KEY_EVENT_NOTIFICATION_ENABLED = "event_notification_enabled";
    public static final String KEY_EVENT_CALL_WEBHOOK_CHANNELS = "event_call_webhook_channels";
    public static final String KEY_EVENT_SMS_WEBHOOK_CHANNELS = "event_sms_webhook_channels";
    public static final String KEY_EVENT_NOTIFICATION_WEBHOOK_CHANNELS = "event_notification_webhook_channels";
    public static final String KEY_NO_WINDOW_MODE = "no_window_mode";
    public static final String KEY_AUTO_START_ENABLED = "auto_start_enabled";
    public static final String KEY_START_SERVICE_ON_APP_LAUNCH = "start_service_on_app_launch";
    public static final String KEY_PERSISTENT_NOTIFICATION_ENABLED = "persistent_notification_enabled";
    public static final String KEY_SERVICE_NOTIFICATION_PERMISSION_PROMPTED_AFTER_START = "service_notification_permission_prompted_after_start";
    public static final String KEY_RUNNING = "running";
    public static final String KEY_STATUS = "status";

    public static final String DEFAULT_BIND_HOST = "0.0.0.0";
    public static final int DEFAULT_HTTP_PORT = 8080;
    public static final int DEFAULT_HTTP_REMOTE_PORT = 18080;
    public static final int DEFAULT_SOCKS_PORT = 1080;
    public static final int DEFAULT_SOCKS_REMOTE_PORT = 11080;
    public static final int DEFAULT_FRPC_SERVER_PORT = 7000;

    private GatewayPrefs() {
    }

    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static int getInt(SharedPreferences prefs, String key, int defaultValue) {
        String value = prefs.getString(key, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public static String getString(SharedPreferences prefs, String key, String defaultValue) {
        String value = prefs.getString(key, defaultValue);
        return value == null ? defaultValue : value;
    }
}
