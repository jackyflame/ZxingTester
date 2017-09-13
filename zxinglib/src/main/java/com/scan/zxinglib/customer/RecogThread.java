package com.scan.zxinglib.customer;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.View;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.scan.zxinglib.zxorg.DecodeFormatManager;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;


/**
 * Created by Android Studio.
 * ProjectName: OrcTester
 * Author: haozi
 * Date: 2017/7/20
 * Time: 11:54
 */

public class RecogThread extends Thread{

    private static final String TAG = "RecogThread2";
    /**关闭标记*/
    private static boolean stopRecogMark = false;
    //初始化扫描区域缓存
    private Rect scanareaRect;
    //时间计算戳
    private long time;
    //时间计算戳
    private long mattime;
    //刷新识别区域标记
    private boolean isRefreshScanarea = true;

    //摄像头引用
    private Camera camera;
    //识别范围标记View
    private View iv_camera_scanarea;

    //识别回调
    private RecogTaskListener recogTaskListener;
    // 扫描视频数据
    private ArrayBlockingQueue<byte[]> mPreviewQueue = new ArrayBlockingQueue<>(1);

    public RecogThread(Camera camera, View iv_camera_scanarea, RecogTaskListener recogTaskListener) {
        this.camera = camera;
        this.iv_camera_scanarea = iv_camera_scanarea;
        this.recogTaskListener = recogTaskListener;
    }

    @Override
    public void run() {
        try{
            while (true) {
                //如果关闭则退出识别
                if (checkRecogStop()) {
                    mPreviewQueue.clear();
                    return;
                }
                //获取数据
                Log.i(TAG, "------------->>take preview start");
                byte[] previewImgData = mPreviewQueue.take();
                Log.i(TAG, "------------->>take preview end");
                //记录起始时间
                time = System.currentTimeMillis();
                //转换原始图像数据
                int width = camera.getParameters().getPreviewSize().width;
                int height = camera.getParameters().getPreviewSize().height;
                //检查数据是否为空
                if (previewImgData == null || checkRecogStop()) {
                    continue;
                }
                //重新设置识别区域和识别OCRID
                if (isRefreshScanarea || scanareaRect == null) {
                    //初始化扫描区域缓存
                    scanareaRect = new Rect();
                    //获取扫描坐标
                    iv_camera_scanarea.getGlobalVisibleRect(scanareaRect);
                }
                //检查是否停止识别
                if (checkRecogStop()) {
                    return;
                }
                //识别
                Rect scropRect = CGlobal.GetRotateRect(scanareaRect, 90);
                Bitmap imgBitmap = CGlobal.makeCropedGrayBitmap(previewImgData, width, height, 90, scropRect);
                //检查是否停止识别
                if (checkRecogStop()) {
                    return;
                }
                //调用识别
                decode(imgBitmap,width,height);
                Log.i(TAG, "------------->>Recoging finished...！！！！！");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void stopScan(){
        stopRecogMark = true;
        if(mPreviewQueue != null){
            mPreviewQueue.clear();
        }
    }

    public void setRecogTaskListener(RecogTaskListener recogTaskListener) {
        this.recogTaskListener = recogTaskListener;
    }

    public static boolean isRecogStoped() {
        return stopRecogMark;
    }

    private boolean checkRecogStop(){
        if(isRecogStoped() == true){
            if(recogTaskListener != null){
                Exception exception = new Exception("Recog stop by user...");
                recogTaskListener.recogError(exception);
            }
            return true;
        }
        return false;
    }

    public void addDetect(byte[] data) {
        if (mPreviewQueue.size() >= 1) {
            mPreviewQueue.clear();
        }
        mPreviewQueue.add(data);
    }

    private MultiFormatReader multiFormatReader;

    private void decode(Bitmap bitmap, int width, int height) {

        long start = System.currentTimeMillis();

        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), pixels);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

        Result  rawResult = null;

        if (binaryBitmap != null) {
            try {
                if(multiFormatReader == null){
                    multiFormatReader = new MultiFormatReader();
                    Map<DecodeHintType,Object> hints = new EnumMap<>(DecodeHintType.class);
                    Collection<BarcodeFormat> decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
                    decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
                    decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
                    hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
                    multiFormatReader.setHints(hints);
                }
                //rawResult = new QRCodeReader().decode(bitmap,hints);
                //rawResult = new QRCodeReader().decode(bitmap);
                //rawResult = new QRCodeMultiReader().decode(bitmap);
                rawResult = multiFormatReader.decodeWithState(binaryBitmap);
            } catch (ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }
        }
        if(recogTaskListener != null){
            String rstStr = "";
            if(rawResult != null){
                rstStr = rawResult.getText();
            }
            recogTaskListener.recogSuccess(rstStr,bitmap);
        }
    }
}
