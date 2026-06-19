package com.centigrade.browser;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class LabActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        CenXTheme.applyWindowColors(this);
        setContentView(R.layout.activity_lab);

        // 为根布局添加状态栏内边距，避免标题穿状态栏
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_lab), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        findViewById(R.id.back).setOnClickListener(v -> finish());
        TextView titleView = findViewById(R.id.appbar_title);
        if (titleView != null) {
            titleView.setText("实验室");
        }

        // 站点鉴权入口
        View cardSiteAuth = findViewById(R.id.card_lab_site_auth);
        ((TextView) cardSiteAuth.findViewById(R.id.tv_entry_title)).setText("站点鉴权");
        ((TextView) cardSiteAuth.findViewById(R.id.tv_entry_summary)).setText("为特定网站注入Header/Cookie/Token");
        cardSiteAuth.setOnClickListener(v ->
                startActivity(new Intent(this, SiteAuthSettingsActivity.class)));

        // Cookie Manager 入口
        View cardCookieManager = findViewById(R.id.card_lab_cookie_manager);
        ((TextView) cardCookieManager.findViewById(R.id.tv_entry_title)).setText("Cookie Manager");
        ((TextView) cardCookieManager.findViewById(R.id.tv_entry_summary)).setText("读 / 写 / 清空站点 Cookie");
        cardCookieManager.setOnClickListener(v ->
                startActivity(new Intent(this, CookieManagerActivity.class)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}