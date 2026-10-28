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
    private TextView mMeta, mHome, mAway, mTime, mCount;
    private ImageView bHome, bAway;
    private boolean mSoon;
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

        // ---- next-match card (focusable): league and day on top, the two teams with the time between them, the countdown under
        View sp1 = new View(getContext());
        addView(sp1, new LinearLayout.LayoutParams(0, 1, 1f));            // centres the card in the free space beside the clock
        matchFrame = new FrameLayout(getContext());
        matchFrame.setFocusable(true);
        matchFrame.setClickable(true);
        final LinearLayout cardBox = new LinearLayout(getContext());
        cardBox.setOrientation(VERTICAL);
        cardBox.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        cardBox.setPadding(px(20), px(9), px(20), px(10));
        cardBox.setBackground(cardBg(false, false));

        mMeta = tv(12.5f, MUTED, false);
        mMeta.setGravity(Gravity.CENTER);
        mMeta.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        cardBox.addView(mMeta, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        // left to right: away team, the time, home team (the home team sits on the right, as the Arabic text reads)
        bAway = new ImageView(getContext());
        bHome = new ImageView(getContext());
        mAway = tv(15, WHITE, true);
        mHome = tv(15, WHITE, true);
        mAway.setMaxWidth(px(130));
        mHome.setMaxWidth(px(130));
        mHome.setGravity(Gravity.END);
        mAway.setTextAlignment(TEXT_ALIGNMENT_VIEW_START);
        mHome.setTextAlignment(TEXT_ALIGNMENT_VIEW_END);
        mTime = tv(22, WHITE, false);
        mTime.setTypeface(Fonts.medium);
        mTime.setGravity(Gravity.CENTER);
        mTime.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        row.addView(bAway, new LinearLayout.LayoutParams(px(36), px(36)));
        LinearLayout.LayoutParams an = new LinearLayout.LayoutParams(0, -2, 1f);
        an.setMarginStart(px(10));
        row.addView(mAway, an);
        LinearLayout.LayoutParams tm = new LinearLayout.LayoutParams(px(104), -2);
        row.addView(mTime, tm);
        LinearLayout.LayoutParams hn = new LinearLayout.LayoutParams(0, -2, 1f);
        hn.setMarginEnd(px(10));
        row.addView(mHome, hn);
        row.addView(bHome, new LinearLayout.LayoutParams(px(36), px(36)));
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, -2);
        rl.topMargin = px(5);
        cardBox.addView(row, rl);

        mCount = tv(12.5f, MUTED, false);
        mCount.setGravity(Gravity.CENTER);
        mCount.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-1, -2);
        cl.topMargin = px(4);
        cardBox.addView(mCount, cl);
        matchFrame.addView(cardBox, new FrameLayout.LayoutParams(px(450), -2));

        matchFrame.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean f) { cardBox.setBackground(cardBg(f, mSoon)); }
        });
        matchFrame.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { MatchesPanel.show(getContext(), null); }
        });
        addView(matchFrame, wrap());
        View sp2 = new View(getContext());
        addView(sp2, new LinearLayout.LayoutParams(0, 1, 1f));
        applySports();
    }

    /** The card's fill: a faint light when idle, brighter with a thin accent line when focused (or soon). */
    private GradientDrawable cardBg(boolean focused, boolean soon) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(focused ? 0x2AFFFFFF : 0x14FFFFFF);
        g.setCornerRadius(px(22));
        if (focused) g.setStroke(Math.max(2, px(2)), (Theme.accent & 0x00FFFFFF) | 0xCC000000);
        else if (soon) g.setStroke(Math.max(1, px(1.5f)), (Theme.accent & 0x00FFFFFF) | 0x77000000);
        return g;
    }

    /** Settings changed while the home is on screen: redraw now (clock format, Hijri, weather, accent colour, match pill). */
    void relook() {
        update();
        if (Theme.sports) Fixtures.refresh(getContext(), true);
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
        ampm.setTextColor(GOLD);
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
            mMeta.setText("المباراة القادمة");
            mHome.setText("");
            mAway.setText("");
            mTime.setText("…");
            mCount.setText(Fixtures.lastError != null ? "⚠ تعذّر التحميل" : "جاري التحميل");
            mSoon = false;
            badge(bHome, null);
            badge(bAway, null);
            return;
        }
        Fixtures.Match m = up.get(0);
        long left = m.start - now;
        boolean live = !m.postponed && left <= 0;
        mSoon = !m.postponed && left < DAY;
        mMeta.setText(Fixtures.ar(m.league) + (m.postponed ? "" : "  ·  " + dayLabel(m.start)));
        mHome.setText(Fixtures.ar(m.home));
        mAway.setText(Fixtures.ar(m.away));
        badge(bHome, Fixtures.badge(m.homeBadge));
        badge(bAway, Fixtures.badge(m.awayBadge));
        mTime.setText(m.postponed ? "—" : fmt(Theme.clock24 ? "HH:mm" : "h:mm a", GREG, m.start));
        mTime.setTextColor(mSoon ? GOLD : WHITE);
        mCount.setText(m.postponed ? "مؤجلة" : live ? "● جارية الآن" : countdownText(left));
        mCount.setTextColor(live ? 0xFFFF453A : mSoon ? GOLD : MUTED);
        if (!matchFrame.isFocused()) ((LinearLayout) matchFrame.getChildAt(0)).setBackground(cardBg(false, mSoon));
    }
}
