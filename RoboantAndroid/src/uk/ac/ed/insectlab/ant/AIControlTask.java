package uk.ac.ed.insectlab.ant;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

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


	public AIControlTask(CameraControl camControl, TextView messageView, ImageView currentStepPic, ImageView goTowardsPic, TextView currentStepNum, TextView goTowardsNum, ProgressBar progressBar) {
		this(camControl, messageView, currentStepPic, goTowardsPic, currentStepNum, goTowardsNum, progressBar, new LinkedList<Bitmap>());
	}

	public AIControlTask(CameraControl camControl, TextView messageView, ImageView currentStepPic, ImageView goTowardsPic, TextView currentStepNum, TextView goTowardsNum, ProgressBar progressBar, List<Bitmap> routePictures) {
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
		if (!mRoutePictures.isEmpty()) {
			mFollowingRoute = true;
		}
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


	int TURN_SPEED = 80;
	int TURN_FOR = 1500;
	private boolean mFinishedTurning;

	private class TurnStep {

		private Bitmap bmp;
		private long time;

		public TurnStep(Bitmap bmp, long time) {
			this.bmp = bmp; this.time = time;
		}
	}

	@Override
	protected Void doInBackground(RoboAntControl... params) {
		Log.i(TAG, "doInBackground");
		mRoboAntControl = params[0];


		while (true) {
			if (mFollowingRoute) {
				mBestImageNum = -1;
				int counter = 0;
				while (mBestImageNum + WITHIN_BEST_TO_STOP < mRoutePictures.size()) {
					Log.i(TAG, "Following Route loop " + counter++);

					mRoboAntControl.setSpeeds(TURN_SPEED, -TURN_SPEED);


					mFinishedTurning = false;
					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							mRoboAntControl.setSpeeds(-TURN_SPEED, TURN_SPEED);

							mHandler.postDelayed(new Runnable() {


								@Override
								public void run() {
									mRoboAntControl.setSpeeds(0, 0);
									mFinishedTurning = true;
								}
							}, 2*TURN_FOR);

						}
					}, TURN_FOR);

					ArrayList<TurnStep> turnsteps = new ArrayList<TurnStep>();
					long startTime = System.currentTimeMillis();
					int num = 0;
					while (!mFinishedTurning) {
						long time = System.currentTimeMillis();
						Bitmap bmp = makeBitmap(takePicture(num++));
						turnsteps.add(new TurnStep(bmp, time - startTime));
					}
					long endTime = System.currentTimeMillis();
					
					Log.i(TAG, "Runnning for " + (endTime - startTime));
					
					moveTowardsMin(turnsteps, mRoutePictures, endTime - startTime);
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

				mRoboAntControl.setSpeeds(TURN_SPEED, -TURN_SPEED);


				mFinishedTurning = false;
				mHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						mRoboAntControl.setSpeeds(-TURN_SPEED, TURN_SPEED);

						mHandler.postDelayed(new Runnable() {


							@Override
							public void run() {
								mRoboAntControl.setSpeeds(0, 0);
								mFinishedTurning = true;
							}
						}, 2*TURN_FOR);

					}
				}, TURN_FOR);

				ArrayList<TurnStep> turnsteps = new ArrayList<TurnStep>();
				long startTime = System.currentTimeMillis();
				int num = 0;
				while (!mFinishedTurning) {
					long time = System.currentTimeMillis();
					Bitmap bmp = makeBitmap(takePicture(num++));
					turnsteps.add(new TurnStep(bmp, time - startTime));
				}
				
				List<Bitmap> bitmap = new ArrayList<Bitmap>();
				bitmap.add(mCompareToBmp);
				long endTime = System.currentTimeMillis();
				
				Log.i(TAG, "Runnning for " + (endTime - startTime));
				
				moveTowardsMin(turnsteps, bitmap, endTime - startTime);
			}
		}
	}
	
	private void moveTowardsMin(ArrayList<TurnStep> turnsteps, List<Bitmap> moveTowards, long ranFor) {
		
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

		mRoboAntControl.setSpeeds(TURN_SPEED, -TURN_SPEED);

		long minTime = turnsteps.get(mCurrentStepToPublish).time;
		
		long oneDirectionTurnFor = ranFor / 2;

		long turnFor = minTime < oneDirectionTurnFor ? oneDirectionTurnFor + minTime : 2 * oneDirectionTurnFor - minTime;

		Log.i(TAG, "minTime is " + minTime + " turnFor " + turnFor);

		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				mRoboAntControl.setSpeeds(0, 0);
				synchronized (lock) {
					lock.notify();
				}
			}
		}, turnFor);
		waitLock();
		Log.i(TAG, "Facing the correct direction!");

		mRoboAntControl.setSpeeds(TURN_SPEED, TURN_SPEED);
		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				mRoboAntControl.setSpeeds(0, 0);
				synchronized (lock) {
					lock.notify();
				}
			}
		}, TURN_FOR);
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

}
