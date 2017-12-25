package com.screenmaker.screenmaker;


import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.Toast;

import com.screenmaker.screenmaker.camera.CameraHelper;

public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener  {

    private static final int PHOTO_QUANTITY = 10;

    private CameraHelper cameraHelper;
    private TextureView mTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mTextureView = findViewById(R.id.textureView_camera);
        mTextureView.setSurfaceTextureListener(this);

        cameraHelper = new CameraHelper(this);
        try {
            cameraHelper.init(CameraCharacteristics.LENS_FACING_FRONT, PHOTO_QUANTITY);
            cameraHelper.setTextureView(mTextureView);
        } catch (CameraAccessException e) {
            Toast.makeText(this, "CameraError", Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
        }
    }

    @Override
    protected void onDestroy() {
        cameraHelper.closeCamera();
        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        try {
            cameraHelper.openCamera();
        } catch (CameraAccessException e) {
            Toast.makeText(this, "CameraError", Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }
}
