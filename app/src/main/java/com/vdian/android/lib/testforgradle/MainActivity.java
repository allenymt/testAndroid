package com.vdian.android.lib.testforgradle;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.vdian.android.lib.testforgradle.activityResult.TestBActivity;
import com.vdian.android.lib.testforgradle.binder.RemoteTestActivity;
import com.vdian.android.lib.testforgradle.dataBinding.TestDateBindingActivity;
import com.vdian.android.lib.testforgradle.datastore.TestDataStoreActivity;
import com.vdian.android.lib.testforgradle.directory.TestAndroidFileDirectory;
import com.vdian.android.lib.testforgradle.ktl.coroutines.TestCoroutinesActivity;
import com.vdian.android.lib.testforgradle.ktl.inline.TestNoInline;
import com.vdian.android.lib.testforgradle.launch_app.LaunchOtherAppActivity;
import com.vdian.android.lib.testforgradle.location.LocationTestActivity;
import com.vdian.android.lib.testforgradle.memory.TestMemory;
import com.vdian.android.lib.testforgradle.oomDemo.OomDemoActivity;
import com.vdian.android.lib.testforgradle.pageing3.PagingTestActivity;
import com.vdian.android.lib.testforgradle.reflex.TestReflexAction;
import com.vdian.android.lib.testforgradle.rom.AppInstallUtil;
import com.vdian.android.lib.testforgradle.rom.RomCheckActivity;
import com.vdian.android.lib.testforgradle.room.RoomTestActivity;
import com.vdian.android.lib.testforgradle.self_view.SelfViewActivity;
import com.vdian.android.lib.testforgradle.single.TestClassInit;
import com.vdian.android.lib.testforgradle.testclass.JAndKClassTest;
import com.vdian.android.lib.testforgradle.testleak.TestLeak1Activity;
import com.vdian.android.lib.testforgradle.thread.TestThreadActivity;
import com.vdian.android.lib.testforgradle.thread.signle.TestSingle;
import com.vdian.android.lib.testforgradle.thread_dump.TestThreadDumpActivity;
import com.vdian.android.lib.testforgradle.touch.TestTouchActivity;
import com.vdian.android.lib.testforgradle.viewBinding.TestViewBindingActivity;
import com.vdian.android.lib.testforgradle.workmanager.WorkManagerTestActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    static TestFinalize quote;


    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            return false;
        }
    });
    private int priInt = 1;

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TestClassInit.Test.main();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int ut_hash = 0;
                int ut_hash1 = ut_hash + priInt;
                ut_hash = ut_hash | 1;
                android.util.Log.i("yulun ut_hash", "ut_hash is " + ut_hash);
                ut_hash = ut_hash | 1 << 1;
                android.util.Log.i("yulun ut_hash", "ut_hash is " + ut_hash);
                ut_hash = ut_hash | 1 << 2;
                android.util.Log.i("yulun ut_hash", "ut_hash is " + ut_hash);
//                ut_hash = ut_hash | 1 << 3;
//                android.util.Log.i("yulun ut_hash", "ut_hash is "+ ut_hash);
                ut_hash = ut_hash | 1 << 4;
                android.util.Log.i("yulun ut_hash", "ut_hash is " + ut_hash);
                ut_hash = ut_hash | 1 << 5;
                android.util.Log.i("yulun ut_hash", "ut_hash is " + ut_hash);
            }
        }, 0);
        try {
            getClassLoader().loadClass("a.b.c.d.d");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Class.forName("comn.xx.xx.abc");
        } catch (Exception e) {
            e.printStackTrace();
        }

        TestAndroidFileDirectory.testPrintFile(this);
        TestMemory.testMemory(this);

        TestReflexAction.test();

        Util.Util.INSTANCE.isOhos();

        AppInstallUtil.trackAppInstallTime(getApplicationContext());

        new TestNoInline().main();
        new JAndKClassTest().testA();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void testAnr(View view) {
        Util.Util.INSTANCE.testTryCache(mHandler);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int t = 0;
        int f = 0;
        for (int i = 0; i < 10000; i++) {
            if (tryDownCdnMax()) {
                t++;
            } else {
                f++;
            }
        }
        android.util.Log.e("yulun", "t is " + t + "  f is " + f);
        return super.dispatchTouchEvent(ev);
    }


    public void testAnr2(View view) {
        android.util.Log.i("testFinalize", "test anr222");
        startActivity(new Intent(MainActivity.this, TestLeak1Activity.class));
    }

    public void goToTouchAc(View view) {
        startActivity(new Intent(MainActivity.this, TestTouchActivity.class));
        android.util.Log.i("getStatusBarHeight", "getStatusBarHeight: " + Util.Util.INSTANCE.getStatusBarHeight(this));

    }

    public void goToLaunchAc(View view) {
        startActivity(new Intent(MainActivity.this, LaunchOtherAppActivity.class));
    }

    public void goToSelfView(View view) {
        startActivity(new Intent(MainActivity.this, SelfViewActivity.class));
    }

    public void goToOomDemo(View view) {
        startActivity(new Intent(MainActivity.this, OomDemoActivity.class));
    }

    public void goToThread(View view) {
        int oldCapacity = 10;
        oldCapacity = oldCapacity >> 1; // 右移1位 除以2
        startActivity(new Intent(MainActivity.this, TestThreadActivity.class));
    }

    public void goToTestBinder(View view) {
        startActivity(new Intent(MainActivity.this, RemoteTestActivity.class));
    }

    public void goToTestThreadDump(View view) {
        startActivity(new Intent(MainActivity.this, TestThreadDumpActivity.class));

    }

    public boolean tryDownCdnMax() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 20 || hour > 22) {
            return false;
        }
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        return random.nextInt(100) % 2 == 0;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TestSingle.main();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            String[] per = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_NETWORK_STATE};
            ActivityCompat.requestPermissions(this, per, 1000);
        }
    }

    public void testViewModel(View v) {
        startActivityForResult(new Intent(MainActivity.this, TestBActivity.class), 1002);

    }


    public void goToRomCheck(View view) {
        startActivity(new Intent(MainActivity.this, RomCheckActivity.class));
    }

    public void goToCoroutines(View view) {
        startActivity(new Intent(MainActivity.this, TestCoroutinesActivity.class));
    }

    public void goToLocation(View view) {
        startActivity(new Intent(MainActivity.this, LocationTestActivity.class));
    }

    public void goToDataBinding(View view) {
        startActivity(new Intent(MainActivity.this, TestDateBindingActivity.class));
    }

    public void goToViewBinding(View view) {
        startActivity(new Intent(MainActivity.this, TestViewBindingActivity.class));
    }

    public void goToWorkManager(View view) {
        startActivity(new Intent(MainActivity.this, WorkManagerTestActivity.class));
    }

    public void goToRoom(View view) {
        startActivity(new Intent(MainActivity.this, RoomTestActivity.class));
    }

    public void goToPaging3(View view) {
        startActivity(new Intent(MainActivity.this, PagingTestActivity.class));
    }

    public void goToDataStore(View view) {
        startActivity(new Intent(MainActivity.this, TestDataStoreActivity.class));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}
