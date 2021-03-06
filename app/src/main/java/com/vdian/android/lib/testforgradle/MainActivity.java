package com.vdian.android.lib.testforgradle;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.vdian.android.lib.testforgradle.activityResult.TestBActivity;
import com.vdian.android.lib.testforgradle.applink.AppLinkTestDomainActivity;
import com.vdian.android.lib.testforgradle.binder.RemoteTestActivity;
import com.vdian.android.lib.testforgradle.directory.TestAndroidFileDirectory;
import com.vdian.android.lib.testforgradle.generic.GenericTest;
import com.vdian.android.lib.testforgradle.launch_app.LaunchOtherAppActivity;
import com.vdian.android.lib.testforgradle.memory.TestMemory;
import com.vdian.android.lib.testforgradle.oomDemo.OomDemoActivity;
import com.vdian.android.lib.testforgradle.reflex.TestChild;
import com.vdian.android.lib.testforgradle.reflex.TestReflexAction;
import com.vdian.android.lib.testforgradle.rom.RomCheckActivity;
import com.vdian.android.lib.testforgradle.rom.RomChecker;
import com.vdian.android.lib.testforgradle.self_view.SelfViewActivity;
import com.vdian.android.lib.testforgradle.single.TestStaticInnerSingle;
import com.vdian.android.lib.testforgradle.testleak.TestLeak1Activity;
import com.vdian.android.lib.testforgradle.thread.TestThreadActivity;
import com.vdian.android.lib.testforgradle.thread_dump.TestThreadDumpActivity;
import com.vdian.android.lib.testforgradle.touch.TestTouchActivity;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static TestFinalize quote;


    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            return false;
        }
    });

    public static void printCollection(List<?> col) {//?????????????????????????????????
        for (Object object : col) {
            System.out.println(object);
        }
    }

    public static int lengthOfLongestSubstring1(String s) {
        boolean[] map = new boolean[128];
        int length = s.length();
        int pre = 0, after = 0;
        int maxValidLength = 0;
        int currentLength = 0;
        while (after < length) {
            char character = s.charAt(after);
            int hash = (int) character;
            if (map[hash]) {
                while (character != s.charAt(pre)) {
                    map[s.charAt(pre)] = false;
                    pre++;
                }
                pre++;
            } else {
                currentLength = after - pre + 1;
                if (maxValidLength < currentLength) {
                    maxValidLength = currentLength;
                }
                map[hash] = true;
            }
            after++;
        }
//        System.out.print(maxValidLength);
        return maxValidLength;
    }


    /**
     * ???????????????????????????
     *
     * @return
     */
    public static boolean isOhos() {
        return RomChecker.Companion.isOhos();
    }

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
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int ut_hash = 0;
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

        isOhos();
    }

    public void testDrawableSize() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        ;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_test_drawable, options);
        System.out.println("bitmap test  w is : " + bitmap.getWidth() + " h is " + bitmap.getHeight() + "  config" + bitmap.getConfig().name() + " mDensity:" + bitmap.getDensity());
        System.out.println("bitmap test byte_count: " + bitmap.getByteCount());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            System.out.println("bitmap test  allocation_count: " + bitmap.getAllocationByteCount());
    }

    public void testAssetsSize() {
        AssetManager assetManager = getResources().getAssets();
        try {
            InputStream inputStream = assetManager.open("ic_launcher_test_assets.png");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            System.out.println("bitmap test  w is : " + bitmap.getWidth() + " h is " + bitmap.getHeight() + "  config" + bitmap.getConfig().name() + " mDensity:" + bitmap.getDensity());
            System.out.println("bitmap test byte_count: " + bitmap.getByteCount());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                System.out.println("bitmap test  allocation_count: " + bitmap.getAllocationByteCount());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void testAnr(View view) {
    }

    public void testPush() {
        //???????????????scheme
        String p = "https://h5.weidian.com/m/koubei/content-detail.html?authorId=295567927&feedId=234211848&0603@spoor=1011.70976.0.2315.videoFeed-295567927-toc-234211848%7Cfocus%26a42281a0-7d26-4cf9-a54a-07a2f5091677";
        try {
            String scheme = URI.create(p.trim()).getScheme();
            if (scheme == null) {
                p = "weidianbuyer://" + p;
            }
        } catch (Exception e) {
//            LogUtil.getLogger().w(e.getMessage(), e);
            p = "weidianbuyer://" + p;
        }
    }

    public void testSegment() {
        try {
            Uri.Builder dynamicRouteBuilder = new Uri.Builder();
            Intent intent = new Intent();
            Uri uri = Uri.parse("https://h5.weidian.com/m/weidian/shop-new.html#?shopId=1351700016&referrer=push&0303@spoor=1011.64704.0.1178.newItemFeed");
            Bundle bundle = new Bundle();
            String query = uri.getEncodedQuery();
            String[] params = new String[]{};
            if (!TextUtils.isEmpty(query)) {
                params = query.split("&");
            }
            if (params.length > 0) {
                for (String param : params) {
                    String[] keyValue = param.split("=", 2);
                    String name = keyValue[0];
                    String value = "";
                    if (keyValue.length > 1) {
                        value = keyValue[1];
                    }
                    try {
                        bundle.putString(name, URLDecoder.decode(value, "UTF-8"));
                        dynamicRouteBuilder.appendQueryParameter(name, URLDecoder.decode(value, "UTF-8"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                intent.putExtras(bundle);
            }

            String segment = uri.getEncodedFragment();
            if (!TextUtils.isEmpty(segment)) {
                dynamicRouteBuilder.encodedFragment(segment);
                if (segment.contains("?")) {
                    String[] seParams = new String[]{};
                    String segmentParam = segment.substring(segment.indexOf("?") + 1, segment.length());
                    seParams = segmentParam.split("&");
                    if (seParams.length > 0) {
                        for (String param : seParams) {
                            String[] keyValue = param.split("=", 2);
                            String name = keyValue[0];
                            String value = "";
                            if (keyValue.length > 1) {
                                value = keyValue[1];
                            }
                            bundle.putString(name, URLDecoder.decode(value, "UTF-8"));
                        }
                        intent.putExtras(bundle);
                    }
                }
            }
            intent.setData(dynamicRouteBuilder.build());


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void toLink(View v) {
        TestStaticInnerSingle.getInstance();
        startActivity(new Intent(this, AppLinkTestDomainActivity.class));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    public int lengthOfLongestSubstring(String s) {
        int leftStart = 0, rightEnd = 0, maxLength = 0;
//        HashSet<Character> pool = new HashSet<>();
        byte[] poolnew = new byte[256];
        int maxValidLength = 0;
        int currentLength = 0;
        int len = s.length();
        while (rightEnd < len) {
            char character = s.charAt(rightEnd);
            int hash = (int) character - (int) ('0');
            if (poolnew[hash] == 1) {
                while (character != s.charAt(leftStart)) {
                    poolnew[s.charAt(leftStart)] = 0;
                    leftStart++;
                }
                leftStart++;
            } else {
                currentLength = rightEnd - leftStart + 1;
                if (maxValidLength < currentLength) {
                    maxValidLength = currentLength;
                }
                poolnew[hash] = 1;
            }
            rightEnd++;
        }
        return maxLength;
    }


    public void testAnr2(View view) {
        android.util.Log.i("testFinalize", "test anr222");
        startActivity(new Intent(MainActivity.this, TestLeak1Activity.class));
    }

    public void goToTouchAc(View view) {
        startActivity(new Intent(MainActivity.this, TestTouchActivity.class));
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
        oldCapacity = oldCapacity >> 1; // ??????1??? ??????2
        startActivity(new Intent(MainActivity.this, TestThreadActivity.class));
    }

    public void goToTestBinder(View view) {
        startActivity(new Intent(MainActivity.this, RemoteTestActivity.class));
    }

    public void goToTestThreadDump(View view) {
        startActivity(new Intent(MainActivity.this, TestThreadDumpActivity.class));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
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


    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}
