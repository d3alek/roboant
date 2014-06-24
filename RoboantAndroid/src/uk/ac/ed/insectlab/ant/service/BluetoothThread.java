package uk.ac.ed.insectlab.ant.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import uk.ac.ed.insectlab.ant.service.RoboantService.BluetoothBond;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothThread extends Thread {
	private static final String TAG = "BluetoothThread";
	private final BluetoothSocket mmSocket;
	private final BufferedReader mmInStream;
	private final OutputStream mmOutStream;
	private BluetoothListener mListener;
	private BluetoothBond mBond;

	interface BluetoothListener {

		void speedReceivedFromBluetooth(int left, int right);

	}



	public BluetoothThread(BluetoothSocket socket, BluetoothListener listener) {
		mmSocket = socket;
		InputStream tmpIn = null;
		OutputStream tmpOut = null;
		mListener = listener;


		// Get the input and output streams, using temp objects because
		// member streams are final
		try {
			tmpIn = socket.getInputStream();
			tmpOut = socket.getOutputStream();
		} catch (IOException e) { }

		mmInStream = new BufferedReader(new InputStreamReader(tmpIn));
		mmOutStream = tmpOut;
	}

	public void run() {
		byte[] buffer = new byte[1024];  // buffer store for the stream
		int bytes; // bytes returned from read()

		// Keep listening to the InputStream until an exception occurs
		while (true) {
			try {
				// Read from the InputStream
				String line = mmInStream.readLine();
				// Send the obtained bytes to the UI activity
				//                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
				//                        .sendToTarget();
				processBytes(line);
			} catch (IOException e) {
				break;
			}
		}
	}

	private void processBytes(String line) {
		String[] speeds = line.split("\\s");
		Log.i("BluetootThread", "line: " + line);
		char first = line.charAt(0);
		if ('0' <= first && first <= '9') {
			int speedL = Integer.parseInt(speeds[0]);
			int speedR = Integer.parseInt(speeds[1]);
			mListener.speedReceivedFromBluetooth(speedL,
					speedR);
		}
		else if (mBond != null)  {
			mBond.bluetoothMessageReceived(line);
		}
	}

	/* Call this from the main activity to send data to the remote device */
	public void write(byte[] bytes) {
		Log.i(TAG, "Writing " + bytes);
		try {
			mmOutStream.write(bytes);
		} catch (IOException e) { }
	}

	/* Call this from the main activity to shutdown the connection */
	public void cancel() {
		try {
			mmSocket.close();
		} catch (IOException e) { }
	}

	public void setBound(BluetoothBond bluetoothBond) {
		mBond = bluetoothBond;
		mBond.bluetoothConnected(this);
	}

}