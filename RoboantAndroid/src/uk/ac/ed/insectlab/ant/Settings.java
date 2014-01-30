package uk.ac.ed.insectlab.ant;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {

	private static final String PREFS_NAME = "roboant";
	private SharedPreferences mPrefs;

	public Settings(Context context) {
		mPrefs = context.getSharedPreferences(PREFS_NAME, 0);
	}

	public SharedPreferences getSharedPrefs() {
		return mPrefs;
	}
}
