package uk.ac.ed.insectlab.ant;

import java.util.regex.Pattern;

import uk.ac.ed.insectlab.ant.ManualControlFragment.ManualControlListener;
import uk.ac.ed.insectlab.ant.NetworkFragment.NetworkListener;
import uk.ac.ed.insectlab.ant.SerialFragment.SerialListener;
import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends Activity implements NetworkListener, SerialListener, ManualControlListener {

	private static final int CAMERA_NUMBER = 1;

	private final String TAG = MainActivity.class.getSimpleName();

	Pattern mPattern = Pattern.compile("(l|r)\\s(-?\\d+)");

	private WakeLock mWakeLock;

	private ManualControlFragment mManualControlFragment;

	private NetworkFragment mNetworkFragment;

	private SerialFragment mSerialFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		//		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		//		this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
		//		this.mWakeLock.acquire();

		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();

		mNetworkFragment = new NetworkFragment();
		mSerialFragment = new SerialFragment();
		

		transaction.add(R.id.fragment_container, mNetworkFragment);
		transaction.add(R.id.fragment_container, mSerialFragment);

		transaction.commit();
	}
	
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
	}

	@Override
	public void speedReceivedFromNetwork(int left, int right) {
		if (mSerialFragment != null) {
			mSerialFragment.setSpeeds(left, right);
		}
	}

	@Override
	public void serverConnected() {
		Log.i(TAG, "Server connected");
	}

	@Override
	public void serverDisconnected() {
		Log.i(TAG, "Server disconnected");
	}

	@Override
	public void deviceSpeedsReceived(int left, int right) {
		Log.i(TAG, "Speeds received " + left + " " + right);
		if (mManualControlFragment != null) {
			mManualControlFragment.setSpeeds(left, right);
		}
	}

	@Override
	public void onSerialConnected() {
		mManualControlFragment = new ManualControlFragment();
		getFragmentManager().beginTransaction()
		.add(R.id.fragment_container, mManualControlFragment).commit();
	}

	@Override
	public void onSerialDisconnected() {
		if (mManualControlFragment != null) {
			getFragmentManager().beginTransaction()
			.remove(mManualControlFragment).commit();
		}
	}

	@Override
	public void setManualSpeeds(int left, int right) {
		Log.i(TAG, "manualSpeeds " + left + " " + right);
		if (mSerialFragment != null) {
			mSerialFragment.setSpeeds(left, right);
		}
	}

}
