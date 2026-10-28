package com.albrhi.tvwidget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/**
 * The home background: a deep navy base with a soft glow in the colour of the focused app.
 *
 * Cheap on purpose: each colour is painted ONCE into a small bitmap (320x180, scaled up smoothly by the GPU),
 * and a change of colour is just a cross-fade between two such bitmaps. The first version re-evaluated two
 * full-screen gradient shaders on every frame, which was the heaviest thing on screen on this TV.
 */
final class Ambient extends FrameLayout {
    private static final int BW = 320, BH = 180;
    private static final Map<String, Integer> COLORS = new HashMap<String, Integer>();
    private static final LruCache<Integer, Bitmap> BITMAPS = new LruCache<Integer, Bitmap>(5);

    private final ImageView front, back;       // a cross-fade is just the alpha of `back`, run by the render thread (no redraw per frame)
    private int color;

    Ambient(Context c) {
        super(c);
        front = layer(c);
        back = layer(c);
        back.setAlpha(0f);
        color = Theme.glow;
        front.setImageBitmap(bitmapFor(color));
    }

    private ImageView layer(Context c) {
        ImageView v = new ImageView(c);
        v.setScaleType(ImageView.ScaleType.FIT_XY);
        addView(v, new FrameLayout.LayoutParams(-1, -1));
        return v;
    }

    /** The colour the glow is heading to (also used for the artwork's rim light). */
    int currentColor() { return color; }

    /** Fades the glow to the colour of this app. */
    void follow(String key, Drawable art) { setGlow(Theme.follow ? colorOf(key, art) : Theme.glow); }

    /** The theme changed: repaint with the new base colour and fade to the colour the glow should have now. */
    void retheme(String key, Drawable art) {
        int target = Theme.follow && key != null ? colorOf(key, art) : Theme.glow;
        setGlow(target, true);
    }

    void setGlow(int target) { setGlow(target, false); }

    private void setGlow(int target, boolean force) {
        if (target == color && !force) return;
        color = target;
        back.animate().cancel();
        Bitmap next = bitmapFor(target);
        if (back.getAlpha() > 0.5f) front.setImageDrawable(back.getDrawable());     // an unfinished fade: settle on where it was heading
        back.setImageBitmap(next);
        back.setAlpha(0f);
        final Bitmap landed = next;
        back.animate().alpha(1f).setDuration(260).withEndAction(new Runnable() {
            public void run() { front.setImageBitmap(landed); back.setAlpha(0f); }
        }).start();
    }

    // ---- painting a colour once ---------------------------------------------------------------

    private static Bitmap bitmapFor(int c) {
        final int key = c ^ (Theme.base * 31);           // the cache key includes the theme's base colour
        Bitmap b = BITMAPS.get(key);
        if (b != null && !b.isRecycled()) return b;
        b = Bitmap.createBitmap(BW, BH, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        cv.drawColor(Theme.base);
        Paint g = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        int r = Color.red(c), gr = Color.green(c), bl = Color.blue(c);
        g.setShader(new RadialGradient(BW * 0.76f, BH * 0.30f, BW * 0.78f,
                new int[]{Color.argb(170, r, gr, bl), Color.argb(60, r, gr, bl), 0x00000000}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, BW, BH, g);
        g.setShader(new LinearGradient(0, BH * 0.40f, 0, BH, 0x00000000, (0xEE000000 | (Theme.base & 0x00FFFFFF)), Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, BW, BH, g);                 // darkens the lower half so the rows stay readable
        BITMAPS.put(key, b);
        return b;
    }

    // ---- colour from artwork ----------------------------------------------------------------

    private static int colorOf(String key, Drawable art) {
        Integer hit = COLORS.get(key);
        if (hit != null) return hit;
        int c = Theme.glow;                       // the theme's own colour as the fallback
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
                    Color.RGBToHSV((int) (r / wsum), (int) (g / wsum), (int) (bl / wsum), hsv);
                    hsv[1] = Math.min(1f, hsv[1] * 1.05f);
                    hsv[2] = Math.min(0.78f, Math.max(0.5f, hsv[2]));      // never so bright it hurts, never so dark it vanishes
                    c = Color.HSVToColor(hsv);
                }
            }
        } catch (Throwable ignored) { }
        COLORS.put(key, c);
        return c;
    }
}
