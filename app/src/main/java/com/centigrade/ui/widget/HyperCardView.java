package com.centigrade.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.centigrade.browser.R;

public class HyperCardView extends CardView {

    public HyperCardView(Context context) {
        super(context);
        init();
    }

    public HyperCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HyperCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float radius = dp(24);
        setRadius(radius);
        setCardElevation(0f);
        setUseCompatPadding(false);
        setPreventCornerOverlap(true);
        setCardBackgroundColor(Color.TRANSPARENT);
        setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bottom_sheet_action_item_bg));
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}