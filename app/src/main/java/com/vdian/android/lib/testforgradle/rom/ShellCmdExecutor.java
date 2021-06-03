package com.vdian.android.lib.testforgradle.rom;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Shell命名执行器
 *
 * @author xiongxunxiang
 * @since 2020-04-19
 */
public class ShellCmdExecutor {

    /**
     * 执行指定shell命令，比如cat等等
     *
     * @param command shell命令
     * @return 执行结果
     * @deprecated use {@link #executeCMD(Context, String)} instead.
     */
    @Deprecated
    public static String exec(String command) {
        BufferedOutputStream bufferedOutputStream = null;
        BufferedInputStream bufferedInputStream = null;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("sh");
            bufferedOutputStream = new BufferedOutputStream(process.getOutputStream());

            bufferedInputStream = new BufferedInputStream(process.getInputStream());
            bufferedOutputStream.write(command.getBytes());
            bufferedOutputStream.write('\n');
            bufferedOutputStream.flush();
            bufferedOutputStream.close();

            process.waitFor();

            String outputStr = getStrFromBufferInputSteam(bufferedInputStream);
            return outputStr;
        } catch (Exception e) {
            return null;
        } finally {
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String getStrFromBufferInputSteam(BufferedInputStream bufferedInputStream) {
        if (null == bufferedInputStream) {
            return "";
        }
        int BUFFER_SIZE = 512;
        byte[] buffer = new byte[BUFFER_SIZE];
        StringBuilder result = new StringBuilder();
        try {
            while (true) {
                int read = bufferedInputStream.read(buffer);
                if (read > 0) {
                    result.append(new String(buffer, 0, read));
                }
                if (read < BUFFER_SIZE) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    /**
     * 获取当前uid
     *
     * @return uid linux uid
     */
    public static String getUidStrFormat() {
        String filter = exec("cat /proc/self/cgroup");
        if (filter == null || filter.length() == 0) {
            return null;
        }

        int uidStartIndex = filter.lastIndexOf("uid");
        int uidEndIndex = filter.lastIndexOf("/pid");
        if (uidStartIndex < 0) {
            return null;
        }
        if (uidEndIndex <= 0) {
            uidEndIndex = filter.length();
        }

        try {
            filter = filter.substring(uidStartIndex + 4, uidEndIndex);
            String strUid = filter.replaceAll("\n", "");
            if (StringUtil.isNumber(strUid)) {
                int uid = Integer.valueOf(strUid);
                filter = String.format("u0_a%d", uid - 10000);
                return filter;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取prop属性值
     *
     * @param context
     * @param cmd     要执行的命令
     * @return 运行结果
     */
    public static ArrayList<String> executeCMD(Context context, String cmd) {
        if (MemoryUtil.isLowMemory(context)) {
            return new ArrayList(Arrays.asList("unknown(low memory)"));
        } else {
            BufferedReader reader = null;
            BufferedReader errReader = null;
            ArrayList resultList = new ArrayList();
            try {
                String shCmd = "/system/bin/sh";
                if (!(new File(shCmd)).exists() || !(new File(shCmd)).canExecute()) {
                    shCmd = "sh";
                }

                ArrayList<String> var24;
                (var24 = new ArrayList(Arrays.asList(shCmd, "-c"))).add(cmd);
                Process process = Runtime.getRuntime().exec(var24.toArray(new String[3]));
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    resultList.add(line);
                }

                errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                while ((line = errReader.readLine()) != null) {
                    resultList.add(line);
                }
                return resultList;
            } catch (Throwable var20) {
                var20.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException var17) {
                        var17.printStackTrace();
                    }
                }

                if (errReader != null) {
                    try {
                        errReader.close();
                    } catch (IOException var16) {
                        var16.printStackTrace();
                    }
                }

            }

            return null;
        }
    }

}
