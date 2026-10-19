package com.albrhi.tvwidget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

import static com.albrhi.tvwidget.WidgetView.*;

/**
 * Glass drop-down menu, the same look as the app context menu: it opens right under the button that
 * was pressed (or at the top-right corner when there is no button, e.g. from the remote's gear key),
 * the focused row becomes a white pill, and everything behind it is dimmed.
 */
final class Dropdown {
    private Dropdown() { }

    static void show(Context c, View anchor, String title, String subtitle, List<Sheet.Item> items, View footer, int widthDp,
                     Runnable onClose) {
        show(c, anchor, title, subtitle, items, footer, null, widthDp, onClose);
    }

    /** @param custom replaces the list of rows with your own content (used by the matches sheet). */
    static void show(final Context c, View anchor, String title, String subtitle, List<Sheet.Item> items, View footer,
                     View custom, int widthDp, final Runnable onClose) {
        final float dn = c.getResources().getDisplayMetrics().density;
        final DisplayMetrics dm = c.getResources().getDisplayMetrics();
        final Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final LinearLayout panel = new LinearLayout(c);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        GradientDrawable pb = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFA1D2742, 0xFA121A2E});
        pb.setCornerRadius(26 * dn);
        pb.setStroke(Math.max(1, Math.round(dn)), 0x33FFFFFF);
        panel.setBackground(pb);
        panel.setElevation(30 * dn);
        panel.setPadding(Math.round(10 * dn), Math.round(10 * dn), Math.round(10 * dn), Math.round(10 * dn));

        if (title != null) {
            LinearLayout head = new LinearLayout(c);
            head.setOrientation(LinearLayout.VERTICAL);
            head.setPadding(Math.round(14 * dn), Math.round(6 * dn), Math.round(14 * dn), Math.round(8 * dn));
            TextView t = Sheet.text(c, title, 18, WHITE, true);
            head.addView(t);
            if (subtitle != null && subtitle.length() > 0) {
                TextView s = Sheet.text(c, subtitle, 12.5f, MUTED, false);
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
                sp.topMargin = Math.round(2 * dn);
                head.addView(s, sp);
            }
            panel.addView(head, new LinearLayout.LayoutParams(-1, -2));
            View line = new View(c);
            line.setBackgroundColor(0x22FFFFFF);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Math.max(1, Math.round(dn)));
            lp.setMargins(Math.round(14 * dn), 0, Math.round(14 * dn), Math.round(4 * dn));
            panel.addView(line, lp);
        }

        View first = null;
        final LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        if (custom == null) {
            for (final Sheet.Item it : items) {
                if (it.header) {
                    TextView h = Sheet.text(c, it.title, 12.5f, MUTED, true);
                    h.setPadding(Math.round(14 * dn), Math.round(10 * dn), 0, Math.round(2 * dn));
                    list.addView(h, new LinearLayout.LayoutParams(-2, -2));
                    continue;
                }
                View row = row(c, it, dn, d);
                list.addView(row, new LinearLayout.LayoutParams(-1, -2));
                if (first == null) first = row;
            }
        }
        // where does it open?
        final int w = Math.round(widthDp * dn);
        int x, y;
        int[] loc = new int[2];
        boolean fromButton = anchor != null && anchor.isAttachedToWindow();
        if (fromButton) {
            anchor.getLocationOnScreen(loc);
            int cx = loc[0] + anchor.getWidth() / 2;
            x = Math.max(Math.round(20 * dn), Math.min(dm.widthPixels - w - Math.round(20 * dn), cx - w / 2));
            y = loc[1] + anchor.getHeight() + Math.round(10 * dn);
        } else {
            x = dm.widthPixels - w - Math.round(40 * dn);
            y = Math.round(36 * dn);
        }

        // what is left under the menu's top edge, minus the header, footer and padding
        int reserve = Math.round(((title != null ? 64 : 12) + (footer != null ? 44 : 0) + 20) * dn);
        int maxList = dm.heightPixels - y - reserve;
        View body = custom != null ? custom : list;
        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.addView(body);
        panel.addView(new Sheet.Capped(c, sv, maxList), new LinearLayout.LayoutParams(-1, -2));

        if (footer != null) {
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
            fp.setMargins(Math.round(14 * dn), Math.round(10 * dn), Math.round(14 * dn), Math.round(6 * dn));
            panel.addView(footer, fp);
        }

        FrameLayout root = new FrameLayout(c);
        root.addView(panel, new FrameLayout.LayoutParams(w, -2));
        d.setContentView(root);
        Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setDimAmount(0.6f);
            win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = win.getAttributes();
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            lp.x = x;
            lp.y = y;
            lp.width = w;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            win.setAttributes(lp);
        }
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface x2) {
                if (Sheet.chained) { Sheet.chained = false; return; }
                if (onClose != null) onClose.run();
            }
        });
        d.show();

        // drops down from the button
        panel.setPivotX(w / 2f);
        panel.setPivotY(0f);
        panel.setAlpha(0f);
        panel.setScaleX(0.88f);
        panel.setScaleY(0.88f);
        panel.setTranslationY(-Math.round(10 * dn));
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setDuration(240)
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();
        for (int i = 0; i < list.getChildCount(); i++) {
            View ch = list.getChildAt(i);
            ch.setAlpha(0f);
            ch.animate().alpha(1f).setStartDelay(50L + 30L * i).setDuration(180).start();
        }
        if (first != null) first.requestFocus();
    }

    private static View row(final Context c, final Sheet.Item it, final float dn, final Dialog d) {
        final LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(true);
        r.setClickable(true);
        r.setMinimumHeight(Math.round(42 * dn));
        r.setPadding(Math.round(14 * dn), Math.round(2 * dn), Math.round(14 * dn), Math.round(2 * dn));
        final GradientDrawable idle = new GradientDrawable();
        idle.setColor(0);
        idle.setCornerRadius(17 * dn);
        final GradientDrawable on = new GradientDrawable();
        on.setColor(it.danger ? 0xFFFF453A : 0xFAFFFFFF);
        on.setCornerRadius(17 * dn);
        r.setBackground(idle);

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        final int idleText = it.danger ? 0xFFFF6B60 : WHITE;
        final TextView title = Sheet.text(c, it.title, 17, idleText, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(title);
        final TextView sub = it.subtitle == null ? null : Sheet.text(c, it.subtitle, 12.5f, MUTED, false);
        if (sub != null) {
            sub.setSingleLine(true);
            sub.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
            sp.topMargin = Math.round(1 * dn);
            col.addView(sub, sp);
        }
        r.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));

        final TextView val = it.value == null ? null : Sheet.text(c, it.value, 13.5f, MUTED, false);
        if (val != null) {
            val.setSingleLine(true);
            val.setEllipsize(TextUtils.TruncateAt.END);
            val.setMaxWidth(Math.round(190 * dn));
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-2, -2);
            vp.setMarginStart(Math.round(10 * dn));
            r.addView(val, vp);
        }

        final ImageView ic = new ImageView(c);
        ic.setImageResource(it.icon);
        final int idleTint = it.danger ? 0xFFFF6B60 : 0xCCFFFFFF;
        Sheet.tint(ic, idleTint);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(Math.round(24 * dn), Math.round(24 * dn));
        ip.setMarginStart(Math.round(12 * dn));
        r.addView(ic, ip);

        r.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean f) {
                r.setBackground(f ? on : idle);
                int fg = it.danger ? 0xFFFFFFFF : 0xFF0B1226;
                title.setTextColor(f ? fg : idleText);
                if (sub != null) sub.setTextColor(f ? (it.danger ? 0xDDFFFFFF : 0xFF4A5675) : MUTED);
                if (val != null) val.setTextColor(f ? (it.danger ? 0xDDFFFFFF : 0xFF4A5675) : MUTED);
                Sheet.tint(ic, f ? fg : idleTint);
                r.animate().scaleX(f ? 1.02f : 1f).scaleY(f ? 1.02f : 1f).setDuration(120).start();
            }
        });
        r.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (it.action != null) it.action.run();
                d.dismiss();
            }
        });
        return r;
    }
}
