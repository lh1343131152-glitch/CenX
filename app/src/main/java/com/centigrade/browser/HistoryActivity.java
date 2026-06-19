package com.centigrade.browser;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {
    
    private EditText searchEdit;
    private ListView listView;
    private TextView tvEmpty;
    private HistoryManager historyManager;
    private List<HistoryManager.HistoryItem> historyList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        searchEdit = findViewById(R.id.search_history);
        listView = findViewById(R.id.list_history);
        tvEmpty = findViewById(R.id.tv_empty);
        
        View backBtn = findViewById(R.id.back);
        TextView titleView = findViewById(R.id.appbar_title);
        if (titleView != null) {
            titleView.setText("历史记录");
        }
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
        
        historyManager = HistoryManager.getInstance(this);
        loadHistory(null);
        
        // 搜索
        searchEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String keyword = searchEdit.getText().toString().trim();
                loadHistory(keyword.isEmpty() ? null : keyword);
                return true;
            }
            return false;
        });
        
        // 点击
        listView.setOnItemClickListener((parent, view, position, id) -> {
            HistoryManager.HistoryItem item = historyList.get(position);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("url", item.url);
            resultIntent.putExtra("title", item.title);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
        
        // 长按删除
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            HistoryManager.HistoryItem item = historyList.get(position);
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("删除记录")
                    .setMessage("删除此条记录？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        historyManager.deleteHistory(item.id);
                        loadHistory(searchEdit.getText().toString().trim());
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });
        
        // 标题点击清除全部
        if (titleView != null) {
            titleView.setOnClickListener(v -> {
                showDeleteAllDialog();
            });
        }

        // 右下角删除按钮——清除全部
        findViewById(R.id.btn_delete_all).setOnClickListener(v -> {
            showDeleteAllDialog();
        });
    }

    private void showDeleteAllDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("清除全部历史记录")
                .setMessage("确定删除所有浏览记录？")
                .setPositiveButton("确定", (dialog, which) -> {
                    historyManager.deleteAllHistory();
                    loadHistory(null);
                    Toast.makeText(this, "历史记录已清除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void loadHistory(String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            historyList = historyManager.searchHistory(keyword);
        } else {
            historyList = historyManager.getAllHistory();
        }
        
        if (historyList.isEmpty()) {
            listView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        
        listView.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        List<Map<String, String>> data = new ArrayList<>();
        for (HistoryManager.HistoryItem item : historyList) {
            Map<String, String> map = new HashMap<>();
            String title = item.title != null && !item.title.isEmpty() ? item.title : item.url;
            if (title.length() > 40) title = title.substring(0, 38) + "...";
            map.put("title", title);
            String url = item.url;
            if (url.length() > 50) url = url.substring(0, 48) + "...";
            map.put("url", url);
            map.put("time", sdf.format(new Date(item.visitedAt)));
            data.add(map);
        }
        
        SimpleAdapter adapter = new SimpleAdapter(
                this, data,
                android.R.layout.simple_list_item_2,
                new String[]{"title", "url"},
                new int[]{android.R.id.text1, android.R.id.text2});
        listView.setAdapter(adapter);
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