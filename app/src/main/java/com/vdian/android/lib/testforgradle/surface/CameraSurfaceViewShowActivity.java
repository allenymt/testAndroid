package com.vdian.android.lib.testforgradle.surface;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.vdian.android.lib.testforgradle.R;

import java.io.IOException;

/**
 * @author yulun
 * @since 2023-07-04 15:19
 * SurfaceView预览相机视图不支持透明度，可以设置缩放旋转属性。
 * 如果需要做动画特效的话不推荐使用SurfaceView显示视图。可以使用TextureView或者GlSurfaceView来显示。
 */
public class CameraSurfaceViewShowActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    SurfaceView mSurfaceView;
    // 辅助控制SurfaceView的大小、格式等，以及监听SurfaceView的状态
    public SurfaceHolder mHolder;
    // 相机对象
    private Camera mCamera;
    private Camera.Parameters mParameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_camera);
        mSurfaceView = findViewById(R.id.mSurface);
        mHolder = mSurfaceView.getHolder();
        // 注册SurfaceHolder的回调方法
        mHolder.addCallback(this);
//        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // Open the Camera in preview mode
            mCamera = Camera.open(0);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    mParameters = mCamera.getParameters();
                    mParameters.setPictureFormat(PixelFormat.JPEG); //图片输出格式
//                    mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);//预览持续发光
                    mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//持续对焦模式
                    mCamera.setParameters(mParameters);
                    mCamera.startPreview();
                    mCamera.cancelAutoFocus();
                }
            }
        });
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void onViewClicked(View view) {
        // 旋转、缩放动画
        PropertyValuesHolder valuesHolderRx = PropertyValuesHolder.ofFloat("rotationX", 0.0f, 360.0f, 0.0F);
        PropertyValuesHolder valuesHolderRy = PropertyValuesHolder.ofFloat("rotationY", 0.0f, 360.0f, 0.0F);
        PropertyValuesHolder valuesHolderSx = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.5f, 1.0f);
        PropertyValuesHolder valuesHolderSy = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.5f, 1.0f);
        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(mSurfaceView, valuesHolderRx, valuesHolderRy, valuesHolderSx, valuesHolderSy);
        objectAnimator.setDuration(5000).start();
    }

}