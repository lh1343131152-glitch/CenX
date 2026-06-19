package com.centigrade.browser.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HostsManager {

    private final SharedPreferences preferences;
    private final Map<String, String> hostsMap = Collections.synchronizedMap(new LinkedHashMap<>());

    public HostsManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(NetResolveConfig.HOSTS_PREF_NAME, Context.MODE_PRIVATE);
        load();
    }

    public synchronized void addHost(String host, String ip) {
        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(ip)) return;
        hostsMap.put(normalize(host), ip.trim());
        save();
    }

    public synchronized void removeHost(String host) {
        hostsMap.remove(normalize(host));
        save();
    }

    public synchronized void clear() {
        hostsMap.clear();
        save();
    }

    public synchronized String getIp(String host) {
        return hostsMap.get(normalize(host));
    }

    public synchronized boolean contains(String host) {
        return hostsMap.containsKey(normalize(host));
    }

    public synchronized Map<String, String> getAll() {
        return new LinkedHashMap<>(hostsMap);
    }

    public synchronized void importFromText(String content) {
        if (TextUtils.isEmpty(content)) return;
        String[] lines = content.split("\\r?\\n");
        for (String rawLine : lines) {
            if (rawLine == null) continue;
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                String ip = parts[0].trim();
                for (int i = 1; i < parts.length; i++) {
                    String host = parts[i].trim();
                    if (!host.isEmpty() && !host.startsWith("#")) {
                        hostsMap.put(normalize(host), ip);
                    }
                }
            }
        }
        save();
    }

    public synchronized List<String> exportLines() {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : hostsMap.entrySet()) {
            list.add(entry.getValue() + " " + entry.getKey());
        }
        return list;
    }

    private void load() {
        hostsMap.clear();
        String json = preferences.getString(NetResolveConfig.HOSTS_KEY_JSON, "");
        if (TextUtils.isEmpty(json)) return;
        try {
            JSONObject object = new JSONObject(json);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                hostsMap.put(normalize(key), object.optString(key, ""));
            }
        } catch (Exception ignored) {
        }
    }

    private void save() {
        try {
            JSONObject object = new JSONObject();
            for (Map.Entry<String, String> entry : hostsMap.entrySet()) {
                object.put(entry.getKey(), entry.getValue());
            }
            preferences.edit().putString(NetResolveConfig.HOSTS_KEY_JSON, object.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private String normalize(String host) {
        return host == null ? "" : host.trim().toLowerCase();
    }
}