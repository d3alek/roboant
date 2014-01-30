package uk.ac.ed.insectlab.ant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.AsyncTask;
import android.util.Log;

public class SaveImageTask extends AsyncTask<byte[], Void, Void> {

	private static final String TAG = SaveImageTask.class.getSimpleName();

	@Override
	protected Void doInBackground(byte[]... params) {
		File file = Util.getOutputMediaFile(Constants.MEDIA_TYPE_IMAGE);
		if (file == null){
			Log.d(TAG, "Error creating media file, check storage permissions");
			return null;
		}

		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(params[0]);
			fos.close();
		} catch (FileNotFoundException e) {
			Log.d(TAG, "File not found: " + e.getMessage());
		} catch (IOException e) {
			Log.d(TAG, "Error accessing file: " + e.getMessage());
		}
		return null;
	}

}
