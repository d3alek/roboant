package uk.ac.ed.insectlab.ant;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AsyncMinSSDTask extends AsyncTask<Bitmap, Void, Double> {

	protected static final int MSG_MIN = 10;
	protected static final int SPIN_OFF_THREADS = 4;
	private static final String TAG = AsyncMinSSDTask.class.getSimpleName();
	private Handler mToNotify;
	//	private List<Bitmap> mAllPics;
	private OneAsyncMinSSDTask[] mThreads = new OneAsyncMinSSDTask[SPIN_OFF_THREADS];
	double[] mThreadsResults = new double[SPIN_OFF_THREADS];
	final CountDownLatch latch = new CountDownLatch(SPIN_OFF_THREADS);

	public AsyncMinSSDTask(final List<Bitmap> pics, Handler toNotify) {
		mToNotify = toNotify;
		//		mAllPics = pics;
		int eachThreadGets = pics.size() / SPIN_OFF_THREADS;
		int from, to;
		for (int i = 0; i < SPIN_OFF_THREADS - 1; ++i) {
			from = i*eachThreadGets;
			to = from + eachThreadGets;
			mThreads[i] = new OneAsyncMinSSDTask(i, pics.subList(from, to));
			mThreadsResults[i] = Double.MAX_VALUE;
		}
		// last one gets more work
		from = (SPIN_OFF_THREADS - 1)*eachThreadGets;
		to = from + eachThreadGets + pics.size() % SPIN_OFF_THREADS;
		
//		Log.i(TAG, "IMHERE " + pics.size() + " " + eachThreadGets + " " + from + " " + to);
		
		mThreads[SPIN_OFF_THREADS - 1] = new OneAsyncMinSSDTask(SPIN_OFF_THREADS - 1, pics.subList(from, to));
	}

	@Override
	protected Double doInBackground(Bitmap... params) {
		for (int i = 0; i < SPIN_OFF_THREADS; ++i) {
			mThreads[i].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		}
		try {
			Log.i(TAG, "Spun off threads, waiting for latch");
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		double min = Double.MAX_VALUE;
		for (int i = 0; i < SPIN_OFF_THREADS; ++i) {
			Log.i(TAG, "Thread " + i + " has result " + mThreadsResults[i]);
			if (mThreadsResults[i] < min) {
				min = mThreadsResults[i];
			}
		}
		
		return min;
	}

	@Override
	protected void onPostExecute(Double result) {
		super.onPostExecute(result);
		Message toSend = mToNotify.obtainMessage(MSG_MIN);
		toSend.obj = result;
		mToNotify.sendMessage(toSend);
		Log.i(TAG, "Sent message " + result + " to creator");
	}


	private class OneAsyncMinSSDTask extends AsyncTask<Bitmap, Void, Double> {

		private final List<Bitmap> mPics;
		private int mId;
		private double mMinSSD;
		private double mMaxSSD;

		public OneAsyncMinSSDTask(int id, final List<Bitmap> sublist) {
			mPics = sublist;
			mId = id;
			mMinSSD = GLOBAL.getSettings().getSSDMin();
			mMaxSSD = GLOBAL.getSettings().getSSDMax();
		}

		@Override
		protected Double doInBackground(Bitmap... params) {
			Bitmap compareTo = params[0];
			double minSSD = Double.MAX_VALUE;
			double thisSSD;
			for (Bitmap bmp : mPics) {
				thisSSD = Util.imagesSSD(compareTo, bmp, mMinSSD, mMaxSSD);
				if (thisSSD < minSSD) {
					minSSD = thisSSD;
				}
			}

			return minSSD;
		}
		
		@Override
		protected void onPostExecute(Double result) {
			super.onPostExecute(result);
			mThreadsResults[mId] = result;
			Log.i(TAG, "Thread " + mId + " ready! " + result + " " + mThreadsResults[mId]);
			latch.countDown();
		}

	}
}
