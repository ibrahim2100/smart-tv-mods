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
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import static com.albrhi.tvwidget.WidgetView.*;

/** Type a new name for an app tile (an empty name or "original" puts the real name back). */
final class RenameDialog {
    private RenameDialog() { }

    interface Callback { void onName(String nameOrNullForOriginal); }

    static void show(Context c, String current, String original, final Callback cb) {
        final float dn = c.getResources().getDisplayMetrics().density;
        final Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0B1226);
        bg.setCornerRadius(18 * dn);
        bg.setStroke(Math.round(dn), 0x33FFFFFF);
        box.setBackground(bg);
        box.setPadding(Math.round(20 * dn), Math.round(16 * dn), Math.round(20 * dn), Math.round(14 * dn));

        TextView t = new TextView(c);
        t.setText("اسم جديد للتطبيق");
        t.setTextColor(GOLD);
        t.setTypeface(Fonts.bold);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        box.addView(t);

        final EditText in = new EditText(c);
        in.setText(current);
        in.setSelectAllOnFocus(true);
        in.setSingleLine(true);
        in.setHint(original);
        in.setHintTextColor(MUTED);
        in.setTextColor(WHITE);
        in.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        in.setImeOptions(EditorInfo.IME_ACTION_DONE);
        GradientDrawable ib = new GradientDrawable();
        ib.setColor(0xFF121B38);
        ib.setCornerRadius(10 * dn);
        in.setBackground(ib);
        in.setPadding(Math.round(14 * dn), Math.round(10 * dn), Math.round(14 * dn), Math.round(10 * dn));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
        ip.topMargin = Math.round(10 * dn);
        box.addView(in, ip);

        LinearLayout row = new LinearLayout(c);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        TextView save = button(c, "حفظ", dn, BLUE);
        TextView orig = button(c, "الاسم الأصلي", dn, 0xFF2A3358);
        TextView cancel = button(c, "إلغاء", dn, 0xFF2A3358);
        row.addView(save, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams m1 = new LinearLayout.LayoutParams(0, -2, 1.3f);
        m1.setMarginStart(Math.round(8 * dn));
        row.addView(orig, m1);
        LinearLayout.LayoutParams m2 = new LinearLayout.LayoutParams(0, -2, 1f);
        m2.setMarginStart(Math.round(8 * dn));
        row.addView(cancel, m2);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = Math.round(12 * dn);
        box.addView(row, rp);

        final Runnable doSave = new Runnable() { public void run() { d.dismiss(); cb.onName(in.getText().toString()); } };
        save.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { doSave.run(); } });
        orig.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { d.dismiss(); cb.onName(null); } });
        cancel.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { d.dismiss(); } });
        in.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int id, android.view.KeyEvent e) { doSave.run(); return true; }
        });

        d.setContentView(box, new LinearLayout.LayoutParams(Math.round(460 * dn), -2));
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setDimAmount(0.65f);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);   // keyboard opens when you press OK on the field
        }
        d.show();
        in.requestFocus();
    }

    private static TextView button(Context c, String s, final float dn, final int color) {
        final TextView b = new TextView(c);
        b.setText(s);
        b.setTextColor(WHITE);
        b.setTypeface(Fonts.bold);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setClickable(true);
        b.setPadding(0, Math.round(10 * dn), 0, Math.round(10 * dn));
        final GradientDrawable idle = new GradientDrawable();
        idle.setColor(color);
        idle.setCornerRadius(10 * dn);
        final GradientDrawable on = new GradientDrawable();
        on.setColor(color);
        on.setCornerRadius(10 * dn);
        on.setStroke(Math.round(2.5f * dn), 0xFFFFFFFF);
        b.setBackground(idle);
        b.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean f) { b.setBackground(f ? on : idle); }
        });
        return b;
    }
}
