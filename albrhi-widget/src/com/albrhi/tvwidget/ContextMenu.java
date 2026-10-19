package com.albrhi.tvwidget;

import android.app.Activity;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import static com.albrhi.tvwidget.WidgetView.*;

/**
 * tvOS-style context menu. The focused app lifts off the grid, everything else is blurred and dimmed,
 * and a glass menu drops down right under the app (or above it when there is no room below).
 * The focused row turns into a white pill with dark text, like on Apple TV.
 */
final class ContextMenu {
    private final Activity act;
    private final FrameLayout root;
    private final View content;
    private final float dn;
    private FrameLayout overlay;
    private LinearLayout panel;
    private Runnable afterClose;
    private boolean open;

    ContextMenu(Activity act, FrameLayout root, View content) {
        this.act = act;
        this.root = root;
        this.content = content;
        this.dn = act.getResources().getDisplayMetrics().density;
    }

    boolean isOpen() { return open; }

    private int px(float dp) { return Math.round(dp * dn); }

    /**
     * @param anchor  the tile that was long-pressed
     * @param face    builds the lifted copy of the tile (same artwork), sized w x h
     * @param onClose called after the menu is gone, with the chosen action (or null)
     */
    void show(View anchor, View face, String title, List<Sheet.Item> items, Runnable onClose) {
        if (open) return;
        open = true;
        afterClose = onClose;

        // exact on-screen box of the (unscaled) tile
        anchor.animate().cancel();
        anchor.setScaleX(1f);
        anchor.setScaleY(1f);
        int[] a = new int[2], r = new int[2];
        anchor.getLocationInWindow(a);
        root.getLocationInWindow(r);
        final int tx = a[0] - r[0], ty = a[1] - r[1], tw = anchor.getWidth(), th = anchor.getHeight();
        final int rootW = root.getWidth(), rootH = root.getHeight();

        overlay = new FrameLayout(act);
        overlay.setClickable(true);
        overlay.setFocusable(false);
        final View scrim = new View(act);
        scrim.setBackgroundColor(0xCC050813);
        scrim.setAlpha(0f);
        overlay.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        // the lifted tile
        FrameLayout lifted = new FrameLayout(act);
        GradientDrawable lb = new GradientDrawable();
        lb.setColor(0xFF151D38);
        lb.setCornerRadius(px(12));
        lifted.setBackground(lb);
        lifted.setClipToOutline(true);
        lifted.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), px(12)); }
        });
        lifted.addView(face, new FrameLayout.LayoutParams(-1, -1));
        final GradientDrawable ring = new GradientDrawable();
        ring.setColor(0);
        ring.setCornerRadius(px(12));
        ring.setStroke(px(2.5f), 0xFFFFFFFF);
        lifted.setForeground(ring);
        lifted.setElevation(px(24));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(tw, th);
        lp.leftMargin = tx;
        lp.topMargin = ty;
        overlay.addView(lifted, lp);

        // the glass menu
        panel = new LinearLayout(act);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        GradientDrawable pb = new GradientDrawable();
        pb.setColor(0xF01B2336);
        pb.setCornerRadius(px(24));
        pb.setStroke(Math.max(1, px(1)), 0x30FFFFFF);
        panel.setBackground(pb);
        panel.setElevation(px(30));
        panel.setPadding(px(8), px(6), px(8), px(6));

        View first = null;
        for (int i = 0; i < items.size(); i++) {
            Sheet.Item it = items.get(i);
            if (i > 0) panel.addView(divider());
            View row = row(it);
            panel.addView(row, new LinearLayout.LayoutParams(-1, px(50)));
            if (first == null) first = row;
        }

        final int menuW = px(290);
        panel.measure(View.MeasureSpec.makeMeasureSpec(menuW, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(rootH, View.MeasureSpec.AT_MOST));
        final int menuH = panel.getMeasuredHeight();
        final int gap = px(16), edge = px(24);
        final int placement;                       // 0 = below the app, 1 = above it, 2 = beside it
        int top, left;
        if (ty + th + gap + menuH <= rootH - px(16)) {
            placement = 0;
            top = ty + th + gap;
            left = Math.max(edge, Math.min(rootW - edge - menuW, tx + tw / 2 - menuW / 2));
        } else if (ty - gap - menuH >= px(16)) {
            placement = 1;
            top = ty - gap - menuH;
            left = Math.max(edge, Math.min(rootW - edge - menuW, tx + tw / 2 - menuW / 2));
        } else {                                   // no room above or below: put it next to the app, on the roomier side
            placement = 2;
            boolean leftSide = tx - gap - menuW >= edge && (tx - edge) >= (rootW - tx - tw - edge);
            left = leftSide ? tx - gap - menuW : Math.min(rootW - edge - menuW, tx + tw + gap);
            top = Math.max(px(16), Math.min(rootH - px(16) - menuH, ty + th / 2 - menuH / 2));
        }
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(menuW, -2);
        pl.leftMargin = left;
        pl.topMargin = top;
        overlay.addView(panel, pl);

        // the app's name as a pill: above the tile, or below it when the menu sits above
        TextView pill = new TextView(act);
        pill.setText(title);
        pill.setTextColor(0xFFFFFFFF);
        pill.setTypeface(Fonts.bold);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        pill.setSingleLine(true);
        pill.setEllipsize(TextUtils.TruncateAt.END);
        pill.setIncludeFontPadding(false);
        pill.setTextDirection(View.TEXT_DIRECTION_RTL);
        pill.setPadding(px(16), px(7), px(16), px(8));
        pill.setMaxWidth(px(320));
        GradientDrawable pg = new GradientDrawable();
        pg.setColor(0xF2141C33);
        pg.setCornerRadius(px(20));
        pg.setStroke(Math.max(1, px(1)), 0x33FFFFFF);
        pill.setBackground(pg);
        pill.setElevation(px(20));
        pill.measure(View.MeasureSpec.makeMeasureSpec(px(320), View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED);
        FrameLayout.LayoutParams ppl = new FrameLayout.LayoutParams(-2, -2);
        int pw = pill.getMeasuredWidth(), ph = pill.getMeasuredHeight();
        ppl.leftMargin = Math.max(edge, Math.min(rootW - edge - pw, tx + tw / 2 - pw / 2));
        ppl.topMargin = placement == 1 ? ty + th + px(16) : Math.max(px(8), ty - ph - px(16));
        overlay.addView(pill, ppl);
        pill.setAlpha(0f);
        pill.animate().alpha(1f).setStartDelay(80).setDuration(200).start();

        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        // everything underneath is blurred and cannot take focus
        if (content instanceof ViewGroup) ((ViewGroup) content).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        if (Build.VERSION.SDK_INT >= 31) {
            try { content.setRenderEffect(RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP)); } catch (Throwable ignored) { }
        }

        // entrance
        scrim.animate().alpha(1f).setDuration(220).start();
        lifted.animate().scaleX(1.14f).scaleY(1.14f).setDuration(240).setInterpolator(new DecelerateInterpolator(1.4f)).start();
        if (placement == 2) {
            panel.setPivotX(left < tx ? menuW : 0f);               // grows out of the edge that faces the app
            panel.setPivotY(ty + th / 2f - top);
        } else {
            panel.setPivotX(tx + tw / 2f - left);
            panel.setPivotY(placement == 0 ? 0f : menuH);
        }
        panel.setAlpha(0f);
        panel.setScaleX(0.84f);
        panel.setScaleY(0.84f);
        panel.setTranslationY(placement == 0 ? -px(14) : placement == 1 ? px(14) : 0);
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setDuration(300)
                .setInterpolator(new OvershootInterpolator(0.9f)).start();
        for (int i = 0; i < panel.getChildCount(); i++) {
            View c = panel.getChildAt(i);
            c.setAlpha(0f);
            c.animate().alpha(1f).setStartDelay(60L + 35L * i).setDuration(200).start();
        }
        if (first != null) first.requestFocus();
    }

    private View divider() {
        View d = new View(act);
        d.setBackgroundColor(0x1AFFFFFF);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, Math.max(1, px(1)));
        p.setMargins(px(14), 0, px(14), 0);
        d.setLayoutParams(p);
        return d;
    }

    private View row(final Sheet.Item it) {
        final LinearLayout r = new LinearLayout(act);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(true);
        r.setClickable(true);
        r.setPadding(px(14), 0, px(14), 0);
        final GradientDrawable idle = new GradientDrawable();
        idle.setColor(0);
        idle.setCornerRadius(px(17));
        final GradientDrawable on = new GradientDrawable();
        on.setColor(it.danger ? 0xFFFF453A : 0xFAFFFFFF);
        on.setCornerRadius(px(17));
        r.setBackground(idle);

        final TextView t = new TextView(act);
        t.setText(it.title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.5f);
        t.setTypeface(Fonts.bold);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        final int idleText = it.danger ? 0xFFFF6B60 : WHITE;
        t.setTextColor(idleText);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));

        final ImageView ic = new ImageView(act);
        ic.setImageResource(it.icon);
        final int idleTint = it.danger ? 0xFFFF6B60 : 0xCCFFFFFF;
        Sheet.tint(ic, idleTint);
        r.addView(ic, new LinearLayout.LayoutParams(px(24), px(24)));

        r.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean f) {
                r.setBackground(f ? on : idle);
                int fg = it.danger ? 0xFFFFFFFF : 0xFF0B1226;
                t.setTextColor(f ? fg : idleText);
                Sheet.tint(ic, f ? fg : idleTint);
                r.animate().scaleX(f ? 1.03f : 1f).scaleY(f ? 1.03f : 1f).setDuration(120).start();
            }
        });
        r.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { close(it.action); }
        });
        return r;
    }

    /** Closes with a short animation, then runs the chosen action. */
    void close(final Runnable action) {
        if (!open) return;
        open = false;
        final FrameLayout ov = overlay;
        overlay = null;
        if (content instanceof ViewGroup) ((ViewGroup) content).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        if (Build.VERSION.SDK_INT >= 31) content.setRenderEffect(null);
        final Runnable done = afterClose;
        afterClose = null;
        panel.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(120).start();
        ov.animate().alpha(0f).setDuration(150).withEndAction(new Runnable() {
            @Override public void run() {
                root.removeView(ov);
                if (done != null) done.run();            // restores focus to the tile
                if (action != null) action.run();
            }
        }).start();
    }

    /** Immediate close (activity pausing). */
    void dismissNow() {
        if (!open) return;
        open = false;
        if (overlay != null) root.removeView(overlay);
        overlay = null;
        if (content instanceof ViewGroup) ((ViewGroup) content).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        if (Build.VERSION.SDK_INT >= 31) content.setRenderEffect(null);
        Runnable done = afterClose;
        afterClose = null;
        if (done != null) done.run();
    }
}
