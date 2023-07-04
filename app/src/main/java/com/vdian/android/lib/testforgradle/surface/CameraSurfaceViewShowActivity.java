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
 */
public class CameraSurfaceViewShowActivity extends  AppCompatActivity implements SurfaceHolder.Callback {
    SurfaceView mSurfaceView;

    public SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.Parameters mParameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_camera);
        mSurfaceView = findViewById(R.id.mSurface);
        mHolder = mSurfaceView.getHolder();
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
//        PropertyValuesHolder valuesHolder2 = PropertyValuesHolder.ofFloat("rotationX", 0.0f, 360.0f, 0.0F);
//        PropertyValuesHolder valuesHolder = PropertyValuesHolder.ofFloat("rotationY", 0.0f, 360.0f, 0.0F);
        PropertyValuesHolder valuesHolder1 = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.5f,1.0f);
        PropertyValuesHolder valuesHolder3 = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.5f,1.0f);
        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(mSurfaceView ,valuesHolder1,valuesHolder3);
        objectAnimator.setDuration(5000).start();
    }

}