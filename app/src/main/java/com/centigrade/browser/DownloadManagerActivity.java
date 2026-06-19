package com.centigrade.browser;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DownloadManagerActivity extends AppCompatActivity {

    private ListView listView;
    private TextView tvEmpty;
    private DownloadAdapter adapter;
    private List<DownloadTask> items;
    private Handler refreshHandler = new Handler();
    private Runnable refreshRunnable;
    // 用于标记已在下载队列中处理过的已完成文件名，避免本地扫描重复添加
    private Set<String> completedFromDb = new HashSet<>();

    // DownloadService 回调注册标志
    private boolean serviceCallbackRegistered = false;

    /**
     * 下载项数据模型：支持已完成文件 和 正在下载的任务
     */
    public static class DownloadTask {
        public long id;
        public String fileName;
        public long fileSize;       // 总字节数（已完成文件）
        public long lastModified;
        public int status;           // DownloadManager.STATUS_*
        public long bytesDownloaded; // 已下载字节
        public long totalBytes;      // 总字节（正在下载时）
        public boolean isPersistentRecord; // 是否为持久化历史记录（文件已不存在）

        // 用于暂停/继续/停止的源信息
        public String downloadUrl;
        public String userAgent;
        public String contentDisposition;
        public String mimeType;

        // 已完成文件的构造
        public DownloadTask(String fileName, long fileSize, long lastModified) {
            this.id = -1;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.lastModified = lastModified;
            this.status = DownloadManager.STATUS_SUCCESSFUL;
            this.bytesDownloaded = fileSize;
            this.totalBytes = fileSize;
            this.isPersistentRecord = false;
        }

        // 持久化历史记录构造
        public DownloadTask(String fileName, long fileSize, long lastModified, boolean isPersistent) {
            this.id = -1;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.lastModified = lastModified;
            this.status = DownloadManager.STATUS_SUCCESSFUL;
            this.bytesDownloaded = fileSize;
            this.totalBytes = fileSize;
            this.isPersistentRecord = isPersistent;
        }

        // 下载队列中的构造（带源信息，用于暂停/继续/停止）
        public DownloadTask(long id, String fileName, int status, long bytesDownloaded, long totalBytes,
                            String downloadUrl, String userAgent, String contentDisposition, String mimeType) {
            this.id = id;
            this.fileName = fileName;
            this.status = status;
            this.bytesDownloaded = bytesDownloaded;
            this.totalBytes = totalBytes;
            this.fileSize = totalBytes;
            this.lastModified = System.currentTimeMillis();
            this.isPersistentRecord = false;
            this.downloadUrl = downloadUrl;
            this.userAgent = userAgent;
            this.contentDisposition = contentDisposition;
            this.mimeType = mimeType;
        }

        // 旧版构造（兼容已暂停任务）
        public DownloadTask(long id, String fileName, int status, long bytesDownloaded, long totalBytes) {
            this(id, fileName, status, bytesDownloaded, totalBytes, null, null, null, null);
        }

        public boolean isDownloading() {
            return status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING;
        }

        public boolean isPaused() {
            return status == DownloadManager.STATUS_PAUSED;
        }

        public int getProgressPercent() {
            if (totalBytes <= 0) return 0;
            return (int) (bytesDownloaded * 100 / totalBytes);
        }
    }

    private class DownloadAdapter extends BaseAdapter {
        private List<DownloadTask> items;
        private LayoutInflater inflater;

        DownloadAdapter(List<DownloadTask> items) {
            this.items = items;
            this.inflater = LayoutInflater.from(DownloadManagerActivity.this);
        }

        @Override
        public int getCount() { return items.size(); }

        @Override
        public Object getItem(int position) { return items.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_download, parent, false);
            }
            final DownloadTask item = items.get(position);

            TextView text1 = convertView.findViewById(R.id.download_name);
            TextView text2 = convertView.findViewById(R.id.download_info);
            ProgressBar progressBar = convertView.findViewById(R.id.download_progress);
            TextView tvProgress = convertView.findViewById(R.id.tv_progress);
            View layoutControls = convertView.findViewById(R.id.layout_download_controls);
            View btnPause = convertView.findViewById(R.id.btn_pause);
            View btnResume = convertView.findViewById(R.id.btn_resume);
            View btnStop = convertView.findViewById(R.id.btn_stop);

            text1.setText(item.fileName != null ? item.fileName : "未知文件");
            text1.setTextColor(CenXTheme.colorOnSurface(DownloadManagerActivity.this));

            // 默认隐藏控制按钮
            layoutControls.setVisibility(View.GONE);
            btnPause.setVisibility(View.GONE);
            btnResume.setVisibility(View.GONE);
            btnStop.setVisibility(View.GONE);

            if (item.isPersistentRecord) {
                // 历史记录
                progressBar.setVisibility(View.GONE);
                tvProgress.setVisibility(View.GONE);
                String sizeStr2 = formatFileSize(item.fileSize);
                String dateStr2 = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                        .format(new Date(item.lastModified));
                text2.setText(sizeStr2 + "  |  " + dateStr2 + "  (历史记录)");
                text2.setTextColor(CenXTheme.colorOnSurfaceVariant(DownloadManagerActivity.this));
                text1.setTextColor(CenXTheme.colorOnSurfaceVariant(DownloadManagerActivity.this));
            } else if (item.isDownloading()) {
                // 正在下载：显示进度条、暂停和停止按钮
                progressBar.setVisibility(View.VISIBLE);
                tvProgress.setVisibility(View.VISIBLE);
                int percent = item.getProgressPercent();
                progressBar.setProgress(Math.min(percent, 100));

                String sizeStr = formatFileSize(item.bytesDownloaded) + " / " + formatFileSize(item.totalBytes);
                String statusStr = item.status == DownloadManager.STATUS_PENDING ? "等待中" : "下载中";
                tvProgress.setText(percent + "%");
                text2.setText(statusStr + "  " + sizeStr);
                text2.setTextColor(CenXTheme.colorOnSurfaceVariant(DownloadManagerActivity.this));

                // 显示暂停和停止按钮
                layoutControls.setVisibility(View.VISIBLE);
                btnPause.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.VISIBLE);

                btnPause.setOnClickListener(v -> pauseDownload(item));
                btnStop.setOnClickListener(v -> stopDownload(item));
            } else if (item.isPaused()) {
                // 已暂停：显示已下载进度、继续和停止按钮
                progressBar.setVisibility(View.VISIBLE);
                tvProgress.setVisibility(View.VISIBLE);

                // 从 DownloadService 获取实时进度（如果有活跃下载则使用实际字节数）
                DownloadService.DownloadTaskInfo activeInfo =
                        DownloadService.getTaskInfo(item.fileName);
                if (activeInfo != null) {
                    item.bytesDownloaded = activeInfo.downloadedBytes;
                    item.totalBytes = activeInfo.totalBytes;
                } else {
                    // 检查本地文件大小
                    File f = new File(
                        getCenXDownloadDir(),
                        item.fileName);
                    if (f.exists() && f.length() > item.bytesDownloaded) {
                        item.bytesDownloaded = f.length();
                    }
                }

                int p = Math.min(item.getProgressPercent(), 100);
                progressBar.setProgress(p);
                tvProgress.setText(p + "%");
                String sizeStr = formatFileSize(item.bytesDownloaded) + " / " + formatFileSize(item.totalBytes);
                text2.setText("已暂停  " + sizeStr);
                text2.setTextColor(CenXTheme.colorOnSurfaceVariant(DownloadManagerActivity.this));

                // 显示继续和停止按钮
                layoutControls.setVisibility(View.VISIBLE);
                btnResume.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.VISIBLE);

                btnResume.setOnClickListener(v -> resumeDownload(item));
                btnStop.setOnClickListener(v -> stopDownload(item));
            } else {
                switch (item.status) {
                    case DownloadManager.STATUS_SUCCESSFUL:
                        progressBar.setVisibility(View.GONE);
                        tvProgress.setVisibility(View.GONE);
                        String sizeStr = formatFileSize(item.fileSize);
                        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                                .format(new Date(item.lastModified));
                        text2.setText(sizeStr + "  |  " + dateStr);
                        text2.setTextColor(CenXTheme.colorOnSurfaceVariant(DownloadManagerActivity.this));
                        break;
                    case DownloadManager.STATUS_FAILED:
                        progressBar.setVisibility(View.GONE);
                        tvProgress.setVisibility(View.GONE);
                        text2.setText("下载失败");
                        text2.setTextColor(CenXTheme.colorPrimary(DownloadManagerActivity.this));
                        break;
                    default:
                        progressBar.setVisibility(View.GONE);
                        tvProgress.setVisibility(View.GONE);
                        text2.setText("未知状态");
                        text2.setTextColor(CenXTheme.colorOnSurfaceVariant(DownloadManagerActivity.this));
                        break;
                }
            }

            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_manager);

        listView = findViewById(R.id.list_downloads);
        tvEmpty = findViewById(R.id.tv_empty);

        findViewById(R.id.back).setOnClickListener(v -> finish());
        TextView titleView = findViewById(R.id.appbar_title);
        if (titleView != null) {
            titleView.setText("下载管理");
        }

        items = new ArrayList<>();
        adapter = new DownloadAdapter(items);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            DownloadTask item = items.get(position);
            if (item.isPersistentRecord) {
                // 历史记录：文件已不存在
                Toast.makeText(this, "该文件已被删除（历史记录）", Toast.LENGTH_SHORT).show();
            } else if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                File file = new File(
                    getCenXDownloadDir(),
                    item.fileName);
                if (file.exists()) {
                    try {
                        Uri contentUri = FileProvider.getUriForFile(
                                this,
                                getPackageName() + ".fileprovider",
                                file
                        );
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(contentUri, getMimeType(item.fileName));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(intent, "打开文件"));
                    } catch (Exception e) {
                        Toast.makeText(this, "无法打开文件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
                }
            } else if (item.isDownloading() || item.status == DownloadManager.STATUS_PAUSED) {
                Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
                startActivity(intent);
            }
        });

        // 长按删除持久化记录
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            DownloadTask item = items.get(position);
            if (item.isPersistentRecord) {
                // 使用自定义底部Dialog替换AlertDialog
                final BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_CenX_BottomSheetDialog);
                dialog.setContentView(R.layout.dialog_delete_record);
                dialog.setCanceledOnTouchOutside(true);

                TextView tvMessage = dialog.findViewById(R.id.tv_delete_record_message);
                if (tvMessage != null) {
                    tvMessage.setText("确定要删除「" + item.fileName + "」的下载记录吗？");
                }

                dialog.findViewById(R.id.btn_delete_cancel).setOnClickListener(v -> dialog.dismiss());
                dialog.findViewById(R.id.btn_delete_confirm).setOnClickListener(v -> {
                    dialog.dismiss();
                    deleteDownloadRecord(item.fileName);
                    refreshData();
                    Toast.makeText(this, "已删除记录", Toast.LENGTH_SHORT).show();
                });

                dialog.show();
                return true;
            }
            return false;
        });

        registerReceiver(downloadCompleteReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        // 注册 DownloadService 回调
        DownloadService.setCallback(new DownloadService.DownloadCallback() {
            @Override
            public void onProgress(String fileName, long downloaded, long total, int percent) {
                runOnUiThread(() -> {
                    // 更新对应下载项的进度
                    for (int i = 0; i < items.size(); i++) {
                        DownloadTask task = items.get(i);
                        if (fileName.equals(task.fileName)) {
                            task.bytesDownloaded = downloaded;
                            task.totalBytes = total;
                            break;
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onComplete(String fileName, String filePath) {
                runOnUiThread(() -> {
                    // 下载完成：保存记录并刷新
                    File f = new File(filePath);
                    saveDownloadRecord(fileName, f.length(), f.lastModified());
                    Toast.makeText(DownloadManagerActivity.this, "下载完成: " + fileName, Toast.LENGTH_SHORT).show();
                    refreshData();
                });
            }

            @Override
            public void onError(String fileName, String error) {
                runOnUiThread(() -> {
                    Toast.makeText(DownloadManagerActivity.this, fileName + ": " + error, Toast.LENGTH_SHORT).show();
                    refreshData();
                });
            }

            @Override
            public void onPaused(String fileName, long downloaded, long total) {
                runOnUiThread(() -> {
                    Toast.makeText(DownloadManagerActivity.this, "已暂停: " + fileName + " (" + (total > 0 ? downloaded * 100 / total : 0) + "%)", Toast.LENGTH_SHORT).show();
                    refreshData();
                });
            }
        });
        serviceCallbackRegistered = true;

        refreshData();
        startAutoRefresh();
    }

    private BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshData();
        }
    };

    private void startAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshData();
                refreshHandler.postDelayed(this, 2000);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(downloadCompleteReceiver);
        } catch (Exception ignored) {}
        refreshHandler.removeCallbacks(refreshRunnable);
        if (serviceCallbackRegistered) {
            DownloadService.setCallback(null);
            serviceCallbackRegistered = false;
        }
    }

    private void refreshData() {
        items.clear();
        completedFromDb.clear();

        // 1. 查询系统 DownloadManager 正在下载/暂停/等待的任务
        queryDownloadQueue();

        // 2. 扫描本地 Downloads 目录中已完成的文件
        scanLocalDownloads();

        // 3. 加载持久化下载历史记录（即使文件已被删除仍然显示）
        loadDownloadRecords();

        adapter.notifyDataSetChanged();

        if (items.isEmpty()) {
            listView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    /**
     * 查询下载任务
     * 1) 从系统 DownloadManager 查询正在下载/暂停的任务
     * 2) 从 DownloadService 查询由它管理的暂停任务（断点续传）
     */
    private void queryDownloadQueue() {
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null) {
            Cursor cursor = null;
            try {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterByStatus(DownloadManager.STATUS_RUNNING
                        | DownloadManager.STATUS_PENDING
                        | DownloadManager.STATUS_PAUSED
                        | DownloadManager.STATUS_SUCCESSFUL
                        | DownloadManager.STATUS_FAILED);

                cursor = dm.query(query);

                if (cursor != null && cursor.moveToFirst()) {
                    Set<String> completedNames = new HashSet<>();

                    do {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE));
                        int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                        long bytesSoFar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        String localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                        String localFileName = title;
                        if (localUri != null && !localUri.isEmpty()) {
                            try {
                                localFileName = Uri.parse(localUri).getLastPathSegment();
                            } catch (Exception ignored) {}
                        }

                        if (title == null || title.isEmpty()) {
                            title = localFileName != null ? localFileName : "未知文件";
                        }

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            if (localFileName != null) {
                                completedNames.add(localFileName);
                                completedNames.add(title);
                                long lastMod = 0;
                                try {
                                    int lastModIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP);
                                    if (lastModIdx >= 0) {
                                        lastMod = cursor.getLong(lastModIdx);
                                    }
                                } catch (Exception ignored) {}
                                if (lastMod <= 0) lastMod = System.currentTimeMillis();
                                saveDownloadRecord(localFileName, totalSize, lastMod);
                            }
                            continue;
                        }

                        String downloadUriStr = null;
                        try {
                            int uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_URI);
                            if (uriIdx >= 0) {
                                downloadUriStr = cursor.getString(uriIdx);
                            }
                        } catch (Exception ignored) {}

                        if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING) {
                            String mime = null;
                            try {
                                int mimeIdx = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE);
                                if (mimeIdx >= 0) {
                                    mime = cursor.getString(mimeIdx);
                                }
                            } catch (Exception ignored) {}
                            items.add(new DownloadTask(id, title, status, bytesSoFar, totalSize,
                                    downloadUriStr, null, null, mime));
                        } else {
                            items.add(new DownloadTask(id, title, status, bytesSoFar, totalSize));
                        }
                    } while (cursor.moveToNext());

                    completedFromDb.addAll(completedNames);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    try { cursor.close(); } catch (Exception ignored) {}
                }
            }
        }

        // --- 关键：加载 DownloadService 管理的暂停任务（断点续传暂停） ---
        Map<String, DownloadService.PausedTaskInfo> servicePaused =
                DownloadService.getPausedTasks(this);
        Set<String> existingNames = new HashSet<>();
        for (DownloadTask task : items) {
            if (task.fileName != null) {
                existingNames.add(task.fileName);
            }
        }

        for (Map.Entry<String, DownloadService.PausedTaskInfo> entry : servicePaused.entrySet()) {
            String fileName = entry.getKey();
            DownloadService.PausedTaskInfo info = entry.getValue();

            if (!existingNames.contains(fileName)) {
                // 获取本地已下载的字节数
                long downloadedBytes = info.downloadedBytes;
                    File partialFile = new File(
                            getCenXDownloadDir(),
                            fileName);
                if (partialFile.exists() && partialFile.length() > downloadedBytes) {
                    downloadedBytes = partialFile.length();
                }

                // 创建暂停状态的 DownloadTask（使用 DownloadManager.STATUS_PAUSED 状态码 - 4）
                DownloadTask task = new DownloadTask(-1, fileName,
                        DownloadManager.STATUS_PAUSED, downloadedBytes, info.totalBytes,
                        info.url, info.userAgent, null, null);
                items.add(task);
                existingNames.add(fileName);
            }
        }

        // 同时加载旧的暂停记录（兼容之前版本的暂停方式）
        List<DownloadTask> legacyPaused = loadPausedTasks();
        for (DownloadTask paused : legacyPaused) {
            if (!existingNames.contains(paused.fileName)) {
                // 检查本地文件大小作为已下载字节
                File f = new File(
                        getCenXDownloadDir(),
                        paused.fileName);
                if (f.exists() && f.length() > 0) {
                    paused.bytesDownloaded = f.length();
                }
                items.add(paused);
                existingNames.add(paused.fileName);
            }
        }
    }

    private void scanLocalDownloads() {
        try {
            File downloadDir = getCenXDownloadDir();
            File[] files = downloadDir.listFiles();
            if (files != null) {
                // 收集已出现在列表中的文件名（包括未完成的任务）
                Set<String> existingNames = new HashSet<>();
                for (DownloadTask task : items) {
                    if (task.fileName != null) {
                        existingNames.add(task.fileName);
                    }
                }
                // 加上系统DownloadManager中已标记为完成的任务
                existingNames.addAll(completedFromDb);

                for (File file : files) {
                    if (file.isFile()) {
                        String name = file.getName();
                        if (!existingNames.contains(name)) {
                            // 扫描到新文件：加入列表并保存到持久化记录
                            saveDownloadRecord(name, file.length(), file.lastModified());
                            items.add(new DownloadTask(name, file.length(), file.lastModified()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File getCenXDownloadDir() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CenX");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.CHINA, "%.1f KB", size / 1024.0);
        } else {
            return String.format(Locale.CHINA, "%.1f MB", size / (1024.0 * 1024.0));
        }
    }

    // ========== 下载记录持久化 ==========

    private static final String PREF_NAME = "download_history";
    private static final String KEY_RECORDS = "records";

    /**
     * 保存一条下载完成记录到 SharedPreferences
     */
    private void saveDownloadRecord(String fileName, long fileSize, long lastModified) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        JSONArray arr = new JSONArray();
        // 读出现有记录
        String json = prefs.getString(KEY_RECORDS, "[]");
        try {
            arr = new JSONArray(json);
        } catch (JSONException ignored) {}

        // 检查是否已存在同名记录，存在则更新
        boolean found = false;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject obj = arr.getJSONObject(i);
                if (obj.optString("fileName", "").equals(fileName)) {
                    obj.put("fileSize", fileSize);
                    obj.put("lastModified", lastModified);
                    found = true;
                    break;
                }
            } catch (JSONException ignored) {}
        }

        if (!found) {
            JSONObject record = new JSONObject();
            try {
                record.put("fileName", fileName);
                record.put("fileSize", fileSize);
                record.put("lastModified", lastModified);
                arr.put(record);
            } catch (JSONException ignored) {}
        }

        prefs.edit().putString(KEY_RECORDS, arr.toString()).apply();
    }

    /**
     * 加载持久化下载记录到列表底部
     * 只加载本地已经不存在文件的记录（文件已被删除）
     */
    private void loadDownloadRecords() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_RECORDS, "[]");
        try {
            JSONArray arr = new JSONArray(json);

            // 收集当前列表中已有的文件名（包括刚扫描到的和正在下载的）
            Set<String> existingNames = new HashSet<>();
            for (DownloadTask task : items) {
                if (task.fileName != null) {
                    existingNames.add(task.fileName);
                }
            }

            // 再加一份本地实际存在的文件（防止本地磁盘文件未出现在items中）
            try {
                File downloadDir = getCenXDownloadDir();
                File[] files = downloadDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) {
                            existingNames.add(f.getName());
                        }
                    }
                }
            } catch (Exception ignored) {}

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.optString("fileName", "未知文件");
                long size = obj.optLong("fileSize", 0);
                long modified = obj.optLong("lastModified", System.currentTimeMillis());

                if (!existingNames.contains(name)) {
                    File localFile = new File(getCenXDownloadDir(), name);
                    if (localFile.exists()) {
                        items.add(new DownloadTask(name, localFile.length(), localFile.lastModified()));
                    } else {
                        items.add(new DownloadTask(name, size, modified, true));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从持久化记录中删除一条（长按删除时调用）
     */
    private void deleteDownloadRecord(String fileName) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_RECORDS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.optString("fileName", "").equals(fileName)) {
                    newArr.put(obj);
                }
            }
            prefs.edit().putString(KEY_RECORDS, newArr.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ========== 结束持久化 ==========

    // ========== 暂停/继续/停止下载 ==========

    private static final String PREF_PAUSED = "download_paused_tasks";
    private static final String KEY_PAUSED_LIST = "paused_list";

    /**
     * 保存暂停的下载任务信息到SharedPreferences
     */
    private void savePausedTask(DownloadTask task) {
        SharedPreferences prefs = getSharedPreferences(PREF_PAUSED, MODE_PRIVATE);
        JSONArray arr = new JSONArray();
        String json = prefs.getString(KEY_PAUSED_LIST, "[]");
        try {
            arr = new JSONArray(json);
        } catch (JSONException ignored) {}

        // 如果已有同名记录则移除旧的
        JSONArray newArr = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.optString("fileName", "").equals(task.fileName)) {
                    newArr.put(obj);
                }
            } catch (JSONException ignored) {}
        }

        JSONObject record = new JSONObject();
        try {
            record.put("fileName", task.fileName != null ? task.fileName : "");
            record.put("downloadUrl", task.downloadUrl != null ? task.downloadUrl : "");
            record.put("userAgent", task.userAgent != null ? task.userAgent : "");
            record.put("contentDisposition", task.contentDisposition != null ? task.contentDisposition : "");
            record.put("mimeType", task.mimeType != null ? task.mimeType : "");
            record.put("totalBytes", task.totalBytes);
            newArr.put(record);
        } catch (JSONException ignored) {}

        prefs.edit().putString(KEY_PAUSED_LIST, newArr.toString()).apply();
    }

    /**
     * 加载暂停任务信息到临时列表中
     */
    private List<DownloadTask> loadPausedTasks() {
        List<DownloadTask> result = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences(PREF_PAUSED, MODE_PRIVATE);
        String json = prefs.getString(KEY_PAUSED_LIST, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String fileName = obj.optString("fileName", "");
                String downloadUrl = obj.optString("downloadUrl", "");
                String userAgent = obj.optString("userAgent", "");
                String contentDisposition = obj.optString("contentDisposition", "");
                String mimeType = obj.optString("mimeType", "");
                long totalBytes = obj.optLong("totalBytes", 0);

                if (!fileName.isEmpty() && !downloadUrl.isEmpty()) {
                    DownloadTask task = new DownloadTask(-1, fileName,
                            DownloadManager.STATUS_PAUSED, 0, totalBytes,
                            downloadUrl, userAgent, contentDisposition, mimeType);
                    result.add(task);
                }
            }
        } catch (JSONException ignored) {}
        return result;
    }

    /**
     * 删除暂停的记录
     */
    private void removePausedTask(String fileName) {
        SharedPreferences prefs = getSharedPreferences(PREF_PAUSED, MODE_PRIVATE);
        String json = prefs.getString(KEY_PAUSED_LIST, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.optString("fileName", "").equals(fileName)) {
                    newArr.put(obj);
                }
            }
            prefs.edit().putString(KEY_PAUSED_LIST, newArr.toString()).apply();
        } catch (JSONException ignored) {}
    }

    /**
     * 暂停下载
     * - 如果正在使用DownloadService下载 → 通知服务暂停
     * - 如果正在使用系统DownloadManager下载 → remove掉，保存源信息供后续续传
     */
    private void pauseDownload(DownloadTask item) {
        if (item.fileName == null) return;

        // 检查是否是 DownloadService 管理的活跃下载
        String[] activeNames = DownloadService.getActiveDownloadNames();
        boolean isServiceManaged = false;
        for (String name : activeNames) {
            if (name.equals(item.fileName)) {
                isServiceManaged = true;
                break;
            }
        }

        if (isServiceManaged) {
            // DownloadService 管理的下载 → 通知暂停（保留文件）
            Intent intent = new Intent(this, DownloadService.class);
            intent.setAction("PAUSE_DOWNLOAD");
            intent.putExtra("fileName", item.fileName);
            startService(intent);
        } else if (item.id >= 0 && item.downloadUrl != null && !item.downloadUrl.isEmpty()) {
            // 系统 DownloadManager 管理的下载 → remove 并保存源信息供续传
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.remove(item.id);
            }
            // 保存源信息供后续断点续传
            savePausedTask(item);
            // 同时保存到 DownloadService 的格式（带已下载字节数）
            File partialFile = new File(
                    getCenXDownloadDir(),
                    item.fileName);
            long downloaded = partialFile.exists() ? partialFile.length() : item.bytesDownloaded;
            item.bytesDownloaded = downloaded;
            // 构造一个带已下载字节的存根
            DownloadService.PausedTaskInfo info = new DownloadService.PausedTaskInfo();
            info.fileName = item.fileName;
            info.url = item.downloadUrl;
            info.downloadedBytes = downloaded;
            info.totalBytes = item.totalBytes;
            info.userAgent = item.userAgent;
            // 手动保存到 DownloadService 的 SP（通过 putExtra 方式）
            saveToServicePausedState(item.fileName, item.downloadUrl, downloaded, item.totalBytes, item.userAgent);

            Toast.makeText(this, "已暂停: " + item.fileName + " (" + (item.totalBytes > 0 ? downloaded * 100 / item.totalBytes : 0) + "%)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "无法暂停: 缺少下载信息", Toast.LENGTH_SHORT).show();
        }
        refreshData();
    }

    /**
     * 保存暂停状态到 DownloadService 的 SharedPreferences 格式
     */
    private void saveToServicePausedState(String fileName, String url, long downloaded, long total, String userAgent) {
        android.content.SharedPreferences prefs = getSharedPreferences("resume_download_paused", MODE_PRIVATE);
        org.json.JSONArray arr;
        String json = prefs.getString("resume_list", "[]");
        try {
            arr = new org.json.JSONArray(json);
        } catch (Exception e) {
            arr = new org.json.JSONArray();
        }
        // 移除旧的同名记录
        org.json.JSONArray newArr = new org.json.JSONArray();
        boolean updated = false;
        for (int i = 0; i < arr.length(); i++) {
            try {
                org.json.JSONObject obj = arr.getJSONObject(i);
                if (obj.optString("fileName", "").equals(fileName)) {
                    obj.put("url", url != null ? url : "");
                    obj.put("downloadedBytes", downloaded);
                    obj.put("totalBytes", total);
                    obj.put("userAgent", userAgent != null ? userAgent : "");
                    newArr.put(obj);
                    updated = true;
                } else {
                    newArr.put(obj);
                }
            } catch (Exception ignored) {}
        }
        if (!updated) {
            try {
                org.json.JSONObject record = new org.json.JSONObject();
                record.put("fileName", fileName);
                record.put("url", url != null ? url : "");
                record.put("downloadedBytes", downloaded);
                record.put("totalBytes", total);
                record.put("userAgent", userAgent != null ? userAgent : "");
                newArr.put(record);
            } catch (Exception ignored) {}
        }
        prefs.edit().putString("resume_list", newArr.toString()).apply();
    }

    /**
     * 继续下载：使用 DownloadService 从断点处继续
     * 关键：传递 resume=true，服务会读取已保存的已下载字节数并发 Range 请求
     */
    private void resumeDownload(DownloadTask item) {
        String url = item.downloadUrl;
        if (url == null || url.isEmpty()) {
            // 检查是否为通过 DownloadService 暂停的任务（有保存的 url）
            Map<String, DownloadService.PausedTaskInfo> pausedTasks =
                    DownloadService.getPausedTasks(this);
            DownloadService.PausedTaskInfo pausedInfo = pausedTasks.get(item.fileName);
            if (pausedInfo != null) {
                url = pausedInfo.url;
            }
        }

        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "无法继续下载：缺少下载链接", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction("START_DOWNLOAD");
        intent.putExtra("url", url);
        intent.putExtra("fileName", item.fileName);
        intent.putExtra("userAgent", item.userAgent);
        intent.putExtra("resume", true);  // 关键：断点续传模式
        startService(intent);

        // 清除旧的暂停记录（DownloadService 内部会保存新进度）
        DownloadService.removePauseState(this, item.fileName);
        Toast.makeText(this, "正在继续下载: " + item.fileName, Toast.LENGTH_SHORT).show();
        refreshData();
    }

    /**
     * 停止下载：同时处理 DownloadService 和 DownloadManager 中的任务
     */
    private void stopDownload(DownloadTask item) {
        if (item.fileName == null) return;

        // 1) 如果是DownloadService管理的活跃下载 → 停止
        String[] activeNames = DownloadService.getActiveDownloadNames();
        boolean isServiceManaged = false;
        for (String name : activeNames) {
            if (name.equals(item.fileName)) {
                isServiceManaged = true;
                break;
            }
        }
        if (isServiceManaged) {
            Intent intent = new Intent(this, DownloadService.class);
            intent.setAction("STOP_DOWNLOAD");
            intent.putExtra("fileName", item.fileName);
            startService(intent);
        }

        // 2) 如果是系统DownloadManager管理的下载 → 调用系统remove
        if (item.id >= 0) {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.remove(item.id);
            }
        }

        // 3) 清除所有暂停记录（两种来源都清）
        DownloadService.removePauseState(this, item.fileName);
        removePausedTask(item.fileName);

        // 4) 删除未完成的临时文件
        try {
            File partialFile = new File(
                    getCenXDownloadDir(),
                    item.fileName);
            if (partialFile.exists()) {
                partialFile.delete();
            }
        } catch (Exception ignored) {}

        Toast.makeText(this, "已停止下载: " + item.fileName, Toast.LENGTH_SHORT).show();
        refreshData();
    }

    // ========== 结束暂停/继续/停止 ==========

    private String getMimeType(String fileName) {
        if (fileName == null) return "*/*";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".mkv")) return "video/*";
        if (lower.endsWith(".mp3") || lower.endsWith(".wav")) return "audio/*";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".apk")) return "application/vnd.android.package-archive";
        if (lower.endsWith(".zip") || lower.endsWith(".rar")) return "application/zip";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        return "*/*";
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