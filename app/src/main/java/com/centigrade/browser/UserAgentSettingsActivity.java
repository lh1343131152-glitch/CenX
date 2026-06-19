package com.centigrade.browser;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class UserAgentSettingsActivity extends BaseSettingsSubActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_agent_settings);
        initSubpage("用户代理");

        RadioGroup uaGroup = findViewById(R.id.ua_group);

        int currentUA = prefs.getUserAgent();
        if (currentUA < uaGroup.getChildCount()) {
            ((RadioButton) uaGroup.getChildAt(currentUA)).setChecked(true);
        }

        uaGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int index = 0;
            if (checkedId == R.id.ua_desktop) index = 1;
            else if (checkedId == R.id.ua_ipad) index = 2;
            else if (checkedId == R.id.ua_iphone) index = 3;

            prefs.setUserAgent(index);
            MainActivity mainActivity = MainActivity.getCurrentInstance();
            if (mainActivity != null && mainActivity.getTabManager() != null) {
                mainActivity.getTabManager().applyUserAgent();
            }
            Toast.makeText(this, "UA已切换，当前标签页立即生效", Toast.LENGTH_SHORT).show();
        });
    }
}