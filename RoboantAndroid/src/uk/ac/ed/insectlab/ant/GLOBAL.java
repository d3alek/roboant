package uk.ac.ed.insectlab.ant;

import android.app.Application;
import android.content.SharedPreferences;

public class GLOBAL extends Application {
	private static final String PREFS_NAME = "roboant";
	private static Settings mSettings;

	@Override
	public void onCreate() {
		mSettings = new Settings(this);
		super.onCreate();
	}
	
	static Settings getSettings() {
		return mSettings;
	}
}
