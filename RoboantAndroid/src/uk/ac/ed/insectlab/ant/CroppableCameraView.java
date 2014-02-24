package uk.ac.ed.insectlab.ant;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.util.AttributeSet;

public class CroppableCameraView extends JavaCameraView {

	private static final String TAG = CroppableCameraView.class.getSimpleName();

	public CroppableCameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setCrop(final int width, final int height) {
		post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mFrameHeight = height;
				mFrameWidth = width;
				AllocateCache();
			}
		});

	}

	@Override
	public void disconnectCamera() {
		super.disconnectCamera();
	}

}
