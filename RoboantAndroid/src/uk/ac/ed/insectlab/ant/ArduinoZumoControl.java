package uk.ac.ed.insectlab.ant;

public interface ArduinoZumoControl {

	void setSpeeds(int speedLeft, int speedRight);

	void simpleTurnInPlaceBlocking(int speed, int turnTime);
}
