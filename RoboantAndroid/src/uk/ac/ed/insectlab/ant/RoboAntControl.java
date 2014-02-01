package uk.ac.ed.insectlab.ant;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class RoboAntControl implements SensorEventListener {
	private int mRightSpeed;
	private int mLeftSpeed;
	private SerialInputOutputManager mSerialIoManager;
	private boolean mLookingAround;
	private LookAroundListener mLookAroundListener;
	private float[] mGravity;
	private float[] mGeomagnetic;
	private float azimut;

	private static final long WRITE_INTERVAL = 25; // min 25
	private static final String TAG = "RoboAntControl";
	private static final int DIR_THRESHOLD = 5;

	int TURN_SPEED = 80;

	public RoboAntControl(SerialInputOutputManager sm) {
		mSerialIoManager = sm;
	}

	@Deprecated
	public synchronized void setRightSpeed(int speed) {
		mRightSpeed = speed; 
		sendSpeeds();
	}

	private synchronized void sendSpeeds() {
		String str = "l" + mLeftSpeed + "r" + mRightSpeed + "\n";
		mSerialIoManager.writeAsync(str.getBytes()); 
		try {
			Thread.sleep(WRITE_INTERVAL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public synchronized void setSpeeds(int left, int right) {
		mLeftSpeed = left; mRightSpeed = right;
		sendSpeeds();
	}
	@Deprecated
	public synchronized void setLeftSpeed(int speed) {
		mLeftSpeed = speed; 
		sendSpeeds();
	}

	public void doLookAroundStep(LookAroundListener listener) {
		Log.i(TAG, "doLookAroundStep " + mLookingAround);
		if (!mLookingAround) {
			mSerialIoManager.writeAsync("t\n".getBytes()); 
			mLookingAround = true;
		}
		else {
			mSerialIoManager.writeAsync("n\n".getBytes()); 
		}
		mLookAroundListener = listener;
	}

	public void lookAroundDone() {
		Log.i(TAG, "lookAroundDone");
		mLookingAround = false;
		mLookAroundListener.lookAroundDone();
	}

	public void lookAroundStepDone(int step) {
		Log.i(TAG, "lookAroundStepDone " + step);
		mLookAroundListener.lookAroundStepDone(step);
	}

	public void doGoTowards(LookAroundListener lookAroundListener, int step) {
		Log.i(TAG, "doGoTowards");
		mLookAroundListener = lookAroundListener;
		mSerialIoManager.writeAsync(("g"+step+'\n').getBytes());
	}

	public void goTowardsDone() {
		Log.i(TAG, "goTowardsDone");
		mLookAroundListener.goTowardsDone();
	}

	public void calibrate() {
		mSerialIoManager.writeAsync("c\n".getBytes()); 
	}
	
	public void setCompassView(TextView view) {
		mCompassView = view;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.i(TAG, "onAccuracyChanged");
	}

	TextView mCompassView;
	private float mDirectionAdjustment;
	private int mTargetDirection;
	private AIControlTask mControl;
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravity = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagnetic = event.values;
		if (mGravity != null && mGeomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				azimut = orientation[0]; // orientation contains: azimut, pitch and roll
				
				if (mControl != null) {
//					Log.i(TAG, "Dir is " + getDirection() + " target is " + mTargetDirection);
//					int offset = getDirection() - mTargetDirection;
					int relativeHeading = relativeHeading(getDirection(), mTargetDirection);

					if (Math.abs(relativeHeading) < DIR_THRESHOLD) {
						mControl.notifyTargetReached();
						mControl = null;
						setSpeeds(0, 0);
					}
					else {
						int dir = relativeHeading > 0 ? 1 : -1;
						//TODO: decreasing speed maybe?
						setSpeeds(dir*TURN_SPEED, -dir*TURN_SPEED);
					}
				}
				if (mCompassView != null) {
					mCompassView.post(new Runnable() {
						
						@Override
						public void run() {
							mCompassView.setText(getDirection() +  "");
						}
					});
				}
			}
		}
	}

	private int relativeHeading(int fromDeg, int toDeg) {
		int relativeHeading = toDeg - fromDeg;
		
		if (relativeHeading > 180) {
			relativeHeading -= 360;
		}
		if (relativeHeading < -180) {
			relativeHeading += 360;
		}

		return relativeHeading;
	}

	public int getDirection() {
//		int dir = (int)(Math.toDegrees(azimut + mDirectionAdjustment));
		int dir = (int)Math.toDegrees(azimut); //TODO: average over n azimuts
		
//		if (dir < -180) {
//			dir = 360 + dir;
//		}
		
//		if (dir < -180) {
//			dir = 360 + dir;
//		}
//		if (dir < 0) {
//			dir += 360;
//		}
		return dir;
	}

	public void setDirectionZero() {
		mDirectionAdjustment = -azimut;
	}

	public void setTargetDirection(int target, AIControlTask aiControlTask) {
		mTargetDirection = target;
		mControl = aiControlTask;
	}
	
	public void doTurn(int degrees, AIControlTask aiControlTask) {
		Log.i(TAG, "Doing turn to " + degrees);
		mTargetDirection = (getDirection() + degrees) % 360;
		mControl = aiControlTask;
	}
}
