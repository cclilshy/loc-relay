package com.cclilshy.locrelay.gateway.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GatewayStartupPolicyTest {
    @Test
    public void defaultsMatchSettingsContract() {
        assertFalse(GatewayStartupPolicy.isAutoStartEnabledByDefault());
        assertFalse(GatewayStartupPolicy.isStartServiceWithAppEnabledByDefault());
        assertFalse(GatewayStartupPolicy.isPersistentNotificationEnabledByDefault());
    }

    @Test
    public void startsWithAppOnlyWhenEnabledAndStopped() {
        assertTrue(GatewayStartupPolicy.shouldStartServiceWithApp(true, false));
        assertFalse(GatewayStartupPolicy.shouldStartServiceWithApp(true, true));
        assertFalse(GatewayStartupPolicy.shouldStartServiceWithApp(false, false));
    }

    @Test
    public void guardedSwitchesOnlyRequestPermissionWhenTurningOn() {
        assertFalse(GatewayStartupPolicy.shouldRequestPermission(false, false));
        assertFalse(GatewayStartupPolicy.nextGuardedSwitchState(false, false));

        assertTrue(GatewayStartupPolicy.shouldRequestPermission(true, false));
        assertFalse(GatewayStartupPolicy.nextGuardedSwitchState(true, false));

        assertFalse(GatewayStartupPolicy.shouldRequestPermission(true, true));
        assertTrue(GatewayStartupPolicy.nextGuardedSwitchState(true, true));
    }

    @Test
    public void firstManualStartRequestsNotificationPermissionOnlyAfterServiceRuns() {
        assertFalse(GatewayStartupPolicy.shouldRequestNotificationPermissionAfterManualStart(false, true, false, false));
        assertFalse(GatewayStartupPolicy.shouldRequestNotificationPermissionAfterManualStart(true, false, false, false));
        assertFalse(GatewayStartupPolicy.shouldRequestNotificationPermissionAfterManualStart(true, true, true, false));
        assertFalse(GatewayStartupPolicy.shouldRequestNotificationPermissionAfterManualStart(true, true, false, true));
        assertTrue(GatewayStartupPolicy.shouldRequestNotificationPermissionAfterManualStart(true, true, false, false));
    }

    @Test
    public void serviceNotificationWarningShowsOnlyWhenRunningWithoutEffectiveNotification() {
        assertFalse(GatewayStartupPolicy.shouldShowServiceNotificationWarning(false, false, false));
        assertTrue(GatewayStartupPolicy.shouldShowServiceNotificationWarning(true, false, false));
        assertTrue(GatewayStartupPolicy.shouldShowServiceNotificationWarning(true, true, false));
        assertTrue(GatewayStartupPolicy.shouldShowServiceNotificationWarning(true, false, true));
        assertFalse(GatewayStartupPolicy.shouldShowServiceNotificationWarning(true, true, true));
    }
}
