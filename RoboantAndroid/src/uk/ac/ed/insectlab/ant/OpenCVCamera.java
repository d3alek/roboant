package uk.ac.ed.insectlab.ant;

import java.util.List;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class OpenCVCamera implements CvCameraViewListener2 {

	public interface FlowListener {
		public boolean flowChanged(double flow);
	}

	public interface PictureListener {
		public void pictureReceived(Bitmap picture);
	}

	MatOfPoint2f mMOP2fptsPrev;
	MatOfPoint2f mMOP2fptsThis;
	Mat matOpFlowThis;
	Mat matOpFlowPrev;
	private MatOfPoint MOPcorners;
	private MatOfPoint2f mMOP2fptsSafe;
	private MatOfByte mMOBStatus;
	private MatOfFloat mMOFerr;
	private List<Point> cornersPrev;
	private List<Point> cornersThis;
	private List<Byte> byteStatus;
	private Point pt;
	private Point pt2;
	private Scalar colorRed = new Scalar(new double[] {200.0, 0.0, 0.0, 0.0});
	private Scalar colorGreen = new Scalar(new double[] {1.0, 200.0, 0.0, 0.0});
	private int iLineThickness = 10;
	private static final int MAX_CORNERS = 40;
	private static final String TAG = null;
	private static final int LASSO_POINTS_NUM = 100;
	private static final double SPRING_FORCE_K = 0.1;

	private static final int CAMERA_RADIUS = 220;

	private int waitFor = 10;
	Mat mRgbaSmall;

	int DOWNSAMPLE_RATE = 2;

	Mat mRgba;
	private Mat mRgbaSave;
	private boolean mCameraSegmented;
	private Point mFlow;
	private double mTotalFlow;
	private FlowListener mFlowListener;
	private PictureListener mPictureListener;

	@Override
	public void onCameraViewStarted(int width, int height) {
		mMOP2fptsPrev = new MatOfPoint2f();
		mMOP2fptsThis = new MatOfPoint2f();
		matOpFlowPrev = new Mat();
		matOpFlowThis = new Mat();
		MOPcorners = new MatOfPoint();
		mMOP2fptsSafe = new MatOfPoint2f();
		mMOBStatus = new MatOfByte();
		mMOFerr = new MatOfFloat();
		mRgbaSmall = new Mat();
		mRgbaSave = new Mat();

	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub

	}


	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		return null;
	}
	@Override
	public Mat onCameraFrameMat(Mat mat) {
		//		mRgba = inputFrame.rgba();
		mRgba = mat;

		//		Rect rangeRect = new Rect(mRgba.cols()/2, mRgba.rows()/2, CAMERA_RADIUS, CAMERA_RADIUS);
		//		
		//		Mat mRgbaCropped = mRgba.submat(rangeRect);

		if (waitFor > 0) {
			waitFor--;
			return mRgba;
		}

		Imgproc.resize(mRgba, mRgbaSmall, new Size(mRgba.cols()/DOWNSAMPLE_RATE, mRgba.rows()/DOWNSAMPLE_RATE));
		mRgba.copyTo(mRgbaSave);

		if (mPictureListener != null) {
			mPictureListener.pictureReceived(getPanoramicPicture());
			mPictureListener = null;
		}

		calcOpticFlow(mRgbaSmall);
		drawOpticFlow(mRgba);

		return mRgba;
	}

	private Point multPointScalar(Point point, double scalar) {
		return new Point(point.x * scalar, point.y * scalar);
	}

	private static Point addPoints(Point p1, Point p2) {
		return new Point(p1.x + p2.x, p1.y + p2.y);
	}

	private static Point subtrPoints(Point p1, Point p2) {
		return new Point(p1.x - p2.x, p1.y - p2.y);
	}

	private static double distPoints(Point p1, Point p2) {
		return Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
	}

	private void drawOpticFlow(Mat mrgba) {
		int bytesSet = 0, bytesSetTop = 0;
		double flowX = 0, flowY = 0;

		double totalFlow = 0;

		for (int x = 0; x < byteStatus.size() - 1; x++) {  
			if (byteStatus.get(x) == 1) {  
				pt = cornersThis.get(x);  
				pt2 = cornersPrev.get(x);  

				pt.set(new double[] {pt.x*DOWNSAMPLE_RATE, pt.y*DOWNSAMPLE_RATE});
				pt2.set(new double[] {pt2.x*DOWNSAMPLE_RATE, pt2.y*DOWNSAMPLE_RATE});

				Core.circle(mrgba, pt, 5, colorRed , iLineThickness  - 1);  
				Core.line(mrgba, pt, pt2, colorRed, iLineThickness);  

				//				avgFlow += Math.sqrt((pt.x - pt2.x)*(pt.x - pt2.x)
				//						+ (pt.y - pt2.y)*(pt.y - pt2.y));
				if (pt.x < mrgba.rows()/2) {
					flowX += pt.x - pt2.x;
					flowY += pt.y - pt2.y;
					bytesSetTop++;
				}
				bytesSet++;

				totalFlow += distPoints(pt, pt2);
			}  

		}  
		if (bytesSet > 0) {
			Point avgFlow = new Point(flowX / bytesSetTop, flowY / bytesSetTop);
			Log.i(TAG, "Avg flow is " + avgFlow.x + " " + avgFlow.y);

			Point showFlowFrom, showFlowTo;

			showFlowFrom = new Point(mRgba.cols()/2., mRgba.rows()/2.);
			showFlowTo = new Point(showFlowFrom.x + avgFlow.x, showFlowFrom.y + avgFlow.y);

			Core.line(mRgba, showFlowFrom, showFlowTo, colorGreen, iLineThickness+1);

			mFlow = avgFlow;
			mTotalFlow = totalFlow/bytesSet;
			if (mFlowListener != null) {
				if (!mFlowListener.flowChanged(mTotalFlow)) {
					mFlowListener = null;
				}
			}
		}
	}

	public void setFlowListener(FlowListener listener) {
		mFlowListener = listener;
	}

	private void calcOpticFlow(Mat rgba) {
		if (mMOP2fptsPrev.rows() == 0) {  
			// first time through the loop so we need prev and this mats  
			// plus prev points  
			// get this mat  
			Imgproc.cvtColor(rgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);  

			// copy that to prev mat  
			matOpFlowThis.copyTo(matOpFlowPrev);  

			// get prev corners  
			Imgproc.goodFeaturesToTrack(matOpFlowPrev, MOPcorners, MAX_CORNERS, 0.01, 20);  
			mMOP2fptsPrev.fromArray(MOPcorners.toArray());  

			// get safe copy of this corners  
			mMOP2fptsPrev.copyTo(mMOP2fptsSafe);  
		}  
		else  
		{  
			// we've been through before so  
			// this mat is valid. Copy it to prev mat  
			matOpFlowThis.copyTo(matOpFlowPrev);  

			// get this mat  
			Imgproc.cvtColor(rgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);  

			// get the corners for this mat  
			Imgproc.goodFeaturesToTrack(matOpFlowThis, MOPcorners, MAX_CORNERS, 0.01, 20);  
			mMOP2fptsThis.fromArray(MOPcorners.toArray());  

			// retrieve the corners from the prev mat  
			// (saves calculating them again)  
			mMOP2fptsSafe.copyTo(mMOP2fptsPrev);  

			// and save this corners for next time through  

			mMOP2fptsThis.copyTo(mMOP2fptsSafe);  
		}  

		Video.calcOpticalFlowPyrLK(matOpFlowPrev, matOpFlowThis, mMOP2fptsPrev, mMOP2fptsThis, mMOBStatus, mMOFerr);  

		cornersPrev = mMOP2fptsPrev.toList();  
		cornersThis = mMOP2fptsThis.toList();  
		byteStatus = mMOBStatus.toList();  



	}

	private Bitmap getPanoramicPicture() {
		Log.i(TAG, "Panoramic picture dimensions " + mRgbaSave.cols() + " " + mRgbaSave.rows());
		Bitmap bmp = Bitmap.createBitmap(mRgbaSave.cols(), mRgbaSave.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(mRgbaSave, bmp);

		return bmp;
	}

	public void getPicture(PictureListener listener) {
		Log.i(TAG, "getPicture");
		mPictureListener = listener;
	}
	
	public double getOpticFlow(Bitmap from, Bitmap to) {
		Mat fromMat = new Mat();
		Mat toMat = new Mat();
		Mat fromGray = new Mat();
		Mat toGray = new Mat();
		
		MatOfPoint MOPcornersFrom = new MatOfPoint();
		MatOfPoint MOPcornersTo = new MatOfPoint();
		
		MatOfPoint2f MOPptsFrom = new MatOfPoint2f();
		MatOfPoint2f MOPptsTo = new MatOfPoint2f();

		Utils.bitmapToMat(from, fromMat);
		Utils.bitmapToMat(to, toMat);
		
		MatOfByte MOBStatus = new MatOfByte();
		MatOfFloat MOFerr = new MatOfFloat();

		Imgproc.cvtColor(fromMat, fromGray, Imgproc.COLOR_RGBA2GRAY);  
		Imgproc.cvtColor(toMat, toGray, Imgproc.COLOR_RGBA2GRAY);  

		Imgproc.goodFeaturesToTrack(fromGray, MOPcornersFrom, MAX_CORNERS, 0.01, 20); 
		Imgproc.goodFeaturesToTrack(toGray, MOPcornersTo, MAX_CORNERS, 0.01, 20); 


		MOPptsFrom.fromArray(MOPcornersFrom.toArray());  
		MOPptsTo.fromArray(MOPcornersTo.toArray());  
		
		Video.calcOpticalFlowPyrLK(fromGray, toGray, MOPptsFrom, MOPptsTo, MOBStatus, MOFerr);  

		List<Point> cornersFrom = MOPptsFrom.toList();  
		List<Point> cornersTo = MOPptsTo.toList();  
		List<Byte> byteStatus = MOBStatus.toList();  
		
		
		int bytesSet = 0;
		double totalFlow = 0;

		for (int x = 0; x < byteStatus.size() - 1; x++) {  
			if (byteStatus.get(x) == 1) {  
				pt = cornersFrom.get(x);  
				pt2 = cornersTo.get(x);  

				pt.set(new double[] {pt.x*DOWNSAMPLE_RATE, pt.y*DOWNSAMPLE_RATE});
				pt2.set(new double[] {pt2.x*DOWNSAMPLE_RATE, pt2.y*DOWNSAMPLE_RATE});

				//				avgFlow += Math.sqrt((pt.x - pt2.x)*(pt.x - pt2.x)
				//						+ (pt.y - pt2.y)*(pt.y - pt2.y));
//				if (pt.x < fromGray.rows()/2) {
//					flowX += pt.x - pt2.x;
//					flowY += pt.y - pt2.y;
//					bytesSetTop++;
//				}
				bytesSet++;

				totalFlow += distPoints(pt, pt2);
			}  

		}  
		if (bytesSet > 0) {
			return totalFlow / bytesSet;
//			Point avgFlow = new Point(flowX / bytesSetTop, flowY / bytesSetTop);
//			Log.i(TAG, "Avg flow is " + avgFlow.x + " " + avgFlow.y);
//
//			Point showFlowFrom, showFlowTo;
//
//			showFlowFrom = new Point(mRgba.cols()/2., mRgba.rows()/2.);
//			showFlowTo = new Point(showFlowFrom.x + avgFlow.x, showFlowFrom.y + avgFlow.y);
//
//			Core.line(mRgba, showFlowFrom, showFlowTo, colorGreen, iLineThickness+1);
//
//			mFlow = avgFlow;
//			mTotalFlow = totalFlow/bytesSet;
//			if (mFlowListener != null) {
//				if (!mFlowListener.flowChanged(mTotalFlow)) {
//					mFlowListener = null;
//				}
//			}
		}
		return 0;
	}

}
