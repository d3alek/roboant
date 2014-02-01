package uk.ac.ed.insectlab.ant;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;

public class RoboPicture {
	
	public enum PictureType {
		GoTowards, LookAround;
	}
	
	byte[] data;
	PictureType type;
	int pictureNum = -1;
	
//	public RoboPicture(byte[] data, PictureType gotowards) {
//		this(data, gotowards, -1);
//	}
	public RoboPicture(byte[] data, PictureType lookaround, int pictureNum) {
		this.data = data; this.type = lookaround; this.pictureNum = pictureNum;
	}
	
	public RoboPicture(Bitmap bmp, PictureType lookaround, int pictureNum) {
		this(bitmapToByteArray(bmp), lookaround, pictureNum);
	}
	
//	public RoboPicture(Bitmap bmp, PictureType gotowards) {
//		this(bitmapToByteArray(bmp), gotowards);
//	}
	
	public static byte[] bitmapToByteArray(Bitmap bmp) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
		return stream.toByteArray();
	}
	@Override
	public String toString() {
		switch (type) {
		case GoTowards: return "gotowards " + pictureNum;
		default:
			return pictureNum + "";
		}
	}
}
