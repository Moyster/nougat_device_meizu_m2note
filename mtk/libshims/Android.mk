LOCAL_PATH := $(call my-dir)

## libshim_agps
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    agps/crypto.c \
    agps/icu53.c \
    agps/ssl.c

LOCAL_SHARED_LIBRARIES := liblog libicuuc libssl libcrypto
LOCAL_MODULE := libshim_agps
LOCAL_CFLAGS := -O3 -Wno-unused-variable -Wno-unused-parameter
LOCAL_PROPRIETARY_MODULE := true
include $(BUILD_SHARED_LIBRARY)


## libshim_ui
include $(CLEAR_VARS)

LOCAL_SRC_FILES := ui/mtk_ui.cpp

#ui/mtk_gbc1.cpp

LOCAL_SHARED_LIBRARIES := libbinder libui
LOCAL_MODULE := libshim_ui
LOCAL_CFLAGS := -O3 -Wno-unused-variable -Wno-unused-parameter
LOCAL_PROPRIETARY_MODULE := true
include $(BUILD_SHARED_LIBRARY)


## libshim_gui
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    gui/mtk_gui.cpp \
    gui/SensorManager.cpp

LOCAL_SHARED_LIBRARIES := libbinder libgui liblog libui libutils
LOCAL_MODULE := libshim_gui
LOCAL_CFLAGS := -O3 -Wno-unused-variable -Wno-unused-parameter
LOCAL_PROPRIETARY_MODULE := true
include $(BUILD_SHARED_LIBRARY)


## libshim_snd
include $(CLEAR_VARS)

LOCAL_SRC_FILES := audio/mtk_audio.cpp

LOCAL_SHARED_LIBRARIES := libbinder libmedia
LOCAL_MODULE := libshim_snd
LOCAL_CFLAGS := -O3 -Wno-unused-variable -Wno-unused-parameter
LOCAL_PROPRIETARY_MODULE := true
include $(BUILD_SHARED_LIBRARY)


## libshim_xlog
include $(CLEAR_VARS)

LOCAL_SRC_FILES := xlog.c

LOCAL_SHARED_LIBRARIES := liblog
LOCAL_MODULE := libshim_xlog
LOCAL_CFLAGS := -O3 -Wno-unused-variable -Wno-unused-parameter
LOCAL_PROPRIETARY_MODULE := true
include $(BUILD_SHARED_LIBRARY)
