package uk.ac.ed.insectlab.ant.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import uk.ac.ed.insectlab.ant.service.RoboantService.BluetoothBond;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothThread extends Thread {
	private static final String TAG = "BluetoothThread";
	private final BluetoothSocket mmSocket;
	private BluetoothListener mListener;
	private BluetoothBond mBond;
	private InputStreamReader mInStream;
	private BufferedReader mmInBufferedStream;
	private OutputStream mmOutStream;
	private String mDeviceName;
	private boolean notifyBondThatConnectedWhenStart;

	interface BluetoothListener {
		void speedReceivedFromBluetooth(int left, int right);

		void bluetoothDisconnected();
	}

	public BluetoothThread(BluetoothSocket socket, BluetoothListener listener,
			String deviceName) {
		mmSocket = socket;
		mListener = listener;
		mDeviceName = deviceName;
	}

	public void run() {
		// Keep listening to the InputStream until an exception occurs
		InputStream tmpIn;
		OutputStream tmpOut;
		try {
			tmpIn = mmSocket.getInputStream();
			tmpOut = mmSocket.getOutputStream();
		} catch (IOException e) {
			closeStreamsAndCancel();
			return;
		}
		try {
			mInStream = new InputStreamReader(tmpIn);
			mmInBufferedStream = new BufferedReader(mInStream);
			mmOutStream = tmpOut;
			if (notifyBondThatConnectedWhenStart && mBond != null) {
				mBond.bluetoothConnected(this);
			}
			while (true) {
				// Read from the InputStream
				String line = null;
				if (mmSocket.isConnected()) {
					line = mmInBufferedStream.readLine();
				} else {
					break;
				}
				// Send the obtained bytes to the UI activity
				// mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
				// .sendToTarget();
				processBytes(line);
			}
		} catch (Exception e) {

		} finally {
			closeStreamsAndCancel();
		}

	}

	private void closeStreamsAndCancel() {
		try {
			mmInBufferedStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				mInStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					mInStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						mmOutStream.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					} finally {
						cancel();
					}
				}
			}
		}
	}

	private void processBytes(String line) {
		String[] speeds = line.split("\\s");
		Log.i("BluetootThread", "line: " + line);
		char first = line.charAt(0);
		char second = line.charAt(1);
		if (isDigit(first) || (first == '-' && isDigit(second))) {
			int speedL = Integer.parseInt(speeds[0]);
			int speedR = Integer.parseInt(speeds[1]);
			mListener.speedReceivedFromBluetooth(speedL, speedR);
		} else if (mBond != null) {
			mBond.bluetoothMessageReceived(line);
		}
	}

	private boolean isDigit(char digit) {
		return '0' <= digit && digit <= '9';
	}

	/* Call this from the main activity to send data to the remote device */
	public void write(byte[] bytes) {
		Log.i(TAG, "Writing " + bytes);
		try {
			if (mmSocket.isConnected()) {
				mmOutStream.write(bytes);
			} else {
				closeStreamsAndCancel();
			}
		} catch (IOException e) {
			closeStreamsAndCancel();
		}
	}

	/* Call this from the main activity to shutdown the connection */
	public void cancel() {
		try {
			mmSocket.close();
		} catch (IOException e) {

		} finally {
			mListener.bluetoothDisconnected();
			mBond.bluetoothDisconnected();
		}
	}

	public void setBound(BluetoothBond bluetoothBond) {
		mBond = bluetoothBond;
		if (mInStream != null && mmOutStream != null) {
			mBond.bluetoothConnected(this);
		} else {
			notifyBondThatConnectedWhenStart = true;
		}
	}

	public String getDeviceName() {
		return mDeviceName;
	}

}