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
import com.vdian.android.lib.testforgradle.activityResult.lauchmodel.TestSingleTaskActivity;
import com.vdian.android.lib.testforgradle.applink.AppLinkActivity;
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
import com.vdian.android.lib.testforgradle.rotate.RotateTestActivity;
import com.vdian.android.lib.testforgradle.self_view.SelfViewActivity;
import com.vdian.android.lib.testforgradle.single.TestClassInit;
import com.vdian.android.lib.testforgradle.surface.SurfaceNavigationActivity;
import com.vdian.android.lib.testforgradle.testclass.JAndKClassTest;
import com.vdian.android.lib.testforgradle.testleak.TestLeak1Activity;
import com.vdian.android.lib.testforgradle.thread.TestThreadActivity;
import com.vdian.android.lib.testforgradle.thread.signle.TestSingle;
import com.vdian.android.lib.testforgradle.thread_dump.TestThreadDumpActivity;
import com.vdian.android.lib.testforgradle.touch.TestTouchActivity;
import com.vdian.android.lib.testforgradle.util.TestFinalize;
import com.vdian.android.lib.testforgradle.util.Util;
import com.vdian.android.lib.testforgradle.viewBinding.TestViewBindingActivity;
import com.vdian.android.lib.testforgradle.workmanager.WorkManagerTestActivity;

public class MainActivity extends AppCompatActivity {

    public static TestFinalize quote;


    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            return false;
        }
    });

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
        // 测试类中 变量 静态变量，构造函数等的初始化顺序
        TestClassInit.Test.main();

        // 输出Android
        TestAndroidFileDirectory.testPrintFile(this);

        // 测试读取内存
        TestMemory.testMemory(this);

        // 反射测试
        TestReflexAction.test();

        // 测试是否鸿蒙
        Util.Util.INSTANCE.isOhos();

        // 测试跟踪APP安装时间
        AppInstallUtil.trackAppInstallTime(getApplicationContext());

        // 内联测试
        new TestNoInline().main();

        // kotlin class 和 java class 对比
        new JAndKClassTest().testA();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
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

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            String[] per = {android.Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(this, per, 1000);
        }
    }


    public void testCatchException(View view) {
        Util.Util.INSTANCE.testTryCache(mHandler);
    }

    public void toLink(View view) {
        startActivity(new Intent(MainActivity.this, AppLinkActivity.class));
    }

    public void testLeak(View view) {
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

    public void goToTestAcRotate(View view) {
        startActivity(new Intent(MainActivity.this, RotateTestActivity.class));

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

    public void goToTestAcLaunch(View view) {
        startActivity(new Intent(MainActivity.this, TestSingleTaskActivity.class));
    }

    public void goToTestSurfaceNavigation(View view) {
        startActivity(new Intent(MainActivity.this, SurfaceNavigationActivity.class));
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
