package com.albrhi.tvwidget;

import android.app.Application;

public class AlbrhiApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        Fonts.init(this);
        CrashGuard.install(this);
    }
}
