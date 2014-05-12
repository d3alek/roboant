package uk.ac.ed.insectlab.ant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;

public class Util {

	private static final String DIR_NAME = "RoboAnt5";


	protected static final String TAG = "Util";


	public static PictureCallback mPicture = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			File pictureFile = getOutputMediaFile(Constants.MEDIA_TYPE_IMAGE);
			if (pictureFile == null){
				Log.d(TAG, "Error creating media file, check storage permissions");
				return;
			}

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
				camera.startPreview();
			} catch (FileNotFoundException e) {
				Log.d(TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, "Error accessing file: " + e.getMessage());
			}
		}
	};

	public static Uri getUriFromPath(String filePath, Context context) {
		long photoId;
		//	    Uri photoUri = MediaStore.Images.Media.getContentUri("external");
		Uri photoUri = MediaStore.Files.getContentUri("external");

		String[] projection = {MediaStore.Images.ImageColumns._ID};
		// TODO This will break if we have no matching item in the MediaStore.
		Cursor cursor = context.getContentResolver().query(photoUri, projection, MediaStore.Files.FileColumns.DATA + " LIKE ?", new String[] { filePath }, null);
		cursor.moveToFirst();

		int columnIndex = cursor.getColumnIndex(projection[0]);
		photoId = cursor.getLong(columnIndex);

		cursor.close();
		return Uri.parse(photoUri.toString() + "/" + photoId + "/");
	}

	public static Uri getOutputMediaDirUri(Context context) {
		return getUriFromPath(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES) + File.separator + DIR_NAME, context);
	}

	/** Create a file Uri for saving an image or video */
	public static Uri getOutputMediaFileUri(int type){
		return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	public static File getOutputMediaFile(int type){
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		Log.i("Util", Environment.getExternalStorageState());

		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), DIR_NAME);
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				Log.d("Util", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		if (type == Constants.MEDIA_TYPE_IMAGE){
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"IMG_"+ timeStamp + ".jpg");
		} else if(type == Constants.MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"VID_"+ timeStamp + ".mp4");
		} else {
			return null;
		}

		return mediaFile;
	}

	public static File getNewRouteStorageDir(Context context) {
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), DIR_NAME);
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File routeStorageDir = new File(mediaStorageDir, "ROUTE_"+timeStamp);

		if (! routeStorageDir.exists()){
			if (! routeStorageDir.mkdirs()){
				Log.d("Util", "failed to create directory");
				return null;
			}
			else {
				context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(routeStorageDir)));
			}
		}

		return routeStorageDir;
	}

	public static File getRoutePictureFile(File dir, int num) {

		File routePictureFile = new File(dir.getPath() + File.separator + "IMG_" + num + ".jpg");

		return routePictureFile;
	}

	/** Check if this device has a camera */
	public static boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance(int camera_number) {
		Camera c = null;
		try {
			c = Camera.open(camera_number); // attempt to get a Camera instance
		}
		catch (Exception e){
			// Camera is not available (in use or does not exist)
			e.printStackTrace();
		}
		return c; // returns null if camera is unavailable
	}
	//
	public static void setCameraDisplayOrientation(Activity activity,
			int cameraId, android.hardware.Camera camera) {
		android.hardware.Camera.CameraInfo info =
				new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0: degrees = 0; break;
		case Surface.ROTATION_90: degrees = 90; break;
		case Surface.ROTATION_180: degrees = 180; break;
		case Surface.ROTATION_270: degrees = 270; break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
	}

	public static Options getRouteFollowingBitmapOpts() {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = 4;
		return opts;
	}

	public static Bitmap getCroppedBitmap(Bitmap bitmap, float centerXRatio, float centerYRatio, float cropRadiusRatio) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		// canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		Log.i(TAG, "Draw circle is " + centerXRatio + " " + centerYRatio + " " + cropRadiusRatio + " Image size is " + bitmap.getWidth() + " " + bitmap.getHeight());
		float centerX = (1 - centerYRatio) * rect.width();
		float centerY = (1 - centerXRatio) * rect.height();
		float radius = cropRadiusRatio * rect.height();
		canvas.drawCircle(centerX, centerY,
				radius, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		//Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
		//return _bmp;
		return output;
	}

	public static Bitmap rotateBitmap(Bitmap bmp, int rotate) {
		Bitmap targetBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
		Canvas canvas = new Canvas(targetBitmap);
		Matrix matrix = new Matrix();
		matrix.setRotate(rotate, bmp.getWidth()/2, bmp.getHeight()/2);
		canvas.drawBitmap(bmp, matrix, new Paint());
		return targetBitmap;
	}

	public static double minInArrray(double[] array) {
		double min = Double.MAX_VALUE;
		for (int i = 0; i < array.length; ++i) {
			if (array[i] < min) {
				min = array[i];
			}
		}
		return min;
	}

	

	public static double imagesSSD(Bitmap b1, Bitmap b2, List<Point> pointsToCheck) {
		return imagesSSD(b1, b2, 0, 0, pointsToCheck);
	}

	public synchronized static double imagesSSD(Bitmap b1, Bitmap b2, double ssdMin, double ssdMax, List<Point> pointsToCheck) {

		double ssd = 0;
		int pixel1, pixel2, g1, g2;

		if (pointsToCheck != null) {
			for (Point p : pointsToCheck) {
				pixel1 = b1.getPixel(p.x, p.y);
				pixel2 = b2.getPixel(p.x, p.y);
				g1 = Color.green(pixel1);
				g2 = Color.green(pixel2);
				ssd += (g1 - g2) * (g1 - g2);
			}
		}
		else {

			for (int i = 0; i < b1.getWidth(); ++i) {
				for (int j = 0; j < b1.getHeight(); ++j) {
					pixel1 = b1.getPixel(i, j);
					pixel2 = b2.getPixel(i, j);
					g1 = Color.green(pixel1);
					g2 = Color.green(pixel2);
					ssd += (g1 - g2) * (g1 - g2);
				}

			}
		}
		if (ssdMin == ssdMax && ssdMax == 0) {
			return ssd;
		}
		return normalizeSSD(ssd, ssdMin, ssdMax);

	}

	private static double normalizeSSD(double ssd, double ssdMin, double ssdMax) {

		double calibrated = (ssd - ssdMin)/(ssdMax - ssdMin);
		if (calibrated < 0 || calibrated > 1) {
			Log.w(TAG, "Calibrated is < 0 or > 1 " + calibrated + " " + ssd);
			if (calibrated < 0) {
				ssdMin = ssd;
			}
			else {
				ssdMax = ssd;
			}

			GLOBAL.getSettings().setSSDCalibrationResults(true, ssdMin, ssdMax);

			return normalizeSSD(ssd, ssdMin, ssdMax);
		}
		return calibrated;
	}
	
//	public static List<Point> getLensPixels(Lens lens) {
//		int left = lens.x - lens.radius;
//		int right = lens.x + lens.radius;
//		int bottom = lens.x + lens.radius;
//		int top = lens.x - lens.radius;
//		ArrayList<Point> points = new ArrayList<Point>();
//		for (int i = left; i < right; ++i) {
//			for (int j = top; j < bottom; ++j) {
//				if (pointsDist(i, j, lens.x, lens.y) <= lens.radius) {
//					points.add(new Point(i, j));
//				}
//			}
//
//		}
//		
//		Log.i(TAG, "For lens (" + lens.x + ", " + lens.y + ", " + lens.radius + ") " + points.size());
//		
//		return points;
//	}
	
	public static List<Point> getLensPixels(Bitmap sample) {
		ArrayList<Point> points = new ArrayList<Point>();
		int pixel;
		for (int i = 0; i < sample.getWidth(); ++i) {
			for (int j = 0; j < sample.getHeight(); ++j) {
				pixel = sample.getPixel(i, j);
				if (Color.green(pixel) != 0) {
					points.add(new Point(i, j));
				}
			}

		}
		
		Log.i(TAG, "For bitmap " + sample.getWidth() + ", " + sample.getHeight() + " - " + points.size());
		
		return points;
	}

	private static double pointsDist(int p1x, int p1y, int p2x, int p2y) {
		return Math.sqrt((p1x - p2x) * (p1x - p2x) + (p1y - p2y) * (p1y - p2y));
	}
}
