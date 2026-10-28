package com.albrhi.tvwidget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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

import java.util.ArrayList;
import java.util.List;

import static com.albrhi.tvwidget.WidgetView.*;

/**
 * The look of every popup in the launcher: a dark glass panel that slides in from the edge,
 * a header with an icon badge, and large rows with a line icon, a title, a one-line hint and a chevron.
 * The focused row lights up in Blaugrana blue and grows slightly.
 */
final class Sheet {
    private Sheet() { }

    /** Set by an action that immediately opens another sheet, so "closed" is not reported in between. */
    static volatile boolean chained;

    static final int FOCUS_A = 0xFF0B5CC4, FOCUS_B = 0xFF1E88E5, DANGER_A = 0xFF9E2A24, DANGER_B = 0xFFE5484D;

    interface IntPick { void pick(int i); }

    static final class Item {
        final String title;
        String subtitle, value;
        int icon;
        Runnable action;
        boolean danger, header, tile, swatches;
        Boolean toggle;                    // non-null: the row shows a switch in this state
        IntPick pick;                      // swatches: called with the chosen index
        int selected;

        private Item(String title) { this.title = title; }

        static Item of(String title, int icon, Runnable action) {
            Item i = new Item(title);
            i.icon = icon;
            i.action = action;
            return i;
        }

        static Item header(String title) {
            Item i = new Item(title);
            i.header = true;
            return i;
        }

        /** A square quick tile (icon, name, short value); consecutive tiles form a grid in the Panel. */
        Item tile() { tile = true; return this; }

        /** Shows an on/off switch at the end of the row instead of a value. */
        Item toggle(boolean on) { toggle = on; return this; }

        /** A row of colour choices (used for the theme). */
        static Item swatches(String title, int selected, IntPick pick) {
            Item i = new Item(title);
            i.swatches = true;
            i.selected = selected;
            i.pick = pick;
            return i;
        }

        Item sub(String s) { subtitle = s; return this; }

        /** A short live value shown at the far end of the row (e.g. the Wi-Fi name). */
        Item value(String s) { value = s; return this; }

        Item danger() { danger = true; return this; }
    }

    // ---- shared look ------------------------------------------------------------------------

    static GradientDrawable panelBackground(float dn) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF111B3B, 0xFF070B1B});
        g.setCornerRadius(28 * dn);
        g.setStroke(Math.max(1, Math.round(dn)), 0x33FFFFFF);
        return g;
    }

    /** Slides a panel in from the right edge with a fade (the app runs right-to-left). */
    static void animateIn(View v, float dn) {
        v.setAlpha(0f);
        v.setTranslationX(48 * dn);
        v.animate().alpha(1f).translationX(0f).setDuration(240).setInterpolator(new DecelerateInterpolator(1.6f)).start();
    }

    static void tint(ImageView v, int color) { v.setColorFilter(color, PorterDuff.Mode.SRC_IN); }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) t.setTypeface(Fonts.bold);
        t.setIncludeFontPadding(false);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);                // Latin names (Apple TV) still line up on the right edge
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        return t;
    }

    private static GradientDrawable round(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(r);
        return g;
    }

    /** A round badge: Blaugrana gradient with a white line icon (or the app's own icon). */
    static View badge(Context c, int iconRes, Drawable custom, int sizeDp, float dn) {
        FrameLayout f = new FrameLayout(c);
        int px = Math.round(sizeDp * dn);
        if (custom != null) {
            ImageView iv = new ImageView(c);
            iv.setImageDrawable(custom);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable g = round(0xFF151D38, 12 * dn);
            f.setBackground(g);
            f.setClipToOutline(true);
            f.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override public void getOutline(View v, android.graphics.Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), v.getHeight() / 4f); }
            });
            f.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{GARNET, BLUE});
            g.setShape(GradientDrawable.OVAL);
            f.setBackground(g);
            ImageView iv = new ImageView(c);
            iv.setImageResource(iconRes);
            tint(iv, WHITE);
            int pad = Math.round(sizeDp * 0.24f * dn);
            iv.setPadding(pad, pad, pad, pad);
            f.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        }
        // an app's own banner is 16:9, so it gets a wide badge instead of being cropped to a square
        f.setLayoutParams(new LinearLayout.LayoutParams(custom != null ? Math.round(px * 1.78f) : px, px));
        return f;
    }

    // ---- the sheet --------------------------------------------------------------------------

    static void show(Context c, String title, String subtitle, int headerIcon, List<Item> items, View footer, Runnable onClose) {
        show(c, title, subtitle, headerIcon, null, items, footer, onClose, 380);
    }

    static void show(final Context c, String title, String subtitle, int headerIcon, Drawable headerDrawable,
                     List<Item> items, View footer, final Runnable onClose, int widthDp) {
        final float dn = c.getResources().getDisplayMetrics().density;
        final Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final LinearLayout panel = new LinearLayout(c);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        panel.setBackground(panelBackground(dn));
        panel.setElevation(24 * dn);
        panel.setPadding(Math.round(18 * dn), Math.round(14 * dn), Math.round(18 * dn), Math.round(10 * dn));

        // header
        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(badge(c, headerIcon, headerDrawable, 42, dn));
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(c, title, 22, WHITE, true);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(t);
        if (subtitle != null && subtitle.length() > 0) {
            TextView s = text(c, subtitle, 13, MUTED, false);
            s.setSingleLine(true);
            s.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
            sp.topMargin = Math.round(3 * dn);
            col.addView(s, sp);
        }
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
        cp.setMarginStart(Math.round(14 * dn));
        head.addView(col, cp);
        panel.addView(head, new LinearLayout.LayoutParams(-1, -2));

        View line = new View(c);
        line.setBackground(new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, new int[]{GARNET, BLUE}));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Math.max(2, Math.round(2 * dn)));
        lp.topMargin = Math.round(10 * dn);
        lp.bottomMargin = Math.round(6 * dn);
        panel.addView(line, lp);

        // rows (scroll when there are many)
        final LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        View first = null;
        for (final Item it : items) {
            if (it.header) {
                TextView h = text(c, it.title, 12.5f, MUTED, true);
                LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-2, -2);
                hp.topMargin = Math.round(12 * dn);
                hp.bottomMargin = Math.round(4 * dn);
                hp.setMarginStart(Math.round(6 * dn));
                list.addView(h, hp);
                continue;
            }
            View row = row(c, it, dn, d);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.topMargin = Math.round(3 * dn);
            list.addView(row, rp);
            if (first == null) first = row;
        }
        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(0, Math.round(2 * dn), 0, Math.round(2 * dn));
        sv.addView(list);
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        int maxList = dm.heightPixels - Math.round((footer == null ? 130 : 196) * dn);
        panel.addView(new Capped(c, sv, maxList), new LinearLayout.LayoutParams(-1, -2));

        if (footer != null) {
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
            fp.topMargin = Math.round(10 * dn);
            panel.addView(footer, fp);
        }
        FrameLayout root = new FrameLayout(c);
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(Math.round(widthDp * dn), -2, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        pl.setMargins(0, Math.round(14 * dn), Math.round(28 * dn), Math.round(14 * dn));
        root.addView(panel, pl);
        d.setContentView(root);

        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setDimAmount(0.55f);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface x) {
                if (chained) { chained = false; return; }
                if (onClose != null) onClose.run();
            }
        });
        d.show();
        animateIn(panel, dn);
        if (first != null) first.requestFocus();
    }

    private static View row(final Context c, final Item it, final float dn, final Dialog d) {
        final LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(true);
        r.setClickable(true);
        r.setMinimumHeight(Math.round(46 * dn));
        r.setPadding(Math.round(10 * dn), Math.round(3 * dn), Math.round(14 * dn), Math.round(3 * dn));
        final int accentA = it.danger ? DANGER_A : FOCUS_A, accentB = it.danger ? DANGER_B : FOCUS_B;
        final GradientDrawable idle = round(0x14FFFFFF, 18 * dn);
        final GradientDrawable on = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, new int[]{accentA, accentB});
        on.setCornerRadius(18 * dn);
        r.setBackground(idle);

        final FrameLayout circle = new FrameLayout(c);
        final GradientDrawable cIdle = round(0x1FFFFFFF, 14 * dn);
        final GradientDrawable cOn = round(0x33FFFFFF, 14 * dn);
        circle.setBackground(cIdle);
        final ImageView icon = new ImageView(c);
        icon.setImageResource(it.icon);
        final int idleTint = it.danger ? 0xFFFF8A80 : 0xFFDCE6FF;
        tint(icon, idleTint);
        int pad = Math.round(9 * dn);
        icon.setPadding(pad, pad, pad, pad);
        circle.addView(icon, new FrameLayout.LayoutParams(-1, -1));
        r.addView(circle, new LinearLayout.LayoutParams(Math.round(36 * dn), Math.round(36 * dn)));

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        final TextView title = text(c, it.title, 16.5f, it.danger ? 0xFFFFB4AB : WHITE, true);
        col.addView(title);
        final TextView sub = it.subtitle == null ? null : text(c, it.subtitle, 12.5f, MUTED, false);
        if (sub != null) {
            sub.setSingleLine(true);
            sub.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
            sp.topMargin = Math.round(2 * dn);
            col.addView(sub, sp);
        }
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
        cp.setMarginStart(Math.round(13 * dn));
        r.addView(col, cp);

        final ImageView chev = new ImageView(c);
        chev.setImageResource(R.drawable.ic_back);
        tint(chev, 0x66FFFFFF);
        r.addView(chev, new LinearLayout.LayoutParams(Math.round(20 * dn), Math.round(20 * dn)));

        r.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean f) {
                r.setBackground(f ? on : idle);
                circle.setBackground(f ? cOn : cIdle);
                tint(icon, f ? WHITE : idleTint);
                tint(chev, f ? WHITE : 0x66FFFFFF);
                title.setTextColor(f ? WHITE : (it.danger ? 0xFFFFB4AB : WHITE));
                if (sub != null) sub.setTextColor(f ? 0xCCFFFFFF : MUTED);
                r.animate().scaleX(f ? 1.025f : 1f).scaleY(f ? 1.025f : 1f).setDuration(130).start();
                r.setElevation(f ? 8 * dn : 0);
            }
        });
        r.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (it.action != null) it.action.run();     // first, so an action can open the next sheet before this one closes
                d.dismiss();
            }
        });
        return r;
    }

    /** Holds the scrolling list and caps its height; the list is measured at its natural size first. */
    static final class Capped extends LinearLayout {
        private final int max;
        private final ScrollView sv;

        Capped(Context c, ScrollView sv, int max) {
            super(c);
            this.max = max;
            this.sv = sv;
            setOrientation(VERTICAL);
            addView(sv, new LinearLayout.LayoutParams(-1, -2));
        }

        @Override protected void onMeasure(int w, int h) {
            View child = sv.getChildAt(0);
            child.measure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(w), MeasureSpec.EXACTLY), MeasureSpec.UNSPECIFIED);
            int want = Math.min(max, child.getMeasuredHeight() + sv.getPaddingTop() + sv.getPaddingBottom());
            super.onMeasure(w, MeasureSpec.makeMeasureSpec(want, MeasureSpec.EXACTLY));
            sv.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(want, MeasureSpec.EXACTLY));
        }
    }

    // ---- small meter used in footers --------------------------------------------------------

    /** "Label ........ value" with a thin bar under it; used = how full it is (0-100). */
    static View meter(Context c, String label, String value, int used, final float dn) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        LinearLayout top = new LinearLayout(c);
        TextView l = text(c, label, 12.5f, MUTED, true);
        TextView v = text(c, value, 12.5f, WHITE, false);
        v.setGravity(Gravity.END);
        top.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));
        top.addView(v, new LinearLayout.LayoutParams(-2, -2));
        box.addView(top, new LinearLayout.LayoutParams(-1, -2));

        final int pct = Math.max(2, Math.min(100, used));
        final int color = used >= 90 ? 0xFFFF453A : used >= 75 ? 0xFFFFB020 : 0xFF34C759;
        View bar = new View(c) {
            private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(android.graphics.Canvas cv) {
                float w = getWidth(), h = getHeight(), r = h / 2f;
                p.setColor(0x22FFFFFF);
                cv.drawRoundRect(0, 0, w, h, r, r, p);
                p.setColor(color);
                float fw = Math.max(h, w * pct / 100f);
                cv.drawRoundRect(0, 0, fw, h, r, r, p);
            }
        };
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, Math.round(6 * dn));
        tp.topMargin = Math.round(5 * dn);
        box.addView(bar, tp);
        return box;
    }

    static List<Item> list() { return new ArrayList<Item>(); }
}
