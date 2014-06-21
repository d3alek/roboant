package uk.ac.ed.insectlab.ant;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import uk.ac.ed.insectlab.ant.CameraFragment.CameraListener;
import uk.ac.ed.insectlab.ant.NetworkFragment.NetworkFragmentListener;
import uk.ac.ed.insectlab.ant.PairedDeviceChooserDialog.BluetoothDeviceChosenListener;
import uk.ac.ed.insectlab.ant.RouteSelectionDialogFragment.RouteSelectedListener;
import uk.ac.ed.insectlab.ant.SerialFragment.SerialFragmentListener;
import uk.ac.ed.insectlab.ant.service.BluetoothThread;
import uk.ac.ed.insectlab.ant.service.RoboantService;
import uk.ac.ed.insectlab.ant.service.RoboantService.BluetoothBond;
import uk.ac.ed.insectlab.ant.service.RoboantService.LocalBinder;
import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity implements NetworkFragmentListener, SerialFragmentListener,
CameraListener, RouteSelectedListener, BluetoothDeviceChosenListener, BluetoothBond {

	private static final int CAMERA_NUMBER = 1;

	private static final String NAVIGATION_FRAGMENT = "navigation_fragment";

	private static final int REQUEST_ENABLE_BT = 0;

	private final String TAG = MainActivity.class.getSimpleName();

	private WakeLock mWakeLock;

	private NetworkFragment mNetworkFragment;

	private SerialFragment mSerialFragment;

	private CameraFragment mCameraFragment;

	private ArduinoZumoControl mRoboantControl;

	private BluetoothAdapter mBluetoothAdapter;

	protected RoboantService mService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		//		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		//		this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
		//		this.mWakeLock.acquire();

		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();

		mNetworkFragment = new NetworkFragment();
		mSerialFragment = new SerialFragment();
		mCameraFragment = new CameraFragment();


		transaction.add(R.id.fragment_container, mNetworkFragment);
		transaction.add(R.id.fragment_container, mSerialFragment);
		transaction.add(R.id.fragment_container, mCameraFragment);

		transaction.commit();

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		else {
			bluetoothPair();
		}


	}

	@Override
	protected void onStart() {
		super.onStart();
		Intent intent = new Intent(this, RoboantService.class);
		startService(intent);
		bindService(intent, mConnection, Context.BIND_ABOVE_CLIENT);
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className,
				IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			binder.bindSerial(mSerialFragment);
			binder.bindNetwork(mNetworkFragment);
			binder.bindBluetooth(MainActivity.this);
			mService = binder.getService();
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};

	private boolean mBound;

	private NavigationFragment mNavigationFragment;

	private BluetoothThread mBluetoothThread;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
			bluetoothPair();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void bluetoothPair() {
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
			// Loop through paired devices
			//		    for (BluetoothDevice device : pairedDevices) {
			// Add the name and address to an array adapter to show in a ListView
			//		        mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
			//		    }
			PairedDeviceChooserDialog.makeInstance(pairedDevices, this).show(getFragmentManager(), "TAG");;
		}
		else {
			Toast.makeText(this, "No paired devices", Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}


	@Override
	protected void onStop() {
		super.onStop();
		if (mBound) {
			unbindService(mConnection);
		}
	}

	@Override
	public void cameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void cameraViewStopped() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLensFound(boolean b) {
		// TODO Auto-generated method stub

	}

	@Override
	public void freeCamera(Handler handler, int cameraFree) {
		if (mCameraFragment == null) {
			handler.sendEmptyMessage(cameraFree);
		}

		else {
			mCameraFragment.releaseCamera();
			handler.sendEmptyMessage(cameraFree);
		}
	}

	@Override
	public void onSerialConnected() {
		mNavigationFragment = (NavigationFragment) getFragmentManager().findFragmentByTag(NAVIGATION_FRAGMENT);
		if (mNavigationFragment == null) {
			mNavigationFragment = new NavigationFragment();
			getFragmentManager().beginTransaction().add(R.id.fragment_container, mNavigationFragment, NAVIGATION_FRAGMENT).commit();
		}

	}

	@Override
	public void onSerialDisconnected() {

	}

	@Override
	public void onRouteSelected(List<Bitmap> bitmap) {
		if (mNavigationFragment != null) {
			mNavigationFragment.onRouteSelected(bitmap);
		}
	}

	@Override
	public void onRecordRoute() {
		if (mNavigationFragment != null) {
			Toast.makeText(MainActivity.this, "Recording route", Toast.LENGTH_SHORT).show();
			mNavigationFragment.recordRoute(mCameraFragment);				
		}
	}

	@Override
	public void recordMessageReceived(final boolean torecord) {
		Log.i(TAG, "IMHERE " + torecord);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (torecord) {
					onRecordRoute();
				}
				else {
					if (mNavigationFragment != null) {
						mNavigationFragment.stopRecordingRoute();
					}
				}		
			}
		});

	}

	@Override
	public void navigationMessageReceived() {
		mNavigationFragment.beginNavigationMostRecentRoute();
	}

	@Override
	public void deviceSelected(BluetoothDevice bluetoothDevice) {
		new ConnectThread(bluetoothDevice).start();
	}
	
	private class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	 
	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord(
	            		UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
	        } catch (IOException e) { }
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	        // Cancel discovery because it will slow down the connection
	        mBluetoothAdapter.cancelDiscovery();
	 
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	 
	        // Do work to manage the connection (in a separate thread)
	        mService.startBluetoothThread(mmSocket);
	    }
	 
	    /** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}

	@Override
	public void bluetoothConnected(BluetoothThread bluetoothThread) {
		mBluetoothThread = bluetoothThread;
	}

	@Override
	public void bluetoothDisconnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bluetoothMessageReceived(String message) {
		Log.i(TAG, "bluetoothMessageReceived " + message);
		if (message.contains("RECORDON")) {
			recordMessageReceived(true);
		}
		else if (message.contains("RECORDOFF")) {
			recordMessageReceived(false);
		}
	}


}
