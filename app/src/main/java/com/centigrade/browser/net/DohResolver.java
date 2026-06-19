package com.centigrade.browser.net;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DohResolver {

    public ResolvedAddress resolve(String host) {
        if (TextUtils.isEmpty(host)) return null;

        ResolvedAddress result = resolveByCloudflare(host, NetResolveConfig.DOH_PRIMARY);
        if (result != null) return result;

        result = resolveByCloudflare(host, NetResolveConfig.DOH_BACKUP);
        if (result != null) return result;

        for (String fallback : NetResolveConfig.PURE_DNS_FALLBACKS) {
            result = resolveByGenericJson(host, fallback);
            if (result != null) return result;
        }
        return null;
    }

    private ResolvedAddress resolveByCloudflare(String host, String endpoint) {
        HttpURLConnection conn = null;
        try {
            String url = endpoint + "?name=" + host + "&type=A";
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(NetResolveConfig.CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(NetResolveConfig.READ_TIMEOUT_MS);
            conn.setRequestProperty("accept", "application/dns-json");
            conn.setRequestProperty("user-agent", NetResolveConfig.UA);

            String body = readString(conn.getInputStream());
            JSONObject json = new JSONObject(body);
            JSONArray answer = json.optJSONArray("Answer");
            if (answer == null) return null;

            for (int i = 0; i < answer.length(); i++) {
                JSONObject item = answer.optJSONObject(i);
                if (item == null) continue;
                if (item.optInt("type") == 1) {
                    String ip = item.optString("data");
                    int ttl = item.optInt("TTL", (int) (NetResolveConfig.DEFAULT_CACHE_TTL_MS / 1000L));
                    if (!TextUtils.isEmpty(ip)) {
                        return new ResolvedAddress(host, ip, System.currentTimeMillis(), ttl * 1000L, "doh_cloudflare");
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private ResolvedAddress resolveByGenericJson(String host, String endpoint) {
        HttpURLConnection conn = null;
        try {
            String url;
            if (endpoint.contains("/resolve")) {
                url = endpoint + "?name=" + host + "&type=A";
            } else {
                url = endpoint + "?name=" + host + "&type=A";
            }

            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(NetResolveConfig.CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(NetResolveConfig.READ_TIMEOUT_MS);
            conn.setRequestProperty("accept", "application/dns-json");
            conn.setRequestProperty("user-agent", NetResolveConfig.UA);

            String body = readString(conn.getInputStream());
            JSONObject json = new JSONObject(body);
            JSONArray answer = json.optJSONArray("Answer");
            if (answer == null) return null;

            for (int i = 0; i < answer.length(); i++) {
                JSONObject item = answer.optJSONObject(i);
                if (item == null) continue;
                if (item.optInt("type") == 1) {
                    String ip = item.optString("data");
                    int ttl = item.optInt("TTL", (int) (NetResolveConfig.DEFAULT_CACHE_TTL_MS / 1000L));
                    if (!TextUtils.isEmpty(ip)) {
                        return new ResolvedAddress(host, ip, System.currentTimeMillis(), ttl * 1000L, "doh_fallback");
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private String readString(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}