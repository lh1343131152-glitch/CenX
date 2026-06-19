package com.centigrade.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;

public class TabManager {
    private static final String TAG = "TabManager";
    
    // 内部（不应拦截）的scheme集合
    private static final Set<String> INTERNAL_SCHEMES = new HashSet<>(Arrays.asList(
            "http", "https", "ftp", "ftps", "file", "about", "javascript", "data", "blob"
    ));
    
    // 广告拦截：常见广告域名列表
    private static final Set<String> AD_DOMAINS = new HashSet<>(Arrays.asList(
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "google-analytics.com", "googletagmanager.com", "googletagservices.com",
            "adservice.google.com", "pagead2.googlesyndication.com",
            "adserver.com", "adtechus.com", "adnxs.com", "adsrvr.org",
            "criteo.com", "criteo.net", "casalemedia.com",
            "moatads.com", "adsafeprotected.com", "scorecardresearch.com",
            "outbrain.com", "taboola.com", "sharethis.com",
            "amazon-adsystem.com", "amazonadsi.com",
            "pubmatic.com", "openx.net", "rubiconproject.com",
            "appnexus.com", "indexww.com", "agkn.com",
            "bluekai.com", "exelator.com", "rlcdn.com",
            "cnzz.com", "sina.com.cn/cpro", "qq.com/ads",
            "baidu.com/ads", "union.baidu.com",
            "taobao.com/ads", "alicdn.com",
            "sogou.com/ads", "360.cn/ads",
            "pos.baidu.com", "cbjs.baidu.com",
            "cpro.baidustatic.com", "hm.baidu.com",
            "c.cnzz.com", "s23.cnzz.com",
            "cm.ipinyou.com", "ad.3.cn",
            "ad.360.cn", "ad.qq.com",
            "gdt.qq.com", "adsrv.qq.com",
            "ads.tencent.com", "d1.com"
    ));

    // 广告拦截：URL路径包含的关键词
    private static final String[] AD_PATH_KEYWORDS = {
            "/ad/", "/ads/", "/advert", "/adserver", "/banner",
            "/popup", "/affiliate", "/click"
    };

    private Context context;
    private List<Tab> tabs;
    private int currentTabIndex;
    private OnTabChangedListener tabListener;
    private final Web401RetryManager web401RetryManager = new Web401RetryManager();

    // 网络状态监听
    public interface NetworkStateListener {
        void onNetworkStateChanged(boolean isConnected);
    }

    private NetworkStateListener networkStateListener;

    public void setNetworkStateListener(NetworkStateListener listener) {
        this.networkStateListener = listener;
    }

    public void notifyNetworkStateChanged(boolean isConnected) {
        if (networkStateListener != null) {
            networkStateListener.onNetworkStateChanged(isConnected);
        }
    }

    /**
     * 检查网络是否可用
     */
    public static boolean isNetworkConnected(Context ctx) {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                    ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * 检查URL是否为广告
     */
    public static boolean isAdUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return false;

            // 检查域名是否在黑名单中
            String lowerHost = host.toLowerCase();
            for (String adDomain : AD_DOMAINS) {
                if (lowerHost.contains(adDomain)) {
                    return true;
                }
            }

            // 检查路径是否包含广告关键词
            String path = uri.getPath();
            if (path != null) {
                String lowerPath = path.toLowerCase();
                for (String keyword : AD_PATH_KEYWORDS) {
                    if (lowerPath.contains(keyword)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
    
    public interface OnTabChangedListener {
        void onTabChanged(int index, Tab tab);
        void onTabCountChanged(int count);
        void onPageTitleChanged(int index, String title);
        void onPageProgressChanged(int index, int progress);
        void onPageFinished(int index, String title, String url);
        /** 视频/网页进入全屏 */
        void onShowCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback customViewCallback);
        /** 退出全屏 */
        void onHideCustomView();
    }
    
    public static class ResourceItem {
        public String url;
        public String type;
        public String mime;
        public String source;
        public String title;

        public ResourceItem(String url, String type, String mime, String source, String title) {
            this.url = url;
            this.type = type;
            this.mime = mime;
            this.source = source;
            this.title = title;
        }
    }

    public static class Tab {
        public GeckoEngine engine;
        public String title;
        public String url;
        public int progress;
        public boolean isLoading;
        public LinkedHashMap<String, ResourceItem> sniffedResources;
        
        public Tab(GeckoEngine engine, String title, String url) {
            this.engine = engine;
            this.title = title;
            this.url = url;
            this.progress = 0;
            this.isLoading = false;
            this.sniffedResources = new LinkedHashMap<>();
        }
    }
    
    public TabManager(Context context) {
        this.context = context;
        this.tabs = new ArrayList<>();
        this.currentTabIndex = -1;
    }
    
    public void setOnTabChangedListener(OnTabChangedListener listener) {
        this.tabListener = listener;
    }
    
    /**
     * 判断URL是否为外部App的URL Scheme，若是则尝试跳转并复制到剪贴板
     * @return true 表示已处理（应拦截WebView加载），false表示无需处理
     */
    private boolean handleExternalScheme(String url) {
        if (url == null || url.isEmpty()) return false;
        
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (scheme == null || scheme.isEmpty()) return false;
        
        // 内部scheme不拦截
        if (INTERNAL_SCHEMES.contains(scheme.toLowerCase())) return false;
        
        try {
            // 1. 复制到剪贴板
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("外部链接", url);
                clipboard.setPrimaryClip(clip);
            }
            
            // 2. 尝试跳转对应App
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                Toast.makeText(context, "正在跳转外部应用...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "未安装对应应用，链接已复制", Toast.LENGTH_SHORT).show();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public int createNewTab(String url) {
        // 创建 GeckoEngine（基于 WebView）
        GeckoEngine engine = new GeckoEngine(context);
        
        String title = "新标签";
        Tab tab = new Tab(engine, title, url != null ? url : "");
        tabs.add(tab);
        currentTabIndex = tabs.size() - 1;
        
        final PreferencesManager prefs = PreferencesManager.getInstance(context);
        final boolean adBlockEnabled = prefs.isAdBlockEnabled();
        
        // 设置引擎回调（替代 WebViewClient + WebChromeClient）
        engine.setEngineCallback(new GeckoEngine.EngineCallback() {
            @Override
            public void onPageStart(String url) {
                tab.isLoading = true;
                tab.url = url;
                tab.sniffedResources.clear();
                if (tabListener != null) {
                    tabListener.onPageProgressChanged(currentTabIndex, 0);
                }
            }

            @Override
            public void onPageStop(String url) {
                tab.isLoading = false;
                tab.url = url;
                if (tabListener != null) {
                    tabListener.onPageProgressChanged(currentTabIndex, 100);
                    tabListener.onPageFinished(currentTabIndex, tab.title, url);
                }

                if (adBlockEnabled && url != null && !url.isEmpty() && !"about:blank".equals(url) && !"about:home".equals(url)) {
                    injectAdBlockCss(engine);
                }
                if (prefs.isResourceSniffEnabled()) {
                    injectResourceSniffer(engine);
                }
            }

            @Override
            public void onPageProgress(int progress) {
                tab.progress = progress;
                if (tabListener != null) {
                    tabListener.onPageProgressChanged(currentTabIndex, progress);
                }
            }

            @Override
            public void onTitleChange(String title) {
                tab.title = title != null ? title : tab.url;
                if (tabListener != null) {
                    tabListener.onPageTitleChanged(currentTabIndex, tab.title);
                }
            }

            @Override
            public boolean onLoadUri(String uri) {
                if (handleExternalScheme(uri)) {
                    return true;
                }
                // 返回 false 让引擎继续正常加载
                return false;
            }

            @Override
            public void onShowCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback customViewCallback) {
                if (tabListener != null) {
                    tabListener.onShowCustomView(view, requestedOrientation, customViewCallback);
                }
            }

            @Override
            public void onHideCustomView() {
                if (tabListener != null) {
                    tabListener.onHideCustomView();
                }
            }
        });
        
        engine.init();
        
        // 加载 URL
        if (url != null && !url.isEmpty()) {
            if ("about:home".equals(url) || "about:blank".equals(url)) {
                loadHomePage(engine);
            } else {
                engine.loadUrl(url);
            }
        } else {
            loadHomePage(engine);
        }
        
        if (tabListener != null) {
            tabListener.onTabChanged(currentTabIndex, tab);
            tabListener.onTabCountChanged(tabs.size());
        }
        
        return currentTabIndex;
    }

    /**
     * 加载默认首页（从 assets/home_page.html 读取模板）
     * 快捷链接从PreferencesManager动态读取
     */
    private void loadHomePage(GeckoEngine engine) {
        PreferencesManager prefs = PreferencesManager.getInstance(context);
        java.util.List<PreferencesManager.QuickLink> links = prefs.getQuickLinks();
        int searchEngine = prefs.getSearchEngine();
        String searchUrl = prefs.getSearchEngineUrl(searchEngine);
        String homeBg = prefs.getHomeBackground();

        // ★ 内联 logo.jpg 为 base64，防止 data: URI 下相对路径无法加载
        String logoBase64 = null;
        try {
            InputStream logoIs = context.getAssets().open("logo.jpg");
            byte[] logoBytes = new byte[logoIs.available()];
            logoIs.read(logoBytes);
            logoIs.close();
            logoBase64 = android.util.Base64.encodeToString(logoBytes, android.util.Base64.NO_WRAP);
        } catch (Exception ignored) {}

        StringBuilder linksHtml = new StringBuilder();
        String[] colors = {"#2932e1", "#fb7299", "#0066ff", "#24292e", "#e74c3c", "#2ecc71",
                "#f39c12", "#9b59b6", "#1abc9c", "#e67e22", "#34495e", "#16a085"};
        int colorIndex = 0;

        for (PreferencesManager.QuickLink link : links) {
            String safeName = escapeHtml(link.name);
            String safeUrl = escapeHtml(link.url);

            if ("image".equals(link.iconType) && link.imagePath != null && !link.imagePath.isEmpty()) {
                String safeImagePath = escapeHtml(link.imagePath);
                String imageSrc = safeImagePath.startsWith("content://") || safeImagePath.startsWith("file://")
                        ? safeImagePath
                        : ("file://" + safeImagePath);
                linksHtml.append("  <a class='quick-link' href='").append(safeUrl).append("'>")
                         .append("    <div class='icon' style='background:#e0e0e0; overflow:hidden;'>")
                         .append("      <img src='").append(imageSrc).append("' class='icon-img' alt='' />")
                         .append("    </div><span>").append(safeName).append("</span></a>\n");
            } else {
                String iconBg;
                if (link.iconColor != null && !link.iconColor.isEmpty() && link.iconColor.startsWith("#")) {
                    iconBg = link.iconColor;
                } else {
                    iconBg = colors[colorIndex % colors.length];
                }
                colorIndex++;

                String safeIcon = escapeHtml(link.icon);
                if (safeIcon.isEmpty()) {
                    safeIcon = safeName.substring(0, 1).toUpperCase();
                }

                linksHtml.append("  <a class='quick-link' href='").append(safeUrl).append("'>")
                         .append("    <div class='icon' style='background:").append(iconBg).append(";'>")
                         .append(safeIcon).append("</div><span>").append(safeName).append("</span></a>\n");
            }
        }

        String homeHtml = readAssetText("home_page.html");
        if (homeHtml == null || homeHtml.isEmpty()) {
            homeHtml = "<!DOCTYPE html><html><body><h3>home_page.html 加载失败</h3></body></html>";
        }

        // 获取当前 CenXTheme 背景色（跟随动态取色和深色/浅色模式）
        String homeBgColor = String.format("#%06X", 0xFFFFFF & CenXTheme.colorSurfaceContainer(context));

        // ★ 修复方案：用 !important 内联覆盖所有背景属性，移除 CSS 中干扰的 color-scheme 自动覆盖
        // 1. 移除 HTML 中的 color-scheme，防止 WebView 自动覆盖 CSS 变量
        homeHtml = homeHtml.replace("color-scheme: light dark;", "");
        // 2. 在 body 标签中内联完整的 background 样式（用 !important 确保最高优先级）
        homeHtml = homeHtml.replace("<body>",
                "<body style=\"background-color:" + homeBgColor + " !important;" +
                "background-image:none !important;" +
                "\">");
        // ★ 3. logo.jpg 内联为 data:image 防止 data: URI 下相对路径加载失败
        if (logoBase64 != null) {
            homeHtml = homeHtml.replace("url('logo.jpg')", "url('data:image/jpeg;base64," + logoBase64 + "')");
        }

        homeHtml = homeHtml.replace("{{QUICK_LINKS}}", linksHtml.toString());
        homeHtml = homeHtml.replace("{{SEARCH_URL}}", searchUrl.replace("%s", "{q}"));
        homeHtml = homeHtml.replace("{{HOME_BG}}", escapeHtml(homeBg == null ? "" : homeBg));
        homeHtml = homeHtml.replace("{{HOME_BG_COLOR}}", homeBgColor);

        engine.loadHomePage(homeHtml);
    }

    private String readAssetText(String fileName) {
        StringBuilder builder = new StringBuilder();
        try {
            InputStream is = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();
            is.close();
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        String escaped = text;
        escaped = escaped.replace("&", "&" + "amp;");
        escaped = escaped.replace("<", "&" + "lt;");
        escaped = escaped.replace(">", "&" + "gt;");
        escaped = escaped.replace("'", "&#39;");
        escaped = escaped.replace(String.valueOf((char) 34), "&" + "quot;");
        return escaped;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback == null ? "" : fallback;
        }
        return value;
    }

    /**
     * 注入广告拦截CSS（隐藏常见广告元素）
     */
    private void injectAdBlockCss(GeckoEngine engine) {
        String css =
            "var style = document.createElement('style');" +
            "style.textContent = '" +
            "  .adsbygoogle,.adsbygoogle-noablate," +
            "  [id*=\"google_ads_iframe\"],[id*=\"google_ads_frame\"]," +
            "  .ad-container,.ad-box,.ad-wrapper,.ad-slot," +
            "  .adsbyadop,.ads-box,.ad-item,.ad-content," +
            "  [class*=\"ad-\"],[class*=\"_ad_\"]," +
            "  iframe[src*=\"doubleclick\"],iframe[src*=\"ads\"]," +
            "  div[class*=\"banner\"],div[id*=\"banner\"]," +
            "  .am-banner,.banner-ad,.advertisement," +
            "  .cpro-widget,.bd-cpro-widget," +
            "  [class*=\"advertisement\"],[id*=\"advertisement\"]," +
            "  iframe[src*=\"cpro\"],div[class*=\"cpro\"]," +
            "  .gg-box,.gg-div,.gg_widget,.ggad," +
            "  .pub_300x250,.pub_300x250m,.trc_related_container" +
            "  { display:none!important;visibility:hidden!important; }" +
            "';" +
            "document.head.appendChild(style);";
        engine.evaluateJavascript(css);
    }

    /**
     * 注入资源嗅探脚本
     * 捕获页面中的显式媒体与资源链接
     */
    private void injectResourceSniffer(GeckoEngine engine) {
        String js =
            "(function(){" +
            "  if(window.__resourceSniffInjected) return;" +
            "  window.__resourceSniffInjected = true;" +
            "  function pickType(url){" +
            "    if(!url) return 'other';" +
            "    if(/\\.(m3u8|mp4|mkv|webm|avi|mov|m4s|ts)(\\?|#|$)/i.test(url)) return 'video';" +
            "    if(/\\.(mp3|wav|flac|ogg|aac|m4a)(\\?|#|$)/i.test(url)) return 'audio';" +
            "    if(/\\.(jpg|jpeg|png|gif|webp|bmp|svg)(\\?|#|$)/i.test(url)) return 'image';" +
            "    if(/\\.(pdf|apk|zip|rar|7z|torrent)(\\?|#|$)/i.test(url)) return 'file';" +
            "    return 'other';" +
            "  }" +
            "  function report(url, type, title){" +
            "    if(!url || /^blob:/i.test(url) || /^data:/i.test(url)) return;" +
            "    if(window.AndroidSniffer && AndroidSniffer.onResourceFound){" +
            "      AndroidSniffer.onResourceFound(url, type || pickType(url), '', 'dom', title || '');" +
            "    }" +
            "  }" +
            "  function collectResources(){" +
            "    document.querySelectorAll('img').forEach(function(img){ report(img.currentSrc || img.src, 'image', img.alt || ''); });" +
            "    document.querySelectorAll('video').forEach(function(v){ report(v.currentSrc || v.src, 'video', document.title || ''); });" +
            "    document.querySelectorAll('video source').forEach(function(s){ report(s.src, 'video', document.title || ''); });" +
            "    document.querySelectorAll('audio').forEach(function(a){ report(a.currentSrc || a.src, 'audio', document.title || ''); });" +
            "    document.querySelectorAll('audio source').forEach(function(s){ report(s.src, 'audio', document.title || ''); });" +
            "    document.querySelectorAll('a[href]').forEach(function(a){ report(a.href, pickType(a.href), (a.textContent || '').trim()); });" +
            "  }" +
            "  collectResources();" +
            "  var observer = new MutationObserver(function(){ setTimeout(collectResources, 400); });" +
            "  if(document.body){ observer.observe(document.body, {childList:true, subtree:true, attributes:true}); }" +
            "  setInterval(collectResources, 3000);" +
            "})();";
        engine.evaluateJavascript(js);
    }

    private void recordSniffedResource(Tab tab, String url, String method, Map<String, String> headers, String mime, String source) {
        recordSniffedResource(tab, url, method, headers, mime, source, "", "");
    }

    private void recordSniffedResource(Tab tab, String url, String method, Map<String, String> headers, String mime, String source, String title, String forcedType) {
        if (tab == null || url == null) return;

        String cleanUrl = url.trim();
        if (cleanUrl.isEmpty()) return;
        String lowerUrl = cleanUrl.toLowerCase(Locale.ROOT);

        if (lowerUrl.startsWith("javascript:") || lowerUrl.startsWith("data:") || lowerUrl.startsWith("blob:")) return;
        if (!(lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://") || lowerUrl.startsWith("file://") || lowerUrl.startsWith("content://"))) return;

        String type = (forcedType != null && !forcedType.isEmpty()) ? forcedType : detectResourceType(cleanUrl, mime);
        if ("other".equals(type) && !looksInterestingResource(cleanUrl, mime)) return;

        String normalizedMime = mime;
        if (normalizedMime == null || normalizedMime.isEmpty()) {
            normalizedMime = guessMimeFromUrl(cleanUrl);
        }

        synchronized (tab.sniffedResources) {
            ResourceItem existing = tab.sniffedResources.get(cleanUrl);
            if (existing == null) {
                tab.sniffedResources.put(cleanUrl, new ResourceItem(cleanUrl, type, normalizedMime, source == null ? "network" : source, title == null ? "" : title));
            } else {
                if ((existing.mime == null || existing.mime.isEmpty()) && normalizedMime != null) {
                    existing.mime = normalizedMime;
                }
                if ((existing.title == null || existing.title.isEmpty()) && title != null && !title.isEmpty()) {
                    existing.title = title;
                }
                if ((existing.type == null || "other".equals(existing.type)) && type != null && !type.isEmpty()) {
                    existing.type = type;
                }
            }
        }
    }

    private boolean looksInterestingResource(String url, String mime) {
        String lowerUrl = url.toLowerCase(Locale.ROOT);
        String lowerMime = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        return lowerUrl.matches(".*\\.(m3u8|mp4|mkv|webm|avi|mov|m4s|ts|mp3|wav|flac|ogg|aac|m4a|jpg|jpeg|png|gif|webp|bmp|svg|pdf|apk|zip|rar|7z|torrent)(\\?.*)?$")
                || lowerMime.startsWith("video/")
                || lowerMime.startsWith("audio/")
                || lowerMime.startsWith("image/")
                || lowerMime.contains("mpegurl")
                || lowerMime.contains("mp2t")
                || lowerMime.equals("application/pdf")
                || lowerMime.equals("application/vnd.android.package-archive");
    }

    private String detectResourceType(String url, String mime) {
        String lowerUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);
        String lowerMime = mime == null ? "" : mime.toLowerCase(Locale.ROOT);

        if (lowerMime.startsWith("video/") || lowerMime.contains("mpegurl") || lowerMime.contains("mp2t")
                || lowerUrl.matches(".*\\.(m3u8|mp4|mkv|webm|avi|mov|m4s|ts)(\\?.*)?$")) {
            return "video";
        }
        if (lowerMime.startsWith("audio/")
                || lowerUrl.matches(".*\\.(mp3|wav|flac|ogg|aac|m4a)(\\?.*)?$")) {
            return "audio";
        }
        if (lowerMime.startsWith("image/")
                || lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|svg)(\\?.*)?$")) {
            return "image";
        }
        if (lowerUrl.matches(".*\\.(pdf|apk|zip|rar|7z|torrent)(\\?.*)?$")
                || "application/pdf".equals(lowerMime)
                || "application/vnd.android.package-archive".equals(lowerMime)) {
            return "file";
        }
        return "other";
    }

    private String guessMimeFromUrl(String url) {
        try {
            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
            if (extension != null && !extension.isEmpty()) {
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT));
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    public List<ResourceItem> getFoundResources() {
        Tab tab = getCurrentTab();
        List<ResourceItem> list = new ArrayList<>();
        if (tab == null) return list;
        synchronized (tab.sniffedResources) {
            list.addAll(tab.sniffedResources.values());
        }
        list.sort((a, b) -> Integer.compare(resourceTypePriority(a.type), resourceTypePriority(b.type)));
        return list;
    }

    private int resourceTypePriority(String type) {
        if ("video".equals(type)) return 0;
        if ("audio".equals(type)) return 1;
        if ("image".equals(type)) return 2;
        if ("file".equals(type)) return 3;
        return 4;
    }

    public void downloadResource(String url) {
        if (url == null || url.trim().isEmpty()) return;
        if (!(context instanceof Activity)) {
            Toast.makeText(context, "当前上下文无法启动下载", Toast.LENGTH_SHORT).show();
            return;
        }
        String downloadUrl = url.trim();
        String mimeType = guessMimeFromUrl(downloadUrl);
        PreferencesManager prefs = PreferencesManager.getInstance(context);
        LightningDownloadHandler.startDownload(
                (Activity) context,
                downloadUrl,
                prefs.getUserAgentString(prefs.getUserAgent()),
                null,
                mimeType,
                -1
        );
    }
    
    // WebView 配置方法已迁移至 GeckoEngine 内部
    
    public void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        if (index == currentTabIndex) return;
        
        currentTabIndex = index;
        Tab tab = tabs.get(index);
        
        if (tabListener != null) {
            tabListener.onTabChanged(index, tab);
            tabListener.onPageProgressChanged(index, tab.progress);
        }
    }
    
    public void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        if (tabs.size() <= 1) return; // 至少保留一个标签
        
        Tab removedTab = tabs.remove(index);
        removedTab.engine.destroy();
        
        // 调整currentTabIndex
        if (index <= currentTabIndex && currentTabIndex > 0) {
            currentTabIndex--;
        } else if (currentTabIndex >= tabs.size()) {
            currentTabIndex = tabs.size() - 1;
        }
        
        // 通知当前标签切换
        if (!tabs.isEmpty() && currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(currentTabIndex);
            if (tabListener != null) {
                tabListener.onTabChanged(currentTabIndex, currentTab);
                tabListener.onTabCountChanged(tabs.size());
            }
        }
    }
    
    public Tab getCurrentTab() {
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            return tabs.get(currentTabIndex);
        }
        return null;
    }
    
    public int getCurrentTabIndex() {
        return currentTabIndex;
    }
    
    public List<Tab> getTabs() {
        return tabs;
    }
    
    public int getTabCount() {
        return tabs.size();
    }
    
    public void goBack() {
        Tab tab = getCurrentTab();
        if (tab != null && tab.engine.canGoBack()) {
            tab.engine.goBack();
        }
    }
    
    public void goForward() {
        Tab tab = getCurrentTab();
        if (tab != null && tab.engine.canGoForward()) {
            tab.engine.goForward();
        }
    }
    
    public void refresh() {
        Tab tab = getCurrentTab();
        if (tab != null) {
            tab.engine.reload();
        }
    }

    public void loadUrlWithSiteHeaders(GeckoEngine engine, String url) {
        if (engine == null || url == null || url.trim().isEmpty()) return;
        // WebView 不支持自定义请求头，直接加载
        engine.loadUrl(url);
    }
    
    public void loadUrl(String url) {
        Tab tab = getCurrentTab();
        if (tab != null) {
            if ("about:home".equals(url)) {
                loadHomePage(tab.engine);
                tab.url = "about:home";
            } else {
                loadUrlWithSiteHeaders(tab.engine, url);
                tab.url = url;
            }
        }
    }
    
    public String getCurrentUrl() {
        Tab tab = getCurrentTab();
        return tab != null ? tab.url : "";
    }
    
    public String getCurrentTitle() {
        Tab tab = getCurrentTab();
        return tab != null ? tab.title : "";
    }
    
    public void applyUserAgent() {
        // WebView 的 User-Agent 在构造时通过 GeckoEngine 内部设置
        // 需要用户重启页面或新建标签后生效
    }

    public void refreshHomePages() {
        for (Tab tab : tabs) {
            if (tab == null || tab.engine == null) continue;
            if ("about:home".equals(tab.url) || "about:blank".equals(tab.url)) {
                loadHomePage(tab.engine);
            }
        }
    }
    
    public void destroy() {
        for (Tab tab : tabs) {
            if (tab.engine != null) {
                tab.engine.destroy();
            }
        }
        tabs.clear();
    }
}