package com.albrhi.tvwidget;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Look and feel, chosen in Quick settings > Appearance and kept in the "ui" preferences.
 * Everything here is read once at start-up ({@link #load}) and again after a change (the home screen is re-created).
 */
final class Theme {
    private Theme() { }

    static final String[] NAMES = {"منتصف الليل", "رمادي", "محيط", "غروب", "بنفسجي"};
    private static final int[] BASE = {0xFF070B16, 0xFF0B0C0F, 0xFF041110, 0xFF120B0A, 0xFF0C0914};
    private static final int[] GLOW = {0xFF2A4A8C, 0xFF3C424F, 0xFF0E6B6B, 0xFF8A3B2A, 0xFF4B2F8A};
    private static final int[] ACCENT = {0xFF7FA8FF, 0xFFDADFE8, 0xFF4FD6D0, 0xFFFFB26B, 0xFFB59CFF};

    static int index, base = BASE[0], glow = GLOW[0], accent = ACCENT[0];
    /** The app's colour tints the background glow (otherwise the theme's own colour stays). */
    static boolean follow = true;
    static boolean hijri, weather = true, clock24, sports;
    static String name = "";

    /** The colour shown for theme i in the picker. */
    static int swatch(int i) { return ACCENT[i]; }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("ui", Context.MODE_PRIVATE); }

    static void load(Context c) {
        SharedPreferences p = prefs(c);
        index = Math.max(0, Math.min(NAMES.length - 1, p.getInt("theme", 0)));
        base = BASE[index];
        glow = GLOW[index];
        accent = ACCENT[index];
        follow = p.getBoolean("follow", true);
        hijri = p.getBoolean("hijri", false);
        weather = p.getBoolean("weather", true);
        clock24 = p.getBoolean("clock24", false);
        sports = p.getBoolean("sports", false);
        name = p.getString("name", "");
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
    }

    /** Time pattern for the clock: 12-hour with a small AM/PM, or 24-hour. */
    static String timePattern() { return clock24 ? "HH:mm" : "h:mm"; }

    static String greeting(int hour) {
        String g = (hour >= 4 && hour < 12) ? "صباح الخير" : "مساء الخير";
        return name.length() > 0 ? g + " يا " + name : g;
    }
}
