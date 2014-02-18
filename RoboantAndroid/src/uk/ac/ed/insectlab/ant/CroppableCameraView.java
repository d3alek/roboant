package uk.ac.ed.insectlab.ant;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.util.AttributeSet;

public class CroppableCameraView extends JavaCameraView {

	private static final String TAG = CroppableCameraView.class.getSimpleName();

	public CroppableCameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setCrop(int width, int height) {
		mFrameHeight = height;
		mFrameWidth = width;
		AllocateCache();
	}
	
	@Override
	public void disconnectCamera() {
		super.disconnectCamera();
	}

}
