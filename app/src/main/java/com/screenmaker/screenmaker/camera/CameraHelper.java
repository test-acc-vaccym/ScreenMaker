package com.screenmaker.screenmaker.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraHelper {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private static final int DEFAULT_WIDTH = 1920;
    private static final int DEFAULT_HEIGHT = 1080;

    private static final int STATE_PREVIEW = 201;
    private static final int STATE_LOCK = 202;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Activity mActivity;

    private CameraHelperBackListener mCameraHelperBackListener;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice = null;
    private CameraCaptureSession mCameraCaptureSession;

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private ImageReader mImageReader;
    private int mTotalRotation;

    private String mCameraID;

    private TextureView mTextureView;
    private int mCameraScreenWidth;
    private int mCameraScreenHeight;

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private int mCaptureState = STATE_PREVIEW;
    private Integer mSensorOrientation;

    public CameraHelper(Context context, TextureView textureView, CameraHelperBackListener cameraHelperBackListener) {
        this.mActivity = (Activity) context;
        this.mCameraHelperBackListener = cameraHelperBackListener;
        mTextureView = textureView;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        launchBackgroundThread();
    }

    public void init(int cameraId, int width, int height) throws CameraAccessException {
        if(width == 0) width = DEFAULT_WIDTH;
        if(height == 0) height = DEFAULT_HEIGHT;
        String[] cameraList = mCameraManager.getCameraIdList();
        CameraCharacteristics cameraCharacteristics = null;
        for (String cameraID : cameraList) {
            cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraID);
            Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraId == facing) {
                this.mCameraID = cameraID;
                mSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

                mTotalRotation = calculateTotalRotation(cameraCharacteristics);
                int[] res = setCameraDimentions(mTotalRotation, width, height);
                mCameraScreenWidth = res[0];
                mCameraScreenHeight = res[1];

                mImageReader = ImageReader.newInstance(mCameraScreenWidth, mCameraScreenHeight, ImageFormat.JPEG, 1);
                mImageReader.setOnImageAvailableListener(mImageListener, mHandler);
                break;
            }
        }

        if (this.mCameraID == null) {
            throw new CameraAccessException(CameraAccessException.CAMERA_DISABLED);
        } else {
            setCamera();
        }
    }

    public void closeCamera() {
        try {
            if (null != mCameraCaptureSession) {
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession.abortCaptures();
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
        } finally {
            mCameraHelperBackListener = null;
            stopBackgroundThread();
        }
    }

    public void takePictures(int picturesQuantity) throws CameraAccessException {
        mCaptureState = STATE_LOCK;
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        mCameraCaptureSession.capture(mPreviewRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                switch (mCaptureState){
                    case STATE_LOCK:
                        Log.e("myLogs", "STATE_LOCK");
                        mCaptureState = STATE_PREVIEW;
                        Integer focusState = result.get(CaptureResult.CONTROL_AF_STATE);
                        if(focusState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                                focusState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                            try {
                                capturePictures(picturesQuantity);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    break;
                }
            }
        }, mHandler);
//        mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//        mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
//        mPreviewRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mTotalRotation);
//
//        List<CaptureRequest> captureRequestList = new ArrayList<>();
//        for (int i = 0; i < picturesQuantity; i++) {
//            captureRequestList.add(mPreviewRequestBuilder.build());
//        }
//        Log.e("myLogs", "takePictures");
//        mCameraCaptureSession.captureBurst(captureRequestList, null, null);
    }

    private void capturePictures(int picturesQuantity) throws CameraAccessException {
        mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
        mPreviewRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mTotalRotation);

        List<CaptureRequest> captureRequestList = new ArrayList<>();
        for (int i = 0; i < picturesQuantity; i++) {
            captureRequestList.add(mPreviewRequestBuilder.build());
        }
        Log.e("myLogs", "capturePictures");
        mCameraCaptureSession.captureBurst(captureRequestList, null, null);
    }

    private int calculateTotalRotation(CameraCharacteristics cameraCharacteristics){
        int deviceRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
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

    private void launchBackgroundThread(){
        mHandlerThread = new HandlerThread("ScreenMaker");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    private void stopBackgroundThread(){
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void launchCameraPreview() {
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(mCameraScreenWidth, mCameraScreenHeight);
        Surface surface = new Surface(texture);

        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;

                    try {
                        mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mHandler);
                        mCameraHelperBackListener.onCameraReady();
                    } catch (CameraAccessException e) {
                        // TODO do smth
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // TODO do smth
                }
            }, null);

        } catch (CameraAccessException e){
                // TODO do smth
        }

    }

    @SuppressLint("MissingPermission")
    private void setCamera() throws CameraAccessException {
        mCameraManager.openCamera(mCameraID, mCameraCallback , mHandler);
    }

    private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {
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
        }
    };

    private ImageReader.OnImageAvailableListener mImageListener = reader -> {
        Log.e("myLogs", "mImageListener " + reader);
        Image image = reader.acquireLatestImage();
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        image.close();
    };

//
//    @SuppressLint("MissingPermission")
//    public Flowable<byte[]> openCamera(int photoQuantity) {
//        final int[] savedPhoto = {0};
//        return Flowable.create(emitter -> mCameraManager.openCamera(mCameraID, new CameraDevice.StateCallback() {
//            @Override
//            public void onOpened(@NonNull CameraDevice camera) {
//                mCameraDevice = camera;
//                SurfaceTexture texture = mTextureView.getSurfaceTexture();
//                texture.setDefaultBufferSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
//                Surface surface = new Surface(texture);
//
//                mImageReader = ImageReader.newInstance(DEFAULT_WIDTH, DEFAULT_HEIGHT,
//                        ImageFormat.JPEG, 1);
//                mImageReader.setOnImageAvailableListener(reader -> {
//                    Image image = null;
//                    while ((image = reader.acquireNextImage()) != null){
//                        Image.Plane[] planes = image.getPlanes();
//                        ByteBuffer buffer = planes[0].getBuffer();
//                        buffer.rewind();
//                        byte[] bytes = new byte[buffer.capacity()];
//                        buffer.get(bytes);
//                        image.close();
//                        emitter.onNext(bytes);
//                        if(savedPhoto[0] == photoQuantity){
//                            emitter.onComplete();
//                        } else {
//                            savedPhoto[0]++;
//                        }
//                    }
//                }, null);
//
//                try {
//                    mPreviewRequestBuilder
//                            = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//                    mPreviewRequestBuilder.addTarget(surface);
//
//                    mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
//
//                        @Override
//                        public void onConfigured(@NonNull CameraCaptureSession session) {
//                            if (null == mCameraDevice) {
//                                return;
//                            }
//                            mCameraCaptureSession = session;
//
//                            try {
//                                mPreviewRequest = mPreviewRequestBuilder.build();
//                                mCameraCaptureSession.setRepeatingRequest(mPreviewRequest,
//                                        null, null);
//                                takePicture(photoQuantity);
//                            } catch (CameraAccessException e) {
//                                emitter.onError(e);
//                            }
//                        }
//
//                        @Override
//                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
//                            emitter.onError(new CameraAccessException(CameraAccessException.CAMERA_ERROR, "configure failed"));
//                        }
//
//                    }, null);
//                } catch (CameraAccessException e) {
//                    emitter.onError(e);
//                }
//            }
//
//            @Override
//            public void onDisconnected(@NonNull CameraDevice camera) {
//                mCameraDevice.close();
//                mCameraDevice = null;
//                emitter.onComplete();
//            }
//
//            @Override
//            public void onError(@NonNull CameraDevice camera, int error) {
//                mCameraDevice.close();
//                mCameraDevice = null;
//                emitter.onError(new RuntimeException("camera error"));
//            }
//        }, null), BackpressureStrategy.BUFFER);
//    }
//
//    public void closeCamera() {
//        try {
//            if (null != mCameraCaptureSession) {
//                mCameraCaptureSession.stopRepeating();
//                mCameraCaptureSession.abortCaptures();
//                mCameraCaptureSession.close();
//                mCameraCaptureSession = null;
//            }
//            if (null != mCameraDevice) {
//                mCameraDevice.close();
//                mCameraDevice = null;
//            }
//            if (null != mImageReader) {
//                mImageReader.close();
//                mImageReader = null;
//            }
//        } catch (Exception e) {
//            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
//        }
//    }
//
//    public void takePicture(int photoQuantity) throws CameraAccessException {
//        final CaptureRequest.Builder captureBuilder =
//                mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//        captureBuilder.addTarget(mImageReader.getSurface());
//
//        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
//        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));
//        takePicture(captureBuilder, photoQuantity);
//    }
//
//    private int getOrientation(int rotation) {
//        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
//    }
//
//    private void takePicture(CaptureRequest.Builder captureBuilder, int photoQuantity) throws CameraAccessException {
//
//        CameraCaptureSession.CaptureCallback CaptureCallback
//                = new CameraCaptureSession.CaptureCallback() {
//            @Override
//            public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
//                super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
//                if (photoQuantity > 0) {
//                    try {
//                        takePicture(captureBuilder, photoQuantity - 1);
//                    } catch (CameraAccessException e) {}
//                }
//            }
//
//        };
//
//        mCameraCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
//    }

}
