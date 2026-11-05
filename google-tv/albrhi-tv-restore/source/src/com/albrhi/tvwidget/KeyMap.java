package com.albrhi.tvwidget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Remote buttons you choose yourself: any key on any remote can open any app.
 * The accessibility service ({@link ControlService}) sees every key before the TV's own software does, so a mapped
 * key opens your app and the TV's default action (e.g. its built-in YouTube / Netflix launch) never happens.
 * Keys needed to use the TV (arrows, OK, Back, Home, Power, volume...) can never be mapped.
 */
final class KeyMap {
    private KeyMap() { }

    static final class Entry {
        int code;
        String pkg, name;
    }

    /** One installed app you can pick. */
    static final class AppRef {
        String pkg, label;
        Drawable icon;
    }

    interface Learn { void onKey(int keyCode); }

    /** Set while "press the button now" is on screen: the next mappable key goes here instead of being mapped. */
    static volatile Learn learning;

    private static volatile Map<Integer, Entry> cache;
    private static final Set<Integer> swallowUp = new HashSet<Integer>();

    /**
     * Some remotes (IR ones on Realtek TVs) send their app buttons as events with no Android key code at all
     * (code 0) and only a hardware scan code. Those are stored under SCAN_BASE + scan code.
     */
    static final int SCAN_BASE = 100000;

    /** The code a key is stored under: its Android key code, or SCAN_BASE + scan code when it has none. */
    static int codeOf(KeyEvent e) {
        int k = e.getKeyCode();
        return k == KeyEvent.KEYCODE_UNKNOWN && e.getScanCode() > 0 ? SCAN_BASE + e.getScanCode() : k;
    }

    private static final int[] PROTECTED = {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_TV_POWER, KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_SETTINGS,
            KeyEvent.KEYCODE_NOTIFICATION, KeyEvent.KEYCODE_SLEEP, KeyEvent.KEYCODE_WAKEUP, KeyEvent.KEYCODE_UNKNOWN};

    static boolean isProtected(int code) {
        for (int p : PROTECTED) if (p == code) return true;
        return false;
    }

    // ---- storage ----------------------------------------------------------------------------

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("keymap", Context.MODE_PRIVATE); }

    private static Map<Integer, Entry> load(Context c) {
        Map<Integer, Entry> m = cache;
        if (m != null) return m;
        m = new LinkedHashMap<Integer, Entry>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("map", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Entry e = new Entry();
                e.code = o.getInt("code");
                e.pkg = o.getString("pkg");
                e.name = o.optString("name", "");
                if (e.name.equals(oldDefault(e.code))) e.name = "";          // an automatic name from an earlier build: use the friendly one
                m.put(e.code, e);
            }
        } catch (Throwable ignored) { }
        cache = m;
        return m;
    }

    private static void save(Context c) {
        JSONArray a = new JSONArray();
        try {
            for (Entry e : load(c).values()) a.put(new JSONObject().put("code", e.code).put("pkg", e.pkg).put("name", e.name));
        } catch (Throwable ignored) { }
        prefs(c).edit().putString("map", a.toString()).apply();
        LayoutBackup.export(c);
    }

    static List<Entry> list(Context c) { return new ArrayList<Entry>(load(c).values()); }

    static Entry get(Context c, int code) { return load(c).get(code); }

    static void put(Context c, int code, String pkg, String name) {
        if (isProtected(code)) return;
        Entry e = load(c).get(code);
        if (e == null) { e = new Entry(); e.code = code; load(c).put(code, e); }
        e.pkg = pkg;
        if (name != null) e.name = name;
        else if (e.name == null) e.name = "";                 // empty = the automatic name
        save(c);
    }

    static void rename(Context c, int code, String name) {
        Entry e = load(c).get(code);
        if (e == null) return;
        e.name = name == null ? "" : name.trim();           // empty = back to the automatic name
        save(c);
    }

    static void remove(Context c, int code) { load(c).remove(code); save(c); }

    /** The backup file keeps your buttons, so restore.sh brings them back. */
    static String exportJson(Context c) { return prefs(c).getString("map", "[]"); }

    static void importJson(Context c, String json) {
        prefs(c).edit().putString("map", json == null ? "[]" : json).apply();
        cache = null;
    }

    private static String oldDefault(int code) {
        String n = KeyEvent.keyCodeToString(code);
        return "زر " + (n.startsWith("KEYCODE_") ? n.substring(8) : n);
    }

    /** A readable name for a key code (the TV's own names like STEM_1 mean nothing to most people). */
    static String defaultName(int code) {
        if (code >= SCAN_BASE) return "زر خاص (" + (code - SCAN_BASE) + ")";
        String n = KeyEvent.keyCodeToString(code);
        n = n.startsWith("KEYCODE_") ? n.substring(8) : n;
        if (n.startsWith("STEM_")) return "زر خاص " + n.substring(5);
        if (n.startsWith("TV_INPUT_")) return "زر الإدخال (" + code + ")";
        if (n.equals("PROG_RED")) return "الزر الأحمر";
        if (n.equals("PROG_GREEN")) return "الزر الأخضر";
        if (n.equals("PROG_YELLOW")) return "الزر الأصفر";
        if (n.equals("PROG_BLUE")) return "الزر الأزرق";
        if (n.length() == 1 && Character.isDigit(n.charAt(0))) return "الرقم " + n;
        if (n.startsWith("MEDIA_")) return "زر الوسائط " + n.substring(6).toLowerCase(java.util.Locale.US).replace('_', ' ');
        if (n.matches("\\d+")) return "زر (" + n + ")";
        return "زر " + n.toLowerCase(java.util.Locale.US).replace('_', ' ');
    }

    /** The name shown for a linked button: yours if you gave one, otherwise the automatic one. */
    static String titleOf(Entry e) { return e.name != null && e.name.length() > 0 ? e.name : defaultName(e.code); }

    // ---- handling a key (called by the accessibility service) -------------------------------

    /** @return true if the key was used here (the TV's own software must not see it). */
    static boolean onKey(Context c, KeyEvent e) {
        int code = codeOf(e);
        if (isProtected(code)) return false;
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
        if (!down) {                                              // the key-up of a key we used is swallowed too
            synchronized (swallowUp) { return swallowUp.remove(code); }
        }
        Learn l = learning;
        if (l != null) {
            learning = null;
            synchronized (swallowUp) { swallowUp.add(code); }
            l.onKey(code);
            return true;
        }
        Entry m = get(c, code);
        if (m == null) return false;
        synchronized (swallowUp) { swallowUp.add(code); }
        if (e.getRepeatCount() == 0) {
            if (isAc(m.pkg)) Ac.send(c, m.pkg.substring(3));
            else if (isLt(m.pkg)) Lights.runToken(c, m.pkg.substring(3));
            else if (ACT_SAVER.equals(m.pkg)) PreviewActivity.toggle(c);
            else if (isAct(m.pkg)) openRecents(c);
            else { arm(m.pkg); launch(c, m.pkg); }
        }
        return true;
    }

    /** Pops the open-apps panel up over whatever is on screen. */
    static void openRecents(Context c) {
        try {
            c.startActivity(new Intent(c, QuickSettingsActivity.class).putExtra("page", "recents")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } catch (Throwable ignored) { }
    }

    // ---- taking the screen back from the TV's own app launch -----------------------------------
    // On some TVs (e.g. Realtek ones) the system starts the button's own app from deep inside the framework, before our
    // key filter and whatever we do with the key. We can't stop that launch, but it lands a moment after ours, so for a
    // short while after a mapped button is pressed we bring our app back whenever another app takes the screen.

    private static volatile String armedPkg;
    private static volatile long armedUntil;
    private static volatile int armedTries;

    private static void arm(String pkg) {
        armedPkg = pkg;
        armedUntil = android.os.SystemClock.uptimeMillis() + 2500;
        armedTries = 0;
    }

    /** Called when another window comes to the front (accessibility service). */
    static void onWindow(Context c, String pkg) {
        String want = armedPkg;
        if (want == null || pkg == null) return;
        if (android.os.SystemClock.uptimeMillis() > armedUntil) { armedPkg = null; return; }
        if (pkg.equals(want) || pkg.equals(c.getPackageName()) || pkg.equals("com.android.systemui") || pkg.equals("android")) return;
        if (++armedTries > 3) { armedPkg = null; return; }
        launch(c, want);
    }

    // ---- apps -------------------------------------------------------------------------------

    /** Installed apps that have a TV launcher entry, sorted by name. */
    static List<AppRef> apps(Context c) {
        List<AppRef> out = new ArrayList<AppRef>();
        PackageManager pm = c.getPackageManager();
        Set<String> seen = new HashSet<String>();
        try {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(c.getPackageName()) || !seen.add(pkg)) continue;
                AppRef a = new AppRef();
                a.pkg = pkg;
                a.label = String.valueOf(ri.loadLabel(pm));
                a.icon = ri.loadIcon(pm);
                out.add(a);
            }
        } catch (Throwable ignored) { }
        Collections.sort(out, new Comparator<AppRef>() { public int compare(AppRef x, AppRef y) { return x.label.compareToIgnoreCase(y.label); } });
        return out;
    }

    /** A button can send an air-conditioner command instead of opening an app: its target is "ac:" + the command id. */
    static boolean isAc(String target) { return target != null && target.startsWith("ac:"); }

    /** "lt:<switch id>:<button>" flips a light button; "lt:r:<n>" runs a lights routine. */
    static boolean isLt(String target) { return target != null && target.startsWith("lt:"); }

    /** A button can also open one of our own screens: "act:recents" is the open-apps switcher. */
    static final String ACT_RECENTS = "act:recents";

    /** "act:saver" shows the screensaver right now. */
    static final String ACT_SAVER = "act:saver";

    static boolean isAct(String target) { return ACT_RECENTS.equals(target) || ACT_SAVER.equals(target); }

    /** "يفتح: Netflix" or "يرسل للمكيف: AC On". */
    static String actionText(Context c, String target) {
        if (isLt(target)) return "الإضاءة: " + Lights.nameOfToken(c, target.substring(3));
        return isAc(target) ? "يرسل للمكيف: " + Ac.nameOf(c, target.substring(3)) : "يفتح: " + labelOf(c, target);
    }

    static String labelOf(Context c, String pkg) {
        if (isAc(pkg)) return "المكيف · " + Ac.nameOf(c, pkg.substring(3));
        if (isLt(pkg)) return "الإضاءة · " + Lights.nameOfToken(c, pkg.substring(3));
        if (ACT_SAVER.equals(pkg)) return "شاشة الانتظار";
        if (isAct(pkg)) return "التطبيقات المفتوحة";
        try {
            PackageManager pm = c.getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (Throwable t) { return pkg; }
    }

    static Drawable iconOf(Context c, String pkg) {
        if (isAc(pkg) || isLt(pkg) || isAct(pkg)) return null;
        try { return c.getPackageManager().getApplicationIcon(pkg); } catch (Throwable t) { return null; }
    }

    static void launch(Context c, String pkg) {
        try {
            PackageManager pm = c.getPackageManager();
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setPackage(pkg);
            List<ResolveInfo> l = pm.queryIntentActivities(q, 0);
            Intent i = l.isEmpty() ? pm.getLaunchIntentForPackage(pkg)
                    : new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                            .setComponent(new ComponentName(pkg, l.get(0).activityInfo.name));
            if (i == null) { Toast.makeText(c, "التطبيق غير مثبّت", Toast.LENGTH_SHORT).show(); return; }
            c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));      // no RESET_TASK_IF_NEEDED: it would send an open app back to its first screen
        } catch (Throwable t) {
            Toast.makeText(c, "تعذّر فتح التطبيق", Toast.LENGTH_SHORT).show();
        }
    }
}
