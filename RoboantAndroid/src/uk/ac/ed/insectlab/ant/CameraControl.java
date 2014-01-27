package uk.ac.ed.insectlab.ant;

public interface CameraControl {
    public void takePicture(CameraReceiver receiver);
    public void givePictureToReceiver();
}

