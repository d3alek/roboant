package uk.ac.ed.insectlab.ant;

public interface LookAroundListener {

    void lookAroundDone();

    void lookAroundStepDone(int step);

    void goTowardsDone();

}
