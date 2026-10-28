package com.albrhi.tvwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.List;

/** Schedules the "15 minutes to kick-off" and "kick-off" alerts for the next Barcelona match. */
final class Alerts {
    private Alerts() { }

    static final String ACTION_PRE = "com.albrhi.tvwidget.ALERT_PRE";
    static final String ACTION_KICK = "com.albrhi.tvwidget.ALERT_KICK";
    static final long PRE_MS = 15L * 60 * 1000;

    static void schedule(Context ctx) {
        Context c = ctx.getApplicationContext();
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(pending(c, ACTION_PRE, 0));
        am.cancel(pending(c, ACTION_KICK, 0));
        if (!Theme.sports) return;
        long now = System.currentTimeMillis();
        List<Fixtures.Match> up = Fixtures.upcoming;
        Fixtures.Match m = null;
        for (Fixtures.Match x : up) if (x.start > now && !x.postponed) { m = x; break; }
        if (m == null) return;
        arm(am, c, m.start - PRE_MS, ACTION_PRE, m.start, now);
        arm(am, c, m.start, ACTION_KICK, m.start, now);
    }

    private static void arm(AlarmManager am, Context c, long at, String action, long matchStart, long now) {
        if (at <= now) return;
        PendingIntent pi = pending(c, action, matchStart);
        try {
            if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (Throwable t) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    private static PendingIntent pending(Context c, String action, long matchStart) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(action).putExtra("start", matchStart);
        return PendingIntent.getBroadcast(c, action.hashCode(), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
