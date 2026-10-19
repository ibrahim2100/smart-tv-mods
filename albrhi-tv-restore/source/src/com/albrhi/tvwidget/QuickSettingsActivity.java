package com.albrhi.tvwidget;

import android.app.Activity;
import android.os.Bundle;

/**
 * A see-through screen that only hosts the quick settings menu, so it can pop up over any app
 * (opened by the remote's Settings key). It closes itself with the menu.
 */
public class QuickSettingsActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        QuickSettings.show(this, new Runnable() { public void run() { finish(); } });
    }

    @Override protected void onStop() {
        super.onStop();
        if (!isFinishing()) finish();      // never leave an empty see-through screen behind
    }
}
