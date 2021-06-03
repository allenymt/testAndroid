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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import com.vdian.android.lib.testforgradle.activityResult.TestBActivity;
import com.vdian.android.lib.testforgradle.applink.AppLinkTestDomainActivity;
import com.vdian.android.lib.testforgradle.directory.TestAndroidFileDirectory;
import com.vdian.android.lib.testforgradle.launch_app.LaunchOtherAppActivity;
import com.vdian.android.lib.testforgradle.memory.TestMemory;
import com.vdian.android.lib.testforgradle.oomDemo.OomDemoActivity;
import com.vdian.android.lib.testforgradle.reflex.TestReflexAction;
import com.vdian.android.lib.testforgradle.self_view.SelfViewActivity;
import com.vdian.android.lib.testforgradle.single.TestStaticInnerSingle;
import com.vdian.android.lib.testforgradle.testleak.TestLeak1Activity;
import com.vdian.android.lib.testforgradle.touch.TestTouchActivity;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static TestFinalize quote;


    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            return false;
        }
    });

    public static void printCollection(List<?> col) {//此方法使用了无限通配符
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
        boolean is64 = android.os.Process.is64Bit();
        String[] abac = Build.SUPPORTED_32_BIT_ABIS;
        String[] xcxx = Build.SUPPORTED_64_BIT_ABIS;
        String[] dsfsadf = Build.SUPPORTED_ABIS;
        android.util.Log.i("yulun ut_hash", "android.os.Build.DISPLAY is " + abac);
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
        android.util.Log.i("yulun 123", "werwer");
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

        Boolean sssss = new Boolean(true);
        if (sssss.equals(Boolean.TRUE)) {
            android.util.Log.i("yulun 123", "123");
        } else {
            android.util.Log.i("yulun 123", "456");
        }

        Boolean sssss1 = new Boolean("true");
        if (sssss1 == Boolean.TRUE) {
            android.util.Log.i("yulun 123", "123");
        } else {
            android.util.Log.i("yulun 123", "456");
        }

        TestAndroidFileDirectory.testPrintFile(this);
        TestMemory.testMemory(this);
//        findViewById(R.id.test1).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                startActivityForResult(new Intent(MainActivity.this, TestLeakActivity.class), 1);
//                android.util.Log.i("isEmulator", "get rom info is " +
//                        EmulatorChecker.romInfo(MainActivity.this) +
//                        " , isEmulator "+ EmulatorChecker.checkEmulator(MainActivity.this) +
//                        " , isEmulator double check is "+EmulatorChecker.isEmulator(MainActivity.this));
//            }
//        });
//        findViewById(R.id.test1).setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return false;
//            }
//        });
//        testJaveStackHead();
        //测试值传递 引用传递 url格式
//        new Test(1).main();
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        });
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        };
//        AbsInterface absInterface = new AbsInterface() {
//            @Override
//            public void sdf() {
//
//            }
//        };
//        absInterface.test();
//        try {
//            Class c = Class.forName("com.vdian.android.lib.testforgradle.abs.AbsInterface",false,getClassLoader());
////            AbsInterface test =(AbsInterface) c.newInstance();
//            AbsInterface test123= new AbsInterface() {
//                @Override
//                public void sdf() {
//
//                }
//            };
//            AbsInterface2 absInterface2 = new AbsInterface2() {
//                @Override
//                public void sdf() {
//                }
//            };
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                android.util.Log.i("test handler", "mHandler");
//            }
//        });
//        GenericA childTest = new GenericA();
//        GenericTest<String> parentTest = childTest;
//        parentTest.setTestParam("2");

//        android.util.Log.i("testGeneric", parentTest.getTestParam());
//        List list4 = new ArrayList();
//        list4.add("hello");
//        list4.add(0);

//        try {
//            TestPrintAbc testPrintAbc = new TestPrintAbc();
//            testPrintAbc.main();
//            UserManager manager = ProxyImpl.getService(UserManager.class);
//            manager.delUser("123");
//
//            manager.addUser(new ResuleCallbackImp<String>() {
//                @Override
//                public void realCallBack() {
//                    System.out.println("realCallBack");
//                }
//            });
//
//            Thread thread = new Thread();
//            UserManager userManager1 = ProxyImpl.getService(new UserManagerImpl());
//            userManager1.findUser("123");
//            System.out.println("proxy result is " + userManager1.findUser("123"));
//            synchronized (UserManager.class){
//
//            }
//
//            synchronized(userManager1.getClass()){
//
//            }
//
//            char[] ss = new char[1];
//            ss[0] = 1;
//            String ss1 = new String(ss);
//            byte[] bytes = ss1.getBytes();
//            System.out.println("中文是的长度: "+bytes.length);
////            Set
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


//        try {
//            GenericTest<String> childTest = new GenericTest();
//            Class refelex = childTest.getClass();
//            Field field = refelex.getDeclaredField("testParam");
//            field.setAccessible(true);
//            field.set(childTest, "123");
//            android.util.Log.i("test reflex", childTest.getTestParam() + "");
//        } catch (Exception e) {
//            android.util.Log.i("test reflex", e.toString());
//        }
//
        try {
            TestChild childTest = new TestChild(100);
            Class refelex = childTest.getClass().getSuperclass();
            Field field = refelex.getDeclaredField("a");

            field.setAccessible(true);
            field.set(childTest, 2);
            childTest.getClass().getMethods();
            childTest.getClass().getDeclaredMethods();
            android.util.Log.i("test reflex", childTest.getA() + "__" + field.getClass().getName());
//
//            Field field1 = refelex.getDeclaredField("c");
//            field1.setAccessible(true);
//            field1.set(null, 55);
//            android.util.Log.i("test reflex", field1.get(null) + "");
//
//            Method method = refelex.getDeclaredMethod("printxxx", String.class);
//            method.setAccessible(true);
//            method.invoke(childTest, "123");
//
//            Method method1 = refelex.getDeclaredMethod("printBBB", String.class);
//            method1.setAccessible(true);
//            method1.invoke(null, "12345");
//
//            Constructor constructor = refelex.getConstructor(int.class);
//            Test child2 = (Test) constructor.newInstance(100);
//            android.util.Log.i("test reflex child2", child2.getA() + "");
//
        } catch (Exception e) {
            e.printStackTrace();
        }
        //通配符 具体泛型 object
//        List<Object> list1 = new ArrayList<>();
//        list1.add("hello");
//        list1.add(0);
//
//        List<String> list2 = new ArrayList<>();
//        list2.add("world");
//
//        List<? extends Number> list3 = new ArrayList<Integer>();
//        Number a = Integer.valueOf(1);
//      Number number=   list3.get(0);
//        list3.add(null);

//        GenericTest<Boolean> c = new GenericTest();
//        c.setTestParam(true);

        TestReflexAction.test();
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

    public void addInterTest(List<? extends Number> list3) {
        for (Number object : list3) {
            System.out.println(object);
        }
    }

    public void testAnr(View view) {
        // 测试activity数据回调
//        Intent intent = new Intent(MainActivity.this, TestBActivity.class);
//        startActivity(intent);

        ThreadLocalTest threadLocalTest = new ThreadLocalTest();
        threadLocalTest.test();
        threadLocalTest.testWeakHashMap();

        // 自定义json解析
//        try {
//            JsonParser.testJsonParser();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        new BlTest().has("1");

//        testPush();
//        ReferenceTest.main(null);
//        new HashTest().test();
//        new FastJsonTest().main();
//        TestArrayMap.main(null);
//        TestSuperPatch.testSuperCall();
//        android.util.Log.i("testFinalize", "test anr111");
//        try {
//            Thread.sleep(30000);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        int a =1;
//        a= a+1;
//        TestFinalize aaa = null;
//        aaa.toString();
//        System.out.println("maxLen is " + lengthOfLongestSubstring1(" "));
//        System.out.println("maxLen is " + lengthOfLongestSubstring1("我的的的 "));
//        System.out.println("maxLen is " + lengthOfLongestSubstring1("bbbb"));
//        System.out.println("maxLen is " + lengthOfLongestSubstring1("pwwddddfffqbdd"));
//        testDrawableSize();
//        testAssetsSize();
//        testSegment();
//        TestDouble.isParallel(this);

//        List<Object> objectList = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            Object obj = new Object();
//            objectList.add(obj);
//            obj = null;
//        }
    }

    public void testPush() {
        //默认添加上scheme
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

    public boolean strInset(int start, int end, HashSet<Character> pool, String s) {
        boolean in = false;
        for (int i = start; i <= end; i++) {
            in = pool.contains(s.charAt(i));
            if (in)
                break;
        }
        return in;
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

    public void testJaveStackHead() {
//        String ss = "ss";
//        String ss1 = "ss";
//
//        android.util.Log.i("testStackHead == ", (ss == ss1) + "");
//        android.util.Log.i("testStackHead equals", "" + ss.equals(ss1));
////        View v = new View(this);
////        v.setTouchDelegate(new TouchDelegate());
//        //jdk 5.0开始的
//        Integer integer = 1;
//        Class<Integer> cls = Integer.TYPE;
//
//        Class cls1 = Activity.class;
//
//        byte b = 127;

        //测试值传递 引用传递 url格式
//        new Test(1).main();

//        TestFinalize testFinalize = new TestFinalize();
//        testFinalize = null;
//        System.gc();
//
////        try {
////            Thread.sleep(8000);
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
//
//        if (quote == null) {
//            android.util.Log.i("testFinalize", "testFinalize 对象被回收了");
//        } else {
//            android.util.Log.i("testFinalize", "testFinalize 对象没有被回收");
//        }
//
//        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
//            @Override
//            public void uncaughtException(Thread thread, Throwable throwable) {
//                Log.e("TXT", "uncaughtException: happen!", throwable);
//            }
//        });
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
        startActivity(new Intent(MainActivity.this, TestBActivity.class));
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
