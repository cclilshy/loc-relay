package com.cclilshy.tayd.gateway.domain;

import com.cclilshy.tayd.gateway.data.GatewayPrefs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class GatewayExtensionCatalog {
    private static final List<GatewayExtension> EXTENSIONS = Collections.unmodifiableList(Arrays.asList(
            new GatewayExtension(
                    "frpc",
                    "FRPC",
                    "Run the bundled FRPC client with explicit custom proxy mappings.",
                    GatewayPrefs.KEY_FRPC_ENABLED,
                    false,
                    "Built-in",
                    Arrays.asList(
                            textField("Server", GatewayPrefs.KEY_FRPC_SERVER, ""),
                            numberField("Server port", GatewayPrefs.KEY_FRPC_SERVER_PORT,
                                    GatewayPrefs.DEFAULT_FRPC_SERVER_PORT),
                            secretField("Token", GatewayPrefs.KEY_FRPC_TOKEN, ""))),
            new GatewayExtension(
                    "webhook",
                    "WebHook",
                    "POST Event Listener payloads as JSON to configured HTTP endpoints.",
                    GatewayPrefs.KEY_WEBHOOK_ENABLED,
                    false,
                    "Built-in",
                    Collections.emptyList()),
            new GatewayExtension(
                    "http",
                    "HTTP PROXY",
                    "HTTP proxy endpoint for browsers, package managers, and command line clients.",
                    GatewayPrefs.KEY_HTTP_ENABLED,
                    true,
                    "",
                    Arrays.asList(
                            numberField("Local port", GatewayPrefs.KEY_HTTP_PORT, GatewayPrefs.DEFAULT_HTTP_PORT),
                            textField("Bind host", GatewayPrefs.KEY_BIND_HOST, GatewayPrefs.DEFAULT_BIND_HOST),
                            textField("Auth username", GatewayPrefs.KEY_HTTP_AUTH_USERNAME, ""),
                            secretField("Auth password", GatewayPrefs.KEY_HTTP_AUTH_PASSWORD, ""))),
            new GatewayExtension(
                    "socks5",
                    "SOCKS5",
                    "SOCKS5 proxy endpoint for applications that need generic TCP egress.",
                    GatewayPrefs.KEY_SOCKS_ENABLED,
                    true,
                    "",
                    Arrays.asList(
                            numberField("Local port", GatewayPrefs.KEY_SOCKS_PORT, GatewayPrefs.DEFAULT_SOCKS_PORT),
                            textField("Bind host", GatewayPrefs.KEY_BIND_HOST, GatewayPrefs.DEFAULT_BIND_HOST),
                            textField("Auth username", GatewayPrefs.KEY_SOCKS_AUTH_USERNAME, ""),
                            secretField("Auth password", GatewayPrefs.KEY_SOCKS_AUTH_PASSWORD, ""))),
            new GatewayExtension(
                    "event_listener",
                    "Event Listener",
                    "Log call, SMS, and app notification events and forward them to WebHook channels.",
                    GatewayPrefs.KEY_EVENT_LISTENER_ENABLED,
                    false,
                    "",
                    Collections.emptyList())));

    private GatewayExtensionCatalog() {
    }

    public static List<GatewayExtension> all() {
        return EXTENSIONS;
    }

    public static GatewayExtension findById(String id) {
        for (GatewayExtension extension : EXTENSIONS) {
            if (extension.getId().equals(id)) {
                return extension;
            }
        }
        throw new IllegalArgumentException("Unknown extension: " + id);
    }

    private static GatewayExtension.Field textField(String label, String key, String defaultValue) {
        return new GatewayExtension.Field(label, key, defaultValue, GatewayExtension.FieldType.TEXT);
    }

    private static GatewayExtension.Field numberField(String label, String key, int defaultValue) {
        return new GatewayExtension.Field(
                label,
                key,
                Integer.toString(defaultValue),
                GatewayExtension.FieldType.NUMBER);
    }

    private static GatewayExtension.Field secretField(String label, String key, String defaultValue) {
        return new GatewayExtension.Field(label, key, defaultValue, GatewayExtension.FieldType.SECRET);
    }

}
