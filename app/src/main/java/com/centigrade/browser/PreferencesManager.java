package com.centigrade.browser;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "browser_prefs";
    private static final String KEY_UA = "user_agent";
    private static final String KEY_SEARCH = "search_engine";
    private static final String KEY_HOMEPAGE = "homepage";
    private static final String KEY_CUSTOM_SEARCH = "custom_search_url";
    private static final String KEY_INCOGNITO = "incognito_mode";
    private static final String KEY_ADBLOCK = "adblock_enabled";
    private static final String KEY_RESOURCE_SNIFF = "resource_sniff_enabled";
    private static final String KEY_HOMEPAGE_TYPE = "homepage_type"; // "default" 或 "custom"
    private static final String KEY_QUICK_LINKS = "quick_links_json"; // JSON数组存储快捷链接
    private static final String KEY_HOME_BACKGROUND = "home_background";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String KEY_DOH_WEBVIEW_ENABLED = "doh_webview_enabled";
    private static final String KEY_RENDER_MODE = "render_mode";
    private static final String KEY_CACHE_MODE_PREF = "cache_mode_pref";
    private static final String KEY_LOAD_MODE = "load_mode";
    public static final String KEY_DYNAMIC_COLOR_ENABLED = "dynamic_color_enabled";
    private static final String KEY_SUPPORT_ZOOM = "support_zoom";
    private static final String KEY_BUILTIN_ZOOM_CONTROLS = "builtin_zoom_controls";
    private static final String KEY_DISPLAY_ZOOM_CONTROLS = "display_zoom_controls";
    private static final String KEY_WIDE_VIEWPORT = "wide_viewport";
    private static final String KEY_LOAD_WITH_OVERVIEW = "load_with_overview";
    private static final String KEY_ALLOW_FILE_ACCESS = "allow_file_access";
    private static final String KEY_ALLOW_CONTENT_ACCESS = "allow_content_access";
    private static final String KEY_ACCEPT_COOKIES = "accept_cookies";
    private static final String KEY_DOM_STORAGE_ENABLED = "dom_storage_enabled";
    private static final String KEY_DATABASE_ENABLED = "database_enabled";
    private static final String KEY_SAVE_FORM_DATA = "save_form_data";
    private static final String KEY_GEOLOCATION_ENABLED = "geolocation_enabled";
    private static final String KEY_JAVASCRIPT_CAN_OPEN_WINDOWS = "javascript_can_open_windows";
    private static final String KEY_SUPPORT_MULTIPLE_WINDOWS = "support_multiple_windows";
    private static final String KEY_ACCEPT_THIRD_PARTY_COOKIES = "accept_third_party_cookies";
    private static final String KEY_TEXT_ZOOM = "text_zoom";
    private static final String KEY_DEFAULT_TEXT_ENCODING = "default_text_encoding";
    private static final String KEY_DO_NOT_TRACK = "do_not_track";
    private static final String KEY_SAVE_DATA = "save_data";
    private static final String KEY_SEND_X_REQUESTED_WITH = "send_x_requested_with";
    
    private static PreferencesManager instance;
    private SharedPreferences prefs;
    
    private PreferencesManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized PreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context);
        }
        return instance;
    }

    public SharedPreferences getSharedPreferences() {
        return prefs;
    }
    
    public void setUserAgent(int uaIndex) {
        prefs.edit().putInt(KEY_UA, uaIndex).apply();
    }
    
    public int getUserAgent() {
        return prefs.getInt(KEY_UA, 0);
    }
    
    public void setSearchEngine(int searchIndex) {
        prefs.edit().putInt(KEY_SEARCH, searchIndex).apply();
    }
    
    public int getSearchEngine() {
        return prefs.getInt(KEY_SEARCH, 0);
    }
    
    public void setHomepage(String url) {
        prefs.edit().putString(KEY_HOMEPAGE, url).apply();
    }
    
    public String getHomepage() {
        String hp = prefs.getString(KEY_HOMEPAGE, "");
        if (hp == null || hp.isEmpty()) {
            return "about:home";
        }
        return hp;
    }
    
    public void setCustomSearchUrl(String url) {
        prefs.edit().putString(KEY_CUSTOM_SEARCH, url).apply();
    }
    
    public String getCustomSearchUrl() {
        return prefs.getString(KEY_CUSTOM_SEARCH, "");
    }
    
    // === 无痕模式 ===
    public boolean isIncognitoMode() {
        return prefs.getBoolean(KEY_INCOGNITO, false);
    }
    
    public void setIncognitoMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_INCOGNITO, enabled).apply();
    }
    
    // === 广告拦截 ===
    public boolean isAdBlockEnabled() {
        return prefs.getBoolean(KEY_ADBLOCK, true);
    }
    
    public void setAdBlockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ADBLOCK, enabled).apply();
    }
    
    // === 资源嗅探 ===
    public boolean isResourceSniffEnabled() {
        return prefs.getBoolean(KEY_RESOURCE_SNIFF, true);
    }
    
    public void setResourceSniffEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_RESOURCE_SNIFF, enabled).apply();
    }
    
    public String getUserAgentString(int uaIndex) {
        switch (uaIndex) {
            case 0:
                return "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36";
            case 1:
                return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36";
            case 2:
                return "Mozilla/5.0 (Android 14; Mobile; rv:124.0) Gecko/124.0 Firefox/124.0";
            case 3:
                return "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) EdgA/123.0.0.0 Mobile Safari/537.36";
            case 4:
                return "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
            case 5:
                return "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
            default:
                return "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36";
        }
    }
    
    public String getSearchEngineUrl(int searchIndex) {
        switch (searchIndex) {
            case 0: return "https://www.baidu.com/s?wd=%s";
            case 1: return "https://www.google.com/search?q=%s";
            case 2: return "https://www.bing.com/search?q=%s";
            case 3: return "https://www.sogou.com/web?query=%s";
            case 4: return "https://duckduckgo.com/?q=%s";
            case 5: return "https://metaso.cn/search/72e85006-68f8-4013-8317-c8d9866e871b?s=nyzav&referrer_s=nyzav&question=%s";
            case 6: // 自定义
                String custom = getCustomSearchUrl();
                if (custom != null && !custom.isEmpty()) {
                    return custom;
                }
                return "https://www.baidu.com/s?wd=%s";
            default: return "https://www.baidu.com/s?wd=%s";
        }
    }
    
    // === 首页类型 ===
    public void setHomepageType(String type) {
        prefs.edit().putString(KEY_HOMEPAGE_TYPE, type).apply();
    }

    public String getHomepageType() {
        return prefs.getString(KEY_HOMEPAGE_TYPE, "default"); // 默认使用默认首页
    }

    // === 快捷链接管理（JSON序列化） ===
    public void setQuickLinks(java.util.List<QuickLink> links) {
        org.json.JSONArray arr = new org.json.JSONArray();
        try {
            for (QuickLink link : links) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("name", link.name);
                obj.put("url", link.url);
                obj.put("icon", link.icon);
                obj.put("iconType", link.iconType != null ? link.iconType : "letter");
                obj.put("iconColor", link.iconColor != null ? link.iconColor : "");
                obj.put("imagePath", link.imagePath != null ? link.imagePath : "");
                arr.put(obj);
            }
        } catch (Exception ignored) {}
        prefs.edit().putString(KEY_QUICK_LINKS, arr.toString()).apply();
    }

    public java.util.List<QuickLink> getQuickLinks() {
        java.util.List<QuickLink> links = new java.util.ArrayList<>();
        String json = prefs.getString(KEY_QUICK_LINKS, "");
        if (json == null || json.isEmpty()) {
            // 返回默认快捷链接
            links.add(new QuickLink("百度", "https://www.baidu.com", "B"));
            links.add(new QuickLink("B站", "https://www.bilibili.com", "B"));
            links.add(new QuickLink("知乎", "https://www.zhihu.com", "知"));
            links.add(new QuickLink("GitHub", "https://github.com", "G"));
            return links;
        }
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject obj = arr.getJSONObject(i);
                QuickLink link = new QuickLink(
                    obj.optString("name", ""),
                    obj.optString("url", ""),
                    obj.optString("icon", "•"),
                    obj.optString("iconType", "letter"),
                    obj.optString("iconColor", ""),
                    obj.optString("imagePath", "")
                );
                if (!link.url.isEmpty()) {
                    links.add(link);
                }
            }
        } catch (Exception ignored) {}
        // 如果为空也返回默认
        if (links.isEmpty()) {
            links.add(new QuickLink("百度", "https://www.baidu.com", "B"));
            links.add(new QuickLink("B站", "https://www.bilibili.com", "B"));
            links.add(new QuickLink("知乎", "https://www.zhihu.com", "知"));
            links.add(new QuickLink("GitHub", "https://github.com", "G"));
        }
        return links;
    }

    public void setHomeBackground(String path) {
        prefs.edit().putString(KEY_HOME_BACKGROUND, path != null ? path : "").apply();
    }

    public String getHomeBackground() {
        return prefs.getString(KEY_HOME_BACKGROUND, "");
    }

    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public boolean isDohWebViewEnabled() {
        return prefs.getBoolean(KEY_DOH_WEBVIEW_ENABLED, false);
    }

    public void setDohWebViewEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DOH_WEBVIEW_ENABLED, enabled).apply();
    }

    public int getRenderMode() {
        return prefs.getInt(KEY_RENDER_MODE, 0);
    }

    public void setRenderMode(int mode) {
        prefs.edit().putInt(KEY_RENDER_MODE, mode).apply();
    }

    public int getCacheModePref() {
        return prefs.getInt(KEY_CACHE_MODE_PREF, 0);
    }

    public void setCacheModePref(int mode) {
        prefs.edit().putInt(KEY_CACHE_MODE_PREF, mode).apply();
    }

    public int getLoadMode() {
        return prefs.getInt(KEY_LOAD_MODE, 0);
    }

    public void setLoadMode(int mode) {
        prefs.edit().putInt(KEY_LOAD_MODE, mode).apply();
    }

    public boolean isDynamicColorEnabled() {
        return prefs.getBoolean(KEY_DYNAMIC_COLOR_ENABLED, android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S);
    }

    public void setDynamicColorEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR_ENABLED, enabled).apply();
    }

    public String getRenderModeName() {
        switch (getRenderMode()) {
            case 1: return "兼容";
            case 2: return "增强";
            default: return "标准";
        }
    }

    public String getCacheModeName() {
        switch (getCacheModePref()) {
            case 1: return "仅缓存优先";
            case 2: return "不使用缓存";
            case 3: return "仅离线缓存";
            default: return "默认";
        }
    }

    public String getLoadModeName() {
        switch (getLoadMode()) {
            case 1: return "极速";
            case 2: return "省流";
            default: return "标准";
        }
    }

    public boolean isSupportZoomEnabled() {
        return prefs.getBoolean(KEY_SUPPORT_ZOOM, true);
    }

    public void setSupportZoomEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SUPPORT_ZOOM, enabled).apply();
    }

    public boolean isBuiltInZoomControlsEnabled() {
        return prefs.getBoolean(KEY_BUILTIN_ZOOM_CONTROLS, true);
    }

    public void setBuiltInZoomControlsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BUILTIN_ZOOM_CONTROLS, enabled).apply();
    }

    public boolean isDisplayZoomControlsEnabled() {
        return prefs.getBoolean(KEY_DISPLAY_ZOOM_CONTROLS, false);
    }

    public void setDisplayZoomControlsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DISPLAY_ZOOM_CONTROLS, enabled).apply();
    }

    public boolean isWideViewportEnabled() {
        return prefs.getBoolean(KEY_WIDE_VIEWPORT, true);
    }

    public void setWideViewportEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_WIDE_VIEWPORT, enabled).apply();
    }

    public boolean isLoadWithOverviewEnabled() {
        return prefs.getBoolean(KEY_LOAD_WITH_OVERVIEW, true);
    }

    public void setLoadWithOverviewEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOAD_WITH_OVERVIEW, enabled).apply();
    }

    public boolean isAllowFileAccessEnabled() {
        return prefs.getBoolean(KEY_ALLOW_FILE_ACCESS, true);
    }

    public void setAllowFileAccessEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALLOW_FILE_ACCESS, enabled).apply();
    }

    public boolean isAllowContentAccessEnabled() {
        return prefs.getBoolean(KEY_ALLOW_CONTENT_ACCESS, true);
    }

    public void setAllowContentAccessEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALLOW_CONTENT_ACCESS, enabled).apply();
    }

    public boolean isAcceptCookiesEnabled() {
        return prefs.getBoolean(KEY_ACCEPT_COOKIES, true);
    }

    public void setAcceptCookiesEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ACCEPT_COOKIES, enabled).apply();
    }

    public boolean isDomStorageEnabled() {
        return prefs.getBoolean(KEY_DOM_STORAGE_ENABLED, true);
    }

    public void setDomStorageEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DOM_STORAGE_ENABLED, enabled).apply();
    }

    public boolean isDatabaseEnabled() {
        return prefs.getBoolean(KEY_DATABASE_ENABLED, true);
    }

    public void setDatabaseEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DATABASE_ENABLED, enabled).apply();
    }

    public boolean isSaveFormDataEnabled() {
        return prefs.getBoolean(KEY_SAVE_FORM_DATA, true);
    }

    public void setSaveFormDataEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SAVE_FORM_DATA, enabled).apply();
    }

    public boolean isGeolocationEnabled() {
        return prefs.getBoolean(KEY_GEOLOCATION_ENABLED, true);
    }

    public void setGeolocationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_GEOLOCATION_ENABLED, enabled).apply();
    }

    public boolean isJavaScriptCanOpenWindowsAutomaticallyEnabled() {
        return prefs.getBoolean(KEY_JAVASCRIPT_CAN_OPEN_WINDOWS, false);
    }

    public void setJavaScriptCanOpenWindowsAutomaticallyEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_JAVASCRIPT_CAN_OPEN_WINDOWS, enabled).apply();
    }

    public boolean isSupportMultipleWindowsEnabled() {
        return prefs.getBoolean(KEY_SUPPORT_MULTIPLE_WINDOWS, false);
    }

    public void setSupportMultipleWindowsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SUPPORT_MULTIPLE_WINDOWS, enabled).apply();
    }

    public boolean isAcceptThirdPartyCookiesEnabled() {
        return prefs.getBoolean(KEY_ACCEPT_THIRD_PARTY_COOKIES, true);
    }

    public void setAcceptThirdPartyCookiesEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ACCEPT_THIRD_PARTY_COOKIES, enabled).apply();
    }

    public int getTextZoom() {
        return prefs.getInt(KEY_TEXT_ZOOM, 100);
    }

    public void setTextZoom(int zoom) {
        if (zoom < 50) zoom = 50;
        if (zoom > 200) zoom = 200;
        prefs.edit().putInt(KEY_TEXT_ZOOM, zoom).apply();
    }

    public String getDefaultTextEncoding() {
        String value = prefs.getString(KEY_DEFAULT_TEXT_ENCODING, "UTF-8");
        return value == null || value.trim().isEmpty() ? "UTF-8" : value;
    }

    public void setDefaultTextEncoding(String encoding) {
        String value = encoding == null ? "UTF-8" : encoding.trim();
        if (value.isEmpty()) value = "UTF-8";
        prefs.edit().putString(KEY_DEFAULT_TEXT_ENCODING, value).apply();
    }

    public boolean isDoNotTrackEnabled() {
        return prefs.getBoolean(KEY_DO_NOT_TRACK, false);
    }

    public void setDoNotTrackEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DO_NOT_TRACK, enabled).apply();
    }

    public boolean isSaveDataEnabled() {
        return prefs.getBoolean(KEY_SAVE_DATA, false);
    }

    public void setSaveDataEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SAVE_DATA, enabled).apply();
    }

    public boolean isSendXRequestedWithEnabled() {
        return prefs.getBoolean(KEY_SEND_X_REQUESTED_WITH, false);
    }

    public void setSendXRequestedWithEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SEND_X_REQUESTED_WITH, enabled).apply();
    }

    // 快捷链接数据类
    public static class QuickLink {
        public String name;
        public String url;
        public String icon;
        public String iconType;  // "letter" 或 "image"
        public String iconColor; // 仅 letter 模式有效，自定义颜色值如 "#FF0000"
        public String imagePath; // 仅 image 模式有效，图片文件路径

        public QuickLink(String name, String url, String icon) {
            this(name, url, icon, "letter", "", "");
        }

        public QuickLink(String name, String url, String icon, String iconType, String iconColor, String imagePath) {
            this.name = name;
            this.url = url;
            this.icon = icon;
            this.iconType = iconType != null ? iconType : "letter";
            this.iconColor = iconColor != null ? iconColor : "";
            this.imagePath = imagePath != null ? imagePath : "";
        }
    }

    // 内置搜索引擎数量（已加秘塔AI）
    public int getBuiltinSearchEngineCount() {
        return 6;
    }
}