package com.centigrade.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.centigrade.browser.R;

public class HyperButton extends AppCompatButton {

    public static final int STYLE_PRIMARY = 0;
    public static final int STYLE_SECONDARY = 1;
    public static final int STYLE_DANGER = 2;

    public HyperButton(Context context) {
        super(context);
        init(context, null);
    }

    public HyperButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HyperButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        int style = STYLE_PRIMARY;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HyperButton);
            style = a.getInt(R.styleable.HyperButton_hyperButtonStyle, STYLE_PRIMARY);
            a.recycle();
        }

        setAllCaps(false);
        setMinHeight(dp(44));
        setMinimumHeight(dp(44));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        setTypeface(Typeface.DEFAULT_BOLD);
        setPadding(dp(16), 0, dp(16), 0);

        applyStyle(style);
    }

private int getAttrColor(Context context, int attrResId) {
    TypedValue typedValue = new TypedValue();
    context.getTheme().resolveAttribute(attrResId, typedValue, true);
    return typedValue.data;
}

    public void applyStyle(int style) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(22));

        switch (style) {
            case STYLE_SECONDARY:
                background.setColor(ContextCompat.getColor(getContext(), R.color.card_background));
                background.setStroke(dp(1), ContextCompat.getColor(getContext(), R.color.surface_stroke));
                setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
                break;
            case STYLE_DANGER:
                background.setColor(0xFFE06A4E);
                setTextColor(ContextCompat.getColor(getContext(), R.color.white));
                break;
            case STYLE_PRIMARY:
            default:
                background.setColor(getAttrColor(getContext(), android.R.attr.colorPrimary));
                setTextColor(ContextCompat.getColor(getContext(), R.color.button_primary_text));
                break;
        }

        setBackground(background);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}