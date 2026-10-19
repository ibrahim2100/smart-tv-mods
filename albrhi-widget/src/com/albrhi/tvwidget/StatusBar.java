package com.albrhi.tvwidget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.albrhi.tvwidget.WidgetView.*;

/** Row above the app grid: "My apps" title, HDMI inputs (one press to switch) and the internet status. */
final class StatusBar extends LinearLayout {
    private static final int GOOD = 0xFF34C759, WARN = 0xFFFFB020, BAD = 0xFFFF453A;
    private static final String TV_APP = "com.mediatek.wwtv.tvcenter";
    private static final String TV_ACTIVITY = "com.mediatek.wwtv.tvcenter.nav.TurnkeyUiMainActivity";

    private final float dn;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final TvInputManager tim;
    private final ConnectivityManager cm;
    private final WifiManager wm;
    private LinearLayout hdmiBox;
    private LinearLayout netPill;
    private Bars bars;
    private TextView netTitle, netSub, healthText;
    private View healthFrame;
    private volatile int pingMs = -2;      // -2 unknown yet, -1 failed
    private boolean pinging;

    private final Runnable tick = new Runnable() {
        public void run() {
            refreshNet();
            refreshHealth();
            h.postDelayed(this, 10000);
        }
    };
    private final Runnable pingTick = new Runnable() {
        public void run() { ping(); h.postDelayed(this, 30000); }
    };
    private final TvInputManager.TvInputCallback inputCb = new TvInputManager.TvInputCallback() {
        @Override public void onInputStateChanged(String id, int state) { refreshInputs(); }
        @Override public void onInputAdded(String id) { refreshInputs(); }
        @Override public void onInputRemoved(String id) { refreshInputs(); }
        @Override public void onInputUpdated(String id) { refreshInputs(); }
    };

    StatusBar(Context c) {
        super(c);
        dn = c.getResources().getDisplayMetrics().density;
        tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
        cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        build();
    }

    void start() {
        try { if (tim != null) tim.registerCallback(inputCb, h); } catch (Throwable ignored) { }
        refreshInputs();
        h.removeCallbacks(tick);
        h.removeCallbacks(pingTick);
        tick.run();
        pingTick.run();
    }

    void stop() {
        h.removeCallbacks(tick);
        h.removeCallbacks(pingTick);
        try { if (tim != null) tim.unregisterCallback(inputCb); } catch (Throwable ignored) { }
    }

    private int px(float dp) { return Math.round(dp * dn); }

    private TextView tv(float sp, int color, boolean bold) {
        TextView t = new TextView(getContext());
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Fonts.bold);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private GradientDrawable round(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(px(r));
        return g;
    }

    private void build() {
        setOrientation(HORIZONTAL);
        setLayoutDirection(LAYOUT_DIRECTION_RTL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClipChildren(false);

        TextView title = tv(14, MUTED, true);
        title.setText("تطبيقاتي");
        addView(title, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        // low memory / storage warning (only visible when something is wrong)
        LinearLayout hp0 = new LinearLayout(getContext());
        hp0.setGravity(Gravity.CENTER_VERTICAL);
        hp0.setBackground(round(0x33FFB020, 10));
        hp0.setPadding(px(11), px(6), px(12), px(6));
        View hdot = new View(getContext());
        GradientDrawable hd = new GradientDrawable();
        hd.setShape(GradientDrawable.OVAL);
        hd.setColor(WARN);
        hdot.setBackground(hd);
        hp0.addView(hdot, new LinearLayout.LayoutParams(px(8), px(8)));
        healthText = tv(13.5f, WARN, true);
        LinearLayout.LayoutParams htp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        htp.setMarginStart(px(7));
        hp0.addView(healthText, htp);
        healthFrame = focusable(hp0, new Runnable() { public void run() { openSettings(Settings.ACTION_APPLICATION_SETTINGS); } });
        healthFrame.setVisibility(GONE);
        LinearLayout.LayoutParams hfl = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        hfl.setMarginEnd(px(12));
        addView(healthFrame, hfl);

        // upcoming matches
        LinearLayout mc = new LinearLayout(getContext());
        mc.setGravity(Gravity.CENTER_VERTICAL);
        mc.setBackground(round(0xFF0F1730, 10));
        mc.setPadding(px(12), px(7), px(14), px(7));
        TextView mt = tv(14, WHITE, true);
        mt.setText("⚽  المباريات");
        mc.addView(mt, wrap());
        LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        ml.setMarginEnd(px(12));
        addView(focusable(mc, new Runnable() { public void run() { matchesMenu(); } }), ml);

        // quick settings
        LinearLayout gear = new LinearLayout(getContext());
        gear.setGravity(Gravity.CENTER_VERTICAL);
        gear.setBackground(round(0xFF0F1730, 10));
        gear.setPadding(px(12), px(7), px(14), px(7));
        ImageView gi = new ImageView(getContext());
        gi.setImageResource(android.R.drawable.ic_menu_preferences);
        gi.setColorFilter(WHITE, PorterDuff.Mode.SRC_IN);
        gear.addView(gi, new LinearLayout.LayoutParams(px(18), px(18)));
        TextView gt = tv(14, WHITE, true);
        gt.setText("الإعدادات");
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        gp.setMarginStart(px(7));
        gear.addView(gt, gp);
        LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        gl.setMarginEnd(px(12));
        addView(focusable(gear, new Runnable() { public void run() { quickSettings(); } }), gl);

        hdmiBox = new LinearLayout(getContext());
        hdmiBox.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        hdmiBox.setClipChildren(false);
        hdmiBox.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        hp.setMarginEnd(px(14));
        addView(hdmiBox, hp);

        netPill = new LinearLayout(getContext());
        netPill.setGravity(Gravity.CENTER_VERTICAL);
        netPill.setBackground(round(0xFF0F1730, 10));
        netPill.setPadding(px(10), px(5), px(12), px(5));
        bars = new Bars(getContext());
        netPill.addView(bars, new LinearLayout.LayoutParams(px(20), px(16)));
        LinearLayout txt = new LinearLayout(getContext());
        txt.setOrientation(VERTICAL);
        netTitle = tv(14, WHITE, true);
        netSub = tv(12.5f, MUTED, false);
        txt.addView(netTitle, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        txt.addView(netSub, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        tp.setMarginStart(px(9));
        netPill.addView(txt, tp);
        addView(focusable(netPill, new Runnable() { public void run() { openSettings(Settings.ACTION_WIFI_SETTINGS); } }),
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    /** Wraps a pill so the remote can focus it (white ring + slight zoom) and press OK. */
    private FrameLayout focusable(View content, final Runnable click) {
        final FrameLayout f = new FrameLayout(getContext());
        f.setFocusable(true);
        f.setClickable(true);
        f.addView(content);
        final GradientDrawable ring = new GradientDrawable();
        ring.setColor(0);
        ring.setCornerRadius(px(10));
        ring.setStroke(px(2), 0xFFFFFFFF);
        ring.setAlpha(0);
        f.setForeground(ring);
        f.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                f.animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f).setDuration(120).start();
                ring.setAlpha(has ? 255 : 0);
            }
        });
        f.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { click.run(); } });
        return f;
    }

    private void openSettings(String action) {
        try {
            getContext().startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable ignored) { }
    }

    private Runnable open(final String action) {
        return new Runnable() { public void run() { openSettings(action); } };
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    private void matchesMenu() { MatchesDialog.show(getContext()); }

    private void openGoogleTvPage() {
        try {
            getContext().startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:com.google.android.apps.tv.launcherx")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable t) { openSettings(Settings.ACTION_SETTINGS); }
    }

    private void quickSettings() { QuickSettings.show(getContext()); }

    // ---- HDMI -------------------------------------------------------------------------------

    private SharedPreferences names() { return getContext().getSharedPreferences("hdmi", Context.MODE_PRIVATE); }

    private void refreshInputs() {
        hdmiBox.removeAllViews();
        if (tim == null) return;
        List<TvInputInfo> ports = new ArrayList<TvInputInfo>();
        java.util.Map<String, String> cecByPort = new java.util.HashMap<String, String>();   // port input id -> device name
        try {
            for (TvInputInfo i : tim.getTvInputList()) {
                if (i.getType() != TvInputInfo.TYPE_HDMI) continue;
                String parent = i.getParentId();
                if (parent == null) ports.add(i);
                else {                                   // a device that answered over HDMI-CEC
                    CharSequence l = i.loadLabel(getContext());
                    if (l != null && l.length() > 0) cecByPort.put(parent, l.toString());
                }
            }
        } catch (Throwable t) { return; }
        Collections.sort(ports, new Comparator<TvInputInfo>() {
            public int compare(TvInputInfo a, TvInputInfo b) { return a.getId().compareTo(b.getId()); }
        });
        int n = 0;
        for (TvInputInfo i : ports) {
            n++;
            int state = TvInputManager.INPUT_STATE_DISCONNECTED;
            try { state = tim.getInputState(i.getId()); } catch (Throwable ignored) { }
            String cec = cecByPort.get(i.getId());
            if (cec != null) state = TvInputManager.INPUT_STATE_CONNECTED;
            hdmiBox.addView(chip(i, n, state, cec));
        }
    }

    /** Turns what a device reports over CEC ("Playstation 5", "Denon AVR") into a short friendly name. */
    static String friendly(String raw) {
        String s = raw.toLowerCase(java.util.Locale.ROOT);
        if (s.contains("playstation") || s.contains("ps5") || s.contains("ps4") || s.contains("sony interactive")) return s.contains("4") ? "PS4" : "PS5";
        if (s.contains("xbox")) return "Xbox";
        if (s.contains("apple")) return "Apple TV";
        if (s.contains("fire") || s.contains("amazon")) return "Fire TV";
        if (s.contains("chromecast") || s.contains("google tv")) return "Chromecast";
        if (s.contains("nintendo") || s.contains("switch")) return "Switch";
        if (s.contains("denon") || s.contains("yamaha") || s.contains("onkyo") || s.contains("marantz") || s.contains("pioneer")
                || s.contains("avr") || s.contains("receiver") || s.contains("soundbar") || s.contains("sound bar")) return "ريسيفر";
        return raw;
    }

    private String label(TvInputInfo i, int n, String cec) {
        String saved = names().getString(i.getId(), null);
        if (saved != null && saved.length() > 0) return saved;           // you named it
        if (cec != null) return friendly(cec);                           // the device told us what it is
        return "HDMI " + n;
    }

    private void renameMenu(final TvInputInfo info, final int n) {
        final String[] presets = {"PS5", "PS4", "Xbox", "ريسيفر", "Apple TV", "Fire TV", "Chromecast", "Switch", "لابتوب", "كاميرا"};
        java.util.List<Sheet.Item> items = Sheet.list();
        for (final String name : presets) {
            items.add(Sheet.Item.of(name, R.drawable.ic_tv, new Runnable() { public void run() { saveName(info.getId(), name); } }));
        }
        items.add(Sheet.Item.of("الاسم الافتراضي", R.drawable.ic_refresh, new Runnable() { public void run() { saveName(info.getId(), null); } })
                .sub("HDMI " + n));
        Sheet.show(getContext(), "سمّ هذا المدخل", "HDMI " + n, R.drawable.ic_tv, items, null, null);
    }

    private void saveName(String id, String name) {
        SharedPreferences.Editor e = names().edit();
        if (name == null) e.remove(id); else e.putString(id, name);
        e.apply();
        LayoutBackup.export(getContext());
        refreshInputs();
    }

    private View chip(final TvInputInfo info, final int n, int state, String cec) {
        final FrameLayout f = new FrameLayout(getContext());
        f.setFocusable(true);
        f.setClickable(true);
        f.setBackground(round(0xFF0F1730, 10));
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(px(10), px(6), px(12), px(6));
        View dot = new View(getContext());
        int color = state == TvInputManager.INPUT_STATE_CONNECTED ? GOOD
                : state == TvInputManager.INPUT_STATE_CONNECTED_STANDBY ? WARN : 0x55FFFFFF;
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        dot.setBackground(g);
        if (state == TvInputManager.INPUT_STATE_CONNECTED) row.addView(dot, new LinearLayout.LayoutParams(px(8), px(8)));
        TextView t = tv(14, state == TvInputManager.INPUT_STATE_DISCONNECTED ? MUTED : WHITE, true);
        t.setText(label(info, n, cec));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.leftMargin = state == TvInputManager.INPUT_STATE_CONNECTED ? px(7) : 0;
        row.addView(t, lp);
        f.addView(row);
        final GradientDrawable ring = new GradientDrawable();   // drawn over the chip, so it never changes its size
        ring.setColor(0);
        ring.setCornerRadius(px(10));
        ring.setStroke(px(2), 0xFFFFFFFF);
        ring.setAlpha(0);
        f.setForeground(ring);
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        fl.leftMargin = px(5);
        fl.rightMargin = px(5);
        f.setLayoutParams(fl);
        f.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                f.animate().scaleX(has ? 1.1f : 1f).scaleY(has ? 1.1f : 1f).setDuration(120).start();
                ring.setAlpha(has ? 255 : 0);
            }
        });
        f.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTo(info.getId()); }
        });
        f.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { renameMenu(info, n); return true; }
        });
        return f;
    }

    private void refreshHealth() {
        Health.Info i = Health.read(getContext());
        if (i.warning == null) { healthFrame.setVisibility(GONE); return; }
        healthText.setText(i.warning);
        healthFrame.setVisibility(VISIBLE);
    }

    private void switchTo(String inputId) {
        Context c = getContext();
        Intent i = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputId))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(new Intent(i).setComponent(new ComponentName(TV_APP, TV_ACTIVITY)));   // this TV's Live TV app
        } catch (Throwable t) {
            try { c.startActivity(i); } catch (Throwable ignored) { }
        }
    }

    // ---- network ----------------------------------------------------------------------------

    private void refreshNet() {
        boolean connected = false, internet = false, wifi = false, eth = false;
        String name = null;
        int level = 0, mbps = 0;
        try {
            Network n = cm.getActiveNetwork();
            NetworkCapabilities nc = n == null ? null : cm.getNetworkCapabilities(n);
            if (nc != null) {
                connected = true;
                internet = nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                wifi = nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                eth = nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
            }
            if (wifi && wm != null) {
                WifiInfo wi = wm.getConnectionInfo();
                String s = wi.getSSID();
                if (s != null && !s.contains("unknown ssid")) name = s.replace("\"", "");
                level = wm.calculateSignalLevel(wi.getRssi());
                mbps = wi.getLinkSpeed();
            }
        } catch (Throwable ignored) { }

        int color;
        String title, sub;
        if (!connected) {
            color = BAD;
            title = "غير متصل";
            sub = "لا يوجد اتصال بالشبكة";
            level = 0;
        } else {
            title = eth ? "إيثرنت" : (name != null ? "Wi-Fi · " + name : "Wi-Fi");
            StringBuilder sb = new StringBuilder();
            if (internet) { sb.append("متصل بالإنترنت"); color = GOOD; }
            else { sb.append("شبكة بدون إنترنت"); color = WARN; }
            if (mbps > 0) sb.append(" · \u2066").append(mbps).append(" Mbps\u2069");
            if (internet && pingMs >= 0) sb.append(" · \u2066").append(pingMs).append(" ms\u2069");
            sub = sb.toString();
            if (eth) level = 4;
        }
        netTitle.setText(title);
        netSub.setText(sub);
        netSub.setTextColor(color);
        bars.set(level, color);
    }

    private void ping() {
        if (pinging) return;
        pinging = true;
        new Thread(new Runnable() {
            public void run() {
                int ms = -1;
                String[] hosts = {"1.1.1.1", "8.8.8.8"};
                for (int k = 0; k < hosts.length && ms < 0; k++) {
                    Socket s = new Socket();
                    try {
                        long t0 = System.nanoTime();
                        s.connect(new InetSocketAddress(hosts[k], 443), 2500);
                        ms = (int) ((System.nanoTime() - t0) / 1000000L);
                    } catch (Throwable ignored) {
                    } finally {
                        try { s.close(); } catch (Throwable ignored) { }
                    }
                }
                pingMs = ms;
                pinging = false;
                h.post(new Runnable() { public void run() { refreshNet(); } });
            }
        }).start();
    }

    /** Four rising bars, like a phone's signal indicator. */
    private static final class Bars extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int level, color = WHITE;

        Bars(Context c) { super(c); }

        void set(int level, int color) { this.level = level; this.color = color; invalidate(); }

        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), hh = getHeight(), bw = w / 7f;
            for (int i = 0; i < 4; i++) {
                float bh = hh * (0.35f + 0.65f * i / 3f);
                float x = i * bw * 1.75f;
                p.setColor(i < level ? color : 0x33FFFFFF);
                cv.drawRoundRect(new RectF(x, hh - bh, x + bw, hh), bw / 2, bw / 2, p);
            }
        }
    }
}
