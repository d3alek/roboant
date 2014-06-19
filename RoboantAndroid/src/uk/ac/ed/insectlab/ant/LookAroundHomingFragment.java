package uk.ac.ed.insectlab.ant;

import java.util.List;
import java.util.concurrent.Semaphore;

import uk.ac.ed.insectlab.ant.SwayingHomingFragment.NavigationListener;
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
import android.widget.TextView;

public class LookAroundHomingFragment extends Fragment {

	protected static final String TAG = LookAroundHomingFragment.class.getSimpleName();
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

	private volatile NavigationListener mListener;
	private View mView;
	private List<Point> mLensPixels;
	private TextView mText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mRoute = GLOBAL.ROUTE;
	}

	@Override
	public void onAttach(Activity activity) {
		Log.i(TAG, "onAttach");
		try {
			mListener = (NavigationListener)activity;
			Log.i(TAG, "mListener added " + mListener);
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
		mText = (TextView) v.findViewById(R.id.text);

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

		if (mLensPixels == null) {
			while (mCamera.getPicture() == null) {
				Log.i(TAG, "Waiting for camera");
			}
			mLensPixels = Util.getLensPixels(mCamera.getPicture());
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

				double ssd = Util.imagesSSD(bmp, mCalibrateBmp, mLensPixels);
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

	private class AIControlTask extends AsyncTask<ArduinoZumoControl, Integer, Void> {
		protected static final String TAG = "AIControlTask";

		private static final int TRAINING_START = 555;

		private static final int TRAINING_END = 6666;

		private ArduinoZumoControl mRoboAntControl;

		String lock = "lock";

		private List<Bitmap> mRoutePictures;

		private boolean mStop = false;

		private NetworkControl mNetworkControl;


		private Semaphore mMinCalcMutex;

		private CameraFragment mCameraControl;

		private WillshawNetwork mWN;

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
			Log.i(TAG, "onPreExecute " + mListener);

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

			if (mLensPixels == null) {
				while (mCamera.getPicture() == null) {
					Log.i(TAG, "Waiting for camera");
				}
				mLensPixels = Util.getLensPixels(mCamera.getPicture());
			}

			if (mSSDCalibrated) {
				mSSDMin = GLOBAL.getSettings().getSSDMin();
				mSSDMax = GLOBAL.getSettings().getSSDMax();
				Log.i(TAG, "mSSD " + mSSDMin + " " + mSSDMax);
			}

			mRoboAntControl = params[0];

			publishProgress(TRAINING_START);
			mWN = new WillshawNetwork(mRoutePictures, mLensPixels);
			publishProgress(TRAINING_END);

			while (true) {
				if (mStop) {
					return null;
				}
				lookAroundHoming(mWN);
			}
		}

		private void lookAroundHoming(WillshawNetwork wn) {
			int turnSpeed = 100, turnTime = 100;
			int maxSteps = 5;
			Bitmap bmp;
			int familiarity;
			while (true) {
				if (mStop) {
					break;
				}
				bmp = takePicture();
				familiarity = wn.process(bmp);
				publishProgress(0, familiarity);
				bmp.recycle();
				for (int step = 0; step < maxSteps; ++step) {
					mRoboant.turnInPlaceBlocking(-turnSpeed, turnTime);
					bmp = takePicture();
					familiarity = wn.process(bmp);
					publishProgress(-step, familiarity);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					bmp.recycle();
				}
				mRoboant.turnInPlaceBlocking(turnSpeed, turnTime * maxSteps);
				for (int step = 0; step < maxSteps; ++step) {
					mRoboant.turnInPlaceBlocking(turnSpeed, turnTime);
					bmp = takePicture();
					familiarity = wn.process(bmp);
					publishProgress(step, familiarity);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					bmp.recycle();
				}
				mRoboant.turnInPlaceBlocking(-turnSpeed, turnTime * maxSteps);
			}
		}

		//		private void swayingHoming(List<Bitmap> routePics) {
		//			int dir = 1; //right
		//			double minDist;
		//			double thisDist;
		//			double prevDist = 0;
		//			int rotateSpeed;
		//
		//			int speedAdj = 300;
		//			double speedAdjGradient = 0.0005;
		//
		//			Bitmap thisPicture;
		//			
		//			boolean GRADIENT = true;
		//
		//			while (true) {
		//				if (mStop) {
		//					break;
		//				}
		//
		//				thisPicture = takePicture();
		//				minDist = Double.MAX_VALUE;
		//
		//				for (int i = 0; i < routePics.size(); ++i) {
		//					if (GRADIENT) {
		//						thisDist = Util.imagesSSD(routePics.get(i), thisPicture, mLensPixels);
		//					}
		//					else {
		//						thisDist = Util.imagesSSD(routePics.get(i), thisPicture, mSSDMin, mSSDMax, mLensPixels);
		//					}
		//					if (thisDist < minDist) {
		//						minDist = thisDist;
		//					}
		//				}
		//				
		//				if (GRADIENT) {
		//					if (prevDist != 0) {
		//						rotateSpeed = (int)(speedAdjGradient * (minDist - prevDist));
		//						Log.i(TAG, "rotateSpeed " + rotateSpeed);
		//					}
		//					else {
		//						rotateSpeed = 0;
		//					}
		//					prevDist = minDist;
		//				}
		//				else {
		//					rotateSpeed = (int)(speedAdj * minDist);
		//				}
		//
		//				publishProgress((float)(dir*rotateSpeed));
		//
		//				if (Math.abs(rotateSpeed) > 40) {
		//					mRoboAntControl.turnInPlaceBlocking(dir*rotateSpeed, 300);
		//				}
		//
		////				dir = -dir;
		//
		//				moveForward(80, 200);
		//			}
		//			mRoboAntControl.setSpeeds(0, 0);
		//		}

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
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);

			if (progress[0] == TRAINING_START) {
				mText.setText("TRAINING STARTED");
				return;
			}
			if (progress[0] == TRAINING_END) {
				mText.setText("TRAINING FINISHED");
				return;
			}

			Matrix matrix=new Matrix();

			int step = progress[0];
			int familiarity = progress[1];

			mArrow.setScaleType(ScaleType.MATRIX);
			matrix.postRotate(step*10, mArrow.getWidth()/2, mArrow.getHeight()/2);
			mArrow.setImageMatrix(matrix);

			mText.setText(familiarity+"");
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
