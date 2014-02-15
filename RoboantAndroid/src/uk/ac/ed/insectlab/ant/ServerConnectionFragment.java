package uk.ac.ed.insectlab.ant;

import java.util.regex.Matcher;

import uk.co.ed.insectlab.ant.R;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class ServerConnectionFragment extends Fragment {
	private final String SERVER_IP = "172.20.153.210";
	private TcpClient mTcpClient;
	private Runnable mConnectorRunnable;
	private Handler mHandler;
	private EditText mServerIP;
	private EditText mServerPort;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mHandler = new Handler();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mServerIP = (EditText)findViewById(R.id.serverIP);
		mServerPort = (EditText)findViewById(R.id.serverPort);

		mServerIP.setText(SERVER_IP);
		mServerPort.setText("1234");
		postConnectorRunnable();
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	public class ConnectTask extends AsyncTask<Void, String, TcpClient> {

		@Override
		protected TcpClient doInBackground(Void... nothing) {
			if (mTcpClient != null) {
				return null;
			}
			//we create a TCPClient object and
			mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {

				@Override
				//here the messageReceived method is implemented
				public void messageReceived(String message) {
					//this method calls the onProgressUpdate
					publishProgress(message);
				}

				@Override
				public void disconnected() {
					//					Log.i(TAG, "Disconnected");
					mTcpClient = null;

					getActivity().runOnUiThread(new Runnable() {

						@Override
						public void run() {
							mServerIP.setEnabled(true);
							mServerPort.setEnabled(true);
						}
					});

					postConnectorRunnable();
				}



				@Override
				public void connected() {
					getActivity().runOnUiThread(new Runnable() {

						@Override
						public void run() {
							mServerIP.setEnabled(false);
							mServerPort.setEnabled(false);
						}
					});
					if (mAIControl != null) {
						mAIControl.setNetworkControl(mTcpClient);
					}
					handler.removeCallbacks(mConnectorRunnable);
				}
			});

			mTcpClient.run(mServerIP.getText().toString(), Integer.parseInt(mServerPort.getText().toString()), handler);
			return null;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			handleMessage(values[0]);
		}

	}

	private void postConnectorRunnable() {
		final int period = 2000;

		mConnectorRunnable = new Runnable() {

			public void run() {
				if (mTcpClient != null) {
					return;
				}
				//				Log.i(TAG, "Trying to connect to server");
				new ConnectTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				mHandler.postDelayed(this, period);
			}
		};

		mHandler.postDelayed(mConnectorRunnable, period);
	}

	private void handleMessage(String message) {
		Matcher matcher = mPattern.matcher(message);
		if (matcher.find()) {
			int speed = Integer.parseInt(matcher.group(2));
			if (mAIControl != null) {
				return;
			}
			if (mRoboAntControl == null) {
				Log.i(TAG, "mRoboAntControl is null message is " + message);
				return;
			}
			if (matcher.group(1).equals("l")) {
				mRoboAntControl.setLeftSpeed(speed);
			}
			else {
				mRoboAntControl.setRightSpeed(speed);
			}
		}
		else if (message.startsWith("cam")) {
			Log.i(TAG, "cam message!");
			if (mCamMessageRecievedAt + CAMERA_TIMEOUT > System.currentTimeMillis()) {
				Log.i(TAG, "ignoring cam message");
				return;
			}
			mCamMessageRecievedAt = System.currentTimeMillis();
		}
		else if (message.startsWith("ai")) {
			Log.i(TAG, "ai message!");
			toggleAI();

		}
		else if (message.startsWith("pic")) {
			Log.i(TAG, "pic message!");
			takePicture();
		}
		else if (message.startsWith("rec")) {
			Log.i(TAG, "rec message!");
			if (!mRecordingRoute) {
				postRecordingRunnable();
				mRecordingRoute = true;
				mRecordingText.setVisibility(View.VISIBLE);
			}
			else {
				mRecordingRoute = false;
				handler.removeCallbacks(mRecordingRunnable);
				synchronized (mRoutePictures) {
					new SaveRecordedRouteTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mRoutePictures);
				}
				mRecordingText.setVisibility(View.INVISIBLE);
			}
		}
		else if (message.startsWith("calibrate")) {
			Log.i(TAG, "Calibrate message");
			//			if (mRoboAntControl != null) {
			//				mRoboAntControl.calibrate();
			//			}

			calibrateSSD();
		}
		else if (message.startsWith("go")) {
			if (mAIControl != null) {
				mAIControl.releaseLock();
			}
		}
	}

	@Override
	public void onResume() {
		postConnectorRunnable();
		super.onResume();
	}

	@Override
	public void onPause() {
		if (mTcpClient != null) {
			mTcpClient.stopClient();
		}
		if (mConnectorRunnable != null) {
			mHandler.removeCallbacks(mConnectorRunnable);
		}
		super.onPause();
	}

}
