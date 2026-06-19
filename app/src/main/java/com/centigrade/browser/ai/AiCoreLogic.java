package com.centigrade.browser.ai;

import android.text.TextUtils;

public class AiCoreLogic {

    public static final int MODE_SUMMARY = 1;
    public static final int MODE_TRANSLATE = 2;
    public static final int MODE_EXPLAIN = 3;
    public static final int MODE_SHORTEN = 4;
    public static final int MODE_EXPAND = 5;
    public static final int MODE_WEB_QA = 6;
    public static final int MODE_KEYWORDS = 7;

    public String buildPrompt(int mode, String text, String pageTitle, String pageDesc, String pageUrl, String userQuestion) {
        String source = safe(text);
        String title = safe(pageTitle);
        String desc = safe(pageDesc);
        String url = safe(pageUrl);
        String question = safe(userQuestion);
        String pageInfo = "标题：" + title + "\n描述：" + desc + "\n链接：" + url;

        switch (mode) {
            case MODE_SUMMARY:
                return "你是浏览器内置AI助手，请用简洁中文总结以下网页或文本内容，保留重点信息，不要废话。若你支持联网，请结合链接理解页面；若不支持联网，则基于提供的页面信息与正文内容回答。\n\n"
                        + pageInfo + "\n\n页面内容：\n" + source;
            case MODE_TRANSLATE:
                String targetLanguage = TextUtils.isEmpty(question) ? "简体中文" : question;
                return "你是浏览器内置AI助手，请将以下网页内容准确翻译成" + targetLanguage + "，保持原意，不要添加无关说明。若原文已经是目标语言，请直接给出更自然的目标语言表达。若你支持联网，可结合链接理解上下文；否则基于给出的内容翻译。\n\n"
                        + pageInfo + "\n\n页面内容：\n" + source;
            case MODE_EXPLAIN:
                return "你是浏览器内置AI助手，请用通俗易懂的中文释义以下网页内容，适合普通用户理解。若你支持联网，可结合链接理解页面；否则基于给出的内容解释。\n\n"
                        + pageInfo + "\n\n页面内容：\n" + source;
            case MODE_SHORTEN:
                return "你是浏览器内置AI助手，请将以下网页内容压缩精简成更短版本，保留核心信息。\n\n"
                        + pageInfo + "\n\n页面内容：\n" + source;
            case MODE_EXPAND:
                return "你是浏览器内置AI助手，请在不偏离原意的前提下扩写以下网页内容，让表达更完整清晰。\n\n"
                        + pageInfo + "\n\n页面内容：\n" + source;
            case MODE_WEB_QA:
                return "你是浏览器内置AI助手，请基于页面信息回答用户问题。如果页面信息不足，就明确说信息不足，不要编造。若你支持联网，可结合链接查看页面。\n\n"
                        + pageInfo + "\n\n页面内容：\n" + source + "\n\n用户问题：\n" + question;
            case MODE_KEYWORDS:
                return "你是浏览器内置AI助手，请从以下网页内容中提取关键词，直接输出关键词列表，简洁即可。\n\n"
                        + pageInfo + "\n\n页面内容：\n" + source;
            default:
                return source;
        }
    }

    public String buildOpenAiRequest(String model, String prompt) {
        String safeModel = jsonEscape(emptyToDefault(model, "gpt-4o-mini"));
        String safePrompt = jsonEscape(emptyToDefault(prompt, ""));
        return "{"
                + "\"model\":\"" + safeModel + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + safePrompt + "\"}],"
                + "\"temperature\":0.7"
                + "}";
    }

    public String modeName(int mode) {
        switch (mode) {
            case MODE_SUMMARY: return "AI总结";
            case MODE_TRANSLATE: return "AI翻译";
            case MODE_EXPLAIN: return "AI释义";
            case MODE_SHORTEN: return "AI精简";
            case MODE_EXPAND: return "AI扩写";
            case MODE_WEB_QA: return "网页问答";
            case MODE_KEYWORDS: return "关键词提取";
            default: return "AI处理";
        }
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private String emptyToDefault(String text, String fallback) {
        return TextUtils.isEmpty(text) ? fallback : text;
    }

    private String jsonEscape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}