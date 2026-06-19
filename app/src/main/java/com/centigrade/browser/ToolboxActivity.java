package com.centigrade.browser;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.centigrade.ui.widget.HyperButton;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class ToolboxActivity extends AppCompatActivity {
    
    private EditText encodeInput, base64Input, colorInput;
    private TextView encodeResult, base64Result, uaInfo, ipResult;
    private View colorPreview;
    private TextView colorInfo;
    private HyperButton btnGetUa, btnQueryIp;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbox);
        
        View backBtn = findViewById(R.id.back);
        TextView titleView = findViewById(R.id.appbar_title);
        if (titleView != null) {
            titleView.setText("工具箱");
        }
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
        
        encodeInput = findViewById(R.id.encode_input);
        encodeResult = findViewById(R.id.encode_result);
        base64Input = findViewById(R.id.base64_input);
        base64Result = findViewById(R.id.base64_result);
        colorInput = findViewById(R.id.color_input);
        colorPreview = findViewById(R.id.color_preview);
        colorInfo = findViewById(R.id.color_info);
        uaInfo = findViewById(R.id.ua_info);
        ipResult = findViewById(R.id.ip_result);
        btnGetUa = findViewById(R.id.btn_get_ua);
        btnQueryIp = findViewById(R.id.btn_query_ip);
        
        // URL编码
        findViewById(R.id.btn_encode).setOnClickListener(v -> {
            String input = encodeInput.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "请输入文本", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                String encoded = URLEncoder.encode(input, "UTF-8");
                encodeResult.setText(encoded);
            } catch (Exception e) {
                encodeResult.setText("编码失败: " + e.getMessage());
            }
        });
        
        // URL解码
        findViewById(R.id.btn_decode).setOnClickListener(v -> {
            String input = encodeInput.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "请输入文本", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                String decoded = URLDecoder.decode(input, "UTF-8");
                encodeResult.setText(decoded);
            } catch (Exception e) {
                encodeResult.setText("解码失败: " + e.getMessage());
            }
        });
        
        // Base64编码
        findViewById(R.id.btn_base64_encode).setOnClickListener(v -> {
            String input = base64Input.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "请输入文本", Toast.LENGTH_SHORT).show();
                return;
            }
            String encoded = android.util.Base64.encodeToString(input.getBytes(), android.util.Base64.DEFAULT);
            base64Result.setText(encoded.trim());
        });
        
        // Base64解码
        findViewById(R.id.btn_base64_decode).setOnClickListener(v -> {
            String input = base64Input.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "请输入文本", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                byte[] decoded = android.util.Base64.decode(input, android.util.Base64.DEFAULT);
                base64Result.setText(new String(decoded, "UTF-8"));
            } catch (Exception e) {
                base64Result.setText("解码失败: " + e.getMessage());
            }
        });
        
        // 颜色预览
        colorInput.setOnEditorActionListener((v, actionId, event) -> {
            updateColorPreview();
            return false;
        });
        
        // 获取UA
        btnGetUa.setOnClickListener(v -> {
            String ua = System.getProperty("http.agent");
            if (ua == null || ua.isEmpty()) {
                ua = "Mozilla/5.0 (Linux; Android 14) Mobile Safari";
            }
            uaInfo.setText(ua);
        });
        
        // IP查询
        btnQueryIp.setOnClickListener(v -> queryLocalIp());
    }
    
    private void updateColorPreview() {
        String input = colorInput.getText().toString().trim();
        if (input.isEmpty()) return;
        
        try {
            int color = android.graphics.Color.parseColor(input);
            colorPreview.setBackgroundColor(color);
            
            int r = android.graphics.Color.red(color);
            int g = android.graphics.Color.green(color);
            int b = android.graphics.Color.blue(color);
            int a = android.graphics.Color.alpha(color);
            
            String hex = String.format("#%02X%02X%02X%02X", a, r, g, b);
            colorInfo.setText("HEX: " + hex + "\nRGB: " + r + ", " + g + ", " + b + "\nAlpha: " + a);
        } catch (Exception e) {
            colorInfo.setText("无效颜色代码");
        }
    }
    
    private void queryLocalIp() {
        ipResult.setText("正在查询...");
        
        new Thread(() -> {
            try {
                // 本地IP
                StringBuilder sb = new StringBuilder("=== 本机IP信息 ===\n");
                
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface iface = interfaces.nextElement();
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            sb.append(iface.getName()).append(": ").append(addr.getHostAddress()).append("\n");
                        }
                    }
                }
                
                // 公网IP
                try {
                    URL ipUrl = new URL("http://ipinfo.io/ip");
                    HttpURLConnection conn = (HttpURLConnection) ipUrl.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String publicIp = reader.readLine();
                    if (publicIp != null) {
                        sb.append("\n公网IP: ").append(publicIp.trim());
                    }
                    reader.close();
                    conn.disconnect();
                } catch (Exception e) {
                    sb.append("\n公网IP查询失败");
                }
                
                final String result = sb.toString();
                runOnUiThread(() -> ipResult.setText(result));
                
            } catch (Exception e) {
                final String error = "查询失败: " + e.getMessage();
                runOnUiThread(() -> ipResult.setText(error));
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