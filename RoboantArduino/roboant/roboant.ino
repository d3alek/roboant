#include <ZumoMotors.h>

#define HEARTBEAT_MS 1000
#define LED_PIN 13

ZumoMotors motors;

static bool initialized = false;
static char buf[100];
static char buflength = 0;
static int i, j, k;
static unsigned long heartbeat_last;
static bool ledToggle = false;
int speedL, speedR;
static bool lookingAround;
static int look_around_count;

void setup() {
  pinMode(LED_PIN, OUTPUT);
  Serial.begin(115200);
  heartbeat_last = millis();
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
      buf[buflength++] = (char) Serial.read();
    } while (Serial.peek() != -1);
    process_buf();
  }
}

bool process_buf() {
  if (buflength == 0) {
    return false;
  }

  for (i = 0; i < buflength; ++i) {
    if (buf[i] == 'l') {
      for (j = i + 1; j < buflength; ++j) {
        if (buf[j] == 'r') break;
      }
      if (j == buflength) {
        // No r found, packet is not full
        return false;
      }
      buf[j] = '\0';
      speedL = atoi(buf+i+1);
      for (k = j + 1; k < buflength; ++k) {
        if (buf[k] == '\n') break;
      }
      if (k == buflength) {
        // No ending new line found, packet is not full
        buf[j] = 'r';
        return false;
      }
      buf[k] = '\0';
      speedR = atoi(buf+j+1);
      motors.setSpeeds(speedL, speedR);
      delay(10);
      i = k;
    }
  }
  buflength = 0;
  return true;
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
