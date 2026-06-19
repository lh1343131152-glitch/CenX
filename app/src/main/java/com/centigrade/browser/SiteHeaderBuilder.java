package com.centigrade.browser;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.CookieManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 站点请求头构建器
 * 每次主页面加载时自动合成：默认浏览器头 + 系统Cookie + 站点级鉴权配置
 */
public class SiteHeaderBuilder {

    public static Map<String, String> build(Context context, String url) {
        Map<String, String> headers = new HashMap<>();
        if (context == null || TextUtils.isEmpty(url)) return headers;

        PreferencesManager prefs = PreferencesManager.getInstance(context);
        SiteAuthManager siteAuthManager = new SiteAuthManager(context);
        SiteAuthConfig config = siteAuthManager.findConfigByUrl(url);

        // 1. 默认浏览器基础请求头
        headers.put("User-Agent", prefs.getUserAgentString(prefs.getUserAgent()));
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Cache-Control", "no-cache");
        headers.put("Pragma", "no-cache");
        headers.put("Connection", "keep-alive");
        headers.put("Upgrade-Insecure-Requests", "1");
        if (prefs.isDoNotTrackEnabled()) {
            headers.put("DNT", "1");
        }
        if (prefs.isSaveDataEnabled()) {
            headers.put("Save-Data", "on");
        }
        if (prefs.isSendXRequestedWithEnabled()) {
            headers.put("X-Requested-With", context.getPackageName());
        }

        // 2. 获取系统 WebView CookieManager 中已存储的 Cookie
        String systemCookie = CookieManager.getInstance().getCookie(url);

        // 3. 站点级鉴权配置注入
        if (config != null && config.enableAuthInjection) {

            // 覆盖 User-Agent
            if (!TextUtils.isEmpty(config.userAgentOverride)) {
                headers.put("User-Agent", config.userAgentOverride);
            }

            // 注入 Authorization
            if (!TextUtils.isEmpty(config.authorization)) {
                headers.put("Authorization", config.authorization);
            }

            // 注入 Referer
            if (!TextUtils.isEmpty(config.referer)) {
                headers.put("Referer", config.referer);
            }

            // 注入 Origin
            if (!TextUtils.isEmpty(config.origin)) {
                headers.put("Origin", config.origin);
            }

            // 合并系统 Cookie 和站点自定义 Cookie
            String mergedCookie = mergeCookies(systemCookie, config.cookie);
            if (!TextUtils.isEmpty(mergedCookie)) {
                headers.put("Cookie", mergedCookie);
            } else if (!TextUtils.isEmpty(systemCookie)) {
                headers.put("Cookie", systemCookie);
            }

            // 注入额外的自定义 JSON 请求头
            if (!TextUtils.isEmpty(config.extraHeadersJson)) {
                try {
                    JSONObject obj = new JSONObject(config.extraHeadersJson);
                    Iterator<String> keys = obj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = obj.optString(key, "");
                        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                            headers.put(key, value);
                        }
                    }
                } catch (Exception ignored) {}
            }
        } else {
            // 没有站点配置时，只携带系统Cookie
            if (!TextUtils.isEmpty(systemCookie)) {
                headers.put("Cookie", systemCookie);
            }
        }

        // 4. 自动补 Origin 头（如果没有显式设置的话）
        if (!headers.containsKey("Origin")) {
            try {
                Uri uri = Uri.parse(url);
                String scheme = uri.getScheme();
                String host = uri.getHost();
                int port = uri.getPort();
                if (!TextUtils.isEmpty(scheme) && !TextUtils.isEmpty(host)) {
                    String origin = scheme + "://" + host + (port > 0 ? ":" + port : "");
                    headers.put("Origin", origin);
                }
            } catch (Exception ignored) {}
        }

        return headers;
    }

    /**
     * 合并系统 Cookie 和站点级自定义 Cookie
     */
    public static String mergeCookies(String systemCookie, String extraCookie) {
        if (TextUtils.isEmpty(systemCookie)) return extraCookie == null ? "" : extraCookie.trim();
        if (TextUtils.isEmpty(extraCookie)) return systemCookie.trim();

        String s1 = systemCookie.trim();
        String s2 = extraCookie.trim();

        if (s1.endsWith(";")) s1 = s1.substring(0, s1.length() - 1).trim();
        if (s2.startsWith(";")) s2 = s2.substring(1).trim();

        if (TextUtils.isEmpty(s1)) return s2;
        if (TextUtils.isEmpty(s2)) return s1;

        return s1 + "; " + s2;
    }
}