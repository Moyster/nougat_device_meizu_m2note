# Hostapd
PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/configs/hostapd/hostapd_default.conf:system/etc/hostapd/hostapd_default.conf \
    $(DEVICE_PATH)/configs/hostapd/hostapd.accept:system/etc/hostapd/hostapd.accept \
    $(DEVICE_PATH)/configs/hostapd/hostapd.deny:system/etc/hostapd/hostapd.deny

# GPS
PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/configs/gps/slp_conf:system/etc/slp_conf \
    $(DEVICE_PATH)/configs/gps/agps_profiles_conf2.xml:system/etc/agps_profiles_conf2.xml

# Bluetooth
PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/configs/bluetooth/auto_pair_devlist.conf:system/etc/bluetooth/auto_pair_devlist.conf \
    $(DEVICE_PATH)/configs/bluetooth/bt_stack.conf.debug:system/etc/bluetooth/bt_stack.conf.debug \
    $(DEVICE_PATH)/configs/bluetooth/bt_did.conf:system/etc/bluetooth/bt_did.conf \
    $(DEVICE_PATH)/configs/bluetooth/bt_stack.conf:system/etc/bluetooth/bt_stack.conf \
    $(DEVICE_PATH)/configs/bluetooth/bt_stack.conf.sqc:system/etc/bluetooth/bt_stack.conf.sqc

# Wifi
WPA_SUPPLICANT_VERSION := VER_0_8_X
BOARD_HOSTAPD_DRIVER := NL80211
BOARD_HOSTAPD_PRIVATE_LIB := lib_driver_cmd_mt66xx
BOARD_WPA_SUPPLICANT_DRIVER := NL80211
BOARD_WPA_SUPPLICANT_PRIVATE_LIB := lib_driver_cmd_mt66xx
WIFI_DRIVER_FW_PATH_PARAM:="/dev/wmtWifi"
WIFI_DRIVER_FW_PATH_STA:=STA
WIFI_DRIVER_FW_PATH_AP:=AP
WIFI_DRIVER_FW_PATH_P2P:=P2P

# Bluetooth
BOARD_HAVE_BLUETOOTH := true
BOARD_HAVE_BLUETOOTH_MTK := true
BOARD_BLUETOOTH_BDROID_BUILDCFG_INCLUDE_DIR := $(DEVICE_PATH)/bluetooth
