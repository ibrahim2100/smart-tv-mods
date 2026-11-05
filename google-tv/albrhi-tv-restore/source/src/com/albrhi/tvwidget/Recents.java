package com.albrhi.tvwidget;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The apps you opened recently, like a phone's app switcher: which are still open in the background and which are
 * only remembered. Android has no public API for another app's tasks, so this reads the activity manager's own report
 * (`dumpsys activity`), which needs the DUMP permission once: `adb shell pm grant com.albrhi.tvwidget android.permission.DUMP`
 * (restore.sh does it). Without it the list is simply empty and the panel says so.
 */
final class Recents {
    private Recents() { }

    static final class Entry {
        String pkg, label;
        Drawable icon;
        boolean running;           // its process is alive right now
        int taskId = -1;           // the system's id of its task, to bring exactly that screen back
    }

    static final class Result {
        final List<Entry> apps = new ArrayList<Entry>();
        boolean allowed = true;    // false when the system refused to tell us (DUMP not granted)
    }

    interface Done { void onDone(Result r); }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // "Recent #3: TaskRecord{... A=com.pkg U=0 StackId=11 sz=1}" (Android 10) and
    // "Recent #3: Task{... type=standard ... A=10094:com.pkg U=0 StackId=1269 sz=1}" (Android 11)
    private static final Pattern TASK = Pattern.compile("A=(?:\\d+:)?(?:android:)?([A-Za-z0-9_.]+)(?:/\\S*)? U=\\d+ StackId=-?\\d+ sz=(\\d+)");
    // Android 14: "Recent #1: Task{8640829 #71 type=standard A=10044:com.android.vending}" (no sz / StackId any more)
    private static final Pattern TASK14 = Pattern.compile("type=(\\w+) A=(?:\\d+:)?(?:android:)?([A-Za-z0-9_.]+)");
    private static final Pattern TASK_ID = Pattern.compile("(?:TaskRecord|Task)\\{[0-9a-f]+ #(\\d+)");
    private static final Pattern PROC = Pattern.compile("ProcessRecord\\{[0-9a-f]+ \\d+:([A-Za-z0-9_.]+)[:/]");

    /** Reads the list on a background thread and calls back on the main thread. */
    static void load(final Context c, final Done done) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() { public void run() {
            Result r = new Result();
            try { read(app, r); } catch (Throwable ignored) { }
            final Result fr = r;
            MAIN.post(new Runnable() { public void run() { done.onDone(fr); } });
        } }, "recents").start();
    }

    private static void read(Context c, Result r) {
        String recents = dump("recents");
        if (recents.length() == 0 || recents.contains("Permission Denial")) { r.allowed = false; return; }
        Set<String> alive = new HashSet<String>();
        String procs = dump("processes");
        Matcher pm = PROC.matcher(procs);
        while (pm.find()) alive.add(pm.group(1));

        Map<String, KeyMap.AppRef> installed = new HashMap<String, KeyMap.AppRef>();
        for (KeyMap.AppRef a : KeyMap.apps(c)) installed.put(a.pkg, a);

        Set<String> seen = new HashSet<String>();
        for (String line : recents.split("\n")) {
            if (!line.contains("Recent #") || line.contains("type=home") || line.contains("type=dream")) continue;
            String pkg;
            Matcher m = TASK.matcher(line);
            if (m.find()) {
                pkg = m.group(1);
                if (Integer.parseInt(m.group(2)) == 0) continue;                    // a task with no activity left is only a ghost
            } else {
                Matcher m14 = TASK14.matcher(line);
                if (!m14.find() || !"standard".equals(m14.group(1))) continue;      // Android 14 does not say how many activities a task has
                pkg = m14.group(2);
            }
            if (!seen.add(pkg)) continue;
            KeyMap.AppRef ref = installed.get(pkg);
            if (ref == null) continue;                                                // only apps you can open from the home screen
            Entry e = new Entry();
            e.pkg = pkg;
            e.label = ref.label;
            e.icon = ref.icon;
            e.running = alive.contains(pkg);
            Matcher tm = TASK_ID.matcher(line);
            if (tm.find()) e.taskId = Integer.parseInt(tm.group(1));
            r.apps.add(e);
            if (r.apps.size() >= 12) break;
        }
    }

    private static String dump(String what) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"dumpsys", "activity", what});
            InputStream in = p.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0 && bo.size() < 4 * 1024 * 1024) bo.write(b, 0, n);
            p.destroy();
            return bo.toString("UTF-8");
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * Brings the app back exactly as you left it: the system moves its existing task to the front (no new launch, so the
     * movie you were in is still there). If that is not possible it falls back to opening the app normally.
     */
    static void open(Context c, Entry e) {
        if (e.taskId >= 0) {
            try {
                ((ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE)).moveTaskToFront(e.taskId, 0);
                return;
            } catch (Throwable ignored) { }
        }
        KeyMap.launch(c, e.pkg);
    }

    /**
     * Opens an app the way a phone does: if it already has a task (open or remembered) that exact screen comes back,
     * otherwise it launches normally. Launching an open app through its launcher icon can send it back to its first screen.
     */
    static void openOrLaunch(final Context c, final String pkg, final Check here) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() { public void run() {
            int id = -1;
            try { id = findTask(app, pkg); } catch (Throwable ignored) { }
            final int tid = id;
            MAIN.post(new Runnable() { public void run() {
                if (tid < 0) KeyMap.launch(app, pkg); else toFront(app, tid, pkg, here, 0);
            } });
        } }, "recents-open").start();
    }

    /** Tells whether the screen that asked is still the one in front, i.e. the request to bring the task forward did not work (yet). */
    interface Check {
        boolean stillHere();
        /** True for a few seconds after the Home button: that is when Android may refuse the request, so waiting and retrying makes sense. */
        boolean justHome();
    }

    /**
     * Android refuses "bring this task forward" from an app for about five seconds after the Home button was pressed
     * (it silently logs "Task to front request ... stopped"), so if we are still in front a moment later we try again,
     * and after a few tries fall back to an ordinary launch.
     */
    private static void toFront(final Context app, final int tid, final String pkg, final Check here, final int attempt) {
        try {
            ((ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE)).moveTaskToFront(tid, 0);
        } catch (Throwable t) { KeyMap.launch(app, pkg); return; }
        if (here == null) return;
        MAIN.postDelayed(new Runnable() { public void run() {
            if (!here.stillHere()) return;                                   // it came to the front
            // Right after Home Android may be blocking the request (retry for a while); otherwise the task simply cannot be brought forward (a remembered task with no screen left): open the app normally, at once
            if (attempt >= (here.justHome() ? 7 : 1)) KeyMap.launch(app, pkg);
            else toFront(app, tid, pkg, here, attempt + 1);
        } }, 450);
    }

    private static int findTask(Context c, String pkg) {
        String recents = dump("recents");
        if (recents.length() == 0 || recents.contains("Permission Denial")) return -1;
        for (String line : recents.split("\n")) {
            if (!line.contains("Recent #") || line.contains("type=home") || line.contains("type=dream")) continue;
            Matcher m = TASK.matcher(line);
            if (m.find()) {
                if (!m.group(1).equals(pkg) || Integer.parseInt(m.group(2)) == 0) continue;
            } else {
                Matcher m14 = TASK14.matcher(line);
                if (!m14.find() || !"standard".equals(m14.group(1)) || !m14.group(2).equals(pkg)) continue;
            }
            Matcher tm = TASK_ID.matcher(line);
            if (tm.find()) return Integer.parseInt(tm.group(1));
        }
        return -1;
    }

    /** Ends one app's background process (the app on screen is never touched by Android's call). */
    static void close(Context c, String pkg) {
        try { ((ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE)).killBackgroundProcesses(pkg); } catch (Throwable ignored) { }
    }

    /**
     * Ends these apps. First the polite way (Android's own background kill); whatever is still alive a moment later is
     * force-stopped through Settings by the accessibility service. {@code done} runs on the main thread at the end.
     */
    static void closeApps(final Context c, final List<String> pkgs, final Runnable done) {
        final Context app = c.getApplicationContext();
        for (String p : pkgs) close(app, p);
        new Thread(new Runnable() { public void run() {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) { }
            Set<String> alive = aliveNow();
            final List<String> left = new ArrayList<String>();
            for (String p : pkgs) if (alive.contains(p)) left.add(p);
            MAIN.post(new Runnable() { public void run() {
                if (left.isEmpty() || !ControlService.forceStop(left, done)) { if (done != null) done.run(); }
            } });
        } }, "recents-close").start();
    }

    /** Packages that have a live process right now. */
    static Set<String> aliveNow() {
        Set<String> alive = new HashSet<String>();
        Matcher pm = PROC.matcher(dump("processes"));
        while (pm.find()) alive.add(pm.group(1));
        return alive;
    }
}
