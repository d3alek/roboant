package uk.ac.ed.insectlab.ant;

import java.util.LinkedList;
import java.util.List;

import uk.ac.ed.insectlab.ant.RouteSelectionDialogFragment.RouteSelectedListener;
import uk.co.ed.insectlab.ant.R;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AIFragment extends Fragment implements RouteSelectedListener {

	private TextView mAIMessage;
	private AIControlTask mAIControl;
	private boolean mRecordingRoute;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mTrackProgressBar = (ProgressBar)findViewById(R.id.track_proress);
		mTrackProgressBar.setVisibility(View.GONE);
		mAIMessage = (TextView)findViewById(R.id.ai_message);
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	private void showRouteSelectionDialog() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		// Create and show the dialog.
		DialogFragment newFragment = new RouteSelectionDialogFragment();
		newFragment.show(ft, "dialog");
	}

	private void toggleAI() {
		if (mAIControl != null) {
			mAIControl.stop();
			mAIMessage.setText("");
			mAIControl = null;
		}
		else {
			TextView currentStepNum = (TextView)findViewById(R.id.current_step_num);
			TextView stepTowardsNum = (TextView)findViewById(R.id.step_towards_num);
			if (mTcpClient == null) {
				NetworkControl dummy = new NetworkControl() {
					@Override
					public void sendPicture(RoboPicture roboPicture) {
						Log.i(TAG, "Ignore sendPicture " + roboPicture.pictureNum);
					}

					@Override
					public void sendMessage(String message) {
						Log.i(TAG, "Ignore sendMessage " + message);
					}
				};
				mAIControl = new AIControlTask(mCamera, dummy, mAIMessage, currentStepNum, stepTowardsNum, mTrackProgressBar, mRoutePictures);
			}
			else {
				mAIControl = new AIControlTask(mCamera, mTcpClient, mAIMessage,  currentStepNum, stepTowardsNum, mTrackProgressBar, mRoutePictures);
			}
			mAIControl.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mRoboAntControl);
		}

	}
	private void takeAIPicture() {
		mAIControl.getHandler().post(new Runnable() {

			@Override
			public void run() {
				mAIControl.stepTowards();
			}
		});

	}
	private void calibrateSSD() {
		if (mAIControl != null) {
			mAIControl.calibrateSSD();
		}

	}

	private long ROUTE_PICTURES_DELAY = 100;
	List<Bitmap> mRoutePictures = new LinkedList<Bitmap>();
	private Runnable mRecordingRunnable;
	private void postRecordingRunnable() {
		for (Bitmap bmp : mRoutePictures) {
			bmp.recycle();
		}
		mRoutePictures.clear();

		handler.post(new Runnable() {

			@Override
			public void run() {
				synchronized (mRoutePictures) {
					if (!mRecordingRoute) {
						return;
					}

					mRecordingRunnable = this;
					mRoutePictures.add(getCameraPicture());
					//					mCamera.getPicture(new OpenCVCamera.PictureListener() {
					//						@Override
					//						public void pictureReceived(Bitmap picture) {
					//							mRoutePictures.add(picture);
					//						}
					//					});
					try {
						handler.postDelayed(this, ROUTE_PICTURES_DELAY);
					}
					catch (Exception e) {
						e.printStackTrace();
						handler.postDelayed(this, 100);
					}
				}
			}
		});
	}
	
	@Override
	public void onRouteSelected(List<Bitmap> bitmap) {
		mRoutePictures = bitmap;
	}
	
	private void showRouteSelectionDialog() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		// Create and show the dialog.
		DialogFragment newFragment = new RouteSelectionDialogFragment();
		newFragment.show(ft, "dialog");
	}
}
