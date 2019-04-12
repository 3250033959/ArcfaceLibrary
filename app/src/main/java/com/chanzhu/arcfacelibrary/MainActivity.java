package com.chanzhu.arcfacelibrary;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.chanzhu.arcfacelibrary.activity.RegisterAndRecognizeActivity;
import com.chanzhu.arcfacelibrary.common.Constants;
import com.chanzhu.arcfacelibrary.faceserver.FaceServer;
import com.chanzhu.arcfacelibrary.util.ImageUtil;
import com.chanzhu.arcfacelibrary.widget.ProgressDialog;
import com.darsh.multipleimageselect.activities.AlbumSelectActivity;
import com.darsh.multipleimageselect.models.Image;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE
            , Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA
    };///sdcard/DCIM/Camera/IMG_20190319_164936.jpg///sdcard/arcfacedemo/register
    private Toast toast;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressDialog = new ProgressDialog(this);
        activeEngine();
    }

    @Override
    protected void onDestroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        FaceServer.getInstance().unInit();
        super.onDestroy();
    }

    public void photoRegister(View view) {
        Intent intent = new Intent(MainActivity.this, AlbumSelectActivity.class);
        intent.putExtra(com.darsh.multipleimageselect.helpers.Constants.INTENT_EXTRA_LIMIT, 1);
        startActivityForResult(intent, com.darsh.multipleimageselect.helpers.Constants.REQUEST_CODE);
    }

    public void videoRegister(View view) {

    }

    public void registerConfirm(View view) {
        startActivity(new Intent(MainActivity.this, RegisterAndRecognizeActivity.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == com.darsh.multipleimageselect.helpers.Constants.REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            final ArrayList<Image> images = data.getParcelableArrayListExtra(com.darsh.multipleimageselect.helpers.Constants.INTENT_EXTRA_IMAGES);
//            StringBuffer stringBuffer = new StringBuffer();
//            for (int i = 0, l = images.size(); i < l; i++) {
//                stringBuffer.append(images.get(i).path + "\n");
//            }
//            ;
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.setMaxProgress(1);
                            progressDialog.show();
                        }
                    });
                    Bitmap bitmap = BitmapFactory.decodeFile(images.get(0).path);
                    bitmap = ImageUtil.alignBitmapForNv21(bitmap);
                    byte[] nv21 = ImageUtil.bitmapToNv21(bitmap, bitmap.getWidth(), bitmap.getHeight());
                    boolean success = FaceServer.getInstance().register(MainActivity.this, nv21, bitmap.getWidth(), bitmap.getHeight(),
                            images.get(0).path.substring(0, images.get(0).path.lastIndexOf(".")));
                    if (success)
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progressDialog != null) {
                                    progressDialog.refreshProgress(1);
                                    progressDialog.dismiss();
                                }
                                showToast(getString(R.string.register_success));
                            }
                        });
                }

            });
        }
    }

    /**
     * 激活引擎
     *
     * @param
     */
    public void activeEngine() {
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
        }

        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                FaceEngine faceEngine = new FaceEngine();
                int activeCode = faceEngine.active(MainActivity.this, Constants.APP_ID, Constants.SDK_KEY);
                emitter.onNext(activeCode);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {
                            showToast(getString(R.string.active_success));
                            FaceServer.getInstance().init(MainActivity.this);
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            showToast(getString(R.string.already_activated));
                        } else {
                            showToast(getString(R.string.active_failed, activeCode));
                        }

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (isAllGranted) {
                activeEngine();
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    private void showToast(String s) {
        if (toast == null) {
            toast = Toast.makeText(this, s, Toast.LENGTH_SHORT);
            toast.show();
        } else {
            toast.setText(s);
            toast.show();
        }
    }
}
