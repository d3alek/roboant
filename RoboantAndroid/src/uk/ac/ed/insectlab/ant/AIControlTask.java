package uk.ac.ed.insectlab.ant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import uk.ac.ed.insectlab.ant.RoboPicture.PictureType;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AIControlTask extends AsyncTask<RoboAntControl, String, Void> implements LookAroundListener {


	protected static final String TAG = "AIControlTask";

	private static final int WITHIN_BEST_TO_STOP = 1;

	private static final int TURN_STEP = 20;
	private static final int TURN_FOR = 7;

	private static final int TURN_SPEED = 100;

	private static final boolean AWAIT_CONFIRM = true;

	double OPTIC_FLOW_TURN = 80/360.;

	private RoboAntControl mRoboAntControl;

	LinkedList<byte[]> mReceivedPictures = new LinkedList<byte[]>();

	String lock = "lock";

	private OpenCVCamera mCameraControl;

	//	private byte[] mCompareTo;

	private TextView mMessageView;

	private int mLookAroundStep;

	private boolean mLookAroundDone;

	private boolean mFollowingRoute;

	private List<Bitmap> mRoutePictures;

	private Bitmap mCompareToBmp;

	private boolean mStop;

	private ImageView mCurrentStepPic;

	private Bitmap mPicToPublish;

	private ImageView mGoTowardsPic;

	private Bitmap mPicToPublishGoTowards;

	private ProgressBar mProgressBar;

	private int mBestImageNum;

	private TextView mCurrentStepNum;

	private TextView mGoTowardsNum;

	private int mCurrentStepToPublish;

	private int mGoTowardsToPublish;

	private HashSet<Point> mPixelsToCheck;

	private NetworkControl mNetworkControl;


	public AIControlTask(OpenCVCamera camControl, NetworkControl networkControl, TextView messageView, ImageView currentStepPic, ImageView goTowardsPic, TextView currentStepNum, TextView goTowardsNum, ProgressBar progressBar) {
		this(camControl, networkControl, messageView, currentStepPic, goTowardsPic, currentStepNum, goTowardsNum, progressBar, new LinkedList<Bitmap>());
	}

	public AIControlTask(OpenCVCamera camControl, NetworkControl networkControl, TextView messageView, ImageView currentStepPic, ImageView goTowardsPic, TextView currentStepNum, TextView goTowardsNum, ProgressBar progressBar, List<Bitmap> routePictures) {
		Log.i(TAG, "constructor");
		mCurrentStepNum = currentStepNum;
		mGoTowardsNum = goTowardsNum;
		mProgressBar = progressBar;
		mCurrentStepPic = currentStepPic;
		mGoTowardsPic = goTowardsPic;
		mCameraControl = camControl;
		mMessageView = messageView;
		mRoutePictures = routePictures;
		mNetworkControl = networkControl;
		if (!mRoutePictures.isEmpty()) {
			mFollowingRoute = true;
		}
	}

	public void setNetworkControl(NetworkControl control) {
		mNetworkControl = control;
	}


	@Override
	protected void onPreExecute() {
		Log.i(TAG, "onPreExecute");
		mMessageView.setText("AI ON");
		if (mFollowingRoute) {
			mProgressBar.setVisibility(View.VISIBLE);
			mProgressBar.setMax(mRoutePictures.size());
		}
		super.onPreExecute();
	}

	private class TurnStep {

		private Bitmap bmp;
		private int deg;

		public TurnStep(Bitmap bmp, int deg) {
			this.bmp = bmp; this.deg = deg;
		}
	}

	double mTotalFlow;

	private HandlerThread mHandlerThread;

	private Handler mHandler;


	@Override
	protected Void doInBackground(RoboAntControl... params) {
		Log.i(TAG, "doInBackground");

		mHandlerThread = new HandlerThread("ai_control_handler");
		mHandlerThread.start();

		while(!mHandlerThread.isAlive()) {};  
		mHandler = new Handler(mHandlerThread.getLooper(), null);

		mRoboAntControl = params[0];

		while (true) {
			if (mFollowingRoute) {
				int counter = 0;

				for (int i = 0; i < mRoutePictures.size(); ++i) {
					Bitmap bmp = mRoutePictures.get(i);
					mNetworkControl.sendPicture(new RoboPicture(bmp, PictureType.GoTowards, i));
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				while (mBestImageNum + WITHIN_BEST_TO_STOP < mRoutePictures.size()) {
					Log.i(TAG, "Following Route loop " + counter++);
					ArrayList<TurnStep> turnsteps = lookAround();
					moveTowardsMin(turnsteps, mRoutePictures);
					Log.i(TAG, "Within " + (mRoutePictures.size() - mBestImageNum) + " of end");
				}
				Log.i(TAG, "Follow route finished");
				publishProgress("Follow route finished");
				mFollowingRoute = false;
			}
			else {
				Log.i(TAG, "Wait lock");
				waitLock();
				Log.i(TAG, "Wait done");

				if (mStop) return null;

				mNetworkControl.sendPicture(new RoboPicture(mCompareToBmp, PictureType.GoTowards, 0));

				ArrayList<TurnStep> turnsteps = lookAround();

				List<Bitmap> bitmap = new ArrayList<Bitmap>();
				bitmap.add(mCompareToBmp);

				moveTowardsMin(turnsteps, bitmap);

			}
		}
	}

	private ArrayList<TurnStep> lookAround() {
		Log.i(TAG, "Starting look around");
		mNetworkControl.sendMessage(NetworkControl.NEW_LOOK_AROUND);

		int dir = 1;

		ArrayList<TurnStep> turnsteps = new ArrayList<TurnStep>();

		for (int turn = 0; turn < TURN_FOR; ++turn) {
			if (turn == TURN_FOR/2) {
				dir = -1;
				//				doTurn(-(turn+2)*TURN_STEP);
				for (int i = 0; i < TURN_FOR/2; ++i) {
					doTurn(-TURN_STEP);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				//				doTurn(-(TURN_FOR/2)*TURN_STEP);
				//				doTurn(-TURN_STEP);
				//				try {
				//					Thread.sleep(200);
				//				} catch (InterruptedException e) {
				//					e.printStackTrace();
				//				}
				//				doTurn(-TURN_STEP);
				//				try {
				//					Thread.sleep(200);
				//				} catch (InterruptedException e) {
				//					e.printStackTrace();
				//				}
				//				doTurn(-TURN_STEP);
				//				try {
				//					Thread.sleep(1000);
				//				} catch (InterruptedException e) {
				//					e.printStackTrace();
				//				}
				Bitmap bmp = takePicture();
				turnsteps.add(new TurnStep(bmp, 0));
				//				Log.i(TAG, "FLOW IS " + mCameraControl.getOpticFlow(mCompareToBmp, bmp));
				mNetworkControl.sendPicture(new RoboPicture(bmp, PictureType.LookAround, 0));
				continue;
			}
			int target;
			if (dir > 0 ) {
				target = (turn + 1) * TURN_STEP;
			}
			else {
				target = -(turn - TURN_FOR/2) * TURN_STEP;
			}
			Log.i(TAG, "Turn " + turn + " aimAngle " + target);

			doTurn(dir*TURN_STEP);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			Bitmap bmp = takePicture();
			turnsteps.add(new TurnStep(bmp, target));
			//			Log.i(TAG, "FLOW IS " + mCameraControl.getOpticFlow(mCompareToBmp, bmp));
			mNetworkControl.sendPicture(new RoboPicture(bmp, PictureType.LookAround, target));
		}

		//		doTurn((TURN_FOR/2 +2)*TURN_STEP);
		for (int i = 0; i < TURN_FOR/2; ++i) {
			doTurn(TURN_STEP);
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//		doTurn(TURN_STEP);
		//		try {
		//			Thread.sleep(200);
		//		} catch (InterruptedException e) {
		//			e.printStackTrace();
		//		}
		//		doTurn(TURN_STEP);
		//		try {
		//			Thread.sleep(200);
		//		} catch (InterruptedException e) {
		//			e.printStackTrace();
		//		}
		return turnsteps;
	}

	private void doTurn(final int angle) {
		Log.i(TAG, "Doing turn " + angle);
		if (angle > 0) {
			mRoboAntControl.setSpeeds(TURN_SPEED, -TURN_SPEED);
		}
		else {
			mRoboAntControl.setSpeeds(-TURN_SPEED, TURN_SPEED);
		}

		mTotalFlow = 0;

		mCameraControl.setFlowListener(new OpenCVCamera.FlowListener() {
			@Override
			public boolean flowChanged(double flow) {
				mTotalFlow += flow;
				Log.i(TAG, "mTotalFlow " + mTotalFlow + " aim is "+  Math.abs(angle * OPTIC_FLOW_TURN));
				if (mTotalFlow > Math.abs(angle * OPTIC_FLOW_TURN)) {
					mRoboAntControl.setSpeeds(0, 0);
					synchronized (lock) {
						lock.notify();
					}
					return false;
				}
				return true;
			}
		});

		waitLock();

	}

	private void moveTowardsMin(ArrayList<TurnStep> turnsteps, List<Bitmap> moveTowards) {

		Log.i(TAG, "Turn pictures: " + turnsteps.size() + " moveTowards pictures " + moveTowards.size());

		int picNum;
		double minTurnDist, minRoutePicDist, dist;
		Bitmap bestPicTowards = null;
		Bitmap minTurnPic = null;
		Bitmap minRoutePic = null;
		minTurnDist = 100000000000L;
		int minStep = -1;
		int minRouteStep = -1;
		int turnNum = 0;
		for (TurnStep step: turnsteps) {
			Bitmap bmp = step.bmp;
			minRoutePicDist = 100000000000L; 
			picNum = 0;
			int currentMinRouteStep = -1;
			bestPicTowards = null;
			for (Bitmap routePic: moveTowards) {
				dist = imagesSSD(routePic, bmp);
				Log.i(TAG, "Dist is " + dist);
				if (dist < minRoutePicDist) {
					minRoutePicDist = dist;
					currentMinRouteStep = picNum;  
					bestPicTowards = routePic;
					Log.i(TAG, "Best route pic for step " + turnNum + " is " + currentMinRouteStep + " " + dist);
				}
				picNum += 1;
			}
			mNetworkControl.sendMessage(Util.newLookAroundSSDMessage(turnsteps.get(turnNum).deg, currentMinRouteStep, minRoutePicDist));
			if (minRoutePicDist < minTurnDist) {
				minTurnDist = minRoutePicDist;
				minTurnPic = bmp;
				minRoutePic = bestPicTowards;
				minStep = turnNum;
				minRouteStep = currentMinRouteStep;
			}
			mPicToPublishGoTowards  = minRoutePic;
			mPicToPublish = minTurnPic;
			mCurrentStepToPublish = minStep;
			mGoTowardsToPublish = minRouteStep;
			publishProgress("Turn " + turnNum + "Current min is " + minStep + " " + minRouteStep + " " + minTurnDist);
			Log.i(TAG, "Current min is " + minStep + " " + minRouteStep + " " + minTurnDist);
			turnNum ++;
		}

		mPicToPublishGoTowards  = minRoutePic;
		mPicToPublish = minTurnPic;
		mCurrentStepToPublish = minStep;
		mGoTowardsToPublish = minRouteStep;
		mBestImageNum = minRouteStep;

		publishProgress("Going towards " + minStep + "\nDist:"+minTurnDist);

		int turnTo = turnsteps.get(minStep).deg;
		mNetworkControl.sendMessage(NetworkControl.TURN_TO + turnTo);
		mNetworkControl.sendMessage(NetworkControl.ROUTE_MATCH + minRouteStep);

		if (AWAIT_CONFIRM) {
			waitLock();
		}

		Log.i(TAG, "Turning to " + turnTo);

		//		doTurn(turnTo);
		//		doTurnUntil(minTurnPic);
		doTurnUntilFlow(minTurnPic, turnTo < 0 ? -1 : 1);

		mRoboAntControl.setSpeeds(100, 100);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		mRoboAntControl.setSpeeds(0, 0);
	}

	private void doTurnUntilFlow(Bitmap untilBmp, int initialDir) {
		Bitmap bmp = takePicture();

		double prevFlow;
		double curFlow = mCameraControl.getOpticFlow(untilBmp, bmp);

		int dir = initialDir;

		bmp.recycle();

		double k = 30;
		int turn_speed = 100;
		int turn_time2 = 100;

		boolean method1 = false;

		while (curFlow > 10) {
			Log.i(TAG, "curFlow is " + curFlow);
			if (method1) {
				mRoboAntControl.setSpeeds(dir*turn_speed, -dir*turn_speed);
				try {
					Thread.sleep(turn_time2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mRoboAntControl.setSpeeds(0, 0);
				try {
					Thread.sleep(turn_time2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			else {
				doTurn((int)(dir*curFlow));
			}
			prevFlow = curFlow;
			bmp = takePicture();
			curFlow = mCameraControl.getOpticFlow(untilBmp, bmp);
			if (prevFlow < curFlow) {
				dir = -dir;
			}
		}

	}

	private void doTurnUntil(Bitmap bmpToMatch) {
		Bitmap bmp = takePicture();

		double prevDif = imagesSSD(bmp, bmpToMatch);
		double curDif;

		int dir = 1;

		bmp.recycle();

		doTurn(dir * TURN_STEP);
		while (true) {
			try {
				Thread.sleep(200);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			bmp = takePicture();
			curDif = imagesSSD(bmp, bmpToMatch);
			if (curDif < 10000000) {
				break;
			}
			Log.i(TAG, "curDif " + curDif + " prevDif " + prevDif);
			dir = curDif < prevDif ? dir : -dir;
			doTurn((int)(dir * TURN_STEP * (curDif/500000000.)));
			bmp.recycle();
			prevDif = curDif;

		}
	}

	private void goTowards(int minStep) {
		mRoboAntControl.doGoTowards(this, minStep);
		waitLock();
	}

	private void lookAroundStep() {
		Log.i(TAG, "Doing look around step");
		mRoboAntControl.doLookAroundStep(this);
		waitLock();
	}

	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		mMessageView.setText(values[0]);
		if (mPicToPublish != null) {
			mCurrentStepPic.setImageBitmap(mPicToPublish);
			mPicToPublish = null;
		}
		if (mPicToPublishGoTowards != null) {
			mGoTowardsPic.setImageBitmap(mPicToPublishGoTowards);
			mPicToPublishGoTowards = null;
		}
		if (mFollowingRoute) {
			mProgressBar.setProgress(mBestImageNum);
		}
		else {
			mProgressBar.setVisibility(View.GONE);
		}

		mCurrentStepNum.setText(mCurrentStepToPublish+"");
		mGoTowardsNum.setText(mGoTowardsToPublish+"");
	}

	private Bitmap mTakePictureBuffer;

	private Bitmap takePicture() {
		Log.i(TAG, "Taking picture..");
		mTakePictureBuffer = null;
		mCameraControl.getPicture(new OpenCVCamera.PictureListener() {

			@Override
			public void pictureReceived(final Bitmap picture) {
				Log.i(TAG, "pictureReceived " + picture);

				GLOBAL.PICTURE_STORAGE = picture;
				GLOBAL.PICTURE_MUTEX.release();
			}
		});

		try {
			GLOBAL.PICTURE_MUTEX.acquire();
			return GLOBAL.PICTURE_STORAGE;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;

	}

	private void waitLock() {
		synchronized (lock) {
			try {
				lock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class Point {
		int x, y;
		public Point(int x, int y) {
			this.x = x; this.y = y;
		}
	}

	private double imagesSSD(Bitmap b1, Bitmap b2) {

		if (mPixelsToCheck == null) {
			int pixel, r, g, bl, a;
			mPixelsToCheck = new HashSet<Point>();
			for (int i = 0; i < b1.getWidth(); ++i) {
				for (int j = 0; j < b1.getHeight(); ++j) {
					pixel = b1.getPixel(i, j);
					r = Color.red(pixel);
					g = Color.green(pixel);
					bl = Color.blue(pixel);
					a = Color.alpha(pixel);
					if (a == 0 || (r == 0 && g == 0 && bl == 0)) {
					}
					else {
						mPixelsToCheck.add(new Point(i, j));
					}
				}
			}
			Log.i(TAG, "mPixelsToCheck size " + mPixelsToCheck.size() + " image size " + b1.getHeight() * b1.getWidth());
		}

		double ssd = 0;
		int pixel1, pixel2, r1, r2, g1, g2, bl1, bl2;

		Log.i(TAG, "mPixelsToCheck size " + mPixelsToCheck.size() + " " + b1.getByteCount() + " " + b2.getByteCount());
		for (Point p: mPixelsToCheck) {
			pixel1 = b1.getPixel(p.x, p.y);
			pixel2 = b2.getPixel(p.x, p.y);
			r1 = Color.red(pixel1);
			r2 = Color.red(pixel2);
			g1 = Color.green(pixel1);
			g2 = Color.green(pixel2);
			bl1 = Color.blue(pixel1);
			bl2 = Color.blue(pixel2);


			ssd += (r1 - r2) * (r1 - r2) +
					(g1 - g2) * (g1 - g2) +
					(bl1 - bl2) * (bl1 - bl2);


		}
		//			}
		//		}

		//        Log.i(TAG, "imagesSSD runtime " + (System.currentTimeMillis() - start));

		return ssd;
	}

	public Handler getHandler() {
		return mHandler;
	}


	public void stepTowards() {
		Log.i(TAG, "Step towards");
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.i(TAG, "IMHERE HEHE");
				Log.i(TAG, Thread.currentThread().getName());
				mCompareToBmp = takePicture();
				mPicToPublishGoTowards = mCompareToBmp;
				//		Log.i(TAG, "Notifying lock");
				//                try {
				//                    Thread.sleep(2000);
				//                } catch (InterruptedException e) {
				//                     TODO Auto-generated catch block
				//                    e.printStackTrace();
				//                }
				synchronized (lock) {
					lock.notify();
				}
			}
		});

		//            }
		//        });


	}

	@Override
	public void lookAroundDone() {
		mLookAroundDone = true;
		synchronized (lock) {
			lock.notify();
		}
	}

	@Override
	public void lookAroundStepDone(int step) {
		mLookAroundStep = step;
		synchronized (lock) {
			lock.notify();
		}
	}

	@Override
	public void goTowardsDone() {
		synchronized (lock) {
			lock.notify();
		}
	}

	public void followRoute(LinkedList<Bitmap> routePictures) {
		Log.i(TAG, "Follow route started!");
		mFollowingRoute = true;
		mRoutePictures = routePictures;
		synchronized (lock) {
			lock.notify();
		}
	}

	public void stop() {
		mStop = true;
		synchronized (lock) {
			lock.notify();
		}
	}

	public void notifyTargetReached() {
		synchronized (lock) {
			lock.notify();
		}
	}

	public void releaseLock() {
		synchronized (lock) {
			lock.notify();
		}
	}

}
