package com.centigrade.browser;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.centigrade.browser.net.ResolvedConnectionFactory;

public class SourceViewActivity extends AppCompatActivity {
    
    private Toolbar toolbar;
    private TextView sourceText;
    private Button btnCopy;
    private String currentUrl;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_source_view);
        
        toolbar = findViewById(R.id.toolbar_source);
        sourceText = findViewById(R.id.source_text);
        btnCopy = findViewById(R.id.btn_copy_source);
        
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setTitle("查看源码");
        }
        
        currentUrl = getIntent().getStringExtra("url");
        if (currentUrl != null && !currentUrl.isEmpty()) {
            toolbar.setSubtitle(currentUrl);
            loadSourceCode(currentUrl);
        } else {
            sourceText.setText("没有可查看的页面源码");
        }
        
        btnCopy.setOnClickListener(v -> {
            String text = sourceText.getText().toString();
            if (!text.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("源码", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "源码已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadSourceCode(final String url) {
        sourceText.setText("正在加载页面源码...");
        
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL link = new URL(url);
                conn = new ResolvedConnectionFactory(this).open(url);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 14)");
                conn.setInstanceFollowRedirects(true);
                
                int responseCode = conn.getResponseCode();
                
                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                }
                
                StringBuilder sb = new StringBuilder();
                sb.append("<!-- HTTP Status: ").append(responseCode).append(" -->\n");
                sb.append("<!-- URL: ").append(url).append(" -->\n\n");
                
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                
                String source = sb.toString();
                runOnUiThread(() -> sourceText.setText(source));
                
            } catch (Exception e) {
                final String error = "加载失败: " + e.getClass().getSimpleName() + " - " + e.getMessage();
                runOnUiThread(() -> sourceText.setText(error));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
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