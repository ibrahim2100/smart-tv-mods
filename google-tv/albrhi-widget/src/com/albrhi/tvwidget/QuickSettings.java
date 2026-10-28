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

    /** Runs when the whole panel closes (not between pages): the home screen picks up any change of look once. */
    private static Runnable closer(final Context c, final Runnable onClose) {
        return new Runnable() { public void run() {
            if (c instanceof HomeActivity) ((HomeActivity) c).applyLookIfChanged();
            if (onClose != null) onClose.run();
        } };
    }

    /** @param onClose runs when the whole panel is closed (not when it just moves to a sub-page). */
    static void show(final Context c, final View anchor, final Runnable onClose) {
        final float dn = c.getResources().getDisplayMetrics().density;
        final Runnable done = closer(c, onClose);
        Health.Info hi = Health.read(c);
        final int paired = RemoteServer.get(c).pairedCount();
        final String ram = hi.ramFreeMb + " MB";

        LinearLayout meters = new LinearLayout(c);
        meters.setOrientation(LinearLayout.HORIZONTAL);
        meters.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        View ramBar = Sheet.meter(c, "الرام", hi.ramFreePct + "% متاحة", 100 - hi.ramFreePct, dn);
        View st = Sheet.meter(c, "التخزين", "\u2066" + String.format(java.util.Locale.US, "%.0f", hi.storageFreeMb / 1024.0) + " GB\u2069 حرة",
                100 - hi.storageFreePct, dn);
        meters.addView(ramBar, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, -2, 1f);
        sp.setMarginStart(Math.round(18 * dn));
        meters.addView(st, sp);

        Panel.show(c, "الإعدادات السريعة", null, new Panel.Builder() { public List<Sheet.Item> items() {
            List<Sheet.Item> items = Sheet.list();
            // the things you reach for most: a grid of tiles
            items.add(Sheet.Item.of("الصورة", R.drawable.ic_picture, open(c, Settings.ACTION_DISPLAY_SETTINGS)).tile());
            items.add(Sheet.Item.of("الصوت", R.drawable.ic_sound, open(c, Settings.ACTION_SOUND_SETTINGS)).tile());
            items.add(Sheet.Item.of("الشبكة", R.drawable.ic_wifi, open(c, Settings.ACTION_WIFI_SETTINGS)).value(networkName(c)).tile());
            items.add(Sheet.Item.of("الجوال", R.drawable.ic_phone, new Runnable() { public void run() { RemoteDialog.show(c); } })
                    .value(paired == 0 ? "غير مربوط" : paired + (paired == 1 ? " جهاز" : " أجهزة")).tile());
            items.add(Sheet.Item.of("المظهر", R.drawable.ic_eye, new Runnable() { public void run() { Sheet.chained = true; appearance(c, anchor, onClose); } })
                    .value(Theme.NAMES[Theme.index]).tile());
            items.add(Sheet.Item.of("الذاكرة", R.drawable.ic_refresh, new Runnable() { public void run() { cleanMemory(c); } }).value(ram).tile());
            // the rest, as quiet rows
            items.add(Sheet.Item.of("كل إعدادات التلفزيون", R.drawable.ic_gear, open(c, Settings.ACTION_SETTINGS)));
            items.add(Sheet.Item.of("أدوات وصيانة", R.drawable.ic_more, new Runnable() { public void run() { Sheet.chained = true; more(c, anchor, onClose); } }));
            return items;
        } }, meters, done);
    }

    private static Sheet.Item back(final Context c, final View anchor, final Runnable onClose) {
        return Sheet.Item.of("رجوع", R.drawable.ic_back, new Runnable() { public void run() { Sheet.chained = true; show(c, anchor, onClose); } });
    }

    /**
     * Look and details. Every change is saved at once and the panel redraws itself (it stays open);
     * the home screen is rebuilt once, when the panel closes.
     */
    private static void appearance(final Context c, final View anchor, final Runnable onClose) {
        Panel.show(c, "المظهر", "الألوان والتفاصيل", new Panel.Builder() { public List<Sheet.Item> items() {
            List<Sheet.Item> items = Sheet.list();
            items.add(back(c, anchor, onClose));
            items.add(Sheet.Item.header("السمة · " + Theme.NAMES[Theme.index]));
            items.add(Sheet.Item.swatches(Theme.NAMES[Theme.index], Theme.index, new Sheet.IntPick() { public void pick(int i) {
                Theme.set(c, "theme", i);
            } }));
            items.add(flag(c, "لون الخلفية يتبع التطبيق", "follow", Theme.follow, "الخلفية تتلوّن بلون التطبيق المحدد"));
            items.add(Sheet.Item.header("الشاشة"));
            items.add(Sheet.Item.of("حواف الشاشة", R.drawable.ic_tv, new Runnable() { public void run() {
                Safe.setLevel(c, (Safe.level(c) + 1) % 3);
            } }).value(Safe.NAMES[Safe.level(c)]).sub("لو التلفزيون يقص أطراف الصورة").stay());
            items.add(Sheet.Item.header("الشريط العلوي"));
            items.add(flag(c, "الوقت بنظام 24 ساعة", "clock24", Theme.clock24, null));
            items.add(flag(c, "التاريخ الهجري", "hijri", Theme.hijri, null));
            items.add(flag(c, "الطقس", "weather", Theme.weather, "حسب موقعك من الـIP"));
            items.add(Sheet.Item.of("الاسم في التحية", R.drawable.ic_edit, new Runnable() { public void run() {
                RenameDialog.show(c, "الاسم في التحية", Theme.name, "بدون اسم", false, new RenameDialog.Callback() {
                    @Override public void onName(String n) {
                        Theme.set(c, "name", n == null ? "" : n.trim());
                        Panel.refresh();
                    }
                });
            } }).value(Theme.name.length() == 0 ? "بدون" : Theme.name).sub("فارغ = بدون تحية").stay());
            items.add(Sheet.Item.header("الرياضة"));
            items.add(Sheet.Item.of("المباريات والتنبيهات", R.drawable.ic_ball, new Runnable() { public void run() {
                Theme.set(c, "sports", !Theme.sports);
                Alerts.schedule(c);                    // arms or cancels the match alarms
            } }).toggle(Theme.sports).sub("بطاقة المباراة وتنبيهاتها"));
            return items;
        } }, null, closer(c, onClose));
    }

    /** A row with a real switch; OK flips it, saves it, and the panel redraws. */
    private static Sheet.Item flag(final Context c, String title, final String key, final boolean on, String sub) {
        Sheet.Item it = Sheet.Item.of(title, R.drawable.ic_eye, new Runnable() { public void run() { Theme.set(c, key, !on); } }).toggle(on);
        return sub == null ? it : it.sub(sub);
    }

    private static void more(final Context c, final View anchor, final Runnable onClose) {
        Panel.show(c, "أدوات وصيانة", null, new Panel.Builder() { public List<Sheet.Item> items() {
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
            return items;
        } }, null, closer(c, onClose));
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
