package uk.ac.ed.insectlab.ant.bluetooth;

import java.text.DecimalFormat;

public enum BluetoothAction {
	RECORDON(true), RECORDOFF(false), NAVIGATION_MODE(true), RECORDING_MODE(
			true), NAVIGATEON(true), NAVIGATEOFF(false), SWAYING_PARAMETER(true), SWAYINGON(
			true), SWAYINGOFF(false);

	private final boolean flag;
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(
			"###.######");

	private BluetoothAction(boolean flag) {
		this.flag = flag;
	}

	public boolean getFlag() {
		return flag;
	}

	public static BluetoothAction fromMessage(String string) {
		for (BluetoothAction action : BluetoothAction.values()) {
			if (string.contains(action.name())) {
				return action;
			}
		}
		return null;
	}

	public static String encodeDouble(double value) {
		return "d" + DECIMAL_FORMAT.format(value) + "bl";
	}
}
