package com.cclilshy.locrelay.frpc.data;

import com.cclilshy.locrelay.proxy.domain.ProxyMapping;
import com.cclilshy.locrelay.gateway.data.GatewayPrefs;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public final class FrpcProxyStore {
    private FrpcProxyStore() {
    }

    public static List<ProxyMapping> parse(String value) {
        List<ProxyMapping> mappings = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return mappings;
        }
        String[] lines = value.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            if (parts.length != 5) {
                throw new IllegalArgumentException("frpc proxy line " + (i + 1)
                        + " must be name|type|localIP|localPort|remotePort");
            }
            mappings.add(new ProxyMapping(
                    parts[0].trim(),
                    parts[1].trim(),
                    parts[2].trim(),
                    parsePort(parts[3], i + 1, "localPort"),
                    parsePort(parts[4], i + 1, "remotePort")));
        }
        return mappings;
    }

    public static String format(List<ProxyMapping> mappings) {
        List<String> lines = new ArrayList<>();
        for (ProxyMapping mapping : mappings) {
            lines.add(mapping.getName()
                    + "|" + mapping.getType()
                    + "|" + mapping.getLocalIp()
                    + "|" + mapping.getLocalPort()
                    + "|" + mapping.getRemotePort());
        }
        return String.join("\n", lines);
    }

    public static boolean hasMapping(SharedPreferences prefs, String name) {
        return hasMapping(GatewayPrefs.getString(prefs, GatewayPrefs.KEY_FRPC_PROXY_MAPPINGS, ""), name);
    }

    public static boolean hasMapping(String value, String name) {
        return find(value, name) != null;
    }

    public static ProxyMapping find(SharedPreferences prefs, String name) {
        return find(GatewayPrefs.getString(prefs, GatewayPrefs.KEY_FRPC_PROXY_MAPPINGS, ""), name);
    }

    public static ProxyMapping find(String value, String name) {
        List<ProxyMapping> mappings;
        try {
            mappings = parse(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        for (ProxyMapping mapping : mappings) {
            if (mapping.getName().equals(name)) {
                return mapping;
            }
        }
        return null;
    }

    public static void upsert(SharedPreferences prefs, ProxyMapping mapping) {
        String next = upsert(GatewayPrefs.getString(prefs, GatewayPrefs.KEY_FRPC_PROXY_MAPPINGS, ""), mapping);
        prefs.edit().putString(GatewayPrefs.KEY_FRPC_PROXY_MAPPINGS, next).apply();
    }

    public static String upsert(String value, ProxyMapping mapping) {
        List<ProxyMapping> mappings = parse(value);
        List<ProxyMapping> next = new ArrayList<>();
        boolean replaced = false;
        for (ProxyMapping existing : mappings) {
            if (existing.getName().equals(mapping.getName())) {
                next.add(mapping);
                replaced = true;
            } else {
                next.add(existing);
            }
        }
        if (!replaced) {
            next.add(mapping);
        }
        return format(next);
    }

    public static void remove(SharedPreferences prefs, String name) {
        String next = remove(GatewayPrefs.getString(prefs, GatewayPrefs.KEY_FRPC_PROXY_MAPPINGS, ""), name);
        prefs.edit().putString(GatewayPrefs.KEY_FRPC_PROXY_MAPPINGS, next).apply();
    }

    public static String remove(String value, String name) {
        List<ProxyMapping> mappings = parse(value);
        List<ProxyMapping> next = new ArrayList<>();
        for (ProxyMapping mapping : mappings) {
            if (!mapping.getName().equals(name)) {
                next.add(mapping);
            }
        }
        return format(next);
    }

    private static int parsePort(String value, int lineNumber, String field) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException err) {
            throw new IllegalArgumentException("frpc proxy line " + lineNumber + " has invalid " + field, err);
        }
    }
}
