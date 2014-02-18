package uk.ac.ed.insectlab.ant;


public interface NetworkControl {
	public void sendMessage(String message);
	public void sendPicture(RoboPicture roboPicture);
}