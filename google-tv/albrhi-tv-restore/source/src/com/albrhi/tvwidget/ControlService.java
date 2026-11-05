package com.albrhi.tvwidget;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Lets the phone move focus, press OK, go back/home and type text on the TV, using Android's
 * accessibility actions (no shell, no key injection). It only acts when the paired phone asks.
 * It does not read or store anything from the screen.
 */
public class ControlService extends AccessibilityService {
    private static volatile ControlService inst;

    static boolean ready() { return inst != null; }

    /**
     * Android switches an accessibility service off when its app is force-stopped. With the permission that restore.sh grants
     * (WRITE_SECURE_SETTINGS) the launcher switches it back on by itself, so your remote buttons never stay dead.
     */
    static void ensureEnabled(android.content.Context c) {
        try {
            if (c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
            String me = c.getPackageName() + "/" + ControlService.class.getName();
            android.content.ContentResolver cr = c.getContentResolver();
            String cur = android.provider.Settings.Secure.getString(cr, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (cur != null && cur.contains(me)) return;
            android.provider.Settings.Secure.putString(cr, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    cur == null || cur.length() == 0 ? me : cur + ":" + me);
            android.provider.Settings.Secure.putInt(cr, android.provider.Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        } catch (Throwable ignored) { }
    }

    static ControlService get() { return inst; }

    private android.content.BroadcastReceiver screenOff;

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        inst = this;
        if (screenOff == null) {        // the TV went to standby: switch the lights off too (see Lights.onTvOff)
            screenOff = new android.content.BroadcastReceiver() {
                @Override public void onReceive(android.content.Context c, Intent i) { Lights.onTvOff(c); }
            };
            try { registerReceiver(screenOff, new android.content.IntentFilter(Intent.ACTION_SCREEN_OFF)); } catch (Throwable ignored) { }
        }
    }

    @Override public boolean onUnbind(android.content.Intent i) {
        inst = null;
        if (screenOff != null) { try { unregisterReceiver(screenOff); } catch (Throwable ignored) { } screenOff = null; }
        return super.onUnbind(i);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && e.getPackageName() != null) {
            try { KeyMap.onWindow(this, e.getPackageName().toString()); } catch (Throwable ignored) { }
        }
    }

    /**
     * The remote's Settings (gear) button opens our short settings menu instead of the TV's full Settings app.
     * Every other key is passed on untouched (we only look at its code).
     */
    @Override protected boolean onKeyEvent(KeyEvent e) {
        try { if (KeyMap.onKey(this, e)) return true; } catch (Throwable ignored) { }      // your own remote buttons; never blocks the other keys
        int k = e.getKeyCode();
        // The Haier remote's gear button sends KEYCODE_NOTIFICATION (83); other remotes send KEYCODE_SETTINGS.
        if (k == KeyEvent.KEYCODE_SETTINGS || k == KeyEvent.KEYCODE_NOTIFICATION) {
            if (e.getAction() == KeyEvent.ACTION_UP) {
                try {
                    startActivity(new Intent(this, QuickSettingsActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                } catch (Throwable ignored) { }
            }
            return true;
        }
        return false;
    }

    @Override public void onInterrupt() { }

    // ---- force stop through the system's own app-info screen ---------------------------------
    // Some TVs refuse ActivityManager.killBackgroundProcesses from an ordinary app ("Invalid packageName"). The one thing
    // that always ends an app is the "Force stop" button in Settings > Apps, so we press it for you, one app at a time.

    private static final String[] FORCE_STOP = {"force stop", "إيقاف إجباري", "فرض الإيقاف", "فرض الإغلاق", "إيقاف التطبيق"};
    private static final String[] CONFIRM = {"ok", "force stop", "موافق", "حسنًا", "حسناً", "نعم", "إيقاف إجباري", "فرض الإيقاف"};
    private final android.os.Handler fsHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private java.util.ArrayList<String> fsQueue;
    private Runnable fsDone;
    private int fsStage;
    private long fsDeadline;

    /** @return false if the service is not running (nothing was started). {@code done} runs on the main thread when the queue is finished. */
    static boolean forceStop(java.util.List<String> pkgs, Runnable done) {
        ControlService s = inst;
        if (s == null || pkgs.isEmpty()) return false;
        s.fsQueue = new java.util.ArrayList<String>(pkgs);
        s.fsDone = done;
        s.fsNext();
        return true;
    }

    private void fsNext() {
        fsHandler.removeCallbacksAndMessages(null);
        if (fsQueue == null || fsQueue.isEmpty()) {
            fsQueue = null;
            performGlobalAction(GLOBAL_ACTION_HOME);
            Runnable d = fsDone;
            fsDone = null;
            if (d != null) d.run();
            return;
        }
        String pkg = fsQueue.remove(0);
        fsStage = 0;
        fsDeadline = android.os.SystemClock.uptimeMillis() + 9000;
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + pkg))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        } catch (Throwable t) { fsHandler.post(new Runnable() { public void run() { fsNext(); } }); return; }
        fsHandler.postDelayed(fsPoll, 900);
    }

    private final Runnable fsPoll = new Runnable() {
        public void run() {
            if (android.os.SystemClock.uptimeMillis() > fsDeadline) { fsNext(); return; }
            AccessibilityNodeInfo root = null;
            try {
                root = getRootInActiveWindow();
                if (root != null) {
                    if (fsStage == 0) {
                        AccessibilityNodeInfo hit = findText(root, FORCE_STOP, false);
                        if (hit != null) {
                            if (!hit.isEnabled() && !parentEnabled(hit)) { fsNext(); return; }        // already stopped: the button is greyed out
                            if (clickUp(hit)) { fsStage = 1; fsHandler.postDelayed(this, 600); return; }
                        }
                    } else {
                        AccessibilityNodeInfo ok = findText(root, CONFIRM, true);
                        if (ok != null && clickUp(ok)) { fsHandler.postDelayed(new Runnable() { public void run() { fsNext(); } }, 700); return; }
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (root != null) root.recycle();
            }
            fsHandler.postDelayed(this, 350);
        }
    };

    private static boolean parentEnabled(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo p = n.getParent();
        return p != null && p.isEnabled();
    }

    /** First node whose text matches one of the labels (exact match when {@code exact}, otherwise "contains"). */
    private static AccessibilityNodeInfo findText(AccessibilityNodeInfo root, String[] labels, boolean exact) {
        for (String l : labels) {
            java.util.List<AccessibilityNodeInfo> hits = root.findAccessibilityNodeInfosByText(l);
            if (hits == null) continue;
            for (AccessibilityNodeInfo h : hits) {
                CharSequence t = h.getText();
                if (t == null) continue;
                String s = t.toString().trim().toLowerCase(java.util.Locale.ROOT);
                if (exact ? s.equals(l) : (s.equals(l) || s.startsWith(l))) return h;
            }
        }
        return null;
    }

    /** Presses the node, or the nearest clickable parent of it (leanback rows are clickable containers around a text). */
    private static boolean clickUp(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur = n;
        for (int i = 0; i < 5 && cur != null; i++) {
            if (cur.isClickable() && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            cur = cur.getParent();
        }
        return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    // ---- actions ----------------------------------------------------------------------------

    boolean global(int action) { return performGlobalAction(action); }

    /** Moves the focus one step in the given View.FOCUS_* direction. */
    boolean move(int direction) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        try {
            AccessibilityNodeInfo cur = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (cur == null) cur = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            AccessibilityNodeInfo next = cur == null ? firstFocusable(root) : cur.focusSearch(direction);
            if (next == null) return false;
            boolean ok = next.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    || next.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            return ok;
        } finally {
            root.recycle();
        }
    }

    /** Presses OK on whatever has the focus. */
    boolean click() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        try {
            AccessibilityNodeInfo n = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (n == null) n = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            while (n != null) {                       // the focused view may sit inside the clickable one
                if (n.isClickable() && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
                n = n.getParent();
            }
            return false;
        } finally {
            root.recycle();
        }
    }

    /** Types text into the focused text field. */
    boolean text(String s) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        try {
            AccessibilityNodeInfo n = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (n == null || !n.isEditable()) return false;
            Bundle b = new Bundle();
            b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, s);
            return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
        } finally {
            root.recycle();
        }
    }

    private static AccessibilityNodeInfo firstFocusable(AccessibilityNodeInfo n) {
        if (n == null) return null;
        if (n.isFocusable() && n.isVisibleToUser()) return n;
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo r = firstFocusable(n.getChild(i));
            if (r != null) return r;
        }
        return null;
    }

    static int direction(String key) {
        if ("up".equals(key)) return View.FOCUS_UP;
        if ("down".equals(key)) return View.FOCUS_DOWN;
        if ("left".equals(key)) return View.FOCUS_LEFT;
        if ("right".equals(key)) return View.FOCUS_RIGHT;
        return 0;
    }
}
