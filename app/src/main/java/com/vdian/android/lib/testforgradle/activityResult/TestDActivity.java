package com.vdian.android.lib.testforgradle.activityResult;

import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.vdian.android.lib.testforgradle.R;
import com.vdian.android.lib.testforgradle.viewmodel.TestViewModel;

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
