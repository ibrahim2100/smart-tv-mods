package com.albrhi.tvwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Barcelona fixtures from TheSportsDB (public test key), cached on disk so the widget survives offline. */
final class Fixtures {
    static final String TEAM_ID = "133739";
    /** TheSportsDB base URL; the key in it can be replaced (setKey) if the public test key is ever switched off. */
    private static volatile String API = "https://www.thesportsdb.com/api/v1/json/123/";
    /** Why the last download failed (null when it worked). */
    static volatile String lastError;
    private static final long REFRESH_MS = 12L * 60 * 60 * 1000;   // fixtures are re-checked twice a day (postponements, new kick-off times)

    static final class Match {
        String home, away, league, venue, homeBadge, awayBadge;
        long start;
        boolean barcaHome;
        Integer homeScore, awayScore;
        String leagueId, id;
        int round;
        boolean postponed;

        String opponent() { return barcaHome ? away : home; }
        String opponentBadge() { return barcaHome ? awayBadge : homeBadge; }
    }

    /** When the fixtures were last downloaded successfully (0 = never). */
    static long updatedAt() { return lastFetch; }

    static volatile List<Match> upcoming = new ArrayList<Match>();
    static volatile Match last;
    static final Set<Runnable> listeners = new CopyOnWriteArraySet<Runnable>();
    private static final Map<String, Bitmap> badges = new ConcurrentHashMap<String, Bitmap>();
    private static final ExecutorService io = Executors.newSingleThreadExecutor();
    private static final ExecutorService img = Executors.newSingleThreadExecutor();
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static long lastFetch, lastAttempt;
    private static boolean inited, fetching, badgesQueued;

    static synchronized void init(Context c) {
        if (inited) return;
        inited = true;
        SharedPreferences p = prefs(c);
        lastFetch = p.getLong("lt", 0L);
        API = "https://www.thesportsdb.com/api/v1/json/" + p.getString("key", "123") + "/";
        try {
            String up = p.getString("up", null);
            if (up != null) {
                List<Match> l = new ArrayList<Match>();
                long now = System.currentTimeMillis();
                JSONArray a = new JSONArray(up);
                for (int i = 0; i < a.length(); i++) {
                    Match m = fromJson(a.getJSONObject(i));
                    if (m.start > now - 3L * 60 * 60 * 1000) l.add(m);
                }
                upcoming = l;
            }
            String lm = p.getString("lastm", null);
            if (lm != null) last = fromJson(new JSONObject(lm));
        } catch (Throwable ignored) { }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("fx", Context.MODE_PRIVATE);
    }

    static void refresh(final Context ctx, boolean force) {
        final Context c = ctx.getApplicationContext();
        init(c);
        if (!badgesQueued) {              // paint the cached badges once per process, even when no download is due
            badgesQueued = true;
            img.execute(new Runnable() {
                public void run() { preloadBadges(c); notifyListeners(); }
            });
        }
        long now = System.currentTimeMillis();
        synchronized (Fixtures.class) {
            if (fetching || (!force && (now - lastFetch < REFRESH_MS || now - lastAttempt < 120000L))) return;
            fetching = true;
            lastAttempt = now;
        }
        io.execute(new Runnable() {
            public void run() {
                try {
                    String next = get(API + "eventsnext.php?id=" + TEAM_ID);
                    if (next == null) { lastError = "تعذّر الوصول لمصدر المباريات"; return; }          // offline: keep cache
                    if (!next.contains("\"events\"")) { lastError = "مصدر المباريات رفض المفتاح الحالي"; return; }
                    lastError = null;
                    long now = System.currentTimeMillis();
                    List<Match> all = new ArrayList<Match>();
                    Match[] lastFin = new Match[1];
                    JSONArray a = new JSONObject(next).optJSONArray("events");
                    for (int i = 0; a != null && i < a.length(); i++) {
                        Match m = toMatch(a.getJSONObject(i));
                        if (m != null && m.start > now - 3L * 60 * 60 * 1000) all.add(m);
                    }
                    Map<String, Integer> hint = new HashMap<String, Integer>();
                    for (Match m : all) hintRound(hint, m);
                    try { Thread.sleep(3000); } catch (InterruptedException ie) { return; }
                    String lastJson = get(API + "eventslast.php?id=" + TEAM_ID);
                    if (lastJson != null) {
                        JSONArray r = new JSONObject(lastJson).optJSONArray("results");
                        for (int i = 0; r != null && i < r.length(); i++) {
                            Match m = toMatch(r.getJSONObject(i));
                            if (m == null) continue;
                            hintRound(hint, m);
                            if (m.homeScore != null && (lastFin[0] == null || m.start > lastFin[0].start)) lastFin[0] = m;
                        }
                    }
                    boolean ok = true;
                    if (last != null && (lastFin[0] == null || last.start > lastFin[0].start)) lastFin[0] = last;
                    ok &= scanLeague(c, "4335", 38, hint, all, lastFin);   // La Liga
                    ok &= scanLeague(c, "4480", 8, hint, all, lastFin);    // Champions League (league phase)
                    // A complete download replaces what we had, so a postponed match disappears from its old slot.
                    // If it was cut short (rate limit), keep the cached matches that the fresh data does not cover.
                    Map<String, Match> byKey = new java.util.LinkedHashMap<String, Match>();
                    for (Match m : all) if (!byKey.containsKey(key(m))) byKey.put(key(m), m);
                    if (!ok) for (Match m : upcoming) if (!byKey.containsKey(key(m))) byKey.put(key(m), m);
                    List<Match> sorted = new ArrayList<Match>(byKey.values());
                    Collections.sort(sorted, new Comparator<Match>() {
                        public int compare(Match x, Match y) { return Long.compare(x.start, y.start); }
                    });
                    List<Match> uniq = new ArrayList<Match>();
                    for (Match m : sorted) {
                        if (m.start < now - 3L * 60 * 60 * 1000) continue;
                        if (uniq.size() < 8) uniq.add(m);
                    }
                    JSONArray out = new JSONArray();
                    for (Match m : uniq) out.put(toJson(m));
                    SharedPreferences.Editor e = prefs(c).edit();
                    e.putString("up", out.toString());
                    if (ok) { lastFetch = System.currentTimeMillis(); e.putLong("lt", lastFetch); }
                    if (lastFin[0] != null) e.putString("lastm", toJson(lastFin[0]).toString());
                    e.apply();
                    Match oldFirst = upcoming.isEmpty() ? null : upcoming.get(0);
                    upcoming = uniq;
                    Alerts.schedule(c);
                    if (oldFirst != null && !uniq.isEmpty() && key(oldFirst).equals(key(uniq.get(0)))
                            && Math.abs(oldFirst.start - uniq.get(0).start) >= 60000L) {
                        AppService.notifyChange(c, uniq.get(0), oldFirst.start);   // postponed / moved
                    }
                    if (lastFin[0] != null) last = lastFin[0];
                    img.execute(new Runnable() { public void run() { preloadBadges(c); notifyListeners(); } });
                } catch (Throwable t) {
                    // keep the cached data; try again on the next tick
                } finally {
                    synchronized (Fixtures.class) { fetching = false; }
                    notifyListeners();
                }
            }
        });
    }

    static void notifyUi() { notifyListeners(); }

    private static void notifyListeners() {
        main.post(new Runnable() {
            public void run() { for (Runnable r : listeners) r.run(); }
        });
    }

    /** Use your own TheSportsDB key (a paid/free personal one) instead of the public test key. */
    static void setKey(Context c, String key) {
        String k = key == null || key.trim().length() == 0 ? "123" : key.trim();
        prefs(c).edit().putString("key", k).putLong("lt", 0L).apply();
        API = "https://www.thesportsdb.com/api/v1/json/" + k + "/";
        lastFetch = 0L;
        refresh(c, true);
    }

    static String get(String url) {
        HttpURLConnection h = null;
        try {
            h = (HttpURLConnection) new URL(url).openConnection();
            h.setConnectTimeout(8000);
            h.setReadTimeout(12000);
            InputStream in = h.getInputStream();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            in.close();
            return out.toString("UTF-8");
        } catch (Throwable t) {
            return null;
        } finally {
            if (h != null) h.disconnect();
        }
    }

    private static String key(Match m) { return m.home + "|" + m.away + "|" + m.league; }

    private static void hintRound(Map<String, Integer> hint, Match m) {
        if (m.leagueId == null || m.round <= 0) return;
        Integer cur = hint.get(m.leagueId);
        if (cur == null || m.round < cur.intValue()) hint.put(m.leagueId, Integer.valueOf(m.round));
    }

    /**
     * Walks a league's rounds, keeps Barcelona's upcoming matches (max 3) and the latest finished one.
     * The free API allows only a handful of calls per burst, so calls are spaced out and capped.
     * Returns false if it had to stop early (rate limit / offline).
     */
    private static boolean scanLeague(Context c, String league, int maxRound, Map<String, Integer> hint,
                                      List<Match> out, Match[] lastFin) {
        SharedPreferences p = prefs(c);
        Integer h = hint.get(league);
        int start = p.getInt("a" + league, Math.max(1, h == null ? 1 : h.intValue()));
        int anchor = start, got = 0, calls = 0;
        long now = System.currentTimeMillis();
        boolean ok = true;
        for (int r = start; r <= maxRound && got < 3; r++) {
            if (calls >= 7) { ok = false; break; }
            try { Thread.sleep(3000); } catch (InterruptedException e) { ok = false; break; }
            calls++;
            String js = get(API + "eventsround.php?id=" + league + "&r=" + r + "&s=" + season());
            if (js == null) { ok = false; break; }
            try {
                JSONArray a = new JSONObject(js).optJSONArray("events");
                if (a == null) continue;
                long maxStart = 0;
                Match mine = null;
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    Match m = toMatch(o);
                    if (m == null) continue;
                    maxStart = Math.max(maxStart, m.start);
                    if (TEAM_ID.equals(o.optString("idHomeTeam")) || TEAM_ID.equals(o.optString("idAwayTeam"))) mine = m;
                }
                if (maxStart > 0 && maxStart < now - 24L * 60 * 60 * 1000) anchor = r;
                if (mine == null) continue;
                if (mine.start > now - 3L * 60 * 60 * 1000) { out.add(mine); got++; }
                else if (mine.homeScore != null && (lastFin[0] == null || mine.start > lastFin[0].start)) lastFin[0] = mine;
            } catch (Throwable ignored) { ok = false; break; }
        }
        p.edit().putInt("a" + league, anchor).apply();
        return ok;
    }

    private static String season() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int y = c.get(java.util.Calendar.YEAR);
        return c.get(java.util.Calendar.MONTH) >= java.util.Calendar.JULY ? y + "-" + (y + 1) : (y - 1) + "-" + y;
    }

    private static JSONObject toJson(Match m) throws org.json.JSONException {
        JSONObject o = new JSONObject();
        o.put("h", m.home).put("a", m.away).put("l", m.league).put("v", m.venue)
                .put("hb", m.homeBadge == null ? "" : m.homeBadge).put("ab", m.awayBadge == null ? "" : m.awayBadge)
                .put("t", m.start).put("bh", m.barcaHome).put("id", m.id == null ? "" : m.id).put("pp", m.postponed);
        if (m.homeScore != null) o.put("hs", m.homeScore.intValue()).put("as", m.awayScore.intValue());
        return o;
    }

    private static Match fromJson(JSONObject o) {
        Match m = new Match();
        m.home = o.optString("h");
        m.away = o.optString("a");
        m.league = o.optString("l");
        m.venue = o.optString("v");
        m.homeBadge = nz(o.optString("hb"));
        m.awayBadge = nz(o.optString("ab"));
        m.start = o.optLong("t");
        m.barcaHome = o.optBoolean("bh");
        m.id = nz(o.optString("id"));
        m.postponed = o.optBoolean("pp");
        if (o.has("hs")) { m.homeScore = Integer.valueOf(o.optInt("hs")); m.awayScore = Integer.valueOf(o.optInt("as")); }
        return m;
    }

    private static Match toMatch(JSONObject o) {
        try {
            Match m = new Match();
            m.home = o.optString("strHomeTeam");
            m.away = o.optString("strAwayTeam");
            m.league = o.optString("strLeague");
            m.id = nz(o.optString("idEvent"));
            m.postponed = "yes".equalsIgnoreCase(o.optString("strPostponed"));
            m.leagueId = nz(o.optString("idLeague"));
            m.round = o.optInt("intRound");
            m.venue = o.optString("strVenue");
            m.homeBadge = nz(o.optString("strHomeTeamBadge"));
            m.awayBadge = nz(o.optString("strAwayTeamBadge"));
            m.barcaHome = TEAM_ID.equals(o.optString("idHomeTeam"));
            String ts = nz(o.optString("strTimestamp"));
            if (ts == null) ts = o.optString("dateEvent") + "T" + o.optString("strTime");
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            m.start = f.parse(ts.length() > 19 ? ts.substring(0, 19) : ts).getTime();
            if (!o.isNull("intHomeScore") && !"".equals(o.optString("intHomeScore"))) {
                m.homeScore = Integer.valueOf(o.optInt("intHomeScore"));
                m.awayScore = Integer.valueOf(o.optInt("intAwayScore"));
            }
            return m;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String nz(String s) { return (s == null || s.length() == 0 || "null".equals(s)) ? null : s; }

    /** One event by id (status, score, postponed flag), or null. */
    static JSONObject lookupEvent(String id) {
        try {
            String js = get(API + "lookupevent.php?id=" + id);
            if (js == null) return null;
            JSONArray a = new JSONObject(js).optJSONArray("events");
            return a == null || a.length() == 0 ? null : a.getJSONObject(0);
        } catch (Throwable t) {
            return null;
        }
    }

    static Match parseEvent(JSONObject o) { return toMatch(o); }

    // ---- badges -----------------------------------------------------------------------------

    static Bitmap badge(String url) { return url == null ? null : badges.get(url); }

    static void preloadBadges(Context c) {
        List<Match> all = new ArrayList<Match>(upcoming);
        if (last != null) all.add(last);
        for (Match m : all) { load(c, m.homeBadge); load(c, m.awayBadge); }
    }

    private static void load(Context c, String url) {
        if (url == null || badges.containsKey(url)) return;
        File f = new File(c.getFilesDir(), "b" + Integer.toHexString(url.hashCode()) + ".png");
        try {
            if (!f.exists()) {
                HttpURLConnection h = (HttpURLConnection) new URL(url + "/small").openConnection();
                h.setConnectTimeout(8000);
                h.setReadTimeout(12000);
                if (h.getResponseCode() != 200) {
                    h.disconnect();
                    h = (HttpURLConnection) new URL(url).openConnection();
                }
                InputStream in = h.getInputStream();
                FileOutputStream out = new FileOutputStream(f);
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) out.write(b, 0, n);
                out.close();
                in.close();
                h.disconnect();
            }
            Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (bm != null) badges.put(url, bm);
        } catch (Throwable t) {
            f.delete();
        }
    }

    // ---- Arabic names -----------------------------------------------------------------------

    private static final Map<String, String> AR = new HashMap<String, String>();
    static {
        String[] kv = {
            "Barcelona", "برشلونة", "Real Madrid", "ريال مدريد", "Atletico Madrid", "أتلتيكو مدريد",
            "Athletic Club", "أتلتيك بلباو", "Athletic Bilbao", "أتلتيك بلباو", "Real Sociedad", "ريال سوسيداد",
            "Real Betis", "ريال بيتيس", "Sevilla", "إشبيلية", "Valencia", "فالنسيا", "Villarreal", "فياريال",
            "Getafe", "خيتافي", "Atlético Madrid", "أتلتيكو مدريد", "Deportivo Alavés", "ألافيس", "Aston Villa", "أستون فيلا",
            "Sabah Baku", "سابا باكو", "Sabah", "سابا باكو", "Brighton", "برايتون", "Real Valladolid", "بلد الوليد",
            "Atlético de Madrid", "أتلتيكو مدريد", "Bayer 04 Leverkusen", "باير ليفركوزن", "Tottenham", "توتنهام", "Girona", "جيرونا", "Celta Vigo", "سيلتا فيغو", "Osasuna", "أوساسونا",
            "Espanyol", "إسبانيول", "Mallorca", "مايوركا", "Rayo Vallecano", "رايو فايكانو", "Alaves", "ألافيس",
            "Deportivo Alaves", "ألافيس", "Levante", "ليفانتي", "Elche", "إلتشي", "Real Oviedo", "ريال أوفييدو",
            "Oviedo", "ريال أوفييدو", "Las Palmas", "لاس بالماس", "Leganes", "ليغانيس", "Valladolid", "بلد الوليد",
            "Paris Saint-Germain", "باريس سان جيرمان", "Bayern Munich", "بايرن ميونخ", "Chelsea", "تشيلسي",
            "Manchester City", "مانشستر سيتي", "Manchester United", "مانشستر يونايتد", "Liverpool", "ليفربول",
            "Arsenal", "آرسنال", "Tottenham Hotspur", "توتنهام", "Newcastle United", "نيوكاسل",
            "Inter Milan", "إنتر ميلان", "Inter", "إنتر ميلان", "AC Milan", "ميلان", "Juventus", "يوفنتوس",
            "Napoli", "نابولي", "Atalanta", "أتالانتا", "Borussia Dortmund", "بوروسيا دورتموند",
            "Bayer Leverkusen", "باير ليفركوزن", "Eintracht Frankfurt", "آينتراخت فرانكفورت",
            "Benfica", "بنفيكا", "Sporting CP", "سبورتينغ لشبونة", "Porto", "بورتو", "Ajax", "أياكس",
            "PSV Eindhoven", "آيندهوفن", "Olympiacos", "أولمبياكوس", "Club Brugge", "كلوب بروج",
            "Slavia Prague", "سلافيا براغ", "Monaco", "موناكو", "Marseille", "مارسيليا", "Galatasaray", "غلطة سراي",
            "FC Copenhagen", "كوبنهاغن", "Copenhagen", "كوبنهاغن", "Qarabag", "قره باغ", "Pafos", "بافوس",
            "Union Saint-Gilloise", "يونيون سان جيلوا", "Bodo/Glimt", "بودو غليمت", "Kairat Almaty", "كايرات ألماتي",
            "Spanish La Liga", "الدوري الإسباني", "UEFA Champions League", "دوري أبطال أوروبا",
            "Copa del Rey", "كأس الملك", "Spanish Copa del Rey", "كأس الملك",
            "Supercopa de Espana", "كأس السوبر الإسباني", "Spanish Super Cup", "كأس السوبر الإسباني",
            "UEFA Super Cup", "كأس السوبر الأوروبي", "FIFA Club World Cup", "كأس العالم للأندية",
            "Club Friendlies", "مباراة ودية", "Friendlies Clubs", "مباراة ودية"
        };
        for (int i = 0; i < kv.length; i += 2) AR.put(kv[i], kv[i + 1]);
    }

    static String ar(String s) {
        if (s == null) return "";
        String v = AR.get(s);
        return v != null ? v : s;
    }
}
