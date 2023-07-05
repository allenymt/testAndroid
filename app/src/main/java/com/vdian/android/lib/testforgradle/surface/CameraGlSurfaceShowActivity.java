package com.vdian.android.lib.testforgradle.surface;

import static android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.vdian.android.lib.testforgradle.R;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author yulun
 * @since 2023-07-04 17:22
 *
 * 基本步骤
 * 在xml中添加GlSurfaceView
 * 创建渲染器类实现GlSurfaceView.Renderer
 * 清除画布，并创建一个纹理并绑定到。
 * 创建一个用来最后显示的SurfaceTexture来显示处理后的数据。
 * 创建Opengl ES程序并添加着色器到该程序中，创建openGl程序的可执行文件，并释放shader资源。
 * 打开摄像头，并配置相关属性。设置预览视图，并开启预览。
 * 添加程序到ES环境中，并设置及启用各类句柄。
 * 在onDrawFrame中进行画布的清理及绘制最新的数据到纹理图形中。
 * 设置一个SurfaceTexture.OnFrameAvailableListener的回调来通知GlSurfaceview渲染新的帧数据。
 *
 * 建议： GlSurfaceView作用简单的理解OpenGl对相机数据进行处理完之后的显示。我们需要明白的是渲染器的渲染周期及渲染方法的调用时机。
 *
 * onSurfaceCreated()当surface创建(第一次进入当前页面)或者重新创建(切换后台再进入)的时候调用。
 * onSurfaceChanged()当surface大小发生改变的时候会被调用。
 * onDrawFrame()绘制当前帧数据的时候被调用。
 */
public class CameraGlSurfaceShowActivity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener {

    // 纹理处理类
    public SurfaceTexture mSurfaceTexture;

    // 相机类
    public static Camera camera;

    // 相机种类，前置1 or 后置0
    private int camera_status = 1;

    // 相机预览的GLSurfaceView
    GLSurfaceView mCameraGlSurfaceView;

    // GLSurfaceView对应的render
    public MyRender mRenderer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_gl_surface_show);
        mCameraGlSurfaceView = findViewById(R.id.camera_gl_surface_view);

        //在setRenderer()方法前调用此方法 , 设定gl版本
        mCameraGlSurfaceView.setEGLContextClientVersion(2);

        // 设置渲染器
        mRenderer = new MyRender();
        mCameraGlSurfaceView.setRenderer(mRenderer);
        mCameraGlSurfaceView.setRenderMode(RENDERMODE_WHEN_DIRTY);
    }


    // SurfaceTexture.OnFrameAvailableListener 是一个监听器接口，用于监听 SurfaceTexture 中的新帧数据是否可用。当新的帧数据到达时，
    // SurfaceTexture 会调用 OnFrameAvailableListener 的 onFrameAvailable 方法，通知应用程序可以获取新的帧数据了。
    //
    //在 Android 中，SurfaceTexture 是一个用于将外部纹理数据（例如相机预览数据）渲染到 TextureView 或 GLSurfaceView 上的类。
    // 应用程序可以通过注册 SurfaceTexture.OnFrameAvailableListener 监听器接口来获取新的纹理帧数据，然后将其用于自定义的图像处理或显示操作。
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mCameraGlSurfaceView.requestRender();
    }


    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_gl_surface_view_animator:
                PropertyValuesHolder valuesHolder1 = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.5f,1.0f);
                PropertyValuesHolder valuesHolder4 = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.5f,1.0f);
                PropertyValuesHolder valuesHolder5 = PropertyValuesHolder.ofFloat("rotationY", 0.0f, 360.0f, 0.0F);
                ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(mCameraGlSurfaceView, valuesHolder1, valuesHolder4,valuesHolder5);
                objectAnimator.setDuration(3000).start();
                break;
            case R.id.btn_gl_surface_view_switch:
                //  切换摄像头
                camera_status ^= 1;
                if (camera != null) {
                    // 切换摄像头前先停止预览
                    camera.stopPreview();
                    camera.release();
                }
                // 切换摄像头后，需要重新激活一次
                mRenderer.mActivityProgram = true;
                // 开启相机
                camera = Camera.open(camera_status);
                try {
                    // 设置预览的texture
                    camera.setPreviewTexture(mSurfaceTexture);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // 开始预览
                camera.startPreview();
                break;
        }
    }

    public class MyRender implements GLSurfaceView.Renderer {
        private final String vertexShaderCode = "uniform mat4 textureTransform;\n" +
                "attribute vec2 inputTextureCoordinate;\n" +
                "attribute vec4 position;            \n" +//NDK坐标点
                "varying   vec2 textureCoordinate; \n" +//纹理坐标点变换后输出
                "\n" +
                " void main() {\n" +
                "     gl_Position = position;\n" +
                "     textureCoordinate = inputTextureCoordinate;\n" +
                " }";

        private final String fragmentShaderCode = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES videoTex;\n" +
                "varying vec2 textureCoordinate;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 tc = texture2D(videoTex, textureCoordinate);\n" +
                "    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +  //所有视图修改成黑白
                "    gl_FragColor = vec4(color,color,color,1.0);\n" +
//                "    gl_FragColor = vec4(tc.r,tc.g,tc.b,1.0);\n" +
                "}\n";
        private FloatBuffer mPosBuffer;
        private FloatBuffer mTexBuffer;
        private float[] mPosCoordinate = {-1, -1, -1, 1, 1, -1, 1, 1};
        private float[] mTexCoordinateBackRight = {1, 1, 0, 1, 1, 0, 0, 0};//顺时针转90并沿Y轴翻转  后摄像头正确，前摄像头上下颠倒
        private float[] mTexCoordinateForntRight = {0, 1, 1, 1, 0, 0, 1, 0};//顺时针旋转90  后摄像头上下颠倒了，前摄像头正确

        public int mProgram;
        public boolean mActivityProgram = false;

        public MyRender() {
            Matrix.setIdentityM(mProjectMatrix, 0);
            Matrix.setIdentityM(mCameraMatrix, 0);
            Matrix.setIdentityM(mMVPMatrix, 0);
            Matrix.setIdentityM(mTempMatrix, 0);
        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            // 添加上面编写的着色器代码并编译它
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        private void createProgram() {
            //通常做法
//            String vertexSource = AssetsUtils.read(CameraGlSurfaceShowActivity.this, "vertex_texture.glsl");
//            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
//            String fragmentSource = AssetsUtils.read(CameraGlSurfaceShowActivity.this, "fragment_texture.glsl");
//            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
            // 创建空的OpenGL ES程序
            mProgram = GLES20.glCreateProgram();

            // 添加顶点着色器到程序中
            GLES20.glAttachShader(mProgram, vertexShader);

            // 添加片段着色器到程序中
            GLES20.glAttachShader(mProgram, fragmentShader);

            // 创建OpenGL ES程序可执行文件
            GLES20.glLinkProgram(mProgram);

            // 释放shader资源
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
        }

        private FloatBuffer convertToFloatBuffer(float[] buffer) {
            FloatBuffer fb = ByteBuffer.allocateDirect(buffer.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            fb.put(buffer);
            fb.position(0);
            return fb;
        }

        private int uPosHandle;
        private int aTexHandle;
        private int mMVPMatrixHandle;
        private float[] mProjectMatrix = new float[16];
        private float[] mCameraMatrix = new float[16];
        private float[] mMVPMatrix = new float[16];
        private float[] mTempMatrix = new float[16];



        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            mSurfaceTexture = new SurfaceTexture(createOESTextureObject());
            createProgram();
//            mProgram = ShaderUtils.createProgram(CameraGlSurfaceShowActivity.this, "vertex_texture.glsl", "fragment_texture.glsl");
            camera = Camera.open(camera_status);
            try {
                camera.setPreviewTexture(mSurfaceTexture);
                camera.startPreview();
            } catch (IOException e) { 
                e.printStackTrace();
            }
            activeProgram();

        }

        //添加程序到ES环境中
        private void activeProgram() {
            // 将程序添加到OpenGL ES环境
            GLES20.glUseProgram(mProgram);

            mSurfaceTexture.setOnFrameAvailableListener(CameraGlSurfaceShowActivity.this);
            // 获取顶点着色器的位置的句柄
            uPosHandle = GLES20.glGetAttribLocation(mProgram, "position");
            aTexHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "textureTransform");

            mPosBuffer = convertToFloatBuffer(mPosCoordinate);
            if(camera_status == 0){
                mTexBuffer = convertToFloatBuffer(mTexCoordinateBackRight);
            }else{
                mTexBuffer = convertToFloatBuffer(mTexCoordinateForntRight);
            }

            GLES20.glVertexAttribPointer(uPosHandle, 2, GLES20.GL_FLOAT, false, 0, mPosBuffer);
            GLES20.glVertexAttribPointer(aTexHandle, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);

            // 启用顶点位置的句柄
            GLES20.glEnableVertexAttribArray(uPosHandle);
            GLES20.glEnableVertexAttribArray(aTexHandle);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            Matrix.scaleM(mMVPMatrix,0,1,-1,1);
            float ratio = (float) width / height;
            Matrix.orthoM(mProjectMatrix, 0, -1, 1, -ratio, ratio, 1, 7);// 3和7代表远近视点与眼睛的距离，非坐标点
            Matrix.setLookAtM(mCameraMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);// 3代表眼睛的坐标点
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mCameraMatrix, 0);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if(mActivityProgram){
                activeProgram();
                mActivityProgram = false;
            }
            if (mSurfaceTexture != null) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                mSurfaceTexture.updateTexImage();
                GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mPosCoordinate.length / 2);
            }
        }
    }

    public static int createOESTextureObject() {
        int[] tex = new int[1];
        //生成一个纹理
        GLES20.glGenTextures(1, tex, 0);
        //将此纹理绑定到外部纹理上
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
        //设置纹理过滤参数
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return tex[0];
    }
}
