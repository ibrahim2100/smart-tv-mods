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

    static ControlService get() { return inst; }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        inst = this;
    }

    @Override public boolean onUnbind(android.content.Intent i) {
        inst = null;
        return super.onUnbind(i);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) { }

    /**
     * The remote's Settings (gear) button opens our short settings menu instead of the TV's full Settings app.
     * Every other key is passed on untouched (we only look at its code).
     */
    @Override protected boolean onKeyEvent(KeyEvent e) {
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
