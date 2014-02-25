package uk.ac.ed.insectlab.ant.service;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ed.insectlab.ant.ArduinoZumoControl;
import uk.ac.ed.insectlab.ant.service.RoboantService.SerialBond;
import android.app.Service;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class SerialThread extends Thread {

	private static final int MESSAGE_REFRESH = 42;
	private static final long REFRESH_TIMEOUT_MILLIS = 5000;
	protected static final String TAG = SerialThread.class.getSimpleName();
	private final static ExecutorService mExecutor = Executors.newSingleThreadExecutor();

	enum SerialState {
		DRIVER_SEARCH, CONNECTED;
	}
	private static SerialState mState;

	interface SerialListener {
		void deviceSpeedsReceived(int left, int right);

		void onSerialConnected();

		void onSerialDisconnected();
	}

	private static SerialListener mSerialListener;

	private static String messageBuffer;
	private static Pattern mSpeedPattern = Pattern.compile("l(-?\\d+)r(-?\\d+)");

	private static UsbSerialDriver sDriver = null;
	private static SerialInputOutputManager mSerialIoManager;

	private static UsbManager mUsbManager;

	private static ArduinoZumoControl mRoboAntControl;

	private static Object lock = new Object();

	private final static SerialInputOutputManager.Listener mIOManagerListener =
			new SerialInputOutputManager.Listener() {

		@Override
		public void onRunError(Exception e) {
			Log.d(TAG, "Runner stopped.");
			synchronized (lock) {
				mState = SerialState.DRIVER_SEARCH;
				Log.i(TAG, "Notifying from " + Thread.currentThread().getName());
				lock.notify();
			}
		}

		@Override
		public void onNewData(final byte[] data) {
			updateReceivedData(data);
		}
	};
	private static SerialBond mListener;
	private boolean mRunning = true;

	public SerialThread(Service service) {

		mUsbManager = (UsbManager) service.getSystemService(Context.USB_SERVICE);
		mSerialListener = (SerialListener) service;

		mState = SerialState.DRIVER_SEARCH;
	}

	private static void refreshDeviceList() {
		UsbSerialDriver driver = UsbSerialProber.findFirstDevice(mUsbManager);
		if (driver != null) {
			sDriver = driver;
			openDriver();
			startIoManager();
		}
	}

	private void stopIoManager() {
		if (mSerialIoManager != null) {
			Log.i(TAG, "Stopping io manager ..");
			if (mRoboAntControl != null) {
				setSpeeds(0, 0);
			}
			mSerialIoManager.stop();
			mSerialIoManager = null;
			mSerialListener.onSerialDisconnected();
			if (mListener != null) {
				mListener.serialDisconnected();
			}

		}
		if (sDriver != null) {
			try {
				sDriver.close();
			} catch (IOException e) {
				// Ignore.
			}
			sDriver = null;
		}
		if (mState == SerialState.CONNECTED) {
			mState = SerialState.DRIVER_SEARCH;
			synchronized (lock) {
				lock.notify();
			}
		}
	}

	private static void startIoManager() {
		if (sDriver != null) {
			Log.i(TAG, "Starting io manager ..");
			mSerialIoManager = new SerialInputOutputManager(sDriver, mIOManagerListener);
			mExecutor.submit(mSerialIoManager);
			mRoboAntControl = new RoboAntControl(mSerialIoManager);
			mState = SerialState.CONNECTED;
			mSerialListener.onSerialConnected();
			if (mListener != null) {
				mListener.serialConnected(mRoboAntControl);
			}
		}
	}

	private static void openDriver() {
		try {
			sDriver.open();
			sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
		} catch (IOException e) {
			Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
			Log.i(TAG, "Error opening serial device");
			try {
				sDriver.close();
			} catch (IOException e2) {
				// Ignore.
			}
			sDriver = null;
			return;
		}
	}

	public void usbDisconnectedIntentReceived() {
		stopIoManager();
		Log.i(TAG, "Notifying from " + Thread.currentThread().getName());
	}

	public void setSpeeds(int left, int right) {
		if (mRoboAntControl != null) {
			mRoboAntControl.setSpeeds(left, right);
		}
	}

	@Override
	public void run() {
		Log.i(TAG, "Thread starting run " + Thread.currentThread().getName());
		while (true) {
			if (!mRunning) {
				return;
			}
			while (mState == SerialState.DRIVER_SEARCH) {
				refreshDeviceList();
				try {
					Thread.sleep(REFRESH_TIMEOUT_MILLIS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (!mRunning) {
				return;
			}

			while (mState == SerialState.CONNECTED) {
				synchronized (lock) {
					try {
						Log.i(TAG, "Start wait " + Thread.currentThread().getName());
						lock.wait();
						Log.i(TAG, "End wait");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private static void updateReceivedData(byte[] data) {
		String message = new String(data);
		if (messageBuffer != null) {
			message = messageBuffer + message;
		}
		Log.i(TAG, "Message from serial: " + message);
		Matcher matcher = mSpeedPattern.matcher(message);
		if (matcher.find()) {
			int left = Integer.parseInt(matcher.group(1));
			int right = Integer.parseInt(matcher.group(2));
			mSerialListener.deviceSpeedsReceived(left, right);
			if (mListener != null) {
				mListener.serialHeartbeat(left, right);
			}
			messageBuffer = null;
		}
		else {
			messageBuffer = message;
		}
	}

	public static ArduinoZumoControl getRoboAntControl() {
		return mRoboAntControl;
	}

	public SerialState getSerialState() {
		return mState;
	}

	public void setBond(SerialBond serialBond) {
		mListener = serialBond;
		if (mState == SerialState.CONNECTED) {
			mListener.serialConnected(mRoboAntControl);
		}
		else {
			mListener.serialDisconnected();
		}
	}

	public Spanned getDescription() {
		String str = "<b>Serial</b> ";
		String status;
		if (mState == SerialState.CONNECTED) {
			status = "connected";
		}
		else {
			status = "disconnected";
		}
		return Html.fromHtml(str + status);
	}

	public void setRunning(boolean running) {
		mRunning = running;
		if (running == false) {
			stopIoManager();
		}
	}

}
