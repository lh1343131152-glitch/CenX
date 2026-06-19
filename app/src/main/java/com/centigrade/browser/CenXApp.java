package com.centigrade.browser;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

import java.util.ArrayList;

public class CenXApp extends Application {
    private final ArrayList<Activity> activeActivities = new ArrayList<>();

    public static void showDynamicColorRestartDialog(Activity activity, boolean enabled) {
        String title = enabled ? "动态取色已开启" : "动态取色已关闭";
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage("修改动态取色需要重启应用才能生效，是否立即重启？")
                .setPositiveButton("重启", (dialog, which) -> {
                    Intent intent = activity.getPackageManager()
                            .getLaunchIntentForPackage(activity.getPackageName());
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        activity.startActivity(intent);
                        activity.finishAffinity();
                    }
                })
                .setNegativeButton("取消", null)
                .setCancelable(false)
                .show();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener dynamicColorListener =
            (sharedPreferences, key) -> {
                if (PreferencesManager.KEY_DYNAMIC_COLOR_ENABLED.equals(key)) {
                    boolean enabled = sharedPreferences.getBoolean(key, false);
                    // 找第一个存活的 Activity 弹窗
                    Activity target = null;
                    for (Activity a : activeActivities) {
                        if (!a.isFinishing()) {
                            target = a;
                            break;
                        }
                    }
                    if (target != null) {
                        showDynamicColorRestartDialog(target, enabled);
                    }
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        PreferencesManager prefs = PreferencesManager.getInstance(this);
        DynamicColorsOptions options = new DynamicColorsOptions.Builder()
                .setPrecondition((activity, theme) ->
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && prefs.isDynamicColorEnabled())
                .build();
        DynamicColors.applyToActivitiesIfAvailable(this, options);

        // 全局监听每个 Activity 创建
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                activeActivities.add(activity);
                CenXTheme.applyWindowColors(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {}

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                activeActivities.remove(activity);
            }
        });

        // 监听动态取色开关变化——弹窗提示重启
        prefs.getSharedPreferences().registerOnSharedPreferenceChangeListener(dynamicColorListener);
    }
}