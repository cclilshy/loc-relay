package com.cclilshy.locrelay.event.domain;

import com.cclilshy.locrelay.event.permission.EventListenerPermissionSnapshot;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class EventListenerLogFormatter {
    private EventListenerLogFormatter() {
    }

    public static final class SmsInfo {
        private final String sender;
        private final long timestampMillis;
        private final String body;
        private final Map<String, Object> metadata;

        public SmsInfo(String sender, long timestampMillis, String body) {
            this(sender, timestampMillis, body, Collections.<String, Object>emptyMap());
        }

        public SmsInfo(String sender, long timestampMillis, String body, Map<String, Object> metadata) {
            this.sender = sender;
            this.timestampMillis = timestampMillis;
            this.body = body;
            this.metadata = metadata == null ? Collections.<String, Object>emptyMap() : metadata;
        }
    }

    public static final class NotificationInfo {
        private final String packageName;
        private final Integer id;
        private final String tag;
        private final String key;
        private final Long postTimeMillis;
        private final String title;
        private final String text;
        private final Map<String, Object> extras;

        public NotificationInfo(
                String packageName,
                Integer id,
                String tag,
                String key,
                Long postTimeMillis,
                String title,
                String text) {
            this(packageName, id, tag, key, postTimeMillis, title, text, Collections.<String, Object>emptyMap());
        }

        public NotificationInfo(
                String packageName,
                Integer id,
                String tag,
                String key,
                Long postTimeMillis,
                String title,
                String text,
                Map<String, Object> extras) {
            this.packageName = packageName;
            this.id = id;
            this.tag = tag;
            this.key = key;
            this.postTimeMillis = postTimeMillis;
            this.title = title;
            this.text = text;
            this.extras = extras == null ? Collections.<String, Object>emptyMap() : extras;
        }
    }

    public static String permissionSnapshot(EventListenerPermissionSnapshot snapshot) {
        return "permissions: call=" + granted(snapshot.isCallGranted())
                + " sms=" + granted(snapshot.isSmsGranted())
                + " notifications=" + granted(snapshot.isNotificationGranted());
    }

    public static String callState(String state) {
        return callState(state, null);
    }

    public static String callState(String state, String incomingNumber) {
        return callState(state, incomingNumber, Collections.<String, Object>emptyMap());
    }

    public static String callState(String state, String incomingNumber, Map<String, Object> extras) {
        StringBuilder out = new StringBuilder("{\"type\":\"call\",\"data\":{");
        boolean comma = appendStringField(out, "state", safeValue(state), false);
        comma = appendOptionalStringField(out, "incomingNumber", incomingNumber, comma);
        appendOptionalJsonField(out, "extras", extras, comma);
        out.append("}}");
        return out.toString();
    }

    public static String smsReceived(int messageCount) {
        return smsReceivedPlaceholder(Math.max(0, messageCount));
    }

    public static String smsReceived(List<SmsInfo> messages) {
        return smsReceived(messages, null, Collections.<String, Object>emptyMap());
    }

    public static String smsReceived(List<SmsInfo> messages, String format, Map<String, Object> extras) {
        List<SmsInfo> safeMessages = messages == null ? Collections.<SmsInfo>emptyList() : messages;
        StringBuilder out = new StringBuilder("{\"type\":\"sms\",\"data\":{");
        boolean comma = appendNumberField(out, "messageCount", safeMessages.size(), false);
        comma = appendOptionalStringField(out, "format", format, comma);
        if (!safeMessages.isEmpty()) {
            if (comma) {
                out.append(',');
            }
            out.append("\"messages\":[");
            for (int i = 0; i < safeMessages.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                appendSmsMessage(out, safeMessages.get(i));
            }
            out.append(']');
            comma = true;
        }
        appendOptionalJsonField(out, "extras", extras, comma);
        out.append("}}");
        return out.toString();
    }

    public static String notificationPosted(String packageName) {
        return notificationPosted(new NotificationInfo(packageName, null, null, null, null, null, null));
    }

    public static String notificationPosted(NotificationInfo info) {
        NotificationInfo safeInfo = info == null
                ? new NotificationInfo(null, null, null, null, null, null, null)
                : info;
        StringBuilder out = new StringBuilder("{\"type\":\"notification\",\"data\":{");
        boolean comma = appendStringField(out, "packageName", safeValue(safeInfo.packageName), false);
        comma = appendOptionalNumberField(out, "id", safeInfo.id, comma);
        comma = appendOptionalStringField(out, "tag", safeInfo.tag, comma);
        comma = appendOptionalStringField(out, "key", safeInfo.key, comma);
        comma = appendOptionalNumberField(out, "postTimeMillis", safeInfo.postTimeMillis, comma);
        comma = appendOptionalStringField(out, "title", safeInfo.title, comma);
        comma = appendOptionalStringField(out, "text", safeInfo.text, comma);
        appendOptionalJsonField(out, "extras", safeInfo.extras, comma);
        out.append("}}");
        return out.toString();
    }

    private static String granted(boolean granted) {
        return granted ? "granted" : "missing";
    }

    private static String safeValue(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim();
    }

    private static String smsReceivedPlaceholder(int messageCount) {
        StringBuilder out = new StringBuilder("{\"type\":\"sms\",\"data\":{");
        appendNumberField(out, "messageCount", messageCount, false);
        out.append("}}");
        return out.toString();
    }

    private static void appendSmsMessage(StringBuilder out, SmsInfo info) {
        out.append('{');
        boolean comma = false;
        if (info != null) {
            comma = appendOptionalStringField(out, "sender", info.sender, comma);
            comma = appendNumberField(out, "timestampMillis", info.timestampMillis, comma);
            comma = appendOptionalStringField(out, "body", info.body, comma);
            appendOptionalJsonField(out, "metadata", info.metadata, comma);
        }
        out.append('}');
    }

    private static boolean appendStringField(StringBuilder out, String name, String value, boolean comma) {
        if (comma) {
            out.append(',');
        }
        out.append(jsonString(name)).append(':').append(jsonString(value));
        return true;
    }

    private static boolean appendOptionalStringField(StringBuilder out, String name, String value, boolean comma) {
        if (value == null || value.trim().isEmpty()) {
            return comma;
        }
        return appendStringField(out, name, value.trim(), comma);
    }

    private static boolean appendNumberField(StringBuilder out, String name, long value, boolean comma) {
        if (comma) {
            out.append(',');
        }
        out.append(jsonString(name)).append(':').append(value);
        return true;
    }

    private static boolean appendOptionalNumberField(StringBuilder out, String name, Number value, boolean comma) {
        if (value == null) {
            return comma;
        }
        return appendNumberField(out, name, value.longValue(), comma);
    }

    private static boolean appendOptionalJsonField(StringBuilder out, String name, Object value, boolean comma) {
        if (isEmptyJsonValue(value)) {
            return comma;
        }
        if (comma) {
            out.append(',');
        }
        out.append(jsonString(name)).append(':').append(jsonValue(value));
        return true;
    }

    private static boolean isEmptyJsonValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }
        if (value instanceof Iterable) {
            return !((Iterable<?>) value).iterator().hasNext();
        }
        return false;
    }

    private static String jsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof CharSequence || value instanceof Character || value instanceof Enum) {
            return jsonString(String.valueOf(value));
        }
        if (value instanceof Map) {
            StringBuilder out = new StringBuilder("{");
            boolean comma = false;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (comma) {
                    out.append(',');
                }
                out.append(jsonString(String.valueOf(entry.getKey()))).append(':').append(jsonValue(entry.getValue()));
                comma = true;
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable) {
            StringBuilder out = new StringBuilder("[");
            boolean comma = false;
            for (Object item : (Iterable<?>) value) {
                if (comma) {
                    out.append(',');
                }
                out.append(jsonValue(item));
                comma = true;
            }
            return out.append(']').toString();
        }
        return jsonString(String.valueOf(value));
    }

    private static String jsonString(String value) {
        StringBuilder out = new StringBuilder("\"");
        String safeValue = value == null ? "" : value;
        for (int i = 0; i < safeValue.length(); i++) {
            char ch = safeValue.charAt(i);
            switch (ch) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '"':
                    out.append("\\\"");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    out.append(ch);
                    break;
            }
        }
        return out.append('"').toString();
    }
}
