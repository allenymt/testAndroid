package com.vdian.android.lib.testforgradle;

import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.vdian.android.lib.testforgradle.activityResult.TestBActivity;
import com.vdian.android.lib.testforgradle.activityResult.lauchmodel.TestSingleTaskActivity;
import com.vdian.android.lib.testforgradle.applink.AppLinkActivity;
import com.vdian.android.lib.testforgradle.binder.RemoteTestActivity;
import com.vdian.android.lib.testforgradle.dataBinding.TestDateBindingActivity;
import com.vdian.android.lib.testforgradle.datastore.TestDataStoreActivity;
import com.vdian.android.lib.testforgradle.directory.TestAndroidFileDirectory;
import com.vdian.android.lib.testforgradle.go_market.TestGoMarketActivity;
import com.vdian.android.lib.testforgradle.gradient.TestGradientActivity;
import com.vdian.android.lib.testforgradle.ktl.coroutines.TestCoroutinesActivity;
import com.vdian.android.lib.testforgradle.ktl.inline.TestNoInline;
import com.vdian.android.lib.testforgradle.launch_app.LaunchOtherAppActivity;
import com.vdian.android.lib.testforgradle.location.LocationTestActivity;
import com.vdian.android.lib.testforgradle.memory.TestMemory;
import com.vdian.android.lib.testforgradle.nfc.NFCIdentificationActivity;
import com.vdian.android.lib.testforgradle.nfc.Fm1208NfcLaunchActivity;
import com.vdian.android.lib.testforgradle.nfc.Ntag424DnaTestActivity;
import com.vdian.android.lib.testforgradle.oomDemo.OomDemoActivity;
import com.vdian.android.lib.testforgradle.pageing3.PagingTestActivity;
import com.vdian.android.lib.testforgradle.reflex.TestReflexAction;
import com.vdian.android.lib.testforgradle.rom.AppInstallUtil;
import com.vdian.android.lib.testforgradle.rom.RomCheck;
import com.vdian.android.lib.testforgradle.rom.RomCheckActivity;
import com.vdian.android.lib.testforgradle.room.RoomTestActivity;
import com.vdian.android.lib.testforgradle.rotate.RotateTestActivity;
import com.vdian.android.lib.testforgradle.self_view.SelfViewActivity;
import com.vdian.android.lib.testforgradle.single.TestClassInit;
import com.vdian.android.lib.testforgradle.surface.SurfaceNavigationActivity;
import com.vdian.android.lib.testforgradle.tablayout.TabLayoutTestActivity;
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

import org.apache.commons.lang3.RandomStringUtils;

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
        Util.Util.INSTANCE.hideSystemUi(this);
        //适配Android P
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindDemoCategories();
        runStartupDiagnostics();
    }

    private void runStartupDiagnostics() {
        TestMath.main();

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

        System.out.println("ios");
        System.out.println("AppID(应用ID)");
        System.out.println(RandomStringUtils.randomAlphanumeric(18).toLowerCase());
        System.out.println("AppSecret(应用密钥)");
        System.out.println(RandomStringUtils.randomAlphanumeric(32).toLowerCase());
        System.out.println("android");
        System.out.println("AppID(应用ID)");
        System.out.println(RandomStringUtils.randomAlphanumeric(18).toLowerCase());
        System.out.println("AppSecret(应用密钥)");
        System.out.println(RandomStringUtils.randomAlphanumeric(32).toLowerCase());
        System.out.println("Android版本：" + Build.VERSION.SDK_INT);
        System.out.println("64位系统：" + RomCheck.Companion.is64Device());
        System.out.println("64位进程：" + RomCheck.Companion.is64Process());
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


    private void bindDemoCategories() {
        findViewById(R.id.category_nfc).setOnClickListener(view -> showNfcDemos());
        findViewById(R.id.category_android).setOnClickListener(view -> showAndroidDemos());
        findViewById(R.id.category_data).setOnClickListener(view -> showDataDemos());
        findViewById(R.id.category_ui).setOnClickListener(view -> showUiDemos());
        findViewById(R.id.category_runtime).setOnClickListener(view -> showRuntimeDemos());
    }

    private void showNfcDemos() {
        showDemoDialog("NFC 与卡片", new DemoEntry[]{
                new DemoEntry("FM1208 · 主动识别与读写", this::openNfcIdentification),
                activityDemo("FM1208 · 冷启动唤起验证", Fm1208NfcLaunchActivity.class),
                new DemoEntry("NTAG 424 DNA · 读写与 SDM", () -> {
                    android.util.Log.i("Ntag424DnaTest", "[NTAG424-DEMO] opening standalone test page");
                    openDemo(Ntag424DnaTestActivity.class);
                })
        });
    }

    private void showAndroidDemos() {
        showDemoDialog("系统与组件", new DemoEntry[]{
                activityDemo("App Link", AppLinkActivity.class),
                new DemoEntry("Activity Result / ViewModel", () ->
                        startActivityForResult(new Intent(this, TestBActivity.class), 1002)),
                activityDemo("启动其他 App", LaunchOtherAppActivity.class),
                activityDemo("Activity 启动模式", TestSingleTaskActivity.class),
                activityDemo("Activity 屏幕旋转", RotateTestActivity.class),
                activityDemo("应用市场跳转", TestGoMarketActivity.class),
                activityDemo("定位", LocationTestActivity.class),
                activityDemo("ROM 与设备检测", RomCheckActivity.class)
        });
    }

    private void showDataDemos() {
        showDemoDialog("数据与后台任务", new DemoEntry[]{
                activityDemo("Data Binding", TestDateBindingActivity.class),
                activityDemo("View Binding", TestViewBindingActivity.class),
                activityDemo("WorkManager", WorkManagerTestActivity.class),
                activityDemo("Room", RoomTestActivity.class),
                activityDemo("Paging 3", PagingTestActivity.class),
                activityDemo("DataStore", TestDataStoreActivity.class)
        });
    }

    private void showUiDemos() {
        showDemoDialog("界面与渲染", new DemoEntry[]{
                activityDemo("Touch 事件", TestTouchActivity.class),
                activityDemo("自定义 View", SelfViewActivity.class),
                activityDemo("Surface / Texture / OpenGL", SurfaceNavigationActivity.class),
                activityDemo("TabLayout", TabLayoutTestActivity.class),
                activityDemo("渐变背景", TestGradientActivity.class)
        });
    }

    private void showRuntimeDemos() {
        showDemoDialog("性能、线程与进程", new DemoEntry[]{
                new DemoEntry("异常捕获", () -> Util.Util.INSTANCE.testTryCache(mHandler)),
                activityDemo("内存泄漏", TestLeak1Activity.class),
                activityDemo("OOM", OomDemoActivity.class),
                activityDemo("多线程", TestThreadActivity.class),
                activityDemo("协程", TestCoroutinesActivity.class),
                activityDemo("Binder", RemoteTestActivity.class),
                activityDemo("线程 Dump", TestThreadDumpActivity.class)
        });
    }

    private DemoEntry activityDemo(String title, Class<? extends Activity> activityClass) {
        return new DemoEntry(title, () -> openDemo(activityClass));
    }

    private void openDemo(Class<? extends Activity> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    private void openNfcIdentification() {
        Intent intent = new Intent(this, NFCIdentificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void showDemoDialog(String title, DemoEntry[] entries) {
        String[] labels = new String[entries.length];
        for (int index = 0; index < entries.length; index++) {
            labels[index] = entries[index].title;
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(labels, (dialog, which) -> entries[which].action.run())
                .setNegativeButton("取消", null)
                .show();
    }

    private static final class DemoEntry {
        private final String title;
        private final Runnable action;

        private DemoEntry(String title, Runnable action) {
            this.title = title;
            this.action = action;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    // 拦截系统Home按钮，拦截返回按钮
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 屏蔽返回按钮
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            // 屏蔽系统Home按钮
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    // 拦截任务列表按钮，防止进程被杀
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        ((ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE)).moveTaskToFront(getTaskId(), 0);
    }
}
