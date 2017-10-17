# RIL
BOARD_PROVIDES_RILD := true
BOARD_RIL_CLASS := ../../../$(DEVICE_PATH)/mtk/ril

# Telephony configs
PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/configs/telephony/ecc_list.xml:system/etc/ecc_list.xml \
    $(DEVICE_PATH)/configs/telephony/spn-conf.xml:system/etc/spn-conf.xml
