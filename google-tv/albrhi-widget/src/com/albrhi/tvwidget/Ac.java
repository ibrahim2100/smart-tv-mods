package com.albrhi.tvwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Air-conditioner commands through Alexa. Alexa has no public API for an app to drive a user's devices, so each
 * command is a Voice Monkey "Routine Trigger" that you tied to an Alexa routine (When: the trigger, Action: the AC).
 * Pressing a command here calls Voice Monkey's trigger API; Alexa runs the routine. Commands are one-way: nothing
 * is read back from the AC. The API token lives only in this app's private storage (never in the backup file).
 */
final class Ac {
    private Ac() { }

    private static final String API = "https://api-v3.voicemonkey.io/";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static final class Cmd {
        String id, name;
    }

    private static SharedPreferences prefs(Context c) { return c.getApplicationContext().getSharedPreferences("ac", Context.MODE_PRIVATE); }

    static boolean hasToken(Context c) { return prefs(c).getString("token", "").length() > 0; }

    static void setToken(Context c, String token) {
        String t = token == null ? "" : token.trim();
        SharedPreferences.Editor e = prefs(c).edit().putString("token", t);
        if (t.length() == 0) e.remove("cmds");
        e.apply();
    }

    /** Forgets the token and the command list. */
    static void disconnect(Context c) { prefs(c).edit().clear().apply(); }

    static List<Cmd> list(Context c) {
        List<Cmd> out = new ArrayList<Cmd>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("cmds", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Cmd m = new Cmd();
                m.id = o.getString("id");
                m.name = o.optString("name", m.id);
                out.add(m);
            }
        } catch (Throwable ignored) { }
        return out;
    }

    static String nameOf(Context c, String id) {
        for (Cmd m : list(c)) if (m.id.equals(id)) return m.name;
        return id;
    }

    // ---- network ----------------------------------------------------------------------------

    private static String call(Context c, String method, String path, String body) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(API + path).openConnection();
        try {
            h.setConnectTimeout(8000);
            h.setReadTimeout(12000);
            h.setRequestMethod(method);
            h.setRequestProperty("Authorization", "Bearer " + prefs(c).getString("token", ""));      // not in the URL: it would end up in logs
            if (body != null) {
                h.setRequestProperty("Content-Type", "application/json");
                h.setDoOutput(true);
                h.getOutputStream().write(body.getBytes("UTF-8"));
            }
            int code = h.getResponseCode();
            if (code >= 400) throw new Exception(describe(code));
            InputStream in = h.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[2048];
            int n;
            while ((n = in.read(b)) > 0) bo.write(b, 0, n);
            return bo.toString("UTF-8");
        } finally {
            h.disconnect();
        }
    }

    private static String describe(int code) {
        if (code == 401) return "الرمز غير صحيح أو انحذف";
        if (code == 404) return "الأمر انحذف من Voice Monkey";
        if (code == 429) return "تجاوزت الحد المسموح، انتظر قليلًا";
        return "رفض الخدمة (" + code + ")";
    }

    /**
     * Pulls the routine triggers from Voice Monkey, keeping the names you already have.
     * Blocking: call it from a background thread. Returns how many commands there are, or throws a readable message.
     */
    static int refreshSync(Context c) throws Exception {
        if (!hasToken(c)) throw new Exception("ما في رمز");
        JSONObject o = new JSONObject(call(c, "GET", "devices", null));
        JSONArray data = o.optJSONArray("data");
        JSONArray out = new JSONArray();
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                JSONObject d = data.getJSONObject(i);
                if (!"routines".equals(d.optString("capability"))) continue;
                out.put(new JSONObject().put("id", d.getString("id")).put("name", d.optString("name", d.getString("id"))));
            }
        }
        prefs(c).edit().putString("cmds", out.toString()).apply();
        return out.length();
    }

    /** Refreshes in the background, then runs {@code done} on the main thread with an empty message or the reason it failed. */
    static void refresh(final Context c, final Done done) {
        new Thread(new Runnable() { public void run() {
            String msg = "";
            try { refreshSync(c); } catch (Throwable t) { msg = readable(t); }
            final String m = msg;
            MAIN.post(new Runnable() { public void run() { if (done != null) done.onDone(m); } });
        } }, "ac-refresh").start();
    }

    interface Done { void onDone(String error); }

    private static final java.util.regex.Pattern DEGREES = java.util.regex.Pattern.compile("(?<![0-9])(1[6-9]|2[0-9]|30)(?![0-9])");
    private static final java.util.regex.Pattern OFF = java.util.regex.Pattern.compile("(?i)\\boff\\b|إطفاء|اطفاء|أطفئ|اطفي|إيقاف|ايقاف");
    private static final java.util.regex.Pattern ON = java.util.regex.Pattern.compile("(?i)\\bon\\b|تشغيل|شغّل|شغل");

    /** The commands are yours to name, so the card guesses what a command does from its name: on, off, a temperature, or something else. */
    private static void status(Context app, String name, int state, String error) {
        int icon = R.drawable.ic_snow, accent = WidgetView.GOLD;
        String working, done, detail = null, big = null;
        java.util.regex.Matcher deg = DEGREES.matcher(name);
        if (OFF.matcher(name).find()) {
            icon = R.drawable.ic_power; accent = 0xFFFF9A5C; working = "جاري إطفاء المكيف…"; done = "تم إطفاء المكيف";
        } else if (ON.matcher(name).find()) {
            icon = R.drawable.ic_power; accent = 0xFF5BD39A; working = "جاري تشغيل المكيف…"; done = "تم تشغيل المكيف";
        } else if (deg.find()) {
            accent = 0xFF62CBFF; working = "جاري ضبط الحرارة…"; done = "تم ضبط الحرارة";
            big = deg.group(1) + "°";
        } else {
            working = "جاري الإرسال…"; done = "تم الإرسال"; detail = name;
        }
        if (state == Notice.FAIL) Notice.show(app, state, icon, 0xFFFF6B6B, "المكيف", "تعذّر الإرسال", error, null);
        else Notice.show(app, state, icon, accent, "المكيف", state == Notice.WORKING ? working : done, detail, big);
    }

    /** Fires one command and shows a status card; "done" only means Voice Monkey accepted it (nothing is read back from the AC). */
    static void send(final Context c, final String id) {
        final Context app = c.getApplicationContext();
        final String name = nameOf(c, id);
        if (!hasToken(c)) { status(app, name, Notice.FAIL, "ادخل الرمز من صفحة الجوال"); return; }
        status(app, name, Notice.WORKING, null);
        new Thread(new Runnable() { public void run() {
            try {
                call(app, "POST", "trigger", new JSONObject().put("device", id).toString());
                status(app, name, Notice.DONE, null);
            } catch (Throwable t) {
                status(app, name, Notice.FAIL, readable(t));
            }
        } }, "ac-send").start();
    }

    private static String readable(Throwable t) {
        String m = t.getMessage();
        if (t instanceof java.net.UnknownHostException || t instanceof java.net.SocketTimeoutException || t instanceof java.net.ConnectException)
            return "لا يوجد اتصال بالإنترنت";
        return m == null || m.length() == 0 ? "تعذّر الإرسال" : m;
    }

}
