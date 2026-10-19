package com.albrhi.tvwidget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * The TV's small HTTP API, used by the phone page now and the phone app later.
 *
 * Safety rules, all enforced here:
 *  - only devices on the local network can connect;
 *  - everything except the page itself, /api/ping and /api/pair needs a token;
 *  - a token is only handed out against a 6-digit PIN that the TV shows while its pairing screen is open
 *    (single use, 10 minutes, 5 wrong tries kill it);
 *  - installing or removing an app still has to be confirmed on the TV with the remote.
 */
final class RemoteServer {
    static final int PORT = 8765;
    private static final long MAX_UPLOAD = 700L * 1024 * 1024;
    private static final long PIN_MS = 10L * 60 * 1000;

    private static RemoteServer inst;

    static synchronized RemoteServer get(Context c) {
        if (inst == null) inst = new RemoteServer(c.getApplicationContext());
        return inst;
    }

    private final Context ctx;
    private final SecureRandom rnd = new SecureRandom();
    private ServerSocket server;
    private ExecutorService pool;
    private volatile boolean running;
    private NsdManager.RegistrationListener nsdListener;

    private volatile String pin;
    private volatile long pinExpiry;
    private int pinFailures;
    /** What the phone page shows under "status" (updated by the installer). */
    volatile String status = "جاهز";

    private RemoteServer(Context c) { ctx = c; }

    // ---- lifecycle --------------------------------------------------------------------------

    synchronized void start() {
        if (running) return;
        running = true;
        pool = Executors.newCachedThreadPool();
        new Thread(new Runnable() {
            public void run() { acceptLoop(); }
        }, "albrhi-remote").start();
        registerNsd();
    }

    synchronized void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (IOException ignored) { }
        if (pool != null) pool.shutdownNow();
        unregisterNsd();
    }

    private void acceptLoop() {
        try {
            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(PORT));
            while (running) {
                final Socket s = server.accept();
                pool.execute(new Runnable() { public void run() { handle(s); } });
            }
        } catch (IOException e) {
            running = false;     // port busy or closed
        }
    }

    private void registerNsd() {
        try {
            NsdManager nsd = (NsdManager) ctx.getSystemService(Context.NSD_SERVICE);
            NsdServiceInfo info = new NsdServiceInfo();
            info.setServiceName("AlbrhiTV");
            info.setServiceType("_albrhitv._tcp");
            info.setPort(PORT);
            nsdListener = new NsdManager.RegistrationListener() {
                public void onServiceRegistered(NsdServiceInfo i) { }
                public void onRegistrationFailed(NsdServiceInfo i, int e) { }
                public void onServiceUnregistered(NsdServiceInfo i) { }
                public void onUnregistrationFailed(NsdServiceInfo i, int e) { }
            };
            nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, nsdListener);
        } catch (Throwable ignored) { }
    }

    private void unregisterNsd() {
        try {
            if (nsdListener != null) {
                ((NsdManager) ctx.getSystemService(Context.NSD_SERVICE)).unregisterService(nsdListener);
                nsdListener = null;
            }
        } catch (Throwable ignored) { }
    }

    // ---- pairing ----------------------------------------------------------------------------

    /** Called when the pairing screen opens on the TV. Returns the PIN to display. */
    synchronized String startPairing() {
        pin = String.format(java.util.Locale.US, "%06d", rnd.nextInt(1000000));
        pinExpiry = SystemClock.elapsedRealtime() + PIN_MS;
        pinFailures = 0;
        return pin;
    }

    synchronized void stopPairing() { pin = null; }

    synchronized String currentPin() {
        return pin != null && SystemClock.elapsedRealtime() < pinExpiry ? pin : null;
    }

    private synchronized String tryPair(String given, String deviceName) {
        String p = currentPin();
        if (p == null || pinFailures >= 5) return null;
        boolean ok = given != null && MessageDigest.isEqual(p.getBytes(), given.trim().getBytes());
        if (!ok) {
            if (++pinFailures >= 5) pin = null;
            return null;
        }
        pin = null;                                  // single use
        byte[] b = new byte[24];
        rnd.nextBytes(b);
        StringBuilder t = new StringBuilder();
        for (byte x : b) t.append(String.format("%02x", x));
        try {
            JSONObject all = tokens();
            all.put(t.toString(), new JSONObject().put("name", deviceName == null ? "" : deviceName).put("time", System.currentTimeMillis()));
            prefs().edit().putString("tokens", all.toString()).apply();
        } catch (Throwable ignored) { }
        status = "تم ربط جهاز جديد";
        return t.toString();
    }

    private SharedPreferences prefs() { return ctx.getSharedPreferences("remote", Context.MODE_PRIVATE); }

    private JSONObject tokens() {
        try { return new JSONObject(prefs().getString("tokens", "{}")); } catch (Throwable t) { return new JSONObject(); }
    }

    synchronized boolean valid(String token) {
        if (token == null || token.length() < 20) return false;
        JSONObject all = tokens();
        Iterator<String> it = all.keys();
        boolean found = false;
        while (it.hasNext()) {                         // compare every key: no early exit on a partial match
            if (MessageDigest.isEqual(it.next().getBytes(), token.getBytes())) found = true;
        }
        return found;
    }

    int pairedCount() { return tokens().length(); }

    String[] pairedNames() {
        JSONObject all = tokens();
        String[] out = new String[all.length()];
        int i = 0;
        Iterator<String> it = all.keys();
        while (it.hasNext()) out[i++] = all.optJSONObject(it.next()).optString("name", "");
        return out;
    }

    synchronized void revokeAll() { prefs().edit().remove("tokens").apply(); }

    // ---- http -------------------------------------------------------------------------------

    private void handle(Socket s) {
        try {
            s.setSoTimeout(60000);
            if (!isLocal(s.getInetAddress())) { s.close(); return; }
            InputStream in = new BufferedInputStream(s.getInputStream(), 16384);
            OutputStream out = s.getOutputStream();
            String head = readHead(in);
            if (head == null) return;
            String[] lines = head.split("\r\n");
            String[] rl = lines[0].split(" ");
            if (rl.length < 2) return;
            String method = rl[0], target = rl[1];
            Map<String, String> h = new HashMap<String, String>();
            for (int i = 1; i < lines.length; i++) {
                int c = lines[i].indexOf(':');
                if (c > 0) h.put(lines[i].substring(0, c).trim().toLowerCase(java.util.Locale.ROOT), lines[i].substring(c + 1).trim());
            }
            String path = target, query = "";
            int q = target.indexOf('?');
            if (q >= 0) { path = target.substring(0, q); query = target.substring(q + 1); }
            long len = 0;
            try { len = Long.parseLong(h.containsKey("content-length") ? h.get("content-length") : "0"); } catch (NumberFormatException ignored) { }
            if ("100-continue".equalsIgnoreCase(h.get("expect"))) { out.write("HTTP/1.1 100 Continue\r\n\r\n".getBytes()); out.flush(); }
            route(method, path, query, h, len, in, out);
            out.flush();
        } catch (Throwable ignored) {
        } finally {
            try { s.close(); } catch (IOException ignored) { }
        }
    }

    private static boolean isLocal(InetAddress a) {
        return a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress();
    }

    private static String readHead(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int c, state = 0;
        while ((c = in.read()) >= 0) {
            b.write(c);
            if (b.size() > 16384) return null;
            if (c == '\r' && (state == 0 || state == 2)) state++;
            else if (c == '\n' && (state == 1 || state == 3)) { state++; if (state == 4) break; }
            else state = 0;
        }
        return state == 4 ? new String(b.toByteArray(), "UTF-8") : null;
    }

    private static String param(String query, String key) {
        for (String kv : query.split("&")) {
            int e = kv.indexOf('=');
            if (e > 0 && kv.substring(0, e).equals(key)) {
                try { return URLDecoder.decode(kv.substring(e + 1), "UTF-8"); } catch (Exception ignored) { }
            }
        }
        return null;
    }

    private static byte[] readBody(InputStream in, long len, int max) throws IOException {
        if (len < 0 || len > max) throw new IOException("body too large");
        byte[] b = new byte[(int) len];
        int off = 0;
        while (off < b.length) {
            int n = in.read(b, off, b.length - off);
            if (n < 0) throw new IOException("short body");
            off += n;
        }
        return b;
    }

    private static JSONObject jsonBody(InputStream in, long len) {
        try { return new JSONObject(new String(readBody(in, len, 8192), "UTF-8")); } catch (Throwable t) { return new JSONObject(); }
    }

    private void send(OutputStream out, int code, String type, byte[] body) throws IOException {
        String msg = code == 200 ? "OK" : code == 400 ? "Bad Request" : code == 401 ? "Unauthorized" : code == 403 ? "Forbidden"
                : code == 404 ? "Not Found" : code == 409 ? "Conflict" : code == 413 ? "Payload Too Large" : code == 507 ? "Insufficient Storage" : "Error";
        out.write(("HTTP/1.1 " + code + " " + msg + "\r\nContent-Type: " + type + "\r\nContent-Length: " + body.length
                + "\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n").getBytes());
        out.write(body);
    }

    private void json(OutputStream out, int code, Object o) throws IOException {
        send(out, code, "application/json; charset=utf-8", o.toString().getBytes("UTF-8"));
    }

    private void err(OutputStream out, int code, String message) throws IOException {
        try { json(out, code, new JSONObject().put("ok", false).put("error", message)); } catch (Exception e) { throw new IOException(e); }
    }

    private void route(String method, String path, String query, Map<String, String> h, long len, InputStream in, OutputStream out)
            throws Exception {
        if (method.equals("GET") && (path.equals("/") || path.equals("/index.html"))) {
            InputStream page = ctx.getResources().openRawResource(R.raw.remote);
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = page.read(buf)) > 0) b.write(buf, 0, n);
            send(out, 200, "text/html; charset=utf-8", b.toByteArray());
            return;
        }
        if (method.equals("GET") && path.equals("/api/ping")) {
            json(out, 200, new JSONObject().put("ok", true).put("name", tvName()).put("model", Build.MODEL));
            return;
        }
        if (method.equals("POST") && path.equals("/api/pair")) {
            JSONObject b = jsonBody(in, len);
            String token = tryPair(b.optString("pin"), b.optString("name"));
            if (token == null) {
                Thread.sleep(700);                                   // slow down guessing
                err(out, 403, currentPin() == null ? "افتح شاشة الربط على التلفزيون أول" : "الرمز غير صحيح");
            } else {
                json(out, 200, new JSONObject().put("ok", true).put("token", token).put("name", tvName()));
            }
            return;
        }
        if (!path.startsWith("/api/")) { err(out, 404, "not found"); return; }
        if (!valid(h.get("x-token"))) { err(out, 401, "غير مصرّح، اربط الجهاز أول"); return; }

        if (method.equals("GET") && path.equals("/api/info")) { json(out, 200, info()); return; }
        if (method.equals("GET") && path.equals("/api/status")) {
            json(out, 200, new JSONObject().put("status", status));
            return;
        }
        if (method.equals("GET") && path.equals("/api/apps")) { json(out, 200, apps()); return; }
        if (method.equals("POST") && path.equals("/api/launch")) {
            String pkg = jsonBody(in, len).optString("pkg");
            Intent i = ctx.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) { err(out, 404, "التطبيق غير موجود"); return; }
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            json(out, 200, new JSONObject().put("ok", true));
            return;
        }
        if (method.equals("POST") && path.equals("/api/uninstall")) {
            String pkg = jsonBody(in, len).optString("pkg");
            if (pkg.length() == 0 || pkg.equals(ctx.getPackageName())) { err(out, 400, "لا يمكن حذف هذا التطبيق"); return; }
            Installer.uninstall(ctx, pkg);
            json(out, 200, new JSONObject().put("ok", true).put("message", "أكّد الحذف على التلفزيون بالريموت"));
            return;
        }
        if (method.equals("GET") && path.equals("/api/control")) {
            json(out, 200, new JSONObject().put("ready", ControlService.ready()));
            return;
        }
        if (method.equals("POST") && path.equals("/api/key")) { key(jsonBody(in, len).optString("key"), out); return; }
        if (method.equals("POST") && path.equals("/api/text")) {
            ControlService cs = ControlService.get();
            if (cs == null) { err(out, 409, "خدمة التحكم غير مفعّلة على التلفزيون"); return; }
            boolean ok = cs.text(jsonBody(in, len).optString("text"));
            json(out, 200, new JSONObject().put("ok", ok).put("message", ok ? "" : "اضغط على خانة الكتابة بالتلفزيون أول"));
            return;
        }
        if (method.equals("GET") && path.equals("/api/volume")) {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            json(out, 200, new JSONObject().put("level", am.getStreamVolume(AudioManager.STREAM_MUSIC))
                    .put("max", am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).put("muted", am.isStreamMute(AudioManager.STREAM_MUSIC)));
            return;
        }
        if (method.equals("POST") && path.equals("/api/volume")) {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            int lv = Math.max(0, Math.min(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), jsonBody(in, len).optInt("level", -1)));
            am.setStreamVolume(AudioManager.STREAM_MUSIC, lv, AudioManager.FLAG_SHOW_UI);
            json(out, 200, new JSONObject().put("ok", true).put("level", lv));
            return;
        }
        if (method.equals("POST") && path.equals("/api/power")) { power(jsonBody(in, len).optString("action"), out); return; }
        if (method.equals("POST") && path.equals("/api/open-url")) { openUrl(jsonBody(in, len).optString("url"), out); return; }
        if (method.equals("POST") && path.equals("/api/settings")) {
            JSONObject b = jsonBody(in, len);
            if (b.has("sportsdbKey")) Fixtures.setKey(ctx, b.optString("sportsdbKey"));
            json(out, 200, new JSONObject().put("ok", true));
            return;
        }
        if (method.equals("POST") && path.equals("/api/install")) { upload(query, len, in, out); return; }
        if (method.equals("POST") && path.equals("/api/install-url")) { download(jsonBody(in, len).optString("url"), out); return; }
        err(out, 404, "not found");
    }

    // ---- control ----------------------------------------------------------------------------

    private void media(int code) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    private void key(String k, OutputStream out) throws Exception {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        boolean ok = true;
        if ("volup".equals(k)) am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        else if ("voldown".equals(k)) am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        else if ("mute".equals(k)) am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI);
        else if ("playpause".equals(k)) media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        else if ("play".equals(k)) media(KeyEvent.KEYCODE_MEDIA_PLAY);
        else if ("pause".equals(k)) media(KeyEvent.KEYCODE_MEDIA_PAUSE);
        else if ("next".equals(k)) media(KeyEvent.KEYCODE_MEDIA_NEXT);
        else if ("prev".equals(k)) media(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        else if ("ff".equals(k)) media(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
        else if ("rw".equals(k)) media(KeyEvent.KEYCODE_MEDIA_REWIND);
        else {
            ControlService cs = ControlService.get();
            if (cs == null) { err(out, 409, "خدمة التحكم غير مفعّلة على التلفزيون"); return; }
            int d = ControlService.direction(k);
            if (d != 0) ok = cs.move(d);
            else if ("ok".equals(k)) ok = cs.click();
            else if ("back".equals(k)) ok = cs.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
            else if ("home".equals(k)) ok = cs.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
            else if ("recents".equals(k)) ok = cs.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS);
            else { err(out, 400, "زر غير معروف"); return; }
        }
        json(out, 200, new JSONObject().put("ok", ok));
    }

    private void power(String action, OutputStream out) throws Exception {
        if ("sleep".equals(action)) {
            ControlService cs = ControlService.get();
            if (cs == null) { err(out, 409, "خدمة التحكم غير مفعّلة على التلفزيون"); return; }
            boolean ok = cs.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
            json(out, 200, new JSONObject().put("ok", ok));
        } else if ("wake".equals(action)) {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, "albrhi:wake");
            try {
                wl.acquire(3000);
                json(out, 200, new JSONObject().put("ok", true));
            } catch (Throwable t) {
                err(out, 500, "ما قدر يوقظ الشاشة");
            }
        } else {
            err(out, 400, "أمر غير معروف");
        }
    }

    private void openUrl(String url, OutputStream out) throws Exception {
        if (!(url.startsWith("https://") || url.startsWith("http://"))) { err(out, 400, "الرابط لازم يبدأ بـ http أو https"); return; }
        Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url), "video/*").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(new Intent(i).setPackage("org.videolan.vlc"));          // VLC plays almost anything
        } catch (Throwable t) {
            try { ctx.startActivity(i); } catch (Throwable t2) { err(out, 404, "ما فيه تطبيق يفتح هذا الرابط"); return; }
        }
        json(out, 200, new JSONObject().put("ok", true));
    }

    // ---- install ----------------------------------------------------------------------------

    private File incoming() { return new File(ctx.getCacheDir(), "incoming.apk"); }

    private void upload(String query, long len, InputStream in, OutputStream out) throws Exception {
        if (len <= 0) { err(out, 400, "لا يوجد ملف"); return; }
        if (len > MAX_UPLOAD) { drain(in, len); err(out, 413, "الملف كبير جدًا (الحد 700 ميغا)"); return; }
        StatFs fs = new StatFs(ctx.getCacheDir().getPath());
        if (fs.getAvailableBytes() < len * 2) { err(out, 507, "مساحة التلفزيون ما تكفي"); return; }
        String name = param(query, "name");
        status = "جاري استقبال " + (name == null ? "الملف" : name) + "…";
        File f = incoming();
        FileOutputStream fo = new FileOutputStream(f);
        try {
            byte[] buf = new byte[65536];
            long left = len;
            while (left > 0) {
                int n = in.read(buf, 0, (int) Math.min(buf.length, left));
                if (n < 0) throw new IOException("connection closed");
                fo.write(buf, 0, n);
                left -= n;
            }
        } finally { fo.close(); }
        finishInstall(f, out);
    }

    private void download(String url, OutputStream out) throws Exception {
        if (!url.startsWith("https://")) { err(out, 400, "الرابط لازم يبدأ بـ https"); return; }
        status = "جاري تنزيل الملف من الرابط…";
        File f = incoming();
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        try {
            if (c.getResponseCode() != 200) { err(out, 400, "الرابط ما رجّع ملف (" + c.getResponseCode() + ")"); return; }
            long declared = c.getContentLengthLong();
            if (declared > MAX_UPLOAD) { err(out, 413, "الملف كبير جدًا (الحد 700 ميغا)"); return; }
            InputStream is = c.getInputStream();
            FileOutputStream fo = new FileOutputStream(f);
            try {
                byte[] buf = new byte[65536];
                long total = 0;
                int n;
                while ((n = is.read(buf)) > 0) {
                    total += n;
                    if (total > MAX_UPLOAD) { err(out, 413, "الملف كبير جدًا (الحد 700 ميغا)"); return; }
                    fo.write(buf, 0, n);
                }
            } finally { fo.close(); is.close(); }
        } finally { c.disconnect(); }
        finishInstall(f, out);
    }

    private void finishInstall(File f, OutputStream out) throws Exception {
        PackageManager pm = ctx.getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(f.getAbsolutePath(), 0);
        if (pi == null) { f.delete(); status = "الملف مو APK صالح"; err(out, 400, "هذا ما هو ملف APK صالح"); return; }
        pi.applicationInfo.sourceDir = f.getAbsolutePath();
        pi.applicationInfo.publicSourceDir = f.getAbsolutePath();
        CharSequence label = pi.applicationInfo.loadLabel(pm);
        String shown = (label == null ? pi.packageName : label.toString());
        if (!ctx.getPackageManager().canRequestPackageInstalls()) {
            status = "فعّل لتطبيقنا: تثبيت من مصادر غير معروفة";
            err(out, 403, "التلفزيون يمنع التثبيت من هذا التطبيق (شغّل restore.sh أو فعّل الخيار من الإعدادات)");
            return;
        }
        status = "بانتظار تأكيدك على التلفزيون: " + shown;
        Installer.install(ctx, f, shown);
        json(out, 200, new JSONObject().put("ok", true).put("package", pi.packageName).put("label", shown)
                .put("message", "أكّد التثبيت على التلفزيون بالريموت"));
    }

    private static void drain(InputStream in, long len) throws IOException {
        byte[] buf = new byte[65536];
        while (len > 0) {
            int n = in.read(buf, 0, (int) Math.min(buf.length, len));
            if (n < 0) return;
            len -= n;
        }
    }

    // ---- info -------------------------------------------------------------------------------

    String tvName() {
        String n = Settings.Global.getString(ctx.getContentResolver(), "device_name");
        return n == null || n.length() == 0 ? Build.MODEL : n;
    }

    static String localIp() {
        try {
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements(); ) {
                NetworkInterface ni = e.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (Enumeration<InetAddress> a = ni.getInetAddresses(); a.hasMoreElements(); ) {
                    InetAddress ad = a.nextElement();
                    if (ad instanceof java.net.Inet4Address && !ad.isLoopbackAddress()) return ad.getHostAddress();
                }
            }
        } catch (Throwable ignored) { }
        return "—";
    }

    private JSONObject info() throws Exception {
        Health.Info hi = Health.read(ctx);
        return new JSONObject().put("name", tvName()).put("model", Build.MODEL).put("android", Build.VERSION.RELEASE)
                .put("ip", localIp()).put("uptimeMin", SystemClock.elapsedRealtime() / 60000)
                .put("ramFreeMb", hi.ramFreeMb).put("ramTotalMb", hi.ramTotalMb)
                .put("storageFreeMb", hi.storageFreeMb).put("storageTotalMb", hi.storageTotalMb);
    }

    private JSONArray apps() throws Exception {
        PackageManager pm = ctx.getPackageManager();
        SharedPreferences l = ctx.getSharedPreferences("layout", Context.MODE_PRIVATE);
        java.util.Set<String> hidden = new java.util.HashSet<String>(java.util.Arrays.asList(l.getString("hidden", "").split(",")));
        Map<String, JSONObject> byPkg = new java.util.TreeMap<String, JSONObject>();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            for (ResolveInfo ri : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(ctx.getPackageName()) || byPkg.containsKey(pkg)) continue;
                boolean sys = (ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                byPkg.put(pkg, new JSONObject().put("pkg", pkg).put("label", String.valueOf(ri.loadLabel(pm)))
                        .put("system", sys).put("hidden", hidden.contains(pkg)).put("tv", cat.equals(Intent.CATEGORY_LEANBACK_LAUNCHER)));
            }
        }
        JSONArray a = new JSONArray();
        for (JSONObject o : byPkg.values()) a.put(o);
        return a;
    }
}
