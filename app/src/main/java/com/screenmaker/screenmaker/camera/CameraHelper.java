package com.screenmaker.screenmaker.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

public class CameraHelper {

    private static final String LOG_TAG = "myLogs";

    private static final int DEFAULT_WIDTH = 1920;
    private static final int DEFAULT_HEIGHT = 1080;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice = null;
    private CameraCaptureSession mCameraCaptureSession;

    private ImageReader mImageReader;

    private String cameraID;
    private TextureView mTextureView;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    public CameraHelper(Context context) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void init(int cameraId) throws CameraAccessException {
        String[] cameraList = mCameraManager.getCameraIdList();
        CameraCharacteristics cc = null;
        for (String cameraID : cameraList) {
            cc = mCameraManager.getCameraCharacteristics(cameraID);
            Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
            if (cameraId == facing) {
                this.cameraID = cameraID;
                break;
            }
        }

        if (this.cameraID == null) {
            throw new CameraAccessException(CameraAccessException.CAMERA_DISABLED);
        }
    }

    public void setTextureView(TextureView textureView) {
        mTextureView = textureView;
    }

    @SuppressLint("MissingPermission")
    public void openCamera() throws CameraAccessException {
        mCameraManager.openCamera(cameraID, mCameraCallback, null);
    }

    public void closeCamera() {
        try {
            if (null != mCameraCaptureSession) {
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }
    }

    private void createCameraPreviewSession() {

        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        Surface surface = new Surface(texture);

        mImageReader = ImageReader.newInstance(DEFAULT_WIDTH, DEFAULT_HEIGHT,
                ImageFormat.YUV_420_888, 1);

        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);

        try {
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mPreviewRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (null == mCameraDevice) {
                        return;
                    }
                    mCameraCaptureSession = session;

                    try {
                        mPreviewRequest = mPreviewRequestBuilder.build();
                        mCameraCaptureSession.setRepeatingRequest(mPreviewRequest,
                                null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }

            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void takePicture(int photoQuantity) throws CameraAccessException {
        final CaptureRequest.Builder captureBuilder =
                mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(mImageReader.getSurface());
        takePicture(captureBuilder, photoQuantity);
    }

    private void takePicture(CaptureRequest.Builder captureBuilder, int photoQuantity) throws CameraAccessException {

        CameraCaptureSession.CaptureCallback CaptureCallback
                = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
                Log.e("myLogs", "onCaptureCompleted");
            }

            @Override
            public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                Log.e("myLogs", "onCaptureSequenceCompleted " + photoQuantity);
                if (photoQuantity > 0) {
                    try {
                        takePicture(captureBuilder, photoQuantity - 1);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

        };

        mCameraCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
    }

    private final CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            Log.i(LOG_TAG, "Open camera  with id:" + mCameraDevice.getId());
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraDevice.close();
            mCameraDevice = null;
            Log.i(LOG_TAG, "disconnect camera with id:" + mCameraDevice.getId());
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.i(LOG_TAG, "error! camera id: " + camera.getId() + " error:" + error);
        }

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = reader -> {
        Log.i(LOG_TAG, "creating photo reader " + reader);
        Image image = null;
        while ((image = reader.acquireLatestImage()) != null){
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            image.close();
            Log.e(LOG_TAG, "bytes " + bytes);
            Log.e(LOG_TAG, "bytes " + bytes.length);
        }
    };





}
