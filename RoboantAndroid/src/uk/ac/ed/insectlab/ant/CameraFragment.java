//package uk.ac.ed.insectlab.ant;
//
//import org.opencv.android.BaseLoaderCallback;
//import org.opencv.android.CameraBridgeViewBase;
//import org.opencv.android.LoaderCallbackInterface;
//
//import uk.co.ed.insectlab.ant.R;
//import android.app.Fragment;
//import android.hardware.Camera;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.SurfaceView;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//
//public class CameraFragment extends Fragment {
//
//	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
//		@Override
//		public void onManagerConnected(int status) {
//			switch (status) {
//			case LoaderCallbackInterface.SUCCESS:
//			{
//				Log.i(TAG, "OpenCV loaded successfully");
//				mOpenCvCameraView.enableView();
//			} break;
//			default:
//			{
//				super.onManagerConnected(status);
//			} break;
//			}
//		}
//	};
//
//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container,
//			Bundle savedInstanceState) {
//		Button capture = (Button)findViewById(R.id.btn_take_picture);
//		capture.setOnClickListener(new Button.OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				takePicture();
//			}
//
//		});
//		Camera camera =  Util.getCameraInstance(CAMERA_NUMBER);
//		Util.setCameraDisplayOrientation(this, CAMERA_NUMBER, camera);
//		camera.release();
//
//		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_preview);
//		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
//		mCamera = new OpenCVCamera();
//
//		mOpenCvCameraView.setCvCameraViewListener(mCamera);
//		return super.onCreateView(inflater, container, savedInstanceState);
//	}
//
//
//	private void takePicture() {
//		if (mCamera != null) {
//			if (mAIControl != null) {
//				takeAIPicture();
//			}
//			else {
//				//				mCamera.getPicture(new OpenCVCamera.PictureListener() {
//				//					@Override
//				//					public void pictureReceived(final Bitmap picture) {
//				//						handler.post(new Runnable() {
//				//
//				//							@Override
//				//							public void run() {
//				//								mStepTowardsPic.setImageBitmap(picture);
//				//
//				//							}
//				//						});
//				//					}
//				//				});
//				mStepTowardsPic.setImageBitmap(getCameraPicture());
//			}
//		}
//	}
//	
//	@Override
//	public void onDestroy() {
//		if (mOpenCvCameraView != null) {
//			mOpenCvCameraView.disableView();
//		}
//		super.onDestroy();
//	}
//	
//	@Override
//	public void onPause() {
//		if (mOpenCvCameraView != null) {
//			mOpenCvCameraView.disableView();
//		}
//		super.onPause();
//	}
//	
//	@Override
//	public void onResume() {
//		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
//		super.onResume();
//	}
//
//}
