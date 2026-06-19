package com.centigrade.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Stack;

/**
 * WebView 引擎封装层 — 浏览器的核心渲染单元
 * 使用 Android 内置 WebView 提供与 GeckoEngine 完全相同的接口
 */
public class GeckoEngine {
    private final WebView webView;
    private final Context context;
    private EngineCallback callback;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean isLoading;
    /** 跟踪当前 URL */
    private volatile String currentUrl = "";
    /** 自定义导航栈（WebView内置历史栈不满足需求，维持自定义栈） */
    private final Stack<String> backStack = new Stack<>();
    private final Stack<String> forwardStack = new Stack<>();
    private boolean canGoBackFlag = false;
    private boolean canGoForwardFlag = false;

    // === Android 桥接接口（供 JS 调用） ===

    /** JS 接口：资源嗅探（window.AndroidSniffer.onResourceFound） */
    public static class AndroidSnifferBridge {
        @JavascriptInterface
        public void onResourceFound(String url, String type, String mime, String source, String title) {
            // 嗅探回调 — 可在此记录资源
        }
    }

    /** JS 接口：长按文本选择（window.Android.onTextSelected） */
    public static class AndroidBridge {
        private final GeckoEngine engine;
        public AndroidBridge(GeckoEngine engine) { this.engine = engine; }
        @JavascriptInterface
        public void onTextSelected(String text) {
            // 长按文本回调 — 由外部处理
        }
    }

    // === 回调接口（与原来完全一致） ===
    public interface EngineCallback {
        default void onPageStart(String url) {}
        default void onPageStop(String url) {}
        default void onPageProgress(int progress) {}
        default void onTitleChange(String title) {}
        default void onCanGoBackChange(boolean canGoBack) {}
        default void onCanGoForwardChange(boolean canGoForward) {}
        /** 返回 true 表示已处理（拦截） */
        default boolean onLoadUri(String uri) { return false; }
        /** 视频/网页内容全屏显示 */
        default void onShowCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback customViewCallback) {}
        /** 退出全屏 */
        default void onHideCustomView() {}
    }

    public void setEngineCallback(EngineCallback cb) {
        this.callback = cb;
    }

    /**
     * 获取 WebView 实例（供高级操作，如 addJavascriptInterface）
     */
    public WebView getWebView() {
        return webView;
    }

    /**
     * 注册 JavaScript 接口（代理 addJavascriptInterface）
     * @param obj  接口实现对象（需含 @JavascriptInterface 方法）
     * @param name JS 中调用的接口名，如 "CenXHome"
     */
    public void addJavascriptInterface(@NonNull Object obj, @NonNull String name) {
        webView.addJavascriptInterface(obj, name);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public GeckoEngine(Context context) {
        this.context = context;
        this.webView = new WebView(context);
        this.isLoading = false;

        WebView.setWebContentsDebuggingEnabled(true);

        // === WebView 基础设置 ===
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        // 支持 ES6 模块等现代特性
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        // 启用 WebRTC 等多媒体能力
        webView.getSettings().setAllowFileAccess(true);
        // 允许透明背景（视频渲染需要）
        webView.setBackgroundColor(0);

        // === WebViewClient ===
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                isLoading = true;
                String displayUrl = (url != null && url.startsWith("data:")) ? "about:home" : url;
                currentUrl = displayUrl;
                uiHandler.post(() -> {
                    if (callback != null) callback.onPageStart(displayUrl);
                });

                // 尽早注入 Fullscreen API 修复（在 onPageFinished 之前）
                String earlyFix =
                    "(function() {" +
                    "  try {" +
                    "    if (document.readyState === 'loading') {" +
                    "      document.addEventListener('DOMContentLoaded', function() {" +
                    "        ['fullscreenEnabled','webkitFullscreenEnabled','mozFullScreenEnabled'].forEach(function(p) {" +
                    "          Object.defineProperty(document, p, {" +
                    "            get: function() { return true; }, configurable: true" +
                    "          });" +
                    "        });" +
                    "        ['fullscreenElement','webkitFullscreenElement','mozFullScreenElement'].forEach(function(p) {" +
                    "          Object.defineProperty(document, p, {" +
                    "            get: function() { return null; }, configurable: true" +
                    "          });" +
                    "        });" +
                    "        try {" +
                    "          var vp = HTMLVideoElement.prototype;" +
                    "          vp.requestFullscreen = vp.webkitRequestFullscreen = function() {" +
                    "            try { this.webkitEnterFullscreen(); } catch(e) {}" +
                    "          };" +
                    "        } catch(e2) {}" +
                    "      });" +
                    "    }" +
                    "  } catch(e) {}" +
                    "})();";
                view.evaluateJavascript(earlyFix, null);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                isLoading = false;
                final String callbackUrl = currentUrl;
                uiHandler.post(() -> {
                    if (callback != null) callback.onPageStop(callbackUrl);
                });

                // 注入 Fullscreen API 修复（解决百度视频等网站全屏按钮灰色问题）
                String fullscreenFix =
                    "(function() {" +
                    "  function fixFullscreen() {" +
                    "    try {" +
                    "      // ----- 1. 覆盖 Fullscreen API 检测 -----" +
                    "      var props = ['fullscreenEnabled','webkitFullscreenEnabled','mozFullScreenEnabled'];" +
                    "      props.forEach(function(p) {" +
                    "        try { Object.defineProperty(document, p, { get: function() { return true; }, configurable: true }); } catch(e1) {}" +
                    "      });" +
                    "      // ----- 2. 覆盖 fullscreenElement -----" +
                    "      var elems = ['fullscreenElement','webkitFullscreenElement','mozFullScreenElement'];" +
                    "      elems.forEach(function(p) {" +
                    "        try { Object.defineProperty(document, p, { get: function() { return null; }, configurable: true }); } catch(e2) {}" +
                    "      });" +
                    "      // ----- 3. 代理 HTMLVideoElement 全屏请求 -----" +
                    "      var proto = HTMLVideoElement.prototype;" +
                    "      var origReq = proto.requestFullscreen || proto.webkitRequestFullscreen || proto.mozRequestFullScreen;" +
                    "      proto.requestFullscreen = proto.webkitRequestFullscreen = function() {" +
                    "        try { if (origReq) return origReq.apply(this, arguments); } catch(e3) {}" +
                    "        try { this.webkitEnterFullscreen(); } catch(e4) {}" +
                    "      };" +
                    "      proto.mozRequestFullScreen = proto.requestFullscreen;" +
                    "      // ----- 4. 代理 exitFullscreen -----" +
                    "      document.exitFullscreen = document.webkitExitFullscreen = document.mozCancelFullScreen = function() {" +
                    "        try { document.webkitExitFullscreen(); } catch(e5) {}" +
                    "      };" +
                    "      // ----- 5. 移除所有 disabled 属性（包括按钮和任意元素）-----" +
                    "      var all = document.querySelectorAll('*');" +
                    "      for (var i = 0; i < all.length; i++) {" +
                    "        var el = all[i];" +
                    "        if (el.hasAttribute && el.hasAttribute('disabled')) {" +
                    "          var txt = (el.textContent || el.innerText || '').toLowerCase();" +
                    "          var cls = (el.className || '').toLowerCase();" +
                    "          var aria = (el.getAttribute('aria-label') || '').toLowerCase();" +
                    "          var title = (el.getAttribute('title') || '').toLowerCase();" +
                    "          if (txt.indexOf('full') >= 0 || txt.indexOf('全屏') >= 0 ||" +
                    "              cls.indexOf('full') >= 0 || aria.indexOf('full') >= 0 || title.indexOf('full') >= 0 ||" +
                    "              el.tagName === 'VIDEO') {" +
                    "            el.removeAttribute('disabled');" +
                    "            el.style.pointerEvents = 'auto';" +
                    "            el.style.cursor = 'pointer';" +
                    "            if (!el._fsFixed) {" +
                    "              el._fsFixed = true;" +
                    "              el.addEventListener('click', function(e) {" +
                    "                var vid = document.querySelector('video');" +
                    "                if (vid && vid.tagName === 'VIDEO') {" +
                    "                  try { vid.webkitEnterFullscreen(); } catch(ex) {}" +
                    "                }" +
                    "                e.stopPropagation();" +
                    "                e.preventDefault();" +
                    "              });" +
                    "            }" +
                    "          }" +
                    "        }" +
                    "      }" +
                    "    } catch(e6) {}" +
                    "  }" +
                    "  fixFullscreen();" +
                    "  var observer = new MutationObserver(function() { fixFullscreen(); });" +
                    "  if (document.body) observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['class','disabled','style'] });" +
                    "})();";

                view.evaluateJavascript(fullscreenFix, null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String uri = request.getUrl().toString();
                if (handleExternalScheme(uri)) {
                    return true;
                }
                if (callback != null && callback.onLoadUri(uri)) {
                    return true;
                }
                return false;
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
            }
        });

        // === WebChromeClient（进度、标题、视频全屏） ===
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                uiHandler.post(() -> {
                    if (callback != null) callback.onPageProgress(newProgress);
                });
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                uiHandler.post(() -> {
                    if (callback != null) callback.onTitleChange(title != null ? title : "");
                });
            }

            @Override
            public void onShowCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback customViewCallback) {
                super.onShowCustomView(view, requestedOrientation, customViewCallback);
                if (callback != null) {
                    callback.onShowCustomView(view, requestedOrientation, customViewCallback);
                }
            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();
                if (callback != null) {
                    callback.onHideCustomView();
                }
            }
        });
    }

    /** 初始化：注册 JS 接口、配置 User-Agent */
    public void init() {
        // 注册 JS 桥接接口 — 名称需与 JS 中 window.xxx 调用一致
        webView.addJavascriptInterface(new AndroidSnifferBridge(), "AndroidSniffer");
        webView.addJavascriptInterface(new AndroidBridge(this), "Android");

        // 注册 CenXHome 接口（主页快捷方式创建、扫码等）
        if (context instanceof MainActivity) {
            webView.addJavascriptInterface(
                    ((MainActivity) context).new HomePageBridge(), "CenXHome");
        }

        // 设置现代化的 User-Agent（解决部分网站 501 Not Implemented 问题）
        String ua = webView.getSettings().getUserAgentString();
        if (ua != null && !ua.contains("CenX")) {
            webView.getSettings().setUserAgentString(ua + " CenX/5.0");
        }
    }

    /** 判断外部 Scheme */
    private boolean handleExternalScheme(String url) {
        if (url == null || url.isEmpty()) return false;
        android.net.Uri uri = android.net.Uri.parse(url);
        String scheme = uri.getScheme();
        if (scheme == null) return false;
        String lowerScheme = scheme.toLowerCase();
        if ("http".equals(lowerScheme) || "https".equals(lowerScheme) ||
                "ftp".equals(lowerScheme) || "file".equals(lowerScheme) ||
                "about".equals(lowerScheme) || "data".equals(lowerScheme) ||
                "blob".equals(lowerScheme) || "javascript".equals(lowerScheme)) {
            return false;
        }
        return true;
    }

    /** 加载 URL */
    public void loadUrl(String url) {
        if (url != null && url.startsWith("javascript:")) {
            webView.evaluateJavascript(url, null);
            return;
        }
        currentUrl = url;
        webView.loadUrl(url);
    }

    /** 加载 HTML 数据 */
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        webView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    /** 加载首页 HTML */
    public void loadHomePage(String homeHtml) {
        currentUrl = "about:home";
        webView.loadDataWithBaseURL("file:///android_asset/", homeHtml, "text/html", "UTF-8", null);
    }

    /** 后退 */
    public boolean canGoBack() {
        return canGoBackFlag || webView.canGoBack();
    }

    public void goBack() {
        if (!backStack.isEmpty()) {
            String previousUrl = backStack.pop();
            if (!currentUrl.isEmpty()) {
                forwardStack.push(currentUrl);
            }
            canGoForwardFlag = true;
            canGoBackFlag = !backStack.isEmpty();
            loadUrl(previousUrl);
        } else if (webView.canGoBack()) {
            webView.goBack();
        }
    }

    /** 前进 */
    public boolean canGoForward() {
        return canGoForwardFlag || webView.canGoForward();
    }

    public void goForward() {
        if (!forwardStack.isEmpty()) {
            String nextUrl = forwardStack.pop();
            if (!currentUrl.isEmpty()) {
                backStack.push(currentUrl);
            }
            canGoBackFlag = true;
            canGoForwardFlag = !forwardStack.isEmpty();
            loadUrl(nextUrl);
        } else if (webView.canGoForward()) {
            webView.goForward();
        }
    }

    /** 停止加载 */
    public void stopLoading() {
        webView.stopLoading();
    }

    /** 刷新 */
    public void reload() {
        webView.reload();
    }

    /**
     * 执行 JavaScript（WebView 原生支持，通过 evaluateJavascript 实现）
     */
    public void evaluateJavascript(String script) {
        if (script != null) {
            webView.evaluateJavascript(script, null);
        }
    }

    /** 执行 JavaScript（带回调） */
    public void evaluateJavascript(String script, @Nullable ValueCallback<String> callback) {
        if (script != null) {
            webView.evaluateJavascript(script, callback != null ? callback : null);
        } else if (callback != null) {
            callback.onReceiveValue("");
        }
    }

    /** 获取当前 URL */
    public String getUrl() {
        return currentUrl;
    }

    /** 获取渲染 View（用于添加到布局） */
    public View getView() {
        return webView;
    }

    public boolean isLoading() {
        return isLoading;
    }

    /** 销毁 */
    public void destroy() {
        webView.removeAllViews();
        webView.destroy();
    }
}