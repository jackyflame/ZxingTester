package com.scan.zxinglib.customer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.scan.zxinglib.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangzhen on 2016/2/3.
 */
public class CameraSetting {

	Camera.Parameters parameters;
	public int srcWidth, srcHeight;
	public int preWidth, preHeight;
	public int picWidth, picHeight;
	public int surfaceWidth, surfaceHeight;
	List<Camera.Size> supportSizeList;
	private boolean isShowBorder = false;
	private Context context;

	private CameraSetting(Context context) {
		this.context = context;
		setScreenSize(context);
	}

	private static CameraSetting single = null;

	/**
	 * 静态工厂方法
	 */
	public static CameraSetting getInstance(Context context) {
		if (single == null) {
			single = new CameraSetting(context);
		}
		return single;
	}

	/**
	 * @return Camera 返回类型
	 * @Title: 关闭相机
	 * @Description: 释放相机资源
	 */
	public Camera closeCamera(Camera camera) {
		try {
			if (camera != null) {
				camera.setPreviewCallback(null);
				camera.stopPreview();
				camera.release();
				camera = null;
			}
		} catch (Exception e) {
			Log.i("TAG", e.getMessage());
		}
		return camera;
	}

	/**
	 * @Title: ${enclosing_method}
	 * @Description: 打开闪光灯
	 * @param camera 相机对象
	 * @return void 返回类型
	 * @throws
	 */
	public void openCameraFlash(Camera camera) {
		if (camera == null){
			camera = Camera.open();
		}
		Camera.Parameters parameters = camera.getParameters();
		List<String> flashList = parameters.getSupportedFlashModes();
		if (flashList != null && flashList.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			camera.setParameters(parameters);
		} else {
			Toast.makeText(context, context.getString(R.string.unsupportflash),Toast.LENGTH_SHORT).show();
		}

	}

	/**
	 * @Title: ${enclosing_method}
	 * @Description: 关闭闪光灯
	 * @param camera 相机对象
	 * @return void 返回类型
	 * @throws
	 */
	public void closedCameraFlash(Camera camera) {
		if (camera == null){
			camera = Camera.open();
		}
		Camera.Parameters parameters = camera.getParameters();
		List<String> flashList = parameters.getSupportedFlashModes();
		if (flashList != null && flashList.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			camera.setParameters(parameters);
		} else {
			Toast.makeText(context, context.getString(R.string.unsupportflash),Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * @param currentActivity
	 * @param sizes 最理想的预览分辨率的宽和高
	 * @param targetRatio
	 * @return 获得最理想的预览尺寸
	 */
	public static Camera.Size getOptimalPreviewSize(Activity currentActivity, List<Camera.Size> sizes, double targetRatio) {
		// Use a very small tolerance because we want an exact match.
		final double ASPECT_TOLERANCE = 0.001;
		if (sizes == null)
			return null;

		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		// Because of bugs of overlay and layout, we sometimes will try to
		// layout the viewfinder in the portrait orientation and thus get the
		// wrong size of mSurfaceView. When we change the preview size, the
		// new overlay will be created before the old one closed, which causes
		// an exception. For now, just get the screen size

		Display display = currentActivity.getWindowManager().getDefaultDisplay();
		int targetHeight = Math.min(display.getHeight(), display.getWidth());

		if (targetHeight <= 0) {
			// We don't know the size of SurfaceView, use screen height
			targetHeight = display.getHeight();
		}

		//如果是竖屏，会导致尺寸对不上，Camera支持尺寸均以横屏来计算
		if(targetRatio < 1 && currentActivity.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
			targetRatio = 1/targetRatio;
		}

		// Try to find an size match aspect ratio and size
		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);			}
		}

		// Cannot find the one match the aspect ratio. This should not happen.
		// Ignore the requirement.
		if (optimalSize == null) {
			System.out.println("No preview size match the aspect ratio");
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	/**
	 * 检查相机参数,以便启动自定义相机
	 */
	public List<Integer> checkCameraParameters(Camera camera, int width, int height) {
		int srcwidth = 640;
		int srcheight = 480;
		List<Integer> list = new ArrayList<Integer>();
		try {
			if (camera != null) {
				// 读取支持的预览尺寸,优先选择640后320
				Camera.Parameters parameters = camera.getParameters();
				List<Camera.Size> PictureSizes = new ArrayList<>();
				//PictureSizes = CameraUtils.splitSize(parameters.get("picture-size-values"), camera);
				PictureSizes = parameters.getSupportedPictureSizes();
				if (width * 9 == height * 16) {
					for (int i = 0; i < PictureSizes.size(); i++) {
						if ((PictureSizes.get(i).width <= 2048 || PictureSizes .get(i).height <= 1536)
								&& ((PictureSizes.get(i).width * 9 == PictureSizes.get(i).height * 16)
								|| PictureSizes.get(i).width * 3 == PictureSizes.get(i).height * 4)) {
							if (PictureSizes.get(i).width > srcwidth || PictureSizes.get(i).height > srcheight) {
								srcwidth = PictureSizes.get(i).width;
								srcheight = PictureSizes.get(i).height;
							}
						}
					}
				} else {
					for (int i = 0; i < PictureSizes.size(); i++) {

						if (PictureSizes.get(i).width == 2048 && PictureSizes.get(i).height == 1536) {
							srcwidth = 2048;
							srcheight = 1536;
						}
						if (PictureSizes.get(i).width == 1920 && PictureSizes.get(i).height == 1080
								&& srcwidth < PictureSizes.get(i).width) {
							srcwidth = PictureSizes.get(i).width;
							srcheight = PictureSizes.get(i).height;
						}
						if (PictureSizes.get(i).width == 1600 && PictureSizes.get(i).height == 1200
								&& srcwidth < PictureSizes.get(i).width) {
							srcwidth = 1600;
							srcheight = 1200;
						}
						if (PictureSizes.get(i).width == 1280 && PictureSizes.get(i).height == 960
								&& srcwidth < PictureSizes.get(i).width) {
							srcwidth = 1280;
							srcheight = 960;

						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		list.clear();
		list.add(0, srcwidth);
		list.add(1, srcheight);
		return list;
	}

	/**
	 * @param mDecorView 设定文件
	 * @Title: 沉寂模式
	 * @Description: 隐藏虚拟按键
	 */
	@TargetApi(19)
	public void hiddenVirtualButtons(View mDecorView) {
		if (Build.VERSION.SDK_INT >= 19) {
			mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE);
		}
	}

	/**
	 * @param previewCallback 相机界面预览回调函数
	 * @param surfaceHolder 相机界面的holder
	 * @param camera 相机对象
	 * @param cancelAutoFocus  是否取消自动对焦
	 * @param currentActivity 相机界面的Activity对象
	 * @param screenRatio 屏幕的宽高比
	 * @return ${return_type} 返回类型
	 * @throws
	 * @Title: ${enclosing_method}
	 * @Description: 设置相机的详细参数
	 */
	public void setCameraParameters(Camera.PreviewCallback previewCallback,
									SurfaceHolder surfaceHolder, Activity currentActivity,
									Camera camera, float screenRatio, boolean cancelAutoFocus) {
		setCameraParameters(previewCallback, surfaceHolder, currentActivity, camera, screenRatio, null ,cancelAutoFocus);
	}

	/**
	 * @param previewCallback 相机界面预览回调函数
	 * @param surfaceHolder 相机界面的holder
	 * @param camera 相机对象
	 * @param cancelAutoFocus  是否取消自动对焦
	 * @param currentActivity 相机界面的Activity对象
	 * @param screenRatio 屏幕的宽高比
	 * @param srcList 拍照分辨率的宽高容器
	 * @return ${return_type} 返回类型
	 * @throws
	 * @Title: ${enclosing_method}
	 * @Description: 设置相机的详细参数
	 */
	public void setCameraParameters(Camera.PreviewCallback previewCallback,SurfaceHolder surfaceHolder, Activity currentActivity,
			Camera camera, float screenRatio, List<Integer> srcList,boolean cancelAutoFocus) {

		if(camera == null){
			return;
		}

		Camera.Parameters parameters = camera.getParameters();
		// if (parameters.getSupportedFocusModes().contains(
		// parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
		// parameters.setFocusMode(parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		// } else
		if (parameters.getSupportedFocusModes().contains(parameters.FOCUS_MODE_AUTO)) {
			parameters.setFocusMode(parameters.FOCUS_MODE_AUTO);
		}
		parameters.setPictureFormat(PixelFormat.JPEG);
		// if (srcList.get(0) >= 1280 && srcList.get(1) >= 720) {
		// parameters.setPictureSize(srcList.get(0), srcList.get(1));
		// System.out.println("srcwidth:" + srcList.get(0) + "--srcheight:"
		// + srcList.get(1));
		// }
		if(supportSizeList == null || supportSizeList.size() == 0){
			supportSizeList = camera.getParameters().getSupportedPreviewSizes();
		}
		Camera.Size optimalPreviewSize = CameraSetting.getInstance(context)
				.getOptimalPreviewSize(currentActivity, supportSizeList, screenRatio);
		//竖屏
		if(context.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
			parameters.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);
			parameters.set("orientation", "portrait");
			parameters.set("rotation", 90); // 镜头角度转90度（默认摄像头是横拍）
			parameters.setRotation(90);
			camera.setDisplayOrientation(90); // 在2.2以上可以使用
		}else{
			parameters.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);
			parameters.set("orientation", "landscape"); //
			parameters.setRotation(0);
			camera.setDisplayOrientation(0); // 在2.2以上可以使用
		}
		try {
			camera.setPreviewDisplay(surfaceHolder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (cancelAutoFocus) {
			camera.cancelAutoFocus();
		}
		try{
			parameters.setPreviewFormat(ImageFormat.NV21);
			//放大倍数
			if (parameters.isZoomSupported()) {
				int zoom = (parameters.getMaxZoom() * 1) / 10;
				if (zoom < parameters.getMaxZoom() && zoom > 0) {
					parameters.setZoom(zoom);
				}
				parameters.setZoom(zoom);
			}
			parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);

			////设置测距区域
			//if (parameters.getMaxNumMeteringAreas() > 0){
			//	List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
			//	// specify an area in center of image
			//	Rect meteringRect = cacu;
			//	// set weight to 60%
			//	meteringAreas.add(new Camera.Area(meteringRect, 1000));
			//	parameters.setMeteringAreas(meteringAreas);
			//}

			////设置聚焦区域
			//if (parameters.getMaxNumFocusAreas() > 0) {
			//	List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
			//	focusAreas.add(new Camera.Area(focusRect, 1000));
			//	parameters.setFocusAreas(focusAreas);
			//}

			camera.setParameters(parameters);
			camera.setPreviewCallback(previewCallback);
			camera.startPreview();
		}catch (Exception e){
			e.printStackTrace();
			Toast.makeText(currentActivity,"暂不支持该分辨率机型",Toast.LENGTH_SHORT).show();
		}

	}

	@SuppressLint("NewApi")
	private void setScreenSize(Context context) {
		int x, y;
		WindowManager wm = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
		Display display = wm.getDefaultDisplay();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			Point screenSize = new Point();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
				display.getRealSize(screenSize);
				x = screenSize.x;
				y = screenSize.y;
			} else {
				display.getSize(screenSize);
				x = screenSize.x;
				y = screenSize.y;
			}
		} else {
			x = display.getWidth();
			y = display.getHeight();
		}

		srcWidth = x;
		srcHeight = y;
	}

	/**
	 * 获取设备的预览分辨率的宽和高
	 * @param camera
	 */
	public void getCameraPreParameters(Camera camera){
		isShowBorder = false;
		// 荣耀七设备
		if ("PLK-TL01H".equals(Build.MODEL)) {
			preWidth = 1920;
			preHeight = 1080;
			return;
		}
		// 其他设备
		parameters = camera.getParameters();
		supportSizeList = parameters.getSupportedPreviewSizes();
		float ratioScreen = (float) srcWidth / srcHeight;

		for (int i = 0; i < supportSizeList.size(); i++) {
			float ratioPreview = (float) supportSizeList.get(i).width / supportSizeList.get(i).height;
			if (ratioScreen == ratioPreview) {// 判断屏幕宽高比是否与预览宽高比一样，如果一样执行如下代码
				if (supportSizeList.get(i).width >= 1280 || supportSizeList.get(i).height >= 720) {// 默认预览以1280*720为标准
					if (preWidth == 0 && preHeight == 0) {// 初始值
						preWidth = supportSizeList.get(i).width;
						preHeight = supportSizeList.get(i).height;
					}
					if (supportSizeList.get(0).width > supportSizeList.get(supportSizeList.size() - 1).width) {
						// 如果第一个值大于最后一个值
						if (preWidth > supportSizeList.get(i).width
								|| preHeight > supportSizeList.get(i).height) {
							// 当有大于1280*720的分辨率但是小于之前记载的分辨率，我们取中间的分辨率
							preWidth = supportSizeList.get(i).width;
							preHeight = supportSizeList.get(i).height;
						}
					} else {
						// 如果第一个值小于最后一个值
						if (preWidth < supportSizeList.get(i).width
								|| preHeight < supportSizeList.get(i).height) {
							// 如果之前的宽度和高度大于等于1280*720，就不需要再筛选了
							if (preWidth >= 1280 || preHeight >= 720) {

							} else {
								// 为了找到合适的分辨率，如果preWidth和preHeight没有比1280*720大的就继续过滤
								preWidth = supportSizeList.get(i).width;
								preHeight = supportSizeList.get(i).height;
							}
						}
					}
				}
			}
		}
		// 说明没有找到程序想要的分辨率
		if (preWidth == 0 || preHeight == 0) {
			isShowBorder = true;
			preWidth = supportSizeList.get(0).width;
			preHeight = supportSizeList.get(0).height;
			for (int i = 0; i < supportSizeList.size(); i++) {

				if (supportSizeList.get(0).width > supportSizeList.get(supportSizeList.size() - 1).width) {
					// 如果第一个值大于最后一个值
					if (preWidth >= supportSizeList.get(i).width
							|| preHeight >= supportSizeList.get(i).height) {
						// 当上一个选择的预览分辨率宽或者高度大于本次的宽度和高度时，执行如下代码:
						if (supportSizeList.get(i).width >= 1280) {
							// 当本次的预览宽度和高度大于1280*720时执行如下代码
							preWidth = supportSizeList.get(i).width;
							preHeight = supportSizeList.get(i).height;

						}
					}
				} else {
					if (preWidth <= supportSizeList.get(i).width
							|| preHeight <= supportSizeList.get(i).height) {
						if (preWidth >= 1280 || preHeight >= 720) {

						} else {
							// 当上一个选择的预览分辨率宽或者高度大于本次的宽度和高度时，执行如下代码:
							if (supportSizeList.get(i).width >= 1280) {
								// 当本次的预览宽度和高度大于1280*720时执行如下代码
								preWidth = supportSizeList.get(i).width;
								preHeight = supportSizeList.get(i).height;

							}
						}

					}
				}
			}
		}
		// 如果没有找到大于1280*720的分辨率的话，取集合中的最大值进行匹配
		if (preWidth == 0 || preHeight == 0) {
			isShowBorder = true;
			if (supportSizeList.get(0).width > supportSizeList.get(supportSizeList.size() - 1).width) {
				preWidth = supportSizeList.get(0).width;
				preHeight = supportSizeList.get(0).height;
			} else {
				preWidth = supportSizeList.get(supportSizeList.size() - 1).width;
				preHeight = supportSizeList.get(supportSizeList.size() - 1).height;
			}
		}
		if (isShowBorder) {
			if (ratioScreen > (float) preWidth / preHeight) {
				surfaceWidth = (int) (((float) preWidth / preHeight) * srcHeight);
				surfaceHeight = srcHeight;
			} else {
				surfaceWidth = srcWidth;
				surfaceHeight = (int) (((float) preHeight / preWidth) * srcHeight);
			}
		} else {
			surfaceWidth = srcWidth;
			surfaceHeight = srcHeight;
		}
	
	}
}
