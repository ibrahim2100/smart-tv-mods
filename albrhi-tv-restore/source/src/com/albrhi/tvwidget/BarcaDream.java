package com.albrhi.tvwidget;

import android.service.dreams.DreamService;

public class BarcaDream extends DreamService {
    private SaverView view;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        Fixtures.init(this);
        view = new SaverView(this);
        setContentView(view);
    }

    @Override public void onDreamingStarted() { super.onDreamingStarted(); view.start(); }

    @Override public void onDreamingStopped() { view.stop(); super.onDreamingStopped(); }
}
