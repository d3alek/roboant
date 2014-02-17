package uk.ac.ed.insectlab.ant;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class SerialFragment extends CardFragment {

	private static final String TAG = SerialFragment.class.getSimpleName();

	private static final int MESSAGE_REFRESH = 42;
	private static final long REFRESH_TIMEOUT_MILLIS = 5000;

	interface SerialListener {
		void deviceSpeedsReceived(int left, int right);

		void onSerialConnected();

		void onSerialDisconnected();
	}


	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_REFRESH:
				Log.i(TAG, "Refresh message received");
				if (sDriver == null) {
					refreshDeviceList();
					mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, REFRESH_TIMEOUT_MILLIS);
				}
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	};

	//    /** Simple container for a UsbDevice and its driver. */
	//    private static class DeviceEntry {
	//        public UsbDevice device;
	//        public UsbSerialDriver driver;
	//
	//        DeviceEntry(UsbDevice device, UsbSerialDriver driver) {
	//            this.device = device;
	//            this.driver = driver;
	//        }
	//    }

	private String messageBuffer;
	private Pattern mSpeedPattern = Pattern.compile("l(-?\\d+)r(-?\\d+)");

	private UsbSerialDriver sDriver = null;
	private SerialInputOutputManager mSerialIoManager;
	private final SerialInputOutputManager.Listener mIOManagerListener =
			new SerialInputOutputManager.Listener() {

		@Override
		public void onRunError(Exception e) {
			Log.d(TAG, "Runner stopped.");
			setStatus(CardStatus.ERROR);
		}

		@Override
		public void onNewData(final byte[] data) {
			updateReceivedData(data);
			//			MainActivity.this.runOnUiThread(new Runnable() {
			//				@Override
			//				public void run() {
			//					MainActivity.this.updateReceivedData(data);
			//				}
			//			});
		}
	};

	private UsbManager mUsbManager;

	private SerialListener mSerialListener;

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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mRoboAntControl = new DummyArduinoZumoControl();
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mSerialListener = (SerialListener)activity;
		} catch(ClassCastException e) {
			e.printStackTrace();
			throw new RuntimeException("Host activity does not implement listener");
		}

		mUsbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setLabel("Serial");
	}

	private void refreshDeviceList() {
		setStatus(CardStatus.LOADING);

		//        new AsyncTask<Void, Void, List<DeviceEntry>>() {
		//            private UsbSerialDriver mDriver;
		//
		//            @Override
		//            protected List<DeviceEntry> doInBackground(Void... params) {
		//                Log.d(TAG, "Refreshing device list ...");
		//                SystemClock.sleep(1000);
		//                final List<DeviceEntry> result = new ArrayList<DeviceEntry>();
		//                for (final UsbDevice device : mUsbManager.getDeviceList().values()) {
		//                    final List<UsbSerialDriver> drivers =
		//                            UsbSerialProber.probeSingleDevice(mUsbManager, device);
		//                    Log.d(TAG, "Found usb device: " + device);
		//                    if (drivers.isEmpty()) {
		//                        Log.d(TAG, "  - No UsbSerialDriver available.");
		//                        result.add(new DeviceEntry(device, null));
		//                    } else {
		//                        for (UsbSerialDriver driver : drivers) {
		//                            Log.d(TAG, "  + " + driver);
		//                            result.add(new DeviceEntry(device, driver));
		//                            mDriver = driver;
		//                        }
		//                    }
		//                }
		//                return result;
		//            }
		//
		//            @Override
		//            protected void onPostExecute(List<DeviceEntry> result) {
		//            	if (mDriver == null) {
		//            		setStatus(CardStatus.NONE);
		//            		stopIoManager();
		//            	}
		//            	else if (sDriver == null) {
		//            		mHandler.removeMessages(MESSAGE_REFRESH);
		//            		sDriver = mDriver;
		//            		startIoManager();
		//            		setStatus(CardStatus.OK);
		//            	}
		//            }
		//
		//        }.execute((Void) null);

		UsbSerialDriver driver = UsbSerialProber.findFirstDevice(mUsbManager);
		if (driver != null && sDriver == null) {
			sDriver = driver;
			mHandler.removeMessages(MESSAGE_REFRESH);
			openDriver();
			startIoManager();
			setStatus(CardStatus.OK);
		}
	}

	private void stopIoManager() {
		if (mSerialIoManager != null) {
			Log.i(TAG, "Stopping io manager ..");
			setSpeeds(0, 0);
			mSerialIoManager.stop();
			mSerialIoManager = null;
			mSerialListener.onSerialDisconnected();
		}
	}

	private void startIoManager() {
		if (sDriver != null) {
			Log.i(TAG, "Starting io manager ..");
			mSerialIoManager = new SerialInputOutputManager(sDriver, mIOManagerListener);
			mExecutor.submit(mSerialIoManager);
			mSerialListener.onSerialConnected();
			mRoboAntControl = new RoboAntControl(mSerialIoManager);
			setStatus(CardStatus.OK);
		}
	}

	private void onDeviceStateChange() {
		stopIoManager();
		startIoManager();
	}

	private void updateReceivedData(byte[] data) {
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
			Log.i(TAG, "No serial device");
			mHandler.sendEmptyMessage(MESSAGE_REFRESH);
		} else {
			openDriver();
		}
		onDeviceStateChange();
	}

	private void openDriver() {
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
			mHandler.sendEmptyMessage(MESSAGE_REFRESH);
			return;
		}
	}


	@Override
	public void onPause() {
		mHandler.removeMessages(MESSAGE_REFRESH);
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


	public void setSpeeds(int left, int right) {
		mRoboAntControl.setSpeeds(left, right);
	}


	public void usbDisconnectedIntentReceived() {
		stopIoManager();
		if (sDriver != null) {
			try {
				sDriver.close();
			} catch (IOException e) {
				// Ignore.
			}
			sDriver = null;
		}
		mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, REFRESH_TIMEOUT_MILLIS);
	}
}
