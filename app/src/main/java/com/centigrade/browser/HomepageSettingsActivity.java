package com.centigrade.browser;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.view.View;

public class HomepageSettingsActivity extends BaseSettingsSubActivity {

    private EditText currentQuickLinkImagePathEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage_settings);
        initSubpage("主页设置");

        RadioGroup homepageTypeGroup = findViewById(R.id.homepage_type_group);
        EditText homepageEdit = findViewById(R.id.homepage_edit);
        Button btnSaveHomepage = findViewById(R.id.btn_save_homepage);
        Button btnManageQuickLinks = findViewById(R.id.btn_manage_quick_links);
        EditText editHomeBackground = findViewById(R.id.edit_home_background);
        Button btnPickHomeBackground = findViewById(R.id.btn_pick_home_background);
        Button btnSaveHomeBackground = findViewById(R.id.btn_save_home_background);

        homepageEdit.setBackground(CenXTheme.inputBackground(this, 22));
        editHomeBackground.setBackground(CenXTheme.inputBackground(this, 22));

        String homepageType = prefs.getHomepageType();
        String homepage = prefs.getHomepage();
        String homeBackground = prefs.getHomeBackground();

        if ("custom".equals(homepageType)) {
            ((RadioButton) findViewById(R.id.homepage_type_custom)).setChecked(true);
            homepageEdit.setVisibility(View.VISIBLE);
            btnSaveHomepage.setVisibility(View.VISIBLE);
        } else {
            ((RadioButton) findViewById(R.id.homepage_type_default)).setChecked(true);
            homepageEdit.setVisibility(View.GONE);
            btnSaveHomepage.setVisibility(View.GONE);
        }
        homepageEdit.setText(homepage);
        editHomeBackground.setText(homeBackground);

        homepageTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.homepage_type_custom) {
                prefs.setHomepageType("custom");
                homepageEdit.setVisibility(View.VISIBLE);
                btnSaveHomepage.setVisibility(View.VISIBLE);
                String current = prefs.getHomepage();
                if ("about:home".equals(current)) {
                    homepageEdit.setText("https://www.baidu.com");
                }
            } else {
                prefs.setHomepageType("default");
                prefs.setHomepage("about:home");
                homepageEdit.setVisibility(View.GONE);
                btnSaveHomepage.setVisibility(View.GONE);
                Toast.makeText(this, "已切换为默认首页", Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveHomepage.setOnClickListener(v -> {
            String url = homepageEdit.getText().toString().trim();
            if (url.isEmpty()) {
                url = "https://www.baidu.com";
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            prefs.setHomepage(url);
            homepageEdit.setText(url);
            Toast.makeText(this, "主页已保存: " + url, Toast.LENGTH_SHORT).show();
        });

        btnManageQuickLinks.setOnClickListener(v -> showQuickLinkManager());

        btnPickHomeBackground.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, 9101);
        });

        btnSaveHomeBackground.setOnClickListener(v -> {
            String path = editHomeBackground.getText().toString().trim();
            prefs.setHomeBackground(path);
            Toast.makeText(this, "主页背景图已保存", Toast.LENGTH_SHORT).show();
        });
    }

    private void showQuickLinkManager() {
        final java.util.List<PreferencesManager.QuickLink> links = prefs.getQuickLinks();
        final com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.ThemeOverlay_CenX_BottomSheetDialog);
        dialog.setContentView(R.layout.dialog_quick_link_list);
        dialog.setCanceledOnTouchOutside(true);

        final String[] names = new String[links.size()];
        for (int i = 0; i < links.size(); i++) {
            names[i] = links.get(i).name + "  (" + links.get(i).url + ")";
        }

        ListView listView = dialog.findViewById(R.id.quick_link_list);
        if (listView != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                dialog.dismiss();
                showQuickLinkEditor(links.get(position), position);
            });
        }

        dialog.findViewById(R.id.btn_add_quick_link).setOnClickListener(v -> {
            dialog.dismiss();
            showQuickLinkEditor(null, -1);
        });

        dialog.findViewById(R.id.btn_close_quick_links).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

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

        editName.setBackground(CenXTheme.inputBackground(this, 18));
        editUrl.setBackground(CenXTheme.inputBackground(this, 18));
        editIcon.setBackground(CenXTheme.inputBackground(this, 18));
        editColor.setBackground(CenXTheme.inputBackground(this, 18));
        editImagePath.setBackground(CenXTheme.inputBackground(this, 18));
        final Button btnBrowseImage = dialog.findViewById(R.id.btn_browse_image);
        final Button btnDelete = dialog.findViewById(R.id.btn_delete_link);
        final Button btnSave = dialog.findViewById(R.id.btn_save_link);
        final Button btnCancel = dialog.findViewById(R.id.btn_cancel_link);

        final String[] currentIconType = {"letter"};

        colorPreview.setOnClickListener(v -> {
            String colorStr = editColor.getText().toString().trim();
            try {
                int c = Color.parseColor(colorStr);
                colorPreview.setBackgroundColor(c);
            } catch (Exception ignored) {
                Toast.makeText(this, "颜色格式无效，使用 #RRGGBB 格式", Toast.LENGTH_SHORT).show();
            }
        });

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

        btnBrowseImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, 9102);
        });

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
                    } catch (Exception ignored) {
                    }
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
                if (icon.isEmpty()) icon = name.substring(0, 1).toUpperCase();
                String colorStr = editColor.getText().toString().trim();
                if (!colorStr.isEmpty()) {
                    if (!colorStr.startsWith("#")) colorStr = "#" + colorStr;
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
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }

            if (requestCode == 9101) {
                EditText editHomeBackground = findViewById(R.id.edit_home_background);
                if (editHomeBackground != null) {
                    editHomeBackground.setText(uri.toString());
                    editHomeBackground.setSelection(editHomeBackground.getText().length());
                }
            } else if (requestCode == 9102) {
                if (currentQuickLinkImagePathEdit != null) {
                    currentQuickLinkImagePathEdit.setText(uri.toString());
                    currentQuickLinkImagePathEdit.setSelection(uri.toString().length());
                    Toast.makeText(this, "已选择图片", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}