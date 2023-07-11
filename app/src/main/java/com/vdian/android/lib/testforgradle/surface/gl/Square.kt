package com.vdian.android.lib.testforgradle.surface.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * @author yulun
 * @since 2023-07-11 15:19
 * 矩形
 */
class Square {
    // 每个顶点的坐标数
    private val COORDS_PER_VERTEX = 3

    // 顶点坐标
    private var squareCoords = floatArrayOf(
        -0.5f, 0.5f, 0.0f,      // top left
        -0.5f, -0.5f, 0.0f,      // bottom left
        0.5f, -0.5f, 0.0f,      // bottom right
        0.5f, 0.5f, 0.0f       // top right
    )


    // 四个顶点的缓冲数组
    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(squareCoords.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(squareCoords)
                position(0)
            }

    // 四个顶点的绘制顺序数组
    private val drawOrder = shortArrayOf(0, 1, 2, 3, 0, 2)

    // 四个顶点绘制顺序数组的缓冲数组
    private val drawListBuffer: ShortBuffer =
        ByteBuffer.allocateDirect(drawOrder.size * 2).order(ByteOrder.nativeOrder())
            .asShortBuffer().apply {
                put(drawOrder)
                position(0)
            }

    /**
     * 顶点着色器代码;
     * 暂时将顶点着色器的源代码硬编码在C风格字符串中
     */
    private val vertexShaderCode =
        "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = vPosition;" +
                "}"

    /**
     * 片段着色器代码
     */
    private val fragmentShaderCode =
        "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"

    // 设置颜色(分别代表red, green, blue and alpha)
    private val color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)

    /**
     * 着色器程序ID引用
     */
    private var mProgram: Int

    init {
        // 编译顶点着色器和片段着色器
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        // glCreateProgram函数创建一个着色器程序，并返回新创建程序对象的ID引用
        mProgram = GLES20.glCreateProgram().also {
            // 把顶点着色器添加到程序对象
            GLES20.glAttachShader(it, vertexShader)
            // 把片段着色器添加到程序对象
            GLES20.glAttachShader(it, fragmentShader)
            // 连接并创建一个可执行的OpenGL ES程序对象
            GLES20.glLinkProgram(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        // glCreateShader函数创建一个顶点着色器或者片段着色器,并返回新创建着色器的ID引用
        val shader = GLES20.glCreateShader(type)
        // 把着色器和代码关联,然后编译着色器
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    // 步长，指定相邻两个顶点之间的字节跨度，如果为 0，则表示顶点数据是紧密排列的。 这里没怎么理解
    private val vertexStride: Int = COORDS_PER_VERTEX * 4

    fun draw() {
        // 激活着色器程序 Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)
        // 获取顶点着色器中的vPosition变量(因为之前已经编译过着色器代码,所以可以从着色器程序中获取);用唯一ID表示
        val position = GLES20.glGetAttribLocation(mProgram, "vPosition")
        // 允许操作顶点对象position
        GLES20.glEnableVertexAttribArray(position)
        // 将顶点数据传递给position指向的vPosition变量
        GLES20.glVertexAttribPointer(
            position, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
            false, vertexStride, vertexBuffer
        )
        // 获取片段着色器中的vColor变量
        val colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        // 通过colorHandle设置绘制的颜色值
        GLES20.glUniform4fv(colorHandle, 1, color, 0)
        // 按drawListBuffer中指定的顺序绘制四边形
        // 由于顶点数据和片段数据都已经绑定过了，这里只需要控制绘制的类型即可，
        // 参考demo Triangle里，是通过GLES20.glDrawArrays()方法来绘制的
        // 这里是控制绘制顺序,类型是画三角形，每个三角形至少三个顶点，因此drawOrder的数量肯定是3的倍数
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer
        )
        // 操作完后,取消允许操作顶点对象position
        GLES20.glDisableVertexAttribArray(position)
    }
}
