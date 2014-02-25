package uk.ac.ed.insectlab.ant;

import java.util.List;
import java.util.concurrent.Semaphore;

import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class SwayingHomingFragment extends Fragment {

	protected static final String TAG = SwayingHomingFragment.class.getSimpleName();
	private ImageView mArrow;
	private NetworkControl mNetwork;
	private ArduinoZumoControl mRoboant;
	private CameraFragment mCamera;
	private List<Bitmap> mRoute;

	private Handler mHandler = new Handler();
	private static AIControlTask mAIControlTask;
	private boolean mSerialSet;
	private boolean mCameraSet;
	//	private View mView;

	interface NavigationListener {

		void onNavigationStarted();

		void onNavigationStopped();

	}

	private NavigationListener mListener;
	private View mView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mRoute = GLOBAL.ROUTE;
	}

	@Override
	public void onAttach(Activity activity) {
		try {
			mListener = (NavigationListener)activity;
		} catch (ClassCastException e) {
			new RuntimeException("Parent activity does not implement listener");
		}
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_swaying_homing, container, false);

		mArrow = (ImageView) v.findViewById(R.id.arrow);

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		mView = getView();
		mView.setVisibility(View.INVISIBLE);
	}

	public void setNetwork(NetworkControl network) {
		mNetwork = network;
		if (mAIControlTask != null) {
			mAIControlTask.setNetworkControl(network);
		}
	}

	public void setSerial(ArduinoZumoControl roboant) {
		mRoboant = roboant;
		mSerialSet = true;
	}

	public void setCamera(CameraFragment camera) {
		mCamera = camera;
		mCameraSet = true;
	}

	private double mCalibrateSSDMin;

	private double mCalibrateSSDMAX;

	private Bitmap mCalibrateBmp;

	private long mLastTimeChanged;


	private boolean mSSDCalibrated;

	private double mSSDMin;

	private double mSSDMax;

	public void calibrateSSD() {
		if (mAIControlTask != null) {
			mAIControlTask.stop();
			mAIControlTask = null;
		}
		mListener.onNavigationStarted();
		mRoboant.setSpeeds(-100, 100);
		mCalibrateSSDMin = Double.MAX_VALUE;
		mCalibrateSSDMAX = Double.MIN_VALUE;
		mCalibrateBmp = mCamera.getPicture();

		mSSDCalibrated = false;

		mLastTimeChanged = System.currentTimeMillis();

		mHandler.postDelayed(new Runnable() {


			@Override
			public void run() {
				Bitmap bmp = mCamera.getPicture();
				boolean changed = false;

				double ssd = Util.imagesSSD(bmp, mCalibrateBmp);
				if (ssd < mCalibrateSSDMin) {
					mCalibrateSSDMin = ssd;
					changed = true;
				}
				if (ssd > mCalibrateSSDMAX) {
					mCalibrateSSDMAX = ssd;
					changed = true;
				}

				if (changed) {
					mLastTimeChanged = System.currentTimeMillis();
					Log.i(TAG, "Changed");
				}
				else {
					if (System.currentTimeMillis() - mLastTimeChanged > 4000) {
						Log.i(TAG, "Changed not");
						// do not post this callback again
						GLOBAL.getSettings().setSSDCalibrationResults(true, mCalibrateSSDMin, mCalibrateSSDMAX);
						mSSDCalibrated = true;
						mSSDMin = mCalibrateSSDMin;
						mSSDMax = mCalibrateSSDMAX;
						mRoboant.setSpeeds(0, 0);
						mListener.onNavigationStopped();
						return;
					}
				}
				mHandler.postDelayed(this, 100);

			}
		}, 100);

	}

	private class AIControlTask extends AsyncTask<ArduinoZumoControl, Float, Void> {
		protected static final String TAG = "AIControlTask";

		private ArduinoZumoControl mRoboAntControl;

		String lock = "lock";

		private List<Bitmap> mRoutePictures;

		private boolean mStop = false;

		private NetworkControl mNetworkControl;


		private Semaphore mMinCalcMutex;

		private CameraFragment mCameraControl;

		public AIControlTask(CameraFragment camControl, NetworkControl networkControl, List<Bitmap> routePictures) {
			mCameraControl = camControl;
			mRoutePictures = routePictures;
			mNetworkControl = networkControl;
			mMinCalcMutex = new Semaphore(0);
			mSSDCalibrated = GLOBAL.getSettings().getSSDCalibrated();
		}

		public void setNetworkControl(NetworkControl control) {
			mNetworkControl = control;
		}


		@Override
		protected void onPreExecute() {
			Log.i(TAG, "onPreExecute");
			mListener.onNavigationStarted();
			mView.setVisibility(View.VISIBLE);
			mView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			getActivity().getActionBar().hide();
			mView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					stop();
				}
			});
			Log.i(TAG, "after onPreExecute");
			super.onPreExecute();
		}


		@Override
		protected Void doInBackground(ArduinoZumoControl... params) {
			Log.i(TAG, "doInBackground");
			if (mSSDCalibrated) {
				mSSDMin = GLOBAL.getSettings().getSSDMin();
				mSSDMax = GLOBAL.getSettings().getSSDMax();
				Log.i(TAG, "mSSD " + mSSDMin + " " + mSSDMax);
			}

			mRoboAntControl = params[0];

			while (true) {
				if (mStop) {
					return null;
				}
				swayingHoming(mRoutePictures);
			}
		}

		private void swayingHoming(List<Bitmap> routePics) {
			int dir = 1; //right
			double minDist;
			double thisDist;
			int rotateSpeed;

			int speedAdj = 200;

			Bitmap thisPicture;

			while (true) {
				if (mStop) {
					break;
				}

				thisPicture = takePicture();
				minDist = Double.MAX_VALUE;

				for (int i = 0; i < routePics.size(); ++i) {
					thisDist = Util.imagesSSD(routePics.get(i), thisPicture, mSSDMin, mSSDMax);
					if (thisDist < minDist) {
						minDist = thisDist;
						Log.i(TAG, "loop min is " + minDist + " at " + i);
					}
				}
				Log.i(TAG, "final min is " + minDist);

				rotateSpeed = (int)(speedAdj * minDist);

				Log.i(TAG, "rotateSpeed is " + rotateSpeed);

				publishProgress((float)(dir*rotateSpeed));

				if (rotateSpeed > 40) {
					mRoboAntControl.simpleTurnInPlaceBlocking(dir*rotateSpeed, 400);
				}

				dir = -dir;

				moveForward(80, 200);
			}
			mRoboAntControl.setSpeeds(0, 0);
		}

		private void moveForward(int speed, int time) {
			mRoboAntControl.setSpeeds(speed, speed);
			try {
				Thread.sleep(time);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mRoboAntControl.setSpeeds(0, 0);
		}

		@Override
		protected void onPostExecute(Void result) {
			Log.i(TAG, "onPostExecute");
			//			mView.setVisibility(View.INVISIBLE);
			getActivity().getActionBar().show();
			mView.setVisibility(View.INVISIBLE);
			mListener.onNavigationStopped();
			super.onPostExecute(result);
		}


		@Override
		protected void onProgressUpdate(Float... angle) {
			super.onProgressUpdate(angle);

			Matrix matrix=new Matrix();

			mArrow.setScaleType(ScaleType.MATRIX);
			matrix.postRotate(angle[0], mArrow.getWidth()/2, mArrow.getHeight()/2);
			mArrow.setImageMatrix(matrix);
		}

		private Bitmap takePicture() {
			Bitmap pic = mCameraControl.getPicture();
			while (pic == null) {
				Log.i(TAG, "Waiting for camera to return non null picture");
				pic = mCameraControl.getPicture();
			}
			return pic;
		}

		public void stop() {
			mStop = true;
			synchronized (lock) {
				lock.notify();
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.swaying_homing_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.calibrate:

			calibrateSSD();
			return true;
		case R.id.navigate:
			mAIControlTask = new AIControlTask(mCamera, mNetwork, mRoute);
			mAIControlTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mRoboant);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPause() {
		if (mAIControlTask != null) {
			mAIControlTask.stop();
			mAIControlTask = null;
		}
		super.onPause();
	}

	public void toggleNavigation() {
		if (mAIControlTask != null) {
			mAIControlTask.stop();
			mAIControlTask = null;
		}
		else {
			mAIControlTask = new AIControlTask(mCamera, mNetwork, mRoute);
			mAIControlTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mRoboant);
		}
	}




}
