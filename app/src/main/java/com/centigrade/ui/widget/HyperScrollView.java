package com.centigrade.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ScrollView;

import com.centigrade.browser.R;

public class HyperScrollView extends ScrollView {

    public HyperScrollView(Context context) {
        super(context);
        init(context, null);
    }

    public HyperScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HyperScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setFillViewport(true);

        boolean clipToPadding = false;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HyperScrollView);
            clipToPadding = a.getBoolean(R.styleable.HyperScrollView_hyperClipToPadding, false);
            a.recycle();
        }

        setClipToPadding(clipToPadding);
    }
}
