# CyanogenMod Hardware
BOARD_USES_CYANOGEN_HARDWARE := true
BOARD_HARDWARE_CLASS := $(DEVICE_PATH)/cmhw

# Device Props
TARGET_SYSTEM_PROP := $(DEVICE_PATH)/system.prop

# Sepolicy
BOARD_SEPOLICY_DIRS := $(DEVICE_PATH)/sepolicy

# Seccomp
BOARD_SECCOMP_POLICY := $(DEVICE_PATH)/seccomp

# GC tweak
PRODUCT_TAGS += dalvik.gc.type-precise

# Codecs
PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/configs/media/media_codecs.xml:system/etc/media_codecs.xml \
    $(DEVICE_PATH)/configs/media/media_profiles.xml:system/etc/media_profiles.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_audio.xml:system/etc/media_codecs_google_audio.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_telephony.xml:system/etc/media_codecs_google_telephony.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_video_le.xml:system/etc/media_codecs_google_video_le.xml
