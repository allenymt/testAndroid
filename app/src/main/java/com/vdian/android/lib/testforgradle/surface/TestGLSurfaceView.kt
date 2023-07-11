package com.vdian.android.lib.testforgradle.surface

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.vdian.android.lib.testforgradle.surface.gl.Square
import com.vdian.android.lib.testforgradle.surface.gl.Triangle
import javax.microedition.khronos.opengles.GL10

/**
 * @author yulun
 * @since 2023-06-28 16:47
 */
class TestGLSurfaceView : GLSurfaceView {
    private val renderer: TestGLRenderer

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {}

    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)
        renderer = TestGLRenderer()
        setRenderer(renderer)
        // 该设置可防止系统在调用 requestRender() 之前重新绘制 GLSurfaceView 帧，更为高效。
        // Render the view only when there is a change in the drawing data
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    class TestGLRenderer : Renderer {
//        lateinit var triangle: Triangle
        lateinit var square: Square
        override fun onSurfaceCreated(
            gl: GL10?,
            config: javax.microedition.khronos.egl.EGLConfig?
        ) {
            GLES20.glClearColor(0.0f, 255.0f, 0.0f, 1.0f)
//            triangle = Triangle()
            square = Square()
        }


        override fun onDrawFrame(unused: GL10) {
            // Redraw background color
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//            triangle.draw()
            square.draw()
        }


        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
        }
    }
}

