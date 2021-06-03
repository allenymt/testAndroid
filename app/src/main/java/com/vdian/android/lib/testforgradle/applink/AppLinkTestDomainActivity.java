package com.vdian.android.lib.testforgradle.applink;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;

import com.vdian.android.lib.testforgradle.R;

import java.util.HashSet;

public class AppLinkTestDomainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_link_test_domain);
        findViewById(R.id.click_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testDomain();
            }
        });
    }

    private void testDomain() {
        HashSet<String> hostSet = new HashSet<String>();
        hostSet.add("feedback");
        hostSet.add("*");
        hostSet.add("wx");
        hostSet.add("wdb-applink.weidian.com");
        hostSet.add("weidian.com");
        for (String host : hostSet) {
            android.util.Log.i("AppLink",
                    host + " match " + Patterns.DOMAIN_NAME.matcher(host).matches());
        }

    }
}
