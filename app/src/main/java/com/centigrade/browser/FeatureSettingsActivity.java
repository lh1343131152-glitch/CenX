package com.centigrade.browser;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.materialswitch.MaterialSwitch;

public class FeatureSettingsActivity extends BaseSettingsSubActivity {

    private View incognitoStatusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_settings);
        initSubpage("功能开关");

        MaterialSwitch switchAdBlock = findViewById(R.id.switch_adblock);
        MaterialSwitch switchIncognito = findViewById(R.id.switch_incognito);
        MaterialSwitch switchResourceSniff = findViewById(R.id.switch_resource_sniff);
        MaterialSwitch switchDohWebView = findViewById(R.id.switch_doh_webview);
        MaterialSwitch switchDynamicColor = findViewById(R.id.switch_dynamic_color);
        View dynamicColorHint = findViewById(R.id.tv_dynamic_color_hint);
        RadioGroup groupRenderMode = findViewById(R.id.group_render_mode);
        RadioGroup groupCacheMode = findViewById(R.id.group_cache_mode);
        RadioGroup groupLoadMode = findViewById(R.id.group_load_mode);
        
        switchAdBlock.setChecked(prefs.isAdBlockEnabled());
        switchIncognito.setChecked(prefs.isIncognitoMode());
        switchResourceSniff.setChecked(prefs.isResourceSniffEnabled());
        switchDohWebView.setChecked(prefs.isDohWebViewEnabled());
        switchDynamicColor.setChecked(prefs.isDynamicColorEnabled());
        updateIncognitoStatusColor(switchIncognito.isChecked());

        boolean dynamicSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S;
        switchDynamicColor.setEnabled(dynamicSupported);
        if (dynamicColorHint != null) {
            dynamicColorHint.setAlpha(dynamicSupported ? 1f : 0.6f);
        }
        if (!dynamicSupported) {
            switchDynamicColor.setChecked(false);
        }

        int renderMode = prefs.getRenderMode();
        if (renderMode == 1) groupRenderMode.check(R.id.radio_render_compat);
        else if (renderMode == 2) groupRenderMode.check(R.id.radio_render_enhanced);
        else groupRenderMode.check(R.id.radio_render_standard);

        int cacheMode = prefs.getCacheModePref();
        if (cacheMode == 1) groupCacheMode.check(R.id.radio_cache_else_network);
        else if (cacheMode == 2) groupCacheMode.check(R.id.radio_cache_no);
        else if (cacheMode == 3) groupCacheMode.check(R.id.radio_cache_only);
        else groupCacheMode.check(R.id.radio_cache_default);

        int loadMode = prefs.getLoadMode();
        if (loadMode == 1) groupLoadMode.check(R.id.radio_load_fast);
        else if (loadMode == 2) groupLoadMode.check(R.id.radio_load_data_save);
        else groupLoadMode.check(R.id.radio_load_standard);

        switchAdBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setAdBlockEnabled(isChecked);
            Toast.makeText(this, "广告拦截" + (isChecked ? "已开启" : "已关闭") + "，刷新页面后生效", Toast.LENGTH_SHORT).show();
        });

        switchIncognito.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setIncognitoMode(isChecked);
            updateIncognitoStatusColor(isChecked);
            Toast.makeText(this, "无痕模式" + (isChecked ? "已开启" : "已关闭") + "，浏览记录不会被保存", Toast.LENGTH_SHORT).show();
        });

        switchResourceSniff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setResourceSniffEnabled(isChecked);
            Toast.makeText(this, "资源嗅探" + (isChecked ? "已开启" : "已关闭") + "，刷新页面后生效", Toast.LENGTH_SHORT).show();
        });

        switchDohWebView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setDohWebViewEnabled(isChecked);
            Toast.makeText(this, "DoH/Hosts资源接管" + (isChecked ? "已开启" : "已关闭") + "，刷新页面后生效", Toast.LENGTH_SHORT).show();
        });

switchDynamicColor.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                    buttonView.setChecked(false);
                    Toast.makeText(this, "动态取色仅支持 Android 12 及以上", Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.setDynamicColorEnabled(isChecked);
                CenXApp.showDynamicColorRestartDialog(this, isChecked);
            });

        groupRenderMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_render_compat) prefs.setRenderMode(1);
            else if (checkedId == R.id.radio_render_enhanced) prefs.setRenderMode(2);
            else prefs.setRenderMode(0);
            Toast.makeText(this, "渲染模式已切换，新的页面生效", Toast.LENGTH_SHORT).show();
        });

        groupCacheMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_cache_else_network) prefs.setCacheModePref(1);
            else if (checkedId == R.id.radio_cache_no) prefs.setCacheModePref(2);
            else if (checkedId == R.id.radio_cache_only) prefs.setCacheModePref(3);
            else prefs.setCacheModePref(0);
            Toast.makeText(this, "缓存策略已切换，新的页面生效", Toast.LENGTH_SHORT).show();
        });

        groupLoadMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_load_fast) prefs.setLoadMode(1);
            else if (checkedId == R.id.radio_load_data_save) prefs.setLoadMode(2);
            else prefs.setLoadMode(0);
            Toast.makeText(this, "加载模式已切换，新的页面生效", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateIncognitoStatusColor(boolean enabled) {
        if (incognitoStatusView == null) return;
        incognitoStatusView.setBackgroundColor(Color.parseColor(enabled ? "#2E7D32" : "#9E9E9E"));
        incognitoStatusView.setAlpha(enabled ? 1f : 0.6f);
    }
}