package uk.ac.ed.insectlab.ant;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class RobotControlFragment extends Fragment {

	private String messageBuffer;
	private Pattern mSpeedPattern = Pattern.compile("l(-?\\d+)r(-?\\d+)");

	private Pattern mLookAroundPattern = Pattern.compile("la\\s(g|d|\\d+)");
	/**
	 * Driver instance, passed in statically via
	 * {@link #show(Context, UsbSerialDriver)}.
	 *
	 * <p/>
	 * This is a devious hack; it'd be cleaner to re-create the driver using
	 * arguments passed in with the {@link #startActivity(Intent)} intent. We
	 * can get away with it because both activities will run in the same
	 * process, and this is a simple demo.
	 */
	private static UsbSerialDriver sDriver = null;
	private SerialInputOutputManager mSerialIoManager;
	private final SerialInputOutputManager.Listener mListener =
			new SerialInputOutputManager.Listener() {

		@Override
		public void onRunError(Exception e) {
			Log.d(TAG, "Runner stopped.");
		}

		@Override
		public void onNewData(final byte[] data) {
			MainActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					MainActivity.this.updateReceivedData(data);
				}
			});
		}
	};
	private ArduinoZumoControl mRoboAntControl;

	private class DummyArduinoZumoControl implements ArduinoZumoControl {
		@Override
		public void setSpeeds(int turnSpeed, int i) {
			Log.i(TAG, "Dummy setSpeeds");
		}

		@Override
		public void simpleTurnInPlaceBlocking(int i, int turnTime) {
			Log.i(TAG, "Dummy simpleTurnInPlaceBlocking");

		}

		@Override
		public void doGoTowards(LookAroundListener aiControlTask,
				int minStep) {
			Log.i(TAG, "Dummy doGoTowards");

		}

		@Override
		public void doLookAroundStep(LookAroundListener aiControlTask) {
			Log.i(TAG, "Dummy doLookAroundStep");
		}

		@Override
		public void setLeftSpeed(int speed) {
			Log.i(TAG, "Dummy setLeftSpeed");
		}

		@Override
		public void setRightSpeed(int speed) {
			Log.i(TAG, "Dummy setRightSpeed");
		}

		@Override
		public void calibrate() {
			Log.i(TAG, "Dummy calibrate");
		}

		@Override
		public void lookAroundDone() {
			Log.i(TAG, "Dummy lookAroundDone");
		}

		@Override
		public void goTowardsDone() {
			Log.i(TAG, "Dummy goTowardsDone");
		}

		@Override
		public void lookAroundStepDone(int parseInt) {
			Log.i(TAG, "Dummy lookAroundStepDone");
		}
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mRoboAntControl = new DummyArduinoZumoControl();
		return super.onCreateView(inflater, container, savedInstanceState);

	}

	private void stopIoManager() {
		if (mSerialIoManager != null) {
			Log.i(TAG, "Stopping io manager ..");
			mSerialIoManager.stop();
			mSerialIoManager = null;
			mRoboAntControl = null;
		}
	}

	private void startIoManager() {
		if (sDriver != null) {
			Log.i(TAG, "Starting io manager ..");
			mSerialIoManager = new SerialInputOutputManager(sDriver, mListener);
			mExecutor.submit(mSerialIoManager);
			mRoboAntControl = new RoboAntControl(mSerialIoManager);
		}
	}

	private void onDeviceStateChange() {
		stopIoManager();
		startIoManager();
	}


	/**
	 * Starts the activity, using the supplied driver instance.
	 *
	 * @param context
	 * @param driver
	 */
	static void show(Context context, UsbSerialDriver driver) {
		sDriver = driver;
		final Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
		context.startActivity(intent);
	}

	private void updateReceivedData(byte[] data) {
		String message = new String(data);
		if (messageBuffer != null) {
			message = messageBuffer + message;
		}
		Log.i(TAG, "Message from serial: " + message);
		Matcher matcher = mSpeedPattern.matcher(message);
		Matcher laMatcher = mLookAroundPattern.matcher(message);
		if (laMatcher.find()) {
			Log.i(TAG, "Look around message received!");
			if (laMatcher.group(1).equals("d")) {
				Log.i(TAG, "Look around done!");
				mRoboAntControl.lookAroundDone();
			}
			else if (laMatcher.group(1).equals("g")) {
				Log.i(TAG, "Go towards done!");
				mRoboAntControl.goTowardsDone();
			}
			else {
				Log.i(TAG, "Look around step " + laMatcher.group(1));
				mRoboAntControl.lookAroundStepDone(Integer.parseInt(laMatcher.group(1)));
			}
			messageBuffer = null;
		}
		else if (matcher.find()) {
			int left = Integer.parseInt(matcher.group(1));
			int right = Integer.parseInt(matcher.group(2));
			mManualControlFragment.setSpeeds(left, right);
			messageBuffer = null;
		}
		else {
			messageBuffer = message;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "Resumed, sDriver=" + sDriver);
		if (sDriver == null) {
			Toast.makeText(this, "No serial device", Toast.LENGTH_LONG).show();;
		} else {
			try {
				sDriver.open();
				sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
			} catch (IOException e) {
				Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
				//                mTitleTextView.setText("Error opening device: " + e.getMessage());
				Toast.makeText(this, "Error opening device", Toast.LENGTH_LONG).show();;
				try {
					sDriver.close();
				} catch (IOException e2) {
					// Ignore.
				}
				sDriver = null;
				return;
			}
			Toast.makeText(this, "Serial device " + sDriver.getClass().getSimpleName(), Toast.LENGTH_LONG).show();;
		}
		onDeviceStateChange();
	}

	@Override
	public void onPause() {
		stopIoManager();
		if (sDriver != null) {
			try {
				sDriver.close();
			} catch (IOException e) {
				// Ignore.
			}
			sDriver = null;
		}

		super.onPause();
	}
}
