package uk.ac.ed.insectlab.ant;

import static android.widget.Toast.LENGTH_SHORT;
import static uk.ac.ed.insectlab.ant.bluetooth.Mode.RECORDING_MODE;

import java.util.List;

import uk.ac.ed.insectlab.ant.BluetoothFragment.BluetoothFragmentListener;
import uk.ac.ed.insectlab.ant.CameraFragment.CameraListener;
import uk.ac.ed.insectlab.ant.NetworkFragment.NetworkFragmentListener;
import uk.ac.ed.insectlab.ant.RouteSelectionDialogFragment.RouteSelectedListener;
import uk.ac.ed.insectlab.ant.SerialFragment.SerialFragmentListener;
import uk.ac.ed.insectlab.ant.service.RoboantService;
import uk.ac.ed.insectlab.ant.service.RoboantService.LocalBinder;
import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity implements NetworkFragmentListener,
		SerialFragmentListener, BluetoothFragmentListener, CameraListener,
		RouteSelectedListener {

	private static final String NAVIGATION_FRAGMENT = "navigation_fragment";

	private final String TAG = MainActivity.class.getSimpleName();

	private NetworkFragment mNetworkFragment;

	private SerialFragment mSerialFragment;

	private CameraFragment mCameraFragment;

	protected RoboantService mService;

	private BluetoothFragment mBluetoothFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		// final PowerManager pm = (PowerManager)
		// getSystemService(Context.POWER_SERVICE);
		// this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
		// "My Tag");
		// this.mWakeLock.acquire();

		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();

		mNetworkFragment = new NetworkFragment();
		mSerialFragment = new SerialFragment();
		mCameraFragment = new CameraFragment();
		mBluetoothFragment = new BluetoothFragment();
		mBluetoothFragment.setMode(RECORDING_MODE);

		// TODO: remove
		onSerialConnected();

		transaction.add(R.id.fragment_container, mBluetoothFragment);
		transaction.add(R.id.fragment_container, mNetworkFragment);
		transaction.add(R.id.fragment_container, mSerialFragment);
		transaction.add(R.id.fragment_container, mCameraFragment);

		transaction.commit();

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
		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			binder.bindSerial(mSerialFragment);
			binder.bindNetwork(mNetworkFragment);
			binder.bindBluetooth(mBluetoothFragment);
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
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
		mNavigationFragment = (NavigationFragment) getFragmentManager()
				.findFragmentByTag(NAVIGATION_FRAGMENT);
		if (mNavigationFragment == null) {
			mNavigationFragment = new NavigationFragment();
			getFragmentManager()
					.beginTransaction()
					.add(R.id.fragment_container, mNavigationFragment,
							NAVIGATION_FRAGMENT).commit();
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
			Toast.makeText(MainActivity.this, "Recording route",
					Toast.LENGTH_SHORT).show();
			mNavigationFragment.recordRoute(mCameraFragment);
		}
	}

	@Override
	public boolean recordMessageReceived(final boolean torecord) {
		if (mNavigationFragment != null) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (torecord) {
						onRecordRoute();
					} else {
						if (mNavigationFragment != null) {
							mNavigationFragment.stopRecordingRoute();
						}
					}
				}
			});
			return true;
		}
		return false;
	}

	@Override
	public void navigationMessageReceived() {
		mNavigationFragment.beginNavigationMostRecentRoute();
	}

	@Override
	public void startBluetoothThread(BluetoothSocket socket, String deviceName) {
		mService.startBluetoothThread(socket, deviceName);
	}

	@Override
	public boolean changeRecordStateFromBluetooth(boolean b) {
		return recordMessageReceived(b);
	}

	@Override
	public boolean changeNavigateStateFromBluetooth(boolean navigate) {
		return false;
	}

	@Override
	public boolean changeToNavigationModeFromBluetooth(boolean b) {
		if (mNavigationFragment == null) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(MainActivity.this,
							"Please connect to Arduino", LENGTH_SHORT).show();
				}
			});
			return false;
		}
		return mNavigationFragment.beginNavigationMostRecentRoute();
	}

	@Override
	public boolean changeToRecordingModeFromBluetooth(boolean b) {
		return false;
	}
}
