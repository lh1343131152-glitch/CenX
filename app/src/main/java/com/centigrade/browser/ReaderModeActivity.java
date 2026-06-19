package com.centigrade.browser;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ReaderModeActivity extends AppCompatActivity {

    private LinearLayout readerRoot;
    private View readerTopBar;
    private ScrollView readerScroll;
    private TextView tvTitle;
    private TextView tvContent;
    private ImageButton btnBack;
    private ImageButton btnTheme;

    private int currentThemeIndex = 0;

    private static class ReaderTheme {
        final String name;
        final String bgColor;
        final String titleColor;
        final String contentColor;
        final String iconColor;

        ReaderTheme(String name, String bgColor, String titleColor, String contentColor, String iconColor) {
            this.name = name;
            this.bgColor = bgColor;
            this.titleColor = titleColor;
            this.contentColor = contentColor;
            this.iconColor = iconColor;
        }
    }

    private final ReaderTheme[] themes = new ReaderTheme[] {
        new ReaderTheme("护眼黄", "#F6EFD9", "#5C4632", "#3E2F22", "#5C4632"),
        new ReaderTheme("夜间黑", "#141414", "#E7E2D8", "#D2CBBE", "#E7E2D8"),
        new ReaderTheme("青灰纸张", "#DCE6E1", "#31433D", "#22302B", "#31433D")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader_mode);

        readerRoot = findViewById(R.id.reader_root);
        readerTopBar = findViewById(R.id.reader_top_bar);
        readerScroll = findViewById(R.id.reader_scroll);
        btnBack = findViewById(R.id.btn_reader_back);
        btnTheme = findViewById(R.id.btn_reader_theme);
        tvTitle = findViewById(R.id.tv_reader_title);
        tvContent = findViewById(R.id.tv_reader_content);

        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");

        tvTitle.setText(TextUtils.isEmpty(title) ? "阅读模式" : title);
        tvContent.setText(formatContent(content));

        applyWindowInsets();
        applyReaderTheme(currentThemeIndex);

        btnBack.setOnClickListener(v -> finish());
        btnTheme.setOnClickListener(v -> showThemePicker());
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(readerRoot, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (readerTopBar != null) {
                readerTopBar.setPadding(
                    readerTopBar.getPaddingStart(),
                    systemBars.top,
                    readerTopBar.getPaddingEnd(),
                    0
                );
            }
            if (readerScroll != null) {
                readerScroll.setPadding(
                    readerScroll.getPaddingStart(),
                    readerScroll.getPaddingTop(),
                    readerScroll.getPaddingEnd(),
                    dp(28) + systemBars.bottom
                );
            }
            return insets;
        });
    }

    private void showThemePicker() {
        String[] names = new String[themes.length];
        for (int i = 0; i < themes.length; i++) {
            names[i] = themes[i].name;
        }

        new AlertDialog.Builder(this)
            .setTitle("选择阅读主题")
            .setSingleChoiceItems(names, currentThemeIndex, (dialog, which) -> {
                currentThemeIndex = which;
                applyReaderTheme(which);
                dialog.dismiss();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void applyReaderTheme(int index) {
        ReaderTheme theme = themes[index];
        int bg = Color.parseColor(theme.bgColor);
        int title = Color.parseColor(theme.titleColor);
        int content = Color.parseColor(theme.contentColor);
        int icon = Color.parseColor(theme.iconColor);

        readerRoot.setBackgroundColor(bg);
        readerTopBar.setBackgroundColor(bg);
        readerScroll.setBackgroundColor(bg);
        tvTitle.setTextColor(title);
        tvContent.setTextColor(content);
        btnBack.setColorFilter(icon);
        btnTheme.setColorFilter(icon);

        Window window = getWindow();
        if (window != null) {
            window.setStatusBarColor(bg);
            window.setNavigationBarColor(bg);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = window.getDecorView();
                int flags = decor.getSystemUiVisibility();
                if (isLightColor(bg)) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (isLightColor(bg)) {
                        flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    } else {
                        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    }
                }
                decor.setSystemUiVisibility(flags);
            }
        }
    }

    private boolean isLightColor(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness < 0.5;
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private String formatContent(String content) {
        if (content == null) return "";
        String text = content.trim();
        text = text.replace("\r", "");
        text = text.replace('\u00A0', ' ');
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("(?i)(上一篇|下一篇|相关推荐|相关阅读|猜你喜欢|返回目录|加入书签|举报|纠错|打赏|评论区).*", "");
        text = text.replaceAll("\\n{3,}", "\n\n");
        text = text.replaceAll("([。！？；：])", "$1\n\n");
        text = text.replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n");
        text = text.replaceAll("(?m)^\\s+", "");
        text = text.replaceAll("(?m)^(.{1,18})$", "\n$1\n");
        return text.trim();
    }
}