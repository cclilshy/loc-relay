package com.cclilshy.tayd.webhook.data;

import com.cclilshy.tayd.webhook.domain.WebhookChannel;

import java.util.ArrayList;
import java.util.List;

public final class WebhookChannelStore {
    private WebhookChannelStore() {
    }

    public static List<WebhookChannel> parse(String stored) {
        List<WebhookChannel> channels = new ArrayList<>();
        if (stored == null || stored.trim().isEmpty()) {
            return channels;
        }
        String[] lines = stored.split("\\n", -1);
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> fields = splitEscaped(line);
            if (fields.size() < 2) {
                continue;
            }
            try {
                channels.add(new WebhookChannel(
                        fields.get(0),
                        fields.get(1),
                        fields.size() > 2 ? fields.get(2) : ""));
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupt persisted rows so one bad channel does not hide the whole list.
            }
        }
        return channels;
    }

    public static String serialize(List<WebhookChannel> channels) {
        StringBuilder out = new StringBuilder();
        if (channels == null) {
            return "";
        }
        for (WebhookChannel channel : channels) {
            if (channel == null) {
                continue;
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(escape(channel.getName()))
                    .append('\t')
                    .append(escape(channel.getTargetUrl()))
                    .append('\t')
                    .append(escape(channel.getProxyUrl()));
        }
        return out.toString();
    }

    public static List<WebhookChannel> upsert(String stored, WebhookChannel channel) {
        List<WebhookChannel> channels = parse(stored);
        boolean replaced = false;
        for (int i = 0; i < channels.size(); i++) {
            if (channels.get(i).getName().equals(channel.getName())) {
                channels.set(i, channel);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            channels.add(channel);
        }
        return channels;
    }

    public static String remove(String stored, String name) {
        List<WebhookChannel> channels = parse(stored);
        List<WebhookChannel> next = new ArrayList<>();
        for (WebhookChannel channel : channels) {
            if (!channel.getName().equals(name)) {
                next.add(channel);
            }
        }
        return serialize(next);
    }

    public static WebhookChannel find(String stored, String name) {
        for (WebhookChannel channel : parse(stored)) {
            if (channel.getName().equals(name)) {
                return channel;
            }
        }
        return null;
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder();
        String safe = value == null ? "" : value;
        for (int i = 0; i < safe.length(); i++) {
            char ch = safe.charAt(i);
            if (ch == '\\') {
                out.append("\\\\");
            } else if (ch == '\t') {
                out.append("\\t");
            } else if (ch == '\n') {
                out.append("\\n");
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static List<String> splitEscaped(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (escaped) {
                if (ch == 't') {
                    current.append('\t');
                } else if (ch == 'n') {
                    current.append('\n');
                } else {
                    current.append(ch);
                }
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '\t') {
                fields.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        fields.add(current.toString());
        return fields;
    }
}
