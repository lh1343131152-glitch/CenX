package com.centigrade.browser.net;

import android.content.Context;
import android.text.TextUtils;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ResolvedConnectionFactory {

    private final Context context;

    public ResolvedConnectionFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    public HttpURLConnection open(String urlString) throws Exception {
        URL originalUrl = new URL(urlString);
        String host = originalUrl.getHost();
        String protocol = originalUrl.getProtocol();

        SmartDnsResolver resolver = SmartDnsResolver.getInstance(context);
        ResolvedAddress address = resolver.resolve(host);

        if (address == null || TextUtils.isEmpty(address.ip)) {
            HttpURLConnection conn = (HttpURLConnection) originalUrl.openConnection(Proxy.NO_PROXY);
            configure(conn, host);
            return conn;
        }

        if ("http".equalsIgnoreCase(protocol)) {
            URL directUrl = rebuildUrlWithIp(originalUrl, address.ip);
            HttpURLConnection conn = (HttpURLConnection) directUrl.openConnection(Proxy.NO_PROXY);
            configure(conn, host);
            conn.setRequestProperty("Host", host);
            return conn;
        }

        HttpsURLConnection conn = (HttpsURLConnection) originalUrl.openConnection(Proxy.NO_PROXY);
        configure(conn, host);
        return conn;
    }

    private void configure(HttpURLConnection conn, String host) {
        conn.setConnectTimeout(NetResolveConfig.CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(NetResolveConfig.READ_TIMEOUT_MS);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", NetResolveConfig.UA);
        if (!TextUtils.isEmpty(host)) {
            conn.setRequestProperty("Host", host);
        }
    }

    private URL rebuildUrlWithIp(URL originalUrl, String ip) throws Exception {
        int port = originalUrl.getPort();
        String file = originalUrl.getFile();
        String protocol = originalUrl.getProtocol();

        StringBuilder sb = new StringBuilder();
        sb.append(protocol).append("://").append(ip);
        if (port != -1) {
            sb.append(":").append(port);
        }
        if (file != null && !file.isEmpty()) {
            sb.append(file);
        }
        return new URL(sb.toString());
    }
}