package com.albrhi.tvwidget;

import android.app.Application;

public class AlbrhiApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        CrashGuard.install(this);
    }
}
