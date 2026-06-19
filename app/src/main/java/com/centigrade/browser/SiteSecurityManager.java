package com.centigrade.browser;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

/**
 * 站点安全与认证记忆管理。
 * 用于记录：
 * 1. 某个域名的 SSL 证书错误处理偏好：继续/取消/未设置
 * 2. 某个域名 + realm 的 HTTP Basic Auth 用户名密码
 */
public class SiteSecurityManager {

    private static final String PREF_NAME = "site_security_prefs";
    private static final String SSL_PREFIX = "ssl_behavior_";
    private static final String AUTH_USER_PREFIX = "auth_user_";
    private static final String AUTH_PASS_PREFIX = "auth_pass_";

    public static final int SSL_BEHAVIOR_NONE = 0;
    public static final int SSL_BEHAVIOR_PROCEED = 1;
    public static final int SSL_BEHAVIOR_CANCEL = 2;

    private final SharedPreferences prefs;

    public SiteSecurityManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getSslBehaviorForUrl(String url) {
        String host = hostFromUrl(url);
        if (TextUtils.isEmpty(host)) return SSL_BEHAVIOR_NONE;
        return prefs.getInt(SSL_PREFIX + host, SSL_BEHAVIOR_NONE);
    }

    public void setSslBehaviorForUrl(String url, int behavior) {
        String host = hostFromUrl(url);
        if (TextUtils.isEmpty(host)) return;
        prefs.edit().putInt(SSL_PREFIX + host, behavior).apply();
    }

    public void clearSslBehaviorForUrl(String url) {
        String host = hostFromUrl(url);
        if (TextUtils.isEmpty(host)) return;
        prefs.edit().remove(SSL_PREFIX + host).apply();
    }

    public AuthCredential getHttpAuthCredential(String host, String realm) {
        String key = authKey(host, realm);
        String user = prefs.getString(AUTH_USER_PREFIX + key, "");
        String pass = prefs.getString(AUTH_PASS_PREFIX + key, "");
        if (TextUtils.isEmpty(user)) return null;
        return new AuthCredential(user, pass == null ? "" : pass);
    }

    public void saveHttpAuthCredential(String host, String realm, String user, String pass) {
        String key = authKey(host, realm);
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(user)) return;
        prefs.edit()
                .putString(AUTH_USER_PREFIX + key, user)
                .putString(AUTH_PASS_PREFIX + key, pass == null ? "" : pass)
                .apply();
    }

    public void clearHttpAuthCredential(String host, String realm) {
        String key = authKey(host, realm);
        prefs.edit()
                .remove(AUTH_USER_PREFIX + key)
                .remove(AUTH_PASS_PREFIX + key)
                .apply();
    }

    private String authKey(String host, String realm) {
        host = host == null ? "" : host.trim().toLowerCase();
        realm = realm == null ? "" : realm.trim();
        if (TextUtils.isEmpty(host)) return "";
        return host + "|" + realm;
    }

    private String hostFromUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            return host == null ? "" : host.trim().toLowerCase();
        } catch (Exception ignored) {
            return "";
        }
    }

    public static class AuthCredential {
        public final String username;
        public final String password;

        public AuthCredential(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
