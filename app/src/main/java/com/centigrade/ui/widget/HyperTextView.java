package com.centigrade.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import androidx.appcompat.widget.AppCompatTextView;

import com.centigrade.browser.R;

public class HyperTextView extends AppCompatTextView {

    public static final int STYLE_TITLE = 0;
    public static final int STYLE_BODY = 1;
    public static final int STYLE_CAPTION = 2;
    public static final int STYLE_VALUE = 3;
    public static final int STYLE_SECTION = 4;

    public HyperTextView(Context context) {
        super(context);
        init(context, null);
    }

    public HyperTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HyperTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        int style = STYLE_BODY;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HyperTextView);
            style = a.getInt(R.styleable.HyperTextView_hyperTextStyle, STYLE_BODY);
            a.recycle();
        }
        applyStyle(style);
    }

    private int getAttrColor(Context context, int attrResId) {
         TypedValue typedValue = new TypedValue();
         context.getTheme().resolveAttribute(attrResId, typedValue, true);
         return typedValue.data;
    }
    public void applyStyle(int style) {
        switch (style) {
            case STYLE_TITLE:
                setTextSize(20);
                setTypeface(Typeface.DEFAULT_BOLD);
                setTextColor(getResources().getColor(R.color.text_primary, getContext().getTheme()));
                break;
            case STYLE_CAPTION:
                setTextSize(12);
                setTypeface(Typeface.DEFAULT);
                setTextColor(getResources().getColor(R.color.text_secondary, getContext().getTheme()));
                break;
            case STYLE_VALUE:
                setTextSize(13);
                setTypeface(Typeface.DEFAULT_BOLD);
                setTextColor(getAttrColor(getContext(), android.R.attr.colorPrimary));
                break;
            case STYLE_SECTION:
                setTextSize(14);
                setTypeface(Typeface.DEFAULT_BOLD);
                setTextColor(getResources().getColor(R.color.text_secondary, getContext().getTheme()));
                break;
            case STYLE_BODY:
            default:
                setTextSize(16);
                setTypeface(Typeface.DEFAULT);
                setTextColor(getResources().getColor(R.color.text_primary, getContext().getTheme()));
                break;
        }
    }
}
