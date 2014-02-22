package uk.ac.ed.insectlab.ant;

import java.util.concurrent.Semaphore;

import org.opencv.core.Mat;

import android.app.Application;
import android.graphics.Bitmap;

public class GLOBAL extends Application {
	private static final String PREFS_NAME = "roboant";
	private static Settings mSettings;

	public static Semaphore PICTURE_MUTEX = new Semaphore(0);
	public static Bitmap PICTURE_STORAGE;
	
	@Override
	public void onCreate() {
		mSettings = new Settings(this);
		super.onCreate();
	}
	
	public static synchronized Settings getSettings() {
		return mSettings;
	}
}
