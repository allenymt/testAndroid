package com.vdian.android.lib.testforgradle.binder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

/**
 * @author yulun
 * @sinice 2021-06-21 20:09
 */
public class IComputerService extends Service {

    private ICompute.Stub mBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new ICompute.Stub() {
            @Override
            public int add(int a, int b) throws RemoteException {
                return a+b;
            }
        };
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
