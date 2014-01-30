package uk.ac.ed.insectlab.ant;

import java.text.DecimalFormat;
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

	@Override
	protected Void doInBackground(RoboAntControl... params) {
		Log.i(TAG, "doInBackground");
		mRoboAntControl = params[0];
		while (true) {

			Log.i(TAG, "IMHERE1");

			if (mFollowingRoute) {
				Log.i(TAG, "IMHERE2");
				mBestImageNum = -1;
				int counter = 0;
				while (mBestImageNum + WITHIN_BEST_TO_STOP < mRoutePictures.size()) {
					Log.i(TAG, "Following Route loop " + counter++);

					int step = -1;
					double minDist = 10000000;
					double dist;
					int minStep = -1;
					Bitmap minPic = null;
					Bitmap minPicTowards = null;

					for (int i = 0; i < 10; ++i) {
						lookAroundStep();

						if (mStop) {
							return null;
						}

						step = mLookAroundStep;

						Bitmap bmp = makeBitmap(takePicture());

						if (mStop) {
							bmp.recycle();
							return null;
						}
						//                    dist = imagesSSD(takePicture(), mCompareTo);
						//                    Log.i(TAG, "Dist " + step + " is " + dist);

						dist = 100000000;

						double thisDist;
						int picNum = 0;
						Bitmap bestPicTowards = null;
						for (Bitmap routePic: mRoutePictures) {
							//                            if (picNum < mBestImageNum) {
							//                                continue;
							//                            }
							thisDist = imagesSSD(routePic, bmp);
							if (thisDist < dist) {
								dist = thisDist;
								mBestImageNum = picNum; 
								bestPicTowards = routePic;
								Log.i(TAG, "Best pic is " + mBestImageNum);
							}
							picNum += 1;
						}

						if (dist < minDist) {
							minPicTowards = bestPicTowards;
							minDist = dist;
							minStep = step;
							minPic = bmp;
							Log.i(TAG, "New min is " + minStep + " : " + minDist);
						}
						else {
							bmp.recycle();
						}
						String publishDist = new DecimalFormat("#.##").format(dist);
						publishProgress(step+":"+publishDist+"\nMin:"+minDist+"\nMinStep:"+minStep);
					}
					mPicToPublishGoTowards  = minPicTowards;
					mPicToPublish = minPic;
					mCurrentStepToPublish = minStep;
					mGoTowardsToPublish = mBestImageNum;
					publishProgress("Going towards " + minStep + "\nDist:"+minDist);
					goTowards(minStep);

					if (mStop) {
						return null;
					}

					Log.i(TAG, "Within " + (mRoutePictures.size() - mBestImageNum) + " of end");
					mReceivedPictures.clear();
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

				double dist, minDist = 1000000000;
				int minStep = 0, step = 0;
				Bitmap minPic = null;
				mLookAroundDone = false;
				mLookAroundStep = 0;
				//        stepDirection(true);

				for (int i = 0; i < 10; ++i) {
					lookAroundStep();
					step = mLookAroundStep;

					Bitmap pic = makeBitmap(takePicture());
					dist = imagesSSD(pic, mCompareToBmp);
					Log.i(TAG, "Dist " + step + " is " + dist);
					if (dist < minDist) {
						minDist = dist;
						minStep = step;
						minPic = pic;
						Log.i(TAG, "New min is " + minStep + " : " + minDist);
					}
					mPicToPublish = pic;
					//                    else {
					//                        pic.recycle();
					//                    }
					String publishDist = new DecimalFormat("#.##").format(dist);
					publishProgress(step+":"+publishDist+"\nMin:"+minDist+"\nMinStep:"+minStep);
				}
				//                mPicToPublish = minPic;
				mPicToPublish = minPic;
				publishProgress("Going towards " + minStep + "\nDist:"+minDist);
				goTowards(minStep);

				Log.i(TAG, "Look around done");
				mReceivedPictures.clear();
			}
		}
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

	private byte[] takePicture() {
		mCameraControl.takePicture(this);

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
