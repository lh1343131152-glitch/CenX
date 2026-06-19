package com.centigrade.browser.net;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Map;

public class WebResourceResolveHelper {

    private final ResolvedConnectionFactory connectionFactory;

    public WebResourceResolveHelper(Context context) {
        this.connectionFactory = new ResolvedConnectionFactory(context);
    }

    public WebResourceResponse loadGet(String url, Map<String, String> headers) {
        HttpURLConnection conn = null;
        try {
            conn = connectionFactory.open(url);
            conn.setRequestMethod("GET");
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
            }

            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (stream == null) return null;

            String mime = guessMime(conn.getContentType());
            String encoding = guessEncoding(conn.getContentType());

            WebResourceResponse response = new WebResourceResponse(mime, encoding, stream);
            response.setStatusCodeAndReasonPhrase(code, conn.getResponseMessage());
            return response;
        } catch (Exception ignored) {
            if (conn != null) conn.disconnect();
            return null;
        }
    }

    private String guessMime(String contentType) {
        if (TextUtils.isEmpty(contentType)) return "text/plain";
        String[] parts = contentType.split(";");
        return parts.length > 0 ? parts[0].trim() : "text/plain";
    }

    private String guessEncoding(String contentType) {
        if (TextUtils.isEmpty(contentType)) return "utf-8";
        String lower = contentType.toLowerCase();
        int index = lower.indexOf("charset=");
        if (index == -1) return "utf-8";
        return contentType.substring(index + 8).trim();
    }
}