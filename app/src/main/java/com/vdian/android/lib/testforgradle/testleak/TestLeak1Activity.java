package com.vdian.android.lib.testforgradle.testleak;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.vdian.android.lib.testforgradle.R;

import java.util.ArrayList;

/**
 * @author yulun
 * @sinice 2021-01-26 11:29
 */
public class TestLeak1Activity extends AppCompatActivity {
    String testLeak;
    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            testLeak = "12345";
            return false;
        }
    });

    Handler mLeakHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            testLeak = "12312312345";
            android.util.Log.i("testLeak", "testLeak result is " + msg.getCallback().toString());
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_leak);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Runnable runnableFinish = new Runnable() {
            @Override
            public void run() {
                android.util.Log.i("testLeak", "testLeak i finish");
                finish();
            }
        };

        Runnable runnableLeakRunnable = new Runnable() {
            @Override
            public void run() {
                testLeak = "123";
                android.util.Log.i("testLeak", "testLeak leak  is $testLeak");
            }
        };

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                android.util.Log.i("testLeak", "i do first");
            }
        });
        mHandler.postDelayed(runnableLeakRunnable, 60*60*1000);
//        mLeakHandler.postDelayed(runnableLeakRunnable, 60*60*1000);
        mHandler.postDelayed(runnableFinish, 150);

        ArrayList<Object> objectList = new ArrayList();
        for (int i = 0; i < 9; i++) {
            objectList.add(new Object());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.i("testLeak", "test leak is onDestroy");
    }
}
