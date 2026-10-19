package com.albrhi.tvwidget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static com.albrhi.tvwidget.WidgetView.*;

/** Barcelona fixtures sheet: the next match as a hero card, the following ones as rows, and the last result. */
final class MatchesDialog {
    private MatchesDialog() { }

    private static final int GOOD = 0xFF34C759, BAD = 0xFFFF453A, DRAW = 0xFFFFB020;

    static void show(final Context c) {
        final float dn = c.getResources().getDisplayMetrics().density;
        final Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(round(0xFF0B1226, 22, dn, 0x33FFFFFF));
        root.setPadding(px(22, dn), px(16, dn), px(22, dn), px(16, dn));

        // ---- header: title + updated-at on one side, buttons on the other
        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(c);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(c, "مباريات برشلونة", 20, GOLD, true);
        final TextView updated = text(c, "", 12.5f, MUTED, false);
        titles.addView(title);
        titles.addView(updated);
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView refresh = button(c, "تحديث", dn, BLUE);
        final TextView close = button(c, "إغلاق", dn, 0xFF2A3358);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(px(96, dn), -2);
        bp.setMarginStart(px(10, dn));
        head.addView(refresh, bp);
        head.addView(close, bp);
        root.addView(head, new LinearLayout.LayoutParams(-1, -2));

        final LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.addView(body);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, px(330, dn));
        sp.topMargin = px(10, dn);
        root.addView(sv, sp);

        final Runnable rebuild = new Runnable() {
            public void run() {
                long upd = Fixtures.updatedAt();
                String err = Fixtures.lastError;
                updated.setTextColor(err != null ? 0xFFFFB020 : MUTED);
                updated.setText(err != null ? "⚠ " + err + (upd == 0 ? "" : "  ·  المعروض من آخر تحديث ناجح")
                        : upd == 0 ? "ما تحمّلت المباريات بعد"
                        : "آخر تحديث: " + dayLabel(upd) + " " + fmt("h:mm a", GREG, upd) + "  ·  يتحدث كل 12 ساعة");
                fill(c, body, dn);
            }
        };
        rebuild.run();
        Fixtures.listeners.add(rebuild);

        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updated.setText("جاري التحديث…");
                Toast.makeText(c, "جاري تحديث المباريات…", Toast.LENGTH_SHORT).show();
                Fixtures.refresh(c, true);
            }
        });
        close.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { d.dismiss(); } });
        d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            public void onDismiss(android.content.DialogInterface x) { Fixtures.listeners.remove(rebuild); }
        });

        d.setContentView(root, new LinearLayout.LayoutParams(px(720, dn), -2));
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setDimAmount(0.7f);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setLayout(px(720, dn), WindowManager.LayoutParams.WRAP_CONTENT);
        }
        d.show();
        close.requestFocus();
    }

    // ---- content ----------------------------------------------------------------------------

    private static void fill(Context c, LinearLayout body, float dn) {
        body.removeAllViews();
        List<Fixtures.Match> up = Fixtures.upcoming;
        if (up.isEmpty()) {
            TextView t = text(c, "ما فيه مباريات محمّلة حاليًا. اضغط «تحديث».", 15, MUTED, false);
            t.setGravity(Gravity.CENTER);
            body.addView(t, new LinearLayout.LayoutParams(-1, px(120, dn)));
        } else {
            body.addView(hero(c, up.get(0), dn));
            if (up.size() > 1) {
                body.addView(section(c, "المباريات القادمة", dn));
                for (int i = 1; i < up.size() && i <= 7; i++) body.addView(row(c, up.get(i), dn));
            }
        }
        Fixtures.Match last = Fixtures.last;
        if (last != null && last.homeScore != null) {
            body.addView(section(c, "آخر نتيجة", dn));
            body.addView(result(c, last, dn));
        }
    }

    private static View hero(Context c, Fixtures.Match m, float dn) {
        long left = m.start - System.currentTimeMillis();
        boolean soon = !m.postponed && left < DAY;
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(0xFF121B38, 18, dn, soon ? GOLD : 0x22FFFFFF, soon ? 2 : 1));
        card.setPadding(px(18, dn), px(12, dn), px(18, dn), px(14, dn));

        LinearLayout top = new LinearLayout(c);
        TextView lbl = text(c, "المباراة القادمة", 13, GOLD, true);
        TextView lg = text(c, Fixtures.ar(m.league), 13, MUTED, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(px(8, dn));
        top.addView(lbl);
        top.addView(lg, lp);
        top.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 1f));
        TextView cd = text(c, m.postponed ? "مؤجلة" : HeaderView.countdownText(left), 14, soon ? GOLD : MUTED, true);
        top.addView(cd);
        card.addView(top, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout mid = new LinearLayout(c);
        mid.setGravity(Gravity.CENTER_VERTICAL);
        mid.addView(side(c, m.homeBadge, m.home, true, dn), new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout center = new LinearLayout(c);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView day = text(c, m.postponed ? "" : dayLabel(m.start), 14, MUTED, false);
        TextView time = text(c, m.postponed ? "—" : fmt("h:mm a", GREG, m.start), 28, GOLD, true);
        day.setGravity(Gravity.CENTER);
        time.setGravity(Gravity.CENTER);
        center.addView(day);
        center.addView(time);
        mid.addView(center, new LinearLayout.LayoutParams(px(150, dn), -2));
        mid.addView(side(c, m.awayBadge, m.away, false, dn), new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.topMargin = px(6, dn);
        card.addView(mid, mp);
        if (m.venue != null && m.venue.length() > 0) {
            TextView v = text(c, "الملعب: " + m.venue, 12, MUTED, false);
            v.setGravity(Gravity.CENTER);
            v.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2);
            vp.topMargin = px(6, dn);
            card.addView(v, vp);
        }
        return card;
    }

    private static View side(Context c, String badgeUrl, String name, boolean first, float dn) {
        LinearLayout s = new LinearLayout(c);
        s.setGravity(Gravity.CENTER_VERTICAL);
        ImageView b = badge(c, Fixtures.badge(badgeUrl));
        TextView n = text(c, Fixtures.ar(name), 20, WHITE, true);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
        if (first) {            // home team on the right: badge at the outer edge
            s.addView(b, new LinearLayout.LayoutParams(px(58, dn), px(58, dn)));
            np.setMarginStart(px(10, dn));
            s.addView(n, np);
        } else {
            np.setMarginEnd(px(10, dn));
            n.setGravity(Gravity.END);
            s.addView(n, np);
            s.addView(b, new LinearLayout.LayoutParams(px(58, dn), px(58, dn)));
        }
        return s;
    }

    private static View section(Context c, String s, float dn) {
        TextView t = text(c, s, 13, MUTED, true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.topMargin = px(14, dn);
        p.bottomMargin = px(2, dn);
        t.setLayoutParams(p);
        return t;
    }

    private static int leagueColor(String league) {
        String l = league == null ? "" : league.toLowerCase();
        if (l.contains("champions")) return 0xFF2F80ED;      // Champions League blue
        if (l.contains("la liga")) return 0xFFA50044;        // La Liga garnet
        if (l.contains("copa") || l.contains("cup")) return GOLD;
        return 0xFF6B7690;
    }

    private static View row(Context c, Fixtures.Match m, float dn) {
        LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(true);
        r.setBackground(round(0xFF121B38, 14, dn, 0));
        r.setPadding(0, px(7, dn), px(14, dn), px(7, dn));
        r.setFocusableInTouchMode(false);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = px(6, dn);
        r.setLayoutParams(rp);

        View bar = new View(c);                     // league colour strip on the start side
        GradientDrawable g = new GradientDrawable();
        g.setColor(leagueColor(m.league));
        g.setCornerRadius(px(2, dn));
        bar.setBackground(g);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(px(4, dn), px(40, dn));
        bp.setMarginEnd(px(12, dn));
        bp.setMarginStart(px(10, dn));
        r.addView(bar, bp);

        r.addView(badge(c, Fixtures.badge(m.opponentBadge())), new LinearLayout.LayoutParams(px(38, dn), px(38, dn)));
        LinearLayout t = new LinearLayout(c);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(text(c, Fixtures.ar(m.opponent()), 17, WHITE, true));
        t.addView(text(c, Fixtures.ar(m.league), 12.5f, MUTED, false));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMarginStart(px(12, dn));
        r.addView(t, tp);

        TextView pill = text(c, m.barcaHome ? "داخل" : "خارج", 12, m.barcaHome ? 0xFFFFFFFF : MUTED, true);
        pill.setGravity(Gravity.CENTER);
        pill.setBackground(round(m.barcaHome ? BLUE : 0xFF1B2547, 9, dn, 0));
        pill.setPadding(px(10, dn), px(3, dn), px(10, dn), px(3, dn));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-2, -2);
        pp.setMarginEnd(px(14, dn));
        r.addView(pill, pp);

        LinearLayout when = new LinearLayout(c);
        when.setOrientation(LinearLayout.VERTICAL);
        when.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView day = text(c, m.postponed ? "مؤجلة" : dayLabel(m.start), 13, MUTED, false);
        TextView time = text(c, m.postponed ? "—" : fmt("h:mm a", GREG, m.start), 18, GOLD, true);
        day.setGravity(Gravity.CENTER);
        time.setGravity(Gravity.CENTER);
        when.addView(day);
        when.addView(time);
        r.addView(when, new LinearLayout.LayoutParams(px(120, dn), -2));

        final GradientDrawable idle = round(0xFF121B38, 14, dn, 0);
        final GradientDrawable on = round(0xFF1A2550, 14, dn, 0xFFFFFFFF, 2);
        final View rr = r;
        r.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean f) { rr.setBackground(f ? on : idle); }
        });
        return r;
    }

    private static View result(Context c, Fixtures.Match m, float dn) {
        int mine = m.barcaHome ? m.homeScore.intValue() : m.awayScore.intValue();
        int theirs = m.barcaHome ? m.awayScore.intValue() : m.homeScore.intValue();
        int color = mine > theirs ? GOOD : mine < theirs ? BAD : DRAW;
        String verdict = mine > theirs ? "فوز" : mine < theirs ? "خسارة" : "تعادل";

        LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(true);
        r.setBackground(round(0xFF121B38, 14, dn, 0));
        r.setPadding(px(14, dn), px(10, dn), px(14, dn), px(10, dn));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = px(6, dn);
        r.setLayoutParams(rp);

        TextView v = text(c, verdict, 13, 0xFF06101F, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(round(color, 9, dn, 0));
        v.setPadding(px(12, dn), px(4, dn), px(12, dn), px(4, dn));
        r.addView(v, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout home = new LinearLayout(c);
        home.setGravity(Gravity.CENTER_VERTICAL);
        home.addView(badge(c, Fixtures.badge(m.homeBadge)), new LinearLayout.LayoutParams(px(34, dn), px(34, dn)));
        LinearLayout.LayoutParams hn = new LinearLayout.LayoutParams(0, -2, 1f);
        hn.setMarginStart(px(10, dn));
        home.addView(text(c, Fixtures.ar(m.home), 16, WHITE, true), hn);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, -2, 1f);
        hp.setMarginStart(px(14, dn));
        r.addView(home, hp);

        // the home team sits on the right, so with a left-to-right number its score goes last
        TextView score = text(c, m.awayScore + "  -  " + m.homeScore, 24, WHITE, true);
        score.setGravity(Gravity.CENTER);
        score.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        r.addView(score, new LinearLayout.LayoutParams(px(120, dn), -2));

        LinearLayout away = new LinearLayout(c);
        away.setGravity(Gravity.CENTER_VERTICAL);
        TextView an = text(c, Fixtures.ar(m.away), 16, WHITE, true);
        an.setGravity(Gravity.END);
        LinearLayout.LayoutParams anp = new LinearLayout.LayoutParams(0, -2, 1f);
        anp.setMarginEnd(px(10, dn));
        away.addView(an, anp);
        away.addView(badge(c, Fixtures.badge(m.awayBadge)), new LinearLayout.LayoutParams(px(34, dn), px(34, dn)));
        r.addView(away, new LinearLayout.LayoutParams(0, -2, 1f));

        final GradientDrawable idle = round(0xFF121B38, 14, dn, 0);
        final GradientDrawable on = round(0xFF1A2550, 14, dn, 0xFFFFFFFF, 2);
        final View rr = r;
        r.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View x, boolean f) { rr.setBackground(f ? on : idle); }
        });
        return r;
    }

    // ---- small helpers ----------------------------------------------------------------------

    private static int px(float dp, float dn) { return Math.round(dp * dn); }

    private static GradientDrawable round(int color, float r, float dn, int stroke) {
        return round(color, r, dn, stroke, 1);
    }

    private static GradientDrawable round(int color, float r, float dn, int stroke, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(r * dn);
        if (stroke != 0) g.setStroke(Math.max(1, Math.round(strokeDp * dn)), stroke);
        return g;
    }

    private static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) t.setTypeface(Fonts.bold);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private static ImageView badge(Context c, Bitmap b) {
        ImageView v = new ImageView(c);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (b != null) v.setImageBitmap(b);
        return v;
    }

    private static TextView button(Context c, String s, final float dn, final int color) {
        final TextView b = text(c, s, 14, WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setClickable(true);
        b.setPadding(0, px(9, dn), 0, px(9, dn));
        final GradientDrawable idle = round(color, 10, dn, 0);
        final GradientDrawable on = round(color, 10, dn, 0xFFFFFFFF, 2.5f);
        b.setBackground(idle);
        b.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean f) { b.setBackground(f ? on : idle); }
        });
        return b;
    }
}
