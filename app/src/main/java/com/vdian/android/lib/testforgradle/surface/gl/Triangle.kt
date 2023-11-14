package com.vdian.android.lib.testforgradle.surface.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * @author yulun
 * @since 2023-07-11 14:40
 * 三角形
 */
class Triangle {
    // 三角形三个点的坐标值(逆时针方向,在3D坐标系中,方向决定了哪面是正面)
    // 这里的逆时针作何理解？？
    private var triangleCoords = floatArrayOf(
        0.0f, 0.5f, 0.0f,      // top
        -0.5f, -0.5f, 0.0f,    // bottom left
        0.5f, -0.5f, 0.0f      // bottom right
    )
    // 每个顶点的坐标数
    private val COORDS_PER_VERTEX = 3

    // 设置颜色(分别代表red, green, blue and alpha)
    private val color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)

    //顶点数组对象：Vertex Array Object，VAO，表示存放顶点的数组，即例子中的triangleCoords；
    //顶点缓冲对象：Vertex Buffer Object，VBO，表示存放顶点缓冲的数据，即例子中的FloatBuffer对象vertexBuffer；
    //索引缓冲对象：Element Buffer Object，EBO或Index Buffer Object，IBO，表示存放顶点索引的数组，3.2小节会涉及到，用于描述顶点之间的顺序来重复使用顶点。
    //顶点缓冲对象
    private var vertexBuffer: FloatBuffer =
        // 坐标点的数目 * float所占字节
        ByteBuffer.allocateDirect(triangleCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                // 把坐标添加到FloatBuffer
                put(triangleCoords)
                // 设置buffer的位置为起始点0
                position(0)
            }

    /// 顶点着色器和片段着色器的区别，它们分别负责的功能是什么？
    // 顶点着色器和片段着色器是OpenGL渲染管线中的两个重要组成部分。
    //
    //顶点着色器是在每个顶点上运行的程序，其主要功能是将输入的顶点数据转换为屏幕上的像素。它可以对顶点进行变换、投影、光照计算等操作，并将处理后的结果传递给下一个阶段。
    //
    //片段着色器是在每个像素上运行的程序，其主要功能是计算像素的颜色值。它可以对像素进行光照、纹理采样、透明度计算等操作，并将最终的颜色值输出。
    //
    //简单来说，顶点着色器负责处理顶点的位置、颜色等属性，片段着色器负责处理像素的颜色、纹理等属性。两者协同工作，最终将3D场景渲染成2D图像。

    /**
     * 顶点着色器代码;
     */
    private val vertexShaderCode =
    // uMVPMatrix变量是需要用于顶点坐标变换的矩阵
        // 作为hook入口，用于绘制时传入模型矩阵、观察矩阵、投影矩阵
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                // 把vPosition顶点经过矩阵变换后传给gl_Position
                "  gl_Position = uMVPMatrix * vPosition;" +
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


    /**
     * 着色器程序ID引用
     */
    private var mProgram: Int

    private var vPMatrixHandle: Int = 0
    init {
        // 编译顶点着色器和片段着色器
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode) // 顶点着色器
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode) // 片段着色器
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

    // 加载和编译着色器代码
    private fun loadShader(type: Int, shaderCode: String): Int {
        // glCreateShader函数创建一个顶点着色器或者片段着色器,并返回新创建着色器的ID引用
        val shader = GLES20.glCreateShader(type)
        // 把着色器和代码关联,然后编译着色器
        GLES20.glShaderSource(shader, shaderCode)
        // 编译着色器
        GLES20.glCompileShader(shader)
        // 返回着色器的引用
        return shader
    }

    // 顶点个数
    private val vertexCount: Int = triangleCoords.size / COORDS_PER_VERTEX
    // 看着像是 内存数，每个顶点占据4字节，Byte, 32位 bit
    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    /**
     * 实际绘制时执行的方法
     **/
    fun draw(mvpMatrix: FloatArray) {
        // 激活着色器程序
        GLES20.glUseProgram(mProgram)
        // 获取顶点着色器中的vPosition变量(因为之前已经编译过着色器代码,所以可以从着色器程序中获取);用唯一ID表示
        val positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        // 获取顶点着色器代码中的uMVPMatrix变量;用唯一ID表示
        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        // 把模型矩阵、观察矩阵、投影矩阵的计算结果传递给顶点着色器代码中的vPMatrixHandle
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)
        // 绘制三角形
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        // 允许操作顶点对象position
        GLES20.glEnableVertexAttribArray(positionHandle)
        // 将顶点数据传递给position指向的vPosition变量
        GLES20.glVertexAttribPointer(
            positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
            false, vertexStride, vertexBuffer)
        // 获取片段着色器中的vColor变量
        val colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        // 通过colorHandle设置绘制的颜色值
        GLES20.glUniform4fv(colorHandle, 1, color, 0)
        // 绘制顶点数组;
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        // 操作完后,取消允许操作顶点对象position
        GLES20.glDisableVertexAttribArray(positionHandle)
    }
}
