package com.albrhi.tvwidget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import static com.albrhi.tvwidget.WidgetView.*;

/** TV side of pairing: shows the address and a one-time PIN, and lists/clears paired devices. */
final class RemoteDialog {
    private RemoteDialog() { }

    static void show(final Context c) {
        final float dn = c.getResources().getDisplayMetrics().density;
        final RemoteServer srv = RemoteServer.get(c);
        srv.start();
        final String pin0 = srv.startPairing();
        final Handler h = new Handler(Looper.getMainLooper());
        final Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0B1226);
        bg.setCornerRadius(20 * dn);
        bg.setStroke(Math.round(dn), 0x33FFFFFF);
        box.setBackground(bg);
        box.setPadding(Math.round(26 * dn), Math.round(20 * dn), Math.round(26 * dn), Math.round(16 * dn));

        box.addView(text(c, "ربط الجوال والتثبيت عن بعد", 17, GOLD, true, Gravity.START));
        box.addView(text(c, "على جوالك (نفس الواي فاي) افتح هذا العنوان:", 13, MUTED, false, Gravity.START), lp(dn, 12));
        final TextView url = text(c, "http://" + RemoteServer.localIp() + ":" + RemoteServer.PORT, 24, WHITE, true, Gravity.CENTER);
        url.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        box.addView(url, lp(dn, 6));
        box.addView(text(c, "ثم اكتب هذا الرمز:", 13, MUTED, false, Gravity.START), lp(dn, 14));
        final TextView pinView = text(c, spaced(pin0), 44, GOLD, true, Gravity.CENTER);
        pinView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        box.addView(pinView, lp(dn, 4));
        final TextView info = text(c, "", 13, MUTED, false, Gravity.CENTER);
        box.addView(info, lp(dn, 8));
        final TextView status = text(c, "", 14, WHITE, false, Gravity.CENTER);
        box.addView(status, lp(dn, 10));

        LinearLayout buttons = new LinearLayout(c);
        buttons.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView close = button(c, "إغلاق", dn, false);
        TextView revoke = button(c, "فصل كل الأجهزة", dn, true);
        close.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { d.dismiss(); } });
        revoke.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                srv.revokeAll();
                srv.status = "تم فصل كل الأجهزة";
            }
        });
        buttons.addView(close, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -2, 1f);
        rp.setMarginStart(Math.round(10 * dn));
        buttons.addView(revoke, rp);
        box.addView(buttons, lp(dn, 16));

        final Runnable tick = new Runnable() {
            public void run() {
                String p = srv.currentPin();
                pinView.setText(p == null ? "—" : spaced(p));
                pinView.setTextColor(p == null ? MUTED : GOLD);
                int n = srv.pairedCount();
                info.setText(p == null ? "انتهى الرمز (أو تم استخدامه). أغلق وافتح هذه الشاشة لرمز جديد."
                        : "الرمز يصلح لجهاز واحد ولمدة 10 دقائق") ;
                status.setText("الأجهزة المرتبطة: " + n + (n > 0 ? "  ·  " + join(srv.pairedNames()) : "") + "\n" + srv.status);
                if (d.isShowing()) h.postDelayed(this, 1000);
            }
        };

        d.setContentView(box, new LinearLayout.LayoutParams(Math.round(560 * dn), -2));
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setDimAmount(0.7f);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            public void onDismiss(android.content.DialogInterface x) { srv.stopPairing(); h.removeCallbacksAndMessages(null); }
        });
        d.show();
        close.requestFocus();
        tick.run();
    }

    private static String spaced(String p) { return p.replaceAll("(?<=.)(?=.)", " "); }

    private static String join(String[] names) {
        StringBuilder b = new StringBuilder();
        for (String n : names) { if (b.length() > 0) b.append("، "); b.append(n.length() > 22 ? n.substring(0, 22) + "…" : n); }
        return b.toString();
    }

    private static LinearLayout.LayoutParams lp(float dn, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = Math.round(top * dn);
        return p;
    }

    private static TextView text(Context c, String s, float sp, int color, boolean bold, int gravity) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) t.setTypeface(Fonts.bold);
        t.setGravity(gravity);
        return t;
    }

    private static TextView button(Context c, String s, final float dn, final boolean danger) {
        final TextView b = text(c, s, 14, WHITE, true, Gravity.CENTER);
        b.setFocusable(true);
        b.setClickable(true);
        b.setPadding(0, Math.round(10 * dn), 0, Math.round(10 * dn));
        final GradientDrawable idle = new GradientDrawable();
        idle.setColor(0xFF121B38);
        idle.setCornerRadius(10 * dn);
        final GradientDrawable on = new GradientDrawable();
        on.setColor(danger ? 0xFF8E2A24 : BLUE);
        on.setCornerRadius(10 * dn);
        b.setBackground(idle);
        b.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean f) { b.setBackground(f ? on : idle); }
        });
        return b;
    }
}
