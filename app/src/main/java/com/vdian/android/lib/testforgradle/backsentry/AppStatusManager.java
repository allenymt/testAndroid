package com.vdian.android.lib.testforgradle.backsentry;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author yulun
 * @since 2022-08-15 11:46
 */
public class AppStatusManager {

    private volatile static AppStatusManager sInstance;

    private Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks;

    private boolean hasActivityResume = false;

    private boolean isForeground = false;

    private List<AppStatusCallback> mAppStatusCallbacks = new ArrayList<>();

    private int activityResumeCount = 0;

    private HashMap<String, Integer> activityNumMap = new HashMap<>();

    private AppStatusManager() {
    }

    public static AppStatusManager getInstance() {
        if (sInstance == null) {
            synchronized (AppStatusManager.class) {
                if (sInstance == null) {
                    sInstance = new AppStatusManager();
                }
            }
        }
        return sInstance;
    }

    //当前app是否在前台
    public boolean isApplicationForeground() {
        return activityResumeCount >= 1;
    }

    //当前app是否在后台
    public boolean isApplicationBackground() {
        return activityResumeCount == 0 && !hasActivityResume;
    }

    public int getActivityResumeCount() {
        return activityResumeCount;
    }

    public void register(Application application) {
        mActivityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                putActivityByName(activity.getClass().getName());
                activityResumeCount++;

                //第一个activity到前台
                if (activityResumeCount == 1) {
                    List<AppStatusCallback> callbacks = new ArrayList<>(mAppStatusCallbacks);
                    for (AppStatusCallback callback : callbacks) {
                        if (callback == null)
                            continue;
                        callback.onApplicationForeground();
                    }
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
                hasActivityResume = true;
                if (!isForeground) {
                    isForeground = true;
                    List<AppStatusCallback> callbacks = new ArrayList<>(mAppStatusCallbacks);
                    for (AppStatusCallback callback : callbacks) {
                        if (callback == null)
                            continue;
                        callback.onForeground();
                    }
                }

            }

            @Override
            public void onActivityPaused(Activity activity) {
                hasActivityResume = false;
            }

            @Override
            public void onActivityStopped(Activity activity) {
                if (removeActivityByName(activity.getClass().getName())) {
                    activityResumeCount--;
                }

                if (activityResumeCount == 0) {
                    // 没有activity存活了
                    List<AppStatusCallback> callbacks = new ArrayList<>(mAppStatusCallbacks);
                    for (AppStatusCallback callback : callbacks) {
                        if (callback == null)
                            continue;
                        callback.onApplicationBackground();
                    }
                }

                if (!hasActivityResume) {
                    isForeground = false;
                    List<AppStatusCallback> callbacks = new ArrayList<>(mAppStatusCallbacks);
                    for (AppStatusCallback callback : callbacks) {
                        if (callback == null)
                            continue;
                        callback.onBackground();
                    }
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        };

        application.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);

    }

    private boolean putActivityByName(String activityName) {
        Integer num = activityNumMap.get(activityName);
        if (num == null) {
            num = 1;
        } else {
            num++;
        }
        activityNumMap.put(activityName, num);
        return true;
    }

    private boolean removeActivityByName(String activityName) {
        Integer num = activityNumMap.get(activityName);
        if (num == null || num ==0) {
            return false;
        } else {
            num--;
        }
        activityNumMap.put(activityName, num);
        return true;
    }

    public void unregister(Application application) {
        application.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
    }

    public synchronized void addAppStatusCallback(AppStatusCallback callback) {
        if (callback == null)
            return;
        mAppStatusCallbacks.add(callback);
    }

    public synchronized void removeAppStatusCallback(AppStatusCallback callback) {
        if (callback == null)
            return;
        mAppStatusCallbacks.remove(callback);
    }


    /**
     * 这是页面级的，应用级的重新开一个，
     */
    public interface AppStatusCallback {

        void onForeground();

        void onBackground();

        void onApplicationForeground();

        void onApplicationBackground();
    }

}
