package com.centigrade.browser;

import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.widget.Button;
import android.widget.Toast;

public class ClearDataSettingsActivity extends BaseSettingsSubActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear_data_settings);
        initSubpage("清除数据");

        Button btnClearCache = findViewById(R.id.btn_clear_cache);
        Button btnClearHistory = findViewById(R.id.btn_clear_history);
        Button btnClearCookies = findViewById(R.id.btn_clear_cookies);

        btnClearCache.setOnClickListener(v -> {
            try {
                WebStorage.getInstance().deleteAllData();
                Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "清除缓存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnClearHistory.setOnClickListener(v -> {
            HistoryManager.getInstance(this).deleteAllHistory();
            Toast.makeText(this, "历史记录已清除", Toast.LENGTH_SHORT).show();
        });

        btnClearCookies.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().removeAllCookies(null);
            } else {
                CookieManager.getInstance().removeAllCookie();
            }
            Toast.makeText(this, "Cookie已清除", Toast.LENGTH_SHORT).show();
        });
    }
}