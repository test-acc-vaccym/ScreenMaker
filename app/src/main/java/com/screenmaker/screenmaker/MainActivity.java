package com.screenmaker.screenmaker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.screenmaker.screenmaker.storage.cryptoinfo.ServiceCryptoInfo;
import com.screenmaker.screenmaker.utils.PermissionsUtils;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    private static final int RESULT_CAMERA_PERMISSION = 101;
    private static final int REQUEST_CODE_CAMERA_ACTIVITY = 202;

    static {
        System.loadLibrary("native-lib");
    }
    private Button mBtnPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnPhoto = findViewById(R.id.button);

        initImageEncryptKeys().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                mBtnPhoto.setEnabled(true);
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
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

    public void onPhotoClick(View v) {
        if (PermissionsUtils.isPermissionGranted(PermissionsUtils.Permissions.CAMERA)) {
            startActivityForResult(new Intent(this, CameraActivity.class), REQUEST_CODE_CAMERA_ACTIVITY);
        } else {
            PermissionsUtils.showPermissionDialogForResult(this, "I need camera", RESULT_CAMERA_PERMISSION);
        }
    }

    private Completable initImageEncryptKeys(){

        return Completable.create(e -> {

            /*Explanation:
              The following must be obtained from back-end.
              Undoubtedly, keeping the keys directly within application is a very bad practice.
              Given keys presented here for demo purposes only*/

            String imageKeyEncryptA = "imageKeyEncryptA";
            String imageKey = "imageKey";

            String imageAliasEncryptA = "imageAliasEncryptA";
            String imageAlias = "imageAlias";
            //////////////////////////////////////

            ServiceCryptoInfo serviceCryptoInfo = new ServiceCryptoInfo();
            long[] aliasAr = serviceCryptoInfo.encryptAndInsert(App.IMAGE_ENCRYPTION_ALIAS_TITLE, imageAlias, imageAliasEncryptA);
            long[] ketAr = serviceCryptoInfo.encryptAndInsert(App.IMAGE_ENCRYPTION_KEY_TITLE, imageKey, imageKeyEncryptA);

            if(aliasAr == null || aliasAr.length == 0 || aliasAr[0] == -1
                    || ketAr == null || ketAr.length == 0 || ketAr[0] == -1){
                e.onError(new InstantiationException("Failed to instantiate image params"));
            } else {
                e.onComplete();
            }
        });

    }

    public native String stringFromJNI();

}
