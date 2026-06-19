package com.centigrade.browser;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AiSettingsActivity extends BaseSettingsSubActivity {

    public static final String PREF_AI = "cenx_ai_config";
    public static final String KEY_API_URL = "api_url";
    public static final String KEY_API_KEY = "api_key";
    public static final String KEY_API_MODEL = "api_model";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_settings);
        initSubpage("AI 设置");

        SharedPreferences aiPrefs = getSharedPreferences(PREF_AI, MODE_PRIVATE);

        EditText editApiUrl = findViewById(R.id.edit_ai_api_url);
        EditText editApiKey = findViewById(R.id.edit_ai_api_key);
        EditText editApiModel = findViewById(R.id.edit_ai_model);
        Button btnSaveConfig = findViewById(R.id.btn_ai_save_config);

        editApiUrl.setBackground(CenXTheme.inputBackground(this, 22));
        editApiKey.setBackground(CenXTheme.inputBackground(this, 22));
        editApiModel.setBackground(CenXTheme.inputBackground(this, 22));

        editApiUrl.setText(aiPrefs.getString(KEY_API_URL, "https://api.openai.com/v1/chat/completions"));
        editApiKey.setText(aiPrefs.getString(KEY_API_KEY, ""));
        editApiModel.setText(aiPrefs.getString(KEY_API_MODEL, "gpt-4o-mini"));

        btnSaveConfig.setOnClickListener(v -> {
            String apiUrl = editApiUrl.getText().toString().trim();
            String apiKey = editApiKey.getText().toString().trim();
            String apiModel = editApiModel.getText().toString().trim();

            if (TextUtils.isEmpty(apiUrl) || TextUtils.isEmpty(apiModel)) {
                Toast.makeText(this, "请填写 Base URL 和模型名称", Toast.LENGTH_SHORT).show();
                return;
            }

            aiPrefs.edit()
                    .putString(KEY_API_URL, apiUrl)
                    .putString(KEY_API_KEY, apiKey)
                    .putString(KEY_API_MODEL, apiModel)
                    .apply();

            Toast.makeText(this, "AI 配置已保存", Toast.LENGTH_SHORT).show();
        });
    }
}