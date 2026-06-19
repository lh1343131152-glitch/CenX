package com.centigrade.browser.ai;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.centigrade.browser.GeckoEngine;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class AiAddressSmart {

    public interface SmartCallback {
        void onLoadUrl(String url);
        void onAiCard(String question, String answer);
        void onError(String error);
    }

    private final AiSelfNet net = new AiSelfNet();
    private final AiSimpleJson json = new AiSimpleJson();
    private final AiCoreLogic logic = new AiCoreLogic();
    private final Handler main = new Handler(Looper.getMainLooper());

    public boolean isProbablyUrl(String input) {
        if (TextUtils.isEmpty(input)) return false;
        String text = input.trim().toLowerCase();
        if (text.startsWith("http://") || text.startsWith("https://")) return true;
        if (text.startsWith("file://") || text.startsWith("about:")) return true;
        if (text.startsWith("javascript:")) return true;
        if (text.contains(" ") || text.contains("？") || text.contains("?")) return false;
        return text.contains(".") || text.contains("/") || text.matches("^[a-z]+://.*");
    }

    public void handle(String input,
                       String searchUrlTemplate,
                       String aiUrl,
                       String aiKey,
                       String aiModel,
                       SmartCallback callback) {
        if (TextUtils.isEmpty(input)) {
            if (callback != null) callback.onError("输入为空");
            return;
        }

        String text = input.trim();
        if (isProbablyUrl(text)) {
            String finalUrl = text.startsWith("http://") || text.startsWith("https://") || text.startsWith("about:")
                    ? text : "https://" + text;
            if (callback != null) callback.onLoadUrl(finalUrl);
            return;
        }

        if (TextUtils.isEmpty(aiUrl) || TextUtils.isEmpty(aiModel)) {
            try {
                String search = String.format(searchUrlTemplate, URLEncoder.encode(text, "UTF-8"));
                if (callback != null) callback.onLoadUrl(search);
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
            return;
        }

        String prompt = "请直接回答以下用户问题，回答简洁清楚，不要输出无关前缀。\n\n问题：\n" + text;
        String body = logic.buildOpenAiRequest(aiModel, prompt);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        if (!TextUtils.isEmpty(aiKey)) {
            headers.put("Authorization", "Bearer " + aiKey);
        }

        net.post(aiUrl, headers, body, new AiSelfNet.NetCallback() {
            @Override
            public void onSuccess(int code, String body) {
                String answer = json.parseAiContent(body);
                if (TextUtils.isEmpty(answer)) answer = body;
                String finalAnswer = answer;
                main.post(() -> {
                    if (callback != null) callback.onAiCard(text, finalAnswer);
                });
            }

            @Override
            public void onError(String error) {
                main.post(() -> {
                    if (callback != null) callback.onError(error);
                });
            }
        });
    }

    public void attachAnswerCard(GeckoEngine engine, String question, String answer) {
        if (engine == null) return;
        String q = escapeHtml(question);
        String a = renderMarkdown(answer);
        String html = "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>AI回答</title>"
                + "<style>"
                + "body{margin:0;padding:18px;background:#f6f7fb;font-family:-apple-system,BlinkMacSystemFont,sans-serif;color:#111;}"
                + ".wrap{max-width:900px;margin:0 auto;}"
                + ".card{background:#fff;border-radius:18px;padding:18px 16px;box-shadow:0 8px 28px rgba(0,0,0,.08);}"
                + ".tag{display:inline-block;background:#111;color:#fff;border-radius:999px;padding:6px 10px;font-size:12px;margin-bottom:12px;}"
                + ".q{font-size:18px;font-weight:700;margin-bottom:12px;line-height:1.5;}"
                + ".a{font-size:15px;line-height:1.85;word-break:break-word;}"
                + ".bar{display:flex;gap:10px;flex-wrap:wrap;margin-top:16px;}"
                + ".btn{display:inline-block;padding:10px 14px;border-radius:12px;background:#111;color:#fff;text-decoration:none;font-size:14px;}"
                + "</style></head><body><div class='wrap'><div class='card'><div class='tag'>CenX AI</div><div class='q'>"
                + q + "</div><div class='a'>" + a + "</div>"
                + "<div class='bar'><a class='btn' href='about:blank'>继续浏览</a></div>"
                + "</div></div></body></html>";
        engine.loadDataWithBaseURL("https://cenx.ai/", html, "text/html", "UTF-8", null);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        String amp = new String(new char[]{'&','a','m','p',';'});
        String lt = new String(new char[]{'&','l','t',';'});
        String gt = new String(new char[]{'&','g','t',';'});
        String quot = new String(new char[]{'&','q','u','o','t',';'});
        String apos = new String(new char[]{'&','#','3','9',';'});
        String value = text;
        value = value.replace("&", amp);
        value = value.replace("<", lt);
        value = value.replace(">", gt);
        value = value.replace(String.valueOf((char) 34), quot);
        value = value.replace("'", apos);
        return value;
    }

    private String renderMarkdown(String text) {
        if (text == null || text.isEmpty()) return "";
        String html = escapeHtml(text);
        html = html.replaceAll("(?s)```(.*?)```", "<pre><code>$1</code></pre>");
        html = html.replaceAll("(?m)^###\\s+(.*)$", "<h3>$1</h3>");
        html = html.replaceAll("(?m)^##\\s+(.*)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^#\\s+(.*)$", "<h1>$1</h1>");
        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("__(.+?)__", "<strong>$1</strong>");
        html = html.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "<em>$1</em>");
        html = html.replaceAll("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)", "<em>$1</em>");
        html = html.replaceAll("`([^`]+)`", "<code>$1</code>");
        html = html.replaceAll("\\[(.+?)\\]\\((https?://[^\\s)]+)\\)", "<a href='$2'>$1</a>");
        html = html.replaceAll("(?m)^>\\s?(.*)$", "<blockquote>$1</blockquote>");
        html = html.replaceAll("(?m)^-\\s+(.*)$", "• $1");
        html = html.replaceAll("(?m)^\\*\\s+(.*)$", "• $1");
        html = html.replace("\n", "<br>");
        return html;
    }

}