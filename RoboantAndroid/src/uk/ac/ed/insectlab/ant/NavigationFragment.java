package uk.ac.ed.insectlab.ant;

import java.util.LinkedList;
import java.util.List;

import uk.co.ed.insectlab.ant.R;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class NavigationFragment extends CardFragment {

	private static final long RECORD_EVERY_MS = 100;
	protected static final String TAG = NavigationFragment.class.getSimpleName();
	private Button mGoButton;
	private View mRouteInfoView;
	private Button mSelectRouteButton;
	private TextView mRouteLength;
	private Button mClearRouteButton;
	private CameraFragment mCamera;
	private Handler mHandler;
	private LinkedList<Bitmap> mRouteRecordingPictures;
	private boolean mRecordingRoute;
	private boolean mToStopRecording;
	private Button mStopRecording;
	
	@Override
	public void onStart() {
		super.onStart();
		mHandler = new Handler();
	}

	@Override
	public View onCreateCardView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_navigation, container, false);
		mSelectRouteButton = (Button)v.findViewById(R.id.btn_select_route);
		mSelectRouteButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				showRouteSelectionDialog();
			}
		});

		mClearRouteButton = (Button)v.findViewById(R.id.btn_clear_route);

		mClearRouteButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				GLOBAL.ROUTE = null;
				mSelectRouteButton.setVisibility(View.VISIBLE);
				mGoButton.setVisibility(View.GONE);
				mRouteInfoView.setVisibility(View.GONE);
			}
		});

		mRouteInfoView = v.findViewById(R.id.route_info);
		mGoButton = (Button)v.findViewById(R.id.btn_go);

		mGoButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(getActivity(), NavigationActivity.class);
				startActivity(i);
			}
		});

		mStopRecording = (Button)v.findViewById(R.id.btn_stop_recording);

		mStopRecording.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				stopRecordingRoute();
			}
		});

		mStopRecording.setVisibility(View.GONE);

		mRouteLength = (TextView)v.findViewById(R.id.route_length);

		mGoButton.setVisibility(View.GONE);
		mRouteInfoView.setVisibility(View.GONE);
		setLabel("Navigation");
		return v;
	}

	private void showRouteSelectionDialog() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		DialogFragment newFragment = new RouteSelectionDialogFragment();
		newFragment.show(ft, "dialog");
	}

	public void onRouteSelected(List<Bitmap> bitmaps) {
		GLOBAL.ROUTE = bitmaps;
		mRouteLength.setText(bitmaps.size() + " ");
		mSelectRouteButton.setVisibility(View.GONE);
		mRouteInfoView.setVisibility(View.VISIBLE);
		mGoButton.setVisibility(View.VISIBLE);
	}

	public void recordRoute(CameraFragment camera) {
		mCamera = camera;
		mRouteRecordingPictures = new LinkedList<Bitmap>();
		mRecordingRoute = true;
		mGoButton.setVisibility(View.INVISIBLE);
		mRouteInfoView.setVisibility(View.INVISIBLE);
		mStopRecording.setVisibility(View.VISIBLE);
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				synchronized (mRouteRecordingPictures) {
					if (mToStopRecording) {
						Log.i(TAG, "Stopping recording runnable");
						mToStopRecording = false;
						return;
					}
					mRouteRecordingPictures.add(mCamera.getPicture());
					Log.i(TAG, mRouteRecordingPictures.size() + " route pictures taken");
					mHandler.postDelayed(this, RECORD_EVERY_MS);
				}
			}
		}, RECORD_EVERY_MS);
	}

	public void stopRecordingRoute() {
		synchronized (mRouteRecordingPictures) {
			if (mRecordingRoute) {
				mRecordingRoute = false;
				mToStopRecording = true;
				mStopRecording.setVisibility(View.GONE);
				onRouteSelected(mRouteRecordingPictures);
				new SaveRecordedRouteTask(getActivity()).execute(mRouteRecordingPictures);
			}
		}
	}

	public void beginNavigationMostRecentRoute() {
		if (GLOBAL.ROUTE == null) {
			String filePath = GLOBAL.getSettings().getMostRecentRouteDirPath();
			if (filePath == null) {
				Toast.makeText(getActivity(), "No route selected recently", Toast.LENGTH_SHORT).show();
				return;
			}
			RouteSelectionDialogFragment.RouteSelectedListener listener = new RouteSelectionDialogFragment.RouteSelectedListener() {
				
				@Override
				public void onRouteSelected(List<Bitmap> bitmap) {
					GLOBAL.ROUTE = bitmap;
					Intent i = new Intent(getActivity(), NavigationActivity.class);
					startActivity(i);
				}
				
				@Override
				public void onRecordRoute() {
					// TODO Auto-generated method stub
					
				}
			};
			new LoadRecordedRouteTask(listener).execute(filePath);
			return;
		}
		Intent i = new Intent(getActivity(), NavigationActivity.class);
		startActivity(i);
	}

}
