package uk.ac.ed.insectlab.ant.service;

import uk.ac.ed.insectlab.ant.ArduinoZumoControl;
import android.util.Log;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class RoboAntControl implements ArduinoZumoControl {
	private int mRightSpeed;
	private int mLeftSpeed;
	private SerialInputOutputManager mSerialIoManager;

	private static final long WRITE_INTERVAL = 25; // min 25
	private static final String TAG = "RoboAntControl";

//	private HandlerThread mHandlerThread;

//	private Handler mHandler;

	int TURN_SPEED = 80;

	public RoboAntControl(SerialInputOutputManager sm) {
		mSerialIoManager = sm;
//		mHandlerThread = new HandlerThread("ai_control_handler");
//		mHandlerThread.start();

//		while(!mHandlerThread.isAlive()) {};  
//		mHandler = new Handler(mHandlerThread.getLooper(), null);
	}

	private synchronized void sendSpeeds() {
		Log.i(TAG, "Sending speeds " + mLeftSpeed + " " + mRightSpeed);
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

	public void simpleTurnInPlaceBlocking(final int turnSpeed,  final int turnTime) {

		setSpeeds(turnSpeed, -turnSpeed);

		try {
			Thread.sleep(turnTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		setSpeeds(0, -0);

	}
}
