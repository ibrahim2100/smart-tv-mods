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
        final int keyCount = KeyMap.list(c).size();

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
            items.add(Sheet.Item.of("المفتوحة", R.drawable.ic_refresh, new Runnable() { public void run() { Sheet.chained = true; recents(c, anchor, onClose); } })
                    .value("التطبيقات").tile());
            items.add(Sheet.Item.of("المكيف", R.drawable.ic_snow, new Runnable() { public void run() { Sheet.chained = true; ac(c, anchor, onClose); } })
                    .value(!Ac.hasToken(c) ? "غير مربوط" : Ac.list(c).size() + " أوامر").tile());
            items.add(Sheet.Item.of("الإضاءة", R.drawable.ic_bulb, new Runnable() { public void run() { Sheet.chained = true; lights(c, anchor, onClose); } })
                    .value(!Lights.configured(c) ? "غير مربوط" : Lights.targets(c).size() + " عناصر").tile());
            items.add(Sheet.Item.of("أزرار الريموت", R.drawable.ic_apps, new Runnable() { public void run() { Sheet.chained = true; remoteKeys(c, anchor, onClose); } })
                    .value(keyCount == 0 ? "غير مخصصة" : keyCount + " مخصّصة"));
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
            if (!Theme.black) items.add(flag(c, "لون الخلفية يتبع التطبيق", "follow", Theme.follow, "الخلفية تتلوّن بلون التطبيق المحدد"));
            items.add(Sheet.Item.header("الشاشة"));
            items.add(Sheet.Item.of("حواف الشاشة", R.drawable.ic_tv, new Runnable() { public void run() {
                Safe.setLevel(c, (Safe.level(c) + 1) % 3);
            } }).value(Safe.NAMES[Safe.level(c)]).sub("لو التلفزيون يقص أطراف الصورة").stay());
            items.add(Sheet.Item.of("صفوف التطبيقات", R.drawable.ic_apps, new Runnable() { public void run() {
                Theme.set(c, "rows", Theme.rows == 2 ? 1 : 2);
            } }).value(Theme.rows == 2 ? "صفّان" : "صف واحد").sub("رتّب التطبيقات على صف أو على صفين").stay());
            items.add(flag(c, "تخطيط مبسّط", "clean", Theme.clean, "بدون اسم التطبيق وأزرار فتح وخيارات وصورة الخلفية، وبلاطات أكبر بإطار أنيق"));
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
            items.add(Sheet.Item.header("الذاكرة"));
            items.add(flag(c, "تنظيف تلقائي قبل فتح التطبيق", "autoclean", Theme.autoClean, "ينهي تطبيقات الخلفية لو الرام قليلة. أوقفه كي لا يُغلق تطبيق خرجت منه بالغلط"));
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


    // ---- open apps (like a phone's app switcher) --------------------------------------------

    /** Apps you used recently: OK goes back to one, "close" ends its background process, and one row closes them all. */
    static void recents(final Context c, final View anchor, final Runnable onClose) {
        final Recents.Result[] res = new Recents.Result[1];
        Panel.show(c, "التطبيقات المفتوحة", "ارجع لتطبيق تركته، أو أغلقه", new Panel.Builder() { public List<Sheet.Item> items() {
            List<Sheet.Item> items = Sheet.list();
            if (anchor != null || c instanceof HomeActivity) items.add(back(c, anchor, onClose));
            final Recents.Result r = res[0];
            if (r == null) { items.add(Sheet.Item.header("جاري القراءة…")); return items; }
            if (!r.allowed) {
                items.add(Sheet.Item.header("ما قدرت أقرأ التطبيقات المفتوحة: يحتاج إذناً يُمنح مرة وحدة بـadb (pm grant … android.permission.DUMP)"));
                return items;
            }
            if (r.apps.isEmpty()) { items.add(Sheet.Item.header("ما في تطبيقات مفتوحة")); return items; }
            items.add(Sheet.Item.header("ارجع إلى"));
            for (final Recents.Entry e : r.apps) {
                items.add(Sheet.Item.of(e.label, R.drawable.ic_apps, new Runnable() { public void run() {
                    Recents.open(c, e);
                    if (c instanceof android.app.Activity && !(c instanceof HomeActivity)) ((android.app.Activity) c).finish();
                } }).sub(e.running ? "مفتوح في الخلفية" : "سيفتح من جديد").icon(e.icon));
            }
            boolean any = false;
            for (Recents.Entry e : r.apps) if (e.running && !Cleaner.keepAlways(e.pkg)) any = true;
            if (any) {
                items.add(Sheet.Item.header("إغلاق"));
                items.add(Sheet.Item.of("إغلاق الكل", R.drawable.ic_trash, new Runnable() { public void run() {
                    List<String> all = new java.util.ArrayList<String>();
                    for (Recents.Entry e : r.apps) if (e.running && !Cleaner.keepAlways(e.pkg)) all.add(e.pkg);
                    closeThese(c, all);
                } }).sub("ينهي كل التطبيقات المفتوحة في الخلفية").danger());
                for (final Recents.Entry e : r.apps) {
                    if (!e.running || Cleaner.keepAlways(e.pkg)) continue;
                    items.add(Sheet.Item.of(e.label, R.drawable.ic_trash, new Runnable() { public void run() {
                        closeThese(c, java.util.Arrays.asList(e.pkg));
                    } }).sub("إغلاق").icon(e.icon));
                }
            }
            return items;
        } }, null, closer(c, onClose));
        Recents.load(c, new Recents.Done() { public void onDone(Recents.Result r) { res[0] = r; Panel.refresh(); } });
    }

    /** Ends the apps and shows a status card; the screen may flash Settings for a moment per app that needed a force stop. */
    private static void closeThese(final Context c, final List<String> pkgs) {
        if (pkgs.isEmpty()) return;
        Notice.show(c, Notice.WORKING, R.drawable.ic_trash, 0xFFFF9A5C, "التطبيقات المفتوحة", "جاري الإغلاق…", pkgs.size() + (pkgs.size() == 1 ? " تطبيق" : " تطبيقات"), null);
        final int n = pkgs.size();
        Recents.closeApps(c, pkgs, new Runnable() { public void run() {
            Notice.show(c, Notice.DONE, R.drawable.ic_trash, 0xFFFF9A5C, "التطبيقات المفتوحة", "تم الإغلاق", n + (n == 1 ? " تطبيق" : " تطبيقات"), null);
        } });
    }

    // ---- light switches (Smart Life / Tuya) -------------------------------------------------

    /** Each button of your switches as a tile (OK flips it, the panel stays open), then the routines. */
    private static void lights(final Context c, final View anchor, final Runnable onClose) {
        if (Lights.configured(c)) Lights.refresh(c, new Lights.Done() { public void onDone(String error) { Panel.refresh(); } });
        Panel.show(c, "الإضاءة", "محلي أولًا، وبالسحابة عند الحاجة", new Panel.Builder() { public List<Sheet.Item> items() {
            List<Sheet.Item> items = Sheet.list();
            items.add(back(c, anchor, onClose));
            if (!Lights.configured(c)) {
                items.add(Sheet.Item.header("ما في أجهزة بعد: على الماك شغّل tools/tuya-keys.py ثم أرسل الملف للتلفزيون (انظر README)"));
                return items;
            }
            items.add(Sheet.Item.header("الأزرار"));
            for (final Lights.Dev d : Lights.devices(c)) {
                for (final Lights.Gang g : d.gangs) {
                    Boolean st = Lights.stateOf(d.id, g.dp);
                    items.add(Sheet.Item.of(g.name, R.drawable.ic_bulb, new Runnable() { public void run() {
                        Lights.set(c, d.id, g.dp, null, new Lights.Done() { public void onDone(String error) { Panel.refresh(); } });
                    } }).value(st == null ? "…" : st ? "شغّالة" : "مطفأة").tile().stay());
                }
            }
            items.add(Sheet.Item.header("الروتين"));
            for (final Lights.Routine r : Lights.routines(c)) {
                items.add(Sheet.Item.of(r.name, R.drawable.ic_power, new Runnable() { public void run() {
                    Lights.run(c, r, new Lights.Done() { public void onDone(String error) { Panel.refresh(); } });
                } }).sub("ينفّذ كل أزراره معًا").stay());
            }
            items.add(Sheet.Item.header("إدارة"));
            items.add(Sheet.Item.of("تحديث الحالة", R.drawable.ic_refresh, new Runnable() { public void run() {
                Lights.refresh(c, new Lights.Done() { public void onDone(String error) {
                    if (error.length() > 0) Toast.makeText(c, "الإضاءة: " + error, Toast.LENGTH_LONG).show();
                    Panel.refresh();
                } });
            } }).sub("يقرأ حالة كل زر من السويتشات").stay());
            items.add(Sheet.Item.of("فصل الإضاءة", R.drawable.ic_trash, new Runnable() { public void run() {
                Lights.disconnect(c);
                Panel.refresh();
            } }).sub("يمسح الأجهزة والمفاتيح من التلفزيون").danger().stay());
            return items;
        } }, null, closer(c, onClose));
    }

    // ---- air conditioner (through Alexa) ----------------------------------------------------

    /** The commands you made in Voice Monkey: OK sends one, the panel stays open so you can send another. */
    private static void ac(final Context c, final View anchor, final Runnable onClose) {
        Panel.show(c, "المكيف", "أوامر عبر Alexa", new Panel.Builder() { public List<Sheet.Item> items() {
            List<Sheet.Item> items = Sheet.list();
            items.add(back(c, anchor, onClose));
            if (!Ac.hasToken(c)) {
                items.add(Sheet.Item.header("ما في رمز بعد: افتح صفحة الجوال (الجوال ← الرابط) والصق رمز Voice Monkey في قسم «المكيف»"));
                return items;
            }
            List<Ac.Cmd> cmds = Ac.list(c);
            items.add(Sheet.Item.header(cmds.isEmpty() ? "ما في أوامر: أنشئ Trigger في Voice Monkey ثم حدّث" : "الأوامر"));
            for (final Ac.Cmd m : cmds) {
                items.add(Sheet.Item.of(m.name, R.drawable.ic_snow, new Runnable() { public void run() { Ac.send(c, m.id); } })
                        .sub("اضغط للإرسال").stay());
            }
            items.add(Sheet.Item.header("إدارة"));
            items.add(Sheet.Item.of("تحديث الأوامر", R.drawable.ic_refresh, new Runnable() { public void run() {
                Toast.makeText(c, "جاري التحديث…", Toast.LENGTH_SHORT).show();
                Ac.refresh(c, new Ac.Done() { public void onDone(String error) {
                    if (error.length() > 0) Toast.makeText(c, "المكيف: " + error, Toast.LENGTH_LONG).show();
                    Panel.refresh();
                } });
            } }).sub("يجلب الأوامر الجديدة من Voice Monkey").stay());
            items.add(Sheet.Item.of("فصل المكيف", R.drawable.ic_trash, new Runnable() { public void run() {
                Ac.disconnect(c);
                Panel.refresh();
            } }).sub("يمسح الرمز والأوامر من التلفزيون").danger().stay());
            return items;
        } }, null, closer(c, onClose));
    }

    // ---- your own remote buttons ------------------------------------------------------------

    /** The list of remote buttons you linked to apps, plus "add a button". */
    private static void remoteKeys(final Context c, final View anchor, final Runnable onClose) {
        Panel.show(c, "أزرار الريموت", "اربط أي زر في الريموت بالتطبيق اللي تختاره", new Panel.Builder() { public List<Sheet.Item> items() {
            List<Sheet.Item> items = Sheet.list();
            items.add(back(c, anchor, onClose));
            if (!ControlService.ready()) items.add(Sheet.Item.header("⚠ خدمة الإتاحة (Albrhi Remote Control) غير شغّالة: الأزرار المخصصة لن تعمل. فعّلها من الإعدادات ← إمكانية الوصول"));
            List<KeyMap.Entry> es = KeyMap.list(c);
            if (!es.isEmpty()) items.add(Sheet.Item.header("الأزرار المربوطة"));
            for (final KeyMap.Entry e : es) {
                items.add(Sheet.Item.of(KeyMap.titleOf(e), R.drawable.ic_apps, new Runnable() { public void run() {
                    Sheet.chained = true;
                    keyDetail(c, anchor, onClose, e.code);
                } }).sub(KeyMap.actionText(c, e.pkg)).icon(KeyMap.iconOf(c, e.pkg)));
            }
            items.add(Sheet.Item.header("جديد"));
            items.add(Sheet.Item.of("إضافة زر", R.drawable.ic_edit, new Runnable() { public void run() {
                Sheet.chained = true;
                addKey(c, anchor, onClose);
            } }).sub("اضغط أي زر في الريموت (يوتيوب، نتفلكس، ملوّن، رقم…) ثم اختر تطبيقه"));
            return items;
        } }, null, closer(c, onClose));
    }

    /** Waits for you to press a button on the remote, then asks which app it should open. */
    private static void addKey(final Context c, final View anchor, final Runnable onClose) {
        LearnDialog.show(c, new KeyMap.Learn() { @Override public void onKey(final int code) {
            pickApp(c, anchor, onClose, "اختر التطبيق لـ" + KeyMap.titleOf(code0(c, code)), new Runnable() { public void run() { remoteKeys(c, anchor, onClose); } },
                    new PickApp() { public void pick(String pkg) {
                        KeyMap.put(c, code, pkg, null);
                        Toast.makeText(c, "تم: " + KeyMap.titleOf(code0(c, code)) + " ← " + KeyMap.labelOf(c, pkg), Toast.LENGTH_SHORT).show();                        remoteKeys(c, anchor, onClose);
                    } });
        } }, new Runnable() { public void run() { remoteKeys(c, anchor, onClose); } });
    }

    private interface PickApp { void pick(String pkg); }

    /** An entry for this code (a throw-away one before it is saved), so its automatic name can be shown. */
    private static KeyMap.Entry code0(Context c, int code) {
        KeyMap.Entry e = KeyMap.get(c, code);
        if (e != null) return e;
        e = new KeyMap.Entry();
        e.code = code;
        e.name = "";
        return e;
    }

    /** Every installed app with its icon; OK picks one. */
    private static void pickApp(final Context c, final View anchor, final Runnable onClose, String title, final Runnable onBack, final PickApp cb) {
        final List<KeyMap.AppRef> apps = KeyMap.apps(c);
        Panel.show(c, title, "التطبيقات المثبتة", new Panel.Builder() { public List<Sheet.Item> items() {
            List<Sheet.Item> items = Sheet.list();
            items.add(Sheet.Item.of("رجوع", R.drawable.ic_back, new Runnable() { public void run() { Sheet.chained = true; onBack.run(); } }));
            items.add(Sheet.Item.header("أدوات"));
            items.add(Sheet.Item.of("التطبيقات المفتوحة", R.drawable.ic_refresh, new Runnable() { public void run() { Sheet.chained = true; cb.pick(KeyMap.ACT_RECENTS); } })
                    .sub("يفتح لوحة الرجوع للتطبيقات وإغلاقها"));
            items.add(Sheet.Item.of("شاشة الانتظار", R.drawable.ic_eye, new Runnable() { public void run() { Sheet.chained = true; cb.pick(KeyMap.ACT_SAVER); } })
                    .sub("تشغّل شاشة الانتظار فوراً (اضغط أي زر لتخرج)"));
            List<Ac.Cmd> acs = Ac.hasToken(c) ? Ac.list(c) : new java.util.ArrayList<Ac.Cmd>();
            List<Lights.Target> lts = Lights.configured(c) ? Lights.targets(c) : new java.util.ArrayList<Lights.Target>();
            if (!lts.isEmpty()) {
                items.add(Sheet.Item.header("الإضاءة"));
                for (final Lights.Target t : lts) {
                    items.add(Sheet.Item.of(t.title, R.drawable.ic_bulb, new Runnable() { public void run() { Sheet.chained = true; cb.pick("lt:" + t.token); } })
                            .sub(t.sub));
                }
                if (acs.isEmpty()) items.add(Sheet.Item.header("التطبيقات"));
            }
            if (!acs.isEmpty()) {
                items.add(Sheet.Item.header("المكيف"));
                for (final Ac.Cmd m : acs) {
                    items.add(Sheet.Item.of(m.name, R.drawable.ic_snow, new Runnable() { public void run() { Sheet.chained = true; cb.pick("ac:" + m.id); } })
                            .sub("يرسل الأمر للمكيف"));
                }
                items.add(Sheet.Item.header("التطبيقات"));
            }
            for (final KeyMap.AppRef a : apps) {
                items.add(Sheet.Item.of(a.label, R.drawable.ic_apps, new Runnable() { public void run() { Sheet.chained = true; cb.pick(a.pkg); } }).icon(a.icon));
            }
            return items;
        } }, null, closer(c, onClose));
    }

    /** One linked button: change its app, rename it, or remove it. */
    private static void keyDetail(final Context c, final View anchor, final Runnable onClose, final int code) {
        final KeyMap.Entry e0 = KeyMap.get(c, code);
        if (e0 == null) { remoteKeys(c, anchor, onClose); return; }
        Panel.show(c, KeyMap.titleOf(e0), KeyMap.actionText(c, e0.pkg), new Panel.Builder() { public List<Sheet.Item> items() {
            final KeyMap.Entry e = KeyMap.get(c, code);
            List<Sheet.Item> items = Sheet.list();
            items.add(Sheet.Item.of("رجوع", R.drawable.ic_back, new Runnable() { public void run() { Sheet.chained = true; remoteKeys(c, anchor, onClose); } }));
            if (e == null) return items;
            items.add(Sheet.Item.of("تغيير التطبيق", R.drawable.ic_apps, new Runnable() { public void run() {
                Sheet.chained = true;
                pickApp(c, anchor, onClose, "اختر التطبيق لـ" + KeyMap.titleOf(e), new Runnable() { public void run() { keyDetail(c, anchor, onClose, code); } },
                        new PickApp() { public void pick(String pkg) { KeyMap.put(c, code, pkg, null); keyDetail(c, anchor, onClose, code); } });
            } }).value(KeyMap.labelOf(c, e.pkg)).icon(KeyMap.iconOf(c, e.pkg)));
            items.add(Sheet.Item.of("إعادة تسمية الزر", R.drawable.ic_edit, new Runnable() { public void run() {
                RenameDialog.show(c, "اسم الزر", e.name == null ? "" : e.name, KeyMap.defaultName(code), false, new RenameDialog.Callback() {
                    @Override public void onName(String n) { KeyMap.rename(c, code, n); Panel.refresh(); }
                });
            } }).value(KeyMap.titleOf(e)).stay());
            items.add(Sheet.Item.of("حذف الزر", R.drawable.ic_trash, new Runnable() { public void run() {
                KeyMap.remove(c, code);
                Sheet.chained = true;
                remoteKeys(c, anchor, onClose);
            } }).sub("يرجع الزر لعمله الأصلي في التلفزيون").danger());
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
