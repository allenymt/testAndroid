package com.vdian.android.lib.testforgradle.touch;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * @author yulun
 * @sinice 2021-04-09 16:12
 */
public class TextTouchTextView extends AppCompatTextView {

    public TextTouchTextView(Context context) {
        super(context);
    }

    public TextTouchTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TextTouchTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Thread.dumpStack();
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Thread.dumpStack();
        return true;
    }
}