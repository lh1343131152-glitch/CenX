package com.centigrade.browser.ai;

import android.content.Context;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

public class AiBrowserKit {

    public interface AiResultCallback {
        void onResult(String text);
        void onError(String error);
    }

    private final Context context;
    private final AiWebTextGetter webTextGetter;
    private final AiSelfNet selfNet;
    private final AiSimpleJson simpleJson;
    private final AiCoreLogic coreLogic;
    private final AiPageOptimize pageOptimize;
    private final AiTextSelectMenu textSelectMenu;
    private final AiAddressSmart addressSmart;
    private final AiRemoteWebReader remoteWebReader;

    public AiBrowserKit(Context context) {
        this.context = context.getApplicationContext();
        this.webTextGetter = new AiWebTextGetter();
        this.selfNet = new AiSelfNet();
        this.simpleJson = new AiSimpleJson();
        this.coreLogic = new AiCoreLogic();
        this.pageOptimize = new AiPageOptimize();
        this.textSelectMenu = new AiTextSelectMenu();
        this.addressSmart = new AiAddressSmart();
        this.remoteWebReader = new AiRemoteWebReader();
    }

    public AiWebTextGetter webTextGetter() {
        return webTextGetter;
    }

    public AiSelfNet selfNet() {
        return selfNet;
    }

    public AiSimpleJson simpleJson() {
        return simpleJson;
    }

    public AiCoreLogic coreLogic() {
        return coreLogic;
    }

    public AiPageOptimize pageOptimize() {
        return pageOptimize;
    }

    public AiTextSelectMenu textSelectMenu() {
        return textSelectMenu;
    }

    public AiAddressSmart addressSmart() {
        return addressSmart;
    }

    public void runPageAi(String pageUrl,
                          int mode,
                          String aiUrl,
                          String aiKey,
                          String aiModel,
                          String userQuestion,
                          AiResultCallback callback) {
        if (TextUtils.isEmpty(aiUrl) || TextUtils.isEmpty(aiModel)) {
            if (callback != null) callback.onError("AI接口未配置");
            return;
        }

        if (!TextUtils.isEmpty(pageUrl) && pageUrl.startsWith("http")) {
            remoteWebReader.read(pageUrl, new AiRemoteWebReader.ReadCallback() {
                @Override
                public void onSuccess(String remoteTitle, String remoteDescription, String remoteContent) {
                    String content = !TextUtils.isEmpty(remoteContent) ? remoteContent : pageUrl;
                    sendAiRequest(mode, aiUrl, aiKey, aiModel, content, remoteTitle, remoteDescription, pageUrl, userQuestion, callback);
                }

                @Override
                public void onError(String error) {
                    if (callback != null) callback.onError("无法读取页面内容: " + error);
                }
            });
        } else {
            sendAiRequest(mode, aiUrl, aiKey, aiModel, pageUrl != null ? pageUrl : "", "", "", pageUrl, userQuestion, callback);
        }
    }

    private void sendAiRequest(int mode,
                               String aiUrl,
                               String aiKey,
                               String aiModel,
                               String source,
                               String pageTitle,
                               String pageDesc,
                               String pageUrl,
                               String userQuestion,
                               AiResultCallback callback) {
        String prompt = coreLogic.buildPrompt(mode, source, pageTitle, pageDesc, pageUrl, userQuestion);
        String body = coreLogic.buildOpenAiRequest(aiModel, prompt);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        if (!TextUtils.isEmpty(aiKey)) {
            headers.put("Authorization", "Bearer " + aiKey);
        }

        selfNet.post(aiUrl, headers, body, new AiSelfNet.NetCallback() {
            @Override
            public void onSuccess(int code, String body) {
                String text = simpleJson.parseAiContent(body);
                if (TextUtils.isEmpty(text)) text = body;
                if (callback != null) callback.onResult(text);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
}