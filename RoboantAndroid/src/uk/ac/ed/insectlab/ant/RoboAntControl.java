package uk.ac.ed.insectlab.ant;

import android.util.Log;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class RoboAntControl {
    private int mRightSpeed;
    private int mLeftSpeed;
    private SerialInputOutputManager mSerialIoManager;
    private boolean mLookingAround;
    private LookAroundListener mLookAroundListener;
    
    private static final long WRITE_INTERVAL = 25; // min 25
    private static final String TAG = "RoboAntControl";
    
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
}
