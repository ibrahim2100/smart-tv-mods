package com.albrhi.tvwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Smart Life / Tuya light switches. Each press tries the LOCAL protocol first (fast, no internet, see {@link Tuya}),
 * and falls back to Tuya's cloud API when the switch cannot be reached on the LAN (for example behind a mesh node).
 * Devices, local keys and cloud credentials are imported once from a private file (files/import/lights.json, made on
 * the Mac by tools/tuya-keys.py) and then live only in this app's private storage: never in the backup, never in git.
 */
final class Lights {
    private Lights() { }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long DISCOVER_EVERY_MS = 60000L;

    static final class Gang { String dp, code, name; }

    static final class Dev { String id, name, key; List<Gang> gangs = new ArrayList<Gang>(); }

    static final class Action { String dev, dp; boolean on; }

    static final class Routine { String name; List<Action> actions = new ArrayList<Action>(); }

    /** An entry for the "remote button" picker: {@code token} is what gets stored in the key map after "lt:". */
    static final class Target { String token, title, sub; }

    interface Done { void onDone(String error); }

    // state shown in the panel, updated by refresh() and by every command
    private static final Map<String, Boolean> STATE = new HashMap<String, Boolean>();
    private static final Map<String, String> VIA = new HashMap<String, String>();
    private static final Map<String, String> IPS = new HashMap<String, String>();
    private static long lastDiscover = 0;
    private static String cloudToken = "";
    private static long cloudTokenUntil = 0;

    private static SharedPreferences prefs(Context c) { return c.getApplicationContext().getSharedPreferences("lights", Context.MODE_PRIVATE); }

    static boolean configured(Context c) { return devices(c).size() > 0; }

    static boolean hasCloud(Context c) { return prefs(c).getString("cid", "").length() > 0; }

    // ---- data ------------------------------------------------------------------------------------

    static List<Dev> devices(Context c) {
        List<Dev> out = new ArrayList<Dev>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("devs", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Dev d = new Dev();
                d.id = o.getString("id");
                d.name = o.optString("name", d.id);
                d.key = o.optString("local_key");
                JSONArray g = o.optJSONArray("gangs");
                for (int j = 0; g != null && j < g.length(); j++) {
                    JSONObject x = g.getJSONObject(j);
                    Gang n = new Gang();
                    n.dp = x.optString("dp", "1");
                    n.code = x.optString("code", "switch_" + n.dp);
                    n.name = x.optString("name", d.name + " " + n.dp);
                    d.gangs.add(n);
                }
                out.add(d);
            }
        } catch (Throwable ignored) { }
        return out;
    }

    private static Dev dev(Context c, String id) {
        for (Dev d : devices(c)) if (d.id.equals(id)) return d;
        return null;
    }

    private static Gang gang(Dev d, String dp) {
        if (d != null) for (Gang g : d.gangs) if (g.dp.equals(dp)) return g;
        return null;
    }

    static List<Routine> routines(Context c) {
        List<Routine> out = new ArrayList<Routine>();
        List<Dev> devs = devices(c);
        Routine off = new Routine(), on = new Routine();
        off.name = "إطفاء كل الإضاءة";
        on.name = "تشغيل كل الإضاءة";
        for (Dev d : devs) {
            for (Gang g : d.gangs) {
                Action a = new Action(); a.dev = d.id; a.dp = g.dp; a.on = false; off.actions.add(a);
                Action b = new Action(); b.dev = d.id; b.dp = g.dp; b.on = true; on.actions.add(b);
            }
        }
        if (!off.actions.isEmpty()) { out.add(off); out.add(on); }
        try {
            JSONArray a = new JSONArray(prefs(c).getString("routines", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Routine r = new Routine();
                r.name = o.optString("name", "روتين");
                JSONArray acts = o.optJSONArray("actions");
                for (int j = 0; acts != null && j < acts.length(); j++) {
                    JSONObject x = acts.getJSONObject(j);
                    Action ac = new Action();
                    ac.dev = x.getString("dev"); ac.dp = x.optString("dp", "1"); ac.on = x.optBoolean("on", true);
                    r.actions.add(ac);
                }
                if (!r.actions.isEmpty()) out.add(r);
            }
        } catch (Throwable ignored) { }
        return out;
    }

    /** True or false when known, null when this button's state has not been read yet. */
    static Boolean stateOf(String devId, String dp) { synchronized (STATE) { return STATE.get(devId + ":" + dp); } }

    static String viaOf(String devId) { synchronized (STATE) { return VIA.get(devId); } }

    private static void remember(String devId, String dp, boolean on, String via) {
        synchronized (STATE) { STATE.put(devId + ":" + dp, on); VIA.put(devId, via); }
    }

    // ---- import / forget -------------------------------------------------------------------------

    /** If lights.json was pushed into files/import/, store it privately, delete the file, and return true. */
    static boolean importPending(Context c) {
        try {
            File dir = c.getExternalFilesDir(null);
            if (dir == null) return false;
            File f = new File(new File(dir, "import"), "lights.json");
            if (!f.exists()) return false;
            InputStream in = new FileInputStream(f);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[4096];
            int n;
            while ((n = in.read(b)) > 0) bo.write(b, 0, n);
            in.close();
            JSONObject o = new JSONObject(bo.toString("UTF-8"));
            JSONObject cloud = o.optJSONObject("cloud");
            SharedPreferences.Editor e = prefs(c).edit();
            if (cloud != null) {
                e.putString("cid", cloud.optString("id")).putString("csecret", cloud.optString("secret"))
                        .putString("cbase", cloud.optString("base", "https://openapi.tuyaeu.com"));
            }
            e.putString("devs", o.optJSONArray("devices") == null ? "[]" : o.getJSONArray("devices").toString());
            if (o.optJSONArray("routines") != null) e.putString("routines", o.getJSONArray("routines").toString());
            e.apply();
            f.delete();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    static void disconnect(Context c) {
        prefs(c).edit().clear().apply();
        synchronized (STATE) { STATE.clear(); VIA.clear(); IPS.clear(); }
    }

    // ---- reaching a switch -----------------------------------------------------------------------

    private static void discoverIfStale(Context c) {
        if (System.currentTimeMillis() - lastDiscover < DISCOVER_EVERY_MS && !IPS.isEmpty()) return;
        lastDiscover = System.currentTimeMillis();
        Map<String, String> found = Tuya.discover(4500);
        synchronized (STATE) { IPS.putAll(found); }
    }

    private static String ipOf(Context c, Dev d) {
        synchronized (STATE) { String ip = IPS.get(d.id); if (ip != null) return ip; }
        discoverIfStale(c);
        synchronized (STATE) { return IPS.get(d.id); }
    }

    private static Map<String, Boolean> parseDps(String json) throws Exception {
        Map<String, Boolean> out = new HashMap<String, Boolean>();
        JSONObject dps = new JSONObject(json).optJSONObject("dps");
        if (dps != null) {
            java.util.Iterator<String> it = dps.keys();
            while (it.hasNext()) { String k = it.next(); Object v = dps.opt(k); if (v instanceof Boolean) out.put(k, (Boolean) v); }
        }
        return out;
    }

    private static String token(Context c) throws Exception {
        if (cloudToken.length() > 0 && System.currentTimeMillis() < cloudTokenUntil) return cloudToken;
        SharedPreferences p = prefs(c);
        String r = Tuya.cloud("GET", p.getString("cbase", ""), p.getString("cid", ""), p.getString("csecret", ""), "", "/v1.0/token?grant_type=1", null);
        JSONObject o = new JSONObject(r);
        if (!o.optBoolean("success")) throw new Exception("Tuya: " + o.optString("msg", "رفض الطلب"));
        cloudToken = o.getJSONObject("result").getString("access_token");
        cloudTokenUntil = System.currentTimeMillis() + 3600000L;
        return cloudToken;
    }

    private static String cloudCall(Context c, String method, String path, String body) throws Exception {
        if (!hasCloud(c)) throw new Exception("لا يوجد اتصال محلي ولا حساب سحابة");
        SharedPreferences p = prefs(c);
        return Tuya.cloud(method, p.getString("cbase", ""), p.getString("cid", ""), p.getString("csecret", ""), token(c), path, body);
    }

    /** Reads every button of one switch: local first, then the cloud. Returns "محلي" or "السحابة". Blocking. */
    private static String readDev(Context c, Dev d) throws Exception {
        String ip = ipOf(c, d);
        if (ip != null) {
            try {
                Map<String, Boolean> m = parseDps(Tuya.query(ip, d.id, d.key));
                for (Gang g : d.gangs) if (m.containsKey(g.dp)) remember(d.id, g.dp, m.get(g.dp), "محلي");
                return "محلي";
            } catch (Throwable ignored) { synchronized (STATE) { IPS.remove(d.id); } }
        }
        JSONObject o = new JSONObject(cloudCall(c, "GET", "/v1.0/devices/" + d.id + "/status", null));
        if (!o.optBoolean("success")) throw new Exception("Tuya: " + o.optString("msg", "رفض الطلب"));
        JSONArray a = o.getJSONArray("result");
        for (int i = 0; i < a.length(); i++) {
            JSONObject x = a.getJSONObject(i);
            for (Gang g : d.gangs) if (g.code.equals(x.optString("code")) && x.opt("value") instanceof Boolean) remember(d.id, g.dp, x.getBoolean("value"), "السحابة");
        }
        return "السحابة";
    }

    /** Sets one button. Returns how it got there ("محلي" or "السحابة"). Blocking. */
    private static String write(Context c, Dev d, Gang g, boolean on) throws Exception {
        String ip = ipOf(c, d);
        if (ip != null) {
            try {
                Tuya.control(ip, d.id, d.key, "{\"" + g.dp + "\":" + on + "}");
                remember(d.id, g.dp, on, "محلي");
                return "محلي";
            } catch (Throwable ignored) { synchronized (STATE) { IPS.remove(d.id); } }
        }
        String body = "{\"commands\":[{\"code\":\"" + g.code + "\",\"value\":" + on + "}]}";
        JSONObject o = new JSONObject(cloudCall(c, "POST", "/v1.0/devices/" + d.id + "/commands", body));
        if (!o.optBoolean("success")) throw new Exception("Tuya: " + o.optString("msg", "رفض الطلب"));
        remember(d.id, g.dp, on, "السحابة");
        return "السحابة";
    }

    // ---- public actions (all asynchronous) -------------------------------------------------------

    /** Re-reads every switch, then runs {@code done} on the main thread with "" or the first error. */
    static void refresh(final Context c, final Done done) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() { public void run() {
            String err = "";
            for (Dev d : devices(app)) {
                try { readDev(app, d); } catch (Throwable t) { if (err.length() == 0) err = readable(t, d.name); }
            }
            final String m = err;
            MAIN.post(new Runnable() { public void run() { if (done != null) done.onDone(m); } });
        } }, "lights-refresh").start();
    }

    /** value: TRUE = on, FALSE = off, null = flip. Shows a status card; calls {@code done} (may be null) at the end. */
    static void set(final Context c, final String devId, final String dp, final Boolean value, final Done done) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() { public void run() {
            Dev d = dev(app, devId);
            Gang g = gang(d, dp);
            if (g == null) { finish(app, done, g, value, null, "هذا الزر غير موجود"); return; }
            Notice.show(app, Notice.WORKING, R.drawable.ic_bulb, WidgetView.GOLD, "الإضاءة", g.name, "جاري الإرسال…", null);
            try {
                boolean target;
                if (value == null) {
                    Boolean cur = stateOf(devId, dp);
                    if (cur == null) { readDev(app, d); cur = stateOf(devId, dp); }
                    if (cur == null) throw new Exception("تعذّرت قراءة حالة الزر");
                    target = !cur;
                } else target = value;
                String via = write(app, d, g, target);
                finish(app, done, g, target, via, null);
            } catch (Throwable t) {
                finish(app, done, g, value, null, readable(t, g.name));
            }
        } }, "lights-set").start();
    }

    private static void finish(final Context app, final Done done, Gang g, Boolean on, String via, String error) {
        if (error != null) Notice.show(app, Notice.FAIL, R.drawable.ic_bulb, 0xFFFF6B6B, "الإضاءة", g == null ? "تعذّر الإرسال" : g.name, error, null);
        else Notice.show(app, Notice.DONE, R.drawable.ic_bulb, on ? WidgetView.GOLD : 0xFF9AA3B2, "الإضاءة", g.name, (on ? "شغّالة" : "مطفأة") + " · " + via, null);
        final String m = error == null ? "" : error;
        MAIN.post(new Runnable() { public void run() { if (done != null) done.onDone(m); } });
    }

    /** Runs a routine: every action at the same time (one thread each), one summary card at the end. */
    static void run(final Context c, final Routine r, final Done done) {
        final Context app = c.getApplicationContext();
        Notice.show(app, Notice.WORKING, R.drawable.ic_bulb, WidgetView.GOLD, "الإضاءة", r.name, "جاري التنفيذ…", null);
        new Thread(new Runnable() { public void run() {
            final List<String> errors = java.util.Collections.synchronizedList(new ArrayList<String>());
            List<Thread> ts = new ArrayList<Thread>();
            for (final Action a : r.actions) {
                Thread t = new Thread(new Runnable() { public void run() {
                    Dev d = dev(app, a.dev);
                    Gang g = gang(d, a.dp);
                    if (g == null) { errors.add("زر غير موجود"); return; }
                    try { write(app, d, g, a.on); } catch (Throwable x) { errors.add(readable(x, g.name)); }
                } }, "lights-routine");
                ts.add(t);
                t.start();
            }
            for (Thread t : ts) { try { t.join(20000); } catch (InterruptedException ignored) { } }
            final String err = errors.isEmpty() ? "" : errors.get(0);
            if (err.length() > 0) Notice.show(app, Notice.FAIL, R.drawable.ic_bulb, 0xFFFF6B6B, "الإضاءة", r.name, err, null);
            else Notice.show(app, Notice.DONE, R.drawable.ic_bulb, WidgetView.GOLD, "الإضاءة", r.name, "تم", null);
            MAIN.post(new Runnable() { public void run() { if (done != null) done.onDone(err); } });
        } }, "lights-run").start();
    }

    // ---- remote buttons --------------------------------------------------------------------------

    /** Everything a remote button can be linked to: each light button (toggle) and each routine. */
    static List<Target> targets(Context c) {
        List<Target> out = new ArrayList<Target>();
        for (Dev d : devices(c)) {
            for (Gang g : d.gangs) {
                Target t = new Target();
                t.token = d.id + ":" + g.dp;
                t.title = g.name;
                t.sub = "يقلب الإضاءة (تشغيل/إطفاء)";
                out.add(t);
                Target on = new Target();
                on.token = "on:" + d.id + ":" + g.dp;
                on.title = "تشغيل " + g.name;
                on.sub = "يشغّلها دائمًا";
                out.add(on);
                Target off = new Target();
                off.token = "off:" + d.id + ":" + g.dp;
                off.title = "إطفاء " + g.name;
                off.sub = "يطفئها دائمًا";
                out.add(off);
            }
        }
        List<Routine> rs = routines(c);
        for (int i = 0; i < rs.size(); i++) {
            Target t = new Target();
            t.token = "r:" + i;
            t.title = rs.get(i).name;
            t.sub = "روتين";
            out.add(t);
        }
        return out;
    }

    private static long lastScene = 0;

    /**
     * An HDMI input was opened from the launcher. If it is the PS5, run the routine whose name starts with "PS5"
     * (for example: everything off, only the small lights on). Ignored within 15 s of the last run.
     */
    static void onInputOpened(Context c, String label) {
        if (label == null || !label.trim().equalsIgnoreCase("PS5") || !configured(c)) return;
        if (System.currentTimeMillis() - lastScene < 15000L) return;
        for (Routine r : routines(c)) {
            if (r.name.startsWith("PS5")) { lastScene = System.currentTimeMillis(); run(c, r, null); return; }
        }
    }

    static String nameOfToken(Context c, String token) {
        for (Target t : targets(c)) if (t.token.equals(token)) return t.title;
        return token;
    }

    /** A key linked to "lt:..." was pressed. */
    static void runToken(Context c, String token) {
        if (token.startsWith("r:")) {
            try {
                List<Routine> rs = routines(c);
                run(c, rs.get(Integer.parseInt(token.substring(2))), null);
            } catch (Throwable ignored) { }
            return;
        }
        Boolean force = null;
        if (token.startsWith("on:")) { force = Boolean.TRUE; token = token.substring(3); }
        else if (token.startsWith("off:")) { force = Boolean.FALSE; token = token.substring(4); }
        int i = token.lastIndexOf(':');
        if (i > 0) set(c, token.substring(0, i), token.substring(i + 1), force, null);
    }

    private static String readable(Throwable t, String what) {
        if (t instanceof java.net.UnknownHostException || t instanceof java.net.SocketTimeoutException || t instanceof java.net.ConnectException)
            return "لا يوجد اتصال بالإنترنت ولا وصول محلي";
        String m = t.getMessage();
        return m == null || m.length() == 0 ? "تعذّر الوصول لـ" + what : m;
    }
}
