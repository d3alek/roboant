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
}
