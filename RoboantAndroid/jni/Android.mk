LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include /home/d3h/Downloads/OpenCV-2.4.8-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := adaptive_histogram 
LOCAL_SRC_FILES := jni_part.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)
