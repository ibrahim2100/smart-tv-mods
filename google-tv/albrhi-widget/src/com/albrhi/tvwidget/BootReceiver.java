package com.albrhi.tvwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** After a restart or an app update: bring the background service (match alerts) back and re-arm the alarms. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        try {
            c.startForegroundService(new Intent(c, AppService.class));
        } catch (Throwable ignored) { }
    }
}
