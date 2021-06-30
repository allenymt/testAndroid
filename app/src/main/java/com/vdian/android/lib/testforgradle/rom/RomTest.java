package com.vdian.android.lib.testforgradle.rom;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import androidx.core.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author yulun
 * @sinice 2020-05-19 16:24
 */
public class RomTest {
    private static Boolean isParallel = null;
    private static Boolean isHook = null;
    private static Boolean isRoot = null;
    private static Boolean isEmulator = null;

    static String getNetworkDetailState(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    return networkInfo.getDetailedState().name();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    static boolean isNetworkBlocked(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    return networkInfo.getDetailedState() == NetworkInfo.DetailedState.BLOCKED;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                NetworkInfo networkInfo = connectivity.getActiveNetworkInfo();
                if (networkInfo != null) {
                    if (networkInfo.isAvailable() || networkInfo.isConnectedOrConnecting()) {
                        return true;
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean isDeviceIdle(Context context) {
        try {
            if (Build.VERSION.SDK_INT < 23) {
                return false;
            }
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                boolean isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
                return powerManager.isDeviceIdleMode() && !isIgnoringOptimizations;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 是否是双开
     */
    static boolean isParallel(Context context) {
        if (isParallel != null) {
            return isParallel;
        }
        isParallel = checkPrivateDataFile(context) || checkPkgName(context) || checkProcess(context);
        return isParallel;
    }

    /**
     * 根据私有文件路径判断是否双开
     *
     * @param context 上下文
     * @return 是否双开
     */
    private static boolean checkPrivateDataFile(Context context) {
        try {
            String path = context.getFilesDir().getAbsolutePath();
            String pakName = context.getPackageName();

            if (checkQihooMagicDataPlugin(context, pakName)) {
                return true;
            }

            if (path.startsWith("/data/data/" + pakName)) {
                return false;
            }

            if (path.startsWith("/data/user/0/" + pakName)) {
                return false;
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 根据进程判断是否双开
     *
     * @param context 上下文
     * @return 是否双开
     */
    private static boolean checkProcess(Context context) {
        String filter = ShellCmdExecutor.getUidStrFormat();
        String result = ShellCmdExecutor.exec("ps");
        if (result == null || result.isEmpty()) {
            return false;
        }

        String[] lines = result.split("\n");

        if (lines == null || lines.length <= 0) {
            return false;
        }
        int exitDirCount = 0;

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(filter)) {
                int pkgStartIndex = lines[i].lastIndexOf(" ");
                String processName = lines[i].substring(pkgStartIndex <= 0
                        ? 0 : pkgStartIndex + 1, lines[i].length());
                File dataFile = new File(String.format("/data/data/%s",
                        processName, Locale.CHINA));
                if (dataFile.exists()) {
                    exitDirCount++;
                }
                if (checkQihooMagicProcessEnv(context, processName)) {
                    return true;
                }
            }
        }
        return exitDirCount > 1;
    }

    /**
     * 检测是否允许在360分身大师双开环境
     *
     * @param context 上下文
     * @param process 360分身环境有一个daemon进程
     * @return 是否360分身环境
     */
    private static boolean checkQihooMagicProcessEnv(Context context, String process) {
        if ("daemon".equals(process)) {
            if (PackageUtil.isInstalled(context, "com.qihoo.magic")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断360分身大师插件目录是否存在当前包分身
     *
     * @param context 上下文
     * @param pkgName 当前包名
     * @return 是否360分身环境
     */
    private static boolean checkQihooMagicDataPlugin(Context context, String pkgName) {
        File file = new File("/data/data/com.qihoo.magic/Plugin", pkgName);
        if (file.exists() && file.canRead()) {
            return true;
        }
        return false;
    }

    /**
     * 根据包名数量判断是否双开，双开特征：数量>1
     *
     * @param context 上下文
     * @return 当前是否双开
     */
    private static boolean checkPkgName(Context context) {
        try {
            if (context == null) {
                return false;
            }
            int count = 0;
            String packageName = context.getPackageName();
            List<PackageInfo> pkgs = PackageUtil.getInstalledPackages(context);
            for (PackageInfo info : pkgs) {
                if (packageName.equals(info.packageName)) {
                    count++;
                }
            }
            return count > 1;
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
        return false;
    }

    /**
     * 是否装了hook类软件
     */
    static boolean isHook(Context context) {
        if (isHook != null) {
            return isHook;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            List<ApplicationInfo> applicationInfoList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            if (applicationInfoList != null && applicationInfoList.size() > 0) {
                for (ApplicationInfo applicationInfo : applicationInfoList) {
                    if (applicationInfo.packageName.equals("de.robv.android.xposed.installer")) {
                        isHook = true;
                        return true;
                    }
                    if (applicationInfo.packageName.equals("com.saurik.substrate")) {
                        isHook = true;
                        return true;
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        isHook = false;
        return false;
    }

    /**
     * 是否root
     */
    static boolean isRoot(Context context) {
        if (isRoot != null) {
            return isRoot;
        }
        try {
            isRoot = RootDetector.isDeviceRooted();
            return isRoot;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        isRoot = false;
        return false;
    }

    /**
     * 是否是模拟器 参考地址
     * https://github.com/Labmem003/anti-counterfeit-android
     */
    public static boolean isEmulator(Context context) {
        if (isEmulator != null) {
            return isEmulator;
        }
        try {
            isEmulator = EmulatorDetector.isEmulator(context);
            return isEmulator;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        isEmulator = false;
        return false;
    }

    /**
     * Root检测
     */
    static class RootDetector {
        static boolean isDeviceRooted() {
            if (checkDeviceDebuggable()) {
                return true;
            }
            if (checkSuperuserApk()) {
                return true;
            }
            if (checkRootPathSU()) {
                return true;
            }
            if (checkRootWhichSU()) {
                return true;
            }
            if (checkBusybox()) {
                return true;
            }
            return false;
        }

        static boolean checkDeviceDebuggable() {
            String buildTags = android.os.Build.TAGS;
            if (buildTags != null && buildTags.contains("test-keys")) {
                return true;
            }
            return false;
        }

        static boolean checkSuperuserApk() {
            try {
                File file = new File("/system/app/Superuser.apk");
                if (file.exists()) {
                    return true;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return false;
        }

        static boolean checkRootPathSU() {
            File file = null;
            final String kSuSearchPaths[] = {"/system/bin/", "/system/xbin/", "/system/sbin/", "/sbin/", "/vendor/bin/"};
            try {
                for (int i = 0; i < kSuSearchPaths.length; i++) {
                    file = new File(kSuSearchPaths[i] + "su");
                    if (file != null && file.exists()) {
                        return true;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return false;
        }

        static boolean checkRootWhichSU() {
            String[] strCmd = new String[]{"which", "su"};
            ArrayList<String> execResult = executeCommand(strCmd);
            return execResult != null;
        }

        static synchronized boolean checkBusybox() {
            try {
                String[] strCmd = new String[]{"busybox", "df"};
                ArrayList<String> execResult = executeCommand(strCmd);
                return execResult != null;
            } catch (Exception e) {
                return false;
            }
        }

        static ArrayList<String> executeCommand(String[] shellCmd) {
            BufferedReader in = null;
            try {
                Process process = Runtime.getRuntime().exec(shellCmd);
                in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                ArrayList<String> fullResponse = new ArrayList<>();
                String line = null;
                while ((line = in.readLine()) != null) {
                    fullResponse.add(line);
                }
                if (fullResponse.size() == 0) {
                    return null;
                }
                return fullResponse;
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }


    /**
     * 模拟器检测
     */
    static class EmulatorDetector {
        private static int rating = -1;

        static boolean isEmulator(Context context) {
            if (isEmulatorAbsolutely(context)) {
                return true;
            }
            int newRating = 0;
            if (rating < 0) {
                if (Build.PRODUCT == null ||
                        Build.PRODUCT.contains("sdk") ||
                        Build.PRODUCT.contains("Andy") ||
                        Build.PRODUCT.contains("ttVM_Hdragon") ||
                        Build.PRODUCT.contains("google_sdk") ||
                        Build.PRODUCT.contains("Droid4X") ||
                        Build.PRODUCT.contains("nox") ||
                        Build.PRODUCT.contains("sdk_x86") ||
                        Build.PRODUCT.contains("sdk_google") ||
                        Build.PRODUCT.contains("vbox86p") ||
                        Build.PRODUCT.contains("aries")) {
                    newRating++;
                }

                if (Build.MANUFACTURER == null ||
                        Build.MANUFACTURER.equals("unknown") ||
                        Build.MANUFACTURER.equals("Genymotion") ||
                        Build.MANUFACTURER.contains("Andy") ||
                        Build.MANUFACTURER.contains("MIT") ||
                        Build.MANUFACTURER.contains("nox") ||
                        Build.MANUFACTURER.contains("TiantianVM")) {
                    newRating++;
                }

                if (Build.BRAND == null ||
                        Build.BRAND.equals("generic") ||
                        Build.BRAND.equals("generic_x86") ||
                        Build.BRAND.equals("TTVM") ||
                        Build.BRAND.contains("Andy")) {
                    newRating++;
                }

                if (Build.DEVICE == null ||
                        Build.DEVICE.contains("generic") ||
                        Build.DEVICE.contains("generic_x86") ||
                        Build.DEVICE.contains("Andy") ||
                        Build.DEVICE.contains("ttVM_Hdragon") ||
                        Build.DEVICE.contains("Droid4X") ||
                        Build.DEVICE.contains("nox") ||
                        Build.DEVICE.contains("generic_x86_64") ||
                        Build.DEVICE.contains("vbox86p") ||
                        Build.DEVICE.contains("aries")) {
                    newRating++;
                }

                if (Build.MODEL == null ||
                        Build.MODEL.equals("sdk") ||
                        Build.MODEL.contains("Emulator") ||
                        Build.MODEL.equals("google_sdk") ||
                        Build.MODEL.contains("Droid4X") ||
                        Build.MODEL.contains("TiantianVM") ||
                        Build.MODEL.contains("Andy") ||
                        Build.MODEL.equals("Android SDK built for x86_64") ||
                        Build.MODEL.equals("Android SDK built for x86")) {
                    newRating++;
                }

                if (Build.HARDWARE == null ||
                        Build.HARDWARE.equals("goldfish") ||
                        Build.HARDWARE.equals("vbox86") ||
                        Build.HARDWARE.contains("nox") ||
                        Build.HARDWARE.contains("ttVM_x86")) {
                    newRating++;
                }

                if (Build.FINGERPRINT == null ||
                        Build.FINGERPRINT.contains("generic/sdk/generic") ||
                        Build.FINGERPRINT.contains("generic_x86/sdk_x86/generic_x86") ||
                        Build.FINGERPRINT.contains("Andy") ||
                        Build.FINGERPRINT.contains("ttVM_Hdragon") ||
                        Build.FINGERPRINT.contains("generic_x86_64") ||
                        Build.FINGERPRINT.contains("generic/google_sdk/generic") ||
                        Build.FINGERPRINT.contains("vbox86p") ||
                        Build.FINGERPRINT.contains("test-keys") ||
                        Build.FINGERPRINT.contains("generic/vbox86p/vbox86p")) {
                    newRating++;
                }

                try {
                    //蓝牙
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter == null) {
                        newRating += 10;
                    } else {
                        // 如果有蓝牙不一定是有效的。获取蓝牙名称，若为null 则默认为模拟器
                        String name = bluetoothAdapter.getName();
                        if (TextUtils.isEmpty(name)) {
                            newRating += 10;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                try {
                    //光感
                    SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                    if (sensorManager == null) {
                        newRating += 10;
                    } else {
                        Sensor sensor8 = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT); //光
                        if (null == sensor8) {
                            newRating += 10;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                try {
                    File sharedFolder = new File(Environment
                            .getExternalStorageDirectory().toString()
                            + File.separatorChar
                            + "windows"
                            + File.separatorChar
                            + "BstSharedFolder");

                    if (sharedFolder.exists()) {
                        newRating += 10;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final CountDownLatch countDownLatch = new CountDownLatch(1);
                final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
                ThreadUtils.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //必须在主线程获取，否则可能crash
                            String opengl = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER);
                            if (opengl != null) {
                                if (opengl.contains("Bluestacks") || opengl.contains("Translator")) {
                                    atomicBoolean.set(true);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            countDownLatch.countDown();
                        }
                    }
                });
                try {
                    countDownLatch.await(200, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (atomicBoolean.get()) {
                    newRating += 10;
                }

                rating = newRating;
            }
            return rating > 3;//不能再少了，否则有可能误判，若增减了新的嫌疑度判定属性，要重新评估该值
        }

        static boolean isEmulatorAbsolutely(Context context) {
            if (mayOnEmulatorViaQEMU(context)) {
                return true;
            }
//            if (mayOnEmulatorViaTelephonyDeviceId(context)) {
//                return true;
//            }
            if (mayIntelOrAmd()) {
                return true;
            }
            if (Build.PRODUCT == null ||
                    Build.PRODUCT.contains("sdk") ||
                    Build.PRODUCT.contains("sdk_x86") ||
                    Build.PRODUCT.contains("sdk_google") ||
                    Build.PRODUCT.contains("Andy") ||
                    Build.PRODUCT.contains("Droid4X") ||
                    Build.PRODUCT.contains("nox") ||
                    Build.PRODUCT.contains("vbox86p") ||
                    Build.PRODUCT.contains("aries")) {
                return true;
            }
            if (Build.MANUFACTURER == null ||
                    Build.MANUFACTURER.equals("Genymotion") ||
                    Build.MANUFACTURER.contains("Andy") ||
                    Build.MANUFACTURER.contains("nox") ||
                    Build.MANUFACTURER.contains("TiantianVM")) {
                return true;
            }
            if (Build.BRAND == null ||
                    Build.BRAND.contains("Andy")) {
                return true;
            }
            if (Build.DEVICE == null ||
                    Build.DEVICE.contains("Andy") ||
                    Build.DEVICE.contains("Droid4X") ||
                    Build.DEVICE.contains("nox") ||
                    Build.DEVICE.contains("vbox86p") ||
                    Build.DEVICE.contains("aries")) {
                return true;
            }
            if (Build.MODEL == null ||
                    Build.MODEL.contains("Emulator") ||
                    Build.MODEL.equals("google_sdk") ||
                    Build.MODEL.contains("Droid4X") ||
                    Build.MODEL.contains("TiantianVM") ||
                    Build.MODEL.contains("Andy") ||
                    Build.MODEL.equals("Android SDK built for x86_64") ||
                    Build.MODEL.equals("Android SDK built for x86")) {
                return true;
            }
            if (Build.HARDWARE == null ||
                    Build.HARDWARE.equals("vbox86") ||
                    Build.HARDWARE.contains("nox") ||
                    Build.HARDWARE.contains("ttVM_x86")) {
                return true;
            }
            if (Build.FINGERPRINT == null ||
                    Build.FINGERPRINT.contains("generic/sdk/generic") ||
                    Build.FINGERPRINT.contains("generic_x86/sdk_x86/generic_x86") ||
                    Build.FINGERPRINT.contains("Andy") ||
                    Build.FINGERPRINT.contains("ttVM_Hdragon") ||
                    Build.FINGERPRINT.contains("generic/google_sdk/generic") ||
                    Build.FINGERPRINT.contains("vbox86p") ||
                    Build.FINGERPRINT.contains("test-keys") ||
                    Build.FINGERPRINT.contains("generic/vbox86p/vbox86p")) {
                return true;
            }
            return false;
        }

        static boolean mayOnEmulatorViaQEMU(Context context) {
            final String[] known_files = {
                    "/system/lib/libc_malloc_debug_qemu.so",
                    "/sys/qemu_trace"
//                    "/system/bin/qemu-props" //这里不能有，三星s6也能检索到这个文件，文章参考https://www.codercto.com/a/34627.html
            };
            for (String pipe : known_files) {
                File qemu_file = new File(pipe);
                if (qemu_file.exists()) {
                    return true;
                }
            }
            return false;
        }

//        static boolean mayOnEmulatorViaTelephonyDeviceId(Context context) {
//            try {
//                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//                if (tm == null) {
//                    return false;
//                }
//
//                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
//                    String deviceId = tm.getDeviceId();
//                    if (deviceId == null || deviceId.length() == 0) {
//                        return false;
//                    }
//
//                    for (int i = 0; i < deviceId.length(); i++) {
//                        if (deviceId.charAt(i) != '0') {
//                            return false;
//                        }
//                    }
//                    return true;
//                }
//            } catch (Throwable e) {
//                e.printStackTrace();
//            }
//            return false;
//        }

        static boolean mayIntelOrAmd() {
            try {
                String[] args = {"/system/bin/cat", "/proc/cpuinfo"};
                ProcessBuilder cmd = new ProcessBuilder(args);
                Process process = cmd.start();
                StringBuffer sb = new StringBuffer();
                String readLine = "";
                BufferedReader responseReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "utf-8"));
                while ((readLine = responseReader.readLine()) != null) {
                    sb.append(readLine);
                }
                responseReader.close();
                String result = sb.toString().toLowerCase();
                if ((result.contains("intel") || result.contains("amd"))) {
                    return true;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return false;
        }
    }
}
