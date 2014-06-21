package uk.ac.ed.insectlab.ant;

import java.util.regex.Matcher;

import uk.ac.ed.insectlab.ant.CameraFragment.CameraListener;
import uk.ac.ed.insectlab.ant.SwayingHomingFragment.NavigationListener;
import uk.ac.ed.insectlab.ant.service.BluetoothThread;
import uk.ac.ed.insectlab.ant.service.RoboantService;
import uk.ac.ed.insectlab.ant.service.RoboantService.BluetoothBond;
import uk.ac.ed.insectlab.ant.service.RoboantService.LocalBinder;
import uk.ac.ed.insectlab.ant.service.RoboantService.NetworkBond;
import uk.ac.ed.insectlab.ant.service.RoboantService.SerialBond;
import uk.ac.ed.insectlab.ant.service.TcpClient;
import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

public class NavigationActivity extends Activity implements SerialBond, NetworkBond, CameraListener,
NavigationListener, BluetoothBond{
	private static final String TAG = NavigationActivity.class.getSimpleName();
	private boolean mBound;
	private CameraFragment mCameraFragment;
	private RoboantService mService;
	Handler mHandler;
	private LookAroundHomingFragment mLookAroundHomingFragment;

	private BluetoothThread mBluetoothThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_navigation);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();

		mCameraFragment = new CameraFragment();
		//		mLookAroundHomingFragment = new SwayingHomingFragment();
		mLookAroundHomingFragment = new LookAroundHomingFragment();

		transaction.add(R.id.fragment_container, mCameraFragment);
		transaction.add(R.id.arrow_container, mLookAroundHomingFragment);

		transaction.commit();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mHandler = new Handler();
		Intent intent = new Intent(this, RoboantService.class);
		startService(intent);
		bindService(intent, mConnection, Context.BIND_ABOVE_CLIENT);

		if (GLOBAL.ROUTE != null && GLOBAL.ROUTE.size() > 0) {
			Bitmap routeSample = GLOBAL.ROUTE.get(0);
			mCameraFragment.setImagePixelsNum(routeSample.getHeight()*routeSample.getWidth());
			mCameraFragment.fixPixelSize();
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {


		@Override
		public void onServiceConnected(ComponentName className,
				IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			binder.bindSerial(NavigationActivity.this);
			binder.bindNetwork(NavigationActivity.this);
			binder.bindBluetooth(NavigationActivity.this);
			mService = binder.getService();
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};

	@Override
	protected void onStop() {
		super.onStop();
		if (mBound) {
			unbindService(mConnection);
		}
	}


	@Override
	public void serverConnected(TcpClient tcpClient) {
		mLookAroundHomingFragment.setNetwork(tcpClient);
	}

	@Override
	public void serverDisconnected() {
		mLookAroundHomingFragment.setNetwork(null);
	}

	@Override
	public void serialDisconnected() {
		mLookAroundHomingFragment.setSerial(null);
	}

	@Override
	public void serialConnected(ArduinoZumoControl roboantControl) {
		mLookAroundHomingFragment.setSerial(roboantControl);
	}

	@Override
	public void serialHeartbeat(int left, int right) {

	}

	@Override
	public void cameraViewStarted(int width, int height) {
		mLookAroundHomingFragment.setCamera(mCameraFragment);
	}

	@Override
	public void cameraViewStopped() {

	}

	@Override
	public void onLensFound(boolean b) {

	}

	@Override
	public void messageReceived(final String message) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Matcher matcher = Constants.mNavigationPattern.matcher(message);

				if (matcher.find()) {
					mLookAroundHomingFragment.toggleNavigation();
				}
			}
		});

	}

	@Override
	public void onNavigationStarted() {
		mService.setRemoteControl(false);
	}

	@Override
	public void onNavigationStopped() {
		mService.setRemoteControl(true);
	}

	@Override
	public void bluetoothConnected(BluetoothThread bluetoothThread) {
		Log.i(TAG, "bluetoothConnected " + bluetoothThread);
		mBluetoothThread = bluetoothThread;
		mLookAroundHomingFragment.setBluetooth(mBluetoothThread);
	}

	@Override
	public void bluetoothDisconnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bluetoothMessageReceived(String message) {
		mLookAroundHomingFragment.onBluetoothMessageReceived(message);
	}

}
