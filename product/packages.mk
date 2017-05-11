# Basic apps
PRODUCT_PACKAGES += \
    Stk \
    Torch \
    Snap

# Mtk symbols & shim
PRODUCT_PACKAGES += \
    libshim_agps \
    libshim_asc \
    libshim_gui \
    libshim_snd \
    libshim_ui \
    libshim_xlog

# libstlport
PRODUCT_PACKAGES += libstlport

# Ion
PRODUCT_PACKAGES += libion

# Audio
PRODUCT_PACKAGES += \
    audio.a2dp.default \
    audio.usb.default \
    audio_policy.stub \
    audio.r_submix.default \
    audio_policy.default \
    libaudio-resampler \
    libaudiopolicymanagerdefault \
    libtinyalsa \
    libtinycompress \
    libtinyxml

#PRODUCT_PACKAGES += libtinymix

# GPS
PRODUCT_PACKAGES += \
    libcurl \
    libnl_2

# Power HAL
PRODUCT_PACKAGES += \
    power.default \
    power.mt6753

# LiveDisplay
PRODUCT_PACKAGES += libjni_livedisplay

# USB
PRODUCT_PACKAGES += com.android.future.usb.accessory

# Wifi
PRODUCT_PACKAGES += \
    libwpa_client \
    hostapd \
    hostapd_cli \
    dhcpcd.conf \
    wpa_supplicant
