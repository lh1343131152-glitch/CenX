package com.centigrade.browser;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

/**
 * 参考 Lightning Browser DownloadHandler 的下载处理器。
 *
 * 目标：
 * 1. 抛弃 CenX 原来的直接 enqueue 简单下载逻辑；
 * 2. 统一使用接近 Lightning 的 DownloadManager 流程；
 * 3. 支持可流式播放内容优先交给外部应用；
 * 4. 支持 SD 卡/下载目录检查；
 * 5. 支持 Cookie、User-Agent；
 * 6. 支持 MIME 为空时后台探测后再入队；
 * 7. 不修改原有下载确认弹窗布局。
 */
public class LightningDownloadHandler {

    private static final String COOKIE_REQUEST_HEADER = "Cookie";

    public static void startDownload(Activity activity,
                                     String url,
                                     String userAgent,
                                     String contentDisposition,
                                     String mimeType,
                                     long contentLength) {
        if (activity == null || TextUtils.isEmpty(url)) return;

        if (!isForcedDownload(url, mimeType)
                && tryOpenStreamable(activity, url, contentDisposition, mimeType)) {
            return;
        }

        startDownloadNoStream(activity, url, userAgent, contentDisposition, mimeType);
    }

    public static void startDownloadNoStream(Activity activity,
                                             String url,
                                             String userAgent,
                                             String contentDisposition,
                                             String mimeType) {
        if (activity == null || TextUtils.isEmpty(url)) return;

        String status = Environment.getExternalStorageState();
        if (Environment.MEDIA_SHARED.equals(status)) {
            Toast.makeText(activity, "存储设备正忙，无法下载", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Environment.MEDIA_MOUNTED.equals(status)) {
            Toast.makeText(activity, "外部存储不可用，无法下载", Toast.LENGTH_SHORT).show();
            return;
        }

        String encodedUrl = encodeSpecialPathChars(url);
        Uri uri;
        try {
            uri = Uri.parse(encodedUrl);
        } catch (Exception e) {
            Toast.makeText(activity, "下载链接无效", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(uri);
        } catch (IllegalArgumentException e) {
            Toast.makeText(activity, "无法创建下载任务", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        File downloadDir = getCenXDownloadDir();
        if (!isWriteAccessAvailable(downloadDir)) {
            Toast.makeText(activity, "下载目录不可写", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(mimeType)) {
            String fixedMime = normalizeMimeType(fileName, mimeType);
            if (!TextUtils.isEmpty(fixedMime)) {
                request.setMimeType(fixedMime);
            }
        }

        request.setDestinationUri(Uri.fromFile(new File(downloadDir, fileName)));
        request.setTitle(fileName);
        request.setDescription(Uri.parse(url).getHost());
        request.setVisibleInDownloadsUi(true);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setAllowedOverRoaming(true);

        String cookies = CookieManager.getInstance().getCookie(url);
        if (!TextUtils.isEmpty(cookies)) {
            request.addRequestHeader(COOKIE_REQUEST_HEADER, cookies);
        }
        if (!TextUtils.isEmpty(userAgent)) {
            request.addRequestHeader("User-Agent", userAgent);
        }

        DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            Toast.makeText(activity, "系统下载服务不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(mimeType)) {
            new Thread(() -> {
                LightningFetchUrlMimeType.Result result =
                        new LightningFetchUrlMimeType(downloadManager, request, encodedUrl, cookies, userAgent)
                                .run();
                activity.runOnUiThread(() -> handleResult(activity, result, fileName));
            }).start();
        } else {
            try {
                downloadManager.enqueue(request);
                saveDownloadRecord(activity, url, fileName, "未知大小");
                saveLegacyDownloadRecord(activity, fileName, 0, System.currentTimeMillis());
                Toast.makeText(activity, "下载已开始: " + fileName, Toast.LENGTH_SHORT).show();
            } catch (IllegalArgumentException e) {
                Toast.makeText(activity, "无法加入下载队列", Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Toast.makeText(activity, "下载目录权限不足", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static boolean isForcedDownload(String url, String mimeType) {
        String lowerUrl = url == null ? "" : url.toLowerCase();
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase();

        if (lowerMime.contains("application/vnd.android.package-archive")) return true;
        if (lowerMime.contains("application/zip")) return true;
        if (lowerMime.contains("application/x-zip-compressed")) return true;
        if (lowerMime.contains("application/x-rar")) return true;
        if (lowerMime.contains("application/x-7z-compressed")) return true;
        if (lowerMime.contains("application/octet-stream")) return true;

        return lowerUrl.matches(".*\\.(apk|apks|xapk|zip|rar|7z|tar|gz|bz2|xz|exe|msi|dmg|pkg|deb|rpm|iso)(\\?.*)?$");
    }

    private static boolean tryOpenStreamable(Activity activity,
                                             String url,
                                             String contentDisposition,
                                             String mimeType) {
        if (!TextUtils.isEmpty(contentDisposition)
                && contentDisposition.regionMatches(true, 0, "attachment", 0, 10)) {
            return false;
        }

        if (TextUtils.isEmpty(mimeType)) {
            return false;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), mimeType);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setComponent(null);
            intent.setSelector(null);

            ResolveInfo info = activity.getPackageManager().resolveActivity(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
            );

            if (info != null && info.activityInfo != null) {
                String pkg = info.activityInfo.packageName;
                if (!activity.getPackageName().equals(pkg)) {
                    try {
                        activity.startActivity(intent);
                        return true;
                    } catch (ActivityNotFoundException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static void handleResult(Activity activity,
                                     LightningFetchUrlMimeType.Result result,
                                     String fileName) {
        if (result == LightningFetchUrlMimeType.Result.SUCCESS) {
            saveDownloadRecord(activity, "", fileName, "未知大小");
            Toast.makeText(activity, "下载已开始: " + fileName, Toast.LENGTH_SHORT).show();
        } else if (result == LightningFetchUrlMimeType.Result.FAILURE_LOCATION) {
            Toast.makeText(activity, "下载目录权限不足", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, "无法加入下载队列", Toast.LENGTH_SHORT).show();
        }
    }

    private static String normalizeMimeType(String fileName, String mimeType) {
        if (TextUtils.isEmpty(mimeType)) return mimeType;
        if ("text/plain".equalsIgnoreCase(mimeType)
                || "application/octet-stream".equalsIgnoreCase(mimeType)) {
            String ext = guessFileExtension(fileName);
            String fixed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            return TextUtils.isEmpty(fixed) ? mimeType : fixed;
        }
        int semicolon = mimeType.indexOf(';');
        if (semicolon >= 0) {
            return mimeType.substring(0, semicolon).trim();
        }
        return mimeType;
    }

    static String guessFileExtension(String fileNameOrUrl) {
        if (TextUtils.isEmpty(fileNameOrUrl)) return "";
        String clean = fileNameOrUrl;
        int q = clean.indexOf('?');
        if (q >= 0) clean = clean.substring(0, q);
        int hash = clean.indexOf('#');
        if (hash >= 0) clean = clean.substring(0, hash);
        int dot = clean.lastIndexOf('.');
        if (dot >= 0 && dot < clean.length() - 1) {
            return clean.substring(dot + 1).toLowerCase();
        }
        return "";
    }

    private static String encodeSpecialPathChars(String url) {
        if (TextUtils.isEmpty(url)) return url;
        StringBuilder sb = new StringBuilder();
        boolean changed = false;
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '[' || c == ']' || c == '|') {
                sb.append('%').append(Integer.toHexString(c));
                changed = true;
            } else {
                sb.append(c);
            }
        }
        return changed ? sb.toString() : url;
    }

    static File getCenXDownloadDir() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CenX");
    }

    private static boolean isWriteAccessAvailable(File dir) {
        if (dir == null) return false;
        if (!dir.isDirectory() && !dir.mkdirs()) {
            return false;
        }
        File probe = new File(dir, ".cenx_download_probe");
        try {
            if (probe.createNewFile()) {
                //noinspection ResultOfMethodCallIgnored
                probe.delete();
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void saveLegacyDownloadRecord(Context context, String fileName, long fileSize, long lastModified) {
        try {
            android.content.SharedPreferences prefs =
                    context.getSharedPreferences("download_history", Context.MODE_PRIVATE);
            org.json.JSONArray arr = new org.json.JSONArray(prefs.getString("records", "[]"));
            boolean found = false;
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject obj = arr.optJSONObject(i);
                if (obj != null && fileName.equals(obj.optString("fileName", ""))) {
                    obj.put("fileSize", fileSize);
                    obj.put("lastModified", lastModified);
                    found = true;
                    break;
                }
            }
            if (!found) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("fileName", fileName == null ? "" : fileName);
                obj.put("fileSize", fileSize);
                obj.put("lastModified", lastModified);
                arr.put(obj);
            }
            prefs.edit().putString("records", arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private static void saveDownloadRecord(Context context, String url, String title, String size) {
        try {
            android.content.SharedPreferences prefs =
                    context.getSharedPreferences("lightning_downloads", Context.MODE_PRIVATE);
            org.json.JSONArray arr = new org.json.JSONArray(prefs.getString("items", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject old = arr.optJSONObject(i);
                if (old != null && title.equals(old.optString("title"))) {
                    return;
                }
            }
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("url", url == null ? "" : url);
            obj.put("title", title == null ? "" : title);
            obj.put("size", size == null ? "" : size);
            obj.put("time", System.currentTimeMillis());
            arr.put(obj);
            prefs.edit().putString("items", arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }
}