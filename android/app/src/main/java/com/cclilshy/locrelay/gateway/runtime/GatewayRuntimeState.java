package com.cclilshy.locrelay.gateway.runtime;

public final class GatewayRuntimeState {
    private static volatile boolean active;

    private GatewayRuntimeState() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}
