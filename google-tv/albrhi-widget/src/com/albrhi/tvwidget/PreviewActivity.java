package com.albrhi.tvwidget;

import android.app.Activity;
import android.os.Bundle;

/** Debug helper: shows the screensaver as a normal screen (adb shell am start -n com.albrhi.tvwidget/.PreviewActivity). */
public class PreviewActivity extends Activity {
    private SaverView view;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Fixtures.init(this);
        view = new SaverView(this);
        setContentView(view);
    }

    @Override protected void onStart() { super.onStart(); view.start(); }

    @Override protected void onStop() { view.stop(); super.onStop(); }
}
