package uk.ac.ed.insectlab.ant;

import org.opencv.core.Point3;

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
	private SharedPreferences mPrefs;

	public Settings(Context context) {
		mPrefs = context.getSharedPreferences(PREFS_NAME, 0);
	}

	public SharedPreferences getSharedPrefs() {
		return mPrefs;
	}

	public Point3 loadLens() {
		if (mPrefs.getFloat(KEY_LENS_RAD, -1) != -1) {
			Point3 lens = new Point3(
					(double)mPrefs.getFloat(KEY_LENS_X, 0),
					(double)mPrefs.getFloat(KEY_LENS_Y, 0),
					(double)mPrefs.getFloat(KEY_LENS_RAD, 0));
			return lens;
		}
		return null;
	}

	public void saveLens(Point3 lens) {
		Editor e = mPrefs.edit();
		e.putFloat(KEY_LENS_X, (float)lens.x);
		e.putFloat(KEY_LENS_Y, (float)lens.y);
		e.putFloat(KEY_LENS_RAD, (float)lens.z);
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
}
