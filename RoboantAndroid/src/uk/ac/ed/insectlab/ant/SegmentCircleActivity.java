package uk.ac.ed.insectlab.ant;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;

import uk.ac.ed.insectlab.ant.CameraFragment.CameraListener;
import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class SegmentCircleActivity extends Activity implements CameraListener {

	protected static final String TAG = SegmentCircleActivity.class.getSimpleName();

	private CameraFragment mCameraFragment;

	private SeekBar mCircleWidthSeeker;

	private SeekBar mCircleHeightSeeker;

	private int mCircleRadMin;
	private int mCircleRadMax;

	private Button mSegmentAgain;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// after this, square region, can easily rotate, try not to modify lib
		setContentView(R.layout.activity_segment_circle);

		FragmentManager fragmentManager = getFragmentManager();

		mCameraFragment = new CameraFragment();

		fragmentManager.beginTransaction().add(R.id.fragment_container, mCameraFragment).commit();


		mCircleWidthSeeker = (SeekBar)findViewById(R.id.circleMin);
		mCircleHeightSeeker = (SeekBar)findViewById(R.id.circleMax);

		SeekBar.OnSeekBarChangeListener seekChangeListener = new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				int id = seekBar.getId();
				if (id == R.id.circleMin) {
					Log.i(TAG, "Min set to " + progress);
					mCircleRadMin = progress;
				}
				else {
					Log.i(TAG, "Max set to " + progress);
					mCircleRadMax = progress;
				}
			}
		};

		mCircleWidthSeeker.setOnSeekBarChangeListener(seekChangeListener);
		mCircleHeightSeeker.setOnSeekBarChangeListener(seekChangeListener);

		mSegmentAgain = (Button)findViewById(R.id.btn_segment_circle);

		mSegmentAgain.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mCameraFragment.segmentAgain(mCircleRadMin, mCircleRadMax);
			}
		});

		mSegmentAgain.setVisibility(View.INVISIBLE);

		mCameraFragment.setSegmenting(true);
		mCameraFragment.showSegmentButton(false);
	}

	@Override
	public void cameraViewStarted(int width, int height) {
		int min = Math.min(width, height);
		mCircleWidthSeeker.setMax(min);
		mCircleHeightSeeker.setMax(min);
	}

	@Override
	public void cameraViewStopped() {

	}

	@Override
	public void onLensFound(boolean found) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mSegmentAgain.setVisibility(View.VISIBLE);
			}
		});
		if (found) {
		}
	}


}
