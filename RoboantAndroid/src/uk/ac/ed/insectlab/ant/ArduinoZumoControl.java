package uk.ac.ed.insectlab.ant;

public interface ArduinoZumoControl {

	void setSpeeds(int turnSpeed, int i);

	void simpleTurnInPlaceBlocking(int speed, int turnTime);

	void doGoTowards(LookAroundListener aiControlTask, int minStep);

	void doLookAroundStep(LookAroundListener aiControlTask);

	void setLeftSpeed(int speed);

	void setRightSpeed(int speed);

	void calibrate();

	void lookAroundDone();

	void goTowardsDone();

	void lookAroundStepDone(int parseInt);

}
