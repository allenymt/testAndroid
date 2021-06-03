package com.vdian.android.lib.testforgradle.activityResult;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.vdian.android.lib.testforgradle.R;

/**
 * @author yulun
 * @sinice 2020-12-07 15:37
 */
public class TestCActivity extends AppCompatActivity {
    static boolean toD = false;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ((TextView)findViewById(R.id.test_text)).setText("TestCActivity");
        findViewById(R.id.test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(1005);
                finish();
            }
        });
        if (!toD){
            toD = true;
            Intent intent = new Intent(TestCActivity.this,TestDActivity.class);
            startActivityForResult(intent,1003);
            finish();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        android.util.Log.i("yulun","TestCActivity requestCode is "+requestCode+ " resultCode is :"+resultCode);

    }
}
