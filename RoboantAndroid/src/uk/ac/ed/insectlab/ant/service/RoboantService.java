package uk.ac.ed.insectlab.ant.service;

import uk.ac.ed.insectlab.ant.ArduinoZumoControl;
import uk.ac.ed.insectlab.ant.MainActivity;
import uk.ac.ed.insectlab.ant.service.BluetoothThread.BluetoothListener;
import uk.ac.ed.insectlab.ant.service.ClientThread.NetworkListener;
import uk.ac.ed.insectlab.ant.service.SerialThread.SerialListener;
import uk.co.ed.insectlab.ant.R;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class RoboantService extends Service implements SerialListener, NetworkListener,
BluetoothListener {
	private final IBinder mBinder = new LocalBinder();
	private SerialThread mSerialThread;
	private ClientThread mClientThread;
	private static final String TAG = RoboantService.class.getSimpleName();
	private static final int NOTIFICATION_ID = 1;
	private static final String ACTION_DISMISS = "dismiss";
	private static final boolean DEFAULT_REMOTE_CONTROL = true;

	public interface SerialBond {

		void serialDisconnected();

		void serialConnected(ArduinoZumoControl roboantControl);

		void serialHeartbeat(int left, int right);

	}

	public interface NetworkBond {

		void serverConnected(TcpClient tcpClient);

		void serverDisconnected();

		void messageReceived(String message);

	}
	
	public interface BluetoothBond {

		void bluetoothConnected(BluetoothThread bluetoothThread);

		void bluetoothDisconnected();

		void bluetoothMessageReceived(String message);

	}

	BroadcastReceiver mUsbDisconnectedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mSerialThread.usbDisconnectedIntentReceived();
		}
	};
	private boolean mRemoteControl = DEFAULT_REMOTE_CONTROL;
	private BluetoothThread mBluetoothThread;

	@Override
	public void onCreate() {
		super.onCreate();

		mSerialThread = new SerialThread(this);
		mClientThread = new ClientThread(this);

		Log.i(TAG, "Roboant service thread is " + Thread.currentThread().getName());
		mSerialThread.start();
		mClientThread.start();

		updateNotification();
	}

	private void updateNotification() {
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.ic_ant)
		.setContentTitle("Roboant")
		.setContentText("Client and serial service");

		NotificationCompat.InboxStyle inboxStyle = 
				new NotificationCompat.InboxStyle();

		inboxStyle.setBigContentTitle("Roboant");

		mBuilder.setStyle(inboxStyle);

		Intent dismissIntent = new Intent(this, RoboantService.class);
		dismissIntent.setAction(ACTION_DISMISS);
		PendingIntent piDismiss = PendingIntent.getService(this, 0, dismissIntent, 0);
		mBuilder.addAction(android.R.drawable.ic_delete, "Terminate", piDismiss);

		inboxStyle.addLine(mSerialThread.getDescription());
		inboxStyle.addLine(mClientThread.getDescription());

		Intent resultIntent = new Intent(this, MainActivity.class);

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(MainActivity.class);
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
						);
		mBuilder.setContentIntent(resultPendingIntent);
		startForeground(NOTIFICATION_ID, mBuilder.build());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String action = intent.getAction();
			if (action == null) {
			}
			else if (action.equals(ACTION_DISMISS)) {
				mSerialThread.setRunning(false);
				mClientThread.setRunning(false);
				stopSelf();
			}
		}

		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	public class LocalBinder extends Binder {
		//		public ArduinoZumoControl getRoboantControl() {
		//			if (mSerialThread.getSerialState() == SerialState.CONNECTED) {
		//				return SerialThread.getRoboAntControl();
		//			}
		//
		//			Log.e(TAG, "getRoboantControl null");
		//			return null;
		//		}

		private BluetoothBond mBluetoothBond;

		public RoboantService getService() {
			return RoboantService.this;
		}

		public void bindSerial(SerialBond serialBond) {
			mSerialThread.setBond(serialBond);
		}

		public void bindNetwork(NetworkBond networkBond) {
			mClientThread.setBond(networkBond);
		}

		public void bindBluetooth(BluetoothBond bluetoothBond) {
			mBluetoothBond = bluetoothBond;
			if (mBluetoothThread != null) {
				mBluetoothThread.setBound(mBluetoothBond);
			}
		}

	}

	@Override
	public void deviceSpeedsReceived(int left, int right) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSerialConnected() {
		Log.i(TAG, "onSerialConnected");
		IntentFilter intentFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbDisconnectedReceiver, intentFilter);
		updateNotification();
	}

	@Override
	public void onSerialDisconnected() {
		Log.i(TAG, "onSerialDisconnected");
		unregisterReceiver(mUsbDisconnectedReceiver);
		updateNotification();
	}

	@Override
	public void speedReceivedFromNetwork(int left, int right) {
		Log.i(TAG, "speed received from network");
		if (mRemoteControl) {
			mSerialThread.setSpeeds(left, right);
		}
	}
	
	@Override
	public void speedReceivedFromBluetooth(int left, int right) {
		Log.i(TAG, "speed received from bluetooth " + left + " " + right);
		if (mRemoteControl) {
			mSerialThread.setSpeeds(left, right);
		}
	}

	@Override
	public void serverConnected() {
		// TODO Auto-generated method stub
		Log.i(TAG, "Server connected");
		updateNotification();

	}

	@Override
	public void serverDisconnected() {
		Log.i(TAG, "Server disconnected");
		updateNotification();
	}

	public void setRemoteControl(boolean remoteControl) {
		mRemoteControl = remoteControl;
	}
	
	public void startBluetoothThread(BluetoothSocket socket) {
		mBluetoothThread = new BluetoothThread(socket, this);
		mBluetoothThread.start();
	}

	public BluetoothThread getBluetoothThread() {
		return mBluetoothThread;
	}

}
