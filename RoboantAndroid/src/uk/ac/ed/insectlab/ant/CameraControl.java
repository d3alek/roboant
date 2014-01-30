package uk.ac.ed.insectlab.ant;


public interface CameraControl {
	public float getCameraXRatio();

	public float getCameraYRatio();

	public float getRadiusRatio();

	void takePicture(CameraReceiver receiver, int pictureNum);
}

