package com.albrhi.tvwidget;

import android.content.Context;

/**
 * Safe area. Many TVs crop (overscan) the edges of the picture, so anything drawn right at the edge
 * can be cut off. Three levels, chosen from Quick settings > "Screen edges".
 */
final class Safe {
    private Safe() { }

    static final String[] NAMES = {"عادي", "متوسط", "كبير"};
    private static final int[] EXTRA_H = {0, 14, 28};
    private static final int[] EXTRA_V = {0, 8, 16};

    static int level(Context c) {
        return Math.max(0, Math.min(2, c.getSharedPreferences("ui", Context.MODE_PRIVATE).getInt("safe", 0)));
    }

    static void setLevel(Context c, int l) {
        c.getSharedPreferences("ui", Context.MODE_PRIVATE).edit().putInt("safe", l).apply();
        LayoutBackup.export(c);
        Theme.notifyChanged();
    }

    /** Horizontal margin from the screen edge, in px. */
    static int h(Context c) { return Math.round((60 + EXTRA_H[level(c)]) * c.getResources().getDisplayMetrics().density); }

    /** Vertical margin from the top/bottom edge, in px. */
    static int v(Context c) { return Math.round((30 + EXTRA_V[level(c)]) * c.getResources().getDisplayMetrics().density); }
}
