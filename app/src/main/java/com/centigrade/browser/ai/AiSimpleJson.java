package com.centigrade.browser.ai;

import android.text.TextUtils;

public class AiSimpleJson {

    public String getString(String json, String key) {
        if (TextUtils.isEmpty(json) || TextUtils.isEmpty(key)) return "";
        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);
        if (keyIndex == -1) return "";
        int colon = json.indexOf(':', keyIndex + token.length());
        if (colon == -1) return "";
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return "";

        if (json.charAt(start) == '\"') {
            start++;
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaped) {
                    if (c == 'n') sb.append('\n');
                    else if (c == 't') sb.append('\t');
                    else if (c == 'r') {}
                    else sb.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '\"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        int end = start;
        while (end < json.length() && ",}]".indexOf(json.charAt(end)) == -1) end++;
        return json.substring(start, end).trim();
    }

    public String getNestedString(String json, String... keys) {
        if (keys == null || keys.length == 0) return "";
        String current = json;
        for (int i = 0; i < keys.length - 1; i++) {
            current = getObject(current, keys[i]);
            if (TextUtils.isEmpty(current)) return "";
        }
        return getString(current, keys[keys.length - 1]);
    }

    public String getObject(String json, String key) {
        if (TextUtils.isEmpty(json) || TextUtils.isEmpty(key)) return "";
        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);
        if (keyIndex == -1) return "";
        int colon = json.indexOf(':', keyIndex + token.length());
        if (colon == -1) return "";
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length() || json.charAt(start) != '{') return "";
        return extractBlock(json, start, '{', '}');
    }

    public String getArray(String json, String key) {
        if (TextUtils.isEmpty(json) || TextUtils.isEmpty(key)) return "";
        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);
        if (keyIndex == -1) return "";
        int colon = json.indexOf(':', keyIndex + token.length());
        if (colon == -1) return "";
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length() || json.charAt(start) != '[') return "";
        return extractBlock(json, start, '[', ']');
    }

    public String getArrayObject(String arrayJson, int index) {
        if (TextUtils.isEmpty(arrayJson) || index < 0) return "";
        int depth = 0;
        int objIndex = -1;
        boolean inString = false;
        boolean escaped = false;
        int start = -1;

        for (int i = 0; i < arrayJson.length(); i++) {
            char c = arrayJson.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '\"') inString = false;
                continue;
            }
            if (c == '\"') {
                inString = true;
                continue;
            }
            if (c == '{') {
                if (depth == 0) {
                    objIndex++;
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objIndex == index && start != -1) {
                    return arrayJson.substring(start, i + 1);
                }
            }
        }
        return "";
    }

    public String parseAiContent(String json) {
        String choices = getArray(json, "choices");
        String first = getArrayObject(choices, 0);
        String message = getObject(first, "message");
        String content = getString(message, "content");
        if (!TextUtils.isEmpty(content)) return content;
        return getString(json, "content");
    }

    private String extractBlock(String text, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '\"') inString = false;
                continue;
            }
            if (c == '\"') {
                inString = true;
                continue;
            }
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return "";
    }
}