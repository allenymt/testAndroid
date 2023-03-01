package com.vdian.android.lib.testforgradle.activityResult.lauchmodel;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import com.vdian.android.lib.testforgradle.R;
import com.vdian.android.lib.testforgradle.generated.callback.OnClickListener;
import com.vdian.android.lib.testforgradle.viewmodel.TestViewModel;

/**
 * @author yulun
 * @sinice 2020-12-07 15:37
 */
public class TestStandardActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(R.anim.flutter_hybrid_top_in, R.anim.flutter_hybrid_exit_bottom);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_standard);
        setTitle("TestStandardActivity");
        findViewById(R.id.test_standard_launch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TestStandardActivity.this,TestSingleTaskActivity.class));
            }
        });

        findViewById(R.id.test_result_launch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(TestStandardActivity.this,TestSingleTaskActivity.class),1000);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

    }

    @Override
    public void finish() {
        super.finish();
    }
}
