package com.vdian.android.lib.testforgradle.activityResult;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.vdian.android.lib.testforgradle.R;
import com.vdian.android.lib.testforgradle.viewmodel.TestViewModel;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yulun
 * @sinice 2020-12-07 15:37
 */
public class TestDActivity extends AppCompatActivity {
    private TestViewModel testViewModel;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        testViewModel = ViewModelProviders.of(this).get(TestViewModel.class);
        android.util.Log.i("testViewModel","TestDActivity onCreate a is "+testViewModel.getA());

        setContentView(R.layout.activity_test);
        ((TextView)findViewById(R.id.test_text)).setText("TestDActivity");
        findViewById(R.id.test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TestDActivity.this,TestCActivity.class);
                startActivityForResult(intent,1003);
                finish();
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        android.util.Log.i("testViewModel","TestDActivity onStart a is "+testViewModel.getA());
    }
}
