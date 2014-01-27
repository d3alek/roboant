package uk.ac.ed.insectlab.ant;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class CameraOverlay extends SurfaceView implements SurfaceHolder.Callback{

    class CameraThread extends Thread {
        public static final int STATE_LOSE = 1;
        public static final int STATE_PAUSE = 2;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;
        public static final int STATE_WIN = 5;
        
        private SurfaceHolder mSurfaceHolder;
        private Handler mHandler;
        private Context mContext;
        private ShapeDrawable mPanoramicCircle;
        private Paint mGreenPaint;
        private int mCanvasWidth = 0;
        private int mCanvasHeight = 0;
        private long mLastTime;
        
        /** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
        private int mMode;
        private boolean mRun = false;

        private final Object mRunLock = new Object();
		private Paint mOrangePaint;

        public CameraThread(SurfaceHolder surfaceHolder, Context context,
                Handler handler) {
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;

            Resources res = context.getResources();
            // cache handles to our key sprites & other drawables
//            mLanderImage = context.getResources().getDrawable(
//                    R.drawable.lander_plain);
//            mFiringImage = context.getResources().getDrawable(
//                    R.drawable.lander_firing);
//            mCrashedImage = context.getResources().getDrawable(
//                    R.drawable.lander_crashed);

            // Initialize paints for speedometer
            mGreenPaint = new Paint();
            mGreenPaint.setAntiAlias(true);
            mGreenPaint.setARGB(100, 0, 255, 0);
            mOrangePaint = new Paint();
            mOrangePaint.setAntiAlias(true);
            mOrangePaint.setARGB(50, 255, 165, 0);

//            mScratchRect = new RectF(0, 0, 0, 0);

            mPanoramicCircle = new ShapeDrawable(new OvalShape());
//            OvalShape oval = new OvalShape();
//            oval.
//            mPanoramicCircle = ShapeDrawable.create
            mPanoramicCircle.setIntrinsicHeight(200);
            mPanoramicCircle.setIntrinsicWidth(200);
//            mPanoramicCircle.

        }

        /**
         * Starts the game, setting parameters for the current difficulty.
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {
                setState(STATE_RUNNING);
            }
        }

        /**
         * Pauses the physics update & animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING) setState(STATE_PAUSE);
            }
        }

        /**
         * Restores game state from the indicated Bundle. Typically called when
         * the Activity is being restored after having been previously
         * destroyed.
         *
         * @param savedState Bundle containing the game state
         */
        public synchronized void restoreState(Bundle savedState) {
            synchronized (mSurfaceHolder) {
                setState(STATE_PAUSE);
            }
        }

        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
//                        if (mMode == STATE_RUNNING) matchCircle();
                        // Critical section. Do not allow mRun to be set false until
                        // we are sure all canvas draw operations are complete.
                        //
                        // If mRun has been toggled false, inhibit canvas operations.
                        synchronized (mRunLock) {
                            if (mRun) doDraw(c);
                        }
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        /**
         * Dump game state to the provided Bundle. Typically called when the
         * Activity is being suspended.
         *
         * @return Bundle with this view's state
         */
        public Bundle saveState(Bundle map) {
            synchronized (mSurfaceHolder) {
                if (map != null) {
//                    map.putInt(KEY_DIFFICULTY, Integer.valueOf(mDifficulty));
                    
                }
            }
            return map;
        }

        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         *
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            // Do not allow mRun to be modified while any canvas operations
            // are potentially in-flight. See doDraw().
            synchronized (mRunLock) {
                mRun = b;
            }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         *
         * @see #setState(int, CharSequence)
         * @param mode one of the STATE_* constants
         */
        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
                setState(mode, null);
            }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         *
         * @param mode one of the STATE_* constants
         * @param message string to add to screen or null
         */
        public void setState(int mode, CharSequence message) {
            /*
             * This method optionally can cause a text message to be displayed
             * to the user when the mode changes. Since the View that actually
             * renders that text is part of the main View hierarchy and not
             * owned by this thread, we can't touch the state of that View.
             * Instead we use a Message + Handler to relay commands to the main
             * thread, which updates the user-text View.
             */
            synchronized (mSurfaceHolder) {
                mMode = mode;

                if (mMode == STATE_RUNNING) {
                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", "");
                    b.putInt("viz", View.INVISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                } else {
//                    mRotating = 0;
//                    mEngineFiring = false;
                    Resources res = mContext.getResources();
                    CharSequence str = "";
//                    if (mMode == STATE_READY)
//                        str = res.getText(R.string.mode_ready);
//                    else if (mMode == STATE_PAUSE)
//                        str = res.getText(R.string.mode_pause);
//                    else if (mMode == STATE_LOSE)
//                        str = res.getText(R.string.mode_lose);
//                    else if (mMode == STATE_WIN)
//                        str = res.getString(R.string.mode_win_prefix)
//                        + mWinsInARow + " "
//                        + res.getString(R.string.mode_win_suffix);

                    if (message != null) {
                        str = message + "\n" + str;
                    }

//                    if (mMode == STATE_LOSE) mWinsInARow = 0;

                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", str.toString());
                    b.putInt("viz", View.VISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
            }
        }

        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;
            }
        }

        /**
         * Resumes from a pause.
         */
        public void unpause() {
            // Move the real time clock up to now
            synchronized (mSurfaceHolder) {
                mLastTime = System.currentTimeMillis() + 100;
            }
            setState(STATE_RUNNING);
        }

        /**
         * Draws the ship, fuel/speed bars, and background to the provided
         * Canvas.
         */
        private void doDraw(Canvas canvas) {
        	canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        	if (mTouchDown) {
	            float left = mCameraX - mTouchRadius;
	            float right = mCameraX + mTouchRadius;
	            float top = mCameraY - mTouchRadius;
	            float bottom = mCameraY + mTouchRadius;
	            canvas.drawOval(new RectF(left, top, right, bottom), mGreenPaint);
	            mTouchRadius += 1;
        	}
        	
        	else {
        		float left = mCameraX - mCameraRadius;
	            float right = mCameraX + mCameraRadius;
	            float top = mCameraY - mCameraRadius;
	            float bottom = mCameraY + mCameraRadius;
	            canvas.drawOval(new RectF(left, top, right, bottom), mOrangePaint);
        	}
        }
    }

    protected static final String TAG = "CameraOverlay";

    private CameraThread thread;

	private GestureDetector mGestureDetector;

	private boolean mTouchDown;

	private int mTouchRadius;

	private float mCameraX;

	private float mCameraY;

	private int mCameraRadius;

	private float mCameraXRatio;

	private float mCameraYRatio;

	private float mRadiusRatio;


    public CameraOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        SurfaceHolder holder = getHolder();

        holder.addCallback(this);
        setZOrderOnTop(true);
        holder.setFormat(PixelFormat.TRANSPARENT);

        thread = new CameraThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
//                mStatusText.setVisibility(m.getData().getInt("viz"));
//                mStatusText.setText(m.getData().getString("text"));
                Log.i(TAG, "Message " + m);
            }
        });
        
        setFocusable(true);
        
        mGestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//    	mGestureDetector.onTouchEvent(event);
    	
    	switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Log.i(TAG, "ACTION_DOWN");
			mTouchRadius = 0;
			mTouchDown = true;
			mCameraX = event.getX();
			mCameraY = event.getY();
			break;
		case MotionEvent.ACTION_UP:
			Log.i(TAG, "ACTION_UP");
			mTouchDown = false;
			mCameraRadius = mTouchRadius;
			mCameraXRatio = mCameraX/getWidth();
			mCameraYRatio = mCameraY/getHeight();
			mRadiusRatio = (float)(mCameraRadius)/getWidth();
			break;
//		default:
//			Log.i(TAG, "Unrecognized action " + event.getAction());
//			break;
		}
    	
    	return true;
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) thread.pause();
    }

    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        thread.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread.setRunning(true);
        thread.start();
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

	public float getCameraX() {
		return mCameraXRatio;
	}

	public float getCameraY() {
		return mCameraYRatio;
	}

	public float getRadius() {
		return mRadiusRatio;
	}
	
	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
//		@Override
//		public boolean onDown(MotionEvent e) {
//			Log.i(TAG, "onDown");
//			return super.onDown(e);
//		}
//		
//		@Override
//		public void onShowPress(MotionEvent e) {
//			Log.i(TAG, "onShowPress");
//			super.onShowPress(e);
//		}
//		
//		@Override
//		public boolean onSingleTapUp(MotionEvent e) {
//			Log.i(TAG, "onSingleTapUp");
//			return super.onSingleTapUp(e);
//		}
	}
}
