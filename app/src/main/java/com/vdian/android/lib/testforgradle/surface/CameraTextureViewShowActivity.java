package com.vdian.android.lib.testforgradle.surface;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.vdian.android.lib.testforgradle.R;

import java.io.IOException;

/**
 * @author yulun
 * @since 2023-07-04 16:29
 */
public class CameraTextureViewShowActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    TextureView mCameraTextureView;
    public Camera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_surface_texture);
        mCameraTextureView = findViewById(R.id.camera_texture_view);
        mCameraTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            mCamera = Camera.open(0);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewTexture(mCameraTextureView.getSurfaceTexture());
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void onViewClicked(View view) {
        PropertyValuesHolder valuesHolder = PropertyValuesHolder.ofFloat("translationX", 0.0f, 0.0f);
        PropertyValuesHolder valuesHolder1 = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.3f,1.0f);
        PropertyValuesHolder valuesHolder4 = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.3f,1.0f);
        PropertyValuesHolder valuesHolder2 = PropertyValuesHolder.ofFloat("rotationX", 0.0f, 2 * 360.0f, 0.0F);
        PropertyValuesHolder valuesHolder5 = PropertyValuesHolder.ofFloat("rotationY", 0.0f, 2 * 360.0f, 0.0F);
        PropertyValuesHolder valuesHolder3 = PropertyValuesHolder.ofFloat("alpha", 1.0f, 0.7f, 1.0F);
        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(mCameraTextureView, valuesHolder, valuesHolder1, valuesHolder2, valuesHolder3,valuesHolder4,valuesHolder5);
        objectAnimator.setDuration(5000).start();
    }
}

