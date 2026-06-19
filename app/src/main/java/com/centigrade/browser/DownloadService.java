package com.centigrade.browser;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import com.centigrade.browser.net.ResolvedConnectionFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 前台下载服务 —— 支持真正的断点续传
 *
 * 核心原理：
 * - 暂停时：中断网络请求，将已下载字节数保存到 SharedPreferences
 * - 继续时：读取之前保存的已下载字节数，发送 Range 请求头从断点继续下载
 * - 写入时：使用 RandomAccessFile 或 FileOutputStream(append=true) 追加写入
 */
public class DownloadService extends Service {

    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String PREF_PAUSED = "resume_download_paused";
    private static final String KEY_RESUME_LIST = "resume_list";

    // 保存每个下载任务的状态（已下载字节数、文件路径等）
    private static final ConcurrentHashMap<String, DownloadTaskInfo> activeDownloads = new ConcurrentHashMap<>();
    // 保存每个下载任务的 Future（用于中断）
    private static final ConcurrentHashMap<String, Future<?>> taskFutures = new ConcurrentHashMap<>();

    private ExecutorService executor;
    private Handler mainHandler;

    public static class DownloadTaskInfo {
        public String url;
        public String fileName;
        public String filePath;
        public long totalBytes;
        public long downloadedBytes;
        public volatile boolean paused = false;
        public volatile boolean stopped = false;

        public DownloadTaskInfo(String url, String fileName, String filePath) {
            this.url = url;
            this.fileName = fileName;
            this.filePath = filePath;
            this.totalBytes = -1;
            this.downloadedBytes = 0;
        }
    }

    // 回调接口 - 用于通知 Activity 更新UI
    public interface DownloadCallback {
        void onProgress(String fileName, long downloaded, long total, int percent);
        void onComplete(String fileName, String filePath);
        void onError(String fileName, String error);
        void onPaused(String fileName, long downloaded, long total);
    }

    private static DownloadCallback callback = null;

    public static void setCallback(DownloadCallback cb) {
        callback = cb;
    }

    // 获取当前正在下载的文件名列表
    public static String[] getActiveDownloadNames() {
        return activeDownloads.keySet().toArray(new String[0]);
    }

    // 获取下载任务信息
    public static DownloadTaskInfo getTaskInfo(String fileName) {
        return activeDownloads.get(fileName);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (action == null) return START_STICKY;

        switch (action) {
            case "START_DOWNLOAD": {
                String url = intent.getStringExtra("url");
                String fileName = intent.getStringExtra("fileName");
                String userAgent = intent.getStringExtra("userAgent");
                boolean resume = intent.getBooleanExtra("resume", false);
                startDownload(url, fileName, userAgent, resume);
                break;
            }
            case "PAUSE_DOWNLOAD": {
                String fileName = intent.getStringExtra("fileName");
                pauseDownload(fileName);
                break;
            }
            case "STOP_DOWNLOAD": {
                String fileName = intent.getStringExtra("fileName");
                stopDownload(fileName);
                break;
            }
        }

        return START_STICKY;
    }

    /**
     * 开始/继续下载
     */
    private void startDownload(final String url, final String fileName,
                               final String userAgent, final boolean resume) {
        if (url == null || fileName == null) return;

        // 如果已经存在相同文件名的下载，则不重复启动
        if (activeDownloads.containsKey(fileName) && !resume) {
            return;
        }

        // 计算文件路径
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        final File file = new File(downloadDir, fileName);

        // 创建或获取任务信息
        DownloadTaskInfo info;
        long resumeFrom = 0;
        if (resume && file.exists()) {
            // 断点续传：使用已存在的文件，从已下载大小继续
            resumeFrom = file.length();
            info = new DownloadTaskInfo(url, fileName, file.getAbsolutePath());
            info.downloadedBytes = resumeFrom;
            // 从保存记录中读取总大小
            long savedTotal = getSavedTotalBytes(fileName);
            if (savedTotal > 0) info.totalBytes = savedTotal;
        } else {
            info = new DownloadTaskInfo(url, fileName, file.getAbsolutePath());
            // 非续传时清空文件
            try {
                if (file.exists()) file.delete();
                file.createNewFile();
            } catch (Exception ignored) {}
        }

        activeDownloads.put(fileName, info);

        // 使用前台服务通知
        startForeground(NOTIFICATION_ID, buildNotification(fileName, 0));

        // 提交下载任务
        Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                downloadFile(url, fileName, file, info, userAgent);
            }
        });
        taskFutures.put(fileName, future);
    }

    /**
     * 实际下载逻辑 —— 支持断点续传
     */
    private void downloadFile(String urlStr, String fileName, File file,
                              DownloadTaskInfo info, String userAgent) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        RandomAccessFile randomAccessFile = null;

        try {
            URL url = new URL(urlStr);
            connection = new ResolvedConnectionFactory(this).open(urlStr);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept-Encoding", "identity");

            if (userAgent != null && !userAgent.isEmpty()) {
                connection.setRequestProperty("User-Agent", userAgent);
            }

            // 添加 Cookie
            try {
                String cookies = android.webkit.CookieManager.getInstance().getCookie(urlStr);
                if (cookies != null && !cookies.isEmpty()) {
                    connection.setRequestProperty("Cookie", cookies);
                }
            } catch (Exception ignored) {}

            long resumeFrom = 0;
            if (file.exists() && file.length() > 0) {
                resumeFrom = file.length();
                // 设置 Range 请求头 — 断点续传核心
                connection.setRequestProperty("Range", "bytes=" + resumeFrom + "-");
            }

            int responseCode = connection.getResponseCode();

            // 处理重定向
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                String newUrl = connection.getHeaderField("Location");
                if (newUrl != null) {
                    // 递归下载新地址
                    downloadFile(newUrl, fileName, file, info, userAgent);
                    return;
                }
            }

            // 206 Partial Content 或 200 OK
            boolean isPartial = (responseCode == 206);
            boolean isFull = (responseCode == 200);

            if (!isPartial && !isFull) {
                notifyError(fileName, "服务器返回错误: " + responseCode);
                return;
            }

            // 获取总大小
            long totalBytes;
            if (isPartial) {
                // 从 Content-Range 解析总大小: "bytes 100-999/1000"
                String contentRange = connection.getHeaderField("Content-Range");
                if (contentRange != null) {
                    int slash = contentRange.lastIndexOf('/');
                    if (slash >= 0) {
                        try {
                            totalBytes = Long.parseLong(contentRange.substring(slash + 1).trim());
                        } catch (NumberFormatException e) {
                            totalBytes = resumeFrom + connection.getContentLengthLong();
                        }
                    } else {
                        totalBytes = resumeFrom + connection.getContentLengthLong();
                    }
                } else {
                    totalBytes = resumeFrom + connection.getContentLengthLong();
                }
            } else {
                totalBytes = connection.getContentLengthLong();
                if (totalBytes <= 0) totalBytes = -1;
            }

            info.totalBytes = totalBytes;
            info.downloadedBytes = resumeFrom;

            // 如果文件已完全下载（续传时可能出现）
            if (totalBytes > 0 && resumeFrom >= totalBytes) {
                notifyComplete(fileName, file.getAbsolutePath());
                cleanupTask(fileName);
                return;
            }

            inputStream = connection.getInputStream();
            randomAccessFile = new RandomAccessFile(file, "rw");
            // 定位到已下载位置尾部
            randomAccessFile.seek(resumeFrom);

            byte[] buffer = new byte[8192];
            int bytesRead;
            long lastNotifyTime = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // 检查是否被暂停
                if (info.paused) {
                    // 保存当前进度到 SharedPreferences
                    savePauseState(fileName, urlStr, info.downloadedBytes, info.totalBytes, userAgent);
                    notifyPaused(fileName, info.downloadedBytes, info.totalBytes);
                    cleanupTask(fileName);
                    return;
                }

                // 检查是否被停止
                if (info.stopped) {
                    // 停止时删除未完成文件，不保存暂停记录
                    try { randomAccessFile.close(); randomAccessFile = null; } catch (Exception ignored) {}
                    try { inputStream.close(); inputStream = null; } catch (Exception ignored) {}
                    try { file.delete(); } catch (Exception ignored) {}
                    cleanupTask(fileName);
                    return;
                }

                randomAccessFile.write(buffer, 0, bytesRead);
                info.downloadedBytes += bytesRead;

                // 更新通知和回调（每200ms一次，避免过于频繁）
                long now = System.currentTimeMillis();
                if (now - lastNotifyTime > 200) {
                    lastNotifyTime = now;
                    int percent = totalBytes > 0 ? (int) (info.downloadedBytes * 100 / totalBytes) : 0;
                    updateNotification(fileName, percent);
                    notifyProgress(fileName, info.downloadedBytes, totalBytes, percent);
                }
            }

            // 下载完成
            // 清除暂停记录
            removePauseState(fileName);
            // 直接保存下载记录到 SharedPreferences（不依赖回调）
            saveDownloadRecord(this, fileName, file.length(), System.currentTimeMillis());
            notifyComplete(fileName, file.getAbsolutePath());
            cleanupTask(fileName);

        } catch (java.net.SocketTimeoutException e) {
            // 超时 – 保存进度以便下次续传
            savePauseState(fileName, urlStr, info.downloadedBytes, info.totalBytes, userAgent);
            notifyError(fileName, "下载超时，已保存进度");
            cleanupTask(fileName);
        } catch (Exception e) {
            // 网络中断 – 保存进度以便下次续传
            if (info.downloadedBytes > 0) {
                savePauseState(fileName, urlStr, info.downloadedBytes, info.totalBytes, userAgent);
            }
            notifyError(fileName, "下载出错: " + e.getMessage());
            cleanupTask(fileName);
            // 删除可能损坏的文件
            try {
                if (file.exists() && file.length() == 0) file.delete();
            } catch (Exception ignored) {}
        } finally {
            try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
            try { if (randomAccessFile != null) randomAccessFile.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
            // 如果没有任何活跃下载了，停止前台服务
            if (activeDownloads.isEmpty()) {
                stopForeground(true);
                stopSelf();
            }
        }
    }

    /**
     * 暂停下载
     */
    public void pauseDownload(String fileName) {
        DownloadTaskInfo info = activeDownloads.get(fileName);
        if (info != null) {
            info.paused = true;
        }
    }

    /**
     * 停止下载：中断下载线程并删除文件
     */
    public void stopDownload(String fileName) {
        DownloadTaskInfo info = activeDownloads.get(fileName);
        if (info != null) {
            info.stopped = true;
        }
        // 取消暂停记录
        removePauseState(fileName);

        // 强制中断线程（解决 inputStream.read() 阻塞问题）
        Future<?> future = taskFutures.remove(fileName);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    /**
     * 获取所有已暂停且在等待续传的任务
     */
    public static Map<String, PausedTaskInfo> getPausedTasks(Context context) {
        Map<String, PausedTaskInfo> result = new HashMap<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_PAUSED, MODE_PRIVATE);
        String json = prefs.getString(KEY_RESUME_LIST, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                PausedTaskInfo info = new PausedTaskInfo();
                info.fileName = obj.optString("fileName", "");
                info.url = obj.optString("url", "");
                info.downloadedBytes = obj.optLong("downloadedBytes", 0);
                info.totalBytes = obj.optLong("totalBytes", -1);
                info.userAgent = obj.optString("userAgent", "");
                if (!info.fileName.isEmpty() && !info.url.isEmpty()) {
                    result.put(info.fileName, info);
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    public static class PausedTaskInfo {
        public String fileName;
        public String url;
        public long downloadedBytes;
        public long totalBytes;
        public String userAgent;
    }

    /**
     * 获取保存的总字节数（用于续传时展示进度）
     */
    private long getSavedTotalBytes(String fileName) {
        SharedPreferences prefs = getSharedPreferences(PREF_PAUSED, MODE_PRIVATE);
        String json = prefs.getString(KEY_RESUME_LIST, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (obj.optString("fileName", "").equals(fileName)) {
                    return obj.optLong("totalBytes", -1);
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private void savePauseState(String fileName, String url, long downloaded, long total, String userAgent) {
        SharedPreferences prefs = getSharedPreferences(PREF_PAUSED, MODE_PRIVATE);
        JSONArray arr = new JSONArray();
        String json = prefs.getString(KEY_RESUME_LIST, "[]");
        try {
            arr = new JSONArray(json);
        } catch (Exception ignored) {}

        // 移除旧记录（若有）
        JSONArray newArr = new JSONArray();
        boolean found = false;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject obj = arr.getJSONObject(i);
                if (obj.optString("fileName", "").equals(fileName)) {
                    // 更新
                    obj.put("url", url);
                    obj.put("downloadedBytes", downloaded);
                    obj.put("totalBytes", total);
                    obj.put("userAgent", userAgent != null ? userAgent : "");
                    newArr.put(obj);
                    found = true;
                } else {
                    newArr.put(obj);
                }
            } catch (Exception ignored) {}
        }

        if (!found) {
            try {
                JSONObject record = new JSONObject();
                record.put("fileName", fileName);
                record.put("url", url);
                record.put("downloadedBytes", downloaded);
                record.put("totalBytes", total);
                record.put("userAgent", userAgent != null ? userAgent : "");
                newArr.put(record);
            } catch (Exception ignored) {}
        }

        prefs.edit().putString(KEY_RESUME_LIST, newArr.toString()).apply();
    }

    public static void removePauseState(Context context, String fileName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_PAUSED, MODE_PRIVATE);
        String json = prefs.getString(KEY_RESUME_LIST, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.optString("fileName", "").equals(fileName)) {
                    newArr.put(obj);
                }
            }
            prefs.edit().putString(KEY_RESUME_LIST, newArr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void removePauseState(String fileName) {
        removePauseState(this, fileName);
    }

    private void cleanupTask(String fileName) {
        activeDownloads.remove(fileName);
        taskFutures.remove(fileName);
        if (activeDownloads.isEmpty()) {
            stopForeground(true);
            stopSelf();
        }
    }

    // ========== 通知 & 回调 ==========

    private void notifyProgress(String fileName, long downloaded, long total, int percent) {
        if (callback != null) {
            mainHandler.post(() -> callback.onProgress(fileName, downloaded, total, percent));
        }
    }

    private void notifyComplete(String fileName, String filePath) {
        if (callback != null) {
            mainHandler.post(() -> callback.onComplete(fileName, filePath));
        }
    }

    private void notifyError(String fileName, String error) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(fileName, error));
        }
    }

    private void notifyPaused(String fileName, long downloaded, long total) {
        if (callback != null) {
            mainHandler.post(() -> callback.onPaused(fileName, downloaded, total));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "下载服务",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("显示下载进度");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void updateNotification(String fileName, int percent) {
        Notification notification = buildNotification(fileName, percent);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String fileName, int percent) {
        Intent intent = new Intent(this, DownloadManagerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String content = percent > 0 ? fileName + " (" + percent + "%)" : fileName;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("正在下载")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if (percent > 0) {
            builder.setProgress(100, percent, false);
        } else {
            builder.setProgress(0, 0, true);
        }

        return builder.build();
    }

    /**
     * 保存下载完成记录到 SharedPreferences（与 DownloadManagerActivity 共享）
     * 即使 Activity 未打开也会持久化保存
     */
    private static void saveDownloadRecord(Context context, String fileName, long fileSize, long lastModified) {
        SharedPreferences prefs = context.getSharedPreferences("download_history", Context.MODE_PRIVATE);
        org.json.JSONArray arr;
        String json = prefs.getString("records", "[]");
        try {
            arr = new org.json.JSONArray(json);
        } catch (Exception e) {
            arr = new org.json.JSONArray();
        }

        // 检查是否已存在同名记录
        boolean found = false;
        for (int i = 0; i < arr.length(); i++) {
            try {
                org.json.JSONObject obj = arr.getJSONObject(i);
                if (obj.optString("fileName", "").equals(fileName)) {
                    obj.put("fileSize", fileSize);
                    obj.put("lastModified", lastModified);
                    found = true;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (!found) {
            try {
                org.json.JSONObject record = new org.json.JSONObject();
                record.put("fileName", fileName);
                record.put("fileSize", fileSize);
                record.put("lastModified", lastModified);
                arr.put(record);
            } catch (Exception ignored) {}
        }

        prefs.edit().putString("records", arr.toString()).apply();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 停止所有下载
        for (Map.Entry<String, Future<?>> entry : taskFutures.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isDone()) {
                entry.getValue().cancel(true);
            }
        }
        activeDownloads.clear();
        taskFutures.clear();
    }
}