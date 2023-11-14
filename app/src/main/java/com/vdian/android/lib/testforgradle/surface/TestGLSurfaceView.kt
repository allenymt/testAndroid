package com.vdian.android.lib.testforgradle.surface

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import com.vdian.android.lib.testforgradle.surface.gl.Square
import com.vdian.android.lib.testforgradle.surface.gl.Triangle
import javax.microedition.khronos.opengles.GL10

/**
 * @author yulun
 * @since 2023-06-28 16:47
 * https://juejin.cn/post/7076751737461145630
 * https://juejin.cn/post/7077132016759603208/
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
        // 三角形
        lateinit var triangle: Triangle

        // 四边形
//        lateinit var square: Square

        // mvPMatrix是"Model View Projection Matrix"的缩写,代表模型矩阵、观察矩阵、投影矩阵
        private val mvPMatrix = FloatArray(16)
        // 投影矩阵
        private val projectionMatrix = FloatArray(16)
        // 观察矩阵
        private val viewMatrix = FloatArray(16)

        override fun onSurfaceCreated(
            gl: GL10?,
            config: javax.microedition.khronos.egl.EGLConfig?
        ) {
            GLES20.glClearColor(0.0f, 255.0f, 0.0f, 1.0f)
            triangle = Triangle()
//            square = Square()
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            val ratio: Float = width.toFloat() / height.toFloat()
            // 通过设置平截头体的范围表示投影矩阵,透视投影
            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

            // 正射投影
//            Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

            // 透视投影
//            Matrix.perspectiveM(projectionMatrix, 0, -ratio, ratio, -1f, 1f)

        }

        override fun onDrawFrame(unused: GL10) {
            // Redraw background color
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            // 设置观察矩阵
            // https://blog.csdn.net/kkae8643150/article/details/52805738 观察矩阵理解
            // 此时设置的参数含义：
            // 相机的坐标位置 ， 0，0，3 ,标识相机在坐标原点的正上方，3个单位的位置
            // 相机观测的位置：0，0，0，标识相机观测的位置在坐标原点
            // 相机的正方向向量：0，1，0，标识相机的正方向向量为Y轴正方向
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0.0f, 1.0f, 0.0f)
            // 计算投影矩阵、观察矩阵变换结果,保存到vPMatrix
            Matrix.multiplyMM(mvPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            // 模型矩阵、观察矩阵、投影矩阵的计算结果vPMatrix传递给其他物体，其他物体依据矩阵进行变换
            triangle.draw(mvPMatrix)
//            square.draw()
        }



    }
}

