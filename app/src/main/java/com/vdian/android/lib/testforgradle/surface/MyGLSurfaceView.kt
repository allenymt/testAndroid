package com.vdian.android.lib.testforgradle.surface

import android.content.Context
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import javax.microedition.khronos.opengles.GL10

/**
 * @author yulun
 * @since 2023-06-28 16:47
 */
class MyGLSurfaceView : GLSurfaceView {
    private val renderer: MyGLRenderer

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {}

    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)
        renderer = MyGLRenderer()
        setRenderer(renderer)
        // 该设置可防止系统在调用 requestRender() 之前重新绘制 GLSurfaceView 帧，更为高效。
        // Render the view only when there is a change in the drawing data
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    class MyGLRenderer : Renderer {
//        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
//            // Set the background frame color
//            GLES20.glClearColor(0.0f, 255.0f, 0.0f, 1.0f)
//        }

        override fun onSurfaceCreated(
            gl: GL10?,
            config: javax.microedition.khronos.egl.EGLConfig?
        ) {
            GLES20.glClearColor(0.0f, 255.0f, 0.0f, 1.0f)
        }


        override fun onDrawFrame(unused: GL10) {
            // Redraw background color
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        }


        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
        }
    }
}

