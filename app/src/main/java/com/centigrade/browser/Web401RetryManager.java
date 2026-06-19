package com.centigrade.browser;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.CookieManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 401 自动重试管理器（GeckoEngine WebView 适配版）
 * 规则：每个URL最多自动重试一次、重试前flush Cookie并重新加载
 */
public class Web401RetryManager {

    private final Set<String> retriedUrls = new HashSet<>();

    /** 判断URL是否应重试（由外部判断是否401后调用） */
    public boolean shouldRetry(String url) {
        return !TextUtils.isEmpty(url) && !retriedUrls.contains(url);
    }

    /** 执行重试：flush Cookie -> 重新构建站点级Header -> 重新加载 */
    public void retry(GeckoEngine engine, String url, Context context) {
        if (engine == null || TextUtils.isEmpty(url) || context == null) return;

        // 标记已重试，防止死循环
        retriedUrls.add(url);

        // 先强制把内存Cookie写盘
        try {
            CookieManager.getInstance().flush();
        } catch (Throwable ignored) {}

        // 重新加载
        engine.loadUrl(url);
    }

    /** 页面加载成功后清除该URL的重试标记，下次如果再401还能重试 */
    public void clearSuccess(String url) {
        if (!TextUtils.isEmpty(url)) {
            retriedUrls.remove(url);
        }
    }

    /** 清除所有重试记录 */
    public void clearAll() {
        retriedUrls.clear();
    }
}