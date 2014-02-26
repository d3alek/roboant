package uk.ac.ed.insectlab.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

public class SaveRecordedRouteTask extends AsyncTask<List<Bitmap>, Void, Void> {

	private static final String TAG = SaveRecordedRouteTask.class.getSimpleName();
	private Context mContext;

	public SaveRecordedRouteTask(Context context) {
		mContext = context;
	}

	@Override
	protected Void doInBackground(List<Bitmap>... params) {
		Log.i(TAG, "Starting doInBackground");
		File dir = Util.getNewRouteStorageDir(mContext);
		GLOBAL.getSettings().setMostRecentRouteDirPath(dir.getAbsolutePath());
		int num = 0;
		for (Bitmap bitmap: params[0]) {
			File picture = Util.getRoutePictureFile(dir, num++);
			try {
				FileOutputStream out = new FileOutputStream(picture);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

}