package com.cclilshy.tayd.gateway.runtime;

import com.cclilshy.tayd.gateway.data.GatewayPrefs;
import com.cclilshy.tayd.gateway.domain.GatewayStartupPolicy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public final class GatewayBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        SharedPreferences prefs = GatewayPrefs.get(context);
        if (!prefs.getBoolean(
                GatewayPrefs.KEY_AUTO_START_ENABLED,
                GatewayStartupPolicy.isAutoStartEnabledByDefault())) {
            return;
        }
        Intent service = new Intent(context, GatewayService.class)
                .setAction(GatewayService.ACTION_START)
                .putExtra(GatewayService.EXTRA_FORCE_FOREGROUND, true);
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
    }
}
