package com.albrhi.tvwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static com.albrhi.tvwidget.WidgetView.*;

/**
 * The fixtures panel (opens from the match card): the next match as a clean card, the following ones as quiet rows
 * with the opponent's crest, the last result, and a refresh row. Same look as the quick-settings panel.
 */
final class MatchesPanel {
    private MatchesPanel() { }

    private static final int GOOD = 0xFF34C759, BAD = 0xFFFF453A, DRAW = 0xFFFFB020;

    static void show(final Context c, final Runnable onClose) {
        final float dn = c.getResources().getDisplayMetrics().density;
        final Runnable[] live = new Runnable[1];
        live[0] = new Runnable() { public void run() { Panel.refresh(); } };       // downloads finishing redraw the open panel
        Fixtures.listeners.add(live[0]);
        Panel.show(c, "المباريات", "برشلونة", new Panel.Builder() { public List<Sheet.Item> items() {
            List<Sheet.Item> items = Sheet.list();
            List<Fixtures.Match> up = Fixtures.upcoming;
            if (up.isEmpty()) {
                items.add(Sheet.Item.custom(note(c, dn, Fixtures.lastError != null ? "⚠ " + Fixtures.lastError : "ما فيه مباريات محمّلة بعد. اضغط «تحديث الآن».")));
            } else {
                items.add(Sheet.Item.header("المباراة القادمة"));
                items.add(Sheet.Item.custom(hero(c, up.get(0), dn)));
                if (up.size() > 1) {
                    items.add(Sheet.Item.header("بعدها"));
                    for (int i = 1; i < up.size() && i <= 3; i++) {
                        final Fixtures.Match m = up.get(i);
                        Drawable crest = crest(c, Fixtures.badge(m.opponentBadge()));
                        Sheet.Item it = Sheet.Item.of(Fixtures.ar(m.opponent()) + (m.barcaHome ? "  (داخل)" : "  (خارج)"), R.drawable.ic_ball, null)
                                .sub(Fixtures.ar(m.league)).value(m.postponed ? "مؤجلة" : dayLabel(m.start) + " · " + fmt(Theme.clock24 ? "HH:mm" : "h:mm a", GREG, m.start)).stay();
                        if (crest != null) it.icon(crest);
                        items.add(it);
                    }
                }
            }
            Fixtures.Match last = Fixtures.last;
            if (last != null && last.homeScore != null) {
                items.add(Sheet.Item.header("آخر نتيجة"));
                items.add(Sheet.Item.custom(result(c, last, dn)));
            }
            items.add(Sheet.Item.header("التحديث"));
            long upd = Fixtures.updatedAt();
            items.add(Sheet.Item.of("تحديث الآن", R.drawable.ic_refresh, new Runnable() { public void run() {
                Toast.makeText(c, "جاري تحديث المباريات…", Toast.LENGTH_SHORT).show();
                Fixtures.refresh(c, true);
            } }).sub(Fixtures.lastError != null ? "⚠ " + Fixtures.lastError
                    : upd == 0 ? "ما تحمّلت بعد" : "آخر تحديث " + dayLabel(upd) + " " + fmt(Theme.clock24 ? "HH:mm" : "h:mm a", GREG, upd) + " · يتحدث كل 12 ساعة").stay());
            return items;
        } }, null, new Runnable() { public void run() {
            Fixtures.listeners.remove(live[0]);
            if (onClose != null) onClose.run();
        } });
    }

    // ---- views -------------------------------------------------------------------------------

    private static View note(Context c, float dn, String s) {
        TextView t = text(c, s, 14, MUTED, false);
        t.setSingleLine(false);
        t.setPadding(px(6, dn), px(10, dn), px(6, dn), px(10, dn));
        return t;
    }

    /** The next match: league and countdown on top, the two teams with the time between them. */
    private static View hero(Context c, Fixtures.Match m, float dn) {
        long left = m.start - System.currentTimeMillis();
        boolean live = !m.postponed && left <= 0;
        boolean soon = !m.postponed && left < DAY;
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        card.setBackground(box(0x14FFFFFF, 20, dn, soon ? (Theme.accent & 0x00FFFFFF) | 0x99000000 : 0));
        card.setPadding(px(16, dn), px(12, dn), px(16, dn), px(14, dn));

        LinearLayout top = new LinearLayout(c);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView cd = text(c, m.postponed ? "مؤجلة" : live ? "● جارية الآن" : TopPills.countdownText(left), 13.5f, live ? BAD : soon ? Theme.accent : MUTED, true);
        top.addView(cd);
        top.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 1f));
        top.addView(text(c, Fixtures.ar(m.league), 13, MUTED, false));
        card.addView(top, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout mid = new LinearLayout(c);
        mid.setGravity(Gravity.CENTER_VERTICAL);
        // left to right: away team, the time, home team (the home team sits on the right, as the Arabic text reads)
        mid.addView(team(c, m.awayBadge, m.away, false, dn), new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout center = new LinearLayout(c);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView day = text(c, m.postponed ? "" : dayLabel(m.start), 13, MUTED, false);
        TextView time = text(c, "", 26, soon ? Theme.accent : WHITE, false);
        time.setTypeface(Fonts.medium);
        if (m.postponed) time.setText("—");
        else {                                              // "11:00" with a small "م" beside it
            String hm = fmt(Theme.clock24 ? "HH:mm" : "h:mm", GREG, m.start);
            if (Theme.clock24) time.setText(hm);
            else {
                android.text.SpannableString sp = new android.text.SpannableString(hm + " " + fmt("a", GREG, m.start));
                sp.setSpan(new android.text.style.RelativeSizeSpan(0.5f), hm.length(), sp.length(), 0);
                time.setText(sp);
            }
        }
        day.setGravity(Gravity.CENTER);
        time.setGravity(Gravity.CENTER);
        center.addView(day);
        center.addView(time);
        mid.addView(center, new LinearLayout.LayoutParams(px(120, dn), -2));
        mid.addView(team(c, m.homeBadge, m.home, true, dn), new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.topMargin = px(10, dn);
        card.addView(mid, mp);
        return card;
    }

    /** One team: its crest above its name, centred. */
    private static View team(Context c, String badgeUrl, String name, boolean home, float dn) {
        LinearLayout t = new LinearLayout(c);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView b = new ImageView(c);
        b.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Bitmap bm = Fixtures.badge(badgeUrl);
        if (bm != null) b.setImageBitmap(bm);
        t.addView(b, new LinearLayout.LayoutParams(px(46, dn), px(46, dn)));
        TextView n = text(c, Fixtures.ar(name), 15, WHITE, true);
        n.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-2, -2);
        np.topMargin = px(6, dn);
        t.addView(n, np);
        return t;
    }

    /** The last result: a verdict chip, the score, and the two teams. */
    private static View result(Context c, Fixtures.Match m, float dn) {
        int mine = m.barcaHome ? m.homeScore.intValue() : m.awayScore.intValue();
        int theirs = m.barcaHome ? m.awayScore.intValue() : m.homeScore.intValue();
        int color = mine > theirs ? GOOD : mine < theirs ? BAD : DRAW;
        String verdict = mine > theirs ? "فوز" : mine < theirs ? "خسارة" : "تعادل";

        LinearLayout r = new LinearLayout(c);
        r.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackground(box(0x0FFFFFFF, 16, dn, 0));
        r.setPadding(px(14, dn), px(10, dn), px(14, dn), px(10, dn));

        TextView v = text(c, verdict, 12.5f, 0xFF06101F, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(box(color, 9, dn, 0));
        v.setPadding(px(10, dn), px(3, dn), px(10, dn), px(3, dn));
        r.addView(v, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout away = side(c, m.awayBadge, m.away, false, dn);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, -2, 1f);
        ap.setMarginStart(px(12, dn));
        r.addView(away, ap);
        TextView score = text(c, m.awayScore + "  -  " + m.homeScore, 22, WHITE, false);     // left to right: away score, home score
        score.setTypeface(Fonts.medium);
        score.setGravity(Gravity.CENTER);
        score.setTextDirection(View.TEXT_DIRECTION_LTR);            // digits keep their order: away score first, then home
        r.addView(score, new LinearLayout.LayoutParams(px(96, dn), -2));
        r.addView(side(c, m.homeBadge, m.home, true, dn), new LinearLayout.LayoutParams(0, -2, 1f));
        return r;
    }

    private static LinearLayout side(Context c, String badgeUrl, String name, boolean home, float dn) {
        LinearLayout s = new LinearLayout(c);
        s.setGravity(Gravity.CENTER_VERTICAL);
        ImageView b = new ImageView(c);
        b.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Bitmap bm = Fixtures.badge(badgeUrl);
        if (bm != null) b.setImageBitmap(bm);
        TextView n = text(c, Fixtures.ar(name), 14, WHITE, true);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
        if (home) {                       // name then crest, the crest at the right edge
            n.setGravity(Gravity.END);
            np.setMarginEnd(px(8, dn));
            s.addView(n, np);
            s.addView(b, new LinearLayout.LayoutParams(px(30, dn), px(30, dn)));
        } else {
            np.setMarginStart(px(8, dn));
            s.addView(b, new LinearLayout.LayoutParams(px(30, dn), px(30, dn)));
            s.addView(n, np);
        }
        return s;
    }

    // ---- helpers -----------------------------------------------------------------------------

    private static Drawable crest(Context c, Bitmap b) { return b == null ? null : new BitmapDrawable(c.getResources(), b); }

    private static int px(float dp, float dn) { return Math.round(dp * dn); }

    private static GradientDrawable box(int color, float r, float dn, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(r * dn);
        if (stroke != 0) g.setStroke(Math.max(2, Math.round(1.5f * dn)), stroke);
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
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        return t;
    }
}
