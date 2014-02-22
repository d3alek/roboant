package uk.ac.ed.insectlab.ant;

import uk.ac.ed.insectlab.ant.CameraFragment.CameraListener;
import uk.ac.ed.insectlab.ant.ManualControlFragment.ManualControlListener;
import uk.ac.ed.insectlab.ant.NetworkFragment.NetworkFragmentListener;
import uk.ac.ed.insectlab.ant.SerialFragment.SerialFragmentListener;
import uk.ac.ed.insectlab.ant.service.RoboantService;
import uk.ac.ed.insectlab.ant.service.RoboantService.LocalBinder;
import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

public class MainActivity extends Activity implements NetworkFragmentListener, SerialFragmentListener,
ManualControlListener, CameraListener {

	private static final int CAMERA_NUMBER = 1;

	private final String TAG = MainActivity.class.getSimpleName();

	private WakeLock mWakeLock;

	private ManualControlFragment mManualControlFragment;

	private NetworkFragment mNetworkFragment;

	private SerialFragment mSerialFragment;

	private CameraFragment mCameraFragment;

	private ArduinoZumoControl mRoboantControl;

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
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder binder = (LocalBinder) service;
			binder.bindSerial(mSerialFragment);
			binder.bindNetwork(mNetworkFragment);
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};

	private boolean mBound;

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
	public void setManualSpeeds(int left, int right) {
		Log.i(TAG, "manualSpeeds " + left + " " + right);
		if (mSerialFragment != null) {
			mSerialFragment.setSpeeds(left, right);
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
	public void deviceSpeedsReceived(int left, int right) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSerialConnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSerialDisconnected() {
		// TODO Auto-generated method stub
		
	}

}
