package com.cclilshy.tayd.common.permission;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

public final class AutoStartPermissionPolicy {
    private AutoStartPermissionPolicy() {
    }

    public static boolean isReady(Context context) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return manager != null && manager.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static Intent guideIntent(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            Intent request = new Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + context.getPackageName()));
            if (request.resolveActivity(context.getPackageManager()) != null) {
                return request;
            }
            Intent list = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            if (list.resolveActivity(context.getPackageManager()) != null) {
                return list;
            }
        }
        Intent details = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        details.setData(Uri.parse("package:" + context.getPackageName()));
        return details;
    }
}
