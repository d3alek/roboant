package uk.ac.ed.insectlab.ant;

public class RoboPicture {
	
	public enum PictureType {
		GoTowards, LookAround;
	}
	
	byte[] data;
	PictureType type;
	int pictureNum = -1;
	
	public RoboPicture(byte[] data, PictureType type) {
		this.data = data; this.type = type;
	}
	public RoboPicture(byte[] data, PictureType type, int pictureNum) {
		this.data = data; this.type = type; this.pictureNum = pictureNum;
	}
	
	@Override
	public String toString() {
		switch (type) {
		case GoTowards: return "gotowards";
		default:
			return pictureNum + "";
		}
	}
}
