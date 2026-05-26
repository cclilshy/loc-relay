package com.cclilshy.locrelay.proxy.domain;

public final class ProxyMapping {
    private final String name;
    private final String type;
    private final String localIp;
    private final int localPort;
    private final int remotePort;

    public ProxyMapping(String name, String type, String localIp, int localPort, int remotePort) {
        this.name = requireNonBlank(name, "name");
        this.type = requireNonBlank(type, "type");
        this.localIp = requireNonBlank(localIp, "localIp");
        this.localPort = requirePort(localPort, "localPort");
        this.remotePort = requirePort(remotePort, "remotePort");
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getLocalIp() {
        return localIp;
    }

    public int getLocalPort() {
        return localPort;
    }

    public int getRemotePort() {
        return remotePort;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static int requirePort(int value, String field) {
        if (value < 1 || value > 65535) {
            throw new IllegalArgumentException(field + " must be between 1 and 65535");
        }
        return value;
    }
}
