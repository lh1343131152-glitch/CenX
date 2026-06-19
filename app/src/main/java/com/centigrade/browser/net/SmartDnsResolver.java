package com.centigrade.browser.net;

import android.content.Context;
import android.text.TextUtils;

public class SmartDnsResolver {

    private static volatile SmartDnsResolver instance;

    private final HostsManager hostsManager;
    private final ResolveCache cache;
    private final DohResolver dohResolver;

    private SmartDnsResolver(Context context) {
        this.hostsManager = new HostsManager(context);
        this.cache = new ResolveCache();
        this.dohResolver = new DohResolver();
    }

    public static SmartDnsResolver getInstance(Context context) {
        if (instance == null) {
            synchronized (SmartDnsResolver.class) {
                if (instance == null) {
                    instance = new SmartDnsResolver(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public ResolvedAddress resolve(String host) {
        if (TextUtils.isEmpty(host)) return null;
        host = normalize(host);

        String hostIp = hostsManager.getIp(host);
        if (!TextUtils.isEmpty(hostIp)) {
            ResolvedAddress address = new ResolvedAddress(
                    host,
                    hostIp,
                    System.currentTimeMillis(),
                    Long.MAX_VALUE / 4,
                    "hosts"
            );
            cache.put(address);
            return address;
        }

        ResolvedAddress cached = cache.get(host);
        if (cached != null) return cached;

        ResolvedAddress resolved = dohResolver.resolve(host);
        if (resolved != null) {
            cache.put(resolved);
            return resolved;
        }
        return null;
    }

    public HostsManager hosts() {
        return hostsManager;
    }

    public ResolveCache cache() {
        return cache;
    }

    public void clearCache() {
        cache.clear();
    }

    private String normalize(String host) {
        return host == null ? "" : host.trim().toLowerCase();
    }
}