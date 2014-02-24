package uk.ac.ed.insectlab.ant;

import uk.ac.ed.insectlab.ant.CameraFragment.CameraListener;
import uk.ac.ed.insectlab.ant.service.RoboantService;
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
import android.os.Bundle;
import android.os.IBinder;

public class NavigationActivity extends Activity implements SerialBond, NetworkBond, CameraListener {
	private boolean mBound;
	private CameraFragment mCameraFragment;
	private SwayingHomingFragment mSwayingHomingFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		
		mCameraFragment = new CameraFragment();
		mSwayingHomingFragment = new SwayingHomingFragment();
		
		transaction.add(R.id.fragment_container, mCameraFragment);
		transaction.add(R.id.fragment_container, mSwayingHomingFragment);
		
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
			LocalBinder binder = (LocalBinder) service;
			binder.bindSerial(NavigationActivity.this);
			binder.bindNetwork(NavigationActivity.this);
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
		mSwayingHomingFragment.setNetwork(tcpClient);
	}

	@Override
	public void serverDisconnected() {
		mSwayingHomingFragment.setNetwork(null);
	}

	@Override
	public void serialDisconnected() {
		mSwayingHomingFragment.setSerial(null);
	}

	@Override
	public void serialConnected(ArduinoZumoControl roboantControl) {
		mSwayingHomingFragment.setSerial(roboantControl);
	}

	@Override
	public void serialHeartbeat(int left, int right) {

	}

	@Override
	public void cameraViewStarted(int width, int height) {
		mSwayingHomingFragment.setCamera(mCameraFragment);
	}

	@Override
	public void cameraViewStopped() {
		
	}

	@Override
	public void onLensFound(boolean b) {
		
	}
}
