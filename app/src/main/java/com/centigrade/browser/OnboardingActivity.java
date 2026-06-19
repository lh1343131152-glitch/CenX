package com.centigrade.browser;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private PreferencesManager prefs;
    private ViewPager2 viewPager;
    private Button actionButton;
    private LinearLayout indicatorContainer;
    private List<OnboardingPage> pages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferencesManager.getInstance(this);

        if (prefs.isOnboardingCompleted()) {
            openMainAndFinish();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.view_pager_onboarding);
        actionButton = findViewById(R.id.btn_onboarding_action);
        indicatorContainer = findViewById(R.id.indicator_container);

        pages = Arrays.asList(
                new OnboardingPage(
                        "欢迎使用CenX",
                        "轻巧、直接、专注浏览体验",
                        "欢迎来到 CenX。\n\n你可以在这里快速搜索、直接输入网址、管理标签页，并使用资源嗅探、历史记录、书签与下载等功能。"
                ),
                new OnboardingPage(
                        "用户协议",
                        "使用前请阅读以下内容",
                        "你懒得看我也懒得写，该做什么不该做什么你我心里都清楚"
                ),
                new OnboardingPage(
                        "使用教程",
                        "三步快速上手",
                        "• 顶部可搜索或输入网址\n• 底部栏可后退、前进、回到主页、查看标签和更多功能\n• 更多菜单中可打开设置、下载管理、资源嗅探等功能\n• 长按主页可以保存书签\n准备好了就进入 CenX。"
                )
        );

        viewPager.setAdapter(new OnboardingAdapter());
        setupIndicators();
        updateIndicators(0);
        updateActionButton(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicators(position);
                updateActionButton(position);
            }
        });

        actionButton.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < pages.size() - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                prefs.setOnboardingCompleted(true);
                openMainAndFinish();
            }
        });
    }

    private void openMainAndFinish() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtras(getIntent());
        startActivity(intent);
        finish();
    }

    private void setupIndicators() {
        indicatorContainer.removeAllViews();
        for (int i = 0; i < pages.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(8), dp(8));
            params.leftMargin = dp(4);
            params.rightMargin = dp(4);
            dot.setLayoutParams(params);
            indicatorContainer.addView(dot);
        }
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicatorContainer.getChildCount(); i++) {
            View view = indicatorContainer.getChildAt(i);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(ContextCompat.getColor(this,
                    i == position ? R.color.guide_indicator_active : R.color.guide_indicator_inactive));
            view.setBackground(drawable);
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.width = dp(8);
            lp.height = dp(8);
            view.setLayoutParams(lp);
        }
    }

    private void updateActionButton(int position) {
        actionButton.setText(position == pages.size() - 1 ? "进入CenX" : "下一步");
    }

    private int dp(int value) {
        return (int) (getResources().getDisplayMetrics().density * value);
    }

    private static class OnboardingPage {
        final String title;
        final String subtitle;
        final String content;

        OnboardingPage(String title, String subtitle, String content) {
            this.title = title;
            this.subtitle = subtitle;
            this.content = content;
        }
    }

    private class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageViewHolder> {

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_page, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            OnboardingPage page = pages.get(position);
            holder.title.setText(page.title);
            holder.subtitle.setText(page.subtitle);
            holder.content.setText(page.content);

            
            
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        class PageViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView subtitle;
            TextView content;

            PageViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tv_onboarding_title);
                subtitle = itemView.findViewById(R.id.tv_onboarding_subtitle);
                content = itemView.findViewById(R.id.tv_onboarding_content);
            }
        }
    }
}