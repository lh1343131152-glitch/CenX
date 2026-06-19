package com.centigrade.browser;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 站点鉴权配置管理器
 * 用 JSON 持久化存储所有站点的鉴权配置到 SharedPreferences
 */
public class SiteAuthManager {

    private static final String PREF_NAME = "site_auth_prefs";
    private static final String KEY_SITE_CONFIGS = "site_configs";

    private final SharedPreferences prefs;

    public SiteAuthManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** 获取所有站点配置列表 */
    public List<SiteAuthConfig> getAllConfigs() {
        List<SiteAuthConfig> list = new ArrayList<>();
        String json = prefs.getString(KEY_SITE_CONFIGS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(SiteAuthConfig.fromJson(arr.optJSONObject(i)));
            }
        } catch (Exception ignored) {}
        return list;
    }

    /** 保存整个站点配置列表 */
    public void saveAllConfigs(List<SiteAuthConfig> list) {
        JSONArray arr = new JSONArray();
        if (list != null) {
            for (SiteAuthConfig config : list) {
                if (config != null) {
                    arr.put(config.toJson());
                }
            }
        }
        prefs.edit().putString(KEY_SITE_CONFIGS, arr.toString()).apply();
    }

    /** 根据 host 精确查找配置 */
    public SiteAuthConfig findConfigByHost(String host) {
        if (TextUtils.isEmpty(host)) return null;
        List<SiteAuthConfig> list = getAllConfigs();
        for (SiteAuthConfig config : list) {
            if (config != null && host.equalsIgnoreCase(config.host)) {
                return config;
            }
        }
        return null;
    }

    /** 根据完整 URL 查找配置 */
    public SiteAuthConfig findConfigByUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            return findConfigByHost(host);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 新增或更新一个站点配置（按 host 去重） */
    public void saveOrUpdate(SiteAuthConfig target) {
        if (target == null || TextUtils.isEmpty(target.host)) return;
        List<SiteAuthConfig> list = getAllConfigs();
        boolean updated = false;
        for (int i = 0; i < list.size(); i++) {
            SiteAuthConfig old = list.get(i);
            if (old != null && target.host.equalsIgnoreCase(old.host)) {
                list.set(i, target);
                updated = true;
                break;
            }
        }
        if (!updated) {
            list.add(target);
        }
        saveAllConfigs(list);
    }

    /** 删除指定 host 的配置 */
    public void deleteByHost(String host) {
        if (TextUtils.isEmpty(host)) return;
        List<SiteAuthConfig> list = getAllConfigs();
        List<SiteAuthConfig> result = new ArrayList<>();
        for (SiteAuthConfig config : list) {
            if (config == null || host.equalsIgnoreCase(config.host)) continue;
            result.add(config);
        }
        saveAllConfigs(result);
    }
}
