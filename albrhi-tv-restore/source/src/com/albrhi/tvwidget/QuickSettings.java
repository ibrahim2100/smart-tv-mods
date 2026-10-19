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

    static void show(Context c) { show(c, null); }

    /** @param onClose runs when the whole sheet is closed (not when it just moves to "more"). */
    static void show(final Context c, final Runnable onClose) {
        final float dn = c.getResources().getDisplayMetrics().density;
        Health.Info hi = Health.read(c);
        int paired = RemoteServer.get(c).pairedCount();
        List<Sheet.Item> items = Sheet.list();
        items.add(Sheet.Item.of("الصورة", R.drawable.ic_picture, open(c, Settings.ACTION_DISPLAY_SETTINGS)).sub("السطوع والألوان ووضع الصورة"));
        items.add(Sheet.Item.of("الصوت", R.drawable.ic_sound, open(c, Settings.ACTION_SOUND_SETTINGS)).sub("المخرج ومستوى الصوت والمؤثرات"));
        items.add(Sheet.Item.of("الشبكة والإنترنت", R.drawable.ic_wifi, open(c, Settings.ACTION_WIFI_SETTINGS)).sub(networkSummary(c)));
        items.add(Sheet.Item.of("ربط الجوال", R.drawable.ic_phone, new Runnable() { public void run() { RemoteDialog.show(c); } })
                .sub(paired == 0 ? "اربط جوالك للتحكم والتثبيت" : paired + (paired == 1 ? " جهاز مرتبط" : " أجهزة مرتبطة")));
        items.add(Sheet.Item.of("كل الإعدادات", R.drawable.ic_gear, open(c, Settings.ACTION_SETTINGS)).sub("إعدادات التلفزيون الكاملة"));
        items.add(Sheet.Item.of("المزيد", R.drawable.ic_more, new Runnable() { public void run() { Sheet.chained = true; more(c, onClose); } })
                .sub("المباريات والأدوات وشاشة الطوارئ"));

        LinearLayout meters = new LinearLayout(c);
        meters.setOrientation(LinearLayout.VERTICAL);
        meters.addView(Sheet.meter(c, "الرام", "\u2066" + hi.ramFreeMb + " MB\u2069  ·  \u2066" + hi.ramFreePct + "%\u2069 متاحة", 100 - hi.ramFreePct, dn));
        View st = Sheet.meter(c, "التخزين", "\u2066" + String.format(java.util.Locale.US, "%.1f", hi.storageFreeMb / 1024.0) + " GB\u2069 حرة",
                100 - hi.storageFreePct, dn);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = Math.round(6 * dn);
        meters.addView(st, sp);

        long now = System.currentTimeMillis();
        Sheet.show(c, "إعدادات سريعة", fmt("EEEE", GREG, now) + "  ·  " + fmt("h:mm a", GREG, now), R.drawable.ic_gear, items, meters, onClose);
    }

    private static void more(final Context c, final Runnable onClose) {
        List<Sheet.Item> items = Sheet.list();
        items.add(Sheet.Item.of("القائمة المختصرة", R.drawable.ic_back, new Runnable() { public void run() { Sheet.chained = true; show(c, onClose); } })
                .sub("الرجوع للخيارات الأساسية"));
        items.add(Sheet.Item.header("المباريات"));
        items.add(Sheet.Item.of("تحديث المباريات الآن", R.drawable.ic_refresh, new Runnable() { public void run() {
            Toast.makeText(c, "جاري تحديث المباريات…", Toast.LENGTH_SHORT).show();
            Fixtures.refresh(c, true);
        } }).sub("إعادة تحميل المواعيد والنتائج"));
        items.add(Sheet.Item.of("تجربة تنبيه المباراة", R.drawable.ic_bell, new Runnable() { public void run() { AppService.testAlert(c); } })
                .sub("يعرض الإشعار كما سيظهر قبل المباراة"));
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
        Sheet.show(c, "المزيد", "أدوات وصيانة", R.drawable.ic_more, null, items, null, onClose, 400);
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
