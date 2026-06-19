package com.centigrade.browser;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;

import com.centigrade.browser.ai.AiAddressSmart;
import com.centigrade.browser.ai.AiBrowserKit;

/**
 * 备份：原 AI 搜索逻辑。
 * 当前主流程已移除 AI 搜索，但源码保留在这里，便于后续恢复或重构。
 */
public final class AiSearchBackup {

    private AiSearchBackup() {
    }

    public static void perform(
            Context context,
            AiBrowserKit aiKit,
            PreferencesManager prefs,
            TabManager tabManager,
            TextView tvTitle,
            String input,
            String aiUrl,
            String aiKey,
            String aiModel,
            Runnable exitSearchMode
    ) {
        aiKit.addressSmart().handle(
                input,
                prefs.getSearchEngineUrl(prefs.getSearchEngine()),
                aiUrl,
                aiKey,
                aiModel,
                new AiAddressSmart.SmartCallback() {
                    @Override
                    public void onLoadUrl(String url) {
                        tabManager.loadUrl(url);
                        if (exitSearchMode != null) exitSearchMode.run();
                    }

                    @Override
                    public void onAiCard(String question, String answer) {
                        TabManager.Tab tab = tabManager.getCurrentTab();
                        if (tab == null) {
                            tabManager.createNewTab("about:blank");
                            tab = tabManager.getCurrentTab();
                        }
                        if (tab != null) {
                            aiKit.addressSmart().attachAnswerCard(tab.engine, question, answer);
                            tab.url = "about:ai";
                            tab.title = "AI回答";
                            if (tvTitle != null) {
                                tvTitle.setText("AI回答");
                            }
                        }
                        if (exitSearchMode != null) exitSearchMode.run();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                        if (exitSearchMode != null) exitSearchMode.run();
                    }
                }
        );
    }
}