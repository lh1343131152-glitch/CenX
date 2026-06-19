package com.centigrade.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.centigrade.browser.R;

public class HyperRootLayout extends LinearLayout {

    public HyperRootLayout(Context context) {
        super(context);
        init(context, null);
    }

    public HyperRootLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HyperRootLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);

        TypedValue surfaceContainerValue = new TypedValue();
        if (context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainer, surfaceContainerValue, true)) {
          //  setBackgroundColor(surfaceContainerValue.data);
        } else {
            setBackgroundColor(ContextCompat.getColor(context, R.color.background));
        }

        boolean useDefaultPadding = false;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HyperRootLayout);
            useDefaultPadding = a.getBoolean(R.styleable.HyperRootLayout_hyperUseDefaultPadding, false);
            a.recycle();
        }

        if (useDefaultPadding) {
            int horizontal = dp(16);
            int vertical = dp(10);
            setPadding(horizontal, vertical, horizontal, vertical);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}