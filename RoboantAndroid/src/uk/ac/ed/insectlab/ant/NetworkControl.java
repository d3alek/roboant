package uk.ac.ed.insectlab.ant;


public interface NetworkControl {
	public static final String NEW_LOOK_AROUND  = "new_look_around";
	public static final String TURN_TO = "turn_to ";
	public static final String ROUTE_MATCH = "route_match ";
	public static final String SSD_MESSAGE = "ssd";
	public static final String SKEWNESS_MESSAGE = "skewness";

	public void sendMessage(String message);
	public void sendPicture(RoboPicture roboPicture);
}