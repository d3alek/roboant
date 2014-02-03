package uk.ac.ed.insectlab.ant;

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

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class TcpClient implements NetworkControl {

	private static final int KEEP_ALIVE_INTERVAL = 10000;
	private static final String TAG = "TcpClient";
	//    public static final String SERVER_IP = "192.168.0.100"; //your computer IP address
	//    public static final int SERVER_PORT = 4444;
	// message to send to the server
	private String mServerMessage;
	// sends message received notifications
	private OnMessageReceived mMessageListener = null;
	// while this is true, the server will continue running
	private boolean mRun = false;
	// used to send messages
	private PrintWriter mBufferOut;
	// used to read messages from the server
	private BufferedReader mBufferIn;
	private long mLastKeepAlive;
	private boolean mStopped;
	private OutputStream mOutputStream;
	private Handler mHandler;

	LinkedList<Runnable> mSendingQueue = new LinkedList<Runnable>();
	private boolean mSendingBlock;


	private Object mSendingLock = new Object();
	private boolean mSendingPicture;

	/**
	 * Constructor of the class. OnMessagedReceived listens for the messages received from server
	 */
	public TcpClient(OnMessageReceived listener) {
		mMessageListener = listener;
	}

	/**
	 * Sends the message entered by client to the server
	 *
	 * @param message text entered by client
	 */
	public void sendMessage(final String message) {
		synchronized (mSendingLock) {

			if (!mSendingPicture && mSendingQueue.isEmpty()) {
				mBufferOut.println(message);
				mBufferOut.flush();
				if (mBufferOut != null && !mBufferOut.checkError()) {
					if (message.startsWith(NetworkControl.NEW_LOOK_AROUND)) {
						mSendingBlock = true;
						Log.i(TAG, "Block sending");
						mSendingQueue.addFirst(new Runnable() {

							@Override
							public void run() {
								// dummy to prevent sending for a while;
							}
						});
						mHandler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// give some time for NEW_LOOK_AROUND MESSAGE to arrive 
								Log.i(TAG, "Unblock sending");
								mSendingQueue.removeFirst();
								mSendingBlock = false;
							}
						}, 2000);
					}
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

	/**
	 * Close the connection and release the members
	 */
	public void stopClient() {

		// send mesage that we are closing the connection
		_sendMessage(Constants.CLOSED_CONNECTION);
		mStopped = true;

		mRun = false;

		if (mBufferOut != null) {
			mBufferOut.flush();
			mBufferOut.close();
		}

		//        mMessageListener = null;
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
			//here you must put your computer's IP address.
			InetAddress serverAddr = InetAddress.getByName(serverip);

			//            Log.e("TCP Client", "C: Connecting...");

			//create a socket to make the connection with the server
			Socket socket = new Socket(serverAddr, serverport);
			socket.setSoTimeout(1000);

			try {

				mMessageListener.connected();
				mLastKeepAlive = System.currentTimeMillis();

				//sends the message to the server
				mOutputStream = socket.getOutputStream();
				mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mOutputStream)), true);

				//receives the message which the server sends back
				mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				// send login name
				_sendMessage(Constants.LOGIN_NAME);

				//in this while the client listens for the messages sent by the server
				while (mRun) {

					//					mHandler.post(executeQueueFront);
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
						//                		Log.i(TAG, "read timeout");
						mServerMessage = null;
					}

					if (mServerMessage != null && mMessageListener != null) {
						//call the method messageReceived from MyActivity class
						mMessageListener.messageReceived(mServerMessage);
					}

				}

				Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");

			} catch (IOException e) {

				//                Log.e("TCP", "S: Error", e);

			} finally {
				//the socket must be closed. It is not possible to reconnect to this socket
				// after it is closed, which means a new socket instance has to be created.
				socket.close();
				if (!mStopped) {
					mMessageListener.disconnected();
				}
			}

		} catch (Exception e) {

			//            Log.e("TCP", "C: Error", e);
			//        	Log.e("TCP", "Connection error");
			if (!mStopped) {
				mMessageListener.disconnected();
			}

		}

	}

	//Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
	//class at on asynckTask doInBackground
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
				if (message.startsWith(NetworkControl.NEW_LOOK_AROUND)) {
					mSendingBlock = true;
					Log.i(TAG, "Block sending");
					mSendingQueue.addFirst(new Runnable() {

						@Override
						public void run() {
							// dummy to prevent sending for a while;
						}
					});
					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							// give some time for NEW_LOOK_AROUND MESSAGE to arrive 
							Log.i(TAG, "Unblock sending");
							mSendingQueue.removeFirst();
							mSendingBlock = false;
						}
					}, 2000);
				}
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