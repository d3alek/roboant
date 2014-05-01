package uk.ac.ed.insectlab.ant;

public interface ArduinoZumoControl {

	void setSpeeds(int speedLeft, int speedRight);

	void turnInPlaceBlocking(int speed, int turnTime);
}
