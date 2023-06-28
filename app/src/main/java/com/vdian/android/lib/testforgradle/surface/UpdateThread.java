package com.vdian.android.lib.testforgradle.surface;

/**
 * @author yulun
 * @since 2023-06-27 15:48
 */
public class UpdateThread extends Thread {
    volatile boolean mIsQuited;

    public UpdateThread(String name) {
        super(name);
    }

    public void quit() {
        mIsQuited = true;
    }

    public boolean isQuited() {
        return mIsQuited;
    }

    @Override
    public void run() {
        if (mIsQuited) return;
    }

}
