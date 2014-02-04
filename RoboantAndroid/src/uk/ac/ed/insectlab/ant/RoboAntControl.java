package uk.ac.ed.insectlab.ant;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class RoboAntControl {
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
	private static final int DIR_THRESHOLD = 8;

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
}
