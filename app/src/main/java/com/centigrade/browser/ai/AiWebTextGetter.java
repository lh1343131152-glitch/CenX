package com.centigrade.browser.ai;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.ValueCallback;
import android.webkit.WebView;

public class AiWebTextGetter {

    public interface TextResultCallback {
        void onResult(PageText pageText);
    }

    public static class PageText {
        public String selectedText;
        public String pageText;
        public String title;
        public String description;
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public void getAll(WebView webView, TextResultCallback callback) {
        if (webView == null) {
            if (callback != null) callback.onResult(new PageText());
            return;
        }
        runJs(webView,
                "(function(){" +
                        "  try {" +
                        "    function textOf(el){ return el ? (el.innerText || el.textContent || '').replace(/\\s+/g,' ').trim() : ''; }" +
                        "    function cleanedText(el){" +
                        "      var t = textOf(el);" +
                        "      t = t.replace(/(上一篇|下一篇|相关阅读|相关推荐|猜你喜欢|评论区|热门评论|返回目录|加入书签|打赏|举报|纠错|分享本文|版权声明|打开百度APP看更多高清图片|点击展开全文|下载知乎App|赞同\\s*\\d+|收藏\\s*\\d+|分享|作者赞过|收起评论).*/g, '').trim();" +
                        "      return t;" +
                        "    }" +
                        "    function clsOf(el){ return ((el && el.className) || '') + ' ' + ((el && el.id) || ''); }" +
                        "    function isBad(el){" +
                        "      var cls = clsOf(el).toLowerCase();" +
                        "      return /header|footer|nav|menu|comment|recommend|related|toolbar|button|aside|sidebar|catalog|popup|modal|banner|ad-?|advert|toolbox|author-card|hot-list|questionheader-footer|comments-container/.test(cls);" +
                        "    }" +
                        "    function score(el){" +
                        "      if(!el || isBad(el)) return 0;" +
                        "      var t = cleanedText(el);" +
                        "      if(!t) return 0;" +
                        "      var pCount = el.querySelectorAll ? el.querySelectorAll('p').length : 0;" +
                        "      var brCount = el.querySelectorAll ? el.querySelectorAll('br').length : 0;" +
                        "      var cls = clsOf(el).toLowerCase();" +
                        "      var bonus = /article|content|detail|post|entry|read|reader|chapter|story|answer|rich_media|main/.test(cls) ? 180 : 0;" +
                        "      return t.length + pCount * 60 + brCount * 8 + bonus;" +
                        "    }" +
                        "    var s='';" +
                        "    try{s=(window.getSelection?window.getSelection().toString():'')||'';}catch(e){}" +
                        "    var t='';" +
                        "    try{t=(document.title||'');}catch(e){}" +
                        "    var d='';" +
                        "    try{" +
                        "      var m=document.querySelector('meta[name=\"description\"],meta[property=\"og:description\"]');" +
                        "      d=m?((m.content||'')+''):'';" +
                        "    }catch(e){}" +
                        "    var selectors = [" +
                        "      '.AnswerItem .RichContent','.RichContent-inner','.RichContent','.ztext','.Post-RichTextContainer'," +
                        "      'article','main','[role=\"main\"]','.article','.post','.content','.entry-content','.post-content','.article-content','.markdown-body','.rich_media_content','.rich_media_area_primary','.news-content','.detail-content','.story','.story-content','.chapter-content','.read-content','.reader-content','.content__article','.article__content','.text','.mainContent','.content-detail','.news-main','.index-module_articleWrap','.index-module_article','.index-module_content','.index-module_detail','.page-content','.readerContent','.app_content','.chapterContent','.book-content','.read-page','.article-page','.text-content','.chapter-body','#content','#article','#article_content','#readtxt','#content1','#js_content'" +
                        "    ];" +
                        "    var candidates = [];" +
                        "    for(var si=0;si<selectors.length;si++){" +
                        "      var found = document.querySelectorAll(selectors[si]);" +
                        "      for(var j=0;j<found.length;j++) candidates.push(found[j]);" +
                        "    }" +
                        "    if(candidates.length === 0){" +
                        "      candidates = Array.prototype.slice.call(document.querySelectorAll('div, section, article, main, td'));" +
                        "      candidates = candidates.filter(function(el){" +
                        "        var txt = cleanedText(el);" +
                        "        return txt.length > 80 && !isBad(el);" +
                        "      });" +
                        "    }" +
                        "    var best = null, bestScore = 0;" +
                        "    for(var i=0;i<candidates.length;i++){" +
                        "      var sc = score(candidates[i]);" +
                        "      if(sc > bestScore){ bestScore = sc; best = candidates[i]; }" +
                        "    }" +
                        "    var body='';" +
                        "    try{" +
                        "      body = cleanedText(best);" +
                        "      if((!body || body.length < 80) && document.body){" +
                        "        var bodyText = cleanedText(document.body);" +
                        "        if(bodyText.length > body.length) body = bodyText;" +
                        "      }" +
                        "    }catch(e){}" +
                        "    return JSON.stringify({selectedText:s,pageText:body,title:t,description:d});" +
                        "  } catch(e) {" +
                        "    return JSON.stringify({selectedText:'',pageText:'',title:(document.title||''),description:''});" +
                        "  }" +
                        "})();",
                value -> {
                    PageText result = parsePageText(value);
                    if (callback != null) callback.onResult(result);
                });
    }

    public void getSelectedText(WebView webView, ValueCallback<String> callback) {
        runJs(webView,
                "(function(){try{return (window.getSelection?window.getSelection().toString():'')||'';}catch(e){return '';}})();",
                value -> {
                    if (callback != null) callback.onReceiveValue(cleanJsString(value));
                });
    }

    public void getPageText(WebView webView, ValueCallback<String> callback) {
        getAll(webView, page -> {
            if (callback != null) callback.onReceiveValue(page == null ? "" : page.pageText);
        });
    }

    public void getTitle(WebView webView, ValueCallback<String> callback) {
        runJs(webView,
                "(function(){try{return document.title||'';}catch(e){return '';}})();",
                value -> {
                    if (callback != null) callback.onReceiveValue(cleanJsString(value));
                });
    }

    public void getDescription(WebView webView, ValueCallback<String> callback) {
        runJs(webView,
                "(function(){" +
                        "try{" +
                        " var m=document.querySelector('meta[name=\"description\"],meta[property=\"og:description\"]');" +
                        " return m?((m.content||'')+''):'';" +
                        "}catch(e){return '';}" +
                        "})();",
                value -> {
                    if (callback != null) callback.onReceiveValue(cleanJsString(value));
                });
    }

    private void runJs(WebView webView, String script, ValueCallback<String> callback) {
        MAIN.post(() -> webView.evaluateJavascript(script, callback));
    }

    private PageText parsePageText(String raw) {
        PageText pageText = new PageText();
        String json = cleanJsString(raw);
        pageText.selectedText = pickJsonValue(json, "selectedText");
        pageText.pageText = pickJsonValue(json, "pageText");
        pageText.title = pickJsonValue(json, "title");
        pageText.description = pickJsonValue(json, "description");
        return pageText;
    }

    private String pickJsonValue(String json, String key) {
        if (TextUtils.isEmpty(json) || TextUtils.isEmpty(key)) return "";
        String token = "\"" + key + "\":";
        int start = json.indexOf(token);
        if (start == -1) return "";
        start += token.length();
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
        return "";
    }

    private String cleanJsString(String value) {
        if (value == null) return "";
        value = value.trim();
        if ("".equals(value)) return "";
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        value = value.replace("\\\\n", "\n")
                .replace("\\\\t", "\t")
                .replace("\\\\r", "")
                .replace("\\\\\"", "\"")
                .replace("\\\\\\\\", "\\");
        return value;
    }
}