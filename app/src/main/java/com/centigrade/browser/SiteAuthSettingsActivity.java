package com.centigrade.browser;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;

import com.centigrade.ui.widget.HyperButton;
import android.widget.Toast;

import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * 站点鉴权设置页
 * 管理每个域名的自定义 Cookie、Authorization、Referer、Origin、UA 覆盖、额外 Header 等
 */
public class SiteAuthSettingsActivity extends BaseSettingsSubActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_auth_settings);
        initSubpage("站点鉴权");

        EditText editHost = findViewById(R.id.edit_site_host);
        EditText editCookie = findViewById(R.id.edit_site_cookie);
        EditText editAuthorization = findViewById(R.id.edit_site_authorization);
        EditText editReferer = findViewById(R.id.edit_site_referer);
        EditText editOrigin = findViewById(R.id.edit_site_origin);
        EditText editUa = findViewById(R.id.edit_site_ua_override);
        EditText editExtraHeaders = findViewById(R.id.edit_site_extra_headers_json);
        MaterialSwitch switchInject = findViewById(R.id.switch_enable_auth_injection);
        MaterialSwitch switchRetry401 = findViewById(R.id.switch_enable_401_retry);
        MaterialSwitch switchForceCookie = findViewById(R.id.switch_force_cookie);
        HyperButton btnLoad = findViewById(R.id.btn_load_site_auth);
        HyperButton btnSave = findViewById(R.id.btn_save_site_auth);
        HyperButton btnDelete = findViewById(R.id.btn_delete_site_auth);

        SiteAuthManager manager = new SiteAuthManager(this);

        // 加载已有站点配置
        btnLoad.setOnClickListener(v -> {
            String host = safeHost(getText(editHost));
            if (TextUtils.isEmpty(host)) {
                Toast.makeText(this, "请输入 host", Toast.LENGTH_SHORT).show();
                return;
            }

            SiteAuthConfig config = manager.findConfigByHost(host);
            if (config == null) {
                Toast.makeText(this, "未找到该站点配置", Toast.LENGTH_SHORT).show();
                return;
            }

            editCookie.setText(config.cookie);
            editAuthorization.setText(config.authorization);
            editReferer.setText(config.referer);
            editOrigin.setText(config.origin);
            editUa.setText(config.userAgentOverride);
            editExtraHeaders.setText(config.extraHeadersJson);
            switchInject.setChecked(config.enableAuthInjection);
            switchRetry401.setChecked(config.enable401Retry);
            switchForceCookie.setChecked(config.forceCookieHeader);
            Toast.makeText(this, "已加载站点配置", Toast.LENGTH_SHORT).show();
        });

        // 保存站点配置
        btnSave.setOnClickListener(v -> {
            String host = safeHost(getText(editHost));
            if (TextUtils.isEmpty(host)) {
                Toast.makeText(this, "host 不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            SiteAuthConfig config = new SiteAuthConfig();
            config.host = host;
            config.cookie = getText(editCookie);
            config.authorization = getText(editAuthorization);
            config.referer = getText(editReferer);
            config.origin = getText(editOrigin);
            config.userAgentOverride = getText(editUa);
            config.extraHeadersJson = getText(editExtraHeaders);
            config.enableAuthInjection = switchInject.isChecked();
            config.enable401Retry = switchRetry401.isChecked();
            config.forceCookieHeader = switchForceCookie.isChecked();

            manager.saveOrUpdate(config);
            Toast.makeText(this, "站点鉴权配置已保存", Toast.LENGTH_SHORT).show();
        });

        // 删除站点配置
        btnDelete.setOnClickListener(v -> {
            String host = safeHost(getText(editHost));
            if (TextUtils.isEmpty(host)) {
                Toast.makeText(this, "请输入 host", Toast.LENGTH_SHORT).show();
                return;
            }
            manager.deleteByHost(host);
            Toast.makeText(this, "站点配置已删除", Toast.LENGTH_SHORT).show();
        });
    }

    private String getText(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    /**
     * 从输入中提取纯净 host
     */
    private String safeHost(String host) {
        host = host == null ? "" : host.trim().toLowerCase();
        if (host.startsWith("http://")) host = host.substring(7);
        if (host.startsWith("https://")) host = host.substring(8);
        int slash = host.indexOf('/');
        if (slash >= 0) host = host.substring(0, slash);
        return host;
    }
}