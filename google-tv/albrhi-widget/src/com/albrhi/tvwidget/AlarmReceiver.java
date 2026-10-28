package com.albrhi.tvwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        try {
            c.startForegroundService(new Intent(c, AppService.class).setAction(i.getAction()).putExtras(i));
        } catch (Throwable ignored) { }
    }
}
