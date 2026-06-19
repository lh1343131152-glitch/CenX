package com.centigrade.browser.net;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResolveCache {
    private final Map<String, ResolvedAddress> cache = new ConcurrentHashMap<>();

    public ResolvedAddress get(String host) {
        ResolvedAddress address = cache.get(normalize(host));
        if (address == null) return null;
        if (address.isExpired()) {
            cache.remove(normalize(host));
            return null;
        }
        return address;
    }

    public void put(ResolvedAddress address) {
        if (address == null || address.host == null || address.ip == null) return;
        cache.put(normalize(address.host), address);
    }

    public void remove(String host) {
        cache.remove(normalize(host));
    }

    public void clear() {
        cache.clear();
    }

    public boolean contains(String host) {
        return get(host) != null;
    }

    private String normalize(String host) {
        return host == null ? "" : host.trim().toLowerCase();
    }
}