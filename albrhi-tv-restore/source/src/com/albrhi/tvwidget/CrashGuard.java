package com.albrhi.tvwidget;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Remembers recent crashes. If the launcher dies three times within two minutes, Home shows an
 * emergency screen (retry / re-enable Google TV / Settings) instead of looping into a black screen.
 */
final class CrashGuard {
    private CrashGuard() { }

    private static final long WINDOW_MS = 2L * 60 * 1000;
    private static final int LIMIT = 3;

    static void install(final Context app) {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                try { record(app, e); } catch (Throwable ignored) { }
                if (prev != null) prev.uncaughtException(t, e);
            }
        });
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("crash", Context.MODE_PRIVATE);
    }

    static void record(Context c, Throwable e) {
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        StringBuilder keep = new StringBuilder();
        for (String s : p.getString("times", "").split(",")) {
            if (s.length() == 0) continue;
            try { if (now - Long.parseLong(s) < WINDOW_MS) keep.append(s).append(','); } catch (NumberFormatException ignored) { }
        }
        keep.append(now);
        p.edit().putString("times", keep.toString())
                .putString("last", e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage())).commit();
    }

    static boolean tripped(Context c) {
        long now = System.currentTimeMillis();
        int n = 0;
        for (String s : prefs(c).getString("times", "").split(",")) {
            if (s.length() == 0) continue;
            try { if (now - Long.parseLong(s) < WINDOW_MS) n++; } catch (NumberFormatException ignored) { }
        }
        return n >= LIMIT;
    }

    static String lastError(Context c) { return prefs(c).getString("last", ""); }

    static void reset(Context c) { prefs(c).edit().remove("times").apply(); }

    /** For the "try the emergency screen" menu entry. */
    static void simulate(Context c) {
        for (int i = 0; i < LIMIT; i++) record(c, new RuntimeException("test (not a real crash)"));
    }
}
