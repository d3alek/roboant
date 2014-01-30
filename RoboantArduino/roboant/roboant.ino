/* Copyright 2012 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: http://code.google.com/p/usb-serial-for-android/
 */

// Sample Arduino sketch for use with usb-serial-for-android.
// Prints an ever-increasing counter, and writes back anything
// it receives.

#include <ZumoMotors.h>
#include <Wire.h>
#include <LSM303.h>

#define SPEED           200 // Maximum motor speed when going straight; variable speed when turning
#define CALIBRATION_SAMPLES 70  // Number of compass readings to take when calibrating
#define CRB_REG_M_2_5GAUSS 0x60 // CRB_REG_M value for magnetometer +/-2.5 gauss full scale
#define CRA_REG_M_220HZ    0x1C // CRA_REG_M value for magnetometer 220 Hz update rate

#define LED_PIN 13
#define HEARTBEAT_MS 1000
#define SPEED_DECR 10
#define TO_ZERO 50
#define SPEED_DECR_INTERVAL 100

#define TURN_BASE_SPEED 90 // Base speed when turning (added to variable speed)

#define DEVIATION_THRESH 8

#define TURN_DEG 30

ZumoMotors motors;

static int counter = 0;
static bool initialized = false;
static char buf[100];
static char buflength = 0;
static int i, j, k;
static unsigned long heartbeat_last;
static bool ledToggle = false;
static unsigned long speed_decr_last;
int speedL, speedR;
static bool lookingAround;
static int look_around_count;

LSM303 compass;

void setup() {
  pinMode(LED_PIN, OUTPUT);
  Serial.begin(115200);
  heartbeat_last = millis();
  speed_decr_last = millis();
  
  
  // Initiate the Wire library and join the I2C bus as a master
  Wire.begin();
   // Initiate LSM303
  compass.init();

  // Enables accelerometer and magnetometer
  compass.enableDefault();

  compass.writeReg(LSM303::CRB_REG_M, CRB_REG_M_2_5GAUSS); // +/- 2.5 gauss sensitivity to hopefully avoid overflow problems
  compass.writeReg(LSM303::CRA_REG_M, CRA_REG_M_220HZ);    // 220 Hz compass update rate

}

void calibrate_compass() {
  
  LSM303::vector<int16_t> running_min = {32767, 32767, 32767}, running_max = {-32767, -32767, -32767};
  unsigned char index;
  
 
  Serial.println("starting calibration");

  // To calibrate the magnetometer, the Zumo spins to find the max/min
  // magnetic vectors. This information is used to correct for offsets
  // in the magnetometer data.
  motors.setLeftSpeed(SPEED);
  motors.setRightSpeed(-SPEED);

  for(index = 0; index < CALIBRATION_SAMPLES; index ++)
  {
    // Take a reading of the magnetic vector and store it in compass.m
    compass.read();
    running_min.x = min(running_min.x, compass.m.x);
    running_min.y = min(running_min.y, compass.m.y);
    running_max.x = max(running_max.x, compass.m.x);
    running_max.y = max(running_max.y, compass.m.y);
    
    Serial.println(index);

    delay(50);
  }

  motors.setLeftSpeed(0);
  motors.setRightSpeed(0);

  Serial.print("max.x   ");
  Serial.print(running_max.x);
  Serial.println();
  Serial.print("max.y   ");
  Serial.print(running_max.y);
  Serial.println();
  Serial.print("min.x   ");
  Serial.print(running_min.x);
  Serial.println();
  Serial.print("min.y   ");
  Serial.print(running_min.y);
  Serial.println();

  // Set calibrated values to compass.m_max and compass.m_min
  compass.m_max.x = running_max.x;
  compass.m_max.y = running_max.y;
  compass.m_min.x = running_min.x;
  compass.m_min.y = running_min.y;
}

void loop() {
  if (!lookingAround && heartbeat_last + HEARTBEAT_MS <= millis()) {
   sendHeartbeat();
   heartbeat_last = millis();
  } 
  if (Serial.peek() != -1) {
    delay(5); // to finish writing
    do {
      if (buflength >= 100) {
        buflength = 0;
      }
      //Serial.print("Received=");
      buf[buflength++] = (char) Serial.read();
      //Serial.print(buf[buflength-1]);
      //Serial.print('\n');
    } while (Serial.peek() != -1);
  }
  if (!process_buf() && speed_decr_last + SPEED_DECR_INTERVAL <= millis()) {
    speed_decr_last = millis();
//    if (speedL != 0) {
//      if (abs(speedL) < TO_ZERO) {
//        speedL = 0;
//      }  
//      else if (speedL > 0) {
//        speedL -= SPEED_DECR;
//      }
//      else {
//        speedL += SPEED_DECR;
//      }
//      motors.setLeftSpeed(speedL);
//      delay(10);
//    }
//    if (speedR != 0) {
//      if (abs(speedR) < TO_ZERO) {
//        speedR = 0;
//      }
//      else if (speedR > 0) {
//        speedR -= SPEED_DECR;
//      }
//      else {
//        speedR += SPEED_DECR;
//      }
//      motors.setRightSpeed(speedR);
//      delay(10);
//    }
  }
}

bool process_buf() {
  if (buflength == 0) {
    return false;
  }

  for (i = 0; i < buflength; ++i) {
    if (buf[i] == 'c') {
      Serial.println("Calibrating compass!");
      calibrate_compass();
    }
    else if (buf[i] == 'g') {
      for (j = 0; j < buflength; ++j) {
        if (buf[j] == '\n') break;
      }
      
      if (j == buflength) {
        // No new line found, packet is not full
        return false;
      }
      
      buf[j] = '\0';
      int st = atoi(buf+i+1);
      goTowards(st);
    }
      
    else if (buf[i] == 't') {
      Serial.print("t received");
      look_around_count = 0;
      lookAround();
      break;
    }
    else if (buf[i] == 'n') {
      Serial.print("n received");
      lookAround();
      break;
    }
    else if (buf[i] == 'l') {
      //Serial.print("l found\n");
      for (j = i + 1; j < buflength; ++j) {
        if (buf[j] == 'r') break;
      }
      if (j == buflength) {
        // No r found, packet is not full
        //Serial.print("no r found\n");
        return false;
      }
      //Serial.print("r found\n");
      buf[j] = '\0';
      speedL = atoi(buf+i+1);
      //Serial.print("speedL=");
      //Serial.print(speedL);
      //Serial.print('\n');
      for (k = j + 1; k < buflength; ++k) {
        if (buf[k] == '\n') break;
      }
      if (k == buflength) {
        // No ending new line found, packet is not full
        buf[j] = 'r';
        //Serial.print("no ending new line found\n");
        return false;
      }
      //Serial.print("ending new line found\n");
      buf[k] = '\0';
      speedR = atoi(buf+j+1);
      //Serial.print("speedR=");
      //Serial.print(speedR);
      //Serial.print('\n');
      motors.setSpeeds(speedL, speedR);
      delay(10);
      i = k;
    }
  }
  buflength = 0;
  return true;
}

#define LOOK_AROUND_SPEED 200
#define LOOK_AROUND_STEP_MS 100
#define LOOK_AROUND_FOR 9

void lookAround() {
  if (look_around_count >= LOOK_AROUND_FOR) {
    Serial.print("la d\n");
    lookingAround = false;
    return;
  }
  lookingAround = true;
  if (look_around_count == LOOK_AROUND_FOR / 2) {
    // return to middle
    for (int i = 0; i < LOOK_AROUND_FOR / 2; ++i) {
      sideStep(false);
    }
  }
  if (look_around_count <= LOOK_AROUND_FOR / 2) {
    sideStep(true);
  }
  else {
    sideStep(false);
  }
    
  look_around_count++;
  Serial.print("la ");
  Serial.print(look_around_count);
  Serial.print('\n');
}

void goTowards(int st) {
  Serial.print("going towards ");
  Serial.print(st);
  Serial.print('\n');
  int goRightFor;
  if (st < LOOK_AROUND_FOR / 2) {
    goRightFor = LOOK_AROUND_FOR/2 + st;
  }
  else {
    goRightFor = LOOK_AROUND_FOR - st;
  }
  for (int i = 0; i < goRightFor; ++i) {
    sideStep(true);
  }
  motors.setLeftSpeed(200);
  delay(10);
  motors.setRightSpeed(200);
  delay(10);
      
  delay(500);
  motors.setLeftSpeed(0);
  delay(10);
  motors.setRightSpeed(0);
  delay(10);
  
  Serial.print("la g\n");
}

void sideStep(bool left) {
  int dir = left ? 1 : -1;
  
  // might be unnecessary
  motors.setSpeeds(0, 0);
  delay(100);
  // end of unnecessary maybe
  
  float heading = averageHeading();
  float target_heading = fmod(heading + dir * TURN_DEG, 360);
  
  float relative_heading = relativeHeading(heading, target_heading);
  
  int speed;
  
  while (abs(relative_heading) > DEVIATION_THRESH) {
    Serial.print("Im in loop");
    Serial.print(relative_heading);
    Serial.println();
    speed = SPEED * relative_heading/180;
    if (speed < 0)
      speed -= TURN_BASE_SPEED;
    else
      speed += TURN_BASE_SPEED;
    motors.setSpeeds(speed, -speed);
    delay(100);
    heading = averageHeading();
    relative_heading = relativeHeading(heading, target_heading);
  }
  
  motors.setSpeeds(0, 0);
}

void sideStepOld(bool left) {
  int dir = left ? 1 : -1;
  motors.setLeftSpeed(dir * 200);
  delay(10);
  motors.setRightSpeed(-dir * 200);
  delay(10);
  delay(LOOK_AROUND_STEP_MS);
  motors.setLeftSpeed(0);
  motors.setRightSpeed(0);
}

void sendHeartbeat() {
  digitalWrite(LED_PIN, ledToggle ? HIGH : LOW);
  ledToggle = !ledToggle;
  
  Serial.print("l");
  Serial.print(speedL);
  Serial.print("r");
  Serial.print(speedR);
  Serial.print('\n');
}

// Converts x and y components of a vector to a heading in degrees.
// This function is used instead of LSM303::heading() because we don't
// want the acceleration of the Zumo to factor spuriously into the
// tilt compensation that LSM303::heading() performs. This calculation
// assumes that the Zumo is always level.
template <typename T> float heading(LSM303::vector<T> v)
{
  float x_scaled =  2.0*(float)(v.x - compass.m_min.x) / ( compass.m_max.x - compass.m_min.x) - 1.0;
  float y_scaled =  2.0*(float)(v.y - compass.m_min.y) / (compass.m_max.y - compass.m_min.y) - 1.0;

  float angle = atan2(y_scaled, x_scaled)*180 / M_PI;
  if (angle < 0)
    angle += 360;
  return angle;
}

// Yields the angle difference in degrees between two headings
float relativeHeading(float heading_from, float heading_to)
{
  float relative_heading = heading_to - heading_from;

  // constrain to -180 to 180 degree range
  if (relative_heading > 180)
    relative_heading -= 360;
  if (relative_heading < -180)
    relative_heading += 360;

  return relative_heading;
}

// Average 10 vectors to get a better measurement and help smooth out
// the motors' magnetic interference.
float averageHeading()
{
  LSM303::vector<int32_t> avg = {0, 0, 0};

  for(int i = 0; i < 10; i ++)
  {
    compass.read();
    avg.x += compass.m.x;
    avg.y += compass.m.y;
  }
  avg.x /= 10.0;
  avg.y /= 10.0;

  // avg is the average measure of the magnetic vector.
  return heading(avg);
}
