package com.albrhi.tvwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

import static com.albrhi.tvwidget.WidgetView.*;

/**
 * The top-left of the home screen: a large, quiet clock with the date (and, if switched on, the Hijri date and weather)
 * written straight on the background. With "sports" switched on (Quick settings > Appearance) the next-match pill
 * (focusable, OK opens the fixtures sheet) sits beside it.
 */
final class TopPills extends LinearLayout {
    private final float dn;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        public void run() {
            update();
            Fixtures.refresh(getContext(), false);
            Weather.refresh(getContext());
            h.postDelayed(this, 15000);
        }
    };
    private final Runnable onData = new Runnable() { public void run() { update(); } };

    private TextView clock, ampm, date, hijri, weather;
    private TextView mNames, mWhen;
    private ImageView bHome, bAway;
    private GradientDrawable matchBg;
    private FrameLayout matchFrame;

    TopPills(Context c) {
        super(c);
        dn = c.getResources().getDisplayMetrics().density;
        build();
    }

    /** The next-match pill, or null while sports are off. */
    View matchView() { return Theme.sports ? matchFrame : null; }

    void start() {
        Fixtures.listeners.add(onData);
        Weather.init(getContext());
        h.removeCallbacks(tick);
        tick.run();
    }

    void stop() {
        h.removeCallbacks(tick);
        Fixtures.listeners.remove(onData);
    }

    private int px(float dp) { return Math.round(dp * dn); }

    private TextView tv(float sp, int color, boolean bold) {
        TextView t = new TextView(getContext());
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Fonts.bold);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private GradientDrawable pill(int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(px(30));
        g.setStroke(Math.max(1, px(1)), 0x33FFFFFF);
        return g;
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    private View divider() {
        View v = new View(getContext());
        v.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(Math.max(1, px(1)), px(28));
        p.setMargins(px(16), 0, px(16), 0);
        v.setLayoutParams(p);
        return v;
    }

    private void build() {
        setOrientation(HORIZONTAL);
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
        setGravity(Gravity.CENTER_VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);

        // ---- clock block: time on the first line, date and details under it (no box, no border)
        LinearLayout block = new LinearLayout(getContext());
        block.setOrientation(VERTICAL);
        block.setLayoutDirection(LAYOUT_DIRECTION_LTR);

        LinearLayout timeRow = new LinearLayout(getContext());
        timeRow.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        timeRow.setGravity(Gravity.BOTTOM);
        clock = tv(42, WHITE, false);
        clock.setTypeface(Fonts.medium);
        ampm = tv(15, GOLD, true);
        timeRow.addView(clock);
        LinearLayout.LayoutParams ap = wrap();
        ap.leftMargin = px(7);
        ap.bottomMargin = px(7);
        timeRow.addView(ampm, ap);
        block.addView(timeRow, wrap());

        LinearLayout line = new LinearLayout(getContext());          // date and weather on one line
        line.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        line.setGravity(Gravity.CENTER_VERTICAL);
        date = tv(16, WHITE, true);
        weather = tv(14, MUTED, false);
        line.addView(date, wrap());
        LinearLayout.LayoutParams wl = wrap();
        wl.setMarginStart(px(14));
        line.addView(weather, wl);
        LinearLayout.LayoutParams dp = wrap();
        dp.topMargin = px(2);
        block.addView(line, dp);
        hijri = tv(13, MUTED, false);
        LinearLayout.LayoutParams hp = wrap();
        hp.topMargin = px(3);
        block.addView(hijri, hp);
        addView(block, wrap());

        // ---- next-match pill (focusable)
        matchFrame = new FrameLayout(getContext());
        matchFrame.setFocusable(true);
        matchFrame.setClickable(true);
        LinearLayout mp = new LinearLayout(getContext());
        mp.setGravity(Gravity.CENTER_VERTICAL);
        mp.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        matchBg = pill(0xB30E1530);
        mp.setBackground(matchBg);
        mp.setPadding(px(18), px(7), px(22), px(7));

        LinearLayout badges = new LinearLayout(getContext());
        badges.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        bHome = new ImageView(getContext());
        bAway = new ImageView(getContext());
        badges.addView(bHome, new LinearLayout.LayoutParams(px(34), px(34)));
        LinearLayout.LayoutParams ab = new LinearLayout.LayoutParams(px(34), px(34));
        ab.leftMargin = px(-8);
        badges.addView(bAway, ab);
        mp.addView(badges, wrap());

        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(VERTICAL);
        mNames = tv(14, WHITE, true);
        mWhen = tv(12.5f, GOLD, false);
        mNames.setMaxWidth(px(300));
        mWhen.setMaxWidth(px(300));
        col.addView(mNames, wrap());
        LinearLayout.LayoutParams wp = wrap();
        wp.topMargin = px(2);
        col.addView(mWhen, wp);
        LinearLayout.LayoutParams cl = wrap();
        cl.setMarginStart(px(12));
        mp.addView(col, cl);
        matchFrame.addView(mp);

        final GradientDrawable ring = new GradientDrawable();
        ring.setColor(0);
        ring.setCornerRadius(px(30));
        ring.setStroke(px(2.5f), 0xFFFFFFFF);
        ring.setAlpha(0);
        matchFrame.setForeground(new android.graphics.drawable.InsetDrawable(ring, Math.round(3 * getResources().getDisplayMetrics().density)));
        matchFrame.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean f) {
                matchFrame.animate().scaleX(f ? 1.04f : 1f).scaleY(f ? 1.04f : 1f).setDuration(120).start();
                ring.setAlpha(f ? 255 : 0);
            }
        });
        matchFrame.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { MatchesDialog.show(getContext(), matchFrame); }
        });
        LinearLayout.LayoutParams ml = wrap();
        ml.setMarginStart(px(34));
        addView(matchFrame, ml);
        applySports();
    }

    /** Shows or hides the match pill (sports setting). */
    void applySports() {
        matchFrame.setVisibility(Theme.sports ? VISIBLE : GONE);
        matchFrame.setFocusable(Theme.sports);
    }

    static String countdownText(long left) {
        long d = left / DAY, hh = (left % DAY) / HOUR, m = (left % HOUR) / MIN;
        if (d == 0) return hh > 0 ? "بعد " + hh + " ساعة و" + m + " دقيقة" : "بعد " + m + " دقيقة";
        if (d == 1) return "بعد يوم و" + hh + " ساعة";
        if (d == 2) return "بعد يومين";
        if (d <= 10) return "بعد " + d + " أيام";
        return "بعد " + d + " يومًا";
    }

    private void badge(ImageView v, Bitmap b) {
        if (b != null) v.setImageBitmap(b); else v.setImageDrawable(null);
    }

    void update() {
        long now = System.currentTimeMillis();
        clock.setText(fmt(Theme.timePattern(), GREG, now));
        ampm.setText(fmt("a", GREG, now));
        ampm.setVisibility(Theme.clock24 ? GONE : VISIBLE);
        date.setText(fmt("EEEE d MMMM", GREG, now));
        hijri.setText(fmt("d MMMM y", HIJRI, now) + " هـ");
        hijri.setVisibility(Theme.hijri ? VISIBLE : GONE);
        boolean wx = Theme.weather && Weather.text != null;
        weather.setText(wx ? Weather.text : "");
        weather.setVisibility(wx ? VISIBLE : GONE);
        applySports();
        if (!Theme.sports) return;

        List<Fixtures.Match> up = Fixtures.upcoming;
        if (up.isEmpty()) {
            mNames.setText("مباريات برشلونة");
            mWhen.setText("جاري التحميل…");
            badge(bHome, null);
            badge(bAway, null);
            return;
        }
        Fixtures.Match m = up.get(0);
        long left = m.start - now;
        boolean soon = !m.postponed && left < DAY;
        matchBg.setStroke(Math.max(1, px(soon ? 2 : 1)), soon ? GOLD : 0x33FFFFFF);
        badge(bHome, Fixtures.badge(m.homeBadge));
        badge(bAway, Fixtures.badge(m.awayBadge));
        mNames.setText(Fixtures.ar(m.home) + "  ×  " + Fixtures.ar(m.away));
        if (m.postponed) mWhen.setText("مؤجلة");
        else if (left <= 0) mWhen.setText("جارية الآن");
        else mWhen.setText(dayLabel(m.start) + " " + fmt("h:mm a", GREG, m.start) + "  ·  " + countdownText(left));
        mWhen.setTextColor(soon ? GOLD : MUTED);
    }
}
