package uk.ac.ed.insectlab.ant;

import android.os.AsyncTask;

class MotorTestTask extends AsyncTask<ArduinoZumoControl, Void, Void> {

    ArduinoZumoControl mRoboAntControl;

    @Override
    protected Void doInBackground(ArduinoZumoControl... params) {
        mRoboAntControl = params[0];

        for (int speed = 0; speed <= 400; speed++)
        {
            mRoboAntControl.setSpeeds(speed, 0);

        }

        for (int speed = 400; speed >= 0; speed--)
        {
            mRoboAntControl.setSpeeds(speed, 0);
        }

        for (int speed = 0; speed >= -400; speed--)
        {
            mRoboAntControl.setSpeeds(speed, 0);
        }

        for (int speed = -400; speed <= 0; speed++)
        {
            mRoboAntControl.setSpeeds(speed, 0);
        }

        for (int speed = 0; speed <= 400; speed++)
        {
            mRoboAntControl.setSpeeds(0, speed);
        }

        for (int speed = 400; speed >= 0; speed--)
        {
            mRoboAntControl.setSpeeds(0, speed);
        }

        for (int speed = 0; speed >= -400; speed--)
        {
            mRoboAntControl.setSpeeds(0, speed);
        }

        for (int speed = -400; speed <= 0; speed++)
        {
            mRoboAntControl.setSpeeds(0, speed);
        }

        return null;
    }

}