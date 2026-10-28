package com.albrhi.tvwidget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import static com.albrhi.tvwidget.WidgetView.*;

/** "Press the button on your remote now": waits for the next mappable key (20 seconds) and reports its code. */
final class LearnDialog {
    private LearnDialog() { }

    private static final int SECONDS = 20;

    static void show(final Context c, final KeyMap.Learn onKey, final Runnable onCancel) {
        final float dn = c.getResources().getDisplayMetrics().density;
        final Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final Handler h = new Handler(Looper.getMainLooper());

        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(0xF5, Color.red(Theme.base) + 10, Color.green(Theme.base) + 10, Color.blue(Theme.base) + 14));
        bg.setCornerRadius(26 * dn);
        bg.setStroke(Math.max(1, Math.round(dn)), 0x22FFFFFF);
        box.setBackground(bg);
        box.setPadding(Math.round(30 * dn), Math.round(26 * dn), Math.round(30 * dn), Math.round(22 * dn));

        TextView title = Sheet.text(c, "اضغط الزر في الريموت الآن", 22, WHITE, false);
        title.setTypeface(Fonts.medium);
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        box.addView(title, new LinearLayout.LayoutParams(-1, -2));
        final TextView sub = Sheet.text(c, "الزر اللي تبغى يفتح تطبيقك (مثل زر يوتيوب أو نتفلكس)", 14, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        sub.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = Math.round(8 * dn);
        box.addView(sub, sp);
        final TextView count = Sheet.text(c, String.valueOf(SECONDS), 40, Theme.accent, false);
        count.setTypeface(Fonts.medium);
        count.setGravity(Gravity.CENTER);
        count.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = Math.round(14 * dn);
        box.addView(count, cp);

        TextView cancel = new TextView(c);
        cancel.setText("إلغاء");
        cancel.setTextColor(WHITE);
        cancel.setTypeface(Fonts.bold);
        cancel.setTextSize(15);
        cancel.setGravity(Gravity.CENTER);
        cancel.setFocusable(true);
        cancel.setClickable(true);
        cancel.setPadding(Math.round(40 * dn), Math.round(10 * dn), Math.round(40 * dn), Math.round(10 * dn));
        final GradientDrawable idle = new GradientDrawable();
        idle.setColor(0x1AFFFFFF);
        idle.setCornerRadius(16 * dn);
        final GradientDrawable on = new GradientDrawable();
        on.setColor(0x2AFFFFFF);
        on.setCornerRadius(16 * dn);
        on.setStroke(Math.max(2, Math.round(2 * dn)), (Theme.accent & 0x00FFFFFF) | 0xCC000000);
        cancel.setBackground(idle);
        cancel.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean f) { v.setBackground(f ? on : idle); }
        });
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
        bp.topMargin = Math.round(18 * dn);
        box.addView(cancel, bp);

        final boolean[] done = {false};
        final KeyMap.Learn learn = new KeyMap.Learn() {
            @Override public void onKey(int code) {
                if (done[0]) return;
                done[0] = true;
                h.removeCallbacksAndMessages(null);
                d.dismiss();
                onKey.onKey(code);
            }
        };
        cancel.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { d.dismiss(); } });
        d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override public void onDismiss(android.content.DialogInterface x) {
                h.removeCallbacksAndMessages(null);
                if (KeyMap.learning == learn) KeyMap.learning = null;
                if (!done[0]) { done[0] = true; if (onCancel != null) onCancel.run(); }
            }
        });
        final int[] left = {SECONDS};
        h.postDelayed(new Runnable() {
            public void run() {
                if (done[0]) return;
                left[0]--;
                if (left[0] <= 0) {
                    Toast.makeText(c, "ما وصل أي زر. جرّب زر ثاني، أو تأكد أن الريموت متصل", Toast.LENGTH_LONG).show();
                    d.dismiss();
                    return;
                }
                count.setText(String.valueOf(left[0]));
                h.postDelayed(this, 1000);
            }
        }, 1000);

        d.setContentView(box, new LinearLayout.LayoutParams(Math.round(520 * dn), -2));
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setDimAmount(0.6f);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        KeyMap.learning = learn;
        d.show();
    }
}
