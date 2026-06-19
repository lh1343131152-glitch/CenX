package com.centigrade.browser;

import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.widget.EditText;

import com.centigrade.ui.widget.HyperButton;
import android.widget.Toast;

public class CookieManagerActivity extends BaseSettingsSubActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cookie_manager);
        initSubpage("Cookie Manager");

        EditText editUrl = findViewById(R.id.edit_cookie_url);
        EditText editCookie = findViewById(R.id.edit_cookie_value);
        HyperButton btnRead = findViewById(R.id.btn_read_cookie);
        HyperButton btnWrite = findViewById(R.id.btn_write_cookie);
        HyperButton btnClear = findViewById(R.id.btn_clear_cookie);

        btnRead.setOnClickListener(v -> {
            String url = safeUrl(editUrl.getText() == null ? "" : editUrl.getText().toString());
            String cookie = CookieManager.getInstance().getCookie(url);
            editCookie.setText(cookie == null ? "" : cookie);
            Toast.makeText(this, cookie == null || cookie.isEmpty() ? "未读取到 Cookie" : "已读取 Cookie", Toast.LENGTH_SHORT).show();
        });

        btnWrite.setOnClickListener(v -> {
            String url = safeUrl(editUrl.getText() == null ? "" : editUrl.getText().toString());
            String cookie = editCookie.getText() == null ? "" : editCookie.getText().toString();
            if (cookie.trim().isEmpty()) {
                Toast.makeText(this, "Cookie 内容不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            CookieManager.getInstance().setCookie(url, cookie);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().flush();
            }
            Toast.makeText(this, "Cookie 已写入", Toast.LENGTH_SHORT).show();
        });

        btnClear.setOnClickListener(v -> {
            String url = safeUrl(editUrl.getText() == null ? "" : editUrl.getText().toString());
            CookieManager.getInstance().setCookie(url, "");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().flush();
            }
            editCookie.setText("");
            Toast.makeText(this, "该站点 Cookie 清空请求已提交", Toast.LENGTH_SHORT).show();
        });
    }

    private String safeUrl(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) value = "https://www.baidu.com";
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "https://" + value;
        }
        return value;
    }
}