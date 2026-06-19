package com.centigrade.browser;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;

import com.centigrade.browser.R;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class CenXTheme {
    private CenXTheme() {}

    public static boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    public static boolean useDynamicColor(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && PreferencesManager.getInstance(context).isDynamicColorEnabled();
    }

    @ColorInt
    public static int colorSurface(Context context) {
        if (useDynamicColor(context)) return getAttrColor(context, com.google.android.material.R.attr.colorSurface);
        return isDarkMode(context) ? Color.parseColor("#FF1F1F1F") : Color.parseColor("#FFFFFFFF");
    }

    @ColorInt
    public static int colorSurfaceVariant(Context context) {
        if (useDynamicColor(context)) return getAttrColor(context, com.google.android.material.R.attr.colorSurfaceVariant);
        return isDarkMode(context) ? Color.parseColor("#FF2A2A2A") : Color.parseColor("#FFFFFFFF");
    }

    @ColorInt
    public static int colorPrimary(Context context) {
        if (useDynamicColor(context)) return getAttrColor(context, com.google.android.material.R.attr.colorPrimary);
        return isDarkMode(context) ? Color.parseColor("#FF5C3A1C") : Color.parseColor("#FFA0642F");
    }

    @ColorInt
    public static int colorOnSurface(Context context) {
        return isDarkMode(context) ? Color.parseColor("#FFF4F4F4") : Color.parseColor("#FF111111");
    }

    @ColorInt
    public static int colorOnSurfaceVariant(Context context) {
        return isDarkMode(context) ? Color.parseColor("#FFC8C8C8") : Color.parseColor("#FF6F6F6F");
    }

    @ColorInt
    public static int colorOutline(Context context) {
        if (useDynamicColor(context)) {
            return isDarkMode(context) ? Color.parseColor("#FF4A4A4A") : Color.parseColor("#FFD8D8D8");
        }
        return isDarkMode(context) ? Color.parseColor("#FF4A4A4A") : Color.parseColor("#FFA0642F");
    }

    @ColorInt
    public static int colorSurfaceContainer(Context context) {
        if (useDynamicColor(context)) return getAttrColor(context, com.google.android.material.R.attr.colorSurfaceContainer);
        return isDarkMode(context) ? Color.parseColor("#FF262626") : Color.parseColor("#FFFFFFFF");
    }

    @ColorInt
    public static int colorSurfaceContainerHigh(Context context) {
        if (useDynamicColor(context)) return getAttrColor(context, com.google.android.material.R.attr.colorSurfaceContainerHigh);
        return isDarkMode(context) ? Color.parseColor("#FF303030") : Color.parseColor("#FFFFFFFF");
    }

    @ColorInt
    public static int colorScrim(Context context) {
        return isDarkMode(context) ? 0xCC000000 : 0x66000000;
    }

    public static GradientDrawable dialogBackground(Context context, float radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colorSurface(context));
        bg.setCornerRadius(dp(context, radiusDp));
        bg.setStroke(Math.max(1, dp(context, 1)), colorOutline(context));
        return bg;
    }

    public static GradientDrawable inputBackground(Context context, float radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colorSurfaceVariant(context));
        bg.setCornerRadius(dp(context, radiusDp));
        bg.setStroke(Math.max(1, dp(context, 1)), colorOutline(context));
        return bg;
    }

    public static GradientDrawable solidBackground(Context context, @ColorInt int color, float radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(context, radiusDp));
        return bg;
    }

    public static void applyWindowColors(Activity activity) {
        if (activity == null) return;
        Window window = activity.getWindow();
        if (window == null) return;

        // 关闭动态取色 + 浅色模式：应用 ThemeOverlay 覆盖 colorOutline 颜色
        if (!useDynamicColor(activity) && !isDarkMode(activity)) {
            activity.getTheme().applyStyle(R.style.ThemeOverlay_CenX_DynamicColorDisabled, true);
        }

        int surface = colorSurfaceContainer(activity);
        boolean isLightMode = !isDarkMode(activity);

        window.setStatusBarColor(surface);
        window.setNavigationBarColor(surface);

        // 根据背景色亮度自动判断状态栏图标用浅色还是深色
        // 亮度计算公式：0.299*R + 0.587*G + 0.114*B（sRGB 亮度系数）
        double luminance = (0.299 * Color.red(surface) + 0.587 * Color.green(surface) + 0.114 * Color.blue(surface)) / 255.0;
        boolean useLightIcons = luminance < 0.5;  // 深色背景 → 浅色图标

        View decor = window.getDecorView();
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, decor);
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!useLightIcons);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                controller.setAppearanceLightNavigationBars(isLightMode);
            }
        }

        if (decor != null) {
            decor.setBackgroundColor(surface);
        }
    }

    @ColorInt
    public static int getAttrColor(Context context, @AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        boolean found = context.getTheme().resolveAttribute(attr, typedValue, true);
        if (!found) return Color.MAGENTA;
        if (typedValue.resourceId != 0) return context.getColor(typedValue.resourceId);
        return typedValue.data;
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}