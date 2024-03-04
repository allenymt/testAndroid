package com.vdian.android.lib.testforgradle.tablayout;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.tabs.TabLayout;
import com.vdian.android.lib.testforgradle.MainActivity;
import com.vdian.android.lib.testforgradle.R;

/**
 * @author yulun
 * @since 2024-01-24 15:07
 */
public class TabLayoutTestActivity extends Activity {
    private TabLayout mTabLayout;
    /**
     * 导航样式文件数组
     */
    protected int[] tabImageResource;

    /**
     * 导航标题数组
     */
    protected String[] tabTitles;

    //是否为图文混合
    private boolean isImageAndChina;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tablayout_test);

        initData();
        initViews();
        initListeners();
        selectTab(0);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        tabImageResource = new int[]{
                R.mipmap.ic_launcher,
                R.mipmap.ic_launcher,
                R.mipmap.ic_launcher,
                R.mipmap.ic_launcher,
                R.mipmap.ic_launcher
        };
        tabTitles = new String[]{
                "发现", "132", "123", "123", "132"
        };
    }

    private void initViews() {
        mTabLayout = findViewById(R.id.tab_layout);
        initTabLayout();
    }

    private void initTabLayout() {
        //图文组合
        if (tabImageResource != null && tabImageResource.length > 0) {
            createImageAndChina();
        } else if (tabTitles != null && tabTitles.length > 0) {
            createChinaItem();
        }
    }

    /**
     * 纯文字Item
     */
    private void createChinaItem() {
        isImageAndChina = false;
        for (int i = 0; i < tabImageResource.length; i++) {
            TabLayout.Tab tab = mTabLayout.newTab();
            if (tab != null) {
                mTabLayout.addTab(tab);
            }
        }
    }

    /**
     * 创建图文混排Item
     */
    private void createImageAndChina() {
        isImageAndChina = true;
        for (int i = 0; i < tabImageResource.length; i++) {
            TabLayout.Tab tab = mTabLayout.newTab();
            if (tab != null) {
                tab.setCustomView(getTabView(i));
                mTabLayout.addTab(tab);
            }
        }
    }

    /**
     * 图文混排Item
     *
     * @param position
     * @return
     */
    private View getTabView(int position) {
        View tabView = LayoutInflater.from(this).inflate(R.layout.tab_item, null);
        setContentItem(tabView, position);
        return tabView;
    }

    //设置TabView显示内容
    private void setContentItem(View tabView, int position) {
        ImageView ivAppIcon = tabView.findViewById(R.id.iv_app_icon);
        TextView tvImUseCoupon = tabView.findViewById(R.id.app_tv_name);
        ivAppIcon.setImageResource(tabImageResource[position]);
        tvImUseCoupon.setText(tabTitles[position]);
//        changeClipFalse(tabView);
        if (position == 0) {
            changeTabStatus(tabView, R.color.app_color_E8380D);
//            changeTabMargin(tabView, R.dimen.app_minus_15dp);
        }
    }

    private void initListeners() {
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //选中效果
                changeTabStatus(tab.getCustomView(), R.color.app_color_E8380D);
                changeTabMargin(tab.getCustomView(), R.dimen.app_minus_15dp);
                changeTabSelect(tab.getCustomView());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //未选中效果
                changeTabStatus(tab.getCustomView(), R.color.app_color_999999);
                changeTabMargin(tab.getCustomView(), R.dimen.app_0dp);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void changeTabStatus(View customView, int p) {
//        View customView = tab.getCustomView();
        TextView tv = customView.findViewById(R.id.app_tv_name);
        tv.setTextColor(ContextCompat.getColor(this, p));
    }

    /**
     * 选择指定选项卡
     *
     * @param position
     */
    public void selectTab(int position) {
        TabLayout.Tab tab = mTabLayout.getTabAt(position);
        changeTabMargin(tab.getCustomView(), R.dimen.app_minus_15dp);
        tab.select();
    }

    /**
     * 修改Tab视图Margin
     *
     * @param customView
     * @param dimenId
     */
    public void changeTabMargin(View customView, int dimenId) {
        ViewGroup targetViewToApplyMargin = (ViewGroup) customView.getParent();
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) targetViewToApplyMargin.getLayoutParams();
        layoutParams.topMargin = (int) getResources().getDimension(dimenId);
        targetViewToApplyMargin.setLayoutParams(layoutParams);
    }

    /**
     * 设置TabLayout下Tab父视图不裁剪子视图
     *
     * @param customView
     */
    public void changeClipFalse(View customView) {
        if (customView != null) {
            ViewGroup targetViewToApplyMargin = (ViewGroup) customView.getParent();
            //循环设置Tab下父视图不裁剪超出部分子视图
            while (targetViewToApplyMargin != mTabLayout) {
                targetViewToApplyMargin.setClipChildren(false);
                targetViewToApplyMargin.setClipToPadding(false);
                targetViewToApplyMargin = (ViewGroup) targetViewToApplyMargin.getParent();
            }
        }
    }

    /**
     * 使用属性动画改变Tab中View的状态
     *
     * @param customView
     */
    private void changeTabSelect(View customView) {
        View iV = customView.findViewById(R.id.iv_app_icon);
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(iV, "", 1.0f, 1.4f, 2.0f)
                .setDuration(200);
        objectAnimator.start();
        objectAnimator.addUpdateListener(animation -> {
            float cVal = (Float) animation.getAnimatedValue();
            iV.setScaleY(cVal);
            iV.setScaleX(cVal);
        });
    }

}
