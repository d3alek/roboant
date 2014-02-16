package uk.ac.ed.insectlab.ant;

import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class ManualControlFragment extends CardFragment {
	private TextView mLeftSpeedIndicator;
	private TextView mRightSpeedIndicator;
	private SeekBar mLeftSeek;
	private SeekBar mRightSeek;

	interface ManualControlListener {

		void setManualSpeeds(int progress, int progress2);

	}

	ManualControlListener mManualControlListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mManualControlListener = (ManualControlListener)activity;
		} catch(ClassCastException e) {
			e.printStackTrace();
			throw new RuntimeException("Host activity does not implement listener");
		}
	}

	@Override
	public View onCreateCardView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
					mManualControlListener.setManualSpeeds(mLeftSeek.getProgress(), mRightSeek.getProgress());
				}
			}
		};
		mLeftSeek.setOnSeekBarChangeListener(seeker);
		mRightSeek.setOnSeekBarChangeListener(seeker);

		setLabel("Manual Control");

		return view;
	}

	public void setSpeeds(final int left, final int right) {
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

}
