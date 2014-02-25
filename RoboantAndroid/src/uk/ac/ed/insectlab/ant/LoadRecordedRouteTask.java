package uk.ac.ed.insectlab.ant;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import uk.ac.ed.insectlab.ant.RouteSelectionDialogFragment.RouteSelectedListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

public class LoadRecordedRouteTask extends AsyncTask<String, Void, List<Bitmap>> {

	private static final String TAG = LoadRecordedRouteTask.class.getSimpleName();
	private RouteSelectedListener mListener;
	private String mDirPath;

	public LoadRecordedRouteTask(RouteSelectedListener listener) {
		mListener = listener;
	}

	@Override
	protected List<Bitmap> doInBackground(String... params) {
		mDirPath = params[0];

		File dir = new File(mDirPath);

		LinkedList<Bitmap> list = new LinkedList<Bitmap>();

		for (File file: dir.listFiles()) {
			Log.i(TAG, "Loading " + file.getPath() + " into bitmap");
			list.add(BitmapFactory.decodeFile(file.getPath()));
		}

		return list;
	}

	@Override
	protected void onPostExecute(List<Bitmap> result) {
		mListener.onRouteSelected(result);
		GLOBAL.getSettings().setMostRecentRouteDirPath(mDirPath);
		super.onPostExecute(result);
	}

}