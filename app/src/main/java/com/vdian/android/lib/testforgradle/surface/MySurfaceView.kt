package com.vdian.android.lib.testforgradle.surface

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.vdian.android.lib.testforgradle.util.LogUtil
import java.lang.Math.sin

/**
 * @author yulun
 * @since 2023-06-27 15:48
 */
class MySurfaceView : SurfaceView, SurfaceHolder.Callback {

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {}

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
    }

    companion object {
        private const val TAG = "MySurfaceView"
    }

    // 抽象的控制器
    private val surfaceHolder: SurfaceHolder = holder
    // 画布, 和surface绑定
    private var canvas: Canvas? = null

    // SurfaceView的绘制线程，必须要有自己的线程吗？ 是的，必现要有，例如GLSurfaceView也是如此
    private var mUpdateThread: UpdateThread? = null
    // 都是标记位
    private var mIsAttached = false
    private var mIsUpdateThreadStarted = false
    // 都是标记位
    @Volatile
    private var canDoDraw = false


    // 记录坐标 画笔等
    private var xx = 0f
    private var yy = 400f
    private val path = Path()
    private val paint = Paint()

    // 画布宽高
    private var mSurfaceWidth = 0
    private var mSurfaceHeight = 0

    init {
        surfaceHolder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        keepScreenOn = true
        path.moveTo(xx, yy)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.color = Color.RED
    }

    // 创建好了
    override fun surfaceCreated(holder: SurfaceHolder) {
        LogUtil.Log.d(TAG, "surfaceCreated")
        setCanDraw(true)
        clearSurface()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        LogUtil.Log.d(TAG, "surfaceChanged")
        mSurfaceWidth = width
        mSurfaceHeight = height
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        LogUtil.Log.d(TAG, "surfaceDestroyed")
        setCanDraw(false)
        // 注意当APP到后台时会 destroy Surface, 回到前台会重新调用 surfaceCreated
        // 因此这里不能移除回调，否则会黑屏
//        surfaceHolder.removeCallback(this)

    }


    override fun onAttachedToWindow() {
        LogUtil.Log.d(TAG, "onAttachedToWindow")
        super.onAttachedToWindow()
        mIsAttached = true
    }

    override fun onDetachedFromWindow() {
        LogUtil.Log.d(TAG, "onDetachedFromWindow")
        super.onDetachedFromWindow()
        mIsAttached = false
        stop()
    }

    // 控制是否可以绘制
    private fun setCanDraw(canDraw: Boolean) {
        canDoDraw = canDraw
    }

    // 开始绘制
    @Synchronized
    fun start() {
        if (mIsUpdateThreadStarted) return

        // 创建渲染控制线程
        if (null == mUpdateThread) {
            mUpdateThread = object : UpdateThread("Update Thread") {
                override fun run() {
                    try {
                        while (!isQuited
                            && !currentThread().isInterrupted
                        ) {
                            // 控制帧率
                            val cost: Long = 16 - draw()
                            if (isQuited) {
                                break
                            }
                            if (cost > 0) {
                                /// TODO 不做帧率控制，在销毁时总是碰到 时差不同步的问题，引起unlockCanvasAndPost失败，这里有好的方法吗？
                                // 说白了，就是darw频率太高，当有改变状态值时，上一次draw还在进行中，状态控制和draw是异步进行的,   其实换个角度想
                                // android设置16ms一帧也是要道理的，这里做个控制就行了
                                SystemClock.sleep(cost)
                            }
                        }
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    } finally {
                        this@MySurfaceView.stop()
                    }
                }
            }
        }
        mIsUpdateThreadStarted = true
        mUpdateThread?.start()
    }

    @Synchronized
    fun stop() {
        mIsUpdateThreadStarted = false
        if (null != mUpdateThread) {
            val thread: UpdateThread = mUpdateThread!!
            mUpdateThread = null
            thread.quit()
            thread.interrupt()
        }
    }

    // 绘制
    private fun draw(): Long {
        if (!canDoDraw) {
            return 0
        }
        if (mSurfaceWidth == 0
            || mSurfaceHeight == 0
        ) {
            return 0
        }
        if (!isShown) {
            clearSurface()
            return 0
        }
        val startTime = SystemClock.uptimeMillis()
        try {
            canvas = surfaceHolder.lockCanvas()
            canvas?.drawColor(Color.WHITE)
            canvas?.drawPath(path, paint)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (canvas != null) {
                try {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }

        xx += 1
        yy = (100 * sin(xx * 2 * Math.PI / 180) + 400).toFloat()
        path.lineTo(xx, yy)
        return SystemClock.uptimeMillis() - startTime
    }

    private fun clearSurface() {
        if (canDoDraw) {
            val canvas = holder.lockCanvas()
            if (null != canvas) {
                canvas.drawColor(
                    Color.TRANSPARENT,
                    PorterDuff.Mode.CLEAR
                )
                if (canDoDraw) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}