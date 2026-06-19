package com.centigrade.browser;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class AiPanelController {

    private final Context context;

    public interface ActionHandler {
        void openAiChat();
        void showAiQuestionDialog(int mode, String title, String hint, String fallbackQuestion);
        void openReaderModeFromCurrentPage();
        void openAiSettings();
    }

    private final MainActivity activity;
    private final PreferencesManager prefs;
    private final ActionHandler handler;

    public AiPanelController(MainActivity activity, PreferencesManager prefs, ActionHandler handler) {
        this.activity = activity;
        this.context = activity;
        this.prefs = prefs;
        this.handler = handler;
    }

    /** 将 dp 转换为像素 */
    private int dp(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public void showAiPanel() {
        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.ThemeOverlay_CenX_BottomSheetDialog);
        dialog.setContentView(R.layout.dialog_ai_panel);
        dialog.setCanceledOnTouchOutside(true);

        View actionChat = dialog.findViewById(R.id.ai_action_chat);
        View actionSummary = dialog.findViewById(R.id.ai_action_summary);
        View actionExplain = dialog.findViewById(R.id.ai_action_explain);
        View actionReader = dialog.findViewById(R.id.ai_action_reader);
        View actionSettings = dialog.findViewById(R.id.ai_action_settings);
        Button btnClose = dialog.findViewById(R.id.btn_ai_panel_close);

        if (actionChat != null) {
            actionChat.setOnClickListener(v -> {
                dialog.dismiss();
                handler.openAiChat();
            });
        }
        if (actionSummary != null) {
            actionSummary.setOnClickListener(v -> {
                dialog.dismiss();
                handler.showAiQuestionDialog(
                        0,
                        "总结要求（可选）",
                        "例如：总结成3点、提取核心观点、适合学生理解",
                        "请简洁总结"
                );
            });
        }
        if (actionExplain != null) {
            actionExplain.setOnClickListener(v -> {
                dialog.dismiss();
                handler.showAiQuestionDialog(
                        1,
                        "解释要求（可选）",
                        "例如：用初中生能懂的话解释",
                        "请通俗解释"
                );
            });
        }
        if (actionReader != null) {
            actionReader.setOnClickListener(v -> {
                dialog.dismiss();
                handler.openReaderModeFromCurrentPage();
            });
        }
        if (actionSettings != null) {
            actionSettings.setOnClickListener(v -> {
                dialog.dismiss();
                handler.openAiSettings();
            });
        }
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    public void showAiQuestionDialog(int mode, String title, String hint, String fallbackQuestion) {
        final Dialog dialog = new Dialog(activity, R.style.ThemeOverlay_CenX_MaterialAlertDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(16));
        root.setBackground(activity.mdDialogBackground());

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(activity.mdColorOnSurface());
        tvTitle.setTypeface(null, Typeface.BOLD);
        root.addView(tvTitle);

        TextView tvHint = new TextView(activity);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hintParams.topMargin = dp(6);
        tvHint.setLayoutParams(hintParams);
        tvHint.setText("可留空，系统会使用默认要求");
        tvHint.setTextSize(13);
        tvHint.setTextColor(activity.mdColorOnSurfaceVariant());
        root.addView(tvHint);

        EditText input = new EditText(activity);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.topMargin = dp(14);
        input.setLayoutParams(inputParams);
        input.setHint(hint);
        input.setHintTextColor(activity.mdColorOnSurfaceVariant());
        input.setTextColor(activity.mdColorOnSurface());
        input.setMinLines(3);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setBackground(activity.mdInputBackground());
        input.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.addView(input);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionsParams.topMargin = dp(16);
        actions.setLayoutParams(actionsParams);
        actions.setGravity(Gravity.END);

        Button btnCancel = new Button(activity);
        btnCancel.setText("取消");
        btnCancel.setTextColor(activity.mdColorOnSurface());
        btnCancel.setBackground(activity.mdButtonBackground(false));

        Button btnOk = new Button(activity);
        btnOk.setText("开始");
        btnOk.setTextColor(activity.isDarkMode() ? Color.BLACK : Color.WHITE);
        btnOk.setBackground(activity.mdButtonBackground(true));

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(44)
        );
        btnParams.leftMargin = dp(10);
        btnCancel.setLayoutParams(btnParams);

        LinearLayout.LayoutParams btnOkParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(44)
        );
        btnOkParams.leftMargin = dp(10);
        btnOk.setLayoutParams(btnOkParams);

        actions.addView(btnCancel);
        actions.addView(btnOk);
        root.addView(actions);

        dialog.setContentView(root);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9f);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            String question = input.getText() == null ? "" : input.getText().toString().trim();
            if (question.isEmpty()) question = fallbackQuestion;
            activity.runAiOnCurrentPage(mode, question, true);
        });

        dialog.show();
    }

}