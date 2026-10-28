package com.albrhi.tvwidget;

import android.content.Context;
import android.graphics.Typeface;

/** IBM Plex Sans Arabic (SIL OFL) in the three weights the launcher uses. */
final class Fonts {
    private Fonts() { }

    static volatile Typeface regular = Typeface.DEFAULT;
    static volatile Typeface medium = Typeface.DEFAULT_BOLD;
    static volatile Typeface bold = Typeface.DEFAULT_BOLD;

    static void init(Context c) {
        try {
            Typeface r = c.getResources().getFont(R.font.plex_regular);
            Typeface m = c.getResources().getFont(R.font.plex_medium);
            Typeface b = c.getResources().getFont(R.font.plex_bold);
            if (r != null) regular = r;
            if (m != null) medium = m;
            if (b != null) bold = b;
        } catch (Throwable ignored) { }       // fall back to the system font
    }
}
