package com.centigrade.browser.ai;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiRemoteWebReader {

    public interface ReadCallback {
        void onSuccess(String title, String description, String content);
        void onError(String error);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public void read(String url, ReadCallback callback) {
        if (TextUtils.isEmpty(url)) {
            if (callback != null) callback.onError("链接为空");
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String finalUrl = url;
                for (int i = 0; i < 3; i++) {
                    URL u = new URL(finalUrl);
                    conn = (HttpURLConnection) u.openConnection();
                    conn.setInstanceFollowRedirects(false);
                    conn.setConnectTimeout(12000);
                    conn.setReadTimeout(12000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0 Mobile Safari/537.36");
                    conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
                    conn.connect();

                    int code = conn.getResponseCode();
                    if (code >= 300 && code < 400) {
                        String location = conn.getHeaderField("Location");
                        conn.disconnect();
                        conn = null;
                        if (TextUtils.isEmpty(location)) break;
                        finalUrl = location.startsWith("http") ? location : new URL(new URL(finalUrl), location).toString();
                        continue;
                    }

                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    String html = readAll(in, detectCharset(conn.getContentType()));
                    String title = extractTitle(html);
                    String desc = extractDescription(html);
                    String content = extractContent(html);
                    if (TextUtils.isEmpty(content)) {
                        throw new RuntimeException("网页正文为空");
                    }
                    String finalTitle = title;
                    String finalDesc = desc;
                    String finalContent = content;
                    MAIN.post(() -> {
                        if (callback != null) callback.onSuccess(finalTitle, finalDesc, finalContent);
                    });
                    return;
                }
                throw new RuntimeException("网页跳转过多或读取失败");
            } catch (Exception e) {
                String error = e.getClass().getSimpleName() + ": " + e.getMessage();
                MAIN.post(() -> {
                    if (callback != null) callback.onError(error);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private String readAll(InputStream in, String charset) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        if (TextUtils.isEmpty(charset)) charset = "UTF-8";
        return out.toString(charset);
    }

    private String detectCharset(String contentType) {
        if (TextUtils.isEmpty(contentType)) return "UTF-8";
        Matcher m = Pattern.compile("charset=([a-zA-Z0-9_\\-]+)").matcher(contentType);
        return m.find() ? m.group(1) : "UTF-8";
    }

    private String extractTitle(String html) {
        return htmlDecode(firstGroup(html, "(?is)<title[^>]*>(.*?)</title>"));
    }

    private String extractDescription(String html) {
        String v = firstGroup(html, "(?is)<meta[^>]+name=[\"']description[\"'][^>]+content=[\"'](.*?)[\"'][^>]*>");
        if (TextUtils.isEmpty(v)) {
            v = firstGroup(html, "(?is)<meta[^>]+property=[\"']og:description[\"'][^>]+content=[\"'](.*?)[\"'][^>]*>");
        }
        return htmlDecode(v);
    }

    private String extractContent(String html) {
        if (TextUtils.isEmpty(html)) return "";
        String s = html;
        s = s.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
        s = s.replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        s = s.replaceAll("(?is)<noscript[^>]*>.*?</noscript>", " ");
        s = s.replaceAll("(?is)<header[^>]*>.*?</header>", " ");
        s = s.replaceAll("(?is)<footer[^>]*>.*?</footer>", " ");
        s = s.replaceAll("(?is)<nav[^>]*>.*?</nav>", " ");
        s = s.replaceAll("(?is)<aside[^>]*>.*?</aside>", " ");
        s = s.replaceAll("(?is)<form[^>]*>.*?</form>", " ");

        String best = pickBestBlock(s);
        if (TextUtils.isEmpty(best)) best = s;

        best = best.replaceAll("(?is)<br\\s*/?>", "\n");
        best = best.replaceAll("(?is)</p>|</div>|</section>|</article>|</li>|</h1>|</h2>|</h3>", "\n");
        best = best.replaceAll("(?is)<[^>]+>", " ");
        best = htmlDecode(best);
        best = best.replace('\u00A0', ' ');
        best = best.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        best = best.replaceAll("\\n{3,}", "\n\n");
        best = best.replaceAll("(?m)^\\s+", "");
        best = best.replaceAll("(?m)\\s+$", "");
        best = best.trim();

        if (best.length() > 12000) {
            best = best.substring(0, 12000);
        }
        return best;
    }

    private String pickBestBlock(String html) {
        String[] patterns = new String[] {
                "(?is)<article[^>]*>(.*?)</article>",
                "(?is)<main[^>]*>(.*?)</main>",
                "(?is)<div[^>]+class=[\"'][^\"']*(content|article|post|entry|detail|reader|chapter|text|ztext|RichContent)[^\"']*[\"'][^>]*>(.*?)</div>",
                "(?is)<section[^>]+class=[\"'][^\"']*(content|article|post|entry|detail|reader|chapter|text)[^\"']*[\"'][^>]*>(.*?)</section>"
        };

        String best = "";
        int bestScore = 0;
        for (String p : patterns) {
            Matcher m = Pattern.compile(p).matcher(html);
            while (m.find()) {
                String block = m.group(m.groupCount());
                if (TextUtils.isEmpty(block)) continue;
                String plain = block.replaceAll("(?is)<[^>]+>", " ");
                plain = htmlDecode(plain).replaceAll("\\s+", " ").trim();
                int score = plain.length();
                if (score > bestScore) {
                    bestScore = score;
                    best = block;
                }
            }
        }
        return best;
    }

    private String firstGroup(String text, String regex) {
        if (TextUtils.isEmpty(text)) return "";
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private String htmlDecode(String s) {
        if (s == null) return "";
        String nbsp = new String(new char[]{'&','n','b','s','p',';'});
        String amp = new String(new char[]{'&','a','m','p',';'});
        String lt = new String(new char[]{'&','l','t',';'});
        String gt = new String(new char[]{'&','g','t',';'});
        String quot = new String(new char[]{'&','q','u','o','t',';'});
        String apos = new String(new char[]{'&','#','3','9',';'});
        String ldquo = new String(new char[]{'&','l','d','q','u','o',';'});
        String rdquo = new String(new char[]{'&','r','d','q','u','o',';'});
        String lsquo = new String(new char[]{'&','l','s','q','u','o',';'});
        String rsquo = new String(new char[]{'&','r','s','q','u','o',';'});

        return s.replace(nbsp, " ")
                .replace(amp, String.valueOf((char) 38))
                .replace(lt, String.valueOf((char) 60))
                .replace(gt, String.valueOf((char) 62))
                .replace(quot, String.valueOf((char) 34))
                .replace(apos, String.valueOf((char) 39))
                .replace(ldquo, String.valueOf((char) 8220))
                .replace(rdquo, String.valueOf((char) 8221))
                .replace(lsquo, String.valueOf((char) 8216))
                .replace(rsquo, String.valueOf((char) 8217));
    }
}