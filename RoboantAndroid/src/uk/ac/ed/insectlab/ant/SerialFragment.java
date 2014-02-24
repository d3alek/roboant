package uk.ac.ed.insectlab.ant;

import uk.ac.ed.insectlab.ant.service.RoboantService.SerialBond;
import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SerialFragment extends CardFragment implements SerialBond {

	private static final String TAG = SerialFragment.class.getSimpleName();

	interface SerialFragmentListener {
		void onSerialConnected();
		void onSerialDisconnected();
	}

	private ArduinoZumoControl mRoboant;
	private SerialFragmentListener mSerialListener;
	private TextView mLeftSpeedIndicator;
	private TextView mRightSpeedIndicator;
	private SeekBar mLeftSeek;
	private SeekBar mRightSeek;

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
	public View onCreateCardView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_manual_control, container, false);

		mLeftSpeedIndicator = (TextView)view.findViewById(R.id.leftValue);
		mRightSpeedIndicator = (TextView)view.findViewById(R.id.rightValue);

		mLeftSeek = (SeekBar)view.findViewById(R.id.left);
		mRightSeek = (SeekBar)view.findViewById(R.id.right);
		OnSeekBarChangeListener seeker = new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					setSpeeds(mLeftSeek.getProgress(), mRightSeek.getProgress());
				}
			}
		};
		mLeftSeek.setOnSeekBarChangeListener(seeker);
		mRightSeek.setOnSeekBarChangeListener(seeker);

		setLabel("Serial");

		return view;
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
			displaySpeeds(left, right);
		}
		else {
			Log.e(TAG, "setSpeeds service is null");
		}
	}

	@Override
	public void serialDisconnected() {
		mRoboant = null;
		setStatus(CardStatus.LOADING);
		mSerialListener.onSerialDisconnected();
	}


	@Override
	public void serialConnected(ArduinoZumoControl roboantControl) {
		mRoboant = roboantControl;
		setStatus(CardStatus.OK);
		mSerialListener.onSerialConnected();
	}

	public void displaySpeeds(final int left, final int right) {
		if (getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mLeftSpeedIndicator.setText(left + " ");
					mRightSpeedIndicator.setText(right + " ");

					mLeftSeek.setProgress(left);
					mRightSeek.setProgress(right);
				}
			});
		}

	};
	
	@Override
	public void setStatus(CardStatus status) {
		super.setStatus(status);
		
		final boolean enabled = (status == CardStatus.OK);
		
		if (getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mLeftSpeedIndicator.setEnabled(enabled);
					mRightSpeedIndicator.setEnabled(enabled);
					mLeftSeek.setEnabled(enabled);
					mRightSeek.setEnabled(enabled);
				}
			});
		}
		
	}


	@Override
	public void serialHeartbeat(int left, int right) {
		displaySpeeds(left, right);
	}
}
