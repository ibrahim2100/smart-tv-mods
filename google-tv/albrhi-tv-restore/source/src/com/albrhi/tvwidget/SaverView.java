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

/** Full-screen Blaugrana stripes + the big widget, drifting slowly to avoid screen burn-in. */
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
        setBackgroundColor(0xFF060A18);
        addView(new Stripes(c), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        widget = new WidgetView(c, true);
        addView(widget, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }

    void start() { widget.start(); h.removeCallbacks(drift); h.postDelayed(drift, 60000); }

    void stop() { widget.stop(); h.removeCallbacks(drift); }

    static final class Stripes extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float strength;

        Stripes(Context c) { this(c, 1f); }

        Stripes(Context c, float strength) { super(c); this.strength = strength; }

        @Override protected void onDraw(Canvas cv) {
            int w = getWidth(), hh = getHeight();
            float sw = w / 14f;
            for (int i = 0; i < 14; i++) {
                p.setShader(null);
                int a = Math.round(0x2E * strength) << 24;
                p.setColor((i % 2 == 0) ? (a | 0xA50044) : (a | 0x004D98));
                cv.drawRect(i * sw, 0, (i + 1) * sw, hh, p);
            }
            p.setShader(new RadialGradient(w / 2f, hh / 2f, Math.max(w, hh) * 0.62f,
                    new int[]{0x00060A18, 0xF2060A18}, new float[]{0.25f, 1f}, Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, hh, p);
        }
    }
}
