package com.screenmaker.screenmaker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;
import com.screenmaker.screenmaker.storage.cryptoinfo.ServiceCryptoInfo;
import com.screenmaker.screenmaker.storage.images.ImageEntry;
import com.screenmaker.screenmaker.storage.images.ServiceImageEntry;
import com.screenmaker.screenmaker.utils.ImageCryptoUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.NoSuchPaddingException;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;

public class CameraActivity extends AppCompatActivity {

    private static final String EXTRA_IMAGE_QUANTITY = "ExtraImageQuantity";

    private static final int DEFAULT_IMAGE_QUANTITY = 10;

    private static final int DEFAULT_WIDTH = 1920;
    private static final int DEFAULT_HEIGHT = 1080;

    private static final int STATE_PREVIEW = 101;
    private static final int STATE_LOCKED = 102;
    private int mCaptureState = STATE_PREVIEW;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private TextureView mTextureView;
    private int mCameraScreenWidth;
    private int mCameraScreenHeight;

    private CameraDevice mCameraDevice;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private String mCameraId;

    private ImageReader mImageReader;
    private int mTotalRotation;

    private CameraManager mCameraManager;

    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;

    private String mImageKey;
    private String mImageAlias;

    private ServiceImageEntry mServiceImageEntry;
    private ImageCryptoUtils mCryptoUtils;

    private int mImageQuantity;
    private int mImagesMade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        launchBackgroundThread();
        prepareCapture()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        mImageQuantity = getIntent().getIntExtra(EXTRA_IMAGE_QUANTITY, DEFAULT_IMAGE_QUANTITY);
                        initKeyService();
                        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        mTextureView = findViewById(R.id.textureView_camera);
                        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
                        mSurfaceTextureListener.onSurfaceTextureAvailable(mTextureView.getSurfaceTexture(), mTextureView.getWidth(), mTextureView.getHeight());
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(CameraActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        finishActivityWithRes(RESULT_CANCELED);
                    }
                });
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocas) {
        super.onWindowFocusChanged(hasFocas);
        View decorView = getWindow().getDecorView();
        if(hasFocas) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private void initKeyService(){
        mServiceImageEntry = new ServiceImageEntry();
        mCryptoUtils = new ImageCryptoUtils(mImageKey, mImageAlias);
    }

    private void setupCamera(int width, int height) throws CameraAccessException {
        if(width == 0) width = DEFAULT_WIDTH;
        if(height == 0) height = DEFAULT_HEIGHT;
        String[] cameraList = mCameraManager.getCameraIdList();
        CameraCharacteristics cameraCharacteristics;
        for (String cameraID : cameraList) {
            cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraID);
            Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && CameraCharacteristics.LENS_FACING_FRONT == facing) {
                this.mCameraId = cameraID;
                mTotalRotation = calculateTotalRotation(cameraCharacteristics);
                int[] res = setCameraDimentions(mTotalRotation, width, height);
                mCameraScreenWidth = res[0];
                mCameraScreenHeight = res[1];

                mImageReader = ImageReader.newInstance(mCameraScreenWidth, mCameraScreenHeight, ImageFormat.JPEG, 1);
                mImageReader.setOnImageAvailableListener(mImageListener, mHandler);
                break;
            }
        }

        if (this.mCameraId == null) {
            Toast.makeText(CameraActivity.this, "Front camera was not found in this device", Toast.LENGTH_LONG).show();
            finishActivityWithRes(RESULT_CANCELED);
        } else {
            connectCamera();
        }
    }

    @Override
    public void onBackPressed() {
        // Not allowing to close activity until photo capturing finishes
    }

    /**
     * Camera permission was successfully asked in the previous @link(MainActivity), so
     * there is no sence in checking it again
     */
    @SuppressLint("MissingPermission")
    private void connectCamera() throws CameraAccessException {
        mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mHandler);
    }

    private int calculateTotalRotation(@Nullable CameraCharacteristics cameraCharacteristics){
        int deviceRotation = getWindowManager().getDefaultDisplay().getRotation();
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        deviceRotation = ORIENTATIONS.get(deviceRotation);

        return (sensorOrientation + deviceRotation + 360) % 360;
    }

    private int[] setCameraDimentions(int totalRotation, int width, int height){
        boolean swap = totalRotation == 90 || totalRotation == 270;
        if(swap){
            return new int[]{height, width};
        } else {
            return new int[]{width, height};
        }
    }

    private void launchCameraPreview() {
        Toast.makeText(CameraActivity.this, "Image capturing", Toast.LENGTH_LONG).show();
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mCameraScreenWidth, mCameraScreenHeight);
        Surface previewSurface = new Surface(surfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mCameraCaptureSession = session;
                            try {
                                mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        new CameraCaptureSession.CaptureCallback() {
                                            @Override
                                            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                                super.onCaptureStarted(session, request, timestamp, frameNumber);
                                                if(mImagesMade < mImageQuantity){
                                                    lockFocus();
                                                } else {
                                                    finishActivityWithRes(RESULT_OK);
                                                }
                                            }
                                        }, mHandler);

                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(CameraActivity.this, "Camera opening error", Toast.LENGTH_LONG).show();
                            finishActivityWithRes(RESULT_CANCELED);
                        }
                    }, null);
        } catch (CameraAccessException e) {
            Toast.makeText(CameraActivity.this, "Camera opening error", Toast.LENGTH_LONG).show();
            finishActivityWithRes(RESULT_CANCELED);
        }
    }

    private void startStillCaptureRequest() {
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mTotalRotation);
            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            Toast.makeText(CameraActivity.this, "Camera photo error", Toast.LENGTH_LONG).show();
            finishActivityWithRes(RESULT_CANCELED);
        }
    }

    private void closeCamera() {
        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void launchBackgroundThread() {
        mHandlerThread = new HandlerThread("ScreenMaker");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void lockFocus() {
        mCaptureState = STATE_LOCKED;
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mHandler);
        } catch (CameraAccessException e) {
            Toast.makeText(CameraActivity.this, "Camera focus error", Toast.LENGTH_LONG).show();
            finishActivityWithRes(RESULT_CANCELED);
        }
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            try {
                setupCamera(width, height);
            } catch (CameraAccessException e) {
                Toast.makeText(CameraActivity.this, "Camera initialization error", Toast.LENGTH_LONG).show();
                finishActivityWithRes(RESULT_CANCELED);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            launchCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
            Toast.makeText(CameraActivity.this, "Camera opening error", Toast.LENGTH_LONG).show();
            finishActivityWithRes(RESULT_CANCELED);
        }
    };

    private final ImageReader.OnImageAvailableListener mImageListener = reader ->
            mHandler.post(new ImageSaver(reader.acquireLatestImage()));

    private class ImageSaver implements Runnable {

        private final Image mImage;

        ImageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            try {
                ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                mImage.close();
                mImagesMade = mServiceImageEntry.getAllImages().size();
                if(mImagesMade < mImageQuantity){
                    byte[] encr = mCryptoUtils.encryptImage(bytes);
                    mServiceImageEntry.insertImage(new ImageEntry(encr));
                }
            } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException |
                    InvalidAlgorithmParameterException | IOException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
        }
    }

    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new
            CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    switch (mCaptureState) {
                        case STATE_LOCKED:
                            mCaptureState = STATE_PREVIEW;
                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                            if(afState != null && (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                                    afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED)) {
                                startStillCaptureRequest();
                            }
                            break;
                    }
                }
            };

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