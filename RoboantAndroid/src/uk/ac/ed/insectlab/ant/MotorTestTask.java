package uk.ac.ed.insectlab.ant;

import android.os.AsyncTask;

class MotorTestTask extends AsyncTask<RoboAntControl, Void, Void> {

    RoboAntControl mRoboAntControl;

    @Override
    protected Void doInBackground(RoboAntControl... params) {
        mRoboAntControl = params[0];

        for (int speed = 0; speed <= 400; speed++)
        {
            mRoboAntControl.setLeftSpeed(speed);

        }

        for (int speed = 400; speed >= 0; speed--)
        {
            mRoboAntControl.setLeftSpeed(speed);
        }

        // run left motor backward

        for (int speed = 0; speed >= -400; speed--)
        {
            mRoboAntControl.setLeftSpeed(speed);
        }

        for (int speed = -400; speed <= 0; speed++)
        {
            mRoboAntControl.setLeftSpeed(speed);
        }

        for (int speed = 0; speed <= 400; speed++)
        {
            mRoboAntControl.setRightSpeed(speed);
        }

        for (int speed = 400; speed >= 0; speed--)
        {
            mRoboAntControl.setRightSpeed(speed);
        }

        for (int speed = 0; speed >= -400; speed--)
        {
            mRoboAntControl.setRightSpeed(speed);
        }

        for (int speed = -400; speed <= 0; speed++)
        {
            mRoboAntControl.setRightSpeed(speed);
        }

        return null;
    }

}