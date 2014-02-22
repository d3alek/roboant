package uk.ac.ed.insectlab.ant;

import uk.ac.ed.insectlab.ant.service.RoboantService.SerialBond;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SerialFragment extends CardFragment implements SerialBond {

	private static final String TAG = SerialFragment.class.getSimpleName();

	interface SerialFragmentListener {
		void deviceSpeedsReceived(int left, int right);
		void onSerialConnected();
		void onSerialDisconnected();
	}

	private ArduinoZumoControl mRoboant;
	private SerialFragmentListener mSerialListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mSerialListener = (SerialFragmentListener)activity;
		} catch(ClassCastException e) {
			e.printStackTrace();
			throw new RuntimeException("Host activity does not implement listener");
		}
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


	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public void setSpeeds(int left, int right) {
		if (mRoboant != null) {
			mRoboant.setSpeeds(left, right);
		}
		else {
			Log.e(TAG, "setSpeeds service is null");
		}
	}

	@Override
	public void serialDisconnected() {
		mRoboant = null;
		setStatus(CardStatus.LOADING);
	}


	@Override
	public void serialConnected(ArduinoZumoControl roboantControl) {
		mRoboant = roboantControl;
		setStatus(CardStatus.OK);
	}
}
