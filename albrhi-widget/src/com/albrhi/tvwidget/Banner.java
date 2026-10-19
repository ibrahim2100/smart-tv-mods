package com.albrhi.tvwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import static com.albrhi.tvwidget.WidgetView.*;

/** A match alert that floats over whatever is on the TV for a few seconds. */
final class Banner {
    private Banner() { }

    private static final Handler H = new Handler(Looper.getMainLooper());
    private static View current;
    private static WindowManager currentWm;

    static void show(final Context ctx, final String title, final String line1, final String line2,
                     final Bitmap left, final Bitmap right, final int seconds) {
        H.post(new Runnable() {
            public void run() { doShow(ctx.getApplicationContext(), title, line1, line2, left, right, seconds); }
        });
    }

    private static void doShow(Context c, String title, String line1, String line2, Bitmap left, Bitmap right, int seconds) {
        remove();
        try {
            RingtoneManager.getRingtone(c, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play();
        } catch (Throwable ignored) { }
        if (!Settings.canDrawOverlays(c)) {
            Toast.makeText(c, title + "\n" + line1, Toast.LENGTH_LONG).show();
            return;
        }
        float dn = c.getResources().getDisplayMetrics().density;

        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0B1226);
        bg.setCornerRadius(18 * dn);
        bg.setStroke(Math.round(2 * dn), GOLD);
        box.setBackground(bg);
        box.setPadding(Math.round(18 * dn), Math.round(12 * dn), Math.round(18 * dn), Math.round(12 * dn));
        box.setElevation(12 * dn);

        box.addView(badge(c, left), new LinearLayout.LayoutParams(Math.round(46 * dn), Math.round(46 * dn)));
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(text(c, title, 14, GOLD, true));
        col.addView(text(c, line1, 18, WHITE, true));
        if (line2 != null && line2.length() > 0) col.addView(text(c, line2, 12, MUTED, false));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
        cp.setMargins(Math.round(16 * dn), 0, Math.round(16 * dn), 0);
        box.addView(col, cp);
        box.addView(badge(c, right), new LinearLayout.LayoutParams(Math.round(46 * dn), Math.round(46 * dn)));

        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = Math.round(36 * dn);
        try {
            box.setAlpha(0f);
            wm.addView(box, lp);
            box.animate().alpha(1f).setDuration(300).start();
            current = box;
            currentWm = wm;
            H.postDelayed(new Runnable() { public void run() { remove(); } }, seconds * 1000L);
        } catch (Throwable ignored) { }
    }

    private static ImageView badge(Context c, Bitmap b) {
        ImageView v = new ImageView(c);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (b != null) v.setImageBitmap(b);
        return v;
    }

    private static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private static void remove() {
        final View v = current;
        final WindowManager wm = currentWm;
        current = null;
        if (v == null || wm == null) return;
        v.animate().alpha(0f).setDuration(300).withEndAction(new Runnable() {
            public void run() { try { wm.removeView(v); } catch (Throwable ignored) { } }
        }).start();
    }
}
