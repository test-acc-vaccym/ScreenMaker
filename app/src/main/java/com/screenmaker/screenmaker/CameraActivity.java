package com.screenmaker.screenmaker;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.Toast;

import com.screenmaker.screenmaker.camera.CameraHelper;
import com.screenmaker.screenmaker.storage.cryptoinfo.ServiceCryptoInfo;
import com.screenmaker.screenmaker.storage.images.ImageEntry;
import com.screenmaker.screenmaker.storage.images.ServiceImageEntry;
import com.screenmaker.screenmaker.utils.ImageCryptoUtils;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;

public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private static final int PHOTO_QUANTITY = 10;

    private CameraHelper cameraHelper;

    private String mImageKey;
    private String mImageAlias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        prepareCapture()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        TextureView mTextureView = findViewById(R.id.textureView_camera);
                        mTextureView.setSurfaceTextureListener(CameraActivity.this);
                        cameraHelper = new CameraHelper(CameraActivity.this);
                        try {
                            cameraHelper.init(CameraCharacteristics.LENS_FACING_FRONT);
                            cameraHelper.setTextureView(mTextureView);
                            onSurfaceTextureAvailable(mTextureView.getSurfaceTexture(), mTextureView.getWidth(), mTextureView.getHeight());
                        } catch (CameraAccessException e) {
                            Toast.makeText(CameraActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            finishActivityWithRes(RESULT_CANCELED);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(CameraActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        finishActivityWithRes(RESULT_CANCELED);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        cameraHelper.closeCamera();
        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        ServiceImageEntry serviceImageEntry = new ServiceImageEntry();
        ImageCryptoUtils cryptoUtils = new ImageCryptoUtils(mImageKey, mImageAlias);
        cameraHelper.openCamera(PHOTO_QUANTITY)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .map(bytes -> {
                    byte[] encr = cryptoUtils.encryptImage(bytes);
                    serviceImageEntry.insertImage(new ImageEntry(encr));
                    return bytes;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSubscriber<byte[]>() {
                    @Override
                    public void onNext(byte[] bytes) {
                        Toast.makeText(CameraActivity.this, "Image saving", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable t) {
                        Toast.makeText(CameraActivity.this, t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        finishActivityWithRes(RESULT_CANCELED);
                    }

                    @Override
                    public void onComplete() {
                        Toast.makeText(CameraActivity.this, "Image capturing complete", Toast.LENGTH_LONG).show();
                        finishActivityWithRes(RESULT_OK);
                    }
                });
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

    @Override
    public void onBackPressed() {

    }

    private Completable prepareCapture() {
        return Completable.create(e -> {
            // clear old images
            ServiceImageEntry serviceImageEntry = new ServiceImageEntry();
            serviceImageEntry.clearAll();

            //Initializing keys
            ServiceCryptoInfo serviceCryptoInfo = new ServiceCryptoInfo();
            mImageKey = serviceCryptoInfo.getAndDecrypt(App.IMAGE_ENCRYPTION_KEY_TITLE);
            mImageAlias = serviceCryptoInfo.getAndDecrypt(App.IMAGE_ENCRYPTION_ALIAS_TITLE);
            if (mImageKey == null || mImageKey.equals("") ||
                    mImageAlias == null || mImageKey.equals("")) {
                e.onError(new InstantiationException("Failed to instantiate the photo camera"));
            } else {
                e.onComplete();
            }
        });
    }

    private void finishActivityWithRes(int result) {
        setResult(result);
        finish();
    }

}
