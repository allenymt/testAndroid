package com.vdian.android.lib.testforgradle.surface

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.AttributeSet
import android.view.Gravity
import android.view.TextureView
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * @author yulun
 * @since 2023-06-28 17:06
 */
class MyTextureView : TextureView, TextureView.SurfaceTextureListener {
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    // 相机
    private var camera: Camera? = null

    private fun init() {
        surfaceTextureListener = this
    }

    // SurfaceTexture 不同于 SurfaceView 会将图像显示在屏幕上，SurfaceTexture 对图像流的处理并不直接显示，而是转为 GL 外部纹理。
    //主要用于纹理处理，然后把处理好的纹理输出到Camera或者MediaPlayer上
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        camera = Camera.open()
        val previewSize: Camera.Size = camera?.parameters?.previewSize ?: return
        layoutParams = FrameLayout.LayoutParams(
            previewSize.width, previewSize.height, Gravity.CENTER
        )
        kotlin.runCatching {
            // 设置预览显示
            camera?.setPreviewTexture(surface)
        }
        // 开始预览
        camera?.startPreview()
        rotation = 90.0f
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        camera?.stopPreview()
        camera?.release()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

    }
}