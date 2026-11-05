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
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import static com.albrhi.tvwidget.WidgetView.*;

/**
 * Every message the launcher shows lives here: a small, quiet card that slides in from the right edge, never takes
 * focus or touches, and slides out again. One card at a time: a new message updates it in place, so pressing a
 * button twice never stacks cards. {@link #show} is a status (working / done / failed), {@link #toast} a plain note.
 */
final class Notice {
    private Notice() { }

    static final int WORKING = 0, DONE = 1, FAIL = 2;

    private static final Handler H = new Handler(Looper.getMainLooper());
    private static LinearLayout box;
    private static WindowManager wm;
    private static ImageView icon;
    private static TextView tHead, tSub, tBig;
    private static GradientDrawable bg, disc;
    private static ObjectAnimator pulse;
    private static Runnable hide;
    private static float dn;

    /** A plain one-line note (replaces the system toast). Safe from any thread. */
    static void toast(Context ctx, String text) {
        boolean bad = text.contains("تعذّر") || text.contains("ما وصل") || text.contains("غير مثبّت") || text.contains("ما تمت");
        show(ctx, bad ? FAIL : DONE, R.drawable.ic_info, bad ? 0xFFFF8A8A : 0xFF8DB4FF, null, text, null, null);
    }

    /** Safe from any thread. {@code label}, {@code detail} and {@code big} may be null. */
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
        disc.setColor(withAlpha(accent, 0x2E));
        bg.setStroke(Math.round(1f * dn), withAlpha(accent, state == FAIL ? 0x88 : 0x38));
        tHead.setText(head);
        String sub = (label == null || label.length() == 0 ? "" : label)
                + (detail == null || detail.length() == 0 ? "" : (label == null || label.length() == 0 ? "" : " · ") + detail);
        tSub.setText(sub);
        tSub.setVisibility(sub.length() == 0 ? View.GONE : View.VISIBLE);
        tBig.setText(big == null ? "" : big);
        tBig.setTextColor(accent);
        tBig.setVisibility(big == null || big.length() == 0 ? View.GONE : View.VISIBLE);

        if (pulse != null) { pulse.cancel(); pulse = null; }
        icon.setAlpha(1f);
        if (state == WORKING) {                                              // a slow breathing icon while the request is out
            pulse = ObjectAnimator.ofFloat(icon, View.ALPHA, 1f, 0.4f);
            pulse.setDuration(750);
            pulse.setRepeatMode(ObjectAnimator.REVERSE);
            pulse.setRepeatCount(ObjectAnimator.INFINITE);
            pulse.start();
        } else {                                                             // a small pop when it lands
            icon.setScaleX(0.82f);
            icon.setScaleY(0.82f);
            icon.animate().scaleX(1f).scaleY(1f).setDuration(240).setInterpolator(new OvershootInterpolator(2f)).start();
        }
        if (hide != null) H.removeCallbacks(hide);
        hide = new Runnable() { public void run() { dismiss(); } };
        H.postDelayed(hide, state == WORKING ? 8000L : state == FAIL ? 4200L : 2300L);   // a stuck "working" card goes away too
    }

    private static int withAlpha(int color, int a) { return (a << 24) | (color & 0xFFFFFF); }

    private static void build(Context c) {
        dn = c.getResources().getDisplayMetrics().density;
        LinearLayout b = new LinearLayout(c);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);                     // icon on the right, text reads right to left
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setMinimumWidth(Math.round(230 * dn));
        bg = new GradientDrawable();
        bg.setColor(0xEB0E1424);
        bg.setCornerRadius(18 * dn);
        b.setBackground(bg);
        b.setPadding(Math.round(12 * dn), Math.round(10 * dn), Math.round(18 * dn), Math.round(10 * dn));
        b.setElevation(8 * dn);

        icon = new ImageView(c);
        int pad = Math.round(9 * dn);
        icon.setPadding(pad, pad, pad, pad);
        disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        icon.setBackground(disc);
        b.addView(icon, new LinearLayout.LayoutParams(Math.round(40 * dn), Math.round(40 * dn)));

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        tHead = label(c, 15, WHITE, true, 2);
        tSub = label(c, 12, MUTED, false, 1);
        col.addView(tHead);
        col.addView(tSub);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
        cp.setMargins(Math.round(14 * dn), 0, Math.round(10 * dn), 0);
        b.addView(col, cp);

        tBig = label(c, 22, WHITE, true, 1);
        b.addView(tBig);

        WindowManager w = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.RIGHT;                         // bottom corner: clear of the clock, network and settings
        lp.x = Math.round(22 * dn);
        lp.y = Safe.v(c) + Math.round(34 * dn);
        try {
            b.setAlpha(0f);
            b.setTranslationX(90 * dn);
            w.addView(b, lp);
            b.animate().alpha(1f).translationX(0f).setDuration(230).setInterpolator(new DecelerateInterpolator(1.6f)).start();
            box = b;
            wm = w;
        } catch (Throwable t) {
            box = null;
        }
    }

    private static TextView label(Context c, float sp, int color, boolean bold, int lines) {
        TextView t = new TextView(c);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) t.setTypeface(Fonts.bold);
        t.setMaxLines(lines);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setMaxWidth(Math.round(270 * dn));
        t.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        t.setGravity(Gravity.START);
        return t;
    }

    private static void dismiss() {
        final View v = box;
        final WindowManager w = wm;
        box = null;
        hide = null;
        if (pulse != null) { pulse.cancel(); pulse = null; }
        if (v == null || w == null) return;
        v.animate().alpha(0f).translationX(90 * dn).setDuration(220).setInterpolator(new DecelerateInterpolator(1.4f)).withEndAction(new Runnable() {
            public void run() { try { w.removeView(v); } catch (Throwable ignored) { } }
        }).start();
    }
}
