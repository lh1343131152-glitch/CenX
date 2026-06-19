package com.centigrade.browser;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    private void applyEdgeToEdgeInsets() {
        final android.view.View root = findViewById(android.R.id.content);
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
        setContentView(R.layout.activity_about);
        applyEdgeToEdgeInsets();

        findViewById(R.id.back).setOnClickListener(v -> finish());
        TextView titleView = findViewById(R.id.appbar_title);
        if (titleView != null) {
            titleView.setText("关于CenX");
        }

        TextView tvVersion = findViewById(R.id.tv_about_version);
        if (tvVersion != null) {
            tvVersion.setText("版本号: v6.0");
        }

        CardView cardUserAgreement = findViewById(R.id.card_user_agreement);
        if (cardUserAgreement != null) {
            cardUserAgreement.setOnClickListener(v -> showUserAgreementDialog());
        }

        CardView cardJoinQQ = findViewById(R.id.card_join_qq);
        if (cardJoinQQ != null) {
            cardJoinQQ.setOnClickListener(v -> {
                String authKey = "u3xef7r%2BfjjJUK3Q6rkXVctoWOMLZnkdXiTTTMHUhgEG4alY8mL%2BM8Pz%2BTvVBS5v";
                String url = "https://qun.qq.com/universal-share/share?ac=1&authKey=" + authKey;

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setPackage("com.tencent.mobileqq");

                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(webIntent);
                }
            });
        }
    }

    private void showUserAgreementDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.about_user_agreement))
                .setMessage(getString(R.string.user_agreement_content))
                .setPositiveButton("我知道了", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}