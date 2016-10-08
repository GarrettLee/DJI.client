# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)
ARM_ROOT = /cygdrive/d/android-ndk-r4
ARM_INC=$(ARM_ROOT)/build/platforms/android-5/arch-arm/usr/include/
ARM_LIB=$(ARM_ROOT)/build/platforms/android-5/arch-arm/usr/lib/



include $(CLEAR_VARS)
LOCAL_C_INCLUDES +=libx264/include
LOCAL_MODULE    := H264Android
LOCAL_SRC_FILES := H264Android.c cabac.c common.c dsputil.c golomb.c h264.c h264utils.c mpegvideo.c
#LOCAL_LDFLAGS += libx264/lib/libx264.a -nostdlib -Bdynamic -Wl,--no-undefined -Wl,-z,noexecstack  -Wl,-z,nocopyreloc -Wl,-soname,/system/lib/libz.so -Wl,-rpath-link=$(ARM_LIB),-dynamic-linker=/system/bin/linker -L$(ARM_LIB) -nostdlib -lc -lm -ldl -lgcc
#LOCAL_LDFLAGS += -L$(LOCAL_PATH)libx264/lib
#LOCAL_STATIC_LIBRARIES := libx264
LOCAL_LDFLAGS += $(LOCAL_PATH)/libx264/lib/libx264.a
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -lgcc
include $(BUILD_SHARED_LIBRARY)
