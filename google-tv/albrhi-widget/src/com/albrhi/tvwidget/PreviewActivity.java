package com.albrhi.tvwidget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Shows the screensaver as a normal full screen: "show it now" from a remote button, and a preview for screenshots
 * (adb shell am start -n com.albrhi.tvwidget/.PreviewActivity). Any button (or a tap) closes it, like a real screensaver.
 */
public class PreviewActivity extends Activity {
    private static volatile PreviewActivity active;
    private SaverView view;

    /** Starts the saver, or closes it if it is already showing (so the same button turns it on and off). */
    static void toggle(Context c) {
        PreviewActivity a = active;
        if (a != null) { a.finish(); return; }
        try { c.startActivity(new Intent(c, PreviewActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION)); } catch (Throwable ignored) { }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Fixtures.init(this);
        view = new SaverView(this);
        setContentView(view);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override protected void onStart() { super.onStart(); active = this; view.start(); }

    @Override protected void onStop() {
        view.stop();
        if (active == this) active = null;
        super.onStop();
        if (!isFinishing()) finish();            // never leave the saver sitting behind another app
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent e) { finish(); return true; }

    @Override public boolean onTouchEvent(MotionEvent e) { if (e.getAction() == MotionEvent.ACTION_DOWN) finish(); return true; }
}
