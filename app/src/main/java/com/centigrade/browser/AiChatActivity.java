package com.centigrade.browser;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.centigrade.browser.net.ResolvedConnectionFactory;
import java.util.ArrayList;
import java.util.List;

public class AiChatActivity extends AppCompatActivity {
    
    private EditText chatInput;
    private Button btnSend;
    private RecyclerView chatRecycler;
    private ChatAdapter adapter;
    private List<ChatMessage> messages;
    private SharedPreferences aiPrefs;
    
    public static class ChatMessage {
        public static final int TYPE_USER = 0;
        public static final int TYPE_AI = 1;
        
        public String content;
        public int type;
        
        public ChatMessage(String content, int type) {
            this.content = content;
            this.type = type;
        }
    }
    
    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private List<ChatMessage> messages;
        
        public ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }
        
        @Override
        public int getItemViewType(int position) {
            return messages.get(position).type;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);
            TextView tv = holder.itemView.findViewById(R.id.tv_message);
            tv.setText(msg.content);
            
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tv.getLayoutParams();
            if (msg.type == ChatMessage.TYPE_USER) {
                params.gravity = android.view.Gravity.END;
                tv.setBackgroundResource(R.drawable.chat_bubble_user);
                tv.setTextColor(getResources().getColor(R.color.button_primary_text));
            } else {
                params.gravity = android.view.Gravity.START;
                tv.setBackgroundResource(R.drawable.chat_bubble_ai);
                tv.setTextColor(getResources().getColor(R.color.text_primary));
            }
            tv.setLayoutParams(params);
        }
        
        @Override
        public int getItemCount() {
            return messages.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);
        
        findViewById(R.id.back).setOnClickListener(v -> finish());
        TextView titleView = findViewById(R.id.appbar_title);
        if (titleView != null) {
            titleView.setText("AI助手");
        }
        chatInput = findViewById(R.id.chat_input);
        btnSend = findViewById(R.id.btn_send);
        chatRecycler = findViewById(R.id.chat_recycler);
        aiPrefs = getSharedPreferences(AiSettingsActivity.PREF_AI, MODE_PRIVATE);
        
        messages = new ArrayList<>();
        messages.add(new ChatMessage("你好！我是CenX的AI助手，有什么可以帮助你的吗？", ChatMessage.TYPE_AI));
        
        adapter = new ChatAdapter(messages);
        chatRecycler.setLayoutManager(new LinearLayoutManager(this));
        chatRecycler.setAdapter(adapter);
        
        btnSend.setOnClickListener(v -> sendMessage());
        
        chatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }
    
    private void sendMessage() {
        String message = chatInput.getText().toString().trim();
        if (message.isEmpty()) return;
        
        // 添加用户消息
        messages.add(new ChatMessage(message, ChatMessage.TYPE_USER));
        adapter.notifyItemInserted(messages.size() - 1);
        chatRecycler.scrollToPosition(messages.size() - 1);
        
        chatInput.setText("");
        
        // 添加正在思考的消息
        messages.add(new ChatMessage("思考中...", ChatMessage.TYPE_AI));
        int thinkingIndex = messages.size() - 1;
        adapter.notifyItemInserted(thinkingIndex);
        chatRecycler.scrollToPosition(thinkingIndex);
        
        // 调用API
        callAiApi(message, thinkingIndex);
    }
    
    private void callAiApi(final String message, final int thinkingIndex) {
        new Thread(() -> {
            try {
                String apiUrl = aiPrefs.getString(AiSettingsActivity.KEY_API_URL, "https://api.openai.com/v1/chat/completions");
                String apiKey = aiPrefs.getString(AiSettingsActivity.KEY_API_KEY, "");
                String model = aiPrefs.getString(AiSettingsActivity.KEY_API_MODEL, "gpt-4o-mini");

                if (TextUtils.isEmpty(apiUrl) || TextUtils.isEmpty(model)) {
                    throw new IllegalStateException("请先配置 OpenAI 接口地址和模型");
                }

                URL url = new URL(apiUrl);
                HttpURLConnection conn = new ResolvedConnectionFactory(this).open(apiUrl);
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(20000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "CenX/4.0");
                if (!TextUtils.isEmpty(apiKey)) {
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                }

                JSONObject payload = new JSONObject();
                payload.put("model", model);

                org.json.JSONArray messagesArr = new org.json.JSONArray();
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", message);
                messagesArr.put(userMsg);
                payload.put("messages", messagesArr);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String result = response.toString().trim();
                if (result.isEmpty()) {
                    result = "(AI返回为空，请稍后重试)";
                } else {
                    try {
                        JSONObject json = new JSONObject(result);
                        org.json.JSONArray choices = json.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            JSONObject first = choices.optJSONObject(0);
                            if (first != null) {
                                JSONObject msgObj = first.optJSONObject("message");
                                if (msgObj != null) {
                                    String content = msgObj.optString("content", "");
                                    if (!TextUtils.isEmpty(content)) {
                                        result = content;
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                final String finalResult = result;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (thinkingIndex < messages.size()) {
                        messages.set(thinkingIndex, new ChatMessage(finalResult, ChatMessage.TYPE_AI));
                        adapter.notifyItemChanged(thinkingIndex);
                        chatRecycler.scrollToPosition(thinkingIndex);
                    }
                });

            } catch (Exception e) {
                final String errorMsg = "请求失败: " + e.getMessage();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (thinkingIndex < messages.size()) {
                        messages.set(thinkingIndex, new ChatMessage(
                                "OpenAI 响应异常: " + errorMsg + "\n\n请检查 Base URL、API Key、模型名称和网络。",
                                ChatMessage.TYPE_AI));
                        adapter.notifyItemChanged(thinkingIndex);
                        chatRecycler.scrollToPosition(thinkingIndex);
                    }
                });
            }
        }).start();
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