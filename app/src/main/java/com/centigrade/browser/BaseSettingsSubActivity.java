package com.centigrade.browser;

import android.os.Bundle;
import android.widget.TextView;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class BaseSettingsSubActivity extends AppCompatActivity {

    protected PreferencesManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        CenXTheme.applyWindowColors(this);
        prefs = PreferencesManager.getInstance(this);
    }

    protected void initSubpage(String title) {
        View backBtn = findViewById(R.id.back);
        TextView titleView = findViewById(R.id.appbar_title);
        View scrollView = findViewById(R.id.settings_subpage_scroll);

        if (titleView != null) {
            titleView.setText(title);
        }
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        final View root = findViewById(R.id.root_settings_subpage);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, systemBars.top, 0, systemBars.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(root);
        }
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