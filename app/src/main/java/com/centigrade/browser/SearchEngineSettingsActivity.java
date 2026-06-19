package com.centigrade.browser;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class SearchEngineSettingsActivity extends BaseSettingsSubActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_engine_settings);
        initSubpage("搜索引擎");

        RadioGroup searchGroup = findViewById(R.id.search_group);
        EditText customSearchEdit = findViewById(R.id.custom_search_edit);
        Button btnSaveCustomSearch = findViewById(R.id.btn_save_custom_search);

        customSearchEdit.setBackground(CenXTheme.inputBackground(this, 22));

        int currentSearch = prefs.getSearchEngine();
        String customSearch = prefs.getCustomSearchUrl();
        customSearchEdit.setText(customSearch);

        if (currentSearch < searchGroup.getChildCount()) {
            ((RadioButton) searchGroup.getChildAt(currentSearch)).setChecked(true);
        }

        searchGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int index = 0;
            if (checkedId == R.id.search_google) index = 1;
            else if (checkedId == R.id.search_bing) index = 2;
            else if (checkedId == R.id.search_sogou) index = 3;
            else if (checkedId == R.id.search_duckduckgo) index = 4;
            if (checkedId == R.id.search_metaso) index = 5;
            else if (checkedId == R.id.search_custom) index = 6;

            prefs.setSearchEngine(index);
            MainActivity mainActivity = MainActivity.getCurrentInstance();
            if (mainActivity != null && mainActivity.getTabManager() != null) {
                mainActivity.getTabManager().refreshHomePages();
            }
            String name = ((RadioButton) findViewById(checkedId)).getText().toString();
            Toast.makeText(this, "搜索引擎已切换为 " + name + "，新搜索立即生效", Toast.LENGTH_SHORT).show();
        });

        btnSaveCustomSearch.setOnClickListener(v -> {
            String url = customSearchEdit.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "请输入搜索地址（含 %s 作为关键词占位符）", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!url.contains("%s")) {
                Toast.makeText(this, "搜索地址必须包含 %s（代表关键词位置）", Toast.LENGTH_LONG).show();
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            prefs.setCustomSearchUrl(url);
            customSearchEdit.setText(url);
            MainActivity mainActivity = MainActivity.getCurrentInstance();
            if (mainActivity != null && mainActivity.getTabManager() != null && prefs.getSearchEngine() == 6) {
                mainActivity.getTabManager().refreshHomePages();
            }
            Toast.makeText(this, "自定义搜索引擎已保存，新搜索立即生效", Toast.LENGTH_SHORT).show();
        });
    }
}