package com.centigrade.browser;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.net.Uri;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {
    
    private PreferencesManager prefs;
    private RadioGroup uaGroup, searchGroup, homepageTypeGroup;
    private EditText homepageEdit, customSearchEdit;
    private Button btnSaveHomepage, btnSaveCustomSearch, btnClearCache, btnClearHistory, btnClearCookies, btnManageQuickLinks;
    private MaterialSwitch switchAdBlock, switchIncognito, switchResourceSniff;
    private EditText currentQuickLinkImagePathEdit;
    private View incognitoStatusView;
    
    private void applyEdgeToEdgeInsets() {
        final View root = findViewById(R.id.root_settings);
        if (root == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        CenXTheme.applyWindowColors(this);
        setContentView(R.layout.activity_settings);
        applyEdgeToEdgeInsets();
        
        prefs = PreferencesManager.getInstance(this);
        
        View backBtn = findViewById(R.id.back);
        TextView titleView = findViewById(R.id.appbar_title);
        if (titleView != null) {
            titleView.setText("设置");
        }
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
        
        setupEntryCard(R.id.card_user_agent, "用户代理", getUaSummary(), "切换移动端/桌面端 UA", UserAgentSettingsActivity.class);
        setupEntryCard(R.id.card_search_engine, "搜索引擎", getSearchSummary(), "设置默认搜索引擎", SearchEngineSettingsActivity.class);
        setupEntryCard(R.id.card_homepage_settings, "主页设置", getHomepageSummary(), "主页、快捷链接、背景图", HomepageSettingsActivity.class);
        setupEntryCard(R.id.card_browser_control_settings, "浏览器控件", getBrowserControlSummary(), "缩放、视口、Cookie、多窗口、编码", BrowserControlSettingsActivity.class);
        setupEntryCard(R.id.card_feature_settings, "功能开关", getFeatureSummary(), "无痕、广告拦截、资源嗅探", FeatureSettingsActivity.class);
        setupEntryCard(R.id.card_lab, "实验室", "站点鉴权 / Cookie Manager", "实验性功能合集", LabActivity.class);
        setupEntryCard(R.id.card_clear_data_settings, "清除数据", "缓存 / 历史 / Cookie", "清理浏览数据", ClearDataSettingsActivity.class);
        setupEntryCard(R.id.card_ai_settings, "AI 设置", "Base URL / API Key / Model", "单独管理 AI 接口配置", AiSettingsActivity.class);

        // 文件访问权限
        final CardView cardFilePermission = findViewById(R.id.card_file_permission);
        final TextView tvFilePermissionStatus = findViewById(R.id.tv_file_permission_status);
        updateFilePermissionStatus(tvFilePermissionStatus);
        cardFilePermission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!android.os.Environment.isExternalStorageManager()) {
                    // 引导用户到系统设置中授予所有文件访问权限
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        // 备选：打开系统设置页
                        Intent altIntent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivity(altIntent);
                    }
                    Toast.makeText(this, "请在设置中开启「允许管理所有文件」", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "已获得所有文件访问权限", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Android 10及以下：请求传统存储权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, 1001);
                } else {
                    Toast.makeText(this, "当前系统版本无需额外授权", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 关于浏览器
        CardView cardAbout = findViewById(R.id.card_about);
        cardAbout.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
            startActivity(intent);
        });
    }

    private void setupEntryCard(int cardId, String title, String value, String summary, Class<?> target) {
        View card = findViewById(cardId);
        if (card == null) return;
        TextView titleView = card.findViewById(R.id.tv_entry_title);
        TextView summaryView = card.findViewById(R.id.tv_entry_summary);
        TextView valueView = card.findViewById(R.id.tv_entry_value);
        if (titleView != null) titleView.setText(title);
        if (summaryView != null) summaryView.setText(summary);
        if (valueView != null) valueView.setText(value);
        card.setOnClickListener(v -> startActivity(new Intent(this, target)));
    }

    private String getUaSummary() {
        switch (prefs.getUserAgent()) {
            case 1: return "桌面";
            case 2: return "iPad";
            case 3: return "iPhone";
            default: return "默认";
        }
    }

    private String getSearchSummary() {
        switch (prefs.getSearchEngine()) {
            case 1: return "Google";
            case 2: return "Bing";
            case 3: return "搜狗";
            case 4: return "DuckDuckGo";
            case 5: return "秘塔";
            case 6: return "自定义";
            default: return "百度";
        }
    }

    private String getHomepageSummary() {
        String home = prefs.getHomepage();
        if (home == null || home.isEmpty()) return "about:home";
        return home.length() > 22 ? home.substring(0, 22) + "..." : home;
    }

    private String getFeatureSummary() {
return "无痕 " + (prefs.isIncognitoMode() ? "开" : "关")
+ " / 广告拦截 " + (prefs.isAdBlockEnabled() ? "开" : "关")
+ " / DoH " + (prefs.isDohWebViewEnabled() ? "开" : "关")
+ " / " + prefs.getRenderModeName()
+ " / " + prefs.getCacheModeName();
}

    private String getBrowserControlSummary() {
        return "缩放 " + (prefs.isSupportZoomEnabled() ? "开" : "关")
                + " / 文本 " + prefs.getTextZoom() + "%"
                + " / 视口 " + (prefs.isWideViewportEnabled() ? "宽" : "标准");
    }

    /**
     * 更新文件权限状态显示
     */   
    private void updateFilePermissionStatus(TextView statusView) {
        if (statusView == null) return;

        boolean granted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            granted = android.os.Environment.isExternalStorageManager();
        } else {
            granted = true;
        }

        statusView.setText(granted ? "已授权" : "未授权");
        int bgColor = granted
                ? CenXTheme.colorPrimary(this)
                : CenXTheme.colorSurfaceContainerHigh(this);
        int textColor = granted
                ? Color.WHITE
                : CenXTheme.colorOnSurface(this);
        statusView.setTextColor(textColor);
        statusView.setBackground(CenXTheme.solidBackground(this, bgColor, 999));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 用户从系统设置返回后刷新权限状态
        TextView statusView = findViewById(R.id.tv_file_permission_status);
        if (statusView != null) {
            updateFilePermissionStatus(statusView);
        }
    }

    /**
     * 快捷链接管理弹窗
     */
    private void showQuickLinkManager() {
        final java.util.List<PreferencesManager.QuickLink> links = prefs.getQuickLinks();
        final com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.ThemeOverlay_CenX_BottomSheetDialog);
        dialog.setContentView(R.layout.dialog_quick_link_list);
        dialog.setCanceledOnTouchOutside(true);

        // 使用简单的ListView
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
     * 快捷链接编辑弹窗
     */
    // 用于图片选择的结果回调
    private int browseImageRequestCode = -1;

    private void showQuickLinkEditor(final PreferencesManager.QuickLink existing, final int editIndex) {
        final com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.ThemeOverlay_CenX_BottomSheetDialog);
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

        // 当前图标类型（默认letter）
        final String[] currentIconType = {"letter"};

        // 颜色预览点击：用EditText的值更新预览色块
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

        // 浏览图片按钮（启动系统文件选择器）
        btnBrowseImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            final int REQUEST_PICK_IMAGE = 9001;
            browseImageRequestCode = REQUEST_PICK_IMAGE;
            startActivityForResult(Intent.createChooser(intent, "选择图标图片"), REQUEST_PICK_IMAGE);
        });

        // 填充现有数据
        if (existing != null) {
            editName.setText(existing.name);
            editUrl.setText(existing.url);
            btnDelete.setVisibility(View.VISIBLE);
            if ("image".equals(existing.iconType) && existing.imagePath != null && !existing.imagePath.isEmpty()) {
                // 图片模式
                iconTypeGroup.check(R.id.icon_type_image);
                editImagePath.setText(existing.imagePath);
                currentIconType[0] = "image";
                letterPanel.setVisibility(View.GONE);
                imagePanel.setVisibility(View.VISIBLE);
            } else {
                // 字母模式
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
            // 新建默认字母模式
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
                // 图片模式
                String imagePath = editImagePath.getText().toString().trim();
                if (imagePath.isEmpty()) {
                    Toast.makeText(this, "请选择或输入图片路径", Toast.LENGTH_SHORT).show();
                    return;
                }
                newLink = new PreferencesManager.QuickLink(name, url, "", "image", "", imagePath);
            } else {
                // 字母模式
                String icon = editIcon.getText().toString().trim();
                if (icon.isEmpty()) {
                    icon = name.substring(0, 1).toUpperCase();
                }
                String colorStr = editColor.getText().toString().trim();
                // 验证颜色格式
                if (!colorStr.isEmpty()) {
                    if (!colorStr.startsWith("#")) {
                        colorStr = "#" + colorStr;
                    }
                    try {
                        Color.parseColor(colorStr);
                    } catch (Exception e) {
                        colorStr = "";
                    }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == 9001) {
                Uri uri = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (Exception ignored) {
                }
                String path = getRealPathFromUri(uri);
                if (path == null || path.trim().isEmpty()) {
                    path = uri.toString();
                }
                if (currentQuickLinkImagePathEdit != null && path != null) {
                    currentQuickLinkImagePathEdit.setText(path);
                    currentQuickLinkImagePathEdit.setSelection(path.length());
                    Toast.makeText(this, "已选择图片", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 将Uri转为实际文件路径
     */
    private String getRealPathFromUri(Uri uri) {
        String path = null;
        String[] proj = { android.provider.MediaStore.Images.Media.DATA };
        android.database.Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA);
                path = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        if (path == null) {
            path = uri.getPath();
        }
        return path;
    }

    private void updateIncognitoStatusColor(boolean enabled) {
        if (incognitoStatusView == null) return;
        incognitoStatusView.setBackgroundColor(Color.parseColor(enabled ? "#2E7D32" : "#9E9E9E"));
        incognitoStatusView.setAlpha(enabled ? 1f : 0.6f);
    }

    private void restartBrowser() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}