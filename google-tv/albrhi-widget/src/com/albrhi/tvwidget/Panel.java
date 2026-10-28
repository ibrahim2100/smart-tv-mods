package com.albrhi.tvwidget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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
 * The quick-settings panel: a calm, dark side panel on the right edge.
 * A grid of square tiles for the things you reach for most, quiet list rows under it, real on/off switches,
 * and a colour picker. The focused item gets a soft light fill and a thin line in the theme's accent colour
 * (no bright blocks, no jumping sizes).
 */
final class Panel {
    private Panel() { }

    private static final int COLS = 3;
    private static Runnable current;          // redraws the panel that is on screen (used when a dialog opened from it finishes)

    /** Redraws the open panel from its builder (e.g. after a name was typed in a dialog). */
    static void refresh() { if (current != null) current.run(); }

    /** Produces the rows each time the panel draws itself (so switches and values always show the current state). */
    interface Builder { List<Sheet.Item> items(); }

    static void show(final Context c, String title, String subtitle, final Builder builder, View footer, final Runnable onClose) {
        final float dn = c.getResources().getDisplayMetrics().density;
        final DisplayMetrics dm = c.getResources().getDisplayMetrics();
        final Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final LinearLayout panel = new LinearLayout(c);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        panel.setElevation(28 * dn);
        panel.setPadding(Math.round(22 * dn), Math.round(20 * dn), Math.round(22 * dn), Math.round(16 * dn));

        TextView t = Sheet.text(c, title, 22, WHITE, false);
        t.setTypeface(Fonts.medium);
        t.setSingleLine(true);
        panel.addView(t, new LinearLayout.LayoutParams(-2, -2));
        if (subtitle != null && subtitle.length() > 0) {
            TextView st = Sheet.text(c, subtitle, 13, MUTED, false);
            st.setSingleLine(true);
            st.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
            sp.topMargin = Math.round(4 * dn);
            panel.addView(st, sp);
        }

        final LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        int w = Math.round(430 * dn);
        int topMargin = Safe.v(c) + Math.round(6 * dn);
        int reserve = Math.round((36 + 34 + (subtitle != null ? 22 : 0) + (footer != null ? 58 : 0) + 10) * dn);
        final int maxList = dm.heightPixels - topMargin * 2 - reserve;
        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(0, 0, 0, Math.round(2 * dn));
        sv.addView(list);
        panel.addView(new Sheet.Capped(c, sv, maxList), new LinearLayout.LayoutParams(-1, -2));
        if (footer != null) {
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
            fp.topMargin = Math.round(14 * dn);
            panel.addView(footer, fp);
        }

        // (re)draws every row from the builder and puts the focus back on the same row
        final Runnable[] render = new Runnable[1];
        final int[] focusAt = {0};
        render[0] = new Runnable() {
            public void run() {
                GradientDrawable pb = new GradientDrawable();
                pb.setColor(blend(Theme.base, 0xFFFFFFFF, 0.06f) & 0x00FFFFFF | 0xF5000000);
                pb.setCornerRadius(26 * dn);
                pb.setStroke(Math.max(1, Math.round(dn)), 0x14FFFFFF);
                panel.setBackground(pb);
                list.removeAllViews();
                LinearLayout gridRow = null;
                int inRow = 0, n = 0;
                View focusView = null, firstView = null;
                final List<Sheet.Item> all = builder.items();
                final int tileH = tileHeight(all, maxList / dn);
                for (final Sheet.Item it : all) {
                    if (it.tile) {
                        if (gridRow == null || inRow == COLS) {
                            gridRow = new LinearLayout(c);
                            gridRow.setOrientation(LinearLayout.HORIZONTAL);
                            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2);
                            gp.topMargin = Math.round(8 * dn);
                            list.addView(gridRow, gp);
                            inRow = 0;
                        }
                        View tv = tile(c, it, dn, d, render[0], tileH < 90);
                        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, Math.round(tileH * dn), 1f);
                        tp.setMargins(inRow == 0 ? 0 : Math.round(4 * dn), 0, inRow == COLS - 1 ? 0 : Math.round(4 * dn), 0);
                        gridRow.addView(tv, tp);
                        inRow++;
                        if (firstView == null) firstView = tv;
                        if (n == focusAt[0]) focusView = tv;
                        tv.setTag(Integer.valueOf(n++));
                        continue;
                    }
                    boolean afterGrid = gridRow != null;
                    gridRow = null;
                    if (it.customView != null) {
                        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                        cp.topMargin = Math.round(8 * dn);
                        list.addView(it.customView, cp);
                        continue;
                    }
                    if (it.header) {
                        TextView h = Sheet.text(c, it.title, 12.5f, 0xFF7F8AA3, true);
                        h.setPadding(Math.round(4 * dn), Math.round(18 * dn), 0, Math.round(4 * dn));
                        list.addView(h, new LinearLayout.LayoutParams(-2, -2));
                        continue;
                    }
                    View v = it.swatches ? swatches(c, it, dn, d, render[0]) : row(c, it, dn, d, render[0]);
                    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
                    rp.topMargin = Math.round((afterGrid ? 16 : 5) * dn);
                    list.addView(v, rp);
                    if (firstView == null) firstView = v;
                    if (n == focusAt[0]) focusView = v;
                    v.setTag(Integer.valueOf(n++));
                }
                View target = focusView != null ? focusView : firstView;
                if (target != null) target.requestFocus();
            }
        };
        // remember which row is focused, so a redraw keeps your place
        list.getViewTreeObserver().addOnGlobalFocusChangeListener(new android.view.ViewTreeObserver.OnGlobalFocusChangeListener() {
            @Override public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                View v = newFocus;
                while (v != null && v != list) {
                    if (v.getTag() instanceof Integer) { focusAt[0] = (Integer) v.getTag(); return; }
                    v = v.getParent() instanceof View ? (View) v.getParent() : null;
                }
            }
        });
        render[0].run();
        current = render[0];

        FrameLayout root = new FrameLayout(c);
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(w, -2, Gravity.RIGHT | Gravity.TOP);
        pl.setMargins(0, topMargin, Safe.h(c) - Math.round(18 * dn), topMargin);
        root.addView(panel, pl);
        d.setContentView(root);
        Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setDimAmount(0.45f);
            win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            win.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface x) {
                if (current == render[0]) current = null;
                if (Sheet.chained) { Sheet.chained = false; return; }
                if (onClose != null) onClose.run();
            }
        });
        d.show();
        panel.setAlpha(0f);
        panel.setTranslationX(36 * dn);
        panel.animate().alpha(1f).translationX(0f).setDuration(220).setInterpolator(new DecelerateInterpolator(1.6f)).start();
    }

    /** Tile height in dp: 98 when there is room, down to 76 so the page fits without scrolling when the screen margins are large. */
    private static int tileHeight(List<Sheet.Item> items, float availDp) {
        int tiles = 0;
        float other = 0;
        boolean after = false;
        for (Sheet.Item it : items) {
            if (it.tile) { tiles++; continue; }
            if (tiles > 0) after = true;
            other += it.customView != null ? 110 : it.header ? 34 : it.swatches ? 62 : it.subtitle != null ? 56 : 51;
        }
        if (tiles == 0) return 98;
        int rows = (tiles + COLS - 1) / COLS;
        float fit = (availDp - other - (after ? 16 : 0)) / rows - 8;
        return Math.round(Math.max(76, Math.min(98, fit)));
    }

    private static int blend(int a, int b, float k) {
        int r = Math.round(Color.red(a) * (1 - k) + Color.red(b) * k), g = Math.round(Color.green(a) * (1 - k) + Color.green(b) * k),
                bl = Math.round(Color.blue(a) * (1 - k) + Color.blue(b) * k);
        return Color.rgb(r, g, bl);
    }

    /** Idle: a faint fill. Focused: a softer light fill and a thin accent line. */
    private static GradientDrawable fill(float dn, boolean focused, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(focused ? 0x2AFFFFFF : 0x0FFFFFFF);
        g.setCornerRadius(radiusDp * dn);
        if (focused) g.setStroke(Math.max(2, Math.round(2 * dn)), (Theme.accent & 0x00FFFFFF) | 0xCC000000);
        return g;
    }

    private static View tile(final Context c, final Sheet.Item it, final float dn, final Dialog d, final Runnable render, boolean compact) {
        final LinearLayout t = new LinearLayout(c);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setFocusable(true);
        t.setClickable(true);
        int vp0 = Math.round((compact ? 5 : 8) * dn);
        t.setPadding(Math.round(6 * dn), vp0, Math.round(6 * dn), vp0);
        t.setBackground(fill(dn, false, 18));
        final ImageView ic = new ImageView(c);
        ic.setImageResource(it.icon);
        Sheet.tint(ic, 0xFFB9C4DD);
        t.addView(ic, new LinearLayout.LayoutParams(Math.round((compact ? 22 : 28) * dn), Math.round((compact ? 22 : 28) * dn)));
        TextView name = Sheet.text(c, it.title, compact ? 13 : 14, WHITE, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setGravity(Gravity.CENTER);
        name.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = Math.round((compact ? 4 : 7) * dn);
        t.addView(name, np);
        if (it.value != null) {
            TextView v = Sheet.text(c, it.value, compact ? 11 : 11.5f, MUTED, false);
            v.setSingleLine(true);
            v.setEllipsize(TextUtils.TruncateAt.END);
            v.setGravity(Gravity.CENTER);
            v.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2);
            vp.topMargin = Math.round(2 * dn);
            t.addView(v, vp);
        }
        t.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean f) {
                t.setBackground(fill(dn, f, 18));
                Sheet.tint(ic, f ? WHITE : 0xFFB9C4DD);
            }
        });
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (it.action != null) it.action.run();
                if (it.stay) render.run(); else d.dismiss();
            }
        });
        return t;
    }

    private static View row(final Context c, final Sheet.Item it, final float dn, final Dialog d, final Runnable render) {
        final LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(true);
        r.setClickable(true);
        r.setMinimumHeight(Math.round(46 * dn));
        r.setPadding(Math.round(14 * dn), Math.round(4 * dn), Math.round(16 * dn), Math.round(4 * dn));
        r.setBackground(fill(dn, false, 16));

        final ImageView ic = new ImageView(c);
        final boolean own = it.iconDrawable != null;
        if (own) ic.setImageDrawable(it.iconDrawable); else ic.setImageResource(it.icon);
        final int idleTint = it.danger ? 0xFFFF8A80 : 0xFFB9C4DD;
        if (!own) Sheet.tint(ic, idleTint);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(Math.round((own ? 32 : 24) * dn), Math.round((own ? 32 : 24) * dn));
        ip.setMarginEnd(Math.round(14 * dn));
        r.addView(ic, ip);

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        final TextView title = Sheet.text(c, it.title, 16.5f, it.danger ? 0xFFFFB4AB : WHITE, false);
        title.setTypeface(Fonts.medium);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(title);
        if (it.subtitle != null) {
            TextView sub = Sheet.text(c, it.subtitle, 12.5f, MUTED, false);
            sub.setSingleLine(true);
            sub.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
            sp.topMargin = Math.round(2 * dn);
            col.addView(sub, sp);
        }
        r.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));

        if (it.toggle != null) {
            r.addView(new Switch(c, it.toggle, dn), new LinearLayout.LayoutParams(Math.round(46 * dn), Math.round(26 * dn)));
        } else if (it.value != null) {
            TextView val = Sheet.text(c, it.value, 14, MUTED, false);
            val.setSingleLine(true);
            val.setEllipsize(TextUtils.TruncateAt.END);
            val.setMaxWidth(Math.round(170 * dn));
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-2, -2);
            vp.setMarginStart(Math.round(10 * dn));
            r.addView(val, vp);
        }

        r.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean f) {
                r.setBackground(fill(dn, f, 16));
                if (!own) Sheet.tint(ic, f ? WHITE : idleTint);
            }
        });
        r.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (it.action != null) it.action.run();
                if (it.stay) render.run(); else d.dismiss();
            }
        });
        return r;
    }

    /** A row of round colour choices; OK picks the focused one. */
    private static View swatches(final Context c, final Sheet.Item it, final float dn, final Dialog d, final Runnable render) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Math.round(4 * dn), Math.round(4 * dn), 0, Math.round(4 * dn));
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < Theme.NAMES.length; i++) {
            final int idx = i;
            final Dot dot = new Dot(c, Theme.swatch(i), i == it.selected, dn);
            dot.setFocusable(true);
            dot.setClickable(true);
            dot.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { if (it.pick != null) it.pick.pick(idx); render.run(); }
            });
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(Math.round(52 * dn), Math.round(52 * dn));
            dp.setMarginEnd(Math.round(6 * dn));
            row.addView(dot, dp);
        }
        box.addView(row);
        return box;
    }

    /** One colour dot: a ring when it is the current theme, a bright ring while focused. */
    private static final class Dot extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int color;
        private final boolean current;
        private final float dn;

        Dot(Context c, int color, boolean current, float dn) {
            super(c);
            this.color = color;
            this.current = current;
            this.dn = dn;
        }

        @Override protected void drawableStateChanged() { super.drawableStateChanged(); invalidate(); }

        @Override protected void onDraw(Canvas cv) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy) - 10 * dn;
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            cv.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.5f * dn);
            if (isFocused()) { p.setColor(WHITE); cv.drawCircle(cx, cy, r + 6 * dn, p); }
            else if (current) { p.setColor(0x99FFFFFF); cv.drawCircle(cx, cy, r + 5 * dn, p); }
        }

        @Override protected void onFocusChanged(boolean gain, int dir, android.graphics.Rect prev) {
            super.onFocusChanged(gain, dir, prev);
            invalidate();
        }
    }

    /** A real on/off switch drawn in the theme's accent. */
    private static final class Switch extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean on;

        Switch(Context c, boolean on, float dn) { super(c); this.on = on; }

        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight(), r = h / 2f;
            p.setColor(on ? Theme.accent : 0x33FFFFFF);
            cv.drawRoundRect(0, 0, w, h, r, r, p);
            p.setColor(on ? 0xFF0B1226 : 0xFFE8ECF5);
            cv.drawCircle(on ? w - r : r, r, r - h * 0.14f, p);
        }
    }
}
