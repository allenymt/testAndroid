package com.vdian.android.lib.testforgradle.self_view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.View

/**
 * @author yulun
 * @sinice 2021-04-14 16:08
 */
class MyViewRed : View{
    val TAG = "yulun"
    var measureTimes=0
    var layoutTimes=0
    var drawTimes=0
    constructor(context: Context) : super(context) {

    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
    }

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr) {

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        android.util.Log.i(TAG,"onMeasure is ${++measureTimes}")
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        android.util.Log.i(TAG,"onLayout is ${++layoutTimes}")
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawColor(Color.RED)
        android.util.Log.i(TAG,"onDraw is ${++drawTimes} $canvas")
    }
}