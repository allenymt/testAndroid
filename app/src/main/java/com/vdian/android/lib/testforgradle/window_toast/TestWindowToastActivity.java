package com.vdian.android.lib.testforgradle.window_toast;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.vdian.android.lib.testforgradle.R;

/**
 * @author yulun
 * @sinice 2020-12-07 15:37
 * 测试学习window toast
 */
public class TestWindowToastActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        // 因为是系统的toast,所以能展示，业务上可以这么理解
        // 但是
        Toast.makeText(this, "123123", Toast.LENGTH_LONG).show();

        android.util.Log.i("testToast", "onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        android.util.Log.i("testToast", "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.i("testToast", "onResume" + "tw is " + findViewById(R.id.test_text).getWidth());
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        android.util.Log.i("testToast", "onPostResume");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        android.util.Log.i("yulun", "TestBActivity requestCode is " + requestCode + " resultCode is :" + resultCode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.i("testToast", "onDestroy");
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 调用时机是在ViewRootImpl setView的时候，调用了requestLayout，如果是第一次，那就回调用ViewGroup的dispatchAttachedToWindow，会循环通知所有View的onAttachedToWindow
        // 这个方法是在onResume和onPostResume之后，具体可参考ActivityThread的handleResumeActivity
        android.util.Log.i("testToast", "onAttachedToWindow");
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // ActivityThread的handleDestroyActivity，调用wm的removeView，会走到ViewRootImpl里的die,调用到ViewGroup的dispatchDetachedFromWindow
        // 在onDestroy之后
        android.util.Log.i("testToast", "onDetachedFromWindow");
    }
}
