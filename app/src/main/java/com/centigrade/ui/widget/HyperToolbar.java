package com.centigrade.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.Toolbar;

public class HyperToolbar extends Toolbar {

    public HyperToolbar(Context context) {
        this(context, null);
    }

    public HyperToolbar(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.toolbarStyle);
    }

    public HyperToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // Removed custom attribute parsing and HyperToolbar specific initialization.
        // This makes it behave like a standard Toolbar.
        setContentInsetsRelative(0, 0); // Retain standard toolbar insets for consistent padding.
    }

    @Override
    public void setTitle(CharSequence title) {
        // Override to delegate directly to the superclass, removing custom logic.
        super.setTitle(title);
    }

    @Override
    public void setTitle(int resId) {
        // Override to delegate directly to the superclass.
        super.setTitle(resId);
    }

    // Removed all custom HyperToolbar specific methods and fields:
    // - hyperTitleText, expandedTextSizeSp, collapsedTextSizeSp fields.
    // - getHyperTitle() method.
    // - setCollapseProgress() method and its dependencies (updateByOffset, resolveTitleColor, setTitleTextSizeSp).
    // - bindScroll() method and its dependencies (RecyclerView, AbsListView listeners).
    // - sp() and pxToSp() utility methods.

    /**
     * Utility method to convert DP units to pixels.
     */
    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}