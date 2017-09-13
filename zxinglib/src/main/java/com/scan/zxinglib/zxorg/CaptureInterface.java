package com.scan.zxinglib.zxorg;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;

import com.google.zxing.Result;
import com.scan.zxinglib.camera.CameraManager;

/**
 * Created by admin on 2017/9/12.
 */

public interface CaptureInterface {

    ViewfinderView getViewfinderView();

    CameraManager getCameraManager();

    Handler getHandler();

    void drawViewfinder();

    void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor);

    void startActivity(Intent intent);

    PackageManager getPackageManager();

    Context getContext();

    void finish();

    void setResult(int rstCode,Intent intent);
}
