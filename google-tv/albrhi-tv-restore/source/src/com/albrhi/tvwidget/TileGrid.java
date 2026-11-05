package com.albrhi.tvwidget;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

/**
 * The apps row. With one row it is a plain horizontal LinearLayout. With two, the first half of the children
 * sit on the top row and the rest on the second, so the order still reads left to right and a tile keeps the same
 * index (moving, hiding and focus code that counts children keep working). Both rows scroll together.
 */
final class TileGrid extends LinearLayout {
    private int rows = 1, firstRowH, secondRowH;

    TileGrid(Context c) { super(c); }

    void setRows(int r) {
        r = r >= 2 ? 2 : 1;
        if (r != rows) { rows = r; requestLayout(); }
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        if (rows < 2) { super.onMeasure(wSpec, hSpec); return; }
        int n = getChildCount(), first = (n + 1) / 2, w1 = 0, w2 = 0;
        firstRowH = 0;
        secondRowH = 0;
        for (int i = 0; i < n; i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == GONE) continue;
            v.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
            int cw = v.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int ch = v.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            if (i < first) { w1 += cw; firstRowH = Math.max(firstRowH, ch); }
            else { w2 += cw; secondRowH = Math.max(secondRowH, ch); }
        }
        int w = Math.max(w1, w2) + getPaddingLeft() + getPaddingRight();
        int h = firstRowH + secondRowH + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(w, wSpec), resolveSize(h, hSpec));
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (rows < 2) { super.onLayout(changed, l, t, r, b); return; }
        int n = getChildCount(), first = (n + 1) / 2;
        int x1 = getPaddingLeft(), x2 = getPaddingLeft();
        int y1 = getPaddingTop(), y2 = getPaddingTop() + firstRowH;
        for (int i = 0; i < n; i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == GONE) continue;
            MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
            boolean top = i < first;
            int x = (top ? x1 : x2) + lp.leftMargin, y = (top ? y1 : y2) + lp.topMargin;
            v.layout(x, y, x + v.getMeasuredWidth(), y + v.getMeasuredHeight());
            int adv = v.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            if (top) x1 += adv; else x2 += adv;
        }
    }
}
