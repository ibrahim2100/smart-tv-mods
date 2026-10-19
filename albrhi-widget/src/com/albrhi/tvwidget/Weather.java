package com.albrhi.tvwidget;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/** Current temperature + sky for the city the TV is in (found by IP), from Open-Meteo. No key needed. */
final class Weather {
    private Weather() { }

    /** e.g. "☀️ 27° صافي · الدمام", or null until the first download. */
    static volatile String text;
    private static long last;
    private static boolean busy;

    private static final Map<String, String> CITY = new HashMap<String, String>();
    static {
        String[] kv = {"Dammam", "الدمام", "Riyadh", "الرياض", "Jeddah", "جدة", "Mecca", "مكة المكرمة", "Makkah", "مكة المكرمة",
                "Medina", "المدينة المنورة", "Al Khobar", "الخبر", "Khobar", "الخبر", "Dhahran", "الظهران", "Tabuk", "تبوك",
                "Abha", "أبها", "Buraydah", "بريدة", "Hail", "حائل", "Jazan", "جازان", "Najran", "نجران", "Taif", "الطائف",
                "Yanbu", "ينبع", "Jubail", "الجبيل", "Al Hofuf", "الهفوف", "Hofuf", "الهفوف", "Qatif", "القطيف",
                "Al Qatif", "القطيف", "Khamis Mushait", "خميس مشيط", "Kuwait City", "مدينة الكويت", "Doha", "الدوحة",
                "Manama", "المنامة", "Dubai", "دبي", "Abu Dhabi", "أبوظبي", "Muscat", "مسقط"};
        for (int i = 0; i < kv.length; i += 2) CITY.put(kv[i], kv[i + 1]);
    }

    static void init(Context c) {
        if (text != null) return;
        SharedPreferences p = c.getApplicationContext().getSharedPreferences("weather", Context.MODE_PRIVATE);
        long t = p.getLong("t", 0);
        if (System.currentTimeMillis() - t < 6L * 60 * 60 * 1000) text = p.getString("text", null);
    }

    static void refresh(Context ctx) {
        final Context c = ctx.getApplicationContext();
        init(c);
        long now = System.currentTimeMillis();
        synchronized (Weather.class) {
            if (busy || now - last < 30L * 60 * 1000) return;
            busy = true;
            last = now;
        }
        new Thread(new Runnable() {
            public void run() {
                boolean ok = false;
                try { ok = fetch(c); } catch (Throwable ignored) { }
                synchronized (Weather.class) {
                    busy = false;
                    if (!ok) last = System.currentTimeMillis() - 25L * 60 * 1000;   // retry in ~5 minutes
                }
                if (ok) Fixtures.notifyUi();
            }
        }).start();
    }

    private static boolean fetch(Context c) throws Exception {
        SharedPreferences p = c.getSharedPreferences("weather", Context.MODE_PRIVATE);
        double lat = Double.longBitsToDouble(p.getLong("lat", 0));
        double lon = Double.longBitsToDouble(p.getLong("lon", 0));
        String city = p.getString("city", null);
        boolean geoFresh = city != null && System.currentTimeMillis() - p.getLong("geoT", 0) < 7L * 24 * 60 * 60 * 1000;
        if (!geoFresh) {
            String g = Fixtures.get("https://ipwho.is/");
            if (g == null) return false;
            JSONObject o = new JSONObject(g);
            if (!o.optBoolean("success")) return false;
            lat = o.getDouble("latitude");
            lon = o.getDouble("longitude");
            city = o.optString("city");
            p.edit().putLong("lat", Double.doubleToLongBits(lat)).putLong("lon", Double.doubleToLongBits(lon))
                    .putString("city", city).putLong("geoT", System.currentTimeMillis()).apply();
        }
        String w = Fixtures.get("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,weather_code,is_day&timezone=auto");
        if (w == null) return false;
        JSONObject cur = new JSONObject(w).getJSONObject("current");
        int temp = (int) Math.round(cur.getDouble("temperature_2m"));
        int code = cur.getInt("weather_code");
        boolean day = cur.optInt("is_day", 1) == 1;
        String ar = CITY.get(city);
        String t = icon(code, day) + " " + temp + "° " + desc(code) + (city.length() > 0 ? " · " + (ar != null ? ar : city) : "");
        text = t;
        p.edit().putString("text", t).putLong("t", System.currentTimeMillis()).apply();
        return true;
    }

    private static String icon(int c, boolean day) {
        if (c == 0) return day ? "☀️" : "🌙";
        if (c <= 2) return day ? "⛅" : "☁️";
        if (c == 3) return "☁️";
        if (c == 45 || c == 48) return "🌫️";
        if (c >= 51 && c <= 57) return "🌦️";
        if ((c >= 61 && c <= 67) || (c >= 80 && c <= 82)) return "🌧️";
        if ((c >= 71 && c <= 77) || c == 85 || c == 86) return "❄️";
        if (c >= 95) return "⛈️";
        return "🌡️";
    }

    private static String desc(int c) {
        if (c == 0) return "صافي";
        if (c == 1) return "صافي غالبًا";
        if (c == 2) return "غائم جزئيًا";
        if (c == 3) return "غائم";
        if (c == 45 || c == 48) return "ضباب";
        if (c >= 51 && c <= 57) return "رذاذ";
        if (c >= 61 && c <= 67) return "مطر";
        if ((c >= 71 && c <= 77) || c == 85 || c == 86) return "ثلج";
        if (c >= 80 && c <= 82) return "زخات مطر";
        if (c >= 95) return "عاصفة رعدية";
        return "";
    }
}
