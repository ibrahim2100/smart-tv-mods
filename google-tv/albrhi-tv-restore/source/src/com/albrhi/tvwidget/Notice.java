package com.albrhi.tvwidget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import static com.albrhi.tvwidget.WidgetView.*;

/**
 * A quiet status card over whatever is on screen (no sound, no focus): what was asked and how it went.
 * One card at a time: a new message updates it in place, so pressing a button twice never stacks cards.
 * The look follows the action: a coloured icon disc, a headline, an optional line below it and an optional big figure.
 */
final class Notice {
    private Notice() { }

    static final int WORKING = 0, DONE = 1, FAIL = 2;

    private static final Handler H = new Handler(Looper.getMainLooper());
    private static LinearLayout box;
    private static WindowManager wm;
    private static ImageView icon;
    private static TextView tLabel, tHead, tDetail, tBig;
    private static GradientDrawable bg, disc;
    private static ObjectAnimator pulse;
    private static Runnable hide;
    private static float dn;

    /** Safe from any thread. {@code detail} and {@code big} may be null. */
    static void show(final Context ctx, final int state, final int iconRes, final int accent,
                     final String label, final String head, final String detail, final String big) {
        H.post(new Runnable() { public void run() { doShow(ctx.getApplicationContext(), state, iconRes, accent, label, head, detail, big); } });
    }

    private static void doShow(Context c, int state, int iconRes, int accent, String label, String head, String detail, String big) {
        if (!Settings.canDrawOverlays(c)) {
            if (state != WORKING) Toast.makeText(c, head + (detail == null ? "" : " · " + detail), Toast.LENGTH_SHORT).show();
            return;
        }
        boolean fresh = box == null || !box.isAttachedToWindow();
        if (fresh) build(c);
        if (box == null) return;
        icon.setImageResource(iconRes);
        icon.setColorFilter(accent);
        disc.setColor(withAlpha(accent, state == WORKING ? 0x26 : 0x33));
        bg.setStroke(Math.round(1.5f * dn), withAlpha(accent, state == WORKING ? 0x40 : 0x99));
        tLabel.setText(label);
        tHead.setText(head);
        tDetail.setText(detail == null ? "" : detail);
        tDetail.setVisibility(detail == null || detail.length() == 0 ? View.GONE : View.VISIBLE);
        tBig.setText(big == null ? "" : big);
        tBig.setTextColor(accent);
        tBig.setVisibility(big == null || big.length() == 0 ? View.GONE : View.VISIBLE);

        if (pulse != null) { pulse.cancel(); pulse = null; }
        icon.setAlpha(1f);
        if (state == WORKING) {                                              // a slow breathing icon while the request is out
            pulse = ObjectAnimator.ofFloat(icon, View.ALPHA, 1f, 0.35f);
            pulse.setDuration(700);
            pulse.setRepeatMode(ObjectAnimator.REVERSE);
            pulse.setRepeatCount(ObjectAnimator.INFINITE);
            pulse.start();
        } else {                                                             // a small pop when it lands
            icon.setScaleX(0.8f);
            icon.setScaleY(0.8f);
            icon.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(new OvershootInterpolator(2.2f)).start();
        }
        if (hide != null) H.removeCallbacks(hide);
        hide = new Runnable() { public void run() { dismiss(); } };
        H.postDelayed(hide, state == WORKING ? 9000L : state == FAIL ? 4800L : 2800L);   // a stuck "working" card goes away too
    }

    private static int withAlpha(int color, int a) { return (a << 24) | (color & 0xFFFFFF); }

    private static void build(Context c) {
        dn = c.getResources().getDisplayMetrics().density;
        LinearLayout b = new LinearLayout(c);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setMinimumWidth(Math.round(380 * dn));
        bg = new GradientDrawable();
        bg.setColor(0xF5101830);
        bg.setCornerRadius(26 * dn);
        b.setBackground(bg);
        b.setPadding(Math.round(18 * dn), Math.round(16 * dn), Math.round(26 * dn), Math.round(16 * dn));
        b.setElevation(14 * dn);

        icon = new ImageView(c);
        int pad = Math.round(14 * dn);
        icon.setPadding(pad, pad, pad, pad);
        disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        icon.setBackground(disc);
        b.addView(icon, new LinearLayout.LayoutParams(Math.round(64 * dn), Math.round(64 * dn)));

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        tLabel = label(c, 12, MUTED, false);
        tHead = label(c, 23, WHITE, true);
        tDetail = label(c, 14, MUTED, false);
        col.addView(tLabel);
        col.addView(tHead);
        col.addView(tDetail);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
        cp.setMargins(Math.round(16 * dn), 0, Math.round(10 * dn), 0);
        b.addView(col, cp);

        tBig = label(c, 40, WHITE, true);
        b.addView(tBig);

        WindowManager w = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = Safe.v(c) + Math.round(56 * dn);
        try {
            b.setAlpha(0f);
            b.setTranslationY(-14 * dn);
            w.addView(b, lp);
            b.animate().alpha(1f).translationY(0f).setDuration(240).start();
            box = b;
            wm = w;
        } catch (Throwable t) {
            box = null;
        }
    }

    private static TextView label(Context c, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) t.setTypeface(Fonts.bold);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private static void dismiss() {
        final View v = box;
        final WindowManager w = wm;
        box = null;
        hide = null;
        if (pulse != null) { pulse.cancel(); pulse = null; }
        if (v == null || w == null) return;
        v.animate().alpha(0f).translationY(-10 * dn).setDuration(260).withEndAction(new Runnable() {
            public void run() { try { w.removeView(v); } catch (Throwable ignored) { } }
        }).start();
    }
}
