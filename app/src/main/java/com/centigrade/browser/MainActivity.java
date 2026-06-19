package com.centigrade.browser;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.materialswitch.MaterialSwitch;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.centigrade.browser.ai.AiSelfNet;
import com.centigrade.browser.ai.AiSimpleJson;

import com.centigrade.browser.ai.AiAddressSmart;
import com.centigrade.browser.ai.AiBrowserKit;
import com.centigrade.browser.ai.AiCoreLogic;

public class MainActivity extends AppCompatActivity implements TabManager.OnTabChangedListener {
    
    private static MainActivity currentInstance;
    private static final int REQUEST_BOOKMARKS = 1001;
    private static final int REQUEST_HISTORY = 1002;
    private static final int REQUEST_SCAN_QR = 1003;
    
    // 内部（不应拦截）的scheme集合
    private static final Set<String> INTERNAL_SCHEMES = new HashSet<>(Arrays.asList(
            "http", "https", "ftp", "ftps", "file", "about", "javascript", "data", "blob"
    ));
    
    private TextView tvTitle;
    private ImageButton btnCancelSearch;
    private EditText editSearch;
    private ImageButton btnSearch;
    private ImageButton btnBack, btnForward, btnRefresh, btnHome, btnTabs, btnMoreBottom;
    private ImageButton btnBackPad, btnForwardPad, btnHomePad, btnTabsPad, btnMorePad;
    private ProgressBar progressBar;
    private FrameLayout webviewContainer;
    private LinearLayout tabContainer;
    private HorizontalScrollView tabScroll;
    private LinearLayout editSearchbar;
    private TabManager tabManager;
    private PreferencesManager prefs;
private HistoryManager historyManager;
private BookmarkManager bookmarkManager;
private boolean isSearchMode = false;
private EditText currentQuickLinkImagePathEdit;
    private boolean pendingHomeRevealAnimation = false;
    private boolean isFullscreenMode = false;
    private boolean isPadMode = false;
 
    private TextView btnTabAddTop;
    private FrameLayout btnExitFullscreen;
    private FrameLayout fullscreenVideoContainer;
    private View fullscreenVideoView;
    private WebChromeClient.CustomViewCallback fullscreenVideoCallback;
    private ImageView homeBackgroundImage;
    private View homeBackgroundScrim;
    private AiBrowserKit aiKit;
    private AiPanelController aiPanelController;
    private final AiSelfNet aiSelfNet = new AiSelfNet();
    private final AiSimpleJson aiSimpleJson = new AiSimpleJson();
    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (tabManager != null) {
                tabManager.notifyNetworkStateChanged(isNetworkAvailable());
            }
        }
    };

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentInstance = this;
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        
        prefs = PreferencesManager.getInstance(this);
        aiKit = new AiBrowserKit(this);
        aiPanelController = new AiPanelController(this, prefs, new AiPanelController.ActionHandler() {
            @Override
            public void openAiChat() {
                MainActivity.this.openAiChat();
            }

            @Override
            public void showAiQuestionDialog(int mode, String title, String hint, String fallbackQuestion) {
                aiPanelController.showAiQuestionDialog(mode, title, hint, fallbackQuestion);
            }

            @Override
            public void openReaderModeFromCurrentPage() {
                MainActivity.this.openReaderModeFromCurrentPage();
            }

            @Override
            public void openAiSettings() {
                MainActivity.this.openAiSettings();
            }

        });
        tabManager = new TabManager(this);
        tabManager.setOnTabChangedListener(this);
        historyManager = HistoryManager.getInstance(this);
        bookmarkManager = BookmarkManager.getInstance(this);
        
        initViews();
        setupTopBar();
        setupBottomBar();
        ensureStartupFilePermissionIfNeeded();
        try {
            registerNetworkReceiver();
        } catch (Exception ignored) {
        }
        
        String incomingUrl = extractUrlFromIntent(getIntent());
        if (!TextUtils.isEmpty(incomingUrl)) {
            tabManager.createNewTab(incomingUrl);
        } else {
            tabManager.createNewTab(prefs.getHomepage());
        }

        
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        String incomingUrl = extractUrlFromIntent(intent);
        if (!TextUtils.isEmpty(incomingUrl)) {
            tabManager.createNewTab(incomingUrl);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentInstance = this;
        updateHomeBackground(tabManager != null ? tabManager.getCurrentUrl() : null);
    }
    public static MainActivity getCurrentInstance() {
        return currentInstance;
    }

    public TabManager getTabManager() {
        return tabManager;
    }

    private String extractUrlFromIntent(Intent intent) {
        if (intent == null) return null;

        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null) {
                String url = data.toString();
                if (!TextUtils.isEmpty(url)) return url;
            }
        }

        if (Intent.ACTION_SEND.equals(action) && intent.getType() != null
                && intent.getType().startsWith("text/")) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            String extracted = extractFirstUrl(sharedText);
            if (!TextUtils.isEmpty(extracted)) {
                return extracted;
            }
        }

        return null;
    }

    private String extractFirstUrl(String text) {
        if (TextUtils.isEmpty(text)) return null;

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(https?://[^\\s]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        String trimmed = text.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        return null;
    }

    private void applyEdgeToEdgeInsets() {
        final View root = findViewById(R.id.root_main);
        final View tabScrollView = findViewById(R.id.tab_scroll);
        final View topBar = findViewById(R.id.top_bar);
        final View bottomBar = findViewById(R.id.bottom_bar);

        if (root == null || tabScrollView == null || topBar == null || bottomBar == null) return;

        final int tabScrollPaddingStart = tabScrollView.getPaddingStart();
        final int tabScrollPaddingTop = tabScrollView.getPaddingTop();
        final int tabScrollPaddingEnd = tabScrollView.getPaddingEnd();
        final int tabScrollPaddingBottom = tabScrollView.getPaddingBottom();

        final int topBarPaddingStart = topBar.getPaddingStart();
        final int topBarPaddingTop = topBar.getPaddingTop();
        final int topBarPaddingEnd = topBar.getPaddingEnd();
        final int topBarPaddingBottom = topBar.getPaddingBottom();

        final int bottomBarPaddingStart = bottomBar.getPaddingStart();
        final int bottomBarPaddingTop = bottomBar.getPaddingTop();
        final int bottomBarPaddingEnd = bottomBar.getPaddingEnd();
        final int bottomBarPaddingBottom = bottomBar.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(0, systemBars.top, 0, 0);

            tabScrollView.setPadding(
                    tabScrollPaddingStart,
                    tabScrollPaddingTop,
                    tabScrollPaddingEnd,
                    tabScrollPaddingBottom
            );

            topBar.setPadding(
                    topBarPaddingStart,
                    topBarPaddingTop,
                    topBarPaddingEnd,
                    topBarPaddingBottom
            );

            bottomBar.setPadding(
                    bottomBarPaddingStart,
                    bottomBarPaddingTop,
                    bottomBarPaddingEnd,
                    bottomBarPaddingBottom + systemBars.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void ensureStartupFilePermissionIfNeeded() {
        if (prefs == null || prefs.isOnboardingCompleted()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    Toast.makeText(this, "首次使用请授予文件访问权限", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    try {
                        Intent altIntent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivity(altIntent);
                    } catch (Exception ignored) {
                    }
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1101);
        }

        prefs.setOnboardingCompleted(true);
    }

    private void initViews() {
        applyEdgeToEdgeInsets();
        tvTitle = findViewById(R.id.tv_title);
        btnCancelSearch = findViewById(R.id.btn_cancel_search);
        editSearch = findViewById(R.id.edit_search);
        btnSearch = findViewById(R.id.btn_search);
        editSearchbar = findViewById(R.id.search_bar);
        progressBar = findViewById(R.id.progress_bar);
        webviewContainer = findViewById(R.id.webview_container);
        tabContainer = findViewById(R.id.tab_container);
        tabScroll = findViewById(R.id.tab_scroll);
        btnTabAddTop = findViewById(R.id.btn_tab_add_top);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnHome = findViewById(R.id.btn_home);
        btnTabs = findViewById(R.id.btn_tabs);
        btnMoreBottom = findViewById(R.id.btn_more_bottom);
        btnBackPad = findViewById(R.id.btn_back_pad);
        btnForwardPad = findViewById(R.id.btn_forward_pad);
        btnHomePad = findViewById(R.id.btn_home_pad);
        btnTabsPad = findViewById(R.id.btn_tabs_pad);
        btnMorePad = findViewById(R.id.btn_more_pad);
        btnExitFullscreen = findViewById(R.id.btn_exit_fullscreen);
        fullscreenVideoContainer = findViewById(R.id.fullscreen_video_container);
        fullscreenVideoView = null;
        
        homeBackgroundImage = findViewById(R.id.home_background_image);
        homeBackgroundScrim = findViewById(R.id.home_background_scrim);
        updateHomeBackground(prefs != null ? prefs.getHomepage() : null);
        

        if (btnExitFullscreen != null) {
            btnExitFullscreen.setOnClickListener(v -> {
                if (isFullscreenMode) toggleFullscreenMode();
            });
        }

        if (btnTabAddTop != null) {
            btnTabAddTop.setOnClickListener(v -> tabManager.createNewTab(prefs.getHomepage()));
        }

        isPadMode = (getResources().getConfiguration().smallestScreenWidthDp >= 720);
        applyAdaptiveBars();
    }
    private void setupTopBar() {
        tvTitle.setOnClickListener(v -> enterSearchMode(true));
        
        btnSearch.setOnClickListener(v -> {
            if (isSearchMode) {
                performSearch();
            } else {
                enterSearchMode(false);
            }
        });

        btnCancelSearch.setOnClickListener(v -> exitSearchMode());
        
        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                performSearch();
                return true;
            }
            return false;
        });
        
        editSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && TextUtils.isEmpty(editSearch.getText().toString().trim())) {
                exitSearchMode();
            }
        });
    }

    private void applyAdaptiveBars() {
        View topBar = findViewById(R.id.top_bar);
        View bottomBar = findViewById(R.id.bottom_bar);
        View padBarActions = findViewById(R.id.pad_bar_actions);
        View btnRefreshView = findViewById(R.id.btn_refresh);
        View tabBarContainer = findViewById(R.id.tab_bar_container);

        if (topBar == null || bottomBar == null || padBarActions == null) return;

        boolean usePadLikeUi = isPadMode
                || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        RelativeLayout.LayoutParams titleParams = tvTitle != null
               ? (RelativeLayout.LayoutParams) tvTitle.getLayoutParams() : null;
        RelativeLayout.LayoutParams searchParams = editSearchbar != null
                ? (RelativeLayout.LayoutParams) editSearchbar.getLayoutParams() : null;

        if (usePadLikeUi) {
            padBarActions.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.GONE);
            if (tabBarContainer != null) {
                tabBarContainer.setVisibility(View.VISIBLE);
            }
            if (btnRefreshView != null) {
                btnRefreshView.setVisibility(View.GONE);
            }
            if (titleParams != null) {
                titleParams.addRule(RelativeLayout.START_OF, R.id.pad_bar_actions);
                tvTitle.setLayoutParams(titleParams);
            }
            if (searchParams != null) {
                searchParams.addRule(RelativeLayout.START_OF, R.id.pad_bar_actions);
                editSearchbar.setLayoutParams(searchParams);
            }
        } else {
            padBarActions.setVisibility(View.GONE);
            bottomBar.setVisibility(View.VISIBLE);
            if (tabBarContainer != null) {
                tabBarContainer.setVisibility(View.GONE);
            }
            if (btnRefreshView != null) {
                btnRefreshView.setVisibility(View.VISIBLE);
            }
            if (titleParams != null) {
                titleParams.addRule(RelativeLayout.START_OF, R.id.btn_refresh);
                tvTitle.setLayoutParams(titleParams);
            }
            if (searchParams != null) {
                searchParams.addRule(RelativeLayout.START_OF, R.id.btn_refresh);
                editSearchbar.setLayoutParams(searchParams);
            }
        }
    }

    private void toggleFullscreenMode() {
        isFullscreenMode = !isFullscreenMode;

        View topBar = findViewById(R.id.top_bar);
        View bottomBar = findViewById(R.id.bottom_bar);
        View tabArea = findViewById(R.id.tab_scroll);
        View tabAdd = findViewById(R.id.btn_tab_add_top);
        View tabBarContainer = findViewById(R.id.tab_bar_container);

        if (isFullscreenMode) {
            if (topBar != null) topBar.setVisibility(View.GONE);
            if (bottomBar != null) bottomBar.setVisibility(View.GONE);
            if (tabArea != null) tabArea.setVisibility(View.GONE);
            if (tabAdd != null) tabAdd.setVisibility(View.GONE);
            if (tabBarContainer != null) tabBarContainer.setVisibility(View.GONE);
            if (btnExitFullscreen != null) btnExitFullscreen.setVisibility(View.VISIBLE);
        } else {
            if (topBar != null) topBar.setVisibility(View.VISIBLE);
            if (btnExitFullscreen != null) btnExitFullscreen.setVisibility(View.GONE);
            applyAdaptiveBars();
        }

        Toast.makeText(this, isFullscreenMode ? "" : "已退出全屏模式", Toast.LENGTH_SHORT).show();
    }

    private void launchScan() {
        Intent intent = new Intent(this, QrScanActivity.class);
        startActivityForResult(intent, REQUEST_SCAN_QR);
    }

    private void openAddQuickLinkFromHome() {
        showQuickLinkEditor(null, -1);
    }

    public class HomePageBridge {
        @JavascriptInterface
        public void addQuickLink() {
            runOnUiThread(() -> openAddQuickLinkFromHome());
        }

        @JavascriptInterface
        public void scanQr() {
            runOnUiThread(() -> launchScan());
        }
    }

    public class AiMarkdownBridge {
        @JavascriptInterface
        public void copyText(String text) {
            runOnUiThread(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("代码", text == null ? "" : text));
                    Toast.makeText(MainActivity.this, "代码已复制", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void enterSearchMode(boolean clearInput) {
        isSearchMode = true;
        tvTitle.setVisibility(View.GONE);
        editSearchbar.setVisibility(View.VISIBLE);
        editSearch.requestFocus();
        
        if (clearInput) {
            editSearch.setText("");
        } else {
            String currentUrl = tabManager.getCurrentUrl();
            if (!TextUtils.isEmpty(currentUrl) && !"about:blank".equals(currentUrl)) {
                editSearch.setText(currentUrl);
                editSearch.setSelection(currentUrl.length());
            } else {
                editSearch.setText("");
            }
        }
        
        android.view.inputmethod.InputMethodManager imm = 
            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(editSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
    }
    
    private void exitSearchMode() {
        isSearchMode = false;
        editSearchbar.setVisibility(View.GONE);
        tvTitle.setVisibility(View.VISIBLE);
        
        android.view.inputmethod.InputMethodManager imm = 
            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
    }
    
    /**
     * 判断URL是否为外部App的URL Scheme，若是则复制到剪贴板并跳转
     * @return true 表示已处理（不应继续WebView加载），false表示无需处理
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
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("外部链接", url);
                clipboard.setPrimaryClip(clip);
            }

            // 2. 尝试跳转对应App
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                Toast.makeText(this, "正在跳转外部应用...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "未安装对应应用，链接已复制", Toast.LENGTH_SHORT).show();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void performSearch() {
        String input = editSearch.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            exitSearchMode();
            return;
        }

        if (handleExternalScheme(input)) {
            exitSearchMode();
            return;
        }

        String finalUrl = processUrl(input);
        if (tabManager.getCurrentTab() == null) {
            tabManager.createNewTab(finalUrl);
        } else {
            tabManager.loadUrl(finalUrl);
        }
        exitSearchMode();
    }

    private boolean isHomePageUrl(String url) {
        if (url == null) return true;
        if ("about:blank".equals(url) || "about:home".equals(url)) return true;
        // 某些情况下主页 URL 可能是 data: 或 file: 开头
        if (url.startsWith("data:text/html") || url.startsWith("file://")) return true;
        return false;
    }

    private void updateHomeBackground(String url) {
        View rootMain = findViewById(R.id.root_main);
        View topBar = findViewById(R.id.top_bar);
        View bottomBar = findViewById(R.id.bottom_bar);

        String background = prefs != null ? prefs.getHomeBackground() : "";
        boolean showBg = !TextUtils.isEmpty(background) && (url == null || isHomePageUrl(url));

        if (showBg) {
            // 1. root_main 设背景图
            try {
                android.graphics.drawable.Drawable d;
                Uri uri = Uri.parse(background);
                String scheme = uri.getScheme();
                if ("content".equals(scheme) || "file".equals(scheme)) {
                    java.io.InputStream is = getContentResolver().openInputStream(uri);
                    if (is != null) {
                        d = android.graphics.drawable.Drawable.createFromStream(is, null);
                        is.close();
                    } else {
                        d = android.graphics.drawable.Drawable.createFromPath(background);
                    }
                } else {
                    d = android.graphics.drawable.Drawable.createFromPath(background);
                }
                if (d != null && rootMain != null) {
                    rootMain.setBackground(d);
                }
            } catch (Exception ignored) {}

            // 2. scrim 透底
            if (homeBackgroundScrim != null) homeBackgroundScrim.setAlpha(0.25f);

            // 3. TopBar/BottomBar 完全透明
            setTransparentWithRetry(topBar, 0, 500);
            setTransparentWithRetry(bottomBar, 0, 500);

            // 4. 系统栏透明（强制最低层 API）
            getWindow().setStatusBarColor(0);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setNavigationBarColor(0);
            // 对 Android 10+ 额外处理
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getWindow().setNavigationBarContrastEnforced(false);
            }

            // 5. 主页 body 透明（JS 注入 + 延时重试防覆盖）
            TabManager.Tab currentTab = tabManager != null ? tabManager.getCurrentTab() : null;
            if (currentTab != null && currentTab.engine != null) {
                String js = "try{document.body.style.background='transparent';" +
                    "document.documentElement.style.background='transparent';" +
                    "document.querySelector('.search-shell')&&" +
                    "(document.querySelector('.search-shell').style.background='transparent');" +
                    "var r=document.querySelector(':root');" +
                    "if(r)r.style.setProperty('--bg','transparent');" +
                    "}catch(e){}";
                currentTab.engine.evaluateJavascript(js, null);
                // 延时重试确保被动态取色覆盖后再次透明
                currentTab.engine.getView().postDelayed(() -> {
                    currentTab.engine.evaluateJavascript(js, null);
                }, 500);
                currentTab.engine.getView().postDelayed(() -> {
                    currentTab.engine.evaluateJavascript(js, null);
                }, 1000);
                currentTab.engine.getView().postDelayed(() -> {
                    currentTab.engine.evaluateJavascript(js, null);
                }, 2000);
            }
            return;
        }

        // 恢复默认
        if (rootMain != null) {
            rootMain.setBackgroundResource(0);
            rootMain.setBackgroundColor(CenXTheme.colorSurface(this));
        }
        if (homeBackgroundImage != null) homeBackgroundImage.setVisibility(View.GONE);
        if (homeBackgroundScrim != null) homeBackgroundScrim.setAlpha(1f);
        if (topBar != null) topBar.setBackgroundResource(R.drawable.home_surface_glass);
        if (bottomBar != null) bottomBar.setBackgroundResource(R.drawable.home_surface_glass);
        try {
            getWindow().setNavigationBarContrastEnforced(true);
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(android.R.attr.statusBarColor, tv, true))
                getWindow().setStatusBarColor(tv.data);
            if (getTheme().resolveAttribute(android.R.attr.navigationBarColor, tv, true))
                getWindow().setNavigationBarColor(tv.data);
        } catch (Exception ignored) {}
    }

    private void setTransparentWithRetry(View view, int color, int retryMs) {
        if (view == null) return;
        view.setBackgroundColor(color);
        view.postDelayed(() -> { if (view != null) view.setBackgroundColor(color); }, retryMs);
        view.postDelayed(() -> { if (view != null) view.setBackgroundColor(color); }, retryMs * 2);
    }

    private String getDisplayTitle(String title, String url) {
        if (isHomePageUrl(url)) {
            return "主页";
        }
        if (!TextUtils.isEmpty(title)) {
            return title;
        }
        if (!TextUtils.isEmpty(url)) {
            return url;
        }
        return "新标签";
    }
    
    private void showBottomMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_CenX_BottomSheetDialog);
        dialog.setContentView(R.layout.dialog_bottom_menu);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View menuBookmarks = dialog.findViewById(R.id.menu_bookmarks);
        View menuHistory = dialog.findViewById(R.id.menu_history);
        View menuDownload = dialog.findViewById(R.id.menu_download);
        View menuSharePage = dialog.findViewById(R.id.menu_share_page);
        View menuSource = dialog.findViewById(R.id.menu_source);
        View menuResourceSniffer = dialog.findViewById(R.id.menu_resource_sniffer);
        View menuIncognito = dialog.findViewById(R.id.menu_incognito);
        ImageView iconIncognito = dialog.findViewById(R.id.icon_incognito);
        TextView textIncognito = dialog.findViewById(R.id.text_incognito);
        View menuScan = dialog.findViewById(R.id.menu_scan);
        View menuFullscreen = dialog.findViewById(R.id.menu_fullscreen);
        View menuAi = dialog.findViewById(R.id.menu_ai);
        View menuToolbox = dialog.findViewById(R.id.menu_toolbox);
        View menuSettings = dialog.findViewById(R.id.menu_settings);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        menuBookmarks.setOnClickListener(v -> {
            dialog.dismiss();
            openBookmarksList();
        });

        menuHistory.setOnClickListener(v -> {
            dialog.dismiss();
            openHistoryList();
        });

        menuDownload.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(MainActivity.this, DownloadManagerActivity.class));
        });

        menuSharePage.setOnClickListener(v -> {
            dialog.dismiss();
            shareCurrentPage();
        });

        menuSource.setOnClickListener(v -> {
            dialog.dismiss();
            openSourceView();
        });

        menuResourceSniffer.setOnClickListener(v -> {
            dialog.dismiss();
            showResourceSnifferDialog();
        });

        boolean isIncognito = prefs.isIncognitoMode();
        int incognitoOnColor = CenXTheme.colorPrimary(this);
        int incognitoOffColor = CenXTheme.colorOnSurface(this);
        if (iconIncognito != null) {
            iconIncognito.setColorFilter(isIncognito ? incognitoOnColor : incognitoOffColor);
        }
        if (textIncognito != null) {
            textIncognito.setTextColor(isIncognito ? incognitoOnColor : incognitoOffColor);
        }
        if (menuIncognito != null) {
            menuIncognito.setOnClickListener(v -> {
                boolean newState = !prefs.isIncognitoMode();
                prefs.setIncognitoMode(newState);
                if (iconIncognito != null) {
                    iconIncognito.setColorFilter(newState ? incognitoOnColor : incognitoOffColor);
                }
                if (textIncognito != null) {
                    textIncognito.setTextColor(newState ? incognitoOnColor : incognitoOffColor);
                }
                Toast.makeText(
                        MainActivity.this,
                        newState ? "无痕模式已开启，浏览记录不会被保存" : "无痕模式已关闭",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        if (menuScan != null) {
            menuScan.setOnClickListener(v -> {
                dialog.dismiss();
                launchScan();
            });
        }
        if (menuFullscreen != null) {
            menuFullscreen.setOnClickListener(v -> {
                dialog.dismiss();
                toggleFullscreenMode();
            });
        }

        if (menuAi != null) {
            menuAi.setOnClickListener(v -> {
                dialog.dismiss();
                showAiPanel();
            });
        }

        menuToolbox.setOnClickListener(v -> {
            dialog.dismiss();
            openToolbox();
        });

        menuSettings.setOnClickListener(v -> {
            dialog.dismiss();
            openSettings();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showAiPanel() {
        if (aiPanelController != null) {
            aiPanelController.showAiPanel();
        }
    }

    private boolean shouldUseDynamicColors() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                && prefs != null
                && prefs.isDynamicColorEnabled();
    }

    private int mdColorSurface() {
        boolean dark = isDarkMode();
        if (shouldUseDynamicColors()) {
            return getColor(dark ? android.R.color.system_neutral1_900 : android.R.color.system_neutral1_10);
        }
        return dark ? Color.parseColor("#FF1C1B1F") : Color.parseColor("#FFFFFBFE");
    }

    private int mdColorSurfaceVariant() {
        boolean dark = isDarkMode();
        if (shouldUseDynamicColors()) {
            return getColor(dark ? android.R.color.system_neutral2_800 : android.R.color.system_neutral2_50);
        }
        return dark ? Color.parseColor("#FF2B2930") : Color.parseColor("#FFE7E0EC");
    }

    private int mdColorPrimary() {
        boolean dark = isDarkMode();
        if (shouldUseDynamicColors()) {
            return getColor(dark ? android.R.color.system_accent1_200 : android.R.color.system_accent1_600);
        }
        return dark ? Color.parseColor("#FFD0BCFF") : Color.parseColor("#FF6750A4");
    }

    int mdColorOnSurface() {
        boolean dark = isDarkMode();
        if (shouldUseDynamicColors()) {
            return getColor(dark ? android.R.color.system_neutral1_50 : android.R.color.system_neutral1_900);
        }
        return dark ? Color.parseColor("#FFE6E1E5") : Color.parseColor("#FF1C1B1F");
    }

    int mdColorOnSurfaceVariant() {
        boolean dark = isDarkMode();
        if (shouldUseDynamicColors()) {
            return getColor(dark ? android.R.color.system_neutral2_200 : android.R.color.system_neutral2_700);
        }
        return dark ? Color.parseColor("#FFCAC4D0") : Color.parseColor("#FF49454F");
    }

    int mdColorOutline() {
        boolean dark = isDarkMode();
        if (shouldUseDynamicColors()) {
            return getColor(dark ? android.R.color.system_neutral2_500 : android.R.color.system_neutral2_400);
        }
        return dark ? Color.parseColor("#FF938F99") : Color.parseColor("#FF79747E");
    }

    android.graphics.drawable.GradientDrawable mdDialogBackground() {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(mdColorSurface());
        bg.setCornerRadius(dp(28));
        bg.setStroke(dp(1), mdColorOutline());
        return bg;
    }

    android.graphics.drawable.GradientDrawable mdInputBackground() {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(mdColorSurfaceVariant());
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), mdColorOutline());
        return bg;
    }

    android.graphics.drawable.GradientDrawable mdButtonBackground(boolean primary) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(primary ? mdColorPrimary() : mdColorSurfaceVariant());
        return bg;
    }

    private void showAiQuestionDialog(int mode, String title, String hint, String fallbackQuestion) {
        if (aiPanelController != null) {
            aiPanelController.showAiQuestionDialog(mode, title, hint, fallbackQuestion);
        }
    }

    private void openReaderModeFromCurrentPage() {
        TabManager.Tab currentTab = tabManager.getCurrentTab();
        if (currentTab == null || currentTab.engine == null) {
            Toast.makeText(this, "当前页面不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        currentTab.engine.evaluateJavascript(
                "(function(){" +
                        "var title=document.title||'';" +
                        "var body=document.body?document.body.innerText:'';" +
                        "return JSON.stringify({title:title,content:body});" +
                        "})()",
                value -> {
                    String title = currentTab.title == null ? "" : currentTab.title;
                    String content = "";
                    try {
                        if (value != null && value.length() >= 2) {
                            String unquoted = value;
                            if (unquoted.startsWith("\"") && unquoted.endsWith("\"")) {
                                unquoted = unquoted.substring(1, unquoted.length() - 1);
                            }
                            unquoted = unquoted.replace("\\n", "\n")
                                    .replace("\\t", "\t")
                                    .replace("\\r", "")
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\");
                            JSONObject obj = new JSONObject(unquoted);
                            title = obj.optString("title", title);
                            content = obj.optString("content", "");
                        }
                    } catch (Exception ignored) {
                    }

                    Intent intent = new Intent(MainActivity.this, ReaderModeActivity.class);
                    intent.putExtra("title", title);
                    intent.putExtra("content", content);
                    intent.putExtra("url", currentTab.url);
                    startActivity(intent);
                }
        );
    }

    private void addCurrentPageBookmark() {
        TabManager.Tab currentTab = tabManager.getCurrentTab();
        if (currentTab == null || currentTab.url == null || currentTab.url.isEmpty() || "about:blank".equals(currentTab.url)) {
            Toast.makeText(this, "无法添加书签：当前页面无效", Toast.LENGTH_SHORT).show();
            return;
        }
        
        final String title = currentTab.title != null && !currentTab.title.isEmpty() ? currentTab.title : currentTab.url;
        final String url = currentTab.url;
        
        if (bookmarkManager.isBookmarked(url)) {
            // 使用自定义底部Dialog替换AlertDialog
            final BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_CenX_BottomSheetDialog);
            dialog.setContentView(R.layout.dialog_bookmark_confirm);
            dialog.setCanceledOnTouchOutside(true);
            
            TextView tvMessage = dialog.findViewById(R.id.tv_bookmark_message);
            if (tvMessage != null) {
                tvMessage.setText("「" + title + "」已经在书签中，是否删除？");
            }
            
            dialog.findViewById(R.id.btn_bookmark_keep).setOnClickListener(v -> dialog.dismiss());
            dialog.findViewById(R.id.btn_bookmark_delete).setOnClickListener(v -> {
                dialog.dismiss();
                int deleted = bookmarkManager.deleteBookmarkByUrl(url);
                if (deleted > 0) {
                    Toast.makeText(this, "已删除书签", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            });
            
            dialog.show();
        } else {
            long id = bookmarkManager.addBookmark(title, url);
            if (id > 0) {
                Toast.makeText(this, "已添加书签: " + title, Toast.LENGTH_SHORT).show();
            } else {
                if (bookmarkManager.isBookmarked(url)) {
                    Toast.makeText(this, "书签已存在，请长按主页按钮管理", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "添加书签失败，请检查数据库", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void openBookmarksList() {
        Intent intent = new Intent(this, BookmarksActivity.class);
        startActivityForResult(intent, REQUEST_BOOKMARKS);
    }
    
    private void openHistoryList() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivityForResult(intent, REQUEST_HISTORY);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SCAN_QR && resultCode == RESULT_OK && data != null) {
            String result = data.getStringExtra(QrScanActivity.EXTRA_SCAN_RESULT);
            if (!TextUtils.isEmpty(result)) {
                String finalUrl = processUrl(result);
                exitSearchMode();
                if (editSearch != null) {
                    editSearch.setText(finalUrl);
                    editSearch.setSelection(finalUrl.length());
                }
                if (tabManager.getCurrentTab() == null) {
                    tabManager.createNewTab(finalUrl);
                } else {
                    tabManager.loadUrl(finalUrl);
                }
                Toast.makeText(this, "扫码成功，正在打开", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (requestCode == 9002 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception ignored) {
            }
            String path = uri.toString();
            if (currentQuickLinkImagePathEdit != null) {
                currentQuickLinkImagePathEdit.setText(path);
                currentQuickLinkImagePathEdit.setSelection(path.length());
                Toast.makeText(this, "已选择图片", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (resultCode == RESULT_OK && data != null) {
            String url = data.getStringExtra("url");
            if (url != null && !url.isEmpty()) {
                tabManager.loadUrl(url);
            }
        }
        
        // 书签列表：在底部菜单中的书签按钮也改为添加当前页面
        if (requestCode == REQUEST_BOOKMARKS && resultCode != RESULT_OK) {
            // 不做特殊处理
        }
    }
    
    private void animateBackToHome() {
        TabManager.Tab tab = tabManager.getCurrentTab();
        final String homepage = prefs.getHomepage();

        pendingHomeRevealAnimation = "about:home".equals(homepage);

        if (tab == null || tab.engine == null) {
            if (!handleExternalScheme(homepage)) {
                tabManager.loadUrl(homepage);
            }
            return;
        }

        final View engineView = tab.engine.getView();

        engineView.animate().cancel();
        engineView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        engineView.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .alpha(0.0f)
                .setDuration(140)
                .withEndAction(() -> {
                    engineView.animate().cancel();
                    engineView.setScaleX(1f);
                    engineView.setScaleY(1f);
                    engineView.setAlpha(1f);
                    engineView.setTranslationY(0f);
                    engineView.setLayerType(View.LAYER_TYPE_NONE, null);
                    if (!handleExternalScheme(homepage)) {
                        tabManager.loadUrl(homepage);
                    }
                })
                .start();
    }

    private void animateHomeReveal(GeckoEngine engine) {
        if (engine == null) return;
        View engineView = engine.getView();

        engineView.animate().cancel();
        engineView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        engineView.setAlpha(0f);
        engineView.setScaleX(1f);
        engineView.setScaleY(1f);
        engineView.setTranslationY(0f);
        engineView.animate()
                .alpha(1f)
                .setDuration(120)
                .withEndAction(() -> engineView.setLayerType(View.LAYER_TYPE_NONE, null))
                .start();
    }

    private void setupBottomBar() {
        View.OnClickListener goHomeListener = v -> {
            String currentUrl = tabManager.getCurrentUrl();
            String homepage = prefs.getHomepage();

            if ("about:home".equals(currentUrl)) {
                return;
            }

            if ("about:home".equals(homepage)) {
                animateBackToHome();
            } else if (!handleExternalScheme(homepage)) {
                tabManager.loadUrl(homepage);
            }
        };

        View.OnClickListener refreshListener = v -> {
            TabManager.Tab tab = tabManager.getCurrentTab();
            if (tab != null) {
                if (tab.isLoading) {
                    tab.engine.stopLoading();
                } else {
                    tabManager.refresh();
                }
            }
        };

        btnBack.setOnClickListener(v -> tabManager.goBack());
        btnForward.setOnClickListener(v -> tabManager.goForward());
        btnRefresh.setOnClickListener(refreshListener);
        btnHome.setOnClickListener(goHomeListener);
        btnTabs.setOnClickListener(v -> showTabSwitcher());
        btnMoreBottom.setOnClickListener(v -> showBottomMenu());

        if (btnBackPad != null) btnBackPad.setOnClickListener(v -> tabManager.goBack());
        if (btnForwardPad != null) btnForwardPad.setOnClickListener(v -> tabManager.goForward());
        if (btnHomePad != null) btnHomePad.setOnClickListener(goHomeListener);
        if (btnTabsPad != null) btnTabsPad.setOnClickListener(v -> showTabSwitcher());
        if (btnMorePad != null) btnMorePad.setOnClickListener(v -> showBottomMenu());
        
        // 长按更多按钮打开快捷链接管理
        btnMoreBottom.setOnLongClickListener(v -> {
            showQuickLinkManager();
            return true;
        });
        if (btnMorePad != null) {
            btnMorePad.setOnLongClickListener(v -> {
                showQuickLinkManager();
                return true;
            });
        }
        
        // 长按主页按钮添加书签
        btnHome.setOnLongClickListener(v -> {
            addCurrentPageBookmark();
            return true;
        });
        if (btnHomePad != null) {
            btnHomePad.setOnLongClickListener(v -> {
                addCurrentPageBookmark();
                return true;
            });
        }
    }
    
    private void showTabSwitcher() {
        if (tabManager.getTabCount() == 0) return;

        boolean usePadLikeUi = isPadMode
                || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (usePadLikeUi) {
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_tab_switcher);
            dialog.setCanceledOnTouchOutside(true);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setGravity(Gravity.TOP);
                WindowManager.LayoutParams params = window.getAttributes();
                params.width = WindowManager.LayoutParams.MATCH_PARENT;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(params);
            }

            LinearLayout tabStripContainer = dialog.findViewById(R.id.tab_strip_container);
            View tabScrollSwitcher = dialog.findViewById(R.id.tab_scroll_switcher);
            View tabListScrollSwitcher = dialog.findViewById(R.id.tab_list_scroll_switcher);
            if (tabScrollSwitcher != null) {
                tabScrollSwitcher.setVisibility(View.VISIBLE);
            }
            if (tabListScrollSwitcher != null) {
                tabListScrollSwitcher.setVisibility(View.GONE);
            }
            if (tabStripContainer != null) {
                tabStripContainer.setOrientation(LinearLayout.HORIZONTAL);
                tabStripContainer.removeAllViews();

                for (int i = 0; i < tabManager.getTabCount(); i++) {
                    final int index = i;
                    TabManager.Tab tab = tabManager.getTabs().get(i);
                    String title = getDisplayTitle(tab.title, tab.url);
                    if (title.length() > 18) {
                        title = title.substring(0, 16) + "...";
                    }

                    LinearLayout item = new LinearLayout(this);
                    item.setOrientation(LinearLayout.HORIZONTAL);
                    item.setGravity(Gravity.CENTER_VERTICAL);
                    item.setPadding(dp(16), dp(8), dp(12), dp(8));

                    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            dp(40)
                    );
                    itemParams.rightMargin = dp(4);
                    item.setLayoutParams(itemParams);

                    boolean isCurrent = i == tabManager.getCurrentTabIndex();

                    android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                    bg.setColor(isCurrent ? Color.WHITE : Color.parseColor("#FFF1F1F1"));
                    bg.setCornerRadii(new float[]{
                            dp(10), dp(10),
                            dp(10), dp(10),
                            dp(4), dp(4),
                            dp(4), dp(4)
                    });
                    if (isCurrent) {
                        bg.setStroke(dp(1), Color.parseColor("#FFD8D8D8"));
                    }
                    item.setElevation(isCurrent ? dp(2) : 0f);
                    item.setBackground(bg);

                    TextView titleView = new TextView(this);
                    titleView.setText(title);
                    titleView.setTextSize(14);
                    titleView.setTextColor(isCurrent ? Color.parseColor("#FF111111") : Color.parseColor("#FF333333"));
                    titleView.setSingleLine(true);

                    TextView closeView = new TextView(this);
                    closeView.setText("×");
                    closeView.setTextSize(17);
                    closeView.setTextColor(isCurrent ? Color.parseColor("#FF444444") : Color.parseColor("#FF777777"));
                    closeView.setPadding(dp(10), 0, 0, 0);

                    item.addView(titleView);
                    item.addView(closeView);

                    item.setOnClickListener(v -> {
                        dialog.dismiss();
                        tabManager.switchToTab(index);
                    });

                    closeView.setOnClickListener(v -> {
                        if (tabManager.getTabCount() <= 1) {
                            // 只剩一个标签时，自动新建一个主页标签再关闭当前标签
                            tabManager.createNewTab(prefs.getHomepage());
                            tabManager.closeTab(index);
                            dialog.dismiss();
                            showTabSwitcher();
                            return;
                        }
                        tabManager.closeTab(index);
                        dialog.dismiss();
                        showTabSwitcher();
                    });

                    tabStripContainer.addView(item);
                }
            }

            dialog.findViewById(R.id.btn_tab_new).setOnClickListener(v -> {
                dialog.dismiss();
                tabManager.createNewTab(prefs.getHomepage());
            });

            dialog.findViewById(R.id.btn_tab_close_current).setOnClickListener(v -> {
                dialog.dismiss();
                int current = tabManager.getCurrentTabIndex();
                if (tabManager.getTabCount() > 1) {
                    tabManager.closeTab(current);
                } else {
                    Toast.makeText(this, "至少保留一个标签", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.findViewById(R.id.btn_tab_cancel).setOnClickListener(v -> dialog.dismiss());

            dialog.show();

            View contentView = dialog.findViewById(android.R.id.content);
            View animTarget = contentView != null ? contentView : (dialog.getWindow() != null ? dialog.getWindow().getDecorView() : null);
            if (animTarget != null) {
                animTarget.setAlpha(0f);
                animTarget.setTranslationY(-dp(14));
                animTarget.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(220)
                        .start();
            }
            return;
        }

        final BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_CenX_BottomSheetDialog);
        dialog.setContentView(R.layout.dialog_tab_switcher);
        dialog.setCanceledOnTouchOutside(true);

        LinearLayout tabListContainer = dialog.findViewById(R.id.tab_list_container);
        View tabScrollSwitcher = dialog.findViewById(R.id.tab_scroll_switcher);
        View tabListScrollSwitcher = dialog.findViewById(R.id.tab_list_scroll_switcher);
        if (tabScrollSwitcher != null) {
            tabScrollSwitcher.setVisibility(View.GONE);
        }
        if (tabListScrollSwitcher != null) {
            tabListScrollSwitcher.setVisibility(View.VISIBLE);
        }

        if (tabListContainer != null) {
            tabListContainer.removeAllViews();

            for (int i = 0; i < tabManager.getTabCount(); i++) {
                final int index = i;
                TabManager.Tab tab = tabManager.getTabs().get(i);
                String title = getDisplayTitle(tab.title, tab.url);
                if (title.length() > 28) {
                    title = title.substring(0, 26) + "...";
                }

                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.HORIZONTAL);
                item.setGravity(Gravity.CENTER_VERTICAL);
                item.setPadding(dp(16), dp(12), dp(12), dp(12));

                LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                itemParams.bottomMargin = dp(8);
                item.setLayoutParams(itemParams);

                boolean isCurrent = i == tabManager.getCurrentTabIndex();

                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setColor(isCurrent ? Color.WHITE : Color.parseColor("#FFF6F6F6"));
                bg.setCornerRadius(dp(14));
                if (isCurrent) {
                    bg.setStroke(dp(1), Color.parseColor("#FFD8D8D8"));
                }
                item.setBackground(bg);

                TextView titleView = new TextView(this);
                titleView.setText(title);
                titleView.setTextSize(15);
                titleView.setTextColor(isCurrent ? Color.parseColor("#FF111111") : Color.parseColor("#FF333333"));
                titleView.setSingleLine(true);
                titleView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView closeView = new TextView(this);
                closeView.setText("关闭");
                closeView.setTextSize(14);
                closeView.setTextColor(isCurrent ? Color.parseColor("#FF666666") : Color.parseColor("#FF888888"));
                closeView.setPadding(dp(12), 0, 0, 0);

                item.addView(titleView);
                item.addView(closeView);

                item.setOnClickListener(v -> {
                    dialog.dismiss();
                    tabManager.switchToTab(index);
                });

                closeView.setOnClickListener(v -> {
                    if (tabManager.getTabCount() <= 1) {
                        // 只剩一个标签时，自动新建一个主页标签再关闭当前标签
                        tabManager.createNewTab(prefs.getHomepage());
                        tabManager.closeTab(index);
                        dialog.dismiss();
                        showTabSwitcher();
                        return;
                    }
                    tabManager.closeTab(index);
                    dialog.dismiss();
                    showTabSwitcher();
                });

                tabListContainer.addView(item);
            }
        }

        dialog.findViewById(R.id.btn_tab_new).setOnClickListener(v -> {
            dialog.dismiss();
            tabManager.createNewTab(prefs.getHomepage());
        });

        dialog.findViewById(R.id.btn_tab_close_current).setOnClickListener(v -> {
            dialog.dismiss();
            int current = tabManager.getCurrentTabIndex();
            if (tabManager.getTabCount() > 1) {
                tabManager.closeTab(current);
            } else {
                // 只剩一个标签时，自动新建一个主页标签再关闭当前标签
                tabManager.createNewTab(prefs.getHomepage());
                tabManager.closeTab(current);
                showTabSwitcher();
            }
        });

        dialog.findViewById(R.id.btn_tab_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        }

        private void showResourceSnifferDialog() {
        java.util.List<TabManager.ResourceItem> resources = tabManager.getFoundResources();
        if (resources.isEmpty()) {
            Toast.makeText(this, "当前页暂未嗅探到资源，先播放视频或等待页面加载", Toast.LENGTH_SHORT).show();
            return;
        }

        final boolean dark = isDarkMode();
        final int surfaceColor = CenXTheme.colorSurface(this);
        final int titleColor = CenXTheme.colorOnSurface(this);
        final int textColor = CenXTheme.colorOnSurface(this);
        final int subTextColor = CenXTheme.colorOnSurfaceVariant(this);
        final int dividerColor = CenXTheme.colorOutline(this);

        final BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_CenX_BottomSheetDialog);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(CenXTheme.dialogBackground(this, 24));
        root.setPadding(0, 0, 0, 8);

        View handle = new View(this);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(36), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.topMargin = dp(8);
        handleParams.bottomMargin = dp(8);
        handle.setLayoutParams(handleParams);
        handle.setBackground(CenXTheme.solidBackground(this, CenXTheme.colorOutline(this), 999));
        handle.setAlpha(0.6f);
        root.addView(handle);

        TextView title = new TextView(this);
        title.setText("资源嗅探 (" + resources.size() + ")");
        title.setTextColor(titleColor);
        title.setTextSize(18);
        title.setPadding(dp(20), dp(4), dp(20), dp(8));
        root.addView(title);

        ListView listView = new ListView(this);
        listView.setBackgroundColor(Color.TRANSPARENT);
        java.util.List<String> items = new java.util.ArrayList<>();
        for (TabManager.ResourceItem item : resources) {
            String label = "【" + safeText(item.type, "other") + "】" + item.url;
            if (item.mime != null && !item.mime.isEmpty()) {
                label += "\n" + item.mime;
            }
            items.add(label);
        }
        listView.setAdapter(new ResourceListAdapter(this, items, surfaceColor, textColor, subTextColor, dividerColor));
        listView.setDivider(new ColorDrawable(dividerColor));
        listView.setDividerHeight(1);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(420));
        listView.setLayoutParams(listParams);
        root.addView(listView);

        Button close = new Button(this);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        closeParams.setMargins(dp(12), dp(8), dp(12), dp(0));
        close.setLayoutParams(closeParams);
        close.setBackground(CenXTheme.solidBackground(this, CenXTheme.colorPrimary(this), 18));
        close.setText("关闭");
        close.setTextColor(Color.WHITE);
        root.addView(close);

        dialog.setContentView(root);

        listView.setOnItemClickListener((parent, view, position, id) ->
                showResourceActionDialog(resources.get(position)));
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showResourceActionDialog(TabManager.ResourceItem item) {
        final boolean previewable = isPreviewableResource(item);
        final boolean dark = isDarkMode();
        final String[] actions = previewable
                ? new String[]{"预览资源", "复制链接", "新标签打开", "下载资源", "分享链接"}
                : new String[]{"复制链接", "新标签打开", "下载资源", "分享链接"};
        final Dialog dialog = new Dialog(this, R.style.ThemeOverlay_CenX_MaterialAlertDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ListView listView = new ListView(this);
        int surfaceColor = CenXTheme.colorSurface(this);
        int textColor = CenXTheme.colorOnSurface(this);
        int dividerColor = CenXTheme.colorOutline(this);
        listView.setBackgroundColor(surfaceColor);
        listView.setDivider(new ColorDrawable(dividerColor));
        listView.setDividerHeight(1);
        listView.setAdapter(new ResourceListAdapter(this,
                java.util.Arrays.asList(actions),
                surfaceColor,
                textColor,
                textColor,
                dividerColor));
        dialog.setContentView(listView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(surfaceColor));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            int actionIndex = position;
            if (previewable) {
                if (actionIndex == 0) {
                    showResourcePreviewDialog(item);
                    return;
                }
                actionIndex -= 1;
            }

            if (actionIndex == 0) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("资源链接", item.url));
                    Toast.makeText(this, "已复制资源链接", Toast.LENGTH_SHORT).show();
                }
            } else if (actionIndex == 1) {
                tabManager.createNewTab(item.url);
            } else if (actionIndex == 2) {
                tabManager.downloadResource(item.url);
            } else if (actionIndex == 3) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, item.url);
                startActivity(Intent.createChooser(intent, "分享资源链接"));
            }
        });

        dialog.show();
    }

    private boolean isPreviewableResource(TabManager.ResourceItem item) {
        if (item == null) return false;
        String type = safeText(item.type, "");
        return "image".equalsIgnoreCase(type) || "video".equalsIgnoreCase(type);
    }

    private void showResourcePreviewDialog(TabManager.ResourceItem item) {
        if (item == null || item.url == null || item.url.trim().isEmpty()) {
            Toast.makeText(this, "资源链接无效", Toast.LENGTH_SHORT).show();
            return;
        }

        final boolean dark = isDarkMode();
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(CenXTheme.colorSurface(this));

        if ("image".equalsIgnoreCase(safeText(item.type, ""))) {
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            root.addView(imageView);

            new Thread(() -> {
                try {
                    java.io.InputStream input = new java.net.URL(item.url).openStream();
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);
                    if (input != null) input.close();
                    runOnUiThread(() -> {
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                        } else {
                            Toast.makeText(this, "图片预览失败", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
            }).start();
        } else {
            VideoView videoView = new VideoView(this);
            videoView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            root.addView(videoView);

            android.widget.MediaController controller = new android.widget.MediaController(this);
            controller.setAnchorView(videoView);
            videoView.setMediaController(controller);
            videoView.setVideoURI(android.net.Uri.parse(item.url));
            videoView.setOnPreparedListener(mp -> videoView.start());
            videoView.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "视频预览失败，可尝试新标签打开", Toast.LENGTH_SHORT).show();
                return false;
            });
        }

        TextView close = new TextView(this);
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.topMargin = dp(20);
        closeParams.rightMargin = dp(20);
        close.setLayoutParams(closeParams);
        close.setText("关闭");
        close.setTextColor(Color.WHITE);
        close.setTextSize(16);
        close.setPadding(dp(12), dp(8), dp(12), dp(8));
        close.setBackground(CenXTheme.solidBackground(this, CenXTheme.colorSurfaceContainerHigh(this), 18));
        close.setOnClickListener(v -> dialog.dismiss());
        root.addView(close);

        dialog.setContentView(root);
        dialog.show();
    }

    private void registerNetworkReceiver() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }

    boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private static class ResourceListAdapter extends ArrayAdapter<String> {
        private final int surfaceColor;
        private final int textColor;
        private final int subTextColor;
        private final int dividerColor;

        ResourceListAdapter(Context context, java.util.List<String> items, int surfaceColor, int textColor, int subTextColor, int dividerColor) {
            super(context, android.R.layout.simple_list_item_1, items);
            this.surfaceColor = surfaceColor;
            this.textColor = textColor;
            this.subTextColor = subTextColor;
            this.dividerColor = dividerColor;
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundColor(surfaceColor);
            row.setPadding(dpStatic(getContext(), 18), dpStatic(getContext(), 14), dpStatic(getContext(), 18), dpStatic(getContext(), 14));

            TextView textView = new TextView(getContext());
            textView.setText(getItem(position));
            textView.setTextColor(textColor);
            textView.setTextSize(15);
            textView.setLineSpacing(0, 1.15f);
            row.addView(textView);

            return row;
        }

        private static int dpStatic(Context context, int value) {
            return (int) (value * context.getResources().getDisplayMetrics().density);
        }
    }

    private String safeText(String text, String fallback) {
        return text == null || text.trim().isEmpty() ? fallback : text.trim();
    }

    int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private String processUrl(String input) {
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input;
        }
        if (input.contains(".") && !input.contains(" ")) {
            return "https://" + input;
        }
        try {
            String encoded = URLEncoder.encode(input, "UTF-8");
            int searchEngine = prefs.getSearchEngine();
            return String.format(prefs.getSearchEngineUrl(searchEngine), encoded);
        } catch (Exception e) {
            return "https://www.baidu.com/s?wd=" + input;
        }
    }
    
    @Override
    public void onBackPressed() {
        if (isSearchMode) {
            exitSearchMode();
            return;
        }
        
        TabManager.Tab tab = tabManager.getCurrentTab();
if (tab != null && tab.engine.canGoBack()) {
                    tab.engine.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    public void onTabChanged(int index, TabManager.Tab tab) {
        webviewContainer.removeAllViews();
        webviewContainer.addView(tab.engine.getView());
        
        tvTitle.setText(getDisplayTitle(tab.title, tab.url));
        
        updateTabBar();
        
        btnBack.setEnabled(tab.engine.canGoBack());
        btnForward.setEnabled(tab.engine.canGoForward());
        applyBarsColorForEngine(tab.engine, tab.url);
        updateHomeBackground(tab.url);

        // 在当前页面注入长按检测脚本
        if (tab.engine != null && tab.url != null && !tab.url.isEmpty() && !"about:blank".equals(tab.url)) {
            injectLongPressScript(tab.engine);
        }
    }
    
    @Override
    public void onTabCountChanged(int count) {
        updateTabBar();
    }
    
    @Override
    public void onPageTitleChanged(int index, String title) {
        if (index == tabManager.getCurrentTabIndex() && !isSearchMode) {
            TabManager.Tab currentTab = tabManager.getCurrentTab();
            String currentUrl = currentTab != null ? currentTab.url : null;
            tvTitle.setText(getDisplayTitle(title, currentUrl));
        }
        updateTabBar();
    }
    
    @Override
    public void onPageProgressChanged(int index, int progress) {
        if (index == tabManager.getCurrentTabIndex()) {
            if (progress < 100 && progress > 0) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(progress);
            } else {
                progressBar.setVisibility(View.GONE);
            }
            
            TabManager.Tab currentTab = tabManager.getCurrentTab();
            if (currentTab != null) {
                if (progress < 100) {
                    btnRefresh.setImageResource(R.drawable.unstart);
                } else {
                    btnRefresh.setImageResource(R.drawable.restart);
                    String currentUrl = tabManager.getCurrentUrl();
                }
        btnBack.setEnabled(currentTab.engine.canGoBack());
                    btnForward.setEnabled(currentTab.engine.canGoForward());
            }
        }
    }
    
    @Override
    public void onPageFinished(int index, String title, String url) {
        // 无痕模式不保存历史记录
if (url != null && !url.isEmpty() && !"about:blank".equals(url)) {
if (!prefs.isIncognitoMode()) {
try {
historyManager.addHistory(title, url);
} catch (Exception e) {
e.printStackTrace();
}
}
}

        // 只在当前标签页加载完成时注入长按检测脚本
        if (index == tabManager.getCurrentTabIndex()) {
            TabManager.Tab tab = tabManager.getCurrentTab();
            if (tab != null && tab.engine != null) {
                injectLongPressScript(tab.engine);
            // 主页时把 body 背景设透明，让背景图透出
            if ("about:home".equals(url) || "about:blank".equals(url)) {
                tab.engine.evaluateJavascript(
                    "try{document.body.style.background='transparent';" +
                    "document.documentElement.style.background='transparent';" +
                    "var s=document.querySelector('.search-shell');" +
                    "if(s)s.style.background='transparent';" +
                    "}catch(e){}", null);
            }
            applyBarsColorForEngine(tab.engine, url);
            updateHomeBackground(url);
                if (pendingHomeRevealAnimation && "about:home".equals(url)) {
                    pendingHomeRevealAnimation = false;
                    tab.engine.getView().post(() -> animateHomeReveal(tab.engine));
                }
            }
        }
    }

    @Override
    public void onShowCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback customViewCallback) {
        if (fullscreenVideoContainer == null || fullscreenVideoView != null) return;

        // 保存视频视图、回调并添加到全屏容器
        fullscreenVideoView = view;
        fullscreenVideoCallback = customViewCallback;
        fullscreenVideoContainer.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        fullscreenVideoContainer.setVisibility(View.VISIBLE);

        // 全屏模式下隐藏系统状态栏和导航栏
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
    }

    @Override
    public void onHideCustomView() {
        if (fullscreenVideoContainer == null || fullscreenVideoView == null) return;

        // 通知 WebView 全屏已退出（关键！）
        if (fullscreenVideoCallback != null) {
            fullscreenVideoCallback.onCustomViewHidden();
            fullscreenVideoCallback = null;
        }

        // 移除视频视图并隐藏容器
        fullscreenVideoContainer.removeView(fullscreenVideoView);
        fullscreenVideoView = null;
        fullscreenVideoContainer.setVisibility(View.GONE);

        // 恢复系统状态栏（清除全屏标志）
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    /**
     * 注入长按文本选择弹窗脚本
     * 使用JS+原生WebView禁用ActionMode组合方式
     */
    private void applyBarsColorForEngine(GeckoEngine engine, String url) {
        // 背景颜色由 updateHomeBackground 统一管理，此处仅保留方法签名用于后续扩展
    }

    public static class JsTextSelectInterface {
        private final MainActivity activity;

        public JsTextSelectInterface(MainActivity activity) {
            this.activity = activity;
        }

        @JavascriptInterface
        public void onTextSelected(String text) {
            if (activity == null || activity.isFinishing()) return;
            activity.runOnUiThread(() -> {
                if (text != null && !text.trim().isEmpty()) {
                    Toast.makeText(activity, text.trim(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void injectLongPressScript(final GeckoEngine engine) {
        if (engine == null) return;

        // 通过 evaluateJavascript 注入JS实现长按检测
        engine.evaluateJavascript(
            "(function(){" +
            "  if(window.__longPressInjected) return;" +
            "  window.__longPressInjected = true;" +
            "  var timeout;" +
            "  document.addEventListener('touchstart', function(e){" +
            "    timeout = setTimeout(function(){" +
            "      var sel = window.getSelection();" +
            "      if(sel && sel.toString().trim()){" +
            "        window.Android.onTextSelected(sel.toString().trim());" +
            "      }" +
            "    }, 800);" +
            "  });" +
            "  document.addEventListener('touchend', function(){ clearTimeout(timeout); });" +
            "  document.addEventListener('touchmove', function(){ clearTimeout(timeout); });" +
            "})();"
        );

    }

    /**
     * 显示长按文本选择弹窗（粘贴、全选、复制、分享网页、询问AI）
     * 使用自定义布局，底部弹出样式
     */
    private void showTextSelectionDialog(final String selectedText) {
        if (selectedText == null || selectedText.trim().isEmpty()) return;
        aiKit.textSelectMenu().showWithSelectedText(this, selectedText, (mode, selected) -> {
            String question = mode == AiCoreLogic.MODE_WEB_QA ? selected : "";
            runAiOnCurrentPage(mode, question, true);
        });
    }

    /**
     * 转义字符串使其安全嵌入JS字符串字面量（单引号包裹）
     */
    private String escapeJsString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 分享当前页面
     */
    private void shareCurrentPage() {
        TabManager.Tab tab = tabManager.getCurrentTab();
        if (tab == null) return;
		String title = getDisplayTitle(tab.title, tab.url);
		//Toast.makeText(this, tab.url.toString(), Toast.LENGTH_SHORT).show();
          
        if ("file:///android_asset/".equals(tab.url.toString())) {
            Toast.makeText(this, "主页不可分享", Toast.LENGTH_SHORT).show();
            return;
        } else {       
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT,"我使用CenX给你分享了一个网页："+ "\n" +"标题："+title + "\n" +"网址："+ tab.url);
        startActivity(Intent.createChooser(shareIntent, "分享网页到"));
   }
    }
    /**
     * 解析AI API返回的JSON
     */
    private String parseAiResponse(String json) {
        try {
            org.json.JSONObject obj = new org.json.JSONObject(json);
            if (obj.optInt("code") == 200) {
                org.json.JSONObject data = obj.optJSONObject("data");
                if (data != null) {
                    return data.optString("answer", "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void runAiOnCurrentPage(int mode, String userQuestion) {
        runAiOnCurrentPage(mode, userQuestion, false);
    }

    void runAiOnCurrentPage(int mode, String userQuestion, boolean showInPage) {
        TabManager.Tab tab = tabManager.getCurrentTab();
        if (tab == null || tab.engine == null) {
            Toast.makeText(this, "当前页面不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        String sourceTitle = tab.title;
        String sourceUrl = tab.url;

        String aiUrl = getSharedPreferences(AiSettingsActivity.PREF_AI, MODE_PRIVATE)
                .getString(AiSettingsActivity.KEY_API_URL, "");
        String aiKey = getSharedPreferences(AiSettingsActivity.PREF_AI, MODE_PRIVATE)
                .getString(AiSettingsActivity.KEY_API_KEY, "");
        String aiModel = getSharedPreferences(AiSettingsActivity.PREF_AI, MODE_PRIVATE)
                .getString(AiSettingsActivity.KEY_API_MODEL, "gpt-4o-mini");

        if (TextUtils.isEmpty(aiUrl) || TextUtils.isEmpty(aiModel)) {
            Toast.makeText(this, "请先配置 AI 接口", Toast.LENGTH_SHORT).show();
            openAiSettings();
            return;
        }

        Toast.makeText(this, "AI处理中...", Toast.LENGTH_SHORT).show();
        String pageUrl = tab.engine.getUrl();
        aiKit.runPageAi(pageUrl, mode, aiUrl, aiKey, aiModel, userQuestion, new AiBrowserKit.AiResultCallback() {
            @Override
            public void onResult(String text) {
                String title = aiKit.coreLogic().modeName(mode);
                showAiResultDialog(title, text);
            }

            @Override
            public void onError(String error) {
                showAiResultDialog("AI错误", error == null ? "未知错误" : error);
            }
        });
    }

    // AI 页内翻译相关代码已全部删除

    private void showAiResultDialog(String title, String result) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_CenX_BottomSheetDialog);
        dialog.setContentView(R.layout.dialog_text_selection);
        dialog.setCanceledOnTouchOutside(true);

        WebView tvSelectedText = dialog.findViewById(R.id.tv_selected_text);
        TextView btnPaste = dialog.findViewById(R.id.btn_paste);
        TextView btnShare = dialog.findViewById(R.id.btn_share_page);
        Button btnCancel = dialog.findViewById(R.id.dialog_btn_cancel);

        if (tvSelectedText != null) {
            tvSelectedText.setBackgroundColor(Color.TRANSPARENT);
            tvSelectedText.getSettings().setJavaScriptEnabled(true);
            tvSelectedText.getSettings().setBuiltInZoomControls(false);
            tvSelectedText.getSettings().setDisplayZoomControls(false);
            tvSelectedText.getSettings().setDomStorageEnabled(false);
            tvSelectedText.addJavascriptInterface(new AiMarkdownBridge(), "AiBridge");
            String markdownText = (title == null ? "AI结果" : title) + "\n\n" + (result == null ? "" : result);
            boolean dark = isDarkMode();
            String textColor = dark ? "#E6EAF2" : "#111111";
            String subBg = dark ? "#23262D" : "#F3F4F6";
            String quoteBg = dark ? "#1F2430" : "#F8F9FB";
            String quoteBorder = dark ? "#4B5563" : "#D0D7DE";
            String linkColor = dark ? "#8AB4F8" : "#2563EB";

            String html = "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                    + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                    + "<style>"
                    + "body{margin:0;padding:0 8px 0 8px;background:transparent;color:" + textColor + ";font-family:-apple-system,BlinkMacSystemFont,sans-serif;}"
                    + "h1,h2,h3{margin:16px 0 10px 0;line-height:1.35;}"
                    + "p,div,li,blockquote{font-size:16px;line-height:1.8;}"
                    + "code{background:" + subBg + ";padding:2px 6px;border-radius:6px;font-family:monospace;}"
                    + ".code-wrap{position:relative;margin:12px 0;}"
                    + ".copy-btn{position:absolute;top:8px;right:8px;border:none;border-radius:8px;padding:6px 10px;background:" + quoteBorder + ";color:" + textColor + ";font-size:12px;}"
                    + "pre{background:" + subBg + ";padding:12px;border-radius:12px;overflow:auto;white-space:pre-wrap;word-break:break-word;}"
                    + "blockquote{margin:12px 0;padding:8px 12px;border-left:4px solid " + quoteBorder + ";background:" + quoteBg + ";border-radius:8px;}"
                    + "a{color:" + linkColor + ";text-decoration:none;}"
                    + "strong{font-weight:700;}"
                    + "em{font-style:italic;}"
                    + "</style>"
                    + "<script>"
                    + "function copyCode(btn){"
                    + "var code=btn.parentElement.querySelector('code');"
                    + "if(!code)return;"
                    + "var text=code.innerText||code.textContent||'';"
                    + "if(window.AiBridge&&window.AiBridge.copyText){window.AiBridge.copyText(text);btn.innerText='已复制';setTimeout(function(){btn.innerText='复制';},1200);return;}"
                    + "if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(text);btn.innerText='已复制';setTimeout(function(){btn.innerText='复制';},1200);}"
                    + "}"
                    + "window.onload=function(){"
                    + "document.querySelectorAll('pre').forEach(function(pre){"
                    + "var wrap=document.createElement('div');wrap.className='code-wrap';"
                    + "pre.parentNode.insertBefore(wrap,pre);wrap.appendChild(pre);"
                    + "var btn=document.createElement('button');btn.className='copy-btn';btn.innerText='复制';btn.onclick=function(){copyCode(btn);};"
                    + "wrap.appendChild(btn);"
                    + "});"
                    + "};"
                    + "</script></head><body>"
                    + renderMarkdownForAiPage(markdownText)
                    + "</body></html>";
            tvSelectedText.loadDataWithBaseURL("about:blank", html, "text/html", "UTF-8", null);
        }
        if (btnPaste != null) {
            btnPaste.setText("复制结果");
            btnPaste.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("AI结果", result == null ? "" : result));
                    Toast.makeText(MainActivity.this, "已复制", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            });
        }
        if (btnShare != null) {
            btnShare.setText("分享结果");
            btnShare.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, result == null ? "" : result);
                startActivity(Intent.createChooser(shareIntent, "分享 AI 结果"));
                dialog.dismiss();
            });
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    private void showAiResultInNewTab(String modeTitle, String question, String result, String sourceTitle, String sourceUrl) {
        int newIndex = tabManager.createNewTab("about:blank");
        TabManager.Tab aiTab = tabManager.getCurrentTab();
        if (aiTab == null || aiTab.engine == null) return;

        String safeMode = modeTitle == null ? "AI结果" : modeTitle;
        String safeQuestion = question == null ? "" : question;
        String safeResult = result == null ? "" : result;
        String safeSourceTitle = sourceTitle == null ? "" : sourceTitle;
        String safeSourceUrl = sourceUrl == null ? "" : sourceUrl;

        String html =
                "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<title>" + escapeHtmlForAiPage(safeMode) + "</title>" +
                "<style>" +
                "body{margin:0;background:#f6f7fb;font-family:-apple-system,BlinkMacSystemFont,sans-serif;color:#111;}" +
                ".wrap{max-width:920px;margin:0 auto;padding:18px;}" +
                ".card{background:#fff;border-radius:18px;padding:18px 16px;box-shadow:0 8px 28px rgba(0,0,0,.08);margin-bottom:14px;}" +
                ".tag{display:inline-block;background:#111;color:#fff;border-radius:999px;padding:6px 10px;font-size:12px;margin-bottom:10px;}" +
                ".title{font-size:22px;font-weight:700;line-height:1.45;margin-bottom:10px;}" +
                ".sub{font-size:13px;color:#666;line-height:1.7;word-break:break-all;}" +
                ".answer{font-size:15px;line-height:1.9;white-space:pre-wrap;word-break:break-word;margin-top:10px;}" +
                ".bar{display:flex;flex-wrap:wrap;gap:10px;margin-top:16px;}" +
                ".btn{display:inline-block;padding:10px 14px;border-radius:12px;background:#111;color:#fff;text-decoration:none;font-size:14px;}" +
                ".btn.light{background:#eef1f6;color:#111;}" +
                ".input{width:100%;box-sizing:border-box;border:none;outline:none;background:#f5f7fb;border-radius:14px;padding:14px 14px;font-size:15px;color:#111;margin-top:12px;}" +
                "</style></head><body><div class='wrap'>" +
                "<div class='card'>" +
                "<div class='tag'>CenX AI · " + escapeHtmlForAiPage(safeMode) + "</div>" +
                "<div class='title'>" + escapeHtmlForAiPage(TextUtils.isEmpty(safeQuestion) ? safeMode : safeQuestion) + "</div>" +
                "<div class='sub'>来源网页：" + escapeHtmlForAiPage(safeSourceTitle) + "<br>" + escapeHtmlForAiPage(safeSourceUrl) + "</div>" +
                "<div class='answer'>" + renderMarkdownForAiPage(safeResult) + "</div>" +
                "</div>" +
                "<div class='card'>" +
                "<div class='tag'>继续操作</div>" +
                "<div class='bar'>" +
                "<a class='btn light' href='" + escapeHtmlForAiPage(safeSourceUrl) + "'>返回原网页</a>" +
                "<span class='btn'>复制结果</span>" +
                "<span class='btn'>分享结果</span>" +
                "</div>" +
                "</div>" +
                "</div></body></html>";

        aiTab.engine.loadDataWithBaseURL("about:blank", html, "text/html", "UTF-8", null);
        aiTab.title = safeMode;
        aiTab.url = "about:ai-result";
        tvTitle.setText(safeMode);
        tabManager.switchToTab(newIndex);
    }

    private String escapeHtmlForAiPage(String text) {
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

    private String renderMarkdownForAiPage(String text) {
        if (text == null || text.isEmpty()) return "";
        String html = escapeHtmlForAiPage(text);

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

    private void openAiSettings() {
        startActivity(new Intent(this, AiSettingsActivity.class));
    }
    
    private void updateTabBar() {
        tabContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < tabManager.getTabCount(); i++) {
            final int index = i;
            TabManager.Tab tab = tabManager.getTabs().get(i);

            View tabView = inflater.inflate(R.layout.item_tab, tabContainer, false);
            TextView tabTitle = tabView.findViewById(R.id.tab_title);
            ImageButton tabClose = tabView.findViewById(R.id.tab_close);

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) tabView.getLayoutParams();
            if (lp != null) {
                lp.setMargins(0, 0, dp(6), 0);
                tabView.setLayoutParams(lp);
            }

            String displayTitle = tab.title != null && !tab.title.isEmpty() ? tab.title : "新标签";
            if (displayTitle.length() > 10) {
                displayTitle = displayTitle.substring(0, 8) + "...";
            }
            tabTitle.setText(displayTitle);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadii(new float[]{dp(10), dp(10), dp(10), dp(10), dp(4), dp(4), dp(4), dp(4)});
            if (i == tabManager.getCurrentTabIndex()) {
                bg.setColor(Color.WHITE);
                bg.setStroke(dp(1), Color.parseColor("#FFD8D8D8"));
                tabTitle.setTextColor(Color.parseColor("#FF2E6BFF"));
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    tabView.setElevation(dp(1));
                }
            } else {
                bg.setColor(Color.parseColor("#FFF1F1F1"));
                tabTitle.setTextColor(getResources().getColor(R.color.text_secondary));
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    tabView.setElevation(0f);
                }
            }

            tabTitle.setBackground(bg);
            tabTitle.setPadding(dp(16), dp(8), dp(12), dp(8));
            tabClose.setColorFilter(i == tabManager.getCurrentTabIndex()
                ? Color.parseColor("#FF5F6368")
                : Color.parseColor("#FF9AA0A6"));

            tabTitle.setOnClickListener(v -> tabManager.switchToTab(index));
            tabClose.setOnClickListener(v -> {
                tabManager.closeTab(index);
                if (tabManager.getTabCount() == 0) {
                    tabManager.createNewTab(prefs.getHomepage());
                }
            });

            tabContainer.addView(tabView);
        }

        tabScroll.post(() -> tabScroll.fullScroll(View.FOCUS_RIGHT));
    }
    
    private void openAiChat() {
        startActivity(new Intent(this, AiChatActivity.class));
    }
    
    private void openSourceView() {
        TabManager.Tab currentTab = tabManager.getCurrentTab();
        if (currentTab != null) {
            Intent intent = new Intent(this, SourceViewActivity.class);
            intent.putExtra("url", currentTab.url);
            startActivity(intent);
        }
    }
    
    private void openToolbox() {
        startActivity(new Intent(this, ToolboxActivity.class));
    }
    
    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    // ===== 快捷链接管理（长按菜单键） =====

    /**
     * 显示快捷链接管理列表弹窗
     */
    private void showQuickLinkManager() {
        final java.util.List<PreferencesManager.QuickLink> links = prefs.getQuickLinks();
        if (links == null) return;

        final BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_CenX_BottomSheetDialog);
        dialog.setContentView(R.layout.dialog_quick_link_list);
        dialog.setCanceledOnTouchOutside(true);

        final String[] names = new String[links.size()];
        for (int i = 0; i < links.size(); i++) {
            names[i] = links.get(i).name + "  (" + links.get(i).url + ")";
        }

        ListView listView = dialog.findViewById(R.id.quick_link_list);
        if (listView != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, names);
            listView.setAdapter(adapter);
            final java.util.List<PreferencesManager.QuickLink> finalLinks = links;
            listView.setOnItemClickListener((parent, view, position, id) -> {
                dialog.dismiss();
                showQuickLinkEditor(finalLinks.get(position), position);
            });
        }

        dialog.findViewById(R.id.btn_add_quick_link).setOnClickListener(v -> {
            dialog.dismiss();
            showQuickLinkEditor(null, -1);
        });

        dialog.findViewById(R.id.btn_close_quick_links).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * 显示快捷链接编辑弹窗
     */
    private void showQuickLinkEditor(final PreferencesManager.QuickLink existing, final int editIndex) {
        final BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_CenX_BottomSheetDialog);
        dialog.setContentView(R.layout.dialog_quick_link_editor);
        dialog.setCanceledOnTouchOutside(true);

        final EditText editName = dialog.findViewById(R.id.edit_link_name);
        final EditText editUrl = dialog.findViewById(R.id.edit_link_url);
        final RadioGroup iconTypeGroup = dialog.findViewById(R.id.icon_type_group);
        final View letterPanel = dialog.findViewById(R.id.letter_icon_panel);
        final View imagePanel = dialog.findViewById(R.id.image_icon_panel);
        final EditText editIcon = dialog.findViewById(R.id.edit_link_icon);
        final EditText editColor = dialog.findViewById(R.id.edit_link_color);
        final View colorPreview = dialog.findViewById(R.id.color_preview);
        final EditText editImagePath = dialog.findViewById(R.id.edit_image_path);
        currentQuickLinkImagePathEdit = editImagePath;
        final Button btnBrowseImage = dialog.findViewById(R.id.btn_browse_image);
        final Button btnDelete = dialog.findViewById(R.id.btn_delete_link);
        final Button btnSave = dialog.findViewById(R.id.btn_save_link);
        final Button btnCancel = dialog.findViewById(R.id.btn_cancel_link);

        final String[] currentIconType = {"letter"};

        // 颜色预览点击
        colorPreview.setOnClickListener(v -> {
            String colorStr = editColor.getText().toString().trim();
            try {
                int c = Color.parseColor(colorStr);
                colorPreview.setBackgroundColor(c);
            } catch (Exception ignored) {
                Toast.makeText(this, "颜色格式无效，使用 #RRGGBB 格式", Toast.LENGTH_SHORT).show();
            }
        });

        // 图标类型切换
        iconTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.icon_type_image) {
                currentIconType[0] = "image";
                letterPanel.setVisibility(View.GONE);
                imagePanel.setVisibility(View.VISIBLE);
            } else {
                currentIconType[0] = "letter";
                letterPanel.setVisibility(View.VISIBLE);
                imagePanel.setVisibility(View.GONE);
            }
        });

        // 浏览图片
        btnBrowseImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            final int REQUEST_PICK_IMAGE = 9002;
            startActivityForResult(Intent.createChooser(intent, "选择图标图片"), REQUEST_PICK_IMAGE);
        });

        // 填充现有数据
        if (existing != null) {
            editName.setText(existing.name);
            editUrl.setText(existing.url);
            btnDelete.setVisibility(View.VISIBLE);
            if ("image".equals(existing.iconType) && existing.imagePath != null && !existing.imagePath.isEmpty()) {
                iconTypeGroup.check(R.id.icon_type_image);
                editImagePath.setText(existing.imagePath);
                currentIconType[0] = "image";
                letterPanel.setVisibility(View.GONE);
                imagePanel.setVisibility(View.VISIBLE);
            } else {
                iconTypeGroup.check(R.id.icon_type_letter);
                editIcon.setText(existing.icon);
                if (existing.iconColor != null && !existing.iconColor.isEmpty()) {
                    editColor.setText(existing.iconColor);
                    try {
                        colorPreview.setBackgroundColor(Color.parseColor(existing.iconColor));
                    } catch (Exception ignored) {}
                }
                currentIconType[0] = "letter";
                letterPanel.setVisibility(View.VISIBLE);
                imagePanel.setVisibility(View.GONE);
            }
        } else {
            iconTypeGroup.check(R.id.icon_type_letter);
            currentIconType[0] = "letter";
            letterPanel.setVisibility(View.VISIBLE);
            imagePanel.setVisibility(View.GONE);
        }

        btnDelete.setOnClickListener(v -> {
            java.util.List<PreferencesManager.QuickLink> allLinks = prefs.getQuickLinks();
            if (editIndex >= 0 && editIndex < allLinks.size()) {
                allLinks.remove(editIndex);
                prefs.setQuickLinks(allLinks);
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                showQuickLinkManager();
            }
        });

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String url = editUrl.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "请输入名称", Toast.LENGTH_SHORT).show();
                return;
            }
            if (url.isEmpty()) {
                Toast.makeText(this, "请输入网址", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            PreferencesManager.QuickLink newLink;
            if ("image".equals(currentIconType[0])) {
                String imagePath = editImagePath.getText().toString().trim();
                if (imagePath.isEmpty()) {
                    Toast.makeText(this, "请选择或输入图片路径", Toast.LENGTH_SHORT).show();
                    return;
                }
                newLink = new PreferencesManager.QuickLink(name, url, "", "image", "", imagePath);
            } else {
                String icon = editIcon.getText().toString().trim();
                if (icon.isEmpty()) {
                    icon = name.substring(0, 1).toUpperCase();
                }
                String colorStr = editColor.getText().toString().trim();
                if (!colorStr.isEmpty()) {
                    if (!colorStr.startsWith("#")) colorStr = "#" + colorStr;
                    try { Color.parseColor(colorStr); } catch (Exception e) { colorStr = ""; }
                }
                newLink = new PreferencesManager.QuickLink(name, url, icon, "letter", colorStr, "");
            }

            java.util.List<PreferencesManager.QuickLink> allLinks = prefs.getQuickLinks();
            if (editIndex >= 0 && editIndex < allLinks.size()) {
                allLinks.set(editIndex, newLink);
            } else {
                allLinks.add(newLink);
            }
            prefs.setQuickLinks(allLinks);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            showQuickLinkManager();
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            showQuickLinkManager();
        });

        dialog.show();
    }

    // ===== 网络状态相关 =====

    /**
     * 检测当前网络是否可用
     */
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        } catch (Exception ignored) {}
        return false;
    }
    

    @Override
protected void onDestroy() {
if (currentInstance == this) {
currentInstance = null;
}
super.onDestroy();
try {
unregisterReceiver(networkReceiver);
} catch (Exception ignored) {}
if (tabManager != null) {
tabManager.destroy();
}
}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        isPadMode = (newConfig.smallestScreenWidthDp >= 720);
        if (!isFullscreenMode) {
            applyAdaptiveBars();
        }
    }
    
}