package com.albrhi.tvwidget;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

/** Free memory and storage of the TV, with a warning when either is running low. */
final class Health {
    private Health() { }

    static final class Info {
        long ramFreeMb, ramTotalMb, storageFreeMb, storageTotalMb;
        int ramFreePct, storageFreePct;
        String warning;      // null when everything is fine
    }

    static Info read(Context c) {
        Info i = new Info();
        try {
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            i.ramFreeMb = mi.availMem / (1024 * 1024);
            i.ramTotalMb = mi.totalMem / (1024 * 1024);
            i.ramFreePct = i.ramTotalMb == 0 ? 100 : (int) (i.ramFreeMb * 100 / i.ramTotalMb);
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            i.storageFreeMb = fs.getAvailableBytes() / (1024 * 1024);
            i.storageTotalMb = fs.getTotalBytes() / (1024 * 1024);
            i.storageFreePct = i.storageTotalMb == 0 ? 100 : (int) (i.storageFreeMb * 100 / i.storageTotalMb);
            if (mi.lowMemory || i.ramFreeMb < 250) i.warning = "الرام قليلة · " + i.ramFreeMb + " MB متاحة";
            else if (i.storageFreeMb < 1500 || i.storageFreePct < 8)
                i.warning = "التخزين شارف يمتلي · " + String.format(java.util.Locale.US, "%.1f", i.storageFreeMb / 1024.0) + " GB حرة";
        } catch (Throwable ignored) { }
        return i;
    }

    static String summary(Info i) {
        // numbers + Latin units are isolated so they keep their order inside the Arabic sentence
        return "الرام: \u2066" + i.ramFreePct + "%\u2069 متاحة (\u2066" + i.ramFreeMb + " MB\u2069)  ·  التخزين: \u2066"
                + String.format(java.util.Locale.US, "%.1f", i.storageFreeMb / 1024.0) + " GB\u2069 حرة";
    }
}
