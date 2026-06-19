package com.centigrade.browser;

import android.app.DownloadManager;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Java 版 Lightning FetchUrlMimeType。
 * 当 WebView 没给出准确 MIME 时，先请求响应头修正 MIME/文件名，再交给 DownloadManager。
 */
class LightningFetchUrlMimeType {

    private final DownloadManager.Request request;
    private final DownloadManager downloadManager;
    private final String uri;
    private final String cookies;
    private final String userAgent;

    LightningFetchUrlMimeType(DownloadManager downloadManager,
                              DownloadManager.Request request,
                              String uri,
                              String cookies,
                              String userAgent) {
        this.request = request;
        this.downloadManager = downloadManager;
        this.uri = uri;
        this.cookies = cookies;
        this.userAgent = userAgent;
    }

    Result run() {
        String mimeType = null;
        String contentDisposition = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(uri);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            if (!TextUtils.isEmpty(cookies)) {
                connection.addRequestProperty("Cookie", cookies);
            }
            if (!TextUtils.isEmpty(userAgent)) {
                connection.setRequestProperty("User-Agent", userAgent);
            }
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String header = connection.getHeaderField("Content-Type");
                if (!TextUtils.isEmpty(header)) {
                    mimeType = header;
                    int semicolonIndex = mimeType.indexOf(';');
                    if (semicolonIndex != -1) {
                        mimeType = mimeType.substring(0, semicolonIndex).trim();
                    }
                }

                String cd = connection.getHeaderField("Content-Disposition");
                if (!TextUtils.isEmpty(cd)) {
                    contentDisposition = cd;
                }
            }
        } catch (IllegalArgumentException | IOException ignored) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        if (!TextUtils.isEmpty(mimeType)) {
            if ("text/plain".equalsIgnoreCase(mimeType)
                    || "application/octet-stream".equalsIgnoreCase(mimeType)) {
                String ext = LightningDownloadHandler.guessFileExtension(uri);
                String newMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                if (!TextUtils.isEmpty(newMimeType)) {
                    request.setMimeType(newMimeType);
                }
            } else {
                request.setMimeType(mimeType);
            }

            String filename = URLUtil.guessFileName(uri, contentDisposition, mimeType);
            request.setDestinationUri(android.net.Uri.fromFile(new java.io.File(LightningDownloadHandler.getCenXDownloadDir(), filename)));
        }

        try {
            downloadManager.enqueue(request);
            return Result.SUCCESS;
        } catch (IllegalArgumentException e) {
            return Result.FAILURE_ENQUEUE;
        } catch (SecurityException e) {
            return Result.FAILURE_LOCATION;
        }
    }

    enum Result {
        FAILURE_ENQUEUE,
        FAILURE_LOCATION,
        SUCCESS
    }
}