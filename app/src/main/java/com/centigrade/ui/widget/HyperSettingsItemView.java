package com.centigrade.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.centigrade.browser.R;

public class HyperSettingsItemView extends LinearLayout {

    private TextView titleView;
    private TextView summaryView;
    private TextView valueView;
    private ImageView arrowView;

    public HyperSettingsItemView(Context context) {
        super(context);
        init(context, null);
    }

    public HyperSettingsItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HyperSettingsItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int minHeight = dp(64);
        setMinimumHeight(minHeight);
        int horizontal = dp(18);
        int vertical = dp(16);
        setPadding(horizontal, vertical, horizontal, vertical);

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(VERTICAL);
        LayoutParams textParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        textContainer.setLayoutParams(textParams);

        titleView = new TextView(context);
        titleView.setTextColor(getResources().getColor(R.color.text_primary, context.getTheme()));
        titleView.setTextSize(16);
        textContainer.addView(titleView);

        summaryView = new TextView(context);
        LayoutParams summaryParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(4);
        summaryView.setLayoutParams(summaryParams);
        summaryView.setTextColor(getResources().getColor(R.color.text_secondary, context.getTheme()));
        summaryView.setTextSize(12);
        textContainer.addView(summaryView);

        addView(textContainer);

        valueView = new TextView(context);
        LayoutParams valueParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        valueParams.leftMargin = dp(12);
        valueView.setLayoutParams(valueParams);
        valueView.setMaxWidth(dp(140));
        valueView.setSingleLine(true);
        valueView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        valueView.setTextColor(getResources().getColor(R.color.text_secondary, context.getTheme()));
        valueView.setTextSize(13);
        addView(valueView);

        arrowView = new ImageView(context);
        LayoutParams arrowParams = new LayoutParams(dp(18), dp(18));
        arrowParams.leftMargin = dp(10);
        arrowView.setLayoutParams(arrowParams);
        arrowView.setImageResource(R.drawable.forward);
        arrowView.setColorFilter(getResources().getColor(R.color.text_secondary, context.getTheme()));
        addView(arrowView);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HyperSettingsItemView);
            setTitle(a.getString(R.styleable.HyperSettingsItemView_hyperTitle));
            setSummary(a.getString(R.styleable.HyperSettingsItemView_hyperSummary));
            setValue(a.getString(R.styleable.HyperSettingsItemView_hyperValue));
            setShowArrow(a.getBoolean(R.styleable.HyperSettingsItemView_hyperShowArrow, true));
            a.recycle();
        } else {
            setShowArrow(true);
        }
    }

    public void setTitle(CharSequence title) {
        titleView.setText(title == null ? "" : title);
    }

    public void setSummary(CharSequence summary) {
        boolean hasSummary = summary != null && summary.length() > 0;
        summaryView.setText(hasSummary ? summary : "");
        summaryView.setVisibility(hasSummary ? VISIBLE : GONE);
    }

    public void setValue(CharSequence value) {
        boolean hasValue = value != null && value.length() > 0;
        valueView.setText(hasValue ? value : "");
        valueView.setVisibility(hasValue ? VISIBLE : GONE);
    }

    public void setShowArrow(boolean showArrow) {
        arrowView.setVisibility(showArrow ? VISIBLE : GONE);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
