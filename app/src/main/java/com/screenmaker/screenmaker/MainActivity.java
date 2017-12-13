package com.screenmaker.screenmaker;


import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int RESULT_CAMERA_PERMISSION = 101;
    private static int COUNT = 1;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }


    private Camera camera;
    private Button btnPhoto;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnPhoto = findViewById(R.id.button);
        imageView = findViewById(R.id.imageView);
        try {
            camera = Camera.open(getFrontCamera());
        } catch (Exception e) {
            Toast.makeText(this, "No front camera on the given device", Toast.LENGTH_LONG).show();
        }
    }

    Camera.PictureCallback pictureCallback = (data, camera) -> {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if(bitmap==null){
            Toast.makeText(MainActivity.this, "Captured image is empty", Toast.LENGTH_LONG).show();
            return;
        } else {
            imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, imageView.getWidth(), imageView.getHeight(), true));
        }

        Log.e("myLogs", "photoTaken");
        Log.e("myLogs", "photoTaken" + data.length);
        Log.e("myLogs", "photoTaken" + camera.toString());
        if(COUNT < 10){
            COUNT++;
            makePhotos();
        }else {
            COUNT = 1;
            btnPhoto.setEnabled(true);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == RESULT_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhotos();
            } else {
                PermissionsUtils.showPermissionDialogForResult(this, "I need camera", RESULT_CAMERA_PERMISSION);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    public void onPhotoClick(View v){
        btnPhoto.setEnabled(false);
        if(PermissionsUtils.isPermissionGranted(PermissionsUtils.Permissions.CAMERA)){
            makePhotos();
        } else {
            PermissionsUtils.showPermissionDialogForResult(this, "I need camera", RESULT_CAMERA_PERMISSION);
        }
    }

    private void makePhotos(){
        camera.startPreview();
        camera.takePicture(null, null, pictureCallback);
    }

    public native String stringFromJNI();

    private int getFrontCamera() {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }
}
