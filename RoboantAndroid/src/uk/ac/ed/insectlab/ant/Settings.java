package uk.ac.ed.insectlab.ant;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Settings {

	private static final String PREFS_NAME = "roboant";
	private static final String KEY_LENS_X = "lens_x";
	private static final String KEY_LENS_Y = "lens_y";
	private static final String KEY_LENS_RAD = "lens_rad";
	private static final String KEY_SSD_MIN = "ssd_min";
	private static final String KEY_SSD_MAX = "ssd_max";
	private static final String KEY_SSD_CALIBRATED = "ssd_calibrated";
	private static final String KEY_SERVER_IP = "server_ip";
	private static final String KEY_SERVER_PORT = "server_port";
	private static final String KEY_IMAGE_PIXELS_NUM = "image_pixels";
	private SharedPreferences mPrefs;

	public Settings(Context context) {
		mPrefs = context.getSharedPreferences(PREFS_NAME, 0);
	}

	public SharedPreferences getSharedPrefs() {
		return mPrefs;
	}

	public Lens loadLens() {
		if (mPrefs.getInt(KEY_LENS_RAD, -1) != -1) {
			return new Lens(mPrefs.getInt(KEY_LENS_X, 0),
					mPrefs.getInt(KEY_LENS_Y, 0),
					mPrefs.getInt(KEY_LENS_RAD, 0));
		}
		return null;
	}

	public void saveLens(Lens lens) {
		Editor e = mPrefs.edit();
		e.putInt(KEY_LENS_X, lens.x);
		e.putInt(KEY_LENS_Y, lens.y);
		e.putInt(KEY_LENS_RAD, lens.radius);
		e.apply();
	}

	public double getSSDMin() {
		return mPrefs.getFloat(KEY_SSD_MIN, 0);
	}
	
	public double getSSDMax() {
		return mPrefs.getFloat(KEY_SSD_MAX, 0);
	}
	
	public boolean getSSDCalibrated() {
		return mPrefs.getBoolean(KEY_SSD_CALIBRATED, false);
	}
	
	public void setSSDCalibrationResults(boolean calibrated, double min, double max) {
		Editor e  = mPrefs.edit();
		e.putBoolean(KEY_SSD_CALIBRATED, calibrated);
		if (calibrated) {
			e.putFloat(KEY_SSD_MIN, (float)min);
			e.putFloat(KEY_SSD_MAX, (float)max);
		}
		e.apply();
	}

	public void setServerAddress(String ip, int port) {
		Editor e = mPrefs.edit();
		e.putString(KEY_SERVER_IP, ip);
		e.putInt(KEY_SERVER_PORT, port);
		e.apply();
	}
	
	public String getServerIP() {
		return mPrefs.getString(KEY_SERVER_IP, "not set");
	}
	
	public int getServerPort() {
		return mPrefs.getInt(KEY_SERVER_PORT, 0);
	}

	public void setImagePixelsNum(int pixels) {
		Editor e = mPrefs.edit();
		e.putInt(KEY_IMAGE_PIXELS_NUM, pixels);
		e.apply();
	}

	public int getImagePixelsNum() {
		return mPrefs.getInt(KEY_IMAGE_PIXELS_NUM, 0);
	}

}
