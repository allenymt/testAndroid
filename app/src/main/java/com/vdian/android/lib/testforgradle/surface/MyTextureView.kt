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

    private var camera: Camera? = null

    private fun init() {
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        camera = Camera.open()
        val previewSize: Camera.Size = camera?.parameters?.previewSize ?: return
        layoutParams = FrameLayout.LayoutParams(
            previewSize.width, previewSize.height, Gravity.CENTER
        )
        kotlin.runCatching {
            camera?.setPreviewTexture(surface)
        }
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