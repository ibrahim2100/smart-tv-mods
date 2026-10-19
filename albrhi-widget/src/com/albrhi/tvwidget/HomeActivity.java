package com.albrhi.tvwidget;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.albrhi.tvwidget.WidgetView.*;

/**
 * Albrhi Home: a light launcher - a short header (clock + next match), a status row, and the apps.
 * Your apps come first; the TV's own tools (Settings, Play Store, Live TV...) sit in their own row below.
 * Long-press OK on an app: move it, rename it, hide it, or uninstall it.
 */
public class HomeActivity extends Activity {
    private static final int COLUMNS = 6;
    private static final int LABEL_SLOT = 28;     // dp of empty space above every tile row, where the name pops up
    private static final String HIDDEN = "__hidden";
    /** First-run order, until you move something yourself. */
    private static final List<String> PRESET = Arrays.asList(
            "io.gh.reisxd.tizentube.cobalt", "com.google.android.youtube.tv", "net.mbc.shahidTV",
            "com.intigral.jawwytv", "com.netflix.ninja", "com.amazon.amazonvideo.livingroom",
            "com.disneyplus.mea", "com.apple.atve.androidtv.appletv", "com.movieboxpro.androidtv",
            "com.thmanyah.shasha.tv", "com.hilal.play", "com.spacetoon.vod", "com.crunchyroll.crunchyroid",
            "org.videolan.vlc", "com.mediatek.wwtv.tvcenter", "com.android.vending", "com.android.tv.settings");
    /** Packages that belong in the "TV tools" row instead of among your apps. */
    private static final String[] TOOL_PREFIXES = {"com.android.", "com.mediatek.", "com.haier.",
            "com.google.android.apps.tv.", "com.google.android.tv", "android."};

    private static final class App {
        String label, pkg;
        ComponentName cn;
        Drawable banner, icon;
        boolean system;
    }

    private final Handler ui = new Handler(Looper.getMainLooper());
    private HeaderView header;
    private StatusBar status;
    private ScrollView scroll;
    private FrameLayout rootView;
    private LinearLayout contentCol;
    private ContextMenu ctxMenu;
    private TextView focusLabel;
    private View labelTile;      // the tile whose name is showing
    private LinearLayout grid;
    private float dn;
    private boolean loading, showHidden, moving;
    private String movingPkg, lastFocusPkg;
    private List<App> all = new ArrayList<App>();          // every launchable app, in your order
    private final Set<String> hidden = new HashSet<String>();
    private final Map<String, String> custom = new HashMap<String, String>();   // pkg -> name you gave it
    private final Map<String, View> tiles = new HashMap<String, View>();
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        dn = getResources().getDisplayMetrics().density;
        if (CrashGuard.tripped(this)) { showEmergency(); return; }
        prefs = getSharedPreferences("layout", MODE_PRIVATE);
        LayoutBackup.importPending(this);     // a layout.json pushed from the Mac (see restore.sh)
        loadHidden();
        loadNames();
        Fixtures.init(this);
        try { startForegroundService(new Intent(this, AppService.class)); } catch (Throwable ignored) { }
        LayoutBackup.export(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF060A18);
        root.addView(new SaverView.Stripes(this, 0.4f), new FrameLayout.LayoutParams(-1, -1));   // calmer than the screensaver

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        col.setPadding(px(48), px(16), px(48), px(8));

        header = new HeaderView(this);
        col.addView(header, new LinearLayout.LayoutParams(-1, -2));

        View bar = new View(this);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, new int[]{GARNET, BLUE});
        g.setCornerRadius(px(2));
        bar.setBackground(g);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, px(3));
        bp.topMargin = px(10);
        bp.bottomMargin = px(8);
        col.addView(bar, bp);

        status = new StatusBar(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.bottomMargin = px(2);
        col.addView(status, sp);

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, px(6), 0, px(6));
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(grid, new FrameLayout.LayoutParams(-1, -2));
        col.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        rootView = root;
        contentCol = col;
        ctxMenu = new ContextMenu(this, root, col);

        // the app's name floats above whichever tile has the focus (the tiles themselves carry no text)
        focusLabel = new TextView(this);
        focusLabel.setTextColor(0xFFFFFFFF);
        focusLabel.setTypeface(Typeface.DEFAULT_BOLD);
        focusLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        focusLabel.setSingleLine(true);
        focusLabel.setEllipsize(TextUtils.TruncateAt.END);
        focusLabel.setIncludeFontPadding(false);
        focusLabel.setTextDirection(View.TEXT_DIRECTION_RTL);
        focusLabel.setPadding(px(15), px(6), px(15), px(7));
        GradientDrawable lb = new GradientDrawable();
        lb.setColor(0xF2141C33);
        lb.setCornerRadius(px(20));
        lb.setStroke(Math.max(1, px(1)), 0x33FFFFFF);
        focusLabel.setBackground(lb);
        focusLabel.setElevation(px(14));
        focusLabel.setAlpha(0f);
        focusLabel.setMaxWidth(px(320));
        root.addView(focusLabel, new FrameLayout.LayoutParams(-2, -2));
        scroll.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override public void onScrollChange(View v, int x, int y, int ox, int oy) { placeLabel(); }
        });
        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        if (header == null) return;                          // emergency screen is showing
        ui.postDelayed(calm, 60000);                         // a full calm minute clears the crash counter
        header.start();
        status.start();
        loadApps();
    }

    @Override protected void onPause() {
        ui.removeCallbacks(calm);
        if (header == null) { super.onPause(); return; }
        if (ctxMenu != null) ctxMenu.dismissNow();
        if (moving) endMove(true);
        header.stop();
        status.stop();
        super.onPause();
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (CrashGuard.tripped(this)) { recreate(); return; }
        // Home pressed again: jump back to the top
        lastFocusPkg = null;
        scroll.fullScroll(View.FOCUS_UP);
        grid.requestFocus();
    }

    @Override public void onBackPressed() {
        if (ctxMenu != null && ctxMenu.isOpen()) { ctxMenu.close(null); return; }
        if (moving) { endMove(true); return; }
        if (showHidden) { showHidden = false; fill(); return; }
        /* this is the home screen */
    }

    private final Runnable calm = new Runnable() { public void run() { CrashGuard.reset(HomeActivity.this); } };

    /** Shown instead of the normal screen after three crashes in two minutes: gives you a way out. */
    private void showEmergency() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        col.setBackgroundColor(0xFF060A18);
        col.setPadding(px(80), px(40), px(80), px(40));
        TextView t = new TextView(this);
        t.setText("الواجهة تعطّلت عدة مرات");
        t.setTextColor(GOLD);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        t.setGravity(Gravity.CENTER);
        col.addView(t);
        TextView m = new TextView(this);
        m.setText("اختر وش تسوي. ما ضاع شي من تعديلاتك.\n" + CrashGuard.lastError(this));
        m.setTextColor(MUTED);
        m.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        m.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-2, -2);
        mp.topMargin = px(10);
        mp.bottomMargin = px(26);
        col.addView(m, mp);
        View first = emergencyButton(col, "إعادة المحاولة", BLUE, new Runnable() { public void run() { CrashGuard.reset(HomeActivity.this); recreate(); } });
        emergencyButton(col, "الرجوع لواجهة Google TV (زر Enable)", 0xFF1A73E8, new Runnable() { public void run() { openGoogleTvPage(); } });
        emergencyButton(col, "الإعدادات", 0xFF2A3358, new Runnable() { public void run() {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Throwable ignored) { }
        } });
        setContentView(col);
        first.requestFocus();
    }

    private View emergencyButton(LinearLayout col, String text, final int color, final Runnable action) {
        final TextView b = new TextView(this);
        b.setText(text);
        b.setTextColor(0xFFFFFFFF);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setClickable(true);
        final GradientDrawable idle = new GradientDrawable();
        idle.setColor(color);
        idle.setCornerRadius(px(14));
        final GradientDrawable on = new GradientDrawable();
        on.setColor(color);
        on.setCornerRadius(px(14));
        on.setStroke(px(3), 0xFFFFFFFF);
        b.setBackground(idle);
        b.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean f) { b.setBackground(f ? on : idle); }
        });
        b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { action.run(); } });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(420), px(52));
        lp.topMargin = px(10);
        col.addView(b, lp);
        return b;
    }

    /** Opens Google TV's launcher page in Settings - it has the Enable button once it is disabled. */
    private void openGoogleTvPage() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.google.android.apps.tv.launcherx"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable t) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Throwable ignored) { }
        }
    }

    private int px(float dp) { return Math.round(dp * dn); }

    // ---- saved layout -----------------------------------------------------------------------

    private void loadHidden() {
        hidden.clear();
        String s = prefs.getString("hidden", "");
        if (s.length() > 0) hidden.addAll(Arrays.asList(s.split(",")));
    }

    private void saveHidden() {
        prefs.edit().putString("hidden", TextUtils.join(",", hidden)).apply();
        LayoutBackup.export(this);
    }

    private void loadNames() {
        custom.clear();
        try {
            JSONObject o = new JSONObject(prefs.getString("names", "{}"));
            java.util.Iterator<String> it = o.keys();
            while (it.hasNext()) { String k = it.next(); custom.put(k, o.optString(k)); }
        } catch (Throwable ignored) { }
    }

    private void saveNames() {
        JSONObject o = new JSONObject();
        try { for (Map.Entry<String, String> e : custom.entrySet()) o.put(e.getKey(), e.getValue()); } catch (Throwable ignored) { }
        prefs.edit().putString("names", o.toString()).apply();
        LayoutBackup.export(this);
    }

    private String nameOf(App a) {
        String c = custom.get(a.pkg);
        return c != null && c.length() > 0 ? c : a.label;
    }

    private void saveOrder() {
        List<String> p = new ArrayList<String>();
        for (App a : all) p.add(a.pkg);
        prefs.edit().putString("order", TextUtils.join(",", p)).apply();
        LayoutBackup.export(this);
    }

    private void sortApps(List<App> list) {
        String saved = prefs.getString("order", "");
        final List<String> order = saved.length() == 0 ? new ArrayList<String>() : Arrays.asList(saved.split(","));
        Collections.sort(list, new Comparator<App>() {
            public int compare(App x, App y) {
                int ox = order.indexOf(x.pkg), oy = order.indexOf(y.pkg);
                if (ox >= 0 && oy >= 0) return ox - oy;
                if (ox >= 0) return -1;     // apps you have placed come first
                if (oy >= 0) return 1;
                int ix = PRESET.indexOf(x.pkg), iy = PRESET.indexOf(y.pkg);
                if (ix < 0) ix = 999;
                if (iy < 0) iy = 999;
                return ix != iy ? ix - iy : x.label.compareToIgnoreCase(y.label);
            }
        });
    }

    private static boolean isTool(String pkg) {
        for (String p : TOOL_PREFIXES) if (pkg.startsWith(p)) return true;
        return false;
    }

    // ---- apps -------------------------------------------------------------------------------

    private void loadApps() {
        if (loading) return;
        loading = true;
        final Context c = this;
        new Thread(new Runnable() {
            public void run() {
                final List<App> list = new ArrayList<App>();
                try {
                    PackageManager pm = c.getPackageManager();
                    Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                    for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                        if (ri.activityInfo.packageName.equals(c.getPackageName())) continue;
                        App a = new App();
                        a.pkg = ri.activityInfo.packageName;
                        a.cn = new ComponentName(a.pkg, ri.activityInfo.name);
                        a.label = String.valueOf(ri.loadLabel(pm));
                        a.banner = ri.activityInfo.loadBanner(pm);
                        a.icon = ri.loadIcon(pm);
                        a.system = (ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                        list.add(a);
                    }
                    sortApps(list);
                } catch (Throwable ignored) { }
                ui.post(new Runnable() {
                    public void run() {
                        loading = false;
                        if (!moving) { all = list; fill(); }
                    }
                });
            }
        }).start();
    }

    private List<App> visible() {
        List<App> v = new ArrayList<App>();
        for (App a : all) if (showHidden || !hidden.contains(a.pkg)) v.add(a);
        return v;
    }

    private List<App> group(boolean tools) {
        List<App> g = new ArrayList<App>();
        for (App a : visible()) if (isTool(a.pkg) == tools) g.add(a);
        return g;
    }

    private void fill() {
        View f = getCurrentFocus();
        String want = f != null && f.getTag() instanceof String ? (String) f.getTag() : lastFocusPkg;
        if (moving) want = movingPkg;
        final int scrollY = scroll.getScrollY();

        grid.removeAllViews();
        tiles.clear();
        labelTile = null;
        if (focusLabel != null) { focusLabel.animate().cancel(); focusLabel.setAlpha(0f); }
        List<App> mine = group(false);
        if (hidden.size() > 0) {
            App t = new App();
            t.pkg = HIDDEN;
            t.label = showHidden ? "إخفاء المخفية" : "المخفية (" + hidden.size() + ")";
            mine.add(t);
        }
        addRows(mine);
        List<App> tools = group(true);
        if (!tools.isEmpty()) {
            TextView head = new TextView(this);
            head.setText("أدوات التلفزيون");
            head.setTextColor(MUTED);
            head.setTypeface(Typeface.DEFAULT_BOLD);
            head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-2, -2);
            hp.setMargins(px(9), px(4), 0, px(16));
            grid.addView(head, hp);
            addRows(tools);
        }
        grid.setClipChildren(false);

        View target = want == null ? null : tiles.get(want);
        if (target != null) target.requestFocus(); else grid.requestFocus();
        scroll.post(new Runnable() { public void run() { scroll.scrollTo(0, scrollY); } });
    }

    private void addRows(List<App> items) {
        LinearLayout row = null;
        for (int i = 0; i < items.size(); i++) {
            if (i % COLUMNS == 0) {
                row = new LinearLayout(this);
                row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                row.setClipChildren(false);
                grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
            row.addView(tile(items.get(i)));
        }
    }

    private View tile(final App a) {
        final boolean special = HIDDEN.equals(a.pkg);
        final boolean dim = !special && hidden.contains(a.pkg);
        final boolean isMoving = moving && a.pkg.equals(movingPkg);
        final int w = px(126), h = px(71);

        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setClipChildren(false);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-2, -2);
        cl.setMargins(px(9), px(0), px(9), px(5));
        cell.setLayoutParams(cl);
        cell.addView(new View(this), new LinearLayout.LayoutParams(1, px(LABEL_SLOT)));   // room for the floating name

        final FrameLayout t = new FrameLayout(this);
        t.setFocusable(true);
        t.setClickable(true);
        t.setTag(a.pkg);
        t.setAlpha(dim ? 0.4f : 1f);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF151D38);
        bg.setCornerRadius(px(12));
        t.setBackground(bg);
        t.setClipToOutline(true);
        t.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), px(12)); }
        });

        if (special) {
            t.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF2A3358, 0xFF1B2240}));
            TextView g = new TextView(this);
            g.setText(showHidden ? "رجوع" : hidden.size() + " مخفية");
            g.setTextColor(0xFFFFFFFF);
            g.setTypeface(Typeface.DEFAULT_BOLD);
            g.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            g.setGravity(Gravity.CENTER);
            t.addView(g, new FrameLayout.LayoutParams(-1, -1));
        } else {
            ImageView iv = new ImageView(this);
            if (a.banner != null) {
                iv.setImageDrawable(a.banner);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                iv.setImageDrawable(a.icon);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iv.setPadding(px(14), px(9), px(14), px(9));
            }
            t.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        }

        final GradientDrawable ring = new GradientDrawable();   // drawn over the tile, never changes its size
        ring.setColor(0);
        ring.setCornerRadius(px(12));
        ring.setStroke(px(2.5f), isMoving ? GOLD : 0xFFFFFFFF);
        ring.setAlpha(isMoving ? 255 : 0);
        t.setForeground(ring);
        if (isMoving) { t.setScaleX(1.15f); t.setScaleY(1.15f); t.setElevation(px(12)); }
        cell.addView(t, new LinearLayout.LayoutParams(w, h));

        t.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean f) {
                if (f) lastFocusPkg = a.pkg;
                if (moving && a.pkg.equals(movingPkg)) return;
                t.animate().scaleX(f ? 1.1f : 1f).scaleY(f ? 1.1f : 1f).setDuration(120).start();
                t.setElevation(f ? px(10) : 0);
                ring.setAlpha(f ? 255 : 0);
                if (f) showLabel(t, special ? a.label : nameOf(a)); else hideLabel(t);
            }
        });
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (moving) { endMove(true); return; }
                if (HIDDEN.equals(a.pkg)) { showHidden = !showHidden; fill(); }
                else launch(a);
            }
        });
        if (!special) {
            t.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    if (!moving) appMenu(a);
                    return true;
                }
            });
        }
        tiles.put(a.pkg, t);
        return cell;
    }

    // ---- the floating name ------------------------------------------------------------------

    private void showLabel(View tile, String name) {
        labelTile = tile;
        focusLabel.setText(name);
        focusLabel.animate().cancel();
        placeLabel();
        ui.postDelayed(new Runnable() { public void run() { if (labelTile != null) placeLabel(); } }, 90);   // after a rebuilt grid has been laid out
        focusLabel.animate().alpha(1f).setDuration(140).start();
    }

    private void hideLabel(View tile) {
        if (labelTile != tile) return;
        labelTile = null;
        focusLabel.animate().cancel();
        focusLabel.animate().alpha(0f).setDuration(100).start();
    }

    /** Centres the name above the focused tile, kept inside the screen. */
    private void placeLabel() {
        final View t = labelTile;
        if (t == null || rootView == null || !t.isLaidOut() || t.getWidth() == 0) return;
        focusLabel.measure(View.MeasureSpec.makeMeasureSpec(px(320), View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED);
        int[] a = new int[2], r = new int[2];
        t.getLocationInWindow(a);
        rootView.getLocationInWindow(r);
        float cx = a[0] - r[0] + t.getWidth() / 2f;
        float x = cx - focusLabel.getMeasuredWidth() / 2f;
        x = Math.max(px(24), Math.min(rootView.getWidth() - px(24) - focusLabel.getMeasuredWidth(), x));
        float y = a[1] - r[1] - focusLabel.getMeasuredHeight() - px(4) - t.getHeight() * 0.04f;
        focusLabel.setTranslationX(x);
        focusLabel.setTranslationY(y);
    }

    // ---- menu / move / rename / hide / uninstall --------------------------------------------

    private void appMenu(final App a) {
        final boolean isHidden = hidden.contains(a.pkg);
        final View tile = tiles.get(a.pkg);
        if (tile == null) return;
        java.util.List<Sheet.Item> items = Sheet.list();
        items.add(Sheet.Item.of("نقل", R.drawable.ic_move, new Runnable() { public void run() { startMove(a); } }));
        items.add(Sheet.Item.of("إعادة تسمية", R.drawable.ic_edit, new Runnable() { public void run() { rename(a); } }));
        items.add(Sheet.Item.of(isHidden ? "إظهار" : "إخفاء", isHidden ? R.drawable.ic_eye : R.drawable.ic_eye_off,
                new Runnable() { public void run() {
                    if (hidden.contains(a.pkg)) hidden.remove(a.pkg); else hidden.add(a.pkg);
                    saveHidden();
                    if (hidden.isEmpty()) showHidden = false;
                    lastFocusPkg = null;
                    fill();
                } }));
        if (a.system) {
            items.add(Sheet.Item.of("معلومات التطبيق", R.drawable.ic_info, new Runnable() { public void run() { appDetails(a.pkg); } }));
        } else {
            items.add(Sheet.Item.of("حذف", R.drawable.ic_trash, new Runnable() { public void run() { uninstall(a.pkg); } }).danger());
        }
        // the lifted copy of the tile
        ImageView iv = new ImageView(this);
        if (a.banner != null && a.banner.getConstantState() != null) {
            iv.setImageDrawable(a.banner.getConstantState().newDrawable().mutate());
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            iv.setImageDrawable(a.icon);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setPadding(px(14), px(9), px(14), px(9));
        }
        focusLabel.animate().cancel();
        focusLabel.setAlpha(0f);                                   // the menu has its own header
        ctxMenu.show(tile, iv, nameOf(a), items, new Runnable() {
            public void run() {
                View t = tiles.get(a.pkg);
                if (t != null) t.requestFocus();
            }
        });
    }

    private void rename(final App a) {
        RenameDialog.show(this, nameOf(a), a.label, new RenameDialog.Callback() {
            @Override public void onName(String name) {
                if (name == null || name.trim().length() == 0 || name.trim().equals(a.label)) custom.remove(a.pkg);
                else custom.put(a.pkg, name.trim());
                saveNames();
                lastFocusPkg = a.pkg;
                fill();
            }
        });
    }

    private void startMove(App a) {
        if (hidden.contains(a.pkg) && !showHidden) return;
        moving = true;
        movingPkg = a.pkg;
        Toast.makeText(this, "حرّك بالأسهم، وOK لتثبيت المكان", Toast.LENGTH_LONG).show();
        fill();
    }

    private void endMove(boolean save) {
        moving = false;
        movingPkg = null;
        if (save) saveOrder();
        fill();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (!moving) return super.dispatchKeyEvent(e);
        int k = e.getKeyCode();
        if (k == KeyEvent.KEYCODE_BACK || k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) {
            if (e.getAction() == KeyEvent.ACTION_UP) endMove(true);
            return true;
        }
        int delta;   // the grid runs right-to-left
        switch (k) {
            case KeyEvent.KEYCODE_DPAD_LEFT: delta = 1; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: delta = -1; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: delta = COLUMNS; break;
            case KeyEvent.KEYCODE_DPAD_UP: delta = -COLUMNS; break;
            default: return true;
        }
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            List<App> g = group(isTool(movingPkg));       // an app only moves inside its own row-group
            int i = -1;
            for (int n = 0; n < g.size(); n++) if (g.get(n).pkg.equals(movingPkg)) i = n;
            int j = i + delta;
            if (i >= 0 && j >= 0 && j < g.size()) {
                int ia = all.indexOf(g.get(i)), ib = all.indexOf(g.get(j));
                Collections.swap(all, ia, ib);
                fill();
            }
        }
        return true;
    }

    private void uninstall(String pkg) {
        try {
            startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable t) {
            appDetails(pkg);
        }
    }

    private void appDetails(String pkg) {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable ignored) { }
    }

    private void launch(App a) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    .setComponent(a.cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Throwable ignored) { }
    }
}
