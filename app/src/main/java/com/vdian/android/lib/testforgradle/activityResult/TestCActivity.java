package com.vdian.android.lib.testforgradle.activityResult;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
        overridePendingTransition(R.anim.flutter_hybrid_top_in, 0);
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

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.flutter_hybrid_top_out);
    }
}
