package com.centigrade.browser;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.materialswitch.MaterialSwitch;

public class BrowserControlSettingsActivity extends BaseSettingsSubActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_control_settings);
        initSubpage("浏览器控件");

        MaterialSwitch switchSupportZoom = findViewById(R.id.switch_support_zoom);
        MaterialSwitch switchBuiltInZoom = findViewById(R.id.switch_builtin_zoom_controls);
        MaterialSwitch switchDisplayZoom = findViewById(R.id.switch_display_zoom_controls);
        MaterialSwitch switchWideViewport = findViewById(R.id.switch_wide_viewport);
        MaterialSwitch switchLoadOverview = findViewById(R.id.switch_load_with_overview);
        MaterialSwitch switchAllowFileAccess = findViewById(R.id.switch_allow_file_access);
        MaterialSwitch switchAllowContentAccess = findViewById(R.id.switch_allow_content_access);
        MaterialSwitch switchAcceptCookies = findViewById(R.id.switch_accept_cookies);
        MaterialSwitch switchDomStorage = findViewById(R.id.switch_dom_storage);
        MaterialSwitch switchDatabase = findViewById(R.id.switch_database);
        MaterialSwitch switchSaveFormData = findViewById(R.id.switch_save_form_data);
        MaterialSwitch switchGeolocation = findViewById(R.id.switch_geolocation_enabled);
        MaterialSwitch switchJsOpenWindow = findViewById(R.id.switch_js_open_windows);
        MaterialSwitch switchMultiWindow = findViewById(R.id.switch_support_multiple_windows);
        MaterialSwitch switchThirdPartyCookies = findViewById(R.id.switch_accept_third_party_cookies);
        MaterialSwitch switchDoNotTrack = findViewById(R.id.switch_do_not_track);
        MaterialSwitch switchSaveData = findViewById(R.id.switch_save_data);
        MaterialSwitch switchSendXRequestedWith = findViewById(R.id.switch_send_x_requested_with);

        EditText editTextZoom = findViewById(R.id.edit_text_zoom);
        EditText editEncoding = findViewById(R.id.edit_default_encoding);
        Button btnSaveAdvanced = findViewById(R.id.btn_save_browser_controls);

        switchSupportZoom.setChecked(prefs.isSupportZoomEnabled());
        switchBuiltInZoom.setChecked(prefs.isBuiltInZoomControlsEnabled());
        switchDisplayZoom.setChecked(prefs.isDisplayZoomControlsEnabled());
        switchWideViewport.setChecked(prefs.isWideViewportEnabled());
        switchLoadOverview.setChecked(prefs.isLoadWithOverviewEnabled());
        switchAllowFileAccess.setChecked(prefs.isAllowFileAccessEnabled());
        switchAllowContentAccess.setChecked(prefs.isAllowContentAccessEnabled());
        switchAcceptCookies.setChecked(prefs.isAcceptCookiesEnabled());
        switchDomStorage.setChecked(prefs.isDomStorageEnabled());
        switchDatabase.setChecked(prefs.isDatabaseEnabled());
        switchSaveFormData.setChecked(prefs.isSaveFormDataEnabled());
        switchGeolocation.setChecked(prefs.isGeolocationEnabled());
        switchJsOpenWindow.setChecked(prefs.isJavaScriptCanOpenWindowsAutomaticallyEnabled());
        switchMultiWindow.setChecked(prefs.isSupportMultipleWindowsEnabled());
        switchThirdPartyCookies.setChecked(prefs.isAcceptThirdPartyCookiesEnabled());
        switchDoNotTrack.setChecked(prefs.isDoNotTrackEnabled());
        switchSaveData.setChecked(prefs.isSaveDataEnabled());
        switchSendXRequestedWith.setChecked(prefs.isSendXRequestedWithEnabled());

        editTextZoom.setText(String.valueOf(prefs.getTextZoom()));
        editEncoding.setText(prefs.getDefaultTextEncoding());

        switchSupportZoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setSupportZoomEnabled(isChecked);
            toastReload("页面缩放");
        });

        switchBuiltInZoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setBuiltInZoomControlsEnabled(isChecked);
            toastReload("内置缩放控件");
        });

        switchDisplayZoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setDisplayZoomControlsEnabled(isChecked);
            toastReload("屏幕缩放按钮");
        });

        switchWideViewport.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setWideViewportEnabled(isChecked);
            toastReload("宽视口");
        });

        switchLoadOverview.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setLoadWithOverviewEnabled(isChecked);
            toastReload("概览模式");
        });

        switchAllowFileAccess.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setAllowFileAccessEnabled(isChecked);
            toastReload("文件访问");
        });

        switchAllowContentAccess.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setAllowContentAccessEnabled(isChecked);
            toastReload("内容访问");
        });

        switchAcceptCookies.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setAcceptCookiesEnabled(isChecked);
            toastReload("所有 Cookie");
        });

        switchDomStorage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setDomStorageEnabled(isChecked);
            toastReload("DOM Storage");
        });

        switchDatabase.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setDatabaseEnabled(isChecked);
            toastReload("Database Storage");
        });

        switchSaveFormData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setSaveFormDataEnabled(isChecked);
            toastReload("表单数据保存");
        });

        switchGeolocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setGeolocationEnabled(isChecked);
            toastReload("地理定位");
        });

        switchJsOpenWindow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setJavaScriptCanOpenWindowsAutomaticallyEnabled(isChecked);
            toastReload("JS 自动开窗");
        });

        switchMultiWindow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setSupportMultipleWindowsEnabled(isChecked);
            toastReload("多窗口支持");
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switchThirdPartyCookies.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.setAcceptThirdPartyCookiesEnabled(isChecked);
                toastReload("第三方 Cookie");
            });
        } else {
            switchThirdPartyCookies.setEnabled(false);
            switchThirdPartyCookies.setChecked(true);
        }

        switchDoNotTrack.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setDoNotTrackEnabled(isChecked);
            toastReload("Do Not Track");
        });

        switchSaveData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setSaveDataEnabled(isChecked);
            toastReload("Save-Data");
        });

        switchSendXRequestedWith.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setSendXRequestedWithEnabled(isChecked);
            toastReload("X-Requested-With");
        });

        btnSaveAdvanced.setOnClickListener(v -> {
            String zoomText = editTextZoom.getText() == null ? "" : editTextZoom.getText().toString().trim();
            String encoding = editEncoding.getText() == null ? "" : editEncoding.getText().toString().trim();

            int zoom = 100;
            if (!TextUtils.isEmpty(zoomText)) {
                try {
                    zoom = Integer.parseInt(zoomText);
                } catch (Exception ignored) {
                }
            }
            prefs.setTextZoom(zoom);
            prefs.setDefaultTextEncoding(encoding);
            android.widget.Toast.makeText(this, "浏览器控件设置已保存，新的页面生效", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void toastReload(String label) {
        android.widget.Toast.makeText(this, label + "已更新，新的页面生效", android.widget.Toast.LENGTH_SHORT).show();
    }
}