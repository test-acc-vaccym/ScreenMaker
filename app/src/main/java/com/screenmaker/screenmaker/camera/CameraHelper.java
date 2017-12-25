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
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private int photoQuantity;

    public CameraHelper(Context context) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void init(int cameraId, int photoQuantity) throws CameraAccessException {
        this.photoQuantity = photoQuantity;
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

        if(this.cameraID == null){
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

    private void createCameraPreviewSession(int photoQuantity) {
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        Surface surface = new Surface(texture);

        mImageReader = createImageReader(photoQuantity);

        try {
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        mCameraCaptureSession = session;
                        mCameraCaptureSession.setRepeatingRequest(createCaptureRequest(surface, CameraDevice.TEMPLATE_PREVIEW), null, null);

                        CaptureRequest photoCaptureRequest = createCaptureRequest(mImageReader.getSurface(), CameraDevice.TEMPLATE_STILL_CAPTURE);
                        setCaptureBurst(mCameraCaptureSession, photoCaptureRequest);
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

    private ImageReader createImageReader(int photoQuantity){
        ImageReader imageReaderYUV = ImageReader.newInstance(DEFAULT_WIDTH, DEFAULT_HEIGHT, ImageFormat.YUV_420_888, photoQuantity);
        imageReaderYUV.setOnImageAvailableListener(
                mOnImageAvailableListener, null);
        return imageReaderYUV;
    }

    private CaptureRequest createCaptureRequest(Surface surface, int template) throws CameraAccessException {
        CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(template);
        captureRequestBuilder.addTarget(surface);
        return captureRequestBuilder.build();
    }

    private List<CaptureRequest> createCaptureList(CaptureRequest photoCaptureRequest, int photoQuantity){
        List<CaptureRequest> captureList = new ArrayList<>();
        for (int i = 0; i < photoQuantity; i++) {
            captureList.add(photoCaptureRequest);
        }
        return captureList;
    }

    private void setCaptureBurst(CameraCaptureSession session, CaptureRequest photoCaptureRequest) throws CameraAccessException {
        List<CaptureRequest> captureList = createCaptureList(photoCaptureRequest, photoQuantity);
        session.captureBurst(captureList, new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                Log.e(LOG_TAG, "onCaptureCompleted test");
            }
        }, null);
    }


    private final CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            Log.i(LOG_TAG, "Open camera  with id:" + mCameraDevice.getId());
            createCameraPreviewSession(photoQuantity);
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
    };

}
