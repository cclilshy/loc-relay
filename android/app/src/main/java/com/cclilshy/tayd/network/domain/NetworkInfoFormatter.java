package com.cclilshy.tayd.network.domain;

import java.util.List;

public final class NetworkInfoFormatter {
    private NetworkInfoFormatter() {
    }

    public static String format(List<NetworkInterfaceInfo> interfaces) {
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        out.append("  \"interfaces\": [\n");
        for (int i = 0; i < interfaces.size(); i++) {
            NetworkInterfaceInfo item = interfaces.get(i);
            out.append("    {\n");
            out.append("      \"name\": ").append(jsonString(item.getName())).append(",\n");
            out.append("      \"displayName\": ").append(jsonNullableString(item.getDisplayName())).append(",\n");
            out.append("      \"up\": ").append(item.isUp()).append(",\n");
            out.append("      \"loopback\": ").append(item.isLoopback()).append(",\n");
            out.append("      \"mtu\": ").append(item.getMtu()).append(",\n");
            out.append("      \"addresses\": [");
            if (!item.getAddresses().isEmpty()) {
                out.append('\n');
            }
            for (int j = 0; j < item.getAddresses().size(); j++) {
                out.append("        ").append(jsonString(item.getAddresses().get(j)));
                if (j < item.getAddresses().size() - 1) {
                    out.append(',');
                }
                out.append('\n');
            }
            if (!item.getAddresses().isEmpty()) {
                out.append("      ");
            }
            out.append("]\n");
            out.append("    }");
            if (i < interfaces.size() - 1) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append("  ]\n");
        out.append("}");
        return out.toString();
    }

    private static String jsonNullableString(String value) {
        return value == null ? "null" : jsonString(value);
    }

    private static String jsonString(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
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
