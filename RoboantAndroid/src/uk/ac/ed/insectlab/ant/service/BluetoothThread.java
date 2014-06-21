package uk.ac.ed.insectlab.ant.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import uk.ac.ed.insectlab.ant.service.RoboantService.BluetoothBond;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothThread extends Thread {
	private static final String TAG = "BluetoothThread";
	private final BluetoothSocket mmSocket;
	private final InputStream mmInStream;
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

		mmInStream = tmpIn;
		mmOutStream = tmpOut;
	}

	public void run() {
		byte[] buffer = new byte[1024];  // buffer store for the stream
		int bytes; // bytes returned from read()

		// Keep listening to the InputStream until an exception occurs
		while (true) {
			try {
				// Read from the InputStream
				bytes = mmInStream.read(buffer);
				// Send the obtained bytes to the UI activity
				//                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
				//                        .sendToTarget();
				processBytes(buffer);
			} catch (IOException e) {
				break;
			}
		}
	}

	private void processBytes(byte[] buffer) {
		try {
			String text = new String(buffer, "UTF-8");
			String[] speeds = text.split("\\s");
			try {
				int speedL = Integer.parseInt(speeds[0]);
				int speedR = Integer.parseInt(speeds[1]);
				mListener.speedReceivedFromBluetooth(speedL,
						speedR);
			} catch(NumberFormatException e) {
				if (mBond != null)  {
					mBond.bluetoothMessageReceived(text);
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
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