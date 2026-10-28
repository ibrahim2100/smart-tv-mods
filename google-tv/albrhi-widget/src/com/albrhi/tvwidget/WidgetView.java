package com.albrhi.tvwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Clock + date (Gregorian and Hijri) + the next Barcelona matches.
 * Same view is used small on top of the home screen and large as the screensaver.
 */
final class WidgetView extends LinearLayout {
    static final int WHITE = 0xFFF4F6FB, MUTED = 0xFF9AA5C0, CARD = 0xFF121B38, BOX = 0xFF1B2547;
    /** Theme colours (see Theme.load): the accent, a second accent for gradients, and the theme's glow. */
    static int GOLD = 0xFF7FA8FF, GARNET = 0xFF7FA8FF, BLUE = 0xFF2A4A8C;
    static final String GREG = "ar_SA@calendar=gregorian;numbers=latn";
    static final String HIJRI = "ar_SA@calendar=islamic-umalqura;numbers=latn";
    static final long DAY = 86400000L, HOUR = 3600000L, MIN = 60000L;

    private final boolean full;
    private final float s, dn;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        public void run() { update(); Fixtures.refresh(getContext(), false); Weather.refresh(getContext()); h.postDelayed(this, 15000); }
    };
    private final Runnable onData = new Runnable() { public void run() { update(); } };

    private TextView weather, clock, ampm, greet, date, hijri, league, nHome, nAway, mTime, mDay, venue, listHead, lastRes;
    private ImageView bHome, bAway;
    private final TextView[] cdNum = new TextView[3];
    private LinearLayout cdRow, listBox, card;
    private View bar;
    private TextView cdText;

    WidgetView(Context c, boolean full) {
        super(c);
        this.full = full;
        this.s = full ? 1.0f : 1f;
        this.dn = c.getResources().getDisplayMetrics().density;
        build();
    }

    // ---- lifecycle --------------------------------------------------------------------------

    void start() {
        Fixtures.listeners.add(onData);
        Weather.init(getContext());
        h.removeCallbacks(tick);
        tick.run();
        Fixtures.refresh(getContext(), false);
        Weather.refresh(getContext());
    }

    void stop() {
        h.removeCallbacks(tick);
        Fixtures.listeners.remove(onData);
    }

    // ---- building ---------------------------------------------------------------------------

    private int px(float dp) { return Math.round(dp * s * dn); }

    private TextView tv(float sp, int color, boolean bold) {
        TextView t = new TextView(getContext());
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp * s);
        t.setTextColor(color);
        if (bold) t.setTypeface(Fonts.bold);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private LinearLayout.LayoutParams lp(int w, int hgt) { return new LinearLayout.LayoutParams(w, hgt); }

    private LinearLayout.LayoutParams lpw(float weight) {
        return new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, weight);
    }

    private GradientDrawable round(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(px(r));
        return g;
    }

    private void build() {
        setOrientation(VERTICAL);
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
        if (!full) {
            GradientDrawable bg = round(0xFF080E20, 22);
            bg.setStroke(Math.max(1, px(1)), 0x33FFFFFF);
            setBackground(bg);
            setPadding(px(18), px(10), px(18), px(10));
        }

        // header: clock + greeting/date
        LinearLayout clockRow = new LinearLayout(getContext());
        clockRow.setOrientation(HORIZONTAL);
        clockRow.setGravity(Gravity.BOTTOM);
        clockRow.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        clock = tv(full ? (Theme.sports ? 76 : 92) : 40, WHITE, true);
        clock.setTypeface((full ? Fonts.medium : Fonts.bold));
        ampm = tv(full ? 24 : 16, GOLD, true);
        clockRow.addView(clock);
        LinearLayout.LayoutParams ap = lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        ap.leftMargin = px(6);
        ap.bottomMargin = px(full ? 10 : 6);
        clockRow.addView(ampm, ap);

        LinearLayout dateCol = new LinearLayout(getContext());
        dateCol.setOrientation(VERTICAL);
        greet = tv(full ? 17 : 13, MUTED, false);
        weather = tv(full ? 17 : 13, MUTED, false);
        date = tv(full ? 24 : 16, WHITE, true);
        hijri = tv(full ? 16 : 13, MUTED, false);
        dateCol.addView(greet);                                 // only shown when you gave a name (Appearance > name)
        LinearLayout.LayoutParams dp = lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        dp.topMargin = px(full ? 8 : 3);
        dateCol.addView(date, dp);
        LinearLayout.LayoutParams wp = lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        wp.topMargin = px(full ? 6 : 2);
        dateCol.addView(weather, wp);
        LinearLayout.LayoutParams hp0 = lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        hp0.topMargin = px(3);
        dateCol.addView(hijri, hp0);

        if (full) {
            setGravity(Gravity.CENTER_HORIZONTAL);
            addView(clockRow, lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            dateCol.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams dc = lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            dc.topMargin = px(6);
            addView(dateCol, dc);
        } else {
            LinearLayout head = new LinearLayout(getContext());
            head.setOrientation(HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);
            head.addView(clockRow);
            View sp = new View(getContext());
            head.addView(sp, new LinearLayout.LayoutParams(0, 1, 1));
            head.addView(dateCol);
            addView(head, lp(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }

        // thin accent line in the theme colours
        bar = new View(getContext());
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, new int[]{GARNET, BLUE});
        g.setCornerRadius(px(2));
        bar.setBackground(g);
        LinearLayout.LayoutParams bp = lp(full ? px(440) : LayoutParams.MATCH_PARENT, px(3));
        bp.topMargin = px(full ? 16 : 7);
        bp.bottomMargin = px(full ? 16 : 6);
        addView(bar, bp);

        // next-match card
        card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        if (full) {
            card.setBackground(round(0x14FFFFFF, 24));
            card.setPadding(px(18), px(14), px(18), px(14));
        }
        LinearLayout labelRow = new LinearLayout(getContext());
        labelRow.setOrientation(HORIZONTAL);
        TextView label = tv(12, GOLD, true);
        label.setText("المباراة القادمة");
        league = tv(11, MUTED, false);
        league.setGravity(Gravity.END);
        league.setPadding(px(12), 0, 0, 0);
        labelRow.addView(label, lpw(1));
        labelRow.addView(league, lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        card.addView(labelRow, lp(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        bHome = new ImageView(getContext());
        bAway = new ImageView(getContext());
        nHome = tv(full ? 13 : 12, WHITE, true);
        nAway = tv(full ? 13 : 12, WHITE, true);
        row.addView(side(bAway, nAway), lpw(1));          // left to right: away team, the time, home team (home on the right)
        LinearLayout mid = new LinearLayout(getContext());
        mid.setOrientation(VERTICAL);
        mid.setGravity(Gravity.CENTER_HORIZONTAL);
        mDay = tv(12, MUTED, false);
        mTime = tv(22, GOLD, true);
        mDay.setGravity(Gravity.CENTER);
        mTime.setGravity(Gravity.CENTER);
        mid.addView(mDay, lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams tp = lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        tp.topMargin = px(2);
        mid.addView(mTime, tp);
        row.addView(mid, lpw(1.15f));
        row.addView(side(bHome, nHome), lpw(1));
        LinearLayout.LayoutParams rp = lp(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        rp.topMargin = px(full ? 8 : 4);
        card.addView(row, rp);

        cdRow = new LinearLayout(getContext());
        cdRow.setOrientation(HORIZONTAL);
        cdRow.setGravity(Gravity.CENTER);
        cdRow.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        String[] units = {"يوم", "ساعة", "دقيقة"};
        for (int i = 0; i < 3; i++) {
            LinearLayout b = new LinearLayout(getContext());
            b.setOrientation(VERTICAL);
            b.setGravity(Gravity.CENTER);
            b.setBackground(round(BOX, 10));
            b.setPadding(px(10), px(full ? 5 : 2), px(10), px(full ? 5 : 2));
            cdNum[i] = tv(full ? 20 : 17, WHITE, true);
            cdNum[i].setGravity(Gravity.CENTER);
            TextView u = tv(full ? 10 : 9, MUTED, false);
            u.setText(units[i]);
            u.setGravity(Gravity.CENTER);
            b.addView(cdNum[i], lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            b.addView(u, lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams bl = lp(px(full ? 62 : 56), LayoutParams.WRAP_CONTENT);
            bl.leftMargin = px(4);
            bl.rightMargin = px(4);
            cdRow.addView(b, bl);
        }
        LinearLayout.LayoutParams cp = lp(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cp.topMargin = px(full ? 9 : 5);
        card.addView(cdRow, cp);

        cdText = tv(full ? 17 : 13, MUTED, false);           // the countdown as one calm line (the boxes above are for the small widget)
        cdText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ct = lp(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ct.topMargin = px(8);
        card.addView(cdText, ct);
        cdText.setVisibility(GONE);

        venue = tv(11, MUTED, false);
        venue.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams vp = lp(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        vp.topMargin = px(8);
        card.addView(venue, vp);
        venue.setVisibility(GONE);

        addView(card, full ? lp(px(440), LayoutParams.WRAP_CONTENT) : lp(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // following matches
        listHead = tv(11, MUTED, true);
        listHead.setText("ثم");
        LinearLayout.LayoutParams hp = lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        hp.topMargin = px(full ? 18 : 2);
        addView(listHead, hp);
        listBox = new LinearLayout(getContext());
        listBox.setOrientation(VERTICAL);
        addView(listBox, full ? lp(px(440), LayoutParams.WRAP_CONTENT) : lp(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        lastRes = tv(11, MUTED, false);
        LinearLayout.LayoutParams lr = lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lr.topMargin = px(8);
        addView(lastRes, lr);
        lastRes.setVisibility(full ? VISIBLE : GONE);
    }

    private LinearLayout side(ImageView img, TextView name) {
        LinearLayout l = new LinearLayout(getContext());
        l.setOrientation(VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        l.addView(img, lp(px(full ? 46 : 34), px(full ? 46 : 34)));
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = lp(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        np.topMargin = px(full ? 4 : 2);
        l.addView(name, np);
        return l;
    }

    // ---- data -> views ----------------------------------------------------------------------

    static String fmt(String pattern, String loc, long t) {
        android.icu.text.SimpleDateFormat f = new android.icu.text.SimpleDateFormat(pattern, new android.icu.util.ULocale(loc));
        return f.format(new Date(t));
    }

    static String dayLabel(long t) {
        Calendar c = Calendar.getInstance();
        long off = c.getTimeZone().getOffset(t);
        long d = Math.floorDiv(t + off, DAY) - Math.floorDiv(System.currentTimeMillis() + off, DAY);
        if (d == 0) return "اليوم";
        if (d == 1) return "غداً";
        return fmt("EEEE d MMM", GREG, t);
    }

    static String greeting(int hour) { return Theme.greeting(hour); }

    void update() {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        clock.setText(fmt(Theme.timePattern(), GREG, now));
        ampm.setText(fmt("a", GREG, now));
        ampm.setVisibility(Theme.clock24 ? GONE : VISIBLE);
        greet.setText(greeting(cal.get(Calendar.HOUR_OF_DAY)));
        greet.setVisibility(Theme.name.length() > 0 ? VISIBLE : GONE);
        boolean wx = Theme.weather && Weather.text != null;
        weather.setText(wx ? Weather.text : "");
        weather.setVisibility(wx ? VISIBLE : GONE);
        date.setText(fmt("EEEE d MMMM", GREG, now));
        hijri.setText(fmt("d MMMM y", HIJRI, now) + " هـ");
        hijri.setVisibility(Theme.hijri ? VISIBLE : GONE);
        int sp = Theme.sports ? VISIBLE : GONE;               // the match card, list and last result only with sports on
        card.setVisibility(sp);
        listHead.setVisibility(sp);
        listBox.setVisibility(sp);
        lastRes.setVisibility(Theme.sports && full ? VISIBLE : GONE);
        bar.setVisibility(Theme.sports ? VISIBLE : GONE);
        if (!Theme.sports) return;

        List<Fixtures.Match> up = Fixtures.upcoming;
        if (up.isEmpty()) {
            league.setText("");
            nHome.setText("برشلونة");
            nAway.setText("—");
            setBadge(bHome, null);
            setBadge(bAway, null);
            mDay.setText("");
            mTime.setText("جاري التحميل");
            for (TextView n : cdNum) n.setText("--");
            listBox.removeAllViews();
            listHead.setVisibility(GONE);
            fillLast();
            return;
        }
        Fixtures.Match m = up.get(0);
        league.setText(Fixtures.ar(m.league));
        nHome.setText(Fixtures.ar(m.home));
        nAway.setText(Fixtures.ar(m.away));
        setBadge(bHome, Fixtures.badge(m.homeBadge));
        setBadge(bAway, Fixtures.badge(m.awayBadge));
        mDay.setText(dayLabel(m.start));
        long left = m.start - now;
        if (m.postponed) {
            mTime.setText("مؤجلة");
            for (TextView n : cdNum) n.setText("--");
        } else if (left <= 0) {
            mTime.setText("جارية الآن");
            for (TextView n : cdNum) n.setText("0");
        } else {
            mTime.setText(fmt("h:mm a", GREG, m.start));
            cdNum[0].setText(String.valueOf(left / DAY));
            cdNum[1].setText(String.valueOf((left % DAY) / HOUR));
            cdNum[2].setText(String.valueOf((left % HOUR) / MIN));
        }
        venue.setText(m.venue == null || m.venue.length() == 0 ? "" : "الملعب: " + m.venue);

        listBox.removeAllViews();
        int max = full ? 1 : 2;
        for (int i = 1; i < up.size() && i <= max; i++) listBox.addView(listRow(up.get(i)));
        listHead.setVisibility(full && up.size() > 1 ? VISIBLE : GONE);
        if (full) {                                          // saver: one line instead of three boxes
            cdRow.setVisibility(GONE);
            cdText.setVisibility(VISIBLE);
            boolean live = !m.postponed && left <= 0;
            cdText.setText(m.postponed ? "مؤجلة" : live ? "● جارية الآن" : TopPills.countdownText(left));
            cdText.setTextColor(live ? 0xFFFF453A : (!m.postponed && left < DAY) ? GOLD : MUTED);
            lastRes.setVisibility(GONE);
        }
        fillLast();
    }

    private void fillLast() {
        Fixtures.Match l = Fixtures.last;
        if (l == null) { lastRes.setText(""); return; }
        lastRes.setText("آخر نتيجة:  " + Fixtures.ar(l.home) + "  " + l.homeScore + " - " + l.awayScore + "  " + Fixtures.ar(l.away));
    }

    private View listRow(Fixtures.Match m) {
        LinearLayout r = new LinearLayout(getContext());
        r.setOrientation(HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackground(round(full ? 0x0FFFFFFF : CARD, 12));
        r.setPadding(px(10), px(full ? 5 : 3), px(10), px(full ? 5 : 3));
        ImageView b = new ImageView(getContext());
        setBadge(b, Fixtures.badge(m.opponentBadge()));
        r.addView(b, lp(px(full ? 22 : 20), px(full ? 22 : 20)));
        TextView name = tv(13, WHITE, true);
        name.setText(Fixtures.ar(m.opponent()) + (m.barcaHome ? "  (داخل)" : "  (خارج)"));
        LinearLayout.LayoutParams np = lpw(1);
        np.leftMargin = px(8);
        np.rightMargin = px(8);
        r.addView(name, np);
        TextView when = tv(12, GOLD, true);
        when.setText(dayLabel(m.start) + " · " + fmt("h:mm a", GREG, m.start));
        r.addView(when, lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams rp = lp(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        rp.topMargin = px(full ? 5 : 4);
        r.setLayoutParams(rp);
        return r;
    }

    private void setBadge(ImageView v, Bitmap b) {
        if (b != null) v.setImageBitmap(b); else v.setImageDrawable(null);
    }
}
