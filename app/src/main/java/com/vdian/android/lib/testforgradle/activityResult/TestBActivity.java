package com.vdian.android.lib.testforgradle.activityResult;

import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.vdian.android.lib.testforgradle.R;
import com.vdian.android.lib.testforgradle.viewmodel.TestViewModel;

/**
 * @author yulun
 * @sinice 2020-12-07 15:37
 */
public class TestBActivity extends AppCompatActivity {
    private TestViewModel testViewModel;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(R.anim.flutter_hybrid_top_in, R.anim.flutter_hybrid_exit_bottom);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        // 因为是系统的toast,所以能展示，业务上可以这么理解
        // 不对，除非windowManager是Applicationd , 如果是ac的 ，那肯定不行，此时
        Toast.makeText(this, "123123", Toast.LENGTH_LONG).show();
        testViewModel = ViewModelProviders.of(this).get(TestViewModel.class);
        android.util.Log.i("testViewModel", "TestBActivity onCreate a is " + testViewModel.getA());
        testViewModel.setA(10);

//        ((TextView) findViewById(R.id.test_text)).setText("TestBActivity");
        findViewById(R.id.test_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(TestBActivity.this, TestBActivity.class);
                    startActivityForResult(intent, 1002);
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ((TextView)findViewById(R.id.test)).setText("Hohohong Test");
//                    }
//                }).start();

            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                ((TextView)findViewById(R.id.test_text)).setText("Hohohong Test");
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        android.util.Log.i("testViewModel","TestBActivity onStart a is "+testViewModel.getA());
        android.util.Log.i("testToast","onStart");
        final int callingUid = Binder.getCallingUid();

    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.i("testToast","onResume" + "tw is "+findViewById(R.id.test_text).getWidth());
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        android.util.Log.i("testToast","onPostResume");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        android.util.Log.i("yulun","TestBActivity requestCode is "+requestCode+ " resultCode is :"+resultCode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.i("testToast","onDestroy");
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 调用时机是在ViewRootImpl setView的时候，调用了requestLayout，如果是第一次，那就回调用ViewGroup的dispatchAttachedToWindow，会循环通知所有View的onAttachedToWindow
        // 这个方法是在onResume和onPostResume之后，具体可参考ActivityThread的handleResumeActivity
        android.util.Log.i("testToast","onAttachedToWindow");
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // ActivityThread的handleDestroyActivity，调用wm的removeView，会走到ViewRootImpl里的die,调用到ViewGroup的dispatchDetachedFromWindow
        // 在onDestroy之后
        android.util.Log.i("testToast","onDetachedFromWindow");
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.flutter_hybrid_enter_bottom, R.anim.flutter_hybrid_top_out);
    }
}
