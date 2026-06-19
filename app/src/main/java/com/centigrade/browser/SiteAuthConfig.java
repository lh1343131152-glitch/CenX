package com.centigrade.browser;

import org.json.JSONObject;

/**
 * 站点级鉴权配置模型
 * 每个域名可以独立保存 Cookie、Authorization、Referer、自定义 Header 等
 */
public class SiteAuthConfig {
    /** 站点 host，例如 "example.com" */
    public String host = "";
    /** 站点级自定义 Cookie 字符串 */
    public String cookie = "";
    /** Authorization 请求头，例如 "Bearer xxx" */
    public String authorization = "";
    /** Referer 请求头 */
    public String referer = "";
    /** Origin 请求头 */
    public String origin = "";
    /** 覆盖默认 UA（为空则使用浏览器全局 UA） */
    public String userAgentOverride = "";
    /** 额外的自定义请求头 JSON，例如 {"X-Token":"abc","X-Device":"mobile"} */
    public String extraHeadersJson = "";
    /** 是否启用鉴权注入（默认 true） */
    public boolean enableAuthInjection = true;
    /** 是否启用 401 自动重试（默认 true） */
    public boolean enable401Retry = true;
    /** 是否强制使用站点 Cookie 覆盖系统 Cookie（默认 false） */
    public boolean forceCookieHeader = false;

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("host", host);
            obj.put("cookie", cookie);
            obj.put("authorization", authorization);
            obj.put("referer", referer);
            obj.put("origin", origin);
            obj.put("userAgentOverride", userAgentOverride);
            obj.put("extraHeadersJson", extraHeadersJson);
            obj.put("enableAuthInjection", enableAuthInjection);
            obj.put("enable401Retry", enable401Retry);
            obj.put("forceCookieHeader", forceCookieHeader);
        } catch (Exception ignored) {}
        return obj;
    }

    public static SiteAuthConfig fromJson(JSONObject obj) {
        SiteAuthConfig config = new SiteAuthConfig();
        if (obj == null) return config;
        config.host = obj.optString("host", "");
        config.cookie = obj.optString("cookie", "");
        config.authorization = obj.optString("authorization", "");
        config.referer = obj.optString("referer", "");
        config.origin = obj.optString("origin", "");
        config.userAgentOverride = obj.optString("userAgentOverride", "");
        config.extraHeadersJson = obj.optString("extraHeadersJson", "");
        config.enableAuthInjection = obj.optBoolean("enableAuthInjection", true);
        config.enable401Retry = obj.optBoolean("enable401Retry", true);
        config.forceCookieHeader = obj.optBoolean("forceCookieHeader", false);
        return config;
    }
}
