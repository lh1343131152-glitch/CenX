package com.centigrade.browser.net;

public class ResolvedAddress {
    public final String host;
    public final String ip;
    public final long resolvedAt;
    public final long ttlMs;
    public final String source;

    public ResolvedAddress(String host, String ip, long resolvedAt, long ttlMs, String source) {
        this.host = host;
        this.ip = ip;
        this.resolvedAt = resolvedAt;
        this.ttlMs = ttlMs;
        this.source = source;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - resolvedAt > ttlMs;
    }
}