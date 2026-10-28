package com.albrhi.tvwidget;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.List;

import static com.albrhi.tvwidget.WidgetView.*;

/**
 * The short settings sheet: picture, sound, network, phone pairing, all settings, and "more".
 * Opened from the on-screen Settings button and from the remote's gear button.
 */
final class QuickSettings {
    private QuickSettings() { }

    static void show(Context c) { show(c, null, null); }

    /** @param onClose runs when the whole panel is closed (not when it just moves to a sub-page). */
    static void show(final Context c, final View anchor, final Runnable onClose) {
        final float dn = c.getResources().getDisplayMetrics().density;
        Health.Info hi = Health.read(c);
        int paired = RemoteServer.get(c).pairedCount();
        List<Sheet.Item> items = Sheet.list();
        // the things you reach for most: a grid of tiles
        items.add(Sheet.Item.of("الصورة", R.drawable.ic_picture, open(c, Settings.ACTION_DISPLAY_SETTINGS)).tile());
        items.add(Sheet.Item.of("الصوت", R.drawable.ic_sound, open(c, Settings.ACTION_SOUND_SETTINGS)).tile());
        items.add(Sheet.Item.of("الشبكة", R.drawable.ic_wifi, open(c, Settings.ACTION_WIFI_SETTINGS)).value(networkName(c)).tile());
        items.add(Sheet.Item.of("الجوال", R.drawable.ic_phone, new Runnable() { public void run() { RemoteDialog.show(c); } })
                .value(paired == 0 ? "غير مربوط" : paired + (paired == 1 ? " جهاز" : " أجهزة")).tile());
        items.add(Sheet.Item.of("المظهر", R.drawable.ic_eye, new Runnable() { public void run() { Sheet.chained = true; appearance(c, anchor, onClose); } })
                .value(Theme.NAMES[Theme.index]).tile());
        items.add(Sheet.Item.of("الذاكرة", R.drawable.ic_refresh, new Runnable() { public void run() { cleanMemory(c); } })
                .value(hi.ramFreeMb + " MB").tile());
        // the rest, as quiet rows
        items.add(Sheet.Item.of("كل إعدادات التلفزيون", R.drawable.ic_gear, open(c, Settings.ACTION_SETTINGS)));
        items.add(Sheet.Item.of("أدوات وصيانة", R.drawable.ic_more, new Runnable() { public void run() { Sheet.chained = true; more(c, anchor, onClose); } }));

        LinearLayout meters = new LinearLayout(c);
        meters.setOrientation(LinearLayout.HORIZONTAL);
        meters.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        View ram = Sheet.meter(c, "الرام", hi.ramFreePct + "% متاحة", 100 - hi.ramFreePct, dn);
        View st = Sheet.meter(c, "التخزين", "\u2066" + String.format(java.util.Locale.US, "%.0f", hi.storageFreeMb / 1024.0) + " GB\u2069 حرة",
                100 - hi.storageFreePct, dn);
        meters.addView(ram, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, -2, 1f);
        sp.setMarginStart(Math.round(18 * dn));
        meters.addView(st, sp);

        Panel.show(c, "الإعدادات السريعة", null, items, meters, onClose);
    }

    private static Sheet.Item back(final Context c, final View anchor, final Runnable onClose) {
        return Sheet.Item.of("رجوع", R.drawable.ic_back, new Runnable() { public void run() { Sheet.chained = true; show(c, anchor, onClose); } });
    }

    /** Look and details: theme colours, clock format, Hijri date, weather, the name in the greeting, and sports (off by default). */
    private static void appearance(final Context c, final View anchor, final Runnable onClose) {
        List<Sheet.Item> items = Sheet.list();
        items.add(back(c, anchor, onClose));
        items.add(Sheet.Item.header("السمة"));
        items.add(Sheet.Item.swatches(Theme.NAMES[Theme.index], Theme.index, new Sheet.IntPick() { public void pick(int i) {
            Theme.set(c, "theme", i);
            applied(c, "السمة: " + Theme.NAMES[Theme.index]);
        } }));
        items.add(onOff(c, "لون الخلفية يتبع التطبيق", "follow", Theme.follow, null));
        items.add(Sheet.Item.header("الشاشة"));
        items.add(Sheet.Item.of("حواف الشاشة", R.drawable.ic_tv, new Runnable() { public void run() { cycleEdges(c); } })
                .value(Safe.NAMES[Safe.level(c)]).sub("لو التلفزيون يقص أطراف الصورة"));
        items.add(Sheet.Item.header("الشريط العلوي"));
        items.add(Sheet.Item.of("الوقت بنظام 24 ساعة", R.drawable.ic_refresh, new Runnable() { public void run() {
            Theme.set(c, "clock24", !Theme.clock24);
            applied(c, Theme.clock24 ? "الوقت: 24 ساعة" : "الوقت: 12 ساعة");
        } }).toggle(Theme.clock24));
        items.add(onOff(c, "التاريخ الهجري", "hijri", Theme.hijri, null));
        items.add(onOff(c, "الطقس", "weather", Theme.weather, "حسب موقعك من الـIP"));
        items.add(Sheet.Item.of("الاسم في التحية", R.drawable.ic_edit, new Runnable() { public void run() {
            RenameDialog.show(c, Theme.name, "", new RenameDialog.Callback() {
                @Override public void onName(String n) { Theme.set(c, "name", n == null ? "" : n.trim()); applied(c, "تم حفظ الاسم"); }
            });
        } }).value(Theme.name.length() == 0 ? "بدون" : Theme.name));
        items.add(Sheet.Item.header("الرياضة"));
        items.add(Sheet.Item.of("المباريات والتنبيهات", R.drawable.ic_ball, new Runnable() { public void run() {
            Theme.set(c, "sports", !Theme.sports);
            Alerts.schedule(c);                    // arms or cancels the match alarms
            applied(c, Theme.sports ? "الرياضة: مفعّلة" : "الرياضة: متوقفة");
        } }).toggle(Theme.sports).sub("بطاقة المباراة وتنبيهاتها"));
        Panel.show(c, "المظهر", "الألوان والتفاصيل", items, null, onClose);
    }

    private static Sheet.Item onOff(final Context c, final String title, final String key, final boolean on, String sub) {
        Sheet.Item it = Sheet.Item.of(title, R.drawable.ic_eye, new Runnable() { public void run() {
            Theme.set(c, key, !on);
            applied(c, title + (on ? ": متوقف" : ": مفعّل"));
        } }).toggle(on);
        return sub == null ? it : it.sub(sub);
    }

    /** A change is saved: the home screen (and the screensaver, next time) pick it up. */
    private static void applied(Context c, String what) {
        Toast.makeText(c, what, Toast.LENGTH_SHORT).show();
        if (c instanceof HomeActivity) ((HomeActivity) c).recreate();      // others pick it up when Home comes back
    }

    private static void more(final Context c, final View anchor, final Runnable onClose) {
        List<Sheet.Item> items = Sheet.list();
        items.add(back(c, anchor, onClose));
        if (Theme.sports) {
            items.add(Sheet.Item.header("المباريات"));
            items.add(Sheet.Item.of("تحديث المباريات الآن", R.drawable.ic_refresh, new Runnable() { public void run() {
                Toast.makeText(c, "جاري تحديث المباريات…", Toast.LENGTH_SHORT).show();
                Fixtures.refresh(c, true);
            } }).sub("إعادة تحميل المواعيد والنتائج"));
            items.add(Sheet.Item.of("تجربة تنبيه المباراة", R.drawable.ic_bell, new Runnable() { public void run() { AppService.testAlert(c); } })
                    .sub("يعرض الإشعار كما سيظهر قبل المباراة"));
        }
        items.add(Sheet.Item.header("النظام"));
        items.add(Sheet.Item.of("التطبيقات", R.drawable.ic_apps, open(c, Settings.ACTION_APPLICATION_SETTINGS)).sub("إدارة التطبيقات المثبتة"));
        items.add(Sheet.Item.of("الموقع", R.drawable.ic_pin, open(c, Settings.ACTION_LOCATION_SOURCE_SETTINGS)).sub("لازم يكون شغّال لإظهار اسم الواي فاي"));
        items.add(Sheet.Item.of("الكيبورد", R.drawable.ic_keyboard, open(c, Settings.ACTION_INPUT_METHOD_SETTINGS)).sub("لوحة المفاتيح واللغات"));
        items.add(Sheet.Item.of("إمكانية الوصول", R.drawable.ic_access, open(c, Settings.ACTION_ACCESSIBILITY_SETTINGS)).sub("خدمة الريموت من الجوال"));
        items.add(Sheet.Item.header("الإنقاذ"));
        items.add(Sheet.Item.of("رجوع لواجهة Google TV", R.drawable.ic_home, new Runnable() { public void run() { googleTvPage(c); } })
                .sub("يفتح صفحة التفعيل (زر Enable)"));
        items.add(Sheet.Item.of("تجربة شاشة الطوارئ", R.drawable.ic_warn, new Runnable() { public void run() {
            CrashGuard.simulate(c);
            c.startActivity(new Intent(c, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } }).sub("للتأكد من خطة الإنقاذ فقط").danger());
        Panel.show(c, "أدوات وصيانة", null, items, null, onClose);
    }

    /** Just the network's name, short enough for a tile. */
    private static String networkName(Context c) {
        String s = networkSummary(c);
        int i = s.indexOf("  ·  ");
        return i > 0 ? s.substring(0, i) : s;
    }

    /** Ends background apps (never the one on screen) and tells you how much memory came back. */
    static void cleanMemory(final Context c) {
        Toast.makeText(c, "جاري تنظيف الذاكرة…", Toast.LENGTH_SHORT).show();
        Cleaner.clean(c, new Cleaner.Done() {
            public void onDone(int freedMb, int apps) {
                Banner.show(c, "تنظيف الذاكرة", freedMb > 0 ? "تحرّر " + freedMb + " MB" : "الذاكرة نظيفة أصلًا",
                        apps + " تطبيق راجعته · ما لمست التطبيق المفتوح", null, null, 6);
            }
        });
    }

    /** Some TVs crop the picture edges: cycles how far from the edge everything is kept. */
    private static void cycleEdges(Context c) {
        Safe.setLevel(c, (Safe.level(c) + 1) % 3);
        if (c instanceof HomeActivity) ((HomeActivity) c).recreate();      // others pick it up when Home comes back
        Toast.makeText(c, "حواف الشاشة: " + Safe.NAMES[Safe.level(c)], Toast.LENGTH_SHORT).show();
    }

    private static String networkSummary(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork();
            NetworkCapabilities nc = n == null ? null : cm.getNetworkCapabilities(n);
            if (nc == null) return "غير متصل";
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "إيثرنت";
            WifiInfo wi = ((WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
            String s = wi.getSSID();
            String name = s == null || s.contains("unknown ssid") ? "Wi-Fi" : s.replace("\"", "");
            return name + "  ·  \u2066" + wi.getLinkSpeed() + " Mbps\u2069";
        } catch (Throwable t) {
            return "حالة الاتصال والشبكات";
        }
    }

    private static Runnable open(final Context c, final String action) {
        return new Runnable() { public void run() {
            try { c.startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Throwable ignored) { }
        } };
    }

    /** Google TV launcher's page in Settings: it has the Enable button while it is disabled. */
    private static void googleTvPage(Context c) {
        try {
            c.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:com.google.android.apps.tv.launcherx")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable t) {
            try { c.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Throwable ignored) { }
        }
    }
}
