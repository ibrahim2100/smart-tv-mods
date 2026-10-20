package com.albrhi.tvwidget;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/**
 * The home background: a deep navy base and a soft glow that takes the colour of the focused app.
 * The colour is worked out once per app from its own banner (saturated pixels count most), then
 * the glow cross-fades to it.
 */
final class Ambient extends View {
    private static final int BASE = 0xFF050816;
    private static final Map<String, Integer> CACHE = new HashMap<String, Integer>();

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color = 0xFF1B3A7A;
    private ValueAnimator anim;
    private Shader glow, floor;
    private int lastW, lastH, lastColor;

    Ambient(Context c) {
        super(c);
        setWillNotDraw(false);
    }

    /** Fades the glow to the colour of this app. */
    void follow(String key, Drawable art) {
        setGlow(colorOf(key, art));
    }

    void setGlow(int target) {
        if (target == color && anim == null) return;
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofObject(new ArgbEvaluator(), color, target);
        anim.setDuration(380);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                color = (Integer) a.getAnimatedValue();
                invalidate();
            }
        });
        anim.start();
    }

    @Override protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        if (w == 0) return;
        cv.drawColor(BASE);
        if (glow == null || w != lastW || h != lastH || color != lastColor) {
            int strong = Color.argb(215, Color.red(color), Color.green(color), Color.blue(color));
            int mid = Color.argb(90, Color.red(color), Color.green(color), Color.blue(color));
            glow = new RadialGradient(w * 0.76f, h * 0.30f, w * 0.78f, new int[]{strong, mid, 0x00000000}, new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP);
            floor = new LinearGradient(0, h * 0.40f, 0, h, 0x00000000, 0xEE04060F, Shader.TileMode.CLAMP);
            lastW = w;
            lastH = h;
            lastColor = color;
        }
        p.setShader(glow);
        cv.drawRect(0, 0, w, h, p);
        p.setShader(floor);
        cv.drawRect(0, 0, w, h, p);          // darkens the lower half so the rows stay readable
        p.setShader(null);
    }

    // ---- colour from artwork ----------------------------------------------------------------

    private static int colorOf(String key, Drawable art) {
        Integer hit = CACHE.get(key);
        if (hit != null) return hit;
        int c = 0xFF1B3A7A;                       // Blaugrana blue as the fallback
        try {
            if (art != null && art.getIntrinsicWidth() > 0) {
                Bitmap b = Bitmap.createBitmap(32, 18, Bitmap.Config.ARGB_8888);
                Canvas cv = new Canvas(b);
                Drawable d = art.getConstantState() != null ? art.getConstantState().newDrawable().mutate() : art;
                d.setBounds(0, 0, 32, 18);
                d.draw(cv);
                float r = 0, g = 0, bl = 0, wsum = 0;
                float[] hsv = new float[3];
                for (int y = 0; y < 18; y++) {
                    for (int x = 0; x < 32; x++) {
                        int px = b.getPixel(x, y);
                        if (Color.alpha(px) < 200) continue;
                        Color.colorToHSV(px, hsv);
                        float weight = hsv[1] * hsv[1] * (hsv[2] > 0.25f ? 1f : 0.1f) * (hsv[2] > 0.97f && hsv[1] < 0.15f ? 0f : 1f);
                        r += Color.red(px) * weight;
                        g += Color.green(px) * weight;
                        bl += Color.blue(px) * weight;
                        wsum += weight;
                    }
                }
                b.recycle();
                if (wsum > 1.2f) {
                    int rr = (int) (r / wsum), gg = (int) (g / wsum), bb = (int) (bl / wsum);
                    Color.RGBToHSV(rr, gg, bb, hsv);
                    hsv[1] = Math.min(1f, hsv[1] * 1.05f);
                    hsv[2] = Math.min(0.78f, Math.max(0.5f, hsv[2]));      // never so bright it hurts, never so dark it vanishes
                    c = Color.HSVToColor(hsv);
                }
            }
        } catch (Throwable ignored) { }
        CACHE.put(key, c);
        return c;
    }
}
