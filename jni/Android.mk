LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# Our C++ code throws exceptions.
LOCAL_CPP_FEATURES := exceptions

# We will need this later to support multiple architectures.
# LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libfoo.so

# Get all (.cpp and .c) files in jni folder.
# http://stackoverflow.com/a/8980441/1751037
MY_SRC_FILES := $(wildcard $(LOCAL_PATH)/*.cpp)
MY_SRC_FILES += $(wildcard $(LOCAL_PATH)/*.c)

LOCAL_MODULE    := cryptosystem
LOCAL_SRC_FILES := $(MY_SRC_FILES:$(LOCAL_PATH)/%=%)

include $(BUILD_SHARED_LIBRARY)
