package com.vdian.android.lib.testforgradle.go_market;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vdian.android.lib.testforgradle.R;
import com.vdian.android.lib.testforgradle.activityResult.lauchmodel.TestStandardActivity;

import java.util.List;

/**
 * @author yulun
 * @since 2023-11-14 18:42
 */
public class TestGoMarketActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_go_market);
        setTitle("TestGoMarketActivity");
        findViewById(R.id.test_go_market).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goAppMarket();
            }
        });
        getTargetPage(Uri.parse("mimarket://details?packageName=com.koudai.weidian.buyer"));
    }

    public static String getTargetPage(Uri uri) {
        if (uri == null) {
            return "";
        }
        String host = uri.getHost();
        List pathSegments = uri.getPathSegments();
        if (!pathSegments.isEmpty()) {
            host = (String) pathSegments.get(0);
        }
        return host;
    }

    private void goAppMarket() {
//        MarketLaunchHelper.INSTANCE.startVivoMarketEvaluate(this);
        MarketLaunchHelper.INSTANCE.startMarketEvaluate(this);
//        MarketHelper.INSTANCE.open(this, "com.koudai.weidian.buyer");
//        val packageName = AppUtil.getAppContext().packageName
//        try {
//            // 默认使用应用市场
//            val uri = Uri.parse("market://details?id=$packageName")
//            val intent = Intent(Intent.ACTION_VIEW, uri);
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
//            AppUtil.getAppContext().startActivity(intent);
//        } catch (e:ActivityNotFoundException) {
//            // 应用市场未安装，打开网页版应用市场
//            // 如果没有安装应用宝，则打开浏览器
//            AppUtil.getAppContext().startActivity(
//                    Intent(
//                            Intent.ACTION_VIEW,
//                            Uri.parse("https://a.app.qq.com/o/simple.jsp?pkgname=$packageName")
//                    )
//            )
//        }
    }

}
