package com.albrhi.tvwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Random;

/** Full-screen theme background + the big clock widget, drifting slowly to avoid screen burn-in. */
final class SaverView extends FrameLayout {
    private final WidgetView widget;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private final float dn;
    private final Runnable drift = new Runnable() {
        public void run() {
            widget.animate().translationX((rnd.nextFloat() * 2 - 1) * 40 * dn)
                    .translationY((rnd.nextFloat() * 2 - 1) * 24 * dn).setDuration(6000).start();
            h.postDelayed(this, 60000);
        }
    };

    SaverView(Context c) {
        super(c);
        dn = c.getResources().getDisplayMetrics().density;
        setBackgroundColor(Theme.base);
        addView(new Ambient(c), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        widget = new WidgetView(c, true);
        addView(widget, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }

    void start() { widget.start(); h.removeCallbacks(drift); h.postDelayed(drift, 60000); }

    void stop() { widget.stop(); h.removeCallbacks(drift); }
}
