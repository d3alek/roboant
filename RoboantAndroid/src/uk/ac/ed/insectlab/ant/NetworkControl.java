package uk.ac.ed.insectlab.ant;


public interface NetworkControl {
	public static final String NEW_LOOK_AROUND  = "new_look_around";
	public static final String TURN_TO = "turn_to ";

	public void sendMessage(String message);
	public void sendPicture(RoboPicture roboPicture);
}