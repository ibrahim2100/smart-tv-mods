package com.albrhi.tvwidget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import static com.albrhi.tvwidget.WidgetView.*;

/** Small remote-friendly popup: a title and a list of focusable choices. */
final class Menu {
    private Menu() { }

    /** Marks a label as a destructive choice (shown in red). */
    static final String DANGER = "⁣";

    static void show(Context c, String title, String[] labels, final Runnable[] actions) { show(c, title, labels, actions, 300); }

    static void show(Context c, String title, String[] labels, final Runnable[] actions, int widthDp) {
        final float dn = c.getResources().getDisplayMetrics().density;
        final Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0B1226);
        bg.setCornerRadius(18 * dn);
        bg.setStroke(Math.round(dn), 0x33FFFFFF);
        box.setBackground(bg);
        box.setPadding(Math.round(18 * dn), Math.round(16 * dn), Math.round(18 * dn), Math.round(12 * dn));

        TextView t = new TextView(c);
        t.setText(title);
        t.setTextColor(GOLD);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t.setSingleLine(true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.bottomMargin = Math.round(10 * dn);
        box.addView(t, tp);

        final LinearLayout list = new LinearLayout(c);     // the choices scroll if there are many
        list.setOrientation(LinearLayout.VERTICAL);
        View first = null;
        for (int i = 0; i < labels.length; i++) {
            final Runnable action = actions[i];
            boolean danger = labels[i].startsWith(DANGER);
            final TextView b = new TextView(c);
            b.setText(danger ? labels[i].substring(DANGER.length()) : labels[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            b.setTextColor(danger ? 0xFFFF6B60 : WHITE);
            b.setGravity(Gravity.CENTER_VERTICAL);
            b.setFocusable(true);
            b.setClickable(true);
            b.setPadding(Math.round(14 * dn), Math.round(8 * dn), Math.round(14 * dn), Math.round(8 * dn));
            final GradientDrawable idle = new GradientDrawable();
            idle.setColor(0xFF121B38);
            idle.setCornerRadius(10 * dn);
            final GradientDrawable on = new GradientDrawable();
            on.setColor(danger ? 0xFF8E2A24 : BLUE);
            on.setCornerRadius(10 * dn);
            b.setBackground(idle);
            b.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean f) { b.setBackground(f ? on : idle); }
            });
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    d.dismiss();
                    if (action != null) action.run();
                }
            });
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
            bp.topMargin = Math.round(5 * dn);
            list.addView(b, bp);
            if (first == null) first = b;
        }
        android.widget.ScrollView sv = new android.widget.ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(list);
        box.addView(sv, new LinearLayout.LayoutParams(-1, -2));

        d.setContentView(box, new LinearLayout.LayoutParams(Math.round(widthDp * dn), -2));
        d.getWindow().setLayout(Math.round(widthDp * dn), WindowManager.LayoutParams.WRAP_CONTENT);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setDimAmount(0.6f);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        d.show();
        if (first != null) first.requestFocus();
    }
}
