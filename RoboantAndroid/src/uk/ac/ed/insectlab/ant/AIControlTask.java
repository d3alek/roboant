package uk.ac.ed.insectlab.ant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import uk.ac.ed.insectlab.ant.RoboPicture.PictureType;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AIControlTask extends AsyncTask<RoboAntControl, String, Void> implements CameraReceiver, LookAroundListener {

	private static final int TURNING_SPEED = 200;

	protected static final String TAG = "AIControlTask";

	private static final int WITHIN_BEST_TO_STOP = 1;

	private static final int TURN_STEP = 20;
	private static final int TURN_FOR = 8;

	private RoboAntControl mRoboAntControl;

	LinkedList<byte[]> mReceivedPictures = new LinkedList<byte[]>();

	Handler mHandler;

	String lock = "lock";

	private CameraControl mCameraControl;

	private byte[] mCompareTo;

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


	public AIControlTask(CameraControl camControl, NetworkControl networkControl, TextView messageView, ImageView currentStepPic, ImageView goTowardsPic, TextView currentStepNum, TextView goTowardsNum, ProgressBar progressBar) {
		this(camControl, networkControl, messageView, currentStepPic, goTowardsPic, currentStepNum, goTowardsNum, progressBar, new LinkedList<Bitmap>());
	}

	public AIControlTask(CameraControl camControl, NetworkControl networkControl, TextView messageView, ImageView currentStepPic, ImageView goTowardsPic, TextView currentStepNum, TextView goTowardsNum, ProgressBar progressBar, List<Bitmap> routePictures) {
		Log.i(TAG, "constructor");
		mCurrentStepNum = currentStepNum;
		mGoTowardsNum = goTowardsNum;
		mProgressBar = progressBar;
		mCurrentStepPic = currentStepPic;
		mGoTowardsPic = goTowardsPic;
		mCameraControl = camControl;
		mMessageView = messageView;
		mHandler = new Handler();
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


	private boolean mFinishedTurning;

	private class TurnStep {

		private Bitmap bmp;
		private int deg;

		public TurnStep(Bitmap bmp, int deg) {
			this.bmp = bmp; this.deg = deg;
		}
	}

	@Override
	protected Void doInBackground(RoboAntControl... params) {
		Log.i(TAG, "doInBackground");
		mRoboAntControl = params[0];


		while (true) {
			if (mFollowingRoute) {
				int counter = 0;

				for (int i = 0; i < mRoutePictures.size(); ++i) {
					Bitmap bmp = mRoutePictures.get(i);
					mNetworkControl.sendPicture(new RoboPicture(bmp, PictureType.GoTowards, i));
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
		mNetworkControl.sendMessage(NetworkControl.NEW_LOOK_AROUND);
		
		int dir = 1;
		
		ArrayList<TurnStep> turnsteps = new ArrayList<TurnStep>();
		
		for (int turn = 0; turn < TURN_FOR; ++turn) {
			if (turn == TURN_FOR/2) {
				dir = -1;
				mRoboAntControl.doTurn(-(turn)*TURN_STEP, this);
				waitLock();
				Bitmap bmp = makeBitmap(takePicture(turn));
				turnsteps.add(new TurnStep(bmp, turn));
				mNetworkControl.sendPicture(new RoboPicture(bmp, PictureType.LookAround, 0));
				continue;
			}
			int target;
			if (dir > 0 ) {
				target = (turn + 1)*TURN_STEP;
			}
			else {
				target = -(turn - TURN_FOR/2) * TURN_STEP;
			}
			Log.i(TAG, "Turn " + turn + " aimAngle " + TURN_STEP);

			mRoboAntControl.doTurn(dir*TURN_STEP, this);
			waitLock();
			
			Bitmap bmp = makeBitmap(takePicture(turn));
			turnsteps.add(new TurnStep(bmp, target));
			mNetworkControl.sendPicture(new RoboPicture(bmp, PictureType.LookAround, target));
		}
		
		mRoboAntControl.doTurn((TURN_FOR/2-1)*TURN_STEP, this);
		waitLock();
		return turnsteps;
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
		
		Log.i(TAG, "Turning to " + turnTo);
		
		mRoboAntControl.doTurn(turnTo, this);
		
		waitLock();

		mRoboAntControl.setSpeeds(100, 100);
		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				mRoboAntControl.setSpeeds(0, 0);
				synchronized (lock) {
					lock.notify();
				}
			}
		}, 2000);
		waitLock();
	}

	private Bitmap makeBitmap(byte[] rawJPEG) {
		Bitmap bmp =  BitmapFactory.decodeByteArray(rawJPEG, 0, rawJPEG.length, Util.getRouteFollowingBitmapOpts());
		Bitmap cropped = Util.getCroppedBitmap(bmp, mCameraControl.getCameraXRatio(),
				mCameraControl.getCameraYRatio(), mCameraControl.getRadiusRatio());
		bmp.recycle();
		return cropped;
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

	private byte[] takePicture(int pictureNum) {
		mCameraControl.takePicture(this, pictureNum);

		waitLock();
		//        return imagesSSD(mReceivedPictures.peekLast(), mCompareTo);
		return mReceivedPictures.peekLast();
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
		//        long start = System.currentTimeMillis();

		//        if (image1.length != image2.length) {
		//            throw(new RuntimeException("Images different length " + image1.length + " " + image2.length));
		//        }
		//        BitmapFactory.Options opts = new BitmapFactory.Options();
		//        opts.inSampleSize = 4;
		//
		//        Bitmap b1 = BitmapFactory.decodeByteArray(image1, 0, image1.length, opts);
		//        Bitmap b2 = BitmapFactory.decodeByteArray(image2, 0, image2.length, opts);

		double ssd = 0;
		int pixel1, pixel2, r1, r2, g1, g2, bl1, bl2;

		//        int cropW  = b1.getWidth() / 4;
		//        int cropH = b1.getHeight() / 8;
		//		for (int i = 0; i < b1.getWidth(); ++i) {
		//			for (int j = 0; j < b1.getHeight(); ++j) {
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


	public void stepTowards(final byte[] imageData) {
		Log.i(TAG, "Step towards " + imageData.length);
		//        mHandler.post(new Runnable() {

		//            @Override
		//            public void run() {
		mCompareTo = imageData;
		mCompareToBmp = makeBitmap(imageData);
		mPicToPublishGoTowards = mCompareToBmp;
		Log.i(TAG, "Notifying lock");
		//                try {
		//                    Thread.sleep(2000);
		//                } catch (InterruptedException e) {
		//                     TODO Auto-generated catch block
		//                    e.printStackTrace();
		//                }
		synchronized (lock) {
			lock.notify();
		}

		//            }
		//        });


	}

	@Override
	public void receivePicture(final byte[] picture) {
		//        mHandler.post(new Runnable() {

		//            @Override
		//            public void run() {
		Log.i(TAG, "Received picture with size " + picture.length);
		mReceivedPictures.add(picture);
		synchronized (lock) {
			lock.notify();
		}

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
//	
//	public void notifySignChanged(int sign) {
//		mRoboAntControl.setSpeeds(sign*TURN_SPEED, -sign*TURN_SPEED);
//	}

}
