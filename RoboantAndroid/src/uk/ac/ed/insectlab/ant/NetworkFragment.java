package uk.ac.ed.insectlab.ant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ed.insectlab.ant.service.RoboantService.NetworkBond;
import uk.ac.ed.insectlab.ant.service.TcpClient;
import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.content.Intent;
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

public class NetworkFragment extends CardFragment implements NetworkBond {
	protected static final String TAG = NetworkFragment.class.getSimpleName();
	protected static final int MESSAGE_CAMERA_FREE = 0;
	private static Handler mHandler;
	private EditText mServerIP;
	private EditText mServerPort;



	interface NetworkFragmentListener {

		void freeCamera(Handler mHandler, int messageCameraFree);

		void recordMessageReceived(boolean torecord);

		void navigationMessageReceived();

	}

	private NetworkFragmentListener mNetworkListener;
	private TcpClient mTcpClient;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mNetworkListener = (NetworkFragmentListener)activity;
		} catch(ClassCastException e) {
			e.printStackTrace();
			throw new RuntimeException("Host activity does not implement listener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
			GLOBAL.getSettings().setServerAddress(ipPort[0], Integer.parseInt(ipPort[1]));
		}
		else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}

	@Override
	public void serverConnected(TcpClient tcpClient) {
		mTcpClient = tcpClient;
		setStatus(CardStatus.OK);
	}

	@Override
	public void serverDisconnected() {
		mTcpClient = null;
		setStatus(CardStatus.LOADING);
	}



	@Override
	public void messageReceived(final String message) {
		Matcher matcher = Constants.mRecordPattern.matcher(message);

		if (matcher.find()) {
			if (matcher.group(1).equals("on")) {
				mNetworkListener.recordMessageReceived(true);
			}
			else {
				mNetworkListener.recordMessageReceived(false);
			}
		}
		else {
			matcher = Constants.mNavigationPattern.matcher(message);
			if (matcher.find()) {
				mNetworkListener.navigationMessageReceived();
			}
		}

	}

}
