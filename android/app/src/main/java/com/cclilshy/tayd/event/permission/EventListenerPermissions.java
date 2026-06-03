package com.cclilshy.tayd.event.permission;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import androidx.core.content.ContextCompat;

public final class EventListenerPermissions {
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    private EventListenerPermissions() {
    }

    public static EventListenerPermissionSnapshot snapshot(Context context) {
        return new EventListenerPermissionSnapshot(
                hasPermission(context, Manifest.permission.READ_PHONE_STATE),
                hasPermission(context, Manifest.permission.RECEIVE_SMS),
                hasNotificationAccess(context));
    }

    private static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasNotificationAccess(Context context) {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (enabled == null || enabled.trim().isEmpty()) {
            return false;
        }
        String packageName = context.getPackageName();
        for (String item : enabled.split(":")) {
            ComponentName component = ComponentName.unflattenFromString(item);
            if (component != null && packageName.equals(component.getPackageName())) {
                return true;
            }
        }
        return false;
    }
}
