package uk.ac.ed.insectlab.ant;

import uk.co.ed.insectlab.ant.R;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ManualControlFragment extends Fragment {
	private EditText mLeftSpeedIndicator;
	private EditText mRightSpeedIndicator;
	private SeekBar mLeftSeek;
	private SeekBar mRightSeek;

	public android.view.View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mLeftSpeedIndicator = (EditText)findViewById(R.id.leftValue);
		mRightSpeedIndicator = (EditText)findViewById(R.id.rightValue);
		
		mLeftSeek = (SeekBar)findViewById(R.id.left);
		mRightSeek = (SeekBar)findViewById(R.id.right);
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
				switch (seekBar.getId()) {
				case R.id.left:
					//					mRoboAntControl.setLeftSpeed(progress);
					//					Log.i(TAG, "Setting left speed to " + progress);
					break;
				case R.id.right:
					//					mRoboAntControl.setRightSpeed(progress);
					//					Log.i(TAG, "Setting right speed to " + progress);
					break;

				default:
					break;
				}

			}


		};
		mLeftSeek.setOnSeekBarChangeListener(seeker);
		mRightSeek.setOnSeekBarChangeListener(seeker);
	};

}
