package uk.ac.ed.insectlab.ant.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class InputDoubleDialog extends DialogFragment {

	private static float mCurrent;
	private static InputDoubleListener mListener;

	public static InputDoubleDialog create(float current,
			InputDoubleListener listener) {
		mCurrent = current;
		mListener = listener;
		return new InputDoubleDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new Builder(getActivity());
		LayoutInflater inflater = (LayoutInflater) getActivity()
				.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_double, null, false);
		final EditText input = (EditText) view.findViewById(R.id.double_text);
		input.setText(BluetoothAction.DECIMAL_FORMAT.format(mCurrent) + "");
		builder.setView(view).setPositiveButton(android.R.string.ok,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						float entered = Float.parseFloat(input.getText()
								.toString());
						mListener.doubleEntered(entered);
					}
				});
		return builder.create();
	}
}
