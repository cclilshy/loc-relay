package com.cclilshy.tayd.common.permission;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public final class NotificationPermissionPolicy {
    private NotificationPermissionPolicy() {
    }

    public static boolean requiresRuntimePermission() {
        return Build.VERSION.SDK_INT >= 33;
    }

    public static boolean canPost(Context context) {
        if (!requiresRuntimePermission()) {
            return true;
        }
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
