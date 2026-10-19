package com.albrhi.tvwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

import static com.albrhi.tvwidget.WidgetView.*;

/**
 * Top strip of the launcher, kept short so the apps get the screen:
 * clock / date / weather on one side, the next Barcelona match on a single compact card.
 * The card gets a gold outline and a live countdown once the match is less than a day away.
 */
final class HeaderView extends LinearLayout {
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

    private TextView clock, ampm, dateLine, greetLine, league, countdown, nHome, nAway, mDay, mTime;
    private ImageView bHome, bAway;
    private LinearLayout card;
    private GradientDrawable cardBg;

    HeaderView(Context c) {
        super(c);
        dn = c.getResources().getDisplayMetrics().density;
        build();
    }

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
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    private static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, w);
    }

    private static CharSequence two(String a, int ca, String b, int cb) {
        SpannableStringBuilder s = new SpannableStringBuilder(a);
        s.setSpan(new ForegroundColorSpan(ca), 0, a.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (b != null && b.length() > 0) {
            int st = s.length();
            s.append("  ·  ").append(b);
            s.setSpan(new ForegroundColorSpan(cb), st, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    private void build() {
        setOrientation(HORIZONTAL);
        setLayoutDirection(LAYOUT_DIRECTION_RTL);
        setGravity(Gravity.CENTER_VERTICAL);

        // ---- clock / date / greeting + weather
        LinearLayout block = new LinearLayout(getContext());
        block.setOrientation(VERTICAL);
        block.setGravity(Gravity.CENTER_HORIZONTAL);      // clock, date and weather share one centre line
        LinearLayout clockRow = new LinearLayout(getContext());
        clockRow.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        clockRow.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        clock = tv(46, WHITE, true);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        ampm = tv(16, GOLD, true);
        clockRow.addView(clock);
        LinearLayout.LayoutParams ap = wrap();
        ap.leftMargin = px(6);
        ap.bottomMargin = px(8);
        clockRow.addView(ampm, ap);
        block.addView(clockRow);
        dateLine = tv(16, WHITE, true);
        greetLine = tv(14, MUTED, false);
        dateLine.setGravity(Gravity.CENTER);
        greetLine.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams t1 = wrap();
        t1.topMargin = px(3);
        block.addView(dateLine, t1);
        block.addView(greetLine, t1);
        LinearLayout.LayoutParams bl = wrap();
        bl.setMarginEnd(px(28));
        addView(block, bl);

        // ---- one compact match card
        card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        cardBg = new GradientDrawable();
        cardBg.setColor(0xFF0F1730);
        cardBg.setCornerRadius(px(16));
        cardBg.setStroke(Math.max(1, px(1)), 0x22FFFFFF);
        card.setBackground(cardBg);
        card.setPadding(px(16), px(9), px(16), px(10));

        LinearLayout rowA = new LinearLayout(getContext());
        rowA.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = tv(13, GOLD, true);
        label.setText("المباراة القادمة");
        league = tv(13, MUTED, false);
        LinearLayout.LayoutParams lg = wrap();
        lg.setMarginStart(px(8));
        countdown = tv(14, GOLD, true);
        rowA.addView(label, wrap());
        rowA.addView(league, lg);
        rowA.addView(new android.view.View(getContext()), new LinearLayout.LayoutParams(0, 1, 1f));
        rowA.addView(countdown, wrap());
        card.addView(rowA, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout rowB = new LinearLayout(getContext());
        rowB.setGravity(Gravity.CENTER_VERTICAL);
        bHome = new ImageView(getContext());
        bAway = new ImageView(getContext());
        nHome = tv(18, WHITE, true);
        nAway = tv(18, WHITE, true);
        nHome.setGravity(Gravity.CENTER_VERTICAL);
        nAway.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout homeSide = new LinearLayout(getContext());
        homeSide.setGravity(Gravity.CENTER_VERTICAL);
        homeSide.addView(bHome, new LinearLayout.LayoutParams(px(40), px(40)));
        LinearLayout.LayoutParams hn = weight(1);
        hn.setMarginStart(px(10));
        homeSide.addView(nHome, hn);

        LinearLayout mid = new LinearLayout(getContext());
        mid.setOrientation(VERTICAL);
        mid.setGravity(Gravity.CENTER_HORIZONTAL);
        mDay = tv(13, MUTED, false);
        mTime = tv(22, GOLD, true);
        mDay.setGravity(Gravity.CENTER);
        mTime.setGravity(Gravity.CENTER);
        mid.addView(mDay, wrap());
        mid.addView(mTime, wrap());

        LinearLayout awaySide = new LinearLayout(getContext());
        awaySide.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams an = weight(1);
        an.setMarginEnd(px(10));
        nAway.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        awaySide.addView(nAway, an);
        awaySide.addView(bAway, new LinearLayout.LayoutParams(px(40), px(40)));

        rowB.addView(homeSide, weight(1));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(px(120), LayoutParams.WRAP_CONTENT);
        rowB.addView(mid, mp);
        rowB.addView(awaySide, weight(1));
        LinearLayout.LayoutParams rb = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        rb.topMargin = px(4);
        card.addView(rowB, rb);

        addView(card, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
    }

    private void badge(ImageView v, Bitmap b) {
        if (b != null) v.setImageBitmap(b); else v.setImageDrawable(null);
    }

    static String countdownText(long left) {
        long d = left / DAY, hh = (left % DAY) / HOUR, m = (left % HOUR) / MIN;
        if (d == 0) return hh > 0 ? "بعد " + hh + " ساعة و" + m + " دقيقة" : "بعد " + m + " دقيقة";
        if (d == 1) return "بعد يوم و" + hh + " ساعة";
        if (d == 2) return "بعد يومين";
        if (d <= 10) return "بعد " + d + " أيام";
        return "بعد " + d + " يومًا";
    }

    void update() {
        long now = System.currentTimeMillis();
        clock.setText(fmt("h:mm", GREG, now));
        ampm.setText(fmt("a", GREG, now));
        dateLine.setText(two(fmt("EEEE d MMMM", GREG, now), WHITE, fmt("d MMMM y", HIJRI, now) + " هـ", GOLD));
        greetLine.setText(two(greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)), MUTED, Weather.text, WHITE));

        List<Fixtures.Match> up = Fixtures.upcoming;
        if (up.isEmpty()) {
            nHome.setText("برشلونة");
            nAway.setText("—");
            league.setText("");
            mDay.setText("");
            mTime.setText("…");
            countdown.setText("جاري التحميل");
            badge(bHome, null);
            badge(bAway, null);
            return;
        }
        Fixtures.Match m = up.get(0);
        long left = m.start - now;
        boolean soon = !m.postponed && left < DAY;               // within a day (or already running)
        cardBg.setStroke(Math.max(1, px(soon ? 2 : 1)), soon ? GOLD : 0x22FFFFFF);
        league.setText(Fixtures.ar(m.league));
        nHome.setText(Fixtures.ar(m.home));
        nAway.setText(Fixtures.ar(m.away));
        badge(bHome, Fixtures.badge(m.homeBadge));
        badge(bAway, Fixtures.badge(m.awayBadge));
        if (m.postponed) {
            mDay.setText("");
            mTime.setText("مؤجلة");
            countdown.setText("");
        } else if (left <= 0) {
            mDay.setText("");
            mTime.setText("جارية الآن");
            countdown.setText("");
        } else {
            mDay.setText(dayLabel(m.start));
            mTime.setText(fmt("h:mm a", GREG, m.start));
            countdown.setText(countdownText(left));
            countdown.setTextColor(soon ? GOLD : MUTED);
        }
    }
}
