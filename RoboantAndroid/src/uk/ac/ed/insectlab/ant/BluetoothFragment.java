package uk.ac.ed.insectlab.ant;

import static android.app.Activity.RESULT_OK;
import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;
import static android.widget.Toast.LENGTH_SHORT;
import static uk.ac.ed.insectlab.ant.CardFragment.CardStatus.ERROR;
import static uk.ac.ed.insectlab.ant.CardFragment.CardStatus.LOADING;
import static uk.ac.ed.insectlab.ant.CardFragment.CardStatus.NONE;
import static uk.ac.ed.insectlab.ant.CardFragment.CardStatus.OK;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;

import uk.ac.ed.insectlab.ant.PairedDeviceChooserDialog.BluetoothDeviceChosenListener;
import uk.ac.ed.insectlab.ant.bluetooth.BluetoothAction;
import uk.ac.ed.insectlab.ant.bluetooth.Mode;
import uk.ac.ed.insectlab.ant.service.BluetoothThread;
import uk.ac.ed.insectlab.ant.service.RoboantService.BluetoothBond;
import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

public class BluetoothFragment extends CardFragment implements BluetoothBond,
		BluetoothDeviceChosenListener {

	private static final String CANCEL_TEXT = "Cancel";

	protected static final String TAG = BluetoothFragment.class.getSimpleName();

	interface BluetoothFragmentListener {

		void startBluetoothThread(BluetoothSocket socket, String deviceName);

		boolean changeRecordStateFromBluetooth(boolean b);

		boolean changeNavigateStateFromBluetooth(boolean navigate);

		boolean changeToNavigationModeFromBluetooth(boolean b);

		boolean changeToRecordingModeFromBluetooth(boolean b);

	}

	private static final int REQUEST_ENABLE_BT = 0;

	private BluetoothFragmentListener mListener;
	private BluetoothAdapter mBluetoothAdapter;

	private BluetoothThread mBluetoothThread;

	private boolean mBluetoothNotSupported;

	private Button mButtonConnect;

	private Handler mHandler;

	private BluetoothConnectThread mBluetoothConnectThread;

	private Mode mMode;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (BluetoothFragmentListener) activity;
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new RuntimeException(
					"Host activity does not implement listener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		mBluetoothAdapter = getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			mBluetoothNotSupported = true;
		}
	}

	@Override
	public View onCreateCardView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_bluetooth, container,
				false);

		mButtonConnect = (Button) view.findViewById(R.id.btn_connect);
		mButtonConnect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mButtonConnect.getText().equals(CANCEL_TEXT)) {
					mBluetoothConnectThread.cancel();
				} else if (!mBluetoothAdapter.isEnabled()) {
					Intent enableBtIntent = new Intent(
							BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
				} else {
					bluetoothPair();
				}
			}
		});

		if (mBluetoothNotSupported) {
			disableWithText("Bluetooth not supported");
		}

		setLabel("Bluetooth");

		return view;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
			bluetoothPair();
		} else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}

	private void bluetoothPair() {
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
			PairedDeviceChooserDialog.makeInstance(pairedDevices, this).show(
					getFragmentManager(), "PairedDeviceChooser");
			;
		} else {
			Toast.makeText(getActivity(), "No paired devices",
					Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	public void bluetoothConnected(BluetoothThread bluetoothThread) {
		disableWithText("Connected to " + bluetoothThread.getDeviceName());
		mBluetoothThread = bluetoothThread;
		sendModeMessage(mMode);
	}

	@Override
	public void bluetoothDisconnected() {
		setStatus(ERROR);
		enableWithText("Connect");
	}

	@Override
	public void bluetoothMessageReceived(final String message) {
		Log.i(TAG, "bluetoothMessageReceived " + message);
		boolean result;
		final BluetoothAction action = BluetoothAction.fromMessage(message);
		if (action == null) {
			Log.i(TAG, "Unrecognized action: " + message);
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(getActivity(),
							"Unrecognized action: " + message, LENGTH_SHORT)
							.show();
				}
			});
			return;
		}
		switch (action) {
		case RECORDON:
		case RECORDOFF:
			result = mListener.changeRecordStateFromBluetooth(action.getFlag());
			break;
		case NAVIGATION_MODE:
			result = mListener.changeToNavigationModeFromBluetooth(action
					.getFlag());
			break;
		case RECORDING_MODE:
			result = mListener.changeToRecordingModeFromBluetooth(action
					.getFlag());
			break;
		case NAVIGATEON:
		case NAVIGATEOFF:
			result = mListener.changeNavigateStateFromBluetooth(action
					.getFlag());
			break;
		default:
			Log.i(TAG, "Unrecognized action: " + action);
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(getActivity(),
							"Unrecognized action: " + action, LENGTH_SHORT)
							.show();
				}
			});
			return;
		}
		if (!result) {
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(getActivity(),
							"Could not execute action: " + action, LENGTH_SHORT)
							.show();
				}
			});

		}
		sendResultMessage(action.name(), result);
	}

	private void sendResultMessage(String string, boolean result) {
		mBluetoothThread.write(getMessageBytes("RESULT\t" + string + "\t"
				+ result));
	}

	private void sendModeMessage(Mode mode) {
		if (mode != null) {
			mBluetoothThread.write(getMessageBytes("MODE\t" + mode.name()));
		} else {
			Toast.makeText(getActivity(), "Mode is null", LENGTH_SHORT).show();
		}
	}

	private byte[] getMessageBytes(String string) {
		if (!string.endsWith("\n")) {
			string = string + "\n";
		}
		try {
			return string.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void bluetoothDeviceSelected(BluetoothDevice bluetoothDevice) {
		enableWithText(CANCEL_TEXT);
		mBluetoothConnectThread = new BluetoothConnectThread(bluetoothDevice);
		mBluetoothConnectThread.start();
	}

	private void disableWithText(final String string) {
		changeButtonStateWithText(string, false);
	}

	private void changeButtonStateWithText(final String string,
			final boolean state) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				if (mButtonConnect != null) {
					mButtonConnect.setText(string);
					mButtonConnect.setEnabled(state);
				}
			}
		});
	}

	private void enableWithText(final String string) {
		changeButtonStateWithText(string, true);
	}

	private class BluetoothConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private BluetoothDevice mmDevice;

		public BluetoothConnectThread(BluetoothDevice device) {
			this.mmDevice = device;
			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server
				// code
				mmSocket = device.createRfcommSocketToServiceRecord(UUID
						.fromString("00001101-0000-1000-8000-00805F9B34FB"));
			} catch (IOException e) {
				throw new RuntimeException();
			}
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			mBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				setStatus(LOADING);
				mmSocket.connect();
			} catch (IOException connectException) {
				connectException.printStackTrace();
				try {
					mmSocket.close();
				} catch (IOException closeException) {
					closeException.printStackTrace();
				} finally {
					setStatus(ERROR);
					enableWithText("Connect");
				}
				return;
			}

			// Do work to manage the connection (in a separate thread)
			setStatus(OK);
			mListener.startBluetoothThread(mmSocket, mmDevice.getName());
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				enableWithText("Connect");
				setStatus(NONE);
			}
		}
	}

	public void setMode(Mode mode) {
		mMode = mode;
	}

}
