package com.centigrade.browser.net;

public final class NetResolveConfig {

    private NetResolveConfig() {}

    public static final String HOSTS_PREF_NAME = "cenx_hosts_store";
    public static final String HOSTS_KEY_JSON = "hosts_json";

    public static final long DEFAULT_CACHE_TTL_MS = 10 * 60 * 1000L;
    public static final int CONNECT_TIMEOUT_MS = 15000;
    public static final int READ_TIMEOUT_MS = 20000;

    public static final String DOH_PRIMARY = "https://cloudflare-dns.com/dns-query";
    public static final String DOH_BACKUP = "https://1.1.1.1/dns-query";

    public static final String[] PURE_DNS_FALLBACKS = new String[] {
            "https://dns.google/resolve",
            "https://1.0.0.1/dns-query"
    };

    public static final String UA = "CenX/4.0 (Android)";
}