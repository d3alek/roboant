package uk.ac.ed.insectlab.ant;

import java.util.ArrayList;
import java.util.Set;

import android.R;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PairedDeviceChooserDialog extends DialogFragment {

	interface BluetoothDeviceChosenListener {

		void deviceSelected(BluetoothDevice bluetoothDevice);
		
	}

	private static BluetoothDeviceChosenListener mListener;
	private static ArrayList<BluetoothDevice> mPairedDevices;
	
	public static PairedDeviceChooserDialog makeInstance(Set<BluetoothDevice> pairedDevices,
			BluetoothDeviceChosenListener listener) {
		mListener = listener;
		mPairedDevices = new ArrayList<BluetoothDevice>(pairedDevices);
		
		PairedDeviceChooserDialog dialog = new PairedDeviceChooserDialog();
		
		return dialog;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.list_content, container, false);
		ListView list = (ListView) v.findViewById(android.R.id.list);
		

		ArrayList<String> deviceNames = new ArrayList<String>();
		for (BluetoothDevice device : mPairedDevices) {
			deviceNames.add(device.getName());
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
				deviceNames);
		
		list.setAdapter(adapter);
		
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mListener.deviceSelected(mPairedDevices.get(position));
				
			}
		});

		return v;
	}
	
}
