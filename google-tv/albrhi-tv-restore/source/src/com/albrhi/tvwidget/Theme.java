package com.albrhi.tvwidget;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Look and feel, chosen in Quick settings > Appearance and kept in the "ui" preferences.
 * Everything here is read once at start-up ({@link #load}) and again after a change (the home screen is re-created).
 */
final class Theme {
    private Theme() { }

    static final String[] NAMES = {"منتصف الليل", "رمادي", "محيط", "غروب", "بنفسجي", "أسود"};
    private static final int[] BASE = {0xFF070B16, 0xFF0B0C0F, 0xFF041110, 0xFF120B0A, 0xFF0C0914, 0xFF000000};
    private static final int[] GLOW = {0xFF2A4A8C, 0xFF3C424F, 0xFF0E6B6B, 0xFF8A3B2A, 0xFF4B2F8A, 0xFF000000};
    private static final int[] ACCENT = {0xFF7FA8FF, 0xFFDADFE8, 0xFF4FD6D0, 0xFFFFB26B, 0xFFB59CFF, 0xFFFFFFFF};

    static int index, base = BASE[0], glow = GLOW[0], accent = ACCENT[0];
    /** The app's colour tints the background glow (otherwise the theme's own colour stays). */
    static boolean follow = true;
    /** The pure-black theme: the background stays black whatever app is focused (no tint). */
    static boolean black;
    static boolean hijri, weather = true, clock24, sports;
    /** Rows of apps on the home screen: 1 or 2. */
    static int rows = 1;
    /** The clean layout: no app name, no Open/Options buttons, no big artwork behind; bigger tiles and a framed focus. */
    static boolean clean;
    /** Free memory by ending background apps before opening an app when RAM is low. Off by default: it can close the app you just left. */
    static boolean autoClean;
    static String name = "";
    /** Set by the home screen while it is on screen: called on the UI thread right after any setting changes, so the screen updates at once. */
    static Runnable onChange;

    /** The colour shown for theme i in the picker. */
    static int swatch(int i) { return i == NAMES.length - 1 ? 0xFF000000 : ACCENT[i]; }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("ui", Context.MODE_PRIVATE); }

    static void load(Context c) {
        SharedPreferences p = prefs(c);
        index = Math.max(0, Math.min(NAMES.length - 1, p.getInt("theme", 0)));
        black = index == NAMES.length - 1;
        base = BASE[index];
        glow = GLOW[index];
        accent = ACCENT[index];
        follow = p.getBoolean("follow", true);
        hijri = p.getBoolean("hijri", false);
        weather = p.getBoolean("weather", true);
        clock24 = p.getBoolean("clock24", false);
        sports = p.getBoolean("sports", false);
        rows = p.getInt("rows", 1) >= 2 ? 2 : 1;
        clean = p.getBoolean("clean", false);
        autoClean = p.getBoolean("autoclean", false);
        name = p.getString("name", "");
        WidgetView.CARD = black ? 0xFF161616 : 0xFF121B38;
        WidgetView.BOX = black ? 0xFF222222 : 0xFF1B2547;
        WidgetView.GOLD = accent;          // the old names stay: they now mean "the accent" and "the theme's glow"
        WidgetView.GARNET = accent;
        WidgetView.BLUE = glow;
    }

    static void set(Context c, String key, int v) { prefs(c).edit().putInt(key, v).apply(); changed(c); }

    static void set(Context c, String key, boolean v) { prefs(c).edit().putBoolean(key, v).apply(); changed(c); }

    static void set(Context c, String key, String v) { prefs(c).edit().putString(key, v).apply(); changed(c); }

    private static void changed(Context c) {
        load(c);
        LayoutBackup.export(c);
        notifyChanged();
    }

    static void notifyChanged() { Runnable r = onChange; if (r != null) r.run(); }

    /** Time pattern for the clock: 12-hour with a small AM/PM, or 24-hour. */
    static String timePattern() { return clock24 ? "HH:mm" : "h:mm"; }

    static String greeting(int hour) {
        String g = (hour >= 4 && hour < 12) ? "صباح الخير" : "مساء الخير";
        return name.length() > 0 ? g + " يا " + name : g;
    }
}
