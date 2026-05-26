package com.cclilshy.locrelay.event.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EventWebhookSubscriptionStore {
    private EventWebhookSubscriptionStore() {
    }

    public static List<String> parse(String stored) {
        List<String> channels = new ArrayList<>();
        if (stored == null || stored.trim().isEmpty()) {
            return channels;
        }
        String[] lines = stored.split("\\n", -1);
        Set<String> seen = new LinkedHashSet<>();
        for (String line : lines) {
            String value = line.trim();
            if (!value.isEmpty()) {
                seen.add(value);
            }
        }
        channels.addAll(seen);
        return channels;
    }

    public static String serialize(List<String> channels) {
        StringBuilder out = new StringBuilder();
        if (channels == null) {
            return "";
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String channel : channels) {
            String value = channel == null ? "" : channel.trim();
            if (!value.isEmpty()) {
                seen.add(value);
            }
        }
        for (String channel : seen) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(channel);
        }
        return out.toString();
    }

    public static boolean isSelected(String stored, String channel) {
        String wanted = channel == null ? "" : channel.trim();
        for (String selected : parse(stored)) {
            if (selected.equals(wanted)) {
                return true;
            }
        }
        return false;
    }

    public static String setSelected(String stored, String channel, boolean selected) {
        String value = channel == null ? "" : channel.trim();
        List<String> channels = parse(stored);
        List<String> next = new ArrayList<>();
        for (String item : channels) {
            if (!item.equals(value)) {
                next.add(item);
            }
        }
        if (selected && !value.isEmpty()) {
            next.add(value);
        }
        return serialize(next);
    }
}
