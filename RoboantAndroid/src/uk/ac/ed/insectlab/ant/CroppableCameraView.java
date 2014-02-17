package uk.ac.ed.insectlab.ant;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.util.AttributeSet;

public class CroppableCameraView extends JavaCameraView {

	private static final String TAG = CroppableCameraView.class.getSimpleName();
	private int mHeight;
	private int mWidth;

	public CroppableCameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		disableFpsMeter();
	}

	public void setCrop(int width, int height) {
//		mMaxHeight = resolution.height;
//		mMaxWidth = resolution.width;
		mFrameHeight = height;
		mFrameWidth = width;
//		connectCamera(getWidth(), getHeight());
		AllocateCache();
		
//		android.view.ViewGroup.LayoutParams lp = getLayoutParams();
//		lp.width = width;
//		lp.height = height;
//		setLayoutParams(lp);
//		setMaxFrameSize(width, height);
//		mCacheBitmap = Bitmap.createBitmap(mFrameWidth, mFrameHeight, Bitmap.Config.ARGB_8888);
	}

}
