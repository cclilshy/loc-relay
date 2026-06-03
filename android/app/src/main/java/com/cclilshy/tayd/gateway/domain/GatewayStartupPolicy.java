package com.cclilshy.tayd.gateway.domain;

public final class GatewayStartupPolicy {
    private GatewayStartupPolicy() {
    }

    public static boolean isAutoStartEnabledByDefault() {
        return false;
    }

    public static boolean isStartServiceWithAppEnabledByDefault() {
        return false;
    }

    public static boolean isPersistentNotificationEnabledByDefault() {
        return false;
    }

    public static boolean shouldStartServiceWithApp(boolean enabled, boolean gatewayActive) {
        return enabled && !gatewayActive;
    }

    public static boolean shouldRequestPermission(boolean requestedEnabled, boolean permissionGranted) {
        return requestedEnabled && !permissionGranted;
    }

    public static boolean nextGuardedSwitchState(boolean requestedEnabled, boolean permissionGranted) {
        return requestedEnabled && permissionGranted;
    }

    public static boolean shouldRequestNotificationPermissionAfterManualStart(
            boolean manualStartPending,
            boolean gatewayStarted,
            boolean alreadyPrompted,
            boolean permissionGranted) {
        return manualStartPending && gatewayStarted && !alreadyPrompted && !permissionGranted;
    }

    public static boolean shouldShowServiceNotificationWarning(
            boolean gatewayActive,
            boolean serviceNotificationEnabled,
            boolean permissionGranted) {
        return gatewayActive && (!serviceNotificationEnabled || !permissionGranted);
    }
}
