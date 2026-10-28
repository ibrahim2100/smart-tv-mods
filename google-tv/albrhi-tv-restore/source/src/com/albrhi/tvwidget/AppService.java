package com.albrhi.tvwidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.json.JSONObject;

import java.util.List;

import static com.albrhi.tvwidget.WidgetView.*;

/** Keeps the match alerts and the live-result check alive in the background. */
public class AppService extends Service {
    static final String ACTION_TEST = "com.albrhi.tvwidget.TEST";
    static final String ACTION_CHANGED = "com.albrhi.tvwidget.CHANGED";
    private static final String CHANNEL = "albrhi_bg";
    /** How long every match alert stays on screen. */
    private static final int ALERT_SECONDS = 10;
    private static final long POLL_MS = 10L * 60 * 1000;

    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean polling;

    private final Runnable pollTick = new Runnable() {
        public void run() { pollOnce(); }
    };

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Albrhi TV", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle("Albrhi TV").setContentText("تنبيهات المباريات").build();
        try {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } catch (Throwable t) {
            startForeground(1, n);
        }
        Fixtures.init(this);
        RemoteServer.get(this).start();
    }

    @Override public int onStartCommand(Intent i, int flags, int startId) {
        Fixtures.init(this);
        Alerts.schedule(this);
        String a = i == null ? null : i.getAction();
        if (Alerts.ACTION_PRE.equals(a) || ACTION_TEST.equals(a)) alertPre();
        else if (Alerts.ACTION_KICK.equals(a)) { alertKick(); startPolling(); }
        else if (ACTION_CHANGED.equals(a)) alertChanged(i.getStringExtra("home"), i.getStringExtra("away"),
                i.getStringExtra("league"), i.getLongExtra("start", 0), i.getLongExtra("old", 0), i.getBooleanExtra("pp", false),
                i.getStringExtra("hb"), i.getStringExtra("ab"));
        else startPolling();      // service (re)started: pick up a match that is already in progress
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        RemoteServer.get(this).stop();
        super.onDestroy();
    }

    // ---- alerts -----------------------------------------------------------------------------

    private Fixtures.Match nextMatch() {
        long now = System.currentTimeMillis();
        List<Fixtures.Match> up = Fixtures.upcoming;
        for (Fixtures.Match m : up) if (m.start > now - 60000L) return m;
        return null;
    }

    private void show(final String title, final String l1, final String l2, final Fixtures.Match m, final int sec) {
        new Thread(new Runnable() {
            public void run() {
                Fixtures.preloadBadges(AppService.this);
                Banner.show(AppService.this, title, l1, l2, Fixtures.badge(m.homeBadge), Fixtures.badge(m.awayBadge), sec);
            }
        }).start();
    }

    private static String names(Fixtures.Match m) { return Fixtures.ar(m.home) + "  ×  " + Fixtures.ar(m.away); }

    private void alertPre() {
        Fixtures.Match m = nextMatch();
        if (m == null) return;
        show("بعد 15 دقيقة", names(m), Fixtures.ar(m.league) + " · " + fmt("h:mm a", GREG, m.start), m, ALERT_SECONDS);
    }

    private void alertKick() {
        Fixtures.Match m = nextMatch();
        if (m == null) return;
        show("انطلقت المباراة", names(m), Fixtures.ar(m.league), m, ALERT_SECONDS);
    }

    private void alertChanged(String home, String away, String league, long start, long old, boolean pp, String hb, String ab) {
        Fixtures.Match m = new Fixtures.Match();
        m.home = home;
        m.away = away;
        m.league = league;
        m.homeBadge = hb;
        m.awayBadge = ab;
        String l2 = pp ? "المباراة مؤجلة" : "الموعد الجديد: " + dayLabel(start) + " " + fmt("h:mm a", GREG, start)
                + "  (كان " + dayLabel(old) + " " + fmt("h:mm a", GREG, old) + ")";
        show(pp ? "تأجيل مباراة" : "تغيّر موعد المباراة", names(m), l2, m, ALERT_SECONDS);
    }

    /** Menu entry "try the alert": shows the 15-minute banner for the next match right now. */
    static void testAlert(Context c) {
        try {
            c.startForegroundService(new Intent(c, AppService.class).setAction(ACTION_TEST));
        } catch (Throwable ignored) { }
    }

    /** Called by Fixtures when a refresh moves the next match. */
    static void notifyChange(Context c, Fixtures.Match m, long oldStart) {
        try {
            c.startForegroundService(new Intent(c, AppService.class).setAction(ACTION_CHANGED)
                    .putExtra("home", m.home).putExtra("away", m.away).putExtra("league", m.league)
                    .putExtra("start", m.start).putExtra("old", oldStart).putExtra("pp", m.postponed)
                    .putExtra("hb", m.homeBadge).putExtra("ab", m.awayBadge));
        } catch (Throwable ignored) { }
    }

    // ---- live result ------------------------------------------------------------------------

    private Fixtures.Match liveMatch() {
        long now = System.currentTimeMillis();
        for (Fixtures.Match m : Fixtures.upcoming) {
            if (m.id != null && m.start <= now + 60000L && now < m.start + 3L * 60 * 60 * 1000) return m;
        }
        return null;
    }

    private void startPolling() {
        if (polling || liveMatch() == null) return;
        polling = true;
        h.postDelayed(pollTick, 2 * 60 * 1000L);
    }

    private void pollOnce() {
        final Fixtures.Match m = liveMatch();
        if (m == null) { polling = false; return; }
        new Thread(new Runnable() {
            public void run() {
                JSONObject o = Fixtures.lookupEvent(m.id);
                boolean done = false;
                if (o != null) {
                    String st = o.optString("strStatus");
                    Fixtures.Match r = Fixtures.parseEvent(o);
                    boolean finished = "FT".equals(st) || "AET".equals(st) || "PEN".equals(st) || "Match Finished".equalsIgnoreCase(st);
                    SharedPreferences p = getSharedPreferences("alerts", Context.MODE_PRIVATE);
                    if (finished && r != null && r.homeScore != null && !p.getBoolean("fin_" + m.id, false)) {
                        p.edit().putBoolean("fin_" + m.id, true).apply();
                        done = true;
                        show("انتهت المباراة", Fixtures.ar(r.home) + "  " + r.homeScore + " - " + r.awayScore + "  " + Fixtures.ar(r.away),
                                Fixtures.ar(r.league), r, ALERT_SECONDS);
                        Fixtures.refresh(AppService.this, true);     // move on to the next match
                    }
                }
                final boolean fin = done;
                h.post(new Runnable() {
                    public void run() {
                        if (fin || liveMatch() == null) polling = false;
                        else h.postDelayed(pollTick, POLL_MS);
                    }
                });
            }
        }).start();
    }
}
