package com.cclilshy.locrelay.event.domain;

import android.os.Bundle;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class AndroidEventInfo {
    private static final int MAX_DEPTH = 4;

    private AndroidEventInfo() {
    }

    public static Map<String, Object> bundleToMap(Bundle bundle, String... skippedKeys) {
        return bundleToMap(bundle, 0, skippedKeys);
    }

    private static Map<String, Object> bundleToMap(Bundle bundle, int depth, String... skippedKeys) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (bundle == null) {
            return result;
        }
        Set<String> skipped = new HashSet<>();
        if (skippedKeys != null) {
            for (String key : skippedKeys) {
                skipped.add(key);
            }
        }
        for (String key : new TreeSet<>(bundle.keySet())) {
            if (skipped.contains(key)) {
                continue;
            }
            try {
                result.put(key, normalize(bundle.get(key), depth + 1));
            } catch (RuntimeException error) {
                result.put(key, "unavailable:" + error.getClass().getSimpleName());
            }
        }
        return result;
    }

    public static Object normalize(Object value) {
        return normalize(value, 0);
    }

    private static Object normalize(Object value, int depth) {
        if (value == null) {
            return null;
        }
        if (depth > MAX_DEPTH) {
            return String.valueOf(value);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof CharSequence || value instanceof Character || value instanceof Enum) {
            return String.valueOf(value);
        }
        if (value instanceof Bundle) {
            return bundleToMap((Bundle) value, depth + 1);
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            if (type.getComponentType() == Byte.TYPE) {
                byte[] bytes = new byte[length];
                for (int i = 0; i < length; i++) {
                    bytes[i] = Array.getByte(value, i);
                }
                return bytesToHex(bytes);
            }
            ArrayList<Object> items = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                items.add(normalize(Array.get(value, i), depth + 1));
            }
            return items;
        }
        if (value instanceof Iterable) {
            ArrayList<Object> items = new ArrayList<>();
            for (Object item : (Iterable<?>) value) {
                items.add(normalize(item, depth + 1));
            }
            return items;
        }
        return String.valueOf(value);
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            hex[i * 2] = digits[value >>> 4];
            hex[i * 2 + 1] = digits[value & 0x0f];
        }
        return new String(hex);
    }
}
