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
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import java.nio.ByteBuffer;
import java.util.Arrays;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

public class CameraHelper {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private static final int DEFAULT_WIDTH = 1920;
    private static final int DEFAULT_HEIGHT = 1080;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Activity mActivity;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice = null;
    private CameraCaptureSession mCameraCaptureSession;

    private ImageReader mImageReader;

    private String mCameraID;
    private TextureView mTextureView;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private Integer mSensorOrientation;

    public CameraHelper(Context context) {
        this.mActivity = (Activity) context;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void init(int cameraId) throws CameraAccessException {
        String[] cameraList = mCameraManager.getCameraIdList();
        CameraCharacteristics cameraCharacteristics = null;
        for (String cameraID : cameraList) {
            cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraID);
            Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraId == facing) {
                this.mCameraID = cameraID;
                mSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                break;
            }
        }

        if (this.mCameraID == null) {
            throw new CameraAccessException(CameraAccessException.CAMERA_DISABLED);
        }
    }

    public void setTextureView(TextureView textureView) {
        mTextureView = textureView;
    }

    @SuppressLint("MissingPermission")
    public Flowable<byte[]> openCamera(int photoQuantity) {
        final int[] savedPhoto = {0};
        return Flowable.create(emitter -> mCameraManager.openCamera(mCameraID, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                mCameraDevice = camera;
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                texture.setDefaultBufferSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
                Surface surface = new Surface(texture);

                mImageReader = ImageReader.newInstance(DEFAULT_WIDTH, DEFAULT_HEIGHT,
                        ImageFormat.JPEG, 1);
                mImageReader.setOnImageAvailableListener(reader -> {
                    Image image = null;
                    while ((image = reader.acquireNextImage()) != null){
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        buffer.rewind();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        image.close();
                        emitter.onNext(bytes);
                        if(savedPhoto[0] == photoQuantity){
                            emitter.onComplete();
                        } else {
                            savedPhoto[0]++;
                        }
                    }
                }, null);

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
                                takePicture(photoQuantity);
                            } catch (CameraAccessException e) {
                                emitter.onError(e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            emitter.onError(new CameraAccessException(CameraAccessException.CAMERA_ERROR, "configure failed"));
                        }

                    }, null);
                } catch (CameraAccessException e) {
                    emitter.onError(e);
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                mCameraDevice.close();
                mCameraDevice = null;
                emitter.onComplete();
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                mCameraDevice.close();
                mCameraDevice = null;
                emitter.onError(new RuntimeException("camera error"));
            }
        }, null), BackpressureStrategy.BUFFER);
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
        }
    }

    public void takePicture(int photoQuantity) throws CameraAccessException {
        final CaptureRequest.Builder captureBuilder =
                mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(mImageReader.getSurface());

        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));
        takePicture(captureBuilder, photoQuantity);
    }

    private int getOrientation(int rotation) {
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private void takePicture(CaptureRequest.Builder captureBuilder, int photoQuantity) throws CameraAccessException {

        CameraCaptureSession.CaptureCallback CaptureCallback
                = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                if (photoQuantity > 0) {
                    try {
                        takePicture(captureBuilder, photoQuantity - 1);
                    } catch (CameraAccessException e) {}
                }
            }

        };

        mCameraCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
    }

}
