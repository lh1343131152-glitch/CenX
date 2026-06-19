package com.centigrade.browser.ai;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class AiSelfNet {

    public interface NetCallback {
        void onSuccess(int code, String body);
        void onError(String error);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public void get(String url, Map<String, String> headers, NetCallback callback) {
        request("GET", url, headers, null, callback);
    }

    public void post(String url, Map<String, String> headers, String body, NetCallback callback) {
        request("POST", url, headers, body, callback);
    }

    public void request(String method, String url, Map<String, String> headers, String body, NetCallback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "CenX-AI/1.0");
                conn.setRequestProperty("Accept", "application/json,text/plain,*/*");

                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            conn.setRequestProperty(entry.getKey(), entry.getValue());
                        }
                    }
                }

                if ("POST".equalsIgnoreCase(method)) {
                    conn.setDoOutput(true);
                    if (TextUtils.isEmpty(conn.getRequestProperty("Content-Type"))) {
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    }
                    String requestBody = body == null ? "" : body;
                    OutputStream os = conn.getOutputStream();
                    os.write(requestBody.getBytes("UTF-8"));
                    os.flush();
                    os.close();
                }

                int code = conn.getResponseCode();
                InputStream input = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                String text = readString(input);

                int finalCode = code;
                String finalText = text;
                MAIN.post(() -> {
                    if (callback != null) callback.onSuccess(finalCode, finalText);
                });
            } catch (Exception e) {
                String error = e.getClass().getSimpleName() + ": " + e.getMessage();
                MAIN.post(() -> {
                    if (callback != null) callback.onError(error);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private String readString(InputStream inputStream) {
        if (inputStream == null) return "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            reader.close();
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }
}