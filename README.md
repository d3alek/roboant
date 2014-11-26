RoboAnt
=======

Autonomous robot made from Android phone + Arduino + Zumo Shield. Includes Android app, Arduino sketch and Python server.

Build dependencies
------------------

Build tested with an old version of [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android/tree/7e9589d5825a331107979fc5fe3548de4035ee82)

Additionally, the following change to the usb-serial-for-android code was made (not sure wheter it would be necessary any more if using a more recent version.

```
--- a/UsbSerialLibrary/src/com/hoho/android/usbserial/util/SerialInputOutputManager.java
+++ b/UsbSerialLibrary/src/com/hoho/android/usbserial/util/SerialInputOutputManager.java

@@ -99,6 +99,10 @@ public class SerialInputOutputManager implements Runnable {
 
     public void writeAsync(byte[] data) {
         synchronized (mWriteBuffer) {
+//            if (mWriteBuffer.remaining() < data.length) {
+//                Log.i(TAG, "Clearing buffer to prevent overflow");
+//                mWriteBuffer.clear();
+//            }
             mWriteBuffer.put(data);
         }
     }
```


Useful links
------------
Build your own RoboAnt - http://blog.inf.ed.ac.uk/insectrobotics/roboant/

Remote control setup step-by-step - http://blog.inf.ed.ac.uk/insectrobotics/roboant-remote-control/


Feel free to use it as a starting point for your own ideas and let us know what you created!

Made by the Insect Robotics Lab
University of Edinburgh
