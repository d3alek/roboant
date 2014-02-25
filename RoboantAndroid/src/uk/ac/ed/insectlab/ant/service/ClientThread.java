package uk.ac.ed.insectlab.ant.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ed.insectlab.ant.GLOBAL;
import uk.ac.ed.insectlab.ant.Settings;
import uk.ac.ed.insectlab.ant.service.RoboantService.NetworkBond;
import android.app.Service;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

public class ClientThread extends Thread {

	private static TcpClient mTcpClient;
	protected static final long CONNECT_ATTEMPT_PERIOD = 2000;
	private static final String TAG = ClientThread.class.getSimpleName();
	private static Pattern mPattern = Pattern.compile("l(-?\\d+)r(-?\\d+)");

	private enum State {
		CONNECTED, DISCONNECTED
	}

	interface NetworkListener {
		void speedReceivedFromNetwork(int left, int right);
		void serverConnected();
		void serverDisconnected();
	}

	private static State mState;

	private static NetworkListener mNetworkListener;
	private static Settings mSettings;
	private static NetworkBond mListener;
	private boolean mRunning = true;

	public ClientThread(Service service) {
		mState = State.DISCONNECTED;
		mNetworkListener = (NetworkListener) service;
		mSettings = GLOBAL.getSettings();
	}

	private static TcpClient.OnMessageReceived mOnMessageReceived = new TcpClient.OnMessageReceived() {
		@Override
		public void messageReceived(String message) {
			Log.i(TAG, "messageReceived " + message);
			Matcher matcher = mPattern.matcher(message);
			if (matcher.find()) {
				int speedL = Integer.parseInt(matcher.group(1));
				int speedR = Integer.parseInt(matcher.group(2));
				mNetworkListener.speedReceivedFromNetwork(speedL, speedR);
			}
			else {
				Log.i(TAG, "sending to mListener" + mListener);
				mListener.messageReceived(message);
				Log.i(TAG, "sent to mListener");
			}
		}

		@Override
		public void disconnected() {
			mState = State.DISCONNECTED;
			mNetworkListener.serverDisconnected();
			mListener.serverDisconnected();
		}

		@Override
		public void connected() {
			mState = State.CONNECTED;
			mNetworkListener.serverConnected();
			mListener.serverConnected(mTcpClient);
		}
	};

	@Override
	public void run() {
		HandlerThread thread = new HandlerThread("ClientThreadHandler");
		thread.start();
		Handler handler = new Handler(thread.getLooper());

		while (true) {
			if (!mRunning) {
				break;
			}
			mTcpClient = new TcpClient(mOnMessageReceived);
			mTcpClient.run(mSettings.getServerIP(), mSettings.getServerPort(), handler);

			Log.i(TAG, "Disconnected in loop");

			if (!mRunning) {
				break;
			}

			try {
				Thread.sleep(CONNECT_ATTEMPT_PERIOD);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public void setBond(NetworkBond networkBond) {
		mListener = networkBond;
		if (mState == State.CONNECTED) {
			mListener.serverConnected(mTcpClient);
		}
		else {
			mListener.serverDisconnected();
		}
	}

	public Spanned getDescription() {
		String str = "<b>Client</b> ";
		String status;
		if (mState == State.CONNECTED) {
			status = "connected";
		}
		else {
			status = "disconnected";
		}
		return Html.fromHtml(str + status);
	}

	public void setRunning(boolean running) {
		mRunning = running;
		if (!running) {
			mTcpClient.stopClient();
		}
	}

}
