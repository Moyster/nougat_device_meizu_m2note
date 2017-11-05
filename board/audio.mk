# Audio
TARGET_CPU_MEMCPY_OPT_DISABLE := true

# Audio config
PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/configs/audio/audio_policy.conf:system/etc/audio_policy.conf \
    $(DEVICE_PATH)/configs/audio/audio_device.xml:system/etc/audio_device.xml \
    $(DEVICE_PATH)/configs/audio/audio_effects.conf:system/etc/audio_effects.conf

# Media config
PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/configs/media/media_codecs.xml:system/etc/media_codecs.xml \
    $(DEVICE_PATH)/configs/media/media_codecs_ffmpeg.xml:system/etc/media_codecs_ffmpeg.xml \
    $(DEVICE_PATH)/configs/media/media_profiles.xml:system/etc/media_profiles.xml

# Codecs
PRODUCT_COPY_FILES += \
    frameworks/av/media/libstagefright/data/media_codecs_google_audio.xml:system/etc/media_codecs_google_audio.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_telephony.xml:system/etc/media_codecs_google_telephony.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_video_le.xml:system/etc/media_codecs_google_video_le.xml
