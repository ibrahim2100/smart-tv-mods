package com.albrhi.tvwidget;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;

import java.util.HashSet;
import java.util.Set;

/**
 * Frees memory the safe way: asks Android to end the BACKGROUND processes of the apps you installed.
 * It never touches the app on screen, the TV's own services, the keyboard, or this launcher.
 * (A background player, e.g. music in VLC, would be stopped.)
 */
final class Cleaner {
    private Cleaner() { }

    interface Done { void onDone(int freedMb, int apps); }

    private static final String[] NEVER = {"com.albrhi.", "android", "com.android.", "com.google.android.gms", "com.google.android.gsf",
            "com.google.android.inputmethod", "com.google.android.tts", "com.google.android.apps.tv.", "com.google.android.tvlauncher",
            "com.google.android.katniss", "com.google.android.permissioncontroller", "com.mediatek.", "com.haier", "com.mstar.",
            "com.google.android.packageinstaller", "com.google.android.webview", "com.google.android.trichromelibrary"};

    private static long availMb(Context c) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ((ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mi);
        return mi.availMem / (1024 * 1024);
    }

    static void clean(final Context ctx, final Done done) {
        final Context c = ctx.getApplicationContext();
        final long before = availMb(c);
        int n = 0;
        try {
            PackageManager pm = c.getPackageManager();
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            Set<String> pkgs = new HashSet<String>();
            Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            Intent ph = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for (ResolveInfo ri : pm.queryIntentActivities(tv, 0)) pkgs.add(ri.activityInfo.packageName);
            for (ResolveInfo ri : pm.queryIntentActivities(ph, 0)) pkgs.add(ri.activityInfo.packageName);
            outer:
            for (String p : pkgs) {
                for (String s : NEVER) if (p.startsWith(s)) continue outer;
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(p, 0);
                    if ((ai.flags & ApplicationInfo.FLAG_PERSISTENT) != 0) continue;
                    am.killBackgroundProcesses(p);
                    n++;
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) { }
        final int count = n;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() { if (done != null) done.onDone((int) Math.max(0, availMb(c) - before), count); }
        }, 1800);
    }
}
