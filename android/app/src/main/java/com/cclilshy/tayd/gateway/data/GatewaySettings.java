package com.cclilshy.tayd.gateway.data;

import com.cclilshy.tayd.gateway.domain.GatewayStartupPolicy;

import android.content.SharedPreferences;

public final class GatewaySettings {
    public final String bindHost;
    public final boolean httpEnabled;
    public final int httpPort;
    public final int httpRemotePort;
    public final String httpAuthUsername;
    public final String httpAuthPassword;
    public final boolean socksEnabled;
    public final int socksPort;
    public final int socksRemotePort;
    public final String socksAuthUsername;
    public final String socksAuthPassword;
    public final boolean frpcEnabled;
    public final String frpcServer;
    public final int frpcServerPort;
    public final String frpcToken;
    public final String frpcProxyMappings;
    public final boolean eventListenerEnabled;
    public final boolean persistentNotificationEnabled;

    private GatewaySettings(
            String bindHost,
            boolean httpEnabled,
            int httpPort,
            int httpRemotePort,
            String httpAuthUsername,
            String httpAuthPassword,
            boolean socksEnabled,
            int socksPort,
            int socksRemotePort,
            String socksAuthUsername,
            String socksAuthPassword,
            boolean frpcEnabled,
            String frpcServer,
            int frpcServerPort,
            String frpcToken,
            String frpcProxyMappings,
            boolean eventListenerEnabled,
            boolean persistentNotificationEnabled) {
        this.bindHost = bindHost;
        this.httpEnabled = httpEnabled;
        this.httpPort = httpPort;
        this.httpRemotePort = httpRemotePort;
        this.httpAuthUsername = httpAuthUsername;
        this.httpAuthPassword = httpAuthPassword;
        this.socksEnabled = socksEnabled;
        this.socksPort = socksPort;
        this.socksRemotePort = socksRemotePort;
        this.socksAuthUsername = socksAuthUsername;
        this.socksAuthPassword = socksAuthPassword;
        this.frpcEnabled = frpcEnabled;
        this.frpcServer = frpcServer;
        this.frpcServerPort = frpcServerPort;
        this.frpcToken = frpcToken;
        this.frpcProxyMappings = frpcProxyMappings;
        this.eventListenerEnabled = eventListenerEnabled;
        this.persistentNotificationEnabled = persistentNotificationEnabled;
    }

    public static GatewaySettings from(SharedPreferences prefs) {
        return new GatewaySettings(
                GatewayPrefs.getString(prefs, GatewayPrefs.KEY_BIND_HOST, GatewayPrefs.DEFAULT_BIND_HOST),
                prefs.getBoolean(GatewayPrefs.KEY_HTTP_ENABLED, true),
                GatewayPrefs.getInt(prefs, GatewayPrefs.KEY_HTTP_PORT, GatewayPrefs.DEFAULT_HTTP_PORT),
                GatewayPrefs.getInt(prefs, GatewayPrefs.KEY_HTTP_REMOTE_PORT, GatewayPrefs.DEFAULT_HTTP_REMOTE_PORT),
                GatewayPrefs.getString(prefs, GatewayPrefs.KEY_HTTP_AUTH_USERNAME, ""),
                GatewayPrefs.getString(prefs, GatewayPrefs.KEY_HTTP_AUTH_PASSWORD, ""),
                prefs.getBoolean(GatewayPrefs.KEY_SOCKS_ENABLED, true),
                GatewayPrefs.getInt(prefs, GatewayPrefs.KEY_SOCKS_PORT, GatewayPrefs.DEFAULT_SOCKS_PORT),
                GatewayPrefs.getInt(prefs, GatewayPrefs.KEY_SOCKS_REMOTE_PORT, GatewayPrefs.DEFAULT_SOCKS_REMOTE_PORT),
                GatewayPrefs.getString(prefs, GatewayPrefs.KEY_SOCKS_AUTH_USERNAME, ""),
                GatewayPrefs.getString(prefs, GatewayPrefs.KEY_SOCKS_AUTH_PASSWORD, ""),
                prefs.getBoolean(GatewayPrefs.KEY_FRPC_ENABLED, false),
                GatewayPrefs.getString(prefs, GatewayPrefs.KEY_FRPC_SERVER, ""),
                GatewayPrefs.getInt(prefs, GatewayPrefs.KEY_FRPC_SERVER_PORT, GatewayPrefs.DEFAULT_FRPC_SERVER_PORT),
                GatewayPrefs.getString(prefs, GatewayPrefs.KEY_FRPC_TOKEN, ""),
                GatewayPrefs.getString(prefs, GatewayPrefs.KEY_FRPC_PROXY_MAPPINGS, ""),
                prefs.getBoolean(GatewayPrefs.KEY_EVENT_LISTENER_ENABLED, false),
                prefs.getBoolean(
                        GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED,
                        GatewayStartupPolicy.isPersistentNotificationEnabledByDefault()));
    }
}
