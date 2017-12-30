package com.screenmaker.screenmaker;

import android.content.Intent;
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

import com.screenmaker.screenmaker.storage.cryptoinfo.ServiceCryptoInfo;
import com.screenmaker.screenmaker.storage.images.ImageEntry;
import com.screenmaker.screenmaker.storage.images.ServiceImageEntry;
import com.screenmaker.screenmaker.utils.ImageCryptoUtils;
import com.screenmaker.screenmaker.utils.KeyCryptoUtils;
import com.screenmaker.screenmaker.utils.PermissionsUtils;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.crypto.NoSuchPaddingException;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableObserver;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;


public class MainActivity extends AppCompatActivity {

    private static final int RESULT_CAMERA_PERMISSION = 101;
    private static final int REQUEST_CODE_CAMERA_ACTIVITY = 202;

    static {
        System.loadLibrary("native-lib");
    }
    private Completable initCompletable;
    private Button btnPhoto;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnPhoto = findViewById(R.id.button);
        imageView = findViewById(R.id.imageView);
        Log.e("myLogs", "test ");

        initCompletable = initImageEncryptKeys();

        initCompletable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                Log.e("myLogs", "MainAc onComplete");
                btnPhoto.setEnabled(true);
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ServiceImageEntry serviceImageEntry = new ServiceImageEntry();
                List<ImageEntry> allImages = serviceImageEntry.getAllImages();
                Log.e("myLogs", "allImages " + allImages.size());
                if(allImages.size() > 0){
                    byte[] imageData = allImages.get(0).getImageData();
                    ServiceCryptoInfo serviceCryptoInfoo = new ServiceCryptoInfo();
                    ImageCryptoUtils imageCryptoUtils = new ImageCryptoUtils(serviceCryptoInfoo.getAndDecrypt(App.IMAGE_ENCRYPTION_KEY_TITLE),
                            serviceCryptoInfoo.getAndDecrypt(App.IMAGE_ENCRYPTION_ALIAS_TITLE));
                    try {
                        byte[] bytes = imageCryptoUtils.decryptImage(imageData);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        Log.e("myLogs", "bitmap " + bitmap);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, imageView.getWidth(), imageView.getHeight(), true));
                            }
                        });
                    } catch (NoSuchPaddingException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InvalidAlgorithmParameterException e) {
                        e.printStackTrace();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    }
                }


            }
        });
        thread.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == RESULT_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(new Intent(this, CameraActivity.class), REQUEST_CODE_CAMERA_ACTIVITY);
            } else {
                PermissionsUtils.showPermissionDialogForResult(this, "I need camera", RESULT_CAMERA_PERMISSION);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){

        }
    }

    public void onPhotoClick(View v) {
        if (PermissionsUtils.isPermissionGranted(PermissionsUtils.Permissions.CAMERA)) {
            startActivityForResult(new Intent(this, CameraActivity.class), REQUEST_CODE_CAMERA_ACTIVITY);
        } else {
            PermissionsUtils.showPermissionDialogForResult(this, "I need camera", RESULT_CAMERA_PERMISSION);
        }
    }

    private Completable initImageEncryptKeys(){

        return Completable.create(e -> {

            //Info: the following must be obtained
            Log.e("myLogs", "initImageEncryptKeys");
            String imageKeyEncryptA = "imageKeyEncryptA";
            String imageKey = "imageKey";

            String imageAliasEncryptA = "imageAliasEncryptA";
            String imageAlias = "imageAlias";
            //////////////////////////////////////







            ServiceCryptoInfo serviceCryptoInfo = new ServiceCryptoInfo();
            long[] aliasAr = serviceCryptoInfo.encryptAndInsert(App.IMAGE_ENCRYPTION_ALIAS_TITLE, imageAlias, imageAliasEncryptA);
            long[] ketAr = serviceCryptoInfo.encryptAndInsert(App.IMAGE_ENCRYPTION_KEY_TITLE, imageKey, imageKeyEncryptA);

            ServiceCryptoInfo serviceCryptoInfo2 = new ServiceCryptoInfo();
            Log.e("myLogs", "initImageEncryptKeys unc " + serviceCryptoInfo2.getAndDecrypt(App.IMAGE_ENCRYPTION_KEY_TITLE));
            Log.e("myLogs", "initImageEncryptKeys unc " + serviceCryptoInfo2.getAndDecrypt(App.IMAGE_ENCRYPTION_ALIAS_TITLE));


            if(aliasAr == null || aliasAr.length == 0 || aliasAr[0] == -1
                    || ketAr == null || ketAr.length == 0 || ketAr[0] == -1){
                e.onError(new InstantiationException("Failed to instantiate image params"));
            } else {
                e.onComplete();
            }
        });

    }

//    private void makePhotos() {
//        makeThePhoto(camera).map(bytes -> {
//            Log.e("myLogs", "encryptImage apply");
//
//            ServiceImageEntry serviceImageEntry = new ServiceImageEntry();
//            long[] id = serviceImageEntry.insertImage(new ImageEntry("test".getBytes()));
//            Log.e("myLogs", "encryptImage save " + id[0]);
//            //serviceImageEntry.clearAll();
//            Log.e("myLogs", "encryptImage clearAll");
//            String testKey = "TESTall i need is toyu";
//
//            String testSalt = "FD7BADF2FBB1999YUZ";
//
//            ImageCryptoUtils cryptoUtils = new ImageCryptoUtils(testKey, testSalt);
//
//            byte[] b = cryptoUtils.encryptImage(bytes);
//            Log.e("myLogs", "encryptImage " + b.length);
//
//            Log.e("myLogs", "insertImage " + b.length);
//            return bytes;
//        }).subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribeWith(new DisposableSubscriber<byte[]>() {
//                    @Override
//                    public void onNext(byte[] bytes) {
//                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, imageView.getWidth(), imageView.getHeight(), true));
//                    }
//
//                    @Override
//                    public void onError(Throwable t) {
//
//                    }
//
//                    @Override
//                    public void onComplete() {
//                        btnPhoto.setEnabled(true);
//                    }
//                });
//    }
//
//    private Flowable<byte[]> makeThePhoto(Camera camera) {
//        return Flowable.create(e -> {
//            camera.startPreview();
//            camera.takePicture(null, null, (bytes, camera1) -> {
//                e.onNext(bytes);
//                e.onComplete();
//            });
//        }, BackpressureStrategy.BUFFER);
//    }

    public native String stringFromJNI();

//    private int getFrontCamera() {
//        int cameraId = -1;
//        int numberOfCameras = Camera.getNumberOfCameras();
//        for (int i = 0; i < numberOfCameras; i++) {
//            Camera.CameraInfo info = new Camera.CameraInfo();
//            Camera.getCameraInfo(i, info);
//            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                cameraId = i;
//                break;
//            }
//        }
//        return cameraId;
//    }
}
