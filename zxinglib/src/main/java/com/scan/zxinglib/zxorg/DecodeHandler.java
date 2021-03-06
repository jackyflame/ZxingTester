/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scan.zxinglib.zxorg;

import android.graphics.Bitmap;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.scan.zxinglib.R;
import com.scan.zxinglib.customer.CGlobal;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Map;

final class DecodeHandler extends Handler {

  private static final String TAG = DecodeHandler.class.getSimpleName();

  private final CaptureInterface activity;
  private final MultiFormatReader multiFormatReader;
  private boolean running = true;
  private final Map<DecodeHintType,Object> hints;

  DecodeHandler(CaptureInterface activity, Map<DecodeHintType,Object> hints) {
    multiFormatReader = new MultiFormatReader();
    multiFormatReader.setHints(hints);
    this.hints = hints;
    this.activity = activity;
  }

  @Override
  public void handleMessage(Message message) {
    if (message == null || !running) {
      return;
    }
    if (message.what == R.id.decode) {
      decode((byte[]) message.obj, message.arg1, message.arg2);
    } else if (message.what == R.id.quit) {
      running = false;
      Looper.myLooper().quit();
    }
  }

  /**
   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
   * reuse the same reader objects from one decode to the next.
   *
   * @param data   The YUV preview frame.
   * @param width  The width of the preview frame.
   * @param height The height of the preview frame.
   */
  private void decode(byte[] data, int width, int height) {
    long start = System.currentTimeMillis();
    Result rawResult = null;

//    Bitmap bitmapCorp =  CGlobal.makeCropedGrayBitmap(data, width, height, 90, activity.getCameraManager().getFramingRect());
//    if (bitmapCorp != null) {
//      int[] pixels = new int[bitmapCorp.getWidth() * bitmapCorp.getHeight()];
//      bitmapCorp.getPixels(pixels, 0, bitmapCorp.getWidth(), 0, 0, bitmapCorp.getWidth(), bitmapCorp.getHeight());
//      RGBLuminanceSource source = new RGBLuminanceSource(bitmapCorp.getWidth(), bitmapCorp.getHeight(), pixels);
//      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
//      try {
//        //rawResult = new QRCodeReader().decode(bitmap,hints);
//        //rawResult = new QRCodeReader().decode(bitmap);
//        //rawResult = new QRCodeMultiReader().decode(bitmap);
//        rawResult = multiFormatReader.decodeWithState(bitmap);
//      } catch (ReaderException re) {
//        // continue
//      } finally {
//        multiFormatReader.reset();
//      }
//    }

    PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);
    if (source != null) {
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      try {
        //rawResult = new QRCodeReader().decode(bitmap,hints);
        //rawResult = new QRCodeReader().decode(bitmap);
        //rawResult = new QRCodeMultiReader().decode(bitmap);
        rawResult = multiFormatReader.decodeWithState(bitmap);
      } catch (ReaderException re) {
        // continue
      } finally {
        multiFormatReader.reset();
      }
    }

    Handler handler = activity.getHandler();
    if (rawResult != null) {
      // Don't log the barcode contents for security.
      long end = System.currentTimeMillis();
      Log.d(TAG, "Found barcode in " + (end - start) + " ms");
      if (handler != null) {
        Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
        Bundle bundle = new Bundle();
        bundleThumbnail(source, bundle);
//        bundleThumbnail(bitmapCorp, bundle);
        message.setData(bundle);
        message.sendToTarget();
      }
    } else {
      if (handler != null) {
        Message message = Message.obtain(handler, R.id.decode_failed);
        Bundle bundle = new Bundle();
        bundleThumbnail(source, bundle);
//        bundleThumbnail(bitmapCorp, bundle);
        message.setData(bundle);
        message.sendToTarget();
      }
    }
  }

  private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
    int[] pixels = source.renderThumbnail();
    int width = source.getThumbnailWidth();
    int height = source.getThumbnailHeight();
    Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
    ByteArrayOutputStream out = new ByteArrayOutputStream();    
    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
    bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
    bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
  }

  private static void bundleThumbnail(Bitmap bitmap, Bundle bundle) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
    bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
    bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) bitmap.getHeight() / bitmap.getWidth());
  }
}
