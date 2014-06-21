package uk.ac.ed.insectlab.ant;

import java.util.Arrays;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

public class WillshawNetwork {

	private static final String TAG = WillshawNetwork.class.getName();
	private int M;
	private int fic = 10;
	private int kc = 20000;

	private int[][] imageToLayer1;

	private boolean[] weights;
	private List<Point> mPixels;

	private boolean[] currentActivation;
	private int threshold = 250;

	public WillshawNetwork(List<Bitmap> routePictures, List<Point> pixels) {

		Bitmap first = routePictures.get(0);
		M = pixels.size();
		mPixels = pixels;
		Log.i(TAG, "M=" + M);

		imageToLayer1 = new int[M][fic];
		connect_generator();
		currentActivation = new boolean[kc];
		weights = new boolean[kc];
		Arrays.fill(weights, true);

		double activatedSum = 0;
		for (Bitmap bmp: routePictures) {
			activatedSum += train(bmp);
		}
		double meanActivatedSum = activatedSum/routePictures.size();
		Log.i(TAG, "Mean sparseness=" + meanActivatedSum/kc);
	}

	private void connect_generator() {
		for (int i = 0; i < M; ++i) {
			for (int j = 0; j < fic; ++j) {
				imageToLayer1[i][j] = (int)(Math.random() * kc);
			}
		}
	}

	private int train(Bitmap bmp) {
		int count = 0;
		activate(bmp);
		
		for (int i = 0; i < kc; ++i) {
			if (currentActivation[i]) {
				count++;
				weights[i] = false;
			}
		}
		
		return count;

	}

	private void activate(Bitmap bmp) {
		int pixel, g;
		Point p;
		int[] intensity = new int[kc];

		Arrays.fill(currentActivation, false);

		for (int i = 0; i < M; ++i) {
			p = mPixels.get(i);
			pixel = bmp.getPixel(p.x, p.y);
			g = Color.green(pixel);
			for (int j = 0; j < fic; ++j) {
				intensity[imageToLayer1[i][j]] += g;
			}
		}

		for (int i = 0; i < kc; ++i) {
			currentActivation[i] = intensity[i] > threshold;
		}
	}

	public int process(Bitmap bmp) {
		activate(bmp);
		int count = 0;
		for (int i = 0; i < kc; ++i) {
			if (currentActivation[i] && weights[i]) {
				count++;
			}
		}
		return count;
	}
	
	public void checkValid() {
		Log.i(TAG, Environment.getExternalStorageDirectory().getAbsolutePath());
//		File file = new File(Environment.getExternalStorageDirectory());
//		try {
//			MatFileReader reader = new MatFileReader(file);
//			Map<String, MLArray> content = reader.getContent();
//			for (Entry<String, MLArray> entry : content.entrySet()) {
//				Log.i(TAG, "Entry: " + entry.getKey());
//				Log.i(TAG, "Contents: " + entry.getValue());
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

}
