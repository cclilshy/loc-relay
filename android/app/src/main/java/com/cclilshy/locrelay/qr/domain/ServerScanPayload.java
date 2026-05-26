package com.cclilshy.locrelay.qr.domain;

import com.cclilshy.locrelay.gateway.data.GatewayPrefs;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class ServerScanPayload {
    private final String server;
    private final int port;
    private final String token;

    private ServerScanPayload(String server, int port, String token) {
        this.server = server;
        this.port = port;
        this.token = token;
    }

    public static ServerScanPayload parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("empty payload");
        }
        try {
            URI uri = new URI(value.trim());
            if (!"loc-relay".equals(uri.getScheme()) || !"server".equals(uri.getHost())) {
                throw new IllegalArgumentException("unsupported payload");
            }
            Map<String, String> query = parseQuery(uri.getRawQuery());
            String server = firstNonEmpty(query.get("addr"), query.get("server"));
            String token = query.get("token");
            if (server == null || token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("missing server config");
            }
            int port = parsePort(query.get("port"));
            return new ServerScanPayload(server, port, token);
        } catch (URISyntaxException err) {
            throw new IllegalArgumentException("invalid payload", err);
        }
    }

    public String getServer() {
        return server;
    }

    public int getPort() {
        return port;
    }

    public String getToken() {
        return token;
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> values = new HashMap<>();
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return values;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                values.put(decode(keyValue[0]), decode(keyValue[1]));
            }
        }
        return values;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException err) {
            throw new IllegalArgumentException("unsupported encoding", err);
        }
    }

    private static int parsePort(String value) {
        if (value == null || value.trim().isEmpty()) {
            return GatewayPrefs.DEFAULT_FRPC_SERVER_PORT;
        }
        try {
            int port = Integer.parseInt(value);
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("invalid port");
            }
            return port;
        } catch (NumberFormatException err) {
            throw new IllegalArgumentException("invalid port", err);
        }
    }

    private static String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        if (second != null && !second.trim().isEmpty()) {
            return second;
        }
        return null;
    }
}
