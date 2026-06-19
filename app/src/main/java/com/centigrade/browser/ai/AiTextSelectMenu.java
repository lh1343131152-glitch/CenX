package com.centigrade.browser.ai;

import android.app.AlertDialog;
import android.content.Context;
import android.webkit.WebView;

public class AiTextSelectMenu {

    public interface MenuActionCallback {
        void onAction(int mode, String selectedText);
    }

    private final AiWebTextGetter textGetter = new AiWebTextGetter();

    public void show(Context context, WebView webView, MenuActionCallback callback) {
        if (context == null || webView == null) return;
        textGetter.getSelectedText(webView, text -> showWithSelectedText(context, text, callback));
    }

    public void showWithSelectedText(Context context, String selectedText, MenuActionCallback callback) {
        if (context == null) return;
        final String selected = selectedText == null ? "" : selectedText.trim();
        if (selected.isEmpty()) return;

        String[] items = new String[]{
                "总结这段",
                "翻译这段",
                "解释这段",
                "基于这段提问"
        };

        new AlertDialog.Builder(context)
                .setTitle(selected.length() > 40 ? selected.substring(0, 40) + "..." : selected)
                .setItems(items, (dialog, which) -> {
                    int mode;
                    switch (which) {
                        case 0:
                            mode = AiCoreLogic.MODE_SUMMARY;
                            break;
                        case 1:
                            mode = AiCoreLogic.MODE_TRANSLATE;
                            break;
                        case 2:
                            mode = AiCoreLogic.MODE_EXPLAIN;
                            break;
                        default:
                            mode = AiCoreLogic.MODE_WEB_QA;
                            break;
                    }
                    if (callback != null) callback.onAction(mode, selected);
                })
                .show();
    }
}