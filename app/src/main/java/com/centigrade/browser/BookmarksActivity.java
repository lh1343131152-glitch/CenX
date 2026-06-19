package com.centigrade.browser;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookmarksActivity extends AppCompatActivity {
    
    private ListView listView;
    private TextView tvEmpty;
    private BookmarkManager bookmarkManager;
    private List<BookmarkManager.BookmarkItem> bookmarks = new ArrayList<>();
    private BookmarkAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);
        
        listView = findViewById(R.id.list_bookmarks);
        tvEmpty = findViewById(R.id.tv_empty);
        
        View backBtn = findViewById(R.id.back);
        TextView titleView = findViewById(R.id.appbar_title);
        if (titleView != null) {
            titleView.setText("书签");
        }
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
        
        bookmarkManager = BookmarkManager.getInstance(this);
        adapter = new BookmarkAdapter();
        listView.setAdapter(adapter);
        
        listView.setOnItemClickListener((parent, view, position, id) -> {
            BookmarkManager.BookmarkItem item = bookmarks.get(position);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("url", item.url);
            resultIntent.putExtra("title", item.title);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
        
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            BookmarkManager.BookmarkItem item = bookmarks.get(position);
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("删除书签")
                    .setMessage("确定删除「" + item.title + "」？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        bookmarkManager.deleteBookmark(item.id);
                        loadBookmarks();
                        Toast.makeText(this, "已删除书签", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadBookmarks();
    }
    
    private void loadBookmarks() {
        bookmarks.clear();
        List<BookmarkManager.BookmarkItem> list = bookmarkManager.getAllBookmarks();
        bookmarks.addAll(list);
        adapter.notifyDataSetChanged();
        
        if (bookmarks.isEmpty()) {
            listView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }
    
    private class BookmarkAdapter extends BaseAdapter {
        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        
        @Override
        public int getCount() {
            return bookmarks.size();
        }
        
        @Override
        public Object getItem(int position) {
            return bookmarks.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return bookmarks.get(position).id;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
                holder = new ViewHolder();
                holder.text1 = convertView.findViewById(android.R.id.text1);
                holder.text2 = convertView.findViewById(android.R.id.text2);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            BookmarkManager.BookmarkItem item = bookmarks.get(position);
            String displayTitle = item.title != null && !item.title.isEmpty() ? item.title : item.url;
            if (displayTitle.length() > 30) {
                displayTitle = displayTitle.substring(0, 28) + "...";
            }
            holder.text1.setText(displayTitle);
            
            String displayUrl = item.url;
            if (displayUrl.length() > 45) {
                displayUrl = displayUrl.substring(0, 42) + "...";
            }
            holder.text2.setText(displayUrl + "  " + sdf.format(new Date(item.createdAt)));
            
            return convertView;
        }
        
        class ViewHolder {
            TextView text1, text2;
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