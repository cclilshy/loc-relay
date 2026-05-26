package com.cclilshy.locrelay.frpc.domain;

import com.cclilshy.locrelay.proxy.domain.ProxyMapping;

import java.util.List;

public final class FrpcConfigRenderer {
    private FrpcConfigRenderer() {
    }

    public static String renderClient(
            String serverAddr,
            int serverPort,
            String token,
            String includeDir,
            List<ProxyMapping> mappings) {
        StringBuilder out = new StringBuilder();
        out.append("serverAddr = ").append(tomlString(serverAddr)).append('\n');
        out.append("serverPort = ").append(validatePort(serverPort, "serverPort")).append('\n');
        out.append('\n');
        out.append("auth.method = \"token\"\n");
        out.append("auth.token = ").append(tomlString(token)).append('\n');
        out.append('\n');
        out.append("includes = [").append(tomlString(includeDir + "/*.toml")).append("]\n");
        if (mappings != null && !mappings.isEmpty()) {
            out.append('\n');
            for (ProxyMapping mapping : mappings) {
                out.append(renderProxy(mapping)).append('\n');
            }
        }
        return out.toString();
    }

    public static String renderProxy(ProxyMapping mapping) {
        return new StringBuilder()
                .append("[[proxies]]\n")
                .append("name = ").append(tomlString(mapping.getName())).append('\n')
                .append("type = ").append(tomlString(mapping.getType())).append('\n')
                .append("localIP = ").append(tomlString(mapping.getLocalIp())).append('\n')
                .append("localPort = ").append(mapping.getLocalPort()).append('\n')
                .append("remotePort = ").append(mapping.getRemotePort()).append('\n')
                .toString();
    }

    private static String tomlString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value is required");
        }
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(ch);
                    break;
            }
        }
        return escaped.append('"').toString();
    }

    private static int validatePort(int value, String field) {
        if (value < 1 || value > 65535) {
            throw new IllegalArgumentException(field + " must be between 1 and 65535");
        }
        return value;
    }
}
