package uk.ac.ed.insectlab.ant.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;

import uk.ac.ed.insectlab.ant.Constants;
import uk.ac.ed.insectlab.ant.NetworkControl;
import uk.ac.ed.insectlab.ant.RoboPicture;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class TcpClient implements NetworkControl {

	private static final int KEEP_ALIVE_INTERVAL = 10000;
	private static final String TAG = "TcpClient";
	private String mServerMessage;
	private OnMessageReceived mMessageListener = null;
	private boolean mRun = false;
	private PrintWriter mBufferOut;
	private BufferedReader mBufferIn;
	private long mLastKeepAlive;
	private boolean mStopped;
	private OutputStream mOutputStream;
	private Handler mHandler;

	LinkedList<Runnable> mSendingQueue = new LinkedList<Runnable>();
	private boolean mSendingBlock;


	private Object mSendingLock = new Object();
	private boolean mSendingPicture;
	private boolean mCrashed;

	public TcpClient(OnMessageReceived listener) {
		mMessageListener = listener;
	}

	public void sendMessage(final String message) {
		synchronized (mSendingLock) {

			if (!mSendingPicture && mSendingQueue.isEmpty()) {

				if (mBufferOut != null && !mBufferOut.checkError()) {
					mBufferOut.println(message);
					mBufferOut.flush();
				}
			}
			else {
				Log.i(TAG, "Delaying sending message " + message);
				mSendingQueue.add(new Runnable() {
					@Override
					public void run() {
						_sendMessage(message);
					}
				});
			}

		}
	}

	public void stopClient() {

		_sendMessage(Constants.CLOSED_CONNECTION);
		mStopped = true;

		mRun = false;

		if (mBufferOut != null) {
			mBufferOut.flush();
			mBufferOut.close();
		}

		mBufferIn = null;
		mBufferOut = null;
		mServerMessage = null;
	}

	public void run(String serverip, int serverport, Handler handler) {
		mStopped = false;
		mRun = true;
		mHandler = handler;

		Runnable executeQueueFront = new Runnable() {


			@Override
			public void run() {
				synchronized (mSendingLock) {
					if (mSendingBlock) {
						Log.i(TAG, "Sending blocked!");
						return;
					}
					if (!mSendingPicture && !mSendingQueue.isEmpty()) {
						mSendingQueue.pop().run();
					}
				}

			}
		};

		try {
			InetAddress serverAddr = InetAddress.getByName(serverip);

			Log.i(TAG, "Creating socket");

			Socket socket = new Socket(serverAddr, serverport);
			socket.setSoTimeout(1000);

			try {
				mMessageListener.connected();
				mLastKeepAlive = System.currentTimeMillis();

				mOutputStream = socket.getOutputStream();
				mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mOutputStream)), true);

				mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				_sendMessage(Constants.LOGIN_NAME);

				while (mRun) {
					executeQueueFront.run();

					if (mLastKeepAlive + KEEP_ALIVE_INTERVAL < System.currentTimeMillis()) {
						Log.i(TAG, "keep-alive");
						sendMessage("keep-alive");
						mLastKeepAlive = System.currentTimeMillis();
					}

					if (!socket.isConnected()) {
						Log.i(TAG, "Socket not connected");
						mRun = false;
						continue;
					}
					if (socket.isOutputShutdown()) {
						Log.i(TAG, "Socket output shutdown");
						mRun = false;
						continue;
					}
					if (socket.isInputShutdown()) {
						Log.i(TAG, "Socket input shutdown");
						mRun = false;
						continue;
					}

					try {
						mServerMessage = mBufferIn.readLine();
						if (mServerMessage == null) {
							Log.i(TAG, "Null message from server. Closing connection");
							mRun = false;
							continue;
						}
					} catch (SocketTimeoutException e) {
						Log.i(TAG, "read timeout");
						mServerMessage = null;
					}

					if (mServerMessage != null && mMessageListener != null) {
						mMessageListener.messageReceived(mServerMessage);
					}
					
				}


			} catch (IOException e) {
				
				mCrashed = true;

				if (!mStopped) {
					mMessageListener.disconnected();
				}

			} finally {
				socket.close();
				if (!mCrashed && !mStopped) {
					mMessageListener.disconnected();
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public interface OnMessageReceived {
		public void messageReceived(String message);

		public void connected();

		public void disconnected();
	}

	public void sendPicture(final RoboPicture picture) {
		synchronized (mSendingLock) {
			if (!mSendingPicture && mSendingQueue.isEmpty()) {
				Log.i(TAG, "Sending picture " + picture.pictureNum);
				new SendPictureTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, picture);
			}
			else {
				Log.i(TAG, "Sending picture " + picture.pictureNum + " delayed, another send in progress");
				mSendingQueue.add(new Runnable() {
					@Override
					public void run() {
						_sendPicture(picture);
					}
				});	
			}
		}
	}

	private void _sendPicture(final RoboPicture picture) {
		synchronized (mSendingLock) {
			Log.i(TAG, "Sending picture " + picture.pictureNum);
			mSendingPicture = true;
			new SendPictureTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, picture);
		}
	}

	private void _sendMessage(final String message) {
		synchronized (mSendingLock) {

			if (mBufferOut != null && !mBufferOut.checkError()) {
				mBufferOut.println(message);
				mBufferOut.flush();
			}
		}
	}

	private class SendPictureTask extends AsyncTask<RoboPicture, Void, Void> {

		@Override
		protected Void doInBackground(RoboPicture... params) {
			RoboPicture picture = params[0];
			if (mBufferOut != null && !mBufferOut.checkError()) {
				mBufferOut.println("picture start " + picture);
				mBufferOut.flush();
				try {
					mOutputStream.write(picture.data);
					mOutputStream.flush();
				} catch (IOException e) {
					Log.i(TAG, "SendPicture failed on writing bytes");
					e.printStackTrace();
				}
				mBufferOut.println("picture end");
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					mSendingPicture = false;
				}
			}, 1000);
			super.onPostExecute(result);
		}

	}
}