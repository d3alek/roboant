package uk.ac.ed.insectlab.ant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class NetworkFragment extends CardFragment {
	protected static final long CONNECT_ATTEMPT_PERIOD = 2000;
	protected static final String TAG = NetworkFragment.class.getSimpleName();
	protected static final int MESSAGE_CAMERA_FREE = 0;
	private static TcpClient mTcpClient;
	private static Handler mHandler;
	private EditText mServerIP;
	private EditText mServerPort;
	Pattern mPattern = Pattern.compile("l(-?\\d+)r(-?\\d+)");
	NetworkListener mNetworkListener;
	private static ConnectTask mConnectTask;
	static class ConnectorRunnable implements Runnable {
		private NetworkFragment mFragment;
		public ConnectorRunnable(NetworkFragment fragment) {
			mFragment = fragment;
		}
		public void run() {
			if (mConnectTask == null) {
				Log.i(TAG, "Trying to connect to server");
				mConnectTask = mFragment.new ConnectTask();
				mConnectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			mHandler.postDelayed(this, CONNECT_ATTEMPT_PERIOD);
		}
	}
	private static Runnable mConnectorRunnable;

	interface NetworkListener {
		void speedReceivedFromNetwork(int left, int right);
		void serverConnected();
		void serverDisconnected();

		void freeCamera(Handler handler, int what);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mNetworkListener = (NetworkListener)activity;
		} catch(ClassCastException e) {
			e.printStackTrace();
			throw new RuntimeException("Host activity does not implement listener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mConnectorRunnable = new ConnectorRunnable(this);
		mHandler = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
				case MESSAGE_CAMERA_FREE:
					IntentIntegrator integrator = new IntentIntegrator(NetworkFragment.this);
					integrator.initiateScan();
					break;
				default: Log.i(TAG, "Unhandled message " + msg);
				}

				return false;
			}
		}); 
	}

	@Override
	public View onCreateCardView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_network, container, false);

		mServerIP = (EditText)view.findViewById(R.id.serverIP);
		mServerPort = (EditText)view.findViewById(R.id.serverPort);

		mServerIP.setText(GLOBAL.getSettings().getServerIP());
		mServerPort.setText(GLOBAL.getSettings().getServerPort() + "");

		Button scanQR = (Button)view.findViewById(R.id.btn_scan_qr);
		scanQR.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mNetworkListener.freeCamera(mHandler, MESSAGE_CAMERA_FREE);

			}
		});

		setLabel("Network");

		return view;
	}


	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
			String serverAddr = scanResult.getContents();
			if (serverAddr == null) {
				return;
			}
			String[] ipPort = serverAddr.split(":");
			mServerIP.setText(ipPort[0]);
			mServerPort.setText(ipPort[1]);
		}
		else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}

	public class ConnectTask extends AsyncTask<Void, String, TcpClient> {

		@Override
		protected void onPreExecute() {
			NetworkFragment.this.setStatus(CardStatus.LOADING);
			Log.i(TAG, "Starting ConnectTask");
			super.onPreExecute();
		}

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
					getActivity().runOnUiThread(new Runnable() {

						@Override
						public void run() {
							NetworkFragment.this.setStatus(CardStatus.NONE);
							mServerIP.setEnabled(true);
							mServerPort.setEnabled(true);
						}
					});
					mNetworkListener.serverDisconnected();
				}

				@Override
				public void connected() {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							NetworkFragment.this.setStatus(CardStatus.OK);
							mServerIP.setEnabled(false);
							mServerPort.setEnabled(false);
						}
					});
					GLOBAL.getSettings().setServerAddress(mServerIP.getText().toString(),
							mServerPort.getText().toString());
					mNetworkListener.serverConnected();
				}
			});

			mTcpClient.run(mServerIP.getText().toString(), Integer.parseInt(mServerPort.getText().toString()), mHandler);
			return null;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			handleMessage(values[0]);
		}

		@Override
		protected void onPostExecute(TcpClient result) {
			super.onPostExecute(result);
			mTcpClient = null;
			mConnectTask = null;
		}

	}

	private void postConnectorRunnable() {
		Log.i(TAG, "postConnectorRunnable");
		mHandler.postDelayed(mConnectorRunnable, CONNECT_ATTEMPT_PERIOD);
	}

	private void handleMessage(String message) {
		Matcher matcher = mPattern.matcher(message);
		if (matcher.find()) {
			int speedL = Integer.parseInt(matcher.group(1));
			int speedR = Integer.parseInt(matcher.group(2));

			mNetworkListener.speedReceivedFromNetwork(speedL, speedR);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		postConnectorRunnable();
	}

	@Override
	public void onPause() {
		if (mTcpClient != null) {
			mTcpClient.stopClient();
			mTcpClient = null;
		}
		if (mConnectorRunnable != null) {
			mHandler.removeCallbacks(mConnectorRunnable);
		}
		if (mConnectTask != null) {
			mConnectTask.cancel(true);
			mConnectTask = null;
		}
		super.onPause();
	}

}
